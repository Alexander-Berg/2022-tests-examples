#include "deploy_manager.h"
#include <saas/deploy_manager/scripts/add_intsearch/action.h>
#include <saas/deploy_manager/scripts/add_replica/action.h>
#include <saas/deploy_manager/scripts/cluster_control/action/action.h>
#include <saas/deploy_manager/scripts/deploy/action.h>
#include <saas/deploy_manager/scripts/release_slots/action.h>
#include <saas/deploy_manager/scripts/reshard/action.h>
#include <saas/deploy_manager/scripts/searchmap/action.h>

#include <saas/library/sharding/sharding.h>

#include <library/cpp/json/json_reader.h>

#include <util/random/random.h>

START_TEST_DEFINE_PARENT(TestDeployManagerRestore, TestDeployManager)

NSearchMapParser::TSlotsPool Pool;

void InitCluster() override {
    UploadCommon();
    UploadService("tests");
    UploadService("tests", 1);
    Pool = ConfigureCluster(2, 1, NSaas::UrlHash, "rtyserver", "tests");
    DeployBe("tests");
    DeploySp();
    DeployIp();
}
bool Run() override {
    i64 countMessages = 10;
    TVector<NRTYServer::TMessage> messagesForDisk;
    GenerateInput(messagesForDisk, countMessages, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed());
    IndexMessages(messagesForDisk, DISK, 3);
    ReopenIndexers();

    ui32 backend = *Controller->GetActiveBackends().begin();

    TString indexdir = Controller->GetConfigValue("IndexDir", "Server", backend);
    Controller->ProcessCommand("stop", backend);
    TFsPath(indexdir).ForceDelete();
    TFsPath(indexdir).MkDirs();
    Controller->ProcessCommand("restart", backend);
    NDaemonController::TClusterControlAction ra("sbtests", "tests", TVector<TString>(1, HostName() + ":" + ToString(Controller->GetConfig().Controllers[backend].Port - 3)), TVector<TString>(),
        NDaemonController::apStartAndWait, true);
    Controller->ExecuteActionOnDeployManager(ra);

    TJsonPtr serverInfo(Controller->GetServerInfo());
    for (NJson::TJsonValue::TArray::const_iterator i = serverInfo->GetArray().begin(); i != serverInfo->GetArray().end(); ++i) {
        i64 count = (*i)["docs_in_final_indexes"].GetInteger();
        if (count != countMessages)
            ythrow yexception() << "incorrect doc count on backend" << i - serverInfo->GetArray().begin() << ": " << count << " != " << countMessages;
    }
    CheckConfigs(true);
    return true;
}
};

START_TEST_DEFINE_PARENT(TestDeployManagerRestoreFromBroken, TestDeployManager)

NSearchMapParser::TSlotsPool Pool;

void InitCluster() override {
    UploadCommon();
    UploadService("tests");
    UploadService("tests", 1);
    Pool = ConfigureCluster(2, 1, NSaas::UrlHash, "rtyserver", "tests");
    DeployBe("tests");
    DeploySp();
    DeployIp();
}

