# -*- coding: utf-8 -*-
__author__ = 'a-vasin'

from decimal import Decimal

import pytest
from dateutil.relativedelta import relativedelta

from balance import balance_db as db, balance_api as api
import btestlib.reporter as reporter
from balance import balance_steps as steps
from balance.distribution.distribution_types import DistributionType, DistributionSubtype
from balance.features import Features
from btestlib import shared
from btestlib import utils
from btestlib.constants import DistributionContractType
from btestlib.data import defaults
from btestlib.matchers import contains_dicts_with_entries

pytestmark = [
    pytest.mark.slow,
    reporter.feature(Features.DISTRIBUTION, Features.PARTNER_REWARD, Features.PARTNER_ACT)
]

START_DT = utils.Date.first_day_of_month() - relativedelta(months=1)


@pytest.mark.parametrize('contract_type, exclude_revshare_type', [
    (DistributionContractType.AGILE, DistributionType.DIRECT),
    (DistributionContractType.UNIVERSAL, DistributionType.VIDEO_HOSTING),],
                         ids=lambda ct, _: DistributionContractType.name(ct))
def test_net_reward_type_fixed_discount(contract_type, exclude_revshare_type):
    revshare_types = [distribution_type for distribution_type in DistributionType
                      if distribution_type.subtype == DistributionSubtype.REVSHARE
                      and distribution_type != exclude_revshare_type]

    fixed_discount = Decimal('5.50')
    revshare_discounts = {revshare_type: fixed_discount for revshare_type in revshare_types}

    client_id, contract_id, tag_id = create_client_and_contract(contract_type, exclude_revshare_type)
    set_avg_discount_pct(contract_id, fixed_discount)

    # создаем площадки
    places_ids, _ = steps.DistributionSteps.create_places(client_id, tag_id, revshare_types)

    check_contract_net_reward(client_id, contract_id, revshare_discounts, tag_id, places_ids)


@pytest.mark.tickets('BALANCE-24553')
@pytest.mark.parametrize('contract_type, exclude_revshare_type',
                         [(DistributionContractType.UNIVERSAL, DistributionType.VIDEO_HOSTING),
                          (DistributionContractType.OFFER, DistributionType.VIDEO_HOSTING)],
                         ids=lambda ct, _: DistributionContractType.name(ct))
def test_net_reward_type_agile_discount(contract_type, exclude_revshare_type):
    revshare_types = [distribution_type for distribution_type in DistributionType
                      if distribution_type.subtype == DistributionSubtype.REVSHARE
                      and distribution_type != exclude_revshare_type]

    revshare_discounts = {revshare_type: steps.DistributionSteps.get_discount_pct(revshare_type, START_DT)
                          for revshare_type in revshare_types}

    client_id, contract_id, tag_id = create_client_and_contract(contract_type, exclude_revshare_type)

    # создаем площадки
    places_ids, _ = steps.DistributionSteps.create_places(client_id, tag_id, revshare_types)

    check_contract_net_reward(client_id, contract_id, revshare_discounts, tag_id, places_ids)


def create_client_and_contract(contract_type, exclude_revshare_type):
    # создаем клиента, плательщика и тэг
    client_id, person_id, tag_id = steps.DistributionSteps.create_distr_client_person_tag()

    # создаем договор дистрибуции
    contract_id, external_id = steps.DistributionSteps.create_full_contract(contract_type, client_id, person_id, tag_id,
                                                                            START_DT, START_DT, reward_type='2',
                                                                            exclude_revshare_type=exclude_revshare_type)

    return client_id, contract_id, tag_id


def check_contract_net_reward(client_id, contract_id, revshare_discounts, tag_id, places_ids):
    # добавляем открутки
    steps.DistributionSteps.create_entity_completions(places_ids, START_DT)
    steps.DistributionSteps.run_calculator_for_contract(contract_id)

    # запускаем генерацию актов
    api.test_balance().GeneratePartnerAct(contract_id, START_DT)

    expected_partner_act_data = steps.DistributionData.create_expected_full_partner_act_data(contract_id, client_id,
                                                                                             tag_id, places_ids,
                                                                                             START_DT,
                                                                                             revshare_discount=revshare_discounts)

    partner_act_data = steps.CommonPartnerSteps.get_partner_act_data_by_contract_id(contract_id)

    utils.check_that(partner_act_data, contains_dicts_with_entries(expected_partner_act_data),
                     u"Проверяем, что партнерские акты имеют ожидаемые параметры")


def set_avg_discount_pct(contract_id, percent, passport_id=defaults.PASSPORT_UID):
    with reporter.step(u"Устанавливаем фиксированный процент скидки: {} для договора: {}"
                               .format(percent, contract_id)):
        attribute_batch_id = db.get_collaterals_by_contract(contract_id)[0]['attribute_batch_id']

        query = 'INSERT INTO t_contract_attributes ' \
                '(ID,DT,CODE,KEY_NUM,VALUE_STR,VALUE_NUM,VALUE_DT,UPDATE_DT,PASSPORT_ID, ATTRIBUTE_BATCH_ID, RELATED_OBJECT_TABLE) ' \
                'VALUES (S_CONTRACT_ATTRIBUTES_ID.NEXTVAL,' \
                'NULL,\'AVG_DISCOUNT_PCT\',NULL,NULL,:percent,NULL,SYSDATE,:passport_id, :attribute_batch_id, \'T_CONTRACT_COLLATERAL\')'
        params = {
            'contract_id': contract_id,
            'percent': percent,
            'passport_id': passport_id,
            'attribute_batch_id': attribute_batch_id
        }

        db.balance().execute(query, params)
        steps.ContractSteps.refresh_contracts_cache(contract_id)
