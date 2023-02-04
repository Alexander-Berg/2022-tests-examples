from decimal import Decimal
from math import isclose

import pytest

from staff.lib.testing import RoomFactory, FloorFactory

from staff.workspace_management.usage_report import bu_usage_model, offices_area
from staff.workspace_management.tests.factories import (
    BusinessUnitFactory,
    OfficeAreaFactory,
    RoomSharePieFactory,
    ShareFactory,
)


@pytest.mark.django_db
def test_bu_usage_model_for_single_bc():
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

    working_area = room1_share_pie.room_area + room2_share_pie.room_area

    # when
    result = list(bu_usage_model(offices_area()))

    # then
    assert len(result) == 2

    first_row = result[0]
    assert first_row.office_name == office.name
    assert first_row.office_name_en == office.name_en
    assert first_row.bu_name == bu1.name
    assert first_row.bu_name_en == bu1.name_en
    expected_first_bu_working_area = (
        room1_share_pie.room_area * Decimal(0.666666) + room2_share_pie.room_area * Decimal(0.5)
    )
    assert isclose(first_row.bu_working_area, expected_first_bu_working_area)
    assert isclose(first_row.bu_working_area_share, (expected_first_bu_working_area / working_area) * 100)
    expected_first_bu_public_area = (
        (expected_first_bu_working_area / working_area) * (office_area.office_area - working_area)
    )
    assert isclose(first_row.bu_public_area, expected_first_bu_public_area)
    assert isclose(first_row.bu_total_area, expected_first_bu_working_area + expected_first_bu_public_area)

    second_row = result[1]
    assert second_row.office_name == office.name
    assert second_row.office_name_en == office.name_en
    assert second_row.bu_name == bu2.name
    assert second_row.bu_name_en == bu2.name_en
    expected_second_bu_working_area = (
        room1_share_pie.room_area * Decimal(0.333333) + room2_share_pie.room_area * Decimal(0.25)
    )
    assert isclose(second_row.bu_working_area, expected_second_bu_working_area)
    assert isclose(second_row.bu_working_area_share, (expected_second_bu_working_area / working_area) * 100)
    expected_second_bu_public_area = (
        (expected_second_bu_working_area / working_area) * (office_area.office_area - working_area)
    )
    assert isclose(second_row.bu_public_area, expected_second_bu_public_area)
    assert isclose(second_row.bu_total_area, expected_second_bu_working_area + expected_second_bu_public_area)


@pytest.mark.django_db
def test_bu_usage_model_do_not_fall_on_empty_shares():
    # given
    office_area = OfficeAreaFactory(office_area=Decimal(1000))
    office = office_area.office
    room1 = RoomFactory(floor=FloorFactory(office=office))
    RoomSharePieFactory(room=room1, room_area=Decimal(300))

    # when
    result = list(bu_usage_model(offices_area()))

    # then
    assert len(result) == 0
