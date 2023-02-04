from datetime import datetime, date
from mock import patch
import pytest
import json

from django.conf import settings
from django.contrib.auth.models import Permission
from django.core.urlresolvers import reverse
from django.http.request import QueryDict

from staff.dismissal.tests.factories import DismissalFactory
from staff.lib.testing import StaffPhoneFactory, DepartmentRoleFactory, TableFactory, DepartmentStaffFactory
from staff.person.models import PHONE_TYPES

from staff.person_profile.views.cards.staff_cards import StaffCards


def get_gaps_mock(person_ids, observer_tz, **gap_data):
    return {
        p_id: {
            'name': 'absence',
            'date_from': datetime.now(),
            'date_to': datetime.now(),
            'right_edge': date.today(),
            'left_edge': date.today(),
            'full_day': True,
            'work_in_absence': False,
            'description': 'Test gap description',
            'id': 463860,
            'staff_id': p_id,
            'color': '#000000',
            **gap_data,
        } for p_id in person_ids
    }


@pytest.fixture
def request_observer_view(company, settings, rf):
    observer = company.persons['yandex-chief']
    observer.tz = 'Europe/Moscow'
    observer.save()

    request = rf.get(reverse('cards:staff_cards'))
    request.user = observer.user
    request.service_is_readonly = False
    request.META['HTTP_REFERER'] = 'https://any.yandex-team.ru/my'

    view = StaffCards.as_view()

    settings.STAFF_HOST = 'staff.test.yandex-team.ru'

    return request, observer, view


def test_staff_card_jsonp_format(company, mocked_mongo, request_observer_view):
    request, observer, view = request_observer_view

    person = company.persons['dep11-person']

    request.GET = QueryDict(person.login)

    response = view(request)
    content = response.content.decode('utf-8')

    bounds = ('callback(', ');')

    assert content.startswith(bounds[0])
    assert content.endswith(bounds[1])

    actual_content = content[len(bounds[0]): 0-len(bounds[1])]
    actual_content = json.loads(actual_content)

    assert person.login in actual_content
    assert actual_content[person.login]
    assert set(actual_content[person.login]) == {
        'login', 'last_name', 'first_name',
        'avatar', 'position',
        'staff_url', 'office', 'phones', 'jabber_status', 'work_email',
        'dep', 'gender', 'office_id', 'custom_work_email',
        'accounts',
        'office', 'last_office',
        'room', 'room_id',
        'is_memorial',
        'is_dismissed',
        'is_homie',
    }


@patch('staff.person_profile.cards.blocks._', lambda x: x)
def test_staff_card_for_regular_person(company, mocked_mongo, settings, request_observer_view):
    request, observer, view = request_observer_view

    person = company.persons['dep11-person']

    request.GET = QueryDict('format=json&{}'.format(person.login))

    response = view(request)
    content = json.loads(response.content)

    assert person.login in content
    assert content[person.login]['accounts'] == [
        {'name': 'api.StaffCards_Wiki', 'url': '//{}/~dep11-person'.format(settings.WIKI_HOST)},
        {'name': 'api.StaffCards_Diary', 'url': '//dep11-person.{}'.format(settings.ATUSHKA_HOST)},
        {'name': 'api.StaffCards_Meetings', 'url': '//{}/invite/my/dep11-person'.format(settings.CALENDAR_HOST)},
    ]
    assert content[person.login]['first_name'] == person.first_name
    assert content[person.login]['last_name'] == person.last_name
    assert content[person.login]['login'] == person.login
    assert content[person.login]['avatar'] == '//{}/user/avatar/dep11-person/100/square'.format(settings. CENTER_MASTER)
    assert content[person.login]['custom_work_email'] == 'dep11-person@yandex-team.ru'
    assert content[person.login]['dep'] == {'bg_color': '', 'fg_color': '', 'name': 'dep11'}
    assert content[person.login]['gender'] == 'M'
    assert content[person.login]['is_dismissed'] is False
    assert content[person.login]['is_homie'] is False
    assert content[person.login]['is_memorial'] is False
    assert content[person.login]['jabber_status'] == 'offline'
    assert content[person.login]['office'] == person.office.name
    assert content[person.login]['office_id'] == person.office.id
    assert content[person.login]['phones'] == {'mobile': '', 'work': None}
    assert content[person.login]['position'] == ''
    assert content[person.login]['staff_url'] == '//staff.test.yandex-team.ru/dep11-person'
    assert content[person.login]['work_email'] == 'dep11-person@yandex-team.ru'


