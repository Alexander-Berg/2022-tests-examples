#include <yandex_io/modules/leds/led_patterns/led_pattern.h>

#include <yandex_io/modules/leds/led_controller/color_readers.h>
#include <yandex_io/modules/leds/led_controller/led_controller.h>

#include <yandex_io/libs/logging/logging.h>
#include <yandex_io/tests/testlib/test_utils.h>
#include <yandex_io/tests/testlib/unittest_helper/unit_test_fixture.h>

#include <library/cpp/testing/unittest/env.h>
#include <library/cpp/testing/unittest/registar.h>

#include <chrono>
#include <future>

using namespace quasar;
using namespace quasar::TestUtils;

using namespace std::chrono;

namespace {
    const int LED_COUNT = 24;
} // namespace

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

class MockZeroLedController: public LedController {
public:
    int getLedCount() const override {
        return 0;
    }

    void drawFrame(const LedCircle& /* colors */) override {
    }

    rgbw_color readColor(const std::string& stringColor) override {
        return readRGBColor(stringColor);
    }

    ~MockZeroLedController() override = default;
};

Y_UNIT_TEST_SUITE_F(TestLedAnimations, QuasarUnitTestFixture) {
    Y_UNIT_TEST(testLedPatternLoad)
    {
        auto ledController = std::make_shared<MockLedController>();
        auto pattern = LedPattern::loadFromFile(ArcadiaSourceRoot() + "/yandex_io/modules/leds/led_patterns/tests/ledpatterns/test.led", ledController);

        UNIT_ASSERT_VALUES_EQUAL(pattern->getName(), "test-led-animation");

        UNIT_ASSERT_VALUES_EQUAL(pattern->frames.size(), 64U);
        UNIT_ASSERT_VALUES_EQUAL(pattern->loopFromIndex, 5); // Shifted with one frame for smooth transition in the loop

        for (size_t i = 0; i < 8; ++i)
        {
            const auto& frame = pattern->frames[i];
            for (size_t j = 0; j < frame.circle.size(); ++j)
            {
                if (j >= i * 3 && j <= i * 3 + 2) {
                    UNIT_ASSERT_VALUES_EQUAL(frame.circle[j].r, 0xff);
                } else {
                    UNIT_ASSERT_VALUES_EQUAL(frame.circle[j].r, 0);
                }
                UNIT_ASSERT_VALUES_EQUAL(frame.circle[j].g, 0);
                UNIT_ASSERT_VALUES_EQUAL(frame.circle[j].b, 0);
            }
        }

#define CHECK_RGB(color, R, G, B)               \
    {                                           \
        UNIT_ASSERT_VALUES_EQUAL((color).r, R); \
        UNIT_ASSERT_VALUES_EQUAL((color).g, G); \
        UNIT_ASSERT_VALUES_EQUAL((color).b, B); \
    }

        const int gradientFrame = 8;

        // 8th frame is a gradient
        auto& frame = pattern->frames[gradientFrame];
        UNIT_ASSERT_VALUES_EQUAL(frame.delayMs, 1000U);
        CHECK_RGB(frame.circle[0], 255, 0, 0);
        CHECK_RGB(frame.circle[6], 128, 0, 128);
        CHECK_RGB(frame.circle[12], 0, 0, 255);
        CHECK_RGB(frame.circle[18], 128, 0, 128);

        const int rorateStartFrame = gradientFrame + 1;
        // 9 - 32 frames are rotate of ff0000 ff0000 ff0000 000000 ...

        frame = pattern->frames[rorateStartFrame];
        UNIT_ASSERT_VALUES_EQUAL(frame.delayMs, 100U);

        CHECK_RGB(frame.circle[0], 255, 0, 0);
        CHECK_RGB(frame.circle[1], 255, 0, 0);
        CHECK_RGB(frame.circle[2], 255, 0, 0);
        CHECK_RGB(frame.circle[3], 0, 0, 0);

        frame = pattern->frames[rorateStartFrame + 1];
        UNIT_ASSERT_VALUES_EQUAL(frame.delayMs, 100U);

        CHECK_RGB(frame.circle[0], 0, 0, 0);
        CHECK_RGB(frame.circle[1], 0xff, 0, 0);
        CHECK_RGB(frame.circle[2], 0xff, 0, 0);
        CHECK_RGB(frame.circle[3], 0xff, 0, 0);
        CHECK_RGB(frame.circle[4], 0, 0, 0);

        frame = pattern->frames[rorateStartFrame + LED_COUNT - 1];
        CHECK_RGB(frame.circle[0], 255, 0, 0);
        CHECK_RGB(frame.circle[1], 255, 0, 0);
        CHECK_RGB(frame.circle[2], 0, 0, 0);
        CHECK_RGB(frame.circle[23], 255, 0, 0);

        const int rotateGradientStartFrame = rorateStartFrame + LED_COUNT;
        // rotate gradient 0 ff0000 12 0000ff 1000
        frame = pattern->frames[rotateGradientStartFrame];
        CHECK_RGB(frame.circle[0], 255, 0, 0);
        CHECK_RGB(frame.circle[6], 128, 0, 128);
        CHECK_RGB(frame.circle[12], 0, 0, 255);
        CHECK_RGB(frame.circle[18], 128, 0, 128);

        frame = pattern->frames[rotateGradientStartFrame + 1];
        CHECK_RGB(frame.circle[1], 255, 0, 0);
        CHECK_RGB(frame.circle[7], 128, 0, 128);
        CHECK_RGB(frame.circle[13], 0, 0, 255);
        CHECK_RGB(frame.circle[19], 128, 0, 128);

        const int smoothGradientFrame = rotateGradientStartFrame + LED_COUNT + 1;
        // gradient 0 ff0000 12 0000ff 500 (smooth 2)
        frame = pattern->frames[smoothGradientFrame];
        CHECK_RGB(frame.circle[0], 255 / 3, 0, 0);
        CHECK_RGB(frame.circle[6], 128 / 3, 0, 128 / 3);
        CHECK_RGB(frame.circle[12], 0, 0, 255 / 3);
        CHECK_RGB(frame.circle[18], 128 / 3, 0, 128 / 3);

        frame = pattern->frames[smoothGradientFrame + 1];
        CHECK_RGB(frame.circle[0], uint8_t(255 / 1.5), 0, 0);
        CHECK_RGB(frame.circle[6], uint8_t(128 / 1.5), 0, uint8_t(128 / 1.5));
        CHECK_RGB(frame.circle[12], 0, 0, uint8_t(255 / 1.5));
        CHECK_RGB(frame.circle[18], uint8_t(128 / 1.5), 0, uint8_t(128 / 1.5));

        frame = pattern->frames[smoothGradientFrame + 2];
        CHECK_RGB(frame.circle[0], 255, 0, 0);
        CHECK_RGB(frame.circle[6], 128, 0, 128);
        CHECK_RGB(frame.circle[12], 0, 0, 255);
        CHECK_RGB(frame.circle[18], 128, 0, 128);

        // smooth transition to first frame in loop
        frame = pattern->frames[smoothGradientFrame + 3];
        CHECK_RGB(frame.circle[0], uint8_t(255 / 1.5), 0, 0);
        CHECK_RGB(frame.circle[6], uint8_t(128 / 1.5), 0, uint8_t(128 / 1.5));
        CHECK_RGB(frame.circle[12], uint8_t(255 / 3), 0, uint8_t(255 / 1.5));
        CHECK_RGB(frame.circle[18], uint8_t(128 / 1.5), 0, uint8_t(128 / 1.5));

#undef CHECK_RGB
    }

    Y_UNIT_TEST(testLedPatternLoadSmoothCycleStart)
    {
        auto ledController = std::make_shared<MockLedController>();
        auto pattern = LedPattern::loadFromFile(ArcadiaSourceRoot() + "/yandex_io/modules/leds/led_patterns/tests/ledpatterns/smooth_cycle_start.led", ledController);
        UNIT_ASSERT_VALUES_EQUAL(pattern->frames.size(), 3U);
        UNIT_ASSERT_VALUES_EQUAL(pattern->loopFromIndex, 2);
        UNIT_ASSERT_VALUES_EQUAL(pattern->frames[0].circle[0].r, 0);
        UNIT_ASSERT_VALUES_EQUAL(pattern->frames[1].circle[0].r, 127);
        UNIT_ASSERT_VALUES_EQUAL(pattern->frames[2].circle[0].r, 0xff);
    }

    Y_UNIT_TEST(testLedPatternLoadLoopBug)
    {
        auto ledController = std::make_shared<MockLedController>();
        auto pattern = LedPattern::loadFromFile(ArcadiaSourceRoot() + "/yandex_io/modules/leds/led_patterns/tests/ledpatterns/loop_bug.led", ledController);
        UNIT_ASSERT_VALUES_EQUAL(pattern->frames.size(), 7U);
        UNIT_ASSERT_VALUES_EQUAL(pattern->loopFromIndex, 3);

        UNIT_ASSERT_VALUES_EQUAL(pattern->frames[0].circle[0].r, 0);
        UNIT_ASSERT_VALUES_EQUAL(pattern->frames[0].delayMs, 1000U);

        UNIT_ASSERT_VALUES_EQUAL(pattern->frames[1].circle[0].r, 127);
        UNIT_ASSERT_VALUES_EQUAL(pattern->frames[1].delayMs, 500U);

        UNIT_ASSERT_VALUES_EQUAL(pattern->frames[2].circle[0].r, 255);
        UNIT_ASSERT_VALUES_EQUAL(pattern->frames[2].delayMs, 500U);

        UNIT_ASSERT_VALUES_EQUAL(pattern->frames[3].circle[0].r, 0x7f);
        UNIT_ASSERT_VALUES_EQUAL(pattern->frames[3].delayMs, 500U);

        UNIT_ASSERT_VALUES_EQUAL(pattern->frames[4].circle[0].r, 0);
        UNIT_ASSERT_VALUES_EQUAL(pattern->frames[4].delayMs, 500U);

        UNIT_ASSERT_VALUES_EQUAL(pattern->frames[5].circle[0].r, 0x7f);
        UNIT_ASSERT_VALUES_EQUAL(pattern->frames[5].delayMs, 500U);

        UNIT_ASSERT_VALUES_EQUAL(pattern->frames[6].circle[0].r, 0xff);
        UNIT_ASSERT_VALUES_EQUAL(pattern->frames[6].delayMs, 500U);
    }

    Y_UNIT_TEST(testLedPatternOneFrameSmoothLoop)
    {
        auto ledController = std::make_shared<MockLedController>();
        auto pattern = LedPattern::loadFromFile(ArcadiaSourceRoot() + "/yandex_io/modules/leds/led_patterns/tests/ledpatterns/one_frame_smooth_loop.led", ledController);
        UNIT_ASSERT_VALUES_EQUAL(pattern->frames.size(), 3U);
        UNIT_ASSERT_VALUES_EQUAL(pattern->frames[0].circle[0].r, 0xFF);
        UNIT_ASSERT_VALUES_EQUAL(pattern->frames[0].circle[0].g, 0x00);

        UNIT_ASSERT_VALUES_EQUAL(pattern->frames[1].circle[0].r, 0x7F);
        UNIT_ASSERT_VALUES_EQUAL(pattern->frames[1].circle[0].g, 0x7F);

        UNIT_ASSERT_VALUES_EQUAL(pattern->frames[2].circle[0].r, 0x00);
        UNIT_ASSERT_VALUES_EQUAL(pattern->frames[2].circle[0].g, 0xFF);

        UNIT_ASSERT_VALUES_EQUAL(pattern->loopFromIndex, 2);
    }

    Y_UNIT_TEST(testLedPatternLoadEmpty)
    {
        auto ledController = std::make_shared<MockZeroLedController>();

        auto pattern = LedPattern::loadFromFile(ArcadiaSourceRoot() + "/yandex_io/modules/leds/led_patterns/tests/ledpatterns/empty.led", ledController);
        UNIT_ASSERT_VALUES_EQUAL(pattern->frames.size(), 1U);
        UNIT_ASSERT_VALUES_EQUAL(pattern->loopFromIndex, 0);
        UNIT_ASSERT_VALUES_EQUAL(pattern->frames[0].circle.size(), 0U);
    }

    using ms = std::chrono::milliseconds;
    Y_UNIT_TEST(testLength) {
        auto ledController = std::make_shared<MockLedController>();
        auto foregroundPattern = std::make_shared<LedPattern>(LED_COUNT, "foregroundPattern", ledController);
        LedFrame frame;
        frame.delayMs = 1;
        foregroundPattern->frames.push_back(frame);
        foregroundPattern->frames.push_back(frame);
        foregroundPattern->frames.push_back(frame);

        UNIT_ASSERT(foregroundPattern->getLength() == ms(3));
    }

    Y_UNIT_TEST(testUpdateTime) {
        /* Arrange */
        auto ledController = std::make_shared<MockLedController>();
        auto foregroundPattern = std::make_shared<LedPattern>(LED_COUNT, "foregroundPattern", ledController);
        LedFrame frame;
        frame.delayMs = 1;
        frame.circle.resize(1);
        frame.circle[0].r = 1;
        foregroundPattern->frames.push_back(frame);
        frame.circle[0].r = 2;
        foregroundPattern->frames.push_back(frame);
        frame.circle[0].r = 3;
        foregroundPattern->frames.push_back(frame);

        /* Act */
        auto now = std::chrono::steady_clock::now();
        auto firstFrame = now + ms(0);
        auto secondFrame = now + ms(1);

        foregroundPattern->startAnimationFrom(now);

        /* Test */
        foregroundPattern->updateTime(firstFrame);
        UNIT_ASSERT_EQUAL(foregroundPattern->getCurrentFrame().circle[0].r, 1);

        foregroundPattern->updateTime(secondFrame);
        UNIT_ASSERT_EQUAL(foregroundPattern->getCurrentFrame().circle[0].r, 2);

        foregroundPattern->updateTime(secondFrame);
        UNIT_ASSERT_EQUAL(foregroundPattern->getCurrentFrame().circle[0].r, 2);
    }
}
