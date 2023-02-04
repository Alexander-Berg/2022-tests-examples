# -*- coding: utf-8 -*-


from textwrap import dedent

import pytest
from dbtemplates.models import Template
from django.core import mail
from django.core.management import call_command
from mock import patch

from idm.core.models import ApproveRequest, Role
from idm.core.plugins import errors
from idm.notification.models import Notice
from idm.tests.utils import (
    create_user, assert_contains, refresh, set_workflow,
    assert_action_chain, add_perms_by_role, clear_mailbox, set_roles_tree, patch_role,
)
from idm.tests.utils import make_absent

pytestmark = [pytest.mark.django_db]


def test_html_template(simple_system, more_users_for_test):
    set_workflow(simple_system, 'approvers = [approver("protoss") | approver("zerg")]')
    terran = create_user('terran')

    assert len(mail.outbox) == 0

    text_template = Template.objects.create(
        name='emails/approve_role_%s.txt' % simple_system.slug,
        content='text letter',
    )

    html_template = Template.objects.create(
        name='emails/approve_role_%s.html' % simple_system.slug,
        content='<html></html>',
    )

    # запросим роль
    Role.objects.request_role(terran, terran, simple_system, 'Комментарий к запросу роли', {'role': 'admin'}, {})

    message = mail.outbox[0]
    assert message.body == '<html></html>'
    assert message.content_subtype == 'html'

    html_template.delete()
    Role.objects.request_role(terran, terran, simple_system, 'Комментарий к запросу роли', {'role': 'manager'}, {})

    message = mail.outbox[2]
    assert 'text letter' in message.body
    assert message.content_subtype == 'plain'

    text_template.delete()  # чтобы не интерферировать с другими тестами; dbtemplates используют кеш


def test_send_notification_about_granted_not_approved_role(simple_system):
    """Проверяет отправку письма со странными ролями: в состоянии granted, но с незакрытым запросом"""

    terran = create_user('terran')
    protos = create_user('protos')

    assert terran.roles.count() == 0
    assert protos.roles.count() == 0
    assert len(mail.outbox) == 0

    # запросим роль, который выдастся без нареканий
    set_workflow(simple_system, 'approvers = []')

    Role.objects.request_role(terran, terran, simple_system, '', {'role': 'admin'}, {})

    assert terran.roles.count() == 1
    role = terran.roles.get()
    assert role.state == 'granted'
    assert role.requests.count() == 1
    assert role.requests.get().approves.count() == 0  # роль выдана автоматически
    # письмо о выдаче роли
    assert len(mail.outbox) == 1

    # ничего не должно отправиться
    call_command('idm_find_granted_not_approved_roles')
    assert len(mail.outbox) == 1

    # запросим новую роль для protos и поставим terran аппрувером
    set_workflow(simple_system, 'approvers = [approver("terran")]')
    assert protos.roles.count() == 0

    Role.objects.request_role(protos, protos, simple_system, '', {'role': 'admin'}, {})

    assert protos.roles.count() == 1
    role = protos.roles.get()
    assert role.state == 'requested'
    assert role.requests.count() == 1
    request = role.requests.get()
    assert not request.is_done
    # добавились 2 письма - о запросе роли и для подтверждения
    assert len(mail.outbox) == 3

    # ничего не должно отправиться
    call_command('idm_find_granted_not_approved_roles')
    assert len(mail.outbox) == 3

    # какая-то магия выдает роль - его команда отправит в письме
    role = protos.roles.get()
    assert role.state == 'requested'
    patch_role(role, state='granted')
    role = refresh(role)
    assert role.state == 'granted'
    assert role.requests.count() == 1
    request = role.requests.get()
    # роль выдана, но запрос еще не отработан
    assert not request.is_done
    assert len(mail.outbox) == 3

    call_command('idm_find_granted_not_approved_roles')

    assert len(mail.outbox) == 5
    assert mail.outbox[3].subject == 'Странные роли.'
    assert ('id: %s' % role.id) in mail.outbox[3].body


