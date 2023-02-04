#include "deploy_manager.h"

#include <saas/rtyserver_test/testerlib/standart_generator.h>
#include <saas/api/factors_erf.h>
#include <saas/deploy_manager/meta/cluster.h>
#include <saas/deploy_manager/scripts/deploy/action.h>
#include <saas/deploy_manager/scripts/add_intsearch/action.h>
#include <saas/deploy_manager/scripts/add_replica/action.h>
#include <saas/deploy_manager/scripts/searchmap/action.h>
#include <saas/library/daemon_base/config/daemon_config.h>
#include <saas/library/daemon_base/controller_actions/restart.h>
#include <saas/library/searchmap/parsers/json/json.h>
#include <saas/util/json/json.h>
#include <saas/util/external/dc.h>
#include <search/idl/meta.pb.h>
#include <yweb/realtime/distributor/client/distclient.h>
#include <google/protobuf/text_format.h>
#include <util/string/vector.h>

START_TEST_DEFINE_PARENT(TestMultipartArchiveRepairAfterAbort, TestDeployManager)
bool InitConfig() override {
    SetIndexerParams(DISK, 10, 1);
    SetMergerParams(false);
    (*ConfigDiff)["IndexGenerator"] = INDEX_COMPONENT_NAME;
    (*ConfigDiff)["Components"] = INDEX_COMPONENT_NAME;
    (*ConfigDiff)["Indexer.Memory.Enabled"] = "false";
    (*ConfigDiff)["Searcher.ArchiveType"] = "AT_MULTIPART";
    (*ConfigDiff)["Searcher.SnippetsDeniedZones"] = "";
    (*ConfigDiff)["Searcher.TwoStepQuery"] = "false";
    return true;
}

bool Run() override {
    UploadCommon();
    UploadService("tests");
    ConfigureCluster(1, 1, NSaas::UrlHash, "rtyserver", "tests");
    DeployBe("tests");

    using namespace NRTYArchive;

    TVector<NRTYServer::TMessage> messages;
    GenerateInput(messages, 50, NRTYServer::TMessage::MODIFY_DOCUMENT, GetIsPrefixed());
    IndexMessages(messages, DISK, 1);
    ReopenIndexers();
    CHECK_TEST_EQ(GetSearchableDocsCount(Controller), 50);
    Controller->AbortBackends(TDuration::Seconds(120));
    CHECK_TEST_EQ(GetSearchableDocsCount(Controller), 50);
    return true;
}
};

START_TEST_DEFINE_PARENT(TestDeployManagerOneService, TestDeployManager)
    bool Run() override {
        UploadCommon();
        UploadService("tests");
        ConfigureCluster(1, 1, NSaas::UrlHash, "rtyserver", "tests");
        DeployBe("tests");
        CHECK_TEST_TRUE(CheckServiceConfigSafe("tests", 0));
        UploadService("tests", 1);
        DeployBe("tests");
        CHECK_TEST_TRUE(CheckServiceConfigSafe("tests", 1));

        NRTYCluster::TSlotsAllocator sa;
        sa.AllowSameHostReplica = true;
        NRTYDeploy::TAddIntSearchAction addInt("tests", "sbtests", NDaemonController::apStartAndWait, 1, 2, sa);
        Controller->ExecuteActionOnDeployManager(addInt);
        Controller->SetRequiredMetaSearchLevel(3);

        DeployIp();

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
        CheckDocuments(messages, deleted);

        return true;
    }
};

START_TEST_DEFINE_PARENT(TestDeployManagerOneServiceWithMacros, TestDeployManager)
    bool Run() override {
        UploadCommon();
        UploadService("service_with_macros");
        ConfigureCluster(1, 1, NSaas::UrlHash, "rtyserver", "service_with_macros");
        DeployBe("service_with_macros");
        DeployIp();

        i64 countMessages = 10;
        TVector<NRTYServer::TMessage> messages;
        GenerateInput(messages, countMessages, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed());
        {
            TDebugLogTimer timer("Index messages full cluster");
            TIndexerClient::TContext context;
            context.Service = "service_with_macros";
            IndexMessages(messages, DISK, context);
            ReopenIndexers("service_with_macros");
        }

        CheckSearchResults(messages, TSearchMessagesContext::BuildDefault("service_with_macros"));

        return true;
    }
};

START_TEST_DEFINE_PARENT(TestDeployManagerRenameService, TestDeployManager)
    void IndexMessagesForService(TVector<NRTYServer::TMessage>& messages, const TString& service = "service_with_macros") {
        i64 countMessages = 10;
        GenerateInput(messages, countMessages, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed());
        TDebugLogTimer timer("Index messages full cluster");
        TIndexerClient::TContext context;
        context.Service = service;
        IndexMessages(messages, DISK, context);
        ReopenIndexers(service);
    }

    NJson::TJsonValue GetClusterMap(const TString& service) {
        TString res = Controller->SendCommandToDeployManager("get_cluster_map?service=" + service  + "&ctype=sbtests");
        NJson::TJsonValue result;
        TStringInput si(res);
        if (!NJson::ReadJsonTree(&si, &result)) {
            ythrow yexception() << "incorrect json from get_cluster_map: " << res;
        }
        return result;
    }

    bool Run() override {
        UploadCommon();
        UploadService("service_with_macros");
        ConfigureCluster(1, 1, NSaas::UrlHash, "rtyserver", "service_with_macros");
        DeployBe("service_with_macros");
        NRTYCluster::TSlotsAllocator sa;
        sa.AllowSameHostReplica = true;
        DeploySp();
        DeployIp();

        TVector<NRTYServer::TMessage> messages;
        IndexMessagesForService(messages, "service_with_macros");
        CheckSearchResults(messages, TSearchMessagesContext::BuildDefault("service_with_macros"));

        NJson::TJsonValue testsClusterMap = GetClusterMap("service_with_macros");
        const NJson::TJsonValue* sources = testsClusterMap.GetValueByPath("cluster.service_with_macros", '.');
        if (!sources) {
            ythrow yexception() << "Section 'cluster.service_with_macros' not found, answer: " << testsClusterMap.GetString();
        }
        TString testsClusterConfig = sources->GetString();

        Controller->UploadDataToDeployManager("{\"sbtests\":\"sbtests_service_with_macros_tag\"}", "/configs/service_with_macros/tags.info");
        TString tagsInfo = Controller->SendCommandToDeployManager("process_storage?action=get&path=/configs/service_with_macros/tags.info");

        NDaemonController::TSimpleSearchmapModifAction renameService("sbtests", "service_with_macros", "rename_service", "rtyserver",true,  "new_service_with_macros", false, true);
        Controller->ExecuteActionOnDeployManager(renameService);

        TString newTagInfo = Controller->SendCommandToDeployManager("process_storage?action=get&path=/configs/new_service_with_macros/tags.info");
        if (newTagInfo != tagsInfo) {
            ERROR_LOG << tagsInfo << "\nnew: " << newTagInfo;
            TEST_FAILED("Config with tags info was changed");
        }

        Controller->SendCommandToDeployManager("process_storage?action=rm&path=/configs/new_service_with_macros/tags.info");

        {
            NJson::TJsonValue clusterMap = GetClusterMap("new_service_with_macros");
            const NJson::TJsonValue* sources = clusterMap.GetValueByPath("cluster.new_service_with_macros.config_types.default.sources", '.');
            if (!sources) {
                ythrow yexception() << "Section 'cluster.new_service_with_macros.config_types.default.sources' not found, answer: " << clusterMap.GetString();
             }
            if (clusterMap.GetValueByPath("cluster.new_service_with_macros", '.')->GetString() != testsClusterConfig) {
                ythrow yexception() << "Section 'cluster.new_service_with_macros.' was changed: " << testsClusterConfig << "\nnew: " << clusterMap.GetValueByPath("cluster.new_service_with_macros", '.')->GetString();
            }
        }
        {
            NJson::TJsonValue clusterMap = GetClusterMap("service_with_macros");
            const NJson::TJsonValue* sources = clusterMap.GetValueByPath("cluster.service_with_macros.config_types", '.');
            if (sources) {
                ythrow yexception() << "Section 'cluster.service_with_macros.config_types' should not be found, answer: " << clusterMap.GetString();
            }
        }

        DeployBe("new_service_with_macros");
        DeployIp("new_service_with_macros");
        DeploySp();
        DeploySp("new_service_with_macros");
        DeployIp();

        CheckSearchResults(messages, TSearchMessagesContext::BuildDefault("new_service_with_macros"));
        try {
            CheckSearchResults(messages, TSearchMessagesContext::BuildDefault("service_with_macros"));
            TEST_FAILED("Service with old name don't removed.");
        } catch (...) {
        }

        messages.clear();
        IndexMessagesForService(messages, "new_service_with_macros");
        CheckSearchResults(messages, TSearchMessagesContext::BuildDefault("new_service_with_macros"));

        return true;
    }
};


START_TEST_DEFINE_PARENT(TestDeployManagerCopyServiceWithAddToSearchMap, TestDeployManager)
     void IndexMessagesForService(TVector<NRTYServer::TMessage>& messages, const TString& service = "service_with_macros") {
         i64 countMessages = 10;
         GenerateInput(messages, countMessages, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed());
         TDebugLogTimer timer("Index messages full cluster");
         TIndexerClient::TContext context;
         context.Service = service;
         IndexMessages(messages, DISK, context);
         ReopenIndexers(service);
     }

    bool Run() override {
        UploadCommon();
        UploadService("service_with_macros");
        NSearchMapParser::TSlotsPool slotsPool =  ConfigureCluster(1, 1, NSaas::UrlHash, "rtyserver", "service_with_macros");
        DeployBe("service_with_macros");
        TVector<NRTYServer::TMessage> messages;
        DeployIp();
        DeploySp();

        IndexMessagesForService(messages, "service_with_macros");
        NDaemonController::TSimpleSearchmapModifAction copyService("sbtests", "service_with_macros", "copy_service", "rtyserver", true, "new_service_with_macros", false, true);
        Controller->ExecuteActionOnDeployManager(copyService);

        TVector<TString> services = {"new_service_with_macros", "service_with_macros"};
        for (auto& service : services) {
            TString clusterMap = Controller->SendCommandToDeployManager("get_cluster_map?service=" + service  + "&ctype=sbtests");
            NJson::TJsonValue clusterMapJson;
            TStringInput si(clusterMap);
            if (!NJson::ReadJsonTree(&si, &clusterMapJson)) {
                ythrow yexception() << "incorrect json from get_cluster_map: " << clusterMap;
            }
            const NJson::TJsonValue* sources = clusterMapJson.GetValueByPath("cluster." + service, '.');
            if (!sources) {
                ythrow yexception() << "section 'cluster." << service << "' not found, answer: " << clusterMap;
            }
        }

        NRTYCluster::TSlotsAllocator sa;
        sa.AllowSameHostReplica = true;
        NDaemonController::TAddReplicaAction replica(1, "new_service_with_macros", "sbtests", 1, sa, NDaemonController::apStartAndWait, RTYSERVER_SERVICE, true);
        Controller->ExecuteActionOnDeployManager(replica);

        DeployBe("new_service_with_macros");
        DeployIp("new_service_with_macros");
        DeploySp();
        DeploySp("new_service_with_macros");
        DeployIp();

        CheckSearchResults(messages, TSearchMessagesContext::BuildDefault("service_with_macros"));
        messages.clear();
        IndexMessagesForService(messages, "new_service_with_macros");
        CheckSearchResults(messages, TSearchMessagesContext::BuildDefault("new_service_with_macros"));

        return true;
    }
};


