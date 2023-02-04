#include "scenario.h"

#include <infra/yp_service_discovery/functional_tests/common/common.h>
#include <infra/yp_service_discovery/functional_tests/common/daemon_runner.h>

#include <yp/cpp/yp/client.h>
#include <infra/libs/logger/test_common.h>

#include <library/cpp/json/json_value.h>
#include <library/cpp/json/json_writer.h>
#include <library/cpp/testing/unittest/registar.h>

#include <util/generic/xrange.h>
#include <util/stream/file.h>
#include <util/string/builder.h>

using namespace NYP;
using namespace NServiceDiscovery;

namespace {
    constexpr TStringBuf DEFAULT_UPDATING_FREQUENCY = "1ms";
    constexpr TStringBuf DEFAULT_TIMEOUT = "10s";
    const TString PATH_TO_CONFIG = "patched_config.json";

    void PatchConfig(const TString& address) {
        auto config = GetDefaultConfig();

        NJson::TJsonValue cluster;
        cluster["Name"] = TEST_CLUSTER_NAME;
        cluster["Address"] = address;
        cluster["EnableSsl"] = false;
        cluster["UpdatingFrequency"] = DEFAULT_UPDATING_FREQUENCY;
        cluster["Timeout"] = DEFAULT_TIMEOUT;
        cluster["GetUpdatesMode"] = "WATCH_UPDATES";

        config["YPEndpointClusterConfigs"] = NJson::JSON_ARRAY;
        config["YPEndpointClusterConfigs"].AppendValue(cluster);

        cluster["GetUpdatesMode"] = "WATCH_SELECTORS";
        config["YPPodClusterConfigs"] = NJson::JSON_ARRAY;
        config["YPPodClusterConfigs"].AppendValue(cluster);

        cluster["GetUpdatesMode"] = "WATCH_SELECTORS";
        config["YPNodeClusterConfigs"] = NJson::JSON_ARRAY;
        config["YPNodeClusterConfigs"].AppendValue(cluster);

        TFileOutput configFile{PATH_TO_CONFIG};
        configFile << WriteJson(config);
    }

    void CreateEndpointSet(NClient::TClientPtr client, const TString& id, const NJson::TJsonValue& labels = {}) {
        NClient::TEndpointSet endpointSet;
        endpointSet.MutableMeta()->set_id(id);
        if (labels.IsDefined()) {
            *endpointSet.MutableLabels() = labels;
        }
        client->CreateObject(endpointSet).GetValueSync();
    }

    void CreateEndpoint(NClient::TClientPtr client, const TString& id, const TString& esid, const NJson::TJsonValue& labels = {}) {
        NClient::TEndpoint endpoint;
        endpoint.MutableMeta()->set_id(id);
        endpoint.MutableMeta()->set_endpoint_set_id(esid);
        if (labels.IsDefined()) {
            *endpoint.MutableLabels() = labels;
        }
        client->CreateObject(endpoint).GetValueSync();
    }

    void CreatePodSet(NClient::TClientPtr client, const TString& id, const NJson::TJsonValue& labels = {}) {
        NClient::TPodSet podSet;
        podSet.MutableMeta()->set_id(id);
        if (labels.IsDefined()) {
            *podSet.MutableLabels() = labels;
        }
        client->CreateObject(podSet).GetValueSync();
    }

    void CreatePod(
        NClient::TClientPtr client
        , const TString& id
        , const TString& psid
        , const NJson::TJsonValue labels = {}
    ) {
        NClient::TPod pod;
        pod.MutableMeta()->set_id(id);
        pod.MutableMeta()->set_pod_set_id(psid);
        if (labels.IsDefined()) {
            *pod.MutableLabels() = labels;
        }

        client->CreateObject(pod).GetValueSync();
    }

    void RemoveEndpoint(NClient::TClientPtr client, const TString& id) {
        client->RemoveObject<NClient::TEndpoint>(id).GetValueSync();
    }

    void RemovePod(NClient::TClientPtr client, const TString& id) {
        client->RemoveObject<NClient::TPod>(id).GetValueSync();
    }

