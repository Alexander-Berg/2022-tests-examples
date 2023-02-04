import datetime
import dateutil
from http import HTTPStatus
import pytest

from maps.b2bgeo.ya_courier.backend.test_lib.conftest import skip_if_remote
from maps.b2bgeo.ya_courier.backend.test_lib.util_offline import (
    Environment,
    local_post,
    local_get,
    local_delete,
    local_patch,
    create_order,
    create_photo_post_dict,
)
from maps.b2bgeo.ya_courier.backend.test_lib.util_db import add_user, add_user_depot, add_user_shared_company

from ya_courier_backend.models import db, Photo, Order, UserRole


def _create_route(env, depot_number=None, route_number='fake route'):
    path = f'/api/v1/companies/{env.default_company.id}/routes'
    if depot_number is None:
        depot_number = env.default_depot.number
    return local_post(
        env.client,
        path,
        headers=env.user_auth_headers,
        data={
            'number': route_number,
            'courier_number': env.default_courier.number,
            'depot_number': depot_number,
            'date': '2022-01-18',
        },
    )


def _create_courier(env, number='8888'):
    path = f'/api/v1/companies/{env.default_company.id}/couriers'
    return local_post(env.client, path, headers=env.user_auth_headers, data={'number': number})


@skip_if_remote
def test_photos_invalid_list_post(env: Environment):
    new_order = create_order(env, route_id=env.default_route.id)

    path_photos = f'/api/v1/internal/companies/{env.default_company.id}/orders/{new_order["id"]}/photos'

    local_post(env.client, path_photos, headers=env.tvm_auth_headers, data=create_photo_post_dict(env),
               expected_status=HTTPStatus.UNPROCESSABLE_ENTITY)


@skip_if_remote
def test_photos_invalid_extra_params_post(env: Environment):
    new_order = create_order(env, route_id=env.default_route.id)

    path_photos = f'/api/v1/internal/companies/{env.default_company.id}/orders/{new_order["id"]}/photos'

    invalid_data = create_photo_post_dict(env)
    invalid_data['extra_field'] = 'extra field data'

    local_post(env.client, path_photos, headers=env.tvm_auth_headers, data=[invalid_data],
               expected_status=HTTPStatus.UNPROCESSABLE_ENTITY)


@skip_if_remote
def test_photos_invalid_fewer_params_post(env: Environment):
    new_order = create_order(env, route_id=env.default_route.id)

    path_photos = f'/api/v1/internal/companies/{env.default_company.id}/orders/{new_order["id"]}/photos'

    invalid_data = create_photo_post_dict(env)
    invalid_data.pop('ttl_s')

    local_post(env.client, path_photos, headers=env.tvm_auth_headers, data=[invalid_data],
               expected_status=HTTPStatus.UNPROCESSABLE_ENTITY)


@pytest.mark.parametrize(
    'created_at',
    [
        '2022-01-18T15:30:45+00:00',
        '2022-01-18T15:30:45+07:00',
        '2022-01-18T15:30:45-09:00',
    ]
    )
@skip_if_remote
def test_photos_correct_timezone_post(env: Environment, created_at):
    new_order = create_order(env, route_id=env.default_route.id)

    path_photos = f'/api/v1/internal/companies/{env.default_company.id}/orders/{new_order["id"]}/photos'

    post_data = create_photo_post_dict(env, created_at=created_at)

    local_post(env.client, path_photos, headers=env.tvm_auth_headers, data=[post_data])

    with env.flask_app.app_context():
        db_photo = db.session.query(Photo).first()

    assert datetime.datetime.fromisoformat(created_at) == db_photo.created_at


@skip_if_remote
def test_photos_no_timezone_post(env: Environment):
    created_at_no_tz = '2022-01-18T15:30:45'
    created_at_utc = created_at_no_tz+'+00:00'

    new_order = create_order(env, route_id=env.default_route.id)

    path_photos = f'/api/v1/internal/companies/{env.default_company.id}/orders/{new_order["id"]}/photos'
    post_data = create_photo_post_dict(env, created_at=created_at_no_tz)
    local_post(env.client, path_photos, headers=env.tvm_auth_headers, data=[post_data])

    with env.flask_app.app_context():
        db_photo = db.session.query(Photo).first()

    assert datetime.datetime.fromisoformat(created_at_utc) == db_photo.created_at


