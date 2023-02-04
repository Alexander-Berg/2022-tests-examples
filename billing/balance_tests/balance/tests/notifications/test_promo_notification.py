# -*- coding: utf-8 -*-
import datetime
import hamcrest
import pytest
from collections import namedtuple

from decimal import Decimal

from balance import balance_db as db
from balance import balance_steps as steps
from btestlib import utils, reporter
from dateutil.relativedelta import relativedelta
from btestlib.constants import PromocodeClass, ProductTypes
from balance.features import Features
from btestlib.matchers import has_entries_casted, matches_in_time
from balance.tests.promocode_new.promocode_commons import DIRECT_YANDEX_FIRM_RUB, DIRECT_YANDEX_FIRM_FISH, \
    create_and_reserve_promocode, fill_calc_params_fixed_discount, create_request, is_request_with_promocode, \
    create_order

pytestmark = [reporter.feature(Features.NOTIFICATION, Features.PROMOCODE),
              pytest.mark.tickets('BALANCE-28045')]

dt = datetime.datetime.now()
dt_1_day_before = dt - relativedelta(days=1)
dt_1_day_after = dt + relativedelta(days=1)

invoice = namedtuple('invoice', 'client_id, service_id, promocode_id, service_order_id, order_id, invoice_id')

OPCODE_PROMOCODE = 90


def test_add_promocode_real_promocode_notification():
    with reporter.step(u'Выставляем счет с промокодом'):
        invoice_params = prepare_invoice()

    utils.check_that(lambda: steps.CommonSteps.get_last_notification(OPCODE_PROMOCODE, invoice_params.order_id) and
                             steps.CommonSteps.get_last_notification(OPCODE_PROMOCODE, invoice_params.order_id
                                                                     )['Promocodes'][0],
                     matches_in_time(has_entries_casted({'UnusedPromocodeQty': '20.003000',
                                                         'AvailablePromocodeQty': '20.003000'}), timeout=300))


def test_transfer_to_and_from_invoice_with_promocode():
    with reporter.step(u'Выставляем счет без промокода'):
        invoice_params_no_promocode = prepare_invoice(promocode_bonus=0)

    utils.check_that(
        steps.CommonSteps.build_notification(OPCODE_PROMOCODE, object_id=invoice_params_no_promocode.order_id
                                             )['args'][0]['Promocodes'],
        hamcrest.equal_to([]),
        step=u'Проверяем нотификацию для счета без промокода')

    with reporter.step(u'Выставляем счет с промокодом'):
        invoice_params_with_promocode = prepare_invoice(client_id=invoice_params_no_promocode.client_id)

    utils.check_that(
        steps.CommonSteps.build_notification(OPCODE_PROMOCODE, object_id=invoice_params_with_promocode.order_id
                                             )['args'][0]['Promocodes'][0],
        has_entries_casted({'UnusedPromocodeQty': '20.003000',
                            'AvailablePromocodeQty': '20.003000'}),
        step=u'Проверяем нотификацию для счета с промокодом')

    with reporter.step(u'Переносим средства со счета с промокодом'):
        steps.OrderSteps.transfer(
            [{'order_id': invoice_params_with_promocode.order_id, 'qty_old': 30.003000, 'qty_new': 15.0015,
              'all_qty': 0}],
            [{'order_id': invoice_params_no_promocode.order_id, 'qty_delta': 1}])

    utils.check_that(
        steps.CommonSteps.build_notification(OPCODE_PROMOCODE, object_id=invoice_params_with_promocode.order_id
                                             )['args'][0]['Promocodes'][0],
        has_entries_casted({'UnusedPromocodeQty': '10.001500',
                            'AvailablePromocodeQty': '10.001500'}),
        step=u'Проверяем нотификацию для счета с промокодом после переноса средств с него')

    with reporter.step(u'Переносим средства на счет с промокодом'):
        steps.OrderSteps.transfer(
            [{'order_id': invoice_params_no_promocode.order_id, 'qty_old': 25.0015, 'qty_new': 17.50075, 'all_qty': 0}],
            [{'order_id': invoice_params_with_promocode.order_id, 'qty_delta': 1}])

    utils.check_that(
        steps.CommonSteps.build_notification(OPCODE_PROMOCODE, object_id=invoice_params_with_promocode.order_id
                                             )['args'][0]['Promocodes'][0],
        has_entries_casted({'UnusedPromocodeQty': '15.002250',
                            'AvailablePromocodeQty': '15.002250'}),
        step=u'Проверяем нотификацию для счета с промокодом после переноса средств на него')


