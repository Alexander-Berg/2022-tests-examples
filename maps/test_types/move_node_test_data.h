#pragma once

#include "common.h"

namespace maps {
namespace wiki {
namespace topo {
namespace test {

class MoveNodeTestData : public CommonTestData
{
public:

    enum Type {Correct, Incorrect, WithoutCheck};

    MoveNodeTestData(
            const std::string& description,
            test::MockStorage storage,
            Type type,
            NodeID nodeId,
            const geolib3::Point2& pos,
            const TopologyRestrictions& restrictions,
            OptionalNodeID mergedNodeId,
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
        , type_(type)
        , nodeId_(nodeId)
        , pos_(pos)
        , restrictions_(restrictions)
        , mergedNodeId_(std::move(mergedNodeId))
        , splitEdges_(std::move(splitEdges))
    {
        REQUIRE(type != Incorrect || CommonTestData::expectedError(),
            "No expected error specified for incorrect test");
    }

    Type type() const { return type_; }
    NodeID nodeId() const { return nodeId_; }
    const geolib3::Point2& pos() const { return pos_; }
    const TopologyRestrictions& restrictions() const { return restrictions_; }
    OptionalNodeID mergedNodeId() const { return mergedNodeId_; }
    const SplitEdges& splitEdges() const { return splitEdges_; }

private:
    Type type_;
    NodeID nodeId_;
    geolib3::Point2 pos_;
    TopologyRestrictions restrictions_;
    OptionalNodeID mergedNodeId_;
    SplitEdges splitEdges_;
};

} // namespace test
} // namespace topo
} // namespace wiki
} // namespace maps
