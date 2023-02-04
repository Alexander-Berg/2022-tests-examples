import json
import pytest

from unittest.mock import patch, Mock

from constance import config
from django.urls import reverse

from intranet.femida.tests import factories as f

pytestmark = pytest.mark.django_db


def test_frontend_version_view_with_perm(client):
    user = f.create_user_with_perm('can_use_frontend_version_view')
    client.login(user.username)
    url = reverse('private-api:frontend:change-version')
    data = {'version': '0.0.1'}
    with patch('intranet.femida.src.api.frontend.views.config', Mock()):
        response = client.post(url, data)
    assert response.status_code == 200


def test_frontend_version_view_without_perm(client):
    user = f.create_user()
    client.login(user.username)
    url = reverse('private-api:frontend:change-version')
    response = client.post(url)
    assert response.status_code == 403


@patch('intranet.femida.src.utils.frontend.get_manifest', Mock(return_value={'key': 'value'}))
def test_frontend_version_change(su_client):
    new_version = '0.0.7'
    config.FRONTEND_VERSION = '0.0.0'
    config.FRONT_STATIC_ASSETS = '{}'
    url = reverse('private-api:frontend:change-version')
    data = {'version': new_version}
    response = su_client.post(url, data)

    assert response.status_code == 200
    assert config.FRONTEND_VERSION == new_version
    assert json.loads(config.FRONT_STATIC_ASSETS)['key'] == 'value'
