# coding: utf-8

import datetime

import pytest
from decimal import Decimal
from dateutil.relativedelta import relativedelta

from balance import balance_steps as steps
from btestlib import utils as utils
from btestlib.constants import Services
from temp.igogor.balance_objects import Contexts, Products, Firms, Paysyses, PersonTypes
from btestlib.matchers import contains_dicts_with_entries, has_entries_casted

MARKET_MARKET_FIRM_FISH = Contexts.DIRECT_FISH_RUB_CONTEXT.new(service=Services.MARKET, product=Products.MARKET,
                                                               firm=Firms.MARKET_111)
DIRECT_CONTEXT = Contexts.DIRECT_FISH_RUB_CONTEXT.new(firm=Firms.YANDEX_1)

OVERDRAFT_LIMIT = Decimal('1000')
OVERDRAFT_DT = datetime.datetime.today() - relativedelta(months=1, days=15)
TODAY = datetime.datetime.today()
QTY_OVERDRAFT = Decimal('30')
QTY_1 = Decimal('15')
QTY_2 = Decimal('60')
QTY_3 = Decimal('30')


@pytest.mark.parametrize('context, person_type, paysys', [
                             (MARKET_MARKET_FIRM_FISH, PersonTypes.UR, Paysyses.BANK_UR_RUB_MARKET),
                             (DIRECT_CONTEXT, PersonTypes.PH, Paysyses.BANK_PH_RUB)
                        ])
def test_overdraft_repayment_by_prepaid(context, person_type, paysys, get_free_user):
    client_id = steps.ClientSteps.create()
    user = get_free_user()
    steps.ClientSteps.link(client_id, user.login)

    # выдаем овердрафт
    steps.OverdraftSteps.set_force_overdraft(client_id, context.service.id, OVERDRAFT_LIMIT,
                                             context.firm.id)

    # создаем реквест и счет
    request_id, service_order_ids_overdraft = create_request(context, client_id, QTY_OVERDRAFT)
    person_id = steps.PersonSteps.create(client_id, person_type.code)
    invoice_id_overdraft, _, _ = steps.InvoiceSteps.create(request_id, person_id, paysys.id, overdraft=1)

    # откручиваем и закрываем актом
    for service_order_id in service_order_ids_overdraft:
        steps.CampaignsSteps.do_campaigns(context.service.id, service_order_id,
                                          {'Bucks': QTY_OVERDRAFT}, 0, OVERDRAFT_DT)
    steps.ActsSteps.generate(client_id)

    invoice_data = steps.InvoiceSteps.get_invoice_data_by_client(client_id)
    price_multiplier_overdraft = context.price * len(service_order_ids_overdraft)
    expected_invoice_data_overdraft = create_expected_invoice_data(paysys.id, person_id, QTY_OVERDRAFT,
                                                                   price_multiplier_overdraft, is_acted=True)

    utils.check_that(invoice_data, contains_dicts_with_entries([expected_invoice_data_overdraft]),
                     u'Сравниваем данные из овердрафтного инвойса')

    # создаем еще реквест и счет на маленькую сумму, на перекрытие овердрафта не хватает
    request_id, service_order_ids = create_request(context, client_id, QTY_1)
    invoice_id_1, _, _ = steps.InvoiceSteps.create(request_id, person_id, paysys.id)
    steps.InvoiceSteps.pay_fair(invoice_id_1)

    price_multiplier_prepayment = context.price * len(service_order_ids)
    invoice_data = steps.InvoiceSteps.get_all_invoice_data_by_id(invoice_id_1)
    expected_invoice_data_1 = create_expected_invoice_data(paysys.id, person_id, Decimal('0'),
                                                           price_multiplier_prepayment)

    utils.check_that(invoice_data, has_entries_casted(expected_invoice_data_1),
                     u'Сравниваем данные из первого предоплатного инвойсов')

    # и еще создаем, в сумме перекрываем овердрафт
    request_id, service_order_ids = create_request(context, client_id, QTY_2, service_order_ids=service_order_ids)
    invoice_id_2, _, _ = steps.InvoiceSteps.create(request_id, person_id, paysys.id)
    steps.InvoiceSteps.pay_fair(invoice_id_2)

    invoice_data = steps.InvoiceSteps.get_all_invoice_data_by_id(invoice_id_2)
    expected_invoice_data_2 = create_expected_invoice_data(paysys.id, person_id, Decimal('0'),
                                                           price_multiplier_prepayment)
    utils.check_that(invoice_data, has_entries_casted(expected_invoice_data_2),
                     u'Сравниваем данные из второго предоплатного инвойсов')

    # и еще создаем, на предыдущем шаге хватило на овердрафт, поэтому здесь деньги зачислятся
    request_id, service_order_ids = create_request(context, client_id, QTY_3, service_order_ids=service_order_ids)
    invoice_id_3, _, _ = steps.InvoiceSteps.create(request_id, person_id, paysys.id)
    steps.InvoiceSteps.pay_fair(invoice_id_3)

    invoice_data = steps.InvoiceSteps.get_all_invoice_data_by_id(invoice_id_3)
    expected_invoice_data_3 = create_expected_invoice_data(paysys.id, person_id, QTY_3,
                                                           price_multiplier_prepayment)
    utils.check_that(invoice_data, has_entries_casted(expected_invoice_data_3),
                     u'Сравниваем данные из третьего предоплатного инвойсов')

    steps.InvoiceSteps.pay_fair(invoice_id_overdraft)

    # проверяем, что после оплаты овердрафтного счета все суммы зачислились на соответствующие счета
    expected_invoice_data_1.update({'consume_sum': QTY_1 * price_multiplier_prepayment})
    expected_invoice_data_2.update({'consume_sum': QTY_2 * price_multiplier_prepayment})
    invoice_data = steps.InvoiceSteps.get_invoice_data_by_client(client_id)
    expected_invoice_data = [
        expected_invoice_data_overdraft,
        expected_invoice_data_1,
        expected_invoice_data_2,
        expected_invoice_data_3
    ]
    utils.check_that(invoice_data, contains_dicts_with_entries(expected_invoice_data),
                     u'Сравниваем данные из по всем инвойсам')


