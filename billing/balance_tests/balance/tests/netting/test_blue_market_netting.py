# coding: utf-8

from decimal import Decimal as D
from hamcrest import anything

import pytest

from balance import balance_steps as steps
from balance.balance_steps import new_taxi_steps as taxi_steps
import balance.balance_db as db
from balance.features import Features
from btestlib import reporter
from btestlib import utils
from btestlib.constants import OEBSOperationType, Services, TransactionType, ServiceCode, BlueMarketOrderType
from btestlib.data.partner_contexts import BLUE_MARKET_PAYMENTS, BLUE_MARKET_612_ISRAEL
from btestlib.matchers import contains_dicts_equal_to, contains_dicts_with_entries

pytestmark = [
    reporter.feature(Features.NETTING, Features.MARKET),
]

prev_month_start_dt, prev_month_end_dt = utils.Date.previous_month_first_and_last_days()

INPUT_ = {'code': 'blue_market_netting', 'forced': True}
PAYMENT_TYPE_MAPPING = {
    BLUE_MARKET_PAYMENTS.name: BlueMarketOrderType.fee,
    BLUE_MARKET_612_ISRAEL.name: BlueMarketOrderType.global_fee,
}


def prepare_instances(context):
    # Создаём клиента, партнёра, плательщика, договор
    client_id, person_id, contract_id, _ = steps.ContractSteps.create_partner_contract(
        context, is_offer=1, additional_params={'start_dt': prev_month_start_dt}
    )

    # получаем id счета
    invoice_id, invoice_eid, _ = steps.InvoiceSteps.get_invoice_by_service_or_service_code(
        contract_id, service_code=ServiceCode.YANDEX_SERVICE
    )

    expected_netting_rows = []

    gen_act = make_act_generator(prev_month_end_dt, client_id, contract_id,
                                 currency=context.currency.iso_code, payment_type=PAYMENT_TYPE_MAPPING[context.name])
    gen_cpf = make_oebs_cash_payment_fact_generator(prev_month_end_dt, invoice_id, invoice_eid)
    gogo_check = make_check_netting(client_id, contract_id, person_id, invoice_id, invoice_eid,
                                    context, expected_netting_rows)

    return client_id, contract_id, gen_act, gen_cpf, gogo_check


