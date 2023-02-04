# coding=utf-8
import pytest
from btestlib.data.partner_contexts import MARKET_BLUE_AGENCY_CONTEXT_SPENDABLE as context
from balance import balance_steps as steps
from btestlib import utils
from datetime import datetime, timedelta
from dateutil.relativedelta import relativedelta
from xmlrpclib import Fault
import json
import balance.balance_db as db

CONTRACT_START_DT = utils.Date.first_day_of_month(datetime.now() - relativedelta(months=2))


def date_generator():
    day = 1
    while True:
        yield CONTRACT_START_DT + timedelta(days=day)
        day += 1


def create_contract(categories):
    client_id, person_id, contract_id, contract_eid = steps.ContractSteps.create_partner_contract(
        context,
        additional_params={
            'start_dt': CONTRACT_START_DT,
            'market_agency_categories': categories
        }
    )
    return client_id, person_id, contract_id, contract_eid


def create_collateral(contract_id, dt, categories):
    return steps.ContractSteps.create_collateral_real(
        contract_id, 7100, {
            'market_agency_categories': categories,
            'DT': dt,
            'SIGN': 1,
            'SIGN_DT': dt
        }
    )


def create_contract_and_collateral(categories):
    client_id, person_id, contract_id, contract_eid = create_contract([{"category_id": 198119, "value": 200}])
    create_collateral(contract_id, next(date_generator()), categories)
    return client_id, person_id, contract_id, contract_eid


def check_attrs(contract_id, expected_dict_list):
    query = """
        with ids as (
            select ca.KEY_NUM, max(ca.id) id
            from t_contract2 c
                     join T_CONTRACT_COLLATERAL cc on cc.CONTRACT2_ID = c.id
                     join T_CONTRACT_ATTRIBUTES ca on ca.COLLATERAL_ID = cc.id
            where ca.code = 'MARKET_AGENCY_CATEGORIES'
              and c.id = :contract_id
              and cc.IS_CANCELLED is null
            group by ca.KEY_NUM
        ) 
        select ca.VALUE_STR from ids
        join T_CONTRACT_ATTRIBUTES ca on ids.id=ca.id
        where ca.VALUE_STR != 'null'
    """
    params = {'contract_id': contract_id}
    result = db.balance().execute(query, params)
    actual_attrs = {row['value_str'] for row in result}
    expected = {json.dumps({k: str(v) for k, v in c.items()}) for c in expected_dict_list}

    assert actual_attrs == expected


@pytest.mark.parametrize(
    "categories, expected_error",
    (
        pytest.param([{"category_id": 90401, "value": "400"}, {"category_id": "198119", "value": 200}], None, id='multiple'),
        pytest.param([{"category_id": "666", "value": "666"}], 'market_agency_category with category_id="666" and value="666" not found', id='not found'),
        pytest.param([{"category_id": "90401"}], 'missing "value" key in one of the market_agency_category', id='missing value'),
        pytest.param([{"value": "200"}], 'missing "category_id" key in one of the market_agency_category', id='missing category_id'),
        pytest.param([dict(category_id=90401, value=400), dict(category_id=90401, value=400)], None, id='duplicate'),
        pytest.param([], u'Не выбрана ни одна категория', id='empty'),
    )
)
@pytest.mark.parametrize(
    "func",
    (
        pytest.param(create_contract, id='contract'),
        pytest.param(create_contract_and_collateral, id='collateral')
    )
)
def test_create_contract_and_collateral(func, categories, expected_error):
    try:
        _, _, contract_id, _ = func(categories)
        assert expected_error is None, 'must to fail'
    except Fault as exc:
        print steps.CommonSteps.get_exception_code(exc, 'contents')
        assert expected_error in steps.CommonSteps.get_exception_code(exc, 'contents')
        return

    check_attrs(contract_id, categories)
