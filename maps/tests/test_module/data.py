from maps.garden.modules.road_graph_build.tests.create_ymapsdf import (
    ymapsdf_utils,
    ymapsdf_types as ym
)
from maps.libs.ymapsdf.py.ft_type import FtType
from maps.libs.ymapsdf.py.rd import AccessId

#                           +++++++fence1++++++++
#                           +                   +
# (4)---[6]->(5)---[7]->(6)<--[5]-->(7)         ************
#  ^          |             +                   *          *
#  |         [4]            +        😎         *   lake   *
# [3]         |             +++++++fence1+++++++*          *
#  |          v             +                   *          *
# (1)<--[1]--(2)<--[2]->(3)<--[8]-->(8)         ************
#                           +                   +
#                           +                   +
#                           +++++++fence2++++++++
#
# Junctions at y = 0 are border junctions.
# Junction (6) has forbidden manoeuvre


def create_test_graph(yt_client, output_yt_directory):
    graph = ymapsdf_utils.Graph()

    j1 = graph.add_junction(0.00, 0.00, is_border=True)
    j2 = graph.add_junction(0.01, 0.00, is_border=True)
    j3 = graph.add_junction(0.02, 0.00, is_border=True)
    j4 = graph.add_junction(0.00, 0.01)
    j5 = graph.add_junction(0.01, 0.01)
    j6 = graph.add_junction(0.02, 0.01)
    j7 = graph.add_junction(0.039, 0.01)
    j8 = graph.add_junction(0.039, 0.00)

    e1 = graph.add_road_element(j1, j2, oneway=ym.Oneway.Backward, access_id=AccessId.ALL)
    e2 = graph.add_road_element(j2, j3, oneway=ym.Oneway.Both, access_id=AccessId.ALL)
    e3 = graph.add_road_element(j1, j4, oneway=ym.Oneway.Forward, access_id=AccessId.ALL)
    e4 = graph.add_road_element(
        j2, j5, oneway=ym.Oneway.Backward, lanes=[
            ym.RoadElementLane(direction=ym.LaneDirection.Forward),
            ym.RoadElementLane(direction=ym.LaneDirection.Backward),
        ],
        access_id=AccessId.ALL)
    e5 = graph.add_road_element(j6, j7, oneway=ym.Oneway.Both, access_id=AccessId.ALL)
    e6 = graph.add_road_element(j4, j5, oneway=ym.Oneway.Forward, access_id=AccessId.ALL)
    e7 = graph.add_road_element(j5, j6, oneway=ym.Oneway.Forward, access_id=AccessId.ALL)
    e8 = graph.add_road_element(j3, j8, oneway=ym.Oneway.Both, access_id=AccessId.ALL)

    graph.add_road_condition(
        ym.RoadConditionType.Forbidden,
        [e7, e5],
        access_id=AccessId.PEDESTRIAN
    )

    fence1 = graph.add_edge([
        (0.04, 0.005),
        (0.02, 0.005),
        (0.02, 0.02),
        (0.04, 0.02),
        (0.04, 0.01)
    ])
    fence2 = graph.add_edge([
        (0.04, 0.00),
        (0.04, -0.01),
        (0.02, -0.01),
        (0.02, 0.005)
    ])
    graph.add_map_feature(type_id=FtType.URBAN_ROADNET_FENCE, geometry=fence1)
    graph.add_map_feature(type_id=FtType.URBAN_ROADNET_FENCE, geometry=fence2)

    graph.add_road(
        [e1, e3, e6, e4],
        rd_type=ym.RoadType.NumberedRoute,
        names=[
            ym.Name("the circle", lang="en"),
            ym.Name("кольцо", lang="ru"),
        ])

    graph.add_road(
        [e2, e8],
        rd_type=ym.RoadType.RoadWithinLocality,
        names=[
            ym.Name("to the beach", lang="en")
        ])

    canal_face = graph.add_face([
        (0.04, 0.00),
        (0.05, 0.00),
        (0.05, 0.01),
        (0.04, 0.01),
        (0.04, 0.00),
    ])

    graph.add_map_feature(
        type_id=FtType.HYDRO_RIVER_CANAL,
        names=[
            ym.Name("the canal", lang="en"),
            ym.Name("канал", lang="ru"),
        ],
        geometry=canal_face)

    graph.write(yt_client, output_yt_directory)