START_TEST_DEFINE_PARENT(TestDeployManagerCopyServiceWithoutAddToSearchMap, TestDeployManager)
    bool Run() override {
        UploadCommon();
        UploadService("service_with_macros");
        NSearchMapParser::TSlotsPool slotsPool =  ConfigureCluster(1, 1, NSaas::UrlHash, "rtyserver", "service_with_macros");
        DeployBe("service_with_macros");
        NRTYCluster::TSlotsAllocator sa;
        sa.AllowSameHostReplica = true;
        DeployIp();

        NDaemonController::TSimpleSearchmapModifAction copyService("sbtests", "service_with_macros", "copy_service", "rtyserver",false,  "second_new_service_with_macros", false, true);
        Controller->ExecuteActionOnDeployManager(copyService);

        TString res = Controller->SendCommandToDeployManager("get_cluster_map?service=second_new_service_with_macros&ctype=sbtests");

        TStringInput si(res);
        NJson::TJsonValue result;

        if (!NJson::ReadJsonTree(&si, &result)) {
            ythrow yexception() << "incorrect json from get_cluster_map: " << res;
        }
        const NJson::TJsonValue* sources = result.GetValueByPath("cluster.second_new_service_with_macros", '.');
        if (sources) {
            ythrow yexception() << "section 'cluster.second_new_service_with_macros.config_types' should not be found in searchmap, answer: " << res;
        }

        return true;
    }
};


SERVICE_TEST_RTYSERVER_DEFINE_PARENT(TestDeployManagerMixShardingBase, TestDeployManager)
NSearchMapParser::TSlotsPool Slots;
ui32 KpsShift = 0;
ui32 MessagesCount = 30;

virtual void DoInitCluster(ui32 kpsShift, ui32 messagesCount) {
    KpsShift = kpsShift;
    MessagesCount = messagesCount;

    UploadCommon();
    UploadService("tests");
    NSaas::TShardsDispatcher::TContext shCtx(NSaas::KeyPrefix, KpsShift);
    Slots = ConfigureCluster(1, 3, shCtx, "rtyserver", "tests");
    DeploySp();
    DeployIp();
}

bool Run() override {
    if (!GetIsPrefixed()) {
        INFO_LOG << "Prefixed only test" << Endl;
        return false;
    }

    TVector<NRTYServer::TMessage> messages;
    GenerateInput(messages, MessagesCount, NRTYServer::TMessage::ADD_DOCUMENT, true);
    for (ui32 i = 0; i < messages.size(); ++i) {
        if (i % 3 == 0) {
            messages[i].MutableDocument()->SetKeyPrefix(1);
        } else if (i % 3 == 1) {
            messages[i].MutableDocument()->SetKeyPrefix((1 << 15));
        } else {
            messages[i].MutableDocument()->SetKeyPrefix(NSearchMapParser::SearchMapShards - 1);
        }
    }

    NSaas::TShardsDispatcher dispatcher(NSaas::TShardsDispatcher::TContext(NSaas::KeyPrefix, KpsShift));
    for (const auto& mes : messages) {
        INFO_LOG << "Shard for message " << mes.GetDocument().GetKeyPrefix() << "/" << mes.GetDocument().GetUrl() << ":" << dispatcher.GetShard(mes) << Endl;

    }

    for (const auto& slot : Slots.GetSlots()) {
        INFO_LOG << "Check interval: " << slot.Shards << Endl;
        for (const auto& mes : messages) {
            auto shard = dispatcher.GetShard(mes);
            if (dispatcher.CheckInterval(shard, slot.Shards)) {
                INFO_LOG << shard << " " << mes.GetDocument().GetUrl() << Endl;
            }
        }
    }

    INFO_LOG << "Check intervals:" << Endl;
    IndexMessages(messages, DISK, 1);
    TSet<std::pair<ui64, TString> > deleted;

    DeleteSomeMessages(messages, deleted, DISK, 5);
    ReopenIndexers();

    CheckDocuments(messages, deleted, 3);
    return true;
}
};

#define DEFINE_KPS_SHARDING_TEST(name, KpsShift, DocsCount) \
    START_TEST_DEFINE_PARENT(name, TestDeployManagerMixShardingBase)\
        void InitCluster() override {\
            DoInitCluster(KpsShift, DocsCount);\
        }\
    };

DEFINE_KPS_SHARDING_TEST(TestDeployManagerMixKps14, 14, 30);
DEFINE_KPS_SHARDING_TEST(TestDeployManagerMixKps5, 5, 30);
DEFINE_KPS_SHARDING_TEST(TestDeployManagerMixKps0, 0, 30);

START_TEST_DEFINE_PARENT(TestDeployManagerOneServiceFailedConfig, TestDeployManager)
bool CheckStates(ui32 failedCount, ui32 successCount, ui32 oldVersion, ui32 newVersion, ui32 oldConf, ui32 newConf) {
    ui32 countInitBE = 0;
    ui32 countOldBE = 0;
    ui32 fails = 0;
    ui32 success = 0;
    for (auto&& i : Controller->GetActiveBackends()) {
        if (CheckServiceConfigSafe("tests", newVersion, i)) {
            countInitBE++;
        }
        if (CheckServiceConfigSafe("tests", oldVersion, i)) {
            countOldBE++;
        }
        if (CheckServiceStatus("Active", i)) {
            success++;
        } else {
            fails++;
        }
    }

    CHECK_TEST_EQ(fails, failedCount);
    CHECK_TEST_EQ(success, successCount);

    CHECK_TEST_EQ(countInitBE + countOldBE, Controller->GetActiveBackends().size());
    CHECK_TEST_EQ(countInitBE, newConf);
    CHECK_TEST_EQ(countOldBE, oldConf);

    return true;
}

bool Run() override {
    UploadCommon(1);
    UploadService("tests", 12);
    NSearchMapParser::TSlotsPool slots = ConfigureCluster(2, 2, NSaas::UrlHash, "rtyserver", "tests");
    DeployBe("tests");
    CHECK_TEST_TRUE(CheckServiceConfigSafe("tests", 12));
    UploadService("tests", 11);
    try {
        DeployBe("tests");
        TEST_FAILED("error on deploy not detected");
    } catch (...) {
    }
    INFO_LOG << "check states before abort..." << Endl;
    CHECK_TEST_TRUE(CheckStates(2, 2, 12, 11, 2, 2));
    INFO_LOG << "check states before abort...OK" << Endl;

    UploadService("tests", 12);
    DeployBe("tests");
    CHECK_TEST_TRUE(CheckServiceConfigSafe("tests", 12));

    Controller->AbortBackends(TDuration::Seconds(120));
    INFO_LOG << "check states after abort..." << Endl;
    CHECK_TEST_TRUE(CheckStates(0, 4, 11, 12, 0, 4));
    INFO_LOG << "check states after abort...OK" << Endl;
    return true;
}
};

START_TEST_DEFINE_PARENT(TestDeployManagerOneServiceFailAndFixConfig, TestDeployManager)
bool Run() override {
    UploadCommon(1);
    UploadService("tests", 11);
    try {
        NSearchMapParser::TSlotsPool slots = ConfigureCluster(1, 1, NSaas::UrlHash, "rtyserver", "tests");
        TEST_FAILED("error on deploy not detected");
    }
    catch (...) {
    }
    UploadService("tests", 12);
    DeployBe("tests");
    Controller->WaitActiveServer(TDuration::Seconds(120));
    CHECK_TEST_TRUE(CheckServiceConfigSafe("tests", 12));
    return true;
}
};

START_TEST_DEFINE_PARENT(TestDeployManagerFailStepsInDeploy, TestDeployManager)
bool Run() override {
    UploadCommon();
    UploadService("tests");
    ConfigureCluster(1, 1, NSaas::UrlHash, "rtyserver", "tests");
    DeployBe("tests");
    UploadService("tests", 1);
    try {
        TDMBuildTaskFailer failer("DEPLOY_TASK");
        DeployBe("tests");
        ERROR_LOG << "Deploy does not fail" << Endl;
        return false;
    } catch (...) {
    }
    try {
        CheckServiceConfig("tests", 1);
        ERROR_LOG << "Deploy does not stopped on fail" << Endl;
        return false;
    }
    catch (...) {
    }
    return true;
}
};

START_TEST_DEFINE_PARENT(TestDeployManagerCurrentConfig, TestDeployManager)
void InitCluster() override {
    UploadCommon();
    UploadService("tests");
    ConfigureCluster(1, 1, NSaas::UrlHash, "rtyserver", "tests");
}
bool Run() override {
    UploadService("tests", 1);
    NDaemonController::TRestartAction ra(NDaemonController::apStartAndWait, true);
    ui32 be = *Controller->GetActiveBackends().begin();
    NDaemonController::TControllerAgent agent(Controller->GetConfig().Controllers[be].Host, Controller->GetConfig().Controllers[be].Port);
    if (!agent.ExecuteAction(ra))
        ythrow yexception() << "Errors while execute restart" << Endl;
    if (ra.IsFinished() && ra.IsFailed())
        ythrow yexception() << "Error in restart action: " << ra.GetInfo();
    CheckServiceConfig("tests", 0);
    return true;
}
};

START_TEST_DEFINE_PARENT(TestDeployManagerLostBackend, TestDeployManager)
void InitCluster() override {
    UploadCommon(2);
    UploadService("tests", 14);
    Controller->UploadDataToDeployManager(" ", "/common/unused/cluster.meta");
    Controller->UploadDataToDeployManager("{}", "/configs/unused/rtuserver.diff-unused");
    ConfigureCluster(1, 1, NSaas::UrlHash, "rtyserver", "tests");
}

bool CheckSearch(const TVector<NRTYServer::TMessage>& messages, const TString& comment) {
    INFO_LOG << "Check search " << comment << "..." << Endl;
    for (ui32 i = 0; i < messages.size(); ++i) {
        TVector<TDocSearchInfo> results;
        TQuerySearchContext::TDocProperties docProperties;
        TQuerySearchContext ctx;
        ctx.DocProperties = &docProperties;
        QuerySearch(messages[i].GetDocument().GetUrl() + "&sgkps=" + ToString(messages[i].GetDocument().GetKeyPrefix()), results, ctx);
        CHECK_TEST_EQ(results.size(), 1);
        CHECK_TEST_NEQ(docProperties.size(), 0);
        TString data(Base64Decode(docProperties[0]->find("data")->second));
        NRTYServer::TMessage::TDocument doc;
        Y_PROTOBUF_SUPPRESS_NODISCARD doc.ParseFromString(data);
        CHECK_TEST_EQ(doc.GetBody(), messages[i].GetDocument().GetBody());
    }
    INFO_LOG << "Check search " << comment << "...OK" << Endl;
    return true;
}

bool Run() override {
    UploadService("tests", 1);
    TVector<NRTYServer::TMessage> messages;
    GenerateInput(messages, 10, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed());
    for (ui32 i = 0; i < messages.size(); ++i) {
        messages[i].MutableDocument()->SetKeyPrefix(GetIsPrefixed() ? 2 : 0);
        messages[i].MutableDocument()->SetBody(ToString(i));
    }
    IndexMessages(messages, DISK, 1);
    ReopenIndexers();
    if (!CheckSearch(messages, "after indexing"))
        ythrow yexception() << "invalid indexing";
    Controller->UploadDataToDeployManager(" ", "/common/sbtests/cluster.meta");
    Controller->AbortBackends(TDuration::Seconds(120));
    if (!CheckSearch(messages, "after restart"))
        ythrow yexception() << "data was lost";
    return true;
}
};

START_TEST_DEFINE_PARENT(TestDeployManagerExternalSharding, TestDeployManager)
void InitCluster() override {
    UploadCommon();
    UploadService("tests");
    ConfigureCluster(1, 2, NSaas::External, "rtyserver", "tests");
    DeploySp();
    DeployIp();
    DeployBe("tests");
}

bool Run() override {
    TVector<NRTYServer::TMessage> messages;

    TString kps = GetIsPrefixed() ? "&kps=1" : "";

    GenerateInput(messages, 20, NRTYServer::TMessage::ADD_DOCUMENT, false);
    int i = 0;
    for (; i < 13; ++i) {
        messages[i].SetExternalShard(10);
    }
    for (; i < 20; ++i) {
        messages[i].SetExternalShard(63000);
    }
    if (GetIsPrefixed()) {
        for (int j = 0; j < 20; ++j)
            messages[j].MutableDocument()->SetKeyPrefix(1);
    }
    IndexMessages(messages, REALTIME, 1);

    TVector<TDocSearchInfo> results;
    QuerySearch("url:\"*\"&mss=0" + kps, results);
    if (results.size() != 13)
        ythrow yexception() << "incorrect number of results in mss=0";

    QuerySearch("url:\"*\"&mss=1" + kps, results);
    if (results.size() != 20 - 13)
        ythrow yexception() << "incorrect number of results in mss=1";

    return true;
}
};

