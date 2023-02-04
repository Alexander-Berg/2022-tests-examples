import datetime
import json
import os
from http import HTTPStatus

import pytest

from yql.api.v1.client import YqlClient

from maps.b2bgeo.ya_courier.backend.test_lib.conftest import skip_if_remote
from maps.b2bgeo.ya_courier.backend.test_lib.util_offline import local_get, local_post, local_delete
from maps.b2bgeo.ya_courier.backend.test_lib.util import source_path
from ya_courier_backend.config.unit_test import YT_COURIER_POSITION_TABLE_PATH
from ya_courier_backend.models import db, RestoringTracksTask, CourierPositionRestored, RecoveredRoute
from ya_courier_backend.models.restoring_tracks_task import RestoringTracksTaskStatus
from ya_courier_backend.tasks.restore_tracks import RestoreTracksTask


def _add_recovered_position(env, route_id, timestamp):
    data = {
        'route_id': route_id,
        'courier_id': env.default_courier.id,
        'time': timestamp,
        'lat': 58.82,
        'lon': 37.73,
        'accuracy': 1,
        'server_time': 1631618252.0
    }
    with env.flask_app.app_context():
        position = CourierPositionRestored(**data)
        db.session.add(position)
        db.session.commit()


def _check_run(expected_count_of_tasks, finished_task):
    recovered_route = db.session.query(RecoveredRoute.route_id).all()
    finished_tasks = db.session.query(RestoringTracksTask.id) \
        .filter(RestoringTracksTask.status == RestoringTracksTaskStatus.finished) \
        .all()
    assert len(finished_tasks) == expected_count_of_tasks
    assert (finished_task.id,) in finished_tasks
    assert (finished_task.route_id,) in recovered_route


@pytest.fixture(scope='module')
def yt_with_data(yt):
    data_root = source_path("maps/b2bgeo/ya_courier/backend")
    table = open(os.path.join(data_root, "bin/tests/data/yql/courier_positions.yson"), "rb")
    schema = json.load(open(os.path.join(data_root, "bin/tests/data/yql/courier_positions_schema.json"), "r"))

    yt = yt.yt_wrapper
    yt.create("table", YT_COURIER_POSITION_TABLE_PATH, recursive=True)

    yt.write_table(
        yt.TablePath(YT_COURIER_POSITION_TABLE_PATH, attributes={"schema": schema}),
        table,
        format="yson",
        raw=True
    )
    return yt


@skip_if_remote
def test_task_run(env, yql_api, yt_with_data):

    yql_client = YqlClient(
        server="localhost",
        port=yql_api.port,
        db="plato"
    )

    update_task_period_s = 2
    periodic_task = RestoreTracksTask(env.flask_app, yql_client, update_task_period_s)
    with env.flask_app.app_context():
        failed_task = RestoringTracksTask.create_task(env.default_route.id)
        failed_task.status = RestoringTracksTaskStatus.fail
        db.session.add(failed_task)

        finished_task = RestoringTracksTask.create_task(env.default_route.id)
        finished_task.status = RestoringTracksTaskStatus.finished
        db.session.add(finished_task)

        db.session.commit()

        not_updated_task = RestoringTracksTask.create_task(env.default_route.id)
        not_updated_task.status = RestoringTracksTaskStatus.in_progress
        db.session.add(not_updated_task)
        db.session.flush()
        not_updated_task.updated_at -= datetime.timedelta(seconds=2 * update_task_period_s)
        db.session.commit()

        task_in_progress = RestoringTracksTask.create_task(env.default_route.id)
        task_in_progress.status = RestoringTracksTaskStatus.in_progress
        db.session.add(task_in_progress)
        db.session.flush()
        task_in_progress.updated_at += datetime.timedelta(days=1)
        db.session.commit()

        task_in_queue = RestoringTracksTask.create_task(env.default_route.id)
        db.session.add(task_in_queue)
        db.session.commit()

        periodic_task.run('')
        _check_run(2, not_updated_task)

        periodic_task.run('')
        _check_run(3, task_in_queue)

        restored_positions = db.session.query(CourierPositionRestored).all()
        assert len(restored_positions) == 1


@skip_if_remote
def test_task_result(env, yql_api, yt_with_data):
    yql_client = YqlClient(
        server="localhost",
        port=yql_api.port,
        db="plato"
    )

    update_task_period_s = 2
    periodic_task = RestoreTracksTask(env.flask_app, yql_client, update_task_period_s)

    with env.flask_app.app_context():
        task = RestoringTracksTask.create_task(env.default_route.id)
        db.session.add(task)
        db.session.flush()
        db.session.expunge(task)
        db.session.commit()

    path = f"/api/v1/companies/{env.default_company.id}/track_recovery/{task.id}"
    resp = local_get(env.client, path, headers=env.superuser_auth_headers)

    assert resp['id'] == task.id
    assert resp['status'] == RestoringTracksTaskStatus.queued.name
    assert 'points_recovered' not in resp

    with env.flask_app.app_context():
        periodic_task.run('')

    resp = local_get(env.client, path, headers=env.superuser_auth_headers)
    assert resp['id'] == task.id
    assert resp['status'] == RestoringTracksTaskStatus.finished.name

    with env.flask_app.app_context():
        positions = db.session.query(CourierPositionRestored) \
            .filter(CourierPositionRestored.route_id == env.default_route.id) \
            .all()
        assert len(positions) == resp['points_recovered']


@skip_if_remote
def test_task_add(env):
    path = f"/api/v1/companies/{env.default_company.id}/track_recovery?route_id={env.default_route.id}"
    resp = env.client.open(path,
                           headers=env.superuser_auth_headers,
                           method='POST',
                           content_type='application/json')

    assert resp.status_code == HTTPStatus.ACCEPTED
    assert resp.json["status"] == RestoringTracksTaskStatus.queued.name
    with env.flask_app.app_context():
        assert (resp.json['id'],) in db.session.query(RestoringTracksTask.id).all()

    assert resp.json['id'] in resp.headers['Location']

    # Check the url link is correct
    local_get(env.client, resp.headers['Location'], headers=env.superuser_auth_headers)


@skip_if_remote
def test_task_delete(env, yql_api, yt_with_data):
    yql_client = YqlClient(
        server="localhost",
        port=yql_api.port,
        db="plato"
    )

    update_task_period_s = 2
    periodic_task = RestoreTracksTask(env.flask_app, yql_client, update_task_period_s)

    path_add = f"/api/v1/companies/{env.default_company.id}/track_recovery?route_id={env.default_route.id}"
    resp = local_post(env.client, path_add, headers=env.superuser_auth_headers, expected_status=HTTPStatus.ACCEPTED)

    with env.flask_app.app_context():
        periodic_task.run('')

    path_result = f"/api/v1/companies/{env.default_company.id}/track_recovery/{resp['id']}"
    resp = local_get(env.client, path_result, headers=env.superuser_auth_headers)
    points_recovered = resp['points_recovered']

    path_delete = f"/api/v1/companies/{env.default_company.id}/routes/{env.default_route.id}/recovered_positions"
    resp = local_delete(env.client, path_delete, headers=env.superuser_auth_headers)

    with env.flask_app.app_context():
        positions = db.session.query(CourierPositionRestored) \
            .filter(CourierPositionRestored.route_id == env.default_route.id) \
            .all()
        assert len(positions) == 0

    assert points_recovered == resp['points_removed']
