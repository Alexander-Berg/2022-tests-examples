# coding: utf-8


from mock import patch

import pytest
from django.core import mail
from django.core.management import call_command

from idm.core.models import Role, ApproveRequest
from idm.core.workflow.exceptions import Forbidden
from idm.core.plugins.errors import PluginError
from idm.tests.utils import (
    assert_action_chain, refresh, set_workflow, DEFAULT_WORKFLOW,
    clear_mailbox, assert_contains, assert_http, capture_http,
)
from idm.users.models import Group
from idm.tests.models.users.test_group_sync import add_members
from idm.users.sync.groups import deprive_depriving_groups


pytestmark = [pytest.mark.django_db, pytest.mark.usefixtures('mock_fetcher')]


def test_deprive_group_role(simple_system, arda_users, department_structure):
    """Протестируем отзыв групповой роли"""

    fellowship = Group.objects.get(slug='fellowship-of-the-ring')
    frodo = arda_users.frodo
    set_workflow(simple_system, group_code=DEFAULT_WORKFLOW)
    role = Role.objects.request_role(frodo, fellowship, simple_system, '', {'role': 'manager'}, None)
    role = refresh(role)
    clear_mailbox()
    assert role.state == 'granted'
    comment = 'Отзываю роль'
    role.deprive_or_decline(frodo, comment=comment)
    role = refresh(role)
    assert role.state == 'deprived'
    assert_action_chain(role, ['request', 'apply_workflow', 'approve', 'grant', 'deprive', 'remove'])
    deprive_action = role.actions.get(action='deprive')
    assert deprive_action.comment == comment
    legolas_role = Role.objects.get(user__username='legolas')
    assert legolas_role.parent_id == role.id
    assert legolas_role.state == 'deprived'
    assert_action_chain(legolas_role, [
        'request', 'approve', 'first_add_role_push', 'grant', 'deprive', 'first_remove_role_push', 'remove',
    ])
    assert len(mail.outbox) == 1
    message = mail.outbox[0]
    assert message.to == [frodo.email]
    assert message.subject == 'Simple система. Роль отозвана'
    assert_contains([
        'Фродо Бэггинс (frodo) отозвал роль в системе "Simple система" у группы "Братство кольца", '
        'в которой вы являетесь ответственным:',
        'Роль: Менеджер',
        'Комментарий: %s' % comment,
        comment
    ], message.body)


def test_deprive_role_in_system_just_once(generic_system, arda_users, department_structure):
    """Если у пользователя есть обычная роль в системе, а мы выдаём персональную по групповой,
    то при отзыве ролей в систему мы должны сходить только один раз"""

    def forbid(*args, **kwargs):
        raise PluginError(1, 'Role already deprived', {})

    frodo = arda_users.frodo
    set_workflow(generic_system, code='approvers=[]', group_code='approvers = []')
    fellowship = Group.objects.get(slug='fellowship-of-the-ring')
    with patch.object(generic_system.plugin.__class__, '_post_data') as post_data:
        post_data.return_value = {
            'data': {
                'token': 'whoa!'
            }
        }
        role = Role.objects.request_role(frodo, frodo, generic_system, '', {'role': 'manager'}, None)
        group_role = Role.objects.request_role(frodo, fellowship, generic_system, '', {'role': 'manager'}, None)
    assert Role.objects.filter(user=frodo, state='granted').count() == 2
    role = refresh(role)
    clear_mailbox()
    with patch.object(generic_system.plugin.__class__, '_post_data', forbid):
        role.deprive_or_decline(frodo, comment='Роль больше не нужна')
    role = refresh(role)
    assert len(mail.outbox) == 1
    message = mail.outbox[0]
    assert message.to == ['frodo@example.yandex.ru']
    assert message.subject == 'Generic система. Роль отозвана'
    assert_contains([
        'Вы отозвали вашу роль в системе "Generic система":',
        'Роль: Менеджер',
        'Комментарий: Роль больше не нужна'
    ], message.body)
    assert role.state == 'deprived'
    with patch.object(generic_system.plugin.__class__, '_post_data') as post_data:
        post_data.return_value = {}
        group_role.deprive_or_decline(frodo)
    assert Role.objects.exclude(state='deprived').count() == 0


