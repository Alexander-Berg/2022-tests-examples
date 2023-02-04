#pragma once

#include "common.h"

#include <yandex/maps/wiki/topo/editor.h>

namespace maps {
namespace wiki {
namespace topo {
namespace test {

class SnapNodesTestData : public CommonTestData
{
public:

    SnapNodesTestData(
            const std::string& description,
            test::MockStorage storage,
            const NodeIDSet& nodeIds,
            const TopologyRestrictions& restrictions,
            SplitEdges splitEdges,
            test::MockStorageDiff resultDiff,
            boost::optional<PrintInfo> printInfo = boost::none,
            boost::optional<ErrorCode> expectedError = boost::none)
        : CommonTestData(
            description,
            storage,
            MockStorage(storage, resultDiff),
            printInfo,
            expectedError)
        , nodeIds_(nodeIds)
        , restrictions_(restrictions)
        , splitEdges_(std::make_shared<SplitEdges>(std::move(splitEdges)))
    {}

    const NodeIDSet& nodeIds() const { return nodeIds_; }
    const TopologyRestrictions& restrictions() const { return restrictions_; }
    const SplitEdges& splitEdges() const { return *splitEdges_; }

private:
    NodeIDSet nodeIds_;
    TopologyRestrictions restrictions_;
    std::shared_ptr<SplitEdges> splitEdges_;
};

} // namespace test
} // namespace topo
} // namespace wiki
} // namespace maps
