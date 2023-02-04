from decimal import Decimal
from math import isclose

import pytest

from staff.lib.testing import RoomFactory, FloorFactory

from staff.workspace_management.usage_report import bc_usage_model, offices_area
from staff.workspace_management.tests.factories import (
    BusinessUnitFactory,
    OfficeAreaFactory,
    RoomSharePieFactory,
    ShareFactory,
)


@pytest.mark.django_db
def test_bc_usage_model_for_single_bc():
    # given
    office_area = OfficeAreaFactory(office_area=Decimal(1000))
    office = office_area.office

    bu1 = BusinessUnitFactory()
    bu2 = BusinessUnitFactory()

    room1 = RoomFactory(floor=FloorFactory(office=office))
    room1_share_pie = RoomSharePieFactory(room=room1, room_area=Decimal(300))
    ShareFactory(room_share_pie=room1_share_pie, business_unit=bu1, share_value=Decimal(66.6666))
    ShareFactory(room_share_pie=room1_share_pie, business_unit=bu2, share_value=Decimal(33.3333))

    room2 = RoomFactory(floor=FloorFactory(office=office))
    room2_share_pie = RoomSharePieFactory(room=room2, room_area=Decimal(400))
    ShareFactory(room_share_pie=room2_share_pie, business_unit=bu1, share_value=Decimal(50))
    ShareFactory(room_share_pie=room2_share_pie, business_unit=bu2, share_value=Decimal(25))

    # when
    result = list(bc_usage_model(offices_area()))

    # then
    assert len(result) == 1
    row = result[0]
    assert row.office_name == office.name
    assert row.office_name_en == office.name_en
    assert row.bc_rent_area == office_area.office_area
    assert row.work_area == room1_share_pie.room_area + room2_share_pie.room_area
    expected_public_area = office_area.office_area - (room1_share_pie.room_area + room2_share_pie.room_area)
    assert row.public_area == expected_public_area
    expected_bu_work_area = room1_share_pie.room_area + room2_share_pie.room_area * Decimal(0.75)
    assert isclose(row.bu_work_area, expected_bu_work_area, abs_tol=0.001)
    expected_free_area = office_area.office_area - expected_public_area - expected_bu_work_area
    assert isclose(row.free_area, expected_free_area, abs_tol=0.001)


@pytest.mark.django_db
def test_bc_usage_model_do_not_fall_on_empty_shares():
    # given
    office_area = OfficeAreaFactory(office_area=Decimal(1000))
    office = office_area.office
    room1 = RoomFactory(floor=FloorFactory(office=office))
    room1_share_pie = RoomSharePieFactory(room=room1, room_area=Decimal(300))

    # when
    result = list(bc_usage_model(offices_area()))

    # then
    assert len(result) == 1
    row = result[0]
    assert row.office_name == office.name
    assert row.office_name_en == office.name_en
    assert row.bc_rent_area == office_area.office_area
    assert row.work_area == room1_share_pie.room_area
    expected_public_area = office_area.office_area - room1_share_pie.room_area
    assert row.public_area == expected_public_area
    assert row.bu_work_area == 0
    assert row.free_area == room1_share_pie.room_area
