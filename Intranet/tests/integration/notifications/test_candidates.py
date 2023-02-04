import pytest

from intranet.femida.src.interviews.models import Interview
from intranet.femida.src.notifications import candidates as notifications
from intranet.femida.src.vacancies.choices import VACANCY_STATUSES

from intranet.femida.tests import factories as f
from intranet.femida.tests.utils import assert_not_raises


@pytest.mark.parametrize('has_initiator', (True, False))
@pytest.mark.parametrize('notification_class', (
    notifications.CandidateCreatedNotification,
    notifications.CandidateUpdatedNotification,
    notifications.CandidateOpenedNotification,
    notifications.CandidateClosedNotification,
))
def test_candidate_base_notifications(has_initiator, notification_class):
    instance = f.create_candidate_with_consideration()
    initiator = instance.responsibles.first() if has_initiator else None

    notification = notification_class(instance, initiator)
    notification.send()


def test_candidate_completed():
    instance = f.create_candidate_with_consideration()

    interviews = f.InterviewFactory.create_batch(3, candidate=instance)
    notification = notifications.CandidateCompletedNotification(instance, interviews=interviews)
    notification.send()


def test_candidate_proposed():
    instance = f.create_heavy_candidate()
    applications = [
        f.create_heavy_application(
            candidate=instance,
            consideration=instance.considerations.first()
        )
        for _ in range(3)
    ]
    initiator = instance.responsibles.first()
    interviews = [
        f.InterviewFactory(
            candidate=instance,
            application=a,
            consideration=a.consideration,
            grade=3,
        )
        for a in applications
    ]

    notification = notifications.CandidateProposedNotification(
        instance=instance,
        initiator=initiator,
        applications=applications,
        pro_level_min=3,
        pro_level_max=4,
        professions=f.ProfessionFactory.create_batch(3),
        interviews=Interview.unsafe.filter(id__in=[i.id for i in interviews]),
        comment='Комментарий',
    )
    notification.send()


def test_submission_seo_check():
    instance = f.create_submission()
    instance.form.vacancies.add(f.create_heavy_vacancy(status=VACANCY_STATUSES.in_progress))
    notification = notifications.SubmissionSeoCheckNotification(instance)
    notification.send()


@pytest.mark.parametrize('notification_class', (
    notifications.VerificationSubmittedNotification,
    notifications.VerificationWarningNotification,
    notifications.VerificationSuccessNotification,
    notifications.VerificationFailureNotification,
))
def test_verification_base_notifications(notification_class):
    verification = f.VerificationFactory()
    notification = notification_class(verification)
    with assert_not_raises():
        notification.send()
