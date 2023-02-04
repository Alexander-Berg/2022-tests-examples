# -*- coding: utf-8 -*-
import datetime
import json
import hamcrest
import pytest

import balance.balance_api as api
from balance import balance_db as db
from balance import balance_steps as steps
from btestlib import utils, reporter
from dateutil.relativedelta import relativedelta
from btestlib.constants import Products, Paysyses, Services, Firms, ContractPaymentType, ContractCommissionType, \
    ContractCreditType, PersonTypes, PaymentMethods, Currencies, Regions, Nds, Collateral
from balance.features import Features
from temp.igogor.balance_objects import Contexts
from decimal import Decimal as D

pytestmark = [reporter.feature(Features.OVERDRAFT, Features.NOTIFICATION),
              pytest.mark.tickets('BALANCE-28633,BALANCE-29318,BALANCE-29715,BALANCE-30527,BALANCE-30610,'
                                  'BALANCE-31064')]

LAST_DAY_OF_PREVIOUS_MONTH = datetime.datetime.now().replace(day=1) - datetime.timedelta(days=1)
OVERDRAFT_OPCODE = 11
CLIENT_OPCODE = 10
ORDER_OPCODE = 1

DT = LAST_DAY_OF_PREVIOUS_MONTH

DIRECT_RUB = Contexts.DIRECT_MONEY_RUB_CONTEXT.new(firm=Firms.YANDEX_1, person_type=PersonTypes.UR,
                                                   currency=Currencies.RUB, region=Regions.RU,
                                                   paysys=Paysyses.BANK_UR_RUB,
                                                   card_paysys=Paysyses.CC_UR_RUB, card_limit=49999.99,
                                                   product=Products.DIRECT_RUB,
                                                   nds=Nds.YANDEX_RESIDENT, is_quasi=0)

DIRECT_KZ = DIRECT_RUB.new(firm=Firms.KZ_25, person_type=PersonTypes.KZU, currency=Currencies.KZT,
                           region=Regions.KZ, paysys=Paysyses.BANK_KZ_UR_TG,
                           card_paysys=Paysyses.CC_KZ_UR_TG, card_limit=1400000,
                           product=Products.DIRECT_KZT, nds=Nds.KAZAKHSTAN, is_quasi=0)

DIRECT_KZ_QUASI = DIRECT_RUB.new(firm=Firms.KZ_25, person_type=PersonTypes.KZU,
                                 currency=Currencies.KZT, region=Regions.KZ,
                                 paysys=Paysyses.BANK_KZ_UR_TG,
                                 card_paysys=Paysyses.CC_KZ_UR_TG, card_limit=1400000,
                                 product=Products.DIRECT_KZT_QUASI, nds=Nds.KAZAKHSTAN, is_quasi=1)

DIRECT_BEL = DIRECT_RUB.new(firm=Firms.REKLAMA_BEL_27, person_type=PersonTypes.BYU,
                            currency=Currencies.BYN, region=Regions.BY,
                            paysys=Paysyses.BANK_BY_UR_BYN, card_paysys=Paysyses.CC_BY_UR_BYN,
                            product=Products.DIRECT_BYN, card_limit=None, nds=Nds.BELARUS, is_quasi=1)


@pytest.mark.parametrize('context', [
    DIRECT_RUB,
    DIRECT_KZ,
    DIRECT_KZ_QUASI,
    DIRECT_BEL
])
@pytest.mark.parametrize('autooverdraft_limit', [
    0,
    1000
])
def test_set_overdraft_params(context, autooverdraft_limit):
    limit = 10000
    default_payment_method = 'bank'

    client_id = steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, context.person_type.code)

    answer = get_autooverdraft(context, client_id, person_id, limit, autooverdraft_limit)

    utils.check_that(answer, hamcrest.equal_to([0, 'SUCCESS']), step=u"Проверяем ответ ручки")

    params = db.balance().execute("SELECT * FROM bo.t_overdraft_params WHERE client_id =:item",
                                  {'item': client_id})[0]

    utils.check_that(params['client_limit'], hamcrest.equal_to(autooverdraft_limit),
                     step=u"Проверяем значение {} в таблице bo.t_overdraft_params".format('client_limit'))
    utils.check_that(params['client_id'], hamcrest.equal_to(client_id),
                     step=u"Проверяем значение {} в таблице bo.t_overdraft_params".format('client_id'))
    utils.check_that(params['person_id'], hamcrest.equal_to(person_id),
                     step=u"Проверяем значение {} в таблице bo.t_overdraft_params".format('person_id'))
    utils.check_that(params['service_id'], hamcrest.equal_to(context.service.id),
                     step=u"Проверяем значение {} в таблице bo.t_overdraft_params".format('service_id'))
    utils.check_that(params['hidden'], hamcrest.equal_to(None),
                     step=u"Проверяем значение {} в таблице bo.t_overdraft_params".format('hidden'))
    utils.check_that(params['iso_currency'], hamcrest.equal_to(context.currency.iso_code),
                     step=u"Проверяем значение {} в таблице bo.t_overdraft_params".format('iso_currency'))
    utils.check_that(params['dt'].strftime("%d.%m.%y"), hamcrest.equal_to(datetime.datetime.now().strftime("%d.%m.%y")),
                     step=u"Проверяем значение {} в таблице bo.t_overdraft_params".format('dt'))
    utils.check_that(params['payment_method_cc'], hamcrest.equal_to(default_payment_method),
                     step=u"Проверяем значение {} в таблице bo.t_overdraft_params".format('payment_method_cc'))


@pytest.mark.parametrize('context', [
    DIRECT_RUB,
    DIRECT_KZ,
    DIRECT_KZ_QUASI,
    DIRECT_BEL
])
def test_get_overdraft_params(context):
    payment_method = 'bank'
    limit = 10000
    autooverdraft_limit = limit / 2

    client_id = steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, context.person_type.code)

    get_autooverdraft(context, client_id, person_id, limit, autooverdraft_limit)

    params = steps.OverdraftSteps.get_overdraft_params(client_id)

    utils.check_that(params['ClientLimit'], hamcrest.equal_to(str(autooverdraft_limit)),
                     step=u"Проверяем значение {} в ответе ручки GetOverdraftParams".format('ClientLimit'))
    utils.check_that(params['PersonID'], hamcrest.equal_to(person_id),
                     step=u"Проверяем значение {} в ответе ручки GetOverdraftParams".format('PersonID'))
    utils.check_that(params['Currency'], hamcrest.equal_to(context.currency.iso_code),
                     step=u"Проверяем значение {} в ответе ручки GetOverdraftParams".format('Currency'))
    utils.check_that(params['DT'].strftime("%d.%m.%y"), hamcrest.equal_to(datetime.datetime.now().strftime("%d.%m.%y")),
                     step=u"Проверяем значение {} в ответе ручки GetOverdraftParams".format('DT'))
    utils.check_that(params['PaymentMethodCC'], hamcrest.equal_to(payment_method),
                     step=u"Проверяем значение {} в ответе ручки GetOverdraftParams".format('PaymentMethodCC'))


@pytest.mark.parametrize('context', [
    DIRECT_RUB,
    # DIRECT_KZ,
    # DIRECT_KZ_QUASI,
    DIRECT_BEL
])
def test_t_overdraft_params_history_and_getparams_after_change(context):
    payment_method_cc = 'bank'
    limit = 10000

    client_id = steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, context.person_type.code)

    get_autooverdraft(context, client_id, person_id, limit, limit)

    with reporter.step(u'Получаем параметры овердрафта из таблицы t_overdraft_params'):
        params_0 = db.balance().execute("SELECT * FROM bo.t_overdraft_params WHERE client_id =:item",
                                        {'item': client_id})[0]

    with reporter.step(u'Получаем историю параметров овердрафта из таблицы t_overdraft_params_history'):
        history = db.balance().execute("SELECT ID FROM bo.t_overdraft_params_history WHERE client_id =:item",
                                       {'item': client_id})

    utils.check_that(len(history), hamcrest.equal_to(0), step=u"Проверяем, что история изначально пуста")

    with reporter.step(u'Меняем лимит автоовердрафта'):
        new_limit = limit / 2
        steps.OverdraftSteps.set_overdraft_params(person_id=person_id, client_limit=new_limit,
                                                  iso_currency=context.currency.iso_code)

    with reporter.step(u'Получаем историю параметров овердрафта из таблицы t_overdraft_params_history'):
        history = db.balance().execute("SELECT * FROM bo.t_overdraft_params_history WHERE client_id =:item",
                                       {'item': client_id})
        utils.check_that(len(history), hamcrest.equal_to(1), step=u"Проверяем, что история содержит только 1 запись")

    utils.check_that(history[0]['overdraft_params_id'], hamcrest.equal_to(params_0['id']),
                     step=u"Проверяем значение {} в таблице t_overdraft_params_history".format('overdraft_params_id'))
    utils.check_that(history[0]['client_limit'], hamcrest.equal_to(params_0['client_limit']),
                     step=u"Проверяем значение {} в таблице t_overdraft_params_history".format('client_limit'))
    utils.check_that(history[0]['client_id'], hamcrest.equal_to(params_0['client_id']),
                     step=u"Проверяем значение {} в таблице t_overdraft_params_history".format('client_id'))
    utils.check_that(history[0]['person_id'], hamcrest.equal_to(params_0['person_id']),
                     step=u"Проверяем значение {} в таблице t_overdraft_params_history".format('person_id'))
    utils.check_that(history[0]['service_id'], hamcrest.equal_to(params_0['service_id']),
                     step=u"Проверяем значение {} в таблице t_overdraft_params_history".format('service_id'))
    utils.check_that(history[0]['iso_currency'], hamcrest.equal_to(params_0['iso_currency']),
                     step=u"Проверяем значение {} в таблице t_overdraft_params_history".format('iso_currency'))
    utils.check_that(history[0]['dt'].strftime("%d.%m.%y"), hamcrest.equal_to(params_0['dt'].strftime("%d.%m.%y")),
                     step=u"Проверяем значение {} в таблице t_overdraft_params_history".format('dt'))
    utils.check_that(history[0]['payment_method_cc'], hamcrest.equal_to(params_0['payment_method_cc']),
                     step=u"Проверяем значение {} в таблице t_overdraft_params_history".format('payment_method_cc'))
    utils.check_that(history[0]['hidden'], hamcrest.equal_to(None),
                     step=u"Проверяем значение {} в таблице t_overdraft_params_history".format('hidden'))

    get_limit = steps.OverdraftSteps.get_overdraft_params(client_id)

    utils.check_that(get_limit['ClientLimit'], hamcrest.equal_to(str(new_limit)),
                     step=u"Проверяем значение {} в ответе ручки GetOverdraftParams".format('ClientLimit'))
    utils.check_that(get_limit['PersonID'], hamcrest.equal_to(person_id),
                     step=u"Проверяем значение {} в ответе ручки GetOverdraftParams".format('PersonID'))
    utils.check_that(get_limit['Currency'], hamcrest.equal_to(context.currency.iso_code),
                     step=u"Проверяем значение {} в ответе ручки GetOverdraftParams".format('Currency'))
    utils.check_that(get_limit['DT'].strftime("%d.%m.%y"),
                     hamcrest.equal_to(datetime.datetime.now().strftime("%d.%m.%y")),
                     step=u"Проверяем значение {} в ответе ручки GetOverdraftParams".format('DT'))
    utils.check_that(get_limit['PaymentMethodCC'], hamcrest.equal_to(payment_method_cc),
                     step=u"Проверяем значение {} в ответе ручки GetOverdraftParams".format('PaymentMethodCC'))


def test_set_overdraft_params_without_overdraft():
    context = DIRECT_RUB
    limit = 10000

    client_id = steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, context.person_type.code)

    with reporter.step(u'Переходим на мультивалютность копированием'):
        steps.ClientSteps.migrate_to_currency(client_id, currency_convert_type='COPY',
                                              dt=LAST_DAY_OF_PREVIOUS_MONTH - relativedelta(days=2),
                                              currency=context.currency.iso_code, region_id=context.region.id)

    try:
        with reporter.step(u'Подключаем автоовердрафт'):
            steps.OverdraftSteps.set_overdraft_params(person_id=person_id, client_limit=limit,
                                                      iso_currency=context.currency.iso_code)
    except Exception, exc:
        utils.check_that(steps.CommonSteps.get_exception_code(exc), hamcrest.equal_to('INVALID_PARAM'))
        utils.check_that(steps.CommonSteps.get_exception_code(exc, tag_name='msg'),
                         hamcrest.equal_to('Invalid parameter for function: Client ' + str(client_id) +
                                           ' doesn\'t have overdraft'))
    else:
        raise utils.TestsError(u"Клиенту доступен автоовердрафт без подключенного овердрафта")


