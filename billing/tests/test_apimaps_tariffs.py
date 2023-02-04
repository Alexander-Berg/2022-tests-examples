# coding=utf-8
import pytest

from .base_tariffs import mskdate, BaseTariffTest

service_id = 3
service_cc = "apimaps"

APIMAPS_TARIFF_1000 = {
    "cc": service_cc + "_1000_yearprepay_1000_overhead",
    "name": "до 1 000 запросов в сутки",
    "description": "120 000 рублей предолпата в год за 1 000 запросов в сутки, 120 рублей за 1 000 запросов превышения",
    "service_id": service_id,
    "tarifficator_config": [
        {"unit": "CreditedActivatorUnit", "params": {"product_id": "508206", "needle_credited": "120000"}},
        {"unit": "StaticLimitsUnit", "params": {"limit_id": service_cc + "_geocoder_hits_daily", "limit": "-1"}},
        {"unit": "StaticLimitsUnit", "params": {"limit_id": service_cc + "_router_hits_daily", "limit": "-1"}},
        {"unit": "PrepayPeriodicallyUnit", "params": {
            "period_mask": "0 0 x x * *", "truncate_period_mask": "0 0 * * * *", "time_zone": "Europe/Moscow",
            "product_id": "508206", "product_value": "120000",
            "ban_reason": 1, "unban_reason": 18
        }},
        {"unit": "DailyStatisticRangeConsumerUnit", "params": {
            "product_id": "508229", "product_value": "120", "time_zone": "Europe/Moscow",
            "range_from": "1000", "quantum": "1000", "precision": "1", "round_method": "ceil",
            "statistic_aggregator": "total"
        }},
        {"unit": "BillDateEventUnit", "params": {"days_before_next_consume": "15", "products_filter": ["508206"]}}
    ]
}

APIMAPS_TARIFF_SIMPLE_CONTRACTLESS = {
    "cc": service_cc + "_SIMPLE_CONTRACTLESS",
    "service_id": service_id,
    "personal_account": {
        "product": '6666666', "firm_id": 1, "default_paysys": 1001
    },
    "tarifficator_config": [
        {"unit": "UnconditionalActivatorUnit", "params": {}},
        {"unit": "StaticLimitsUnit", "params": {"limit_id": service_cc + "_requests_daily", "limit": "-1"}},
        {"unit": "StaticLimitsUnit", "params": {"limit_id": service_cc + "_cells_daily", "limit": "-1"}},
        {"unit": "StaticLimitsUnit", "params": {"limit_id": service_cc + "_orders_daily", "limit": "-1"}},
        {"unit": "PrepayPeriodicallyUnit", "params": {
            "period_mask": "0 0 x x * *", "truncate_period_mask": "0 0 * * * *", "time_zone": "Europe/Moscow",
            "product_id": "6666666", "product_value": "120000",
            "ban_reason": 1, "unban_reason": 18
        }},
        {"unit": "TodayPrepayStatisticRangeConsumerUnit", "params": {
            "time_zone": "Europe/Moscow",
            "range_from": "1000", "quantum": "1000", "round_method": "ceil", "precision": "1",
            "skip_when_range_out": True, "include_range_from": False, "statistic_aggregator": "total",
            "limit_statistic_aggregator": "total",
            "product_id": "6666667", "product_value": "120", "autocharge_personal_account": True,
            "ban_reason": 2, "unban_reason": 19
        }}
    ]
}


TESTCASE_APIMAPS_SIMPLE_CONTRACTLESS = (
    "TESTCASE_APIMAPS_SIMPLE_CONTRACTLESS",
    {
        "tariff": APIMAPS_TARIFF_SIMPLE_CONTRACTLESS,
        "start_dt": mskdate(2014,  2,  2,  1, 0),
        "end_dt":   mskdate(2015,  3,  5,  0, 0),
        "statistic": {
            mskdate(2014,  2,  2,  0): {"total":  777},
            mskdate(2014,  2,  2,  2): {"total": 1100},
            mskdate(2014,  2,  2,  3): {"total": 4100},
            mskdate(2014,  2,  2,  4): {"total":  800},
            mskdate(2014,  2,  3,  0): {"total": 1000},
            mskdate(2014,  2,  3,  1): {"total":    1},
            mskdate(2014,  2,  3,  2): {"total": 1000},
            mskdate(2015,  2,  5,  2): {"total": 1001},
            mskdate(2015,  2,  6,  4): {"total": 1001},
        },
        "payments": {
            mskdate(2014,  2,  2,  2): {"6666666": "120000", "6666667": "730"},
            mskdate(2014,  2,  3,  4): {"6666667": "120"},
            mskdate(2015,  2,  5,  1): {"6666666": "120000"},
            mskdate(2015,  2,  6,  3): {"6666667": "110"},
        },
        "checkpoints": {
            mskdate(2014,  2,  2,  1): {
                "ban_reason": 1,
                "ban": True
            },
            mskdate(2014,  2,  2,  2): {
                "products__6666666__consumed": "120000",
                "products__6666667__TodayPrepayStatisticRangeConsumerUnit__uncountable_stat": "777",
                "ban_reason": 18,
                "ban": False
            },
            mskdate(2014,  2,  2, 23): {
                "products__6666666__consumed": "120000",
                "products__6666667__consumed": "600",
                "ban_reason": 18,
                "ban": False
            },
            mskdate(2014,  2,  3,  2): {
                "products__6666667__consumed": "720",
                "ban_reason": 18,
                "ban": False
            },
            mskdate(2014,  2,  3,  3): {
                "products__6666667__consumed": "720",
                "ban_reason": 2,
                "ban": True
            },
            mskdate(2014,  2,  3,  4): {
                "products__6666667__consumed": "840",
                "ban_reason": 19,
                "ban": False
            },
            mskdate(2015,  2,  5,  0): {
                "ban_reason": 1,
                "ban": True
            },
            mskdate(2015,  2,  5,  2): {
                "ban": False
            },
            mskdate(2015,  2,  5,  3): {
                "ban_reason": 2,
                "ban": True
            },
            mskdate(2015,  2,  6,  1): {
                "ban_reason": 19,
                "ban": False
            },
            mskdate(2015,  2,  6,  5): {
                "products__6666667__consumed": "960",
            },
        },
        "watchpoints": {
            mskdate(2014,  2,  2,  0): '---',
            mskdate(2014,  2,  2,  1): '---',
            mskdate(2014, 10, 26,  1): '---',
        },
        "skip_rate": 0.8,
    }
)

