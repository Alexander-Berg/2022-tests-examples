# -*- coding: utf-8 -*-


import pytest
import requests
from django.core.management import call_command
from django.test.utils import mail
from django.utils import timezone
from mock import patch

from idm.core.constants.groupmembership import GROUPMEMBERSHIP_STATE
from idm.core.models import Action, Role
from idm.sync.staff import users
from idm.tests.utils import (
    create_user, refresh, create_fake_response, members_created,
    make_role, assert_action_chain, clear_mailbox, assert_contains,
)
from idm.users import signals
from idm.users.constants.user import USER_TYPES
from idm.users.models import Group, GroupMembership, User
from idm.utils import json

# разрешаем использование базы в тестах
pytestmark = [pytest.mark.django_db]


def staff_response(userlist):
    return {'limit': 50,
            'links': {},
            'page': 1,
            'pages': 1,
            'result': userlist,
            'total': len(userlist),
            }


@pytest.fixture
def art():
    """
    Фикстура для тестов, создает пользователя "art"
    """
    return create_user('art')


@pytest.fixture
def fantom():
    """
    Фикстура для тестов, создает пользователя "fantom"
    """
    return create_user('fantom')


@pytest.fixture
def admin():
    """
    Фикстура для тестов, создает суперпользователя "admin"
    """
    return create_user('admin', superuser=True)


@pytest.fixture
def robot():
    """
    Создает робота
    """
    return create_user('robot', is_robot=True)


@pytest.fixture
def other_robot():
    """
    Создает робота
    """
    return create_user('other_robot', is_robot=True)


def user_dict(user):
    """Возвращает словарик годный для формирования JSON ответа, имитирующего
    ответ Staff API
    """
    return {
        'uid': user.uid,
        'department_group': {
            'id': getattr(user.department_group, 'external_id', None),
        },
        'id': user.center_id,
        'language': {
            'ui': 'ru',
        },
        'location': {
            'office': {
                'id': 150,
            },
        },
        'login': user.username,
        'name': {
            'first': {
                'en': user.first_name_en,
                'ru': user.first_name,
            },
            'last': {
                'en': user.last_name_en,
                'ru': user.last_name,
            },
        },
        'official': {
            'affiliation': user.affiliation,
            'is_dismissed': not user.is_active,
            'is_robot': user.is_robot,
            'join_at': '2011-05-05',
            'position': {
                'ru': 'менеджер среднего звена',
            },
            'quit_at': None,
        },
        'personal': {
            'gender': 'male',
        },
        'phones': [
            {
                'number': '+71234567890',
                'type': 'home',
            },
            {
                'number': '+70987654321, +71231234567',
                'type': 'mobile',
            },
        ],
        'robot_owners': [
            {
                'person': {
                'id': pk,
                },
            } for pk in user.responsibles.values_list('staff_id', flat=True)
        ],
        'work_email': user.email,
    }


def add_users_to_department_group(users_data, external_id, usernames):
    for username in usernames:
        user = create_user(username)
        user_from_staff = user_dict(user)
        user_from_staff['department_group']['id'] = external_id
        users_data.append(user_from_staff)


def move_members(users_data, from_group, to_group, usernames):
    for user in users_data:
        if user['department_group']['id'] == from_group and user['login'] in usernames:
            user['department_group']['id'] = to_group


def test_import_users(art):

    assert art.position == ''
    assert art.mobile_phone is None

    with patch.object(requests.sessions.Session, 'request') as get:
        art_usr_dct = user_dict(art)

        get.return_value = create_fake_response(json.dumps(
            staff_response([art_usr_dct])
        ))
        users.import_users()

        art = refresh(art)

        assert art.position == 'менеджер среднего звена'
        assert art.mobile_phone == '+70987654321'


def test_import_users_with_empty_phones(art, fantom):

    with patch.object(requests.sessions.Session, 'request') as get:
        fantom_dict = user_dict(fantom)
        fantom_dict['phones'] = None

        get.return_value = create_fake_response(json.dumps(
            staff_response([
                user_dict(art),
                fantom_dict,
            ]
            )))
        users.import_users()

        assert refresh(art).mobile_phone == '+70987654321'
        assert refresh(fantom).mobile_phone is None