bool Run() override {
    const ui64 countMessages = 10;
    TVector<NRTYServer::TMessage> messagesForDisk;
    GenerateInput(messagesForDisk, countMessages, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed());
    IndexMessages(messagesForDisk, DISK, 3);
    ReopenIndexers();


    const ui32 cleanBackend = *Controller->GetActiveBackends().begin();
    {
        TString indexdir = Controller->GetConfigValue("IndexDir", "Server", cleanBackend);
        Controller->ProcessCommand("stop", cleanBackend);
        TFsPath(indexdir).ForceDelete();
        TFsPath(indexdir).MkDirs();
        Controller->ProcessCommand("restart", cleanBackend);
    }

    auto it = Controller->GetActiveBackends().begin();
    ++it;
    const ui32 brokenBackend = *it;
    {
        const TJsonPtr indices = Controller->ProcessCommand("get_final_indexes&full_path=true", brokenBackend);
        Controller->ProcessCommand("stop", brokenBackend);
        for (const auto& dirVal: indices->GetArray()) {
            for (const auto& dir : dirVal["dirs"].GetArray()) {
                const TFsPath indexDir(dir.GetString());
                TFile key(indexDir / "indexkey", RdWr | CreateAlways);
                TFile inv(indexDir / "indexinv", RdWr | CreateAlways);
            }
        }
        MUST_BE_BROKEN(Controller->RestartBackend(brokenBackend));
        const TInstant start = Now();
        while (Now() - start < TDuration::Seconds(300)) {
            Sleep(TDuration::Seconds(2));
            const TString res = Controller->SendCommandToDeployManager("broadcast?command=get_status&ctype=sbtests&filter=$status$");
            DEBUG_LOG << res << Endl;
            NJson::TJsonValue jsonVal;
            CHECK_TEST_EQ(NJson::ReadJsonFastTree(res, &jsonVal), true);
            const auto addr = Controller->GetConfig().Controllers[brokenBackend];
            const TString addrS = addr.Host + ":" + ToString(addr.Port - 3);
            DEBUG_LOG << "check path " << addrS << Endl;
            DEBUG_LOG << "check value " << jsonVal["tests"][addrS]["$status$"]["status"].GetString() << Endl;
            if (jsonVal["tests"][addrS]["$status$"]["status"].GetString() == "FailedIndex") {
                break;
            }
        };
    }

    NDaemonController::TClusterControlAction ra("sbtests", "tests",
        TVector<TString>(1, HostName() + ":" + ToString(Controller->GetConfig().Controllers[cleanBackend].Port - 3)), TVector<TString>(),
        NDaemonController::apStartAndWait, true);

    // No useful slots to restore from
    MUST_BE_BROKEN(Controller->ExecuteActionOnDeployManager(ra));
    return true;
}
};


START_TEST_DEFINE_PARENT(TestDeployManagerReplaceReplica, TestDeployManager)
void InitCluster() override {
    UploadCommon();
    UploadService("tests");
    ConfigureCluster(3, 2, NSaas::UrlHash, "rtyserver", "tests");
    DeploySp();
    DeployIp();
}

bool Run() override {
    i64 countMessages = 30;
    TVector<NRTYServer::TMessage> messages;
    TSet<std::pair<ui64, TString> > deleted;
    GenerateInput(messages, countMessages, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed());
    {
        TDebugLogTimer timer("Index messages full cluster");
        IndexMessages(messages, DISK, 1);
        DeleteSomeMessages(messages, deleted, DISK, 3);
        ReopenIndexers();
    }
    TBackendProxy::TBackendSet lifeBe = Controller->GetActiveBackends();
    /*
    messages.clear();
    GenerateInput(messages, countMessages, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed());
    DEBUG_LOG << "Index messages part of cluster..." << Endl;
    IndexMessages(messages, DISK, 1);
    DeleteSomeMessages(messages, deleted, DISK, 3);
    DEBUG_LOG << "Index messages part of cluster...OK" << Endl;
    */
    TVector<TString> broken;
    TBackendProxy::TBackendSet::const_iterator br = lifeBe.begin();
    for (int i = 0; i < 2; ++i) {
        Controller->ProcessCommand("stop", *br);
        broken.push_back(HostName() + ":" + ToString((Controller->GetConfig().Controllers[*(br++)].Port - 3)));
    }
    {
        TDebugLogTimer timer(ToString(broken.size()) +  " backends (" + JoinStrings(broken, ",") + ") broken, restore");
        NDaemonController::TClusterControlAction ra("sbtests", "tests", TVector<TString>(), broken,
            NDaemonController::apStartAndWait, true);
        Controller->ExecuteActionOnDeployManager(ra);
    }
    CheckDocuments(messages, deleted, lifeBe.size());
    return true;
}
};

START_TEST_DEFINE_PARENT(TestDeployManagerReplaceAbsentReplica, TestDeployManager)
void InitCluster() override {
    UploadCommon();
    UploadService("tests");
    ConfigureCluster(2, 2, NSaas::UrlHash, "rtyserver", "tests");
    DeploySp();
    DeployIp();
}

