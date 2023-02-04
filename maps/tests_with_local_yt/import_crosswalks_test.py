import os

import yatest.common

from maps.garden.modules.road_graph_build.tests.create_ymapsdf \
    import ymapsdf_utils
from maps.garden.modules.road_graph_build.tests.create_ymapsdf.ymapsdf_types \
    import AccessIdMask, Oneway
from maps.libs.road_graph_import_yt.pylib import graph_import
from maps.libs.ymapsdf.py import rd


#      [5]╍╍[6]
#       ┋    ┊
# [1]══[2]══[3]══[4]
#            ┊
#           [7]
#
# [1]-[2]-[3]-[4] is a two-level automobile road, with road elements on
# z-levels 0 and 1. [2]-[5]-[6]-[3]-[7] is a walking path for pedestrians and
# bicycles; [2]-[5] is on the ground level (zlev=0), and [6]-[3]-[7] is
# elevated above ground (zlev=1).
# For drivers on [1]-[2]-[3]-[4], [2] is a crosswalk on zlev=0, and [3] is a
# crosswalk on zlev=1.


def test_import_crosswalks(yt_stuff):
    graph = ymapsdf_utils.Graph()

    j1 = graph.add_junction(0.000, 0.000)
    j2 = graph.add_junction(0.001, 0.000)
    j3 = graph.add_junction(0.002, 0.000)
    j4 = graph.add_junction(0.003, 0.000)
    j5 = graph.add_junction(0.001, 0.001)
    j6 = graph.add_junction(0.002, 0.001)
    j7 = graph.add_junction(0.002, -0.001)

    graph.add_road_element(
        j1, j2, f_zlev=0, t_zlev=0,
        access_id=AccessIdMask.CAR, oneway=Oneway.Both)
    graph.add_road_element(
        j1, j2, f_zlev=1, t_zlev=1,
        access_id=AccessIdMask.CAR, oneway=Oneway.Both)
    graph.add_road_element(
        j2, j3, f_zlev=0, t_zlev=0,
        access_id=AccessIdMask.CAR, oneway=Oneway.Both)
    graph.add_road_element(
        j2, j3, f_zlev=1, t_zlev=1,
        access_id=AccessIdMask.CAR, oneway=Oneway.Both)
    graph.add_road_element(
        j3, j4, f_zlev=0, t_zlev=0,
        access_id=AccessIdMask.CAR, oneway=Oneway.Both)
    graph.add_road_element(
        j3, j4, f_zlev=1, t_zlev=1,
        access_id=AccessIdMask.CAR, oneway=Oneway.Both)
    graph.add_road_element(
        j2, j5, f_zlev=0, t_zlev=0,
        fc=rd.FunctionalClass.PEDESTRIAN_ROAD,
        fow=rd.FormOfWay.PEDESTRIAN_CROSSWALK,
        access_id=AccessIdMask.PEDESTRIAN | AccessIdMask.BICYCLE,
        oneway=Oneway.Both)
    graph.add_road_element(
        j5, j6, f_zlev=0, t_zlev=1,
        fc=rd.FunctionalClass.PEDESTRIAN_ROAD,
        fow=rd.FormOfWay.PEDESTRIAN_CROSSWALK,
        access_id=AccessIdMask.PEDESTRIAN | AccessIdMask.BICYCLE,
        oneway=Oneway.Both)
    graph.add_road_element(
        j6, j3, f_zlev=1, t_zlev=1,
        fc=rd.FunctionalClass.PEDESTRIAN_ROAD,
        fow=rd.FormOfWay.PEDESTRIAN_CROSSWALK,
        access_id=AccessIdMask.PEDESTRIAN | AccessIdMask.BICYCLE,
        oneway=Oneway.Both)
    graph.add_road_element(
        j3, j7, f_zlev=1, t_zlev=1,
        fc=rd.FunctionalClass.PEDESTRIAN_ROAD,
        fow=rd.FormOfWay.PEDESTRIAN_CROSSWALK,
        access_id=AccessIdMask.PEDESTRIAN | AccessIdMask.BICYCLE,
        oneway=Oneway.Both)

    yt_client = yt_stuff.get_yt_client()
    graph.write(yt_client, "//ymapsdf")

    os.environ["YT_PROXY"] = yt_stuff.get_server()
    graph_import.build_topology(
        desiredAccessId=["auto", "truck", "taxi"],
        geodata6Path=yatest.common.binary_path(
            "geobase/data/v6/geodata6.bin"),
        tzdataZonesBinPath=yatest.common.binary_path(
            "maps/data/test/tzdata/tzdata.tar.gz"),
        rdElTables=["//ymapsdf/rd_el"],
        rdJcTables=["//ymapsdf/rd_jc"],
        outputEdgesTable="//graph-import/edges",
        outputVerticesTable="//graph-import/vertices",
        outputProblematicRdJcTable="//graph-import/problematic_rd_jc")

    assert {
        (e["f_rd_jc_id"], e["f_zlev"], e["t_rd_jc_id"], e["t_zlev"])
        for e in yt_client.read_table("//graph-import/edges")
        if e["ends_with_crosswalk"]
    } == {
        (1, 0, 2, 0),
        (3, 0, 2, 0),
        (2, 1, 3, 1),
        (4, 1, 3, 1),
    }