START_TEST_DEFINE_PARENT(TestDeployManagerRevert, TestDeployManager)
void InitCluster() override {
    UploadCommon();
    UploadService("tests");
    ConfigureCluster(2, 2, NSaas::UrlHash, "rtyserver", "tests");
}

bool Run() override {
    TBackendProxy::TBackendSet stopBe;
    stopBe.insert(*Controller->GetActiveBackends().rbegin());
    Controller->ProcessCommand("no_file_operations&set=true", stopBe);
    UploadService("tests", 1);
    bool wasError = false;
    try {
        DeployBe("tests");
    } catch (...) {
        wasError = true;
    }
    if (!wasError)
        ythrow yexception() << "deploy to stopped backend " << Controller->GetConfig().Controllers[*stopBe.begin()].Host << ":" << Controller->GetConfig().Controllers[*stopBe.begin()].Port << "was success";
    Controller->ProcessCommand("no_file_operations&set=false", stopBe);
    for (TBackendProxy::TBackendSet::const_iterator i = Controller->GetActiveBackends().begin(); i != Controller->GetActiveBackends().end(); ++i) {
        wasError = false;
        TDebugLogTimer timer("Check config on backend " + Controller->GetConfig().Controllers[*i].Host + ":" + ToString(Controller->GetConfig().Controllers[*i].Port));
        try {
            CheckServiceConfig("tests", 1, *i);
        } catch (...) {
            wasError = true;
        }
        if (!wasError)
            ythrow yexception() << "there is new config on backend " << Controller->GetConfig().Controllers[*i].Host << ":" << Controller->GetConfig().Controllers[*i].Port;
    }
    CheckServiceConfig("tests", 0);
    return true;
}
};

START_TEST_DEFINE_PARENT(TestDeployManagerNTasks, TestDeployManager)
    bool Run() override {
        UploadCommon();
        UploadService("tests");
        UploadService("tests", 1);

        ConfigureCluster(1, 2, NSaas::UrlHash, "rtyserver", "tests");

        TVector<NDaemonController::TAction::TPtr> tasks;
        TSet<TString> ids;
        for (int i = 0; i < 5; ++i) {
            NRTYDeploy::TDeployAction daStart("tests", "sbtests", "NEW",
                NDaemonController::apStart, RTYSERVER_SERVICE);
            Controller->ExecuteActionOnDeployManager(daStart);
            NRTYDeploy::TWaitAsyncAction* daWait = new NRTYDeploy::TWaitAsyncAction(daStart.ActionId());
            daWait->SetIdTask(daStart.GetIdTask());
            tasks.push_back(daWait);
            ids.insert(daStart.GetIdTask());

            DEBUG_LOG << daStart.GetIdTask() << Endl;

            if (ids.size() > 2)
                ythrow yexception() << "too many unique task id";
        }
        for (TVector<NDaemonController::TAction::TPtr>::iterator i = tasks.begin(); i != tasks.end(); ++i) {
            Controller->ExecuteActionOnDeployManager(**i);
        }
        CheckServiceConfig("tests", 1);
        return true;
    }
};

START_TEST_DEFINE_PARENT(TestDeployManagerOneServiceDistributor, TestDeployManager)
bool Run() override {
    UploadCommon();
    UploadService("tests");
    ConfigureCluster(1, 2, NSaas::UrlHash, "rtyserver", "tests");

    Controller->SendCommandToDeployManager("modify_searchmap?ctype=sbtests&action=enable_indexing&slots_vector=localhost:10005&service=tests");
    // patch searchmap
    TString searchmap = Controller->SendCommandToDeployManager("process_storage?path=/common/sbtests/cluster.meta&download=yes&action=get");
    DEBUG_LOG << "Searchmap: " << searchmap << Endl;
    NSaas::TCluster cluster;
    cluster.Deserialize(searchmap);
    auto service = cluster.ServiceSearchMap("tests");
    if (!service)
        ythrow yexception() << "service tests not found in searchmap";
    service->SetServers("localhost:20100");
    service->Stream  = "localhost:20101";
    const TString& patchedSearchmap = cluster.Serialize();
    Controller->UploadDataToDeployManager(patchedSearchmap, "/common/sbtests/cluster.meta");

    NRTYDeploy::TDeployAction da1("tests", "sbtests", "NEW",
                                  NDaemonController::apStartAndWait, RTYSERVER_SERVICE);
    Controller->ExecuteActionOnDeployManager(da1);

    TJsonPtr serverInfo(Controller->GetServerInfo());
    TSet<NSearchMapParser::TShardsInterval> intervals;
    for (const NJson::TJsonValue& info : serverInfo->GetArray()) {
        auto resultDistributor = NRealTime::ParseDistributorString(info["config"]["Server"][0]["ModulesConfig"][0]["DOCFETCHER"][0]["Stream"][0]["DistributorServers"].GetString()).at(0);
        auto expectedDistributor = NRealTime::ParseDistributorString("localhost:20100").at(0);
        if (resultDistributor.Host != expectedDistributor.Host)
            ythrow yexception() << "invalid deploy with distributor";

        if (info["config"]["Server"][0]["ModulesConfig"][0]["DOCFETCHER"][0]["Stream"][0]["DistributorStream"].GetString() != "localhost:20101")
            ythrow yexception() << "invalid deploy with stream";
        NSearchMapParser::TShardIndex shardMin = FromString<NSearchMapParser::TShardIndex>(info["config"]["Server"][0]["ModulesConfig"][0]["DOCFETCHER"][0]["Stream"][0]["ShardMin"].GetString());
        NSearchMapParser::TShardIndex shardMax = FromString<NSearchMapParser::TShardIndex>(info["config"]["Server"][0]["ModulesConfig"][0]["DOCFETCHER"][0]["Stream"][0]["ShardMax"].GetString());
        NSearchMapParser::TShardsInterval interval(shardMin, shardMax);
        DEBUG_LOG << shardMin << " " << shardMax << Endl;
        if (!intervals.insert(interval).second)
            ythrow yexception() << "invalid deploy with shards: interval " << interval.ToString() << " set twice";
    }
    for (TSet<NSearchMapParser::TShardsInterval>::const_iterator i = intervals.begin(); i != intervals.end(); ++i) {
        TSet<NSearchMapParser::TShardsInterval>::const_iterator j = i;
        ++j;
        if (j != intervals.end() && !i->FollowedBy(*j))
            ythrow yexception() << "invalid deploy with shards: intervals has garps";
    }
    if (intervals.begin()->GetMin() != 0)
        ythrow yexception() << "invalid deploy with ShardMin";
    if (intervals.rbegin()->GetMax() != NSearchMapParser::SearchMapShards)
        ythrow yexception() << "invalid deploy with ShardMax";
    return true;
}
};

START_TEST_DEFINE_PARENT(TestDeployManagerMetaSearchMap, TestDeployManager)
bool Run() override {
    UploadCommon();
    UploadService("tests");
    ConfigureCluster(1, 2, NSaas::UrlHash, "rtyserver", "tests");
    {
        TDebugLogTimer timer("Add replica");
        NRTYCluster::TSlotsAllocator sa;
        sa.AllowSameHostReplica = true;
        NRTYDeploy::TAddIntSearchAction addInt("tests", "sbtests", NDaemonController::apStartAndWait, 1, 1, sa);
        Controller->ExecuteActionOnDeployManager(addInt);
    }
    TString intSlot;
    TString clusterText = Controller->SendCommandToDeployManager("process_storage?root=/&path=/common/sbtests/cluster.meta&action=get&download=yes");
    DEBUG_LOG << "Cluster: \n" << clusterText << Endl;
    NSaas::TClusterConst cluster;
    cluster.Deserialize(clusterText);
    //Searchproxy
    {
        NSearchMapParser::TSearchMap jsmp(cluster.SPSearchMap());
        DEBUG_LOG << "Searchmap for Searchproxy: \n" << jsmp.SerializeToJson() << Endl;
        if (jsmp.GetSlots().size() != 1) {
            ythrow yexception() << "Node count for searchproxy is " << jsmp.GetSlots().size() << " expected 1";
        }
        intSlot = jsmp.GetSlots()[0].GetSlotName();
    }
    //Indexerproxy
    {
        NSearchMapParser::TSearchMap jsmp(cluster.IPSearchMap());

        DEBUG_LOG << "Searchmap for Indexerproxy: \n" << jsmp.SerializeToJson() << Endl;
        if (jsmp.GetSlots().size() != 2) {
            ythrow yexception() << "Node count for indexerproxy is " << jsmp.GetSlots().size() << " expected 2";
        }
    }
    //intsearch
    DEBUG_LOG << "IntSlot: " << intSlot << Endl;
    {
        NSearchMapParser::TSearchMap jsmp(cluster.IntMetaSearchMap(intSlot));

        DEBUG_LOG << "Searchmap for intsearch: \n" << jsmp.SerializeToJson() << Endl;
        if (jsmp.GetSlots().size() != 2) {
            ythrow yexception() << "Node count for intsearch is " << jsmp.GetSlots().size() << " expected 2";
        }
    }
    return true;
}

};

SERVICE_TEST_RTYSERVER_DEFINE_PARENT(TestDeployManagerFetchErrorBase, TestDeployManager)
protected:
NSearchMapParser::TSlotsPool SearchSlots;
NSearchMapParser::TSlotsPool FetchSlots;

bool InitConfig() override {
    if (!TestDeployManager::InitConfig())
        return false;
    SetIndexerParams(ALL, 10);
    (*ConfigDiff)["Searcher.EnableUrlHash"] = true;
    (*ConfigDiff)["Searcher.FiltrationModel"] = "SIMPLE";
    return true;
}

void InitCluster() override {
    UploadCommon();
    UploadService("tests");
    UploadService("tests", 1);
    SearchSlots = ConfigureCluster(1, 1, NSaas::UrlHash, "rtyserver", "tests");
    NRTYCluster::TSlotsAllocator sa;
    sa.AllowSameHostReplica = true;
    NDaemonController::TAddReplicaAction replica(1, "tests", "sbtests", 1, sa, NDaemonController::apStartAndWait, RTYSERVER_SERVICE, true);
    Controller->ExecuteActionOnDeployManager(replica);
    for (auto&& i : SearchSlots.GetSlots()) {
        DEBUG_LOG << "sss: " << i.Serialize().GetStringRobust() << Endl;
    }
    FetchSlots = replica.GetPool();
    for (auto&& i : FetchSlots.GetSlots()) {
        DEBUG_LOG << "fff: " << i.Serialize().GetStringRobust() << Endl;
    }
    NDaemonController::TSimpleSearchmapModifAction dsf(FetchSlots, "sbtests", "tests", "disable_search_filtration", RTYSERVER_SERVICE, false);
    NDaemonController::TSimpleSearchmapModifAction df(SearchSlots, "sbtests", "tests", "disable_fetch", RTYSERVER_SERVICE, false);
    NDaemonController::TSimpleSearchmapModifAction di(FetchSlots, "sbtests", "tests", "disable_indexing", RTYSERVER_SERVICE, true);
    Controller->ExecuteActionOnDeployManager(dsf);
    Controller->ExecuteActionOnDeployManager(df);
    Controller->ExecuteActionOnDeployManager(di);
}

virtual bool DoRun(ui32 fetchCode) {
    i64 countMessages = 30;
    TVector<NRTYServer::TMessage> messages;
    GenerateInput(messages, countMessages, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed());
    for (auto& msg : messages) {
        if (GetIsPrefixed())
            msg.MutableDocument()->SetKeyPrefix(GetIsPrefixed());
    }
    {
        TDebugLogTimer timer("Index messages search replica");
        try {
            IndexMessages(messages, DISK, 1, 0);
        } catch (...) {
        }
    }
    NDaemonController::TSimpleSearchmapModifAction ei(FetchSlots, "sbtests", "tests", "enable_indexing", RTYSERVER_SERVICE, true);
    Controller->ExecuteActionOnDeployManager(ei);
    ReopenIndexers();
    DeploySp();
    TString reply;
    for (ui32 i = 0; i < messages.size(); ++i) {
        const ui32 notFetchedStatus = ProcessQuery("/?text=" + messages[i].GetDocument().GetBody() +  "&kps=" + ToString(GetIsPrefixed()), &reply);
        if (notFetchedStatus != fetchCode) {
            ythrow yexception() << "incorrect HTTP code for NotFetched: " << notFetchedStatus;
        }
    }
    return true;
}
};

