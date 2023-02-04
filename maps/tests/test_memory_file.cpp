#include <maps/factory/libs/dataset/memory_file.h>

#include <maps/factory/libs/unittest/tests_common.h>

namespace maps::factory::dataset::tests {
using namespace factory::tests;

Y_UNIT_TEST_SUITE(memory_file_should) {

Y_UNIT_TEST(write_and_read_data)
{
    const std::vector<uint8_t> data{1, 2, 3};
    const MemoryFile file = MemoryFile::unique("data1.bin");
    file.write(data);
    const std::vector<uint8_t> result = file.read();
    EXPECT_EQ(data, result);
}

Y_UNIT_TEST(delete_file_in_destructor)
{
    {
        const MemoryFile file = MemoryFile::unique("data2.txt");
        file.write("test");
        EXPECT_THAT(MemoryFile::files(), SizeIs(1));
    }
    EXPECT_THAT(MemoryFile::files(), SizeIs(0));
}

Y_UNIT_TEST(read_and_delete_when_moved)
{
    const std::string data = "test data";
    MemoryFile file = MemoryFile::unique("data3.bin");
    file.write(data);
    const std::string result = std::move(file).readString();
    EXPECT_EQ(data, result);
    EXPECT_THAT(MemoryFile::files(), SizeIs(0));
}

} // suite
} // namespace maps::factory::dataset::tests