bool Run() override {
    i64 countMessages = 30;
    TVector<NRTYServer::TMessage> messages;
    TSet<std::pair<ui64, TString> > deleted;
    GenerateInput(messages, countMessages, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed());
    {
        TDebugLogTimer timer("Index messages full cluster");
        IndexMessages(messages, DISK, 1);
        DeleteSomeMessages(messages, deleted, DISK, 3);
        ReopenIndexers();
    }
    const ui32 be = *Controller->GetActiveBackends().begin();
    const NSearchMapParser::TSearchMapHost smHost = FindBackend(be);
    {
        TVector<TString> slots;
        slots.push_back(Controller->GetConfig().Controllers[be].Host + ":" + ToString(Controller->GetConfig().Controllers[be].Port - 3));
        TDebugLogTimer timer("release slots");
        NDaemonController::TReleaseSlotsAction ra(slots, "sbtests", "tests", RTYSERVER_SERVICE, true, false);
        Controller->ExecuteActionOnDeployManager(ra);
    }
    {
        TDebugLogTimer timer("Absent backends replace");
        NDaemonController::TClusterControlAction ra("sbtests", "tests", NDaemonController::apStartAndWait);
        TVector<TString> absentSlots;
        absentSlots.push_back("_" + ToString(smHost.Shards.GetMin()) + "-" + ToString(smHost.Shards.GetMax()) + "_0");
        ra.SetAbsentSlots(absentSlots);
        TVector<TString> newSlots;
        newSlots.push_back(smHost.GetSlotName());
        ra.SetNewSlotsPool(newSlots);
        Controller->ExecuteActionOnDeployManager(ra);
    }
    {
        const NSearchMapParser::TSearchMap sm = GetSearchMapFromDM();
        const NSearchMapParser::TSearchMapService* servicePtr = sm.GetService("tests");
        if (!servicePtr)
            ythrow yexception() << "service tests not found in searchmap";
        const TVector<NSearchMapParser::TSearchMapHost>& slots = servicePtr->GetSlots();
        for (const auto& slot: slots) {
            if (slot.Name == smHost.Name && slot.ControllerPort() == smHost.ControllerPort()) {
                CHECK_TEST_EQ(slot.Shards.GetMin(), smHost.Shards.GetMin());
                CHECK_TEST_EQ(slot.Shards.GetMax(), smHost.Shards.GetMax());
                break;
            }
        }
    }

    return true;
}
};

START_TEST_DEFINE_PARENT(TestDeployManagerReleaseSlot, TestDeployManager)
void InitCluster() override {
    UploadCommon();
    UploadService("tests");
    ConfigureCluster(2, 1, NSaas::UrlHash, "rtyserver", "tests");
    DeploySp();
    DeployIp();
}

bool Run() override {
    i64 countMessages = 30;
    TVector<NRTYServer::TMessage> messages;
    GenerateInput(messages, countMessages, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed());
    {
        TDebugLogTimer timer("Index messages full cluster");
        IndexMessages(messages, DISK, 1);
        ReopenIndexers();
    }
    TVector<TString> slots;
    ui32 be = *Controller->GetActiveBackends().begin();
    slots.push_back(Controller->GetConfig().Controllers[be].Host + ":" + ToString(Controller->GetConfig().Controllers[be].Port - 3));
    TDebugLogTimer timer("release slots");
    NDaemonController::TReleaseSlotsAction ra(slots, "sbtests", "tests", RTYSERVER_SERVICE, true, false);
    Controller->ExecuteActionOnDeployManager(ra);
    TJsonPtr serverInfo(Controller->GetServerInfo());
    NJson::TJsonValue& info = (*serverInfo)[0];
    if (info["docs_in_disk_indexers"].GetStringRobust() != "0")
        ythrow yexception() << "index was not cleaned: " << info.GetStringRobust();
    return true;
}
};

START_TEST_DEFINE_PARENT(TestDeployManagerCheckSlots, TestDeployManager)

NSearchMapParser::TSlotsPool Pool;

void InitCluster() override {
    sleep(30);
    UploadCommon();
    UploadService("tests");
    Pool = ConfigureCluster(2, 2, NSaas::UrlHash, "rtyserver", "tests");
    DeploySp();
    DeployIp();
}

