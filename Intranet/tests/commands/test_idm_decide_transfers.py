# -*- coding: utf-8 -*-


import datetime
import pytest
from django.core import mail
from django.core.management import call_command
from django.utils import timezone
from freezegun import freeze_time

from idm.core.models import Role, Transfer, Action
from idm.tests.utils import (
    set_workflow, DEFAULT_WORKFLOW, add_members, remove_members, clear_mailbox, move_group, capture_requests,
    create_fake_response
)

pytestmark = [pytest.mark.django_db]


@pytest.mark.parametrize('changing_duties', [True, False])
def test_user_transfers(simple_system, arda_users, department_structure, changing_duties):
    """Проверим, что пользовательские перемещения пересматриваются всегда"""

    varda = arda_users.varda
    fellowship = department_structure.fellowship
    valinor = department_structure.valinor

    set_workflow(simple_system, group_code=DEFAULT_WORKFLOW)
    varda_role = Role.objects.request_role(varda, varda, simple_system, '', {'role': 'admin'})
    clear_mailbox()

    add_members(fellowship, [varda])
    remove_members(valinor, [varda])

    assert Transfer.objects.count() == 1
    transfer = Transfer.objects.get()
    assert transfer.user_id == varda.id
    assert transfer.state == 'undecided'

    with capture_requests() as mocked:
        mocked.http_get.return_value = create_fake_response({
            'proposals': [{'applied_at': '2020-04-29T18:33:23.001', 'new_department': 'x'}] if changing_duties else [],
            'rotations': [],
        })
        call_command('idm_decide_transfers')

    transfer.refresh_from_db()
    varda_role.refresh_from_db()

    if changing_duties:
        assert transfer.state == 'auto_accepted'
        assert varda_role.state == 'need_request'
    else:
        assert transfer.state == 'auto_rejected'
        assert varda_role.state == 'granted'

    assert len(mail.outbox) == 0


@pytest.mark.parametrize('changing_duties', [True, False, None])
def test_group_transfers(simple_system, arda_users, department_structure, changing_duties):
    """Проверим, что групповые перемещения пересматриваются тогда, когда нам об этом явно сказал Стафф"""

    fellowship = department_structure.fellowship
    valinor = department_structure.valinor
    frodo = arda_users.frodo

    set_workflow(simple_system, group_code=DEFAULT_WORKFLOW)
    role = Role.objects.request_role(frodo, fellowship, simple_system, '', {'role': 'admin'}, None)
    move_group(fellowship, valinor.parent)
    assert Transfer.objects.count() == fellowship.members.count() + 1
    assert Transfer.objects.filter(type='user').count() == 0
    assert Transfer.objects.filter(type='group').count() == 1
    group_transfer = Transfer.objects.get(type='group')

    usergroup_transfers = Transfer.objects.filter(type='user_group')
    assert usergroup_transfers.count() == fellowship.members.count()
    assert Transfer.objects.filter(state='undecided').count() == Transfer.objects.count()

    with capture_requests() as mocked:
        mocked.http_get.return_value = create_fake_response({
            'result': [{
                'proposal_id': 100,
                'applied_at': '2017-12-30 10:00:00',
                'changing_duties': changing_duties,
            }]
        })
        call_command('idm_decide_transfers')

    group_transfer.refresh_from_db()
    expected_dict = {
        True: 'auto_accepted',
        False: 'auto_rejected',
        None: 'undecided',
    }
    expected = expected_dict[changing_duties]
    assert group_transfer.state == expected
    assert usergroup_transfers.filter(state=expected).count() == usergroup_transfers.count()
    sync_action = Action.objects.filter(action='transfers_decided').get()
    expected = {
        'status': 0,
        'report': {
            'group_transfers_accept': 0,
            'group_transfers_errors': 0,
            'group_transfers_reject': 0,
            'group_transfers_success': 1,
            'user_transfers_accept': 0,
            'user_transfers_errors': 0,
            'user_transfers_reject': 0,
            'user_transfers_success': 0,
        }
    }
    if changing_duties is True:
        expected['report']['group_transfers_accept'] = 1
        action = role.actions.get(action='ask_rerequest')
        assert action.comment == (
            'Необходимо перезапросить роль в связи с перемещением группы из '
            '"/Средиземье/Объединения/Братство кольца/" в "/Братство кольца/"'
        )
    elif changing_duties is False:
        expected['report']['group_transfers_reject'] = 1
        action = role.actions.get(action='keep_granted')
        assert action.comment == (
            'Роль не перезапрошена, так как по данным заявки из Стаффа пересмотр не требуется. '
            'Группа была перемещена из "/Средиземье/Объединения/Братство кольца/" в "/Братство кольца/"'
        )
    else:
        expected['report']['group_transfers_success'] = 0
    assert sync_action.data == expected
    if changing_duties is not None:
        decision_action = {
            True: 'transfer_accepted',
            False: 'transfer_rejected',
        }[changing_duties]
        transfer_action = Action.objects.get(action=decision_action, transfer__parent=None)
        expected = {
            'decisions': [
                {'proposal_id': 100, 'applied_at': '2017-12-30 10:00:00', 'changing_duties': changing_duties}
            ]
        }
        assert transfer_action.data == expected


