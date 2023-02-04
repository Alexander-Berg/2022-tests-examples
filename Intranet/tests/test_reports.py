# coding: utf-8


import io
import re
import zipfile
from textwrap import dedent

import mock
import pytest
from datetime import datetime
from django.core import mail
from django.utils.timezone import utc

from idm.core.models import Action, ApproveRequest, Role
from idm.reports.tasks import make_report
from idm.tests.utils import (assert_contains, clear_mailbox, raw_make_role, set_workflow, assert_num_queries,
                             assert_num_queries_lte)
from idm.utils import reverse

pytestmark = [pytest.mark.django_db]


def assert_csv_report(text, message, user, comment):
    assert message.to == [user.email]
    assert message.subject == 'Отчёт сформирован.'
    assert_contains([
        'Добрый день',
        'Отчёт сформирован и приложен к данному письму.',
        comment,
        'Ваш IDM'
    ], message.body)
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
    assert report_text.replace('\r\n', '\n').strip() == dedent(text).strip()


def test_approvers_column_with_parent_role(client, simple_system, arda_users, department_structure):
    requests_url = reverse('api_dispatch_list', api_name='frontend', resource_name='rolerequests')
    set_workflow(simple_system, 'approvers = ["legolas"]', group_code='approvers = ["legolas"]')
    client.login('frodo')
    legolas = arda_users.legolas
    frodo = arda_users.frodo
    fellowship = department_structure.fellowship

    # обычная роль
    result = client.json.post(requests_url, {
        'path': '/manager/',
        'system': 'simple',
        'user': 'frodo',
    }).json()
    role = Role.objects.get(id=result['id'])
    approve_request = role.get_last_request().get_approve_requests()[0]
    approve_request.set_approved(legolas)

    # связанная с групповой роль
    result = client.json.post(requests_url, {
        'path': '/manager/',
        'system': 'simple',
        'group': fellowship.external_id,
    }).json()
    group_role = Role.objects.get(id=result['id'])
    approve_request = group_role.get_last_request().get_approve_requests()[0]
    approve_request.set_approved(legolas)

    ApproveRequest.objects.update(updated=datetime(1970, 1, 1, 0, 0, 0, tzinfo=utc))
    Role.objects.filter(state='granted').update(granted_at=datetime(1970, 1, 1, 0, 0, 0, tzinfo=utc))

    clear_mailbox()

    comment = 'Хочу увидеть всё, что скрыто'
    with assert_num_queries_lte(14):
        # выборка user-a
        # создание action-а
        # выборка количества
        # выборка ролей
        # префетч approve (2: своих и родительской роли)
        # префетч approverequest (2: своих и родительской роли)
        # префетч пользователей-подтверждающих из approverequest (2: своих и родительской роли)
        # -- отправка сообщения:
        # выборка сайта не происходит из-за кеширования
        # выборка шаблона (2 раза)
        # создание notice
        # выборка шаблона (2 раза)
        make_report(Role.objects.filter(user=frodo).reports_related(), 'csv', 'roles', frodo, comment)

    assert Action.objects.filter(action='report_requested').count() == 1
    action = Action.objects.get(action='report_requested')
    assert action.user_id == frodo.id

    assert len(mail.outbox) == 1
    message = mail.outbox[0]

    assert_csv_report(
        """
            Сотрудник;Логин;Тип группы;Должность;Отдел;Система;Роль;Код роли;Доп. данные;Состояние;Выдана;Подтвердили

            Фродо Бэггинс;frodo;-;;Братство кольца;Simple система;Роль: Менеджер;"{""role"": ""manager""}";;Выдана;1970-01-01 03:00;legolas: 1970-01-01 03:00:00+03:00
            Фродо Бэггинс;frodo;-;;Братство кольца;Simple система;Роль: Менеджер;"{""role"": ""manager""}";;Выдана;1970-01-01 03:00;выдана как связанная, legolas: 1970-01-01 03:00:00+03:00

        """,
        message, frodo, comment
    )


