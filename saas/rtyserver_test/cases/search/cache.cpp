#include <saas/rtyserver_test/testerlib/rtyserver_test.h>
#include <saas/util/queue.h>
#include <util/thread/pool.h>
#include <google/protobuf/text_format.h>
#include <search/idl/meta.pb.h>

SERVICE_TEST_RTYSERVER_DEFINE(TestSearchCache)
    void CheckCaches(bool state) {
        TBackendProxy::TCachePolicy cp = Controller->GetCachePolicy();
        if (cp != TBackendProxy::NO_CACHE)
            state = true;
        TJsonPtr serverInfo(Controller->GetServerInfo());
        NJson::TJsonValue* info = &(*serverInfo)[0];;
        const NJson::TJsonValue::TMapType& statuses = (*info)["caches_state"].GetMap();
        for (NJson::TJsonValue::TMapType::const_iterator i = statuses.begin(), e = statuses.end(); i != e; ++i)
            if (!strstr(i->first.c_str(), "memory")) {

                int cur_state = i->second.GetInteger();
                VERIFY_WITH_LOG(cur_state == 1 || cur_state == 0, "Incorrect value cur_state");
                if (cur_state != state) {
                    ythrow yexception() << i->first << " cache state is '" << cur_state << "', but must be '" << state << "'";
                }
            }
    }

    bool RunTest(bool cachesOn) {
        TVector<NRTYServer::TMessage> messages;
        GenerateInput(messages, 1, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed());
        IndexMessages(messages, REALTIME, 1);
        Sleep(TDuration::Seconds(2));
        TVector<TDocSearchInfo> results;
        TString keyPrefix = "&kps=" + ToString(messages.front().GetDocument().GetKeyPrefix());
        QuerySearch("body" + keyPrefix, results);
        if (results.size() != 1)
            ythrow yexception() << "first search not work";
        Sleep(TDuration::Seconds(2));
        QuerySearch("body" + keyPrefix, results);
        if (results.size() != 1)
            ythrow yexception() << "second search not work";
        messages.front().MutableDocument()->SetBody("new text");
        messages.front().SetMessageType(NRTYServer::TMessage::MODIFY_DOCUMENT);
        ReopenIndexers();
        QuerySearch("body" + keyPrefix, results);
        if (results.size() != 1)
            ythrow yexception() << "doc not found";
        CheckCaches(cachesOn);
        IndexMessages(messages, REALTIME, 1);
        Sleep(TDuration::Seconds(2));

        CheckCaches(cachesOn);

        QuerySearch("body" + keyPrefix, results);
        if (results.size() != 0)
            ythrow yexception() << "cache is old";
        QuerySearch("new text" + keyPrefix, results);
        if (results.size() != 1)
            ythrow yexception() << "modify not work";
        return true;
    }
};

START_TEST_DEFINE_PARENT(TestSearchCacheNo, TestSearchCache)
    bool Run() override {
        VERIFY_WITH_LOG(Controller->GetCachePolicy() == TBackendProxy::NO_CACHE, "Incorrect usage test TestSearchCacheNo");
        return RunTest(false);
    }
public:
    bool InitConfig() override {
        SetSearcherParams(abFALSE, "auto", "");
        return true;
    }
};

START_TEST_DEFINE_PARENT(TestSearchCacheCorrect, TestSearchCache)
    bool Run() override {
        return RunTest(true);
    }
public:
    bool InitConfig() override {
        SetSearcherParams(abTRUE, "auto", "CacheSupporter");
        return true;
    }
};

START_TEST_DEFINE_PARENT(TestSearchCacheNoRespondBaseSearches, TestSearchCache)
bool Run() override {
    size_t countMessages = 1;
    TVector<NRTYServer::TMessage> messages;
    GenerateInput(messages, countMessages, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed());
    IndexMessages(messages, DISK, 1);
    ReopenIndexers();
    CheckCaches(true);
    TVector<TDocSearchInfo> results;
    TString keyPrefix = "&kps=" + ToString(messages.front().GetDocument().GetKeyPrefix());
    QuerySearch("body""&timeout=5000000&delay=rty_base" + keyPrefix, results);
    CHECK_TEST_EQ(results.size(), 0);
    Sleep(TDuration::Seconds(45));
    QuerySearch("body""&timeout=5000000&delay=rty_base" + keyPrefix, results);
    CHECK_TEST_EQ(results.size(), countMessages);
    return true;
}
public:
    bool InitConfig() override {
        SetIndexerParams(REALTIME, 10, 1);
        SetSearcherParams(abTRUE, "auto", "CacheSupporter", 1, abTRUE);
        (*SPConfigDiff)["SearchConfig.HttpStatuses.IncompleteStatus"] = "200";
        return true;
    }
};

