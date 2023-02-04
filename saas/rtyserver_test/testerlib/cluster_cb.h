#pragma once

#include "rtyserver_test.h"

#include <library/cpp/yconf/patcher/config_patcher.h>


void CopyAndPatch(const TFsPath& from, const TFsPath& to, const TConfigFieldsPtr configDiff) {
    if (!!configDiff && from.Basename().StartsWith("rtyserver.conf")) {
        NConfigPatcher::TOptions options;
        TString original = TUnbufferedFileInput(from.GetPath()).ReadAll();
        TUnbufferedFileOutput fo(to.GetPath());
        fo << NConfigPatcher::Patch(original, configDiff->Serialize(), options);
    } else
        NFs::Copy(from, to);
}

void CopyConfigs(const TFsPath& from, const TFsPath& to, const TConfigFieldsPtr configDiff) {
    if (from.IsFile()) {
        CopyAndPatch(from, to / from.Basename(), configDiff);
    } else {
        TVector<TString> configs;
        from.ListNames(configs);
        to.MkDirs();
        for (TVector<TString>::const_iterator i = configs.begin(); i != configs.end(); ++i) {
            TFsPath oldFile = from / *i;
            if (oldFile.IsFile())
                CopyAndPatch(oldFile, to / *i, configDiff);
            else
                CopyConfigs(oldFile, to / *i, configDiff);
        }
    }
}

void CopyConfigs(NMiniCluster::TConfig& clusterConf, const TFsPath& from, const TFsPath& to, const TConfigFieldsPtr configDiff) {
    TFsPath fromReal = (from / "start_configs").Exists() ? (from / "start_configs") : from;
    for (NMiniCluster::TConfig::TNodesConfig::const_iterator nd = clusterConf.Nodes.begin(); nd != clusterConf.Nodes.end(); ++nd)
        CopyConfigs(fromReal, to / nd->Name / "configs", configDiff);
    CopyConfigs(from, to / "copy", configDiff);
    clusterConf.RootPath = to.GetPath() + "/";
}

void ClearConfigs(const NMiniCluster::TConfig& clusterConf, const TFsPath& to) {
    for (NMiniCluster::TConfig::TNodesConfig::const_iterator nd = clusterConf.Nodes.begin(); nd != clusterConf.Nodes.end(); ++nd)
        (to / nd->Name).ForceDelete();
    (to / "copy").ForceDelete();
}


class TClusterCallback : public TTestSet::ICallback {
public:
    TClusterCallback(NMiniCluster::TConfig& clusterConfig, const TFsPath& configsRoot, bool useMetaSerivices)
        : ClusterConfig(clusterConfig)
        , ConfigsRoot(configsRoot)
        , UseMetaSerivices(useMetaSerivices)
    {}

    virtual void StopCluster() {
        if (!!Cluster) {
            Cluster->Stop(true);
            Cluster.Reset(nullptr);
        }
    }

    virtual void RestartCluster(TBackendProxyConfig& proxyConfig, const TString& testDir, const TConfigFieldsPtr configDiff) {
        StopCluster();
        ClearConfigs(ClusterConfig, ClusterConfig.RootPath);
        TFsPath path(testDir);
        for (NMiniCluster::TConfig::TNodesConfig::const_iterator i = ClusterConfig.Nodes.begin(); i != ClusterConfig.Nodes.end(); ++i) {
            if (i->Product == "rtyserver") {
                (path / i->Name / "index/").MkDirs();
                (path / i->Name / "detach/").MkDirs();
                (path / i->Name / "cache/").MkDirs();
            }
        }
        CopyConfigs(ClusterConfig, ConfigsRoot, ClusterConfig.RootPath, configDiff);
        Cluster.Reset(new TMiniCluster(ClusterConfig, UseMetaSerivices));
        Cluster->AdaptBackendProxyConfig(proxyConfig);
        Cluster->GenerateSearchmap(ClusterConfig.RootPath + "cluster.meta");
        CopyConfigs(ClusterConfig, ClusterConfig.RootPath + "cluster.meta", ClusterConfig.RootPath, configDiff);
        CopyConfigs(ClusterConfig, ClusterConfig.RootPath + Cluster->GetSmapFilename(), ClusterConfig.RootPath, configDiff);
        Cluster->Run();
        if (!proxyConfig.ControllingDeployManager){
            Cluster->RunServers();
        }
    }
    const TMiniCluster& GetCluster() const {
        return *Cluster;
    }
    virtual bool RunNode(TString nodeName, TRtyTestNodeType product) {
        return Cluster->DoWithNode(nodeName, "run", product);
    }
    virtual bool WaitNode(TString nodeName, TRtyTestNodeType product) {
        return Cluster->DoWithNode(nodeName, "wait", product);
    }
    virtual bool RestartNode(TString nodeName, TRtyTestNodeType product) {
        return Cluster->DoWithNode(nodeName, "restart", product);
    }
    virtual bool StopNode(TString nodeName, TRtyTestNodeType product) {
        return Cluster->DoWithNode(nodeName, "stop", product);
    }
    virtual bool SendSignalNode(TString nodeName, TRtyTestNodeType product, ui32 signum) {
        NJson::TJsonValue param(NJson::JSON_MAP);
        param["signal"] = signum;
        return Cluster->DoWithNode(nodeName, "signal", product, param);
    }
    virtual TSet<TString> GetNodesNames(TRtyTestNodeType product) const {
        return Cluster->GetNodesNames(product);
    }
private:
    THolder<TMiniCluster> Cluster;
    NMiniCluster::TConfig& ClusterConfig;
    TFsPath ConfigsRoot;
    bool UseMetaSerivices;
};

