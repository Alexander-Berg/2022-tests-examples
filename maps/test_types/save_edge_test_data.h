#pragma once

#include "common.h"

namespace maps {
namespace wiki {
namespace topo {
namespace test {

class SaveEdgeTestData : public CommonTestData
{
public:

    SaveEdgeTestData(
            const std::string& description,
            test::MockStorage storage,
            SourceEdgeID edgeId,
            const geolib3::Polyline2& newPolyline,
            const geolib3::Polyline2& alignedPolyline,
            geolib3::PointsVector splitPoints,
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
        , restrictions_(restrictions)
        , edgeId_(edgeId)
        , newPolyline_(newPolyline)
        , alignedPolyline_(alignedPolyline)
        , splitPoints_(std::move(splitPoints))
        , splitEdges_(std::move(splitEdges))
    {}

    const SourceEdgeID& edgeId() const { return edgeId_; }
    const geolib3::Polyline2& newPolyline() const { return newPolyline_; }
    const geolib3::Polyline2& alignedPolyline() const { return alignedPolyline_; }
    const geolib3::PointsVector& splitPoints() const { return splitPoints_; }
    const TopologyRestrictions& restrictions() const { return restrictions_; }
    const SplitEdges& splitEdges() const { return splitEdges_; }

private:
    TopologyRestrictions restrictions_;
    SourceEdgeID edgeId_;
    geolib3::Polyline2 newPolyline_;
    geolib3::Polyline2 alignedPolyline_;
    geolib3::PointsVector splitPoints_;
    SplitEdges splitEdges_;
};

} // namespace test
} // namespace topo
} // namespace wiki
} // namespace maps
