#include <saas/rtyserver_test/testerlib/rtyserver_test.h>
#include <saas/rtyserver_test/util/doc_info.h>
#include <util/generic/ymath.h>
#include <saas/rtyserver_test/util/tass_parsers.h>

SERVICE_TEST_RTYSERVER_DEFINE(TimestampTest)
public:
TSet<TString> Indices;
TString IndicesDir;
TSet<TString> GetIndexDirectories() {
    PrintInfoServer();
    TJsonPtr serverInfo(Controller->GetServerInfo());
    NJson::TJsonValue& info = (*serverInfo)[0];
    IndicesDir = info["config"]["Server"][0]["IndexDir"].GetString();

    TSet<TString> indices;
    const NJson::TJsonValue::TMapType& indicesMap = info["indexes"].GetMap();
    for (NJson::TJsonValue::TMapType::const_iterator i = indicesMap.begin(); i != indicesMap.end(); ++i) {
        const TString& name = i->first;
        if (!name.StartsWith("index_"))
            continue;

        if (i->second["count"].GetInteger() == 0)
            continue;

        indices.insert(name);
    }
    return indices;
}
bool InitConfig() override {
    SetIndexerParams(ALL, 100, 1);
    SetMergerParams(true);
    return true;
}
};

START_TEST_DEFINE_PARENT(TestOldDocCountSignal, TimestampTest)
bool Run() override{
    TVector<TDuration> intervals = {
            TDuration::Hours(1), TDuration::Hours(2),
            TDuration::Hours(24),
            TDuration::Hours(48) };
    ui32 docCount = 2 * intervals.size();

    TVector<NRTYServer::TMessage> messages1, messages2;
    GenerateInput(messages1, docCount, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed());
    GenerateInput(messages2, docCount, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed());

    ui64 kps = GetIsPrefixed() ? 1 : 0;
    for (ui32 i = 0; i < docCount; ++i) {
        messages1[i].MutableDocument()->SetKeyPrefix(kps);
        messages2[i].MutableDocument()->SetKeyPrefix(kps);
    }

    TInstant border = Now() - TDuration::Days(GetMetricsMaxAgeDays());
    for (ui32 i = 0; i < intervals.size(); ++i) {
        TInstant timestamp = border - intervals[i];
        messages1[i].MutableDocument()->SetModificationTimestamp(timestamp.Seconds());
        messages2[i].MutableDocument()->SetModificationTimestamp(timestamp.Seconds());
    }
    for (ui32 i = 0; i < intervals.size(); ++i) {
        TInstant timestamp = border + intervals[i];
        messages1[intervals.size() + i].MutableDocument()->SetModificationTimestamp(timestamp.Seconds());
        messages2[intervals.size() + i].MutableDocument()->SetModificationTimestamp(timestamp.Seconds());
    }

    IndexMessages(messages1, DISK, 1);
    ReopenIndexers();

    IndexMessages(messages2, DISK, 1);
    ReopenIndexers();

    Controller->ProcessCommand("create_merger_tasks");
    Controller->ProcessCommand("do_all_merger_tasks");

    TString tassResults;
    Controller->ProcessQuery("/tass", &tassResults, "localhost", Controller->GetConfig().Controllers[0].Port, false);

    ui64 oldDocCount;
    if (!TRTYTassParser::GetTassValue(tassResults, "old-doc-count_avvv", &oldDocCount))
        ythrow (yexception() << "Failed to get old documents count from TUnistat data");

    CHECK_TEST_EQ(oldDocCount, docCount);
    return true;
}
};

START_TEST_DEFINE_PARENT(TestUpdateTimestamp, TimestampTest)
bool Run() override{
    ui32 streamId = 1;
    TVector<NRTYServer::TMessage> messages1, messages2;
    GenerateInput(messages1, 1, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed());
    GenerateInput(messages2, 1, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed());

    ui64 kps = GetIsPrefixed() ? 1 : 0;
    messages1[0].MutableDocument()->SetKeyPrefix(kps);
    messages2[0].MutableDocument()->SetKeyPrefix(kps);

    auto timestamp = messages1[0].MutableDocument()->AddTimestamps();
    timestamp->SetStream(streamId);
    timestamp->SetValue(2000);

    timestamp = messages2[0].MutableDocument()->AddTimestamps();
    timestamp->SetStream(streamId);
    timestamp->SetValue(1000);

    IndexMessages(messages1, DISK, 1);
    ReopenIndexers();

    Sleep(TDuration().Minutes(2));

    IndexMessages(messages2, DISK, 1);
    ReopenIndexers();

    Controller->ProcessCommand("create_merger_tasks");
    Controller->ProcessCommand("do_all_merger_tasks");

    TJsonPtr result = Controller->ProcessCommand("get_timestamp&stream="+ToString(streamId));

    CHECK_TEST_EQ((*result->GetArray().begin())["min_timestamp"].GetUInteger(), 1000);
    CHECK_TEST_EQ((*result->GetArray().begin())["timestamp"].GetUInteger(), 1000);
    return true;
}