def test_autoovedraft_to_agency_subclient():
    context = DIRECT_RUB
    limit = 1000

    agency_id = steps.ClientSteps.create({'IS_AGENCY': 1})
    person_id = steps.PersonSteps.create(agency_id, context.person_type.code)
    client_id = steps.ClientSteps.create()

    with reporter.step(u'Переходим на мультивалютность копированием'):
        steps.ClientSteps.migrate_to_currency(client_id, currency_convert_type='COPY',
                                              dt=LAST_DAY_OF_PREVIOUS_MONTH - relativedelta(days=5),
                                              currency=context.currency.iso_code, region_id=context.region.id)

    with reporter.step(u'Заключаем договор с агенством'):
        contract_params = {'CLIENT_ID': agency_id,
                           'PERSON_ID': person_id,
                           'DT': utils.Date.date_to_iso_format(datetime.datetime.now() - relativedelta(days=180)),
                           'FINISH_DT': utils.Date.date_to_iso_format(datetime.datetime.now()
                                                                      + relativedelta(days=180)),
                           'IS_SIGNED': utils.Date.date_to_iso_format(datetime.datetime.now()
                                                                      - relativedelta(days=180)),
                           'PAYMENT_TYPE': ContractPaymentType.POSTPAY,
                           'CREDIT_TYPE': ContractCreditType.BY_TERM_AND_SUM}
        _, _ = steps.ContractSteps.create_contract_new(ContractCommissionType.OPT_AGENCY_PREM, contract_params)

    with reporter.step(u'Подключаем овердрафт'):
        steps.OverdraftSteps.set_force_overdraft(client_id, context.service.id, limit, context.firm.id,
                                                 currency=context.currency.iso_code)

    try:
        with reporter.step(u'Подключаем автоовердрафт'):
            steps.OverdraftSteps.set_overdraft_params(person_id=person_id, client_limit=limit)
    except Exception, exc:
        utils.check_that(steps.CommonSteps.get_exception_code(exc), hamcrest.equal_to('INVALID_PARAM'))
        utils.check_that(steps.CommonSteps.get_exception_code(exc, tag_name='msg'),
                         hamcrest.equal_to('Invalid parameter for function: Agencies can\'t have an overdraft'))
    else:
        raise utils.TestsError(u"Субклиенту агенства доступен автоовердрафт")


def test_autoovedraft_to_agency():
    context = DIRECT_RUB
    limit = 10000

    client_id = steps.ClientSteps.create({'IS_AGENCY': 1})
    person_id = steps.PersonSteps.create(client_id, context.person_type.code)

    with reporter.step(u'Переходим на мультивалютность копированием'):
        steps.ClientSteps.migrate_to_currency(client_id, currency_convert_type='COPY',
                                              dt=LAST_DAY_OF_PREVIOUS_MONTH - relativedelta(days=5),
                                              currency=context.currency.iso_code, region_id=context.region.id)

    with reporter.step(u'Подключаем овердрафт'):
        steps.OverdraftSteps.set_force_overdraft(client_id, context.service.id, limit, context.firm.id,
                                                 currency=context.currency.iso_code)

    try:
        with reporter.step(u'Подключаем автоовердрафт'):
            steps.OverdraftSteps.set_overdraft_params(person_id=person_id, client_limit=limit)
    except Exception, exc:
        utils.check_that(steps.CommonSteps.get_exception_code(exc), hamcrest.equal_to('INVALID_PARAM'))
        utils.check_that(steps.CommonSteps.get_exception_code(exc, tag_name='msg'),
                         hamcrest.equal_to('Invalid parameter for function: Agencies can\'t have an overdraft'))
    else:
        raise utils.TestsError(u"Агенству доступен автоовердрафт")


def test_unexist_currency():
    context = DIRECT_RUB
    limit = 10000
    iso_currency = 'RUX'

    client_id = steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, PersonTypes.UR.code)

    with reporter.step(u'Переходим на мультивалютность копированием'):
        steps.ClientSteps.migrate_to_currency(client_id, currency_convert_type='COPY',
                                              dt=LAST_DAY_OF_PREVIOUS_MONTH - relativedelta(days=5),
                                              currency=context.currency.iso_code, region_id=context.region.id)

    with reporter.step(u'Подключаем овердрафт'):
        steps.OverdraftSteps.set_force_overdraft(client_id, context.service.id, limit, context.firm.id,
                                                 currency=context.currency.iso_code)

    try:
        with reporter.step(u'Пробуем подключить автоовердрафт с несмуществующей валютой'):
            steps.OverdraftSteps.set_overdraft_params(person_id=person_id, client_limit=limit,
                                                      iso_currency=iso_currency)
    except Exception, exc:
        utils.check_that(steps.CommonSteps.get_exception_code(exc), hamcrest.equal_to('INVALID_PARAM'))
        utils.check_that(steps.CommonSteps.get_exception_code(exc, tag_name='msg'),
                         hamcrest.equal_to(
                             'Invalid parameter for function: Invalid iso_currency={}'.format(iso_currency)))
    else:
        raise utils.TestsError(u"Клиенту может быть подключен автоовердрафт с несуществующей валютой")


def test_unable_autooverdraft_for_fish_client():
    context = DIRECT_RUB
    limit = 10000

    client_id = steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, context.person_type.code)

    with reporter.step(u'Подключаем овердрафт'):
        steps.OverdraftSteps.set_force_overdraft(client_id, context.service.id, limit, context.firm.id)

    try:
        with reporter.step(u'Пробуем подключить автоовердрафт фишечному клиенту'):
            steps.OverdraftSteps.set_overdraft_params(person_id=person_id, client_limit=limit,
                                                      iso_currency=context.currency.iso_code)
    except Exception, exc:
        utils.check_that(steps.CommonSteps.get_exception_code(exc), hamcrest.equal_to('INVALID_PARAM'))
        utils.check_that(steps.CommonSteps.get_exception_code(exc, tag_name='msg'),
                         hamcrest.equal_to(
                             'Invalid parameter for function: Autooverdraft not allowed for fish clients'))
    else:
        raise utils.TestsError(u"Фишечному клиенту доступен автоовердрафт")

    try:
        with reporter.step(u'Пробуем подключить автоовердрафт фишечному клиенту'):
            steps.OverdraftSteps.set_overdraft_params(person_id=person_id, client_limit=limit,
                                                      iso_currency=None)
    except Exception, exc:
        utils.check_that(steps.CommonSteps.get_exception_code(exc), hamcrest.equal_to('INVALID_PARAM'))
        utils.check_that(steps.CommonSteps.get_exception_code(exc, tag_name='msg'),
                         hamcrest.equal_to(
                             'Invalid parameter for function: Autooverdraft not allowed for fish clients'))
    else:
        raise utils.TestsError(u"Фишечному клиенту доступен автоовердрафт")


@pytest.mark.parametrize('context', [
    DIRECT_RUB,
    # DIRECT_KZ,
    # DIRECT_KZ_QUASI,
    DIRECT_BEL
])
def test_autooverdraft_after_act(context):
    limit = 90
    qty_limit = get_qty_limit(context, limit)
    qty = 600
    completion_qty = 690

    client_id = steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, context.person_type.code)

    get_autooverdraft(context, client_id, person_id, overdraft_limit=limit, autooverdraft_limit=qty_limit)

    with reporter.step(u'Создаем заказы с единым счетом и перекручиваем до лимита'):
        invoice_id, main_order_id, child_order_id = \
            get_orders_and_invoice(context, client_id, person_id, DT, qty, completion_qty)

    with reporter.step(u'Актимся и выставляем автоовердрафтный счет'):
        act_id = steps.ActsSteps.generate(client_id, force=1, date=DT)[0]

        autooverdraft_id = db.balance().execute("SELECT ID FROM bo.t_overdraft_params WHERE client_id =:item",
                                                {'item': client_id})[0]['id']
        steps.CommonSteps.export('AUTO_OVERDRAFT', 'OverdraftParams', autooverdraft_id, with_enqueue=True)

    get_and_check_autooverdraft_invoice_sum(context, client_id, limit, qty_limit, invoice_id)


@pytest.mark.parametrize('context', [
    DIRECT_RUB,
    # DIRECT_KZ,
    # DIRECT_KZ_QUASI,
    DIRECT_BEL
])
@pytest.mark.parametrize('act_force', [0, 1])
def test_act_after_overdraft_export_fail(context, act_force):
    limit = 90
    qty_limit = get_qty_limit(context, limit)
    qty = 600
    completion_qty = 690

    client_id = steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, context.person_type.code)

    get_autooverdraft(context, client_id, person_id, overdraft_limit=limit, autooverdraft_limit=qty_limit)

    with reporter.step(u'Создаем заказы с единым счетом и перекручиваем до лимита'):
        get_orders_and_invoice(context, client_id, person_id, DT, qty, completion_qty)

    with reporter.step(u'Ставим автоовердрафт в очередь'):
        autooverdraft_id = db.balance().execute("SELECT ID FROM bo.t_overdraft_params WHERE client_id =:item",
                                                {'item': client_id})[0]['id']

        api.test_balance().Enqueue('OverdraftParams', autooverdraft_id, 'AUTO_OVERDRAFT')

    with reporter.step(u'Меняем статус обработки автоовердрафта и пытаемся заактиться'):
        db.balance().execute("update t_export set state = 2 where type = 'AUTO_OVERDRAFT' and OBJECT_ID =:item",
                             {'item': autooverdraft_id})

        steps.ActsSteps.enqueue([client_id], force=act_force, date=datetime.datetime.now())
        export_input = (steps.CommonSteps.get_pickled_value('''
        select input from t_export where type = 'MONTH_PROC' and classname = 'Client' and object_id = {}
        '''.format(client_id)))

        with pytest.raises(Exception):
            steps.CommonSteps.export('MONTH_PROC', 'Client', client_id, with_enqueue=True, input_=export_input)

        error_message = db.balance().execute("select error from t_export where type = 'MONTH_PROC' "
                                             "and classname = 'Client' and object_id =:item",
                                             {'item': client_id})[0]['error']
        utils.check_that(error_message,
                         hamcrest.equal_to('Auto overdraft is not processed for params id {}'.format(autooverdraft_id)))


@pytest.mark.parametrize('context', [
    DIRECT_RUB,
    DIRECT_KZ,
    DIRECT_KZ_QUASI,
    DIRECT_BEL
])
def test_overshipment_less_than_limit(context):
    limit = 60
    qty_limit = get_qty_limit(context, limit)
    qty = 600
    completion_qty = 630

    client_id = steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, context.person_type.code)

    get_autooverdraft(context, client_id, person_id, overdraft_limit=limit, autooverdraft_limit=qty_limit)

    with reporter.step(u'Создаем заказы с единым счетом и перекручиваем ниже лимита'):
        invoice_id, main_order_id, child_order_id = \
            get_orders_and_invoice(context, client_id, person_id, DT, qty, completion_qty)

    get_act_and_autooverdraft_invoice(client_id, DT)

    check_client_notification(client_id, qty_limit, overshipment=completion_qty - qty)
    check_order_notification(main_order_id, qty_limit, overshipment=completion_qty - qty,
                             is_notification_expected=True)
    check_order_notification(child_order_id, qty_limit, overshipment=completion_qty - qty,
                             is_notification_expected=False)

    get_and_check_autooverdraft_invoice_sum(context, client_id, limit,
                                            overshipment_qty=completion_qty - qty,
                                            prepay_invoice_id=invoice_id)


@pytest.mark.no_parallel
@pytest.mark.parametrize('context', [
    DIRECT_RUB,
    DIRECT_KZ,
    DIRECT_KZ_QUASI,
    DIRECT_BEL
])
@pytest.mark.parametrize('overcompletion_qty, threshold, autooverdrafted', [
    (10, 5, True),
    (10, 15, False),
    (8, 5, True),
    (8, 15, False),
    (20, 5, True),
    (20, 15, False)
])
def test_overshipment_over_limit_below_threshold(context, overcompletion_qty, threshold, autooverdrafted):
    item = 'AUTO_OVERDRAFT_THRESHOLDS'
    thresholds = {context.currency.iso_code: threshold}
    limit = 10
    qty_limit = get_qty_limit(context, limit)
    qty = 600
    completion_qty = qty + overcompletion_qty

    client_id = steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, context.person_type.code)

    get_autooverdraft(context, client_id, person_id, overdraft_limit=limit, autooverdraft_limit=qty_limit)

    with reporter.step(u'Создаем заказы с единым счетом и перекручиваем'):
        invoice_id, main_order_id, child_order_id = \
            get_orders_and_invoice(context, client_id, person_id, DT, qty, completion_qty)

    try:
        db.balance().execute(
            '''update bo.t_config set value_json = :thresholds where item = :item''',
            {'thresholds': json.dumps(thresholds), 'item': item}
        )
        with reporter.step(u'Пробуем выставить автоовердрафтный счет'):
            autooverdraft_id = db.balance().execute('''SELECT id FROM bo.t_overdraft_params WHERE client_id = :item''',
                                                    {'item': client_id})[0]['id']
            steps.CommonSteps.export('AUTO_OVERDRAFT', 'OverdraftParams', autooverdraft_id, with_enqueue=True)
            if not autooverdrafted:
                get_and_check_autooverdraft_skipped_export(autooverdraft_id, client_id, invoice_id)
            else:
                overshipment = min(qty_limit, overcompletion_qty)
                get_act_and_autooverdraft_invoice(client_id, DT)

                check_client_notification(client_id, qty_limit, overshipment=overshipment)
                check_order_notification(main_order_id, qty_limit, overshipment=overshipment,
                                         is_notification_expected=True)
                check_order_notification(child_order_id, qty_limit, overshipment=overshipment,
                                         is_notification_expected=False)

                get_and_check_autooverdraft_invoice_sum(context, client_id, limit,
                                                        overshipment_qty=overshipment,
                                                        prepay_invoice_id=invoice_id)
    finally:
        db.balance().execute(
            '''update bo.t_config set value_json = :thresholds where item = :item''',
            {'thresholds': '{}', 'item': item}
        )


