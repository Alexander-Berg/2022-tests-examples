# coding: utf-8

import pytest
from django.conf import settings
from django.core.management import call_command
from django.db.models import Q
from django.test.utils import override_settings
from django.utils import timezone
from freezegun import freeze_time
from waffle.testutils import override_switch

from idm.core import depriving
from idm.core.constants.action import ACTION, ACTION_DATA_KEYS, ACTION_STATE
from idm.core.constants.role import ROLE_STATE
from idm.core.models import Action, Role
from idm.tests.utils import add_perms_by_role, raw_make_role, remove_members, set_workflow

pytestmark = pytest.mark.django_db


def test_get_roles_data(simple_system, complex_system, arda_users):
    # Подготовим данные для теста
    frodo = arda_users.frodo
    sam = arda_users.sam
    bilbo = arda_users.bilbo

    depriving_at = timezone.now() - timezone.timedelta(minutes=settings.IDM_DEPRIVING_AFTER_MIN + 1)

    begin_action = Action.objects.create(
        action='test_action',
        data={ACTION_DATA_KEYS.ACTION_STATE: ACTION_STATE.BEGIN},
    )
    completed_action = Action.objects.create(
        action='test_action',
        data={ACTION_DATA_KEYS.ACTION_STATE: ACTION_STATE.COMPLETED},
    )

    frodo_simple_role = raw_make_role(
        frodo,
        simple_system,
        {'role': 'admin'},
        state=ROLE_STATE.DEPRIVING_VALIDATION,
        depriving_at=depriving_at,
        depriver=bilbo,
    )
    frodo_complex_role = raw_make_role(
        frodo,
        complex_system,
        {'project': 'rules', 'role': 'admin'},
        state=ROLE_STATE.DEPRIVING_VALIDATION,
        depriving_at=depriving_at,
        depriver=bilbo,
    )

    sam_simple_role = raw_make_role(
        sam,
        simple_system,
        {'role': 'admin'},
        state=ROLE_STATE.DEPRIVING_VALIDATION,
        depriving_at=depriving_at,
        depriver=bilbo,
    )
    sam_complex_role = raw_make_role(
        sam,
        complex_system,
        {'project': 'rules', 'role': 'admin'},
        state=ROLE_STATE.DEPRIVING_VALIDATION,
        parent=sam_simple_role,
        depriving_at=depriving_at,
        depriver=bilbo,
    )

    # В выводе отсутствует, родительское событие ещё не завершилось
    Action.objects.create(action=ACTION.DEPRIVE, role=frodo_simple_role, parent=begin_action, requester=bilbo)
    # В выводе будет, родительское событие завершилось, но если нет фильтра по системе
    a2 = Action.objects.create(action=ACTION.DEPRIVE, role=frodo_complex_role, parent=completed_action, requester=bilbo)
    # В выводе будет, родительское событие отсутсвует
    a3 = Action.objects.create(action=ACTION.DEPRIVE, role=sam_simple_role, requester=bilbo)
    # В выводе отсутствует, роль выдана по другой роли
    Action.objects.create(action=ACTION.DEPRIVE, role=sam_complex_role, requester=bilbo)

    # Код теста
    expected_data = sorted(
        Action.objects.filter(pk__in=[a2.pk, a3.pk]).values('role', 'role__depriver', 'role__user', 'role__group'),
        key=lambda x: x['role'],
    )
    data = sorted(depriving.get_roles_data(), key=lambda x: x['role'])
    assert data == expected_data

    # Проверим фильтрацию по системе
    expected_data = list(Action.objects.filter(pk=a3.pk).values('role', 'role__depriver', 'role__user', 'role__group'))
    data = list(depriving.get_roles_data(system=simple_system))
    assert data == expected_data


