#include "deploy_manager.h"

#include <saas/deploy_manager/scripts/deploy/action.h>
#include <saas/deploy_manager/scripts/add_replica/action.h>
#include <saas/deploy_manager/scripts/searchmap/action.h>

#include <saas/rtyserver_test/common/hostname.h>

#include <saas/library/searchmap/parsers/json/json.h>

#include <util/folder/filelist.h>

namespace {
    inline TString GetSearchProxyServiceConfig() {
        return "<Service>\nName: %SERVICENAME%\n</Service>";
    }
}

namespace {
    inline TString GetIndexerProxyServiceConfig() {
        return "<%SERVICENAME%>\n</%SERVICENAME%>";
    }
}

bool TestDeployManager::CheckServiceConfigSafe(const TString& service, const ui32 version, i32 be) {
    try {
        CheckServiceConfig(service, version, be);
        return true;
    } catch (...) {
        return false;
    }
}

bool TestDeployManager::CheckServiceStatus(const TString& status, i32 be) {
    TJsonPtr serverInfo(Controller->GetServerInfo());
    ui32 id = 0;
    for (TBackendProxy::TBackendSet::const_iterator i = Controller->GetActiveBackends().begin(); i != Controller->GetActiveBackends().end(); ++i, ++id) {
        NJson::TJsonValue& info = (*serverInfo)[id];
        if (*i == (ui32)be || be < 0) {
            if (info["server_status"].GetStringRobust() != status) {
                DEBUG_LOG << "For " << id << " server " << info["server_status"].GetStringRobust() << " != " << status << Endl;
                return false;
            }
        }
    }
    return true;
}

void TestDeployManager::CheckServiceConfig(const TString& service, const ui32 version, i32 be) {
    TJsonPtr serverInfo(Controller->GetServerInfo());
    ui32 id = 0;
    TString localPath = "/services/" + service + "/" + (version ? "ver." + ToString(version) + "/" : "");
    TString path = GetRunDir() + "/copy/" + localPath + "/rtyserver.diff-" + service;
    if (!TFsPath(path).Exists()) {
        path = GetRunDir() + "/copy/" + localPath + "/rtyserver.diff-%SERVICENAME%";
    }
    VERIFY_WITH_LOG(TFsPath(path).Exists(), "%s", path.data());
    TUnbufferedFileInput fi(path);
    NJson::TJsonValue value;
    VERIFY_WITH_LOG(NJson::ReadJsonTree(&fi, true, &value), "%s", path.data());

    NJson::TJsonValue::TMapType map;
    CHECK_WITH_LOG(value.GetMap(&map));

    for (TBackendProxy::TBackendSet::const_iterator i = Controller->GetActiveBackends().begin(); i != Controller->GetActiveBackends().end(); ++i, ++id) {
        NJson::TJsonValue& info = (*serverInfo)[id];
        if (*i == (ui32)be || be < 0) {
            for (auto&& idConf : map) {
                TVector<TString> vs = SplitString(idConf.first, ".");
                NJson::TJsonValue jsonCmp = info["config"]["Server"];
                TString path = "";
                for (ui32 i = 0; i < vs.size(); ++i) {
                    NJson::TJsonValue copy = jsonCmp[0][vs[i]];
                    jsonCmp = copy;
                }

                TString cmpValue = idConf.second.GetStringRobust();
                if (jsonCmp.GetStringRobust() != cmpValue)
                    ythrow yexception() << jsonCmp.GetStringRobust() << " != " << cmpValue << " for " << *i << "/" << idConf.first << " in " << info["config"]["Server"].GetStringRobust();
            }
        }
    }
}

void TestDeployManager::UploadCommon(i32 ver, bool uploadEmptySearchmap) {
    TDebugLogTimer timer("Upload common files");
    if (ver != -1) {
        UploadFiles("defaults/ver." + ToString(ver), "/defaults/");
    } else {
        UploadFiles("defaults", "/defaults/");
    }
    UploadFiles("common", "/common/");
    UploadFiles("indexerproxy", "/configs/indexerproxy/");
    UploadFiles("searchproxy", "/configs/searchproxy/");
    UploadFiles("intsearch", "/configs/intsearch/");
    if (uploadEmptySearchmap)
        Controller->UploadDataToDeployManager(" ", "/common/" + CTYPE + "/cluster.meta");
    else
        Controller->UploadFileToDeployManager(GetRunDir() + "/copy/cluster.meta", "/common/" + CTYPE + "/cluster.meta");
}

