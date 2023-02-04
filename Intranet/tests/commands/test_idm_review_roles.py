# coding: utf-8
from datetime import timedelta
from unittest import mock

import constance
import pytest
from django.conf import settings
from django.core import mail
from django.core.management import call_command
from django.utils import timezone
from freezegun import freeze_time
from waffle.testutils import override_switch

from idm.core.constants.role import ROLE_STATE
from idm.core.models import Role, Action, ApproveRequest, RoleRequest
from idm.monitorings.metric import ReviewRolesThresholdExceededMetric
from idm.tests.utils import (set_workflow, refresh, clear_mailbox, assert_contains, make_role,
                             assert_action_chain, expire_role, DEFAULT_WORKFLOW, raw_make_role, create_user,
                             create_system)

pytestmark = [pytest.mark.django_db]


@pytest.fixture(autouse=True)
def use_robot_here(idm_robot):
    """Все тесты в этом модуле используют фикстуру с роботом"""


@pytest.fixture(autouse=True)
def calendar_get_holidays_mock():
    with mock.patch('idm.integration.calendar.get_holidays', return_value=[]) as _mock:
        yield _mock


def test_regular_roles_review_rerequest(simple_system, arda_users):

    """
    Запускаем команду регулярного пересмотра ролей и следим,
    чтобы роли без аппруверов сразу продлились, остальные - по подтверждению
    """

    frodo = arda_users.get('frodo')
    legolas = arda_users.get('legolas')
    frodo_admin = make_role(frodo, simple_system, {'role': 'admin'})
    frodo_manager = make_role(frodo, simple_system, {'role': 'manager'})

    set_workflow(simple_system, """
if role == {'role': 'manager'}:
    approvers = []
else:
    approvers = [approver('gandalf')]
""")

    # даже если роль выдана после обновления workflow, все равно будем проверять
    legolas_admin = make_role(legolas, simple_system, {'role': 'admin'})
    legolas_manager = make_role(legolas, simple_system, {'role': 'manager'})
    clear_mailbox()

    # вызываем команду регулярного пересмотра ролей
    with freeze_time(timezone.now() + timedelta(settings.IDM_DEFAULT_REVIEW_ROLES_DAYS + 1)):
        call_command('idm_review_roles')

    # письмо не посылается, так как мы больше этого не делаем:
    # http://wiki.yandex-team.ru/Upravljator/emails
    assert len(mail.outbox) == 0

    frodo_admin = refresh(frodo_admin)
    frodo_manager = refresh(frodo_manager)
    legolas_admin = refresh(legolas_admin)
    legolas_manager = refresh(legolas_manager)

    for manager_role in (frodo_manager, legolas_manager):
        assert manager_role.state == 'granted'
        assert manager_role.is_active
        assert manager_role.requests.count() == 1
        expected_chain = [
            'request', 'approve', 'first_add_role_push', 'grant',
            'review_rerequest', 'apply_workflow', 'approve', 'grant',
        ]
        assert_action_chain(manager_role, expected_chain)
        review_action = manager_role.actions.get(action='review_rerequest')
        expected_message = 'Перезапрос роли в связи с регулярным пересмотром'
        assert review_action.comment == expected_message
        assert manager_role.expire_at is None

    for admin_role in (frodo_admin, legolas_admin):
        assert admin_role.state == 'review_request'
        assert admin_role.is_active
        assert admin_role.requests.count() == 1
        assert not admin_role.requests.get().is_done
        assert_action_chain(admin_role, [
            'request', 'approve', 'first_add_role_push', 'grant', 'review_rerequest', 'apply_workflow',
        ])
        review_action = admin_role.actions.get(action='review_rerequest')
        expected_message = 'Перезапрос роли в связи с регулярным пересмотром'
        assert review_action.comment == expected_message
        assert admin_role.expire_at is not None


