import os

import yatest.common

from maps.garden.modules.road_graph_build.tests.create_ymapsdf \
    import ymapsdf_utils
from maps.garden.modules.road_graph_build.tests.create_ymapsdf.ymapsdf_types \
    import Oneway, RoadConditionType

from maps.libs.road_graph_import_yt.pylib import graph_import


def test_one_edge_manoeuvres(yt_stuff):
    condition_types = [
        (RoadConditionType.BorderControl, graph_import.ConditionType.BORDER_CROSSING_POST),
        (RoadConditionType.RailroadCrossing, graph_import.ConditionType.RAILROAD_CROSSING),
        (RoadConditionType.SpeedBump, graph_import.ConditionType.SPEED_BUMP),
        (RoadConditionType.TollBooth, graph_import.ConditionType.TOLL_POST),
        (RoadConditionType.TrafficLight, graph_import.ConditionType.TRAFFIC_LIGHT),
    ]

    graph = ymapsdf_utils.Graph()

    js = []
    es = []
    for i, (road_condition_type, _) in enumerate(condition_types):
        # Create a subgraph [1]<->[2]<->[3] for each condition type
        j1, j2, j3 = (graph.add_junction(0.001 * k, 0.001 * i) for k in range(3))
        js.append((j1, j2, j3))

        e12 = graph.add_road_element(j1, j2, oneway=Oneway.Both)
        e23 = graph.add_road_element(j2, j3, oneway=Oneway.Both)
        es.append((e12, e23))

        graph.add_road_condition(road_condition_type, [e12], first_junction=j2)
        graph.add_road_condition(road_condition_type, [e23], first_junction=j2)

    yt_client = yt_stuff.get_yt_client()
    graph.write(yt_client, "//ymapsdf")

    yt_client.create("table", "//test/empty_table", recursive=True, force=True)

    os.environ["YT_PROXY"] = yt_stuff.get_server()
    graph_import.build_topology(
        desiredAccessId=["auto"],
        geodata6Path=yatest.common.binary_path(
            "geobase/data/v6/geodata6.bin"),
        tzdataZonesBinPath=yatest.common.binary_path(
            "maps/data/test/tzdata/tzdata.tar.gz"),
        rdElTables=["//ymapsdf/rd_el"],
        rdJcTables=["//ymapsdf/rd_jc"],
        outputEdgesTable="//graph-import/edges",
        outputVerticesTable="//graph-import/vertices",
        outputProblematicRdJcTable="//graph-import/problematic_rd_jc")

    graph_import.build_manoeuvres_table(
        desiredAccessId=["auto"],
        condTables=["//ymapsdf/cond"],
        condRdSeqTables=["//ymapsdf/cond_rd_seq"],
        condDtTables=["//ymapsdf/cond_dt"],
        verticesTable="//graph-import/vertices",
        edgesTable="//graph-import/edges",
        condVehicleRestrictionsTable="//test/empty_table",
        outputManoeuvresTable="//graph-import/manoeuvres")

    for i, (_, graph_import_condition_type) in enumerate(condition_types):
        graph_import.filter_one_edge_manoeuvres(
            manoeuvresTable="//graph-import/manoeuvres",
            outputEdgeIdsTable=f"//test/e_ids_{i}",
            desiredConditionType=graph_import_condition_type)

    edge_junctions = {
        row["e_id"]: (row["f_rd_jc_id"], row["t_rd_jc_id"])
        for row in yt_client.read_table("//graph-import/edges")
    }

    for i in range(len(condition_types)):
        assert {
            edge_junctions[e["e_id"]]
            for e in yt_client.read_table(f"//test/e_ids_{i}")
        } == {
            (js[i][0].jc_id, js[i][1].jc_id),
            (js[i][2].jc_id, js[i][1].jc_id),
        }
