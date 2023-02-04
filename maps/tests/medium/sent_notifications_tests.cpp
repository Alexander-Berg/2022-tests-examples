#include "helpers.h"

#include <yandex/maps/wiki/social/sent_notification.h>
#include <yandex/maps/wiki/social/sent_notification_gateway.h>

#include <library/cpp/testing/unittest/registar.h>

namespace maps::wiki::social::tests {

Y_UNIT_TEST_SUITE(sent_notifications_suite) {

Y_UNIT_TEST_F(new_sent_notification, DbFixture)
{
    pqxx::work txn(conn);
    SentNotificationGateway gtw(txn);

    TUid UID = 100;

    SentNotification notification;
    notification.uid = UID;
    notification.channel = NotificationChannel::Email;
    notification.type = NotificationType::WelcomeToService;

    gtw.insert(notification);

    UNIT_ASSERT(notification.id != 0);
    UNIT_ASSERT_EQUAL(notification.uid, UID);
    UNIT_ASSERT_EQUAL(notification.channel, NotificationChannel::Email);
    UNIT_ASSERT_EQUAL(notification.type, NotificationType::WelcomeToService);
}

Y_UNIT_TEST_F(load_sent_notification, DbFixture)
{
    pqxx::work txn(conn);
    SentNotificationGateway gtw(txn);

    TUid UID_1 = 1;
    TUid UID_2 = 2;

    // add notifications
    //
    SentNotification notification_1;
        notification_1.uid = UID_1;
        notification_1.channel = NotificationChannel::Email;
        notification_1.type = NotificationType::WelcomeToService;

    SentNotification notification_2;
        notification_2.uid = UID_1;
        notification_2.channel = NotificationChannel::Email;
        notification_2.type = NotificationType::YourEditsArePublished;
        notification_2.sentAt = chrono::TimePoint::clock::now();

    SentNotification notification_3;
        notification_3.uid = UID_2;
        notification_3.channel = NotificationChannel::Bell;
        notification_3.type = NotificationType::WelcomeToService;

    gtw.insert(notification_1);
    gtw.insert(notification_2);
    gtw.insert(notification_3);

    // load notifications
    //
    auto notificationsFiltered = gtw.load(
        table::SentNotificationTbl::uid == UID_1 &&
        table::SentNotificationTbl::type == NotificationType::YourEditsArePublished
    );

    UNIT_ASSERT(notificationsFiltered.size() == 1);

    auto notification = notificationsFiltered.front();
    UNIT_ASSERT_EQUAL(notification.id, notification_2.id);
    UNIT_ASSERT_EQUAL(notification.uid, notification_2.uid);
    UNIT_ASSERT_EQUAL(notification.channel, notification_2.channel);
    UNIT_ASSERT_EQUAL(notification.type, notification_2.type);
    UNIT_ASSERT_EQUAL(notification.args, notification_2.args);
    UNIT_ASSERT_EQUAL(notification.sentAt, notification_2.sentAt);
}

} // sent notifications suite

} // namespace maps::wiki::social::tests
