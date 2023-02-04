# coding: utf-8


import itertools
from mock import patch
import pytest
from django.core.management import call_command
from django.utils import timezone

from idm.core.constants.groupmembership import GROUPMEMBERSHIP_STATE
from idm.core.constants.role import ROLE_STATE
from idm.core.constants.system import SYSTEM_GROUP_POLICY
from idm.core.models import Role, RoleField
from idm.permissions.utils import add_perms_by_role
from idm.tests.utils import assert_contains
from idm.users.models import GroupMembership
from idm.utils import reverse

from idm.tests.utils import refresh, set_workflow, DEFAULT_WORKFLOW, capture_http

pytestmark = pytest.mark.django_db


def get_bb_with_response(response):
    class Blackbox(object):
        def __init__(self, *args, **kwargs):
            pass

        def userinfo(self, *args, **kwargs):
            return response
    return Blackbox


@pytest.fixture
def membership_url():
    return reverse('api_dispatch_list', api_name='frontend', resource_name='membership')


def as_pairs(response):
    return [(membership['user']['username'], membership['group']['id']) for membership in response['objects']]


def test_user_group_filter(client, arda_users, department_structure, membership_url):
    """
    GET /frontend/membership/?direction=down
    """
    client.login('frodo')
    fellowship = department_structure.fellowship

    response = client.json.get(membership_url, {'group': fellowship.external_id})
    assert response.status_code == 200
    data = response.json()
    expected_logins = ['aragorn', 'boromir', 'frodo', 'gandalf', 'gimli', 'legolas', 'meriadoc', 'peregrin', 'sam']
    assert as_pairs(data) == list(zip(expected_logins, itertools.repeat(fellowship.external_id)))

    data = client.json.get(
        membership_url,
        {'group': '%s,%s' % (fellowship.external_id, department_structure.valinor.external_id)}
    ).json()
    expected_logins = {'aragorn', 'boromir', 'frodo', 'gandalf', 'gimli', 'legolas', 'meriadoc', 'peregrin', 'sam',
                       'varda', 'manve'}
    assert {membership['user']['username'] for membership in data['objects']} == expected_logins

    response = client.json.get(membership_url, {'user': 'sam', 'direction': 'down'})
    assert response.status_code == 200
    data = response.json()
    expected_groups = [department_structure.shire.external_id,
                       department_structure.fellowship.external_id]
    assert as_pairs(data) == list(zip(itertools.repeat('sam'), expected_groups))

    # проверяем несколько пользователей
    response = client.json.get(membership_url, {'user': 'sam,varda', 'direction': 'down'})
    assert response.status_code == 200
    data = response.json()
    expected_groups = [('sam', department_structure.shire.external_id),
                       ('sam', department_structure.fellowship.external_id),
                       ('varda', department_structure.valinor.external_id)]
    assert as_pairs(data) == expected_groups

    # проверяем тип группы
    expected_groups = [('sam', department_structure.shire.external_id),
                       ('sam', department_structure.fellowship.external_id)]
    data = client.json.get(membership_url, {'user': 'sam', 'group__type': 'department', 'direction': 'down'}).json()
    assert as_pairs(data) == expected_groups

    data = client.json.get(membership_url, {'user': 'sam', 'group__type': 'wiki', 'direction': 'down'}).json()
    assert len(data['objects']) == 0

    data = client.json.get(membership_url,
                           {'user': 'sam', 'group__type': 'wiki,department', 'direction': 'down'}).json()
    assert as_pairs(data) == expected_groups

    # проверяем неправильные параметры
    response = client.json.get(membership_url)
    assert response.status_code == 400

    response = client.json.get(membership_url, {'user': 'invalid'})
    assert response.status_code == 400

    response = client.json.get(membership_url, {'group': 'invalid'})
    assert response.status_code == 400


