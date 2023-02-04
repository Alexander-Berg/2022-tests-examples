#include <saas/rtyserver_test/testerlib/rtyserver_test.h>
#include <saas/util/json/json.h>
#include <saas/rtyserver_test/util/doc_info.h>
#include <saas/rtyserver_test/util/tass_parsers.h>
#include <saas/rtyserver/docfetcher/config_wd.h>
#include <robot/library/oxygen/base/sr_utils/sr_utils.h>
#include <library/cpp/http/misc/httpcodes.h>
#include <util/generic/ymath.h>

START_TEST_DEFINE(TestDocfetcherReplies)
bool Run() override {
    TVector<NRTYServer::TMessage> messagesAdd;

    GenerateInput(messagesAdd, 20, NRTYServer::TMessage::ADD_DOCUMENT, false);
    const bool prefixed = GetIsPrefixed();
    for (ui32 i = 0; i < messagesAdd.size(); ++i) {
        messagesAdd[i].MutableDocument()->SetUrl("message-" + ToString(i));
        if (!prefixed) {
            messagesAdd[i].MutableDocument()->SetKeyPrefix(1);
        }
    }

    IndexMessages(messagesAdd, REALTIME, 1);

    const NJson::TJsonValue& replies = Controller->GetDistributorReplies();
    DEBUG_LOG << "Replies: " << NUtil::JsonToString(replies) << Endl;
    const NJson::TJsonValue::TArray& arr = replies["Normal"]["Errors"].GetArray();
    if (arr.size() != 10)
        ythrow yexception() << "incorrect number of replies";
    for (int i = 0; i < 10; ++i)
        CheckLastResult(arr[i]);
    return true;
}
void CheckLastResult(const NJson::TJsonValue& dfreply) {
    int status = dfreply["status"].GetInteger();
    if (status != 0)
        ythrow yexception() << "message " << dfreply["description"].GetString() << " has not been indexed";
}
};

START_TEST_DEFINE(TestDocfetcherConsistentClientMergeByVersion)
bool Run() override {
    TVector<NRTYServer::TMessage> src;
    GenerateInput(src, 1, NRTYServer::TMessage::ADD_DOCUMENT, false);
    const TString& prefix = GetIsPrefixed() ? "&kps=1" : "";
    if (prefix) {
        src[0].MutableDocument()->SetKeyPrefix(1);
    }

    TVector<NRTYServer::TMessage> m(25, src[0]);

    Controller->ProcessCommand("stop");
    for (ui32 i = 0; i < 25; ++i) {
        m[i].SetMessageId(i + 10);
        m[i].MutableDocument()->SetVersion((i + 15) % 26);
    }
    TIndexerClient::TContext ctx;
    ctx.DoWaitIndexing = false;
    IndexMessages(m, REALTIME, ctx);

    Controller->ProcessCommand("restart");
    Controller->WaitActiveServer();
    Controller->WaitEmptyIndexingQueues();

    TVector<TDocSearchInfo> results;

    TQuerySearchContext context;
    context.ResultCountRequirement = 1;
    context.AttemptionsCount = 20;
    context.PrintResult = true;

    QuerySearch("url:\"*\"" + prefix, results, context);
    if (results.size() != 1) {
        ythrow yexception() << "expected exactly 1 result";
    }

    TJsonPtr jsonDocInfoPtr = Controller->GetDocInfo(results[0].GetSearcherId(), results[0].GetDocId());
    DEBUG_LOG << NUtil::JsonToString(*jsonDocInfoPtr) << Endl;
    TDocInfo di(*jsonDocInfoPtr);
    if (Abs(di.GetDDKDocInfo()["Version"] - 25) > 0.01) {
        ythrow yexception() << "expected version 25, got " << di.GetDDKDocInfo()["Version"];
    }

    return true;
}
};