@pytest.mark.parametrize('context', [
    DIRECT_RUB,
    # DIRECT_KZ,
    # DIRECT_KZ_QUASI,
    # DIRECT_BEL
])
def test_overshipment_less_than_limit_single_account_case(context):
    limit = 60
    qty_limit = get_qty_limit(context, limit)
    qty = 600
    completion_qty = 630

    client_id = steps.ClientSteps.create({'REGION_ID': 225},
                                         single_account_activated=True, enable_single_account=True
                                         )
    person_id = steps.PersonSteps.create(client_id, context.person_type.code)
    single_account_number = steps.ElsSteps.create_els(client_id)
    personal_account_id, _ = steps.ElsSteps.get_ls_by_person_id_and_els_number(person_id=person_id,
                                                                               els_number=single_account_number)
    with reporter.step(u'Меняем дату включения ЛС на прошлый месяц'):
        db.balance().execute(
            "update T_INVOICE set turn_on_dt = date'{}' where id = :invoice_id".format(DT.strftime("%Y-%m-%d")),
            {'invoice_id': personal_account_id}
        )

    get_autooverdraft(context, client_id, person_id, overdraft_limit=limit, autooverdraft_limit=qty_limit)

    service_order_id = steps.OrderSteps.next_id(service_id=context.service.id)
    service_order_id_2 = steps.OrderSteps.next_id(service_id=context.service.id)
    main_order_id = steps.OrderSteps.create(client_id, service_order_id, service_id=context.service.id,
                                            product_id=context.product.id, params={'AgencyID': None})
    child_order_id = steps.OrderSteps.create(client_id, service_order_id_2, service_id=context.service.id,
                                             product_id=context.product.id,
                                             params={'AgencyID': None, 'GroupServiceOrderID': service_order_id})

    orders_list = [{'ServiceID': context.service.id, 'ServiceOrderID': service_order_id_2, 'Qty': qty}]
    request_id = steps.RequestSteps.create(client_id, orders_list, additional_params=dict(InvoiceDesireDT=DT))
    invoice_id, _, _ = steps.InvoiceSteps.create(request_id, person_id, context.paysys.id, credit=0,
                                                 contract_id=None, overdraft=0, endbuyer_id=None)

    payment_id = db.get_payments_by_invoice(invoice_id)[0]['id']
    steps.InvoiceSteps.pay_fair(personal_account_id, payment_sum=600, orig_id=payment_id)

    steps.CampaignsSteps.do_campaigns(context.service.id, service_order_id_2, {'Money': completion_qty}, 0, DT)

    steps.CommonSteps.export('UA_TRANSFER', 'Client', client_id, input_={'for_dt': datetime.datetime.now()})

    get_act_and_autooverdraft_invoice(client_id, DT)

    check_client_notification(client_id, qty_limit, overshipment=completion_qty - qty)
    check_order_notification(main_order_id, qty_limit, overshipment=completion_qty - qty,
                             is_notification_expected=True)
    check_order_notification(child_order_id, qty_limit, overshipment=completion_qty - qty,
                             is_notification_expected=False)

    get_and_check_autooverdraft_invoice_sum(context, client_id, limit,
                                            overshipment_qty=completion_qty - qty,
                                            prepay_invoice_id=invoice_id)


@pytest.mark.parametrize('context', [
    DIRECT_RUB,
    # DIRECT_KZ,
    # DIRECT_KZ_QUASI,
    DIRECT_BEL
])
def test_overshipment_less_than_limit_2_orders(context):
    limit = 60
    qty_limit = get_qty_limit(context, limit)
    qty = 600
    completion_qty = 630

    client_id = steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, context.person_type.code)

    get_autooverdraft(context, client_id, person_id, overdraft_limit=limit, autooverdraft_limit=qty_limit)

    with reporter.step(u'Создаем заказы с единым счетом и перекручиваем ниже лимита'):
        service_order_id = steps.OrderSteps.next_id(service_id=context.service.id)
        service_order_id_2 = steps.OrderSteps.next_id(service_id=context.service.id)
        service_order_id_3 = steps.OrderSteps.next_id(service_id=context.service.id)
        main_order_id = steps.OrderSteps.create(client_id, service_order_id, service_id=context.service.id,
                                                product_id=context.product.id, params={'AgencyID': None})
        child_order_id_1 = steps.OrderSteps.create(client_id, service_order_id_2, service_id=context.service.id,
                                                   product_id=context.product.id,
                                                   params={'AgencyID': None, 'GroupServiceOrderID': service_order_id})
        child_order_id_2 = steps.OrderSteps.create(client_id, service_order_id_3, service_id=context.service.id,
                                                   product_id=context.product.id,
                                                   params={'AgencyID': None, 'GroupServiceOrderID': service_order_id})

        orders_list = [{'ServiceID': context.service.id, 'ServiceOrderID': service_order_id_2, 'Qty': qty / 2},
                       {'ServiceID': context.service.id, 'ServiceOrderID': service_order_id_3, 'Qty': qty / 2}]
        request_id = steps.RequestSteps.create(client_id, orders_list, additional_params=dict(InvoiceDesireDT=DT))
        invoice_id, _, _ = steps.InvoiceSteps.create(request_id, person_id, context.paysys.id, credit=0,
                                                     contract_id=None, overdraft=0, endbuyer_id=None)

        steps.InvoiceSteps.pay(invoice_id)
        steps.InvoiceSteps.set_turn_on_dt(invoice_id, DT)
        steps.CampaignsSteps.do_campaigns(context.service.id, service_order_id_2, {'Money': completion_qty / 2}, 0, DT)
        steps.CampaignsSteps.do_campaigns(context.service.id, service_order_id_3, {'Money': completion_qty / 2}, 0, DT)

        steps.CommonSteps.export('UA_TRANSFER', 'Client', client_id, input_={'for_dt': datetime.datetime.now()})

    get_act_and_autooverdraft_invoice(client_id, DT)

    check_client_notification(client_id, qty_limit, overshipment=completion_qty - qty)
    check_order_notification(main_order_id, qty_limit, overshipment=completion_qty - qty,
                             is_notification_expected=True)
    check_order_notification(child_order_id_1, qty_limit, overshipment=completion_qty - qty,
                             is_notification_expected=False)
    check_order_notification(child_order_id_2, qty_limit, overshipment=completion_qty - qty,
                             is_notification_expected=False)

    get_and_check_autooverdraft_invoice_sum(context, client_id, limit,
                                            overshipment_qty=completion_qty - qty,
                                            prepay_invoice_id=invoice_id)


@pytest.mark.parametrize('context', [
    DIRECT_RUB,
    # DIRECT_KZ,
    # DIRECT_KZ_QUASI,
    DIRECT_BEL
])
def test_overshipment_over_limit(context):
    limit = 90
    qty_limit = get_qty_limit(context, limit)
    qty = 600
    completion_qty = 900

    client_id = steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, context.person_type.code)

    get_autooverdraft(context, client_id, person_id, overdraft_limit=limit, autooverdraft_limit=qty_limit)

    with reporter.step(u'Создаем заказы с единым счетом и перекручиваем сверх лимита'):
        invoice_id, main_order_id, child_order_id = \
            get_orders_and_invoice(context, client_id, person_id, DT, qty, completion_qty)

    get_act_and_autooverdraft_invoice(client_id, DT)

    check_client_notification(client_id, qty_limit, overshipment=qty_limit)
    check_order_notification(main_order_id, qty_limit, overshipment=qty_limit, is_notification_expected=True)
    check_order_notification(child_order_id, qty_limit, overshipment=qty_limit, is_notification_expected=False)

    get_and_check_autooverdraft_invoice_sum(context, client_id, limit,
                                            overshipment_qty=qty_limit,
                                            prepay_invoice_id=invoice_id)


@pytest.mark.parametrize('context', [
    DIRECT_RUB,
    # DIRECT_KZ,
    # DIRECT_KZ_QUASI,
    DIRECT_BEL
])
def test_overshipment_up_to_limit_different_limits(context):
    limit = 90
    qty_limit = get_qty_limit(context, limit)
    autooverdraft_limit = limit / 2
    qty_autooverdraft_limit = get_qty_limit(context, autooverdraft_limit)
    qty = 600
    completion_qty = qty + qty_autooverdraft_limit

    client_id = steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, context.person_type.code)

    get_autooverdraft(context, client_id, person_id,
                      overdraft_limit=limit,
                      autooverdraft_limit=qty_autooverdraft_limit)

    with reporter.step(u'Создаем заказы с единым счетом и перекручиваем сверх лимита'):
        invoice_id, main_order_id, child_order_id = \
            get_orders_and_invoice(context, client_id, person_id, DT, qty, completion_qty)

    get_act_and_autooverdraft_invoice(client_id, DT)

    check_client_notification(client_id, qty_limit,
                              overshipment=qty_autooverdraft_limit)
    check_order_notification(main_order_id, qty_limit,
                             overshipment=qty_autooverdraft_limit,
                             is_notification_expected=True)
    check_order_notification(child_order_id, qty_limit,
                             overshipment=qty_autooverdraft_limit,
                             is_notification_expected=False)

    get_and_check_autooverdraft_invoice_sum(context, client_id, limit,
                                            overshipment_qty=qty_autooverdraft_limit,
                                            prepay_invoice_id=invoice_id)


@pytest.mark.parametrize('context', [
    DIRECT_RUB,
    DIRECT_KZ,
    DIRECT_KZ_QUASI,
    DIRECT_BEL
])
def test_overshipment_up_to_limit(context):
    limit = 90
    qty_limit = get_qty_limit(context, limit)
    qty = 600
    completion_qty = qty + qty_limit

    client_id = steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, context.person_type.code)

    get_autooverdraft(context, client_id, person_id, overdraft_limit=limit, autooverdraft_limit=qty_limit)

    with reporter.step(u'Создаем заказы с единым счетом и перекручиваем до лимита'):
        invoice_id, main_order_id, child_order_id = \
            get_orders_and_invoice(context, client_id, person_id, DT, qty, completion_qty)

    get_act_and_autooverdraft_invoice(client_id, DT)

    check_client_notification(client_id, qty_limit, overshipment=qty_limit)
    check_order_notification(main_order_id, qty_limit, overshipment=qty_limit, is_notification_expected=True)
    check_order_notification(child_order_id, qty_limit, overshipment=qty_limit, is_notification_expected=False)

    get_and_check_autooverdraft_invoice_sum(context, client_id, limit,
                                            overshipment_qty=qty_limit,
                                            prepay_invoice_id=invoice_id)


@pytest.mark.parametrize('product_id', [
    Products.MEDIA_DIRECT_RUB.id,
    508918
])
@pytest.mark.parametrize('payment_method', [
    PaymentMethods.BANK.cc,
    PaymentMethods.CARD.cc
])
def test_media_direct_overshipment(product_id, payment_method):
    context = DIRECT_RUB
    limit = 90
    qty_limit = get_qty_limit(context, limit)
    qty = 600
    completion_qty = 900

    client_id = steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, context.person_type.code)

    get_autooverdraft(context, client_id, person_id, overdraft_limit=limit, autooverdraft_limit=qty_limit,
                      payment_method=payment_method)

    with reporter.step(u'Создаем заказы с единым счетом (продукт одного из дочерних заказов берется из контекста) '
                       u'и перекручиваем сверх лимита'):
        dt = DT

        service_order_id = steps.OrderSteps.next_id(service_id=context.service.id)
        service_order_id_2 = steps.OrderSteps.next_id(service_id=context.service.id)
        service_order_id_3 = steps.OrderSteps.next_id(service_id=context.service.id)
        main_order_id = steps.OrderSteps.create(client_id, service_order_id, service_id=context.service.id,
                                                product_id=context.product.id, params={'AgencyID': None})
        child_order_id = steps.OrderSteps.create(client_id, service_order_id_2, service_id=context.service.id,
                                                 product_id=context.product.id,
                                                 params={'AgencyID': None, 'GroupServiceOrderID': service_order_id})
        child_order_id_media = steps.OrderSteps.create(client_id, service_order_id_3, service_id=context.service.id,
                                                       product_id=product_id,
                                                       params={'AgencyID': None,
                                                               'GroupServiceOrderID': service_order_id})

        orders_list = [{'ServiceID': context.service.id, 'ServiceOrderID': service_order_id_2, 'Qty': qty}]
        request_id = steps.RequestSteps.create(client_id, orders_list, additional_params=dict(InvoiceDesireDT=dt))
        invoice_id_1, _, _ = steps.InvoiceSteps.create(request_id, person_id, context.paysys.id, credit=0,
                                                       contract_id=None, overdraft=0, endbuyer_id=None)
        steps.InvoiceSteps.pay(invoice_id_1)
        steps.InvoiceSteps.set_turn_on_dt(invoice_id_1, dt)
        steps.CampaignsSteps.do_campaigns(context.service.id, service_order_id_2, {'Money': completion_qty}, 0, dt)

        orders_list = [{'ServiceID': context.service.id, 'ServiceOrderID': service_order_id_3, 'Qty': qty}]
        request_id = steps.RequestSteps.create(client_id, orders_list, additional_params=dict(InvoiceDesireDT=dt))
        invoice_id_2, _, _ = steps.InvoiceSteps.create(request_id, person_id, context.paysys.id, credit=0,
                                                       contract_id=None, overdraft=0, endbuyer_id=None)
        steps.InvoiceSteps.pay(invoice_id_2)
        steps.InvoiceSteps.set_turn_on_dt(invoice_id_2, dt)
        steps.CampaignsSteps.do_campaigns(context.service.id, service_order_id_3, {'Money': completion_qty}, 0, dt)

        steps.CommonSteps.export('UA_TRANSFER', 'Client', client_id, input_={'for_dt': datetime.datetime.now()})

    get_act_and_autooverdraft_invoice(client_id, DT)

    check_client_notification(client_id, qty_limit, overshipment=qty_limit)
    check_order_notification(main_order_id, qty_limit, overshipment=qty_limit, is_notification_expected=True)
    check_order_notification(child_order_id, None, None, is_notification_expected=False)
    check_order_notification(child_order_id_media, None, None, is_notification_expected=False)

    query = "SELECT TOTAL_SUM, TOTAL_ACT_SUM " \
            "FROM T_INVOICE " \
            "WHERE REQUEST_ID IN (SELECT ID from T_REQUEST WHERE CLIENT_ID = :client_id)" \
            "AND ID NOT IN (:invoice_id_1, :invoice_id_2)"
    overdraft_invoice_sum = db.balance().execute(query, {'client_id': client_id,
                                                         'invoice_id_1': invoice_id_1,
                                                         'invoice_id_2': invoice_id_2})

    utils.check_that(len(overdraft_invoice_sum), hamcrest.equal_to(1),
                     step=u'Проверяем, что создан только один овердрафтный счет')

    utils.check_that(D(overdraft_invoice_sum[0]['total_sum']), hamcrest.less_than_or_equal_to(D(limit)),
                     step=u'Проверяем, что овердрафтный счет выставлен на сумму не выше лимита')

    overshipment_qty = qty_limit

    utils.check_that(D(overdraft_invoice_sum[0]['total_sum']),
                     hamcrest.equal_to(D(overshipment_qty)
                                       if not context.is_quasi
                                       else overshipment_qty * (1 + D(context.nds) / 100)),
                     step=u'Проверяем, что овердрафтный счет выставлен на сумму перекрута')

    utils.check_that(overdraft_invoice_sum[0]['total_act_sum'],
                     hamcrest.equal_to(overdraft_invoice_sum[0]['total_sum']),
                     step=u'Проверяем, что сумма акта и сумма овердрафтного счета совпадают')