bool Run() override {
    i64 countMessages = 30;
    TVector<NRTYServer::TMessage> messages;
    GenerateInput(messages, countMessages, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed());
    {
        TDebugLogTimer timer("Index messages full cluster");
        IndexMessages(messages, DISK, 1);
        ReopenIndexers();
    }
    TString res = Controller->SendCommandToDeployManager("broadcast?command=get_status&ctype=sbtests&filter=$status$");
    DEBUG_LOG << res << Endl;
    NJson::TJsonValue jsonVal;
    CHECK_TEST_FAILED(!NJson::ReadJsonFastTree(res, &jsonVal), "Incorrect json");
    for (auto&& i : Pool.GetSlots()) {
        CHECK_TEST_EQ(jsonVal["tests"][i.GetSlotName()]["$status$"]["status"].GetString(), "OK");
        CHECK_TEST_NEQ(jsonVal["tests"][i.GetSlotName()]["$status$"]["last_ping"].GetInteger(), 0);
        CHECK_TEST_NEQ(jsonVal["tests"][i.GetSlotName()]["$status$"]["last_reply"].GetInteger(), 0);
        CHECK_TEST_EQ(jsonVal["tests"][i.GetSlotName()]["$status$"]["pings_info"].IsMap(), true);
    }
    return true;
}
};

START_TEST_DEFINE_PARENT(TestDeployManagerAddEmptyReplica, TestDeployManager)
void InitCluster() override {
    UploadCommon();
    UploadService("tests");
    ConfigureCluster(1, 2, NSaas::UrlHash, "rtyserver", "tests");
    DeploySp();
    DeployIp();
}

bool Run() override {
    TDebugLogTimer timer("Add replica");
    for (int i = 0; i < 2; ++i) {
        NRTYCluster::TSlotsAllocator sa;
        sa.AllowSameHostReplica = true;
        NDaemonController::TAddReplicaAction replica(2, "tests", "sbtests", 1, sa, NDaemonController::apStartAndWait, RTYSERVER_SERVICE, true);
        Controller->ExecuteActionOnDeployManager(replica);
    }
    return true;
}
};

START_TEST_DEFINE_PARENT(TestDeployManagerDisableAll, TestDeployManager)
NSearchMapParser::TSlotsPool Replica;
NSearchMapParser::TSlotsPool SearchSlots;
void InitCluster() override {
    UploadCommon();
    UploadService("tests");
    SearchSlots = ConfigureCluster(2, 2, NSaas::UrlHash, "rtyserver", "tests");
    Replica.Add(SearchSlots.GetSlots()[0]);
    DeploySp();
    DeployIp();
}

bool Run() override {
    i64 countMessages = 30;
    TVector<NRTYServer::TMessage> messages;
    GenerateInput(messages, countMessages, NRTYServer::TMessage::MODIFY_DOCUMENT, GetIsPrefixed());

    {
        TDebugLogTimer timer("Disable all indexing");
        NDaemonController::TSimpleSearchmapModifAction dsf(SearchSlots, "sbtests", "tests", "disable_indexing", RTYSERVER_SERVICE, true);
        try {
            Controller->ExecuteActionOnDeployManager(dsf);
            CHECK_TEST_FAILED(true, "Incorrect command executed normally");
        } catch (...) {
        }
    }
    {
        TDebugLogTimer timer("Disable all searching");
        NDaemonController::TSimpleSearchmapModifAction dsf(SearchSlots, "sbtests", "tests", "disable_search", RTYSERVER_SERVICE, true);
        try {
            Controller->ExecuteActionOnDeployManager(dsf);
            CHECK_TEST_FAILED(true, "Incorrect command executed normally");
        } catch (...) {
        }
    }

    {
        TDebugLogTimer timer("Disable slot indexing");
        NDaemonController::TSimpleSearchmapModifAction dsf(Replica, "sbtests", "tests", "disable_indexing", RTYSERVER_SERVICE, true);
        try {
            Controller->ExecuteActionOnDeployManager(dsf);
        } catch (...) {
            CHECK_TEST_FAILED(true, "Command not executed normally");
        }
    }
    CheckEnabled(&Replica, nullptr);

    {
        try {
            TDebugLogTimer timer("Index messages full cluster");
            IndexMessages(messages, DISK, 1);
            CHECK_TEST_FAILED(true, "Incorrect behaviour after indexing full disabling");
        } catch (...) {
        }
    }

    {
        TDebugLogTimer timer("Enable slot indexing");
        NDaemonController::TSimpleSearchmapModifAction dsf(Replica, "sbtests", "tests", "enable_indexing", RTYSERVER_SERVICE, true);
        try {
            Controller->ExecuteActionOnDeployManager(dsf);
        } catch (...) {
            CHECK_TEST_FAILED(true, "Command not executed normally");
        }
    }
    CheckEnabled(nullptr, nullptr);

    {
        TDebugLogTimer timer("Index messages full cluster");
        IndexMessages(messages, DISK, 1);
        ReopenIndexers();
    }

    TSet<std::pair<ui64, TString>> deleted;
    CheckDocuments(messages, deleted);

    {
        TDebugLogTimer timer("Disable slot search");
        NDaemonController::TSimpleSearchmapModifAction dsf(Replica, "sbtests", "tests", "disable_search", RTYSERVER_SERVICE, true);
        try {
            Controller->ExecuteActionOnDeployManager(dsf);
        } catch (...) {
            CHECK_TEST_FAILED(true, "Command not executed normally");
        }
    }

    CheckDocuments(messages, deleted);
    CheckEnabled(nullptr, &Replica);

    {
        TDebugLogTimer timer("Enable slot search");
        NDaemonController::TSimpleSearchmapModifAction dsf(Replica, "sbtests", "tests", "enable_search", RTYSERVER_SERVICE, true);
        try {
            Controller->ExecuteActionOnDeployManager(dsf);
        } catch (...) {
            CHECK_TEST_FAILED(true, "Command not executed normally");
        }
    }

    CheckDocuments(messages, deleted);
    CheckEnabled(nullptr, nullptr);
    return true;
}
};

