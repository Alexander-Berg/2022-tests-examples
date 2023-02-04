#include "cluster_deploy_managers.h"

#include <saas/deploy_manager/storage/abstract.h>

#include <library/cpp/zookeeper/zookeeper.h>

namespace NMiniCluster {
    void TClusterDeployManagers::Stop(bool clearStorage) {
        if (clearStorage && !GetNodes().empty()) {
            DEBUG_LOG << "try to clear caches.." << Endl;
            TString  result;
            ExecuteCommand(TExecutePolicy("*", TExecutePolicy::IGNORE_ERROR), "?command=clear_caches", result, 100000, "");
            DEBUG_LOG << "try to clear caches..OK" << Endl;
        }
        TNodeSet<TController, TServer>::Stop();
        if (clearStorage && !GetNodes().empty()) try {
            DEBUG_LOG << "try to clear storages.." << Endl;
            TServer::TConfig cfg(*GetNodes().begin()->second->ConstructParams);
            THolder<NRTYDeploy::IVersionedStorage> storage(NRTYDeploy::IVersionedStorage::Create(cfg.GetStorageOptions()));
            DEBUG_LOG << "try to clear storage.." << Endl;
            storage->RemoveNode("/", true);
            DEBUG_LOG << "try to clear storage..OK" << Endl;
            DEBUG_LOG << "try to clear queue.." << Endl;
            NRTYDeploy::IVersionedStorage::TOptions copy = cfg.GetStorageOptions();
            copy.LocalOptions.Root = copy.QueueName;
            copy.ZooOptions.Root = copy.QueueName;
            storage.Reset(NRTYDeploy::IVersionedStorage::Create(copy));
            storage->RemoveNode("/", true);
            DEBUG_LOG << "try to clear queue..OK" << Endl;
            DEBUG_LOG << "try to clear ..OK" << Endl;
        } catch (const yexception& e) {
            ERROR_LOG << "An exception has occurred: " << e.what() << Endl;
        }
    }

    void TClusterDeployManagers::SetPorts(TNode<TClusterDeployManagers::TController, TClusterDeployManagers::TServer>& node, ui16 firstPort) const {
        node.AddPatch("DeployManager.HttpOptions.Port", ToString(firstPort));
        node.AddPatch("DeployManager.DeployManagerBalanser.Port", ToString(firstPort));
        node.AddPatch("DeployManager.DeployManagerBalanser.Host", HostName());
    }
    TString TClusterDeployManagers::GetBinName() const {
        return "deploy_manager";
    }
}
