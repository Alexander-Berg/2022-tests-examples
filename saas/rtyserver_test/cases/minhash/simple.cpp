#include <saas/rtyserver_test/testerlib/search_checker.h>
#include <saas/rtyserver_test/testerlib/rtyserver_test.h>
#include <saas/library/daemon_base/actions_engine/controller_script.h>
#include <saas/library/daemon_base/controller_actions/restart.h>
#include <saas/rtyserver/controller/controller_actions/clear_index_action.h>
#include <saas/rtyserver/common/common_messages.h>

#include <saas/api/factors_erf.h>
#include <saas/util/bomb.h>

#include <util/generic/ymath.h>

START_TEST_DEFINE(TestMinHash)
    bool Run() override {
        const int CountMessages = 100;
        TVector<NRTYServer::TMessage> messagesToMem;
        TVector<NRTYServer::TMessage> messagesToDisk;
        GenerateInput(messagesToMem, CountMessages, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed());
        GenerateInput(messagesToDisk, CountMessages, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed());

        IndexMessages(messagesToMem, REALTIME, 1);
        IndexMessages(messagesToDisk, DISK, 1);

        TSet<std::pair<ui64, TString> > deleted;
        CheckSearchResults(messagesToDisk, deleted, 0);

        CheckSearchResults(messagesToMem, deleted);
        if (!SendIndexReply) {
            Controller->WaitEmptyIndexingQueues();
            sleep(10);
        }

        ReopenIndexers();

        CheckSearchResults(messagesToMem, deleted);
        CheckSearchResults(messagesToDisk, deleted);

        DeleteSomeMessages(messagesToMem, deleted, REALTIME, rand() % 7 + 2);
        DeleteSomeMessages(messagesToDisk, deleted, DISK, rand() % 7 + 2);

        if (!SendIndexReply) {
            Controller->WaitEmptyIndexingQueues();
            sleep(10);
        }
        CheckSearchResults(messagesToMem, deleted);
        CheckSearchResults(messagesToDisk, deleted);

        ReopenIndexers();

        CheckSearchResults(messagesToMem, deleted);
        CheckSearchResults(messagesToDisk, deleted);

        TSet<ui64> prefixesRemoved;
        for (unsigned i = 0; i < messagesToMem.size(); ++i) {
            ui64 keyPrefix = messagesToMem[i].GetDocument().GetKeyPrefix();
            if (deleted.find(std::make_pair(keyPrefix, messagesToMem[i].GetDocument().GetUrl())) != deleted.end())
                continue;
            if (prefixesRemoved.find(keyPrefix) == prefixesRemoved.end()) {
                DeleteQueryResult("url:\"*\"&kps=" + ToString(keyPrefix), REALTIME);
                prefixesRemoved.insert(keyPrefix);
            }
        }
        for (unsigned i = 0; i < messagesToDisk.size(); ++i) {
            ui64 keyPrefix = messagesToDisk[i].GetDocument().GetKeyPrefix();
            if (deleted.find(std::make_pair(keyPrefix, messagesToDisk[i].GetDocument().GetUrl())) != deleted.end())
                continue;
            if (prefixesRemoved.find(keyPrefix) == prefixesRemoved.end()) {
                DeleteQueryResult("url:\"*\"&kps=" + ToString(keyPrefix), REALTIME);
                prefixesRemoved.insert(keyPrefix);
            }
        }

        if (!SendIndexReply) {
            Controller->WaitEmptyIndexingQueues();
            sleep(10);
        }

        CheckSearchResults(messagesToMem, deleted, 0);
        CheckSearchResults(messagesToDisk, deleted, 0);
        return true;
    }
    bool InitConfig() override {
        if (!SendIndexReply) {
            SetIndexerParams(ALL, 2000, -1, 0);
        }
        (*ConfigDiff)["UrlToDocIdManager"] = "MinHash";
        return true;
    }
};

