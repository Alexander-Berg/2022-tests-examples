#include "helpers.h"

#include <library/cpp/testing/unittest/registar.h>
#include <maps/wikimap/mapspro/libs/misc_point_to_indoor/include/point_to_indoor.h>
#include <maps/libs/geolib/include/conversion.h>
#include <maps/libs/geolib/include/distance.h>
#include <yandex/maps/wiki/revision/revisionsgateway.h>
#include <yandex/maps/wiki/tasks/tool_commands.h>

namespace maps::wiki::misc::tests {

Y_UNIT_TEST_SUITE(get_area_object_id) {

Y_UNIT_TEST_F(get_indoor_by_point_and_level_test, DBFixture)
{
    // The scheme of existing indoors with geo coordinates is:
    //
    // (coinsiding indoor levels "1" and "2"
    //    and indoor objects ids 1 and 2)
    //                 |
    //     ^ Y, Lat    v
    //     | 55.001+-------+          +-------+
    //     |       |       |          |       |
    //     |       |(55,55)|          |       | <- indoor level = "1"
    //     |       |       |          |       |    indoor object id = 11
    //     | 54.999+-------+          +-------+
    //     |    36.999   37.001    37.002   37.004
    //     |
    //     +-------------------------------------->
    //                                            X, Lon

    importDataToRevision(pool(), arcadiaDataPath("indoor_levels.json"));
    syncViewWithRevision(*this);

    auto txn = pool().slaveTransaction();
    txn->exec("SET search_path=vrevisions_trunk,public");

    { // point is inside single indoor with requested indoor_level
        const auto indoorId = getIndoorObjectId(
            *txn,
            revision::TRUNK_BRANCH_ID,
            geolib3::geoPoint2Mercator(geolib3::Point2(37.003, 55.0)),
            0, // searchRadius
            "1" // indoor level
        );
        UNIT_ASSERT(indoorId);
        UNIT_ASSERT_EQUAL(*indoorId, 11);
    }

    { // point is inside single indoor with different indoor_level
        const auto indoorId = getIndoorObjectId(
            *txn,
            revision::TRUNK_BRANCH_ID,
            geolib3::geoPoint2Mercator(geolib3::Point2(37.003, 55.0)),
            0, // searchRadius
            "13" // indoor level
        );
        UNIT_ASSERT(!indoorId);
    }

    { // point is inside two overlaping indoor_levels
        const auto indoorId = getIndoorObjectId(
            *txn,
            revision::TRUNK_BRANCH_ID,
            geolib3::geoPoint2Mercator(geolib3::Point2(37.0, 55.0)),
            0, // searchRadius
            "2" // indoor level
        );
        UNIT_ASSERT(indoorId);
        UNIT_ASSERT_EQUAL(*indoorId, 2);
    }

    { // point is outside of any indoors (searchRadius = 0)
        const auto indoorId = getIndoorObjectId(
            *txn,
            revision::TRUNK_BRANCH_ID,
            geolib3::geoPoint2Mercator(geolib3::Point2(37.0015, 55.0)),
            0, // searchRadius
            "1" // indoor level
        );
        UNIT_ASSERT(!indoorId);
    }

    { // point is outside of any indoor
        const auto queryPoint = geolib3::geoPoint2Mercator(geolib3::Point2(37.0011, 55.0011));
        const auto nearestIndoorPoint = geolib3::geoPoint2Mercator(geolib3::Point2(37.001, 55.001));

        const auto ratio = geolib3::MercatorRatio::fromMercatorPoint(queryPoint);
        const auto distanceMeters = ratio.toMeters(geolib3::distance(queryPoint, nearestIndoorPoint));

        { // searchRadius < distance to nearest indoor point
            const auto searchRadius = 0.9999 * distanceMeters;
            const auto indoorId = getIndoorObjectId(
                *txn,
                revision::TRUNK_BRANCH_ID,
                queryPoint,
                searchRadius,
                "1" // indoor level
                );
            UNIT_ASSERT(!indoorId);
        }
        { // searchRadius > distance to nearest indoor point
            const auto searchRadius = 1.0001 * distanceMeters;
            const auto indoorId = getIndoorObjectId(
                *txn,
                revision::TRUNK_BRANCH_ID,
                queryPoint,
                searchRadius,
                "2" // indoor level
                );
            UNIT_ASSERT(indoorId);
            UNIT_ASSERT_EQUAL(*indoorId, 2);
        }
    }

    { // nearest of indoor objects with same appropriate indoor_level is found
        const auto queryPoint = geolib3::geoPoint2Mercator(geolib3::Point2(36.998, 55.0));
        const auto furthestIndoorLevelCenter = geolib3::geoPoint2Mercator(geolib3::Point2(37.003, 55.0));

        const auto ratio = geolib3::MercatorRatio::fromMercatorPoint(queryPoint);
        const auto searchRadius = ratio.toMeters(geolib3::distance(queryPoint, furthestIndoorLevelCenter));

        const auto indoorId = getIndoorObjectId(
            *txn,
            revision::TRUNK_BRANCH_ID,
            queryPoint,
            searchRadius,
            "1" // indoor level
        );
        UNIT_ASSERT(indoorId);
        UNIT_ASSERT_EQUAL(*indoorId, 1);
    }
}

} // Y_UNIT_TEST_SUITE

} // namespace maps::wiki::misc::tests
