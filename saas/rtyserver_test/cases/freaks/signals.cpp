#include <saas/rtyserver_test/testerlib/rtyserver_test.h>
#include <util/system/fs.h>

START_TEST_DEFINE(TestSighup)
bool Run() override{
    //rm log; signup; check log
    ui32 sighup_num;
#if defined(_unix_)
    sighup_num = SIGHUP;
#else
    ythrow yexception() << "test is for unix only" << Endl;
#endif
    TString slog = Controller->GetConfigValue("LoggerType", "DaemonConfig", TBackendProxy::TBackendSet(), TNODE_SEARCHPROXY);
    CHECK_TEST_FAILED(!NFs::Exists(slog), "searchproxy log " + slog + " does not exist");
    NFs::Remove(slog);
    bool res = Callback->SendSignalNode("*", TNODE_SEARCHPROXY, sighup_num);
    CHECK_TEST_FAILED(!res, "signal not sent");

    TString ilog = Controller->GetConfigValue("LoggerType", "DaemonConfig", TBackendProxy::TBackendSet(), TNODE_INDEXERPROXY);
    CHECK_TEST_FAILED(!NFs::Exists(ilog), "indexerproxy log " + ilog + " does not exist");
    NFs::Remove(ilog);
    res = Callback->SendSignalNode("*", TNODE_INDEXERPROXY, sighup_num);
    CHECK_TEST_FAILED(!res, "signal not sent");
    Sleep(TDuration::Seconds(3));

    CHECK_TEST_FAILED(!NFs::Exists(slog), "searchproxy log " + slog + " hasnt recovered after sighup");
    CHECK_TEST_FAILED(!NFs::Exists(ilog), "indexerproxy log " + ilog + " hasnt recovered after sighup");

    TString blog = Controller->GetConfigValue("LoggerType", "DaemonConfig", TBackendProxy::TBackendSet(), TNODE_RTYSERVER);
    CHECK_TEST_FAILED(!NFs::Exists(blog), "backend log " + blog + " does not exist");
    NFs::Remove(blog);
    res = Callback->SendSignalNode("*", TNODE_RTYSERVER, sighup_num);
    CHECK_TEST_FAILED(!res, "signal not sent");
    Sleep(TDuration::Seconds(3));
    CHECK_TEST_FAILED(!NFs::Exists(blog), "backend log " + blog + " hasnt recovered after sighup");
    return true;
}
};

START_TEST_DEFINE(TestSigint)
bool Run() override{
    ui32 sigint_num;
#if defined(_unix_)
    sigint_num = SIGINT;
#else
    ythrow yexception() << "test is for unix only" << Endl;
#endif
    bool res = Callback->SendSignalNode("*", TNODE_INDEXERPROXY, sigint_num);
    CHECK_TEST_FAILED(!res, "signal not sent");
    DEBUG_LOG << "waiting started" << Endl;
    Callback->WaitNode("*", TNODE_INDEXERPROXY);
    DEBUG_LOG << "waiting stopped" << Endl;
    return true;
}
};
