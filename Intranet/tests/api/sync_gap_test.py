from datetime import datetime
import pytest
import json

from django.core.urlresolvers import reverse

from staff.groups.models import Group, GROUP_TYPE_CHOICES


@pytest.mark.django_db
def test_sync_error(client):
    url = reverse('gap:api-need-update-gap', args=[2])
    group = Group()
    group.type = GROUP_TYPE_CHOICES.SERVICE
    group.service_id = 1
    group.created_at = datetime.now()
    group.modified_at = datetime.now()
    group.save()
    data = json.dumps({})
    response = client.post(url, data, content_type='application/json')
    assert response.status_code == 400
    data = json.loads(response.content)
    assert data == {
        "errors": {
            "service": [
                {"code": "invalid_choice"}
            ],
        }
    }


@pytest.mark.django_db
def test_sync_correct(client):
    url = reverse('gap:api-need-update-gap', args=[1])
    group = Group()
    group.type = GROUP_TYPE_CHOICES.SERVICE
    group.service_id = 1
    group.created_at = datetime.now()
    group.modified_at = datetime.now()
    group.save()
    data = json.dumps({})
    response = client.post(url, data, content_type='application/json')
    assert response.status_code == 200
