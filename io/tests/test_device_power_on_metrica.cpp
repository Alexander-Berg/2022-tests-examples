#include <yandex_io/modules/metrics/device_power_on/device_power_on_metrica.h>

#include <yandex_io/libs/json_utils/json_utils.h>
#include <yandex_io/libs/telemetry/null/null_metrica.h>
#include <yandex_io/tests/testlib/unittest_helper/unit_test_fixture.h>

#include <library/cpp/testing/gmock_in_unittest/gmock.h>
#include <library/cpp/testing/unittest/registar.h>

#include <fstream>

using namespace YandexIO;
using namespace quasar;
using testing::_;

namespace {

    class TelemetryMock: public NullMetrica {
    public:
        MOCK_METHOD(void, reportEvent, (const std::string&, const std::string&, ITelemetry::Flags), (override));
    };

    class Fixture: public QuasarUnitTestFixture {
    public:
        void SetUp(NUnitTest::TTestContext& context) override {
            QuasarUnitTestFixture::SetUp(context);
        }
        void TearDown(NUnitTest::TTestContext& context) override {
            removeMonotonic();
            removeGuard();

            QuasarUnitTestFixture::TearDown(context);
        }

        static void writeToMonotonicFile(const std::string& data) {
            std::ofstream f(monotonicFile_, std::ios_base::out | std::ios_base::trunc);
            if (!data.empty()) {
                f << data;
            }
        }

        static void removeMonotonic() {
            std::remove(monotonicFile_);
        }

        static void removeGuard() {
            std::remove(guardFile_);
        }

        static void createGuard() {
            std::ofstream f(guardFile_);
        }

        static YandexIO::SDKState makeSynchronizedState(bool synchronized = true) {
            YandexIO::SDKState state;
            state.ntpdState.clockSynchronized = synchronized;
            return state;
        }

        static constexpr const char* guardFile_ = "guard";
        static constexpr const char* monotonicFile_ = "monotonic";
    };

    MATCHER_P(VerifyMetricJson, powerOffTs, "description") {
        *result_listener << "Input json: " << arg << '\n';

        const auto json = parseJson(arg);
        if (!json.isMember("powerOffTimestampSec")) {
            return false;
        }
        const auto lastMonotonicTs = json["powerOffTimestampSec"].asInt();
        if (lastMonotonicTs != powerOffTs) {
            return false;
        }
        if (!json.isMember("nowTimestampSec")) {
            return false;
        }
        const int32_t metricaNowTs = json["nowTimestampSec"].asInt();
        const auto now = std::chrono::system_clock::now();
        const int32_t nowTs = std::chrono::duration_cast<std::chrono::seconds>(now.time_since_epoch()).count();

        const int32_t nowDiff = nowTs - metricaNowTs;
        // metrica value should be close to current
        if (nowDiff > 5) {
            return false;
        }

        return true;
    }

} // namespace

Y_UNIT_TEST_SUITE_F(TestDevicePowerOnMetrica, Fixture) {
    Y_UNIT_TEST(testDevicePowerOnMetrica) {
        const auto telemetry = std::make_shared<TelemetryMock>();
        constexpr int32_t powerOffTs = 1632914175;

        writeToMonotonicFile(std::to_string(powerOffTs));

        PowerOnMetricaSender sender(telemetry, guardFile_, monotonicFile_);

        EXPECT_CALL(*telemetry, reportEvent("device_power_on", VerifyMetricJson(powerOffTs), _));

        sender.onSDKState(makeSynchronizedState());
    }

    Y_UNIT_TEST(testMetricaWithoutMonotonicFile) {
        const auto telemetry = std::make_shared<TelemetryMock>();
        // do not create file with monotonic file. Value of lastMonotonicTs should be -1
        PowerOnMetricaSender sender(telemetry, guardFile_, monotonicFile_);

        EXPECT_CALL(*telemetry, reportEvent("device_power_on", VerifyMetricJson(-1), _));

        sender.onSDKState(makeSynchronizedState());
    }

    Y_UNIT_TEST(testEnsureCalledOnce) {
        const auto telemetry = std::make_shared<TelemetryMock>();

        PowerOnMetricaSender sender(telemetry, guardFile_, monotonicFile_);

        EXPECT_CALL(*telemetry, reportEvent("device_power_on", _, _)).Times(1);

        sender.onSDKState(makeSynchronizedState());
        sender.onSDKState(makeSynchronizedState());
        sender.onSDKState(makeSynchronizedState());
        sender.onSDKState(makeSynchronizedState());
    }

    Y_UNIT_TEST(testEnsureNotCalledIfGuardExists) {
        const auto telemetry = std::make_shared<TelemetryMock>();
        createGuard();

        PowerOnMetricaSender sender(telemetry, guardFile_, monotonicFile_);

        EXPECT_CALL(*telemetry, reportEvent(_, _, _)).Times(0);

        sender.onSDKState(makeSynchronizedState());
    }

    Y_UNIT_TEST(testEnsureNotCalledIfStateNotSynchronized) {
        const auto telemetry = std::make_shared<TelemetryMock>();
        PowerOnMetricaSender sender(telemetry, guardFile_, monotonicFile_);

        EXPECT_CALL(*telemetry, reportEvent(_, _, _)).Times(0);
        sender.onSDKState(makeSynchronizedState(false));
    }

    Y_UNIT_TEST(testInvalidMonotonicFileContent) {
        const auto telemetry = std::make_shared<TelemetryMock>();
        writeToMonotonicFile("not a number");

        PowerOnMetricaSender sender(telemetry, guardFile_, monotonicFile_);

        EXPECT_CALL(*telemetry, reportEvent("device_power_on", VerifyMetricJson(-1), _));
        sender.onSDKState(makeSynchronizedState());
    }

    Y_UNIT_TEST(testEmptyMonotonicFileContent) {
        const auto telemetry = std::make_shared<TelemetryMock>();
        writeToMonotonicFile("");

        PowerOnMetricaSender sender(telemetry, guardFile_, monotonicFile_);

        EXPECT_CALL(*telemetry, reportEvent("device_power_on", VerifyMetricJson(-1), _));
        sender.onSDKState(makeSynchronizedState());
    }

} /* TestFeedbackShiftLogger */
