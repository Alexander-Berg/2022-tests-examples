# coding: utf-8


import pytest
from django.core import mail

from idm.core.workflow.exceptions import Forbidden, InactiveSystemError
from idm.core.exceptions import RoleStateSwitchError
from idm.core.models import Role, ApproveRequest, Approve, RoleRequest
from idm.notification.models import Notice
from idm.tests.utils import (set_workflow, refresh, clear_mailbox,
                             assert_action_chain, assert_contains, add_perms_by_role)

pytestmark = pytest.mark.django_db


def test_get_role_state_if_there_are_three_and_approvers(simple_system, arda_users):
    """Проверяем изменение статуса роли пользователя, когда все 3 аппрувера должны подтвердить роль
    или отклонить её. Вариант, где все подтверждают роль"""

    set_workflow(simple_system, 'approvers = [approver("legolas"), approver("gandalf"), approver("varda")]')

    frodo = arda_users.frodo
    role = Role.objects.request_role(frodo, frodo, simple_system, '', {'role': 'admin'}, None)

    assert Approve.objects.count() == 3

    role = refresh(role)
    assert role.requests.count() == 1  # запрос создан, но не отработал
    assert role.get_last_request().approves.count() == 3
    assert not role.get_last_request().is_done

    # approved, approved, approved
    approve_requests = ApproveRequest.objects.select_related_for_set_decided()
    approve_requests.get(approver=arda_users.legolas).set_approved(arda_users.legolas)
    role = refresh(role)
    assert role.state == 'requested'

    approve_requests.get(approver=arda_users.gandalf).set_approved(arda_users.gandalf)
    role = refresh(role)
    assert role.state == 'requested'

    approve_requests.get(approver=arda_users.varda).set_approved(arda_users.varda)
    role = refresh(role)
    assert role.state == 'granted'
    assert_action_chain(role, [
        'request', 'apply_workflow', 'approve', 'approve', 'approve', 'first_add_role_push', 'grant',
    ])


def test_get_role_state_if_there_are_three_and_approvers_one_declines(simple_system, arda_users):
    """Проверяем изменение статуса роли пользователя, когда все 3 аппрувера должны подтвердить роль
    или отклонить её. Вариант, где кто-нибудь отклоняет роль уже после подтверждения другим"""
    set_workflow(simple_system, 'approvers = [approver("legolas"), approver("gandalf"), approver("varda")]')

    frodo = arda_users.frodo
    role = Role.objects.request_role(frodo, frodo, simple_system, '', {'role': 'admin'}, None)

    assert Approve.objects.filter(role_request__role=role).count() == 3

    role = refresh(role)
    assert role.requests.count() == 1  # запрос не отработал
    assert role.get_last_request().approves.count() == 3
    assert not role.get_last_request().is_done

    # approved, declined, NA
    approve_requests = ApproveRequest.objects.select_related_for_set_decided()
    approve_requests.get(approver=arda_users.legolas).set_approved(arda_users.legolas)
    role = refresh(role)
    assert role.state == 'requested'

    approve_requests.get(approver=arda_users.varda).set_declined(arda_users.varda)
    role = refresh(role)
    assert role.state == 'declined'
    assert_action_chain(role, ['request', 'apply_workflow', 'approve', 'decline'])


def test_requester_is_notified_about_role_decline(simple_system, arda_users):
    """Проверяем оповещения для запросившего роль о том, что в её выдаче отказано
    в случае, если запросивший не совпадает с владельцем"""

    frodo = arda_users.frodo
    add_perms_by_role('responsible', frodo, simple_system)
    legolas = arda_users.legolas

    set_workflow(simple_system, 'approvers = ["varda"]')
    role = Role.objects.request_role(frodo, legolas, simple_system, '', {'role': 'admin'}, None)
    clear_mailbox()
    request = ApproveRequest.objects.select_related_for_set_decided().get()
    request.fetch_approver()
    request.set_declined(request.approver, comment='Just no.')
    role = refresh(role)
    assert role.state == 'declined'
    assert len(mail.outbox) == 2
    requester_mail, subject_mail = mail.outbox
    assert requester_mail.to == ['frodo@example.yandex.ru']
    assert requester_mail.subject == 'Запрошенная вами роль в системе "Simple система" отклонена'
    assert requester_mail.cc == []
    assert_contains((
        'varda отказал в выдаче запрошенной вами для пользователя legolas роли в системе "Simple система":',
        'Роль: Админ',
        'Комментарий: Just no.'
    ), requester_mail.body)
    assert subject_mail.to == ['legolas@example.yandex.ru']
    assert subject_mail.subject == 'Simple система. Заявка на роль отклонена'
    assert_contains((
        'varda отклонил запрос роли в системе "Simple система" для пользователя legolas:',
        'Роль: Админ',
        'Комментарий: Just no.'
    ), subject_mail.body)


