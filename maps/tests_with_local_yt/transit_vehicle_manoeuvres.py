import os

import pytest

from mapreduce.yt.python.yt_stuff import YtConfig
from maps.libs.road_graph_import_yt.pylib.graph_import import build_transit_vehicle_manoeuvres_table


@pytest.fixture(scope="module")
def yt_config(request):
    return YtConfig(
        enable_debug_logging=True,
    )


def make_edge_dict(e_id, fc, f_v_id, t_v_id):
    return {
        "e_id": e_id,
        "t_zlev": 0,
        "rd_el_id": 0,
        "reversed": 0,
        "t_rd_jc_id": 0,
        "rev_e_id": 0,
        "f_v_id": f_v_id,
        "f_rd_jc_id": 0,
        "f_zlev": 0,
        "t_v_id": t_v_id,
        "access_id": 61,
        "dr": 0,
        "fc": fc,
        "ferry": 0,
        "fow": 0,
        "isocode": "RU",
        "length": 1
    }


def make_edge_vehicle_restriction_dict(e_id, access_id, max_weight_limit):
    return {
        "e_id": e_id,
        "vehicle_restrictions" : [{
            "access_id": access_id,
            "axle_weight_limit": None,
            "height_limit": None,
            "length_limit": None,
            "level": 0,
            "max_weight_limit": max_weight_limit,
            "min_eco_class": None,
            "pass_id": "None",
            "payload_limit": None,
            "schedules": [],
            "trailer_not_allowed": False,
            "universal_id": "universal_id",
            "weight_limit": None,
            "width_limit": None
        }]
    }


def prepare_data(yt_client):
    yt_client.create("table", "//ymapsdf/edges", recursive=True)
    yt_client.write_table(
        "//ymapsdf/edges",
        [
            make_edge_dict(0, 8, 0, 5),
            make_edge_dict(1, 7, 1, 5),
            make_edge_dict(2, 6, 5, 2),
            make_edge_dict(3, 7, 5, 3),
            make_edge_dict(4, 8, 5, 4),
        ])

    yt_client.create("table", "//ymapsdf/edges_vehicle_restrictions", recursive=True)
    yt_client.write_table(
        "//ymapsdf/edges_vehicle_restrictions",
        [
            make_edge_vehicle_restriction_dict(2, 4, 3.5),
            make_edge_vehicle_restriction_dict(3, 4, 3.5),
            make_edge_vehicle_restriction_dict(4, 4, 3.5)
        ])


def test_transit_vehicle_manoeuvres(yt_stuff):
    yt_client = yt_stuff.get_yt_client()
    prepare_data(yt_client)

    os.environ["YT_PROXY"] = yt_stuff.get_server()

    build_transit_vehicle_manoeuvres_table(
        edgesTable="//ymapsdf/edges",
        edgesVehicleRestrictionsTable="//ymapsdf/edges_vehicle_restrictions",
        outputTransitVehicleManoeuvresTable="//output/transit_vehicle_manoeuvres")

    manoeuvres = []
    for row in yt_client.read_table("//output/transit_vehicle_manoeuvres"):
        manoeuvres.append([row["from_e_id"], row["to_e_id"]])

    assert len(manoeuvres) == 5
    assert ([0, 2] in manoeuvres)
    assert ([0, 3] in manoeuvres)
    assert ([1, 2] in manoeuvres)
    assert ([1, 3] in manoeuvres)
    assert ([1, 4] in manoeuvres)
