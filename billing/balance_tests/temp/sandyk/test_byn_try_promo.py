import datetime

import allure
import pytest

import balance.balance_db as db
from balance import balance_steps as steps
from balance.features import Features
from btestlib.constants import Services, Products, ContractPaymentType, Currencies, Managers, Firms, PersonTypes, Paysyses, Regions


PERSON_TYPE_BY = PersonTypes.BYU.code
PAYSYS_ID_BY = Paysyses.BANK_BY_UR_BYN.id
FIRM_ID_BY = Paysyses.BANK_BY_UR_BYN.firm.id
SERVICE_ID =Services.DIRECT.id
PRODUCT_ID =  Products.DIRECT_FISH.id
CURRENCY_PRODUCT_ID_BY =  Products.DIRECT_BYN.id
PAYMENT_TYPE = ContractPaymentType.PREPAY
CURRENCY_BY = Paysyses.BANK_BY_UR_BYN.currency.num_code

PERSON_TYPE_TRY = PersonTypes.TRU.code

DT = datetime.datetime.now()

QTY = 5901
START_DT = str(datetime.datetime.today().strftime("%Y-%m-%d")) + 'T00:00:00'

PERSON_MAPPER = {}

# promocode_data = {'promocode':'MRRY-6USP-DNTW-CC8Q', 'firm_id':27,  }


# @pytest.mark.tickets('BALANCE-26533')
# def test_byn():
#     steps.PromocodeSteps.clean_up(2624012)
#     client_id = steps.ClientSteps.create(params={'IS_AGENCY':0})
#     steps.ClientSteps.link(client_id,'torvald-test-15')
#     person_id = steps.PersonSteps.create(client_id, PERSON_TYPE_BY)
#     service_order_id = steps.OrderSteps.next_id(SERVICE_ID)
#     order_id = steps.OrderSteps.create(client_id, service_order_id, PRODUCT_ID, SERVICE_ID ,
#                                        {'TEXT': 'Py_Test order', 'AgencyID': None, 'ManagerUID': None})
#     orders_list = [
#         {'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': QTY, 'BeginDT': DT}
#     ]
#     request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list,
#                                            additional_params={'InvoiceDesireDT': DT, 'DenyPromocode':0 })
#
#     # invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=PAYSYS_ID,
#     #                                              credit=0, contract_id=None, overdraft=0, endbuyer_id=None)
#     # steps.InvoiceSteps.pay(invoice_id)


# def test_try():
#     QTY = 100
#     steps.PromocodeSteps.clean_up(2624013)
#     client_id = steps.ClientSteps.create(params={'IS_AGENCY': 0})
#     steps.ClientSteps.link(client_id, 'torvald-test-15')
#     person_id = steps.PersonSteps.create(client_id, PERSON_TYPE_TRY)
#     service_order_id = steps.OrderSteps.next_id(SERVICE_ID)
#     order_id = steps.OrderSteps.create(client_id, service_order_id, PRODUCT_ID, SERVICE_ID,
#                                        {'TEXT': 'Py_Test order', 'AgencyID': None, 'ManagerUID': None})
#     orders_list = [{'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': QTY, 'BeginDT': DT}]
#     request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list,
#                                            additional_params={'InvoiceDesireDT': DT, 'DenyPromocode': 0})
#
#     # invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=PAYSYS_ID,
#     #                                              credit=0, contract_id=None, overdraft=0, endbuyer_id=None)
#     # steps.InvoiceSteps.pay(invoice_id)




def test_try_cur():
    QTY =149
    steps.PromocodeSteps.clean_up(2624013)
    client_id = steps.ClientSteps.create(params={'IS_AGENCY':0})
    steps.ClientSteps.migrate_to_currency(client_id=client_id, currency_convert_type='COPY', dt=DT, service_id=SERVICE_ID,
                                          region_id=Regions.TR.id, currency='TRY')

    steps.ClientSteps.link(client_id,'torvald-test-15')
    person_id = steps.PersonSteps.create(client_id, PERSON_TYPE_TRY)
    service_order_id = steps.OrderSteps.next_id(SERVICE_ID)
    order_id = steps.OrderSteps.create(client_id, service_order_id, 503354, SERVICE_ID ,
                                       {'TEXT': 'Py_Test order', 'AgencyID': None, 'ManagerUID': None})
    orders_list = [
        {'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': QTY, 'BeginDT': DT}
    ]
    request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list,
                                           additional_params={'InvoiceDesireDT': DT, 'DenyPromocode':0 })

    # invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=1050,
    #                                              credit=0, contract_id=None, overdraft=0, endbuyer_id=None)
    # steps.InvoiceSteps.pay(invoice_id)



# def test_byn_cur():
#     QTY = 120
#     steps.PromocodeSteps.clean_up(2624012)
#     client_id = steps.ClientSteps.create(params={'IS_AGENCY':0})
#     # steps.ClientSteps.migrate_to_currency(client_id=client_id, currency_convert_type='COPY', dt=DT, service_id=SERVICE_ID,
#     #                                       region_id=Regions.BY.id, currency='BYN')
#
#     steps.ClientSteps.link(client_id,'torvald-test-15')
#     person_id = steps.PersonSteps.create(client_id, PERSON_TYPE_BY)
#     service_order_id = steps.OrderSteps.next_id(SERVICE_ID)
#     order_id = steps.OrderSteps.create(client_id, service_order_id, 1475, SERVICE_ID ,
#                                        {'TEXT': 'Py_Test order', 'AgencyID': None, 'ManagerUID': None})
#     orders_list = [
#         {'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': QTY, 'BeginDT': DT}
#     ]
#     request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list,
#                                            additional_params={'InvoiceDesireDT': DT, 'DenyPromocode':0 })
#
#     # invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=1050,
#     #                                              credit=0, contract_id=None, overdraft=0, endbuyer_id=None)
#     # steps.InvoiceSteps.pay(invoice_id)
