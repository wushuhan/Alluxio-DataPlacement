package ectests;

import alluxio.AlluxioURI;
import alluxio.client.file.FileSystem;
import alluxio.client.file.options.DeleteOptions;
import alluxio.exception.AlluxioException;
import ectests.ECReadExecutor;
import ectests.PrepareECTestFiles;
import org.apache.commons.lang3.RandomUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

import static java.lang.Thread.sleep;

public class MixedTest implements Callable {

    private ectests.PrepareECTestFiles fileWriter;
    private ectests.ECReadExecutor fileReader;
    private static final String TEST_PATH = "/ec_tests_master";
    private static final String opq_Path = System.getProperty("user.home") + "/ec_test_files/opQ.txt";
    private static ArrayBlockingQueue opQueue;
    private final FileSystem fs;
    private static final Logger LOG = LoggerFactory.getLogger(MixedTest.class);

    public static boolean isOpt;

    public MixedTest() throws Exception {
        this.fs = FileSystem.Factory.get();
    }

    private static ArrayBlockingQueue getopQueue() throws IOException {
        ArrayBlockingQueue queue = new ArrayBlockingQueue(4096);
        String line;
        BufferedReader br = new BufferedReader(new FileReader(opq_Path));
        while ((line = br.readLine()) != null) {
            queue.add(line);
        }
        br.close();
        return queue;
    }

    private void operate() throws Exception {
        String line = (String)opQueue.take();
        String op = line.split(" ")[0];
        int fileId = Integer.parseInt(line.split(" ")[1]);
        switch (op){
            case "w":
                writeFile(fileId);
                break;
            case "d":
                deleteFile(fileId);
                break;
            case "r":
                readFile(fileId);
                break;
            default:
                break;
        }
    }

    private void writeFile(int fileId) throws IOException, AlluxioException {
        AlluxioURI filePath = new AlluxioURI(String.format("%s/%s", TEST_PATH, fileId));
        fileWriter = new PrepareECTestFiles();
        if(!fs.exists(filePath)){
            LOG.info("Write file "+fileId);
            fileWriter.writeFile(fileId, isOpt);
        }
    }

    private void deleteFile(int fileId) throws IOException, AlluxioException {
        AlluxioURI filePath = new AlluxioURI(String.format("%s/%s", TEST_PATH, fileId));
        if (fs.exists(filePath)) {
            LOG.info("Delete file "+fileId);
            fs.delete(filePath, DeleteOptions.defaults().setRecursive(true).setUnchecked(true));
        }
    }

    private void readFile(int fileId) throws Exception {
        AlluxioURI filePath = new AlluxioURI(String.format("%s/%s", TEST_PATH, fileId));
        fileReader = new ECReadExecutor();
        if(fs.exists(filePath)){
            LOG.info("Read file "+fileId);
            fileReader.readFile(String.valueOf(fileId));
        }
    }

    @Override
    public Object call() throws Exception {
        operate();
        return null;
    }

    public static void main(String[] args) throws Exception {
        FileSystem fs = FileSystem.Factory.get();
        AlluxioURI testDir = new AlluxioURI(TEST_PATH);
        int sleep_time = Integer.parseInt(args[0]);
        isOpt = Boolean.parseBoolean(args[1]);
        if (fs.exists(testDir)) {
            fs.delete(testDir, DeleteOptions.defaults().setRecursive(true).setUnchecked(true));
        }

        opQueue = getopQueue();
        while (!opQueue.isEmpty()){
            MixedTest test = new MixedTest();
            FutureTask task = new FutureTask(test);
            new Thread(task).start();
            sleep(new RandomUtils().nextLong(sleep_time, sleep_time+250));
        }
    }
}
