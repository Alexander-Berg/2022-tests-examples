import pytest

from django.conf import settings

from staff.lib.testing import (
    FloorFactory,
    OfficeFactory,
    RoomFactory,
)

from staff.map.utils import get_map_room_id_to_room_square


@pytest.mark.django_db
def test_get_room_squares_map():
    redrose = OfficeFactory(name='redrose', name_en='en_name')
    first = FloorFactory(name='First', office=redrose, intranet_status=1)
    rooms = [
        RoomFactory(
            floor=first,
            seats=2,
            room_type=0,
            geometry='0,0,3,0,3,3,0,3,0,0',
        ),
        RoomFactory(
            floor=first,
            seats=2,
            room_type=0,
            geometry='',
        ),
        RoomFactory(
            floor=first,
            seats=4,
            room_type=0,
            geometry='10,10,12,10,12,12,10,12,10,10'
        )
    ]

    squares_map = get_map_room_id_to_room_square()
    assert len(squares_map) == 2
    assert squares_map[rooms[0].id] == 9
    assert squares_map[rooms[2].id] == 4

    squares_map_by_ten = get_map_room_id_to_room_square(0.1)
    assert len(squares_map_by_ten) == 2
    assert squares_map_by_ten[rooms[0].id] == 0.9
    assert squares_map_by_ten[rooms[2].id] == 0.4


@pytest.mark.django_db
def test_get_room_squares_map_by_batches():
    room_count = 17
    batch_size = 3

    settings.ROOM_USAGE_BATCH_SIZE = batch_size

    for i in range(room_count):
        RoomFactory(geometry=f'0,0,0,{i},{i},{i},{i},0,0,0')

    squares_map = get_map_room_id_to_room_square()
    assert len(squares_map) == room_count
    assert set(squares_map.values()) == set(i ** 2 for i in range(room_count))
