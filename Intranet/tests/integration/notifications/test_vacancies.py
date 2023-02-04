import pytest

from intranet.femida.src.notifications import vacancies as notifications
from intranet.femida.src.vacancies.serializers import VacancyDiffEstimationSerializer

from intranet.femida.tests import factories as f


def test_vacancy_created():
    instance = f.create_heavy_vacancy()

    notification = notifications.VacancyCreatedNotification(instance)
    notification.send()


def test_vacancy_updated():
    instance = f.create_heavy_vacancy()
    initiator = instance.main_recruiter

    old_data = VacancyDiffEstimationSerializer(instance).data
    f.VacancyCityFactory.create_batch(3, vacancy=instance)
    new_data = VacancyDiffEstimationSerializer(instance).data
    diff = VacancyDiffEstimationSerializer.get_diff(old_data, new_data)

    notification = notifications.VacancyUpdatedNotification(instance, initiator, diff=diff)
    notification.send()


@pytest.mark.parametrize('notification_class', (
    notifications.VacancyApprovedNotification,
    notifications.VacancyClosedNotification,
    notifications.VacancyResumedNotification,
    notifications.VacancySuspendedNotification,
))
def test_vacancy_status_changes(notification_class):
    instance = f.create_heavy_vacancy()
    initiator = instance.main_recruiter

    notification = notification_class(instance, initiator)
    notification.send()


# TODO: написать тест для notify_about_unused_submission_form
