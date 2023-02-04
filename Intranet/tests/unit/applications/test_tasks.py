import pytest

from unittest.mock import patch

from intranet.femida.src.applications.tasks import (
    notify_vacancy_applications_closed_task,
    close_forgotten_applications_task,
)
from intranet.femida.src.interviews.choices import APPLICATION_STATUSES, APPLICATION_RESOLUTIONS
from intranet.femida.src.utils.datetime import shifted_now

from intranet.femida.tests import factories as f


pytestmark = pytest.mark.django_db


@patch('intranet.femida.src.communications.controllers.wiki_format', lambda x: x)
@patch('intranet.femida.src.applications.tasks.ApplicationBulkClosedNotification.send')
def test_notify_vacancy_applications_closed(mocked_notification):
    applications_count = 2
    applications = f.ApplicationFactory.create_batch(applications_count)
    notify_vacancy_applications_closed_task([a.id for a in applications])
    assert mocked_notification.call_count == applications_count


@pytest.mark.parametrize('applications_count', (1, 5))
@patch('intranet.femida.src.communications.controllers.wiki_format', lambda x: x)
def test_notify_vacancy_applications_closed_queries(django_assert_num_queries, applications_count):
    """
    Проверяет, что кол-во запросов в БД не растёт при большем кол-ве прет-в.
    Итого, в таске должно быть 9 запросов:
    1. SELECT инициатора
    2. SELECT прет-в
    3. INSERT сообщений
    4-9. Префетчи (6 штук, см. таск)
    """
    applications = f.ApplicationFactory.create_batch(applications_count)
    with django_assert_num_queries(9):
        notify_vacancy_applications_closed_task([a.id for a in applications])


def test_close_forgotten_applications():
    with patch('model_utils.fields.now', return_value=shifted_now(days=-32)):
        forgotten_application = f.ApplicationFactory()
        forgotten_application_with_offer = f.ApplicationFactory()
        f.OfferFactory(application=forgotten_application_with_offer)

    applications = (
        # Забытое прет-во
        forgotten_application,
        # Забытое прет-во с оффером
        forgotten_application_with_offer,
        # Свежее активное прет-во
        f.ApplicationFactory(),
        # Закрытое прет-во
        f.ApplicationFactory(
            status=APPLICATION_STATUSES.closed,
            resolution=APPLICATION_RESOLUTIONS.incorrect,
        ),
    )

    close_forgotten_applications_task()
    closed_applications = []
    for application in applications:
        application.refresh_from_db()
        is_closed = (
            application.status == APPLICATION_STATUSES.closed
            and application.resolution == APPLICATION_RESOLUTIONS.consideration_archived
        )
        if is_closed:
            closed_applications.append(application)

    assert closed_applications == [forgotten_application]
