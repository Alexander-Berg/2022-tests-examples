#include <saas/rtyserver_test/testerlib/rtyserver_test.h>
#include <saas/rtyserver_test/util/tass_parsers.h>
#include <library/cpp/json/json_reader.h>

START_TEST_DEFINE(TestSearchProxyBroadcast)
    size_t CountMessages = 10;

void CheckBackends(TString res, bool allSlotsOk=true, bool isPing=false){
    NJson::TJsonValue result;
    TStringInput si(res);
    if (!NJson::ReadJsonTree(&si, &result))
        ythrow yexception() << "incorrect json from broadcast: " << res;
    if (isPing)
        return;

    TMap<TString, NJson::TJsonValue> replies;
    for (auto&& reply : result.GetArray()) {
        const TString& slot = reply["Host"].GetString() + ":" + reply["SearchPort"].GetStringRobust();
        replies[slot] = reply["Response"];
    }
    for (size_t i = 0; i < Controller->GetConfig().Controllers.size(); ++i){
        TString slot = Controller->GetConfig().Controllers[i].Host + ":" + ToString(Controller->GetConfig().Controllers[i].Port - 3);
        if (!replies.contains(slot))
            ythrow yexception() << "not found slot '" << slot << "' in reply: " << res;

        if (allSlotsOk){
            size_t docs = replies[slot]["docs_in_disk_indexers"].GetUInteger();
            if (docs != CountMessages)
                ythrow yexception() << "incorrect docs_in_disk_indexers: " << docs << " != " << CountMessages << ", res: " << res;
        }
    }
}

bool Run() override {
    DEBUG_LOG << "start_test\n";
    TVector<NRTYServer::TMessage> messages;
    GenerateInput(messages, CountMessages, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed());
    IndexMessages(messages, DISK, 1);

    TString res;
    DEBUG_LOG << "Check /broadcast/..." << Endl;
    ProcessQuery("/broadcast/?info_server=yes&service=tests", &res);
    CheckBackends(res);
    ProcessQuery("/broadcast/?info_server=yes&fake_cgi_service=", &res);
    CheckBackends(res);
    ui32 code = ProcessQuery("/broadcast/?info_server=yes&service=somefiga", &res);
    if (code != 404)
        ythrow yexception() << "incorrect service: expected 404 code, found " << code;
    DEBUG_LOG << "Check /broadcast/... Ok" << Endl;
    CHECK_TEST_EQ(ProcessQuery("/broadcast/?text=url:%22*%22&service=tests", &res), 200);

    DEBUG_LOG << "Check /global_ping/..." << Endl;
    ProcessQuery("/global_ping/?service=tests", &res);
    CheckBackends(res, true, true);
    ProcessQuery("/global_ping/?fake_cgiservice=tests", &res);
    CheckBackends(res, true, true);
    DEBUG_LOG << "Check /global_ping/... Ok" << Endl;

    DEBUG_LOG << "Check with one sleepy backend..." << Endl;
    Controller->ProcessCommand("stop", TBackendProxy::TBackendSet(0), "");
    ProcessQuery("/broadcast/?info_server=yes&service=tests&complete=yes", &res);
    CheckBackends(res, false, false);
    ProcessQuery("/global_ping/?service=tests", &res);
    CheckBackends(res, true, true);
    DEBUG_LOG << "Check with one sleepy backend... Ok" << Endl;
    if (Controller->GetConfig().Controllers.size() == 2){
        Controller->ProcessCommand("stop", TBackendProxy::TBackendSet(1), "");
        ProcessQuery("/global_ping/?service=tests", &res);
        CheckBackends(res, false, true);
    }
    return true;
}
};

