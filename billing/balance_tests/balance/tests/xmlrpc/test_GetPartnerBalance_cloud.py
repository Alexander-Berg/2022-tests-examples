# coding: utf-8

from balance.features import Features

__author__ = 'a-vasin'

from datetime import datetime, timedelta
from decimal import Decimal as D

import pytest
from pytest import param
from hamcrest import anything, contains_inanyorder, contains

import balance.balance_steps as steps
import balance.balance_db as db
import btestlib.utils as utils
from balance.tests.partner_schema_acts.test_cloud_acts import CLOUD_KZ_PH_CONTEXT
from btestlib import reporter
from btestlib.constants import PersonTypes, Regions, Collateral
from btestlib.matchers import contains_dicts_equal_to
from btestlib.data.partner_contexts import CLOUD_RU_CONTEXT as UR_CONTEXT, CLOUD_KZ_CONTEXT

CURRENT_MONTH_START = utils.Date.first_day_of_month(datetime.now())
THREE_MONTHS_AGO_START, THREE_MONTHS_AGO_END, _, TWO_MONTHS_AGO_END, _, _ = utils.Date.previous_three_months_start_end_dates()

AMOUNT = D('38.36')

PH_CONTEXT = UR_CONTEXT.new(person_type=PersonTypes.PH)

pytestmark = [reporter.feature(Features.CLOUD), reporter.feature(Features.XMLRPC), pytest.mark.tickets('BALANCE-29211',
                                                                                                       'BALANCE-26562')]


@pytest.mark.smoke
@pytest.mark.parametrize("context, is_expired, expected", [
    param(UR_CONTEXT, True,
          lambda ctx: ctx.new(first_debt_amount=ctx.act_sum,
                              first_debt_dt=ctx.act_dt,
                              expired_debt_amount=ctx.act_sum,
                              expired_dt=calc_expired_dt(region=ctx.region.id, debt_dt=ctx.act_dt, payment_term=ctx.payment_term)),
          id='act_expired_UR'),
    param(PH_CONTEXT, False,
          lambda ctx: ctx.new(first_debt_amount=ctx.act_sum,
                              first_debt_dt=ctx.act_dt,
                              expired_debt_amount=None,
                              expired_dt=None),
          id='act_not_expired_PH'),
    param(CLOUD_KZ_CONTEXT, True,
          lambda ctx: ctx.new(first_debt_amount=ctx.act_sum,
                              first_debt_dt=ctx.act_dt,
                              expired_debt_amount=ctx.act_sum,
                              expired_dt=calc_expired_dt(region=ctx.region.id, debt_dt=ctx.act_dt, payment_term=ctx.payment_term)),
          id='act_expired_UR_KZ'),
    param(CLOUD_KZ_PH_CONTEXT, False,
          lambda ctx: ctx.new(first_debt_amount=ctx.act_sum,
                              first_debt_dt=ctx.act_dt,
                              expired_debt_amount=None,
                              expired_dt=None),
          id='act_not_expired_PH_KZ')
])
def test_debt_expiration_date(context, is_expired, expected):
    # igogor: на всякий случай, чтобы не поменять состояние глобального объекта, при прямых руках не нужно
    ctx = context.new()

    act_dt = THREE_MONTHS_AGO_END
    ctx = prepare_contract(ctx, payment_term=calc_payment_term(act_dt=act_dt, is_expired=is_expired,
                                                               region_id=ctx.region.id))
    ctx = prepare_act(ctx, act_sum=AMOUNT, act_dt=act_dt)

    partner_balance = steps.PartnerSteps.get_partner_balance(ctx.service, [ctx.contract_id])

    ctx = expected(ctx)
    expected_partner_balance = expected_balance(ctx)

    utils.check_that(partner_balance, contains_dicts_equal_to([expected_partner_balance]),
                     u'Проверяем ответ метода GetPartnerBalance')


@pytest.mark.parametrize("context", [PH_CONTEXT, CLOUD_KZ_PH_CONTEXT])
def test_without_acts(context):
    ctx = context.new()

    ctx = prepare_contract(ctx)

    partner_balance = steps.PartnerSteps.get_partner_balance(ctx.service, [ctx.contract_id])

    expected_partner_balance = expected_balance(ctx)

    utils.check_that(partner_balance, contains_dicts_equal_to([expected_partner_balance]),
                     u'Проверяем ответ метода GetPartnerBalance')


