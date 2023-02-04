#include "deploy_manager.h"
#include <saas/deploy_manager/meta/cluster.h>
#include <saas/deploy_manager/scripts/broadcast/broadcast_action.h>
#include <saas/deploy_manager/scripts/deploy/action.h>
#include <library/cpp/json/json_reader.h>

START_TEST_DEFINE_PARENT(TestDeployManagerBroadcast, TestDeployManager)
void CheckServiceSection(TString res, TString service, NJson::TJsonValue& result) {
    TStringInput si(res);
    if (!NJson::ReadJsonTree(&si, &result))
        ythrow yexception() << "incorrect json from broadcast: " << res;
    if (!result.Has(service))
        ythrow yexception() << "service '" << service << "' section not found: " << res;
}

void CheckReplies(TString res, bool filtered) {
    NJson::TJsonValue result;
    CheckServiceSection(res, "tests", result);
    for (size_t i = 0; i < Controller->GetConfig().Controllers.size(); ++i) {
        TString slot = Controller->GetConfig().Controllers[i].Host + ":" + ToString(Controller->GetConfig().Controllers[i].Port - 3);
        if (!result["tests"].Has(slot))
            ythrow yexception() << " absent slot: " << slot << " res: " << res;
        if (!result["tests"][slot].GetValueByPath("result.Svn_author", '.'))
            ythrow yexception() << "'result.Svn_author' not found in result, res: " << res;
        if (filtered && result["tests"][slot].GetValueByPath("result.Build_host", '.'))
            ythrow yexception() << "unexpected field in filtered result, res: " << res;
    }
}

void CheckRepliesIproxy(TString res) {
    NJson::TJsonValue result;
    TString service = "indexerproxy";
    CheckServiceSection(res, service, result);
    TString slot = Controller->GetConfig().Indexer.Host + ":" + ToString(Controller->GetConfig().Indexer.Port);
    if (!result[service].Has(slot))
        ythrow yexception() << " absent slot for indexerproxy: " << slot << " res: " << res;
    if (!result[service][slot].GetValueByPath("result.Svn_revision", '.'))
        ythrow yexception() << "'result.Svn_revision' not found in result, res: " << res;
}

void CheckRepliesUnused(TString res) {
    NJson::TJsonValue result;
    TString service = "unused";
    CheckServiceSection(res, service, result);
    if (result.GetMap().size() != 1)
        ythrow yexception() << "must be only one section 'unused', got " << res;
    if (result[service].GetMap().size() != 0)
        ythrow yexception() << " unused slots count must be zero, got " << res;
}

void CheckTable(TString res, TString words) {
    if (res.find(words) == NPOS)
        ythrow yexception() << " not found " << words << " in " << res;
    if (SplitString(res, "</tr").size() < 3)
        ythrow yexception() << " too little rows count " << SplitString(res, "</tr").size() << " in " << res;
}


bool CheckBroadcastGet() {
    TString res = Controller->SendCommandToDeployManager("broadcast?command=get_info_server&ctype=sbtests");
    CheckReplies(res, false);
    res = Controller->SendCommandToDeployManager("broadcast?command=get_info_server&ctype=sbtests&service=tests&filter=result.Svn_author,result.Svn_revision");
    CheckReplies(res, true);
    res = Controller->SendCommandToDeployManager("broadcast?command=get_info_server&ctype=sbtests&service_type=indexerproxy&filter=result.Svn_author,result.Svn_revision");
    CheckRepliesIproxy(res);
    res = Controller->SendCommandToDeployManager("broadcast?command=get_info_server&ctype=sbtests&service=indexerproxy&filter=result.Svn_author,result.Svn_revision");
    CheckRepliesIproxy(res);
    res = Controller->SendCommandToDeployManager("broadcast?command=get_info_server&ctype=sbtests&service=unused&filter=result.Svn_author,result.Svn_revision");
    CheckRepliesUnused(res);
    MUST_BE_BROKEN(Controller->SendCommandToDeployManager("broadcast?command=get_info_server&ctype=badctype&filter=result.Svn_author"));
    return true;
}

