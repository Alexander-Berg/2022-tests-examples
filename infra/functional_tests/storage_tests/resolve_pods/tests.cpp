#include <infra/yp_service_discovery/functional_tests/common/storage_tests/common.h>

using namespace NJson;
using namespace NInfra;
using namespace NYP;
using namespace NServiceDiscovery;

constexpr std::array<TStringBuf, 4> SENSORS_WITH_YP_CLUSTER = {
    TStringBuf("yp_replica.pods.master.requests"),
    TStringBuf("yp_replica.pods.master.failures"),
    TStringBuf("yp_replica.pods.backup.age"),
    TStringBuf("yp_replica.pods.backup.age_indicator"),
};

constexpr std::array<TStringBuf, 1> DYNAMIC_SENSORS_WITH_YP_CLUSTER = {
    TStringBuf("service.resolve_pods.empty_responses"),
};

constexpr std::array<TStringBuf, 1> SENSORS = {
    TStringBuf("service.resolve_pods.access_denied"),
};

Y_UNIT_TEST_SUITE(ResolvePods) {
    Y_UNIT_TEST(TestResolveAndSensors) {
        InitPodStorages();
        RunDaemon(GetPatchedConfigOptions());

        UNIT_ASSERT_VALUES_EQUAL(Ping().Content, EXPECTED_PING_RESPONSE);

        auto validateResponse = [](const NInfra::TRouterResponse& routerResponse) {
            TStringStream responseSs{routerResponse.Content};
            TJsonValue response = ReadJsonTree(&responseSs, /* throwOnError */ true);

            UNIT_ASSERT_VALUES_EQUAL(response["timestamp"].GetUInteger(), 1664008293409880283LL);

            UNIT_ASSERT_VALUES_EQUAL(response["host"].GetString(), HostName());

            UNIT_ASSERT_VALUES_EQUAL(response["ruid"].GetString(), DEFAULT_RUID);

            UNIT_ASSERT(response["pod_set"]["pods"].IsArray());

            auto& pods = response["pod_set"]["pods"].GetArraySafe();
            UNIT_ASSERT_VALUES_EQUAL(pods.size(), 3);

            Sort(pods.begin(), pods.end(), [](const auto& lhs, const auto& rhs) {
                return lhs["id"].GetString() < rhs["id"].GetString();
            });

            static constexpr std::array<TStringBuf, 3> EXPECTED_IDS = {{TStringBuf("216a7bd9"),
                                                                        TStringBuf("952c86ec"),
                                                                        TStringBuf("993dghuhw")}};

            static constexpr std::array<TStringBuf, 3> EXPECTED_NODE_IDS = {{TStringBuf("672f9d22766e6828"),
                                                                             TStringBuf("8286e66722d9f276"),
                                                                             TStringBuf("65r56fty5erty")}};

            static constexpr std::array<TStringBuf, 3> EXPECTED_LABELS = {{TStringBuf("[1,\"2\",{\"key\":\"value\"}]"),
                                                                           TStringBuf("1"),
                                                                           TStringBuf("null")}};

            static constexpr std::array<TStringBuf, 3> EXPECTED_SECOND_LABELS = {{TStringBuf("null"),
                                                                                  TStringBuf("null"),
                                                                                  TStringBuf("null")}};

            for (size_t i = 0; i < 3; ++i) {
                UNIT_ASSERT_VALUES_EQUAL(pods[i]["id"].GetString(), EXPECTED_IDS[i]);
                UNIT_ASSERT_VALUES_EQUAL(pods[i]["node_id"].GetString(), EXPECTED_NODE_IDS[i]);

                auto& labels = pods[i]["pod_label_selector_results"].GetArraySafe();
                UNIT_ASSERT_VALUES_EQUAL(labels.size(), 2);
                UNIT_ASSERT_VALUES_EQUAL(labels[0].GetString(), EXPECTED_LABELS[i]);
                UNIT_ASSERT_VALUES_EQUAL(labels[1].GetString(), EXPECTED_SECOND_LABELS[i]);
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
            { // test resolve_pods/json handler
                TJsonValue request;
                request["cluster_name"] = "sas-test";
                request["pod_set_id"] = "s-testing-balancer";
                request["client_name"] = "storage-tests";
                request["ruid"] = DEFAULT_RUID;
                request["pod_label_selectors"].AppendValue("shard_id");
                request["pod_label_selectors"].AppendValue("non_existing_label");

                auto resp = ResolvePodsJson(WriteJson(request));
                validateResponse(resp);
            }

            { // test resolve_pods handler
                NApi::TReqResolvePods request;
                request.set_cluster_name("sas-test");
                request.set_pod_set_id("s-testing-balancer");
                request.set_client_name("storage-tests");
                request.set_ruid(DEFAULT_RUID.data());
                request.add_pod_label_selectors("shard_id");
                request.add_pod_label_selectors("non_existing_label");

                TString strReq;
                UNIT_ASSERT(request.SerializeToString(&strReq));

                const TString strResp = ResolvePods(strReq).Content;

                NApi::TRspResolvePods response;
                UNIT_ASSERT(response.ParseFromString(strResp));

                NProtobufJson::TProto2JsonConfig cfg;
                validateResponse({NProtobufJson::Proto2Json(response, cfg), {}});
            }

            { // test resolve_pods handler with empty response (pod set doesn't exist)
                NApi::TReqResolvePods request;
                request.set_cluster_name("sas-test");
                request.set_pod_set_id("non-existing-psid");
                request.set_client_name("storage-tests");
                request.set_ruid(DEFAULT_RUID.data());

                TString strReq;
                UNIT_ASSERT(request.SerializeToString(&strReq));

                const TString strResp = ResolvePods(strReq).Content;

                NApi::TRspResolvePods response;
                UNIT_ASSERT(response.ParseFromString(strResp));

                TJsonValue expectedResponse;
                expectedResponse["host"] = HostName();
                expectedResponse["ruid"] = DEFAULT_RUID;
                expectedResponse["timestamp"] = 1664008293409880283;

                NProtobufJson::TProto2JsonConfig cfg;
                UNIT_ASSERT_JSON_EQ_JSON(NProtobufJson::Proto2Json(response, cfg), expectedResponse);
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

            constexpr TStringBuf requests = "yp_service_discovery.service.resolve_pods.requests";
            constexpr TStringBuf successes = "yp_service_discovery.service.resolve_pods.successes";
            constexpr TStringBuf emptyResponses = "yp_service_discovery.service.resolve_pods.empty_responses";

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
                    UNIT_ASSERT(labels.Has("pods_resolve_status"));
                    successesByStatus[labels["pods_resolve_status"].GetString()] += sensor["value"].GetInteger();
                }
            }
            for (const auto& dynamicSensor : dynamicSensors) {
                const NJson::TJsonValue labels = dynamicSensor["labels"];
                const TStringBuf& name = labels["sensor"].GetString();
                if (name == emptyResponses) {
                    UNIT_ASSERT(labels.Has("yp_cluster"));
                    if (labels["yp_cluster"].GetString() == TStringBuf("sas-test") && labels.Has("pod_set_id")) {
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
