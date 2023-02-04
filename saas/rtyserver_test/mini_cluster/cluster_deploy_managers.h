#pragma once

#include "mini_cluster_abstract.h"
#include <saas/deploy_manager/server/server.h>
#include <saas/library/daemon_base/daemon/controller.h>
#include <saas/library/daemon_base/daemon/daemon.h>

namespace NMiniCluster {
    class TClusterDeployManagers : public TNodeSet<TController, THttpDeployManager> {
    public:
        void Stop(bool clearStorage);
        virtual void SetPorts(TNode<TController, TServer>& node, ui16 firstPort) const;
        virtual TString GetBinName() const;
    };
}
