from datetime import datetime, timedelta, time
from http import HTTPStatus

import dateutil.tz
import pytest
from freezegun import freeze_time

from maps.b2bgeo.ya_courier.backend.test_lib.conftest import skip_if_remote
from maps.b2bgeo.ya_courier.backend.test_lib.util_offline import Environment, local_patch, local_post, local_get
from maps.b2bgeo.ya_courier.backend.test_lib.util_db import set_company_import_depot_garage, set_company_enabled_sms_types, \
    set_company_sms_time_window, update_route
from maps.b2bgeo.ya_courier.backend.test_lib.util import prepare_push_positions_data
from ya_courier_backend.models import db, EtaType, TrackingToken, NotificationType, Order, Garage, Sms, SmsType
from ya_courier_backend.models.logistic_company import CompanySmsTypes, SmsTimeWindow
from maps.b2bgeo.libs.py_sqlalchemy_utils.iso_datatime import get_isoformat_str
from ya_courier_backend.util.order_time_windows import convert_time_offset_to_date
from ya_courier_backend.util.sms import send_start_sms, send_arrival_sms
from ya_courier_backend.util.tracking import create_tracking_token

# Monday, August 31, 2020 10:00:00 AM GMT+03:00
TEST_TIMEZONE = 'Europe/Moscow'
TEST_DATETIME = datetime.fromtimestamp(1598857200).replace(tzinfo=dateutil.tz.gettz(TEST_TIMEZONE))

SMS_TIME_WINDOW_IN_TEST_DATETIME = \
    SmsTimeWindow(start=(TEST_DATETIME - timedelta(hours=1)).time().replace(tzinfo=None),
                  end=(TEST_DATETIME + timedelta(hours=10)).time().replace(tzinfo=None))

SMS_TIME_WINDOW_AFTER_TEST_DATETIME = \
    SmsTimeWindow(start=(TEST_DATETIME + timedelta(minutes=15)).time().replace(tzinfo=None),
                  end=(TEST_DATETIME + timedelta(hours=10)).time().replace(tzinfo=None))

ORDER_LOCATIONS = [
    {"lat": 55.733827, "lon": 37.588722},
    {"lat": 55.729299, "lon": 37.580116}
]
GARAGE_LOCATION = {
    "lat": 55.664695,
    "lon": 37.562443
}
ORDER_LOCATION = {
    "lat": 55.8185462,
    "lon": 37.66126693
}
DEPOT_LOCATION = {
    "lat": 55.799087,
    "lon": 37.729377
}
TIME_INTERVALS = ["15:00-18:00", "20:00-23:00"]
ETAS = [EtaType.arrival_time.value, EtaType.delivery_time.value]
COMPANY_ENABLED_SMS_TYPES_TO_SMS_TYPES = {
    CompanySmsTypes.shift_start: [SmsType.shift_start],
    CompanySmsTypes.arrival: [SmsType.approaching, SmsType.nearby],
}


def _get_orders_data(route_id):
    return [{
        "address": "Leo Tolstoy Str, 16",
        "number": 'test_order' + "_{}".format(i),
        "lat": loc["lat"],
        "lon": loc["lon"],
        "route_id": route_id,
        "service_duration_s": 300,
        "time_interval": time_interval,
        "eta_type": eta,
    } for i, (loc, time_interval, eta) in enumerate(zip(ORDER_LOCATIONS, TIME_INTERVALS, ETAS))]


def _get_route_data(courier_id, depot_id):
    return {
        "courier_id": courier_id,
        "date": TEST_DATETIME.date().isoformat(),
        "depot_id": depot_id,
        "number": "test_route",
    }