def test_send_email_with_passport_if_fail(generic_system):
    """В письме о сфейлившемся запросе роли отправляем паспортный логин, если он был заведен"""

    admin = create_user('admin', superuser=True)
    terran = create_user('terran')

    assert terran.roles.count() == 0
    assert terran.passport_logins.count() == 0
    assert len(mail.outbox) == 0
    assert Notice.objects.count() == 0

    with patch.object(generic_system.plugin.__class__, 'add_role') as add_role:
        add_role.side_effect = errors.PluginFatalError(1, 'blah minor', {'a': 'b'})

        Role.objects.request_role(admin, terran, generic_system, '', {'role': 'admin'},
                                  {'passport-login': 'yndx.terran.admin'})

    terran = refresh(terran)
    assert terran.passport_logins.count() == 1
    assert terran.roles.count() == 1

    role = terran.roles.get()
    assert role.state == 'awaiting'
    last_action = role.actions.order_by('-id')[0]
    assert last_action.action == 'await'
    terran.passport_logins.update(is_fully_registered=True)
    with patch.object(generic_system.plugin.__class__, 'add_role') as add_role:
        add_role.side_effect = errors.PluginFatalError(1, 'blah minor', {'a': 'b'})
        Role.objects.poke_awaiting_roles()
    role = refresh(role)
    assert role.state == 'failed'
    # Проверим, что добавился role action
    last_action = role.actions.order_by('-id')[0]
    assert last_action.action == 'fail'
    assert last_action.error == '''PluginFatalError: code=1, message="blah minor", data={'a': 'b'}, answer="None"'''

    # пользователь и запросивший роль должны получить письма о неудаче
    assert len(mail.outbox) == 2
    assert mail.outbox[0].subject == 'Произошла ошибка при добавлении запрошенной вами в системе "Generic система" роли'
    assert mail.outbox[0].to == ['admin@example.yandex.ru']
    assert_contains([
        'При добавлении роли для terran в систему "Generic система" произошла ошибка:',
        'Роль: Админ',
        'Ошибка: PluginFatalError: code=1, message="blah minor", data={\'a\': \'b\'}, answer="None"',
        'Ваш IDM',
    ], mail.outbox[0].body)

    assert mail.outbox[1].subject == 'Ошибка при добавлении роли в систему "Generic система"'
    assert mail.outbox[1].to == ['terran@example.yandex.ru']

    assert_contains([
        'При добавлении роли для terran в систему "Generic система" произошла ошибка:',
        'Роль: Админ',
        'Ошибка: PluginFatalError: code=1, message="blah minor", data={\'a\': \'b\'}, answer="None"',
        'В Паспорте был заведен новый логин: yndx.terran.admin',
        'Вы можете восстановить пароль при утере по адресу ',
        'https://passport.yandex.ru/passport?mode=restore, используя свой рабочий email.',
        'Ваш IDM',
    ], mail.outbox[1].body)

    # должны создаться уведомления
    assert Notice.objects.count() == 2
    notice_1 = Notice.objects.get(recipient=terran)
    assert notice_1.recipient == terran
    assert notice_1.subject == 'Ошибка при добавлении роли в систему "Generic система"'
    assert not notice_1.is_seen
    assert_contains([
        'При добавлении роли для terran в систему "Generic система" произошла ошибка:',
        'Роль: Админ',
        'Ошибка: PluginFatalError: code=1, message="blah minor", data={\'a\': \'b\'}, answer="None"',
        'В Паспорте был заведен новый логин: yndx.terran.admin',
        'Вы можете восстановить пароль при утере по адресу ',
        'https://passport.yandex.ru/passport?mode=restore, используя свой рабочий email.'
    ], notice_1.message)
    notice_2 = Notice.objects.get(recipient=admin)
    assert notice_2.recipient == admin
    assert notice_2.subject == 'Произошла ошибка при добавлении запрошенной вами в системе "Generic система" роли'
    assert not notice_2.is_seen
    assert_contains([
        'При добавлении роли для terran в систему "Generic система" произошла ошибка:',
        'Роль: Админ',
        'Ошибка: PluginFatalError: code=1, message="blah minor", data={\'a\': \'b\'}, answer="None"',
    ], notice_2.message)


def test_send_email_with_passport_if_ok(generic_system, arda_users):
    """В письме об успешной выдаче роли отправлем паспортный логин, если он был заведен"""

    frodo = arda_users.frodo

    with patch.object(generic_system.plugin.__class__, '_post_data') as post_data:
        post_data.return_value = {
            'data': {
                'token': 'whoa!'
            }
        }
        with patch('idm.sync.passport.exists') as passport_exists, patch('idm.sync.passport.register_login'):
            passport_exists.return_value = False
            Role.objects.request_role(frodo, frodo, generic_system, '', {'role': 'admin'},
                                      {'passport-login': 'yndx-frodo'})

    assert frodo.passport_logins.count() == 1
    assert frodo.roles.count() == 1
    role = frodo.roles.get()

    assert role.state == 'awaiting'
    last_action = role.actions.order_by('-id')[0]
    assert last_action.action == 'await'

    frodo.passport_logins.update(is_fully_registered=True)
    with patch('idm.sync.passport.exists') as passport_exists,\
            patch.object(generic_system.plugin.__class__, '_post_data') as post_data:
        post_data.return_value = {
            'data': {
                'token': 'whoa!'
            }
        }
        passport_exists.return_value = True
        Role.objects.poke_awaiting_roles()

    role = refresh(role)
    assert role.state == 'granted'

    # Проверим, что добавился role action
    assert_action_chain(
        role, ['request', 'apply_workflow', 'approve', 'await', 'approve', 'first_add_role_push', 'grant']
    )

    # пользователь и запросивший роль должны получить письма
    assert len(mail.outbox) == 1
    message = mail.outbox[0]
    assert message.to == ['frodo@example.yandex.ru']
    assert message.subject == 'Generic система. Новая роль'
    assert_contains([
        'Вы получили новую роль в системе "Generic система":',
        'Роль: Админ',
        '''Паспортный логин: yndx-frodo''',
    ], message.body)

    clear_mailbox()

    # теперь логин не новый, запросим ещё одну роль на него же
    with patch.object(generic_system.plugin.__class__, '_post_data') as post_data:
        post_data.return_value = {
            'data': {
                'token': 'whoa!'
            }
        }
        with patch('idm.sync.passport.exists') as passport_exists, patch('idm.sync.passport.register_login'):
            passport_exists.return_value = True
            Role.objects.request_role(frodo, frodo, generic_system, '', {'role': 'manager'},
                                      {'passport-login': 'yndx-frodo'})
    assert len(mail.outbox) == 1
    message = mail.outbox[0]
    assert message.to == ['frodo@example.yandex.ru']
    assert message.subject == 'Generic система. Новая роль'

    assert_contains([
        'Вы получили новую роль в системе "Generic система":',
        'Роль: Менеджер',
        '''Паспортный логин: yndx-frodo''',
    ], message.body)


