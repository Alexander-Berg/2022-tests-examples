#include <maps/factory/libs/rendering/release_tiles.h>

#include <maps/factory/libs/geometry/geolib.h>
#include <maps/factory/libs/db/aoi_gateway.h>
#include <maps/factory/libs/db/delivery_gateway.h>
#include <maps/factory/libs/db/mosaic_gateway.h>
#include <maps/factory/libs/db/mosaic_source_gateway.h>
#include <maps/factory/libs/db/mosaic_source_pipeline.h>
#include <maps/factory/libs/db/order_gateway.h>
#include <maps/factory/libs/db/release_gateway.h>
#include <maps/factory/libs/db/secret_object_gateway.h>
#include <maps/factory/libs/db/secret_object.h>
#include <maps/factory/libs/db/source_gateway.h>
#include <maps/factory/libs/delivery/cog.h>
#include <maps/factory/libs/storage/local_storage.h>

#include <maps/factory/libs/processing/tests/test_context.h>
#include <maps/factory/libs/unittest/tests_common.h>
#include <maps/factory/libs/unittest/fixture.h>

#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/testing/gmock_in_unittest/gmock.h>

#include <maps/libs/tile/include/geometry.h>
#include <maps/libs/tile/include/hash.h>

namespace maps::factory::rendering::tests {
namespace {

using namespace testing;
using namespace maps::factory::tests;
using namespace maps::factory::storage;
using namespace maps::factory::geometry;

const std::string root = ArcadiaSourceRoot();
const std::string cogPathDg = root + "/maps/factory/test_data/cog/dg_058800151040_01_P001";
const std::string cogPathScanex = root + "/maps/factory/test_data/cog/scanex_13174627";
const std::string demPath = root + "/maps/factory/test_data/dem/058800151040_01_DEM.TIF";

geolib3::MultiPolygon2 geomDg()
{
    const static geolib3::MultiPolygon2 g = toGeolibMultiPolygon(
        Geometry::fromGeoJson(delivery::Cog(*storage::storageFromDir(cogPathDg)).contourGeoJson())
            .transformedTo(mercatorSr()));
    return g;
}

const Eigen::Vector2d geomDgShift(100, 200);

geolib3::MultiPolygon2 geomDgShifted()
{
    const static geolib3::MultiPolygon2 g = toGeolibMultiPolygon(
        toGeom(geomDg()).shifted(geomDgShift));
    return g;
}

geolib3::MultiPolygon2 geomScanex()
{
    const static geolib3::MultiPolygon2 g = toGeolibMultiPolygon(
        Geometry::fromGeoJson(
            delivery::Cog(*storage::storageFromDir(cogPathScanex)).contourGeoJson())
            .transformedTo(mercatorSr()));
    return g;
}

db::Release prepareRelease(sql_chemistry::Transaction& txn)
{
    db::Order order(2020, db::OrderType::Tasking);
    db::OrderGateway(txn).insert(order);

    db::Source source("test_source", db::SourceType::Local, "/");
    db::SourceGateway(txn).insert(source);

    db::Delivery delivery(source.id(), "2019-01-01", "test_delivery", "/");
    db::DeliveryGateway(txn).insert(delivery);

    db::MosaicSource msDg("dg_1");
    {
        db::Aoi aoi(order.id(), "aoi_dg", geomDg().polygonAt(0));
        db::AoiGateway(txn).insert(aoi);
        msDg.setDeliveryId(delivery.id());
        msDg.setMercatorGeom(geomDg());
        msDg.setMinZoom(10);
        msDg.setMaxZoom(16);
        msDg.setSatellite("WV03");
        msDg.setOrderId(order.id());
        msDg.setAoiId(aoi.id());
        msDg.setCogPath(cogPathDg);
        db::MosaicSourcePipeline(txn).insertNew(msDg);
        db::MosaicSourcePipeline(txn).transition(msDg,
            db::MosaicSourceStatus::Ready, db::UserRole(unittest::TEST_CUSTOMER_USER_ID, db::Role::Customer));
    }
    db::MosaicSource msSc("scanex_1");
    {
        db::Aoi aoi(order.id(), "aoi_scanex", geomScanex().polygonAt(0));
        db::AoiGateway(txn).insert(aoi);
        msSc.setDeliveryId(delivery.id());
        msSc.setMercatorGeom(geomScanex());
        msSc.setMinZoom(11);
        msSc.setMaxZoom(15);
        msSc.setSatellite("WV03");
        msSc.setOrderId(order.id());
        msSc.setAoiId(aoi.id());
        msSc.setCogPath(cogPathScanex);
        db::MosaicSourcePipeline(txn).insertNew(msSc);
        db::MosaicSourcePipeline(txn).transition(msSc,
            db::MosaicSourceStatus::Ready, db::UserRole(unittest::TEST_CUSTOMER_USER_ID, db::Role::Customer));
    }

    db::Release release = db::Release("Test_release")
        .setStatus(db::ReleaseStatus::Ready)
        .setIssue(1);
    db::ReleaseGateway(txn).insert(release);
    {
        db::Mosaic m(msDg.id(), 3, 10, 16, geomDg());
        m.setReleaseId(release.id());
        db::MosaicGateway(txn).insert(m);
    }
    {
        db::Mosaic m(msSc.id(), 1, 11, 15, geomScanex());
        m.setReleaseId(release.id());
        db::MosaicGateway(txn).insert(m);
    }
    {
        db::Mosaic m(msDg.id(), 2, 9, 15, geomDgShifted());
        m.setReleaseId(release.id());
        m.setShift(toGeolibVector(geomDgShift));
        db::MosaicGateway(txn).insert(m);
    }
    return release;
}

MosaicInfos releaseInfos()
{
    return {
        MosaicInfo{
            .tag = "MosaicId(1)",
            .cogPath = cogPathDg,
            .shiftMeters = {0, 0},
            .minZoom = 10,
            .maxZoom = 16,
            .demPath = demPath,
            .mercatorGeom = geomDg(),
            .zOrder = 3,
        },
        MosaicInfo{
            .tag = "MosaicId(3)",
            .cogPath = cogPathDg,
            .shiftMeters = geomDgShift,
            .minZoom = 9,
            .maxZoom = 15,
            .demPath = demPath,
            .mercatorGeom = geomDgShifted(),
            .zOrder = 2,
        },
        MosaicInfo{
            .tag = "MosaicId(2)",
            .cogPath = cogPathScanex,
            .shiftMeters = {0, 0},
            .minZoom = 11,
            .maxZoom = 15,
            .demPath = demPath,
            .mercatorGeom = geomScanex(),
            .zOrder = 1,
        },
    };
}

} // namespace

Y_UNIT_TEST_SUITE(release_tiles_should) {

Y_UNIT_TEST(load_mosaics)
{
    unittest::Fixture fixture;
    auto conn = fixture.conn();
    pqxx::work work(conn);
    const db::Release release = prepareRelease(work);

    ReleaseTiles rt(release.id(), work);

    EXPECT_THAT(rt.releaseId(), Eq(release.id()));
    EXPECT_THAT(rt.issueId(), Eq(release.issue().value()));
    const auto& mosaics = rt.mosaics();
    EXPECT_THAT(mosaics.size(), Eq(3u));
    EXPECT_THAT(get<0>(mosaics[0]), Eq(cogPathDg));
    EXPECT_THAT(get<0>(mosaics[1]), Eq(cogPathDg));
    EXPECT_THAT(get<0>(mosaics[2]), Eq(cogPathScanex));
    EXPECT_THAT(get<1>(mosaics[0]).id(), Eq(1));
    EXPECT_THAT(get<1>(mosaics[1]).id(), Eq(3));
    EXPECT_THAT(get<1>(mosaics[2]).id(), Eq(2));
}

Y_UNIT_TEST(load_infos)
{
    unittest::Fixture fixture;
    auto conn = fixture.conn();
    pqxx::work work(conn);
    const db::Release release = prepareRelease(work);

    ReleaseTiles rt(release.id(), work, demPath);

    EXPECT_THAT(rt.size(), Eq(3u));
    const auto& infos = rt.infos();
    EXPECT_THAT(infos.size(), Eq(rt.size()));
    const auto expected = releaseInfos();
    EXPECT_THAT(infos, ElementsAreArray(expected));
}

Y_UNIT_TEST(load_infos_in_sorted_order)
{
    unittest::Fixture fixture;
    auto conn = fixture.conn();
    pqxx::work work(conn);
    const db::Release release = prepareRelease(work);

    ReleaseTiles rt(release.id(), work, demPath);
    const auto& infos = rt.infos();
    EXPECT_THAT(infos[0].zOrder, Eq(3u));
    EXPECT_THAT(infos[1].zOrder, Eq(2u));
    EXPECT_THAT(infos[2].zOrder, Eq(1u));
}

Y_UNIT_TEST(load_empty_secrets)
{
    unittest::Fixture fixture;
    auto conn = fixture.conn();
    pqxx::work work(conn);
    prepareRelease(work);

    SecretObjectsGeometry so(uniteGeometry(releaseInfos()), work);

    EXPECT_THAT(so.secrets(), IsEmpty());
    EXPECT_THAT(so.regions(), IsEmpty());
}

Y_UNIT_TEST(load_secrets)
{
    unittest::Fixture fixture;
    auto conn = fixture.conn();
    pqxx::work work(conn);
    prepareRelease(work);
    db::SecretObjects secrets{
        db::SecretObject("so1", geomDg(), 17),
        db::SecretObject("so2", geomDgShifted(), 18)
    };
    db::SecretObjectGateway(work).insert(secrets);

    SecretObjectsGeometry so(uniteGeometry(releaseInfos()), work);

    const auto unitedGeom = uniteGeometry(secrets);
    EXPECT_THAT(so.secrets(), SizeIs(2u));
    for (tile::Zoom z = 0; z <= tile::MAX_ZOOM; ++z) {
        if (z < 17) {
            EXPECT_THAT(so.regions(), Not(Contains(Key(z))));
        } else if (z == 17) {
            EXPECT_THAT(so.regions(), Contains(Key(z)));
            EXPECT_THAT(so.regions().find(z)->second.geom(), GeoEq(geomDg()));
        }
    }
}

Y_UNIT_TEST(iterate_all_tiles)
{
    MosaicInfo info{
        .cogPath = cogPathDg,
        .shiftMeters = {0, 0},
        .minZoom = 8,
        .maxZoom = 18,
        .demPath = demPath,
        .mercatorGeom = geomDg(),
    };
    const MosaicInfos infos{info};
    std::vector<tile::Tile> result;

    size_t ranges = 0;
    iterateTiles(infos, [&](const tile::Tile& top, unsigned zEnd, const BundleIndices&) {
        ++ranges;
        for (auto& tile: tile::TileRange(top, zEnd)) {
            result.push_back(tile);
        }
    }, 14);

    const auto tiles = tile::intersectedTiles(info.mercatorGeom, info.minZoom, info.maxZoom);
    std::vector<tile::Tile> expected(tiles.begin(), tiles.end());
    EXPECT_THAT(result, UnorderedElementsAreArray(expected));
    EXPECT_THAT(ranges, Lt(expected.size()));
}

} // suite
} //namespace maps::factory::rendering::tests