def test_requester_is_not_notified_about_group_role_decline_if_he_is_responsible(simple_system, arda_users,
                                                                                 department_structure):
    """Проверяем оповещения для запросившего роль о том, что в её выдаче отказано
    в случае, если запросивший не совпадает с владельцем, а владелец - группа"""

    frodo = arda_users.frodo
    fellowship = department_structure.fellowship
    add_perms_by_role('responsible', frodo, simple_system)

    set_workflow(simple_system, group_code='approvers = ["varda"]')
    role = Role.objects.request_role(frodo, fellowship, simple_system, '', {'role': 'admin'}, None)
    clear_mailbox()
    request = ApproveRequest.objects.select_related_for_set_decided().get()
    request.fetch_approver()
    request.set_declined(request.approver, comment='Just no.')
    role = refresh(role)
    assert role.state == 'declined'
    assert len(mail.outbox) == 1
    subject_mail, = mail.outbox
    assert subject_mail.to == ['frodo@example.yandex.ru']
    assert subject_mail.subject == 'Simple система. Заявка на роль отклонена'
    assert_contains((
        'varda отклонил запрос роли в системе "Simple система" на группу "Братство кольца", '
        'в которой вы являетесь ответственным:',
        'Роль: Админ',
        'Комментарий: Just no.'
    ), subject_mail.body)


def test_requester_is_notified_about_group_role_decline(simple_system, arda_users, department_structure):
    """Проверяем оповещения для запросившего роль о том, что в её выдаче отказано
    в случае, если запросивший не совпадает с владельцем, владелец - группа, и запросивший - не один из её владельцев"""

    legolas = arda_users.legolas
    fellowship = department_structure.fellowship
    add_perms_by_role('responsible', legolas, simple_system)

    set_workflow(simple_system, group_code='approvers = ["varda"]')
    role = Role.objects.request_role(legolas, fellowship, simple_system, '', {'role': 'admin'}, None)
    clear_mailbox()

    request = ApproveRequest.objects.select_related_for_set_decided().get()
    request.fetch_approver()
    request.set_declined(request.approver, comment='Just no.')
    role = refresh(role)
    assert role.state == 'declined'
    assert len(mail.outbox) == 2
    requester_mail, subject_mail = mail.outbox
    assert requester_mail.to == ['legolas@example.yandex.ru']
    assert requester_mail.subject == 'Запрошенная вами роль в системе "Simple система" отклонена'
    assert requester_mail.cc == []
    assert_contains((
        'varda отказал в выдаче запрошенной вами для группы "Братство кольца" роли в системе "Simple система":',
        'Роль: Админ',
        'Комментарий: Just no.'
    ), requester_mail.body)
    assert subject_mail.to == ['frodo@example.yandex.ru']
    assert subject_mail.subject == 'Simple система. Заявка на роль отклонена'
    assert_contains((
        'varda отклонил запрос роли в системе "Simple система" на группу "Братство кольца", '
        'в которой вы являетесь ответственным:',
        'Роль: Админ',
        'Комментарий: Just no.'
    ), subject_mail.body)


def test_get_role_state_if_there_are_three_and_approvers_first_declines(simple_system, arda_users):
    """Проверяем изменение статуса роли пользователя, когда все 3 аппрувера должны подтвердить роль
    или отклонить её. Вариант, где кто-нибудь отклоняет роль сразу же"""

    set_workflow(simple_system, 'approvers = [approver("legolas"), approver("gandalf"), approver("varda")]')
    frodo = arda_users.frodo

    role = Role.objects.request_role(frodo, frodo, simple_system, '', {'role': 'admin'}, None)

    assert Approve.objects.filter(role_request__role=role).count() == 3

    role = refresh(role)
    assert role.requests.count() == 1  # запрос не отработал
    assert role.get_last_request().approves.count() == 3
    assert not role.get_last_request().is_done

    # declined, NA, NA
    approve_requests = ApproveRequest.objects.select_related_for_set_decided()
    approve_requests.get(approver=arda_users.varda).set_declined(arda_users.varda)
    role = refresh(role)
    assert role.state == 'declined'
    assert_action_chain(role, ['request', 'apply_workflow', 'decline'])


