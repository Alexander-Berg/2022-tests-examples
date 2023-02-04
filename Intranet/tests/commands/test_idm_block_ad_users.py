# -*- coding: utf-8 -*-
import datetime
import re

import pytest
import requests
from django.conf import settings
from django.core import mail
from django.core import management
from django.core.management import CommandError
from django.utils.encoding import force_bytes
from freezegun import freeze_time
from mock import patch, MagicMock, call, ANY

from idm.core.models import Action
from idm.monitorings.metric import FiredUsersLimitExceededMetric
from idm.tests.utils import create_fake_response, assert_contains, capture_requests, mock_ids_repo, MockedTrackerIssue
from idm.users.models import User
from idm.utils import http

pytestmark = [pytest.mark.django_db]


@pytest.fixture
def dismissed_user(arda_users):
    frodo = arda_users.frodo
    frodo.is_active = False
    frodo.ldap_active = False
    frodo.save()
    return frodo


@pytest.fixture
def dismissed_user_for_block(arda_users):
    frodo = arda_users.frodo
    frodo.is_active = False
    frodo.ldap_active = True
    frodo.save()
    return frodo


def test_block_user(dismissed_user, simple_system, fake_newldap):
    """
    TestpalmID: 3456788-223
    """
    fake_newldap.search_s.return_value = [(
        'CN=Frodo Baggins,CN=Users,DC=Ruler,DC=tst', {
            'cn': ['Frodo Baggins'],
            'userAccountControl': [1],
            'memberOf': ['SomeGroup'],
        }
    )]

    with patch.object(requests.sessions.Session, 'request') as get:
        get.return_value = create_fake_response([])
        with mock_ids_repo('startrek2') as st_repo:
            st_repo.create.return_value = MockedTrackerIssue('ISSUE-123')
            management.call_command('idm_block_ad_users', mode='full')

    assert dismissed_user.actions.count() == 2
    disable_action = dismissed_user.actions.get(action='user_ad_disable')
    assert disable_action.data == {
        'reason': 'Повторная блокировка пользователя, которого уже блокировали ранее.'
    }
    move_to_old_action = dismissed_user.actions.get(action='user_ad_move_to_old')
    assert move_to_old_action.data == {
        'reason': 'Повторная блокировка пользователя, которого уже блокировали ранее.',
        'original_ou': 'CN=Users,DC=ld,DC=yandex,DC=ru',
    }

    assert len(fake_newldap.modify_s.call_args_list) == 2
    assert fake_newldap.modify_s.call_args_list == [
        call('CN=Frodo Baggins,CN=Users,DC=Ruler,DC=tst', [
            (2, 'userAccountControl', b'3')
        ]),
        call('SomeGroup', [
            (1, 'member', b'CN=Frodo Baggins,CN=Users,DC=Ruler,DC=tst')
        ])
    ]
    # перемещение в Old Users
    assert fake_newldap.rename_s.call_args_list == [
        call('CN=Frodo Baggins,CN=Users,DC=Ruler,DC=tst', 'CN=Frodo Baggins', 'OU=OLD Users,DC=ld,DC=yandex,DC=ru')
    ]


@pytest.mark.parametrize('can_block,can_move', [
    [False, True],
    [True, False],
])
def test_cannot_block_user(dismissed_user, simple_system, fake_newldap, can_block, can_move):
    fake_newldap.search_s.return_value = [(
        'CN=Frodo Baggins,CN=Users,DC=Ruler,DC=tst', {
            'cn': ['Frodo Baggins'],
            'userAccountControl': [1],
        }
    )]
    if not can_block:
        fake_newldap.modify_s.side_effect = Exception
    if not can_move:
        fake_newldap.rename_s.side_effect = Exception

    with capture_requests() as mocked:
        mocked.http_get.return_value = create_fake_response([])
        with mock_ids_repo('startrek2') as st_repo:
            st_repo.create.return_value = MockedTrackerIssue('ISSUE-123')
            management.call_command('idm_block_ad_users', mode='full')

    if can_block:
        st_repo.create.assert_not_called()
    else:
        expected_calls = [
            call(
                components=None, description=ANY, 
                followers=[], queue='WINADMINREQHD', 
                summary='Не удалось заблокировать пользователей в AD', 
                type='task'
            )
        ]
        
        st_repo.create.assert_has_calls(expected_calls)
    assert dismissed_user.actions.count() == 1
    if can_block:
        assert dismissed_user.actions.filter(action='user_ad_disable').exists()
    if can_move:
        assert dismissed_user.actions.filter(action='user_ad_move_to_old').exists()


