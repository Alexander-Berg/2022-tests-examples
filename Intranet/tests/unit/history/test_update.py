import pytest

from django.core.urlresolvers import reverse
from django.contrib.contenttypes.models import ContentType

from plan.history.models import HistoryRawEntry
from plan.services.models import Service
from common import factories


@pytest.mark.parametrize('api', ('services-api', 'api-v3', 'api-v4'))
def test_update_history(api, client, data_history):
    client.login(data_history.staff.login)
    request_data = {'description': {'ru': 'smth'}}
    response = client.json.patch(
        reverse(f'{api}:service-detail', args=(data_history.service.id,)),
        request_data
    )

    assert response.status_code == 200

    assert HistoryRawEntry.objects.count() == 1

    history = HistoryRawEntry.objects.get(object_id=data_history.service.id)
    assert history.request_data == request_data
    assert history.endpoint == f'{api}:service-detail'
    assert history.staff == data_history.staff
    assert history.content_type == ContentType.objects.get_for_model(Service)


@pytest.mark.parametrize('api', ('services-api', 'api-v3', 'api-v4'))
def test_update_history_tags(api, client, data_history):
    client.login(data_history.staff.login)
    tag = factories.ServiceTagFactory()

    request_data = {'tags': [tag.id]}
    response = client.json.patch(
        reverse(f'{api}:service-detail', args=(data_history.service.id,)),
        request_data
    )

    assert response.status_code == 200

    assert HistoryRawEntry.objects.count() == 1

    history = HistoryRawEntry.objects.get(object_id=data_history.service.id)
    assert history.request_data == request_data
    assert history.endpoint == f'{api}:service-detail'
    assert history.staff == data_history.staff
    assert history.content_type == ContentType.objects.get_for_model(Service)
