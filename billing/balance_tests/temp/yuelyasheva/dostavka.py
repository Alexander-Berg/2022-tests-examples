# -*- coding: utf-8 -*-
__author__ = 'yuelyasheva'

import datetime

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
    Currencies,PaymentMethods


MAIN_DT = datetime.datetime.now()
TODAY = utils.Date.nullify_time_of_date(datetime.datetime.today())
CONTRACT_START_DT = utils.Date.date_to_iso_format(TODAY - relativedelta(months=4))
CONTRACT_END_DT = utils.Date.date_to_iso_format(TODAY + relativedelta(months=3))

client_id, service_product_id = steps.SimpleApi.create_partner_and_product(Services.DOSTAVKA)
person_id = steps.PersonSteps.create(client_id, 'ur')

contract_params = {'CLIENT_ID': client_id,
                   'PERSON_ID': person_id,
                   'DT': CONTRACT_START_DT,
                   'FINISH_DT': CONTRACT_END_DT,
                   'IS_SIGNED': CONTRACT_START_DT,
                   'PAYMENT_TYPE': 3,
                   'PAYMENT_TERM': 15,
                   'SERVICES': [101, 120],
                   'PERSONAL_ACCOUNT': 0,
                   'LIFT_CREDIT_ON_PAYMENT': 0,
                   'PERSONAL_ACCOUNT_FICTIVE': 0,
                   'CURRENCY': 810,
                   'FIRM': Firms.MARKET_111.id,
                   'CALC_DEFERMANT': 0,
                   'COMMISSION': 0,
                   'CREDIT_TYPE': 1,
                   'MINIMAL_PAYMENT_COMMISSION': 30,
                   'PARTNER_CREDIT': 1
                   }

contract_id, contract_eid = steps.ContractSteps.create_contract_new(ContractCommissionType.NO_AGENCY, contract_params)

service_order_id, trust_payment_id, purchase_token, payment_id = \
        steps.SimpleApi.create_trust_payment(Services.DOSTAVKA, service_product_id,
                                             commission_category=Decimal('100'),
                                             paymethod=PaymentMethods.CASH)