START_TEST_DEFINE_PARENT(TestDeployManagerFetchError206, TestDeployManagerFetchErrorBase)

void InitCluster() override {
    UploadCommon();
    UploadService("tests");
    UploadService("tests", 13);
    SearchSlots = ConfigureCluster(1, 1, NSaas::UrlHash, "rtyserver", "tests");
    NRTYCluster::TSlotsAllocator sa;
    sa.AllowSameHostReplica = true;
    NDaemonController::TAddReplicaAction replica(1, "tests", "sbtests", 1, sa, NDaemonController::apStartAndWait, RTYSERVER_SERVICE, true);
    Controller->ExecuteActionOnDeployManager(replica);
    for (auto&& i : SearchSlots.GetSlots()) {
        DEBUG_LOG << "sss: " << i.Serialize().GetStringRobust() << Endl;
    }
    FetchSlots = replica.GetPool();
    for (auto&& i : FetchSlots.GetSlots()) {
        DEBUG_LOG << "fff: " << i.Serialize().GetStringRobust() << Endl;
    }
    NDaemonController::TSimpleSearchmapModifAction dsf(FetchSlots, "sbtests", "tests", "disable_search_filtration", RTYSERVER_SERVICE, false);
    NDaemonController::TSimpleSearchmapModifAction df(SearchSlots, "sbtests", "tests", "disable_fetch", RTYSERVER_SERVICE, false);
    NDaemonController::TSimpleSearchmapModifAction di(FetchSlots, "sbtests", "tests", "disable_indexing", RTYSERVER_SERVICE, true);
    Controller->ExecuteActionOnDeployManager(dsf);
    Controller->ExecuteActionOnDeployManager(df);
    Controller->ExecuteActionOnDeployManager(di);
}

bool Run() override {
    return DoRun(206);
}
};

START_TEST_DEFINE_PARENT(TestDeployManagerFetchError200, TestDeployManagerFetchErrorBase)
bool Run() override {
    return DoRun(200);
}
};

START_TEST_DEFINE_PARENT(TestDeployManagerFetchReplica, TestDeployManager)

NSearchMapParser::TSlotsPool SearchSlots;
NSearchMapParser::TSlotsPool FetchSlots;

bool InitConfig() override {
    if (!TestDeployManager::InitConfig())
        return false;
    SetIndexerParams(ALL, 10);
    (*ConfigDiff)["Searcher.EnableUrlHash"] = true;
    (*ConfigDiff)["Searcher.FiltrationModel"] = "SIMPLE";
    (*ConfigDiff)["Searcher.FactorsInfo"] = GetResourcesDirectory() + "/text_relev/text_factors.cfg";
    return true;
}

void InitCluster() override {
    UploadCommon();
    UploadService("search_with_factors");
    UploadService("search_with_factors", 1);
    SearchSlots = ConfigureCluster(1, 1, NSaas::UrlHash, "rtyserver", "search_with_factors");
    NRTYCluster::TSlotsAllocator sa;
    sa.AllowSameHostReplica = true;
    NDaemonController::TAddReplicaAction replica(1, "search_with_factors", "sbtests", 1, sa, NDaemonController::apStartAndWait, RTYSERVER_SERVICE, true);
    Controller->ExecuteActionOnDeployManager(replica);
    for (auto&& i : SearchSlots.GetSlots()) {
        DEBUG_LOG << "sss: " << i.Serialize().GetStringRobust() << Endl;
    }
    FetchSlots = replica.GetPool();
    for (auto&& i : FetchSlots.GetSlots()) {
        DEBUG_LOG << "fff: " << i.Serialize().GetStringRobust() << Endl;
    }
    NDaemonController::TSimpleSearchmapModifAction dsf(FetchSlots, "sbtests", "search_with_factors", "disable_search_filtration", RTYSERVER_SERVICE, false);
    NDaemonController::TSimpleSearchmapModifAction df(SearchSlots, "sbtests", "search_with_factors", "disable_fetch", RTYSERVER_SERVICE, false);
    NDaemonController::TSimpleSearchmapModifAction di(FetchSlots, "sbtests", "search_with_factors", "disable_indexing", RTYSERVER_SERVICE, true);
    Controller->ExecuteActionOnDeployManager(dsf);
    Controller->ExecuteActionOnDeployManager(df);
    Controller->ExecuteActionOnDeployManager(di);
}

bool Run() override {
    i64 countMessages = 30;
    TVector<NRTYServer::TMessage> messages;
    TSet<std::pair<ui64, TString> > deleted;
    GenerateInput(messages, countMessages, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed());
    for (auto& msg : messages) {
        NSaas::AddSimpleFactor("stat1", "2.5", *msg.MutableDocument()->MutableFactors());
        NSaas::AddSimpleFactor("stat2", "4.3", *msg.MutableDocument()->MutableFactors());
        NSaas::AddSimpleFactor("stat3", "6.1", *msg.MutableDocument()->MutableFactors());
    }
    TVector<NRTYServer::TMessage> reverseMessages(messages.rbegin(), messages.rend());
    {
        TDebugLogTimer timer("Index messages search replica");
        try {
            IndexMessages(messages, DISK, 1, 0, true, true, TDuration(), TDuration(), 1, "search_with_factors");
            CHECK_TEST_TRUE(false);
        } catch (...) {
        }
    }
    {
        TDebugLogTimer timer("modify searchmap");
        NDaemonController::TSimpleSearchmapModifAction ei(FetchSlots, "sbtests", "search_with_factors", "enable_indexing", RTYSERVER_SERVICE, true);
        NDaemonController::TSimpleSearchmapModifAction di(SearchSlots, "sbtests", "search_with_factors", "disable_indexing", RTYSERVER_SERVICE, true);
        Controller->ExecuteActionOnDeployManager(ei);

        ReopenIndexers("search_with_factors");

        Controller->ExecuteActionOnDeployManager(di);
        DeployIp();
    }
    {
        TDebugLogTimer timer("Index messages fetch replica");
        try {
            IndexMessages(reverseMessages, DISK, 1, 0, true, true, TDuration(), TDuration(), 1, "search_with_factors");
            CHECK_TEST_TRUE(false);
        } catch (...) {
        }
        NDaemonController::TSimpleSearchmapModifAction ei(SearchSlots, "sbtests", "search_with_factors", "enable_indexing", RTYSERVER_SERVICE, true);
        Controller->ExecuteActionOnDeployManager(ei);
        ReopenIndexers("search_with_factors");
    }
    DeploySp();
    CheckSearchResults(messages, TSearchMessagesContext::BuildDefault("search_with_factors"));
    return true;
}
};

START_TEST_DEFINE_PARENT(TestDeployManagerMetaService, TestDeployManager)
void InitCluster() override {
    UploadCommon();
    UploadService("tests");

    ConfigureCluster(1, 1, NSaas::UrlHash, "rtyserver", "tests");
    Controller->SetActiveBackends(TBackendProxy::TBackendSet());
    NSearchMapParser::TMetaService ms;
    ms.Name = "meta-service";
    ms.Components.push_back(NSearchMapParser::TMetaComponent("tests"));
    NDaemonController::TSimpleSearchmapModifAction addService(TVector<TString>(), "sbtests", ms.Name, "add_service", "metaservice");
    NDaemonController::TSimpleSearchmapModifAction editService(ms.SerializeToJson().GetStringRobust(), "sbtests", ms.Name, "metaservice");
    Controller->ExecuteActionOnDeployManager(addService);
    Controller->ExecuteActionOnDeployManager(editService);
    DeployIp();
    DeploySp();
}
bool Run() override {
    ui64 countMessages = 30;
    TVector<NRTYServer::TMessage> messages;
    TSet<std::pair<ui64, TString> > deleted;
    GenerateInput(messages, countMessages, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed());
    ui64 kps = messages[0].GetDocument().GetKeyPrefix();
    for (auto& msg : messages)
        msg.MutableDocument()->SetKeyPrefix(kps);
    IndexMessages(messages, REALTIME, 1);
    TVector<TDocSearchInfo> results;
    QuerySearch("body&service=meta-service&kps=" + ToString(kps), results);
    if (results.size() != countMessages)
        ythrow yexception() << "meta-service: invalid results count: " << results.size() << " != " << countMessages;
    QuerySearch("body&service=tests&kps=" + ToString(kps), results);
    if (results.size() != countMessages)
        ythrow yexception() << "tests: invalid results count: " << results.size() << " != " << countMessages;
    return true;
}
};

START_TEST_DEFINE_PARENT(TestSearchProxyMetaServiceRedirect, TestDeployManager)
void InitCluster() override {
    UploadCommon();
    UploadService("tests");
    UploadService("tests", 3);

    ConfigureCluster(1, 1, NSaas::UrlHash, "rtyserver", "tests");
    Controller->SetActiveBackends(TBackendProxy::TBackendSet());
    NSearchMapParser::TMetaService ms;
    ms.Name = "meta-service";
    ms.Components.push_back(NSearchMapParser::TMetaComponent("tests"));
    NDaemonController::TSimpleSearchmapModifAction addService(TVector<TString>(), "sbtests", ms.Name, "add_service", "metaservice");
    NDaemonController::TSimpleSearchmapModifAction editService(ms.SerializeToJson().GetStringRobust(), "sbtests", ms.Name, "metaservice");
    Controller->ExecuteActionOnDeployManager(addService);
    Controller->ExecuteActionOnDeployManager(editService);
    DeployIp();
    DeploySp();
}
bool Run() override {
    ui64 countMessages = 10;
    TVector<NRTYServer::TMessage> messages;
    GenerateInput(messages, countMessages, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed());
    ui64 kps = messages[0].GetDocument().GetKeyPrefix();
    for (auto& msg : messages)
        msg.MutableDocument()->SetKeyPrefix(kps);
    IndexMessages(messages, REALTIME, 1);

    TVector<TDocSearchInfo> results;
    QuerySearch("body&service=meta-service&kps=" + ToString(kps), results);
    if (results[0].GetUrl() != messages[0].GetDocument().GetUrl()) {
        ythrow yexception() << "incorrect url " << results[0].GetUrl();
    }

    QuerySearch("body&service=tests&kps=" + ToString(kps), results);
    if (results[0].GetUrl() == messages[0].GetDocument().GetUrl()) {
        ythrow yexception() << "incorrect url " << results[0].GetUrl();
    }

    UploadService("meta-service", 1);
    DeploySp();

    QuerySearch("body&service=meta-service&kps=" + ToString(kps), results);
    if (results[0].GetUrl() == messages[0].GetDocument().GetUrl()) {
        ythrow yexception() << "incorrect url " << results[0].GetUrl();
    }
    return true;
}
};

START_TEST_DEFINE_PARENT(TestSearchProxyMetaServiceQtree, TTestSearchProxyMetaServiceRedirectCaseClass)
bool Run() override {
    ui64 countMessages = 10;
    TVector<NRTYServer::TMessage> messages;
    GenerateInput(messages, countMessages, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed());
    ui64 kps = messages[0].GetDocument().GetKeyPrefix();
    for (auto& msg : messages)
        msg.MutableDocument()->SetKeyPrefix(kps);
    IndexMessages(messages, REALTIME, 1);

    const TString& qtree = "cHic4wrg4uFgEGCQ4FBg0GAyYBBiScpPqZRiUGLQYjBisOLhYAHKMQDlGAwYHBg8GAIYIhgSGDI-9F3awDGBkWEWI8MiRrCWTYwMexkZgMCAwYLRaTcjB6MAgxRYRolBg9GA0YMxYDITWC9jkRcXWEKAUYIRZK4FQxBjRxTT0nVJ3AKMUsyZqcVA-5iTOIEGMKpDmSxSjJVApnASF1CUSR2sIoOhgqnID9OsziimBZujGDfCjEuBmsEoxQgzmRlmHDfQOObMvHSYeVVMHEwZ7AXsDYwMQO8BAPvBMlw%2C";

    TVector<TDocSearchInfo> results;
    QuerySearch("fakerequest&service=meta-service&kps=" + ToString(kps) + "&qtree=" + qtree, results);
    if (results.size() != 10) {
        ythrow yexception() << "cannot find by qtree in metaservice";
    }
    return true;
}
};

