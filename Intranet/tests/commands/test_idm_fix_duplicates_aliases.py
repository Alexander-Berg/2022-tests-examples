# coding: utf-8

import pytest
from django.core import management
from django.db import connection

from idm.core.models import RoleNode, RoleAlias

pytestmark = pytest.mark.django_db


@pytest.fixture
def node_with_dublicate_aliases(arda_users_with_roles, rollback_index_role_alias):
    cursor = connection.cursor()
    cmd = '''insert into upravlyator_rolealias
             (type, name, name_en, is_active, node_id) values
             ('test1', 'алиас', 'alias', true, %s)'''

    node_id = RoleNode.objects.get(name='Могучий Пользователь').id
    cursor.execute(cmd % (node_id))
    cursor.execute(cmd % (node_id))

    return arda_users_with_roles


@pytest.mark.skip(reason='падает финалайзер фикстуры')
def test_command(node_with_dublicate_aliases):
    node = RoleNode.objects.get(name='Могучий Пользователь')
    assert RoleAlias.objects.filter(
        is_active=True,
        type='test1',
        name='алиас',
        name_en='alias',
        node=node,
    ).count() == 2
    management.call_command('idm_fix_duplicates_aliases')
    assert RoleAlias.objects.filter(
        is_active=True,
        type='test1',
        name='алиас',
        name_en='alias',
        node=node,
    ).count() == 1
