#include "deploy_manager.h"
#include <saas/library/daemon_base/controller_actions/restart.h>
#include <library/cpp/json/json_reader.h>
#include <saas/deploy_manager/scripts/searchmap/action.h>
#include <saas/deploy_manager/scripts/add_replica/action.h>
#include <saas/deploy_manager/server/messages.h>
#include <library/cpp/yconf/patcher/unstrict_config.h>
#include <saas/library/searchmap/searchmap.h>
#include <saas/rtyserver/common/common_messages.h>
#include <saas/util/external/dc.h>

START_TEST_DEFINE_PARENT(TestDeployManagerSetListGetConf, TestDeployManager)
void Check(const TString& listConfRequest, const TString& expectedService, const TString& confSubstr="") {
    TString listStr = Controller->SendCommandToDeployManager("list_conf?" + listConfRequest);
    NJson::TJsonValue list;
    TStringInput si(listStr);
    if (!NJson::ReadJsonTree(&si, &list))
        ythrow yexception() << "incorrect json from list_conf for " << listConfRequest << ": " << listStr;
    const TString gotService = list["slot_info"]["service"].GetString();
    if (gotService != expectedService)
        ythrow yexception() << "service expected=" << expectedService << ", got=" << gotService;
    const NJson::TJsonValue::TArray& files = list["files"].GetArray();
    if (files.size() < 4)
        ythrow yexception() << "too few files in list (" << files.size() << " < 4) for " << listConfRequest << ":" << listStr;
    bool foundMainConf = false;
    for (NJson::TJsonValue::TArray::const_iterator i = files.begin(); i != files.end(); ++i) {
        TString url = i->operator[]("url").GetString();
        i32 version = i->operator[]("version").GetInteger();
        TString content = Controller->SendCommandToDeployManager("get_conf?root=/&filename=" + url + "&version=" + ToString(version));
        if (!content)
            ythrow yexception() << "invalid content for " << url << ", version " << version << " for " << listConfRequest;
        if (confSubstr && i->operator[]("rename").GetString() == "rtyserver.conf-common") {
            foundMainConf = true;
            if (content.find(confSubstr) == TString::npos)
                ythrow yexception() << "Not found substring " << confSubstr << " in config";
        }
    }
    if (confSubstr && !foundMainConf)
        ythrow yexception() << "Not found rtyserver.conf-common";
}

bool Run() override {
    UploadCommon();
    Controller->UploadDataToDeployManager(" ", "/common/unused/cluster.meta");
    UploadService("tests");
    Controller->UploadFileToDeployManager(GetRunDir() + "/copy/searchmap_two_services", "/common/sbtests/cluster.meta");
    Check("slot=host1:10005", "tests");
    Check("slot=chush1:10005&service_type=rtyserver", "unused");
    TDatacenterUtil::Instance().SetRealHost("host-1-gencfg", "host1");
    //"local" cheat in TDatacenterUtil, maybe remove it?
    Check("slot=host1__host-1-gencfg:10005&service_type=rtyserver", "tests");
    TString content1 = Controller->SendCommandToDeployManager("get_conf?service=tests&filename=rtyserver.diff-tests");
    TString content2 = Controller->SendCommandToDeployManager("get_conf?service=tests&filename=rtyserver.diff-tests&orig=da");
    if (content1 == content2)
        ythrow yexception() << "orig does not work, orig:\n" << content2 << "\nno orig:\n" << content1;
    UploadService("tests", 1);
    Check("slot=host1:10005", "tests", "TimingCheckIntervalMilliseconds : 9900");
    return true;
}
};

START_TEST_DEFINE_PARENT(TestDeployManagerDistributorCfg, TestDeployManager)
bool Run() override {
    UploadCommon();
    UploadService("tests");
    Controller->UploadFileToDeployManager(GetRunDir() + "/copy/cluster.distr.meta", "/common/sbtests/cluster.meta");
    Controller->SendCommandToDeployManager("modify_searchmap?ctype=sbtests&action=enable_indexing&slots_vector=localhost:10005&service=tests");
    const TString& rtyConfig = Controller->SendCommandToDeployManager("get_conf?service=tests&filename=rtyserver.diff-tests&ctype=sbtests");

    NJson::TJsonValue jsonConfig = TUnstrictConfig::ToJson(rtyConfig);
    if (jsonConfig["Server"][0]["ModulesConfig"][0]["DOCFETCHER"][0]["Stream"][0]["DistributorServers"].GetString() != "localhost:20100")
        ythrow yexception() << "incorrect ModulesConfig.DOCFETCHER.DistributorServers";
    return true;
}
};

