from nile.api.v1 import Record
from yt.yson import YsonEntity


'''
    Returns all status changes for all issues in the following form

      issue  |   timestamp   | old_status |   new_status
    ---------+---------------+------------+---------------
     jfha423 | 1387035938582 |    Null    |      open
     j2a4yu1 | 1387040000000 |    close   |      open
     jfha423 | 1387036042746 |    open    |  readyForTest
'''


def create_issue_status_changes(issue_events, statuses):

    def flatten_by_status_ids_change(records):
        '''
        Map-operation. Returns status changes of issue from history.
        Parse YSON changes of each record. Returns date, old and
        new id of status.
        Original value of status is designated as '#'
        which is YsonEntity in code.
        '''
        for record in records:
            for change in record.changes:
                if change[b'field'] != b'status':
                    continue

                old_value = change[b'oldValue'][b'value']
                if old_value is not None and old_value is not YsonEntity:
                    old_status_id = old_value[b'id']
                else:
                    old_status_id = None
                new_status_id = change[b'newValue'][b'value'][b'id']
                yield Record(
                    issue=record.issue,
                    date=record.date,
                    old_status_id=old_status_id,
                    new_status_id=new_status_id
                )

    issue_status_ids_changes = issue_events.map(flatten_by_status_ids_change)

    status_names_old = statuses.project(old_status_id='id', old_status='key')
    status_names_new = statuses.project(new_status_id='id', new_status='key')

    issue_status_changes = issue_status_ids_changes.join(
        status_names_old,
        by='old_status_id',
        type='left'
    ).join(
        status_names_new,
        by='new_status_id',
        type='left'
    ).project(
        'issue',
        'old_status',
        'new_status',
        timestamp='date',
    )

    return issue_status_changes
