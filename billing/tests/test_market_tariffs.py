# coding=utf-8
import pytest

from .base_tariffs import mskdate, BaseTariffTest


service_id = 15
service_cc = "market"

MARKET_TARIFF_MINI = {
    "cc": service_cc + '_api_client_mini',
    "name": "Минимальный",
    "description": '',
    "weight": 20,
    "info_for_table": {
        "month_payment": {"_type": 'money', "value": '20000', "currency": 'RUR'},
        "hits_limit": {"_type": 'number', "value": 11000}
    },
    "service_id": service_id,
    "personal_account": {
        "product": '508202',
        "firm_id": 111,
        "default_paysys": 11101001
    },
    "bay_in_products": [
        {"product_id": "508202", "product_value": "20000"}
    ],
    "attachable_in_ui": True,
    "questionnaire_id": "3412",
    "tarifficator_config": [
        {"unit": "PersonalAccountActivatorUnit", "params": {}},
        {"unit": "StaticLimitsUnit", "params": {"limit_id": service_cc + "_light_hits_daily", "limit": "-1"}},
        {"unit": "StaticLimitsUnit", "params": {"limit_id": service_cc + "_heavy_hits_daily", "limit": "-1"}},
        {"unit": "StaticLimitsUnit", "params": {"limit_id": service_cc + "_special_hits_daily", "limit": "-1"}},
        {"unit": "PrepayPeriodicallyUnit", "params": {
            "period_mask": "0 0 x * * *", "truncate_period_mask": "0 0 * * * *", "time_zone": "Europe/Moscow",
            "product_id": "508202", "product_value": "20000",
            "ban_reason": 104, "unban_reason": 106
        }}
    ]
}

TESTCASE_MARKET_TARIFF_MINI_START_STOP = (
    'TESTCASE_MARKET_TARIFF_MINI_START_STOP',
    {
        "tariff": MARKET_TARIFF_MINI,
        "start_dt": mskdate(2014,  2,  2,  0),
        "end_dt":   mskdate(2014,  5,  6,  0),
        "statistic": {
            mskdate(2014,  2,  3): {"total": 100}
        },
        "payments": {
            mskdate(2014,  2,  3,  1): {"508202": "20000"},
            mskdate(2014,  3,  2,  1): {"508202": "20000"},
            mskdate(2014,  4,  2,  1): {"508202": "19000"},
            mskdate(2014,  4,  5,  1): {"508202": "1000"},
        },
        "checkpoints": {
            mskdate(2014,  2,  3,  0): {
                "ban": True,
                "is_active": True,
                "products__508202__consumed": "0",
            },
            mskdate(2014,  2,  3,  1): {
                "ban": False,
                "is_active": True,
                "products__508202__consumed": "20000",
                "products__508202__next_consume_date": mskdate(2014, 3, 3, 0, 0),
            },
            mskdate(2014,  4,  3,  1): {
                "ban": True,
                "is_active": True,
                "products__508202__consumed": "40000",
                "products__508202__credited": "59000",
            },
            mskdate(2014,  4,  5,  1): {
                "ban": False,
                "is_active": True,
                "products__508202__consumed": "60000",
                "products__508202__next_consume_date": mskdate(2014, 5, 5, 0, 0),
            },
            mskdate(2014,  5,  5,  1): {
                "ban": True,
                "is_active": True,
                "products__508202__consumed": "60000",
                "products__508202__credited": "60000",
            },
        },
        "watchpoints": {
            mskdate(2014,  2,  2,  0): 'start',
            mskdate(2014,  2,  3,  0): 'start2',
            mskdate(2014,  2,  3,  1): '1st payment',
            mskdate(2014,  3,  2,  1): '2nd payment',
            mskdate(2014,  4,  2,  1): '3rd part pay, ban',
            mskdate(2014,  4,  5,  1): '3rd full pay',
            mskdate(2014,  5,  4,  23): 'still payed',
            mskdate(2014,  5,  5,  1): 'ban, not enough funds',
        },
        "skip_rate": 0.8,
    }
)