START_TEST_DEFINE_PARENT(TestDeployManagerGetService, TestDeployManager)
void CheckSlot(const TString& slot, const TString& service, const TString& ctype) {
    TString res = Controller->SendCommandToDeployManager("get_service?slot=" + slot);
    NJson::TJsonValue result;
    TStringInput si(res);
    if (!NJson::ReadJsonTree(&si, &result))
        ythrow yexception() << "incorrect json from list_conf: " << res;
    if (service == "" && ctype == "") {
        if (result.GetArray().size() != 0)
            ythrow yexception() << "result must be empty: " << res;
        return;
    }
    if (result.GetArray().size() != 1)
        ythrow yexception() << "incorrect count of services (must be 1)" << res;
    if (result[0]["name"].GetString() != service || result[0]["ctype"].GetString() != ctype)
        ythrow yexception() << "invalid service-ctype pair got: " << res << ", must be " << service << "-" << ctype;
}
void CheckServerSection(NJson::TJsonValue& result, TString res) {
    if (result.GetArray().size() != 2)
        ythrow yexception() << "incorrect count of services (must be 2)" << res;
    TSet<TString> services;
    for (size_t i = 0; i < result.GetArray().size(); ++i)
        services.insert(result[i]["name"].GetString());
    if (services.find("tests") == services.end())
        ythrow yexception() << "service tests not found in " << res;
    if (services.find("newservice") == services.end())
        ythrow yexception() << "service newservice not found in " << res;
}
void CheckServer() {
    TString res = Controller->SendCommandToDeployManager("get_service?server=host1");
    NJson::TJsonValue result;
    TStringInput si(res);
    if (!NJson::ReadJsonTree(&si, &result))
        ythrow yexception() << "incorrect json from list_conf: " << res;
    CheckServerSection(result, res);
}
void CheckNoServerSlot() {
    TString res = Controller->SendCommandToDeployManager("get_service?ctype=sbtests");
    NJson::TJsonValue result;
    TStringInput si(res);
    if (!NJson::ReadJsonTree(&si, &result))
        ythrow yexception() << "incorrect json from list_conf: " << res;
    CheckServerSection(result["host1"], res);
}
bool Run() override {
    Controller->UploadFileToDeployManager(GetRunDir() + "/copy/searchmap_two_services", "/common/sbtests/cluster.meta");
    CheckSlot("host1:10005", "tests", "sbtests");
    CheckSlot("host2:10015", "newservice", "sbtests");
    CheckSlot("host1:10005&ctype=sbtests", "tests", "sbtests");
    CheckSlot("host2:10015&service=newservice", "newservice", "sbtests");
    CheckSlot("host2:10015&service=newservice&ctype=sbtests", "newservice", "sbtests");
    CheckSlot("host2:10015&service=tests", "", "");
    CheckSlot("loaclhost:10005", "unused", "unused");
    CheckServer();
    CheckNoServerSlot();
    return true;
}
};

START_TEST_DEFINE_PARENT(TestDeployManagerGetCtypesServices, TestDeployManager)
void CheckCtypes() {
    TString res = Controller->SendCommandToDeployManager("ctypes");
    NJson::TJsonValue result;
    TStringInput si(res);
    if (!NJson::ReadJsonTree(&si, &result))
        ythrow yexception() << "incorrect json from ctypes: " << res;
    if (result.GetArray().size() != 1)
        ythrow yexception() << "incorrect count of ctypes (must be 1)" << res;
    if (result[0].GetString() != "sbtests")
        ythrow yexception() << "invalid ctypes got: " << res << ", must be [\"sbtests\"]";
}
void CheckServices() {
    TString res = Controller->SendCommandToDeployManager("get_services?fill_slots=true");
    NJson::TJsonValue result, result_expected;
    TStringInput si(res);
    if (!NJson::ReadJsonTree(&si, &result))
        ythrow yexception() << "incorrect json from get_services: " << res;
    if (!result.IsMap())
        ythrow yexception() << "result is not map: " << res;
    TString res_exp = "{\"sbtests\":{\"indexerproxy\":{},\"intsearch\":{},\"metaservice\":{},\"deploy_manager\":{\"deploy_manager\":[\"";
    res_exp += Controller->GetConfig().DeployManager.Host + ":" + ToString(Controller->GetConfig().DeployManager.Port);
    res_exp += "\"]},\"rtyserver\" :{\"newservice\":[\"host1:10015\",\"host2:10015\"],\"tests\" :[\"host1:10005\",\"host2:10005\"]},\"searchproxy\" :{}},\"unused\":[\"unused\"]}";
    TStringInput si_exp(res_exp);
    if (!NJson::ReadJsonTree(&si_exp, &result_expected))
        ythrow yexception() << "internal error";
    if (result != result_expected)
        ythrow yexception() << "result differs from expected: " << res << ", must be " << res_exp;
}
bool Run() override {
    Controller->UploadFileToDeployManager(GetRunDir() + "/copy/searchmap_two_services", "/common/sbtests/cluster.meta");
    CheckCtypes();
    CheckServices();
    return true;
}
};

START_TEST_DEFINE_PARENT(TestDeployManagerEndPointSets, TestDeployManager)
/*
void CheckSearchMap() {
    NDaemonController::TControllerAgent agent(
        Controller->GetConfig().Searcher.Host, Controller->GetConfig().Searcher.Port + 3);
    TString reply;
    agent.ExecuteCommand("?command=get_file&filename=searchmap.json", reply, 5000, 2, "");
    NJson::TJsonValue result;
    TStringInput si(reply);
    if (!NJson::ReadJsonTree(&si, &result))
        ythrow yexception() << "incorrect json from get_file: " << reply;
    DEBUG_LOG << result["result"].GetString();
}*/

