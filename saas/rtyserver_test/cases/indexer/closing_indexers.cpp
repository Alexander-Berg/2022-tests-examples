#include <saas/rtyserver_test/testerlib/rtyserver_test.h>
#include <util/folder/filelist.h>
#include <saas/rtyserver/common/common_messages.h>
#include <saas/rtyserver/common/sharding.h>
#include <library/cpp/mediator/messenger.h>

START_TEST_DEFINE(TestMemoryLeakIndexing, TTestMarksPool::NoCoverage)
bool Run() override {
    ui32 shardsNumber = GetShardsNumber();
    TVector<NRTYServer::TMessage> messagesToDisk;
    ui32 maxDocs = GetMaxDocuments();
    GenerateInput(messagesToDisk, shardsNumber * maxDocs / 3, NRTYServer::TMessage::MODIFY_DOCUMENT, GetIsPrefixed());
    IndexMessages(messagesToDisk, DISK, 100, 0, true, true, TDuration(), TDuration(), 16);
    ReopenIndexers();
    return true;
}
public:
    bool InitConfig() override {
        SetMergerParams(1, 1, mcpNEWINDEX);
        SetIndexerParams(REALTIME, 1000, 2);
        SetIndexerParams(DISK, 1000, 4);
        return true;
    }
};

START_TEST_DEFINE(TestEMPTY_INDEXERS_CLOSING, TTestMarksPool::OneBackendOnly)
bool Run() override {
    Controller->ProcessCommand("stop&rigid=false");
    if (!NFs::Exists(GetIndexDir()))
        TEST_FAILED("Incorrect index dir for empty-index-test");
    TDirsList dirList;
    const char *dir = nullptr;
    dirList.Fill(GetIndexDir());
    while ((dir = dirList.Next()) != nullptr) {
        TEST_FAILED("Non empty index after empty server stopped");
    }

    return true;
}

public:
    bool InitConfig() override {
        SetIndexerParams(REALTIME, 10, 8);
        SetIndexerParams(DISK, 10, 1);
        return true;
    }
};

START_TEST_DEFINE(TestINDEXERS_CLOSING)
    bool Run() override {
        ui32 shardsNumber = GetShardsNumber();
        TVector<NRTYServer::TMessage> messagesToDisk1, messagesToDisk2;
        ui32 maxDocs = GetMaxDocuments();
        GenerateInput(messagesToDisk1, shardsNumber * maxDocs, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed());
        GenerateInput(messagesToDisk2, shardsNumber * maxDocs, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed());
        IndexMessages(messagesToDisk1, DISK, 1);
        IndexMessages(messagesToDisk2, DISK, 1);
        DEBUG_LOG << "Check Disk1" << Endl;
        if (!CheckIndexSize(2 * shardsNumber * maxDocs, DISK, 100))
            TEST_FAILED("Incorrect documents count. Case A");
        CheckSearchResults(messagesToDisk1);
        DEBUG_LOG << "Check Disk2" << Endl;
        CheckSearchResults(messagesToDisk2);


        TVector<NRTYServer::TMessage> messagesToMemory;
        GenerateInput(messagesToMemory, 10 * GetMaxDocuments(), NRTYServer::TMessage::MODIFY_DOCUMENT, GetIsPrefixed());
        IndexMessages(messagesToMemory, REALTIME, 1);
        DEBUG_LOG << "Check Memory" << Endl;
        if (!CheckIndexSize(10 * GetMaxDocuments() + 2 * shardsNumber * maxDocs, DISK, 100)) {
            TEST_FAILED("Incorrect documents count. Case B");
        }
        if (!CheckIndexSize(0, REALTIME, 100)) {
            TEST_FAILED("Incorrect documents count. Case B2");
        }
        CheckSearchResults(messagesToMemory);
        ReopenIndexers();
        if (!CheckIndexSize(10 * GetMaxDocuments() + 2 * shardsNumber * maxDocs, DISK, 100)) {
            TEST_FAILED("Incorrect documents count. Case C");
        }
        DEBUG_LOG << "Check Memory2" << Endl;
        CheckSearchResults(messagesToMemory);
        return true;
    }
public:
    bool InitConfig() override {
        SetIndexerParams(REALTIME, 10, 8);
        SetIndexerParams(DISK, 10, 1);
        (*ConfigDiff)["Indexer.Memory.MaxDocumentsReserveCapacityCoeff"] = 20;
        return true;
    }
};

