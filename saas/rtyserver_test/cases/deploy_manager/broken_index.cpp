#include "deploy_manager.h"
#include <saas/library/searchmap/searchmap.h>
#include <saas/deploy_manager/scripts/cluster_control/action/action.h>

SERVICE_TEST_RTYSERVER_DEFINE_PARENT(TestBrokenIndex, TestDeployManager)

    NSearchMapParser::TSlotsPool Pool;

    void InitCluster() override {
        UploadCommon();
        UploadService("tests");
        ConfigureCluster(2, 1, NSaas::UrlHash, "rtyserver", "tests");
        DeployBe("tests");
        DeployIp();
        DeploySp();
    }

    virtual void BreakIndex(const TFsPath& path) = 0;

    bool Run() override {
        i64 countMessages = 10;
        TVector<NRTYServer::TMessage> messages;
        GenerateInput(messages, countMessages, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed());
        MUSTNT_BE_BROKEN(IndexMessages(messages, DISK, 3));
        ReopenIndexers();
        ui32 backend = *Controller->GetActiveBackends().begin();
        TJsonPtr indexes = Controller->ProcessCommand("get_final_indexes&full_path=true", backend);
        Controller->ProcessCommand("stop", backend);

        NJson::TJsonValue::TArray dirs;
        dirs.clear();

        for (const auto& dirVal: indexes->GetArray()) {
            for (const auto& dir : dirVal["dirs"].GetArray()) {
                BreakIndex(dir.GetString());
            }
        }
        NOTICE_LOG << "DEPLOY AFTER BREAK" << Endl;
        try {
            DeployBe("tests");
            TEST_FAILED("error not detected");
        } catch (...) {
        }
        NOTICE_LOG << "RESTART AFTER BREAK" << Endl;
        TString res = Controller->SendCommandToDeployManager("broadcast?command=get_status&ctype=sbtests&filter=$status$");
        NOTICE_LOG << "INFO AFTER DEPLOY: " << res << Endl;
        MUST_BE_BROKEN(Controller->RestartBackend(backend));
        NOTICE_LOG << "RESTART AFTER BREAK OK" << Endl;

        TInstant start = Now();
        bool atFirst = true;
        while (Now() - start < TDuration::Seconds(300)) {
            try {
                Sleep(TDuration::Seconds(2));
                TString res = Controller->SendCommandToDeployManager("broadcast?command=get_status&ctype=sbtests&filter=$status$");
                DEBUG_LOG << res << Endl;
                NJson::TJsonValue jsonVal;
                CHECK_TEST_EQ(NJson::ReadJsonFastTree(res, &jsonVal), true);
                TBackendProxyConfig::TAddress addr = Controller->GetConfig().Controllers[backend];
                TString addrS = addr.Host + ":" + ToString(addr.Port - 3);
                DEBUG_LOG << "check path " << addrS << Endl;
                DEBUG_LOG << "check value " << jsonVal["tests"][addrS]["$status$"]["status"].GetString() << Endl;
                if (jsonVal["tests"][addrS]["$status$"]["status"].GetString() == "FailedIndex") {
                    if (atFirst) {
                        MUST_BE_BROKEN(DeployBe("tests"));
                        atFirst = false;
                        continue;
                    } else {
                        break;
                    }
                }
            } catch (...) {
                ERROR_LOG << "on broadcast read: " << CurrentExceptionMessage() << Endl;
            }
        };
        CHECK_TEST_EQ(atFirst, false);
        for (ui32 i = 0; i < 12; ++i) {
            sleep(10);
            NDaemonController::TClusterControlAction action("sbtests", "", TVector<TString>(), TVector<TString>(),
                NDaemonController::apStartAndWait, true);
            Controller->ExecuteActionOnDeployManager(action);
            if (Controller->IsServerActive())
                break;
        }
        CHECK_TEST_EQ(Controller->IsServerActive(), true);

        TSet<std::pair<ui64, TString> > deleted;
        CheckDocuments(messages, deleted);
        return true;
    }
};

START_TEST_DEFINE_PARENT(TestBrokenIndexEmptyKeyInv, TestBrokenIndex)
    void BreakIndex(const TFsPath& path) override {
        TFile key(path / "indexkey", RdWr | CreateAlways);
        TFile inv(path / "indexinv", RdWr | CreateAlways);
    }
};