void CheckSearchMapStorage() {
    TString clusterText = Controller->SendCommandToDeployManager("process_storage?root=/&path=/common/sbtests/cluster.meta&action=get&download=yes");
    DEBUG_LOG << "Cluster: \n" << clusterText << Endl;
    NSaas::TClusterConst cluster;
    cluster.Deserialize(clusterText);
}
bool Run() override {
    UploadCommon();
    Controller->UploadFileToDeployManager(GetRunDir() + "/copy/cluster_eps.meta", "/common/sbtests/cluster.meta");
  //  DeploySp();
    CheckSearchMapStorage();
    Controller->SendCommandToDeployManager("add_endpointsets?eps=vla@ep2-sh1:0-35533,vla@ep2-sh2:35534-65533&ctype=sbtests&service=newservice");
    CheckSearchMapStorage();
    Controller->SendCommandToDeployManager("release_endpointsets?action=release&eps=vla@ep2-sh1&ctype=sbtests&service=newservice");
    CheckSearchMapStorage();
    return true;
}
};

START_TEST_DEFINE_PARENT(TestDeployManagerExternalCluster, TestDeployManager)
void CheckMap(const TString& res, const TString& service, const TString& sourceMustHave) {
    NJson::TJsonValue result;
    TStringInput si(res);
    if (!NJson::ReadJsonTree(&si, &result))
        ythrow yexception() << "incorrect json from get_cluster_map: " << res;
    const NJson::TJsonValue* sources = result.GetValueByPath("cluster." + service + ".config_types.default.sources", '.');
    if (!sources)
        ythrow yexception() << "section 'cluster." << service << ".config_types.default.sources' not found, answer: " << res;
    if (!sources->GetValueByPath(sourceMustHave, '.'))
        ythrow yexception() << "not found host '" << sourceMustHave << "' in 'sources', answer: " << res;
}
bool Run() override {
    Controller->UploadFileToDeployManager(GetRunDir() + "/copy/searchmap_two_services", "/common/sbtests/cluster.meta");
    TFileInput fi(GetRunDir() + "/copy/searchmap_for_inverse.json");
    TString smapText = fi.ReadAll();

    Controller->SendCommandToDeployManager(
        "external_cluster?service=newservice&ctype=sbtests&action=set&source=post", 0, smapText);
    Controller->SendCommandToDeployManager(
        "external_cluster?service=killerfi4aservice&ctype=sbtests&action=set&source=post", 0, smapText);

    TString res = Controller->SendCommandToDeployManager("get_cluster_map?service=tests&ctype=sbtests");
    CheckMap(res, "tests", "0-35533.local.host1:10005");
    res = Controller->SendCommandToDeployManager("get_cluster_map?service=newservice&ctype=sbtests");
    CheckMap(res, "newservice", "35534-65533.local.host4:7300");
    res = Controller->SendCommandToDeployManager("get_cluster_map?service=killerfi4aservice&ctype=sbtests");
    CheckMap(res, "killerfi4aservice", "0-35533.local.host5:27855");
    return true;
}
};

START_TEST_DEFINE_PARENT(TestDeployManagerSecretKey, TestDeployManager)
void CheckValue(TString res, TString path, TString value) {
    NJson::TJsonValue result;
    TStringInput si(res);
    if (!NJson::ReadJsonTree(&si, &result))
        ythrow yexception() << "incorrect json from secret_key: " << res;
    const NJson::TJsonValue* got_value = result.GetValueByPath(path, '.');
    if (!got_value || got_value->GetString() != value)
        ythrow yexception() << "incorrect value, must be " << value << ", got " << (got_value ? got_value->GetString() : "absent");
}
bool Run() override {
    UploadCommon();
    const TString tests_key = "bfc0e42a74f3d19b8bb8e0fc9c620055";
    const TString newserv_key = "3162d8ec7cc52a80fc891440b0127910";
    Controller->UploadFileToDeployManager(GetRunDir() + "/copy/searchmap_two_services", "/common/sbtests/cluster.meta");
    TString res;

    res = Controller->SendCommandToDeployManager("secret_key");
    CheckValue(res, "sbtests.newservice.json_to_rty", newserv_key);

    res = Controller->SendCommandToDeployManager("secret_key?service=tests");
    CheckValue(res, "sbtests.tests.json_to_rty", tests_key);

    res = Controller->SendCommandToDeployManager("secret_key?service=newservice&ctype=sbtests");
    CheckValue(res, "sbtests.newservice.json_to_rty", newserv_key);

    res = Controller->SendCommandToDeployManager("secret_key?ctype=sbtests");
    CheckValue(res, "sbtests.tests.json_to_rty", tests_key);

    res = Controller->SendCommandToDeployManager("secret_key?key=" + tests_key);
    CheckValue(res, "sbtests.tests.adapter", "json_to_rty");
    return true;
}
};

