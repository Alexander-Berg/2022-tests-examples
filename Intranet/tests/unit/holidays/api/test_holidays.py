from django.urls import reverse
from plan.holidays.models import Holiday


def test_get_holidays(client):
    url = reverse('api-frontend:holidays-list')
    response = client.json.get(url, data={
        'date_from': '2020-01-01',
        'date_to': '2020-02-01',
    })
    assert response.status_code == 200
    assert response.json() == {
        'results': [
            {'end': '2020-01-05', 'start': '2020-01-04', 'interval_type': 'weekend', },
            {'end': '2020-01-12', 'start': '2020-01-11', 'interval_type': 'weekend', },
            {'end': '2020-01-19', 'start': '2020-01-18', 'interval_type': 'weekend', },
            {'end': '2020-01-26', 'start': '2020-01-25', 'interval_type': 'weekend', },
            {'end': '2020-02-01', 'start': '2020-02-01', 'interval_type': 'weekend', },
        ],
    }

    response = client.json.get(url, data={
        'date_from': '2020-01-04',
        'date_to': '2020-01-4',
    })
    assert response.status_code == 200
    assert response.json() == {
        'results': [
            {'end': '2020-01-04', 'start': '2020-01-04', 'interval_type': 'weekend', },
        ],
    }

    response = client.json.get(url, data={
        'date_from': '2020-01-01',
        'date_to': '2020-01-01',
    })
    assert response.status_code == 200
    assert response.json() == {
        'results': [],
    }

    response = client.json.get(url, data={
        'date_from': '2020-01',
        'date_to': '2020-01-01',
    })
    assert response.status_code == 400
    assert response.json() == {'date_from': ['Date has wrong format. Use one of these formats instead: YYYY-MM-DD.']}

    response = client.json.get(url, data={
        'date_fromo': '2020-01-01',
        'date_to': '2020-01-01',
    })
    assert response.status_code == 400
    assert response.json() == {'date_from': ['This field is required.']}

    response = client.json.get(url, data={
        'date_from': '2020-01-02',
        'date_to': '2020-01-01',
    })
    assert response.status_code == 400
    assert response.json() == {'non_field_errors': ['date_from must precede date_to']}


def test_get_holidays_by_type(client):
    url = reverse('api-frontend:holidays-list')
    Holiday.objects.create(date='2020-01-17', is_holiday=True)
    Holiday.objects.create(date='2020-01-20', is_holiday=True)
    Holiday.objects.create(date='2020-01-21', is_holiday=True)
    Holiday.objects.create(date='2020-01-22', is_holiday=True)
    Holiday.objects.create(date='2020-01-27', is_holiday=False)

    response = client.json.get(url, data={
        'date_from': '2020-01-11',
        'date_to': '2020-01-27',
    })
    assert response.status_code == 200
    assert response.json() == {
        'results': [
            {'end': '2020-01-12', 'start': '2020-01-11', 'interval_type': 'weekend', },
            {'end': '2020-01-17', 'start': '2020-01-17', 'interval_type': 'holiday', },
            {'end': '2020-01-19', 'start': '2020-01-18', 'interval_type': 'weekend', },
            {'end': '2020-01-22', 'start': '2020-01-20', 'interval_type': 'holiday', },
            {'end': '2020-01-27', 'start': '2020-01-25', 'interval_type': 'weekend', },
        ],
    }
