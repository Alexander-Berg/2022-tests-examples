#include <saas/rtyserver_test/testerlib/rtyserver_test.h>
#include <saas/rtyserver_test/cases/repair/repair_test.h>
#include <util/system/thread.h>

SERVICE_TEST_RTYSERVER_DEFINE(TestRigidStopBase)
protected:
    struct TIndexProcParams {
        TVector<NRTYServer::TMessage>* Messages;
        TestRigidStopBase* This;
        bool Success;
    };
    static void* TestRigidStopIndexProc(void* mess) {
        TRY
            TIndexProcParams* params = (TIndexProcParams*)mess;
            params->Success = false;
            params->This->IndexMessages(*params->Messages, DISK, 1, 600000);
            params->Success = true;
        CATCH("TestRigidStopIndexProc");
        return nullptr;
    }
    virtual void RestartBackend() = 0;

public:
    bool Run() override {
        TVector<NRTYServer::TMessage> messages;
        GenerateInput(messages, GetMaxDocuments(), NRTYServer::TMessage::MODIFY_DOCUMENT, GetIsPrefixed());
        TIndexProcParams params;
        params.Messages = &messages;
        params.This = this;
        TThread indexThread(&TestRigidStopIndexProc, &params);
        indexThread.Start();
        sleep(1);
        RestartBackend();
        indexThread.Join();
        if (!params.Success)
            ythrow yexception() << "Indexer thread fails";
        Controller->WaitIsRepairing();
        ReopenIndexers();
        CheckSearchResults(messages);
        return true;
    }
public:
    bool InitConfig() override {
        SetEnabledRepair();
        SetMergerParams(true, 1, -1, mcpNONE);
        (*ConfigDiff)["ModulesConfig.DOCFETCHER.OverlapAge"] = "20";
        return true;
    }
};

START_TEST_DEFINE_PARENT(TestRigidStop, TestRigidStopBase)
    void RestartBackend() override {
        TRestartServerStatistics restartStatistics;
        Controller->RestartServer(true, &restartStatistics);
        if (restartStatistics.StopTimeMilliseconds > 15000)
            ythrow yexception() << "Rigid stop is too slow. It takes " << restartStatistics.StopTimeMilliseconds << " ms.";
    }
};

START_TEST_DEFINE_PARENT(TestRigidStopSlow, TestRigidStopBase)
    void RestartBackend() override {
        Controller->ProcessCommand("stop&rigid=true");
        Sleep(TDuration::Seconds(15));
        Controller->RestartServer();
    }
};

START_TEST_DEFINE(TestRigidStopMerger)
    bool Run() override {
        TVector<NRTYServer::TMessage> messages;
        GenerateInput(messages, 10 * GetMaxDocuments(), NRTYServer::TMessage::MODIFY_DOCUMENT, GetIsPrefixed());
        TRestartServerStatistics restartStatistics;
        IndexMessages(messages, DISK, 1);
        ReopenIndexers();

        Controller->ProcessCommand("create_merger_tasks");
        Controller->RestartServer(true, &restartStatistics);
        if (restartStatistics.StopTimeMilliseconds > 15000)
            ythrow yexception() << "Rigid stop is too slow. It takes " << restartStatistics.StopTimeMilliseconds << " ms.";
        Controller->WaitIsRepairing();
        Controller->ProcessCommand("create_merger_tasks");
        Controller->ProcessCommand("do_all_merger_tasks");
        CheckSearchResults(messages);
        return true;
    }
public:
    bool InitConfig() override {
        SetEnabledRepair();
        SetMergerParams(true, 1, 1, mcpNONE);
        return true;
    }
};

START_TEST_DEFINE_PARENT(TestRigidStopRepairer, TRepairTest, TTestMarksPool::OneBackendOnly)
    bool Run() override {
        PrepareIndex(9999);
        Controller->RestartServer();
        TRestartServerStatistics restartStatistics;
        Controller->RestartServer(true, &restartStatistics);
        if (restartStatistics.StopTimeMilliseconds > 15000)
            ythrow yexception() << "Rigid stop is too slow. It takes " << restartStatistics.StopTimeMilliseconds << " ms.";
        Check(9999);
        return true;
    }

    bool InitConfig() override {
        SetEnabledRepair();
        SetMergerParams(true, 1, 1, mcpNONE);
        SetIndexerParams(ALL, 10000, 1);
        return true;
    }

    void Check(int countDocs) {
        Controller->WaitIsRepairing();
        ReopenIndexers();
        for (int attemption = 0; (attemption < 5) && (QueryCount() != countDocs); attemption++)
            sleep(1);
        if (QueryCount() != countDocs) {
            ERROR_LOG << "Count documents after repair is incorrect - " << QueryCount() << " != " << countDocs << Endl;
            ythrow yexception() << "Count documents after repair is incorrect";
        }
    }
};

START_TEST_DEFINE(TestRigidStopIndexer)
using TRTYServerTestCase::Run;

void Run(const TBackendProxyConfig::TIndexer& indexer) {
    TSocket socket(TNetworkAddress(indexer.Host, indexer.Port));//, TDuration::Seconds(1));
    TSocketOutput socketOutput(socket);
    const ui32 trashLength = 0xFFFFFFFF;
    TRestartServerStatistics restartStatistics;
    sleep(1);
    socketOutput.Write(&trashLength, sizeof(trashLength));
    Controller->RestartServer(true, &restartStatistics);
    if (restartStatistics.StopTimeMilliseconds > 15000)
        ythrow yexception() << "Rigid stop is too slow. It takes " << restartStatistics.StopTimeMilliseconds << " ms.";
}

bool Run() override {
    Run(Controller->GetConfig().Indexer);
    return true;
}
};
