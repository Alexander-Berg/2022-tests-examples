#include "repair_test.h"

#include <util/folder/filelist.h>

void TRepairTest::Check(int countDocs) {
    Controller->WaitIsRepairing();
    ReopenIndexers();
    for (int attemption = 0; (attemption < 5) && (QueryCount() != countDocs); attemption++)
        sleep(1);
    if (QueryCount() != countDocs) {
        ERROR_LOG << "Count documents after repair is incorrect - " << QueryCount() << " != " << countDocs << Endl;
        ythrow yexception() << "Count documents after repair is incorrect";
    }
}

void TRepairTest::PrepareIndex(ui32 countDocs) {
    TVector<NRTYServer::TMessage> messagesForDisk;
    GenerateInput(messagesForDisk, countDocs, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed());
    IndexMessages(messagesForDisk, DISK, 1);
    Controller->ProcessCommand("stop&rigid=yes");

    const TFsPath& indexDir = GetIndexDir();
    TFileEntitiesList flist(TFileEntitiesList::EM_DIRS_SLINKS);
    flist.Fill(indexDir);
    TString directory;
    while (directory = flist.Next()) {
        if (directory.StartsWith("temp"))
            TempIndices.push_back((indexDir / directory).c_str());
    }

    for (auto&& index : TempIndices)
        DEBUG_LOG << "Found temp index: " << index << Endl;
}

bool TRepairTest::InitConfig() {
    SetIndexerParams(ALL, 10000, 1);
    SetEnabledRepair();
    return true;
}

START_TEST_DEFINE_PARENT(TestRepairDecrementMaxDocument, TRepairTest)
    bool Run() override {
        PrepareIndex(99);
        ConfigDiff->clear();
        (*ConfigDiff)["Indexer.Disk.MaxDocuments"] = 10;
        ApplyConfig();
        Controller->RestartServer();
        Check(99);
        return true;
    }
    bool InitConfig() override {
        SetIndexerParams(DISK, 100, 1);
        SetEnabledRepair();
        return true;
    }
};
