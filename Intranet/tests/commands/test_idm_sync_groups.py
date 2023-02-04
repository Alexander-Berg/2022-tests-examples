# coding: utf-8


from datetime import datetime

import pytest
from django.core.management import call_command
from django.core import mail
from django.utils import timezone
from mock import patch, call

from idm.core.models import Role
from idm.core.constants.groupmembership import GROUPMEMBERSHIP_STATE
from idm.core.constants.system import SYSTEM_GROUP_POLICY
from idm.tests.utils import move_group, set_workflow, DEFAULT_WORKFLOW, clear_mailbox
from idm.users.models import GroupMembership
from idm.users.tasks import SendLoginRemindersToNewGroupMembers

pytestmark = [pytest.mark.django_db]


def test_options():
    """Проверим, что вызов manage-команды базово работает :)"""

    with patch('idm.users.tasks.sync_group_type') as sync_group_type:
        sync_group_type.return_value = True
        call_command('idm_sync_groups')
        assert sync_group_type.call_args_list == [
            call(group_type='department', block=False),
            call(group_type='wiki', block=False),
            call(group_type='service', block=False),
        ]


def test_sync_indirect_memberships(arda_users, department_structure):
    shire_joined = timezone.now() - timezone.timedelta(days=20)
    fellowship_joined = timezone.now() - timezone.timedelta(days=10)
    frodo = arda_users.frodo
    fellowship = department_structure.fellowship
    shire = department_structure.shire
    earth = department_structure.earth
    lands = department_structure.lands
    associations = department_structure.associations
    valinor = department_structure.valinor
    GroupMembership.objects.all().delete()

    # Проверим что новые опосредованные членства создаются после вступления в группу
    GroupMembership.objects.create(user=frodo, group=fellowship, state=GROUPMEMBERSHIP_STATE.ACTIVE, is_direct=True,
                                   date_joined=fellowship_joined)
    GroupMembership.objects.create(user=frodo, group=shire, state=GROUPMEMBERSHIP_STATE.ACTIVE, is_direct=True,
                                   date_joined=shire_joined)

    with patch('idm.users.tasks.sync_group_type') as sync_group_type:
        sync_group_type.return_value = True
        call_command('idm_sync_groups')

    for membership in GroupMembership.objects.filter(is_direct=False):
        assert membership.date_joined < timezone.now()
        assert membership.date_joined > timezone.now() - timezone.timedelta(minutes=5)

    assert GroupMembership.objects.filter(state=GROUPMEMBERSHIP_STATE.ACTIVE).count() == 5
    assert GroupMembership.objects.filter(user=frodo, is_direct=True).count() == 2
    assert GroupMembership.objects.filter(user=frodo, is_direct=False).count() == 3

    for membership in GroupMembership.objects.filter(group=associations):
        assert membership.date_joined < timezone.now()
        assert membership.date_joined > timezone.now() - timezone.timedelta(minutes=5)

    # Проверим, что опосредованные членства становятся неактивными при выходе из группы
    GroupMembership.objects.filter(user=frodo, group=fellowship).update(
        state=GROUPMEMBERSHIP_STATE.INACTIVE, date_leaved=timezone.now(),
    )

    with patch('idm.users.tasks.sync_group_type') as sync_group_type:
        sync_group_type.return_value = True
        call_command('idm_sync_groups')
    assert GroupMembership.objects.filter(state=GROUPMEMBERSHIP_STATE.ACTIVE).count() == 3
    assert GroupMembership.objects.filter(state=GROUPMEMBERSHIP_STATE.INACTIVE).count() == 2

    expected_inactiv_group_ids = sorted([fellowship.id, associations.id])
    inactive_group_ids = sorted(GroupMembership.objects.filter(
        state=GROUPMEMBERSHIP_STATE.INACTIVE,
    ).values_list('group_id', flat=True))
    assert inactive_group_ids == expected_inactiv_group_ids

    for membership in GroupMembership.objects.filter(group=associations):
        assert membership.date_joined < timezone.now()
        assert membership.date_joined > timezone.now() - timezone.timedelta(minutes=5)

    # Проверим, что после возвращения в группу членство вновь станет активным, обновится дата вступления
    # и новое не создается
    old_date_joined = (
        GroupMembership
        .objects
        .filter(group__in=[shire, earth.id, lands.id]).values_list('date_joined', flat=True)
    )
    GroupMembership.objects.filter(user=frodo, group=fellowship).update(
        state=GROUPMEMBERSHIP_STATE.ACTIVE, date_joined=timezone.now(),
    )
    with patch('idm.users.tasks.sync_group_type') as sync_group_type:
        sync_group_type.return_value = True
        call_command('idm_sync_groups')
    assert GroupMembership.objects.count() == 5
    assert GroupMembership.objects.filter(state=GROUPMEMBERSHIP_STATE.ACTIVE).count() == 5
    for membership in GroupMembership.objects.filter(group__in=[fellowship, associations]):
        assert membership.date_joined < timezone.now()
        assert membership.date_joined > timezone.now() - timezone.timedelta(minutes=5)
    # У старых членств дата не поменялась
    for membership in GroupMembership.objects.filter(group__in=[shire, earth.id, lands.id]):
        assert membership.date_joined in old_date_joined

    # Переместим fellowship из associations в shire,
    # проверим что опосредованное членство frodo в associations станет неактивным
    move_group(fellowship, shire)
    with patch('idm.users.tasks.sync_group_type') as sync_group_type:
        sync_group_type.return_value = True
        call_command('idm_sync_groups')
    assert GroupMembership.objects.filter(state=GROUPMEMBERSHIP_STATE.ACTIVE).count() == 5
    assert GroupMembership.objects.filter(
        user=frodo, is_direct=True, state=GROUPMEMBERSHIP_STATE.ACTIVE,
    ).count() == 2
    assert GroupMembership.objects.filter(
        user=frodo, is_direct=False, state=GROUPMEMBERSHIP_STATE.ACTIVE,
    ).count() == 3
    assert GroupMembership.objects.filter(
        user=frodo, is_direct=False, state=GROUPMEMBERSHIP_STATE.INACTIVE,
    ).count() == 1
    assert associations.memberships.filter(state=GROUPMEMBERSHIP_STATE.ACTIVE).count() == 0

    assert GroupMembership.objects.get(
        user=frodo, is_direct=False, state=GROUPMEMBERSHIP_STATE.INACTIVE,
    ).group_id == associations.id

    # Переместим fellowship обратно,
    # проверим что опосредованное членство frodo в associations стало активным
    move_group(fellowship, associations)
    with patch('idm.users.tasks.sync_group_type') as sync_group_type:
        sync_group_type.return_value = True
        call_command('idm_sync_groups')
    assert GroupMembership.objects.filter(state=GROUPMEMBERSHIP_STATE.ACTIVE).count() == 5
    assert associations.memberships.get(user=frodo).state == GROUPMEMBERSHIP_STATE.ACTIVE
    # При возвращении в опосредованную группу сбрасывается date_leaved
    assert associations.memberships.get(user=frodo).date_leaved is None

    # Переместим группу на уровень выше непосредственного членства,
    # проверим что опосредованное членство отзовется, а новое выдастся
    shire.memberships.filter(user=frodo).update(state=GROUPMEMBERSHIP_STATE.INACTIVE)
    with patch('idm.users.tasks.sync_group_type') as sync_group_type:
        sync_group_type.return_value = True
        call_command('idm_sync_groups')
    assert earth.memberships.get(user=frodo).state == GROUPMEMBERSHIP_STATE.ACTIVE

    move_group(associations, valinor)
    with patch('idm.users.tasks.sync_group_type') as sync_group_type:
        sync_group_type.return_value = True
        call_command('idm_sync_groups')
    assert earth.memberships.get(user=frodo).state == GROUPMEMBERSHIP_STATE.INACTIVE
    assert valinor.memberships.get(user=frodo).state == GROUPMEMBERSHIP_STATE.ACTIVE