START_TEST_DEFINE(TestMinHashMergeMultipartTextArchive)
    bool Run() override {
        TVector<NRTYServer::TMessage> messages;
        GenerateInput(messages, 100, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed());
        IndexMessages(messages, DISK, 1);
        ReopenIndexers();
        NRTYArchive::TMultipartConfig config;
        config.ReadContextDataAccessType = NRTYArchive::IDataAccessor::DIRECT_FILE;
        config.Compression = NRTYArchive::IArchivePart::COMPRESSED;
        {
            TSet<TString> indexes = Controller->GetFinalIndexes();

            for (const auto& path : indexes) {
                if (path.StartsWith("temp_"))
                    continue;
                TFsPath fullArc(path + "/indexarc");
                auto archive = TArchiveOwner::Create(fullArc, config);
                size_t partsCount = archive->GetPartsCount();
                CHECK_TEST_EQ(partsCount, 2);
            }
            Controller->RestartServer();
        }

        Controller->ProcessCommand("do_all_merger_tasks");

        {
            TSet<TString> indexes = Controller->GetFinalIndexes();
            ui32 indexesCount = 0;
            for (const auto& path : indexes) {
                if (path.StartsWith("temp_"))
                    continue;
                TFsPath fullArc(path + "/indexarc");
                auto archive = TArchiveOwner::Create(fullArc, config);
                size_t partsCount = archive->GetPartsCount();
                CHECK_TEST_EQ(partsCount, 3);
                indexesCount++;
            }
            CHECK_TEST_EQ(indexesCount, 1);
            Controller->RestartServer();
        }
        CheckSearchResults(messages);
        return true;
    }

    bool InitConfig() override {
        SetIndexerParams(DISK, 50, 1);
        SetMergerParams(true, 1, -1, mcpNONE);
        (*ConfigDiff)["UrlToDocIdManager"] = "MinHash";
        (*ConfigDiff)["Searcher.ArchiveType"] = "AT_MULTIPART";
        return true;
    }
};

START_TEST_DEFINE(TestMinHashMixComponentsSearch)
bool Run() override {
    const int CountMessages = 100;
    TVector<NRTYServer::TMessage> messagesToMem;
    TVector<NRTYServer::TMessage> messagesToDisk;
    GenerateInput(messagesToMem, CountMessages, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed());
    GenerateInput(messagesToDisk, CountMessages, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed());

    IndexMessages(messagesToMem, REALTIME, 1);
    IndexMessages(messagesToDisk, DISK, 1);

    TSet<std::pair<ui64, TString> > deleted;
    CheckSearchResults(messagesToDisk, deleted, 0);

    CheckSearchResults(messagesToMem, deleted);
    if (!SendIndexReply) {
        Controller->WaitEmptyIndexingQueues();
        sleep(10);
    }

    ReopenIndexers();

    {
        TVector<TDocSearchInfo> results;
        QuerySearch(messagesToMem[1].GetDocument().GetUrl() + "&component=FULLARC&" + (GetIsPrefixed() ? ("sgkps=" + ToString(messagesToMem[1].GetDocument().GetKeyPrefix())) : ""), results);
        CHECK_TEST_EQ(results.size(), 1);
    }

    return true;
}
bool InitConfig() override {
    if (!SendIndexReply) {
        SetIndexerParams(ALL, 2000, -1, 0);
    }
    (*ConfigDiff)["UrlToDocIdManager"] = "MinHash";
    (*ConfigDiff)["Searcher.TwoStepQuery"] = false;
    (*SPConfigDiff)["Service.MetaSearch.TwoStepQuery"] = false;
    return true;
}
};

START_TEST_DEFINE(TestMinHashRegularCloseIndexByLiveTime)
bool Run() override {
    const int CountMessages = 3000;
    TVector<NRTYServer::TMessage> messagesToMem;
    GenerateInput(messagesToMem, CountMessages, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed());
    IndexMessages(messagesToMem, REALTIME, 1);

    TInstant timeStart = Now();
    TVector<TDocSearchInfo> results;
    while (Now() - timeStart < TDuration::Minutes(10)) {
        QuerySearch("url:\"*\"" + GetAllKps(messagesToMem) + "&relev=attr_limit=10000&numdoc=10000", results);
        if (results.size() == CountMessages)
            break;
    }
    CHECK_TEST_EQ(results.size(), CountMessages);
    return true;
}
bool InitConfig() override {
    (*ConfigDiff)["UrlToDocIdManager"] = "MinHash";
    SetIndexerParams(ALL, 5000, -1, 0);
    (*ConfigDiff)["indexer.disk.TimeToLiveSec"] = 60;
    return true;
}
};

START_TEST_DEFINE(TestMinHashTimeoutIndexerProxy)
bool Run() override {
    const int CountMessages = 1;
    TVector<NRTYServer::TMessage> messagesToMem;
    GenerateInput(messagesToMem, CountMessages, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed());

    TIndexerClient::TContext context;
    try {
        context.CgiRequest = "&timeout=1000&index_sleep=2s";
        IndexMessages(messagesToMem, REALTIME, context);
        TEST_FAILED("Incorrect timeout processing " + CurrentExceptionMessage());
    } catch (...) {
    }

    try {
        context.CgiRequest = "&timeout=0";
        IndexMessages(messagesToMem, REALTIME, context);
        TEST_FAILED("Incorrect timeout processing (case 'expired doc') " + CurrentExceptionMessage());
    } catch (...) {
    }

    try {
        context.CgiRequest = "&timeout=100000&index_sleep=2s";
        IndexMessages(messagesToMem, REALTIME, context);
    } catch (...) {
        TEST_FAILED("Incorrect timeout processing " + CurrentExceptionMessage());
    }

    return true;
}
bool InitConfig() override {
    (*ConfigDiff)["UrlToDocIdManager"] = "MinHash";
    if (!SendIndexReply) {
        SetIndexerParams(ALL, 2000, -1, 0);
    }
    return true;
}
};


