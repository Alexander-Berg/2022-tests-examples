import os
import yatest.common
import shutil


GRAPH_PATH = yatest.common.binary_path("maps/data/test/graph3")
BBOX = "37.602822 55.735028 37.635871 55.748792"


def bin(name):
    return yatest.common.binary_path(os.path.join(
        "maps/libs/jams/static-graph/tools",
        name,
        name))


def test_mms_version():
    for file in [
            "data.mms.2",
            "edges_rtree.mms.2",
            "graph_precalc.mms.2",
            "titles.mms.2",
            "topology.mms.2",
            "edges_persistent_index.mms.1"]:
        yatest.common.execute([
            bin("show_mms_version"),
            "--file",
            os.path.join(GRAPH_PATH, file)
        ])


def test_cut_graph():
    yatest.common.execute([
        bin("cut_graph"),
        "--graph-folder", GRAPH_PATH,
        "--bbox", "30 50 30 60",
        "--output-data", "data.mms",
        "--output-topology", "topology.mms",
        "--output-titles", "titles.mms"
    ])


def test_endoscope():
    yatest.common.execute([
        bin("endoscope"),
        os.path.join(GRAPH_PATH, "data.mms.2")
    ])


def create_coverage_dir():
    os.mkdir('coverage_dir')
    shutil.copy(yatest.common.build_path('maps/data/test/geoid/geoid.mms.1'), 'coverage_dir/geoid.mms.1')
    shutil.copy(yatest.common.build_path('maps/data/test/trf/trf.mms.1'), 'coverage_dir/trf.mms.1')


def remove_coverage_dir():
    shutil.rmtree('coverage_dir')


def test_shortest_paths():
    create_coverage_dir()
    for i in [0, 1]:
        with open("precalc-{}".format(i), "wb") as precalc:
            yatest.common.execute([
                bin("build_shortest_paths"),
                "--graph-topology", os.path.join(GRAPH_PATH, "topology.mms.2"),
                "--graph-data", os.path.join(GRAPH_PATH, "data.mms.2"),
                "--part-index", str(i)
            ], stdout=precalc, env={"REGION_MAP_LAYERS_CONFIG": "./coverage_dir/"})
    yatest.common.execute([
        bin("merge_shortest_paths"),
        "--precalc", "precalc-0,precalc-1"
    ])
    remove_coverage_dir()
