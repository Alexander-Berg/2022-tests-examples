import pytest

from staff.achievery.tests.factories.domain import PersonFactory
from staff.keys.models import SSHKey
from staff.lib.testing import SSHKeyFactory

from staff.person_profile.controllers import ssh_key as ssh_key_ctl
from staff.person_profile.controllers.ssh_key import SSHKeyError


@pytest.mark.django_db
def test_revoke_by_fingerprints():
    person = PersonFactory()
    initial_key = SSHKeyFactory(
        fingerprint_sha256='test',
        staff_id=person.id,
        intranet_status=1,
    )

    assert not ssh_key_ctl.revoke_by_fingerprints(['test'], person, 'test_author')

    ssh_key = SSHKey.objects.get(id=initial_key.id)
    assert ssh_key.intranet_status == 0


@pytest.mark.django_db
def test_revoke_by_fingerprints_nonexistent_fingerprint():
    person = PersonFactory()

    with pytest.raises(SSHKeyError):
        ssh_key_ctl.revoke_by_fingerprints(['test'], person, 'test_author')


@pytest.mark.django_db
def test_add():
    person = PersonFactory()

    key_data = {
        'key': 'ssh-rsa testKey',
        'fingerprint': 'test',
        'fingerprint_sha256': 'SHA256:test',
        'description': 'description',
    }

    ssh_key_ctl.add([key_data], person, 'test_author')

    ssh_key = SSHKey.objects.get(staff_id=person.id)
    assert ssh_key.intranet_status == 1
    for key, value in key_data.items():
        assert getattr(ssh_key, key) == value


@pytest.mark.django_db
def test_add_restores_key():
    person = PersonFactory()

    key_data = {
        'key': 'ssh-rsa testKey',
        'fingerprint': 'test',
        'fingerprint_sha256': 'SHA256:test',
        'description': 'description',
    }
    initial_key = SSHKeyFactory(
        staff_id=person.id,
        intranet_status=0,
        **key_data,
    )

    ssh_key_ctl.add([key_data], person, 'test_author')

    ssh_key = SSHKey.objects.get(id=initial_key.id)
    assert ssh_key.intranet_status == 1
    for key, value in key_data.items():
        assert getattr(ssh_key, key) == value


@pytest.mark.django_db
def test_add_malformed_data():
    person = PersonFactory()

    key_data = {}

    with pytest.raises(SSHKeyError):
        ssh_key_ctl.add([key_data], person, 'test_author')
