#include "geometry_tile_index.h"
#include "testing_common.h"

#include <maps/factory/libs/tileindex/impl/tile_range.h>
#include <maps/factory/libs/tileindex/impl/tree_stats.h>
#include <maps/factory/libs/tileindex/tile_index.h>

#include <boost/filesystem/operations.hpp>
#include <boost/range/algorithm/sort.hpp>

using namespace testing;

namespace maps {
namespace tileindex {
namespace impl {
namespace tests {

namespace {
maps::geolib3::Polygon2 earthPolyFromDb()
{
    static unsigned char earthBin[]
        = {0x01, 0x03, 0x00, 0x00, 0x20, 0x43, 0x0d, 0x00, 0x00, 0x01, 0x00,
            0x00, 0x00, 0x05, 0x00, 0x00, 0x00, 0x52, 0x10, 0x7c, 0x45, 0xf8,
            0x1b, 0x73, 0xc1, 0x52, 0x10, 0x7c, 0x45, 0xf8, 0x1b, 0x73, 0xc1,
            0x52, 0x10, 0x7c, 0x45, 0xf8, 0x1b, 0x73, 0xc1, 0x52, 0x10, 0x7c,
            0x45, 0xf8, 0x1b, 0x73, 0x41, 0x52, 0x10, 0x7c, 0x45, 0xf8, 0x1b,
            0x73, 0x41, 0x52, 0x10, 0x7c, 0x45, 0xf8, 0x1b, 0x73, 0x41, 0x52,
            0x10, 0x7c, 0x45, 0xf8, 0x1b, 0x73, 0x41, 0x52, 0x10, 0x7c, 0x45,
            0xf8, 0x1b, 0x73, 0xc1, 0x52, 0x10, 0x7c, 0x45, 0xf8, 0x1b, 0x73,
            0xc1, 0x52, 0x10, 0x7c, 0x45, 0xf8, 0x1b, 0x73, 0xc1};
    return geolib3::EWKB::read<geolib3::SpatialReference::Epsg3395,
                               geolib3::Polygon2>(std::string(
        reinterpret_cast<const char*>(earthBin), sizeof(earthBin)));
}

constexpr Zoom MAX_ZOOM = 5;

std::vector<Release> rndReleases()
{
    std::vector<Release> rs;
    const Issue issues = randomNumber(5u, 30u);
    for (Issue issue = 0; issue < issues; ++issue) {
        const auto minZoom = randomNumber(0u, MAX_ZOOM / 2u);
        const auto maxZoom = randomNumber(MAX_ZOOM / 2u + 1u, MAX_ZOOM + 1u);
        Release rel{issue};
        for (Zoom z = minZoom; z <= maxZoom; ++z) {
            rel.setGeometry(
                z, randomGeometry<MercatorProjection>(randomNumber(1u, 10u)));
        }
        rs.emplace_back(std::move(rel));
    }
    return rs;
}

template <typename D1, typename D2>
void checkEqual(const TileIndex<D1>& actual, const D2& expected)
{
    EXPECT_THAT(actual.issues(), Eq(expected.issues()));
    for (Zoom z = 0; z <= MAX_ZOOM; ++z) {
        for (Tile tile: TileRange{z}) {
            EXPECT_THAT(actual.resolveIssue(tile),
                Eq(expected.resolveIssue(tile)));
        }
    }
};
} // namespace

Y_UNIT_TEST_SUITE(Release_Should)
{
Y_UNIT_TEST(be_sorted_in_acs_issue_order)
{
    Releases rs{{42}, {1}, {0}, {44}};
    boost::sort(rs, OrderByIssueAsc{});
    EXPECT_THAT(rs | ad::transformed([](auto&& r) { return r.issue(); }),
        ElementsAre(0, 1, 42, 44));
}

Y_UNIT_TEST(not_set_empty_geom)
{
    Release r{42};
    EXPECT_ANY_THROW(r.setGeometry(1, MercatorGeometry{}));
    EXPECT_ANY_THROW(r.geometry(1));
}

Y_UNIT_TEST(get_zooms_in_sorted_order)
{
    Release r{42u};
    r.setGeometry(5, randomGeometry()).setGeometry(2, randomGeometry());
    EXPECT_THAT(r.issue(), Eq(42u));
    EXPECT_THAT(r.zooms(), ElementsAre(2, 5));
}

Y_UNIT_TEST(get_geometry_for_zoom)
{
    const auto g1 = randomGeometry();
    const auto g3 = randomGeometry();
    Release r{42};
    r.setGeometry(1, g1).setGeometry(3, g3);
    EXPECT_THAT(r.geometry(1), Eq(g1));
    EXPECT_ANY_THROW(r.geometry(2));
    EXPECT_THAT(r.geometry(3), Eq(g3));
}

Y_UNIT_TEST(save_and_load_to_dir)
{
    Releases rs{
        Release{0},
        Release{1}
            .setGeometry(1, randomGeometry())
            .setGeometry(2, randomGeometry()),
        Release{42}.setGeometry(5, randomGeometry()),
    };
    static const fs::path dir = "tmp_rel_1";
    remove_all(dir);
    save(rs, dir);
    EXPECT_THAT(loadReleases(dir), ElementsAreArray(rs));
}

Y_UNIT_TEST(load_from_empty_dir)
{
    static const fs::path dir = "tmp_rel_2";
    remove_all(dir);
    create_directories(dir);
    EXPECT_THAT(loadReleases(dir), IsEmpty());
}
}

Y_UNIT_TEST_SUITE(TileIndex_Should)
{
Y_UNIT_TEST(mark_Earth_using_one_tile)
{
    Releases rs{
        Release{0},
        Release{1},
    };
    const auto EarthDb = earthPolyFromDb();
    const auto Earth = MercatorProjection{}(Tile::Earth());
    for (Zoom zoom = 0; zoom < MAX_ZOOM; ++zoom) {
        rs[0].setGeometry(zoom, MercatorGeometry{{EarthDb}});
        rs[1].setGeometry(zoom, MercatorGeometry{{Earth}});
    }
    EditableTileIndex index{MAX_ZOOM};
    index.addReleases(rs);
    for (Zoom zoom = 0; zoom < MAX_ZOOM; ++zoom) {
        const auto stats = calculateStats(index.trees()[zoom]);
        EXPECT_THAT(stats.leafs, Eq(2u));
    }
}

Y_UNIT_TEST(check_against_geometry_index)
{
    const auto rs = rndReleases();
    EditableTileIndex index;
    index.addReleases(rs);
    GeometryTileIndex gindex(rs);
    checkEqual(index, gindex);
}

Y_UNIT_TEST(save_and_load_empty)
{
    EditableTileIndex index;
    std::ostringstream ss;
    mms::write(ss, index);
    const auto data = ss.str();
    const auto& loaded
        = mms::safeCast<MappedTileIndex>(data.data(), data.size());
    EXPECT_THAT(loaded.issues(), Eq(index.issues()));
    EXPECT_THAT(loaded.resolveIssue(0, Tile::Earth()), IsNothing());
}

Y_UNIT_TEST(save_and_load_random)
{
    const auto rs = rndReleases();
    EditableTileIndex index;
    index.addReleases(rs);

    std::ostringstream ss;
    mms::write(ss, index);
    const auto data = ss.str();
    const auto& loaded
        = mms::safeCast<MappedTileIndex>(data.data(), data.size());

    checkEqual(index, loaded);
}

Y_UNIT_TEST(add_by_parts)
{
    const auto all = rndReleases();
    const auto mid = all.begin() + all.size() / 2;
    std::vector<Release> part1(all.begin(), mid),
        part2(mid + 1, all.end());
    EditableTileIndex index, index2;
    index.addReleases(part1);
    index.addRelease(*mid);
    index.addReleases(part2);
    index2.addReleases(all);
    checkEqual(index, index2);
}

Y_UNIT_TEST(save_load_edit)
{
    const auto rs = rndReleases();
    const auto mid = rs.begin() + rs.size() / 2;
    std::vector<Release> part1(rs.begin(), mid), part2(mid, rs.end());
    EditableTileIndex orig;
    orig.addReleases(part1);

    std::ostringstream ss;
    mms::write(ss, orig);
    const auto data = ss.str();
    EditableTileIndex loaded;
    mms::copy(mms::safeCast<MappedTileIndex>(data.data(), data.size()),
        loaded);

    loaded.addReleases(part2);
    orig.addReleases(part2);

    checkEqual(orig, loaded);
}

Y_UNIT_TEST(save_and_map_from_file)
{
    const auto rs = rndReleases();
    EditableTileIndex orig;
    orig.addReleases(rs);

    const auto path = "temp_index_1.mms";
    save(orig, path);
    auto loaded = loadTileIndex(path);

    checkEqual(orig, *loaded);
}

Y_UNIT_TEST(save_and_load_from_file)
{
    const auto rs = rndReleases();
    EditableTileIndex orig;
    orig.addReleases(rs);

    const auto path = "temp_index_1.mms";
    save(orig, path);
    auto loaded = loadEditableTileIndex(path);

    checkEqual(orig, loaded);
}
}

} // namespace tests
} // namespace impl
} // namespace tileindex
} // namespace maps
