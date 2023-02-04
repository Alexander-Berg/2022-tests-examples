"""
This test is intended to verify that ClusterizeReducer works with all
the classes that it uses, and that the results are at least sensible.

Feel free to add more targets here, but remember not to test
for fine details of clusterization results.
The test should pass even if the clusterization algorithm was changed,
it should fail only if the clusterizer does not work at all,
or if it produces a really bad clusterization.

Note that maps/data/test/graph4 is a rather old graph and has old street names,
few barriers, etc.
"""
import unittest
import collections
import json
import copy
import pprint

import yatest.common

from yandex.maps.road_graph.road_graph import RoadGraph
from maps.libs.succinct_rtree.python.succinct_rtree import RtreeImpl
from maps.carparks.libs.common.py import CarparkType
from maps.carparks.libs.geometry.py import CarparkIndex, CarparkInfoMap

from maps.carparks.tools.carparks_miner.lib.create_clusterized_points_table import ClusterizeReducer


def prepare_clusterizer():
    carparks_root = yatest.common.binary_path("maps/carparks/test_data")
    graph_root = yatest.common.binary_path("maps/data/test/graph4")

    carpark_index = CarparkIndex(carparks_root)
    carpark_info_map = CarparkInfoMap(carparks_root)

    road_graph = RoadGraph(graph_root + "/road_graph.fb")
    rtree_fb = RtreeImpl(graph_root + "/rtree.fb", road_graph)

    clusterizer = ClusterizeReducer("graph_version", "carparks_dataset")
    clusterizer._road_graph = road_graph
    clusterizer._rtree_fb = rtree_fb
    clusterizer._carpark_index = carpark_index
    clusterizer._carpark_info_map = carpark_info_map

    return clusterizer


def load_data():
    rows_by_target = collections.defaultdict(list)
    filename = yatest.common.source_path("maps/carparks/tools/carparks_miner/tests_integration/data.json")
    with open(filename) as f:
        for line in f.readlines():
            data = json.loads(line)
            data['address'] = None
            data['kind'] = None
            rows_by_target[(data['is_organization'], data['id'])].append(data)
    return rows_by_target


def run(clusterizer, rows_by_target):
    result_by_target = {}
    for key_tuple in rows_by_target:
        result_by_target[key_tuple] = []
        this_result = list(clusterizer(
            {'is_organization': key_tuple[0], 'id': key_tuple[1]}, rows_by_target[key_tuple]))
        for r in this_result:
            if "geometry" in r:
                r["carpark_info"] = json.loads(r["carpark_info"])
                result_by_target[key_tuple].append(r)
    return result_by_target


class ClusterizationIntegrationTest(unittest.TestCase):
    def _is_value_found_and_expected(self, dictionary, key_path, expected_value):
        current_node = dictionary
        key_parts = key_path.split(".")
        for part in key_parts:
            if not isinstance(current_node, dict) or part not in current_node:
                return False
            current_node = current_node[part]
        return current_node == expected_value

    def _check_result(self, result_clusters, expected_clusters):
        for expected_cluster in expected_clusters:
            suspicious_clusters = []
            found = False
            for cluster in result_clusters:
                matches = 0
                for key in expected_cluster:
                    if self._is_value_found_and_expected(cluster, key, expected_cluster[key]):
                        matches += 1
                if matches == len(expected_cluster):
                    found = True
                    break
                if matches > 0:
                    cluster = copy.deepcopy(cluster)
                    del cluster["geometry"]  # to make output shorter
                    suspicious_clusters.append(cluster)
            if not found:
                message = "Expected cluster {} not found. Suspected clusters: {}" \
                    .format(expected_cluster, pprint.pformat(suspicious_clusters))
                self.fail(message)

    def _check_serpukhov(self, clusters):
        expected_clusters = [
            {"type": "yard",
             "street_name": None,
             "behind_barrier": False},
            {"type": "frontage",
             "street_name": "Оборонная улица",
             "behind_barrier": False},
            {"type": "road",
             "street_name": "Sittsenabivnaya ulitsa",
             "behind_barrier": False},
            {"type": "road",
             "street_name": "Оборонная улица",
             "behind_barrier": False}
        ]
        self._check_result(clusters, expected_clusters)

    def _check_dorogomilovskaya(self, clusters):
        expected_clusters = [
            {"type": "yard",
             "street_name": None,
             "behind_barrier": False},
            {"type": "road",
             "street_name": "Большая Дорогомиловская улица",
             "carpark_info.type": CarparkType.Toll,
             "behind_barrier": False}
        ]
        self._check_result(clusters, expected_clusters)

    def _check_atrium(self, clusters):
        expected_clusters = [
            {"type": "bld",
             "carpark_info.type": CarparkType.TollBld},
            {"type": "area",
             "carpark_info.type": CarparkType.ParkAndRide}
        ]
        self._check_result(clusters, expected_clusters)

    def test_all(self):
        rows_by_target = load_data()
        clusterizer = prepare_clusterizer()

        clusters_by_target = run(clusterizer, rows_by_target)

        # Россия, Московская область, Серпухов, Оборонная улица, 9
        self._check_serpukhov(clusters_by_target[(False, 56590086)])
        # Россия, Москва, Большая Дорогомиловская улица, 16
        self._check_dorogomilovskaya(clusters_by_target[(False, 56710002)])
        # ТРЦ Атриум
        self._check_atrium(clusters_by_target[(True, 1024926429)])
