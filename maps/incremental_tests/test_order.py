import pytest
import requests
import json
import datetime
import random
import sys
import dateutil
from time import sleep

from maps.b2bgeo.ya_courier.backend.test_lib.config import PAGINATION_PAGE_SIZE
from maps.b2bgeo.ya_courier.backend.test_lib.util import (
    entity_equal, env_post_request, env_get_request,
    env_delete_request, env_patch_request, api_path_with_company_id,
    create_company_entity, create_route_env, get_courier_quality,
    create_tmp_company, create_tmp_user, get_order_details,
    get_order_sequence, get_order, find_company_by_id,
    post_order_sequence, create_tmp_users, push_positions,
    get_position_shifted_east, create_route_envs, sms_state,
    patch_order, patch_company
)
from maps.b2bgeo.ya_courier.backend.test_lib.util_sharing import UserKind
from maps.b2bgeo.ya_courier.backend.test_lib.conftest import skip_if_remote, skip_if_local
from ya_courier_backend.models import UserRole, OrderHistoryEvent, OrderStatus, SmsType
from ya_courier_backend.models.order import (
    has_order_history_event, get_order_history_event_records
)
from maps.b2bgeo.ya_courier.backend.test_lib.env.ya_courier import DEFAULT_UPDATE_ROUTE_STATES_PERIOD_S, DEFAULT_UPDATE_ROUTE_STATES_DELAY_S

ID = None
COURIER_ID = None
ROUTE_ID = None
ROUTE_NUMBER = '23423-2000'
COURIER_NUMBER = None
ROUTE_DATE = None
ROUTE_HISTORY_LEN = 0
ENTITY = {
    'number': '20170513-{}'.format(str(random.randint(100, 1000))),
    'customer_number': '224',
    'route_number': ROUTE_NUMBER,
    'address': 'ул. Льва Толстого, 16',
    'amount': 1,
    'comments': 'слева от лошади, бц Морозов',
    'payment_type': 'cash',
    'volume': 1.0,
    'weight': 2.1,
    'description': 'ipad3',
    'phone': '+79161111111',
    'time_interval': '11:00 - 23:00',
    'lat': 55.7447,
    'lon': 37.6728,
    'customer_name': 'Игорь',
    'status': 'new',
    'mark_delivered_radius': 100
}

NEW_ENTITY = {
    'number': ENTITY['number'],
    'customer_number': '224',
    'route_number': ROUTE_NUMBER,
    'address': 'ул. Льва Толстого, 17',
    'amount': 2,
    'comments': 'справа от лошади, бц Морозов',
    'payment_type': 'card',
    'volume': 2.0,
    'weight': 3.1,
    'description': 'ipad4',
    'phone': None,
    'time_interval': '12:00 - 23:00',
    'service_duration_s': 600,
    'lat': 55.7448,
    'lon': 37.6729,
    'status': 'finished',
    'customer_name': 'Ольга'
}

NEW_ENTITY_PATCH = {k: v for k, v in NEW_ENTITY.items() if not k.endswith('_number')}

ENTITY2 = {
    'number': '234234',
    'customer_number': '224',
    'route_number': ROUTE_NUMBER,
    'address': 'ул. Льва Толстого, 15',
    'amount': 3,
    'comments': 'за лошадью, бц Морозов',
    'payment_type': 'card',
    'volume': 1.0,
    'weight': 1.1,
    'description': 'ipad2',
    'time_interval': '10-23',
    'service_duration_s': 700,
    'lat': 55.7449,
    'lon': 37.6730,
    'customer_name': 'Олег',
    'status': 'confirmed'
}

ENTITY_NO_TIME_INTERVAL = {
    'number': '234234177',
    'customer_number': '224',
    'route_number': ROUTE_NUMBER,
    'address': 'ул. Льва Толстого, 15',
    'amount': 3,
    'comments': 'за лошадью, бц Морозов',
    'payment_type': 'card',
    'volume': 1.0,
    'weight': 1.1,
    'description': 'ipad2',
    'phone': '+79161111111',
    'service_duration_s': 700,
    'lat': 55.7449,
    'lon': 37.6730,
    'customer_name': 'Олег',
    'status': 'confirmed'
}

ENTITY_ISO_TIME_INTERVAL = {
    'number': '2342341',
    'customer_number': '224',
    'route_number': ROUTE_NUMBER,
    'address': 'ул. Льва Толстого, 15',
    'amount': 3,
    'comments': 'за лошадью, бц Морозов',
    'payment_type': 'card',
    'volume': 1.0,
    'weight': 1.1,
    'description': 'ipad2',
    'phone': '+79161111111',
    'time_interval': '2020-10-30T10:00:00+03:00/2020-10-30T20:00:00+03:00',
    'service_duration_s': 700,
    'lat': 55.7449,
    'lon': 37.6730,
    'customer_name': 'Олег',
    'status': 'confirmed'
}

ENTITY_MULTIPLE_TIME_INTERVALS = {
    'number': '234235',
    'customer_number': '224',
    'route_number': ROUTE_NUMBER,
    'address': 'ул. Льва Толстого, 15',
    'amount': 3,
    'comments': 'за лошадью, бц Морозов',
    'payment_type': 'card',
    'volume': 1.0,
    'weight': 1.1,
    'description': 'ipad2',
    'phone': '+79161111111',
    'time_interval': '10-23, 12-22',
    'service_duration_s': 700,
    'lat': 55.7449,
    'lon': 37.6730,
    'customer_name': 'Олег',
    'status': 'confirmed'
}

ENTITY_BAD_ORDER_VALUES = {
    "address": 123,
    "amount": 'one',
    "comments": [1, 2, 3],
    "customer_id": 'id',
    "customer_name": {"first_name": "test"},
    "customer_number": 123,
    "lat": None,
    "lon": "zero",
    "mark_delivered_radius": 600,
    "number": 112233,
    "payment_type": 0,
    "phone": 100500,
    "route_id": 'route-number',
    "service_duration_s": None,
    "shared_service_duration_s": [],
    "shared_with_company_id": [],
    "status": 0,
    "time_interval": -1,
    "volume": 'one',
    "weight": {},
    "extra_field": "extra_data"
}

ROUTE = {
    'number': ROUTE_NUMBER,
    'courier_number': 'TEST_ORDER_COURIER',
    'depot_number': '23453',
    'date': '2017-07-22',
    'imei': 12345678901234567,
}

COURIER = {
    'number': 'TEST_ORDER_COURIER',
    'phone': '+71234553425',
    'name': 'Олеся'
}

DEPOT = {
    'number': '23453',
    'name': 'Склад 2',
    'address': 'ул. Льва Толстого, 18',
    'time_interval': '0.00:00-23:59',
    'lat': 55.7446,
    'lon': 37.6727,
    'description': 'курьерский вход',
    'service_duration_s': 500,
    'order_service_duration_s': 9
}

EXTERNAL_FK = ['customer_number', 'route_number']
INTERNAL_FK = ['route_id']


