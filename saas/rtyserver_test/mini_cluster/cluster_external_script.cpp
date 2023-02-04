#include "cluster_external_script.h"

namespace NMiniCluster {
    void TClusterExternalScripts::SetPorts(TNode<TClusterExternalScripts::TController, TClusterExternalScripts::TServer>& node, ui16 firstPort) const {
        node.AddPatch("SearchProxy.Port", ToString(firstPort));
    }

    TString TClusterExternalScripts::GetBinName() const {
        return "external-script";
    }
}

typedef NRTYExternalScript::TServer TExternalScriptServer;
