#include "load.h"

#include <saas/rtyserver_test/testerlib/rtyserver_test.h>
#include <saas/rtyserver_test/testerlib/globals.h>
#include <saas/protos/rtyserver.pb.h>

using namespace NRTYServer;

START_TEST_DEFINE(TestLoadUpTime)
    bool Run() override{
        //do nothing, it's start time measuring
        TJsonPtr infos = Controller->GetServerInfo();
        TStringStream ss;
        NJson::WriteJson(&ss, infos.Get());
        NOTICE_LOG << ss.Str() << Endl;

        return true;
    }
};

START_TEST_DEFINE_PARENT(TestLoadMergerTime, TestLoadCommon)
    bool Run() override{
        TJsonPtr infos = Controller->GetServerInfo();
        TStringStream ss;
        NJson::WriteJson(&ss, infos.Get());
        NOTICE_LOG << ss.Str() << Endl;
        SendProfSignals();

        Controller->ProcessCommand("create_merger_tasks");
        Controller->ProcessCommand("do_all_merger_tasks");
        SendProfSignals();

        infos = Controller->GetServerInfo();
        NJson::WriteJson(&ss, infos.Get());
        NOTICE_LOG << ss.Str() << Endl;
        return true;
    }
};