START_TEST_DEFINE(TestCacheLiveTime)
bool Run() override {
    size_t countMessages = 1;
    TVector<NRTYServer::TMessage> messages;
    TVector<NRTYServer::TMessage> messagesAdd;
    GenerateInput(messages, countMessages, NRTYServer::TMessage::MODIFY_DOCUMENT, GetIsPrefixed());
    GenerateInput(messagesAdd, countMessages, NRTYServer::TMessage::MODIFY_DOCUMENT, GetIsPrefixed());
    messagesAdd.front().MutableDocument()->SetKeyPrefix(messages.front().GetDocument().GetKeyPrefix());
    IndexMessages(messages, REALTIME, 1);
    TString KpsQuery = "&kps=0&";
    if (GetIsPrefixed())
        KpsQuery = "&kps=" + ToString(messages.front().GetDocument().GetKeyPrefix()) + "&";

    TVector<TDocSearchInfo> results;
    QuerySearch("body" + KpsQuery, results);
    if (results.size() != 1)
        ythrow yexception() << "Incorrect count documents A";

    messages.front().MutableDocument()->SetBody("aaaaa");
    IndexMessages(messages, REALTIME, 1);
    QuerySearch("aaaaa" + KpsQuery, results);
    if (results.size() != 1)
        ythrow yexception() << "Incorrect count documents B";

    ReopenIndexers();

    QuerySearch("body" + KpsQuery, results);
    if (results.size() != 0)
        ythrow yexception() << "Incorrect count documents C";

    QuerySearch("aaaaa" + KpsQuery, results);
    if (results.size() != 1)
        ythrow yexception() << "Incorrect count documents D";

    QuerySearch("aaaaa+|+body" + KpsQuery, results);
    if (results.size() != 1)
        ythrow yexception() << "Incorrect count documents E";

    messages.front().MutableDocument()->SetBody("ccccc");
    IndexMessages(messages, REALTIME, 1);

    QuerySearch("aaaaa" + KpsQuery, results);
    if (results.size() != 1)
        ythrow yexception() << "Incorrect count documents E";

    IndexMessages(messagesAdd, REALTIME, 1);
    QuerySearch("aaaaa+|+body" + KpsQuery, results);
    if (results.size() != 2)
        ythrow yexception() << "Incorrect count documents L";

    QuerySearch("ccccc" + KpsQuery, results);
    if (results.size() != 1)
        ythrow yexception() << "Incorrect count documents K";

    sleep(61);
    QuerySearch("aaaaa+|+body" + KpsQuery, results);
    if (results.size() != 1)
        ythrow yexception() << "Incorrect count documents L";

    QuerySearch("body" + KpsQuery, results);
    if (results.size() != 1)
        ythrow yexception() << "Incorrect count documents G";

    QuerySearch("aaaaa" + KpsQuery, results);
    if (results.size() != 0)
        ythrow yexception() << "Incorrect count documents G";

    QuerySearch("ccccc" + KpsQuery, results);
    if (results.size() != 1)
        ythrow yexception() << "Incorrect count documents C1";

    return true;
}
public:
    bool InitConfig() override {
        SetSearcherParams(abTRUE, "60s", "");
        return true;
    }
};