@pytest.mark.parametrize('is_robot', [True, False])
def test_import_notify_responsibles(robot, is_robot):
    robot_login = robot.username
    assert robot.notify_responsibles is False
    with patch.object(requests.sessions.Session, 'request') as get:
        robot_data = user_dict(robot)
        robot_data['official']['is_robot'] = is_robot
        get.return_value = create_fake_response(json.dumps(staff_response([robot_data])))
        robot.delete()
        users.import_users()
    robot = User.objects.get(username=robot_login)
    assert robot.notify_responsibles is is_robot


def test_robot_owners(art, fantom, robot, other_robot, django_assert_num_queries):
    # один раз синхронизируем, чтобы потом не было лишних запросов
    with patch.object(requests.sessions.Session, 'request') as get:
        get.return_value = create_fake_response(
            json.dumps(
                staff_response(
                    [user_dict(u) for u in (art, fantom, robot, other_robot)]
                )
            )
        )
        users.import_users()

    robot.add_responsibles(art)
    assert robot.responsibles.get() == art

    last_action = robot.owns.order_by('pk').last()
    assert (last_action.user_id, last_action.action) == (art.id, 'robot_responsible_added')

    staff_response_dicts = []
    for user in (other_robot, robot):
        robot_dict = user_dict(user)
        # Добавляем ответственного в staff-api
        robot_dict['robot_owners'].append({'person': {'id': fantom.staff_id}})
        staff_response_dicts.append(robot_dict)
    staff_response_dicts.extend([user_dict(art), user_dict(fantom),])

    with patch.object(requests.sessions.Session, 'request') as get:
        get.return_value = create_fake_response(json.dumps(staff_response(staff_response_dicts)))
        with django_assert_num_queries(12):
            # Получение всех пользователей
            # Получение всех ответственных
            # Savepoint
            # Добавление ответственного для двух роботов x2
            # # Проверка, что такой ответственности нет
            # # Создание ответственности
            # # Создание экшена про ответственность
            # Release Saveopint

            users.import_users()

    assert set(robot.responsibles.all()) == {art, fantom}
    last_action = robot.owns.order_by('pk').last()
    assert (last_action.user_id, last_action.action) == (fantom.id, 'robot_responsible_added')

    # Оставим только одного ответственного в staff-api
    robot_dict['robot_owners'] = [{'person': {'id': fantom.staff_id}}]

    with patch.object(requests.sessions.Session, 'request') as get:
        get.return_value = create_fake_response(json.dumps(
            staff_response([
                user_dict(art),
                user_dict(fantom),
                robot_dict,
            ]
            )))
        users.import_users()

    assert set(robot.responsibles.all()) == {fantom}
    last_action = robot.owns.order_by('pk').last()
    assert (last_action.user_id, last_action.action) == (art.id, 'robot_responsible_removed')


def test_change_department(arda_users, department_structure):
    """Тестирование смены подразделения"""

    data = [
        {
            'user': arda_users.bilbo,
            'new_group': department_structure.lands,
            'old_group': department_structure.shire,
        },
        {
            'user': arda_users.boromir,
            'new_group': department_structure.valinor,
            'old_group': department_structure.fellowship,
        },
        {
            'user': arda_users.gimli,
            'new_group': department_structure.shire,
            'old_group': department_structure.fellowship,
        }
    ]

    for membership in data:
        membership['new_group'].add_members([membership['user']])
        new_membership = GroupMembership.objects.select_related('user', 'user__department_group').get(
            user=membership['user'],
            group=membership['new_group'],
            is_direct=True,
        )

        signals.memberships_added.send(
            sender=membership['user'].department_group,
            memberships=[new_membership],
            group=membership['new_group']
        )

    assert Action.objects.filter(action='user_change_department').count() == len(data)

    for action, membership in zip(Action.objects.filter(action='user_change_department').order_by('added'), data):
        assert action.user_id == membership['user'].id
        assert action.data == {
            'department_from': membership['old_group'].id,
            'department_to': membership['new_group'].id,
            'dep_name_from': 'Не указан' if membership['old_group'].name is None else membership['old_group'].name,
            'dep_name_to': membership['new_group'].name,
        }


