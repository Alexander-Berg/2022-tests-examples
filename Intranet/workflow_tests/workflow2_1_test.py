import pytest

import uuid
from datetime import date

from staff.budget_position.workflow_service import entities
from staff.budget_position.workflow_service.entities.workflows.date_provider import DateProvider


DISMISSAL_DATES = [
    # last day of month in future -> should be aligned
    (date(2000, 1, 1), date(2001, 10, 31), date(2001, 11, 29)),
    # not last day of month in future -> keep as is
    (date(2000, 1, 1), date(2001, 10, 15), date(2001, 10, 15)),
    # the day before the last day of the month in future -> keep as is
    (date(2000, 1, 1), date(2001, 10, 29), date(2001, 10, 29)),
    # dismissal in the past, today is the last day -> should be aligned
    (date(2010, 1, 31), date(2001, 10, 15), date(2010, 2, 27)),
    # dismissal in the past, today is the last day -> should be aligned (leap year case)
    (date(2024, 1, 31), date(2001, 10, 15), date(2024, 2, 28)),
    # dismissal in the past, today is NOT the last day -> should be aligned
    (date(2010, 1, 25), date(2001, 10, 15), date(2010, 1, 30)),
    # no dismissal date, today is NOT the last day -> should be aligned
    (date(2000, 1, 1), None, date(2000, 1, 30)),
    # no dismissal date, today is the day before the last day -> should be aligned (leap year)
    (date(2000, 1, 30), None, date(2000, 2, 28)),
]


@pytest.mark.parametrize('today, dismissal_date, aligned_dismissal_date', DISMISSAL_DATES)
def test_workflow_2_1_creation_dismissal_date_cases(today, dismissal_date, aligned_dismissal_date):
    # given
    workflow_id = uuid.uuid1()
    change = entities.Change()
    change.dismissal_date = dismissal_date
    date_provider = DateProvider()
    date_provider.today = lambda: today

    # when
    result = entities.workflows.Workflow2_1(workflow_id, [change], date_provider)

    # then
    assert len(result._changes) == 1
    assert result._changes[0].dismissal_date == aligned_dismissal_date


def test_schemes_will_be_erased_by_workflow():
    # given
    workflow_id = uuid.uuid1()
    change = entities.Change(review_scheme_id=1, reward_scheme_id=2, bonus_scheme_id=3)

    # when
    result = entities.workflows.Workflow2_1(workflow_id, [change])

    # then
    assert result.changes[0].review_scheme_id is None
    assert result.changes[0].reward_scheme_id is None
    assert result.changes[0].bonus_scheme_id is None