@pytest.mark.parametrize('autoapprove_reason', ['RUN_REASON.REREQUEST', 'RUN_REASON.CREATE_APPROVE_REQUEST'])
def test_regular_roles_review_rerequest_auto_approve(simple_system, arda_users, autoapprove_reason):
    frodo = arda_users.get('frodo')
    frodo_admin = make_role(frodo, simple_system, {'role': 'admin'})
    frodo_manager = make_role(frodo, simple_system, {'role': 'manager'})

    set_workflow(simple_system, f"""
if reason == {autoapprove_reason} and role == {{'role': 'manager'}}:
    approvers = []
else:
    approvers = [approver('gandalf')]
""")

    with freeze_time(timezone.now() + timedelta(settings.IDM_DEFAULT_REVIEW_ROLES_DAYS + 1)):
        call_command('idm_review_roles')

    frodo_admin = refresh(frodo_admin)
    frodo_manager = refresh(frodo_manager)
    if autoapprove_reason == 'RUN_REASON.REREQUEST':
        assert frodo_manager.state == 'granted'
    else:
        assert frodo_manager.state == 'review_request'
    assert frodo_manager.is_active
    assert frodo_manager.requests.count() == 1

    assert frodo_admin.state == 'review_request'
    assert frodo_admin.is_active
    assert frodo_admin.requests.count() == 1


def test_dont_review_other_system_roles(simple_system, generic_system, arda_users):
    frodo = arda_users.frodo

    generic_system.auth_factor = 'cert'
    generic_system.save()
    generic_system_role = make_role(
        frodo, generic_system,
        data={'role': 'manager'},
    )
    generic_system_role.granted_at = timezone.now() - timezone.timedelta(days=361)
    generic_system_role.save(update_fields=['granted_at'])
    simple_system_role = make_role(
        frodo, simple_system,
        data={'role': 'manager'},
    )
    simple_system_role.granted_at = timezone.now() - timezone.timedelta(days=361)
    simple_system_role.save(update_fields=['granted_at'])

    assert list(
        Role.objects
        .get_roles_for_review(simple_system, timezone.now())
        .values_list('pk', flat=True)
    ) == [simple_system_role.pk]


@pytest.mark.parametrize('review_required_default', [True, False, None])
@pytest.mark.parametrize('review_required', [True, False, None])
def test_review_roles_system_review_required_option(simple_system, arda_users, review_required_default, review_required):
    simple_system.review_required_default = review_required_default
    simple_system.save(update_fields=['review_required_default'])

    frodo = arda_users.frodo
    sam = arda_users.sam

    roles_for_users = {}
    role_types = ('manager', 'admin')

    for user, role_type in zip((frodo, sam), role_types):
        roles_for_users[user] = make_role(user, simple_system, data={'role': role_type})
        roles_for_users[user].granted_at = timezone.now() - timezone.timedelta(days=361)
        roles_for_users[user].save(update_fields=['granted_at'])
    roles_for_users[sam].node.review_required = review_required
    roles_for_users[sam].node.save(update_fields=['review_required'])

    expected_role_pks = set()
    if review_required_default is None:
        expected_role_pks = {role.pk for role in roles_for_users.values()}
    elif review_required_default is True:
        expected_role_pks = {role.pk for role in roles_for_users.values() if role.node.review_required is not False}
    elif review_required_default is False:
        expected_role_pks = {role.pk for role in roles_for_users.values() if role.node.review_required}

    assert expected_role_pks == \
           set(Role.objects.get_roles_for_review(simple_system, timezone.now()).values_list('pk', flat=True))


