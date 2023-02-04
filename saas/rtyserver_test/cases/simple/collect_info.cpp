
#include <saas/rtyserver_test/testerlib/rtyserver_test.h>
#include <saas/rtyserver_test/util/doc_info.h>
#include <saas/util/json/json.h>
#include <saas/api/clientapi.h>

#include <library/cpp/http/io/stream.h>

#include <util/system/fs.h>


using namespace NRTY;

START_TEST_DEFINE(TestCollectInfoLogs)
    bool InitConfig() override {
        (*ConfigDiff)["Searcher.AccessLog"] = Controller->GetConfigValue("LoggerType", "DaemonConfig") + "_acc";
        return true;
    }

    bool Run() override {
        TVector<NRTYServer::TMessage> messages;
        GenerateInput(messages, 10, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed());
        IndexMessages(messages, REALTIME, 1, 0, false);
        Query("/?text=body&ms=proto&hr=da&numdoc=1");
        Sleep(TDuration::Seconds(2));
        DEBUG_LOG << Query("/?info_server=yes") << Endl;
        TString accessLog = Controller->GetConfigValue("AccessLog", "Server.Searcher");
        if (!accessLog)
            ythrow yexception() << "TestCollectInfo must be started with access log";
        CHECK_TEST_TRUE(TFsPath(accessLog).Exists());
        TUnbufferedFileInput file(accessLog);
        TString log = file.ReadAll();
        CHECK_TEST_TRUE(!log.empty());
        CheckSearchResults(messages);
        TUnbufferedFileInput fileCheckDuration(accessLog);
        TString logCheckDuration = file.ReadAll();
        if (logCheckDuration.find("uration=") == TString::npos)
            ythrow yexception() << "Duration not wrote in access.log";
        TString stdOutPath = Controller->GetConfigValue("StdOut", "DaemonConfig");
        TString stdErrPath = Controller->GetConfigValue("StdErr", "DaemonConfig");
        TString loggerType = Controller->GetConfigValue("LoggerType", "DaemonConfig");
        Cout << "test" << Endl;
        Cerr << "test" << Endl;
        DEBUG_LOG << stdOutPath << " checking" << Endl;
        CHECK_TEST_TRUE(TFsPath(stdOutPath).Exists());
        DEBUG_LOG << stdErrPath << " checking" << Endl;
        CHECK_TEST_TRUE(TFsPath(stdErrPath).Exists());
        if (!!loggerType && loggerType != "console" && loggerType != "stderr" && loggerType != "stdout")
            CHECK_TEST_TRUE(TFsPath(loggerType).Exists());
        Controller->ProcessCommand("reopenlog");
        Cout << "test" << Endl;
        Cerr << "test" << Endl;
        NFs::Remove(stdOutPath);
        NFs::Remove(stdErrPath);
        NFs::Remove(accessLog);
        NFs::Remove(loggerType);
        Cout << "test" << Endl;
        Cerr << "test" << Endl;
        Controller->ProcessCommand("reopenlog");
        Cout << "test" << Endl;
        Cerr << "test" << Endl;
        NFs::Remove(stdOutPath);
        NFs::Remove(stdErrPath);
        NFs::Remove(accessLog);
        NFs::Remove(loggerType);
        CHECK_TEST_TRUE(!TFsPath(stdOutPath).Exists());
        CHECK_TEST_TRUE(!TFsPath(stdErrPath).Exists());
        Controller->ProcessCommand("reopenlog");
        Cout << "test1" << Endl;
        Cerr << "test2" << Endl;
        Cout.Flush();
        Cerr.Flush();
        Y_ENSURE(TFsPath(stdOutPath).Exists(), "StdOut file does not exist");
        Y_ENSURE(TFsPath(stdErrPath).Exists(), "StdErr file does not exist");

        {
            TUnbufferedFileInput fi(stdErrPath);
            TString content = fi.ReadAll();
            CHECK_TEST_EQ(content.substr(content.size() - 6), "test2\n");
        }
        {
            TUnbufferedFileInput fi(stdOutPath);
            CHECK_TEST_EQ(fi.ReadAll(), "test1\n");
        }

        return true;
    }
};