START_TEST_DEFINE(TestDocfetcherSearchOpenByAge)
bool Run() override {
    const bool prefixed = GetIsPrefixed();
    TVector<NRTYServer::TMessage> first;
    TVector<NRTYServer::TMessage> second;
    TVector<TDocSearchInfo> results;

    TIndexerClient::TContext ctx;
    GenerateInput(first, 1, NRTYServer::TMessage::ADD_DOCUMENT, prefixed);
    GenerateInput(second, 1, NRTYServer::TMessage::ADD_DOCUMENT, prefixed);
    first[0].MutableDocument()->SetModificationTimestamp(Seconds() - 2 * 10000);
    if (prefixed) {
        first[0].MutableDocument()->SetKeyPrefix(1);
        second[0].MutableDocument()->SetKeyPrefix(1);
    }

    const TString& kps = prefixed ? "&kps=1" : "";

    auto code = QuerySearch("url:\"*\"", results, nullptr, nullptr, true);
    if (!IsServerError(code)) {
        ythrow yexception() << "expected search unavailable, got " << code;
    }

    IndexMessages(first, REALTIME, ctx);

    code = QuerySearch("url:\"*\"", results, nullptr, nullptr, true);
    if (!IsServerError(code)) {
        ythrow yexception() << "expected search unavailable, got " << code;
    }

    CHECK_TEST_EQ(Controller->GetServerBrief(), "Fusion_Banned");

    IndexMessages(second, REALTIME, ctx);

    code = QuerySearch("url:\"*\"" + kps, results, nullptr, nullptr, true);
    if (IsServerError(code)) {
        ythrow yexception() << "expected search available, got " << code;
    }
    if (results.size() != 2) {
        ythrow yexception() << "incorrect results count, expected 2, got " << results.size();
    }

    CHECK_TEST_EQ(Controller->GetServerBrief(), "OK");

    return true;
}

bool InitConfig() override {
    (*ConfigDiff)["ModulesConfig.DOCFETCHER.Stream.SearchOpenThreshold"] = "10000";
    (*ConfigDiff)["ModulesConfig.DOCFETCHER.Stream.RtyClientNumThreads"] = "1";
    (*ConfigDiff)["ModulesConfig.DOCFETCHER.Stream.ReceiveDurationAsDocAge"] = false;
    return true;
}
};

START_TEST_DEFINE(TestDocfetcherSearchOpenOnExhaustion)
bool Run() override {
    const bool prefixed = GetIsPrefixed();
    TVector<NRTYServer::TMessage> first;
    TVector<TDocSearchInfo> results;

    TIndexerClient::TContext ctx;
    GenerateInput(first, 1, NRTYServer::TMessage::ADD_DOCUMENT, prefixed);
    first[0].MutableDocument()->SetModificationTimestamp(Seconds() - 2 * 10000);
    if (prefixed) {
        first[0].MutableDocument()->SetKeyPrefix(1);
    }

    const TString& kps = prefixed ? "&kps=1" : "";

    auto code = QuerySearch("url:\"*\"", results, nullptr, nullptr, true);
    if (!IsServerError(code)) {
        ythrow yexception() << "expected search unavailable, got " << code;
    }

    IndexMessages(first, REALTIME, ctx);

    code = QuerySearch("url:\"*\"", results, nullptr, nullptr, true);
    if (!IsServerError(code)) {
        ythrow yexception() << "expected search unavailable, got " << code;
    }

    Sleep(TDuration::Seconds(100 + 10));

    code = QuerySearch("url:\"*\"" + kps, results, nullptr, nullptr, true);
    if (IsServerError(code)) {
        ythrow yexception() << "expected search available, got " << code;
    }
    if (results.size() != 1) {
        ythrow yexception() << "incorrect results count, expected 1, got " << results.size();
    }

    return true;
}

bool InitConfig() override {
    (*ConfigDiff)["ModulesConfig.DOCFETCHER.Stream.SearchOpenThreshold"] = "10000";
    (*ConfigDiff)["ModulesConfig.DOCFETCHER.Stream.RtyClientNumThreads"] = "1";
    (*ConfigDiff)["ModulesConfig.DOCFETCHER.Stream.ReceiveDurationAsDocAge"] = false;
    return true;
}
};

START_TEST_DEFINE(TestDocfetcherReopenIndexersOnExhaustion)
    bool Run() override {
        const bool prefixed = GetIsPrefixed();
        TVector<NRTYServer::TMessage> first;
        TVector<TDocSearchInfo> results;

        TIndexerClient::TContext ctx;
        GenerateInput(first, 1, NRTYServer::TMessage::ADD_DOCUMENT, prefixed);
        first[0].MutableDocument()->SetModificationTimestamp(Seconds() - 2 * 10000);
        if (prefixed) {
            first[0].MutableDocument()->SetKeyPrefix(1);
        }

        const TString& kps = prefixed ? "&kps=1" : "";
        IndexMessages(first, DISK, ctx);

        QuerySearch("url:\"*\"", results, nullptr, nullptr, true);
        if (results.size()) {
            ythrow yexception() << "expected 0 search results, got " << results.size();
        }

        Sleep(TDuration::Seconds(100 + 10));

        QuerySearch("url:\"*\"" + kps, results, nullptr, nullptr, true);
        if (results.size() != 1) {
            ythrow yexception() << "incorrect results count, expected 1, got " << results.size();
        }

        return true;
    }

    bool InitConfig() override {
        (*ConfigDiff)["ModulesConfig.DOCFETCHER.Stream.RtyClientNumThreads"] = "1";
        (*ConfigDiff)["ModulesConfig.DOCFETCHER.Stream.ReceiveDurationAsDocAge"] = false;
        return true;
    }
};

