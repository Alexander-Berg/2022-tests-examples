#include "fixtures.h"

#include <maps/libs/geolib/include/polygon.h>
#include <yandex/maps/wiki/common/aoi.h>
#include <yandex/maps/wiki/unittest/arcadia.h>

#include <library/cpp/testing/unittest/registar.h>

namespace maps::wiki::common::tests {

namespace {

const uint64_t ANY_COMMIT_ID = 1;
const uint64_t ANY_AOI_ID = 123;
const double ANY_AREA = 1;
const geolib3::Polygon2 POLYGON({{0, 0}, {0, 10}, {10, 10}, {10, 0}});

const auto AOI_PARAMS = [](){
    AoiParams params;

    params.aoiId = ANY_AOI_ID;
    params.commitId = ANY_COMMIT_ID;
    params.name = "smth";
    params.type = 0;
    params.area = ANY_AREA;
    params.polygonMerc = POLYGON;

    return params;
}();

} // namespace

Y_UNIT_TEST_SUITE(aoi_tests) {

Y_UNIT_TEST_F(inside_aoi, DBFixture)
{
    auto viewTrunkTxn = pool().masterWriteableTransaction();

    addAoiRegionToViewTrunk(AOI_PARAMS, viewTrunkTxn.get());

    auto aois = calculateAoisContainingPosition(
        geolib3::Point2(5, 5), viewTrunkTxn.get());

    UNIT_ASSERT(aois.size() == 1);
    UNIT_ASSERT(*aois.begin() == ANY_AOI_ID);
}

Y_UNIT_TEST_F(outside_aoi, DBFixture)
{
    auto viewTrunkTxn = pool().masterWriteableTransaction();

    addAoiRegionToViewTrunk(AOI_PARAMS, viewTrunkTxn.get());

    auto aois = calculateAoisContainingPosition(
        geolib3::Point2(11, 11), viewTrunkTxn.get());

    UNIT_ASSERT(aois.empty());
}

} // Y_UNIT_TEST_SUITE(aoi_tests)

} // namespace maps::wiki::common::tests
