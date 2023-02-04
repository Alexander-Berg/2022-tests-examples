#pragma once

#include "mini_cluster_abstract.h"
#include <saas/rtyserver/controller/controller.h>

namespace NMiniCluster {
    class TClusterBackends : public TNodeSet<TRTYController, TRTYServer> {
        virtual void SetPorts(TNode<TController, TServer>& node, ui16 firstPort) const;
        virtual TString GetBinName() const;
    };
}
