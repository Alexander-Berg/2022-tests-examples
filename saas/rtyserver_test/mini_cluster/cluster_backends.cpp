#include "cluster_backends.h"

namespace NMiniCluster {
    void TClusterBackends::SetPorts(TNode<TClusterBackends::TController, TClusterBackends::TServer>& node, ui16 firstPort) const {
        node.AddPatch("Server.Searcher.HttpOptions.Port", ToString(firstPort));
        node.AddPatch("Server.BaseSearchersServer.Port", ToString(firstPort + 1));
        node.AddPatch("Server.Indexer.Common.HttpOptions.Port", ToString(firstPort + 2));
    }

    TString TClusterBackends::GetBinName() const {
        return "rtyserver";
    }
}

