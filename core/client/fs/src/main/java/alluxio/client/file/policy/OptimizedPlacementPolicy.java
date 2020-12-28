package alluxio.client.file.policy;

import alluxio.AlluxioURI;
import alluxio.client.block.BlockWorkerInfo;
import alluxio.client.block.policy.BlockLocationPolicy;
import alluxio.client.block.policy.options.GetWorkerOptions;
import alluxio.wire.WorkerNetAddress;
import com.google.common.base.Objects;
import com.google.common.collect.Lists;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

public class OptimizedPlacementPolicy implements FileWriteLocationPolicy, BlockLocationPolicy {

    private String mLocalHostName;
    private int mIndex;
    private List<BlockWorkerInfo> mWorkerInfoList;
    private List<BlockWorkerInfo> mWorkersToWriteList;
    private boolean mInitialized = false;
    private final HashMap<Long, WorkerNetAddress> mBlockLocationCache = new HashMap<>();
    private AlluxioURI mFilePath;


    public OptimizedPlacementPolicy(){}
    public OptimizedPlacementPolicy(AlluxioURI mWritePath){
        mFilePath = mWritePath;
    }

    @Override
    public WorkerNetAddress getWorker(GetWorkerOptions options) {
        WorkerNetAddress address = mBlockLocationCache.get(options.getBlockId());
        if (address != null) {
            return address;
        }
        address = getWorkerForNextBlock(options.getBlockWorkerInfos(), options.getBlockSize());
        mBlockLocationCache.put(options.getBlockId(), address);
        return address;
    }

    @Override
    public WorkerNetAddress getWorkerForNextBlock(Iterable<BlockWorkerInfo> workerInfoList, long blockSizeBytes) {
        if(!mInitialized){
            mWorkerInfoList = Lists.newArrayList(workerInfoList);
            mWorkersToWriteList = workersToWrite(mWorkerInfoList, mFilePath.getName());
            mInitialized = true;
            mIndex = 0;
        }
        WorkerNetAddress candidate = mWorkersToWriteList.get(mIndex).getNetAddress();
        mIndex++;

        return candidate;
    }

    public List<BlockWorkerInfo> workersToWrite(List<BlockWorkerInfo> mWorkerInfoList, String filename){
        List<BlockWorkerInfo> workersToWriteList = new ArrayList<>();
        String workersToWritePath = System.getProperty("user.home") + "/ec_test_files/workersToWrite.txt";
        int fileId = Integer.parseInt(filename);
        String line;
        int lineCount = 0;

        mWorkerInfoList.sort(new Comparator<BlockWorkerInfo>() {

            public long ipToLong(String strIp) {
                String[]ip = strIp.split("\\.");
                return (Long.parseLong(ip[0]) << 24) +
                        (Long.parseLong(ip[1]) << 16) +
                        (Long.parseLong(ip[2]) << 8) +
                        Long.parseLong(ip[3]);
            }
            @Override
            public int compare(BlockWorkerInfo o1, BlockWorkerInfo o2) {
                String Name1 = o1.getNetAddress().getHost();
                String Name2 = o2.getNetAddress().getHost();

                InetAddress Host1 = null;
                InetAddress Host2 = null;
                try {
                    Host1 = InetAddress.getByName(Name1);
                    Host2 = InetAddress.getByName(Name2);
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                }
                String ip1 = Host1.getHostAddress();
                String ip2 = Host2.getHostAddress();

                long host1 = ipToLong(ip1);
                long host2 = ipToLong(ip2);
                return new Long(host1 - host2).intValue();
            }
        });

        try {
            BufferedReader br = new BufferedReader(new FileReader(workersToWritePath));
            while((line = br.readLine()) != null){
                if (lineCount == fileId){
                    break;
                }
                lineCount++;
            }
            String[] workers = line.split(" ");
            for(String worker : workers){
                workersToWriteList.add(mWorkerInfoList.get(Integer.parseInt(worker)));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return workersToWriteList;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof OptimizedPlacementPolicy)) {
            return false;
        }
        OptimizedPlacementPolicy that = (OptimizedPlacementPolicy) o;
        return Objects.equal(mLocalHostName, that.mLocalHostName)
                &&Objects.equal(mIndex, that.mIndex)
                &&Objects.equal(mWorkerInfoList, that.mWorkerInfoList)
                && Objects.equal(mInitialized, that.mInitialized)
                && Objects.equal(mBlockLocationCache, that.mBlockLocationCache);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(mLocalHostName, mIndex, mWorkerInfoList,
                 mInitialized, mBlockLocationCache);
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
                .add("mLocalHostName", mLocalHostName)
                .add("mIndex", mIndex)
                .add("mWorkerInfoList", mWorkerInfoList)
                .add("mInitialized", mInitialized)
                .add("mBlockLocationCache", mBlockLocationCache)
                .toString();
    }
}