bool InitConfig() override {
    SetIndexerParams(ALL, 100, 1);
    SetMergerParams(true);
    (*ConfigDiff)["ComponentsConfig.DDK.MaxUpdateAge"] = 60;
    return true;
}

};

START_TEST_DEFINE_PARENT(TestTimestampSimple, TimestampTest)
    bool Run() override{
        TVector<NRTYServer::TMessage> messages1, messages2;
        GenerateInput(messages1, 10, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed());
        GenerateInput(messages2, 10, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed());
        ui64 kps = GetIsPrefixed() ? 1 : 0;
        for (int i = 0; i < 10; ++i) {
            messages1[i].MutableDocument()->SetModificationTimestamp(10);
            messages2[i].MutableDocument()->SetModificationTimestamp(100000);
            messages1[i].MutableDocument()->SetKeyPrefix(kps);
            messages2[i].MutableDocument()->SetKeyPrefix(kps);
        }
        IndexMessages(messages1, DISK, 1);
        ReopenIndexers();
        {
            const TSet<TString>& curIndices = GetIndexDirectories();
            NRTYServer::TIndexTimestamp ts1(IndicesDir + '/' + *curIndices.begin());
            if (SafeIntegerCast<ui64>(ts1.Get()) != 10)
                ythrow yexception() << "incorrect timestamp after 1 doc";
            if (ts1.GetAvg() != 10)
                ythrow yexception() << "incorrect avg timestamp after 1 doc";
            Indices.insert(*curIndices.begin());
        }

        IndexMessages(messages2, DISK, 1);
        ReopenIndexers();
        {
            const TSet<TString>& curIndices = GetIndexDirectories();
            NRTYServer::TIndexTimestamp ts2(IndicesDir + '/' + *(++curIndices.begin()));
            if (SafeIntegerCast<ui64>(ts2.Get()) != 100000)
                ythrow yexception() << "incorrect timestamp after 2 doc";
            if (ts2.GetAvg() != 100000)
                ythrow yexception() << "incorrect avg timestamp after 2 doc";
        }

        Controller->ProcessCommand("create_merger_tasks");
        Controller->ProcessCommand("do_all_merger_tasks");

        {
            const TSet<TString>& curIndices = GetIndexDirectories();
            NRTYServer::TIndexTimestamp tsm(IndicesDir + '/' + *curIndices.begin());
            if (SafeIntegerCast<ui64>(tsm.Get()) != 100000)
                ythrow yexception() << "incorrect timestamp after merge";
            if (tsm.GetAvg() != 50005)
                ythrow yexception() << "incorrect avg timestamp after merge";
        }

        return true;
    }
};

