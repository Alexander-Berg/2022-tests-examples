#pragma once

#include "mini_cluster_abstract.h"
#include <saas/rtyserver/controller/controller.h>
#include <saas/tools/rtyserver_emulator/server/rtyserver_emulator.h>

namespace NMiniCluster {
    class TClusterEmulators : public TNodeSet<TController, TEmulator> {
        virtual void SetPorts(TNode<TController, TServer>& node, ui16 firstPort) const;
        virtual TString GetBinName() const;
    };
}
