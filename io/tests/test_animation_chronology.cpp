#include <yandex_io/modules/leds/led_manager/led_animator.h>
#include <yandex_io/modules/leds/led_manager/ng/animation_conductor.h>
#include <yandex_io/modules/leds/led_manager/ng/default_animation_composition.h>
#include <yandex_io/modules/leds/led_manager/ng/default_animation_conductor.h>

#include <yandex_io/modules/leds/led_controller/null_led_controller.h>
#include <yandex_io/modules/leds/led_patterns/led_pattern.h>

#include <yandex_io/tests/testlib/test_utils.h>

#include <library/cpp/testing/unittest/registar.h>

#include <algorithm>
#include <chrono>
#include <vector>

using namespace quasar;
using namespace quasar::TestUtils;

using namespace std::chrono;

const int LED_COUNT = 1;

Y_UNIT_TEST_SUITE(AnimationChronology) {
    Y_UNIT_TEST(testBackgroundWorksOk) {
        AnimationChronology animationChronology;

        auto ledController = std::make_shared<NullLedController>();
        auto ledController2 = std::make_shared<NullLedController>();

        LedFrame backgroundFrame1;

        auto bgPattern = std::make_shared<LedPattern>(LED_COUNT, ledController);
        std::shared_ptr<DefaultAnimationConductor> animationConductor = std::make_shared<DefaultAnimationConductor>(bgPattern);
        animationChronology.playNow(animationConductor);

        auto animations = animationChronology.getAnimations();
        UNIT_ASSERT_EQUAL(animations.size(), 1);
        UNIT_ASSERT_EQUAL(bgPattern, animations[0]);

        auto anotherPattern = std::make_shared<LedPattern>(LED_COUNT, ledController);
        animationConductor = std::make_shared<DefaultAnimationConductor>(anotherPattern);
        animationChronology.playNow(animationConductor);
        animations = animationChronology.getAnimations();
        UNIT_ASSERT_EQUAL(animations.size(), 1);
        UNIT_ASSERT_EQUAL(anotherPattern, animations[0]);

        auto patternWithController2 = std::make_shared<LedPattern>(LED_COUNT, ledController2);
        animationConductor = std::make_shared<DefaultAnimationConductor>(patternWithController2);
        animationChronology.playNow(animationConductor);
        animations = animationChronology.getAnimations();
        UNIT_ASSERT_EQUAL(animations.size(), 1);
        UNIT_ASSERT_EQUAL(patternWithController2, animations[0]);
    }

    Y_UNIT_TEST(testForegroundWorksOk) {
        AnimationChronology animationChronology;

        auto ledController = std::make_shared<NullLedController>();
        auto ledController2 = std::make_shared<NullLedController>();

        auto bgPattern = std::make_shared<LedPattern>(LED_COUNT, ledController);
        std::shared_ptr<DefaultAnimationConductor> animationConductor = std::make_shared<DefaultAnimationConductor>(bgPattern);
        animationChronology.playNow(animationConductor);

        auto foregroundPatternLedController2 = std::make_shared<LedPattern>(LED_COUNT, ledController2);
        animationConductor = std::make_shared<DefaultAnimationConductor>(foregroundPatternLedController2);
        animationConductor->setSubstitutionType(SubstitutionType::FOREGROUND);
        animationChronology.playNow(animationConductor);
        auto animations = animationChronology.getAnimations();
        UNIT_ASSERT_EQUAL(animations.size(), 2);
        UNIT_ASSERT(std::find(animations.begin(), animations.end(), foregroundPatternLedController2) != 0);
        UNIT_ASSERT(std::find(animations.begin(), animations.end(), bgPattern) != 0);

        auto anotherForegroundPatternLedController2 = std::make_shared<LedPattern>(LED_COUNT, ledController2);
        animationConductor = std::make_shared<DefaultAnimationConductor>(anotherForegroundPatternLedController2);
        animationConductor->setSubstitutionType(SubstitutionType::FOREGROUND);
        animationChronology.playNow(animationConductor);
        animations = animationChronology.getAnimations();
        UNIT_ASSERT_EQUAL(animations.size(), 2);
        UNIT_ASSERT(std::find(animations.begin(), animations.end(), bgPattern) != 0);
        UNIT_ASSERT(std::find(animations.begin(), animations.end(), anotherForegroundPatternLedController2) != 0);
    }

    Y_UNIT_TEST(testSubstitutionWorksOk) {
        AnimationChronology animationChronology;

        auto ledController = std::make_shared<NullLedController>();
        auto ledController2 = std::make_shared<NullLedController>();

        auto foregroundPatternOfLedController = std::make_shared<LedPattern>(LED_COUNT, ledController);
        auto foregroundPatternOfLedController2 = std::make_shared<LedPattern>(LED_COUNT, ledController2);
        std::vector<std::shared_ptr<Animation>> p = std::vector<std::shared_ptr<Animation>>{foregroundPatternOfLedController, foregroundPatternOfLedController2};
        const std::shared_ptr<DefaultAnimationComposition> ptr = std::make_shared<DefaultAnimationComposition>(p, false);
        std::shared_ptr<DefaultAnimationConductor> animationConductor = std::make_shared<DefaultAnimationConductor>(
            std::vector<const std::shared_ptr<AnimationComposition>>{ptr});
        animationConductor->setSubstitutionType(SubstitutionType::FOREGROUND);
        animationChronology.playNow(animationConductor);

        auto anotherForegroundPatternLedController = std::make_shared<LedPattern>(LED_COUNT, ledController);
        animationConductor = std::make_shared<DefaultAnimationConductor>(anotherForegroundPatternLedController);
        animationConductor->setSubstitutionType(SubstitutionType::SUBSTITUTION);
        animationChronology.playNow(animationConductor);

        auto animations = animationChronology.getAnimations();
        UNIT_ASSERT_EQUAL(animations.size(), 2);
        UNIT_ASSERT(std::find(animations.begin(), animations.end(), anotherForegroundPatternLedController) != 0);
        UNIT_ASSERT(std::find(animations.begin(), animations.end(), foregroundPatternOfLedController2) != 0);
    }
}
