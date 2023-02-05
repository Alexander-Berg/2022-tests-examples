from maps.wikimap.feedback.pushes.filter_pushes.lib.filter_pushes import (
    filter_pushes,
    filter_useless_and_opened_mapper,
    prepare_pushes_to_filter,
)
from maps.wikimap.feedback.pushes.helpers import helpers
from nile.api.v1 import (
    clusters,
    local,
    Record,
)
from qb2.api.v1 import typing as qt
from nile.drivers.debug.stream import DebugWriter
from yt.yson import to_yson_type


def test_filter_useless_and_opened_mapper():
    records = [
        Record(
            push_metrika_events={b"ButtonClicked": {b"useless": []}},
            puid="uid1"
        ),
        Record(
            push_metrika_events={b"ButtonClicked": {b"useless": []}},
            puid=None
        ),
        Record(
            push_metrika_events={b"Delivered": {}, b"Shown": {}, b"Opened": {}},
            puid="uid2"
        ),
        Record(
            useless=b"1",
            push_metrika_events={b"Delivered": {}, b"Opened": {}, b"ButtonClicked": {b"useless": []}},
            puid="uid3"
        ),
    ]
    useless = DebugWriter()
    opened = DebugWriter()

    filter_useless_and_opened_mapper(records, useless, opened)

    helpers.compare_records_lists(
        sorted(useless),
        sorted([Record(uid="uid1"), Record(uid="uid3")])
    )
    helpers.compare_records_lists(
        sorted(opened),
        [Record(uid="uid2")]
    )


def test_prepare_pushes_to_filter():
    cluster = clusters.MockYQLCluster()
    job = cluster.job()
    prepare_pushes_to_filter(job, "//sup_logs_path", b"geoplatform_address_request")
    useless = []
    opened = []
    job.local_run(
        sources={
            "sup_log": local.StreamSource(
                [
                    Record(
                        push_metrika_events={
                            b"ButtonClicked": {b"useless": []},
                            b"Delivered": {},
                            b"Shown": {},
                            b"Opened": {},
                        },
                        push_type=b"test",  # filter by push_type
                        puid=b"uid1"
                    ),
                    Record(
                        push_metrika_events={
                            b"ButtonClicked": {b"useless": []},
                            # not delivered
                            b"Shown": {},
                            b"Opened": {},
                        },
                        push_type=b"geoplatform_address_request",
                        puid=b"uid2"
                    ),
                    Record(
                        push_metrika_events={
                            b"ButtonClicked": {b"useless": []},
                            b"Delivered": {},
                            # not shown
                        },
                        push_type=b"geoplatform_address_request",
                        puid=b"uid3"
                    ),
                    Record(
                        push_metrika_events={
                            b"ButtonClicked": {b"useless": []},
                            b"Delivered": {},
                            b"Shown": {},
                        },
                        push_type=b"geoplatform_address_request",
                        puid=b"uid4"
                    ),
                    Record(
                        push_metrika_events={
                            b"Delivered": {},
                            b"Shown": {},
                            # not opened
                        },
                        useless=None,
                        push_type=b"geoplatform_address_request",
                        puid=b"uid5"
                    ),
                    Record(
                        push_metrika_events={
                            b"Delivered": {},
                            b"Shown": {},
                            b"Opened": {},
                        },
                        push_type=b"geoplatform_address_request",
                        puid=b"uid6",
                    ),
                ],
                schema={
                    "push_metrika_events": qt.Optional[qt.Yson],
                    "push_type": qt.Optional[qt.String],
                    "useless": qt.Optional[qt.String],
                    "puid": qt.Optional[qt.String],
                }
            ),
        },
        sinks={
            "useless": local.ListSink(useless),
            "opened": local.ListSink(opened),
        }
    )
    helpers.compare_records_lists(
        useless,
        [Record(uid=b"uid4")]
    )
    helpers.compare_records_lists(
        opened,
        [Record(uid=b"uid6")]
    )


def _prepare_job():
    cluster = clusters.MockYQLCluster()
    job = cluster.job()
    filter_pushes(
        job,
        push_type=b"geoplatform_address_request",
        push_id_field="ghash9",
        input_table="//input_table",
        old_pushes_paths=["//old_pushes_path1", "//old_pushes_path2"],
        sup_logs_path="//sup_logs_path",
    )
    return job


