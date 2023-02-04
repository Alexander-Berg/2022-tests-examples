#include "test_data.h"
#include <maps/factory/libs/db/mosaic.h>
#include <maps/factory/libs/db/mosaic_gateway.h>

#include <maps/factory/libs/db/mosaic_source.h>
#include <maps/factory/libs/db/mosaic_source_gateway.h>

#include <maps/factory/libs/unittest/fixture.h>

#include <maps/libs/geolib/include/test_tools/comparison.h>
#include <maps/libs/geolib/include/transform.h>

#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/testing/gmock_in_unittest/gmock.h>

#include <pqxx/pqxx>

#include <string>

namespace maps::factory::db::tests {

using namespace table::alias;

Y_UNIT_TEST_SUITE(test_mosaic_gateway) {

Y_UNIT_TEST(create_mosaic)
{
    const geolib3::MultiPolygon2 TEST_MOSAIC_GEOMETRY{{
        geolib3::Polygon2{geolib3::PointsVector{
            {0.0, 0.0},
            {2.0, 0.0},
            {2.0, 2.0},
            {0.0, 2.0},
            {0.0, 0.0}
        }}
    }};

    Mosaic m(
        /* mosaicSourceId = */ 0, //FIXME: MosaicSource is not supported
        /* zOrder = */ 31337,
        /* minZoom = */ 10,
        /* maxZoom = */ 18,
        TEST_MOSAIC_GEOMETRY
    );
    m.setModifiedNowBy("John");

    unittest::Fixture fixture;
    {
        pqxx::connection conn(fixture.postgres().connectionString());
        pqxx::work txn(conn);
        MosaicGateway(txn).insert(m);
        txn.commit();
        EXPECT_EQ(m.id(), 1);
    }

    {
        pqxx::connection conn(fixture.postgres().connectionString());
        pqxx::work txn(conn);
        Mosaic loadedMosaic = MosaicGateway(txn).loadById(m.id());

        EXPECT_EQ(m.zOrder(), loadedMosaic.zOrder());
        EXPECT_EQ(m.minZoom(), loadedMosaic.minZoom());
        EXPECT_EQ(m.maxZoom(), loadedMosaic.maxZoom());
        EXPECT_TRUE(geolib3::test_tools::approximateEqual(
            m.mercatorGeom(), loadedMosaic.mercatorGeom(), 1e-9));
        EXPECT_EQ(m.modifiedBy(), loadedMosaic.modifiedBy());
        EXPECT_EQ(m.modifiedAt(), loadedMosaic.modifiedAt());
    }
}

Y_UNIT_TEST(test_load_mosaic_with_mosaic_source_geom)
{
    const std::string NAME = "test_mosaic";
    const geolib3::MultiPolygon2 MOSAIC_SOURCE_ORIGINAL_GEOMETRY = TEST_MOSAIC_SOURCE_GEOMETRY;
    auto translation = geolib3::SimpleGeometryTransform2(geolib3::AffineTransform2::translate(10, 10));
    const geolib3::MultiPolygon2 MOSAIC_SOURCE_GEOMETRY =
        translation(MOSAIC_SOURCE_ORIGINAL_GEOMETRY, geolib3::TransformDirection::Forward);

    const geolib3::MultiPolygon2 MOSAIC_GEOMETRY =
        translation(MOSAIC_SOURCE_GEOMETRY, geolib3::TransformDirection::Forward);

    unittest::Fixture fixture;
    pqxx::connection conn(fixture.postgres().connectionString());

    int64_t msId = 0;

    {
        auto ms1 = MosaicSource(NAME + "1");
        ms1.setMercatorGeoms(MOSAIC_SOURCE_GEOMETRY, MOSAIC_SOURCE_ORIGINAL_GEOMETRY);

        auto ms2 = MosaicSource(NAME + "2");
        ms2.setMercatorGeoms(MOSAIC_SOURCE_GEOMETRY, MOSAIC_SOURCE_GEOMETRY);

        pqxx::work txn(conn);
        MosaicSourceGateway(txn).insert(ms1);
        MosaicSourceGateway(txn).insert(ms2);
        msId = ms1.id();
        txn.commit();
    }

    Mosaic testMosaic1(
        msId,
        /* zOrder = */ 31337,
        /* minZoom = */ 10,
        /* maxZoom = */ 18,
        MOSAIC_GEOMETRY
    );

    Mosaic testMosaic2(
        msId,
        /* zOrder = */ 31337,
        /* minZoom = */ 10,
        /* maxZoom = */ 18,
        MOSAIC_GEOMETRY
    );

    {
        pqxx::work txn(conn);
        MosaicGateway(txn).insert(testMosaic1);
        MosaicGateway(txn).insert(testMosaic2);
        txn.commit();
    }

    {
        pqxx::work txn(conn);
        Mosaics loadedMosaics =
            sql_chemistry::Gateway<table::MosaicWithMosaicSourceGeom>(txn).load(
                _Mosaic::mosaicSourceId == _MosaicSource::id);

        ASSERT_EQ(loadedMosaics.size(), 2u);

        EXPECT_TRUE(geolib3::test_tools::approximateEqual(
            MOSAIC_SOURCE_ORIGINAL_GEOMETRY,
            loadedMosaics.at(0).mercatorGeom(),
            1e-9));

        EXPECT_EQ(
            sql_chemistry::Gateway<table::MosaicWithMosaicSourceGeom>(txn)
                .load(
                    _Mosaic::mosaicSourceId == _MosaicSource::id &&
                    table::Mosaic::id == testMosaic1.id())
                .size(),
            1u);

        EXPECT_EQ(
            sql_chemistry::Gateway<table::MosaicWithMosaicSourceGeom>(txn)
                .load(
                    _Mosaic::mosaicSourceId == _MosaicSource::id,
                    sql_chemistry::orderBy(table::Mosaic::id).limit(1))
                .size(),
            1u);
    }
}

Y_UNIT_TEST(test_load_zoom_area_info)
{
    const std::string TEST_MOSAIC_NAME = "test_mosaic";
    const double sizeSize = 10;
    const double x = 4000000;
    const double y = 7000000;
    const geolib3::MultiPolygon2 TEST_MOSAIC_GEOMETRY{{
        geolib3::Polygon2{geolib3::PointsVector{
            {x, y},
            {x + sizeSize, y},
            {x + sizeSize, y + sizeSize},
            {x, y + sizeSize},
            {x, y}
        }}
    }};

    Mosaic m(
        /* mosaicSourceId = */ 0, //FIXME: MosaicSource is not supported
        /* zOrder = */ 31337,
        /* minZoom = */ 10,
        /* maxZoom = */ 18,
        TEST_MOSAIC_GEOMETRY
    );

    unittest::Fixture fixture;
    pqxx::connection conn(fixture.postgres().connectionString());
    {
        pqxx::work txn(conn);
        MosaicGateway(txn).insert(m);
        txn.commit();
        EXPECT_EQ(m.id(), 1);
    }

    {
        pqxx::work txn(conn);
        auto mosaicZoomAreas = loadZoomAreaInfo(MosaicGateway(txn), sql_chemistry::AnyFilter());
        ASSERT_EQ(mosaicZoomAreas.size(), 1u);
        EXPECT_EQ(mosaicZoomAreas[0].minZoom, m.minZoom());
        EXPECT_EQ(mosaicZoomAreas[0].maxZoom, m.maxZoom());
        EXPECT_EQ(mosaicZoomAreas[0].mercatorAreaSqrMeters, 100);
        EXPECT_NEAR(mosaicZoomAreas[0].geodeticAreaSqrMeters, 35.9, 0.1);
    }
}

} // suite
} // namespace maps::factory::db::tests