def test_review_hidden_role(pt1_system, arda_users):
    """
    Скрытые роли не перезапрашиваются
    """
    frodo = arda_users.get('frodo')
    frodo.passport_logins.create(login='yndx.frodo', state='created', is_fully_registered=True)
    frodo_admin = make_role(
        frodo, pt1_system,
        data={'project': 'proj2', 'role': 'invisible_role'},
        fields_data={'passport-login': 'yndx.frodo'},
    )

    set_workflow(pt1_system, "approvers = [approver('gandalf')]")
    clear_mailbox()

    # вызываем команду регулярного пересмотра ролей
    with freeze_time(timezone.now() + timedelta(settings.IDM_DEFAULT_REVIEW_ROLES_DAYS + 1)):
        call_command('idm_review_roles')

    frodo_admin = refresh(frodo_admin)
    assert frodo_admin.state == 'granted'
    expected_chain = ['request', 'approve', 'first_add_role_push', 'grant']
    assert_action_chain(frodo_admin, expected_chain)


def test_regular_roles_review_rerequest_approve(simple_system, arda_users, idm_robot):
    """Запускаем команду регулярного пересмотра ролей, проверяем, что проаппрувленные роли снова выдались"""

    frodo = arda_users.get('frodo')
    gandalf = arda_users.get('gandalf')

    role = Role.objects.request_role(frodo, frodo, simple_system, '', {'role': 'admin'}, None)
    set_workflow(simple_system, "approvers = [approver('gandalf')]")
    clear_mailbox()

    # вызываем команду регулярного пересмотра ролей
    with freeze_time(timezone.now() + timedelta(settings.IDM_DEFAULT_REVIEW_ROLES_DAYS + 1)):
        call_command('idm_review_roles')

    role = refresh(role)
    assert role.state == 'review_request'
    assert_action_chain(role, [
        'request', 'apply_workflow', 'approve', 'first_add_role_push', 'grant', 'review_rerequest', 'apply_workflow'
    ])
    assert role.is_active
    assert role.expire_at is not None

    assert role.requests.count() == 2  # второй запрос
    assert not role.get_last_request().is_done
    assert len(mail.outbox) == 0

    # подтвердим роль и убедимся, что она снова granted плюс указано про пересмотр роли
    aprequest = role.get_last_request().approves.get().requests.select_related_for_set_decided().get()
    aprequest.set_approved(gandalf)

    role = refresh(role)
    assert role.state == 'granted'
    assert role.is_active
    assert_action_chain(role, [
        'request', 'apply_workflow', 'approve', 'first_add_role_push', 'grant',
        'review_rerequest', 'apply_workflow', 'approve', 'grant',
    ])

    grant_action = Action.objects.filter(action='grant').order_by('-pk').first()
    expected = 'Перезапрошенная в связи с регулярным пересмотром роль повторно подтверждена и осталась в системе.'
    assert grant_action.comment == expected

    with pytest.raises(RoleRequest.DoesNotExist):
        role.get_open_request()


@pytest.mark.parametrize('workflow,requests_count', [
    ("approvers = [approver('frodo')]", 1),
    ("approvers = [approver('legolas') | approver('frodo')]", 2),
    ("approvers = [approver('frodo'), approver('frodo') | approver('legolas')]", 1),
    ("approvers = [approver('frodo'), approver('frodo'), approver('gandalf') | approver('frodo')]", 1),
])
def test_regular_roles_review_rerequest_auto_approve_if_user_is_approver(
        simple_system, arda_users, workflow, requests_count):
    """Запускаем команду регулярного пересмотра ролей. Проверяем, что если пользователь, для которого
    запрашивается регулярный пересмотр, сам же является и подтверждающим этой роли, то перевыдача роли
    происходит автоматически. RULES-1479"""

    set_workflow(simple_system, workflow)
    frodo = arda_users.get('frodo')
    role = make_role(frodo, simple_system, {'role': 'admin'})
    clear_mailbox()

    # вызываем команду регулярного пересмотра ролей
    with freeze_time(timezone.now() + timedelta(settings.IDM_DEFAULT_REVIEW_ROLES_DAYS + 1)):
        call_command('idm_review_roles')

    role = refresh(role)
    assert role.state == 'granted'
    assert role.is_active
    assert role.expire_at is None
    assert role.requests.count() == 1

    role_request = role.requests.get()
    assert role_request.is_done is True
    assert role_request.approves.count() == 1
    approve = role_request.approves.get()
    assert approve.requests.count() == requests_count
    assert len(mail.outbox) == 0

    assert_action_chain(role, [
        'request', 'approve', 'first_add_role_push', 'grant', 'review_rerequest', 'apply_workflow', 'approve', 'grant'
    ])
    action_review_rerequest = role.actions.get(action='review_rerequest')
    action_approve = role.actions.filter(action='approve').order_by('-pk')[0]
    action_grant = role.actions.filter(action='grant').order_by('-pk')[0]
    expected_comment = 'Перезапрос роли в связи с регулярным пересмотром'
    assert action_review_rerequest.comment == expected_comment

    expected_comment = ('Перезапрошенная в связи с регулярным пересмотром роль повторно '
                        'подтверждена и осталась в системе.')
    assert action_grant.comment == expected_comment


