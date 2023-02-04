import pytest

from unittest.mock import patch

from intranet.femida.src.applications.controllers import (
    close_vacancy_applications,
    bulk_close_applications,
)
from intranet.femida.src.interviews.choices import APPLICATION_STATUSES, APPLICATION_RESOLUTIONS
from intranet.femida.src.interviews.models import Application
from intranet.femida.src.utils.datetime import shifted_now

from intranet.femida.tests import factories as f


pytestmark = pytest.mark.django_db


MODIFICATION_TIME = shifted_now(days=1)


@patch('intranet.femida.src.applications.controllers.notify_vacancy_applications_closed_task.delay')
@patch('intranet.femida.src.applications.controllers.timezone.now', lambda: MODIFICATION_TIME)
def test_close_vacancy_applications(mocked_notify):
    vacancy = f.VacancyFactory()
    active_applications = f.ApplicationFactory.create_batch(
        size=2,
        vacancy=vacancy,
        status=APPLICATION_STATUSES.in_progress,
    )
    closed_applications = f.ApplicationFactory.create_batch(
        size=2,
        vacancy=vacancy,
        status=APPLICATION_STATUSES.closed,
        resolution=APPLICATION_RESOLUTIONS.on_hold,
    )

    resolution = APPLICATION_RESOLUTIONS.incorrect
    close_vacancy_applications(vacancy, resolution=resolution)

    # Проверяем, что закрылись активные прет-ва
    for application in active_applications:
        application.refresh_from_db()
        application.candidate.refresh_from_db()
        assert application.status == APPLICATION_STATUSES.closed
        assert application.resolution == resolution
        assert application.modified == MODIFICATION_TIME
        assert application.candidate.modified < MODIFICATION_TIME

    # Проверяем, что не тронули закрытые прет-ва
    for application in closed_applications:
        application.refresh_from_db()
        assert application.resolution == APPLICATION_RESOLUTIONS.on_hold

    assert mocked_notify.called


@patch('intranet.femida.src.applications.controllers.notify_vacancy_applications_closed_task.delay')
@patch('intranet.femida.src.applications.controllers.timezone.now', lambda: MODIFICATION_TIME)
def test_bulk_close_applications(mocked_notify):
    applications = f.ApplicationFactory.create_batch(
        size=2,
        status=APPLICATION_STATUSES.in_progress,
    )
    application_ids = sorted(a.id for a in applications)
    qs = Application.unsafe.filter(id__in=application_ids).order_by('id')
    resolution = APPLICATION_RESOLUTIONS.incorrect
    bulk_close_applications(qs, resolution, 'reason')

    for application in applications:
        application.refresh_from_db()
        application.candidate.refresh_from_db()
        assert application.status == APPLICATION_STATUSES.closed
        assert application.resolution == resolution
        assert application.modified == MODIFICATION_TIME
        assert application.candidate.modified < MODIFICATION_TIME

    mocked_notify.assert_called_once_with(
        application_ids=application_ids,
        reason='reason',
        initiator_id=None,
    )
