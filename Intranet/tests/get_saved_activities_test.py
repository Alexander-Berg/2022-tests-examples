import pytest

from staff.map.models import OfficePart
from staff.lib.testing import OfficeFactory

from staff.whistlah.utils import get_saved_activities
from staff.whistlah.tests.factories import StaffLastOfficeFactory


@pytest.mark.django_db()
def test_get_saved_activities():
    # given
    last_office = StaffLastOfficeFactory()

    # when
    result = get_saved_activities([last_office.staff.login])

    # then
    assert len(result) == 1
    assert result[last_office.staff.login]['office_id'] == last_office.office_id


@pytest.mark.django_db()
def test_get_saved_activities_for_complex_office():
    # given
    red_rose = OfficeFactory(code='kr')

    mamontov = OfficeFactory(code='rrm')
    OfficePart.objects.create(main_office=red_rose, part_of_main_office=mamontov)

    morozov = OfficeFactory(code='redrose')
    OfficePart.objects.create(main_office=red_rose, part_of_main_office=morozov)

    last_office = StaffLastOfficeFactory(office=red_rose)

    # when
    result = get_saved_activities([last_office.staff.login])

    # then
    assert len(result) == 1
    assert result[last_office.staff.login]['office_parts_codes'] == [mamontov.code, morozov.code]
