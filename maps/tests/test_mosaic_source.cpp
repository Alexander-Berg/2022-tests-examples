#include <maps/factory/libs/db/mosaic_source.h>
#include <maps/factory/libs/db/mosaic_source_gateway.h>

#include <maps/factory/libs/unittest/tests_common.h>
#include <maps/factory/libs/unittest/fixture.h>
#include <maps/factory/libs/db/tests/test_data.h>

namespace maps::factory::db::tests {
using namespace factory::tests;

Y_UNIT_TEST_SUITE(mosaic_source_gateway_should) {

MosaicSource testMs()
{
    MosaicSource ms(TEST_MOSAIC_SOURCE_NAME);
    ms.setMercatorGeom(TEST_MOSAIC_SOURCE_GEOMETRY);
    ms.setMinZoom(10);
    ms.setMaxZoom(18);
    ms.setSatellite(TEST_MOSAIC_SOURCE_SATELLITE);
    return ms;
}

Y_UNIT_TEST(edit_mosaic_source)
{
    unittest::Fixture fixture;
    auto conn = fixture.conn();
    int64_t msId = 0;

    {
        auto ms = testMs();
        pqxx::work txn(conn);
        MosaicSourceGateway(txn).insert(ms);
        msId = ms.id();
        txn.commit();
        EXPECT_EQ(msId, 1);
    }
    {
        pqxx::work txn(conn);
        MosaicSource ms = MosaicSourceGateway(txn).loadById(msId);
        EXPECT_EQ(ms.name(), TEST_MOSAIC_SOURCE_NAME);
        EXPECT_EQ(ms.minZoom(), 10u);
        EXPECT_EQ(ms.maxZoom(), 18u);
        EXPECT_EQ(ms.satellite(), TEST_MOSAIC_SOURCE_SATELLITE);
        EXPECT_THAT(ms.mercatorGeom(), GeoEq(TEST_MOSAIC_SOURCE_GEOMETRY));
    }
}

Y_UNIT_TEST(reset_geom_to_original)
{
    auto originalGeom = geolib3::SimpleGeometryTransform2(geolib3::AffineTransform2::translate(2, 2))
        (TEST_MOSAIC_SOURCE_GEOMETRY, geolib3::Forward);

    unittest::Fixture fixture;
    auto conn = fixture.conn();
    int64_t msId = 0;

    {
        auto ms = testMs();
        ms.setMercatorGeoms(ms.mercatorGeom(), originalGeom);
        pqxx::work txn(conn);
        MosaicSourceGateway(txn).insert(ms);
        msId = ms.id();
        txn.commit();
        EXPECT_THAT(ms.mercatorGeom(), GeoEq(TEST_MOSAIC_SOURCE_GEOMETRY));
        EXPECT_THAT(ms.mercatorOriginalGeom(), GeoEq(originalGeom));
    }

    {
        pqxx::work txn(conn);
        MosaicSource ms = MosaicSourceGateway(txn).loadById(msId);
        EXPECT_THAT(ms.mercatorGeom(), GeoEq(TEST_MOSAIC_SOURCE_GEOMETRY));
        EXPECT_THAT(ms.mercatorOriginalGeom(), GeoEq(originalGeom));
    }

    {
        pqxx::work txn(conn);
        MosaicSource ms = MosaicSourceGateway(txn).loadById(msId);
        ms.resetMercatorGeomToOriginal();
        MosaicSourceGateway(txn).update(ms);
        txn.commit();
        EXPECT_THAT(ms.mercatorGeom(), GeoEq(originalGeom));
        EXPECT_THAT(ms.mercatorOriginalGeom(), GeoEq(originalGeom));
    }

    {
        pqxx::work txn(conn);
        MosaicSource ms = MosaicSourceGateway(txn).loadById(msId);
        EXPECT_THAT(ms.mercatorGeom(), GeoEq(originalGeom));
        EXPECT_THAT(ms.mercatorOriginalGeom(), GeoEq(originalGeom));
    }
}

Y_UNIT_TEST(load_st_extent)
{
    unittest::Fixture fixture;
    auto conn = fixture.conn();
    int64_t msId = 0;

    {
        auto ms = testMs();
        pqxx::work txn(conn);
        MosaicSourceGateway(txn).insert(ms);
        msId = ms.id();
        txn.commit();
        EXPECT_EQ(msId, 1);
    }
    {
        pqxx::work txn(conn);
        geolib3::BoundingBox bbox = MosaicSourceGateway(txn)
            .loadStExtent(table::MosaicSource::geometry, sql_chemistry::AnyFilter());
        EXPECT_THAT(bbox, GeoEq(TEST_MOSAIC_SOURCE_GEOMETRY.boundingBox()));
    }
}

Y_UNIT_TEST(load_zoom_area_info)
{
    const double sizeSize = 10;
    const double x = 4000000;
    const double y = 7000000;
    const geolib3::MultiPolygon2 geom{{
        geolib3::Polygon2{geolib3::PointsVector{
            {x, y},
            {x + sizeSize, y},
            {x + sizeSize, y + sizeSize},
            {x, y + sizeSize},
            {x, y}
        }}
    }};

    auto ms = testMs();
    ms.setMercatorGeom(geom);

    auto ms2 = testMs();
    ms2.setName(ms2.name() + "_2");
    ms2.setMercatorGeom(geom);

    unittest::Fixture fixture;
    auto conn = fixture.conn();
    {
        pqxx::work txn(conn);
        MosaicSourceGateway(txn).insert(ms);
        MosaicSourceGateway(txn).insert(ms2);
        txn.commit();
    }

    {
        pqxx::work txn(conn);
        MosaicSourceGateway gtw(txn);
        auto mosaicAreas = loadZoomAreaInfo(gtw, sql_chemistry::AnyFilter());
        ASSERT_EQ(mosaicAreas.size(), 1u);
        EXPECT_EQ(mosaicAreas[0].minZoom, ms.minZoom());
        EXPECT_EQ(mosaicAreas[0].maxZoom, ms.maxZoom());
        EXPECT_EQ(mosaicAreas[0].mercatorAreaSqrMeters, 100);
        EXPECT_NEAR(mosaicAreas[0].geodeticAreaSqrMeters, 35.9, 0.1);
    }
}

} // suite

} // namespace maps::factory::db::tests