def test_full_campaign():
    with reporter.step(u'Выставляем и полностью откручиваем счет с промокодом'):
        invoice_params = prepare_invoice()

        steps.CampaignsSteps.do_campaigns(invoice_params.service_id,
                                          invoice_params.service_order_id,
                                          {'Bucks': 30.003}, 0, dt)

    utils.check_that(
        steps.CommonSteps.build_notification(OPCODE_PROMOCODE, object_id=invoice_params.order_id
                                             )['args'][0]['Promocodes'],
        hamcrest.equal_to([]),
        step=u'Проверяем нотификацию после откручивания счета с промокодом')

    with reporter.step(u'Уменьшаем количество откруток на полностью открученном заказе'):
        steps.CampaignsSteps.do_campaigns(invoice_params.service_id,
                                          invoice_params.service_order_id,
                                          {'Bucks': 25}, 0, dt)

    utils.check_that(
        steps.CommonSteps.build_notification(OPCODE_PROMOCODE, object_id=invoice_params.order_id
                                             )['args'][0]['Promocodes'][0],
        has_entries_casted({'UnusedPromocodeQty': '3.335500',
                            'AvailablePromocodeQty': '3.335500'}),
        step=u'Проверяем нотификацию при уменьшении откруток после полной открутки')


def test_tear_promocode():
    with reporter.step(u'Выставляем счет с промокодом'):
        invoice_params = prepare_invoice()

    steps.PromocodeSteps.tear_off_promocode(client_id=invoice_params.client_id,
                                            promocode_id=invoice_params.promocode_id)

    utils.check_that(
        steps.CommonSteps.build_notification(
            OPCODE_PROMOCODE,
            object_id=invoice_params.order_id
        )['args'][0]['Promocodes'],
        hamcrest.equal_to([]),
        step=u'Проверяем нотификацию после отрыва промокода'
    )


def test_tear_promocode_champaigned_equal_to_acted():
    with reporter.step(u'Выставляем счет с промокодом, частично откручиваем и актим'):
        invoice_params = prepare_invoice()

        steps.CampaignsSteps.do_campaigns(invoice_params.service_id,
                                          invoice_params.service_order_id,
                                          {'Bucks': 15}, 0, dt)

        act_id = steps.ActsSteps.generate(invoice_params.client_id, force=1, date=dt)[0]

    steps.PromocodeSteps.tear_off_promocode(client_id=invoice_params.client_id,
                                            promocode_id=invoice_params.promocode_id)

    utils.check_that(
        steps.CommonSteps.build_notification(
            OPCODE_PROMOCODE,
            object_id=invoice_params.order_id
        )['args'][0]['Promocodes'],
        hamcrest.equal_to([]),
        step=u'Проверяем нотификацию после отрыва промокода'
    )


