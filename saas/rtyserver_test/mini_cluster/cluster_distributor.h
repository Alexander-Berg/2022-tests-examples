#pragma once

#include "mini_cluster_abstract.h"

#include <saas/library/daemon_base/daemon/controller.h>
#include <saas/library/daemon_base/daemon/daemon.h>

#include <yweb/realtime/distributor/monolith/server/server.h>

namespace NMiniCluster {
    class TClusterDistributors: public TNodeSet<TController, NMonolithDistributor::TServer> {
        virtual void SetPorts(TNode<TController, TServer>& node, ui16 firstPort) const;
        virtual TString GetBinName() const;
    };
}
