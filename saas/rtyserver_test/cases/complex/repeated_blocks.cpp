#include <saas/rtyserver_test/testerlib/rtyserver_test.h>
#include <util/system/thread.h>
#include <util/random/random.h>
#include <library/cpp/logger/global/global.h>

SERVICE_TEST_RTYSERVER_DEFINE(TRepeatBlocksTestCase, TTestMarksPool::NoCoverage)
    void TestRepeatBlock(TIndexerType indexer, const size_t blockSize, const size_t countThreads)
    {
        const size_t countRepeats = 10;
        TVector<NRTYServer::TMessage> messages;
        GenerateInput(messages, blockSize * countThreads, NRTYServer::TMessage::MODIFY_DOCUMENT, GetIsPrefixed(), TAttrMap(), "random");
        IndexMessages(messages, indexer, countRepeats, 0, true, true, TDuration(), TDuration(), countThreads);
        if (indexer == DISK)
            ReopenIndexers();
        CheckSearchResults(messages, TSet<std::pair<ui64, TString> >(), 1, -1, true, countThreads);
    }

    void TestSetRepeatBlock(TIndexerType indexer, const size_t countThreads)
    {
        ui32 maxDocuments = GetMaxDocuments();
        TestRepeatBlock(indexer, maxDocuments, countThreads);
        TestRepeatBlock(indexer, maxDocuments * 2u, countThreads);
    }
public:
    bool InitConfig() override {
        SetIndexerParams(ALL, 10, 1);
        return true;
    }
};

SERVICE_TEST_RTYSERVER_DEFINE_PARENT(TMultiThreadTestCase, TRepeatBlocksTestCase)
    static void* SearchThreadFunction(void* ThisVoid)
    {
        TMultiThreadTestCase* This = (TMultiThreadTestCase*)ThisVoid;
        TRY
            const size_t countRequests = 100;
            const bool isPrefixed = This->GetIsPrefixed();
            TVector<TDocSearchInfo> results;
            for(size_t i = 0; i < countRequests; ++i)
            {
                TString query = "body" + ToString(RandomNumber<ui16>(2));
                if (isPrefixed)
                    query += "&kps=" + ToString(RandomNumber<ui16>(2));
                This->QuerySearch(query, results);
            }
            return nullptr;
        CATCH("Error in SearchThreadFunction");
        return (void*)1;
    }

    void TestMultiThread(TIndexerType indexer)
    {
        typedef TSimpleSharedPtr<TThread> TThreadPtr;
        typedef TVector<TThreadPtr> TThreads;
        TThreads threads;
        const size_t countIndexThreads = 3;
        const size_t countSearchThreads = 3;
        for (size_t i = 0; i < countSearchThreads; ++i) {
            threads.push_back(TSimpleSharedPtr<TThread>(new TThread(&SearchThreadFunction, (void*)this)));
            threads.back()->Start();
        }
        TestSetRepeatBlock(indexer, countIndexThreads);
        for (TThreads::const_iterator i = threads.begin(), e = threads.end(); i != e; ++i)
            if((*i)->Join())
                ythrow yexception() << "TestMultiThread failed with exception";
    }

public:
    bool InitConfig() override {
        SetIndexerParams(ALL, 10);
        return true;
    }
};

START_TEST_DEFINE_PARENT(TestMultiThreadDisk, TMultiThreadTestCase)
    bool Run() override {
        TestMultiThread(DISK);
        return true;
    }
};

START_TEST_DEFINE_PARENT(TestMultiThreadMemory, TMultiThreadTestCase)
    bool Run() override {
        TestMultiThread(REALTIME);
        return true;
    }
};

START_TEST_DEFINE_PARENT(TestRepeatBlockDisk, TRepeatBlocksTestCase)
    bool Run() override {
        TestSetRepeatBlock(DISK, 1);
        return true;
    }
};

START_TEST_DEFINE_PARENT(TestRepeatBlockMemory, TRepeatBlocksTestCase)
    bool Run() override {
        TestSetRepeatBlock(REALTIME, 1);
        return true;
    }
};
