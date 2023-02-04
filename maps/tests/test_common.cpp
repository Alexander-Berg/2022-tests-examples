#include <maps/factory/libs/processing/common.h>

#include <maps/factory/libs/processing/tests/test_s3.h>

#include <maps/factory/libs/unittest/tests_common.h>
#include <maps/factory/libs/storage/local_storage.h>
#include <maps/factory/libs/unittest/fixture.h>

namespace maps::factory::processing::tests {
using namespace testing;
using namespace maps::factory::tests;
using namespace maps::factory::storage;

namespace {

const std::string DATA_DIR = SRC_("data");

const std::string ARCHIVE_NAME = "11123809";

const std::string EXTRACTED_DIR = ArcadiaSourceRoot()
                                  + "/maps/factory/test_data/scanex_deliveries/11123809_extracted_mini";

} // namespace

Y_UNIT_TEST_SUITE(common_tasks_should) {

Y_UNIT_TEST(download_archive_from_mds_to_temp)
{
    const auto mds = testS3(this->Name_); // MDS is the same as S3 with anonymous GET
    const auto tmpDir = localStorage("./tmp")->dir(this->Name_);
    const auto dataDir = localStorage(DATA_DIR);
    dataDir->file(ARCHIVE_NAME)->copy(*mds->file(ARCHIVE_NAME));

    const DownloadFileFromMds worker;
    const LocalFilePath localArchivePath = worker(
        MdsFilePath(mds->file(ARCHIVE_NAME)->absPath().native()),
        LocalDirectoryPath(tmpDir->absPath().native()));

    EXPECT_EQ(localArchivePath.val(), tmpDir->absPath() / ARCHIVE_NAME);
    EXPECT_TRUE(tmpDir->file(ARCHIVE_NAME)->exists());
    EXPECT_EQ(tmpDir->file(ARCHIVE_NAME)->readToString(), dataDir->file(ARCHIVE_NAME)->readToString());
}

Y_UNIT_TEST(extract_local_archive_to_directory)
{
    const auto tmpDir = localStorage("./tmp")->dir(this->Name_);
    const auto dataDir = localStorage(DATA_DIR);
    const auto extractedTmpDir = tmpDir->dir("extracted");
    const auto extractedDataDir = localStorage(EXTRACTED_DIR);

    const ExtractArchiveToDirectory worker;
    worker(
        LocalFilePath(dataDir->file(ARCHIVE_NAME)->absPath().native()),
        LocalDirectoryPath(extractedTmpDir->absPath().native()));

    const auto paths = extractedTmpDir->list(Select::Files);
    EXPECT_EQ(paths.size(), 6u);
    for (const auto& path: paths) {
        EXPECT_EQ(extractedTmpDir->file(path)->readToString(), extractedDataDir->file(path)->readToString());
    }
}

Y_UNIT_TEST(check_file_md5)
{
    const auto dataDir = localStorage(DATA_DIR);
    const auto file = dataDir->file("11123809");
    constexpr auto expectedMd5 = "08d3838fe4efe0c19dc244d19d0caa36";

    const CheckFileMd5 worker;
    EXPECT_NO_THROW(worker(LocalFilePath(file->absPath().native()), FileMd5Checksum(expectedMd5)));
    EXPECT_THROW(
        worker(LocalFilePath(file->absPath().native()), FileMd5Checksum("08d3838fe4efe0c19dc244d19d0caa37")),
        RuntimeError);
}

Y_UNIT_TEST(upload_local_directory_to_s3)
{
    const auto s3DataDir = testS3(this->Name_)->dir("uploaded_data");
    const auto localDir = localStorage("./tmp")->dir(this->Name_);
    localDir->file("test1.txt")->writeString("test data\n");
    localDir->file("test2.txt")->writeString("test\ndata\n\n");

    const UploadDirectoryToS3 worker(testS3Auth());
    worker(
        LocalDirectoryPath(localDir->absPath().native()),
        S3DirectoryPath(s3DataDir->absPath().native()));

    EXPECT_EQ(s3DataDir->file("test1.txt")->readToString(), "test data\n");
    EXPECT_EQ(s3DataDir->file("test2.txt")->readToString(), "test\ndata\n\n");
}

Y_UNIT_TEST(download_directory_from_s3)
{
    const auto s3 = testS3(this->Name_);
    const auto s3DataDir = s3->dir("uploaded_data");
    const auto localDir = localStorage("./tmp")->dir(this->Name_);
    s3->dir("other")->file("wrong.txt")->writeString("wrong\n");
    s3DataDir->file("test1.txt")->writeString("test data\n");
    s3DataDir->file("test2.txt")->writeString("test\ndata\n\n");

    const DownloadDirectoryFromS3 worker{
        .pool = storage::S3Pool{testS3Auth()}
    };
    worker(
        S3DirectoryPath(s3DataDir->absPath().native()),
        LocalDirectoryPath(localDir->absPath().native()));

    EXPECT_EQ(localDir->file("test1.txt")->readToString(), "test data\n");
    EXPECT_EQ(localDir->file("test2.txt")->readToString(), "test\ndata\n\n");
}

Y_UNIT_TEST(remove_local_directory)
{
    const auto tmpDir = localStorage("./tmp")->dir(this->Name_);
    tmpDir->file(ARCHIVE_NAME)->writeString("\1\2\3");
    const auto extractedDir = tmpDir->dir("extracted");
    extractedDir->file("test1.txt")->writeString("test data\n");
    extractedDir->file("test2.txt")->writeString("test\ndata\n\n");

    EXPECT_TRUE(extractedDir->file("test1.txt")->exists());
    EXPECT_TRUE(extractedDir->file("test2.txt")->exists());

    const RemoveLocalDirectory worker;
    worker(
        LocalDirectoryPath(tmpDir->absPath().native()));

    EXPECT_FALSE(extractedDir->file("test1.txt")->exists());
    EXPECT_FALSE(extractedDir->file("test2.txt")->exists());
}

Y_UNIT_TEST(run_local_ecstatic)
{
    const auto tmpDir = localStorage("./tmp")->dir(this->Name_);
    const auto srcDir = tmpDir->dir("src");
    srcDir->file("tmp.txt")->writeString("test");
    const LocalEcstaticClient client(tmpDir->absPathStr());
    const EcstaticDataset ds("a=1");
    UploadFileToEcstatic{&client}(LocalDirectoryPath(srcDir->absPathStr()), ds);
    EXPECT_EQ(tmpDir->dir("hold/a=1/testing")->file("tmp.txt")->readToString(), "test");
    ActivateEcstaticDataset{&client}(ds, EcstaticBranch("testing"));
    EXPECT_EQ(tmpDir->dir("a=1/testing")->file("tmp.txt")->readToString(), "test");
    DeactivateEcstaticDataset{&client}(ds, EcstaticBranch("testing"));
    EXPECT_FALSE(tmpDir->dir("a=1/testing")->file("tmp.txt")->exists());
    RemoveEcstaticDataset{&client}(ds);
    EXPECT_FALSE(tmpDir->dir("hold/a=1/testing")->file("tmp.txt")->exists());
}

} // suite

} //namespace maps::factory::delivery::tests
