# -*- coding: utf-8 -*-

from decimal import Decimal as D
from hamcrest import has_entries
import pytest
import datetime

from balance import balance_db as db
from balance import balance_steps as steps
from balance.features import AuditFeatures
from btestlib import utils as utils
import btestlib.reporter as reporter
from temp.igogor.balance_objects import PromocodeClass
from promocode_commons import DIRECT_YANDEX_FIRM_RUB, DT_1_DAY_AFTER, \
    create_and_reserve_promocode, create_request, create_invoice, create_order, \
    fill_calc_params_fixed_sum, add_nds_to_amount

_, END_LAST_MONTH = utils.Date.previous_month_first_and_last_days()

DT = END_LAST_MONTH
@pytest.mark.audit(reporter.feature(AuditFeatures.RV_C10_1))
def test_audit_promocode():
    context = DIRECT_YANDEX_FIRM_RUB

    sum_bonus = D(50)
    qty = D(500)
    price = D(1)
    reporter.attach(u'Параметры для теста: сумма бонуса {0}, количество продукта {1}, '
                    u'цена {2}'.format(sum_bonus, qty, price))

    with reporter.step(u'Вычисляем сумму бонуса с НДС'):
        bonus_with_nds = add_nds_to_amount(sum_bonus, nds=context.nds)
        reporter.attach(u'Сумма бонуса с НДС {}'.format(bonus_with_nds))
    total_sum = qty * price
    sum_with_discount = total_sum - bonus_with_nds
    discount_pct = (bonus_with_nds / total_sum) * D(100)

    client_id = steps.ClientSteps.create()
    steps.ClientSteps.migrate_to_currency(client_id=client_id, currency_convert_type='COPY',
                                          dt=DT - datetime.timedelta(days=1),
                                          currency=context.currency.iso_code, region_id=context.region.id
                                          )
    person_id = steps.PersonSteps.create(client_id, context.person_type.code)

    calc_params = fill_calc_params_fixed_sum(currency_bonuses={context.currency.iso_code: sum_bonus},
                                             reference_currency=None, adjust_quantity=False,
                                             apply_on_create=True)
    create_and_reserve_promocode(calc_class_name=PromocodeClass.FIXED_SUM,
                                 client_id=client_id,
                                 firm_id=context.firm.id,
                                 calc_params=calc_params,
                                 end_dt=DT_1_DAY_AFTER,
                                 service_ids=[context.service.id],
                                 start_dt=DT - datetime.timedelta(days=1))
    service_order_id, _ = create_order(context=context, agency_id=None, client_id=client_id)
    orders_list = [
        {'ServiceID': context.service.id, 'ServiceOrderID': service_order_id, 'Qty': qty,
         'BeginDT': DT},
    ]
    request_id, _ = create_request(context, client_id, orders_list=orders_list,
                                   invoice_dt=DT)
    invoice_id = create_invoice(request_id, person_id, paysys_id=context.paysys.id
                                )
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
