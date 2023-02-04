import pandas as pd
import numpy as np

import pytest
from dataclasses import dataclass

from maps.infra.monitoring.sla_calculator.core.calculator import (
    all_4xx_5xx, stratify, compute_sla, _impact,
    compute_period_multi_sla, compute_sla_graph, http_statuses
)
from maps.infra.monitoring.sla_calculator.core.service import default_status_for_chart

from helpers import PresetFactory


pd.options.mode.chained_assignment = 'raise'


@dataclass
class EndpointMetricsTable:
    endpoint: str
    good_all: float
    good_codes_timings: float
    good_codes: float
    date: str


@dataclass
class BadCodesTable:
    status_endpoint: str
    impact_all: float
    impact_codes_timings: float
    impact_codes: float
    date: str


def almost_equal(a, b):
    return abs(a - b) < 1e-7


class TestStratify:
    def test_endpoints_have_same_weight(self):
        statuses_a = PresetFactory.statuses('a', '2017-01-01', good=10000, bad=0)
        statuses_b = PresetFactory.statuses('b', '2017-01-02', good=0, bad=50000)

        statuses = stratify(pd.concat([statuses_a, statuses_b], ignore_index=True))
        assert almost_equal(compute_sla(statuses), 0.5)

    def test_no_effect_for_one_endpoint(self):
        statuses = PresetFactory.statuses('a', '2017-01-01', good=10000, bad=30000)
        assert almost_equal(
            compute_sla(statuses),
            compute_sla(stratify(statuses)))

    def test_dates_are_idependent(self):
        statuses_a = PresetFactory.statuses('a', '2017-01-01', good=10000, bad=30000)
        statuses_b = PresetFactory.statuses('a', '2017-01-02', good=30000, bad=10000)

        statuses = stratify(pd.concat([statuses_a, statuses_b], ignore_index=True))

        assert almost_equal(
            compute_sla(statuses[statuses['date'] == '2017-01-01']),
            compute_sla(statuses_a))
        assert almost_equal(
            compute_sla(statuses[statuses['date'] == '2017-01-02']),
            compute_sla(statuses_b))

    def test_stratify_limit(self):
        ''' 'a' has very low RPS and does not affect SLA '''
        statuses_a = PresetFactory.statuses('a', '2017-01-01', good=100, bad=300)
        statuses_b = PresetFactory.statuses('b', '2017-01-01', good=10000000, bad=30000000)

        assert almost_equal(
            compute_sla(stratify(pd.concat([statuses_a, statuses_b], ignore_index=True))),
            compute_sla(statuses_b))

    def test_empty(self):
        assert 0 == len(stratify(pd.DataFrame(columns=['date', 'endpoint', 'amount', 'too_long'])))

    def test_empty_dates(self):
        statuses = stratify(
            pd.concat([
                # 2017-01-01 count <  stratify_limit for both endpoints
                PresetFactory.statuses('/a', '2017-01-01', good=100, bad=0),
                PresetFactory.statuses('/b', '2017-01-01', good=0, bad=1000),
                # 2017-01-02 one endpoint is above stratify_limit
                PresetFactory.statuses('/a', '2017-01-02', good=500, bad=100),
                PresetFactory.statuses('/b', '2017-01-02', good=1500, bad=500),
                # 2017-01-03 is blank
                PresetFactory.statuses_blank_date('2017-01-03'),
                # 2017-01-04 both endpoints above stratify_limit
                PresetFactory.statuses('/a', '2017-01-04', good=4500, bad=500),
                PresetFactory.statuses('/b', '2017-01-04', good=500, bad=4500)
            ]),
            limit=1000
        )
        expected = pd.concat([
            PresetFactory.statuses('/b', '2017-01-02', good=0.75, bad=0.25).assign(amount_totals=2000),
            PresetFactory.statuses('/a', '2017-01-04', good=0.9, bad=0.1).assign(amount_totals=5000),
            PresetFactory.statuses('/b', '2017-01-04', good=0.1, bad=0.9).assign(amount_totals=5000),
            # Expect '2017-01-01' and '2017-01-03' as blanks
            PresetFactory.statuses_blank_date('2017-01-01').drop(columns=['too_long']),
            PresetFactory.statuses_blank_date('2017-01-03').drop(columns=['too_long'])
        ])
        pd.testing.assert_frame_equal(
            statuses.sort_values(by=['date'], ignore_index=True),
            expected.sort_values(by=['date'], ignore_index=True)
        )