START_TEST_DEFINE_PARENT(TestDeployManagerDisableIndexingWithQueue, TestDeployManager)
NSearchMapParser::TSlotsPool Replica;
NSearchMapParser::TSlotsPool SearchSlots;
void InitCluster() override {
    UploadCommon();
    UploadService("tests");
    SearchSlots = ConfigureCluster(2, 2, NSaas::UrlHash, "rtyserver", "tests");
    Replica.Add(SearchSlots.GetSlots()[0]);
    Replica.Add(SearchSlots.GetSlots()[2]);
    DeploySp();
    UploadServiceCommon("", INDEXER_PROXY_SERVICE, 1);
    DeployIp();
}

bool Run() override {
    i64 countMessages = 30;
    TVector<NRTYServer::TMessage> messages;
    GenerateInput(messages, countMessages, NRTYServer::TMessage::MODIFY_DOCUMENT, GetIsPrefixed());

    ui32 kps = GetIsPrefixed() ? 1 : 0;

    for (auto&& i : messages) {
        i.MutableDocument()->SetKeyPrefix(kps);
    }

    {
        TDebugLogTimer timer("Disable slot indexing");
        NDaemonController::TSimpleSearchmapModifAction dsf(Replica, "sbtests", "tests", "disable_indexing", RTYSERVER_SERVICE, true);
        Controller->ExecuteActionOnDeployManager(dsf);
    }

    IndexMessages(messages, REALTIME, 1);

    sleep(10);

    NJson::TJsonValue infoIP = Controller->GetInfoServerProxy("indexer");
    DEBUG_LOG << infoIP.GetStringRobust() << Endl;
    ui32 count = 0;
    NJson::TJsonValue queuesJson;
    CHECK_TEST_TRUE(infoIP.GetValueByPath("result.queues.tests", queuesJson, '.'));
    for (ui32 i = 0; i < Replica.Size(); ++i) {
        NJson::TJsonValue qSizeJson;

        const TString path = Replica.GetSlots()[i].Name + ":" + ToString(Replica.GetSlots()[i].IndexerPort);
        DEBUG_LOG << path << Endl;

        CHECK_TEST_TRUE(queuesJson.Has(path));
        CHECK_TEST_TRUE(queuesJson[path].Has("docs"));
        count += queuesJson[path]["docs"].GetUInteger();
    }

    CHECK_TEST_EQ(count, 30);

    {
        TDebugLogTimer timer("Enable all indexing");
        NDaemonController::TSimpleSearchmapModifAction dsf(Replica, "sbtests", "tests", "enable_indexing", RTYSERVER_SERVICE, true);
        Controller->ExecuteActionOnDeployManager(dsf);
    }

    sleep(10);

    TVector<TDocSearchInfo> results;
    TQuerySearchContext qsc;
    qsc.ResultCountRequirement = 30;
    qsc.AttemptionsCount = 3;
    qsc.PrintResult = true;

    for (ui32 i = 0; i < 10; ++i) {
        QuerySearch("url:\"*\"&service=tests&relev=" + ToString(i) + "&kps=" + ToString(kps), results, qsc);
        CHECK_TEST_EQ(results.size(), 30);
    }

    return true;
}
};