START_TEST_DEFINE(TestCollectInfoStatFields)
bool Run() override {
    TVector<NRTYServer::TMessage> messages;
    GenerateInput(messages, 10, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed());
    IndexMessages(messages, REALTIME, 1, 0, false);
    ReopenIndexers();

    TJsonPtr serverInfo(Controller->GetServerInfo());
    NJson::TJsonValue info = (*serverInfo)[0];
    DEBUG_LOG << info.GetStringRobust() << Endl;
    DEBUG_LOG << info["files_size"].GetStringRobust() << Endl;
    DEBUG_LOG << info["files_size"]["indexarc"].GetStringRobust() << Endl;
    ui64 sizeArc = info["files_size"]["indexarc"].GetInteger();
    ui64 sizeSum = info["files_size"]["__SUM"].GetInteger();
    ui64 countSum = info["files_size"]["__COUNT"].GetInteger();
    CHECK_TEST_NEQ(sizeSum, 0);
    CHECK_TEST_NEQ(sizeArc, 0);
    CHECK_TEST_NEQ(countSum, 0);

    CHECK_TEST_TRUE(info.Has("Svn_revision"));
    CHECK_TEST_TRUE(info.Has("controller_uptime"));
    CHECK_TEST_TRUE(info.Has("server_uptime"));
    CHECK_TEST_TRUE(info.Has("uptime"));
    TString revision = info["Svn_revision"].GetString();
    ui32 revInt;

    DEBUG_LOG << revision << Endl;

    CHECK_TEST_TRUE(TryFromString<ui32>(revision, revInt));

    return true;
}
};

START_TEST_DEFINE(TestCollectInfoRPS)
bool InitConfig() override {
    (*ConfigDiff)["Monitoring.Enabled"] = false;
    return true;
}
bool Check(bool mustBe) {
    TJsonPtr serverInfo(Controller->GetServerInfo());
    NJson::TJsonValue& info = (*serverInfo)[0];

    NJson::TJsonValue searchRps;
    if (Cluster->GetNodesNames(TNODE_SEARCHPROXY).size()) {
        CHECK_TEST_TRUE(info.Has("search_rps_neh"));
        searchRps = info["search_rps_neh"];
    } else {
        CHECK_TEST_TRUE(info.Has("search_rps_http"));
        searchRps = info["search_rps_http"];
    }

    CHECK_TEST_TRUE(info.Has("search_rps_neh_base"));
    NJson::TJsonValue baseSearchRps = info["search_rps_neh_base"];
    CHECK_TEST_TRUE(baseSearchRps.Has("1"));
    CHECK_TEST_TRUE(baseSearchRps.Has("3"));
    CHECK_TEST_TRUE(baseSearchRps.Has("10"));
    CHECK_TEST_TRUE(baseSearchRps.Has("30"));
    double baseRps = baseSearchRps["1"].GetDouble() + baseSearchRps["3"].GetDouble() + baseSearchRps["10"].GetDouble() + baseSearchRps["30"].GetDouble();

    CHECK_TEST_TRUE(searchRps.Has("1"));
    CHECK_TEST_TRUE(searchRps.Has("3"));
    CHECK_TEST_TRUE(searchRps.Has("10"));
    CHECK_TEST_TRUE(searchRps.Has("30"));

    double rps = searchRps["1"].GetDouble() + searchRps["3"].GetDouble() + searchRps["10"].GetDouble() + searchRps["30"].GetDouble();
    if (mustBe) {
        CHECK_TEST_LESS(1e-4, baseRps);
        CHECK_TEST_LESS(1e-4, rps);
    } else {
        CHECK_TEST_LESS(baseRps, 1e-4);
        CHECK_TEST_LESS(rps, 1e-4);
    }
    return true;
}
bool Run() override {
    TVector<NRTYServer::TMessage> messages;
    sleep(40);
    CHECK_TEST_TRUE(Check(false));
    GenerateInput(messages, 10, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed());
    CHECK_TEST_TRUE(Check(false));
    IndexMessages(messages, REALTIME, 1, 0, false);
    CHECK_TEST_TRUE(Check(false));
    CheckSearchResults(messages);
    CHECK_TEST_TRUE(Check(true));
    sleep(40);
    CHECK_TEST_TRUE(Check(false));
    return true;
}
};