def test_regular_roles_review_rerequest_not_auto_approved_if_user_is_just_one_of_approvers(simple_system, arda_users):
    """Запускаем команду регулярного пересмотра ролей. Проверяем, что если пользователь, для которого
    запрашивается регулярный пересмотр, является подтверждающим этой роли, но не единственным подтверждающим,
    то перевыдача роли автоматически не происходит, однако подтверждения требуются только от других
    подтверждающих. Тикет: RULES-1479"""

    frodo = arda_users.get('frodo')
    workflows_data = (
        {
            'workflow': "approvers = ['frodo', 'legolas']",
            'approvers_repr': '[approver(frodo, priority=1), approver(legolas, priority=1)]',
            'node': {'role': 'admin'},
        },
        {
            'workflow': "approvers = [approver('gandalf') | approver('frodo'), 'legolas']",
            'approvers_repr': '[any_from([approver(gandalf, priority=1),approver(frodo, '
                              'priority=2)]), approver(legolas, priority=1)]',
            'node': {'role': 'manager'},
        }
    )
    for workflow_data in workflows_data:
        workflow, approvers_repr = workflow_data['workflow'], workflow_data['approvers_repr']
        set_workflow(simple_system, workflow)
        role = make_role(frodo, simple_system, workflow_data['node'])
        clear_mailbox()

        # вызываем команду регулярного пересмотра ролей
        with freeze_time(timezone.now() + timedelta(settings.IDM_DEFAULT_REVIEW_ROLES_DAYS + 1)):
            call_command('idm_review_roles')

        role = refresh(role)
        assert role.state == 'review_request'
        assert role.is_active
        assert role.expire_at is not None
        assert role.requests.count() == 1

        role_request = role.requests.get()
        assert role_request.approves.count() == 2
        assert not role_request.is_done
        assert not mail.outbox

        assert_action_chain(role, ['request', 'approve', 'first_add_role_push', 'grant', 'review_rerequest', 'apply_workflow'])
        action = role.actions.get(action='review_rerequest')
        assert action.comment == 'Перезапрос роли в связи с регулярным пересмотром'


def test_review_rerequest_creates_one_approve_request_per_repeating_user(simple_system, arda_users):
    """Запускаем команду регулярного пересмотра ролей. Проверяем, что если в нескольких OR-группах есть
    один и тот же пользователь, для него создастся несколько approve request-ов по количеству вхождений, но
    один из них будет главным"""

    workflow = "approvers = [approver('legolas') | 'gandalf', approver('varda') | 'gandalf']"
    set_workflow(simple_system, workflow)
    frodo = arda_users.get('frodo')
    role = make_role(frodo, simple_system, {'role': 'admin'})
    clear_mailbox()

    # вызываем команду регулярного пересмотра ролей
    with freeze_time(timezone.now() + timedelta(settings.IDM_DEFAULT_REVIEW_ROLES_DAYS + 1)):
        call_command('idm_review_roles')
    role = refresh(role)
    assert role.state == 'review_request'
    assert role.expire_at is not None

    assert role.requests.count() == 1
    role_request = role.requests.get()
    assert not role_request.is_done
    assert role_request.approves.count() == 2
    assert ApproveRequest.objects.count() == 4
    assert ApproveRequest.objects.filter(parent=None).count() == 3
    gandalf_requests = ApproveRequest.objects.filter(approver=arda_users.gandalf).order_by('pk')
    assert gandalf_requests.count() == 2
    request1, request2 = gandalf_requests
    assert request1.parent is None
    assert request2.parent_id == request1.id

    review_action = role.actions.get(action='review_rerequest')
    expected_message = 'Перезапрос роли в связи с регулярным пересмотром'
    assert review_action.comment == expected_message
    assert len(mail.outbox) == 0


