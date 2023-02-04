import unittest
import datetime

from agency_rewards.rewards.platform.forecasts import get_forecast_query, format_forecast
from agency_rewards.rewards.utils.bunker import ForecastData

from . import create_bunker


class TestPlatformForecasts(unittest.TestCase):
    def test_format_forecast(self):
        calc = create_bunker(
            {
                'scale': 'media',
                'freq': 'hf',
                'calc_type': 'r',
                'currency': 'RUR',
                'from_dt': '2020-03-01T00:00:00.000Z',
                'till_dt': '2021-03-01T00:00:00.000Z',
                "calendar": "f",
                "forecast_dist": [
                    {"month": "2020-03-01T00:00:00.000Z", "pct": 17},
                    {"month": "2020-04-01T00:00:00.000Z", "pct": 19},
                    {"month": "2020-05-01T00:00:00.000Z", "pct": 14},
                    {"month": "2020-06-01T00:00:00.000Z", "pct": 17},
                    {"month": "2020-07-01T00:00:00.000Z", "pct": 17},
                    {"month": "2020-08-01T00:00:00.000Z", "pct": 15},
                ],
            },
            insert_dt=datetime.datetime(2020, 5, 5),
        )

        tests = [
            # распределение передаем из калькуляции, все расчитываем по распределению
            {
                'data': [
                    {
                        'contract_id': 1,
                        'contract_eid': '1-01',
                        'discount_type': 25,
                        'turnover_to_charge': 1000.0,
                        'reward_to_charge': 100.0,
                        'delkredere_to_charge': 0,
                        'delkredere_to_pay': 0,
                        'reward_to_pay': 100.0,
                        'reward_to_pay_src': 0,
                        'nds': 1,
                        'currency': 'RUR',
                        'reward_type': 302,
                    }
                ],
                'checks': [
                    ['distribution_pct', 36],
                    # сумма распределений за 2020-03 + 2020-04, так как считаем в 2020-05 месяце
                    ['turnover_to_charge_actual', 360],  # turnover_to_charge * distribution_pct / 100
                    ['reward_to_charge_actual', 36],  # reward_to_charge * distribution_pct / 100
                ],
            },
        ]
        for t in tests:
            res = format_forecast(t['data'], calc)
            for r in res:
                for check in t['checks']:
                    self.assertEqual(r[check[0]], check[1])

    def test_get_forecast_query(self):
        src = "/home/balance/origin/201903"
        dst = "/home/balance/origin_forecast/201903"
        pct = 35

        tests = [
            {
                'columns': ["c1", "c2"],
                'res': (
                    "\n"
                    f"    insert into `{dst}` with truncate\n"
                    f"    select t.c1 * 100 / {pct} as c1,"
                    f" t.c2 * 100 / {pct} as c2,"
                    f" t.* without t.c1, t.c2\n"
                    f"      from `{src}` as t"
                ),
            },
            {
                'columns': [],
                'res': ("\n" f"    insert into `{dst}` with truncate\n" f"    select t.*\n" f"      from `{src}` as t"),
            },
        ]

        for t in tests:
            fd = ForecastData(key="some-no-sense-key", origin_path=src, new_path=dst, columns=t["columns"])
            q = get_forecast_query(fd, pct)
            self.assertEqual(t['res'], q)