def test_group_mode(client, arda_users, department_structure, membership_url):
    """
    GET /frontend/membership/?group=X&mode=Y&direction=down
    """
    client.login('frodo')
    associations = department_structure.associations
    fellowship = department_structure.fellowship
    associations_memberships = [('sauron', associations.external_id)]
    fellowship_logins = ['aragorn', 'boromir', 'frodo', 'gandalf', 'gimli', 'legolas', 'meriadoc', 'peregrin', 'sam']
    fellowship_memberships = [(username, fellowship.external_id) for username in fellowship_logins]
    sauron_associations_memberships = [('sauron', associations.external_id)]
    frodo_fellowship_memberships = [('frodo', fellowship.external_id)]

    # Вызов с mode=all работает так же, как и без указания режима
    response = client.json.get(membership_url, {'group': associations.external_id, 'direction': 'down'})
    assert response.status_code == 200
    data = response.json()
    expected = sorted(associations_memberships + fellowship_memberships, key=lambda item: item[0])
    assert as_pairs(data) == expected

    # проверим all
    response = client.json.get(membership_url,
                               {'group': associations.external_id, 'mode': 'all', 'direction': 'down'}).json()
    assert as_pairs(response) == expected

    # вызов с mode=direct выдаст только членства в конкретной группе
    response = client.json.get(membership_url,
                               {'group': associations.external_id, 'mode': 'direct', 'direction': 'down'})
    assert response.status_code == 200
    data = response.json()
    assert as_pairs(data) == associations_memberships

    data = client.json.get(membership_url, {'user': 'frodo,sauron', 'group': associations.external_id,
                                            'direction': 'down'}).json()
    assert as_pairs(data) == frodo_fellowship_memberships + sauron_associations_memberships

    data = client.json.get(membership_url, {'user': 'frodo,sauron', 'group': associations.external_id,
                                            'mode': 'direct', 'direction': 'down'}).json()
    assert as_pairs(data) == sauron_associations_memberships


def test_get_inactive_users(client, arda_users, department_structure, membership_url):
    """
    GET /frontend/membership/?group=X&is_active=Y
    """
    client.login('frodo')
    fellowship = department_structure.fellowship

    GroupMembership.objects.filter(
        user=arda_users.frodo
    ).update(date_leaved=timezone.now(), state=GROUPMEMBERSHIP_STATE.INACTIVE)

    data = client.json.get(membership_url, {'group': fellowship.external_id}).json()
    expected_logins = ['aragorn', 'boromir', 'gandalf', 'gimli', 'legolas', 'meriadoc', 'peregrin', 'sam']
    assert as_pairs(data) == list(zip(expected_logins, itertools.repeat(fellowship.external_id)))

    data = client.json.get(membership_url, {'group': fellowship.external_id, 'is_active': False}).json()
    assert as_pairs(data) == [('frodo', fellowship.external_id)]


def test_user_mode(client, arda_users, department_structure, membership_url):
    """
    GET /frontend/membership/?user=X&mode=Y
    """
    client.login('frodo')
    memberships = [('frodo', group_id) for group_id in (department_structure.valinor.external_id,
                                                        department_structure.earth.external_id,
                                                        department_structure.lands.external_id,
                                                        department_structure.associations.external_id,
                                                        department_structure.fellowship.external_id,
                                                        department_structure.shire.external_id)]

    # Вызов с mode=all работает так же, как и без указания режима
    response = client.json.get(membership_url, {'user': arda_users.frodo, 'direction': 'up'})
    assert response.status_code == 200
    data = response.json()
    assert as_pairs(data) == memberships

    # явно укажем all
    response = client.json.get(membership_url, {'user': arda_users.frodo, 'mode': 'all', 'direction': 'up'})
    assert response.status_code == 200
    data = response.json()
    assert as_pairs(data) == memberships

    # вызов с mode=direct выдаст только членства в "нижних" группах
    response = client.json.get(membership_url, {'user': 'frodo', 'mode': 'direct', 'direction': 'up'})
    assert response.status_code == 200
    data = response.json()
    assert as_pairs(data) == [('frodo', group.external_id) for group in (
        department_structure.valinor,
        department_structure.fellowship,
        department_structure.shire,
    )]

    # фильтр по группе работает
    data = client.json.get(membership_url, {'user': arda_users.frodo,
                                            'group': [department_structure.valinor.external_id,
                                                      department_structure.shire.external_id],
                                            'direction': 'up'}).json()
    assert as_pairs(data) == [('frodo', department_structure.valinor.external_id),
                              ('frodo', department_structure.shire.external_id)]