def test_regular_roles_review_rerequest_deprive(simple_system, arda_users):
    """Запускаем команду регулярного пересмотра ролей, проверяем, что отозванная роль удалена с нужным сообщением"""

    frodo = arda_users.get('frodo')
    gandalf = arda_users.get('gandalf')
    role = make_role(frodo, simple_system, {'role': 'admin'})
    set_workflow(simple_system, "approvers = [approver('gandalf')]")
    clear_mailbox()

    with freeze_time(timezone.now() + timedelta(settings.IDM_DEFAULT_REVIEW_ROLES_DAYS + 1)):
        call_command('idm_review_roles')
    role = refresh(role)

    # отзовем роль
    aprequest = ApproveRequest.objects.select_related_for_set_decided().get()
    aprequest.set_declined(gandalf)

    role = refresh(role)
    assert role.state == 'deprived'
    assert not role.is_active
    assert_action_chain(role, [
        'request', 'approve', 'first_add_role_push', 'grant', 'review_rerequest',
        'apply_workflow', 'deprive', 'first_remove_role_push', 'remove',
    ])
    deprive_action = role.actions.get(action='deprive')
    expected_comment = 'Перезапрошенная в связи с регулярным пересмотром роль отозвана и будет удалена из системы'
    assert deprive_action.comment == expected_comment
    assert role.requests.count() == 1
    assert role.requests.get().is_done
    assert len(mail.outbox) == 1
    message = mail.outbox[0]
    assert message.to == ['frodo@example.yandex.ru']
    assert message.subject == 'Simple система. Роль отозвана'
    assert 'gandalf отозвал вашу роль в системе "Simple система":' in message.body


@pytest.mark.parametrize(
    'deprive_not_immediately,target_state',
    [
        (True, ROLE_STATE.DEPRIVING_VALIDATION),
        (False, ROLE_STATE.DEPRIVED),
    ]
)
def test_regular_roles_review_rerequest_expire(simple_system, arda_users, deprive_not_immediately, target_state):
    """Запускаем команду регулярного пересмотра ролей, проверяем, что неподтвержденная роль протухает и отзывается
    с нужным сообщением
    """

    frodo = arda_users.get('frodo')
    role = make_role(frodo, simple_system, {'role': 'admin'})
    clear_mailbox()
    set_workflow(simple_system, "approvers = [approver('gandalf')]")

    with freeze_time(timezone.now() + timedelta(settings.IDM_DEFAULT_REVIEW_ROLES_DAYS + 1)):
        call_command('idm_review_roles')

    role = refresh(role)
    assert role.state == 'review_request'
    action = role.actions.exclude(action='apply_workflow').order_by('-pk').first()
    assert action.action == 'review_rerequest'
    assert action.comment == 'Перезапрос роли в связи с регулярным пересмотром'

    # выполним команду сбора просроченных ролей и посмотрим что будет
    expire_role(role)
    with override_switch('idm.deprive_not_immediately', active=deprive_not_immediately):
        call_command('idm_deprive_expired')

    role = refresh(role)
    assert role.state == target_state
    assert_action_chain(
        role,
        [
            'request', 'approve', 'first_add_role_push', 'grant', 'review_rerequest', 'apply_workflow', 'expire',
        ] + (['first_remove_role_push', 'remove'] if not deprive_not_immediately else [])
    )
    expire_action = role.actions.get(action='expire')
    expected_comment = 'Перезапрошенная в связи с регулярным пересмотром роль так и не получила подтвержения в срок.'
    assert expire_action.comment.startswith(expected_comment)
    assert role.requests.count() == 1
    assert role.requests.get().is_done

    if not deprive_not_immediately:
        assert len(mail.outbox) == 1
        message = mail.outbox[0]
        assert message.to == ['frodo@example.yandex.ru']
        assert message.subject == 'Simple система. Роль отозвана'
        assert_contains([
            'Робот отозвал вашу роль в системе "Simple система":',
            'Комментарий: Перезапрошенная в связи с регулярным пересмотром роль так и не получила подтвержения в срок.'
        ], message.body)


