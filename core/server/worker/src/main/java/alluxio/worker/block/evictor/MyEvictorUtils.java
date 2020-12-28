package alluxio.worker.block.evictor;
import alluxio.Configuration;
import alluxio.PropertyKey;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Array;
import java.util.Deque;
import java.util.logging.Logger;
import java.io.File;


public class MyEvictorUtils {

    public static double[] getLinePara(double[][] points) {
        double dbRt[] = new double[2];
        double dbXSum = 0;
        for (int i = 0; i < points[0].length; i++) {
            dbXSum = dbXSum + points[0][i];
        }
        double dbXAvg = dbXSum / points[0].length;
        double dbWHeadVal = 0;
        for (int i = 0; i < points[0].length; i++) {
            dbWHeadVal = dbWHeadVal + (points[0][i] - dbXAvg) * points[1][i];
        }
        double dbWDown = 0;
        double dbWDownP = 0;
        double dbWDownN = 0;
        dbXSum = 0;
        for (int i = 0; i < points[0].length; i++) {
            dbWDownP = dbWDownP + points[0][i] * points[0][i];
            dbXSum = dbXSum + points[0][i];
        }
        dbWDown = dbWDownP - (dbXSum * dbXSum / points[0].length);
        double dbW = dbWHeadVal / dbWDown;
        dbRt[0] = dbW;
        double dbBSum = 0;
        for (int i = 0; i < points[0].length; i++) {
            dbBSum = dbBSum + (points[1][i] - dbW * points[0][i]);
        }
        double dbB = dbBSum / points[0].length;
        dbRt[1] = dbB;
        return dbRt;
    }

    public static double[][] calculateAccessDense(Deque accessRecord, int intervalCount) {
        int[] frames = (int[]) Array.newInstance(int.class, accessRecord.size());
        System.arraycopy(accessRecord.toArray(), 0, frames, 0, accessRecord.size());

        int framenum=frames.length;
        // 概率密度
        double Pd;

        // 区间上下界和中间值  D
        int upInterval, downInterval, middleValue;
        // 每个区间内的帧数量
        int count = 0;

        // 横纵坐标保存数组
        double[][] frameArray = new double[intervalCount][2];

//        Arrays.sort(frames);

        int minFrame = frames[0];
        int maxFrame = frames[framenum - 1];
        // 区间宽度
        int interval = (maxFrame - minFrame) / intervalCount;

        System.out.println("Min=" + minFrame + " " + "Max=" + maxFrame);

        for (int k = 0; k < intervalCount; k++) {

            upInterval = minFrame + (k + 1) * interval - 1;
            downInterval = minFrame + k * interval;
            middleValue = downInterval + interval / 2; // 中点值（每一个横坐标）

            for (int i = 0; i < framenum; i++) {
                if (frames[i] < upInterval && frames[i] >= downInterval) {
                    count++;
                }
            }
            Pd = (double) count / framenum / interval; // 纵坐标
            frameArray[k][0] = middleValue;
            frameArray[k][1] = Pd;
            count = 0;
        }

        return frameArray;
    }

    public static double[] updateIOLoad() {
        Logger log = Logger.getLogger("MyEvictorUtils.class");
        log.info("开始收集磁盘IO使用率");
        double ioUsage[] = new double[2];
        Process pro = null;
        Runtime r = Runtime.getRuntime();
        try {
            String command = "iostat -d -x";
            pro = r.exec(command);
            BufferedReader in = new BufferedReader(new InputStreamReader(pro.getInputStream()));
            String line = null;
            int count = 0;
            while ((line = in.readLine()) != null) {
                if (line.contains("sda")) {
                    log.info(line);
                    String[] temp = line.split("\\s+");
                    if (temp.length > 1) {
                        float util = Float.parseFloat(temp[temp.length - 1]);
                        ioUsage[0] = (ioUsage[0] > util) ? ioUsage[0] : util;
                    }
                    log.info("sda: IO使用率为: " + ioUsage[0]);
                }
                if (line.contains("sdb")) {
                    log.info(line);
                    String[] temp = line.split("\\s+");
                    if (temp.length > 1) {
                        float util = Float.parseFloat(temp[temp.length - 1]);
                        ioUsage[1] = (ioUsage[1] > util) ? ioUsage[1] : util;
                    }
                    log.info("sdb: IO使用率为: " + ioUsage[1]);
                }
            }
            // if(ioUsage > 0){
            // log.info("本节点磁盘IO使用率为: " + ioUsage);
            // ioUsage /= 100;
            // }
            in.close();
            pro.destroy();
        } catch (IOException e) {
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            // log.error("IoUsage发生InstantiationException. " + e.getMessage());
            // log.error(sw.toString());
        }
        return ioUsage;
    }

    public static double[] updateStorageLoad() {
        File[] paths = {new File("/dev/shm"), new File("/home")};
        double[] storageLoad = new double[2];
        double[] total = {300, 1024};
        for (int i = 0; i < paths.length; i++) {
            double usable = paths[i].getUsableSpace() / 1024 / 1024;
//            double total = paths[i].getTotalSpace() / 1024 / 1024;

            storageLoad[i] =1 - usable/total[i];
        }
        return storageLoad;
    }

    public static int getBandWidth(double blockSize, String Tier) {
        int BWOfMEM = 6656;
        int[] BWOfSSD = {518, 966, 1843, 2662, 3276, 3788, 4300, 4505, 4505, 3788,
                3481, 3481, 3481, 3481, 3481, 3481, 3481, 3379, 3072};
        int[] BWOfHDD = {63, 104, 151, 224, 277, 352, 369, 407, 395, 403, 393};
        int bw = 0;
        if (Tier == "MEM") {
            bw=BWOfMEM;
        }
        if (Tier == "SSD") {
            for (int i = 0; Math.pow(2, i) <= blockSize; i++)
                bw = BWOfSSD[i];
        }
        return bw;
    }

//    public int isRedundency(Long blockId){
//
//    }
}