def test_change_department_group(arda_users, department_structure):
    frodo = arda_users.frodo
    old_department_group = frodo.department_group
    new_department_group = frodo.department_group.parent

    # В базе изменения не сохраняем, делаем присваивание для того, чтоб сгенерировать замоканый ответ стаффа
    frodo.department_group = new_department_group
    data = [user_dict(user) for user in arda_users.values() if user.type == USER_TYPES.USER]

    with patch.object(requests.sessions.Session, 'request') as get:
        get.return_value = create_fake_response(json.dumps(staff_response(data)))
        users.import_users()
    frodo.refresh_from_db()

    assert frodo.department_group_id == new_department_group.id
    assert Action.objects.count() == 3  # Выход из группы, добавление в другую группу, изменение департамента для frodo
    expected_actions = {'user_joined_group', 'user_quit_group', 'user_change_department'}
    assert set(Action.objects.values_list('action', flat=True)) == expected_actions

    change_department_action = Action.objects.get(action='user_change_department')
    assert change_department_action.user_id == frodo.id
    assert change_department_action.data == {
        'department_from': old_department_group.id,
        'department_to': new_department_group.id,
        'dep_name_from': old_department_group.name,
        'dep_name_to': new_department_group.name,
    }


def test_department_memberships_sync(group_roots, flat_arda_users, mock_fetcher):
    """Тестируем добавление/перемещение членов департаментной группы без изменения структуры групп"""
    root_dep, root_service, root_wiki = group_roots
    saruman = flat_arda_users.saruman
    aragorn = flat_arda_users.aragorn

    simple_staff_data = [
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
            'id': 103,
            'url': 'evil-lands',
            'name': 'Lands of Evil',
            'parent': {
                'id': 101
            }
        },
        {
            'id': 104,
            'url': 'mordon',
            'name': 'Mordor',
            'parent': {
                'id': 103
            }
        }
    ]
    # проверка, что добавление пользователей работает так, как ожидается
    root_dep.fetcher.set_data(('staff', 'group'), simple_staff_data)
    root_dep.fetcher.set_data(('staff', 'groupmembership'), [])

    staff_users_data = []
    add_users_to_department_group(staff_users_data, 102, ['aragorn', 'legolas'])
    add_users_to_department_group(staff_users_data, 103, ['witch-king-of-angmar', 'nazgul', 'saruman'])
    result, _ = root_dep.synchronize()
    assert result is True
    evil_lands = Group.objects.get(external_id=103)

    # members_count = 5 (непосредственных) + 5 (опосредованных)
    # actions_count = 5 (user_joined_group) + 5 (user_change_department)
    with members_created(members_count=10, actions_count=10) as changed_data:
        with patch.object(requests.sessions.Session, 'request') as get:
            get.return_value = create_fake_response(json.dumps(staff_response(staff_users_data)))
            users.import_users()

    memberships = changed_data.get_new_objects(GroupMembership)
    memberships_pairs = {
        (membership.user.username, membership.group.external_id)
        for membership in memberships.filter(is_direct=True).select_related('user', 'group')
    }
    expected_membership_pairs = {
        ('aragorn', 102),
        ('legolas', 102),
        ('witch-king-of-angmar', 103),
        ('nazgul', 103),
        ('saruman', 103),
    }
    assert refresh(aragorn).department_group_id == Group.objects.get(external_id=102).id
    assert memberships_pairs == expected_membership_pairs
    assert GroupMembership.objects.filter(is_direct=False).count() == 5
    actions = changed_data.get_new_objects(Action)
    action_member_joined = actions.filter(action='user_joined_group').select_related('user', 'group')
    assert action_member_joined.count() == 5
    action_member_joined_data = {(action.user.username, action.group.external_id) for action in action_member_joined}
    assert action_member_joined_data == expected_membership_pairs

    # проверка, что удаление пользователей из группы работает так, как ожидается
    move_members(staff_users_data, 103, root_dep.external_id, ['saruman'])

    with patch.object(requests.sessions.Session, 'request') as get:
        get.return_value = create_fake_response(json.dumps(staff_response(staff_users_data)))
        users.import_users()
    assert GroupMembership.objects.get(user=saruman, group__external_id=103).state == GROUPMEMBERSHIP_STATE.INACTIVE
    actions = changed_data.get_new_objects(Action)
    action_member_quit = actions.select_related('group', 'user').get(action='user_quit_group')
    assert action_member_quit.group.external_id == 103
    assert action_member_quit.user.username == 'saruman'
    evil_lords = evil_lands.members.values_list('username', flat=True)
    assert set(evil_lords) == {'nazgul', 'witch-king-of-angmar'}

    # проверка, что одновременное добавление и удаление пользователей логируется необходимым образом
    move_members(staff_users_data, root_dep.external_id, 103, ['saruman'])
    move_members(staff_users_data, 103, root_dep.external_id, ['nazgul'])
    with members_created(members_count=1, actions_count=6) as changed_data:
        with patch.object(requests.sessions.Session, 'request') as get:
            get.return_value = create_fake_response(json.dumps(staff_response(staff_users_data)))
            users.import_users()
    actions = changed_data.get_new_objects(Action)
    assert set(evil_lands.members.values_list('username', flat=True)) == {'witch-king-of-angmar', 'saruman'}
    join_action = actions.select_related('user').get(action='user_joined_group', group__external_id=103)
    quit_action = actions.select_related('user').get(action='user_quit_group', group__external_id=103)
    assert join_action.user.username == 'saruman'
    assert quit_action.user.username == 'nazgul'