@skip_if_remote
def test_tracking_with_eta_delivery_time(env: Environment):
    # create route
    route = local_post(env.client,
                       f'/api/v1/companies/{env.default_company.id}/routes',
                       headers=env.user_auth_headers,
                       data=_get_route_data(env.default_courier.id, env.default_depot.id))

    # create orders:
    # 1) eta_type = arrival_time  , time_window = 15:00-18:00
    # 2) eta_type = delivery_time , time_window = 20:00-23:00
    orders = []
    for order in _get_orders_data(route['id']):
        orders.append(local_post(env.client,
                                 f'/api/v1/companies/{env.default_company.id}/orders',
                                 headers=env.user_auth_headers,
                                 data=order))

    # update etas and get routed_orders
    routed_orders = local_get(env.client,
                              f'/api/v1/couriers/{env.default_courier.id}/routes/{route["id"]}/routed-orders',
                              query={
                                  'lat': env.default_depot.lat,
                                  'lon': env.default_depot.lon,
                                  'timestamp': TEST_DATETIME.timestamp()
                              },
                              headers=env.user_auth_headers)

    # create tracks for orders
    tracks = []
    for order in orders:
        tracks.append(local_post(env.client,
                                 f'/api/v1/couriers/{env.default_courier.id}/routes/{route["id"]}/create-track',
                                 query={'order_id': order['id']},
                                 headers=env.user_auth_headers))

    # check first order eta_iso
    tracking = local_get(env.client,
                         f'/api/v1/tracking/{tracks[0]["track_id"]}/track',
                         headers=env.user_auth_headers)
    # tracking['eta_iso'] = '2020-08-31T10:17:58.784397+03:00'
    assert routed_orders['route'][0]['waiting_duration_s']
    assert tracking['eta_iso'] == convert_time_offset_to_date(routed_orders['route'][0]['arrival_time_s'],
                                                              TEST_DATETIME.date(),
                                                              TEST_TIMEZONE).isoformat()

    # check second order eta_iso
    tracking = local_get(env.client,
                         f'/api/v1/tracking/{tracks[1]["track_id"]}/track',
                         headers=env.user_auth_headers)
    # tracking['eta_iso'] = '2020-08-31T20:00:00+03:00'
    assert routed_orders['route'][1]['waiting_duration_s']
    assert tracking['eta_iso'] == convert_time_offset_to_date(routed_orders['route'][1]['arrival_time_s'] +
                                                              routed_orders['route'][1]['waiting_duration_s'],
                                                              TEST_DATETIME.date(),
                                                              TEST_TIMEZONE).isoformat()


@skip_if_remote
def test_tracking_tokens_are_created_after_first_depot_departure_only(env: Environment):
    set_company_import_depot_garage(env, env.default_company.id, True)

    task_id = "mock_task_uuid__ongoing_route"
    import_path = f"/api/v1/companies/{env.default_company.id}/mvrp_task?task_id={task_id}"
    [route] = local_post(env.client, import_path, headers=env.user_auth_headers)

    path_route_info = f"/api/v1/companies/{env.default_company.id}/route-info?route_id={route['id']}"
    [route_info] = local_get(env.client, path_route_info, headers=env.user_auth_headers)

    arrival_time = route_info['nodes'][1]['value']['time_windows'][0]['start']['value']
    path_push = f"/api/v1/couriers/{env.default_courier.id}/routes/{route['id']}/push-positions"
    locations = [(55.82, 37.63, arrival_time)]
    local_post(env.client, path_push, headers=env.user_auth_headers, data=prepare_push_positions_data(locations))

    route_state_context = {'lat': 55.82, 'lon': 37.63, 'timestamp': arrival_time + 1800}

    # Assert that no notifications are added while we are going to depot
    update_route(env, route['id'], route_state_context)

    with env.flask_app.app_context():
        assert not db.session.query(TrackingToken).all()

    locations = [(55.7447, 37.6727, arrival_time + 600)]
    local_post(env.client, path_push, headers=env.user_auth_headers, data=prepare_push_positions_data(locations))
    locations = [(55.7447, 37.6727, arrival_time + 1200)]
    local_post(env.client, path_push, headers=env.user_auth_headers, data=prepare_push_positions_data(locations))

    # Assert that no notifications are added while we are not departed from depot
    update_route(env, route['id'], route_state_context)

    with env.flask_app.app_context():
        assert not db.session.query(TrackingToken).all()

    locations = [(55.82, 37.63, arrival_time + 1800)]
    local_post(env.client, path_push, headers=env.user_auth_headers, data=prepare_push_positions_data(locations))

    # Assert that notifications are added after first depot departure
    update_route(env, route['id'], route_state_context)

    expected_tokens = [
        {"number": "126", "company_id": env.default_shared_company.id, "notification_type": NotificationType.shift_start},
        {"number": "126", "company_id": env.default_company.id, "notification_type": NotificationType.shift_start},
        {"number": "78", "company_id": env.default_company.id, "notification_type": NotificationType.shift_start},
        {"number": "126", "company_id": env.default_shared_company.id, "notification_type": NotificationType.approaching},
        {"number": "126", "company_id": env.default_company.id, "notification_type": NotificationType.approaching},
    ]

    with env.flask_app.app_context():
        query = (
            db.session.query(TrackingToken.company_id, TrackingToken.notification_type, Order.number)
            .join(Order, Order.id == TrackingToken.order_id)
            .order_by(TrackingToken.id)
        )
        assert [row._asdict() for row in query.all()] == expected_tokens