class TestComputeSla:
    def test_one_endpoint(self):
        for part in np.arange(0, 1, 0.1):
            statuses = PresetFactory.statuses('a', '2017-01-01', good=10000 * part, bad=10000 * (1 - part))
            assert almost_equal(compute_sla(statuses), part)

    def test_many_dates(self):
        assert almost_equal(
            compute_sla(pd.concat([
                PresetFactory.statuses('a', '2017-01-01', good=1000, bad=3000),
                PresetFactory.statuses('a', '2017-01-02', good=3000, bad=1000),
            ], ignore_index=True)),
            0.5)

    def test_blank_date_ignored(self):
        sla = compute_sla(pd.concat([
            PresetFactory.statuses('a', '2017-01-01', good=1000, bad=3000),
            PresetFactory.statuses_blank_date('2017-01-02'),
            PresetFactory.statuses('a', '2017-01-03', good=3000, bad=1000),
        ], ignore_index=True))
        assert almost_equal(sla, 0.5)

    def test_many_endpoints(self):
        assert almost_equal(
            compute_sla(pd.concat([
                PresetFactory.statuses('a', '2017-01-01', good=1000, bad=3000),
                PresetFactory.statuses('b', '2017-01-01', good=3000, bad=1000),
            ], ignore_index=True)),
            0.5)

    def test_empty(self):
        sla = compute_sla(pd.DataFrame(columns=['date', 'endpoint', 'amount', 'too_long']))
        assert np.isnan(sla)  # expect NaN if no data

    def test_blank(self):
        sla = compute_sla(pd.concat([
            PresetFactory.statuses_blank_date('2022-02-22'),
            PresetFactory.statuses_blank_date('2022-02-23')
        ]))
        assert np.isnan(sla)  # expect NaN if data is blank

    def test_custom_is_code_an_error(self):
        statuses = PresetFactory.statuses('a', '2017-01-01', good=300, bad=700)
        assert almost_equal(compute_sla(statuses, lambda r: r['status'] == 200), .7)

    def test_custom_is_code_an_error_with_custom_column(self):
        def is_code_an_error(r):
            return r['status'] != 500 and r['custom_column'] >= 4
        statuses = PresetFactory.statuses_custom_column(
            'a', '2017-01-01', good2=2, bad3=5, good4=11, bad5=23)
        assert almost_equal(compute_sla(statuses, is_code_an_error), 30./41.)


