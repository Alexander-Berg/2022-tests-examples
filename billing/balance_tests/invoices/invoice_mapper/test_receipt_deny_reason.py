# -*- coding: utf-8 -*-

import pytest
import re

from balance import constants as cst
from balance import mapper

from tests import object_builder as ob
from tests.balance_tests.invoices.invoice_common import create_invoice
from tests.balance_tests.pay_policy.pay_policy_common import create_pay_policy

PAYSYS_ID = 1000


def create_service(session, fiscal_balance_allowed):
    service = ob.ServiceBuilder.construct(session)
    service.fiscal_service.balance_allowed = fiscal_balance_allowed
    create_pay_policy(
        session, firm_id=cst.FirmId.YANDEX_OOO, region_id=cst.RegionId.RUSSIA, service_id=service.id,
        paymethods_params=[('RUB', 1001)]
    )
    session.flush()
    return service


@pytest.mark.parametrize(
    'fiscal_balance_allowed',
    [0, 1]
)
def test_one_service(session, fiscal_balance_allowed):
    service = create_service(session, fiscal_balance_allowed)
    invoice = create_invoice(session, paysys_id=PAYSYS_ID, service_id=service.id)

    if fiscal_balance_allowed:
        assert invoice.receipt_deny_reason is None
    else:
        assert invoice.receipt_deny_reason == 'Not allowed service {}'.format({service.id})


@pytest.mark.parametrize(
    'fiscal_balance_allowed_1, fiscal_balance_allowed_2',
    [(0, 0), (1, 0), (1, 1)]
)
def test_two_services(session, fiscal_balance_allowed_1, fiscal_balance_allowed_2):
    service_1 = create_service(session, fiscal_balance_allowed_1)
    service_2 = create_service(session, fiscal_balance_allowed_2)

    client = ob.ClientBuilder.construct(session)
    paysys = ob.Getter(mapper.Paysys, PAYSYS_ID).build(session).obj
    person = ob.PersonBuilder.construct(session, client=client, type=paysys.category)

    order_1 = ob.OrderBuilder(client=client, product_id=cst.DIRECT_PRODUCT_ID, service_id=service_1.id)
    order_2 = ob.OrderBuilder(client=client, product_id=cst.DIRECT_PRODUCT_ID, service_id=service_2.id)

    invoice = create_invoice(session, client=client, person=person, paysys_id=PAYSYS_ID,
                             orders=[(order_1, 100), (order_2, 100)])

    if fiscal_balance_allowed_1 or fiscal_balance_allowed_2:
        assert invoice.receipt_deny_reason is None
    else:
        service_1_id, service_2_id = re.findall(r'Not allowed service set\(\[(\d+), (\d+)\]\)',
                                                invoice.receipt_deny_reason)[0]
        assert {int(service_1_id), int(service_2_id)} == {service_1.id, service_2.id}
