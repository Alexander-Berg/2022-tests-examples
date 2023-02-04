#include "load.h"

#include <saas/rtyserver_test/testerlib/rtyserver_test.h>
#include <saas/rtyserver_test/testerlib/globals.h>
#include <saas/protos/rtyserver.pb.h>

#include <util/system/env.h>

using namespace NRTYServer;

SERVICE_TEST_RTYSERVER_DEFINE_PARENT(TestLoadIndexCommon, TestLoadCommon)
    ui16 IndexPort = 0;
    bool Run() override{
        IndexPort = Controller->GetConfig().Indexer.Port;
        SetEnv("INDEXER_PORT", ToString(IndexPort));
        if (GetEnv("LOG_PATH") == TString())
            SetEnv("LOG_PATH", NFs::CurrentWorkingDirectory());
        SendProfSignals();

        INFO_LOG << "running indexing..." << Endl;
        if (!DoRun()) {
            return false;
        }
        INFO_LOG << "indexing done" << Endl;

        SendProfSignals();
        TRY
            TString tassResult;
        Controller->ProcessQuery("/tass", &tassResult, "localhost", IndexPort + 3, false);
        INFO_LOG << "tass_signals: " << tassResult << Endl;
        CATCH("getting /tass")
        return true;
    }
virtual bool DoRun(){
    return false;
}
};

START_TEST_DEFINE_PARENT(TestLoadIndex, TestLoadIndexCommon)
    bool DoRun() override{
        if (!Callback->RunNode("run_dolb"))
            ythrow yexception() << "fail to run node" << Endl;
        Callback->StopNode("run_dolb");
        return true;
    }
};

START_TEST_DEFINE_PARENT(TestLoadIndexAndFake, TestLoadIndexCommon)
bool DoRun() override {
    if (!Callback->RunNode("run_dolb_fake"))
        ythrow yexception() << "fail to run fake dolb" << Endl;
    if (!Callback->RunNode("run_dolb"))
        ythrow yexception() << "fail to run indexing" << Endl;
    Callback->StopNode("run_dolb_fake");
    return true;
}
};

START_TEST_DEFINE_PARENT(TestLoadIndexAndRestart, TestLoadIndexCommon)
bool DoRun() override{
    if (!Callback->RunNode("run_dolb"))
        ythrow yexception() << "fail to run node" << Endl;
    sleep(10);
    NOTICE_LOG << "restarting indexerproxy..." << Endl;
    TString ans;
    Controller->ProcessQuery("/?command=restart", &ans, "localhost", IndexPort + 3, false);

    if (!Callback->StopNode("run_dolb"))
        ythrow yexception() << "fail to stop node" << Endl;
    return true;
}
};