    TString GenerateRequest(const TString& esid, const TString& clientName = "", const TVector<TString>& labelSelectors = {}, const TVector<TString>& esLabelSelectors = {}) {
        NJson::TJsonValue req;
        req["cluster_name"] = TEST_CLUSTER_NAME;
        req["endpoint_set_id"] = esid;
        req["client_name"] = clientName;
        for (const auto& labelSelector : labelSelectors) {
            req["label_selectors"].AppendValue(labelSelector);
        }
        for (const auto& labelSelector : esLabelSelectors) {
            req["endpoint_set_label_selectors"].AppendValue(labelSelector);
        }
        return NJson::WriteJson(req);
    }

    TString GeneratePodRequest(const TString& psid, const TString& clientName = "", const TVector<TString>& podLabelSelectors = {}) {
        NJson::TJsonValue req;
        req["cluster_name"] = TEST_CLUSTER_NAME;
        req["pod_set_id"] = psid;
        req["client_name"] = clientName;
        for (const auto& labelSelector : podLabelSelectors) {
            req["pod_label_selectors"].AppendValue(labelSelector);
        }
        return NJson::WriteJson(req);
    }

    NJson::TJsonValue ParseResponse(const TString& response) {
        TStringStream ss{response};
        return NJson::ReadJsonTree(&ss, /* throwOnError */ true);
    }

    TString GetEndpointId(size_t id) {
        return TStringBuilder() << "id-" << id;
    }

    size_t GetEndpointId(TStringBuf id) {
        id.NextTok('-');
        return FromString<size_t>(id);
    }

}

