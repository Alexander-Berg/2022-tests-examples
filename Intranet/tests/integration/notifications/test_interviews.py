import itertools
import pytest


from intranet.femida.src.interviews.choices import (
    INTERVIEW_TYPES,
    INTERVIEW_RESOLUTIONS,
    INTERVIEW_ROUND_STATUSES,
)
from intranet.femida.src.notifications import interviews as n

from intranet.femida.tests import factories as f
from intranet.femida.tests.utils import get_mocked_event, assert_not_raises


interview_types = (
    INTERVIEW_TYPES.hr_screening,
    INTERVIEW_TYPES.regular,
)

notification_classes = (
    n.InterviewCreatedInterviewerNotification,
    n.InterviewCancelledNotification,
)


@pytest.mark.parametrize(
    'interview_type, notification_class',
    itertools.product(interview_types, notification_classes),
)
def test_interview_created_and_cancelled(interview_type, notification_class):
    instance = f.create_interview(
        type=interview_type,
        interviewer=f.UserFactory(),
        cancel_reason='Причина отмены',
    )
    initiator = f.create_user()
    instance.candidate.responsibles.add(initiator)

    notification = notification_class(instance, initiator)
    notification.send()


def test_interview_created_team_notification():
    application = f.create_application()
    instance = f.create_interview(
        type=INTERVIEW_TYPES.regular,
        interviewer=f.UserFactory(),
        application=application,
    )
    initiator = f.create_user()
    instance.candidate.responsibles.add(initiator)

    notification = n.InterviewCreatedTeamNotification(
        instance=instance,
        initiator=initiator,
        vacancy=application.vacancy,
        event=get_mocked_event(),
    )
    notification.send()


def test_interview_reassigned():
    old_interviewer = f.UserFactory()
    instance = f.create_interview(interviewer=f.UserFactory())
    initiator = instance.candidate.responsibles.first()

    notification = n.InterviewReassignedNotification(
        instance=instance,
        initiator=initiator,
        old_interviewer=old_interviewer,
        reason='Причина переназначения',
    )
    notification.send()


@pytest.mark.parametrize('interview_type', (
    INTERVIEW_TYPES.hr_screening,
    INTERVIEW_TYPES.regular,
))
def test_interview_finished(interview_type):
    instance = f.create_interview(
        type=interview_type,
        interviewer=f.UserFactory(),
        finished_by=f.UserFactory(),
        grade=0,
        resolution=INTERVIEW_RESOLUTIONS.nohire,
    )
    notification = n.InterviewFinishedNotification(instance, instance.finished_by)
    notification.send()


@pytest.mark.parametrize('status', (
    INTERVIEW_ROUND_STATUSES.planned,
    INTERVIEW_ROUND_STATUSES.cancelled,
))
def test_interview_round_planning_finished(status):
    instance = f.InterviewRoundFactory(status=status)
    interview = instance.interviews.create(
        created_by=instance.created_by,
        interviewer=f.UserFactory(),
    )
    notification = n.InterviewRoundPlanningFinishedNotification(instance, interviews=[interview])
    with assert_not_raises():
        notification.send()


def test_interview_estimated():
    instance = f.create_interview(
        type=INTERVIEW_TYPES.regular,
        interviewer=f.UserFactory(),
        grade=3,
    )
    notification = n.InterviewEstimatedNotification(instance)
    with assert_not_raises():
        notification.send()
