#include <maps/factory/libs/common/archive.h>

#include <maps/libs/common/include/file_utils.h>

#include <maps/factory/libs/unittest/tests_common.h>

namespace maps::factory::tests {

Y_UNIT_TEST_SUITE(archive_should) {

Y_UNIT_TEST(list_files)
{
    const std::string src = SRC_("data/test_archive.tar");

    const auto files = Archive(src).listEntries();

    EXPECT_THAT(toStrings(files), ElementsAre("test1.txt", "test2.txt"));
}

Y_UNIT_TEST(read_files)
{
    const Archive ar(SRC_("data/test_archive.tar").c_str());

    EXPECT_EQ(ar.readEntry("test1.txt"), "test data\n");
    EXPECT_EQ(ar.readEntry("test2.txt"), "test\ndata\n\n");
}

Y_UNIT_TEST(read_compressed_files)
{
    const Archive ar(SRC_("data/test_archive.tar.gz").c_str());

    EXPECT_EQ(ar.readEntry("test1.txt"), "test data\n");
    EXPECT_EQ(ar.readEntry("test2.txt"), "test\ndata\n\n");
}

Y_UNIT_TEST(extract_tar)
{
    const std::string dst = "tmp_extract_tar";
    const std::string src = SRC_("data/test_archive.tar");

    Archive(src).extract(dst);

    EXPECT_EQ(common::readFileToString(dst + "/test1.txt"), "test data\n");
    EXPECT_EQ(common::readFileToString(dst + "/test2.txt"), "test\ndata\n\n");
}

Y_UNIT_TEST(extract_tar_without_extension)
{
    const std::string dst = "tmp_extract_tar_without_extension";
    const std::string src = SRC_("data/test_archive_tar");

    Archive(src).extract(dst);

    EXPECT_EQ(common::readFileToString(dst + "/test1.txt"), "test data\n");
    EXPECT_EQ(common::readFileToString(dst + "/test2.txt"), "test\ndata\n\n");
}

Y_UNIT_TEST(extract_tar_gz)
{
    const std::string dst = "tmp_extract_tar_gz";
    const std::string src = SRC_("data/test_archive.tar.gz");

    Archive(src).extract(dst);

    EXPECT_EQ(common::readFileToString(dst + "/test1.txt"), "test data\n");
    EXPECT_EQ(common::readFileToString(dst + "/test2.txt"), "test\ndata\n\n");
}

Y_UNIT_TEST(extract_tar_gz_without_extension)
{
    const std::string dst = "tmp_extract_tar_gz_without_extension";
    const std::string src = SRC_("data/test_archive_tar_gz");

    Archive(src).extract(dst);

    EXPECT_EQ(common::readFileToString(dst + "/test1.txt"), "test data\n");
    EXPECT_EQ(common::readFileToString(dst + "/test2.txt"), "test\ndata\n\n");
}

Y_UNIT_TEST(extract_tar_xz_with_subdirs)
{
    const std::string dst = "tmp_extract_tar_xz_with_subdirs";
    const std::string src = SRC_("data/test_archive_subdirs.tar.xz");

    Archive(src).extract(dst);

    EXPECT_EQ(common::readFileToString(dst + "/dir1/file1.txt"), "test\n");
    EXPECT_EQ(common::readFileToString(dst + "/dir1/dir2/file1.txt"), "test 2\n");
}

} // suite

} // namespace maps::factory::tests
