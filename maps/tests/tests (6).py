from maps.wikimap.stat.tasks_payment.tasks_logging.assessment.lib import assessment_log
from maps.wikimap.stat.libs import nile_ut

from cyson import UInt
from nile.api.v1 import Record
from nile.drivers.common.progress import CommandFailedError
from yt.yson import to_yson_type

import pytest


def test_should_get_grades_at():
    result = nile_ut.yt_run(
        assessment_log._get_grades_at,
        date=b'2021-05-14',
        assessment_grade_table=nile_ut.Table([
            Record(unit_id=1, graded_by=1, graded_at=b'2021-05-13T01:01:01.123456+01:00', test_column=1),
            Record(unit_id=2, graded_by=2, graded_at=b'2021-05-14T02:02:02.123456+02:00', test_column=2),
            Record(unit_id=3, graded_by=3, graded_at=b'2021-05-15T03:03:03.123456+03:00', test_column=3),
            Record(unit_id=4, graded_by=4, graded_at=b'2021-05-14T04:04:04.123456+04:00', test_column=4),
        ])
    )

    assert sorted([
        Record(unit_id=2, graded_by=2, graded_at=b'2021-05-14T02:02:02.123456+02:00'),
        Record(unit_id=4, graded_by=4, graded_at=b'2021-05-14T04:04:04.123456+04:00'),
    ]) == sorted(result)


def test_should_add_entity():
    result = nile_ut.yt_run(
        assessment_log._add_entity,
        log=nile_ut.Table([
            Record(unit_id=1, graded_by=11, graded_at=b'2021-05-14T01:01:01.123456+01:00'),
            Record(unit_id=1, graded_by=12, graded_at=b'2021-05-14T02:02:02.123456+02:00'),
            Record(unit_id=2, graded_by=21, graded_at=b'2021-05-14T03:03:03.123456+03:00'),
        ]),
        assessment_unit_table=nile_ut.Table([
            Record(unit_id=1, entity_domain=b'domain 1', entity_id=b'101', test_column=1),
            Record(unit_id=2, entity_domain=b'domain 2', entity_id=b'102', test_column=2),
            Record(unit_id=3, entity_domain=b'domain 3', entity_id=b'103', test_column=3),
        ]),
    )

    assert sorted([
        Record(graded_by=11, graded_at=b'2021-05-14T01:01:01.123456+01:00', entity_domain=b'domain 1', entity_id=b'101'),
        Record(graded_by=12, graded_at=b'2021-05-14T02:02:02.123456+02:00', entity_domain=b'domain 1', entity_id=b'101'),
        Record(graded_by=21, graded_at=b'2021-05-14T03:03:03.123456+03:00', entity_domain=b'domain 2', entity_id=b'102'),
    ]) == sorted(result)


def test_should_split_log():
    result_edits, result_moderation, result_feedback, result_tracker = nile_ut.yt_run(
        assessment_log._split,
        log=nile_ut.Table([
            Record(entity_domain=b'edits',      test_column=101),
            Record(entity_domain=b'feedback',   test_column=102),
            Record(entity_domain=b'moderation', test_column=103),
            Record(entity_domain=b'tracker',    test_column=104),
            Record(entity_domain=b'feedback',   test_column=105),
            Record(entity_domain=b'other',      test_column=106),
        ])
    )

    assert sorted([
        Record(entity_domain=b'edits', test_column=101),
    ]) == sorted(result_edits)

    assert sorted([
        Record(entity_domain=b'feedback', test_column=102),
        Record(entity_domain=b'feedback', test_column=105),
    ]) == sorted(result_feedback)

    assert sorted([
        Record(entity_domain=b'moderation', test_column=103),
    ]) == sorted(result_moderation)

    assert sorted([
        Record(entity_domain=b'tracker', test_column=104),
    ]) == sorted(result_tracker)


def test_should_convert_entity_to_int():
    result = nile_ut.yt_run(
        assessment_log._convert_entity_id_to_int,
        log=nile_ut.Table([
            Record(entity_id=b'42', test_column=24)
        ])
    )

    assert [Record(entity_id=42, test_column=24)] == result


