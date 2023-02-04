import dateutil.parser

import pytest
from django.core.urlresolvers import reverse

from plan.suspicion.models import Complaint
from common import factories

pytestmark = pytest.mark.django_db


@pytest.mark.parametrize('api', ['api-v3', 'api-v4'])
def test_complaints_get(client, api):
    service1 = factories.ServiceFactory()
    complaint1 = factories.ComplaintFactory(service=service1)
    complaint2 = factories.ComplaintFactory(service=service1)
    service2 = factories.ServiceFactory()
    factories.ComplaintFactory(service=service2)

    response = client.json.get(reverse(api + ':complaint-list'), {'service': service1.id})
    assert response.status_code == 200
    results = response.json()['results']
    assert [result['message'] for result in results] == [complaint2.message, complaint1.message]
    assert dateutil.parser.parse(results[0]['created_at']) == complaint2.created_at
    assert results[0]['service'] == service1.id


@pytest.mark.parametrize('api', ['api-v3', 'api-v4'])
def test_complaints_post(client, data, api):
    client.login(data.staff.login)
    response = client.json.post(
        reverse(api + ':complaint-list'),
        {'service': data.service.id, 'message': 'complaint'}
    )
    assert response.status_code == 201
    complaint = Complaint.objects.get()
    assert complaint.service == data.service
    assert complaint.author == data.staff
    assert complaint.message == 'complaint'