@pytest.mark.parametrize("context, act_sum, payment_sum, expected", [
    param(PH_CONTEXT, AMOUNT, AMOUNT * 2,
          lambda ctx: ctx.new(receipt_sum=ctx.payment_sum,
                              first_debt_amount=None,
                              first_debt_dt=None,
                              expired_debt_amount=None,
                              expired_dt=None),
          id='Payment_more_than_act_No_debt_PH'),
    param(UR_CONTEXT, AMOUNT, AMOUNT,
          lambda ctx: ctx.new(receipt_sum=ctx.payment_sum,
                              first_debt_amount=None,
                              first_debt_dt=None,
                              expired_debt_amount=None,
                              expired_dt=None),
          id='Payment_equal_to_act_No_debt_UR'),
    param(PH_CONTEXT, AMOUNT * 2, AMOUNT,
          lambda ctx: ctx.new(receipt_sum=ctx.payment_sum,
                              first_debt_amount=ctx.act_sum - ctx.payment_sum,
                              first_debt_dt=ctx.act_dt,
                              expired_debt_amount=ctx.act_sum - ctx.payment_sum,
                              expired_dt=calc_expired_dt(region=ctx.region.id, debt_dt=ctx.act_dt, payment_term=ctx.payment_term)),
          id='Payment_less_than_act_Has_debt_PH'),
    param(CLOUD_KZ_PH_CONTEXT, AMOUNT, AMOUNT * 2,
          lambda ctx: ctx.new(receipt_sum=ctx.payment_sum,
                              first_debt_amount=None,
                              first_debt_dt=None,
                              expired_debt_amount=None,
                              expired_dt=None),
          id='Payment_more_than_act_No_debt_PH_KZ'),
    param(CLOUD_KZ_CONTEXT, AMOUNT, AMOUNT,
          lambda ctx: ctx.new(receipt_sum=ctx.payment_sum,
                              first_debt_amount=None,
                              first_debt_dt=None,
                              expired_debt_amount=None,
                              expired_dt=None),
          id='Payment_equal_to_act_No_debt_UR_KZ'),
    param(CLOUD_KZ_PH_CONTEXT, AMOUNT * 2, AMOUNT,
          lambda ctx: ctx.new(receipt_sum=ctx.payment_sum,
                              first_debt_amount=ctx.act_sum - ctx.payment_sum,
                              first_debt_dt=ctx.act_dt,
                              expired_debt_amount=ctx.act_sum - ctx.payment_sum,
                              expired_dt=calc_expired_dt(region=ctx.region.id, debt_dt=ctx.act_dt, payment_term=ctx.payment_term)),
          id='Payment_less_than_act_Has_debt_PH_KZ'),

])
def test_debt_amount(context, act_sum, payment_sum, expected):
    ctx = context.new()

    ctx = prepare_contract(ctx)
    ctx = prepare_act(ctx, act_sum=act_sum)
    ctx = make_payment(ctx, sum=payment_sum)

    partner_balance = steps.PartnerSteps.get_partner_balance(ctx.service, [ctx.contract_id])

    ctx = expected(ctx)
    expected_partner_balance = expected_balance(ctx)

    utils.check_that(partner_balance, contains_dicts_equal_to([expected_partner_balance]),
                     u'Проверяем ответ метода GetPartnerBalance')