START_TEST_DEFINE_PARENT(TestBrokenIndexSuspended, TTestBrokenIndexEmptyKeyInvCaseClass)
    bool Run() override {
        i64 countMessages = 10;
        TVector<NRTYServer::TMessage> messages;
        GenerateInput(messages, countMessages, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed());
        IndexMessages(messages, DISK, 1);
        ReopenIndexers();
        ui32 backend = *Controller->GetActiveBackends().begin();
        TJsonPtr indexes = Controller->ProcessCommand("get_final_indexes&full_path=true", backend);
        Controller->ProcessCommand("stop", backend);

        NJson::TJsonValue::TArray dirs;
        dirs.clear();

        for (const auto& dirVal: indexes->GetArray()) {
            for (const auto& dir : dirVal["dirs"].GetArray()) {
                BreakIndex(dir.GetString());
            }
        }
        MUST_BE_BROKEN(Controller->RestartBackend(backend));

        TInstant start = Now();
        bool atFirst = true;
        while (Now() - start < TDuration::Seconds(300)) {
            Sleep(TDuration::Seconds(2));
            TString res = Controller->SendCommandToDeployManager("broadcast?command=get_status&ctype=sbtests&filter=$status$");
            DEBUG_LOG << res << Endl;
            NJson::TJsonValue jsonVal;
            CHECK_TEST_EQ(NJson::ReadJsonFastTree(res, &jsonVal), true);
            auto addr = Controller->GetConfig().Controllers[backend];
            TString addrS = addr.Host + ":" + ToString(addr.Port - 3);
            DEBUG_LOG << "check path " << addrS << Endl;
            DEBUG_LOG << "check value " << jsonVal["tests"][addrS]["$status$"]["status"].GetString() << Endl;
            if (jsonVal["tests"][addrS]["$status$"]["status"].GetString() == "FailedIndex") {
                if (atFirst) {
                    MUST_BE_BROKEN(DeployBe("tests"));
                    atFirst = false;
                    continue;
                } else {
                    break;
                }
            }
        };

        TInstant timeStart = Now();
        while (Now() - timeStart < TDuration::Minutes(10)) {
            sleep(10);
            NDaemonController::TClusterControlAction action("sbtests", "", TVector<TString>(), TVector<TString>(),
                NDaemonController::apStart, true);
            action.SetSuspended(true);
            Controller->ExecuteActionOnDeployManager(action);
            TString taskId = action.GetInfo();

            if (GetTaskState(taskId) == "FINISHED") {
                sleep(10);
                continue;
            }

            CHECK_TEST_EQ(GetTaskState(taskId), "SUSPENDED");

            Controller->SendCommandToDeployManager("control_task?action=resume&task_id=" + taskId);

            CHECK_TEST_NEQ(GetTaskState(taskId), "SUSPENDED");

            Controller->SendCommandToDeployManager("control_task?action=suspend&task_id=" + taskId);

            for (int i = 0; i < 10; ++i) {
                if (GetTaskState(taskId) == "SUSPENDED")
                    break;
                sleep(1);
            }
            CHECK_TEST_EQ(GetTaskState(taskId), "SUSPENDED");

            Controller->SendCommandToDeployManager("control_task?action=resume&task_id=" + taskId);

            for (int i = 0; i < 120; ++i) {
                if (GetTaskState(taskId) == "IN_PROGRESS" || GetTaskState(taskId) == "FINISHED")
                    break;
                sleep(1);
            }
            CHECK_TEST_EQ(GetTaskState(taskId) == "IN_PROGRESS" || GetTaskState(taskId) == "FINISHED", true);

            action.SetAsyncPolicy(NDaemonController::apWait);
            Controller->ExecuteActionOnDeployManager(action);

            for (int i = 0; i < 120; ++i) {
                if (GetTaskState(taskId) == "FINISHED")
                    break;
                sleep(1);
            }
            CHECK_TEST_EQ(GetTaskState(taskId), "FINISHED");

            if (Controller->IsServerActive())
                break;
        }
        CHECK_TEST_EQ(Controller->IsServerActive(), true);

        TSet<std::pair<ui64, TString> > deleted;
        CheckDocuments(messages, deleted);

        return true;
    }

    TString GetTaskState(const TString& taskId) {
        TString info = Controller->SendCommandToDeployManager("deploy_info?id=" + taskId);
        TStringInput input(info);
        NJson::TJsonValue json;
        if (!NJson::ReadJsonTree(&input, &json))
            ythrow yexception() << "invalid reply from deploy_info: " << info;
        return json["result_code"].GetString();
    }
};
