#include "testing_common.h"

#include <maps/factory/libs/tileindex/impl/tile_range.h>
#include <maps/factory/libs/tileindex/impl/tree_compact_expand.h>
#include <maps/factory/libs/tileindex/impl/tree_stats.h>
#include <maps/factory/libs/tileindex/impl/visitors.h>

#include <boost/range/algorithm/copy.hpp>
#include <boost/range/combine.hpp>

using namespace testing;

namespace maps {
namespace tileindex {
namespace impl {
namespace tests {

namespace {
EditableTree randomTree(Zoom z)
{
    EditableTree tree;
    const auto rootsCount = randomNumber(5u, 20u);
    for (RootIdx rootIdx = 0; rootIdx < rootsCount; ++rootIdx) {
        const auto tiles = randomNumber(0u, 5u * z);
        tree.beginMarking();
        for (auto tile = 0u; tile < tiles; ++tile) {
            tree.markTile(randomTile(randomNumber(z / 2, z + 1)));
        }
        tree.endMarking();
    }
    return tree;
}

template <typename... D1, typename... D2>
void checkHaveSameTile(
    const Tree<D1...>& actual,
    const Tree<D2...>& expected,
    Zoom z)
{
    for (Tile tile: TileRange{z}) {
        EXPECT_THAT(actual.resolveRootIndex(tile),
            Eq(expected.resolveRootIndex(tile)));
    }
};
} // namespace

Y_UNIT_TEST_SUITE(VariableLengthIntCompression_Should)
{
namespace vli = vlint;

Y_UNIT_TEST(get_max_value_compressed_size)
{
    std::vector<uint8_t> data(vli::maxCompressedBytes(1));
    const auto next
        = vli::compressOne(data, std::numeric_limits<uint64_t>::max());
    EXPECT_THAT(next, IsEmpty());
    const uint8_t expected[]
        = {0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0x01};
    EXPECT_THAT(data, ElementsAreArray(expected));
}

Y_UNIT_TEST(compress)
{
    const uint64_t values[] = {
        0x00, ~0x00ull, 0x7F, 0xFF, 0x08'03, 0x01ull << 63, 0x42,
    };
    const uint64_t expected[] = {
        0x00,                                                       // 0
        0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0x01, // 1
        0x7F,                                                       // 2
        0xFF, 0x01,                                                 // 3
        0x83, 0x10,                                                 // 4
        0x80, 0x80, 0x80, 0x80, 0x80, 0x80, 0x80, 0x80, 0x80, 0x01, // 5
        0x42,                                                       // 6
    };

    std::vector<uint8_t> data(
        vli::maxCompressedBytes(boost::size(values)));
    const auto next = vli::compress(data, values);
    EXPECT_THAT(next.begin(), Eq(data.begin() + boost::size(expected)));
    EXPECT_THAT(next.end(), Eq(data.end()));

    data.erase(next.begin(), next.end());
    EXPECT_THAT(data, ElementsAreArray(expected));
}

Y_UNIT_TEST(decompress)
{
    const uint8_t data[] = {
        0x00,                                                       // 0
        0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0x01, // 1
        0x0F,                                                       // 2
        0xFF, 0x01,                                                 // 3
        0x83, 0x10,                                                 // 4
        0x80, 0x80, 0x80, 0x80, 0x80, 0x80, 0x80, 0x80, 0x80, 0x01, // 5
        0x42,                                                       // 6
    };
    const uint64_t expected[] = {
        0x00, ~0x00ull, 0x0F, 0xFF, 0x08'03, 0x01ull << 63, 0x42,
    };

    std::vector<uint64_t> values(boost::size(expected));
    vli::decompress(data, values);

    EXPECT_THAT(values, ElementsAreArray(expected));
}

Y_UNIT_TEST(decompress_part)
{
    const uint8_t data[] = {
        0x00,                                                       // 0
        0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0x01, // 1
        0x0F,                                                       // 2
        0xFF, 0x01,                                                 // 3
        0x83, 0x10,                                                 // 4
        0x80, 0x80, 0x80, 0x80, 0x80, 0x80, 0x80, 0x80, 0x80, 0x01, // 5
        0x42,                                                       // 6
    };

    std::vector<uint64_t> values(2);
    vli::decompress(data, values);

    EXPECT_THAT(values, ElementsAre(0x00ull, ~0x00ull));
}

Y_UNIT_TEST(skip_compressed_parts)
{
    const uint8_t data[] = {
        0x00,                                                       // 0
        0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0x01, // 1
        0x0F,                                                       // 2
        0xFF, 0x01,                                                 // 3
        0x83, 0x10,                                                 // 4
        0x80, 0x80, 0x80, 0x80, 0x80, 0x80, 0x80, 0x80, 0x80, 0x01, // 5
        0x42,                                                       // 6
    };
    TArrayRef<const uint8_t> v{data};
    EXPECT_THAT(vli::skip({}), IsEmpty());
    EXPECT_THAT(vli::skip(v, 0), Eq(v));
    EXPECT_THAT(vli::skip(v, 6), ElementsAre(0x42u));
    EXPECT_THAT(vli::skip(v, 7), IsEmpty());
    EXPECT_THAT(vli::skip(v), Eq(v.subspan(1)));
    EXPECT_THAT(vli::skip(v, 2), Eq(v.subspan(11)));
    EXPECT_THAT(vli::skip(v.subspan(11)), Eq(v.subspan(12)));
    EXPECT_THAT(vli::skip(v.subspan(11), 5), IsEmpty());
    EXPECT_THAT(vli::skip(vli::skip(v, 3), 3), ElementsAre(0x42u));
    EXPECT_THAT(vli::skip(vli::skip(v, 4), 4), IsEmpty());
}

Y_UNIT_TEST(compress_and_decompress)
{
    const auto values = randomData<uint64_t>(5);
    std::vector<uint8_t> data(vli::maxCompressedBytes(values.size()));
    std::vector<uint64_t> result(values.size());

    const auto next = vli::compress(data, values);
    data.erase(next.begin(), next.end());
    vli::decompress(data, result);

    EXPECT_THAT(result, ElementsAreArray(values));
}
}

Y_UNIT_TEST_SUITE(CompactTree_Should)
{

Y_UNIT_TEST(mix_and_unmix)
{
    const auto nodeIdx = randomNumber(10u);

    auto mixUnmix = [&](Destination dst) {
        return unpackDestination(packDestination(dst, nodeIdx), nodeIdx);
    };

    EXPECT_THAT(mixUnmix(NULL_DESTINATION), Eq(NULL_DESTINATION));

    for (size_t j = 0; j < nodeIdx * 2; ++j) {
        EXPECT_THAT(mixUnmix(Destination::makeLeaf(j)),
            Eq(Destination::makeLeaf(j)));
    }
    for (size_t j = 1; j < nodeIdx; ++j) {
        EXPECT_THAT(mixUnmix(Destination::makeNode(j)),
            Eq(Destination::makeNode(j)));
    }
}

Y_UNIT_TEST(save_and_load_empty)
{
    std::ostringstream ss;
    StandaloneTileTree tree;
    mms::write(ss, tree);
    const auto data = ss.str();
    const auto& loaded
        = mms::safeCast<MappedTileTree>(data.data(), data.size());
    EXPECT_THAT(loaded.rootsCount(), Eq(0u));
    EXPECT_THAT(countNodes(loaded), Eq(1u));
}

Y_UNIT_TEST(save_and_load_random)
{
    const Zoom z = randomNumber(2u, 5u);
    const auto tree = randomTree(z);

    std::ostringstream ss;
    mms::write(ss, compact(tree));
    const auto data = ss.str();
    const auto& mapped
        = mms::safeCast<MappedTileTree>(data.data(), data.size());
    EXPECT_THAT(mapped, EqOstr(tree));
    check(mapped);
    const auto expanded = expand(mapped);
    EXPECT_THAT(expanded, EqOstr(tree));
    checkHaveSameTile(tree, mapped, z);
}

Y_UNIT_TEST(save_load_editable)
{
    const Zoom z = randomNumber(2u, 5u);
    const auto tree = randomTree(z);

    std::ostringstream ss;
    mms::write(ss, compact(tree));
    const auto data = ss.str();
    const auto loaded
        = expand(mms::safeCast<MappedTileTree>(data.data(), data.size()));

    EXPECT_THAT(loaded, EqOstr(tree));
    check(loaded);
    checkHaveSameTile(tree, loaded, z);
}

Y_UNIT_TEST(compact_random)
{
    const Zoom z = randomNumber(2u, 5u);
    const auto tree = randomTree(z);
    const auto compacted = compact(tree);

    EXPECT_THAT(compacted, EqOstr(tree));
    check(compacted);
    checkHaveSameTile(tree, compacted, z);
}

Y_UNIT_TEST(compact_and_expand_random)
{
    const Zoom z = randomNumber(2u, 5u);
    const auto tree = randomTree(z);
    const auto compacted = compact(tree);
    const auto expanded = expand(compacted);

    EXPECT_THAT(expanded, EqOstr(tree));
    check(expanded);
    checkHaveSameTile(tree, expanded, z);
}

Y_UNIT_TEST(iterate_over_blocks)
{
    std::vector<Node> blocks = {
        {{Destination::makeLeaf(0u), Destination::makeLeaf(9u),
            NULL_DESTINATION}},
        {{Destination::makeNode(0u), Destination::makeNode(1u),
            Destination::makeNode(1u), Destination::makeLeaf(0u)}},
    };

    MmsTree<mms::Standalone> tree;
    for (const auto& b: blocks) {
        tree.appendNode(b);
    }

    std::vector<Node> res;
    for (auto nodeIdx: allNodeIndices(tree)) {
        res.push_back(tree.node(nodeIdx));
    }

    EXPECT_THAT(res, Eq(blocks));
    EXPECT_THAT(countNodes(tree), Eq(3u));
}

Y_UNIT_TEST(calculate_stats)
{
    EditableTree tree;
    tree.markTile(Tile{2, 1, 2});
    const auto st = calculateStats(compact(tree));
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
