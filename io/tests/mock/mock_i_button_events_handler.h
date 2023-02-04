#pragma once

#include <yandex_io/modules/buttons/click_handlers/swipe_manager/i_button_events_handler.h>

#include <library/cpp/testing/gmock_in_unittest/gmock.h>

#include <functional>

using ::testing::An;

class MockIButtonEventsHandler: public IButtonEventsHandler {
public:
    MOCK_METHOD(void, buttonPressed, (ButtonIdx buttonIdx), (override));
    MOCK_METHOD(void, buttonReleased, (ButtonIdx buttonIdx), (override));
};