class TestOrder(object):

    def _delete_order_by_id(self, system_env_with_db, order_id):
        return env_delete_request(
            system_env_with_db,
            api_path_with_company_id(system_env_with_db, "orders/{}".format(order_id))
        )

    def _delete_order_by_number(self, system_env_with_db, order_number, strict=True):
        response = env_get_request(
            system_env_with_db,
            api_path_with_company_id(system_env_with_db, "orders?number={}".format(order_number))
        )
        assert response.status_code == requests.codes.ok
        j = response.json()

        assert self._delete_order_by_id(system_env_with_db, j[0]['id']).status_code == requests.codes.ok if len(j) == 1 else not strict

    def test_setup(self, system_env_with_db):
        create_company_entity(system_env_with_db, "couriers", COURIER)
        create_company_entity(system_env_with_db, "depots", DEPOT)
        create_company_entity(system_env_with_db, "routes", ROUTE)
        self._delete_order_by_number(system_env_with_db, ENTITY['number'], strict=False)

    def test_create_order_schema(self, system_env_with_db):
        def try_create_order(data):
            r = env_post_request(
                system_env_with_db,
                path=api_path_with_company_id(system_env_with_db, "orders"),
                data=data
            )
            assert r.status_code == requests.codes.unprocessable
            return r.json()['message']

        for field, bad_value in ENTITY_BAD_ORDER_VALUES.items():
            data = ENTITY.copy()
            data[field] = bad_value
            assert 'schema validation failed' in try_create_order(data)

        data = ENTITY.copy()
        data.pop('address')
        assert "'address' is a required property" in try_create_order(data)

    @pytest.mark.parametrize('use_batch', [False, True])
    def test_create_order_without_route(self, system_env_with_db, use_batch):
        data = ENTITY.copy()
        del data['route_number']
        assert 'route_id' not in data

        assert env_post_request(
            system_env_with_db,
            path=api_path_with_company_id(system_env_with_db, "orders-batch" if use_batch else "orders"),
            data=[data] if use_batch else data
        ).status_code == requests.codes.unprocessable

    @pytest.mark.parametrize('use_batch', [False, True])
    def test_create_order_with_route_number(self, system_env_with_db, use_batch):
        assert 'route_id' not in ENTITY
        assert 'route_number' in ENTITY

        response = env_post_request(
            system_env_with_db,
            path=api_path_with_company_id(system_env_with_db, "orders-batch" if use_batch else "orders"),
            data=[ENTITY] if use_batch else ENTITY
        )
        assert response.status_code == requests.codes.ok
        j = response.json()

        if not use_batch:
            global ROUTE_ID
            ROUTE_ID = j['route_id']

        self._delete_order_by_number(system_env_with_db, ENTITY['number'])

    @pytest.mark.parametrize('use_batch', [False, True])
    def test_create_order_with_route_id(self, system_env_with_db, use_batch):
        data = ENTITY.copy()
        del data['route_number']
        data['route_id'] = ROUTE_ID

        assert env_post_request(
            system_env_with_db,
            path=api_path_with_company_id(system_env_with_db, "orders-batch" if use_batch else "orders"),
            data=[data] if use_batch else data
        ).status_code == requests.codes.ok

        self._delete_order_by_number(system_env_with_db, data['number'])

    @pytest.mark.parametrize('use_batch', [False, True])
    def test_create_order_without_time_interval(self, system_env_with_db, use_batch):
        data = ENTITY.copy()
        del data['time_interval']

        assert env_post_request(
            system_env_with_db,
            path=api_path_with_company_id(system_env_with_db, "orders-batch" if use_batch else "orders"),
            data=[data] if use_batch else data
        ).status_code == requests.codes.unprocessable

    @pytest.mark.parametrize('use_batch', [False, True])
    def test_create_order_with_invalid_time_interval(self, system_env_with_db, use_batch):
        data = ENTITY.copy()
        data['time_interval'] = '10'

        assert env_post_request(
            system_env_with_db,
            path=api_path_with_company_id(system_env_with_db, "orders-batch" if use_batch else "orders"),
            data=[data] if use_batch else data
        ).status_code == requests.codes.unprocessable

    @pytest.mark.parametrize('use_batch', [False, True])
    def test_create_order_with_unknown_fields(self, system_env_with_db, use_batch):
        data = ENTITY.copy()
        data['unknown_field'] = 123

        assert env_post_request(
            system_env_with_db,
            path=api_path_with_company_id(system_env_with_db, "orders-batch" if use_batch else "orders"),
            data=[data] if use_batch else data
        ).status_code == requests.codes.unprocessable

    def test_wrong_batch(self, system_env_with_db):
        response = env_post_request(
            system_env_with_db,
            path=api_path_with_company_id(system_env_with_db, "orders-batch"),
            data=ENTITY
        )
        j = response.json()
        print(json.dumps(j))

        assert response.status_code == requests.codes.unprocessable

    def test_wrong_batch_wrong_field_type(self, system_env_with_db):
        data = ENTITY.copy()
        data['route_number'] = 112233
        response = env_post_request(
            system_env_with_db,
            path=api_path_with_company_id(system_env_with_db, "orders-batch"),
            data=[data]
        )
        j = response.json()
        print(json.dumps(j))

        assert response.status_code == requests.codes.unprocessable

    def test_batch_with_additional_known_property(self, system_env_with_db):
        data = ENTITY.copy()
        data['status_log'] = 'Some message'
        response = env_post_request(
            system_env_with_db,
            path=api_path_with_company_id(system_env_with_db, "orders-batch"),
            data=[data]
        )
        j = response.json()
        print(json.dumps(j))

        assert response.status_code == requests.codes.ok

    def test_batch(self, system_env_with_db):
        response = env_post_request(
            system_env_with_db,
            path=api_path_with_company_id(system_env_with_db, "orders-batch"),
            data=[ENTITY, ENTITY2],
        )
        j = response.json()
        print(json.dumps(j))
        assert response.status_code == requests.codes.ok
        assert j['inserted'] + j['updated'] == 2

    def test_batch_multiple_time_intervals(self, system_env_with_db):
        response = env_post_request(
            system_env_with_db,
            path=api_path_with_company_id(system_env_with_db, "orders-batch"),
            data=[ENTITY, ENTITY_MULTIPLE_TIME_INTERVALS],
        )
        j = response.json()
        print(json.dumps(j))
        assert response.status_code == requests.codes.unprocessable

    def test_post_multiple_time_intervals(self, system_env_with_db):
        response = env_post_request(
            system_env_with_db,
            path=api_path_with_company_id(system_env_with_db, "orders"),
            data=ENTITY_MULTIPLE_TIME_INTERVALS
        )
        j = response.json()
        print(json.dumps(j))
        assert response.status_code == requests.codes.unprocessable

    def test_batch_update(self, system_env_with_db):
        ENTITY['time_interval'] = ENTITY2['time_interval'] = '01:00 - 23:00'

        response = env_post_request(
            system_env_with_db,
            path=api_path_with_company_id(system_env_with_db, "orders-batch"),
            data=[ENTITY, ENTITY2],
        )
        j = response.json()
        print(json.dumps(j))
        assert response.status_code == requests.codes.ok
        assert j['inserted'] == 0
        assert j['updated'] == 2

    def test_batch_no_time_interval(self, system_env_with_db):
        response = env_post_request(
            system_env_with_db,
            path=api_path_with_company_id(system_env_with_db, "orders-batch"),
            data=[ENTITY_NO_TIME_INTERVAL],
        )
        j = response.json()
        print(json.dumps(j))
        assert response.status_code == requests.codes.unprocessable

    def test_batch_iso_time_interval(self, system_env_with_db):
        response = env_post_request(
            system_env_with_db,
            path=api_path_with_company_id(system_env_with_db, "orders-batch"),
            data=[ENTITY_ISO_TIME_INTERVAL],
        )
        j = response.json()
        print(json.dumps(j))
        assert response.status_code == requests.codes.ok

    def test_batch_duplicate_items(self, system_env_with_db):
        response = env_post_request(
            system_env_with_db,
            path=api_path_with_company_id(system_env_with_db, "orders-batch"),
            data=[ENTITY, ENTITY],
        )
        assert response.status_code == requests.codes.unprocessable

    def test_list(self, system_env_with_db):
        response = env_get_request(
            system_env_with_db,
            api_path_with_company_id(system_env_with_db, "orders")
        )
        j = response.json()
        print(json.dumps(j))

        assert response.status_code == requests.codes.ok
        assert PAGINATION_PAGE_SIZE >= len(j) > 1

    def test_pagination_page_0(self, system_env_with_db):
        response = env_get_request(
            system_env_with_db,
            api_path_with_company_id(system_env_with_db, "orders?page=0")
        )
        j = response.json()
        print(json.dumps(j))

        assert response.status_code == requests.codes.unprocessable
        assert 'Must be greater than or equal to 1' in response.content.decode()

    def test_pagination_page_1(self, system_env_with_db):
        response = env_get_request(
            system_env_with_db,
            api_path_with_company_id(system_env_with_db, "orders?page=1")
        )
        j = response.json()
        print(json.dumps(j))

        assert response.status_code == requests.codes.ok
        assert PAGINATION_PAGE_SIZE >= len(j) > 1

    def test_pagination_page_1m(self, system_env_with_db):
        response = env_get_request(
            system_env_with_db,
            api_path_with_company_id(system_env_with_db, "orders?page=1000000")
        )
        j = response.json()
        print(json.dumps(j))

        assert response.status_code == requests.codes.ok
        assert len(j) == 0

    def test_list_by_non_existing_number(self, system_env_with_db):
        response = env_get_request(
            system_env_with_db,
            api_path_with_company_id(system_env_with_db, "orders?number=non-existing-number")
        )
        j = response.json()

        assert response.status_code == requests.codes.ok, f"error {response.status_code}: {response.text}"
        assert len(j) == 0

    def test_list_by_number(self, system_env_with_db):
        path = "orders?number={}".format(ENTITY['number'])
        response = env_get_request(
            system_env_with_db,
            api_path_with_company_id(system_env_with_db, path)
        )
        j = response.json()
        print(json.dumps(j))

        assert response.status_code == requests.codes.ok
        assert len(j) == 1
        assert entity_equal(j[0], ENTITY)

        global ID
        ID = j[0]['id']

        for key in INTERNAL_FK:
            ENTITY[key] = j[0][key]

        for key in EXTERNAL_FK:
            del ENTITY[key]

    def test_list_by_non_existing_route(self, system_env_with_db):
        path = "orders?route_id=90909090"
        assert env_get_request(
            system_env_with_db,
            api_path_with_company_id(system_env_with_db, path)
        ).status_code == requests.codes.not_found

    def test_patch_multiple_time_intervals(self, system_env_with_db):
        path = "orders/{}".format(ID)
        response = env_patch_request(
            system_env_with_db,
            path=api_path_with_company_id(system_env_with_db, path),
            data={'time_interval': '10-23, 12-22'}
        )
        j = response.json()
        print(json.dumps(j))
        assert response.status_code == requests.codes.unprocessable

    def test_get(self, system_env_with_db):
        path = "orders/{}".format(ID)
        response = env_get_request(
            system_env_with_db,
            api_path_with_company_id(system_env_with_db, path)
        )
        j = response.json()
        print(json.dumps(j))
        assert response.status_code == requests.codes.ok
        assert entity_equal(j, ENTITY)
        assert j['route_id'] == ROUTE_ID

        response = env_get_request(
            system_env_with_db,
            api_path_with_company_id(
                system_env_with_db,
                "{}/{}".format('routes', ROUTE_ID)
            )
        )
        j = response.json()
        print(json.dumps(j))
        assert response.status_code == requests.codes.ok

        global COURIER_ID, ROUTE_DATE
        COURIER_ID = j['courier_id']
        ROUTE_DATE = j['date']

    def test_verification(self, system_env_with_db):
        response = env_get_request(
            system_env_with_db,
            api_path_with_company_id(
                system_env_with_db,
                "verification?date={}".format(ROUTE_DATE)
            )
        )
        j = response.json()
        print(json.dumps(j))

        assert response.status_code == requests.codes.ok
        assert len([item for item in j if item['order_number'] in [ENTITY['number'], ENTITY2['number']]]) == 2

    def test_get_by_courier_and_route_wo_location(self, system_env_with_db):
        response = env_get_request(
            system_env_with_db,
            'couriers/{}/routes/{}/routed-orders'.format(
                COURIER_ID, ROUTE_ID
            ),
        )

        j = response.json()
        print(json.dumps(j))

        assert response.status_code == requests.codes.unprocessable

    def test_get_courier_number(self, system_env_with_db):
        response = env_get_request(
            system_env_with_db,
            api_path_with_company_id(
                system_env_with_db,
                "couriers/{}".format(COURIER_ID)
            )
        )

        j = response.json()

        global COURIER_NUMBER
        COURIER_NUMBER = j['number']

    # route-history + 1
    def test_get_by_courier_and_route_w_location(self, system_env_with_db):
        response = env_get_request(
            system_env_with_db,
            'couriers/{}/routes/{}/routed-orders?lat={}&lon={}&time_now={}'.format(
                COURIER_ID, ROUTE_ID, 55.685771, 37.459491, '15:00'
            )
        )
        j = response.json()
        print(json.dumps(j))

        assert response.status_code == requests.codes.ok

        order = [order for order in j['route'] if order['id'] == ID][0]
        assert entity_equal(order, ENTITY)

    # route-history + 1(?)
    def test_patch_confirmed(self, system_env_with_db):
        path = "orders/{}".format(ID)
        response = env_patch_request(
            system_env_with_db,
            path=api_path_with_company_id(system_env_with_db, path),
            data={'status': 'confirmed'}
        )
        j = response.json()
        print(json.dumps(j))

        global ENTITY
        ENTITY['status'] = 'confirmed'

        assert response.status_code == requests.codes.ok
        assert entity_equal(j, ENTITY)
        # assert j.get('confirmed_at') is not None

    @skip_if_remote
    def test_route_history(self, system_env_with_db):
        response = env_get_request(
            system_env_with_db,
            '{}/route-history?courier_number={}&date={}'.format(
                api_path_with_company_id(system_env_with_db),
                COURIER_NUMBER,
                ROUTE_DATE
            )
        )
        j = response.json()
        print(j, COURIER_NUMBER, ROUTE_DATE)
        print(j[-1])

        global ROUTE_HISTORY_LEN, ID
        ROUTE_HISTORY_LEN = len(j)

        assert response.status_code == requests.codes.ok
        assert ID in j[-1]['next_orders']

    @skip_if_remote
    def test_route_history_fail(self, system_env_with_db):
        response = env_get_request(
            system_env_with_db,
            '{}/route-history?courier_number={}'.format(
                api_path_with_company_id(system_env_with_db),
                COURIER_NUMBER
            )
        )
        assert response.status_code == requests.codes.unprocessable

        response = env_get_request(
            system_env_with_db,
            '{}/route-history?date={}'.format(
                api_path_with_company_id(system_env_with_db),
                ROUTE_DATE
            )
        )
        assert response.status_code == requests.codes.unprocessable

    @skip_if_remote
    def test_route_history_fail_2(self, system_env_with_db):
        response = env_get_request(
            system_env_with_db,
            '{}/route-history?courier_number={}&date=16.06.2020'.format(
                api_path_with_company_id(system_env_with_db),
                COURIER_NUMBER
            )
        )
        assert response.status_code == requests.codes.unprocessable

    # route-history + 1
    def test_get_by_courier_and_route_with_location_after_confirmed(self, system_env_with_db):
        response = env_get_request(
            system_env_with_db,
            'couriers/{}/routes/{}/routed-orders?lat={}&lon={}&time_now={}'.format(
                COURIER_ID, ROUTE_ID, 55.685772, 37.459492, '15:00'
            )
        )
        j = response.json()
        print(json.dumps(j))

        assert response.status_code == requests.codes.ok

        order = [order for order in j['route'] if order['id'] == ID][0]
        assert entity_equal(order, ENTITY)

    def test_get_by_courier_and_route_with_wrong_location(self, system_env_with_db):
        response = env_get_request(
            system_env_with_db,
            'couriers/{}/routes/{}/routed-orders?lat={}&lon={}&time_s={}'.format(
                COURIER_ID, ROUTE_ID, 55.685773, 37.459493, '15:00'
            )
        )
        j = response.json()
        print(json.dumps(j))

        assert response.status_code == requests.codes.unprocessable

    def test_get_by_courier_and_route_with_overrides(self, system_env_with_db):
        response = env_get_request(
            system_env_with_db,
            ('couriers/{}/routes/{}/routed-orders?lat={}&lon={}&time_now={}&override_order_id={}' +
             '&override_time_interval={}').format(
                COURIER_ID, ROUTE_ID, 55.685774, 37.459494, '15:00', ID, '09-23'
            )
        )
        j = response.json()
        print(json.dumps(j))
        assert response.status_code == requests.codes.ok

        order = [order for order in j['route'] if order['id'] == ID][0]
        assert order['status'] == 'confirmed'

        overrided_entity = ENTITY.copy()
        overrided_entity['time_interval'] = '09:00 - 23:00'
        assert entity_equal(order, overrided_entity)

    def test_get_by_courier_and_route_with_overrides_status_cancelled(self, system_env_with_db):
        response = env_get_request(
            system_env_with_db,
            (
                'couriers/{}/routes/{}/routed-orders?lat={}&lon={}&time_now={}&override_order_id={}' +
                '&override_time_interval={}&override_status={}').format(
                COURIER_ID, ROUTE_ID, 55.685775, 37.459495, '15:00', ID, '09-23', 'cancelled'
            )
        )
        j = response.json()
        print(json.dumps(j))

        assert response.status_code == requests.codes.ok

        order = [order for order in j['route'] if order['id'] == ID][0]
        assert order['status'] == 'cancelled'

        overrided_entity = ENTITY.copy()
        overrided_entity['time_interval'] = '09:00 - 23:00'
        overrided_entity['status'] = 'cancelled'
        assert entity_equal(order, overrided_entity)

    def test_get_by_courier_and_route_with_wrong_overrides(self, system_env_with_db):
        response = env_get_request(
            system_env_with_db,
            (
                'couriers/{}/routes/{}/routed-orders?lat={}&lon={}&time_now={}&override_order_id={}' +
                '&override_time_interval={}').format(
                COURIER_ID, ROUTE_ID, 55.685776, 37.459496, '15:00', ID, '09-21-22'
            )
        )
        j = response.json()
        print(json.dumps(j))

        assert response.status_code == requests.codes.unprocessable

    def test_get_by_courier_and_route_with_wrong_override_status(self, system_env_with_db):
        response = env_get_request(
            system_env_with_db,
            (
                'couriers/{}/routes/{}/routed-orders?lat={}&lon={}&time_now={}&override_order_id={}' +
                '&override_time_interval={}&override_status={}').format(
                COURIER_ID, ROUTE_ID, 55.685777, 37.459497, '15:00', ID, '09-23', 'old'
            )
        )
        j = response.json()
        print(json.dumps(j))

        assert response.status_code == requests.codes.unprocessable

    def test_patch_order_schema(self, system_env_with_db):
        def try_patch_order(data):
            response = env_patch_request(
                system_env_with_db,
                path=api_path_with_company_id(system_env_with_db, f"orders/{ID}"),
                data=data
            )
            assert response.status_code == requests.codes.unprocessable
            return response.json()['message']

        for field, bad_value in ENTITY_BAD_ORDER_VALUES.items():
            data = NEW_ENTITY_PATCH.copy()
            data[field] = bad_value
            assert 'schema validation failed' in try_patch_order(data)

    def test_patch(self, system_env_with_db):
        path = "orders/{}".format(ID)
        response = env_patch_request(
            system_env_with_db,
            path=api_path_with_company_id(system_env_with_db, path),
            data=NEW_ENTITY_PATCH
        )
        j = response.json()

        if response.status_code != requests.codes.ok:
            print('test_patch: {}'.format(json.dumps(j)), file=sys.stderr)
            sys.stderr.flush()

        assert response.status_code == requests.codes.ok
        assert entity_equal(j, NEW_ENTITY_PATCH)
        assert j.get('delivered_at') is not None

    # route-history + 1
    def test_get_by_courier_and_route_with_location_after_delivery(self, system_env_with_db):
        response = env_get_request(
            system_env_with_db,
            'couriers/{}/routes/{}/routed-orders?lat={}&lon={}&time_now={}'.format(
                COURIER_ID, ROUTE_ID, 55.685778, 37.459498, '15:00'
            )
        )
        j = response.json()
        print(json.dumps(j))

        assert response.status_code == requests.codes.ok
        assert len([order for order in j['route'] if order['id'] == ID]) == 1

    @skip_if_remote
    def test_route_history2(self, system_env_with_db):
        response = env_get_request(
            system_env_with_db,
            '{}/route-history?courier_number={}&date={}'.format(
                api_path_with_company_id(system_env_with_db),
                COURIER_NUMBER,
                ROUTE_DATE
            )
        )
        j = response.json()
        print(j[-1])

        assert response.status_code == requests.codes.ok
        assert len(j) >= ROUTE_HISTORY_LEN + 2  # routed-orders called twice + possible periodic-thread update
        assert ID in j[-1]['finished_orders']
        assert ID not in j[-1]['next_orders']

    def test_patch_order_in_route_schema(self, system_env_with_db):
        # this test checks that order.status_log field, which is present in the response, is also accepted in a PATCH
        # request.
        order = get_order(system_env_with_db, ID)
        current_status = order['status']
        new_status = 'new' if current_status == 'confirmed' else 'confirmed'
        data = {k: v for k, v in order.items() if not k.endswith('_number')}
        data['status'] = new_status

        response = env_patch_request(
            system_env_with_db, f'couriers/{COURIER_ID}/routes/{ROUTE_ID}/orders/{ID}', data=data)

        assert response.ok, response.text
        assert get_order(system_env_with_db, ID)['status'] == new_status

    def test_patch_order_in_route(self, system_env_with_db):
        one_more_entity = NEW_ENTITY.copy()
        one_more_entity['weight'] = 4.4
        response = env_patch_request(
            system_env_with_db,
            'couriers/{}/routes/{}/orders/{}'.format(
                COURIER_ID, ROUTE_ID, ID
            ),
            data=one_more_entity
        )
        j = response.json()
        print(json.dumps(j))

        assert response.status_code == requests.codes.ok
        assert entity_equal(j, one_more_entity)

    def test_wrong_time_interval(self, system_env_with_db):
        wrong_entity = NEW_ENTITY.copy()
        wrong_entity['time_interval'] = "11"
        response = env_patch_request(
            system_env_with_db,
            '{}/{}/{}'.format(
                api_path_with_company_id(system_env_with_db), "orders", ID
            ),
            data=wrong_entity
        )
        j = response.json()
        print(json.dumps(j))

        assert response.status_code == requests.codes.unprocessable

    def test_patch_order_with_route_id(self, system_env_with_db):
        one_more_entity = NEW_ENTITY.copy()

        # Using own route id is allowed
        one_more_entity['weight'] = 5.5
        one_more_entity['route_id'] = ROUTE_ID
        response = env_patch_request(
            system_env_with_db,
            'couriers/{}/routes/{}/orders/{}'.format(
                COURIER_ID, ROUTE_ID, ID
            ),
            data=one_more_entity
        )
        assert response.status_code == requests.codes.ok
        assert entity_equal(response.json(), one_more_entity)

        # Moving order to other route is not allowed
        one_more_entity['weight'] = 6.6
        one_more_entity['route_id'] = 9999999
        response = env_patch_request(
            system_env_with_db,
            'couriers/{}/routes/{}/orders/{}'.format(
                COURIER_ID, ROUTE_ID, ID
            ),
            data=one_more_entity
        )
        assert response.status_code == requests.codes.unprocessable
        assert response.json()['message'] == 'Moving order to other route is not allowed'

    def test_delete(self, system_env_with_db):
        path = "orders/{}".format(ID)
        response = env_delete_request(
            system_env_with_db,
            api_path_with_company_id(system_env_with_db, path)
        )
        j = response.json()
        print(json.dumps(j))
        assert response.status_code == requests.codes.ok

        response = env_get_request(
            system_env_with_db,
            api_path_with_company_id(system_env_with_db, path)
        )
        j = response.json()
        print(json.dumps(j))
        assert response.status_code == requests.codes.not_found

    def test_post(self, system_env_with_db):
        response = env_post_request(
            system_env_with_db,
            path=api_path_with_company_id(system_env_with_db, "orders"),
            data=ENTITY
        )
        j = response.json()
        print(json.dumps(j))
        assert response.status_code == requests.codes.ok
        assert j["service_duration_s"] == 600
        assert j["shared_service_duration_s"] == 0
        assert len(j["history"]) == 2
        assert j["history"][0]["event"] == "ORDER_CREATED"
        assert entity_equal(j, ENTITY)

        global ID
        ID = j['id']

    def test_post_without_route(self, system_env_with_db):
        entity_number_1 = ENTITY["number"] + "_wo_route_1"
        entity_number_2 = ENTITY["number"] + "_wo_route_2"

        def cleanup(number):
            resp = env_get_request(
                system_env_with_db,
                api_path_with_company_id(system_env_with_db, "orders?number={}".format(number))
            )
            resp.raise_for_status()
            if resp.json():
                self._delete_order_by_id(system_env_with_db, resp.json()[0]["id"])

        cleanup(entity_number_1)
        cleanup(entity_number_2)
        try:
            entity = ENTITY.copy()
            entity["number"] = entity_number_1
            if "route_id" in entity:
                del entity["route_id"]
            response = env_post_request(
                system_env_with_db,
                path=api_path_with_company_id(system_env_with_db, "orders"),
                data=entity
            )
            j = response.json()
            print(json.dumps(j))

            assert response.status_code == requests.codes.unprocessable

            entity = ENTITY.copy()
            entity["number"] = entity_number_2
            if "route_id" in entity:
                del entity["route_id"]

            response = env_post_request(
                system_env_with_db,
                path=api_path_with_company_id(system_env_with_db, "orders-batch"),
                data=[entity]
            )
            j = response.json()
            print(json.dumps(j))

            assert response.status_code == requests.codes.unprocessable
        finally:
            cleanup(entity_number_1)
            cleanup(entity_number_2)

    def test_post_duplicate(self, system_env_with_db):
        response = env_post_request(
            system_env_with_db,
            path=api_path_with_company_id(system_env_with_db, "orders"),
            data=ENTITY
        )
        j = response.json()
        print(json.dumps(j))

        assert response.status_code == requests.codes.unprocessable

    def test_cleanup(self, system_env_with_db):
        response = self._delete_order_by_id(system_env_with_db, ID)
        j = response.json()
        print(json.dumps(j))

        assert response.status_code == requests.codes.ok


