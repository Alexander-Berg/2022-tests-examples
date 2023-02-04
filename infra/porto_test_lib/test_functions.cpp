#include "test_functions.h"

#include <infra/pod_agent/libs/porto_client/nested_client.h>
#include <infra/pod_agent/libs/porto_client/simple_client.h>

#include <infra/pod_agent/libs/porto_client/porto_test_lib/client_with_retries.h>

#include <util/folder/dirut.h>
#include <util/string/cast.h>
#include <util/system/env.h>
#include <util/system/thread.h>

namespace NInfra::NPodAgent::NPortoTestLib  {

TLoggerConfig GetLoggerConfig() {
    TLoggerConfig result;
    result.SetBackend(TLoggerConfig_ELogBackend_STDERR);
    result.SetLevel("ERROR");
    return result;
}
static TLogger logger(GetLoggerConfig());

namespace {

void RemoveOldLayers(const TString& layerPrefix) {
    const ui64 AgeInSeconds = 10800;

    TPortoClientPtr client = GetSimplePortoClient();
    auto list = client->ListLayers("", layerPrefix + "*").Success();

    for (const auto& layer : list) {
        if (layer.last_usage() >= AgeInSeconds) {
            auto result = client->RemoveLayer(layer.name(), "");
            if (!result) {
                Cerr << ToString(result.Error().Code) << " : " << result.Error().Action << " : "
                     << result.Error().Message << Endl;
            }
        }
    }
}

void RemoveOldStorages(const TString& storagePrefix) {
    const ui64 AgeInSeconds = 10800;
    TPortoClientPtr client = GetSimplePortoClient();
    auto list = client->ListStorages("", storagePrefix + "*").Success();

    for (const auto& storage : list) {
        if (storage.last_usage() >= AgeInSeconds) {
            auto result = client->RemoveStorage(storage.name(), "");
            if (!result) {
                Cerr << ToString(result.Error().Code) << " : " << result.Error().Action << " : "
                     << result.Error().Message << Endl;
            }
        }
    }
}

void RemoveOldVolumes(const TString& volumesSubstr) {
    const ui64 AgeInSeconds = 10800;
    TPortoClientPtr client = GetSimplePortoClient();
    auto list = client->ListVolumes().Success();

    for (const auto& volume : list) {
        TInstant changeTime = TInstant::Seconds(volume.change_time());
        if (changeTime < (TInstant::Now() - TDuration::Seconds(AgeInSeconds))
            && GetBaseName(volume.path()).Contains(volumesSubstr)) {
            auto result = client->UnlinkVolume(volume.path(), TPortoContainerName::NoEscape("***"));
            if (!result) {
                Cerr << ToString(result.Error().Code) << " : " << result.Error().Action << " : "
                << result.Error().Message << Endl;
            }
            break;
        }
    }
}

} // namespace

TPortoClientPtr GetSimplePortoClient() {
    static TPortoClientPtr client = new TSimplePortoClient(logger.SpawnFrame(), new TPortoConnectionPool(8), PORTO_CLIENT_DEFAULT_TIMEOUT);
    return client;
}

TPortoClientPtr GetTestPortoClient(const TString& containersPrefix, const TString& layerPrefix, const TString& storagePrefix, const TString& volumesSubstr) {
    DestroyOldContainers(containersPrefix);
    RemoveOldVolumes(volumesSubstr);
    RemoveOldLayers(layerPrefix);
    RemoveOldStorages(storagePrefix);

    return GetSimplePortoClient();
}

TPortoClientPtr GetTestPortoClientWithRetries(const TString& containersPrefix, const TString& layerPrefix, const TString& storagePrefix, const TString& volumesSubstr) {
    static TPortoClientPtr client = new TPortoClientWithRetries(GetTestPortoClient(containersPrefix, layerPrefix, storagePrefix, volumesSubstr));
    return client;
}

void DestroyOldContainers(const TString& prefix) {
    const ui64 AgeInSeconds = 3600;

    TPortoClientPtr client = GetSimplePortoClient();
    auto list = client->List(prefix + "*").Success();

    for (const auto& name : list) {
        auto result = client->GetProperty(name, EPortoContainerProperty::CreationTimeRaw, 0);
        if (!result) {
            Cerr << ToString(result.Error().Code) << " : " << result.Error().Action << " : " << result.Error().Message << Endl;
            continue;
        }

        TString creationTime = result.Success();
        if (TInstant::Now().Seconds() - FromString<time_t>(creationTime) >= AgeInSeconds) {
            auto result = client->Destroy(name);
            if (!result) {
                Cerr << ToString(result.Error().Code) << " : " << result.Error().Action << " : " << result.Error().Message << Endl;
            }
        }
    }
}

bool IsInsideSandboxPortoIsolation() {
    return GetEnv("container") == "porto";
}

} // namespace NInfra::NPodAgent::NPortoTestLib
