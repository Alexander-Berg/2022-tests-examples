#include "merger_test.h"

#include <saas/rtyserver_test/testerlib/rtyserver_test.h>


START_TEST_DEFINE_PARENT(TestMergerStatic, TMergerTest, TTestMarksPool::StaticData,
                            TTestMarksPool::Merger, TTestMarksPool::OneBackendOnly)
    bool Prepare() override {
        if (GetIsPrefixed())
            PrepareData("prefix/merge/1");
        return true;
    }

    bool Run() override {
        if (!GetIsPrefixed())
            return true;
        CheckMergerResult();
        return true;
    }
public:
    bool InitConfig() override {
        TMergerTest::InitConfig();
        SetMergerParams(true, -1, -1, mcpNONE);
        (*ConfigDiff)["Indexer.Common.UseSlowUpdate"] = 0;
        (*ConfigDiff)["Components"] = "INDEX,MAKEUP,DDK,FASTARC";
        (*ConfigDiff)["ComponentsConfig.FULLARC.ActiveLayers"] = "base";
        return true;
    }
};
