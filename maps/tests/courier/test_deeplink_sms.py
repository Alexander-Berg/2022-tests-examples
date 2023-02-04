from datetime import datetime
from flask import g
from http import HTTPStatus

from maps.b2bgeo.ya_courier.backend.test_lib.conftest import skip_if_remote
from maps.b2bgeo.ya_courier.backend.test_lib.util_offline import Environment, local_post
from ya_courier_backend.models import db, DeeplinkLog, Courier, Route
from ya_courier_backend.models.user import UserRole
from ya_courier_backend.util.oauth import UserAccount
from ya_courier_backend.util.node import import_nodes
from ya_courier_backend.util.util import get_by_id_and_company_id


def _init_context(env: Environment):
    g.user = UserAccount(
        id=None,
        login="robot",
        uid=None,
        company_ids=[1],
        is_super=False,
        confirmed_at=datetime.now(),
        role=UserRole.app
    )
    g.dirty_routes = set()
    nodes = [{
        "type": "order",
        "value": {
            "number": "order-1",
            "time_interval": "00:00-23:59",
            "address": "22, Bank Street",
            "lat": 56.320617,
            "lon": 43.999417,
            "status": "confirmed"
        }
    }]
    import_nodes(nodes, env.default_company.id, env.default_route.id)

    courier = get_by_id_and_company_id(
        Courier, env.default_courier.id, env.default_company.id)
    courier.phone = '+1234567890'

    route = get_by_id_and_company_id(Route, env.default_company.id, env.default_route.id)
    route.imei = 123


def _get_path(company_id, route_id):
    return f"/api/v1/companies/{company_id}/routes/{route_id}/deeplink-sms"


@skip_if_remote
def test_deeplink_sms(env: Environment):
    with env.flask_app.app_context():
        _init_context(env)
        local_post(
            env.client,
            _get_path(env.default_company.id, env.default_route.id),
            data={},
            headers=env.user_auth_headers,
            expected_status=HTTPStatus.OK
        )
        log = db.session.query(DeeplinkLog) \
            .filter(
                DeeplinkLog.company_id == env.default_company.id,
                DeeplinkLog.route_id == env.default_route.id,
                DeeplinkLog.courier_id == env.default_courier.id
            ) \
            .first()
        assert log is not None
        assert log.error is None
        assert log.sent_at is not None
        assert log.short_url.startswith('http')


@skip_if_remote
def test_deeplink_sms_phone_is_not_set(env: Environment):
    with env.flask_app.app_context():
        _init_context(env)

        courier = get_by_id_and_company_id(
            Courier, env.default_courier.id, env.default_company.id)
        courier.phone = None

        local_post(
            env.client,
            _get_path(env.default_company.id, env.default_route.id),
            data={},
            headers=env.user_auth_headers,
            expected_status=HTTPStatus.UNPROCESSABLE_ENTITY
        )
        log = db.session.query(DeeplinkLog) \
            .filter(
                DeeplinkLog.company_id == env.default_company.id,
                DeeplinkLog.route_id == env.default_route.id,
                DeeplinkLog.courier_id == env.default_courier.id
            ) \
            .first()
        assert log is None


@skip_if_remote
def test_deeplink_sms_phone_is_invalid(env: Environment):
    with env.flask_app.app_context():
        _init_context(env)

        courier = get_by_id_and_company_id(
            Courier, env.default_courier.id, env.default_company.id)
        courier.phone = 'invalid'

        local_post(
            env.client,
            _get_path(env.default_company.id, env.default_route.id),
            data={},
            headers=env.user_auth_headers,
            expected_status=HTTPStatus.UNPROCESSABLE_ENTITY
        )
        log = db.session.query(DeeplinkLog) \
            .filter(
                DeeplinkLog.company_id == env.default_company.id,
                DeeplinkLog.route_id == env.default_route.id,
                DeeplinkLog.courier_id == env.default_courier.id
            ) \
            .first()
        assert log is None


@skip_if_remote
def test_deeplink_sms_route_is_empty(env: Environment):
    with env.flask_app.app_context():
        _init_context(env)

        import_nodes([], env.default_company.id, env.default_route.id)

        local_post(
            env.client,
            _get_path(env.default_company.id, env.default_route.id),
            data={},
            headers=env.user_auth_headers,
            expected_status=HTTPStatus.UNPROCESSABLE_ENTITY
        )
        log = db.session.query(DeeplinkLog) \
            .filter(
                DeeplinkLog.company_id == env.default_company.id,
                DeeplinkLog.route_id == env.default_route.id,
                DeeplinkLog.courier_id == env.default_courier.id
            ) \
            .first()
        assert log is None


@skip_if_remote
def test_deeplink_sms_route_wrong_company_id(env: Environment):
    with env.flask_app.app_context():
        _init_context(env)

        local_post(
            env.client,
            _get_path(env.default_company.id + 100532, env.default_route.id),
            data={},
            headers=env.user_auth_headers,
            expected_status=HTTPStatus.FORBIDDEN
        )
        log = db.session.query(DeeplinkLog) \
            .filter(
                DeeplinkLog.route_id == env.default_route.id,
                DeeplinkLog.courier_id == env.default_courier.id
            ) \
            .first()
        assert log is None
