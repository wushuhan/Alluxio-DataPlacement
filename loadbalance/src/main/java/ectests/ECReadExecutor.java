/*
 * Licensed to the University of California, Berkeley under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package ectests;

import alluxio.AlluxioURI;
import alluxio.client.ReadType;
import alluxio.client.file.FileInStream;
import alluxio.client.file.FileSystem;
import alluxio.client.file.URIStatus;
import alluxio.client.file.options.OpenFileOptions;
import alluxio.erasurecode.rawcoder.NativeRSRawDecoder;
import alluxio.exception.AlluxioException;
import alluxio.util.CommonUtils;
import alluxio.util.FormatUtils;
import org.apache.commons.lang3.RandomUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.*;
import java.util.concurrent.*;

import static java.lang.Thread.sleep;

public class ECReadExecutor implements Callable{

    private static final Logger LOG = LoggerFactory.getLogger(ECReadExecutor.class);

    private FileSystem mFileSystem = FileSystem.Factory.get();

    // ExecutorService executor;
    private ExecutorService executor;

    private CompletionService<ReadFromBlockResult> completionService;

    private List<Future<ReadFromBlockResult>> futures;

    private int mNumThreads;

    private static ArrayBlockingQueue fileNamequeue;

    private static ArrayBlockingQueue readTimequeue;
    private static long readCnt;

    private static long originTimeMs;

//    private static ArrayList<Double> lambda;

    public static final String TEST_PATH = "/ec_tests_master";

    private static final String fileNamesPath = System.getProperty("user.home") + "/ec_test_files/fileNamequeue.txt";

    private static final String readTimesPath = System.getProperty("user.home") + "/ec_test_files/readTimequeue.txt";

    private static final String KValuePath = System.getProperty("user.home") + "/ec_test_files/k.txt";

    private static final String NValuePath = System.getProperty("user.home") + "/ec_test_files/n.txt";

    private static final String logDirPath = System.getProperty("user.dir") + "/TieredStoreLogs";

    private static FileWriter mLog;

    private static FileWriter decodingLog;

    private static FileWriter readingLog;

    private static FileWriter inmemLog;

    private static ArrayList mKValues = new ArrayList();

    private static ArrayList mNValues = new ArrayList();

    public static int floorDiv(int x, int y) {
        int r = x / y;
        if ((x ^ y) < 0 && (r * y != x)) {
            r --;
        }
        return r;
    }

    public ECReadExecutor() throws Exception{

        mNumThreads = 20;
        // executor = Executors.newFixedThreadPool(mNumThreads);

        LOG.info("Using the custom future returning service.");
//        executor = new CustomFutureReturningExecutor(mNumThreads,
//                                                     mNumThreads,
//                                                     Long.MAX_VALUE,
//                                                     TimeUnit.DAYS,
//                                                     new LinkedBlockingQueue<Runnable>()
//        );
        executor = Executors.newCachedThreadPool();
        // A list of futures for ExecutorCompletionService so that we can cancel them if unnecessary
        futures = new ArrayList<Future<ReadFromBlockResult>>(mNumThreads);

        File tLogDir = new File(logDirPath);
        if (!tLogDir.exists()) {
            tLogDir.mkdir();
        }
//        num++;
        mLog = new FileWriter(logDirPath + "/readLatency.txt", true); // append
        decodingLog = new FileWriter(logDirPath + "/decodingLatency.txt", true);
        readingLog = new FileWriter(logDirPath + "/fileReadLatency.txt", true);
        inmemLog = new FileWriter(logDirPath + "/inMemLatency.txt", true);
        BufferedReader br = new BufferedReader(new FileReader(NValuePath));
        String line;
        while ((line = br.readLine()) != null)
            mNValues.add(Integer.parseInt(line));
        br = new BufferedReader(new FileReader(KValuePath));
        while ((line = br.readLine()) != null)
            mKValues.add(Integer.parseInt(line));
        br.close();
    }

    /**
     * Reads specified number of blocks along with running additional reads as specified by the
     * mNumAdditionalReads. to mitigate straggling nodes.
     *
     * @param
     * @param
     * @return
     * @throws IOException
     * @throws AlluxioException
     */

    public int readFileStragglerMitigate(
            ReadType readType, int numAdditionalReads, AlluxioURI alluxioURI, String filename,
            boolean skipDecoding, byte[] data, int mKValueForEc, int mNValueForEc, long readCnt
    ) throws Exception {

        long mBlockSize;
        int mNumAdditionalReads;
        int numTasks;
        final OpenFileOptions mReadOptions;
        final AlluxioURI mFilepath;
        HashSet<Integer> blocksNumsToRead;
        long mFilesize;
        int mTotalBytesRead;
        boolean mSkipDecoding = false;
        // setting this to 0 as of now
        int mPosInBlocks = 0;
        ArrayList<Integer> blocksToRead;
        // 设定使用多少个多余线程读数据
        mNumAdditionalReads = numAdditionalReads;
        mReadOptions = OpenFileOptions.defaults().setReadType(readType);
        mFilepath = alluxioURI;
        mSkipDecoding = skipDecoding;

        completionService = new ExecutorCompletionService<ReadFromBlockResult>(executor);
        try {
            URIStatus status = mFileSystem.getStatus(mFilepath);
//            mKValueForEc = (int) mKValues.take();
            LOG.info("debug: k value got from status is " + mKValueForEc);
//            mNValueForEc = (int) mNValues.take();
            LOG.info("debug: n value got from status is " + mNValueForEc);
            mBlockSize = status.getBlockSizeBytes();
            // cannot use the filesize returned by status since it also counts parities
            // as parities are written as additional blocks in the file
            mFilesize = mBlockSize * mKValueForEc;
            LOG.info("mblocksize is " + mFilesize);
            inmemLog.write(filename+" "+mFilesize+" "+status.getInMemoryPercentage()+"\n");
            inmemLog.flush();
        } catch (Exception e) {
            e.printStackTrace();
            LOG.error("Error setting up ECFileRead", e);
            throw e;
        }

        LOG.info("debug: number of additional reads = " + mNumAdditionalReads);
        numTasks = Math.min(mKValueForEc + mNumAdditionalReads, mKValueForEc + mNValueForEc);
        blocksNumsToRead = new HashSet<Integer>(numTasks);
        mTotalBytesRead = 0;

        // check that num of threads allowed is higher than num tasks to be run in parallel
//        assert mNumThreads >= numTasks;
        //////

        ByteBuffer returnData = ByteBuffer.wrap(data);
        ReadBlocksProcessor processor = new ReadBlocksProcessor(mKValueForEc, mNValueForEc);
        int bytesReadFromEachBlock = 0;

        LOG.info("Reading data with straggler mitigation from file , " + mFilepath);


        // amount of data to read from each block
        int perBlockBufferSize = floorDiv(data.length, mKValueForEc);// Math.floorDiv(data.length,
        // mKValueForEc);
        final long startTimeMs = CommonUtils.getCurrentMs();

        // Get the block numbers to be read uniformly at random
//        blocksNumsToRead = chooseRandomSubset(mNValueForEc, numTasks);
        // LOG.info(FormatUtils.formatTimeTakenMs(startTimeMs, "ecisawesome: Choosing random subset "));
        long submitReadTaskTime = CommonUtils.getCurrentMs();

        int cnt = 0;
//        blocksToRead = getBlocksToRead(filename, circle, filenum);
//        for (int blockNum : blocksToRead){
        for (int blockNum = 0; blockNum < numTasks; blockNum++) {//todo：修改读请求发送策略
            if (futures.size() > cnt) {
                futures.set(cnt, completionService.submit(new ReadFromABlock(
                        cnt,
                        mFileSystem,
                        mFilepath,
                        mReadOptions,
                        blockNum,
                        mPosInBlocks,
                        perBlockBufferSize,
                        mBlockSize,
                        readCnt
                )));
            } else {
                futures.add(completionService.submit(new ReadFromABlock(
                        cnt,
                        mFileSystem,
                        mFilepath,
                        mReadOptions,
                        blockNum,
                        mPosInBlocks,
                        perBlockBufferSize,
                        mBlockSize,
                        readCnt
                )));
            }
            cnt++;
        }
        LOG.info(FormatUtils.formatTimeTakenMs(
                startTimeMs,
                "ecisawesome: Setting futures for " + mFilepath
        ));
        LOG.info("Submitted all read tasks.");

        int blkReadcount = 0;
        try {
            for (int i = 0; i < numTasks; i++) {
                Future<ReadFromBlockResult> completedTask = completionService.take();
                // waiting for first task to complete
                ReadFromBlockResult returnBlkNumAndBufClass = completedTask.get();
                // the above line waits to get the result from the task that finished earliest
                processor.registerARead(returnBlkNumAndBufClass);
                blkReadcount++;
                LOG.info("Count = " + blkReadcount);
                if (blkReadcount == mKValueForEc) {
                    LOG.info("Required number of reads done, breaking.." + blkReadcount);

                    LOG.info(FormatUtils.formatTimeTakenMs(
                            submitReadTaskTime,
                            "ecisawesome" + data.length + ":skipDecode:" + (mSkipDecoding ? 1 : 0)
                                    + ": Reading necessary blocks in parallel for " + mFilepath
                    ));

                    // Necessary number of reads have been finished. Now process them.
                    executor.shutdown();
                    break;
                }
            }
        } catch (Exception e) {
            if (blkReadcount < mKValueForEc) {
                // Not sufficient blocks read
                e.printStackTrace();
                LOG.error(
                        "Exception in read method before reading sufficient number of blocks for file"
                                + mFilepath,
                        e
                );
                throw e;
            }
        }
        //(rashmi) experimenting without canceling the additional reads.
        //If not cancelling, then need to have extraThreads > 0 (see above). If cancelling can set this to 0.
    /*finally {
      // all remaining futures cancelled
      long futureCancelStartTime = CommonUtils.getCurrentMs();
      try {
        for (Future<ReadFromBlockResult> f : futures) {
          f.cancel(true);
        }
      } catch (Exception e) {
        LOG.info("Exception came when cancelling futures for file " + mFilepath);
        e.printStackTrace();
      }

      LOG.info(FormatUtils.formatTimeTakenMs(futureCancelStartTime,
          "ecisawesome: Cancelling futures for " + mFilepath + " with k =" + mKValueForEc));
    }*/
        LOG.info(
                FormatUtils.formatTimeTakenMs(
                        startTimeMs,
                        "readFile file " + mFilepath + " in parallel "
                ));

        // get the number of bytes read from each block as the minimum read from blocks
        bytesReadFromEachBlock = Collections.min(processor.mBytesRead);
        // process the read blocks
        if (!mSkipDecoding && (mKValueForEc > 1) && (mNValueForEc > 0)) {
            // do decoding
            processor.processAllReads();
            // combine the buffers in the k data blocks into the output buffer that wraps the byte array
            // provided as input
            for (int i = 0; i < mKValueForEc; i++) {
                // set position to 0 to read from beginning
                processor.mReadData[i].position(0);
                LOG.info("for debug: put data size " + processor.mReadData[i].remaining());
                returnData.put(processor.mReadData[i]);
            }
        } else {
            LOG.info("ecisawesome: Not doing decoding.");
            // no decoding to be done - copy k valid blocks read into output buffer
            int copyCount = 0;
            for (int j : processor.mValidBlkNums) {
                processor.mReadData[j].position(0);
                // transfer only bytesReadFromEAchBlock number of bytes
                processor.mReadData[j].limit(bytesReadFromEachBlock);
                returnData.put(processor.mReadData[j]);
                copyCount++;
                // copy only k blocks - if finished copying k blocks then break
                if (copyCount >= mKValueForEc) {
                    break;
                }
            }
        }
        readingLog.write(filename+" "+(CommonUtils.getCurrentMs() - startTimeMs)+"\n");
        readingLog.flush();

        return (returnData.position());
    }

