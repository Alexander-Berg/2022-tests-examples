#pragma once

#include "base.h"

#include <yandex/maps/wiki/geom_tools/common.h>

#include <boost/optional.hpp>

namespace maps {
namespace wiki {
namespace geom_tools {
namespace test {

struct PolygonBuilderTestData : public TestDataBase {
    PolygonBuilderTestData(
            GeolibLinearRingVector shells_,
            GeolibLinearRingVector holes_,
            boost::optional<GeolibPolygonVector> expectedPolygons_)
        : shells(std::move(shells_))
        , holes(std::move(holes_))
        , expectedPolygons(std::move(expectedPolygons_))
    {}

    GeolibLinearRingVector shells;
    GeolibLinearRingVector holes;
    boost::optional<GeolibPolygonVector> expectedPolygons;
};

} // namespace test
} // namespace geom_tools
} // namespace wiki
} // namespace maps
