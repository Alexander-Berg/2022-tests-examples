#include <saas/rtyserver_test/testerlib/rtyserver_test.h>
#include <kernel/multipart_archive/multipart.h>
#include <util/system/thread.h>

#include <atomic>

SERVICE_TEST_RTYSERVER_DEFINE(TestPartialBackends)
void StartOrStopBackend(int bn, bool start){
    TBackendProxy::TBackendSet ab = Controller->GetActiveBackends();
    DEBUG_LOG << (start ? "Starting" : "Stopping") << " backend " << bn << "..." << Endl;
    Controller->ProcessCommand(start ? "restart" : "stop", TBackendProxy::TBackendSet(bn));
    DEBUG_LOG << (start ? "Starting" : "Stopping") << " backend " << bn << "... Ok" << Endl;
    if (start)
        ab.insert(bn);
    else
        ab.erase(bn);
    Controller->SetActiveBackends(ab);
}
void StartBackend(int bn){
    StartOrStopBackend(bn, true);
}
void StopBackend(int bn){
    StartOrStopBackend(bn, false);
}
ui32 SafeQueryCount() {
    try {
        return QueryCount();
    } catch(...) {
    }
    return 0;
}

void CheckCountWithWait(int count) {
    while (SafeQueryCount() < (ui32)count)
        Sleep(TDuration::Seconds(1));
    CheckCount(count);
}

};

static ui32 GetQueueSize(NDaemonController::TControllerAgent& controller) {
    TString reply;
    TString command = "?command=get_info_server";
    controller.ExecuteCommand(command, reply, 3000, 1, "");
    NJson::TJsonValue json;
    TStringStream ss(reply);
    NJson::ReadJsonTree(&ss, &json);
    return json.GetMap().at("result").GetMap().at("queues").GetMap().at("common").GetMap().at("docs").GetUInteger();
}

static bool CheckStarted(NDaemonController::TControllerAgent& controller) {
    TString reply;
    TString command = "?command=get_info_server";
    controller.ExecuteCommand(command, reply, 3000, 1, "");
    NJson::TJsonValue json;
    TStringStream ss(reply);
    NJson::ReadJsonTree(&ss, &json);
    return json.GetMap().at("result").GetMap().at("controller_status") == "Active";
}

static void RestartProxy(NDaemonController::TControllerAgent& controller) {
    INFO_LOG << "Restarting proxy..." << Endl;
    TString restartCommand = "?command=restart";
    TString reply;
    controller.ExecuteCommand(restartCommand, reply, 3000, 1, "");

    while (!CheckStarted(controller)) {
        Sleep(TDuration::MilliSeconds(30));
    }
}

START_TEST_DEFINE_PARENT(TestIndexerProxyRestart, TestPartialBackends)
bool Run() override {
    if (Controller->GetActiveBackends().size() != 2)
        ythrow yexception() << "incorrect backends number for this test, must be 2, found " << Controller->GetActiveBackends().size();

    StopBackend(0);
    const int CountMessages = 1000;
    TVector<NRTYServer::TMessage> messages;
    GenerateInput(messages, CountMessages, NRTYServer::TMessage::MODIFY_DOCUMENT, GetIsPrefixed());
    IndexMessages(messages, REALTIME, 1, 0, true);

    NDaemonController::TControllerAgent IPAgent(Controller->GetConfig().Indexer.Host, Controller->GetConfig().Indexer.Port + 3);
    CHECK_TEST_EQ(GetQueueSize(IPAgent), CountMessages);

    RestartProxy(IPAgent);

    CHECK_TEST_EQ(GetQueueSize(IPAgent), CountMessages);

    {
        TVector<NRTYServer::TMessage> messagesPart;
        GenerateInput(messagesPart, CountMessages, NRTYServer::TMessage::MODIFY_DOCUMENT, GetIsPrefixed());
        IndexMessages(messagesPart, REALTIME, 1, 0, true);
        messages.insert(messages.end(), messagesPart.begin(), messagesPart.end());
    }

    CHECK_TEST_EQ(GetQueueSize(IPAgent), 2 * CountMessages);

    RestartProxy(IPAgent);

    CHECK_TEST_EQ(GetQueueSize(IPAgent), 2 * CountMessages);

    {
        TVector<NRTYServer::TMessage> messagesPart;
        GenerateInput(messagesPart, CountMessages, NRTYServer::TMessage::MODIFY_DOCUMENT, GetIsPrefixed());
        IndexMessages(messagesPart, REALTIME, 1, 0, true);
        messages.insert(messages.end(), messagesPart.begin(), messagesPart.end());
    }

    CHECK_TEST_EQ(GetQueueSize(IPAgent), 3 * CountMessages);

    StopBackend(1);
    StartBackend(0);

    while (GetQueueSize(IPAgent) != 0) {
        Sleep(TDuration::Seconds(1));
        INFO_LOG << "Waiting queue empty (" << GetQueueSize(IPAgent) << ")" << Endl;
    }

    CheckSearchResults(messages);

    return true;
}