def test_non_useless():
    job = _prepare_job()
    non_useless = []
    job.local_run(
        sources={
            "new_pushes": local.StreamSource([
                Record(uid=b"uid1"),
                Record(uid=b"uid2"),
                Record(uid=b"uid3"),
            ]),
            "useless": local.StreamSource([
                Record(uid=b"uid1"),
            ]),
        },
        sinks={
            "non_useless": local.ListSink(non_useless)
        }
    )
    helpers.compare_records_lists(
        sorted(non_useless),
        [Record(uid=b"uid2"), Record(uid=b"uid3")]
    )


def test_opened_old_pushes():
    job = _prepare_job()
    opened_old_pushes = []
    job.local_run(
        sources={
            "old_pushes": local.StreamSource([
                Record(uid=b"uid1"),
                Record(uid=b"uid2"),
                Record(uid=b"uid3"),
            ]),
            "opened": local.StreamSource([
                Record(uid=b"uid1"),
            ]),
        },
        sinks={
            "opened_old_pushes": local.ListSink(opened_old_pushes)
        }
    )
    helpers.compare_records_lists(
        sorted(opened_old_pushes),
        [Record(uid=b"uid1")]
    )


def test_filter_pushes():
    job = _prepare_job()
    filtered_pushes = []
    job.local_run(
        sources={
            "new_pushes": local.StreamSource([
                Record(uid=b"uid1", ghash9=b"ghash1"),
                Record(uid=b"uid2", ghash9=b"ghash2"),
                Record(uid=b"uid3", ghash9=b"ghash3"),
            ]),
            "opened_old_pushes": local.StreamSource([
                Record(uid=b"uid1", ghash9=b"ghash1"),
                Record(uid=b"uid3", ghash9=b"ghash3_"),
            ]),
            "useless": local.StreamSource([
                Record(uid=b"uid2"),
            ]),
        },
        sinks={
            "filtered_pushes": local.ListSink(filtered_pushes)
        }
    )
    helpers.compare_records_lists(
        sorted(filtered_pushes),
        sorted([
            Record(uid=b"uid3", ghash9=b"ghash3"),
        ])
    )


def test_prepare_ugc_assignments_data():
    cluster = clusters.MockYQLCluster()
    job = cluster.job()
    filter_pushes(
        job,
        push_type=b"geoplatform_settlement_scheme",
        push_id_field="assignment_id",
        input_table="//input_table",
        old_pushes_paths=["//old_pushes_path1", "//old_pushes_path2"],
        sup_logs_path="//sup_logs_path",
    )
    ugc_data = []
    job.local_run(
        sources={
            "raw_ugc_data": local.StreamSource([
                Record(uid=1, task_id=b"task1", status=to_yson_type(b"done")),
                Record(uid=1, task_id=b"task2", status=to_yson_type(b"active")),
                Record(uid=2, task_id=b"task1", status=to_yson_type(b"active")),
                Record(uid=3, task_id=b"task3", status=to_yson_type(b"expired")),
            ]),
        },
        sinks={
            "ugc_data": local.ListSink(ugc_data)
        }
    )
    helpers.compare_records_lists(
        sorted(ugc_data),
        sorted([
            Record(uid=b"1", assignment_id=b"task2"),
            Record(uid=b"2", assignment_id=b"task1"),
        ])
    )


def test_filter_by_ugc_status():
    cluster = clusters.MockYQLCluster()
    job = cluster.job()
    filter_pushes(
        job,
        push_type=b"geoplatform_settlement_scheme",
        push_id_field="assignment_id",
        input_table="//input_table",
        old_pushes_paths=["//old_pushes_path1", "//old_pushes_path2"],
        sup_logs_path="//sup_logs_path",
    )
    result = []
    job.local_run(
        sources={
            "ugc_data": local.StreamSource([
                Record(uid=b"1", assignment_id=b"task1"),
                Record(uid=b"1", assignment_id=b"task2"),
                Record(uid=b"3", assignment_id=b"task3"),
            ]),
            "filtered_pushes": local.StreamSource([
                Record(uid=b"1", assignment_id=b"task1"),
                Record(uid=b"2", assignment_id=b"task2"),
                Record(uid=b"3", assignment_id=b"task1"),
                Record(uid=b"4", assignment_id=b"task1"),
            ]),
        },
        sinks={
            "filtered_by_ugc_status": local.ListSink(result)
        }
    )
    helpers.compare_records_lists(
        sorted(result),
        sorted([
            Record(uid=b"1", assignment_id=b"task1"),
        ])
    )