def test_get_role_state_if_there_are_three_or_approvers_one_approves(simple_system, arda_users):
    """Проверяем изменение статуса роли пользователя, когда любой из 3 аппруведов
    может подтвердить роль или отклонить её. Вариант, где кто-нибудь подтверждает роль сразу же"""

    set_workflow(simple_system, 'approvers = [approver("gandalf") | approver("legolas") | approver("varda")]')

    frodo = arda_users.frodo
    role = Role.objects.request_role(frodo, frodo, simple_system, '', {'role': 'admin'}, None)

    assert Approve.objects.filter(role_request__role=role).count() == 1

    role = refresh(role)
    assert role.requests.count() == 1  # запрос не отработал
    assert role.get_last_request().approves.count() == 1
    assert not role.get_last_request().is_done

    # approved, None, None
    ApproveRequest.objects.select_related_for_set_decided().get(approver=arda_users.legolas).set_approved(arda_users.legolas)
    role = refresh(role)
    assert role.state == 'granted'
    assert_action_chain(role, ['request', 'apply_workflow', 'approve', 'first_add_role_push', 'grant'])


def test_get_role_state_if_there_are_three_or_approvers_one_approves_one_declines(simple_system, arda_users):
    """Проверяем изменение статуса роли пользователя, когда любой из 3 аппруведов
    может подтвердить роль или отклонить её. Вариант, где кто-нибудь подтверждает роль,
    а кто-то другой отклоняет её. Роль при этом сначала выдаётся, а потом отзывается."""

    set_workflow(simple_system, 'approvers = [approver("gandalf") | approver("legolas") | approver("varda")]')
    frodo = arda_users.frodo

    role = Role.objects.request_role(frodo, frodo, simple_system, '', {'role': 'admin'}, None)

    assert Approve.objects.filter(role_request__role=role).count() == 1

    role = refresh(role)
    assert role.requests.count() == 1  # запрос не отработал
    assert role.get_last_request().approves.count() == 1
    assert not role.get_last_request().is_done

    # approved, deprived, NA
    apporove_requests = ApproveRequest.objects.select_related_for_set_decided()
    apporove_requests.get(approver=arda_users.gandalf).set_approved(arda_users.gandalf)
    role = refresh(role)
    assert role.state == 'granted'
    assert_action_chain(role, ['request', 'apply_workflow', 'approve', 'first_add_role_push', 'grant'])

    # роль уже подтверждена сотрудником gandalf. varda против, и отзывает роль.
    apporove_requests.get(approver=arda_users.varda).set_declined(arda_users.varda)
    role = refresh(role)
    assert role.state == 'deprived'
    assert_action_chain(role, [
        'request', 'apply_workflow', 'approve', 'first_add_role_push', 'grant',
        'deprive', 'first_remove_role_push', 'remove',
    ])


def test_get_role_state_if_there_are_three_or_approvers_one_declines(simple_system, arda_users):
    """Проверяем изменение статуса роли пользователя, когда любой из 3 аппруведов
    может подтвердить роль или отклонить её. Вариант, где кто-нибудь сразу отклоняет роль."""

    set_workflow(simple_system, 'approvers = [approver("gandalf") | approver("legolas") | approver("varda")]')
    frodo = arda_users.frodo

    role = Role.objects.request_role(frodo, frodo, simple_system, '', {'role': 'admin'}, None)

    assert Approve.objects.filter(role_request__role=role).count() == 1

    role = refresh(role)
    assert role.requests.count() == 1  # запрос не отработал
    assert role.get_last_request().approves.count() == 1
    assert not role.get_last_request().is_done

    # declined, NA, NA
    apporove_requests = ApproveRequest.objects.select_related_for_set_decided().filter(approve__role_request__role=role).all()
    apporove_requests.get(approver=arda_users.legolas).set_declined(arda_users.legolas)
    role = refresh(role)
    assert role.state == 'declined'
    assert_action_chain(role, ['request', 'apply_workflow', 'decline'])


def test_get_role_state_and_or(simple_system, arda_users):
    """Проверяем изменение статуса роли пользователя комбинания аппрувов (and, or)
    рассматриваются комбинации подтверждений / отказов"""
    set_workflow(simple_system, 'approvers = [approver("legolas") | approver("sauron"), approver("gandalf")]')
    frodo = arda_users.frodo

    role = Role.objects.request_role(frodo, frodo, simple_system, '', {'role': 'admin'}, None)

    assert Approve.objects.filter(role_request__role=role).count() == 2

    role = refresh(role)
    assert role.requests.count() == 1
    assert role.get_last_request().approves.count() == 2
    assert not role.get_last_request().is_done

    approve_requests = ApproveRequest.objects.select_related_for_set_decided().filter(approve__role_request__role=role).all()
    # legolas подтвердил
    approve_requests.get(approver=arda_users.legolas).set_approved(arda_users.legolas)
    role = refresh(role)
    assert role.state == 'requested'
    assert_action_chain(role, ['request', 'apply_workflow', 'approve'])

    # gandalf подтвердил
    approve_requests.get(approver=arda_users.gandalf).set_approved(arda_users.gandalf)
    role = refresh(role)
    assert role.state == 'granted'
    assert_action_chain(role, ['request', 'apply_workflow', 'approve', 'approve', 'first_add_role_push', 'grant'])

    # но пришел sauron и всё испортил
    approve_requests.get(approver=arda_users.sauron).set_declined(arda_users.sauron)
    role = refresh(role)
    # отозвав роль
    assert role.state == 'deprived'
    assert_action_chain(role, [
        'request', 'apply_workflow', 'approve', 'approve', 'first_add_role_push', 'grant',
        'deprive', 'first_remove_role_push', 'remove',
    ])


