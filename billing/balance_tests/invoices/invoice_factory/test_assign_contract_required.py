import pytest
import datetime

from balance.actions.invoice_create import InvoiceFactory
from billing.contract_iface import ContractTypeId
from balance.constants import PREPAY_PAYMENT_TYPE
from invoice_factory_common import (create_client, create_request, create_person,
                                    create_paysys, create_contract, create_service,
                                    create_currency, create_firm, create_order)

NOW = datetime.datetime.now()


def test_wo_contract():
    i_f = InvoiceFactory()
    assert i_f.assign_contract_required is False


@pytest.mark.parametrize('is_signed', [NOW, None])
def test_w_signed_contract(session, client, firm, service, currency, is_signed):
    person = create_person(session, client=client)
    contract = create_contract(session, commission=ContractTypeId.NON_AGENCY, firm=firm.id,
                               client=client, payment_type=PREPAY_PAYMENT_TYPE, services={service.id},
                               is_signed=is_signed, person=person, currency=currency.num_code,
                               non_resident_clients=1)
    i_f = InvoiceFactory(credit=2, contract=contract)
    if is_signed:
        assert i_f.assign_contract_required is True
    else:
        assert i_f.assign_contract_required is False


def test_y_invoice(session, client, firm, service, currency):
    person = create_person(session, client=client)
    contract = create_contract(session, commission=ContractTypeId.NON_AGENCY, firm=firm.id,
                               client=client, payment_type=PREPAY_PAYMENT_TYPE, services={service.id},
                               is_signed=NOW, person=person, currency=currency.num_code,
                               non_resident_clients=1)
    i_f = InvoiceFactory(contract=contract, requested_type='y_invoice')

    assert i_f.assign_contract_required is False
