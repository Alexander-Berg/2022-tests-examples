import pytest

from mock import patch

from idm.utils import reverse
from idm.users.constants.user import USER_TYPES
from idm.tests.utils import create_user


def test_yt_export(client, users_for_test):
    url = reverse('export_roles')

    response = client.get(url)
    assert response.status_code == 401

    client.login(users_for_test[0])
    response = client.get(url)
    assert response.status_code == 403

    tvm_app = create_user('123', type=USER_TYPES.TVM_APP)
    client.login(tvm_app)

    with patch('idm.api.testapi.yt.select_rows') as yt_client:
        yt_client.return_value = iter([{'blob': 'some_string'}])
        response = client.get(url)

    assert response.status_code == 200
    assert response.content == b'some_string'
