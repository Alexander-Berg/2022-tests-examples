from nile.api.v1 import (
    aggregators as na,
    filters as nf,
    extractors as ne,
    statface as ns,
    Record
)

from itertools import product

import maps.automotive.qa.metrics.common.lib.measures as common_measures
from maps.automotive.qa.metrics.common.lib import table_names, report_names
from maps.automotive.qa.metrics.common.lib.table_names import YCAR_QUEUES as QUEUES
from maps.automotive.qa.metrics.common.lib.issue_add_details import tickets_list_to_string
from maps.automotive.qa.metrics.common.lib.issue_priority import priorities_named

from . import local_measures
from .issue_status_changes import create_issue_status_changes
from .issue_status_intervals import create_issue_status_intervals
from .issue_touches_window import create_issue_touches_window
from .issue_testing_hours import create_issue_testing_hours
from .issues_add_qa import issues_add_qa


def append_issue_queue(issues):
    def issue_queue_mapper(rows):
        for row in rows:
            queue = str.split(str(row.issue_key, 'utf-8'), '-')[0]
            yield Record(
                row,
                queue=queue.encode()
            )

    return issues.map(issue_queue_mapper)


'''
    Result example:
      issue  |   fielddate  | window | hours_in_testing | priority | queue | type | qa_engineer
    ---------+--------------+--------+------------------+----------+-------+------+------------
     jfha423 |   2019-07-01 |    7   |       34.7       |  normal  |  YAP  | bug  |  dalapenko
     ur45rip |   2019-08-01 |    1   |       10         |  normal  |  YAP  | task |  dalapenko
'''


def join_issue_testing_hours_with_additional_info(
        issue_testing_hours,
        issues):

    issues_with_info = issue_testing_hours.join(
        issues,
        by='issue'
    )
    return append_issue_queue(issues_with_info)


'''
    Creates denormalized issue table with values of type and priorities.

    Result example:
      issue  | issue_key | type | priority | qa_engineer
    ---------+-----------+------+----------+------------
     jfha423 | NMAPS-133 | bug  | blocker  |  dalapenko
     ur45rip | NMAPS-456 | task | normal   |  dalapenko
'''


def denormalize_issues(issues, types, priorities, testers_logins):
    issues = issues_add_qa(issues, testers_logins)

    issues_renamed = issues.project(
        'qa_engineer',
        issue='id',
        issue_key='key',
        type_id='type',
        priority_id='priority',
    )

    types_renamed = types.project(
        type_id='id',
        type='key'
    )

    return issues_renamed.join(
        types_renamed,
        by='type_id'
    ).join(
        priorities,
        by='priority_id'
    ).project(
        'issue',
        'issue_key',
        'type',
        'priority',
        'qa_engineer'
    )


'''
    In case if for some combination of (fielddate, window, priority, product, status)
    there was no tickets in testing we should manually add zeros for all
    aggregated values for this combination.
    Otherwise we'll see weird gaps in StatFace report
'''


