# coding: utf-8
__author__ = 'a-vasin'

import pytest
from dateutil.relativedelta import relativedelta

import btestlib.reporter as reporter
from balance import balance_steps as steps, balance_api as api
from balance.distribution.distribution_types import DistributionType
from balance.features import Features
from btestlib import shared
from btestlib import utils
from btestlib.constants import DistributionContractType, Currencies, NdsNew as Nds, Firms
from btestlib.matchers import contains_dicts_with_entries

pytestmark = [
    pytest.mark.slow,
    reporter.feature(Features.DISTRIBUTION, Features.PARTNER_REWARD, Features.PARTNER_ACT)
]

# CONTRACT_TYPES = [
#     (DistributionContractType.AGILE, DistributionType.VIDEO_HOSTING),
#     (DistributionContractType.UNIVERSAL, DistributionType.DIRECT),
#     (DistributionContractType.OFFER, DistributionType.DIRECT)
# ]

START_DT = utils.Date.first_day_of_month() - relativedelta(months=1)


@pytest.mark.parametrize('contract_type, contract_currency, product_currency, firm, person_type, exclude_revshare_type',
                         [
                             # Yandex
                             (DistributionContractType.UNIVERSAL, Currencies.USD, Currencies.USD, Firms.YANDEX_1, 'yt', DistributionType.VIDEO_HOSTING),
                             (DistributionContractType.AGILE, Currencies.TRY, Currencies.EUR, Firms.YANDEX_1, 'yt', DistributionType.VIDEO_HOSTING),
                             (DistributionContractType.OFFER, Currencies.USD, Currencies.TRY, Firms.YANDEX_1, 'yt', DistributionType.DIRECT),
                             (DistributionContractType.UNIVERSAL, Currencies.USD, Currencies.UAH, Firms.YANDEX_1, 'yt', DistributionType.VIDEO_HOSTING),

                             # Europe_AG
                             (DistributionContractType.AGILE, Currencies.TRY, Currencies.EUR, Firms.EUROPE_AG_7, 'sw_yt', DistributionType.VIDEO_HOSTING),
                             (DistributionContractType.UNIVERSAL, Currencies.USD, Currencies.TRY, Firms.EUROPE_AG_7, 'sw_yt', DistributionType.VIDEO_HOSTING),

                             # Service_AG
                             (DistributionContractType.AGILE, Currencies.TRY, Currencies.EUR, Firms.SERVICES_AG_16, 'sw_yt', DistributionType.VIDEO_HOSTING),
                             (DistributionContractType.UNIVERSAL, Currencies.USD, Currencies.TRY, Firms.SERVICES_AG_16, 'sw_yt', DistributionType.VIDEO_HOSTING),

                             # Market
                             (DistributionContractType.UNIVERSAL, Currencies.RUB, Currencies.EUR, Firms.MARKET_111, 'yt', DistributionType.VIDEO_HOSTING)
                         ],
                         ids=lambda contract_type, contract_currency, product_currency, firm, person_type, exclude_type: '{}-{}-firm{}'.format(
                             contract_currency.iso_code, product_currency.iso_code, firm.id))
def test_currency(contract_type, contract_currency, product_currency,
                  firm, person_type, exclude_revshare_type):

    # создаем клиента, плательщика и тэг
    client_id, person_id, tag_id = steps.DistributionSteps.create_distr_client_person_tag(person_type=person_type)

    # создаем договор дистрибуции
    contract_id, external_id = steps.DistributionSteps.create_full_contract(contract_type, client_id, person_id,
                                                                            tag_id,
                                                                            START_DT, START_DT,
                                                                            nds=Nds.NOT_RESIDENT,
                                                                            firm=firm,
                                                                            contract_currency=contract_currency,
                                                                            product_currency=product_currency,
                                                                            exclude_revshare_type=exclude_revshare_type)

    # создаем площадки
    places_ids = steps.DistributionSteps.create_fixed_and_revshare_places(client_id, tag_id, exclude_revshare_type)

    # добавляем открутки
    steps.DistributionSteps.create_entity_completions(places_ids, START_DT, currency=contract_currency)
    steps.DistributionSteps.run_calculator_for_contract(contract_id)

    # запускаем генерацию актов
    api.test_balance().GeneratePartnerAct(contract_id, START_DT)

    partner_act_data = steps.CommonPartnerSteps.get_partner_act_data_by_contract_id(contract_id)

    product_exchange_rate = steps.CommonSteps.get_latest_exchange_rate(product_currency, contract_currency, START_DT)
    rub_exchange_rate = steps.CommonSteps.get_latest_exchange_rate(Currencies.RUB, contract_currency, START_DT)
    expected_partner_act_data = steps.DistributionData.create_expected_full_partner_act_data(contract_id, client_id,
                                                                                             tag_id, places_ids,
                                                                                             START_DT,
                                                                                             currency=contract_currency,
                                                                                             product_exchange_rate=product_exchange_rate,
                                                                                             rub_exchange_rate=rub_exchange_rate,
                                                                                             nds=Nds.NOT_RESIDENT)

    # expected_partner_act_data = steps.DistributionSteps.normalize_old_partner_act_data(expected_partner_act_data)

    utils.check_that(partner_act_data, contains_dicts_with_entries(expected_partner_act_data),
                     u"Проверяем, что партнерские акты имеют ожидаемые параметры")
