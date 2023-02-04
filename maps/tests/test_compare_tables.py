import pytest

from maps.analyzer.pylibs.test_tools.compare_tables import assert_equal_tables
from yt.wrapper import ypath_join


def test(ytc):
    lhs = ypath_join('//test_1', 'lhs')
    rhs = ypath_join('//test_1', 'rhs')
    assert_equal_tables(ytc, lhs, rhs, float_columns=['column'])

    lhs = ypath_join('//test_2', 'lhs')
    rhs = ypath_join('//test_2', 'rhs')
    assert_equal_tables(ytc, lhs, rhs, float_columns=['column'])

    lhs = ypath_join('//test_2', 'lhs')
    rhs = ypath_join('//test_2', 'rhs')
    with pytest.raises(AssertionError):
        assert_equal_tables(ytc, lhs, rhs)

    lhs = ypath_join('//test_3', 'lhs')
    rhs = ypath_join('//test_3', 'rhs')
    with pytest.raises(AssertionError):
        assert_equal_tables(ytc, lhs, rhs, float_columns=['column'])

    lhs = ypath_join('//test_4', 'lhs')
    rhs = ypath_join('//test_4', 'rhs')
    with pytest.raises(AssertionError):
        assert_equal_tables(ytc, lhs, rhs, float_columns=['column'])

    lhs = ypath_join('//test_5', 'lhs')
    rhs = ypath_join('//test_5', 'rhs')
    with pytest.raises(AssertionError):
        assert_equal_tables(ytc, lhs, rhs, float_columns=['column'])

    lhs = ypath_join('//test_6', 'lhs')
    rhs = ypath_join('//test_6', 'rhs')
    with pytest.raises(AssertionError):
        assert_equal_tables(ytc, lhs, rhs, float_columns=['column'])

    lhs = ypath_join('//test_7', 'lhs')
    rhs = ypath_join('//test_7', 'rhs')
    with pytest.raises(AssertionError):
        assert_equal_tables(ytc, lhs, rhs, float_columns=['column'])

    lhs = ypath_join('//test_8', 'lhs')
    rhs = ypath_join('//test_8', 'rhs')
    assert_equal_tables(ytc, lhs, rhs, float_columns=['column'])

    lhs = ypath_join('//test_9', 'lhs')
    rhs = ypath_join('//test_9', 'rhs')
    with pytest.raises(AssertionError):
        assert_equal_tables(ytc, lhs, rhs, float_columns=['column'])

    lhs = ypath_join('//test_10', 'lhs')
    rhs = ypath_join('//test_10', 'rhs')
    with pytest.raises(AssertionError):
        assert_equal_tables(ytc, lhs, rhs, float_columns=['column'])

    lhs = ypath_join('//test_11', 'lhs')
    rhs = ypath_join('//test_11', 'rhs')
    with pytest.raises(AssertionError):
        assert_equal_tables(ytc, lhs, rhs, float_columns=['column'])

    lhs = ypath_join('//test_12', 'lhs')
    rhs = ypath_join('//test_12', 'rhs')
    with pytest.raises(AssertionError):
        assert_equal_tables(ytc, lhs, rhs, float_columns=['column'])