# плательщики в овердрафтном и предоплатном счетах не совпадают
def test_overdraft_repayment_by_prepaid_other_payer(get_free_user):
    client_id = steps.ClientSteps.create()
    user = get_free_user()
    steps.ClientSteps.link(client_id, user.login)

    # выдаем овердрафт
    steps.OverdraftSteps.set_force_overdraft(client_id, DIRECT_CONTEXT.service.id, OVERDRAFT_LIMIT,
                                             DIRECT_CONTEXT.firm.id)

    # создаем реквест и счет
    request_id, service_order_ids_overdraft = create_request(DIRECT_CONTEXT, client_id, QTY_OVERDRAFT)
    person_id_overdraft = steps.PersonSteps.create(client_id, PersonTypes.UR.code)
    invoice_id_overdraft, _, _ = steps.InvoiceSteps.create(request_id, person_id_overdraft, Paysyses.BANK_UR_RUB.id,
                                                           overdraft=1)

    # откручиваем и закрываем актами
    for service_order_id in service_order_ids_overdraft:
        steps.CampaignsSteps.do_campaigns(DIRECT_CONTEXT.service.id, service_order_id,
                                          {'Bucks': QTY_OVERDRAFT}, 0, OVERDRAFT_DT)
    steps.ActsSteps.generate(client_id)

    invoice_data_overdraft = steps.InvoiceSteps.get_invoice_data_by_client(client_id)
    expected_invoice_data_overdraft = create_expected_invoice_data(Paysyses.BANK_UR_RUB.id, person_id_overdraft,
                                                                   QTY_OVERDRAFT,
                                                                   DIRECT_CONTEXT.price * len(
                                                                       service_order_ids_overdraft),
                                                                   is_acted=True)

    utils.check_that(invoice_data_overdraft, contains_dicts_with_entries([expected_invoice_data_overdraft]),
                     u'Сравниваем данные из овердрафтного инвойса')

    # создаем еще реквест и счет, но с другим плательщиком
    request_id, service_order_ids = create_request(DIRECT_CONTEXT, client_id, QTY_1)
    person_id_prepayment = steps.PersonSteps.create(client_id, PersonTypes.UR.code)
    invoice_id_prepayment, _, _ = steps.InvoiceSteps.create(request_id, person_id_prepayment, Paysyses.BANK_UR_RUB.id)
    steps.InvoiceSteps.pay_fair(invoice_id_prepayment)

    invoice_data_prepayment = steps.InvoiceSteps.get_all_invoice_data_by_id(invoice_id_prepayment)
    expected_invoice_data = create_expected_invoice_data(Paysyses.BANK_UR_RUB.id, person_id_prepayment, QTY_1,
                                                           DIRECT_CONTEXT.price * len(
                                                               service_order_ids))
    utils.check_that(invoice_data_prepayment, has_entries_casted(expected_invoice_data),
                     u'Проверяем, что вся сумма зачислилась на предоплатный счет')


