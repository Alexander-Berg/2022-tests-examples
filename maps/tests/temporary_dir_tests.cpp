#include <maps/libs/common/include/temporary_dir.h>
#include <maps/libs/common/include/file_utils.h>

#include <library/cpp/testing/gtest/gtest.h>

namespace fs = std::filesystem;

namespace maps::common::tests {
using namespace ::testing;
namespace {

std::string stringizePath(const fs::path& path) {
    return path.string();
}

} //anonymous namespace

TEST(temporary_dir_should, remove_all_contents) {
    fs::path tempDirPath;
    fs::path tempFile;
    {
        TemporaryDir tempDir("./tmp");
        tempDirPath = tempDir.path();
        EXPECT_TRUE(fs::exists(tempDirPath));

        tempFile = tempDirPath / "temp-file";
        writeFile(tempFile.string(), "some test contents");
        EXPECT_TRUE(fs::exists(tempFile));
    }
    EXPECT_FALSE(fs::exists(tempDirPath));
    EXPECT_FALSE(fs::exists(tempFile));
}

TEST(temporary_dir_should, test_conversion) {
    {
        TemporaryDir tempDir("./tmp");
#ifdef _WIN32
        EXPECT_THAT(stringizePath(tempDir), HasSubstr("tmp\\temp-dir"));
#else
        EXPECT_THAT(stringizePath(tempDir), HasSubstr("tmp/temp-dir"));
#endif
    }
    {
        TemporaryDir tempDir("./tmp");
#ifdef _WIN32
        EXPECT_THAT((tempDir / "magic").string(), EndsWith("\\magic"));
#else
        EXPECT_THAT((tempDir / "magic").string(), EndsWith("/magic"));
#endif
    }
}

} //namespace maps::common::tests
