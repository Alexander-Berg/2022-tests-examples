#include <infra/yp_service_discovery/functional_tests/common/storage_tests/common.h>

using namespace NJson;
using namespace NInfra;
using namespace NYP;
using namespace NServiceDiscovery;

constexpr std::array<TStringBuf, 4> SENSORS_WITH_YP_CLUSTER = {
    TStringBuf("yp_replica.node.master.requests"),
    TStringBuf("yp_replica.node.master.failures"),
    TStringBuf("yp_replica.node.backup.age"),
    TStringBuf("yp_replica.node.backup.age_indicator"),
};

constexpr std::array<TStringBuf, 1> DYNAMIC_SENSORS_WITH_YP_CLUSTER = {
    TStringBuf("service.resolve_node.empty_responses"),
};

constexpr std::array<TStringBuf, 1> SENSORS = {
    TStringBuf("service.resolve_node.access_denied"),
};

Y_UNIT_TEST_SUITE(ResolveNode) {
    Y_UNIT_TEST(TestResolveAndSensors) {
        InitNodeStorages();
        RunDaemon(GetPatchedConfigOptions());

        UNIT_ASSERT_VALUES_EQUAL(Ping().Content, EXPECTED_PING_RESPONSE);

        auto validateResponse = [](const NInfra::TRouterResponse& routerResponse) {
            TStringStream responseSs{routerResponse.Content};
            TJsonValue response = ReadJsonTree(&responseSs, /* throwOnError */ true);

            UNIT_ASSERT_VALUES_EQUAL(response["timestamp"].GetUInteger(), 1664008293409880283LL);

            UNIT_ASSERT_VALUES_EQUAL(response["host"].GetString(), HostName());

            UNIT_ASSERT_VALUES_EQUAL(response["ruid"].GetString(), DEFAULT_RUID);

            UNIT_ASSERT(response["pods"].IsArray());

            auto& pods = response["pods"].GetArraySafe();
            UNIT_ASSERT_VALUES_EQUAL(pods.size(), 2);

            Sort(pods.begin(), pods.end(), [](const auto& lhs, const auto& rhs) {
                return lhs["id"].GetString() < rhs["id"].GetString();
            });

            static constexpr std::array<TStringBuf, 2> EXPECTED_IDS = {{TStringBuf("216a7bd9"),
                                                                        TStringBuf("952c86ec")}};

            static constexpr std::array<TStringBuf, 2> EXPECTED_FQDNS = {{TStringBuf("216a7bd9.yandex.net"),
                                                                             TStringBuf("952c86ec.yandex.net")}};

            for (size_t i = 0; i < 2; ++i) {
                UNIT_ASSERT_VALUES_EQUAL(pods[i]["id"].GetString(), EXPECTED_IDS[i]);
                UNIT_ASSERT_VALUES_EQUAL(pods[i]["dns"]["persistent_fqdn"].GetString(), EXPECTED_FQDNS[i]);
            }
        };

        { // validate initialization of sensors

            const TJsonValue::TArray& sensors = GetSensors();
            const TJsonValue::TArray& dynamicSensors = GetDynamicSensors();

            for (const TStringBuf sensorSuffix : SENSORS_WITH_YP_CLUSTER) {
                const TString sensorName = TString::Join("yp_service_discovery.", sensorSuffix);
                for (const TStringBuf ypCluster : CLUSTERS) {
                    const auto& sensor = FindIf(sensors.begin(), sensors.end(), [&sensorName, &ypCluster](const auto& it) {
                        return it["labels"]["sensor"].GetString() == sensorName && it["labels"]["yp_cluster"].GetString() == ypCluster;
                    });
                    UNIT_ASSERT(sensor != sensors.end());
                }
            }

            for (const TStringBuf sensorSuffix : DYNAMIC_SENSORS_WITH_YP_CLUSTER) {
                const TString sensorName = TString::Join("yp_service_discovery.", sensorSuffix);
                for (const TStringBuf ypCluster : CLUSTERS) {
                    const auto& sensor = FindIf(dynamicSensors.begin(), dynamicSensors.end(), [&sensorName, &ypCluster](const auto& it) {
                        return it["labels"]["sensor"].GetString() == sensorName && it["labels"]["yp_cluster"].GetString() == ypCluster;
                    });
                    UNIT_ASSERT(sensor != dynamicSensors.end());
                }
            }

            for (const TStringBuf sensorSuffix : SENSORS) {
                const TString sensorName = TString::Join("yp_service_discovery.", sensorSuffix);
                const auto& sensor = FindIf(sensors.begin(), sensors.end(), [&sensorName](const auto& it) {
                    return it["labels"]["sensor"].GetString() == sensorName;
                });
                UNIT_ASSERT(sensor != sensors.end());
            }
        }

        for (size_t i = 0; i < 10; ++i) {
            { // test resolve_node/json handler
                TJsonValue request;
                request["cluster_name"] = "sas-test";
                request["node_id"] = "8286e66722d9f276";
                request["client_name"] = "storage-tests";
                request["ruid"] = DEFAULT_RUID;

                auto resp = ResolveNodeJson(WriteJson(request));
                validateResponse(resp);
            }

            { // test resolve_node handler
                NApi::TReqResolveNode request;
                request.set_cluster_name("sas-test");
                request.set_node_id("8286e66722d9f276");
                request.set_client_name("storage-tests");
                request.set_ruid(DEFAULT_RUID.data());

                TString strReq;
                UNIT_ASSERT(request.SerializeToString(&strReq));

                const TString strResp = ResolveNode(strReq).Content;

                NApi::TRspResolveNode response;
                UNIT_ASSERT(response.ParseFromString(strResp));

                validateResponse({NProtobufJson::Proto2Json<NApi::TRspResolveNode>(response), {}});
            }

            { // test resolve_node handler with empty response (node doesn't exist)
                NApi::TReqResolveNode request;
                request.set_cluster_name("sas-test");
                request.set_node_id("non-existing-node");
                request.set_client_name("storage-tests");
                request.set_ruid(DEFAULT_RUID.data());

                TString strReq;
                UNIT_ASSERT(request.SerializeToString(&strReq));

                const TString strResp = ResolveNode(strReq).Content;

                NApi::TRspResolveNode response;
                UNIT_ASSERT(response.ParseFromString(strResp));

                TJsonValue expectedResponse;
                expectedResponse["host"] = HostName();
                expectedResponse["ruid"] = DEFAULT_RUID;
                expectedResponse["timestamp"] = 1664008293409880283;

                UNIT_ASSERT_JSON_EQ_JSON(NProtobufJson::Proto2Json<NApi::TRspResolveNode>(response), expectedResponse);
            }
        }

        { // validate sensors
            const TJsonValue::TArray& sensors = GetSensors();
            const TJsonValue::TArray& dynamicSensors = GetDynamicSensors();

            for (auto& sensor : sensors) {
                const auto& name = sensor["labels"]["sensor"].GetString();
                if (name.Contains("yp_replica")) {
                    UNIT_ASSERT(sensor["labels"].Has("yp_cluster"));
                }
            }

            for (auto& dynamicSensor : dynamicSensors) {
                const auto& name = dynamicSensor["labels"]["sensor"].GetString();
                if (name.Contains("yp_replica")) {
                    UNIT_ASSERT(dynamicSensor["labels"].Has("yp_cluster"));
                }
            }

            constexpr TStringBuf requests = "yp_service_discovery.service.resolve_node.requests";
            constexpr TStringBuf successes = "yp_service_discovery.service.resolve_node.successes";
            constexpr TStringBuf emptyResponses = "yp_service_discovery.service.resolve_node.empty_responses";

            TMap<TStringBuf, ui64> accumulatedSensors;
            TMap<TStringBuf, ui64> successesByStatus;
            for (const auto& sensor : sensors) {
                const NJson::TJsonValue labels = sensor["labels"];
                const TStringBuf& name = labels["sensor"].GetString();
                if (name == requests || name == successes) {
                    UNIT_ASSERT(labels.Has("yp_cluster"));
                    UNIT_ASSERT_VALUES_EQUAL(labels["yp_cluster"].GetString(), "sas-test");
                    accumulatedSensors[name] += sensor["value"].GetInteger();
                }

                if (name == successes) {
                    UNIT_ASSERT(labels.Has("node_resolve_status"));
                    successesByStatus[labels["node_resolve_status"].GetString()] += sensor["value"].GetInteger();
                }
            }

            for (const auto& dynamicSensor : dynamicSensors) {
                const NJson::TJsonValue labels = dynamicSensor["labels"];
                const TStringBuf& name = labels["sensor"].GetString();
                if (name == emptyResponses) {
                    UNIT_ASSERT(labels.Has("yp_cluster"));
                    if (labels["yp_cluster"].GetString() == TStringBuf("sas-test") && labels.Has("node_id")) {
                        accumulatedSensors[name] += dynamicSensor["value"].GetInteger();
                        UNIT_ASSERT_VALUES_EQUAL(labels["yp_cluster"].GetString(), "sas-test");
                    } else {
                        UNIT_ASSERT_VALUES_EQUAL(dynamicSensor["value"].GetInteger(), 0);
                    }
                }
            }

            UNIT_ASSERT_VALUES_EQUAL(accumulatedSensors.size(), 3);
            for (const auto& [name, value] : accumulatedSensors) {
                if (name == emptyResponses) {
                    UNIT_ASSERT_VALUES_EQUAL(value, 10);
                } else {
                    UNIT_ASSERT_VALUES_EQUAL(value, 30);
                }
            }

            UNIT_ASSERT_VALUES_EQUAL(successesByStatus["OK"], 20);
            UNIT_ASSERT_VALUES_EQUAL(successesByStatus["NOT_EXISTS"], 10);
            UNIT_ASSERT_VALUES_EQUAL(successesByStatus["EMPTY"], 0);
        }

        StopDaemon();
    }

}
