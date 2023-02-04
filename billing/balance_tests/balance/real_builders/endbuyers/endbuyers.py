# -*- coding: utf-8 -*-

import datetime
from decimal import Decimal as D
from balance import balance_api as api
from jsonrpc import dispatcher
from balance import balance_db as db
import uuid

from balance import balance_steps as steps
from balance import balance_steps
from btestlib import utils
from temp.igogor.balance_objects import Contexts
from btestlib.constants import Currencies, Firms, ContractCommissionType, Services, PersonTypes, Paysyses, InvoiceType, \
    Products, Permissions, User, Users, Export
from btestlib.data.partner_contexts import CORP_TAXI_RU_CONTEXT_GENERAL_MIGRATED, ZAXI_RU_CONTEXT
from balance.real_builders import common_defaults
from balance.tests.conftest import get_free_user
from dateutil.relativedelta import relativedelta

to_iso = utils.Date.date_to_iso_format
dt_delta = utils.Date.dt_delta

NOW = datetime.datetime.now()
YESTERDAY = datetime.datetime.now() - datetime.timedelta(days=2)
TOMORROW = datetime.datetime.now() + datetime.timedelta(days=1)
FUTURE = datetime.datetime.now() + datetime.timedelta(days=5)
NOW_ISO = to_iso(NOW)
HALF_YEAR_AFTER_NOW_ISO = to_iso(NOW + datetime.timedelta(days=180))
HALF_YEAR_BEFORE_NOW_ISO = to_iso(NOW - datetime.timedelta(days=180))
ORDER_DT = NOW
INVOICE_DT = NOW
COMPLETIONS_DT = NOW
ACT_DT = NOW
END_DT = datetime.datetime(year=2025, month=1, day=1)
START_DT = datetime.datetime(year=2020, month=1, day=1)

CONTEXT = Contexts.DIRECT_FISH_RUB_CONTEXT.new(firm=Firms.YANDEX_1,
                                               contract_type=ContractCommissionType.NO_AGENCY)
QTY = D('250')
COMPLETIONS = D('99.99')


@dispatcher.add_method
def test_agency_with_comm_contract_and_endbuyer(login='yndx-balance-assessor-100'):
    SERVICES = [Services.MEDIA_BANNERS.id, Services.GEO.id, Services.MEDIA_70.id,
                Services.MEDIA_BANNERS_167.id, Services.DIRECT.id, Services.BAYAN.id,
                Services.BANKI.id, Services.SPAMDEF.id, Services.TECHNOLOGIES.id]
    CREDIT_LIMIT_RUB = D('5700')

    contract_params = {'DT': to_iso(START_DT),
                       'FINISH_DT': to_iso(END_DT),
                       'IS_SIGNED': to_iso(START_DT),
                       'PAYMENT_TYPE': 3,
                       'PAYMENT_TERM': 100,
                       'CREDIT_TYPE': 2,
                       'CREDIT_LIMIT_SINGLE': CREDIT_LIMIT_RUB,
                       'SERVICES': SERVICES,
                       'PERSONAL_ACCOUNT': 0,
                       'LIFT_CREDIT_ON_PAYMENT': 0,
                       'PERSONAL_ACCOUNT_FICTIVE': 0,
                       'CURRENCY': str(Currencies.RUB.num_code),
                       'FIRM': CONTEXT.firm.id,
                       'EXTERNAL_ID': u'договорище'
                       }
    client_id = steps.ClientSteps.create(params={'NAME': common_defaults.CLIENT_NAME, 'IS_AGENCY': 1})
    person_id = steps.PersonSteps.create(client_id, PersonTypes.UR.code, common_defaults.FIXED_UR_PARAMS)

    # Создаём договор:
    contract_params.update({'CLIENT_ID': client_id,
                            'PERSON_ID': person_id})
    contract_id, contract_external_id = steps.ContractSteps.create_contract_new(ContractCommissionType.COMMISS, contract_params)
    endbuyer_id = steps.PersonSteps.create(client_id, PersonTypes.ENDBUYER_UR.code, common_defaults.FIXED_ENDBUYER_UR_PARAMS)
    if login:
        steps.ClientSteps.link(client_id, login)
    return client_id