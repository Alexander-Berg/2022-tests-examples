import pytest
import json
from datetime import date, timedelta

from django.core.urlresolvers import reverse
from django.http import HttpResponse

from staff.lib.testing import (
    TableBookFactory,
    StaffFactory,
    TableFactory,
)


class TableBookTestData:
    user = None
    table = None
    table_books = None


@pytest.fixture
def table_books_data():
    result = TableBookTestData()
    result.user = StaffFactory(login='spock')
    result.tables = [TableFactory() for _ in range(3)]

    result.table_books = [
        TableBookFactory(
            staff=result.user,
            table=result.tables[0],
            date_from=date(2001, 1, 1),
            date_to=date(2001, 1, 5),
        ),
        TableBookFactory(
            staff=result.user,
            table=result.tables[1],
            date_from=date.today() - timedelta(days=10),
            date_to=date.today() + timedelta(days=10),
        ),
        TableBookFactory(
            staff=result.user,
            table=result.tables[2],
            date_from=date.today() + timedelta(days=10),
            date_to=date.today() + timedelta(days=20),
        ),
    ]

    return result


@pytest.mark.django_db
def test_table_book_view(client, table_books_data):
    url = reverse('profile:table_books', kwargs={'login': table_books_data.user.login})
    client.login(user=table_books_data.user)
    response: HttpResponse = client.get(url)
    assert response, response.status == 200

    result = json.loads(response.content)

    assert len(result['target']['table_books']) == 2
    assert [book['table_id'] for book in result['target']['table_books']] == [
        table_books_data.table_books[1].table_id,
        table_books_data.table_books[2].table_id,
    ]
