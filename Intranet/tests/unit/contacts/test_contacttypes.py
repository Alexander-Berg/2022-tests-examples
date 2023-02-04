import pytest
from django.core.urlresolvers import reverse

from common import factories

pytestmark = pytest.mark.django_db


def test_get_contacttypes(client):
    t = factories.ContactTypeFactory()

    response = client.json.get(
        reverse('contacts:contact_types')
    )
    assert response.status_code == 200

    results = response.json()
    assert len(results['content']['types']) == 1
    assert results['content']['types'][0]['id'] == t.code
    assert results['content']['types'][0]['name'] == t.name


def test_get_common_type_by_servicetag(client):
    service = factories.ServiceFactory()
    common_type = factories.ContactTypeFactory()

    response = client.json.get(
        reverse('contacts:contact_types'),
        {
            'service': service.pk,
        }
    )
    assert response.status_code == 200

    results = response.json()
    assert len(results['content']['types']) == 1
    assert {ctype['id'] for ctype in results['content']['types']} == {common_type.code}


def test_get_contacttypes_by_servicetags(client):
    tag1 = factories.ServiceTagFactory()
    service1 = factories.ServiceFactory()
    service1.tags.add(tag1)

    tag2 = factories.ServiceTagFactory()
    service2 = factories.ServiceFactory()
    service2.tags.add(tag2)

    common_type = factories.ContactTypeFactory()

    service1_type = factories.ContactTypeFactory()
    service1_type.tags.add(tag1)

    service2_type = factories.ContactTypeFactory()
    service2_type.tags.add(tag2)

    response = client.json.get(
        reverse('contacts:contact_types'),
        {
            'service': service1.pk,
        }
    )
    assert response.status_code == 200

    results = response.json()
    assert len(results['content']['types']) == 2
    assert {ctype['id'] for ctype in results['content']['types']} == {common_type.code, service1_type.code}
