from yatest.common import source_path
import mock

from contextlib import contextmanager
from yt.wrapper import ypath_join

import maps.analyzer.sandbox.eta_prediction.offline_eta_quality.lib as lib
import maps.analyzer.pylibs.envkit as envkit
import maps.analyzer.pylibs.schema as s
import maps.analyzer.toolkit.lib as tk
from maps.pylibs.yt.lib import unwrap_yt_error


def enrich_stats(ytc, stats, **kwargs) -> str:
    return stats


def evaluate_metrics(ytc, stats, **kwargs) -> str:
    # very wrong, should be metrics schema instead, but it is nowhere to be found
    t = ytc.create_temp_table(schema=s.table([s.column('rows', s.Uint64, None)], None).schema)
    ytc.write_table(t, [{'rows': ytc.get(ypath_join(stats, '@row_count'))}])
    return t


# CHYT can't be tested locally (CHYT-136), thus we just disable it mocking with some functions
# MAPSJAMS-3780 for at least testing `eta_metrics` library itself
@contextmanager
def patch_chyt():
    with mock.patch('maps.analyzer.pylibs.eta_metrics.enrich_stats', side_effect=enrich_stats):
        with mock.patch('maps.analyzer.pylibs.eta_metrics.evaluate_metrics', side_effect=evaluate_metrics):
            yield


def run_impl(ytc, model_name, date_begin, date_end, fmt_days_to_check):
    cfg = tk.utils.load_json(source_path('maps/analyzer/sandbox/eta_prediction/offline_eta_quality/tests/conf/{}.json'.format(model_name or 'no_models')))
    # lags = cfg['models'].get(model_name, {}).get('lags', [0])

    results_root = ypath_join(lib.stats.RESULTS_ROOT, cfg['name'])

    with patch_chyt(), unwrap_yt_error():
        ytc.create('map_node', results_root, recursive=True)
        lib.run(
            config=cfg,
            begin_date=date_begin,
            end_date=date_end,
            days_back=15,
            force=True,
        )

    def ensure_created(day):
        stats_table = ypath_join(results_root, 'stats', day)
        assert ytc.exists(stats_table), 'should create stats_table: {}'.format(stats_table)
        assert ytc.get(ypath_join(stats_table, "@row_count")) > 0
        assert envkit.yt.svn_revision_attribute(ytc, stats_table) == envkit.config.SVN_REVISION, "stats should set svn revision attribute"
        assert s.is_strong_schema(ytc, stats_table)

        yt_metrics_table = ypath_join(results_root, 'metrics', day)
        yt_metrics_hourly_table = ypath_join(results_root, 'metrics_hourly', day)

        for metrics_table in (yt_metrics_table, yt_metrics_hourly_table):
            assert ytc.exists(metrics_table), 'should create metrics_table'
            assert ytc.get(ypath_join(metrics_table, "@row_count")) > 0
            assert envkit.yt.svn_revision_attribute(ytc, metrics_table) == envkit.config.SVN_REVISION, "metrics should set svn revision attribute"
            assert s.is_strong_schema(ytc, metrics_table)

        # FIXME: Uncomment when local tests with CHYT made possible
        # metrics_table = tk.quality.metrics.read_metrics_table(ytc, yt_metrics_table)
        # metrics_hourly_table = tk.quality.metrics.read_metrics_table(ytc, yt_metrics_hourly_table)

        # def check_metrics(metrics, prediction_type, lag, is_hourly):
        #     params = {
        #         'region_id': 213,
        #         'prediction_type': prediction_type,
        #         'length_group': '15_25km',
        #         'lag': lag,
        #     }
        #     if is_hourly:
        #         params.update({
        #             'local_day': day,
        #             'local_hour': 20,
        #         })

        #     vehicle_types_found = {"user": False, "ufo": False}
        #     params_matched = False
        #     for r in metrics:
        #         if all(r.get(k) == v for k, v in params.items()):
        #             params_matched = True
        #         if r.get("vehicle_type") in vehicle_types_found:
        #             vehicle_types_found[r.get("vehicle_type")] = True
        #     if not params_matched:
        #         raise ValueError('cannot find metrics:\n{}\nin table:\n{}'.format(params, metrics))
        #     assert all(list(vehicle_types_found.values()))
        #     assert not any(m['prediction_type'] == 'not_suitable' for m in metrics_table)

        # if model_name is not None:
        #     for lag in lags:
        #         check_metrics(metrics_table, 'model:{}'.format(model_name), lag, False)
        #         check_metrics(metrics_hourly_table, 'model:{}'.format(model_name), lag, True)

        # for prediction_type in ('average', 'jams'):
        #     check_metrics(metrics_table, prediction_type, 0, False)
        #     check_metrics(metrics_hourly_table, prediction_type, 0, True)

    for fmt_day in fmt_days_to_check:
        ensure_created(fmt_day)