START_TEST_DEFINE_PARENT(TestDeployManagerMetaServiceWithSuggest, TestDeployManager)
void InitCluster() override {
    UploadCommon();
    UploadService("search_with_factors");
    UploadService("suggest");
    ConfigureCluster(1, 1, NSaas::UrlHash, "rtyserver", "search_with_factors");
    ConfigureCluster(1, 1, NSaas::UrlHash, "rtyserver", "suggest");
    Controller->SetActiveBackends(TBackendProxy::TBackendSet());
    NSearchMapParser::TMetaService ms;
    ms.Name = "meta-service";
    ms.Components.push_back(NSearchMapParser::TMetaComponent("suggest", true, 0));
    ms.Components.push_back(NSearchMapParser::TMetaComponent("search_with_factors", false, 1));
    NDaemonController::TSimpleSearchmapModifAction addService(TVector<TString>(), "sbtests", ms.Name, "add_service", "metaservice");
    NDaemonController::TSimpleSearchmapModifAction editService(ms.SerializeToJson().GetStringRobust(), "sbtests", ms.Name, "metaservice");
    Controller->ExecuteActionOnDeployManager(addService);
    Controller->ExecuteActionOnDeployManager(editService);
    DeployIp();
    DeploySp();
}

bool Run() override {
    TVector<NRTYServer::TMessage> messages;
    TVector<NRTYServer::TMessage> update;
    TSet<std::pair<ui64, TString> > deleted;
    GenerateInput(messages, 10, NRTYServer::TMessage::MODIFY_DOCUMENT, GetIsPrefixed(), TAttrMap(), "some");
    GenerateInput(messages, 10, NRTYServer::TMessage::MODIFY_DOCUMENT, GetIsPrefixed(), TAttrMap(), "other");
    GenerateInput(messages, 10, NRTYServer::TMessage::MODIFY_DOCUMENT, GetIsPrefixed(), TAttrMap(), "test");
    GenerateInput(update, 30, NRTYServer::TMessage::DEPRECATED__UPDATE_DOCUMENT, GetIsPrefixed(), TAttrMap(), "aaa");
    ui64 kps = messages[0].GetDocument().GetKeyPrefix();
    for (ui32 msgId = 0; msgId < messages.size(); ++msgId) {
        auto& msg = messages[msgId];
        auto& upd = update[msgId];
        msg.MutableDocument()->SetKeyPrefix(kps);
        if (msgId % 2)
            upd.MutableDocument()->ClearBody();
        upd.MutableDocument()->SetKeyPrefix(kps);
        upd.MutableDocument()->SetUrl(msg.GetDocument().GetUrl());
        NSaas::AddSimpleFactor("stat1", "2.5", *msg.MutableDocument()->MutableFactors());
        NSaas::AddSimpleFactor("stat2", "4.3", *msg.MutableDocument()->MutableFactors());
        NSaas::AddSimpleFactor("stat3", "6.1", *msg.MutableDocument()->MutableFactors());
        NSaas::AddSimpleFactor("stat1", "2.1", *upd.MutableDocument()->MutableFactors());
        NSaas::AddSimpleFactor("stat2", "4.1", *upd.MutableDocument()->MutableFactors());
        NSaas::AddSimpleFactor("stat3", "6.2", *upd.MutableDocument()->MutableFactors());
    }
    {
        NSearchMapParser::TMetaService ms;
        ms.Name = "meta-service";
        ms.Components.push_back(NSearchMapParser::TMetaComponent("suggest", true, 0, false, true));
        ms.Components.push_back(NSearchMapParser::TMetaComponent("search_with_factors", false, 1));
        NDaemonController::TSimpleSearchmapModifAction editService(ms.SerializeToJson().GetStringRobust(), "sbtests", ms.Name, "metaservice");
        Controller->ExecuteActionOnDeployManager(editService);
        DeployIp();

        IndexMessages(messages, REALTIME, 1, 0, 0, true, TDuration(), TDuration(), 1, "meta-service");
    }

    UploadService("suggest", 1);
    DeployBe("suggest");
    IndexMessages(messages, REALTIME, 1, 0, 0, true, TDuration(), TDuration(), 1, "meta-service");

    ReopenIndexers("meta-service");
    TVector<TDocSearchInfo> results;
    TQuerySearchContext qsc;
    qsc.ResultCountRequirement = 1;
    qsc.AttemptionsCount = 10;
    QuerySearch("so&service=suggest&sgkps=" + ToString(kps), results, qsc);
    CHECK_TEST_EQ(results.size(), 1);
    CHECK_DSI_URL_RLV(results[0], "some", 100 << 23);

    qsc.ResultCountRequirement = 20;
    QuerySearch("some | other&numdoc=1000&service=meta-service&kps=" + ToString(kps), results, qsc);
    CHECK_TEST_EQ(results.size(), 20);


    {
        NSearchMapParser::TMetaService ms;
        ms.Name = "meta-service";
        ms.Components.push_back(NSearchMapParser::TMetaComponent("suggest", true, 0, false, true));
        ms.Components.push_back(NSearchMapParser::TMetaComponent("search_with_factors", false, 1));
        NDaemonController::TSimpleSearchmapModifAction editService(ms.SerializeToJson().GetStringRobust(), "sbtests", ms.Name, "metaservice");
        Controller->ExecuteActionOnDeployManager(editService);
        DeployIp();
    }

    IndexMessages(update, REALTIME, 1, 0, 0, true, TDuration(), TDuration(), 1, "meta-service");

    qsc.ResultCountRequirement = 25;
    QuerySearch("some | other | aaa&numdoc=1000&service=meta-service&kps=" + ToString(kps), results, qsc);
    CHECK_TEST_EQ(results.size(), 25);

    qsc.ResultCountRequirement = 1;
    QuerySearch("so&service=suggest&sgkps=" + ToString(kps), results, qsc);
    CHECK_TEST_EQ(results.size(), 1);
    CHECK_DSI_URL_RLV(results[0], "some", 100 << 23);

    return true;
}
};

START_TEST_DEFINE_PARENT(TestDeployManagerMetaServiceTwoComponents, TestDeployManager)
void InitCluster() override {
    UploadCommon();
    UploadService("tests");
    UploadService("tests2");
    ConfigureCluster(1, 1, NSaas::UrlHash, "rtyserver", "tests");
    ConfigureCluster(1, 1, NSaas::UrlHash, "rtyserver", "tests2");
    Controller->SetActiveBackends(TBackendProxy::TBackendSet());
    NSearchMapParser::TMetaService ms;
    ms.Name = "meta-service";
    ms.Components.push_back(NSearchMapParser::TMetaComponent("tests"));
    ms.Components.push_back(NSearchMapParser::TMetaComponent("tests2"));
    NDaemonController::TSimpleSearchmapModifAction addService(TVector<TString>(), "sbtests", ms.Name, "add_service", "metaservice");
    NDaemonController::TSimpleSearchmapModifAction editService(ms.SerializeToJson().GetStringRobust(), "sbtests", ms.Name, "metaservice");
    Controller->ExecuteActionOnDeployManager(addService);
    Controller->ExecuteActionOnDeployManager(editService);
    DeployIp();
    DeploySp();
}

inline void Check(ui64 kps, const TString& text, const TString& service, ui32 numdoc, ui32 count, const THashMap<TString, TString>& searchProps = THashMap<TString, TString>()) {
    TVector<TDocSearchInfo> results;
    TString query = text + "&service=" + service + "&kps=" + ToString(kps) + "&numdoc=" + ToString(numdoc);
    THashMultiMap<TString, TString> searchProperties;
    QuerySearch(query, results, nullptr, &searchProperties);
    if (results.size() != count)
        ythrow yexception() << "some-service: invalid results count by request '" << query << "': " << results.size() << " != " << count;
    for (const auto& prop : searchProps) {
        auto resProp = searchProperties.find(prop.first);
        if (resProp == searchProperties.end())
            ythrow yexception() << "some-service: cannot find search property " << prop.first << " by request '" << query << "'";
        if (resProp->second != prop.second)
            ythrow yexception() << "some-service: search property " << prop.first << " by request '" << query << "' has incorrect value: " << resProp->second << " != " << prop.second;
    }
}

bool Run() override {
    TVector<NRTYServer::TMessage> messages;
    TSet<std::pair<ui64, TString> > deleted;
    GenerateInput(messages, 10, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed(), TAttrMap(), "some");
    GenerateInput(messages, 10, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed(), TAttrMap(), "other");
    GenerateInput(messages, 10, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed(), TAttrMap(), "test");
    ui64 kps = messages[0].GetDocument().GetKeyPrefix();
    for (auto& msg : messages)
        msg.MutableDocument()->SetKeyPrefix(kps);
    IndexMessages(TVector<NRTYServer::TMessage>(messages.begin(), messages.begin() + 10), REALTIME, 1, 0, 0, true, TDuration(), TDuration(), 1, "tests");
    IndexMessages(TVector<NRTYServer::TMessage>(messages.begin() + 10, messages.begin() + 20), REALTIME, 1, 0, 0, true, TDuration(), TDuration(), 1, "tests2");
    IndexMessages(TVector<NRTYServer::TMessage>(messages.begin() + 20, messages.begin() + 30), REALTIME, 1, 0, 0, true, TDuration(), TDuration(), 1, "meta-service");
    TVector<TDocSearchInfo> results;
    THashMap<TString, TString> noTier1;
    noTier1["Unanswer_1_TIER"] = "0/0";
    Check(kps, "some", "tests", 100, 10);
    Check(kps, "other", "tests", 100, 0);
    Check(kps, "test", "tests", 100, 10);
    Check(kps, "some", "tests2", 100, 0);
    Check(kps, "other", "tests2", 100, 10);
    Check(kps, "test", "tests2", 100, 10);
    Check(kps, "some", "meta-service", 100, 10);
    Check(kps, "other", "meta-service", 100, 10);
    Check(kps, "test", "meta-service", 100, 20);
    Check(kps, "some | other", "meta-service", 100, 20);
    Check(kps, "some | other", "meta-service", 10, 10, noTier1);
    return true;
}
};

START_TEST_DEFINE_PARENT(TestDeployManagerMetaServiceTwoComponentsNoIndexing, TestDeployManager)
void InitCluster() override {
    UploadCommon();
    UploadService("tests");
    UploadService("tests2");
    ConfigureCluster(1, 1, NSaas::UrlHash, "rtyserver", "tests");
    ConfigureCluster(1, 1, NSaas::UrlHash, "rtyserver", "tests2");
    Controller->SetActiveBackends(TBackendProxy::TBackendSet());
    NSearchMapParser::TMetaService ms;
    ms.Name = "meta-service";
    ms.Components.push_back(NSearchMapParser::TMetaComponent("tests", false, 1, true));
    ms.Components.push_back(NSearchMapParser::TMetaComponent("tests2", false, 1, true));
    NDaemonController::TSimpleSearchmapModifAction addService(TVector<TString>(), "sbtests", ms.Name, "add_service", "metaservice");
    NDaemonController::TSimpleSearchmapModifAction editService(ms.SerializeToJson().GetStringRobust(), "sbtests", ms.Name, "metaservice");
    Controller->ExecuteActionOnDeployManager(addService);
    Controller->ExecuteActionOnDeployManager(editService);
    DeployIp();
    DeploySp();
}

