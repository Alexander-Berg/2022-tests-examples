import copy

import maps.analyzer.pylibs.ytml.pool as pool
import pytest


def test_TsvPoolBuilder():
    builder_1 = pool._TsvPoolBuilder()

    row_0 = {'a': 0}
    with pytest.raises(KeyError):
        next(builder_1(row_0))

    row_1 = {'_target': 10, 'c': 2, 'b': 1, 'a': 0}
    result_1 = next(builder_1(row_1))
    int(result_1['key'])  # must be convertable to int
    assert result_1['subkey'] is None
    assert result_1['value'] == pool._TSV_VALUE_TEMPLATE.format(
        target=10, weight_or_group=1, features='0\t1\t2'
    )

    row_2 = {
        '_target': 10, pool._ID: 0, pool._WEIGHT: 20, pool._SUBKEY: 'hello',
        'a': 0
    }
    result_2 = next(builder_1(row_2))
    assert result_2['key'] == '0'
    assert result_2['subkey'] == 'hello'
    assert result_2['value'] == pool._TSV_VALUE_TEMPLATE.format(
        target=10, weight_or_group=20, features='0'
    )

    row_3 = copy.deepcopy(row_2)
    del row_3[pool._WEIGHT]
    row_3[pool._GROUP] = 0
    result_3 = next(builder_1(row_3))
    assert result_3['value'] == pool._TSV_VALUE_TEMPLATE.format(
        target=10, weight_or_group=0, features='0'
    )

    builder_2 = pool._TsvPoolBuilder(lambda x: 'goodbye')
    row_4 = {'_target': 10, 'a': 0}
    result_4 = next(builder_2(row_4))
    assert result_4['subkey'] == 'goodbye'