@pytest.mark.parametrize(
    ('tags', 'expected_status'),
    [
        (['len of 81 is too long'.ljust(81)], HTTPStatus.UNPROCESSABLE_ENTITY),
        (['len of 80 is valid '.ljust(80)], HTTPStatus.OK),
        (['valid', 'tags'], HTTPStatus.OK),
        ('this is not a list', HTTPStatus.UNPROCESSABLE_ENTITY),
        (['forbidden char', 'is=present'], HTTPStatus.UNPROCESSABLE_ENTITY),
    ]
)
@skip_if_remote
def test_photos_tags_post(env: Environment, tags, expected_status):
    new_order = create_order(env, route_id=env.default_route.id)

    path_photos = f'/api/v1/internal/companies/{env.default_company.id}/orders/{new_order["id"]}/photos'

    post_data = create_photo_post_dict(env, tags=tags)

    local_post(env.client, path_photos, headers=env.tvm_auth_headers, data=[post_data], expected_status=expected_status)


@skip_if_remote
def test_photos_invalid_tvm_post(env: Environment):
    new_order = create_order(env, route_id=env.default_route.id)

    path_photos = f'/api/v1/internal/companies/{env.default_company.id}/orders/{new_order["id"]}/photos'

    post_data = create_photo_post_dict(env)

    local_post(env.client, path_photos, headers=env.user_auth_headers, data=[post_data],
               expected_status=HTTPStatus.UNAUTHORIZED)


@skip_if_remote
def test_photos_valid_only_required_post(env: Environment):
    new_order = create_order(env, route_id=env.default_route.id)

    path_photos = f'/api/v1/internal/companies/{env.default_company.id}/orders/{new_order["id"]}/photos'

    post_data = create_photo_post_dict(env)
    post_data.pop('comment')

    local_post(env.client, path_photos, headers=env.tvm_auth_headers, data=[post_data],
               expected_status=HTTPStatus.OK)

    path_photos_get = f'/api/v1/companies/{env.default_company.id}/photos'
    photos = local_get(env.client, path_photos_get, headers=env.user_auth_headers)
    assert len(photos) == 1
    assert 'comment' not in photos[0]


@pytest.mark.parametrize(
    ('photos_count', 'expected_status'),
    [
        (100, HTTPStatus.OK),
        (101, HTTPStatus.UNPROCESSABLE_ENTITY),
    ]
)
@skip_if_remote
def test_photos_batch_post(env: Environment, photos_count, expected_status):
    new_order = create_order(env, route_id=env.default_route.id)

    path_photos = f'/api/v1/internal/companies/{env.default_company.id}/orders/{new_order["id"]}/photos'

    local_post(env.client, path_photos, headers=env.tvm_auth_headers,
               data=[create_photo_post_dict(env) for _ in range(photos_count)], expected_status=expected_status)


@skip_if_remote
def test_photos_valid_post(env: Environment):
    new_order = create_order(env, route_id=env.default_route.id)

    path_photos = f'/api/v1/internal/companies/{env.default_company.id}/orders/{new_order["id"]}/photos'

    post_data = create_photo_post_dict(env)

    post_response = local_post(env.client, path_photos, headers=env.tvm_auth_headers, data=[post_data])
    with env.flask_app.app_context():
        db_photo = db.session.query(Photo).first()

    assert len(post_response) == 1
    assert 'id' in post_response[0]

    post_data.pop('created_at')
    for key in post_data:
        assert post_data[key] == db_photo.as_dict()[key]


@skip_if_remote
def test_photo_order_cascade_delete(env: Environment):
    new_order = create_order(env, route_id=env.default_route.id)

    path_photos = f'/api/v1/internal/companies/{env.default_company.id}/orders/{new_order["id"]}/photos'

    post_data = create_photo_post_dict(env)

    local_post(env.client, path_photos, headers=env.tvm_auth_headers, data=[post_data])
    with env.flask_app.app_context():
        db_photo = db.session.query(Photo).first()
        assert db_photo
        Order.delete(order_id=new_order['id'])
        db_photo = db.session.query(Photo).first()
        assert not db_photo


