#include <library/cpp/testing/unittest/registar.h>

#include <yandex_io/libs/subprocess/subprocess.h>
#include <yandex_io/tests/testlib/unittest_helper/unit_test_fixture.h>

using namespace quasar;

Y_UNIT_TEST_SUITE_F(TestUtils, QuasarUnitTestFixture) {
    Y_UNIT_TEST(getSystemCommandOutputTail) {
        {
            auto output = getSystemCommandOutputTail("echo", {"test echo"}, 10000);
            UNIT_ASSERT_VALUES_EQUAL(output.stdOut, "test echo\n");
            UNIT_ASSERT(output.stdErr.empty());
        }
        {
            auto output = getSystemCommandOutputTail("echo", {"test echo"}, 2);
            UNIT_ASSERT_VALUES_EQUAL(output.stdOut, "o\n");
            UNIT_ASSERT(output.stdErr.empty());
        }
        {
            auto output = getSystemCommandOutputTail("absent_system_command", {}, 1234);
            UNIT_ASSERT_VALUES_EQUAL(output.stdOut, std::string());
            UNIT_ASSERT(!output.stdErr.empty()); // system specific output
        }
        {
            auto output = getSystemCommandOutputTail("head", {"-c", "3000", "/dev/zero"}, 1234);
            UNIT_ASSERT_VALUES_EQUAL(output.stdErr, "");
            UNIT_ASSERT_VALUES_EQUAL(output.stdOut.size(), 1234);
        }
        {
            auto output = getSystemCommandOutputTail("head", {"-c", "3000", "/dev/zero", "; curl", "http://www.yandex.ru/"}, 1234);
            UNIT_ASSERT_VALUES_EQUAL(output.stdOut.size(), 1234);
            UNIT_ASSERT(!output.stdErr.empty()); // error: got extra arguments
        }
    }

    Y_UNIT_TEST(testRunSystemCommand) {
        UNIT_ASSERT_VALUES_EQUAL(runSystemCommand("true"), 0);
        UNIT_ASSERT_VALUES_EQUAL(runSystemCommand("false"), 1);

        UNIT_ASSERT_VALUES_EQUAL(runSystemCommand("/bin/sh", {"-c", "echo foo > bar"}), 0);
        UNIT_ASSERT_VALUES_EQUAL(getSystemCommandOutputTail("cat", {"bar"}, 10).stdOut, "foo\n");

        UNIT_ASSERT_VALUES_EQUAL(runSystemCommand("bad-command"), 255);
    }
}
