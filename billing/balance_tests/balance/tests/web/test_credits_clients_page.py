# -*- coding: utf-8 -*-
__author__ = 'yuelyasheva'

import datetime

import json
import pytest
import balance.balance_db as db
from hamcrest import equal_to
from decimal import Decimal
from dateutil.relativedelta import relativedelta
from btestlib.data.defaults import Date

import btestlib.reporter as reporter
import btestlib.utils as utils
from balance import balance_steps as steps
from balance import balance_web as web
from balance.features import Features
from btestlib.constants import Services
from temp.igogor.balance_objects import Contexts, Products, Firms, Paysyses, PersonTypes, ContractCommissionType, \
    Currencies, Collateral, Users

MAIN_DT = datetime.datetime.now()

DIRECT_CONTEXT_FIRM_1 = Contexts.DIRECT_FISH_RUB_CONTEXT.new(firm=Firms.YANDEX_1,
                                                             contract_type=ContractCommissionType.OPT_AGENCY)
DIRECT_CONTEXT_FIRM_4 = Contexts.DIRECT_FISH_RUB_CONTEXT.new(firm=Firms.YANDEX_INC_4,
                                                             person_type=PersonTypes.USU,
                                                             paysys=Paysyses.BANK_US_UR_USD,
                                                             contract_type=ContractCommissionType.USA_OPT_CLIENT)
DIRECT_CONTEXT_FIRM_7 = Contexts.DIRECT_FISH_RUB_CONTEXT.new(firm=Firms.EUROPE_AG_7,
                                                             person_type=PersonTypes.SW_UR,
                                                             paysys=Paysyses.BANK_SW_UR_CHF,
                                                             contract_type=ContractCommissionType.SW_OPT_CLIENT)

TODAY = utils.Date.nullify_time_of_date(datetime.datetime.today())
CONTRACT_START_DT = utils.Date.date_to_iso_format(TODAY - relativedelta(months=4))

CREDIT_LIMIT_RUB = Decimal('5700')
CREDIT_LIMIT_CHF_USD = Decimal('78')
QTY = Decimal('50')


SERVICES = [Services.MEDIA_BANNERS.id, Services.GEO.id, Services.MEDIA_70.id,
            Services.MEDIA_BANNERS_167.id, Services.DIRECT.id, Services.BAYAN.id,
            Services.BANKI.id, Services.SPAMDEF.id, Services.TECHNOLOGIES.id]


def test_individual_credit(get_free_user):
    context = DIRECT_CONTEXT_FIRM_1

    # создаем агентство и оптовый агентский договор с кредитом по сроку и сумме
    agency_id = steps.ClientSteps.create({'IS_AGENCY': 1})
    user = Users.TMP_TEST_USER
    steps.ClientSteps.link(agency_id, user.login)
    person_id = steps.PersonSteps.create(agency_id, context.person_type.code)

    contract_params = {'CLIENT_ID': agency_id,
                       'PERSON_ID': person_id,
                       'DT': CONTRACT_START_DT,
                       'IS_SIGNED': CONTRACT_START_DT,
                       'PAYMENT_TYPE': 3,
                       'PAYMENT_TERM': 100,
                       'CREDIT_TYPE': 2,
                       'CREDIT_LIMIT_SINGLE': CREDIT_LIMIT_RUB,
                       'SERVICES': SERVICES,
                       'PERSONAL_ACCOUNT': 0,
                       'LIFT_CREDIT_ON_PAYMENT': 0,
                       'PERSONAL_ACCOUNT_FICTIVE': 0,
                       'CURRENCY': str(Currencies.RUB.num_code),
                       'FIRM': context.firm.id,
                       }

    contract_id, contract_eid = steps.ContractSteps.create_contract_new(context.contract_type, contract_params)

    # создаем клиента и допник на индивидуальный кредитный лимит
    client_id = steps.ClientSteps.create()
    credit_limits = [{"id": "1",
                      "num": "{0}".format(client_id),
                      "client": "{0}".format(client_id),
                      "client_limit": "1000",
                      "client_payment_term": "45",
                      "client_credit_type": "1000",
                      "client_limit_currency": "RUR"}]

    collateral_params = {'CONTRACT2_ID': contract_id,
                         'CLIENT_LIMITS': str(json.dumps(credit_limits)),
                         'DT': utils.Date.date_to_iso_format(TODAY),
                         'IS_SIGNED': utils.Date.date_to_iso_format(TODAY)}
    steps.ContractSteps.create_collateral(Collateral.SUBCLIENT_CREDIT_LIMIT, collateral_params)

    with web.Driver() as driver:
        client_credit_page = web.AdminInterface.ClientCreditPage.open_and_wait(driver, client_id=agency_id)
        utils.check_that(client_credit_page.is_credit_table_present(), equal_to(True),
                         u'Проверяем, что на странице кредитов есть таблица с кредитами')
        utils.check_that(client_credit_page.is_individual_credit_table_present(), equal_to(True),
                         u'Проверяем, что на странице кредитов есть таблица с индивидуальными кредитами')