bool CheckBroadcastAction(const std::initializer_list<NSearchMapParser::TSearchMap> sms, const bool filtered,
    const std::initializer_list<std::pair<TString, TVector<TString>>>& serviceSlots) const
{
    NDaemonController::TBroadcastAction broadcastAction;
    if (sms.size() == 0) {
        ythrow yexception() << "No searchmaps provided";
    } else if (sms.size() == 1) {
        broadcastAction = NDaemonController::TBroadcastAction ("get_info_server", "ignored", "*", "ignored", false, sms.begin()->SerializeToJson());
    } else {
        NJson::TJsonValue arr(NJson::JSON_ARRAY);
        for (const auto& sm: sms) {
            arr.AppendValue(sm.SerializeToJsonObject());
        }
        const TString strMaps = NJson::WriteJson(arr);
        broadcastAction = NDaemonController::TBroadcastAction ("get_info_server", "ignored", "*", "ignored", false, strMaps);
        broadcastAction.SetIsMultiple(true);
    }

    if (filtered) {
        broadcastAction.GetFilter().insert("result.Svn_author");
        broadcastAction.GetFilter().insert("result.Svn_revision");
    }

    Controller->ExecuteActionOnDeployManager(broadcastAction);
    const  NJson::TJsonValue& result = broadcastAction.GetResult();
    for (const auto& pair: serviceSlots) {
        const TString service = pair.first;
        const TVector<TString>& slots = pair.second;
        CHECK_TEST_TRUE(result.Has(service));

        CHECK_TEST_EQ(result[service].GetMap().size(), slots.size());
        for (const TString& slot: slots) {
            CHECK_TEST_TRUE(result[service].Has(slot));
            CHECK_TEST_TRUE(result[service][slot].GetValueByPath("result.Svn_author", '.'));
            if (filtered) {
                CHECK_TEST_FAILED(result[service][slot].GetValueByPath("result.Build_host", '.'), "Value should be removed by filter");
                CHECK_TEST_FAILED(result[service][slot].GetValueByPath("result.server_status_global", '.'), "Value should be removed by filter");
            }
        }
    }
    return true;
}

bool CheckBroadcastSkipPingAction(const TString& service, const TString& ctype) const
{
    NSearchMapParser::TSearchMap mutableSm = GetSearchMapFromDM(service, ctype);
    NSearchMapParser::TSearchMapService* servicePtr = mutableSm.GetService(service);
    CHECK_TEST_TRUE(servicePtr != nullptr);
    servicePtr->SkipPing = true;
    const TString smStr = mutableSm.SerializeToJson();
    DEBUG_LOG << smStr << Endl;
    NDaemonController::TBroadcastAction broadcastAction = NDaemonController::TBroadcastAction("get_info_server", "ignored", "*", "ignored", false, smStr);
    Controller->ExecuteActionOnDeployManager(broadcastAction);
    const  NJson::TJsonValue& result = broadcastAction.GetResult();
    CHECK_TEST_FAILED(result.Has(service), "Service wtih skip ping is had been broadcasted");
    return true;
}

TVector<TString> GetBackendsSlots() const {
    TVector<TString> slots;
    slots.reserve(Controller->GetConfig().Controllers.size());
    for (const auto& controller: Controller->GetConfig().Controllers) {
        const TString slot = controller.Host + ":" + ToString(controller.Port - 3);
        slots.push_back(slot);
    }
    return slots;
}

bool CheckBackendsBroadcast() const {
    static const TString ctype = "sbtests";
    static const TString service = "tests";
    const NSearchMapParser::TSearchMap sm = GetSearchMapFromDM(service, ctype);

    const TVector<TString> slots = GetBackendsSlots();
    CHECK_TEST_TRUE(CheckBroadcastAction({sm}, false, {std::make_pair(service, slots)}));
    CHECK_TEST_TRUE(CheckBroadcastAction({sm}, true, {std::make_pair(service, slots)}));
    CHECK_TEST_TRUE(CheckBroadcastSkipPingAction(service, ctype));

    return true;
}

NSearchMapParser::TSearchMap GetFakeMapByServiceType(const TString& serviceName, const TString& serviceType, const TString& ctype) const {
    const TString slotsStr = Controller->SendCommandToDeployManager("using_slots?service=" + serviceName + "&ctype=" + ctype + "&service_type=" + serviceType);
    DEBUG_LOG << slotsStr << Endl;

    NJson::TJsonValue slotsJson;
    if (!NJson::ReadJsonFastTree(slotsStr, &slotsJson)) {
        ythrow yexception() << "Failed to parse json result";
    }

    const NJson::TJsonValue& cTypeJson = slotsJson[ctype];
    NSearchMapParser::TSearchMapReplica replica;
    for (const auto& entry: cTypeJson.GetMap()) {
        for (const auto& smhJson: entry.second.GetArray()) {
            NSearchMapParser::TSearchMapHost host;
            if (!host.Deserialize(smhJson)) {
                ythrow yexception() << "Failed to deserialize host";
            }
            replica.Hosts.push_back(host);
        }
    }
    replica.Alias = "default";

    NSearchMapParser::TSearchMap sm;
    NSearchMapParser::TSearchMapService& sms = sm.AddService(serviceName);
    sms.Replicas.push_back(replica);
    return sm;
}

