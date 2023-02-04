#include <saas/rtyserver_test/testerlib/rtyserver_test.h>

#include <library/cpp/digest/md5/md5.h>
#include <util/system/env.h>

namespace{
bool CheckErrorCodes(TVector<NRTYServer::TReply>& replies, int code){
    NJson::TJsonValue rep;
    for (auto i : replies){
           TStringInput si(i.GetStatusMessage());
           NJson::ReadJsonTree(&si, &rep);
           CHECK_TEST_FAILED(rep["http_code"].GetInteger() != code, "wrong code");
        }
    return true;
}
}

START_TEST_DEFINE(TestIprErrorNoRabbit)
bool Run() override{
    if (Controller->GetActiveBackends().size() != 2)
        ythrow yexception() << "incorrect backends number for this test, must be 2, found " << Controller->GetActiveBackends().size();
    const int CountMessages = 10;
    TVector<NRTYServer::TMessage> messages;
    GenerateInput(messages, CountMessages, NRTYServer::TMessage::MODIFY_DOCUMENT, GetIsPrefixed());
    TVector<NRTYServer::TReply> replies = IndexMessages(messages, REALTIME, 1);

    DEBUG_LOG << "Stopping one backend..." << Endl;
    Controller->ProcessCommand("stop", TBackendProxy::TBackendSet(0));
    DEBUG_LOG << "Stopping one backend...Ok" << Endl;

    replies = IndexMessages(messages, REALTIME, 1, 0, false, true, TDuration(), TDuration(), 1, "tests", 0, true);
    CHECK_TEST_FAILED(!CheckErrorCodes(replies, 513), "wrong 'cant queue' code, must be 513");

    DEBUG_LOG << "Stopping second backend..." << Endl;
    Controller->ProcessCommand("stop", TBackendProxy::TBackendSet(1));
    DEBUG_LOG << "Stopping second backend...Ok" << Endl;

    replies = IndexMessages(messages, REALTIME, 1, 0, false, true, TDuration(), TDuration(), 1, "tests", 0, true);
    CHECK_TEST_FAILED(!CheckErrorCodes(replies, 598), "wrong 'unavailable' code, must be 598");
    return true;
}
};

START_TEST_DEFINE(TestIprErrorDiffAnswers)
bool Run() override{
    const int CountMessages = 10;
    TVector<NRTYServer::TMessage> messages;
    GenerateInput(messages, CountMessages, NRTYServer::TMessage::MODIFY_DOCUMENT, GetIsPrefixed());
    TVector<NRTYServer::TReply> replies = IndexMessages(messages, REALTIME, 1, 0, false, true, TDuration(), TDuration(), 1, "tests", 0, true);
    CHECK_TEST_FAILED(!CheckErrorCodes(replies, 512), "wrong 'different answers' code, must be 512");
    return true;
}
bool InitConfig() override{
    if (Controller->GetActiveBackends().size() < 2)
        ythrow yexception() << "incorrect backends number for this test, must be at least 2, found " << Controller->GetActiveBackends().size();
    TConfigFieldsPtr diff(new TConfigFields);
    (*diff)["ShardsNumber"] = 1;
    (*diff)["IsPrefixedIndex"] = true;
    Controller->ApplyConfigDiff(diff, TBackendProxy::TBackendSet(0));
    (*diff)["IsPrefixedIndex"] = false;
    Controller->ApplyConfigDiff(diff, TBackendProxy::TBackendSet(1));
    return true;
}
};

START_TEST_DEFINE(TestIprVoid)
bool Run() override{
    const int CountMessages = 1;
    TVector<NRTYServer::TMessage> messages;
    GenerateInput(messages, CountMessages, NRTYServer::TMessage::MODIFY_DOCUMENT, GetIsPrefixed());
    TVector<NRTYServer::TReply> replies = IndexMessages(messages, REALTIME, 1, 0, false, true, TDuration(), TDuration(), 1, "tests", 0, true);
    CHECK_TEST_FAILED(!CheckErrorCodes(replies, 302), "wrong 'void' code, must be 302");
    return true;
}
};

