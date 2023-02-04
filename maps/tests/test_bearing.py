import pytest

from maps.garden.modules.export_cams.lib.extract import bearing


def test_bearing_limits():
    point1 = (35, 55)
    point2 = (35, 70)
    assert bearing(point1, point2) == pytest.approx(0)
    point3 = (30, 60)
    assert bearing(point1, point3) > 180.
    assert bearing(point1, point3) < 360.
