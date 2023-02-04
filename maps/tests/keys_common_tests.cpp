#include <maps/factory/libs/tileindex/tests/testing_common.h>
#include <maps/factory/libs/tileindex/tools/lib/keys_common.h>

using namespace testing;

namespace maps {
namespace tileindex {
namespace impl {

namespace tests {

Y_UNIT_TEST_SUITE(Check_s3_keys_Should)
{
Y_UNIT_TEST(parse_issued_tile)
{
    for (size_t i = 0; i < 50; ++i) {
        std::stringstream ss;
        IssuedTile it{randomTile(randomNumber(25u)), randomNumber(2048u)};
        ss << it;
        IssuedTile result{};
        ss >> result;
        EXPECT_THAT(result, Eq(it));
    }
}

Y_UNIT_TEST(pack_and_unpack_tile_with_issue)
{
    for (size_t i = 0; i < 50; ++i) {
        IssuedTile it{randomTile(randomNumber(25u)), randomNumber(2048u)};
        const auto packed = packTile(it);
        const auto result = unpackTile(packed);
        EXPECT_THAT(result, Eq(it));
    }
}

Y_UNIT_TEST(parse_key)
{
    std::vector<std::string> keys{
        "--0AAAAAAAA-lwEAAAAAABIAAACsAAAA",
        "--0AAAAAAAA-mAEAAAAAABIAAACsAAAA",
        "CgAAAAAAAAAUAAAAAAAAAB4AAAAoAAAA",
        "AwAAAAAAAAAEAAAAAAAAAAUAAAAGAAAA",
        "CQAAAAAAAAAIAAAAAAAAAAcAAAAGAAAA",
        "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
    };
    std::vector<IssuedTile> tiles{
        {{60923, 104254, 18}, 172},
        {{60923, 104510, 18}, 172},
        {{10, 20, 30}, 40},
        {{3, 4, 5}, 6},
        {{9, 8, 7}, 6},
        {{0, 0, 0}, 0},
    };
    for (size_t i = 0; i < keys.size(); ++i) {
        EXPECT_THAT(parseKey(keys[i]), Eq(tiles[i]));
    }
}

Y_UNIT_TEST(parse_random_key)
{
    for (size_t i = 0; i < 50; ++i) {
        IssuedTile it{randomTile(randomNumber(25u)), randomNumber(2048u)};
        const auto key = makeKey(it);
        const auto result = parseKey(key);
        EXPECT_THAT(result, Eq(it));
    }
}

Y_UNIT_TEST(append_to_file)
{
    const fs::path f = "append_to_file.bin";
    fs::remove(f);
    {
        const uint64_t data1[]{1u, 2u, 3u};
        const uint64_t data2[]{10u, 20u, 30u};
        appendPackedTiles(f, data1);
        appendPackedTiles(f, data2);
    }
    EXPECT_THAT(readAllPackedTiles(f),
        ElementsAre(1u, 2u, 3u, 10u, 20u, 30u));
}

Y_UNIT_TEST(get_last_item_from_file)
{
    const fs::path f = "get_last_item_from_file.bin";
    fs::remove(f);
    {
        const uint64_t data[]{1u, 2u, 3u};
        appendPackedTiles(f, data);
    }
    EXPECT_THAT(lastPackedTile(f), Eq(3u));
}

Y_UNIT_TEST(get_last_item_from_file_with_one_item)
{
    const fs::path f = "get_last_item_from_file_with_one_item.bin";
    fs::remove(f);
    {
        const uint64_t data[]{1u};
        appendPackedTiles(f, data);
    }
    EXPECT_THAT(lastPackedTile(f), Eq(1u));
}

Y_UNIT_TEST(get_last_item_throw_when_file_empty)
{
    const fs::path f = "get_last_item_throw_when_file_empty.bin";
    fs::remove(f);
    {
        fs::ofstream ofs{f};
    }
    EXPECT_ANY_THROW(lastPackedTile(f));
}
}

} // namespace tests
} // namespace impl
} // namespace tileindex
} // namespace maps
