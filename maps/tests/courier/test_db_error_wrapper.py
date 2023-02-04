from http import HTTPStatus

from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker

from maps.b2bgeo.ya_courier.backend.test_lib.conftest import skip_if_remote
from maps.b2bgeo.ya_courier.backend.test_lib.util_offline import local_post, local_patch
from maps.b2bgeo.ya_courier.backend.test_lib.util import check_response_with_db_error
from ya_courier_backend.util.db_errors import FIELD_VALUE_LENGTH_MESSAGE, LOCK_NOT_AVAILABLE_MESSAGE, \
    ROUTE_NUMBER_UNIQUE_CONSTRAINT_MESSAGE, COURIER_UNIQUE_CONSTRAINT_MESSAGE, \
    IMEI_IS_ZERO_MESSAGE
from ya_courier_backend.models import db, Courier


@skip_if_remote
def test_duplicate_key_courier(env):
    courier_data = {'number': env.default_courier.number}
    path_courier = f'/api/v1/companies/{env.default_company.id}/couriers'
    resp_json = local_post(env.client, path_courier, headers=env.user_auth_headers, data=courier_data,
                           expected_status=HTTPStatus.UNPROCESSABLE_ENTITY)

    check_response_with_db_error(resp_json['message'],
                                 COURIER_UNIQUE_CONSTRAINT_MESSAGE.format(number=courier_data['number']))


@skip_if_remote
def test_duplicate_key_route(env):
    route_data = {
        'number': env.default_route.number,
        'courier_id': env.default_courier.id,
        'depot_id': env.default_depot.id,
        'date': '2021-01-01'
    }
    path_route = f'/api/v1/companies/{env.default_company.id}/routes'
    resp_json = local_post(env.client, path_route, headers=env.user_auth_headers, data=route_data,
                           expected_status=HTTPStatus.UNPROCESSABLE_ENTITY)

    check_response_with_db_error(resp_json['message'],
                                 ROUTE_NUMBER_UNIQUE_CONSTRAINT_MESSAGE.format(number=env.default_route.number))


@skip_if_remote
def test_lock_not_available(env):
    engine = create_engine(env.flask_app.config['SQLALCHEMY_DATABASE_URI'])
    Session = sessionmaker(bind=engine)
    session = Session()

    with env.flask_app.app_context():
        db.session.execute("set lock_timeout='10s'")
        db.session.commit()

    session.query(Courier).with_for_update().all()

    courier_data = [{'number': env.default_courier.number, 'phone': '89995553311'}]
    path_courier = f'/api/v1/companies/{env.default_company.id}/couriers-batch'
    resp_json = local_post(env.client, path_courier, headers=env.user_auth_headers, data=courier_data,
                           expected_status=HTTPStatus.SERVICE_UNAVAILABLE)

    check_response_with_db_error(resp_json['message'], LOCK_NOT_AVAILABLE_MESSAGE)


@skip_if_remote
def test_value_too_long(env):
    too_long_value = 'a' * 100

    route_data = {
        'number': too_long_value,
        'date': '2021-01-01',
        'courier_number': env.default_courier.number,
        'depot_number': env.default_depot.number
    }
    path_route = f'/api/v1/companies/{env.default_company.id}/routes'
    resp_json = local_post(env.client, path_route, headers=env.user_auth_headers, data=route_data,
                           expected_status=HTTPStatus.UNPROCESSABLE_ENTITY)

    check_response_with_db_error(resp_json['message'], FIELD_VALUE_LENGTH_MESSAGE.format(field_length='80'))


def test_imei_is_zero(env):
    route_data = {
        'imei': 0
    }
    path_route = f'/api/v1/companies/{env.default_company.id}/routes/{env.default_route.id}'
    resp_json = local_patch(env.client, path_route, headers=env.user_auth_headers, data=route_data,
                            expected_status=HTTPStatus.UNPROCESSABLE_ENTITY)

    check_response_with_db_error(resp_json['message'], IMEI_IS_ZERO_MESSAGE)