def test_another_service_overshipment():
    context = DIRECT_RUB
    limit = 90
    qty_limit = get_qty_limit(context, limit)
    qty = 600
    direct_order_completion_qty = 640
    market_order_completion_qty = 650

    client_id = steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, context.person_type.code)

    get_autooverdraft(context, client_id, person_id, overdraft_limit=limit, autooverdraft_limit=qty_limit)

    with reporter.step(u'Создаем заказы с единым счетом на Директ и перекручиваем сверх лимита'):
        invoice_id_direct, main_order_id, child_order_id = \
            get_orders_and_invoice(context, client_id, person_id, DT, qty, direct_order_completion_qty)

    with reporter.step(u'Создаем и перекручиваем заказ на Маркет'):
        another_service = Services.MARKET
        another_product = Products.MARKET
        service_order_id_market = steps.OrderSteps.next_id(service_id=another_service.id)
        order_id = steps.OrderSteps.create(client_id, service_order_id_market, service_id=another_service.id,
                                           product_id=another_product.id, params={'AgencyID': None})
        orders_list = [{'ServiceID': another_service.id, 'ServiceOrderID': service_order_id_market, 'Qty': qty / 30}]
        request_id = steps.RequestSteps.create(client_id, orders_list, additional_params=dict(InvoiceDesireDT=DT))
        invoice_id_market, _, _ = steps.InvoiceSteps.create(request_id, person_id, context.paysys.id, credit=0,
                                                            contract_id=None, overdraft=0, endbuyer_id=None)
        steps.InvoiceSteps.pay(invoice_id_market)
        steps.InvoiceSteps.set_turn_on_dt(invoice_id_market, DT)

        steps.CampaignsSteps.do_campaigns(another_service.id, service_order_id_market,
                                          {'Money': market_order_completion_qty}, 0, DT)

    get_act_and_autooverdraft_invoice(client_id, DT)

    query = "SELECT TOTAL_SUM, TOTAL_ACT_SUM " \
            "FROM T_INVOICE " \
            "WHERE REQUEST_ID IN (SELECT ID from T_REQUEST WHERE CLIENT_ID = :client_id)" \
            "AND ID NOT IN (:invoice_id_direct, :invoice_id_market)"
    overdraft_invoice_sum = db.balance().execute(query, {'client_id': client_id,
                                                         'invoice_id_direct': invoice_id_direct,
                                                         'invoice_id_market': invoice_id_market})

    utils.check_that(len(overdraft_invoice_sum), hamcrest.equal_to(1),
                     step=u'Проверяем, что создан только один овердрафтный счет')

    utils.check_that(overdraft_invoice_sum[0]['total_sum'], hamcrest.equal_to(direct_order_completion_qty - qty),
                     step=u'Проверяем, что в овердрафтный счет не попадают перекруты с заказов на другие сервисы')

    utils.check_that(overdraft_invoice_sum[0]['total_act_sum'],
                     hamcrest.equal_to(overdraft_invoice_sum[0]['total_sum']),
                     step=u'Проверяем, что сумма акта и сумма овердрафтного счета совпадают')


def test_fish_overshipment():
    limit = 90.0
    qty = 600.0
    fish_overshipmen = 30.0
    currency_overshipment = 90.0
    total_overshipment = fish_overshipmen + currency_overshipment

    client_id = steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, PersonTypes.UR.code)

    service_id = Services.DIRECT.id
    product_id = Products.DIRECT_FISH.id
    paysys_id = Paysyses.BANK_UR_RUB.id

    with reporter.step(u'Создаем и перекручиваем фишечный заказ'):
        service_order_id_fish = steps.OrderSteps.next_id(service_id=service_id)
        order_id_fish = steps.OrderSteps.create(client_id, service_order_id_fish, service_id=service_id,
                                                product_id=product_id, params={'AgencyID': None})
        orders_list = [
            {'ServiceID': service_id, 'ServiceOrderID': service_order_id_fish, 'Qty': qty / 30, 'BeginDT': DT}]
        request_id = steps.RequestSteps.create(client_id, orders_list, additional_params=dict(InvoiceDesireDT=DT))
        invoice_id_fish, _, _ = steps.InvoiceSteps.create(request_id, person_id, paysys_id, credit=0,
                                                          contract_id=None, overdraft=0, endbuyer_id=None)
        steps.InvoiceSteps.pay(invoice_id_fish)
        steps.InvoiceSteps.set_turn_on_dt(invoice_id_fish, DT)
        steps.CampaignsSteps.do_campaigns(service_id, service_order_id_fish,
                                          {'Bucks': (qty + fish_overshipmen) / 30}, 0, DT)

    with reporter.step(u'Переходим на мультивалютность миграцией'):
        steps.ClientSteps.migrate_to_currency(client_id, currency_convert_type='MODIFY',
                                              dt=LAST_DAY_OF_PREVIOUS_MONTH - relativedelta(days=2))

    with reporter.step(u'Подключаем овердрафт'):
        steps.OverdraftSteps.set_force_overdraft(client_id, service_id, limit, Firms.YANDEX_1.id, currency='RUB')

    with reporter.step(u'Подключаем автоовердрафт'):
        steps.OverdraftSteps.set_overdraft_params(person_id=person_id, client_limit=limit)

    with reporter.step(u'Создаем и перекручиваем валютный заказ'):
        service_order_id_money = steps.OrderSteps.next_id(service_id=service_id)
        order_id_money = steps.OrderSteps.create(client_id, service_order_id_money, service_id=service_id,
                                                 product_id=product_id, params={'AgencyID': None})
        orders_list = [{'ServiceID': service_id, 'ServiceOrderID': service_order_id_money, 'Qty': qty}]
        request_id = steps.RequestSteps.create(client_id, orders_list, additional_params=dict(InvoiceDesireDT=DT))
        invoice_id_money, _, _ = steps.InvoiceSteps.create(request_id, person_id, paysys_id, credit=0,
                                                           contract_id=None, overdraft=0, endbuyer_id=None)
        steps.InvoiceSteps.pay(invoice_id_money)
        steps.InvoiceSteps.set_turn_on_dt(invoice_id_money, DT)
        steps.CampaignsSteps.do_campaigns(service_id, service_order_id_money,
                                          {'Money': qty + currency_overshipment}, 0, DT)

    get_act_and_autooverdraft_invoice(client_id, DT)

    query = "SELECT TOTAL_SUM, TOTAL_ACT_SUM " \
            "FROM T_INVOICE " \
            "WHERE REQUEST_ID IN (SELECT ID from T_REQUEST WHERE CLIENT_ID = :client_id)" \
            "AND ID NOT IN (:invoice_id_1, :invoice_id_2)"
    overdraft_invoice_sum = db.balance().execute(query, {'client_id': client_id,
                                                         'invoice_id_1': invoice_id_fish,
                                                         'invoice_id_2': invoice_id_money})

    utils.check_that(len(overdraft_invoice_sum), hamcrest.equal_to(1),
                     step=u'Проверяем, что создан только один овердрафтный счет')

    utils.check_that(overdraft_invoice_sum[0]['total_sum'], hamcrest.equal_to(limit),
                     step=u'Проверяем, что овердрафтный счет выставлен на сумму лимита')

    utils.check_that(overdraft_invoice_sum[0]['total_act_sum'],
                     hamcrest.equal_to(overdraft_invoice_sum[0]['total_sum']),
                     step=u'Проверяем, что сумма акта и сумма овердрафтного счета совпадают')

    query = "SELECT CONSUME_SUM from T_ORDER WHERE ID IN (:order_1, :order_2)"
    consume_sums = db.balance().execute(query, {'order_1': order_id_fish,
                                                'order_2': order_id_money})

    utils.check_that(consume_sums[0]['consume_sum'],
                     hamcrest.equal_to(str(qty + (limit * fish_overshipmen / total_overshipment))),
                     step=u'Проверяем, что перекрутки сверх лимита распределены пропорционально '
                          u'открученному по каждому заказу клиента')

    utils.check_that(consume_sums[1]['consume_sum'],
                     hamcrest.equal_to(str(qty + (limit * currency_overshipment / total_overshipment))),
                     step=u'Проверяем, что перекрутки сверх лимита распределены пропорционально '
                          u'открученному по каждому заказу клиента')


@pytest.mark.parametrize('context', [
    DIRECT_RUB,
    # DIRECT_KZ,
    # DIRECT_KZ_QUASI,
    DIRECT_BEL
])
def test_current_month_shipment_not_in_overdraft_invoice(context):
    limit = 90
    qty_limit = get_qty_limit(context, limit)
    qty = 600
    previous_month_completion_qty = 630
    this_month_completion_qty = 660

    client_id = steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, context.person_type.code)

    get_autooverdraft(context, client_id, person_id, overdraft_limit=limit, autooverdraft_limit=qty_limit)

    with reporter.step(u'Создаем заказы с единым счетом, перекручиваем сверх лимита'):
        service_order_id = steps.OrderSteps.next_id(service_id=context.service.id)
        service_order_id_2 = steps.OrderSteps.next_id(service_id=context.service.id)
        main_order_id = steps.OrderSteps.create(client_id, service_order_id, service_id=context.service.id,
                                                product_id=context.product.id, params={'AgencyID': None})
        child_order_id = steps.OrderSteps.create(client_id, service_order_id_2, service_id=context.service.id,
                                                 product_id=context.product.id,
                                                 params={'AgencyID': None, 'GroupServiceOrderID': service_order_id})

        orders_list = [{'ServiceID': context.service.id, 'ServiceOrderID': service_order_id_2, 'Qty': qty}]
        request_id = steps.RequestSteps.create(client_id, orders_list, additional_params=dict(InvoiceDesireDT=DT))
        invoice_id, _, _ = steps.InvoiceSteps.create(request_id, person_id, context.paysys.id, credit=0,
                                                     contract_id=None, overdraft=0, endbuyer_id=None)

        steps.InvoiceSteps.pay(invoice_id)
        steps.InvoiceSteps.set_turn_on_dt(invoice_id, DT)
        steps.CampaignsSteps.do_campaigns(context.service.id, service_order_id_2,
                                          {'Money': previous_month_completion_qty}, 0, DT)

        steps.CommonSteps.export('UA_TRANSFER', 'Client', client_id, input_={'for_dt': datetime.datetime.now()})

    with reporter.step(u'Крутим заказ в текущем месяце'):
        now = datetime.datetime.now()
        steps.CampaignsSteps.do_campaigns(context.service.id, service_order_id_2,
                                          {'Money': this_month_completion_qty}, 0, now)

    get_act_and_autooverdraft_invoice(client_id, DT)

    query = "SELECT TOTAL_SUM, TOTAL_ACT_SUM " \
            "FROM T_INVOICE " \
            "WHERE REQUEST_ID IN (SELECT ID from T_REQUEST WHERE CLIENT_ID = :client_id)" \
            "AND ID != :invoice_id"
    overdraft_invoice_sum = db.balance().execute(query, {'client_id': client_id,
                                                         'invoice_id': invoice_id})

    utils.check_that(len(overdraft_invoice_sum), hamcrest.equal_to(1),
                     step=u'Проверяем, что создан только один овердрафтный счет')

    utils.check_that(D(overdraft_invoice_sum[0]['total_sum']),
                     hamcrest.equal_to(D(previous_month_completion_qty - qty)
                                       if not context.is_quasi
                                       else (previous_month_completion_qty - qty) * (1 + D(context.nds) / 100)),
                     step=u'Проверяем, что в овердрафтный счет не попадают открутки из текущего месяца')

    utils.check_that(overdraft_invoice_sum[0]['total_act_sum'],
                     hamcrest.equal_to(overdraft_invoice_sum[0]['total_sum']),
                     step=u'Проверяем, что сумма акта и сумма овердрафтного счета совпадают')


