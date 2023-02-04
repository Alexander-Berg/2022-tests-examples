#pragma once

#include "common.h"

namespace maps {
namespace wiki {
namespace topo {
namespace test {

class MergeEdgesTestData : public CommonTestData
{
public:
    MergeEdgesTestData(
            const std::string& description,
            test::MockStorage storage,
            NodeID commonNodeId,
            const TopologyRestrictions& restrictions,
            test::MockStorageDiff resultDiff,
            boost::optional<ErrorCode> expectedError = boost::none)
        : CommonTestData(
              description,
              storage,
              MockStorage(storage, resultDiff),
              boost::none,
              expectedError)
        , commonNodeId_(commonNodeId)
        , restrictions_(restrictions)
    {}

    NodeID commonNodeId() const { return commonNodeId_; }
    const TopologyRestrictions& restrictions() const { return restrictions_; }

private:
    NodeID commonNodeId_;
    TopologyRestrictions restrictions_;
};

} // namespace test
} // namespace topo
} // namespace wiki
} // namespace maps
