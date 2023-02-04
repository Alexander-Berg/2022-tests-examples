#include <yandex_io/modules/buttons/click_handlers/swipe_manager/swipe_manager.h>

#include <yandex_io/libs/logging/logging.h>
#include <yandex_io/tests/testlib/test_utils.h>
#include <yandex_io/tests/testlib/unittest_helper/unit_test_fixture.h>

using namespace quasar;
using namespace quasar::TestUtils;
using namespace std::literals;

namespace {

    class SwipeManagerFixture: public QuasarUnitTestFixture {
    public:
        SwipeManagerFixture()
            : testButtons({(int)Btn::A,
                           (int)Btn::B,
                           (int)Btn::C,
                           (int)Btn::D})
        {
        }

    protected:
        enum class Btn {
            A,
            B,
            C,
            D
        };
        std::unordered_set<int> testButtons;

        std::promise<void> timeoutPromise;
    };

} // namespace

Y_UNIT_TEST_SUITE_F(SwipeManagerTest, SwipeManagerFixture) {
    Y_UNIT_TEST(testOnClick) {
        EventChecker<int> checker({1});
        SwipeManager manager(testButtons, 150ms, 150ms);
        manager.addOnClickAction((int)Btn::A, [&checker]() {
            UNIT_ASSERT(checker.addEvent(1));
        });
        manager.startHandlingEvents();

        manager.buttonPressed((int)Btn::A);
        manager.buttonReleased((int)Btn::A);

        checker.waitAllEvents();
    }

    Y_UNIT_TEST(testOnClick_wrongButton) {
        SwipeManager manager(testButtons, 150ms, 150ms);
        manager.addOnClickAction((int)Btn::B, [this]() {
            timeoutPromise.set_value();
        });
        manager.startHandlingEvents();

        manager.buttonPressed((int)Btn::A);
        manager.buttonReleased((int)Btn::A);

        auto status = timeoutPromise.get_future().wait_for(std::chrono::seconds(1));
        UNIT_ASSERT_EQUAL(status, std::future_status::timeout);
    }

    Y_UNIT_TEST(testOnClick_hasOnSwipe) {
        EventChecker<int> checker({1});
        SwipeManager manager(testButtons, 150ms, 150ms);
        manager.addOnClickAction((int)Btn::A, [&checker]() {
            UNIT_ASSERT(checker.addEvent(1));
        });
        manager.addOnSwipeAction({(int)Btn::A, (int)Btn::B}, [&checker]() {
            UNIT_ASSERT(checker.addEvent(2));
        });
        manager.startHandlingEvents();

        manager.buttonPressed((int)Btn::A);
        manager.buttonReleased((int)Btn::A);

        checker.waitAllEvents();

        auto status = timeoutPromise.get_future().wait_for(std::chrono::seconds(1));
        UNIT_ASSERT_EQUAL(status, std::future_status::timeout);
    }

    Y_UNIT_TEST(testOnSwipe) {
        EventChecker<int> checker({1});
        SwipeManager manager(testButtons, 250ms, 250ms);
        manager.addOnSwipeAction({(int)Btn::A, (int)Btn::B}, [&checker]() {
            UNIT_ASSERT(checker.addEvent(1));
        });
        manager.startHandlingEvents();

        manager.buttonPressed((int)Btn::A);
        manager.buttonReleased((int)Btn::A);

        manager.buttonPressed((int)Btn::B);
        manager.buttonReleased((int)Btn::B);

        checker.waitAllEvents();

        auto status = timeoutPromise.get_future().wait_for(std::chrono::seconds(1));
        UNIT_ASSERT_EQUAL(status, std::future_status::timeout);
    }

    Y_UNIT_TEST(testOnSwipe_hasOnClick) {
        EventChecker<int> checker({1});
        SwipeManager manager(testButtons, 250ms, 250ms);
        manager.addOnClickAction((int)Btn::A, [this, &checker]() {
            UNIT_ASSERT(checker.addEvent(-1));
            timeoutPromise.set_value();
        });
        manager.addOnSwipeAction({(int)Btn::A, (int)Btn::B}, [&checker]() {
            UNIT_ASSERT(checker.addEvent(1));
        });
        manager.startHandlingEvents();

        manager.buttonPressed((int)Btn::A);
        manager.buttonReleased((int)Btn::A);

        manager.buttonPressed((int)Btn::B);
        manager.buttonReleased((int)Btn::B);

        checker.waitAllEvents();

        auto status = timeoutPromise.get_future().wait_for(std::chrono::seconds(1));
        UNIT_ASSERT_EQUAL(status, std::future_status::timeout);
    }

    Y_UNIT_TEST(testOnPressedAction) {
        EventChecker<int> checker({1, 2});
        SwipeManager manager(testButtons, 150ms, 150ms);
        manager.addOnClickAction((int)Btn::A, [this, &checker]() {
            UNIT_ASSERT(checker.addEvent(-1));
            timeoutPromise.set_value();
        });
        manager.addOnPressedAction((int)Btn::A, [&checker]() { UNIT_ASSERT(checker.addEvent(1)); }, std::chrono::milliseconds(200), [&checker]() { UNIT_ASSERT(checker.addEvent(2)); });
        manager.startHandlingEvents();

        manager.buttonPressed((int)Btn::A);
        checker.waitForEvents(1);

        YIO_LOG_INFO("onReleased of comb should trigger");
        manager.buttonReleased((int)Btn::A);
        checker.waitAllEvents();

        YIO_LOG_INFO("button release should not trigger onClick");
        auto status = timeoutPromise.get_future().wait_for(std::chrono::seconds(1));
        UNIT_ASSERT_EQUAL(status, std::future_status::timeout);
    }

    Y_UNIT_TEST(testOnPressedAction_several) {
        EventChecker<int> checker({1, 2, 3, 4});
        SwipeManager manager(testButtons, 150ms, 150ms);
        manager.addOnClickAction((int)Btn::A, [this, &checker]() {
            UNIT_ASSERT(checker.addEvent(-1));
            timeoutPromise.set_value();
        });
        manager.addOnClickAction((int)Btn::B, [this, &checker]() {
            UNIT_ASSERT(checker.addEvent(-1));
            timeoutPromise.set_value();
        });
        manager.addOnPressedAction((int)Btn::A, [&checker]() { UNIT_ASSERT(checker.addEvent(1)); }, std::chrono::milliseconds(0), [&checker]() { UNIT_ASSERT(checker.addEvent(2)); });
        manager.addOnPressedAction((int)Btn::B, [&checker]() { UNIT_ASSERT(checker.addEvent(3)); }, std::chrono::milliseconds(0), [&checker]() { UNIT_ASSERT(checker.addEvent(4)); });
        manager.startHandlingEvents();

        manager.buttonPressed((int)Btn::A);
        YIO_LOG_INFO("onPressed of the first comb should trigger");
        checker.waitForEvents(1);
        manager.buttonReleased((int)Btn::A);
        checker.waitForEvents(2);

        manager.buttonPressed((int)Btn::B);
        checker.waitForEvents(3);
        manager.buttonReleased((int)Btn::B);
        checker.waitAllEvents();

        YIO_LOG_INFO("button release should not trigger onClick");
        auto status = timeoutPromise.get_future().wait_for(std::chrono::seconds(1));
        UNIT_ASSERT_EQUAL(status, std::future_status::timeout);
    }

    Y_UNIT_TEST(testResolver_returnToIdle_afterClick) {
        EventChecker<int> checker({1, 2});
        SwipeManager manager(testButtons, 150ms, 150ms);
        manager.addOnClickAction((int)Btn::A, [&checker]() {
            UNIT_ASSERT(checker.addEvent(1));
        });
        manager.addOnClickAction((int)Btn::B, [&checker]() {
            UNIT_ASSERT(checker.addEvent(2));
        });
        manager.startHandlingEvents();

        manager.buttonPressed((int)Btn::A);
        manager.buttonReleased((int)Btn::A);
        checker.waitForEvents(1);

        manager.buttonPressed((int)Btn::B);
        manager.buttonReleased((int)Btn::B);
        checker.waitAllEvents();
    }

    Y_UNIT_TEST(testResolver_returnToIdle_afterSwipe) {
        EventChecker<int> checker({1, 2});
        SwipeManager manager(testButtons, 150ms, 150ms);
        manager.addOnSwipeAction({(int)Btn::A, (int)Btn::B}, [&checker]() {
            UNIT_ASSERT(checker.addEvent(1));
        });
        manager.addOnClickAction((int)Btn::A, [&checker]() {
            UNIT_ASSERT(checker.addEvent(-1));
        });
        manager.addOnClickAction((int)Btn::B, [&checker]() {
            UNIT_ASSERT(checker.addEvent(-1));
        });
        manager.addOnClickAction((int)Btn::C, [&checker]() {
            UNIT_ASSERT(checker.addEvent(2));
        });
        manager.startHandlingEvents();

        manager.buttonPressed((int)Btn::A);
        manager.buttonReleased((int)Btn::A);
        manager.buttonPressed((int)Btn::B);
        manager.buttonReleased((int)Btn::B);
        checker.waitForEvents(1);

        manager.buttonPressed((int)Btn::C);
        manager.buttonReleased((int)Btn::C);
        checker.waitAllEvents();
    }

    Y_UNIT_TEST(testResolver_ignoresNotPressedButtons) {
        EventChecker<int> checker({1});
        SwipeManager manager(testButtons, 150ms, 150ms);
        manager.addOnClickAction((int)Btn::A, [this, &checker]() {
            UNIT_ASSERT(checker.addEvent(-1));
            timeoutPromise.set_value();
        });
        manager.addOnClickAction((int)Btn::B, [&checker]() {
            UNIT_ASSERT(checker.addEvent(1));
        });
        manager.startHandlingEvents();

        manager.buttonReleased((int)Btn::A);

        auto status = timeoutPromise.get_future().wait_for(std::chrono::seconds(1));
        UNIT_ASSERT_EQUAL(status, std::future_status::timeout);

        manager.buttonPressed((int)Btn::B);
        manager.buttonReleased((int)Btn::B);
        checker.waitAllEvents();
    }

    Y_UNIT_TEST(testOnPressed_severalCombinationsOnSameButtons) {
        EventChecker<int> checker({1, 2});
        SwipeManager manager(testButtons, 150ms, 150ms);
        manager.addOnPressedAction((int)Btn::A, [&checker]() {
            UNIT_ASSERT(checker.addEvent(1));
        }, std::chrono::milliseconds(200));
        manager.addOnPressedAction((int)Btn::A, [&checker]() {
            UNIT_ASSERT(checker.addEvent(2));
        }, std::chrono::milliseconds(500));
        manager.startHandlingEvents();

        manager.buttonPressed((int)Btn::A);

        checker.waitAllEvents();
    }

    Y_UNIT_TEST(testClickAndPress) {
        EventChecker<int> checker({1, 2});
        SwipeManager manager(testButtons, 150ms, 150ms);
        manager.addOnPressedAction((int)Btn::A, [&checker]() {
            UNIT_ASSERT(checker.addEvent(2));
        }, 0ms);
        manager.addOnClickAction((int)Btn::A, [&checker]() {
            UNIT_ASSERT(checker.addEvent(1));
        });
        manager.startHandlingEvents();

        manager.buttonPressed((int)Btn::A);
        manager.buttonReleased((int)Btn::A);
        manager.buttonPressed((int)Btn::A);

        checker.waitAllEvents();
    }
}
