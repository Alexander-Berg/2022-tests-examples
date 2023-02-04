#include <maps/factory/libs/processing/build_tileindex.h>

#include <maps/factory/libs/db/mosaic_gateway.h>
#include <maps/factory/libs/db/mosaic.h>
#include <maps/factory/libs/db/mosaic_source_gateway.h>
#include <maps/factory/libs/db/mosaic_source.h>
#include <maps/factory/libs/db/release_gateway.h>
#include <maps/factory/libs/db/release.h>
#include <maps/factory/libs/unittest/fixture.h>
#include <maps/factory/libs/processing/tests/test_context.h>

#include <maps/factory/libs/tileindex/tile_index.h>
#include <maps/libs/log8/include/log8.h>

#include <library/cpp/testing/gmock_in_unittest/gmock.h>

#include <pqxx/pqxx>

#include <filesystem>

namespace maps::factory::processing::tests {

namespace {

using namespace testing;

const std::string OUTPUT_FILEPATH = "./tmp/build_tileindex/";
const std::string OUTPUT_FILENAME = OUTPUT_FILEPATH + impl::TILEINDEX_FILE_NAME;

const geolib3::MultiPolygon2 TEST_MOSAIC_GEOMETRY{{
    geolib3::Polygon2{geolib3::PointsVector{
        {37.5381, 55.8096},
        {37.7369, 55.8119},
        {37.7408, 55.7000},
        {37.5426, 55.6979}
    }}
}};

class Fixture : public unittest::Fixture {
public:
    Fixture()
        : mosaicSource("test_source")
    {
        std::filesystem::create_directory(OUTPUT_FILEPATH);
        pqxx::connection conn(postgres().connectionString());
        pqxx::work txn(conn);
        db::MosaicSourceGateway(txn).insert(mosaicSource);
        txn.commit();
    }

    void addReleases(const size_t releaseNum = 1)
    {
        pqxx::connection conn(postgres().connectionString());
        pqxx::work txn(conn);

        for (size_t i = 0; i != releaseNum; ++i) {
            db::Release release(std::to_string(releases.size()));
            release.setStatus(db::ReleaseStatus::Testing)
                   .setIssue(releases.size());
            db::ReleaseGateway(txn).insert(release);

            db::Mosaic mosaic(
                mosaicSource.id(),
                mosaics.size(),
                0, 18, TEST_MOSAIC_GEOMETRY);
            mosaic.setReleaseId(release.id());
            db::MosaicGateway(txn).insert(mosaic);

            maxIssue = release.issue().value();
            issues.push_back(maxIssue);
            releases.push_back(release);
            mosaics.push_back(mosaic);
        }
        txn.commit();
    }

    size_t maxIssue = 0;
    std::vector<size_t> issues;
    db::MosaicSource mosaicSource;
    std::vector<db::Release> releases;
    std::vector<db::Mosaic> mosaics;
};

} // namespace

Y_UNIT_TEST_SUITE(build_tileindex_tasks_should) {

Y_UNIT_TEST(build_simple_tileindex)
{

    Fixture fixture;
    TestContext ctx(fixture.postgres().connectionString());

    fixture.addReleases(4);

    const BuildTileIndex worker;
    worker(ReleaseIssue(fixture.maxIssue - 1),
        LocalDirectoryPath(OUTPUT_FILEPATH),
        ctx);

    ASSERT_TRUE(std::filesystem::exists(OUTPUT_FILENAME));

    tileindex::MappedTileIndexHolder tileindex = tileindex::loadTileIndex(OUTPUT_FILENAME);

    ASSERT_FALSE(tileindex->empty());
    EXPECT_EQ(tileindex->issues().size(), 3u);

    std::vector<size_t> correctIssues{0, 1, 2};
    std::vector<size_t> issues(tileindex->issues().begin(), tileindex->issues().end());
    EXPECT_EQ(issues, correctIssues);
}

Y_UNIT_TEST(create_correct_geometries)
{

    Fixture fixture;
    TestContext ctx(fixture.postgres().connectionString());
    pqxx::connection conn(fixture.postgres().connectionString());
    pqxx::work txn(conn);

    const auto polygon1 = geolib3::Polygon2(
        geolib3::PointsVector{
            {4042637.665653249248862, 7527006.277060680091381},
            {4236083.450230312533677, 7527952.222217536531389},
            {4239394.25827930867672, 7339236.163424752652645},
            {4045002.528545389417559, 7340182.108581609092653},
            {4042637.665653249248862, 7527006.277060680091381}
        }
    );
    const auto polygon2 = geolib3::Polygon2(
        geolib3::PointsVector{
            {4140542.989387851208448, 7434776.624267214909196},
            {4135340.291025142651051, 7623019.710481570102274},
            {4336826.609435482881963, 7623965.655638426542282},
            {4341556.335219763219357, 7436668.514580926857889},
            {4140542.989387851208448, 7434776.624267214909196}
        }
    );

    fixture.addReleases(2);
    fixture.mosaics[0].setMinMaxZoom(18, 19);
    fixture.mosaics[0].setMercatorGeom(geolib3::MultiPolygon2({polygon1}));
    fixture.mosaics[1].setMinMaxZoom(19, 20);
    fixture.mosaics[1].setMercatorGeom(geolib3::MultiPolygon2({polygon2}));
    db::MosaicGateway(txn).update(fixture.mosaics);
    txn.commit();

    const BuildTileIndex worker;
    worker(ReleaseIssue(fixture.maxIssue),
        LocalDirectoryPath(OUTPUT_FILEPATH),
        ctx);

    ASSERT_TRUE(std::filesystem::exists(OUTPUT_FILENAME));

    tileindex::MappedTileIndexHolder tileindex = tileindex::loadTileIndex(OUTPUT_FILENAME);

    ASSERT_FALSE(tileindex->empty());
    EXPECT_EQ(tileindex->issues().size(), 2u);

    const size_t tile1x = 157667;
    const size_t tile1y = 82490;
    const size_t tile2x = 159078;
    const size_t tile2y = 81420;

    auto tile1 = tileindex::Tile({tile1x, tile1y}, 18);
    auto tile2 = tileindex::Tile({tile2x, tile2y}, 18);
    EXPECT_TRUE(tileindex->resolveIssue(tile1));
    EXPECT_FALSE(tileindex->resolveIssue(tile2));

    tile1 = tileindex::Tile({tile1x * 2, tile1y * 2}, 19);
    tile2 = tileindex::Tile({tile2x * 2, tile2y * 2}, 19);
    EXPECT_TRUE(tileindex->resolveIssue(tile1));
    EXPECT_TRUE(tileindex->resolveIssue(tile2));

    tile1 = tileindex::Tile({tile1x * 4, tile1y * 4}, 20);
    tile2 = tileindex::Tile({tile2x * 4, tile2y * 4}, 20);
    EXPECT_FALSE(tileindex->resolveIssue(tile1));
    EXPECT_TRUE(tileindex->resolveIssue(tile2));
}

} // suite
} // namespace maps::factory::processing::tests