@pytest.mark.parametrize("context, act_dt_2, payment_sum, expected", [
    param(PH_CONTEXT, TWO_MONTHS_AGO_END, AMOUNT,
          lambda ctx: ctx.new(first_debt_dt=ctx.act_dt_1,
                              first_debt_amount=ctx.act_sum_1 - ctx.payment_sum,
                              expired_dt=calc_expired_dt(region=ctx.region.id, debt_dt=ctx.act_dt_1, payment_term=ctx.payment_term),
                              expired_debt_amount=ctx.act_sum_1 + ctx.act_sum_2 - ctx.payment_sum),
          id='payment_less_than_first_act_PH'),
    param(UR_CONTEXT, TWO_MONTHS_AGO_END, AMOUNT * 3,
          lambda ctx: ctx.new(first_debt_dt=ctx.act_dt_2,
                              first_debt_amount=ctx.act_sum_2,
                              expired_dt=calc_expired_dt(region=ctx.region.id, debt_dt=ctx.act_dt_2, payment_term=ctx.payment_term),
                              expired_debt_amount=ctx.act_sum_2),
          id='payment_equal_to_first_act_UR'),
    param(UR_CONTEXT, TWO_MONTHS_AGO_END, AMOUNT * 5,
          lambda ctx: ctx.new(first_debt_dt=None,
                              first_debt_amount=None,
                              expired_dt=None,
                              expired_debt_amount=None),
          id='payment_more_than_both_acts_PH'),
    param(UR_CONTEXT, CURRENT_MONTH_START, AMOUNT,
          lambda ctx: ctx.new(first_debt_dt=ctx.act_dt_1,
                              first_debt_amount=ctx.act_sum_1 - ctx.payment_sum,
                              expired_dt=calc_expired_dt(region=ctx.region.id, debt_dt=ctx.act_dt_1, payment_term=ctx.payment_term),
                              expired_debt_amount=ctx.act_sum_1 - ctx.payment_sum),
          id='first_act_expired_second_not_UR'),
    param(CLOUD_KZ_PH_CONTEXT, TWO_MONTHS_AGO_END, AMOUNT,
          lambda ctx: ctx.new(first_debt_dt=ctx.act_dt_1,
                              first_debt_amount=ctx.act_sum_1 - ctx.payment_sum,
                              expired_dt=calc_expired_dt(region=ctx.region.id, debt_dt=ctx.act_dt_1, payment_term=ctx.payment_term),
                              expired_debt_amount=ctx.act_sum_1 + ctx.act_sum_2 - ctx.payment_sum),
          id='payment_less_than_first_act_PH_KZ'),
    param(CLOUD_KZ_CONTEXT, TWO_MONTHS_AGO_END, AMOUNT * 3,
          lambda ctx: ctx.new(first_debt_dt=ctx.act_dt_2,
                              first_debt_amount=ctx.act_sum_2,
                              expired_dt=calc_expired_dt(region=ctx.region.id, debt_dt=ctx.act_dt_2, payment_term=ctx.payment_term),
                              expired_debt_amount=ctx.act_sum_2),
          id='payment_equal_to_first_act_UR_KZ'),
    param(CLOUD_KZ_CONTEXT, TWO_MONTHS_AGO_END, AMOUNT * 5,
          lambda ctx: ctx.new(first_debt_dt=None,
                              first_debt_amount=None,
                              expired_dt=None,
                              expired_debt_amount=None),
          id='payment_more_than_both_acts_PH_KZ'),
    param(CLOUD_KZ_CONTEXT, CURRENT_MONTH_START, AMOUNT,
          lambda ctx: ctx.new(first_debt_dt=ctx.act_dt_1,
                              first_debt_amount=ctx.act_sum_1 - ctx.payment_sum,
                              expired_dt=calc_expired_dt(region=ctx.region.id, debt_dt=ctx.act_dt_1, payment_term=ctx.payment_term),
                              expired_debt_amount=ctx.act_sum_1 - ctx.payment_sum),
          id='first_act_expired_second_not_UR_KZ')
])
def test_several_acts(context, act_dt_2, payment_sum, expected):
    ctx = context.new()

    ctx = prepare_contract(ctx, payment_term=20)
    ctx = prepare_act(ctx, act_dt=THREE_MONTHS_AGO_END, act_sum=AMOUNT * 3)
    # igogor: сохраняем данные о первом акте явно, т.к. они перетрутся в функции
    ctx = ctx.new(act_dt_1=ctx.act_dt, act_sum_1=ctx.act_sum)

    ctx = prepare_act(ctx, act_sum=AMOUNT * 2, act_dt=act_dt_2)
    # igogor: сохраняем данные о втором акте явно для единообразия
    ctx = ctx.new(act_dt_2=ctx.act_dt, act_sum_2=ctx.act_sum)

    ctx = make_payment(ctx, sum=payment_sum)

    partner_balance = steps.PartnerSteps.get_partner_balance(ctx.service, [ctx.contract_id])

    ctx = expected(ctx).new(act_sum=ctx.act_sum_1 + ctx.act_sum_2, receipt_sum=ctx.payment_sum)

    utils.check_that(partner_balance, contains_dicts_equal_to([expected_balance(ctx)]),
                     u'Проверяем ответ метода GetPartnerBalance')