def test_group_roles_by_depriver(simple_system, arda_users, department_structure):
    # Подготовим данные для теста
    frodo = arda_users.frodo
    sam = arda_users.sam
    bilbo = arda_users.bilbo
    fellowship = department_structure.fellowship

    depriving_at = timezone.now() - timezone.timedelta(minutes=settings.IDM_DEPRIVING_AFTER_MIN + 1)

    frodo_role = raw_make_role(
        frodo,
        simple_system,
        {'role': 'admin'},
        state=ROLE_STATE.DEPRIVING_VALIDATION,
        depriving_at=depriving_at,
        depriver=bilbo,
    )
    sam_role = raw_make_role(
        sam,
        simple_system,
        {'role': 'admin'},
        state=ROLE_STATE.DEPRIVING_VALIDATION,
        depriving_at=depriving_at,
        depriver=bilbo,
    )
    fellowship_role = raw_make_role(
        fellowship,
        simple_system,
        {'role': 'admin'},
        state=ROLE_STATE.DEPRIVING_VALIDATION,
        depriving_at=depriving_at,
        depriver=bilbo,
    )

    Action.objects.create(action=ACTION.DEPRIVE, role=frodo_role, requester=bilbo)
    Action.objects.create(action=ACTION.DEPRIVE, role=sam_role, requester=bilbo)
    Action.objects.create(action=ACTION.DEPRIVE, role=fellowship_role, requester=bilbo)

    # Код теста
    data = depriving.group_role_data_by_depriver()
    assert list(data.keys()) == [bilbo.pk]
    assert data[bilbo.pk]['subjects'] == {f'user_{frodo.pk}', f'user_{sam.pk}', f'group_{fellowship.pk}'}
    assert data[bilbo.pk]['roles'] == {frodo_role.pk, sam_role.pk, fellowship_role.pk}


def test_get_roles_ids_to_deprive(simple_system, arda_users, department_structure):
    """
    TestpalmID: 3456788-100
    """
    # Подготовим данные для теста
    frodo = arda_users.frodo
    sam = arda_users.sam
    bilbo = arda_users.bilbo
    fellowship = department_structure.fellowship

    depriving_at = timezone.now() - timezone.timedelta(minutes=settings.IDM_DEPRIVING_AFTER_MIN + 1)

    frodo_role = raw_make_role(
        frodo,
        simple_system,
        {'role': 'admin'},
        state=ROLE_STATE.DEPRIVING_VALIDATION,
        depriving_at=depriving_at,
        depriver=bilbo,
    )
    sam_role = raw_make_role(
        sam,
        simple_system,
        {'role': 'admin'},
        state=ROLE_STATE.DEPRIVING_VALIDATION,
        depriving_at=depriving_at,
        depriver=bilbo,
    )
    fellowship_role = raw_make_role(
        fellowship,
        simple_system,
        {'role': 'admin'},
        state=ROLE_STATE.DEPRIVING_VALIDATION,
        depriving_at=depriving_at,
        depriver=frodo,
    )
    frodo_role_with_parent = raw_make_role(
        frodo,
        simple_system,
        {'role': 'admin'},
        state=ROLE_STATE.DEPRIVING_VALIDATION,
        depriving_at=depriving_at,
        parent=fellowship_role,
        depriver=frodo,
    )
    sam_role_with_parent = raw_make_role(
        sam,
        simple_system,
        {'role': 'admin'},
        state=ROLE_STATE.DEPRIVING_VALIDATION,
        depriving_at=depriving_at,
        parent=fellowship_role,
        depriver=frodo,
    )

    Action.objects.create(action=ACTION.DEPRIVE, role=frodo_role, requester=bilbo)
    Action.objects.create(action=ACTION.DEPRIVE, role=sam_role, requester=bilbo)
    Action.objects.create(action=ACTION.DEPRIVE, role=fellowship_role, requester=frodo)
    Action.objects.create(action=ACTION.DEPRIVE, role=frodo_role_with_parent, requester=frodo)
    Action.objects.create(action=ACTION.DEPRIVE, role=sam_role_with_parent, requester=frodo)

    def old_get_roles_ids_to_deprive_adapter(force: bool = False):
        data_dict = depriving.get_roles_ids_to_deprive(force=force)
        data = set()
        for key, value in data_dict.items():
            data.update(value)
        data = sorted(data)
        return data

    # Проверим, что при передаче force игнорируется превышение treshold
    with override_settings(IDM_SUBJECT_DEPRIVING_THRESHOLD=1):
        data = old_get_roles_ids_to_deprive_adapter(force=True)
    expected_data = sorted([frodo_role.pk, sam_role.pk, fellowship_role.pk])
    assert data == expected_data

    # Проверим, что при отсутсвии превышения threshold получим все нужные данные
    data = old_get_roles_ids_to_deprive_adapter(force=True)
    assert data == expected_data


def test_idm_deprive_depriving_roles_expired_role(simple_system, arda_users):
    set_workflow(simple_system, 'approvers = []')
    frodo = arda_users.frodo

    role = Role.objects.request_role(
        requester=frodo,
        subject=frodo,
        system=simple_system,
        comment='',
        data={'role': 'admin'},
    )
    role.actions.create(action=ACTION.EXPIRE)
    role.state = ROLE_STATE.DEPRIVING_VALIDATION
    role.depriving_at = timezone.now() - timezone.timedelta(minutes=20)
    role.save(update_fields=['state', 'depriving_at'])
    with override_switch('idm.deprive_not_immediately', active=True):
        call_command('idm_deprive_depriving_roles')

    role.refresh_from_db()
    assert role.state == 'deprived'


