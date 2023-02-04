# -*- coding: utf-8 -*-
__author__ = 'a-vasin'

import pytest
from dateutil.relativedelta import relativedelta
from hamcrest import empty

import btestlib.reporter as reporter
from balance import balance_steps as steps, balance_api as api
from balance.distribution.distribution_types import DistributionType, DistributionSubtype
from balance.features import Features
from btestlib import shared
from btestlib import utils

pytestmark = [
    pytest.mark.slow,
    reporter.feature(Features.DISTRIBUTION, Features.PARTNER_REWARD, Features.PARTNER_ACT)
]

START_DT = utils.Date.first_day_of_month() - relativedelta(months=1)

def test_universal_contract_test_mode(shared_data):
    exclude_revshare_type = DistributionType.VIDEO_HOSTING

    # создаем клиента, плательщика и тэг
    client_id, person_id, tag_id = steps.DistributionSteps.create_distr_client_person_tag()

    # создаем договор дистрибуции
    products_revshare = [(str(distr_type.contract_price_id),
                          str(distr_type.default_price) if distr_type != exclude_revshare_type else '')
                         for distr_type in DistributionType if distr_type.subtype == DistributionSubtype.REVSHARE]
    contract_id, external_id = steps.ContractSteps.create_contract('universal_distr_test_mode',
                                                                   {'CLIENT_ID': client_id, 'PERSON_ID': person_id,
                                                                    'DT': START_DT,
                                                                    'DISTRIBUTION_TAG': tag_id,
                                                                    'SERVICE_START_DT': START_DT,
                                                                    'PRODUCTS_REVSHARE': products_revshare,
                                                                    'INSTALL_PRICE': DistributionType.INSTALLS.default_price,
                                                                    'SEARCH_PRICE': DistributionType.SEARCHES.default_price,
                                                                    'ACTIVATION_PRICE': DistributionType.ACTIVATIONS.default_price
                                                                    })

    # создаем площадки
    places_ids = steps.DistributionSteps.create_fixed_and_revshare_places(client_id, tag_id, exclude_revshare_type)

    # добавляем открутки
    steps.DistributionSteps.create_entity_completions(places_ids, START_DT)
    steps.DistributionSteps.run_calculator_for_contract(contract_id)

    # запускаем генерацию актов
    api.test_balance().GeneratePartnerAct(contract_id, START_DT)

    partner_act_data = steps.CommonPartnerSteps.get_partner_act_data_by_contract_id(contract_id)

    utils.check_that(partner_act_data, empty(), u"Проверяем, что партнерские акты не созданы")
