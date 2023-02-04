from datetime import timedelta

from agency_rewards.rewards.common.notifications import get_insert_date
from agency_rewards.rewards.utils.dates import (
    get_quarter_first_day,
    get_quarter_last_day,
    get_previous_quarter_first_day,
    get_previous_quarter_last_day,
    get_first_dt_prev_month,
)
from billing.agency_rewards.tests_platform.common import TestBase
from billing.agency_rewards.tests_platform.common.scheme import rewards_history, rewards


class TestRewardsErrorsDetection(TestBase):
    @classmethod
    def get_prev_month_range(cls, now):
        prev_month_end = (now.replace(day=1, hour=0, minute=0, second=0, microsecond=0) - timedelta(seconds=1)).replace(
            microsecond=0
        )

        prev_month_start = prev_month_end.replace(day=1, hour=0, minute=0, second=0, microsecond=0)
        return prev_month_start, prev_month_end

    @classmethod
    def setup_fixtures(cls, session):
        now = get_insert_date(session)
        past = now - timedelta(weeks=13)
        dt1, dt2 = cls.get_prev_month_range(now)
        q_start = get_quarter_first_day(get_first_dt_prev_month(now))
        q_end = get_quarter_last_day(get_first_dt_prev_month(now))
        prev_q_start = get_previous_quarter_first_day(get_first_dt_prev_month(now))
        prev_q_end = get_previous_quarter_last_day(get_first_dt_prev_month(now))

        reports_history = [
            {
                'contract_id': 44,
                'contract_eid': 'C-44',
                'reward_type': 301,
                'discount_type': 32,
                'turnover_to_pay': 1,
                'turnover_to_charge': 1,
                'reward_to_pay': 1,
                'reward_to_charge': -100,
                'from_dt': q_start,
                'till_dt': q_end,
                'insert_dt': now,
            },
            {
                'contract_id': 43,
                'contract_eid': 'C-43',
                'reward_type': 301,
                'discount_type': 32,
                'turnover_to_pay': 1,
                'turnover_to_charge': 1,
                'reward_to_pay': 1,
                'reward_to_charge': 100,
                'from_dt': q_start,
                'till_dt': q_end,
                'insert_dt': now,
            },
            {
                'contract_id': 45,
                'contract_eid': 'C-45',
                'reward_type': 301,
                'discount_type': 32,
                'turnover_to_pay': 1,
                'turnover_to_charge': 1,
                'reward_to_pay': 1,
                'reward_to_charge': -100,
                'from_dt': prev_q_start,
                'till_dt': prev_q_end,
                'insert_dt': past,
            },
            {
                'contract_id': 46,
                'contract_eid': 'C-46',
                'reward_type': 301,
                'discount_type': 32,
                'turnover_to_pay': 1,
                'turnover_to_charge': 1,
                'reward_to_pay': 1,
                'reward_to_charge': 100,
                'from_dt': prev_q_start,
                'till_dt': prev_q_end,
                'insert_dt': past,
            },
            {
                'contract_id': 42,
                'contract_eid': 'C-42',
                'reward_type': 320,
                'discount_type': 32,
                'turnover_to_pay': 1,
                'turnover_to_charge': 1,
                'reward_to_pay': 1,
                'reward_to_charge': -10,
                'from_dt': q_start,
                'till_dt': q_end,
                'insert_dt': now,
            },
            {
                'contract_id': 41,
                'contract_eid': 'C-41',
                'reward_type': 320,
                'discount_type': 32,
                'turnover_to_pay': 1,
                'turnover_to_charge': 1,
                'reward_to_pay': 1,
                'reward_to_charge': 10,
                'from_dt': q_start,
                'till_dt': q_end,
                'insert_dt': now,
            },
            {
                'contract_id': 40,
                'contract_eid': 'C-40',
                'reward_type': 320,
                'discount_type': 32,
                'turnover_to_pay': 1,
                'turnover_to_charge': 1,
                'reward_to_pay': 1,
                'reward_to_charge': -10,
                'from_dt': prev_q_start,
                'till_dt': prev_q_end,
                'insert_dt': past,
            },
            {
                'contract_id': 39,
                'contract_eid': 'C-39',
                'reward_type': 320,
                'discount_type': 32,
                'turnover_to_pay': 1,
                'turnover_to_charge': 1,
                'reward_to_pay': 1,
                'reward_to_charge': 10,
                'from_dt': prev_q_start,
                'till_dt': prev_q_end,
                'insert_dt': past,
            },
        ]

        session.execute(rewards_history.insert(), reports_history)

        reports = [
            {
                'contract_id': 42,
                'contract_eid': 'C-42',
                'reward_type': 320,
                'discount_type': 32,
                'turnover_to_pay': 1,
                'turnover_to_charge': 1,
                'nds': 0.1,
                'currency': 'RUR',
                'reward_to_pay': 1,
                'reward_to_charge': -10,
                'from_dt': q_start,
                'till_dt': q_end,
                'insert_dt': now,
            },
            {
                'contract_id': 41,
                'contract_eid': 'C-41',
                'reward_type': 320,
                'discount_type': 32,
                'turnover_to_pay': 1,
                'turnover_to_charge': 1,
                'nds': 0.1,
                'currency': 'RUR',
                'reward_to_pay': 1,
                'reward_to_charge': 10,
                'from_dt': dt1,
                'till_dt': dt2,
                'insert_dt': now,
            },
            {
                'contract_id': 40,
                'contract_eid': 'C-40',
                'reward_type': 320,
                'discount_type': 31,
                'turnover_to_pay': 1,
                'turnover_to_charge': 1,
                'nds': 0.1,
                'currency': 'RUR',
                'reward_to_pay': 1,
                'reward_to_charge': -10,
                'from_dt': prev_q_start,
                'till_dt': prev_q_end,
                'insert_dt': past,
            },
            {
                'contract_id': 39,
                'contract_eid': 'C-39',
                'reward_type': 320,
                'discount_type': 31,
                'turnover_to_pay': 1,
                'nds': 0.1,
                'currency': 'RUR',
                'turnover_to_charge': 1,
                'reward_to_pay': 1,
                'reward_to_charge': 10,
                'from_dt': prev_q_start,
                'till_dt': prev_q_end,
                'insert_dt': past,
            },
        ]

        session.execute(rewards.insert(), reports)