def test_sync_with_nonexistent_group(group_roots, arda_users):
    """Проверим работу синхронизации пользователей, если получаемнесуществующиую департаментную группу"""
    frodo = arda_users['frodo']
    frodo_old_department = frodo.department_group
    sam = arda_users['sam']
    sam_old_department = sam.department_group
    staff_users_data = []
    add_users_to_department_group(staff_users_data, 1000001, [frodo.username])
    add_users_to_department_group(staff_users_data, sam_old_department.parent.external_id, [sam.username])

    with patch.object(requests.sessions.Session, 'request') as get:
        get.return_value = create_fake_response(json.dumps(staff_response(staff_users_data)))
        users.import_users()

    # Фродо остался в старой группе, т.к. мы пока не знаем ничего про новую группу
    # переместим при следующем синке
    frodo.refresh_from_db()
    assert frodo.department_group_id == frodo_old_department.id

    # Сэм удачно переместился в вышестоящий департамент
    sam.refresh_from_db()
    assert sam.department_group_id == sam_old_department.parent_id


def test_roles_hold_when_user_change_department(group_roots, flat_arda_users, simple_system, mock_fetcher):
    """Если сотрудник выходит из группы в unaware системе, его роль становится на холд"""
    root_dep, root_service, root_wiki = group_roots
    legolas = flat_arda_users['legolas']
    simple_staff_data = [
        {
            'id': 101,
            'url': 'middle-earth',
            'name': 'Middle Earth',
            'responsibles': [
                {
                    'person': {
                        'id': legolas.center_id,
                        'official': {'is_dismissed': False},
                    }
                },
            ]
        }
    ]
    staff_users_data = []
    add_users_to_department_group(staff_users_data, 101, ['legolas'])
    root_dep.fetcher.set_data(('staff', 'group'), simple_staff_data)
    root_dep.fetcher.set_data(('staff', 'groupmembership'), [])
    result, _ = root_dep.synchronize()
    assert result is True

    with patch.object(requests.sessions.Session, 'request') as get:
        get.return_value = create_fake_response(json.dumps(staff_response(staff_users_data)))
        users.import_users()
        call_command('idm_poke_hanging_roles', '--stage', 'request_or_deprive_personal')

    middle_earth = Group.objects.get(external_id=101)
    make_role(middle_earth, simple_system, {'role': 'manager'})

    user_role = Role.objects.get(user=legolas)
    assert user_role.state == 'granted'

    move_members(staff_users_data, 101, root_dep.external_id, ['legolas'])

    clear_mailbox()
    with patch.object(requests.sessions.Session, 'request') as get:
        get.return_value = create_fake_response(json.dumps(staff_response(staff_users_data)))
        users.import_users()
        call_command('idm_poke_hanging_roles', '--stage', 'request_or_deprive_personal')

    user_role = refresh(user_role)
    assert user_role.state == 'onhold'
    assert len(mail.outbox) == 0

    call_command('idm_send_roles_reminders')
    assert len(mail.outbox) == 1

    message = mail.outbox[0]
    assert message.to == ['legolas@example.yandex.ru']
    assert message.subject == 'Некоторые роли скоро будут отозваны'
    assert_contains(
        [
            'Некоторые ваши роли скоро будут отозваны в связи с выходом из групп/удалением групп.',
            'Чтобы предотвратить отзыв ролей, вы можете восстановить членство в группе, '
            'связавшись с ответственными группы или запросить такую же персональную роль '
            'с помощью функции клонирования.',
            'Список ролей, которые скоро будут отозваны:',
            'В связи с выходом из группы/удалением группы Middle Earth будут отозваны роли:',
            'Система: Simple система. Роль: Менеджер. Дата отзыва:',
            '(https://example.com/system/simple/#role=%d)' % user_role.pk,
            'Ответственные за группу:',
            'Список ролей в интерфейсе IDM:',
            'https://example.com/user/legolas#f-status=active,f-state=onhold,main=roles,sort-by=-updated',
        ],
        message.body
    )

    move_members(staff_users_data, root_dep.external_id, 101, ['legolas'])
    with patch.object(requests.sessions.Session, 'request') as get:
        get.return_value = create_fake_response(json.dumps(staff_response(staff_users_data)))
        users.import_users()
        call_command('idm_poke_hanging_roles', '--stage', 'request_or_deprive_personal')
    assert result is True

    user_role = refresh(user_role)
    assert user_role.state == 'granted'
    assert_action_chain(user_role, ['request', 'approve', 'first_add_role_push', 'grant', 'hold', 'grant'])