def test_passport_login_in_memberships(client, arda_users, department_structure, membership_url, pt1_system):
    frodo, sam = arda_users.frodo, arda_users.sam
    shire = department_structure.shire
    frodo.active_responsibilities.delete()

    client.login('frodo')
    data = client.json.get(membership_url, {'user': frodo, 'direction': 'up'}).json()

    # Логинов не привязано
    assert {gm['passport_login'] for gm in data['objects']} == {None}
    # Юзер может привязать логин, только если входит в группу
    expected = [True, True, True, True, True, True]
    assert [gm['requester_can_assign_passport_login'] for gm in data['objects']] == expected

    # Создадим паспортный логин для frodo и привяжем его к членству frodo в Shire
    Role.objects.request_role(frodo, frodo, pt1_system, '', {'project': 'proj1', 'role': 'admin'},
                              {'passport-login': 'yndx-frodo'})

    gm_shire = frodo.memberships.get(group=shire)
    gm_shire.passport_login = frodo.passport_logins.get()
    gm_shire.save()

    data = client.json.get(membership_url, {'user': frodo, 'direction': 'up'}).json()

    # Видим в членстве в Shire привязанный логин
    gm_shire_data = [gm for gm in data['objects'] if gm['group']['slug'] == shire.slug]
    assert len(gm_shire_data) == 1
    assert gm_shire_data[0]['passport_login']['id'] == gm_shire.passport_login.pk

    # В других членствах логина нет
    gm_other_data = [gm for gm in data['objects'] if gm['group']['slug'] != shire.slug]
    assert len(gm_other_data) == 5
    expected = [None] * len(gm_other_data)
    assert [gm['passport_login'] for gm in gm_other_data] == expected
    client.logout()

    client.login('sam')
    data = client.json.get(membership_url, {'user': frodo, 'direction': 'up'}).json()

    # Посторонний не может привязать логин
    assert [gm['requester_can_assign_passport_login'] is False for gm in data['objects']]

    # Дадим постороннему прав на привязку паспортного логина
    add_perms_by_role('passport_login_manager', sam)
    data = client.json.get(membership_url, {'user': frodo, 'direction': 'up'}).json()

    # Появляется возможность привязать логин к группам, куда юзер входит
    expected = [True, True, True, True, True, True]
    assert [gm['requester_can_assign_passport_login'] for gm in data['objects']] == expected

    # Проверим страницу группы ('direction': 'down')
    data = client.json.get(membership_url, {'group': shire.external_id, 'direction': 'down'}).json()

    # Логин привязан только у frodo
    gm_frodo_data = [gm for gm in data['objects'] if gm['user']['username'] == frodo.username]
    assert len(gm_frodo_data) == 1
    assert gm_frodo_data[0]['passport_login']['id'] == gm_shire.passport_login.pk

    gm_other_data = [gm for gm in data['objects'] if gm['user']['username'] != frodo.username]
    assert len(gm_other_data) == 4
    expected = [None] * len(gm_other_data)
    assert [gm['passport_login'] for gm in gm_other_data] == expected

    # Членство в группе Shire непосредственное, пермишн позволяет привязать логин каждого
    assert all([gm['requester_can_assign_passport_login'] for gm in data['objects']])

    client.logout()
    client.login('frodo')
    data = client.json.get(membership_url, {'group': shire.external_id, 'direction': 'down'}).json()

    # К своему членству можно привязать логин
    gm_frodo_data = [gm for gm in data['objects'] if gm['user']['username'] == frodo.username]
    assert gm_frodo_data[0]['requester_can_assign_passport_login'] is True

    # Без пермишна к чужим членствам логины привязать нельзя
    gm_other_data = [gm for gm in data['objects'] if gm['user']['username'] != frodo.username]
    assert len(gm_other_data) == 4
    expected = [False] * len(gm_other_data)
    assert [gm['requester_can_assign_passport_login'] for gm in gm_other_data] == expected


