from itertools import product

from maps.automotive.qa.metrics.common.lib.datetime_interval import (
    DatetimeInterval,
    datetime_intervals_overlap
)

from nile.api.v1 import Record


'''
    Creates table with information if any testing interval of given issue
    has common points with interval of (fielddate, window)

    'issue_testing_intervals' has following schema:
        'issue',
        'status'
        'begin_timestamp',
        'end_timestamp'

    result table schema:
        'issue',
        'status',
        'fielddate',
        'window'
'''


def create_issue_touches_window(issue_testing_intervals, dates, windows):

    def issue_intervals_reducer(issue_intervals):
        for key, rows in issue_intervals:
            for (row, date, window) in product(rows, dates, windows):
                window_interval = DatetimeInterval.from_date_and_window(
                    date,
                    window
                )

                row_interval = DatetimeInterval.from_timestamps_ms(
                    row.begin_timestamp,
                    row.end_timestamp
                )

                intervals_overlap = datetime_intervals_overlap(
                    window_interval,
                    row_interval
                )

                if intervals_overlap is not None:
                    yield Record(
                        issue=key.issue,
                        status=row.status,
                        fielddate=str(date.date()).encode(),
                        window=window
                    )

    issue_touches_window = issue_testing_intervals.groupby(
        'issue'
    ).reduce(
        issue_intervals_reducer
    ).unique(
        'issue',
        'status',
        'fielddate',
        'window'
    )

    return issue_touches_window