def test_deprive_role_in_system_just_once_vice_versa(generic_system, arda_users, department_structure):
    """Если у пользователя есть персональная роль, выданная по групповой, а пользователь запросил ещё и
    личную, то при отзыве ролей в систему мы должны всё равно сходить только один раз"""

    frodo = arda_users.frodo
    set_workflow(generic_system, code='approvers=[]', group_code='approvers = []')
    fellowship = department_structure.fellowship

    def forbid_frodo(self, method, data, **_):
        if data['login'] == 'frodo':
            raise PluginError(1, 'Role already deprived', {})
        return {}

    with patch.object(generic_system.plugin.__class__, '_post_data') as post_data:
        post_data.return_value = {
            'data': {
                'token': 'whoa!'
            }
        }
        role = Role.objects.request_role(frodo, frodo, generic_system, '', {'role': 'manager'}, None)
        group_role = Role.objects.request_role(frodo, fellowship, generic_system, '', {'role': 'manager'}, None)
    assert Role.objects.filter(user=frodo, state='granted').count() == 2
    role = refresh(role)
    with patch.object(generic_system.plugin.__class__, '_post_data', forbid_frodo):
        group_role.deprive_or_decline(frodo, comment='Отзываю роль')
    assert_action_chain(group_role.refs.get(user=frodo), ['request', 'approve', 'grant', 'deprive', 'remove'])
    clear_mailbox()
    with patch.object(generic_system.plugin.__class__, '_post_data') as post_data:
        post_data.return_value = {}
        role.deprive_or_decline(frodo, comment='Роль больше не нужна')
    assert len(mail.outbox) == 1
    message = mail.outbox[0]
    assert message.subject == 'Generic система. Роль отозвана'
    assert_contains([
        'Вы отозвали вашу роль в системе "Generic система":',
        'Роль: Менеджер',
        'Комментарий: Роль больше не нужна'
    ], message.body)
    assert Role.objects.exclude(state='deprived').count() == 0


def test_depriving_group_role_deprives_roles_for_inactive_users_too(simple_system, arda_users, department_structure):
    """При отзыве групповой роли можно отозвать и роли у неактивного пользователя"""

    frodo = arda_users.frodo
    legolas = arda_users.legolas
    fellowship = Group.objects.get(slug='fellowship-of-the-ring')
    set_workflow(simple_system, group_code=DEFAULT_WORKFLOW)
    role = Role.objects.request_role(frodo, fellowship, simple_system, '', {'role': 'manager'}, None)
    legolas.is_active = False
    legolas.save()
    role.deprive_or_decline(frodo)
    legolas_ref = role.refs.get(user=legolas)
    assert legolas_ref.state == 'deprived'
    assert_action_chain(legolas_ref, [
        'request', 'approve', 'first_add_role_push', 'grant', 'deprive', 'first_remove_role_push', 'remove'
    ])


def test_rerequest_failed_ref(arda_users, simple_system, department_structure):
    """Проверяем, что запрошенная связанная роль, по каким-то причинам не выданная,
    перезапрашивается, вместо заведения новой связанной роли"""

    frodo = arda_users.frodo
    fellowship = department_structure.fellowship
    set_workflow(simple_system, code='error', group_code='approvers = []')
    group_role = Role.objects.request_role(frodo, fellowship, simple_system, '', {'role': 'manager'}, None)
    assert Role.objects.count() == len(fellowship.employees.all()) + 1
    for role in Role.objects.filter(group=None):
        role.state = 'failed'
        role.save(update_fields=['state'])
    group_role.request_refs()
    assert Role.objects.count() == len(fellowship.employees.all()) + 1
    user_role = Role.objects.select_related('system__actual_workflow', 'node', 'user').exclude(user=None).first()
    with pytest.raises(Forbidden) as exc:
        user_role.rerequest(user_role.user)
    assert str(exc.value) == 'Перезапрос роли невозможен: Только роботы могут запрашивать связанные роли'
    for role in Role.objects.filter(group=None):
        role.set_raw_state('failed', is_active=False)
    # Намножим ролей
    n = 2
    for role in Role.objects.filter(group=None):
        for i in range(n):
            role.pk = None
            role.save()
    # Проверяем, что перезапрошено только по одной роли для каждого пользователя
    group_role.request_refs()
    assert Role.objects.filter(state='failed').count() == n * fellowship.employees.count()
    assert Role.objects.filter(group=None, state='granted').count() == fellowship.employees.count()
    # Если среди набора связанных ролей пользователя есть активная, другие не будут перезапрошены
    group_role.request_refs()
    assert Role.objects.filter(state='failed').count() == n * fellowship.employees.count()
    assert Role.objects.filter(group=None, state='granted').count() == fellowship.employees.count()
    personal_role = group_role.refs.get(user__username='legolas', state='granted')
    assert_action_chain(personal_role, ['rerequest', 'approve', 'first_add_role_push', 'grant'])
    rereq_action = personal_role.actions.get(action='rerequest')
    assert rereq_action.comment == 'Reference role re-request'


def test_transition_from_review_request_to_onhold(arda_users, simple_system):
    frodo = arda_users.frodo
    legolas = arda_users.legolas
    set_workflow(simple_system, 'approvers = ["legolas"]', group_code='approvers = ["legolas"]')
    fellowship = Group.objects.get(slug='fellowship-of-the-ring')

    role = Role.objects.request_role(frodo, fellowship, simple_system, '', {'role': 'superuser'}, {})
    request = ApproveRequest.objects.select_related_for_set_decided().get()
    request.set_approved(legolas)
    role.refresh_from_db()

    role.set_state('review_request')
    role.refresh_from_db()
    assert role.state == 'review_request'

    fellowship.mark_depriving()
    fellowship = refresh(fellowship)
    fellowship.deprive()

    role = refresh(role)

    assert role.state == 'onhold'
    assert role.last_request.is_done is True


