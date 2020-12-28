/*
 * The Alluxio Open Foundation licenses this work under the Apache License, version 2.0
 * (the "License"). You may not use this work except in compliance with the License, which is
 * available at www.apache.org/licenses/LICENSE-2.0
 *
 * This software is distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied, as more fully set forth in the License.
 *
 * See the NOTICE file distributed with this work for information regarding copyright ownership.
 */

package alluxio.worker.block.evictor;

import alluxio.Sessions;
import alluxio.StorageTierAssoc;
import alluxio.WorkerStorageTierAssoc;
import alluxio.collections.Pair;
import alluxio.exception.BlockDoesNotExistException;
import alluxio.worker.block.BlockMetadataManagerView;
import alluxio.worker.block.BlockStoreLocation;
import alluxio.worker.block.allocator.Allocator;
import alluxio.worker.block.meta.BlockMeta;
import alluxio.worker.block.meta.StorageDirView;
import alluxio.worker.block.meta.StorageTierView;

import com.google.common.base.Function;
import com.google.common.collect.Iterators;

import java.lang.reflect.Array;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicLong;

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;


@NotThreadSafe
public final class AffinityEvictor extends AbstractEvictor {

    private final StorageTierAssoc mStorageTierAssoc = new WorkerStorageTierAssoc();

    ;
    /**
     * [sjz] 数据块收益公式: B = P * C, P表示再访问概率 P= old P * e^(-a * (now - lastUpdateTime)),
     * C 表示收益率,由本层存储和上层存储的时延决定, C = upperTL - currentTL, TL = Size / BW + SeekLatency
     */

    /**
     * Map from block id to the last updated logic time count.
     */
    private final Map<Long, Long> mBlockIdToLastUpdateTime = new ConcurrentHashMap<>();

    // Map to record System time that block been access or commit lastly
//    private final Map<Long, Long> mBlockIdToLastSystemTime = new ConcurrentHashMap<>();

    // Map to record block size in MB
    private final Map<Long, Double> mBlockIdToSizeMB = new ConcurrentHashMap<>();

    // Map from block id to the P value of the block
    private final Map<Long, Double> mBlockIdToAccessTimes = new ConcurrentHashMap<>();

    // Map from block id to benefit ratio factor, because the BenefitRatio = P * ((1/BWcur - 1/BWupper) + 1/size * (SLcur - SLupper)),
    // so the Benefit Factor is (1/BWcur - 1/BWupper) + 1/size * (SLcur - SLupper)
    // Benefit Factor of MEM and SSD
    private final Map<Long, Double> mBlockIdToBFOfMAS = new ConcurrentHashMap<>();

    // Benefit Factor of SSD and HDD
    private final Map<Long, Double> mBlockIdToBFOfSAH = new ConcurrentHashMap<>();

    // Map from location to tier ordinal
    private final Map<Long, Integer> mBlockIdToTierOrdinal = new ConcurrentHashMap<>();

    // Map from Tier Ordinal to Sorted Map, which reduce the execution time when sort
    private final Map<Integer, HashMap<Long, Double>> mTierToMap = new ConcurrentHashMap<>();

    private final Map<Long, Long> firstAccessTime = new ConcurrentHashMap<>();

    private final Map<Long, Deque<Long>> accessRecord = new ConcurrentHashMap<>();

    // Bandwidth in MB of each storage tier
//    private final int BWOfMEM = 6656;
//
//    private final int BWOfSSD = {518, 966, 1843, 2662, 3276, 3788, 4300, 4505, 4505, 3788,
//            3481, 3481, 3481, 3481, 3481, 3481, 3481, 3379, 3072};
//
//    private final int[] BWOfHDD = {63, 104, 151, 224, 277, 352, 369, 407, 395, 403, 393};

    // set seek latency of HDD to 15 millseconds
//    private final double SLOfHDD = 0.015;

    // Euler
    private static double Euler = 2.718;

    // probility of re-access is equal to p(X+x)= S(X)* e^(-a * x) or +1
    private static double lambda = 0.35;

    private static double AFactor = 10e-3;

    // max size of block, 128MB
    private static double mMaxSize = 128 * 1024 * 1024;

    private static double K = 100;

    private static double alpha = -2;

    private static double predictTime = 100;

    private static int intervalCount = 50;

    public static double[] IOLoad;

    public static double[] storageLoad;

