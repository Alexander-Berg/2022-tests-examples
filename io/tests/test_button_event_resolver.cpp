#include <yandex_io/modules/buttons/click_handlers/click_manager/button_event_resolver.h>

#include <yandex_io/libs/base/named_callback_queue.h>
#include <yandex_io/libs/logging/logging.h>
#include <yandex_io/tests/testlib/test_utils.h>
#include <yandex_io/tests/testlib/unittest_helper/unit_test_fixture.h>

using namespace quasar;
using namespace quasar::TestUtils;
using namespace std::literals;

using BtnEvent = ButtonEventResolver::Event;

namespace {

    class ButtonEventResolverFixture: public QuasarUnitTestFixture {
    public:
        ButtonEventResolverFixture()
            : callbackQueue_(std::make_shared<NamedCallbackQueue>("test"))
        {
        }

        void createResolver(EventChecker<BtnEvent>& checker, uint8_t maxLevel = 3, std::chrono::milliseconds releaseTm = 200ms, std::chrono::milliseconds pressTm = 200ms) {
            auto func = [&checker](int /*buttonIdx*/, BtnEvent event) {
                YIO_LOG_DEBUG("Resolved event: " << (int)event);
                UNIT_ASSERT(checker.addEvent(event));
            };

            resolver_ = std::make_unique<ButtonEventResolver>(100, callbackQueue_, std::move(func), maxLevel, releaseTm, pressTm);
        }

        void press() {
            callbackQueue_->add([this]() { resolver_->buttonPressed(); });
        };

        void release() {
            callbackQueue_->add([this]() { resolver_->buttonReleased(); });
        }

        void click() {
            press();
            release();
        }

        void doubleClick() {
            click();
            click();
        }

        void tripleClick() {
            doubleClick();
            click();
        }

    protected:
        std::shared_ptr<NamedCallbackQueue> callbackQueue_;
        std::unique_ptr<ButtonEventResolver> resolver_;
        std::vector<BtnEvent> resolvedEvents_;
        std::mutex mutex_;
        quasar::SteadyConditionVariable CV_;
    };

} // namespace

Y_UNIT_TEST_SUITE_F(ButtonEventResolverTest, ButtonEventResolverFixture) {
    Y_UNIT_TEST(testResolver_press) {
        EventChecker<BtnEvent> checker({BtnEvent::PRESSED});
        createResolver(checker);

        press();
        checker.waitAllEvents();
    }

    Y_UNIT_TEST(testResolver_release) {
        EventChecker<BtnEvent> checker({BtnEvent::PRESSED, BtnEvent::RELEASED});
        createResolver(checker);

        press();
        checker.waitForEvents(1);

        release();
        checker.waitAllEvents();
    }

    Y_UNIT_TEST(testResolver_click) {
        EventChecker<BtnEvent> checker({BtnEvent::CLICK});
        createResolver(checker);

        click();
        checker.waitAllEvents();
    }

    Y_UNIT_TEST(testResolver_clickAndPressed) {
        EventChecker<BtnEvent> checker({BtnEvent::CLICK_AND_PRESSED});
        createResolver(checker);

        click();
        press();
        checker.waitAllEvents();
    }

    Y_UNIT_TEST(testResolver_doubleClick) {
        EventChecker<BtnEvent> checker({BtnEvent::DOUBLE_CLICK});
        createResolver(checker);

        doubleClick();
        checker.waitAllEvents();
    }

    Y_UNIT_TEST(testResolver_doubleClickAndPressed) {
        EventChecker<BtnEvent> checker({BtnEvent::DOUBLE_CLICK_AND_PRESSED});
        createResolver(checker);

        doubleClick();
        press();
        checker.waitAllEvents();
    }

    Y_UNIT_TEST(testResolver_tripleClick) {
        EventChecker<BtnEvent> checker({BtnEvent::TRIPLE_CLICK});
        createResolver(checker);

        tripleClick();
        checker.waitAllEvents();
    }

    Y_UNIT_TEST(testResolver_maxLevel_1) {
        EventChecker<BtnEvent> checker({BtnEvent::CLICK, BtnEvent::CLICK, BtnEvent::CLICK, BtnEvent::PRESSED});
        createResolver(checker, 1);

        tripleClick();
        press();

        checker.waitAllEvents();
    }

    Y_UNIT_TEST(testResolver_maxLevel_2) {
        EventChecker<BtnEvent> checker({BtnEvent::DOUBLE_CLICK, BtnEvent::DOUBLE_CLICK, BtnEvent::PRESSED});
        createResolver(checker, 2);

        tripleClick();
        click();
        press();

        checker.waitAllEvents();
    }

    Y_UNIT_TEST(testResolver_manyClicks) {
        EventChecker<BtnEvent> checker({
            BtnEvent::TRIPLE_CLICK, BtnEvent::CLICK_AND_PRESSED, BtnEvent::RELEASED, BtnEvent::DOUBLE_CLICK,
        });
        createResolver(checker);

        tripleClick();
        checker.waitForEvents(1);

        click();
        press();
        checker.waitForEvents(2);

        release();
        doubleClick();
        checker.waitAllEvents();
    }

    Y_UNIT_TEST(testResolver_manyClicks_ignoresExtra) {
        EventChecker<BtnEvent> checker({
            BtnEvent::DOUBLE_CLICK_AND_PRESSED, BtnEvent::RELEASED, BtnEvent::CLICK, BtnEvent::TRIPLE_CLICK,
        });
        createResolver(checker);

        doubleClick();
        press();
        press(); // should be ignored
        checker.waitForEvents(1);

        release();
        release(); // should be ignored
        press();
        press(); // should be ignored
        release();
        checker.waitForEvents(3);

        tripleClick();
        checker.waitAllEvents();
    }
}