START_TEST_DEFINE(TestINDEXERS_CLOSING_ONE_BASE)
bool Run() override {
    ui32 shardsNumber = GetShardsNumber();
    TVector<NRTYServer::TMessage> messagesToDisk1, messagesToDisk2;
    ui32 maxDocs = GetMaxDocuments();
    GenerateInput(messagesToDisk1, shardsNumber * maxDocs, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed());
    IndexMessages(messagesToDisk1, DISK, 1);
    DEBUG_LOG << "Check Disk0" << Endl;
    if (!CheckIndexSize(shardsNumber * maxDocs, DISK, 100))
        TEST_FAILED("Incorrect documents count. Case A");
    GenerateInput(messagesToDisk2, shardsNumber * maxDocs, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed());
    IndexMessages(messagesToDisk2, DISK, 1);
    DEBUG_LOG << "Check Disk1" << Endl;
    if (!CheckIndexSize(2 * shardsNumber * maxDocs, DISK, 100))
        TEST_FAILED("Incorrect documents count. Case A1");

    CheckSearchResults(messagesToDisk1);
    DEBUG_LOG << "Check Disk2" << Endl;
    CheckSearchResults(messagesToDisk2);


    TVector<NRTYServer::TMessage> messagesToMemory;
    GenerateInput(messagesToMemory, 10 * maxDocs, NRTYServer::TMessage::MODIFY_DOCUMENT, GetIsPrefixed());
    IndexMessages(messagesToMemory, REALTIME, 1);
    DEBUG_LOG << "Check Memory" << Endl;
    if (!CheckIndexSize(10 * maxDocs + 2 * shardsNumber * maxDocs, DISK, 100)) {
        TEST_FAILED("Incorrect documents count. Case B");
    }
    if (!CheckIndexSize(0, REALTIME, 100)) {
        TEST_FAILED("Incorrect documents count. Case C");
    }
    CheckSearchResults(messagesToMemory);
    ReopenIndexers();
    if (!CheckIndexSize(10 * GetMaxDocuments() + 2 * shardsNumber * maxDocs, DISK, 100)) {
        TEST_FAILED("Incorrect documents count. Case D");
    }
    DEBUG_LOG << "Check Memory2" << Endl;
    CheckSearchResults(messagesToMemory);
    return true;
}
public:
    bool InitConfig() override {
        SetMergerParams(true, 1, -1, mcpCONTINUOUS);
        (*ConfigDiff)["SearchersCountLimit"] = 1;
        (*ConfigDiff)["Indexer.Memory.MaxDocumentsReserveCapacityCoeff"] = 20;
        SetIndexerParams(REALTIME, 10, 1);
        SetIndexerParams(DISK, 10, 1);
        return true;
    }
};

START_TEST_DEFINE(TestINDEXERS_CLOSING_TWO_SEARCHERS)
bool CheckShardsCount(ui32 required) {
    TJsonPtr info = Controller->GetServerInfo();
    NJson::TJsonValue::TArray jsonArr;
    CHECK_TEST_EQ(info->GetArray(&jsonArr), true);
    NJson::TJsonValue countShards = jsonArr[0]["search_sources_count"];
    if (!countShards.IsInteger()) {
        ythrow yexception() << "there is no countShards: " << info->GetStringRobust() << Endl;
    }
    CHECK_TEST_EQ(countShards.GetInteger(), required);
    return true;
}

bool CheckMemoryDocsCount(ui32 required) {
    TJsonPtr info = Controller->GetServerInfo();
    NJson::TJsonValue::TArray jsonArr;
    CHECK_TEST_EQ(info->GetArray(&jsonArr), true);
    NJson::TJsonValue countDocs = jsonArr[0]["docs_in_memory_indexes"];
    if (!countDocs.IsInteger()) {
        ythrow yexception() << "there is no docs_in_memory_indexes: " << info->GetStringRobust() << Endl;
    }
    CHECK_TEST_EQ(countDocs.GetInteger(), required);
    return true;
}