void RunLocalMasterTests(const TString& address) {
    PatchConfig(address);

    NProtoConfig::TLoadConfigOptions options;
    options.Path = PATH_TO_CONFIG;

    RunDaemon(options);

    UNIT_ASSERT_VALUES_EQUAL(Ping().Content, EXPECTED_PING_RESPONSE);

    NClient::TClientOptions clientOptions;
    clientOptions
        .SetAddress(address)
        .SetEnableSsl(false);

    auto ypClient = NClient::CreateClient(clientOptions);

    const TString esid1 = "esid-1";


    NJson::TJsonValue esLabels;
    esLabels.InsertValue("key1", "value1");
    CreateEndpointSet(ypClient, esid1, esLabels);

    NJson::TJsonValue labels;
    labels.InsertValue("key", "value");
    CreateEndpoint(ypClient, GetEndpointId(1), esid1, labels);

    { // test case 1
        // The UpdatingFrequency of the YpServiceDicovery is 1ms and the SyncDB interval is 10ms
        // so we should get the endpoint in less than 200ms + 40 * 10 ms (just magic const)
        Sleep(TDuration::MilliSeconds(200));

        constexpr size_t MAX_ATTEMPTS = 10;

        bool gotResponse = false;
        bool validData = false;
        for (size_t attemp = 0; attemp < MAX_ATTEMPTS; ++attemp) {
            const auto req = GenerateRequest(esid1, "local-yt-tests", {"key", "non_existent_key"}, {"key1", "non_existent_key"});
            const auto resp = ParseResponse(ResolveEndpointsJson(req).Content);

            if (resp["resolve_status"] == 2) {
                gotResponse = true;

                const auto& endpointSet = resp["endpoint_set"];
                const auto& esid = endpointSet["endpoint_set_id"].GetString();
                const auto& endpoints = endpointSet["endpoints"].GetArray();

                validData = esid == esid1
                    && endpoints.size() == 1
                    && endpoints.front()["id"] == GetEndpointId(1)
                    && endpoints.front()["label_selector_results"].GetArraySafe().size() == 2
                    && endpoints.front()["label_selector_results"][0] == "value"
                    && endpoints.front()["label_selector_results"][1] == "null"
                    && endpointSet["label_selector_results"].GetArraySafe().size() == 2
                    && endpointSet["label_selector_results"][0]  == "value1"
                    && endpointSet["label_selector_results"][1]  == "null";

                break;
            } else {
                Sleep(TDuration::Seconds(1));
            }
        }
        UNIT_ASSERT(gotResponse);
        UNIT_ASSERT(validData);
    }

    RemoveEndpoint(ypClient, GetEndpointId(1));

    { // test case 2
        // The rules are the same as in test case 1
        Sleep(TDuration::MilliSeconds(200));

        constexpr size_t MAX_ATTEMPTS = 10;

        for (size_t attemp = 0; attemp < MAX_ATTEMPTS; ++attemp) {
            const auto req = GenerateRequest(esid1, "local-yt-tests");
            const auto resp = ParseResponse(ResolveEndpointsJson(req).Content);

            if (resp["resolve_status"] == 3) {
                const auto& esid = resp["endpoint_set"]["endpoint_set_id"].GetString();
                const auto& endpoints = resp["endpoint_set"]["endpoints"].GetArray();

                UNIT_ASSERT_VALUES_EQUAL(esid, esid1);
                UNIT_ASSERT(endpoints.empty());
                break;
            } else {
                UNIT_ASSERT(resp["resolve_status"] == 2); // endpoint set exists and non-empty
                Sleep(TDuration::Seconds(1));
            }
        }
    }

    for (size_t i = 0; i < 100; ++i) {
        CreateEndpoint(ypClient, GetEndpointId(i), esid1);
    }

    { // test case 3
        // The rules are the same as in test case 1
        Sleep(TDuration::MilliSeconds(200));

        constexpr size_t MAX_ATTEMPTS = 10;

        bool gotResponse = false;
        bool validData = false;
        for (size_t attemp = 0; attemp < MAX_ATTEMPTS; ++attemp) {
            const auto req = GenerateRequest(esid1, "local-yt-tests");
            const auto resp = ParseResponse(ResolveEndpointsJson(req).Content);

            if (resp.Has("timestamp")) {
                gotResponse = true;

                const auto& esid = resp["endpoint_set"]["endpoint_set_id"].GetString();
                auto endpoints = resp["endpoint_set"]["endpoints"].GetArray();

                Sort(endpoints.begin(), endpoints.end(), [](const auto& lhs, const auto& rhs) { return GetEndpointId(lhs["id"].GetString()) < GetEndpointId(rhs["id"].GetString()); });

                if (endpoints.size() == 100) {
                    validData |= esid == esid1 && AllOf(xrange(100), [&endpoints](size_t i) { return endpoints[i]["id"].GetString() == GetEndpointId(i); });
                }

                if (validData) {
                    break;
                }
            }
            Sleep(TDuration::Seconds(1));
        }
        UNIT_ASSERT(gotResponse);
        UNIT_ASSERT(validData);
    }

    for (size_t i = 0; i < 100; i += 2) {
        RemoveEndpoint(ypClient, GetEndpointId(i));
    }

    { // test case 4
        // The rules are the same as in test case 1
        Sleep(TDuration::MilliSeconds(200));

        constexpr size_t MAX_ATTEMPTS = 10;

        bool gotResponse = false;
        bool validData = false;
        for (size_t attemp = 0; attemp < MAX_ATTEMPTS; ++attemp) {
            const auto req = GenerateRequest(esid1, "local-yt-tests");
            const auto resp = ParseResponse(ResolveEndpointsJson(req).Content);

            if (resp.Has("timestamp")) {
                gotResponse = true;

                const auto& esid = resp["endpoint_set"]["endpoint_set_id"].GetString();
                auto endpoints = resp["endpoint_set"]["endpoints"].GetArray();

                Sort(endpoints.begin(), endpoints.end(), [](const auto& lhs, const auto& rhs) { return GetEndpointId(lhs["id"].GetString()) < GetEndpointId(rhs["id"].GetString()); });

                if (endpoints.size() == 50) {
                    validData |= esid == esid1 && AllOf(xrange(1, 100, 2), [&endpoints](size_t i) { return endpoints[i / 2]["id"].GetString() == GetEndpointId(i); });
                }

                if (validData) {
                    break;
                }
            }
            Sleep(TDuration::Seconds(1));
        }
        UNIT_ASSERT(gotResponse);
        UNIT_ASSERT(validData);
    }

    {
        // test case 5
            // check exception in case of lack of client_name
        Sleep(TDuration::MilliSeconds(200));

        const auto req = GenerateRequest(esid1);
        UNIT_ASSERT_EXCEPTION(ResolveEndpointsJson(req), yexception);
    }

    {
        // test case 6
            // check values in histograms for yp_replica
        constexpr std::array<TStringBuf, 0> zeroReplicaSensors = {{
        }};

        const NJson::TJsonValue::TArray& sensors = GetSensors();
        auto result = GetSumForAllHistograms(sensors);

        for (auto [name, value] : result) {
            if (name.Contains(TStringBuf(".yp_replica."))) {
                if (IsIn(zeroReplicaSensors, name)) {
                    UNIT_ASSERT_EQUAL(value, 0);
                } else {
                    UNIT_ASSERT_GT(value, 0); // this means that SelfClosingLogger did his job correctly.
                }
            }
        }
    }

    const TString psid1 = "psid-1";

    CreatePodSet(ypClient, psid1);

    const TString pid1 = "pid-1";

    NJson::TJsonValue podLabels;
    podLabels.InsertValue("shard_id", "1");

    CreatePod(ypClient, pid1, psid1, podLabels);

    { // test case 7
        // Test ResolvePods labels
        // The UpdatingFrequency of the YpServiceDicovery is 1ms and the SyncDB interval is 10ms
        // so we should get the endpoint in less than 200ms + 40 * 10 ms (just magic const)
        Sleep(TDuration::MilliSeconds(200));

        constexpr size_t MAX_ATTEMPTS = 10;

        bool gotResponse = false;
        bool validData = false;
        for (size_t attempt = 0; attempt < MAX_ATTEMPTS; ++attempt) {
            const auto req = GeneratePodRequest(psid1, "local-yt-tests", {"shard_id", "other_label"});
            const auto resp = ParseResponse(ResolvePodsJson(req).Content);

            if (resp["resolve_status"] == 2) {
                gotResponse = true;

                const auto& podSet = resp["pod_set"];
                const auto& pods = podSet["pods"].GetArray();

                validData = pods.size() == 1
                    && pods.front()["id"] == "pid-1"
                    && pods.front()["pod_label_selector_results"].GetArraySafe().size() == 2
                    && pods.front()["pod_label_selector_results"][0] == "1"
                    && pods.front()["pod_label_selector_results"][1] == "null";

                break;
            } else {
                Sleep(TDuration::Seconds(1));
            }
        }
        UNIT_ASSERT(gotResponse);
        UNIT_ASSERT(validData);
    }

    RemovePod(ypClient, pid1);

    const TString pid2 = "pid-2";
    CreatePod(ypClient, pid2, psid1);


    { // test case 8
        // Test ResolvePods. No labels in storage element
        Sleep(TDuration::Seconds(1));

        constexpr size_t MAX_ATTEMPTS = 10;

        bool gotResponse = false;
        bool validData = false;
        for (size_t attempt = 0; attempt < MAX_ATTEMPTS; ++attempt) {
            const auto req = GeneratePodRequest(psid1, "local-yt-tests", {"shard_id", "other_label"});
            const auto resp = ParseResponse(ResolvePodsJson(req).Content);

            if (resp["resolve_status"] == 2) {
                gotResponse = true;

                const auto& podSet = resp["pod_set"];
                const auto& pods = podSet["pods"].GetArray();

                validData = pods.size() == 1
                    && pods.front()["id"] == "pid-2"
                    && pods.front()["pod_label_selector_results"].GetArraySafe().size() == 2
                    && pods.front()["pod_label_selector_results"][0] == "null"
                    && pods.front()["pod_label_selector_results"][1] == "null";

                break;
            } else {
                Sleep(TDuration::Seconds(1));
            }
        }
        UNIT_ASSERT(gotResponse);
        UNIT_ASSERT(validData);
    }

    StopDaemon();
}

void RunTest(const TString& address) {
    try {
        RunLocalMasterTests(address);
    } catch (...) {
        TBackTrace::FromCurrentException().PrintTo(Cerr);
        throw;
    }
}
