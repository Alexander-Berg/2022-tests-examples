#pragma once

#include "handle.h"
#include "http_alive_handle.h"
#include "http_public_handle.h"
#include "spec_holder.h"

#include <infra/pod_agent/libs/porto_client/client.h>

#include <library/cpp/json/json_value.h>
#include <library/cpp/testing/unittest/tests_data.h>

#include <util/thread/factory.h>
#include <util/system/thread.h>

namespace NInfra::NPodAgent::NDaemonTest  {

struct TPodAgentParams {
    TString PodAgentBinaryFilePath;

    size_t GCPeriodMS;
    size_t TreesUpdatePeriodMS;
    size_t PortoPoolSize;
    size_t TreesWorkersPoolSize;
    size_t GRPCPort;
    size_t HTTPPort;
    size_t HTTPAlivePort;

    TString SaveSpecFileName;
    TString CacheVolumePath;
    TString CacheStorage;
    TString PublicVolumePath;
    TString PublicVolumeStorage;
    TString PodAgentBinaryFileName;
    TString PublicHumanReadableSaveSpecFileName;
    TString CachedHumanReadableSaveSpecFileName;
    TString ResourcesContainerUser;
    TString ResourcesContainerGroup;
    TString ResourcesDownloadStoragePrefix;
    TString LoggerBackend;
    TString LoggerLevel;
    TString LoggerFilePath;
    TString LogsTransmitterJobWorkerEventsLoggerBackend;
    TString LogsTransmitterJobWorkerEventsLoggerLevel;
    TString LogsTransmitterJobWorkerEventsLoggerFilePath;
    TString TreeTraceEventsLoggerBackend;
    TString TreeTraceEventsLoggerLevel;
    TString TreeTraceEventsLoggerFilePath;
    TString LayerPrefix;
    TString PersistentStoragePrefix;
    TString ContainersPrefix;
    TString ResourcesDownloadPath;
    TString VolumeStorage;
    TString RbindVolumeStorageDir;

    bool LockSelfMemory;

    TString VirtualDisksRootPath;
    bool VirtualDisksEnable;

    TString YtPath;
    TString BaseSearchPath;

    TString NetworkDevice;

