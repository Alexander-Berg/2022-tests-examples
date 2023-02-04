#include "testing_common.h"

#include <maps/factory/libs/tileindex/impl/tile_range.h>
#include <maps/factory/libs/tileindex/impl/tree_compact_expand.h>
#include <maps/factory/libs/tileindex/impl/visitors.h>
#include <maps/factory/libs/tileindex/impl/tree_stats.h>

#include <maps/libs/geolib/include/prepared_polygon.h>

using namespace testing;

namespace maps {
namespace tileindex {
namespace impl {
namespace tests {
namespace {

using PrepPoly = std::unique_ptr<geolib3::PreparedPolygon2>;

std::vector<PrepPoly> genRndPolys()
{
    std::vector<PrepPoly> polys;
    for (int j = 0; j < 5; ++j) {
        polys.push_back(
            std::make_unique<geolib3::PreparedPolygon2>(randomGeometry()));
    }
    return polys;
}

void markAll(EditableTree& tree, const std::vector<PrepPoly>& polys, Zoom z)
{
    for (auto& poly: polys) {
        tree.beginMarking();
        tree.markGeom(*poly, z, UnitProjection{});
        tree.endMarking();
    }
}

template <typename... D>
void checkSame(
    const Tree<D...>& tree,
    const std::vector<PrepPoly>& polys,
    Zoom z)
{
    check(tree);

    for (Tile tile: TileRange{z}) {
        auto tilePoly = UnitProjection{}(tile);
        boost::optional<RootIdx> lastMarked{};
        for (size_t i = polys.size(); i-- > 0;) {
            if (spatialRelation(*polys[i], tilePoly, geolib3::Intersects)) {
                lastMarked = i;
                break;
            }
        }
        EXPECT_THAT(tree.resolveRootIndex(tile), Eq(lastMarked));
    }
}
} // namespace

Y_UNIT_TEST_SUITE(Geometry_TreeShould)
{
Y_UNIT_TEST(get_tile_coords_within_unit_square)
{
    using geolib3::BoundingBox;
    const auto ur = UnitProjection{};
    EXPECT_THAT(ur(Tile::Earth()), Eq(BoundingBox({0, 0}, {1.0, 1.0})));
    EXPECT_THAT(ur({0, 0, 1}), Eq(BoundingBox({0, 0}, {0.5, 0.5})));
    EXPECT_THAT(ur({0, 0, 2}), Eq(BoundingBox({0, 0}, {0.25, 0.25})));
    EXPECT_THAT(ur({1, 3, 2}), Eq(BoundingBox({0.25, 0.75}, {0.5, 1.0})));
}

Y_UNIT_TEST(mark_random_unit_poly)
{
    const Zoom z = 4u;
    EditableTree tree;
    const auto polys = genRndPolys();
    markAll(tree, polys, z);
    checkSame(tree, polys, z);
}

Y_UNIT_TEST(mark_at_zero_zoom)
{
    const auto ur = UnitProjection{};
    const Zoom z = 0u;
    EditableTree tree;
    tree.beginMarking();
    tree.markGeom(ur(Tile{1, 2, 6}), z, ur);
    tree.endMarking();
    tree.beginMarking();
    tree.markGeom(ur(Tile{9, 12, 6}), z, ur);
    tree.endMarking();
    tree.beginMarking();
    tree.endMarking();
    EXPECT_THAT(tree.resolveRootIndex(0, Tile(0, 0, z)), OptEq(0u));
    EXPECT_THAT(tree.resolveRootIndex(1, Tile(0, 0, z)), OptEq(1u));
    EXPECT_THAT(tree.resolveRootIndex(2, Tile(0, 0, z)), OptEq(1u));
    EXPECT_THAT(boost::lexical_cast<std::string>(tree), StrEq(R"("0"
`-- (0, 0, 0) : "0"
"1"
`-- (0, 0, 0) : "1"
"2"
`-- (0, 0, 0) : "1"
)"));
}

Y_UNIT_TEST(check_random_poly_when_compact)
{
    const Zoom z = 4u;
    EditableTree tree;
    const auto polys = genRndPolys();
    markAll(tree, polys, z);
    const auto compacted = compact(tree);
    checkSame(compacted, polys, z);
}

Y_UNIT_TEST(mark_compact_expand)
{
    const Zoom z = 4u;
    StandaloneTileTree tree;
    const auto polys = genRndPolys();
    for (auto& poly: polys) {
        auto expanded = expand(tree);
        expanded.beginMarking();
        expanded.markGeom(*poly, z, UnitProjection{});
        expanded.endMarking();
        tree = compact(expanded);
    }
    checkSame(tree, polys, z);
}

Y_UNIT_TEST(mark_any_tile_poly_with_one_leaf)
{
    for (Zoom z = 0; z < 20; ++z) {
        const auto tile = randomTile(z);
        EditableTree tree;
        tree.beginMarking();
        tree.markGeom(
            geolib3::PreparedPolygon2{MercatorProjection{}(tile)}, z,
            MercatorProjection{-1e-6});
        tree.endMarking();

        const auto stats = calculateStats(tree);
        EXPECT_THAT(stats.leafs, Eq(1u));
    }
}

Y_UNIT_TEST(topological_sort_tiles)
{
    const Zoom z = 4u;
    EditableTree tree;
    for (int k = 0; k < 3; ++k) {
        tree.beginMarking();
        for (RootIdx j = 0; j < 4; ++j) {
            tree.markTile(randomTile(randomNumber(z - 1) + 2));
        }
        tree.endMarking();
    }

    const auto sorted = tree.topologicallySorted();
    check(sorted);

    EXPECT_THAT(boost::lexical_cast<std::string>(sorted),
        StrEq(boost::lexical_cast<std::string>(tree)));

    for (auto nodeIdx: allNodeIndices(sorted)) {
        for (Edge edge: OutgoingEdges{nodeIdx}) {
            if (sorted.destination(edge).isValidNode()) {
                // All links point to previous blocks.
                EXPECT_THAT(sorted.destination(edge).nodeIndex(),
                    Lt(nodeIdx));
            }
        }
    }
}

Y_UNIT_TEST(topological_sort_geoms)
{
    const Zoom z = 4u;
    EditableTree tree;
    const auto polys = genRndPolys();
    markAll(tree, polys, z);

    const auto sorted = tree.topologicallySorted();

    EXPECT_THAT(boost::lexical_cast<std::string>(tree),
        StrEq(boost::lexical_cast<std::string>(tree)));
    checkSame(sorted, polys, z);

    for (auto nodeIdx: allNodeIndices(sorted)) {
        for (Edge edge: OutgoingEdges{nodeIdx}) {
            if (sorted.destination(edge).isValidNode()) {
                // All links point to previous blocks.
                EXPECT_THAT(sorted.destination(edge).nodeIndex(),
                    Lt(nodeIdx));
            }
        }
    }
}

Y_UNIT_TEST(add_using_merge)
{
    const Zoom z = 4u;
    EditableTree tree;
    const auto polys = genRndPolys();
    for (auto& poly: polys) {
        EditableTree tmpTree;
        tmpTree.beginMarking();
        tmpTree.markGeom(*poly, z, UnitProjection{});
        tmpTree.endMarking();

        tree.mergeOneRoot(tmpTree);
    }
    checkSame(tree, polys, z);
}
}

} // namespace tests
} // namespace impl
} // namespace tileindex
} // namespace maps
