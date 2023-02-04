#include "cluster_indexerproxies.h"

namespace NMiniCluster {
    void TClusterIndexerproxies::SetPorts(TNode<TClusterIndexerproxies::TController, TClusterIndexerproxies::TServer>& node, ui16 firstPort) const {
        node.AddPatch("Proxy.HttpOptions.Port", ToString(firstPort));
        node.AddPatch("Proxy.NehOptions.Port", ToString(firstPort + 1));
        node.AddPatch("Proxy.Export.Port", ToString(firstPort + 2));
    }

    TString TClusterIndexerproxies::GetBinName() const {
        return "indexerproxy";
    }
}
