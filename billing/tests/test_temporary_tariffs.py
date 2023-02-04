# coding=utf-8
import pytest

from .base_tariffs import mskdate, BaseTariffTest


service_id = 666
service_cc = "temp"

TEMP_TARIFF_21 = {
    "cc": service_cc + '_temp_21_days',
    "name": "21 days",
    "description": '',
    "service_id": service_id,
    "attachable_in_ui": True,
    "tarifficator_config": [
        {"unit": "TemporaryActivatorUnit", "params": {
            "days": 21, "period_mask": "",
            "ban_reason": 104, "unban_reason": 106
        }}
    ]
}


TEMP_TARIFF_MONTH = {
    "cc": service_cc + '_temp_month',
    "name": "month",
    "description": '',
    "service_id": service_id,
    "attachable_in_ui": True,
    "tarifficator_config": [
        {"unit": "TemporaryActivatorUnit", "params": {
            "period_mask": "0 0 x * * *", "time_zone": "Europe/Moscow",
            "ban_reason": 104, "unban_reason": 106
        }}
    ]
}


TESTCASE_TEMP_TARIFF_21_START_STOP = (
    'TESTCASE_TEMP_TARIFF_21_START_STOP',
    {
        "tariff": TEMP_TARIFF_21,
        "start_dt": mskdate(2014,  2,  2,  0),
        "end_dt":   mskdate(2014,  2, 24,  0),
        "statistic": {
        },
        "payments": {
        },
        "checkpoints": {
            mskdate(2014,  2,  2,  0): {
                "ban": None,
                "is_active": True,
            },
            mskdate(2014,  2, 23,  0): {
                "ban": None,
                "is_active": True,
            },
            mskdate(2014,  2, 23,  1): {
                "ban": True,
                "is_active": True,
            },
        },
        "watchpoints": {
            mskdate(2014,  2,  2,  0): '1',
            mskdate(2014,  2,  3,  0): '2',
            mskdate(2014,  2,  3,  1): '3',
            mskdate(2014,  2, 23,  0): '4',
            mskdate(2014,  2, 23,  1): '5',
        },
        "skip_rate": 0.8,
    }
)


TESTCASE_TEMP_TARIFF_MONTH_START_STOP = (
    'TESTCASE_TEMP_TARIFF_MONTH_START_STOP',
    {
        "tariff": TEMP_TARIFF_MONTH,
        "start_dt": mskdate(2014,  2,  2,  0),
        "end_dt":   mskdate(2014,  3,  5,  0),
        "statistic": {
        },
        "payments": {
        },
        "checkpoints": {
            mskdate(2014,  2,  2,  0): {
                "ban": None,
                "is_active": True,
            },
            mskdate(2014,  3,  2,  0): {
                "ban": None,
                "is_active": True,
            },
            mskdate(2014,  3,  2,  1): {
                "ban": True,
                "is_active": True,
            },
        },
        "watchpoints": {
            mskdate(2014,  2,  2,  0): '1',
            mskdate(2014,  3,  2,  0): '2',
            mskdate(2014,  3,  2,  1): '3',
        },
        "skip_rate": 0.8,
    }
)


class TestTempTariff(BaseTariffTest):
    @pytest.mark.parametrize("case_name, case", [
        TESTCASE_TEMP_TARIFF_21_START_STOP,
        TESTCASE_TEMP_TARIFF_MONTH_START_STOP,
    ])
    def test_temp_tariffs(self, case_name, case):
        return self._run_test_case(**case)