START_TEST_DEFINE_PARENT(TestDeployManagerClusterMap, TestDeployManager)
void CheckMap(TString res) {
    NJson::TJsonValue result;
    TStringInput si(res);
    if (!NJson::ReadJsonTree(&si, &result))
        ythrow yexception() << "incorrect json from get_cluster_map: " << res;
    const NJson::TJsonValue* sources = result.GetValueByPath("cluster.tests.config_types.default.sources", '.');
    if (!sources)
        ythrow yexception() << "section 'cluster.tests.config_types.default.sources' not found, answer: " << res;
    if (!sources->GetValueByPath("0-35533.local.host1:10005", '.'))
        ythrow yexception() << "not found host '0-35533.local.host1:10005' in 'sources', answer: " << res;
    if (!sources->GetValueByPath("35534-65533.local.host2:10005", '.'))
        ythrow yexception() << "not found host '35534-65533.local.host2:10005' in 'sources', answer: " << res;
}
bool Run() override {
    Controller->UploadFileToDeployManager(GetRunDir() + "/copy/searchmap_two_services", "/common/sbtests/cluster.meta");
    TString res = Controller->SendCommandToDeployManager("get_cluster_map?service=tests&ctype=sbtests");
    CheckMap(res);
    return true;
}
};

START_TEST_DEFINE_PARENT(TestDeployManagerGetUsedSlots, TestDeployManager)
TSet<TString> BackendSlotslist() {
    TSet<TString> result;
    for (size_t i = 0; i < Controller->GetConfig().Controllers.size(); ++i) {
        result.insert(Controller->GetConfig().Controllers[i].Host + ":" + ToString(Controller->GetConfig().Controllers[i].Port - 3));
    }
    return result;
}

bool CheckUnusedSlotslist(TString query, const TSet<TString>& slots, const TString& ctype = "sbtests", const TString& dc = "local") {
    DEBUG_LOG << "checking " << query << ", must be: " << JoinStrings(slots.begin(), slots.end(), ",") << Endl;
    try {
        TString res = Controller->SendCommandToDeployManager("free_slots?" + query);
        return CheckCommonSlotsStructure(res, ctype, dc, 0, 10000, &slots);
    } catch (...) {
        FAIL_LOG("%s", CurrentExceptionMessage().data());
    }
    return true;
}

bool CheckUsingSlotslist(TString query, const TSet<TString>& slots, const TString& ctype = "sbtests", const TString& dc = "local") {
    DEBUG_LOG << "checking " << query << ", must be: " << JoinStrings(slots.begin(), slots.end(), ",") << Endl;
    try {
        TString res = Controller->SendCommandToDeployManager("using_slots?" + query);
        return CheckUsingSlotsStructure(res, ctype, dc, 0, 10000, &slots);
    } catch (...) {
        FAIL_LOG("%s", CurrentExceptionMessage().data());
    }
    return true;
}

bool Run() override {
    TString res;
    Controller->UploadFileToDeployManager(GetRunDir() + "/copy/searchmap_two_services", "/common/sbtests/cluster.meta");

    CHECK_TEST_TRUE(CheckUsingSlotslist("service=*&ctype=sbtests", TSet<TString>({"host1:10005","host1:10015","host2:10005","host2:10015"})));

    CHECK_TEST_TRUE(CheckUsingSlotslist("service=tests&ctype=sbtests", TSet<TString>({"host1:10005","host2:10005"})));
    CHECK_TEST_TRUE(CheckUsingSlotslist("service=newservice&ctype=sbtests", TSet<TString>({"host1:10015","host2:10015"})));

    auto spServers = TSet<TString>({Controller->GetConfig().Searcher.Host + ":" + ToString(Controller->GetConfig().Searcher.Port)});
    CHECK_TEST_TRUE(CheckUsingSlotslist("ctype=sbtests&service=searchproxy&service_type=searchproxy", spServers));
    CHECK_TEST_TRUE(CheckUsingSlotslist("ctype=sbtests&service=*&service_type=searchproxy", spServers));

    CHECK_TEST_TRUE(CheckUsingSlotslist("ctype=sbtests&service=tests&service_type=searchproxy", spServers));

    //unused //use cluster.cfg
    CheckUnusedSlotslist("service=tests&ctype=sbtests", BackendSlotslist());
    CheckUnusedSlotslist("service=*&ctype=sbtests", BackendSlotslist());
    CheckUnusedSlotslist("service=newservice&ctype=sbtests", BackendSlotslist());

    return true;
}
};

START_TEST_DEFINE_PARENT(TestDeployManagerGetSlots, TestDeployManager)

bool Run() override {
    TString res;
    res = Controller->SendCommandToDeployManager("slots_by_tag?tag=saas_yp_cloud_prestable");
    CHECK_TEST_TRUE(CheckSlotsByTagStructure(res, "local", 10));
    Controller->UploadDataToDeployManager("{\"rtyserver\":\"saas_yp_cloud_prestable\"}", "/common/sbtagtest/tags.info");
    Controller->UploadDataToDeployManager("", "/common/sbtagtest/cluster.meta");
    Controller->SendCommandToDeployManager("?command=restart", 3);
    res = Controller->SendCommandToDeployManager("using_slots?service=*&ctype=sbtagtest&service_type=rtyserver");
    CHECK_TEST_TRUE(CheckUsingSlotsStructure(res, "sbtagtest", "", 0, 0));
    res = Controller->SendCommandToDeployManager("free_slots?service=*&ctype=sbtagtest&service_type=rtyserver");
    CHECK_TEST_TRUE(CheckCommonSlotsStructure(res, "sbtagtest", "", 10, 10000));
    res = Controller->SendCommandToDeployManager("available_slots?service=*&ctype=sbtagtest&service_type=rtyserver");
    CHECK_TEST_TRUE(CheckCommonSlotsStructure(res, "sbtagtest", "", 10, 10000));
    Controller->SendCommandToDeployManager("process_storage?action=rm&path=/common/sbtagtest/tags.info");
    return true;
}
};

