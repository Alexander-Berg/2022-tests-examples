# -*- coding: utf-8 -*-
import pytest

from balance import constants as cst
from balance.default_bank_rules import get_invoice_default_bank


class SubClass(object):

    def __init__(self, **kwargs):
        self.__dict__.update(kwargs)

    def __getattr__(self, key):
        if key in self.__dict__:
            return super(SubClass, self).__getattr__(key)

    def __repr__(self):
        return str(self.__dict__)


class MockInvoice(object):
    iso_currency = 'RUB'
    resident = False
    person = None
    firm = SubClass(id=cst.FirmId.YANDEX_OOO)
    paysys = SubClass(nds=18)
    service = SubClass(id=cst.ServiceId.DIRECT)
    is_alien_repr = False

    def __init__(self, **kwargs):
        self.__dict__.update(kwargs)

    def __repr__(self):
        return 'Invoice(currency={s.iso_currency}, ' \
               'resident={s.resident}, ' \
               'person={s.person}, ' \
               'firm={s.firm}, ' \
               'paysys={s.paysys}, ' \
               'service={s.service}, ' \
               'is_alien_repr={s.is_alien_repr})'.format(s=self)


@pytest.mark.parametrize('bank_id, invoice', [
    ({10303: None}, MockInvoice(firm=SubClass(id=cst.FirmId.TAXI_KZ))),
    ({10303: None}, MockInvoice(firm=SubClass(id=cst.FirmId.YANDEX_KZ))),
    ({10303: None}, MockInvoice(firm=SubClass(id=cst.FirmId.YANDEX_DELIVERY_KZ))),
    ({10303: None}, MockInvoice(firm=SubClass(id=cst.FirmId.ZAPRAVKI_KZ))),
    ({10303: None}, MockInvoice(firm=SubClass(id=cst.FirmId.CLOUD_KZ))),
    ({10303: None}, MockInvoice(firm=SubClass(id=cst.FirmId.UBER_KZ))),
    ({2128: None}, MockInvoice(firm=SubClass(id=cst.FirmId.YANDEX_EU_AG))),
    ({2131: None}, MockInvoice(firm=SubClass(id=cst.FirmId.YANDEX_INC))),
    ({2004: None}, MockInvoice(firm=SubClass(id=cst.FirmId.YANDEX_UA))),
    ({2129: None}, MockInvoice(firm=SubClass(id=cst.FirmId.SERVICES_EU_AG))),
    ({2103: None}, MockInvoice(firm=SubClass(id=cst.FirmId.AUTORU))),
    ({2018: None}, MockInvoice(firm=SubClass(id=cst.FirmId.MARKET),
                               iso_currency='RUB')),
    ({2117: None}, MockInvoice(firm=SubClass(id=cst.FirmId.MARKET),
                               iso_currency='USD')),
    ({2117: None}, MockInvoice(firm=SubClass(id=cst.FirmId.MARKET),
                               iso_currency='EUR')),
    ({2117: None}, MockInvoice(firm=SubClass(id=cst.FirmId.MARKET),
                               iso_currency='BYN')),
    ({2117: None}, MockInvoice(firm=SubClass(id=cst.FirmId.MARKET),
                               iso_currency='KZT')),
    ({2020: None}, MockInvoice(firm=SubClass(id=cst.FirmId.TAXI))),
    ({2003: None}, MockInvoice(service=SubClass(id=cst.ServiceId.YANDEX_MEDIANA),
                               firm=SubClass(id=cst.FirmId.YANDEX_OOO),
                               person=SubClass(type='ph'))),
    ({2003: None}, MockInvoice(service=SubClass(id=cst.ServiceId.ADFOX),
                               firm=SubClass(id=cst.FirmId.YANDEX_OOO),
                               person=SubClass(type='ph'),
                               resident=1)),
    ({2002: None}, MockInvoice(service=SubClass(id=cst.ServiceId.ADFOX),
                               firm=SubClass(id=cst.FirmId.YANDEX_OOO),
                               person=SubClass(type='ph'),
                               resident=0)),
    ({2002: None}, MockInvoice(firm=SubClass(id=cst.FirmId.YANDEX_OOO),
                               person=SubClass(type='ph'),
                               iso_currency='USD')),
    ({2003: None}, MockInvoice(firm=SubClass(id=cst.FirmId.YANDEX_OOO),
                               person=SubClass(type='ph'),
                               iso_currency='EUR')),
    ({2002: None}, MockInvoice(firm=SubClass(id=cst.FirmId.YANDEX_OOO),
                               person=SubClass(type='ph'),
                               iso_currency='BYR')),
    ({2002: None}, MockInvoice(firm=SubClass(id=cst.FirmId.YANDEX_OOO), paysys=SubClass(nds=0))),
    ({2002: None}, MockInvoice(firm=SubClass(id=cst.FirmId.YANDEX_OOO), person=SubClass(is_alien_repr=True))),
    ({2002: 20, 2003: 30, 2007: 50}, MockInvoice(firm=SubClass(id=cst.FirmId.YANDEX_OOO), person=SubClass(type='ur'))),
    ({2001: None}, MockInvoice(firm=SubClass(id=cst.FirmId.YANDEX_OOO), person=SubClass(type='ph'))),
    ({2002: None}, MockInvoice(service=SubClass(id=cst.ServiceId.GEOCON), firm=SubClass(id=cst.FirmId.YANDEX_OOO))),
    ({2015: 50, 2023: 50}, MockInvoice(firm=SubClass(id=cst.FirmId.VERTIKALI)))
], ids=lambda v: str(v))
def test_default_bank_rules(bank_id, invoice):
    assert bank_id == get_invoice_default_bank(invoice)