@patch('staff.person_profile.cards.blocks._', lambda x: x)
def test_staff_card_for_person_with_phones_and_gaps(company, mocked_mongo, request_observer_view):
    request, observer, view = request_observer_view

    person = company.persons['dep11-person']

    StaffPhoneFactory(staff=person, number='+7 925 047-55-58', type=PHONE_TYPES.MOBILE)
    StaffPhoneFactory(staff=person, number='+380 96 117 4835', type=PHONE_TYPES.MOBILE)

    person.mobile_phone = '+7 925 047-55-58, +380 96 117 4835'
    person.work_phone = 3566
    person.position = 'Database [removal] specialist'
    person.save()

    dep = person.department
    dep.bg_color = '#ffffff'
    dep.fg_color = '#beaf00'
    dep.name = 'Custom department name'
    dep.save()

    request.GET = QueryDict('format=json&{}'.format(person.login))

    with patch('staff.person_profile.views.cards.base.get_gaps', get_gaps_mock):
        response = view(request)

    content = json.loads(response.content)

    assert person.login in content
    assert 'phones' in content[person.login]
    assert content[person.login]['phones'] == {
        'mobile': '+7 925 047-55-58',
        'work': person.work_phone,
    }
    assert 'dep' in content[person.login]
    assert content[person.login]['dep'] == {
        'bg_color': dep.bg_color,
        'fg_color': dep.fg_color,
        'name': dep.name,
    }
    assert content[person.login]['position'] == person.position
    assert content[person.login]['gap'] == {
        'url': '//gap.test.tools.yandex-team.ru/',
        'color': '#000000',
        'name': 'api.StaffCards_IsAbsent',
        'time': 'api.StaffCards_Today'
    }


@patch('staff.person_profile.cards.blocks._', lambda x: x)
def test_staff_card_for_person_with_covid_gap(company, mocked_mongo, request_observer_view):
    request, observer, view = request_observer_view

    person = company.persons['dep11-person']
    request.GET = QueryDict('format=json&{}'.format(person.login))

    with patch(
            'staff.person_profile.views.cards.base.get_gaps',
            lambda *args: get_gaps_mock(*args, name='illness', is_covid=True)
    ):
        response = view(request)
        content = json.loads(response.content)
        assert content[person.login]['gap'] == {
            'url': '//gap.test.tools.yandex-team.ru/',
            'color': '#000000',
            'name': 'api.StaffCards_IsIll_Covid',
            'time': 'api.StaffCards_Today'
        }

    with patch(
            'staff.person_profile.views.cards.base.get_gaps',
            lambda *args: get_gaps_mock(*args, name='illness', is_covid=False)
    ):
        response = view(request)
        content = json.loads(response.content)
        assert content[person.login]['gap'] == {
            'url': '//gap.test.tools.yandex-team.ru/',
            'color': '#000000',
            'name': 'api.StaffCards_IsIll',
            'time': 'api.StaffCards_Today'
        }


