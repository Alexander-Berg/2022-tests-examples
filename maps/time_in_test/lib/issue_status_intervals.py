from itertools import zip_longest
from itertools import tee

from nile.api.v1 import (
    Record
)

'''
    Returns all status intervals for all issues including open-ended
    last interval. Example:

      issue  |    status    | begin_timestamp | end_timestamp
    ---------+--------------+-----------------+-----------------
     jfha423 |     open     |  1387035938582  | 1387036042746
     jfha423 | readyForTest |  1387036042746  | 9999999999999
     j2a4yu1 |     open     |  1387040000000  | 1387050000000
'''


def create_issue_status_intervals(issue_status_changes):

    def events_coupler(issue_actions):
        '''
            Reduce-operation.
            Actions should be sorted by time.
            For each issue it collects sequential
            changes of statuses and transforms them into intervals.
        '''

        future_timestamp_ms = 9999999999999  # 2286 year
        infinity_status_record = Record(timestamp=future_timestamp_ms)

        for key, records in issue_actions:
            first, second = tee(records)
            next(second, None)

            consecutive_statuses = zip_longest(
                first,
                second,
                fillvalue=infinity_status_record
            )

            for prev, cur in consecutive_statuses:
                yield Record(
                    issue=key.issue,
                    status=prev.new_status,
                    begin_timestamp=prev.timestamp,
                    end_timestamp=cur.timestamp
                )

    issue_status_intervals = issue_status_changes.groupby(
        'issue'
    ).sort(
        'timestamp'
    ).reduce(
        events_coupler
    )

    return issue_status_intervals
