#pragma once

#include "cutter.h"
#include "partitioner.h"
#include "polygon_builder.h"
#include "ring_builder.h"

#include <yandex/maps/wiki/test_tools/suite.h>

#include <maps/libs/common/include/exception.h>

#include <memory>
#include <map>
#include <set>
#include <string>

namespace maps {
namespace wiki {
namespace geom_tools {
namespace test {

typedef test_tools::TestSuitesHolder<
    PolygonCutterTestData,
    PolygonPartitionTestData,
    PolygonBuilderTestData,
    RingBuilderTestData>
MainTestSuite;

MainTestSuite* mainTestSuite();

#include <yandex/maps/wiki/test_tools/helper_macro.h>

} // namespace test
} // namespace geom_tools
} // namespace wiki
} // namespace maps
