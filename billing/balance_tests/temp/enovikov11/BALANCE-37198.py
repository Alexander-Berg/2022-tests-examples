import datetime
from balance import balance_steps as steps
from balance import balance_api as api
from btestlib.data.partner_contexts import CLOUD_KZ_CONTEXT
context = CLOUD_KZ_CONTEXT
product_id = 512220
client_id, person_id, contract_id, _ = steps.ContractSteps.create_partner_contract(context, is_offer=1)
service_order_id = steps.OrderSteps.next_id(context.service.id)
steps.OrderSteps.create(client_id, service_order_id, product_id=product_id,
                        service_id=context.service.id)
orders_list = [{'ServiceID': context.service.id, 'ServiceOrderID': service_order_id, 'Qty': 10.5,
                'BeginDT': datetime.datetime.now()}]
request_id = steps.RequestSteps.create(client_id, orders_list,
                                       additional_params={'InvoiceDesireDT': datetime.datetime.now(),
                                                          'InvoiceDesireType': 'charge_note'})
paysys_id = api.medium().GetRequestChoices({'OperatorUid': 16571028,
                                            'RequestID': request_id})['paysys_list'][0]['id']
invoice_id, _, _ = steps.InvoiceSteps.create(request_id, person_id, paysys_id,
                                             credit=0, contract_id=contract_id)
