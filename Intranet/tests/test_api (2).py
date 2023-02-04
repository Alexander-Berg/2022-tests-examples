from datetime import datetime, date

import pytest
import json

from mock import patch
from waffle.models import Switch

from django.core.urlresolvers import reverse
from staff.lib.testing import (
    StaffFactory,
    OfficeFactory,
    DepartmentFactory,
    OrganizationFactory,
    GroupFactory,
    TableFactory,
    FloorFactory,
    TokenFactory,
    RouteFactory,
)

from staff.achievery.tests.factories.model import (
    AchievementFactory,
    IconFactory,
)

from staff.person.models import (
    Staff,
    FAMILY_STATUS,
    DOMAIN,
    EDU_STATUS,
    EDU_DIRECTION,
    EMPLOYMENT,
    GENDER,
)
from staff.groups.models import GROUP_TYPE_CHOICES
from staff.departments.models import DepartmentRoles


TEST_WORK_PHONE = 3566


class MockWelcomeEmailNotificationClass():
    def __init__(self):
        self.person = None
        self.init_data = {}
        self.send_data = {}
        self.mails_sent = 0

    def send(self, **kwargs):
        self.send_data = kwargs
        self.mails_sent += 1

    def __call__(self, person, *args, **kwargs):
        self.person = person
        self.init_data = kwargs
        return self


@pytest.fixture
def initial(db, settings):
    settings.DEBUG = True
    RouteFactory(target='@')
    initial_data = {}
    initial_data['token'] = TokenFactory(
        token='killa',
        ips='127.0.0.1',
        hostnames='localhost'
    )
    initial_data['login'] = 'burr'
    initial_data['department'] = DepartmentFactory(
        name='Яндекс',
        name_en='Yandex',
        code='yandex',
        url='yandex'
    )
    initial_data['office'] = OfficeFactory()
    initial_data['organization'] = OrganizationFactory()
    initial_data['table'] = TableFactory(
        floor=FloorFactory(office=initial_data['office'])
    )
    initial_data['root_department_group'] = GroupFactory(
        name='__departments__',
        department=None,
        service_id=None,
        parent=None,
        type=GROUP_TYPE_CHOICES.DEPARTMENT,
    )
    initial_data['yandex'] = GroupFactory(
        name='Яндекс',
        department=initial_data['department'],
        service_id=None,
        parent=initial_data['root_department_group'],
        type=GROUP_TYPE_CHOICES.DEPARTMENT,
    )

    initial_data['yandexmoney'] = DepartmentFactory(
        name='ЯндексДеньги',
        name_en='YandexMoney',
        code='yamoney',
        url='yandex_money',
        parent=initial_data['department'],
    )
    GroupFactory(
        name='ЯДеньги',
        department=initial_data['yandexmoney'],
        service_id=None,
        parent=initial_data['root_department_group'],
        type=GROUP_TYPE_CHOICES.DEPARTMENT,
    )

    StaffFactory(login=settings.ACHIEVERY_ROBOT_PERSON_LOGIN)
    beginner_ach = AchievementFactory(id=settings.ACHIEVEMENT_BEGINNER_ID)
    restored_ach = AchievementFactory(id=settings.ACHIEVEMENT_RESTORED_ID)
    employee_ach = AchievementFactory(id=settings.ACHIEVEMENT_EMPLOYEE_ID)
    IconFactory(achievement=beginner_ach, level=-1)
    IconFactory(achievement=restored_ach, level=-1)
    IconFactory(achievement=employee_ach, level=1)
    RouteFactory(transport_id='email', params='{}')

    initial_data['max_person_data'] = {
        'uid': 'testuidforstaff',
        'guid': 'testguidforstaff',
        'last_name': 'Бурундук',
        'first_name': 'Василий',
        'first_name_en': 'Vasyly',
        'last_name_en': 'Bouroonduck',
        'middle_name': 'Алибабаевич',
        'family_status': FAMILY_STATUS.SINGLE,
        'edu_place': 'НИИ Заборостроеия',
        'edu_status': EDU_STATUS.SPECIALIST,
        'edu_direction': EDU_DIRECTION.TECHNICAL,
        'edu_date': date.today(),
        'position': 'Заборостроитель',
        'employment': EMPLOYMENT.FULL,
        'department': initial_data['department'].id,
        'office': initial_data['office'].id,
        'table': initial_data['table'].id,
        'login': initial_data['login'],
        'login_mail': initial_data['login'],
        'domain': DOMAIN.YAMONEY_RU,
        'login_crm': initial_data['login'],
        'join_at': date(2000, 1, 1),
        'birthday': date.today(),
        'login_passport': initial_data['login'],
        'mobile_phone': '+7 (900) 123-45-67, +7 (900) 124-45-67',
        'home_email': '%s@domain.com' % initial_data['login'],
        'address': '',
        'organization': initial_data['organization'].id,
        'gender': GENDER.MALE,
        'lang_ui': 'ru',
        'lang_content': 'en',
        'native_lang': 'tr',
        'photo': open(__file__.replace('tests.pyc', '1px.png')),
        'work_phone': TEST_WORK_PHONE,
    }
    return initial_data