START_TEST_DEFINE(TestCollectInfoRPSCustom)
bool InitConfig() override {
    (*ConfigDiff)["Monitoring.Enabled"] = false;
    return true;
}
bool Check(bool mustBe) {
    TJsonPtr serverInfo(Controller->GetServerInfo());
    NJson::TJsonValue& info = (*serverInfo)[0];

    NJson::TJsonValue searchRps;
    if (Cluster->GetNodesNames(TNODE_SEARCHPROXY).size()) {
        CHECK_TEST_TRUE(info.Has("search_rps_neh"));
        searchRps = info["search_rps_neh"];
    } else {
        CHECK_TEST_TRUE(info.Has("search_rps_http"));
        searchRps = info["search_rps_http"];
    }

    CHECK_TEST_TRUE(info.Has("search_rps_neh_base"));
    NJson::TJsonValue baseSearchRps = info["search_rps_neh_base"];
    CHECK_TEST_TRUE(baseSearchRps.Has("1"));
    CHECK_TEST_TRUE(baseSearchRps.Has("3"));
    CHECK_TEST_TRUE(baseSearchRps.Has("10"));
    CHECK_TEST_TRUE(baseSearchRps.Has("30"));
    double baseRps = baseSearchRps["1"].GetDouble() + baseSearchRps["3"].GetDouble() + baseSearchRps["10"].GetDouble() + baseSearchRps["30"].GetDouble();

    CHECK_TEST_TRUE(searchRps.Has("1"));
    CHECK_TEST_TRUE(searchRps.Has("3"));
    CHECK_TEST_TRUE(searchRps.Has("10"));
    CHECK_TEST_TRUE(searchRps.Has("30"));
    double rps = searchRps["1"].GetDouble() + searchRps["3"].GetDouble() + searchRps["10"].GetDouble() + searchRps["30"].GetDouble();
    CHECK_TEST_LESS(baseRps, 1e-4);
    if (mustBe) {
        CHECK_TEST_LESS(1e-4, rps);
    } else {
        CHECK_TEST_LESS(rps, 1e-4);
    }
    return true;
}
bool Run() override {
    TVector<NRTYServer::TMessage> messages;
    sleep(40);
    CHECK_TEST_TRUE(Check(false));
    CHECK_TEST_TRUE(Check(false));
    GenerateInput(messages, 10, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed());
    CHECK_TEST_TRUE(Check(false));
    IndexMessages(messages, REALTIME, 1, 0, false);
    CHECK_TEST_TRUE(Check(false));
    try {
        Query("/?text=" + messages[0].GetDocument().GetUrl() + "&ms=proto&meta_search=first_found&numdoc=1");
    } catch (...) {
    }
    CHECK_TEST_TRUE(Check(true));
    sleep(40);
    CHECK_TEST_TRUE(Check(false));
    return true;
}
};

START_TEST_DEFINE(TestInfoRequest)
    bool Run() override {
        TVector<NRTYServer::TMessage> messages;
        GenerateInput(messages, 10, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed());
        IndexMessages(messages, DISK, 1, 0, false);
        ReopenIndexers();
        TString sCount = Query("/?info=doccount");
        if (!sCount)
            ythrow yexception() << "Cannot get doccount";
        if (FromString<int>(sCount) != 10)
            ythrow yexception() << "doccount is incorrect: " << sCount << " != 10";
        return true;
    }
};

START_TEST_DEFINE(TestDocInfo)
#define CHECK_RESULTS(A,B) if (A != B) ythrow yexception() << "check " #A "==" #B " failed " << A << " != " << B;
TDocSearchInfo GetDocSearchInfo(const TString& url, const ui64 kps) {
    TVector<TDocSearchInfo> results;
    const TString kpsString = kps ? "&kps=" + ToString(kps) : "";
    QuerySearch("url:\"" + url + "\"" + kpsString, results);
    if (results.size() != 1)
        ythrow yexception() << "url " << url << " is not unique";
    return results[0];
}

bool Run() override {
    const ui64 kps = GetIsPrefixed() ? 1 : 0;
    TAction msg;
    msg.SetActionType(TAction::atAdd);
    msg.SetPrefix(kps);
    msg.SetId(1);
    TDocument& doc = msg.GetDocument();
    doc.SetUrl("sample");
    doc.SetMimeType("text/html");
    doc.SetBody("body");
    doc.SetTimestamp(10);

    doc.AddFactor("start", 0.1);
    doc.AddFactor("finish", 1);
    TCSBlock& cs = doc.AddCS("CSDomain");
    cs.AddFactor("cs_bbb", 2);
    cs.AddFactor("cs_aaa", 42);
    TQSBlock& qs = doc.AddQS("QSText");
    const TString keys[] = { "key1", "whatever" };
    for (ui32 i = 0; i < Y_ARRAY_SIZE(keys); ++i) {
        qs.At("qs_aaa", keys[i]).Set(i);
        qs.At("qs_bbb", keys[i]).Set(100 - i);
    }

    TVector<NRTYServer::TMessage> messages(1, msg.ToProtobuf());
    IndexMessages(messages, DISK, 1);
    ReopenIndexers();
    const TDocSearchInfo& dsi = GetDocSearchInfo("sample", kps);
    TJsonPtr jsonDocInfoPtr = Controller->GetDocInfo(dsi.GetSearcherId(), dsi.GetDocId());
    DEBUG_LOG << NUtil::JsonToString(*jsonDocInfoPtr) << Endl;
    TDocInfo di(*jsonDocInfoPtr);
    CHECK_RESULTS(di.GetErfDocInfo()["finish"], 1);
    CHECK_RESULTS(di.GetCSDocInfo()["CSDomain"]["cs_aaa"], 42);
    CHECK_RESULTS(di.GetQSDocInfo()["QSText"]["whatever"]["qs_aaa"], 1);

    const TString infoRequest = Query("/?info=docinfo:docid:" + dsi.GetFullDocId());
    DEBUG_LOG << infoRequest << Endl;
    auto jsonDocInfo2 = NUtil::JsonFromString(infoRequest);
    TDocInfo di2(jsonDocInfo2);
    CHECK_RESULTS(di.GetErfDocInfo()["finish"], 1);
    CHECK_RESULTS(di.GetCSDocInfo()["CSDomain"]["cs_aaa"], 42);
    CHECK_RESULTS(di.GetQSDocInfo()["QSText"]["whatever"]["qs_aaa"], 1);

    return true;
}

