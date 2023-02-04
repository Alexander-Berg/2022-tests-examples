# coding=utf-8
import pytest

from .base_tariffs import mskdate, BaseTariffTest

service_id = 6
service_cc = "speechkit"

SPEECHKIT_TARIFF_CONTRACTLESS = {
    "service_id": service_id,
    "cc": service_cc + "_gold_contractless_2018",
    "name": "Золотой",
    "tarifficator_config": [
        {"unit": "UnconditionalActivatorUnit", "params": {}},
        {"unit": "StaticLimitsUnit", "params": {"limit_id": service_cc + "_voice_unit_daily", "limit": "-1"}},
        {"unit": "StaticLimitsUnit", "params": {"limit_id": service_cc + "_tts_unit_daily", "limit": "-1"}},
        {"unit": "StaticLimitsUnit", "params": {"limit_id": service_cc + "_ner_unit_daily", "limit": "-1"}},
        {"unit": "TodayPrepayStatisticRangeConsumerUnit", "params": {
            "time_zone": "Europe/Moscow",
            "range_from": "0", "quantum": "1000", "round_method": "ceil", "precision": "1",
            "skip_when_range_out": True, "include_range_from": False, "statistic_aggregator": "voice_unit + tts_unit + ner_unit",
            "limit_statistic_aggregator": "voice_unit + tts_unit + ner_unit",
            "transit_yesterday_stat": True, "true_prepay": True,
            "product_id": "507905", "product_value": "100", "autocharge_personal_account": True,
            "ban_reason": 110, "unban_reason": 111
        }},
        {"unit": "NextTariffSwitchEventUnit", "params": {
            "next_tariff": "The_next_tariff",
            "condition": "D(state_get('products__507905__consumed', 0)) >= 200",
            "share_state": {
                "products__507905__TodayPrepayStatisticRangeConsumerUnit__uncountable_stat":
                    "str(D(state_get('products__507905__TodayPrepayStatisticRangeConsumerUnit__uncountable_stat')) + "
                    "D(state_get('products__507905__TodayPrepayStatisticRangeConsumerUnit__consumed_stat')))",
                "products__507905__TodayPrepayStatisticRangeConsumerUnit__consumed_stat_dt":
                    "state_get('products__507905__TodayPrepayStatisticRangeConsumerUnit__consumed_stat_dt')"
            }
        }}
    ]
}


TESTCASE_SPEECHKIT_TARIFF_CONTRACTLESS = (
    "TESTCASE_SPEECHKIT_TARIFF_CONTRACTLESS",
    {
        "tariff": SPEECHKIT_TARIFF_CONTRACTLESS,
        "start_dt": mskdate(2014,  2,  2,  1, 0),
        "end_dt":   mskdate(2014,  5,  5,  0, 0),
        "statistic": {
            mskdate(2014,  2,  2,  0): {"voice_unit": 6, "tts_unit": 60, "ner_unit":  600},
            mskdate(2014,  2,  2,  1): {"voice_unit": 0, "tts_unit":  0, "ner_unit": 1600},
            mskdate(2014,  2,  3,  0): {"voice_unit": 6, "tts_unit": 60, "ner_unit": 600},
        },
        "payments": {
            mskdate(2014,  2,  2,  3): {"507905": "2000"},
        },
        "checkpoints": {
            mskdate(2014,  2,  2,  1): {
                "is_active": True,
                "ban": True,
                "products__507905__TodayPrepayStatisticRangeConsumerUnit__uncountable_stat": "666",
                "products__507905__consumed": "0",
                "products__507905__credited": "0",
            },
            mskdate(2014,  2,  2,  2): {
                "is_active": True,
                "ban": True,
                "products__507905__TodayPrepayStatisticRangeConsumerUnit__uncountable_stat": "666",
                "products__507905__consumed": "0",
                "products__507905__credited": "0",
            },
            mskdate(2014,  2,  2,  3): {
                "ban": False,
                "products__507905__TodayPrepayStatisticRangeConsumerUnit__uncountable_stat": "666",
                "products__507905__consumed": "200",
                "products__507905__credited": "2000",
            },
            mskdate(2014,  2,  3,  0): {
                "ban": False,
                "products__507905__TodayPrepayStatisticRangeConsumerUnit__uncountable_stat": "400",
                "products__507905__consumed": "200",
                "products__507905__credited": "2000",
            },
            mskdate(2014,  2,  3,  1): {
                "ban": False,
                "products__507905__TodayPrepayStatisticRangeConsumerUnit__uncountable_stat": "400",
                "products__507905__consumed": "300",
                "products__507905__credited": "2000",
            },
            mskdate(2014,  2,  4,  0): {
                "ban": False,
                "products__507905__TodayPrepayStatisticRangeConsumerUnit__uncountable_stat": "734",
                "products__507905__consumed": "300",
                "products__507905__credited": "2000",
            },
        },
        "watchpoints": {
            mskdate(2014,  2,  2,  1): "START",
            mskdate(2014,  2,  2,  2): "BAN",
            mskdate(2014,  2,  2,  3): "UNBAN",
            mskdate(2014,  2,  4,  0): "---",
        },
        "skip_rate": 0.8,
    }
)


class TestPogodaTariff(BaseTariffTest):
    @pytest.mark.parametrize("case_name, case", [
        TESTCASE_SPEECHKIT_TARIFF_CONTRACTLESS,
    ])
    def test_apimaps_tariffs(self, case_name, case):
        return self._run_test_case(**case)
