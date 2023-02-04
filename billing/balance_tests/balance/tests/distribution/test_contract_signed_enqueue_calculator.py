# -*- coding: utf-8 -*-
__author__ = 'a-vasin'

import pytest
from dateutil.relativedelta import relativedelta
from hamcrest import empty
from hamcrest import anything

from datetime import datetime
from decimal import Decimal

import btestlib.reporter as reporter
from balance import balance_steps as steps, balance_api as api, balance_db as db
from balance.distribution.distribution_types import DistributionType, DistributionSubtype
from balance.features import Features
from btestlib.matchers import equal_to, contains_dicts_with_entries
from btestlib import utils
from btestlib.data import defaults
from btestlib.constants import Nds, NdsNew

pytestmark = [
    pytest.mark.slow,
    reporter.feature(Features.DISTRIBUTION, Features.PARTNER_REWARD, Features.PARTNER_ACT)
]

CUR_MONTH_DT = utils.Date.first_day_of_month()
START_DT = utils.Date.first_day_of_month() - relativedelta(months=1)
PREV_DT = utils.Date.first_day_of_month() - relativedelta(months=2)
SERVICE_START_DT = utils.Date.first_day_of_month() - relativedelta(months=3)

OPERATOR_UID = defaults.PASSPORT_UID

contract_month_query = '''
select e.object_id, cm.month_dt, e.enqueue_dt
    from bo.t_export e
    join bo.t_contract_month cm on (cm.id = e.object_id)
    where type = 'ENTITY_CALC' and cm.contract_id = :cid
    '''


def check_enqueued_months(contract_id, message):
    enqueued_data = db.balance().execute(contract_month_query, {'cid': contract_id})
    expected_dts = [{'object_id': anything(), 'enqueue_dt': anything(),
                     'month_dt': CUR_MONTH_DT - relativedelta(months=i)} for i in range(4)]
    utils.check_that(enqueued_data, contains_dicts_with_entries(expected_dts), message)
    return [item['object_id'] for item in enqueued_data]


def create_updated_contract_params(contract_id, contract_params, patch_params):
    updated_contract_params = contract_params.copy()
    for k, v in patch_params.items():
        updated_contract_params[k] = v
    updated_contract_params['ID'] = str(contract_id)
    return updated_contract_params


@reporter.feature(Features.TO_UNIT)
@pytest.mark.parametrize('signed_method', ['IS_SIGNED', 'IS_FAXED'])
def test_contract_test_mode_enqueued_when_changed_and_signed(signed_method):
    exclude_revshare_type = DistributionType.VIDEO_HOSTING

    # создаем клиента, плательщика и тэг
    client_id, person_id, tag_id = steps.DistributionSteps.create_distr_client_person_tag()

    products_revshare = [(str(distr_type.contract_price_id),
                          str(distr_type.default_price) if distr_type != exclude_revshare_type else '')
                         for distr_type in DistributionType if distr_type.subtype == DistributionSubtype.REVSHARE]

    contract_params = {'CLIENT_ID': client_id, 'PERSON_ID': person_id,
                       'DT': START_DT,
                       'DISTRIBUTION_TAG': tag_id,
                       'SERVICE_START_DT': SERVICE_START_DT,
                       'PRODUCTS_REVSHARE': products_revshare,
                       'INSTALL_PRICE': DistributionType.INSTALLS.default_price,
                       'MEMO': 'memo_666',
                       'SEARCH_PRICE': DistributionType.SEARCHES.default_price,
                       'ACTIVATION_PRICE': DistributionType.ACTIVATIONS.default_price,
                       'MANAGER_CODE': defaults.Managers.VECHER.code,
                       }

    # создаем договор дистрибуции в тестовом режиме
    contract_id, external_id = steps.ContractSteps.create_contract('universal_distr_test_mode', contract_params)
    check_enqueued_months(contract_id, u'Проверяем, что после создания договора в очередь проставились 4 месяца'
                                       u' с момента SERVICE_START_DT до текущего месяца')

    db_sysdate = db.balance().execute('select sysdate from dual')[0]['sysdate']

    # Меняем поле MEMO-like
    patch_params = {'MEMO': 'MEMO XXX 666 freaky robot test pupik'}
    updated_contract_params = create_updated_contract_params(contract_id, contract_params, patch_params)
    contract_id, external_id = steps.ContractSteps.create_contract('universal_distr_test_mode', updated_contract_params)
    enqueued_count = len(db.balance().execute(contract_month_query + ' and enqueue_dt > :db_sysdate',
                                              {'cid': contract_id, 'db_sysdate': db_sysdate}))
    utils.check_that(enqueued_count, equal_to(0), u'Проверяем, что изменении полей аля-МЕМО в договоре '
                                                  u'в тестовом периоде пересчёт головы не ставится в очередь '
                                                  u'(DB sysdate: %s)' % (db_sysdate, ))

    # Меняем значиме поля
    patch_params = {'SEARCH_PRICE': Decimal('6.66')}
    updated_contract_params = create_updated_contract_params(contract_id, contract_params, patch_params)
    contract_id, external_id = steps.ContractSteps.create_contract('universal_distr_test_mode', updated_contract_params)
    check_enqueued_months(contract_id, u'Проверяем, что после изменения значимых полей в очередь проставились 4 месяца '
                                       u'с момента SERVICE_START_DT до текущего месяца')

    # Подписываем договор
    patch_params = {signed_method: datetime.now().replace(microsecond=0).isoformat()}
    updated_contract_params = create_updated_contract_params(contract_id, contract_params, patch_params)
    contract_id, external_id = steps.ContractSteps.create_contract('universal_distr_test_mode', updated_contract_params)

    check_enqueued_months(contract_id, u'Проверяем, что после подписания в очередь проставились 4 месяца '
                                       u'с момента SERVICE_START_DT до текущего месяца')

