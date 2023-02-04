# coding: utf-8
__author__ = 'vorobyov-as'


import pytest
from datetime import datetime

from balance.features import Features
from balance import balance_steps as steps, balance_api as api
from balance.distribution.distribution_types import DistributionType

from dateutil.relativedelta import relativedelta

from btestlib.constants import Currencies, DistributionContractType, Firms, Managers, NdsNew as Nds, PersonTypes
from btestlib.matchers import contains_dicts_with_entries
import btestlib.reporter as reporter
from btestlib import shared, utils


pytestmark = [
    pytest.mark.slow,
    reporter.feature(Features.DISTRIBUTION, Features.PARTNER_REWARD, Features.PARTNER_ACT)
]


START_DT = utils.Date.first_day_of_month() - relativedelta(months=1)


CONTRACT_PARAMS = dict(
        ctype='DISTRIBUTION',
        distribution_contract_type=DistributionContractType.UNIVERSAL,
        manager_uid=Managers.VECHER.uid,
        start_dt=START_DT,
        service_start_dt=START_DT,
        products_revshare={'13002': DistributionType.TAXI_LUCKY_RIDE.default_price},
        supplements=[1],
        product_search='qwerty',
        signed=True,
    )


@pytest.mark.parametrize('firm, currency, person_type, nds',
                         [
                             (Firms.TAXI_13, Currencies.RUB, PersonTypes.UR, Nds.DEFAULT),
                             (Firms.TAXI_BV_22, Currencies.USD, PersonTypes.EU_YT, Nds.ZERO),
                             (Firms.TAXI_BV_22, Currencies.EUR, PersonTypes.EU_YT, Nds.ZERO),
                         ], ids=['Revshare Taxi Russia, RUB',
                                 'Revshare Taxi BV, USD',
                                 'Revshare Taxi BV, EUR']
                         )
def test_taxi_rs(firm, currency, person_type, nds):

    contract_params = dict(CONTRACT_PARAMS,
                           currency=currency.iso_code,
                           products_currency=currency.iso_code)

    client_id, person_id, tag_id = (steps.DistributionSteps
                                    .create_distr_client_person_tag(person_type=person_type.code))

    place_id, _ = (steps.DistributionSteps
                   .create_distr_place(
                        client_id, tag_id, {DistributionType.TAXI_LUCKY_RIDE.page_id})
                   )

    places_ids = {DistributionType.TAXI_LUCKY_RIDE: place_id}

    contract_params = dict(contract_params,
                           firm_id=firm.id, client_id=client_id, person_id=person_id,
                           distribution_tag=tag_id, nds=nds.nds_id)

    contract_id, _ = (steps.contract_steps.ContractSteps
                      .create_common_contract(contract_params))

    # добавляем открутки
    steps.DistributionSteps.create_entity_completions(places_ids, START_DT)
    steps.DistributionSteps.run_calculator_for_contract(contract_id)

    api.test_balance().GeneratePartnerAct(contract_id, START_DT)

    check_act(client_id, contract_id, place_id, tag_id, currency, nds)


def rub_exchange_rate(currency):
    return steps.CommonSteps.get_latest_exchange_rate(Currencies.RUB, currency, START_DT)


def check_act(client_id, contract_id, place_id, tag_id, currency, nds):
    partner_act_data = steps.CommonPartnerSteps.get_partner_act_data_by_contract_id(contract_id)

    expected_partner_act_data = [
        steps.DistributionData.create_revshare_data_row(
            DistributionType.TAXI_LUCKY_RIDE, contract_id, client_id, tag_id,
            place_id, START_DT, currency=currency, nds=nds,
            exchange_rate=rub_exchange_rate(currency))]

    utils.check_that(partner_act_data, contains_dicts_with_entries(expected_partner_act_data),
                     u"Проверяем, что партнерские акты имеют ожидаемые параметры")