START_TEST_DEFINE(TestDocfetcherDocumentFromFuture)
bool Run() override {
    const bool prefixed = GetIsPrefixed();
    TVector<NRTYServer::TMessage> first;
    TVector<TDocSearchInfo> results;

    TIndexerClient::TContext ctx;
    GenerateInput(first, 1, NRTYServer::TMessage::ADD_DOCUMENT, prefixed);
    first[0].MutableDocument()->SetModificationTimestamp(Seconds() + 10000);
    if (prefixed) {
        first[0].MutableDocument()->SetKeyPrefix(1);
    }

    const TString& kps = prefixed ? "&kps=1" : "";
    IndexMessages(first, REALTIME, ctx);

    QuerySearch("url:\"*\"" + kps, results, nullptr, nullptr, true);
    if (results.empty()) {
        ythrow yexception() << "expected 1 search results, got " << results.size();
    }

    return true;
}

bool InitConfig() override {
    (*ConfigDiff)["ModulesConfig.DOCFETCHER.Stream.RtyClientNumThreads"] = "1";
    (*ConfigDiff)["ModulesConfig.DOCFETCHER.Stream.ReceiveDurationAsDocAge"] = true;
    return true;
}
};

START_TEST_DEFINE(TestSynchronization)
bool Run() override {
    const ui32 docCount = 130;
    bool docsInIndex = false;
    for (ui32 i = 0; i < 10 && !docsInIndex; ++i) {
        Sleep(TDuration::Minutes(1));

        TString tassResults;
        Controller->ProcessQuery("/tass", &tassResults, "localhost", Controller->GetConfig().Controllers[0].Port, false);
        ui64 indexDiskDocCount = 0;
        if (!TRTYTassParser::GetTassValue(tassResults, "index-disk-docs_avvv", &indexDiskDocCount)) {
            INFO_LOG << "Failed to get count of docs in disk index from TUnistat data" << Endl;
        }

        docsInIndex = (indexDiskDocCount == docCount);
    }

    CHECK_TEST_TRUE(docsInIndex);

    TVector<NRTYServer::TMessage> messagesAdd;
    GenerateInput(messagesAdd, 20, NRTYServer::TMessage::ADD_DOCUMENT, false);
    IndexMessages(messagesAdd, REALTIME, 1);
    ReopenIndexers();

    CHECK_TEST_EQ(Controller->GetServerBrief(), "OK");

    return true;
}

bool InitConfig() override {
    SetMergerParams(true, 1, -1, mcpTIME);
    (*ConfigDiff)["SearchersCountLimit"] = 1;
    (*ConfigDiff)["Monitoring.Enabled"] = false;
    (*ConfigDiff)["ModulesConfig.DOCFETCHER.Enabled"] = true;

    (*ConfigDiff)["ModulesConfig.DOCFETCHER.Stream.ShardMin"] = 21844;
    (*ConfigDiff)["ModulesConfig.DOCFETCHER.Stream.ShardMax"] = 32765;

    (*ConfigDiff)["ModulesConfig.DOCFETCHER.Stream.SyncThreshold"] = 1419120000;
    (*ConfigDiff)["ModulesConfig.DOCFETCHER.Stream.SyncServer"] = "saas-zookeeper1.search.yandex.net:14880,saas-zookeeper2.search.yandex.net:14880,saas-zookeeper3.search.yandex.net:14880,saas-zookeeper4.search.yandex.net:14880,saas-zookeeper5.search.yandex.net:14880";
    (*ConfigDiff)["ModulesConfig.DOCFETCHER.Stream.SyncPath"] = "/indexBackups/backups_for_tests";
    return true;
}
};


START_TEST_DEFINE(TestSimultaneousDetach)

