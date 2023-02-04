#include <maps/factory/libs/db/dem_patch_gateway.h>
#include <maps/factory/libs/db/dem_release_gateway.h>

#include <maps/factory/libs/unittest/fixture.h>
#include <maps/factory/libs/unittest/tests_common.h>

#include <library/cpp/testing/gtest/gtest.h>

namespace maps::factory::db {
using introspection::operator==;
} // namespace maps::factory::db

namespace maps::factory::dem::tests {
using namespace factory::tests;
using namespace db::table::alias;
using namespace geometry;

Y_UNIT_TEST_SUITE(dem_gateways_should) {

Y_UNIT_TEST(test_creating_release)
{
    unittest::Fixture fixture;
    pqxx::connection conn(fixture.postgres().connectionString());
    pqxx::work txn(conn);

    db::DemRelease release("name");
    release.setDescription("description");
    db::DemReleaseGateway(txn).insert(release);

    ASSERT_NE(release.id(), 0);

    release = db::DemReleaseGateway(txn).loadById(release.id());
    EXPECT_EQ(release.name(), "name");
    EXPECT_EQ(release.description(), "description");
    EXPECT_EQ(release.status(), db::DemReleaseStatus::New);
    EXPECT_FALSE(release.issueId());
}

Y_UNIT_TEST(test_creating_patch)
{
    unittest::Fixture fixture;
    pqxx::connection conn(fixture.postgres().connectionString());
    pqxx::work txn(conn);

    db::DemRelease release("name");
    release.setDescription("description");
    db::DemReleaseGateway(txn).insert(release);

    const tile::Tile tile(1, 2, 3);
    const int version = 4;
    db::IssuedTileMap tiles;
    tiles.insert({tile, version});

    db::DemPatch patch("patch_name", release.id());
    patch.setCorrections(MultiPoint3d{{1, 2, 3}});
    patch.setTiles(tiles);
    patch.setDescription("patch_description");
    patch.setCogPath("path");
    patch.setBbox(boxFromPoints(1.0, 2.0, 3.0, 4.0));
    db::DemPatchGateway(txn).insert(patch);

    ASSERT_NE(patch.id(), 0);

    patch = db::DemPatchGateway(txn).loadById(patch.id());
    EXPECT_EQ(patch.name(), "patch_name");
    EXPECT_EQ(patch.description(), "patch_description");
    EXPECT_EQ(patch.releaseId(), release.id());
    ASSERT_TRUE(patch.hasCorrections());
    EXPECT_EQ(patch.corrections()->at(0), Point3d(1, 2, 3));
    ASSERT_TRUE(patch.hasTiles());
    ASSERT_TRUE(patch.tiles()->contains(tile));
    ASSERT_EQ(patch.tiles()->size(), 1u);
    ASSERT_EQ(patch.tiles()->at(tile), version);
    ASSERT_TRUE(patch.hasCog());
    EXPECT_EQ(*patch.cogPath(), "path");
    ASSERT_TRUE(patch.hasBbox());
    EXPECT_THAT(*patch.bbox(), EigEq(boxFromPoints(1.0, 2.0, 3.0, 4.0)));
}

Y_UNIT_TEST(test_reading_writing_wkb)
{
    unittest::Fixture fixture;
    pqxx::connection conn(fixture.postgres().connectionString());
    pqxx::work txn(conn);

    db::DemRelease release("name");
    db::DemReleaseGateway(txn).insert(release);

    db::DemPatch patch("patch_name", release.id());
    patch.setCorrections(MultiPoint3d{{1, 2, 3}});
    patch.setBbox(boxFromPoints(1.0, 2.0, 3.0, 4.0));
    db::DemPatchGateway(txn).insert(patch);

    db::DemPatch loadedPatch = db::DemPatchGateway(txn).loadById(patch.id());
    ASSERT_TRUE(loadedPatch.hasCorrections());
    EXPECT_EQ(*loadedPatch.corrections(), *patch.corrections());
}

} // suite

} // namespace maps::factory::dem::tests
