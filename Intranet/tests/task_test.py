import pytest
from datetime import date

from staff.lib.testing import (
    StaffFactory,
    OfficeFactory,
    RoomFactory,
    FloorFactory,
    TableFactory,
)
from staff.whistlah.tests.factories import StaffLastOfficeFactory

from staff.map.tasks import (
    update_staff_office_logs,
    update_rooms_usage,
)
from staff.map.models import StaffOfficeLog, RoomUsage, SourceTypes


@pytest.mark.django_db
def test_update_staff_office_logs():
    staff_office_logs = StaffOfficeLog.objects.all()
    assert len(staff_office_logs) == 0

    redrose = OfficeFactory(name='redrose', name_en='en_name')
    first = FloorFactory(name='First', office=redrose, intranet_status=1)

    room = RoomFactory(
        pk=420,
        floor=first,
        seats=2,
        room_type=0,
        geometry='0,0,3,0,3,3,0,3,0,0',
    )
    TableFactory(
        floor=first,
        intranet_status=1,
        coord_x=100,
        coord_y=100,
        room=room,
        num=1,
    )
    TableFactory(
        floor=first,
        intranet_status=1,
        coord_x=110,
        coord_y=110,
        room=room,
        num=2,
    )
    user = StaffFactory(login='user', table=None, room=room, office=redrose)

    StaffLastOfficeFactory(
        staff=user,
        office=redrose,
        office_name=redrose.name,
        office_name_en=redrose.name_en,
        is_vpn=False,
    )

    update_staff_office_logs(hours=1)

    staff_office_logs = StaffOfficeLog.objects.all()
    assert len(staff_office_logs) == 1
    assert staff_office_logs[0].staff == user
    assert staff_office_logs[0].office == redrose
    assert staff_office_logs[0].date == date.today()
    assert staff_office_logs[0].source == 'whistlah'

    update_staff_office_logs(hours=1)

    staff_office_logs = StaffOfficeLog.objects.all()
    assert len(staff_office_logs) == 1

    update_rooms_usage(source=SourceTypes.WHISTLAH.value, update_date=date.today())
    room_usage = RoomUsage.objects.all()
    assert len(room_usage) == 1

    assert room_usage[0].usage == 0.5
    assert room_usage[0].square_based_usage == 0.5
    assert room_usage[0].room == room
    assert room_usage[0].date == date.today()
    assert room_usage[0].source == 'whistlah'
