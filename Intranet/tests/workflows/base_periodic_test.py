import pytest
from datetime import datetime

from staff.gap.workflows.base_workflow import BasePeriodicWorkflow
from staff.gap.workflows.office_work.workflow import OfficeWorkWorkflow


@pytest.mark.django_db
@pytest.mark.parametrize(
    'periodic_data, dates_from',
    [
        [
            {
                'periodic_date_to': datetime(2015, 1, 20, 0, 0),
                'period': 1,
                'periodic_map_weekdays': [3, 5],
            },
            [
                datetime(2015, 1, 1),
                datetime(2015, 1, 3),
                datetime(2015, 1, 8),
                datetime(2015, 1, 10),
                datetime(2015, 1, 15),
                datetime(2015, 1, 17),
            ],
        ],
        [
            {
                'periodic_date_to': datetime(2015, 1, 20, 0, 0),
                'period': 2,
                'periodic_map_weekdays': [3, 5],
            },
            [
                datetime(2015, 1, 1),
                datetime(2015, 1, 3),
                datetime(2015, 1, 15),
                datetime(2015, 1, 17),
            ],
        ],
        [
            {
                'periodic_date_to': datetime(2015, 1, 20, 0, 0),
                'period': 1,
                'periodic_map_weekdays': [5],
            },
            [
                datetime(2015, 1, 3),
                datetime(2015, 1, 10),
                datetime(2015, 1, 17),
            ],
        ],
        [
            {
                'periodic_date_to': datetime(2015, 1, 20, 0, 0),
                'period': 1,
                'periodic_map_weekdays': [1, 3, 5],
            },
            [
                datetime(2015, 1, 1),
                datetime(2015, 1, 3),
                datetime(2015, 1, 6),
                datetime(2015, 1, 8),
                datetime(2015, 1, 10),
                datetime(2015, 1, 13),
                datetime(2015, 1, 15),
                datetime(2015, 1, 17),
                datetime(2015, 1, 20),
            ],
        ],
        [
            {
                'periodic_date_to': datetime(2015, 1, 15, 0, 0),
                'period': 1,
                'periodic_map_weekdays': [3, 5],
            },
            [
                datetime(2015, 1, 1),
                datetime(2015, 1, 3),
                datetime(2015, 1, 8),
                datetime(2015, 1, 10),
                datetime(2015, 1, 15),
            ],
        ],
    ]
)
def test_generate_dates_from(gap_test, periodic_data, dates_from):
    base_gap = gap_test.get_base_periodic_gap(OfficeWorkWorkflow)

    base_periodic_workflow = BasePeriodicWorkflow()

    base_gap.update(periodic_data)

    generate_dates_from = base_periodic_workflow.generate_dates_from(
        base_gap['date_from'],
        base_gap,
    )

    assert list(generate_dates_from) == dates_from
