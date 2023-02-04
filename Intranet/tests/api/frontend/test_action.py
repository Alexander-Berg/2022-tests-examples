# coding: utf-8


import csv
import io
import re
import zipfile

import pytest
from django.core import mail
from django.core.management import call_command
from django.db.models import Max
from django.utils.encoding import force_text
from mock import patch

from idm.core.constants.system import SYSTEM_GROUP_POLICY
from idm.core.models import Role, Action, RoleField, Transfer, GroupMembershipSystemRelation
from idm.inconsistencies.models import Inconsistency
from idm.tests.utils import (refresh, set_workflow, add_perms_by_role, make_role, mock_all_roles,
                             assert_contains, clear_mailbox, raw_make_role, change_department)
from idm.users.models import Group, GroupMembership
from idm.utils import reverse
from idm.tests.utils import attrdict, mock_group_memberships


pytestmark = [pytest.mark.django_db, pytest.mark.robot]


@pytest.fixture
def actions_url():
    return reverse('api_dispatch_list', api_name='frontend', resource_name='actions')


@pytest.fixture
def actions_for_report(client, simple_system, pt1_system, arda_users):
    frodo = arda_users.frodo
    gandalf = arda_users.gandalf
    legolas = arda_users.legolas
    simple_system.request_policy = 'anyone'
    simple_system.save()
    RoleField.objects.filter(node__system=pt1_system).update(is_active=False)

    Action.objects.all().delete()  # избавимся от созданных фикстурами Action-ов
    add_perms_by_role('superuser', gandalf)
    client.login('gandalf')

    role_active = Role.objects.request_role(legolas, frodo, simple_system, '', {'role': 'manager'})
    role_inactive = Role.objects.request_role(frodo, frodo, pt1_system, '', {'project': 'proj1', 'role': 'manager'})
    role_inactive.refresh_from_db()
    role_inactive.deprive_or_decline(gandalf)
    role_inactive.refresh_from_db()
    clear_mailbox()

    active = attrdict({action.action: action for action in role_active.actions.all()})
    inactive = attrdict({action.action: action for action in role_inactive.actions.all()})

    return attrdict({
        'active': active,
        'inactive': inactive
    })


def test_get_role_history(client, simple_system, actions_url, arda_users, idm_robot):
    """
    GET /frontend/actions?role=<role_id>
    """
    gandalf = arda_users.gandalf
    legolas = arda_users.legolas

    set_workflow(simple_system, 'approvers = ["varda"]')

    # role granted to gandalf
    add_perms_by_role('responsible', legolas, simple_system)
    superuser_role = Role.objects.request_role(legolas, gandalf, simple_system, None, {'role': 'superuser'}, None)

    # approve role request
    approve_request = superuser_role.requests.get().approves.get().requests.get()
    approve_request.fetch_approver()
    approve_request.set_approved(approve_request.approver)
    # deprive role
    refresh(superuser_role).deprive_or_decline(gandalf)

    client.login('gandalf')
    # get role history
    data = client.json.get(actions_url, {'role': superuser_role.id}).json()
    assert data['meta']['total_count'] == 8
    assert [item['action'] for item in data['objects']] == [
        'remove', 'first_remove_role_push', 'deprive', 'grant',
        'first_add_role_push', 'approve', 'apply_workflow', 'request',
    ]
    expected_users = ['robot-idm', 'robot-idm', 'gandalf', 'robot-idm', 'robot-idm', 'varda', 'robot-idm', 'legolas']
    assert [item['requester']['username'] for item in data['objects']] == expected_users
    assert str(superuser_role.id) in data['objects'][0]['role']
    item = data['objects'][0]
    assert set(item.keys()) == {
        'action', 'added', 'approverequest', 'comment', 'data', 'error', 'group', 'human', 'human_noun', 'human_verb',
        'id', 'impersonator', 'inconsistency', 'membership', 'ref_count', 'requester',
        'responsibility', 'role', 'role_alias', 'role_field', 'role_node', 'rolerequest', 'system', 'user', 'workflow'
    }