def test_get_role_state_and_or2(simple_system, arda_users):
    """Проверяем изменение статуса роли пользователя комбинания аппрувов (and, or)
    рассматриваются комбинации подтверждений / отказов"""

    frodo = arda_users.frodo
    set_workflow(simple_system, 'approvers = [approver("sauron") | approver("legolas"), approver("gandalf")]')

    role = Role.objects.request_role(frodo, frodo, simple_system, '', {'role': 'admin'}, None)

    assert Approve.objects.filter(role_request__role=role).count() == 2

    role = refresh(role)
    assert role.requests.count() == 1  # запрос не отработал
    assert role.get_last_request().approves.count() == 2
    assert not role.get_last_request().is_done

    approve_requests = ApproveRequest.objects.select_related_for_set_decided().filter(approve__role_request__role=role).all()
    # gandalf подтвердил
    approve_requests.get(approver=arda_users.gandalf).set_approved(arda_users.gandalf)

    # sauron отклонил
    approve_requests.get(approver=arda_users.sauron).set_declined(arda_users.sauron)

    role = refresh(role)
    # и роль перешла в declined
    assert role.state == 'declined'
    assert_action_chain(role, ['request', 'apply_workflow', 'approve', 'decline'])

    # и если даже legolas придёт и попробует подтвердить, то ничего не изменится, так как такой переход невозможен
    with pytest.raises(RoleStateSwitchError):
        approve_requests.get(approver=arda_users.legolas).set_approved(arda_users.legolas)
    role = refresh(role)

    assert role.state == 'declined'
    assert_action_chain(role, ['request', 'apply_workflow', 'approve', 'decline'])


def test_rerequest_of_declined_role(simple_system, arda_users):
    """Проверяем возможность перезапросить роль после того, как ранее в её выдаче было отказано"""
    set_workflow(simple_system, 'approvers = [approver("sauron")]')
    frodo = arda_users.frodo

    role = Role.objects.request_role(frodo, frodo, simple_system, '', {'role': 'admin'}, None)

    role = refresh(role)
    assert role.requests.count() == 1  # запрос не отработал
    assert role.get_last_request().approves.count() == 1
    assert not role.get_last_request().is_done

    # sauron отклонил
    ApproveRequest.objects.select_related_for_set_decided().get(approver=arda_users.sauron).set_declined(arda_users.sauron)
    role = refresh(role)
    assert role.state == 'declined'
    assert not role.is_active

    # поговорили, решили: эта роль нужна - перезапрашиваем
    role = Role.objects.request_role(frodo, frodo, simple_system, '', {'role': 'admin'}, None)

    role = refresh(role)
    assert role.requests.count() == 1  # создается новый запрос уже для другой (перезапрошенной) роли
    assert role.get_last_request().approves.count() == 1
    assert not role.get_last_request().is_done
    assert RoleRequest.objects.count() == 2
    assert role.state == 'requested'
    assert not role.is_active

    ApproveRequest.objects.select_related_for_set_decided().get(approve__role_request__role=role,
                               approver=arda_users.sauron).set_approved(arda_users.sauron)
    role = refresh(role)
    assert role.state == 'granted'
    assert role.is_active


def test_request_role_for_approver_set_right_approver(simple_system, arda_users):
    """Тестируем добавление роли, когда запрашивающий и один из подтверждающих - одно лицо,
    должен выставиться правильный выдавший"""
    set_workflow(simple_system, 'approvers=[approver("frodo") | approver("legolas") | approver("sam")]')
    legolas = arda_users.legolas

    assert legolas.roles.count() == 0

    role = Role.objects.request_role(legolas, legolas, simple_system, '', {'role': 'admin'}, None)
    assert legolas.roles.count() == 1

    role = refresh(role)
    assert role.state == 'granted'
    assert role.is_active is True

    action = role.actions.select_related('approverequest__approver').get(action='approve')
    assert action.requester_id == legolas.id
    assert action.approverequest.approver == legolas