def test_indirect_and_direct_membership_in_same_group(arda_users, department_structure):
    shire_joined = timezone.now() - timezone.timedelta(days=20)
    fellowship_joined = timezone.now() - timezone.timedelta(days=10)
    associations_joined = timezone.now() - timezone.timedelta(days=5)
    frodo = arda_users.frodo
    fellowship = department_structure.fellowship
    shire = department_structure.shire
    associations = department_structure.associations
    GroupMembership.objects.all().delete()

    # Проверим что у frodo в группе associations создастся только непосредственное членство,
    # т.к. он не входит ни в одну из ниже стоящих групп
    GroupMembership.objects.create(user=frodo, group=associations, state=GROUPMEMBERSHIP_STATE.ACTIVE,
                                   is_direct=True, date_joined=associations_joined)
    with patch('idm.users.tasks.sync_group_type') as sync_group_type:
        sync_group_type.return_value = True
        call_command('idm_sync_groups')
    assert associations.memberships.count() == 1
    assert associations.memberships.direct().filter(state=GROUPMEMBERSHIP_STATE.ACTIVE).count() == 1

    # Проверим что при вступлении в ниже стоящую группу у frodo в associations будет два членства
    # (непосредственное и опосредованное)
    GroupMembership.objects.create(user=frodo, group=fellowship, state=GROUPMEMBERSHIP_STATE.ACTIVE,
                                   is_direct=True, date_joined=fellowship_joined)
    GroupMembership.objects.create(user=frodo, group=shire, state=GROUPMEMBERSHIP_STATE.ACTIVE,
                                   is_direct=True, date_joined=shire_joined)
    with patch('idm.users.tasks.sync_group_type') as sync_group_type:
        sync_group_type.return_value = True
        call_command('idm_sync_groups')

    assert GroupMembership.objects.count() == 6
    assert GroupMembership.objects.filter(state=GROUPMEMBERSHIP_STATE.ACTIVE).count() == 6
    assert associations.memberships.count() == 2
    assert associations.memberships.direct().filter(state=GROUPMEMBERSHIP_STATE.ACTIVE).count() == 1
    assert associations.memberships.indirect().filter(state=GROUPMEMBERSHIP_STATE.ACTIVE).count() == 1

    # Сделаем непосредственное членство frodo в fellowship неактивным
    # Проверим, что опосредованное членство в associations станет неактивным,
    # а непосредственное останется активным
    GroupMembership.objects.filter(
        user=frodo, group=fellowship, is_direct=True
    ).update(state=GROUPMEMBERSHIP_STATE.INACTIVE)
    with patch('idm.users.tasks.sync_group_type') as sync_group_type:
        sync_group_type.return_value = True
        call_command('idm_sync_groups')
    assert GroupMembership.objects.count() == 6
    assert associations.memberships.indirect().filter(state=GROUPMEMBERSHIP_STATE.INACTIVE).count() == 1
    assert associations.memberships.direct().filter(state=GROUPMEMBERSHIP_STATE.ACTIVE).count() == 1

    # Вернем активное членство в fellowship и сделаем непосредственное членство в associations неактивным
    # Проверим, что опосредованное членство станет активным, а непосредственное - неактивным
    GroupMembership.objects.filter(
        user=frodo, group=fellowship, is_direct=True
    ).update(state=GROUPMEMBERSHIP_STATE.ACTIVE)
    GroupMembership.objects.filter(
        user=frodo, group=associations, is_direct=True
    ).update(state=GROUPMEMBERSHIP_STATE.INACTIVE)
    with patch('idm.users.tasks.sync_group_type') as sync_group_type:
        sync_group_type.return_value = True
        call_command('idm_sync_groups')

    assert GroupMembership.objects.count() == 6
    assert GroupMembership.objects.filter(state=GROUPMEMBERSHIP_STATE.ACTIVE).count() == 5
    assert associations.memberships.count() == 2
    assert associations.memberships.direct().filter(state=GROUPMEMBERSHIP_STATE.INACTIVE).count() == 1
    assert associations.memberships.indirect().filter(state=GROUPMEMBERSHIP_STATE.ACTIVE).count() == 1

    # Сделаем непосредственное членство frodo в fellowship неактивным
    # Проверим, что опосредованное членство станет неактивным
    GroupMembership.objects.filter(
        user=frodo, group=fellowship, is_direct=True
    ).update(state=GROUPMEMBERSHIP_STATE.INACTIVE)
    with patch('idm.users.tasks.sync_group_type') as sync_group_type:
        sync_group_type.return_value = True
        call_command('idm_sync_groups')

    assert GroupMembership.objects.count() == 6
    assert GroupMembership.objects.filter(state=GROUPMEMBERSHIP_STATE.ACTIVE).count() == 3
    assert associations.memberships.count() == 2
    assert associations.memberships.direct().filter(state=GROUPMEMBERSHIP_STATE.INACTIVE).count() == 1
    assert associations.memberships.indirect().filter(state=GROUPMEMBERSHIP_STATE.INACTIVE).count() == 1

    # Сделаем непосредственное членство frodo в associations активным
    # Проверим, что опосредованное членство в associations осстанется неактивным
    GroupMembership.objects.filter(
        user=frodo, group=associations, is_direct=True
    ).update(state=GROUPMEMBERSHIP_STATE.ACTIVE)
    with patch('idm.users.tasks.sync_group_type') as sync_group_type:
        sync_group_type.return_value = True
        call_command('idm_sync_groups')

    assert GroupMembership.objects.count() == 6
    assert GroupMembership.objects.filter(state=GROUPMEMBERSHIP_STATE.ACTIVE).count() == 4
    assert associations.memberships.count() == 2
    assert associations.memberships.direct().filter(state=GROUPMEMBERSHIP_STATE.ACTIVE).count() == 1
    assert associations.memberships.indirect().filter(state=GROUPMEMBERSHIP_STATE.INACTIVE).count() == 1
    assert fellowship.memberships.get(user=frodo).state == GROUPMEMBERSHIP_STATE.INACTIVE


