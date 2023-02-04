#include <saas/rtyserver_test/testerlib/rtyserver_test.h>

START_TEST_DEFINE(TestKiloShard, TTestMarksPool::NoCoverage, TTestMarksPool::Slow)
public:
    bool Run() override {
        if (!GetIsPrefixed())
            return true;
        TVector<NRTYServer::TMessage> messages;
        TVector<NRTYServer::TMessage> messagesToSearch;
        ui32 maxDocs = GetMaxDocuments();
        ui32 shards = GetShardsNumber();
        GenerateInput(messages, maxDocs * shards, NRTYServer::TMessage::ADD_DOCUMENT, true);
        for (size_t i = 0; i < maxDocs; ++i)
            messagesToSearch.push_back(messages[i * shards]);
        IndexMessages(messages, DISK, 1);
        ReopenIndexers();
        CheckSearchResults(messagesToSearch);
        return true;
    }
public:
    bool InitConfig() override {
        SetIndexerParams(DISK, 10);
        SetIndexerParams(REALTIME, 10, 1);
        (*ConfigDiff)["ShardsNumber"] = 1000;
        return true;
    }
};