# дата хорошая: сегодня, но статус странный - блокируем
block_params = [['2017-07-17', state, True] for state in (0, 10, 20, 30, 60, 70)]
# статус хороший, но дата слишком в прошлом - блокируем
block_params.extend(['2017-07-08', state, True] for state in (40, 50))
# статус хороший, но дата слишком в будущем - блокируем
block_params.extend(['2018-07-31', state, True] for state in (40, 50))
# дата отстоит от текущей не больше чем на пару недель в прошлое, статус хороший - не блокируем, человека нанимают
block_params.extend(['2017-07-10', state, False] for state in (40, 50))
# дата отстоит от текущей не больше чем на год в будущее, статус хороший - не блокируем, тут человека нанимают
block_params.extend(['2018-05-01', state, False] for state in (40, 50))


@pytest.mark.parametrize('join_at,state,will_block', block_params)
def test_hired_users(dismissed_user, fake_newldap, join_at, state, will_block):
    fake_newldap.search_s.return_value = [(
        'CN=Frodo Baggins,CN=Users,DC=Ruler,DC=tst', {
            'cn': ['Frodo Baggins'],
            'userAccountControl': [1],
            'memberOf': ['SomeGroup'],
        }
    )]
    with capture_requests() as mocked:
        mocked.http_get.return_value = create_fake_response([
            {'username': 'frodo', 'join_at': join_at, 'state': state},
            # ещё нет на стаффе
            {'username': 'newborn', 'join_at': '2017-07-17', 'state': 50},
        ])

        with freeze_time(datetime.datetime(2017, 7, 17)):
            management.call_command('idm_block_ad_users', mode='full')
    dismissed_user.refresh_from_db()
    if not will_block:
        assert dismissed_user.ldap_blocked_timestamp is None
        assert fake_newldap.modify_s.call_args_list == []
    else:
        assert dismissed_user.ldap_blocked_timestamp is not None
        assert fake_newldap.modify_s.call_args_list == [
            call('CN=Frodo Baggins,CN=Users,DC=Ruler,DC=tst', [(2, 'userAccountControl', b'3')]),
            call('SomeGroup', [(1, 'member', b'CN=Frodo Baggins,CN=Users,DC=Ruler,DC=tst')])
        ]


def test_nobody_is_blocked(arda_users):
    """Никого блокировать не нужно"""
    with capture_requests() as mocked:
        mocked.http_get.return_value = create_fake_response([])
        management.call_command('idm_block_ad_users', mode='full')

    for user in arda_users:
        if user == 'tvm_app': continue
        user = getattr(arda_users, user)
        assert user.ldap_blocked_timestamp is None


def test_fast_block(dismissed_user_for_block, fake_newldap, settings):
    fake_newldap.search_s = MagicMock()
    fake_newldap.search_s.return_value = [('CN=Frodo Baggins,CN=Users,DC=Ruler,DC=tst', {
        'cn': ['Frodo Baggins'],
        'userAccountControl': [1],
    })]

    # Отключаем аккаунт
    with patch.object(http, 'post') as mock_post:
        management.call_command('idm_block_ad_users', mode='fast')
        mock_post.assert_called_with(
            settings.PASSPORT_CLOSE_SESSIONS_URL,
            json={'username': 'frodo'},
            tvm_id=settings.PASSPORT_CLOSE_SESSIONS_TVM_ID,
        )

    assert Action.objects.count() == 2
    move_to_old = dismissed_user_for_block.actions.get(action='user_ad_move_to_old')
    ad_disable = dismissed_user_for_block.actions.get(action='user_ad_disable')
    assert move_to_old.data == {
        'reason': 'Увольнение сотрудника по данным из staff',
        'original_ou': 'CN=Users,DC=ld,DC=yandex,DC=ru'
    }
    assert ad_disable.data == {'reason': 'Увольнение сотрудника по данным из staff'}

    dismissed_user_for_block.refresh_from_db()
    assert dismissed_user_for_block.ldap_active is False
    assert dismissed_user_for_block.ldap_blocked is False

    fake_newldap.rename_s.assert_called_with('CN=Frodo Baggins,CN=Users,DC=Ruler,DC=tst',
                                             'CN=Frodo Baggins', 'OU=OLD Users,DC=ld,DC=yandex,DC=ru')
    fake_newldap.modify_s.assert_called_with('CN=Frodo Baggins,CN=Users,DC=Ruler,DC=tst',
                                             [(2, 'userAccountControl', b'3')])