def test_tear_promocode_champaigned_more_than_acted():
    with reporter.step(u'Выставляем счет с промокодом, частично откручиваем и актим'):
        invoice_params = prepare_invoice()

        steps.CampaignsSteps.do_campaigns(invoice_params.service_id,
                                          invoice_params.service_order_id,
                                          {'Bucks': 10}, 0, dt)

        act_id = steps.ActsSteps.generate(invoice_params.client_id, force=1, date=dt)[0]

    with reporter.step(u'Увеличиваем количество откруток'):
        steps.CampaignsSteps.do_campaigns(invoice_params.service_id,
                                          invoice_params.service_order_id, {
                                              'Bucks': 15}, 0, dt)

    steps.PromocodeSteps.tear_off_promocode(client_id=invoice_params.client_id,
                                            promocode_id=invoice_params.promocode_id)

    utils.check_that(
        steps.CommonSteps.build_notification(
            OPCODE_PROMOCODE,
            object_id=invoice_params.order_id
        )['args'][0]['Promocodes'],
        hamcrest.equal_to([]),
        step=u'Проверяем нотификацию после отрыва промокода'
    )


def test_tear_promocode_champaigned_less_than_acted():
    with reporter.step(u'Выставляем счет с промокодом, частично откручиваем и актим'):
        invoice_params = prepare_invoice()

        steps.CampaignsSteps.do_campaigns(invoice_params.service_id,
                                          invoice_params.service_order_id,
                                          {'Bucks': 15.0015}, 0, dt)

        act_id = steps.ActsSteps.generate(invoice_params.client_id, force=1, date=dt)[0]

    with reporter.step(u'Уменьшаем количество откруток'):
        steps.CampaignsSteps.do_campaigns(invoice_params.service_id,
                                          invoice_params.service_order_id,
                                          {'Bucks': 7.50075}, 0, dt)

    utils.check_that(
        steps.CommonSteps.build_notification(OPCODE_PROMOCODE, object_id=invoice_params.order_id
                                             )['args'][0]['Promocodes'][0],
        has_entries_casted({'UnusedPromocodeQty': '15.002250',
                            'AvailablePromocodeQty': '10.001500'}),
        step=u'Проверяем нотификацию после уменьшения количества откруток')

    steps.PromocodeSteps.tear_off_promocode(client_id=invoice_params.client_id,
                                            promocode_id=invoice_params.promocode_id)

    utils.check_that(
        steps.CommonSteps.build_notification(OPCODE_PROMOCODE, object_id=invoice_params.order_id
                                             )['args'][0]['Promocodes'][0],
        has_entries_casted({'UnusedPromocodeQty': '5.000750',
                            'AvailablePromocodeQty': '0.000000'}),
        step=u'Проверяем нотификацию после отрыва промокода')


@pytest.mark.tickets('BALANCE-31318')
def test_tear_promocode_zero_consume():
    with reporter.step(u'Выставляем счет без промокода'):
        invoice_params_no_promocode = prepare_invoice(promocode_bonus=0)

    with reporter.step(u'Выставляем счет с промокодом'):
        invoice_params_with_promocode = prepare_invoice(client_id=invoice_params_no_promocode.client_id,
                                                        promocode_bonus=1)

    with reporter.step(u'Переносим средства со счета с промокодом'):
        steps.OrderSteps.transfer(
            [{'order_id': invoice_params_with_promocode.order_id, 'qty_old': 10.999890, 'qty_new': 10.999740,
              'all_qty': 0}],
            [{'order_id': invoice_params_no_promocode.order_id, 'qty_delta': 1}])

    steps.PromocodeSteps.tear_off_promocode(client_id=invoice_params_with_promocode.client_id,
                                            promocode_id=invoice_params_with_promocode.promocode_id)

    utils.check_that(
        steps.CommonSteps.build_notification(
            OPCODE_PROMOCODE,
            object_id=invoice_params_with_promocode.order_id
        )['args'][0]['Promocodes'],
        hamcrest.equal_to([]),
        step=u'Проверяем нотификацию после отрыва промокода'
    )

    promo_consumes = db.balance().execute('select * from t_consume where invoice_id = :item and archive = 0',
                                          {'item': invoice_params_with_promocode.invoice_id})

    utils.check_that(len(promo_consumes), hamcrest.equal_to(2),
                     step=u'Проверяем, что оба конзюма остались в счете с промокодом')  # BALANCE-31386


