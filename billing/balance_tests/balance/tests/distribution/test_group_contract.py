# coding: utf-8
__author__ = 'a-vasin'

import xmlrpclib

import pytest
from dateutil.relativedelta import relativedelta
from hamcrest import empty, has_length, contains_string, equal_to

import btestlib.reporter as reporter
from balance import balance_steps as steps, balance_api as api
from balance.distribution.distribution_types import DistributionType
from balance.features import Features
from btestlib import shared
from btestlib import utils
from btestlib import constants
from btestlib.constants import DistributionContractType, NdsNew as Nds, Firms
from btestlib.matchers import contains_dicts_with_entries

pytestmark = [
    pytest.mark.slow,
    reporter.feature(Features.DISTRIBUTION, Features.PARTNER_REWARD, Features.PARTNER_ACT)
]

GROUP_START_DT = utils.Date.first_day_of_month() - relativedelta(months=4)

FIRST_SERVICE_START_DT = GROUP_START_DT
FIRST_START_DT = FIRST_SERVICE_START_DT + relativedelta(months=1)
FIRST_END_DT = FIRST_SERVICE_START_DT + relativedelta(months=2, days=-1)

# универсальный договор имеет смещение всех дат на месяц в будущее
SECOND_SERVICE_START_DT = FIRST_SERVICE_START_DT + relativedelta(months=1)
SECOND_START_DT = SECOND_SERVICE_START_DT + relativedelta(months=1)
SECOND_END_DT = SECOND_SERVICE_START_DT + relativedelta(months=2, days=-1)

TAIL_TIME = 1


@pytest.mark.parametrize('firm, person_type, is_offer',
                         [
                             (Firms.YANDEX_1, 'ur', True),
                             # (Firms.YANDEX_1, 'yt', False),
                             # (Firms.SERVICES_AG_16, 'sw_yt', False),
                             (Firms.EUROPE_AG_7, 'sw_yt', False),
                             # (Firms.MARKET_111, 'ur', False)
                         ], ids=lambda f, _, io: "{}_{}".format(Firms.name(f), 'OFFER' if io else 'GENERAL'))
def test_not_generated_acts_group_contract(firm, person_type, is_offer):

    # создаем клиента, плательщика и два тэга
    client_id, person_id, first_tag_id = steps.DistributionSteps.create_distr_client_person_tag(
        person_type=person_type)
    second_tag_id = steps.DistributionSteps.create_distr_tag(client_id)

    group_contract_id, first_contract_id, second_contract_id = \
        create_group_and_children_contracts(client_id, person_id, first_tag_id, second_tag_id, is_offer, firm)

    # создаем площадки
    first_places_ids = steps.DistributionSteps.create_fixed_and_revshare_places(client_id, first_tag_id,
                                                                                DistributionType.VIDEO_HOSTING)
    second_places_ids = steps.DistributionSteps.create_fixed_and_revshare_places(client_id, second_tag_id,
                                                                                 DistributionType.DIRECT)

    # добавляем открутки
    steps.DistributionSteps.create_entity_completions(first_places_ids, SECOND_START_DT)
    steps.DistributionSteps.create_entity_completions(second_places_ids, SECOND_START_DT)
    steps.DistributionSteps.run_calculator_for_contract(first_contract_id)
    steps.DistributionSteps.run_calculator_for_contract(second_contract_id)
    steps.DistributionSteps.run_calculator_for_contract(group_contract_id)

    # запускаем генерацию актов
    api.test_balance().GeneratePartnerAct(group_contract_id, SECOND_START_DT)

    partner_act_data = steps.CommonPartnerSteps.get_partner_act_data_by_contract_id(group_contract_id)

    utils.check_that(partner_act_data, empty(), u"Проверяем, что по груповому договору не генерируются акты")