class TestComputeImpact:
    def test_one_endpoint(self):
        for good in np.arange(0, 1, 0.25):
            for bad in np.arange(0, 1. - good, 0.25):
                for timeout_good in np.arange(0, 1 - good - bad, 0.25):
                    timeout_bad = 1. - good - bad - timeout_good
                    total = 1000
                    statuses = PresetFactory.endpoint_metrics(
                        '404:/a',
                        '2021-01-01',
                        good=total * good,
                        bad=total * bad,
                        timeout_good=total * timeout_good,
                        timeout_bad=total * timeout_bad,
                    )
                    assert almost_equal(_impact(statuses, statuses, 'good_all'), 1 - good)
                    assert almost_equal(_impact(statuses, statuses, 'good_codes'), 1 - good - timeout_good)
                    assert almost_equal(_impact(statuses, statuses, 'good_codes_timings'), 1 - good)

    def test_many_dates(self):
        total = 1000
        bad = .3
        timeout_good = .1
        timeout_bad = .04
        good = 1 - bad - timeout_good - timeout_bad
        statuses = pd.concat([
            PresetFactory.endpoint_metrics(
                '404:/a',
                '2021-01-01',
                good=total * good,
                bad=total * bad,
                timeout_good=total * timeout_good,
                timeout_bad=total * timeout_bad,
            ),
            PresetFactory.endpoint_metrics(
                '404:/a',
                '2021-01-02',
                good=total * good,
                bad=total * bad,
                timeout_good=total * timeout_good,
                timeout_bad=total * timeout_bad,
            ),
        ], ignore_index=True)
        assert almost_equal(_impact(statuses, statuses, 'good_all'), 1 - good)
        assert almost_equal(_impact(statuses, statuses, 'good_codes'), 1 - good - timeout_good)
        assert almost_equal(_impact(statuses, statuses, 'good_codes_timings'), 1 - good)

    def test_many_endpoints(self):
        total = 1000
        bad = .3
        timeout_good = .1
        timeout_bad = .04
        good = 1 - bad - timeout_good - timeout_bad
        statuses = pd.concat([
            PresetFactory.endpoint_metrics(
                '404:/a',
                '2021-01-01',
                good=total * good,
                bad=total * bad,
                timeout_good=total * timeout_good,
                timeout_bad=total * timeout_bad,
            ),
            PresetFactory.endpoint_metrics(
                '404:/b',
                '2021-01-01',
                good=total * good,
                bad=total * bad,
                timeout_good=total * timeout_good,
                timeout_bad=total * timeout_bad,
            ),
        ], ignore_index=True)
        assert almost_equal(_impact(statuses, statuses, 'good_all'), 1 - good)
        assert almost_equal(_impact(statuses, statuses, 'good_codes'), 1 - good - timeout_good)
        assert almost_equal(_impact(statuses, statuses, 'good_codes_timings'), 1 - good)

    def test_empty(self):
        statuses = pd.DataFrame(columns=[
            'date', 'status_endpoint', 'amount', 'good_all', 'good_codes', 'good_codes_timings'
        ])
        assert almost_equal(_impact(statuses, statuses, 'good_all'), 0)


