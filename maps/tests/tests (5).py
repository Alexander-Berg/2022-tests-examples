from maps.wikimap.stat.tasks_payment.tasks_logging.feedback.lib import feedback_log
from maps.wikimap.stat.libs import nile_ut

from nile.api.v1 import Record
from yt.yson import to_yson_type

import pytest
from unittest.mock import patch


def point_to_ewkb_mock(lon, lat):
    return 'EWKB: lon={}, lat={}'.format(lon, lat).encode()


def test_should_get_paid_operations_at_date():
    result = nile_ut.yt_run(
        feedback_log._get_paid_operations_at_date,
        '2020-01-25',
        feedback=nile_ut.Table([
            Record(id=1, position=[1.1, 1.1], source=b'source 1', type=b'type 1', test_column=1, commit_ids=None, history=to_yson_type(
                [
                    {b'operation': b'accept', b'modifiedBy': 11, b'modifiedAt': b'2020-01-25 01:01:01+01:01'},
                    {b'operation': b'other',  b'modifiedBy': 12, b'modifiedAt': b'2020-01-25 01:01:01+01:01'},
                ]
            )),
        ]),
        social_commit_event=nile_ut.Table([])
    )

    assert sorted([
        Record(iso_datetime=b'2020-01-25T01:01:01+01:01', position=[1.1, 1.1], source=b'source 1', type=b'type 1', operation=b'accept', puid=11),
    ]) == sorted(result)


@patch(
    'maps.wikimap.stat.tasks_payment.tasks_logging.feedback.lib.feedback_log._point_to_ewkb',
    new=point_to_ewkb_mock
)
def test_should_calculate_geometry():
    result = nile_ut.yt_run(
        feedback_log._position_to_geom_and_bbox,
        operations_at_date=nile_ut.Table([
            Record(test_column=1, position=[1.1, 1.2])
        ])
    )

    assert result == [
        Record(test_column=1, geom=b'EWKB: lon=1.1, lat=1.2', lat_min=1.2, lon_min=1.1, lat_max=1.2, lon_max=1.1)
    ]


def test_should_fail_on_wrong_geometry():
    from nile.drivers.common.progress import CommandFailedError

    # Empty geometry
    with pytest.raises(CommandFailedError):
        nile_ut.yt_run(
            feedback_log._position_to_geom_and_bbox,
            operations_at_date=nile_ut.Table([Record(position=None)])
        )

    # Not a point
    with pytest.raises(CommandFailedError):
        nile_ut.yt_run(
            feedback_log._position_to_geom_and_bbox,
            operations_at_date=nile_ut.Table([Record(position=[1.1, 1.2, 1.3])])
        )


def test_should_calculate_task_id():
    result = nile_ut.yt_run(
        feedback_log._get_task_id,
        operations_with_geom_and_bbox=nile_ut.Table([
            Record(source=b'source', type=b'type', operation=b'accept', test_column=1),
        ])
    )

    assert sorted([
        Record(task_id=b'feedback/accept/source/type', test_column=1),
    ]) == sorted(result)


def test_should_format_table_as_task_log():
    result = nile_ut.yt_run(
        feedback_log._make_tasks_log,
        operations_with_task_id=nile_ut.Table([
            Record(task_id=b'task id', iso_datetime=b'2020-01-25T01:01:01+01:01', puid=1, geom=b'geom', lat_min=1.1, lon_min=1.2, lat_max=1.1, lon_max=1.2)
        ])
    )

    assert result == [
        Record(iso_datetime=b'2020-01-25T01:01:01+01:01', puid=1, task_id=b'task id', quantity=1.0, geom=b'geom', lat_min=1.1, lon_min=1.2, lat_max=1.1, lon_max=1.2)
    ]


@patch(
    'maps.wikimap.stat.tasks_payment.tasks_logging.feedback.lib.feedback_log._point_to_ewkb',
    new=point_to_ewkb_mock
)
def test_should_convert_input_tables_to_result():
    result = nile_ut.yt_run(
        feedback_log.make_feedback_log,
        '2020-01-25',
        feedback=nile_ut.Table([
            Record(id=1, position=[1.1, 1.2], source=b'source 1', type=b'type 1', commit_ids=to_yson_type([]), history=to_yson_type(
                [
                    # First accept has been already paid, so the second one should not be.
                    {b'operation': b'accept', b'modifiedBy': 11, b'modifiedAt': b'2020-01-24 11:11:11+00:00'},
                    {b'operation': b'reject', b'modifiedBy': 12, b'modifiedAt': b'2020-01-25 12:12:12+00:00'},
                ]
            )),
            Record(id=2, position=[2.1, 2.2], source=b'source 2', type=b'type 2', commit_ids=to_yson_type([]), history=to_yson_type(
                [
                    # It is okay to pay just after need info.
                    {b'operation': b'need-info', b'modifiedBy': 21, b'modifiedAt': b'2020-01-25 21:21:21+00:00'},
                    {b'operation': b'accept',    b'modifiedBy': 22, b'modifiedAt': b'2020-01-25 22:22:22+00:00'},
                ]
            ))
        ]),
        social_commit_event=nile_ut.Table([])
    )

    assert sorted([
        Record(
            iso_datetime=b'2020-01-25T21:21:21+00:00', puid=21, task_id=b'feedback/need-info/source 2/type 2',
            quantity=1.0, geom=b'EWKB: lon=2.1, lat=2.2', lat_min=2.2, lon_min=2.1, lat_max=2.2, lon_max=2.1
        ),
        Record(
            iso_datetime=b'2020-01-25T22:22:22+00:00', puid=22, task_id=b'feedback/accept/source 2/type 2',
            quantity=1.0, geom=b'EWKB: lon=2.1, lat=2.2', lat_min=2.2, lon_min=2.1, lat_max=2.2, lon_max=2.1
        ),
    ]) == sorted(result)
