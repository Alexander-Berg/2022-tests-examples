#include <maps/factory/libs/storage/s3_storage.h>
#include <maps/factory/libs/storage/s3_pool.h>
#include <maps/factory/libs/storage/http_storage.h>

#include <maps/factory/libs/storage/local_storage.h>

#include <maps/factory/libs/unittest/tests_common.h>

namespace maps::factory::storage::tests {
using namespace testing;
using namespace maps::factory::tests;

namespace {

storage::AuthPtr testS3Auth()
{
    return S3CloudStorage::keyAuth({.accessKeyId = "1234567890", .secretKey = "abcdefabcdef"});
}

std::string testS3Endpoint()
{
    std::string port = GetEnv("S3MDS_PORT");
    REQUIRE(!port.empty(), "Cannot connect to local S3 server");
    return "http://127.0.0.1:" + port;
}

S3Ptr testS3(const std::string& bucket)
{
    auto s3 = storage::s3Storage(testS3Endpoint(), bucket, "", testS3Auth());
    s3->createBucket();
    return s3;
}

} // namespace

Y_UNIT_TEST_SUITE(s3_storage_should) {

Y_UNIT_TEST(put_and_get_file)
{
    auto s3 = testS3(this->Name_);
    auto file = s3->file("test.txt");
    EXPECT_FALSE(file->exists());
    const std::string str = "test data";
    file->writeString(str);
    EXPECT_TRUE(file->exists());
    EXPECT_EQ(file->readToString(), str);
    EXPECT_EQ(file->size(), static_cast<Bytes>(str.size()));
    EXPECT_EQ(file->md5(), "eb733a00c0c9d336e65691a37ab54293");
}

Y_UNIT_TEST(copy_between_filesystem)
{
    auto s3 = testS3(this->Name_);
    auto file = s3->file("test.txt");
    const std::string str = "test data";
    file->writeString(str);
    auto local = localStorage("./tmp")->dir(this->Name_);
    auto localFile = local->file("test.txt");
    file->copy(*localFile);
    EXPECT_EQ(localFile->readToString(), str);

    auto other = s3->file("other.txt");
    localFile->copy(*other);
    EXPECT_EQ(other->readToString(), str);
}

Y_UNIT_TEST(copy_to_cloud)
{
    auto s3 = testS3(this->Name_);
    auto file = s3->file("test.txt");
    const std::string str = "test data";
    file->writeString(str);
    auto other = s3->file("other.txt");
    file->copy(*other);
    EXPECT_EQ(other->readToString(), str);
}

Y_UNIT_TEST(list_objects)
{
    auto s3 = testS3(this->Name_);
    s3->file("test1.txt")->writeString("test 1");
    s3->file("test2.txt")->writeString("test 2");
    EXPECT_THAT(toStrings(s3->list(Select::FilesRecursive)),
        ElementsAre("test1.txt", "test2.txt"));
}

Y_UNIT_TEST(remove_file)
{
    auto s3 = testS3(this->Name_);
    auto file = s3->file("test.txt");
    file->writeString("test data");
    EXPECT_TRUE(file->remove());
    EXPECT_FALSE(file->exists());
}

Y_UNIT_TEST(sub_dirs)
{
    auto s3 = testS3(this->Name_);
    auto file = s3->dir("a")->dir("b/c")->file("test.txt");
    const std::string str = "test data";
    file->writeString(str);
    EXPECT_EQ(file->readToString(), str);
    auto other = s3->dir("foo/bar")->dir("baz")->file("other.txt");
    file->copy(*other);
    EXPECT_EQ(other->readToString(), str);
    EXPECT_THAT(toStrings(s3->list(Select::FilesRecursive)),
        ElementsAre("a/b/c/test.txt", "foo/bar/baz/other.txt"));
}

Y_UNIT_TEST(read_without_auth)
{
    auto s3 = testS3(this->Name_);
    auto file = s3->file("test.txt");
    const std::string str = "test data";
    file->writeString(str);
    auto http = httpStorage(s3->absPath());
    auto httpFile = http->file("test.txt");
    EXPECT_TRUE(httpFile->exists());
    EXPECT_EQ(httpFile->readToString(), str);
}

Y_UNIT_TEST(use_pool)
{
    const auto endpoint = testS3Endpoint();
    S3Pool pool(testS3Auth());
    auto local = pool.storage("./tmp");
    auto s1 = pool.storage(testS3("pool_bucket_1")->absPath());
    auto s2 = pool.storage(testS3("pool_bucket_1")->absPath());
    auto s3 = pool.storage(testS3("pool_bucket_2")->absPath());
    auto s4 = pool.storage(testS3("pool_bucket_1")->dir("dir")->absPath());
    auto s5 = pool.storage(testS3("pool_bucket_1")->dir("dir")->dir("dir2")->absPath());

    EXPECT_EQ(local->absPath().native(), "./tmp");
    EXPECT_EQ(s1->absPath().native(), endpoint + "/pool_bucket_1");
    EXPECT_EQ(s2->absPath().native(), endpoint + "/pool_bucket_1");
    EXPECT_EQ(s3->absPath().native(), endpoint + "/pool_bucket_2");
    EXPECT_EQ(s4->absPath().native(), endpoint + "/pool_bucket_1/dir");
    EXPECT_EQ(s5->absPath().native(), endpoint + "/pool_bucket_1/dir/dir2");

    local->file("test.txt")->writeString("test 1");
    EXPECT_EQ(local->file("test.txt")->readToString(), "test 1");

    s1->file("test.txt")->writeString("test 2");
    EXPECT_EQ(s1->file("test.txt")->readToString(), "test 2");
    EXPECT_EQ(s2->file("test.txt")->readToString(), "test 2");
    EXPECT_FALSE(s3->file("test.txt")->exists());

    s1->dir("dir")->file("test.txt")->writeString("test 3");
    EXPECT_EQ(s4->file("test.txt")->readToString(), "test 3");

    s1->dir("dir")->dir("dir2")->file("test.txt")->writeString("test 4");
    EXPECT_EQ(s4->dir("dir2")->file("test.txt")->readToString(), "test 4");
    EXPECT_EQ(s5->file("test.txt")->readToString(), "test 4");
}

} // suite

} //namespace maps::factory::storage::tests
