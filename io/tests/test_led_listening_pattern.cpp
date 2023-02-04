#include <yandex_io/modules/leds/led_patterns/led_pattern.h>
#include <yandex_io/modules/leds/led_patterns/listening_pattern.h>

#include <yandex_io/modules/leds/led_controller/null_led_controller.h>

#include <yandex_io/tests/testlib/test_utils.h>
#include <yandex_io/tests/testlib/unittest_helper/unit_test_fixture.h>

#include <library/cpp/testing/unittest/env.h>
#include <library/cpp/testing/unittest/registar.h>

#include <chrono>

using namespace quasar;
using namespace quasar::TestUtils;

using namespace std::chrono;

namespace {
    const int LED_COUNT = 24;

    class MockLedController: public LedController {
    public:
        int getLedCount() const override {
            return LED_COUNT;
        }

        void drawFrame(const LedCircle& /* colors */) override {
        }

        rgbw_color readColor(const std::string& /*stringColor*/) override {
            return {};
        }

        int getWidth() const override {
            return LED_COUNT;
        }

        ~MockLedController() override = default;
    };

} // namespace

Y_UNIT_TEST_SUITE(testLedListeningPattern) {

    Y_UNIT_TEST(testLedListeningPatternProtocol)
    {
        // ListeningPattern is tricky, we simply check that using it the usual way produces no exceptions, etc
        auto now = std::chrono::steady_clock::now();
        auto firstFrame = now + std::chrono::milliseconds(99);
        auto secondFrame = now + std::chrono::milliseconds(100);
        auto secondFrame2 = now + std::chrono::milliseconds(101);
        auto thirdFrame = now + std::chrono::milliseconds(200);

        auto ledController = std::make_shared<MockLedController>();
        auto pattern = std::make_shared<ListeningPattern>(LedPattern::loadFromFile(ArcadiaSourceRoot() + "/yandex_io/modules/leds/led_patterns/tests/ledpatterns/no_loop.led", ledController), 0);
        pattern->startAnimationFrom(now);

        pattern->updateTime(firstFrame);
        pattern->updateTime(secondFrame);
        pattern->updateTime(secondFrame2);
        pattern->updateTime(thirdFrame);

        pattern->drawCurrentFrame();

    }

}
