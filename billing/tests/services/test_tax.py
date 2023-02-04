from datetime import datetime
from decimal import Decimal

import hamcrest as hm
import pytest
from billing.contract_iface import JSONContract
from dataclasses import asdict

from billing.library.python.calculator.models.firm import FirmModel
from billing.library.python.calculator.services.tax import TaxService
from billing.library.python.calculator.test_utils.builder import (
    gen_tax_policy, gen_tax_policy_pct, gen_firm,
    gen_general_contract, gen_spendable_contract,
)
from billing.library.python.calculator.util import to_msk_dt
from billing.library.python.calculator.values import PersonType


class TestTaxService:
    FIRM_ID = 13
    CLIENT_ID = 11
    PERSON_ID = 66

    FIRST_SID = 1
    SECOND_SID = 2

    SPENDABLE_CONTRACT_ID = 10
    GENERAL_CONTRACT_ID = 20

    tax_policies = [gen_tax_policy(1, 'a1', 'resident', resident=1, default_tax=1, spendable_nds_id=18,
                                   percents=[
                                       gen_tax_policy_pct(
                                           1,
                                           'a1',
                                           to_msk_dt(datetime(2020, 1, 1)),
                                           nds_pct=Decimal('18.00'),
                                           hidden=0,
                                       ),
                                       gen_tax_policy_pct(
                                           2,
                                           'a2',
                                           to_msk_dt(datetime(2020, 2, 1)),
                                           nds_pct=Decimal('16.00'),
                                           hidden=1,
                                       ),
                                       gen_tax_policy_pct(
                                           3,
                                           'a3',
                                           to_msk_dt(datetime(2021, 1, 1)),
                                           nds_pct=Decimal('20.00'),
                                           hidden=0
                                       ),
                                   ]),
                    gen_tax_policy(2, 'a2', 'nonresident', resident=0, spendable_nds_id=0,
                                   percents=[
                                       gen_tax_policy_pct(
                                           11,
                                           'b1',
                                           to_msk_dt(datetime(2020, 1, 1)),
                                           nds_pct=Decimal('10.00'),
                                           hidden=0,
                                       ),
                                       gen_tax_policy_pct(
                                           12,
                                           'b2',
                                           to_msk_dt(datetime(2020, 2, 1)),
                                           nds_pct=Decimal('16.00'),
                                           hidden=1,
                                       ),
                                       gen_tax_policy_pct(
                                           13,
                                           'b3',
                                           to_msk_dt(datetime(2021, 1, 1)),
                                           nds_pct=Decimal('30.00'),
                                           hidden=0,
                                       )
                                   ]),
                    gen_tax_policy(1, 'a1', 'resident_other', resident=1, default_tax=0,
                                   percents=[
                                       gen_tax_policy_pct(
                                           21,
                                           'c1',
                                           to_msk_dt(datetime(2020, 1, 1)),
                                           nds_pct=Decimal('0.00'),
                                           hidden=0,
                                       )
                                   ]),
                    ]

    @pytest.mark.parametrize('dt,person_type,expected_policy_name,expected_policy_pct', [
        (to_msk_dt(datetime(2020, 1, 1)), PersonType.YT, 'nonresident',
         {'id': 11, 'dt': to_msk_dt(datetime(2020, 1, 1, 0, 0)), 'nds_pct': 10}),
        (to_msk_dt(datetime(2020, 1, 1)), PersonType.UR, 'resident',
         {'id': 1, 'dt': to_msk_dt(datetime(2020, 1, 1, 0, 0)), 'nds_pct': 18}),
        (to_msk_dt(datetime(2020, 2, 2)), PersonType.UR, 'resident',
         {'id': 1, 'dt': to_msk_dt(datetime(2020, 1, 1, 0, 0)), 'nds_pct': 18}),
        (to_msk_dt(datetime(2021, 1, 1)), PersonType.YT, 'nonresident',
         {'id': 13, 'dt': to_msk_dt(datetime(2021, 1, 1, 0, 0)), 'nds_pct': 30}),
    ])
    def test_tax_policy(
        self,
        dt: datetime,
        person_type: PersonType,
        expected_policy_name: str,
        expected_policy_pct: dict,
    ):
        firm = FirmModel(**gen_firm(
            TestTaxService.FIRM_ID,
            'some_mdh_id',
            tax_policies=TestTaxService.tax_policies,
        ))

        tp = TaxService.tax_policy(firm, firm.person_category_by_type(person_type), dt)

        hm.assert_that(asdict(tp), hm.has_entries({'name': expected_policy_name}))
        hm.assert_that(asdict(TaxService.tax_by_date(tp, dt)), hm.has_entries(expected_policy_pct))

    @pytest.mark.parametrize('dt,is_spendable,person_type,contract_params,expected_policy_name,expected_policy_pct', [
        (to_msk_dt(datetime(2020, 1, 1)), False, PersonType.UR, {}, 'resident',
         {'id': 1, 'dt': to_msk_dt(datetime(2020, 1, 1, 0, 0)), 'nds_pct': 18}),
        (to_msk_dt(datetime(2020, 1, 1)), False, PersonType.YT, {}, 'nonresident',
         {'id': 11, 'dt': to_msk_dt(datetime(2020, 1, 1, 0, 0)), 'nds_pct': 10}),
        (to_msk_dt(datetime(2020, 1, 1)), True, PersonType.UR, {'nds': 0}, 'resident_other',
         {'id': 21, 'dt': to_msk_dt(datetime(2020, 1, 1, 0, 0)), 'nds_pct': 0}),
        (to_msk_dt(datetime(2020, 1, 1)), True, PersonType.UR, {'nds': 18}, 'resident',
         {'id': 1, 'dt': to_msk_dt(datetime(2020, 1, 1, 0, 0)), 'nds_pct': 18}),
        (to_msk_dt(datetime(2020, 1, 1)), True, PersonType.YT, {'nds': 0}, 'nonresident',
         {'id': 11, 'dt': to_msk_dt(datetime(2020, 1, 1, 0, 0)), 'nds_pct': 10}),
    ])
    def test_contract_tax(
        self,
        dt: datetime,
        is_spendable: bool,
        person_type: PersonType,
        contract_params: dict,
        expected_policy_name: str,
        expected_policy_pct: dict,
    ):
        firm = FirmModel(**gen_firm(
            TestTaxService.FIRM_ID,
            'some_mdh_id',
            tax_policies=TestTaxService.tax_policies,
        ))

        if is_spendable:
            contract = JSONContract(contract_data=gen_spendable_contract(
                TestTaxService.SPENDABLE_CONTRACT_ID,
                TestTaxService.CLIENT_ID,
                TestTaxService.PERSON_ID,
                firm=TestTaxService.FIRM_ID,
                person_type=person_type,
                services=[TestTaxService.FIRST_SID],
                **contract_params),
            )
        else:
            contract = JSONContract(contract_data=gen_general_contract(
                TestTaxService.GENERAL_CONTRACT_ID,
                TestTaxService.CLIENT_ID,
                TestTaxService.PERSON_ID,
                firm=TestTaxService.FIRM_ID,
                person_type=person_type,
                services=[TestTaxService.SECOND_SID],
                **contract_params),
            )

        tp = TaxService.tax_policy_by_contract_state(firm, contract.current_signed(), dt)

        hm.assert_that(asdict(tp), hm.has_entries({'name': expected_policy_name}))
        hm.assert_that(asdict(TaxService.tax_by_date(tp, dt)), hm.has_entries(expected_policy_pct))
