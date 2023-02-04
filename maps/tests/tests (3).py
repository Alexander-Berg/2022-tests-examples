import pytest

from maps.wikimap.stat.tasks_payment.tasks_logging.mrc_pedestrian.lib import mrc_pedestrian_log
from maps.wikimap.stat.libs import nile_ut
from nile.api.v1 import Record


def test_should_convert_microseconds_to_isodate():
    result = nile_ut.yt_run(
        mrc_pedestrian_log._convert_microseconds_to_isodate,
        mrc_pedestrian_region_log=nile_ut.Table([
            Record(timestamp=1630443599000000, test_column=b'1'),
        ])
    )

    assert sorted([
        Record(isodate=b'2021-08-31T23:59:59+03:00', test_column=b'1'),
    ]) == sorted(result)


def test_should_get_mrc_regions_to_bill():
    result = nile_ut.yt_run(
        mrc_pedestrian_log._mrc_regions_to_bill,
        date='2021-09-01',
        mrc_pedestrian_region_log=nile_ut.Table([
            Record(object_id=1, name=b'region 1', status=b'awaiting_check', isodate=b'2021-08-31', test_column=42),

            Record(object_id=2, name=b'region 2', status=b'can_start', isodate=b'2021-08-25', test_column=42),
            Record(object_id=2, name=b'region 2', status=b'awaiting_check', isodate=b'2021-09-01', test_column=42),

            Record(object_id=3, name=b'region 3', status=b'can_start', isodate=b'2021-08-25', test_column=42),
            Record(object_id=3, name=b'region 3', status=b'awaiting_check', isodate=b'2021-09-01T02:00:00', test_column=42),
            Record(object_id=3, name=b'region 3', status=b'can_start', isodate=b'2021-09-01T03:00:00', test_column=42),
            Record(object_id=3, name=b'region 3', status=b'other_status', isodate=b'2021-09-01T03:30:00', test_column=42),
            Record(object_id=3, name=b'region 3', status=b'can_start', isodate=b'2021-09-01T04:00:00', test_column=42),
            Record(object_id=3, name=b'region 3', status=b'awaiting_check', isodate=b'2021-09-01T22:00:00', test_column=42),

            Record(object_id=4, name=b'region 4', status=b'can_start', isodate=b'2021-08-25', test_column=42),
            Record(object_id=4, name=b'region 4', status=b'other_status', isodate=b'2021-09-01', test_column=42),

            Record(object_id=5, name=b'region 5', status=b'can_start', isodate=b'2021-08-25', test_column=42),
            Record(object_id=5, name=b'region 5', status=b'awaiting_check', isodate=b'2021-09-02', test_column=42),
        ])
    )

    assert sorted([
        Record(mrc_region_id=2, mrc_region_name=b'region 2 (2)', started_at=b'2021-08-25', finished_at=b'2021-09-01'),
        Record(mrc_region_id=3, mrc_region_name=b'region 3 (3)', started_at=b'2021-08-25', finished_at=b'2021-09-01T02:00:00'),
        Record(mrc_region_id=3, mrc_region_name=b'region 3 (3)', started_at=b'2021-09-01T03:00:00', finished_at=b'2021-09-01T22:00:00'),
    ]) == sorted(result)


def test_should_get_walk_objects():
    result = nile_ut.yt_run(
        mrc_pedestrian_log._get_walk_objects,
        mrc_regions_to_bill=nile_ut.Table([
            Record(mrc_region_id=2, mrc_region_name=b'region 2 (2)', started_at=b'2021-08-25', finished_at=b'2021-09-01'),
            Record(mrc_region_id=3, mrc_region_name=b'region 3 (3)', started_at=b'2021-08-25', finished_at=b'2021-09-01T02:00:00'),
            Record(mrc_region_id=3, mrc_region_name=b'region 3 (3)', started_at=b'2021-09-01T04:00:00', finished_at=b'2021-09-01T22:00:00'),
            Record(mrc_region_id=4, mrc_region_name=b'region 4 (4)', started_at=b'2021-08-25', finished_at=b'2021-09-01T04:00:00'),
        ]),
        signals_walk_object_replica=nile_ut.Table([
            # signal not in zone to bill
            Record(
                id=11,
                feedback_task_id=11,
                user_id=b'1',
                task_id=b'mrc_pedestrian:1',
                created_at=b'2021-08-31',
                geometry=b'geom'
            ),
            # signal in zone to bill created after zone is finished
            Record(
                id=21,
                feedback_task_id=21,
                user_id=b'2',
                task_id=b'mrc_pedestrian:2',
                created_at=b'2021-09-01T03:00:00',
                geometry=b'geom'
            ),
            # signal in zone to bill created before zone is started
            Record(
                id=22,
                feedback_task_id=22,
                user_id=b'2',
                task_id=b'mrc_pedestrian:2',
                created_at=b'2021-08-20',
                geometry=b'geom'
            ),
            # signal in zone to bill created when zone is finished and not started yet
            Record(
                id=31,
                feedback_task_id=31,
                user_id=b'3',
                task_id=b'mrc_pedestrian:3',
                created_at=b'2021-09-01T03:00:00',
                geometry=b'geom'
            ),
            # signal in zone to bill, created when zone is started and not finished yet
            Record(
                id=32,
                feedback_task_id=32,
                user_id=b'3',
                task_id=b'mrc_pedestrian:3',
                created_at=b'2021-08-30T03:00:00',
                geometry=b'geom'
            ),
            # signal in zone to bill, created when zone is started and not finished yet
            Record(
                id=33,
                feedback_task_id=33,
                user_id=b'3',
                task_id=b'mrc_pedestrian:3',
                created_at=b'2021-09-01T05:00:00',
                geometry=b'geom'
            ),
            # signal in zone to bill, created when zone is started and not finished yet
            # with region_id in obsolete ugc_account_task_id column instead of task_id
            Record(
                id=41,
                feedback_task_id=41,
                user_id=b'4',
                ugc_account_task_id=b'mrc_pedestrian:4',
                created_at=b'2021-08-30T04:00:00',
                geometry=b'geom'
            ),
        ])
    )

    assert sorted([
        Record(
            feedback_task_id=32,
            mrc_region=b'region 3 (3)',
            user_id=b'3',
            finished_at=b'2021-09-01T02:00:00',
            geom=b'geom'
        ),
        Record(
            feedback_task_id=33,
            mrc_region=b'region 3 (3)',
            user_id=b'3',
            finished_at=b'2021-09-01T22:00:00',
            geom=b'geom'
        ),
        Record(
            feedback_task_id=41,
            mrc_region=b'region 4 (4)',
            user_id=b'4',
            finished_at=b'2021-09-01T04:00:00',
            geom=b'geom'
        ),
    ]) == sorted(result)


