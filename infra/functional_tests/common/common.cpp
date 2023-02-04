#include "common.h"

#include <library/cpp/json/json_reader.h>
#include <library/cpp/resource/resource.h>

#include <infra/libs/sensors/sensor.h>

#include <util/stream/file.h>

namespace NYP::NServiceDiscovery {
    NJson::TJsonValue GetDefaultConfig() {
        const TString raw = NResource::Find(DEFAULT_CONFIG);
        TStringInput input{raw};

        NJson::TJsonValue result = NJson::ReadJsonTree(&input, /* throwOnerror */ true);
        for (const TStringBuf loggerPath : LOGGER_CONFIG_PATHS) {
            if (auto loggerConfig = result.GetValueByPath(loggerPath)) {
                (*loggerConfig)["DuplicateToUnifiedAgent"] = false;
                (*loggerConfig)["DuplicateToErrorBooster"] = false;
            }
        }

        return result;
    }

    TVector<std::pair<TString, ui64> > GetSumForAllHistograms(const NJson::TJsonValue::TArray& sensors) {
        TVector<std::pair<TString, ui64> > result;
        for (const auto& it : sensors) {
            if (it["kind"] == "HIST" || it["kind"] == "HIST_RATE") {

                ui64 sum = 0;
                const auto& histogram = it["hist"]["buckets"].GetArray();
                for (ui32 i = 0; i < NMonitoring::HISTOGRAM_MAX_BUCKETS_COUNT; ++i) {
                    sum += histogram[i].GetInteger();
                }

                sum += it["hist"]["inf"].GetInteger();
                result.emplace_back(it["labels"]["sensor"].GetString(), sum);
            }
        }
        return result;
    }
}
