#include "test_canon.h"

#include <infra/pod_agent/libs/daemon/main.h>
#include <infra/pod_agent/libs/porto_client/porto_test_lib/client_with_retries.h>
#include <infra/pod_agent/libs/porto_client/porto_test_lib/test_functions.h>

#include <library/cpp/testing/unittest/env.h>
#include <library/cpp/testing/unittest/registar.h>

#include <util/folder/dirut.h>
#include <util/system/user.h>

namespace NInfra::NPodAgent::NDaemonTest {

void TMyRunDaemon::operator()() {
    const char* argv[Args_.size()];

    for (size_t i = 0; i < Args_.size(); ++i) {
        argv[i] = Args_[i].c_str();
    }

    RunDaemon(Args_.size(), argv);
}

ITestCanon::ITestCanon(const TString& testName):
    TestName_(testName)
    , PortManager_()
    , Params_(
        {
            BinaryPath("infra/pod_agent/daemons/pod_agent/pod_agent") // PodAgentBinaryFilePath
            , 60000 // GCPeriodMS
            , 5000 // TreesUpdatePeriodMS
            , 8 // PortoPoolSize
            , 8 // TreesWorkersPoolSize
            , PortManager_.GetPort() // GRPCPort
            , PortManager_.GetPort() // HTTPPort
            , PortManager_.GetPort() // HTTPAlivePort
            , TestName_ + "_" + ToString(TThread::CurrentThreadId()) + "_current_spec.bin" // SaveSpecFileName
            , MakeFullPathPrefix() + "_cache_volume" // CacheVolumePath
            , TEST_PREFIX + "PodAgentCacheStorage_" + ToString(TThread::CurrentThreadId()) + "_" + TestName_ // CacheStorage
            , MakeFullPathPrefix() + "_public_volume" // PublicVolumePath
            , TEST_PREFIX + "PodAgentPublicVolumeStorage_" + ToString(TThread::CurrentThreadId()) + "_" + TestName_ // PublicVolumeStorage
            , "pod_agent" // PodAgentBinaryFileName
            , "human_readable_current_spec.json" // PublicHumanReadableSaveSpecFileName
            , "human_readable_current_spec.json" // CachedHumanReadableSaveSpecFileName
            , GetUsername() // ResourcesContainerUser
            , "porto" // ResourcesContainerGroup
            , TEST_PREFIX + "PodAgentResourceDownloadStorage_" + ToString(TThread::CurrentThreadId()) + "_" + TestName_ // ResourcesDownloadStoragePrefix
            , "STDERR" // LoggerBackend
            , "WARNING" // LoggerLevel
            , "" // LoggerFilePath
            , "STDERR" // LogsTransmitterJobWorkerEventsLoggerBackend
            , "WARNING" // LogsTransmitterJobWorkerEventsLoggerLevel
            , "" // LogsTransmitterJobWorkerEventsLoggerFilePath
            , "STDERR" //TreeTraceEventsLoggerBackend
            , "WARNING" //TreeTraceEventsLoggerLevel
            , "" //TreeTraceEventsLoggerFilePath
            , TEST_PREFIX + ToString(TThread::CurrentThreadId()) + "_" + TestName_ + "_" // LayerPrefix
            , TEST_PREFIX + ToString(TThread::CurrentThreadId()) + "_" + TestName_ + "_" // PersistentStoragePrefix
            , TEST_PREFIX + ToString(TThread::CurrentThreadId()) + "_" + TestName_ + "_" // ContainersPrefix
            , MakeFullPathPrefix() + "_resources_volume" // ResourcesDownloadPath
            , MakeFullPathPrefix() + "_volumes" // VolumeStorage
            , MakeFullPathPrefix() + "_rbind_volumes" // RbindVolumeStorageDir
            , false // LockSelfMemory
            , MakeFullPathPrefix() + "_virtual_disks" // VirtualDisksRootPath
            , false // VirtualDisksEnable
            , "/yt" // YtPath
            , "/basesearch" // BaseSearchPath

            // It is impossible to test the network normally, so in order not to break anything, we assume that it does not work
            , "this_device_does_not_exist_" + ToString(TThread::CurrentThreadId()) // NetworkDevice
        }
    )
    , PortoClient_(NPortoTestLib::GetTestPortoClientWithRetries(TEST_PREFIX, TEST_PREFIX, TEST_PREFIX, TestName_)) // TODO replace TestName_ -> TEST_PREFIX
    , SpecHolder_(ArcadiaSourceRoot() + "/infra/pod_agent/libs/daemon/tests/test_lib/specs.json")
    , Handle_("localhost:" + ToString(Params_.GRPCPort), TDuration::MilliSeconds(10000))
    , HttpPublicHandle_(Params_.HTTPPort, TDuration::MilliSeconds(10000))
    , HttpAliveHandle_(Params_.HTTPAlivePort, TDuration::MilliSeconds(10000))
{}

void ITestCanon::Test() {
    Init();
    DoTest();
    Finish();
}

void ITestCanon::PrepareVirtualDisks(const TVector<TString>& virtualDisks) {
    UNIT_ASSERT_C(Params_.VirtualDisksEnable, "You can't prepare virtual disks when they are disabled");

    if (NFs::Exists(Params_.VirtualDisksRootPath)) {
        NFs::RemoveRecursive(Params_.VirtualDisksRootPath);
    }
    NFs::MakeDirectoryRecursive(Params_.VirtualDisksRootPath);

    // Porto use root access for directories and they cannot be removed by the normal method
    // We use this volume to clean everything after test
    PortoClient_->CreateVolume(Params_.VirtualDisksRootPath, "", "", {}, 0, "", EPortoVolumeBackend::Auto, {""}, {}, false).Success();

    for (const TString& virtualDisk : virtualDisks) {
        const TString curDir = Params_.VirtualDisksRootPath + "/" + virtualDisk;
        NFs::MakeDirectoryRecursive(curDir);
    }
}

void ITestCanon::RemoveVolumes() {
    {
        auto result = PortoClient_->UnlinkVolume(RealPath(Params_.CacheVolumePath), TPortoContainerName::NoEscape("***"));
        if (!result && result.Error().Code != EPortoError::VolumeNotFound) {
            result.Success();
        }
    }

    {
        auto result = PortoClient_->UnlinkVolume(RealPath(Params_.PublicVolumePath), TPortoContainerName::NoEscape("***"));
        if (!result && result.Error().Code != EPortoError::VolumeNotFound) {
            result.Success();
        }
    }

    {
        if (Params_.VirtualDisksEnable) {
            auto result = PortoClient_->UnlinkVolume(RealPath(Params_.VirtualDisksRootPath), TPortoContainerName::NoEscape("***"));
            if (!result && result.Error().Code != EPortoError::VolumeNotFound) {
                result.Success();
            }
        }
    }

    auto list = PortoClient_->ListVolumes().Success();
    for (const auto& volume : list) {
        if (volume.path().StartsWith(Params_.VolumeStorage) || volume.path().StartsWith(Params_.ResourcesDownloadPath)) {
            auto result = PortoClient_->UnlinkVolume(volume.path(), TPortoContainerName::NoEscape("***"));
            if (!result && result.Error().Code != EPortoError::VolumeNotFound) {
                result.Success();
            }
        }
    }
}

void ITestCanon::RemoveLayers() {
    auto list = PortoClient_->ListLayers("", Params_.LayerPrefix + "*").Success();
    for (const auto& name : list) {
        PortoClient_->RemoveLayer(name.name()).Success();
    }
}

void ITestCanon::RemoveStorages() {
    auto listStorage = PortoClient_->ListStorages("", Params_.PersistentStoragePrefix + "*").Success();
    for (const auto& storage : listStorage) {
        PortoClient_->RemoveStorage(storage.name()).Success();
    }
    auto listDownloadStorage = PortoClient_->ListStorages("", Params_.ResourcesDownloadStoragePrefix + "*").Success();
    for (const auto& storage : listDownloadStorage) {
        PortoClient_->RemoveStorage(storage.name()).Success();
    }

    PortoClient_->RemoveStorage(Params_.CacheStorage).Success();
    PortoClient_->RemoveStorage(Params_.PublicVolumeStorage).Success();
}

void ITestCanon::WaitForReadiness() {
    TString response;
    for (i32 i = 0; i < 20; ++i) {
        try {
            response = Handle_.Ping();
        } catch (yexception exception) {
        }

        if (response == "pong") {
            return;
        }

        Sleep(TDuration::MilliSeconds(1000));
    }

    UNIT_ASSERT_C(0, "Something went wrong. We haven't received pong.");
}

TMap<TString, size_t> ITestCanon::GetPortoCallsMap() {
    TMap<TString, size_t> result;
    static const size_t prefixLen = strlen("pod_agent_porto_call_");
    static const size_t suffixLen = strlen("_deee");
    NJson::TJsonValue json;
    NJson::ReadJsonTree(Handle_.Sensors(TMultiUnistat::ESignalPriority::DEBUG), &json, true);
    for (auto& it : json.GetArraySafe()) {
        auto signalName = it[0].GetStringSafe();
        if (signalName.StartsWith("pod_agent_porto_call_") && !signalName.EndsWith(/* any */ "_exit_code_deee")) {
            result[signalName.substr(prefixLen, signalName.size() - prefixLen - suffixLen)] = it[1].GetUIntegerSafe();
        }
    }

    return result;
}

API::TPodAgentStatus ITestCanon::UpdatePodAgentRequestAndWaitForReady(const API::TPodAgentRequest& spec, size_t maxIter, std::function<bool(const API::TPodAgentStatus&)> readyHook) {
    API::TPodAgentStatus status;
    for (size_t i = 0; i < maxIter; ++i) {
        status = CorrectPodAgentStatus(Handle_.UpdatePodAgentRequest(spec));

        if (readyHook(status)) {
            break;
        }

        Sleep(TDuration::MilliSeconds(500));
    }
    UNIT_ASSERT_C(readyHook(status), status.Utf8DebugString());

    return status;
}

API::TPodAgentStatus ITestCanon::GetPodAgentStatus() {
    return CorrectPodAgentStatus(Handle_.GetPodAgentStatus());
}

TString ITestCanon::MakeFullPathPrefix() {
    return RealPath(GetWorkPath()) + "/" + TEST_PREFIX + TestName_ + "_" + ToString(TThread::CurrentThreadId());
}

} // namespace NInfra::NPodAgent::NDaemonTest
