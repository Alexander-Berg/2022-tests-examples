#include <saas/rtyserver_test/testerlib/rtyserver_test.h>


// request tree for "body"
constexpr auto canonicRequestTree = "cHicXY-tqgJRFEa_78yAx8EwaJGTZIqDaTCJScRgFMPlMkmG"
                                    "EbUoaFGTUYsINpMIVvEn324Un-Q-gsczWNxpsxdrwXZ-nIyE"
                                    "i7wswBcBsnY0imcKHkooo5qRtmbQDAFqaKKFX3TQ_18_z3JD"
                                    "7IgDjXIl_gg9d-LBOECF9SMlXSjDPfgM2NoK43Ncc8w5z3e5"
                                    "gjaXoTieorRLZQ26E9-KUlpl0Sy24szPRVJfRFGzPqZi3Pgu"
                                    "rEKxv4W8fCKxcamY1KwkktYRazDsJZW5kGJB6FdedOQx1Q,,";


START_TEST_DEFINE(TestRearrangeRTYQTree)

bool Run() override {
    bool prefixed = GetIsPrefixed();

    TVector<NRTYServer::TMessage> messages;
    GenerateInput(messages, 1, NRTYServer::TMessage::ADD_DOCUMENT, prefixed);

    TString kps;
    if (prefixed) {
        messages[0].MutableDocument()->SetKeyPrefix(1);
        kps = "&kps=1";
    }

    IndexMessages(messages, DISK, 1);
    ReopenIndexers();

    TVector<TDocSearchInfo> results;
    THashMultiMap<TString, TString> searchProperties;
    QuerySearch("body&rty_qtree=true" + kps, results, nullptr, &searchProperties, true);

    const auto it = searchProperties.find("rty_qtree");
    CHECK_TEST_TRUE(it != searchProperties.end());
    CHECK_TEST_EQ(it->second, canonicRequestTree);
    return true;
}

bool InitConfig() override{
    (*SPConfigDiff)["SearchConfig.ReArrangeOptions"] = "RTYProperties";
    return true;
}

}; // TestPatentHighlighter