@pytest.mark.parametrize('context', [
    BLUE_MARKET_PAYMENTS,
    BLUE_MARKET_612_ISRAEL,
], ids=lambda context: context.name)
def test_netting(context):
    client_id, contract_id, gen_act, gen_cpf, gogo_check = prepare_instances(context)

    total_acts_amount = D('0')
    user_payments_amount = D('0')
    total_oebs_insert_netting_amount = D('0')


    with reporter.step('#1. Начальные условаия - у пользователя 2 акта на 200 и 2 оплаты на 100'):
        act_amount = D(100)
        total_acts_amount += gen_act(act_amount)
        total_acts_amount += gen_act(act_amount)

        user_pay_amount = D(50)
        user_payments_amount += gen_cpf(user_pay_amount, OEBSOperationType.ONLINE)
        user_payments_amount += gen_cpf(user_pay_amount, OEBSOperationType.INSERT)

        # Взаимозачет - 200 рефанд, 100 payment
        with reporter.step(u'Запускаем взаимозачет для договора: {}'.format(contract_id)):
            steps.CommonSteps.export('PARTNER_PROCESSING', 'Contract', contract_id, input_=INPUT_)
        gogo_check(total_acts_amount, user_payments_amount)


    with reporter.step('#2. Юзер оплатил 50'):
        user_pay_amount = D(50)
        user_payments_amount += gen_cpf(user_pay_amount, OEBSOperationType.ONLINE)
        # убедимся, что process_payments проставил договор на экспорт PARTNER_PROCESSING
        expected_export_data = {
            'state': '0',
            'input': {"forced": 'True', "code": 'blue_market_netting'},
        }
        export_data = steps.ExportSteps.get_export_data(contract_id, 'Contract', 'PARTNER_PROCESSING')
        utils.check_that([export_data], contains_dicts_with_entries([expected_export_data]))

        # Взаимозачет - 50 payment
        with reporter.step(u'Запускаем взаимозачет для договора: {}'.format(contract_id)):
            steps.CommonSteps.export('PARTNER_PROCESSING', 'Contract', contract_id, input_=INPUT_)
        gogo_check(None, user_pay_amount)


    with reporter.step('#3. ОЕБС удержал 50'):
        insert_netting_amount = D(50)
        total_oebs_insert_netting_amount += gen_cpf(insert_netting_amount, OEBSOperationType.INSERT_NETTING)
        # убедимся, что process_payments проставил договор на экспорт PARTNER_PROCESSING
        expected_export_data = {
            'state': '0',
            'input': {"forced": 'True', "code": 'blue_market_netting'},
        }
        export_data = steps.ExportSteps.get_export_data(contract_id, 'Contract', 'PARTNER_PROCESSING')
        utils.check_that([export_data], contains_dicts_with_entries([expected_export_data]))

        # Взаимозачета нет
        with reporter.step(u'Запускаем взаимозачет для договора: {}'.format(contract_id)):
            steps.CommonSteps.export('PARTNER_PROCESSING', 'Contract', contract_id, input_=INPUT_)
        gogo_check(None, None)


    with reporter.step('#4. Выставился акт на 150'):
        act_amount = D(150)
        total_acts_amount += gen_act(act_amount)
        # Взаимозачет - 150 refund
        with reporter.step(u'Запускаем взаимозачет для договора: {}'.format(contract_id)):
            steps.CommonSteps.export('PARTNER_PROCESSING', 'Contract', contract_id, input_=INPUT_)
        gogo_check(act_amount, None)


    with reporter.step('#5. ОЕБС удержал 150 и юзер заплатил 150, образовалась переплата'):
        insert_netting_amount = D(150)
        total_oebs_insert_netting_amount += gen_cpf(insert_netting_amount, OEBSOperationType.INSERT_NETTING)
        user_pay_amount = D(150)
        user_payments_amount += gen_cpf(user_pay_amount, OEBSOperationType.ONLINE)

        # Взаимозачета нет
        with reporter.step(u'Запускаем взаимозачет для договора: {}'.format(contract_id)):
            steps.CommonSteps.export('PARTNER_PROCESSING', 'Contract', contract_id, input_=INPUT_)
        gogo_check(None, None)


    with reporter.step('#6. Выставился акт на 100, выставилась корректировка на 100 за счет переплаты'):
        act_amount = D(100)
        total_acts_amount += gen_act(act_amount)
        # Взаимозачет - 100 refund, 100 payment
        with reporter.step(u'Запускаем взаимозачет для договора: {}'.format(contract_id)):
            steps.CommonSteps.export('PARTNER_PROCESSING', 'Contract', contract_id, input_=INPUT_)
        gogo_check(act_amount, act_amount)


    with reporter.step('#7. Выставился акт на 100, выставилась корректировка на 50 за счет переплаты'):
        act_amount = D(100)
        total_acts_amount += gen_act(act_amount)
        # Взаимозачет - 100 refund, 50 payment
        with reporter.step(u'Запускаем взаимозачет для договора: {}'.format(contract_id)):
            steps.CommonSteps.export('PARTNER_PROCESSING', 'Contract', contract_id, input_=INPUT_)
        gogo_check(act_amount, D(50))

    # удаление акта должно быть в тесте всегда последним,
    # т.к. он удаляется тупо и сломается вся последующая генерация
    with reporter.step('#7. Удален последний акт на 100, выставился неттинг на -100'):
        act_data = steps.ActsSteps.get_all_act_data(client_id)
        last_act_id = max([ad['id'] for ad in act_data])
        steps.ActsSteps.hide_force(last_act_id)
        with reporter.step(u'Запускаем взаимозачет для договора: {}'.format(contract_id)):
            steps.CommonSteps.export('PARTNER_PROCESSING', 'Contract', contract_id, input_=INPUT_)
        gogo_check(-act_amount, None)


