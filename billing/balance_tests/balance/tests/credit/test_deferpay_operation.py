from datetime import datetime, timedelta

import pytest
from hamcrest import not_none, equal_to, none

import balance.balance_db as db
import btestlib.reporter as reporter
import btestlib.utils as utils
from balance import balance_steps as steps
from balance.features import Features

pytestmark = [pytest.mark.priority('mid')
    , reporter.feature(Features.DEFERPAY, Features.CREDIT, Features.INVOICE)
              ]

to_iso = utils.Date.date_to_iso_format
NOW = datetime.now()
NOW_ISO = to_iso(NOW)
HALF_YEAR_AFTER_NOW_ISO = to_iso(NOW + timedelta(days=180))
HALF_YEAR_BEFORE_NOW_ISO = to_iso(NOW - timedelta(days=180))

deferpay_params = {}


@pytest.mark.smoke
@pytest.mark.parametrize('PERSONAL_ACCOUNT_FICTIVE_VALUE',
                         [0, 1]
                         )
def test_deferpay_operation(PERSONAL_ACCOUNT_FICTIVE_VALUE):
    PERSON_TYPE = 'ur'
    PAYSYS_ID = 1003
    SERVICE_ID = 7
    PRODUCT_ID = 1475
    contract_type = 'opt_agency_post'

    client_id = steps.ClientSteps.create({'IS_AGENCY': 1})
    person_id = steps.PersonSteps.create(client_id, PERSON_TYPE)

    service_order_id = steps.OrderSteps.next_id(service_id=SERVICE_ID)
    steps.OrderSteps.create(client_id=client_id, product_id=PRODUCT_ID, service_id=SERVICE_ID,
                            service_order_id=service_order_id)
    service_order_id2 = steps.OrderSteps.next_id(service_id=SERVICE_ID)
    steps.OrderSteps.create(client_id=client_id, product_id=PRODUCT_ID, service_id=SERVICE_ID,
                            service_order_id=service_order_id2)
    orders_list = [
        {'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': 200, 'BeginDT': NOW}
    ]
    request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list)

    contract_id, _ = steps.ContractSteps.create_contract(contract_type, {'CLIENT_ID': client_id, 'PERSON_ID': person_id,
                                                                         'DT': HALF_YEAR_BEFORE_NOW_ISO,
                                                                         'FINISH_DT': HALF_YEAR_AFTER_NOW_ISO,
                                                                         'IS_SIGNED': HALF_YEAR_BEFORE_NOW_ISO,
                                                                         'SERVICES': [7],
                                                                         'FIRM': '1',
                                                                         'PERSONAL_ACCOUNT_FICTIVE': PERSONAL_ACCOUNT_FICTIVE_VALUE,
                                                                         'DISCOUNT_POLICY_TYPE': 0})

    invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=PAYSYS_ID,
                                                 credit=1, contract_id=contract_id, overdraft=0, endbuyer_id=None)
    # make negative reverse
    request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list)
    invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=PAYSYS_ID,
                                                 credit=1, contract_id=contract_id, overdraft=0, endbuyer_id=None)
    operations_list = []
    # collect operation from deferpay
    deferpays = db.get_deferpays_by_invoice(invoice_id)
    deferpay_operations_ids = [deferpay['operation_id'] for deferpay in deferpays]
    for operation_id in deferpay_operations_ids:
        operations_list.append(db.get_operation_by_id(operation_id)[0])
    # collect operation from consume
    consume = db.get_consumes_by_invoice(invoice_id)[0]
    consume_operation_id = consume['operation_id']
    operations_list.append(db.get_operation_by_id(consume_operation_id)[0])
    # collect operation from reverse
    reverse = db.get_reverse_by_invoice(invoice_id)[0]
    reverse_operation_id = reverse['operation_id']
    operations_list.append(db.get_operation_by_id(reverse_operation_id)[0])
    for operation in operations_list:
        utils.check_that(operation['insert_traceback_id'], none())
        utils.check_that(operation['create_traceback_id'], not_none())
        utils.check_that(operation['passport_id'], not_none())
        utils.check_that(operation['type_id'], equal_to(10002))
        utils.check_that(operation['parent_operation_id'], none())
        utils.check_that(operation['invoice_id'], invoice_id)