def test_send_email_with_context_variables_from_handle(generic_system, arda_users):
    """В письме об успешной выдаче роли отправляем переданные нам системой переменные контекста"""

    template = Template()
    template.name = 'emails/role_granted_%s.txt' % generic_system.slug
    template.content = '''
    You have got a new role in the system "{{ role.system.name }}": {{ role.email_humanize|safe }}.
    Your passport login: {{ passport_login }}
    Your target: {{ target }}
    Your Moria password: {{ password }}
    Your token: {{ token }}
    Your cheese: {{ role.system_specific.cheese }}
    '''
    template.save()

    frodo = arda_users.frodo
    frodo.passport_logins.create(login='yndx-frodo', state='created', is_fully_registered=True)

    with patch.object(generic_system.plugin.__class__, '_post_data') as post_data:
        post_data.return_value = {
            'data': {
                'token': 'ouch!',
                'cheese': 'red Leicester',
            },
            'context': {
                'target': 'Moria',
                'password': 'mellon',
                'token': 'whoa!',
            }
        }
        with patch('idm.sync.passport.exists') as passport_exists:
            passport_exists.return_value = True
            Role.objects.request_role(frodo, frodo, generic_system, '', {'role': 'admin'},
                                      {'passport-login': 'yndx-frodo'})

    role = frodo.roles.get()
    assert role.state == 'granted'

    # пользователь должен получить письмо с дополнительными данными от ручки
    assert len(mail.outbox) == 1
    message = mail.outbox[0]
    assert message.to == ['frodo@example.yandex.ru']
    assert message.subject == 'Generic система. Новая роль'
    assert_contains([
        'You have got a new role in the system "Generic система": Роль: Админ',
        'Your target: Moria',
        'Your Moria password: mellon',
        'Your token: whoa!',  # приоритет имеют данные из словаря context
        'Your cheese: red Leicester',
    ], message.body)
    assert message.content_subtype == 'plain'
    template.delete()  # чтобы не интерферировать с другими тестами; dbtemplates используют кеш


def test_send_html_email(generic_system, arda_users):
    """В письме об успешной выдаче роли отправляем переданные нам системой переменные контекста"""

    text_template = Template.objects.create(
        name='emails/role_granted_%s.txt' % generic_system.slug,
        content='text',
    )

    html_template = Template.objects.create(
        name='emails/role_granted_%s.html' % generic_system.slug,
        content='<html></html>',
    )

    frodo = arda_users.frodo
    frodo.passport_logins.create(login='yndx-frodo', state='created', is_fully_registered=True)

    with patch.object(generic_system.plugin.__class__, '_post_data') as post_data:
        post_data.return_value = {
            'data': {
                'token': 'ouch!',
                'cheese': 'red Leicester',
            },
            'context': {
                'target': 'Moria',
                'password': 'mellon',
                'token': 'whoa!',
            }
        }
        with patch('idm.sync.passport.exists') as passport_exists:
            passport_exists.return_value = True
            Role.objects.request_role(frodo, frodo, generic_system, '', {'role': 'admin'},
                                      {'passport-login': 'yndx-frodo'})

    message = mail.outbox[0]
    assert message.body == '<html></html>'
    assert message.content_subtype == 'html'

    html_template.delete()  # чтобы не интерферировать с другими тестами; dbtemplates используют кеш
    text_template.delete()


def test_send_user_role_emails_request_for_self(simple_system, users_for_test):
    """Проверяем отправление почты пользователю и аппруверам при запросе роли самому себе"""
    (art, fantom, terran, admin) = users_for_test

    set_workflow(simple_system, 'approvers = [approver("art"), approver("fantom")]')

    assert len(mail.outbox) == 0
    assert Notice.objects.count() == 0

    Role.objects.request_role(terran, terran, simple_system, '1->2', {'role': 'admin'}, {})

    # уйдут 3 письма: terran - простое, art и fantom - со ссылкой на подтверждение роли
    art_mail, fantom_mail, terran_mail = mail.outbox
    assert art_mail.to == ['art@example.yandex.ru']
    assert fantom_mail.to == ['fantom@example.yandex.ru']
    assert terran_mail.to == ['terran@example.yandex.ru']
    assert_contains([
        'Добрый день',
        'Легат Аврелий',
        'Simple система',
        'Роль: Админ',
        'Вы получили запрос, он ожидает вашего подтверждения',
        'https://example.com/queue/',
        'Комментарий:',
        '1->2'
    ], art_mail.body)
    assert remove_first_row(art_mail.body) == remove_first_row(fantom_mail.body)

    assert_contains([
        'Simple система',
        'Роль: Админ',
        'Центурион Марк',
        'Легионер Тит',
    ], terran_mail.body)


def test_send_user_role_emails_approvers_and(simple_system, users_for_test):
    """Проверяем отправление почты пользователю и аппруверам при запросе роли
    Требуется подтвержение роли от всех аппруверов"""
    (art, fantom, terran, admin) = users_for_test

    set_workflow(simple_system, 'approvers = [approver("art"), approver("fantom")]')

    assert len(mail.outbox) == 0
    assert Notice.objects.count() == 0

    role = Role.objects.request_role(admin, terran, simple_system, '', {'role': 'admin'}, {})

    # уйдут 3 письма: terran - простое, art и fantom - со ссылкой на подтверждение роли
    art_mail, fantom_mail, terran_mail = mail.outbox
    assert art_mail.to == ['art@example.yandex.ru']
    assert fantom_mail.to == ['fantom@example.yandex.ru']
    assert terran_mail.to == ['terran@example.yandex.ru']

    assert_contains([
        'Добрый день',
        'admin',
        'Легат Аврелий',
        'Simple система',
        'Роль: Админ',
        'Вы получили запрос, он ожидает вашего подтверждения',
        'https://example.com/queue/'
    ], art_mail.body)
    assert remove_first_row(art_mail.body) == remove_first_row(fantom_mail.body)

    assert_contains([
        'admin',
        'Simple система',
        'Роль: Админ',
        'Центурион Марк',
        'Легионер Тит',
    ], terran_mail.body)

    # проверим создание уведомлений
    assert Notice.objects.count() == 3

    notice = Notice.objects.get(recipient__username='terran')
    assert not notice.is_seen
    assert notice.subject == 'Роль в системе "Simple система" требует подтверждения.'

    role = refresh(role)
    assert role.requests.count() == 1
    role_request = role.requests.get()
    assert role_request.approves.count() == 2
    assert role_request.is_done is False


