from copy import deepcopy
from datetime import timedelta, datetime

import pytest

from django.core import mail
from django.core.management import call_command
from django.db.models import Count
from django.utils import timezone
from freezegun import freeze_time

from idm.core.exceptions import SynchronizationError
from idm.core.models import Action, Role, Transfer
from idm.core.constants.affiliation import AFFILIATION
from idm.core.constants.groupmembership import GROUPMEMBERSHIP_STATE
from idm.nodes.hashers import Hasher
from idm.tests.utils import (create_user, models_didnt_change, CountIncreasedContext, refresh, raw_make_role, make_role, assert_contains, assert_action_chain, set_workflow,
                             DEFAULT_WORKFLOW, add_perms_by_role, clear_mailbox, members_created)
from idm.users.constants.group import GROUP_TYPES
from idm.users.models import Group, GroupMembership, GroupResponsibility, User
from idm.users.sync.groups import deprive_depriving_groups, sync_indirect_memberships

pytestmark = [pytest.mark.django_db, pytest.mark.usefixtures('mock_fetcher')]


def login_to_center_id(username):
    return User.objects.get(username=username).center_id


def nothing_changed():
    return models_didnt_change(Group, Action, GroupResponsibility, GroupMembership, User)


def refresh_dep_group(group, structure, memberships=None):
    group = Group.objects.get(pk=group.pk)
    group.hasher = Hasher(debug=False)
    group.fetcher.set_data(('staff', 'group'), structure)
    group.fetcher.set_data(('staff', 'groupmembership'), memberships)
    return group


def responsible_created(responsibles_count, actions_count):
    return CountIncreasedContext(
        (Group, 0),
        (GroupResponsibility, responsibles_count),
        (GroupMembership, 0),
        (User, 0),
        (Action, actions_count),
    )


def add_responsibles(structure, external_id, responsibilities):
    for group in structure:
        if group.get('id') != external_id:
            continue
        for username, rank in responsibilities:
            user = create_user(username, department_group=None)
            department = group.setdefault('department', {})
            if 'heads' not in department:
                department['heads'] = []
            department['heads'].append({
                'role': rank,
                'person': {
                    'id': user.center_id,
                    'official': {
                        'is_dismissed': False,
                    },
                }
            })


def remove_responsibles(structure, external_id, usernames):
    center_ids = set(User.objects.filter(username__in=usernames).values_list('center_id', flat=True))
    for group in structure:
        if group.get('id') != external_id or 'department' not in group:
            continue
        department = group['department']
        department['heads'] = [head for head in department['heads'] if head['person']['id'] not in center_ids]


def change_responsible_rank(structure, external_id, username, new_rank):
    center_id = User.objects.get(username=username).center_id
    for group in structure:
        if group.get('id') != external_id:
            continue
        department = group['department']
        for head in department.get('heads', ()):
            if head['person']['id'] == center_id:
                head['role'] = new_rank


def add_members(membership_data, external_id, usernames):
    username_to_id = dict(
        User.objects.filter(username__in=usernames).values_list('username', 'center_id')
    )
    for username in usernames:
        create_user(username)
        membership_data.append({
            'id': 1,  # важно, чтобы это поле присутствовало. его значение не важно
            'group': {
                'id': external_id,
            },
            'person': {
                'id': username_to_id[username],
                'official': {'is_dismissed': False},
            }
        })


def remove_members(memberships, external_id, usernames=None):
    if usernames is not None:
        # удаляем указанных пользователей
        ids_to_remove = set(
            User.objects.filter(username__in=usernames).values_list('center_id', flat=True)
        )

        memberships[:] = [
            membership for membership in memberships
            if membership['group']['id'] != external_id or membership['person']['id'] not in ids_to_remove
        ]
    else:
        # удаляем всех пользователей из группы
        memberships[:] = [
            membership for membership in memberships
            if membership['group']['id'] != external_id
        ]


def assert_nothing_changed_if_nothing_done(group):
    """Проверка, что если мы ничего не изменяли, то синхронизация не должна заметить изменений"""
    with nothing_changed():
        refreshed = refresh(group)
        refreshed.hasher = group.hasher
        refreshed.fetcher = group.fetcher
        result, _ = refreshed.synchronize()
        assert result is False


def get_action_stats(actions):
    stats_qs = actions.values_list('action').annotate(act_count=Count('action')).values_list('action', 'act_count')
    stats_qs = stats_qs.order_by('action')
    stats = dict(stats_qs)
    return stats


@pytest.mark.robotless
def test_department_responsibilities_sync(group_roots, flat_arda_users):
    """Тестируем добавление/удаление ответственных, добавление/удаление
    членов группы без изменения структуры групп"""
    root_dep, root_service, root_wiki = group_roots

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
    memberships_data = []
    root_dep.fetcher.set_data(('staff', 'group'), simple_staff_data)
    root_dep.fetcher.set_data(('staff', 'groupmembership'), memberships_data)
    assert Action.objects.count() == 0
    assert Group.objects.count() == 3  # 3 корня
    updated_before = root_dep.updated_at
    with CountIncreasedContext((Group, 4), (Action, 4)) as changed_data:
        result, _ = root_dep.synchronize()
        assert result is True
    assert root_dep.updated_at != updated_before
    deep = Group.objects.get(external_id=104)
    assert deep.parent.external_id == 103
    assert deep.path == '/0/101/103/104/'
    actions = changed_data.get_new_objects(Action)
    add_actions = actions.filter(action='group_created').select_related('group')
    assert add_actions.count() == 4
    assert {action.group.external_id for action in add_actions} == {101, 102, 103, 104}
    assert GroupMembership.objects.count() == 0
    assert GroupResponsibility.objects.count() == 0
    root_dep = refresh_dep_group(root_dep, simple_staff_data, memberships_data)
    updated_before = root_dep.updated_at
    with nothing_changed():
        result, _ = root_dep.synchronize()
        assert result is False
    root_dep = refresh_dep_group(root_dep, simple_staff_data, memberships_data)
    assert updated_before != root_dep.updated_at

    # проверка, что добавление ответственных работает так, как ожидается
    with responsible_created(responsibles_count=2, actions_count=2) as changed_data:
        add_responsibles(simple_staff_data, 103, [
            ('sauron', 'chief'),
            ('saruman', 'deputy')
        ])
        result, _ = root_dep.synchronize()
        assert result is True
    responsibilities = changed_data.get_new_objects(GroupResponsibility)
    assert responsibilities.count() == 2
    resposibilities_data = {
        (resp.user.username, resp.group.external_id, resp.rank)
        for resp in responsibilities.select_related('user', 'group')
    }
    assert resposibilities_data == {('sauron', 103, 'head'), ('saruman', 103, 'deputy')}
    new_actions = changed_data.get_new_objects(Action)
    assert new_actions.count() == 2
    responsible_actions = new_actions.filter(action='group_responsible_added').select_related('user', 'group')
    assert responsible_actions.count() == 2
    action_data = {
        (action.user.username, action.group.slug, action.data['rank'])
        for action in responsible_actions.select_related('user', 'group')
    }
    assert action_data == {('sauron', 'evil-lands', 'head'), ('saruman', 'evil-lands', 'deputy')}

    assert_nothing_changed_if_nothing_done(root_dep)

    # проверка, что удаление ответственных работает так, как ожидается
    responsibles_count = GroupResponsibility.objects.filter(is_active=True).count()
    with CountIncreasedContext((Action, 1)) as changed_data:
        remove_responsibles(simple_staff_data, 103, ['saruman'])
        root_dep = refresh_dep_group(root_dep, simple_staff_data, memberships_data)
        result, _ = root_dep.synchronize()
        assert result is True
    assert GroupResponsibility.objects.filter(is_active=True).count() == responsibles_count - 1
    evil_lands = Group.objects.get(external_id=103)
    responsibilities = evil_lands.active_responsibilities.filter(is_active=True).values_list('user__username', 'rank')
    assert list(responsibilities) == [('sauron', 'head')]
    actions = changed_data.get_new_objects(Action)
    responsible_removed_action = actions.select_related('user', 'group').get(action='group_responsible_removed')
    assert responsible_removed_action.group.external_id == 103
    assert responsible_removed_action.user.username == 'saruman'
    assert responsible_removed_action.data.get('rank') == 'deputy'

    assert_nothing_changed_if_nothing_done(root_dep)

    # проверка, что смена должности воспринимается как удаление и добавление ответственного
    responsibilities_amount = GroupResponsibility.objects.filter(is_active=True).count()
    with CountIncreasedContext((Action, 2)) as changed_data:
        change_responsible_rank(simple_staff_data, 103, 'sauron', 'deputy')
        root_dep = refresh_dep_group(root_dep, simple_staff_data, memberships_data)
        result, _ = root_dep.synchronize()
        assert result is True
    assert GroupResponsibility.objects.filter(is_active=True).count() == responsibilities_amount
    responsibilities = evil_lands.active_responsibilities.values_list('user__username', 'rank')
    assert list(responsibilities) == [('sauron', 'deputy')]
    actions = changed_data.get_new_objects(Action)
    responsible_added_action = actions.select_related('user', 'group').get(action='group_responsible_added')
    responsible_removed_action = actions.select_related('user', 'group').get(action='group_responsible_removed')
    assert responsible_added_action.group.external_id == 103
    assert responsible_removed_action.group.external_id == 103
    assert responsible_removed_action.user.username == 'sauron'
    assert responsible_added_action.user.username == 'sauron'
    assert responsible_removed_action.data.get('rank') == 'head'
    assert responsible_added_action.data.get('rank') == 'deputy'

    assert_nothing_changed_if_nothing_done(root_dep)

    # проверка, что одновременное добавление и удаление ответственных работает так, как ожидается
    with responsible_created(responsibles_count=0, actions_count=2) as changed_data:
        remove_responsibles(simple_staff_data, 103, ['sauron'])
        add_responsibles(simple_staff_data, 103, [('saruman', 'deputy')])
        result, _ = root_dep.synchronize()
        assert result is True
    responsibilities = evil_lands.active_responsibilities.values_list('user__username', 'rank')
    assert list(responsibilities) == [('saruman', 'deputy')]
    actions = changed_data.get_new_objects(Action)
    responsible_added_action = actions.select_related('group', 'user').get(action='group_responsible_added')
    responsible_removed_action = actions.select_related('group', 'user').get(action='group_responsible_removed')

    assert responsible_added_action.group.external_id == 103
    assert responsible_removed_action.group.external_id == 103
    assert responsible_removed_action.user.username == 'sauron'
    assert responsible_added_action.user.username == 'saruman'
    assert responsible_removed_action.data.get('rank') == 'deputy'
    assert responsible_added_action.data.get('rank') == 'deputy'

    assert_nothing_changed_if_nothing_done(root_dep)