@patch('staff.person_profile.cards.blocks._', lambda x: x)
def test_staff_card_for_multiple_persons(company, mocked_mongo, request_observer_view):
    request, observer, view = request_observer_view

    person1 = company.persons['dep11-person']
    person2 = company.persons['dep1-chief']

    request.GET = QueryDict('{}&{}&format=json'.format(person1.login, person2.login))

    response = view(request)
    content = json.loads(response.content)

    assert person1.login in content
    assert person2.login in content

    assert content[person1.login]['accounts'] == [
        {'name': 'api.StaffCards_Wiki', 'url': '//{}/~dep11-person'.format(settings.WIKI_HOST)},
        {'name': 'api.StaffCards_Diary', 'url': '//dep11-person.{}'.format(settings.ATUSHKA_HOST)},
        {'name': 'api.StaffCards_Meetings', 'url': '//{}/invite/my/dep11-person'.format(settings.CALENDAR_HOST)}
    ]
    assert content[person1.login]['first_name'] == person1.first_name
    assert content[person1.login]['last_name'] == person1.last_name
    assert content[person1.login]['login'] == person1.login
    assert content[person1.login]['avatar'] == '//{}/user/avatar/dep11-person/100/square'.format(settings.CENTER_MASTER)
    assert content[person1.login]['custom_work_email'] == 'dep11-person@yandex-team.ru'
    assert content[person1.login]['dep'] == {'bg_color': '', 'fg_color': '', 'name': 'dep11'}
    assert content[person1.login]['gender'] == person1.gender
    assert content[person1.login]['is_dismissed'] is False
    assert content[person1.login]['is_homie'] is False
    assert content[person1.login]['is_memorial'] is False
    assert content[person1.login]['jabber_status'] == 'offline'
    assert content[person1.login]['office'] == person1.office.name
    assert content[person1.login]['office_id'] == person1.office.id
    assert content[person1.login]['phones'] == {'mobile': '', 'work': None}
    assert content[person1.login]['position'] == ''
    assert content[person1.login]['staff_url'] == '//staff.test.yandex-team.ru/dep11-person'
    assert content[person1.login]['work_email'] == 'dep11-person@yandex-team.ru'

    assert content[person2.login]['accounts'] == [
        {'name': 'api.StaffCards_Wiki', 'url': '//{}/~dep1-chief'.format(settings.WIKI_HOST)},
        {'name': 'api.StaffCards_Diary', 'url': '//dep1-chief.{}'.format(settings.ATUSHKA_HOST)},
        {'name': 'api.StaffCards_Meetings', 'url': '//{}/invite/my/dep1-chief'.format(settings.CALENDAR_HOST)}
    ]

    assert content[person2.login]['login'] == person2.login
    assert content[person2.login]['first_name'] == person2.first_name
    assert content[person2.login]['last_name'] == person2.last_name
    assert content[person2.login]['avatar'] == '//{}/user/avatar/dep1-chief/100/square'.format(settings. CENTER_MASTER)
    assert content[person2.login]['custom_work_email'] == 'dep1-chief@yandex-team.ru'
    assert content[person2.login]['dep'] == {'bg_color': '', 'fg_color': '', 'name': person2.department.name}
    assert content[person2.login]['gender'] == 'M'
    assert content[person2.login]['is_dismissed'] is False
    assert content[person2.login]['is_homie'] is False
    assert content[person2.login]['is_memorial'] is False
    assert content[person2.login]['jabber_status'] == 'offline'
    assert content[person2.login]['office'] == person2.office.name
    assert content[person2.login]['office_id'] == person2.office.id
    assert content[person2.login]['phones'] == {'mobile': '', 'work': None}
    assert content[person2.login]['position'] == ''
    assert content[person2.login]['staff_url'] == '//staff.test.yandex-team.ru/dep1-chief'
    assert content[person2.login]['work_email'] == 'dep1-chief@yandex-team.ru'