bool Run() override {
    TVector<NRTYServer::TMessage> messages;
    TSet<std::pair<ui64, TString> > deleted;
    GenerateInput(messages, 10, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed(), TAttrMap(), "some");
    ui64 kps = messages[0].GetDocument().GetKeyPrefix();
    for (auto& msg : messages)
        msg.MutableDocument()->SetKeyPrefix(kps);
    IndexMessages(messages, REALTIME, 1, 0, 0, true, TDuration(), TDuration(), 1, "meta-service");
    TQuerySearchContext context;
    context.ResultCountRequirement = 0;
    TVector<TDocSearchInfo> results;
    QuerySearch("some&service=tests&kps=" + ToString(kps), results, context);
    CHECK_TEST_EQ(results.size(), 0);
    QuerySearch("some&service=meta-service&kps=" + ToString(kps), results, context);
    CHECK_TEST_EQ(results.size(), 0);

    NSearchMapParser::TMetaService ms;
    ms.Name = "meta-service";
    ms.Components.push_back(NSearchMapParser::TMetaComponent("tests", false, 1, false));
    ms.Components.push_back(NSearchMapParser::TMetaComponent("tests2", false, 1, false));
    NDaemonController::TSimpleSearchmapModifAction editService(ms.SerializeToJson().GetStringRobust(), "sbtests", ms.Name, "metaservice");
    Controller->ExecuteActionOnDeployManager(editService);
    DeployIp();
    DeploySp();

    IndexMessages(messages, REALTIME, 1, 0, 0, true, TDuration(), TDuration(), 1, "meta-service");
    context.ResultCountRequirement = 20;
    QuerySearch("some&service=tests&kps=" + ToString(kps), results, context);
    CHECK_TEST_EQ(results.size(), 10);
    QuerySearch("some&service=meta-service&kps=" + ToString(kps), results, context);
    CHECK_TEST_EQ(results.size(), 20);

    return true;
}
};

START_TEST_DEFINE_PARENT(TestDeployManagerMetaServiceTwoComponentsDiffSharding, TestDeployManager)
void InitCluster() override {
    UploadCommon();
    UploadService("tests");
    UploadService("tests2");
    ConfigureCluster(1, 2, NSaas::UrlHash, "rtyserver", "tests");
    ConfigureCluster(1, 2, NSaas::Broadcast, "rtyserver", "tests2");
    Controller->SetActiveBackends(TBackendProxy::TBackendSet());
    NSearchMapParser::TMetaService ms;
    ms.Name = "meta-service";
    ms.Components.push_back(NSearchMapParser::TMetaComponent("tests"));
    ms.Components.push_back(NSearchMapParser::TMetaComponent("tests2"));
    NDaemonController::TSimpleSearchmapModifAction addService(TVector<TString>(), "sbtests", ms.Name, "add_service", "metaservice");
    NDaemonController::TSimpleSearchmapModifAction editService(ms.SerializeToJson().GetStringRobust(), "sbtests", ms.Name, "metaservice");
    Controller->ExecuteActionOnDeployManager(addService);
    Controller->ExecuteActionOnDeployManager(editService);
    DeployIp();
    DeploySp();
}

bool Run() override {
    TVector<NRTYServer::TMessage> messages;
    TSet<std::pair<ui64, TString> > deleted;
    GenerateInput(messages, 10, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed());
    ui64 kps = messages[0].GetDocument().GetKeyPrefix();
    for (auto& msg : messages) {
        msg.MutableDocument()->SetKeyPrefix(kps);
    }
    IndexMessages(messages, REALTIME, 1, 0, 0, true, TDuration(), TDuration(), 1, "meta-service");
    TVector<TDocSearchInfo> results;
    TSearchMessagesContext smc = TSearchMessagesContext::BuildDefault("tests");
    TSearchMessagesContext smc2 = TSearchMessagesContext::BuildDefault("tests2");
    smc2.CountResults.clear();
    smc2.CountResults.insert(2);

    CheckSearchResults(messages, smc);
    CheckSearchResults(messages, smc2);

    ReopenIndexers("meta-service");

    CheckSearchResults(messages, smc);
    CheckSearchResults(messages, smc2);

    return true;
}
};

START_TEST_DEFINE_PARENT(TestDeployManagerRemoveOneOfTwoServices, TestDeployManager)
void InitCluster() override {
    UploadCommon();
    UploadService("tests");
    UploadService("tests2");
    ConfigureCluster(1, 1, NSaas::UrlHash, "rtyserver", "tests");
    ConfigureCluster(1, 1, NSaas::UrlHash, "rtyserver", "tests2");
    Controller->SetActiveBackends(TBackendProxy::TBackendSet());
    DeployIp();
    DeploySp();
}

bool Run() override {
    TVector<NRTYServer::TMessage> messages;
    TSet<std::pair<ui64, TString> > deleted;
    GenerateInput(messages, 10, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed(), TAttrMap(), "some");
    ui64 kps = messages[0].GetDocument().GetKeyPrefix();
    for (auto& msg : messages)
        msg.MutableDocument()->SetKeyPrefix(kps);
    IndexMessages(TVector<NRTYServer::TMessage>(messages.begin(), messages.begin() + 10), REALTIME, 1, 0, 0, true, TDuration(), TDuration(), 1, "tests");

    TVector<TString> slots;
    NDaemonController::TSimpleSearchmapModifAction deleteService(slots, "sbtests", "tests2", "remove_service", "rtyserver");
    Controller->ExecuteActionOnDeployManager(deleteService);
    DeploySp();

    TVector<TDocSearchInfo> results;
    QuerySearch("some&service=tests&kps=" + ToString(kps), results);

    if (results.empty())
        throw yexception() << "no results";

    CHECK_TEST_EQ(400, QuerySearch("some&service=tests2&kps=" + ToString(kps), results));

    return true;
}
};

START_TEST_DEFINE_PARENT(TestDeployManagerStringSort, TestDeployManager)
void InitCluster() override {
    UploadCommon();
    UploadService("stringsort");
    ConfigureCluster(1, 2, NSaas::UrlHash, "rtyserver", "stringsort");
    DeployIp();
    DeploySp();
}

void Test(TIndexerType indexer) {
    if (GetIsPrefixed())
        return;
    ui32 docsCount = 60;
    TStandartDocumentGenerator* sdg = new TStandartDocumentGenerator(GetIsPrefixed());
    TString textForSearch = "disk";
    if (indexer == REALTIME)
        textForSearch = "realtime";
    sdg->SetTextConstant(textForSearch);
    TStandartAttributesFiller* saf = new TStandartAttributesFiller();
    saf->SetDocumentsCount(docsCount);
    for (ui32 i = 0; i < docsCount; i++) {
        TString attr(ToString(i));
        attr = TString(10 - attr.size(), '0') + attr;
        saf->AddDocAttribute(i, "sortprop", attr, TStandartAttributesFiller::atProp);
    }
    sdg->RegisterFiller("gr", saf);
    TStandartMessagesGenerator smg(sdg, SendIndexReply);
    TVector<NRTYServer::TMessage> messages;
    GenerateInput(messages, docsCount, smg);
    IndexMessages(TVector<NRTYServer::TMessage>(messages.begin(), messages.begin() + docsCount / 2), indexer, 1, 0, 0, true, TDuration(), TDuration(), 1, "stringsort");
    ReopenIndexers("stringsort");
    IndexMessages(TVector<NRTYServer::TMessage>(messages.begin() + docsCount / 2, messages.end()), indexer, 1, 0, 0, true, TDuration(), TDuration(), 1, "stringsort");
    ReopenIndexers("stringsort");
    TVector<TDocSearchInfo> results;
    TQuerySearchContext searchContext;
    searchContext.PrintResult = true;
    TQuerySearchContext::TDocProperties props;
    searchContext.DocProperties = &props;
    QuerySearch(textForSearch + "&service=stringsort&how=gta_sortprop&numdoc=10", results, searchContext);
    TString currentTextRelevance = "9";
    for (int i = 0; i < results.ysize(); ++i) {
        TString textRelev = props[i]->find("sortprop")->second;
        if (!textRelev || currentTextRelevance < textRelev)
            ythrow yexception() << "Incorrect textRelev sequence " << textRelev;
        currentTextRelevance = textRelev;
    }
    if (FromString<ui32>(currentTextRelevance) != docsCount - 10)
        ythrow yexception() << "invalid last element " << currentTextRelevance;
    QuerySearch(textForSearch + "&service=stringsort&g=0..10.1.-1.0.0.-1.gta_sortprop.1.", results, searchContext);
    currentTextRelevance = "";
    for (int i = 0; i < results.ysize(); ++i) {
        TString textRelev = props[i]->find("sortprop")->second;
        if (!textRelev || currentTextRelevance > textRelev)
            ythrow yexception() << "Incorrect textRelev sequence " << textRelev;
        currentTextRelevance = textRelev;
    }
    if (FromString<ui32>(currentTextRelevance) != 9)
        ythrow yexception() << "invalid last element " << currentTextRelevance;
}

bool Run() override {
    Test(REALTIME);
    Test(DISK);
    return true;
}
};

class TSlaConfig: TSectionParser<TAnyYandexConfig> {
public:
    struct TCtype {
        ui32 AbcUserService{};
    };

    struct TSla {
        THashMap<TString, TCtype> Ctypes;
    };

    TSlaConfig(const TString& text)
        : TParser(text.c_str(), "Sla")
    {
        const auto& sections = ServerSection->GetAllChildren();

        auto [it, upper] = sections.equal_range("ctype");
        for (; it != upper; ++it) {
            const auto& ctypeSect = it->second;
            const auto& directives = ctypeSect->GetDirectives();

            TString ctypeName;
            directives.GetValue("Ctype", ctypeName);

            const ui32 abcId = directives.Value("AbcUserService", 0);

            Result.Ctypes[ctypeName] = {abcId};
        }
    }

    const TSla& GetResult() const {
        return Result;
    }

private:
    TSla Result;
};

START_TEST_DEFINE_PARENT(TestDeployManagerSlaDescription, TestDeployManager)
    void InitCluster() override {
        UploadCommon();
        UploadService("tests");
        Controller->UploadFileToDeployManager(GetRunDir() + "/copy/sla_description-for-yconf.conf", "/configs/tests/sla_description.conf");
        Controller->UploadFileToDeployManager(GetResourcesDirectory() + "/per_dc_srch.tags.info", "/common/sbtests/tags.info");
        Controller->UploadFileToDeployManager(GetResourcesDirectory() + "/per_dc_srch.cluster.meta", "/common/sbtests/cluster.meta");
        Controller->SendCommandToDeployManager("?command=restart", 3);
    }

    bool Check(const TString& slot) {
        NJson::TJsonValue list;
        {
            TString listStr = Controller->SendCommandToDeployManager("list_conf?last_versions=true&slot=" + slot);
            TStringInput si(listStr);
            CHECK_TEST_TRUE(NJson::ReadJsonTree(&si, &list));
        }

        TString sla_description;
        for (const auto& file : list["files"].GetArray()) {
            TString filename = file["rename"].GetString();
            if (filename == "sla_description-tests.conf") {
                TString url = file["url"].GetString();
                i32 version = file["version"].GetInteger();
                sla_description = Controller->SendCommandToDeployManager("get_conf?root=/&filename=" + url + "&version=" + ToString(version));
                break;
            }
        }
        CHECK_TEST_TRUE(sla_description);

        TSlaConfig conf(sla_description);
        CHECK_TEST_EQ(conf.GetResult().Ctypes.FindPtr("stable")->AbcUserService, 1423);
        CHECK_TEST_EQ(conf.GetResult().Ctypes.FindPtr("prestable")->AbcUserService, 142);
        CHECK_TEST_TRUE(conf.GetResult().Ctypes.FindPtr("testing") == nullptr);

        return true;
    }

    bool Run() override {
        CHECK_TEST_TRUE(Check("dc1__sp1:80"));
        CHECK_TEST_TRUE(Check("dc1__host1:80"));
        return true;
    }
};


SERVICE_TEST_RTYSERVER_DEFINE_PARENT(TestDeployManagerPerDCSearchCore, TestDeployManager)

