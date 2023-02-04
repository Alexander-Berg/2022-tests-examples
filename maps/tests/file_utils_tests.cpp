#include <maps/libs/common/include/file_utils.h>
#include <maps/libs/common/include/exception.h>

#include <library/cpp/testing/gtest/gtest.h>
#include <util/system/fstat.h>

#include <algorithm>
#include <cstdint>
#include <string>
#include <vector>

namespace fs = std::filesystem;

namespace maps::common::tests {

using namespace ::testing;

namespace {

const std::string TEST_FILE_NAME = "./tests/data/file.dat";

const std::string EXPECTED_TEXT_CONTENT =
    "SOME_DATA_THAT_CAN'T_APPEAR_RANDOMLY\n"
    "SOME_DATA_THAT_CAN'T_APPEAR_RANDOMLY\n"
    "SOME_DATA_THAT_CAN'T_APPEAR_RANDOMLY\n"
    "SOME_DATA_THAT_CAN'T_APPEAR_RANDOMLY\n";

const std::vector<uint8_t> EXPECTED_BINARY_CONTENT{
    reinterpret_cast<const uint8_t*>(EXPECTED_TEXT_CONTENT.data()),
    reinterpret_cast<const uint8_t*>(EXPECTED_TEXT_CONTENT.data() + EXPECTED_TEXT_CONTENT.size()),
};

const std::vector<char> EXPECTED_CHAR_VECTOR_CONTENT(
    EXPECTED_TEXT_CONTENT.begin(),
    EXPECTED_TEXT_CONTENT.end()
);

const std::string TRANSACTION_FILE_NAME = "./output.txt";
const std::string TRANSACTION_FILE_TEXT = "Hello world!";
const std::string TRANSACTION_FILE_OTHER_TEXT = "SOME_DATA_THAT_CAN'T_APPEAR_RANDOMLY";
const std::string TRANSACTION_FILE_DIR = "transaction";

struct TmpFile {
    std::string path;
    ~TmpFile() { removeFile(path); }
    operator const std::string&() const { return path; }
};

} //anonymous namespace

TEST(File_utils_should, read_file_size) {
    EXPECT_EQ(getFileSize(TEST_FILE_NAME), EXPECTED_TEXT_CONTENT.size());
}

TEST(File_utils_should, read_file) {
    EXPECT_EQ(readFileToString(TEST_FILE_NAME), EXPECTED_TEXT_CONTENT);
    EXPECT_EQ(readFileToVector(TEST_FILE_NAME), EXPECTED_BINARY_CONTENT);
    EXPECT_EQ(readFileToCharVector(TEST_FILE_NAME), EXPECTED_CHAR_VECTOR_CONTENT);
}

#ifndef _WIN32
TEST(File_utils_should, read_text_file_via_symlink) {
    TmpFile link{"./tmp_text_symlink"};
    fs::create_symlink(TEST_FILE_NAME, link.path);
    const auto textSize = getFileSize(link);
    const auto textData = readFileToString(link);
    EXPECT_EQ(textSize, EXPECTED_TEXT_CONTENT.size());
    EXPECT_EQ(textData, EXPECTED_TEXT_CONTENT);
}
#endif

TEST(File_utils_should, throw_when_file_not_found) {
    EXPECT_THROW(readFileToString("./missing.txt"), RuntimeError);
    EXPECT_THROW(readFileToVector("./missing.dat"), RuntimeError);
    EXPECT_THROW(readFileToCharVector("./missing.dat"), RuntimeError);
}

TEST(File_utils_should, throw_when_reading_directory) {
    EXPECT_THROW(readFileToString("./tests/data"), RuntimeError);
    EXPECT_THROW(readFileToVector("./tests/data"), RuntimeError);
    EXPECT_THROW(readFileToCharVector("./tests/data"), RuntimeError);
}

TEST(File_utils_should, write_and_read_binary_file) {
    TmpFile tmpDat{"./tmp.txt"};
    writeFile(tmpDat, EXPECTED_BINARY_CONTENT);
    EXPECT_EQ(readFileToString(tmpDat), EXPECTED_TEXT_CONTENT);
    EXPECT_EQ(readFileToVector(tmpDat), EXPECTED_BINARY_CONTENT);
    EXPECT_EQ(readFileToCharVector(tmpDat), EXPECTED_CHAR_VECTOR_CONTENT);
}

TEST(File_utils_should, write_and_read_empty_file) {
    TmpFile emptyFile{"./tmp_empty.dat"};
    writeFile(emptyFile, std::string_view{});
    EXPECT_EQ(readFileToString(emptyFile).size(), 0u);
    EXPECT_EQ(readFileToVector(emptyFile).size(), 0u);
    EXPECT_EQ(readFileToCharVector(emptyFile).size(), 0u);
}

TEST(File_utils_should, throw_when_try_overwrite_directory) {
    EXPECT_THROW(writeFile("./tests/data", EXPECTED_TEXT_CONTENT), RuntimeError);
    EXPECT_THROW(writeFile("./tests/data", EXPECTED_BINARY_CONTENT), RuntimeError);
}

TEST(File_utils_should, throw_when_write_to_not_existing_directory) {
    fs::remove_all("./missing");
    EXPECT_THROW(writeFile("./missing/tmp.txt", EXPECTED_TEXT_CONTENT), RuntimeError);
    EXPECT_THROW(writeFile("./missing/tmp.dat", EXPECTED_BINARY_CONTENT), RuntimeError);
}

TEST(File_utils_should, create_parent_directories) {
    fs::remove_all("./missing_parent_1");
    fs::remove_all("./missing_parent_2");
    fs::remove_all("./missing_parent_3");
    fs::remove_all("./missing_parent_4");
    fs::remove_all("../missing_parent_5");
    EXPECT_NO_THROW(createParentDirs("./missing_parent_1/tmp.txt"));
    EXPECT_NO_THROW(createParentDirs("./missing_parent_1/tmp2.txt"));
    EXPECT_NO_THROW(createParentDirs("./missing_parent_2/tmp.txt"));
    EXPECT_NO_THROW(createParentDirs("./missing_parent_3/missing/tmp.txt"));
    EXPECT_NO_THROW(createParentDirs("missing_parent_4/tmp.txt"));
    EXPECT_NO_THROW(createParentDirs("../missing_parent_5/tmp.txt"));
    EXPECT_NO_THROW(createParentDirs("./tmp.txt"));
    EXPECT_NO_THROW(createParentDirs("../tmp.txt"));
    EXPECT_NO_THROW(createParentDirs("tmp.txt"));
    EXPECT_NO_THROW(createParentDirs(""));
    EXPECT_TRUE(fs::is_directory("./missing_parent_1"));
    EXPECT_TRUE(fs::is_directory("./missing_parent_2"));
    EXPECT_TRUE(fs::is_directory("./missing_parent_3/missing"));
    EXPECT_TRUE(fs::is_directory("./missing_parent_4"));
    EXPECT_TRUE(fs::is_directory("../missing_parent_5"));
}

TEST(File_utils_should, not_create_directories_when_parent_is_file) {
    writeFile("tmp.txt", "");
    EXPECT_THROW(createParentDirs("tmp.txt/tmp.txt"), RuntimeError);
    EXPECT_FALSE(fs::is_directory("./tmp.txt"));
}

TEST(File_utils_should, remove_file) {
    const std::string tmpEmptyDat = "./tmp_empty.dat";
    writeFile(tmpEmptyDat, std::vector<uint8_t>{});
    EXPECT_TRUE(removeFile(tmpEmptyDat));
    EXPECT_FALSE(removeFile(tmpEmptyDat));
}

TEST(File_utils_should, join_paths) {
    auto toPreferred = [](std::string path) {
#ifdef _WIN32
        std::replace(path.begin(), path.end(), '/', '\\');
#endif
        return path;
    };

    EXPECT_EQ(joinPath("foo", "bar"), toPreferred("foo/bar"));
    EXPECT_EQ(joinPath("foo", "bar", "baz"), toPreferred("foo/bar/baz"));
    EXPECT_EQ(joinPath("foo", "", "baz"), toPreferred("foo/baz"));
    EXPECT_EQ(joinPath("foo/", "baz"), "foo/baz");
#ifndef _WIN32
    EXPECT_EQ(joinPath("foo", "/baz"), "/baz");
#endif

    std::string s1 = "foo", s2 = "bar";
    EXPECT_EQ(joinPath(s1, s2), toPreferred("foo/bar"));

    char c1[] = "foo", c2[] = "bar";
    EXPECT_EQ(joinPath(c1, c2), toPreferred("foo/bar"));

    std::string_view v1 = s1, v2 = s2;
    EXPECT_EQ(joinPath(v1, v2), toPreferred("foo/bar"));

    const char* p1 = s1.c_str(), *p2 = s2.c_str();
    EXPECT_EQ(joinPath(p1, p2), toPreferred("foo/bar"));

    TString t1 = "foo", t2 = "bar";
    EXPECT_EQ(joinPath(t1, t2), toPreferred("foo/bar"));
}

TEST(File_utils_should, file_transaction) {
    fs::remove_all(TRANSACTION_FILE_DIR);
    fs::create_directory(TRANSACTION_FILE_DIR);

    fs::path filename = fs::path(TRANSACTION_FILE_DIR) / TRANSACTION_FILE_NAME;

    FileTransaction transaction(filename);
    transaction.stream() << TRANSACTION_FILE_TEXT;
    transaction.commit();

    auto directoryIterator = fs::directory_iterator(TRANSACTION_FILE_DIR);
    EXPECT_EQ(std::distance(begin(directoryIterator), end(directoryIterator)), 1);

    std::ifstream in(filename.string());
    std::string textInFile;
    std::getline(in, textInFile);
    EXPECT_EQ(TRANSACTION_FILE_TEXT, textInFile);
}


TEST(File_utils_should, file_transaction_rollback) {
    fs::remove_all(TRANSACTION_FILE_DIR);
    fs::create_directory(TRANSACTION_FILE_DIR);

    {
        fs::path filename = fs::path(TRANSACTION_FILE_DIR) / TRANSACTION_FILE_NAME;
        FileTransaction transaction(filename);
        transaction.stream() << TRANSACTION_FILE_TEXT;
    }

    auto directoryIterator = fs::directory_iterator(TRANSACTION_FILE_DIR);
    EXPECT_EQ(std::distance(begin(directoryIterator), end(directoryIterator)), 0);
}


TEST(File_utils_should, not_mutate_file_before_commit) {
    fs::remove_all(TRANSACTION_FILE_DIR);
    fs::create_directory(TRANSACTION_FILE_DIR);

    fs::path filename = fs::path(TRANSACTION_FILE_DIR) / TRANSACTION_FILE_NAME;

    writeFile(filename.string(), TRANSACTION_FILE_OTHER_TEXT);

    FileTransaction transaction(filename);
    transaction.stream() << TRANSACTION_FILE_TEXT;

    EXPECT_EQ(TRANSACTION_FILE_OTHER_TEXT, readFileToString(filename.string()));

    transaction.commit();

    auto directoryIterator = fs::directory_iterator(TRANSACTION_FILE_DIR);
    EXPECT_EQ(std::distance(begin(directoryIterator), end(directoryIterator)), 1);
    EXPECT_EQ(TRANSACTION_FILE_TEXT, readFileToString(filename.string()));
}

TEST(File_utils_should, create_temp_file_in_the_same_directory) {
    fs::remove_all(TRANSACTION_FILE_DIR);
    fs::create_directory(TRANSACTION_FILE_DIR);

    fs::path filename = fs::path(TRANSACTION_FILE_DIR) / TRANSACTION_FILE_NAME;

    FileTransaction transaction(filename.string());
    transaction.stream() << TRANSACTION_FILE_TEXT;

    auto directoryIterator = fs::directory_iterator(TRANSACTION_FILE_DIR);
    EXPECT_EQ(std::distance(begin(directoryIterator), end(directoryIterator)), 1);
    EXPECT_FALSE(std::ifstream(filename.string()));
}

TEST(File_utils_should, create_file_with_correct_default_mode) {
    fs::remove_all(TRANSACTION_FILE_DIR);
    fs::create_directory(TRANSACTION_FILE_DIR);

    fs::path filename = fs::path(TRANSACTION_FILE_DIR) / TRANSACTION_FILE_NAME;

    FileTransaction transaction(filename);
    transaction.stream() << TRANSACTION_FILE_TEXT;
    transaction.commit();

    TFileStat stat = TFileStat(filename.string().c_str());
#ifndef _win_
    EXPECT_EQ(stat.Mode & 0777, static_cast<ui32>(0644));
#else
    EXPECT_EQ(stat.Mode & _S_IWRITE, _S_IWRITE);
#endif
}

TEST(File_utils_should, create_file_with_correct_custom_mode) {
    fs::remove_all(TRANSACTION_FILE_DIR);
    fs::create_directory(TRANSACTION_FILE_DIR);

    fs::path filename = fs::path(TRANSACTION_FILE_DIR) / TRANSACTION_FILE_NAME;

    FileTransaction transaction(filename, S_IRUSR | S_IXUSR | S_IRGRP | S_IXGRP | S_IROTH | S_IXOTH);
    transaction.stream() << TRANSACTION_FILE_TEXT;
    transaction.commit();

    TFileStat stat = TFileStat(filename.string().c_str());
#ifndef _win_
    EXPECT_EQ(stat.Mode & 0777, static_cast<ui32>(0555));
#else
    EXPECT_EQ(stat.Mode & _S_IWRITE, 0); // readonly
#endif

    Chmod(filename.string().c_str(), S_IRWXU | S_IRWXG | S_IRWXO);
}

TEST(File_utils_should, create_unique_dir) {
    fs::path uniqueDirPath = createUniqueDir(
        /* baseDir = */ "./tmp",
        /* prefix = */ "prefix",
        /* suffix = */ "suffix"
    );

    EXPECT_THAT(uniqueDirPath.filename().string(), StartsWith("prefix-"));
    EXPECT_THAT(uniqueDirPath.filename().string(), EndsWith("-suffix"));

    fs::remove_all(uniqueDirPath);
}

} //namespace maps::common::tests