class TestChangingOrderTimeWindows(object):
    @skip_if_remote
    def test_previous_day(self, system_env_with_db):
        route_date = datetime.date(2019, 3, 1)

        with create_route_env(system_env_with_db,
                              "test_previous_date",
                              route_date=route_date.isoformat()) as route_env:
            previous_day = route_date - datetime.timedelta(days=1)
            time_interval = {
                'start': datetime.datetime.combine(previous_day, datetime.time(6, 0, 0)).astimezone(),
                'end': datetime.datetime.combine(route_date, datetime.time(8, 0, 0)).astimezone()
            }
            time_interval_str = "{}/{}".format(time_interval['start'].isoformat(), time_interval['end'].isoformat())

            order_patch_data = {
                'time_interval': time_interval_str
            }

            order_number = route_env['orders'][0]['number']
            expected_error_msg = "Time window of the order {} begins before the route starts".format(order_number)

            response = env_patch_request(
                system_env_with_db,
                path=api_path_with_company_id(system_env_with_db, 'orders/{}'.format(route_env['orders'][0]['id'])),
                data=order_patch_data
            )

            assert response.status_code == requests.codes.unprocessable
            assert response.json()['message'] == expected_error_msg

            order_patch_data['number'] = order_number
            response = env_post_request(
                system_env_with_db,
                path=api_path_with_company_id(system_env_with_db, 'orders-batch'),
                data=[order_patch_data]
            )

            assert response.status_code == requests.codes.unprocessable
            assert response.json()['message'] == expected_error_msg

    @skip_if_remote
    def test_different_time_zones(self, system_env_with_db):
        """
        Test the following workflow:
        - create depots in Moscow and Vladivostok and routes for these depots
        - create orders for Moscow route
        - move Moscow orders to Vladivostok route
            * check that no error occured while time_windows shifting
              when new absolute time window involve previous or next day
            * check that INTERVAL_UPDATE event wasn't triggered
        """
        msk_depot_data = {
            'lat': 56.311739,
            'lon': 38.136341,
            'address': 'Sergiev Posad',
            'time_zone': 'Europe/Moscow'
        }
        msk_order_locations = [
            {
                'lat': 56.353244,
                'lon': 38.183455
            },
            {
                'lat': 56.263458,
                'lon': 38.084525
            }
        ]
        msk_time_intervals = {
            "06:00-10:00",
            "18:00-22:00"
        }

        vld_depot_data = {
            'lat': 43.144527,
            'lon': 131.912754,
            'address': 'Vladivostok',
            'time_zone': 'Asia/Vladivostok'
        }

        with create_route_env(system_env_with_db,
                              "test_different_time_zones_1",
                              order_locations=msk_order_locations,
                              time_intervals=msk_time_intervals,
                              route_date="2019-02-27",
                              depot_data=msk_depot_data) as route_env_msk:
            with create_route_env(system_env_with_db,
                                  "test_different_time_zones_2",
                                  route_date="2019-02-27",
                                  depot_data=vld_depot_data) as route_env_vld:

                order_patch_data = {
                    'route_id': route_env_vld['route']['id']
                }

                order_patch_data['number'] = route_env_msk['orders'][0]['number']
                response = env_patch_request(
                    system_env_with_db,
                    path=api_path_with_company_id(system_env_with_db,
                                                  'orders/{}'.format(route_env_msk['orders'][0]['id'])),
                    data=order_patch_data
                )
                assert response.status_code == requests.codes.ok
                assert not has_order_history_event(response.json(), OrderHistoryEvent.interval_update)

                order_patch_data['number'] = route_env_msk['orders'][1]['number']
                response = env_patch_request(
                    system_env_with_db,
                    path=api_path_with_company_id(system_env_with_db,
                                                  'orders/{}'.format(route_env_msk['orders'][1]['id'])),
                    data=order_patch_data
                )
                assert response.status_code == requests.codes.ok
                assert not has_order_history_event(response.json(), OrderHistoryEvent.interval_update)

    @skip_if_remote
    def test_move_order_to_route_with_another_date(self, system_env_with_db):
        """
        Test the following workflow:
        - create 2 routes for 2 different days
        - move one order to another route
            * check that INTERVAL_UPDATE event wasn't triggered
        """
        current_day = datetime.date(2019, 5, 24)
        next_day = current_day + datetime.timedelta(days=1)
        with create_route_env(system_env_with_db,
                              "test_move_order_to_route_with_another_date_1",
                              route_date=current_day.isoformat()) as current_day_env:
            with create_route_env(system_env_with_db,
                                  "test_move_order_to_route_with_another_date_2",
                                  route_date=next_day.isoformat()) as next_day_env:

                order_patch_data = {
                    'number': next_day_env['orders'][0]['number'],
                    'route_id': current_day_env['route']['id']
                }
                order_path = "orders/{}".format(next_day_env['orders'][0]['id'])

                response = env_patch_request(
                    system_env_with_db,
                    path=api_path_with_company_id(system_env_with_db, order_path),
                    data=order_patch_data
                )
                assert response.status_code == requests.codes.ok
                assert not has_order_history_event(response.json(), OrderHistoryEvent.interval_update)


