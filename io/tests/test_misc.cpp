#include <yandex_io/libs/base/crc32.h>
#include <yandex_io/libs/base/utils.h>

#include <yandex_io/libs/json_utils/json_utils.h>

#include <yandex_io/tests/testlib/unittest_helper/unit_test_fixture.h>

#include <library/cpp/testing/unittest/registar.h>

#include <util/folder/path.h>

#include <fstream>

using namespace quasar;

Y_UNIT_TEST_SUITE_F(TestMisc, QuasarUnitTestFixture) {
    Y_UNIT_TEST(testCRC32)
    {
        UNIT_ASSERT_VALUES_EQUAL(getCrc32("Hello"), 0xf7d18982);
    }

    Y_UNIT_TEST(testCRC32Extend) {
        Crc32 crc;
        crc.extend("Hello");
        UNIT_ASSERT_VALUES_EQUAL(crc.checksum(), 4157704578);
        crc.extend("world");
        UNIT_ASSERT_VALUES_EQUAL(crc.checksum(), 3065389949);
    }

    Y_UNIT_TEST(testGetFileContent)
    {
        const std::string fileName = JoinFsPaths(tryGetRamDrivePath(), "quasar_content");
        std::fstream file(fileName, std::fstream::out);

        file << "abracadabra";

        if (!file.good())
        {
            throw std::runtime_error(std::string("Couldn't create file for testGetFileContent: ") + strerror(errno));
        }

        file.close();

        UNIT_ASSERT_VALUES_EQUAL(getFileContent(fileName), "abracadabra");
        UNIT_ASSERT_VALUES_EQUAL(getFileTail(fileName, 5), "dabra");
        UNIT_ASSERT_VALUES_EQUAL(getFileTail(fileName, 11), "abracadabra");
        UNIT_ASSERT_VALUES_EQUAL(getFileTail(fileName, 12), "abracadabra");

        ::unlink(fileName.c_str());
    }
}
