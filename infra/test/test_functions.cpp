#include "test_functions.h"

#include <infra/pod_agent/libs/behaviour/bt/nodes/base/mock_node.h>

namespace NInfra::NPodAgent {

namespace {

static TLogger logger({});

} // namespace

TTreePtr GetEmptyTree(const TString& treeId) {
    return new TTree(logger, treeId, new TMockNode({1, treeId + "_empty_tree_root"}, TTickResult(ENodeStatus::SUCCESS)));
}

} // namespace NInfra::NPodAgent