START_TEST_DEFINE_PARENT(TestDeployManagerSearchAllSources, TestDeployManager)
NSearchMapParser::TSlotsPool Replica1;
NSearchMapParser::TSlotsPool Replica2;
void InitCluster() override {
    UploadCommon();
    UploadService("tests");
    NSearchMapParser::TSlotsPool SearchSlots = ConfigureCluster(2, 1, NSaas::UrlHash, "rtyserver", "tests");
    Replica1.Add(SearchSlots.GetSlots()[0]);
    Replica2.Add(SearchSlots.GetSlots()[1]);
    DeploySp();
    DeployIp();
}

bool PrepareReplica(const NSearchMapParser::TSlotsPool& Replica, const TVector<NRTYServer::TMessage>& messages) {
    {
        TDebugLogTimer timer("Disable slot indexing");
        NDaemonController::TSimpleSearchmapModifAction dsf(Replica, "sbtests", "tests", "disable_indexing", RTYSERVER_SERVICE, true);
        try {
            Controller->ExecuteActionOnDeployManager(dsf);
        } catch (...) {
            CHECK_TEST_FAILED(true, "Command not executed normally");
        }
    }
    CheckEnabled(&Replica, nullptr);

    {
        try {
            TDebugLogTimer timer("Index messages full cluster");
            IndexMessages(messages, REALTIME, 1);
            CHECK_TEST_FAILED(true, "Incorrect behaviour after indexing full disabling");
        } catch (...) {
        }
    }

    {
        TDebugLogTimer timer("Disable slot indexing");
        NDaemonController::TSimpleSearchmapModifAction dsf(Replica, "sbtests", "tests", "enable_indexing", RTYSERVER_SERVICE, true);
        try {
            Controller->ExecuteActionOnDeployManager(dsf);
        } catch (...) {
            CHECK_TEST_FAILED(true, "Command not executed normally");
        }
    }
    CheckEnabled(nullptr, nullptr);
    return true;
}

bool Run() override {
    i64 countMessages = 1;
    TVector<NRTYServer::TMessage> messages1;
    GenerateInput(messages1, countMessages, NRTYServer::TMessage::MODIFY_DOCUMENT, GetIsPrefixed());
    TVector<NRTYServer::TMessage> messages2;
    GenerateInput(messages2, countMessages, NRTYServer::TMessage::MODIFY_DOCUMENT, GetIsPrefixed());

    messages2[0].MutableDocument()->SetKeyPrefix(messages1[0].MutableDocument()->GetKeyPrefix());

    CHECK_TEST_EQ(PrepareReplica(Replica1, messages1), true);
    CHECK_TEST_EQ(PrepareReplica(Replica2, messages2), true);

    TVector<TDocSearchInfo> results;
    QuerySearch("body&service=tests&kps=" + ToString(messages1[0].MutableDocument()->GetKeyPrefix()), results);
    CHECK_TEST_EQ(results.size(), 1);

    UploadService("tests", 4);
    DeploySp("tests");

    QuerySearch("body&service=tests&kps=" + ToString(messages1[0].MutableDocument()->GetKeyPrefix()), results);
    CHECK_TEST_EQ(results.size(), 2);

    UploadService("tests", 5);
    DeploySp("tests");

    QuerySearch("body&service=tests&kps=" + ToString(messages1[0].MutableDocument()->GetKeyPrefix()), results);
    CHECK_TEST_EQ(results.size(), 1);

    UploadService("tests", 6);
    MUST_BE_BROKEN(DeploySp("tests"));

    QuerySearch("body&service=tests&kps=" + ToString(messages1[0].MutableDocument()->GetKeyPrefix()), results);
    CHECK_TEST_EQ(results.size(), 1);

    UploadService("tests", 7);
    MUST_BE_BROKEN(DeployBe("tests"));

    QuerySearch("body&service=tests&kps=" + ToString(messages1[0].MutableDocument()->GetKeyPrefix()), results);
    CHECK_TEST_EQ(results.size(), 1);

    UploadService("tests", 8);
    MUST_BE_BROKEN(DeployBe("tests"));

    try {
        CheckServiceConfig("tests", 8);
        CHECK_TEST_FAILED(true, "no detected error");
    } catch (...) {
    }

    QuerySearch("body&service=tests&kps=" + ToString(messages1[0].MutableDocument()->GetKeyPrefix()), results);
    CHECK_TEST_EQ(results.size(), 1);

    UploadService("tests", 1);
    DeployBe("tests");

    try {
        CheckServiceConfig("tests", 1);
    } catch (...) {
        CHECK_TEST_FAILED(true, "error in configs compare");
    }

    return true;
}
};

