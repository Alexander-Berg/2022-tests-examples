import pytest

from staff.lib.testing import StaffFactory
from staff.map.models import Room, Floor, Office
import datetime
from django.core.urlresolvers import resolve
from mock import Mock
from json import loads

from staff.person.models import LANG

now = datetime.datetime.now()
JQUERY_CODE = 'jQuery1234567890123456789_1234567890123'

DUMP_OFFICES = [
    Office(**{
        'from_staff_id': 1,
        'id': 1,
        'name': 'Москва, БЦ Морозов',
        'name_en': 'Moscow, BC Morozov',
        'native_lang': 'ru',
        'created_at': now,
        'modified_at': now,
    }),
    Office(**{
        'from_staff_id': 1,
        'id': 2,
        'name': 'Москва, БЦ Мамонтов',
        'name_en': 'Moscow, BC Mamontov',
        'native_lang': 'ru',
        'created_at': now,
        'modified_at': now,
    }),
    Office(**{
        'from_staff_id': 1,
        'id': 3,
        'name': 'Москва, БЦ Строганов',
        'name_en': 'Moscow, BC Stroganov',
        'native_lang': 'ru',
        'created_at': now,
        'modified_at': now,
    }),
]

DUMP_FLOORS = [
    Floor(**{
        'from_staff_id': 1,
        'id': 1,
        'name': '1 этаж',
        'num': 1,
        'office': DUMP_OFFICES[0],
        'created_at': now,
        'modified_at': now,
    }),
    Floor(**{
        'from_staff_id': 1,
        'id': 2,
        'name': '5 этаж',
        'num': 5,
        'office': DUMP_OFFICES[1],
        'created_at': now,
        'modified_at': now,
    }),
    Floor(**{
        'from_staff_id': 1,
        'id': 3,
        'name': '2 этаж',
        'num': 2,
        'office': DUMP_OFFICES[2],
        'created_at': now,
        'modified_at': now,
    }),
    Floor(**{
        'from_staff_id': 1,
        'id': 4,
        'name': '1 этаж',
        'num': 1,
        'office': DUMP_OFFICES[0],
        'created_at': now,
        'modified_at': now,
    }),
]

DUMP_ROOMS = [
    Room(**{
        'from_staff_id': 1,
        'id': 10001,
        'num': 158,
        'name': 'Коворкинг 1',
        'name_alternative': 'room 1',
        'floor': DUMP_FLOORS[0],
        'room_type': 11,
        'created_at': now,
        'modified_at': now,
    }),
    Room(**{
        'from_staff_id': 1,
        'id': 10002,
        'num': 158,
        'name': 'Кофепоинт 1',
        'name_alternative': 'room 2',
        'floor': DUMP_FLOORS[1],
        'room_type': 10,
        'created_at': now,
        'modified_at': now,
    }),
    Room(**{
        'from_staff_id': 1,
        'id': 10003,
        'num': 158,
        'name': 'Коворкинг 2',
        'name_alternative': 'room 3',
        'floor': DUMP_FLOORS[2],
        'room_type': 11,
        'created_at': now,
        'modified_at': now,
    }),
    Room(**{
        'from_staff_id': 1,
        'id': 10004,
        'num': 158,
        'name': 'Комната 1',
        'name_alternative': 'room 4',
        'floor': DUMP_FLOORS[3],
        'room_type': 9,
        'created_at': now,
        'modified_at': now,
    }),
    Room(**{
        'from_staff_id': 1,
        'id': 10005,
        'num': 158,
        'name': 'Кофепоинт 2',
        'name_alternative': 'room 5',
        'floor': DUMP_FLOORS[0],
        'room_type': 10,
        'created_at': now,
        'modified_at': now,
    }),
    Room(**{
        'from_staff_id': 1,
        'id': 10006,
        'num': 158,
        'name': 'Коворкинг 3',
        'name_alternative': 'room 6',
        'floor': DUMP_FLOORS[0],
        'room_type': 11,
        'created_at': now,
        'modified_at': now,
    }),
    Room(**{
        'from_staff_id': 7,
        'id': 10007,
        'num': 158,
        'name': 'к о в о р к и н г',
        'name_alternative': 'room 6',
        'floor': DUMP_FLOORS[0],
        'room_type': 11,
        'created_at': now,
        'modified_at': now,
    }),
]


