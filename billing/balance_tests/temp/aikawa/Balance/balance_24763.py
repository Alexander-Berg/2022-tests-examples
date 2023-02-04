# import datetime
#
# from balance import balance_steps as steps
# from btestlib.constants import Paysyses, PersonTypes, Services, Products, ContractCommissionType, Firms, \
#     ContractPaymentType, Regions, Currencies
# from btestlib import utils as utils
# from balance import balance_db as db
# from balance import balance_api as api
#
# dt = datetime.datetime.now()
#
# TODAY = utils.Date.nullify_time_of_date(datetime.datetime.now())
# YESTERDAY = utils.Date.nullify_time_of_date(datetime.datetime.now() - datetime.timedelta(days=1))
#
# SERVICE_ID = 7
# PRODUCT_ID = 1475
# PAYSYS_ID = 1003
#
# client_id = steps.ClientSteps.create()
# steps.ClientSteps.link(client_id, 'aikawa-test-10')
# person_id = steps.PersonSteps.create(client_id, 'ur')
# service_order_id = steps.OrderSteps.next_id(service_id=SERVICE_ID)
# order_id = steps.OrderSteps.create(client_id=client_id, product_id=PRODUCT_ID, service_id=SERVICE_ID,
#                                    service_order_id=service_order_id, params={'AgencyID': None})
# orders_list = [
#     {'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': 100, 'BeginDT': YESTERDAY}
# ]
# request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list,
#                                        additional_params={'InvoiceDesireDT': YESTERDAY})

# invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=PAYSYS_ID,
#                                              credit=0, contract_id=None, overdraft=0)
# steps.InvoiceSteps.pay(invoice_id)
# steps.CampaignsSteps.do_campaigns(SERVICE_ID, service_order_id, {'Bucks': 100}, 0, YESTERDAY)
# steps.ClientSteps.migrate_to_currency(client_id=client_id, currency='RUB', service_id=SERVICE_ID,
#                                       currency_convert_type='MODIFY', dt=TODAY, region_id=225)
# steps.CommonSteps.export('MIGRATE_TO_CURRENCY', 'Client', client_id)
# print db.get_order_by_id(order_id)[0]['completion_fixed_qty']
#
# steps.CampaignsSteps.do_campaigns(SERVICE_ID, service_order_id, {'Bucks': 90}, 0, YESTERDAY)
# print db.get_order_by_id(order_id)[0]['completion_fixed_qty']


print 'https://balance-26194.branch.greed-dev.paysys.yandex.ru/consumes-history-new.xml'
print 'https://balance-26194.branch.greed-dev.paysys.yandex.ru/consumes-history.xml'
print 'https://balance-26194.branch.greed-dev.paysys.yandex.ru/deferpays.xml'
print 'https://balance-26194.branch.greed-dev.paysys.yandex.ru/new-credit.xml'
print 'https://balance-26194.branch.greed-dev.paysys.yandex.ru/new-overdraft-ur.xml'
print 'https://balance-26194.branch.greed-dev.paysys.yandex.ru/patch-invoice-person.xml'
