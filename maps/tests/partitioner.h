#pragma once

#include "base.h"

#include <yandex/maps/wiki/geom_tools/common.h>

namespace maps {
namespace wiki {
namespace geom_tools {
namespace test {

struct PolygonPartitionTestData : public TestDataBase {
    PolygonPartitionTestData(
            GeolibPolygonVector polygons_,
            size_t maxVertices_,
            double minSize_,
            double tolerance_,
            GeolibPolygonVector expected_)
        : polygons(std::move(polygons_))
        , maxVertices(maxVertices_)
        , minSize(minSize_)
        , tolerance(tolerance_)
        , expected(expected_)
    {}

    GeolibPolygonVector polygons;
    size_t maxVertices;
    double minSize;
    double tolerance;
    GeolibPolygonVector expected;
};

} // namespace test
} // namespace geom_tools
} // namespace wiki
} // namespace maps
