from shapely.geometry import LineString
from maps.poi.streetview_poi.sign_hypotheses.lib.utils import (
    get_geometry_with_nearest_sign
    )


def test_nearest_sign():
    oid = 1
    org = [0, 0]
    nearest_sign_line = LineString([org, [0, 1]])
    other_sign_line = LineString([org, [2, 1]])
    org_oid_to_lat_lon = {oid: org}
    geometry = {
        oid: [
            nearest_sign_line,
            other_sign_line
        ]
    }
    new_geometry = get_geometry_with_nearest_sign(geometry, org_oid_to_lat_lon)
    assert len(new_geometry) == 1
    assert new_geometry[oid] == nearest_sign_line
