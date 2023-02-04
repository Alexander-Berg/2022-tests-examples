import os
from datetime import datetime, timedelta
from decimal import Decimal

from agency_rewards.rewards.scheme import runs
from agency_rewards.rewards.utils.dates import (
    get_quarter_first_day,
    get_quarter_last_day,
    get_first_dt_prev_month,
)
from billing.agency_rewards.tests_platform.common import TestBase
from billing.agency_rewards.tests_platform.common.scheme import rewards_history


class TestReportDifferences(TestBase):
    @classmethod
    def get_prev_month_range(cls, now):
        prev_month_end = (now.replace(day=1, hour=0, minute=0, second=0, microsecond=0) - timedelta(seconds=1)).replace(
            microsecond=0
        )

        prev_month_start = prev_month_end.replace(day=1, hour=0, minute=0, second=0, microsecond=0)
        return prev_month_start, prev_month_end

    @classmethod
    def setup_fixtures(cls, session):
        def create_fake_t_ar_run(insert_dt):
            run = {
                'run_dt': insert_dt.date(),
                'insert_dt': insert_dt,
                'start_dt': insert_dt,
                'finish_dt': insert_dt + timedelta(minutes=3),
                'type': 'calc',
            }
            session.execute(runs.insert(), run)

        now = datetime.strptime(os.getenv('YA_AR_INSERT_DT'), '%Y.%m.%d %H:%M:%S')
        past = now - timedelta(minutes=10)
        too_early = now - timedelta(minutes=20)
        too_early_last_day = too_early - timedelta(days=1)
        for dt in (past, too_early, too_early_last_day):
            create_fake_t_ar_run(dt)
        dt1, dt2 = cls.get_prev_month_range(now)
        q_start = get_quarter_first_day(get_first_dt_prev_month(now))
        q_end = get_quarter_last_day(get_first_dt_prev_month(now))

        reports = [
            # same records
            {
                'contract_id': 1,
                'contract_eid': 'C-1',
                'reward_type': 310,
                'discount_type': 1,
                'turnover_to_pay': 1,
                'turnover_to_charge': 1,
                'reward_to_pay': 1,
                'reward_to_charge': 1,
                'from_dt': dt1,
                'till_dt': dt2,
                'insert_dt': now,
            },
            {
                'contract_id': 1,
                'contract_eid': 'C-1',
                'reward_type': 310,
                'discount_type': 1,
                'turnover_to_pay': 1,
                'turnover_to_charge': 1,
                'reward_to_pay': 1,
                'reward_to_charge': 1,
                'from_dt': dt1,
                'till_dt': dt2,
                'insert_dt': past,
            },
            {
                'contract_id': 1,
                'contract_eid': 'C-1',
                'reward_type': 310,
                'discount_type': 1,
                'turnover_to_pay': 1,
                'turnover_to_charge': 1,
                'reward_to_pay': 1,
                'reward_to_charge': 1,
                'from_dt': dt1,
                'till_dt': dt2,
                'insert_dt': too_early,
            },
            # different reward_to_pay
            {
                'contract_id': 1,
                'contract_eid': 'C-1',
                'reward_type': 312,
                'discount_type': 1,
                'turnover_to_pay': 1,
                'turnover_to_charge': 1,
                'reward_to_pay': 10,
                'reward_to_charge': 1,
                'from_dt': dt1,
                'till_dt': dt2,
                'insert_dt': now,
            },
            {
                'contract_id': 1,
                'contract_eid': 'C-1',
                'reward_type': 312,
                'discount_type': 1,
                'turnover_to_pay': 1,
                'turnover_to_charge': 1,
                'reward_to_pay': 20,
                'reward_to_charge': 1,
                'from_dt': dt1,
                'till_dt': dt2,
                'insert_dt': past,
            },
            {
                'contract_id': 1,
                'contract_eid': 'C-1',
                'reward_type': 312,
                'discount_type': 1,
                'turnover_to_pay': 1,
                'turnover_to_charge': 1,
                'reward_to_pay': 10,
                'reward_to_charge': 1,
                'from_dt': dt1,
                'till_dt': dt2,
                'insert_dt': too_early,
            },
            # different discount_type (one is null, other is not)
            {
                'contract_id': 1,
                'contract_eid': 'C-1',
                'reward_type': 312,
                'discount_type': 3,
                'turnover_to_pay': 1,
                'turnover_to_charge': 1,
                'reward_to_pay': 10,
                'reward_to_charge': 1,
                'from_dt': dt1,
                'till_dt': dt2,
                'insert_dt': now,
            },
            {
                'contract_id': 1,
                'contract_eid': 'C-1',
                'reward_type': 312,
                'discount_type': None,
                'turnover_to_pay': 1,
                'turnover_to_charge': 1,
                'reward_to_pay': 10,
                'reward_to_charge': 1,
                'from_dt': dt1,
                'till_dt': dt2,
                'insert_dt': past,
            },
            # different turnover to charge
            {
                'contract_id': 2,
                'contract_eid': 'C-2',
                'reward_type': 312,
                'discount_type': 3,
                'turnover_to_pay': 1,
                'turnover_to_charge': 1,
                'reward_to_pay': 20,
                'reward_to_charge': 1,
                'from_dt': dt1,
                'till_dt': dt2,
                'insert_dt': now,
            },
            {
                'contract_id': 2,
                'contract_eid': 'C-2',
                'reward_type': 312,
                'discount_type': 3,
                'turnover_to_pay': 1,
                'turnover_to_charge': 2,
                'reward_to_pay': 20,
                'reward_to_charge': 1,
                'from_dt': dt1,
                'till_dt': dt2,
                'insert_dt': past,
            },
            # differences but too old
            {
                'contract_id': 2,
                'contract_eid': 'C-2',
                'reward_type': 312,
                'discount_type': 3,
                'turnover_to_pay': 1,
                'turnover_to_charge': 1,
                'reward_to_pay': 20,
                'reward_to_charge': 1,
                'from_dt': dt1,
                'till_dt': dt2,
                'insert_dt': too_early,
            },
            {
                'contract_id': 2,
                'contract_eid': 'C-2',
                'reward_type': 312,
                'discount_type': 3,
                'turnover_to_pay': 1,
                'turnover_to_charge': 2,
                'reward_to_pay': 20,
                'reward_to_charge': 1,
                'from_dt': dt1,
                'till_dt': dt2,
                'insert_dt': too_early_last_day,
            },
            # disappeared records
            {
                'contract_id': 10,
                'contract_eid': 'C-10',
                'reward_type': 314,
                'discount_type': 10,
                'turnover_to_pay': 1,
                'turnover_to_charge': 1,
                'reward_to_pay': 1,
                'reward_to_charge': 1,
                'from_dt': dt1,
                'till_dt': dt2,
                'insert_dt': past,
            },
            {
                'contract_id': 11,
                'contract_eid': 'C-11',
                'reward_type': 314,
                'discount_type': None,
                'turnover_to_pay': 1,
                'turnover_to_charge': 1,
                'reward_to_pay': 1,
                'reward_to_charge': 1,
                'from_dt': dt1,
                'till_dt': dt2,
                'insert_dt': past,
            },
            # new records
            {
                'contract_id': 12,
                'contract_eid': 'C-12',
                'reward_type': 315,
                'discount_type': 12,
                'turnover_to_pay': 1,
                'turnover_to_charge': 1,
                'reward_to_pay': 1,
                'reward_to_charge': 1,
                'from_dt': dt1,
                'till_dt': dt2,
                'insert_dt': now,
            },
            {
                'contract_id': 13,
                'contract_eid': 'C-13',
                'reward_type': 315,
                'discount_type': None,
                'turnover_to_pay': 1,
                'turnover_to_charge': 1,
                'reward_to_pay': 1,
                'reward_to_charge': 1,
                'from_dt': dt1,
                'till_dt': dt2,
                'insert_dt': now,
            },
            # previous quarter, different reward to pay
            {
                'contract_id': 15,
                'contract_eid': 'C-15',
                'reward_type': 400,
                'discount_type': 12,
                'turnover_to_pay': 1,
                'turnover_to_charge': 1,
                'reward_to_pay': 1,
                'reward_to_charge': 1,
                'from_dt': q_start,
                'till_dt': q_end,
                'insert_dt': now,
            },
            {
                'contract_id': 15,
                'contract_eid': 'C-15',
                'reward_type': 400,
                'discount_type': 12,
                'turnover_to_pay': 1,
                'turnover_to_charge': 1,
                'reward_to_pay': 100,
                'reward_to_charge': 1,
                'from_dt': q_start,
                'till_dt': q_end,
                'insert_dt': past,
            },
            # previous quarter, new record
            {
                'contract_id': 16,
                'contract_eid': 'C-16',
                'reward_type': 400,
                'discount_type': 12,
                'turnover_to_pay': 1,
                'turnover_to_charge': 1,
                'reward_to_pay': 1,
                'reward_to_charge': 1,
                'from_dt': q_start,
                'till_dt': q_end,
                'insert_dt': now,
            },
            # previous quarter, disappeared record
            {
                'contract_id': 17,
                'contract_eid': 'C-17',
                'reward_type': 500,
                'discount_type': None,
                'turnover_to_pay': 1,
                'turnover_to_charge': 1,
                'reward_to_pay': 1,
                'reward_to_charge': 1,
                'from_dt': q_start,
                'till_dt': q_end,
                'insert_dt': past,
            },
            # есть расхождение после округления
            {
                'contract_id': 20,
                'contract_eid': 'C-1',
                'reward_type': 310,
                'discount_type': 7,
                'turnover_to_pay': Decimal('100.2349'),
                'turnover_to_charge': 1,
                'reward_to_pay': 1,
                'reward_to_charge': 1,
                'from_dt': dt1,
                'till_dt': dt2,
                'insert_dt': now,
            },
            {
                'contract_id': 20,
                'contract_eid': 'C-1',
                'reward_type': 310,
                'discount_type': 7,
                'turnover_to_pay': Decimal('100.2249'),
                'turnover_to_charge': 1,
                'reward_to_pay': 1,
                'reward_to_charge': 1,
                'from_dt': dt1,
                'till_dt': dt2,
                'insert_dt': past,
            },
            # нет расхождения после округления
            {
                'contract_id': 21,
                'contract_eid': 'C-1',
                'reward_type': 310,
                'discount_type': 7,
                'turnover_to_pay': Decimal('100.2349'),
                'turnover_to_charge': 1,
                'reward_to_pay': 1,
                'reward_to_charge': 1,
                'from_dt': dt1,
                'till_dt': dt2,
                'insert_dt': now,
            },
            {
                'contract_id': 21,
                'contract_eid': 'C-1',
                'reward_type': 310,
                'discount_type': 7,
                'turnover_to_pay': Decimal('100.2291'),
                'turnover_to_charge': 1,
                'reward_to_pay': 1,
                'reward_to_charge': 1,
                'from_dt': dt1,
                'till_dt': dt2,
                'insert_dt': past,
            },
        ]

        session.execute(rewards_history.insert(), reports)