    /**
     * Logic time count.
     */
    private AtomicLong mLogicTimeCount = new AtomicLong(0L);

    /**
     * Creates a new instance of {@link LRFUEvictor}.
     *
     * @param view      a view of block metadata information
     * @param allocator an allocation policy
     */
    public AffinityEvictor(BlockMetadataManagerView view, Allocator allocator) {
        super(view, allocator);

        for (StorageTierView tier : mManagerView.getTierViews()) {
            // 将不同层的数据块Id和收益率映射信息分开保存;
            Integer tierOrdinal = tier.getTierViewOrdinal();

            HashMap<Long, Double> idToBR = new HashMap<>();
            for (StorageDirView dir : tier.getDirViews()) {
                // 遍历所有各层各目录可淘汰的数据块
                for (BlockMeta block : dir.getEvictableBlocks()) {
                    long blockId = block.getBlockId();
                    mBlockIdToLastUpdateTime.put(blockId, 0L);
                    mBlockIdToAccessTimes.put(blockId, 1.0);
                    mBlockIdToSizeMB.put(blockId, Double.valueOf(block.getBlockSize() / (1024 * 1024)));
                    mBlockIdToTierOrdinal.put(blockId, tierOrdinal);
                    mBlockIdToBFOfMAS.put(
                            blockId,
                            getBenefitFactor(
                                    blockId,
                                    "MAS"
                            )
                    );
                    mBlockIdToBFOfSAH.put(
                            blockId,
                            getBenefitFactor(
                                    blockId,
                                    "SAH"
                            )
                    );
                    if (tier.getTierViewOrdinal() == 0) {
                        // 0层
                        idToBR.put(blockId, mBlockIdToBFOfMAS.get(blockId) * calculateP(storageLoad[0], IOLoad[0], "P"));
                    } else {
                        // 1或2层
                        idToBR.put(blockId, mBlockIdToBFOfSAH.get(blockId) * calculateP(storageLoad[1], IOLoad[1], "E"));
                    }
                }
            }
            mTierToMap.put(tierOrdinal, idToBR);
        }
    }

    /**
     * BenefitFactor = (1/BWcur - 1/BWupper) + 1/Size * (SLcur - SLupper)]
     *
     * @param Mode
     * @return BenefitRatio, which update when the P value changed
     */
//    private double getBenefitFactor(double blockSize, String Mode) {
//        if (Mode.equals("MAS")) {
//            // when mode is 0, the benefit ratio is between MEM and SSD
//            return getBenefitFactor(blockSize, BWOfMEM, BWOfSSD, 0, 0);
//        }
//        if (Mode.equals("SAH")) {
//            return getBenefitFactor(blockSize, BWOfSSD, BWOfHDD, 0, SLOfHDD);
//        }
//        return 0;
//    }
    private double getBenefitFactor(
            long blockId,
            String Tier
    ) {
        double blockSize = mBlockIdToSizeMB.get(blockId);
        int bandWidth = MyEvictorUtils.getBandWidth(blockSize, Tier);
//        int isRedundency = isRedundency(blockId);
        double accessTimes;


        accessTimes = mBlockIdToAccessTimes.get(blockId);
        return blockSize * (bandWidth * accessTimes);
//        return blockSize * (bandWidth * accessTimes + alpha * isRedundency);
    }

    private double calculateAccessTimes(double dbRt[]) {
        double weight = dbRt[0];
        double bias = dbRt[1];
        long currentTime = mLogicTimeCount.incrementAndGet();
        return ((currentTime + 0.5 * predictTime) * weight + bias) * predictTime;
    }

    public static double calculateP(double storageLoad, double ioLoad, String Mode) {
        double utilization = Math.max(storageLoad, ioLoad);
        if (Mode.equals("P")) {
            return 1 - (Math.pow(K, utilization) - 1) / (K - 1);
        } else {
            return (Math.pow(K, utilization) - 1) / (K - 1);
        }
    }


    @Nullable
    @Override
    // [sjz]
    public EvictionPlan freeSpaceWithView(
            long bytesToBeAvailable, BlockStoreLocation location,
            BlockMetadataManagerView view, Mode mode
    ) {
        synchronized (mBlockIdToLastUpdateTime) {
            updatePValueWithoutAccess();
            mManagerView = view;
            List<BlockTransferInfo> toMove = new ArrayList<>();
            List<Pair<Long, BlockStoreLocation>> toEvict = new ArrayList<>();
            EvictionPlan plan = new EvictionPlan(toMove, toEvict);
            StorageDirView candidateDir = cascadingEvict(bytesToBeAvailable, location, plan, mode);
            mManagerView.clearBlockMarks();
            if (candidateDir == null) {
                return null;
            }
            return plan;
        }
    }

