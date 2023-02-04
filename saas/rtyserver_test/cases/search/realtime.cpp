#include <saas/rtyserver_test/testerlib/rtyserver_test.h>
#include <util/charset/wide.h>
#include <util/string/vector.h>

START_TEST_DEFINE(TestRealtimeDocumentsLimit)
    bool Run() override {
        const int CountMessages = 150;
        TVector<NRTYServer::TMessage> messagesToMem, messagesToMem1;
        GenerateInput(messagesToMem, CountMessages, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed());
        GenerateInput(messagesToMem1, 15, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed());

        IndexMessages(messagesToMem, REALTIME, 1);

        sleep(5);
        TVector<TDocSearchInfo> results;

        ui32 count = 0;
        for (ui32 i = 0; i < messagesToMem.size(); ++i) {
            QuerySearch("url:\"" + messagesToMem[i].GetDocument().GetUrl() + "\"&numdoc=500&nocache=da&kps=" + ToString(messagesToMem[i].GetDocument().GetKeyPrefix()), results);
            CHECK_TEST_LESSEQ(results.size(), 1);
            if (results.size() == 1) {
                count++;
            }
        }
        CHECK_TEST_TRUE(count >= 10 * GetShardsNumber());
        CHECK_TEST_LESSEQ(count, (10 + 2) * GetShardsNumber());

        ReopenIndexers();
        TSearchMessagesContext smc;
        CheckSearchResults(messagesToMem, smc);

        IndexMessages(messagesToMem1, REALTIME, 1);

        sleep(5);
        count = 0;
        for (ui32 i = 0; i < messagesToMem1.size(); ++i) {
            QuerySearch("url:\"" + messagesToMem1[i].GetDocument().GetUrl() + "\"&numdoc=500&nocache=da&kps=" + ToString(messagesToMem1[i].GetDocument().GetKeyPrefix()), results);
            CHECK_TEST_LESSEQ(results.size(), 1);
            if (results.size() == 1) {
                count++;
            }
        }

        if (GetIsPrefixed()) {
            CHECK_TEST_EQ(count, 15);
        } else {
            CHECK_TEST_TRUE(count >= 10);
            CHECK_TEST_LESSEQ(count, 10 + 2);
        }

        return true;
    }
    bool InitConfig() override {
        (*ConfigDiff)["Indexer.Memory.DocumentsLimit"] = "10";
        (*ConfigDiff)["Indexer.Disk.Threads"] = "2";
        return true;
    }
};