def test_multiple_group_transfers(simple_system, arda_users, department_structure):
    """Если в период между двумя запусками таски произошло несколько тперемещений группы,
    то мы применяем последнее, а пересмотр ролей выполняется если указан хотя бы для одного."""

    fellowship = department_structure.fellowship
    valinor = department_structure.valinor
    shire = department_structure.shire
    frodo = arda_users.frodo
    sauron = arda_users.sauron

    set_workflow(simple_system, group_code=DEFAULT_WORKFLOW)
    role = Role.objects.request_role(frodo, fellowship, simple_system, '', {'role': 'admin'}, None)
    move_group(fellowship, shire.parent)
    fellowship.memberships.create(user=sauron, is_direct=True, state='active')
    move_group(fellowship, valinor.parent)
    # Два трансфера, в каждом все пользовательские и один для группы. В первом нет Саурона
    assert Transfer.objects.count() == 2 * (fellowship.members.count() + 1) - 1
    assert Transfer.objects.filter(type='user').count() == 0
    assert Transfer.objects.filter(type='group').count() == 2

    usergroup_transfers = Transfer.objects.filter(type='user_group')
    assert usergroup_transfers.count() == fellowship.members.count() * 2 - 1
    assert Transfer.objects.filter(state='undecided').count() == Transfer.objects.count()

    with capture_requests() as mocked:
        mocked.http_get.return_value = create_fake_response({
            'result': [
                {
                     'proposal_id': 100,
                     'applied_at': '2017-12-30 10:00:00',
                     'changing_duties': True,
                },
                {
                    'proposal_id': 101,
                    'applied_at': '2017-12-30 11:00:00',
                    'changing_duties': False,
                }
            ]
        })
        call_command('idm_decide_transfers')

    expired_transfer = Transfer.objects.filter(type='group').first()
    group_transfer = Transfer.objects.filter(type='group').last()
    assert expired_transfer.target_path == group_transfer.source_path
    assert group_transfer.target_path == '/fellowship-of-the-ring/'
    assert group_transfer.state == 'auto_accepted'
    assert expired_transfer.state == 'expired'
    assert usergroup_transfers.filter(state='auto_accepted').count() == fellowship.memberships.count()
    assert usergroup_transfers.filter(state='expired').count() == fellowship.memberships.count() - 1
    sync_action = Action.objects.filter(action='transfers_decided').get()
    expected = {
        'status': 0,
        'report': {
            'group_transfers_errors': 1,
            'group_transfers_reject': 0,
            'group_transfers_success': 1,
            'user_transfers_reject': 0,
            'user_transfers_accept': 0,
            'user_transfers_errors': 0,
            'user_transfers_success': 0,
            'group_transfers_accept': 1,
        }
    }
    assert sync_action.data == expected
    transfer_action = Action.objects.get(action='transfer_accepted', transfer__parent=None)
    expected = {
        'decisions': [
            {'proposal_id': 100, 'applied_at': '2017-12-30 10:00:00', 'changing_duties': True},
            {'proposal_id': 101, 'applied_at': '2017-12-30 11:00:00', 'changing_duties': False},
        ]
    }
    assert transfer_action.data == expected