@skip_if_remote
@pytest.mark.parametrize("enabled_sms_types", [[], [CompanySmsTypes.arrival], [CompanySmsTypes.shift_start],
                                               [CompanySmsTypes.shift_start, CompanySmsTypes.arrival]])
def test_sms_types_restrictions(env: Environment, enabled_sms_types):
    set_company_import_depot_garage(env, env.default_company.id, True)
    set_company_enabled_sms_types(env, env.default_company.id, enabled_sms_types)

    task_id = "mock_task_uuid__ongoing_route"
    import_path = f"/api/v1/companies/{env.default_company.id}/mvrp_task?task_id={task_id}"
    [route] = local_post(env.client, import_path, headers=env.user_auth_headers)

    path_route_info = f"/api/v1/companies/{env.default_company.id}/route-info?route_id={route['id']}"
    [route_info] = local_get(env.client, path_route_info, headers=env.user_auth_headers)

    arrival_time = route_info['nodes'][1]['value']['time_windows'][0]['start']['value']
    path_push = f"/api/v1/couriers/{env.default_courier.id}/routes/{route['id']}/push-positions"
    locations = [(55.82, 37.63, arrival_time)]
    local_post(env.client, path_push, headers=env.user_auth_headers, data=prepare_push_positions_data(locations))

    route_state_context = {'lat': 55.82, 'lon': 37.63, 'timestamp': arrival_time + 1800}

    # Assert that no sms are sent while we are going to depot
    update_route(env, route['id'], route_state_context)

    with env.flask_app.app_context():
        assert not db.session.query(Sms).all()

    locations = [(55.7447, 37.6727, arrival_time + 600)]
    local_post(env.client, path_push, headers=env.user_auth_headers, data=prepare_push_positions_data(locations))
    locations = [(55.7447, 37.6727, arrival_time + 1200)]
    local_post(env.client, path_push, headers=env.user_auth_headers, data=prepare_push_positions_data(locations))

    # Assert that no sms are sent while we are not departed from depot
    update_route(env, route['id'], route_state_context)

    with env.flask_app.app_context():
        assert not db.session.query(Sms).all()

    locations = [(55.82, 37.63, arrival_time + 1800)]
    local_post(env.client, path_push, headers=env.user_auth_headers, data=prepare_push_positions_data(locations))

    # Assert that sms are sent after first depot departure
    update_route(env, route['id'], route_state_context)

    expected_sms = list(filter(lambda x: any([x["type"] in
                                              COMPANY_ENABLED_SMS_TYPES_TO_SMS_TYPES[enabled_sms_type]
                                              for enabled_sms_type in enabled_sms_types]), [
        {"number": "126", "type": SmsType.shift_start},
        {"number": "126", "type": SmsType.approaching},
        {"number": "78", "type": SmsType.shift_start},
    ]))

    with env.flask_app.app_context():
        query = (
            db.session.query(Sms.type, Order.number)
            .join(Order, Order.id == Sms.order_id)
            .order_by(Order.number)
        )
        assert [row._asdict() for row in query.all()] == expected_sms


