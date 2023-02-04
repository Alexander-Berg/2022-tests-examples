from datetime import datetime, time as dt_time
from http import HTTPStatus
import pytest
import time

from maps.b2bgeo.ya_courier.backend.test_lib.conftest import skip_if_remote
from maps.b2bgeo.ya_courier.backend.test_lib.env.mvrp_solver_mock import ROUTES_DATE
from maps.b2bgeo.ya_courier.backend.test_lib.util_offline import local_get, local_post, local_patch
from maps.b2bgeo.ya_courier.backend.test_lib.util import MOSCOW_TZ

from ya_courier_backend.util.preorder import PREORDER_LIMIT

TEST_DATETIME = datetime.combine(ROUTES_DATE, dt_time(9), tzinfo=MOSCOW_TZ)


def _preorder_dict(prefix, additional_fields=False):
    preorder_dict = {
        'number': prefix + '1',
        'phone': '+71112223344',
        'delivery_info': {
            'status': 'active',
        },
    }
    if additional_fields:
        preorder_dict['delivery_info'] = {
            'point': {
                "lat": 58.82,
                "lon": 37.73,
            },
            'delivery_interval': {
                'from': '2019-03-06T17:15:10+03:00',
                'to': '2019-03-07T17:15:10+03:00',
            },
            'address': 'Some address',
            'comment': 'Some comment',
            'time_zone': 'Europe/Moscow',
            'status': 'active',
        }
    return preorder_dict


@skip_if_remote
def test_preorder_get_format(env):
    preorder = _preorder_dict('get_format', additional_fields=True)
    get_path = f'/api/v1/companies/{env.default_company.id}/pre-orders' + \
        '?delivery_from=2019-03-06T17:15:10-03:00&delivery_to=2019-03-06T17:15:10-03:00'
    post_path = f'/api/v1/companies/{env.default_company.id}/pre-orders'

    new_preorders = local_post(env.client, post_path, headers=env.tvm_auth_headers, data=[preorder])
    assert len(new_preorders) == 1
    preorder['id'] = new_preorders[0]['id']
    preorder['created_at'] = new_preorders[0]['created_at']

    resp = local_get(env.client, get_path, headers=env.tvm_auth_headers)[0]
    assert preorder == resp


@skip_if_remote
def test_preorder_get_single(env):
    preorder = _preorder_dict('get_single', additional_fields=True)
    get_path = f'/api/v1/companies/{env.default_company.id}/pre-orders' + \
        '?delivery_from=2019-03-06T17:15:10-03:00&delivery_to=2019-03-06T17:15:10-03:00'
    post_path = f'/api/v1/companies/{env.default_company.id}/pre-orders'

    preorder_id = local_post(env.client, post_path, headers=env.tvm_auth_headers, data=[preorder])[0]['id']
    get_path = f'/api/v1/companies/{env.default_company.id}/pre-orders/{preorder_id}'

    assert local_get(env.client, get_path, headers=env.tvm_auth_headers)


@skip_if_remote
def test_preorder_post(env):
    reservation_required = _preorder_dict('post', additional_fields=False)
    reservation_full = _preorder_dict('post', additional_fields=True)
    reservation_full['number'] += ' some suffix'
    post_path = f'/api/v1/companies/{env.default_company.id}/pre-orders'

    local_post(env.client, post_path, headers=env.tvm_auth_headers,
               data=[reservation_required], expected_status=HTTPStatus.OK)

    local_post(env.client, post_path, headers=env.tvm_auth_headers,
               data=[reservation_full], expected_status=HTTPStatus.OK)

    local_post(env.client, post_path, headers=env.tvm_auth_headers,
               data=[{'number': 'some post', 'phone': '+71112223344'}], expected_status=HTTPStatus.OK)


@skip_if_remote
def test_preorder_post_incorret_second_layer_data(env):
    reservation_full = _preorder_dict('post', additional_fields=True)
    reservation_full['delivery_info']['delivery_interval'] = {
        'start': '2019-03-06T17:15:10+03:00',
        'end': '2019-03-07T17:15:10+03:00',
    },
    post_path = f'/api/v1/companies/{env.default_company.id}/pre-orders'

    local_post(env.client, post_path, headers=env.tvm_auth_headers,
               data=[reservation_full], expected_status=HTTPStatus.UNPROCESSABLE_ENTITY)


