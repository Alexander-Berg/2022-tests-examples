# -*- coding: utf-8 -*-
import datetime
import hamcrest
import pytest

from balance import balance_db as db
from balance import balance_steps as steps
from btestlib import utils, reporter
from btestlib.constants import Products, Paysyses, Regions, Processings
from btestlib.utils import CheckMode, check_mode, aDict
from simpleapi.common.payment_methods import PaystepCard
from simpleapi.data import uids_pool as uids
from simpleapi.data.cards_pool import ALPHA_PAYSTEP_VISA
from simpleapi.steps import balance_steps as not_true_balance_steps
from simpleapi.steps import paystep_steps as paystep
from balance.features import Features
from cashmachines.darkspirit_steps import ReceiptsSteps
from balance.integrations import Integrations


@reporter.feature(Features.PAYMENT)
@pytest.mark.tickets('BALANCE-27366', 'BALANCE-28653')
@pytest.mark.integration(Integrations.CHECK)
def test_check_info_in_t_fiscal_receipt():
    uid = uids.get_random_of_type(uids.Types.random_from_all)

    paymethod = PaystepCard(Processings.ALPHA)
    card = ALPHA_PAYSTEP_VISA

    client_id, person_id, invoice_id, external_invoice_id, total_invoice_sum = \
        not_true_balance_steps.prepare_data_for_paystep(context=aDict({'paysys': Paysyses.CC_PH_RUB}), user=uid,
                                                        person_type='ph', product=Products.DIRECT_FISH,
                                                        qty=100.456, dt=datetime.datetime.now())

    with check_mode(CheckMode.FAILED):
        paystep.pay_by(paymethod, Products.DIRECT_FISH.service, user=uid, card=card, region_id=Regions.RU.id,
                       invoice_id=invoice_id, data_for_checks={'invoice_id': invoice_id,
                                                               'external_id': external_invoice_id,
                                                               'total_sum': total_invoice_sum,
                                                               'currency_iso_code':
                                                                   Paysyses.CC_PH_RUB.currency.iso_code})

    payment_id = db.get_payments_by_invoice_id(invoice_id)[0]['id']
    steps.CommonSteps.export('CASH_REGISTER', 'Payment', payment_id)

    with reporter.step(u'Проверяем, что input пуст в t_export'):
        utils.check_that(steps.ExportSteps.get_export_data(payment_id, 'Payment', 'CASH_REGISTER')['input'],
                         hamcrest.equal_to('None'))

    with reporter.step(u'Получаем краткие данные чека из таблицы BO.T_FISCAL_RECEIPT'):
        query = "SELECT RECEIPT_FN, RECEIPT_FD, RECEIPT_FPD FROM T_FISCAL_RECEIPT WHERE PAYMENT_ID=:item"
        check_data = db.balance().execute(query, {'item': payment_id})[0]

    with reporter.step(u'Проверяем наличие чека с данными из BO.T_FISCAL_RECEIPT в даркспирите'):
        check = ReceiptsSteps.get_receipt(check_data['receipt_fn'], check_data['receipt_fd'],
                                          check_data['receipt_fpd'])
        utils.check_that(check['receipt_calculated_content']['rows'][0]['qty'],
                         hamcrest.equal_to('100.456'),
                         step='Проверяем совпадение количества фишек в чеке и в счете')


@reporter.feature(Features.PAYMENT)
@pytest.mark.tickets('BALANCE-29494')
@pytest.mark.integration(Integrations.CHECK)
@pytest.mark.parametrize('qty', [2924.0049, 1223.0050, 3122.0051])
# https://st.yandex-team.ru/TRUSTDUTY-868
def test_check_info_amount_around(qty):
    uid = uids.get_random_of_type(uids.Types.random_from_all)

    paymethod = PaystepCard(Processings.ALPHA)
    card = ALPHA_PAYSTEP_VISA

    client_id, person_id, invoice_id, external_invoice_id, total_invoice_sum = \
        not_true_balance_steps.prepare_data_for_paystep(context=aDict({'paysys': Paysyses.CC_PH_RUB}), user=uid,
                                                        person_type='ph', product=Products.DIRECT_RUB,
                                                        qty=qty, dt=datetime.datetime.now())

    with check_mode(CheckMode.FAILED):
        paystep.pay_by(paymethod, Products.DIRECT_RUB.service, user=uid, card=card, region_id=Regions.RU.id,
                       invoice_id=invoice_id, data_for_checks={'invoice_id': invoice_id,
                                                               'external_id': external_invoice_id,
                                                               'total_sum': total_invoice_sum,
                                                               'currency_iso_code':
                                                                   Paysyses.CC_PH_RUB.currency.iso_code})

    payment_id = db.get_payments_by_invoice_id(invoice_id)[0]['id']
    export = steps.CommonSteps.export('CASH_REGISTER', 'Payment', payment_id)
