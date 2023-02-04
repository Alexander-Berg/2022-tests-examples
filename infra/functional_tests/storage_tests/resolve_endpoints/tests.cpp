#include <infra/yp_service_discovery/functional_tests/common/storage_tests/common.h>

using namespace NJson;
using namespace NInfra;
using namespace NYP;
using namespace NServiceDiscovery;

constexpr std::array<TStringBuf, 4> SENSORS_WITH_YP_CLUSTER = {
    TStringBuf("yp_replica.endpoints.master.requests"),
    TStringBuf("yp_replica.endpoints.master.failures"),
    TStringBuf("yp_replica.endpoints.backup.age"),
    TStringBuf("yp_replica.endpoints.backup.age_indicator"),
};

constexpr std::array<TStringBuf, 1> DYNAMIC_SENSORS_WITH_YP_CLUSTER = {
    TStringBuf("service.resolve_endpoints.empty_responses"),
};

constexpr std::array<TStringBuf, 1> SENSORS = {
    TStringBuf("service.resolve_endpoints.access_denied"),
};

Y_UNIT_TEST_SUITE(ResolveEndpoints) {
    Y_UNIT_TEST(TestResolveAndSensors) {
        InitEndpointStorages();
        RunDaemon(GetPatchedConfigOptions());

        UNIT_ASSERT_VALUES_EQUAL(Ping().Content, EXPECTED_PING_RESPONSE);

        auto validateResponse = [](const NInfra::TRouterResponse& routerResponse) {
            TStringStream responseSs{routerResponse.Content};
            TJsonValue response = ReadJsonTree(&responseSs, /* throwOnError */ true);

            UNIT_ASSERT_VALUES_EQUAL(response["timestamp"].GetUInteger(), 1664008293409882283LL);

            UNIT_ASSERT_VALUES_EQUAL(response["host"].GetString(), HostName());

            UNIT_ASSERT_VALUES_EQUAL(response["ruid"].GetString(), DEFAULT_RUID);

            UNIT_ASSERT(response["endpoint_set"]["endpoints"].IsArray());

            auto& endpoints = response["endpoint_set"]["endpoints"].GetArraySafe();
            UNIT_ASSERT_VALUES_EQUAL(endpoints.size(), 7);

            Sort(endpoints.begin(), endpoints.end(), [](const auto& lhs, const auto& rhs) {
                return lhs["id"].GetString() < rhs["id"].GetString();
            });

            static constexpr std::array<TStringBuf, 7> EXPECTED_IDS = {{TStringBuf("47f851bf"),
                                                                        TStringBuf("4f233a7e"),
                                                                        TStringBuf("769c2be7"),
                                                                        TStringBuf("d21553d0"),
                                                                        TStringBuf("d62b037b"),
                                                                        TStringBuf("e07d2a95"),
                                                                        TStringBuf("f69080a6")}};

            static constexpr std::array<TStringBuf, 7> EXPECTED_IPS = {{TStringBuf("2a02:6b8:c0c:a82:10d:b9ae:af3e:0"),
                                                                        TStringBuf("2a02:6b8:c08:1303:10d:b9ae:5d89:0"),
                                                                        TStringBuf("2a02:6b8:c00:2e1:10d:b9ae:4836:0"),
                                                                        TStringBuf("2a02:6b8:c0c:b02:10d:b9ae:13eb:0"),
                                                                        TStringBuf("2a02:6b8:c08:1491:10d:b9ae:1e8f:0"),
                                                                        TStringBuf("2a02:6b8:c08:12a6:10d:b9ae:4421:0"),
                                                                        TStringBuf("2a02:6b8:c08:112a:10d:b9ae:fc93:0")}};

            for (size_t i = 0; i < 7; ++i) {
                UNIT_ASSERT_VALUES_EQUAL(endpoints[i]["id"].GetString(), EXPECTED_IDS[i]);
                UNIT_ASSERT_VALUES_EQUAL(endpoints[i]["ip6_address"].GetString(), EXPECTED_IPS[i]);
                UNIT_ASSERT_VALUES_EQUAL(endpoints[i]["protocol"].GetString(), TStringBuf("TCP"));
                UNIT_ASSERT_VALUES_EQUAL(endpoints[i]["port"].GetInteger(), 22);
                UNIT_ASSERT_VALUES_EQUAL(endpoints[i]["label_selector_results"][0].GetString(), ToString(i + 1));
                UNIT_ASSERT_VALUES_EQUAL(endpoints[i]["label_selector_results"][1].GetString(), "null");
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
            { // test resolve_endpoints/json handler
                TJsonValue request;
                request["cluster_name"] = "sas-test";
                request["endpoint_set_id"] = "slonnn-tunnel-test";
                request["client_name"] = "storage-tests";
                request["ruid"] = DEFAULT_RUID;
                request["label_selectors"].AppendValue("key");
                request["label_selectors"].AppendValue("non_existent_key");

                validateResponse(ResolveEndpointsJson(WriteJson(request)));
            }

            { // test resolve_endpoints handler
                NApi::TReqResolveEndpoints request;
                request.set_cluster_name("sas-test");
                request.set_endpoint_set_id("slonnn-tunnel-test");
                request.set_client_name("storage-tests");
                request.set_ruid(DEFAULT_RUID.data());
                request.add_label_selectors("key");
                request.add_label_selectors("non_existent_key");

                TString strReq;
                UNIT_ASSERT(request.SerializeToString(&strReq));

                const TString strResp = ResolveEndpoints(strReq).Content;

                NApi::TRspResolveEndpoints response;
                UNIT_ASSERT(response.ParseFromString(strResp));

                validateResponse({NProtobufJson::Proto2Json<NApi::TRspResolveEndpoints>(response), {}});
            }

            { // test resolve_endpoints handler with empty response (endpoint set doesn't exist)
                NApi::TReqResolveEndpoints request;
                request.set_cluster_name("sas-test");
                request.set_endpoint_set_id("elshiko-non-existing-esid");
                request.set_client_name("storage-tests");
                request.set_ruid(DEFAULT_RUID.data());

                TString strReq;
                UNIT_ASSERT(request.SerializeToString(&strReq));

                const TString strResp = ResolveEndpoints(strReq).Content;

                NApi::TRspResolveEndpoints response;
                UNIT_ASSERT(response.ParseFromString(strResp));

                TJsonValue expectedResponse;
                expectedResponse["host"] = HostName();
                expectedResponse["ruid"] = DEFAULT_RUID;
                expectedResponse["timestamp"] = 1664008293409882283;

                UNIT_ASSERT_JSON_EQ_JSON(NProtobufJson::Proto2Json<NApi::TRspResolveEndpoints>(response), expectedResponse);
            }

            { // test resolve_endpoints handler with empty endpoint_set (endpoint set exists)
                NApi::TReqResolveEndpoints request;
                request.set_cluster_name("sas-test");
                request.set_endpoint_set_id("img-viewer");
                request.set_client_name("storage-tests");
                request.set_ruid(DEFAULT_RUID.data());

                TString strReq;
                UNIT_ASSERT(request.SerializeToString(&strReq));

                const TString strResp = ResolveEndpoints(strReq).Content;

                NApi::TRspResolveEndpoints response;
                UNIT_ASSERT(response.ParseFromString(strResp));

                TJsonValue expectedResponse;
                expectedResponse["endpoint_set"]["endpoint_set_id"] = "img-viewer";
                expectedResponse["resolve_status"] = 3;
                expectedResponse["host"] = HostName();
                expectedResponse["ruid"] = DEFAULT_RUID;
                expectedResponse["timestamp"] = 1664008293409882283;

                UNIT_ASSERT_JSON_EQ_JSON(NProtobufJson::Proto2Json<NApi::TRspResolveEndpoints>(response), expectedResponse);
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

            constexpr TStringBuf requests = "yp_service_discovery.service.resolve_endpoints.requests";
            constexpr TStringBuf successes = "yp_service_discovery.service.resolve_endpoints.successes";
            constexpr TStringBuf emptyResponses = "yp_service_discovery.service.resolve_endpoints.empty_responses";

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
                    UNIT_ASSERT(labels.Has("endpoints_resolve_status"));
                    successesByStatus[labels["endpoints_resolve_status"].GetString()] += sensor["value"].GetInteger();
                }
            }

            for (const auto& dynamicSensor : dynamicSensors) {
                const NJson::TJsonValue labels = dynamicSensor["labels"];
                const TStringBuf& name = labels["sensor"].GetString();
                if (name == emptyResponses) {
                    UNIT_ASSERT(labels.Has("yp_cluster"));
                    if (labels["yp_cluster"].GetString() == TStringBuf("sas-test") && labels.Has("endpoint_set_id")) {
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
                    UNIT_ASSERT_VALUES_EQUAL(value, 20);
                } else {
                    UNIT_ASSERT_VALUES_EQUAL(value, 40);
                }
            }

            UNIT_ASSERT_VALUES_EQUAL(successesByStatus["OK"], 20);
            UNIT_ASSERT_VALUES_EQUAL(successesByStatus["NOT_EXISTS"], 10);
            UNIT_ASSERT_VALUES_EQUAL(successesByStatus["EMPTY"], 10);
        }

        StopDaemon();
    }

    Y_UNIT_TEST(TestOldFormatVersion) {
        InitEndpointStorages(/* storageFormatVersion = */ 0);
        RunDaemon(GetPatchedConfigOptions());

        TJsonValue request;
        request["cluster_name"] = "sas-test";
        request["endpoint_set_id"] = "slonnn-tunnel-test";
        request["client_name"] = "storage-tests";

        auto rawResponse = ResolveEndpointsJson(WriteJson(request));
        TStringStream responseSs{rawResponse.Content};
        TJsonValue response = ReadJsonTree(&responseSs, /* throwOnError */ true);

        UNIT_ASSERT(!response.Has("endpoint_set"));

        StopDaemon();
    }

    Y_UNIT_TEST(TestWatches) {
        auto makeRequest = [&](const TJsonValue& request) {
            auto rawResponse = ResolveEndpointsJson(WriteJson(request));
            TStringStream responseSs{rawResponse.Content};
            TJsonValue response = ReadJsonTree(&responseSs, /* throwOnError */ true);
            return response;
        };

        InitEndpointStorages();
        RunDaemon(GetPatchedConfigOptions());

        TJsonValue request;
        request["cluster_name"] = "sas-test";
        request["endpoint_set_id"] = "slonnn-tunnel-test";
        request["client_name"] = "storage-tests";

        auto response = makeRequest(request);
        UNIT_ASSERT_VALUES_EQUAL(response["resolve_status"].GetIntegerRobust(), 2);

        request["watch_token"] = response["watch_token"];
        response = makeRequest(request);
        UNIT_ASSERT_VALUES_EQUAL(response["resolve_status"].GetIntegerRobust(), 1);

        request["watch_token"] = "123456";
        response = makeRequest(request);
        UNIT_ASSERT_VALUES_EQUAL(response["resolve_status"].GetIntegerRobust(), 2);

        request.EraseValue("watch_token");
        request["endpoint_set_id"] = "unexistent_endpoint_set_id";
        response = makeRequest(request);
        UNIT_ASSERT_VALUES_EQUAL(response["resolve_status"].GetIntegerRobust(), 0);

        StopDaemon();
    }
}