START_TEST_DEFINE_PARENT(TestDeployManagerDeployBadConfigs, TestDeployManager)
void InitCluster() override {
    UploadCommon();
    UploadService("tests");
    ConfigureCluster(1, 1, NSaas::UrlHash, "rtyserver", "tests");
    DeploySp();
    DeployIp();
    DeployBe("tests");
}
bool Run() override {
    i64 countMessages = 1;
    TVector<NRTYServer::TMessage> messages;
    GenerateInput(messages, countMessages, NRTYServer::TMessage::MODIFY_DOCUMENT, GetIsPrefixed());
    IndexMessages(messages, DISK, 1);
    ReopenIndexers();
    UploadService("tests", 9);
    NSearchMapParser::TSlotsPool slots;
    NRTYDeploy::TDeployAction daBE("tests", "sbtests", "NEW", NDaemonController::apStartAndWait, RTYSERVER_SERVICE, 1, &slots);
    Controller->ExecuteActionOnDeployManager(daBE);
    Controller->AbortBackends(TDuration::Seconds(120));
    TVector<TDocSearchInfo> results;
    QuerySearch("body&service=tests&kps=" + ToString(messages[0].MutableDocument()->GetKeyPrefix()), results);
    CHECK_TEST_EQ(results.size(), 1);
    return true;
}
};

START_TEST_DEFINE_PARENT(TestDeployManagerAddReplica, TestDeployManager)
void InitCluster() override {
    UploadCommon();
    UploadService("tests");
    ConfigureCluster(1, 2, NSaas::UrlHash, "rtyserver", "tests");
    DeploySp();
    DeployIp();
}

bool Run() override {
    i64 countMessages = 30;
    TVector<NRTYServer::TMessage> messages;
    TSet<std::pair<ui64, TString> > deleted;
    GenerateInput(messages, countMessages, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed());
    {
        TDebugLogTimer timer("Index messages full cluster");
        IndexMessages(messages, DISK, 1);
        DeleteSomeMessages(messages, deleted, DISK, 3);
        ReopenIndexers();
    }
    {
        TDebugLogTimer timer("Add replica");
        NRTYCluster::TSlotsAllocator sa;
        sa.AllowSameHostReplica = true;

        NRTYDeploy::TAddIntSearchAction addInt("tests", "sbtests", NDaemonController::apStartAndWait, 2, 2, sa);
        Controller->ExecuteActionOnDeployManager(addInt);
        Controller->SetRequiredMetaSearchLevel(3);
    }
    TDebugLogTimer timer("Add replica");
    for (int i = 0; i < 2; ++i) {
        NRTYCluster::TSlotsAllocator sa;
        sa.AllowSameHostReplica = true;
        NDaemonController::TAddReplicaAction replica(2, "tests", "sbtests", 1, sa, NDaemonController::apStartAndWait, RTYSERVER_SERVICE, true);
        Controller->ExecuteActionOnDeployManager(replica);
        CheckDocuments(messages, deleted);
    }
    return true;
}
};

SERVICE_TEST_RTYSERVER_DEFINE_PARENT(TestDeployManagerReshard, TestDeployManager)
void InitClusterBy(ui32 m, NSaas::ShardingType shardBy, ui32 kpsShift) {
    UploadCommon();
    UploadService("tests");
    ConfigureCluster(1, m, NSaas::TShardsDispatcher::TContext(shardBy, kpsShift), "rtyserver", "tests");
    DeploySp();
    DeployIp();
    ShardBy = shardBy;
}
TVector<NRTYServer::TMessage> Messages;
TSet<std::pair<ui64, TString> > Deleted;
NSaas::ShardingType ShardBy;

