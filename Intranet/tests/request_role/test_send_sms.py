# coding: utf-8
import random

import mock
from constance import config
from mock import patch, MagicMock
import pytest
from dbtemplates.models import Template

from django.conf import settings
from django.core import mail

from idm.core.workflow.exceptions import NoMobilePhoneError
from idm.core.models import Action, Approve, ApproveRequest, Role
from idm.tests.templates.utils import render_template
from idm.tests.utils import set_workflow, refresh, Response, mock_ids_repo, capture_requests, create_system, \
    random_slug, create_user, DEFAULT_WORKFLOW
from idm.testenv.models import AllowedPhone
from idm.notification.sms import send_sms

# разрешаем использование базы в тестах
pytestmark = [pytest.mark.django_db]

GOOD_ANSWER = '''<?xml version="1.0" encoding="utf-8"?>
<doc>
    <message-sent id="2091000000666809" />
</doc>'''


def test_request_role_with_sms_without_mobile_phone(simple_system, arda_users):
    """Проверяем добавление роли (должно сфейлиться) при указании параметра send_sms
    и отсутствии телефона сотрудника на staff"""

    Action.objects.all().delete()  # удалим экшены, которые были созданы при инициализации фикстур
    set_workflow(simple_system, "send_sms = True; approvers = []")

    frodo = arda_users.frodo
    assert frodo.mobile_phone is None
    expected = (
        'Согласно правилу workflow необходимо послать пользователю frodo sms, '
        'однако у него не указан на стаффе мобильный телефон'
    )

    with mock_ids_repo('staff.person', {}):
        with capture_requests({}):
            with pytest.raises(NoMobilePhoneError) as excinfo:
                Role.objects.request_role(frodo, frodo, simple_system, '', {'role': 'manager'}, None)

    assert str(excinfo.value) == expected

    assert frodo.roles.count() == 0
    assert len(mail.outbox) == 0
    assert list(Action.objects.values_list('action', flat=True)) == ['change_workflow']  # никаких экшенов про роль
    assert Approve.objects.count() == 0


def test_request_role_with_sms_with_mobile_phone(simple_system, arda_users, monkeypatch):
    """Проверяем добавление роли при указании параметра send_sms и наличии телефона сотрудника на staff"""
    set_workflow(simple_system, "send_sms = True; approvers = []")

    phone_number = '+71234567890'
    AllowedPhone.objects.create(mobile_phone=phone_number).save()

    frodo = arda_users.frodo

    frodo.mobile_phone = phone_number
    frodo.save()

    assert 0 == frodo.roles.count()
    assert 0 == len(mail.outbox)

    request_params = {}

    def request(response):
        def wrapper(*args, **kwargs):
            request_params.update(kwargs)
            return Response(200, response)
        return wrapper

    xml = '''<?xml version="1.0" encoding="utf-8"?>
<doc>
    <message-sent id="2091000000666809" />
</doc>'''
    monkeypatch.setattr('requests.sessions.Session.request', request(xml))

    person_data = {
        'phones': [
            {
                'number': phone_number,
                'is_main': True,
            }
        ]
    }
    with mock_ids_repo('staff.person', person_data):
        Role.objects.request_role(frodo, frodo, simple_system, '', {'role': 'manager'}, None)

    assert request_params['params']['phone'] == phone_number

    frodo = refresh(frodo)
    assert frodo.roles.count() == 1
    role = frodo.roles.all()[0]
    assert 'granted' == role.state
    assert role.is_active
    assert Action.objects.count() == 8
    assert len(mail.outbox) == 1
    assert role.requests.count() == 1

    # неотправка СМС не фейлит роль
    xml = '''<?xml version="1.0" encoding="utf-8"?>
<doc>
    <error>User ID not specified</error>
    <errorcode>NOUID</errorcode>
</doc>'''
    monkeypatch.setattr('requests.sessions.Session.request', request(xml))

    with mock_ids_repo('staff.person', person_data):
        Role.objects.request_role(frodo, frodo, simple_system, '', {'role': 'admin'}, None)

    assert request_params['params']['phone'] == phone_number

    terran = refresh(frodo)
    assert terran.roles.count() == 2
    assert ['granted', 'granted'] == [r.state for r in terran.roles.all()]
    role = terran.roles.order_by('-pk').first()
    assert 'granted' == role.state
    assert role.is_active
    assert Action.objects.count() == 13
    assert len(mail.outbox) == 2
    assert role.requests.count() == 1
    assert role.requests.get().approves.count() == 0


