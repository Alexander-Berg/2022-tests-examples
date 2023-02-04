import mock
from unittest.mock import patch

from watcher.tasks.schedule import sync_important_schedules


def test_sync_important_schedules(scope_session, schedule_factory, service_factory):
    service = service_factory()
    schedule = schedule_factory(service=service)
    schedule_one = schedule_factory(service=service)
    schedule_two = schedule_factory(service=service, is_important=True)

    class FakeYQLRequest:
        def __init__(self):
            self.status = 'COMPLETED'
            self.run = mock.MagicMock()
            table_mock = mock.MagicMock()
            table_mock.rows = (
                (service.slug, schedule.slug),
            )
            self.get_results = mock.MagicMock(return_value=[table_mock])

    with patch('yql.api.v1.client.YqlClient.query', return_value=FakeYQLRequest()):
        sync_important_schedules()
    scope_session.refresh(schedule)
    assert schedule.is_important is True
    scope_session.refresh(schedule_one)
    assert schedule_one.is_important is False
    scope_session.refresh(schedule_two)
    assert schedule_two.is_important is False
