from mock import patch

from django.urls import reverse
from django.test import modify_settings


@modify_settings(
    MIDDLEWARE={
        'append': 'intranet.compositor.src.middleware.YauthMiddleware'
    }
)
def test_yauth_check_exempt_success(client):
    url = reverse('ping')
    with patch('django_yauth.authentication_mechanisms.tvm.TVM2'):
        response = client.get(url)
    response_json = response.json()
    assert response.status_code == 200
    assert response_json == {'status': 'ok'}


@modify_settings(
    MIDDLEWARE={
        'append': 'intranet.compositor.src.middleware.YauthMiddleware',
        'remove': 'intranet.compositor.src.middleware.MockAuthMiddleware',
    }
)
def test_tvm2_required(client):
    url = reverse('api_v1:workflows-list')
    with patch('django_yauth.authentication_mechanisms.tvm.TVM2'):
        response = client.get(url)
    response_json = response.json()
    assert response.status_code == 401
    assert response_json == {'error': 'Invalid tvm2 ticket'}