class TestComputeMultiSla:
    def test_one_endpoint(self):
        for timely_good in np.arange(0, 1, 0.25):
            for timely_bad in np.arange(0, 1. - timely_good, 0.25):
                for timeout_good in np.arange(0, 1 - timely_good - timely_bad, 0.25):
                    timeout_bad = 1. - timely_good - timely_bad - timeout_good
                    total = 1000
                    statuses = PresetFactory.multi_statuses(
                        'a',
                        '2017-01-01',
                        good=total * timely_good,
                        bad=total * timely_bad,
                        timeout_good=total * timeout_good,
                        timeout_bad=total * timeout_bad,
                    )
                    endpoint_metrics, bad_codes = compute_period_multi_sla(
                        statuses,
                        sorted(statuses["date"].unique()),
                        period=1,
                        is_code_an_error=lambda r: r['status'] == 500,
                        sla_limit=.95,
                        status_for_chart=default_status_for_chart,
                    )

                    all_good = timely_good + timeout_good
                    pd.testing.assert_frame_equal(
                        endpoint_metrics,
                        pd.DataFrame([
                            EndpointMetricsTable('a', timely_good, timely_good, all_good, '2017-01-01'),
                            EndpointMetricsTable('*', timely_good, timely_good, all_good, '2017-01-01'),
                        ]),
                    )

                    all_bad = timely_bad + timeout_bad
                    expected_bad_codes = pd.DataFrame([
                        BadCodesTable('200:a',  timeout_good,    timeout_good,    0.,      '2017-01-01'),
                        BadCodesTable('200:*',  timeout_good,    timeout_good,    0.,      '2017-01-01'),
                        BadCodesTable('500:a',  all_bad,         all_bad,         all_bad, '2017-01-01'),
                        BadCodesTable('500:*',  all_bad,         all_bad,         all_bad, '2017-01-01'),
                        BadCodesTable('*:a',    1 - timely_good, 1 - timely_good, all_bad, '2017-01-01'),
                        BadCodesTable('*:*',    1 - timely_good, 1 - timely_good, all_bad, '2017-01-01'),
                        BadCodesTable('TARGET', .05,             .05,             .05,     '2017-01-01'),
                    ])
                    expected_bad_codes = expected_bad_codes[
                        (expected_bad_codes.impact_all > 0)
                        | (expected_bad_codes.impact_codes_timings > 0)
                        | (expected_bad_codes.impact_codes > 0)
                        | (expected_bad_codes.status_endpoint == '*:*')
                    ].set_index('status_endpoint')
                    pd.testing.assert_frame_equal(bad_codes.set_index('status_endpoint'), expected_bad_codes)

    def test_many_dates(self):
        total = 1000
        timely_bad = .3
        timeout_good = .1
        timeout_bad = .04
        timely_good = 1 - timely_bad - timeout_good - timeout_bad
        statuses = pd.concat([
            PresetFactory.multi_statuses(
                'a',
                '2021-01-01',
                good=total * timely_good,
                bad=total * timely_bad,
                timeout_good=total * timeout_good,
                timeout_bad=total * timeout_bad,
            ),
            PresetFactory.multi_statuses(
                'a',
                '2021-01-02',
                good=total * timely_good,
                bad=total * timely_bad,
                timeout_good=total * timeout_good,
                timeout_bad=total * timeout_bad,
            ),
        ], ignore_index=True)

        endpoint_metrics, bad_codes = compute_period_multi_sla(
            statuses,
            sorted(statuses["date"].unique()),
            period=1,
            is_code_an_error=lambda r: r['status'] == 500,
            sla_limit=.98,
            status_for_chart=default_status_for_chart,
        )
        all_good = timely_good + timeout_good
        pd.testing.assert_frame_equal(
            endpoint_metrics.set_index(['endpoint', 'date']),
            pd.DataFrame([
                EndpointMetricsTable('a', timely_good, timely_good, all_good, '2021-01-01'),
                EndpointMetricsTable('*', timely_good, timely_good, all_good, '2021-01-01'),
                EndpointMetricsTable('a', timely_good, timely_good, all_good, '2021-01-02'),
                EndpointMetricsTable('*', timely_good, timely_good, all_good, '2021-01-02'),
            ]).set_index(['endpoint', 'date']),
        )

        all_bad = timely_bad + timeout_bad
        expected_bad_codes = pd.DataFrame([
            BadCodesTable('200:a',  timeout_good,    timeout_good,    0.,      '2021-01-01'),
            BadCodesTable('200:*',  timeout_good,    timeout_good,    0.,      '2021-01-01'),
            BadCodesTable('500:a',  all_bad,         all_bad,         all_bad, '2021-01-01'),
            BadCodesTable('500:*',  all_bad,         all_bad,         all_bad, '2021-01-01'),
            BadCodesTable('*:a',    1 - timely_good, 1 - timely_good, all_bad, '2021-01-01'),
            BadCodesTable('*:*',    1 - timely_good, 1 - timely_good, all_bad, '2021-01-01'),
            BadCodesTable('TARGET', .02,             .02,             .02,     '2021-01-01'),
            BadCodesTable('200:a',  timeout_good,    timeout_good,    0.,      '2021-01-02'),
            BadCodesTable('200:*',  timeout_good,    timeout_good,    0.,      '2021-01-02'),
            BadCodesTable('500:a',  all_bad,         all_bad,         all_bad, '2021-01-02'),
            BadCodesTable('500:*',  all_bad,         all_bad,         all_bad, '2021-01-02'),
            BadCodesTable('*:a',    1 - timely_good, 1 - timely_good, all_bad, '2021-01-02'),
            BadCodesTable('*:*',    1 - timely_good, 1 - timely_good, all_bad, '2021-01-02'),
            BadCodesTable('TARGET', .02,             .02,             .02,     '2021-01-02'),
        ])
        expected_bad_codes = expected_bad_codes[
            (expected_bad_codes.impact_all > 0)
            | (expected_bad_codes.impact_codes_timings > 0)
            | (expected_bad_codes.impact_codes > 0)
            | (expected_bad_codes.status_endpoint == '*:*')
        ].set_index('status_endpoint')
        pd.testing.assert_frame_equal(bad_codes.set_index('status_endpoint'), expected_bad_codes)

    def test_many_endpoints(self):
        total = 1000
        timely_bad = .3
        timeout_good = .1
        timeout_bad = .04
        timely_good = 1 - timely_bad - timeout_good - timeout_bad
        statuses = pd.concat([
            PresetFactory.multi_statuses(
                'a',
                '2021-01-01',
                good=total * timely_good,
                bad=total * timely_bad,
                timeout_good=total * timeout_good,
                timeout_bad=total * timeout_bad,
            ),
            PresetFactory.multi_statuses(
                'b',
                '2021-01-01',
                good=total * timely_good,
                bad=total * timely_bad,
                timeout_good=total * timeout_good,
                timeout_bad=total * timeout_bad,
            ),
        ], ignore_index=True)
        endpoint_metrics, bad_codes = compute_period_multi_sla(
            statuses,
            sorted(statuses["date"].unique()),
            period=1,
            is_code_an_error=lambda r: r['status'] == 500,
            sla_limit=.98,
            status_for_chart=default_status_for_chart,
        )
        all_good = timely_good + timeout_good
        pd.testing.assert_frame_equal(
            endpoint_metrics,
            pd.DataFrame([
                EndpointMetricsTable('a', timely_good, timely_good, all_good, '2021-01-01'),
                EndpointMetricsTable('b', timely_good, timely_good, all_good, '2021-01-01'),
                EndpointMetricsTable('*', timely_good, timely_good, all_good, '2021-01-01'),
            ]),
        )

        all_bad = timely_bad + timeout_bad
        expected_bad_codes = pd.DataFrame([
            BadCodesTable('200:a',  timeout_good / 2,      timeout_good / 2,      0.,          '2021-01-01'),
            BadCodesTable('200:b',  timeout_good / 2,      timeout_good / 2,      0.,          '2021-01-01'),
            BadCodesTable('200:*',  timeout_good,          timeout_good,          0.,          '2021-01-01'),
            BadCodesTable('500:a',  all_bad / 2,           all_bad / 2,           all_bad / 2, '2021-01-01'),
            BadCodesTable('500:b',  all_bad / 2,           all_bad / 2,           all_bad / 2, '2021-01-01'),
            BadCodesTable('500:*',  all_bad,               all_bad,               all_bad,     '2021-01-01'),
            BadCodesTable('*:a',    (1 - timely_good) / 2, (1 - timely_good) / 2, all_bad / 2, '2021-01-01'),
            BadCodesTable('*:b',    (1 - timely_good) / 2, (1 - timely_good) / 2, all_bad / 2, '2021-01-01'),
            BadCodesTable('*:*',    1 - timely_good,       1 - timely_good,       all_bad,     '2021-01-01'),
            BadCodesTable('TARGET', .02,                   .02,                   .02,         '2021-01-01'),
        ])
        expected_bad_codes = expected_bad_codes[
            (expected_bad_codes.impact_all > 0)
            | (expected_bad_codes.impact_codes_timings > 0)
            | (expected_bad_codes.impact_codes > 0)
            | (expected_bad_codes.status_endpoint == '*:*')
        ].set_index('status_endpoint')
        pd.testing.assert_frame_equal(bad_codes.set_index('status_endpoint'), expected_bad_codes)

    def test_no_impact(self):
        total = 1000
        timely_bad = 0.
        timeout_good = 0.
        timeout_bad = 0.
        timely_good = 1 - timely_bad - timeout_good - timeout_bad
        statuses = PresetFactory.multi_statuses(
            'a',
            '2021-01-01',
            good=total * timely_good,
            bad=total * timely_bad,
            timeout_good=total * timeout_good,
            timeout_bad=total * timeout_bad,
        )
        endpoint_metrics, bad_codes = compute_period_multi_sla(
            statuses,
            sorted(statuses["date"].unique()),
            period=1,
            is_code_an_error=lambda r: r['status'] == 500,
            sla_limit=.98,
            status_for_chart=default_status_for_chart,
        )
        all_good = timely_good + timeout_good
        pd.testing.assert_frame_equal(
            endpoint_metrics,
            pd.DataFrame([
                EndpointMetricsTable('a', timely_good, timely_good, all_good, '2021-01-01'),
                EndpointMetricsTable('*', timely_good, timely_good, all_good, '2021-01-01'),
            ]),
        )

        all_bad = timely_bad + timeout_bad
        expected_bad_codes = pd.DataFrame([
            BadCodesTable('*:*',    1 - timely_good, 1 - timely_good, all_bad, '2021-01-01'),
            BadCodesTable('TARGET',             .02,             .02,     .02, '2021-01-01'),
        ])
        pd.testing.assert_frame_equal(bad_codes, expected_bad_codes)

    def test_empty(self):
        statuses = PresetFactory.multi_statuses(
            'a', '2017-01-01', good=0, bad=0, timeout_good=0, timeout_bad=0)[0:0]

        endpoint_metrics, bad_codes = compute_period_multi_sla(
            statuses,
            sorted(statuses["date"].unique()),
            period=1,
            is_code_an_error=lambda r: r['status'] == 500,
            sla_limit=.95,
            status_for_chart=default_status_for_chart,
        )
        # Expect empty results
        assert endpoint_metrics.empty
        assert bad_codes.empty

    def test_blank(self):
        statuses = pd.concat([
            PresetFactory.statuses_blank_date('2022-01-01'),
            PresetFactory.statuses_blank_date('2022-01-02'),
        ])
        endpoint_metrics, bad_codes = compute_period_multi_sla(
            statuses,
            sorted(statuses["date"].unique()),
            period=7,
            is_code_an_error=None,
            sla_limit=.95,
            status_for_chart=default_status_for_chart,
        )
        assert endpoint_metrics.empty
        assert bad_codes.empty

    def test_bicycle_router(self):
        statuses = PresetFactory.statuses_bicycle_router()

        endpoint_metrics, bad_codes = compute_period_multi_sla(
            statuses,
            sorted(statuses["date"].unique()),
            period=1,
            is_code_an_error=all_4xx_5xx(exclude=[400, 401, 403, 404, 429]),
            sla_limit=.98,
            status_for_chart=default_status_for_chart,
        )
        pd.testing.assert_frame_equal(
            endpoint_metrics,
            pd.DataFrame([
                EndpointMetricsTable('/v2/route',   1 - (33+30975)/10463050, 1 - (33+30975)/10463050, 1 - 33 / 10463050, '2021-07-18'),
                EndpointMetricsTable('/v2/summary', 1-4.636805497335284e-05, 1-4.636805497335284e-05, 1., '2021-07-18'),
                EndpointMetricsTable('/v2/uri',     1.,                      1.,                      1., '2021-07-18'),
                EndpointMetricsTable('*',           1.-0.000577800278161056, 1.-0.000577800278161056, 1-5.745618387065283e-07, '2021-07-18'),
            ]),
        )

        expected_bad_codes = pd.DataFrame([
            BadCodesTable('200:/v2/route', 0.0005393046349677186, 0.0005393046349677186, 0., '2021-07-18'),
            BadCodesTable('200:/v2/summary', 3.792108135463087e-05, 3.792108135463087e-05, 0., '2021-07-18'),
            BadCodesTable('200:*', 0.0005772257163223494, 0.0005772257163223494, 0., '2021-07-18'),
            BadCodesTable('*:/v2/route', 0.0005393046349677186, 0.0005393046349677186, 0., '2021-07-18'),
            BadCodesTable('*:/v2/summary', 3.792108135463087e-05, 3.792108135463087e-05, 0., '2021-07-18'),
            BadCodesTable('*:*', 0.0005772257163223494, 0.0005772257163223494, 0., '2021-07-18'),
            BadCodesTable('TARGET', .02, .02, .02, '2021-07-18'),
        ])
        # 'date': ['2021-07-18'] * 11,
        # 'amount': [839468.0, 2370.0, 9487001.0, 33.0, 103203.0, 30975.0, 661351.0, 4.6308465e7, 1683.0, 495.0, 27.0],
        # 'endpoint': ['/v2/route'] * 6 + ['/v2/summary'] * 4 + ['/v2/uri'],
        # 'status': [429, 499, 200, 400, 200, 200, 429] + [200] * 4,
        # 'too_long': [False] * 5 + [True, False, False, True, True, False],
        # 'external_good': [True] * 11,

        expected_bad_codes = expected_bad_codes[
            (expected_bad_codes.impact_all > 0)
            | (expected_bad_codes.impact_codes_timings > 0)
            | (expected_bad_codes.impact_codes > 0)
            | (expected_bad_codes.status_endpoint == '*:*')
        ].set_index('status_endpoint')
        pd.testing.assert_frame_equal(bad_codes.set_index('status_endpoint'), expected_bad_codes)

    def test_custom_status_for_charts(self):
        def _custom_status_for_charts(rec: pd.Series) -> str:
            if rec.endpoint == 'b' and rec.status == 200:
                return 'b-200'
            return rec.status

        total = 1000
        timely_bad = .3
        timeout_good = .1
        timeout_bad = .04
        timely_good = 1 - timely_bad - timeout_good - timeout_bad
        total = 1000
        statuses = pd.concat([
            PresetFactory.multi_statuses(
                'a',
                '2021-01-01',
                good=total * timely_good,
                bad=total * timely_bad,
                timeout_good=total * timeout_good,
                timeout_bad=total * timeout_bad,
            ),
            PresetFactory.multi_statuses(
                'b',
                '2021-01-01',
                good=total * timely_good,
                bad=total * timely_bad,
                timeout_good=total * timeout_good,
                timeout_bad=total * timeout_bad,
            ),
        ], ignore_index=True)
        endpoint_metrics, bad_codes = compute_period_multi_sla(
            statuses,
            sorted(statuses["date"].unique()),
            period=1,
            is_code_an_error=lambda r: r['status'] == 500,
            sla_limit=.95,
            status_for_chart=_custom_status_for_charts,
        )

        all_good = timely_good + timeout_good
        expected_ict = timely_good
        expected_all = timely_good
        pd.testing.assert_frame_equal(
            endpoint_metrics,
            pd.DataFrame([
                EndpointMetricsTable('a', expected_all, expected_ict, all_good, '2021-01-01'),
                EndpointMetricsTable('b', expected_all, expected_ict, all_good, '2021-01-01'),
                EndpointMetricsTable('*', expected_all, expected_ict, all_good, '2021-01-01'),
            ]),
        )

        all_bad = timely_bad + timeout_bad
        expected_bad_codes = pd.DataFrame([
            BadCodesTable('200:a',   timeout_good / 2,      timeout_good / 2,      0.,          '2021-01-01'),
            BadCodesTable('200:*',   timeout_good / 2,      timeout_good / 2,      0.,          '2021-01-01'),
            BadCodesTable('500:a',   all_bad / 2,           all_bad / 2,           all_bad / 2, '2021-01-01'),
            BadCodesTable('500:b',   all_bad / 2,           all_bad / 2,           all_bad / 2, '2021-01-01'),
            BadCodesTable('500:*',   all_bad,               all_bad,               all_bad,     '2021-01-01'),
            BadCodesTable('b-200:b', timeout_good / 2,      timeout_good / 2,      0.,          '2021-01-01'),
            BadCodesTable('b-200:*', timeout_good / 2,      timeout_good / 2,      0.,          '2021-01-01'),
            BadCodesTable('*:a',     (1 - timely_good) / 2, (1 - timely_good) / 2, all_bad / 2, '2021-01-01'),
            BadCodesTable('*:b',     (1 - timely_good) / 2, (1 - timely_good) / 2, all_bad / 2, '2021-01-01'),
            BadCodesTable('*:*',     1 - timely_good,       1 - timely_good,       all_bad,     '2021-01-01'),
            BadCodesTable('TARGET',  .05,                    .05,                    .05,       '2021-01-01'),
        ])
        expected_bad_codes = expected_bad_codes[
            (expected_bad_codes.impact_all > 0)
            | (expected_bad_codes.impact_codes_timings > 0)
            | (expected_bad_codes.impact_codes > 0)
            | (expected_bad_codes.status_endpoint == '*:*')
        ].set_index('status_endpoint')
        pd.testing.assert_frame_equal(bad_codes.set_index('status_endpoint'), expected_bad_codes)


