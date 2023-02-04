import json

import pytest

from django.core.urlresolvers import reverse


@pytest.mark.django_db
def test_departments_meta_view_without_url(client):
    view_url = reverse('proposal-api:departments-meta')
    response = client.get(view_url)
    assert response.status_code == 400


@pytest.mark.django_db
def test_departments_meta_view(company, client):
    view_url = reverse('proposal-api:departments-meta')
    view_url += '?url=' + '&url='.join(company.departments.keys())
    response = client.get(view_url)
    assert response.status_code == 200
    response_content = json.loads(response.content)
    assert set(response_content.keys()) == set(company.departments.keys())