def test_passport_login_assignment(client, arda_users, department_structure, membership_url, pt1_system):
    frodo, sam = arda_users.frodo, arda_users.sam
    shire = department_structure.shire

    client.login('frodo')

    gm_shire = frodo.memberships.get(group=shire)
    gm_shire_url = membership_url + str(gm_shire.pk) + '/'

    assert frodo.passport_logins.count() == 0
    assert gm_shire.passport_login is None

    # Создадим логин через API
    response = client.json.patch(gm_shire_url, {'passport_login': 'yndx-frodo'})
    assert response.status_code == 202
    assert frodo.passport_logins.count() == 1
    gm_shire.refresh_from_db()
    gm_shire.fetch_passport_login()
    assert gm_shire.passport_login == frodo.passport_logins.get()

    # Привяжем тот же самый логин, что уже привязан, ничего измениться не должно
    response = client.json.patch(gm_shire_url, {'passport_login': 'yndx-frodo'})
    assert response.status_code == 304
    assert frodo.passport_logins.count() == 1

    data = client.json.get(membership_url, {'user': frodo, 'direction': 'up'}).json()

    # Видим в членстве в Shire привязанный логин
    gm_shire_data = [gm for gm in data['objects'] if gm['group']['slug'] == shire.slug]
    assert len(gm_shire_data) == 1
    assert gm_shire_data[0]['passport_login']['id'] == gm_shire.passport_login.pk

    # Отвяжем логин
    response = client.json.patch(gm_shire_url, {'passport_login': ''})
    assert response.status_code == 202
    assert frodo.passport_logins.count() == 1
    gm_shire.refresh_from_db()
    assert gm_shire.passport_login is None

    client.logout()

    # Посторонний привязать логин не может
    client.login('sam')
    response = client.json.patch(gm_shire_url, {'passport_login': 'yndx-frodo'})
    assert response.status_code == 403
    # Дадим прав на привязку логина
    add_perms_by_role('passport_login_manager', sam)
    response = client.json.patch(gm_shire_url, {'passport_login': 'yndx-frodo'})
    assert response.status_code == 202
    gm_shire.refresh_from_db()
    gm_shire.fetch_passport_login()
    assert gm_shire.passport_login == frodo.passport_logins.get()

    # Создадим логин
    response = client.json.patch(gm_shire_url, {'passport_login': 'another-frodo'})
    assert response.status_code == 202
    assert frodo.passport_logins.count() == 2
    gm_shire.refresh_from_db()
    gm_shire.fetch_passport_login()
    assert gm_shire.passport_login == frodo.passport_logins.get(login='another-frodo')

    # Чужой логин привязать нельзя
    sam_gm_shire = sam.memberships.get(group=shire)
    sam_gm_shire_url = membership_url + str(sam_gm_shire.pk) + '/'
    response = client.json.patch(sam_gm_shire_url, {'passport_login': 'yndx-frodo'})
    assert response.status_code == 400

    client.logout()

    # Руководителю не требуется пермишен для привязки логина сотрудника
    client.login('varda')
    response = client.json.patch(gm_shire_url, {'passport_login': 'ya-frodo'})
    assert response.status_code == 202
    assert frodo.passport_logins.count() == 3
    gm_shire.refresh_from_db()
    gm_shire.fetch_passport_login()
    assert gm_shire.passport_login == frodo.passport_logins.get(login='ya-frodo')

    # Отвяжем логин
    response = client.json.patch(gm_shire_url, {'passport_login': ''})
    response.status_code = 202
    gm_shire.refresh_from_db()
    gm_shire.fetch_passport_login()
    assert gm_shire.passport_login is None

    # Попробуем привязать невалидные логины
    for login in ['3trash-login', '.trash-login', '-trash-login', 'trash--login', 'trash-.login',
                  'trash.-login', 'trash-login.', 'trash-login-', 'trash@login']:
        response = client.json.patch(gm_shire_url, {'passport_login': login})
        assert response.status_code == 400
        assert_contains(['Логин может содержать только латиницу'], response.content.decode('utf-8'))
    response = client.json.patch(gm_shire_url, {'passport_login': 'long-long-too-long-passport-login'})
    assert response.status_code == 400
    assert_contains(['Логин не может быть длиннее'], response.content.decode('utf-8'))


