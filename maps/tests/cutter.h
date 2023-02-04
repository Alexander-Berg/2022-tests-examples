#pragma once

#include "../cut/cut_line.h"
#include "../cut/cutter.h"
#include "base.h"

#include <yandex/maps/wiki/geom_tools/common.h>

namespace maps {
namespace wiki {
namespace geom_tools {
namespace test {

struct PolygonCutterTestData : public TestDataBase {
    PolygonCutterTestData(
            geolib3::Polygon2 polygon_,
            CutLine cutLine_,
            PolygonCutter::Result expectedResult_)
        : polygon(std::move(polygon_))
        , cutLine(std::move(cutLine_))
        , expectedResult(std::move(expectedResult_))
    {}

    geolib3::Polygon2 polygon;
    CutLine cutLine;
    PolygonCutter::Result expectedResult;
};

} // namespace test
} // namespace geom_tools
} // namespace wiki
} // namespace maps