    @Override
    protected StorageDirView cascadingEvict(
            long bytesToBeAvailable, BlockStoreLocation location,
            EvictionPlan plan, Mode mode
    ) {
        location = updateBlockStoreLocation(bytesToBeAvailable, location);

        // 1. If bytesToBeAvailable can already be satisfied without eviction, return the eligible
        // StoargeDirView
        StorageDirView candidateDirView =
                EvictorUtils.selectDirWithRequestedSpace(
                        bytesToBeAvailable,
                        location,
                        mManagerView
                );
        if (candidateDirView != null) {
            return candidateDirView;
        }

        // 2. Iterate over blocks in order until we find a StorageDirView that is in the range of
        // location and can satisfy bytesToBeAvailable after evicting its blocks iterated so far
        EvictionDirCandidates dirCandidates = new EvictionDirCandidates();
        // 相比AbstractEvictor类cascadingEvict替换了迭代器函数
        Iterator<Long> it = getBlockIterator(location);
        while (it.hasNext() && dirCandidates.candidateSize() < bytesToBeAvailable) {
            long blockId = it.next();
            try {
                BlockMeta block = mManagerView.getBlockMeta(blockId);
                if (block != null) { // might not present in this view
                    if (block.getBlockLocation().belongsTo(location)) {
                        String tierAlias = block.getParentDir().getParentTier().getTierAlias();
                        int dirIndex = block.getParentDir().getDirIndex();
                        dirCandidates.add(
                                mManagerView.getTierView(tierAlias).getDirView(dirIndex),
                                blockId,
                                block.getBlockSize()
                        );
                    }
                }
            } catch (BlockDoesNotExistException e) {
                it.remove();
                onRemoveBlockFromIterator(blockId);
            }
        }

        // 3. If there is no eligible StorageDirView, return null
        if (mode == Mode.GUARANTEED && dirCandidates.candidateSize() < bytesToBeAvailable) {
            return null;
        }

        // 4. cascading eviction: try to allocate space in the next tier to move candidate blocks
        // there. If allocation fails, the next tier will continue to evict its blocks to free space.
        // Blocks are only evicted from the last tier or it can not be moved to the next tier.
        candidateDirView = dirCandidates.candidateDir();
        if (candidateDirView == null) {
            return null;
        }
        List<Long> candidateBlocks = dirCandidates.candidateBlocks();
        StorageTierView nextTierView = mManagerView.getNextTier(candidateDirView.getParentTierView());
        if (nextTierView == null) {
            // This is the last tier, evict all the blocks.
            for (Long blockId : candidateBlocks) {
                try {
                    BlockMeta block = mManagerView.getBlockMeta(blockId);
                    if (block != null) {
                        candidateDirView.markBlockMoveOut(blockId, block.getBlockSize());
                        plan.toEvict().add(new Pair<>(
                                blockId,
                                candidateDirView.toBlockStoreLocation()
                        ));
                    }
                } catch (BlockDoesNotExistException e) {
                    continue;
                }
            }
        } else {
            for (Long blockId : candidateBlocks) {
                try {
                    BlockMeta block = mManagerView.getBlockMeta(blockId);
                    if (block == null) {
                        continue;
                    }
                    StorageDirView nextDirView = mAllocator.allocateBlockWithView(
                            Sessions.MIGRATE_DATA_SESSION_ID,
                            block.getBlockSize(),
                            BlockStoreLocation.anyDirInTier(nextTierView.getTierViewAlias()),
                            mManagerView
                    );
                    if (nextDirView == null) {
                        nextDirView = cascadingEvict(
                                block.getBlockSize(),
                                BlockStoreLocation.anyDirInTier(nextTierView.getTierViewAlias()),
                                plan,
                                mode
                        );
                    }
                    if (nextDirView == null) {
                        // If we failed to find a dir in the next tier to move this block, evict it and
                        // continue. Normally this should not happen.
                        plan.toEvict().add(new Pair<>(blockId, block.getBlockLocation()));
                        candidateDirView.markBlockMoveOut(blockId, block.getBlockSize());
                        continue;
                    }
                    plan.toMove().add(new BlockTransferInfo(blockId, block.getBlockLocation(),
                                                            nextDirView.toBlockStoreLocation()
                    ));
                    candidateDirView.markBlockMoveOut(blockId, block.getBlockSize());
                    nextDirView.markBlockMoveIn(blockId, block.getBlockSize());
                } catch (BlockDoesNotExistException e) {
                    continue;
                }
            }
        }

        return candidateDirView;
    }

