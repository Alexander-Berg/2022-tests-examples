import json

from unittest.mock import MagicMock, patch, Mock

import pytest
from django.contrib.auth.models import AnonymousUser

from django.core.urlresolvers import reverse

from staff.lib.testing import StaffFactory, SSHKeyFactory
from staff.person_profile.controllers.ssh_key import SSHKeyError
from staff.keys.models import SSHKey
from staff.keys.views import skotty_upload_ssh_keys, skotty_revoke_ssh_keys, keys_audit

SSH_KEY = (
    'ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAABAQDmCvbWwxazuJx+P82dyNOvHo9RTJd/NPsuUFJy6qdqE6Pp2/NI/Sasjmp6IFNG'
    '+4DtKMkwFx1h9LNnmJux+1wS44liQoyLbeYa55pbTKt0VP+CRXREMdYkAD1mASokENtKeTurWVQCvnPzV2ErISs+UEMkFHOsEL2CURM'
    '/0GvPZgBs9cI7VT43k2K4S2K6XqbBlcRShU8U7cRwMdcKnklNhbl00mm9AMYhkn76vnrgOfgMB'
    '+D8Mr6E2C243kvH2Mkgr6qvL6RA1NlSq9DL5OsXCosGDXEh3NRir8rl0qeDmE9TAbhwYmkbXJAP708n9fm6TfU6SI5OfGcli3KGRxnD '
    'you@example.com '
)


@pytest.mark.django_db
def test_keys_audit_view(rf):
    person = StaffFactory()
    person.user.is_superuser = True
    dismissed_person = StaffFactory(is_dismissed=True)
    key = SSHKey(
        staff=dismissed_person,
        description='description',
        key=SSH_KEY,
        intranet_status=0,
    )
    key.save()

    url = reverse('keys:keys-audit')
    request = rf.get(url+'?invalid=0&is_deleted=1&bits=2049')
    request.yauser = MagicMock()
    request.user = person.user
    request.real_user_ip = '1.2.3.4'
    response = keys_audit(request)

    assert response.status_code == 200
    assert SSH_KEY in b''.join(response.streaming_content).decode()


@pytest.mark.django_db
def test_skotty_upload_keys(rf):
    person = StaffFactory()
    person.user.is_superuser = True
    data = [
        {
            'key': SSH_KEY,
            'description': 'some key',
        },
    ]

    url = reverse('keys:skotty-upload-ssh-keys')
    request = rf.post(url, content_type='application/json', data=json.dumps(data))
    request.yauser = MagicMock()
    request.user = person.user
    request.real_user_ip = '1.2.3.4'
    response = skotty_upload_ssh_keys(request)

    assert response.status_code == 200
    assert json.loads(response.content) == {'success': True}


@pytest.mark.django_db
def test_skotty_upload_keys_invalid_json(rf):
    person = StaffFactory()
    person.user.is_superuser = True

    url = reverse('keys:skotty-upload-ssh-keys')
    request = rf.post(url, content_type='application/json', data='hello')
    request.yauser = MagicMock()
    request.user = person.user
    request.real_user_ip = '1.2.3.4'
    response = skotty_upload_ssh_keys(request)

    assert response.status_code == 400
    assert json.loads(response.content) == {'success': False, 'errors': ['Invalid JSON']}


@pytest.mark.django_db
def test_skotty_upload_keys_invalid_key(rf):
    data = [
        {
            'key': 'invalid key',
            'description': 'some key',
        },
    ]

    url = reverse('keys:skotty-upload-ssh-keys')
    request = rf.post(url, content_type='application/json', data=json.dumps(data))
    person = StaffFactory()
    person.user.is_superuser = True
    request.yauser = MagicMock()
    request.user = person.user
    request.real_user_ip = '1.2.3.4'
    response = skotty_upload_ssh_keys(request)

    assert response.status_code == 400
    assert json.loads(response.content) == {'success': False, 'errors': ['staff-general_ssh_key_field']}


