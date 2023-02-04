import pytest
from django.core.urlresolvers import reverse

from common import factories


pytestmark = pytest.mark.django_db


def test_get_tags(client, data):
    tag = factories.ServiceTagFactory()

    response = client.json.get(reverse('services-api:tags-list'))

    assert response.status_code == 200

    json = response.json()['results']
    assert len(json) == 1
    assert json[0]['id'] == tag.id
