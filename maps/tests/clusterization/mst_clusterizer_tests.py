import unittest

import maps.carparks.tools.carparks_miner.lib.clusterization.mst_clusterizer as tested_module

from maps.carparks.tools.carparks_miner.lib.clusterization import geotools


class MSTClusterizerTests(unittest.TestCase):
    def test_one_point(self):
        points = [(1, 1)]
        clusterizer = tested_module.MSTClusterizer(geotools.fast_geodistance, 1, 1)

        result = clusterizer.fit_predict(points)

        self.assertEqual(result, [0])

    def test_nearby_points_are_merged(self):
        points = [(1, 1),
                  (1 + 1e-6, 1 + 1e-6)]
        clusterizer = tested_module.MSTClusterizer(geotools.fast_geodistance, 1, 1)

        result = clusterizer.fit_predict(points)

        self.assertEqual(result, [0, 0])

    def test_far_away_points_are_not_merged(self):
        points = [(1, 1),
                  (10, 10)]
        clusterizer = tested_module.MSTClusterizer(geotools.fast_geodistance, 1, 1)

        result = clusterizer.fit_predict(points)

        self.assertEqual(result, [0, 1])

    def test_many_clusters(self):
        points = [(1, 1),
                  (10, 10),
                  (20, 20),
                  (10, 10),
                  (10, 10),
                  (20, 20)]
        clusterizer = tested_module.MSTClusterizer(geotools.fast_geodistance, 1, 1)

        result = clusterizer.fit_predict(points)

        self.assertEqual(result, [0, 1, 2, 1, 1, 2])
