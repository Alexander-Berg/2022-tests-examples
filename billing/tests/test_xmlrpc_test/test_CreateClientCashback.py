# coding=utf-8

import datetime
from decimal import Decimal

import pytest
from dateutil.relativedelta import relativedelta
import sqlalchemy as sa

import butils.datetools
from balance import mapper
from tests import object_builder as ob


_CLIENT_ID = 123
_SERVICE_ID = 987
_NOW = butils.datetools.trunc_date(datetime.datetime.now())


@pytest.fixture(autouse=True)
def prepare_db(session):
    ob.ClientBuilder(id=_CLIENT_ID).build(session)
    ob.ServiceBuilder(id=_SERVICE_ID).build(session)


@pytest.fixture(params=[_NOW, None])
def existing_cashback(session, request):
    return ob.ClientCashbackBuilder.construct(
        session,
        client_id=_CLIENT_ID,
        service_id=_SERVICE_ID,
        iso_currency="RUB",
        bonus=Decimal("100"),
        start_dt=request.param,
        finish_dt=request.param and (request.param + relativedelta(months=3))
    )


@pytest.fixture(params=[True, False])
def cashback_settings(session, request):
    return ob.CashbackSettingsBuilder.construct(
        session,
        client_id=_CLIENT_ID,
        service_id=_SERVICE_ID,
        is_auto_charge_enabled=request.param,
    )


@pytest.mark.parametrize("cashback_mtl", [3, None])
def test_creates_client_cashback_if_new(session, test_xmlrpc_srv, cashback_mtl):
    test_xmlrpc_srv.CreateClientCashback({
        "client_id": _CLIENT_ID,
        "service_id": _SERVICE_ID,
        "currency": "RUB",
        "reward": "123.5",
        "cashback_months_to_live": cashback_mtl,
    })

    if cashback_mtl is not None:
        dt_filters = (
            mapper.ClientCashback.start_dt == _NOW,
            mapper.ClientCashback.finish_dt == (_NOW + relativedelta(months=cashback_mtl)),
        )
    else:
        # Плейсхолдерные даты, потому что оракл не умеет в сравнения NULLов
        placeholder_dt = datetime.datetime(1, 1, 1)
        dt_filters = (
            sa.func.nvl(mapper.ClientCashback.start_dt, placeholder_dt) == placeholder_dt,
            sa.func.nvl(mapper.ClientCashback.finish_dt, placeholder_dt) == placeholder_dt,
        )

    created_cashback = (
        session.query(mapper.ClientCashback)
        .filter(
            mapper.ClientCashback.client_id == _CLIENT_ID,
            mapper.ClientCashback.service_id == _SERVICE_ID,
            mapper.ClientCashback.iso_currency == "RUB",
            *dt_filters
        )
        .one()
    )

    assert created_cashback.bonus == Decimal("123.5")


def test_updates_cashback_if_exists(session, test_xmlrpc_srv, existing_cashback):
    cashback_mtl = 3 if existing_cashback.finish_dt else None

    test_xmlrpc_srv.CreateClientCashback({
        "client_id": _CLIENT_ID,
        "service_id": _SERVICE_ID,
        "currency": "RUB",
        "reward": "123.5",
        "cashback_months_to_live": cashback_mtl,
    })

    if cashback_mtl is not None:
        dt_filters = (
            mapper.ClientCashback.start_dt == _NOW,
            mapper.ClientCashback.finish_dt == (_NOW + relativedelta(months=cashback_mtl)),
        )
    else:
        # Плейсхолдерные даты, потому что оракл не умеет в сравнения NULLов
        placeholder_dt = datetime.datetime(1, 1, 1)
        dt_filters = (
            sa.func.nvl(mapper.ClientCashback.start_dt, placeholder_dt) == placeholder_dt,
            sa.func.nvl(mapper.ClientCashback.finish_dt, placeholder_dt) == placeholder_dt,
        )

    updated_cashback = (
        session.query(mapper.ClientCashback)
        .filter(
            mapper.ClientCashback.client_id == _CLIENT_ID,
            mapper.ClientCashback.service_id == _SERVICE_ID,
            mapper.ClientCashback.iso_currency == "RUB",
            *dt_filters
        )
        .one()
    )

    assert updated_cashback.bonus == Decimal("223.5")