START_TEST_DEFINE(TestMinHashMultiRestart)
    bool Run() override {
        const int CountMessages = 100;
        TVector<NRTYServer::TMessage> messagesToMem;
        TVector<NRTYServer::TMessage> messagesToDisk;
        GenerateInput(messagesToMem, CountMessages, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed());
        GenerateInput(messagesToDisk, CountMessages, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed());

        IndexMessages(messagesToMem, REALTIME, 1);
        IndexMessages(messagesToDisk, DISK, 1);

        CheckSearchResults(messagesToMem);

        for (int i = 0; i < 10; i++)
            Controller->RestartServer(i % 2);

        CheckSearchResults(messagesToMem);
        CheckSearchResults(messagesToDisk);

        return true;
    }
    bool InitConfig() override {
    (*ConfigDiff)["UrlToDocIdManager"] = "MinHash";
        if (!SendIndexReply) {
            SetIndexerParams(ALL, 2000, -1, 0);
        }
        return true;
    }
};

START_TEST_DEFINE(TestMinHashInstantMemIndexing)
bool Run() override {
    const int CountMessages = 3000;
    TVector<NRTYServer::TMessage> messagesToMem;
    GenerateInput(messagesToMem, CountMessages, NRTYServer::TMessage::MODIFY_DOCUMENT, GetIsPrefixed());
    TIndexerClient::TContext contextIndex;
    TInstant start = Now();
    contextIndex.CgiRequest = "&instant_reply=yes";
    IndexMessages(messagesToMem, REALTIME, contextIndex);

    TDuration dur1 = Now() - start;

    TSet<std::pair<ui64, TString> > deleted;
    CheckSearchResults(messagesToMem, deleted);

    start = Now();
    contextIndex.CgiRequest = "";
    IndexMessages(messagesToMem, REALTIME, contextIndex);

    TDuration dur2 = Now() - start;

    DEBUG_LOG << dur1 << "/" << dur2 << Endl;

    CHECK_TEST_LESS(dur1.Seconds() / dur2.Seconds(), 0.5);

    start = Now();
    contextIndex.CgiRequest = "&instant_reply=yes";
    IndexMessages(messagesToMem, REALTIME, contextIndex);

    dur1 = Now() - start;

    CheckSearchResults(messagesToMem, deleted);

    start = Now();
    contextIndex.CgiRequest = "";
    IndexMessages(messagesToMem, REALTIME, contextIndex);

    dur2 = Now() - start;

    DEBUG_LOG << dur1 << "/" << dur2 << Endl;

    CHECK_TEST_LESS(dur1.Seconds() / dur2.Seconds(), 0.5);

    return true;
}
bool InitConfig() override {
    (*ConfigDiff)["UrlToDocIdManager"] = "MinHash";
    if (!SendIndexReply) {
        SetIndexerParams(ALL, 500, -1, 0);
    }
    return true;
}
};

START_TEST_DEFINE(TestMinHash1000MemDocs)
bool Run() override {
    const int CountMessages = 1000;
    TVector<NRTYServer::TMessage> messagesToMem;
    GenerateInput(messagesToMem, CountMessages, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed());
    IndexMessages(messagesToMem, REALTIME, 1);

    TString kps;
    if (GetIsPrefixed())
        kps = GetAllKps(messagesToMem);
    TVector<TDocSearchInfo> results;

    TQuerySearchContext context;
    context.ResultCountRequirement = CountMessages;
    context.AttemptionsCount = 20;
    context.PrintResult = true;

    QuerySearch("url:\"*\"&numdoc=2000" + kps, results, context);
    if (results.size() != 1000)
        throw yexception() << "found only " << results.size() << " docs, expected 1000";

    return true;
}
bool InitConfig() override {
    (*ConfigDiff)["UrlToDocIdManager"] = "MinHash";
    if (!SendIndexReply) {
        SetIndexerParams(ALL, 2000, -1, 0);
    }
    return true;
}
};

