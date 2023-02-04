import time
from datetime import datetime, timedelta

from dateutil.tz import gettz
from freezegun import freeze_time

from maps.b2bgeo.ya_courier.backend.test_lib.conftest import skip_if_remote
from maps.b2bgeo.ya_courier.backend.test_lib.util_offline import local_post, set_default_route_time_interval
from maps.b2bgeo.ya_courier.backend.test_lib.util import prepare_push_positions_data
from ya_courier_backend.models import db, RouteEvent
from ya_courier_backend.tasks.check_courier_connection_loss import CheckCourierConnectionLossTask


def _add_position(env, timestamp):
    path_push = f"/api/v1/couriers/{env.default_courier.id}/routes/{env.default_route.id}/push-positions"
    locations = [(58.82, 37.73, timestamp)]
    local_post(env.client, path_push, headers=env.user_auth_headers, data=prepare_push_positions_data(locations))


def _get_route_events(env, route_id, courier_id):
    with env.flask_app.app_context():
        return db.session.query(RouteEvent)\
            .filter(RouteEvent.route_id == route_id)\
            .filter(RouteEvent.courier_id == courier_id).all()


@skip_if_remote
def test_courier_reconnection(env):
    route = set_default_route_time_interval(env)

    start_datetime = (datetime.combine(route.date, datetime.min.time()) + timedelta(seconds=route.route_start_s))\
        .astimezone(gettz(env.default_depot.time_zone))

    task = CheckCourierConnectionLossTask(env.flask_app)

    with freeze_time(start_datetime) as freezed_time:
        first_push_pos_server_time = time.time()
        _add_position(env, time.time())
        freezed_time.tick(delta=timedelta(seconds=env.default_company.courier_connection_loss_s + 1))
        with env.flask_app.app_context():
            task.run('')
            second_push_pos_server_time = time.time()
            _add_position(env, time.time())
            task.run('')

            route_events = _get_route_events(env, route.id, env.default_courier.id)
            assert len(route_events) == 1
            assert route_events[0].start_timestamp == first_push_pos_server_time
            assert route_events[0].finish_timestamp == second_push_pos_server_time


@skip_if_remote
def test_courier_stable_connection(env):
    route = set_default_route_time_interval(env)

    start_datetime_s = route.route_finish_s - env.default_company.courier_connection_loss_s
    start_datetime = (datetime.combine(route.date, datetime.min.time()) + timedelta(seconds=start_datetime_s)) \
        .astimezone(gettz(env.default_depot.time_zone))

    task = CheckCourierConnectionLossTask(env.flask_app)

    with freeze_time(start_datetime) as freezed_time:
        _add_position(env, time.time())
        freezed_time.tick(delta=timedelta(seconds=env.default_company.courier_connection_loss_s / 2))
        with env.flask_app.app_context():
            task.run('')
            _add_position(env, time.time())
            freezed_time.tick(delta=timedelta(seconds=env.default_company.courier_connection_loss_s + 1))
            task.run('')

            route_events = _get_route_events(env, route.id, env.default_courier.id)
            assert len(route_events) == 0
