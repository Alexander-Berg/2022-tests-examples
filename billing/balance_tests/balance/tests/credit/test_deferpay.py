from datetime import datetime, timedelta
from decimal import Decimal as D

import pytest
from hamcrest import has_entries

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


@pytest.mark.smoke
@pytest.mark.parametrize('PERSONAL_ACCOUNT_FICTIVE_VALUE',
                         [0, 1]
                         )
def test_deferpay_creation(PERSONAL_ACCOUNT_FICTIVE_VALUE):
    PERSON_TYPE = 'ur'
    PAYSYS_ID = 1003
    SERVICE_ID = 7
    PRODUCT_ID = 1475
    MSR = 'Bucks'
    contract_type = 'opt_agency_post'

    client_id = steps.ClientSteps.create({'IS_AGENCY': 1})
    person_id = steps.PersonSteps.create(client_id, PERSON_TYPE)

    service_order_id = steps.OrderSteps.next_id(service_id=SERVICE_ID)
    order_id = steps.OrderSteps.create(client_id=client_id, product_id=PRODUCT_ID, service_id=SERVICE_ID,
                                       service_order_id=service_order_id)
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

    deferpay = db.get_deferpays_by_invoice(invoice_id)[0]
    consume_sum = db.get_invoice_by_id(invoice_id)[0]['consume_sum']
    deferpay_params = {}
    deferpay_params.update({'paysys_id': PAYSYS_ID,
                            'orig_request_id': D(request_id),
                            'invoice_id': invoice_id,
                            'effective_sum': consume_sum,
                            'client_id': client_id,
                            'request_id': request_id,
                            'person_id': person_id})
    utils.check_that(deferpay, has_entries(deferpay_params))
