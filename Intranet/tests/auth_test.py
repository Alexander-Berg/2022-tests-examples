import pytest

from staff.lib.auth.auth_mechanisms.anonymous import Mechanism as AnonymousMechanism
from staff.lib.auth.auth_mechanisms.center import Mechanism as CenterMechanism

from staff.lib.testing import TokenFactory


@pytest.mark.django_db
def test_center_auth_mechanism(rf):
    center_url = '/any-center-url/'
    center_token_name = 'token'
    center_token_value = 'test_token_value'
    center_client_ip = '1.2.3.4'

    TokenFactory(
        token=center_token_value,
        ips=center_client_ip,
        hostnames='center-client-host.yandex-team.ru',
    )

    correct_request1 = rf.post(
        center_url,
        data={center_token_name: center_token_value},
        HTTP_X_REAL_IP=center_client_ip,
        REMOTE_ADDR='1.2.3.5',
    )
    correct_request2 = rf.post(
        center_url,
        data={center_token_name: center_token_value},
        HTTP_X_REAL_IP='8.8.8.8',
        REMOTE_ADDR=center_client_ip,
    )
    correct_request_with_token_in_header = rf.get(
        center_url,
        HTTP_X_REAL_IP='8.8.8.8',
        REMOTE_ADDR=center_client_ip,
        HTTP_X_YANDEX_DJANGO_API_AUTH=center_token_value,
    )

    wrong_ip_request = rf.post(
        center_url,
        data={center_token_name: center_token_value},
        HTTP_X_REAL_IP='8.8.8.8',
        REMOTE_ADDR='9.9.9.9',
    )

    wrong_token_name_request = rf.post(
        center_url,
        data={center_token_name: 'randomstring'},
        HTTP_X_REAL_IP=center_client_ip,
        REMOTE_ADDR='9.9.9.9',
    )

    no_token_request = rf.post(
        center_url,
        data={},
        REMOTE_ADDR='9.9.9.9',
    )

    center_auth_mechanism = CenterMechanism()

    assert center_auth_mechanism.is_valid(correct_request1)
    assert center_auth_mechanism.is_valid(correct_request2)
    assert center_auth_mechanism.is_valid(correct_request_with_token_in_header)

    assert not center_auth_mechanism.is_valid(wrong_ip_request)
    assert not center_auth_mechanism.is_valid(wrong_token_name_request)
    assert not center_auth_mechanism.is_valid(no_token_request)


PATHS = [
    (AnonymousMechanism.ANONYMOUS_AVAILABLE_PATHS[0], True),
    (AnonymousMechanism.ANONYMOUS_AVAILABLE_PATHS[1] + 'any', True),
    (AnonymousMechanism.ANONYMOUS_AVAILABLE_PATHS[2], True),
    ('/profile-api/meta/', False),
    ('/secure-url/?param=value', False),
]


@pytest.mark.parametrize('path, is_valid', PATHS)
def test_anonymous_auth_mechanism(rf, path, is_valid):
    request = rf.get(path)
    anonymouth_auth_mechanism = AnonymousMechanism()
    assert anonymouth_auth_mechanism.is_valid(request) == is_valid