@pytest.fixture
def change_roles_initial(kinds):
    yandex = DepartmentFactory(
        name='Яндекс',
        name_en='Yandex',
        code='yandex',
        url='yandex'
    )
    yandexmoney = DepartmentFactory(
        name='ЯндексДеньги',
        name_en='YandexMoney',
        code='yamoney',
        url='yandex_money',
        parent=yandex,
    )
    person1 = StaffFactory(
        login='login1',
        department=yandex
    )
    person2 = StaffFactory(
        login='login2',
        department=yandexmoney
    )

    def wrong_login_msg(login):
        return {'login': ['Нет сотрудника с логином %s' % login]}

    def wrong_dep_url_msg(url):
        return {'url': ['Нет подразделения с url %s' % url]}

    def bad_person_department(person, dep):
        return {'__all__': ['Сотрудник %s не входит в подразделение %s' % (person, dep)]}

    return locals()


@pytest.fixture()
def change_staff_initial(db):
    dep = DepartmentFactory(url='..')
    return {
        'departemnt': dep,
        'token': TokenFactory(token='killa', ips='127.0.0.1', hostnames='localhost'),
        'login': 'burr',
        'gorilla': StaffFactory(
            login='gorilla',
            lang_ui='ru',
            tz='Africa/Abidjan',
            department=dep,
        )
    }


@pytest.fixture
def token(db):
    return TokenFactory(
        token='killa',
        ips='127.0.0.1',
        hostnames='localhost'
    )


@pytest.fixture
def patched_ad(monkeypatch):
    monkeypatch.setattr(
        'staff.person.ad.get_ad_person_data',
        lambda no_matter_login: {
           'work_phone': TEST_WORK_PHONE,
           'has_exchange': False,
           'passwd_set_at': datetime.now(),
        }
    )


@pytest.fixture
def patched_notification(monkeypatch):
    welcome_email_notification = MockWelcomeEmailNotificationClass()
    monkeypatch.setattr(
        'staff.person.notifications.WelcomeMailNotification',
        welcome_email_notification
    )
    return welcome_email_notification


def test_qrcode_returns_not_found_on_dismissed_on_rkn_mode(client, token):
    StaffFactory(login='tester', is_dismissed=True)
    Switch(name='rkn_mode', active=True).save()
    url = reverse('api-get_qr', args=['tester'])
    response = client.get(url + '?token=killa')
    assert response.status_code == 404


def test_qrcode_returns_found_on_dismissed(client, token):
    StaffFactory(login='tester', is_dismissed=True)
    url = reverse('api-get_qr', args=['tester'])
    with patch('staff.vcard.vcard_generator._get_raw_photo'):
        response = client.get(url + '?token=killa')
    assert response.status_code == 200


def test_vcard_returns_empty_content_on_dismissed_on_rkn_mode(client, token):
    StaffFactory(login='tester', is_dismissed=True)
    Switch(name='rkn_mode', active=True).save()
    url = reverse('api-get_vcard', args=['tester'])
    response = client.get(url + '?token=killa')
    assert len(response.content) == 0


def test_vcard_returns_nonempty_content(client, token):
    StaffFactory(login='tester', is_dismissed=True)
    url = reverse('api-get_vcard', args=['tester'])

    with patch('staff.vcard.vcard_generator._get_raw_photo'):
        response = client.get(url + '?token=killa')

    assert len(response.content) > 0


def test_invalid_login(change_staff_initial, client):
    data = {'token': change_staff_initial['token'].token, 'tz': 'Europe/Helsinki'}
    url = reverse('api-change_staff', args=['unknown'])
    response = client.post(url, data)
    assert response.status_code == 404
    gorilla = Staff.objects.get(login='gorilla')
    assert gorilla.tz == 'Africa/Abidjan'


