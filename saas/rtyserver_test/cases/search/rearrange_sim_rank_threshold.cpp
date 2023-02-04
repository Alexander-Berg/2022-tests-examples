#include <saas/rtyserver_test/testerlib/rtyserver_test.h>

//
// Test for TRankThresholdRule (the rule simulates shrinking the indexes to a fraction of the original size)
//
START_TEST_DEFINE(TestRearrangeSimRankThreshold)
    bool Run() override {
        const TString myText = "merit";
        const TString otherText = "worthlessness";

        TVector<NRTYServer::TMessage> messagesTest;

        const int messageCount = 120;
        const int messageBlock = 5;
        static_assert(messageCount % (messageBlock * 3) == 0, "messageCount");

        for(int i = 0; i < messageCount / (messageBlock * 3); i++) {
                GenerateInput(messagesTest, messageBlock, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed(), TAttrMap(), otherText);
                GenerateInput(messagesTest, messageBlock, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed(), TAttrMap(), myText);
                GenerateInput(messagesTest, messageBlock, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed(), TAttrMap(), otherText);
        }

        IndexMessages(messagesTest, REALTIME, 1);

        TString query = "merit""&timeout=5000000";

        if (GetIsPrefixed()) {
            TString keyPrefix = "&kps=" + ToString(messagesTest.front().GetDocument().GetKeyPrefix());
            query += keyPrefix;
        }

        ReopenIndexers();

        TVector<TDocSearchInfo> results;
        QuerySearch(query, results);
        CHECK_TEST_EQ(results.size(), messageCount / 3);

        results.clear();
        query += "&rearr=scheme_Local/RankThreshold/Fraction=0.5";
        QuerySearch(query, results);
        //docids with this text are not strictly uniformly distributed
        CHECK_TEST_LESSEQ(abs((int)results.size() - messageCount / 6), 3);
        return true;
    }

    bool InitConfig() override {
        (*ConfigDiff)["Searcher.ReArrangeOptions"] = "RankThreshold()";
        SetIndexerParams(REALTIME, 200, 1);

        return true;
    }
};