@skip_if_remote
def test_preorder_post_incorrect_phone(env):
    reservation_full = _preorder_dict('post', additional_fields=True)
    post_path = f'/api/v1/companies/{env.default_company.id}/pre-orders'
    reservation_full['phone'] = 'incorrect phone'

    local_post(env.client, post_path, headers=env.tvm_auth_headers,
               data=[reservation_full], expected_status=HTTPStatus.UNPROCESSABLE_ENTITY)


@skip_if_remote
def test_preorder_post_incorrect_status(env):
    reservation_full = _preorder_dict('post', additional_fields=True)
    post_path = f'/api/v1/companies/{env.default_company.id}/pre-orders'
    reservation_full['delivery_info']['status'] = 'incorrect status'

    local_post(env.client, post_path, headers=env.tvm_auth_headers,
               data=[reservation_full], expected_status=HTTPStatus.UNPROCESSABLE_ENTITY)


@skip_if_remote
def test_preorder_post_incorrect_time_zone(env):
    reservation_full = _preorder_dict('post', additional_fields=True)
    post_path = f'/api/v1/companies/{env.default_company.id}/pre-orders'
    reservation_full['delivery_info']['time_zone'] = 'incorrect time zone'

    local_post(env.client, post_path, headers=env.tvm_auth_headers,
               data=[reservation_full], expected_status=HTTPStatus.UNPROCESSABLE_ENTITY)


@skip_if_remote
def test_preorder_incorrect_post(env):
    post_path = f'/api/v1/companies/{env.default_company.id}/pre-orders'

    local_post(env.client, post_path, headers=env.tvm_auth_headers,
               data=[{'number': '1'}], expected_status=HTTPStatus.UNPROCESSABLE_ENTITY)

    reservation = _preorder_dict('incorrect_post')
    reservation['some strange key'] = 'some strange value'
    local_post(env.client, post_path, headers=env.tvm_auth_headers,
               data=[reservation], expected_status=HTTPStatus.UNPROCESSABLE_ENTITY)


@skip_if_remote
@pytest.mark.parametrize(
    argnames="from_datetime, response_datetime",
    argvalues=[
        ('2019-03-07T10:00:00', '2019-03-07T14:00:00+07:00'),
        ('2019-03-07T09:00:00+05:00', '2019-03-07T11:00:00+07:00'),
    ],
)
@skip_if_remote
def test_preorder_post_with_time_zone(env, from_datetime, response_datetime):
    reservation = _preorder_dict('truncated_date_utc_offset', additional_fields=True)
    reservation['delivery_info']['time_zone'] = 'UTC+07:00'
    reservation['delivery_info']['delivery_interval']['from'] = from_datetime
    get_path = f'/api/v1/companies/{env.default_company.id}/pre-orders' + \
        '?delivery_from=2019-03-06T17:15:10-03:00&delivery_to=2019-03-07T17:15:10-03:00'
    post_path = f'/api/v1/companies/{env.default_company.id}/pre-orders'

    local_post(env.client, post_path, headers=env.tvm_auth_headers,
               data=[reservation], expected_status=HTTPStatus.OK)
    resp = local_get(env.client, get_path, headers=env.tvm_auth_headers)[0]
    assert resp['delivery_info']['delivery_interval']['from'] == response_datetime


@skip_if_remote
def test_preorder_repeatable_post(env):
    post_path = f'/api/v1/companies/{env.default_company.id}/pre-orders'

    reservation = _preorder_dict('repeatable_post')
    local_post(env.client, post_path, headers=env.tvm_auth_headers,
               data=[reservation, reservation], expected_status=HTTPStatus.UNPROCESSABLE_ENTITY)


