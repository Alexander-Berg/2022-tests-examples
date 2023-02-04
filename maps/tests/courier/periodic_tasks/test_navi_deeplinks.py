from datetime import datetime

import pytest
from flask import g

from maps.b2bgeo.ya_courier.backend.test_lib.conftest import skip_if_remote
from maps.b2bgeo.ya_courier.backend.test_lib.util_offline import Environment
from maps.b2bgeo.ya_courier.backend.test_lib.util import MOSCOW_TZ
from ya_courier_backend.models import db, Courier, LogisticCompany, DeeplinkLog, Route
from ya_courier_backend.models.user import UserRole
from ya_courier_backend.tasks.send_navi_deeplinks import SendNaviDeeplinksTask
from ya_courier_backend.util.node import import_nodes
from ya_courier_backend.util.oauth import UserAccount
from ya_courier_backend.util.phone import IncorrectPhoneFormat, normalize_phone
from ya_courier_backend.util.util import get_by_id, get_by_id_and_company_id


def _create_nodes(company_id, route_id):
    nodes = [{
        "type": "order",
        "value": {
            "number": "order-1",
            "time_interval": "00:00-23:59",
            "address": "22, Bank Street",
            "lat": 56.320618,
            "lon": 43.999413,
            "status": "confirmed"
        }
    }]
    g.dirty_routes = set()
    import_nodes(nodes, company_id, route_id)


@skip_if_remote
def test_send_sms(env: Environment):
    with env.flask_app.app_context():
        g.user = UserAccount(
            id=None,
            login="robot",
            uid=None,
            company_ids=[1],
            is_super=False,
            confirmed_at=datetime.now(),
            role=UserRole.app
        )

        _create_nodes(env.default_company.id, env.default_courier.id)

        courier = get_by_id_and_company_id(Courier, 1, 1)
        courier.phone = '+1234567890'

        company = get_by_id(LogisticCompany, 1)
        company.update({'send_navi_deeplinks': True})

        task = SendNaviDeeplinksTask(env.flask_app)
        task_state = {
            'now_timestamp': datetime(2021, 1, 1, tzinfo=MOSCOW_TZ)
        }
        result = task.run(task_state)
        assert not result['start_immediately']

        log = db.session.query(DeeplinkLog) \
            .filter(
                DeeplinkLog.courier_id == courier.id,
                DeeplinkLog.company_id == company.id
            ) \
            .first()
        assert log.error is None
        assert log.sent_at is not None
        assert log.short_url.startswith('http')


@skip_if_remote
def test_do_not_send_sms_if_company_flag_is_not_set(env: Environment):
    with env.flask_app.app_context():
        g.user = UserAccount(
            id=None,
            login="robot",
            uid=None,
            company_ids=[1],
            is_super=False,
            confirmed_at=datetime.now(),
            role=UserRole.app
        )

        _create_nodes(1, 1)

        courier = get_by_id_and_company_id(Courier, 1, 1)
        courier.phone = '+1234567890'

        company = get_by_id(LogisticCompany, 1)
        company.update({'send_navi_deeplinks': False})

        task = SendNaviDeeplinksTask(env.flask_app)
        task_state = {
            'now_timestamp': datetime(2021, 1, 1, tzinfo=MOSCOW_TZ)
        }
        result = task.run(task_state)
        assert not result['start_immediately']

        log = db.session.query(DeeplinkLog) \
            .filter(
                DeeplinkLog.courier_id == courier.id,
                DeeplinkLog.company_id == company.id
            ) \
            .one_or_none()
        assert log is None


@skip_if_remote
def test_do_not_send_sms_if_phone_is_not_set(env: Environment):
    with env.flask_app.app_context():
        g.user = UserAccount(
            id=None,
            login="robot",
            uid=None,
            company_ids=[1],
            is_super=False,
            confirmed_at=datetime.now(),
            role=UserRole.app
        )

        _create_nodes(1, 1)

        courier = get_by_id_and_company_id(Courier, 1, 1)

        company = get_by_id(LogisticCompany, 1)
        company.update({'send_navi_deeplinks': True})

        task = SendNaviDeeplinksTask(env.flask_app)
        task_state = {
            'now_timestamp': datetime(2021, 1, 1, tzinfo=MOSCOW_TZ)
        }
        result = task.run(task_state)
        assert not result['start_immediately']

        log = db.session.query(DeeplinkLog) \
            .filter(
                DeeplinkLog.courier_id == courier.id,
                DeeplinkLog.company_id == company.id
            ) \
            .one_or_none()
        assert log is None


