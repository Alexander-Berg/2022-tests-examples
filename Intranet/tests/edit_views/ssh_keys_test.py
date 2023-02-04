import json
import os

from mock import Mock, patch
import pytest

from django.conf import settings
from django.core.urlresolvers import reverse

from staff.keys.models import SSHKey
from staff.lib.testing import StaffFactory
from staff.person_profile.forms.ssh_key import SSHKeyValidator

from staff.person_profile.edit_views.edit_ssh_keys_view import edit_ssh_keys

DIR = os.path.dirname(os.path.abspath(__file__))


def _read_key(name):
    with open('%s/ssh_keys/%s' % (DIR, name), 'r') as f:
        return f.read().strip()


def _read_fingerprint(name):
    with open('%s/ssh_keys/%s.fingerprint' % (DIR, name), 'r') as f:
        return f.read().strip()


VALID_0_KEY = _read_key('valid.0.key')
VALID_0_KEY_FP = _read_fingerprint('valid.0.key')
VALID_0_KEY_FP_SHA256 = 'SHA256:krULYcv9eVZDn3cTvmq8hHY69CKd4QQ4zHYDVBazsCY'
VALID_1_KEY = _read_key('valid.1.key')
VALID_1_KEY_FP = _read_fingerprint('valid.1.key')
VALID_1_KEY_FP_SHA256 = 'SHA256:KXeRcJ/SxaacEvVHTs5ZgWAu22qgUXk+NGLw6pXAygQ'
TOO_SHORT_KEY = _read_key('too.short.key')


@pytest.fixture()
def test_person(db):
    return StaffFactory(
        login=settings.AUTH_TEST_USER,
    )


@pytest.mark.django_db()
def test_ssh_keys(client, test_person):
    _test_new_valid_key(client, test_person)
    _test_edit(client, test_person)
    _test_second_valid_key(client, test_person)
    _test_too_short(client, test_person)
    _test_delete_key(client, test_person)
    _test_new_empty(client, test_person)


def _test_new_valid_key(client, test_person):
    data = {'ssh_keys': [
        # добавляем валидный новый SSH (RSA 2048)
        {
            'id': '',
            'key': VALID_0_KEY,
            'description': 'Новый валидный ключ',
        },
    ]}

    response = client.post(
        reverse('profile:edit-ssh-keys', kwargs={'login': test_person.login}),
        json.dumps(data),
        content_type='application/json'
    )

    assert response.status_code == 200, response.content
    answer = json.loads(response.content)

    key_id = SSHKey.objects.order_by('-id').first().id

    assert answer == {
        'target': {
            'ssh_keys': [
                {
                    'id': key_id,
                    'key': VALID_0_KEY,
                    'fingerprint': VALID_0_KEY_FP,
                    'fingerprint_sha256': VALID_0_KEY_FP_SHA256,
                    'description': 'Новый валидный ключ',
                },
            ]
        }
    }


def _test_edit(client, test_person):
    key_id = SSHKey.objects.order_by('-id').first().id

    data = {'ssh_keys': [
        # пытаемся изменить существующий ключ
        {
            'id': key_id,
            'fingerprint': 'ABC',
            'key': 'XYZ',
            'description': 'Валидный новый ключ',
        },
    ]}

    response = client.post(
        reverse('profile:edit-ssh-keys', kwargs={'login': test_person.login}),
        json.dumps(data),
        content_type='application/json'
    )

    assert response.status_code == 200, response.content
    answer = json.loads(response.content)
    assert answer == {
        'target': {
            'ssh_keys': [
                {
                    'id': key_id,
                    'key': VALID_0_KEY,
                    'fingerprint': VALID_0_KEY_FP,
                    'fingerprint_sha256': VALID_0_KEY_FP_SHA256,
                    'description': 'Валидный новый ключ',
                },
            ]
        }
    }


def _test_second_valid_key(client, test_person):
    key_id = SSHKey.objects.order_by('-id').first().id

    data = {'ssh_keys': [
        # оставляем валидный SSH
        {
            'id': key_id,
            'key': '',
            'description': 'Валидный новый ключ',
        },
        # добавляем валидный новый SSH (DSA 2048)
        {
            'id': '',
            'key': VALID_1_KEY,
            'description': 'Очень новый валидный ключ',
        },
    ]}

    response = client.post(
        reverse('profile:edit-ssh-keys', kwargs={'login': test_person.login}),
        json.dumps(data),
        content_type='application/json'
    )

    assert response.status_code == 200, response.content
    answer = json.loads(response.content)
    assert answer == {
        'target': {
            'ssh_keys': [
                {
                    'id': key_id,
                    'key': VALID_0_KEY,
                    'fingerprint': VALID_0_KEY_FP,
                    'fingerprint_sha256': VALID_0_KEY_FP_SHA256,
                    'description': 'Валидный новый ключ',
                },
                {
                    'id': key_id + 1,
                    'key': VALID_1_KEY,
                    'fingerprint': VALID_1_KEY_FP,
                    'fingerprint_sha256': VALID_1_KEY_FP_SHA256,
                    'description': 'Очень новый валидный ключ',
                },
            ]
        }
    }