def test_approvers_are_both_owner_and_requester(simple_system, arda_users):
    """Тестируем добавление роли, когда среди подтверждающих есть и запрашивающий, и владелец"""
    add_perms_by_role('superuser', arda_users.varda)
    set_workflow(simple_system, 'approvers=[approver("legolas") | "frodo" | "varda" | "gandalf"]')

    role1 = Role.objects.request_role(arda_users.varda, arda_users.frodo, simple_system, '', {'role': 'admin'}, None)
    role1 = refresh(role1)
    assert role1.state == 'granted'
    assert_action_chain(role1, ['request', 'apply_workflow', 'approve', 'first_add_role_push', 'grant'])
    approve_action = role1.actions.select_related('approverequest__approver').get(action='approve')
    assert approve_action.requester_id == arda_users.frodo.id
    assert approve_action.approverequest.approver == arda_users.frodo

    # а теперь наоборот
    set_workflow(simple_system, 'approvers=[approver("legolas") | "varda" | "frodo" | "gandalf"]')
    role2 = Role.objects.request_role(arda_users.varda, arda_users.frodo, simple_system, '', {'role': 'manager'}, None)
    role2 = refresh(role2)
    assert role2.state == 'granted'
    assert_action_chain(role2, ['request', 'apply_workflow', 'approve', 'first_add_role_push', 'grant'])
    approve_action = role2.actions.select_related('approverequest__approver').get(action='approve')
    assert approve_action.requester_id == arda_users.varda.id
    assert approve_action.approverequest.approver == arda_users.varda


def test_request_role_for_approver_himself_with_complex_workflow(simple_system, arda_users):
    """Тестируем добавление роли, когда запрашивающий и один из подтверждающих - одно лицо"""

    set_workflow(simple_system, 'approvers=[approver("varda"), approver("gimli") | approver("frodo")]')
    frodo = arda_users.frodo
    legolas = arda_users.legolas

    clear_mailbox()
    role = Role.objects.request_role(frodo, legolas, simple_system, '', {'role': 'admin'}, None)

    assert legolas.roles.count() == 1
    role = refresh(role)
    assert role.state == 'requested'
    assert role.is_active is False

    assert role.requests.count() == 1  # запрос еще не отработал
    role_request = role.requests.get()
    assert role_request.approves.count() == 2
    assert role_request.is_done is False

    assert Approve.objects.count() == 2
    for ap in Approve.objects.select_related('role_request__requester'):
        assert frodo == ap.role_request.requester
        assert role_request == ap.role_request

    assert ApproveRequest.objects.count() == 3
    for ar in ApproveRequest.objects.filter(approve__role_request__role=role):
        if ar.approver_id == frodo.id:
            assert ar.approved is True
            assert ar.decision == 'approve'
        else:
            assert ar.approved is None
            assert ar.decision == ''

    first_approve, second_approve = Approve.objects.order_by('pk')
    assert first_approve.state == 'waiting'
    assert second_approve.state == 'approved'

    assert role.get_num_approves_left() == 1

    assert len(mail.outbox) == 2
    # письмо для подтверждения роли protos
    assert mail.outbox[0].to == ['varda@example.yandex.ru']
    assert mail.outbox[0].subject == 'Подтверждение роли. Simple система.'
    # письмо о оставшихся подтверждениях gamer
    assert mail.outbox[1].to == ['legolas@example.yandex.ru']
    assert mail.outbox[1].subject == 'Роль в системе "Simple система" требует подтверждения.'


def test_request_role_for_approver_himself_if_approver_is_in_several_or_groups(simple_system, arda_users):
    frodo = arda_users.frodo
    workflows = [
        "approvers = ['frodo']",
        "approvers = [approver('frodo') | 'sauron']",
        "approvers = [approver('sauron') | 'frodo']",
        "approvers = ['frodo', approver('frodo') | 'sauron']",
        "approvers = ['frodo', approver('sauron') | 'frodo']",
        "approvers = [approver('sauron') | 'frodo', 'frodo']",
    ]
    for workflow in workflows:
        set_workflow(simple_system, workflow)

        role = Role.objects.request_role(frodo, frodo, simple_system, '', {'role': 'admin'}, None)

        # всё автоматически выдано
        frodo = refresh(frodo)
        assert frodo.roles.count() == 1
        role = refresh(role)
        assert role.state == 'granted'
        assert role.is_active is True
        assert len(mail.outbox) == 1
        message = mail.outbox[0]
        assert message.subject == 'Simple система. Новая роль'

        clear_mailbox()
        role.delete()


