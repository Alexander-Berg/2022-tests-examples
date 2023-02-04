# coding: utf-8

import collections
import datetime as dt

import pytest

from balance import constants
from balance.actions.dcs.compare.ccaob import to_str_value

from tests.balance_tests.dcs.dcs_common import create_contract
from tests.balance_tests.dcs.prepare_data.common import run_prepare_data_for_ccaob

OEBS_CONTRACT_TYPE4TAXI_CORP = 81

class TestCollateralAttributes(object):
    Case = collections.namedtuple('Case', 'contract_type contract_kwargs expected')

    @staticmethod
    def get_attribute_value(row):
        attribute_code = row['attribute_code']
        value = row['value']

        if attribute_code == 'SERVICES' and int(value) == 1:
            return row['attribute_key']
        return value

    @pytest.mark.parametrize('case', [
        Case(contract_type='GENERAL', contract_kwargs=None, expected={'contract_type': 0}),
        Case(contract_type='SPENDABLE',
             contract_kwargs={'is_offer': 1, 'services': [constants.ServiceId.TAXI_CORP, ]},
             expected={'contract_type': OEBS_CONTRACT_TYPE4TAXI_CORP}),
    ], ids=lambda case: case.contract_type)
    def test_collateral_attributes(self, session, case):
        contract_kwargs = case.contract_kwargs
        if contract_kwargs is None:
            contract_kwargs = dict()

        contract = create_contract(session, ctype=case.contract_type, **contract_kwargs)
        session.add(contract)
        session.flush()

        col0 = contract.col0

        expected = {
            'collateral_id': col0.id,
            'contract_id': contract.id,
            'is_signed': bool(col0.is_signed),
            'is_faxed': bool(col0.is_faxed),
            'is_cancelled': bool(col0.is_cancelled),
            'collateral_type_id': col0.collateral_type_id or 0,
            'start_dt': col0.dt.replace(microsecond=0),
            'services': contract.current.services,
            'contract_type': 0,
            'org_id': 121,
        }

        if hasattr(contract.current, 'finish_dt'):
            finish_dt = contract.current.finish_dt.replace(microsecond=0) - dt.timedelta(days=1)
            expected['finish_dt'] = finish_dt

        if contract.type == 'GENERAL':
            expected['payment_type'] = contract.current.payment_type

        if case.expected is not None:
            expected.update(case.expected)

        for key in expected.iterkeys():
            if key == 'services':
                continue
            expected[key] = to_str_value(expected[key], col0.id, key)

        result = {}
        for row in run_prepare_data_for_ccaob(contract.id):
            assert row['contract_id'] == contract.id
            assert row['collateral_id'] == col0.id

            attribute_code = row['attribute_code'].lower()

            attribute_value = self.get_attribute_value(row)
            if attribute_code == 'services':
                result.setdefault(attribute_code, set()).add(attribute_value)
            else:
                result[attribute_code] = attribute_value

        assert result == expected
