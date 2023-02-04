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


payment_id = 6605601460

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