@skip_if_remote
def test_photos_valid_get(env: Environment):
    new_order = create_order(env, route_id=env.default_route.id)
    path_photos = f'/api/v1/internal/companies/{env.default_company.id}/orders/{new_order["id"]}/photos'
    post_data = create_photo_post_dict(env)

    post_response = local_post(env.client, path_photos, headers=env.tvm_auth_headers, data=[post_data])

    path_photos_get = f'/api/v1/companies/{env.default_company.id}/photos?ids={str(post_response[0]["id"])}'
    get_response = local_get(env.client, path_photos_get, headers=env.user_auth_headers)

    post_data.pop('created_at')
    for key in post_data:
        assert post_data[key] == get_response[0][key]


@pytest.mark.parametrize(
    'filter',
    [
        'ids',
        'route_ids',
        'order_ids',
        'courier_id',
        'tag'
    ]
    )
@skip_if_remote
def test_photos_filters_get(env: Environment, filter):
    new_order_1 = create_order(env, route_id=env.default_route.id)
    path_photos_1 = f'/api/v1/internal/companies/{env.default_company.id}/orders/{new_order_1["id"]}/photos'
    post_data_1 = create_photo_post_dict(env, tags=['tag_1', 'tag_2'])
    local_post(env.client, path_photos_1, headers=env.tvm_auth_headers, data=[post_data_1])

    new_route_2 = _create_route(env)
    new_order_2 = create_order(env, route_id=new_route_2['id'], number='9999')
    new_courier_2 = _create_courier(env, '8888')
    path_photos_2 = f'/api/v1/internal/companies/{env.default_company.id}/orders/{new_order_2["id"]}/photos'
    post_data_2 = create_photo_post_dict(env, tags=['tag_2', 'tag_3'], courier_id=str(new_courier_2['id']))
    post_response_2 = local_post(env.client, path_photos_2, headers=env.tvm_auth_headers, data=[post_data_2])

    filter_data = {
        'ids': post_response_2[0]['id'],
        'route_ids': new_route_2['id'],
        'order_ids': new_order_2['id'],
        'courier_id': new_courier_2['id'],
        'tag': 'tag_3',
    }

    path_photos_get = f'/api/v1/companies/{env.default_company.id}/photos?{filter}={filter_data[filter]}'
    get_response = local_get(env.client, path_photos_get, headers=env.user_auth_headers)

    assert len(get_response) == 1
    assert get_response[0]['id'] == post_response_2[0]['id']


@pytest.mark.parametrize(
    'cgi_params',
    [
        'route_ids=1',
        'order_ids=1453',
        'ids=1,2',
        'tag=address',
        'route_ids=1&ids=1',
    ]
)
@skip_if_remote
def test_photos_cgi_params_get(env: Environment, cgi_params):
    new_order = create_order(env, route_id=env.default_route.id)

    path_photos = f'/api/v1/internal/companies/{env.default_company.id}/orders/{new_order["id"]}/photos'
    post_data = [create_photo_post_dict(env), create_photo_post_dict(env)]

    local_post(env.client, path_photos, headers=env.tvm_auth_headers, data=post_data)

    path_photos_get = f'/api/v1/companies/{env.default_company.id}/photos?{cgi_params}'

    local_get(env.client, path_photos_get, headers=env.user_auth_headers,
              expected_status=HTTPStatus.OK)


@skip_if_remote
def test_photos_invalid_cgi_params_route_order_get(env: Environment):
    path_photos_get = f'/api/v1/companies/{env.default_company.id}/photos?route_ids=1&order_ids=1'

    local_get(env.client, path_photos_get, headers=env.user_auth_headers,
              expected_status=HTTPStatus.UNPROCESSABLE_ENTITY)


