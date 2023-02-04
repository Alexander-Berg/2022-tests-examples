#include "mocks/mock_i_activity.h"

#include <yandex_io/libs/activity_tracker/activity_tracker.h>

#include <library/cpp/testing/unittest/registar.h>

using namespace testing;
using namespace YandexIO;

Y_UNIT_TEST_SUITE(ActivityTrackerTest) {
    Y_UNIT_TEST(testAddActivityErrors) {
        ActivityTracker activityTracker;
        ASSERT_FALSE(activityTracker.addActivity(nullptr));
        ASSERT_TRUE(activityTracker.getCurrentFocusChannel() == std::nullopt);

        auto activity = std::make_shared<MockIActivity>();
        EXPECT_CALL(*activity, getAudioChannel()).WillRepeatedly(Return(static_cast<quasar::proto::AudioChannel>(-1)));
        ASSERT_FALSE(activityTracker.addActivity(activity));
        ASSERT_TRUE(activityTracker.getCurrentFocusChannel() == std::nullopt);
    }

    Y_UNIT_TEST(testAddActivityToHigherPriorityChannel) {
        ActivityTracker activityTracker;

        auto activity1 = std::make_shared<MockIActivity>();
        auto activity2 = std::make_shared<MockIActivity>();
        EXPECT_CALL(*activity1, getAudioChannel()).WillRepeatedly(Return(quasar::proto::CONTENT_CHANNEL));
        EXPECT_CALL(*activity2, getAudioChannel()).WillRepeatedly(Return(quasar::proto::DIALOG_CHANNEL));

        EXPECT_CALL(*activity1, isLocal()).WillRepeatedly(Return(true));
        EXPECT_CALL(*activity2, isLocal()).WillRepeatedly(Return(true));

        {
            InSequence sequence;

            EXPECT_CALL(*activity1, setForeground()).Times(1);
            EXPECT_CALL(*activity1, setBackground()).Times(1);
            EXPECT_CALL(*activity2, setForeground()).Times(1);
        }

        ASSERT_TRUE(activityTracker.addActivity(activity1));
        ASSERT_TRUE(activityTracker.getCurrentFocusChannel() == quasar::proto::CONTENT_CHANNEL);
        ASSERT_TRUE(activityTracker.addActivity(activity2));
        ASSERT_TRUE(activityTracker.getCurrentFocusChannel() == quasar::proto::DIALOG_CHANNEL);
    }

    Y_UNIT_TEST(testAddActivityToLowerPriorityChannel) {
        ActivityTracker activityTracker;

        auto activity1 = std::make_shared<MockIActivity>();
        auto activity2 = std::make_shared<MockIActivity>();
        EXPECT_CALL(*activity1, getAudioChannel()).WillRepeatedly(Return(quasar::proto::DIALOG_CHANNEL));
        EXPECT_CALL(*activity2, getAudioChannel()).WillRepeatedly(Return(quasar::proto::CONTENT_CHANNEL));

        EXPECT_CALL(*activity1, isLocal()).WillRepeatedly(Return(true));
        EXPECT_CALL(*activity2, isLocal()).WillRepeatedly(Return(true));

        {
            InSequence sequence;

            EXPECT_CALL(*activity1, setForeground()).Times(1);
            EXPECT_CALL(*activity1, setBackground()).Times(0);
            EXPECT_CALL(*activity2, setForeground()).Times(0);
        }

        ASSERT_TRUE(activityTracker.addActivity(activity1));
        ASSERT_TRUE(activityTracker.getCurrentFocusChannel() == quasar::proto::DIALOG_CHANNEL);
        ASSERT_TRUE(activityTracker.addActivity(activity2));
        ASSERT_TRUE(activityTracker.getCurrentFocusChannel() == quasar::proto::DIALOG_CHANNEL);
    }

    Y_UNIT_TEST(testAddActivityToSamePriorityChannel) {
        ActivityTracker activityTracker;

        auto activity1 = std::make_shared<MockIActivity>();
        auto activity2 = std::make_shared<MockIActivity>();
        EXPECT_CALL(*activity1, getAudioChannel()).WillRepeatedly(Return(quasar::proto::DIALOG_CHANNEL));
        EXPECT_CALL(*activity2, getAudioChannel()).WillRepeatedly(Return(quasar::proto::DIALOG_CHANNEL));

        EXPECT_CALL(*activity1, isLocal()).WillRepeatedly(Return(true));
        EXPECT_CALL(*activity2, isLocal()).WillRepeatedly(Return(true));

        {
            InSequence sequence;

            EXPECT_CALL(*activity1, setForeground()).Times(1);
            EXPECT_CALL(*activity1, setBackground()).Times(1);
            EXPECT_CALL(*activity2, setForeground()).Times(1);
        }

        ASSERT_TRUE(activityTracker.addActivity(activity1));
        ASSERT_TRUE(activityTracker.getCurrentFocusChannel() == quasar::proto::DIALOG_CHANNEL);
        ASSERT_TRUE(activityTracker.addActivity(activity2));
        ASSERT_TRUE(activityTracker.getCurrentFocusChannel() == quasar::proto::DIALOG_CHANNEL);
    }

    Y_UNIT_TEST(testAddActivityToSamePriorityChannelButRemote) {
        ActivityTracker activityTracker;

        auto activity1 = std::make_shared<MockIActivity>();
        auto activity2 = std::make_shared<MockIActivity>();
        EXPECT_CALL(*activity1, getAudioChannel()).WillRepeatedly(Return(quasar::proto::DIALOG_CHANNEL));
        EXPECT_CALL(*activity2, getAudioChannel()).WillRepeatedly(Return(quasar::proto::DIALOG_CHANNEL));

        EXPECT_CALL(*activity1, isLocal()).WillRepeatedly(Return(true));
        EXPECT_CALL(*activity2, isLocal()).WillRepeatedly(Return(false));

        EXPECT_CALL(*activity1, setForeground()).Times(1);

        ASSERT_TRUE(activityTracker.addActivity(activity1));
        ASSERT_TRUE(activityTracker.getCurrentFocusChannel() == quasar::proto::DIALOG_CHANNEL);
        ASSERT_TRUE(activityTracker.addActivity(activity2));
        ASSERT_TRUE(activityTracker.getCurrentFocusChannel() == quasar::proto::DIALOG_CHANNEL);
    }

    Y_UNIT_TEST(testRemoveActivityErrors) {
        ActivityTracker activityTracker;
        ASSERT_FALSE(activityTracker.removeActivity(nullptr));

        auto activity = std::make_shared<MockIActivity>();
        EXPECT_CALL(*activity, getAudioChannel()).WillRepeatedly(Return(static_cast<quasar::proto::AudioChannel>(-1)));
        ASSERT_FALSE(activityTracker.removeActivity(activity));
        ASSERT_TRUE(activityTracker.getCurrentFocusChannel() == std::nullopt);
    }

    Y_UNIT_TEST(testRemoveBackgroundActivityFromSameChannel) {
        ActivityTracker activityTracker;

        auto activity1 = std::make_shared<MockIActivity>();
        auto activity2 = std::make_shared<MockIActivity>();
        EXPECT_CALL(*activity1, getAudioChannel()).WillRepeatedly(Return(quasar::proto::DIALOG_CHANNEL));
        EXPECT_CALL(*activity2, getAudioChannel()).WillRepeatedly(Return(quasar::proto::DIALOG_CHANNEL));

        EXPECT_CALL(*activity1, isLocal()).WillRepeatedly(Return(true));
        EXPECT_CALL(*activity2, isLocal()).WillRepeatedly(Return(true));

        {
            InSequence sequence;

            EXPECT_CALL(*activity1, setForeground()).Times(1);
            EXPECT_CALL(*activity1, setBackground()).Times(1);
            EXPECT_CALL(*activity2, setForeground()).Times(1);
            EXPECT_CALL(*activity1, setBackground()).Times(1);
        }

        ASSERT_TRUE(activityTracker.addActivity(activity1));
        ASSERT_TRUE(activityTracker.getCurrentFocusChannel() == quasar::proto::DIALOG_CHANNEL);
        ASSERT_TRUE(activityTracker.addActivity(activity2));
        ASSERT_TRUE(activityTracker.getCurrentFocusChannel() == quasar::proto::DIALOG_CHANNEL);
        ASSERT_TRUE(activityTracker.removeActivity(activity1));
        ASSERT_TRUE(activityTracker.getCurrentFocusChannel() == quasar::proto::DIALOG_CHANNEL);
    }

    Y_UNIT_TEST(testRemoveForegroundActivityFromSameChannel) {
        ActivityTracker activityTracker;

        auto activity1 = std::make_shared<MockIActivity>();
        auto activity2 = std::make_shared<MockIActivity>();
        EXPECT_CALL(*activity1, getAudioChannel()).WillRepeatedly(Return(quasar::proto::DIALOG_CHANNEL));
        EXPECT_CALL(*activity2, getAudioChannel()).WillRepeatedly(Return(quasar::proto::DIALOG_CHANNEL));

        EXPECT_CALL(*activity1, isLocal()).WillRepeatedly(Return(true));
        EXPECT_CALL(*activity2, isLocal()).WillRepeatedly(Return(true));

        {
            InSequence sequence;

            EXPECT_CALL(*activity1, setForeground()).Times(1);
            EXPECT_CALL(*activity1, setBackground()).Times(1);
            EXPECT_CALL(*activity2, setForeground()).Times(1);
            EXPECT_CALL(*activity2, setBackground()).Times(1);
            EXPECT_CALL(*activity1, setForeground()).Times(1);
        }

        ASSERT_TRUE(activityTracker.addActivity(activity1));
        ASSERT_TRUE(activityTracker.getCurrentFocusChannel() == quasar::proto::DIALOG_CHANNEL);
        ASSERT_TRUE(activityTracker.addActivity(activity2));
        ASSERT_TRUE(activityTracker.getCurrentFocusChannel() == quasar::proto::DIALOG_CHANNEL);
        ASSERT_TRUE(activityTracker.removeActivity(activity2));
        ASSERT_TRUE(activityTracker.getCurrentFocusChannel() == quasar::proto::DIALOG_CHANNEL);
    }

    Y_UNIT_TEST(testRemoveForegroundActivityFromHigherChannel) {
        ActivityTracker activityTracker;

        auto activity1 = std::make_shared<MockIActivity>();
        auto activity2 = std::make_shared<MockIActivity>();
        EXPECT_CALL(*activity1, getAudioChannel()).WillRepeatedly(Return(quasar::proto::CONTENT_CHANNEL));
        EXPECT_CALL(*activity2, getAudioChannel()).WillRepeatedly(Return(quasar::proto::DIALOG_CHANNEL));

        EXPECT_CALL(*activity1, isLocal()).WillRepeatedly(Return(true));
        EXPECT_CALL(*activity2, isLocal()).WillRepeatedly(Return(true));

        {
            InSequence sequence;

            EXPECT_CALL(*activity1, setForeground()).Times(1);
            EXPECT_CALL(*activity1, setBackground()).Times(1);
            EXPECT_CALL(*activity2, setForeground()).Times(1);
            EXPECT_CALL(*activity2, setBackground()).Times(1);
            EXPECT_CALL(*activity1, setForeground()).Times(1);
        }

        ASSERT_TRUE(activityTracker.addActivity(activity1));
        ASSERT_TRUE(activityTracker.getCurrentFocusChannel() == quasar::proto::CONTENT_CHANNEL);
        ASSERT_TRUE(activityTracker.addActivity(activity2));
        ASSERT_TRUE(activityTracker.getCurrentFocusChannel() == quasar::proto::DIALOG_CHANNEL);
        ASSERT_TRUE(activityTracker.removeActivity(activity2));
        ASSERT_TRUE(activityTracker.getCurrentFocusChannel() == quasar::proto::CONTENT_CHANNEL);
    }

    Y_UNIT_TEST(testRemoveBackgroundActivityFromLowerChannel) {
        ActivityTracker activityTracker;

        auto activity1 = std::make_shared<MockIActivity>();
        auto activity2 = std::make_shared<MockIActivity>();
        EXPECT_CALL(*activity1, getAudioChannel()).WillRepeatedly(Return(quasar::proto::CONTENT_CHANNEL));
        EXPECT_CALL(*activity2, getAudioChannel()).WillRepeatedly(Return(quasar::proto::DIALOG_CHANNEL));

        EXPECT_CALL(*activity1, isLocal()).WillRepeatedly(Return(true));
        EXPECT_CALL(*activity2, isLocal()).WillRepeatedly(Return(true));

        {
            InSequence sequence;

            EXPECT_CALL(*activity1, setForeground()).Times(1);
            EXPECT_CALL(*activity1, setBackground()).Times(1);
            EXPECT_CALL(*activity2, setForeground()).Times(1);
            EXPECT_CALL(*activity1, setBackground()).Times(1);
        }

        ASSERT_TRUE(activityTracker.addActivity(activity1));
        ASSERT_TRUE(activityTracker.getCurrentFocusChannel() == quasar::proto::CONTENT_CHANNEL);
        ASSERT_TRUE(activityTracker.addActivity(activity2));
        ASSERT_TRUE(activityTracker.getCurrentFocusChannel() == quasar::proto::DIALOG_CHANNEL);
        ASSERT_TRUE(activityTracker.removeActivity(activity1));
        ASSERT_TRUE(activityTracker.getCurrentFocusChannel() == quasar::proto::DIALOG_CHANNEL);
    }

    Y_UNIT_TEST(testRemoveBackgroundActivityFromLowerChannel2) {
        ActivityTracker activityTracker;

        auto activity1 = std::make_shared<MockIActivity>();
        auto activity2 = std::make_shared<MockIActivity>();
        auto activity3 = std::make_shared<MockIActivity>();
        EXPECT_CALL(*activity1, getAudioChannel()).WillRepeatedly(Return(quasar::proto::CONTENT_CHANNEL));
        EXPECT_CALL(*activity2, getAudioChannel()).WillRepeatedly(Return(quasar::proto::CONTENT_CHANNEL));
        EXPECT_CALL(*activity3, getAudioChannel()).WillRepeatedly(Return(quasar::proto::DIALOG_CHANNEL));

        EXPECT_CALL(*activity1, isLocal()).WillRepeatedly(Return(true));
        EXPECT_CALL(*activity2, isLocal()).WillRepeatedly(Return(true));
        EXPECT_CALL(*activity3, isLocal()).WillRepeatedly(Return(true));

        {
            InSequence sequence;

            EXPECT_CALL(*activity3, setForeground()).Times(1);
            EXPECT_CALL(*activity1, setBackground()).Times(1);
            EXPECT_CALL(*activity1, setForeground()).Times(0);
            EXPECT_CALL(*activity2, setBackground()).Times(0);
            EXPECT_CALL(*activity2, setForeground()).Times(0);
        }

        ASSERT_TRUE(activityTracker.addActivity(activity3));
        ASSERT_TRUE(activityTracker.getCurrentFocusChannel() == quasar::proto::DIALOG_CHANNEL);
        ASSERT_TRUE(activityTracker.addActivity(activity1));
        ASSERT_TRUE(activityTracker.getCurrentFocusChannel() == quasar::proto::DIALOG_CHANNEL);
        ASSERT_TRUE(activityTracker.addActivity(activity2));
        ASSERT_TRUE(activityTracker.getCurrentFocusChannel() == quasar::proto::DIALOG_CHANNEL);
        ASSERT_TRUE(activityTracker.removeActivity(activity1));
        ASSERT_TRUE(activityTracker.getCurrentFocusChannel() == quasar::proto::DIALOG_CHANNEL);
    }

    Y_UNIT_TEST(testActivityLIFOOrder) {
        ActivityTracker activityTracker;

        auto activity1 = std::make_shared<MockIActivity>();
        auto activity2 = std::make_shared<MockIActivity>();
        auto activity3 = std::make_shared<MockIActivity>();
        EXPECT_CALL(*activity1, getAudioChannel()).WillRepeatedly(Return(quasar::proto::DIALOG_CHANNEL));
        EXPECT_CALL(*activity2, getAudioChannel()).WillRepeatedly(Return(quasar::proto::DIALOG_CHANNEL));
        EXPECT_CALL(*activity3, getAudioChannel()).WillRepeatedly(Return(quasar::proto::DIALOG_CHANNEL));

        EXPECT_CALL(*activity1, isLocal()).WillRepeatedly(Return(true));
        EXPECT_CALL(*activity2, isLocal()).WillRepeatedly(Return(true));
        EXPECT_CALL(*activity3, isLocal()).WillRepeatedly(Return(true));
        {
            InSequence sequence;

            EXPECT_CALL(*activity1, setForeground()).Times(1);
            EXPECT_CALL(*activity1, setBackground()).Times(1);
            EXPECT_CALL(*activity2, setForeground()).Times(1);
            EXPECT_CALL(*activity2, setBackground()).Times(1);
            EXPECT_CALL(*activity3, setForeground()).Times(1);
            EXPECT_CALL(*activity3, setBackground()).Times(1);
            EXPECT_CALL(*activity2, setForeground()).Times(1);
            EXPECT_CALL(*activity2, setBackground()).Times(1);
            EXPECT_CALL(*activity1, setForeground()).Times(1);
            EXPECT_CALL(*activity1, setBackground()).Times(1);
        }

        ASSERT_TRUE(activityTracker.addActivity(activity1));
        ASSERT_TRUE(activityTracker.getCurrentFocusChannel() == quasar::proto::DIALOG_CHANNEL);
        ASSERT_TRUE(activityTracker.addActivity(activity2));
        ASSERT_TRUE(activityTracker.getCurrentFocusChannel() == quasar::proto::DIALOG_CHANNEL);
        ASSERT_TRUE(activityTracker.addActivity(activity3));
        ASSERT_TRUE(activityTracker.getCurrentFocusChannel() == quasar::proto::DIALOG_CHANNEL);
        ASSERT_TRUE(activityTracker.removeActivity(activity3));
        ASSERT_TRUE(activityTracker.getCurrentFocusChannel() == quasar::proto::DIALOG_CHANNEL);
        ASSERT_TRUE(activityTracker.removeActivity(activity2));
        ASSERT_TRUE(activityTracker.getCurrentFocusChannel() == quasar::proto::DIALOG_CHANNEL);
        ASSERT_TRUE(activityTracker.removeActivity(activity1));
        ASSERT_TRUE(activityTracker.getCurrentFocusChannel() == std::nullopt);
    }
}
