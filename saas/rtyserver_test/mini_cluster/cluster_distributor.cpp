#include "cluster_distributor.h"

namespace NMiniCluster {
    void TClusterDistributors::SetPorts(TNode<TClusterDistributors::TController, TClusterDistributors::TServer>& node, ui16 firstPort) const {
        node.AddPatch("ClusterMap.ServiceMaps.ServiceMap.Port", ToString(firstPort));
    }

    TString TClusterDistributors::GetBinName() const {
        return "monolith";
    }
}
