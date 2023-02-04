#include <yandex/maps/wiki/tasks/status_writer.h>
#include <maps/libs/common/include/file_utils.h>

#include <library/cpp/testing/unittest/registar.h>

#include <fstream>

namespace maps {
namespace wiki {
namespace tasks {
namespace tests {

Y_UNIT_TEST_SUITE(status_writer) {

Y_UNIT_TEST(test_status_writer)
{
    {
        StatusWriter statusWriter(std::nullopt);
        statusWriter.flush(); // Should not be any exception.
    }

    const std::string fPath("status-writer-module-test.status");
    StatusWriter statusWriter(fPath);

    statusWriter.reset();
    statusWriter.flush();
    UNIT_ASSERT_STRINGS_EQUAL(common::readFileToString(fPath), "0;OK");

    statusWriter.reset();
    statusWriter.warn("file not found");
    statusWriter.flush();
    UNIT_ASSERT_STRINGS_EQUAL(common::readFileToString(fPath), "1;WARN: file not found. ");

    statusWriter.reset();
    statusWriter.err("authorization failed");
    statusWriter.flush();
    UNIT_ASSERT_STRINGS_EQUAL(common::readFileToString(fPath), "2;ERR: authorization failed. ");

    statusWriter.reset();
    statusWriter.err("authorization failed");
    statusWriter.warn("file not found");
    statusWriter.flush();
    UNIT_ASSERT_STRINGS_EQUAL(common::readFileToString(fPath), "2;ERR: authorization failed. WARN: file not found. ");
}

} // Y_UNIT_TEST_SUITE

} // namespace tests
} // namespace tasks
} // namespace wiki
} // namespace maps
