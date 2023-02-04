import mock
import pytest

from staff.lib.testing import StaffFactory
from staff.keys import tasks
from staff.keys.models import SSHKey


@pytest.mark.django_db
def test_keys_audit_view():
    person = StaffFactory()
    keys = []
    for _ in range(2):
        key = SSHKey(
            staff=person,
            description='description',
            key='key',
        )
        key.save()
        keys.append(key)
    ids_to_return = [k.id for k in keys]
    ids_to_return += [i+100 for i in ids_to_return]
    keys_from_cauth = [
        {
            'id': i,
            'intranet_status': 0,
            'created_at': '2021-06-01T16:50:17',
            'updated_at': '2021-06-01T16:50:17',
            'description': 'asd',
            'key': 'asd',
            'fingerprint': 'asd',
            'fingerprint_sha256': 'asd',
            'staff_id': person.id,
        }
        for i in set(ids_to_return)
    ]

    return_value = mock.Mock(**{
        'json.return_value': {'keys': keys_from_cauth},
        'status_code': 200,
    })
    path = 'staff.keys.tasks.requests.get'
    with mock.patch(path, return_value=return_value):
        tasks.sync_keys()
    assert SSHKey.objects.count() == len(keys_from_cauth)