START_TEST_DEFINE(TestMinHashSetSlotInfo)
bool Run() override {
    const int CountMessages = 1;
    TVector<NRTYServer::TMessage> messages;
    GenerateInput(messages, CountMessages, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed());
    IndexMessages(messages, REALTIME, 1);

    {
        const TString slotInfoData = "{\"ctype\":\"prestable\", \"service\": \"tests\", \"slot\": \"localhost:15020\", \"shardmin\": 0, \"shardmax\": 65533, \"disableindexing\": true, \"disablesearch\": false}";
        Controller->ProcessCommand("set_slot_info", -1, slotInfoData);
        TJsonPtr res = Controller->ProcessCommand("check_indexing");
        CHECK_TEST_TRUE(!(*res)[0]["running"].GetBoolean());
    }

    MUST_BE_BROKEN(IndexMessages(messages, REALTIME, 1));

    Controller->RestartServer();

    MUST_BE_BROKEN(IndexMessages(messages, REALTIME, 1));

    TString kps;
    if (GetIsPrefixed())
        kps = GetAllKps(messages);
    TVector<TDocSearchInfo> results;
    QuerySearch("url:\"*\"&numdoc=2000" + kps, results);
    CHECK_TEST_EQ(results.size(), CountMessages);
    results.clear();

    {
        const TString slotInfoData = "{\"ctype\":\"prestable\", \"service\": \"tests\", \"slot\": \"localhost:15020\", \"shardmin\": 0, \"shardmax\": 65533, \"disableindexing\": false, \"disablesearch\": true}";
        Controller->ProcessCommand("set_slot_info", -1, slotInfoData);
        TJsonPtr res = Controller->ProcessCommand("check_search");
        CHECK_TEST_TRUE(!(*res)[0]["running"].GetBoolean());
    }
    {
        TJsonPtr res = Controller->ProcessCommand("check_indexing");
        CHECK_TEST_TRUE((*res)[0]["running"].GetBoolean());
    }

    if (Cluster->GetNodesNames(TNODE_SEARCHPROXY).size()) {
        CHECK_TEST_EQ(502, QuerySearch("url:\"*\"&numdoc=2000" + kps, results));
    } else {
        MUST_BE_BROKEN(QuerySearch("url:\"*\"&numdoc=2000" + kps, results));
    }

    Controller->RestartServer();

    {
        TJsonPtr res = Controller->ProcessCommand("check_search");
        CHECK_TEST_TRUE(!(*res)[0]["running"].GetBoolean());
    }

    if (Cluster->GetNodesNames(TNODE_SEARCHPROXY).size()) {
        CHECK_TEST_EQ(502, QuerySearch("url:\"*\"&numdoc=2000" + kps, results));
    } else {
        MUST_BE_BROKEN(QuerySearch("url:\"*\"&numdoc=2000" + kps, results));
    }

    Controller->ProcessCommand("delete_file&filename=description");
    return true;
}
bool InitConfig() override {
    (*ConfigDiff)["UrlToDocIdManager"] = "MinHash";
    return true;
}
};

namespace {
    class TCallBack : public NDaemonController::IControllerAgentCallback {
    public:
        TCallBack(THolder<NRTYScript::TRTYClusterScript>& script)
            : Script(script)
        {}

        void OnAfterActionStep(const TActionContext& /*context*/, NDaemonController::TAction& action) override {
            SaveLoad();
            if (action.IsFinished() && !action.IsFailed())
                ythrow yexception() << "action success: " << action.Serialize().GetStringRobust();
            DEBUG_LOG << "action complete: " << action.Serialize().GetStringRobust() << Endl;
        }

        void OnBeforeActionStep(const TActionContext& /*context*/, NDaemonController::TAction& /*action*/) override {
            SaveLoad();
        }

    private:
        void SaveLoad() {
            NJson::TJsonValue j = Script->Serialize();
            DEBUG_LOG << WriteJson(&j) << Endl;
        }

        THolder<NRTYScript::TRTYClusterScript>& Script;
    };

}

START_TEST_DEFINE(TestMinHashRestartBadServer)
bool Run() override {
    THolder<NRTYScript::TRTYClusterScript> script;
    TCallBack scriptCallback(script);
    script.Reset(new NRTYScript::TRTYClusterScript("TestTask", "TestParent"));
    TString startName = script->AddAction("some_host", 8080, "", new NDaemonController::TRestartAction(NDaemonController::apStart)).GetName();
    DEBUG_LOG << "restart start action:" << startName << Endl;
    TString waitName = script->AddAction("some_host", 8080, "", new NDaemonController::TRestartAction(startName)).GetName();
    DEBUG_LOG << "restart wait action:" << waitName << Endl;
    TBomb bomb(TDuration::Seconds(10), "action is frozen");
    script->Execute(16, &scriptCallback);
    return true;
}
};

