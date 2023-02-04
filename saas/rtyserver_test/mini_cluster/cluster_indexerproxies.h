#pragma once

#include "mini_cluster_abstract.h"
#include <saas/indexerproxy/server/server.h>
#include <saas/library/daemon_base/daemon/controller.h>
#include <saas/library/daemon_base/daemon/daemon.h>

namespace NMiniCluster {
    class TClusterIndexerproxies : public TNodeSet<TController, THttpProxyRTYServer> {
        virtual void SetPorts(TNode<TController, TServer>& node, ui16 firstPort) const;
        virtual TString GetBinName() const;
    };
}