@pytest.mark.parametrize('context', [
    DIRECT_RUB,
    # DIRECT_KZ,
    # DIRECT_KZ_QUASI,
    DIRECT_BEL
])
def test_current_month_payment(context):
    limit = 90
    qty_limit = get_qty_limit(context, limit)
    qty = 600
    previous_month_completion_qty = 630
    this_month_completion_qty = 660

    client_id = steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, context.person_type.code)

    get_autooverdraft(context, client_id, person_id, overdraft_limit=limit, autooverdraft_limit=qty_limit)

    with reporter.step(u'Создаем заказы с единым счетом и перекручиваем сверх лимита'):
        service_order_id = steps.OrderSteps.next_id(service_id=context.service.id)
        service_order_id_2 = steps.OrderSteps.next_id(service_id=context.service.id)
        main_order_id = steps.OrderSteps.create(client_id, service_order_id, service_id=context.service.id,
                                                product_id=context.product.id, params={'AgencyID': None})
        child_order_id = steps.OrderSteps.create(client_id, service_order_id_2, service_id=context.service.id,
                                                 product_id=context.product.id,
                                                 params={'AgencyID': None, 'GroupServiceOrderID': service_order_id})

        orders_list = [{'ServiceID': context.service.id, 'ServiceOrderID': service_order_id_2, 'Qty': qty}]
        request_id = steps.RequestSteps.create(client_id, orders_list, additional_params=dict(InvoiceDesireDT=DT))
        invoice_id_previous_month, _, _ = steps.InvoiceSteps.create(request_id, person_id, context.paysys.id, credit=0,
                                                                    contract_id=None, overdraft=0, endbuyer_id=None)

        steps.InvoiceSteps.pay(invoice_id_previous_month)
        steps.InvoiceSteps.set_turn_on_dt(invoice_id_previous_month, DT)
        steps.CampaignsSteps.do_campaigns(context.service.id, service_order_id_2,
                                          {'Money': previous_month_completion_qty}, 0, DT)

    with reporter.step(u'Пополняем заказ в текущем месяце, оплачиваем счет и крутим его'):
        now = datetime.datetime.now()
        orders_list = [{'ServiceID': context.service.id, 'ServiceOrderID': service_order_id_2, 'Qty': qty}]
        request_id = steps.RequestSteps.create(client_id, orders_list, additional_params=dict(InvoiceDesireDT=now))
        invoice_id_current_month, _, _ = steps.InvoiceSteps.create(request_id, person_id, context.paysys.id, credit=0,
                                                                   contract_id=None, overdraft=0, endbuyer_id=None)

        steps.InvoiceSteps.pay(invoice_id_current_month)
        steps.CampaignsSteps.do_campaigns(context.service.id, service_order_id_2,
                                          {'Money': this_month_completion_qty}, 0, now)

    with reporter.step(u'Разбираем общий счет, выставляем автоовердрафтный счет и актимся'):
        steps.CommonSteps.export('UA_TRANSFER', 'Client', client_id, input_={'for_dt': datetime.datetime.now()})

    get_act_and_autooverdraft_invoice(client_id, DT)

    query = "SELECT TOTAL_SUM, TOTAL_ACT_SUM " \
            "FROM T_INVOICE " \
            "WHERE REQUEST_ID IN (SELECT ID from T_REQUEST WHERE CLIENT_ID = :client_id)" \
            "AND ID NOT IN (:invoice_1, :invoice_2)"
    overdraft_invoice_sum = db.balance().execute(query, {'client_id': client_id,
                                                         'invoice_1': invoice_id_previous_month,
                                                         'invoice_2': invoice_id_current_month})

    utils.check_that(len(overdraft_invoice_sum), hamcrest.equal_to(1),
                     step=u'Проверяем, что создан только один овердрафтный счет')

    utils.check_that(D(overdraft_invoice_sum[0]['total_sum']),
                     hamcrest.equal_to(D(previous_month_completion_qty - qty)
                                       if not context.is_quasi
                                       else (previous_month_completion_qty - qty) * (1 + D(context.nds) / 100)),
                     step=u'Проверяем, что овердрафтный счет выставлен на сумму перекрута')

    utils.check_that(overdraft_invoice_sum[0]['total_act_sum'],
                     hamcrest.equal_to(overdraft_invoice_sum[0]['total_sum']),
                     step=u'Проверяем, что сумма акта и сумма овердрафтного счета совпадают')


@pytest.mark.parametrize('context', [
    DIRECT_RUB,
    # DIRECT_KZ,
    # DIRECT_KZ_QUASI,
    DIRECT_BEL
])
def test_current_month_payment_acted(context):
    limit = 90
    qty_limit = get_qty_limit(context, limit)
    qty = 600

    client_id = steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, context.person_type.code)

    get_autooverdraft(context, client_id, person_id, overdraft_limit=limit, autooverdraft_limit=qty_limit)

    with reporter.step(u'Создаем заказы с единым счетом, перекручиваем сверх лимита'):
        service_order_id = steps.OrderSteps.next_id(service_id=context.service.id)
        service_order_id_2 = steps.OrderSteps.next_id(service_id=context.service.id)
        main_order_id = steps.OrderSteps.create(client_id, service_order_id, service_id=context.service.id,
                                                product_id=context.product.id, params={'AgencyID': None})
        child_order_id = steps.OrderSteps.create(client_id, service_order_id_2, service_id=context.service.id,
                                                 product_id=context.product.id,
                                                 params={'AgencyID': None, 'GroupServiceOrderID': service_order_id})

    with reporter.step(u'Пополняем заказ в текущем месяце и куртим его'):
        now = datetime.datetime.now()
        orders_list = [{'ServiceID': context.service.id, 'ServiceOrderID': service_order_id_2, 'Qty': qty}]
        request_id = steps.RequestSteps.create(client_id, orders_list, additional_params=dict(InvoiceDesireDT=now))
        invoice_id_current_month, _, _ = steps.InvoiceSteps.create(request_id, person_id, context.paysys.id, credit=0,
                                                                   contract_id=None, overdraft=0, endbuyer_id=None)

        steps.InvoiceSteps.pay(invoice_id_current_month)
        steps.CampaignsSteps.do_campaigns(context.service.id, service_order_id_2,
                                          {'Money': qty}, 0, now)

    with reporter.step(u'Разбираем общий счет, актимся и пытаемся выставить автоовердрафтный счет'):
        steps.CommonSteps.export('UA_TRANSFER', 'Client', client_id, input_={'for_dt': datetime.datetime.now()})

        act_id = steps.ActsSteps.generate(client_id, force=1, date=now)[0]

        autooverdraft_id = db.balance().execute("SELECT ID FROM bo.t_overdraft_params WHERE client_id =:item",
                                                {'item': client_id})[0]['id']

        with pytest.raises(Exception):
            steps.CommonSteps.export('AUTO_OVERDRAFT', 'OverdraftParams', autooverdraft_id, with_enqueue=True)

        error_message = db.balance().execute("select error from t_export where type = 'AUTO_OVERDRAFT' "
                                             "and classname = 'OverdraftParams' and object_id =:item",
                                             {'item': autooverdraft_id})[0]['error']
        expected_err_msg = 'Invalid parameter for function: ' \
                           'Invoice {} that is to be withdrawn has acts'.format(invoice_id_current_month)
        utils.check_that(error_message, hamcrest.equal_to(expected_err_msg))


@pytest.mark.parametrize('context', [
    DIRECT_RUB,
    # DIRECT_KZ,
    # DIRECT_KZ_QUASI,
    DIRECT_BEL
])
def test_unused_qty(context):
    limit = 90
    qty_limit = get_qty_limit(context, limit)
    qty = 600
    order_1_completion_qty = 400
    order_2_completion_qty = 660

    client_id = steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, context.person_type.code)

    get_autooverdraft(context, client_id, person_id, overdraft_limit=limit, autooverdraft_limit=qty_limit)

    with reporter.step(u'Создаем и недокручиваем заказ'):
        service_order_id_not_full_shipment = steps.OrderSteps.next_id(service_id=context.service.id)
        order_id = steps.OrderSteps.create(client_id, service_order_id_not_full_shipment, service_id=context.service.id,
                                           product_id=context.product.id, params={'AgencyID': None})
        orders_list = [
            {'ServiceID': context.service.id, 'ServiceOrderID': service_order_id_not_full_shipment, 'Qty': qty}]
        request_id = steps.RequestSteps.create(client_id, orders_list, additional_params=dict(InvoiceDesireDT=DT))
        invoice_id_not_full_shipment, _, _ = steps.InvoiceSteps.create(request_id, person_id, context.paysys.id,
                                                                       credit=0, contract_id=None, overdraft=0,
                                                                       endbuyer_id=None)
        steps.InvoiceSteps.pay(invoice_id_not_full_shipment)
        steps.InvoiceSteps.set_turn_on_dt(invoice_id_not_full_shipment, DT)
        steps.CampaignsSteps.do_campaigns(context.service.id, service_order_id_not_full_shipment,
                                          {'Money': order_1_completion_qty}, 0, DT)

    with reporter.step(u'Создаем заказы с единым счетом и перекручиваем сверх лимита'):
        invoice_id_overshipment, main_order_id, child_order_id = \
            get_orders_and_invoice(context, client_id, person_id, DT, qty, order_2_completion_qty)

    get_act_and_autooverdraft_invoice(client_id, DT)

    query = "SELECT TOTAL_SUM, TOTAL_ACT_SUM " \
            "FROM T_INVOICE " \
            "WHERE REQUEST_ID IN (SELECT ID from T_REQUEST WHERE CLIENT_ID = :client_id)" \
            "AND ID NOT IN (:invoice_id_1, :invoice_id_2)"
    overdraft_invoice_sum = db.balance().execute(query, {'client_id': client_id,
                                                         'invoice_id_1': invoice_id_not_full_shipment,
                                                         'invoice_id_2': invoice_id_overshipment})

    utils.check_that(len(overdraft_invoice_sum), hamcrest.equal_to(1),
                     step=u'Проверяем, что создан только один овердрафтный счет')

    utils.check_that(D(overdraft_invoice_sum[0]['total_sum']),
                     hamcrest.equal_to(D(order_2_completion_qty - qty)
                                       if not context.is_quasi
                                       else (order_2_completion_qty - qty) * (1 + D(context.nds) / 100)),
                     step=u'Проверяем, что перекрут по заказу не покрыт свободными средствами с другого заказа')

    utils.check_that(overdraft_invoice_sum[0]['total_act_sum'],
                     hamcrest.equal_to(overdraft_invoice_sum[0]['total_sum']),
                     step=u'Проверяем, что сумма акта и сумма овердрафтного счета совпадают')


@pytest.mark.parametrize('context', [
    DIRECT_RUB,
    # DIRECT_KZ,
    # DIRECT_KZ_QUASI,
    DIRECT_BEL
])
def test_autoverdraft_with_overdraft_ban(context):
    limit = 60
    qty_limit = get_qty_limit(context, limit)
    qty = 600
    completion_qty = 650

    client_id = steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, context.person_type.code)

    get_autooverdraft(context, client_id, person_id, overdraft_limit=limit, autooverdraft_limit=qty_limit)

    with reporter.step(u'Создаем заказы с единым счетом и перекручиваем сверх лимита'):
        invoice_id, main_order_id, child_order_id = \
            get_orders_and_invoice(context, client_id, person_id, DT, qty, completion_qty)

    with reporter.step(u'Баним клиента'):
        db.balance().execute("update t_client set overdraft_ban = 1 where id = :client_id", {'client_id': client_id})

    get_act_and_autooverdraft_invoice(client_id, DT)

    get_and_check_autooverdraft_invoice_sum(context, client_id, limit,
                                            overshipment_qty=completion_qty - qty,
                                            prepay_invoice_id=invoice_id)


@pytest.mark.parametrize('context', [
    DIRECT_RUB,
    # DIRECT_KZ,
    # DIRECT_KZ_QUASI,
    DIRECT_BEL
])
@pytest.mark.parametrize('completion_qty, payment_method', [
    (660, PaymentMethods.CARD.cc),
    (660, PaymentMethods.YAMONEY_WALLET.cc),
    (2000000, PaymentMethods.CARD.cc)
])
def test_pay_by_bank_when_your_payment_method_unavailable(completion_qty, payment_method, context):
    limit = 3000000
    qty_limit = get_qty_limit(context, limit)
    qty = 600

    client_id = steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, context.person_type.code)

    get_autooverdraft(context, client_id, person_id, overdraft_limit=limit, autooverdraft_limit=qty_limit,
                      payment_method=payment_method)

    with reporter.step(u'Создаем заказы с единым счетом и перекручиваем сверх лимита'):
        invoice_id, main_order_id, child_order_id = \
            get_orders_and_invoice(context, client_id, person_id, DT, qty, completion_qty)

    get_act_and_autooverdraft_invoice(client_id, DT)

    params = db.balance().execute("SELECT * FROM bo.t_overdraft_params WHERE client_id =:item",
                                  {'item': client_id})[0]['payment_method_cc']
    utils.check_that(params, hamcrest.equal_to(payment_method),
                     step=u'Проверяем, что метод оплаты в таблице bo.t_overdraft_params не изменился')

    with reporter.step(u'Смотрим paysys_id автоовердрафтного счета'):
        query = "SELECT PAYSYS_ID " \
                "FROM T_INVOICE " \
                "WHERE REQUEST_ID IN (SELECT ID from T_REQUEST WHERE CLIENT_ID = :client_id)" \
                "AND ID NOT IN (:invoice_id)"
        overdraft_invoice_payment_method = db.balance().execute(query, {'client_id': client_id,
                                                                        'invoice_id': invoice_id})[0]['paysys_id']

    if payment_method == PaymentMethods.CARD.cc and (not context.card_limit or completion_qty <= context.card_limit):
        expected_paysys_id = context.card_paysys.id
    else:
        expected_paysys_id = context.paysys.id

    utils.check_that(overdraft_invoice_payment_method, hamcrest.equal_to(expected_paysys_id),
                     step=u'Проверяем способ оплаты автоовердрафтного счета')


