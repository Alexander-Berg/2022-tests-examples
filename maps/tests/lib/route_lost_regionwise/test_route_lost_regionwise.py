from maps.analyzer.pylibs.test_tools.compare_tables import assert_equal_tables
from maps.pylibs.yt.lib import unwrap_yt_error

import maps.analyzer.toolkit.lib.quality.route_lost as route_lost


def test_calc_route_lost_regionwise(ytc):
    source = "//route_lost_regionwise/table.in"
    expected = "//route_lost_regionwise/table.out"

    with unwrap_yt_error():
        result = route_lost.calc_route_lost_regionwise(
            ytc, source, "//route_lost_regionwise"
        )

    assert_equal_tables(ytc, expected, result)
