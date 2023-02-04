#include "mock/mock_i_button_events_handler.h"

#include <yandex_io/modules/buttons/click_handlers/swipe_manager/button_events_regulator.h>

#include <yandex_io/libs/logging/logging.h>
#include <yandex_io/tests/testlib/test_utils.h>
#include <yandex_io/tests/testlib/unittest_helper/unit_test_fixture.h>

using namespace quasar;
using namespace quasar::TestUtils;
using namespace testing;

namespace {
    class ButtonEventsRegulatorFixture: public QuasarUnitTestFixture {
    public:
        ButtonEventsRegulatorFixture()
            : buttonEventsHandlerMock_(std::make_shared<MockIButtonEventsHandler>())
            , regulator(buttonEventsHandlerMock_)
        {
            ON_CALL(*buttonEventsHandlerMock_, buttonPressed(_))
                .WillByDefault([this](int buttonIdx) {
                    UNIT_ASSERT(checker->addEvent(buttonIdx * 10));
                });
            ON_CALL(*buttonEventsHandlerMock_, buttonReleased(_))
                .WillByDefault([this](int buttonIdx) {
                    UNIT_ASSERT(checker->addEvent(buttonIdx));
                });
        }

    protected:
        std::shared_ptr<MockIButtonEventsHandler> buttonEventsHandlerMock_;
        ButtonEventsRegulator regulator;
        std::unique_ptr<EventChecker<int>> checker;
    };

} // namespace

Y_UNIT_TEST_SUITE_F(ButtonEventsRegulatorTest, ButtonEventsRegulatorFixture) {
    Y_UNIT_TEST(testNoIntersections) {
        checker = std::make_unique<EventChecker<int>>(std::vector<int>{10, 1, 20, 2});

        regulator.buttonPressed(1);
        regulator.buttonReleased(1);
        regulator.buttonPressed(2);
        regulator.buttonReleased(2);

        checker->waitAllEvents();
    }

    Y_UNIT_TEST(testButtonPressed_secondButtonPressed) {
        checker = std::make_unique<EventChecker<int>>(std::vector<int>{10, 1, 20, 2});

        regulator.buttonPressed(1);
        regulator.buttonPressed(2);
        regulator.buttonReleased(1);
        regulator.buttonReleased(2);

        checker->waitAllEvents();
    }

    Y_UNIT_TEST(testButtonPressed_secondButtonClick) {
        checker = std::make_unique<EventChecker<int>>(std::vector<int>{10, 1});

        regulator.buttonPressed(1);
        regulator.buttonPressed(2);
        regulator.buttonReleased(2);
        regulator.buttonReleased(1);

        checker->waitAllEvents();
    }
}
