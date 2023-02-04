from datetime import datetime
from typing import Dict

import pytest
from billing.contract_iface import JSONContract

from hamcrest import assert_that, has_properties

from billing.hot.calculators.taxi.calculator.core.constants import CASHLESS_SID, SUBVENTION_SID
from billing.hot.calculators.taxi.calculator.core.entities.firm import Firm
from billing.hot.calculators.taxi.calculator.core.util import to_msk_dt
from billing.hot.calculators.taxi.calculator.tests.builder import (
    gen_firm, gen_general_contract, gen_spendable_contract, gen_tax_policy, gen_tax_policy_pct
)
from billing.hot.calculators.taxi.calculator.tests.const import (
    CLIENT_ID, GENERAL_CONTRACT_ID, PERSON_ID, SPENDABLE_CONTRACT_ID, TAXI_RU_FIRM, UR_PERSON, YT_PERSON
)


class TestFirm:
    tax_policies = [gen_tax_policy(1, 'a1', 'resident', resident=1, default_tax=1, spendable_nds_id=18,
                                   percents=[gen_tax_policy_pct(1, 'a1', to_msk_dt(datetime(2020, 1, 1)), nds_pct=18, hidden=0),
                                             gen_tax_policy_pct(2, 'a2', to_msk_dt(datetime(2020, 2, 1)), nds_pct=16, hidden=1),
                                             gen_tax_policy_pct(3, 'a3', to_msk_dt(datetime(2021, 1, 1)), nds_pct=20, hidden=0)]),
                    gen_tax_policy(2, 'a2', 'nonresident', resident=0, spendable_nds_id=0,
                                   percents=[gen_tax_policy_pct(11, 'b1', to_msk_dt(datetime(2020, 1, 1)), nds_pct=10, hidden=0),
                                             gen_tax_policy_pct(12, 'b2', to_msk_dt(datetime(2020, 2, 1)), nds_pct=16, hidden=1),
                                             gen_tax_policy_pct(13, 'b3', to_msk_dt(datetime(2021, 1, 1)), nds_pct=30, hidden=0)]),
                    gen_tax_policy(1, 'a1', 'resident_other', resident=1, default_tax=0,
                                   percents=[gen_tax_policy_pct(21, 'c1', to_msk_dt(datetime(2020, 1, 1)), nds_pct=0, hidden=0)])
                    ]

    @pytest.mark.parametrize('dt,person_type,expected_policy_name,expected_policy_pct',
                             [(to_msk_dt(datetime(2020, 1, 1)), YT_PERSON, 'nonresident',
                               {'id': 11, 'dt': to_msk_dt(datetime(2020, 1, 1, 0, 0)), 'nds_pct': 10}),
                              (to_msk_dt(datetime(2020, 1, 1)), UR_PERSON, 'resident',
                               {'id': 1, 'dt': to_msk_dt(datetime(2020, 1, 1, 0, 0)), 'nds_pct': 18}),
                              (to_msk_dt(datetime(2020, 2, 2)), UR_PERSON, 'resident',
                               {'id': 1, 'dt': to_msk_dt(datetime(2020, 1, 1, 0, 0)), 'nds_pct': 18}),
                              (to_msk_dt(datetime(2021, 1, 1)), YT_PERSON, 'nonresident',
                               {'id': 13, 'dt': to_msk_dt(datetime(2021, 1, 1, 0, 0)), 'nds_pct': 30}),
                              ])
    def test_tax_policy(self, dt: datetime, person_type: str, expected_policy_name: str, expected_policy_pct: Dict):
        firm = Firm(**gen_firm(TAXI_RU_FIRM, 'some_mdh_id', tax_policies=TestFirm.tax_policies))
        tp = firm.get_tax_policy(firm.get_person_category_info(person_type), dt)
        assert_that(tp, has_properties({'name': expected_policy_name}))
        assert_that(tp.tax_by_date(dt), has_properties(expected_policy_pct))

    @pytest.mark.parametrize('dt,is_spendable,person_type,contract_params,expected_policy_name,expected_policy_pct',
                             [(to_msk_dt(datetime(2020, 1, 1)), False, UR_PERSON, {}, 'resident',
                               {'id': 1, 'dt': to_msk_dt(datetime(2020, 1, 1, 0, 0)), 'nds_pct': 18}),
                              (to_msk_dt(datetime(2020, 1, 1)), False, YT_PERSON, {}, 'nonresident',
                               {'id': 11, 'dt': to_msk_dt(datetime(2020, 1, 1, 0, 0)), 'nds_pct': 10}),
                              (to_msk_dt(datetime(2020, 1, 1)), True, UR_PERSON, {'nds': 0}, 'resident_other',
                               {'id': 21, 'dt': to_msk_dt(datetime(2020, 1, 1, 0, 0)), 'nds_pct': 0}),
                              (to_msk_dt(datetime(2020, 1, 1)), True, UR_PERSON, {'nds': 18}, 'resident',
                               {'id': 1, 'dt': to_msk_dt(datetime(2020, 1, 1, 0, 0)), 'nds_pct': 18}),
                              (to_msk_dt(datetime(2020, 1, 1)), True, YT_PERSON, {'nds': 0}, 'nonresident',
                               {'id': 11, 'dt': to_msk_dt(datetime(2020, 1, 1, 0, 0)), 'nds_pct': 10}),
                              ])
    def test_contract_tax(self, dt: datetime, is_spendable: bool,
                          person_type: str, contract_params: Dict,
                          expected_policy_name: str, expected_policy_pct: Dict):
        firm = Firm(**gen_firm(TAXI_RU_FIRM, 'some_mdh_id', tax_policies=TestFirm.tax_policies))
        if is_spendable:
            contract = JSONContract(contract_data=gen_spendable_contract(SPENDABLE_CONTRACT_ID,
                                                                         CLIENT_ID,
                                                                         PERSON_ID,
                                                                         person_type=person_type,
                                                                         services=[SUBVENTION_SID],
                                                                         **contract_params))
        else:
            contract = JSONContract(contract_data=gen_general_contract(GENERAL_CONTRACT_ID,
                                                                       CLIENT_ID,
                                                                       PERSON_ID,
                                                                       person_type=person_type,
                                                                       services=[CASHLESS_SID],
                                                                       **contract_params))
        tp = firm.get_tax_policy_by_contract_state(contract.current_signed(), dt)
        assert_that(tp, has_properties({'name': expected_policy_name}))
        assert_that(tp.tax_by_date(dt), has_properties(expected_policy_pct))