void TestDeployManager::Deploy(const TString& service, const TString& serviceType, const TString& forceServices) {
    TDebugLogTimer timer("Deploy " + service + "/" + serviceType);
    NRTYDeploy::TDeployAction daBE(service, CTYPE, !!forceServices ? "CURRENT" : "NEW", NDaemonController::apStartAndWait, serviceType, 0.01, nullptr, forceServices);
    Controller->ExecuteActionOnDeployManager(daBE);
}

void TestDeployManager::DeployBe(const TString& service) {
    Deploy(service, RTYSERVER_SERVICE);
}

void TestDeployManager::DeployBe(const TString& service, ui32 version) {
    UploadService(service, version);
    DeployBe(service);
}

void TestDeployManager::DeployMetaservice(const TString& service) {
    Deploy(service, META_SERVICE_SERVICE);
}

void TestDeployManager::DeploySp(const TString& forceServices) {
    Deploy(SEARCH_PROXY_SERVICE, SEARCH_PROXY_SERVICE, forceServices);
}

void TestDeployManager::DeployIp(const TString& forceServices) {
    Deploy(INDEXER_PROXY_SERVICE, INDEXER_PROXY_SERVICE, forceServices);
}

void TestDeployManager::UploadFiles(const TString& pathInResources, const TString& pathInDM) {
    TString path = GetRunDir() + "/copy/" + pathInResources + "/";
    TFileList fl;
    fl.Fill(path);
    const char* fileName;
    while (fileName = fl.Next()) {
        INFO_LOG << "Upload file " << path + TString(fileName) << " -> " << pathInDM + "/" + TString(fileName) << Endl;
        Controller->UploadFileToDeployManager(path + TString(fileName), pathInDM + "/" + TString(fileName));
    }
}

void TestDeployManager::UploadService(const TString& service, ui32 version) {
    UploadServiceCommon("services", service, version);
}

void TestDeployManager::UploadServiceCommon(const TString& root, const TString& service, ui32 version) {
    TString localPath = "/" + root + "/" + service + "/" + (version ? "ver." + ToString(version) + "/" : "");
    UploadFiles(localPath, "/configs/" + service + "/");
    if (root == "services") {
        if (!TFsPath(GetRunDir() + "/copy/" + localPath + "/searchproxy-" + service + ".conf").Exists() && !TFsPath(GetRunDir() + "/copy/" + localPath + "/searchproxy-%SERVICENAME%.conf").Exists())
            Controller->UploadDataToDeployManager(GetSearchProxyServiceConfig(), "/configs/" + service + "/searchproxy-%SERVICENAME%.conf");
        if (!TFsPath(GetRunDir() + "/copy/" + localPath + "/indexerproxy-" + service + ".conf").Exists() && !TFsPath(GetRunDir() + "/copy/" + localPath + "/indexerproxy-%SERVICENAME%.conf").Exists())
            Controller->UploadDataToDeployManager(GetIndexerProxyServiceConfig(), "/configs/" + service + "/indexerproxy-%SERVICENAME%.conf");
    }
}

NSearchMapParser::TSlotsPool TestDeployManager::ConfigureCluster(ui32 replics, ui32 shardsInReplica, NSaas::ShardingType shardBy, const TString& serviceType, const TString& service) {
    return ConfigureCluster(replics, shardsInReplica, NSaas::TShardsDispatcher::TContext(shardBy), serviceType, service);
}