@pytest.mark.parametrize('context', [
    DIRECT_RUB,
    # DIRECT_KZ,
    # DIRECT_KZ_QUASI,
    # DIRECT_BEL
])
def test_unable_autooverdraft_to_brand_client(context):
    dt = datetime.datetime.now() - relativedelta(days=1)
    contract_dt = dt - relativedelta(days=1)
    # contract_dt = dt + relativedelta(days=2)  # если дата начала договора в будущем все равно работает
    limit = 10000
    qty_limit = get_qty_limit(context, limit)

    client_id_1 = steps.ClientSteps.create()
    person_id_1 = steps.PersonSteps.create(client_id_1, context.person_type.code)
    overdraft_object_id_client_1 = \
        str((context.firm.id + context.service.id * 10000 + client_id_1 * 100000000) * 10)

    is_brand_client = steps.CommonSteps.build_notification(CLIENT_OPCODE, object_id=client_id_1
                                                           )['args'][0].get('HasEquivalentOrBrandClients')
    utils.check_that(is_brand_client, hamcrest.equal_to('0'),
                     step=u"Проверяем значение HasEquivalentOrBrandClients в нотификации по клиенту "
                          u"до заключения тех связки")

    client_id_2 = steps.ClientSteps.create()
    person_id_2 = steps.PersonSteps.create(client_id_2, context.person_type.code)
    overdraft_object_id_client_2 = \
        str((context.firm.id + context.service.id * 10000 + client_id_2 * 100000000) * 10)

    is_brand_client = steps.CommonSteps.build_notification(CLIENT_OPCODE, object_id=client_id_2
                                                           )['args'][0].get('HasEquivalentOrBrandClients')
    utils.check_that(is_brand_client, hamcrest.equal_to('0'),
                     step=u"Проверяем значение HasEquivalentOrBrandClients в нотификации ро клиентну"
                          u"до подключения овердрафта")

    with reporter.step(u'Переходим на мультивалютность копированием'):
        steps.ClientSteps.migrate_to_currency(client_id_1, currency_convert_type='COPY',
                                              dt=LAST_DAY_OF_PREVIOUS_MONTH - relativedelta(days=5),
                                              currency=context.currency.iso_code, region_id=context.region.id)
        steps.ClientSteps.migrate_to_currency(client_id_2, currency_convert_type='COPY',
                                              dt=LAST_DAY_OF_PREVIOUS_MONTH - relativedelta(days=5),
                                              currency=context.currency.iso_code, region_id=context.region.id)

    with reporter.step(u'Подключаем овердрафт'):
        steps.OverdraftSteps.set_force_overdraft(client_id_1, context.service.id, limit, context.firm.id,
                                                 currency=context.currency.iso_code,
                                                 limit_wo_tax=int(D(limit) / (1 + D(context.nds) / 100)))
        steps.OverdraftSteps.set_force_overdraft(client_id_2, context.service.id, limit, context.firm.id,
                                                 currency=context.currency.iso_code,
                                                 limit_wo_tax=int(D(limit) / (1 + D(context.nds) / 100)))

    is_brand_client = steps.CommonSteps.build_notification(CLIENT_OPCODE, object_id=client_id_1
                                                           )['args'][0].get('HasEquivalentOrBrandClients')
    utils.check_that(is_brand_client, hamcrest.equal_to('0'),
                     step=u"Проверяем значение HasEquivalentOrBrandClients в нотификации по клиенту"
                          u" после подключения овердрафта")

    is_brand_client = steps.CommonSteps.build_notification(CLIENT_OPCODE, object_id=client_id_2
                                                           )['args'][0].get('HasEquivalentOrBrandClients')
    utils.check_that(is_brand_client, hamcrest.equal_to('0'),
                     step=u"Проверяем значение HasEquivalentOrBrandClients в нотификации по клиенту"
                          u" после подключения овердрафта")

    is_brand_client = steps.CommonSteps.build_notification(OVERDRAFT_OPCODE, object_id=overdraft_object_id_client_1
                                                           )['args'][0].get('HasEquivalentOrBrandClients')
    utils.check_that(is_brand_client, hamcrest.equal_to('0'),
                     step=u"Проверяем значение HasEquivalentOrBrandClients в нотификации по овердрафту"
                          u" после подключения овердрафта")

    is_brand_client = steps.CommonSteps.build_notification(OVERDRAFT_OPCODE, object_id=overdraft_object_id_client_2
                                                           )['args'][0].get('HasEquivalentOrBrandClients')
    utils.check_that(is_brand_client, hamcrest.equal_to('0'),
                     step=u"Проверяем значение HasEquivalentOrBrandClients в нотификации по овердрафту"
                          u" после подключения овердрафта")

    with reporter.step(u'Заключаем договор о техсвязке'):
        _, _ = steps.ContractSteps.create_brand_contract(
            client_id_1,
            client_id_2,
            dt=contract_dt
        )

    is_brand_client = steps.CommonSteps.build_notification(CLIENT_OPCODE, object_id=client_id_1
                                                           )['args'][0].get('HasEquivalentOrBrandClients')
    utils.check_that(is_brand_client, hamcrest.equal_to('1'),
                     step=u"Проверяем значение HasEquivalentOrBrandClients в нотификации по клиенту"
                          u" после заключения тех связки")

    is_brand_client = steps.CommonSteps.build_notification(CLIENT_OPCODE, object_id=client_id_2
                                                           )['args'][0].get('HasEquivalentOrBrandClients')
    utils.check_that(is_brand_client, hamcrest.equal_to('1'),
                     step=u"Проверяем значение HasEquivalentOrBrandClients в нотификации по клиенту"
                          u" после заключения тех связки")

    is_brand_client = steps.CommonSteps.build_notification(OVERDRAFT_OPCODE, object_id=overdraft_object_id_client_1
                                                           )['args'][0].get('HasEquivalentOrBrandClients')
    utils.check_that(is_brand_client, hamcrest.equal_to('1'),
                     step=u"Проверяем значение HasEquivalentOrBrandClients в нотификации по овердрафту"
                          u" после заключения тех связки")

    is_brand_client = steps.CommonSteps.build_notification(OVERDRAFT_OPCODE, object_id=overdraft_object_id_client_2
                                                           )['args'][0].get('HasEquivalentOrBrandClients')
    utils.check_that(is_brand_client, hamcrest.equal_to('1'),
                     step=u"Проверяем значение HasEquivalentOrBrandClients в нотификации по овердрафту"
                          u" после заключения тех связки")

    try:
        with reporter.step(u'Подключаем автоовердрафт'):
            steps.OverdraftSteps.set_overdraft_params(person_id=person_id_1, client_limit=qty_limit,
                                                      iso_currency=context.currency.iso_code)
    except Exception, exc:
        utils.check_that(steps.CommonSteps.get_exception_code(exc), hamcrest.equal_to('INVALID_PARAM'))
        utils.check_that(steps.CommonSteps.get_exception_code(exc, tag_name='msg'),
                         hamcrest.equal_to('Invalid parameter for function: Client has equivalent clients or brand'))
    else:
        raise utils.TestsError(u"Клиенту доступен автоовердрафт при наличии техсвязки")

    is_brand_client = steps.CommonSteps.build_notification(CLIENT_OPCODE, object_id=client_id_1
                                                           )['args'][0].get('HasEquivalentOrBrandClients')
    utils.check_that(is_brand_client, hamcrest.equal_to('1'),
                     step=u"Проверяем значение HasEquivalentOrBrandClients в нотификации по клиенту"
                          u" после попытки подключения автоовердрафта")

    is_brand_client = steps.CommonSteps.build_notification(CLIENT_OPCODE, object_id=client_id_2
                                                           )['args'][0].get('HasEquivalentOrBrandClients')
    utils.check_that(is_brand_client, hamcrest.equal_to('1'),
                     step=u"Проверяем значение HasEquivalentOrBrandClients в нотификации по клиенту"
                          u" после попытки подключения автоовердрафта")

    is_brand_client = steps.CommonSteps.build_notification(OVERDRAFT_OPCODE, object_id=overdraft_object_id_client_1
                                                           )['args'][0].get('HasEquivalentOrBrandClients')
    utils.check_that(is_brand_client, hamcrest.equal_to('1'),
                     step=u"Проверяем значение HasEquivalentOrBrandClients в нотификации по овердрафту"
                          u" после попытки подключения автоовердрафта")

    is_brand_client = steps.CommonSteps.build_notification(OVERDRAFT_OPCODE, object_id=overdraft_object_id_client_2
                                                           )['args'][0].get('HasEquivalentOrBrandClients')
    utils.check_that(is_brand_client, hamcrest.equal_to('1'),
                     step=u"Проверяем значение HasEquivalentOrBrandClients в нотификации по овердрафту"
                          u" после попытки подключения автоовердрафта")

    try:
        with reporter.step(u'Подключаем автоовердрафт'):
            steps.OverdraftSteps.set_overdraft_params(person_id=person_id_2, client_limit=qty_limit,
                                                      iso_currency=context.currency.iso_code)
    except Exception, exc:
        utils.check_that(steps.CommonSteps.get_exception_code(exc), hamcrest.equal_to('INVALID_PARAM'))
        utils.check_that(steps.CommonSteps.get_exception_code(exc, tag_name='msg'),
                         hamcrest.equal_to('Invalid parameter for function: Client has equivalent clients or brand'))
    else:
        raise utils.TestsError(u"Клиенту доступен автоовердрафт при наличии техсвязки")

    is_brand_client = steps.CommonSteps.build_notification(CLIENT_OPCODE, object_id=client_id_1
                                                           )['args'][0].get('HasEquivalentOrBrandClients')
    utils.check_that(is_brand_client, hamcrest.equal_to('1'),
                     step=u"Проверяем значение HasEquivalentOrBrandClients в нотификации по клиенту"
                          u" после попытки подключения автоовердрафта")

    is_brand_client = steps.CommonSteps.build_notification(CLIENT_OPCODE, object_id=client_id_2
                                                           )['args'][0].get('HasEquivalentOrBrandClients')
    utils.check_that(is_brand_client, hamcrest.equal_to('1'),
                     step=u"Проверяем значение HasEquivalentOrBrandClients в нотификации по клиенту"
                          u" после попытки подключения автоовердрафта")

    is_brand_client = steps.CommonSteps.build_notification(OVERDRAFT_OPCODE, object_id=overdraft_object_id_client_1
                                                           )['args'][0].get('HasEquivalentOrBrandClients')
    utils.check_that(is_brand_client, hamcrest.equal_to('1'),
                     step=u"Проверяем значение HasEquivalentOrBrandClients в нотификации по овердрафту"
                          u" после попытки подключения автоовердрафта")

    is_brand_client = steps.CommonSteps.build_notification(OVERDRAFT_OPCODE, object_id=overdraft_object_id_client_2
                                                           )['args'][0].get('HasEquivalentOrBrandClients')
    utils.check_that(is_brand_client, hamcrest.equal_to('1'),
                     step=u"Проверяем значение HasEquivalentOrBrandClients в нотификации по овердрафту"
                          u" после попытки подключения автоовердрафта")