def test_paginated_ordering(system_env_with_db):
    route_1_num_orders = int(1.05 * PAGINATION_PAGE_SIZE)
    route_2_num_orders = int(0.1 * PAGINATION_PAGE_SIZE)
    order_location = {'lat': NEW_ENTITY['lat'], 'lon': NEW_ENTITY['lon']}
    env = system_env_with_db
    with create_tmp_company(env, "Test company test_paginated_ordering") as company_id:
        with create_tmp_user(env, company_id, UserRole.admin) as user:
            auth = env.get_user_auth(user)
            with create_route_env(env,
                                  "test_pagination_page_2_1_",
                                  order_locations=[order_location] * route_1_num_orders,
                                  company_id=company_id,
                                  auth=auth) as route_env1:
                with create_route_env(env,
                                      "test_pagination_page_2_2_",
                                      order_locations=[order_location] * route_2_num_orders,
                                      company_id=company_id,
                                      auth=auth) as route_env2:
                    expected_order_ids = []
                    for route_id in [route_env1['route']['id'], route_env2['route']['id']]:
                        order_ids = get_order_sequence(
                            env,
                            route_id,
                            company_id=company_id,
                            auth=auth)['order_ids']
                        random.shuffle(order_ids)
                        post_order_sequence(
                            env,
                            route_id,
                            order_ids,
                            company_id=company_id,
                            auth=auth)
                        expected_order_ids.extend(order_ids)

                    assert len(expected_order_ids) == route_1_num_orders + route_2_num_orders

                    def fetch_paged_order_ids():
                        ids = []
                        page = 1
                        while True:
                            response = env_get_request(
                                env,
                                api_path_with_company_id(env, "orders?page={}".format(page), company_id=company_id),
                                auth=auth
                            )
                            assert response.status_code == requests.codes.ok
                            orders = response.json()
                            assert len(orders) <= PAGINATION_PAGE_SIZE
                            if len(orders) == 0:
                                break
                            ids.extend([o['id'] for o in orders])
                            page += 1

                        return ids

                    assert expected_order_ids == fetch_paged_order_ids()