def test_report_actions(client, actions_for_report, actions_url):
    actions = actions_for_report

    client.login('legolas')
    data = client.json.get(actions_url).json()
    assert len(data['objects']) == 5

    client.login('gandalf')
    data = client.json.get(actions_url).json()
    assert len(data['objects']) == 13

    data = client.json.get(actions_url, {'is_active': 'false'}).json()
    assert {action['id'] for action in data['objects']} == {
        actions.inactive.request.id, actions.inactive.approve.id, actions.inactive.grant.id,
        actions.inactive.deprive.id, actions.inactive.remove.id, actions.inactive.apply_workflow.id,
        actions.inactive.first_add_role_push.id, actions.inactive.first_remove_role_push.id,
    }

    data = client.json.get(actions_url, {'action': 'grant'}).json()
    assert {action['id'] for action in data['objects']} == {actions.active.grant.id, actions.inactive.grant.id}

    data = client.json.get(actions_url, {'action': 'grant,deprive'}).json()
    assert len(data['objects']) == 3

    data = client.json.get(actions_url, {'role': actions.inactive.grant.role_id}).json()
    assert {action['id'] for action in data['objects']} == {
        actions.inactive.request.id, actions.inactive.approve.id, actions.inactive.grant.id,
        actions.inactive.deprive.id, actions.inactive.remove.id, actions.inactive.apply_workflow.id,
        actions.inactive.first_add_role_push.id, actions.inactive.first_remove_role_push.id,
    }

    data = client.json.get(actions_url, {'system': 'simple'}).json()
    assert {action['id'] for action in data['objects']} == {actions.active.request.id, actions.active.approve.id,
                                                            actions.active.grant.id, actions.active.apply_workflow.id}

    data = client.json.get(actions_url, {'user': 'legolas,varda'}).json()
    assert {action['id'] for action in data['objects']} == {actions.active.request.id}


def test_report_actions_post(client, actions_for_report, actions_url):
    client.login('gandalf')

    data = client.json.post(actions_url, {}).json()
    assert data == {
        'error_code': 'BAD_REQUEST',
        'message': 'Invalid data sent',
        'errors': {
            'format': ['Обязательное поле.']
        }
    }

    client.login('legolas')
    data = client.json.post(actions_url, {'format': 'csv'}).json()
    assert data == {
        'message': 'Отчёт формируется и будет отправлен вам на почту по завершении'
    }
    assert len(mail.outbox) == 1
    message = mail.outbox[0]
    assert message.to == ['legolas@example.yandex.ru']
    assert message.subject == 'Отчёт сформирован.'

    attachments = message.attachments
    assert len(attachments) == 1

    filename, data, content_type = attachments[0]
    assert re.match(r'^report_\d+_\d+\.csv\.zip', filename)
    assert content_type == 'application/zip'
    input_zip = io.BytesIO(data)
    input_zip = zipfile.ZipFile(input_zip)
    assert len(input_zip.namelist()) == 1
    zipped_filename = input_zip.namelist()[0]
    assert re.match(r'^report_\d+_\d+\.csv', zipped_filename)
    report_text = input_zip.read(zipped_filename).decode('utf8')
    assert report_text.count('\n') == 8
    lines = report_text.splitlines()
    header, emptyline, line1, line2, line3, line4, line5, line6 = lines
    assert header == (
        'Когда;Сотрудник;Кто (логин);Действие;Система;Роль;Владелец роли;Данные роли;'
        'Комментарий;Дополнительная информация'
    )
    assert emptyline == ''
    # отрежем время
    assert line1[17:] == 'legolas;legolas;запрос роли;Simple система;Роль: Менеджер;Фродо Бэггинс (frodo);;;'
    assert line2[17:] == 'Робот;idm;применение воркфлоу;Simple система;Роль: Менеджер;Фродо Бэггинс (frodo);;Подтверждающие: [];'
    assert line3[17:] == 'Робот;idm;подтверждение роли;Simple система;Роль: Менеджер;Фродо Бэггинс (frodo);;;'
    assert line4[17:] == 'Робот;idm;первая попытка добавить роль в систему;Simple система;Роль: Менеджер;Фродо Бэггинс (frodo);;;'
    assert line5[17:] == (
        'Робот;idm;выдача роли;Simple система;Роль: Менеджер;Фродо Бэггинс (frodo);;'
        'Роль подтверждена и добавлена в систему.;'
    )
    assert line6[17:] == 'legolas;legolas;заказан отчет;;;;;;'