START_TEST_DEFINE(TestMinHashClearIndex)
bool Run() override {
    i64 countMessages = 30;
    TVector<NRTYServer::TMessage> messages;
    GenerateInput(messages, countMessages, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed());
    IndexMessages(messages, DISK, 1);
    ReopenIndexers();
    NDaemonController::TClearIndexAction cia(NDaemonController::apStartAndWait);
    NDaemonController::TControllerAgent agent(Controller->GetConfig().Controllers[0].Host, Controller->GetConfig().Controllers[0].Port);
    if (!agent.ExecuteAction(cia))
        ythrow yexception() << "Errors while execute restart" << Endl;
    Controller->RestartServer();
    TVector<TDocSearchInfo> results;
    QuerySearch("url:\"*\"" + GetAllKps(messages), results);
    if (!results.empty())
        ythrow yexception() << results.size() << " documents were not deleted";
    return true;
}
};

START_TEST_DEFINE(TestMinHashFetchAllFactors)
bool InitConfig() override {
    SetIndexerParams(ALL, 600, 1);
    SetMergerParams(true, 1, -1, mcpNONE);
    (*ConfigDiff)["UrlToDocIdManager"] = "MinHash";
    (*ConfigDiff)["Searcher.FactorsInfo"] = FactorsFileName;
    (*ConfigDiff)["Searcher.EnableUrlHash"] = "true";
    (*ConfigDiff)["Searcher.SkipSameDocids"] = "true";
    (*ConfigDiff)["Indexer.Memory.MaxDocumentsReserveCapacityCoeff"] = 2;
    return true;
}

bool Run() override {
    TVector<NRTYServer::TMessage> messages;
    GenerateInput(messages, 5, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed());
    for (int i = 0; i < messages.ysize(); i++) {
        messages[i].MutableDocument()->SetKeyPrefix(messages[0].GetDocument().GetKeyPrefix());
        NSaas::AddSimpleFactor("stat1", "12.3", *messages[i].MutableDocument()->MutableFactors());
        NSaas::AddSimpleFactor("stat2", "12.3", *messages[i].MutableDocument()->MutableFactors());
        NSaas::AddSimpleFactor("stat3", "12.3", *messages[i].MutableDocument()->MutableFactors());
    }
    TVector<NRTYServer::TMessage> reverseMessages(messages.rbegin(), messages.rend());
    IndexMessages(messages, REALTIME, 1);
    TString kps = GetAllKps(messages);
    ReopenIndexers();
    TVector<TDocSearchInfo> results;
    QuerySearch("url:\"*\"&haha=da" + kps, results);
    if (results.size() != 5)
        ythrow yexception() << "document not found";
    TString dh1 = results[0].GetFullDocId();
    TString dh2 = "Z3C9D6E7C4A32D6";
    TString dh3 = results[1].GetFullDocId().substr(results[1].GetFullDocId().find("-") + 1);

    QuerySearch("url:\"*\"&allfctrs=da&noqtree=1&broadcast_fetch=1&dh=" + dh1 + "&dh=" + dh2 + "&dh=" + dh3 + "&" + kps, results, nullptr, nullptr, true);
    if (results.size() != 3)
        ythrow yexception() << "fetch does not work: results count is equal to " << results.size();
    ui32 emptyUrl = 0;
    for (ui32 i = 0; i < results.size(); ++i) {
        if (!results[i].UrlInitialized())
            emptyUrl++;
    }
    if (emptyUrl != 1)
        ythrow yexception() << "incorrect doc detector failed: " << emptyUrl;
    bool check1 = false;
    bool check2 = false;
    bool check3 = false;
    for (ui32 i = 0; i < results.size(); ++i) {
        if (results[i].GetFullDocId() == dh1)
            check1 = true;
        if (results[i].GetFullDocId() == dh2)
            check2 = true;
        if (results[i].GetFullDocId() == dh3)
            check3 = true;
    }
    if (!check1 || !check2 || !check3)
        ythrow yexception() << "incorrect docs docids: " << check1 << check2 << check3;

    ui64 factorRequestCount = Controller->GetMetric("MetaSearch_FactorRequestTime_Total");
    if (factorRequestCount != 1) {
        ythrow yexception() << "incorrect factor request count: " << factorRequestCount;
    }

    return true;
}
};

