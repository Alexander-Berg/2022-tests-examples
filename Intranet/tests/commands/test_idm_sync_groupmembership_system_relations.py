# coding: utf-8


import pytest
from freezegun import freeze_time

from waffle.models import Switch
from django.core.management import call_command
from django.conf import settings
from django.utils import timezone

from idm.core.constants.groupmembership import GROUPMEMBERSHIP_STATE
from idm.core.constants.groupmembership_system_relation import MEMBERSHIP_SYSTEM_RELATION_STATE
from idm.core.constants.system import SYSTEM_GROUP_POLICY
from idm.core.models import GroupMembershipSystemRelation, UserPassportLogin, Role
from idm.users.models import Group, GroupMembership, User
from idm.tests.utils import raw_make_role

pytestmark = [pytest.mark.django_db]


def test_get_groups_in_system_ids(simple_system, complex_system, department_structure):
    # Проверим, что функция отдает id групп, у которых для системы есть активные роли
    Switch.objects.create(name='idm.sync_groupmembership_system_relations', active=True)
    fellowship = department_structure.fellowship
    shire = department_structure.shire
    raw_make_role(fellowship, simple_system, {'role': 'manager'}, None, state='deprived')
    raw_make_role(shire, simple_system, {'role': 'admin'}, None, state='granted')

    raw_make_role(fellowship, complex_system, {'project': 'rules', 'role': 'admin'}, None, state='rerequested')
    raw_make_role(shire, complex_system, {'project': 'subs', 'role': 'developer'}, None, state='imported')

    assert list(simple_system.get_group_ids_with_active_roles()) == [shire.id]
    assert sorted(complex_system.get_group_ids_with_active_roles()) == sorted([shire.id, fellowship.id])


def test_bulk_create_groupmembership_system_relations(complex_system, department_structure, settings, arda_users):
    # Проверим, что функция успешно создает связи членство-система
    Switch.objects.create(name='idm.sync_groupmembership_system_relations', active=True)
    settings.IDM_CREATE_GROUPMEMBERSHIPSYSTEMRELATION_BATCH_SIZE = 2
    fellowship = department_structure.fellowship
    shire = department_structure.shire
    valinor = department_structure.valinor
    group_ids = sorted([fellowship.id, shire.id, valinor.id])
    membership_ids = GroupMembership.objects.filter(
        group_id__in=group_ids,
        state=GROUPMEMBERSHIP_STATE.ACTIVE,
    ).values_list('id', flat=True)
    assert not GroupMembershipSystemRelation.objects.count()
    GroupMembershipSystemRelation.objects.bulk_create_groupmembership_system_relations(membership_ids, complex_system)
    assert GroupMembershipSystemRelation.objects.count() == len(membership_ids)
    assert set(GroupMembershipSystemRelation.objects.values_list('membership__group__id', flat=True)) == set(group_ids)
    assert GroupMembershipSystemRelation.objects.filter(state='activating').count() == len(membership_ids)


def test_mark_depriving_when_role_is_inactive(simple_system, department_structure):
    # Проверим, что команда переводит связи членство-система в неактивное состояние,
    # если в системе нет активных ролей на группу
    Switch.objects.create(name='idm.sync_groupmembership_system_relations', active=True)
    fellowship = department_structure.fellowship
    shire = department_structure.shire
    valinor = department_structure.valinor

    group_ids = sorted([fellowship.id, shire.id, valinor.id])
    membership_ids = GroupMembership.objects.filter(
        group_id__in=group_ids,
        state=GROUPMEMBERSHIP_STATE.ACTIVE,
    ).values_list('id', flat=True)
    GroupMembershipSystemRelation.objects.bulk_create_groupmembership_system_relations(membership_ids, simple_system)

    assert GroupMembershipSystemRelation.objects.filter(state='activating').count() == len(membership_ids)

    raw_make_role(fellowship, simple_system, {'role': 'manager'}, None, state='deprived')
    raw_make_role(shire, simple_system, {'role': 'admin'}, None, state='granted')
    shire.memberships.filter(user__username='bilbo').update(state=GROUPMEMBERSHIP_STATE.INACTIVE)

    call_command('idm_sync_groupmembership_system_relations', '--system', simple_system.slug)

    # роль fellowship в статусе deprived
    # у valinor вообще нет ролей
    # членство bilbo удалили, а IDM_ONHOLD_GROUPMEMBERSHIP_SYSTEM_SECONDS > 0
    expected_depriving_count = fellowship.memberships.count() + valinor.memberships.count()

    assert GroupMembershipSystemRelation.objects.filter(state='depriving').count() == expected_depriving_count
    groups = set(
        GroupMembershipSystemRelation
        .objects
        .filter(state='activating')
        .values_list('membership__group_id', flat=True)
    )
    assert groups == {shire.id}