@skip_if_remote
def test_preorder_post_number_already_exists(env):
    post_path = f'/api/v1/companies/{env.default_company.id}/pre-orders'

    reservation = _preorder_dict('post_number_already_exists')
    local_post(env.client, post_path, headers=env.tvm_auth_headers,
               data=[reservation], expected_status=HTTPStatus.OK)

    local_post(env.client, post_path, headers=env.tvm_auth_headers,
               data=[reservation], expected_status=HTTPStatus.UNPROCESSABLE_ENTITY)


@skip_if_remote
def test_preorder_get_by_created(env):
    reservation = _preorder_dict('get_by_created', additional_fields=True)
    get_path = f'/api/v1/companies/{env.default_company.id}/pre-orders'
    post_path = f'/api/v1/companies/{env.default_company.id}/pre-orders'

    now = time.time()
    day = 24 * 60 * 60

    from_date = datetime.fromtimestamp(now - day)
    to_date = datetime.fromtimestamp(now + day)
    intersect_query = f'?created_from={from_date}&created_to={to_date}'

    from_date = datetime.fromtimestamp(now + day)
    to_date = datetime.fromtimestamp(now + 2 * day)
    not_intersect_query = f'?created_from={from_date}&created_to={to_date}'

    local_post(env.client, post_path, headers=env.tvm_auth_headers,
               data=[reservation], expected_status=HTTPStatus.OK)

    assert len(local_get(env.client, get_path + intersect_query, headers=env.tvm_auth_headers)) == 1
    assert len(local_get(env.client, get_path + not_intersect_query, headers=env.tvm_auth_headers)) == 0


@skip_if_remote
def test_preorder_get_by_delivery(env):
    reservation = _preorder_dict('get_by_delivery', additional_fields=True)
    get_path = f'/api/v1/companies/{env.default_company.id}/pre-orders'
    post_path = f'/api/v1/companies/{env.default_company.id}/pre-orders'

    intersect_query = '?delivery_from=2019-03-06T17:15:10-03:00&delivery_to=2019-03-07T17:15:10-03:00'
    not_intersect_query = '?delivery_from=2019-02-06T17:15:10-03:00&delivery_to=2019-02-07T17:15:10-03:00'

    local_post(env.client, post_path, headers=env.tvm_auth_headers,
               data=[reservation], expected_status=HTTPStatus.OK)

    assert len(local_get(env.client, get_path + intersect_query, headers=env.tvm_auth_headers)) == 1
    assert len(local_get(env.client, get_path + not_intersect_query, headers=env.tvm_auth_headers)) == 0


@skip_if_remote
def test_preorder_patch_delivery_fields(env):
    reservation = _preorder_dict('patch')
    post_path = f'/api/v1/companies/{env.default_company.id}/pre-orders'
    resp = local_post(env.client, post_path, headers=env.tvm_auth_headers,
                      data=[reservation], expected_status=HTTPStatus.OK)
    get_path = f'/api/v1/companies/{env.default_company.id}/pre-orders/{resp[0]["id"]}'
    patch_path = f'/api/v1/companies/{env.default_company.id}/pre-orders/{resp[0]["id"]}/delivery_info'

    local_patch(env.client, patch_path, headers=env.tvm_auth_headers,
                data={}, expected_status=HTTPStatus.OK)

    local_patch(env.client, patch_path, headers=env.tvm_auth_headers,
                data={'time_zone': 'UTC'}, expected_status=HTTPStatus.OK)

    local_patch(env.client, patch_path, headers=env.tvm_auth_headers,
                data={'time_zone': 'some strange time zone'}, expected_status=HTTPStatus.UNPROCESSABLE_ENTITY)

    local_patch(env.client, patch_path, headers=env.tvm_auth_headers,
                data={'status': 'cancelled'}, expected_status=HTTPStatus.OK)
    preorder = local_get(env.client, get_path, headers=env.tvm_auth_headers)
    assert preorder['delivery_info']['status'] == 'cancelled'

    local_patch(env.client, patch_path, headers=env.tvm_auth_headers,
                data={'address': 'some address', 'comment': 'my home'}, expected_status=HTTPStatus.OK)
    preorder = local_get(env.client, get_path, headers=env.tvm_auth_headers)
    assert preorder['delivery_info']['address'] == 'some address'
    assert preorder['delivery_info']['comment'] == 'my home'