def remove_first_row(s):
    return '\n'.join(s.split('\n')[1:])


def test_send_user_role_emails_approvers_or(simple_system, users_for_test):
    """Проверяем отправление почты пользователю и аппруверам при запросе роли
    Требуется подтверждение роли по крайней мере от одного аппрувера"""
    (art, fantom, terran, admin) = users_for_test

    set_workflow(simple_system, 'approvers = [approver("art") | approver("fantom")]')

    assert len(mail.outbox) == 0

    # оба одобряющих по дефолту на работе
    role = Role.objects.request_role(admin, terran, simple_system, '', {'role': 'admin'}, {})

    # уйдут 2 письма: terran - простое, art (fantom только если art нет на работе) - со ссылкой на подтверждение роли
    assert len(mail.outbox) == 2
    art_message, terran_message = mail.outbox
    assert art_message.to == ['art@example.yandex.ru']
    assert_contains([
        'Добрый день',
        'admin',
        'Легат Аврелий',
        'Simple система',
        'Роль: Админ',
        'Вы получили запрос, он ожидает вашего подтверждения',
        'https://example.com/queue/'
    ], art_message.body)
    assert terran_message.to == ['terran@example.yandex.ru']
    assert_contains([
        'admin',
        'Simple система',
        'Роль: Админ',
        'Центурион Марк',
        'Легионер Тит',
    ], terran_message.body)
    role = refresh(role)
    assert role.requests.count() == 1
    role_request = role.requests.get()
    assert role_request.approves.count() == 1
    assert role_request.is_done is False

    clear_mailbox()

    # art в отпуске
    art.is_absent = True
    art.save()
    Role.objects.request_role(admin, terran, simple_system, '', {'role': 'manager'}, {})

    assert len(mail.outbox) == 2
    fantom_message, terran_message = mail.outbox
    assert fantom_message.to == ['fantom@example.yandex.ru']
    assert terran_message.to == ['terran@example.yandex.ru']

    assert_contains([
        'Добрый день',
        'admin',
        'Легат Аврелий',
        '"Simple система"',
        'Роль: Менеджер',
        'Вы получили запрос, он ожидает вашего подтверждения',
        'https://example.com/queue/',
        'Ваш IDM',
    ], fantom_message.body)


def test_send_user_role_emails_approvers_or_send_to_all(simple_system, users_for_test):
    """Проверяем отправление почты пользователю и аппруверам при запросе роли.
    Требуется подтверждение роли по крайней мере от одного аппрувера"""

    (art, fantom, terran, admin) = users_for_test

    set_workflow(simple_system, 'approvers = [approver("art") | approver("fantom")]')

    assert len(mail.outbox) == 0

    make_absent([art, fantom])

    role = Role.objects.request_role(terran, terran, simple_system, '', {'role': 'admin'}, {})

    # уйдут 2 письма: terran - простое, art – со ссылкой на подтверждение роли,
    assert len(mail.outbox) == 2
    art_mail, terran_mail = mail.outbox
    assert art_mail.to == ['art@example.yandex.ru']
    assert terran_mail.to == ['terran@example.yandex.ru']
    assert_contains([
        'Добрый день',
        'Легат Аврелий',
        'Simple система',
        'Роль: Админ',
        'Вы получили запрос, он ожидает вашего подтверждения',
        'https://example.com/queue/',
        'Основной подтверждающий, оповещен:',
        'Ваш IDM',
    ], art_mail.body)
    assert_contains([
        'Simple система',
        'Роль: Админ',
        'Центурион Марк',
        'Легионер Тит',
    ], terran_mail.body)

    role = refresh(role)
    assert role.requests.count() == 1
    role_request = role.requests.get()
    assert role_request.is_done is False
    assert role_request.approves.count() == 1
    assert role_request.approves.get().requests.count() == 2


def test_send_user_role_emails_approvers_or_absent(simple_system, arda_users):
    """Проверяем отправление почты пользователю и аппруверам при запросе роли, оба аппрувера отсутствуют"""

    varda = arda_users.varda
    # TODO: убрать, когда проверка прав будет смотреть на группы, а не департаменты
    add_perms_by_role('responsible', varda, simple_system)
    frodo = arda_users.frodo
    set_workflow(simple_system, 'approvers = [approver("legolas") | "gandalf", approver("aragorn") | "gimli"]')

    assert len(mail.outbox) == 0

    make_absent([arda_users.legolas, arda_users.gandalf, arda_users.aragorn])
    role = Role.objects.request_role(varda, frodo, simple_system, '', {'role': 'admin'}, {})

    # Письмо пошлётся gimli, так как он присутствует в свой or-группе, а также Леголасу , так как
    # оба они отсутствуют, а на каждую OR-группу должно быть отправлено хотя бы одно письмо.
    # Плюс одно письмо отправится запрашивающему, frodo.
    assert len(mail.outbox) == 3
    assert mail.outbox[-1].to == ['frodo@example.yandex.ru']
    assert mail.outbox[-1].subject == 'Роль в системе "Simple система" требует подтверждения.'
    assert_contains([
        'varda запросил для вас роль',
        'Simple система',
        'Роль: Админ',
        'legolas',
        'aragorn или gimli',
    ], mail.outbox[-1].body)
    messages_to_approvers = mail.outbox[:-1]

    for message, expected in zip(messages_to_approvers, ['legolas', 'gimli']):
        assert message.to == ['%s@example.yandex.ru' % expected]
        assert message.subject == 'Подтверждение роли. Simple система.'

    role = refresh(role)
    assert role.requests.count() == 1
    role_request = role.requests.get()
    assert role_request.is_done is False
    assert role_request.approves.count() == 2
    assert ApproveRequest.objects.count() == 4