@skip_if_remote
def test_photos_invalid_cgi_params_filter_get(env: Environment):
    path_photos_get = f'/api/v1/companies/{env.default_company.id}/photos?ids=1&invalid_filter=3,4'

    local_get(env.client, path_photos_get, headers=env.user_auth_headers,
              expected_status=HTTPStatus.UNPROCESSABLE_ENTITY)


@skip_if_remote
def test_photos_missing_courier_id_get(env: Environment):
    path_photos_get = f'/api/v1/companies/{env.default_company.id}/photos?ids=1'
    _, user_auth = add_user(env, 'test_app_login', UserRole.app)

    local_get(env.client, path_photos_get, headers=user_auth,
              expected_status=HTTPStatus.FORBIDDEN)


@pytest.mark.parametrize(
    'user_role',
    [
        UserRole.dispatcher,
        UserRole.manager,
    ]
)
@skip_if_remote
def test_photos_get_with_no_accsess_to_depot(env: Environment, user_role):
    new_order = create_order(env, route_id=env.default_route.id)
    path_photos = f'/api/v1/internal/companies/{env.default_company.id}/orders/{new_order["id"]}/photos'
    post_data = create_photo_post_dict(env)

    local_post(env.client, path_photos, headers=env.tvm_auth_headers, data=[post_data])

    path_photos_get = f'/api/v1/companies/{env.default_company.id}/photos?route_ids={env.default_route.id}'
    user_id, user_auth = add_user(env, 'test_photo_user_login', user_role, user_company_id=env.default_company.id)

    get_response = local_get(env.client, path_photos_get, headers=user_auth, expected_status=HTTPStatus.FORBIDDEN)

    add_user_depot(env, user_id, env.default_route.depot_id)

    get_response = local_get(env.client, path_photos_get, headers=user_auth)
    assert len(get_response) == 1


@skip_if_remote
def test_photos_expired_get(env: Environment):
    new_order = create_order(env, route_id=env.default_route.id)
    path_photos = f'/api/v1/internal/companies/{env.default_company.id}/orders/{new_order["id"]}/photos'
    post_data = [
        create_photo_post_dict(
            env,
            created_at=(datetime.datetime.now(dateutil.tz.gettz('UTC'))-
                        datetime.timedelta(days=13, hours=23, minutes=58)).isoformat(),
            ttl_s=1209600,
        ),
        create_photo_post_dict(
            env,
            created_at=(datetime.datetime.now(dateutil.tz.gettz('UTC'))-datetime.timedelta(weeks=2)).isoformat(),
            ttl_s=1209600,
        ),
    ]

    post_response = local_post(env.client, path_photos, headers=env.tvm_auth_headers, data=post_data)

    path_photos_get = f'/api/v1/companies/{env.default_company.id}/photos?route_ids={env.default_route.id}'
    get_response = local_get(env.client, path_photos_get, headers=env.user_auth_headers)

    assert len(get_response) == 1
    assert get_response[0]['id'] == post_response[0]['id']


@skip_if_remote
def test_photos_valid_delete(env: Environment):
    new_order = create_order(env, route_id=env.default_route.id)
    path_photos = f'/api/v1/internal/companies/{env.default_company.id}/orders/{new_order["id"]}/photos'
    post_response = local_post(env.client, path_photos, headers=env.tvm_auth_headers,
                               data=[create_photo_post_dict(env)])

    path_photos_get = f'/api/v1/companies/{env.default_company.id}/photos'
    get_response = local_get(env.client, path_photos_get, headers=env.user_auth_headers)
    assert len(get_response) == 1

    id_to_delete = post_response[0]['id']
    path_photos_delete = f'/api/v1/internal/companies/{env.default_company.id}/photos?ids={id_to_delete}'
    local_delete(env.client, path_photos_delete, headers=env.tvm_auth_headers)

    get_response = local_get(env.client, path_photos_get, headers=env.user_auth_headers)
    assert len(get_response) == 0


