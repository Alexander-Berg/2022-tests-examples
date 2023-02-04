# coding=utf-8
import pytest

from .base_tariffs import mskdate, BaseTariffTest

service_id = 18
service_cc = "pogoda"

POGODA_TARIFF_MAIN = {
    "service_id": service_id,
    "cc": service_cc + "_main_2018",
    "name": "Основной",
    "description": "20 000 рублей предолпата в месяц.",
    "tarifficator_config": [
        {"unit": "CreditedActivatorUnit", "params": {"product_id": "508206", "needle_credited": "20000"}},
        {"unit": "PrepayPeriodicallyUnit", "params": {
            "period_mask": "0 0 x * * *", "time_zone": "Europe/Moscow",
            "product_id": "508206", "product_value": "20000",
            "ban_reason": 122, "unban_reason": 123
        }},
        {"unit": "PrepaySubscribePeriodicallyRangeConsumerUnit", "params": {
            "period_mask": "0 0 x * * *", "time_zone": "Europe/Moscow",
            "range_from": "2000000", "quantum": "1", "precision": "1", "round_method": "ceil",
            "statistic_aggregator": "hits",
            "subscription_product_id": "508206", "product_id": "508207",  "product_value": "0.01",
            "ban_reason": 122, "unban_reason": 123
        }},
        {"unit": "BillDateEventUnit", "params": {"days_before_next_consume": "7", "products_filter": ["508206"]}}
    ]
}


TESTCASE_POGODA_TARIFF_MAIN = (
    "TESTCASE_POGODA_SIMPLE_CONTRACTLESS",
    {
        "tariff": POGODA_TARIFF_MAIN,
        "start_dt": mskdate(2014,  2,  2,  0, 0),
        "end_dt":   mskdate(2014,  5,  5,  0, 0),
        "statistic": {
            mskdate(2014,  2,  2,  1): {"hits": 2000000},
            mskdate(2014,  2,  2,  2): {"hits": 700},
            mskdate(2014,  2,  3,  0): {"hits": 800},
        },
        "payments": {
            mskdate(2014,  2,  2,  1): {"508206": "20000", "508207": "7"},
            mskdate(2014,  2,  3,  5): {"508206":     "0", "508207": "8"},
            mskdate(2014,  3,  5,  1): {"508206": "20000", "508207": "0"},
        },
        "checkpoints": {
            mskdate(2014,  2,  2,  1): {
                "is_active": True,
                "products__508206__next_consume_date": mskdate(2014,  3,  2,  0),
                "products__508206__consumed": "20000",
            },
            mskdate(2014,  2,  2,  3): {
                "products__508207__consumed": "7.00",
            },
            mskdate(2014,  2,  3,  1): {
                "ban": True,
            },
            mskdate(2014,  2,  3,  5): {
                "ban": False,
                "products__508207__consumed": "15.00",
            },
            mskdate(2014,  3,  2,  1): {
                "ban": True,
            },
            mskdate(2014,  3,  5,  1): {
                "ban": False,
                "products__508206__next_consume_date": mskdate(2014,  4,  5,  0),
                "products__508206__consumed": "40000",
            },
        },
        "watchpoints": {
            mskdate(2014,  2,  2,  0): "START",
            mskdate(2014,  3,  5,  1): 'UNBAN',
            mskdate(2015,  3,  5,  0): "END",
        },
        "skip_rate": 0.8,
    }
)


class TestPogodaTariff(BaseTariffTest):
    @pytest.mark.parametrize("case_name, case", [
        TESTCASE_POGODA_TARIFF_MAIN,
    ])
    def test_pogoda_tariffs(self, case_name, case):
        return self._run_test_case(**case)
