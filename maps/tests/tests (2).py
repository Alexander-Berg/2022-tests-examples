from maps.wikimap.stat.tasks_payment.tasks_logging.tracker.cartographic_log.lib import cartographic_log

from maps.wikimap.stat.libs import nile_ut

from nile.api.v1 import Record
from yt.yson import to_yson_type

import datetime


def timestamp(date_iso_str):
    return datetime.datetime.fromisoformat(date_iso_str).timestamp() * 1000


def test_should_make_log():
    result = nile_ut.yt_run(
        cartographic_log._make_log,
        issues_with_puid=nile_ut.Table([
            Record(puid=1, component=b'component 1', ticket=b'queue1-1', date=b'2020-01-25', story_points=1.1),
            Record(puid=2, component=b'component 2', ticket=b'queue2-2', date=b'2020-01-25', story_points=2.2),
            Record(puid=1, component=b'component 3', ticket=b'queue3-3', date=b'2020-01-26', story_points=3.3),
            Record(puid=4, component=b'component 4', ticket=b'queue4-4', date=b'2020-01-26', story_points=None),
            Record(puid=5, component=b'component 5', ticket=b'queue5-5', date=b'2020-01-27'),
        ])
    )

    assert sorted([
        Record(iso_datetime=b'2020-01-25', puid=1, task_id='tracker/queue1/component 1',
               quantity=1.1, geom=None, lat_min=None, lon_min=None, lat_max=None, lon_max=None),
        Record(iso_datetime=b'2020-01-25', puid=2, task_id='tracker/queue2/component 2',
               quantity=2.2, geom=None, lat_min=None, lon_min=None, lat_max=None, lon_max=None),
        Record(iso_datetime=b'2020-01-26', puid=1, task_id='tracker/queue3/component 3',
               quantity=3.3, geom=None, lat_min=None, lon_min=None, lat_max=None, lon_max=None),
        Record(iso_datetime=b'2020-01-26', puid=4, task_id='tracker/queue4/component 4',
               quantity=0.0, geom=None, lat_min=None, lon_min=None, lat_max=None, lon_max=None),
        Record(iso_datetime=b'2020-01-27', puid=5, task_id='tracker/queue5/component 5',
               quantity=0.0, geom=None, lat_min=None, lon_min=None, lat_max=None, lon_max=None),
    ]) == sorted(result)


def test_should_convert_input_tables_to_result():
    'Checks that the whole job works as it is expected. (an integration test)'

    open_changes = to_yson_type([{b'field': b'status', b'newValue': {b'value': {b'id': b'status id open'}}}])
    closed_changes = to_yson_type([{b'field': b'status', b'newValue': {b'value': {b'id': b'status id closed'}}}])

    result = nile_ut.yt_run(
        cartographic_log.make_tracker_cartographic_log,
        dates=['2020-01-25', '2020-01-26'],
        linked_accounts=nile_ut.Table([
            Record(staff_uid=1, puid=11,  is_primary_link=True,  date='2020-01-25'),
            Record(staff_uid=2, puid=22,  is_primary_link=True,  date='2020-01-26'),
            Record(staff_uid=3, puid=33,  is_primary_link=True,  date='2020-01-25'),
            Record(staff_uid=4, puid=44,  is_primary_link=True,  date='2020-01-25'),  # Wrong date
            Record(staff_uid=5, puid=55,  is_primary_link=True,  date='2020-01-25'),
            Record(staff_uid=5, puid=555, is_primary_link=False, date='2020-01-25'),  # Should be ignored
            Record(staff_uid=6, puid=66,  is_primary_link=True,  date='2020-01-25'),
            Record(staff_uid=7, puid=77,  is_primary_link=True,  date='2020-01-25'),  # Unused entry
        ]),
        components_table=nile_ut.Table([
            Record(id='component id 1', name=b'component 1'),
            Record(id='component id 2', name=b'component 2'),
            Record(id='component id 3', name=b'component 3'),
            Record(id='component id 4', name=b'component 4'),
            Record(id='component id 5', name=b'component 5'),
            Record(id='component id 6', name=b'component 6'),
            Record(id='component id 7', name=b'component 7'),  # Unused entry
        ]),
        issue_events_table=nile_ut.Table([
            # First closed at another day
            Record(issue=b'issue id 1', date=timestamp('2020-01-23'), changes=closed_changes),
            Record(issue=b'issue id 1', date=timestamp('2020-01-24'), changes=open_changes),
            Record(issue=b'issue id 1', date=timestamp('2020-01-25'), changes=closed_changes),

            # Non-closed
            Record(issue=b'issue id 2', date=timestamp('2020-01-26'), changes=open_changes),

            # Okay, but reopened later
            Record(issue=b'issue id 3', date=timestamp('2020-01-25'), changes=closed_changes),
            Record(issue=b'issue id 3', date=timestamp('2020-01-27'), changes=open_changes),

            # Okay, but without linked account
            Record(issue=b'issue id 4', date=timestamp('2020-01-26'), changes=closed_changes),

            # Unpaid resolution
            Record(issue=b'issue id 5', date=timestamp('2020-01-25'), changes=closed_changes),

            # Unpaid queue
            Record(issue=b'issue id 6', date=timestamp('2020-01-25'), changes=closed_changes)
        ]),
        issues_table=nile_ut.Table([
            Record(id='issue id 1', resolution=b'resolution id fixed', assignee=1, components=to_yson_type([b'component id 1']), key=b'MAPSPW-1',     storyPoints=1),
            Record(id='issue id 2', resolution=b'resolution id fixed', assignee=2, components=to_yson_type([b'component id 2']), key=b'FEEDBACKPW-2', storyPoints=2),
            Record(id='issue id 3', resolution=b'resolution id fixed', assignee=3, components=to_yson_type([b'component id 3']), key=b'OUTKARTPW-3',  storyPoints=3),
            Record(id='issue id 4', resolution=b'resolution id fixed', assignee=4, components=to_yson_type([b'component id 4']), key=b'MAPSPW-4',     storyPoints=4),
            Record(id='issue id 5', resolution=b'resolution id other', assignee=5, components=to_yson_type([b'component id 5']), key=b'FEEDBACKPW-5', storyPoints=5),
            Record(id='issue id 6', resolution=b'resolution id fixed', assignee=5, components=to_yson_type([b'component id 6']), key=b'OTHER-6',      storyPoints=6),
            Record(id='issue id 7', resolution=b'resolution id fixed', assignee=7, components=to_yson_type([b'component id 7']), key=b'OUTKARTPW-7',  storyPoints=7),  # Unused entry
        ]),
        resolutions_table=nile_ut.Table([
            Record(id=b'resolution id fixed',      key=b'fixed'),
            Record(id=b'resolution id incomplete', key=b'incomplete'),
            Record(id=b'resolution id other',      key=b'other'),
            Record(id=b'resolution id unused',     key=b'unused'),  # Unused entry
        ]),
        statuses_table=nile_ut.Table([
            Record(id='status id closed', key=b'closed'),
            Record(id='status id open',   key=b'open'),
            Record(id='status id other',  key=b'other'),  # Unused entry
        ])
    )

    assert sorted([
        Record(iso_datetime='2020-01-25', puid=33, task_id='tracker/outkartpw/component 3', quantity=3.0,
               geom=None, lat_min=None, lon_min=None, lat_max=None, lon_max=None),
        Record(iso_datetime='2020-01-26', puid=4, task_id='tracker/mapspw/component 4', quantity=4.0,
               geom=None, lat_min=None, lon_min=None, lat_max=None, lon_max=None),
    ]) == sorted(result)
