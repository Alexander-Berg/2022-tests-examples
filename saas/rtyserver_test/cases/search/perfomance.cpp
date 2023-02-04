#include <saas/rtyserver_test/testerlib/rtyserver_test.h>
#include <saas/util/queue.h>


START_TEST_DEFINE(TestSearchProxyCgiOverrideStrict)
bool Run() override {
    CHECK_TEST_TRUE(Cluster->GetNodesNames(TNODE_SEARCHPROXY).size());
    size_t countMessages = 1;
    TVector<NRTYServer::TMessage> messages;
    GenerateInput(messages, countMessages, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed());
    IndexMessages(messages, REALTIME, 1);

    TVector<TDocSearchInfo> results;
    QuerySearch("body" + GetAllKps(messages), results);
    if (results.size() != 0)
        ythrow yexception() << "Incorrect search results count case A";
    return true;
}
bool InitConfig() override {
    (*SPConfigDiff)["Service.StrictCgiParams.text"] = "aaa";
    return true;
}
};

START_TEST_DEFINE(TestSearchProxyCgiOverrideSoft)
bool Run() override {
    CHECK_TEST_TRUE(Cluster->GetNodesNames(TNODE_SEARCHPROXY).size());
    size_t countMessages = 1;
    TVector<NRTYServer::TMessage> messages;
    GenerateInput(messages, countMessages, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed());
    IndexMessages(messages, REALTIME, 1);

    TVector<TDocSearchInfo> results;
    CHECK_TEST_NEQ(200, QuerySearch("body&timeout=500000" + GetAllKps(messages), results));
    CHECK_TEST_EQ(results.size(), 0);

    CHECK_TEST_EQ(200, QuerySearch("body&timeout=500000&delay=rty_meta.0&timeout=9999999&" + GetAllKps(messages), results));
    CHECK_TEST_EQ(results.size(), 1);
    return true;
}
bool InitConfig() override {
    (*SPConfigDiff)["Service.SoftCgiParams.delay"] = "rty_meta.10000000";
    return true;
}
};

SERVICE_TEST_RTYSERVER_DEFINE(TTestSearchPerfomance)
public:
    void GenerateMessages(TVector<NRTYServer::TMessage>& messages, size_t count) {
        const size_t COUNT_WORLDS = 10;
        GenerateInput(messages, count, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed());
        for (size_t i = messages.size() - count; i < messages.size(); ++i) {
            TString text;
            for (size_t j = 0; j < COUNT_WORLDS; ++j)
                text += " " + GetRandomWord();
            messages[i].MutableDocument()->SetBody(text);
        }
    }

    struct TSeriesResults {
        ui64 IndexingTime;
        float IndexingRPS;
        ui64 SearchingTime;
        float SearchingRPS;
    };

    void Test(TIndexerType indexer) {
        const size_t SERIES_SIZE = 100000;
        const size_t SERIES_COUNT = 10;
        const size_t INDEXING_COUNT_THREADS = 4;
        const size_t SEARCHING_COUNT_THREADS = 4;

        TVector<NRTYServer::TMessage> messages;
        messages.reserve(SERIES_SIZE);
        TVector<TSeriesResults> results;
        TString report;
        for (size_t iSeries = 0; iSeries < SERIES_COUNT; ++iSeries) {
            results.push_back(TSeriesResults());
            TSeriesResults& result = results.back();
            messages.clear();
            GenerateMessages(messages, SERIES_SIZE);

            ui64 startTime = millisec();
            IndexMessages(messages, indexer, 1, 0, true, true, TDuration(), TDuration(), INDEXING_COUNT_THREADS);
            result.IndexingTime = millisec() - startTime;

            TJsonPtr serverInfo(Controller->GetServerInfo());
            NJson::TJsonValue* info = &(*serverInfo)[0];;
            if (indexer == DISK) {
                result.IndexingRPS = (*info)["disk_index_rps"].GetDouble();
                ReopenIndexers();
                Controller->ProcessCommand("create_merger_tasks");
                Controller->ProcessCommand("do_all_merger_tasks");
            } else {
                result.IndexingRPS = (*info)["memory_index_rps"].GetDouble();
                Sleep(TDuration::Seconds(1));
            }

            startTime = millisec();
            CheckSearchResults(messages, TSet<std::pair<ui64, TString> >(), 1, 1, true, SEARCHING_COUNT_THREADS);
            result.SearchingTime = millisec() - startTime;
            serverInfo = Controller->GetServerInfo();
            info = &(*serverInfo)[0];
            if (Cluster->GetNodesNames(TNODE_SEARCHPROXY).size()) {
                result.SearchingRPS = (*info)["search_rps_neh"].GetDouble();
            } else {
                result.SearchingRPS = (*info)["search_rps_http"].GetDouble();
            }

            if (result.SearchingRPS < 1e-5) {
                ythrow yexception() << "Incorrect result.SearchingRPS value : " << result.SearchingRPS;
            }
            TString reportItem;
            TStringOutput so(reportItem);
            so << iSeries << ") IndexingTime = " << result.IndexingTime << ", ";
            so << "IndexingRPS = " << result.IndexingRPS << ", ";
            so << "SearchingTime = " << result.SearchingTime << ", ";
            so << "SearchingRPS = " << result.SearchingRPS << Endl;
            INFO_LOG << reportItem;
            report += reportItem;
        }
        INFO_LOG << "TestMemorySearchPerfomance results:" << Endl << report;
    }

    bool InitConfig() override {
        SetIndexerParams(ALL, 1000000, 1);
        SetMergerParams(true, 1, -1, mcpNONE);
        return true;
    }
};

