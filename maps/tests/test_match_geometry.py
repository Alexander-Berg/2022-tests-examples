import pytest

from maps.analyzer.pylibs.test_tools.compare_tables import assert_equal_tables

import maps.analyzer.pylibs.envkit as envkit

from maps.analyzer.pylibs.graphmatching.lib import match_geometry


def test_match_geometry_1(ytc):
    expected = "//match_geometry/table1.filtered.rg.out"
    result = match_geometry(
        ytc,
        "//match_geometry/table1.in",
    )
    assert_equal_tables(ytc, expected, result, unordered=True)
    assert envkit.graph.graph_version_attribute(ytc, result) == envkit.graph.VERSION


def test_match_geometry_2(ytc):
    with pytest.raises(Exception):
        match_geometry(
            ytc,
            "//match_geometry/table1.in",
            fail_on_broken=True,
        )


def test_match_geometry_3(ytc):
    expected = "//match_geometry/table1.filtered.rg.out"
    result = match_geometry(
        ytc,
        "//match_geometry/table1.in",
    )
    assert_equal_tables(ytc, expected, result, unordered=True)
    assert envkit.graph.graph_version_attribute(ytc, result) == envkit.graph.VERSION


def test_match_geometry_4(ytc):
    expected_list = ["//match_geometry/table1.filtered.rg.out", "//match_geometry/table2.rg.out"]
    result_list = match_geometry(
        ytc,
        ["//match_geometry/table1.in", "//match_geometry/table2.in"],
    )
    for expected, result in zip(expected_list, result_list):
        assert_equal_tables(ytc, expected, result, unordered=True)
        assert envkit.graph.graph_version_attribute(ytc, result) == envkit.graph.VERSION


def test_match_geometry_5(ytc):
    expected = "//match_geometry/table1.filtered.rg.out"
    result = match_geometry(
        ytc,
        "//match_geometry/table1.in",
        threads=5,
        op_spec={'mapper': {'cpu_limit': 1}},  # test cluster have no CPUs
    )
    assert_equal_tables(ytc, expected, result, unordered=True)
    assert envkit.graph.graph_version_attribute(ytc, result) == envkit.graph.VERSION
