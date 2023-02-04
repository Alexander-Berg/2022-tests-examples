# -*- coding: utf-8 -*-
import datetime
from decimal import Decimal as D

from balance import balance_steps as steps
from btestlib import utils
from temp.igogor.balance_objects import Contexts

to_iso = utils.Date.date_to_iso_format
dt_delta = utils.Date.dt_delta

NOW = datetime.datetime.now()
NOW_ISO = to_iso(NOW)
HALF_YEAR_AFTER_NOW_ISO = to_iso(NOW + datetime.timedelta(days=180))
HALF_YEAR_BEFORE_NOW_ISO = to_iso(NOW - datetime.timedelta(days=180))
ORDER_DT = NOW
INVOICE_DT = NOW
COMPLETIONS_DT = NOW
ACT_DT = NOW

context = Contexts.DIRECT_MONEY_RUB_CONTEXT
QTY = D('250')
COMPLETIONS = D('100')

# Создаём клиента
# client_id = 10
# client_id = steps.ClientSteps.create(params={'login': 'test_login'})
#
# # Привязать клиента к логину
# # steps.ClientSteps.link(client_id, 'natabers')
#
# # Создаём плательщика
# person_params = {}
# person_id = None or steps.PersonSteps.create(client_id, context.person_type.code, person_params)
#
# # Создаём договор:
# contract_id, _ = steps.ContractSteps.create_contract('opt_agency_prem_post',
#                                                      {'CLIENT_ID': client_id,
#                                                      'PERSON_ID': person_id,
#                                                       'FIRM': '1',
#                                                       'CURRENCY': '810',  # RUR
#                                                       'BANK_DETAILS_ID': '7627',  # АО Юникредит Банк
#                                                       'MANAGER_CODE': '28133',  # Яковенко Екатерина Сергеевна
#                                                       'MANAGER_BO_CODE': '30726',  # Артельная Анна Витальевна
#                                                       'DT': to_iso(datetime.datetime(2020, 03, 01)),
#                                                       'FINISH_DT': to_iso(datetime.datetime(2021, 03, 01)),
#                                                       'UNILATERAL': '1',
#                                                       'TICKETS': 'BALANCEDUTY-207',
#                                                       'IS_SIGNED': to_iso(datetime.datetime(2020, 03, 01)),
#                                                       'SERVICES': [Services.GEO.id],
#                                                       # 'PRINT_TEMPLATE': '/sales/processing/Billing-agreements/YandexGSAP/opt/premium/2/',
#                                                       'WHOLESALE_AGENT_PREMIUM_AWARDS_SCALE_TYPE': '2',  # Премиум 2015
#                                                       'CREDIT_TYPE': '1',  # по сроку
#                                                       'PAYMENT_TERM': '45',  # 45 дней
#                                                       'CALC_DEFERMANT': '0',  # от даты акта
#                                                       # # 'COMMISSION_TYPE': 48,
#                                                       # # 'NON_RESIDENT_CLIENTS': 0,
#                                                       # # 'DEAL_PASSPORT': '2015-12-01T00:00:00',
#                                                       # 'REPAYMENT_ON_CONSUME': 0,
#                                                       # 'PERSONAL_ACCOUNT': 1,
#                                                       # 'LIFT_CREDIT_ON_PAYMENT': 0,
#                                                       # 'PERSONAL_ACCOUNT_FICTIVE': 1,
#                                                       'CREDIT_LIMIT_SINGLE': '1158648553',
#                                                       })

# Создаём доп.соглашение:
steps.ContractSteps.create_collateral(
    2160,
    {
        'contract2_id': 6269590,
        'dt': '2021-03-01T00:00:00',
    },
)