//    private ArrayList<Integer> getBlocksToRead(String filename, int circle, int filenum) {
//        int fileId = Integer.parseInt(filename);
//        String blocksToReadPath = System.getProperty("user.home") + "/ec_test_files/blocksToRead.txt";
//        String line;
//        ArrayList<Integer> BlocksToRead = new ArrayList<>();
//        int lineCount = 0;
//        try {
//            BufferedReader br = new BufferedReader(new FileReader(blocksToReadPath));
//            while((line = br.readLine()) != null){
//                if(fileId + circle * filenum == lineCount){
//                    break;
//                }
//                lineCount++;
//            }
//        String[] Blocks = line.split(" ");
//        for(int i = 0; i < Blocks.length; i++){
//            BlocksToRead.add(Integer.parseInt(Blocks[i]));
//        }
//
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        return BlocksToRead;
//    }


    public void close() {
        long executorShutdownStartTime = CommonUtils.getCurrentMs();
        executor.shutdown();
        LOG.info(FormatUtils.formatTimeTakenMs(
                executorShutdownStartTime,
                "ecisawesome: Shutting down the executor "
        ));
    }

//    public ArrayList<Double> getLambda() throws IOException {
//        BufferedReader lambdaReader = new BufferedReader(new FileReader(
//                System.getProperty("user.home") + "/ec_test_files/lambda.txt"));
//        String[] strlambda = lambdaReader.readLine().split(" ");
//        ArrayList<Double> res = new ArrayList<>();
//        for (String ele : strlambda){
//            res.add(Double.valueOf(ele));
//        }
//        return res;
//    }

    public void readFile(String fileName) throws Exception {

        AlluxioURI alluxioURI = new AlluxioURI(String.format("%s/%s", TEST_PATH, fileName));
        ReadType readType = ReadType.CACHE_PROMOTE;
        int numAdditionalReads = 10; // 设置读取副本的线程上限
        boolean skipDecoding = false;
        int mBlockSizeBytes = this.getBlockSizeBytes(alluxioURI);
        int mKValueForEC = (int)mKValues.get(Integer.parseInt(fileName));
        int mNValueForEC = (int)mNValues.get(Integer.parseInt(fileName));
        byte[] data = new byte[mBlockSizeBytes*mKValueForEC];
        long ByteSizetoRead = mBlockSizeBytes*mKValueForEC;
//        int circleTimes = 50;//循环读取统一文件的次数
//        lambda = getLambda();

//        for (int i = 0; i < circleTimes; i++){
        long startTimeMs = CommonUtils.getCurrentMs();
//        int filenum = lambda.size();
//        ECReadExecutor ecReadExecutor = new ECReadExecutor();
//        long waitTimeMs = CommonUtils.getCurrentMs() - originTimeMs - (long)readTimequeue.take();
//        LOG.info("Read file "+fileName);
//        if(waitTimeMs > 0) sleep(waitTimeMs);
        readCnt++;
        this.readFileStragglerMitigate(readType, numAdditionalReads, alluxioURI, fileName, skipDecoding, data, mKValueForEC, mNValueForEC, readCnt);
        long endTimeMs = CommonUtils.getCurrentMs();
        long tTimeTaken = endTimeMs - startTimeMs;
        LOG.info("Read file "+fileName+" took "+tTimeTaken+" ms.");
//            if (waitTimeMs > tTimeTaken){
//                sleep(waitTimeMs - tTimeTaken);
//                LOG.info("file "+fileName+" : waited for "+(waitTimeMs - tTimeTaken)+" ms.");
//            }
//        }
//        for (int i = 0; i < 3; i++){
//
////            mLog.write("read file "+fileName+": should read bytes is "+ByteSizetoRead +" truly get bytes is "+readByteSize+"\n");
////            mLog.write("ec Read " + fileName + " in " + tTimeTaken + " ms.\n");
//            if(waitTimeMs > tTimeTaken){
//                sleep(waitTimeMs - tTimeTaken);
//            }
//        }
//        mLog.flush();
    }

    @Override
    public Object call() throws Exception {
        readFile((String) fileNamequeue.take());
        return null;
    }


    private class ReadBlocksProcessor {

        private ByteBuffer[] mReadData;

        private HashSet<Integer> mValidBlkNums;

        private int[] mInvalidBlkNums;

        private int mKValueForEC;

        private int mNValueForEC;

        private List<Integer> mBytesRead;

        public ReadBlocksProcessor(int mkValueForEc, int mNValueForEc) {
            mKValueForEC = mkValueForEc;
            mNValueForEC = mNValueForEc;
            mReadData = new ByteBuffer[mKValueForEC + mNValueForEC];
            mValidBlkNums = new HashSet<Integer>(mKValueForEC);
            mInvalidBlkNums = new int[mNValueForEc];
            mBytesRead = new ArrayList<Integer>();
        }

        private void registerARead(ReadFromBlockResult blkNumBufClass) {
            int blkNum = blkNumBufClass.mBlockNum;
            mReadData[blkNum] = blkNumBufClass.mBuffer.duplicate();
            // set the byte order
            mReadData[blkNum].order(ByteOrder.nativeOrder());
            mValidBlkNums.add(blkNum);
            mBytesRead.add(blkNumBufClass.mReadLen);
        }

        /**
         * Processes all the reads together once minimum number of blocks have been read. Identifies
         * which data blocks have not been read and then uses decoder to decode them.
         *
         * @return bytes processed in each block (which is same for each block)
         */
        private void processAllReads() {
            // get invalid data block numbers to be recovered
            int idx = 0;
            int bytesToProcess;
            for (int i = 0; i < mKValueForEC; i++) {
                if (!mValidBlkNums.contains(i)) {
                    mInvalidBlkNums[idx++] = i;
                }
            }
            mInvalidBlkNums = Arrays.copyOf(mInvalidBlkNums, idx);

            // update mPosInBlocks based on min of the bytes read from all the k valid blks
            bytesToProcess = Collections.min(mBytesRead);
            // truncate all the buffers to bytesToProcess len
            // Always make sure that the decoding method using the "limit"
            for (int j : mValidBlkNums) {
                mReadData[j].limit(bytesToProcess);
            }

            if (mInvalidBlkNums.length > 0) {
                // one or more of data blocks missing: do deocoding
                LOG.info("Invalid block numbers are: " + Arrays.toString(mInvalidBlkNums));
                LOG.info("bytesToProcess in each block: " + bytesToProcess);
                ByteBuffer[] outputs = new ByteBuffer[mInvalidBlkNums.length];
                // Create output buffers
                for (int j = 0; j < mInvalidBlkNums.length; j++) {
                    outputs[j] = ByteBuffer.allocate(bytesToProcess);
                    outputs[j].order(ByteOrder.nativeOrder());
                }
                long startDecodeTime = CommonUtils.getCurrentMs();
                NativeRSRawDecoder rsDecoder = new NativeRSRawDecoder(
                        mKValueForEC,
                        mNValueForEC
                );
                rsDecoder.decode(mReadData, mInvalidBlkNums, outputs);
                long stopDecodeTime = CommonUtils.getCurrentMs();
                long DecodeTime = stopDecodeTime - startDecodeTime;
                LOG.info("ecisawesome: Decoding took "+DecodeTime+" ms");

                try {
                    decodingLog.write(mInvalidBlkNums.length+" "+DecodeTime+"\n");
                    decodingLog.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }

//              LOG.info(FormatUtils.formatTimeTakenMs(startDecodeTime, "ecisawesome: Decoding "));
                idx = 0;
                for (int l : mInvalidBlkNums) {
                    mReadData[l] = ByteBuffer.allocate(bytesToProcess);
                    // set the byte order
                    mReadData[l].order(ByteOrder.nativeOrder());
                    mReadData[l].put(outputs[idx]);
                    mReadData[l].flip();
                    idx++;
                }
            }
        }
    }

    /**
     * This class represents the return type for each read task submitted. A new class is needed in
     * order to keep track of the block numbers along with the buffers.
     *
     * @author rashmikv
     */
    private class ReadFromBlockResult {

        private int mBlockNum;

        private ByteBuffer mBuffer;

        private int mReadLen;

        public ReadFromBlockResult(int blockNum, ByteBuffer buf, int readLen) {
            mBlockNum = blockNum;
            mBuffer = buf;
            mReadLen = readLen;
        }

    }

    public abstract class FutureTaskWrapper<T> extends FutureTask<T> {

        public FutureTaskWrapper(Callable<T> c) {
            super(c);
        }

        abstract int getTaskId();
    }

    public interface IdentifiableCallable<T> extends Callable<T> {

        int getId(); // For getting task Id

        void cancelTask(); // Method for supporting non-standard cancellation

        RunnableFuture<T> newTask();
    }

    public class CustomFutureReturningExecutor extends ThreadPoolExecutor {

        public CustomFutureReturningExecutor(
                int corePoolSize, int maximumPoolSize, long keepAliveTime,
                TimeUnit unit, BlockingQueue<Runnable> workQueue
        ) {
            super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue);
        }

        @Override
        protected <T> RunnableFuture<T> newTaskFor(Callable<T> callable) {
            if (callable instanceof IdentifiableCallable) {
                return ((IdentifiableCallable<T>) callable).newTask();
            } else {
                return super.newTaskFor(callable); // A regular Callable, delegate to parent
            }
        }
    }

    private class ReadFromABlock implements IdentifiableCallable<ReadFromBlockResult> {

        private final int mTaskId;

        volatile boolean mCancelled;

        volatile boolean mCompleted;

        FileSystem mTachyonFileSystem;

        FileInStream mInputStream = null;

        OpenFileOptions mReadOptions;

        int mBlockNum;

        int mToReadLen;

        int mStartPos;

        long mBlockSize;

        long mReadCnt;

        AlluxioURI mFilepath;

        private ReadFromABlock(
                int taskId, FileSystem tachyonFileSystem, AlluxioURI filepath, OpenFileOptions readOptions,
                int blockNum, int startPos, int len, long blockSize, long readCnt
        ) {
            mTaskId = taskId;
            mTachyonFileSystem = tachyonFileSystem;
            mFilepath = filepath;
            mReadOptions = readOptions;
            mBlockNum = blockNum;
            mToReadLen = len;
            mStartPos = startPos;
            mBlockSize = blockSize;
            mReadCnt = readCnt;

        }

        private ReadFromBlockResult read() throws IOException, AlluxioException, Exception {
            ReadFromBlockResult retVal;
            int bytesRead = 0;
            LOG.info("In thread for reading block number = " + mBlockNum);
            ByteBuffer buf = ByteBuffer.allocate(mToReadLen);
            final long blkStartTimeMs = CommonUtils.getCurrentMs();

            try {
                final long fileOpenStartTimeMs = CommonUtils.getCurrentMs();
                mInputStream = mTachyonFileSystem.openFile(mFilepath, mReadOptions);
                 LOG.info(FormatUtils.formatTimeTakenMs(fileOpenStartTimeMs,
                 "ecisawesome: Opening file = " + mFilepath.getName() + " for block " + mBlockNum));

                final long seekStartTimeMs = CommonUtils.getCurrentMs();
                mInputStream.seek((long) mBlockNum * mBlockSize + mStartPos);
                bytesRead = mInputStream.read(buf.array());
                 LOG.info(FormatUtils.formatTimeTakenMs(seekStartTimeMs,
                 "ecisawesome: Seeking and reading from file = " + mFilepath.getName() + " for block "
                 + mBlockNum));

                mInputStream.close();

            } catch (Exception e) {
                LOG.info("Exception in opening & reading file: " + mFilepath + " for block number "
                                 + mBlockNum + " throwing exception.");
                throw e;
            }
            LOG.info(FormatUtils.formatTimeTakenMs(
                    blkStartTimeMs,
                    "ecisawesome: Reading file = " + mFilepath.getName() + " and block " + mBlockNum
            ));
            mLog.write(mReadCnt+" "+mFilepath.getName()+" "+mBlockNum+" "+(CommonUtils.getCurrentMs()-blkStartTimeMs)+"\n");
            mLog.flush();
            LOG.info("Read block " + mBlockNum);
            buf.order(ByteOrder.nativeOrder());
            retVal = new ReadFromBlockResult(mBlockNum, buf, bytesRead);
            return retVal;
        }

        public ReadFromBlockResult call() throws Exception {
            return read();
        }

        @Override
        public synchronized int getId() {
            return mTaskId;
        }

        @Override
        public synchronized void cancelTask() {
            mCancelled = true;
            // Cleanup, only if not yet completed
            if (!mCompleted) {
                if (mInputStream != null) {
                    try {
                        mInputStream.close();
                    } catch (IOException e) {
                        LOG.error(
                                "Ignoring exception in closing cancelled file: " + mFilepath
                                        + " for block number " + mBlockNum + " throwing exception.",
                                e
                        );
                    }
                }
            }
        }

        @Override
        public RunnableFuture<ReadFromBlockResult> newTask() {
            return new FutureTaskWrapper<ReadFromBlockResult>(this) {

                @Override
                public boolean cancel(boolean mayInterruptIfRunning) {
                    //ReadFromABlock.this.cancelTask();
                    return super.cancel(mayInterruptIfRunning);
                }

                @Override
                public int getTaskId() {
                    return getId();
                }
            };
        }
    }

    public int getBlockSizeBytes(AlluxioURI filePath) throws Exception{
        return (int)mFileSystem.getStatus(filePath).getBlockSizeBytes();
    }