START_TEST_DEFINE(TestSearchProxyReturnCodes)
bool Run() override {
    TVector<NRTYServer::TMessage> messages;
    GenerateInput(messages, 1, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed());
    IndexMessages(messages, REALTIME, 1);

    TString reply;
    TString kps = GetAllKps(messages);

    const ui32 ok = ProcessQuery("/?text=body" + kps, &reply);
    if (ok != 200)
        ythrow yexception() << "incorrect HTTP code for Ok: " << ok;

    const ui32 emptySet = ProcessQuery("/?text=yandex", &reply);
    if (emptySet != 404)
        ythrow yexception() << "incorrect HTTP code for Ok: " << emptySet;

    const ui32 syntaxError = ProcessQuery("/?text=\"", &reply);
    if (syntaxError != 414)
        ythrow yexception() << "incorrect HTTP code for SyntaxError: " << syntaxError;

    const ui32 emptyRequest = ProcessQuery("/?text=", &reply);
    if (emptyRequest != 413)
        ythrow yexception() << "incorrect HTTP code for EmptyRequest: " << emptyRequest;

    const ui32 timeout = ProcessQuery("/?text=yandex&timeout=500000&delay=sp_postsearch.1000000", &reply);
    if (timeout != 402)
        ythrow yexception() << "incorrect HTTP code for Timeout: " << timeout;

    Controller->ProcessCommand("stop", TBackendProxy::TBackendSet(0), "");
    const ui32 incomplete = ProcessQuery("/?text=yandex", &reply);
    if (incomplete != 502)
        ythrow yexception() << "incorrect HTTP code for Incomplete: " << incomplete;

    TString tassResult;
    Controller->ProcessQuery("/tass", &tassResult, "localhost", Controller->GetConfig().Searcher.Port + 3, false);
    DEBUG_LOG << tassResult;
    i64 incompleteSignalCnt = 0;
    TRTYTassParser::GetTassValue(tassResult, "search-CTYPE-incomplete_dmmm", &incompleteSignalCnt);
    Y_ENSURE(incompleteSignalCnt == 1, "incorrect signal value for incomplete");

    //const ui32 unknown = ProcessQuery("/?text=yandex&timeout=1", &reply);
    //if (unknown != 400)
    //    ythrow yexception() << "incorrect HTTP code for Unknown: " << unknown;

    return true;
}

bool InitConfig() override{
    (*SPConfigDiff)["SearchConfig.HttpStatuses.EmptySetStatus"] = "404";
    (*SPConfigDiff)["SearchConfig.HttpStatuses.SyntaxErrorStatus"] = "414";
    (*SPConfigDiff)["SearchConfig.HttpStatuses.EmptyRequestStatus"] = "413";
    (*SPConfigDiff)["SearchConfig.HttpStatuses.UnknownErrorStatus"] = "400";
    (*SPConfigDiff)["SearchConfig.HttpStatuses.TimeoutStatus"] = "402";
    (*SPConfigDiff)["SearchConfig.HttpStatuses.IncompleteStatus"] = "502";
    return true;
}
};

START_TEST_DEFINE(TestSearchProxyMisspell)
bool Run() override {
    TVector<NRTYServer::TMessage> messages;
    GenerateInput(messages, 1, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed());
    IndexMessages(messages, REALTIME, 1);

    TString Kps = GetAllKps(messages);
    TVector<TDocSearchInfo> results;
    QuerySearch("body" + Kps, results, nullptr, nullptr, true);
    Y_ENSURE(results.size(), "no results body-default-msp");
    QuerySearch("body&msp=force" + Kps, results, nullptr, nullptr, true);
    Y_ENSURE(results.size(), "no results body-force-msp");
    QuerySearch("body&msp=no" + Kps, results, nullptr, nullptr, true);
    Y_ENSURE(results.size(), "no results body-no-msp");
    QuerySearch("body&msp=try_at_first" + Kps, results, nullptr, nullptr, true);
    Y_ENSURE(results.size(), "no results body-try_at_first-msp");

    QuerySearch("bodi" + Kps, results, nullptr, nullptr, true);
    Y_ENSURE(results.size(), "no results bodi-default-msp");
    QuerySearch("bodi&msp=force" + Kps, results, nullptr, nullptr, true);
    Y_ENSURE(results.size(), "no results bodi-force-msp");
    QuerySearch("bodi&msp=no" + Kps, results, nullptr, nullptr, true);
    Y_ENSURE(!results.size(), "no results bodi-no-msp");
    QuerySearch("bodi&msp=try_at_first" + Kps, results, nullptr, nullptr, true);
    Y_ENSURE(results.size(), "no results bodi-try_at_first-msp");

    return true;
}
};

