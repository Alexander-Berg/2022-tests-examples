#include <yandex_io/libs/base/persistent_file.h>

#include <yandex_io/tests/testlib/unittest_helper/unit_test_fixture.h>

#include <library/cpp/testing/unittest/registar.h>

#include <util/folder/filelist.h>
#include <util/folder/path.h>
#include <util/generic/guid.h>

#include <fstream>
#include <memory>
#include <sstream>
#include <string>

#include <fcntl.h>

using namespace quasar;
namespace {

    struct Fixture: public QuasarUnitTestFixture {
        Fixture() {
            TEST_FILE = tryGetRamDrivePath() + "/quasar_persistent_test.dat";
            unlink(TEST_FILE.c_str());
        }

        ~Fixture() {
            unlink(TEST_FILE.c_str());
        }

        std::string TEST_FILE;
    };

} // namespace

Y_UNIT_TEST_SUITE_F(TestPersistentFile, Fixture) {
    Y_UNIT_TEST(testPersistentFileOpen)
    {
        std::unique_ptr<PersistentFile> f(new PersistentFile(TEST_FILE, PersistentFile::Mode::TRUNCATE));

        /* Try to create file in inaccessible storage */

        try {
            PersistentFile f("/sys/quasar_lalala", PersistentFile::Mode::TRUNCATE);
            UNIT_ASSERT(false);
        } catch (const std::runtime_error& e) {
            UNIT_ASSERT(true);
        }

        /* Check that is has been created with correct rights */

        UNIT_ASSERT_VALUES_EQUAL(access(TEST_FILE.c_str(), F_OK | R_OK | W_OK), 0);
        UNIT_ASSERT_LT(access(TEST_FILE.c_str(), X_OK), 0);

        /* Open the same file second time without closing - should just fail */

        UNIT_ASSERT_VALUES_EQUAL(access(TEST_FILE.c_str(), F_OK | R_OK | W_OK), 0);
        UNIT_ASSERT_LT(access(TEST_FILE.c_str(), X_OK), 0);
    }

    Y_UNIT_TEST(testPersistentFileWriteAppend)
    {
        std::unique_ptr<PersistentFile> f(new PersistentFile(TEST_FILE, PersistentFile::Mode::APPEND));
        std::ifstream checkFile;
        constexpr const char TEST_STRING[]{"Hello world"};
        constexpr const size_t TEST_STRING_LEN{sizeof(TEST_STRING) / sizeof(TEST_STRING[0]) - 1}; /* Do not count \0 in the end */

        constexpr const size_t NR_WRITES{3};

        /* Write TEST_STRING two times */

        const auto writeTwoStrings = [&f, &TEST_STRING]() {
            std::string s{TEST_STRING};
            UNIT_ASSERT_VALUES_EQUAL(f->write(s), true);
            UNIT_ASSERT_VALUES_EQUAL(f->write(s.data(), s.size()), true);
        };

        /* Write a set (NR_WRITES * 2) of strings into a file */

        for (size_t i = 0; i < NR_WRITES; ++i) {
            writeTwoStrings();
        }

        /* Check the set of strings exists in a file */

        checkFile.open(TEST_FILE);
        std::stringstream buffer;
        buffer << checkFile.rdbuf();

        std::string checkString{buffer.str()};

        size_t pos{};
        for (size_t i = 0; i < NR_WRITES * 2; ++i) {
            pos = checkString.find(TEST_STRING, pos);
            UNIT_ASSERT(pos != std::string::npos);
            pos += TEST_STRING_LEN;
        }

        pos = checkString.find(TEST_STRING, pos);
        UNIT_ASSERT(pos == std::string::npos);
    }

    Y_UNIT_TEST(testPersistentFileWriteTruncate)
    {
        std::ifstream checkFile;
        constexpr const char TEST_STRING[]{"Hello world"};
        constexpr const size_t TEST_STRING_LEN{sizeof(TEST_STRING) / sizeof(TEST_STRING[0]) - 1}; /* Do not count \0 in the end */

        constexpr const size_t NR_WRITES{3};

        const auto writeTwoStrings = [this, &TEST_STRING]() {
            std::unique_ptr<PersistentFile> f(new PersistentFile(TEST_FILE, PersistentFile::Mode::TRUNCATE));

            std::string s{TEST_STRING};
            UNIT_ASSERT_VALUES_EQUAL(f->write(s), true);
            UNIT_ASSERT_VALUES_EQUAL(f->write(s.data(), s.size()), true);
        };

        /* Write a set of strings into a file */

        for (size_t i = 0; i < NR_WRITES; ++i) {
            writeTwoStrings();
        }

        /* Check there are only two strings - as the file being closed every time
         * when writeTwoString called, when opened it should truncate its content.
         */

        checkFile.open(TEST_FILE);
        UNIT_ASSERT_VALUES_EQUAL(checkFile.good(), true);

        std::stringstream buffer;
        buffer << checkFile.rdbuf();

        std::string checkString{buffer.str()};

        size_t pos{};
        for (size_t i = 0; i < 2; ++i) {
            pos = checkString.find(TEST_STRING, pos);
            UNIT_ASSERT(pos != std::string::npos);
            pos += TEST_STRING_LEN;
        }

        pos = checkString.find(TEST_STRING, pos);
        UNIT_ASSERT(pos == std::string::npos);
    }

    Y_UNIT_TEST(testAtomicFileDoNotLeaveTmpFileOnFail)
    {
        TFsPath dir("tmp-" + CreateGuidAsString());
        dir.MkDir();
        {
            // actual file name is empty. Should fail, but do not leave any files
            AtomicFile file(dir.GetName() + "/");
        }
        TFileEntitiesList fileList(TFileEntitiesList::EM_FILES_DIRS_SLINKS);
        fileList.Fill(dir.GetName());
        UNIT_ASSERT_VALUES_EQUAL(fileList.Size(), 0);

        dir.DeleteIfExists();
    }

    Y_UNIT_TEST(testTransactionFileDoNotLeaveTmpFileOnFail)
    {
        TFsPath dir("tmp-" + CreateGuidAsString());
        dir.MkDir();
        {
            TransactionFile file(dir.GetName() + "/");
            file.commit();
        }
        TFileEntitiesList fileList(TFileEntitiesList::EM_FILES_DIRS_SLINKS);
        fileList.Fill(dir.GetName());
        UNIT_ASSERT_VALUES_EQUAL(fileList.Size(), 0);

        dir.DeleteIfExists();
    }
}
