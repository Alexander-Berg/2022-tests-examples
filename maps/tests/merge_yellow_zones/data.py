from shapely import geometry

YELLOW_ZONES_YT_DATA = [
    {
        "dilated_shape": geometry.Polygon([(2, 0), (1, 1), (1, 0)]).wkb.hex()
    },
    {
        "dilated_shape": geometry.Polygon([(175, 75), (176, 76), (174, 75)]).wkb.hex()
    }
]