def test_invalid_request(change_staff_initial, client):
    data = {'token': change_staff_initial['token'].token, 'timezone': 'Europe/Helsinki'}
    url = reverse('api-change_staff', args=['gorilla'])
    response = client.post(url, data)
    assert response.status_code == 409
    gorilla = Staff.objects.get(login='gorilla')
    assert gorilla.tz == 'Africa/Abidjan'


def test_all_correct(change_staff_initial, client):
    data = {
        'token': change_staff_initial['token'].token,
        'tz': 'Europe/Moscow',
        'lang_ui': 'en'
    }
    url = reverse('api-change_staff', args=['gorilla'])
    response = client.post(url, data)
    assert response.status_code == 200
    gorilla = Staff.objects.get(login='gorilla')
    assert gorilla.tz == 'Europe/Moscow'
    assert gorilla.lang_ui == 'en'


def test_correct_tz(change_staff_initial, client):
    data = {
        'token': change_staff_initial['token'].token,
        'tz': 'Europe/Moscow',
    }
    url = reverse('api-change_staff', args=['gorilla'])
    response = client.post(url, data)
    assert response.status_code == 200
    gorilla = Staff.objects.get(login='gorilla')
    assert gorilla.tz == 'Europe/Moscow'
    assert gorilla.lang_ui == 'ru'


def test_correct_lang_ui(change_staff_initial, client):
    data = {
        'token': change_staff_initial['token'].token,
        'lang_ui': 'en'
    }
    url = reverse('api-change_staff', args=['gorilla'])
    response = client.post(url, data)
    assert response.status_code == 200
    gorilla = Staff.objects.get(login='gorilla')
    assert gorilla.tz == 'Africa/Abidjan'
    assert gorilla.lang_ui == 'en'


def test_invalid_post_data(change_staff_initial, client):
    data = {
        'token': change_staff_initial['token'].token,
        'tz': 'Europe',
        'lang_ui': 'gb',
    }
    url = reverse('api-change_staff', args=['gorilla'])
    response = client.post(url, data)
    assert response.status_code == 409
    gorilla = Staff.objects.get(login='gorilla')
    assert gorilla.tz == 'Africa/Abidjan'
    assert gorilla.lang_ui == 'ru'


def get_url(role, department_url):
    view_name = {
        DepartmentRoles.CHIEF.value: 'departments_set-chief',
        DepartmentRoles.DEPUTY.value: 'departments_set-deputy',
    }
    return reverse(
        view_name.get(role),
        args=[department_url, ]
    )


def set_head(client, role, department, person=None):
    def role_name(role_id):
        return DepartmentRoles.get_name(role_id).lower()

    assert getattr(department, role_name(role)) != person
    url = get_url(role, department.url)
    if person:
        result = client.post(url, {'login': person.login})
    else:
        result = client.post(url)
    result = json.loads(result.content)
    if result['result'] == 'ok':
        assert getattr(department, role_name(role)) == person
    return result


DEPARTMENT_ROLES_TO_TEST = [
    (code, text)
    for code, text in DepartmentRoles.choices()
    if code in [DepartmentRoles.CHIEF.value, DepartmentRoles.DEPUTY.value]
]


def test_set_head(change_roles_initial, client):
    for role, _ in DEPARTMENT_ROLES_TO_TEST:
        result = set_head(
            client,
            role,
            change_roles_initial['yandexmoney'],
            change_roles_initial['person2']
        )
        assert result['result'] == 'ok'


def test_wrong_user(change_roles_initial, client):
    wrong = 'wrong_login'
    for role, _ in DEPARTMENT_ROLES_TO_TEST:
        url = get_url(role, change_roles_initial['yandexmoney'].url)
        result = json.loads(client.post(url, {'login': wrong}).content)
        assert result['result'] == 'error'
        assert result['errors'] == change_roles_initial['wrong_login_msg'](wrong)


def test_wrong_dep_url(change_roles_initial, client):
    wrong = 'wrong_dep_url'
    for role, _ in DEPARTMENT_ROLES_TO_TEST:
        url = get_url(role, wrong)
        result = json.loads(
            client.post(url, {'login': change_roles_initial['person2'].login}).content
        )
        assert result['result'] == 'error'
        assert result['errors'] == change_roles_initial['wrong_dep_url_msg'](wrong)


def test_delete_head(change_roles_initial, client):
    for role, _ in DEPARTMENT_ROLES_TO_TEST:
        set_head(
            client,
            role,
            change_roles_initial['yandexmoney'],
            change_roles_initial['person2']
        )
        set_head(
            client,
            role,
            change_roles_initial['yandexmoney'],
            person=None
        )
