#include <yandex_io/modules/leds/led_manager/led_animator.h>
#include <yandex_io/modules/leds/led_manager/ng/animation_conductor.h>
#include <yandex_io/modules/leds/led_manager/ng/default_animation_conductor.h>

#include <yandex_io/modules/leds/led_controller/led_controller.h>
#include <yandex_io/modules/leds/led_controller/null_led_controller.h>
#include <yandex_io/modules/leds/led_patterns/led_pattern.h>

#include <yandex_io/tests/testlib/test_utils.h>

#include <library/cpp/testing/unittest/registar.h>

#include <chrono>

using namespace quasar;
using namespace quasar::TestUtils;

using namespace std::chrono;

const int LED_COUNT = 1;

Y_UNIT_TEST_SUITE(LedSubsystem) {

    Y_UNIT_TEST(testDefaultConductorWithAnimationChronology) {
        AnimationChronology animationChronology;

        auto ledController = std::make_shared<NullLedController>();

        LedFrame backgroundFrame1;

        /* Create first background pattern */
        auto pattern = std::make_shared<LedPattern>(LED_COUNT, ledController);

        backgroundFrame1.circle.resize(LED_COUNT);
        backgroundFrame1.circle[0].r = 1;
        backgroundFrame1.delayMs = 3000;
        pattern->frames.push_back(backgroundFrame1);
        pattern->frames.push_back(backgroundFrame1);

        std::shared_ptr<DefaultAnimationConductor> defaultAnimationConductor = std::make_shared<DefaultAnimationConductor>(pattern);
        animationChronology.playNow(defaultAnimationConductor);

        UNIT_ASSERT_EQUAL(animationChronology.getAnimations().size(), 1);
    }

    Y_UNIT_TEST(testForegroundConductorWithAnimationChronology) {
        AnimationChronology animationChronology;

        auto ledController = std::make_shared<NullLedController>();

        LedFrame backgroundFrame1;

        /* Create first background pattern */
        auto pattern = std::make_shared<LedPattern>(LED_COUNT, ledController);

        backgroundFrame1.circle.resize(LED_COUNT);
        backgroundFrame1.circle[0].r = 1;
        backgroundFrame1.delayMs = 3000;
        pattern->frames.push_back(backgroundFrame1);
        pattern->frames.push_back(backgroundFrame1);

        std::shared_ptr<DefaultAnimationConductor> defaultAnimationConductor = std::make_shared<DefaultAnimationConductor>(pattern);
        defaultAnimationConductor->setSubstitutionType(SubstitutionType::FOREGROUND);
        animationChronology.playNow(defaultAnimationConductor);

        UNIT_ASSERT_EQUAL(animationChronology.getAnimations().size(), 1);
    }

    Y_UNIT_TEST(testEmptyDrawLoop) {
        AnimationChronology animationChronology;
        UNIT_ASSERT_EQUAL(animationChronology.getAnimations().size(), 0);

        auto ledController = std::make_shared<NullLedController>();

        LedAnimator animator(LedPattern::getIdlePattern(LED_COUNT, ledController));
    }

    Y_UNIT_TEST(testAnimationPlay) {
        AnimationChronology animationChronology;

        auto ledController = std::make_shared<NullLedController>();

        LedFrame backgroundFrame1;

        /* Create first background pattern */
        auto pattern = std::make_shared<LedPattern>(LED_COUNT, ledController);

        backgroundFrame1.circle.resize(LED_COUNT);
        backgroundFrame1.circle[0].r = 1;
        backgroundFrame1.delayMs = 1;
        pattern->frames.push_back(backgroundFrame1);
        pattern->frames.push_back(backgroundFrame1);

        UNIT_ASSERT(!pattern->finished());

        std::shared_ptr<DefaultAnimationConductor> defaultAnimationConductor = std::make_shared<DefaultAnimationConductor>(pattern);
        defaultAnimationConductor->setSubstitutionType(SubstitutionType::FOREGROUND);
        animationChronology.playNow(std::move(defaultAnimationConductor));

        LedAnimator animator(LedPattern::getIdlePattern(LED_COUNT, ledController));
    }

    Y_UNIT_TEST(testAnimationPriorities) {
        AnimationChronology animationChronology;

        auto ledController = std::make_shared<NullLedController>();

        LedFrame frame;

        /* First, create substitution pattern */
        auto substitutionPattern = std::make_shared<LedPattern>(LED_COUNT, "substitutionPattern", ledController);

        frame.circle.resize(LED_COUNT);
        frame.circle[0].r = 1;
        frame.delayMs = 1;
        substitutionPattern->frames.push_back(frame);

        std::shared_ptr<DefaultAnimationConductor> substitutionAnimationConductor = std::make_shared<DefaultAnimationConductor>(substitutionPattern);
        substitutionAnimationConductor->setSubstitutionType(SubstitutionType::SUBSTITUTION);
        animationChronology.playNow(std::move(substitutionAnimationConductor));

        /* Then create first foreground pattern */
        auto foregroundPattern = std::make_shared<LedPattern>(LED_COUNT, "foregroundPattern", ledController);

        frame.circle.resize(LED_COUNT);
        frame.circle[0].r = 2;
        frame.delayMs = 1;
        foregroundPattern->frames.push_back(frame);

        std::shared_ptr<DefaultAnimationConductor> foregroundAnimationConductor = std::make_shared<DefaultAnimationConductor>(foregroundPattern);
        foregroundAnimationConductor->setSubstitutionType(SubstitutionType::FOREGROUND);
        animationChronology.playNow(std::move(foregroundAnimationConductor));

        UNIT_ASSERT_EQUAL(animationChronology.getAnimations().size(), 1);
        UNIT_ASSERT_EQUAL(animationChronology.getAnimations().front(), substitutionPattern);

        auto ledController2 = std::make_shared<NullLedController>();

        /* Then create second foreground pattern */
        auto foregroundPattern2 = std::make_shared<LedPattern>(LED_COUNT, "foregroundPattern2", ledController2);

        frame.circle.resize(LED_COUNT);
        frame.circle[0].r = 3;
        frame.delayMs = 1;
        foregroundPattern2->frames.push_back(frame);

        std::shared_ptr<DefaultAnimationConductor> foregroundAnimationConductor2 = std::make_shared<DefaultAnimationConductor>(foregroundPattern2);
        foregroundAnimationConductor2->setSubstitutionType(SubstitutionType::FOREGROUND);
        animationChronology.playNow(std::move(foregroundAnimationConductor2));

        UNIT_ASSERT_EQUAL(animationChronology.getAnimations().size(), 2);
        const std::vector<std::shared_ptr<Animation>>& animations = animationChronology.getAnimations();
        const std::set<std::shared_ptr<Animation>, std::owner_less<std::shared_ptr<Animation>>> animationsSet(
            animations.begin(), animations.end());
        const std::set<std::shared_ptr<Animation>, std::owner_less<std::shared_ptr<Animation>>> etalonAnimations{
            substitutionPattern,
            foregroundPattern2,
        };
        UNIT_ASSERT_EQUAL(etalonAnimations, animationsSet);
    }
}