def test_request_role_with_sms_with_outdated_mobile_phone(simple_system, arda_users, monkeypatch):
    """В базе хранится старый номер телефона, на стаффе - новый"""
    set_workflow(simple_system, "send_sms = True; approvers = []")

    old_phone_number = '+71234567890'
    new_phone_number = '+78005553535'
    AllowedPhone.objects.create(mobile_phone=old_phone_number).save()
    AllowedPhone.objects.create(mobile_phone=new_phone_number).save()
    person_data = {
        'phones': [
            {
                'number': new_phone_number,
                'is_main': True,
            }
        ]
    }

    frodo = arda_users.frodo
    sam = arda_users.sam
    gandalf = arda_users.gandalf

    frodo.mobile_phone = old_phone_number
    frodo.save()
    sam.mobile_phone = old_phone_number
    sam.save()

    assert frodo.roles.count() == 0
    assert len(mail.outbox) == 0

    request_params = {}

    def request(response):
        def wrapper(*args, **kwargs):
            request_params.update(kwargs)
            return Response(200, response)
        return wrapper

    xml = '''<?xml version="1.0" encoding="utf-8"?>
<doc>
    <message-sent id="2091000000666809" />
</doc>'''
    monkeypatch.setattr('requests.sessions.Session.request', request(xml))

    with mock_ids_repo('staff.person', None, ValueError):
        Role.objects.request_role(gandalf, frodo, simple_system, '', {'role': 'manager'}, None)
    assert request_params['params']['phone'] == old_phone_number

    with mock_ids_repo('staff.person', person_data):
        Role.objects.request_role(gandalf, sam, simple_system, '', {'role': 'admin'}, None)
    assert request_params['params']['phone'] == new_phone_number


def test_request_role_with_sms_with_mobile_phone_custom_template(simple_system, arda_users, monkeypatch):
    """Проверяем добавление роли при указании параметра send_sms и наличии телефона сотрудника на staff, плюс кастомный шаблон"""
    set_workflow(simple_system, "send_sms = True; approvers = []")

    phone_number = '+71234567890'
    AllowedPhone.objects.create(mobile_phone=phone_number).save()

    frodo = arda_users.frodo
    frodo.mobile_phone = phone_number
    frodo.save()

    assert frodo.roles.count() == 0
    assert len(mail.outbox) == 0

    template = Template(name='sms/role_granted_simple.txt', content='Роль выдана!!!')
    template.save()

    request_params = {}

    def request(*args, **kwargs):
        url = kwargs.get('url')
        if 'center' in url:
            return Response(200, '{"mobile_phone": ""}')

        else:
            request_params['url'] = url
            request_params.update(kwargs)
            return Response(200, '''<?xml version="1.0" encoding="utf-8"?>
                                    <doc>
                                        <message-sent id="2091000000666809" />
                                    </doc>''')

    monkeypatch.setattr('requests.sessions.Session.request', request)

    with mock_ids_repo('staff.person', None, ValueError):
        Role.objects.request_role(frodo, frodo, simple_system, '', {'role': 'manager'}, None)

    assert request_params['params']['phone'] == phone_number
    assert settings.SEND_SMS_URL.split('{params}')[0] in request_params['url']

    frodo = refresh(frodo)
    assert frodo.roles.count() == 1
    role = frodo.roles.get()
    assert 'granted' == role.state
    assert role.is_active
    assert Action.objects.count() == 8
    assert len(mail.outbox) == 1
    assert role.requests.count() == 1
    assert role.requests.get().approves.count() == 0

    # неотправка СМС не фейлит роль
    request_params = {}

    def request(*args, **kwargs):
        url = kwargs.get('url')
        if 'center' in url:
            return Response(200, '{"mobile_phone": ""}')

        else:
            request_params['url'] = url
            request_params.update(kwargs)
            return Response(200, '''<?xml version="1.0" encoding="utf-8"?>
                                    <doc>
                                        <error>User ID not specified</error>
                                        <errorcode>NOUID</errorcode>
                                    </doc>''')

    monkeypatch.setattr('requests.request', request)

    with mock_ids_repo('staff.person', None, ValueError):
        Role.objects.request_role(frodo, frodo, simple_system, '', {'role': 'admin'}, None)

    assert request_params['params']['phone'] == phone_number
    assert settings.SEND_SMS_URL.split('{params}')[0] in request_params['url']

    frodo = refresh(frodo)
    assert frodo.roles.count() == 2
    assert ['granted', 'granted'] == [r.state for r in frodo.roles.all()]
    role = frodo.roles.exclude(pk=role.pk).get()
    assert 'granted' == role.state
    assert role.is_active
    assert Action.objects.count() == 13
    assert len(mail.outbox) == 2
    assert role.requests.count() == 1
    assert Approve.objects.filter(role_request__role=role).count() == 0