@skip_if_remote
def test_photos_valid_multiple_delete(env: Environment):
    new_order = create_order(env, route_id=env.default_route.id)
    path_photos = f'/api/v1/internal/companies/{env.default_company.id}/orders/{new_order["id"]}/photos'
    post_data = [create_photo_post_dict(env), create_photo_post_dict(env), create_photo_post_dict(env)]
    post_response = local_post(env.client, path_photos, headers=env.tvm_auth_headers, data=post_data)

    path_photos_get = f'/api/v1/companies/{env.default_company.id}/photos'
    get_response = local_get(env.client, path_photos_get, headers=env.user_auth_headers)
    assert len(get_response) == 3

    ids_to_delete = ','.join([post_response[0]['id'], post_response[2]['id']])
    path_photos_delete = f'/api/v1/internal/companies/{env.default_company.id}/photos?ids={ids_to_delete}'
    local_delete(env.client, path_photos_delete, headers=env.tvm_auth_headers)

    get_response = local_get(env.client, path_photos_get, headers=env.user_auth_headers)
    assert len(get_response) == 1
    assert get_response[0]['id'] == post_response[1]['id']


@skip_if_remote
def test_photos_invalid_long_delete(env: Environment):
    long_id_list = ','.join([str(x) for x in range(1, 102)])

    path_photos_delete = f'/api/v1/internal/companies/{env.default_company.id}/photos?ids={long_id_list}'
    local_delete(env.client, path_photos_delete, headers=env.tvm_auth_headers,
                 expected_status=HTTPStatus.UNPROCESSABLE_ENTITY)


@skip_if_remote
def test_photos_invalid_empty_cgi_delete(env: Environment):
    path_photos_delete = f'/api/v1/internal/companies/{env.default_company.id}/photos?ids='
    local_delete(env.client, path_photos_delete, headers=env.tvm_auth_headers,
                 expected_status=HTTPStatus.UNPROCESSABLE_ENTITY)


@skip_if_remote
def test_photos_invalid_extra_params_delete(env: Environment):
    path_photos_delete = f'/api/v1/internal/companies/{env.default_company.id}/photos?ids=1&order_ids=1'
    local_delete(env.client, path_photos_delete, headers=env.tvm_auth_headers,
                 expected_status=HTTPStatus.UNPROCESSABLE_ENTITY)


@skip_if_remote
def test_photos_with_different_expire_state(env: Environment):
    new_order = create_order(env, route_id=env.default_route.id)
    path_photos = f'/api/v1/internal/companies/{env.default_company.id}/orders/{new_order["id"]}/photos'
    post_data = [
        create_photo_post_dict(
            env,
            created_at=(datetime.datetime.now(dateutil.tz.gettz('UTC')) - datetime.timedelta(weeks=2)).isoformat(),
            ttl_s=60 * 60,
        ),
    ]
    post_response = local_post(env.client, path_photos, headers=env.tvm_auth_headers, data=post_data)

    ids = ','.join([item['id'] for item in post_response])
    path_photos_get = f'/api/v1/companies/{env.default_company.id}/photos?ids={ids}'
    local_get(env.client, path_photos_get, headers=env.user_auth_headers, expected_status=HTTPStatus.NOT_FOUND)


@pytest.mark.parametrize(
    'user_role',
    [
        UserRole.dispatcher,
        UserRole.manager,
    ]
)
@skip_if_remote
def test_photos_get_with_multiple_depot_access(env: Environment, user_role):
    photo_count = 2

    new_order = create_order(env, route_id=env.default_route.id)
    user_id, user_auth = add_user(env, 'test_photo_user_login', user_role,
                                  user_company_id=env.default_company.id)

    # Create depot and route with aceess so cartesian product will work
    depot_number = 'additional depot'
    depot_data = {
        'address': 'Some address',
        'lat': 55,
        'lon': 33,
        'number': depot_number,
    }
    add_depot_path = f'/api/v1/companies/{env.default_company.id}/depots'
    depot = local_post(env.client, add_depot_path, headers=env.user_auth_headers, data=depot_data)
    add_user_depot(env, user_id, depot['id'])
    _create_route(env, depot_number=depot_number)

    path_photos = f'/api/v1/internal/companies/{env.default_company.id}/orders/{new_order["id"]}/photos'
    post_data = create_photo_post_dict(env)

    local_post(env.client, path_photos, headers=env.tvm_auth_headers, data=[post_data] * photo_count)

    path_photos_get = f'/api/v1/companies/{env.default_company.id}/photos'

    get_response = local_get(env.client, path_photos_get, headers=user_auth)
    assert len(get_response) == 0

    add_user_depot(env, user_id, env.default_route.depot_id)

    get_response = local_get(env.client, path_photos_get, headers=user_auth)
    assert len(get_response) == photo_count

    path_photos_get += f'?order_ids={new_order["id"]}'
    get_response = local_get(env.client, path_photos_get, headers=user_auth)
    assert len(get_response) == photo_count

    get_response = local_get(env.client, path_photos_get + '&page=1', headers=user_auth)
    assert len(get_response) == photo_count

    get_response = local_get(env.client, path_photos_get + '&page=100', headers=user_auth)
    assert len(get_response) == 0