@pytest.mark.robotless
def test_group_memberships_sync(group_roots, flat_arda_users):
    """Тестируем добавление/удаление членов группы
    без изменения структуры групп"""
    root_dep, root_service, root_wiki = group_roots
    saruman = flat_arda_users.saruman

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
    memberships_data = []
    add_members(memberships_data, 102, ['aragorn', 'legolas'])
    add_members(memberships_data, 103, ['witch-king-of-angmar', 'nazgul', 'saruman'])

    # members_count = 5 (непосредственных)
    # actions_count = 5 (user_joined_group) + 4 (group_created)
    # groups_count = 4
    with members_created(members_count=5, actions_count=9, groups_count=4) as changed_data:
        root_wiki = refresh_dep_group(root_wiki, simple_staff_data, memberships_data)
        result, _ = root_wiki.synchronize()
        assert result is True
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
    assert memberships_pairs == expected_membership_pairs
    assert GroupMembership.objects.filter(is_direct=False).count() == 0
    actions = changed_data.get_new_objects(Action)
    action_member_joined = actions.filter(action='user_joined_group').select_related('user', 'group')
    assert action_member_joined.count() == 5
    action_member_joined_data = {(action.user.username, action.group.external_id) for action in action_member_joined}
    assert action_member_joined_data == expected_membership_pairs

    # проверка, что удаление пользователей из группы работает так, как ожидается
    remove_members(memberships_data, 103, ['saruman'])
    with members_created(members_count=0, actions_count=1) as changed_data:
        root_wiki = refresh_dep_group(root_wiki, simple_staff_data, memberships_data)
        result, _ = root_wiki.synchronize()
        assert result is True
    assert GroupMembership.objects.filter(state=GROUPMEMBERSHIP_STATE.ACTIVE).count() == 4
    assert GroupMembership.objects.get(user=saruman).state == GROUPMEMBERSHIP_STATE.INACTIVE
    actions = changed_data.get_new_objects(Action)
    action_member_quit = actions.select_related('group', 'user').get(action='user_quit_group')
    assert action_member_quit.group.external_id == 103
    assert action_member_quit.user.username == 'saruman'
    evil_lands = Group.objects.get(external_id=103)
    evil_lords = evil_lands.members.values_list('username', flat=True)
    assert set(evil_lords) == {'nazgul', 'witch-king-of-angmar'}

    # проверка, что одновременное добавление и удаление пользователей логируется необходимым образом
    add_members(memberships_data, 103, ['saruman'])
    remove_members(memberships_data, 103, ['nazgul'])
    with members_created(members_count=0, actions_count=2) as changed_data:
        root_wiki = refresh_dep_group(root_wiki, simple_staff_data, memberships_data)
        result, _ = root_wiki.synchronize()
        assert result is True
    actions = changed_data.get_new_objects(Action)
    assert set(evil_lands.members.values_list('username', flat=True)) == {'witch-king-of-angmar', 'saruman'}
    join_action = actions.select_related('user', 'group').get(action='user_joined_group')
    quit_action = actions.select_related('user', 'group').get(action='user_quit_group')
    assert join_action.group.external_id == 103
    assert quit_action.group.external_id == 103
    assert join_action.user.username == 'saruman'
    assert quit_action.user.username == 'nazgul'

    assert_nothing_changed_if_nothing_done(root_wiki)


@pytest.mark.robotless
def test_group_data_changes(group_roots):
    """Тестируем переименование и вообще изменение данных групп без изменения их структуры"""

    root_dep, root_service, root_wiki = group_roots
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
            'url': 'mordor',
            'name': 'Mordor',
            'parent': {
                'id': 103
            }
        }
    ]
    memberships_data = []
    root_dep.fetcher.set_data(('staff', 'group'), simple_staff_data)
    root_dep.fetcher.set_data(('staff', 'groupmembership'), memberships_data)
    root_dep.synchronize()
    renamed_data = [
        {
            'id': 101,
            'url': 'middle-earth-of-arda',
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
            'name': 'Lands of Great Evil',
            'parent': {
                'id': 101
            }
        },
        {
            'id': 104,
            'url': 'lands-of-great-happiness',
            'name': 'Lands of Complex Pleasures',
            'parent': {
                'id': 103
            }
        }
    ]
    with CountIncreasedContext((Group, 0), (User, 0), (Action, 3)) as changed_data:
        root_dep.fetcher.set_data(('staff', 'group'), renamed_data)
        result, _ = root_dep.synchronize()
        assert result is True
    actions = changed_data.get_new_objects(Action)
    assert set(actions.values_list('action', flat=True)) == {'group_changed'}
    action_data = {}
    for action in actions:
        action.fetch_group()
        action_data[action.group.external_id] = action.data['diff']

    expected_action_data = {
        101: {
            'slug': ['middle-earth', 'middle-earth-of-arda']
        },
        103: {
            'name': ['Lands of Evil', 'Lands of Great Evil'],
            'name_en': ['Lands of Evil', 'Lands of Great Evil'],
        },
        104: {
            'name': ['Mordor', 'Lands of Complex Pleasures'],
            'name_en': ['Mordor', 'Lands of Complex Pleasures'],
            'slug': ['mordor', 'lands-of-great-happiness']
        }
    }
    assert action_data == expected_action_data


@pytest.mark.robotless
def test_rename_department_group_and_change_responsibles(group_roots, flat_arda_users):
    """Тестируем одновременное изменения данных группы и ответственных"""

    root_dep, root_service, root_wiki = group_roots
    legolas = flat_arda_users.legolas
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
            },
            'department': {
                'heads': [
                    {
                        'person': {
                            'id': legolas.center_id,
                            'official': {'is_dismissed': False},
                        },
                        'role': 'chief'
                    },
                ]
            },
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
            'url': 'mordor',
            'name': 'Mordor',
            'parent': {
                'id': 103
            }
        }
    ]
    memberships_data = []
    add_members(memberships_data, 102, ['legolas', 'aragorn'])
    root_dep.fetcher.set_data(('staff', 'group'), simple_staff_data)
    root_dep.fetcher.set_data(('staff', 'groupmembership'), memberships_data)
    with CountIncreasedContext((Group, 4), (GroupResponsibility, 1), (Action, 5)) as new_data:
        result, _ = root_dep.synchronize()
        assert result is True
    actions = new_data.get_new_objects(Action)
    stats = get_action_stats(actions)
    expected_stats = {
        'group_created': 4,
        'group_responsible_added': 1,
    }
    assert stats == expected_stats
    remove_responsibles(simple_staff_data, 102, ['legolas'])
    simple_staff_data[1].update({
        'slug': 'renamed',
        'name': 'Good lands'
    })
    active_responsibilities_amount = GroupResponsibility.objects.filter(is_active=True).count()
    with CountIncreasedContext((Group, 0), (Action, 2)) as changed_data:
        result, _ = root_dep.synchronize()
        assert result is True
    assert GroupResponsibility.objects.filter(is_active=True).count() == active_responsibilities_amount - 1
    actions = changed_data.get_new_objects(Action)
    stats = get_action_stats(actions)
    expected_stats = {
        'group_changed': 1,
        'group_responsible_removed': 1,
    }
    assert stats == expected_stats
    group_changed = actions.get(action='group_changed')
    expected_diff = {
        'name': ['Lands of Good', 'Good lands'],
        'name_en': ['Lands of Good', 'Good lands'],
    }
    assert group_changed.data.get('diff') == expected_diff