    @Override
    protected Iterator<Long> getBlockIterator() {
        List<Map.Entry<Long, Double>> globalList = new ArrayList<>();
        for (int i = 0; i < mStorageTierAssoc.size(); i++) {
            List<Map.Entry<Long, Double>> list = new ArrayList<Entry<Long, Double>>(mTierToMap.get(i).entrySet());
            Collections.sort(list, new Comparator<Entry<Long, Double>>() {
                @Override
                public int compare(Entry<Long, Double> o1, Entry<Long, Double> o2) {
                    // 正序排列
                    if (o1.getValue() < o2.getValue()) {
                        return -1;
                    } else {
                        return 1;
                    }
                }
            });
            globalList.addAll(list);
        }

        return Iterators.transform(
                globalList.iterator(),
                new Function<Map.Entry<Long, Double>, Long>() {
                    @Override
                    //Entry<Long blockId, Double CRFValue>
                    public Long apply(Entry<Long, Double> input) {
                        return input.getKey();
                    }
                }
        );
    }

    /**
     * [sjz] 保留原有迭代器,设计了基于分层的迭代器,降低排序开销
     */
    protected Iterator<Long> getBlockIterator(BlockStoreLocation location) {
        HashMap map = mTierToMap.get(mStorageTierAssoc.getOrdinal(location.tierAlias()));
        List<Map.Entry<Long, Double>> list = new ArrayList<Entry<Long, Double>>(map.entrySet());
        Collections.sort(list, new Comparator<Entry<Long, Double>>() {
            @Override
            public int compare(Entry<Long, Double> o1, Entry<Long, Double> o2) {
                if (o1.getValue() < o2.getValue()) {
                    return -1;
                } else {
                    return 1;
                }
            }
        });
//        for(Entry<Long, Double> entry : list){
//            System.out.println(entry.getKey());
//        }
        return Iterators.transform(list.iterator(), new Function<Entry<Long, Double>, Long>() {
            @Nullable
            @Override
            public Long apply(@Nullable Entry<Long, Double> input) {
                return input.getKey();
            }
        });
    }


    @Override
    public void onAccessBlock(long userId, long blockId) {
        updateOnAccess(blockId);
    }

    private void updateOnAccess(long blockId) {
        synchronized (mBlockIdToLastUpdateTime) {
            mLogicTimeCount.incrementAndGet();
            updatePValueWithAccess(blockId);
            updateAccessRecord(blockId);
            IOLoad = MyEvictorUtils.updateIOLoad();
            storageLoad = MyEvictorUtils.updateStorageLoad();
        }
    }

    @Override
    public void onCommitBlock(long userId, long blockId, BlockStoreLocation location) {
        updateOnCommit(blockId);
    }