def test_report_workflow_actions(client, arda_users, simple_system, actions_url):
    """Проверим выгрузку действий по изменению workflow"""

    frodo = arda_users.frodo
    legolas = arda_users.legolas

    add_perms_by_role('responsible', frodo, simple_system)
    add_perms_by_role('responsible', legolas, simple_system)
    set_workflow(simple_system, 'approvers = []')
    Action.objects.all().delete()
    simple_system = refresh(simple_system)

    new_wf = simple_system.clone_workflow(frodo)
    new_wf.edit('approvers = ["legolas"]', 'approvers = ["varda"]', 'Hello there')
    new_wf.fetch_system()
    new_wf.system.fetch_actual_workflow()
    new_wf.commit(frodo)
    new_wf.approve(legolas)

    client.login('frodo')

    data = client.json.post(actions_url, {'format': 'csv'}).json()
    assert data == {
        'message': 'Отчёт формируется и будет отправлен вам на почту по завершении'
    }

    assert len(mail.outbox) == 8
    message = mail.outbox[-1]
    assert message.to == ['frodo@example.yandex.ru']
    assert message.subject == 'Отчёт сформирован.'
    with zipfile.ZipFile(io.BytesIO(message.attachments[0][1])) as zfile:
        reader = csv.reader(map(force_text, zfile.open(zfile.namelist()[0]).readlines()), delimiter=';', quoting=csv.QUOTE_MINIMAL)
    next(reader)  # заголовок
    next(reader)  # пустая строка
    row1, row2 = reader
    # отрежем время
    assert ';'.join(row2[1:]) == 'Фродо Бэггинс;frodo;заказан отчет;;;;;;'
    assert ';'.join(row1[1:-1]) == 'Фродо Бэггинс;frodo;изменение workflow;Simple система;;;;'
    expected = '''
Изменил workflow: Фродо Бэггинс (frodo).
Подтвердил workflow: legolas (legolas).
Изменения пользовательского workflow:
@@ -1 +1 @@
-approvers = []
+approvers = ["legolas"]
Изменения группового workflow:
@@ -0,0 +1 @@
+approvers = ["varda"]
'''
    assert row1[-1] == expected


@patch('xlwt.ExcelMagic.MAX_ROW', 2)
def test_report_lot_of_actions(client, actions_for_report, actions_url):
    client.login('gandalf')
    data = client.json.post(actions_url, {'format': 'xls'}).json()
    assert data == {
        'message': 'Отчёт формируется и будет отправлен вам на почту по завершении'
    }
    assert len(mail.outbox) == 1
    message = mail.outbox[0]
    assert message.to == ['gandalf@example.yandex.ru']
    assert message.subject == 'Ошибка формирования отчёта.'
    assert_contains([
        'Количество строк в отчёте больше ограничения xls файла',
        'Ваш IDM'
    ], message.body)


@pytest.mark.robot
def test_inconsistency_their_side(client, simple_system, arda_users, actions_url):
    roles = [{
        'login': 'gandalf',
        'roles': [{'role': 'admin'}],
    }]
    with mock_all_roles(simple_system, roles):
        call_command('idm_check_and_resolve', check_only=True)
    inconsistency = Inconsistency.objects.select_related('our_role__node', 'system__actual_workflow').get()
    inconsistency.resolve()

    action = inconsistency.actions.latest('pk')

    client.login('gandalf')
    response = client.json.get(
        reverse('api_dispatch_detail', api_name='frontend', resource_name='actions', pk=action.id)
    )
    assert response.status_code == 200
    data = response.json()
    expected = (
        'У gandalf обнаружена роль "Роль: Админ" в системе Simple система, которая отсутствует в IDM.'
    )
    assert data['inconsistency_comment'] == expected


def test_actions_by_id(client, simple_system, actions_for_report, actions_url):
    actions = actions_for_report

    client.login('gandalf')

    data = client.json.get(actions_url, {'id': actions.inactive.grant.id}).json()
    assert {action['id'] for action in data['objects']} == {actions.inactive.grant.id}

    data = client.json.get(actions_url, {'id': Action.objects.all().aggregate(Max('id'))['id__max'] + 1}).json()
    assert len(data['objects']) == 0


def test_actions_by_system(client, simple_system, pt1_system, actions_for_report, actions_url):
    actions = actions_for_report

    client.login('gandalf')
    data = client.json.get(actions_url, {'system': 'simple'}).json()
    assert {action['id'] for action in data['objects']} == {actions.active.request.id, actions.active.approve.id,
                                                            actions.active.grant.id, actions.active.apply_workflow.id}

    actions.active.request.system = pt1_system
    actions.active.request.save()
    actions.active.approve.system = pt1_system
    actions.active.approve.save()
    actions.active.grant.system = pt1_system
    actions.active.grant.save()
    actions.active.apply_workflow.system = pt1_system
    actions.active.apply_workflow.save()
    data = client.json.get(actions_url, {'system': 'simple'}).json()
    assert len(data['objects']) == 0