def test_should_add_edits_category_id():
    result = nile_ut.yt_run(
        assessment_log._add_edits_category_id,
        edits_log=nile_ut.Table([
            Record(graded_by=11, graded_at=b'2021-05-14T01:01:01.123456+01:00', entity_domain=b'edits', entity_id=101),
            Record(graded_by=33, graded_at=b'2021-05-14T03:03:03.123456+03:00', entity_domain=b'edits', entity_id=103),  # common (category is absent in social.commit_event)
            Record(graded_by=77, graded_at=b'2021-05-14T07:07:07.123456+07:00', entity_domain=b'edits', entity_id=107),  # service (no entry in social.commit_event)
        ]),
        social_commit_event_table=nile_ut.Table([
            Record(commit_id=101, primary_object_category_id=b'category 1', type=to_yson_type(b'edit'),  test_column=1),
            Record(commit_id=102, primary_object_category_id=b'category 2', type=to_yson_type(b'edit'),  test_column=2),
            Record(commit_id=103, primary_object_category_id=None,          type=to_yson_type(b'edit'),  test_column=3),
            Record(commit_id=104, primary_object_category_id=None,          type=to_yson_type(b'edit'),  test_column=4),
            Record(commit_id=105, primary_object_category_id=b'category 5', type=to_yson_type(b'edit'),  test_column=5),
            Record(commit_id=103, primary_object_category_id=b'category 6', type=to_yson_type(b'other'), test_column=6),
        ]),
    )

    assert sorted([
        Record(graded_by=11, graded_at=b'2021-05-14T01:01:01.123456+01:00', entity_domain=b'edits', category_id=b'category 1'),
        Record(graded_by=33, graded_at=b'2021-05-14T03:03:03.123456+03:00', entity_domain=b'edits', category_id=b'common'),
        Record(graded_by=77, graded_at=b'2021-05-14T07:07:07.123456+07:00', entity_domain=b'edits', category_id=b'service'),
    ]) == sorted(result)


def test_should_add_moderation_category_id():
    result = nile_ut.yt_run(
        assessment_log._add_moderation_category_id,
        moderation_log=nile_ut.Table([
            Record(graded_by=21, graded_at=b'2021-05-14T02:02:02.123456+02:00', entity_domain=b'moderation', entity_id=102),
            Record(graded_by=21, graded_at=b'2021-05-14T04:04:04.123456+04:00', entity_domain=b'moderation', entity_id=104),
        ]),
        social_task_table=nile_ut.Table([
            Record(event_id=101, primary_object_category_id=b'category 1', test_column=1),
            Record(event_id=102, primary_object_category_id=b'category 2', test_column=2),
            Record(event_id=103, primary_object_category_id=None,          test_column2=3),
            Record(event_id=104, primary_object_category_id=None,          test_column2=4),
            Record(event_id=105, primary_object_category_id=b'category 3', test_column=3),
        ]),
    )

    assert sorted([
        Record(graded_by=21, graded_at=b'2021-05-14T02:02:02.123456+02:00', entity_domain=b'moderation', category_id=b'category 2'),
        Record(graded_by=21, graded_at=b'2021-05-14T04:04:04.123456+04:00', entity_domain=b'moderation', category_id=b'common'),
    ]) == sorted(result)


def test_should_add_task_id_to_commits_log():
    result = nile_ut.yt_run(
        assessment_log._add_task_id_to_commits_log,
        commits_log=nile_ut.Table([
            Record(entity_domain=b'domain', category_id=b'category', test_column=42)
        ])
    )
    assert [Record(task_id=b'assessment/domain/category', test_column=42)] == result


def test_should_fail_add_task_id_to_commits_log_for_empty_data():
    with pytest.raises(CommandFailedError, match='expected a bytes-like object, NoneType found'):
        nile_ut.yt_run(
            assessment_log._add_task_id_to_commits_log,
            commits_log=nile_ut.Table([
                Record(entity_domain=None, category_id=b'category', test_column=42)
            ])
        )

    with pytest.raises(CommandFailedError, match='expected a bytes-like object, NoneType found'):
        nile_ut.yt_run(
            assessment_log._add_task_id_to_commits_log,
            commits_log=nile_ut.Table([
                Record(entity_domain=b'domain', category_id=None, test_column=42)
            ])
        )


def test_should_add_feedback_source_type():
    result = nile_ut.yt_run(
        assessment_log._add_feedback_source_type,
        feedback_log=nile_ut.Table([
            Record(graded_by=12, graded_at=b'2021-05-14T01:01:01.123456+01:00', entity_domain=b'feedback', entity_id=101),
        ]),
        social_feedback_task_table=nile_ut.Table([
            Record(id=UInt(101), source=b'source 1', type=b'type 1', test_column=1),

            Record(id=UInt(102), source=b'source 2', type=b'type 2', test_column=2),  # no related records in feedback log
        ]),
    )

    assert [
        Record(graded_by=12, graded_at=b'2021-05-14T01:01:01.123456+01:00', entity_domain=b'feedback', feedback_source=b'source 1', feedback_type=b'type 1'),
    ] == result


