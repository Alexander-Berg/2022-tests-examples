#!/usr/bin/python
# coding=utf-8

import datetime
import json
import os

import pytest

import btestlib.reporter as reporter
import check.db
from balance import balance_api as api
from balance import balance_steps as steps
from balance.balance_db import balance
from check import db
from check import steps as check_steps
from check import utils
from check.defaults import DATA_DIR


def create_contract(client_id, person_id, contract_type, services=[37],
                    start_dt=datetime.datetime.now().replace(day=1),
                    end_dt=datetime.datetime.now().replace(day=1) + datetime.timedelta(weeks=5)):
    start_dt = start_dt.strftime('%Y-%m-%dT00:00:00')
    end_dt = end_dt.strftime('%Y-%m-%dT00:00:00')
    contract_id, _ = steps.ContractSteps.create_contract(contract_type,
                                                         {'CLIENT_ID': client_id,
                                                          'PERSON_ID': person_id,
                                                          'DT': '{0}'.format(start_dt),
                                                          'FINISH_DT': '{0}'.format(end_dt),
                                                          'IS_SIGNED': '{0}'.format(start_dt),
                                                          'CURRRENCY': 'USD',
                                                          'SERVICES': services})
    return contract_id


def refresh_data_in_cache():
    contracts = {}
    client_id = check_steps.create_client()
    steps.ExportSteps.export_oebs(client_id=client_id)

    # ----------------------------------------------------------------------------
    # ----------------------------------------------------------------------------
    description = 'CHECK_2515_contract_offer_diffs_without_diff'
    reporter.log(description)
    contract_id = check_steps.create_contract_offer(is_offer=False)

    attribute_batch_id = balance().execute('select attribute_batch_id from T_CONTRACT_COLLATERAL where CONTRACT2_ID = :contract_id ',
                                      {'contract_id': contract_id})[0]["attribute_batch_id"]
    collateral_id = \
    balance().execute('select id from T_CONTRACT_COLLATERAL where CONTRACT2_ID = :contract_id ',
                      {'contract_id': contract_id})[0]["id"]

    query = """
        update bo.t_contract_attributes set value_num = 0 
        where collateral_id = :collateral_id 
        and attribute_batch_id = :attribute_batch_id 
        and code = 'IS_OFFER'
    """
    query_params = {'collateral_id': collateral_id,
                    'attribute_batch_id': attribute_batch_id,
                    'num': 0}
    balance().execute(query, query_params)
    steps.ContractSteps.refresh_contracts_cache(contract_id)

    contracts[description] = {'id': contract_id, 'state': 0, 'attribute_code': 'IS_OFFER'}
    # ----------------------------------------------------------------------------
    return contracts


@pytest.fixture(scope="module")
def contract_list():
    contracts_cache = {
        u'CHECK_2515_contract_offer_diffs_without_diff':{
            u'state': 0,
            u'id': 1048652,
            u'attribute_code': u'IS_OFFER'
        }
    }

    objects = []
    for description in contracts_cache:
        objects.append(str(contracts_cache[description]['id']))
    utils.run_check_new('caob', str(','.join(objects)))

    cmp_id = check.db.get_cmp_id_list(['caob'])
    return contracts_cache, cmp_id


@pytest.mark.parametrize("type_", ['caob'])
@pytest.mark.parametrize("description", [
    'CHECK_2515_contract_offer_diffs_without_diff'
]
    , ids=lambda x: x
                         )
def test_no_shared_caob(contract_list, description, type_):
    contracts, cmp_id = contract_list
    contract_id = contracts[description]['id']
    attribute_code = contracts[description]['attribute_code']
    query = """
            select key_num
            from bo.t_contract_attributes
            where collateral_id =
            (select id
            from bo.t_contract_collateral
            where contract2_id = :contract_id
            and num is null)
            and code = :attribute_code
            """
    query_params = {'contract_id': contract_id, 'attribute_code': attribute_code}
    res = balance().execute(query, query_params)
    attribute_key = res[0]['key_num'] if res else -1
    if attribute_key is None:
        attribute_key = -1
    table = 'cmp.{0}_cmp_data'.format(type_)
    query = ('select state ' \
             'from {0} ' \
             'where contract_id= {1} ' \
             'and cmp_id= {2} ' \
             'and attribute_code = \'{3}\' ' \
             'and attribute_key = {4}').format(table, contract_id, cmp_id[type_], attribute_code, attribute_key)
    res = api.test_balance().ExecuteSQL('cmp', query)
    state = res[0]['state'] if res else 0
    expected_state = contracts[description]['state']
    print(contract_id)
    assert expected_state == state


if __name__ == "__main__":
    pass
