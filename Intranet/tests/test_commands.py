import pytest

from unittest.mock import patch, MagicMock
from intranet.yasanta.backend.gifts.models import Event, SantaEntry
from intranet.yasanta.backend.gifts.management.commands.common import add_entries_by_staff_api_query


@patch(
    'intranet.yasanta.backend.gifts.management.commands.common.get_people',
    MagicMock(return_value=['login2', 'login3']),
)
@pytest.mark.django_db
def test_add_entries_by_staff_api_query():
    event_code = 'event'
    event = Event.objects.create(code=event_code)
    SantaEntry.objects.bulk_create([
        SantaEntry(
            event=event,
            lucky_login=login,
            collector_login=login,
        )
        for login in ('login1', 'login2')
    ])

    add_entries_by_staff_api_query(event_code=event_code, query='')

    logins = list(SantaEntry.objects.filter(event=event).values_list('lucky_login', flat=True))
    assert len(logins) == 3
    assert set(logins) == {'login1', 'login2', 'login3'}
