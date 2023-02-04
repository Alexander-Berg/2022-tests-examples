import pytest
from decimal import Decimal as D
from mock import patch

from balance.mapper import Paysys
from balance import exc
from butils.decimal_unit import DecimalUnit

from tests.balance_tests.invoices.invoice_common import create_invoice
from tests import object_builder as ob


def test_paysys_wo_limit(session):
    invoice = create_invoice(session, 666666666666, 1001)
    assert invoice.paysys.first_limit is None
    assert bool(invoice.is_amount_too_big()) is False


@pytest.mark.parametrize('reliable_cc_payer, amount, is_amount_too_big',
                         [(False, D('49999.99'), False),
                          (False, D('50000'), True),
                          (True, D('249999.99'), False),
                          (True, D('250000'), True)])
def test_w_limit(session, reliable_cc_payer, amount, is_amount_too_big):
    invoice = create_invoice(session, amount, 1002)
    invoice.client.reliable_cc_payer = reliable_cc_payer
    invoice.session.flush()
    assert invoice.paysys.first_limit == DecimalUnit('49999.99', 'RUB')
    assert invoice.paysys.second_limit == DecimalUnit('249999.99', 'RUB')
    assert bool(invoice.is_amount_too_big()) is is_amount_too_big


def test_force_paysys(session):
    invoice = create_invoice(session, 50000, 1001)
    paysys = ob.Getter(Paysys, 1002).build(invoice.session).obj
    assert bool(invoice.is_amount_too_big(paysys=paysys)) is True


@pytest.mark.parametrize('force_paysys_limit, is_amount_too_big',
                         [(D('9999.99'), True),
                          (D('10000'), False)])
def test_force_limit(session, force_paysys_limit, is_amount_too_big):
    invoice = create_invoice(session, 10000, 1002)
    assert bool(invoice.is_amount_too_big(paysys_limit=force_paysys_limit)) is is_amount_too_big


def test_w_exception(session):
    invoice = create_invoice(session, 50000, 1002)
    with pytest.raises(exc.PAYSYS_LIMIT_EXCEEDED):
        invoice.is_amount_too_big(raise_exception=True)


@pytest.mark.parametrize('amount, is_amount_too_big',
                         [(D('666'), False),
                          (D('667'), True)])
def test_trust_api_paysys(session, amount, is_amount_too_big):
    invoice = create_invoice(session, amount, 106430012015052)
    with patch('balance.trust_api.actions.get_payment_methods', return_value=[{'payment_method': 'yamoney_wallet',
                                                                               'currency': 'RUB',
                                                                               'firm_id': 1,
                                                                               'max_amount': 666}]):
        assert invoice.is_amount_too_big() is is_amount_too_big


def test_trust_api_paysys_wo_paymethod(session):
    invoice = create_invoice(session, D('55555555555'), 106430012015052)
    with patch('balance.trust_api.actions.get_payment_methods', return_value=[]):
        assert invoice.is_amount_too_big() is None


@pytest.mark.parametrize('amount, is_amount_too_big',
                         [(D('666'), False),
                          (D('667'), True)])
def test_big_terminals_paysys(session, amount, is_amount_too_big):
    invoice = create_invoice(session, amount, 1000)
    session.config.__dict__['PAYSYS_TERMINAL_LIMITS_PAYMETHODS'] = [[7, 1201]]

    with patch('balance.trust_api.actions.get_terminals_limit', return_value=DecimalUnit('666', 'RUB')):
        assert invoice.is_amount_too_big() is is_amount_too_big
