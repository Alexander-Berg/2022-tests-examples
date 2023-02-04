#include <maps/factory/libs/storage/ftp_storage.h>
#include <maps/factory/libs/storage/impl/ftp_file.h>

#include <maps/factory/libs/unittest/tests_common.h>

namespace maps::factory::storage::tests {
using namespace testing;
using namespace maps::factory::tests;

Y_UNIT_TEST_SUITE(ftp_storage_should) {

Y_UNIT_TEST(parse_curl_output_for_directory)
{
    EXPECT_THAT(impl::ftpParseCurlOutputLine(
        "-rw-rw-r--    1 1000     1000            0 Mar 25 00:08 file1.txt"),
        impl::IsDirPathPair(false, "file1.txt"));
    EXPECT_THAT(impl::ftpParseCurlOutputLine(
        "drwxrwxr-x    2 1000     1000         4096 Mar 25 00:08 SubDir2"),
        impl::IsDirPathPair(true, "SubDir2"));
    EXPECT_THAT(impl::ftpParseCurlOutputLine(
        "drwxrwxr-x    2 1000     1000         4096 Mar 25 00:05 subdir"),
        impl::IsDirPathPair(true, "subdir"));
    EXPECT_THAT(impl::ftpParseCurlOutputLine(
        "-rw-------    1 1000     1000           23 Mar 25 00:10 tmp-1-b181-7bab.txt"),
        impl::IsDirPathPair(false, "tmp-1-b181-7bab.txt"));
    EXPECT_THAT(impl::ftpParseCurlOutputLine(
        "-rw-rw-r--    1 1000     1000            0 Mar 25 00:08 SomeFile3.txt => Some/Other/Dir"),
        impl::IsDirPathPair(false, "SomeFile3.txt"));
}

Y_UNIT_TEST(not_parse_wrong_curl_output_for_directory)
{
    EXPECT_THROW(impl::ftpParseCurlOutputLine(""), RuntimeError);
    EXPECT_THROW(impl::ftpParseCurlOutputLine("some file 3.txt"), RuntimeError);
    EXPECT_THROW(impl::ftpParseCurlOutputLine(
        "-rw-rw-r--    1 1000     1000            0 Mar 25 00:08 "), RuntimeError);
}

Y_UNIT_TEST(insert_user_in_path)
{
    EXPECT_EQ(ftpStorage("ftps://some.site/some/place", "user", "pass")->pathWithUser("other/place"),
        "ftps://user:pass@some.site/some/place/other/place");
    EXPECT_EQ(ftpStorage("ftps://some.site/some/place", "user", "pass")->pathWithUser(),
        "ftps://user:pass@some.site/some/place");
    EXPECT_EQ(ftpStorage("ftp://some.other.site/", "only_user")->pathWithUser(),
        "ftp://only_user@some.other.site");
    EXPECT_EQ(ftpStorage("ftps://site.site/")->pathWithUser(),
        "ftps://site.site");
}

} // suite
} //namespace maps::factory::storage::tests
