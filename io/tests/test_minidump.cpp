#include <yandex_io/libs/minidump/quasar_minidump.h>

#include <yandex_io/libs/json_utils/json_utils.h>

#include <yandex_io/tests/testlib/unittest_helper/unit_test_fixture.h>

#include <library/cpp/testing/unittest/registar.h>

#include <pthread.h>
#include <unistd.h>
#include <sys/stat.h>
#include <sys/wait.h>

using namespace quasar;

#define HANDLE_EINTR(x) ({                                  \
    __typeof__(x) eintr_wrapper_result;                     \
    do {                                                    \
        eintr_wrapper_result = (x);                         \
    } while (eintr_wrapper_result == -1 && errno == EINTR); \
    eintr_wrapper_result;                                   \
})

void WaitForProcessToTerminate(pid_t process_id, int expected_status) {
    int status = 0;

    UNIT_ASSERT(HANDLE_EINTR(waitpid(process_id, &status, 0)) != -1);
    UNIT_ASSERT(WIFSIGNALED(status) != 0);
    UNIT_ASSERT(expected_status == WTERMSIG(status));
}

Y_UNIT_TEST_SUITE_F(TestMinidump, QuasarUnitTestFixture) {
    Y_UNIT_TEST(testMinidumpInstructionPointerMemoryNullPointer) {
        const auto device = getDeviceForTests();
        const pid_t child = fork();

        if (child == 0) {
            Json::Value minidumpConfig;
            Json::Value testServiceConfig;

            minidumpConfig["enabled"] = true;
            minidumpConfig["fileName"] = "out_of_short_string_optimization_name_test_service.dmp";
            minidumpConfig["sizeLimitKB"] = 200;

            testServiceConfig["Minidump"] = minidumpConfig;

            QuasarMinidump::getInstance().init("out_of_short_string_optimization_name_test_service", testServiceConfig, device->telemetry(), "test_version");

            using void_function = void (*)();
            volatile void_function memory_function =
                reinterpret_cast<void_function>(NULL);

            memory_function();

            exit(1);
        }

        WaitForProcessToTerminate(child, SIGSEGV);

        struct stat st;
        UNIT_ASSERT(stat("out_of_short_string_optimization_name_test_service.dmp", &st) == 0);
        UNIT_ASSERT(st.st_size > 0);
        UNIT_ASSERT(stat("out_of_short_string_optimization_name_test_service.dmp.json", &st) == 0);
        UNIT_ASSERT(st.st_size > 0);

        Json::Value metadata = readJsonFromFile("out_of_short_string_optimization_name_test_service.dmp.json");
        UNIT_ASSERT_VALUES_EQUAL("test_version", tryGetString(metadata, "softwareVersion"));

        unlink("out_of_short_string_optimization_name_test_service.dmp");
        unlink("out_of_short_string_optimization_name_test_service.dmp.json");
    }
}