START_TEST_DEFINE(TestIprDistributorPartial)
TVector<NRTYServer::TReply> IndexOneDoc() {
    const int CountMessages = 1;
    TVector<NRTYServer::TMessage> messages;
    GenerateInput(messages, CountMessages, NRTYServer::TMessage::MODIFY_DOCUMENT, GetIsPrefixed());
    return IndexMessages(messages, REALTIME, 1, 0, false, true, TDuration(), TDuration(), 1, "tests", 0, true);
}
bool Run() override{
    auto allActive = IndexOneDoc();
    CHECK_TEST_FAILED(!CheckErrorCodes(allActive, 200), "wrong Ok code, must be 200");

    Callback->StopNode("monolith", TNODE_DISTRIBUTOR);
    auto oneInactive = IndexOneDoc();
    CHECK_TEST_FAILED(!CheckErrorCodes(oneInactive, 206), "wrong Partial code, must be 206");

    Callback->StopNode("monolith2", TNODE_DISTRIBUTOR);
    auto twoInactive = IndexOneDoc();
    CHECK_TEST_FAILED(!CheckErrorCodes(twoInactive, 513), "wrong Cannot put to queue code, must be 513");

    Callback->StopNode("monolith3", TNODE_DISTRIBUTOR);
    auto allInactive = IndexOneDoc();
    CHECK_TEST_FAILED(!CheckErrorCodes(allInactive, 513), "wrong Cannot put to queue code, must be 513");
    return true;
}
bool InitConfig() override {
    (*IPConfigDiff)["Services.tests.Quorum"] = 0.5f;
    return true;
}
};

START_TEST_DEFINE(TestIprDistributorTooBig)
bool Run() override {
    const int CountMessages = 1;
    TVector<NRTYServer::TMessage> messages;
    GenerateInput(messages, CountMessages, NRTYServer::TMessage::MODIFY_DOCUMENT, GetIsPrefixed());
    TString hugeBody(10 * 1024 * 1024, '1');
    messages[0].MutableDocument()->SetBody(hugeBody);
    TVector<NRTYServer::TReply> replies = IndexMessages(messages, REALTIME, 1, 0, false, true, TDuration(), TDuration(), 1, "tests", 0, true);
    CHECK_TEST_FAILED(!CheckErrorCodes(replies, 400), "wrong 'distributor partial' code, must be 400");
    return true;
}
};

START_TEST_DEFINE(TestIprDistributorQueue)
bool Run() override {
    const int CountMessages = 15;
    TVector<NRTYServer::TMessage> messages;
    GenerateInput(messages, CountMessages, NRTYServer::TMessage::MODIFY_DOCUMENT, GetIsPrefixed());
    const TString& prefix = GetIsPrefixed() ? "&kps=1" : "";
    if (prefix) {
        for (auto&& message: messages)
            message.MutableDocument()->SetKeyPrefix(1);
    }

    Callback->StopNode("monolith", TNODE_DISTRIBUTOR);
    TVector<NRTYServer::TReply> replies = IndexMessages(messages, REALTIME, 1, 0, false, true, TDuration(), TDuration(), 1, "tests", 0, true);

    auto address = Controller->GetConfig().Indexer;
    auto result  = Controller->ProcessCommandOneHost("get_info_server", address.Host, address.Port + 3);
    if (!result) {
        ythrow yexception() << "cannot get indexerproxy info_server";
    }
    DEBUG_LOG << result->GetStringRobust() << Endl;
    auto queue = (*result)["result"]["queues"]["tests"]["tests"]["docs"].GetUIntegerRobust();
    if (!queue) {
        ythrow yexception() << "queue should not be empty";
    }

    Callback->RestartNode("monolith", TNODE_DISTRIBUTOR);

    TVector<TDocSearchInfo> results;

    TQuerySearchContext context;
    context.ResultCountRequirement = CountMessages;
    context.AttemptionsCount = 20;
    context.PrintResult = true;

    QuerySearch("url:\"*\"" + prefix, results, context);
    if (results.size() != CountMessages) {
        ythrow yexception() << "expected exactly 1 result";
    }

    return true;
}
};