TESTCASE_APIMAPS_1000_TWO_YEARS = (
    "TESTCASE_APIMAPS_1000_TWO_YEARS",
    {
        "tariff": APIMAPS_TARIFF_1000,
        "start_dt": mskdate(2014,  2,  2,  0, 0),
        "end_dt":   mskdate(2016,  2,  2,  0, 0),
        "statistic": {
            mskdate(2014,  2,  3): {"total": 100}
        },
        "payments": {
            mskdate(2014,  2,  2,  1): {"508206": "120000"},
            mskdate(2015,  2,  2,  0): {"508206": "120000"}
        },
        "checkpoints": {
            mskdate(2014,  2,  2,  0): {
                "products__508206__credited_deficit": "120000",
                "is_active": False
            },
            mskdate(2014,  2,  2,  1): {
                "products__508206__consumed": "120000",
                "is_active": True
            },
            mskdate(2015,  2,  2,  0): {
                "products__508206__consumed": "120000",
                "is_active": True
            },
            mskdate(2015,  2,  2,  1): {
                "products__508206__consumed": "240000",
                "is_active": True
            }
        },
        "watchpoints": {
            mskdate(2014,  2,  2,  0): '--',
            mskdate(2015,  2,  2,  0): '--',
            mskdate(2016,  2,  2,  0): '--',
            mskdate(2016,  2,  2,  1): '--',
        },
        "skip_rate": 0.8,
    },
)

TESTCASE_APIMAPS_1000_OVERHEAD = (
    "TESTCASE_APIMAPS_1000_OVERHEAD",
    {
        "tariff": APIMAPS_TARIFF_1000,
        "start_dt": mskdate(2015,  2,  2,  0,  0),
        "end_dt":   mskdate(2015,  3,  2,  0,  0),
        "statistic": {
            mskdate(2015,  2,  2,  0,  0): {"total": 1100},
            mskdate(2015,  2,  3,  0,  0): {"total": 1100},
            mskdate(2015,  2,  4,  0,  0): {"total": 1100},
            mskdate(2015,  2,  5,  0,  0): {"total": 1100},
            mskdate(2015,  2,  6,  0,  0): {"total": 1100},
            mskdate(2015,  2,  7,  0,  0): {"total": 1100},
        },
        "payments": {
            mskdate(2015,  2,  2,  0,  0): {"508206": "120000"}
        },
        "checkpoints": {
            mskdate(2015,  2,  2,  0,  0): {
                "products__508206__consumed": "120000",
                "is_active": True
            },
            mskdate(2015,  2,  3,  0,  0): {
                "products__508229__consumed": "120",
            },
            mskdate(2015,  2,  4,  0,  0): {
                "products__508229__consumed": "240",
            },
            mskdate(2015,  2,  8,  0,  0): {
                "products__508229__consumed": "720",
            },
        },
        "watchpoints": {
            mskdate(2015,  2,  2,  0,  0): '--',
            mskdate(2015,  2,  8,  0,  0): '--',
            mskdate(2015,  3,  2,  0,  0): '--',
        },
        "skip_rate": 0.8,
    },
)


class TestApimapsTariff(BaseTariffTest):
    @pytest.mark.parametrize("case_name, case", [
        TESTCASE_APIMAPS_1000_OVERHEAD,
        TESTCASE_APIMAPS_1000_TWO_YEARS,
        TESTCASE_APIMAPS_SIMPLE_CONTRACTLESS,
    ])
    def test_apimaps_tariffs(self, case_name, case):
        return self._run_test_case(**case)