bool InitConfig() override {
    (*ConfigDiff)["Searcher.FiltrationModel"] = "SIMPLE";
    (*ConfigDiff)["Searcher.FactorsInfo"] = GetResourcesDirectory() + "/factors/mix_factors.cfg";
    (*ConfigDiff)["Indexer.Common.XmlParserConfigFile"] = "";
    return true;
}
};

START_TEST_DEFINE(TestDocInfoSourcesLimit)
TDocSearchInfo GetDocSearchInfo(const TString& url, const ui64 kps) {
    TVector<TDocSearchInfo> results;
    const TString kpsString = kps ? "&kps=" + ToString(kps) : "";
    QuerySearch("url:\"" + url + "\"" + kpsString, results);
    if (results.size() != 1)
        ythrow yexception() << "url " << url << " is not unique";
    return results[0];
}

bool Run() override {
    TVector<NRTYServer::TMessage> messages1;
    GenerateInput(messages1, 1, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed());
    IndexMessages(messages1, DISK, 1);
    ReopenIndexers();

    TVector<NRTYServer::TMessage> messages2;
    GenerateInput(messages2, 1, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed());
    messages2[0].MutableDocument()->SetUrl("yandex");
    IndexMessages(messages2, DISK, 1);
    ReopenIndexers();

    Controller->ProcessCommand("create_merger_tasks");
    Controller->ProcessCommand("do_all_merger_tasks");

    TVector<NRTYServer::TMessage> messages3;
    GenerateInput(messages3, 1, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed());
    IndexMessages(messages3, DISK, 1);
    ReopenIndexers();

    auto dsi = GetDocSearchInfo(messages2[0].GetDocument().GetUrl(), messages2[0].GetDocument().GetKeyPrefix());
    TJsonPtr jsonDocInfoPtr = Controller->GetDocInfo(dsi.GetSearcherId(), dsi.GetDocId());
    DEBUG_LOG << NUtil::JsonToString(*jsonDocInfoPtr) << Endl;

    if ((*jsonDocInfoPtr)["info"]["INDEX"]["archive_url"] != "yandex") {
        ythrow yexception() << "incorrect docInfo: " << (*jsonDocInfoPtr)["info"]["INDEX"]["archive_url"] << " != yandex" << Endl;
    }

    return true;
}
bool InitConfig() override {
    (*ConfigDiff)["SearchersCountLimit"] = 1;
    SetMergerParams(true, 1, -1, mcpTIME, 5000000);
    return true;
}
};

START_TEST_DEFINE(TestPing)
bool Run() override {
    TString res;
    ui32 code = Controller->ProcessQuery("/ping", &res, Controller->GetConfig().Searcher.Host, Controller->GetConfig().Searcher.Port, false);
    CHECK_TEST_FAILED(code != 200, "bad code from ping, searchproxy: " + ToString<ui32>(code));
    CHECK_TEST_FAILED(res[0] != '1' || res.length() > 3, "bad answer from searchproxy, expected '1', got " + res);
    code = Controller->ProcessQuery("/ping", &res, Controller->GetConfig().Indexer.Host, Controller->GetConfig().Indexer.Port, false);
    CHECK_TEST_FAILED(code != 200, "bad code from ping, indexerproxy: " + ToString<ui32>(code));
    CHECK_TEST_FAILED(res[0] != '1' || res.length() > 3, "bad answer from indexerproxy, expected '1', got " + res);
    code = Controller->ProcessQuery("/ping", &res, Controller->GetConfig().Controllers[0].Host, Controller->GetConfig().Controllers[0].Port - 3, false);
    CHECK_TEST_FAILED(code != 200, "bad code from ping, backend: " + ToString<ui32>(code));
    CHECK_TEST_FAILED(res[0] != '1' || res.length() > 3, "bad answer from backend, expected '1', got " + res);
    return true;
}
};

