#include "merger_test.h"

START_TEST_DEFINE_PARENT(TestMergerBadIndex, TMergerTest, TTestMarksPool::OneBackendOnly)
bool Prepare() override {
    if (GetIsPrefixed())
        return true;
    else
        PrepareData("non_prefix/merge_bad_index");
    return true;
}
bool Run() override {
    if (GetIsPrefixed())
        return true;

    CheckMergerResult();
    CHECK_TEST_FAILED(QueryCount() != 112757, "Incorrect doc count after merge");
    return true;
}

bool InitConfig() override {
    (*ConfigDiff)["ShardsNumber"] = 1;
    (*ConfigDiff)["Components"] = "INDEX,MAKEUP,DDK,FASTARC";
    (*ConfigDiff)["ComponentsConfig.FULLARC.ActiveLayers"] = "base";
    (*ConfigDiff)["Indexer.Common.UseSlowUpdate"] = false;
    SetMergerParams(true, 1, -1, mcpNONE, -1, 10000000);
    return true;
}
};
