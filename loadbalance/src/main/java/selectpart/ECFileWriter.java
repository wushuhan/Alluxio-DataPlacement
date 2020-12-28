package selectpart;

import alluxio.AlluxioURI;
import alluxio.client.WriteType;
import alluxio.client.file.FileOutStream;
import alluxio.client.file.FileSystem;
import alluxio.client.file.options.CreateFileOptions;
import alluxio.client.file.policy.FileWriteLocationPolicy;
import alluxio.client.file.policy.OptimizedPlacementPolicy;
import alluxio.client.file.policy.RoundRobinPolicy;
import alluxio.erasurecode.rawcoder.NativeRSRawEncoder;
import alluxio.exception.AlluxioException;
import alluxio.util.CommonUtils;
import alluxio.util.FormatUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static java.lang.Math.min;

/**
 * Created by renfei on 2017/11/24.
 * <p>
 * This class is the client writer that writes files into Alluxio, given k and the byte[] to write.
 */
public class ECFileWriter {//Todo：设置合适的K和N

    protected int mK;

    protected int mN;

    protected int mFileSize;

    private CreateFileOptions mWriteOptions;

    private FileSystem mFileSystem;

    private AlluxioURI mFilePath;

    private int tBlockSize;

    private static final Logger LOG = LoggerFactory.getLogger(ECFileWriter.class);

    private FileWriteLocationPolicy policy;

    public ECFileWriter(
            int k, int n, int fileSize, FileSystem fileSystem,
            AlluxioURI writePath, boolean isOpt
    ) {
        mK = k;
        mN = n;
//        mFileSize = fileSize;
        mFileSystem = fileSystem;
        mFilePath = writePath;
        if (fileSize % k == 0) {
            tBlockSize = fileSize / mK;
        } else {
            // NOTE: Assume that file size in bytes is much larger than the number of machines
            tBlockSize = fileSize / mK + 1;
        }
        mFileSize = (k + n) * tBlockSize;
        if(isOpt){
            policy = new OptimizedPlacementPolicy(mFilePath);
        } else {
            policy = new RoundRobinPolicy();

        }

        mWriteOptions = CreateFileOptions.defaults().setWriteType(WriteType.MUST_CACHE)
                .setBlockSizeBytes(tBlockSize)
                .setLocationPolicy(policy);
//                .setLocationPolicy(rrp);
//                .setWriteTier(1)
//                .setKValueForEC(mK)
//                .setNValueForEC(mN);
        LOG.info("k value after set in write is " + mK + "n value after set in write is" + mN);
    }

    public void setWriteOption(CreateFileOptions writeOptions) {
        mWriteOptions = writeOptions;
    }

    public void writeFile(byte[] buf) {
        ByteBuffer[] input = new ByteBuffer[mK];
        ByteBuffer[] output = new ByteBuffer[mN];
        int curIndex = 0;
        for (int i = 0; i < mK; i++) {
            input[i] = ByteBuffer.allocate(tBlockSize);
            input[i].order(ByteOrder.nativeOrder());
            for (int j = 0; j < tBlockSize; j++) {
                input[i].put(buf[min(curIndex++, buf.length - 1)]);
            }
            input[i].flip();
        }
        for (int i = 0; i < mN; i++) {
            output[i] = ByteBuffer.allocate(tBlockSize);
            output[i].order(ByteOrder.nativeOrder());
        }

        if (mN == 0) {
            // numParities= 0 nothing to do
            LOG.info("n=0 nothing to do.");
        } else {
            // numParities > 0
            if (mK == 1) {
                // k=1 and n>k: replication
                LOG.info("Replication.");
                // copy input buffer into each of the output buffer
                for (int j = 0; j < mN; j++) {
                    input[0].position(0);
                    output[j].put(input[0]);
                    output[j].position(0);
                    input[0].flip();

                }
            } else {
                LOG.info("Will do encoding using encoder");
                // k>1 and n>k: Call the encoder
                long startEncodeTimeMs = CommonUtils.getCurrentMs();
                NativeRSRawEncoder rsEncoder = new NativeRSRawEncoder(mK, mN);
                rsEncoder.encode(input, output);
                LOG.info(FormatUtils.formatTimeTakenMs(
                        startEncodeTimeMs,
                        "ecisawesome:" + tBlockSize + "*" + mK + "=" + tBlockSize * mK + " and " + tBlockSize + "*" + mN + "=" + tBlockSize * mN + ": Time taken to encode file "
                ));
            }
        }

//        byte[] extBuf = new byte[tBlockSize*(mK+mN)];
//        int curOffset = 0;
//        for(int i=0;i<mK;i++){
//            int remain = input[i].remaining();
//            input[i].get(extBuf, curOffset, tBlockSize);
//            curOffset += tBlockSize;
//        }
//        for(int j=0;j<mN;j++){
//            output[j].get(extBuf, curOffset, tBlockSize);
//            curOffset += tBlockSize;
//        }

        try {
            // the write policy is uniquely random
            FileOutStream os = mFileSystem.createFile(mFilePath, mWriteOptions);
            for (int j = 0; j < mK; j ++) {
                LOG.info("Writing block " + j);
                if (input[j] == null) {
                    LOG.info("Error: input null");
                }
                os.write(input[j].array());//Todo：找到接口配置分发策略(alluxio.user.file.write.location.policy.class)
            }
            for (int j = 0; j < mN; j ++) {
                LOG.info("Writing block " + j + mK);
                if (output[j] == null) {
                    LOG.info("Error: output null");
                }
                os.write(output[j].array());
            }
            os.close();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (AlluxioException e) {
            e.printStackTrace();
        }


    }

//    public static void main(String[] args){
//        int mk = 1;
//
//        int mn = 1;
//
//        int fileSize = 10000;
//
//        FileSystem fs = FileSystem.Factory.get();
//
//        AlluxioURI filePath = new AlluxioURI(String.format("%s/%s", "/ec_test_files", 1));
//
//        ECFileWriter ecFileWriter = new ECFileWriter(mk, mn, fileSize, fs, filePath, false);
//
//        byte[] testBuf = new byte[10000];
//        ecFileWriter.writeFile(testBuf);
//    }
}
