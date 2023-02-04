# -*- coding: utf-8 -*-
import datetime
import pytest
import hamcrest as hm

from billing.contract_iface.constants import ContractTypeId

from balance.paystep import get_payment_preview
from balance import mapper
from balance.actions import single_account
from balance.constants import ServiceId, FirmId, PREPAY_PAYMENT_TYPE, POSTPAY_PAYMENT_TYPE, TransferMode

from tests.balance_tests.paystep.paystep_common import (
    create_client,
    create_order,
    create_contract,
    create_request,
    create_product,
    create_person,
    create_price_tax_rate,
)
from tests.object_builder import Getter

pytestmark = [
    pytest.mark.paystep,
    pytest.mark.usefixtures('switch_new_paystep_flag'),
]


NOW = datetime.datetime.now()


def test_get_payment_preview_overdraft_real_case(session):
    client = create_client(session, region_id=225)
    person_1 = create_person(session, client=client, type='ur')
    person_2 = create_person(session, client=client, type='ur')
    client.set_overdraft_limit(ServiceId.DIRECT, FirmId.YANDEX_OOO, 333, None)

    order = create_order(session, client=client,
                         service=Getter(mapper.Service, ServiceId.DIRECT),
                         product=create_product(session, create_taxes=True, create_price=True,
                                                reference_price_currency='RUR'))
    request_ = create_request(session, client=client, orders=[order])
    paysys = Getter(mapper.Paysys, 1003).build(session).obj

    contract = create_contract(session, commission=ContractTypeId.NON_AGENCY, firm=FirmId.YANDEX_OOO,
                               client=client, payment_type=PREPAY_PAYMENT_TYPE,
                               services={ServiceId.DIRECT}, is_signed=NOW, person=person_1,
                               currency=810)

    pp_info_1 = get_payment_preview(request_, person_1, contract, paysys)
    assert pp_info_1.overdrafts == dict()
    pp_info_2 = get_payment_preview(request_, person_2, None, paysys)
    assert pp_info_2.overdrafts['is_available'] == 1


@pytest.mark.parametrize('config', [1, 0])
def test_get_payment_preview_single_account_real_case(session, config):
    session.config.__dict__['SINGLE_ACCOUNT_PAYSTEP_OVERDRAFT_ENABLED'] = config
    client = create_client(session, region_id=225, with_single_account=True)
    person = create_person(session, client=client, type='ur')
    single_account.prepare.process_client(client)

    client.set_overdraft_limit(ServiceId.DIRECT, FirmId.YANDEX_OOO, 333, None)

    order = create_order(session, client=client,
                         service=Getter(mapper.Service, ServiceId.DIRECT),
                         product=create_product(session, create_taxes=True, create_price=True,
                                                reference_price_currency='RUR'))
    request_ = create_request(session, client=client, orders=[order])
    paysys = Getter(mapper.Paysys, 1003).build(session).obj

    pp_info = get_payment_preview(request_, person, None, paysys)
    if config:
        assert pp_info.overdrafts['is_available'] == 1
    else:
        assert pp_info.overdrafts == dict()


@pytest.mark.parametrize(
    'w_contract',
    [False, True],
)
def test_ns_contract_condition(session, w_contract):
    client = create_client(session, region_id=225)
    client.force_contractless_invoice = True
    person = create_person(session, client=client, type='ph')
    order = create_order(session, client=client, service=Getter(mapper.Service, ServiceId.DIRECT))
    request_ = create_request(session, client=client, orders=[order])
    contract = create_contract(session, commission=ContractTypeId.NON_AGENCY, firm=FirmId.YANDEX_OOO,
                               client=client, payment_type=POSTPAY_PAYMENT_TYPE,
                               services={ServiceId.DIRECT}, is_signed=NOW, person=person,
                               currency=810)
    paysys = Getter(mapper.Paysys, 1001).build(session).obj
    currency = session.query(mapper.Currency).filter_by(iso_code='RUB').one()
    create_price_tax_rate(session, order.product, client.country, currency, price=1)

    pp_info = get_payment_preview(request_, person, contract if w_contract else None, paysys)
    assert pp_info.fake_invoice
    hm.assert_that(
        pp_info.fake_invoice,
        hm.has_properties(
            request=request_,
            person=person,
            paysys=paysys,
            contract=contract if w_contract else None,
        ),
    )
