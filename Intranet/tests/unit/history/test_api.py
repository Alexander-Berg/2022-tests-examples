from django.core.urlresolvers import reverse
from plan.services.models import Service
from common import factories
from django.contrib.contenttypes.models import ContentType


def test_get_history(client, data_history):
    client.login(data_history.staff.login)
    for i in range(5):
        factories.HistoryRawEntryFactory(
            object_id=data_history.service.id,
            request_data={'test': 'smth'},
            method='POST',
            content_type=ContentType.objects.get_for_model(Service),
        )
    factories.HistoryRawEntryFactory(
        object_id=data_history.meta_service.id,
        request_data={'test': 'smth'},
        method='POST',
        content_type=ContentType.objects.get_for_model(Service),
    )
    response = client.json.get(
        reverse('api-v4:history-list'),
        {'object_id': data_history.service.id}
    )
    assert response.status_code == 200
    results = response.json()['results']
    assert len(results) == 5
    assert results[0]['request_data'] == {'test': 'smth'}
    assert results[0]['object_id'] == str(data_history.service.id)
