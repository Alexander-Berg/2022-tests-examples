# coding=utf-8
import pytest

from .base_tariffs import mskdate, BaseTariffTest


YEARLY_PERIODICAL_CONSUME_TARIFF = {
    "tarifficator_config": [
        {"unit": "PersonalAccountActivatorUnit", "params": {}},
        {"unit": "PeriodicalConsumerUnit", "params": {
            "period_mask": "0 0 x x * *", "time_zone": "Europe/Moscow",
            "product_id": "508206", "product_value": "100000",
        }},
    ]
}


TESTCASE_YEARLY_PERIODICAL_CONSUME_TARIFF = (
    'TESTCASE_YEARLY_PERIODICAL_CONSUME_TARIFF',
    {
        "tariff": YEARLY_PERIODICAL_CONSUME_TARIFF,
        "start_dt": mskdate(2014,  2,  2,  0),
        "end_dt":   mskdate(2016,  5,  6,  0),
        "statistic": {
        },
        "payments": {
        },
        "checkpoints": {
            mskdate(2014,  2,  2,  0): {
                "is_active": True,
                "products__508206__consumed": "100000",
                "products__508206__next_consume_date": mskdate(2015, 2, 2, 0, 0),
            },
            mskdate(2015,  2,  2,  0): {
                "is_active": True,
                "products__508206__consumed": "100000",
                "products__508206__next_consume_date": mskdate(2015, 2, 2, 0, 0),
            },
            mskdate(2015,  2,  2,  1): {
                "is_active": True,
                "products__508206__consumed": "200000",
                "products__508206__next_consume_date": mskdate(2016, 2, 2, 0, 0),
            },
            mskdate(2016,  2,  2,  1): {
                "is_active": True,
                "products__508206__consumed": "300000",
                "products__508206__next_consume_date": mskdate(2017, 2, 2, 0, 0),
            },
        },
        "watchpoints": {
            mskdate(2014,  2,  2,  0): 'start',
            mskdate(2016,  5,  6,  0): 'end',
        },
        "skip_rate": 0.8,
    }
)


MONTHLY_PERIODICAL_CONSUME_TARIFF = {
    "tarifficator_config": [
        {"unit": "PersonalAccountActivatorUnit", "params": {}},
        {"unit": "PeriodicalConsumerUnit", "params": {
            "period_mask": "0 0 x * * *", "time_zone": "Europe/Moscow",
            "product_id": "508206", "product_value": "100001.111",
        }},
    ]
}


TESTCASE_MONTHLY_PERIODICAL_CONSUME_TARIFF = (
    'TESTCASE_MONTHLY_PERIODICAL_CONSUME_TARIFF',
    {
        "tariff": MONTHLY_PERIODICAL_CONSUME_TARIFF,
        "start_dt": mskdate(2014,  2,  2,  0),
        "end_dt":   mskdate(2014,  5,  6,  0),
        "statistic": {
        },
        "payments": {
        },
        "checkpoints": {
            mskdate(2014,  2,  2,  0): {
                "is_active": True,
                "products__508206__consumed": "100001.111",
                "products__508206__next_consume_date": mskdate(2014, 3, 2, 0, 0),
            },
            mskdate(2014,  3,  2,  0): {
                "is_active": True,
                "products__508206__consumed": "100001.111",
                "products__508206__next_consume_date": mskdate(2014, 3, 2, 0, 0),
            },
            mskdate(2014,  3,  2,  1): {
                "is_active": True,
                "products__508206__consumed": "200002.222",
                "products__508206__next_consume_date": mskdate(2014, 4, 2, 0, 0),
            },
            mskdate(2014,  4,  2,  1): {
                "is_active": True,
                "products__508206__consumed": "300003.333",
                "products__508206__next_consume_date": mskdate(2014, 5, 2, 0, 0),
            },
            mskdate(2014, 5,  2,  1): {
                "is_active": True,
                "products__508206__consumed": "400004.444",
                "products__508206__next_consume_date": mskdate(2014, 6, 2, 0, 0),
            },
        },
        "watchpoints": {
            mskdate(2014,  2,  2,  0): 'start',
            mskdate(2014,  5,  6,  0): 'end',
        },
        "skip_rate": 0,
    }
)


class TestPeriodicalConsumeTariff(BaseTariffTest):
    @pytest.mark.parametrize("case_name, case", [
        TESTCASE_YEARLY_PERIODICAL_CONSUME_TARIFF,
        TESTCASE_MONTHLY_PERIODICAL_CONSUME_TARIFF
    ])
    def test_market_tariffs(self, case_name, case):
        return self._run_test_case(**case)