START_TEST_DEFINE_PARENT(TestTimestampExtra, TimestampTest)
bool Run() override {
    TVector<NRTYServer::TMessage> messages1, messages2;
    GenerateInput(messages1, 10, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed());
    GenerateInput(messages2, 10, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed());
    ui64 kps = GetIsPrefixed() ? 1 : 0;
    for (int i = 0; i < 10; ++i) {
        messages1[i].MutableDocument()->SetModificationTimestamp(1);
        messages2[i].MutableDocument()->SetModificationTimestamp(1);
        messages1[i].MutableDocument()->SetKeyPrefix(kps);
        messages2[i].MutableDocument()->SetKeyPrefix(kps);
        auto p1 = messages1[i].MutableDocument()->AddTimestamps();
        p1->SetStream(2);
        p1->SetValue(12345);

        auto p2 = messages2[i].MutableDocument()->AddTimestamps();
        p2->SetStream(3);
        p2->SetValue(123456);
    }
    IndexMessages(messages1, DISK, 1);
    ReopenIndexers();
    {
        const TSet<TString>& curIndices = GetIndexDirectories();
        NRTYServer::TIndexTimestamp ts1(IndicesDir + '/' + *curIndices.begin());
        if (SafeIntegerCast<ui64>(ts1.Get()) != 12345)
            ythrow yexception() << "incorrect timestamp after 1 doc";
        if (SafeIntegerCast<ui64>(ts1.Get(2)) != 12345)
            ythrow yexception() << "incorrect stream 2 after 1 doc";
        Indices.insert(*curIndices.begin());
    }

    IndexMessages(messages2, DISK, 1);
    ReopenIndexers();
    {
        const TSet<TString>& curIndices = GetIndexDirectories();
        NRTYServer::TIndexTimestamp ts2(IndicesDir + '/' + *(++curIndices.begin()));
        if (SafeIntegerCast<ui64>(ts2.Get()) != 123456)
            ythrow yexception() << "incorrect timestamp after 2 doc";
        if (SafeIntegerCast<ui64>(ts2.Get(3)) != 123456)
            ythrow yexception() << "incorrect stream 3 after 2 doc";
    }

    Controller->ProcessCommand("create_merger_tasks");
    Controller->ProcessCommand("do_all_merger_tasks");

    {
        const TSet<TString>& curIndices = GetIndexDirectories();
        NRTYServer::TIndexTimestamp tsm(IndicesDir + '/' + *curIndices.begin());
        if (SafeIntegerCast<ui64>(tsm.Get()) != 123456)
            ythrow yexception() << "incorrect timestamp after merge";
        if (SafeIntegerCast<ui64>(tsm.Get(2)) != 12345)
            ythrow yexception() << "incorrect stream 2 after merge";
        if (SafeIntegerCast<ui64>(tsm.Get(3)) != 123456)
            ythrow yexception() << "incorrect stream 3 after merge";
    }

    return true;
}
};

START_TEST_DEFINE(TestVersioning)
bool Run() override {
    TVector<NRTYServer::TMessage> messagesAdd;
    GenerateInput(messagesAdd, 1, NRTYServer::TMessage::MODIFY_DOCUMENT, GetIsPrefixed());
    messagesAdd[0].MutableDocument()->SetVersion(10);
    IndexMessages(messagesAdd, REALTIME, 1);
    messagesAdd[0].MutableDocument()->SetVersion(1);
    MUST_BE_BROKEN(IndexMessages(messagesAdd, REALTIME, 1));

    ReopenIndexers();
    MUST_BE_BROKEN(IndexMessages(messagesAdd, REALTIME, 1));

    return true;
}
};

START_TEST_DEFINE(TestVersionedDeleteRealtime)
bool Run() override {
    const bool prefixed = GetIsPrefixed();
    const TString& kps = prefixed ? "&kps=1" : "";
    const TString& q = "url:\"*\"";

    TVector<TDocSearchInfo> results;

    TVector<NRTYServer::TMessage> messagesAdd;
    GenerateInput(messagesAdd, 1, NRTYServer::TMessage::MODIFY_DOCUMENT, GetIsPrefixed());
    messagesAdd[0].MutableDocument()->SetKeyPrefix(prefixed ? 1 : 0);
    messagesAdd[0].MutableDocument()->SetVersion(1);
    IndexMessages(messagesAdd, REALTIME, 1);

    QuerySearch(q + kps, results);
    Y_ENSURE(results.size() == 1, "incorrect results count after add");

    TVector<NRTYServer::TMessage> messagesDelete;
    messagesDelete.push_back(BuildDeleteMessage(messagesAdd[0]));
    messagesDelete[0].MutableDocument()->SetVersion(10);
    IndexMessages(messagesDelete, REALTIME, 1);

    QuerySearch(q + kps, results);
    Y_ENSURE(results.size() == 0, "incorrect results count after delete");

    MUST_BE_BROKEN(IndexMessages(messagesAdd, REALTIME, 1));

    return true;
}
};

