import pytest
from mock import MagicMock
from tests import object_builder as ob
from balance.constants import PaysysGroupIDs, PaymentMethodIDs
from balance.mapper import Service, PaymentMethod
import balance.trust_api.actions as trust_actions


def create_paysys(session, **kwargs):
    return ob.PaysysBuilder(**kwargs).build(session).obj


def create_firm(session, **kwargs):
    return ob.FirmBuilder(**kwargs).build(session).obj


def create_currency(session, **kwargs):
    currency = ob.CurrencyBuilder(**kwargs).build(session).obj
    currency.iso_code = currency.char_code
    return currency


def test_paysys_get_trust_paymethods(session):
    currency = create_currency(session)
    payment_method = ob.Getter(PaymentMethod, PaymentMethodIDs.bank).build(session).obj
    paysys = create_paysys(session, group_id=PaysysGroupIDs.auto_trust, payment_method=payment_method,
                           firm=create_firm(session), currency=currency.char_code, iso_currency=currency.iso_code)
    trust_actions.get_payment_methods = MagicMock(return_value=[{'firm_id': paysys.firm.id,
                                                                 'payment_method': paysys.payment_method.cc,
                                                                 'currency': paysys.currency_mapper.char_code}])
    service = ob.Getter(Service, 7).build(session).obj
    result = paysys.get_trust_paymethods(service=service)
    assert result == [{'currency': currency.char_code,
                       'firm_id': paysys.firm.id,
                       'payment_method': paysys.payment_method.cc}]
