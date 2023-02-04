# -*- coding: utf-8 -*-
__author__ = 'sandyk'

import datetime

import pytest
from hamcrest import has_entries

import btestlib.reporter as reporter
from balance import balance_steps as steps
from balance.features import Features
from btestlib import utils
from btestlib.constants import Firms

MAIN_DT = datetime.datetime.now()
MIGRATION_DT = MAIN_DT - datetime.timedelta(days=1)

KZT_FIRM_ID = Firms.KZ_25.id
BEL_FIRM_ID = Firms.REKLAMA_BEL_27.id
OOO_FIRM_ID = Firms.YANDEX_1.id

DIRECT_SERVICE_ID = 7

PERSON_PARAMS = {'kzu': {'region_id': 159},
                 'byu': {'region_id': 149},
                 'ur': {'region_id': 225}}


@reporter.feature(Features.OVERDRAFT)
@pytest.mark.tickets('BALANCE-26203')
@pytest.mark.parametrize("firm, person_type, currency, limit, unit",
                         [
                             (1, 'ur', 'RUB', 10000, 'Money'),
                             (1, 'ur', None, 340, 'Bucks'),
                             (25, 'kzu', 'KZT', 50000, 'Money'),
                             (25, 'kzu', None, 430, 'Bucks'),
                             (27, 'byu', 'BYN', 270, 'Money')
                         ])
def test_fair_overdraft_mv_client(firm, person_type, currency, limit, unit):
    client_id = steps.ClientSteps.create()
    if unit == 'Money':
        steps.ClientSteps.migrate_to_currency(client_id=client_id, currency_convert_type='COPY', dt=MIGRATION_DT,
                                              service_id=DIRECT_SERVICE_ID,
                                              region_id=PERSON_PARAMS[person_type]['region_id'], currency=currency)
        steps.CommonSteps.export('MIGRATE_TO_CURRENCY', 'Client', client_id)
    steps.ClientSteps.set_overdraft(client_id, DIRECT_SERVICE_ID, limit, firm_id=firm, start_dt=MAIN_DT,
                                    currency=currency, invoice_currency=currency)

    steps.CommonSteps.export('OVERDRAFT', 'Client', client_id)

    given_limit = steps.OverdraftSteps.get_limit_by_client(client_id)
    utils.check_that(given_limit[0], has_entries(
        {'currency': currency, 'firm_id': firm, 'overdraft_limit': limit, 'client_id': client_id,
         'service_id': DIRECT_SERVICE_ID, 'iso_currency': currency}))