@pytest.mark.robotless
def test_department_group_moved(group_roots, flat_arda_users):
    """Тест перемещения групп"""

    root_dep, root_service, root_wiki = group_roots

    sauron = flat_arda_users.sauron
    saruman = flat_arda_users.saruman
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
            'url': 'orodruin',
            'name': 'Orodruin',
            'parent': {
                'id': 103
            },
            'department': {
                'heads': [
                    {
                        'person': {
                            'id': saruman.center_id,
                            'official': {'is_dismissed': False},
                        },
                        'role': 'chief'
                    }
                ]
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
    root_dep.fetcher.set_data(('staff', 'group'), simple_staff_data)
    root_dep.fetcher.set_data(('staff', 'groupmembership'), [])
    with CountIncreasedContext((Group, 5), (Action, 6)) as changed_data:
        result, _ = root_dep.synchronize()
        assert result is True
    stats = get_action_stats(changed_data.get_new_objects(Action))
    expected_stats = {
        'group_created': 5,
        'group_responsible_added': 1
    }
    assert stats == expected_stats

    # Перемещаем группу orodruin под создаваемую группу mordor
    moved_and_changed_data = [
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
            'id': 105,
            'url': 'eldarion',
            'name': 'Eldarion',
            'parent': {
                'id': 102
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
            'id': 106,
            'url': 'mordor',
            'name': 'Mordor',
            'parent': {
                'id': 103
            }
        },
        {
            'id': 104,
            'url': 'orodruin',
            'name': 'Mount Doom',
            'parent': {
                'id': 106
            },
            'department': {
                'heads': [
                    {
                        'person': {
                            'id': sauron.center_id,
                            'official': {'is_dismissed': False},
                        },
                        'role': 'chief'
                    }
                ]
            }
        }
    ]
    with CountIncreasedContext((Group, 2), (Action, 6), (GroupResponsibility, 1)) as new_data:
        root_dep.fetcher.set_data(('staff', 'group'), moved_and_changed_data)
        result, _ = root_dep.synchronize()
        assert result is True
    actions = new_data.get_new_objects(Action)
    stats = get_action_stats(actions)
    expected_stats = {
        'group_created': 2,
        'group_responsible_added': 1,
        'group_responsible_removed': 1,
        'group_moved': 1,
        'group_changed': 1,
    }
    assert stats == expected_stats
    sheer = Group.objects.get(slug='sheer')
    assert sheer.state == 'depriving'
    orodruin = Group.objects.get(slug='orodruin')
    assert orodruin.name == 'Mount Doom'
    assert orodruin.state == 'active'

    orodruin_action = actions.get(action='group_moved')
    expected_data = {
        'moved_from': {
            'id': orodruin.pk,
            'path': '/0/101/103/104/'
        },
        'moved_to': '/0/101/103/106/104/',
    }
    assert orodruin_action.data == expected_data
    responsible_removed_action = actions.get(action='group_responsible_removed')
    expected_data = {
        'rank': 'head'
    }
    assert responsible_removed_action.data == expected_data

    with CountIncreasedContext((Group, 0), (Action, 2)) as new_data:
        deprive_depriving_groups(force=True)

    deleted_sheer = new_data.get_new_objects(Action).get(group__slug='sheer')
    assert deleted_sheer.data is None

    assert_nothing_changed_if_nothing_done(root_dep)


def test_delete_group(groups_with_roles, simple_system):
    """Тест удаления групп"""
    fellowship = Group.objects.get(slug='fellowship-of-the-ring')
    expired_role = raw_make_role(fellowship, simple_system, {'role': 'poweruser'}, state='expired')
    fellowship.state = 'depriving'
    fellowship.deprive()

    expired_role = refresh(expired_role)
    assert expired_role.state == 'expired'

    for role in groups_with_roles['fellowship']:
        role = refresh(role)
        assert role.state == 'onhold'
        for personal in role.refs.all():
            assert personal.state == 'onhold'

    for gm in GroupMembership.objects.filter(group=fellowship):
        assert gm.state == GROUPMEMBERSHIP_STATE.INACTIVE
        assert Action.objects.filter(user_id=gm.user_id, group_id=gm.group_id, action='user_quit_group').exists()

    for gm in GroupMembership.objects.filter(group=fellowship):
        assert gm.state == GROUPMEMBERSHIP_STATE.INACTIVE


@pytest.mark.robotless
def test_move_node_with_deleted_subnodes(group_roots, flat_arda_users):
    """Тест на перемещение группы с удалёнными потомками"""

    root_dep, root_service, root_wiki = group_roots
    sauron = flat_arda_users.sauron
    saruman = flat_arda_users.saruman
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
            'url': 'orodruin',
            'name': 'Orodruin',
            'parent': {
                'id': 103
            },
            'department': {
                'heads': [
                    {
                        'person': {
                            'id': saruman.center_id,
                            'official': {'is_dismissed': False},
                        },
                        'role': 'chief'
                    }
                ]
            }
        },
        {
            'id': 120,
            'url': 'throne',
            'name': 'Throne of Sauron',
            'parent': {
                'id': 104,
            }
        },
        {
            'id': 110,
            'url': 'sheer',
            'name': 'Halflings land',
            'parent': {
                'id': 101
            }
        },
    ]
    root_dep.fetcher.set_data(('staff', 'group'), simple_staff_data)
    root_dep.fetcher.set_data(('staff', 'groupmembership'), [])
    root_dep.synchronize()
    # теперь удалим throne. для экономии кода сделаем это напрямую, без удаления через синхронизацию.
    group = Group.objects.get(slug='throne')
    group.state = 'deprived'
    group.save()

    moved_and_changed_data = [
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
            'id': 105,
            'url': 'eldarion',
            'name': 'Eldarion',
            'parent': {
                'id': 102
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
            'id': 106,
            'url': 'mordor',
            'name': 'Mordor',
            'parent': {
                'id': 103
            }
        },
        {
            'id': 104,
            'url': 'orodruin',
            'name': 'Mount Doom',
            'parent': {
                'id': 106
            },
            'department': {
                'heads': [
                    {
                        'person': {
                            'id': sauron.center_id,
                            'official': {'is_dismissed': False},
                        },
                        'role': 'chief'
                    }
                ]
            }
        }
    ]
    with CountIncreasedContext((Group, 2), (Action, 6), (GroupResponsibility, 1)) as new_data:
        root_dep.fetcher.set_data(('staff', 'group'), moved_and_changed_data)
        result, _ = root_dep.synchronize()
        assert result is True
    actions = new_data.get_new_objects(Action)
    stats = get_action_stats(actions)
    expected_stats = {
        'group_created': 2,
        'group_changed': 1,
        'group_responsible_added': 1,
        'group_moved': 1,
        'group_responsible_removed': 1,
    }
    assert stats == expected_stats
    # удалённый sheer
    assert Group.objects.filter(state='depriving').count() == 1
    # ранее удалённый throne
    assert Group.objects.filter(state='deprived').count() == 1
    orodruin = Group.objects.get(slug='orodruin')
    assert orodruin.parent.slug == 'mordor'


@pytest.mark.robotless
def test_move_node_with_subnodes(group_roots, flat_arda_users):
    """Тест на перемещение группы вместе с поддеревом"""

    root_dep, root_service, root_wiki = group_roots
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
                'id': 101,
            }
        },
        {
            'id': 103,
            'url': 'sheer',
            'name': 'Sheer',
            'parent': {
                'id': 102,
            }
        },
        {
            'id': 104,
            'url': 'frodo-house',
            'name': 'The house of Frodo',
            'parent': {
                'id': 103,
            }
        }
    ]
    root_dep.fetcher.set_data(('staff', 'group'), simple_staff_data)
    root_dep.fetcher.set_data(('staff', 'groupmembership'), [])
    root_dep.synchronize()

    moved_data = [
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
                'id': 101,
            }
        },
        {
            'id': 103,
            'url': 'sheer',
            'name': 'Sheer',
            'parent': {
                'id': 101,
            }
        },
        {
            'id': 104,
            'url': 'frodo-house',
            'name': 'The house of Frodo',
            'parent': {
                'id': 103,
            }
        }
    ]

    with CountIncreasedContext((Group, 0), (Action, 2)) as new_data:
        root_dep.fetcher.set_data(('staff', 'group'), moved_data)
        result, _ = root_dep.synchronize()
        assert result is True
    actions = new_data.get_new_objects(Action)
    stats = get_action_stats(actions)
    expected_stats = {
        'group_moved': 2,
    }
    assert stats == expected_stats
    house = Group.objects.get(slug='frodo-house')
    assert house.path == '/0/101/103/104/'
    transfer = house.transfers.get()
    assert transfer.source_path == '/middle-earth/good-lands/sheer/frodo-house/'
    assert transfer.target_path == '/middle-earth/sheer/frodo-house/'


@pytest.mark.robotless
def test_moved_node_with_added_subnode(group_roots, flat_arda_users):
    """У перемещённого узла появляется потомок"""

    root_dep, root_service, root_wiki = group_roots
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
                'id': 101,
            }
        },
        {
            'id': 103,
            'url': 'sheer',
            'name': 'Sheer',
            'parent': {
                'id': 102,
            }
        }
    ]
    root_dep.fetcher.set_data(('staff', 'group'), simple_staff_data)
    root_dep.fetcher.set_data(('staff', 'groupmembership'), [])
    root_dep.synchronize()

    moved_data = [
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
                'id': 101,
            }
        },
        {
            'id': 103,
            'url': 'sheer',
            'name': 'Sheer',
            'parent': {
                'id': 101,
            }
        },
        {
            'id': 104,
            'url': 'frodo-house',
            'name': 'The house of Frodo',
            'parent': {
                'id': 103,
            }
        }
    ]

    with CountIncreasedContext((Group, 1), (Action, 2)) as new_data:
        root_dep.fetcher.set_data(('staff', 'group'), moved_data)
        result, _ = root_dep.synchronize()
        assert result is True
    actions = new_data.get_new_objects(Action)
    stats = get_action_stats(actions)
    expected_stats = {
        'group_moved': 1,
        'group_created': 1,
    }
    assert stats == expected_stats
    sheer = Group.objects.get(slug='sheer')
    assert sheer.path == '/0/101/103/'
    house = Group.objects.get(slug='frodo-house')
    assert house.path == '/0/101/103/104/'