def test_user_roles_csv_report(simple_system, arda_users, department_structure):
    set_workflow(simple_system, 'approvers = ["legolas"]')
    fellowship = department_structure.fellowship
    frodo = arda_users.frodo
    frodo.position = 'Хоббит'
    frodo.save()
    raw_make_role(frodo, simple_system, {'role': 'manager'}, fields_data={'passport-login': 'yndx-frodo'},
                  state='requested')
    raw_make_role(frodo, simple_system, {'role': 'manager'}, fields_data={'passport-login': 'yndx-frodo-bearer'},
                  system_specific={'login': 'iamfrodo'})
    raw_make_role(frodo, simple_system, {'role': 'manager'}, system_specific={'passport-login': 'yndx-frodo-friend'})
    raw_make_role(fellowship, simple_system, {'role': 'manager'})
    Role.objects.filter(state='granted').update(granted_at=datetime(1970, 1, 1, 0, 0, 0, tzinfo=utc))

    clear_mailbox()

    comment = 'Хочу увидеть всё, что скрыто'
    with assert_num_queries(12):
        # выборка user-a
        # создание action-а
        # выборка количества
        # выборка ролей
        # -- отправка сообщения:
        # выборка сайта
        # выборка шаблона (3 раза)
        # создание notice
        # выборка шаблона (3 раза)
        make_report(Role.objects.reports_related(), 'csv', 'roles', frodo, comment)

    assert Action.objects.filter(action='report_requested').count() == 1
    action = Action.objects.get(action='report_requested')
    assert action.user_id == frodo.id

    assert len(mail.outbox) == 1
    message = mail.outbox[0]

    assert_csv_report(
        '''
            Сотрудник;Логин;Тип группы;Должность;Отдел;Система;Роль;Код роли;Доп. данные;Состояние;Выдана;Подтвердили

            Фродо Бэггинс;frodo;-;Хоббит;Братство кольца;Simple система;Роль: Менеджер;"{""role"": ""manager""}";passport-login: yndx-frodo;Запрошена;;
            Фродо Бэггинс;frodo;-;Хоббит;Братство кольца;Simple система;Роль: Менеджер;"{""role"": ""manager""}";login: iamfrodo, passport-login: yndx-frodo-bearer;Выдана;1970-01-01 03:00;
            Фродо Бэггинс;frodo;-;Хоббит;Братство кольца;Simple система;Роль: Менеджер;"{""role"": ""manager""}";passport-login: yndx-frodo-friend;Выдана;1970-01-01 03:00;
            Братство кольца;;department;-;-;Simple система;Роль: Менеджер;"{""role"": ""manager""}";;Выдана;1970-01-01 03:00;
        ''',
        message, frodo, comment
    )


def test_failed_report(arda_users, settings):
    clear_mailbox()
    with mock.patch('xlwt.Workbook.add_sheet') as add_sheet:
        add_sheet.side_effect = ValueError
        make_report(Role.objects.all(), 'xls', 'actions', arda_users.frodo, 'Комментарий')

    assert len(mail.outbox) == 1
    message = mail.outbox[0]
    assert message.to == ['frodo@example.yandex.ru']
    assert message.cc == list(settings.EMAILS_FOR_PROBLEMS)
    assert message.subject == 'Ошибка формирования отчёта.'
    assert_contains(['При формировании отчёта произошла ошибка'], message.body)


def test_too_long_text_for_xls_cells(simple_system, arda_users):
    """Проверим случай, когда текст не умещается в excel-ячейку"""

    frodo = arda_users.frodo
    set_workflow(simple_system, 'approvers = []\n' + '#long text\n' * 3000)
    make_report(Action.objects.select_related('requester', 'system'), 'xls', 'actions', frodo, 'Хочу увидеть всё, что скрыто')
    assert len(mail.outbox) == 1
    message = mail.outbox[0]
    assert message.to == ['frodo@example.yandex.ru']
    assert message.subject == 'Отчёт сформирован.'
    assert_contains([
        'Добрый день',
        'Отчёт сформирован и приложен к данному письму.',
        'Хочу увидеть всё, что скрыто',
        'Ваш IDM'
    ], message.body)