void ReshardTo(ui32 n) {
    {
    TDebugLogTimer timer("Reshard index to " + ToString(n) + " shards");
    NDaemonController::TReshardAction ra(0, NSearchMapParser::SearchMapShards, n, "sbtests", "tests", RTYSERVER_SERVICE);
    Controller->ExecuteActionOnDeployManager(ra);
    }
    CheckDocuments(Messages, Deleted);
}

void PrepareMessages() {
    i64 countMessages = 300;
    bool isPrefixed = GetIsPrefixed();
    GenerateInput(Messages, countMessages, NRTYServer::TMessage::ADD_DOCUMENT, isPrefixed);
    if (isPrefixed)
        for (TVector<NRTYServer::TMessage>::iterator i = Messages.begin(); i != Messages.end(); ++i)
            i->MutableDocument()->SetKeyPrefix(RandomNumber<ui64>(Max<ui64>() / 2));

    TDebugLogTimer timer("Index messages full cluster");
    IndexMessages(Messages, DISK, 1);
    ReopenIndexers();
    CheckDocuments(Messages, Deleted);
    DeleteSomeMessages(Messages, Deleted, DISK, 3);
}

void RunMtoN(ui32 n) {
    if (ShardBy == NSaas::KeyPrefix && !GetIsPrefixed())
        return;
    PrepareMessages();
    ReshardTo(n);
}
};

START_TEST_DEFINE_PARENT(TestDeployManagerReshardSomeTimes, TestDeployManagerReshard)
    void InitCluster() override {
        InitClusterBy(1, NSaas::UrlHash, 0);
    }
    bool Run() override {
        PrepareMessages();
        ReshardTo(7);
        ReshardTo(1);
        ReshardTo(3);
        ReshardTo(1);
        return true;
    }
};

#define DEFINE_RESHARD_M_TO_N_TEST(name, M, N, ShardBy, KpsShift) \
    START_TEST_DEFINE_PARENT(name, TestDeployManagerReshard)\
        void InitCluster() override {\
            InitClusterBy(M, ShardBy, KpsShift);\
        }\
        bool Run() override {\
            RunMtoN(N);\
            return true;\
        }\
    };

DEFINE_RESHARD_M_TO_N_TEST(TestDeployManagerReshard1ToN, 1, 4, NSaas::KeyPrefix, 0);
DEFINE_RESHARD_M_TO_N_TEST(TestDeployManagerReshard1ToNMix5, 1, 4, NSaas::KeyPrefix, 5);
DEFINE_RESHARD_M_TO_N_TEST(TestDeployManagerReshardMTo1, 4, 1, NSaas::KeyPrefix, 0);
DEFINE_RESHARD_M_TO_N_TEST(TestDeployManagerReshardMTo1Mix7, 4, 1, NSaas::KeyPrefix, 7);
DEFINE_RESHARD_M_TO_N_TEST(TestDeployManagerReshardMToN, 3, 5, NSaas::KeyPrefix, 0);
DEFINE_RESHARD_M_TO_N_TEST(TestDeployManagerReshardMToNMix10, 3, 5, NSaas::KeyPrefix, 10);
DEFINE_RESHARD_M_TO_N_TEST(TestDeployManagerReshard1ToNUrlHash, 1, 4, NSaas::UrlHash, 0);
DEFINE_RESHARD_M_TO_N_TEST(TestDeployManagerReshardMTo1UrlHash, 4, 1, NSaas::UrlHash, 0);
DEFINE_RESHARD_M_TO_N_TEST(TestDeployManagerReshardMToNUrlHash, 3, 5, NSaas::UrlHash, 0);

DEFINE_RESHARD_M_TO_N_TEST(TestDeployManagerReshard2To10UrlHash, 2, 10, NSaas::UrlHash, 0);
DEFINE_RESHARD_M_TO_N_TEST(TestDeployManagerReshard10To2UrlHash, 10, 2, NSaas::UrlHash, 0);