@pytest.mark.robotless
def test_service_groups(group_roots, flat_arda_users):
    """Тестируем синхронизацию сервисных групп"""

    root_dep, root_service, root_wiki = group_roots
    sauron = flat_arda_users['sauron']
    frodo = flat_arda_users['frodo']
    fellowship = [flat_arda_users[username] for username in ('frodo', 'legolas', 'aragorn', 'gimli',
                                                             'gandalf', 'boromir')]
    darkforces = [flat_arda_users[username] for username in ('sauron', 'saruman', 'nazgul')]

    group_data = [
        {
            'id': 101,
            'url': 'middle-earth',
            'name': 'Middle Earth',
            'service': {
                'id': None
            }
        },
        {
            'id': 102,
            'url': 'good-lands',
            'name': 'Lands of Good',
            'parent': {
                'id': 101
            },
            'service': {
                'id': 1001
            }
        },
        {
            'id': 103,
            'url': 'evil-lands',
            'name': 'Lands of Evil',
            'parent': {
                'id': 101
            },
            'service': {
                'id': 1002
            },
            'responsibles': [
                {'person': {'id': sauron.center_id, 'official': {'is_dismissed': False}}},
            ]
        },
        {
            'id': 104,
            'url': 'mordor',
            'name': 'Mordor',
            'parent': {
                'id': 103
            },
            'service': {
                'id': 1003
            }
        },
        {
            'id': 110,
            'url': 'ring-brotherhood',
            'name': 'Ring brotherhood',
            'parent': {
                'id': 101
            },
            'service': {
                'id': 1004
            },
            'responsibles': [
                {'person': {'id': frodo.center_id, 'official': {'is_dismissed': False}}},
            ]
        }
    ]
    memberships_data = []
    add_members(memberships_data, 103, [person.username for person in darkforces])
    add_members(memberships_data, 110, [person.username for person in fellowship])
    root_service.fetcher.set_data(('abc', 'service_members'), [])
    root_service.fetcher.set_data(('staff', 'group'), deepcopy(group_data), for_lookup=('type', 'service'))
    root_service.fetcher.set_data(('staff', 'group'), [], for_lookup=('type', 'servicerole'))
    root_service.fetcher.set_data(('staff', 'groupmembership'), memberships_data)
    root_service.fetcher.set_data(('abc', 'services'), [])
    with CountIncreasedContext((Group, 5), (GroupResponsibility, 2), (GroupMembership, 9),
                               (Action, 16)) as new_data:
        result, _ = root_service.synchronize()
        assert result is True

    actions = new_data.get_new_objects(Action)
    stats = get_action_stats(actions)
    expected_stats = {
        'group_created': 5,
        'group_responsible_added': 2,
        'user_joined_group': 9
    }
    assert stats == expected_stats
    assert_nothing_changed_if_nothing_done(root_service)

    cutted_group_data = deepcopy(group_data)
    cutted_group_data[2]['responsibles'] = []
    cutted_membership_data = deepcopy(memberships_data)
    remove_members(cutted_membership_data, 103)

    active_memberships_amount = GroupMembership.objects.filter(state__in=GROUPMEMBERSHIP_STATE.ACTIVE_STATES).count()
    active_responsibilities_amount = GroupResponsibility.objects.filter(is_active=True).count()
    with CountIncreasedContext((Group, 0), (Action, 4)) as changed_data:
        root_service.fetcher.set_data(('staff', 'group'), cutted_group_data, for_lookup=('type', 'service'))
        root_service.fetcher.set_data(('staff', 'groupmembership'), cutted_membership_data)
        result, _ = root_service.synchronize()
        assert result is True

    assert GroupMembership.objects.filter(state=GROUPMEMBERSHIP_STATE.ACTIVE).count() == active_memberships_amount - 3
    assert GroupMembership.objects.filter(state=GROUPMEMBERSHIP_STATE.INACTIVE).count() == 3
    assert GroupResponsibility.objects.filter(is_active=True).count() == active_responsibilities_amount - 1
    group = Group.objects.get(slug='evil-lands')
    assert group.members.count() == 0
    assert group.responsibles.count() == 0

    actions = changed_data.get_new_objects(Action)
    stats = get_action_stats(actions)
    expected_stats = {
        'group_responsible_removed': 1,
        'user_quit_group': 3
    }
    assert stats == expected_stats
    root_service.fetcher.set_data(('staff', 'group'), deepcopy(cutted_group_data), for_lookup=('type', 'service'))
    assert_nothing_changed_if_nothing_done(root_service)

    with nothing_changed():
        deprive_depriving_groups()


@pytest.mark.robotless
def test_servicerole_groups(group_roots, flat_arda_users, settings):
    """Тестируем синхронизацию сервисных подгрупп"""

    root_dep, root_service, root_wiki = group_roots
    frodo = flat_arda_users.frodo
    sam = flat_arda_users.sam
    nazgul = flat_arda_users.nazgul
    darkforces = [flat_arda_users[username] for username in ('sauron', 'saruman', 'nazgul')]

    # точные числа неважны
    PLAN_BROTHERHOOD_SERVICE_ID = 1004
    PLAN_DWARVES_CUSTOMROLE_ID = 100
    STAFF_BROTHERHOOD_SERVICE_GROUP_ID = 110

    group_data = [
        {
            'id': 101,
            'url': 'svc_middle',
            'name': 'Middle Earth',
            'service': {
                'id': None
            }
        },
        {
            'id': STAFF_BROTHERHOOD_SERVICE_GROUP_ID,
            'url': 'svc_brotherhood',
            'name': 'Ring brotherhood',
            'parent': {
                'id': 101
            },
            'service': {
                'id': PLAN_BROTHERHOOD_SERVICE_ID
            },
        },
        {
            'id': 103,
            'url': 'svc_evil',
            'name': 'Lands of Evil',
            'parent': {
                'id': 101
            },
            'service': {
                'id': 1002
            },
            'responsibles': [
                {'person': {'id': flat_arda_users.sauron.center_id, 'official': {'is_dismissed': False}}},
            ]
        },
    ]
    memberships_data = []
    add_members(memberships_data, STAFF_BROTHERHOOD_SERVICE_GROUP_ID, ['frodo'])
    add_members(memberships_data, 103, [person.username for person in darkforces])
    service_role_data = [
        {
            'id': 10001,
            'url': 'svc_brotherhood_carriers',
            'name': 'Носители колец',
            'parent': {
                'id': STAFF_BROTHERHOOD_SERVICE_GROUP_ID,
                'service': {
                    'id': PLAN_BROTHERHOOD_SERVICE_ID,
                }
            },
            'role_scope': 'carriers',
        },
        {
            'id': 10002,
            'url': 'svc_sauron_spongers',
            'name': 'Прихлебатели Саурона',
            'parent': {
                'id': 103,
                'service': {
                    'id': PLAN_BROTHERHOOD_SERVICE_ID
                }
            },
            'role_scope': 'spongers',
        }
    ]
    add_members(memberships_data, 10001, ['frodo'])
    add_members(memberships_data, 10002, ['nazgul'])
    abc_data = [
        {
            'id': 200,
            'state': 'approved',
            'service': {
                'id': PLAN_BROTHERHOOD_SERVICE_ID,
                'slug': 'brotherhood'
            },
            'person': {
                'id': frodo.center_id
            },
            'role': {
                'id': settings.IDM_PLANNER_HEAD_ROLE,
                'scope': {'slug': 'carriers'}
            },
        },
        {
            'id': 201,
            'state': 'approved',
            'service': {
                'id': PLAN_BROTHERHOOD_SERVICE_ID,
                'slug': 'brotherhood'
            },
            'person': {
                'id': sam.center_id
            },
            'role': {
                'id': settings.IDM_PLANNER_DEPUTY_ROLE,
                'scope': {'slug': 'carriers'}
            },
        },
        {
            'id': 204,
            'state': 'approved',
            'service': {
                'id': 1002,
                'slug': 'SAURON_SPONGERS'
            },
            'person': {
                'id': nazgul.center_id
            },
            'role': {
                'id': settings.IDM_PLANNER_HEAD_ROLE,
                'scope': {'slug': 'spongers'}
            }
        }
    ]
    root_service.fetcher.set_data(('staff', 'group'), deepcopy(group_data), for_lookup=('type', 'service'))
    root_service.fetcher.set_data(('staff', 'group'), deepcopy(service_role_data), for_lookup=('type', 'servicerole'))
    root_service.fetcher.set_data(('abc', 'service_members'), deepcopy(abc_data))
    root_service.fetcher.set_data(('staff', 'groupmembership'), deepcopy(memberships_data))
    with CountIncreasedContext((Group, 5), (GroupResponsibility, 4), (GroupMembership, 4),
                               (Action, 13)) as new_data:
        result, _ = root_service.synchronize()
        assert result is True

    # создались сервисные группы
    spongers = Group.objects.get(slug='svc_sauron_spongers')
    carriers = Group.objects.get(slug='svc_brotherhood_carriers')
    assert list(spongers.members.all()) == [nazgul]
    assert list(carriers.members.all()) == [frodo]

    # Руководитель сервиса
    responsibility = GroupResponsibility.objects.get(group__external_id=110, rank='head')
    assert responsibility.user_id == frodo.id

    # Зам руководителя сервиса
    responsibility = GroupResponsibility.objects.get(group__external_id=110, rank='deputy')
    assert responsibility.user_id == sam.id

    actions = new_data.get_new_objects(Action)
    stats = get_action_stats(actions)
    expected_stats = {
        'group_created': 5,
        'group_responsible_added': 4,
        'user_joined_group': 4,
    }
    assert stats == expected_stats
    assert_nothing_changed_if_nothing_done(root_service)