NSearchMapParser::TSlotsPool TestDeployManager::ConfigureCluster(ui32 replics, ui32 shardsInReplica, const NSaas::TShardsDispatcher::TContext& shardBy, const TString& serviceType, const TString& service) {
    Controller->WaitServerDesiredStatus(THashSet<TCiString>({"Active", "NotRunnable", "Stopped"}));
    TDebugLogTimer timer("Configure cluster " + ToString(replics) + " replics by " + ToString(shardsInReplica) + "shards, shard by " + shardBy.ToString());
    NDaemonController::TSimpleSearchmapModifAction addService(TVector<TString>(), CTYPE, service, "add_service", serviceType);
    Controller->ExecuteActionOnDeployManager(addService);

    NSearchMapParser::TServiceSpecificOptions opts;
    opts.ShardsDispatcher.Reset(new NSaas::TShardsDispatcher(shardBy));
    NDaemonController::TSimpleSearchmapModifAction editService(opts, CTYPE, service, serviceType);
    Controller->ExecuteActionOnDeployManager(editService);
    TBackendProxy::TBackendSet lifeBe;

    Controller->WaitServerDesiredStatus(THashSet<TCiString>({"Active", "NotRunnable", "Stopped"}));

    NRTYCluster::TSlotsAllocator sa;
    sa.AllowSameHostReplica = true;
    NDaemonController::TAddReplicaAction replica(shardsInReplica, service, CTYPE, replics, sa, NDaemonController::apStartAndWait, serviceType, false);
    Controller->ExecuteActionOnDeployManager(replica);
    NSearchMapParser::TSlotsPool pool = replica.GetPool();
    if (serviceType == "rtyserver") {
        const TVector<NSearchMapParser::TSearchMapHost>& slots = pool.GetSlots();
        if (slots.size() != shardsInReplica * replics)
            ythrow yexception() << "invalid replica created " << pool.Serialize().GetStringRobust();
        for (TVector<NSearchMapParser::TSearchMapHost>::const_iterator i = slots.begin(); i != slots.end(); ++i) {
            ui32 be = Controller->GetConfig().FindController(TestsHostName(), i->ControllerPort());
            if (be > Controller->GetConfig().Controllers.size())
                ythrow yexception() << "cannot find port " << i->ControllerPort();
            lifeBe.insert(be);
        }
        Controller->SetActiveBackends(lifeBe);
        for (TBackendProxy::TBackendSet::const_iterator i = lifeBe.begin(); i != lifeBe.end(); ++i)
            DEBUG_LOG << *i << ") " << Controller->GetConfig().Controllers[*i].Host << ":" << Controller->GetConfig().Controllers[*i].Port << Endl;
    }
    return pool;
}

NSearchMapParser::TSearchMapHost TestDeployManager::FindBackend(ui32 be, const TString& service, const TString& ctype) const {
    const NSearchMapParser::TSearchMap sm = GetSearchMapFromDM(service, ctype);
    const NSearchMapParser::TSearchMapService* servicePtr = sm.GetService(service);
    if (!servicePtr)
        ythrow yexception() << "service " << service << " not found in searchmap";
    const auto& controller = Controller->GetConfig().Controllers[be];
    const TVector<NSearchMapParser::TSearchMapHost>& slots = servicePtr->GetSlots();
    for (const auto& host: slots) {
        if (controller.Host == host.Name && controller.Port == host.ControllerPort()) {
            return host;
        }
    }
    ythrow yexception() << "Backend " << be << " not found";
}

NSearchMapParser::TSearchMap TestDeployManager::GetSearchMapFromDM(const TString& service, const TString& ctype) const {
    const TString searchmap = Controller->SendCommandToDeployManager("get_conf?root=/&filename=/common/" + ctype + "/cluster.meta&ctype=" + ctype + "&service=" + service);
    DEBUG_LOG << "Searchmap: " << searchmap << Endl;
    TStringInput si(searchmap);
    NSearchMapParser::TJsonSearchMapParser jsmp(si);
    return jsmp.GetSearchMap(false, false);
}

void TestDeployManager::RenewActiveBackends() {
    TDebugLogTimer timer("Renew active backends");
    NSearchMapParser::TSearchMap smp = GetSearchMapFromDM();
    NSearchMapParser::TSearchMapService* service = smp.GetService("tests");
    if (!service)
        ythrow yexception() << "service tests not found in searchmap";
    const TVector<NSearchMapParser::TSearchMapHost>& slots = service->GetSlots();
    TBackendProxy::TBackendSet lifeBe;
    for (TVector<NSearchMapParser::TSearchMapHost>::const_iterator host = slots.begin(); host != slots.end(); ++host)
        lifeBe.insert(Controller->GetConfig().FindController(host->Name, host->ControllerPort()));
    Controller->SetActiveBackends(lifeBe);
}