@skip_if_remote
@pytest.mark.parametrize(
    argnames='patch_suffix, status',
    argvalues=[('', HTTPStatus.OK), ('/delivery_info', HTTPStatus.UNPROCESSABLE_ENTITY)],
)
def test_preorder_patch_non_delivery_fields(env, patch_suffix, status):
    reservation = _preorder_dict('patch')
    post_path = f'/api/v1/companies/{env.default_company.id}/pre-orders'
    resp = local_post(env.client, post_path, headers=env.tvm_auth_headers,
                      data=[reservation], expected_status=HTTPStatus.OK)
    patch_path = f'/api/v1/companies/{env.default_company.id}/pre-orders/{resp[0]["id"]}'
    patch_path += patch_suffix

    local_patch(env.client, patch_path, headers=env.tvm_auth_headers,
                data={'number': 'some number'}, expected_status=status)


@skip_if_remote
def test_preorder_incorrect_patch(env):
    reservation = _preorder_dict('incorrect_patch', additional_fields=True)
    post_path = f'/api/v1/companies/{env.default_company.id}/pre-orders'
    resp = local_post(env.client, post_path, headers=env.tvm_auth_headers,
                      data=[reservation], expected_status=HTTPStatus.OK)
    patch_path = f'/api/v1/companies/{env.default_company.id}/pre-orders/{resp[0]["id"]}'

    local_patch(env.client, patch_path, headers=env.tvm_auth_headers,
                data={'new number field': 'some new number'}, expected_status=HTTPStatus.UNPROCESSABLE_ENTITY)

    local_patch(env.client, patch_path, headers=env.tvm_auth_headers,
                data={'time_zone': 'UTC'}, expected_status=HTTPStatus.UNPROCESSABLE_ENTITY)

    incorrect_number_path = f'/api/v1/companies/{env.default_company.id}/pre-orders/very_strange_number'
    local_patch(env.client, incorrect_number_path, headers=env.tvm_auth_headers,
                data={}, expected_status=HTTPStatus.NOT_FOUND)


@skip_if_remote
def test_preorder_post_too_many_entites(env):
    reservations = []
    for i in range(PREORDER_LIMIT + 1):
        reservation = _preorder_dict('post_too_many_entites')
        reservation['number'] += str(i)
        reservations.append(reservation)

    post_path = f'/api/v1/companies/{env.default_company.id}/pre-orders'
    local_post(env.client, post_path, headers=env.tvm_auth_headers,
               data=reservations, expected_status=HTTPStatus.UNPROCESSABLE_ENTITY)


@skip_if_remote
def test_preorder_get_pagination(env):
    reservations = []
    for i in range(15):
        reservation = _preorder_dict('get_pagination', additional_fields=True)
        reservation['number'] += str(i)
        reservations.append(reservation)
    post_path = f'/api/v1/companies/{env.default_company.id}/pre-orders'
    local_post(env.client, post_path, headers=env.tvm_auth_headers,
               data=reservations, expected_status=HTTPStatus.OK)

    get_path = f'/api/v1/companies/{env.default_company.id}/pre-orders' + \
        '?delivery_from=2010-03-06T17:15:10-03:00&delivery_to=2032-03-07T17:15:10-03:00'

    first_part = local_get(env.client, get_path + '&per_page=10&page=1', headers=env.tvm_auth_headers)
    second_part = local_get(env.client, get_path + '&per_page=10&page=2', headers=env.tvm_auth_headers)

    assert len(first_part) == 10
    assert len(second_part) == 5
    numbers = set([item['number'] for item in reservations])
    assert numbers == set([item['number'] for item in first_part + second_part])


@skip_if_remote
def test_preorder_no_tvm(env):
    post_path = f'/api/v1/companies/{env.default_company.id}/pre-orders'

    reservation = _preorder_dict('no_tvm')
    local_post(env.client, post_path, headers=env.user_auth_headers,
               data=[reservation], expected_status=HTTPStatus.UNAUTHORIZED)

    local_post(env.client, post_path, headers=env.superuser_auth_headers,
               data=[reservation], expected_status=HTTPStatus.UNAUTHORIZED)
