#include <yandex_io/modules/buttons/click_handlers/swipe_manager/swipe_event_resolver.h>

#include <yandex_io/libs/base/named_callback_queue.h>
#include <yandex_io/libs/logging/logging.h>
#include <yandex_io/tests/testlib/test_utils.h>
#include <yandex_io/tests/testlib/unittest_helper/unit_test_fixture.h>

using namespace quasar;
using namespace quasar::TestUtils;
using namespace std::literals;

namespace {

    using Event = SwipeEventResolver::Event;
    using SwipeEventChecker = EventChecker<std::pair<Event, std::vector<int>>>;

    constexpr int ButtonA = 1;
    constexpr int ButtonB = 2;
    constexpr int ButtonC = 3;

    class SwipeEventResolverFixture: public QuasarUnitTestFixture {
    public:
        SwipeEventResolverFixture()
            : callbackQueue_(std::make_shared<NamedCallbackQueue>("SwipeEventResolverFixture"))
        {
        }

        void createResolver(SwipeEventChecker& checker, uint8_t maxSwipeLength = 3, std::chrono::milliseconds togglePressedThreshold = 500ms, std::chrono::milliseconds nextPressThreshold = 500ms) {
            auto func = [&checker](Event event, std::vector<int> buttons) {
                std::stringstream ss;
                for (auto b : buttons) {
                    ss << std::to_string(b) << " ";
                }
                YIO_LOG_DEBUG("Resolved event: " << (int)event << " for buttons: " << ss.str());
                UNIT_ASSERT(checker.addEvent(std::make_pair(event, buttons)));
            };

            resolver_ = std::make_unique<SwipeEventResolver>(callbackQueue_, std::move(func), maxSwipeLength, togglePressedThreshold, nextPressThreshold);
        }

        void press(int buttonIdx) {
            callbackQueue_->add([this, buttonIdx]() { resolver_->buttonPressed(buttonIdx); });
        };

        void release(int buttonIdx) {
            callbackQueue_->add([this, buttonIdx]() { resolver_->buttonReleased(buttonIdx); });
        }

        void click(int buttonIdx) {
            press(buttonIdx);
            release(buttonIdx);
        }

    protected:
        std::shared_ptr<NamedCallbackQueue> callbackQueue_;
        std::unique_ptr<SwipeEventResolver> resolver_;
    };

} // namespace

Y_UNIT_TEST_SUITE_F(SwipeEventResolverTest, SwipeEventResolverFixture) {
    Y_UNIT_TEST(testResolver_press) {
        SwipeEventChecker checker({{Event::PRESSED, {ButtonA}}});
        createResolver(checker);

        press(ButtonA);
        checker.waitAllEvents();
    }

    Y_UNIT_TEST(testResolver_release) {
        SwipeEventChecker checker({{Event::PRESSED, {ButtonA}},
                                   {Event::RELEASED, {ButtonA}}});
        createResolver(checker);

        press(ButtonA);
        checker.waitForEvents(1);

        release(ButtonA);
        checker.waitAllEvents();
    }

    Y_UNIT_TEST(testResolver_click) {
        SwipeEventChecker checker({{Event::CLICK, {ButtonA}}});
        createResolver(checker);

        press(ButtonA);
        release(ButtonA);
        checker.waitAllEvents();
    }

    Y_UNIT_TEST(testResolver_swipe) {
        SwipeEventChecker checker({{Event::SWIPE, {ButtonA, ButtonB, ButtonC}}});
        createResolver(checker);

        click(ButtonA);
        click(ButtonB);
        click(ButtonC);
        checker.waitAllEvents();
    }

    Y_UNIT_TEST(testResolver_swipe_maxLength_2) {
        SwipeEventChecker checker({{Event::SWIPE, {ButtonA, ButtonB}},
                                   {Event::CLICK, {ButtonC}}});
        createResolver(checker, 2);

        click(ButtonA);
        click(ButtonB);
        click(ButtonC);
        checker.waitAllEvents();
    }

    Y_UNIT_TEST(testResolver_swipe_notReleased) {
        SwipeEventChecker checker({{Event::SWIPE, {ButtonA, ButtonB, ButtonC}},
                                   {Event::CLICK, {ButtonA}}});
        createResolver(checker);

        click(ButtonA);
        click(ButtonB);
        press(ButtonC);
        checker.waitForEvents(1);

        click(ButtonB);   // this should be ignored
        release(ButtonC); // this should not create RELEASED event
        click(ButtonA);

        checker.waitAllEvents();
    }

    Y_UNIT_TEST(testResolver_buttonPressedTwice) {
        SwipeEventChecker checker({{Event::CLICK, {ButtonA}},
                                   {Event::PRESSED, {ButtonA}}});
        createResolver(checker);

        click(ButtonA);
        press(ButtonA);
        checker.waitAllEvents();
    }

    Y_UNIT_TEST(testResolver_buttonPressedTwice_whileSwipe) {
        SwipeEventChecker checker({{Event::SWIPE, {ButtonB, ButtonA}},
                                   {Event::CLICK, {ButtonA}}});
        createResolver(checker);

        click(ButtonB);
        click(ButtonA);
        click(ButtonA);
        checker.waitAllEvents();
    }

    Y_UNIT_TEST(testResolver_manyEvents) {
        SwipeEventChecker checker({{Event::SWIPE, {ButtonA, ButtonB}},
                                   {Event::CLICK, {ButtonC}},
                                   {Event::PRESSED, {ButtonB}},
                                   {Event::RELEASED, {ButtonB}},
                                   {Event::SWIPE, {ButtonC, ButtonA}}});
        createResolver(checker);

        click(ButtonA);
        click(ButtonB);
        checker.waitForEvents(1);

        click(ButtonC);
        checker.waitForEvents(2);

        press(ButtonB);
        checker.waitForEvents(3);

        release(ButtonB);
        checker.waitForEvents(4);

        click(ButtonC);
        click(ButtonA);
        checker.waitAllEvents();
    }

    Y_UNIT_TEST(testResolver_manyEvents_ignoreExtraInput) {
        SwipeEventChecker checker({{Event::SWIPE, {ButtonA, ButtonB}},
                                   {Event::CLICK, {ButtonC}},
                                   {Event::PRESSED, {ButtonB}},
                                   {Event::RELEASED, {ButtonB}},
                                   {Event::SWIPE, {ButtonC, ButtonA}}});
        createResolver(checker);

        release(ButtonC); // should be ignored

        click(ButtonA);
        click(ButtonB);
        checker.waitForEvents(1);

        click(ButtonC);
        checker.waitForEvents(2);

        press(ButtonB);
        release(ButtonC); // should be ignored
        checker.waitForEvents(3);

        click(ButtonA); // should be ignored
        release(ButtonB);
        checker.waitForEvents(4);

        click(ButtonC);
        click(ButtonA);
        checker.waitAllEvents();
    }
}