@pytest.mark.parametrize('action', ['break', 'turn_off'])
def test_idm_deprive_depriving_roles_broken_system(simple_system, arda_users, action):
    set_workflow(simple_system, 'approvers = []')
    frodo = arda_users.frodo

    role = Role.objects.request_role(
        requester=frodo,
        subject=frodo,
        system=simple_system,
        comment='',
        data={'role': 'admin'},
    )
    role.actions.create(action=ACTION.EXPIRE)
    role.state = ROLE_STATE.DEPRIVING_VALIDATION
    role.depriving_at = timezone.now() - timezone.timedelta(minutes=20)
    role.save(update_fields=['state', 'depriving_at'])

    if action == 'break':
        simple_system.is_broken = True
        simple_system.save(update_fields=['is_broken'])
    elif action == 'turn_off':
        simple_system.is_active = False
        simple_system.save(update_fields=['is_active'])

    with override_switch('idm.deprive_not_immediately', active=True):
        call_command('idm_deprive_depriving_roles')

    role.refresh_from_db()
    assert role.state == ROLE_STATE.DEPRIVING_VALIDATION

    simple_system.is_broken = False
    simple_system.is_active = True
    simple_system.save()

    with override_switch('idm.deprive_not_immediately', active=True):
        call_command('idm_deprive_depriving_roles')

    role.refresh_from_db()
    assert role.state == 'deprived'


def test_idm_deprive_roles_regular(simple_system, arda_users, department_structure):
    set_workflow(simple_system, group_code='approvers = []')
    frodo = arda_users.frodo
    add_perms_by_role('superuser', frodo)

    now = timezone.now()

    for user in arda_users.values():
        if user.department_group is None:
            continue
        Role.objects.request_role(
            requester=user,
            subject=user,
            system=simple_system,
            comment='',
            data={'role': 'admin'},
        )
    parent_action = Action.objects.create(action='test_action')
    fellowship = department_structure.fellowship

    group_role = Role.objects.request_role(
        requester=frodo,
        subject=fellowship,
        system=simple_system,
        comment='',
        data={'role': 'admin'},
    )
    ref_roles_count = group_role.refs.count()

    with override_switch('idm.deprive_not_immediately', active=True):
        # Проверим, что сразу отзывается роль, только в том случае, если отзывающий - владелец роли
        # Иначе роли переходят в статус depriving
        (
            Role.objects
            .filter(parent__isnull=True)
            .select_related('user', 'user__department_group')
        ).deprive_or_decline(depriver=frodo, parent_action=parent_action)
        depriving_count = Role.objects.filter(state=ROLE_STATE.DEPRIVING_VALIDATION).count()
        assert depriving_count == Role.objects.count() - 1 - ref_roles_count  # Роль frodo отозвалась
        deprived_role = Role.objects.get(state='deprived')
        assert deprived_role.user_id == frodo.id
        assert 'Роль будет отозвана через 15 минут' not in deprived_role.actions.get(action='deprive').data.get('comment', '')

        depriving_role = Role.objects.filter(state=ROLE_STATE.DEPRIVING_VALIDATION).first()
        assert 'Роль будет отозвана через 15 минут' in depriving_role.actions.get(action='deprive').data['comment']

        # Проверим, что если parent_action в статусе `begin`, то роли не отзываются
        parent_action.data[ACTION_DATA_KEYS.ACTION_STATE] = ACTION_STATE.BEGIN
        parent_action.save()
        with freeze_time(now + timezone.timedelta(minutes=settings.IDM_DEPRIVING_AFTER_MIN - 1)):
            call_command('idm_deprive_depriving_roles')
        assert Role.objects.filter(state=ROLE_STATE.DEPRIVING_VALIDATION).count() == depriving_count

        parent_action.data[ACTION_DATA_KEYS.ACTION_STATE] = ACTION_STATE.COMPLETED
        parent_action.save()

        # Проверим, что если прошло меньше IDM_DEPRIVING_AFTER_MIN минут, то роли не отзываются
        with freeze_time(now + timezone.timedelta(minutes=settings.IDM_DEPRIVING_AFTER_MIN - 1)):
            call_command('idm_deprive_depriving_roles')
        assert Role.objects.filter(state=ROLE_STATE.DEPRIVING_VALIDATION).count() == depriving_count

        # Проверим, что при превышении лимита на отзыв, роли не отзываются
        with override_settings(IDM_SUBJECT_DEPRIVING_THRESHOLD=depriving_count-1):
            call_command('idm_deprive_depriving_roles')
        assert Role.objects.filter(state=ROLE_STATE.DEPRIVING_VALIDATION).count() == depriving_count

        # Проверим, что при отсутствии превышения лимита на отзыв и прошествии IDM_DEPRIVING_AFTER_MIN времени
        # роли отзываются
        with freeze_time(now + timezone.timedelta(minutes=20)):
            call_command('idm_deprive_depriving_roles')

        assert Role.objects.filter(state=ROLE_STATE.DEPRIVING_VALIDATION).count() == 0
        assert Role.objects.filter(state='deprived').count() == Role.objects.count()


