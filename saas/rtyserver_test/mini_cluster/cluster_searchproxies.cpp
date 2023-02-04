#include "cluster_searchproxies.h"

namespace NMiniCluster {
    void TClusterSearchproxies::SetPorts(TNode<TClusterSearchproxies::TController, TClusterSearchproxies::TServer>& node, ui16 firstPort) const {
        node.AddPatch("SearchProxy.Port", ToString(firstPort));
    }

    TString TClusterSearchproxies::GetBinName() const {
        return "searchproxy";
    }
}