START_TEST_DEFINE_PARENT(TestDeployManagerGetFiles, TestDeployManager)

bool CheckFilesByServicesJson(const TString& res) {
    NJson::TJsonValue result;
    if (!NJson::ReadJsonFastTree(res, &result)) {
        ythrow yexception() << "incorrect json from get_files: " << res;
    }

    if (!result.IsMap()) {
        ythrow yexception() << "result must be a map: " << res;
    }
    if (!result.Has("files")) {
        ythrow yexception() << "must has 'files' key in result: " << res;
    }
    if (!result.Has("services")) {
        ythrow yexception() << "must has 'services' key in result: " << res;
    }

    result = result["services"];

    if (!result.IsMap()) {
        ythrow yexception() << "services must be in map in result: " << res;
    }

    int isServiceWithTagsExist = false;

    for (const auto& service : result.GetMap()) {
        if (!service.second.IsMap()) {
            ythrow yexception() << "service desription must be a map in result: " << res;
        }

        if (!service.second["tags"].IsArray()) {
            ythrow yexception() << "tags must be an array in result: " << res;
        }

        if (!service.second["files"].IsMap()) {
            ythrow yexception() << "files must be an array in service description: " << res;
        }

        if (service.second["tags"].GetArray().size()) {
            isServiceWithTagsExist = true;
        }

        for (const auto& file : service.second["files"].GetMap()) {
            if (!file.second.IsMap()) {
                ythrow yexception() << "sandbox_files and url_files must be in map in result: " << res;
            }
            if (!file.second.Has("file_type") || !file.second["file_type"].IsString()) {
                ythrow yexception() << "file_type is incorrect in service's files description: " << res;
            }
            if (!file.second.Has("local_path") || !file.second["local_path"].IsString()) {
                ythrow yexception() << "file_type is incorrect in service's files description: " << res;
            }
        }
    }

    if (!isServiceWithTagsExist) {
        ythrow yexception() << "at least one service must has tags in result: " << result;
    }
    return true;
}

bool Run() override {
    TString res;
    Controller->UploadFileToDeployManager(GetRunDir() + "/copy/searchmap_two_services", "/common/sbtests/cluster.meta");
    Controller->UploadDataToDeployManager("{\"sbtests\":\"ALL_SAAS_CLOUD_BASE_R1\"}", "/configs/newservice/tags.info");
    Controller->UploadDataToDeployManager("{\"sbtests\":\"saas_personal_clck_prestable\"}", "/configs/tests/tags.info");
    res = Controller->SendCommandToDeployManager("get_files?ctype=sbtests&service_type=rtyserver");
    CHECK_TEST_TRUE(CheckFilesByServicesJson(res));
    return true;
}
};

START_TEST_DEFINE_PARENT(TestDeployManagerGetFilesWithEmptyCtype, TestDeployManager)
bool Run() override {
    TString res;
    Controller->UploadFileToDeployManager(GetRunDir() + "/copy/searchmap_two_services", "/common/sbtests/cluster.meta");
    try {
        Controller->SendCommandToDeployManager("get_files");
    } catch (...) {
    }
    return true;
}
};

START_TEST_DEFINE_PARENT(TestDeployManagerLockerInfo, TestDeployManager)
bool Run() override {
    TString res;
    Controller->SendCommandToDeployManager("locker?action=acquire&lock_name=/configs/unused&lock_info=lock_first");
    res = Controller->SendCommandToDeployManager("locker?action=info&lock_name=/configs/unused");
    CHECK_TEST_FAILED(res.find("lock_first") == TString::npos, "not corresponding lock info");

    MUST_BE_BROKEN(Controller->SendCommandToDeployManager("locker?action=acquire&lock_name=/configs/unused&lock_info=lock_second"));

    res = Controller->SendCommandToDeployManager("locker?action=acquire&lock_name=/configs/unused&lock_info=lock_first");
    CHECK_TEST_FAILED(res.find("ALREADY LOCKED") == TString::npos, "not 'already locked' message");

    res = Controller->SendCommandToDeployManager("locker?action=info&lock_name=/configs/unused");
    CHECK_TEST_FAILED(res.find("lock_first") == TString::npos, "not corresponding lock info");

    Controller->SendCommandToDeployManager("locker?action=release&lock_name=/configs/unused&lock_info=lock_first");
    res = Controller->SendCommandToDeployManager("locker?action=info&lock_name=/configs/unused");
    CHECK_TEST_FAILED(res.find("CAN'T READ") == TString::npos, "incorrect status in answer");

    res = Controller->SendCommandToDeployManager("locker?action=info");
    return true;
}
};