START_TEST_DEFINE(TestMinHashDiskIndexerSearch)
bool Run() override {
    const int CountMessages = 20000;
    TVector<NRTYServer::TMessage> messagesToDisk1;
    GenerateInput(messagesToDisk1, CountMessages, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed());
    DEBUG_LOG << "TestDiskIndexerSearch::Indexation::START" << Endl;
    TIndexerClient::TContext context;
    THolder<TCallbackForCheckSearch> checker(new TCallbackForCheckSearch(this, 100));
    context.Callback = checker.Get();
    checker->Start();
    DEBUG_LOG << "TestDiskIndexerSearch::Indexation::CHECKER_STARTED" << Endl;
    IndexMessages(messagesToDisk1, REALTIME, context);
    DEBUG_LOG << "TestDiskIndexerSearch::Indexation::FINISHED" << Endl;
    checker->Stop();
    DEBUG_LOG << "TestDiskIndexerSearch::Indexation::CHECKER_STOPPED" << Endl;

    bool result = checker->CheckAndPrintInfo(60, 0.99);
    CHECK_TEST_EQ(result, true);
    CHECK_TEST_NEQ(checker->GetDocsCount(), 0);

    TSet<std::pair<ui64, TString> > deleted;
    CheckSearchResults(messagesToDisk1, deleted, 1);
    return true;
}
bool InitConfig() override {
    SetIndexerParams(DISK, 15000, -1, 0);
    SetEnabledDiskSearch();
    (*ConfigDiff)["UrlToDocIdManager"] = "MinHash";
    SetMergerParams(true, 1, -1, mcpCONTINUOUS);
    (*ConfigDiff)["Searcher.ExternalSearch"] = "rty_relevance";
    (*ConfigDiff)["Searcher.EnableUrlHash"] = "true";
    (*ConfigDiff)["Searcher.SkipSameDocids"] = "true";
    (*ConfigDiff)["Indexer.Memory.Enabled"] = "false";
    return true;
}
};

START_TEST_DEFINE(TestMinHashDiskIndexerSearchMODIFY)
struct TMergePause: public IMessageProcessor {
    TManualEvent ContinueMerger;

    TMergePause() {
        RegisterGlobalMessageProcessor(this);
    }
    ~TMergePause() {
        UnregisterGlobalMessageProcessor(this);
    }
    TString Name() const override {
        return "MergePause";
    }
    bool Process(IMessage* message) override {
        if (auto m = message->As<TUniversalAsyncMessage>()) {
            if (m->GetType() == "mergerDeletePhase") {
                ContinueMerger.Wait();
            }
            return true;
        }
        return false;
    }
};
template <class T>
struct TSignalEvent {
    T& E;

    TSignalEvent(T& e)
        : E(e)
    {}
    ~TSignalEvent() {
        E.Signal();
    }
};
bool Run() override {
    const int CountMessages = 20;
    TVector<NRTYServer::TMessage> messagesToDisk1;
    GenerateInput(messagesToDisk1, CountMessages, NRTYServer::TMessage::MODIFY_DOCUMENT, GetIsPrefixed());
    DEBUG_LOG << "TestDiskIndexerSearch::Indexation::START" << Endl;
    IndexMessages(messagesToDisk1, REALTIME, 1);
    DEBUG_LOG << "TestDiskIndexerSearch::Indexation::FINISHED" << Endl;
    PrintInfoServer();
    ReopenIndexers();
    TSet<std::pair<ui64, TString> > deleted;
    CheckSearchResults(messagesToDisk1, deleted, 1);

    PrintInfoServer();
    TMergePause mergePause;
    IndexMessages(messagesToDisk1, REALTIME, 1);
    {
        TSignalEvent<decltype(mergePause.ContinueMerger)> guard(mergePause.ContinueMerger);
        CheckSearchResults(messagesToDisk1, deleted, 1);
    }

    sleep(20);

    PrintInfoServer();
    CheckSearchResults(messagesToDisk1, deleted, 1);

    return true;
}
bool InitConfig() override {
    SetIndexerParams(DISK, 15000, -1, 0);
    SetEnabledDiskSearch();
    (*ConfigDiff)["UrlToDocIdManager"] = "MinHash";
    (*ConfigDiff)["Indexer.Memory.TimeToLiveSec"] = 10;
    SetMergerParams(true, 1, -1, mcpCONTINUOUS);
    (*ConfigDiff)["Searcher.ExternalSearch"] = "rty_relevance";
    (*ConfigDiff)["Searcher.EnableUrlHash"] = "true";
    (*ConfigDiff)["Indexer.Memory.Enabled"] = "false";
    return true;
}
};