@pytest.mark.tickets('BALANCE-31386')
def test_tear_promocode_only_zero_consume():
    with reporter.step(u'Выставляем счет без промокода'):
        invoice_params_no_promocode = prepare_invoice(promocode_bonus=0)

    with reporter.step(u'Выставляем счет с промокодом'):
        invoice_params_with_promocode = prepare_invoice(client_id=invoice_params_no_promocode.client_id)

    with reporter.step(u'Переносим средства со счета с промокодом'):
        steps.OrderSteps.transfer(
            [{'order_id': invoice_params_with_promocode.order_id, 'qty_old': 30.003000, 'qty_new': 0.00015,
              'all_qty': 0}],
            [{'order_id': invoice_params_no_promocode.order_id, 'qty_delta': 1}])

    steps.PromocodeSteps.tear_off_promocode(client_id=invoice_params_with_promocode.client_id,
                                            promocode_id=invoice_params_with_promocode.promocode_id)

    utils.check_that(
        steps.CommonSteps.build_notification(
            OPCODE_PROMOCODE,
            object_id=invoice_params_no_promocode.order_id
        )['args'][0]['Promocodes'],
        hamcrest.equal_to([]),
        step=u'Проверяем нотификацию после отрыва промокода'
    )

    promo_consume = db.balance().execute('select * from t_consume where invoice_id = :invoice_id and archive = 0 '
                                         'and parent_order_id = :order_id',
                                         {'invoice_id': invoice_params_with_promocode.invoice_id,
                                          'order_id': invoice_params_with_promocode.order_id})[0]

    utils.check_that(promo_consume, has_entries_casted({'current_qty': '0.00005',
                                                        'consume_qty': '0.00005'}),
                     step=u'Проверяем, что c нулевого конзюма снимаются промокодные средства')


def test_full_champaign_new_promocode():
    with reporter.step(u'Выставляем счет с промокодом'):
        invoice_params = prepare_invoice_new_promocode()

    utils.check_that(
        steps.CommonSteps.build_notification(OPCODE_PROMOCODE, object_id=invoice_params.order_id
                                             )['args'][0]['Promocodes'][0],
        has_entries_casted({'UnusedPromocodeQty': '0.3492',
                            'AvailablePromocodeQty': '0.3492'}),
        step=u'Проверяем нотификацию по промокоду')

    with reporter.step(u'Полностью откручиваем'):
        steps.CampaignsSteps.do_campaigns(invoice_params.service_id,
                                          invoice_params.service_order_id,
                                          {'Money': 6.3492}, 0, dt)

    utils.check_that(
        steps.CommonSteps.build_notification(OPCODE_PROMOCODE, object_id=invoice_params.order_id
                                             )['args'][0]['Promocodes'],
        hamcrest.equal_to([]),
        step=u'Проверяем нотификацию после откручивания счета с промокодом')


def test_tear_new_promocode():
    with reporter.step(u'Выставляем счет с промокодом'):
        invoice_params = prepare_invoice_new_promocode()

    steps.PromocodeSteps.tear_off_promocode(
        client_id=invoice_params.client_id,
        promocode_id=invoice_params.promocode_id,
    )

    utils.check_that(
        steps.CommonSteps.build_notification(
            OPCODE_PROMOCODE,
            object_id=invoice_params.order_id
        )['args'][0]['Promocodes'],
        hamcrest.equal_to([]),
        step=u'Проверяем нотификацию после отрыва промокода'
    )


