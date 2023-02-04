# coding: utf-8
__author__ = 'yuelyasheva'

import datetime

import hamcrest
import pytest

from btestlib import utils
from balance import balance_db as db
from balance import balance_steps as steps
from btestlib.data.defaults import Date
from btestlib.constants import PersonTypes, Paysyses, Services, Export, InvoiceType, Products, Firms, NdsNew
from btestlib.utils import XmlRpc, Decimal
from btestlib.matchers import contains_dicts_equal_to, contains_dicts_with_entries, has_entries_casted

# перенесено из джавы ru.yandex.autotests.balance.tests.paystep.paystepRules.oldCatalogRequests
@pytest.mark.parametrize('service_id, product_id',
                         [(Services.CATALOG1.id, Products.CATALOG1_87.id, ),
                          (Services.CATALOG2.id, Products.CATALOG2_1636.id),
                          ],
                         ids=['Catalog 5',
                              'Catalog 6']
                         )
def test_not_old_catalog_orders(service_id, product_id):
    NOW = datetime.datetime.now()
    PAYSYS_ID = Paysyses.BANK_UR_RUB.id
    QTY = Decimal('50')

    client_id = steps.ClientSteps.create()

    # создаем плательщика с непроверенными документами
    person_id = steps.PersonSteps.create(client_id, PersonTypes.UR.code)
    service_order_id = steps.OrderSteps.next_id(service_id=service_id)

    order_id = steps.OrderSteps.create(client_id, service_order_id, service_id=service_id,
                                       product_id=product_id, params={'AgencyID': None})
    orders_list = [{'ServiceID': service_id, 'ServiceOrderID': service_order_id, 'Qty': QTY, 'BeginDT': NOW}]

    # сдвигаем дату заказа на 5 дней - 10 минут
    delta = 120./86400 - 5
    steps.OrderSteps.move_order_dt(service_order_id=service_order_id, service_id=service_id,
                                   delta=Decimal(delta))
    request_id = steps.RequestSteps.create(client_id, orders_list, additional_params=dict(InvoiceDesireDT=NOW))

    invoice_id, _, _ = steps.InvoiceSteps.create(request_id, person_id, PAYSYS_ID)
    expected_invoice_data = steps.CommonData.create_expected_invoice_data(None, person_id, Decimal('0'),
                                                                          InvoiceType.PREPAYMENT, PAYSYS_ID,
                                                                          Firms.YANDEX_1)
    expected_invoice_data.update({'effective_sum': get_product_price_with_nds(product_id) * QTY})
    invoice_data = steps.InvoiceSteps.get_all_invoice_data_by_id(invoice_id)

    utils.check_that(invoice_data, has_entries_casted(expected_invoice_data),
                     u'Сравниваем данные из ивойсов с шаблоном')


def get_product_price_with_nds(product_id):
    query = "select price, tax from t_price where product_id = :product_id order by dt desc"
    params = {'product_id': product_id}
    res = db.balance().execute(query, params)[0]
    price, tax = res['price'], res['tax']
    if tax == 0:
        price = price * NdsNew.DEFAULT.koef_on_dt(datetime.datetime.today())
    return price