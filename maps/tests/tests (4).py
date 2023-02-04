from maps.wikimap.stat.tasks_payment.tasks_logging.moderation.lib.moderation_log import make_moderation_log_job
from maps.wikimap.stat.tasks_payment.tasks_logging.libs.geometry.tests.lib.mocks import geometry_to_bbox_mock

from nile.api.v1 import (
    Record,
    clusters,
    local
)


COMMIT_EVENT_PATH = '//path/to/social_commit_event'
RESULT_PATH = '//path/to/moderation_log'
TASK_ACTIVE_PATH = '//path/to/social_task_active'
TASK_CLOSED_PATH = '//path/to/social_task_closed'


def test_should_remove_tasks_by_date():
    job = clusters.MockCluster().job()

    result = []

    social_task_closed = [
        # Both wrong
        Record(event_id=1, closed_at=b'2020-01-24', closed_by=1, resolved_at=b'2020-01-24', resolved_by=1, primary_object_category_id=b'cat 1'),
        # Correct closed_at
        Record(event_id=2, closed_at=b'2020-01-25', closed_by=2, resolved_at=b'2020-01-26', resolved_by=2, primary_object_category_id=b'cat 2'),
        # Correct resolved_by
        Record(event_id=3, closed_at=b'2020-01-26', closed_by=3, resolved_at=b'2020-01-25', resolved_by=3, primary_object_category_id=b'cat 3'),
        # Correct both
        Record(event_id=4, closed_at=b'2020-01-25', closed_by=4, resolved_at=b'2020-01-25', resolved_by=4, primary_object_category_id=b'cat 4')
    ]

    make_moderation_log_job(
        job, '2020-01-25',
        RESULT_PATH, TASK_ACTIVE_PATH, TASK_CLOSED_PATH, COMMIT_EVENT_PATH
    ).local_run(
        sources={
            'social.task_active': local.StreamSource([]),
            'social.task_closed': local.StreamSource(social_task_closed)
        },
        sinks={'tasks': local.ListSink(result)}
    )

    assert result == [
        Record(event_id=2, closed_at=b'2020-01-25', closed_by=2, resolved_at=b'2020-01-26', resolved_by=2, primary_object_category_id=b'cat 2'),
        Record(event_id=3, closed_at=b'2020-01-26', closed_by=3, resolved_at=b'2020-01-25', resolved_by=3, primary_object_category_id=b'cat 3'),
        Record(event_id=4, closed_at=b'2020-01-25', closed_by=4, resolved_at=b'2020-01-25', resolved_by=4, primary_object_category_id=b'cat 4')
    ]


def test_remove_tasks_if_closed_at_or_resolved_at_absent():
    job = clusters.MockCluster().job()

    result = []

    social_task_active = [
        Record(event_id=1, closed_at=None, closed_by=0, resolved_at=b'2020-01-24', resolved_by=1, primary_object_category_id=b'cat 1'),
        Record(event_id=2, closed_at=None, closed_by=0, resolved_at=b'2020-01-25', resolved_by=2, primary_object_category_id=b'cat 2'),
        Record(event_id=3, closed_at=None, closed_by=0, resolved_at=None,          resolved_by=0, primary_object_category_id=b'cat 3')
    ]

    make_moderation_log_job(
        job, '2020-01-25',
        RESULT_PATH, TASK_ACTIVE_PATH, TASK_CLOSED_PATH, COMMIT_EVENT_PATH
    ).local_run(
        sources={
            'social.task_active': local.StreamSource(social_task_active),
            'social.task_closed': local.StreamSource([])
        },
        sinks={'tasks': local.ListSink(result)}
    )

    assert result == [
        Record(event_id=2, closed_at=None, closed_by=0, resolved_at=b'2020-01-25', resolved_by=2, primary_object_category_id=b'cat 2')
    ]