def test_disable_account_not_removing_from_groups(dismissed_user_for_block, fake_newldap, settings):
    # Теперь то же самое, но не удаляем из групп
    # TODO: перенести в тесты про Splunk
    from idm.core.tasks import DisableAccount

    fake_newldap.search_s = MagicMock()
    fake_newldap.search_s.return_value = [('CN=Frodo Baggins,CN=Users,DC=Ruler,DC=tst', {
        'cn': ['Frodo Baggins'],
        'userAccountControl': [1],
    })]

    with patch.object(http, 'post') as mock_post:
        DisableAccount.delay(
            username='frodo',
            reason='Увольнение сотрудника по данным из staff',
            move_to_old=False,
        )
        mock_post.assert_called_with(
            settings.PASSPORT_CLOSE_SESSIONS_URL,
            json={'username': 'frodo'},
            tvm_id=settings.PASSPORT_CLOSE_SESSIONS_TVM_ID,
        )

    user = dismissed_user_for_block
    user.refresh_from_db()
    assert user.ldap_blocked is True
    assert user.ldap_active is True
    assert Action.objects.count() == 1

    action = Action.objects.get()
    assert action.action == 'user_ad_disable'
    assert action.user_id == user.id
    assert action.data == {
        'reason': 'Увольнение сотрудника по данным из staff',
    }
    assert fake_newldap.rename_s.call_args_list == []
    fake_newldap.modify_s.assert_called_with('CN=Frodo Baggins,CN=Users,DC=Ruler,DC=tst',
                                             [(2, 'userAccountControl', b'3')])


@pytest.mark.parametrize('mode', ('fast', 'full'))
def test_idm_deprive_roles_threshold_exceed(arda_users, mode, fake_newldap, settings):
    fake_newldap.search_s = MagicMock()
    fake_newldap.search_s.return_value = [('CN=Frodo Baggins,CN=Users,DC=Ruler,DC=tst', {
        'cn': ['Frodo Baggins'],
        'userAccountControl': [1],
    })]

    threshold = User.objects.users().count() - 1
    User.objects.update(is_active=False, ldap_active=True)

    # незаблокированные в ldap
    with pytest.raises(CommandError, match='^Dismissed users threshold violation$'):
        # mock newhire
        with capture_requests() as mocked:
            mocked.http_get.return_value = create_fake_response([])
            management.call_command('idm_block_ad_users', threshold=threshold, mode=mode)

    # Блокировка не была произведена
    # Также в тесте не был перехвачен ldap, он бы упал, если бы действительно работал
    assert User.objects.filter(ldap_active=True).count() == User.objects.count()
    assert len(mail.outbox) == 0
    assert FiredUsersLimitExceededMetric.get() == threshold + 1


@pytest.mark.parametrize('mode', ('fast', 'full'))
def test_idm_deprive_roles_threshold_less(arda_users, fake_newldap, mode, settings):
    def search_s(ou, modifier, query):
        username = re.match(r'\(SAMAccountName=(?P<username>[^)]+)\)$', query).group('username')
        return [('CN=%s,CN=Users,DC=Ruler,DC=tst' % username, {
            'cn': ['Frodo Baggins'],
            'userAccountControl': [1],
        })]

    def modify_s(value, diff):
        if 'frodo' in value or 'bilbo' in value:
            raise Exception('Cannot block %s' % value)

    fake_newldap.search_s.side_effect = search_s
    fake_newldap.modify_s.side_effect = modify_s

    threshold = User.objects.users().count() + 1
    User.objects.update(is_active=False, ldap_active=True)

    # mock newhire
    with capture_requests() as mocked:
        with mock_ids_repo('startrek2') as st_repo:
            st_repo.create.return_value = MockedTrackerIssue('ISSUE-123')
            mocked.http_get.return_value = create_fake_response([])
            management.call_command('idm_block_ad_users', threshold=threshold, mode=mode)
    if mode == 'full':
        st_repo.create.assert_called_once()
    else:
        st_repo.create.assert_not_called()
    unblocked_users = User.objects.users().filter(ldap_active=True)
    assert unblocked_users.count() == 2
    for user in unblocked_users:
        assert user.ldap_blocked_timestamp is None


def test_newhire_failure(dismissed_user):
    """Неответ Наниматора – инцидент"""

    with capture_requests() as mocked:
        mocked.http_get.side_effect = http.RequestException()
        management.call_command('idm_block_ad_users', mode='full')
    assert len(mail.outbox) == 2
    mail1, mail2 = mail.outbox
    assert mail1.subject == mail2.subject == 'Сверка уволенных пользователей не удалась из-за неответа Наниматора'
    assert_contains(
        ['Сверка не удалась из-за неответа Наниматора'],
        mail1.body
    )


@pytest.mark.parametrize('mode', ('fast', 'full'))
@pytest.mark.parametrize('exceed_threshold', (True, False))
@pytest.mark.parametrize('block', (True, False))
def test_threshold_setting(arda_users, fake_newldap, settings, mode, exceed_threshold, block):
    fake_newldap.search_s.return_value = [('CN=Frodo Baggins,CN=Users,DC=Ruler,DC=tst', {
        'cn': ['Frodo Baggins'],
        'userAccountControl': [1],
    })]

    count = User.objects.users().count()
    if exceed_threshold:
        settings.IDM_DISMISSED_USERS_THRESHOLD = count
    else:
        settings.IDM_DISMISSED_USERS_THRESHOLD = count + 1
    User.objects.update(is_active=False, ldap_active=True)

    # mock newhire
    with capture_requests() as mocked:
        mocked.http_get.return_value = create_fake_response([])
        if exceed_threshold:
            with pytest.raises(CommandError):
                management.call_command('idm_block_ad_users', mode=mode, block=block)
        else:
            management.call_command('idm_block_ad_users', mode=mode, block=block)

    if exceed_threshold:
        # предохранитель сработал
        assert User.objects.filter(ldap_active=False).count() == 0
    else:
        assert User.objects.users().filter(ldap_active=True).count() == 0