def test_actions_by_user(client, actions_for_report, actions_url, idm_robot, arda_users):
    actions = actions_for_report

    client.login('gandalf')
    data = client.json.get(actions_url, {'user': 'legolas'}).json()
    assert {action['id'] for action in data['objects']} == {actions.active.request.id}

    data = client.json.get(actions_url, {'user': 'frodo'}).json()
    assert {action['id'] for action in data['objects']} == {
        action.id
        for action in set(set(actions.active.values()) | set(actions.inactive.values()))
        if action.user_id == arda_users.frodo.id
    }

    actions.inactive.deprive.fetch_requester()
    gandalf = actions.inactive.deprive.requester
    actions.inactive.deprive.user = None
    actions.inactive.deprive.save()
    data = client.json.get(actions_url, {'user': 'gandalf'}).json()
    assert len(data['objects']) == 1

    actions.inactive.deprive.requester = gandalf
    actions.inactive.deprive.save()
    data = client.json.get(actions_url, {'user': 'gandalf'}).json()
    assert len(data['objects']) == 1

    actions.inactive.deprive.requester = None
    actions.inactive.deprive.impersonator = gandalf
    actions.inactive.deprive.save()
    data = client.json.get(actions_url, {'user': 'gandalf'}).json()
    assert len(data['objects']) == 1

    actions.inactive.deprive.impersonator = None
    actions.inactive.deprive.save()
    data = client.json.get(actions_url, {'user': 'gandalf'}).json()
    assert len(data['objects']) == 0


def test_actions_by_path(client, actions_for_report, actions_url, arda_users, pt1_system):
    frodo = arda_users.frodo

    actions = actions_for_report
    client.login('gandalf')

    role_active_adm = Role.objects.request_role(frodo, frodo, pt1_system, '', {'project': 'proj1', 'role': 'admin'})
    role_active_adm = attrdict({action.action: action for action in role_active_adm.actions.all()})
    actions['active_adm'] = role_active_adm

    data = client.json.get(actions_url, {'system': 'test1', 'path': '/proj1/'}).json()
    assert {action['id'] for action in data['objects']} == {actions.inactive.request.id, actions.inactive.approve.id,
                                                            actions.inactive.grant.id, actions.inactive.deprive.id,
                                                            actions.inactive.remove.id, actions.inactive.apply_workflow.id,
                                                            actions.active_adm.request.id, actions.active_adm.apply_workflow.id,
                                                            actions.active_adm.approve.id, actions.active_adm.grant.id}

    data = client.json.get(actions_url, {'system': 'test1', 'path': '/proj1/manager/'}).json()
    assert {action['id'] for action in data['objects']} == {actions.inactive.request.id, actions.inactive.approve.id,
                                                            actions.inactive.grant.id, actions.inactive.deprive.id,
                                                            actions.inactive.remove.id, actions.inactive.apply_workflow.id}

    data = client.json.get(actions_url, {'system': 'test1', 'path': '/proj1/admin/'}).json()
    assert {action['id'] for action in data['objects']} == {actions.active_adm.request.id, actions.active_adm.apply_workflow.id,
                                                            actions.active_adm.approve.id, actions.active_adm.grant.id}

    data = client.json.get(actions_url, {'system': 'simple', 'path': '/manager/'}).json()
    assert {action['id'] for action in data['objects']} == {actions.active.request.id, actions.active.approve.id,
                                                            actions.active.grant.id, actions.active.apply_workflow.id}


def test_system_actions_by_resp(client, actions_for_report, actions_url, arda_users, pt1_system):
    meriadoc = arda_users.meriadoc

    add_perms_by_role('responsible', meriadoc, pt1_system)
    data = client.json.get(actions_url, {'system': 'test1'}).json()
    assert len(data['objects']) == 6

    pt1_system.is_broken = True
    pt1_system.save()
    data = client.json.get(actions_url, {'system': 'test1'}).json()
    assert len(data['objects']) == 7


def test_actions_by_group(client, actions_for_report, actions_url, group_roots):
    actions = actions_for_report

    group = Group.objects.create(
        parent=Group.objects.get_root('service'),
        type='service',
        name='group',
        slug='group',
        external_id=1
    )

    client.login('gandalf')

    data = client.json.get(actions_url, {'group': 1}).json()
    assert len(data['objects']) == 0

    actions.active.grant.group = group
    actions.active.grant.save()
    data = client.json.get(actions_url, {'group': 1}).json()
    assert len(data['objects']) == 1


