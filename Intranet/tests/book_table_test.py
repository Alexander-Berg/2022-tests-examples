from datetime import date, timedelta
import json
import pytest

from django.core.urlresolvers import reverse

from staff.map.errors import TableBookConflictType
from staff.map.models import TableBook


def expected_error(error_key: TableBookConflictType, expected_data: dict) -> dict:
    return {
        'errors': {
            'table': [
                {
                    'office': [
                        {
                            'error_key': error_key.value,
                            'data': expected_data,
                        },
                    ],
                },
            ],
        },
    }


@pytest.mark.django_db
def test_book_table_add(superuser_client, book_table_test_data):
    args = [book_table_test_data.TABLES[0].id]
    data = {
        'book': [
            {
                'person': book_table_test_data.STAFF[0].id,
                'date_from': (date.today() - timedelta(5)).isoformat(),
                'date_to': (date.today() + timedelta(5)).isoformat(),
                'description': '',
            },
        ],
    }
    url = reverse('map-occupy_table', args=args)
    response = superuser_client.post(url, json.dumps(data), content_type='application/json')
    result = json.loads(response.content)
    assert len(result['booked']['book_added']) == 1
    assert result['booked']['book_added'][0]['table_id'] == book_table_test_data.TABLES[0].id


@pytest.mark.django_db
def test_book_table_delete(superuser_client, book_table_test_data):
    args = [book_table_test_data.TABLES[0].id]
    data = {
        'book': [
            {
                'person': book_table_test_data.STAFF[0].id,
                'date_from': (date.today() - timedelta(5)).isoformat(),
                'date_to': (date.today() + timedelta(5)).isoformat(),
                'description': '',
            },
        ],
    }
    url = reverse('map-occupy_table', args=args)
    response = superuser_client.post(url, json.dumps(data), content_type='application/json')
    response_data = json.loads(response.content)
    staff_id = response_data['booked']['book_added'][0]['staff_id']

    data = {}
    response = superuser_client.post(url, json.dumps(data), content_type='application/json')
    response_data = json.loads(response.content)

    assert len(response_data['booked']['book_deleted']) == 1
    assert int(response_data['booked']['book_deleted'][0]) == int(staff_id)
    assert TableBook.objects.count() == 0


@pytest.mark.django_db
def test_book_table_bad_date_range(superuser_client, book_table_test_data):
    args = [book_table_test_data.TABLES[0].id]
    data = {
        'book': [
            {
                'person': book_table_test_data.STAFF[0].id,
                'date_from': (date.today() + timedelta(10)).isoformat(),
                'date_to': (date.today() + timedelta(5)).isoformat(),
                'description': '',
            },
        ],
    }
    url = reverse('map-occupy_table', args=args)
    response = superuser_client.post(url, json.dumps(data), content_type='application/json')
    response_data = json.loads(response.content)

    assert response.status_code == 400
    assert response_data == {
        'errors': {
            'book': {
                '0': {
                    '__all__': [
                        {
                            'error_key': 'date-field-invalid',
                        },
                    ],
                },
            },
        },
    }
    assert TableBook.objects.count() == 0


@pytest.mark.django_db
def test_book_table_date_overlap(superuser_client, book_table_test_data):
    args = [book_table_test_data.TABLES[0].id]
    data = {
        'book': [
            {
                'person': book_table_test_data.STAFF[0].id,
                'date_from': (date.today() - timedelta(5)).isoformat(),
                'date_to': (date.today() + timedelta(5)).isoformat(),
                'description': '',
            },
        ],
    }
    url = reverse('map-occupy_table', args=args)
    response = superuser_client.post(url, json.dumps(data), content_type='application/json')

    assert response.status_code == 200
    assert TableBook.objects.count() == 1

    args = [book_table_test_data.TABLES[1].id]
    data = {
        'book': [
            {
                'person': book_table_test_data.STAFF[0].id,
                'date_from': (date.today() - timedelta(2)).isoformat(),
                'date_to': (date.today() + timedelta(8)).isoformat(),
                'description': '',
            },
        ],
    }
    url = reverse('map-occupy_table', args=args)
    response = superuser_client.post(url, json.dumps(data), content_type='application/json')
    response_data = json.loads(response.content)

    assert response.status_code == 400
    assert response_data == expected_error(
        TableBookConflictType.STAFF_BOOKED_ANOTHER,
        {
            'staff_id': book_table_test_data.STAFF[0].id,
            'staff_login': book_table_test_data.STAFF[0].login,
            'overlap_start': (date.today() - timedelta(2)).isoformat(),
            'overlap_finish': (date.today() + timedelta(5)).isoformat(),
            'other_table_id': book_table_test_data.TABLES[0].id,
        },
    )
    assert TableBook.objects.count() == 1


