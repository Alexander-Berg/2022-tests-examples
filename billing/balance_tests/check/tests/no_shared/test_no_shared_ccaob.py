# coding: utf-8
__author__ = 'chihiro'

import json

import pytest
from hamcrest import equal_to

import btestlib.reporter as reporter
from balance import balance_api as api
from balance.balance_db import balance
from balance.balance_steps import ContractSteps
from btestlib import utils as butils
from check import steps as check_steps
from check import utils


def new_section(description):
    print(description)
    return description


def refresh_data_in_cache():
    contracts = {}

    # ----------------------------------------------------------------------------
    # ----------------------------------------------------------------------------
    description = 'CHECK_2515_contract_offer_diffs_without_diff'
    reporter.log(description)
    contract_id = check_steps.create_contract_offer(is_offer=False)
    collateral_id = balance().execute('select id from T_CONTRACT_COLLATERAL where CONTRACT2_ID = :contract_id ',
                                      {'contract_id': contract_id})[0]["id"]
    attribute_batch_id = \
        balance().execute('select attribute_batch_id from T_CONTRACT_COLLATERAL where ID = :contract_id ',
                          {'contract_id': collateral_id})[0]["attribute_batch_id"]

    query = """
            update bo.t_contract_attributes set value_num = 0 
            where collateral_id = :collateral_id 
            and attribute_batch_id = :attribute_batch_id 
            and code = 'IS_OFFER'
        """
    query_params = {'collateral_id': collateral_id,
                    'num': 0, 'attribute_batch_id': attribute_batch_id}
    balance().execute(query, query_params)
    ContractSteps.refresh_contracts_cache(contract_id)

    contracts[description] = {'id': contract_id, 'state': 0, 'attribute_code': 'IS_OFFER',
                              'collateral_id': collateral_id}
    # ----------------------------------------------------------------------------
    return contracts


@pytest.fixture(scope="module")
def fixtures():

    contracts_cache = refresh_data_in_cache()
    utils.create_data_file('ccaob_data.txt', json.dumps(contracts_cache))

    objects = []
    for description in contracts_cache:
        if str(contracts_cache[description]['id']) not in objects:
            objects.append(str(contracts_cache[description]['id']))
    cmp_id = utils.run_check_new('ccaob', str(','.join(objects)))
    contracts_cache['diff_contracts'] = get_diff_orders(
        cmp_id=cmp_id,
        check_code_name='ccaob')
    return contracts_cache


def get_diff_orders(cmp_id, check_code_name):
    query = """
        select collateral_id, state, attribute_code
        from cmp.{0}_cmp_data
        where cmp_id = {1}
    """.format(check_code_name, cmp_id)
    rows = api.test_balance().ExecuteSQL('cmp', query)
    res = []
    for row in rows:
        res.append([row['collateral_id'], row['attribute_code'], row['state']])
    return res


@pytest.mark.parametrize(
    "description",
    [
        'CHECK_2515_contract_offer_diffs_without_diff'
    ],
    ids=lambda x: x
)
def test_ccaob(fixtures, description):
    print(description)
    check_diff(fixtures[description]['collateral_id'], fixtures['diff_contracts'],
               fixtures[description]['attribute_code'], fixtures[description]['state'])


def check_diff(collateral_id, diff_contracts, attribute_code, expected_state):
    state = 0
    for row in diff_contracts:
        if row[0] == collateral_id:
            if attribute_code == row[1]:
                state = row[2]

    print('collateral_id - ' + str(collateral_id))
    print('state = ' + str(state) + ';   expected - ' + str(expected_state))
    butils.check_that(state, equal_to(expected_state))



if __name__ == "__main__":
    pass