START_TEST_DEFINE(TestIndexStat)
bool Run() override {
    TVector<NRTYServer::TMessage> messages;
    GenerateInput(messages, 2, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed());
    messages[0].MutableDocument()->SetBody("this is a sample sentence<hr>this is another one<hr>and one more");
    messages[1].MutableDocument()->SetBody("qq ww ee rr tt yy<hr>zz xx cc vv bb nn mm aaaaaa");
    IndexMessages(messages, DISK, 1, 0, false);
    ReopenIndexers();

    if (!CheckStatJson(7, 26))
        return false;

    TVector<NRTYServer::TMessage> messages2;
    GenerateInput(messages2, 1, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed());
    messages2[0].MutableDocument()->SetBody("aa bb");
    IndexMessages(messages2, DISK, 1, 0, false);
    ReopenIndexers();

    if (!CheckStatJson(9, 28))
        return false;

    return true;
}
bool CheckStatJson(ui64 sent, ui64 word) {
    TString stat = Query("/?info=indexstat");
    TStringInput in(stat);
    NJson::TJsonValue json;
    CHECK_TEST_EQ(ReadJsonTree(&in, &json), true);
    NJson::TJsonValue value;
    CHECK_TEST_EQ(json.GetValueByPath("SentCount", value), true);
    CHECK_TEST_EQ(value.GetUInteger(), sent);
    CHECK_TEST_EQ(json.GetValueByPath("WordCount", value), true);
    CHECK_TEST_EQ(value.GetUInteger(), word);
    return true;
}
};

START_TEST_DEFINE(TestIndexStatKps)
bool InitConfig() override {
    SetMergerParams(true, 1, -1, mcpNONE);
    return true;
}
bool Run() override {
    TVector<NRTYServer::TMessage> messages;
    GenerateInput(messages, 100, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed());

    TVector<NRTYServer::TMessage> messages1;
    TVector<NRTYServer::TMessage> messages2;
    for (size_t i = 0; i < messages.size(); i++) {
        if (i < messages.size() / 3)
            messages1.push_back(messages[i]);
        else
            messages2.push_back(messages[i]);
    }

    IndexMessages(messages1, DISK, 1, 0, false);
    ReopenIndexers();
    if (!CheckIndexStat(messages1)) {
        return false;
    }

    IndexMessages(messages2, DISK, 1, 0, false);
    ReopenIndexers();
    if (!CheckIndexStat(messages)) {
        return false;
    }

    Controller->ProcessCommand("create_merger_tasks");
    Controller->ProcessCommand("do_all_merger_tasks");
    ReopenIndexers();
    if (!CheckIndexStat(messages)) {
        return false;
    }

    TSet<std::pair<ui64, TString>> deleted;
    DeleteSomeMessages(messages, deleted, REALTIME);
    if (!CheckIndexStat(messages)) {
        return false;
    }

    Controller->RestartServer();
    if (!CheckIndexStat(messages)) {
        return false;
    }

    return true;
}
bool CheckIndexStat(const TVector<NRTYServer::TMessage>& messages, const TSet<std::pair<ui64, TString>>& deleted = TSet<std::pair<ui64, TString>>()) {
    TString stat = Query("/?info=indexstat");
    INFO_LOG << stat << Endl;
    TStringInput in(stat);
    NJson::TJsonValue json;
    CHECK_TEST_EQ(ReadJsonTree(&in, &json), true);

    NJson::TJsonValue value;
    if (GetIsPrefixed()) {
        CHECK_TEST_EQ(json.GetValueByPath("KPS", value), true);

        TMap<i64, i64> prefixes;
        for (size_t i = 0; i < messages.size(); i++) {
            TString url = messages[i].GetDocument().GetUrl();
            ui64 kps = messages[i].GetDocument().GetKeyPrefix();
            if (deleted.find(std::make_pair(kps, url)) == deleted.end()) {
                prefixes[kps] += 1;
            }
        }

        auto kpsMap = value.GetMap();
        CHECK_TEST_EQ(prefixes.size(), kpsMap.size());
        for (auto&& p : prefixes) {
            CHECK_TEST_EQ(p.second, kpsMap[ToString(p.first)].GetIntegerRobust());
        }
    } else {
        CHECK_TEST_EQ(json.GetValueByPath("KPS", value), false);
    }
    return true;
}
};
