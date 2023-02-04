#include <infra/yp_service_discovery/functional_tests/common/storage_tests/common.h>

using namespace NJson;
using namespace NInfra;
using namespace NYP;
using namespace NServiceDiscovery;

constexpr std::initializer_list<TStringBuf> HISTOGRAMS = {
    TStringBuf("service.ping_response_time"),
    TStringBuf("service.sensors_response_time"),
    TStringBuf("service.resolve_endpoints.resolve_response_time")
};

namespace {
    void InitStorages(ui64 storageFormatVersion = STORAGE_JSON_FORMAT_VERSION) {
        NYP::NYPReplica::TStorageOptions options;
        options.Meta.Version = storageFormatVersion;
        for (const TStringBuf cluster : CLUSTERS) {
            options.Paths.StoragePath = TFsPath("storage/endpoints") / cluster;
            options.Paths.BackupPath = TFsPath("backup/endpoints") / cluster;
            NYP::NYPReplica::NTesting::TTestingStorage<NYP::NYPReplica::TEndpointReplicaObject, NYP::NYPReplica::TEndpointSetReplicaObject> storage(options);
            UNIT_ASSERT(storage.Open(/* validate */ false));
            storage.LoadFromJson("storage.json");
        }
    }
}

Y_UNIT_TEST_SUITE(Functional) {
    Y_UNIT_TEST(TestHistogramsWork) {
        InitStorages();
        RunDaemon(GetPatchedConfigOptions());

        TJsonValue request;
        request["cluster_name"] = "sas-test";
        request["endpoint_set_id"] = "slonnn-tunnel-test";
        request["client_name"] = "sensors-tests";
        request["ruid"] = DEFAULT_RUID;

        const ui32 attemptCount = 2048;

        for (ui32 i = 0; i < attemptCount; ++i) {
            Sensors();
            DynamicSensors();
            Ping();
            ResolveEndpointsJson(WriteJson(request));
        }

        const TJsonValue::TArray& sensors = GetSensors();
        const auto& sumsOfAllHistograms = GetSumForAllHistograms(sensors);
        THashMap<TStringBuf, ui64> accumulatedSums;
        for (const auto& [name, sum] : sumsOfAllHistograms) {
            if (name.Contains("yp_replica")) continue;
            accumulatedSums[name] += sum;
        }

        for (const TStringBuf& histogramName : HISTOGRAMS) {
            const TString histogramNameWithGroup = TString::Join(TStringBuf("yp_service_discovery."), histogramName);
            UNIT_ASSERT(accumulatedSums[histogramNameWithGroup] >= attemptCount);
        }

        StopDaemon();
    }
}
