from nile.api.v1 import Record


# -------- Input --------

FT = [
    Record(ft_id=1, ft_type_id=1904),
    Record(ft_id=2, ft_type_id=1904),
    Record(ft_id=3, ft_type_id=1904),
    Record(ft_id=4, ft_type_id=1904),
    Record(ft_id=5, ft_type_id=1904),
    Record(ft_id=6, ft_type_id=1904),
    Record(ft_id=7, ft_type_id=1),
]

FT_NM = [
    Record(ft_id=1, name=b"1", lang=b"ru", is_auto=False),
    Record(ft_id=3, name=b"3", lang=b"ru", is_auto=False),
    Record(ft_id=4, name=b"4", lang=b"ru", is_auto=False),
    Record(ft_id=4, name=b"f", lang=b"en", is_auto=False),
    Record(ft_id=5, name=b"5", lang=b"en", is_auto=False),
    Record(ft_id=5, name=b"5", lang=b"ru", is_auto=True),
    Record(ft_id=6, name=b"6", lang=b"ru", is_auto=False),
]

FT_CENTER = [
    Record(ft_id=1, node_id=1),
    Record(ft_id=2, node_id=2),
    Record(ft_id=3, node_id=3),
    Record(ft_id=4, node_id=4),
    Record(ft_id=5, node_id=5),
    Record(ft_id=6, node_id=6),
    Record(ft_id=7, node_id=7),
]

NODE = [
    Record(node_id=1, x=37.496308, y=55.419728),
    Record(node_id=2, x=37.495503, y=55.418601),
    Record(node_id=3, x=65.556745, y=57.121307),
    Record(node_id=4, x=37.744991, y=55.633501),  # Inside building.
    Record(node_id=5, x=37.745011, y=55.633487),  # Within tolerance.
    Record(node_id=6, x=37.745036, y=55.633449),  # Outside.
    Record(node_id=7, x=47.0, y=57.0),  # Not an entrance.
]

FT_ADDR = [
    Record(ft_id=1, addr_id=1),
    Record(ft_id=2, addr_id=1),
    Record(ft_id=2, addr_id=11),
    Record(ft_id=3, addr_id=2),
    Record(ft_id=4, addr_id=3),
    Record(ft_id=5, addr_id=3),
    Record(ft_id=6, addr_id=3),
    Record(ft_id=7, addr_id=4),
]

BLD = [
    Record(bld_id=1, ft_type_id=101),
    Record(bld_id=1, ft_type_id=101),
    Record(bld_id=2, ft_type_id=101),
    Record(bld_id=3, ft_type_id=101),
    Record(bld_id=4, ft_type_id=101),
    Record(bld_id=5, ft_type_id=102),
]

BLD_ADDR = [
    Record(bld_id=1, addr_id=1),
    Record(bld_id=1, addr_id=11),
    Record(bld_id=2, addr_id=2),
    Record(bld_id=3, addr_id=3),
    Record(bld_id=4, addr_id=4),
    Record(bld_id=5, addr_id=5),
    Record(bld_id=6, addr_id=1),
]

BUILDINGS = [
    Record(bld_id=1, addr_id=1),
    Record(bld_id=1, addr_id=11),
    Record(bld_id=2, addr_id=2),
    Record(bld_id=3, addr_id=3),
    Record(bld_id=4, addr_id=4),
]