    private void updateOnCommit(long blockId) {
        synchronized (mBlockIdToLastUpdateTime) {
            mLogicTimeCount.incrementAndGet();
            long currentLogicTime = mLogicTimeCount.incrementAndGet();

            // if mBlockIdToSize contain the blockId, then do nothing, else get the block size
            if (mBlockIdToSizeMB.containsKey(blockId)) {
                // do nothing
            } else {
                for (StorageTierView tier : mManagerView.getTierViews()) {
                    for (StorageDirView dir : tier.getDirViews()) {
                        for (BlockMeta block : dir.getEvictableBlocks()) {
                            if (block.getBlockId() == blockId) {
                                // size in MB
                                mBlockIdToSizeMB.put(blockId, Double.valueOf(block.getBlockSize() / (1024 * 1024)));
                                mBlockIdToTierOrdinal.put(blockId, tier.getTierViewOrdinal());
                                mBlockIdToAccessTimes.put(blockId, 1.0);
                                mBlockIdToLastUpdateTime.put(blockId, currentLogicTime);
                                mBlockIdToBFOfMAS.put(blockId, getBenefitFactor(blockId, "MAS"));
                                mBlockIdToBFOfSAH.put(blockId, getBenefitFactor(blockId, "SAH"));
                                HashMap<Long, Double> map = null;
                                int tierOrdinal = tier.getTierViewOrdinal();
                                if (mTierToMap.containsKey(tierOrdinal)) {
                                    map = mTierToMap.get(tierOrdinal);
                                } else {
                                    map = new HashMap<Long, Double>();
                                    mTierToMap.put(tierOrdinal, map);
                                }

                                if (mBlockIdToTierOrdinal.get(blockId) == 0) {
                                    map.put(blockId, mBlockIdToAccessTimes.get(blockId) * mBlockIdToBFOfMAS.get(blockId));
                                } else {
                                    map.put(blockId, mBlockIdToAccessTimes.get(blockId) * mBlockIdToBFOfSAH.get(blockId));
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * [sjz] 检查从上层替换数据块是否增加上层存储收益
     *
     * @param blockId
     * @param location 待移动数据块的迁移目标位置
     * @return true表示增加收益, 迁移合法;false表示无法增加,迁移不合法
     */
//    @Override
//    public boolean checkWithKnapsackProblem(long blockId, BlockStoreLocation location) {
//        updatePAndBR(location);
//        int tierOrdinalOfBlock = mBlockIdToTierOrdinal.get(blockId);
//        double promoteBenefit = 0;
//        double promoteSize = mBlockIdToSizeMB.get(blockId);
//        double evictBenefit = 0;
//        int tierOrdinal = mStorageTierAssoc.getOrdinal(location.tierAlias());
//        HashMap map = mTierToMap.get(tierOrdinal);
//
//        double availableSpace = (double) EvictorUtils.getDirWithMaxFreeSpace(
//                (long) (promoteSize * 1024 * 1024),
//                location,
//                mManagerView
//        ).getAvailableBytes() / (1024 * 1024);
//        System.out.println("init available space" + availableSpace);
//        if (tierOrdinalOfBlock == 0) {
//            // 待移动数据块在0层
//            return false;
//        }
//        promoteBenefit = mBlockIdToSizeMB.get(blockId) * mBlockIdToAccessTimes.get(blockId) * (tierOrdinalOfBlock == 1 ? mBlockIdToBFOfMAS.get(
//                blockId) : mBlockIdToBFOfSAH.get(blockId));
//        Iterator<Long> it = getBlockIterator(location);
//        //[sjz] evictBenefit < promoteBenefit && availableSpace > promoteSize 时迁移合法,否则不合法
//        while (evictBenefit < promoteBenefit) {
//            if (availableSpace > promoteSize) {
//                return true;
//            }
//            if (it.hasNext()) {
//                long evictId = it.next();
//                availableSpace += mBlockIdToSizeMB.get(evictId);
//                if (map.containsKey(evictId)) {
//                    evictBenefit += (double) map.get(evictId) * mBlockIdToSizeMB.get(evictId) * mBlockIdToAccessTimes.get(evictId);
//                }
//            } else {
//                return false;
//            }
//        }
//        System.out.println("false! e:" + evictBenefit + " p:" + promoteBenefit);
//        return false;
//    }

    private void updatePAndBR(BlockStoreLocation location) {
        synchronized (mBlockIdToLastUpdateTime) {
            updatePValueWithoutAccess();
            int tierOrdinal = mStorageTierAssoc.getOrdinal(location.tierAlias());
            HashMap<Long, Double> map = mTierToMap.get(tierOrdinal);
            if (tierOrdinal == 0) {
                // 0 层
                for (Map.Entry<Long, Double> entry : map.entrySet()) {
                    long blockId = entry.getKey();
                    map.put(
                            blockId,
                            mBlockIdToAccessTimes.get(blockId) * mBlockIdToBFOfMAS.get(blockId)
                    );
                }
            } else {
                // 1 or 2层
                for (Map.Entry<Long, Double> entry : map.entrySet()) {
                    long blockId = entry.getKey();
                    map.put(
                            blockId,
                            mBlockIdToAccessTimes.get(blockId) * mBlockIdToBFOfSAH.get(blockId)
                    );
                }
            }
        }
    }

    @Override
    public void onRemoveBlockByClient(long userId, long blockId) {
        updateOnRemoveBlock(blockId);
    }

    @Override
    public void onRemoveBlockByWorker(long userId, long blockId) {
        updateOnRemoveBlock(blockId);
    }

    @Override
    protected void onRemoveBlockFromIterator(long blockId) {
        mBlockIdToLastUpdateTime.remove(blockId);
        mBlockIdToAccessTimes.remove(blockId);
        mBlockIdToSizeMB.remove(blockId);
        mTierToMap.get(mBlockIdToTierOrdinal.get(blockId)).remove(blockId);
        mBlockIdToBFOfMAS.remove(blockId);
        mBlockIdToBFOfSAH.remove(blockId);
        mBlockIdToTierOrdinal.remove(blockId);
    }

    @Override
    public void onMoveBlockByClient(
            long sessionId, long blockId, BlockStoreLocation oldLocation,
            BlockStoreLocation newLocation
    ) {
        updateOnMove(blockId, oldLocation, newLocation);
    }

    @Override
    public void onMoveBlockByWorker(
            long sessionId, long blockId, BlockStoreLocation oldLocation,
            BlockStoreLocation newLocation
    ) {
        updateOnMove(blockId, oldLocation, newLocation);
    }

    //[sjz] 数据块迁移须更新mLocationToMap
    private void updateOnMove(
            long blockId, BlockStoreLocation oldLocation,
            BlockStoreLocation newLocation
    ) {
        synchronized (mBlockIdToLastUpdateTime) {
            mLogicTimeCount.incrementAndGet();
            int oldTierOrdinal = mStorageTierAssoc.getOrdinal(oldLocation.tierAlias());
            int newTierOrdinal = mStorageTierAssoc.getOrdinal(newLocation.tierAlias());
            mBlockIdToTierOrdinal.put(blockId, newTierOrdinal);
            if (oldTierOrdinal != newTierOrdinal) {
                mTierToMap.get(oldTierOrdinal).remove(blockId);
                if (newTierOrdinal == 0) {
                    mTierToMap.get(newTierOrdinal).put(blockId, calculateP(storageLoad[0], IOLoad[0], "P") * mBlockIdToBFOfMAS.get(blockId));
                } else {
                    mTierToMap.get(newTierOrdinal).put(blockId, calculateP(storageLoad[1], IOLoad[1], "E") * mBlockIdToBFOfSAH.get(blockId));
                }
            }
        }
    }

    private void updatePValueWithoutAccess() {
        long currentLogicTime = mLogicTimeCount.get();
        for (Entry<Long, Double> entry : mBlockIdToAccessTimes.entrySet()) {
            long blockId = entry.getKey();
            double PValue = entry.getValue();
            mBlockIdToAccessTimes.put(
                    blockId, calculateAccessTimes(
                            MyEvictorUtils.getLinePara(MyEvictorUtils.calculateAccessDense(
                                    accessRecord.get(blockId), intervalCount))));
            mBlockIdToLastUpdateTime.put(blockId, currentLogicTime);
        }
    }

    private void updatePValueWithAccess(Long blockId) {
        long currentLogicTime = mLogicTimeCount.get();
        if (mBlockIdToAccessTimes.containsKey(blockId)) {
            double PValue = mBlockIdToAccessTimes.get(blockId);
            mBlockIdToAccessTimes.put(
                    blockId, calculateAccessTimes(
                            MyEvictorUtils.getLinePara(MyEvictorUtils.calculateAccessDense(
                                    accessRecord.get(blockId), intervalCount))) + 1.0);
            mBlockIdToLastUpdateTime.put(blockId, currentLogicTime);
        } else {
            mBlockIdToAccessTimes.put(blockId, 1.0);
        }
    }

    private void updateAccessRecord(Long blockId){
        long newAccess = mLogicTimeCount.incrementAndGet();
        Deque<Long> currentRecord = accessRecord.containsKey(blockId) ? accessRecord.get(blockId) : new ConcurrentLinkedDeque<>();
        currentRecord.addLast(newAccess);
        while(currentRecord.getFirst()<newAccess-predictTime){
            currentRecord.removeFirst();
        }
        accessRecord.put(blockId, currentRecord);
    }

    /**
     * Updates {@link #mBlockIdToLastUpdateTime}  when block is
     * removed.
     *
     * @param blockId id of the block to be removed
     */
    private void updateOnRemoveBlock(long blockId) {
        synchronized (mBlockIdToLastUpdateTime) {
            mLogicTimeCount.incrementAndGet();
            mBlockIdToAccessTimes.remove(blockId);
            mBlockIdToLastUpdateTime.remove(blockId);
            mBlockIdToSizeMB.remove(blockId);
        }
    }
}