@pytest.mark.robotless
def test_wiki_groups(group_roots, flat_arda_users):
    """Проверяем синхронизацию вики-групп"""

    root_dep, root_service, root_wiki = group_roots
    sauron = flat_arda_users['sauron']
    saruman = flat_arda_users['saruman']
    frodo = flat_arda_users['frodo']
    fellowship = [flat_arda_users[username] for username in ('frodo', 'legolas', 'aragorn', 'gimli', 'gandalf',
                                                             'boromir')]
    darkforces = [flat_arda_users[username] for username in ('sauron', 'saruman', 'nazgul')]

    group_data = [
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
            },
        },
        {
            'id': 103,
            'url': 'evil-lands',
            'name': 'Lands of Evil',
            'parent': {
                'id': 101
            },
        },
        {
            'id': 104,
            'url': 'mordor',
            'name': 'Mordor',
            'parent': {
                'id': 103
            },
            'responsibles': [
                {
                    'person': {
                        'id': sauron.center_id,
                        'official': {'is_dismissed': False},
                    },
                },
                {
                    'person': {
                        'id': saruman.center_id,
                        'official': {'is_dismissed': False},
                    }
                }
            ]
        },
        {
            'id': 110,
            'url': 'ring-brotherhood',
            'name': 'Ring brotherhood',
            'parent': {
                'id': 101
            },
            'responsibles': [
                {
                    'person': {
                        'id': frodo.center_id,
                        'official': {'is_dismissed': False},
                    }
                },
            ]
        }
    ]
    membership_pairs = [(110, person) for person in fellowship] + [(104, person) for person in darkforces]
    wiki_data = [
        {
            'id': person.center_id + group_id,
            'person': {
                'id': person.center_id,
                'official': {'is_dismissed': False},
            },
            'group': {
                'id': group_id
            }
        } for (group_id, person) in membership_pairs
    ]
    root_wiki.fetcher.set_data(('staff', 'group'), deepcopy(group_data))
    root_wiki.fetcher.set_data(('staff', 'groupmembership'), deepcopy(wiki_data))
    with CountIncreasedContext((Group, 5), (GroupResponsibility, 3), (GroupMembership, 9),
                               (Action, 5+3+9)):
        result, _ = root_wiki.synchronize()
        assert result is True

    assert sauron.member_of.count() == 1
    assert sauron.member_of.get().external_id == 104
    assert sauron.active_responsibilities.count() == 1
    responsibility = sauron.active_responsibilities.select_related('group').get()
    assert responsibility.group.external_id == 104
    assert responsibility.rank == 'head'

    mordor = Group.objects.get(slug='mordor')
    responsibility_data = list(mordor.active_responsibilities.values_list('user__username', 'rank').
                               order_by('user__username'))
    expected_data = [('saruman', 'head'), ('sauron', 'head')]
    assert responsibility_data == expected_data

    root_wiki.fetcher.set_data(('staff', 'group'), deepcopy(group_data))
    root_wiki.fetcher.set_data(('staff', 'groupmembership'), deepcopy(wiki_data))
    assert_nothing_changed_if_nothing_done(root_wiki)


def test_roles_hold(group_roots, flat_arda_users, simple_system):
    """Если сотрудник выходит из группы в unaware системе, его роль становится на холд"""
    root_dep, root_service, root_wiki = group_roots
    legolas = flat_arda_users['legolas']
    legolas.department_group = root_dep
    legolas.save(update_fields=['department_group'])
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
    memberships_data = []
    add_members(memberships_data, 101, ['legolas'])
    root_wiki.fetcher.set_data(('staff', 'group'), simple_staff_data)
    root_wiki.fetcher.set_data(('staff', 'groupmembership'), memberships_data)

    # ToDo: что будет с синком, если группа ещё не создана с таким external_id?

    # Action = 3 (group_created, group_responsible_added, user_joined_group)
    with CountIncreasedContext((Group, 1), (Action, 3)):
        result, _ = root_wiki.synchronize()
        assert result is True
    middle_earth = Group.objects.get(external_id=101)
    make_role(middle_earth, simple_system, {'role': 'manager'})
    user_role = Role.objects.get(user=legolas)
    assert user_role.state == 'granted'

    remove_members(memberships_data, 101, ['legolas'])
    root_wiki = refresh_dep_group(root_wiki, simple_staff_data, memberships_data)
    clear_mailbox()
    result, _ = root_wiki.synchronize()
    assert result is True
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
            'Ответственные за группу: legolas',
            'Список ролей в интерфейсе IDM:',
            'https://example.com/user/legolas#f-status=active,f-state=onhold,main=roles,sort-by=-updated',
        ],
        message.body
    )

    add_members(memberships_data, 101, ['legolas'])
    root_wiki = refresh_dep_group(root_wiki, simple_staff_data, memberships_data)
    result, _ = root_wiki.synchronize()
    assert result is True
    call_command('idm_poke_hanging_roles', '--stage', 'request_or_deprive_personal')

    user_role = refresh(user_role)
    assert user_role.state == 'granted'
    assert_action_chain(user_role, ['request', 'approve', 'first_add_role_push', 'grant', 'hold', 'grant'])


def test_roles_hold_external_user(group_roots, arda_users, simple_system):
    """Случай внешнего сотрудника"""

    frodo = arda_users.frodo
    frodo.affiliation = AFFILIATION.EXTERNAL
    frodo.save()

    root_dep, root_service, root_wiki = group_roots
    simple_staff_data = [{
        'id': 101,
        'url': 'middle-earth',
        'name': 'Middle Earth',
    }]
    memberships_data = []
    add_members(memberships_data, 101, ['frodo'])
    root_wiki.fetcher.set_data(('staff', 'group'), simple_staff_data)
    root_wiki.fetcher.set_data(('staff', 'groupmembership'), memberships_data)

    # Action = 3 (group_created, user_joined_group)
    with CountIncreasedContext((Group, 1), (Action, 2)):
        result, _ = root_wiki.synchronize()
        assert result is True

    middle_earth = Group.objects.get(external_id=101, type=GROUP_TYPES.WIKI)
    make_role(middle_earth, simple_system, {'role': 'manager'})
    user_role = Role.objects.get(user=frodo)
    assert user_role.state == 'granted'

    remove_members(memberships_data, 101, ['frodo'])
    root_wiki = refresh_dep_group(root_wiki, simple_staff_data, memberships_data)
    clear_mailbox()
    result, _ = root_wiki.synchronize()
    assert result is True
    call_command('idm_poke_hanging_roles', '--stage', 'request_or_deprive_personal')

    user_role = refresh(user_role)
    assert user_role.state == 'onhold'
    assert len(mail.outbox) == 0

    call_command('idm_send_roles_reminders')
    assert len(mail.outbox) == 1

    message = mail.outbox[0]
    assert message.to == ['varda@example.yandex.ru']
    assert message.subject == 'Некоторые роли Фродо Бэггинс скоро будут отозваны'
    assert_contains(
        [
            'Некоторые роли сотрудника Фродо Бэггинс скоро будут отозваны в связи с выходом из групп/удалением групп.',
            'Чтобы предотвратить отзыв ролей, вы можете восстановить членство в группе, '
            'связавшись с ответственными группы или запросить такую же персональную роль '
            'с помощью функции клонирования.',
            'Список ролей, которые скоро будут отозваны:',
            'В связи с выходом из группы/удалением группы Middle Earth будут отозваны роли:',
            'Система: Simple система. Роль: Менеджер. Дата отзыва:',
            '(https://example.com/system/simple/#role=%d)' % user_role.pk,
            'Список ролей в интерфейсе IDM:',
            'https://example.com/user/frodo#f-status=active,f-state=onhold,main=roles,sort-by=-updated',
        ],
        message.body
    )

    add_members(memberships_data, 101, ['frodo'])
    root_wiki = refresh_dep_group(root_wiki, simple_staff_data, memberships_data)
    result, _ = root_wiki.synchronize()
    assert result is True
    call_command('idm_poke_hanging_roles', '--stage', 'request_or_deprive_personal')

    user_role = refresh(user_role)
    assert user_role.state == 'granted'
    assert_action_chain(user_role, ['request', 'approve', 'first_add_role_push', 'grant', 'hold', 'grant'])