START_TEST_DEFINE_PARENT(TestCacheLiveTimeSupportModifyMultiIndexes, TestSearchCache)
    bool Run() override {
        size_t countMessages = 1;
        TVector<NRTYServer::TMessage> messages1, messages2, messages3, messages4;
        GenerateInput(messages1, countMessages, NRTYServer::TMessage::MODIFY_DOCUMENT, GetIsPrefixed());
        GenerateInput(messages2, countMessages, NRTYServer::TMessage::MODIFY_DOCUMENT, GetIsPrefixed());
        GenerateInput(messages3, countMessages, NRTYServer::TMessage::MODIFY_DOCUMENT, GetIsPrefixed());
        GenerateInput(messages4, countMessages, NRTYServer::TMessage::MODIFY_DOCUMENT, GetIsPrefixed());

        messages2.front().MutableDocument()->SetKeyPrefix(messages1.front().GetDocument().GetKeyPrefix());
        messages3.front().MutableDocument()->SetKeyPrefix(messages1.front().GetDocument().GetKeyPrefix());
        messages4.front().MutableDocument()->SetKeyPrefix(messages1.front().GetDocument().GetKeyPrefix());
        CheckCaches(Controller->GetCachePolicy() != TBackendProxy::NO_CACHE);

        IndexMessages(messages1, DISK, 1);
        ReopenIndexers();

        IndexMessages(messages2, DISK, 1);
        ReopenIndexers();

        IndexMessages(messages3, DISK, 1);
        ReopenIndexers();

        IndexMessages(messages4, DISK, 1);
        ReopenIndexers();
        CheckCaches(Controller->GetCachePolicy() != TBackendProxy::NO_CACHE);

        messages1.front().MutableDocument()->SetBody("aaaaa");
        messages2.front().MutableDocument()->SetBody("bbbbb");
        messages3.front().MutableDocument()->SetBody("ccccc");
        messages4.front().MutableDocument()->SetBody("ddddd");
        CheckCaches(Controller->GetCachePolicy() != TBackendProxy::NO_CACHE);

        TVector<TDocSearchInfo> results;
        TString KpsQuery = "&kps=" + ToString(messages1.front().GetDocument().GetKeyPrefix()) + "&";

        QuerySearch("body" + KpsQuery, results);
        CHECK_TEST_EQ(results.size(), 4);

        QuerySearch("aaaaa" + KpsQuery, results);
        CHECK_TEST_EQ(results.size(), 0);

        QuerySearch("bbbbb" + KpsQuery, results);
        CHECK_TEST_EQ(results.size(), 0);

        QuerySearch("ccccc" + KpsQuery, results);
        CHECK_TEST_EQ(results.size(), 0);

        QuerySearch("ddddd" + KpsQuery, results);
        CHECK_TEST_EQ(results.size(), 0);

        IndexMessages(messages1, REALTIME, 1);
        IndexMessages(messages2, REALTIME, 1);
        IndexMessages(messages3, DISK, 1);
        IndexMessages(messages4, DISK, 1);

        if (Controller->GetCachePolicy() == TBackendProxy::LIFE_TIME) {
            QuerySearch("body" + KpsQuery, results);
            CHECK_TEST_EQ(results.size(), 4);

            QuerySearch("aaaaa" + KpsQuery, results);
            CHECK_TEST_EQ(results.size(), 1);

            QuerySearch("bbbbb" + KpsQuery, results);
            CHECK_TEST_EQ(results.size(), 1);

            QuerySearch("ccccc" + KpsQuery, results);
            CHECK_TEST_EQ(results.size(), 0);

            QuerySearch("ddddd" + KpsQuery, results);
            CHECK_TEST_EQ(results.size(), 0);
        } else {
            QuerySearch("body" + KpsQuery, results);
            CHECK_TEST_EQ(results.size(), 2);

            QuerySearch("aaaaa" + KpsQuery, results);
            CHECK_TEST_EQ(results.size(), 1);

            QuerySearch("bbbbb" + KpsQuery, results);
            CHECK_TEST_EQ(results.size(), 1);

            QuerySearch("ccccc" + KpsQuery, results);
            CHECK_TEST_EQ(results.size(), 0);

            QuerySearch("ddddd" + KpsQuery, results);
            CHECK_TEST_EQ(results.size(), 0);
        }
        return true;
    }
    bool InitConfig() override {
        SetSearcherParams(abTRUE, "auto", "CacheSupporter");
        return true;
    }
};