START_TEST_DEFINE(TestIndexingTimeDeadline)
bool Run() override {
    const ui64 kps = GetIsPrefixed() ? 1 : 0;
    TVector<NRTYServer::TMessage> immortal;
    GenerateInput(immortal, 1, NRTYServer::TMessage::MODIFY_DOCUMENT, GetIsPrefixed());
    immortal[0].MutableDocument()->SetDeadlineMinutesUTC(Max<ui32>());
    immortal[0].MutableDocument()->SetKeyPrefix(kps);

    TVector<NRTYServer::TMessage> special;
    GenerateInput(special, 1, NRTYServer::TMessage::MODIFY_DOCUMENT, GetIsPrefixed());
    special[0].MutableDocument()->SetKeyPrefix(kps);

    IndexMessages(immortal, REALTIME, 1);
    IndexMessages(special, REALTIME, 1);

    TVector<TDocSearchInfo> results;
    QuerySearch("body&kps=" + ToString(kps), results);
    if (results.size() != 2)
        ythrow yexception() << "incorrect search results count before deadline " << results.size();
    Sleep(TDuration::Minutes(2));
    ReopenIndexers();
    QuerySearch("body&kps=" + ToString(kps), results);
    if (results.size() != 1)
        ythrow yexception() << "incorrect search results count after deadline " << results.size();
    return true;
}
bool InitConfig() override {
    (*ConfigDiff)["ComponentsConfig.DDK.DefaultLifetimeMinutes"] = 1;
    return true;
}
};

START_TEST_DEFINE(TestStreamId)
TString Kps;

void Check(const TVector<NRTYServer::TMessage>& messages) {
    for (size_t i = 0; i < messages.size(); ++i) {
        TVector<TDocSearchInfo> results;
        const TString& q = "url:\"" + messages[i].GetDocument().GetUrl() + "\"";
        QuerySearch(q + Kps, results);
        if (results.size() != 1)
            ythrow yexception() << "incorrect number of results: " << results.size();

        TJsonPtr jsonDocInfoPtr = Controller->GetDocInfo(results[0].GetSearcherId(), results[0].GetDocId());
        DEBUG_LOG << jsonDocInfoPtr->GetStringRobust() << Endl;
        TDocInfo di(*jsonDocInfoPtr);
        if (Abs(di.GetDDKDocInfo()["StreamId"] - i) > 0.01) {
            ythrow yexception() << "expected version " << i << ", got " << di.GetDDKDocInfo()["StreamId"];
        }
    }
}
bool Run() override {
    bool prefixed = GetIsPrefixed();
    Kps = prefixed ? "&kps=1" : "";

    TVector<NRTYServer::TMessage> incorrectStream;
    GenerateInput(incorrectStream, 1, NRTYServer::TMessage::MODIFY_DOCUMENT, GetIsPrefixed());
    incorrectStream[0].MutableDocument()->SetStreamId(Max<ui16>() + 100);
    MUST_BE_BROKEN(IndexMessages(incorrectStream, REALTIME, 1));

    TVector<NRTYServer::TMessage> messages;
    GenerateInput(messages, 10, NRTYServer::TMessage::MODIFY_DOCUMENT, GetIsPrefixed());
    for (size_t i = 0; i < 10; ++i) {
        messages[i].MutableDocument()->SetStreamId(i);
        if (prefixed) {
            messages[i].MutableDocument()->SetKeyPrefix(1);
        }
    }
    IndexMessages(messages, REALTIME, 1);
    Check(messages);

    ReopenIndexers();
    Check(messages);

    IndexMessages(messages, REALTIME, 1);

    Controller->ProcessCommand("create_merger_tasks");
    Controller->ProcessCommand("do_all_merger_tasks");
    Check(messages);

    return true;
}
};

START_TEST_DEFINE(TestFutureDeadline)
bool Run() override {
    const ui64 kps = GetIsPrefixed() ? 1 : 0;
    TVector<NRTYServer::TMessage> fromFuture;
    GenerateInput(fromFuture, 1, NRTYServer::TMessage::MODIFY_DOCUMENT, GetIsPrefixed());
    fromFuture[0].MutableDocument()->SetModificationTimestamp(Seconds() + 10);
    fromFuture[0].MutableDocument()->SetKeyPrefix(kps);
    IndexMessages(fromFuture, REALTIME, 1);

    TVector<TDocSearchInfo> results;
    QuerySearch("body&kps=" + ToString(kps), results);
    if (results.size() != 1)
        ythrow yexception() << "incorrect search results count before deadline: " << results.size();

    Sleep(TDuration::Minutes(2));
    ReopenIndexers();
    QuerySearch("body&kps=" + ToString(kps), results);
    if (results.size() != 0)
        ythrow yexception() << "incorrect search results count after deadline " << results.size();
    return true;
}
bool InitConfig() override {
    (*ConfigDiff)["ComponentsConfig.DDK.DefaultLifetimeMinutes"] = 1;
    return true;
}
};