MARKET_TARIFF_MINI0917 = {
    "cc": service_cc + '_api_client_mini_0917',
    "name": "Минимальный",
    "description": '',
    "weight": 20,
    "info_for_table": {
        "days31_payment": {"_type": 'money', "value": '20000', "currency": 'RUR'},
        "hits_limit": {"_type": 'number', "value": 11000}
    },
    "service_id": service_id,
    "personal_account": {
        "product": '508202',
        "firm_id": 111,
        "default_paysys": 11101001
    },
    "bay_in_products": [
        {"product_id": "508202", "product_value": "20000"}
    ],
    "attachable_in_ui": True,
    "questionnaire_id": "3412",
    "tarifficator_config": [
        {"unit": "PersonalAccountActivatorUnit", "params": {}},
        {"unit": "StaticLimitsUnit", "params": {"limit_id": service_cc + "_light_hits_daily", "limit": "-1"}},
        {"unit": "StaticLimitsUnit", "params": {"limit_id": service_cc + "_heavy_hits_daily", "limit": "-1"}},
        {"unit": "StaticLimitsUnit", "params": {"limit_id": service_cc + "_special_hits_daily", "limit": "-1"}},
        {"unit": "DailyPrepaySeveralDaysUnit", "params": {
            "several_days": 31, "time_zone": "Europe/Moscow",
            "product_id": "508202", "product_value": "20000",
            "ban_reason": 104, "unban_reason": 106
        }}
    ]
}

TESTCASE_MARKET_TARIFF_MINI0917_START_STOP = (
    'TESTCASE_MARKET_TARIFF_MINI0917_START_STOP',
    {
        "tariff": MARKET_TARIFF_MINI0917,
        "start_dt": mskdate(2014,  2,  2,  0),
        "end_dt":   mskdate(2014,  5,  16,  0),
        "statistic": {
            mskdate(2014,  2,  3): {"total": 100}
        },
        "payments": {
            mskdate(2014,  2,  3,  1): {"508202": "20000"},
            mskdate(2014,  3,  2,  1): {"508202": "20000"},
            mskdate(2014,  4,  7,  1): {"508202": "10000"},
        },
        "checkpoints": {
            mskdate(2014,  2,  3,  0): {
                "ban": True,
                "is_active": True,
                "products__508202__consumed": "0",
            },
            mskdate(2014,  2,  3,  1): {
                "ban": False,
                "is_active": True,
                "products__508202__consumed": "645.16",
                "products__508202__next_consume_date": mskdate(2014, 2, 4, 0, 0),
                "products__508202__payed_days": 1,
            },
            mskdate(2014,  4,  3,  1): {
                "ban": False,
                "is_active": True,
                "products__508202__consumed": "38709.68",
                "products__508202__credited": "40000",
                "products__508202__payed_days": 29,
            },
            mskdate(2014,  4,  6,  0): {
                "ban": False,
                "is_active": True,
                "products__508202__consumed": "40000.00",
                "products__508202__next_consume_date": mskdate(2014, 4, 6, 0, 0),
            },
            mskdate(2014,  4,  6,  1): {
                "ban": True,
                "is_active": True,
                "products__508202__consumed": "40000.00",
                "products__508202__next_consume_date": None,
            },
            mskdate(2014,  4,  7,  1): {
                "ban": False,
                "is_active": True,
                "products__508202__consumed": "40645.16",
                "products__508202__credited": "50000",
                "products__508202__next_consume_date": mskdate(2014, 4, 8, 0, 0),
            },
            mskdate(2014,  4, 22,  0): {
                "ban": False,
                "is_active": True,
                "products__508202__consumed": "49677.42",
                "products__508202__credited": "50000",
                "products__508202__next_consume_date": mskdate(2014, 4, 22, 0, 0),
                "products__508202__payed_days": 15,
            },
            mskdate(2014,  4, 22,  1): {
                "ban": True,
                "is_active": True,
                "products__508202__consumed": "49677.42",
                "products__508202__credited": "50000",
            },
        },
        "watchpoints": {
            mskdate(2014,  2,  3,  0): 'start',
            mskdate(2014,  2,  3,  1): '1st payment',
            mskdate(2014,  3,  2,  1): '2nd payment',
            mskdate(2014,  4,  6,  0): 'before ban',
            mskdate(2014,  4,  6,  1): 'ban, not enough funds',
            mskdate(2014,  4,  7,  1): '3rd payment, unban',
            mskdate(2014,  4, 22,  0): 'before ban',
            mskdate(2014,  4, 22,  1): 'ban, not enough funds',
        },
        "skip_rate": 0.8,
    }
)


class TestMarketTariff(BaseTariffTest):
    @pytest.mark.parametrize("case_name, case", [
        TESTCASE_MARKET_TARIFF_MINI_START_STOP,
        TESTCASE_MARKET_TARIFF_MINI0917_START_STOP,
    ])
    def test_market_tariffs(self, case_name, case):
        return self._run_test_case(**case)