@pytest.mark.parametrize('requester_sex', ['M', 'F'])
@pytest.mark.parametrize('impersonator_sex', ['M', 'F', None])
def test_actions_humanization(client, simple_system, arda_users, requester_sex, impersonator_sex):
    actions_url = reverse('api_dispatch_list', api_name='v1', resource_name='actions')
    request_url = reverse('api_dispatch_list', api_name='v1', resource_name='rolerequests')
    varda = arda_users.varda
    varda.sex = requester_sex
    varda.save()
    manve = arda_users.manve

    if impersonator_sex:
        manve.sex = impersonator_sex
        manve.save()
        add_perms_by_role('impersonator', manve, simple_system)

    expected = {
        'M': 'запросил',
        'F': 'запросила',
    }

    set_workflow(simple_system, 'approvers = ["gandalf"]')
    if impersonator_sex:
        client.login('manve')
        client.json.post(request_url, {
            'path': '/manager/',
            'system': 'simple',
            'user': 'frodo',
            '_requester': 'varda'
        })
    else:
        client.login('varda')
        client.json.post(request_url, {
            'path': '/manager/',
            'system': 'simple',
            'user': 'frodo',
        })

    client.login('varda')
    role = Role.objects.get()
    data = client.json.get(actions_url, {'role': role.pk}).json()
    if impersonator_sex:
        assert data['objects'][1]['human'] == expected[impersonator_sex]
    else:
        assert data['objects'][1]['human'] == expected[requester_sex]


@pytest.mark.parametrize('api_name', ['frontend', 'v1'])
def test_change_department(client, simple_system, arda_users, department_structure, api_name):
    """Тестирование смены подразделения пользователем"""

    gandalf = arda_users.gandalf
    fellowship = department_structure.fellowship
    valinor = department_structure.valinor
    set_workflow(simple_system, 'approvers = [approver("legolas")]')
    gandalf_role = raw_make_role(gandalf, simple_system, {'role': 'admin'}, state='granted')
    actions_url = reverse('api_dispatch_list', api_name=api_name, resource_name='actions')

    # при переходе сотрудника в другой отдел роли должны переходить в need_request
    change_department(gandalf, fellowship, valinor)
    assert refresh(gandalf_role).state == 'granted'
    transfer = Transfer.objects.select_related('user').get()
    transfer.accept(bypass_checks=True)
    assert refresh(gandalf_role).state == 'need_request'

    expected_comment = (
        'Необходимо перезапросить роль в связи со сменой подразделения '
        'с "/Средиземье/Объединения/Братство кольца/" на "/Валинор/"'
    )

    # проверим, что комментарий видно так же и в API
    client.login('gandalf')
    history = client.json.get(actions_url, {'role': gandalf_role.pk}).json()
    assert len(history['objects']) == 1
    action_data = history['objects'][0]
    assert action_data['comment'] == expected_comment


def test_get_groupmembership_inconsistencies_actions(simple_system, department_structure, arda_users, client):
    simple_system.group_policy = SYSTEM_GROUP_POLICY.AWARE_OF_MEMBERSHIPS_WITH_LOGINS
    simple_system.push_batch_size = 3
    simple_system.save()
    group_ids = [department_structure.fellowship.id, department_structure.shire.id, department_structure.valinor.id]
    membership_ids = GroupMembership.objects.filter(group_id__in=group_ids, state='active').values_list('id', flat=True)
    GroupMembershipSystemRelation.objects.bulk_create_groupmembership_system_relations(membership_ids, simple_system)
    GroupMembershipSystemRelation.objects.update(state='activated')

    with patch.object(simple_system.plugin.__class__, 'add_group_membership'):
        with patch.object(simple_system.plugin.__class__, 'remove_group_membership'):
            with mock_group_memberships(simple_system, []):
                call_command('idm_process_groupmembership_inconsistencies')

    actions_url = reverse('api_dispatch_list', api_name='frontend', resource_name='actions')
    add_perms_by_role('superuser', arda_users.gandalf)
    client.login('gandalf')
    response = client.json.get(actions_url)
    assert response.status_code == 200
    data = response.json()
    assert len(data['objects']) == Action.objects.count()