@skip_if_remote
@pytest.mark.parametrize("sms_time_window", [None,
                                             SMS_TIME_WINDOW_IN_TEST_DATETIME,
                                             SMS_TIME_WINDOW_AFTER_TEST_DATETIME])
def test_sms_time_windows(env: Environment, sms_time_window):
    set_company_import_depot_garage(env, env.default_company.id, True)
    sms_time_window_start = sms_time_window.start if sms_time_window else time(hour=0, minute=0, second=0)
    set_company_sms_time_window(env, env.default_company.id, sms_time_window)

    with freeze_time(TEST_DATETIME) as freezed_time:
        task_id = "mock_task_uuid__sms_time_window_test"
        import_path = f"/api/v1/companies/{env.default_company.id}/mvrp_task?task_id={task_id}"
        [route] = local_post(env.client, import_path, headers=env.user_auth_headers)

        path_route_info = f"/api/v1/companies/{env.default_company.id}/route-info?route_id={route['id']}"
        [route_info] = local_get(env.client, path_route_info, headers=env.user_auth_headers)

        arrival_time = route_info['nodes'][1]['value']['time_windows'][0]['start']['value']
        path_push = f"/api/v1/couriers/{env.default_courier.id}/routes/{route['id']}/push-positions"
        locations = [(55.82, 37.63, arrival_time)]
        local_post(env.client, path_push, headers=env.user_auth_headers, data=prepare_push_positions_data(locations))

        route_state_context = {'lat': 55.82, 'lon': 37.63, 'timestamp': arrival_time + 1800}

        # Assert that no sms are sent while we are going to depot
        update_route(env, route['id'], route_state_context)

        with env.flask_app.app_context():
            assert not db.session.query(Sms).all()

        locations = [(55.7447, 37.6727, arrival_time + 600)]
        local_post(env.client, path_push, headers=env.user_auth_headers, data=prepare_push_positions_data(locations))
        locations = [(55.7447, 37.6727, arrival_time + 1200)]
        local_post(env.client, path_push, headers=env.user_auth_headers, data=prepare_push_positions_data(locations))

        # Assert that no sms are sent while we are not departed from depot
        update_route(env, route['id'], route_state_context)

        with env.flask_app.app_context():
            assert not db.session.query(Sms).all()

        locations = [(55.82, 37.63, arrival_time + 1800)]
        local_post(env.client, path_push, headers=env.user_auth_headers, data=prepare_push_positions_data(locations))

        # Assert that sms are sent after first depot departure
        update_route(env, route['id'], route_state_context)

        expected_sms = [
            {"number": "126", "type": SmsType.shift_start},
            {"number": "126", "type": SmsType.approaching},
            {"number": "78", "type": SmsType.shift_start},
        ] if sms_time_window_start <= datetime.now(dateutil.tz.gettz(TEST_TIMEZONE)).time().replace(tzinfo=None) else []

        with env.flask_app.app_context():
            query = (
                db.session.query(Sms.type, Order.number)
                .join(Order, Order.id == Sms.order_id)
                .order_by(Order.number)
            )
            assert [row._asdict() for row in query.all()] == expected_sms

        freezed_time.tick(delta=timedelta(minutes=15))
        update_route(env, route['id'], route_state_context)

        expected_sms = expected_sms or [{"number": "126", "type": SmsType.approaching}]

        with env.flask_app.app_context():
            query = (
                db.session.query(Sms.type, Order.number)
                .join(Order, Order.id == Sms.order_id)
                .order_by(Order.number)
            )
            assert [row._asdict() for row in query.all()] == expected_sms


