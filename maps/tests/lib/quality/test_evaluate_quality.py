from maps.pylibs.yt.lib.unwrap_yt_error import unwrap_yt_error

from maps.analyzer.toolkit.lib.quality.evaluate_quality import evaluate_jams_quality
from maps.analyzer.toolkit.lib.build_historic_jams import to_realtime_jams


# This test checks that the process runs.
# Numbers in the result may be not final.
def test_evaluate_jams_quality_0(ytc):
    with unwrap_yt_error():
        expected = {
            'absolute_error': 5.0,
            'effective_etalon_length': 110.573114803094,
            'relative_error': 1.535714285
        }
        result = evaluate_jams_quality(
            ytc, "//evaluate_quality/table0.assessors.in",
            jams="//evaluate_quality/table0.checked.in",
            cut_length=10000,
            min_length=9000,
            expire_age=10,
            group_by=[],
        )
        for metric_name, expected_metric in expected.items():
            assert abs(result[metric_name] - expected_metric) < 1e-6


# TODO: to fix according to new logic
def _test_evaluate_jams_quality_1(ytc):
    with unwrap_yt_error():
        expected = {
            'absolute_error': 6.0,
            'effective_etalon_length': 44.451431288890682,
            'relative_error': 1.2719298245614035
        }
        result = evaluate_jams_quality(
            ytc, "//evaluate_quality/table1.assessors.in",
            jams="//evaluate_quality/table1.checked.in",
            cut_length=20000,
            min_length=19000,
            expire_age=10,
            group_by=[],
        )
        for metric_name, expected_metric in expected.items():
            assert abs(result[metric_name] - expected_metric) < 1e-6


def test_evaluate_jams_quality_2(ytc):
    with unwrap_yt_error():
        expected = {
            'absolute_error': 5.0,
            'effective_etalon_length': 110.573114803094,
            'relative_error': 1.535714285
        }
        result = evaluate_jams_quality(
            ytc, "//evaluate_quality/table0.assessors.in",
            jams=to_realtime_jams(ytc, "//evaluate_quality/table2.historic.in", historic_translation_date="20150101"),
            cut_length=10000,
            min_length=9000,
            expire_age=10,
            group_by=[],
        )
        for metric_name, expected_metric in expected.items():
            assert abs(result[metric_name] - expected_metric) < 1e-6
