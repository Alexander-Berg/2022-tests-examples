/*
 * Copyright © 2016 YANDEX. All rights reserved.
 */

package com.yandex.datasync.internal.operation.network.sync.merge;

import com.yandex.datasync.MergeAtomSize;
import com.yandex.datasync.MergeWinner;
import com.yandex.datasync.YDSContext;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertThat;

public class MergerBuilderTest {

    public static final YDSContext MOCK_CONTEXT = YDSContext.APP;

    public static final String MOCK_DATABASE_ID = "user_schedule";

    private MergerBuilder mergerBuilder;

    @SuppressWarnings("ConstantConditions")
    @Before
    public void setUp() {
        mergerBuilder = new MergerBuilder(MOCK_CONTEXT, MOCK_DATABASE_ID, null, null, true);
    }

    @Test
    public void testBuildTheirsValue() throws Exception {
        mergerBuilder.withMergeWinner(MergeWinner.THEIRS).withAtomSize(MergeAtomSize.VALUE);
        assertThat(mergerBuilder.build(), instanceOf(AcceptTheirsValueMergeStrategy.class));
    }

    @Test
    public void testBuildMineValue() throws Exception {
        mergerBuilder.withMergeWinner(MergeWinner.MINE).withAtomSize(MergeAtomSize.VALUE);
        assertThat(mergerBuilder.build(), instanceOf(AcceptMineValueMergeStrategy.class));
    }

    @Test
    public void testBuildMineRecord() throws Exception {
        mergerBuilder.withMergeWinner(MergeWinner.MINE).withAtomSize(MergeAtomSize.RECORD);
        assertThat(mergerBuilder.build(), instanceOf(AcceptMineRecordMergeStrategy.class));
    }

    @Test
    public void testBuildTheirsRecord() throws Exception {
        mergerBuilder.withMergeWinner(MergeWinner.THEIRS).withAtomSize(MergeAtomSize.RECORD);
        assertThat(mergerBuilder.build(), instanceOf(AcceptTheirsRecordMergeStrategy.class));
    }

    @Test
    public void testBuildMineCollection() throws Exception {
        mergerBuilder.withMergeWinner(MergeWinner.MINE).withAtomSize(MergeAtomSize.COLLECTION);
        assertThat(mergerBuilder.build(), instanceOf(AcceptMineCollectionsMergeStrategy.class));
    }

    @Test
    public void testBuildTheirsCollection() throws Exception {
        mergerBuilder.withMergeWinner(MergeWinner.THEIRS).withAtomSize(MergeAtomSize.COLLECTION);
        assertThat(mergerBuilder.build(), instanceOf(AcceptTheirsCollectionsMergeStrategy.class));
    }
}