def test_overdraft_repayment_by_prepaid_other_service(get_free_user):
    client_id = steps.ClientSteps.create()
    user = get_free_user()
    steps.ClientSteps.link(client_id, user.login)

    # выдаем овердрафт
    steps.OverdraftSteps.set_force_overdraft(client_id, DIRECT_CONTEXT.service.id, OVERDRAFT_LIMIT,
                                             DIRECT_CONTEXT.firm.id)

    # создаем реквест и счет
    request_id, service_order_ids_overdraft = create_request(DIRECT_CONTEXT, client_id, QTY_OVERDRAFT)
    person_id = steps.PersonSteps.create(client_id, PersonTypes.UR.code)
    invoice_id_overdraft, _, _ = steps.InvoiceSteps.create(request_id, person_id, Paysyses.BANK_UR_RUB.id,
                                                           overdraft=1)

    # откручиваем и закрываем актами
    for service_order_id in service_order_ids_overdraft:
        steps.CampaignsSteps.do_campaigns(DIRECT_CONTEXT.service.id, service_order_id,
                                          {'Bucks': QTY_OVERDRAFT}, 0, OVERDRAFT_DT)
    steps.ActsSteps.generate(client_id)

    invoice_data_overdraft = steps.InvoiceSteps.get_invoice_data_by_client(client_id)
    expected_invoice_data_overdraft = create_expected_invoice_data(Paysyses.BANK_UR_RUB.id, person_id,
                                                                   QTY_OVERDRAFT,
                                                                   DIRECT_CONTEXT.price * len(
                                                                       service_order_ids_overdraft),
                                                                   is_acted=True)

    utils.check_that(invoice_data_overdraft, contains_dicts_with_entries([expected_invoice_data_overdraft]),
                     u'Сравниваем данные из овердрафтного инвойса')

    # создаем еще реквест и счет, но на другой сервис
    request_id, service_order_ids = create_request(MARKET_MARKET_FIRM_FISH, client_id, QTY_1)
    invoice_id_prepayment, _, _ = steps.InvoiceSteps.create(request_id, person_id, Paysyses.BANK_UR_RUB_MARKET.id)
    steps.InvoiceSteps.pay_fair(invoice_id_prepayment)

    invoice_data_prepayment = steps.InvoiceSteps.get_all_invoice_data_by_id(invoice_id_prepayment)
    expected_invoice_data = create_expected_invoice_data(Paysyses.BANK_UR_RUB_MARKET.id, person_id, QTY_1,
                                                         MARKET_MARKET_FIRM_FISH.price * len(
                                                               service_order_ids))
    utils.check_that(invoice_data_prepayment, has_entries_casted(expected_invoice_data),
                     u'Проверяем, что вся сумма зачислилась на предоплатный счет')


def test_instant_overdraft(get_free_user):
    client_id = steps.ClientSteps.create()
    user = get_free_user()
    steps.ClientSteps.link(client_id, user.login)

    # выдаем овердрафт
    steps.OverdraftSteps.set_force_overdraft(client_id, DIRECT_CONTEXT.service.id, OVERDRAFT_LIMIT,
                                             DIRECT_CONTEXT.firm.id)

    # создаем реквест и счет
    request_id, service_order_ids_overdraft = create_request(DIRECT_CONTEXT, client_id, QTY_OVERDRAFT, dt=TODAY)
    person_id = steps.PersonSteps.create(client_id, PersonTypes.PH.code)
    invoice_id_overdraft, _, _ = steps.InvoiceSteps.create(request_id, person_id, Paysyses.YM_PH_RUB.id,
                                                           overdraft=1)

    # откручиваем и закрываем актами
    for service_order_id in service_order_ids_overdraft:
        steps.CampaignsSteps.do_campaigns(DIRECT_CONTEXT.service.id, service_order_id,
                                          {'Bucks': QTY_OVERDRAFT}, 0, TODAY)
    steps.ActsSteps.generate(client_id)

    # включаем счет
    steps.InvoiceSteps.turn_on(invoice_id_overdraft)

    # создаем еще реквест и счет на маленькую сумму, должно зачислиться на предоплатный счет
    request_id, service_order_ids = create_request(DIRECT_CONTEXT, client_id, QTY_1)
    invoice_id_prepayment, _, _ = steps.InvoiceSteps.create(request_id, person_id, Paysyses.BANK_PH_RUB.id)
    steps.InvoiceSteps.pay_fair(invoice_id_prepayment)

    price_multiplier_prepayment = DIRECT_CONTEXT.price * len(service_order_ids)
    invoice_data = steps.InvoiceSteps.get_all_invoice_data_by_id(invoice_id_prepayment)
    expected_invoice_data_prepayment = create_expected_invoice_data(Paysyses.BANK_PH_RUB.id, person_id, QTY_1,
                                                                    price_multiplier_prepayment)

    utils.check_that(invoice_data, has_entries_casted(expected_invoice_data_prepayment),
                     u'Проверяем, что деньги зачислились на предоплатный счет')


def create_request(context, client_id, qty, dt=OVERDRAFT_DT, service_order_ids=None):
    if not service_order_ids:
        service_order_ids = [steps.OrderSteps.next_id(MARKET_MARKET_FIRM_FISH.service.id) for i in range(5)]
    for i in range(5):
        steps.OrderSteps.create(client_id, service_order_ids[i], product_id=context.product.id,
                                service_id=context.service.id)
    order_list = [{'ServiceOrderID': service_order_id,
                   'ServiceID': context.service.id,
                   'Qty': qty} for service_order_id in service_order_ids]
    request_id = steps.RequestSteps.create(client_id, order_list,
                                           additional_params={'InvoiceDesireDT': dt})
    return request_id, service_order_ids


def create_expected_invoice_data(paysys_id, person_id, qty, price_multiplier, is_acted=False):
    expected_invoice_data = {'paysys_id': paysys_id,
                             'contract_id': None,
                             'person_id': person_id,
                             'consume_sum': qty * price_multiplier,
                             'total_act_sum': qty * price_multiplier if is_acted else Decimal('0')}
    return expected_invoice_data