bool Run() override {
    ui32 shardsNumber = GetShardsNumber();
    TVector<NRTYServer::TMessage> messagesToDisk1, messagesToDisk2, messagesToDisk3, messagesToMemory;
    ui32 maxDocs = GetMaxDocuments() - 1;
    GenerateInput(messagesToMemory, shardsNumber * maxDocs, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed());
    GenerateInput(messagesToDisk1, shardsNumber * maxDocs, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed());
    GenerateInput(messagesToDisk2, shardsNumber * maxDocs, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed());
    GenerateInput(messagesToDisk3, shardsNumber * maxDocs, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed());

    IndexMessages(messagesToDisk1, DISK, 1);
    ReopenIndexers();
    TSearchMessagesContext context = TSearchMessagesContext::BuildDefault(1);
    TSearchMessagesContext context0 = TSearchMessagesContext::BuildDefault(0);
    CHECK_TEST_TRUE(CheckSearchResultsSafe(messagesToDisk1, context));

    IndexMessages(messagesToDisk2, DISK, 1);
    ReopenIndexers();
    CHECK_TEST_TRUE(CheckSearchResultsSafe(messagesToDisk2, context));

    IndexMessages(messagesToDisk3, DISK, 1);
    ReopenIndexers();
    CHECK_TEST_TRUE(CheckSearchResultsSafe(messagesToDisk3, context0));

    IndexMessages(messagesToMemory, REALTIME, 1);
    CHECK_TEST_TRUE(CheckSearchResultsSafe(messagesToMemory, context));

    Controller->ProcessCommand("create_merger_tasks");
    Controller->ProcessCommand("do_all_merger_tasks");

    CHECK_TEST_TRUE(CheckShardsCount(shardsNumber * (1 + 2)));

    CHECK_TEST_TRUE(CheckSearchResultsSafe(messagesToDisk1, context));

    CHECK_TEST_TRUE(CheckSearchResultsSafe(messagesToDisk2, context));

    CHECK_TEST_TRUE(CheckSearchResultsSafe(messagesToDisk3, context));

    CHECK_TEST_TRUE(CheckSearchResultsSafe(messagesToMemory, context));

    ReopenIndexers();

    CHECK_TEST_TRUE(CheckShardsCount(shardsNumber * (1 + 2)));

    CHECK_TEST_TRUE(CheckSearchResultsSafe(messagesToDisk1, context));

    CHECK_TEST_TRUE(CheckSearchResultsSafe(messagesToDisk2, context));

    CHECK_TEST_TRUE(CheckSearchResultsSafe(messagesToDisk3, context));

    CHECK_TEST_TRUE(CheckSearchResultsSafe(messagesToMemory, context));

    CHECK_TEST_TRUE(CheckMemoryDocsCount(messagesToMemory.size()));
    CHECK_TEST_TRUE(CheckShardsCount(shardsNumber * (1 + 2)));

    Controller->ProcessCommand("create_merger_tasks");
    Controller->ProcessCommand("do_all_merger_tasks");

    CHECK_TEST_TRUE(CheckShardsCount(shardsNumber * (1 + 2)));

    CHECK_TEST_TRUE(CheckSearchResultsSafe(messagesToDisk1, context));

    CHECK_TEST_TRUE(CheckSearchResultsSafe(messagesToDisk2, context));

    CHECK_TEST_TRUE(CheckSearchResultsSafe(messagesToDisk3, context));

    CHECK_TEST_TRUE(CheckSearchResultsSafe(messagesToMemory, context));
    CHECK_TEST_TRUE(CheckMemoryDocsCount(0));

    return true;
}
public:
    bool InitConfig() override {
        SetMergerParams(true, 1, -1, mcpTIME, 5000000);
        (*ConfigDiff)["SearchersCountLimit"] = 2;
        (*ConfigDiff)["Merger.MaxSegments"] = 2;
        (*ConfigDiff)["Indexer.Memory.MaxDocumentsReserveCapacityCoeff"] = 20;
        SetIndexerParams(REALTIME, 10, 1);
        SetIndexerParams(DISK, 10, 1);
        return true;
    }
};