@skip_if_remote
def test_status_update_events_source(system_env_with_db):
    env = system_env_with_db

    user_types = {
        UserKind.admin: UserRole.admin,
        UserKind.app: UserRole.app
    }

    with create_tmp_users(env, [system_env_with_db.company_id] * len(user_types), list(user_types.values())) as users:
        users_info = dict(zip(user_types.keys(), users))
        with create_route_env(env, 'test_status_update_events_source') as route_env:
            order_id = route_env['orders'][0]['id']

            # Set order status via api by admin
            changes = {
                'status': 'confirmed'
            }

            path = api_path_with_company_id(system_env_with_db, "orders/{}".format(order_id))
            response = env_patch_request(
                system_env_with_db,
                path=path,
                data=changes,
                auth=env.get_user_auth(users_info[UserKind.admin])
            )
            assert response.status_code == requests.codes.ok
            order = response.json()
            status_update_events = get_order_history_event_records(order, OrderHistoryEvent.status_update)
            assert len(status_update_events) == 1
            assert "source" in status_update_events[0]
            assert status_update_events[0]["source"] == {
                "initiator": "user_api",
                "user_role": "admin"
            }

            # Set order status via app
            changes = {
                'status': 'finished'
            }

            path = "couriers/{}/routes/{}/orders/{}".format(
                route_env['courier']['id'],
                route_env['route']['id'],
                order_id
            )
            response = env_patch_request(
                system_env_with_db,
                path=path,
                data=changes,
                auth=env.get_user_auth(users_info[UserKind.app])
            )
            assert response.status_code == requests.codes.ok
            order = response.json()
            status_update_events = get_order_history_event_records(order, OrderHistoryEvent.status_update)
            assert len(status_update_events) == 2
            assert "source" in status_update_events[1]
            assert status_update_events[1]["source"] == {
                "initiator": "app"
            }