@pytest.mark.parametrize("is_offer", [True, False], ids=lambda io: 'OFFER' if io else 'GENERAL')
def test_head_act_date_agile_contract(is_offer):

    # создаем клиента, плательщика и два тэга
    client_id, person_id, first_tag_id = steps.DistributionSteps.create_distr_client_person_tag()
    second_tag_id = steps.DistributionSteps.create_distr_tag(client_id)

    group_contract_id, first_contract_id, second_contract_id = \
        create_group_and_children_contracts(client_id, person_id, first_tag_id, second_tag_id, is_offer)

    # создаем площадки
    first_places_ids = steps.DistributionSteps.create_fixed_and_revshare_places(client_id, first_tag_id,
                                                                                DistributionType.VIDEO_HOSTING)
    second_places_ids = steps.DistributionSteps.create_fixed_and_revshare_places(client_id, second_tag_id,
                                                                                 DistributionType.DIRECT)

    # добавляем открутки
    steps.DistributionSteps.create_entity_completions(first_places_ids, FIRST_SERVICE_START_DT)
    steps.DistributionSteps.run_calculator_for_contract(first_contract_id)
    steps.DistributionSteps.create_entity_completions(second_places_ids, SECOND_SERVICE_START_DT)
    steps.DistributionSteps.run_calculator_for_contract(second_contract_id)

    api.test_balance().GeneratePartnerAct(first_contract_id, FIRST_START_DT)
    api.test_balance().GeneratePartnerAct(second_contract_id, FIRST_START_DT)

    partner_act_data = steps.CommonPartnerSteps.get_partner_act_data_by_contract_id(group_contract_id)
    expected_partner_act_data = steps.DistributionData.create_expected_full_partner_act_data(group_contract_id,
                                                                                             client_id,
                                                                                             first_tag_id,
                                                                                             first_places_ids,
                                                                                             FIRST_START_DT,
                                                                                             nds_dt=FIRST_SERVICE_START_DT)

    utils.check_that(partner_act_data, contains_dicts_with_entries(expected_partner_act_data),
                     u"Проверяем, что по гибкому договору сгенерированы акты за голову")
    utils.check_that(partner_act_data, has_length(len(expected_partner_act_data)),
                     u"Проверяем, что по универсальному договору актов нет")

    # запускаем генерацию актов
    api.test_balance().GeneratePartnerAct(first_contract_id, SECOND_START_DT)
    api.test_balance().GeneratePartnerAct(second_contract_id, SECOND_START_DT)

    partner_act_data = steps.CommonPartnerSteps.get_partner_act_data_by_contract_id(group_contract_id)
    expected_partner_act_data += steps.DistributionData.create_expected_full_partner_act_data(group_contract_id,
                                                                                              client_id,
                                                                                              second_tag_id,
                                                                                              second_places_ids,
                                                                                              SECOND_START_DT,
                                                                                              nds_dt=SECOND_SERVICE_START_DT)

    utils.check_that(partner_act_data, contains_dicts_with_entries(expected_partner_act_data),
                     u"Проверяем, что по гибкому и универсальному договорам сгенерированы акты за голову")
    utils.check_that(partner_act_data, has_length(len(expected_partner_act_data)),
                     u"Проверяем, что по универсальному и гибкому договорам акты сгенерированы только один раз")


@pytest.mark.parametrize('firm, person_type, nds, is_offer',
                         [
                             (Firms.YANDEX_1, 'ur', Nds.DEFAULT, False),
                             (Firms.SERVICES_AG_16, 'sw_yt', Nds.NOT_RESIDENT, True),
                             # (Firms.EUROPE_AG_7, 'sw_yt', Nds.NOT_RESIDENT, False),
                             (Firms.MARKET_111, 'ur', Nds.DEFAULT, True)
                         ], ids=lambda f, _, __, io: "{}_{}".format(Firms.name(f), 'OFFER' if io else 'GENERAL'))
def test_regular_act_date_both_contracts(firm, person_type, nds, is_offer):

    # создаем клиента, плательщика и два тэга
    client_id, person_id, first_tag_id = steps.DistributionSteps.create_distr_client_person_tag(
        person_type=person_type)
    second_tag_id = steps.DistributionSteps.create_distr_tag(client_id)

    group_contract_id, first_contract_id, second_contract_id = \
        create_group_and_children_contracts(client_id, person_id, first_tag_id, second_tag_id, is_offer, firm)

    # создаем площадки
    first_places_ids = steps.DistributionSteps.create_fixed_and_revshare_places(client_id, first_tag_id,
                                                                                DistributionType.VIDEO_HOSTING)
    second_places_ids = steps.DistributionSteps.create_fixed_and_revshare_places(client_id, second_tag_id,
                                                                                 DistributionType.DIRECT)

    # добавляем открутки
    steps.DistributionSteps.create_entity_completions(first_places_ids, FIRST_START_DT)
    steps.DistributionSteps.create_entity_completions(second_places_ids, SECOND_START_DT)
    steps.DistributionSteps.run_calculator_for_contract(first_contract_id)
    steps.DistributionSteps.run_calculator_for_contract(second_contract_id)

    # запускаем генерацию актов
    api.test_balance().GeneratePartnerAct(first_contract_id, SECOND_START_DT)
    api.test_balance().GeneratePartnerAct(second_contract_id, SECOND_START_DT)

    partner_act_data = steps.CommonPartnerSteps.get_partner_act_data_by_contract_id(group_contract_id)
    expected_partner_act_data = steps.DistributionData.create_expected_full_partner_act_data(group_contract_id,
                                                                                             client_id,
                                                                                             first_tag_id,
                                                                                             first_places_ids,
                                                                                             SECOND_START_DT,
                                                                                             nds=nds, nds_dt=FIRST_START_DT)
    expected_partner_act_data += steps.DistributionData.create_expected_full_partner_act_data(group_contract_id,
                                                                                              client_id,
                                                                                              second_tag_id,
                                                                                              second_places_ids,
                                                                                              SECOND_START_DT,
                                                                                              nds=nds)

    utils.check_that(partner_act_data, contains_dicts_with_entries(expected_partner_act_data),
                     u"Проверяем, что по сгенерированы акты по гибкому и универсальному договорам")


