from maps.libs.road_graph_build_yt.pylib import graph_builder


OUTPUT_PATH = "./"
VERSION = "1"
YT_CLUSTER = "hahn"
YT_INPUT_PATH = "//home/maps/users/aadavtyan/yandex_graph"


def main():

    graph_builder.build_graph(
        YT_INPUT_PATH,
        VERSION,
        OUTPUT_PATH + "road_graph.fb",
        OUTPUT_PATH + "edges_persistent_index.fb")

    graph_builder.build_carparks(
        YT_INPUT_PATH,
        OUTPUT_PATH + "carparks.mms.1")

    graph_builder.build_rtree(
        OUTPUT_PATH + "road_graph.fb",
        OUTPUT_PATH + "rtree.fb")

    graph_builder.build_barriers_rtree(
        YT_INPUT_PATH,
        VERSION,
        OUTPUT_PATH + "rtree.fb",
        OUTPUT_PATH + "road_graph.fb",
        OUTPUT_PATH + "routing_barriers_rtree.mms.1",
        graph_builder.BARRIER_SETTINGS_AUTO)
