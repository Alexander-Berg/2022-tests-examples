import pytest
from intranet.femida.src.interviews.models import Interview

from intranet.femida.src.interviews.choices import APPLICATION_RESOLUTIONS
from intranet.femida.src.notifications import applications as n
from intranet.femida.src.notifications.candidates import CandidateApplicationBulkCreatedNotification
from intranet.femida.src.vacancies.choices import VACANCY_ROLES

from intranet.femida.tests import factories as f


class FakeMessage:

    def __init__(self, html):
        self.html = html


@pytest.mark.parametrize('notification_class, message', (
    (n.ApplicationResolutionChangedNotification, None),
    (n.ApplicationActivatedNotification, 'Комментарий'),
    (n.ApplicationClosedNotification, 'Комментарий'),
    (n.ApplicationBulkClosedNotification, 'Комментарий'),
    (n.AcceptProposalNotification, 'Комментарий'),
    (n.RejectProposalNotification, 'Комментарий'),
))
def test_application_notifications(notification_class, message):
    message = None if message is None else FakeMessage(message)
    instance = f.create_heavy_application(resolution=APPLICATION_RESOLUTIONS.test_task_sent)
    initiator = instance.vacancy.head

    notification = notification_class(
        instance=instance,
        initiator=initiator,
        message=message,
    )
    notification.send()


def test_application_notifications_bulk_create():
    candidate = f.create_candidate_with_consideration()
    consideration = candidate.considerations.first()
    applications = [
        f.create_heavy_application(
            candidate=candidate,
            consideration=consideration,
        )
        for _ in range(3)
    ]

    # Добавляем одного пользователя на 2 вакансии,
    # чтобы стригерить балковую нотификацию
    f.VacancyMembershipFactory.create(
        vacancy=applications[0].vacancy,
        member=applications[1].vacancy.recruiters[0],
        role=VACANCY_ROLES.recruiter,
    )

    interview = f.create_interview(
        candidate=candidate,
        application=applications[0],
        consideration=consideration,
        state=Interview.STATES.finished,
    )

    notification = CandidateApplicationBulkCreatedNotification(
        instance=candidate,
        initiator=f.UserFactory(),
        applications=applications,
        message=f.MessageFactory(),
        interviews=[interview],
    )
    notification.send()
