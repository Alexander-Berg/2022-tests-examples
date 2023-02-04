from intranet.femida.src.notifications.base import (
    R,
    FetchingNotificationBase,
)


emails = ['%s@email.com' % i for i in range(5)]


def test_receivers_addition():
    r1 = R(lambda x: [emails[0], emails[1]])
    r2 = R([
        lambda x: [emails[0], emails[2]],
        lambda x: [emails[3], emails[2]],
    ])
    r3 = R()

    class ReceiversFetchingNotification(FetchingNotificationBase):
        receivers = r1 + r2 + r3

    n = ReceiversFetchingNotification(None)
    assert n.fetch_receivers() == set(emails[:4])


def test_receivers_subtraction():
    r1 = R(lambda x: [emails[0], emails[1]])
    r2 = R([
        lambda x: [emails[0], emails[2]],
        lambda x: [emails[3], emails[2]],
    ])
    r3 = R(invert_fetchers=lambda x: [emails[2]])
    r4 = R()

    class ReceiversFetchingNotification(FetchingNotificationBase):
        receivers = r2 - r1 + r3 - r4

    n = ReceiversFetchingNotification(None)
    assert n.fetch_receivers() == {emails[3]}


def test_receivers_complex():
    r1 = R(lambda x: [emails[0], emails[1]])
    r2 = R([
        lambda x: [emails[0], emails[2]],
        lambda x: [emails[3], emails[2]],
    ])
    r3 = R(invert_fetchers=lambda x: [emails[4]])
    r4 = R(lambda x: [emails[1]])

    class ReceiversFetchingNotification(FetchingNotificationBase):
        receivers = r1 + r2 - r3 - r4

    n = ReceiversFetchingNotification(None)
    assert n.fetch_receivers() == set(emails[:5]) - {emails[1]}
