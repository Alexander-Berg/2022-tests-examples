import pytest
from datetime import datetime

from intranet.hrdb_ext.src.amo.core.search import get_requests_statuses
from intranet.hrdb_ext.tests.amo.factories import TicketRequestFactory


@pytest.mark.django_db
def test_requests_search():
    _ = [
        TicketRequestFactory(created_at=datetime(2022, 1, 1)),
        TicketRequestFactory(created_at=datetime(2022, 1, 2)),
        TicketRequestFactory(created_at=datetime(2022, 1, 3)),
    ]

    found = get_requests_statuses(created_after='2022-01-04')
    assert found.count() == 0

    found = get_requests_statuses(created_after=datetime(2022, 1, 3).date())
    assert found.count() == 1
    assert found.first().created_at.date() == datetime(2022, 1, 3).date()

    found = get_requests_statuses(created_after='2022-01-01')
    assert found.count() == 3

    serialized = get_requests_statuses(created_after='2022-01-01', serialize=True)
    assert len(serialized) == 3
