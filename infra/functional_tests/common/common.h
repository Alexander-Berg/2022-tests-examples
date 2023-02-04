#pragma once

#include <library/cpp/json/json_value.h>

namespace NYP::NServiceDiscovery {
    constexpr TStringBuf DEFAULT_CONFIG = "/yp_service_discovery/proto_config.json";
    constexpr TStringBuf EXPECTED_PING_RESPONSE = TStringBuf("{\"data\":\"pong\"}");
    constexpr TStringBuf TEST_CLUSTER_NAME = "test";
    constexpr TStringBuf DEFAULT_RUID = "ruid:12345_67890";

    constexpr std::initializer_list<TStringBuf> LOGGER_CONFIG_PATHS = {
        TStringBuf("LoggerConfig"),
        TStringBuf("EndpointsReplicaLoggerConfig"),
        TStringBuf("PodsReplicaLoggerConfig"),
        TStringBuf("NodeReplicaLoggerConfig"),
        TStringBuf("ConfigUpdatesLoggerConfig"),
        TStringBuf("AdminHttpServiceLoggerConfig"),
        TStringBuf("DiscoveryHttpServiceLoggerConfig"),
    };

    NJson::TJsonValue GetDefaultConfig();
    TVector<std::pair<TString, ui64> > GetSumForAllHistograms(const NJson::TJsonValue::TArray& sensors);
}