def test_tear_new_promocode_champaigned_equal_to_acted():
    with reporter.step(u'Выставляем счет с промокодом, частично откручиваем и актим'):
        invoice_params = prepare_invoice_new_promocode()

        steps.CampaignsSteps.do_campaigns(invoice_params.service_id,
                                          invoice_params.service_order_id,
                                          {'Money': 3.1746}, 0, dt)
        act_id = steps.ActsSteps.generate(invoice_params.client_id, force=1, date=dt)[0]

    utils.check_that(
        steps.CommonSteps.build_notification(OPCODE_PROMOCODE, object_id=invoice_params.order_id
                                             )['args'][0]['Promocodes'][0],
        has_entries_casted({'UnusedPromocodeQty': '0.1746',
                            'AvailablePromocodeQty': '0.1746'}),
        step=u'Проверяем нотификацию до отрыва промокода')

    steps.PromocodeSteps.tear_off_promocode(
        client_id=invoice_params.client_id,
        promocode_id=invoice_params.promocode_id,
    )

    utils.check_that(
        steps.CommonSteps.build_notification(
            OPCODE_PROMOCODE,
            object_id=invoice_params.order_id
        )['args'][0]['Promocodes'],
        hamcrest.equal_to([]),
        step=u'Проверяем нотификацию после отрыва промокода'
    )


def test_tear_new_promocode_champaigned_more_than_acted():
    with reporter.step(u'Выставляем счет с промокодом, частично откручиваем и актим'):
        invoice_params = prepare_invoice_new_promocode()

        steps.CampaignsSteps.do_campaigns(invoice_params.service_id,
                                          invoice_params.service_order_id,
                                          {'Money': 2.1746}, 0, dt)
        act_id = steps.ActsSteps.generate(invoice_params.client_id, force=1, date=dt)[0]

    with reporter.step(u'Увеличиваем количетсво откруток'):
        steps.CampaignsSteps.do_campaigns(invoice_params.service_id,
                                          invoice_params.service_order_id,
                                          {'Money': 3.1746}, 0, dt)

    steps.PromocodeSteps.tear_off_promocode(
        client_id=invoice_params.client_id,
        promocode_id=invoice_params.promocode_id,
    )

    utils.check_that(
        steps.CommonSteps.build_notification(
            OPCODE_PROMOCODE,
            object_id=invoice_params.order_id
        )['args'][0]['Promocodes'],
        hamcrest.equal_to([]),
        step=u'Проверяем нотификацию после отрыва промокода'
    )


def test_tear_new_promocode_champaigned_less_than_acted():
    with reporter.step(u'Выставляем счет с промокодом, частично откручиваем и актим'):
        invoice_params = prepare_invoice_new_promocode()

        steps.CampaignsSteps.do_campaigns(invoice_params.service_id,
                                          invoice_params.service_order_id,
                                          {'Money': 3.1746}, 0, dt)
        act_id = steps.ActsSteps.generate(invoice_params.client_id, force=1, date=dt)[0]

    with reporter.step(u'Уменьшаем количество откруток'):
        steps.CampaignsSteps.do_campaigns(invoice_params.service_id,
                                          invoice_params.service_order_id,
                                          {'Money': 2.1746}, 0, dt)

    utils.check_that(
        steps.CommonSteps.build_notification(OPCODE_PROMOCODE, object_id=invoice_params.order_id
                                             )['args'][0]['Promocodes'][0],
        has_entries_casted({'UnusedPromocodeQty': '0.2296',
                            'AvailablePromocodeQty': '0.1746'}),
        step=u'Проверяем нотификацию после отрыва промокода')

    steps.PromocodeSteps.tear_off_promocode(
        client_id=invoice_params.client_id,
        promocode_id=invoice_params.promocode_id,
    )

    utils.check_that(
        steps.CommonSteps.build_notification(OPCODE_PROMOCODE, object_id=invoice_params.order_id
                                             )['args'][0]['Promocodes'][0],
        has_entries_casted({'UnusedPromocodeQty': '0.0550',
                            'AvailablePromocodeQty': '0.0000'}),
        step=u'Проверяем нотификацию после отрыва промокода')