START_TEST_DEFINE(TestINDEXER_CONTENT_AFTER_CLOSE_ONE_BASE)
bool Run() override {
    ui32 shardsNumber = GetShardsNumber();
    TVector<NRTYServer::TMessage> messagesToDisk1, messagesToDisk2;
    ui32 maxDocs = GetMaxDocuments();
    GenerateInput(messagesToDisk1, shardsNumber * maxDocs, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed());
    TString keyPrefix = "0";
    if (GetIsPrefixed()) {
        keyPrefix = "1";
        for (ui32 i = 0; i < messagesToDisk1.size(); ++i) {
            messagesToDisk1[i].MutableDocument()->SetKeyPrefix(1);
        }
    }
    DEBUG_LOG << "indexation..." << Endl;
    IndexMessages(messagesToDisk1, REALTIME, 1);
    sleep(10);
    DEBUG_LOG << "remove..." << Endl;
    DeleteQueryResult("url:\"*\"&kps=" + keyPrefix, REALTIME);
    ReopenIndexers();
    TVector<TDocSearchInfo> results;
    QuerySearch("url:\"*\"&kps=" + keyPrefix, results);
    if (results.size())
        TEST_FAILED("incorrect documents count: " + ToString(results.size()));
    return true;
}
public:
    bool InitConfig() override {
        SetMergerParams(true, 1, -1, mcpNEWINDEX);
        (*ConfigDiff)["SearchersCountLimit"] = 1;
        (*ConfigDiff)["Indexer.Memory.MaxDocumentsReserveCapacityCoeff"] = 20;
        (*ConfigDiff)["ShardsNumber"] = 1;
        SetIndexerParams(REALTIME, 10, 1);
        SetIndexerParams(DISK, 10, 1);
        return true;
    }
};

START_TEST_DEFINE(TestINDEXER_ORDER_AFTER_CLOSE_ONE_BASE)
bool Run() override {
    ui32 shardsNumber = GetShardsNumber();
    TVector<NRTYServer::TMessage> messagesToDisk1, messagesToDisk2;
    ui32 maxDocs = GetMaxDocuments();
    GenerateInput(messagesToDisk1, 2 * shardsNumber * maxDocs, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed());
    TString keyPrefix = "0";
    for (ui32 i = 0; i < messagesToDisk1.size(); ++i) {
        ::NRTYServer::TAttribute* attr = messagesToDisk1[i].MutableDocument()->AddGroupAttributes();
        attr->SetName("unique_attr");
        attr->SetValue(ToString(i));
        attr->SetType(NRTYServer::TAttribute::INTEGER_ATTRIBUTE);
    }
    if (GetIsPrefixed()) {
        keyPrefix = "1";
        for (ui32 i = 0; i < messagesToDisk1.size(); ++i) {
            messagesToDisk1[i].MutableDocument()->SetKeyPrefix(1);
        }
    }
    DEBUG_LOG << "indexation..." << Endl;
    IndexMessages(messagesToDisk1, REALTIME, 1);
    sleep(10);
    TVector<TDocSearchInfo> results;
    QuerySearch("url:\"*\"&kps=" + keyPrefix, results);
    CHECK_TEST_EQ(results.size(), 20);
    DEBUG_LOG << "remove..." << Endl;
    DeleteQueryResult("url:\"*\"&fa=unique_attr:12&kps=" + keyPrefix, REALTIME);
    Controller->ProcessCommand("create_merger_tasks");
    Controller->ProcessCommand("do_all_merger_tasks");
    QuerySearch("url:\"*\"&fa=unique_attr:12&kps=" + keyPrefix, results);
    CHECK_TEST_EQ(results.size(), 0);
    return true;
}
public:
    bool InitConfig() override {
        SetMergerParams(true, 1, -1, mcpTIME, 1000000);
        SetPruneAttrSort("unique_attr");
        (*ConfigDiff)["SearchersCountLimit"] = 1;
        (*ConfigDiff)["Indexer.Memory.MaxDocumentsReserveCapacityCoeff"] = 20;
        (*ConfigDiff)["ShardsNumber"] = 1;
        SetIndexerParams(REALTIME, 10, 1);
        SetIndexerParams(DISK, 10, 1);
        return true;
    }
};