//    public int getKValueForEC(AlluxioURI filePath) throws Exception{
//        return mFileSystem.getStatus(filePath).getKValueForEC();
//    }

    private static ArrayBlockingQueue getFileNamequeue() throws IOException {
        ArrayBlockingQueue queue = new ArrayBlockingQueue(4096);
        String line;
        BufferedReader br = new BufferedReader(new FileReader(fileNamesPath));
        while ((line = br.readLine()) != null) {
            queue.add(line);
        }
        br.close();
        return queue;
    }

    private static ArrayBlockingQueue getReadTimequeue() throws IOException {
        ArrayBlockingQueue queue = new ArrayBlockingQueue(4096);
        String line;
        BufferedReader br = new BufferedReader(new FileReader(readTimesPath));
        while ((line = br.readLine()) != null) {
            queue.add(Long.parseLong(line));
        }
        br.close();
        return queue;
    }

    public static void main(String[] args) throws Exception{
//        int fileNum = Integer.parseInt(args[0]);
        fileNamequeue = getFileNamequeue();
        readTimequeue = getReadTimequeue();
//        new FileWriter(logDirPath + "/readLatency.txt", true).write("------------------------"); // append
//        new FileWriter(logDirPath + "/decodingLatency.txt", true).write("--------------------------");
        originTimeMs = CommonUtils.getCurrentMs();
            while (!fileNamequeue.isEmpty()){
                ECReadExecutor ecReadExecutor = new ECReadExecutor();
                FutureTask task = new FutureTask(ecReadExecutor);
                new Thread(task).start();
                sleep(new RandomUtils().nextLong(0, 100));
            }



//        mLog.close();
//        decodingLog.close();
//        String fileName = args[0];
//        AlluxioURI alluxioURI = new AlluxioURI(String.format("%s/%s", TEST_PATH, fileName));
//
//        ReadType readType = ReadType.CACHE;
//        int numAdditionalReads = 10; // 设置读取副本的线程上限
//        boolean skipDecoding = false;
//        int mBlockSizeBytes = ecReadExecutor.getBlockSizeBytes(alluxioURI);
//        int mKValueForEC = ecReadExecutor.getKValueForEC(alluxioURI);
//        byte[] data = new byte[mBlockSizeBytes*mKValueForEC];
//
//        long startTimeMs = CommonUtils.getCurrentMs();
//        int readByteSize = ecReadExecutor.readFileStragglerMitigate(readType, numAdditionalReads, alluxioURI, fileName, skipDecoding, data);
//        long endTimeMs = CommonUtils.getCurrentMs();
//        long tTimeTaken = endTimeMs - startTimeMs;
//        mLog.write("read file "+fileName+": should read bytes is "+mBlockSizeBytes*mKValueForEC +" truly get bytes is "+readByteSize+"\n");
//        mLog.write("ec Read " + fileName + " in " + tTimeTaken + " ms.\n");
//        mLog.flush();
//        mLog.close();
//        decodingLog.close();
//        return;
//        synchronized (mLog){
//            mLog.write(String.format("%s\t%s\n",fileName, tTimeTaken));
//        }
    }
}