def make_act_generator(dt, client_id, contract_id, currency, payment_type):
    def wrapper(amount):
        return generate_act(amount, dt, client_id, contract_id, currency, payment_type)
    return wrapper


def generate_act(amount, dt, client_id, contract_id, currency, payment_type):
    with reporter.step(u'Создаем открутки и генериуем акт на дату: {}'.format(dt)):
        steps.PartnerSteps.create_fake_partner_stat_aggr_tlog_completion(dt,
                                                                         type_=payment_type,
                                                                         service_id=Services.BLUE_MARKET.id,
                                                                         client_id=client_id, amount=amount,
                                                                         last_transaction_id=0, currency=currency)
        steps.CommonPartnerSteps.generate_partner_acts_fair_and_export(client_id, contract_id, dt)

    return amount


def make_oebs_cash_payment_fact_generator(dt, invoice_id, invoice_eid):
    def wrapper(user_payment_amount, operation_type):
        return create_cash_payment_fact(user_payment_amount, operation_type, dt, invoice_id, invoice_eid)
    return wrapper


def create_cash_payment_fact(user_payment_amount, operation_type, dt, invoice_id, invoice_eid):
    taxi_steps.TaxiSteps.create_cash_payment_fact(invoice_eid, user_payment_amount, dt, operation_type)

    # бонусом создаем авансы, чтобы убедиться, что они не учитываются
    taxi_steps.TaxiSteps.create_cash_payment_fact(invoice_eid, user_payment_amount, dt, OEBSOperationType.SF_AVANS)
    taxi_steps.TaxiSteps.process_payment(invoice_id)
    return user_payment_amount


def make_check_netting(client_id, contract_id, person_id, invoice_id, invoice_eid, context, expected_netting_rows):
    def wrapper(refund_amount=None, payment_amount=None):
        return do_check(client_id, contract_id, person_id, invoice_id, invoice_eid, context, expected_netting_rows,
                        refund_amount, payment_amount)
    return wrapper


def do_check(client_id, contract_id, person_id, invoice_id, invoice_eid, context, expected_netting_rows,
             refund_amount=None, payment_amount=None):
    if refund_amount:
        expected_netting_rows.append(
                taxi_steps.TaxiData.create_expected_correction_data(client_id, contract_id, person_id,
                                                                    invoice_eid,
                                                                    anything(),
                                                                    refund_amount,
                                                                    context,
                                                                    transaction_type=TransactionType.REFUND,
                                                                    internal=0))
    if payment_amount:
        expected_netting_rows.append(
                taxi_steps.TaxiData.create_expected_correction_data(client_id, contract_id, person_id,
                                                                    invoice_eid,
                                                                    anything(),
                                                                    payment_amount,
                                                                    context,
                                                                    transaction_type=TransactionType.PAYMENT,
                                                                    internal=0))

    correction_data = taxi_steps.TaxiSteps.get_thirdparty_corrections_by_contract_id(contract_id)

    utils.check_that(correction_data, contains_dicts_equal_to(expected_netting_rows),
                     'Сравниваем данные по корректировке с шаблоном')


@pytest.mark.parametrize('context', [
    BLUE_MARKET_PAYMENTS,
], ids=lambda context: context.name)
def test_netting_payout_ready_dt(context):
    _, contract_id, gen_act, _, _ = prepare_instances(context)

    gen_act(D(100))
    with reporter.step(u'Запускаем взаимозачет для договора: {}'.format(contract_id)):
        steps.CommonSteps.export('PARTNER_PROCESSING', 'Contract', contract_id, input_=INPUT_)

    with reporter.step(u'Проверяем, что payout_ready_dt установлена в dt'):
        row = db.balance().execute(
            'select payout_ready_dt, dt from t_thirdparty_corrections where contract_id=:contract_id',
            dict(contract_id=contract_id)
        )[0]
        assert row['payout_ready_dt'] == row['dt']
