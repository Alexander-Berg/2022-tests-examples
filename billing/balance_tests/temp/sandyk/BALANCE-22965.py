__author__ = 'sandyk'

import datetime

import pytest

import btestlib.reporter as reporter
from balance import balance_db as db
from balance import balance_steps as steps
from balance import balance_web as web
from balance.features import Features


SERVICE_ID = 70
LOGIN = 'clientuid45'
PRODUCT_ID = 505039
PRODUCT_ID1 = 503328

# 505039  =17
# 503328=17
# 503270  <>17
# 503271  <>17

PAYSYS_ID = 1001
PERSON_TYPE ='ur'
QUANT = 10
MAIN_DT = datetime.datetime.now()

pytestmark = [pytest.mark.tickets('BALANCE-22965', 'BALANCE-22786')
    , reporter.feature(Features.UI, Features.INVOICE)
              ]


def data():
    agency_id = steps.ClientSteps.create({'IS_AGENCY': 1})
    steps.ClientSteps.link(agency_id, LOGIN)
    client_id = steps.ClientSteps.create({'IS_AGENCY': 0})
    person_id = steps.PersonSteps.create(agency_id, PERSON_TYPE)
    service_order_id = steps.OrderSteps.next_id(SERVICE_ID)
    service_order_id1 = steps.OrderSteps.next_id(SERVICE_ID)
    order_id = steps.OrderSteps.create (client_id, service_order_id, PRODUCT_ID, SERVICE_ID,
        {'TEXT':'Py_Test order','AgencyID' : agency_id, 'ManagerUID': None})
    order_id1 = steps.OrderSteps.create (client_id, service_order_id1, PRODUCT_ID1, SERVICE_ID,
        {'TEXT':'Py_Test order','AgencyID' : agency_id, 'ManagerUID': None})
    orders_list = [
        {'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': QUANT, 'BeginDT': MAIN_DT}
        ,{'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id1, 'Qty': QUANT, 'BeginDT': MAIN_DT}
    ]
    request_id = steps.RequestSteps.create(agency_id, orders_list, additional_params={'InvoiceDesireDT': MAIN_DT})

    return agency_id, person_id, request_id


def success_or_fail_invoice(contract_id, request_id, is_fail=0):
    my_driver = web.Driver()
    with my_driver as driver:
        Paychoose = web.Paychoose(driver)
        page = web.Paychoose.open(driver, request_id)
        if is_fail == 0:
            save_button = driver.find_element(*web.Paychoose.SAVE_BUTTON)
            save_button.click()
            Paypreview = web.Paypreview(driver)
            invoice = driver.find_element(*web.Paypreview.CREATE_INVOICE)
            invoice.click()
            invoice_id = db.balance().execute('select id from t_invoice where contract_id = :contract_id',
                                              named_params={'contract_id': contract_id})[0]['id']
            return invoice_id
        elif is_fail == 1:
            try:
                save_button = driver.find_element(*web.Paychoose.SAVE_BUTTON)
            except Exception as e:
                if 'Unable to locate element' in str(e):
                    return 'It is impossible to create invoice with unnecessary  contract'
                else:
                    raise Exception('The text of exception doesn`t matched with expected one')


def test_spec_scale():
    agency_id, person_id, request_id = data()
    contract_id, _ = steps.ContractSteps.create_contract_new('opt_agency_prem',
                                                             {'CLIENT_ID': agency_id, 'PERSON_ID': person_id,
                                                              'IS_SIGNED': '2015-01-01T00:00:00'
                                                                 , 'SERVICES': [70, 67, 77, 7]
                                                                 , 'SCALE': 3
                                                                 , 'PAYMENT_TYPE': 2
                                                              })
    print 'invoice_id: {0}'.format(str(success_or_fail_invoice(contract_id, request_id, is_fail=0)))


def test_not_spec_scale():
    agency_id, person_id, request_id = data()
    contract_id, _ = steps.ContractSteps.create_contract_new('opt_agency_prem',
                                                             {'CLIENT_ID': agency_id, 'PERSON_ID': person_id,
                                                              'IS_SIGNED': '2015-01-01T00:00:00'
                                                                 , 'SERVICES': [70, 67, 77, 7]
                                                                 , 'SCALE': 1
                                                                 , 'PAYMENT_TYPE': 2
                                                              })
    print success_or_fail_invoice(contract_id, request_id, is_fail=1)


#
def test_another_contract():
    agency_id, person_id, request_id = data()
    contract_id, _ = steps.ContractSteps.create_contract_new('no_agency',
                                                             {'CLIENT_ID': agency_id, 'PERSON_ID': person_id,
                                                              'IS_SIGNED': '2015-01-01T00:00:00'
                                                                 , 'SERVICES': [70, 67, 77, 7]
                                                                 , 'PAYMENT_TYPE': 2
                                                              })
    print 'invoice_id: {0}'.format(str(success_or_fail_invoice(contract_id, request_id, is_fail=0)))


if __name__ == "__main__":
    pytest.main("BALANCE-22965.py -v -s")