@pytest.mark.django_db
def test_skotty_upload_keys_controller_exception(rf):
    url = reverse('keys:skotty-upload-ssh-keys')
    request = rf.post(url, content_type='application/json', data='[]')
    person = StaffFactory()
    person.user.is_superuser = True
    request.yauser = MagicMock()
    request.user = person.user
    request.real_user_ip = '1.2.3.4'
    with patch('staff.keys.views.ssh_key_ctl.add', Mock(side_effect=SSHKeyError('Unknown error'))):
        response = skotty_upload_ssh_keys(request)

    assert response.status_code == 400
    assert json.loads(response.content) == {'success': False, 'errors': ['Error saving ssh keys: Unknown error']}


@pytest.mark.django_db
def test_skotty_upload_keys_no_user_ticket(rf):
    url = reverse('keys:skotty-upload-ssh-keys')
    request = rf.post(url, content_type='application/json', data='[]')
    person = StaffFactory()
    person.user.is_superuser = True
    request.yauser = MagicMock()
    request.real_user_ip = '1.2.3.4'
    request.user = AnonymousUser()
    with patch('staff.lib.decorators._check_service_id', Mock(return_value=True)):
        response = skotty_upload_ssh_keys(request)

    assert response.status_code == 400
    assert json.loads(response.content) == {'success': False, 'errors': ['User ticket is required']}


@pytest.mark.django_db
def test_skotty_revoke_keys(rf):
    person = StaffFactory()
    person.is_superuser = True
    initial_key = SSHKeyFactory(key='test', fingerprint_sha256='test', staff_id=person.id)
    data = {'login': person.login, 'fingerprints': [initial_key.fingerprint_sha256]}

    url = reverse('keys:skotty-revoke-ssh-keys')
    request = rf.post(url, content_type='application/json', data=json.dumps(data))
    request.user = person
    request.yauser = MagicMock()
    request.real_user_ip = '1.2.3.4'
    response = skotty_revoke_ssh_keys(request)

    assert response.status_code == 200
    assert json.loads(response.content) == {'success': True}


@pytest.mark.django_db
def test_skotty_revoke_keys_nonexistent_key(rf):
    person = StaffFactory()
    person.user.is_superuser = True
    SSHKeyFactory(key='test', fingerprint='test', staff_id=person.id)
    data = {'login': person.login, 'fingerprints': ['test1']}

    url = reverse('keys:skotty-revoke-ssh-keys')
    request = rf.post(url, content_type='application/json', data=json.dumps(data))
    request.yauser = MagicMock()
    request.user = person.user
    request.real_user_ip = '1.2.3.4'
    response = skotty_revoke_ssh_keys(request)

    assert response.status_code == 400
    assert json.loads(response.content) == {
        'success': False,
        'errors': ['Error revoking ssh keys: SSHKey matching query does not exist.'],
    }


@pytest.mark.django_db
def test_skotty_revoke_keys_nonexistent_user(rf):
    person = StaffFactory()
    person.user.is_superuser = True
    initial_key = SSHKeyFactory(key='test', fingerprint='test', staff_id=person.id)
    data = {'login': 'test', 'fingerprints': [initial_key.fingerprint_sha256]}

    url = reverse('keys:skotty-revoke-ssh-keys')
    request = rf.post(url, content_type='application/json', data=json.dumps(data))
    request.yauser = MagicMock()
    request.user = person.user
    response = skotty_revoke_ssh_keys(request)

    assert response.status_code == 400
    assert json.loads(response.content) == {'success': False, 'errors': ['User does not exist']}


@pytest.mark.django_db
def test_skotty_revoke_keys_invalid_json(rf):
    person = StaffFactory()
    person.user.is_superuser = True

    url = reverse('keys:skotty-revoke-ssh-keys')
    request = rf.post(url, content_type='application/json', data='test')
    request.yauser = MagicMock()
    request.user = person.user
    response = skotty_revoke_ssh_keys(request)

    assert response.status_code == 400
    assert json.loads(response.content) == {'success': False, 'errors': ['Invalid JSON']}
