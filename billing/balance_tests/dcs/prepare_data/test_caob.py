# coding: utf-8

import collections
import datetime as dt

import mock
import pytest

from balance import muzzle_util as ut, constants
from balance.actions.dcs.utils.common import to_str_value

from tests.balance_tests.dcs.dcs_common import create_contract
from tests.balance_tests.dcs.prepare_data.common import run_prepare_data_for_caob

OEBS_CONTRACT_TYPE4TAXI_CORP = 81

@pytest.fixture
def bu_contracts_start_dt_map():
    return dict()


@pytest.fixture
def bu_start_dt_mock(bu_contracts_start_dt_map):
    patch_path = 'balance.actions.dcs.compare.caob._get_bu_contracts_start_dt_map'
    with mock.patch(patch_path, side_effect=lambda: bu_contracts_start_dt_map) as mock_:
        yield mock_


@pytest.mark.usefixtures('bu_start_dt_mock')
class TestContractAttributes(object):
    Case = collections.namedtuple('Case', 'contract_type contract_kwargs expected')

    @staticmethod
    def get_attribute_value(row):
        attribute_code = row['attribute_code']
        if attribute_code == 'SERVICES' and row['value_num'] == '1':
            return row['attribute_key']

        for key in ('value_num', 'value_str', 'value_dt'):
            value = row[key]
            if value is not None:
                break
        return value

    @pytest.mark.parametrize('case', [
        Case(contract_type='GENERAL', contract_kwargs=None, expected={'commission': 0}),
        Case(contract_type='SPENDABLE',
             contract_kwargs={'is_offer': 1, 'services': [constants.ServiceId.TAXI_CORP, ]},
             expected={'commission': OEBS_CONTRACT_TYPE4TAXI_CORP}),
    ], ids=lambda case: case.contract_type)
    def test_contract_attributes(self, session, case):
        contract_kwargs = case.contract_kwargs
        if contract_kwargs is None:
            contract_kwargs = dict()

        contract = create_contract(session, ctype=case.contract_type, **contract_kwargs)
        session.add(contract)
        session.flush()

        expected = {
            'contract_id': contract.id,
            'contract_eid': contract.external_id,
            'start_dt': ut.trunc_date(contract.col0.dt),
            'is_offer': int(contract.is_offer),
            'person_id': 'P{}'.format(contract.person_id),
            'payment_type': contract.current.payment_type,
            'manager_code': contract.current.manager_code,
            'services': contract.current.services,
            'currency_code': 'RUB',
            'org_id': '121',
        }
        if hasattr(contract.current, 'finish_dt'):
            finish_dt = ut.trunc_date(contract.current.finish_dt) - dt.timedelta(days=1)
            expected['finish_dt'] = finish_dt

        if case.expected is not None:
            expected.update(case.expected)

        for key in expected.iterkeys():
            if key == 'services':
                continue
            expected[key] = to_str_value(expected[key])

        result = {}
        for row in run_prepare_data_for_caob(contract.id):
            assert row['real_contract_id'] == contract.id

            attribute_code = row['attribute_code'].lower()

            attribute_value = self.get_attribute_value(row)
            if attribute_code == 'services':
                if attribute_value is not None:
                    result.setdefault(attribute_code, set()).add(attribute_value)
            else:
                result[attribute_code] = attribute_value

        assert result == expected

    def test_print_form_dt(self, session):
        contract_kwargs = {
            'ctype': 'GENERAL',
            'services': [constants.ServiceId.TAXI_CASH, ],
            'start_dt': dt.datetime(2000, 1, 1),
            'print_form_dt': dt.datetime(2000, 6, 1),
        }
        contract = create_contract(session, **contract_kwargs)
        expected = dt.date(2000, 6, 1).strftime('%Y-%m-%d')

        for row in run_prepare_data_for_caob(contract.id):
            attribute_code = row['attribute_code'].lower()
            if attribute_code == 'start_dt':
                attribute_value = self.get_attribute_value(row)
                assert attribute_value == expected
                break
        else:
            assert False, 'not found start_dt attribute'

    def test_tail_time(self, session):
        contract_kwargs = {
            'ctype': 'DISTRIBUTION',
            'services': [constants.ServiceId.DISTRIBUTION, ],
            'end_dt': dt.datetime(2000, 1, 1),
            'tail_time': 6,
        }
        contract = create_contract(session, **contract_kwargs)
        expected = dt.date(2000, 7, 1).strftime('%Y-%m-%d')

        for row in run_prepare_data_for_caob(contract.id):
            attribute_code = row['attribute_code'].lower()
            if attribute_code == 'finish_dt':
                attribute_value = self.get_attribute_value(row)
                assert attribute_value == expected
                break
        else:
            assert False, 'not found finish_dt attribute'


@pytest.mark.usefixtures('bu_start_dt_mock')
def test_not_comparable(session):
    min_finish_dt = ut.trunc_date(dt.datetime.now())
    contract_finish_dt = min_finish_dt - dt.timedelta(days=1)

    contract = create_contract(session, finish_dt=contract_finish_dt)
    session.add(contract)
    session.flush()

    rows = run_prepare_data_for_caob(contract.id, min_finish_dt)
    assert len(rows) == 0


@pytest.mark.usefixtures('bu_start_dt_mock')
def test_bu_contract_correct_start_dt(session, bu_contracts_start_dt_map):
    balance_value = dt.datetime(2000, 1, 1)
    oebs_value = dt.datetime(2000, 2, 2)

    contract = create_contract(session, dt=balance_value)
    session.add(contract)
    session.flush()

    bu_contracts_start_dt_map[contract.id] = oebs_value
    for row in run_prepare_data_for_caob(contract.id):
        if row['attribute_code'] == 'START_DT':
            assert row['value_dt'] == to_str_value(oebs_value)
            break
    else:
        assert False