void TestDeployManager::CheckEnabled(const NSearchMapParser::TSlotsPool* disabledSlotsIndex, const NSearchMapParser::TSlotsPool* disabledSlotsSearch) const {
    TDebugLogTimer timer("Check enabled");
    NSearchMapParser::TSearchMap smp = GetSearchMapFromDM();
    NSearchMapParser::TSearchMapService* service = smp.GetService("tests");
    if (!service)
        ythrow yexception() << "service tests not found in searchmap";
    const TVector<NSearchMapParser::TSearchMapHost>& slots = service->GetSlots();
    TVector<TString> errors;
    for (auto&& host : slots) {
        if (disabledSlotsIndex) {
            if ((disabledSlotsIndex->SlotExists(host.GetSlotName()) && !host.DisableIndexing) ||
                (!disabledSlotsIndex->SlotExists(host.GetSlotName()) && host.DisableIndexing))
                ythrow yexception() << "Incorrect disable indexing status for " << host.GetSlotName();
        } else {
            if (host.DisableIndexing)
                ythrow yexception() << "Incorrect disable indexing status for " << host.GetSlotName();
        }
        if (disabledSlotsSearch) {
            if ((disabledSlotsSearch->SlotExists(host.GetSlotName()) && !host.DisableSearch) ||
                (!disabledSlotsSearch->SlotExists(host.GetSlotName()) && host.DisableSearch))
                ythrow yexception() << "Incorrect disable search status for " << host.GetSlotName();
        } else {
            if (host.DisableSearch)
                ythrow yexception() << "Incorrect disable search status for " << host.GetSlotName();
        }
    }
}

void TestDeployManager::CheckDocuments(const TVector<NRTYServer::TMessage>& messages, const TSet<std::pair<ui64, TString>>& deleted, ui32 slotsCount) {
    TDebugLogTimer timer("Check documents");
    CheckSearchResults(messages, deleted);
    ui32 restoreValueMetaSearchLevel = Controller->GetRequiredMetaSearchLevel();
    Controller->SetRequiredMetaSearchLevel(2);
    NSearchMapParser::TSearchMap smp = GetSearchMapFromDM();
    NSearchMapParser::TSearchMapService* service = smp.GetService("tests");
    if (!service)
        ythrow yexception() << "service tests not found in searchmap";
    const TVector<NSearchMapParser::TSearchMapHost>& slots = service->GetSlots();
    if (slotsCount && slots.size() != slotsCount) {
        TStringStream errors;
        errors << "incorrect slots count: " << slots.size() << "!=" << slotsCount << ", slots:" << Endl;
        for (const auto& host: slots)
            errors << host.GetSlotName() << Endl;
        ythrow yexception() << errors.Str();
    }
    TVector<TString> errors;
    for (const auto& host: slots) {
        if (host.DisableSearch)
            continue;

        for (const auto& msg: messages) {
            const NSearchMapParser::TShardIndex shard = service->ShardsDispatcher->GetShard(msg);
            const auto& url = msg.GetDocument().GetUrl();
            const ui64 kps = msg.GetDocument().GetKeyPrefix();

            TVector<TDocSearchInfo> results;
            QuerySearch("url:\"" + url + "\"&timeout=100000000&kps=" + ToString(kps), results, nullptr, nullptr, false, host.Name, host.SearchPort);
            size_t expected = 0;
            if (deleted.find(std::make_pair(kps, url)) == deleted.end() && host.Shards.Check(shard))
                expected = 1;
            if (results.size() != expected)
                errors.push_back("Document kps=" + ToString(kps) + ", url=" + url +
                    " count incorrect on " + host.GetSlotName() + ", " + ToString(results.size()) + " != " + ToString(expected));
        }
    }
    Controller->SetRequiredMetaSearchLevel(restoreValueMetaSearchLevel);
    if (!errors.empty())
        ythrow yexception() << "Errors in documents:\n" << JoinStrings(errors, "\n");
}

bool TestDeployManager::CheckBackends(const TVector<TString>& backends) {
    TDebugLogTimer timer("Check backends");
    NSearchMapParser::TSearchMap smp = GetSearchMapFromDM();
    NSearchMapParser::TSearchMapService* service = smp.GetService("tests");
    const TVector<NSearchMapParser::TSearchMapHost>& slots = service->GetSlots();
    for (auto&& i : backends) {
        bool found = false;
        for (auto&& host : slots) {
            if (host.GetSlotName() == i) {
                found = true;
                break;
            }
        }
        if (!found) {
            ERROR_LOG << "backend " << i << " not found" << Endl;
            return false;
        }
    }
    return true;
}