def test_roles_unhold_on_group_moving(group_roots, flat_arda_users, simple_system):
    """Проверим, что отложенные роли восстанавливаются,
    если удаленная группа восстановлена в другом месте"""

    root_dep = group_roots[0]

    frodo = flat_arda_users.frodo
    initial_data = [
        {
            'id': 101,
            'url': 'middle-earth',
            'name': 'Middle Earth',
        },
        {
            'id': 102,
            'url': 'good-lands',
            'name': 'Lands of Good',
            'parent': {
                'id': 101
            }
        },
        {
            'id': 110,
            'url': 'sheer',
            'name': 'Halflings land',
            'parent': {
                'id': 101
            }
        }
    ]
    memberships_data = []
    add_members(memberships_data, 110, ['frodo'])
    root_dep.fetcher.set_data(('staff', 'group'), initial_data)
    root_dep.fetcher.set_data(('staff', 'groupmembership'), memberships_data)

    root_dep.synchronize()

    sheer = Group.objects.get(slug='sheer')

    set_workflow(simple_system, code='approvers = []', group_code='approvers = []', user=frodo)
    simple_system.request_policy = 'anyone'
    group_role = Role.objects.request_role(frodo, sheer, simple_system, '', {'role': 'manager'}, None)

    assert group_role.state == 'granted'

    # Удаляем группу sheer из импорта
    sheer_deleted_data = [
        {
            'id': 101,
            'url': 'middle-earth',
            'name': 'Middle Earth',
        },
        {
            'id': 102,
            'url': 'good-lands',
            'name': 'Lands of Good',
            'parent': {
                'id': 101
            }
        }
    ]
    root_dep.fetcher.set_data(('staff', 'group'), sheer_deleted_data)
    root_dep.synchronize()

    deprive_depriving_groups(force=True)

    sheer.refresh_from_db()
    assert sheer.state == 'deprived'

    group_role.refresh_from_db()
    assert group_role.state == 'onhold'

    # Возвращаем группу sheer в другое место дерева
    sheer_moved_data = [
        {
            'id': 101,
            'url': 'middle-earth',
            'name': 'Middle Earth',
        },
        {
            'id': 102,
            'url': 'good-lands',
            'name': 'Lands of Good',
            'parent': {
                'id': 101
            }
        },
        {
            'id': 110,
            'url': 'sheer',
            'name': 'Halflings land',
            'parent': {
                'id': 102
            }
        }
    ]
    root_dep.fetcher.set_data(('staff', 'group'), sheer_moved_data)
    root_dep.synchronize()

    sheer.refresh_from_db()
    assert sheer.state == 'active'

    group_role.refresh_from_db()
    assert group_role.state == 'granted'

    call_command('idm_poke_hanging_roles')

    for role in group_role.refs.all():
        assert role.state == 'granted'


def test_deprive_group_role_with_awaiting_refs(pt1_system, arda_users, department_structure):
    """Протестируем отзыв групповой роли"""

    fellowship = Group.objects.get(slug='fellowship-of-the-ring')
    frodo = arda_users.frodo
    set_workflow(pt1_system, group_code=DEFAULT_WORKFLOW)
    role = Role.objects.request_role(frodo, fellowship, pt1_system, '', {'project': 'proj1', 'role': 'admin'}, None)
    assert role.state == 'granted'
    legolas_role = Role.objects.get(user__username='legolas')
    assert legolas_role.state == 'awaiting'  # к членству не прикреплён логин

    role.deprive_or_decline(frodo, comment='')
    role = refresh(role)
    assert role.state == 'deprived'
    assert_action_chain(role, ['request', 'apply_workflow', 'approve', 'grant', 'deprive', 'remove'])

    legolas_role = refresh(legolas_role)
    assert legolas_role.state == 'declined'
    assert_action_chain(legolas_role, ['request', 'approve', 'await', 'decline'])


@pytest.mark.parametrize('group_state', ['active', 'depriving', 'deprived'])
def test_deprive_group_role_when_group_is_removed(aware_generic_system, arda_users, department_structure, group_state):
    """Протестируем отзыв групповой роли"""

    fellowship = Group.objects.get(slug='fellowship-of-the-ring')
    frodo = arda_users.frodo
    aware_generic_system.fetch_actual_workflow()
    set_workflow(aware_generic_system, group_code=DEFAULT_WORKFLOW)

    return_value = {'code': 0}

    with capture_http(aware_generic_system, return_value):
        role = Role.objects.request_role(frodo, fellowship, aware_generic_system, '', {'role': 'admin'}, None)
    role.refresh_from_db()
    assert role.state == 'granted'
    fellowship.state = group_state
    fellowship.save(update_fields=['state'])

    data = {
        'group': fellowship.external_id,
        'role': '{"role": "admin"}',
        'path': '/role/admin/',
        'fields': 'null',
    }
    if group_state in ['depriving', 'deprived']:
        data['deleted'] = 1
    with capture_http(aware_generic_system, return_value) as sender:
        role.deprive_or_decline(frodo, comment='')
        assert_http(sender.http_post, url='http://example.com/remove-role/', data=data, timeout=60)
    role.refresh_from_db()
    assert role.state == 'deprived'