def test_roles_hold_robot(group_roots, flat_arda_users, simple_system, robot_gollum):
    """Случай с роботом, проверка отправки сообщения владельцу."""
    root_dep, root_service, root_wiki = group_roots
    legolas = flat_arda_users['legolas']
    robot_gollum.department_group = root_dep
    robot_gollum.add_responsibles([legolas])
    robot_gollum.save()
    simple_staff_data = [
        {
            'id': 101,
            'url': 'middle-earth',
            'name': 'Middle Earth',
        }
    ]
    memberships_data = []
    add_members(memberships_data, 101, ['gollum'])
    root_wiki.fetcher.set_data(('staff', 'group'), simple_staff_data)
    root_wiki.fetcher.set_data(('staff', 'groupmembership'), memberships_data)
    root_wiki.synchronize()

    middle_earth = Group.objects.get(external_id=101)
    make_role(middle_earth, simple_system, {'role': 'manager'})
    user_role = Role.objects.get(user=robot_gollum)

    remove_members(memberships_data, 101, ['gollum'])
    root_wiki = refresh_dep_group(root_wiki, simple_staff_data, memberships_data)
    clear_mailbox()
    root_wiki.synchronize()
    call_command('idm_poke_hanging_roles', '--stage', 'request_or_deprive_personal')

    user_role = refresh(user_role)
    assert user_role.state == 'onhold'
    assert len(mail.outbox) == 0

    call_command('idm_send_roles_reminders')
    assert len(mail.outbox) == 1
    message = mail.outbox[0]
    assert message.to == ['legolas@example.yandex.ru']
    assert message.subject == 'Некоторые роли gollum скоро будут отозваны'
    assert_contains(
        [
            'Некоторые роли робота gollum скоро будут отозваны в связи с выходом из групп/удалением групп.',
            'Чтобы предотвратить отзыв ролей, вы можете восстановить членство в группе, '
            'связавшись с ответственными группы или запросить такую же персональную роль '
            'с помощью функции клонирования.',
            'Список ролей, которые скоро будут отозваны:',
            'В связи с выходом из группы/удалением группы Middle Earth будут отозваны роли:',
            'Система: Simple система. Роль: Менеджер. Дата отзыва:',
            '(https://example.com/system/simple/#role=%d)' % user_role.pk,
            'Список ролей в интерфейсе IDM:',
            'https://example.com/user/gollum#f-status=active,f-state=onhold,main=roles,sort-by=-updated',
        ],
        message.body
    )


def test_roles_hold_deprive(group_roots, flat_arda_users, simple_system):
    """замороженную роль можно отозвать"""
    root_dep, root_service, root_wiki = group_roots
    legolas = flat_arda_users.legolas
    legolas.department_group = root_dep
    legolas.save(update_fields=['department_group'])
    simple_staff_data = [
        {
            'id': 101,
            'url': 'middle-earth',
            'name': 'Middle Earth',
        }
    ]
    memberships_data = []
    add_members(memberships_data, 101, ['legolas'])
    root_wiki.fetcher.set_data(('staff', 'group'), simple_staff_data)
    root_wiki.fetcher.set_data(('staff', 'groupmembership'), memberships_data)
    root_wiki.synchronize()
    middle_earth = Group.objects.get(external_id=101)
    make_role(middle_earth, simple_system, {'role': 'manager'})
    user_role = Role.objects.get(user=legolas)
    assert user_role.state == 'granted'

    remove_members(memberships_data, 101, ['legolas'])
    root_wiki = refresh_dep_group(root_wiki, simple_staff_data, memberships_data)
    result, _ = root_wiki.synchronize()
    assert result is True
    call_command('idm_poke_hanging_roles', '--stage', 'request_or_deprive_personal')

    user_role = refresh(user_role)
    assert user_role.state == 'onhold'

    user_role.deprive_or_decline(legolas)

    user_role = refresh(user_role)
    assert user_role.state == 'deprived'
    assert_action_chain(user_role, [
        'request', 'approve', 'first_add_role_push', 'grant', 'hold', 'deprive', 'first_remove_role_push', 'remove',
    ])


def test_postponed_depriving(group_roots, flat_arda_users, simple_system, settings):
    """Протестируем отложенный отзыв группы"""

    set_workflow(simple_system, group_code=DEFAULT_WORKFLOW)
    root_dep = group_roots[0]
    frodo = flat_arda_users.frodo

    root_data = {
        'id': 101,
        'url': 'middle-earth',
        'name': 'Middle Earth',
        'department': {
            'heads': [{
                'person': {'id': frodo.center_id, 'official': {'is_dismissed': False}},
                'role': 'chief'
            }]
        }
    }
    small_data = [
        root_data,
        {
            'id': 102,
            'url': 'good-lands',
            'name': 'Lands of Good',
            'parent': {
                'id': 101
            }
        }
    ]
    root_dep.fetcher.set_data(('staff', 'group'), small_data)
    root_dep.fetcher.set_data(('staff', 'groupmembership'), [])
    with CountIncreasedContext((Group, 2)):
        result, _ = root_dep.synchronize()
        assert result is True

    group = Group.objects.get(slug='good-lands')
    role = Role.objects.request_role(frodo, group, simple_system, '', {'role': 'manager'}, None)
    role = refresh(role)
    assert role.state == 'granted'

    # оп, пропала группа
    small_data = [root_data]
    root_dep.fetcher.set_data(('staff', 'group'), small_data)
    with CountIncreasedContext((Group, 0)):
        result, _ = root_dep.synchronize()
        assert result is True

    # но ничего такого не случилось!
    now = timezone.now()
    group = refresh(group)
    assert group.state == 'depriving'
    assert group.expire_at > now
    role = refresh(role)
    assert role.state == 'granted'

    # проставим expire_at группы в конкретное значение, а потом проверим, что повторная синхронизация не меняет его
    saved_dt = group.expire_at
    fixed_dt = timezone.make_aware(datetime(2016, 12, 31, 1, 2, 3), timezone.utc)
    arda = Group.objects.get(slug='arda')  # тоже в depriving
    assert arda.state == 'depriving'
    arda.expire_at = fixed_dt
    arda.save(update_fields=('expire_at',))
    small_data = [root_data]
    root_dep.fetcher.set_data(('staff', 'group'), small_data)
    with CountIncreasedContext((Group, 0)):
        root_dep.synchronize()
    arda = refresh(arda)
    assert arda.expire_at == fixed_dt
    arda.expire_at = saved_dt
    arda.save(update_fields=('expire_at',))

    # но стоит пройти небольшому количеству времени, и всё отзовётся:
    with freeze_time(now + timedelta(hours=settings.IDM_DEPRIVING_GROUP_HOURS, seconds=1)):
        deprive_depriving_groups()

    group = refresh(group)
    assert group.state == 'deprived'
    arda = refresh(arda)
    assert arda.state == 'deprived'
    role = refresh(role)
    assert role.state == 'onhold'
    assert_action_chain(role, ['request', 'apply_workflow', 'approve', 'grant', 'hold'])
    deprive_action = role.actions.get(action='hold')
    assert deprive_action.comment == 'Роль отложена в связи с удалением группы'


def test_group_restored(group_roots, flat_arda_users, simple_system):
    """Протестируем восстановление группы в случае, если она сначала пропала, а потом снова появилась"""

    set_workflow(simple_system, group_code=DEFAULT_WORKFLOW)
    root_dep = group_roots[0]
    frodo = flat_arda_users.frodo

    root_data = {
        'id': 101,
        'url': 'middle-earth',
        'name': 'Middle Earth',
        'department': {
            'heads': [{
                'person': {'id': frodo.center_id, 'official': {'is_dismissed': False}},
                'role': 'chief'
            }]
        }
    }
    child_data = {
        'id': 102,
        'url': 'good-lands',
        'name': 'Lands of Good',
        'parent': {
            'id': 101
        }
    }
    grandchild_data = {
        'id': 103,
        'url': 'valinor',
        'name': 'Valinor',
        'parent': {
            'id': 102
        }
    }
    small_data = [root_data, child_data, grandchild_data]
    root_dep.fetcher.set_data(('staff', 'group'), small_data)
    root_dep.fetcher.set_data(('staff', 'groupmembership'), [])
    with CountIncreasedContext((Group, 3)):
        result, _ = root_dep.synchronize()
        assert result is True

    group = Group.objects.get(slug='good-lands')
    valinor = Group.objects.get(slug='valinor')
    role = Role.objects.request_role(frodo, group, simple_system, '', {'role': 'manager'}, None)
    role = refresh(role)
    assert role.state == 'granted'

    # оп, пропали группы
    small_data = [root_data]
    root_dep.fetcher.set_data(('staff', 'group'), small_data)
    with CountIncreasedContext((Group, 0)):
        result, _ = root_dep.synchronize()
        assert result is True

    # но ничего такого не случилось!
    now = timezone.now()
    group = refresh(group)
    assert group.state == 'depriving'
    assert group.expire_at > now
    role = refresh(role)
    assert role.state == 'granted'
    valinor = refresh(valinor)
    assert valinor.state == 'depriving'

    # группы вернулись, человек сделает всё остальное:
    small_data = [root_data, child_data, grandchild_data]
    root_dep.fetcher.set_data(('staff', 'group'), small_data)
    with CountIncreasedContext((Group, 0)):
        result, _ = root_dep.synchronize()
        assert result is True

    group = refresh(group)
    assert group.state == 'active'
    assert group.expire_at is None
    role = refresh(role)
    assert role.state == 'granted'
    valinor = refresh(valinor)
    assert valinor.state == 'active'


