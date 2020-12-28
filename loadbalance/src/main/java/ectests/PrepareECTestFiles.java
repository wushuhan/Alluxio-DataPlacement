package ectests;

import alluxio.AlluxioURI;
import alluxio.client.file.FileSystem;
import alluxio.client.file.options.DeleteOptions;
import alluxio.examples.BasicOperations;
import alluxio.exception.AlluxioException;
import alluxio.util.CommonUtils;
import alluxio.util.FormatUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import selectpart.ECFileWriter;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;


public class PrepareECTestFiles {

    private static final Logger LOG = LoggerFactory.getLogger(BasicOperations.class);

    public static final String TEST_PATH = "/ec_tests_master"; //Directory in Alluxio for the test files.

    private final String mLocalFile = System.getProperty("user.home") + "/ec_test_files/test_local_file";


    private final ArrayList<Integer> mKValues = new ArrayList<>();

    private final ArrayList<Integer> mNValues = new ArrayList<>();

    private final ArrayList<Integer> mFileSizes = new ArrayList<>();

    public static boolean isOpt;

//    private ArrayList<AlluxioURI> mURIList = new ArrayList<AlluxioURI>();

    public PrepareECTestFiles() throws IOException {

        // read KVALUE data
        // k values
        String KValuePath = System.getProperty("user.home") + "/ec_test_files/k.txt";
        System.out.println("K values path: " + KValuePath);
        String line;
        BufferedReader br = new BufferedReader(new FileReader(KValuePath));
        while ((line = br.readLine()) != null) {
            mKValues.add(Integer.parseInt(line));
        }
        br.close();
        // read NVALUE data
        // n values
        String NValuePath = System.getProperty("user.home") + "/ec_test_files/n.txt";
        System.out.println("N values path: " + NValuePath);
        br = new BufferedReader(new FileReader(NValuePath));
        while ((line = br.readLine()) != null) {
            mNValues.add(Integer.parseInt(line));
        }
        br.close();
        //read fileSize data
        //file sizes
        String fileSizePath = System.getProperty("user.home") + "/ec_test_files/fileSize.txt";
        System.out.println("fileSize data path: " + fileSizePath);
        br = new BufferedReader(new FileReader(fileSizePath));
        while ((line = br.readLine()) != null) {
            mFileSizes.add(Integer.parseInt(line));
        }
        br.close();
    }

    private void writeFiles(boolean isOpt) throws Exception {


        //generate the test files given kValues.
        AlluxioURI testDir = new AlluxioURI(TEST_PATH);
        FileSystem fs = FileSystem.Factory.get();
        if (fs.exists(testDir)) {
            fs.delete(testDir, DeleteOptions.defaults().setRecursive(true).setUnchecked(true));
        }
//        FileInputStream is = new FileInputStream(mLocalFile);
//        int tFileLength = (int) new File(mLocalFile).length();
//        byte[] tBuf = new byte[tFileLength];
//        // read file from local file system to byteBuffer
//        int tBytesRead = is.read(tBuf);
//        is.close();
        for (int fileIndex = 0; fileIndex < mKValues.size(); fileIndex++) {
            writeFile(fileIndex, isOpt);
        }
//        fs.getK();
    }

    public void writeFile(int fileIndex, boolean isOpt) throws IOException, AlluxioException {
        FileSystem fs = FileSystem.Factory.get();
        int tFileLength = mFileSizes.get(fileIndex);
        byte[] tBuf = new byte[tFileLength];
//            Arrays.fill(tBuf, (byte) 0);
        tBuf[tFileLength - 1] = '\0';
        AlluxioURI filePath = new AlluxioURI(String.format("%s/%s", TEST_PATH, fileIndex));
        //copyFromLocal(filePath, mKValues.get(fileIndex), fs, mLocalFile);
        copyFromBuffer(filePath, mKValues.get(fileIndex), mNValues.get(fileIndex), fs ,isOpt ,tBuf);
    }

    // First load local file in memory, then write the file into Alluxio
//    private void copyFromLocal(AlluxioURI writePath, int k, FileSystem fileSystem, String localFile)
//            throws IOException, AlluxioException {
//
//        LOG.debug("Loading data into memory...");
//        // Read file into memory
//        FileInputStream is = new FileInputStream(localFile);
//        int tFileLength = (int) new File(localFile).length();
//        byte[] tBuf = new byte[tFileLength];
//        // The total file is read into the buffer.
//        int tBytesRead = is.read(tBuf);
//        LOG.info("Read local file into memory with bytes:" + tBytesRead);
//        // Then write the file from memory into Alluxio.
//        SPFileWriter tSPWriter = new SPFileWriter(k, tFileLength, fileSystem, writePath);
//        long tStartTimeMs = CommonUtils.getCurrentMs();
//        tSPWriter.writeFile(tBuf);
//        is.close();
//        LOG.info(FormatUtils.formatTimeTakenMs(
//                tStartTimeMs,
//                "writing " + localFile + " into Alluxio path " + writePath
//        ));
//    }

    // Write the file with an in-memory byte buffer (should be faster than copying local file on disk)
    private void copyFromBuffer(AlluxioURI writePath, int k, int n, FileSystem fileSystem, boolean isOpt, byte[] buf) {
        long tStartTimeMs = CommonUtils.getCurrentMs();
        int tFileLength = buf.length;

        ECFileWriter tECWriter = new ECFileWriter(k, n, tFileLength, fileSystem, writePath, isOpt);
        tECWriter.writeFile(buf);
        LOG.info(FormatUtils.formatTimeTakenMs(
                tStartTimeMs,
                "writing" + writePath
        ));
    }

    public static void main(String[] args) throws Exception {
        PrepareECTestFiles operator;
        isOpt = Boolean.parseBoolean(args[0]);
        operator = new PrepareECTestFiles();
        operator.writeFiles(isOpt);
        System.exit(0);
    }
}
