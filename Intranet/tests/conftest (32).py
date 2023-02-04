from datetime import date
import pytest

from staff.lib.testing import (
    DepartmentFactory,
    FloorFactory,
    OfficeFactory,
    RoomFactory,
    StaffFactory,
    TableBookFactory,
    TableFactory,
    TableReserveFactory,
)

from staff.map.models import ROOM_TYPES


class MapEditTableTestData:
    redrose = None
    first = None
    table = None
    another_table = None
    department = None
    spock = None
    kirk = None
    mccoy = None
    table_data = None
    user = None


@pytest.fixture
def map_edit_table_test_data():
    result = MapEditTableTestData()
    result.redrose = OfficeFactory(name='Red Rose', city=None, intranet_status=1)
    result.first = FloorFactory(name='First', office=result.redrose,
                                intranet_status=1)

    result.table = TableFactory(coord_x=100, coord_y=100, floor=result.first,
                                num=1701)
    result.another_table = TableFactory(coord_x=70, coord_y=70,
                                        floor=result.first, num=42)

    result.department = DepartmentFactory(name='enterprise')
    result.spock = StaffFactory(login='spock', table=result.table,
                                department=result.department)

    result.kirk = StaffFactory(login='kirk', department=result.department,
                               table=None, office=result.redrose)

    result.mccoy = StaffFactory(login='mccoy', department=result.department,
                                table=None, office=result.redrose)

    TableReserveFactory(staff=result.kirk, department=result.department,
                        table=result.table)

    TableBookFactory(staff=result.mccoy, table=result.table,
                     date_from=date.today(), date_to=date.today(),
                     description='Doctor here')

    result.table_data = {
        'coord_x': 10,
        'coord_y': 10,
        'floor': result.first,
        'num': None
    }

    result.user = StaffFactory(login='mr_plant')

    return result


class MapTestEditRoomData:
    redrose = None
    first = None
    data = None
    user = None


@pytest.fixture
def map_edit_room_test_data():
    result = MapTestEditRoomData()

    result.redrose = OfficeFactory(name='Red Rose', city=None, intranet_status=1)
    result.other_office = OfficeFactory(name='Some Office', city=None, intranet_status=1)

    result.first = FloorFactory(name='First', office=result.redrose,
                                intranet_status=1)
    result.second = FloorFactory(name='Second', office=result.redrose,
                                 intranet_status=1)

    result.other_first = FloorFactory(name='Other First', office=result.other_office, intranet_status=1)

    result.table = TableFactory(coord_x=100, coord_y=100, floor=result.first, num=1701)

    result.data = {
        'coord_x': 10,
        'coord_y': 10,
        'name': 'unknown',
        'name_en': 'unknown_en',
        'additional': '',
        'floor': result.second,
        'room_type': ROOM_TYPES.ROOM_COFFEE,
        'num': 42
    }

    result.room = RoomFactory(coord_x=100, coord_y=100, name='room_name',
                              additional='description', floor=result.first,
                              room_type=ROOM_TYPES.ROOM_SMOKING, num=777)

    result.other_room = RoomFactory(
        coord_x=100,
        coord_y=100,
        name='First room in some other office',
        additional='description',
        floor=result.other_first,
        room_type=ROOM_TYPES.OFFICE,
        num=1,
    )

    result.rooms = [
        RoomFactory(
            coord_x=100,
            coord_y=100,
            name='First room',
            additional='description',
            floor=result.first,
            room_type=ROOM_TYPES.OFFICE,
            num=1,
        ),
        RoomFactory(
            coord_x=200,
            coord_y=200,
            name='Second room',
            additional='description',
            floor=result.first,
            room_type=ROOM_TYPES.OFFICE,
            num=2,
        ),
        RoomFactory(
            coord_x=300,
            coord_y=300,
            name='Third room',
            additional='description',
            floor=result.first,
            room_type=ROOM_TYPES.OFFICE,
            num=3,
        ),
    ]

    result.staff = {
        'spock': StaffFactory(
            id=100001,
            login='spock',
            table=result.table,
            room=result.rooms[0],
            office=result.redrose,
        ),
        'kirk': StaffFactory(
            id=100002,
            login='kirk',
            table=result.table,
            room=result.rooms[0],
            office=result.redrose,
        ),
        'mccoy': StaffFactory(
            id=100003,
            login='mccoy',
            table=None,
            room=result.rooms[1],
            office=result.redrose,
        ),
        'uhura': StaffFactory(
            id=100004,
            login='uhura',
            table=None,
            room=None,
            office=result.redrose,
        ),
    }

    result.user = StaffFactory(login='mr_plant')
    return result


class BookTableTestData:
    TABLES = None
    STAFF = None


@pytest.fixture
def book_table_test_data():
    data = BookTableTestData()
    data.TABLES = [TableFactory() for _ in range(2)]
    data.STAFF = [StaffFactory() for _ in range(3)]
    return data