def test_should_add_task_id_to_feedback_log():
    result = nile_ut.yt_run(
        assessment_log._add_task_id_to_feedback_log,
        feedback_log=nile_ut.Table([
            Record(entity_domain=b'domain', feedback_source=b'source', feedback_type=b'type', test_column=42)
        ])
    )
    assert [Record(task_id=b'assessment/domain/source/type', test_column=42)] == result


def test_should_fail_add_task_id_to_feedback_log_for_empty_data():
    with pytest.raises(CommandFailedError, match='expected a bytes-like object, NoneType found'):
        nile_ut.yt_run(
            assessment_log._add_task_id_to_feedback_log,
            feedback_log=nile_ut.Table([
                Record(entity_domain=None, feedback_source=b'source', feedback_type=b'type', test_column=42)
            ])
        )

    with pytest.raises(CommandFailedError, match='expected a bytes-like object, NoneType found'):
        nile_ut.yt_run(
            assessment_log._add_task_id_to_feedback_log,
            feedback_log=nile_ut.Table([
                Record(entity_domain=b'domain', feedback_source=None, feedback_type=b'type', test_column=42)
            ])
        )

    with pytest.raises(CommandFailedError, match='expected a bytes-like object, NoneType found'):
        nile_ut.yt_run(
            assessment_log._add_task_id_to_feedback_log,
            feedback_log=nile_ut.Table([
                Record(entity_domain=b'domain', feedback_source=b'source', feedback_type=None, test_column=42)
            ])
        )


def test_should_add_task_id_to_tracker_log():
    result = nile_ut.yt_run(
        assessment_log._add_task_id_to_tracker_log,
        tracker_log=nile_ut.Table([
            Record(graded_by=11, graded_at=b'at 1', entity_domain=b'domain 1', entity_id=b'queue-1'),
            Record(graded_by=33, graded_at=b'at 3', entity_domain=b'domain 3', entity_id=b'another-queue-3'),
        ]),
        issues_table=nile_ut.Table([
            Record(key=b'queue-1',         components=to_yson_type([b'component id 1']), issues_other_column=1),
            Record(key=b'queue-2',         components=to_yson_type([b'component id 2']), issues_other_column=2),
            Record(key=b'another-queue-3', components=to_yson_type([b'component id 3']), issues_other_column=3),
        ]),
        components_table=nile_ut.Table([
            Record(id=b'component id 1', name=b'component 1', components_other_column=1),
            Record(id=b'component id 2', name=b'component 2', components_other_column=2),
            Record(id=b'component id 3', name=b'component 3', components_other_column=3),
        ])
    )

    assert sorted([
        Record(graded_by=11, graded_at=b'at 1', task_id=b'assessment/tracker/queue/component 1'),
        Record(graded_by=33, graded_at=b'at 3', task_id=b'assessment/tracker/another-queue/component 3'),
    ]) == sorted(result)


def test_should_convert_to_result_format():
    result = nile_ut.yt_run(
        assessment_log._convert_to_result_format,
        log=nile_ut.Table([
            Record(graded_by=1, graded_at=b'2021-05-14T01:01:01.123456+01:00', task_id=b'task id 1'),
            Record(graded_by=2, graded_at=b'2021-05-14T02:02:02.123456+02:00', task_id=b'task id 2'),
            Record(graded_by=3, graded_at=b'2021-05-14T03:03:03.123456+03:00', task_id=b'task id 3'),
        ])
    )

    assert sorted([
        Record(iso_datetime=b'2021-05-14T01:01:01+01:00', puid=1, task_id=b'task id 1', quantity=1.0, geom=None, lat_min=None, lon_min=None, lat_max=None, lon_max=None),
        Record(iso_datetime=b'2021-05-14T02:02:02+02:00', puid=2, task_id=b'task id 2', quantity=1.0, geom=None, lat_min=None, lon_min=None, lat_max=None, lon_max=None),
        Record(iso_datetime=b'2021-05-14T03:03:03+03:00', puid=3, task_id=b'task id 3', quantity=1.0, geom=None, lat_min=None, lon_min=None, lat_max=None, lon_max=None),
    ]) == sorted(result)