def test_should_add_walk_object_resolution():
    result = nile_ut.yt_run(
        mrc_pedestrian_log._add_walk_object_resolution,
        pedestrian_objects=nile_ut.Table([
            Record(
                feedback_task_id=1,
                user_id=b'1',
                finished_at=b'2021-09-01',
                geom=b'geom'
            ),
            Record(
                feedback_task_id=2,
                user_id=b'2',
                finished_at=b'2021-09-01',
                geom=b'geom'
            ),
        ]),
        feedback_dump=nile_ut.Table([
            Record(id=1, resolution=b'accepted'),
            Record(id=2, resolution=b'rejected', reject_reason=b'incorrect-data'),
            Record(id=3, resolution=b'rejected', reject_reason=b'no-data'),
        ])
    )

    assert sorted([
        Record(
            user_id=b'1',
            finished_at=b'2021-09-01',
            geom=b'geom',
            resolution=b'accepted',
            reject_reason=None
        ),
        Record(
            user_id=b'2',
            finished_at=b'2021-09-01',
            geom=b'geom',
            resolution=b'rejected',
            reject_reason=b'incorrect-data'
        ),
    ]) == sorted(result)


@pytest.mark.parametrize(
    "resolution, reject_reason, validity",
    [
        (b'rejected', b'incorrect-data', b'invalid'),
        (b'rejected', b'no-info', b'invalid'),
        (b'rejected', b'no-process', b'invalid'),
        (b'rejected', b'prohibited-by-rules', b'invalid'),
        (b'rejected', b'spam', b'invalid'),
        (b'rejected', b'other', b'valid'),
        (b'approved', None, b'valid'),
        (None, None, b'valid'),
    ]
)
def test_should_add_task_id(resolution, reject_reason, validity):
    result = nile_ut.yt_run(
        mrc_pedestrian_log._add_task_id,
        pedestrian_objects=nile_ut.Table([
            Record(resolution=resolution, reject_reason=reject_reason, test_column=b'test')
        ])
    )
    assert [Record(task_id=b'feedback/created/mrc-pedestrian-region/' + validity, test_column=b'test')] == result


def test_should_convert_to_result_format():
    result = nile_ut.yt_run(
        mrc_pedestrian_log._convert_to_result_format,
        pedestrian_objects=nile_ut.Table([
            Record(
                user_id=b'1',
                finished_at=b'2021-09-01T01:01:01',
                geom=b'01010000000000000000C054400000000000004B40',
                lat_max=54.0,
                lat_min=54.0,
                lon_max=83.0,
                lon_min=83.0,
                task_id=b'feedback/created/mrc-pedestrian-region/valid',
                mrc_region=b'region 1 (1)'
            ),
        ])
    )

    assert sorted([
        Record(
            iso_datetime=b'2021-09-01T01:01:01',
            puid=1,
            task_id=b'feedback/created/mrc-pedestrian-region/valid',
            mrc_region=b'region 1 (1)',
            quantity=1.0,
            geom=b'01010000000000000000C054400000000000004B40',
            lat_max=54.0,
            lat_min=54.0,
            lon_max=83.0,
            lon_min=83.0
        ),
    ]) == sorted(result)


def test_should_create_log():
    result = nile_ut.yt_run(
        mrc_pedestrian_log.make_log,
        date='2021-09-01',
        signals_walk_object_replica=nile_ut.Table([
            Record(
                id=1,
                feedback_task_id=1,
                user_id=b'1',
                task_id=b'mrc_pedestrian:1',
                created_at=b'2021-09-01T03:00:00',
                geometry=b'01010000000000000000C054400000000000004B40'
            ),
        ]),
        feedback_dump=nile_ut.Table([
            Record(id=1, resolution=b'accepted', reject_reason=None),
        ]),
        mrc_pedestrian_region_log=nile_ut.Table([
            Record(object_id=1, status=b'can_start', timestamp=1630443599000000, name=b'region 1'),
            Record(object_id=1, status=b'awaiting_check', timestamp=1630515600000000, name=b'region 1'),
        ])
    )

    assert [Record(
        iso_datetime=b'2021-09-01T20:00:00+03:00',
        puid=1,
        task_id=b'feedback/created/mrc-pedestrian-region/valid',
        mrc_region=b'region 1 (1)',
        quantity=1.0,
        geom=b'01010000000000000000C054400000000000004B40',
        lat_max=54.0,
        lat_min=54.0,
        lon_max=83.0,
        lon_min=83.0
    )] == result