@skip_if_remote
def test_routed_orders_time_now_greater_1_day(system_env_with_db):
    """
    - create order
    - perform routed-orders query with time_now > 1 day
        * check that order has START event
    """
    with create_route_env(system_env_with_db,
                          "test_routed_orders_time_now_greater_1_day",
                          order_locations=[{"lat": 55.733827, "lon": 37.588722}],
                          time_intervals=["05:00-1.01:00"]) as route_env:
        order_id = route_env["orders"][0]["id"]
        courier_id = route_env["courier"]["id"]
        route_id = route_env["route"]["id"]

        order = get_order(system_env_with_db, order_id)
        assert not has_order_history_event(order, OrderHistoryEvent.start)

        response = env_get_request(
            system_env_with_db,
            'couriers/{}/routes/{}/routed-orders?lat={}&lon={}&time_now={}'.format(
                courier_id, route_id, 55.685771, 37.459491, '1.01:00'
            )
        )
        assert response.ok

        order = get_order(system_env_with_db, order_id)
        assert has_order_history_event(order, OrderHistoryEvent.start)


@skip_if_remote
def test_start_history_event_absence_for_finished_order(system_env_with_db):
    route_date = datetime.date.today()
    order_locations = [
        {"lat": 55.733827, "lon": 37.588722},
        {"lat": 55.732521, "lon": 37.600870}
    ]
    test_tz = dateutil.tz.gettz("Europe/Moscow")

    with create_route_env(system_env_with_db,
                          "test_start_history_event_absence_for_finished_order",
                          order_locations=order_locations,
                          time_intervals=["08:00-1.01:00", "07:00-1.01:00"],
                          route_date=route_date.isoformat()) as route_env:
        courier_id = route_env["courier"]["id"]
        route_id = route_env["route"]["id"]

        # Deliver order before minimal start time of route orders intervals
        ts = datetime.datetime.combine(route_date, datetime.time(hour=6)).astimezone(test_tz).timestamp()

        positions_for_delivery = [
            (order_locations[0]['lat'], order_locations[0]['lon'], ts),
            (order_locations[0]['lat'], order_locations[0]['lon'], ts + 600)
        ]
        push_positions(system_env_with_db, courier_id, route_id, positions_for_delivery)

        # Send position with timestamp more than minimal start time of route orders intervals
        # for triggering start event for route orders
        ts = datetime.datetime.combine(route_date, datetime.time(hour=9)).astimezone(test_tz).timestamp()
        positions_for_triggering_start = [
            (order_locations[0]['lat'], order_locations[0]['lon'], ts),
        ]
        push_positions(system_env_with_db, courier_id, route_id, positions_for_triggering_start)

        # Wait route state update by background threads
        sleep(DEFAULT_UPDATE_ROUTE_STATES_DELAY_S + DEFAULT_UPDATE_ROUTE_STATES_PERIOD_S + 5)

        delivered_order = get_order(system_env_with_db, route_env["orders"][0]["id"])
        assert delivered_order["status"] == OrderStatus.finished.value
        assert not has_order_history_event(delivered_order, OrderHistoryEvent.start)

        not_delivered_order = get_order(system_env_with_db, route_env["orders"][1]["id"])
        assert not_delivered_order["status"] == OrderStatus.new.value
        assert has_order_history_event(not_delivered_order, OrderHistoryEvent.start)

        response = env_get_request(
            system_env_with_db,
            api_path_with_company_id(
                system_env_with_db,
                "sms-report?date={}".format(route_date.isoformat())
            )
        )
        assert response.status_code == requests.codes.ok
        sms_list = response.json()

        assert next((sms for sms in sms_list if sms["order_id"] == delivered_order["id"] and sms["type"] == "shift_start"), None) is None
        assert next((sms for sms in sms_list if sms["order_id"] == not_delivered_order["id"] and sms["type"] == "shift_start"), None) is not None


@skip_if_remote
@pytest.mark.parametrize(
    'route_start,order0_window_start,order1_window_start,pushpos_times_and_expected_sms_counts', [
        ('10:00', '11:00', '12:00', [('09:30', 0), ('10:30', 2)]),
        ('10:00', '09:00', '11:00', [('09:30', 0), ('10:30', 2)]),
        ('10:00', '08:00', '09:00', [('09:30', 0), ('10:30', 2)])
    ]
)
def test_route_start_affects_start_smses(
        system_env_with_db,
        route_start,
        order0_window_start,
        order1_window_start,
        pushpos_times_and_expected_sms_counts):
    """
    Test the following workflow:
    - create a two-order route in Moscow with route_start specified
      (trying different combiantions of times: start_time is before,
      after, in between orders' time window starts)
    - send courier positions before and after route_start time
        * check that start SMSes ("Courier picked up you order from depot") are sent
          only after route_start time.
    """

    test_tz = dateutil.tz.gettz('Europe/Moscow')
    route_date = datetime.date.today()
    courier_location = {'lat': 55.733, 'lon': 37.588}
    order_locations = []
    for i in range(2):
        lat, lon = get_position_shifted_east(courier_location['lat'], courier_location['lon'], 2000 * (i + 1))
        order_locations.append({'lat': lat, 'lon': lon})

    with create_route_env(
        system_env_with_db,
        'test_route_start_affects_start_smses',
        order_locations=order_locations,
        time_intervals=['{}-1.23:59'.format(order0_window_start), '{}-1.23:59'.format(order1_window_start)],
        route_date=route_date.isoformat(),
        route_start=route_start
    ) as route_env:
        courier_id = route_env['courier']['id']
        route_id = route_env['route']['id']
        order_ids = [x['id'] for x in route_env['orders']]

        for pushpos_time, expected_sms_count in pushpos_times_and_expected_sms_counts:
            # Send courier position
            t = datetime.datetime.strptime(pushpos_time, '%H:%M').time()
            ts = datetime.datetime.combine(route_date, t).astimezone(test_tz).timestamp()
            pos = (courier_location['lat'], courier_location['lon'], ts)
            push_positions(system_env_with_db, courier_id, route_id, [pos])

            # Trigger SMS sending
            response = env_get_request(
                system_env_with_db,
                'couriers/{}/routes/{}/routed-orders?lat={}&lon={}&time_now={}'.format(
                    courier_id, route_id, pos[0], pos[1], pushpos_time
                )
            )
            assert response.status_code == requests.codes.ok

            # Check that the number of sent start sms messages equals expected
            response = env_get_request(
                system_env_with_db,
                api_path_with_company_id(
                    system_env_with_db,
                    'sms-report?date={}'.format(route_date.isoformat())
                )
            )
            assert response.status_code == requests.codes.ok
            sms_list = response.json()
            assert expected_sms_count == \
                len([sms for sms in sms_list if sms['order_id'] in order_ids and sms['type'] == 'shift_start'])


