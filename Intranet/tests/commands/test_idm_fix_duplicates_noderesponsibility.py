# coding: utf-8


from django.core import management
from django.db import connection
import pytest

from idm.core.models import RoleNode, NodeResponsibility
from idm.users.models import User


pytestmark = pytest.mark.django_db


@pytest.fixture
def system_with_dublicates(arda_users_with_roles, rollback_index_node_responsibility):
    cursor = connection.cursor()
    cmd = '''insert into upravlyator_noderesponsibility
             (created_at, updated_at, is_active, notify, node_id, user_id) values
             ('%s', '%s', true, true, %s, %s)'''
    date1 = '2018-11-11 10:00:00'
    date2 = '2018-12-11 10:00:00'
    user_id = User.objects.get(username='frodo').id
    node_id = RoleNode.objects.get(name='Могучий Пользователь').id
    cursor.execute(cmd % (date1, date1, node_id, user_id))
    cursor.execute(cmd % (date2, date2, node_id, user_id))

    node_id = RoleNode.objects.get(name='Менеджер').id
    cursor.execute(cmd % (date1, date2, node_id, user_id))

    user_id = User.objects.get(username='gandalf').id
    cursor.execute(cmd % (date1, date2, node_id, user_id))

    return arda_users_with_roles


@pytest.mark.skip(reason='падает финалайзер фикстуры')
def test_command(system_with_dublicates):
    assert NodeResponsibility.objects.filter(is_active=True).count() == 4
    management.call_command('idm_fix_duplicates_noderesponsibility')
    assert NodeResponsibility.objects.filter(is_active=True).count() == 3
    frodo = User.objects.get(username='frodo')
    assert NodeResponsibility.objects.filter(is_active=True, user=frodo).count() == 2
    node = RoleNode.objects.get(name='Могучий Пользователь')
    assert NodeResponsibility.objects.filter(is_active=True, user=frodo, node=node).count() == 1
