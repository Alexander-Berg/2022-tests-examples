#pragma once

#include <yandex_io/libs/activity_tracker/interface/i_activity.h>

#include <library/cpp/testing/gmock_in_unittest/gmock.h>

namespace YandexIO {

    class MockIActivity: public IActivity {
    public:
        MOCK_METHOD(std::string, activityName, (), (const, override));
        MOCK_METHOD(void, setBackground, (), (override));
        MOCK_METHOD(void, setForeground, (), (override));
        MOCK_METHOD(quasar::proto::AudioChannel, getAudioChannel, (), (const, override));
        MOCK_METHOD(bool, isLocal, (), (const, override));
    };

} // namespace YandexIO