def test_review_request_group_roles_on_workflow_change(simple_system, arda_users, department_structure):
    """Запускаем команду регулярного пересмотра групповых ролей и следим, чтобы роли без аппруверов сразу продлились,
    остальные - по подтверждению"""

    fellowship = department_structure.fellowship
    shire = department_structure.shire
    frodo = arda_users.frodo
    gandalf = arda_users.gandalf
    set_workflow(simple_system, group_code=DEFAULT_WORKFLOW)
    fellowship_admin = Role.objects.request_role(frodo, fellowship, simple_system, '', {'role': 'admin'}, None)
    fellowship_manager = Role.objects.request_role(frodo, fellowship, simple_system, '', {'role': 'manager'}, None)
    set_workflow(simple_system, group_code="""
if role == {'role': 'manager'}:
    approvers = []
else:
    approvers = [approver('gandalf')]
""")

    # даже если роль выдана после обновления workflow, все равно будем проверять
    shire_admin = Role.objects.request_role(frodo, shire, simple_system, '', {'role': 'admin'}, None)
    shire_manager = Role.objects.request_role(frodo, shire, simple_system, '', {'role': 'manager'}, None)
    approve_request = shire_admin.get_open_request().approves.get().requests.get()
    approve_request.set_approved(gandalf)
    for role in (fellowship_admin, fellowship_manager, shire_admin, shire_manager):
        role = refresh(role)
        assert role.state == 'granted'
    clear_mailbox()

    # вызываем команду регулярного пересмотра ролей
    with freeze_time(timezone.now() + timedelta(settings.IDM_DEFAULT_REVIEW_ROLES_DAYS + 1)):
        call_command('idm_review_roles')

    assert len(mail.outbox) == 0
    ref_roles = Role.objects.filter(parent__isnull=False)
    assert ref_roles.count() == ref_roles.filter(state='granted').count()

    fellowship_admin = refresh(fellowship_admin)
    fellowship_manager = refresh(fellowship_manager)
    shire_admin = refresh(shire_admin)
    shire_manager = refresh(shire_manager)

    for manager_role in (fellowship_manager, shire_manager):
        assert manager_role.state == 'granted'
        assert manager_role.is_active
        assert manager_role.requests.count() == 2
        expected_chain = ['request', 'apply_workflow', 'approve', 'grant', 'review_rerequest', 'apply_workflow', 'approve', 'grant']
        assert_action_chain(manager_role, expected_chain)
        review_action = manager_role.actions.get(action='review_rerequest')
        expected_message = 'Перезапрос роли в связи с регулярным пересмотром'
        assert review_action.comment == expected_message
        assert manager_role.expire_at is None

    for admin_role in (fellowship_admin, shire_admin):
        assert admin_role.state == 'review_request'
        assert admin_role.is_active
        assert admin_role.requests.count() == 2
        assert not admin_role.get_last_request().is_done
        assert_action_chain(admin_role, ['request', 'apply_workflow', 'approve', 'grant', 'review_rerequest', 'apply_workflow'])
        review_action = admin_role.actions.get(action='review_rerequest')
        expected_message = "Перезапрос роли в связи с регулярным пересмотром"
        assert review_action.comment == expected_message
        assert admin_role.expire_at is not None


