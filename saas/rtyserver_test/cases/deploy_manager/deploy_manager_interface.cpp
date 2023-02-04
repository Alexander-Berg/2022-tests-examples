#include "deploy_manager.h"
#include <saas/deploy_manager/meta/cluster.h>
#include <saas/deploy_manager/scripts/deploy/action.h>
#include <library/cpp/json/json_reader.h>

START_TEST_DEFINE_PARENT(TestDeployManagerDashboardScript, TestDeployManager)

NJson::TJsonValue GetDMReply(const TString& request) const {
    TString res = Controller->SendCommandToDeployManager(request);
    DEBUG_LOG << res << Endl;
    TStringInput inp(res);
    NJson::TJsonValue parsedRes;
    if (!NJson::ReadJsonTree(&inp, &parsedRes))
        ythrow yexception() << "incorrect json from request: " << res;
    return parsedRes;
}

TSet<TString> GetBackendsSlots() const {
    TSet<TString> slots;
    for (size_t i = 0; i < Controller->GetConfig().Controllers.size(); ++i) {
        TString slot = Controller->GetConfig().Controllers[i].Host + ":" + ToString(Controller->GetConfig().Controllers[i].Port - 3);
        slots.insert(slot);
    }
    return slots;
}

static TString BuildRequest(const TString& service, const TString& serviceType, const bool allNeighbors) {
    static const TString ctype = "sbtests";
    return "dashboard?ctype=" + ctype +
        "&filter=result.controller_status%2Cid" +
        "&groupings=$datacenter$;host%28%29&min_ok_uptime=600s" +
        "&service=" + service + "&service_type=" + serviceType + "&slots_filters=neighbors" + (allNeighbors ? ",all_neighbors" : "");
}

bool CheckClusterInfoGrouping(const TString& service, const TString& serviceType, const TSet<TString>& slots, const bool allNeighbors = false) const {
    const NJson::TJsonValue value = GetDMReply(BuildRequest(service, serviceType, allNeighbors));
    CHECK_TEST_TRUE(value.IsArray());
    const NJson::TJsonValue& hostsGroup = value[0];
    CHECK_TEST_TRUE(hostsGroup.IsMap());
    CHECK_TEST_TRUE(hostsGroup.Has("hosts"));
    CHECK_TEST_TRUE(hostsGroup["hosts"].IsArray());
    CHECK_TEST_EQ(hostsGroup["hosts"].GetArray().size(), 1);
    CHECK_TEST_TRUE(hostsGroup["hosts"][0].IsMap());
    CHECK_TEST_TRUE(hostsGroup["hosts"][0].Has("slots"));
    const NJson::TJsonValue& slotsGroup = hostsGroup["hosts"][0]["slots"];
    CHECK_TEST_TRUE(slotsGroup.IsArray());
    CHECK_TEST_EQ(slotsGroup.GetArray().size(), slots.size());
    DEBUG_LOG << "Slots: " << JoinStrings(slots.cbegin(), slots.cend(), ",") << Endl;
    for (const NJson::TJsonValue& v : slotsGroup.GetArray()) {
        DEBUG_LOG << "map slot: " << v["slot"].GetString() << Endl;
        CHECK_TEST_TRUE(slots.contains(v["slot"].GetString()));
    }
    return true;
}

bool CheckRtyNeigbors() const {
    const TSet<TString> slots = GetBackendsSlots();
    CHECK_TEST_TRUE(CheckClusterInfoGrouping("tests", "rtyserver", slots));
    return true;
}

bool CheckIpNeigbors() const {
    TSet<TString> slots;
    slots.insert(Controller->GetConfig().Indexer.GetString());
    CHECK_TEST_TRUE(CheckClusterInfoGrouping("indexerproxy", "indexerproxy", slots));
    return true;
}


bool CheckCombinedNeighbors() {
    TSet<TString> slots = GetBackendsSlots();
    slots.insert(Controller->GetConfig().Indexer.GetString());
    slots.insert(Controller->GetConfig().DeployManager.Host + ":" + ToString(Controller->GetConfig().DeployManager.Port));
    slots.insert(Controller->GetConfig().Searcher.GetString());
    CHECK_TEST_TRUE(CheckClusterInfoGrouping("tests", "rtyserver", slots, true));
    return true;
}


bool Run() override {
    UploadCommon();
    UploadService("tests");
    ConfigureCluster(2, 1, NSaas::UrlHash, "rtyserver", "tests");
    DeployBe("tests");

    CHECK_TEST_TRUE(CheckRtyNeigbors());
    CHECK_TEST_TRUE(CheckIpNeigbors());
    CHECK_TEST_TRUE(CheckCombinedNeighbors());
    return true;
}
};

