# coding: utf-8
__author__ = 'pelmeshka'


# Вспомогательный скрипт, который генерит ссылку на печатную форму на сервис SERVICE с продуктом PRODUCT
# от фирмы FIRM для юрика по договору


from balance import balance_steps as steps
import balance.balance_web as web
from btestlib.constants import PersonTypes, Firms, Services, Products
from btestlib.data.defaults import Date

FIRM = Firms.MEDIASERVICES_121
SERVICE = Services.SHOP
PRODUCT = Products.DIRECT_FISH

client_id = steps.ClientSteps.create()
invoice_owner = client_id
person_id = steps.PersonSteps.create(client_id, PersonTypes.UR.code)
service_order_id = steps.OrderSteps.next_id(SERVICE.id)

contract_id, _ = steps.ContractSteps.create_contract('no_agency_post',
                                                     {'CLIENT_ID': invoice_owner,
                                                      'PERSON_ID': person_id,
                                                      'SERVICES': [SERVICE.id],
                                                      'FIRM': FIRM.id,
                                                      'FINISH_DT': Date.HALF_YEAR_AFTER_TODAY_ISO,
                                                      })
order_id = steps.OrderSteps.create(client_id, service_order_id, PRODUCT.id, SERVICE.id,
                                   {'TEXT': 'Py_Test order', 'AgencyID': None, 'ManagerUID': 403026233})
orders_list = [{'ServiceID': SERVICE.id, 'ServiceOrderID': service_order_id, 'Qty': 45, 'BeginDT': Date.TODAY}]

request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list,
                                       additional_params=dict(FirmID=FIRM.id))

invoice_id, _, _ = steps.InvoiceSteps.create(credit=0, request_id=request_id, person_id=person_id,
                                             paysys_id=int(str(FIRM.id) + '01003'),
                                             contract_id=contract_id)
url = web.ClientInterface.InvoicePublishPage.url(invoice_id=invoice_id)
print('Final invoice-publish url: ' + url)
