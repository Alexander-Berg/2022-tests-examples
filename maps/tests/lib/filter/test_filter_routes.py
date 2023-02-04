from maps.analyzer.toolkit.lib.filter_routes import filter_routes
from maps.analyzer.pylibs.test_tools.compare_tables import assert_equal_tables

ROUTES = {
    'good_route': [(3, 1), (5, 0), (5, 1), (5, 2), (2, 0)],
    'bad_route': [(7, 1), (2, 0), (2, 1), (4, 1), (4, 2)]
}


def test_filter_routes(ytc):
    expected = "//filter_routes/table1.out"
    result = filter_routes(ytc, "//filter_routes/table1.in", ROUTES)
    assert_equal_tables(ytc, expected, result)