def test_should_not_remove_tasks_by_date_border_cases():
    'This test also checks that timezone is not taken into account.'

    job = clusters.MockCluster().job()

    result = []

    social_task_closed = [
        Record(event_id=11, closed_at=b'2020-01-24T23:59:59+03:00', closed_by=1, resolved_at=b'2020-01-23', resolved_by=1, primary_object_category_id=b'cat 1'),
        Record(event_id=12, closed_at=b'2020-01-25T00:00:00+03:00', closed_by=2, resolved_at=b'2020-01-23', resolved_by=2, primary_object_category_id=b'cat 2'),
        Record(event_id=13, closed_at=b'2020-01-25T23:59:59+03:00', closed_by=3, resolved_at=b'2020-01-23', resolved_by=3, primary_object_category_id=b'cat 3'),
        Record(event_id=14, closed_at=b'2020-01-26T00:00:00+03:00', closed_by=4, resolved_at=b'2020-01-23', resolved_by=4, primary_object_category_id=b'cat 4'),

        Record(event_id=21, closed_at=b'2020-01-23', closed_by=1, resolved_at=b'2020-01-24T23:59:59+03:00', resolved_by=1, primary_object_category_id=b'cat 1'),
        Record(event_id=22, closed_at=b'2020-01-23', closed_by=2, resolved_at=b'2020-01-25T00:00:00+03:00', resolved_by=2, primary_object_category_id=b'cat 2'),
        Record(event_id=23, closed_at=b'2020-01-23', closed_by=3, resolved_at=b'2020-01-25T23:59:59+03:00', resolved_by=3, primary_object_category_id=b'cat 3'),
        Record(event_id=24, closed_at=b'2020-01-23', closed_by=4, resolved_at=b'2020-01-26T00:00:00+03:00', resolved_by=4, primary_object_category_id=b'cat 4')
    ]

    make_moderation_log_job(
        job, '2020-01-25',
        RESULT_PATH, TASK_ACTIVE_PATH, TASK_CLOSED_PATH, COMMIT_EVENT_PATH
    ).local_run(
        sources={
            'social.task_active': local.StreamSource([]),
            'social.task_closed': local.StreamSource(social_task_closed)
        },
        sinks={'tasks': local.ListSink(result)}
    )

    assert result == [
        Record(event_id=12, closed_at=b'2020-01-25T00:00:00+03:00', closed_by=2, resolved_at=b'2020-01-23', resolved_by=2, primary_object_category_id=b'cat 2'),
        Record(event_id=13, closed_at=b'2020-01-25T23:59:59+03:00', closed_by=3, resolved_at=b'2020-01-23', resolved_by=3, primary_object_category_id=b'cat 3'),

        Record(event_id=22, closed_at=b'2020-01-23', closed_by=2, resolved_at=b'2020-01-25T00:00:00+03:00', resolved_by=2, primary_object_category_id=b'cat 2'),
        Record(event_id=23, closed_at=b'2020-01-23', closed_by=3, resolved_at=b'2020-01-25T23:59:59+03:00', resolved_by=3, primary_object_category_id=b'cat 3'),
    ]


def test_should_append_geometry_and_created_by():
    job = clusters.MockCluster().job()

    result = []

    tasks = [
        Record(event_id=1, closed_at=b'2020-01-25', closed_by=1, resolved_at=b'2020-01-25', resolved_by=1, primary_object_category_id=b'cat 1'),
        Record(event_id=3, closed_at=b'2020-01-25', closed_by=3, resolved_at=b'2020-01-25', resolved_by=3, primary_object_category_id=b'cat 3'),
        Record(event_id=4, closed_at=b'2020-01-25', closed_by=4, resolved_at=b'2020-01-25', resolved_by=4, primary_object_category_id=b'cat 4')
    ]

    commit_event = [
        Record(event_id=1, bounds_geom=b'HEX GEOMETRY 1', created_by=1),
        Record(event_id=2, bounds_geom=b'HEX GEOMETRY 2', created_by=2),
        Record(event_id=4, bounds_geom=b'HEX GEOMETRY 4', created_by=4)
    ]

    make_moderation_log_job(
        job, '2020-01-25',
        RESULT_PATH, TASK_ACTIVE_PATH, TASK_CLOSED_PATH, COMMIT_EVENT_PATH
    ).local_run(
        sources={
            'tasks': local.StreamSource(tasks),
            'social.commit_event': local.StreamSource(commit_event)
        },
        sinks={'tasks with geometry and created_by': local.ListSink(result)}
    )

    assert sorted(result) == sorted([
        Record(closed_at=b'2020-01-25', closed_by=1, resolved_at=b'2020-01-25', resolved_by=1, created_by=1, primary_object_category_id=b'cat 1', bounds_geom=b'HEX GEOMETRY 1'),
        Record(closed_at=b'2020-01-25', closed_by=3, resolved_at=b'2020-01-25', resolved_by=3, created_by=None, primary_object_category_id=b'cat 3', bounds_geom=None),
        Record(closed_at=b'2020-01-25', closed_by=4, resolved_at=b'2020-01-25', resolved_by=4, created_by=4, primary_object_category_id=b'cat 4', bounds_geom=b'HEX GEOMETRY 4')
    ])