class TestComputeSlaGraph:
    def test_compute_sla_graph(self):
        statuses = pd.concat([
            PresetFactory.statuses('a', '2017-01-01', good=0, bad=3000),
            PresetFactory.statuses('a', '2017-01-02', good=1000, bad=2000),
            PresetFactory.statuses('a', '2017-01-03', good=2000, bad=1000),
            PresetFactory.statuses('a', '2017-01-04', good=3000, bad=0),
        ], ignore_index=True)

        graph = compute_sla_graph(statuses, [1, 4])

        assert len(graph) == 4
        assert almost_equal(graph.d1['2017-01-04'], 1)
        assert almost_equal(graph.d4['2017-01-04'], 0.5)
        assert almost_equal(graph.d1['2017-01-02'], 1.0 / 3)

    def test_blank_dates(self):
        statuses = pd.concat([
            PresetFactory.statuses('/route', '2022-06-22', good=1, bad=9),
            PresetFactory.statuses_blank_date('2022-06-23'),
            # 2020-06-24 is missing
            PresetFactory.statuses('/route', '2022-06-25', good=9, bad=1)
        ])
        graph = compute_sla_graph(statuses, [1, 4])
        # Expect all NaN for '2022-06-23' and '2020-06-24' omitted
        expected = pd.DataFrame({
            'd1': {'2022-06-22': 0.1         , '2022-06-23': float('NaN'), '2022-06-25': 0.9},
            'd4': {'2022-06-22': float('NaN'), '2022-06-23': float('NaN'), '2022-06-25': float('NaN')},
        })
        pd.testing.assert_frame_equal(graph, expected)