@pytest.mark.parametrize('changing_duties', [True, False, None])
def test_group_transfers_for_subtree(simple_system, arda_users, department_structure, changing_duties):
    """Проверим случай, когда перемещается сразу ветка дерева. Ручка Стаффа отдаёт только те группы,
    у которых поменялся parent. А пересмотреть перемещения нам нужно для всех групп, даже для тех, которые
    были перемещены вместе с родителем, без смены parent-a."""

    associations = department_structure.associations
    valinor = department_structure.valinor

    move_group(associations, valinor)

    assert set(Transfer.objects.values_list('type', flat=True).distinct()) == {'group', 'user_group'}
    group_transfers = Transfer.objects.group()
    user_group_transfers = Transfer.objects.user_group()
    assert group_transfers.count() == 2
    # assert user_group_transfers.count() == associations.get_descendant_members().count()

    assert Transfer.objects.filter(state='undecided').count() == Transfer.objects.count()
    association_transfer = group_transfers.get(group=associations)
    fellowship_transfer = group_transfers.get(group=department_structure.fellowship)

    def moved_only_parent_group(url, params, **kwargs):
        if url.endswith('associations'):
            result = create_fake_response({
                'result': [{
                    'proposal_id': 100,
                    'applied_at': '2017-12-30 10:00:00',
                    'changing_duties': changing_duties,
                }]
            })
        else:
            result = create_fake_response({
                'result': []
            })
        return result

    with capture_requests() as mocked:
        mocked.http_get.side_effect = moved_only_parent_group
        call_command('idm_decide_transfers')

    fellowship_transfer.refresh_from_db()
    association_transfer.refresh_from_db()
    expected_dict = {
        True: 'auto_accepted',
        False: 'auto_rejected',
        None: 'undecided',
    }
    expected = expected_dict[changing_duties]
    assert association_transfer.state == expected
    assert fellowship_transfer.state == expected
    if changing_duties is not None:
        assert fellowship_transfer.parent_id == association_transfer.id
    assert user_group_transfers.filter(state=expected).count() == user_group_transfers.count()


def test_parameters(simple_system, arda_users, department_structure):
    """Проверим работу параметров команды idm_decide_transfers, когда они заданы"""

    fellowship = department_structure.fellowship
    valinor = department_structure.valinor

    move_group(fellowship, valinor.parent)

    with capture_requests() as mocked:
        from_date = datetime.datetime(year=2017, month=12, day=12, hour=11, tzinfo=timezone.utc)
        to_date = datetime.datetime(year=2017, month=12, day=13, hour=10, tzinfo=timezone.utc)

        call_command('idm_decide_transfers', from_date=from_date, to_date=to_date)

    assert len(mocked.http_get.call_args_list) == 1
    call = mocked.http_get.call_args_list[0]
    expected = {
        'from': '2017-12-12T11:00:00',
        'to': '2017-12-13T10:00:00',
    }
    assert call[1]['params'] == expected


def test_default_parameters(simple_system, arda_users, department_structure, settings):
    """Проверим работу параметров команды idm_decide_transfers, когда они заданы"""

    fellowship = department_structure.fellowship
    valinor = department_structure.valinor

    move_group(fellowship, valinor.parent)
    now = timezone.now()
    future_1m = now + datetime.timedelta(seconds=60)

    with capture_requests() as mocked:
        mocked.http_get.return_value = create_fake_response({
            'result': [{
                'proposal_id': 100,
                'applied_at': '2017-12-30 10:00:00',
                'changing_duties': True,
            }]
        })
        with freeze_time(now):
            call_command('idm_decide_transfers')

    assert len(mocked.http_get.call_args_list) == 1
    call = mocked.http_get.call_args_list[0]
    expected = {
        'from': settings.STAFF_TRANSFERS_INITIAL_DATE,
        'to': now.strftime('%Y-%m-%dT%H:%M:%S'),
    }
    assert call[1]['params'] == expected

    move_group(fellowship, valinor)

    # во второй раз есть уже экшен, поэтому мы не от даты релиза смотрим, а от даты последнего action-а
    assert Action.objects.filter(action='transfers_decided').count() == 1
    with capture_requests() as mocked:
        mocked.http_get.return_value = create_fake_response({
            'result': [{
                'proposal_id': 100,
                'applied_at': '2017-12-30 10:00:00',
                'changing_duties': True,
            }]
        })
        with freeze_time(future_1m):
            call_command('idm_decide_transfers')

    assert len(mocked.http_get.call_args_list) == 1
    call = mocked.http_get.call_args_list[0]
    expected = {
        'from': now.strftime('%Y-%m-%dT%H:%M:%S'),
        'to': future_1m.strftime('%Y-%m-%dT%H:%M:%S'),
    }
    assert call[1]['params'] == expected