@override_switch('idm.deprive_not_immediately', active=True)
def test_deprive_personal_roles_if_user_inactive(simple_system, arda_users, department_structure):
    set_workflow(simple_system, group_code='approvers = []')
    frodo = arda_users.frodo
    fellowship = department_structure.fellowship

    group_role = Role.objects.request_role(
        requester=frodo,
        subject=fellowship,
        system=simple_system,
        comment='',
        data={'role': 'admin'},
    )

    frodo_role = frodo.roles.get()
    assert frodo_role.state == ROLE_STATE.GRANTED
    frodo.is_active = False
    frodo.save()
    remove_members(fellowship, [frodo])
    frodo_role.refresh_from_db()
    assert frodo_role.state == ROLE_STATE.DEPRIVED


@override_switch('idm.deprive_not_immediately', active=True)
def test_deprive_personal_ref_roles_if_parent_active(simple_system, arda_users):
    frodo = arda_users.frodo
    bilbo = arda_users.bilbo

    depriving_at = timezone.now() - timezone.timedelta(minutes=settings.IDM_DEPRIVING_AFTER_MIN + 1)

    parent_role = raw_make_role(
        frodo,
        simple_system,
        {'role': 'admin'},
        state=ROLE_STATE.GRANTED,
    )
    ref_role = raw_make_role(
        frodo,
        simple_system,
        {'role': 'manager'},
        state=ROLE_STATE.DEPRIVING_VALIDATION,
        parent=parent_role,
        depriving_at=depriving_at,
    )

    Action.objects.create(action=ACTION.DEPRIVE, role=ref_role, requester=bilbo)
    call_command('idm_deprive_depriving_roles')
    parent_role.refresh_from_db()
    assert parent_role.state == ROLE_STATE.GRANTED
    ref_role.refresh_from_db()
    assert ref_role.state == ROLE_STATE.DEPRIVED


@override_switch('idm.deprive_not_immediately', active=True)
def test_deprive_personal_roles_if_group_active(simple_system, arda_users, department_structure):
    frodo = arda_users.frodo
    bilbo = arda_users.bilbo
    fellowship = department_structure.fellowship

    depriving_at = timezone.now() - timezone.timedelta(minutes=settings.IDM_DEPRIVING_AFTER_MIN + 1)

    parent_role = raw_make_role(
        fellowship,
        simple_system,
        {'role': 'admin'},
        state=ROLE_STATE.GRANTED,
    )
    ref_role = raw_make_role(
        frodo,
        simple_system,
        {'role': 'manager'},
        state=ROLE_STATE.DEPRIVING_VALIDATION,
        parent=parent_role,
        depriving_at=depriving_at,
    )

    Action.objects.create(action=ACTION.DEPRIVE, role=ref_role, requester=bilbo)
    call_command('idm_deprive_depriving_roles')
    parent_role.refresh_from_db()
    assert parent_role.state == ROLE_STATE.GRANTED
    ref_role.refresh_from_db()
    assert ref_role.state == ROLE_STATE.DEPRIVED


@override_switch('idm.deprive_not_immediately', active=True)
def test_deprive_without_parent_action_state(simple_system, arda_users):
    role = raw_make_role(
        arda_users.frodo,
        simple_system,
        {'role': 'manager'},
        state=ROLE_STATE.DEPRIVING_VALIDATION,
        depriving_at=timezone.now(),
    )
    parent_action = Action.objects.create(action=ACTION.MASS_ACTION, data={'key': 'value'})
    role.actions.create(action=ACTION.DEPRIVE, parent=parent_action)

    call_command('idm_deprive_depriving_roles')
    role.refresh_from_db()
    assert role.state == ROLE_STATE.DEPRIVED


