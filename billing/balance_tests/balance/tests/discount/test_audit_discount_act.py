# -*- coding: utf-8 -*-

from decimal import Decimal as D
import pytest
from hamcrest import has_entries

from btestlib import utils as utils

_, END_LAST_MONTH = utils.Date.previous_month_first_and_last_days()
from balance import balance_db as db
from balance import balance_steps as steps
from balance.features import AuditFeatures
from btestlib import utils as utils
import btestlib.reporter as reporter
from temp.igogor.balance_objects import Contexts, Products, Firms, Currencies, \
    ContractCommissionType, Regions

DT = END_LAST_MONTH


@pytest.mark.audit(reporter.feature(AuditFeatures.RV_C10_1))
def test_audit_discount():
    qty = D(100)
    discount_pct = D(20)
    price = D(1)

    reporter.attach(u'Параметры для теста: скидка {0}, количество продукта {1}, '
                    u'цена {2}'.format(discount_pct, qty, price))

    with reporter.step(u'Вычисляем сумму со скидкой'):
        sum_with_discount = (qty * price) * (D(100) - discount_pct) / D(100)
        reporter.attach(u'Сумма со скидкой {}'.format(sum_with_discount))

    context = Contexts.DIRECT_FISH_SW_EUR_CONTEXT.new(contract_type=ContractCommissionType.SW_OPT_CLIENT,
                                                      firm=Firms.EUROPE_AG_7,
                                                      currency=Currencies.EUR,
                                                      product=Products.DIRECT_EUR,
                                                      region=Regions.SW)
    contract_params = {
        'DISCOUNT_FIXED': discount_pct,
    }
    client_id, person_id, contract_id, _ = steps.ContractSteps.create_general_contract_by_context(context,
                                                                                                  additional_params=contract_params,
                                                                                                  start_dt=DT)
    steps.ClientSteps.migrate_to_currency(client_id=client_id,
                                          currency_convert_type='COPY',
                                          dt=DT,
                                          currency=context.currency.iso_code,
                                          region_id=context.region.id
                                          )

    service_order_id = steps.OrderSteps.next_id(context.service.id)
    steps.OrderSteps.create(client_id, service_order_id, service_id=context.service.id,
                            product_id=context.product.id)
    orders_list = [{'ServiceID': context.service.id, 'ServiceOrderID': service_order_id,
                    'Qty': qty}]
    request_id = steps.RequestSteps.create(client_id, orders_list, additional_params={'InvoiceDesireDT': DT})
    invoice_id, _, _ = steps.InvoiceSteps.create(request_id, person_id,
                                                 context.paysys.id,
                                                 contract_id=contract_id)
    steps.InvoiceSteps.pay(invoice_id)

    steps.CampaignsSteps.do_campaigns(context.service.id, service_order_id,
                                      {context.product.type.code: qty}, 0, campaigns_dt=DT)
    steps.ActsSteps.generate(client_id, force=1, date=DT)

    expected_data_in_consume = {
        'consume_qty': qty,
        'consume_sum': sum_with_discount,
        'current_qty': qty,
        'current_sum': sum_with_discount,
        'completion_qty': qty,
        'completion_sum': sum_with_discount,
        'act_qty': qty,
        'act_sum': sum_with_discount,
        'static_discount_pct': discount_pct,
        'discount_pct': discount_pct,
        'price': price,
    }
    utils.check_that(db.get_consumes_by_invoice(invoice_id)[0],
                     has_entries(expected_data_in_consume),
                     step=u'Проверяем сумму (с учетом скидки) и скидку в заявке')

    expected_data_in_act = {
        'act_sum': sum_with_discount,
        'amount': sum_with_discount,
    }
    utils.check_that(steps.ActsSteps.get_act_data_by_client(client_id)[0],
                     has_entries(expected_data_in_act),
                     step=u'Проверяем сумму (с учетом скидки) в акте')