def prepare_invoice(qty=10, promocode_bonus=20, client_id=None):
    context = DIRECT_YANDEX_FIRM_FISH

    client_id = client_id or steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, context.person_type.code)
    request_params = {}
    promocode_id = None

    if promocode_bonus:
        promo_resp, = steps.PromocodeSteps.create_new(
            calc_class_name=PromocodeClass.LEGACY_PROMO,
            calc_params=steps.PromocodeSteps.fill_calc_params(
                promocode_type=PromocodeClass.LEGACY_PROMO,
                middle_dt=dt_1_day_before + datetime.timedelta(seconds=1),
                bonus1=promocode_bonus,
                bonus2=promocode_bonus,
                minimal_qty=0,
                discount_pct=0
            ),
            promocodes=[steps.PromocodeSteps.generate_code()],
            start_dt=dt_1_day_before,
            end_dt=dt_1_day_after,
            service_ids=[context.service.id],
            firm_id=context.firm.id
        )
        promocode_id = promo_resp['id']
        promocode_code = promo_resp['code']
        steps.PromocodeSteps.make_reservation(client_id, promocode_id, dt_1_day_before, dt_1_day_after)
        # request_params = {'PromoCode': promocode_code}

    service_order_id = steps.OrderSteps.next_id(service_id=context.service.id)
    order_id = steps.OrderSteps.create(client_id=client_id, product_id=context.product.id,
                                       service_id=context.service.id,
                                       service_order_id=service_order_id, params={'AgencyID': None})
    orders_list = [{'ServiceID': context.service.id, 'ServiceOrderID': service_order_id, 'Qty': qty, 'BeginDT': dt}]
    request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list,
                                           additional_params=request_params)

    invoice_id, invoice_external_id, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id,
                                                                   paysys_id=context.paysys.id, credit=0,
                                                                   contract_id=None, overdraft=0,
                                                                   endbuyer_id=None)
    steps.InvoiceSteps.pay(invoice_id)

    return invoice(client_id=client_id, service_id=context.service.id, promocode_id=promocode_id,
                   service_order_id=service_order_id, order_id=order_id, invoice_id=invoice_id)


def prepare_invoice_new_promocode():
    context = DIRECT_YANDEX_FIRM_RUB

    client_id = steps.ClientSteps.create()
    if context.product.type == ProductTypes.MONEY:
        steps.ClientSteps.migrate_to_currency(client_id=client_id, currency_convert_type='COPY', dt=dt_1_day_before,
                                              currency=context.currency.iso_code, region_id=context.region.id
                                              )
    calc_params = fill_calc_params_fixed_discount(discount_pct=5.5, apply_on_create=True,
                                                  adjust_quantity=True)
    promocode_id, promocode_code = create_and_reserve_promocode(calc_class_name=PromocodeClass.FIXED_DISCOUNT,
                                                                client_id=client_id,
                                                                firm_id=context.firm.id,
                                                                calc_params=calc_params,
                                                                end_dt=dt_1_day_after,
                                                                service_ids=[context.service.id],
                                                                start_dt=dt_1_day_before)
    service_order_id, order_id = create_order(context=context, agency_id=None, client_id=client_id)
    orders_list = [{'ServiceID': context.service.id, 'ServiceOrderID': service_order_id, 'Qty': Decimal('6'),
                    'BeginDT': dt}]
    request_id, _ = create_request(context, client_id, orders_list=orders_list)
    # utils.check_that(is_request_with_promocode(promocode_id, request_id), hamcrest.equal_to(True))
    person_id = steps.PersonSteps.create(client_id, context.person_type.code)
    invoice_id, invoice_external_id, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id,
                                                                   paysys_id=context.paysys.id,
                                                                   credit=0, contract_id=None, overdraft=0,
                                                                   endbuyer_id=None)
    steps.InvoiceSteps.pay(invoice_id)
    return invoice(client_id=client_id, service_id=context.service.id, promocode_id=promocode_id,
                   service_order_id=service_order_id, order_id=order_id, invoice_id=invoice_id)