@pytest.mark.parametrize('modify_failed', [True, False])
@pytest.mark.parametrize('is_blocked', [True, False])
@pytest.mark.parametrize('is_old', [True, False])
@pytest.mark.parametrize('has_no_groups', [True, False])
def test_block_partially_blocked(dismissed_user, simple_system, fake_newldap, is_old,
                                 has_no_groups, is_blocked, modify_failed):
    """Проверим различные комбинации блокировки пользователя и что мы его доблокируем,
    если что-то из признаков остаётся"""

    if is_old:
        CN = 'Old Users'
    else:
        CN = 'Users'

    class ADState(object):
        def __init__(self):
            self.removed_from_groups = False

        def search_in_ad(self, org_unit, *args, **kwargs):
            result = None
            if is_old and org_unit.startswith('OU=OLD Users') or not is_old and org_unit.startswith('CN=Users'):
                result = [(
                    'CN=Frodo Baggins,CN=%s,DC=Ruler,DC=tst' % CN, {
                        'cn': ['Frodo Baggins'],
                        'userAccountControl': [
                            0b11 if is_blocked  # inactive, second bit is 1
                            else 0b01  # active, second bit is 0
                        ],
                        'memberOf': [] if has_no_groups or self.removed_from_groups else ['SomeGroup'],
                    }
                )]
            return result

        def modify_s(self, args, kwargs):
            # Удаление из группы
            if args == 'SomeGroup':
                if modify_failed:
                    raise Exception
                self.removed_from_groups = True

    ad_state = ADState()
    fake_newldap.modify_s.side_effect = ad_state.modify_s
    fake_newldap.search_s.side_effect = ad_state.search_in_ad
    with patch.object(requests.sessions.Session, 'request') as get:
        get.return_value = create_fake_response([])
        with mock_ids_repo('startrek2') as st_repo:
            st_repo.create.return_value = MockedTrackerIssue('ISSUE-123')
            management.call_command('idm_block_ad_users', mode='full')

    assert st_repo.create.called == (not has_no_groups and modify_failed)
    if not has_no_groups and modify_failed:
        args, kwargs = st_repo.create.call_args
        assert kwargs['queue'] == settings.IDM_TRACKER_REMOVE_FROM_AD_GROUPS_QUEUE
        assert kwargs['summary'] == 'Не удалось удалить пользователей из групп в AD'
        assert dismissed_user.username in kwargs['description']

    dismissed_user.refresh_from_db()

    if is_blocked and is_old:
        fake_newldap.disable_user.assert_not_called()
    else:
        assert dismissed_user.ldap_blocked_timestamp is not None

    actions_count = 0
    if not is_old:
        actions_count += 1
    if not is_blocked:
        actions_count += 1

    assert dismissed_user.actions.count() == actions_count

    if not is_blocked:
        disable_action = dismissed_user.actions.get(action='user_ad_disable')
        assert disable_action.data == {
            'reason': 'Повторная блокировка пользователя, которого уже блокировали ранее.'
        }
    if not is_old:
        move_to_old_action = dismissed_user.actions.get(action='user_ad_move_to_old')
        assert move_to_old_action.data == {
            'reason': 'Повторная блокировка пользователя, которого уже блокировали ранее.',
            'original_ou': 'CN=Users,DC=ld,DC=yandex,DC=ru',
        }

    modify_calls = []
    if not is_blocked:
        modify_calls.append(call('CN=Frodo Baggins,CN=%s,DC=Ruler,DC=tst' % CN, [
            (2, 'userAccountControl', b'3')
        ]))
    if not has_no_groups:
        modify_calls.append(call('SomeGroup', [
            (1, 'member', force_bytes('CN=Frodo Baggins,CN=%s,DC=Ruler,DC=tst' % CN))
        ]))
    assert fake_newldap.modify_s.call_args_list == modify_calls
    if is_old:
        assert fake_newldap.rename_s.call_args_list == []
    else:
        # перемещение в Old Users
        assert fake_newldap.rename_s.call_args_list == [
            call('CN=Frodo Baggins,CN=Users,DC=Ruler,DC=tst', 'CN=Frodo Baggins', 'OU=OLD Users,DC=ld,DC=yandex,DC=ru')
        ]
