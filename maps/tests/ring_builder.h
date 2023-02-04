#pragma once

#include "base.h"

#include <yandex/maps/wiki/geom_tools/common.h>

#include <boost/optional.hpp>

namespace maps {
namespace wiki {
namespace geom_tools {
namespace test {

struct RingBuilderTestData : public TestDataBase {
    RingBuilderTestData(
            geolib3::PolylinesVector geoms_,
            double tolerance_,
            boost::optional<geolib3::LinearRing2> expectedRing_)
        : geoms(std::move(geoms_))
        , tolerance(tolerance_)
        , expectedRing(std::move(expectedRing_))
    {}

    geolib3::PolylinesVector geoms;
    double tolerance;
    boost::optional<geolib3::LinearRing2> expectedRing;
};

} // namespace test
} // namespace geom_tools
} // namespace wiki
} // namespace maps