    TVector<TString> GetPodAgentParamsVector() const {
        TVector<TString> res = {
            // As in mod chooser '<pod_agent_binary> <run_mode>'
            PodAgentBinaryFilePath + " run"

            , "-V", "BaseSearchBind.BaseSearchPath=" + BaseSearchPath
            , "-V", "BehaviorTicker.TreesWorkersPoolSize=" + ToString(TreesWorkersPoolSize)
            , "-V", "Cache.SaveSpecFileName=" + SaveSpecFileName
            , "-V", "Cache.HumanReadableSaveSpecFileName=" + CachedHumanReadableSaveSpecFileName
            , "-V", "Cache.Storage=" + CacheStorage
            , "-V", "Cache.VolumePath=" + CacheVolumePath
            , "-V", "GarbageCollector.PeriodMs=" + ToString(GCPeriodMS)
            , "-V", "GrpcService.Port=" + ToString(GRPCPort)
            , "-V", "HttpPublicService.Port=" + ToString(HTTPPort)
            , "-V", "HttpAliveService.Port=" + ToString(HTTPAlivePort)
            , "-V", "Ip.Device=" + NetworkDevice
            , "-V", "Logger.Backend=" + LoggerBackend
            , "-V", "Logger.Level=" + LoggerLevel
            , "-V", "Logger.Path=" + LoggerFilePath
            , "-V", "LogsTransmitter.JobWorkerEventsLogger.Backend=" + LogsTransmitterJobWorkerEventsLoggerBackend
            , "-V", "LogsTransmitter.JobWorkerEventsLogger.Level=" + LogsTransmitterJobWorkerEventsLoggerLevel
            , "-V", "LogsTransmitter.JobWorkerEventsLogger.Path=" + LogsTransmitterJobWorkerEventsLoggerFilePath
            , "-V", "TreeTraceLogger.Backend=" + TreeTraceEventsLoggerBackend
            , "-V", "TreeTraceLogger.Level=" + TreeTraceEventsLoggerLevel
            , "-V", "TreeTraceLogger.Path=" + TreeTraceEventsLoggerFilePath
            , "-V", "MemoryLock.LockSelfMemory=" + ToString(LockSelfMemory)
            , "-V", "Porto.ContainersPrefix=" + ContainersPrefix
            , "-V", "Porto.LayerPrefix=" + LayerPrefix
            , "-V", "Porto.PersistentStoragePrefix=" + PersistentStoragePrefix
            , "-V", "Porto.PoolSize=" + ToString(PortoPoolSize)
            , "-V", "PublicVolume.PodAgentBinaryFileName=" + PodAgentBinaryFileName
            , "-V", "PublicVolume.HumanReadableSaveSpecFileName=" + PublicHumanReadableSaveSpecFileName
            , "-V", "PublicVolume.Storage=" + PublicVolumeStorage
            , "-V", "PublicVolume.VolumePath=" + PublicVolumePath
            , "-V", "RbindVolumes.Storage=" + RbindVolumeStorageDir
            , "-V", "Resources.ContainerGroup=" + ResourcesContainerGroup
            , "-V", "Resources.ContainerUser=" + ResourcesContainerUser
            , "-V", "Resources.DownloadStoragePrefix=" + ResourcesDownloadStoragePrefix
            , "-V", "Resources.DownloadVolumePath=" + ResourcesDownloadPath
            , "-V", "TreesUpdate.PeriodMs=" + ToString(TreesUpdatePeriodMS)
            , "-V", "VirtualDisks.Enable=" + ToString(VirtualDisksEnable)
            , "-V", "VirtualDisks.RootPath=" + VirtualDisksRootPath
            , "-V", "Volumes.Storage=" + VolumeStorage
            , "-V", "YtBind.YtPath=" + YtPath
        };

        return res;
    }
};

class TMyRunDaemon {
public:
    TMyRunDaemon(const TPodAgentParams& params): Args_(params.GetPodAgentParamsVector()) {
    }

    void operator()();

private:
    TVector<TString> Args_;
};

class ITestCanon {
public:
    ITestCanon(const TString& testName);

    void Test();

protected:
    virtual void Init() {
        Thread_ = SystemThreadFactory()->Run(TMyRunDaemon(Params_));
        WaitForReadiness();
    }

    virtual void DoTest() = 0;

    virtual void Finish() {
        Handle_.Shutdown();
        Thread_->Join();
        DestroyContainersWithPrefix(PortoClient_, Params_.ContainersPrefix);
        RemoveVolumes();
        RemoveLayers();
        RemoveStorages();
    }

    void PrepareVirtualDisks(const TVector<TString>& virualDisks);

    void RemoveVolumes();

    void RemoveLayers();

    void RemoveStorages();

    void WaitForReadiness();

    TMap<TString, size_t> GetPortoCallsMap();

    API::TPodAgentStatus UpdatePodAgentRequestAndWaitForReady(
        const API::TPodAgentRequest& spec
        , size_t maxIter
        , std::function<bool(const API::TPodAgentStatus&)> readyHook = CheckAllObjectsReady
    );

    API::TPodAgentStatus GetPodAgentStatus();

    TString MakeFullPathPrefix();

protected:
    const TString TestName_;
    TPortManager PortManager_;
    TPodAgentParams Params_;
    TPortoClientPtr PortoClient_;
    TSpecHolder SpecHolder_;
    THandle Handle_;
    THttpPublicHandle HttpPublicHandle_;
    THttpAliveHandle HttpAliveHandle_;
    TAutoPtr<IThreadFactory::IThread> Thread_;
};

} // namespace NInfra::NPodAgent::NDaemonTest