bool CheckIpBroadcast() const {
    static const TString ctype = "sbtests";
    static const TString service = "indexerproxy";
    const NSearchMapParser::TSearchMap sm = GetFakeMapByServiceType(service, service, ctype);

    const TVector<TString> slots(1, TString(Controller->GetConfig().Indexer.Host + ":" + ToString(Controller->GetConfig().Indexer.Port)));
    CHECK_TEST_TRUE(CheckBroadcastAction({sm}, false, {std::make_pair(service, slots)}));
    CHECK_TEST_TRUE(CheckBroadcastAction({sm}, true, {std::make_pair(service, slots)}));

    return true;
}

bool CheckMultipleSearchMapsBroadcast() const {
    static const TString ctype = "sbtests";
    static const TString ipService = "indexerproxy";
    static const TString rtyService = "tests";

    const NSearchMapParser::TSearchMap ipSm = GetFakeMapByServiceType(ipService, ipService, ctype);
    const NSearchMapParser::TSearchMap rtySm = GetSearchMapFromDM(rtyService, ctype);

    const TVector<TString> ipSlots(1, TString(Controller->GetConfig().Indexer.Host + ":" + ToString(Controller->GetConfig().Indexer.Port)));
    const TVector<TString> rtySlots = GetBackendsSlots();
    CHECK_TEST_TRUE(CheckBroadcastAction({ipSm, rtySm}, false, {std::make_pair(ipService, ipSlots), std::make_pair(rtyService, rtySlots)}));
    CHECK_TEST_TRUE(CheckBroadcastAction({ipSm, rtySm}, true, {std::make_pair(ipService, ipSlots), std::make_pair(rtyService, rtySlots)}));
    return true;
}

bool CheckBroadcastPost() const {
    CHECK_TEST_TRUE(CheckBackendsBroadcast());
    CHECK_TEST_TRUE(CheckIpBroadcast());
    CHECK_TEST_TRUE(CheckMultipleSearchMapsBroadcast());
    return true;
}

bool CheckBroadcastTable() {
    TString res = Controller->SendCommandToDeployManager("broadcast_table?command=get_info_server&ctype=sbtests&service=tests");
    const ui32 be = *Controller->GetActiveBackends().begin();
    CheckTable(res, Controller->GetConfig().Controllers[be].Host + ":" + ToString(Controller->GetConfig().Controllers[be].Port - 3));
    res = Controller->SendCommandToDeployManager("broadcast_table?command=get_info_server&ctype=sbtests&filter=result.cpu_load_user,result.cpu_load_system");
    CheckTable(res, "cpu_load_system");
    res = Controller->SendCommandToDeployManager("broadcast_table?command=get_info_server&ctype=sbtests&filter=result.Svn_revision,result.server_status&by_replicas=true");
    CheckTable(res, "Active");
    res = Controller->SendCommandToDeployManager("broadcast_table?command=get_info_server&ctype=sbtests&service=searchproxy&filter=result.Svn_revision");
    CheckTable(res, "Svn_revision");
    return true;
}

bool Run() override {
    UploadCommon();
    UploadService("tests");
    ConfigureCluster(2, 1, NSaas::UrlHash, "rtyserver", "tests");
    DeployBe("tests");

    CHECK_TEST_TRUE(CheckBroadcastGet());
    CHECK_TEST_TRUE(CheckBroadcastPost());
    CHECK_TEST_TRUE(CheckBroadcastTable());

    return true;
}
};

START_TEST_DEFINE_PARENT(TestDeployManagerCheckPingStore, TestDeployManager)
TVector<TString> BackendSlots;
void InitSlotslist() {
    for (size_t i = 0; i < Controller->GetConfig().Controllers.size(); ++i) {
        TString slot = Controller->GetConfig().Controllers[i].Host + ":" + ToString(Controller->GetConfig().Controllers[i].Port - 3);
        BackendSlots.push_back(slot);
    }
}

void CheckDeployPing(TString res, TString status, TString service = "tests") {
    NJson::TJsonValue result;
    TStringInput si(res);
    if (!NJson::ReadJsonTree(&si, &result))
        ythrow yexception() << "incorrect json from deploy_ping: " << res;
    if (result[service].GetMap().size() != BackendSlots.size())
        ythrow yexception() << "incorrect slots count in deploy_ping, expected " << BackendSlots.size() << ", res: " << res;
    for (size_t i = 0; i < BackendSlots.size(); ++i) {
        if (!result[service].Has(BackendSlots[i]))
            ythrow yexception() << "slot " << BackendSlots[i] << " not found in deploy_ping, res: " << res;
        if (result[service][BackendSlots[i]] != status)
            ythrow yexception() << "wrong status for slot " << BackendSlots[i] << ", expected " << status << ", res: " << res;
    }
}