START_TEST_DEFINE(TestCacheLiveTimeSupportModify)
bool Run() override {
    size_t countMessages = 1;
    TVector<NRTYServer::TMessage> messages;
    TVector<NRTYServer::TMessage> messagesAdd;
    GenerateInput(messages, countMessages, NRTYServer::TMessage::MODIFY_DOCUMENT, GetIsPrefixed());
    GenerateInput(messagesAdd, countMessages, NRTYServer::TMessage::MODIFY_DOCUMENT, GetIsPrefixed());
    messagesAdd.front().MutableDocument()->SetKeyPrefix(messages.front().GetDocument().GetKeyPrefix());
    IndexMessages(messages, REALTIME, 1);
    TString KpsQuery = "&kps=0&";
    if (GetIsPrefixed())
        KpsQuery = "&kps=" + ToString(messages.front().GetDocument().GetKeyPrefix()) + "&";

    TVector<TDocSearchInfo> results;
    QuerySearch("body" + KpsQuery, results);
    if (results.size() != 1)
        ythrow yexception() << "Incorrect count documents A";

    messages.front().MutableDocument()->SetBody("aaaaa");
    IndexMessages(messages, REALTIME, 1);
    QuerySearch("aaaaa" + KpsQuery, results);
    if (results.size() != 1)
        ythrow yexception() << "Incorrect count documents B";

    ReopenIndexers();

    QuerySearch("body" + KpsQuery, results);
    QuerySearch("body" + KpsQuery, results);
    if (results.size() != 0)
        ythrow yexception() << "Incorrect count documents C";

    QuerySearch("aaaaa" + KpsQuery, results);
    if (results.size() != 1)
        ythrow yexception() << "Incorrect count documents D";

    QuerySearch("aaaaa+|+body" + KpsQuery, results);
    if (results.size() != 1)
        ythrow yexception() << "Incorrect count documents E";

    messages.front().MutableDocument()->SetBody("ccccc");
    IndexMessages(messages, REALTIME, 1);

    QuerySearch("aaaaa" + KpsQuery, results);
    if (results.size() != 0)
        ythrow yexception() << "Incorrect count documents E";

    IndexMessages(messagesAdd, REALTIME, 1);
    QuerySearch("aaaaa+|+body" + KpsQuery, results);
    if (results.size() != 1)
        ythrow yexception() << "Incorrect count documents L";

    QuerySearch("ccccc" + KpsQuery, results);
    if (results.size() != 1)
        ythrow yexception() << "Incorrect count documents K";

    sleep(60);
    QuerySearch("aaaaa+|+body" + KpsQuery, results);
    if (results.size() != 1)
        ythrow yexception() << "Incorrect count documents L";

    QuerySearch("body" + KpsQuery, results);
    if (results.size() != 1)
        ythrow yexception() << "Incorrect count documents G";

    QuerySearch("aaaaa" + KpsQuery, results);
    if (results.size() != 0)
        ythrow yexception() << "Incorrect count documents G";

    QuerySearch("ccccc" + KpsQuery, results);
    if (results.size() != 1)
        ythrow yexception() << "Incorrect count documents C";

    return true;
}
public:
    bool InitConfig() override {
        SetSearcherParams(abTRUE, "500s", "CacheSupporter");
        return true;
    }
};

START_TEST_DEFINE(TestCacheDeleteQuery)
bool Run() override {
    size_t countMessages = 23;
    TVector<NRTYServer::TMessage> messages;
    GenerateInput(messages, countMessages, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed());
    TString KpsQuery = "&kps=0&";
    if (GetIsPrefixed()) {
        for (ui32 i = 0; i < messages.size(); ++i) {
            messages[i].MutableDocument()->SetKeyPrefix(1);
        }
        KpsQuery = "&kps=" + ToString(messages.front().GetDocument().GetKeyPrefix()) + "&";

    }
    IndexMessages(messages, DISK, 1);
    ReopenIndexers();

    DeleteQueryResult("url:\"*\"" + KpsQuery + "&ftests=yes", DISK);

    TVector<TDocSearchInfo> results;
    QuerySearch("url:\"*\"" + KpsQuery, results);
    if (results.size() != 0)
        ythrow yexception() << "Incorrect count documents A";

    return true;
}
public:
    bool InitConfig() override {
        SetSearcherParams(abTRUE, "5000s", "CacheSupporter");
        (*ConfigDiff)["Searcher.PageScanSize"] = 10;
        return true;
    }
};