bool InitConfig() override {
    (*IPConfigDiff)["Dispatcher.DeferredMQ.AsyncMode"] = false;
    return true;
}
};

START_TEST_DEFINE_PARENT(TestSearchOneOfTwo, TestPartialBackends)
bool Run() override{
    if (Controller->GetActiveBackends().size() != 2)
        ythrow yexception() << "incorrect backends number for this test, must be 2, found " << Controller->GetActiveBackends().size();
    const int CountMessages = 13;
    TVector<NRTYServer::TMessage> messages;
    GenerateInput(messages, CountMessages, NRTYServer::TMessage::MODIFY_DOCUMENT, GetIsPrefixed());
    IndexMessages(messages, REALTIME, 1);

    StopBackend(0);

    CheckSearchResults(messages);

    StartBackend(0);
    StopBackend(1);

    CheckSearchResults(messages);
    return true;
}
};

START_TEST_DEFINE_PARENT(TestDelOneOfTwo, TestPartialBackends)
bool Run() override{
    if (Controller->GetActiveBackends().size() < 2)
        ythrow yexception() << "incorrect backends number for this test, must be at least 2, found " << Controller->GetActiveBackends().size();
    const int CountMessages = 13;
    TVector<NRTYServer::TMessage> messages;
    GenerateInput(messages, CountMessages, NRTYServer::TMessage::MODIFY_DOCUMENT, GetIsPrefixed());
    int keyPrefix = GetIsPrefixed() ? 10 : 0;
    if (keyPrefix)
        for (ui32 i = 0; i < messages.size(); ++i) {
                messages[i].MutableDocument()->SetKeyPrefix(keyPrefix);
        }

    IndexMessages(messages, REALTIME, 1);

    StopBackend(0);

    CheckCount(CountMessages);
    SendIndexReply = false;
    DeleteSpecial(keyPrefix);
    CheckCountWithWait(0);
    TVector<TDocSearchInfo> results;
    QuerySearch("url:\"*\"&kps=" + ToString(keyPrefix), results);
    CHECK_TEST_FAILED(results.size() != 0, "deletion doesn't work");

    return true;
}
};

START_TEST_DEFINE_PARENT(TestIndexQueueThreeBe, TestPartialBackends, TTestMarksPool::NeedRabbit)
bool Run() override{
    if (Controller->GetActiveBackends().size() != 3)
        ythrow yexception() << "incorrect backends number for this test, must be 3, found " << Controller->GetActiveBackends().size();
    const int CountMessages = 6;
    TVector<NRTYServer::TMessage> messages1, messages2;
    GenerateInput(messages1, CountMessages, NRTYServer::TMessage::MODIFY_DOCUMENT, GetIsPrefixed());
    GenerateInput(messages2, CountMessages, NRTYServer::TMessage::MODIFY_DOCUMENT, GetIsPrefixed());

    StopBackend(2);
    StopBackend(1);

    IndexMessages(messages1, REALTIME, 1, 0, false);
    Controller->ProcessCommand("wait_empty_indexing_queues", TBackendProxy::TBackendSet(0));
    CheckSearchResults(messages1);

    StartBackend(2);
    StopBackend(0);

    IndexMessages(messages2, REALTIME, 1, 0, false);
    CheckSearchResults(messages1);

    StartBackend(1);
    StopBackend(2);
    Controller->ProcessCommand("wait_empty_indexing_queues", TBackendProxy::TBackendSet(1));

    CheckSearchResults(messages2);
    CheckSearchResults(messages1);

    return true;
}
};

START_TEST_DEFINE_PARENT(TestIndexQueue, TestPartialBackends, TTestMarksPool::NeedRabbit)
bool Run() override{
    if (Controller->GetActiveBackends().size() < 2)
        ythrow yexception() << "incorrect backends number for this test, must be at least 2, found " << Controller->GetActiveBackends().size();
    const int CountMessages = 60;
    TVector<NRTYServer::TMessage> messages;
    GenerateInput(messages, CountMessages, NRTYServer::TMessage::MODIFY_DOCUMENT, GetIsPrefixed());

    StopBackend(0);
    IndexMessages(messages, REALTIME, 1, 0, false);
    StartBackend(0);

    CheckCountWithWait(CountMessages);

    return true;
}
};