def test_should_split_tasks_by_closed_at_and_resolved_at():
    job = clusters.MockCluster().job()

    closed_tasks = []
    resolved_tasks = []

    tasks_with_bbox = [
        Record(closed_at=b'2020-01-24',       closed_by=1,   resolved_at=b'2020-01-24',       resolved_by=1),   # Another date
        Record(closed_at=b'2020-01-24',       closed_by=2,   resolved_at=b'2020-01-25',       resolved_by=2),   # Resolved at date
        Record(closed_at=b'2020-01-25',       closed_by=3,   resolved_at=b'2020-01-24',       resolved_by=3),   # Closed at date
        Record(closed_at=b'2020-01-25', closed_by=82282794,  resolved_at=b'2020-01-25',       resolved_by=4),   # Autoclosed task, closed by robot
        Record(closed_at=b'2020-01-25',       closed_by=8,   resolved_at=b'2020-01-25',       resolved_by=8),   # Autoclosed task, closed by resolver
        Record(closed_at=b'2020-01-25',       closed_by=9,   resolved_at=b'2020-01-25',       resolved_by=82282794),   # Autoclosed task, with confused robot and moderator
        Record(closed_at=None,                closed_by=0,   resolved_at=b'2020-01-25',       resolved_by=5),   # Not closed task
        Record(closed_at=b'2020-01-25',       closed_by=61,  resolved_at=b'2020-01-25',       resolved_by=62),  # resolved_by != closed_by
        Record(closed_at=b'2020-01-25 17:17', closed_by=7,   resolved_at=b'2020-01-25 17:07', resolved_by=7),   # resolved_at != closed_at

    ]

    make_moderation_log_job(
        job, '2020-01-25',
        RESULT_PATH, TASK_ACTIVE_PATH, TASK_CLOSED_PATH, COMMIT_EVENT_PATH
    ).local_run(
        sources={'tasks with bbox': local.StreamSource(tasks_with_bbox)},
        sinks={
            'closed tasks': local.ListSink(closed_tasks),
            'resolved tasks': local.ListSink(resolved_tasks),
        }
    )

    assert sorted(closed_tasks) == sorted([
        Record(closed_at=b'2020-01-25', closed_by=82282794),  # Autoclosed task by 4
        Record(closed_at=b'2020-01-25', closed_by=82282794),  # Autoclosed task by 8
        Record(closed_at=b'2020-01-25', closed_by=82282794),  # Autoclosed task by 9
        Record(closed_at=b'2020-01-25', closed_by=3),
        Record(closed_at=b'2020-01-25', closed_by=61),
        Record(closed_at=b'2020-01-25 17:17', closed_by=7)
    ])

    assert sorted(resolved_tasks) == sorted([
        Record(resolved_at=b'2020-01-25', resolved_by=2),
        Record(resolved_at=b'2020-01-25', resolved_by=4),
        Record(resolved_at=b'2020-01-25', resolved_by=8),
        Record(resolved_at=b'2020-01-25', resolved_by=9),
        Record(resolved_at=b'2020-01-25', resolved_by=5),
        Record(resolved_at=b'2020-01-25', resolved_by=62),
        Record(resolved_at=b'2020-01-25 17:07', resolved_by=7)
    ])


def test_should_remove_self_created_closed_tasks():
    job = clusters.MockCluster().job()

    result = []

    closed_tasks = [
        Record(closed_by=1, created_by=1),
        Record(closed_by=2, created_by=20),
        Record(closed_by=3, created_by=3)
    ]

    make_moderation_log_job(
        job, '2020-01-25',
        RESULT_PATH, TASK_ACTIVE_PATH, TASK_CLOSED_PATH, COMMIT_EVENT_PATH
    ).local_run(
        sources={'closed tasks': local.StreamSource(closed_tasks)},
        sinks={'closed tasks without self created': local.ListSink(result)}
    )

    assert result == [
        Record(closed_by=2),
    ]


