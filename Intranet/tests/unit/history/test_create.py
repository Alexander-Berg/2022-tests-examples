import pytest
from mock import patch

from django.core.urlresolvers import reverse
from django.contrib.contenttypes.models import ContentType

from plan.history.models import HistoryRawEntry
from plan.services.models import Service


@pytest.mark.parametrize('api', ('services-api', 'api-v3', 'api-v4'))
def test_create_history(api, client, data_history):
    client.login(data_history.staff.login)

    with patch('plan.services.tasks.register_service'):
        response = client.json.post(
            reverse(f'{api}:service-list'),
            {
                'name': 'Сервис 3',
                'slug': 'slug3',
                'owner': data_history.staff.login,
            }
        )
        assert response.status_code == 400
        assert HistoryRawEntry.objects.count() == 0

        valid_data = {
            'name': {'ru': 'Сервис 3', 'en': 'Service 3'},
            'slug': 'slug3',
            'owner': data_history.staff.login,
        }
        response = client.json.post(
            reverse(f'{api}:service-list'),
            valid_data,
        )

        assert response.status_code == 201

    assert HistoryRawEntry.objects.count() == 1

    service = Service.objects.get(slug='slug3')

    history = HistoryRawEntry.objects.get(object_id=service.id)
    assert history.request_data == valid_data
    assert history.endpoint == f'{api}:service-list'
    assert history.staff == data_history.staff
    assert history.content_type == ContentType.objects.get_for_model(Service)
