import pytest

import maps.analyzer.pylibs.eta_metrics as eta
import maps.analyzer.pylibs.test_tools as test_tools


def test_extend_stats(ytc):
    expected = ['//extend_stats/table1.out', '//extend_stats/table2.out']
    result = eta.extend_stats(ytc, [
        '//extend_stats/table1.in',
        '//extend_stats/table2.in',
    ])
    for e, r in zip(expected, result):
        test_tools.assert_equal_tables(ytc, e, r, unordered=True)


def test_extend_stats_with_required(ytc):
    [result] = eta.extend_stats(ytc, ['//extend_stats/table1.in'], require_extend_with=eta.schema.EXTENDED_COLUMNS)
    test_tools.assert_equal_tables(ytc, '//extend_stats/table1.out', result, unordered=True)

    [result] = eta.extend_stats(ytc, ['//extend_stats/table2.in'], require_extend_with=[
        eta.schema.LENGTH_GROUP, eta.schema.TIMEZONE, eta.schema.LOCAL_ETALON_AT,
    ])
    test_tools.assert_equal_tables(ytc, '//extend_stats/table2.out', result, unordered=True)

    with pytest.raises(ValueError):
        eta.extend_stats(ytc, ['//extend_stats/table2.in'], require_extend_with=eta.schema.EXTENDED_COLUMNS)