class TMessageProcessor : public IMessageProcessor {
public:
    TMessageProcessor()
        : StopThreadCount(0)
    {
        RegisterGlobalMessageProcessor(this);
    }
    ~TMessageProcessor() {
        UnregisterGlobalMessageProcessor(this);
    }
protected:
    TString Name() const override {
        return "TestSimultaneousDetachMessageProcessor";
    }
    bool Process(IMessage* message_) override {
        if (auto message = message_->As<TUniversalAsyncMessage>()) {
            if (message->GetType() == "mergerStopping") {
                if (!StopThreadCount) {
                    StopThreadCount++;
                    SecondQueryStoppingMerger.WaitT(TDuration::Seconds(10));
                }
                else {
                    SecondQueryStoppingMerger.Signal();
                }
                return true;
            }
        }
        return true;
    }
private:
    int StopThreadCount;
    TAutoEvent SecondQueryStoppingMerger;
};

void GetValueFromString(const TString& str, const TString& key, TString& value) {
    TStringStream ss;
    ss << str;
    NJson::TJsonValue jsonValue;
    NJson::ReadJsonTree(&ss, &jsonValue);
    value = jsonValue.GetValueByPath(key, '.')->GetString();
}

void SendDetachQuery(TString& id) {
    const TString query = "/?command=synchronizer&action=detach&sharding_type=url_hash&min_shard=0&max_shard=65533&async=yes";
    TString queryResult;
    Controller->ProcessQuery(query, &queryResult, "localhost", Controller->GetConfig().Controllers[0].Port, false);
    GetValueFromString(queryResult, "id", id);
}
void GetDetachStatus(const TString& query, TString& status) {
    TString queryResult;
    Controller->ProcessQuery(query, &queryResult, "localhost", Controller->GetConfig().Controllers[0].Port, false);
    GetValueFromString(queryResult, "result.task_status", status);
}

bool Run() override {
    TMessageProcessor messageProcessor;
    TString id1, id2;
    SendDetachQuery(id1);
    SendDetachQuery(id2);
    const TString getStatusQuery1 = "/?command=get_async_command_info&id=" + id1;
    const TString getStatusQuery2 = "/?command=get_async_command_info&id=" + id2;
    TString status;
    for (ui32 tries = 0; tries < 60; ++tries) {
        GetDetachStatus(getStatusQuery1, status);
        if (status == "FINISHED" || status == "FAILED" || status == "NOT_FOUND")
            break;
        Sleep(TDuration().Seconds(1));
    }
    CHECK_EQ_WITH_LOG(status, "FINISHED");

    for (ui32 tries = 0; tries < 60; ++tries) {
        GetDetachStatus(getStatusQuery2, status);
        if (status == "FINISHED" || status == "FAILED" || status == "NOT_FOUND")
            break;
        Sleep(TDuration().Seconds(1));
    }
    CHECK_EQ_WITH_LOG(status, "FINISHED");
    return true;
}

bool InitConfig() override {
    (*ConfigDiff)["Merger.Enabled"] = true;
    return true;
}
};

SERVICE_TEST_RTYSERVER_DEFINE(TestMinRankHandleBase)
bool Prepare() override {
    CheckStaticAsserts();
    TFsPath("wd_opts_ip.tmp").DeleteIfExists();
    TFsPath("wd_opts.tmp").DeleteIfExists();
    SetHandle(0.4);
    if (UseFilterOnProxy)
        Sleep(TDuration::Seconds(10));
    return TRTYServerTestCase::Prepare();
}


bool Run() override {
    INFO_LOG << "Starting test with enabled watchdog" << Endl;
    //Run with handle
    bool ok = RunOnce(true);
    if (!ok)
        return false;

    //Set an empty value in the file and try again
    SetEmptyHandle();
    INFO_LOG << "Starting test with empty watchdog file" << Endl;
    SetHandle();
    Sleep(TDuration::Seconds(10)); //let the watchdog catch the change
    if (!RunOnce(true))
        return false;

    //Remove the file and try again
    INFO_LOG << "Starting test without watchdog file" << Endl;
    SetHandle();
    Sleep(TDuration::Seconds(10)); //let the watchdog catch the change
    return RunOnce();
}

