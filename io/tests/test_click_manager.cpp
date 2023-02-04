#include <yandex_io/modules/buttons/click_handlers/click_manager/click_manager.h>

#include <yandex_io/libs/logging/logging.h>
#include <yandex_io/tests/testlib/test_utils.h>
#include <yandex_io/tests/testlib/unittest_helper/unit_test_fixture.h>

using namespace quasar;
using namespace quasar::TestUtils;
using namespace std::literals;

namespace {

    class ClickManagerFixture: public QuasarUnitTestFixture {
    public:
        ClickManagerFixture()
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
        std::set<int> testButtons;

        std::promise<void> timeoutPromise;
    };

} // namespace

Y_UNIT_TEST_SUITE_F(ClickManagerTest, ClickManagerFixture) {
    Y_UNIT_TEST(testOnClick) {
        EventChecker<int> checker({1});
        ClickManager clickManager(testButtons, 150ms, 150ms);
        clickManager.addOnClickAction((int)Btn::A, [&checker]() {
            UNIT_ASSERT(checker.addEvent(1));
        });
        clickManager.startHandlingEvents();

        clickManager.buttonPressed((int)Btn::A);
        clickManager.buttonReleased((int)Btn::A);

        checker.waitAllEvents();
    }

    Y_UNIT_TEST(testOnClick_wrongButton) {
        ClickManager clickManager(testButtons, 150ms, 150ms);
        clickManager.addOnClickAction((int)Btn::B, [this]() {
            timeoutPromise.set_value();
        });
        clickManager.startHandlingEvents();

        clickManager.buttonPressed((int)Btn::A);
        clickManager.buttonReleased((int)Btn::A);

        auto status = timeoutPromise.get_future().wait_for(std::chrono::seconds(1));
        UNIT_ASSERT_EQUAL(status, std::future_status::timeout);
    }

    Y_UNIT_TEST(testOnDoubleClick) {
        EventChecker<int> checker({1});
        ClickManager clickManager(testButtons, 250ms, 250ms);
        clickManager.addOnClickAction((int)Btn::A, [this, &checker]() {
            UNIT_ASSERT(checker.addEvent(-1));
            timeoutPromise.set_value();
        });
        clickManager.addOnDoubleClickAction((int)Btn::A, [&checker]() {
            UNIT_ASSERT(checker.addEvent(1));
        });
        clickManager.startHandlingEvents();

        clickManager.buttonPressed((int)Btn::A);
        clickManager.buttonReleased((int)Btn::A);

        clickManager.buttonPressed((int)Btn::A);
        clickManager.buttonReleased((int)Btn::A);

        checker.waitAllEvents();

        auto status = timeoutPromise.get_future().wait_for(std::chrono::seconds(1));
        UNIT_ASSERT_EQUAL(status, std::future_status::timeout);
    }

    Y_UNIT_TEST(testOnClick_hasOnDoubleClick) {
        EventChecker<int> checker({1});
        ClickManager clickManager(testButtons, 150ms, 150ms);
        clickManager.addOnClickAction((int)Btn::A, [&checker]() {
            UNIT_ASSERT(checker.addEvent(1));
        });
        clickManager.addOnDoubleClickAction((int)Btn::A, [this, &checker]() {
            UNIT_ASSERT(checker.addEvent(-1));
            timeoutPromise.set_value();
        });
        clickManager.startHandlingEvents();

        clickManager.buttonPressed((int)Btn::A);
        clickManager.buttonReleased((int)Btn::A);

        checker.waitAllEvents();

        auto status = timeoutPromise.get_future().wait_for(std::chrono::seconds(1));
        UNIT_ASSERT_EQUAL(status, std::future_status::timeout);
    }

    Y_UNIT_TEST(testCombination_singleButtonCombination) {
        EventChecker<int> checker({1, 2});
        ClickManager clickManager(testButtons, 150ms, 150ms);
        clickManager.addOnClickAction((int)Btn::A, [this, &checker]() {
            UNIT_ASSERT(checker.addEvent(-1));
            timeoutPromise.set_value();
        });
        clickManager.addOnPressedAction((int)Btn::A, [&checker]() { UNIT_ASSERT(checker.addEvent(1)); }, std::chrono::milliseconds(200), [&checker]() { UNIT_ASSERT(checker.addEvent(2)); });
        clickManager.startHandlingEvents();

        clickManager.buttonPressed((int)Btn::A);
        checker.waitForEvents(1);

        YIO_LOG_INFO("onReleased of comb should trigger");
        clickManager.buttonReleased((int)Btn::A);
        checker.waitAllEvents();

        YIO_LOG_INFO("button release should not trigger onClick");
        auto status = timeoutPromise.get_future().wait_for(std::chrono::seconds(1));
        UNIT_ASSERT_EQUAL(status, std::future_status::timeout);
    }

    Y_UNIT_TEST(testCombination_severalCombinations) {
        EventChecker<int> checker({1, 2, 3, 4});
        ClickManager clickManager(testButtons, 150ms, 150ms);
        clickManager.addOnClickAction((int)Btn::A, [this, &checker]() {
            UNIT_ASSERT(checker.addEvent(-1));
            timeoutPromise.set_value();
        });
        clickManager.addOnClickAction((int)Btn::B, [this, &checker]() {
            UNIT_ASSERT(checker.addEvent(-1));
            timeoutPromise.set_value();
        });
        clickManager.addOnPressedAction((int)Btn::A, [&checker]() { UNIT_ASSERT(checker.addEvent(1)); }, std::chrono::milliseconds(0), [&checker]() { UNIT_ASSERT(checker.addEvent(2)); });
        clickManager.addOnCombinationPressedAction(
            {(int)Btn::A, (int)Btn::B}, [&checker]() { UNIT_ASSERT(checker.addEvent(3)); }, std::chrono::milliseconds(0), [&checker]() { UNIT_ASSERT(checker.addEvent(4)); });
        clickManager.startHandlingEvents();

        clickManager.buttonPressed((int)Btn::A);
        YIO_LOG_INFO("onPressed of the first comb should trigger");
        checker.waitForEvents(1);

        YIO_LOG_INFO("onReleased of the first comb and onPressed of the second one should trigger");
        clickManager.buttonPressed((int)Btn::B);
        checker.waitForEvents(2);
        checker.waitForEvents(3);

        YIO_LOG_INFO("onReleased of of the second comb should trigger");
        clickManager.buttonReleased((int)Btn::A);
        checker.waitAllEvents();

        clickManager.buttonReleased((int)Btn::B);
        YIO_LOG_INFO("button release should not trigger onClick");
        auto status = timeoutPromise.get_future().wait_for(std::chrono::seconds(1));
        UNIT_ASSERT_EQUAL(status, std::future_status::timeout);
    }

    Y_UNIT_TEST(testCombination_extraButtonsDontTrigger) {
        ClickManager clickManager(testButtons, 150ms, 150ms);
        clickManager.addOnCombinationPressedAction({(int)Btn::A, (int)Btn::B}, [this]() {
            timeoutPromise.set_value();
        });
        clickManager.addOnCombinationPressedAction({(int)Btn::B, (int)Btn::C}, [this]() {
            timeoutPromise.set_value();
        });
        clickManager.startHandlingEvents();

        clickManager.buttonPressed((int)Btn::A);
        clickManager.buttonPressed((int)Btn::C);
        clickManager.buttonPressed((int)Btn::B);

        auto status = timeoutPromise.get_future().wait_for(std::chrono::seconds(1));
        UNIT_ASSERT_EQUAL(status, std::future_status::timeout);
    }

    Y_UNIT_TEST(testCombination_triggerOnReleaseWhenOnPressIsNull) {
        EventChecker<int> checker({1, 2});
        ClickManager clickManager(testButtons, 0ms, 150ms);
        clickManager.addOnPressedAction(
            (int)Btn::A,
            nullptr,
            std::chrono::milliseconds(0),
            [&checker]() {
                UNIT_ASSERT(checker.addEvent(1));
            });
        clickManager.addOnCombinationPressedAction(
            {(int)Btn::A, (int)Btn::B},
            nullptr,
            std::chrono::milliseconds(0),
            [&checker]() {
                UNIT_ASSERT(checker.addEvent(2));
            });

        clickManager.startHandlingEvents();

        clickManager.buttonPressed((int)Btn::A);
        clickManager.buttonReleased((int)Btn::A);
        checker.waitForEvents(1);

        clickManager.buttonPressed((int)Btn::B);
        clickManager.buttonPressed((int)Btn::A);
        clickManager.buttonReleased((int)Btn::A);
        checker.waitAllEvents();
    }

    Y_UNIT_TEST(testManager_returnToIdle_afterClick) {
        EventChecker<int> checker({1, 2});
        ClickManager clickManager(testButtons, 150ms, 150ms);
        clickManager.addOnClickAction((int)Btn::A, [&checker]() {
            UNIT_ASSERT(checker.addEvent(1));
        });
        clickManager.addOnClickAction((int)Btn::B, [&checker]() {
            UNIT_ASSERT(checker.addEvent(2));
        });
        clickManager.startHandlingEvents();

        clickManager.buttonPressed((int)Btn::A);
        clickManager.buttonReleased((int)Btn::A);
        checker.waitForEvents(1);

        clickManager.buttonPressed((int)Btn::B);
        clickManager.buttonReleased((int)Btn::B);
        checker.waitAllEvents();
    }

    Y_UNIT_TEST(testManager_returnToIdle_afterCombination) {
        EventChecker<int> checker({1, 2});
        ClickManager clickManager(testButtons, 150ms, 150ms);
        clickManager.addOnCombinationPressedAction({(int)Btn::A, (int)Btn::B, (int)Btn::C}, [&checker]() {
            UNIT_ASSERT(checker.addEvent(1));
        });
        clickManager.addOnClickAction((int)Btn::B, [&checker]() {
            UNIT_ASSERT(checker.addEvent(2));
        });
        clickManager.startHandlingEvents();

        clickManager.buttonPressed((int)Btn::A);
        clickManager.buttonPressed((int)Btn::B);
        clickManager.buttonPressed((int)Btn::C);
        checker.waitForEvents(1);
        clickManager.buttonReleased((int)Btn::B);
        clickManager.buttonReleased((int)Btn::A);
        clickManager.buttonReleased((int)Btn::C);

        clickManager.buttonPressed((int)Btn::B);
        clickManager.buttonReleased((int)Btn::B);
        checker.waitAllEvents();
    }

    Y_UNIT_TEST(testManager_returnToIdle_afterDoubleClick) {
        EventChecker<int> checker({1, 2});
        ClickManager clickManager(testButtons, 150ms, 150ms);
        clickManager.addOnDoubleClickAction((int)Btn::A, [&checker]() {
            UNIT_ASSERT(checker.addEvent(1));
        });
        clickManager.addOnClickAction((int)Btn::A, [&checker]() {
            UNIT_ASSERT(checker.addEvent(2));
        });
        clickManager.startHandlingEvents();

        clickManager.buttonPressed((int)Btn::A);
        clickManager.buttonReleased((int)Btn::A);
        clickManager.buttonPressed((int)Btn::A);
        clickManager.buttonReleased((int)Btn::A);
        checker.waitForEvents(1);

        clickManager.buttonPressed((int)Btn::A);
        clickManager.buttonReleased((int)Btn::A);
        checker.waitAllEvents();
    }

    Y_UNIT_TEST(testManager_ignoresNotPressedButtons) {
        EventChecker<int> checker({1});
        ClickManager clickManager(testButtons, 150ms, 150ms);
        clickManager.addOnClickAction((int)Btn::A, [this, &checker]() {
            UNIT_ASSERT(checker.addEvent(-1));
            timeoutPromise.set_value();
        });
        clickManager.addOnClickAction((int)Btn::B, [&checker]() {
            UNIT_ASSERT(checker.addEvent(1));
        });
        clickManager.startHandlingEvents();

        clickManager.buttonReleased((int)Btn::A);

        auto status = timeoutPromise.get_future().wait_for(std::chrono::seconds(1));
        UNIT_ASSERT_EQUAL(status, std::future_status::timeout);

        clickManager.buttonPressed((int)Btn::B);
        clickManager.buttonReleased((int)Btn::B);
        checker.waitAllEvents();
    }

    Y_UNIT_TEST(testCombination_severalCombinationsOnSameButtons) {
        EventChecker<int> checker({1, 2});
        ClickManager clickManager(testButtons, 150ms, 150ms);
        clickManager.addOnCombinationPressedAction({(int)Btn::A, (int)Btn::B}, [&checker]() {
            UNIT_ASSERT(checker.addEvent(1));
        }, std::chrono::milliseconds(200));
        clickManager.addOnCombinationPressedAction({(int)Btn::A, (int)Btn::B}, [&checker]() {
            UNIT_ASSERT(checker.addEvent(2));
        }, std::chrono::milliseconds(500));
        clickManager.startHandlingEvents();

        clickManager.buttonPressed((int)Btn::B);
        clickManager.buttonPressed((int)Btn::A);

        checker.waitAllEvents();
    }

    Y_UNIT_TEST(testClickAndPress) {
        EventChecker<int> checker({1, 2});
        ClickManager clickManager(testButtons, 150ms, 150ms);
        clickManager.addOnPressedAction((int)Btn::A, [&checker]() {
            UNIT_ASSERT(checker.addEvent(2));
        }, 0ms);
        clickManager.addOnClickAction((int)Btn::A, [&checker]() {
            UNIT_ASSERT(checker.addEvent(1));
        });
        clickManager.startHandlingEvents();

        clickManager.buttonPressed((int)Btn::A);
        clickManager.buttonReleased((int)Btn::A);
        clickManager.buttonPressed((int)Btn::A);

        checker.waitAllEvents();
    }

    Y_UNIT_TEST(testDoubleClickAndPress) {
        EventChecker<int> checker({1, 2});
        ClickManager clickManager(testButtons, 150ms, 150ms);
        clickManager.addOnPressedAction((int)Btn::A, [&checker]() {
            UNIT_ASSERT(checker.addEvent(2));
        }, 0ms);
        clickManager.addOnDoubleClickAction((int)Btn::A, [&checker]() {
            UNIT_ASSERT(checker.addEvent(1));
        });
        clickManager.startHandlingEvents();

        clickManager.buttonPressed((int)Btn::A);
        clickManager.buttonReleased((int)Btn::A);
        clickManager.buttonPressed((int)Btn::A);
        clickManager.buttonReleased((int)Btn::A);
        clickManager.buttonPressed((int)Btn::A);

        checker.waitAllEvents();
    }

    Y_UNIT_TEST(testClickAndPress_breaksCombination) {
        ClickManager clickManager(testButtons, 150ms, 150ms);
        clickManager.addOnCombinationPressedAction({(int)Btn::B, (int)Btn::A}, [this]() {
            timeoutPromise.set_value();
        });
        clickManager.addOnClickAction((int)Btn::A, [this]() {
            timeoutPromise.set_value();
        });
        clickManager.startHandlingEvents();

        clickManager.buttonPressed((int)Btn::B);

        clickManager.buttonPressed((int)Btn::A);
        clickManager.buttonReleased((int)Btn::A);
        clickManager.buttonPressed((int)Btn::A);

        auto status = timeoutPromise.get_future().wait_for(std::chrono::seconds(1));
        UNIT_ASSERT_EQUAL(status, std::future_status::timeout);
    }

    Y_UNIT_TEST(testIdleOnButtonsReleasing) {
        ClickManager clickManager(testButtons, 150ms, 150ms);
        clickManager.addOnCombinationPressedAction({(int)Btn::B, (int)Btn::A}, [this]() {
            timeoutPromise.set_value();
        });
        clickManager.startHandlingEvents();

        clickManager.buttonPressed((int)Btn::C);
        clickManager.buttonPressed((int)Btn::B);
        clickManager.buttonReleased((int)Btn::C);
        clickManager.buttonPressed((int)Btn::A);
        // A and B are pressed but state is RELEASING
        auto status = timeoutPromise.get_future().wait_for(std::chrono::seconds(1));
        UNIT_ASSERT_EQUAL(status, std::future_status::timeout);
    }
}