void CheckDeployPingProxy(TString res, TString status, TString service = "searchproxy") {
    NJson::TJsonValue result;
    TStringInput si(res);
    if (!NJson::ReadJsonTree(&si, &result))
        ythrow yexception() << "incorrect json from deploy_ping: " << res;
    if (result[service].GetMap().size() != 1)
        ythrow yexception() << "incorrect slots count in deploy_ping for proxy, expected 1, res: " << res;
    TString slot = Controller->GetConfig().Searcher.Host + ":" + ToString(Controller->GetConfig().Searcher.Port);
    if (result[service][slot] != status)
        ythrow yexception() << "wrong status for slot " << slot << ", expected " << status << ", res: " << res;
}

bool LooksLikeInfoServer(NJson::TJsonValue info) {
    CHECK_TEST_FAILED(!info.IsMap(), "infoserver is not map")
    CHECK_TEST_FAILED(!info.Has("config"), "infoserver hasn't 'config'")
    CHECK_TEST_FAILED(!info.Has("docs_in_final_indexes"), "infoserver hasn't 'docs_in_final_indexes'")
    CHECK_TEST_FAILED(!info.Has("Svn_revision"), "infoserver hasn't 'Svn_revision'")
    return true;
}

void CheckInfoStored(TString stored) {
    TStringInput ssi(stored);
    NJson::TJsonValue jst, js, info;
    if (!NJson::ReadJsonTree(&ssi, &jst))
        ythrow yexception() << "incorrect json from storage: " << stored;
    if (jst["files"].GetArray().size() != BackendSlots.size())
        ythrow yexception() << " incorrect slots number stored, must be " << BackendSlots.size() << ", got " << jst["files"].GetArray().size() << ", res: " << stored;
    for (size_t i = 0; i < jst["files"].GetArray().size(); ++i) {
        TString slot = jst["files"].GetArray()[i].GetString();
        TString slotRes = Controller->SendCommandToDeployManager("process_storage?action=get&path=/common/savedinfo/" + slot + "&root=/");
        TStringInput si(slotRes);
        if (!NJson::ReadJsonTree(&si, &js))
            ythrow yexception() << "incorrect json about slot " << slot << " info: " << slotRes;
        TStringInput ss(js["data"].GetString());
        if (!NJson::ReadJsonTree(&ss, &info))
            ythrow yexception() << "incorrect info_server about slot " << slot << ", info: " << js["data"].GetString();
        if (!LooksLikeInfoServer(info))
            ythrow yexception() << "something wrong with info_server, slot " << slot << ", info: " << js["data"].GetString();
    }
}

bool Run() override {
    InitSlotslist();
    TString res;
    res = Controller->SendCommandToDeployManager("deploy_ping?ctype=sbtests&service=unused");
    CheckDeployPing(res, "Stopped", "unused");
    res = Controller->SendCommandToDeployManager("deploy_ping?ctype=sbtests&service_type=searchproxy");
    START_STOP_LOG(CheckDeployPingProxy(res, "Stopped"));

    UploadCommon();
    UploadService("tests");
    ConfigureCluster(2, 1, NSaas::UrlHash, "rtyserver", "tests");
    res = Controller->SendCommandToDeployManager("deploy_ping?ctype=sbtests&service=tests");
    START_STOP_LOG(CheckDeployPing(res, "Active"));
    res = Controller->SendCommandToDeployManager("deploy_ping?ctype=sbtests");
    START_STOP_LOG(CheckDeployPing(res, "Active"));

    DeployBe("tests");

    res = Controller->SendCommandToDeployManager("deploy_ping?ctype=sbtests");
    START_STOP_LOG(CheckDeployPing(res, "Active"));
    res = Controller->SendCommandToDeployManager("deploy_ping?ctype=sbtests&service_type=searchproxy");
    START_STOP_LOG(CheckDeployPingProxy(res, "Active"));

//    Cout << Controller->GetConfig().DeployManager.Port << Endl;
//    sleep (10000);
    CheckConfigs(true);
    UploadService("tests", 1);
    CheckConfigs(false);

    res = Controller->SendCommandToDeployManager("store_cluster_info?ctype=sbtests&store_path=/common/savedinfo");
    TString wasInfo = Controller->SendCommandToDeployManager("process_storage?action=get&path=/common/savedinfo&root=/");
    START_STOP_LOG(CheckInfoStored(wasInfo));
    MUST_BE_BROKEN(Controller->SendCommandToDeployManager("store_cluster_info?ctype=sbtests"));

    return true;
}
};