START_TEST_DEFINE(TestMerger_ONE_BASE)
ui32 ShardsNumber;
bool CheckSearchersCount(ui32 countWithSearch, ui32 countWithNoSearch, ui32 countFinalDocs, ui32 countMemoryDocs) {
    bool result = true;
    for (ui32 att = 0; att < 90; ++att) {
        INFO_LOG << "Attemption compare N" << att << Endl;
        PrintInfoServer();
        TMessageCollectServerInfo mCollect;
        SendGlobalMessage(mCollect);
        TVector<ui32> shCount(ShardsNumber, 0);
        TVector<ui32> shCountNSearch(ShardsNumber, 0);
        TVector<ui32> shCountDocs(ShardsNumber, 0);
        TVector<ui32> shCountNSearchDocs(ShardsNumber, 0);
        TVector<ui32> shCountMemoryDocs(ShardsNumber, 0);
        TVector<ui32> shCountTempDocs(ShardsNumber, 0);
        for (auto i : mCollect.GetIndexInfo()) {
            TString name = i.first;
            ui32 shard = NRTYServer::GetShard(i.first);
            INFO_LOG << i.second.GetJsonInfo().GetStringRobust() << Endl;
            if (name.StartsWith("index_")) {
                if (i.second.Cache.Defined()) {
                    shCount[shard]++;
                    shCountDocs[shard] += *i.second.CountWithnoDel;
                } else {
                    shCountNSearchDocs[shard] += *i.second.CountWithnoDel;
                    shCountNSearch[shard]++;
                }
            }
            if (name.StartsWith("temp__")) {
                shCountTempDocs[shard] += *i.second.CountWithnoDel;
            }
            if (name.StartsWith("memory_")) {
                shCountMemoryDocs[shard] += *i.second.CountWithnoDel;
            }
        }
        result = true;
        for (auto i : shCount) {
            CHECK_TEST_LESS(i, countWithSearch + 1);
            if (i != countWithSearch) {
                result = false;
                break;
            }
        }
        for (auto i : shCountNSearch) {
            if (i != countWithNoSearch) {
                DEBUG_LOG << i << " != " << countWithNoSearch << Endl;
                result = false;
                break;
            }
        }
        for (auto i : shCountDocs) {
            if (i != countFinalDocs) {
                DEBUG_LOG << i << " != " << countFinalDocs << Endl;
                result = false;
                break;
            }
        }
        for (ui32 i = 0; i < shCountNSearchDocs.size(); ++i) {
            if (shCountNSearchDocs[i] + shCountTempDocs[i] != countMemoryDocs) {
                DEBUG_LOG << shCountNSearchDocs[i] + shCountTempDocs[i] << " != " << countMemoryDocs << Endl;
                result = false;
                break;
            }
            if (shCountMemoryDocs[i] != countMemoryDocs) {
                DEBUG_LOG << shCountMemoryDocs[i] << " != " << countMemoryDocs << Endl;
                result = false;
                break;
            }
        }
        if (!result) {
            sleep(1);
            continue;
        }
        break;
    }
    return result;
}

bool Run() override {
    ShardsNumber = GetShardsNumber();
    TVector<NRTYServer::TMessage> messagesToDisk1, messagesToDisk2;
    ui32 maxDocs = GetMaxDocuments();
    GenerateInput(messagesToDisk1, ShardsNumber * maxDocs, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed());
    IndexMessages(messagesToDisk1, REALTIME, 1);

    CHECK_TEST_EQ(CheckSearchersCount(1, 1, maxDocs, 0), true);

    Controller->ProcessCommand("create_merger_tasks");
    Controller->ProcessCommand("do_all_merger_tasks");

    GenerateInput(messagesToDisk2, ShardsNumber * maxDocs * 2.5, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed());
    IndexMessages(messagesToDisk2, REALTIME, 1);

    CHECK_TEST_EQ(CheckSearchersCount(1, 1, maxDocs * 3, maxDocs * 0.5), true);

    TMessageCollectServerInfo mCollect;
    SendGlobalMessage(mCollect);
    if (mCollect.GetDocsInDiskIndexers() != mCollect.GetDocsInMemoryIndexes())
        sleep(1);
    PrintInfoServer();
    SendGlobalMessage(mCollect);
    CHECK_TEST_EQ(mCollect.GetDocsInDiskIndexers(), mCollect.GetDocsInMemoryIndexes());
    return true;
}
public:
    bool InitConfig() override {
        SetMergerParams(true, 1, -1, mcpNEWINDEX);
        (*ConfigDiff)["SearchersCountLimit"] = 1;
        (*ConfigDiff)["Indexer.Memory.MaxDocumentsReserveCapacityCoeff"] = 20;
        SetIndexerParams(REALTIME, 10, 1);
        SetIndexerParams(DISK, 10, 1);
        return true;
    }
};

