# coding: utf-8
__author__ = 'a-vasin'

from xmlrpclib import Fault

import pytest
from dateutil.relativedelta import relativedelta
from hamcrest import has_length, contains_string

from balance import balance_db as db, balance_api as api
import btestlib.reporter as reporter
from balance import balance_steps as steps
from balance.distribution.distribution_types import DistributionType, DistributionSubtype
from balance.features import Features
from btestlib import shared, utils
from btestlib.constants import DistributionContractType

pytestmark = [
    pytest.mark.tickets('BALANCE-11569'),
    reporter.feature(Features.DISTRIBUTION, Features.PARTNER_REWARD, Features.REVSHARE)
]

START_DT = utils.Date.first_day_of_month() - relativedelta(months=1)


# В новой схеме эта проверка не имеет смысла, тк открутки с внутренними completion_type
# отфильтровываются ещё в заборщике

# @pytest.mark.parametrize("completion_type, is_completions_expected, exclude_revshare_type",
#                          [
#                              (1, True, DistributionType.DIRECT),
#                              (6, False, DistributionType.VIDEO_HOSTING),
#                              (7, False, DistributionType.DIRECT),
#                              (8, True, DistributionType.VIDEO_HOSTING)
#                          ],
#                          ids=[
#                              "COMMERCE",
#                              "INNER",
#                              "PROMO",
#                              "CAMPAIGNS"
#                          ])
# def test_inner_revshare(completion_type, is_completions_expected, exclude_revshare_type):
#     revshare_types = [distribution_type for distribution_type in DistributionType
#                       if distribution_type.subtype == DistributionSubtype.REVSHARE
#                       and distribution_type != exclude_revshare_type]
#
#     expected_length = len(revshare_types) if is_completions_expected else 0
#
#     # создаем клиента, плательщика и тэг
#     client_id, person_id, tag_id = steps.DistributionSteps.create_distr_client_person_tag()
#
#     # создаем договор дистрибуции
#     contract_id, _ = steps.DistributionSteps.create_full_contract(DistributionContractType.REVSHARE, client_id,
#                                                                   person_id, tag_id, START_DT, START_DT,
#                                                                   revshare_types=revshare_types,
#                                                                   exclude_revshare_type=None)
#     # создаем площадки
#     places_ids, _ = steps.DistributionSteps.create_places(client_id, tag_id, revshare_types)
#
#     # добавляем открутки
#     steps.DistributionSteps.create_entity_completions(places_ids, START_DT, completion_type)
#     api.test_balance().RunPartnerCalculator(contract_id, START_DT)
#
#     completions = steps.DistributionSteps.get_distribution_money(START_DT, places_ids)
#     utils.check_that(completions, has_length(expected_length), u"Проверяем количество откруток во view")


@reporter.feature(Features.TO_UNIT)
@pytest.mark.parametrize("contract_type", [
    DistributionContractType.REVSHARE,
    DistributionContractType.AGILE,
    DistributionContractType.UNIVERSAL,
    DistributionContractType.OFFER
], ids=lambda ct: DistributionContractType.name(ct))
def test_incompatible_revshare_products(contract_type):
    client_id, person_id, tag_id = steps.DistributionSteps.create_distr_client_person_tag()

    revshare_types = [DistributionType.DIRECT, DistributionType.VIDEO_HOSTING]

    with pytest.raises(Fault) as error:
        steps.DistributionSteps.create_full_contract(contract_type, client_id, person_id, tag_id,
                                                     START_DT, START_DT, revshare_types=revshare_types,
                                                     exclude_revshare_type=None)

    expected_error = u"В одном договоре невозможно использовать продукты 'Дистрибуция.Разделение доходов Директ' и 'Видеореклама на площадке'"
    utils.check_that(error.value.faultString, contains_string(expected_error), u'Проверяем текст ошибки')


# ---------------------------------------------
# Utils

def get_view_completions(places_ids):
    # a-vasin: так убого, потому что через IN реально дольше запрос отрабатывает
    with reporter.step(u"Получаем количество откруток из вьюхи"):
        result = []
        for place_id in places_ids.values():
            query = "SELECT * FROM V_DISTR_REVSHARE_TURNOVER WHERE PLACE_ID=:place_id"
            params = {'place_id': place_id}
            result += db.balance().execute(query, params)
        return result