@pytest.mark.parametrize("is_offer", [True, False], ids=lambda io: 'OFFER' if io else 'GENERAL')
def test_tail_act_date_universal_contract(is_offer):

    # создаем клиента, плательщика и два тэга
    client_id, person_id, first_tag_id = steps.DistributionSteps.create_distr_client_person_tag()
    second_tag_id = steps.DistributionSteps.create_distr_tag(client_id)

    group_contract_id, first_contract_id, second_contract_id = \
        create_group_and_children_contracts(client_id, person_id, first_tag_id, second_tag_id, is_offer)

    # создаем площадки
    first_places_ids = steps.DistributionSteps.create_fixed_and_revshare_places(client_id, first_tag_id,
                                                                                DistributionType.VIDEO_HOSTING)
    second_places_ids = steps.DistributionSteps.create_fixed_and_revshare_places(client_id, second_tag_id,
                                                                                 DistributionType.DIRECT)

    act_date = utils.Date.first_day_of_month(SECOND_END_DT + relativedelta(months=1))

    # добавляем открутки
    steps.DistributionSteps.create_entity_completions(first_places_ids, act_date)
    steps.DistributionSteps.create_entity_completions(second_places_ids, act_date)
    steps.DistributionSteps.run_calculator_for_contract(first_contract_id)
    steps.DistributionSteps.run_calculator_for_contract(second_contract_id)

    # запускаем генерацию актов
    api.test_balance().GeneratePartnerAct(first_contract_id, act_date)
    api.test_balance().GeneratePartnerAct(second_contract_id, act_date)

    partner_act_data = steps.CommonPartnerSteps.get_partner_act_data_by_contract_id(group_contract_id)

    expected_partner_act_data = steps.DistributionData.filter_not_tail_data_rows(
        steps.DistributionData.create_expected_full_partner_act_data(group_contract_id,
                                                                     client_id,
                                                                     second_tag_id,
                                                                     second_places_ids,
                                                                     act_date)
    )

    utils.check_that(partner_act_data, contains_dicts_with_entries(expected_partner_act_data),
                     u"Проверяем, что по сгенерированы акты по хвосту универсального договора")
    utils.check_that(partner_act_data, has_length(len(expected_partner_act_data)),
                     u"Проверяем, что сгенерированы только акты хвоста универсального договора")