def test_dismiss_user(art):
    """
    TestpalmID: 3456788-99
    """
    assert art.is_active
    assert not art.idm_found_out_dismissal
    with patch.object(requests.sessions.Session, 'request') as get:
        art_dict = user_dict(art)
        art_dict['official']['is_dismissed'] = True

        get.return_value = create_fake_response(json.dumps(
            staff_response([art_dict])
        ))
        users.import_users()

        art = refresh(art)

        assert not art.is_active
        assert art.idm_found_out_dismissal is not None


def test_restore_user(art):
    art.is_active = False
    art.idm_found_out_dismissal = timezone.now()
    art.ldap_active = False
    art.ldap_blocked_timestamp = timezone.now()
    art.save()

    with patch.object(requests.sessions.Session, 'request') as get:
        art_dict = user_dict(art)
        art_dict['official']['is_dismissed'] = False

        get.return_value = create_fake_response(json.dumps(
            staff_response([art_dict])
        ))
        users.import_users()

        art = refresh(art)

        assert art.is_active
        assert art.idm_found_out_dismissal is None
        assert art.ldap_active
        assert art.ldap_blocked_timestamp is None


def test_user_hired_back(arda_users, department_structure):
    frodo = arda_users.frodo

    data = [user_dict(user) for user in arda_users.values() if user.type == USER_TYPES.USER]
    frodo.is_active = False
    frodo.save(update_fields=['is_active'])
    frodo.memberships.update(state='inactive')

    with patch.object(requests.sessions.Session, 'request') as get:
        get.return_value = create_fake_response(json.dumps(staff_response(data)))
        users.import_users()
    frodo.refresh_from_db()
    frodo.fetch_department_group()

    # Проверим, что членства frodo в департаментных группах восстановилось
    expected_active_memberships = frodo.department_group.get_ancestors(include_self=True).count() - 1  # Вычтем dep_root
    assert frodo.memberships.filter(group__type='department', state='active').count() == expected_active_memberships

    assert Action.objects.count() == 1  # Добавление в группу для frodo
    expected_actions = {'user_joined_group'}
    assert set(Action.objects.values_list('action', flat=True)) == expected_actions


def test_user_dismissed(arda_users, department_structure):
    # Проверим, что при увольнении пользователь будет удален из департаментной группы
    frodo = arda_users.frodo

    # Изменения для frodo в базе не сохраняем, изменяем для генерации ответа от staff-api
    frodo.is_active = False
    data = [user_dict(user) for user in arda_users.values() if user.type == USER_TYPES.USER]

    with patch.object(requests.sessions.Session, 'request') as get:
        get.return_value = create_fake_response(json.dumps(staff_response(data)))
        users.import_users()
    frodo.refresh_from_db()
    assert frodo.is_active is False

    # Проверим, что членства frodo в департаментных группах стало неактивным
    department_membership = frodo.memberships.get(group_id=frodo.department_group_id)
    assert department_membership.state == 'inactive'

    assert Action.objects.count() == 1  # Выход из группы frodo
    expected_actions = {'user_quit_group'}
    assert set(Action.objects.values_list('action', flat=True)) == expected_actions