def test_mark_depriving_after_hold(simple_system, department_structure):
    Switch.objects.create(name='idm.sync_groupmembership_system_relations', active=True)
    fellowship = department_structure.fellowship
    shire = department_structure.shire
    valinor = department_structure.valinor
    group_ids = sorted([fellowship.id, shire.id, valinor.id])
    membership_ids = GroupMembership.objects.filter(
        group_id__in=group_ids,
        state=GROUPMEMBERSHIP_STATE.ACTIVE,
    ).values_list('id', flat=True)
    for group in (fellowship, shire, valinor):
        raw_make_role(group, simple_system, {'role': 'manager'})

    call_command('idm_sync_groupmembership_system_relations', '--system', simple_system.slug)

    # За время холда может поменяться паспортный логин в членстве, мы должны его посинкать, запушить
    # Это не должно повлиять на то, членство станет depriving через нужный срок
    for user in User.objects.iterator():
        login = UserPassportLogin(
            user=user,
            login='yndx-' + user.username,
        )
        login.save()
    with freeze_time(timezone.now() + timezone.timedelta(days=3)):
        for membership in GroupMembership.objects.iterator():
            membership.passport_login = UserPassportLogin.objects.get(user_id=membership.user_id)
            membership.save()
        call_command('idm_sync_groupmembership_system_relations', '--system', simple_system.slug)
        simple_system.push_need_update_group_memberships_async()

    GroupMembership.objects.all().update(state='inactive')
    with freeze_time(timezone.now() + timezone.timedelta(seconds=settings.IDM_ONHOLD_GROUPMEMBERSHIP_SYSTEM_SECONDS + 10)):
        call_command('idm_sync_groupmembership_system_relations', '--system', simple_system.slug)
        # Был случай, что поле updated_at не обновлялось
        membership = GroupMembershipSystemRelation.objects.first()
        assert membership.updated_at - membership.created_at >= timezone.timedelta(
            seconds=settings.IDM_ONHOLD_GROUPMEMBERSHIP_SYSTEM_SECONDS)
    assert GroupMembershipSystemRelation.objects.filter(state='depriving').count() == len(membership_ids)


def test_mark_activating(simple_system, department_structure):
    # Проверим, что команда переводит связи группа-система в активное состояние,
    # если в системе появились активные роли на группу
    Switch.objects.create(name='idm.sync_groupmembership_system_relations', active=True)
    fellowship = department_structure.fellowship
    shire = department_structure.shire
    valinor = department_structure.valinor

    group_ids = sorted([fellowship.id, shire.id, valinor.id])
    membership_ids = GroupMembership.objects.filter(
        group_id__in=group_ids,
        state=GROUPMEMBERSHIP_STATE.ACTIVE,
    ).values_list('id', flat=True)
    GroupMembershipSystemRelation.objects.bulk_create_groupmembership_system_relations(membership_ids, simple_system)
    GroupMembershipSystemRelation.objects.update(state='depriving')

    assert GroupMembershipSystemRelation.objects.filter(state='depriving').count() == len(membership_ids)

    raw_make_role(fellowship, simple_system, {'role': 'manager'}, None, state='deprived')
    raw_make_role(shire, simple_system, {'role': 'admin'}, None, state='granted')

    call_command('idm_sync_groupmembership_system_relations', '--system', simple_system.slug)

    assert GroupMembershipSystemRelation.objects.filter(state='activating').count() == shire.memberships.count()
    groups = set(
        GroupMembershipSystemRelation
        .objects
        .filter(state='activating')
        .values_list('membership__group_id', flat=True)
    )
    assert groups == {shire.id}