@patch('staff.person_profile.cards.blocks._', lambda x: x)
def test_staff_card_for_dismissed_person(company, mocked_mongo, request_observer_view):
    request, observer, view = request_observer_view

    dismissed_person = company.persons['dep111-person']
    dismissed_person.is_dismissed = True
    DismissalFactory(staff=dismissed_person)
    dismissed_person.save()

    request.GET = QueryDict('{}&format=json'.format(dismissed_person.login))

    response = view(request)
    content = json.loads(response.content)

    assert dismissed_person.login in content
    assert content[dismissed_person.login]['accounts'] == [
        {'name': 'api.StaffCards_Wiki', 'url': '//{}/~dep111-person'.format(settings.WIKI_HOST)},
        {'name': 'api.StaffCards_Diary', 'url': '//dep111-person.{}'.format(settings.ATUSHKA_HOST)},
        {'name': 'api.StaffCards_Meetings', 'url': '//{}/invite/my/dep111-person'.format(settings.CALENDAR_HOST)},
    ]
    assert content[dismissed_person.login]['login'] == dismissed_person.login
    assert not content[dismissed_person.login]['first_name']
    assert not content[dismissed_person.login]['last_name']
    expected_url = f'//{settings.CENTER_MASTER}/user/avatar/dep111-person/100/square'
    assert content[dismissed_person.login]['avatar'] == expected_url
    assert content[dismissed_person.login]['custom_work_email'] == 'dep111-person@yandex-team.ru'
    assert content[dismissed_person.login]['dep'] == {
        'bg_color': '#909090',
        'fg_color': '#ffffff',
        'name': 'api.StaffCards_FormerEmployee',
    }
    assert content[dismissed_person.login]['gender'] == 'M'
    assert content[dismissed_person.login]['is_dismissed'] is True
    assert content[dismissed_person.login]['is_memorial'] is False
    assert content[dismissed_person.login]['jabber_status'] == 'offline'
    assert content[dismissed_person.login]['position'] == ''
    assert content[dismissed_person.login]['staff_url'] == '//staff.test.yandex-team.ru/dep111-person'
    assert content[dismissed_person.login]['work_email'] == 'dep111-person@yandex-team.ru'


def test_card_is_not_available_for_unprivileged_externals(company, rf, mocked_mongo):
    observer = company.persons['ext-person']
    observer.tz = 'Europe/Moscow'

    observer.save()

    target = company.persons['dep11-person']

    StaffPhoneFactory(staff=target, number='+7 925 047-55-58', type=PHONE_TYPES.MOBILE)
    target.mobile_phone = '+7 925 047-55-58'
    target.work_phone = 3566
    target.position = 'Database [removal] specialist'
    target.save()

    request = rf.get(reverse('cards:staff_cards'))
    request.user = observer.user
    request.service_is_readonly = False
    request.META['HTTP_REFERER'] = 'https://any.yandex-team.ru/my'
    request.GET = QueryDict('format=json&{}'.format(target.login))

    view = StaffCards.as_view()
    response = view(request)

    content = json.loads(response.content)

    assert content == {}


def test_phone_is_not_visible_for_privileged_externals(company, rf, mocked_mongo):
    observer = company.persons['ext-person']
    observer.tz = 'Europe/Moscow'

    role = DepartmentRoleFactory(id='TEST_ROLE_NAME')
    role.permissions.add(Permission.objects.get(codename='can_view_profiles'))
    DepartmentStaffFactory(staff=observer, department=company.dep11, role=role)

    observer.user.user_permissions.add(Permission.objects.get(codename='can_view_profiles'))
    observer.save()

    target = company.persons['dep11-person']

    StaffPhoneFactory(staff=target, number='+7 925 047-55-58', type=PHONE_TYPES.MOBILE)
    target.table = TableFactory()
    target.mobile_phone = '+7 925 047-55-58'
    target.work_phone = 3566
    target.position = 'Database [removal] specialist'
    target.save()

    request = rf.get(reverse('cards:staff_cards'))
    request.user = observer.user
    request.service_is_readonly = False
    request.META['HTTP_REFERER'] = 'https://any.yandex-team.ru/my'
    request.GET = QueryDict('format=json&{}'.format(target.login))

    view = StaffCards.as_view()
    response = view(request)

    content = json.loads(response.content)

    assert target.login in content
    assert 'phones' not in content[target.login]

    assert 'last_office' in content[target.login] and content[target.login]['last_office'] == {'ago': '', 'office': ''}

    assert 'jabber_status' not in content[target.login]
    assert 'office' in content[target.login] and content[target.login]['office'] == ''

    assert 'office_id' not in content[target.login]
