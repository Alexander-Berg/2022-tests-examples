# coding: utf-8
from __future__ import unicode_literals

import pytest

from staff_api.v3_0.idm.workflows.idm_context import IDMUser
from staff_api.v3_0.idm.workflows.personal import (
    get_staffapi_fields,
    fields_intersects,
    parse_url,
    is_safe_to_approve_partial_access,
    PERSON,
)

fields_set1 = {
    'official.is_dismissed',
    'official.is_robot',
    'login',
    'name.last',
    'yandex',
}
fields_set2 = {
    'id',
    'name.first.en',
    'official.affiliation',
}
fields_set3 = {
    'name.last.en',
    'official'
}

intersections = [
    (fields_set1, fields_set2, False),
    (fields_set1, fields_set3, True),
    (fields_set2, fields_set3, True),
    (fields_set1, set(), False),
    (fields_set1, fields_set1, True),
    (fields_set1, {'login'}, True),
    (fields_set1, {'name.last.en'}, True),
    (fields_set1, {'name.first'}, False),
    (fields_set1, {'name.first.en'}, False),
]

@pytest.mark.parametrize('set1, set2, result', intersections)
def test_fields_intersects(set1, set2, result):
    assert fields_intersects(set1, set2) is result


url_with_fields = [
    (
        'staff-api.yandex-team.ru/v3/persons?_limit=3&login=alexrasyuk,terrmit&_fields=uid,'
        'work_email,phones.is_main,phones.number,phones.type,official,accounts,name',
        {'phones.type', 'phones.number', 'uid', 'official', 'accounts', 'phones.is_main',
        'login', 'work_email', 'name'}
    ),
    (
        'https://staff-api.yandex-team.ru/v3/groupmembership?_fields=person.login,group.department.id,'
        'id&_limit=1000&_nopage=1&_query=id%3E9130517&group.is_deleted=false&'
        'group.type=department&person.official.is_dismissed=false&_sort=group.department.url',
        {'id', 'group.department.id', 'group.department.url', 'group.is_deleted', 'group.type',
        'person.login', 'person.official.is_dismissed'}
    ),
]

@pytest.mark.parametrize('url, fields_set', url_with_fields)
def test_get_staffapi_fields(url, fields_set):
    _, params = parse_url(url)
    assert get_staffapi_fields(params) == fields_set


yandex_user = IDMUser(_works_in_dep = lambda dep_url: dep_url == 'yandex')
ext_user = IDMUser(_works_in_dep = lambda dep_url: dep_url == 'ext')
outstaff_user = IDMUser(_works_in_dep = lambda dep_url: dep_url == 'outstaff')
robot_user = IDMUser(is_robot=True, _works_in_dep = lambda _: False,)

url1 = 'https://staff-api.yandex-team.ru/v3/persons?_fields=uid,cars,work_mode'
url2 = 'https://staff-api.yandex-team.ru/v3/persons?_fields=uid,work_mode&_sort=login'
url3 = 'https://staff-api.yandex-team.ru/v3/persons?_fields=uid,yandex.login&_sort=login'

request_combinations = [
    (yandex_user, url1, True),
    (yandex_user, url2, True),
    (yandex_user, url3, False),  # yandex.login
    (ext_user, url1, False),
    (ext_user, url2, False),
    (ext_user, url3, False),
    (outstaff_user, url1, False),
    (outstaff_user, url2, False),
    (outstaff_user, url3, False),
    (robot_user, url1, True),
    (robot_user, url2, False),  # login
    (robot_user, url3, False),  # yandex.login, login
]

@pytest.mark.parametrize('user, access_url, is_safe', request_combinations)
def test_safe_to_approve_personal_partial_access(user, access_url, is_safe):
    resource = PERSON
    assert is_safe_to_approve_partial_access(user, resource, access_url) is is_safe