@pytest.mark.parametrize('group_policy', SYSTEM_GROUP_POLICY.AWARE_OF_MEMBERSHIPS)
def test_create_missing_groupmembreship_system_relations(
        simple_system, complex_system, department_structure, group_policy,
    ):
    # Проверим, что команда создает новые связи группа-система, если в системе появились активные роли на группу
    # но только для системы с групповой политиками 'aware_of_memberships'
    Switch.objects.create(name='idm.sync_groupmembership_system_relations', active=True)
    simple_system.group_policy = group_policy
    simple_system.save()

    fellowship = department_structure.fellowship
    shire = department_structure.shire
    valinor = department_structure.valinor

    raw_make_role(fellowship, simple_system, {'role': 'manager'}, None, state='deprived')
    raw_make_role(valinor, simple_system, {'role': 'manager'}, None, state='need_request')
    raw_make_role(shire, simple_system, {'role': 'admin'}, None, state='granted')

    raw_make_role(fellowship, complex_system, {'project': 'rules', 'role': 'admin'}, None, state='rerequested')
    raw_make_role(shire, complex_system, {'project': 'subs', 'role': 'developer'}, None, state='imported')

    call_command('idm_sync_groupmembership_system_relations')

    expected_memberships_count = shire.memberships.count() + valinor.memberships.count()

    assert GroupMembershipSystemRelation.objects.count() == expected_memberships_count
    system_ids = set(GroupMembershipSystemRelation.objects.values_list('system_id', flat=True))
    assert system_ids == {simple_system.id}
    group_ids = set(GroupMembershipSystemRelation.objects.values_list('membership__group_id', flat=True))
    assert group_ids == {shire.id, valinor.id}


@pytest.mark.parametrize('group_policy', SYSTEM_GROUP_POLICY.AWARE_OF_MEMBERSHIPS)
def test_sync_passport_logins(simple_system, department_structure, group_policy):
    # Паспортые логины хранятся в таблице системочленств, если паспортный логин
    # в групповом членстве изменился с последнего синка, проставляем флаг need_update
    Switch.objects.create(name='idm.sync_groupmembership_system_relations', active=True)
    simple_system.group_policy = group_policy
    simple_system.save()
    fellowship = department_structure.fellowship
    shire = department_structure.shire
    valinor = department_structure.valinor
    raw_make_role(fellowship, simple_system, {'role': 'manager'}, None, state='granted')
    raw_make_role(valinor, simple_system, {'role': 'manager'}, None, state='granted')
    raw_make_role(shire, simple_system, {'role': 'admin'}, None, state='granted')
    for user in User.objects.iterator():
        login = UserPassportLogin(
            user=user,
            login='yndx-' + user.username,
        )
        login.save()
    call_command('idm_sync_groupmembership_system_relations')
    assert GroupMembershipSystemRelation.objects.count() == (
        fellowship.memberships.count() + shire.memberships.count() + valinor.memberships.count())
    assert set(GroupMembershipSystemRelation.objects.values_list('passport_login', flat=True)) == {None}
    for membership in shire.memberships.iterator():
        membership.passport_login = UserPassportLogin.objects.get(user_id=membership.user_id)
        membership.save()
    call_command('idm_sync_groupmembership_system_relations')
    assert set(GroupMembershipSystemRelation.objects.exclude(
        membership__group__slug='the-shire').values_list('passport_login', flat=True)) == {None}
    for membership in shire.memberships.iterator():
        system_membership = GroupMembershipSystemRelation.objects.get(membership=membership)
        assert system_membership.passport_login_id is not None
        assert system_membership.passport_login_id == membership.passport_login_id

    GroupMembershipSystemRelation.objects.update(state='activated', need_update=False)
    for membership in shire.memberships.select_related('user').iterator():
        passport_login = UserPassportLogin(
            user=membership.user,
            login='login-' + membership.user.username,
        )
        passport_login.save()
        membership.passport_login = passport_login
        membership.save()
    for membership in fellowship.memberships.iterator():
        membership.passport_login = UserPassportLogin.objects.filter(user_id=membership.user_id).first()
        membership.save()
    need_update = group_policy == SYSTEM_GROUP_POLICY.AWARE_OF_MEMBERSHIPS_WITH_LOGINS

    call_command('idm_sync_groupmembership_system_relations')
    assert set(GroupMembershipSystemRelation.objects.filter(
        membership__group__slug='the-shire').values_list('need_update', flat=True)) == {need_update}
    assert set(GroupMembershipSystemRelation.objects.filter(
        membership__group__slug='fellowship-of-the-ring').values_list('need_update', flat=True)) == {need_update}
    assert set(GroupMembershipSystemRelation.objects.exclude(
        membership__group__slug__in=['the-shire', 'fellowship-of-the-ring']
    ).values_list('need_update', flat=True)) == {False}

    GroupMembershipSystemRelation.objects.update(state='hold', need_update=False)
    for membership in GroupMembership.objects.iterator():
        membership.passport_login = None
        membership.save()
    call_command('idm_sync_groupmembership_system_relations')
    assert set(GroupMembershipSystemRelation.objects.filter(
        membership__group__slug='the-shire').values_list('need_update', flat=True)) == {need_update}
    assert set(GroupMembershipSystemRelation.objects.filter(
        membership__group__slug='fellowship-of-the-ring').values_list('need_update', flat=True)) == {need_update}
    assert set(GroupMembershipSystemRelation.objects.exclude(
        membership__group__slug__in=['the-shire', 'fellowship-of-the-ring']
    ).values_list('need_update', flat=True)) == {False}


