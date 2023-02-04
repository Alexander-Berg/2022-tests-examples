#include "testing_common.h"

#include <maps/factory/libs/tileindex/impl/tile_range.h>
#include <maps/factory/libs/tileindex/impl/tree_editable.h>
#include <maps/factory/libs/tileindex/impl/tree_stats.h>
#include <maps/factory/libs/tileindex/impl/visitors.h>

#include <boost/algorithm/cxx11/one_of.hpp>

using namespace testing;

namespace maps {
namespace tileindex {
namespace impl {

inline std::ostream& operator<<(std::ostream& os, const EditableTree& tree)
{
    return os << static_cast<const Tree<EditableTree>&>(tree);
}

namespace tests {

Y_UNIT_TEST_SUITE(ExpandedTree_Should)
{
Y_UNIT_TEST(throw_when_root_not_found)
{
    EditableTree tree;
    tree.markTile(Tile{0, 0, 0});
    EXPECT_THROW(tree.resolveRootIndex(2, {0, 0, 0}), std::exception);
}

Y_UNIT_TEST(mark_earth_tile)
{
    const Zoom z = randomNumber(1u, 4u);
    EditableTree tree;
    tree.markTile(Tile::Earth());
    for (Tile tile: TileRange{z}) {
        EXPECT_THAT(tree.resolveRootIndex(tile), OptEq(0u));
    }
}

Y_UNIT_TEST(mark_deepest_tile)
{
    const Zoom z = randomNumber(1u, 4u);
    EditableTree tree;
    auto deepestTile = randomTile(z);
    tree.markTile(deepestTile);
    for (Tile tile: TileRange{z}) {
        EXPECT_THAT(tree.resolveRootIndex(tile),
            EqWhen(tile == deepestTile, 0u));
    }
}

Y_UNIT_TEST(mark_middle_tile)
{
    const Zoom z = randomNumber(3u, 5u);
    EditableTree tree;
    const auto middleTile = randomTile(randomNumber(1u, z - 1u));
    tree.markTile(middleTile);
    for (Tile tile: TileRange{z}) {
        EXPECT_THAT(tree.resolveRootIndex(tile),
            EqWhen(middleTile.contains(tile), 0u));
    }
}

Y_UNIT_TEST(mark_deepest_neighbour_tiles)
{
    const Zoom z = 3u;
    std::vector<Tile> tiles{{0, 0, z}, {0, 1, z}, {7, 0, z}, {6, 0, z}};
    EditableTree tree;
    for (Tile tile: tiles) {
        tree.markTile(tile);
    }
    for (Tile tile: TileRange{z}) {
        EXPECT_THAT(tree.resolveRootIndex(tile),
            EqWhen(al::one_of_equal(tiles, tile), 0u));
    }
}

Y_UNIT_TEST(mark_not_overlaping_tiles)
{
    const Zoom z = 3u;
    Tile big{0, 0, 1}, small{4, 4, z};
    EditableTree tree1;
    EditableTree tree2;

    tree1.markTile(big);
    tree1.markTile(small);

    tree2.markTile(small);
    tree2.markTile(big);

    for (Tile tile: TileRange{z}) {
        auto contains = big.contains(tile) || small.contains(tile);
        EXPECT_THAT(tree1.resolveRootIndex(tile), EqWhen(contains, 0u));
        EXPECT_THAT(tree2.resolveRootIndex(tile), EqWhen(contains, 0u));
    }
}

Y_UNIT_TEST(mark_child_tile_after_parent)
{
    const Zoom z = 3u;
    Tile big{0, 0, 1}, small{1, 2, z};
    EditableTree tree;
    tree.markTile(big);
    tree.markTile(small);
    for (Tile tile: TileRange{z}) {
        EXPECT_THAT(tree.resolveRootIndex(tile),
            EqWhen(big.contains(tile), 0u));
    }
}

Y_UNIT_TEST(mark_parent_tile_after_child)
{
    const Zoom z = 3u;
    Tile big{0, 0, 1}, small{1, 2, z};
    EditableTree tree;
    tree.markTile(small);
    tree.markTile(big);
    for (Tile tile: TileRange{z}) {
        EXPECT_THAT(tree.resolveRootIndex(tile),
            EqWhen(big.contains(tile), 0u));
    }
}

Y_UNIT_TEST(reference_older_node_when_tiles_dont_overlap)
{
    const Zoom z = 3u;
    Tile big{0, 0, 1}, small{4, 4, z};
    EditableTree tree;

    tree.beginMarking();
    tree.markTile(big);
    tree.endMarking();

    tree.beginMarking();
    tree.markTile(small);
    tree.endMarking();

    for (Tile tile: TileRange{z}) {
        EXPECT_THAT(tree.resolveRootIndex(0, tile),
            EqWhen(big.contains(tile), 0u));

        if (small.contains(tile)) {
            EXPECT_THAT(tree.resolveRootIndex(1, tile), OptEq(1u));
        } else if (big.contains(tile)) {
            EXPECT_THAT(tree.resolveRootIndex(1, tile), OptEq(0u));
        } else {
            EXPECT_THAT(tree.resolveRootIndex(1, tile), IsNothing());
        }
    }
}

Y_UNIT_TEST(copy_older_node_when_tiles_overlap)
{
    const Zoom z = 4u;
    Tile big{0, 0, 1}, small{1, 2, z - 1};
    EditableTree tree;

    tree.beginMarking();
    tree.markTile(big);
    tree.endMarking();

    tree.beginMarking();
    tree.markTile(small);
    tree.endMarking();

    for (Tile tile: TileRange{z}) {
        EXPECT_THAT(tree.resolveRootIndex(0, tile),
            EqWhen(big.contains(tile), 0u));

        if (small.contains(tile)) {
            EXPECT_THAT(tree.resolveRootIndex(1, tile), OptEq(1u));
        } else if (big.contains(tile)) {
            EXPECT_THAT(tree.resolveRootIndex(1, tile), OptEq(0u));
        } else {
            EXPECT_THAT(tree.resolveRootIndex(1, tile), IsNothing());
        }
    }
}

Y_UNIT_TEST(reference_random_older_node)
{
    const Zoom z = 4u;
    EditableTree tree;
    std::vector<std::vector<Tile>> stampTiles(4);
    for (RootIdx rootIdx = 0; rootIdx < stampTiles.size(); ++rootIdx) {
        const auto tilesCount = randomNumber(5u, 30u);
        tree.beginMarking();
        for (size_t i = 0; i < tilesCount; ++i) {
            const auto tile = randomTile(randomNumber(2u, z - 1u));
            tree.markTile(tile);
            stampTiles[rootIdx].push_back(tile);
        }
        tree.endMarking();
    }

    for (Tile tile: TileRange{z}) {
        boost::optional<RootIdx> lastMarked;
        for (RootIdx rootIdx = 0; rootIdx < stampTiles.size();
             ++rootIdx) {
            auto& markedTiles = stampTiles[rootIdx];
            if (al::any_of(markedTiles, [&](const Tile& t) {
                return tile.within(t);
            })) {
                lastMarked = rootIdx;
            }

            EXPECT_THAT(tree.resolveRootIndex(rootIdx, tile),
                Eq(lastMarked));
        }
    }
}

Y_UNIT_TEST(stamp_empty_area)
{
    const Zoom z = 3u;
    EditableTree tree;
    Tile tile1{0, 0, 0}, tile2{4, 4, z};

    tree.beginMarking();
    tree.endMarking();

    check(tree);
    for (Tile tile: TileRange{z}) {
        EXPECT_THAT(tree.resolveRootIndex(tile), IsNothing());
    }

    tree.beginMarking();
    tree.endMarking();

    check(tree);
    for (Tile tile: TileRange{z}) {
        EXPECT_THAT(tree.resolveRootIndex(tile), IsNothing());
    }

    tree.beginMarking();
    tree.markTile(tile1);
    tree.endMarking();

    check(tree);
    for (Tile tile: TileRange{z}) {
        EXPECT_THAT(tree.resolveRootIndex(tile),
            EqWhen(tile.within(tile1), 2u));
    }

    tree.beginMarking();
    tree.endMarking();

    check(tree);
    for (Tile tile: TileRange{z}) {
        EXPECT_THAT(tree.resolveRootIndex(tile),
            EqWhen(tile.within(tile1), 2u));
    }

    tree.beginMarking();
    tree.markTile(tile2);
    tree.endMarking();

    check(tree);
    for (Tile tile: TileRange{z}) {
        auto found = tree.resolveRootIndex(tile);
        if (tile.within(tile2)) {
            EXPECT_THAT(found, OptEq(4u));
        } else if (tile.within(tile1)) {
            EXPECT_THAT(found, OptEq(2u));
        } else {
            EXPECT_THAT(found, IsNothing());
        }
    }

    tree.beginMarking();
    tree.endMarking();

    check(tree);
    for (Tile tile: TileRange{z}) {
        auto found = tree.resolveRootIndex(tile);
        if (tile.within(tile2)) {
            EXPECT_THAT(found, OptEq(4u));
        } else if (tile.within(tile1)) {
            EXPECT_THAT(found, OptEq(2u));
        } else {
            EXPECT_THAT(found, IsNothing());
        }
    }
}

Y_UNIT_TEST(pretty_print)
{
    EditableTree tree;
    tree.markTiles({
        Tile{0, 0, 2},
        Tile{1, 0, 2},
        Tile{1, 1, 2},
        Tile{0, 2, 3},
        Tile{0, 3, 3},
        Tile{1, 2, 3},
        Tile{1, 3, 3},
    });

    std::string expect = R"("0"
`-- (0, 0, 0)
    `-- (0, 0, 1)
        +-- (0, 0, 2) : "0"
        +-- (1, 0, 2) : "0"
        +-- (0, 1, 2)
        |   +-- (0, 2, 3) : "0"
        |   +-- (1, 2, 3) : "0"
        |   +-- (0, 3, 3) : "0"
        |   `-- (1, 3, 3) : "0"
        `-- (1, 1, 2) : "0"
)";
    EXPECT_THAT(boost::lexical_cast<std::string>(tree), StrEq(expect));
}

Y_UNIT_TEST(pretty_print_backref)
{

    EditableTree tree;
    tree.beginMarking();
    tree.markTile(Tile{0, 0, 1});
    tree.endMarking();
    tree.beginMarking();
    tree.markTile(Tile{4, 4, 3});
    tree.endMarking();
    std::string expect = R"("0"
`-- (0, 0, 0)
    `-- (0, 0, 1) : "0"
"1"
`-- (0, 0, 0)
    +-- (0, 0, 1) : "0"
    `-- (1, 1, 1)
        `-- (2, 2, 2)
            `-- (4, 4, 3) : "1"
)";
    EXPECT_THAT(boost::lexical_cast<std::string>(tree), StrEq(expect));
}

Y_UNIT_TEST(tiles_count)
{
    EditableTree tree;
    tree.beginMarking();
    tree.markTiles({
        Tile{1, 1, 1},
        Tile{3, 1, 2},
        Tile{5, 1, 3},
    });
    tree.endMarking();

    visitor::TilesCount<EditableTree> tiles{3u};
    visitAll(tree, tiles);
    EXPECT_THAT(tiles.total(), Eq(16u + 4u + 1u));

    tree.beginMarking();
    tree.markTiles({
        Tile{1, 1, 2},
        Tile{0, 2, 3},
    });
    tree.endMarking();

    visitAll(tree, tiles);
    EXPECT_THAT(tiles.total(), Eq(16u + 4u + 1u + 4u + 1u));
    EXPECT_THAT(tiles.root(0u), Eq(16u + 4u + 1u));
    EXPECT_THAT(tiles.root(1u), Eq(4u + 1u));

    tree.beginMarking();
    tree.markTiles({
        Tile{4, 4, 3},
        Tile{5, 1, 3},
    });
    tree.endMarking();

    visitAll(tree, tiles);
    EXPECT_THAT(tiles.total(), Eq(16u + 4u + 1u + 4u + 1u + 2u));
    EXPECT_THAT(tiles.root(0u), Eq(16u + 4u + 1u));
    EXPECT_THAT(tiles.root(1u), Eq(4u + 1u));
    EXPECT_THAT(tiles.root(2u), Eq(2u));
}

Y_UNIT_TEST(check_random_tree)
{
    EditableTree tree;
    for (RootIdx j = 0; j < 10; ++j) {
        tree.markTile(randomTile(randomNumber(0u, 5u)));
    }
    check(tree);
}

Y_UNIT_TEST(check_random_tree_without_backrefs)
{
    EditableTree tree;
    for (int k = 0; k < 20; ++k) {
        tree.beginMarking();
        for (RootIdx j = 0; j < 10; ++j) {
            tree.markTile(randomTile(randomNumber(0u, 5u)));
        }
    }
    check(tree);
}

Y_UNIT_TEST(check_random_tree_with_backrefs)
{
    EditableTree tree;
    for (int k = 0; k < 20; ++k) {
        tree.beginMarking();
        for (RootIdx j = 0; j < 10; ++j) {
            tree.markTile(randomTile(randomNumber(0u, 5u)));
        }
        tree.endMarking();
    }
    check(tree);
}

Y_UNIT_TEST(print_blocks)
{
    std::vector<Node> blocks = {
        {{Destination::makeLeaf(0u), Destination::makeLeaf(9u),
            NULL_DESTINATION}},
        {{Destination::makeNode(0u), Destination::makeNode(1u),
            Destination::makeNode(1u), Destination::makeLeaf(0u)}},
    };

    EditableTree tree;
    for (const auto& b: blocks) {
        tree.appendNode(b);
    }
    tree.appendRoot(1);

    std::ostringstream ss;
    printNodes(ss, tree);
    EXPECT_THAT(ss.str(), StrEq(R"(1r : {(0), (9), -, -}
2  : {-, 1, 1, (0)}
)"));
}

Y_UNIT_TEST(merge_random_trees)
{
    EditableTree tree;
    EditableTree treeMerged;
    for (int k = 0; k < 20; ++k) {
        EditableTree treeTmp;
        treeTmp.beginMarking();
        tree.beginMarking();
        for (RootIdx j = 0; j < 20; ++j) {
            const auto tile = randomTile(randomNumber(0u, 5u));
            treeTmp.markTile(tile);
            tree.markTile(tile);
        }
        treeTmp.endMarking();
        tree.endMarking();

        treeMerged.mergeOneRoot(treeTmp);
    }

    check(treeMerged);
    EXPECT_THAT(treeMerged, Eq(tree));
}

Y_UNIT_TEST(calculate_stats)
{
    EditableTree tree;
    tree.markTile(Tile{0, 0, 2});
    const auto st = calculateStats(tree);
    EXPECT_THAT(st.leafs, Eq(1u));
    EXPECT_THAT(st.nodes, Eq(2u));
    // Root + internal nodes + null block.
    EXPECT_THAT(st.nulls, Eq(3u + 6u + 4u));
}
}

} // namespace tests
} // namespace impl
} // namespace tileindex
} // namespace maps