START_TEST_DEFINE_PARENT(TestDeployManagerStorage, TestDeployManager)
void LooksLikeDiff(TString diff) {
    TVector<TString> lines = SplitString(diff, "\n");
    int samplesFound = 0;
    for (auto l : lines) {
        if (l[0] == '+' && l.find("TwoStepQuery") != TString::npos)
            ++samplesFound;
        if (l[0] == '-' && l.find("#include") != TString::npos)
            ++samplesFound;
    }
    if (samplesFound != 2)
        ythrow yexception() << "not found some lines in diff: " << diff;
}
bool Run() override {
    TString res;

    Controller->SendCommandToDeployManager("process_storage?action=make&path=/some/path");
    Controller->SendCommandToDeployManager("process_storage?action=get&path=/some/path");
    MUST_BE_BROKEN(Controller->SendCommandToDeployManager("process_storage?action=get&path=/some/fake/path"));

    Controller->UploadFileToDeployManager(GetRunDir() + "/copy/searchproxy/searchproxy.conf", "/some/path/file.conf");
    res = Controller->SendCommandToDeployManager("process_storage?action=version&path=/some/path/file.conf");
    i64 ver1 = FromString<i64>(res);
    Controller->SendCommandToDeployManager("process_storage?action=get&path=/some/path");
    TString file_origin = Controller->SendCommandToDeployManager("process_storage?action=get&path=/some/path/file.conf&version=-1");

    Controller->UploadFileToDeployManager(GetRunDir() + "/copy/searchproxy/query-language", "/some/path/file.conf");
    res = Controller->SendCommandToDeployManager("process_storage?action=version&path=/some/path/file.conf");
    i64 ver2 = FromString<i64>(res);

    res = Controller->SendCommandToDeployManager("process_storage?action=cmp&path=/some/path/file.conf&version1=" + ToString(ver1) + "&version2=" + ToString(ver2));
    LooksLikeDiff(res);
    res = Controller->SendCommandToDeployManager("process_storage?action=cmp&path=/some/path/file.conf&version1=" + ToString(ver2) + "&version2=-1");
    MUST_BE_BROKEN(Controller->SendCommandToDeployManager("process_storage?action=cmp&path=/some/path/file.conf&version1=" + ToString(ver2) + "&version2=-3"));
    MUST_BE_BROKEN(Controller->SendCommandToDeployManager("process_storage?action=cmp&path=/some/path/file.conf&version2=-1"));

    res = Controller->SendCommandToDeployManager("process_storage?action=get&path=/some/path/file.conf&version=" + ToString(ver1));
    CHECK_TEST_FAILED(res != file_origin, "incorrect content with version");
    Controller->SendCommandToDeployManager("process_storage?action=restore&path=/some/path/file.conf&version=" + ToString(ver1));
    res = Controller->SendCommandToDeployManager("process_storage?action=get&path=/some/path/file.conf");
    CHECK_TEST_FAILED(res != file_origin, "incorrect content after restore");
    Controller->SendCommandToDeployManager("process_storage?action=get&path=/");

    return true;
}
};

START_TEST_DEFINE_PARENT(TestDeployManagerAbort, TestDeployManager)
void InitCluster() override {
    UploadCommon();
    UploadService("tests");
    ConfigureCluster(1, 1, NSaas::UrlHash, "rtyserver", "tests");
}
bool Run() override {
    NDaemonController::TRestartAction ra(NDaemonController::apStartAndWait, true);
    NDaemonController::TControllerAgent agent(Controller->GetConfig().Controllers[0].Host, Controller->GetConfig().Controllers[0].Port);
    if (!agent.ExecuteAction(ra))
        ythrow yexception() << "Errors while execute restart" << Endl;
    if (ra.IsFinished() && ra.IsFailed())
        ythrow yexception() << "Error in restart action: " << ra.GetInfo();
    NDaemonController::TRestartAction raTimeout(NDaemonController::apStartAndWait, false, false, TDuration::Seconds(1));
    raTimeout.SetAdditionParams("sleep=60s");
    TInstant start = Now();
    if (!agent.ExecuteAction(raTimeout))
        ythrow yexception() << "Errors while execute restart" << Endl;
    if (raTimeout.IsFinished() && raTimeout.IsFailed())
        ythrow yexception() << "Error in restart action: " << raTimeout.GetInfo();
    if (Now() - start > TDuration::Seconds(30))
        ythrow yexception() << "restart too long";
    return true;
}
};