START_TEST_DEFINE(TestCacheSupporterUsage)
bool Run() override {
    size_t countMessages = 13;
    TVector<NRTYServer::TMessage> messages;
    GenerateInput(messages, countMessages, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed());
    TString KpsQuery = "&kps=0&";
    if (GetIsPrefixed()) {
        for (ui32 i = 0; i < messages.size(); ++i) {
            messages[i].MutableDocument()->SetKeyPrefix(1);
        }
        KpsQuery = "&kps=" + ToString(messages.front().GetDocument().GetKeyPrefix()) + "&";

    }
    IndexMessages(messages, DISK, 1);
    ReopenIndexers();
    TVector<TDocSearchInfo> results;

    TString searchResult;
    TString hrSearchResult;
    NMetaProtocol::TReport report;
    CHECK_TEST_FAILED(200 != ProcessQuery("/?dump=eventlog&ms=proto&text=url:\"*\"&numdoc=10&p=0" + KpsQuery, &searchResult), "Can't process search request");
    CHECK_TEST_FAILED(!report.ParseFromString(searchResult), "Can't parse report");
    ::google::protobuf::TextFormat::PrintToString(report, &hrSearchResult);
    DEBUG_LOG << hrSearchResult << Endl;
    CHECK_TEST_FAILED(report.GetTotalDocCount(0) != 13, "Incorrect documents count: " + ToString(report.GetTotalDocCount(0)));
    const NMetaProtocol::TGrouping& grouping(report.GetGrouping(0));
    CHECK_TEST_FAILED(grouping.GetNumDocs(0) != 13, "Incorrect docs count in report: " + ToString(grouping.GetNumDocs(0)));
    CHECK_TEST_FAILED(grouping.GetNumGroups(0) != 13, "Incorrect groups count in report: " + ToString(grouping.GetNumGroups(0)));

    DeleteQueryResult("url:\"*\"" + KpsQuery + "&p=0&ftests=yes", DISK);

    CHECK_TEST_FAILED(200 != ProcessQuery("/?dump=eventlog&ms=proto&text=url:\"*\"&numdoc=10&p=0" + KpsQuery, &searchResult), "Can't process search request");
    CHECK_TEST_FAILED(!report.ParseFromString(searchResult), "Can't parse report");
    ::google::protobuf::TextFormat::PrintToString(report, &hrSearchResult);
    DEBUG_LOG << hrSearchResult << Endl;
    CHECK_TEST_FAILED(report.GetTotalDocCount(0) != 13, "Incorrect documents count: " + ToString(report.GetTotalDocCount(0)));
    const NMetaProtocol::TGrouping& grouping1(report.GetGrouping(0));
    CHECK_TEST_FAILED(grouping1.GetNumDocs(0) != 3, "Incorrect docs count in report: " + ToString(grouping1.GetNumDocs(0)));
    CHECK_TEST_FAILED(grouping1.GetNumGroups(0) != 3, "Incorrect groups count in report: " + ToString(grouping1.GetNumGroups(0)));

    return true;
}
public:
    bool InitConfig() override {
        SetSearcherParams(abTRUE, "5000s", "CacheSupporter");
        (*ConfigDiff)["Searcher.PageScanSize"] = 10;
        return true;
    }
};

START_TEST_DEFINE_PARENT(TestHoldMetaSearch, TestSearchCache)
class TSearchJob : public IObjectInQueue {
    TTestHoldMetaSearchCaseClass& Owner;
public:
    TSearchJob(TTestHoldMetaSearchCaseClass& owner)
        : Owner(owner)
    {}

    void Process(void* /*ThreadSpecificResource*/) override {
        THolder<TSearchJob> suicide;
        Owner.SearchFunc();
    }

};

bool StopSearch;

public:
    void SearchFunc() {
        while(!StopSearch) {
            TString word = GetRandomWord();
            TVector<TDocSearchInfo> results;
            QuerySearch(word + "&timeout=50000&delay=rty_base", results);
        }
    }

    bool Run() override {
        StopSearch = false;
        TRTYMtpQueue queue;
        size_t countMessages = 1;
        TVector<NRTYServer::TMessage> messages;
        GenerateInput(messages, countMessages, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed());
        IndexMessages(messages, DISK, 1);
        ReopenIndexers();
        CheckCaches(true);
        queue.Start(1);
        if (!queue.Add(new TSearchJob(*this))) {
            ERROR_LOG << "Can't add TSearchJob" << Endl;
            return false;
        }
        ui64 startTime = millisec();
        while (millisec() - startTime < 20000) {
            CheckCaches(true);
            Sleep(TDuration::MilliSeconds(1000));
//            TGuardOfTransactionSearchArea g;
        }
        StopSearch = true;
        queue.Stop();
        return true;
    }
public:
    bool InitConfig() override {
        SetIndexerParams(REALTIME, 10, 1);
        SetSearcherParams(abTRUE, "auto", "auto", 1);
        return true;
    }
};