def test_order_no_phone(system_env_with_db):
    route_date = datetime.date.today()
    order_locations = [
        {"lat": 55.733827, "lon": 37.588722},
        {"lat": 55.732521, "lon": 37.600870}
    ]
    with create_route_env(system_env_with_db,
                          "test_order_no_phone",
                          order_locations=order_locations,
                          time_intervals=["08:00-1.01:00", "07:00-1.01:00"],
                          route_date=route_date.isoformat()) as route_env:
        entity_no_phone = ENTITY.copy()
        del entity_no_phone["phone"]
        entity_no_phone["route_number"] = route_env["route"]["number"]
        entity_no_phone["number"] = "test_order_no_phone_new1"
        entity_phone_is_none = ENTITY.copy()
        entity_phone_is_none["route_number"] = route_env["route"]["number"]
        entity_phone_is_none["number"] = "test_order_no_phone_new2"
        entity_phone_is_none["phone"] = None

        # create
        response = env_post_request(
            system_env_with_db,
            path=api_path_with_company_id(system_env_with_db, "orders"),
            data=entity_no_phone,
        )
        assert response.status_code == requests.codes.ok
        response = env_post_request(
            system_env_with_db,
            path=api_path_with_company_id(system_env_with_db, "orders"),
            data=entity_phone_is_none,
        )
        j = response.json()
        assert response.status_code == requests.codes.ok

        # patch
        path = "orders/{}".format(j["id"])
        response = env_patch_request(
            system_env_with_db,
            path=api_path_with_company_id(system_env_with_db, path),
            data={'phone': "12346"}
        )
        assert response.status_code == requests.codes.ok
        path = "orders/{}".format(j["id"])
        response = env_patch_request(
            system_env_with_db,
            path=api_path_with_company_id(system_env_with_db, path),
            data={'phone': None}
        )
        assert response.status_code == requests.codes.ok

        # batch
        response = env_post_request(
            system_env_with_db,
            path=api_path_with_company_id(system_env_with_db, "orders-batch"),
            data=[entity_no_phone, entity_phone_is_none],
        )
        j = response.json()
        assert response.status_code == requests.codes.ok
        assert j['updated'] == 2


def test_shared_service_duration_s(system_env_with_db):
    """
    Test the following workflow:
    - create route env with 2 orders without shared_service_duration_s
    - perform /routed-orders query with location equal to the first order location
        * check that arrival time to the first order is equal to time_now parameter
            of /routed-orders query
    - patch both orders with shared_service_duration_s
    - perform the same /routed-orders query
        * check that arrival time to second order increased by shared_service_duration_s
    """

    def get_second_order_arrival_time(route_env):
        # pass first location as current location
        response = env_get_request(
            system_env_with_db,
            "couriers/{}/routes/{}/routed-orders?lat={}&lon={}&time_now={}".format(
                route_env["courier"]["id"],
                route_env["route"]["id"],
                order_locations[0]["lat"],
                order_locations[0]["lon"],
                "12:00"
            )
        )
        assert response.status_code == requests.codes.ok

        response = env_get_request(
            system_env_with_db,
            api_path_with_company_id(
                system_env_with_db,
                "route-state?courier_number={}&date={}".format(
                    route_env["courier"]["number"],
                    route_date
                )
            )
        )
        assert response.status_code == requests.codes.ok

        routed_orders = response.json()["routed_orders"]
        assert len(routed_orders) == 2
        assert routed_orders[0]["id"] == route_env["orders"][0]["id"]
        assert routed_orders[0]["arrival_time_s"] == 12 * 60 * 60
        assert routed_orders[1]["id"] == route_env["orders"][1]["id"]

        return routed_orders[1]["arrival_time_s"]

    route_date = datetime.date.today().isoformat()
    order_locations = [
        {"lat": 55.733827, "lon": 37.588722},
        {"lat": 55.732521, "lon": 37.600870}
    ]
    with create_route_env(system_env_with_db,
                          "test_shared_service_duration_s",
                          order_locations=order_locations,
                          route_date=route_date) as route_env:

        arrival_time_wo_shared_service_duration = get_second_order_arrival_time(route_env)

        shared_service_duration_value = 300

        for order in route_env["orders"]:
            path = "orders/{}".format(order["id"])
            response = env_patch_request(
                system_env_with_db,
                path=api_path_with_company_id(system_env_with_db, path),
                data={"shared_service_duration_s": shared_service_duration_value}
            )
            assert response.status_code == requests.codes.ok
            response_order = response.json()
            assert response_order["shared_service_duration_s"] == shared_service_duration_value

        arrival_time_with_shared_service_duration = get_second_order_arrival_time(route_env)

        assert arrival_time_wo_shared_service_duration + shared_service_duration_value == \
            pytest.approx(arrival_time_with_shared_service_duration)


@skip_if_local
def test_date_passing_to_svrp_query(system_env_with_db):
    """
    Test the following workflow:
    - create 2 route envs with route_date at Thursday and Friday with time intervals for 2 days
    - perform /routed-orders query with time_now at the second day for both routes
        * check that arrival time to the second order in Friday route is less than arrival time
            to the second order in Thursday route because in Friday route second day is weeked day
            unlike work day in Thursday route
    Test must use driving router so it can not be launched in component tests
    """

    def get_second_order_arrival_time(route_env):
        response = env_get_request(
            system_env_with_db,
            "couriers/{}/routes/{}/routed-orders?lat={}&lon={}&time_now={}".format(
                route_env["courier"]["id"], route_env["route"]["id"], 55.685772, 37.459492, "1.08:00"
            )
        )
        assert response.status_code == requests.codes.ok
        response_route = response.json()["route"]
        assert len(response_route) == 2
        return response_route[1]["arrival_time_s"]

    route_dates = [
        datetime.date(2020, 1, 16).isoformat(),  # Thirsday
        datetime.date(2020, 1, 17).isoformat()  # Friday
    ]

    order_locations = [
        {"lat": 55.733827, "lon": 37.588722},
        {"lat": 55.732521, "lon": 37.600870}
    ]

    time_intervals_list = [["07:00-19:00", "1.07:00-1.19:00"]] * 2

    with create_route_envs(system_env_with_db,
                           "test_date_passing_to_svrp_query",
                           time_intervals_list=time_intervals_list,
                           order_locations=order_locations,
                           route_dates=route_dates) as route_envs:

        arrival_times = [get_second_order_arrival_time(route_env) for route_env in route_envs]
        assert arrival_times[0] > arrival_times[1]


ORDER_LOCATION = {
    "lat": 55.898266,
    "lon": 37.586571
}

DEPOT_LOCATION = {
    "lat": 55.733827,
    "lon": 37.588722
}


@skip_if_remote
def test_positive_mark_route_started_radius(system_env_with_db):
    """
    Test the following workflow:
    - create route env with single order and depot's field mark_route_started_radius equal to default value (500)
    - perform /routed-orders query with time_now after route start and coordinates at a distance less than radius
        * check that order doesn't have START event
        * check that start sms isn't sent
    - perform /routed-orders query with time_now after route start and coordinates at a distance more than radius
        * check that order has START event
        * check that start sms is sent
    """

    route_datetime = datetime.datetime.now(tz=dateutil.tz.gettz('Europe/Moscow'))

    route_start_radius = 500

    location_inside_radius = get_position_shifted_east(
        DEPOT_LOCATION["lat"],
        DEPOT_LOCATION["lon"],
        route_start_radius - 10
    )

    location_outside_of_radius = get_position_shifted_east(
        DEPOT_LOCATION["lat"],
        DEPOT_LOCATION["lon"],
        route_start_radius + 10
    )

    depot_data = {
        "lat": DEPOT_LOCATION["lat"],
        "lon": DEPOT_LOCATION["lon"]
    }

    with create_route_env(system_env_with_db,
                          "test_route_start_radius",
                          depot_data=depot_data,
                          order_locations=[ORDER_LOCATION],
                          route_date=route_datetime.date().isoformat()) as route_env:
        courier_id = route_env["courier"]["id"]
        route_id = route_env["route"]["id"]
        order_id = route_env["orders"][0]["id"]

        response = env_get_request(
            system_env_with_db,
            "couriers/{}/routes/{}/routed-orders?lat={}&lon={}&time_now={}".format(
                courier_id,
                route_id,
                location_inside_radius[0],
                location_inside_radius[1],
                "08:00"
            )
        )
        assert response.status_code == requests.codes.ok
        order = get_order(system_env_with_db, order_id)
        assert not has_order_history_event(order, OrderHistoryEvent.start)
        sms_list = sms_state(system_env_with_db, route_datetime)
        order_sms = [sms for sms in sms_list if sms["order_id"] == order_id and
                                                sms["type"] == SmsType.shift_start.value]
        assert len(order_sms) == 0

        response = env_get_request(
            system_env_with_db,
            "couriers/{}/routes/{}/routed-orders?lat={}&lon={}&time_now={}".format(
                courier_id,
                route_id,
                location_outside_of_radius[0],
                location_outside_of_radius[1],
                "08:00"
            )
        )
        assert response.status_code == requests.codes.ok
        order = get_order(system_env_with_db, order_id)
        assert has_order_history_event(order, OrderHistoryEvent.start)
        sms_list = sms_state(system_env_with_db, route_datetime)
        order_sms = [sms for sms in sms_list if sms["order_id"] == order_id and
                                                sms["type"] == SmsType.shift_start.value]
        assert len(order_sms) == 1