def test_should_remove_self_created_resolved_tasks():
    job = clusters.MockCluster().job()

    result = []

    resolved_tasks = [
        Record(resolved_by=1, created_by=1),
        Record(resolved_by=2, created_by=20),
        Record(resolved_by=3, created_by=3)
    ]

    make_moderation_log_job(
        job, '2020-01-25',
        RESULT_PATH, TASK_ACTIVE_PATH, TASK_CLOSED_PATH, COMMIT_EVENT_PATH
    ).local_run(
        sources={'resolved tasks': local.StreamSource(resolved_tasks)},
        sinks={'resolved tasks without self created': local.ListSink(result)}
    )

    assert result == [
        Record(resolved_by=2),
    ]


def test_should_remove_autoclosed_dependent_tasks():
    job = clusters.MockCluster().job()

    result = []

    closed_tasks_without_self_created_tasks = [
        Record(closed_at=b'2020-01-25 01:01:01', closed_by=1, primary_object_category_id=b'cat 1'),
        Record(closed_at=b'2020-01-25 02:02:02', closed_by=1, primary_object_category_id=b'cat 1'),  # another closed_at
        Record(closed_at=b'2020-01-25 01:01:01', closed_by=2, primary_object_category_id=b'cat 1'),  # another closed_by
        Record(closed_at=b'2020-01-25 01:01:01', closed_by=1, primary_object_category_id=b'cat 2'),  # another category
        Record(closed_at=b'2020-01-25 01:01:01', closed_by=1, primary_object_category_id=b'cat 1'),  # same as first
    ]

    make_moderation_log_job(
        job, '2020-01-25',
        RESULT_PATH, TASK_ACTIVE_PATH, TASK_CLOSED_PATH, COMMIT_EVENT_PATH
    ).local_run(
        sources={'closed tasks without self created': local.StreamSource(closed_tasks_without_self_created_tasks)},
        sinks={'closed tasks without autoclosed': local.ListSink(result)}
    )

    assert sorted(result) == sorted([
        Record(closed_at=b'2020-01-25 01:01:01', closed_by=1, primary_object_category_id=b'cat 1'),
        Record(closed_at=b'2020-01-25 02:02:02', closed_by=1, primary_object_category_id=b'cat 1'),
        Record(closed_at=b'2020-01-25 01:01:01', closed_by=2, primary_object_category_id=b'cat 1'),
        Record(closed_at=b'2020-01-25 01:01:01', closed_by=1, primary_object_category_id=b'cat 2'),
    ])


def test_should_remove_autoresolved_dependent_tasks():
    job = clusters.MockCluster().job()

    result = []

    resolved_tasks_without_self_created = [
        Record(resolved_at=b'2020-01-25 01:01:01', resolved_by=1, primary_object_category_id=b'cat 1'),
        Record(resolved_at=b'2020-01-25 02:02:02', resolved_by=1, primary_object_category_id=b'cat 1'),  # another resolved_at
        Record(resolved_at=b'2020-01-25 01:01:01', resolved_by=2, primary_object_category_id=b'cat 1'),  # another resolved_by
        Record(resolved_at=b'2020-01-25 01:01:01', resolved_by=1, primary_object_category_id=b'cat 2'),  # another category
        Record(resolved_at=b'2020-01-25 01:01:01', resolved_by=1, primary_object_category_id=b'cat 1'),  # same as first
    ]

    make_moderation_log_job(
        job, '2020-01-25',
        RESULT_PATH, TASK_ACTIVE_PATH, TASK_CLOSED_PATH, COMMIT_EVENT_PATH
    ).local_run(
        sources={'resolved tasks without self created': local.StreamSource(resolved_tasks_without_self_created)},
        sinks={'resolved tasks without autoresolved': local.ListSink(result)}
    )

    assert sorted(result) == sorted([
        Record(resolved_at=b'2020-01-25 01:01:01', resolved_by=1, primary_object_category_id=b'cat 1'),
        Record(resolved_at=b'2020-01-25 02:02:02', resolved_by=1, primary_object_category_id=b'cat 1'),
        Record(resolved_at=b'2020-01-25 01:01:01', resolved_by=2, primary_object_category_id=b'cat 1'),
        Record(resolved_at=b'2020-01-25 01:01:01', resolved_by=1, primary_object_category_id=b'cat 2'),
    ])


