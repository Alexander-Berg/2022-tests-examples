#include <saas/rtyserver_test/testerlib/rtyserver_test.h>

START_TEST_DEFINE(TestUselessRearranges)
    bool Run() override {
        SetSearcherParams(abAUTO, "auto", "AntiDup");
        MUST_BE_BROKEN(ApplyConfig());
        return true;
    }
};

START_TEST_DEFINE(TestSearchProxyAntiDup)
bool Run() override {
    TVector<NRTYServer::TMessage> messages;
    bool prefixed = GetIsPrefixed();
    TString kps;
    GenerateInput(messages, 1, NRTYServer::TMessage::ADD_DOCUMENT, prefixed);
    if (prefixed) {
        messages[0].MutableDocument()->SetKeyPrefix(1);
        kps = "&kps=1";
    }

    IndexMessages(messages, DISK, 1);
    ReopenIndexers();

    IndexMessages(messages, DISK, 1);
    ReopenIndexers();

    TVector<TDocSearchInfo> results;
    QuerySearch("body" + kps, results, nullptr, nullptr, true);
    CHECK_TEST_EQ(results.size(), 1);
    return true;
}
bool InitConfig() override{
    (*SPConfigDiff)["SearchConfig.ReArrangeOptions"] = "AntiDup(CheckInGroups={GlueByUrl=1,RangeLimit=100,RmDupLimit=100})";
    return true;
}
};