@pytest.mark.parametrize("is_offer", [True, False], ids=lambda io: 'OFFER' if io else 'GENERAL')
def test_two_heads_group(is_offer):
    group_type = DistributionContractType.GROUP_OFFER if is_offer else DistributionContractType.GROUP
    child_type = DistributionContractType.CHILD_OFFER if is_offer else DistributionContractType.AGILE

    second_service_start_dt = utils.Date.first_day_of_month() - relativedelta(months=4)
    second_start_dt = second_service_start_dt + relativedelta(months=1)
    second_end_dt = second_start_dt + relativedelta(months=2, days=-1)

    first_service_start_dt = second_service_start_dt - relativedelta(months=9)
    first_start_dt = second_start_dt - relativedelta(months=9)
    first_end_dt = second_end_dt - relativedelta(months=9)

    client_id, person_id, tag_id = steps.DistributionSteps.create_distr_client_person_tag()

    group_contract_id = create_group_contract(group_type, client_id, person_id, first_service_start_dt)
    first_contract_id, _ = create_child_contract(child_type, client_id, person_id, tag_id,
                                              group_contract_id, first_service_start_dt, first_start_dt,
                                              first_end_dt, DistributionType.VIDEO_HOSTING)

    second_contract_id, _ = create_child_contract(child_type, client_id, person_id, tag_id,
                                               group_contract_id, second_service_start_dt, second_start_dt,
                                               second_end_dt, DistributionType.VIDEO_HOSTING, multiplier=2)

    # создаем площадки
    places_ids = steps.DistributionSteps.create_fixed_and_revshare_places(client_id, tag_id,
                                                                          DistributionType.VIDEO_HOSTING)
    # добавляем открутки
    steps.DistributionSteps.create_entity_completions(places_ids, first_service_start_dt)
    steps.DistributionSteps.create_entity_completions(places_ids, first_start_dt)
    steps.DistributionSteps.run_calculator_for_contract(first_contract_id)

    # запускаем генерацию актов
    api.test_balance().GeneratePartnerAct(first_contract_id, first_start_dt)

    partner_act_data = steps.CommonPartnerSteps.get_partner_act_data_by_contract_id(group_contract_id)

    expected_partner_act_data = steps.DistributionData.create_expected_full_partner_act_data(group_contract_id,
                                                                                             client_id, tag_id,
                                                                                             places_ids, first_start_dt,
                                                                                             acts_number=2)

    utils.check_that(partner_act_data, contains_dicts_with_entries(expected_partner_act_data),
                     u"Проверяем, что партнерские акты имеют ожидаемые параметры для первого договора")

    #steps.DistributionSteps.run_calculator_for_contract(second_contract_id)

    # добавляем открутки
    steps.DistributionSteps.create_entity_completions(places_ids, second_service_start_dt)
    steps.DistributionSteps.create_entity_completions(places_ids, second_start_dt)
    steps.DistributionSteps.run_calculator_for_contract(second_contract_id)

    # запускаем генерацию актов
    api.test_balance().GeneratePartnerAct(second_contract_id, second_start_dt)

    partner_act_data = steps.CommonPartnerSteps.get_partner_act_data_by_contract_id(group_contract_id)

    expected_partner_act_data += steps.DistributionData.create_expected_full_partner_act_data(group_contract_id,
                                                                                              client_id, tag_id,
                                                                                              places_ids,
                                                                                              second_start_dt,
                                                                                              acts_number=2,
                                                                                              price_multiplier=2)

    utils.check_that(partner_act_data, contains_dicts_with_entries(expected_partner_act_data),
                     u"Проверяем, что партнерские акты имеют ожидаемые параметры для второго договора")


@reporter.feature(Features.TO_UNIT)
@pytest.mark.parametrize("group_type, child_type", [
    (DistributionContractType.GROUP, DistributionContractType.CHILD_OFFER),
    (DistributionContractType.GROUP_OFFER, DistributionContractType.AGILE)
], ids=lambda g, c: "{}_WITH_{}".format(DistributionContractType.name(g), DistributionContractType.name(c)))
def test_not_matching_contract_types(group_type, child_type):
    client_id, person_id, tag_id = steps.DistributionSteps.create_distr_client_person_tag()

    group_contract_id = create_group_contract(group_type, client_id, person_id, FIRST_SERVICE_START_DT)

    with pytest.raises(xmlrpclib.Fault) as error:
        create_child_contract(child_type, client_id, person_id, tag_id, group_contract_id, FIRST_SERVICE_START_DT,
                              FIRST_START_DT, FIRST_END_DT, DistributionType.VIDEO_HOSTING)

    expected_error = u'тип договора не совместим с типом родительского договора'
    utils.check_that(error.value.faultString, contains_string(expected_error), u'Проверяем текст ошибки')


@reporter.feature(Features.TO_UNIT)
def test_child_offer_without_group_offer():
    group_contract_id = None

    client_id, person_id, tag_id = steps.DistributionSteps.create_distr_client_person_tag()

    with pytest.raises(xmlrpclib.Fault) as error:
        create_child_contract(DistributionContractType.CHILD_OFFER, client_id, person_id, tag_id, group_contract_id,
                              FIRST_SERVICE_START_DT, FIRST_START_DT, FIRST_END_DT, DistributionType.VIDEO_HOSTING)

    expected_error = u'для дочерней офферты должен быть указан родительский договор'
    utils.check_that(error.value.faultString, contains_string(expected_error), u'Проверяем текст ошибки')