def test_should_convert_closed_tasks_to_result_format():
    job = clusters.MockCluster().job()

    result = []

    closed_tasks_without_autoclosed = [
        Record(closed_at=b'2020-01-25', closed_by=1, primary_object_category_id=b'cat 1', bounds_geom=b'geom 1', lat_min=11, lon_min=12, lat_max=13, lon_max=14),
        Record(closed_at=b'2020-01-25', closed_by=2, primary_object_category_id=None,     bounds_geom=b'geom 2', lat_min=21, lon_min=22, lat_max=23, lon_max=24)
    ]

    make_moderation_log_job(
        job, '2020-01-25',
        RESULT_PATH, TASK_ACTIVE_PATH, TASK_CLOSED_PATH, COMMIT_EVENT_PATH
    ).local_run(
        sources={'closed tasks without autoclosed': local.StreamSource(closed_tasks_without_autoclosed)},
        sinks={'result closed tasks': local.ListSink(result)}
    )

    assert result == [
        Record(iso_datetime=b'2020-01-25T00:00:00+03:00', puid=1, task_id=b'moderation/closed/cat 1',  quantity=1.0, geom=b'geom 1', lat_min=11, lon_min=12, lat_max=13, lon_max=14),
        Record(iso_datetime=b'2020-01-25T00:00:00+03:00', puid=2, task_id=b'moderation/closed/common', quantity=1.0, geom=b'geom 2', lat_min=21, lon_min=22, lat_max=23, lon_max=24)
    ]


def test_should_convert_resolved_tasks_to_result_format():
    job = clusters.MockCluster().job()

    result = []

    resolved_tasks_without_autoresolved = [
        Record(resolved_at=b'2020-01-25', resolved_by=1, primary_object_category_id=b'cat 1', bounds_geom=b'geom 1', lat_min=11, lon_min=12, lat_max=13, lon_max=14),
        Record(resolved_at=b'2020-01-25', resolved_by=2, primary_object_category_id=None,     bounds_geom=b'geom 2', lat_min=21, lon_min=22, lat_max=23, lon_max=24)
    ]

    make_moderation_log_job(
        job, '2020-01-25',
        RESULT_PATH, TASK_ACTIVE_PATH, TASK_CLOSED_PATH, COMMIT_EVENT_PATH
    ).local_run(
        sources={'resolved tasks without autoresolved': local.StreamSource(resolved_tasks_without_autoresolved)},
        sinks={'result resolved tasks': local.ListSink(result)}
    )

    assert result == [
        Record(iso_datetime=b'2020-01-25T00:00:00+03:00', puid=1, task_id=b'moderation/resolved/cat 1',  quantity=1.0, geom=b'geom 1', lat_min=11, lon_min=12, lat_max=13, lon_max=14),
        Record(iso_datetime=b'2020-01-25T00:00:00+03:00', puid=2, task_id=b'moderation/resolved/common', quantity=1.0, geom=b'geom 2', lat_min=21, lon_min=22, lat_max=23, lon_max=24)
    ]