@pytest.mark.parametrize('source_state', MEMBERSHIP_SYSTEM_RELATION_STATE.CAN_BE_PUT_ON_DEPRIVING_STATES)
@pytest.mark.parametrize('group_policy', SYSTEM_GROUP_POLICY.AWARE_OF_MEMBERSHIPS)
def test_deprive_dismissed_groupmembreship_system_relations(
        simple_system, department_structure, group_policy, arda_users, source_state
    ):
    # Проверим, что команда сразу удаляет системочленства уволенных
    Switch.objects.create(name='idm.sync_groupmembership_system_relations', active=True)
    simple_system.group_policy = group_policy
    simple_system.save()

    sam = arda_users.sam
    shire = department_structure.shire

    raw_make_role(shire, simple_system, {'role': 'admin'}, None, state='granted')

    call_command('idm_sync_groupmembership_system_relations')
    assert GroupMembershipSystemRelation.objects.count() == shire.memberships.count()
    GroupMembershipSystemRelation.objects.filter(membership__user=sam).update(state=source_state)

    sam.is_active=False
    sam.save()

    call_command('idm_sync_groupmembership_system_relations')
    assert GroupMembershipSystemRelation.objects.get(membership__user=sam).state == 'depriving'


def test_mark_depriving_with_active_duplicate(simple_system, arda_users):
    frodo = arda_users.frodo
    group = Group.objects.create(type='department', external_id=0)
    m_1 = GroupMembership.objects.create(group=group, user=frodo, is_direct=True, state='active')
    m_2 = GroupMembership.objects.create(group=group, user=frodo, is_direct=False, state='active')

    gms_1 = GroupMembershipSystemRelation.objects.create(membership=m_1, system=simple_system, state='activated')
    gms_2 = GroupMembershipSystemRelation.objects.create(membership=m_2, system=simple_system, state='activated')

    GroupMembershipSystemRelation.objects.filter(pk=gms_1.pk).mark_depriving()

    # сразу перешло в deprived, так как был дубль
    gms_1.refresh_from_db()
    assert gms_1.state == 'deprived'

    GroupMembershipSystemRelation.objects.filter(pk=gms_2.pk).mark_depriving()

    # дубля уже нет, поэтому переходит в depriving
    gms_2.refresh_from_db()
    assert gms_2.state == 'depriving'