@pytest.fixture
def rooms():
    for item in DUMP_ROOMS:
        item.save()


@pytest.fixture
def floors():
    for item in DUMP_FLOORS:
        item.save()


@pytest.fixture
def offices():
    for item in DUMP_OFFICES:
        item.save()


@pytest.fixture
def rus_user():
    st = StaffFactory(login='ya_tester', lang_ui=LANG.RU)
    user = Mock()
    user.get_profile = lambda: st
    user.is_authenticated = lambda: True
    return user


def collect_result(response, status=200):
    assert response.status_code == status
    result = response.content.decode('utf-8')
    assert result.startswith(JQUERY_CODE)
    result = loads(result.replace(JQUERY_CODE, '')[1:-1])
    return lambda x: [item[x] for item in result]


@pytest.mark.parametrize('room_type_query', [11, 'COWORKING'])
@pytest.mark.django_db
def test_filter_room_by_office_id(rf, offices, floors, rooms, rus_user, room_type_query):
    url = '/center/api/autocomplete/multi/'

    # поищем по названию
    kwargs = {
        'callback': JQUERY_CODE,
        'types': 'new_room',
        'q': 'ко',
        'new_room__floor__office__id': 1,
        'new_room__room_type': room_type_query,
    }

    request = rf.get(url, kwargs)
    request.user = rus_user
    request.LANGUAGE_CODE = 'ru'

    response = resolve(url).func(request)

    find_only = collect_result(response)
    assert find_only('_id') == [10001, 10006]
    assert find_only('_text') == [
        # важен порядок :по длине строки:
        'Коворкинг 1',
        'Коворкинг 3',
    ]
    assert find_only('_type') == ['new_room'] * 2


@pytest.mark.parametrize('room_type_query', [11, 'COWORKING'])
@pytest.mark.django_db
def test_filter_room_by_office_name(rf, offices, floors, rooms, rus_user, room_type_query):
    url = '/center/api/autocomplete/multi/'

    # поищем по названию
    kwargs = {
        'callback': JQUERY_CODE,
        'types': 'new_room',
        'q': 'ко',
        'new_room__floor__office__name': 'Москва, БЦ Строганов',
        'new_room__room_type': room_type_query,
    }

    request = rf.get(url, kwargs)
    request.user = rus_user
    request.LANGUAGE_CODE = 'ru'

    response = resolve(url).func(request)

    find_only = collect_result(response)
    assert find_only('_id') == [10003]
    assert find_only('_text') == [
        # важен порядок :по длине строки:
        'Коворкинг 2',
    ]
    assert find_only('_type') == ['new_room']


@pytest.mark.parametrize('room_type_query', [11, 'COWORKING'])
@pytest.mark.django_db
def test_filter_room_by_office_id_and_empty_query(rf, offices, floors, rooms, rus_user, room_type_query):
    url = '/center/api/autocomplete/multi/'

    # поищем по названию
    kwargs = {
        'callback': JQUERY_CODE,
        'types': 'new_room',
        'q': '',
        'new_room__floor__office__id': 1,
        'new_room__room_type': room_type_query,
    }

    request = rf.get(url, kwargs)
    request.user = rus_user
    request.LANGUAGE_CODE = 'ru'

    response = resolve(url).func(request)

    find_only = collect_result(response)

    # третий (id=10007) коворкинг не находился в первых тестах, потому что
    # его название не содержит "ко", но найдется сейчас, потому что по
    # пустому запросу возвращаются все коворкинги
    assert find_only('_id') == [10001, 10006, 10007]
    assert find_only('_text') == [
        'Коворкинг 1',
        'Коворкинг 3',
        'к о в о р к и н г',
    ]
    assert find_only('_type') == ['new_room'] * 3