def test_creates_cashback_settings_if_new(session, test_xmlrpc_srv):
    test_xmlrpc_srv.CreateClientCashback({
        "client_id": _CLIENT_ID,
        "service_id": _SERVICE_ID,
        "currency": "RUB",
        "reward": "123.5",
        "cashback_months_to_live": None,
        "auto_consume_enabled": True,
    })

    created_settings = session.query(mapper.ClientCashbackSettings).filter(
        mapper.ClientCashbackSettings.client_id == _CLIENT_ID,
        mapper.ClientCashbackSettings.service_id == _SERVICE_ID,
    ).one()
    updated_client = session.query(mapper.Client).filter_by(id=_CLIENT_ID).one()

    assert created_settings.is_auto_charge_enabled
    assert updated_client.cashback_settings.get(_SERVICE_ID) == created_settings


def test_does_not_create_cashback_settings_if_auto_consume_is_false(session, test_xmlrpc_srv):
    test_xmlrpc_srv.CreateClientCashback({
        "client_id": _CLIENT_ID,
        "service_id": _SERVICE_ID,
        "currency": "RUB",
        "reward": "123.5",
        "cashback_months_to_live": None,
        "auto_consume_enabled": False,
    })

    created_settings = session.query(mapper.ClientCashbackSettings).filter(
        mapper.ClientCashbackSettings.client_id == _CLIENT_ID,
        mapper.ClientCashbackSettings.service_id == _SERVICE_ID,
    ).first()
    updated_client = session.query(mapper.Client).filter_by(id=_CLIENT_ID).one()

    assert created_settings is None
    assert updated_client.cashback_settings.get(_SERVICE_ID) is None


def test_updates_cashback_settings_if_exist(session, test_xmlrpc_srv, cashback_settings):
    test_xmlrpc_srv.CreateClientCashback({
        "client_id": _CLIENT_ID,
        "service_id": _SERVICE_ID,
        "currency": "RUB",
        "reward": "123.5",
        "cashback_months_to_live": None,
        "auto_consume_enabled": True,
    })

    updated_settings = session.query(mapper.ClientCashbackSettings).filter(
        mapper.ClientCashbackSettings.client_id == _CLIENT_ID,
        mapper.ClientCashbackSettings.service_id == _SERVICE_ID,
    ).one()

    assert updated_settings.is_auto_charge_enabled


def test_returns_updated_cashback_data(session, test_xmlrpc_srv):
    # other cashback
    ob.ClientCashbackBuilder.construct(
        session,
        client_id=_CLIENT_ID,
        service_id=_SERVICE_ID,
        iso_currency="RUB",
        bonus=Decimal("100"),
        start_dt=_NOW - relativedelta(months=1),
        finish_dt=_NOW + relativedelta(months=2),
    )

    result = test_xmlrpc_srv.CreateClientCashback({
        "client_id": _CLIENT_ID,
        "service_id": _SERVICE_ID,
        "currency": "RUB",
        "reward": "123.5",
        "cashback_months_to_live": 3,
        "auto_consume_enabled": True,
    })

    assert result == {
        "client_id": _CLIENT_ID,
        "service_id": _SERVICE_ID,
        "auto_consume_enabled": True,
        "cashbacks": [
            {
                "currency": "RUB",
                "reward": "123.5",
                "start_dt": _NOW,
                "finish_dt": _NOW + relativedelta(months=3),
            }, {
                "currency": "RUB",
                "reward": "100",
                "start_dt": _NOW - relativedelta(months=1),
                "finish_dt": _NOW + relativedelta(months=2),
            }
        ]
    }