START_TEST_DEFINE_PARENT(TestDeployManagerSearchproxyConfig, TestDeployManager)
void InitCluster() override {
    UploadCommon();
    UploadService("tests");
    ConfigureCluster(1, 1, NSaas::UrlHash, "rtyserver", "tests");
    Controller->UploadDataToDeployManager("<Service>\nName: meta-service\n</Service>", "/configs/meta-service/searchproxy-meta-service.conf");
    NSearchMapParser::TMetaService ms;
    ms.Name = "meta-service";
    ms.Components.push_back(NSearchMapParser::TMetaComponent("tests"));
    NDaemonController::TSimpleSearchmapModifAction addService(TVector<TString>(), "sbtests", ms.Name, "add_service", "metaservice");
    NDaemonController::TSimpleSearchmapModifAction editService(ms.SerializeToJson().GetStringRobust(), "sbtests", ms.Name, "metaservice");
    Controller->ExecuteActionOnDeployManager(addService);
    Controller->ExecuteActionOnDeployManager(editService);
}
void CheckService(const TString& service) {
    TFsPath path = GetRunDir() + "/sp/configs/searchproxy-" + service + ".conf";
    if (!path.Exists())
        ythrow yexception() << "there is no " << path.GetName();
    if (TUnbufferedFileInput(path).ReadAll().find("invalid") != TString::npos)
        ythrow yexception() << "invalid content of " << path.GetName();
}
bool Run() override {
    DeployBe("tests");
    DeployMetaservice("meta-service");
    Controller->UploadDataToDeployManager("<Service>\nName: meta-service-invalid\n</Service>", "/configs/meta-service/searchproxy-meta-service.conf");
    Controller->UploadDataToDeployManager("<Service>\nName: tests-service-invalid\n</Service>", "/configs/tests/searchproxy-tests.conf");
    DeploySp();
    CheckService("tests");
    CheckService("meta-service");
    return true;
}
};

START_TEST_DEFINE_PARENT(TestDeployManagerSearchproxyConfigCurrent, TestDeployManager)
void InitCluster() override {
    UploadCommon();
    UploadService("tests");
    ConfigureCluster(1, 1, NSaas::UrlHash, "rtyserver", "tests");
    DeploySp();
    DeployIp();
}
void CheckService(const TString& service) {
    TFsPath path = GetRunDir() + "/sp/configs/searchproxy-" + service + ".conf";
    if (!path.Exists())
        ythrow yexception() << "there is no " << path.GetName();
    if (TUnbufferedFileInput(path).ReadAll().find("invalid") != TString::npos)
        ythrow yexception() << "invalid content of " << path.GetName();
}
bool Run() override {
    DeployBe("tests");
    DeploySp();

    TVector<NRTYServer::TMessage> messages;
    TSet<std::pair<ui64, TString> > deleted;
    GenerateInput(messages, 1, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed());
    {
        IndexMessages(messages, DISK, 1);
        ReopenIndexers();
        IndexMessages(messages, DISK, 1);
        ReopenIndexers();
    }
    TVector<TDocSearchInfo> results;
    QuerySearch("url:\"*\"&service=tests&" + GetAllKps(messages), results);
    CHECK_TEST_EQ(results.size(), 2);
    Controller->UploadDataToDeployManager("<Service>\nName: tests\nMergeOptions:SkipSameDocids=true\n</Service>", "/configs/tests/searchproxy-tests.conf");
    DeploySp("tests");
    QuerySearch("url:\"*\"&service=tests&" + GetAllKps(messages), results);
    CHECK_TEST_EQ(results.size(), 1);
    DeploySp();
    QuerySearch("url:\"*\"&service=tests&" + GetAllKps(messages), results);
    CHECK_TEST_EQ(results.size(), 1);
    DeployBe("tests");
    DeploySp();
    QuerySearch("url:\"*\"&service=tests&" + GetAllKps(messages), results);
    CHECK_TEST_EQ(results.size(), 1);

    return true;
}
};

START_TEST_DEFINE_PARENT(TestDeployManagerIndexerproxyConfigCurrent, TestDeployManager)
void InitCluster() override {
    UploadCommon();
    UploadService("tests");
    ConfigureCluster(1, 1, NSaas::UrlHash, "rtyserver", "tests");
    DeploySp();
    DeployIp();
}
bool Run() override {
    DeployBe("tests");
    DeploySp();
    DeployIp();

    TVector<NRTYServer::TMessage> messages0;
    TVector<NRTYServer::TMessage> messages1;
    TVector<NRTYServer::TMessage> messages2;
    TVector<NRTYServer::TMessage> messages3;
    TSet<std::pair<ui64, TString> > deleted;
    GenerateInput(messages0, 1, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed());
    GenerateInput(messages1, 1, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed());
    GenerateInput(messages2, 1, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed());
    GenerateInput(messages3, 1, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed());
    {
        DeployBe("tests", 1);
        DeployIp();
        TSearchMessagesContext context;
        context.CountResults.insert(1);
        IndexMessages(messages0, REALTIME, 1);
        CheckSearchResults(messages0, context);
    }
    {
        DeployBe("tests", 10);
        DeployIp();
        TSearchMessagesContext context;
        context.CountResults.insert(0);
        IndexMessages(messages1, REALTIME, 1);
        CheckSearchResults(messages1, context);
    }
    {
        DeployBe("tests", 1);
        DeployIp("tests");
        TSearchMessagesContext context;
        context.CountResults.insert(1);
        IndexMessages(messages2, REALTIME, 1);
        CheckSearchResults(messages2, context);
    }
    {
        DeployBe("tests", 10);
        DeployIp("tests");
        TSearchMessagesContext context;
        context.CountResults.insert(0);
        IndexMessages(messages3, REALTIME, 1);
        CheckSearchResults(messages3, context);
    }

    return true;
}
};

