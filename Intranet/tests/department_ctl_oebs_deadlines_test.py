import pytest
import datetime

from staff.proposal.hr_deadlines import NearestDeadline

from staff.departments.models import HrDeadline, DEADLINE_TYPE


dec = datetime.date(year=2019, month=12, day=1)
jan = datetime.date(year=2020, month=1, day=1)
feb = datetime.date(year=2020, month=2, day=1)
mar = datetime.date(year=2020, month=3, day=1)


@pytest.fixture
def hr_structure_deadlines(db):
    """Structure change deadlines on 18-th of december, january, february"""
    #     ------x------1---------------x-------1--------------x-->
    #                 Jan                     Feb

    deadlines_dates = {
        month: month.replace(day=18)
        for month in (dec, jan, feb, mar)
    }
    return [
        HrDeadline.objects.create(month=month, type=DEADLINE_TYPE.STRUCTURE_CHANGE, date=ddl_date)
        for month, ddl_date in deadlines_dates.items()
    ]


oebs_creation_dates = [
    (datetime.date(year=2019, month=12, day=5),  dec),
    (datetime.date(year=2019, month=12, day=19),  jan),
    (datetime.date(year=2020, month=1, day=2),  jan),
    (datetime.date(year=2020, month=1, day=17),  jan),
    (datetime.date(year=2020, month=1, day=18),  jan),
    (datetime.date(year=2020, month=1, day=19),  feb),
    (datetime.date(year=2020, month=2, day=12),  feb),
    (datetime.date(year=2020, month=2, day=25),  mar),
]


@pytest.mark.parametrize('date_creation, oebs_creation', oebs_creation_dates)
def test_get_oebs_creation_date(hr_structure_deadlines, date_creation, oebs_creation):
    deadline = NearestDeadline().find_for_deadline_type(date_creation, DEADLINE_TYPE.STRUCTURE_CHANGE)
    assert deadline.month == oebs_creation, date_creation
