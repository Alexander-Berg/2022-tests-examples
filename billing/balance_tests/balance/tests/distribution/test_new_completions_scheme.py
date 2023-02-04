# -*- coding: utf-8 -*-
__author__ = 'vorobyov-as'

import pytest
from dateutil.relativedelta import relativedelta

import btestlib.reporter as reporter
from balance import balance_steps as steps, balance_api as api
from balance.distribution.distribution_types import DistributionType
from balance.features import Features
from btestlib import utils
from btestlib.constants import DistributionContractType, Firms, Currencies
from btestlib.matchers import contains_dicts_with_entries
from btestlib import shared
from btestlib.data import defaults

pytestmark = [
    pytest.mark.slow,
    reporter.feature(Features.DISTRIBUTION, Features.PARTNER_REWARD, Features.PARTNER_ACT)
]

CONTRACT_TYPES = [DistributionContractType.AGILE, DistributionContractType.UNIVERSAL]
START_DT = utils.Date.first_day_of_month() - relativedelta(months=1)
PASSPORT_ID = defaults.PASSPORT_UID


@reporter.feature(Features.DISTRIBUTION, Features.PARTNER_REWARD, Features.PARTNER_ACT)
@pytest.mark.parametrize('contract_type, firm, exclude_revshare_type', [
    (DistributionContractType.OFFER, Firms.YANDEX_1, DistributionType.DIRECT),
    #(DistributionContractType.AGILE, Firms.MARKET_111, DistributionType.VIDEO_HOSTING),
    #pytest.mark.smoke((DistributionContractType.UNIVERSAL, Firms.YANDEX_1, DistributionType.VIDEO_HOSTING)),
], ids=lambda ct, f, _: "{}_{}".format(DistributionContractType.name(ct), Firms.name(f)))
def test_full_contract_partner_act_data(contract_type, firm, exclude_revshare_type):

    client_id, person_id, tag_id = steps.DistributionSteps.create_distr_client_person_tag()
    # создаем договор дистрибуции
    contract_id, external_id = steps.DistributionSteps.create_full_contract(
        contract_type, client_id, person_id, tag_id, START_DT, START_DT, firm=firm,
        exclude_revshare_type=exclude_revshare_type
    )
    # создаем площадки
    places_ids = steps.DistributionSteps.create_fixed_and_revshare_places(client_id, tag_id, exclude_revshare_type)

    # добавляем открутки
    #steps.DistributionSteps.create_completions(places_ids, START_DT)
    steps.DistributionSteps.create_entity_completions(places_ids, START_DT)
    steps.DistributionSteps.run_calculator_for_contract(contract_id)

    # запускаем генерацию актов
    api.test_balance().GeneratePartnerAct(contract_id, START_DT)

    expected_partner_act_data = steps.DistributionData.create_expected_full_partner_act_data(contract_id, client_id,
                                                                                             tag_id, places_ids,
                                                                                             START_DT)

    partner_act_data = steps.CommonPartnerSteps.get_partner_act_data_by_contract_id(contract_id)

    utils.check_that(partner_act_data, contains_dicts_with_entries(expected_partner_act_data),
                     u"Проверяем, что партнерские акты имеют ожидаемые параметры")
