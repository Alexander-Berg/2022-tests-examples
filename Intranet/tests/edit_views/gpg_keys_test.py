import json
import os

import pytest

from django.conf import settings
from django.core.urlresolvers import reverse

from staff.lib.testing import StaffFactory
from staff.keys.models import GPGKey

from staff.person_profile.edit_views.edit_gpg_keys_view import edit_gpg_keys


DIR = os.path.dirname(os.path.abspath(__file__))


def _read_key(name):
    with open('%s/gpg_keys/%s' % (DIR, name), 'r') as f:
        return f.read().strip()


def _read_fingerprint(name):
    with open('%s/gpg_keys/%s.fingerprint' % (DIR, name), 'r') as f:
        return f.read().strip()


VALID_0_KEY = _read_key('valid.0.key')
VALID_0_KEY_FP = _read_fingerprint('valid.0.key')
VALID_1_KEY = _read_key('valid.1.key')
VALID_1_KEY_FP = _read_fingerprint('valid.1.key')
INVALID_KEY = _read_key('invalid.key')
TWO_KEYS = _read_key('two.keys')


@pytest.mark.django_db()
def test_gpg_keys(client):

    test_person = StaffFactory(
        login=settings.AUTH_TEST_USER,
    )

    _test_new_valid_key(client, test_person)
    _test_edit(client, test_person)
    _test_second_valid_key(client, test_person)
    _test_delete_key(client, test_person)
    _test_new_invalid_key(client, test_person)
    _test_two_keys(client, test_person)


def _test_new_valid_key(client, test_person):
    data = {'gpg_keys': [
        # добавляем валидный новый GPG (RSA 2048)
        {
            'id': '',
            'fingerprint': '',
            'key': VALID_0_KEY,
            'description': 'Новый валидный ключ',
        },
    ]}

    response = client.post(
        reverse('profile:edit-gpg-keys', kwargs={'login': test_person.login}),
        json.dumps(data),
        content_type='application/json'
    )

    assert response.status_code == 200, response.content
    answer = json.loads(response.content)

    id = GPGKey.objects.order_by('-id').first().id

    assert answer == {'target': {'gpg_keys': [
        {
            'id': id,
            'key': VALID_0_KEY,
            'fingerprint': VALID_0_KEY_FP,
            'description': 'Новый валидный ключ',
        },
    ]}}


def _test_edit(client, test_person):
    id = GPGKey.objects.order_by('-id').first().id

    data = {'gpg_keys': [
        # пытаемся изменить существующий ключ
        {
            'id': id,
            'fingerprint': 'ABC',
            'key': 'XYZ',
            'description': 'Валидный новый ключ',
        },
    ]}

    response = client.post(
        reverse('profile:edit-gpg-keys', kwargs={'login': test_person.login}),
        json.dumps(data),
        content_type='application/json'
    )

    assert response.status_code == 200, response.content
    answer = json.loads(response.content)
    assert answer == {'target': {'gpg_keys': [
        {
            'id': id,
            'key': VALID_0_KEY,
            'fingerprint': VALID_0_KEY_FP,
            'description': 'Валидный новый ключ',
        },
    ]}}


def _test_second_valid_key(client, test_person):
    id = GPGKey.objects.order_by('-id').first().id

    data = {'gpg_keys': [
        # оставляем валидный GPG
        {
            'id': id,
            'fingerprint': '',
            'key': '',
            'description': 'Валидный новый ключ',
        },
        # добавляем валидный новый GPG (RSA 2048)
        {
            'id': '',
            'fingerprint': '',
            'key': VALID_1_KEY,
            'description': 'Очень новый валидный ключ',
        },
    ]}

    response = client.post(
        reverse('profile:edit-gpg-keys', kwargs={'login': test_person.login}),
        json.dumps(data),
        content_type='application/json'
    )

    assert response.status_code == 200, response.content
    answer = json.loads(response.content)
    assert answer == {'target': {'gpg_keys': [
        {
            'id': id,
            'key': VALID_0_KEY,
            'fingerprint': VALID_0_KEY_FP,
            'description': 'Валидный новый ключ',
        },
        {
            'id': id + 1,
            'key': VALID_1_KEY,
            'fingerprint': VALID_1_KEY_FP,
            'description': 'Очень новый валидный ключ',
        },
    ]}}


def _test_delete_key(client, test_person):
    id = GPGKey.objects.order_by('-id').first().id

    data = {'gpg_keys': [
        # оставляем второй валидный ключ
        {
            'id': id,
            'fingerprint': '',
            'key': '',
            'description': 'Очень новый валидный ключ',
        },
    ]}

    response = client.post(
        reverse('profile:edit-gpg-keys', kwargs={'login': test_person.login}),
        json.dumps(data),
        content_type='application/json'
    )

    assert response.status_code == 200, response.content
    answer = json.loads(response.content)
    assert answer == {'target': {'gpg_keys': [
        {
            'id': id,
            'key': VALID_1_KEY,
            'fingerprint': VALID_1_KEY_FP,
            'description': 'Очень новый валидный ключ',
        },
    ]}}


def _test_new_invalid_key(client, test_person):
    data = {'gpg_keys': [
        # добавляем [чуть-чуть] невалидный новый GPG (RSA 2048)
        {
            'id': '',
            'fingerprint': '',
            'key': INVALID_KEY,
            'description': 'Новый немножко невалидный ключ',
        },
        # добавляем [вообще совсем] невалидный текст вместо ключа
        {
            'id': '',
            'fingerprint': '',
            'key': 'QWERTYUIOP{}',
            'description': 'Новый вообще совсем невалидный ключ',
        },
    ]}

    response = client.post(
        reverse('profile:edit-gpg-keys', kwargs={'login': test_person.login}),
        json.dumps(data),
        content_type='application/json'
    )

    assert response.status_code == 400, response.content
    answer = json.loads(response.content)
    assert answer == {'errors': {'gpg_keys': {
        '0': {'key': [{'error_key': 'staff-general_gpg_key_field'}]},
        '1': {'key': [{'error_key': 'staff-general_gpg_key_field'}]},
    }}}


def _test_two_keys(client, test_person):
    data = {'gpg_keys': [
        # добавляем сдвоенный новый GPG (RSA 2048)
        {
            'id': '',
            'fingerprint': '',
            'key': TWO_KEYS,
            'description': 'Два ключа',
        },
    ]}

    response = client.post(
        reverse('profile:edit-gpg-keys', kwargs={'login': test_person.login}),
        json.dumps(data),
        content_type='application/json'
    )

    assert response.status_code == 400, response.content
    answer = json.loads(response.content)
    assert answer == {'errors': {'gpg_keys': {
        '0': {'key': [{'error_key': 'staff-general_gpg_key_field'}]},
    }}}


@pytest.mark.django_db()
def test_adding_same_keys_not_allowed(person_profile_request_factory):
    person1 = StaffFactory()
    person2 = StaffFactory()

    data = {
        'gpg_keys': [{
            'id': '',
            'fingerprint': '',
            'key': VALID_0_KEY,
            'description': 'Новый валидный ключ',
        }],
    }

    request = person_profile_request_factory.post(person1, 'profile:edit-gpg-keys', data)
    response = edit_gpg_keys(request, login=person1.login)
    assert response.status_code == 200

    request = person_profile_request_factory.post(person2, 'profile:edit-gpg-keys', data)
    response = edit_gpg_keys(request, login=person2.login)
    assert response.status_code == 400
