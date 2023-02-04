#pragma once

#include "../events_data.h"
#include "mock_storage.h"
#include "../test_tools/geom_comparison.h"

#include <yandex/maps/wiki/topo/common.h>
#include <yandex/maps/wiki/topo/exception.h>

#include <maps/libs/geolib/include/point.h>
#include <maps/libs/geolib/include/polyline.h>

#include <boost/optional.hpp>

#include <vector>
#include <set>

namespace maps {
namespace wiki {
namespace topo {
namespace test {

const boost::optional<size_t> UNLIMITED_MAX_INTERSECTED_EDGES = boost::none;
const boost::optional<size_t> UNLIMITED_MAX_INTERSECTIONS_WITH_EDGE = boost::none;
const boost::optional<size_t> NO_INTERSECTED_EDGES = boost::optional<size_t>(0);
const boost::optional<size_t> NO_INTERSECTIONS_WITH_EDGE = boost::optional<size_t>(0);


const boost::optional<ErrorCode> NO_ERROR = boost::none;

enum class GravityType { Tolerance, VertexGravity, JunctionGravity };

typedef std::set<GravityType> GravityTypesSet;

struct PointGravity
{
    geolib3::Point2 point;
    GravityType gravityType;
};

struct PrintInfo {
    geolib3::PointsVector pointsOfInterest;

    std::vector<PointGravity> pointsGravity;
    std::map<EdgeID, GravityTypesSet> edgesGravity;
    GravityTypesSet newGeomGravities;
};

struct SplitEdgeData
{
    SourceEdgeID sourceId;
    EdgeIDVector resultedEdges;
};

typedef std::vector<SplitEdgeData> SplitEdges;

class CommonTestData {
public:
    CommonTestData(
            const std::string& desc,
            MockStorage original,
            MockStorage result,
            boost::optional<PrintInfo> printInfo,
            boost::optional <ErrorCode> expectedError)
        : desc_(desc)
        , original_(std::move(original))
        , result_(std::move(result))
        , printInfo_(printInfo)
        , expectedError_(expectedError)
    {}

    void setName(const std::string& name) { name_ = name; }

    const std::string& name() const { return name_; }
    const std::string& description() const { return desc_; }

    const MockStorage& original() const { return original_; }
    const MockStorage& result() const { return result_; }

    const boost::optional<PrintInfo>& printInfo() const { return printInfo_; }
    const boost::optional<ErrorCode>& expectedError() const { return expectedError_; }

protected:
    std::string name_;
    std::string desc_;

    MockStorage original_;
    MockStorage result_;

    boost::optional<PrintInfo> printInfo_;
    boost::optional <ErrorCode> expectedError_;
};

} // namespace test
} // namespace topo
} // namespace wiki
} // namespace maps
