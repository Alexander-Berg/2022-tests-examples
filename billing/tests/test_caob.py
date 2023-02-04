from datetime import datetime, timedelta, date
import cyson

import pytest

from billing.dwh.src.dwh.grocery.dcs.export.contracts import is_comparable, extract_attributes
from billing.dwh.src.dwh.grocery.dcs.export.contracts.balance_common.utils import trunc_date, to_str_value
from billing.contract_iface.contract_json import JSONContract

from .utils import (
    gen_general_contract, gen_spendable_contract, gen_distribution_contract
)

EMPTY_YSON = cyson.dumps({})


def get_attribute_value(row):
    attribute_code = row['attribute_code']
    if attribute_code == 'SERVICES' and row['value_num'] == '1':
        return row['attribute_key']

    for key in ('value_num', 'value_str', 'value_dt'):
        value = row[key]
        if value is not None:
            break
    return value


def test_not_comparable():
    min_finish_dt = datetime.now()
    contract_finish_dt = min_finish_dt - timedelta(days=1)

    contract_data = gen_general_contract()
    contract_data['collaterals']['0']['finish_dt'] = contract_finish_dt.isoformat()

    assert not is_comparable(contract_yson=cyson.dumps(contract_data),
                             firm_export_yson=EMPTY_YSON,
                             person_yson=EMPTY_YSON,
                             oebs_configs_yson=EMPTY_YSON,
                             min_finish_dt=min_finish_dt)


def test_print_form_dt(some_firm_export):
    contract_data = gen_general_contract()

    contract_data['collaterals']['0']['start_dt'] = datetime(2000, 1, 1).isoformat()
    contract_data['collaterals']['0']['print_form_dt'] = datetime(2000, 6, 1).isoformat()

    expected = date(2000, 6, 1).strftime('%Y-%m-%d')

    attrs = extract_attributes(contract_yson=cyson.dumps(contract_data),
                               firm_export_yson=cyson.dumps(some_firm_export),
                               person_yson=EMPTY_YSON,
                               oebs_configs_yson=EMPTY_YSON,
                               currency_codes={})
    for row in attrs:
        attribute_code = row['attribute_code'].lower()
        if attribute_code == 'start_dt':
            attribute_value = get_attribute_value(row)
            assert attribute_value == expected
            break
    else:
        assert False, 'not found start_dt attribute'


def test_tail_time(some_firm_export):
    contract_data = gen_distribution_contract()
    expected = date(2000, 7, 1).strftime('%Y-%m-%d')

    contract_data['collaterals']['0']['end_dt'] = datetime(2000, 1, 1).isoformat()
    contract_data['collaterals']['0']['tail_time'] = 6

    attrs = extract_attributes(contract_yson=cyson.dumps(contract_data),
                               firm_export_yson=cyson.dumps(some_firm_export),
                               person_yson=EMPTY_YSON,
                               oebs_configs_yson=EMPTY_YSON,
                               currency_codes={})

    for row in attrs:
        attribute_code = row['attribute_code'].lower()
        if attribute_code == 'finish_dt':
            attribute_value = get_attribute_value(row)
            assert attribute_value == expected
            break
    else:
        assert False, 'not found finish_dt attribute'


@pytest.mark.parametrize(
    'contract_type, contract_kwargs',
    [
        pytest.param('GENERAL',
                     {'services': ['366560231', ]}),
        pytest.param('SPENDABLE',
                     {'services': ['135', ]}),
    ],
)
def test_contract_attributes(contract_type, contract_kwargs, some_firm_export):
    if contract_type == 'GENERAL':
        contract_data = gen_general_contract(**contract_kwargs)
    elif contract_type == 'SPENDABLE':
        contract_data = gen_spendable_contract(**contract_kwargs)
    else:
        raise AttributeError(f"Unknown type {contract_type}")

    contract = JSONContract(contract_data=contract_data)
    expected = {
        'contract_id': contract.id,
        'contract_eid': contract.external_id,
        'start_dt': trunc_date(contract.col0.dt),
        'is_offer': int(contract.is_offer),
        'person_id': 'P{}'.format(contract.person_id),
        'payment_type': contract.current.payment_type,
        'services': contract.current.services,
        'currency_code': 'RUB',
        'org_id': some_firm_export[0]['oebs_org_id'],
    }

    if contract.current.get('finish_dt'):
        finish_dt = trunc_date(contract.current.finish_dt) - timedelta(days=1)
        expected['finish_dt'] = finish_dt

    for key in expected.keys():
        if key == 'services':
            continue
        expected[key] = to_str_value(expected[key])

    attrs = extract_attributes(contract_yson=cyson.dumps(contract_data),
                               firm_export_yson=cyson.dumps(some_firm_export),
                               person_yson=EMPTY_YSON,
                               oebs_configs_yson=EMPTY_YSON,
                               currency_codes={})

    result = {}
    for row in attrs:
        assert row['real_contract_id'] == contract.id

        attribute_code = row['attribute_code'].lower()

        attribute_value = get_attribute_value(row)
        if attribute_code == 'services':
            if attribute_value is not None:
                result.setdefault(attribute_code, set()).add(attribute_value)
        else:
            result[attribute_code] = attribute_value

    for key in expected.keys():
        assert result[key] == expected[key]
