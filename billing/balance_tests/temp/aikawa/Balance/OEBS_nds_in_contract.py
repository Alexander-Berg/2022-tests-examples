import datetime

import pytest

import btestlib.balance_db as db
from btestlib import balance_matchers as mtch
from btestlib import balance_steps as steps
from btestlib import utils as ut

dt = datetime.datetime.now().replace(microsecond=0).isoformat()
SERVICE_ID = 7
PRODUCT_ID = 1475
MSR = 'Bucks'

contract_params_list = [
    {'contract_type': 'comm_pre', 'PERSON_TYPE': 'ur', 'PAYSYS_ID': 1003}
    # {'contract_type': 'ukr_opt_ag_prem', 'PERSON_TYPE': 'ua', 'PAYSYS_ID': 1017}
    # ,{'contract_type': 'ukr_comm', 'PERSON_TYPE': 'ua', 'PAYSYS_ID': 1017}
    ]


def check_retrodiscount_export(contract_params_list, DISCARD_NDS):
    client_id = steps.ClientSteps.create({'IS_AGENCY': '1'})
    person_id = steps.PersonSteps.create(client_id, contract_params_list['PERSON_TYPE'])
    service_order_id = steps.OrderSteps.next_id(service_id=SERVICE_ID)
    steps.OrderSteps.create(client_id=client_id, product_id=PRODUCT_ID, service_id=SERVICE_ID, service_order_id=service_order_id)
    orders_list = [
        {'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': 200, 'BeginDT': dt}
    ]
    contract_params_dict = {
        'client_id': client_id,
        'person_id': person_id,
        'dt': '2015-04-30T00:00:00',
        'FINISH_DT': '2016-06-30T00:00:00',
        'is_signed': '2015-01-01T00:00:00',
        'SERVICES': [7],
        'FIRM': 1,
        'DISCARD_NDS': DISCARD_NDS
    }
    print contract_params_list['contract_type']
    contract_id, _ = steps.ContractSteps.create(contract_params_list['contract_type'], contract_params_dict)

    request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list, additional_params=dict(InvoiceDesireDT=dt))
    invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=contract_params_list['PAYSYS_ID'],
                                                 credit=0, contract_id=contract_id, overdraft=0, endbuyer_id=None)
    steps.InvoiceSteps.pay(invoice_id)
    steps.CampaignsSteps.do_campaigns(SERVICE_ID, service_order_id, {'Bucks': 200}, 0, dt)
    return contract_id


@pytest.mark.parametrize('contract_params_list', contract_params_list)
@pytest.mark.parametrize('DISCARD_NDS', ['0', '1'])
def test_check_retrodiscount_export(contract_params_list, DISCARD_NDS):
    contract_id = check_retrodiscount_export(contract_params_list, DISCARD_NDS)
    assert contract_id == 0


if __name__ == "__main__":
    pytest.main("OEBS_nds_in_contract.py -vk 'test_check_retrodiscount_export'")
