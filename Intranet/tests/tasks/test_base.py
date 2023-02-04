import pytest
from django.core.management import call_command

from idm.celery_app import app
from idm.core.models import CommandTimestamp, GroupMembershipSystemRelation, Role, Action
from idm.framework.task import BaseTask, DelayingError
from idm.tests.utils import all_actions, refresh, role_actions, set_switch

pytestmark = [pytest.mark.django_db]


class SomeTaskBase(BaseTask):
    default_retry_delay = None

    def init(self, hello, **kwargs):
        if not hasattr(SomeTask, 'safetorun'):
            SomeTask.safetorun = True
            raise DelayingError('Please retry')
        SomeTask.hello = hello
        SomeTask.foo = kwargs.get('foo')


SomeTask = app.register_task(SomeTaskBase())


def test_retry_preserves_params():
    """Проверка, что при повторном запуске все параметры передаются в таск"""

    SomeTask.delay(hello='world', foo='bar')
    assert SomeTask.hello == 'world'
    assert SomeTask.foo == 'bar'


class SomeUnmonitoredTask(SomeTaskBase):
    monitor_success = False


SomeUnmonitoredTask = app.register_task(SomeUnmonitoredTask())


@pytest.mark.parametrize('with_retry', [False, True])
def test_write_timestamp_for_monitored_task(with_retry):
    CommandTimestamp.objects.all().delete()
    SomeTask.safetorun = not with_retry
    SomeTask.delay(hello='world', foo='bar')
    assert CommandTimestamp.objects.count() == 1
    timestamp = CommandTimestamp.objects.get()
    assert timestamp.command == 'tasks.SomeTaskBase'


@pytest.mark.parametrize('with_retry', [False, True])
def test_do_not_write_timestamp_for_unmonitored_task(with_retry):
    CommandTimestamp.objects.all().delete()
    SomeUnmonitoredTask.safetorun = not with_retry
    SomeUnmonitoredTask.delay(hello='world', foo='bar')
    assert not CommandTimestamp.objects.exists()


@pytest.mark.parametrize('switch_add,switch_remove', [
    ('disable_pushes_all', 'disable_pushes_all'),
    ('disable_pushes_add_roles', 'disable_pushes_remove_roles'),
])
def test_disable_role_pushes(system_aware_of_memberships, arda_users,
                             switch_add, switch_remove):
    """
    проверяем флаги
        disable_pushes_add_roles
        disable_pushes_remove_roles
        disable_pushes_all
    """
    frodo = arda_users.frodo
    system = system_aware_of_memberships
    dump_state = system.plugin.system_mock.dump_state

    with set_switch(switch_add, True):
        role = Role.objects.request_role(frodo, frodo, system, '', {'role': 'admin'})
        assert dump_state() == {'memberships': set(), 'roles': set()}
        assert refresh(role).state == 'approved'
        call_command('idm_poke_hanging_roles', stage='poke_approved')
        assert dump_state() == {'memberships': set(), 'roles': set()}
        assert refresh(role).state == 'approved'
    call_command('idm_poke_hanging_roles', stage='poke_approved')
    role.refresh_from_db()

    assert dump_state() == {'memberships': set(), 'roles': {('/role/admin/', 'frodo', None)}}
    assert refresh(role).state == 'granted'
    assert role_actions(role) == ['request', 'apply_workflow', 'approve', 'first_add_role_push', 'grant']

    with set_switch(switch_remove, True):
        role.deprive_or_decline(frodo)
        assert refresh(role).state == 'depriving'
        assert dump_state() == {'memberships': set(), 'roles': {('/role/admin/', 'frodo', None)}}
        call_command('idm_poke_hanging_roles', stage='poke_depriving')
        assert refresh(role).state == 'depriving'
        assert dump_state() == {'memberships': set(), 'roles': {('/role/admin/', 'frodo', None)}}
    call_command('idm_poke_hanging_roles', stage='poke_depriving')

    assert dump_state() == {'memberships': set(), 'roles': set()}
    assert refresh(role).state == 'deprived'
    assert role_actions(role)[5:] == ['deprive', 'first_remove_role_push', 'redeprive', 'redeprive', 'remove']


@pytest.mark.parametrize('switch_add,switch_remove,deprive_actions', [
    ('disable_pushes_add_memberships', 'disable_pushes_remove_memberships',
     ['deprive', 'first_remove_role_push', 'remove', 'mass_action', 'sysmembership_deprive']),
    ('disable_pushes_all', 'disable_pushes_all',
     ['deprive', 'first_remove_role_push', 'redeprive', 'redeprive', 'remove', 'mass_action', 'sysmembership_deprive']),
])
def test_disable_memberships_pushes(system_aware_of_memberships, arda_users, department_structure,
                                    switch_add, switch_remove, deprive_actions):
    """
    проверяем флаги
        disable_pushes_add_memberships
        disable_pushes_remove_memberships
        disable_pushes_all
    """
    Action.objects.all().delete()
    frodo = arda_users.frodo
    system = system_aware_of_memberships
    dump_state = system.plugin.system_mock.dump_state
    group = department_structure.valinor
    group.memberships.exclude(user=frodo).delete()

    assert dump_state() == {'memberships': set(), 'roles': set()}
    with set_switch(switch_add, True):
        role = Role.objects.request_role(frodo, group, system, '', {'role': 'admin'})
        assert set(GroupMembershipSystemRelation.objects.values_list('state', flat=True)) == {'activating'}
        assert dump_state() == {'memberships': set(), 'roles': set()}
        call_command('idm_push_groupmembership_system_relations')
        assert set(GroupMembershipSystemRelation.objects.values_list('state', flat=True)) == {'activating'}
        assert dump_state() == {'memberships': set(), 'roles': set()}
        assert refresh(role).state == 'awaiting'
    call_command('idm_push_groupmembership_system_relations')
    Role.objects.poke_awaiting_roles()
    assert set(GroupMembershipSystemRelation.objects.values_list('state', flat=True)) == {'activated'}
    assert dump_state() == {'memberships': {('frodo', 'valinor')},
                            'roles': {('/role/admin/', None, 'valinor')}}
    assert all_actions() == [
        'request', 'apply_workflow', 'approve', 'await', 'mass_action',
        'sysmembership_activate', 'approve', 'first_add_role_push', 'grant',
    ]
    Action.objects.all().delete()
    with set_switch(switch_remove, True):
        refresh(role).deprive_or_decline(frodo)
        assert dump_state()['memberships'] == {('frodo', 'valinor')}
        Role.objects.poke_hanging_depriving_roles()
        call_command('idm_sync_groupmembership_system_relations')
        call_command('idm_push_groupmembership_system_relations')
        assert dump_state()['memberships'] == {('frodo', 'valinor')}
    Role.objects.poke_hanging_depriving_roles()
    call_command('idm_sync_groupmembership_system_relations')
    call_command('idm_push_groupmembership_system_relations')
    assert dump_state()['memberships'] == set()
    assert set(GroupMembershipSystemRelation.objects.values_list('state', flat=True)) == {'deprived'}
    assert all_actions() == deprive_actions