@pytest.mark.parametrize("context", [UR_CONTEXT, CLOUD_KZ_CONTEXT])
def test_several_contracts(context):
    ctx = context.new()

    with reporter.step(u'Создаем первый договор, акт и платеж (без долга)'):
        ctx1 = prepare_contract(ctx)
        ctx1 = prepare_act(ctx1, act_sum=AMOUNT * 3, act_dt=THREE_MONTHS_AGO_END)
        ctx1 = make_payment(ctx1, sum=AMOUNT * 3)

        expected_balance_1 = expected_balance(ctx1.new(receipt_sum=ctx1.payment_sum))

    with reporter.step(u'Создаем второй договор на того же клиента, акт и платеж (с долгом)'):
        ctx2 = prepare_contract(ctx, client_id=ctx1.client_id)
        ctx2 = prepare_act(ctx2, act_sum=AMOUNT * 2, act_dt=TWO_MONTHS_AGO_END)
        ctx2 = make_payment(ctx2, sum=AMOUNT)

        expected_balance_2 = expected_balance(ctx2.new(
            receipt_sum=ctx2.payment_sum,
            first_debt_amount=ctx2.act_sum - ctx2.payment_sum,
            first_debt_dt=ctx2.act_dt,
            expired_debt_amount=ctx2.act_sum - ctx2.payment_sum,
            expired_dt=calc_expired_dt(region=ctx2.region.id, debt_dt=ctx2.act_dt, payment_term=ctx2.payment_term)))

    partner_balance = steps.PartnerSteps.get_partner_balance(ctx.service, [ctx1.contract_id, ctx2.contract_id])

    utils.check_that(partner_balance, contains_dicts_equal_to([expected_balance_1, expected_balance_2]),
                     u'Проверяем ответ метода GetPartnerBalance')


@pytest.mark.parametrize("context", [PH_CONTEXT, CLOUD_KZ_PH_CONTEXT])
def test_several_payments(context):
    ctx = context.new()

    ctx = prepare_contract(ctx)
    ctx = prepare_act(ctx, act_sum=AMOUNT * 2)

    with reporter.step(u'Совершаем первый платеж меньше суммы акта'):
        ctx = make_payment(ctx, sum=AMOUNT)
        ctx = ctx.new(payment_sum_1=ctx.payment_sum)

        partner_balance = steps.PartnerSteps.get_partner_balance(ctx.service, [ctx.contract_id])

    expected_balance_1 = expected_balance(ctx.new(
        receipt_sum=ctx.payment_sum,
        first_debt_amount=ctx.act_sum - ctx.payment_sum,
        first_debt_dt=ctx.act_dt,
        expired_debt_amount=ctx.act_sum - ctx.payment_sum,
        expired_dt=calc_expired_dt(region=ctx.region.id, debt_dt=ctx.act_dt, payment_term=ctx.payment_term)))
    utils.check_that(partner_balance, contains_dicts_equal_to([expected_balance_1]),
                     u'Проверяем что в ответе метода GetPartnerBalance есть информация о долге')

    with reporter.step(u'Совершаем второй платеж так, чтобы сумма платежей стала равна сумме акта'):
        ctx = make_payment(ctx, sum=AMOUNT)
        ctx = ctx.new(payment_sum_2=ctx.payment_sum)

        partner_balance = steps.PartnerSteps.get_partner_balance(ctx.service, [ctx.contract_id])

    expected_balance_2 = expected_balance(ctx.new(receipt_sum=ctx.payment_sum_1 + ctx.payment_sum_2,
                                                  first_debt_amount=None,
                                                  first_debt_dt=None,
                                                  expired_debt_amount=None,
                                                  expired_dt=None))
    utils.check_that(partner_balance, contains_dicts_equal_to([expected_balance_2]),
                     u'Проверяем что в ответе метода GetPartnerBalance нет информации о долге')


@pytest.mark.parametrize("context", [PH_CONTEXT])
def test_contract_with_collaterals(context):
    ctx = context.new()

    ctx = prepare_contract(ctx)
    ctx = prepare_contract_collateral(ctx)

    partner_balance = steps.PartnerSteps.get_partner_balance(ctx.service, [ctx.contract_id])

    expected_partner_balance = expected_balance(ctx)

    utils.check_that(partner_balance, contains_dicts_equal_to([expected_partner_balance]),
                     u'Проверяем что в ответе поля Ticket и Memo содержат всю историю изменений')