def test_request_role_for_approver_himself(simple_system, arda_users):
    """Тестируем добавление роли, когда запрашивающий и подтверждающие - одно лицо"""

    frodo = arda_users.frodo
    legolas = arda_users.legolas

    set_workflow(simple_system, 'approvers=[approver("frodo")]')

    role = Role.objects.request_role(frodo, legolas, simple_system, '', {'role': 'admin'}, None)

    assert ApproveRequest.objects.count() == 1
    ar = ApproveRequest.objects.select_related_for_set_decided().get()
    assert ar.approver_id == frodo.id
    assert ar.approved is True
    assert ar.decision == 'approve'
    assert Approve.objects.count() == 1
    approve = Approve.objects.get()
    assert approve.state == 'approved'

    role = refresh(role)
    assert role.user_id == legolas.id
    assert role.state == 'granted'
    assert role.is_active

    # пользователь должен получить от нас письмо что добавлена роль
    assert len(mail.outbox) == 1
    assert mail.outbox[0].to == ['legolas@example.yandex.ru']


def test_twice_approve_deprive_by_differrent_approvers(simple_system, arda_users):
    """Тестирует подтверждение уже выданной роли и отзыв уже отозванного"""

    set_workflow(simple_system, 'approvers = [any_from(["legolas", "varda", "gimli", "gandalf"])]')
    frodo = arda_users.frodo

    def _test_role_granted(role):
        role = refresh(role)
        assert role.state == 'granted'
        assert role.is_active
        role_request = role.requests.get()
        assert role_request.is_done
        assert_action_chain(role, ['request', 'apply_workflow', 'approve', 'first_add_role_push', 'grant'])

    def _test_role_deprived(role):
        role = refresh(role)
        assert role.state == 'deprived'
        assert not role.is_active

        with pytest.raises(RoleRequest.DoesNotExist):
            role.get_open_request()
        assert_action_chain(role, [
            'request', 'apply_workflow', 'approve', 'first_add_role_push', 'grant',
            'deprive', 'first_remove_role_push', 'remove',
        ])

    # запросим роль
    role = Role.objects.request_role(frodo, frodo, simple_system, '', {'role': 'admin'}, None)

    role = refresh(role)
    assert role.state == 'requested'
    assert not role.is_active
    role_request = role.get_open_request()
    assert role_request.approves.count() == 1
    assert not role_request.is_done

    approve_requests = ApproveRequest.objects.select_related_for_set_decided()

    # подтвердим роль - она сразу будет выдана
    legolas_ar = approve_requests.get(approver=arda_users.legolas)
    legolas_ar.set_approved(arda_users.legolas)
    _test_role_granted(role)

    # подтвердим роль вторым аппрувером - ничего не изменится
    varda_ar = approve_requests.get(approver=arda_users.varda)
    varda_ar.set_approved(arda_users.varda)
    _test_role_granted(role)

    # попробуем отозвать роль от лица того пользователя, который уже подтвердил её - роль останется выданной
    # так как изменять свои решения нельзя
    varda_ar = refresh(varda_ar)
    varda_ar.set_declined(arda_users.varda)
    _test_role_granted(role)

    # отзовем роль одним аппрувером - роль удалится
    gimli_ar = approve_requests.get(approver=arda_users.gimli)
    gimli_ar.set_declined(arda_users.gimli)
    _test_role_deprived(role)

    # отзовем роль вторым аппрувером - роль уже удалена - ничего не изменится
    gandalf_ar = approve_requests.get(approver=arda_users.gandalf)
    gandalf_ar.set_declined(arda_users.gandalf)
    _test_role_deprived(role)

    role = refresh(role)
    with pytest.raises(Forbidden):  # нельзя отозвать отозванную роль
        role.deprive_or_decline(frodo)


def test_rerequest_role_with_approves(simple_system, arda_users):
    """Проверяет состояние роли при её перезапросе"""

    set_workflow(simple_system, 'approvers = []')

    frodo = arda_users.frodo
    role = Role.objects.request_role(frodo, frodo, simple_system, '', {'role': 'admin'}, None)

    role = refresh(role)
    assert role.state == 'granted'
    assert role.is_active
    assert role.requests.count() == 1
    assert role.requests.get().approves.count() == 0

    # переводим роль в состояние need_request
    role.set_state('need_request', comment='change department')
    role = refresh(role)
    assert role.state == 'need_request'
    assert role.is_active
    assert role.expire_at is not None
    assert role.requests.count() == 1
    assert role.get_num_approves_left() == 0

    # переводим роль в состояние rerequested, перезапросив её
    set_workflow(simple_system, 'approvers = [approver("legolas") | approver("sauron")]')
    role = Role.objects.select_related('system__actual_workflow', 'node').get(pk=role.pk)
    role.rerequest(frodo)

    role = refresh(role)
    assert role.state == 'rerequested'
    assert role.is_active
    assert role.expire_at is not None
    assert role.get_num_approves_left() == 1
    assert role.requests.count() == 2
    role_request = role.get_last_request()
    assert not role_request.is_done
    assert role_request.approves.count() == 1

    # legolas подтвердил роль
    ApproveRequest.objects.select_related_for_set_decided().get(approver=arda_users.legolas).set_approved(arda_users.legolas)

    role = refresh(role)
    assert role.state == 'granted'
    assert role.is_active
    assert role.expire_at is None
    assert role.get_num_approves_left() == 0

    assert role.requests.count() == 2

    with pytest.raises(RoleRequest.DoesNotExist):
        role.get_open_request()

    # повторим: переведем в need_request
    role.set_state('need_request', comment='change department again')
    role = refresh(role)
    assert role.state == 'need_request'
    assert role.is_active
    assert role.expire_at is not None
    assert role.requests.count() == 2
    assert role.get_num_approves_left() == 0

    # переводим роль в состояние rerequested, перезапросив её
    set_workflow(simple_system, 'approvers = [approver("legolas") | approver("varda")]')
    role = Role.objects.select_related('system__actual_workflow', 'node').get(pk=role.pk)
    role.rerequest(frodo)

    role = refresh(role)
    assert role.state == 'rerequested'
    assert role.is_active
    assert role.expire_at is not None
    assert role.get_num_approves_left() == 1

    assert role.requests.count() == 3
    role_request = role.get_last_request()
    assert not role_request.is_done
    assert role_request.approves.count() == 1


