import mock
import pytest
from django_idm_api.exceptions import BadRequest

from infra.cauth.server.common.models import Access
from infra.cauth.server.master.api.idm.hooks import CauthIdmHooks
from __tests__.utils import create_server, get_or_create_server_group, create_access_rule

pytestmark = pytest.mark.django_db


@mock.patch('infra.cauth.server.master.api.idm.forms.server_is_resolved', return_value=True)
def test_add_eine_role_for_baremetal_server(patched, users):
    user = users.frodo
    server = create_server('server.net', is_baremetal=True)
    role = {
        'role': 'eine',
        'dst': server.fqdn,
    }

    CauthIdmHooks().add_role_impl(user.login, role, {})

    access_queryset = Access.query.filter_by(
        dst_server_id=server.id,
        src_user_id=user.uid,
        type='eine',
    )
    assert access_queryset.count() == 1


@mock.patch('infra.cauth.server.master.api.idm.forms.server_is_resolved', return_value=True)
def test_add_eine_role_forbidden_for_virtual_server(patched, users):
    user = users.frodo
    server = create_server('server.net', is_baremetal=False)
    role = {
        'role': 'eine',
        'dst': server.fqdn,
    }

    with pytest.raises(BadRequest):
        CauthIdmHooks().add_role_impl(user.login, role, {})

    access_queryset = Access.query.filter_by(
        dst_server_id=server.id,
        src_user_id=user.uid,
        type='eine',
    )
    assert access_queryset.count() == 0


def test_add_eine_role_for_server_group(users):
    user = users.frodo
    group = get_or_create_server_group(source_name='bot')
    role = {
        'role': 'eine',
        'dst': group.name,
    }

    CauthIdmHooks().add_role_impl(user.login, role, {})

    access_queryset = Access.query.filter_by(
        dst_group_id=group.id,
        src_user_id=user.uid,
        type='eine',
    )
    assert access_queryset.count() == 1


@pytest.mark.parametrize('source_name', ['cms', 'yp', 'walle'])
def test_add_eine_role_forbidden_for_not_bot_server_group(users, source_name):
    user = users.frodo
    group = get_or_create_server_group(source_name=source_name)
    role = {
        'role': 'eine',
        'dst': group.name,
    }

    with pytest.raises(BadRequest):
        CauthIdmHooks().add_role_impl(user.login, role, {})

    access_queryset = Access.query.filter_by(
        dst_group_id=group.id,
        src_user_id=user.uid,
        type='eine',
    )
    assert access_queryset.count() == 0


def test_remove_eine_role(users):
    server = create_server('server.net')
    user = users.frodo
    access = create_access_rule('eine', user, server)
    role = {
        'role': 'eine',
        'dst': server.fqdn,
    }

    CauthIdmHooks().remove_role_impl(user.login, role, {}, is_fired=False)

    assert Access.query.filter_by(id=access.id).count() == 0