@pytest.mark.parametrize("context, edo_info", [
    param(UR_CONTEXT, [], id='no_edo'),
    param(UR_CONTEXT, [{}], id='with_edo'),
    param(UR_CONTEXT, [{}, {'status': 'INVITED_BY_ME'}], id='multiple_edos'),
    param(UR_CONTEXT, [{'to_dt': datetime.now()}], id='with_filtered_edo'),
])
def test_contract_with_edo_info(context, edo_info):
    ctx = context.new()

    ctx = prepare_contract(ctx)
    ctx = prepare_contract_collateral(ctx)

    for edo in edo_info:
        ctx = accept_edo(ctx, **edo)

    partner_balance = steps.PartnerSteps.get_partner_balance(ctx.service, [ctx.contract_id])

    expected_partner_balance = expected_balance(ctx)

    utils.check_that(partner_balance, contains_dicts_equal_to([expected_partner_balance]),
                     u'Проверяем что в ответе поля Ticket и Memo содержат всю историю изменений')


# -------------------------
# Utils
def prepare_contract(context, payment_term=5, client_id=None):
    with reporter.step(u'Подготавливаем договор {} с плательщиком {}, сроком погащения кредита {}'.format(
            u'на клиента {}'.format(client_id) if client_id else u'',
            context.person_type.code,
            payment_term)):
        project_id = steps.PartnerSteps.create_cloud_project_uuid()

        # todo-igogor плохо что обновляем не только в конце перед ретерном
        context = context.new(project_id=project_id, payment_term=payment_term)

        client_id, person_id, contract_id = create_contract(context, client_id=client_id)

        # ручка возвращает историю всех значений для полей тикета и описания, формируем ожидаемый в ответе список
        response_tickets = [contains(anything(), context.tickets)] if context.tickets else []
        response_memo = [contains(anything(), u'Договор создан автоматически\n{}'.format(context.memo))]\
            if context.memo else []

        invoice_info_row = db.balance().execute(
            "select id, external_id from t_invoice where CONTRACT_ID = :contract_id and type = 'personal_account'",
            dict(contract_id=contract_id), single_row=True,
            descr=u'Получаем id лицевого счета')

        return context.new(contract_id=contract_id, client_id=client_id, person_id=person_id,
                           response_tickets=response_tickets, response_memo=response_memo,
                           personal_invoice_id=invoice_info_row['id'],
                           personal_invoice_eid=invoice_info_row['external_id'])


def prepare_contract_collateral(context, tickets='TEST-2', memo='description_2'):
    params = {'tickets': tickets, 'memo': memo, 'is_signed': datetime.now()}

    steps.ContractSteps.create_collateral_real(context.contract_id, Collateral.OTHER, params)

    # ручка возвращает историю всех значений для полей тикета и описания, формируем ожидаемый в ответе список
    response_tickets = (context.response_tickets or []) + ([contains(anything(), tickets)] if tickets else [])
    response_memo = (context.response_memo or []) + ([contains(anything(), memo)] if memo else [])

    return context.new(response_tickets=response_tickets, response_memo=response_memo)


def prepare_act(context, act_sum=AMOUNT, act_dt=TWO_MONTHS_AGO_END):
    with reporter.step(u'Подготавливаем акт по договору {} в периоде {}'.format(context.contract_id, act_dt)):
        steps.PartnerSteps.create_cloud_completion(context.contract_id, act_dt, act_sum, product=context.product)

        steps.CommonPartnerSteps.generate_partner_acts_fair_and_export(context.client_id, context.contract_id, act_dt)

        return context.new(act_sum=act_sum, act_dt=act_dt)


def create_contract(context, client_id=None):
    is_offer = 1 if context.person_type == PersonTypes.PH else 0
    client_id, person_id, contract_id, _ = steps.ContractSteps.create_partner_contract(context, client_id=client_id,
                                                                                       is_offer=is_offer,
                                                                                       additional_params={
                                                                                           'projects': [
                                                                                               context.project_id],
                                                                                           'payment_term': context.payment_term,
                                                                                           'start_dt': THREE_MONTHS_AGO_START,
                                                                                           'tickets': context.tickets,
                                                                                           'memo': context.memo,
                                                                                       })
    return client_id, person_id, contract_id