START_TEST_DEFINE_PARENT(TestDelQueue, TestPartialBackends, TTestMarksPool::NeedRabbit)
bool Run() override{
    if (Controller->GetActiveBackends().size() < 2)
        ythrow yexception() << "incorrect backends number for this test, must be at least 2, found " << Controller->GetActiveBackends().size();
    const int CountMessages1 = 26, CountMessages2 = 12;
    TVector<NRTYServer::TMessage> messages1, messages2, messagesDeleted;
    GenerateInput(messages1, CountMessages1, NRTYServer::TMessage::MODIFY_DOCUMENT, GetIsPrefixed());
    GenerateInput(messages2, CountMessages2, NRTYServer::TMessage::MODIFY_DOCUMENT, GetIsPrefixed());

    IndexMessages(messages1, REALTIME, 1);
    IndexMessages(messages2, REALTIME, 1);

    StopBackend(1);
    for (unsigned i = 0; i < messages1.size(); ++i) {
        messagesDeleted.push_back(BuildDeleteMessage(messages1[i]));
    }
    IndexMessages(messagesDeleted, REALTIME, 1, 0, false);
    StartBackend(1);
    CheckCountWithWait(CountMessages2);

    return true;
}
};

START_TEST_DEFINE_PARENT(TestDelAsync, TestPartialBackends)
private:
    struct TDelProcParams {
        TString Query;
        TTestDelAsyncCaseClass* This;
        bool Success;
    };
    static void* TestDelAsyncProc(void* query) {
        TRY
            TDelProcParams* params = (TDelProcParams*)query;
            params->Success = false;
            params->This->DeleteQueryResult(params->Query, REALTIME);
            params->Success = true;
        CATCH("TestDelAsyncProc");
        return nullptr;
    }
public:
bool Run() override{
    if (Controller->GetActiveBackends().size() < 2)
        ythrow yexception() << "incorrect backends number for this test, must be at least 2, found " << Controller->GetActiveBackends().size();
    const int CountMessages1 = 180, CountMessages2 = 12;
    TVector<NRTYServer::TMessage> messages1, messages2;
    GenerateInput(messages1, CountMessages1, NRTYServer::TMessage::MODIFY_DOCUMENT, GetIsPrefixed());
    GenerateInput(messages2, CountMessages2, NRTYServer::TMessage::MODIFY_DOCUMENT, GetIsPrefixed());

    int keyPrefix = GetIsPrefixed() ? 10 : 0;
    for (ui32 i = 0; i < messages1.size(); ++i) {
        messages1[i].MutableDocument()->SetBody("body-to-delete sometext");
        if (keyPrefix)
            messages1[i].MutableDocument()->SetKeyPrefix(keyPrefix);
    }

    IndexMessages(messages1, REALTIME, 1);
    IndexMessages(messages2, REALTIME, 1);

    StopBackend(1);

    TDelProcParams params;
    params.Success = false;
    params.Query = "sometext&kps=" + ToString(keyPrefix);
    params.This = this;
    TThread delThread(&TestDelAsyncProc, &params);
    delThread.Start();

    Sleep(TDuration::Seconds(1));
    StartBackend(1);
    delThread.Join();
    if (!params.Success)
        ythrow yexception() << "Deletion thread fails";
    CheckCountWithWait(CountMessages2);

    return true;
}
};

START_TEST_DEFINE_PARENT(TestFlapBackend, TestPartialBackends, TTestMarksPool::NeedRabbit)

class TFlapper : IThreadFactory::IThreadAble {
public:
    TFlapper(TBackendProxy& controller)
        : Controller(controller)
        , Stopped(false)
        , Thread(SystemThreadFactory()->Run(this))
    {}

    virtual ~TFlapper() {
        Stop();
    }

    void DoExecute() override {
        while(!Stopped) {
            Controller.ProcessCommand("disable_indexing", 0);
            if (Wait())
                return;
            Controller.ProcessCommand("enable_indexing", 0);
            Wait();
        }
    }
    void Stop() {
        Stopped = true;
        StopEvent.Signal();
        Thread->Join();
    }

private:
    inline bool Wait() {
        StopEvent.WaitT(TDuration::MilliSeconds(500));
        return Stopped;
    }
    TBackendProxy& Controller;
    std::atomic<bool> Stopped;
    TAutoEvent StopEvent;
    TAutoPtr<IThreadFactory::IThread> Thread;
};

bool Run() override{
    if (Controller->GetActiveBackends().size() < 2)
        ythrow yexception() << "incorrect backends number for this test, must be at least 2, found " << Controller->GetActiveBackends().size();
    const int CountMessages = 10000;
    TVector<NRTYServer::TMessage> messages;
    GenerateInput(messages, CountMessages, NRTYServer::TMessage::MODIFY_DOCUMENT, GetIsPrefixed());

    StopBackend(0);
    IndexMessages(messages, REALTIME, 1, 0, false);
    StartBackend(0);
    TFlapper flapper(*Controller);
    CheckCountWithWait(CountMessages);
    StopBackend(1);
    CheckSearchResults(messages);
    return true;
}
};