def test_send_user_role_emails_approvers_or_absent_send_to_all(simple_system, users_for_test):
    """Проверяем отправление почты пользователю и аппруверам при запросе роли,
    оба аппрувера отсутствуют"""
    (art, fantom, terran, admin) = users_for_test

    set_workflow(simple_system, 'approvers = [approver("art") | approver("fantom")]')

    assert len(mail.outbox) == 0
    role = Role.objects.request_role(admin, terran, simple_system, '', {'role': 'admin'}, {})

    # уйдет 2 письма для terran,art
    assert len(mail.outbox) == 2
    art_mail, terran_mail = mail.outbox
    assert art_mail.to == ['art@example.yandex.ru']
    assert terran_mail.to == ['terran@example.yandex.ru']
    assert_contains([
        'Добрый день',
        'admin',
        'Легат Аврелий',
        '"Simple система"',
        'Роль: Админ',
        'Вы получили запрос, он ожидает вашего подтверждения',
        'https://example.com/queue/'
    ], art_mail.body)

    assert_contains([
        'admin',
        'Simple система',
        'Роль: Админ',
        'Центурион Марк',
        'Легионер Тит',
    ], terran_mail.body)

    role = refresh(role)
    assert role.requests.count() == 1
    role_request = role.requests.get()
    assert role_request.is_done is False
    assert role_request.approves.count() == 1
    assert role_request.approves.get().requests.count() == 2


def test_notify_everyone(simple_system, arda_users):
    """Проверяем опцию notify_everyone"""

    frodo = arda_users.frodo
    workflow_code = 'approvers = [approver("legolas") | "gandalf", approver("varda") | "manve"]'

    make_absent([arda_users.legolas, arda_users.manve])

    set_workflow(simple_system, workflow_code)
    Role.objects.request_role(frodo, frodo, simple_system, '', {'role': 'admin'}, {})
    assert len(mail.outbox) == 3
    assert [message.to[0].split('@', 1)[0] for message in mail.outbox] == ['gandalf', 'varda', 'frodo']
    assert {message.subject for message in mail.outbox[:-1]} == {'Подтверждение роли. Simple система.'}
    assert mail.outbox[-1].subject == 'Роль в системе "Simple система" требует подтверждения.'
    Role.objects.all().delete()
    clear_mailbox()
    set_workflow(simple_system, 'notify_everyone = True; %s' % workflow_code)

    Role.objects.request_role(frodo, frodo, simple_system, '', {'role': 'admin'}, {})
    assert len(mail.outbox) == 5
    expected = ['legolas', 'gandalf', 'varda', 'manve', 'frodo']
    assert [message.to[0].split('@', 1)[0] for message in mail.outbox] == expected
    assert {message.subject for message in mail.outbox[:-1]} == {'Подтверждение роли. Simple система.'}
    assert mail.outbox[-1].subject == 'Роль в системе "Simple система" требует подтверждения.'


def test_notify_explicitly_true(simple_system, arda_users):
    """Проверяем опцию approver('username', True)"""

    frodo = arda_users.frodo
    workflow_code = ('approvers = [approver("legolas", notify=True) | "gandalf",'
                     'approver("varda", notify=True) | "manve"]')

    make_absent([arda_users.legolas, arda_users.manve])
    set_workflow(simple_system, workflow_code)
    Role.objects.request_role(frodo, frodo, simple_system, '', {'role': 'admin'}, {})
    assert len(mail.outbox) == 4
    assert [message.to[0].split('@', 1)[0] for message in mail.outbox] == ['gandalf', 'legolas', 'varda', 'frodo']
    assert {message.subject for message in mail.outbox[:-1]} == {'Подтверждение роли. Simple система.'}
    assert mail.outbox[-1].subject == 'Роль в системе "Simple система" требует подтверждения.'
    Role.objects.all().delete()
    clear_mailbox()
    set_workflow(simple_system, 'notify_everyone = True; %s' % workflow_code)

    Role.objects.request_role(frodo, frodo, simple_system, '', {'role': 'admin'}, {})
    assert len(mail.outbox) == 5
    expected = ['legolas', 'gandalf', 'varda', 'manve', 'frodo']
    assert [message.to[0].split('@', 1)[0] for message in mail.outbox] == expected
    assert {message.subject for message in mail.outbox[:-1]} == {'Подтверждение роли. Simple система.'}
    assert mail.outbox[-1].subject == 'Роль в системе "Simple система" требует подтверждения.'


def test_notify_explicitly_false(simple_system, arda_users):
    """Проверяем опцию approver('username', False)"""

    frodo = arda_users.frodo
    workflow_code = ('approvers = [approver("legolas", notify=False) | "gandalf", '
                     'approver("varda", notify=False) | "manve"]')

    make_absent([arda_users.legolas, arda_users.manve])
    set_workflow(simple_system, workflow_code)
    Role.objects.request_role(frodo, frodo, simple_system, '', {'role': 'admin'}, {})
    assert len(mail.outbox) == 3

    for message, expected in zip(mail.outbox, ['gandalf', 'manve', 'frodo']):
        assert len(message.to) == 1
        assert message.to[0] == expected + '@example.yandex.ru'

    Role.objects.all().delete()
    clear_mailbox()
    set_workflow(simple_system, 'notify_everyone = True; %s' % workflow_code)

    Role.objects.request_role(frodo, frodo, simple_system, '', {'role': 'admin'}, {})
    assert len(mail.outbox) == 5
    expected = ['legolas', 'gandalf', 'varda', 'manve', 'frodo']
    assert [mess.to[0].split('@', 1)[0] for mess in mail.outbox] == expected
    assert {mess.subject for mess in mail.outbox[:-1]} == {'Подтверждение роли. Simple система.'}
    assert mail.outbox[-1].subject == 'Роль в системе "Simple система" требует подтверждения.'