SHAPES = {
    # Clipped corner, reference:
    # https://n.maps.yandex.ru/-/CCUmVZCKPC, stripped of details.
    1: b"01030000000100000009000000523d34548ebf4240ffee9eb5bbb54b40018fdd426dbf4240823b09d2b0b54b40df8a82e862bf4240c823252ea1b54b40a98dc1f56ebf424059b0f75c92b54b400c1a74ee68bf4240"
       b"748ce71091b54b402ece481f5bbf4240d8c03f6da0b54b40ecaba84a66bf424051512565b3b54b40e4a75ad388bf4240e6dd986bbfb54b40523d34548ebf4240ffee9eb5bbb54b40",
    # Obtuse corner, reference:
    # https://n.maps.yandex.ru/-/CCUmVZTioA
    2: b"0103000000010000000700000057718c5f96635040019a73b6618f4c40e32a604ca463504058a1b392778f4c400731cdfda063504062cc192e8b8f4c408e10faa6a4635040e55456c68b8f4c40579de42da8635040"
       b"ba955372768f4c40b6bbbb5999635040146516715f8f4c4057718c5f96635040019a73b6618f4c40",
    # Horseshoe-like, reference:
    # https://n.maps.yandex.ru/-/CCUmVZhzhD, stripped of details.
    3: b"010300000001000000090000001f58b6584fdf424081abc0b914d14b40c7c775f357df42401a3826dffad04b4019208fe877df42401c299016fdd04b40b369a25879df4240780b8752f9d04b40b553576452df4240"
       b"f50838fbf8d04b4001de02094adf4240fdf67a9916d14b40ff818b785fdf42405b9c15771ad14b40f88290d860df4240df4520dd16d14b401f58b6584fdf424081abc0b914d14b40",
}

BLD_GEOM = [
    Record(bld_id=1, shape=SHAPES[1]),
    Record(bld_id=2, shape=SHAPES[2]),
    Record(bld_id=3, shape=SHAPES[3]),
]


# -------- Output --------

JOIN_ENTRANCES_WITH_COORDS_OUTPUT = [
    Record(ft_id=1, x=37.496308, y=55.419728),
    Record(ft_id=2, x=37.495503, y=55.418601),
    Record(ft_id=3, x=65.556745, y=57.121307),
    Record(ft_id=4, x=37.744991, y=55.633501),
    Record(ft_id=5, x=37.745011, y=55.633487),
    Record(ft_id=6, x=37.745036, y=55.633449),
]

JOIN_ENTRANCES_WITH_NAMES_OUTPUT = [
    Record(ft_id=1, name=b"1"),
    Record(ft_id=2, name=None),
    Record(ft_id=3, name=b"3"),
    Record(ft_id=4, name=b"4"),
    Record(ft_id=5, name=b"5"),
    Record(ft_id=6, name=b"6"),
]

JOIN_ENTRANCES_WITH_BUILDINGS_OUTPUT = [
    Record(ft_id=1, bld_id=1, shape=SHAPES[1], bld_lat=55.419372, bld_lon=37.495166, ghash6=b"ucfem0"),
    Record(ft_id=2, bld_id=1, shape=SHAPES[1], bld_lat=55.419372, bld_lon=37.495166, ghash6=b"ucfem0"),
    Record(ft_id=3, bld_id=2, shape=SHAPES[2], bld_lat=57.120959, bld_lon=65.557045, ghash6=b"v6nsgy"),
    Record(ft_id=4, bld_id=3, shape=SHAPES[3], bld_lat=55.633184, bld_lon=37.744686, ghash6=b"ucfu6z"),
    Record(ft_id=5, bld_id=3, shape=SHAPES[3], bld_lat=55.633184, bld_lon=37.744686, ghash6=b"ucfu6z"),
    Record(ft_id=6, bld_id=3, shape=SHAPES[3], bld_lat=55.633184, bld_lon=37.744686, ghash6=b"ucfu6z"),
]

PREPARE_ENTRANCES_OUTPUT = [
    Record(
        bld_id=1,
        bld_lat=55.419372,
        bld_lon=37.495166,
        entrances=[
            {b"lat": 55.418601, b"lon": 37.495503},
            {b"lat": 55.419728, b"lon": 37.496308, b"name": b"1"},
        ],
        ghash6=b"ucfem0",
    ),
    Record(
        bld_id=2,
        bld_lat=57.120959,
        bld_lon=65.557045,
        entrances=[
            {b"lat": 57.121307, b"lon": 65.556745, b"name": b"3"},
        ],
        ghash6=b"v6nsgy",
    ),
    Record(
        bld_id=3,
        bld_lat=55.633184,
        bld_lon=37.744686,
        entrances=[
            {b"lat": 55.633501, b"lon": 37.744991, b"name": b"4"},
            {b"lat": 55.633487, b"lon": 37.745011, b"name": b"5"},
        ],
        ghash6=b"ucfu6z",
    ),
]
