import pytest

from intranet.femida.src.utils.authdata import get_active_flags, FRONTEND_PREFIX
from intranet.femida.tests import factories as f

pytestmark = pytest.mark.django_db


def test_active_flags(django_client):
    user = f.create_user()
    django_client.authenticate(user)
    flags_data = [
        ('flag1', None),
        (f'{FRONTEND_PREFIX}flag2', user),  # send to frontend
        ('flag3', user),
        (f'{FRONTEND_PREFIX}flag4', user),  # send to frontend
        ('flag5', None),
        (f'{FRONTEND_PREFIX}flag6', None),
    ]

    for (flg, usr) in flags_data:
        flag = f.create_waffle_flag(flg)
        if usr:
            flag.users.add(usr)

    request = django_client._get_request(method_name='get', path='/')
    result = get_active_flags(request, prefix=FRONTEND_PREFIX)

    assert len(result) == 2
