#pragma once

#include "mini_cluster_abstract.h"
#include <saas/rtyserver_test/external_script/external_script.h>
#include <saas/library/daemon_base/daemon/controller.h>
#include <saas/library/daemon_base/daemon/daemon.h>

namespace NMiniCluster {
    class TClusterExternalScripts : public TNodeSet<TController, NRTYExternalScript::TServer> {
        virtual void SetPorts(TNode<TController, TServer>& node, ui16 firstPort) const;
        virtual TString GetBinName() const;
    };
}
