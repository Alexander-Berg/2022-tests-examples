#include <yandex_io/modules/metrics/auxiliary/aux_metrics_sender.h>

#include <yandex_io/libs/json_utils/json_utils.h>
#include <yandex_io/libs/telemetry/null/null_metrica.h>
#include <yandex_io/tests/testlib/unittest_helper/unit_test_fixture.h>

#include <library/cpp/testing/gmock_in_unittest/gmock.h>
#include <library/cpp/testing/unittest/registar.h>

#include <future>

using namespace YandexIO;
using namespace quasar;
using testing::_;

namespace {

    constexpr auto HOUR = std::chrono::hours(1);

    class TelemetryMock: public NullMetrica {
    public:
        MOCK_METHOD(void, reportEvent, (const std::string&, ITelemetry::Flags), (override));
    };

} // namespace

Y_UNIT_TEST_SUITE(TestAuxMetricsSender) {

    Y_UNIT_TEST_F(testSendEnabledDisabled, QuasarUnitTestFixture) {
        auto telemetry = std::make_shared<TelemetryMock>();
        AuxMetricsSender sender(telemetry, HOUR);

        {
            testing::InSequence seq;
            EXPECT_CALL(*telemetry, reportEvent("auxEnabled", _));
            EXPECT_CALL(*telemetry, reportEvent("auxDisabled", _));
        }
        sender.auxEnabled();
        sender.auxDisabled();
    }

    Y_UNIT_TEST_F(testHeartbit, QuasarUnitTestFixture) {
        auto telemetry = std::make_shared<TelemetryMock>();
        AuxMetricsSender sender(telemetry, std::chrono::seconds(1));

        std::promise<void> ping1;
        std::promise<void> ping2;

        {
            testing::InSequence seq;
            EXPECT_CALL(*telemetry, reportEvent("auxEnabled", _));

            EXPECT_CALL(*telemetry, reportEvent("progressHeartbeatAux", _))
                .Times(testing::AtLeast(3))
                .WillOnce(testing::Return()) // do nothing on first send
                .WillOnce(testing::Invoke([&]() {
                    ping1.set_value(); // notify first ping
                }))
                .WillOnce(testing::Invoke([&]() {
                    ping2.set_value(); // notify second ping
                }));

            EXPECT_CALL(*telemetry, reportEvent("auxDisabled", _));
        }
        sender.auxEnabled();

        ping1.get_future().get();
        ping2.get_future().get();

        sender.auxDisabled();
    }

}