@pytest.mark.parametrize('context', [
    DIRECT_RUB,
    # DIRECT_KZ,
    # DIRECT_KZ_QUASI,
    # DIRECT_BEL
])
def test_enable_autooverdraft_to_client_after_brand_finish(context):
    limit = 10000
    qty_limit = get_qty_limit(context, limit)

    client_id_1 = steps.ClientSteps.create()
    person_id_1 = steps.PersonSteps.create(client_id_1, context.person_type.code)
    overdraft_object_id_client_1 = \
        str((context.firm.id + context.service.id * 10000 + client_id_1 * 100000000) * 10)

    client_id_2 = steps.ClientSteps.create()
    person_id_2 = steps.PersonSteps.create(client_id_2, context.person_type.code)
    overdraft_object_id_client_2 = \
        str((context.firm.id + context.service.id * 10000 + client_id_2 * 100000000) * 10)

    with reporter.step(u'Переходим на мультивалютность копированием'):
        steps.ClientSteps.migrate_to_currency(client_id_1, currency_convert_type='COPY',
                                              dt=LAST_DAY_OF_PREVIOUS_MONTH - relativedelta(days=5),
                                              currency=context.currency.iso_code, region_id=context.region.id)
        steps.ClientSteps.migrate_to_currency(client_id_2, currency_convert_type='COPY',
                                              dt=LAST_DAY_OF_PREVIOUS_MONTH - relativedelta(days=5),
                                              currency=context.currency.iso_code, region_id=context.region.id)

    with reporter.step(u'Подключаем овердрафт'):
        steps.OverdraftSteps.set_force_overdraft(client_id_1, context.service.id, limit, context.firm.id,
                                                 currency=context.currency.iso_code,
                                                 limit_wo_tax=int(D(limit) / (1 + D(context.nds) / 100)))
        steps.OverdraftSteps.set_force_overdraft(client_id_2, context.service.id, limit, context.firm.id,
                                                 currency=context.currency.iso_code,
                                                 limit_wo_tax=int(D(limit) / (1 + D(context.nds) / 100)))

    with reporter.step(u'Заключаем договор о техсвязке'):
        _, _ = steps.ContractSteps.create_brand_contract(
            client_id_1,
            client_id_2,
            dt=datetime.datetime.now() - datetime.timedelta(5),
            finish_dt=datetime.datetime.now() - datetime.timedelta(1),
        )

    is_brand_client = steps.CommonSteps.build_notification(CLIENT_OPCODE, object_id=client_id_1
                                                           )['args'][0].get('HasEquivalentOrBrandClients')
    utils.check_that(is_brand_client, hamcrest.equal_to('0'),
                     step=u"Проверяем значение HasEquivalentOrBrandClients в нотификации по клиенту"
                          u" после разрыва тех связки")

    is_brand_client = steps.CommonSteps.build_notification(CLIENT_OPCODE, object_id=client_id_2
                                                           )['args'][0].get('HasEquivalentOrBrandClients')
    utils.check_that(is_brand_client, hamcrest.equal_to('0'),
                     step=u"Проверяем значение HasEquivalentOrBrandClients в нотификации по клиенту"
                          u" после разрыва тех связки")

    is_brand_client = steps.CommonSteps.build_notification(OVERDRAFT_OPCODE, object_id=overdraft_object_id_client_1
                                                           )['args'][0].get('HasEquivalentOrBrandClients')
    utils.check_that(is_brand_client, hamcrest.equal_to('0'),
                     step=u"Проверяем значение HasEquivalentOrBrandClients в нотификации по овердрафту"
                          u" после разрыва тех связки")

    is_brand_client = steps.CommonSteps.build_notification(OVERDRAFT_OPCODE, object_id=overdraft_object_id_client_2
                                                           )['args'][0].get('HasEquivalentOrBrandClients')
    utils.check_that(is_brand_client, hamcrest.equal_to('0'),
                     step=u"Проверяем значение HasEquivalentOrBrandClients в нотификации по овердрафту"
                          u" после разрыва тех связки")

    with reporter.step(u'Подключаем автоовердрафт обоим клиентам'):
        steps.OverdraftSteps.set_overdraft_params(person_id=person_id_1, client_limit=limit,
                                                  iso_currency=context.currency.iso_code)
        steps.OverdraftSteps.set_overdraft_params(person_id=person_id_2, client_limit=limit,
                                                  iso_currency=context.currency.iso_code)


@pytest.mark.parametrize('context', [
    DIRECT_RUB,
    # DIRECT_KZ,
    # DIRECT_KZ_QUASI,
    # DIRECT_BEL
])
def test_brand_contract_unable_with_autooverdraft(context):
    limit = 10000
    qty_limit = get_qty_limit(context, limit)
    dt = datetime.datetime.now()

    client_id_1 = steps.ClientSteps.create()
    person_id_1 = steps.PersonSteps.create(client_id_1, context.person_type.code)

    client_id_2 = steps.ClientSteps.create()
    person_id_2 = steps.PersonSteps.create(client_id_2, context.person_type.code)

    with reporter.step(u'Переходим на мультивалютность копированием'):
        steps.ClientSteps.migrate_to_currency(client_id_1, currency_convert_type='COPY',
                                              dt=LAST_DAY_OF_PREVIOUS_MONTH - relativedelta(days=5),
                                              currency=context.currency.iso_code, region_id=context.region.id)

    with reporter.step(u'Переходим на мультивалютность копированием'):
        steps.ClientSteps.migrate_to_currency(client_id_2, currency_convert_type='COPY',
                                              dt=LAST_DAY_OF_PREVIOUS_MONTH - relativedelta(days=5),
                                              currency=context.currency.iso_code, region_id=context.region.id)

    with reporter.step(u'Подключаем овердрафт'):
        steps.OverdraftSteps.set_force_overdraft(client_id_2, context.service.id, limit, context.firm.id,
                                                 currency=context.currency.iso_code,
                                                 limit_wo_tax=int(D(limit) / (1 + D(context.nds) / 100)))

    with reporter.step(u'Подключаем автоовердрафт'):
        steps.OverdraftSteps.set_overdraft_params(person_id=person_id_2, client_limit=limit,
                                                  iso_currency=context.currency.iso_code)

    with reporter.step(u'Проверяем невозможность заключение договора о тех связке с клиентом, '
                       u'которому подключен автоовердрафт'):
        try:
            _, _ = steps.ContractSteps.create_brand_contract(client_id_1, client_id_2, dt=dt)
        except Exception, exc:
            utils.check_that(steps.CommonSteps.get_exception_code(exc), hamcrest.equal_to('CONTRACT_RULE_VIOLATION'))
            utils.check_that(steps.CommonSteps.get_exception_code(exc, tag_name='msg'),
                             hamcrest.equal_to(u"Rule violation: 'Клиент(ы) " + str(client_id_2) +
                                               u" имеют автоовердрафт. Включение их в бренд запрещено.'"))
        else:
            raise utils.TestsError(u"Клиенту доступна техсвязка при подключенном автоовердрафте")

    with reporter.step(u'Проверяем невозможность заключение договора о тех связке для клиента, '
                       u'которому подключен автоовердрафт'):
        try:
            _, _ = steps.ContractSteps.create_brand_contract(client_id_2, client_id_1, dt=dt)
        except Exception, exc:
            utils.check_that(steps.CommonSteps.get_exception_code(exc), hamcrest.equal_to('CONTRACT_RULE_VIOLATION'))
            utils.check_that(steps.CommonSteps.get_exception_code(exc, tag_name='msg'),
                             hamcrest.equal_to(u"Rule violation: 'Клиент(ы) " + str(client_id_2) +
                                               u" имеют автоовердрафт. Включение их в бренд запрещено.'"))
        else:
            raise utils.TestsError(u"Клиенту доступна техсвязка при подключенном автоовердрафте")


@pytest.mark.parametrize('context', [
    DIRECT_RUB,
    # DIRECT_KZ,
    # DIRECT_KZ_QUASI,
    # DIRECT_BEL
])
def test_brand_contract_enable_to_client_having_zero_autooverdraft(context):
    limit = 10000
    qty_limit = get_qty_limit(context, limit)
    dt = datetime.datetime.now()

    client_id_1 = steps.ClientSteps.create()
    person_id_1 = steps.PersonSteps.create(client_id_1, context.person_type.code)

    client_id_2 = steps.ClientSteps.create()
    person_id_2 = steps.PersonSteps.create(client_id_2, context.person_type.code)

    with reporter.step(u'Переходим на мультивалютность копированием'):
        steps.ClientSteps.migrate_to_currency(client_id_1, currency_convert_type='COPY',
                                              dt=LAST_DAY_OF_PREVIOUS_MONTH - relativedelta(days=5),
                                              currency=context.currency.iso_code, region_id=context.region.id)

    with reporter.step(u'Переходим на мультивалютность копированием'):
        steps.ClientSteps.migrate_to_currency(client_id_2, currency_convert_type='COPY',
                                              dt=LAST_DAY_OF_PREVIOUS_MONTH - relativedelta(days=5),
                                              currency=context.currency.iso_code, region_id=context.region.id)

    with reporter.step(u'Подключаем овердрафт'):
        steps.OverdraftSteps.set_force_overdraft(client_id_2, context.service.id, limit, context.firm.id,
                                                 currency=context.currency.iso_code,
                                                 limit_wo_tax=int(D(limit) / (1 + D(context.nds) / 100)))

    with reporter.step(u'Подключаем автоовердрафт c нулевым лимитом'):
        steps.OverdraftSteps.set_overdraft_params(person_id=person_id_2, client_limit=0,
                                                  iso_currency=context.currency.iso_code)

    with reporter.step(u'Заключаем договор о тех связке с клиентом, которому подключен нулевой автоовердрафт'):
        _, _ = steps.ContractSteps.create_brand_contract(client_id_1, client_id_2, dt=dt)


@pytest.mark.parametrize('context', [
    DIRECT_RUB,
    # DIRECT_KZ,
    # DIRECT_KZ_QUASI,
    # DIRECT_BEL
])
def test_brand_contract_enable_with_client_having_zero_autooverdraft(context):
    limit = 10000
    qty_limit = get_qty_limit(context, limit)
    dt = datetime.datetime.now()

    client_id_1 = steps.ClientSteps.create()
    person_id_1 = steps.PersonSteps.create(client_id_1, context.person_type.code)

    client_id_2 = steps.ClientSteps.create()
    person_id_2 = steps.PersonSteps.create(client_id_2, context.person_type.code)

    with reporter.step(u'Переходим на мультивалютность копированием'):
        steps.ClientSteps.migrate_to_currency(client_id_1, currency_convert_type='COPY',
                                              dt=LAST_DAY_OF_PREVIOUS_MONTH - relativedelta(days=5),
                                              currency=context.currency.iso_code, region_id=context.region.id)

    with reporter.step(u'Переходим на мультивалютность копированием'):
        steps.ClientSteps.migrate_to_currency(client_id_2, currency_convert_type='COPY',
                                              dt=LAST_DAY_OF_PREVIOUS_MONTH - relativedelta(days=5),
                                              currency=context.currency.iso_code, region_id=context.region.id)

    with reporter.step(u'Подключаем овердрафт'):
        steps.OverdraftSteps.set_force_overdraft(client_id_2, context.service.id, limit, context.firm.id,
                                                 currency=context.currency.iso_code,
                                                 limit_wo_tax=int(D(limit) / (1 + D(context.nds) / 100)))

    with reporter.step(u'Подключаем автоовердрафт c нулевым лимитом'):
        steps.OverdraftSteps.set_overdraft_params(person_id=person_id_2, client_limit=0,
                                                  iso_currency=context.currency.iso_code)

    with reporter.step(u'Заключаем договор о тех связке для клиента, которому подключен нулевой автоовердрафт'):
        _, _ = steps.ContractSteps.create_brand_contract(client_id_1, client_id_2, dt=dt)


@pytest.mark.parametrize('services', [
    [Services.SHOP.id, Services.DIRECT.id],
    [Services.SHOP.id]
])
@pytest.mark.parametrize('autooverdraft_limit', [
    # 0,
    1000
])
def test_prepay_contract_having_autooverdraft(services, autooverdraft_limit):
    context = DIRECT_RUB
    limit = 10000

    client_id = steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, context.person_type.code)

    person_id_no_autooverdraft = steps.PersonSteps.create(client_id, context.person_type.code)

    get_autooverdraft(context, client_id, person_id, limit, autooverdraft_limit)

    if autooverdraft_limit > 0 and Services.DIRECT.id in services:
        try:
            with reporter.step(u'Заключаем договор с плательщиком, которому доступен автоовердрафт'):
                prepare_direct_contract(client_id, person_id, services)
        except Exception, exc:
            utils.check_that(steps.CommonSteps.get_exception_code(exc), hamcrest.equal_to('CONTRACT_RULE_VIOLATION'))
            utils.check_that(steps.CommonSteps.get_exception_code(exc, tag_name='msg'),
                             hamcrest.equal_to(u'Rule violation: \'Плательщик не может быть использован в договоре, '
                                               u'так как для него настроен порог отключения. Пожалуйста, обратитесь '
                                               u'к менеджеру клиента или клиенту и попросите его изменить плательщика '
                                               u'в настройках порога.\''))
        else:
            raise utils.TestsError(u"С плательщиком, которому подключен автоовердафт, можно заключить "
                                   u"предоплатный договор")
    else:
        prepare_direct_contract(client_id, person_id, services)

    prepare_direct_contract(client_id, person_id_no_autooverdraft, services)


@pytest.mark.parametrize('services', [
    [Services.SHOP.id, Services.BAYAN.id],
    [Services.SHOP.id, Services.DIRECT.id]
])
@pytest.mark.parametrize('autooverdraft_limit', [
    # 0,
    1000
])
def test_prepay_contract_collateral_change_service_having_autooverdraft(services, autooverdraft_limit):
    dt = datetime.datetime.now()
    context = DIRECT_RUB
    limit = 10000

    client_id = steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, context.person_type.code)

    contract_id = prepare_direct_contract(client_id, person_id, services,
                                          additional_params={'SERVICES': [Services.SHOP.id]})

    get_autooverdraft(context, client_id, person_id, limit, autooverdraft_limit)

    collateral_params = {'CONTRACT2_ID': contract_id,
                         'DT': dt,
                         'IS_SIGNED': utils.Date.to_iso(utils.Date.nullify_time_of_date(dt)),
                         'SERVICES': services}

    if Services.DIRECT.id in services and autooverdraft_limit:
        try:
            with reporter.step(u'Заключаем допник с автоовердрафтным плательщиком о переходе '
                               u'на сервис с автоовердрафтом'):
                steps.ContractSteps.create_collateral(Collateral.CHANGE_SERVICES, collateral_params)
        except Exception, exc:
            utils.check_that(steps.CommonSteps.get_exception_code(exc), hamcrest.equal_to('CONTRACT_RULE_VIOLATION'))
            utils.check_that(steps.CommonSteps.get_exception_code(exc, tag_name='msg'),
                             hamcrest.equal_to(u'Rule violation: \'Плательщик не может быть использован в договоре, '
                                               u'так как для него настроен порог отключения. Пожалуйста, обратитесь '
                                               u'к менеджеру клиента или клиенту и попросите его изменить плательщика '
                                               u'в настройках порога.\''))
        else:
            raise utils.TestsError(u"С автоовердрафтным плательщиком можно заключить допник о переводе "
                                   u"на сервис с автооовердрафтом")
    else:
        with reporter.step(u'Заключаем допник с автоовердрафтным плательщиком о переходе на сервис без автоовердрафта'):
            steps.ContractSteps.create_collateral(Collateral.CHANGE_SERVICES, collateral_params)