def test_should_convert_input_tables_to_result(monkeypatch):
    'Checks that the whole job works as it is expected. (an integration test)'

    monkeypatch.setattr(
        'maps.wikimap.stat.tasks_payment.tasks_logging.moderation.lib.moderation_log.mercator_ewkb_geometry_to_geo_bbox',
        geometry_to_bbox_mock
    )

    job = clusters.MockCluster().job()

    result = []

    social_task_active = [
        # Resolved, but not closed
        Record(event_id=1, closed_at=None, closed_by=0, resolved_at=b'2020-01-25', resolved_by=1, primary_object_category_id=b'cat 1'),

        # Autoresolved (simultaneously with event_id = 1)
        Record(event_id=11, closed_at=None, closed_by=0, resolved_at=b'2020-01-25', resolved_by=1, primary_object_category_id=b'cat 1')
    ]
    social_task_closed = [
        # Another date
        Record(event_id=2, closed_at=b'2020-01-24', closed_by=2, resolved_at=b'2020-01-24', resolved_by=2, primary_object_category_id=b'cat 2'),

        # Closed at another date
        Record(event_id=31, closed_at=b'2020-01-26', closed_by=31, resolved_at=b'2020-01-25', resolved_by=31, primary_object_category_id=b'cat 31'),

        # Resolved at another date
        Record(event_id=32, closed_at=b'2020-01-25', closed_by=32, resolved_at=b'2020-01-24', resolved_by=32, primary_object_category_id=b'cat 32'),

        # Autoclosed (simultaneously with event_id = 32)
        Record(event_id=33, closed_at=b'2020-01-25', closed_by=32, resolved_at=b'2020-01-24', resolved_by=32, primary_object_category_id=b'cat 32'),

        # closed_at != resolved_at and closed_by != resolved_by
        Record(event_id=4, closed_at=b'2020-01-25 14:14', closed_by=42, resolved_at=b'2020-01-25 04:04', resolved_by=41, primary_object_category_id=b'cat 4'),

        # closed_at != resolved_at and closed_by == resolved_by
        Record(event_id=5, closed_at=b'2020-01-25 15:15', closed_by=5, resolved_at=b'2020-01-25 05:05', resolved_by=5, primary_object_category_id=b'cat 5'),

        # closed_at == resolved_at and closed_by != resolved_by
        Record(event_id=6, closed_at=b'2020-01-25', closed_by=62, resolved_at=b'2020-01-25', resolved_by=61, primary_object_category_id=b'cat 6'),

        # closed_at == resolved_at and closed_by == resolved_by (autoclosed)
        Record(event_id=7, closed_at=b'2020-01-25', closed_by=7, resolved_at=b'2020-01-25', resolved_by=7, primary_object_category_id=b'cat 7'),

        # No category
        Record(event_id=8, closed_at=b'2020-01-25', closed_by=82, resolved_at=b'2020-01-25', resolved_by=81, primary_object_category_id=None),

        # Resolved by author (self-created)
        Record(event_id=9, closed_at=b'2020-01-25', closed_by=9, resolved_at=b'2020-01-25', resolved_by=90, primary_object_category_id=b'cat 9'),

        # Closed by author (self-created)
        Record(event_id=10, closed_at=b'2020-01-25', closed_by=100, resolved_at=b'2020-01-25', resolved_by=10, primary_object_category_id=b'cat 10')
    ]

    commit_event = [
        Record(event_id=1, bounds_geom=b'11, 12, 13, 14', created_by=10),
        Record(event_id=2, bounds_geom=b'21, 22, 23, 24', created_by=20),
        Record(event_id=31, bounds_geom=b'311, 312, 313, 314', created_by=30),
        Record(event_id=32, bounds_geom=b'321, 322, 323, 324', created_by=30),
        Record(event_id=4, bounds_geom=b'41, 42, 43, 44', created_by=40),
        Record(event_id=5, bounds_geom=b'51, 52, 53, 54', created_by=50),
        Record(event_id=6, bounds_geom=b'61, 62, 63, 64', created_by=60),
        Record(event_id=7, bounds_geom=b'71, 72, 73, 74', created_by=70),
        Record(event_id=8, bounds_geom=b'81, 82, 83, 84', created_by=80),
        Record(event_id=9, bounds_geom=b'91, 92, 93, 94', created_by=90),
        Record(event_id=10, bounds_geom=b'101, 102, 103, 104', created_by=100)
    ]

    make_moderation_log_job(
        job, '2020-01-25',
        RESULT_PATH, TASK_ACTIVE_PATH, TASK_CLOSED_PATH, COMMIT_EVENT_PATH
    ).local_run(
        sources={
            'social.task_active': local.StreamSource(social_task_active),
            'social.task_closed': local.StreamSource(social_task_closed),
            'social.commit_event': local.StreamSource(commit_event)
        },
        sinks={'result': local.ListSink(result)}
    )

    assert sorted(result) == sorted([
        Record(iso_datetime=b'2020-01-25T00:00:00+03:00', puid=1, task_id=b'moderation/resolved/cat 1', quantity=1.0,
               geom=b'11, 12, 13, 14', lat_min=11.0, lon_min=12.0, lat_max=13.0, lon_max=14.0),
        Record(iso_datetime=b'2020-01-25T00:00:00+03:00', puid=31, task_id=b'moderation/resolved/cat 31', quantity=1.0,
               geom=b'311, 312, 313, 314', lat_min=311.0, lon_min=312.0, lat_max=313.0, lon_max=314.0),
        Record(iso_datetime=b'2020-01-25T00:00:00+03:00', puid=32, task_id=b'moderation/closed/cat 32', quantity=1.0,
               geom=b'321, 322, 323, 324', lat_min=321.0, lon_min=322.0, lat_max=323.0, lon_max=324.0),
        Record(iso_datetime=b'2020-01-25T04:04:00+03:00', puid=41, task_id=b'moderation/resolved/cat 4', quantity=1.0,
               geom=b'41, 42, 43, 44', lat_min=41.0, lon_min=42.0, lat_max=43.0, lon_max=44.0),
        Record(iso_datetime=b'2020-01-25T14:14:00+03:00', puid=42, task_id=b'moderation/closed/cat 4', quantity=1.0,
               geom=b'41, 42, 43, 44', lat_min=41.0, lon_min=42.0, lat_max=43.0, lon_max=44.0),
        Record(iso_datetime=b'2020-01-25T05:05:00+03:00', puid=5, task_id=b'moderation/resolved/cat 5', quantity=1.0,
               geom=b'51, 52, 53, 54', lat_min=51.0, lon_min=52.0, lat_max=53.0, lon_max=54.0),
        Record(iso_datetime=b'2020-01-25T15:15:00+03:00', puid=5, task_id=b'moderation/closed/cat 5', quantity=1.0,
               geom=b'51, 52, 53, 54', lat_min=51.0, lon_min=52.0, lat_max=53.0, lon_max=54.0),
        Record(iso_datetime=b'2020-01-25T00:00:00+03:00', puid=61, task_id=b'moderation/resolved/cat 6', quantity=1.0,
               geom=b'61, 62, 63, 64', lat_min=61.0, lon_min=62.0, lat_max=63.0, lon_max=64.0),
        Record(iso_datetime=b'2020-01-25T00:00:00+03:00', puid=62, task_id=b'moderation/closed/cat 6', quantity=1.0,
               geom=b'61, 62, 63, 64', lat_min=61.0, lon_min=62.0, lat_max=63.0, lon_max=64.0),
        Record(iso_datetime=b'2020-01-25T00:00:00+03:00', puid=7, task_id=b'moderation/resolved/cat 7', quantity=1.0,
               geom=b'71, 72, 73, 74', lat_min=71.0, lon_min=72.0, lat_max=73.0, lon_max=74.0),
        Record(iso_datetime=b'2020-01-25T00:00:00+03:00', puid=81, task_id=b'moderation/resolved/common', quantity=1.0,
               geom=b'81, 82, 83, 84', lat_min=81.0, lon_min=82.0, lat_max=83.0, lon_max=84.0),
        Record(iso_datetime=b'2020-01-25T00:00:00+03:00', puid=82, task_id=b'moderation/closed/common', quantity=1.0,
               geom=b'81, 82, 83, 84', lat_min=81.0, lon_min=82.0, lat_max=83.0, lon_max=84.0),
        Record(iso_datetime=b'2020-01-25T00:00:00+03:00', puid=9, task_id=b'moderation/closed/cat 9', quantity=1.0,
               geom=b'91, 92, 93, 94', lat_min=91.0, lon_min=92.0, lat_max=93.0, lon_max=94.0),
        Record(iso_datetime=b'2020-01-25T00:00:00+03:00', puid=10, task_id=b'moderation/resolved/cat 10', quantity=1.0,
               geom=b'101, 102, 103, 104', lat_min=101.0, lon_min=102.0, lat_max=103.0, lon_max=104.0),
        Record(iso_datetime=b'2020-01-25T00:00:00+03:00', puid=82282794, task_id=b'moderation/closed/cat 7', quantity=1.0,
               geom=b'71, 72, 73, 74', lat_min=71.0, lon_min=72.0, lat_max=73.0, lon_max=74.0)
    ])