@skip_if_remote
def test_positions_processing_garage_visit_detection(env: Environment):
    set_company_import_depot_garage(env, env.default_company.id, True)

    task_id = "mock_task_uuid__second_reduced_with_garage_last"
    import_path = f"/api/v1/companies/{env.default_company.id}/mvrp_task?task_id={task_id}"
    [route] = local_post(env.client, import_path, headers=env.user_auth_headers)

    path_route_info = f"/api/v1/companies/{env.default_company.id}/route-info?route_id={route['id']}"
    [route_info] = local_get(env.client, path_route_info, headers=env.user_auth_headers)

    pos_time = route_info['nodes'][1]['value']['time_windows'][0]['start']['value']
    path_push = f"/api/v1/couriers/{env.default_courier.id}/routes/{route['id']}/push-positions"

    # visit last garage location, garage must not be marked as visited
    locations = [(GARAGE_LOCATION["lat"], GARAGE_LOCATION["lon"], pos_time)]
    local_post(env.client, path_push, headers=env.user_auth_headers, data=prepare_push_positions_data(locations))

    # visit first depot
    locations = [(DEPOT_LOCATION["lat"], DEPOT_LOCATION["lon"], pos_time + 1800)]
    local_post(env.client, path_push, headers=env.user_auth_headers, data=prepare_push_positions_data(locations))

    # visit order
    locations = [(ORDER_LOCATION["lat"], ORDER_LOCATION["lon"], pos_time + 3600)]
    local_post(env.client, path_push, headers=env.user_auth_headers, data=prepare_push_positions_data(locations))

    # visit last garage
    garage_last_pos_time = pos_time + 5400
    locations = [(GARAGE_LOCATION["lat"], GARAGE_LOCATION["lon"], garage_last_pos_time)]
    local_post(env.client, path_push, headers=env.user_auth_headers, data=prepare_push_positions_data(locations))

    [route_info] = local_get(env.client, path_route_info, headers=env.user_auth_headers)

    with env.flask_app.app_context():
        garages = Garage.get(slave_session=False)
        assert len(garages) == 1, garages.as_dict()
        assert garages[0].history[1]["event"] == "VISIT", garages[0].as_dict()
        assert get_isoformat_str(garage_last_pos_time, dateutil.tz.gettz(route_info["meta"]["planning_area"]["time_zone"])) == garages[0].history[1]["position"]["time"]


@skip_if_remote
def test_position_source_unistat_signal(env: Environment):
    task_id = "mock_task_uuid__ongoing_route"
    import_path = f"/api/v1/companies/{env.default_company.id}/mvrp_task?task_id={task_id}"
    [route] = local_post(env.client, import_path, headers=env.user_auth_headers)

    path_route = f"/api/v1/companies/{env.default_company.id}/routes/{route['id']}"
    imei = 1234567890
    local_patch(env.client, path_route, headers=env.user_auth_headers, data={'imei': imei})

    path_route_info = f"/api/v1/companies/{env.default_company.id}/route-info?route_id={route['id']}"
    [route_info] = local_get(env.client, path_route_info, headers=env.user_auth_headers)
    pos_time = route_info['nodes'][1]['value']['time_windows'][0]['start']['value']

    # push position without source
    path_push = f"/api/v1/couriers/{env.default_courier.id}/routes/{route['id']}/push-positions"
    locations = [(55.82, 37.63, pos_time), (55.81, 37.63, pos_time + 10)]
    local_post(env.client, path_push, headers=env.user_auth_headers, data=prepare_push_positions_data(locations))

    # push position with source=navi-ch
    path_push = f"/api/v1/couriers/{env.default_courier.id}/routes/{route['id']}/push-positions?source=navi-ch"
    locations = [(55.80, 37.63, pos_time + 20), (55.79, 37.63, pos_time + 30), (55.78, 37.63, pos_time + 40)]
    local_post(env.client, path_push, headers=env.user_auth_headers, data=prepare_push_positions_data(locations))

    # push position from gps-tracker
    path_push = f"/api/v1/gps-trackers/{imei}/push-positions"
    locations = [(55.77, 37.63, datetime.fromtimestamp(pos_time + 50).replace(tzinfo=dateutil.tz.gettz(TEST_TIMEZONE)).isoformat())]
    local_post(env.client, path_push, headers=env.superuser_auth_headers, data=prepare_push_positions_data(locations))

    # check unistat signals
    path_unistat = "/api/v1/unistat"
    signals = local_get(env.client, path_unistat, headers=env.user_auth_headers)
    signals = {signal[0]: signal[1] for signal in signals}
    assert signals['pushed_positions_courier_app_summ'] == 2
    assert signals['pushed_positions_gps_tracker_summ'] == 1
    assert signals['pushed_positions_navi_ch_summ'] == 3


