#include <maps/factory/tools/fix-geometry/tests/fixture.h>
#include <maps/factory/tools/fix-geometry/lib/include/fix_geometry.h>

#include <maps/factory/libs/db/mosaic_source_gateway.h>
#include <maps/factory/libs/db/release_gateway.h>

#include <library/cpp/testing/gtest/gtest.h>

const geolib3::MultiPolygon2 SPIKED_GEOMETRY({
    geolib3::Polygon2(
        geolib3::PointsVector{
            {10827556.803956756, 286808.1209935184},
            {10827556.803956756, 287091.9430277179},
            {10843857.89603545, 287091.9430277179},
            {10843857.7515034, 287091.9421327699},
            {10843857.227846608, 287091.93595288036},
            {10827556.803956756, 286808.1209935184}
        }
    )
});

TEST_F(Fixture, fix_mosaic_sources_with_spikes)
{
    {
        db::MosaicSource source{"test_source"};
        source.setMercatorGeom(SPIKED_GEOMETRY);
        auto txn = txnHandle();
        db::MosaicSourceGateway(*txn).insert(source);
        txn->commit();
    }

    FixGeometry<db::MosaicSourceGateway> fixGeometry(dbPool(), false, false, false, 1000);
    EXPECT_EQ(fixGeometry.getAllBrokenIds().at(0), 1);

    fixGeometry.fixGeometries({1});
    EXPECT_EQ(fixGeometry.getAllBrokenIds().size(), 0u);
}

TEST_F(Fixture, fix_mosaics_with_spikes)
{
    {
        db::MosaicSource source{"test_source"};
        auto txn = txnHandle();
        db::MosaicSourceGateway(*txn).insert(source);
        db::Release release{"test_release"};
        db::ReleaseGateway(*txn).insert(release);
        db::Mosaic mosaic{source.id(), 1, 1, 19, SPIKED_GEOMETRY};
        mosaic.setReleaseId(release.id());
        db::MosaicGateway(*txn).insert(mosaic);
        txn->commit();
    }

    FixGeometry<db::MosaicGateway> fixGeometry(dbPool(), false, false, false, 1000);
    EXPECT_EQ(fixGeometry.getAllBrokenIds().at(0), 1);

    fixGeometry.fixGeometries({1});
    EXPECT_EQ(fixGeometry.getAllBrokenIds().size(), 0u);
}
