#include <saas/rtyserver_test/testerlib/rtyserver_test.h>
#include "repair_test.h"

START_TEST_DEFINE_PARENT(TestRepairStaticTemps, TRepairTest,
                                  TTestMarksPool::Repair, TTestMarksPool::OneBackendOnly)
    bool Run() override {
        PrepareIndex(9999);
        Controller->RestartServer();
        Check(9999);
        return true;
    }
};

START_TEST_DEFINE_PARENT(TestRepairDisabled, TRepairTest,
                         TTestMarksPool::Repair, TTestMarksPool::OneBackendOnly)
                         bool Run() override {
    PrepareIndex(9999);
    Controller->RestartServer();
    Check(0);
    for (auto&& tempIndex : TempIndices) {
        if (NFs::Exists(tempIndex))
            ythrow yexception() << tempIndex << " still exists";
    }
    return true;
}
bool InitConfig() override {
    SetIndexerParams(ALL, 20000, 1);
    SetEnabledRepair(false);
    return true;
}
};

START_TEST_DEFINE(TestRepairDeleting, TTestMarksPool::Repair)
bool Run() override {
    ui32 shardNumber = GetShardsNumber();
    size_t countMessages = GetMaxDocuments() / 2u;
    TVector<NRTYServer::TMessage> messages, deletedMess;
    GenerateInput(messages, shardNumber * countMessages, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed());
    IndexMessages(messages, REALTIME, 1);
    TSet<std::pair<ui64, TString> > deleted;
    DeleteSomeMessages(messages, deleted, REALTIME, 2);
    CheckSearchResults(messages, deleted);
    Controller->RestartServer(true);
    Controller->WaitIsRepairing();
    ReopenIndexers();
    CheckSearchResults(messages, deleted);
    return true;
}
bool InitConfig() override {
    SetIndexerParams(ALL, 20);
    SetEnabledRepair();
    return true;
}
};