def test_send_user_role_emails_approvers_and_absent(simple_system, users_for_test):
    """Проверяем отправление почты пользователю и аппруверам при запросе роли, оба аппрувера отсутствуют"""
    (art, fantom, terran, admin) = users_for_test

    set_workflow(simple_system, 'approvers = [approver("art"), approver("fantom")]')

    assert len(mail.outbox) == 0

    make_absent([art, fantom])
    role = Role.objects.request_role(admin, terran, simple_system, '', {'role': 'admin'}, {})

    # письма аппруверам, хотя они отсутствуют, всё равно уйдут, так как кроме них никто подтвердить не может
    assert len(mail.outbox) == 3

    role = refresh(role)
    assert role.requests.count() == 1


def test_send_decline_user_role_emails_approvers_and(simple_system, users_for_test):
    """Проверяем отправление писем об отказе аппувером подтвердить роль пользователя
    в случае необходимости подтверждения от каждого аппрувера
    Все аппруверы отказали"""
    (art, fantom, terran, admin) = users_for_test

    set_workflow(simple_system, 'approvers = [approver("art"), approver("fantom")]')
    role = Role.objects.request_role(admin, terran, simple_system, '', {'role': 'admin'}, {})
    assert len(mail.outbox) == 3

    art_request = ApproveRequest.objects.select_related_for_set_decided().get(approver__username='art')
    fantom_request = ApproveRequest.objects.select_related_for_set_decided().get(approver__username='fantom')

    role = refresh(role)
    assert role.requests.count() == 1  # запрос создан, но еще не отработан
    role_request = role.requests.get()
    assert role_request.approves.count() == 2
    assert role_request.is_done is False
    clear_mailbox()

    # в случае отказа
    art_request.set_declined(art)

    role = refresh(role)
    assert role.requests.count() == 1  # запрос создан и отработан
    role_request = refresh(role_request)
    assert role_request.approves.count() == 2
    assert role_request.is_done is True
    assert len(mail.outbox) == 2

    # terran получит уведомление об отказе
    requester_mail, decline_email = mail.outbox
    assert decline_email.to == ['terran@example.yandex.ru']
    assert requester_mail.to == ['admin@example.yandex.ru']

    assert_contains([
        'Добрый день',
        'Центурион Марк (art) отклонил запрос роли в системе "Simple система" для пользователя Легат Аврелий (terran):',
        'Роль: Админ',
        'Ваш IDM'
    ], decline_email.body)

    # а к запросу на аппрув роли будет привязано действие
    request = ApproveRequest.objects.get(approver__username='art')
    decline_action = request.actions.get()
    assert decline_action.requester_id == art.id
    assert decline_action.action == 'decline'
    fantom_request.set_declined(fantom)

    # количество писем не изменилось, потому что запрос и так уже отклонил art
    assert len(mail.outbox) == 2

    role_request = refresh(role_request)
    assert role_request.approves.count() == 2
    assert role_request.is_done is True


def test_send_decline_user_role_emails_approvers_or(simple_system, users_for_test):
    """Проверяем отправление писем об отказе аппувером подтвердить роль пользователя
    в случае необходимости подтверждения от одного из аппруверов
    Все аппруверы отказали"""
    (art, fantom, terran, admin) = users_for_test

    set_workflow(simple_system, 'approvers = [approver("art") | approver("fantom")]')

    role = Role.objects.request_role(admin, terran, simple_system, '', {'role': 'admin'}, {})
    art_request = ApproveRequest.objects.select_related_for_set_decided().get(approver=art)
    fantom_request = ApproveRequest.objects.select_related_for_set_decided().get(approver=fantom)
    assert len(mail.outbox) == 2
    clear_mailbox()

    role = refresh(role)
    assert role.requests.count() == 1
    role_request = role.requests.get()
    assert role_request.is_done is False

    art_request.set_declined(art)
    assert len(mail.outbox) == 2
    requester_mail, decline_mail = mail.outbox
    assert requester_mail.to == ['admin@example.yandex.ru']
    assert requester_mail.subject == 'Запрошенная вами роль в системе "Simple система" отклонена'
    assert_contains((
        'Центурион Марк отказал в выдаче запрошенной вами для пользователя Легат Аврелий '
        'роли в системе "Simple система":',
        'Роль: Админ'
    ), requester_mail.body)

    assert decline_mail.to == ['terran@example.yandex.ru']

    assert_contains([
        'Добрый день',
        'Центурион Марк (art) отклонил запрос роли в системе "Simple система" для пользователя Легат Аврелий (terran):',
        'Роль: Админ',
        'Ваш IDM'
    ], decline_mail.body)

    role_request = refresh(role_request)
    assert role_request.is_done is True

    fantom_request.set_declined(fantom)
    # количество писем не изменилось, потому что запрос и так уже отклонил art
    assert len(mail.outbox) == 2
    role_request = refresh(role_request)
    assert role_request.is_done is True