@pytest.mark.parametrize('services', [
    [Services.SHOP.id, Services.DIRECT.id],
    [Services.SHOP.id]
])
@pytest.mark.parametrize('autooverdraft_limit', [
    # 0,
    1000
])
def test_to_prepay_contract_collateral_having_autooverdraft(services, autooverdraft_limit):
    dt = datetime.datetime.now()
    context = DIRECT_RUB
    limit = 10000

    client_id = steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, context.person_type.code)

    contract_id = prepare_direct_contract(client_id, person_id, services,
                                          additional_params={'PAYMENT_TYPE': ContractPaymentType.POSTPAY,
                                                             'REPAYMENT_ON_CONSUME': 1,
                                                             'PERSONAL_ACCOUNT': 1,
                                                             'PERSONAL_ACCOUNT_FICTIVE': 1
                                                             })

    get_autooverdraft(context, client_id, person_id, limit, autooverdraft_limit)

    collateral_params = {'CONTRACT2_ID': contract_id,
                         'DT': dt,
                         'IS_SIGNED': utils.Date.to_iso(utils.Date.nullify_time_of_date(dt))}

    if Services.DIRECT.id in services and autooverdraft_limit:
        try:
            with reporter.step(u'Заключаем допник с автоовердрафтным плательщиком о переходе на предоплату '
                               u'при наличии в договоре автоовердрафтного сервиса'):
                steps.ContractSteps.create_collateral(Collateral.DO_PREPAY, collateral_params)
        except Exception, exc:
            utils.check_that(steps.CommonSteps.get_exception_code(exc), hamcrest.equal_to('CONTRACT_RULE_VIOLATION'))
            utils.check_that(steps.CommonSteps.get_exception_code(exc, tag_name='msg'),
                             hamcrest.equal_to(u'Rule violation: \'Плательщик не может быть использован в договоре, '
                                               u'так как для него настроен порог отключения. Пожалуйста, обратитесь '
                                               u'к менеджеру клиента или клиенту и попросите его изменить плательщика '
                                               u'в настройках порога.\''))
        else:
            raise utils.TestsError(u"С автоовердрафтным плательщиком можно заключить допник с постоплатным договорм "
                                   u"на автоовердрафтный сервис о переводе на сервис с автооовердрафтом")
    else:
        with reporter.step(u'Заключаем допник с автоовердрафтным плательщиком о переходе на предоплату при наличии '
                           u'в договоре сервиса без автоовердрафта'):
            steps.ContractSteps.create_collateral(Collateral.DO_PREPAY, collateral_params)


@pytest.mark.parametrize('services', [
    [Services.SHOP.id, Services.DIRECT.id],
    [Services.SHOP.id]
])
@pytest.mark.parametrize('autooverdraft_limit', [
    # 0,
    1000
])
def test_prepay_contract_collateral_change_service_having_prepay_contract_and_autoover(services, autooverdraft_limit):
    dt = datetime.datetime.now()
    context = DIRECT_RUB
    limit = 10000

    client_id = steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, context.person_type.code)

    with reporter.step(u'Переходим на мультивалютность копированием'):
        steps.ClientSteps.migrate_to_currency(client_id, currency_convert_type='COPY',
                                              dt=LAST_DAY_OF_PREVIOUS_MONTH - relativedelta(days=5),
                                              currency=context.currency.iso_code, region_id=context.region.id)

    with reporter.step(u'Подключаем овердрафт'):
        steps.OverdraftSteps.set_force_overdraft(client_id, context.service.id, limit, context.firm.id,
                                                 currency=context.currency.iso_code,
                                                 limit_wo_tax=int(D(limit) / (1 + D(context.nds) / 100)))

    contract_id = prepare_direct_contract(client_id, person_id, services)

    with reporter.step(u'Подключаем автоовердрафт'):
        steps.OverdraftSteps.set_overdraft_params(person_id=person_id, client_limit=autooverdraft_limit,
                                                  iso_currency=context.currency.iso_code,
                                                  payment_method='bank')

    services = [Services.BAYAN.id]
    collateral_params = {'CONTRACT2_ID': contract_id,
                         'DT': dt,
                         'IS_SIGNED': utils.Date.to_iso(utils.Date.nullify_time_of_date(dt)),
                         'SERVICES': services}

    if Services.DIRECT.id in services and autooverdraft_limit:
        try:
            with reporter.step(u'Заключаем допник с автоовердрафтным плательщиком и предоплатным договором '
                               u'на сервис с автоовердрафтом'):
                steps.ContractSteps.create_collateral(Collateral.CHANGE_SERVICES, collateral_params)
        except Exception, exc:
            utils.check_that(steps.CommonSteps.get_exception_code(exc), hamcrest.equal_to('CONTRACT_RULE_VIOLATION'))
            utils.check_that(steps.CommonSteps.get_exception_code(exc, tag_name='msg'),
                             hamcrest.equal_to(u'Rule violation: \'Плательщик не может быть использован в договоре, '
                                               u'так как для него настроен порог отключения. Пожалуйста, обратитесь '
                                               u'к менеджеру клиента или клиенту и попросите его изменить плательщика '
                                               u'в настройках порога.\''))
        else:
            raise utils.TestsError(u"С автоовердрафтным плательщиком можно заключить допник при наличии предоплатного "
                                   u"этоговора на автоовердрафтный сервис")
    else:
        with reporter.step(u'Заключаем допник с автоовердрафтным плательщиком и предоплатным договором '
                           u'на сервис без автоовердрафта'):
            steps.ContractSteps.create_collateral(Collateral.CHANGE_SERVICES, collateral_params)


#
def get_autooverdraft(context, client_id, person_id, overdraft_limit, autooverdraft_limit, payment_method='bank'):
    with reporter.step(u'Переходим на мультивалютность копированием'):
        steps.ClientSteps.migrate_to_currency(client_id, currency_convert_type='COPY',
                                              dt=LAST_DAY_OF_PREVIOUS_MONTH - relativedelta(days=5),
                                              currency=context.currency.iso_code, region_id=context.region.id)

    with reporter.step(u'Подключаем овердрафт'):
        steps.OverdraftSteps.set_force_overdraft(client_id, context.service.id, overdraft_limit, context.firm.id,
                                                 currency=context.currency.iso_code,
                                                 limit_wo_tax=int(D(overdraft_limit) / (1 + D(context.nds) / 100)))

    with reporter.step(u'Подключаем автоовердрафт'):
        answer = steps.OverdraftSteps.set_overdraft_params(person_id=person_id, client_limit=autooverdraft_limit,
                                                           iso_currency=context.currency.iso_code,
                                                           payment_method=payment_method)

    return answer


def get_qty_limit(context, limit):
    return int(D(limit) / (1 + D(context.nds) / 100)) if context.is_quasi else limit


def get_orders_and_invoice(context, client_id, person_id, dt, qty, completion_qty):
    service_order_id = steps.OrderSteps.next_id(service_id=context.service.id)
    service_order_id_2 = steps.OrderSteps.next_id(service_id=context.service.id)
    main_order_id = steps.OrderSteps.create(client_id, service_order_id, service_id=context.service.id,
                                            product_id=context.product.id, params={'AgencyID': None})
    child_order_id = steps.OrderSteps.create(client_id, service_order_id_2, service_id=context.service.id,
                                             product_id=context.product.id,
                                             params={'AgencyID': None, 'GroupServiceOrderID': service_order_id})

    orders_list = [{'ServiceID': context.service.id, 'ServiceOrderID': service_order_id_2, 'Qty': qty}]
    request_id = steps.RequestSteps.create(client_id, orders_list, additional_params=dict(InvoiceDesireDT=dt))
    invoice_id, _, _ = steps.InvoiceSteps.create(request_id, person_id, context.paysys.id, credit=0,
                                                 contract_id=None, overdraft=0, endbuyer_id=None)

    steps.InvoiceSteps.pay(invoice_id)
    steps.InvoiceSteps.set_turn_on_dt(invoice_id, dt)
    steps.CampaignsSteps.do_campaigns(context.service.id, service_order_id_2, {'Money': completion_qty}, 0, dt)

    steps.CommonSteps.export('UA_TRANSFER', 'Client', client_id, input_={'for_dt': datetime.datetime.now()})

    return invoice_id, main_order_id, child_order_id


def get_act_and_autooverdraft_invoice(client_id, dt):
    with reporter.step(u'Выставляем автоовердрафтный счет и актимся'):
        autooverdraft_id = db.balance().execute("SELECT ID FROM bo.t_overdraft_params WHERE client_id =:item",
                                                {'item': client_id})[0]['id']
        steps.CommonSteps.export('AUTO_OVERDRAFT', 'OverdraftParams', autooverdraft_id, with_enqueue=True)

        act_id = steps.ActsSteps.generate(client_id, force=1, date=dt)[0]


def check_client_notification(client_id, limit, overshipment):
    client_notification = steps.CommonSteps.build_notification(CLIENT_OPCODE, object_id=client_id)
    utils.check_that(D(client_notification['args'][0]['OverdraftLimit']), hamcrest.equal_to(D(limit)))
    utils.check_that(D(client_notification['args'][0]['OverdraftSpent']), hamcrest.equal_to(D(overshipment)))


def check_order_notification(order_id, limit, overshipment, is_notification_expected=False):
    order_notification = steps.CommonSteps.build_notification(ORDER_OPCODE, order_id)['args'][0]
    if is_notification_expected:
        utils.check_that(D(order_notification.get('OverdraftLimit', None)), hamcrest.equal_to(D(limit)))
        utils.check_that(D(order_notification.get('OverdraftSpentQty', None)),
                         hamcrest.equal_to(D(overshipment)))
    else:
        utils.check_that(order_notification.get('OverdraftLimit', None), hamcrest.is_(None))
        utils.check_that(order_notification.get('OverdraftSpentQty', None), hamcrest.is_(None))


def get_and_check_autooverdraft_invoice_sum(context, client_id, limit, overshipment_qty, prepay_invoice_id):
    query = "SELECT TOTAL_SUM, TOTAL_ACT_SUM " \
            "FROM T_INVOICE " \
            "WHERE REQUEST_ID IN (SELECT ID from T_REQUEST WHERE CLIENT_ID = :client_id)" \
            "AND ID != :invoice_id"
    overdraft_invoice_sum = db.balance().execute(query, {'client_id': client_id,
                                                         'invoice_id': prepay_invoice_id})

    utils.check_that(len(overdraft_invoice_sum), hamcrest.equal_to(1),
                     step=u'Проверяем, что создан только один овердрафтный счет')

    utils.check_that(D(overdraft_invoice_sum[0]['total_sum']), hamcrest.less_than_or_equal_to(D(limit)),
                     step=u'Проверяем, что овердрафтный счет выставлен на сумму не выше лимита')

    utils.check_that(D(overdraft_invoice_sum[0]['total_sum']),
                     hamcrest.equal_to(D(overshipment_qty)
                                       if not context.is_quasi
                                       else overshipment_qty * (1 + D(context.nds) / 100)),
                     step=u'Проверяем, что овердрафтный счет выставлен на сумму перекрута')

    utils.check_that(overdraft_invoice_sum[0]['total_act_sum'],
                     hamcrest.equal_to(overdraft_invoice_sum[0]['total_sum']),
                     step=u'Проверяем, что сумма акта и сумма овердрафтного счета совпадают')

    return overdraft_invoice_sum


def get_and_check_autooverdraft_skipped_export(autooverdraft_id, client_id, prepay_invoice_id):
    output = steps.ExportSteps.get_export_output(autooverdraft_id, 'OverdraftParams', 'AUTO_OVERDRAFT')
    utils.check_that(output,
                     hamcrest.equal_to('Zero sum invoice'),
                     step=u'Проверяем фиктивность обработки автоовердрафта в экспорте')
    query = "SELECT ID " \
            "FROM T_INVOICE " \
            "WHERE REQUEST_ID IN (SELECT ID from T_REQUEST WHERE CLIENT_ID = :client_id)" \
            "AND ID != :invoice_id"
    overdraft_invoices = db.balance().execute(query, {'client_id': client_id, 'invoice_id': prepay_invoice_id})
    utils.check_that(len(overdraft_invoices), hamcrest.equal_to(0),
                     step=u'Проверяем отсутствие овердрафтных счетов')


def prepare_direct_contract(client_id, person_id, services, additional_params=None):
    dt = datetime.datetime.now()

    additional_params = additional_params or {}

    params = {
        'CLIENT_ID': client_id,
        'PERSON_ID': person_id,
        'SERVICES': services,
        'FIRM': Firms.YANDEX_1.id,
        'CURRENCY': Currencies.RUB.num_code,
        'PAYMENT_TYPE': ContractPaymentType.PREPAY,
        'DT': dt - relativedelta(months=15),
        'FINISH_DT': dt + relativedelta(months=12),
        'IS_SIGNED': utils.Date.to_iso(utils.Date.nullify_time_of_date(dt - relativedelta(months=15)))
    }

    params.update(additional_params)

    contract_id, _ = steps.ContractSteps.create_contract_new(ContractCommissionType.NO_AGENCY, params)

    return contract_id