def create_aggregated_values(job, dates, testers_logins):
    statuses = job.table(table_names.STATUSES).label('statuses')
    types = job.table(table_names.TYPES).label('types')
    priorities_table = job.table(table_names.PRIORITIES).label('priorities')
    priorities = priorities_named(priorities_table)

    issues = job.table(table_names.QUEUE_PATHS[0] + '/issues').label('issues')
    issue_events = job.table(table_names.QUEUE_PATHS[0] + '/issue_events').label('issue_events')

    if len(table_names.QUEUE_PATHS) > 1:
        for i in range(1, len(table_names.QUEUE_PATHS)):
            next_issues = job.table(table_names.QUEUE_PATHS[i] + '/issues')
            next_issues_events = job.table(table_names.QUEUE_PATHS[i] + '/issue_events')
            issues = job.concat(issues, next_issues).label('issues')
            issue_events = job.concat(issue_events, next_issues_events).label('issue_events')

    issue_status_changes = create_issue_status_changes(issue_events, statuses)

    issue_status_intervals = create_issue_status_intervals(
        issue_status_changes
    )

    issue_testing_intervals = issue_status_intervals.filter(
        nf.custom(lambda x: x in local_measures.STATUSES, 'status')
    )

    issue_touches_window = create_issue_touches_window(
        issue_testing_intervals,
        dates,
        common_measures.WINDOWS
    )

    issue_testing_hours = create_issue_testing_hours(
        issue_touches_window,
        issue_testing_intervals
    ).label('issue_testing_hours')

    issue_denormalized = denormalize_issues(issues, types, priorities, testers_logins)
    issue_denormalized_filtered = issue_denormalized.filter(
        nf.custom(lambda x: x in local_measures.TYPES, 'type')
    )

    issue_testing_hours_joined = join_issue_testing_hours_with_additional_info(
        issue_testing_hours,
        issue_denormalized_filtered
    ).label('issue_testing_hours_joined')

    aggregated_values = issue_testing_hours_joined.groupby(
        'fielddate',
        'window',
        'priority',
        'queue',
        'type',
        'status',
        'qa_engineer'
    ).aggregate(
        tickets_in_testing=na.count(),
        hours_in_testing=na.sum('hours_in_testing'),
        tickets=na.distinct('issue_key', sorted=True)
    )

    aggregated_values_with_detail_string = tickets_list_to_string(aggregated_values)
    return aggregated_values_with_detail_string


def complement_aggregated_values_with_zeros(job, dates, aggregated_values, testers_logins):
    '''
        Creating temp table on cluster with zero-valued aggregate values
        for all combinations of measures.

        We create it in a hacky way inside job - taking single row of arbitrary
        small table (we chose priorities), and then applying
        our custom map operation to it.

        There is more straightforward way of creating temporary table -
        using cluster. But the problem is that we don't have access to
        cluster instance inside Nirvana Nile Cube.
    '''
    def zero_combinations_mapper(records):
        for record in records:
            pass

        testers_logins.append(b'not_qa_engineer')

        measures_set = product(
            dates,
            common_measures.WINDOWS,
            common_measures.PRIORITIES,
            QUEUES,
            local_measures.TYPES,
            local_measures.STATUSES,
            testers_logins
        )

        for measures in measures_set:
            yield Record(
                fielddate=str(measures[0].date()),
                window=measures[1],
                priority=measures[2],
                queue=measures[3],
                type=measures[4],
                status=measures[5],
                qa_engineer=measures[6],
                hours_in_testing=0,
                tickets_in_testing=0,
                tickets_details=''
            )

    zero_aggregates = job\
        .table(table_names.PRIORITIES)\
        .top(1, by='order', mode='max')\
        .map(zero_combinations_mapper)

    aggregated_values_full = zero_aggregates.join(
        aggregated_values,
        by=['fielddate', 'window', 'priority', 'queue', 'type', 'status', 'qa_engineer'],
        type='left_only'
    ).concat(
        aggregated_values
    )

    return aggregated_values_full


def make_testable_job(job, dates, testers_logins):

    for i, login in enumerate(testers_logins):
        testers_logins[i] = login.encode()

    aggregated_values = create_aggregated_values(job, dates, testers_logins)

    return complement_aggregated_values_with_zeros(
        job, dates, aggregated_values, testers_logins
    )


def make_job(job, dates, statface_client, testers_logins):

    aggregated_values_full = make_testable_job(job, dates, testers_logins)

    '''
        The reason why we rename 'type' to 'type_kind' is that
        StatFace doesn't allows to name measures as 'type'.
        It is reserved keyword
    '''
    aggregated_values_full_renamed = aggregated_values_full.project(
        ne.all(exclude=['type']),
        type_kind='type'
    )

    report = ns.StatfaceReport()\
        .path(report_names.TIME_IN_TEST)\
        .scale('daily')\
        .client(statface_client)

    aggregated_values_full_renamed.publish(report, allow_change_job=True)

    return job
