# -*- coding: utf-8 -*-
import pytest
import datetime
import mock
import sqlalchemy as sa

from balance import mapper
from balance import constants as cst

from tests import object_builder as ob

from tests.balance_tests.promocode.common import (
    NOW,
    create_promocode,
    create_client,
    reserve_promocode,
    create_order,
    create_invoice,
)


@pytest.fixture(scope='module', autouse=True)
def mock_secrets_storage():
    with mock.patch('butils.application.secret.load_secret_from_file') as m:
        m.return_value = 'XXX'
        yield


@pytest.fixture(autouse=True)
def mock_rates():
    def patch_func(cls, dt, from_currency, to_currency, **kwargs):
        if isinstance(from_currency, basestring) and isinstance(to_currency, basestring):
            return {
                ('USD', 'RUB'): 70
            }[(from_currency, to_currency)]
        else:
            return sa.case(
                [(from_currency == to_currency, sa.text('1'))],
                else_=sa.text('2'),  # псевдо курс на любые сочетания валют
            )

    patcher = mock.patch('balance.mapper.common.CurrencyRate.get_cross_rate', classmethod(patch_func))
    patcher.start()
    yield
    patcher.stop()


@pytest.fixture()
def pc_params():
    return {
        'start_dt': NOW,
        'end_dt': NOW + datetime.timedelta(days=10),
    }


@pytest.fixture()
def promocode(request, session):
    return create_promocode(session, getattr(request, 'param', None))


@pytest.fixture()
def client(session):
    return create_client(session)


@pytest.fixture()
def agency(session):
    return ob.ClientBuilder(is_agency=True).build(session).obj


@pytest.fixture()
def reservation(session, promocode, client):
    return reserve_promocode(session, promocode, client)


@pytest.fixture
def order(request, session, client):
    product_id = getattr(request, 'param', cst.DIRECT_PRODUCT_RUB_ID)
    return create_order(session, client, product_id)


@pytest.fixture
def invoice(request, session, client, order):
    base_params = {
        'qty': 1000,
        'paysys_cc': 'ph',
        'discount_pct': None,
        'adjust_qty': False,
    }
    params = getattr(request, 'param', {})
    base_params.update(params)

    paysys = session.query(mapper.Paysys).getone(firm_id=cst.FirmId.YANDEX_OOO, cc=base_params['paysys_cc'])
    person = ob.PersonBuilder(client=order.client, type=paysys.category)

    invoice = create_invoice(
        session,
        base_params['qty'],
        client,
        order,
        paysys, person,
        base_params['discount_pct'], base_params['adjust_qty']
    )
    invoice.create_receipt(invoice.effective_sum)
    return invoice