void TestDeployManager::CheckConfigs(bool equal) {
    TVector<TString> backendSlots;
    for (auto&& i : Controller->GetActiveBackends()){
        TString slot = Controller->GetConfig().Controllers[i].Host + ":" + ToString(Controller->GetConfig().Controllers[i].Port - 3);
        backendSlots.push_back(slot);
    }
    TString res = Controller->SendCommandToDeployManager("check_configs?ctype=" + CTYPE);
    NJson::TJsonValue result;
    TStringInput si(res);
    if (!NJson::ReadJsonTree(&si, &result))
        ythrow yexception() << "incorrect json from check_configs: " << res;
    for (size_t i = 0; i < backendSlots.size(); ++i) {
        if (!result["tests"].Has(backendSlots[i]))
            ythrow yexception() << "slot " << backendSlots[i] << " not found in deploy_ping, res: " << res;
        NJson::TJsonValue slotConfigs = result["tests"][backendSlots[i]];
        if (equal)
            for (auto i : slotConfigs.GetMap()) {
                if (i.second["from_host"] != i.second["last_deployed"] || i.second["from_host"] != i.second["last_stored"])
                    ythrow yexception() << "not equal hashes in check_configs, file " << i.first << " got " << res;
            }
        else {
            NJson::TJsonValue confCommon = result["tests"][backendSlots[i]]["rtyserver.conf-common"];
            if (confCommon["last_deployed"] == confCommon["last_stored"] || confCommon["last_deployed"] != confCommon["from_host"])
                ythrow yexception() << "hash didn't change: " << res;
        }
    }
}

bool TestDeployManager::SetAlertsUniqueNames() {
    NDaemonController::TControllerAgent agent(Controller->GetConfig().DeployManager.Host, Controller->GetConfig().DeployManager.Port + 3);
    TString reply;
    TConfigFields configDiff;
    TString nowTs = ToString(Now().Seconds());
    configDiff["Alerts.Juggler.CommonTag"] = "saas_autotest_" + nowTs;
    configDiff["Alerts.Golovan.AlertsPrefix"] = "saastest.gen." + nowTs;
    TString command = "?command=set_config&fields=" + configDiff.Serialize() + "&prefix=DeployManager&reread_config=yes";
    return agent.ExecuteCommand(command, reply, 3000, 1, "");
}

bool TestDeployManager::CheckAlertsCount(const ui16 mustBe, const TString& cgis, NJson::TJsonValue& alerts, const int jugCnt) {
    Sleep(TDuration::Seconds(0.5));
    TString alertsList = Controller->SendCommandToDeployManager("process_alerts?action=list" + cgis);
    DEBUG_LOG << alertsList << Endl;
    NJson::TJsonValue alertsJs;
    TStringStream ss(alertsList);
    NJson::ReadJsonTree(&ss, &alertsJs);
    alerts = NJson::TJsonValue(NJson::JSON_MAP);
    const NJson::TJsonValue& checks = alertsJs.GetMap().at("checks").GetMap().at("content");
    alerts["checks"] = checks;
    ui16 checksCnt = 0;
    for (const auto& gHost : checks.GetMap()) {
        checksCnt += gHost.second.GetMap().ysize();
    }
    CHECK_TEST_EQ(checksCnt, (jugCnt == -1 ? mustBe : ui16(jugCnt)));
    const NJson::TJsonValue& yasms = alertsJs.GetMap().at("alerts").GetMap().at("content").GetMap().at("response").GetMap().at("result");
    alerts["alerts"] = yasms;
    CHECK_TEST_EQ(yasms.GetArray().ysize(), mustBe);
    return true;
}

const TString TestDeployManager::CTYPE = "sbtests";

TDMRequestFailer::~TDMRequestFailer() {
    UnregisterGlobalMessageProcessor(this);
}

TDMRequestFailer::TDMRequestFailer(const char* command) : TDebugProcessorDMRequestFailer(command) {
    RegisterGlobalMessageProcessor(this);
}

TDMBuildTaskFailer::~TDMBuildTaskFailer() {
    UnregisterGlobalMessageProcessor(this);
}

TDMBuildTaskFailer::TDMBuildTaskFailer(const char* taskType) : TDebugProcessorBuildTaskFailer(taskType) {
    RegisterGlobalMessageProcessor(this);
}
