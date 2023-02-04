#include <maps/wikimap/mapspro/libs/views/include/regions.h>

#include <maps/wikimap/mapspro/libs/views/tests/helpers/contour_object_geom.h>

#include <yandex/maps/wiki/unittest/arcadia.h>

#include <library/cpp/testing/unittest/registar.h>

namespace maps::wiki::views::tests {

using namespace contour_object_geom;

Y_UNIT_TEST_SUITE_F(should_not_get_regions, unittest::ArcadiaDbFixture) {
    Y_UNIT_TEST(if_no_regions)
    {
        auto txn = pool().masterWriteableTransaction();
        UNIT_ASSERT(trunk::getRegions(*txn, BBOX_INSIDE_DEFAULT_REGION).empty());
    }

    Y_UNIT_TEST(if_contour_object_is_not_ad)
    {
        auto txn = pool().masterWriteableTransaction();
        ContourObjectGeom().partOfRegion(DEFAULT_REGION).domainAttrs({{"cat:ad_neutral", "1"}}).insert(*txn);
        UNIT_ASSERT(trunk::getRegions(*txn, BBOX_INSIDE_DEFAULT_REGION).empty());
    }

    Y_UNIT_TEST(if_contour_object_is_not_part_of_a_region)
    {
        {   // both types of `srv:is_part_of_region...` attributes absent
            auto txn = pool().masterWriteableTransaction();
            ContourObjectGeom().insert(*txn);
            UNIT_ASSERT(trunk::getRegions(*txn, BBOX_INSIDE_DEFAULT_REGION).empty());
        }

        {   // `srv:is_part_of_region_<region>` attribute absent
            auto txn = pool().masterWriteableTransaction();
            ContourObjectGeom().serviceAttrs({{SRV_IS_PART_OF_REGION, "1"}}).insert(*txn);
            UNIT_ASSERT(trunk::getRegions(*txn, BBOX_INSIDE_DEFAULT_REGION).empty());
        }

        {   // `srv:is_part_of_region` attribute absent
            auto txn = pool().masterWriteableTransaction();
            ContourObjectGeom().serviceAttrs({{srvIsPartOfRegionAttrName(DEFAULT_REGION), "1"}}).insert(*txn);
            UNIT_ASSERT(trunk::getRegions(*txn, BBOX_INSIDE_DEFAULT_REGION).empty());
        }
    }

    Y_UNIT_TEST(if_bbox_outside_contour_object)
    {
        auto txn = pool().masterWriteableTransaction();
        ContourObjectGeom().partOfRegion(DEFAULT_REGION).insert(*txn);
        UNIT_ASSERT(trunk::getRegions(*txn, BBOX_OUTSIDE_DEFAULT_REGION).empty());
    }
}

Y_UNIT_TEST_SUITE_F(should_get_regions, unittest::ArcadiaDbFixture) {
    Y_UNIT_TEST(if_bbox_inside_contour_object)
    {
        auto txn = pool().masterWriteableTransaction();
        ContourObjectGeom().partOfRegion(DEFAULT_REGION).insert(*txn);
        UNIT_ASSERT_EQUAL(
            trunk::getRegions(*txn, BBOX_INSIDE_DEFAULT_REGION),
            revision::DBIDSet({DEFAULT_REGION})
        );
    }

    Y_UNIT_TEST(if_bbox_intersect_contour_object)
    {
        auto txn = pool().masterWriteableTransaction();
        ContourObjectGeom().partOfRegion(DEFAULT_REGION).insert(*txn);
        UNIT_ASSERT_EQUAL(
            trunk::getRegions(*txn, BBOX_INTERSECT_DEFAULT_REGION),
            revision::DBIDSet({DEFAULT_REGION})
        );
    }

    Y_UNIT_TEST(if_bbox_intersect_several_contour_objects)
    {
        auto txn = pool().masterWriteableTransaction();

        //     ^
        //     |
        //  50 |   +---------+---------+---------+
        //     |   |       1 |       2 |       3 |
        //     |   |    +----+----+    |         |
        //     |   |    |    |    |    |         |
        //  48 |   +----+----+----+----+---------+
        //     |        |    bbox |
        //     |        +---------+
        //     |
        //     +------------------------------------->
        //         48        50        52        54
        ContourObjectGeom(geolib3::Polygon2({{48, 48}, {50, 48}, {50, 50}, {48, 50}})).partOfRegion(1).insert(*txn);
        ContourObjectGeom(geolib3::Polygon2({{50, 48}, {52, 48}, {52, 50}, {50, 50}})).partOfRegion(2).insert(*txn);
        ContourObjectGeom(geolib3::Polygon2({{52, 48}, {54, 48}, {54, 50}, {52, 50}})).partOfRegion(3).insert(*txn);

        UNIT_ASSERT_EQUAL(
            trunk::getRegions(*txn, {{49, 47}, {51, 49}}),
            revision::DBIDSet({1, 2})
        );
    }

    Y_UNIT_TEST(if_contour_object_belongs_to_several_regions)
    {
        auto txn = pool().masterWriteableTransaction();

        ContourObjectGeom().partOfRegion(24).partOfRegion(42).insert(*txn);

        UNIT_ASSERT_EQUAL(
            trunk::getRegions(*txn, BBOX_INSIDE_DEFAULT_REGION),
            revision::DBIDSet({24, 42})
        );
    }
}

} // namespace maps::wiki::views::tests