def test_onhold_is_silent_if_role_is_still_applicable(group_roots, arda_users, simple_system):
    """Протестируем случай, когда человек выходит из группы, но его роль остаётся активной.
    Роль должна переходить в onhold, но письмо уходить не должно."""

    set_workflow(simple_system, group_code=DEFAULT_WORKFLOW)
    dep_root, service_root, wiki_root = group_roots
    frodo = arda_users.frodo
    gandalf = arda_users.gandalf
    add_perms_by_role('responsible', gandalf, simple_system)

    wiki_data = [
        {
            'id': 200,
            'url': 'allstaff',
            'name': 'All staff'
        },
        {
            'id': 300,
            'url': 'other-wiki-group',
            'name': 'Other group',
        },
    ]
    wiki_membership_data = [
        {
            'id': 1000,
            'person': {'id': frodo.center_id, 'official': {'is_dismissed': False}},
            'group': {'id': 200},
        },
        {
            'id': 2000,
            'person': {'id': frodo.center_id, 'official': {'is_dismissed': False}},
            'group': {'id': 300},
        },
    ]

    wiki_data_copy = deepcopy(wiki_data)
    wiki_root.fetcher.set_data(('staff', 'group'), wiki_data)
    wiki_root.fetcher.set_data(('staff', 'groupmembership'), wiki_membership_data)
    wiki_root.synchronize()
    call_command('idm_poke_hanging_roles', '--stage', 'request_or_deprive_personal')
    allstaf_group = Group.objects.get(slug='allstaff')
    assert allstaf_group.members.get() == frodo

    frodo.refresh_from_db()
    allstaf_role = Role.objects.request_role(gandalf, allstaf_group, simple_system, '', {'role': 'manager'}, None)
    allstaf_role = refresh(allstaf_role)
    assert allstaf_role.state == 'granted'
    assert allstaf_role.refs.count() == 1
    allstaf_ref = allstaf_role.refs.get()
    assert allstaf_ref.state == 'granted'

    other_group = Group.objects.get(slug='other-wiki-group')
    other_role = Role.objects.request_role(gandalf, other_group, simple_system, '', {'role': 'manager'}, None)
    other_ref = other_role.refs.get()
    assert other_ref.state == 'granted'

    # frodo пропадает из группы `Other group`
    wiki_membership_data.pop()
    wiki_root.fetcher.set_data(('staff', 'group'), wiki_data)
    wiki_root.fetcher.set_data(('staff', 'groupmembership'), wiki_membership_data)
    wiki_root.synchronize()
    call_command('idm_poke_hanging_roles', '--stage', 'request_or_deprive_personal')
    result, _ = dep_root.synchronize()
    assert result is True

    other_ref = refresh(other_ref)
    assert other_ref.state == 'onhold'
    allstaf_ref = refresh(allstaf_ref)
    assert allstaf_ref.state == 'granted'
    assert len(mail.outbox) == 0
    call_command('idm_send_roles_reminders')
    assert len(mail.outbox) == 0

    # про последнюю роль в onhold письмо должно уйти
    wiki_root.fetcher.set_data(('staff', 'group'), wiki_data_copy)
    wiki_root.fetcher.set_data(('staff', 'groupmembership'), [])
    wiki_root.synchronize()
    call_command('idm_poke_hanging_roles', '--stage', 'request_or_deprive_personal')
    allstaf_ref = refresh(allstaf_ref)
    assert allstaf_ref.state == 'onhold'
    assert len(mail.outbox) == 0
    call_command('idm_send_roles_reminders')
    assert len(mail.outbox) == 1
    message = mail.outbox[0]
    assert message.to == ['frodo@example.yandex.ru']
    assert message.subject == 'Некоторые роли скоро будут отозваны'
    assert_contains([
        'Некоторые ваши роли скоро будут отозваны в связи с выходом из групп/удалением групп.',
        'Чтобы предотвратить отзыв ролей, вы можете восстановить членство в группе, связавшись с ответственными '
        'группы или запросить такую же персональную роль с помощью функции клонирования.',
        'Список ролей, которые скоро будут отозваны:',
        'В связи с выходом из группы/удалением группы All staff будут отозваны роли:',
        'Система: Simple система. Роль: Менеджер. Дата отзыва:',
        'В связи с выходом из группы/удалением группы Other group будут отозваны роли:',
        'Система: Simple система. Роль: Менеджер. Дата отзыва:',
    ], message.body)


def test_group_is_teleporting(group_roots, flat_arda_users, simple_system):
    """ Проверим случай, когда группа сначала исчезла, а потом появилась в другом месте """

    set_workflow(simple_system, group_code=DEFAULT_WORKFLOW)
    root_dep = group_roots[0]
    frodo = flat_arda_users.frodo

    root_data = {
        'id': 101,
        'url': 'middle-earth',
        'name': 'Middle Earth',
        'department': {
            'heads': [{
                'person': {'id': frodo.center_id, 'official': {'is_dismissed': False}},
                'role': 'chief'
            }]
        }
    }
    child_data = {
        'id': 102,
        'url': 'good-lands',
        'name': 'Lands of Good',
        'parent': {
            'id': 101
        }
    }
    grandchild_data = {
        'id': 103,
        'url': 'valinor',
        'name': 'Valinor',
        'parent': {
            'id': 102
        }
    }
    small_data = [root_data, child_data, grandchild_data]
    root_dep.fetcher.set_data(('staff', 'group'), small_data)
    root_dep.fetcher.set_data(('staff', 'groupmembership'), [])
    with CountIncreasedContext((Group, 3)):
        result, _ = root_dep.synchronize()
        assert result is True
        Transfer.objects.create_user_group_transfers()
        call_command('idm_poke_hanging_roles', '--stage', 'request_or_deprive_personal')

    valinor = Group.objects.get(slug='valinor')
    role = Role.objects.request_role(frodo, valinor, simple_system, '', {'role': 'manager'}, None)
    role = refresh(role)
    assert role.state == 'granted'

    # пропадает внучатая группа
    small_data = [root_data, child_data]
    root_dep.fetcher.set_data(('staff', 'group'), small_data)
    with CountIncreasedContext((Group, 0)):
        result, _ = root_dep.synchronize()
        assert result is True
        Transfer.objects.create_user_group_transfers()
        call_command('idm_poke_hanging_roles', '--stage', 'request_or_deprive_personal')

    # и появляется в другом месте
    grandchild_data['parent']['id'] = 101
    small_data = [root_data, child_data, grandchild_data]
    root_dep.fetcher.set_data(('staff', 'group'), small_data)
    with CountIncreasedContext((Group, 0)):
        result, _ = root_dep.synchronize()
        assert result is True
        Transfer.objects.create_user_group_transfers()
        call_command('idm_poke_hanging_roles', '--stage', 'request_or_deprive_personal')

    valinor = refresh(valinor)
    assert valinor.state == 'active'
    assert valinor.expire_at is None
    role = refresh(role)
    assert role.state == 'granted'
    assert role.group_id == valinor.id
    assert Transfer.objects.count() == 1
    transfer = Transfer.objects.select_related('user', 'group').get()
    transfer.accept(bypass_checks=True)
    role = refresh(role)
    assert role.state == 'need_request'


def test_add_members_to_moved_group(group_roots, arda_users, simple_system):
    """ Проверим случай добавления людей в переехавшую группу"""

    set_workflow(simple_system, group_code=DEFAULT_WORKFLOW)
    root_wiki = group_roots[2]
    frodo = arda_users.frodo
    legolas = arda_users.legolas
    gandalf = arda_users.gandalf
    add_perms_by_role('responsible', frodo, simple_system)

    root_data = {
        'id': 101,
        'url': 'middle-earth',
        'name': 'Middle Earth',
        'department': {
            'heads': [{
                'person': {'id': frodo.center_id, 'official': {'is_dismissed': False}},
                'role': 'chief'
            }]
        }
    }
    child_data = {
        'id': 102,
        'url': 'good-lands',
        'name': 'Lands of Good',
        'parent': {
            'id': 101
        }
    }
    grandchild_data = {
        'id': 103,
        'url': 'valinor',
        'name': 'Valinor',
        'parent': {
            'id': 102
        }
    }
    small_data = [root_data, child_data, grandchild_data]
    memberships_data = []
    root_wiki.fetcher.set_data(('staff', 'group'), small_data)
    root_wiki.fetcher.set_data(('staff', 'groupmembership'), memberships_data)
    with CountIncreasedContext((Group, 3)):
        result, _ = root_wiki.synchronize()
        assert result is True
        call_command('idm_poke_hanging_roles', '--stage', 'request_or_deprive_personal')

    root = Group.objects.get(slug='middle-earth', type=GROUP_TYPES.WIKI)
    valinor = Group.objects.get(slug='valinor', type=GROUP_TYPES.WIKI)
    role = Role.objects.request_role(frodo, root, simple_system, '', {'role': 'manager'}, None)
    role = refresh(role)
    assert role.state == 'granted'

    # группа переезжает, добавляются новые люди
    grandchild_data['parent']['id'] = 101
    add_members(memberships_data, 103, ['legolas'])
    small_data = [root_data, child_data, grandchild_data]
    root_wiki.fetcher.set_data(('staff', 'group'), small_data)
    root_wiki.fetcher.set_data(('staff', 'groupmembership'), memberships_data)
    with CountIncreasedContext((Group, 0)):
        result, _ = root_wiki.synchronize()
        sync_indirect_memberships()
        call_command('idm_poke_hanging_roles', '--stage', 'request_or_deprive_personal')

    valinor = refresh(valinor)
    assert valinor.state == 'active'
    assert valinor.expire_at is None
    assert valinor.parent.slug == 'middle-earth'
    ref = legolas.roles.get()
    assert ref.state == 'granted'
    assert ref.parent_id == role.id

    # добавим ещё людей отдельно
    add_members(memberships_data, 103, ['legolas', 'gandalf'])
    root_wiki.fetcher.set_data(('staff', 'group'), small_data)
    root_wiki.fetcher.set_data(('staff', 'groupmembership'), memberships_data)
    with CountIncreasedContext((Group, 0)):
        result, _ = root_wiki.synchronize()
        sync_indirect_memberships()
        call_command('idm_poke_hanging_roles', '--stage', 'request_or_deprive_personal')

    gandalf_ref = gandalf.roles.get()
    assert gandalf_ref.state == 'granted'
    assert gandalf_ref.parent_id == role.id


