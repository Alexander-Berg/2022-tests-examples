#include <maps/wikimap/mapspro/libs/poi_conflicts/include/poi_conflicts.h>
#include <library/cpp/testing/unittest/registar.h>

Y_UNIT_TEST_SUITE(simple_tests)
{
Y_UNIT_TEST(test_poi_conflicts_construction)
{
    UNIT_ASSERT_NO_EXCEPTION(maps::wiki::poi_conflicts::PoiConflicts());
}

Y_UNIT_TEST(test_poi_conflicts_detection)
{
    const maps::wiki::poi_conflicts::PoiConflicts poiConflicts;
    const double maxDistance = poiConflicts.maxConflictDistanceMercator();
    const auto maxZoom = poiConflicts.conflictZoom(maxDistance);
    UNIT_ASSERT(maxZoom);
    UNIT_ASSERT_EQUAL(*maxZoom, 19);
    const auto zoom1 = poiConflicts.conflictZoom(maxDistance / 4);
    UNIT_ASSERT(zoom1);
    UNIT_ASSERT(*zoom1 > *maxZoom);
    UNIT_ASSERT(!poiConflicts.conflictZoom(maxDistance * 2));
}
} // Y_UNIT_TEST_SUITE