START_TEST_DEFINE(TestINDEXER_DOCS_VERSION)
bool Run() override {
    TVector<NRTYServer::TMessage> messagesToDisk;
    TVector<NRTYServer::TMessage> messagesToDelete;
    GenerateInput(messagesToDisk, 1, NRTYServer::TMessage::MODIFY_DOCUMENT, GetIsPrefixed());
    messagesToDisk[0].MutableDocument()->SetBody("abc");
    messagesToDisk[0].MutableDocument()->SetVersion(10);
    messagesToDelete.push_back(BuildDeleteMessage(messagesToDisk[0]));
    IndexMessages(messagesToDisk, REALTIME, 1);

    TVector<TDocSearchInfo> results;
    QuerySearch("abc&" + GetAllKps(messagesToDisk), results);
    if (results.size() != 1)
        TEST_FAILED("Case 1");

    messagesToDisk[0].MutableDocument()->SetBody("addc");
    messagesToDisk[0].MutableDocument()->SetVersion(1);
    try {
        IndexMessages(messagesToDisk, REALTIME, 1);
        TEST_FAILED("Incorrect document indexed");
    } catch(...) {

    }
    QuerySearch("abc&" + GetAllKps(messagesToDisk), results);
    if (results.size() != 1)
        TEST_FAILED("Case 2");


    messagesToDelete[0].MutableDocument()->SetVersion(12);
    IndexMessages(messagesToDelete, REALTIME, 1);
    QuerySearch("abc&" + GetAllKps(messagesToDisk), results);
    if (results.size() != 0)
        TEST_FAILED("Case 3");

    messagesToDisk[0].MutableDocument()->SetBody("addc");
    messagesToDisk[0].MutableDocument()->SetVersion(1);
    try {
        IndexMessages(messagesToDisk, REALTIME, 1);
        TEST_FAILED("Incorrect document indexed");
    } catch(...) {

    }
    QuerySearch("abc&" + GetAllKps(messagesToDisk), results);
    if (results.size() != 0)
        TEST_FAILED("Case 4");

    messagesToDisk[0].MutableDocument()->SetBody("addc");
    messagesToDisk[0].MutableDocument()->SetVersion(13);
    IndexMessages(messagesToDisk, REALTIME, 1);
    QuerySearch("addc&" + GetAllKps(messagesToDisk), results);
    if (results.size() != 1)
        TEST_FAILED("Case 5");

    messagesToDelete[0].MutableDocument()->SetVersion(12);
    try {
        IndexMessages(messagesToDelete, REALTIME, 1);
        TEST_FAILED("Incorrect document indexed");
    } catch(...) {

    }
    QuerySearch("addc&" + GetAllKps(messagesToDisk), results);
    if (results.size() != 1)
        TEST_FAILED("Case 6");

    CheckSearchResults(messagesToDisk);

    Controller->ProcessCommand("restart&wait=true");

    messagesToDelete[0].MutableDocument()->SetVersion(12);
    try {
        IndexMessages(messagesToDelete, REALTIME, 1);
        TEST_FAILED("Incorrect document indexed");
    } catch(...) {

    }

    messagesToDelete[0].MutableDocument()->SetVersion(14);
    IndexMessages(messagesToDelete, REALTIME, 1);
    QuerySearch("addc&" + GetAllKps(messagesToDisk), results);
    if (results.size() != 0)
        TEST_FAILED("Case 6");

    return true;
}
public:
    bool InitConfig() override {
        (*ConfigDiff)["Indexer.Common.DocsInfoLiveTimeSeconds"] = 10000;
        SetMergerParams(true, 1, -1, mcpNEWINDEX);
        SetIndexerParams(REALTIME, 10, 8);
        SetIndexerParams(DISK, 10, 1);
        return true;
    }
};