START_TEST_DEFINE_PARENT(TestDeployManagerCancelTask, TestDeployManager)
struct TProcessor: public IMessageProcessor {
    TManualEvent Event;
    TProcessor() {
        Event.Signal();
        RegisterGlobalMessageProcessor(this);
    }
    ~TProcessor() {
        UnregisterGlobalMessageProcessor(this);
    }
    bool Process(IMessage* message) override {
        TMessageBeforeDMProcessTask* msg = dynamic_cast<TMessageBeforeDMProcessTask*>(message);
        if (msg) {
            Event.Wait();
            return true;
        }
        return false;
    }
    TString Name() const override {
        return "TestDeployManagerCancelTask TProcessor";
    }
public:
    void PauseDeploy() {
        Event.Reset();
    }
    void ContinueDeploy() {
        Event.Signal();
    }
};
TProcessor Processor;
void InitCluster() override {
    UploadCommon();
    UploadService("tests");
    ConfigureCluster(1, 1, NSaas::UrlHash, "rtyserver", "tests");
}
bool Run() override {
    Processor.PauseDeploy();
    TString taskId = Controller->SendCommandToDeployManager("/deploy?ctype=sbtests&service=tests&on_problem=ignore&service_type=rtyserver");
    taskId = taskId.substr(0, taskId.size() - 1); // remove trailing line feed char
    CGIEscape(taskId);
    bool spoiled = Spoil(taskId);
    TString resp = Controller->SendCommandToDeployManager("/control_task?action=cancel&task_id=" + taskId);
    CHECK_TEST_EQ(resp, "OK");
    Processor.ContinueDeploy();
    TInstant timeStart = Now();
    while (Now() - timeStart < TDuration::Seconds(120)) {
        try {
            TString info = Controller->SendCommandToDeployManager("deploy_info?id=" + taskId);
            CHECK_TEST_EQ(spoiled, false);
            DEBUG_LOG << info << Endl;
            NJson::TJsonValue jsonInfo;
            CHECK_WITH_LOG(NJson::ReadJsonFastTree(info, &jsonInfo));
            if (jsonInfo["is_finished"].GetBoolean()) {
                break;
            } else {
                sleep(1);
            }
        } catch (...) {
            CHECK_TEST_EQ(spoiled, true);
            return true;
        }
    }
    TString fails = Controller->SendCommandToDeployManager("process_storage?path=failed_tasks/" + taskId + "&download=yes&action=get");
    DEBUG_LOG << fails << Endl;
    CHECK_TEST_NEQ(fails, "");
    CHECK_TEST_NEQ(fails.find("SUSPENDED"), TString::npos);
    return true;
}
virtual bool Spoil(const TString&) {
    return false;
}
};

START_TEST_DEFINE_PARENT(TestDeployManagerCancelSpoiledTask, TTestDeployManagerCancelTaskCaseClass)
bool Spoil(const TString& taskId) override {
    Controller->UploadDataToDeployManager("blah-blah-blah", "/cluster_tasks/" + taskId);
    return true;
}
};

START_TEST_DEFINE_PARENT(TestDeployManagerRemoveExpiredTask, TestDeployManager)
void InitCluster() override {
    UploadCommon();
    UploadService("tests");
    DeployIp();
    Sleep(TDuration::Seconds(50));
    DeploySp();
}

bool Run() override {
    NDaemonController::TControllerAgent agent(Controller->GetConfig().DeployManager.Host, Controller->GetConfig().DeployManager.Port + 3);
    TString reply;
    TConfigFields configDiff;
    configDiff["Storage.ClusterTaskLifetimeSec"] = "100";
    TString command = "?command=set_config&fields=" + configDiff.Serialize() + "&prefix=DeployManager&reread_config=yes";
    agent.ExecuteCommand(command, reply, 3000, 1, "");

    const TString pingCommand = "?command=get_status";
    agent.ExecuteCommand(pingCommand, reply, 3000, 1, "");
    DEBUG_LOG << "Status reply: " << reply << Endl;

    {
        TString res = Controller->SendCommandToDeployManager("process_storage?action=get&path=/cluster_tasks/");
        DEBUG_LOG << res << Endl;
        NJson::TJsonValue json;
        TStringStream ss(res);
        NJson::ReadJsonTree(&ss, &json);
        CHECK_TEST_EQ(json.GetMap().at("files").GetArray().size(), 2);
    }
    Sleep(TDuration::Seconds(60));
    NDaemonController::TRestartAction ra(NDaemonController::apStartAndWait);
    ra.SetReReadConfig(false);
    if (!agent.ExecuteAction(ra))
        ythrow yexception() << "cannot restart deploy_manager";

    {
        TString res = Controller->SendCommandToDeployManager("process_storage?action=get&path=/cluster_tasks/");
        DEBUG_LOG << res << Endl;
        NJson::TJsonValue json;
        TStringStream ss(res);
        NJson::ReadJsonTree(&ss, &json);
        CHECK_TEST_EQ(json.GetMap().at("files").GetArray().size(), 1);
    }
    return true;
}
};