@pytest.mark.parametrize('failure_reason', ['multiple_logins', 'passport_failure'])
def test_aware_of_memberships_with_passport_logins(pt1_system, arda_users, department_structure, failure_reason):
    pt1_system.group_policy = SYSTEM_GROUP_POLICY.AWARE_OF_MEMBERSHIPS_WITH_LOGINS
    pt1_system.save()
    frodo = arda_users.frodo
    group = department_structure.fellowship
    set_workflow(pt1_system, group_code=DEFAULT_WORKFLOW)
    group_role = Role.objects.request_role(frodo, group, pt1_system, '', {'project': 'proj1', 'role': 'admin'}, None)
    clear_mailbox()
    sauron = arda_users.sauron
    saruman = arda_users.saruman
    GroupMembership.objects.create(user=sauron, group=group, state=GROUPMEMBERSHIP_STATE.ACTIVE,
                                   is_direct=True, date_joined=timezone.now())
    saruman_membership = GroupMembership.objects.create(user=saruman, group=group, state=GROUPMEMBERSHIP_STATE.ACTIVE,
                                                        is_direct=True, date_joined=timezone.now())
    with patch('idm.users.tasks.sync_group_type') as sync_group_type:
        sync_group_type.return_value = True
        if failure_reason == 'multiple_logins':
            for login in ['login1', 'login2']:
                sauron.passport_logins.create(login=login, state='created')
            call_command('idm_sync_groups')
            SendLoginRemindersToNewGroupMembers()
        else:
            with patch('idm.sync.passport.exists', side_effect=lambda login: 'sauron' in login):
                call_command('idm_sync_groups')
                SendLoginRemindersToNewGroupMembers()
    saruman_membership = GroupMembership.objects.select_related('passport_login').get(pk=saruman_membership.pk)
    assert saruman_membership.passport_login.login == 'yndx-saruman'

    message = mail.outbox[0]
    assert message.subject == 'Участие в группе требует указания паспортного логина'
    assert message.to[0] == 'sauron@example.yandex.ru'