START_TEST_DEFINE_PARENT(TestSearchPerfomanceMemory, TTestSearchPerfomance, TTestMarksPool::NoCoverage)
    bool Run() override {
        Test(REALTIME);
        return true;
    }
};

START_TEST_DEFINE_PARENT(TestSearchPerfomanceDisk, TTestSearchPerfomance, TTestMarksPool::NoCoverage)
    bool Run() override {
        Test(DISK);
        return true;
    }
};

namespace {
    class TSearcher : public IObjectInQueue {
    public:
        TSearcher(TBackendProxy* proxy, int id, const char* query, size_t count, bool prefixed)
            : Proxy(proxy)
            , Query(query)
            , Count(count)
            , Id(id)
            , Prefixed(prefixed)
        {}

        void Process(void* /*ThreadSpecificResource*/) override {
            TVector<TDocSearchInfo> results;
            Proxy->QuerySearch(Query, results, Prefixed, nullptr, nullptr);
            if (results.size() != Count)
                ythrow yexception() << "cannot find document " << Id;
        }
    private:
        TBackendProxy* Proxy;
        const TString Query;
        const size_t Count;
        int Id;
        bool Prefixed;
    };
};

START_TEST_DEFINE(TestSlowSearch)

bool Prefixed = false;
size_t CountMessages = 1;
ui32 MessageCheckers = 100;

bool Run() override {
    TVector<NRTYServer::TMessage> messages;
    GenerateInput(messages, CountMessages, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed());
    IndexMessages(messages, DISK, 1);
    ReopenIndexers();
    TRTYMtpQueue queue;
    Prefixed = GetIsPrefixed();
    queue.Start(0);
    for (int i = 0; i < 500; ++i)
        queue.SafeAdd(new TSearcher(Controller, i, "&text=body&delay=rty_base&numdoc=100", CountMessages, Prefixed));
    queue.Stop();

    return true;
}
};

START_TEST_DEFINE(TestDelaySearch)

bool Prefixed = false;
size_t CountMessages = 1;
ui32 MessageCheckers = 100;
ui32 CheckDuration(const TString& delay) {
    TRTYMtpQueue queue;
    queue.Start(1);
    TInstant start = Now();
    for (ui32 i = 0; i < MessageCheckers; ++i) {
        TString request = "&text=body1\"&delay=" + delay + "&numdoc=100";
        queue.SafeAdd(new TSearcher(Controller, i, request.data(), CountMessages, Prefixed));
    }
    queue.Stop();
    return (Now() - start).MicroSeconds() / (double)MessageCheckers;
}

bool Run() override {
    TVector<NRTYServer::TMessage> messages;
    GenerateInput(messages, CountMessages, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed());
    IndexMessages(messages, DISK, 1);
    ReopenIndexers();
    TRTYMtpQueue queue;
    Prefixed = GetIsPrefixed();

    ui32 rtyMetaDurationus = CheckDuration("rty_meta.50000.50");
    ui32 rtyMetaDuration1us = CheckDuration("rty_meta.50000");
    ui32 rtySPDurationus = CheckDuration("sp.50000");
    ui32 rtyBaseDurationus = CheckDuration("rty_base.50000");
    ui32 rtySimpleDurationus = CheckDuration("");

    INFO_LOG << "rtyMetaDurationus: " << rtyMetaDurationus << Endl;
    INFO_LOG << "rtyMetaDuration1us: " << rtyMetaDuration1us << Endl;
    INFO_LOG << "rtySPDurationus: " << rtySPDurationus << Endl;
    INFO_LOG << "rtyBaseDurationus: " << rtyBaseDurationus << Endl;
    INFO_LOG << "rtySimpleDurationus: " << rtySimpleDurationus << Endl;

    CHECK_TEST_LESS(rtyMetaDuration1us, rtyMetaDurationus * 2.5);
    CHECK_TEST_LESS(rtyMetaDurationus * 1.3, rtyMetaDuration1us);
    CHECK_TEST_LESS(rtySimpleDurationus * 1.3, rtyMetaDurationus);
    CHECK_TEST_LESS(rtySimpleDurationus * 1.3, rtyBaseDurationus);

    if (Cluster->GetNodesNames(TNODE_SEARCHPROXY).size()) {
        CHECK_TEST_LESS(rtySimpleDurationus * 1.3, rtySPDurationus);
        CHECK_TEST_LESS(rtySPDurationus, rtyMetaDuration1us * 2);
    } else {
        CHECK_TEST_LESS(rtySPDurationus * 1.3, rtyMetaDurationus);
    }

    return true;
}
};

