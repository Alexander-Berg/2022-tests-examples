import pytest

from intranet.femida.src.notifications import offers as n

from intranet.femida.tests import factories as f
from intranet.femida.tests.utils import assert_not_raises


@pytest.mark.parametrize('notification_class', (
    n.OfferSentForApprovalNotification,
    n.OfferSentNotification,
    n.OfferDeletedNotification,
    n.OfferRejectedNotification,
))
def test_offer_notifications(notification_class):
    instance = f.create_offer()
    initiator = instance.vacancy.main_recruiter
    with assert_not_raises():
        notification = notification_class(instance, initiator)
        notification.send()


@pytest.mark.parametrize('notification_class', (
    n.OfferAcceptedNotification,
    n.OfferHRSurveyRecruiterNotification,
    n.OfferHRSurveyHiringManagerNotification,
))
def test_offer_notifications_no_initiator(notification_class):
    instance = f.create_offer()
    with assert_not_raises():
        notification = notification_class(instance)
        notification.send()


# TODO: написать тест для send_offer_to_candidate
