# coding: utf-8
__author__ = 'yuelyasheva'

import datetime

import hamcrest
import pytest

from btestlib import utils
from balance import balance_db as db
from balance import balance_steps as steps
from btestlib.data.defaults import Date
from btestlib.constants import PersonTypes, Paysyses, Services, Export, InvoiceType, Products
from btestlib.utils import XmlRpc, Decimal
from btestlib.matchers import contains_dicts_equal_to


def test_payer_doc_verification():
    SERVICE_ID = Services.DIRECT.id
    PRODUCT_ID = Products.DIRECT_FISH.id
    NOW = datetime.datetime.now()
    PAYSYS_ID = Paysyses.SW_PH_CHF.id

    client_id = steps.ClientSteps.create()

    # создаем плательщика с непроверенными документами
    person_id = steps.PersonSteps.create(client_id, PersonTypes.SW_PH.code, {"verified-docs": "0"})
    service_order_id = steps.OrderSteps.next_id(service_id=SERVICE_ID)

    order_id = steps.OrderSteps.create(client_id, service_order_id, service_id=SERVICE_ID,
                                       product_id=PRODUCT_ID, params={'AgencyID': None})
    orders_list = [{'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': 50, 'BeginDT': NOW}]
    request_id = steps.RequestSteps.create(client_id, orders_list, additional_params=dict(InvoiceDesireDT=NOW))

    invoice_id, _, _ = steps.InvoiceSteps.create(request_id, person_id, PAYSYS_ID)

    # пытаемся оплатить счет и убеждаемся, что платеж не процессится
    with pytest.raises(XmlRpc.XmlRpcError) as exc_info:
        steps.InvoiceSteps.pay_fair(invoice_id)
        utils.check_that(
            'DEFER_INVOICE_PAYMENT: Person for invoice with id {} is not verified'.format(person_id)
            in exc_info.value.response, hamcrest.equal_to(True),
            step=u'Проверим, что платеж на счет с непроверенным плательщиком не разбирается')

    # проверяем, что в инвойсе нет turn_on_dt
    turn_on_dt = steps.InvoiceSteps.get_all_invoice_data_by_id(invoice_id)['turn_on_dt']
    utils.check_that(turn_on_dt, hamcrest.equal_to(None))

    # выставляем галку про проверку документов
    db.BalanceBO().insert_extprop(object_id=person_id, classname="\'Person\'", attrname="\'verified_docs\'",
                                  passport_uid=16571028, value_num=1)

    # успешно разбираем платеж
    steps.CommonSteps.export(Export.Type.PROCESS_PAYMENTS, Export.Classname.INVOICE, invoice_id)

    # проверяем, что в инвойсе проставилась turn_on_dt
    turn_on_dt = steps.InvoiceSteps.get_all_invoice_data_by_id(invoice_id)['turn_on_dt']
    utils.check_that(turn_on_dt, hamcrest.anything())

    # проверяем, что создался конзюм:
    consume_data = steps.ConsumeSteps.get_consumes_sum_by_client_id(client_id)
    expected_consume_data = steps.CommonData.create_expected_consume_data(PRODUCT_ID,
                                                                         Decimal('0'),
                                                                         InvoiceType.PREPAYMENT,
                                                                         current_qty=Decimal('50'),
                                                                         act_qty=Decimal('0'),
                                                                         act_sum=Decimal('0'),
                                                                         completion_qty=Decimal('0'),
                                                                         completion_sum=Decimal('0'))

    utils.check_that(consume_data, contains_dicts_equal_to([expected_consume_data]),
                     u'Сравниваем данные из конзюмов с шаблоном')