def make_payment(context, sum=None):
    if sum:
        context = context.new(payment_sum=sum)
    with reporter.step(u'Зачисляем {} на лицевой счет договора {}'.format(sum, context.contract_id)):
        steps.InvoiceSteps.pay(context.personal_invoice_id, payment_sum=context.payment_sum)

    return context


def accept_edo(context, from_dt=datetime.now(), to_dt=None, status='FRIENDS'):
    steps.PersonSteps.accept_edo(context.person_id, context.firm.id, from_dt, to_dt=to_dt, status=status, edo_type_id=1)

    if not to_dt:
        expected_edo_response = (context.edo or []) + [{
                'edo_type': u'Тензор',
                'status': status,
                'from_dt': utils.Date.nullify_microseconds_of_date(from_dt),
                'to_dt': None,
        }]

        return context.new(edo=expected_edo_response)

    return context


def expected_balance(context):
    return utils.remove_empty({
        'ContractID': context.contract_id,
        'DT': anything(),  # a-vasin: всегда сетится в now для GetPartnerBalance
        'ReceiptSum': context.receipt_sum or '0',
        'Currency': context.currency.iso_code,
        'ActSum': utils.dround(context.act_sum or D('0'), 2),
        'ConsumeSum': utils.dround(context.act_sum or D('0'), 2),
        'Amount': 0,  # igogor: для клауда всегда 0
        'FirstDebtAmount': context.first_debt_amount,
        'FirstDebtFromDT': utils.Date.date_to_iso_format(context.first_debt_dt, pass_none=True),
        'FirstDebtPaymentTermDT': utils.Date.date_to_iso_format(
            calc_expired_dt(region=context.region.id, debt_dt=context.first_debt_dt, payment_term=context.payment_term),
            pass_none=True),
        'ExpiredDebtAmount': context.expired_debt_amount,
        'ExpiredDT': utils.Date.date_to_iso_format(context.expired_dt, pass_none=True),
        'LastActDT': utils.Date.date_to_iso_format(
            context.act_dt and utils.Date.last_day_of_month(context.act_dt),
            pass_none=True),
        'PersonalAccountExternalID': context.personal_invoice_eid,
        'Tickets': contains_inanyorder(*context.response_tickets),
        'Memo': contains_inanyorder(*context.response_memo),
        'Edo': contains_dicts_equal_to(context.edo or []),
    })


def get_holidays(from_dt, to_dt=None, region_id=Regions.RU.id):
    query_holidays_count = """
    select count(*) as holidays
    from mv_working_calendar
    where region_id = :region_id
        and dt>= :start_dt
        and dt <= :expired_dt and day_descr='Праздник'
    """
    to_dt = to_dt or datetime.now()
    params = {
        'region_id': region_id,
        'start_dt': from_dt,
        'expired_dt': to_dt,
    }

    holiday_count = db.balance().execute(query_holidays_count, params)[0]['holidays']
    while True:
        to_dt += timedelta(days=1)
        params['start_dt'] = to_dt
        params['expired_dt'] = to_dt
        is_next_day_holiday = db.balance().execute(query_holidays_count, params)[0]['holidays']
        if not is_next_day_holiday:
            return holiday_count
        holiday_count += is_next_day_holiday


def calc_payment_term(act_dt, is_expired, region_id):
    payment_terms = [5, 10, 11, 12, 13, 14, 15, 20, 25, 30, 32, 34, 35, 40, 45, 50, 60, 75, 80, 90, 100, 120, 180]
    days_since_act = abs((datetime.now() - act_dt).days)
    holidays = get_holidays(act_dt, region_id=region_id)
    for i in range(len(payment_terms) - 1):
        if payment_terms[i] <= days_since_act - holidays < payment_terms[i + 1]:
            return payment_terms[i] if is_expired else payment_terms[i + 1]
    raise utils.TestsError('Cannot select payment_term, act_dt is too close')


def calc_expired_dt(debt_dt, payment_term, region=Regions.RU.id):
    if debt_dt is None:
        return None
    query = """
    select dt
    from mv_working_calendar
    where dt >= :start_dt and region_id = :region_id and (calendar_day = 1 or five_day = 1)
    order by dt
    offset :offset rows
    fetch next 1 rows only
    """
    res = db.balance().execute(query, {'start_dt': debt_dt, 'region_id': region, 'offset': payment_term})
    return res[0]['dt'] if res else debt_dt + timedelta(days=payment_term)