@pytest.mark.parametrize('send_sms', [True, False])
def test_role_request_with_send_sms_flag(simple_system, arda_users, send_sms):
    """Параметр send_sms должен определять отправку sms"""
    set_workflow(simple_system, 'send_sms = {}; approvers = [\'gandalf\']'.format(send_sms))

    phone_number = '+71234567890'
    AllowedPhone.objects.create(mobile_phone=phone_number).save()

    frodo = arda_users.frodo
    gandalf = arda_users.gandalf
    frodo.mobile_phone = phone_number
    frodo.save()

    template = Template(name='sms/role_granted_simple.txt', content='Роль выдана!!!')
    template.save()

    with patch('requests.sessions.Session.request') as mock_requests:
        with mock_ids_repo('staff.person', None, ValueError):
            role = Role.objects.request_role(frodo, frodo, simple_system, '', {'role': 'admin'}, None)
            assert role.state == 'requested'
            assert mock_requests.call_count == 0
            ApproveRequest.objects.select_related_for_set_decided().get(approver=gandalf).set_approved(gandalf)
            role.refresh_from_db()
            assert role.state == 'granted'

    expected_call_count = 1 if send_sms else 0
    assert mock_requests.call_count == expected_call_count


@pytest.mark.parametrize('send_sms', [True, False])
def test_role_request_with_send_sms_flag_for_group(simple_system, department_structure, arda_users, send_sms):
    """Параметр send_sms должен определять отправку sms в том числе и для групповой роли"""
    set_workflow(
        simple_system,  
        group_code='send_sms = {}; approvers = [\'gandalf\']'.format(send_sms)
    )

    phone_number = '+71234567890'
    AllowedPhone.objects.create(mobile_phone=phone_number).save()

    sauron = arda_users.sauron
    gandalf = arda_users.gandalf
    sauron.mobile_phone = phone_number
    sauron.save()

    template = Template(name='sms/role_granted_simple.txt', content='Роль выдана!!!')
    template.save()
    
    associations_group = department_structure.associations

    with patch('requests.sessions.Session.request') as mock_requests:
        with mock_ids_repo('staff.person', None, ValueError):
            role = Role.objects.request_role(arda_users.frodo, associations_group, simple_system, '', {'role': 'admin'}, None)
            assert role.state == 'requested'
            assert mock_requests.call_count == 0
            ApproveRequest.objects.select_related_for_set_decided().get(approver=gandalf).set_approved(gandalf)
            role.refresh_from_db()
            assert role.state == 'granted'
            user_role = Role.objects.get(user=sauron, parent=role)
            assert user_role.state == 'granted'

    expected_call_count = 1 if send_sms else 0
    assert mock_requests.call_count == expected_call_count


@pytest.mark.parametrize('env', ['testing', 'production'])
@pytest.mark.parametrize('whitelist', [True, False])
def test_sms_testing_whitelist(env, whitelist, monkeypatch):
    mobile_phone = '+71234567890'
    if whitelist:
        AllowedPhone.objects.create(mobile_phone=mobile_phone).save()
    response = MagicMock()
    message_sent_id = '123456789012345'
    content = """<?xml version="1.0" encoding="utf-8"?>
           <doc>
           <message-sent id="{}" />
           <gates ids="15" />
           </doc>
    """.format(message_sent_id)
    response.content = content.encode('utf-8')
    message = 'message'
    monkeypatch.setattr('yenv.type', env)
    with capture_requests(answer=response) as mocked:
        result, info = send_sms(message, mobile_phone, '<username>')
    if env == 'production':
        assert result
        assert info == message_sent_id
        assert mocked['http_get'].call_count == 1
        assert mocked['http_get'].call_args[1]['params']['text'] == message
    elif whitelist:
        assert result
        assert info == message_sent_id
        assert mocked['http_get'].call_count == 1
        assert mocked['http_get'].call_args[1]['params']['text'] == '[Testing] ' + message
    else:
        assert not result
        assert info == 'Mobile phone is not in testing whitelist'
        assert mocked['http_get'].call_count == 0


@pytest.mark.parametrize('empty_secret_context', [True, False])
def test_yandex_id_role_with_empty_secret_context(empty_secret_context):
    system = create_system(
        config.YANDEX_ID_SYSTEM_SLUG,
        public=True,
        workflow='\n'.join([DEFAULT_WORKFLOW, 'send_sms = True']),
    )
    user = create_user()

    node_data = system.nodes.last().data
    phone = f'+7{random.randint(10**10, 10**11 - 1)}'
    with mock.patch('idm.core.tasks.roles.send_sms', return_value=(True, '')) as send_sms_mock, \
            mock.patch('idm.core.plugins.dumb.Plugin.add_role') as add_role_mock,\
            mock.patch('idm.users.models.User.actual_mobile_phone', new_callable=mock.PropertyMock, return_value=phone):

        add_role_mock.return_value = {'data': node_data}
        if not empty_secret_context:
            add_role_mock.return_value['context'] = {random_slug(): random_slug()}

        Role.objects.request_role(create_user(), user, system, '', node_data)

    if empty_secret_context:
        send_sms_mock.assert_not_called()
    else:
        send_sms_mock.assert_called_once_with(mock.ANY,phone, user.username)