START_TEST_DEFINE(TestINDEXER_DOCS_VERSION_REMOVE_OLD_DATA)
bool Run() override {
    TVector<NRTYServer::TMessage> messagesToDisk;
    TVector<NRTYServer::TMessage> messagesToDelete;
    GenerateInput(messagesToDisk, 1, NRTYServer::TMessage::MODIFY_DOCUMENT, GetIsPrefixed());
    messagesToDisk[0].MutableDocument()->SetBody("abc");
    messagesToDisk[0].MutableDocument()->SetVersion(10);
    messagesToDelete.push_back(BuildDeleteMessage(messagesToDisk[0]));
    IndexMessages(messagesToDisk, REALTIME, 1);

    TVector<TDocSearchInfo> results;
    QuerySearch("abc&" + GetAllKps(messagesToDisk), results);
    if (results.size() != 1)
        TEST_FAILED("Case 1");

    sleep(5);
    Controller->ProcessCommand("create_merger_tasks");
    sleep(15);

    messagesToDelete[0].MutableDocument()->SetVersion(1);
    IndexMessages(messagesToDelete, REALTIME, 1);
    QuerySearch("abc&" + GetAllKps(messagesToDisk), results);
    if (results.size() != 0)
        TEST_FAILED("Case 3");

    messagesToDelete[0].MutableDocument()->SetVersion(130);
    messagesToDelete[0].MutableDocument()->SetBody("query_del:url:\"*\"");
    try {
        IndexMessages(messagesToDelete, REALTIME, 1);
        TEST_FAILED("Incorrect document indexed");
    } catch(...) {

    }

    return true;
}
public:
    bool InitConfig() override {
        (*ConfigDiff)["Indexer.Common.DocsInfoLiveTimeSeconds"] = 1;
        SetIndexerParams(REALTIME, 10, 8);
        SetIndexerParams(DISK, 10, 1);
        return true;
    }
};

START_TEST_DEFINE(TestINDEXERS_CLOSING_ONE_BASE_PRE)
bool Run() override {
    ui32 shardsNumber = GetShardsNumber();
    TVector<NRTYServer::TMessage> messagesToDisk1, messagesToDisk2;
    ui32 maxDocs = GetMaxDocuments();
    GenerateInput(messagesToDisk1, shardsNumber * maxDocs, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed());
    GenerateInput(messagesToDisk2, shardsNumber * maxDocs, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed());
    IndexMessages(messagesToDisk1, REALTIME, 1);
    sleep(10);
    if (!CheckIndexSize(shardsNumber * maxDocs, DISK))
        ythrow yexception() << "Incorrect documents count. Case A1";
    if (!CheckIndexSize(0, REALTIME))
        ythrow yexception() << "Incorrect documents count. Case B1";
    IndexMessages(messagesToDisk2, REALTIME, 1);
    sleep(10);
    if (!CheckIndexSize(shardsNumber * maxDocs * 2, DISK))
        ythrow yexception() << "Incorrect documents count. Case A2";
    if (!CheckIndexSize(shardsNumber * maxDocs, REALTIME))
        ythrow yexception() << "Incorrect documents count. Case B2";
    return true;
}
public:
    bool InitConfig() override {
        SetMergerParams(true, 1, -1, mcpTIME, 1000 * 3600);
        (*ConfigDiff)["SearchersCountLimit"] = 1;
        (*ConfigDiff)["Indexer.Memory.MaxDocumentsReserveCapacityCoeff"] = 20;
        SetIndexerParams(REALTIME, 10, 8);
        SetIndexerParams(DISK, 10, 1);
        return true;
    }
};

START_TEST_DEFINE(TestDisableIndexing)
bool Run() override {
    TVector<NRTYServer::TMessage> messages;
    ui32 maxDocs = 1;
    GenerateInput(messages, maxDocs, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed());
    bool wasError = false;
    Controller->ProcessCommand("disable_indexing");
    try {
        IndexMessages(messages, REALTIME, 1);
    } catch (...) {
        wasError = true;
    }
    if (!wasError)
        ythrow yexception() << "indexing was not disabled";
    Controller->ProcessCommand("enable_indexing");
    IndexMessages(messages, REALTIME, 1);
    CheckSearchResults(messages);
    return true;
}
public:
    bool InitConfig() override {
        SetMergerParams(true, 1, -1, mcpTIME, 1000 * 3600);
        (*ConfigDiff)["SearchersCountLimit"] = 1;
        (*ConfigDiff)["Indexer.Memory.MaxDocumentsReserveCapacityCoeff"] = 20;
        SetIndexerParams(REALTIME, 10, 8);
        SetIndexerParams(DISK, 10, 1);
        return true;
    }
};