bool Check(const TString& slot, const TString& backends, const TString& location) {
    NJson::TJsonValue list;
    {
        TString listStr = Controller->SendCommandToDeployManager("list_conf?last_versions=true&slot=" + slot);
        TStringInput si(listStr);
        CHECK_TEST_TRUE(NJson::ReadJsonTree(&si, &list));
    }

    TString searchmap;
    for (const auto& file : list["files"].GetArray()) {
        TString filename = file["rename"].GetString();
        if (filename != "searchmap.json")
            continue;

        TString url = file["url"].GetString();
        i32 version = file["version"].GetInteger();
        searchmap = Controller->SendCommandToDeployManager("get_conf?root=/&filename=" + url + "&version=" + ToString(version));
    }

    CHECK_TEST_TRUE(searchmap);
    TStringInput si(searchmap);
    NSearchMapParser::TJsonSearchMapParser jsmp(si);
    TVector<NSearchMapParser::TSearchMapHost> hosts = jsmp.GetSearchMap().GetSlots();

    TSet<TString> be;
    for (const auto& s : SplitString(backends, ","))
        be.insert(s);

    for (const auto& host : hosts) {
        CHECK_TEST_EQ(host.Group, !!location ? location + "-1" : "");
        CHECK_TEST_TRUE(be.contains(host.GetSlotName()));
        be.erase(host.GetSlotName());
    }

    if (!be.empty()) {
        TStringStream ss;
        ss << "three is no slots ";
        for (const auto& s : be)
            ss << ", " << s;
        ss << " in sm for " << slot;
        ythrow yexception() << ss.Str();
    }
    return true;
}

bool Run() override {
    CHECK_TEST_TRUE(Check("dc1__int1:80", "dc1__host1:80,dc1__host2:80", "location1"));
    CHECK_TEST_TRUE(Check("dc2__int2:80", "dc2__host3:80,dc2__host4:80", "location2"));
    CHECK_TEST_TRUE(Check("dc1__sp1:80", "dc1__int1:80", "location1"));
    CHECK_TEST_TRUE(Check("dc2__sp2:80", "dc2__int2:80", "location2"));
    CHECK_TEST_TRUE(Check("dc3__sp3:80", "dc1__int1:80,dc2__int2:80", ""));
    return true;
}
};

START_TEST_DEFINE_PARENT(TestDeployManagerPerDcSearch, TestDeployManagerPerDCSearchCore)
    void InitCluster() override {
        UploadCommon();
        UploadService("tests");
        Controller->UploadFileToDeployManager(GetResourcesDirectory() + "/per_dc_srch.tags.info", "/common/sbtests/tags.info");
        Controller->UploadFileToDeployManager(GetResourcesDirectory() + "/per_dc_srch.cluster.meta", "/common/sbtests/cluster.meta");
        Controller->SendCommandToDeployManager("?command=restart", 3);
    }
};

START_TEST_DEFINE_PARENT(TestDeployManagerPerDcSearchTagsInServices, TestDeployManagerPerDCSearchCore)
    void InitCluster() override {
        UploadCommon();
        UploadService("tests");
        Controller->UploadFileToDeployManager(GetResourcesDirectory() + "/per_dc_srch_tests.tags.info", "/configs/tests/tags.info");
        Controller->UploadFileToDeployManager(GetResourcesDirectory() + "/per_dc_srch_others.tags.info", "/common/sbtests/tags.info");
        Controller->UploadFileToDeployManager(GetResourcesDirectory() + "/per_dc_srch.cluster.meta", "/common/sbtests/cluster.meta");
        Controller->SendCommandToDeployManager("?command=restart", 3);
    }
};

START_TEST_DEFINE_PARENT(TestDeployManagerAllocatingSlots, TestDeployManager)
void InitCluster() override {
    UploadCommon();
    UploadService("tests");
    Controller->UploadFileToDeployManager(GetResourcesDirectory() + "/per_dc_srch.tags.info", "/common/sbtests/tags.info");
    NDaemonController::TRestartAction ra(NDaemonController::apStartAndWait);
    NDaemonController::TControllerAgent agent(Controller->GetConfig().DeployManager.Host, Controller->GetConfig().DeployManager.Port + 3);
    if(!agent.ExecuteAction(ra))
        ythrow yexception() << "cannot restart deploy_manager";
}

struct TReplicaInfo {
    TSet<TString> Hosts;
    TVector<NSearchMapParser::TShardsInterval> Intervals;
};

bool Run() override {
    NRTYCluster::TSlotsAllocator sa;
    sa.AllowSameHostReplica = true;
    sa.MultiDC = false;
    sa.PrefferedDCOnly = true;
    sa.PrefDCs.push_back("dc1");
    sa.PrefDCs.push_back("dc2");
    sa.OneServerOnSlot = true;
    NDaemonController::TSimpleSearchmapModifAction addService(TVector<TString>(), "sbtests", "tests", "add_service", "rtyserver");
    Controller->ExecuteActionOnDeployManager(addService);
    try {
        NDaemonController::TAddReplicaAction addReplicaAction(4, "tests", "sbtests", 1, sa, NDaemonController::apStartAndWait, "rtyserver", false, true);
        Controller->ExecuteActionOnDeployManager(addReplicaAction);
        TEST_FAILED("no error!");
    } catch (...) {
    }
    try {
        NDaemonController::TAddReplicaAction addReplicaAction(0, "tests", "sbtests", 1, sa, NDaemonController::apStartAndWait, "rtyserver", false, true);
        Controller->ExecuteActionOnDeployManager(addReplicaAction);
        TEST_FAILED("no error!");
    } catch (...) {
    }
    {
        sa.OneServerOnSlot = false;
        NDaemonController::TAddReplicaAction addReplicaAction(3, "tests", "sbtests", 2, sa, NDaemonController::apStartAndWait, "rtyserver", false, true);
        Controller->ExecuteActionOnDeployManager(addReplicaAction);
        TVector<NSearchMapParser::TSearchMapHost> slots = addReplicaAction.GetPool().GetSlots();
        TMap<TString, TReplicaInfo> replics;
        for (const auto& slot : slots) {
            const TString dc = TDatacenterUtil::Instance().GetDatacenter(slot.Name);
            if (dc != "dc1" && dc != "dc2")
                ythrow yexception() << "wrong DC for" << slot.GetSlotName();
            TReplicaInfo& ri = replics[dc];
            if (!ri.Hosts.insert(slot.Name).second)
                ythrow yexception() << "two slots on one host " << slot.Name;
            ri.Intervals.push_back(slot.Shards);
        }
        CHECK_TEST_EQ(replics.size(), 2);
        for (const auto& ri : replics) {
            TVector<NSearchMapParser::TShardsInterval> merged = NSearchMapParser::TShardsInterval::Merge(ri.second.Intervals);
            if (merged.size() != 1 || merged[0].GetMin() != 0 || merged[0].GetMax() != NSearchMapParser::SearchMapShards)
                ythrow yexception() << "invalid replica " << ri.first;
        }
    }
    {
        NRTYCluster::TSlotsAllocator saL;
        saL.AllowSameHostReplica = true;
        saL.MultiDC = false;
        saL.PrefferedDCOnly = true;
        saL.PrefDCs.push_back("dc1");
        saL.PrefDCs.push_back("dc2");
        saL.PrefDCs.push_back("dc3");
        saL.OneServerOnSlot = true;
        NDaemonController::TAddReplicaAction addReplicaAction(0, "tests", "sbtests", 1, saL, NDaemonController::apStartAndWait, "rtyserver", false, true);
        Controller->ExecuteActionOnDeployManager(addReplicaAction);
        for (auto&& i : addReplicaAction.GetPool().GetSlots()) {
            const TString dc = TDatacenterUtil::Instance().GetDatacenter(i.Name);
            if (dc != "dc3")
                ythrow yexception() << "wrong DC for " << i.GetSlotName();
        }
    }


    sa.MultiDC = true;
    sa.OneServerOnSlot = false;
    sa.PrefDCs.clear();
    sa.PrefDCs.push_back("dc2");
    sa.PrefDCs.push_back("dc1");
    NDaemonController::TAddReplicaAction addReplicaAction1(4, "tests", "sbtests", 1, sa, NDaemonController::apStartAndWait, "rtyserver", false, true);
    Controller->ExecuteActionOnDeployManager(addReplicaAction1);
    TMap<TString, ui32> dcs;
    for (auto&& i : addReplicaAction1.GetPool().GetSlots()) {
        const TString dc = TDatacenterUtil::Instance().GetDatacenter(i.Name);
        dcs[dc]++;
        if (dc != "dc1" && dc != "dc2")
            ythrow yexception() << "wrong DC for" << i.GetSlotName();
    }

    CHECK_TEST_EQ(dcs.size(), 2);
    CHECK_TEST_EQ(dcs["dc2"], 3);
    CHECK_TEST_EQ(dcs["dc1"], 1);

    sa.OneServerOnSlot = false;
    sa.PrefDCs.clear();
    sa.PrefDCs.push_back("dc4");
    sa.PrefferedDCOnly = true;
    NDaemonController::TAddReplicaAction addReplicaAction3(4, "tests", "sbtests", 1, sa, NDaemonController::apStartAndWait, "rtyserver", false, true);
    Controller->ExecuteActionOnDeployManager(addReplicaAction3);
    TMap<TString, ui32> hosts;
    for (auto&& i : addReplicaAction3.GetPool().GetSlots()) {
        const TString dc = TDatacenterUtil::Instance().GetDatacenter(i.Name);
        if (dc != "dc4")
            ythrow yexception() << "wrong DC for " << i.GetSlotName();
        ++hosts[i.GetShortHostName()];
    }

    CHECK_TEST_EQ(hosts.size(), 2);
    CHECK_TEST_EQ(hosts["dc4__big_host.domain.com"], 2);
    CHECK_TEST_EQ(hosts["dc4__small_host.domain.com"], 2);

    sa.MultiDC = false;
    sa.PrefferedDCOnly = false;
    sa.PrefDCs.clear();
    sa.PrefDCs.push_back("dc2");
    sa.PrefDCs.push_back("dc1");
    NDaemonController::TAddReplicaAction addReplicaAction2(3, "tests", "sbtests", 1, sa, NDaemonController::apStartAndWait, "rtyserver", false, true);
    Controller->ExecuteActionOnDeployManager(addReplicaAction2);

    for (auto&& i : addReplicaAction2.GetPool().GetSlots()) {
        const TString dc = TDatacenterUtil::Instance().GetDatacenter(i.Name);
        if (dc != "dc3")
            ythrow yexception() << "wrong DC for" << i.GetSlotName();
    }

    return true;
}
};

START_TEST_DEFINE_PARENT(TestDeployManagerAllocatingSlotsCustomSlots, TTestDeployManagerAllocatingSlotsCaseClass)
bool Run() override {
    NRTYCluster::TSlotsAllocator sa;
    sa.AllowSameHostReplica = true;
    sa.PrefferedDCOnly = true;
    sa.CustomSlots.insert(NRTYCluster::TSlotData("dc4__big_host.domain.com", 70));
    sa.CustomSlots.insert(NRTYCluster::TSlotData("dc4__big_host.domain.com", 100));
    sa.CustomSlots.insert(NRTYCluster::TSlotData("dc4__small_host.domain.com", 80));
    sa.CustomSlots.insert(NRTYCluster::TSlotData("dc4__small_host.domain.com", 100));
    NDaemonController::TSimpleSearchmapModifAction addService(TVector<TString>(), "sbtests", "tests", "add_service", "rtyserver");
    Controller->ExecuteActionOnDeployManager(addService);
    NDaemonController::TAddReplicaAction addReplicaAction(4, "tests", "sbtests", 1, sa, NDaemonController::apStartAndWait, "rtyserver", false, true);
    Controller->ExecuteActionOnDeployManager(addReplicaAction);
    TVector<NSearchMapParser::TSearchMapHost> slots = addReplicaAction.GetPool().GetSlots();
    CHECK_TEST_EQ(slots.size(), 4);
    for (auto&& i : slots) {
        CHECK_TEST_TRUE(sa.CustomSlots.contains(NRTYCluster::TSlotData(i.Name, i.SearchPort)));
    }
    return true;
}
};

