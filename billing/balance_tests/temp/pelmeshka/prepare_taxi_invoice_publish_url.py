# coding: utf-8
__author__ = 'pelmeshka'

# Скрипт для создания ПФ счёта от Yandex.TAXI B.V. для Литвы на 507862 продукт

from datetime import datetime
from balance import balance_steps as steps
import balance.balance_web as web
from btestlib.constants import PersonTypes, Firms, Services, Currencies, Regions, Nds
from btestlib.data.defaults import Taxi
from decimal import Decimal
from dateutil.relativedelta import relativedelta
import btestlib.utils as utils


CONTRACT_START_DT = utils.Date.first_day_of_month(datetime.now() - relativedelta(months=1))
ADVANCE_PAYMENT_SUM = Decimal('100')
QTY = Decimal('50')

firm = Firms.TAXI_BV_22
services = [Services.TAXI_111, Services.TAXI_128]
person_type = PersonTypes.EU_YT.code
contract_template = Taxi.ContractTemplate.NO_AGENCY
nds = Nds.NONE
region = Regions.LT
currency = Currencies.EUR
cash_product = 507862


client_id, contract_id, person_id = \
    steps.TaxiSteps.create_taxi_contract_prepay(CONTRACT_START_DT, ADVANCE_PAYMENT_SUM, firm,
                                                person_type, currency, region, nds, services=services,
                                                contract_template=contract_template)

service_order_id = steps.OrderSteps.next_id(Taxi.CASH_SERVICE_ID)
steps.OrderSteps.create(client_id, service_order_id, product_id=cash_product,
                        service_id=Taxi.CASH_SERVICE_ID)

orders_list = [{'ServiceID': Taxi.CASH_SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': QTY,
                'BeginDT': CONTRACT_START_DT}]
request_id = steps.RequestSteps.create(client_id, orders_list)

invoice_id, _, _ = steps.InvoiceSteps.create(credit=0, request_id=request_id, person_id=person_id,
                                             paysys_id=int(str(firm.id) + '01039'),
                                             contract_id=contract_id)
url = web.ClientInterface.InvoicePublishPage.url(invoice_id=invoice_id)
print('Final invoice-publish url: ' + url)