@pytest.mark.parametrize('login_exists', [False, True])
@pytest.mark.parametrize('register_via', ['manual', 'command'])
def test_assignment_pokes_awaiting_roles(client, arda_users, department_structure, membership_url, pt1_system, register_via, login_exists):
    client.login('frodo')

    frodo = arda_users.frodo
    gandalf = arda_users.gandalf
    for login in ['frodo-login1', 'frodo-login2']:
        frodo.passport_logins.create(login=login, state='created', is_fully_registered=False)
    for login in ['gandalf-login1', 'gandalf-login2']:
        gandalf.passport_logins.create(login=login, state='created', is_fully_registered=True)

    if login_exists:
        frodo.passport_logins.create(login='frodo-newlogin', state='created', is_fully_registered=False)
        gandalf.passport_logins.create(login='gandalf-newlogin', state='created', is_fully_registered=True)

    group = department_structure.fellowship
    set_workflow(pt1_system, group_code=DEFAULT_WORKFLOW)
    members_count = group.members.count()

    group_role = Role.objects.request_role(frodo, group, pt1_system, '', {'project': 'proj1', 'role': 'admin'}, None)
    assert Role.objects.count() == 1 + members_count
    assert Role.objects.filter(user=None).count() == 1
    assert Role.objects.filter(group=None).count() == members_count
    assert group_role.state == 'granted'
    frodo_role = Role.objects.get(user=frodo)
    gandalf_role = Role.objects.get(user=gandalf)
    assert frodo_role.state == 'awaiting'  # У членства не выбран логин.
    assert gandalf_role.state == 'awaiting'  # Аналогично
    assert not frodo_role.passport_logins.exists()
    assert not gandalf_role.passport_logins.exists()

    # Добавим для Гендальфа логин через API
    client.login('gandalf')
    gandalf_membership = gandalf.memberships.get(group=group)
    assert gandalf_membership.passport_login is None
    url = membership_url + str(gandalf_membership.pk) + '/'
    response = client.json.patch(url, {'passport_login': 'gandalf-newlogin'})
    assert response.status_code == 202
    gandalf_membership = refresh(gandalf_membership)
    gandalf_login = gandalf.passport_logins.get(login='gandalf-newlogin')
    assert gandalf_membership.passport_login_id == gandalf_login.id

    if not login_exists:
        gandalf_role = refresh(gandalf_role)
        assert gandalf_role.state == 'awaiting'
        assert gandalf_role.passport_logins.get() == gandalf_login
        gandalf.passport_logins.update(is_fully_registered=True)
        Role.objects.poke_awaiting_roles()
    gandalf_role = refresh(gandalf_role)
    assert gandalf_role.state == 'granted'
    assert gandalf_role.passport_logins.get() == gandalf_login

    # Добавим для Фродо логин через API
    client.login('frodo')
    frodo_membership = frodo.memberships.get(group=group)
    assert frodo_membership.passport_login is None
    url = membership_url + str(frodo_membership.pk) + '/'
    response = client.json.patch(url, {'passport_login': 'frodo-newlogin'})
    assert response.status_code == 202
    frodo_membership = refresh(frodo_membership)
    frodo_login = frodo.passport_logins.get(login='frodo-newlogin')
    assert frodo_membership.passport_login_id == frodo_login.id

    frodo_role = refresh(frodo_role)
    frodo_login = refresh(frodo_login)
    assert frodo_role.state == 'awaiting'  # А вот у Фродо логин недореган
    assert frodo_role.passport_logins.get() == frodo_login
    if register_via == 'manual':
        frodo.passport_logins.update(is_fully_registered=True)
        Role.objects.poke_awaiting_roles()
    elif register_via == 'command':
        with patch('idm.sync.passport.get_external_blackbox') as bb, \
                capture_http(pt1_system, {'code': 0}):
            bb.side_effect = get_bb_with_response({
                'attributes': {
                    '1005': '1'
                }
            })
            call_command('idm_check_awaiting_logins')

    frodo_role = refresh(frodo_role)
    if register_via == 'command':
        assert frodo_role.state == 'awaiting'
        call_command('idm_check_awaiting_roles')
        frodo_role.refresh_from_db()

    assert frodo_role.state == 'granted'
    assert frodo_role.passport_logins.get() == frodo_login