bool RunOnce(bool deleteMessages=false) {
    const TVector<double> sample{-10.0, -2.0, 0.0, 0.1, 0.4, 0.5, 0.9 };
    size_t expected = (size_t) std::count_if(sample.begin(), sample.end(), [=](double r) { return r >= HandleValue; });

    const bool prefixed = GetIsPrefixed();
    TVector<NRTYServer::TMessage> messages;
    TVector<TDocSearchInfo> results;
    TIndexerClient::TContext ctx;
    ctx.DoWaitIndexing = true;
    ctx.IgnoreErrors = (expected < sample.size()); // suppress "Messages not indexed" for the filtered messages

    GenerateInput(messages, 7, NRTYServer::TMessage::ADD_DOCUMENT, prefixed);
    int k = 0;
    for (double rank : sample) {
        auto& message = messages[k++];
        Y_ASSERT(message.HasDocument());
        message.MutableDocument()->SetFilterRank(rank);
        if (prefixed)
            message.MutableDocument()->SetKeyPrefix(1);
    }

    const TString& kps = prefixed ? "&kps=1" : "";
    IndexMessages(messages, REALTIME, ctx);

    QuerySearch("url:\"*\"" + kps, results, nullptr, nullptr, true);

    if (deleteMessages)
        DeleteMessages(messages);

    if (results.size() != expected) {
        ythrow (yexception() << "expected " << expected << " search results, got " << results.size());
    }
    return true;
}

bool Finish() override {
    HandleFile.DeleteIfExists();
    return TRTYServerTestCase::Finish();
}

bool InitConfig() override {
    (*ConfigDiff)["ModulesConfig.DOCFETCHER.Stream.RtyClientNumThreads"] = "1";
    (*ConfigDiff)["ModulesConfig.DOCFETCHER.Stream.ReceiveDurationAsDocAge"] = false;
    return true;
}

protected:
//Instrumental methods
void DeleteMessages(const TVector<NRTYServer::TMessage>& messages) {
    TVector<NRTYServer::TMessage> delMessages;
    for (const auto& message : messages) {
        delMessages.push_back(BuildDeleteMessage(message));
        DEBUG_LOG << "Deleting document: " << message.GetDocument().GetUrl() << Endl;
    }

    TIndexerClient::TContext ctx;
    ctx.DoWaitIndexing = true;
    ctx.IgnoreErrors = true;
    IndexMessages(delMessages, ALL, ctx);
}

static void CheckStaticAsserts() {
    //Check values of the constants
    VERIFY_WITH_LOG(NRTYServer::TMessage::TDocument::default_instance().GetFilterRank() > 0,
                    "The TDocument::FilterRank default value is used for actions (DELETE_DOCUMENT, etc). It should be big enough to pass rank_threshold.");
    static_assert(NOxygen::ImpossibleRank >= NFusion::TMinRankCondition::DisabledValue, "TMinRankCondition::DisabledValue");
    static_assert(NOxygen::ImpossibleRank >= NFusion::TMinRankCondition::PausedValue, "TMinRankCondition::PausedValue");
    static_assert(NFusion::TMinRankCondition::PausedValue > NFusion::TMinRankCondition::DisabledValue,"TMinRankCondition::PausedValue");
}

protected:
void SetUseFilterOnProxy(bool vl) {
    UseFilterOnProxy = vl;
    HandleFile = TFsPath(UseFilterOnProxy ? "wd_opts_ip.tmp" : "wd_opts.tmp");
}

void SetHandle(double minRank = RankMinValue) {
    HandleFile.DeleteIfExists();

    if (minRank > RankMinValue) {
        TUnbufferedFileOutput file(HandleFile.GetPath());
        const char* handleName = UseFilterOnProxy
                                ? "refresh.iproxy.rank_threshold"
                                : "refresh.df.rank_threshold";

        file << handleName << "=" << minRank << Endl;
        file.Finish();
    }

    HandleValue = minRank;
}

void SetEmptyHandle() {
    HandleFile.DeleteIfExists();
    TUnbufferedFileOutput file(HandleFile.GetPath());
    file << Endl;
    HandleValue = RankMinValue;
}

protected:
static constexpr double RankMinValue = std::numeric_limits<double>::lowest();
bool UseFilterOnProxy = false;
double HandleValue = RankMinValue;
TFsPath HandleFile;
};


START_TEST_DEFINE_PARENT(TestDocfetcherMinRankHandle, TestMinRankHandleBase)
bool Prepare() override {
    SetUseFilterOnProxy(false);
    return TestMinRankHandleBase::Prepare();
}
};

START_TEST_DEFINE_PARENT(TestIndexerProxyMinRankHandle, TestMinRankHandleBase)
bool Prepare() override {
    SetUseFilterOnProxy(true);
    return TestMinRankHandleBase::Prepare();
}
};
