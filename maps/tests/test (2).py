from maps.wikimap.feedback.pushes.addresses.prepare_buildings.lib.prepare_buildings import (
    prepare_buildings,
    bld_mapper,
    BLD,
    BLD_GEOM,
    BLD_ADDR
)
from maps.wikimap.feedback.pushes.helpers import helpers
from maps.libs.geolib.cython.point import PyPoint2
from nile.api.v1 import (
    clusters,
    local,
    Record
)


BLD_SHAPE1 = helpers.make_ewkb_shape([
    PyPoint2(37.444, 55.444),
    PyPoint2(37.444, 55.446),
    PyPoint2(37.446, 55.446),
    PyPoint2(37.446, 55.444),
])
BLD_AREA1 = 28054.064465745636


BLD_SHAPE2 = helpers.make_ewkb_shape([
    PyPoint2(27.444, 54.444),
    PyPoint2(27.444, 54.446),
    PyPoint2(27.446, 54.446),
    PyPoint2(27.446, 54.444),
])
BLD_AREA2 = 28757.543257881032


def test_mapper():
    records = [
        Record(
            bld_id=1,
            shape=BLD_SHAPE1,
            xmin=37.444,
            xmax=37.446,
            ymin=55.444,
            ymax=55.446,
            addr_id="addr_id1"
        ),
        Record(
            bld_id=2,
            shape=BLD_SHAPE1,
            xmin=37.446,
            xmax=37.448,
            ymin=55.446,
            ymax=55.448,
            addr_id="addr_id2"
        ),
        Record(
            bld_id=3,
            shape=BLD_SHAPE2,
            xmin=27.444,
            xmax=27.446,
            ymin=54.444,
            ymax=54.446
        )
    ]
    helpers.compare_records_lists(
        sorted(list(bld_mapper(records))),
        sorted([
            Record(
                bld_area=BLD_AREA1,
                bld_id=1,
                bld_shape=BLD_SHAPE1,
                ghash6=b"ucfekn",
                ghash9=b'ucfekj6we',
                has_addr=True,
                bld_lat=55.445,
                bld_lon=37.445
            ),
            Record(
                bld_area=BLD_AREA1,
                bld_id=2,
                bld_shape=BLD_SHAPE1,
                ghash6=b"ucfekn",
                ghash9=b'ucfekju44',
                has_addr=True,
                bld_lat=55.447,
                bld_lon=37.447
            ),
            Record(
                bld_area=BLD_AREA2,
                bld_id=3,
                bld_shape=BLD_SHAPE2,
                ghash6=b"u9etb8",
                ghash9=b'u9et8x2v4',
                has_addr=False,
                bld_lat=54.445,
                bld_lon=27.445
            )
        ])
    )


def test_prepare_bld():
    cluster = clusters.MockYQLCluster()
    job = cluster.job()
    job = prepare_buildings(job, "fake_ymapsdf_path", "fake_output_path")
    result = []
    job.local_run(
        sources={
            BLD: local.StreamSource([
                Record(bld_id=1, ft_type_id=101, cond=0),
                Record(bld_id=2, ft_type_id=102, cond=0),
                Record(bld_id=3, ft_type_id=101, cond=0),
            ]),
            BLD_GEOM: local.StreamSource([
                Record(
                    bld_id=1,
                    shape=BLD_SHAPE1,
                    xmin=37.444,
                    xmax=37.446,
                    ymin=55.444,
                    ymax=55.446
                ),
                Record(
                    bld_id=2,
                    shape=BLD_SHAPE1,
                    xmin=37.444,
                    xmax=37.446,
                    ymin=55.444,
                    ymax=55.446
                ),
                Record(
                    bld_id=3,
                    shape=BLD_SHAPE2,
                    xmin=27.444,
                    xmax=27.446,
                    ymin=54.444,
                    ymax=54.446
                )
            ]),
            BLD_ADDR: local.StreamSource([
                Record(bld_id=1, addr_id="addr_id1"),
                Record(bld_id=2, addr_id="addr_id2"),
            ]),
        },
        sinks={
            "output": local.ListSink(result),
        }
    )
    helpers.compare_records_lists(
        sorted(result),
        sorted([
            Record(
                bld_area=BLD_AREA1,
                bld_id=1,
                bld_shape=BLD_SHAPE1,
                ghash6=b"ucfekn",
                ghash9=b'ucfekj6we',
                has_addr=True,
                bld_lat=55.445,
                bld_lon=37.445
            ),
            Record(
                bld_area=BLD_AREA2,
                bld_id=3,
                bld_shape=BLD_SHAPE2,
                ghash6=b"u9etb8",
                ghash9=b'u9et8x2v4',
                has_addr=False,
                bld_lat=54.445,
                bld_lon=27.445
            )
        ])
    )