# При создании дочерних договоров у них должна быть сквозная нумерация в external_id (1, 2, 3,...)
@reporter.feature(Features.TO_UNIT)
@pytest.mark.tickets('BALANCE-28843')
@pytest.mark.parametrize("group_type, child_type", [
    (DistributionContractType.GROUP_OFFER, DistributionContractType.CHILD_OFFER),
    (DistributionContractType.GROUP, DistributionContractType.UNIVERSAL),
], ids=lambda g, c: "CHILD_EXT_ID_{}_WITH_{}".format(DistributionContractType.name(g), DistributionContractType.name(c)))
def test_through_numeration_for_child_offer_external_id(group_type, child_type):
    with reporter.step(u'Создаём клиента и плательщика'):
        client_id, person_id, _ = steps.DistributionSteps.create_distr_client_person_tag()
    with reporter.step(u'Создаём групповой (родительский) договор'):
        group_contract_id = create_group_contract(group_type, client_id, person_id, FIRST_SERVICE_START_DT)

    for child_num in range(1, 3):
        new_tag_id = steps.DistributionSteps.create_distr_tag(client_id)
        with reporter.step(u'Создаём дочерний подписанный договор с новым тэгом (шаг ' + str(child_num) + ')'):
            _, child_external_contract_id = create_child_contract(child_type, client_id, person_id, new_tag_id,
                                                            group_contract_id, FIRST_SERVICE_START_DT,
                                                            FIRST_START_DT, FIRST_END_DT, DistributionType.VIDEO_HOSTING)

        utils.check_that(child_external_contract_id, equal_to(str(child_num)),
                         u'Сравниваем ожидаемые и полученные external_id для дочерних договоров')


# -----------------------------------------------
# Utils

def create_group_and_children_contracts(client_id, person_id, first_tag_id,
                                        second_tag_id, is_offer, firm=Firms.YANDEX_1):
    with reporter.step(u"Создаем групповой договор с двумя детьми (универсальный и гибкий договоры)"):
        group_type = DistributionContractType.GROUP_OFFER if is_offer else DistributionContractType.GROUP
        first_type = DistributionContractType.CHILD_OFFER if is_offer else DistributionContractType.AGILE
        second_type = DistributionContractType.CHILD_OFFER if is_offer else DistributionContractType.UNIVERSAL

        group_contract_id = create_group_contract(group_type, client_id, person_id, GROUP_START_DT, firm)

        first_contract_id, _ = create_child_contract(first_type, client_id, person_id, first_tag_id,
                                                  group_contract_id, FIRST_SERVICE_START_DT, FIRST_START_DT,
                                                  FIRST_END_DT, DistributionType.VIDEO_HOSTING, firm)

        second_contract_id, _ = create_child_contract(second_type, client_id, person_id, second_tag_id,
                                                   group_contract_id, SECOND_SERVICE_START_DT, SECOND_START_DT,
                                                   SECOND_END_DT, DistributionType.DIRECT, firm)

        return group_contract_id, first_contract_id, second_contract_id


def create_group_contract(contract_type, client_id, person_id, start_dt, firm=Firms.YANDEX_1):
    create_contract_method = steps.ContractSteps.create_offer \
        if 'OFFER' in DistributionContractType.name(contract_type) \
        else steps.ContractSteps.create_common_contract

    group_contract_id, _ = steps.DistributionSteps.create_full_contract(contract_type, client_id, person_id, None,
                                                                        start_dt, start_dt, firm=firm,
                                                                        create_contract=create_contract_method)

    return group_contract_id


def create_child_contract(contract_type, client_id, person_id, tag_id, parent_contract_id,
                          service_start_dt, start_dt, end_dt, exclude_revshare_type, firm=Firms.YANDEX_1,
                          multiplier=1):
    create_contract_method = steps.ContractSteps.create_offer \
        if 'OFFER' in DistributionContractType.name(contract_type) \
        else steps.ContractSteps.create_common_contract

    with reporter.step(u'Создаём дочерний подписанный договор'):
        contract_id, external_contract_id = steps.DistributionSteps.create_full_contract(contract_type, client_id,
                                                                      person_id, tag_id,
                                                                      start_dt, service_start_dt, firm=firm,
                                                                      parent_contract_id=parent_contract_id,
                                                                      multiplier=multiplier,
                                                                      exclude_revshare_type=exclude_revshare_type,
                                                                      create_contract=create_contract_method)

    steps.ContractSteps.create_collateral(constants.Collateral.DISTR_TERMINATION,
                                          {'CONTRACT2_ID': contract_id,
                                          'DT': (start_dt + relativedelta(days=1)).isoformat(),
                                          'TAIL_TIME': TAIL_TIME,
                                          'END_DT': end_dt.isoformat(),
                                          'IS_SIGNED': service_start_dt.isoformat()})

    return contract_id, external_contract_id