def test_double_approve_role(simple_system, arda_users):
    """Проверяем, что если дважды подтвердить один и тот же approve request, ничего такого не произойдёт"""

    set_workflow(simple_system, 'approvers = [approver("legolas")]')
    frodo = arda_users.frodo
    legolas = arda_users.legolas
    role = Role.objects.request_role(frodo, frodo, simple_system, '', {'role': 'admin'}, None)
    approve_request = ApproveRequest.objects.select_related_for_set_decided().get()
    approve_request.set_approved(legolas)
    approve_request.set_approved(legolas)
    approve_request = refresh(approve_request)
    approve_request.set_approved(legolas)
    approve_request = refresh(approve_request)
    role = refresh(role)
    assert_action_chain(role, ['request', 'apply_workflow', 'approve', 'first_add_role_push', 'grant'])
    assert approve_request.approved is True
    assert approve_request.decision == 'approve'


def test_approve_decline_role(simple_system, arda_users):
    """Проверяем, что если попробовать сначала отклонить, а потом подтвердить approve request,
    он останется отклонённым"""

    set_workflow(simple_system, 'approvers = [approver("legolas")]')
    frodo = arda_users.frodo
    legolas = arda_users.legolas
    role = Role.objects.request_role(frodo, frodo, simple_system, '', {'role': 'admin'}, None)
    approve_request = ApproveRequest.objects.select_related_for_set_decided().get()
    approve_request.set_declined(legolas)
    approve_request.set_approved(legolas)
    approve_request = refresh(approve_request)
    approve_request.set_approved(legolas)
    approve_request = refresh(approve_request)
    role = refresh(role)
    assert_action_chain(role, ['request', 'apply_workflow', 'decline'])
    assert approve_request.approved is False
    assert approve_request.decision == 'decline'


def test_approve_and_deprive_role_with_comment(simple_system, arda_users):
    """Проверяем комментарии аппруверов при подтверждении и отзыве роли"""

    set_workflow(simple_system, 'approvers = [approver("legolas") | approver("sauron")]')
    frodo = arda_users.frodo

    assert len(mail.outbox) == 0
    assert Notice.objects.count() == 0

    role = Role.objects.request_role(frodo, frodo, simple_system, '', {'role': 'admin'}, None)

    role = refresh(role)
    assert not role.is_active
    assert role.state == 'requested'
    assert len(mail.outbox) == 2

    # legolas подтверждает роль с комментарием
    approve_requests = ApproveRequest.objects.select_related_for_set_decided()
    approve_requests.get(approver=arda_users.legolas).set_approved(arda_users.legolas, 'because he is a human!')
    role = refresh(role)
    assert role.is_active
    assert role.state == 'granted'
    approve_action = role.actions.get(action='approve')
    assert approve_action.requester_id == arda_users.legolas.id
    assert approve_action.data['comment'] == 'because he is a human!'
    assert len(mail.outbox) == 3

    # sauron отзовет роль
    approve_requests.get(approver=arda_users.sauron).set_declined(arda_users.sauron, 'I object!')
    role = refresh(role)
    assert not role.is_active
    assert role.state == 'deprived'
    deprive_action = role.actions.get(action='deprive')
    assert deprive_action.requester_id == arda_users.sauron.id
    assert deprive_action.data['comment'] == 'I object!'

    assert len(mail.outbox) == 4
    assert ['frodo@example.yandex.ru'] == mail.outbox[3].to
    assert_contains(['Комментарий', 'I object'], mail.outbox[3].body)