def test_send_approve_user_role_emails(simple_system, users_for_test):
    """Аппруверы подтверждают роль пользователя
    Пользователю отправляется письмо только после подстверждения последним аппрувером
    Статус роли пользователя устанавливается в 'approved'"""
    (art, fantom, terran, admin) = users_for_test

    set_workflow(simple_system, 'approvers = [approver("art"), approver("fantom")]')

    assert len(mail.outbox) == 0
    role = Role.objects.request_role(admin, terran, simple_system, '', {'role': 'admin'}, {})

    assert len(mail.outbox) == 3
    assert ApproveRequest.objects.count() == 2
    assert ApproveRequest.objects.filter(decision='').count() == 2

    role = refresh(role)
    assert role.user_id == terran.id
    assert role.state == 'requested'
    art_request = ApproveRequest.objects.select_related_for_set_decided().get(approver__username='art')
    fantom_request = ApproveRequest.objects.select_related_for_set_decided().get(approver__username='fantom')

    assert role.requests.count() == 1  # запрос еще новый
    role_request = role.get_last_request()
    assert role_request.approves.count() == 2
    assert not role_request.is_done

    art_request.set_approved(art)
    request = ApproveRequest.objects.get(approver__username='art')
    assert request.approved is True
    assert request.decision == 'approve'

    assert len(mail.outbox) == 3

    assert role.requests.count() == 1  # запрос еще не отработан, но один из аппрувов - да
    role_request = refresh(role_request)
    assert role_request.approves.count()
    assert not role_request.is_done

    fantom_request.set_approved(fantom)
    role = refresh(role)
    assert role.state == 'granted'
    assert len(mail.outbox) == 4
    assert mail.outbox[3].to == ['terran@example.yandex.ru']
    assert_contains(['Вы получили новую роль в системе',
                     'Simple система',
                     'Роль: Админ',
                     'Роль была запрошена пользователем admin'], mail.outbox[3].body)

    assert role.requests.count() == 1  # запрос отработан
    role_request = refresh(role_request)
    assert role_request.is_done


def test_qr(generic_system, arda_users):
    frodo = arda_users.frodo
    set_roles_tree(generic_system, {
        'code': 0,
        'roles': {
            'slug': 'role',
            'name': 'Роль',
            'values': {
                'long': 'Длинный пароль',
            },
        },
    })
    set_workflow(generic_system)

    with patch.object(generic_system.plugin.__class__, '_post_data') as post_data:
        post_data.return_value = {
            'context': {
                'password': 'hello',
            }
        }
        role = Role.objects.request_role(frodo, frodo, generic_system, '', {'role': 'long'}, None)
    role = refresh(role)
    assert role.state == 'granted'
    assert len(mail.outbox) == 1
    message = mail.outbox[0]
    assert len(message.attachments) == 1
    attachment = message.attachments[0]
    filename, data, format_ = attachment
    assert filename == 'password.gif'
    assert format_ == 'image/gif'
    assert data.startswith(b'GIF87')


def test_notifications_with_consecutive_priority(arda_users, simple_system):
    """
        Проверяет что письма уходят людям с учетом приоритетов
    """

    #  если в группе у всех не проставлен приоритет, он проставляется по порядку слева направо начиная с 1
    workflow_ = dedent("""
        approvers = [any_from(['gimli', 'sam']) | approver('bilbo'), approver('galadriel',
                    priority=1) | any_from(['saruman', 'gandalf', 'varda']), approver('manve') | 'aragorn' | 'meriadoc',
                     any_from(['boromir', 'peregrin'], priority=1) | any_from(['galadriel', 'sauron'])]
        """)

    set_workflow(simple_system, workflow_, workflow_)

    Role.objects.request_role(arda_users.frodo, arda_users.frodo, simple_system, '', {'role': 'admin'})

    assert len(mail.outbox) == 6

    for message, expected in zip(mail.outbox, ['gimli', 'galadriel', 'manve', 'boromir', 'peregrin', 'frodo']):
        assert len(message.to) == 1 and message.to[0] == '{}@example.yandex.ru'.format(expected)


def test_notifications_with_notify_all_false(arda_users, simple_system):
    """
        Проверяет что письма уходят людям с учетом приоритетов и флагом notify
    """

    #  уведомления никому не придут, кроме запрашивающего, т.к. notify=False
    workflow_ = "approvers = [any_from(['gimli', 'sam'], priority=1, notify=False)]"

    set_workflow(simple_system, workflow_, workflow_)

    role = Role.objects.request_role(arda_users.frodo, arda_users.frodo, simple_system, '', {'role': 'admin'})

    assert len(mail.outbox) == 1

    for message, expected in zip(mail.outbox, ['frodo']):
        assert len(message.to) == 1 and message.to[0] == '{}@example.yandex.ru'.format(expected)

    clear_mailbox()

    # после пересчета основного приоритета уведомления все равно не придут
    ApproveRequest.objects.recalculate_main_priority()
    assert len(mail.outbox) == 0

    # но основной приоритет проставится
    assert role.last_request.approves.first().main_priority == 1


def test_notifications_with_notify_true(arda_users, simple_system):
    """
        Проверяет случаи когда аппруверам с флагом notify=True приходят уведомления несмотря на приоритет.
        Аппруверам с notify=False уведомления приходить не будут
    """
    workflow_ = dedent("""
        approvers = [any_from(['gimli', 'sam'], priority=1) | approver('sauron', priority=2) |
        approver('bilbo', priority=10, notify=True),
        approver('galadriel', priority=2) | approver('varda', priority=4) |
        any_from(['saruman', 'gandalf', 'varda'], priority=1, notify=False),
        approver('boromir', priority=1, notify=False) | any_from(['meriadoc', 'legolas']) |
        any_from(['galadriel', 'sauron'], notify=True)]
        """)

    set_workflow(simple_system, workflow_, workflow_)

    Role.objects.request_role(arda_users.frodo, arda_users.frodo, simple_system, '', {'role': 'admin'})

    assert len(mail.outbox) == 8

    for message, expected in zip(mail.outbox, ['gimli', 'sam', 'bilbo', 'galadriel', 'meriadoc', 'legolas', 'sauron',
                                               'frodo']):
        assert len(message.to) == 1 and message.to[0] == '{}@example.yandex.ru'.format(expected)

    make_absent([arda_users.gimli, arda_users.sam])

    clear_mailbox()

    ApproveRequest.objects.recalculate_main_priority()

    assert len(mail.outbox) == 2

    for message, expected in zip(mail.outbox, ['sauron', 'galadriel']):
        assert len(message.to) == 1 and message.to[0] == '{}@example.yandex.ru'.format(expected)