def test_login_multiple_roles(client, arda_users, department_structure, membership_url, pt1_system, simple_system):
    for system in (simple_system, pt1_system):
        set_workflow(system, group_code=DEFAULT_WORKFLOW)

    RoleField.objects.filter(node__system=simple_system).delete()

    frodo = arda_users.frodo
    login = frodo.passport_logins.create(login='frodo1', state='created', is_fully_registered=True)
    new_login = frodo.passport_logins.create(login='frodo2', state='created', is_fully_registered=True)

    group = department_structure.fellowship
    frodo_membership = frodo.memberships.get(group=group)
    frodo_membership.passport_login = login
    frodo_membership.save()

    group_role_1 = Role.objects.request_role(frodo, group, pt1_system, '', {'project': 'proj1', 'role': 'admin'}, None)
    group_role_2 = Role.objects.request_role(frodo, group, pt1_system, '', {'project': 'proj1', 'role': 'manager'}, None)
    group_role_3 = Role.objects.request_role(frodo, group, simple_system, '', {'role': 'manager'}, None)

    personal_role_1 = frodo.roles.active().get(parent=group_role_1)
    personal_role_2 = frodo.roles.active().get(parent=group_role_2)
    personal_role_3 = frodo.roles.active().get(parent=group_role_3)
    assert personal_role_1.state == personal_role_2.state == personal_role_3.state == ROLE_STATE.GRANTED

    client.login('frodo')
    url = membership_url + str(frodo_membership.pk) + '/'
    response = client.json.patch(url, {'passport_login': 'frodo2'})
    assert response.status_code == 202

    for r in (personal_role_1, personal_role_2):
        r.refresh_from_db()
        assert r.state == ROLE_STATE.DEPRIVED

    personal_role_3.refresh_from_db()
    assert personal_role_3.state == ROLE_STATE.GRANTED

    new_personal_role_1 = frodo.roles.active().get(parent=group_role_1)
    new_personal_role_2 = frodo.roles.active().get(parent=group_role_2)
    assert new_personal_role_1.state == new_personal_role_2.state == ROLE_STATE.GRANTED


