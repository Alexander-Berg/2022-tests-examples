import pytest

from unittest.mock import ANY

from django.urls import reverse


@pytest.mark.parametrize('ok_session_id, expected_ok_session_id', (
    ('123', '123'),
    ('456', '456'),
    (None, ANY),
))
def test_meta(client, ok_session_id, expected_ok_session_id):
    client.force_authenticate('robot-ok')
    url = reverse('api:meta')
    headers = {}
    if ok_session_id is not None:
        headers['HTTP_X_OK_SESSION_ID'] = ok_session_id

    response = client.get(url, **headers)

    assert response.status_code == 200, response.content
    response_data = response.json()
    assert response_data == {
        'environment': 'development',
        'user': {
            'login': 'robot-ok',
            'uid': '123456',
            'language': 'ru',
        },
        'ok_session_id': expected_ok_session_id,
    }