START_TEST_DEFINE(TestMinHashDiskIndexerSearchAddAfterMerge)
bool Run() override {
    const int CountMessages = 20000;
    TVector<NRTYServer::TMessage> messagesToDisk1;
    GenerateInput(messagesToDisk1, CountMessages, NRTYServer::TMessage::MODIFY_DOCUMENT, GetIsPrefixed());
    DEBUG_LOG << "TestDiskIndexerSearch::Indexation::START" << Endl;
    TIndexerClient::TContext context;
    THolder<TCallbackForCheckSearch> checker(new TCallbackForCheckSearch(this, 100));
    context.Callback = checker.Get();
    checker->Start();
    DEBUG_LOG << "TestDiskIndexerSearch::Indexation::CHECKER_STARTED" << Endl;
    IndexMessages(messagesToDisk1, REALTIME, context);
    DEBUG_LOG << "TestDiskIndexerSearch::Indexation::FINISHED" << Endl;
    checker->Stop();
    DEBUG_LOG << "TestDiskIndexerSearch::Indexation::CHECKER_STOPPED" << Endl;

    bool result = checker->CheckAndPrintInfo(60, 0.99);
    CHECK_TEST_EQ(result, true);
    CHECK_TEST_NEQ(checker->GetDocsCount(), 0);

    TSet<std::pair<ui64, TString> > deleted;
    CheckSearchResults(messagesToDisk1, deleted, 1);
    return true;
}
bool InitConfig() override {
    SetIndexerParams(DISK, 15000, -1, 0);
    SetMergerParams(true, 1, -1, mcpCONTINUOUS, 500000);
    SetEnabledDiskSearch();
    (*ConfigDiff)["UrlToDocIdManager"] = "MinHash";
    (*ConfigDiff)["SearchersCountLimit"] = 1;
    (*ConfigDiff)["Searcher.ExternalSearch"] = "rty_relevance";
    (*ConfigDiff)["Searcher.EnableUrlHash"] = "true";
    (*ConfigDiff)["Indexer.Memory.Enabled"] = "false";
    return true;
}
};

START_TEST_DEFINE(TestMinHashDiskIndexerSearchAfterClose)
bool Run() override {
    const int CountMessages = 35;
    TVector<NRTYServer::TMessage> messagesToDisk1;
    GenerateInput(messagesToDisk1, CountMessages, NRTYServer::TMessage::MODIFY_DOCUMENT, GetIsPrefixed());
    DEBUG_LOG << "TestDiskIndexerSearch::Indexation::START" << Endl;
    TIndexerClient::TContext context;
    DEBUG_LOG << "TestDiskIndexerSearch::Indexation::CHECKER_STARTED" << Endl;
    IndexMessages(messagesToDisk1, REALTIME, context);
    DEBUG_LOG << "TestDiskIndexerSearch::Indexation::FINISHED" << Endl;

    sleep(10);

    TSet<std::pair<ui64, TString> > deleted;
    CheckSearchResults(messagesToDisk1, deleted, 1);
    return true;
}
bool InitConfig() override {
    SetIndexerParams(DISK, 10, -1, 0);
    SetMergerParams(true, 1, -1, mcpCONTINUOUS, 500000);
    SetEnabledDiskSearch();
    (*ConfigDiff)["UrlToDocIdManager"] = "MinHash";
    (*ConfigDiff)["SearchersCountLimit"] = 1;
    (*ConfigDiff)["Indexer.Memory.Enabled"] = "false";
    return true;
}
};

START_TEST_DEFINE(TestMinHashRemoveDocIdsByNextVersionSource)
bool Run() override {
    if (GetIsPrefixed())
        return true;
    TVector<TDocSearchInfo> results;

    const int CountMessages = 100;
    TVector<NRTYServer::TMessage> messages;
    GenerateInput(messages, CountMessages, NRTYServer::TMessage::MODIFY_DOCUMENT, GetIsPrefixed());
    IndexMessages(messages, REALTIME, 1);
    QuerySearch("body" + GetAllKps(messages) + "&numdoc=10000", results);
    CHECK_TEST_EQ(results.size(), 100);
    ReopenIndexers();
    QuerySearch("body" + GetAllKps(messages) + "&numdoc=10000", results);
    CHECK_TEST_EQ(results.size(), 100);

    for (size_t i = 0; i < CountMessages / 2; i++)
        messages.erase(messages.begin() + i);
    for (auto&& i : messages) {
        i.MutableDocument()->SetBody("asd");
    }
    IndexMessages(messages, DISK, 1);
    ReopenIndexers();
    QuerySearch("body" + GetAllKps(messages) + "&numdoc=10000", results);
    CHECK_TEST_EQ(results.size(), 100);

    QuerySearch("asd" + GetAllKps(messages) + "&numdoc=10000", results);
    CHECK_TEST_EQ(results.size(), 0);

    Controller->ProcessCommand("create_merger_tasks");
    Controller->ProcessCommand("do_all_merger_tasks");

    QuerySearch("body" + GetAllKps(messages) + "&numdoc=10000", results);
    CHECK_TEST_EQ(results.size(), 50);

    QuerySearch("asd" + GetAllKps(messages) + "&numdoc=10000", results);
    CHECK_TEST_EQ(results.size(), 50);

    for (auto&& i : messages) {
        i.MutableDocument()->SetBody("bsf");
    }
    IndexMessages(messages, REALTIME, 1);

    QuerySearch("body" + GetAllKps(messages) + "&numdoc=10000", results);
    CHECK_TEST_EQ(results.size(), 50);

    QuerySearch("asd" + GetAllKps(messages) + "&numdoc=10000", results);
    CHECK_TEST_EQ(results.size(), 0);

    QuerySearch("bsf" + GetAllKps(messages) + "&numdoc=10000", results);
    CHECK_TEST_EQ(results.size(), 50);

    return true;
}
bool InitConfig() override {
    SetIndexerParams(ALL, 600, 1);
    SetMergerParams(true, 1, -1, mcpTIME, 10000000);
    (*ConfigDiff)["UrlToDocIdManager"] = "MinHash";
    (*ConfigDiff)["SearchersCountLimit"] = 1;
    return true;
}
};