@skip_if_remote
def test_zero_mark_route_started_radius(system_env_with_db):
    """
    Test the following workflow:
    - create route env with single order and depot's field mark_route_started_radius equal to 0
    - perform /routed-orders query with time_now after route start and coordinates equal to depot's corrdinates
        * check that order has START event
        * check that start sms is sent
    """

    route_datetime = datetime.datetime.now(tz=dateutil.tz.gettz('Europe/Moscow'))

    depot_data = {
        "mark_route_started_radius": 0,
        "lat": DEPOT_LOCATION["lat"],
        "lon": DEPOT_LOCATION["lon"]
    }

    with create_route_env(system_env_with_db,
                          "test_route_start_radius",
                          depot_data=depot_data,
                          order_locations=[ORDER_LOCATION],
                          route_date=route_datetime.date().isoformat()) as route_env:
        courier_id = route_env["courier"]["id"]
        route_id = route_env["route"]["id"]
        order_id = route_env["orders"][0]["id"]

        response = env_get_request(
            system_env_with_db,
            "couriers/{}/routes/{}/routed-orders?lat={}&lon={}&time_now={}".format(
                courier_id,
                route_id,
                DEPOT_LOCATION["lat"],
                DEPOT_LOCATION["lon"],
                "08:00"
            )
        )
        assert response.status_code == requests.codes.ok
        order = get_order(system_env_with_db, order_id)
        assert has_order_history_event(order, OrderHistoryEvent.start)
        sms_list = sms_state(system_env_with_db, route_datetime)
        order_sms = [sms for sms in sms_list if sms["order_id"] == order_id and
                                                sms["type"] == SmsType.shift_start.value]
        assert len(order_sms) == 1


@pytest.mark.parametrize("radius, positions_shift, expected_status",
                         [(100, 150, 'new'), (200, 150, 'finished'), (400, 350, 'finished'), (400, 450, 'new'), (None, 200, 'finished'), (None, 400, 'new')])
def test_mark_delivered_radius(system_env_with_db, radius, positions_shift, expected_status):
    """
    Check radius that is used to mark order as visited is taken from order.mark_delivered_radius if specified else from company.mark_delivered_radius.
    Radius that was used when order was marked as visited is available in /courier-quality and in history events in /order-details in used_mark_delivered_radius.
    Currently specified mark_delivered_radius of an order is available in /orders and /order-details.
    """
    with create_tmp_company(system_env_with_db, "Test company test_mark_delivered_radius") as company_id:
        patch_company(system_env_with_db, {'mark_delivered_radius': 300}, company_id=company_id, auth=system_env_with_db.auth_header_super)
        tmp_company = find_company_by_id(system_env_with_db, company_id, auth=system_env_with_db.auth_header_super)

        now = datetime.datetime.now(tz=dateutil.tz.gettz('Europe/Moscow'))

        order_lat = 55.733827
        order_lon = 37.588722

        with create_route_env(system_env_with_db,
                              "test_mark_delivered_radius",
                              order_locations=[{"lat": order_lat, "lon": order_lon}],
                              time_intervals=[now.isoformat() + '/' + (now + datetime.timedelta(hours=1)).isoformat()],
                              company_id=company_id,
                              route_date=now.date().isoformat(),
                              mark_delivered_radius=radius,
                              auth=system_env_with_db.auth_header_super) as route_env:
            courier_id = route_env["courier"]["id"]
            route_id = route_env["route"]["id"]
            depot_id = route_env["depot"]["id"]
            service_duration_s = route_env["orders"][0]["service_duration_s"]
            order_number = route_env["orders"][0]["number"]
            order_id = route_env["orders"][0]["id"]

            shifted_order_lat, shifted_order_lon = get_position_shifted_east(order_lat, order_lon, positions_shift)

            track = [(shifted_order_lat, shifted_order_lon, now.timestamp())]
            push_positions(system_env_with_db, courier_id, route_id, track, auth=system_env_with_db.auth_header_super)

            track = [(shifted_order_lat, shifted_order_lon, (now + datetime.timedelta(seconds=service_duration_s)).timestamp())]
            push_positions(system_env_with_db, courier_id, route_id, track, auth=system_env_with_db.auth_header_super)

            tracked_radius = radius if radius else tmp_company['mark_delivered_radius']
            shifted_order_lat, shifted_order_lon = get_position_shifted_east(order_lat, order_lon, tracked_radius + 100)

            track = [(shifted_order_lat, shifted_order_lon, (now + datetime.timedelta(seconds=service_duration_s + 10)).timestamp())]
            push_positions(system_env_with_db, courier_id, route_id, track, auth=system_env_with_db.auth_header_super)

            order = get_order(system_env_with_db, order_id, company_id=company_id, auth=system_env_with_db.auth_header_super)
            assert order['mark_delivered_radius'] == radius

            order_details = get_order_details(system_env_with_db, order_number, company_id=company_id, auth=system_env_with_db.auth_header_super)
            assert order_details['status'] == expected_status

            courier_report = get_courier_quality(system_env_with_db, now.date().isoformat(), company_id=company_id, depot_id=depot_id, auth=system_env_with_db.auth_header_super)
            assert len(courier_report) == 1

            if expected_status == 'finished':
                assert courier_report[0]['used_mark_delivered_radius'] == tracked_radius

                for event in [OrderHistoryEvent.arrival, OrderHistoryEvent.visit, OrderHistoryEvent.departure]:
                    records = get_order_history_event_records(order_details, event)
                    assert len(records) == 1
                    assert records[0]['used_mark_delivered_radius'] == tracked_radius

                new_radius = tracked_radius + 100
                patch_order(system_env_with_db, order, {'mark_delivered_radius': new_radius}, company_id=company_id, auth=system_env_with_db.auth_header_super)

                order = get_order(system_env_with_db, order_id, company_id=company_id, auth=system_env_with_db.auth_header_super)
                assert order['mark_delivered_radius'] == new_radius

                order_details = get_order_details(system_env_with_db, order_number, company_id=company_id, auth=system_env_with_db.auth_header_super)
                assert order_details['mark_delivered_radius'] == new_radius

                for event in [OrderHistoryEvent.arrival, OrderHistoryEvent.visit, OrderHistoryEvent.departure]:
                    records = get_order_history_event_records(order_details, event)
                    assert len(records) == 1
                    assert records[0]['used_mark_delivered_radius'] == tracked_radius

                courier_report = get_courier_quality(system_env_with_db, now.date().isoformat(), company_id=company_id, depot_id=depot_id, auth=system_env_with_db.auth_header_super)
                assert courier_report[0]['used_mark_delivered_radius'] == tracked_radius
            else:
                assert courier_report[0]['used_mark_delivered_radius'] is None


def test_order_status_invalid(system_env_with_db):
    route_date = datetime.date.today()
    order_locations = [
        {"lat": 55.733827, "lon": 37.588722},
        {"lat": 55.732521, "lon": 37.600870}
    ]
    with create_route_env(system_env_with_db,
                          "test_order_status_invalid",
                          order_locations=order_locations,
                          time_intervals=["08:00-1.01:00", "07:00-1.01:00"],
                          route_date=route_date.isoformat()) as route_env:
        entity = ENTITY.copy()
        entity["route_number"] = route_env["route"]["number"]
        entity["number"] = "test_order_status_invalid_order"
        entity["status"] = "partly_cancelled"

        # create
        response = env_post_request(
            system_env_with_db,
            path=api_path_with_company_id(system_env_with_db, "orders"),
            data=entity,
        )
        assert response.status_code == requests.codes.unprocessable_entity
        assert "Invalid order status value: 'partly_cancelled'" in response.json()[
            'message']

        # patch
        path = f"orders/{route_env['orders'][0]['id']}"
        response = env_patch_request(
            system_env_with_db,
            path=api_path_with_company_id(system_env_with_db, path),
            data={'status': "partly_cancelled"}
        )
        assert response.status_code == requests.codes.unprocessable_entity
        assert "Invalid order status value: 'partly_cancelled'" in response.json()[
            'message']

        # batch
        response = env_post_request(
            system_env_with_db,
            path=api_path_with_company_id(system_env_with_db, "orders-batch"),
            data=[entity],
        )
        assert response.status_code == requests.codes.unprocessable_entity
        assert "Invalid order status value: 'partly_cancelled'" in response.json()[
            'message']