def test_not_granted_are_not_reviewed(simple_system, arda_users):
    """Проверим, что роли в любых состояниях, кроме 'granted', не пересматриваются"""

    frodo = arda_users.frodo
    set_workflow(simple_system, 'approvers = ["legolas"]')
    for state in ROLE_STATE.ALL_STATES:
        Role.objects.all().delete()
        role = raw_make_role(frodo, simple_system, {'role': 'admin'}, state=state)
        with freeze_time(timezone.now() + timedelta(days=settings.IDM_DEFAULT_REVIEW_ROLES_DAYS)):
            call_command('idm_review_roles')
        role = refresh(role)
        if state == 'granted':
            assert role.state == 'review_request'
        else:
            assert role.state == state


def test_previous_requester_does_not_affect_review(simple_system, arda_users):
    """Проверим, что если человек, запросивший роль, находился в списке подтверждающих, при пересмотре данный факт
    не должен быть учтён"""

    frodo = arda_users.frodo
    legolas = arda_users.legolas
    set_workflow(simple_system, 'approvers = ["frodo"]')

    role = Role.objects.request_role(frodo, legolas, simple_system, '', {'role': 'admin'}, None)
    role = refresh(role)
    assert role.state == 'granted'
    with freeze_time(timezone.now() + timedelta(settings.IDM_DEFAULT_REVIEW_ROLES_DAYS + 1)):
        call_command('idm_review_roles')

    role = refresh(role)
    assert_action_chain(role, [
        'request', 'apply_workflow', 'approve', 'first_add_role_push', 'grant', 'review_rerequest', 'apply_workflow',
    ])

    assert role.state == 'review_request'
    assert role.requests.count() == 2
    role_request = role.get_open_request()
    assert role_request.is_done is False
    request = role_request.approves.get().requests.get()
    assert request.approver_id == frodo.id
    assert request.approved is None
    assert request.decision == ''


@pytest.mark.parametrize('user_type', ['robot', 'tvm'])
def test_robot_roles_are_not_reviewed(simple_system, arda_users, robot_gollum, user_type):
    """Проверим, что роли роботов и TVM-приложений не пересматриваются"""
    simple_system.use_tvm_role = True
    simple_system.save()

    frodo = arda_users.frodo
    user_without_review = robot_gollum if user_type == 'robot' else arda_users.tvm_app

    human_role = Role.objects.request_role(frodo, frodo, simple_system, '', {'role': 'admin'}, None)
    user_without_review_role = Role.objects.request_role(
        user_without_review, user_without_review, simple_system, '', {'role': 'admin'}, None
    )

    set_workflow(simple_system, "approvers = [approver('gandalf')]")
    clear_mailbox()

    # вызываем команду регулярного пересмотра ролей
    with freeze_time(timezone.now() + timedelta(settings.IDM_DEFAULT_REVIEW_ROLES_DAYS + 1)):
        call_command('idm_review_roles')

    human_role.refresh_from_db()
    user_without_review_role.refresh_from_db()
    assert human_role.state == 'review_request'
    assert_action_chain(human_role, [
        'request', 'apply_workflow', 'approve', 'first_add_role_push', 'grant', 'review_rerequest', 'apply_workflow',
    ])
    assert user_without_review_role.state == 'granted'
    assert_action_chain(user_without_review_role, ['request', 'apply_workflow', 'approve', 'first_add_role_push', 'grant'])


def test_review_roles_exceeding_threshold(arda_users, simple_system):
    constance.config.IDM_REVIEW_ROLES_PER_SYSTEM_DEFAULT_THRESHOLD = 0

    frodo = arda_users.get('frodo')
    frodo_manager = make_role(frodo, simple_system, {'role': 'manager'})

    # вызываем команду регулярного пересмотра ролей
    with freeze_time(timezone.now() + timedelta(settings.IDM_DEFAULT_REVIEW_ROLES_DAYS + 1)):
        call_command('idm_review_roles')

    frodo_manager = refresh(frodo_manager)
    # Проверяем что роль не перезапрошена, потому что превышен лимит пересмотров ролей
    assert frodo_manager.requests.count() == 0

    cached_data = ReviewRolesThresholdExceededMetric.get()
    assert cached_data == {simple_system.slug: 1}