def test_sync_unknown_member(group_roots, simple_system):
    """Проверим, что синхронизация падает, если в members приходит id пользователя, о котором мы ещё не знаем"""

    set_workflow(simple_system, group_code=DEFAULT_WORKFLOW)
    dep_root, service_root, wiki_root = group_roots
    simple_data = [{
        'id': 101,
        'url': 'middle-earth-user',
        'name': 'Middle Earth',
    }]
    memberships_data = [{
        'id': 100,
        'group': {'id': 101},
        'person': {'id': 1000**3, 'official': {'is_dismissed': False}},  # неизвестный id
    }]

    wiki_root.fetcher.set_data(('staff', 'group'), simple_data)
    wiki_root.fetcher.set_data(('staff', 'groupmembership'), memberships_data)

    with pytest.raises(SynchronizationError) as exc:
        result, _ = wiki_root.synchronize()
    assert str(exc.value) == 'Unknown member in group 101'

    # ответственного тоже может не быть
    unknown_responsible_data = [{
        'id': 101,
        'url': 'middle-earth-resp',
        'name': 'Middle Earth',
        'department': {
            'heads': [{
                'person': {'id': 1000**3, 'official': {'is_dismissed': False}},  # неизвестный id
                'role': 'chief'
            }]
        }
    }]
    dep_root.fetcher.set_data(('staff', 'group'), unknown_responsible_data)
    dep_root.fetcher.set_data(('staff', 'groupmembership'), [])
    with pytest.raises(SynchronizationError) as exc:
        result, _ = dep_root.synchronize()
    assert str(exc.value) == 'Unknown responsible in group 101'


def test_sync_user_is_both_head_and_deputy(group_roots, simple_system, flat_arda_users):
    """Проверим, что человек одновременно может быть и руководителем, и заместителем, и мы не падаем от этого.
    Такое действительно случалось в продакшене: RULES-3866. Заодно проверим возможность дубликатов записей об
    ответственности"""

    frodo = flat_arda_users.frodo
    root_dep = group_roots[0]
    head_and_deputy_data = [{
        'id': 101,
        'url': 'middle-earth-user',
        'name': 'Middle Earth',
        'department': {
            'heads': [{
                'person': {'id': frodo.center_id, 'official': {'is_dismissed': False}},
                'role': 'chief',
            },  {
                'person': {'id': frodo.center_id, 'official': {'is_dismissed': False}},
                'role': 'chief',
            },  {
                'person': {'id': frodo.center_id, 'official': {'is_dismissed': False}},
                'role': 'deputy',
            }]
        }
    }]

    root_dep.fetcher.set_data(('staff', 'group'), head_and_deputy_data)
    root_dep.fetcher.set_data(('staff', 'groupmembership'), [])
    result, _ = root_dep.synchronize()


def test_dismissed_users_are_not_group_members(group_roots, flat_arda_users):
    """Уволенные сотрудники не могут быть ни членами групп, ни ответственными за группы"""

    root_dep, root_service, root_wiki = group_roots
    gandalf = flat_arda_users.gandalf
    saruman = flat_arda_users.saruman
    aragorn = flat_arda_users.aragorn
    legolas = flat_arda_users.legolas
    frodo = flat_arda_users.frodo

    simple_staff_data = [
        {
            'id': 101,
            'url': 'middle-earth',
            'name': 'Middle Earth',
            'department': {
                'heads': [
                    {
                        'role': 'head',
                        'person': {
                            'id': gandalf.center_id,
                            'official': {'is_dismissed': True},
                        }
                    },
                    {
                        'role': 'deputy',
                        'person': {
                            'id': frodo.center_id,
                            'official': {'is_dismissed': False},
                        }
                    },
                    {
                        'rank': 'deputy',
                        'person': {
                            'id': saruman.center_id,
                            'official': {'is_dismissed': True},
                        }
                    }
                ],
            },
        }
    ]
    memberships_data = []
    add_members(memberships_data, 101, ['legolas'])
    memberships_data.append({
        'id': 100,
        'group': {'id': 101},
        'person': {
            'id': aragorn.center_id,
            'official': {'is_dismissed': True},
        }
    })
    root_dep.fetcher.set_data(('staff', 'group'), simple_staff_data)
    root_dep.fetcher.set_data(('staff', 'groupmembership'), memberships_data)
    root_dep.synchronize()

    assert GroupResponsibility.objects.count() == 1
    responsibility = GroupResponsibility.objects.get()
    assert responsibility.user_id == frodo.id
    assert responsibility.rank == 'deputy'
    assert responsibility.is_active is True

    # Состав департаментных групп обновляем при синхронизации пользователей
    # для теста используем wiki-группу
    wiki_data = [{
        'id': 101,
        'url': 'middle-earth-user',
        'name': 'Middle Earth',
    }]

    root_wiki.fetcher.set_data(('staff', 'group'), wiki_data)
    root_wiki.fetcher.set_data(('staff', 'groupmembership'), memberships_data)
    root_wiki.synchronize()

    assert GroupMembership.objects.count() == 1
    membership = GroupMembership.objects.get()
    assert membership.user_id == legolas.id
    assert membership.state == GROUPMEMBERSHIP_STATE.ACTIVE


def test_idm_deprive_roles_group_refs(simple_system, flat_arda_users, group_roots):
    """Проверяем, что если сначала удалить сотрудника из группы, а потом уволить, связанные с групповыми
    роли будут сначала перемещены в onhold, а потом удалены сразу же при увольнении
    """
    root_dep, root_service, root_wiki = group_roots
    legolas = flat_arda_users.legolas
    # У пользователя должен быть какой-то департамент
    legolas.department_group = root_dep
    legolas.save(update_fields=['department_group'])
    simple_staff_data = [
        {
            'id': 101,
            'url': 'middle-earth',
            'name': 'Middle Earth',
        }
    ]
    membership_data = []
    add_members(membership_data, 101, ['legolas'])
    root_wiki.fetcher.set_data(('staff', 'group'), simple_staff_data)
    root_wiki.fetcher.set_data(('staff', 'groupmembership'), membership_data)
    root_wiki.synchronize()
    middle_earth = Group.objects.get(external_id=101)
    make_role(middle_earth, simple_system, {'role': 'manager'})
    user_role = Role.objects.get(user=legolas)
    assert user_role.state == 'granted'

    remove_members(membership_data, 101, ['legolas'])
    root_wiki = refresh_dep_group(root_wiki, simple_staff_data, membership_data)
    result, _ = root_wiki.synchronize()
    assert result is True
    call_command('idm_poke_hanging_roles', '--stage', 'request_or_deprive_personal')

    user_role = refresh(user_role)
    assert user_role.state == 'onhold'

    legolas.is_active = False
    legolas.save()

    call_command('idm_deprive_roles')

    user_role = refresh(user_role)
    assert user_role.state == 'deprived'


@pytest.mark.robotless
def test_department_num_queries(group_roots, flat_arda_users, department_structure, django_assert_num_queries):
    """Тестируем синхронизацию сервисных групп"""

    root_dep, root_service, root_wiki = group_roots

    group_data = []
    memberships_data = []
    for group in root_dep.get_descendants():
        data = {'id': group.external_id, 'url': group.slug, 'name': group.name_en}
        if group.parent.external_id:
            data['parent'] = {'id': group.parent.external_id}
        group_data.append(data)
        add_members(memberships_data, group.external_id, [user.username for user in group.members.all()])

    Group.objects.update(hash='')

    root_dep.fetcher.set_data(('staff', 'group'), deepcopy(group_data))
    root_dep.fetcher.set_data(('staff', 'groupmembership'), memberships_data)
    with django_assert_num_queries(78):
        result, _ = root_dep.synchronize()
        assert result is True