START_TEST_DEFINE(TestIprAuth)
bool Run() override{
    const int CountMessages = 5;
    TVector<NRTYServer::TMessage> messages;
    bool prefixed = GetIsPrefixed();
    int keyPrefix = prefixed ? 1234 : 0;
    GenerateInput(messages, CountMessages, NRTYServer::TMessage::MODIFY_DOCUMENT, prefixed);
    if (prefixed){
        for(ui32 i = 0; i < messages.size(); ++i){
            messages[i].MutableDocument()->SetKeyPrefix(keyPrefix);
        }
    }

    DEBUG_LOG << "checking unauthorized indexing case..." << Endl;
    TVector<NRTYServer::TReply> replies = IndexMessages(messages, REALTIME, 1, 0,
                                                        false, true, TDuration(), TDuration(), 1,
                                                        "tests", 0, true);
    CHECK_TEST_FAILED(!CheckErrorCodes(replies, 401), "wrong 'unauthorized' code, must be 401");
    CheckCount(0);
    DEBUG_LOG << "checking unauthorized indexing case... Ok" << Endl;

    DEBUG_LOG << "checking authorized indexing case..." << Endl;
    TString auth = MD5::Calc("cookie" + MD5::Calc("tests " + ToString(keyPrefix)));
    IndexMessages(messages, REALTIME, 1, 0,
                  false, true, TDuration(), TDuration(), 1,
                  "tests", 0, true,
                  "&auth=" + auth);
    CheckCount(CountMessages);
    DEBUG_LOG << "checking authorized indexing case... Ok" << Endl;

    TVector<TDocSearchInfo> searchResults;
    TString searchQuery = "url:\"*\"&kps=" + ToString(keyPrefix);

    DEBUG_LOG << "checking unauthorized search case..." << Endl;
    MUST_BE_BROKEN(QuerySearch(searchQuery, searchResults));
    DEBUG_LOG << "checking unauthorized search case... Ok" << Endl;

    DEBUG_LOG << "checking authorized search case..." << Endl;
    ui16 code = QuerySearch(searchQuery + "&auth=" + auth, searchResults);
    CHECK_TEST_FAILED(code != 200, "wrong search ok code");
    CHECK_TEST_FAILED(searchResults.size() != CountMessages, "wrong results count: " + ToString(searchResults.size()));
    DEBUG_LOG << "checking authorized search case... Ok" << Endl;
    return true;
    }
};

START_TEST_DEFINE(TestIprUsererr)
bool Run() override{
    DEBUG_LOG << "running incorrect service case..." << Endl;
    const int CountMessages = 1;
    TVector<NRTYServer::TMessage> messages;
    GenerateInput(messages, CountMessages, NRTYServer::TMessage::MODIFY_DOCUMENT, GetIsPrefixed());
    TVector<NRTYServer::TReply> replies = IndexMessages(messages, REALTIME, 1, 0, false, true, TDuration(), TDuration(), 1, "tests1", 0, true);
    CHECK_TEST_FAILED(!CheckErrorCodes(replies, 400), "wrong 'incorrect service hash' code, must be 400");
    replies = IndexMessages(messages, REALTIME, 1, 0, false, true, TDuration(), TDuration(), 1, "tests", 0, true);
    CHECK_TEST_FAILED(!CheckErrorCodes(replies, 200), "wrong 'ok' code, must be 200");
    DEBUG_LOG << "incorrect service case.. Ok" << Endl;

    ui16 port = Controller->GetConfig().Indexer.Port;
    SetEnv("INDEXER_PORT", ToString(port));
    DEBUG_LOG << "running incorrect json case..." << Endl;
    if (!Callback->RunNode("test_incorrect_json"))
        ythrow yexception() << "incorrect json case failed";
    Callback->WaitNode("test_incorrect_json");
    DEBUG_LOG << "incorrect json case.. Ok" << Endl;

    DEBUG_LOG << "running incorrect request case..." << Endl;
    if (!Callback->RunNode("test_badrequest"))
        ythrow yexception() << "bad request case failed";
    Callback->WaitNode("test_badrequest");
    DEBUG_LOG << "incorrect request case.. Ok" << Endl;

    DEBUG_LOG << "running mixed body&children case..." << Endl;
    if (!Callback->RunNode("test_mix_body_children"))
        ythrow yexception() << "mix body&children case failed";
    Callback->WaitNode("test_mix_body_children");
    DEBUG_LOG << "mixed body&children case.. Ok" << Endl;

    return true;
}
};

START_TEST_DEFINE(TestIprInconsistency)
bool Run() override {
    if (Controller->GetActiveBackends().size() < 2)
        ythrow yexception() << "incorrect backends number for this test, must be at least 2, found " << Controller->GetActiveBackends().size();

    const int CountMessages = 1;
    TVector<NRTYServer::TMessage> messages;
    GenerateInput(messages, CountMessages, NRTYServer::TMessage::MODIFY_DOCUMENT, GetIsPrefixed());
    messages[0].MutableDocument()->SetVersion(100);
    Controller->ProcessCommand("stop", TBackendProxy::TBackendSet(0));

    TVector<NRTYServer::TReply> replies = IndexMessages(messages, REALTIME, 1, 0, false, true, TDuration(), TDuration(), 1, "tests", 0, true);
    CHECK_TEST_FAILED(!CheckErrorCodes(replies, 513), "wrong 'cant store in q' code, must be 513");

    Controller->ProcessCommand("restart", TBackendProxy::TBackendSet(0));

    messages[0].MutableDocument()->SetVersion(1);
    replies = IndexMessages(messages, REALTIME, 1, 0, false, true, TDuration(), TDuration(), 1, "tests", 0, true);
    CHECK_TEST_FAILED(!CheckErrorCodes(replies, 409), "wrong 'inconsistency' code, must be 409");

    return true;
}
};