@skip_if_remote
def test_photos_for_shared_company(env: Environment):
    #  1. Prepare two orders and user from another company with photo on first order
    new_order = create_order(env, route_id=env.default_route.id)
    new_order_2 = create_order(
        env, route_id=env.default_route.id, number='test_photo_order')
    user_id, user_auth = add_user(env, 'test_photo_user_login', UserRole.admin,
                                  user_company_id=env.default_shared_company.id)
    add_user_shared_company(env, user_id, env.default_company.id)
    path_photos_get = f'/api/v1/companies/{env.default_company.id}/photos'

    path_photos = f'/api/v1/internal/companies/{env.default_company.id}/orders/{new_order["id"]}/photos'
    post_data = create_photo_post_dict(env)
    photo_id = local_post(env.client, path_photos, headers=env.tvm_auth_headers, data=[post_data])[0]['id']

    #  2. Check that we get forbidden error anywhere
    local_get(env.client, path_photos_get +
              f'?route_ids={env.default_route.id}', headers=user_auth, expected_status=HTTPStatus.FORBIDDEN)
    local_get(env.client, path_photos_get +
              f'?order_ids={new_order["id"]}', headers=user_auth, expected_status=HTTPStatus.FORBIDDEN)
    local_get(env.client, path_photos_get +
              f'?ids={photo_id}', headers=user_auth, expected_status=HTTPStatus.FORBIDDEN)

    #  3. Share order without photos
    patch_order_path = f'/api/v1/companies/{env.default_company.id}/orders/{new_order_2["id"]}'
    path_order_data = {'shared_with_company_ids': [env.default_shared_company.id]}
    local_patch(env.client, patch_order_path, headers=env.user_auth_headers, data=path_order_data)

    #  4. Route is avaliable but no photos for shared orders
    assert len(local_get(env.client, path_photos_get +
               f'?route_ids={env.default_route.id}', headers=user_auth)) == 0

    #  5. One of orders not shared
    local_get(env.client, path_photos_get +
              f'?order_ids={new_order["id"]}', headers=user_auth, expected_status=HTTPStatus.FORBIDDEN)

    #  6. We try to get photo, but it will be filtered and we didn't have any to present, so we give not_found
    local_get(env.client, path_photos_get +
              f'?ids={photo_id}', headers=user_auth, expected_status=HTTPStatus.NOT_FOUND)

    #  7. Share second order and see that we can see photo right now
    patch_order_path = f'/api/v1/companies/{env.default_company.id}/orders/{new_order["id"]}'
    path_order_data = {'shared_with_company_ids': [env.default_shared_company.id]}
    local_patch(env.client, patch_order_path, headers=env.user_auth_headers, data=path_order_data)

    assert len(local_get(env.client, path_photos_get +
               f'?route_ids={env.default_route.id}', headers=user_auth)) == 1
    assert len(local_get(env.client, path_photos_get +
               f'?order_ids={new_order["id"]}', headers=user_auth)) == 1
    assert len(local_get(env.client, path_photos_get +
               f'?ids={photo_id}', headers=user_auth)) == 1
