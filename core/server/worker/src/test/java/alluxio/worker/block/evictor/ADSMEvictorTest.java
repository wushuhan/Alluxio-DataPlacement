package alluxio.worker.block.evictor;

import alluxio.Configuration;
import alluxio.PropertyKey;
import alluxio.worker.block.ADSMTestUtils;
import alluxio.worker.block.BlockMetadataManager;
import alluxio.worker.block.BlockMetadataManagerView;
import alluxio.worker.block.BlockStoreEventListener;
import alluxio.worker.block.BlockStoreLocation;
import alluxio.worker.block.allocator.Allocator;
import alluxio.worker.block.allocator.MaxFreeAllocator;
import alluxio.worker.block.meta.StorageDir;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

public class ADSMEvictorTest {

    private static final long SESSION_ID = 2;

    private static final long BLOCK_ID = 10;

    private BlockMetadataManager mMetaManager;

    private BlockMetadataManagerView mManagerView;

    private Evictor mEvictor;

    private double mStepFactor;

    private double mAttenuationFactor;

    private final Map<Long, Double> mBlockIdToSizeMB = new ConcurrentHashMap<>();

    // Map from block id to the P value of the block
    private final Map<Long, Double> mBlockIdToPValue = new ConcurrentHashMap<>();

    // Map from block id to benefit ratio (BR = P * C / Size) of MEM and SSD
    private final Map<Long, Double> mBlockIdToBROfMAS = new ConcurrentHashMap<>();

    // Map from block id to benefit ratio of SSD and HDD
    private final Map<Long, Double> mBlockIdToBROfSAH = new ConcurrentHashMap<>();

    // Map from location to tier ordinal
    private final Map<Long, Integer> mBlockIdToTierOrdinal = new ConcurrentHashMap<>();

    // Map from location to Sorted Map, which reduce the execution time when sort
    private final Map<BlockStoreLocation, HashMap<Long, Double>> mLocationToMap = new ConcurrentHashMap<>();

    // Bandwidth in MB of each storage tier
    private final int BWOfMEM = 1100;

    private final int BWOfSSD = 750;

    private final int BWOfHDD = 550;

    // set seek latency of HDD to 15 millseconds
    private final double SLOfHDD = 0.015;

    /**
     * Rule to create a new temporary folder during each test.
     */
    @Rule
    public TemporaryFolder mTestFolder = new TemporaryFolder();

    @Before
    public final void before() throws Exception {
        File tempFolder = mTestFolder.newFolder();
        mMetaManager = ADSMTestUtils.defaultMetadataManager(tempFolder.getAbsolutePath());
        mManagerView =
                new BlockMetadataManagerView(mMetaManager, Collections.<Long>emptySet(),
                                             Collections.<Long>emptySet()
                );
        Configuration.set(PropertyKey.WORKER_EVICTOR_CLASS, ADSMEvictor.class.getName());
        Configuration.set(PropertyKey.WORKER_ALLOCATOR_CLASS, MaxFreeAllocator.class.getName());
        Allocator allocator = Allocator.Factory.create(mManagerView);
        mEvictor = Evictor.Factory.create(mManagerView, allocator);
    }

    private void cache(long sessionId, long blockId, long bytes, int tierLevel, int dirIdx)
            throws Exception {
        StorageDir dir = mMetaManager.getTiers().get(tierLevel).getDir(dirIdx);
        ADSMTestUtils.cache(sessionId, blockId, bytes, dir, mMetaManager, mEvictor);
    }

    /**
     * Access the block to update {@link Evictor}.
     */
    private void access(long blockId) {
        ((BlockStoreEventListener) mEvictor).onAccessBlock(SESSION_ID, blockId);
    }

    @Test
    public void evictInBottomTier() throws Exception {
//        int bottomTierOrdinal = ADSMTestUtils
//                .TIER_ORDINAL[ADSMTestUtils.TIER_ORDINAL.length - 1];
        int SSDTierOrdinal = 1;
        int HDDTierOrdinal = 2;
        Map<Long, Double> blockIdToCRF = new HashMap<>();
        // capacity increases with index
        long[] bottomTierDirCapacity = ADSMTestUtils.TIER_CAPACITY_BYTES[SSDTierOrdinal];
        int nDir = bottomTierDirCapacity.length;
        // fill in dirs from larger to smaller capacity with blockId equal to BLOCK_ID plus dir index
        cache(SESSION_ID, 1, 2 * 1024 * 1024, SSDTierOrdinal, 0);
        cache(SESSION_ID, 2, 4 * 1024 * 1024, SSDTierOrdinal, 0);
        cache(SESSION_ID, 3, 3 * 1024 * 1024, SSDTierOrdinal, 0);

        cache(SESSION_ID, 4, 1 * 1024 * 1024, HDDTierOrdinal, 0);
        BlockStoreLocation anyDirInSSDTier =
                BlockStoreLocation.anyDirInTier(ADSMTestUtils.TIER_ALIAS[SSDTierOrdinal]);
        // 检查是否符合背包思想
        System.out.println(mEvictor.checkWithKnapsackProblem(4, anyDirInSSDTier));
        EvictionPlan plan = mEvictor.freeSpaceWithView(6 * 1024 * 1024, anyDirInSSDTier, mManagerView);
//        Assert.assertNotNull(plan);
        System.out.println(plan.toMove().size());
        System.out.println(plan.toMove().get(0).getBlockId());
        System.out.println(plan.toMove().get(1).getBlockId());
    }
}