@pytest.mark.django_db
def test_book_table_dates_overlap(superuser_client, book_table_test_data):
    args = [book_table_test_data.TABLES[0].id]
    data = {
        'book': [
            {
                'person': book_table_test_data.STAFF[0].id,
                'date_from': (date.today() - timedelta(5)).isoformat(),
                'date_to': (date.today() + timedelta(5)).isoformat(),
                'description': '',
            },
            {
                'person': book_table_test_data.STAFF[1].id,
                'date_from': (date.today() - timedelta(2)).isoformat(),
                'date_to': (date.today() + timedelta(10)).isoformat(),
                'description': '',
            },
        ],
    }
    url = reverse('map-occupy_table', args=args)
    response = superuser_client.post(url, json.dumps(data), content_type='application/json')
    response_data = json.loads(response.content)

    assert response.status_code == 400
    assert response_data == expected_error(
        TableBookConflictType.BOOK_OVERLAP,
        {
            'older_staff_id': book_table_test_data.STAFF[0].id,
            'newer_staff_id': book_table_test_data.STAFF[1].id,
            'older_staff_login': book_table_test_data.STAFF[0].login,
            'newer_staff_login': book_table_test_data.STAFF[1].login,
            'overlap_start': (date.today() - timedelta(2)).isoformat(),
            'overlap_finish': (date.today() + timedelta(5)).isoformat(),
        }
    )
    assert TableBook.objects.count() == 0


@pytest.mark.django_db
def test_book_table_dates_overlap_one_person(superuser_client, book_table_test_data):
    args = [book_table_test_data.TABLES[0].id]
    data = {
        'book': [
            {
                'person': book_table_test_data.STAFF[0].id,
                'date_from': (date.today() - timedelta(5)).isoformat(),
                'date_to': (date.today() + timedelta(5)).isoformat(),
                'description': '',
            },
            {
                'person': book_table_test_data.STAFF[0].id,
                'date_from': (date.today() - timedelta(2)).isoformat(),
                'date_to': (date.today() + timedelta(10)).isoformat(),
                'description': '',
            },
        ],
    }
    url = reverse('map-occupy_table', args=args)
    response = superuser_client.post(url, json.dumps(data), content_type='application/json')
    response_data = json.loads(response.content)

    assert response.status_code == 400
    assert response_data == expected_error(
        TableBookConflictType.BOOK_OVERLAP_ONE_PERSON,
        {
            'staff_id': book_table_test_data.STAFF[0].id,
            'staff_login': book_table_test_data.STAFF[0].login,
            'overlap_start': (date.today() - timedelta(2)).isoformat(),
            'overlap_finish': (date.today() + timedelta(5)).isoformat(),
        }
    )
    assert TableBook.objects.count() == 0


@pytest.mark.django_db
def test_book_dont_created_if_bad_data(superuser_client, book_table_test_data):
    args = [book_table_test_data.TABLES[0].id]
    data = {
        'book': [
            {
                'person': book_table_test_data.STAFF[0].id,
                'date_from': (date.today() - timedelta(5)).isoformat(),
                'date_to': (date.today() + timedelta(5)).isoformat(),
                'description': '',
            },
            {
                'person': book_table_test_data.STAFF[1].id,
                'date_from': (date.today() + timedelta(10)).isoformat(),
                'date_to': (date.today() + timedelta(20)).isoformat(),
                'description': '',
            },
            {
                'person': book_table_test_data.STAFF[2].id,
                'date_from': (date.today() + timedelta(15)).isoformat(),
                'date_to': (date.today() + timedelta(14)).isoformat(),
                'description': '',
            },
        ],
    }

    url = reverse('map-occupy_table', args=args)
    response = superuser_client.post(url, json.dumps(data), content_type='application/json')

    assert response.status_code == 400
    assert TableBook.objects.count() == 0


@pytest.mark.django_db
def test_book_table_two_weeks_exceeded_error(superuser_client, book_table_test_data, settings):
    args = [book_table_test_data.TABLES[0].id]
    data = {
        'book': [
            {
                'person': book_table_test_data.STAFF[0].id,
                'date_from': date.today().isoformat(),
                'date_to': (date.today() + timedelta(settings.MAP_TABLE_MAX_BOOK_PERIOD_FROM_TODAY + 1)).isoformat(),
                'description': '',
            },
        ],
    }
    url = reverse('map-occupy_table', args=args)
    response = superuser_client.post(url, json.dumps(data), content_type='application/json')
    response_data = json.loads(response.content)
    assert response.status_code == 400
    assert response_data == {
        'errors': {
            'book': {
                '0': {
                    '__all__': [
                        {
                            'error_key': 'two-weeks-exceeded',
                        },
                    ],
                },
            },
        },
    }
