#pragma once

#include "cluster_backends.h"
#include "cluster_emulators.h"
#include "cluster_searchproxies.h"
#include "cluster_indexerproxies.h"
#include "cluster_deploy_managers.h"
#include "cluster_external_script.h"
#include "cluster_distributor.h"
#include <saas/rtyserver_test/common/test_abstract.h>
#include <saas/rtyserver_test/common/backend_config.h>

class TMiniCluster {
public:
    TMiniCluster(const NMiniCluster::TConfig& config, bool withMetaServices);
    ~TMiniCluster();
     void Stop(bool clearStorage);
     void Run();
     void RunServers();
     bool DoWithNode(TString nodeName, TString action, TRtyTestNodeType product = TNODE_SCRIPT, NJson::TJsonValue params=NJson::TJsonValue());
     void AdaptBackendProxyConfig(TBackendProxyConfig& proxyConfig) const;
     void GenerateSearchmap(const TString& filename) const;
     TSet<TString> GetNodesNames(TRtyTestNodeType product = TNODE_SCRIPT) const;

     const NMiniCluster::TClusterBackends& GetBackends() const {
         return Backends;
     }

     const NMiniCluster::TConfig& GetConfig() const {
         return Config;
     }

    const NSaas::TCluster& GetCluster() const {
         return Cluster;
     }

    const TString GetSmapFilename() const {
        return Searchmap.SmapFilename ? Searchmap.SmapFilename : "searchmap.json";
    }

private:
    const NMiniCluster::TConfig& Config;
    bool Running;
    bool ControllingDeployManager;

    void GenerateTagsInfo() const;
    void CleanZooKeeper() const;
    void BuildCluster(const bool withMetaServices);

    NMiniCluster::TClusterBackends Backends;
    NMiniCluster::TClusterEmulators Emulators;
    NMiniCluster::TClusterSearchproxies Searchproxies;
    NMiniCluster::TClusterSearchproxies IntSearches;
    NMiniCluster::TClusterIndexerproxies Indexerproxies;
    NMiniCluster::TClusterDeployManagers DeployManagers;
    NMiniCluster::TClusterSearchmap Searchmap;
    NMiniCluster::TClusterExternalScripts ExternalScripts;
    NMiniCluster::TClusterDistributors Distributors;

    NSaas::TCluster Cluster;
};