@pytest.mark.parametrize('login_exists', [False, True])
def test_login_change_rerequests_roles(client, arda_users, department_structure, membership_url, pt1_system, login_exists):
    client.login('frodo')

    frodo = arda_users.frodo
    gandalf = arda_users.gandalf
    frodo_logins = [
        frodo.passport_logins.create(login=login, state='created', is_fully_registered=True)
        for login in ['frodo-login1', 'frodo-login2']
    ]
    gandalf_logins = [
        gandalf.passport_logins.create(login=login, state='created', is_fully_registered=True)
        for login in ['gandalf-login1', 'gandalf-login2']
    ]
    if login_exists:
        gandalf.passport_logins.create(login='gandalf-newlogin', state='created', is_fully_registered=True)

    group = department_structure.fellowship
    set_workflow(pt1_system, group_code=DEFAULT_WORKFLOW)
    members_count = group.members.count()

    gandalf_membership = gandalf.memberships.get(group=group)
    gandalf_membership.passport_login = gandalf_logins[0]
    gandalf_membership.save()
    frodo_membership = frodo.memberships.get(group=group)
    frodo_membership.passport_login = frodo_logins[0]
    frodo_membership.save()

    group_role = Role.objects.request_role(frodo, group, pt1_system, '', {'project': 'proj1', 'role': 'admin'}, None)
    assert Role.objects.count() == 1 + members_count
    assert Role.objects.filter(user=None).count() == 1
    assert Role.objects.filter(group=None).count() == members_count
    assert group_role.state == 'granted'
    frodo_role = Role.objects.get(user=frodo)
    gandalf_role = Role.objects.get(user=gandalf)
    assert frodo_role.state == 'granted'  # У членства не выбран логин.
    assert gandalf_role.state == 'granted'  # Аналогично
    assert frodo_role.passport_logins.get() == frodo_logins[0]
    assert gandalf_role.passport_logins.get() == gandalf_logins[0]

    # Поменяем для Гендальфа логин через API
    client.login('gandalf')
    url = membership_url + str(gandalf_membership.pk) + '/'
    response = client.json.patch(url, {'passport_login': 'gandalf-newlogin'})
    assert response.status_code == 202
    newlogin = gandalf.passport_logins.get(login='gandalf-newlogin')
    gandalf_membership = refresh(gandalf_membership)
    assert gandalf_membership.passport_login_id == newlogin.id
    gandalf_role = refresh(gandalf_role)
    assert gandalf_role.passport_logins.get() == gandalf_logins[0]
    assert gandalf_role.state == 'deprived'
    assert gandalf.roles.count() == 2
    new_gandalf_role = Role.objects.order_by('-added').first()
    assert new_gandalf_role != gandalf_role
    if not login_exists:
        new_gandalf_role = refresh(new_gandalf_role)
        assert new_gandalf_role.state == 'awaiting'
        assert new_gandalf_role.passport_logins.get() == newlogin
        gandalf.passport_logins.update(is_fully_registered=True)
        Role.objects.poke_awaiting_roles()
    new_gandalf_role = refresh(new_gandalf_role)
    assert new_gandalf_role.state == 'granted'
    assert new_gandalf_role.passport_logins.get() == newlogin

    # Удалим у Фродо логин через API
    client.login('frodo')
    url = membership_url + str(frodo_membership.pk) + '/'
    response = client.json.patch(url, {'passport_login': ''})
    assert response.status_code == 202
    frodo_membership = refresh(frodo_membership)
    assert frodo_membership.passport_login is None
    frodo_role = refresh(frodo_role)
    assert frodo_role.passport_logins.get() == frodo_logins[0]
    assert frodo_role.state == 'deprived'
    assert frodo.roles.count() == 2
    new_frodo_role = Role.objects.order_by('-added').first()
    assert new_frodo_role != frodo_role
    assert new_frodo_role.state == 'awaiting'
    assert not new_frodo_role.passport_logins.exists()


def test_assignment_pokes_awaiting_personal_roles(arda_users, pt1_system):

    frodo = arda_users.frodo
    frodo.passport_logins.create(login='yndx-frodo', state='created', is_fully_registered=False)
    pt1_system.group_policy = SYSTEM_GROUP_POLICY.AWARE_OF_MEMBERSHIPS_WITH_LOGINS
    pt1_system.save()

    frodo_role = Role.objects.request_role(
        frodo, frodo, pt1_system, '',
        {'project': 'proj1', 'role': 'admin'},
        {'passport-login': 'yndx_frodo'}
    )

    frodo_role.refresh_from_db()
    assert frodo_role.state == ROLE_STATE.AWAITING  # Логин не дорегистрирован

    frodo.passport_logins.update(is_fully_registered=True)
    Role.objects.poke_awaiting_roles()
    frodo_role.refresh_from_db()

    assert frodo_role.state == ROLE_STATE.GRANTED
