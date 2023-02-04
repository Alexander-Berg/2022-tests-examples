# -*- coding: utf-8 -*-
import hamcrest

from balance import balance_db as db
from balance import balance_steps as steps
from btestlib import reporter
from btestlib import utils
from cashmachines.darkspirit_steps import ReceiptsSteps

payment_id = 6631404392

steps.CommonSteps.export('CASH_REGISTER', 'Payment', payment_id)

with reporter.step(u'Проверяем, что input пуст в t_export'):
    utils.check_that(steps.ExportSteps.get_export_data(payment_id, 'Payment', 'CASH_REGISTER')['input'], hamcrest.equal_to('None'))

with reporter.step(u'Получаем краткие данные чека из таблицы BO.T_FISCAL_RECEIPT'):
    query = "SELECT RECEIPT_FN, RECEIPT_FD, RECEIPT_FPD FROM T_FISCAL_RECEIPT WHERE PAYMENT_ID=:item"
    check_data = db.balance().execute(query, {'item': payment_id})[0]

# with reporter.step(u'Проверяем наличие чека с данными из BO.T_FISCAL_RECEIPT в даркспирите'):
#     check = ReceiptsSteps.get_receipt(check_data['receipt_fn'], check_data['receipt_fd'],
#                                       check_data['receipt_fpd'])