START_TEST_DEFINE(TestMinHashSearchRoyksopp)
bool Run() override {
    TVector<NRTYServer::TMessage> messages;
    GenerateInput(messages, 1, NRTYServer::TMessage::MODIFY_DOCUMENT, GetIsPrefixed(), TAttrMap(), "Röyksopp");
    IndexMessages(messages, REALTIME, 1);

    TVector<TDocSearchInfo> results;
    QuerySearch("Röyksopp" + GetAllKps(messages), results);
    CHECK_TEST_EQ(results.size(), 1);

    QuerySearch("Royksopp" + GetAllKps(messages), results);
    CHECK_TEST_EQ(results.size(), 0);

    QuerySearch("Roeyksopp" + GetAllKps(messages), results);
    CHECK_TEST_EQ(results.size(), 1);

    return true;
}
};

//zen_qs case SAAS-2877
START_TEST_DEFINE(TestMinHashKvKeySpacesEscaping)
bool Run() override {

    TVector<NRTYServer::TMessage> messages;
    GenerateInput(messages, 2, NRTYServer::TMessage::ADD_DOCUMENT, true);

    messages[0].MutableDocument()->SetUrl("desktop_id:4560558+2fe8802f-de09-4367-9dd2-5dfea132fda0");
    messages[1].MutableDocument()->SetUrl("yandexuid:2193044581472398442");

    const ui64 shard0Kps = 1;
    const ui64 shard1Kps = 60000;
    messages[0].MutableDocument()->SetKeyPrefix(shard0Kps);
    messages[1].MutableDocument()->SetKeyPrefix(shard1Kps);

    IndexMessages(messages, REALTIME, 1);
    ReopenIndexers();

    const TString extraOpts = "&sp_meta_search=multi_proxy";
    TQuerySearchContext context;
    context.PrintResult = true;
    context.AttemptionsCount = 3;

    for (const auto& m : messages) {
        context.ResultCountRequirement = 1;

        TVector<TDocSearchInfo> results;
        QuerySearch(CGIEscapeRet(m.GetDocument().GetUrl()) + "&sgkps=" +ToString(m.GetDocument().GetKeyPrefix()) + extraOpts, results, context);
        CHECK_TEST_EQ(results.size(), 1);
    }

    TString joinKeys;
    for (const auto& m : messages) {
        if (!joinKeys.empty()) {
            joinKeys += ',';
        }
        joinKeys += CGIEscapeRet(m.GetDocument().GetUrl());
    }

    const TString jkps = ToString(shard0Kps) + "," + ToString(shard1Kps);
    TVector<TDocSearchInfo> results;
    context.ResultCountRequirement = messages.size();
    QuerySearch(joinKeys + "&sgkps=" + jkps + extraOpts, results, context);
    CHECK_TEST_EQ(results.size(), messages.size());

    return true;
}
bool InitConfig() override {
    (*ConfigDiff)["UrlToDocIdManager"] = "MinHash";
    (*ConfigDiff)["IndexGenerator"] = FULL_ARCHIVE_COMPONENT_NAME;
    (*ConfigDiff)["Components"] = FULL_ARCHIVE_COMPONENT_NAME;
    (*ConfigDiff)["Indexer.Memory.Enabled"] = "false";
    (*ConfigDiff)["Searcher.SnippetsDeniedZones"] = "";
    (*ConfigDiff)["Searcher.TwoStepQuery"] = "false";
    (*SPConfigDiff)["Service.MetaSearch.TwoStepQuery"] = "false";

    (*SPConfigDiff)["Service.CgiParams.meta_search"] = "first_found";
    (*SPConfigDiff)["Service.CgiParams.normal_kv_report"] = "da";
    (*SPConfigDiff)["Service.CgiParams.noqtree"] = "1";
    return true;
}
};