@skip_if_remote
def test_422_on_negative_time(env: Environment):
    task_id = "mock_task_uuid__ongoing_route"
    import_path = f"/api/v1/companies/{env.default_company.id}/mvrp_task?task_id={task_id}"
    [route] = local_post(env.client, import_path, headers=env.user_auth_headers)

    # push-positions-v2
    path_push_v2 = f"/api/v1/couriers/{env.default_courier.id}/routes/{route['id']}/push-positions-v2"
    data_v2 = {
        'positions': [
            {
                'coords': {
                    'accuracy': 10,
                    'latitude': 55.79,
                    'longitude': 37.63
                },
                'timestampMeta': {
                    "systemTime": -100
                }
            }
        ]
    }
    local_post(env.client,
               path_push_v2,
               headers=env.user_auth_headers,
               data=data_v2,
               expected_status=HTTPStatus.UNPROCESSABLE_ENTITY)

    # push-positions-v3
    path_push_v3 = f"/api/v1/couriers/{env.default_courier.id}/routes/{route['id']}/push-positions-v3"
    data_v3 = {
        "positions": [
            {
                "speed": 26,
                "heading": 15,
                "accuracy": 15,
                "point": {"lat": 52.20373, "lon": 20.257364},
                "timestamp": -62135600400
            }
        ]
    }
    local_post(env.client,
               path_push_v3,
               headers=env.user_auth_headers,
               data=data_v3,
               expected_status=HTTPStatus.UNPROCESSABLE_ENTITY)


@skip_if_remote
@pytest.mark.parametrize('locale', ['ru_RU', 'es_MX', 'tr_TR'])
def test_sms_localization(env: Environment, locale):
    set_company_import_depot_garage(env, env.default_company.id, True)
    set_company_enabled_sms_types(env, env.default_company.id, [CompanySmsTypes.shift_start, CompanySmsTypes.arrival])

    task_id = 'mock_task_uuid__ongoing_route'
    import_path = f'/api/v1/companies/{env.default_company.id}/mvrp_task?task_id={task_id}'
    [route] = local_post(env.client, import_path, headers=env.user_auth_headers)

    with env.flask_app.app_context():
        order = Order.get(slave_session=False, order_number='126', first=True)
        company_path = f'/api/v1/companies/{env.default_company.id}'

        # create english notiftications
        local_patch(
            env.client,
            company_path,
            data={'locale': 'en_US'},
            headers=env.user_auth_headers)
        tracking_token_en = create_tracking_token(None, order.id, env.default_company.id, None)
        send_start_sms(tracking_token_en, order)
        send_arrival_sms(tracking_token_en, order.id, 100, SmsType.nearby, env.default_courier, env.default_company)

        # create translated notifications
        local_patch(
            env.client,
            company_path,
            data={'locale': locale},
            headers=env.user_auth_headers)
        tracking_token_loc = create_tracking_token(None, order.id, env.default_company.id, None)
        send_start_sms(tracking_token_loc, order)
        send_arrival_sms(tracking_token_loc, order.id, 100, SmsType.nearby, env.default_courier, env.default_company)

        all_sms = db.session.query(Sms).order_by(Sms.id).all()
        assert len(all_sms) == 4
        for sms in all_sms:
            assert sms.text

        # check that english and translated texts differ
        sms_start_en, sms_arrival_en, sms_start_loc, sms_arrival_loc = all_sms
        assert tracking_token_en.tracking_url != tracking_token_loc.tracking_url
        assert sms_start_en.text != sms_start_loc.text
        assert sms_arrival_en.text != sms_arrival_loc.text