@override_switch('idm.deprive_not_immediately', active=True)
def test_deprive_group_ref_roles_if_parent_active(simple_system, arda_users, department_structure):
    bilbo = arda_users.bilbo
    fellowship = department_structure.fellowship

    depriving_at = timezone.now() - timezone.timedelta(minutes=settings.IDM_DEPRIVING_AFTER_MIN + 1)

    parent_role = raw_make_role(
        fellowship,
        simple_system,
        {'role': 'admin'},
        state=ROLE_STATE.GRANTED,
    )
    ref_role = raw_make_role(
        fellowship,
        simple_system,
        {'role': 'manager'},
        state=ROLE_STATE.DEPRIVING_VALIDATION,
        parent=parent_role,
        depriving_at=depriving_at,
    )

    Action.objects.create(action=ACTION.DEPRIVE, role=ref_role, requester=bilbo)
    call_command('idm_deprive_depriving_roles')
    parent_role.refresh_from_db()
    assert parent_role.state == ROLE_STATE.GRANTED
    ref_role.refresh_from_db()
    assert ref_role.state == ROLE_STATE.DEPRIVED


@override_switch('idm.deprive_not_immediately', active=True)
def test_deprive_group_ref_roles_not_immediately(simple_system, arda_users, department_structure, idm_robot):
    frodo = arda_users.frodo
    fellowship = department_structure.fellowship
    workflow = '''
approvers = []
if role == {'role': 'admin'}:
    ref_roles = [{'system': 'simple', 'role_data': {'role': 'manager'}}]
'''

    set_workflow(simple_system, group_code=workflow)

    parent_role = Role.objects.request_role(
        frodo,
        fellowship,
        simple_system,
        '',
        {'role': 'admin'},
    )
    ref_role = parent_role.refs.filter(group__isnull=False).get()

    assert ref_role.state == ROLE_STATE.GRANTED
    set_workflow(simple_system, group_code='approvers = []')
    parent_role.rerun_workflow(robot=idm_robot)

    parent_role.refresh_from_db()
    assert parent_role.state == ROLE_STATE.GRANTED
    ref_role.refresh_from_db()
    assert ref_role.state == ROLE_STATE.DEPRIVING_VALIDATION
    with freeze_time(timezone.now() + timezone.timedelta(minutes=settings.IDM_DEPRIVING_AFTER_MIN + 1)):
        call_command('idm_deprive_depriving_roles')
    ref_role.refresh_from_db()
    assert ref_role.state == ROLE_STATE.DEPRIVED


@override_switch('idm.deprive_not_immediately', active=True)
def test_command_create_mass_action(simple_system, arda_users, department_structure):
    bilbo = arda_users.bilbo
    fellowship = department_structure.fellowship

    depriving_at = timezone.now() - timezone.timedelta(minutes=settings.IDM_DEPRIVING_AFTER_MIN + 1)

    parent_role = raw_make_role(
        fellowship,
        simple_system,
        {'role': 'admin'},
        state=ROLE_STATE.GRANTED,
    )
    ref_role = raw_make_role(
        fellowship,
        simple_system,
        {'role': 'manager'},
        state=ROLE_STATE.DEPRIVING,
        parent=parent_role,
        depriving_at=depriving_at,
    )

    action_filter = (
            Q(action=ACTION.MASS_ACTION) &
            Q(data__contains={'name': 'deprive_depriving_roles'})
    )
    Action.objects.create(action=ACTION.DEPRIVE, role=ref_role, requester=bilbo)
    assert not Action.objects.filter(action_filter)
    call_command('idm_deprive_depriving_roles')
    assert Action.objects.filter(action_filter) is not None


@override_switch('idm.deprive_not_immediately', active=True)
def test_deprive_personal_roles_of_dismissed_users_immediately(simple_system, arda_users, department_structure):
    gandalf = arda_users.gandalf
    frodo = arda_users.frodo
    fellowship = department_structure.fellowship

    raw_make_role(fellowship, simple_system, {'role': 'admin'}, state=ROLE_STATE.GRANTED)
    call_command('idm_poke_hanging_roles', '--stage', 'request_or_deprive_personal')
    gandalf_role = gandalf.roles.get()
    frodo_role = frodo.roles.get()

    frodo.is_active = False
    frodo.save()

    Role.objects.filter(
        pk__in=(gandalf_role.pk, frodo_role.pk)
    ).update(
        state=ROLE_STATE.DEPRIVING_VALIDATION,
        depriving_at=timezone.now() + timezone.timedelta(days=1)
    )

    call_command('idm_deprive_depriving_roles')

    gandalf_role.refresh_from_db()
    assert gandalf_role.state == ROLE_STATE.DEPRIVING_VALIDATION
    frodo_role.refresh_from_db()
    assert frodo_role.state == ROLE_STATE.DEPRIVED
