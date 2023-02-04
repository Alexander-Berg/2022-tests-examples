import datetime
import pytest
from mock import Mock
from json import loads

from django.core.urlresolvers import resolve

from staff.lib.testing import StaffFactory
from staff.map.models import Room, Floor, Office, Table, TableBook
from staff.person.models import LANG


now = datetime.datetime.now()
JQUERY_CODE = 'jQuery1234567890123456789_1234567890123'

DUMP_OFFICES = [
    Office(
        from_staff_id=1,
        id=1,
        name='Москва, БЦ Морозов',
        name_en='Moscow, BC Morozov',
        native_lang='ru',
        created_at=now,
        modified_at=now,
    ),
]

DUMP_FLOORS = [
    Floor(
        from_staff_id=1,
        id=1,
        name='1 этаж',
        num=1,
        office=DUMP_OFFICES[0],
        created_at=now,
        modified_at=now,
    ),
]

DUMP_ROOMS = [
    Room(
        from_staff_id=1,
        id=10001,
        num=158,
        name='Коворкинг 1',
        name_alternative='room 1',
        floor=DUMP_FLOORS[0],
        room_type=11,
        created_at=now,
        modified_at=now,
    ),
    Room(
        from_staff_id=1,
        id=10002,
        num=158,
        name='Кофепоинт 1',
        name_alternative='room 2',
        floor=DUMP_FLOORS[0],
        room_type=10,
        created_at=now,
        modified_at=now,
    ),
]

DUMP_TABLES = [
    Table(
        from_staff_id=1,
        floor=DUMP_FLOORS[0],
        room=DUMP_ROOMS[0],
        intranet_status=1,
        num=1,
        created_at=now,
        modified_at=now,
    ),
    Table(
        from_staff_id=1,
        floor=DUMP_FLOORS[0],
        room=DUMP_ROOMS[0],
        intranet_status=1,
        num=2,
        created_at=now,
        modified_at=now,
    ),
    Table(
        from_staff_id=1,
        floor=DUMP_FLOORS[0],
        room=DUMP_ROOMS[0],
        intranet_status=1,
        num=3,
        created_at=now,
        modified_at=now,
    ),
    Table(
        from_staff_id=1,
        floor=DUMP_FLOORS[0],
        room=DUMP_ROOMS[0],
        intranet_status=0,
        num=4,
        created_at=now,
        modified_at=now,
    ),
    Table(
        from_staff_id=1,
        floor=DUMP_FLOORS[0],
        room=DUMP_ROOMS[1],
        intranet_status=1,
        num=5,
        created_at=now,
        modified_at=now,
    ),
    Table(
        from_staff_id=1,
        floor=DUMP_FLOORS[0],
        room=DUMP_ROOMS[1],
        intranet_status=1,
        num=6,
        created_at=now,
        modified_at=now,
    ),
    Table(
        from_staff_id=1,
        floor=DUMP_FLOORS[0],
        room=DUMP_ROOMS[0],
        intranet_status=1,
        num=7,
        created_at=now,
        modified_at=now,
    ),
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
def tables():
    for item in DUMP_TABLES:
        item.save()


@pytest.fixture
def rus_user():
    st = StaffFactory(login='ya_tester', lang_ui=LANG.RU, table=DUMP_TABLES[1])
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


@pytest.mark.django_db
def test_filter_room_by_office_id_and_empty_query(rf, offices, floors, rooms, tables, rus_user):
    TableBook(
        staff=rus_user.get_profile(),
        table=DUMP_TABLES[6],
        date_from=datetime.date(2021, 10, 12),
        date_to=datetime.date(2021, 10, 13),
        created_at=now,
        modified_at=now,
    ).save()
    TableBook(
        staff=rus_user.get_profile(),
        table=DUMP_TABLES[6],
        date_from=datetime.date(2021, 10, 22),
        date_to=datetime.date(2021, 10, 23),
        created_at=now,
        modified_at=now,
    ).save()
    url = '/center/api/autocomplete/multi/'

    # поищем по названию
    kwargs = {
        'callback': JQUERY_CODE,
        'types': 'table_can_be_booked',
        'q': '',
        'table_can_be_booked__room_id': DUMP_ROOMS[0].id,
        'table_can_be_booked__date_from': '2021-10-12',
        'table_can_be_booked__date_to': '2021-10-14',
    }

    request = rf.get(url, kwargs)
    request.user = rus_user
    request.LANGUAGE_CODE = 'ru'

    response = resolve(url).func(request)

    find_only = collect_result(response)

    # DUMP_TABLES[1] - не подходит тк занят постоянно
    # DUMP_TABLES[3] - не подходит тк удален
    # DUMP_TABLES[4-5] - не подходит тк не та комната
    # DUMP_TABLES[6] - не подходит тк существует бронь
    assert find_only('_id') == [
        DUMP_TABLES[0].id,
        DUMP_TABLES[2].id,
    ]
    assert find_only('_text') == [
        str(DUMP_TABLES[0].id),
        str(DUMP_TABLES[2].id),
    ]
    assert find_only('_type') == ['table_can_be_booked'] * 2
