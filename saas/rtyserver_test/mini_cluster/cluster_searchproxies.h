#pragma once

#include "mini_cluster_abstract.h"
#include <saas/searchproxy/core/searchproxyserver.h>
#include <saas/library/daemon_base/daemon/controller.h>
#include <saas/library/daemon_base/daemon/daemon.h>

namespace NMiniCluster {
    class TClusterSearchproxies : public TNodeSet<TController, TSearchProxyServer> {
        virtual void SetPorts(TNode<TController, TServer>& node, ui16 firstPort) const;
        virtual TString GetBinName() const;
    };
}
