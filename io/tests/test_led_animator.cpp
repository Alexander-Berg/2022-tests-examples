#include <yandex_io/modules/leds/led_manager/led_animator.h>
#include <yandex_io/modules/leds/led_manager/ng/default_animation_composition.h>
#include <yandex_io/modules/leds/led_manager/ng/default_animation_conductor.h>

#include <yandex_io/modules/leds/led_controller/led_controller.h>
#include <yandex_io/modules/leds/led_patterns/led_pattern.h>

#include <yandex_io/libs/logging/logging.h>
#include <yandex_io/tests/testlib/unittest_helper/unit_test_fixture.h>

#include <library/cpp/testing/unittest/env.h>
#include <library/cpp/testing/unittest/registar.h>

#include <chrono>
#include <future>

using namespace quasar;

using namespace std::chrono;
namespace {
    class MockLedController: public LedController {
    public:
        int getLedCount() const override {
            return 1;
        }

        void drawFrame(const LedCircle& /* colors */) override {
        }

        rgbw_color readColor(const std::string& /*stringColor*/) override {
            return {};
        }

        int getWidth() const override {
            return 1;
        }

        ~MockLedController() override = default;
    };

    class MockAnimation: public Animation {
    public:
        MockAnimation(const std::weak_ptr<quasar::LedDevice>& ledDevice, const std::function<void()>& updateTimeCallback)
            : Animation(ledDevice)
            , updateTimeCallback(updateTimeCallback)
        {
        }

        std::function<void()> updateTimeCallback;
        std::function<void()> startedCallback;
        void drawCurrentFrame() override {
        }
        bool finished() const override {
            return false;
        }
        TimePoint getEndOfFrameTimePoint() const override {
            return Animation::TimePoint();
        }
        void resetAnimation() override {
        }
        void updateTime(TimePoint /*timePoint*/) override {
            updateTimeCallback();
        }
        void startAnimationFrom(TimePoint /*timePoint*/) override {
        }
        nanoseconds getLength() const override {
            return std::chrono::nanoseconds();
        }
    };

} // namespace

Y_UNIT_TEST_SUITE_F(TestLedAnimator, QuasarUnitTestFixture) {
    Y_UNIT_TEST(testLedAnimatorHandlesEmptyConductors) {
        auto ledController = std::make_shared<MockLedController>();
        LedAnimator animator(LedPattern::getIdlePattern(1, ledController));

        auto emptyConductor = std::make_shared<DefaultAnimationConductor>(std::vector<const std::shared_ptr<AnimationComposition>>{});
        animator.play(emptyConductor);

        auto pattern = std::make_shared<LedPattern>(1, "name", ledController);

        auto conductor = std::make_shared<DefaultAnimationConductor>(pattern, SubstitutionType::FOREGROUND);
        animator.play(conductor);

        UNIT_ASSERT(emptyConductor->isFinished());
    }

    Y_UNIT_TEST(testLedAnimatorFinishGetsCaled) {
        auto ledController = std::make_shared<MockLedController>();
        LedAnimator animator(LedPattern::getIdlePattern(1, ledController));

        auto emptyConductor = std::make_shared<DefaultAnimationConductor>(std::vector<const std::shared_ptr<AnimationComposition>>{});
        animator.play(emptyConductor);

        auto pattern = std::make_shared<LedPattern>(1, "name", ledController);

        auto conductor = std::make_shared<DefaultAnimationConductor>(pattern, SubstitutionType::FOREGROUND);

        std::mutex m;
        std::unique_lock lock(m);
        bool cond = false;
        quasar::SteadyConditionVariable scv;
        conductor->setOnFinishedListener([&cond, &scv, &m] {
            YIO_LOG_WARN(" OnFinishedListener! ")
            std::lock_guard<std::mutex> lockGuard(m);
            cond = true;
            scv.notify_one();
        });
        animator.play(conductor);

        scv.wait(lock, [&]() { return cond; });

        UNIT_ASSERT(cond);
    }

    Y_UNIT_TEST(testLedAnimatorUpdateTimeGetsCalled) {
        auto ledController = std::make_shared<MockLedController>();

        std::mutex m;
        bool cond = false;
        quasar::SteadyConditionVariable scv;

        auto animation = std::make_shared<MockAnimation>(ledController, [&cond, &scv, &m] {
            std::lock_guard<std::mutex> lockGuard(m);
            cond = true;
            scv.notify_one();
        });

        auto conductor = std::make_shared<DefaultAnimationConductor>(std::vector<const std::shared_ptr<AnimationComposition>>{
            std::make_shared<DefaultAnimationComposition>(std::vector<std::shared_ptr<Animation>>{animation}, false)});
        LedAnimator animator(LedPattern::getIdlePattern(1, ledController));
        animator.play(conductor);

        std::unique_lock lock(m);
        scv.wait(lock, [&]() { return cond; });

        UNIT_ASSERT(cond);
    }
}