def test_decline_role_with_comment(simple_system, arda_users):
    """Проверяем комментарии аппрувера при отказе выдачи роли"""
    set_workflow(simple_system, 'approvers = [approver("sauron")]')

    frodo = arda_users.frodo
    sauron = arda_users.sauron

    assert len(mail.outbox) == 0
    assert Notice.objects.count() == 0

    role = Role.objects.request_role(frodo, frodo, simple_system, '', {'role': 'admin'}, None)
    role = refresh(role)
    assert not role.is_active
    assert role.state == 'requested'
    assert len(mail.outbox) == 2

    # sauron отклонит роль
    ApproveRequest.objects.select_related_for_set_decided().get(approver=sauron).set_declined(sauron, 'sauron does not like people')

    role = refresh(role)
    assert not role.is_active
    assert role.state == 'declined'
    approve_action = role.actions.get(action='decline')
    assert approve_action.requester_id == sauron.id
    assert approve_action.comment == 'sauron does not like people'

    assert len(mail.outbox) == 3
    assert ['frodo@example.yandex.ru'] == mail.outbox[2].to
    assert_contains(['Комментарий', 'sauron does not like people'], mail.outbox[2].body)


def test_request_role_for_repeating_user_does_not_generate_double_mails(simple_system, arda_users):
    """Проверим, что повторяющийся в workflow подтверждающий получит только одно письмо"""

    legolas = arda_users.legolas
    frodo = arda_users.frodo

    assert len(mail.outbox) == 0

    workflow = "approvers = [approver('gimli') | 'varda', approver('saruman') | 'varda']"
    set_workflow(simple_system, workflow)

    role = Role.objects.request_role(frodo, legolas, simple_system, '', {'role': 'admin'}, None)
    role = refresh(role)
    legolas = refresh(legolas)

    assert legolas.roles.count() == 1
    assert role.state == 'requested'
    assert role.is_active is False
    assert ApproveRequest.objects.count() == 4
    assert set(ApproveRequest.objects.values_list('approver__username', flat=True)) == {'gimli', 'varda', 'saruman'}
    assert len(mail.outbox) == 3

    # письма уходят только первому из каждой OR-группы, поэтому varda не получит писем
    expected = [['gimli@example.yandex.ru'], ['saruman@example.yandex.ru'], ['legolas@example.yandex.ru']]
    assert [message.to for message in mail.outbox] == expected
    expected = ['Подтверждение роли. Simple система.',
                'Подтверждение роли. Simple система.',
                'Роль в системе "Simple система" требует подтверждения.']
    assert [message.subject for message in mail.outbox] == expected


def test_group_role_is_not_auto_approved_if_approver_of_group_role_is_responsible(simple_system, arda_users,
                                                                                  department_structure):
    """ Проверим, что роль не подтверждается автоматически, если ответственный за группу подтверждающий,
     но запрашивает при этом не он"""

    fellowship = department_structure.fellowship
    legolas = arda_users.legolas

    set_workflow(simple_system, group_code='approvers = ["frodo"]')
    # зададим политику в отношении права запроса роли, иначе у legolas не получится запросить роль
    simple_system.request_policy = 'anyone'
    simple_system.save()
    role = Role.objects.request_role(legolas, fellowship, simple_system, '', {'role': 'admin'}, None)
    role = refresh(role)
    assert role.state == 'requested'


def test_group_role_is_auto_approved_if_approver_of_group_role_is_requester(simple_system, arda_users,
                                                                            department_structure):
    """ Проверим, что роль автоматически подтверждается, если подтверждающий является ответственным за группу,
    и он при этом запрашивает роль"""

    fellowship = department_structure.fellowship
    frodo = arda_users.frodo
    set_workflow(simple_system, group_code='approvers = ["frodo"]')
    role = Role.objects.request_role(frodo, fellowship, simple_system, '', {'role': 'admin'}, None)
    role = refresh(role)
    assert role.state == 'granted'


def test_inactive_system(simple_system, arda_users):
    """Проверяем изменение статуса роли пользователя, когда все 3 аппрувера должны подтвердить роль
    или отклонить её. Вариант, где все подтверждают роль"""

    set_workflow(simple_system, 'approvers = [approver("legolas")]')

    frodo = arda_users.frodo
    role = Role.objects.request_role(frodo, frodo, simple_system, '', {'role': 'admin'}, None)
    role = refresh(role)
    assert role.requests.count() == 1  # запрос создан, но не отработал

    simple_system.is_active = False
    simple_system.save()
    with pytest.raises(InactiveSystemError):
        ApproveRequest.objects.select_related_for_set_decided().get(approver=arda_users.legolas).set_approved(arda_users.legolas)
