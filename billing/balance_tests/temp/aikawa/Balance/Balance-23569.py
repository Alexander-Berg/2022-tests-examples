import copy
import datetime

from hamcrest import has_entries

import btestlib.utils as utils
from balance import balance_steps as steps
from btestlib.matchers import matches_in_time

dt = datetime.datetime.now()
ORDER_DT = dt


class ClientNotification(object):
    DEFAULT_VALUES = {'BusinessUnit': '0',
                      'CanBeForceCurrencyConverted': '0',
                      'ClientCurrency': '',
                      # 'ClientID': client_id,
                      'MigrateToCurrencyDone': '0',
                      'MinPaymentTerm': '0000-00-00',
                      'NonResident': '0',
                      'OverdraftBan': '0',
                      'OverdraftLimit': '0',
                      'OverdraftSpent': '0.00',
                      # 'Tid': '20160420140303745'
                      }

    def __init__(self, parameters={}):
        self.values = copy.deepcopy(ClientNotification.DEFAULT_VALUES)
        self.values.update(parameters)


def currency_case():
    PERSON_TYPE = 'ur'
    SERVICE_ID = 7
    PRODUCT_ID = 1475
    PAYSYS_ID = 1003
    client_id = steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, PERSON_TYPE)
    service_order_id = steps.OrderSteps.next_id(service_id=SERVICE_ID)
    order_id = steps.OrderSteps.create(client_id=client_id, product_id=PRODUCT_ID, service_id=SERVICE_ID,
                                       service_order_id=service_order_id)
    orders_list = [
        {'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': 500, 'BeginDT': dt}
    ]
    request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list)
    invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=PAYSYS_ID,
                                                 credit=0, contract_id=None, overdraft=0, endbuyer_id=None)
    steps.InvoiceSteps.pay(invoice_id)
    print steps.CommonSteps.get_last_notification(10, client_id)


# currency_case()


def contract_case():
    PERSON_TYPE = 'ur'
    SERVICE_ID = 7
    PRODUCT_ID = 1475
    PAYSYS_ID = 1003
    contract_type = 'no_agency_post'
    client_id = steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, PERSON_TYPE)
    service_order_id = steps.OrderSteps.next_id(service_id=SERVICE_ID)
    order_id = steps.OrderSteps.create(client_id=client_id, product_id=PRODUCT_ID, service_id=SERVICE_ID,
                                       service_order_id=service_order_id)
    orders_list = [
        {'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': 500, 'BeginDT': dt}
    ]
    request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list)
    contract_id, _ = steps.ContractSteps.create_contract(contract_type, {'CLIENT_ID': client_id, 'PERSON_ID': person_id,
                                                                         'DT': '2015-04-30T00:00:00',
                                                                         'FINISH_DT': '2016-09-30T00:00:00',
                                                                         'IS_SIGNED': '2015-01-01T00:00:00',
                                                                         'SERVICES': [7], 'PAYMENT_TERM': '40'})
    invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=PAYSYS_ID,
                                                 credit=1, contract_id=contract_id, overdraft=0, endbuyer_id=None)
    # steps.InvoiceSteps.pay(invoice_id)
    # print steps.CommonSteps.get_last_notification(10, client_id)
    utils.check_that(lambda: steps.CommonSteps.get_last_notification(10, client_id),
                     matches_in_time(has_entries(ClientNotification({
                         'CanBeForceCurrencyConverted': '1'
                     }).values), timeout=300))


# contract_case()
print steps.CommonSteps.get_last_notification(10, 17545989)
