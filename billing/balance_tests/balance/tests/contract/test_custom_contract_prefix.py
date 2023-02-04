# -*- coding: utf-8 -*-
__author__ = 'alshkit'

import pytest
from hamcrest import contains_string

import balance.balance_db as db
import btestlib.reporter as reporter
from balance import balance_steps as steps
from btestlib import utils
from btestlib.data.partner_contexts import CORP_TAXI_RU_CONTEXT_SPENDABLE_MIGRATED, FOOD_COURIER_SPENDABLE_BY_CONTEXT
from balance.features import Features


# забираем префикс договора из соответствующей таблицы
def get_custom_contract_prefix(service, firm, is_offer, contract_type):
    query = 'SELECT prefix from t_contract_prefix where service_id=:service_id and firm_id=:firm_id and ' \
            'is_offer=:is_offer and contract_type=:contract_type'
    with reporter.step(u'Забираем префикс для договора из таблицы t_contract_prefix'):
        result = db.balance().execute(query, {'service_id': service.id, 'firm_id': firm.id,
                                              'is_offer': is_offer, 'contract_type': contract_type})[0]['prefix']
    return result


# по мотивам https://st.yandex-team.ru/BALANCE-26559
# префикс должен применяться, если договор соответствует записи в t_contract_prefix - первый тест
# префикс не должен применяться, если договор не соответствует записиа в t_contract_prefix - второй тест
# логика только для расходных договоров
@reporter.feature(Features.TO_UNIT)
@pytest.mark.parametrize('context, expected_prefix, is_offer',
                         [
                             (FOOD_COURIER_SPENDABLE_BY_CONTEXT, None, 1),
                             (CORP_TAXI_RU_CONTEXT_SPENDABLE_MIGRATED, 'РАС', 0),
                         ],
                         ids=
                         [
                             'Prefix for contraxt from prefix table',
                             'Prefix for contract not from prefix table',
                         ]
                         )
def test_custom_contract_prefix(context, expected_prefix, is_offer):
    client_id, _, _, contract_eid = steps.ContractSteps.create_partner_contract(context, is_offer=is_offer)

    contract_prefix = expected_prefix or \
                      get_custom_contract_prefix(context.service, context.firm, is_offer,
                                                 context.contract_type.name)
    utils.check_that(contract_eid, contains_string(contract_prefix.decode('utf-8')),
                     step='Проверим, что использовался кастомный префикс')