def test_notifications_with_repetitions(arda_users, simple_system):
    """
        Проверяет случаи когда аппрувер состоит в нескольких AND-группах и ему несколько раз приходит письмо
    """
    worklow_ = dedent("""
        approvers = [approver('gimli', priority=1) | approver('sam', priority=2),
                    any_from(['gimli', 'sam'], notify=True)]
    """)

    set_workflow(simple_system, worklow_, worklow_)

    Role.objects.request_role(arda_users.frodo, arda_users.frodo, simple_system, '', {'role': 'admin'})

    assert len(mail.outbox) == 3  # gimli с первой группы, sam со второй и frodo

    for message, expected in zip(mail.outbox, ['gimli', 'sam', 'frodo']):
        assert len(message.to) == 1 and message.to[0] == '{}@example.yandex.ru'.format(expected)

    clear_mailbox()

    make_absent([arda_users.gimli])

    ApproveRequest.objects.recalculate_main_priority()

    assert len(mail.outbox) == 2  # sam с первой группы, gimli со второй

    for message, expected in zip(mail.outbox, ['sam', 'gimli']):
        assert len(message.to) == 1 and message.to[0] == '{}@example.yandex.ru'.format(expected)


@pytest.mark.parametrize('passport_logins', [['yndx-frodo'], ['yndx-frodo', 'yndx-frodo-other-login']])
def test_send_notification_about_awaiting_role_without_passport_login(pt1_system, arda_users, department_structure,
                                                                      passport_logins):
    set_workflow(pt1_system, group_code='approvers = []')
    sam = arda_users.sam
    frodo = arda_users.frodo
    for login in passport_logins:
        frodo.passport_logins.create(login=login, state='created', is_fully_registered=False)

    gimli = arda_users.gimli
    gimli.passport_logins.create(login='yndx-gimli-1', state='created', is_fully_registered=False)
    gimli.passport_logins.create(login='yndx-gimli-2', state='created', is_fully_registered=False)

    fellowship = department_structure['fellowship']
    fellowship.memberships.exclude(user__in=[frodo, sam, gimli]).update(state='inactive')
    # gimli письма не отправим, считаем что ему когда-то раньше уже ушло письмо
    fellowship.memberships.filter(user=gimli).update(notified_about_passport_login=True)

    Role.objects.request_role(frodo, fellowship, pt1_system, '', {'project': 'proj1', 'role': 'admin'}, None)
    passport_logins_count = len(passport_logins)

    if passport_logins_count < 2:
        # Если паспортных логинов не больше одного, то привязываем его к членству в группе и к роли, письмо не отправим
        # Отправим только одно письмо frodo о выдаче роли на группу
        assert len(mail.outbox) == 1
        message_about_role = mail.outbox[0]
    else:
        # У frodo два паспортных логина, мы не знаем какой из них выбрать
        # переведем роль в статус awaiting и отправим письмо о необходимости привязать паспортный логин
        assert len(mail.outbox) == 2
        message_about_group, message_about_role = mail.outbox
        assert message_about_group.to == ['frodo@example.yandex.ru']
        assert message_about_group.subject == 'Участие в группе требует указания паспортного логина'
        assert_contains(
            [
                'Вы входите в группу %s'
                % fellowship.name,
                'https://example.com/user/%s/groups#f-status-member=active,f-mode=all,f-group=%s'
                % (frodo.username, fellowship.external_id),
            ],
            message_about_group.body,
        )
    assert message_about_role.to == ['frodo@example.yandex.ru']
    assert message_about_role.subject == 'Test1 система. Новая роль'


def test_send_user_role_emails_many_approvers(simple_system, arda_users):
    varda = arda_users.varda
    frodo = arda_users.frodo
    set_workflow(
        simple_system,
        'approvers = [approver("legolas") | "gandalf", approver("aragorn") | "gimli" | "sauron"| '
        '"saruman"| "nazgul"| "witch-king-of-angmar"|"galadriel" | "manve" | "sam"]'
    )
    Role.objects.request_role(varda, frodo, simple_system, '', {'role': 'admin'}, {})
    assert_contains([
        '[gimli, sauron, saruman, nazgul, witch-king-of-angmar, galadriel или ещё 2 человека]',
        '(полный список можно посмотреть в карточке запроса)'],
        mail.outbox[-1].body
    )


def test_fields_data_in_email(complex_system, arda_users):
    frodo = arda_users.frodo

    assert len(mail.outbox) == 0
    assert Notice.objects.count() == 0

    set_workflow(complex_system, 'approvers = [approver("galadriel")]')
    Role.objects.request_role(
        requester=frodo,
        subject=frodo,
        system=complex_system,
        comment='Wanna role!',
        data={'project': 'subs', 'role': 'developer'},
        fields_data={'passport-login': 'frodo', 'field_1': '1'},
    )

    assert len(mail.outbox) == 2
    galadriel_mail, frodo_mail = mail.outbox

    assert_contains([
        'Complex система',
        'Проект: Подписки, Роль: Разработчик',
        '''Данные полей: {'field_1': '1'}''',
        'Паспортный логин: frodo',
        'Комментарий: Wanna role!',
        'Вы получили запрос, он ожидает вашего подтверждения',
        'https://example.com/queue/',
    ], galadriel_mail.body)

    assert_contains([
        'Вы запросили роль в системе "Complex система"',
        'Проект: Подписки, Роль: Разработчик',
        '''Данные полей: {'field_1': '1'}''',
        'Паспортный логин: frodo',
        'galadriel',
    ], frodo_mail.body)