@pytest.mark.long
def test_contract_changed_after_test_mode():
    exclude_revshare_type = DistributionType.VIDEO_HOSTING

    # создаем клиента, плательщика и тэг
    client_id, person_id, tag_id = steps.DistributionSteps.create_distr_client_person_tag()

    rs_distribution_types = [distribution_type for distribution_type in DistributionType
                             if distribution_type.subtype == DistributionSubtype.REVSHARE
                             and distribution_type != exclude_revshare_type]

    # создаем площадки
    places_ids, _ = steps.DistributionSteps.create_places(client_id, tag_id, rs_distribution_types)

    products_revshare = [(str(distr_type.contract_price_id),
                          str(distr_type.default_price) if distr_type != exclude_revshare_type else '')
                         for distr_type in DistributionType if distr_type.subtype == DistributionSubtype.REVSHARE]

    contract_params = {'CLIENT_ID': client_id, 'PERSON_ID': person_id,
                       'DT': START_DT,
                       'DISTRIBUTION_TAG': tag_id,
                       'SERVICE_START_DT': SERVICE_START_DT,
                       'PRODUCTS_REVSHARE': products_revshare,
                       'MANAGER_CODE': defaults.Managers.VECHER.code,
                       'NDS': NdsNew.ZERO.nds_id  # Создаём с нулевым, чтобы потом поменять,
                                                  # и убедиться, что изменения нормально подхватываются
                       }

    # создаем договор дистрибуции в тестовом режиме
    contract_id, external_id = steps.ContractSteps.create_contract('universal_distr_test_mode', contract_params)

    check_enqueued_months(contract_id,  u'При создании договора проставляются на пересчет 4 месяца '
                                        u'(с момента SERVICE_START_DT)')

    # добавляем открутки, на два месяца
    # при этом дата начала действия договора (dt) совпадает со START_DT
    steps.DistributionSteps.create_entity_completions(places_ids, START_DT)
    steps.DistributionSteps.create_entity_completions(places_ids, PREV_DT)

    # Можно запускать калькулятор и через разборщик месяцев, смотри ниже
    api.test_balance().RunPartnerCalculator(contract_id, START_DT)
    api.test_balance().RunPartnerCalculator(contract_id, PREV_DT)

    for dt in (START_DT, PREV_DT):
        completions_info = steps.DistributionSteps.get_distribution_money(dt, places_ids)
        expected_completions_info = (
            steps.DistributionData.create_expected_full_completion_rows(
                contract_id, client_id, tag_id, places_ids, dt, nds=NdsNew.ZERO))

        utils.check_that(completions_info, contains_dicts_with_entries(expected_completions_info),
                         u"Проверяем, что открутки имеют ожидаемые параметры на дату %s, НДС 0%%" % (dt, ))

    # Меняем условия договора: НДС
    patch_params = {'NDS': NdsNew.DEFAULT.nds_id}
    updated_contract_params = create_updated_contract_params(contract_id, contract_params, patch_params)
    contract_id, external_id = steps.ContractSteps.create_contract('universal_distr_test_mode', updated_contract_params)

    enqueued_ids = check_enqueued_months(contract_id, u'При изменении НДС проставляются на пересчет 4 месяца '
                                                      u'(с момента SERVICE_START_DT)')
    for oid in enqueued_ids:
        steps.CommonSteps.export('ENTITY_CALC', 'ContractMonth', oid)

    for dt in (START_DT, PREV_DT):
        completions_info = steps.DistributionSteps.get_distribution_money(dt, places_ids)
        expected_completions_info = (
            steps.DistributionData.create_expected_full_completion_rows(
                contract_id, client_id, tag_id, places_ids, dt, nds=NdsNew.DEFAULT))

        utils.check_that(completions_info, contains_dicts_with_entries(expected_completions_info),
                         u"Проверяем, что открутки имеют ожидаемые параметры на дату %s, НДС стандартный" % (dt, ))

    # Продписываем договор
    # Примечание: можно сменить обратно на НДС 0%
    patch_params = {'IS_SIGNED': datetime.now().replace(microsecond=0).isoformat(),
                    'NDS': NdsNew.DEFAULT.nds_id
                    }
    updated_contract_params = create_updated_contract_params(contract_id, contract_params, patch_params)
    contract_id, external_id = steps.ContractSteps.create_contract('universal_distr_test_mode', updated_contract_params)
    enqueued_ids = check_enqueued_months(contract_id, u'При подписании калькулируется за 4 месяца')
    for oid in enqueued_ids:
        steps.CommonSteps.export('ENTITY_CALC', 'ContractMonth', oid)

    for dt in (START_DT, PREV_DT):
        completions_info = steps.DistributionSteps.get_distribution_money(dt, places_ids)
        expected_completions_info = (
            steps.DistributionData.create_expected_full_completion_rows(
                contract_id, client_id, tag_id, places_ids, dt, nds=NdsNew.DEFAULT))

        utils.check_that(completions_info, contains_dicts_with_entries(expected_completions_info),
                         u"Проверяем, что открутки имеют ожидаемые параметры на дату %s" % (dt, ))
