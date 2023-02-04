from maps.automotive.qa.metrics.common.lib.datetime_interval import DatetimeInterval

from nile.api.v1 import (
    datetime as nd,
    Record
)

''' Straightforward example:

    Testing intervals - T1, T2. Windows - W1, W2:
    -----------------------------------
    (  T1  )     (    T2    )
              [  W1  ]    [  W2  ]
    ---------------------------------->
    1      2  3  4   5    6 7    8

    Hours in testing:
    W1: 2 - 1 + 5 - 4 = 2
    W2: 2 - 1 + 7 - 4 = 4


    Tables schemas:
        'issue_touches_window':
            'issue',
            'status',
            'fielddate',
            'window'

        'issue_testing_intervals':
            'issue',
            'status',
            'begin_timestamp',
            'end_timestamp'

        result table 'issue_testing_hours':
            'issue',
            'status'
            'fielddate',
            'window',
            'hours_in_testing'
'''


def create_issue_testing_hours(issue_touches_window, issue_testing_intervals):

    def reducer(groups):
        for key, records in groups:

            window_interval = DatetimeInterval.from_date_and_window(
                nd.Datetime.from_iso(key.fielddate),
                key.window
            )

            total_seconds = 0
            for record in records:
                record_interval = DatetimeInterval.from_timestamps_ms(
                    record.begin_timestamp,
                    record.end_timestamp
                )

                record_interval_cutted = DatetimeInterval(
                    min(record_interval.begin, window_interval.end),
                    min(record_interval.end, window_interval.end)
                )

                total_seconds += (
                    record_interval_cutted.end - record_interval_cutted.begin
                ).total_seconds()

            yield Record(
                issue=key.issue,
                status=key.status,
                fielddate=key.fielddate,
                window=key.window,
                hours_in_testing=total_seconds / 3600.0
            )

    issue_testing_hours = issue_touches_window.join(
        issue_testing_intervals,
        by=('issue', 'status')
    ).groupby(
        'issue',
        'status',
        'fielddate',
        'window'
    ).reduce(
        reducer
    )

    return issue_testing_hours
