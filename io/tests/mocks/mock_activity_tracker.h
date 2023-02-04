#pragma once

#include <yandex_io/libs/activity_tracker/activity_tracker.h>

#include <yandex_io/protos/quasar_proto.pb.h>

#include <library/cpp/testing/gmock_in_unittest/gmock.h>

namespace YandexIO {

    class MockActivityTracker: public ActivityTracker {
    public:
        MOCK_METHOD(bool, addActivity, (const IActivityPtr&), (override));
        MOCK_METHOD(bool, removeActivity, (const IActivityPtr&), (override));
        MOCK_METHOD(std::optional<quasar::proto::AudioChannel>, getCurrentFocusChannel, (), (const, override));
    };

} // namespace YandexIO
