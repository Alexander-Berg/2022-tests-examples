from maps.wikimap.stat.tasks_payment.tasks_logging.autocart.lib import autocart_log
from maps.wikimap.stat.tasks_payment.tasks_logging.libs.geometry.tests.lib.mocks import geometry_to_bbox_mock
from maps.wikimap.stat.libs import nile_ut
from nile.api.v1 import Record
from nile.drivers.common.progress import CommandFailedError

import pytest


def test_should_get_tasks_dumped_at_and_normalize_logins():
    result = nile_ut.yt_run(
        autocart_log._get_tasks_dumped_at,
        date=b'2021-06-02',
        assessors_table=nile_ut.Table([
            Record(dump_date=b'2021-06-02 01:01:01', login=b'Login 1', shape=b'shape 1', test_column=1),
            Record(dump_date=b'2021-06-03 02:02:02', login=b'login 2', shape=b'shape 2', test_column=2),
            Record(dump_date=b'2021-06-02 03:03:03', login=b'login.3', shape=b'shape 3', test_column=3),
        ])
    )

    assert sorted([
        Record(dump_date=b'2021-06-02 01:01:01', login=b'login 1', shape=b'shape 1'),
        Record(dump_date=b'2021-06-02 03:03:03', login=b'login-3', shape=b'shape 3'),
    ]) == sorted(result)


def test_should_check_for_absent_puids():
    with pytest.raises(AssertionError, match="Login b'login 1' has no puid"):
        list(
            autocart_log._check_for_absent_puids([Record(login=b'login 1')])
        )

    with pytest.raises(AssertionError, match="Login b'login 2' has no puid"):
        list(
            autocart_log._check_for_absent_puids([Record(login=b'login 2', puid=None)])
        )


def test_should_convert_login_to_puid():
    result = nile_ut.yt_run(
        autocart_log._login_to_puid,
        log=nile_ut.Table([
            Record(dump_date=b'date', login=b'login 1', shape=b'shape 11'),
            Record(dump_date=b'date', login=b'login-2', shape=b'shape 21'),
            Record(dump_date=b'date', login=b'login 1', shape=b'shape 12'),
        ]),
        nmaps_users_dump_log=nile_ut.Table([
            Record(um_login=b'Login 1', puid=b'puid 1', test_column=1),
            Record(um_login=b'login.2', puid=b'puid 2', test_column=2),
            Record(um_login=b'login 4', puid=b'puid 4', test_column=4),
        ])
    )

    assert sorted([
        Record(dump_date=b'date', puid=b'puid 1', shape=b'shape 11'),
        Record(dump_date=b'date', puid=b'puid 2', shape=b'shape 21'),
        Record(dump_date=b'date', puid=b'puid 1', shape=b'shape 12'),
    ]) == sorted(result)


def test_should_not_convert_login_to_puid_when_puid_is_absent():
    with pytest.raises(CommandFailedError, match="Login b'login 1' has no puid"):
        nile_ut.yt_run(
            autocart_log._login_to_puid,
            log=nile_ut.Table([
                Record(dump_date=b'date', login=b'login 1', shape=b'shape 11'),
            ]),
            nmaps_users_dump_log=nile_ut.Table([])
        )


def test_should_convert_to_result_format():
    result = nile_ut.yt_run(
        autocart_log._convert_to_result_format,
        log=nile_ut.Table([
            Record(dump_date=b'2021-06-02 01:01:01', puid=b'1', shape=b'shape 1', lat_min=1.1, lon_min=1.2, lat_max=1.3, lon_max=1.4),
            Record(dump_date=b'2021-06-02 02:02:02', puid=b'2', shape=b'shape 2', lat_min=2.1, lon_min=2.2, lat_max=2.3, lon_max=2.4),
        ])
    )

    assert sorted([
        Record(iso_datetime=b'2021-06-02T01:01:01+03:00', puid=1, task_id=b'autocart/bld', quantity=1.0, geom=b'shape 1', lat_min=1.1, lon_min=1.2, lat_max=1.3, lon_max=1.4),
        Record(iso_datetime=b'2021-06-02T02:02:02+03:00', puid=2, task_id=b'autocart/bld', quantity=1.0, geom=b'shape 2', lat_min=2.1, lon_min=2.2, lat_max=2.3, lon_max=2.4),
    ]) == sorted(result)


def test_should_make_log(monkeypatch):
    monkeypatch.setattr(
        'maps.wikimap.stat.tasks_payment.tasks_logging.autocart.lib.autocart_log.geo_wkb_geometry_to_geo_bbox',
        geometry_to_bbox_mock
    )

    result = nile_ut.yt_run(
        autocart_log.make_log,
        '2021-06-02',
        assessors_table=nile_ut.Table([
            Record(dump_date=b'2021-06-02 01:01:01', login=b'login 1', shape=b'1.1, 1.2, 1.3, 1.4'),
            Record(dump_date=b'2021-06-03 02:02:02', login=b'login 1', shape=b'2.1, 2.2, 2.3, 2.4'),  # Wrong date
            Record(dump_date=b'2021-06-02 03:03:03', login=b'login 3', shape=b'3.1, 3.2, 3.3, 3.4'),
            Record(dump_date=b'2021-06-02 04:04:04', login=b'login 3', shape=b'4.1, 4.2, 4.3, 4.4'),
        ]),
        nmaps_users_dump_log=nile_ut.Table([
            Record(um_login=b'login 1', puid=b'1'),
            Record(um_login=b'login 2', puid=b'2'),  # excess login
            Record(um_login=b'login 3', puid=b'3'),
        ])
    )

    assert sorted([
        Record(iso_datetime=b'2021-06-02T01:01:01+03:00', puid=1, task_id=b'autocart/bld', quantity=1.0,
               geom=b'1.1, 1.2, 1.3, 1.4', lat_min=1.1, lon_min=1.2, lat_max=1.3, lon_max=1.4),
        Record(iso_datetime=b'2021-06-02T03:03:03+03:00', puid=3, task_id=b'autocart/bld', quantity=1.0,
               geom=b'3.1, 3.2, 3.3, 3.4', lat_min=3.1, lon_min=3.2, lat_max=3.3, lon_max=3.4),
        Record(iso_datetime=b'2021-06-02T04:04:04+03:00', puid=3, task_id=b'autocart/bld', quantity=1.0,
               geom=b'4.1, 4.2, 4.3, 4.4', lat_min=4.1, lon_min=4.2, lat_max=4.3, lon_max=4.4),
    ]) == sorted(result)
