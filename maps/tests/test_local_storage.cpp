#include <maps/factory/libs/storage/local_storage.h>

#include <maps/factory/libs/unittest/tests_common.h>
#include <maps/libs/introspection/include/comparison.h>
#include <maps/libs/introspection/include/stream_output.h>

#include <boost/filesystem.hpp>

namespace maps::factory::storage {
using introspection::operator<<;
using introspection::operator==;
using introspection::operator!=;
}

namespace maps::factory::storage::tests {
using namespace testing;
using namespace maps::factory::tests;

Y_UNIT_TEST_SUITE(filesystem_storage_should) {

const std::string src = ArcadiaSourceRoot() + "/maps/factory/test_data/dg_deliveries/058800151040_01";

Y_UNIT_TEST(list_files)
{
    EXPECT_THAT(toStrings(FilesystemStorage::make(src)->list(Select::Files)),
        UnorderedElementsAre(
            "058800151040_01_LAYOUT.JPG",
            "058800151040_01_P001_FITTED.GEOJSON",
            "058800151040_01_P001_MUL.TIF",
            "058800151040_01_P001_PAN.TIF",
            "058800151040_01_P001_STATS.JSON",
            "058800151040_01_P001_WARPED.GEOJSON",
            "058800151040_01_README.TXT",
            "058800151040_01_README.XML"));

    EXPECT_THAT(toStrings(FilesystemStorage::make(src)->dir("058800151040_01_P001_MUL")->list(Select::Files)),
        UnorderedElementsAre(
            "18SEP10082942-M2AS-058800151040_01_P001-BROWSE.JPG",
            "18SEP10082942-M2AS-058800151040_01_P001.IMD",
            "18SEP10082942-M2AS-058800151040_01_P001.RPB",
            "18SEP10082942-M2AS-058800151040_01_P001.TIL",
            "18SEP10082942-M2AS-058800151040_01_P001.XML",
            "18SEP10082942-M2AS-058800151040_01_P001_README.TXT",
            "18SEP10082942-M2AS_R1C1-058800151040_01_P001.TIF",
            "18SEP10082942-M2AS_R1C2-058800151040_01_P001.TIF",
            "18SEP10082942-M2AS_R1C3-058800151040_01_P001.TIF",
            "INTERNAL.TXT"));
}

Y_UNIT_TEST(list_files_recursively)
{
    auto files = FilesystemStorage::make(src)->list(Select::FilesRecursive);
    EXPECT_EQ(files.size(), 52u);
}

Y_UNIT_TEST(get_absolute_file_path)
{
    auto st = FilesystemStorage::make(src);
    auto path1 = st->absPath("GIS_FILES/058800151040_01_ORDER_SHAPE.shx").native();
    auto path2 = st->dir("GIS_FILES")->absPath("058800151040_01_ORDER_SHAPE.shx").native();
    EXPECT_THAT(path1, EndsWith("058800151040_01/GIS_FILES/058800151040_01_ORDER_SHAPE.shx"));
    EXPECT_EQ(path1, path2);
}

Y_UNIT_TEST(get_current_directory_path)
{
    auto st = FilesystemStorage::make(src);
    EXPECT_EQ(st->absPath("").native(), src);
    EXPECT_EQ(st->absPath().native(), src);
    EXPECT_EQ(st->dir("")->absPath("").native(), src);
    EXPECT_EQ(st->dir("GIS_FILES")->dir("..")->absPath("").native(), src);
}

Y_UNIT_TEST(list_directories)
{
    auto files = FilesystemStorage::make(src)->list(Select::Directories);
    auto expected = {
        "058800151040_01_P001_MUL",
        "058800151040_01_P001_PAN",
        "GIS_FILES"
    };
    EXPECT_THAT(toStrings(files), UnorderedElementsAreArray(expected));
}

Y_UNIT_TEST(read_file_to_string)
{
    auto st = FilesystemStorage::make(src);
    EXPECT_THAT(st->file("058800151040_01_README.XML")->readToString(),
        StartsWith("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n<README>\n  <VERSION>28.3</VERSION>"));
    EXPECT_THROW(st->file("NOT_FOUND.XML")->readToString(), RuntimeError);
}

Y_UNIT_TEST(check_file_exists)
{
    auto st = FilesystemStorage::make(src);
    EXPECT_TRUE(st->file("058800151040_01_README.XML")->exists());
    EXPECT_EQ(st->file("058800151040_01_README.XML")->size(), 5153u);
    EXPECT_FALSE(st->file("NOT_FOUND.XML")->exists());
}

Y_UNIT_TEST(list_files_in_not_existing_dir)
{
    auto st = FilesystemStorage::make("not_existing");
    EXPECT_TRUE(st->list(Select::Files).empty());
    EXPECT_TRUE(st->list(Select::Directories).empty());
    EXPECT_TRUE(st->list(Select::FilesRecursive).empty());
    EXPECT_FALSE(st->file("058800151040_01_README.XML")->exists());
}

Y_UNIT_TEST(clone_self)
{
    auto st = FilesystemStorage::make(src);
    EXPECT_THAT(toStrings(st->dir("058800151040_01_P001_MUL")->list(Select::Files)), SizeIs(10u));
}

Y_UNIT_TEST(write_string_to_file)
{
    fs::path dir = "tmp_01";
    auto dst = FilesystemStorage::make(dir);
    std::string data = "test data";
    dst->file("a/b/tmp.txt")->writeString(data);
    EXPECT_TRUE(fs::exists(dir / "a" / "b" / "tmp.txt"));
    EXPECT_EQ(dst->file("a/b/tmp.txt")->readToString(), data);
}

Y_UNIT_TEST(read_write_vector_to_file)
{
    fs::path dir = "tmp_012";
    auto dst = FilesystemStorage::make(dir);
    std::string dataStr = "test data";
    std::vector<uint8_t> data(dataStr.size());
    memcpy(data.data(), dataStr.data(), dataStr.size());
    dst->file("a/b/tmp.txt")->writeVector(data);
    EXPECT_TRUE(fs::exists(dir / "a" / "b" / "tmp.txt"));
    EXPECT_EQ(dst->file("a/b/tmp.txt")->readToString(), dataStr);
    EXPECT_EQ(dst->file("a/b/tmp.txt")->readToVector(), data);
}

Y_UNIT_TEST(create_file)
{
    fs::path dir = "tmp_011";
    auto dst = FilesystemStorage::make(dir);
    dst->file("a/b/tmp.txt")->touch();
    EXPECT_TRUE(fs::exists(dir / "a" / "b" / "tmp.txt"));
}

Y_UNIT_TEST(remove_file)
{
    fs::path dir = "tmp_02";
    {
        auto dst = FilesystemStorage::make(dir);
        fs::create_directories(dir);
        fs::ofstream{dir / "tmp.txt"} << "test";
        EXPECT_TRUE(fs::exists(dir / "tmp.txt"));
        dst->file("tmp.txt")->remove();
        EXPECT_FALSE(fs::exists(dir / "tmp.txt"));
    }
    EXPECT_FALSE(fs::exists(dir));
}

Y_UNIT_TEST(remove_directory)
{
    fs::path dir = "tmp_03/";
    {
        auto dst = FilesystemStorage::make(dir);
        fs::create_directories(dir);
        dst->file("tmp.txt")->remove();
    }
    EXPECT_FALSE(fs::exists(dir));
}

Y_UNIT_TEST(overwrite_file)
{
    fs::path dir = "tmp_04";

    auto dst = FilesystemStorage::make(dir);
    auto file = dst->file("tmp.txt");
    file->writeString("TestTestTest\nTest\n");
    file->writeString("Foo");
    EXPECT_EQ(file->readToString(), "Foo");
}

Y_UNIT_TEST(rename_file)
{
    fs::path dir = "tmp_05";

    auto dst = FilesystemStorage::make(dir);
    auto file = dst->file("tmp.txt");
    file->writeString("Foo");
    auto file2 = dst->file("tmp2.txt");
    file->move(*file2);
    EXPECT_FALSE(file->exists());
    EXPECT_EQ(file2->readToString(), "Foo");
}

Y_UNIT_TEST(read_md5)
{
    const std::pair<std::string, std::string> fileMd5[]{
        {"058800151040_01_P001_MUL/18SEP10082942-M2AS-058800151040_01_P001-BROWSE.JPG",
            "2c5d3c2879397e787c33d8df166d9e14"},
        {"058800151040_01_P001_MUL/18SEP10082942-M2AS-058800151040_01_P001.IMD",
            "97806d94bddd1a5eb565a5098253c4dc"},
        {"058800151040_01_P001_MUL/18SEP10082942-M2AS-058800151040_01_P001.RPB",
            "8958eee6b39b749bf0007e983a09fc15"},
        {"058800151040_01_P001_MUL/18SEP10082942-M2AS-058800151040_01_P001.TIL",
            "83a748a1eeb519d78c174a7cffed27ad"},
        {"058800151040_01_P001_MUL/18SEP10082942-M2AS-058800151040_01_P001.XML",
            "ff674ca6d36d627e6d82d030cef4ffe2"},
        {"058800151040_01_P001_MUL/18SEP10082942-M2AS-058800151040_01_P001_README.TXT",
            "1868e2a78e589072410305630ece5e82"},
        {"058800151040_01_P001_MUL/INTERNAL.TXT",
            "c6dac7d6f42983cff1a2f310520398a0"},
        {"058800151040_01_P001_MUL/18SEP10082942-M2AS_R1C1-058800151040_01_P001.TIF",
            "b2f1a64f182f756ec948598369a9c390"},
        {"058800151040_01_P001_MUL/18SEP10082942-M2AS_R1C2-058800151040_01_P001.TIF",
            "1c73a612009a8e8309f617a4d7e54cd1"},
        {"058800151040_01_P001_MUL/18SEP10082942-M2AS_R1C3-058800151040_01_P001.TIF",
            "cbaa3c4ebf5f074bd38f0d4abfc5fc59"},
    };
    auto st = FilesystemStorage::make(src);
    for (const auto&[file, md5]: fileMd5) {
        EXPECT_EQ(st->file(file)->md5(), md5);
    }
}

} // suite

Y_UNIT_TEST_SUITE(cloud_storage_should) {

Y_UNIT_TEST(parse_cloud_path)
{
    auto check = [](auto path, auto dir, auto host, auto bucket) {
        EXPECT_EQ(ParsedPath::parse(path), (ParsedPath{dir, host, bucket})) << path;
    };

    check("/", "/", "", "");
    check("./", "./", "", "");
    check("./dir/", "./dir/", "", "");
    check("/dir", "/dir", "", "");
    check("/dir/a/../", "/dir/a/../", "", "");
    check("http://s3.mds.yandex.net/bucket1/dir/", "dir/", "s3.mds.yandex.net", "bucket1");
    check("https://s3.mds.yandex.net/bucket/dir2", "dir2", "s3.mds.yandex.net", "bucket");
    check("http://s3.mds.yandex.net/bucket/dir/dir2/", "dir/dir2/", "s3.mds.yandex.net", "bucket");
    check("https://s3.yandex.net/bucket/dir/dir2", "dir/dir2", "s3.yandex.net", "bucket");
    check("s3://s3.mds.yandex.net/bucket2/", "", "s3.mds.yandex.net", "bucket2");
    check("s3://s3.mds.yandex.net/bucket2", "", "s3.mds.yandex.net", "bucket2");
    check("s3://s3.mds.yandex.net/bucket/dir/../dir2", "dir/../dir2", "s3.mds.yandex.net", "bucket");

    EXPECT_THROW(ParsedPath::parse("s3mds://s3.mds.yandex.net/maps-sat"), RuntimeError);
    EXPECT_THROW(ParsedPath::parse(""), RuntimeError);
    EXPECT_THROW(ParsedPath::parse("file:///dir5/file6.txt"), RuntimeError);
    EXPECT_THROW(ParsedPath::parse("file://localhost/dir/file.txt"), RuntimeError);
    EXPECT_THROW(ParsedPath::parse("file://dir/file.txt"), RuntimeError);
    EXPECT_THROW(ParsedPath::parse("ftp://dir/file.txt"), RuntimeError);
    EXPECT_THROW(ParsedPath::parse("file://123.45.83.99/dir/file.txt"), RuntimeError);
    EXPECT_THROW(ParsedPath::parse("s30://s3.mds.yandex.net/bucket/dir/file.txt"), RuntimeError);
    EXPECT_THROW(ParsedPath::parse("/test/https://s3.mds.yandex.net/bucket/dir/file.txt"), RuntimeError);
}

Y_UNIT_TEST(check_files_md5)
{
    auto tmp = localStorage("./tmp/check_files_md5");
    auto file1 = tmp->file("dir/test.file.1.txt");
    file1->writeString("test data\n");
    auto file2 = tmp->file("file2.txt");
    file2->writeString("test\ndata\n\n");
    constexpr auto sum1 = "39a870a194a787550b6b5d1f49629236";
    constexpr auto sum2 = "877357363503c39e2fc2263ebcd8301e";

    EXPECT_EQ(file1->md5(), sum1);
    EXPECT_EQ(file2->md5(), sum2);

    EXPECT_FALSE(file1->checkMd5());
    tmp->file("dir/test.file.1.txt.md5")->writeString(sum1);
    EXPECT_TRUE(file1->checkMd5());
    tmp->file("dir/test.file.1.txt.md5")->writeString(sum2);
    EXPECT_THROW(file1->checkMd5(), RuntimeError);

    EXPECT_FALSE(file2->checkMd5());
    tmp->file("file2.txt.MD5")->writeString(sum2);
    EXPECT_TRUE(file2->checkMd5());
    tmp->file("file2.txt.MD5")->writeString(sum1);
    EXPECT_THROW(file2->checkMd5(), RuntimeError);
}

} // suite

} //namespace maps::factory::storage::tests
