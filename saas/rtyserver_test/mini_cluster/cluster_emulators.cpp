#include "cluster_emulators.h"

namespace NMiniCluster {
    void TClusterEmulators::SetPorts(TNode<TClusterEmulators::TController, TClusterEmulators::TServer>& node, ui16 firstPort) const {
        node.AddPatch("Emulator.Search.HttpOptions.Port", ToString(firstPort));
        node.AddPatch("Emulator.Controller.HttpOptions.Port", ToString(firstPort + 1));
        node.AddPatch("Emulator.Index.HttpOptions.Port", ToString(firstPort + 2));
    }

    TString TClusterEmulators::GetBinName() const {
        return "rtyserver_emulator";
    }
}