_COMPUTE_HTTP_STATUSES_TEST_CASES = [
    [{
        'date': '2017-01-01',
        'endpoint': 'test_endpoint',
        'status': '200',
        'amount': '100',
        'too_long%': '0',
        'too_long_test_endpoint': '1',
        'test_group': 42
    }],
    [(
        '200',            # status
        '100',            # amount
        42,               # test_group
        'test_endpoint',  # endpoint
        '1',              # too_long_test_endpoint
        '0',              # too_long_%
        '2017-01-01',     # date
    )],
    [[
        '200',            # status
        100,              # amount
        42,               # test_group
        'test_endpoint',  # endpoint
        '1',              # too_long_test_endpoint
        '0',              # too_long_%
        '2017-01-01',     # date
    ]],
]


class MockTable:
    def __init__(self, rows):
        self.rows = rows

    def fetch_full_data(self):
        pass


class TestComputeHttpStatuses:
    @pytest.mark.parametrize('rows', _COMPUTE_HTTP_STATUSES_TEST_CASES, ids=str)
    def test_rows(self, monkeypatch, rows):
        table = MockTable(rows)

        monkeypatch.setattr('yql.client.operation.YqlSqlOperationRequest.run', lambda _: None)
        monkeypatch.setattr('yql.client.operation.YqlSqlOperationRequest.get_results', lambda _: [table])
        statuses = http_statuses(
            ['%'],
            ['%'],
            max_latency={'test_endpoint': 0.2, '%': 0.1},
            date='2017-01-01',
            groups={'test_group': 'TEST EXPRESSION'})

        row = statuses.iloc[0]
        assert row['amount'] == 100
        assert row['status'] == 200
        assert row['too_long']
        assert row['test_group'] == 42
