#include <maps/factory/libs/dataset/block_cache.h>
#include <maps/factory/libs/unittest/tests_common.h>

namespace maps::factory::dataset::tests {
using namespace maps::factory::tests;

Y_UNIT_TEST_SUITE(block_cache_should) {

Y_UNIT_TEST(create_empty_block)
{
    const Index element = 2;
    const Index bands = 4;
    TLruBlockCache bc(BLOCK_AREA * element * bands * 20);
    TBlockKey key({10, 20}, 3, "test");
    BlockPtr block = bc.Block(key, element, bands);
    EXPECT_EQ(block->SizeBytes(), element * bands * BLOCK_AREA);
    EXPECT_FALSE(block->IsLoaded());
    EXPECT_FALSE(block->IsLocked());
}

Y_UNIT_TEST(get_block)
{
    const Index element = 2;
    const Index bands = 4;
    TLruBlockCache bc(BLOCK_AREA * element * bands * 20);
    TBlockKey key1({10, 20}, 3, "test");
    TBlockKey key2({20, 10}, 3, "test");
    BlockPtr block11 = bc.Block(key1, element, bands);
    BlockPtr block21 = bc.Block(key2, element, bands);
    BlockPtr block12 = bc.Block(key1, element, bands);
    BlockPtr block22 = bc.Block(key2, element, bands);
    EXPECT_EQ(block11, block12);
    EXPECT_EQ(block21, block22);
    EXPECT_NE(block11, block21);
    EXPECT_NE(block12, block22);
}
/*

Y_UNIT_TEST(evict_block)
{
    const Index element = 2;
    const Index bands = 4;
    const size_t oneBlockSize = BLOCK_AREA * element * bands;
    TLruBlockCache bc(oneBlockSize * 2);
    TBlockKey key1({10, 20}, 1, "test");
    TBlockKey key2({10, 20}, 2, "test");
    TBlockKey key3({10, 20}, 3, "test");
    EXPECT_EQ(bc.size(), 0u);
    EXPECT_EQ(bc.sizeBytes(), 0u);
    EXPECT_EQ(bc.maxSizeBytes(), oneBlockSize * 2);
    BlockPtr block11 = bc.Block(key1, element, bands);
    EXPECT_EQ(bc.size(), 1u);
    EXPECT_EQ(bc.sizeBytes(), oneBlockSize);
    BlockPtr block21 = bc.Block(key2, element, bands);
    EXPECT_EQ(bc.size(), 2u);
    EXPECT_EQ(bc.sizeBytes(), oneBlockSize * 2);
    BlockPtr block31 = bc.Block(key3, element, bands); // evict 1
    EXPECT_EQ(bc.size(), 2u);
    EXPECT_EQ(bc.sizeBytes(), oneBlockSize * 2);
    BlockPtr block12 = bc.Block(key1, element, bands); // evict 2
    EXPECT_EQ(bc.size(), 2u);
    EXPECT_EQ(bc.sizeBytes(), oneBlockSize * 2);
    BlockPtr block13 = bc.Block(key1, element, bands);
    EXPECT_EQ(bc.size(), 2u);
    EXPECT_EQ(bc.sizeBytes(), oneBlockSize * 2);
    BlockPtr block32 = bc.Block(key3, element, bands);
    EXPECT_EQ(bc.size(), 2u);
    EXPECT_EQ(bc.sizeBytes(), oneBlockSize * 2);
    //EXPECT_NE(block11, block12);
    EXPECT_EQ(block12, block13);
    EXPECT_EQ(block31, block32);
}

Y_UNIT_TEST(clear_cache)
{
    const Index element = 2;
    const Index bands = 4;
    const size_t oneBlockSize = BLOCK_AREA * element * bands;
    TLruBlockCache bc(oneBlockSize * 2);
    TBlockKey key1({10, 20}, 1, "test");
    BlockPtr block11 = bc.Block(key1, element, bands);
    BlockPtr block12 = bc.Block(key1, element, bands);
    bc.clear();
    EXPECT_EQ(bc.size(), 0u);
    EXPECT_EQ(bc.sizeBytes(), 0u);
    EXPECT_EQ(bc.maxSizeBytes(), oneBlockSize * 2);
    BlockPtr block13 = bc.Block(key1, element, bands);
    EXPECT_EQ(block11, block12);
    EXPECT_NE(block11, block13);
}
*/

} // suite

} //namespace maps::factory::dataset::tests
