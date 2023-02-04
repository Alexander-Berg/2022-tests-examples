import datetime
import pytest

from unittest.mock import patch

from watcher.config import settings
from watcher.enums import EventType, EventSource, EventState
from watcher.db import Event
from watcher.tasks.sync import (
    process_changes_from_logbroker,
    notify_staff_duty,
)
from watcher.logic.timezone import now


def test_process_changes_from_logbroker(scope_session):
    modified_at = now() - datetime.timedelta(minutes=30)
    table_name = 'services_servicemember'

    return_value = [
        (table_name,
         'update',
         {
             'modified_at': str(modified_at),
             'id': 292833,
         },
         {
             'keynames': ['id'],
             'keytypes': ['bigint'],
             'keyvalues': [292833],
         })
    ]
    with patch('watcher.logic.logbroker.get_new_data_from_topic', return_value=return_value) as data_patch:
        process_changes_from_logbroker.__call__()
        data_patch.assert_called_once()

    events: Event = scope_session.query(Event).all()
    assert len(events) == 1
    assert events[0].table == table_name
    assert events[0].kind == 'update'
    assert events[0].obj_id == 292833
    assert events[0].source == EventSource.logbroker
    assert events[0].type == EventType.db_event
    assert events[0].state == EventState.new
    assert events[0].remote_modified_at == modified_at


@pytest.mark.parametrize('is_exportable', (True, False))
@pytest.mark.parametrize('notify_staff', (True, False))
def test_notify_staff_duty(service_factory, is_exportable, notify_staff):
    current = settings.NOTIFY_STAFF
    settings.NOTIFY_STAFF = notify_staff
    service = service_factory(is_exportable=is_exportable)
    with patch('watcher.tasks.sync.staff_client') as mock_client:
        notify_staff_duty(service_id=service.id)

    if is_exportable and notify_staff:
        mock_client.notify_duty.assert_called_once_with(
            service_id=service.id,
            service_slug=service.slug,
        )
    else:
        mock_client.notify_duty.assert_not_called()

    settings.NOTIFY_STAFF = current
