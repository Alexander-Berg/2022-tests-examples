#include <saas/rtyserver_test/testerlib/rtyserver_test.h>

SERVICE_TEST_RTYSERVER_DEFINE(TSlowSocketTestCase)
    void Test(size_t messagesCount, const TDuration& interByteTimeout,
              const TDuration& interMessageTimeout,
              TIndexerType indexer,
              bool reopen)
    {
        TVector<NRTYServer::TMessage> messages;
        GenerateInput(messages, messagesCount, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed());
        IndexMessages(messages, indexer, 1, 0, true, true, interByteTimeout, interMessageTimeout);
        if (reopen)
            ReopenIndexers();
        CheckSearchResults(messages);
    }
};

START_TEST_DEFINE_PARENT(TestSlowIndexingDisk, TSlowSocketTestCase, TTestMarksPool::NoCoverage)
    bool Run() override {
        DEBUG_LOG << "Try slow socket disk" << Endl;
        Test(1, TDuration::MilliSeconds(500), TDuration(), DISK, true);
        return true;
    }
};

START_TEST_DEFINE_PARENT(TestSlowIndexingMemory, TSlowSocketTestCase, TTestMarksPool::NoCoverage)
    bool Run() override {
        DEBUG_LOG << "Try slow socket memory" << Endl;
        Test(1, TDuration::MilliSeconds(500), TDuration(), REALTIME, false);
        return true;
    }
};

START_TEST_DEFINE_PARENT(TestRareIndexingDisk, TSlowSocketTestCase, TTestMarksPool::NoCoverage)
    bool Run() override {
        DEBUG_LOG << "Try rare messages disk" << Endl;
        Test(10, TDuration(), TDuration::MilliSeconds(1000), DISK, true);
        return true;
    }
};

START_TEST_DEFINE_PARENT(TestRareIndexingMemory, TSlowSocketTestCase, TTestMarksPool::NoCoverage)
    bool Run() override {
        DEBUG_LOG << "Try rare messages memory" << Endl;
        Test(10, TDuration(), TDuration::MilliSeconds(1000), REALTIME, false);
        return true;
    }
};
