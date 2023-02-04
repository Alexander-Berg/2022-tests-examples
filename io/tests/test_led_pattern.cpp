#include <yandex_io/modules/leds/led_patterns/led_pattern.h>

#include <yandex_io/modules/leds/led_controller/color_readers.h>

#include <yandex_io/tests/testlib/test_utils.h>
#include <yandex_io/tests/testlib/unittest_helper/unit_test_fixture.h>

#include <library/cpp/testing/unittest/env.h>
#include <library/cpp/testing/unittest/registar.h>

#include <chrono>

using namespace quasar;
using namespace quasar::TestUtils;

namespace {

    const int LED_COUNT = 24;

    class MockLedController: public LedController {
    public:
        int getLedCount() const override {
            return LED_COUNT;
        }

        void drawFrame(const LedCircle& /* colors */) override {
        }

        rgbw_color readColor(const std::string& stringColor) override {
            return readRGBColor(stringColor);
        }

        int getWidth() const override {
            return LED_COUNT;
        }

        ~MockLedController() override = default;
    };

    Y_UNIT_TEST_SUITE(testLedPattern) {

        Y_UNIT_TEST(testLedPatternProtocol)
        {
            auto now = std::chrono::steady_clock::now();
            auto firstFrame = now + std::chrono::milliseconds(99);
            auto secondFrame = now + std::chrono::milliseconds(100);
            auto secondFrame2 = now + std::chrono::milliseconds(101);
            auto thirdFrame = now + std::chrono::milliseconds(200);

            auto ledController = std::make_shared<MockLedController>();
            auto pattern = LedPattern::loadFromFile(ArcadiaSourceRoot() + "/yandex_io/modules/leds/led_patterns/tests/ledpatterns/test.led", ledController);
            pattern->startAnimationFrom(now);
            UNIT_ASSERT(pattern->getEndOfFrameTimePoint() == secondFrame);

            pattern->updateTime(firstFrame);
            UNIT_ASSERT(pattern->getEndOfFrameTimePoint() == secondFrame);

            pattern->updateTime(secondFrame);
            UNIT_ASSERT(pattern->getEndOfFrameTimePoint() == thirdFrame);

            pattern->updateTime(secondFrame2);
            UNIT_ASSERT(pattern->getEndOfFrameTimePoint() == thirdFrame);
        }

        Y_UNIT_TEST(testLedPatternFinish)
        {
            auto now = std::chrono::steady_clock::now();
            auto next = now + std::chrono::milliseconds(99);
            auto end = now + std::chrono::seconds(100000);

            auto ledController = std::make_shared<MockLedController>();
            auto pattern = std::make_shared<LedPattern>(LED_COUNT, ledController);
            LedFrame frame;
            frame.delayMs = 10;
            pattern->addSmoothFrame(frame);
            pattern->startAnimationFrom(now);

            UNIT_ASSERT(!pattern->finished());

            pattern->updateTime(next);

            UNIT_ASSERT(pattern->finished());

            pattern->updateTime(end);

            UNIT_ASSERT(pattern->finished());
        }

        Y_UNIT_TEST(testOutOfOrder)
        {
            auto now = std::chrono::steady_clock::now();
            auto next = now + std::chrono::milliseconds(99);
            auto end = now + std::chrono::seconds(100000);

            auto ledController = std::make_shared<MockLedController>();
            auto pattern = std::make_shared<LedPattern>(LED_COUNT, ledController);
            LedFrame frame;
            frame.delayMs = 10;
            pattern->addSmoothFrame(frame);

            pattern->drawCurrentFrame();
            pattern->drawCurrentFrame();

            pattern->updateTime(end);

            UNIT_ASSERT(!pattern->finished());

            pattern->updateTime(next);

            UNIT_ASSERT(!pattern->finished());

            pattern->updateTime(end);

            UNIT_ASSERT(!pattern->finished());
        }

    }

} // namespace