def test_review_roles_at_holiday(calendar_get_holidays_mock: mock.MagicMock):
    approver = create_user()
    # иначе роль после перезапроса автоматом подтверждается
    system = create_system(workflow=f'approvers= ["{approver.username}"]')
    user = create_user()
    role = raw_make_role(user, system, system.nodes.last().data)
    assert role.state == ROLE_STATE.GRANTED

    # вызываем команду регулярного пересмотра ролей
    fake_now = timezone.now() + timedelta(settings.IDM_DEFAULT_REVIEW_ROLES_DAYS + 1)
    expiration_date = fake_now + timedelta(settings.REQUESTED_ROLE_TTL)
    calendar_get_holidays_mock.return_value = [
        expiration_date.date(), expiration_date.date() + timedelta(days=2)
    ]
    with freeze_time(fake_now):
        call_command('idm_review_roles')

        role.refresh_from_db()
        assert role.state == ROLE_STATE.REVIEW_REQUEST
        assert role.expire_at == (
                expiration_date + timedelta(days=1)
        ).replace(hour=15, minute=0, second=0, microsecond=0)

    calendar_get_holidays_mock.assert_called_once_with(expiration_date.date())


def test_review_roles_failed_workflow(simple_system, arda_users):
    frodo = arda_users.get('frodo')
    frodo_manager = make_role(frodo, simple_system, {'role': 'manager'})

    set_workflow(simple_system, 'raise Exception("Some workflow error")')

    # вызываем команду регулярного пересмотра ролей
    fake_now = timezone.now() + timedelta(settings.IDM_DEFAULT_REVIEW_ROLES_DAYS + 1)
    with freeze_time(fake_now):
        call_command('idm_review_roles')

    frodo_manager = refresh(frodo_manager)

    system = frodo_manager.system
    assert system.metainfo.roles_failed_on_last_review is not None
    assert frodo_manager.pk in system.metainfo.roles_failed_on_last_review

    created_action = Action.objects.select_related('role').get(action='review_role_error')
    assert created_action is not None
    assert created_action.role.id == frodo_manager.id
    assert created_action.data['comment'] == 'Не удалось пересмотреть роль'


def test_review_roles_fixed_failed_workflow(simple_system, arda_users):
    frodo = arda_users.get('frodo')
    frodo_manager = make_role(frodo, simple_system, {'role': 'manager'})
    simple_system.metainfo.roles_failed_on_last_review = [frodo_manager.id]
    simple_system.metainfo.save(update_fields=['roles_failed_on_last_review'])

    set_workflow(simple_system, "approvers = [approver('gandalf')]")

    # вызываем команду регулярного пересмотра ролей
    fake_now = timezone.now() + timedelta(settings.IDM_DEFAULT_REVIEW_ROLES_DAYS + 1)
    with freeze_time(fake_now):
        call_command('idm_review_roles')

    frodo_manager = refresh(frodo_manager)

    system = frodo_manager.system
    assert system.metainfo.roles_failed_on_last_review is None


def test_review_empty_roles_after_failed_workflow(simple_system, arda_users):
    fake_role_id = 1
    simple_system.metainfo.roles_failed_on_last_review = [fake_role_id]
    simple_system.metainfo.save(update_fields=['roles_failed_on_last_review'])

    # вызываем команду регулярного пересмотра ролей
    fake_now = timezone.now() + timedelta(settings.IDM_DEFAULT_REVIEW_ROLES_DAYS + 1)
    with freeze_time(fake_now):
        call_command('idm_review_roles')

    simple_system.refresh_from_db()
    assert simple_system.metainfo.roles_failed_on_last_review is None