def test_should_make_log():
    result = nile_ut.yt_run(
        assessment_log.make_log,
        nile_ut.Job(),
        '2021-05-14',
        assessment_grade_table=nile_ut.Table([
            Record(unit_id=1, graded_by=11, graded_at=b'2021-05-14T01:01:01.123456+01:00'),
            Record(unit_id=2, graded_by=21, graded_at=b'2021-05-14T02:02:02.123456+02:00'),
            Record(unit_id=2, graded_by=22, graded_at=b'2021-05-14T03:03:03.123456+03:00'),
            Record(unit_id=3, graded_by=31, graded_at=b'2021-05-14T04:04:04.123456+04:00'),
            Record(unit_id=4, graded_by=41, graded_at=b'2021-05-14T05:05:05.123456+05:00'),

            # Filtered out by date:
            Record(unit_id=3, graded_by=31, graded_at=b'2021-05-13T04:04:04.123456+04:00'),
            Record(unit_id=5, graded_by=51, graded_at=b'2021-05-13T05:05:05.123456+05:00'),
        ]),
        assessment_unit_table=nile_ut.Table([
            Record(unit_id=1, entity_domain=b'edits',      entity_id=b'101'),
            Record(unit_id=2, entity_domain=b'feedback',   entity_id=b'101'),
            Record(unit_id=3, entity_domain=b'moderation', entity_id=b'102'),
            Record(unit_id=4, entity_domain=b'tracker',    entity_id=b'queue-104'),

            Record(unit_id=5, entity_domain=b'edits',      entity_id=b'105'),  # received no grades at 2021-05-14
        ]),
        social_commit_event_table=nile_ut.Table([
            Record(commit_id=101, primary_object_category_id=b'category c1', type=to_yson_type(b'edit'),  test_column1=1),
            Record(commit_id=102, primary_object_category_id=b'category c2', type=to_yson_type(b'edit'),  test_column1=2),

            Record(commit_id=103, primary_object_category_id=b'category c3', type=to_yson_type(b'edit'),  test_column1=2),  # received no grades at 2021-05-14
            Record(commit_id=104, primary_object_category_id=b'category c4', type=to_yson_type(b'edit'),  test_column1=2),  # no such assessment unit
            Record(commit_id=105, primary_object_category_id=b'category c5', type=to_yson_type(b'other'), test_column1=2),  # filtered out by event type
        ]),
        social_feedback_task_table=nile_ut.Table([
            Record(id=UInt(101), source=b'source 1', type=b'type 1', test_column2=1),

            Record(id=UInt(102), source=b'source 1', type=b'type 2', test_column2=2),  # no such assessment unit
        ]),
        social_task_table=nile_ut.Table([
            Record(event_id=102, primary_object_category_id=b'category t2', test_column3=2),
            Record(event_id=105, primary_object_category_id=b'category t5', test_column3=5),
        ]),
        issues_table=nile_ut.Table([
            Record(key=b'queue-104', components=to_yson_type([b'component id 4'])),
        ]),
        components_table=nile_ut.Table([
            Record(id=b'component id 4', name=b'component 4'),
        ]),
    )

    assert sorted([
        Record(iso_datetime=b'2021-05-14T01:01:01+01:00', puid=11, task_id=b'assessment/edits/category c1',         quantity=1.0, geom=None, lat_min=None, lon_min=None, lat_max=None, lon_max=None),
        Record(iso_datetime=b'2021-05-14T02:02:02+02:00', puid=21, task_id=b'assessment/feedback/source 1/type 1',  quantity=1.0, geom=None, lat_min=None, lon_min=None, lat_max=None, lon_max=None),
        Record(iso_datetime=b'2021-05-14T03:03:03+03:00', puid=22, task_id=b'assessment/feedback/source 1/type 1',  quantity=1.0, geom=None, lat_min=None, lon_min=None, lat_max=None, lon_max=None),
        Record(iso_datetime=b'2021-05-14T04:04:04+04:00', puid=31, task_id=b'assessment/moderation/category t2',    quantity=1.0, geom=None, lat_min=None, lon_min=None, lat_max=None, lon_max=None),
        Record(iso_datetime=b'2021-05-14T05:05:05+05:00', puid=41, task_id=b'assessment/tracker/queue/component 4', quantity=1.0, geom=None, lat_min=None, lon_min=None, lat_max=None, lon_max=None),
    ]) == sorted(result)