@skip_if_remote
def test_do_not_send_sms_if_phone_is_invalid(env: Environment):
    with env.flask_app.app_context():
        g.user = UserAccount(
            id=None,
            login="robot",
            uid=None,
            company_ids=[1],
            is_super=False,
            confirmed_at=datetime.now(),
            role=UserRole.app
        )

        _create_nodes(1, 1)

        courier = get_by_id_and_company_id(Courier, 1, 1)
        courier.phone = 'asdf'

        company = get_by_id(LogisticCompany, 1)
        company.update({'send_navi_deeplinks': True})

        task = SendNaviDeeplinksTask(env.flask_app)
        task_state = {
            'now_timestamp': datetime(2021, 1, 1, tzinfo=MOSCOW_TZ)
        }
        result = task.run(task_state)
        assert not result['start_immediately']

        log = db.session.query(DeeplinkLog) \
            .filter(
                DeeplinkLog.courier_id == courier.id,
                DeeplinkLog.company_id == company.id
            ) \
            .one_or_none()
        assert log is None


@skip_if_remote
def test_do_not_send_sms_if_phone_is_empty_string(env: Environment):
    with env.flask_app.app_context():
        g.user = UserAccount(
            id=None,
            login="robot",
            uid=None,
            company_ids=[1],
            is_super=False,
            confirmed_at=datetime.now(),
            role=UserRole.app
        )

        _create_nodes(1, 1)

        courier = get_by_id_and_company_id(Courier, 1, 1)
        courier.phone = ''

        company = get_by_id(LogisticCompany, 1)
        company.update({'send_navi_deeplinks': True})

        task = SendNaviDeeplinksTask(env.flask_app)
        task_state = {
            'now_timestamp': datetime(2021, 1, 1, tzinfo=MOSCOW_TZ)
        }
        result = task.run(task_state)
        assert not result['start_immediately']

        log = db.session.query(DeeplinkLog) \
            .filter(
                DeeplinkLog.courier_id == courier.id,
                DeeplinkLog.company_id == company.id
            ) \
            .one_or_none()
        assert log is None


@skip_if_remote
def test_do_not_send_sms_if_route_is_empty(env: Environment):
    with env.flask_app.app_context():
        g.user = UserAccount(
            id=None,
            login="robot",
            uid=None,
            company_ids=[1],
            is_super=False,
            confirmed_at=datetime.now(),
            role=UserRole.app
        )

        courier = get_by_id_and_company_id(Courier, 1, 1)
        courier.phone = '+1234567890'

        company = get_by_id(LogisticCompany, 1)
        company.update({'send_navi_deeplinks': True})

        task = SendNaviDeeplinksTask(env.flask_app)
        task_state = {
            'now_timestamp': datetime(2021, 1, 1, tzinfo=MOSCOW_TZ)
        }
        result = task.run(task_state)
        assert not result['start_immediately']

        log = db.session.query(DeeplinkLog) \
            .filter(
                DeeplinkLog.courier_id == courier.id,
                DeeplinkLog.company_id == company.id
            ) \
            .one_or_none()
        assert log is None


@skip_if_remote
def test_do_not_send_sms_if_route_imei_is_set(env: Environment):
    with env.flask_app.app_context():
        g.user = UserAccount(
            id=None,
            login="robot",
            uid=None,
            company_ids=[1],
            is_super=False,
            confirmed_at=datetime.now(),
            role=UserRole.app
        )

        _create_nodes(1, 1)

        courier = get_by_id_and_company_id(Courier, 1, 1)
        courier.phone = '+1234567890'

        route = get_by_id_and_company_id(Route, 1, 1)
        route.imei = 123

        company = get_by_id(LogisticCompany, 1)
        company.update({'send_navi_deeplinks': True})

        task = SendNaviDeeplinksTask(env.flask_app)
        task_state = {
            'now_timestamp': datetime(2021, 1, 1, tzinfo=MOSCOW_TZ)
        }
        result = task.run(task_state)
        assert not result['start_immediately']

        log = db.session.query(DeeplinkLog) \
            .filter(
                DeeplinkLog.courier_id == courier.id,
                DeeplinkLog.company_id == company.id
            ) \
            .one_or_none()
        assert log is None


@skip_if_remote
def test_phone_normalizer(env: Environment):
    with env.flask_app.app_context():
        assert normalize_phone('+7 911 111-11-11') == '+79111111111'
        assert normalize_phone('+7(911)111-11-11') == '+79111111111'
        assert normalize_phone('8(911)111-11-11') == '+79111111111'
        assert normalize_phone('79111111111') == '+79111111111'

        assert normalize_phone('+375 24 111 11 11') == '+375241111111'
        assert normalize_phone('+375 (24) 111-11-11') == '+375241111111'
        assert normalize_phone('375241111111') == '+375241111111'

        assert normalize_phone('+1 212 555 4626') == '+12125554626'
        assert normalize_phone('+1 (212) 555-4626') == '+12125554626'
        assert normalize_phone('12125554626') == '+12125554626'

        with pytest.raises(IncorrectPhoneFormat) as e:
            normalize_phone('')
        assert "phone is empty" in str(e.value)

        with pytest.raises(IncorrectPhoneFormat) as e:
            normalize_phone('invalid string')
        assert "phone doesn't contain any digits" in str(e.value)

        with pytest.raises(IncorrectPhoneFormat) as e:
            normalize_phone('+79111111111;+79122222222')
        assert "phone number is too long" in str(e.value)
