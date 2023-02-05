from codecs import decode
from maps.wikimap.feedback.pushes.entrances.prepare_entrances.lib.prepare_entrances import (
    alphanumeric_key,
    prepare_entrances,
    FT,
    FT_NM,
    FT_ADDR,
    FT_CENTER,
    NODE,
    BLD,
    BLD_ADDR,
    BLD_GEOM,
)
from maps.wikimap.feedback.pushes.entrances.prepare_entrances.tests.lib import (
    data,
)
from maps.wikimap.feedback.pushes.helpers import helpers
from nile.api.v1.clusters import MockYQLCluster
from nile.api.v1.local import StreamSource, ListSink
from shapely import (
    geometry,
    wkb,
)
import pytest


@pytest.fixture
def job():
    cluster = MockYQLCluster()
    job = cluster.job()
    prepare_entrances(job, "fake_ymapsdf_path", "fake_output_path")
    return job


def test_join_entrances_with_coords(job):
    output = []
    job.local_run(
        sources={
            FT:                       StreamSource(data.FT),
            FT_CENTER:                StreamSource(data.FT_CENTER),
            NODE:                     StreamSource(data.NODE),
        },
        sinks={
            "with_coords":  ListSink(output),
        },
    )
    helpers.compare_records_lists(
        sorted(output),
        sorted(data.JOIN_ENTRANCES_WITH_COORDS_OUTPUT),
    )


def test_join_entrances_with_names(job):
    output = []
    job.local_run(
        sources={
            FT:                       StreamSource(data.FT),
            FT_NM:                    StreamSource(data.FT_NM),
        },
        sinks={
            "with_names":  ListSink(output),
        },
    )
    helpers.compare_records_lists(
        sorted(output),
        sorted(data.JOIN_ENTRANCES_WITH_NAMES_OUTPUT),
    )


def test_buildings(job):
    output = []
    job.local_run(
        sources={
            BLD:               StreamSource(data.BLD),
            BLD_ADDR:          StreamSource(data.BLD_ADDR),
        },
        sinks={
            "buildings":  ListSink(output),
        },
    )

    helpers.compare_records_lists(
        sorted(output),
        sorted(data.BUILDINGS),
    )


def test_join_entrances_with_buildings(job):
    output = []
    job.local_run(
        sources={
            FT:                StreamSource(data.FT),
            FT_ADDR:           StreamSource(data.FT_ADDR),
            "buildings":       StreamSource(data.BUILDINGS),
            BLD_GEOM:          StreamSource(data.BLD_GEOM),
        },
        sinks={
            "with_buildings":  ListSink(output),
        },
    )

    helpers.compare_records_lists(
        sorted(output),
        sorted(data.JOIN_ENTRANCES_WITH_BUILDINGS_OUTPUT),
        ignored_fields={"bld_lon", "bld_lat", "shape"},
    )

    for record in output:
        point = geometry.Point(record["bld_lon"], record["bld_lat"])
        shape = wkb.loads(decode(data.SHAPES[record["bld_id"]], "hex"))
        assert shape.contains(point), \
                f"Building of shape {shape} does not contain {point}"

    for record, expected in zip(
            sorted(output),
            sorted(data.JOIN_ENTRANCES_WITH_BUILDINGS_OUTPUT)):
        shape = wkb.loads(decode(record["shape"], "hex"))
        expected_shape = wkb.loads(decode(expected["shape"], "hex"))
        # Allow a bit of variation.
        expected_shape = expected_shape.buffer(1e-5)
        assert expected_shape.contains(shape)


def test_prepare_entrances(job):
    output = []
    job.local_run(
        sources={
            "with_coords":
                StreamSource(data.JOIN_ENTRANCES_WITH_COORDS_OUTPUT),
            "with_names":
                StreamSource(data.JOIN_ENTRANCES_WITH_NAMES_OUTPUT),
            "with_buildings":
                StreamSource(data.JOIN_ENTRANCES_WITH_BUILDINGS_OUTPUT),
        },
        sinks={
            "output": ListSink(output),
        },
    )

    helpers.compare_records_lists(
        sorted(output),
        sorted(data.PREPARE_ENTRANCES_OUTPUT),
    )


def test_alphanumeric_key():
    assert alphanumeric_key({"name": b"123asd1fg123"}) == [b"", 123, b"asd", 1, b"fg", 123, b""]
    assert alphanumeric_key({}) == []
    assert alphanumeric_key({}) < alphanumeric_key({"name": b"1"})
    assert alphanumeric_key({"name": bytes("1а", "utf-8")}) < alphanumeric_key({"name": bytes("1б", "utf-8")})
    assert alphanumeric_key({"name": bytes("1б", "utf-8")}) < alphanumeric_key({"name": b"10"})
