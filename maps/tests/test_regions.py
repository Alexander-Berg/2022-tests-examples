import pytest

from maps.analyzer.pylibs.eta_metrics import round_regions, RegionLevel, extend_regions, set_regions_names
from maps.analyzer.pylibs.test_tools import assert_equal_tables


def test_round_regions_ignore(ytc):
    expected = '//round_regions/table1.out'
    [result] = round_regions(
        ytc, ['//round_regions/table1.in'],
        level=RegionLevel.CITY,
        atlantidize=False,
    )
    assert_equal_tables(ytc, expected, result, unordered=True)


def test_round_regions_atlantidize(ytc):
    expected = '//round_regions/table2.out'
    [result] = round_regions(
        ytc, ['//round_regions/table1.in'],
        level=RegionLevel.CITY,
        atlantidize=True,
    )
    assert_equal_tables(ytc, expected, result, unordered=True)


def test_round_regions_asserts(ytc):
    with pytest.raises(AssertionError):
        round_regions(ytc, '//round_regions/table1.in')


def test_extend_regions(ytc):
    expected = '//extend_regions/table1.out'
    [result] = extend_regions(ytc, ['//extend_regions/table1.in'])
    assert_equal_tables(ytc, expected, result, unordered=True)


def test_set_regions_names(ytc):
    expected = '//set_regions_names/table1.out'
    [result] = set_regions_names(ytc, ['//set_regions_names/table1.in'])
    assert_equal_tables(ytc, expected, result, unordered=True)