def _test_too_short(client, test_person):
    data = {
        'ssh_keys': [
            # добавляем слишком короткий SSH (DSA 1024)
            {
                'id': '',
                'key': TOO_SHORT_KEY,
                'description': 'Слишком короткий ключ',
            },
        ]
    }

    response = client.post(
        reverse('profile:edit-ssh-keys', kwargs={'login': test_person.login}),
        json.dumps(data),
        content_type='application/json',
    )

    assert response.status_code == 400, response.content
    answer = json.loads(response.content)
    assert answer == {
        'errors': {
            'ssh_keys': {
                '0': {'key': [{'error_key': 'staff-too_short_ssh_key_field'}]},
            }
        }
    }


def _test_delete_key(client, test_person):
    key_id = SSHKey.objects.order_by('-id').first().id

    data = {'ssh_keys': [
        # оставляем второй валидный ключ
        {
            'id': key_id,
            'key': '',
            'description': 'Очень новый валидный ключ',
        },
    ]}

    response = client.post(
        reverse('profile:edit-ssh-keys', kwargs={'login': test_person.login}),
        json.dumps(data),
        content_type='application/json'
    )

    assert response.status_code == 200, response.content
    answer = json.loads(response.content)
    assert answer == {'target': {'ssh_keys': [
        {
            'id': key_id,
            'key': VALID_1_KEY,
            'fingerprint': VALID_1_KEY_FP,
            'fingerprint_sha256': VALID_1_KEY_FP_SHA256,
            'description': 'Очень новый валидный ключ',
        },
    ]}}


def _test_new_empty(client, test_person):
    data = {'ssh_keys': [
        # пробелы вместо ключа
        {
            'id': '',
            'key': '      ',
            'description': 'Пробелы',
        },
        # пустой
        {
            'id': '',
            'key': '',
            'description': 'Пустой',
        },
    ]}

    response = client.post(
        reverse('profile:edit-ssh-keys', kwargs={'login': test_person.login}),
        json.dumps(data),
        content_type='application/json'
    )

    assert response.status_code == 400, response.content
    answer = json.loads(response.content)
    assert answer == {
        'errors': {
            'ssh_keys': {
                '0': {'key': [{'error_key': 'staff-invalid_key_ssh_key_field'}]},
                '1': {'key': [{'error_key': 'staff-invalid_key_ssh_key_field'}]},
            }
        }
    }


@pytest.mark.django_db()
def test_adding_same_keys_not_allowed(person_profile_request_factory):
    person1 = StaffFactory()
    person2 = StaffFactory()

    data = {
        'ssh_keys': [{
            'id': '',
            'key': VALID_0_KEY,
            'description': 'Новый валидный ключ',
        }],
    }

    request = person_profile_request_factory.post(person1, 'profile:edit-ssh-keys', data)
    response = edit_ssh_keys(request, login=person1.login)
    assert response.status_code == 200

    request = person_profile_request_factory.post(person2, 'profile:edit-ssh-keys', data)
    response = edit_ssh_keys(request, login=person2.login)
    assert response.status_code == 400


@pytest.mark.django_db()
def test_adding_same_keys_from_exceptions_list_allowed(person_profile_request_factory):

    person1 = StaffFactory()
    person2 = StaffFactory()

    data = {
        'ssh_keys': [{
            'id': '',
            'key': VALID_0_KEY,
            'description': 'Новый валидный ключ',
        }],
    }

    request = person_profile_request_factory.post(person1, 'profile:edit-ssh-keys', data)
    response = edit_ssh_keys(request, login=person1.login)
    assert response.status_code == 200

    with patch.object(SSHKeyValidator, 'allowed_ssh_keys_duplicates_fingerprints', new=[VALID_0_KEY_FP_SHA256]):
        request = person_profile_request_factory.post(person2, 'profile:edit-ssh-keys', data)
        response = edit_ssh_keys(request, login=person2.login)
        assert response.status_code == 200


def test_send_verify_code(client, test_person):
    cauth_answer = {'status': 'THIS IS PROXY'}
    mock_post = Mock(**{'json.return_value': cauth_answer})
    with patch('staff.person_profile.edit_views.edit_ssh_keys_view._cauth_session.post', return_value=mock_post):
        response = client.post(
            reverse('profile:send-verify-code', kwargs={'login': test_person.login}),
            content_type='application/json',
        )
        assert response.status_code == 200, response.content
        assert json.loads(response.content) == cauth_answer