START_TEST_DEFINE(TestCacheAfterRestart, TTestMarksPool::OneBackendOnly)
bool Run() override {

    const unsigned int countMedMessages = 10;
    TVector<NRTYServer::TMessage> messagesTest, messagesTest1;
    TVector<NRTYServer::TMessage> messagesTest1Delete;
    INFO_LOG << "Testing disk index" << Endl;

    ui64 keyPrefix = 0;

    GenerateInput(messagesTest, countMedMessages, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed(), TAttrMap());
    GenerateInput(messagesTest1, countMedMessages, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed(), TAttrMap(), "blah");
    if (GetIsPrefixed()) {
        for (size_t i = 0; i < messagesTest.size(); i++) {
            messagesTest[i].MutableDocument()->SetKeyPrefix(1);
        }
        for (size_t i = 0; i < messagesTest1.size(); i++) {
            messagesTest1[i].MutableDocument()->SetKeyPrefix(1);
        }
        keyPrefix = 1;
    }

    IndexMessages(messagesTest1, REALTIME, 1);
    IndexMessages(messagesTest, REALTIME, 1);

    ReopenIndexers();

    TVector<TDocSearchInfo> results;

    QuerySearch("body&kps=" + ToString(keyPrefix), results);
    if (results.size() != countMedMessages)
        ythrow yexception() << "failed case A: " << results.size() << " != " << countMedMessages;

    QuerySearch("blah&kps=" + ToString(keyPrefix), results);
    if (results.size() != countMedMessages)
        ythrow yexception() << "failed case B: " << results.size() << " != " << countMedMessages;

    for (size_t i = 0; i < messagesTest1.size(); i++)
        messagesTest1Delete.push_back(BuildDeleteMessage(messagesTest1[i]));
    IndexMessages(messagesTest1Delete, REALTIME, 1);

    QuerySearch("blah&kps=" + ToString(keyPrefix), results);
    if (!results.size())
        ythrow yexception() << "failed case C1: " << results.size() << " != " << countMedMessages;

    QuerySearch("blah&kps=" + ToString(keyPrefix), results);
    if (!results.size())
        ythrow yexception() << "failed case C1: " << results.size() << " != " << countMedMessages;

    ReopenIndexers();
    Controller->RestartServer(true);
    Controller->WaitIsRepairing();

    QuerySearch("blah&kps=" + ToString(keyPrefix), results);
    CHECK_TEST_NEQ(results.size(), 0);

    sleep(34);

    QuerySearch("blah&kps=" + ToString(keyPrefix), results);
    CHECK_TEST_EQ(results.size(), 0);

    for (size_t i = 0; i < messagesTest1.size(); i++)
        messagesTest1[i].SetMessageType(NRTYServer::TMessage::MODIFY_DOCUMENT);
    IndexMessages(messagesTest1, REALTIME, 1);

    Controller->RestartServer(true);
    Controller->WaitIsRepairing();

    QuerySearch("blah&kps=" + ToString(keyPrefix), results);
    if (results.size() != countMedMessages)
        ythrow yexception() << "failed case E: " << results.size() << " != " << countMedMessages;

    return true;
}
public:
    bool InitConfig() override {
        TString rd = GetRunDir();
        (*ConfigDiff)["Searcher.QueryCache.Dir"] = rd + "/cache";
        SetSearcherParams(abTRUE, "30s", "");
        SetEnabledRepair();
        return true;
    }
};

START_TEST_DEFINE(TestSearchProxyCache)
bool Run() override {
    size_t countMessages = 10;
    TVector<NRTYServer::TMessage> messages;
    GenerateInput(messages, countMessages, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed());
    IndexMessages(messages, REALTIME, 1);

    CheckSearchResults(messages);
    Sleep(TDuration::Seconds(10));
    CheckSearchResults(messages);
    Sleep(TDuration::Seconds(10));

    const TString& query = "url:\"" + messages[0].GetDocument().GetUrl() + "\"";
    const ui64 kps = messages[0].GetDocument().GetKeyPrefix();

    TVector<TDocSearchInfo> results;
    QuerySearch(query + "&kps=" + ToString(kps), results);
    if (!results.size())
        ythrow yexception() << "sanity check failed";

    DeleteSpecial();
    CheckSearchResults(messages);

    QuerySearch(query + "&kps=" + ToString(kps), results);
    if (!results.size())
        ythrow yexception() << "cache doesn't work";

    QuerySearch(query + "&kps=" + ToString(kps) + "&relev=rubbish", results);
    if (!results.size())
        ythrow yexception() << "TextKps doesn't work";

    return true;
}
};