START_TEST_DEFINE(TestSearchTimeout)
bool Run() override {
    size_t countMessages = 1;
    TVector<NRTYServer::TMessage> messages;
    GenerateInput(messages, countMessages, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed());
    IndexMessages(messages, DISK, 1);
    ReopenIndexers();
    TRTYMtpQueue queue;
    bool prefixed = GetIsPrefixed();
    TVector<TDocSearchInfo> fake;
    QuerySearch("abv" + GetAllKps(messages), fake);
    queue.Start(0);
    ui64 start = millisec();
    for (int i = 0; i < 100; ++i)
        queue.SafeAdd(new TSearcher(Controller, i, "&text=body&delay=rty_base&numdoc=100&timeout=1000000", 0, prefixed));
    queue.Stop();
    ui64 time = millisec() - start;
    if (time > 2000)
        ythrow yexception() << "Too slow: " << time << "ms";
    queue.Start(0);
    start = millisec();
    for (int i = 0; i < 100; ++i)
        queue.SafeAdd(new TSearcher(Controller, i, "&text=body&delay=rty_base&numdoc=100&timeout=1000000&mss=1000000", 0, prefixed));
    queue.Stop();
    time = millisec() - start;
    if (time > 2000)
        ythrow yexception() << "Too slow: " << time << "ms";
    queue.Start(0);
    start = millisec();
    for (int i = 0; i < 100; ++i)
        queue.SafeAdd(new TSearcher(Controller, i, "&text=body&delay=rty_base.5000000&numdoc=100&timeout=1000000&mss=1000000", 0, prefixed));
    queue.Stop();
    time = millisec() - start;
    if (time > 2000)
        ythrow yexception() << "Too slow: " << time << "ms";
    queue.Start(0);
    start = millisec();
    for (int i = 0; i < 100; ++i)
        queue.SafeAdd(new TSearcher(Controller, i, "&text=body&delay=rty_base.2000000&numdoc=100&timeout=1000000&mss=1000000", 0, prefixed));
    queue.Stop();
    time = millisec() - start;
    if (time > 2000)
        ythrow yexception() << "Too slow: " << time << "ms";

    Controller->ProcessCommand("stop");
    return true;
}
bool InitConfig() override {
    SetSearcherParams(abAUTO, "auto", "auto", 1);
    (*ConfigDiff)["BaseSearchersServer.Threads"] = 1;
    return true;
}
};

START_TEST_DEFINE(TestBigMss)
bool Run() override {
    size_t countMessages = 100;
    TVector<NRTYServer::TMessage> messages;
    GenerateInput(messages, countMessages, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed());
    TRTYMtpQueue queue;
    bool prefixed = GetIsPrefixed();
    queue.Start(0);
    for (int i = 0; i < 50; ++i) {
        for (int j = 0; j < 9; ++j)
            queue.SafeAdd(new TSearcher(Controller, i * 10 + j, "&text=body&mss=1000000", 0, prefixed));
        queue.SafeAdd(new TSearcher(Controller, i * 10 + 9, "&text=body&mss=1000000&delay=rty_base.2000000", 0, prefixed));
    }
    IndexMessages(messages, REALTIME, 100);
    queue.Stop();
    return true;
}
bool InitConfig() override {
    SetIndexerParams(ALL, 100, 1);
    SetMergerParams(true, 1, -1, mcpNEWINDEX);
    return true;
}
};

START_TEST_DEFINE(TestSearchProxySlowSearch)
bool Run() override {
    size_t countMessages = 1;
    TVector<NRTYServer::TMessage> messages;
    GenerateInput(messages, countMessages, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed());
    IndexMessages(messages, REALTIME, 1);

    bool prefixed = GetIsPrefixed();
    TRTYMtpQueue queue;
    queue.Start(0);
    for (int i = 0; i < 10; ++i)
        queue.SafeAdd(new TSearcher(Controller, 1, "&text=body&delay=rty_base", 1, prefixed));
    TInstant start(Now());
    TString discarded;
    ProcessQuery("&info_server=yes", &discarded);
    TInstant end(Now());
    if (end - start > TDuration::MilliSeconds(100))
        ythrow yexception() << "cannot get info_server through search port";
    queue.Stop();
    return true;
}
bool InitConfig() override {
    (*SPConfigDiff)["Threads"] = "1";
    return true;
}
};