struct TTagsInfoParams {
    ui32 DCs= 1;
    ui32 RTYServers= 2;
    ui32 RTYSlots= 2;
    ui32 SPServers = 2;
    ui32 IPServers = 2;
    ui32 IntSearchServers = 2;
};

#define GENERATE_SLOTS(hostPrefix, service, serversCount) {\
    NJson::TJsonValue slots(NJson::JSON_ARRAY); \
    for (ui64 dc = 0; dc < params.DCs; ++dc) { \
        for (ui64 server = 0; server < serversCount; ++server) { \
            TString slotName = "dc" + ToString(dc) + "__" + hostPrefix + ToString(server) + ":15000"; \
            slots.AppendValue(NJson::TJsonValue(slotName)); \
        } \
    } \
    TString path = TString(service) + ".slots"; \
    result.SetValueByPath(path, slots, '.'); \
}

static void GenerateTagsInfo(const TTagsInfoParams params, const TString& path) {
    NJson::TJsonValue result;

    {
        NJson::TJsonValue slots(NJson::JSON_ARRAY);
        for (ui64 dc = 0; dc < params.DCs; ++dc) {
            for (ui64 server = 0; server < params.RTYServers; ++server) {
                for (ui64 sl = 0; sl < params.RTYSlots; ++sl) {
                    TString slotName = "dc" + ToString(dc) + "__host" + ToString(server) + ":" + ToString(80 + 10*sl);
                    slots.AppendValue(NJson::TJsonValue(slotName));
                }
            }
        }
        result.SetValueByPath("rtyserver.slots", slots, '.');
    }
    GENERATE_SLOTS("sp", "searchproxy", params.SPServers);
    GENERATE_SLOTS("ip", "indexerproxy", params.IPServers);
    GENERATE_SLOTS("int", "intsearch", params.IntSearchServers);
    {
        NJson::TJsonValue slots;
        slots.AppendValue("localhost:80");
        result.SetValueByPath("deploy_manager.slots", slots, '.');
    }

    TFileOutput out(path);
    NJson::WriteJson(&out, &result, true);
}


START_TEST_DEFINE_PARENT(TestDeployManagerOnePerServerPolicy, TestDeployManager)
void InitCluster() override {
    TTagsInfoParams params;
    params.RTYServers = 4;
    params.RTYSlots = 3;
    GenerateTagsInfo(params, "tmp.tags.info");
    UploadCommon();
    UploadService("tests");
    Controller->UploadFileToDeployManager("tmp.tags.info", "/common/sbtests/tags.info");
    NDaemonController::TRestartAction ra(NDaemonController::apStartAndWait);
    NDaemonController::TControllerAgent agent(Controller->GetConfig().DeployManager.Host, Controller->GetConfig().DeployManager.Port + 3);
    if(!agent.ExecuteAction(ra))
        ythrow yexception() << "cannot restart deploy_manager";
}

bool Run() override {
    NRTYCluster::TSlotsAllocator sa;
    sa.AllowSameHostReplica = true;
    sa.MultiDC = false;
    sa.PrefferedDCOnly = true;
    sa.PrefDCs.push_back("dc0");
    sa.OneServerOnSlot = true;
    {
        NDaemonController::TSimpleSearchmapModifAction addService(TVector<TString>(), "sbtests", "tests3", "add_service", "rtyserver");
        Controller->ExecuteActionOnDeployManager(addService);
        NDaemonController::TAddReplicaAction addReplicaAction(3, "tests3", "sbtests", 1, sa, NDaemonController::apStartAndWait, "rtyserver", false, true);
        Controller->ExecuteActionOnDeployManager(addReplicaAction);

        TVector<NSearchMapParser::TSearchMapHost> slots = addReplicaAction.GetPool().GetSlots();
        TSet<TString> hosts;
        for (const auto& slot : slots) {
            hosts.insert(slot.Name);
        }
        CHECK_TEST_TRUE(hosts.size() == 3);
    }
    {
        NDaemonController::TSimpleSearchmapModifAction addService(TVector<TString>(), "sbtests", "tests4", "add_service", "rtyserver");
        Controller->ExecuteActionOnDeployManager(addService);
        NDaemonController::TAddReplicaAction addReplicaAction(4, "tests4", "sbtests", 1, sa, NDaemonController::apStartAndWait, "rtyserver", false, true);
        Controller->ExecuteActionOnDeployManager(addReplicaAction);

        TVector<NSearchMapParser::TSearchMapHost> slots = addReplicaAction.GetPool().GetSlots();
        TSet<TString> hosts;
        for (const auto& slot : slots) {
            hosts.insert(slot.Name);
        }
        CHECK_TEST_TRUE(hosts.size() == 4);
    }

    TString searchmap = Controller->SendCommandToDeployManager("process_storage?path=/common/sbtests/cluster.meta&download=yes&action=get");
    DEBUG_LOG << "cluster.meta: \n" << searchmap << Endl;
    return true;
}
};

START_TEST_DEFINE_PARENT(TestDeployManagerStoragePerformance, TestDeployManager)
void InitCluster() override {
    TTagsInfoParams params;
    params.RTYServers = 3000;
    params.RTYSlots = 1;
    GenerateTagsInfo(params, "tmp.tags.info");
    UploadCommon();
    UploadService("tests");
    Controller->UploadFileToDeployManager("tmp.tags.info", "/common/sbtests/tags.info");
    NDaemonController::TRestartAction ra(NDaemonController::apStartAndWait);
    NDaemonController::TControllerAgent agent(Controller->GetConfig().DeployManager.Host, Controller->GetConfig().DeployManager.Port + 3);
    if(!agent.ExecuteAction(ra))
        ythrow yexception() << "cannot restart deploy_manager";
}

bool Run() override {
    NDaemonController::TControllerAgent agent(Controller->GetConfig().DeployManager.Host, Controller->GetConfig().DeployManager.Port + 3);
    TString reply;
    TConfigFields configDiff;
    configDiff["Checker.Enabled"] = "false";
    TString command = "?command=set_config&fields=" + configDiff.Serialize() + "&prefix=DeployManager&reread_config=yes";
    agent.ExecuteCommand(command, reply, 30000, 1, "");
    DEBUG_LOG << reply << Endl;

    NRTYCluster::TSlotsAllocator sa;
    sa.AllowSameHostReplica = true;
    NDaemonController::TSimpleSearchmapModifAction addService(TVector<TString>(), "sbtests", "tests", "add_service", "rtyserver");
    Controller->ExecuteActionOnDeployManager(addService);
    NDaemonController::TAddReplicaAction addReplicaAction(900, "tests", "sbtests", 3, sa, NDaemonController::apStartAndWait, "rtyserver", false, true);
    Controller->ExecuteActionOnDeployManager(addReplicaAction);

    TInstant start = TInstant::Now();
    TString searchmap = Controller->SendCommandToDeployManager("process_storage?path=/common/sbtests/cluster.meta&download=yes&action=get");
    DEBUG_LOG << "cluster.meta: \n" << searchmap << Endl;
    TDuration readingTime = TInstant::Now() - start;
    DEBUG_LOG << "cluster.meta reading time: " << readingTime.MilliSeconds() << Endl;
    CHECK_TEST_TRUE(readingTime.Seconds() < 1);
    DeployIp();
    DeploySp();
    return true;
}
};

START_TEST_DEFINE_PARENT(TestDeployManagerMobileApplication, TestDeployManager)
void FixMessage(NRTYServer::TMessage& msg, ui64 kps, const TString& country) {
    msg.MutableDocument()->SetKeyPrefix(kps);
    auto attr = msg.MutableDocument()->AddSearchAttributes();
    attr->SetName("mid");
    attr->SetValue(country);
    attr->SetType(NRTYServer::TAttribute::LITERAL_ATTRIBUTE);
}
void InitCluster() override {
    UploadCommon();
    UploadService("mobile-application");
    UploadService("tests");
    UploadService("tests2");

    ConfigureCluster(1, 1, NSaas::UrlHash, "rtyserver", "tests");
    ConfigureCluster(1, 1, NSaas::UrlHash, "rtyserver", "tests2");
    Controller->SetActiveBackends(TBackendProxy::TBackendSet());
    NSearchMapParser::TMetaService ms;
    ms.Name = "mobile-application";
    ms.Components.push_back(NSearchMapParser::TMetaComponent("tests"));
    ms.Components.push_back(NSearchMapParser::TMetaComponent("tests2"));
    NDaemonController::TSimpleSearchmapModifAction addService(TVector<TString>(), "sbtests", ms.Name, "add_service", "metaservice");
    NDaemonController::TSimpleSearchmapModifAction editService(ms.SerializeToJson().GetStringRobust(), "sbtests", ms.Name, "metaservice");
    Controller->ExecuteActionOnDeployManager(addService);
    Controller->ExecuteActionOnDeployManager(editService);
    DeployIp();
    DeploySp();
}
bool Run() override {
    ui64 countMessages = 1;
    ui64 kps = GetIsPrefixed() ? 1 : 0;

    TVector<NRTYServer::TMessage> ruForRu;
    TVector<NRTYServer::TMessage> trForRu;
    TVector<NRTYServer::TMessage> ruForTr;
    TVector<NRTYServer::TMessage> trForTr;
    GenerateInput(ruForRu, countMessages, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed());
    GenerateInput(trForRu, countMessages, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed());
    GenerateInput(ruForTr, countMessages, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed());
    GenerateInput(trForTr, countMessages, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed());
    for (auto& msg : ruForRu) {
        FixMessage(msg, kps, "ru");
    }
    for (auto& msg : trForRu) {
        FixMessage(msg, kps, "tr");
    }
    for (auto& msg : ruForTr) {
        FixMessage(msg, kps, "ru");
    }
    for (auto& msg : trForTr) {
        FixMessage(msg, kps, "tr");
    }

    TIndexerClient::TContext ctx;
    ctx.Service = "tests";
    IndexMessages(ruForRu, REALTIME, ctx);
    IndexMessages(trForRu, REALTIME, ctx);

    ctx.Service = "tests2";
    IndexMessages(ruForTr, REALTIME, ctx);
    IndexMessages(trForTr, REALTIME, ctx);

    TVector<TDocSearchInfo> results;

    QuerySearch("body+mid:\"ru\"&service=tests&kps=" + ToString(kps), results, nullptr, nullptr, true);
    if (results.size() != countMessages)
        ythrow yexception() << "tests: invalid results count: " << results.size() << " != " << countMessages;
    QuerySearch("body+mid:\"tr\"&service=tests2&kps=" + ToString(kps), results, nullptr, nullptr, true);
    if (results.size() != countMessages)
        ythrow yexception() << "tests2: invalid results count: " << results.size() << " != " << countMessages;

    QuerySearch("body+mid:\"ru\"&service=mobile-application&kps=" + ToString(kps), results, nullptr, nullptr, true);
    if (results.size() != countMessages)
        ythrow yexception() << "mobile-application-rus: invalid results count: " << results.size() << " != " << countMessages;
    QuerySearch("body+mid:\"tr\"&service=mobile-application&kps=" + ToString(kps), results, nullptr, nullptr, true);
    if (results.size() != countMessages)
        ythrow yexception() << "mobile-application-for: invalid results count: " << results.size() << " != " << countMessages;
    return true;
}
};

START_TEST_DEFINE_PARENT(TestDeployManagerUI32Shards, TestDeployManager)
bool Run() override {
    UploadCommon();
    UploadService("tests");

    NSaas::TShardsDispatcher::TContext shCtx(NSaas::UrlHash, NSaas::ShardsCount::UI32, 0);
    ConfigureCluster(1, 2, shCtx, "rtyserver", "tests");

    DeploySp();
    DeployIp();

    TVector<NRTYServer::TMessage> messages;
    GenerateInput(messages, 100, NRTYServer::TMessage::MODIFY_DOCUMENT, GetIsPrefixed());
    {
        IndexMessages(messages, DISK, 1);
        ReopenIndexers();
    }
    TVector<TDocSearchInfo> results;
    QuerySearch("url:\"*\"&service=tests&" + GetAllKps(messages), results);
    CHECK_TEST_EQ(results.size(), 100);

    return true;
}
};
