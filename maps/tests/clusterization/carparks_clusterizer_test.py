from mock import patch, Mock, ANY
import pytest
import random

from collections import namedtuple
from contextlib import contextmanager
from sklearn import cluster as skcluster

from maps.carparks.tools.carparks_miner.lib.clusterization import (
    mst_clusterizer, snapping)

import maps.carparks.tools.carparks_miner.lib.clusterization.carparks_clusterizer \
    as tested_module


DBSCAN = namedtuple('DBSCAN', ['points', 'return_value'])
MSTClusterizer = namedtuple('MSTClusterizer', ['points', 'return_value', 'base_area'])


class TestCarparksClusterizer():
    def mock_fit_predict(self, input_points, return_value):
        clusterizer = Mock()

        clusterizer.fit_predict = Mock(
            side_effect=lambda points: return_value if points == input_points else None)

        return clusterizer

    @contextmanager
    def mock_clusterizers(self, dbscan, mst=None, points_to_keep=None):
        patch_dbscan = patch.object(
            skcluster, 'DBSCAN',
            Mock(return_value=self.mock_fit_predict(
                [point.original_point() for point in dbscan.points],
                dbscan.return_value))
        )

        patch_dbscan.start()

        if mst:
            def side_effect_mst_clusterizer(metric, base_area, edge_base_length):
                if mst.base_area is not None and mst.base_area != base_area:
                    raise RuntimeError('Unexpected base_area')
                return self.mock_fit_predict(mst.points, mst.return_value)

            patch_mst_clusterizer = patch.object(
                mst_clusterizer, 'MSTClusterizer',
                Mock(side_effect=side_effect_mst_clusterizer)
            )

            patch_mst_clusterizer.start()

        if points_to_keep:
            patch_random_sample = patch.object(random, 'sample', return_value=points_to_keep)

            patch_random_sample.start()

        yield

        patch.stopall()

    @pytest.fixture()
    def mock_carparks_clusterizer(self):
        self._clusterizer = tested_module.CarparksClusterizer()

    def test_simple_clusterization(self, mock_carparks_clusterizer):
        points = [snapping.SnappedPoint(i, j) for i in range(3) for j in range(3)]

        db = DBSCAN(points=points, return_value=[0] * 9)
        mst = MSTClusterizer(points=points, return_value=[0] * 9, base_area=None)

        with self.mock_clusterizers(db, mst):
            assert self._clusterizer.fit_predict(points) == [0] * 9

    def test_forbidden_points_are_not_clusterized(self, mock_carparks_clusterizer):
        points = [snapping.SnappedPoint(i, j, clusterize=True if i > 0 else False)
                  for i in range(3) for j in range(3)]

        db = DBSCAN(points=[point for point in points if point.clusterize],
                    return_value=[0] * 6)
        mst = MSTClusterizer(points=ANY, return_value=[0] * 6, base_area=None)

        with self.mock_clusterizers(db, mst):
            expected_clusters = [-2 if i == 0 else 0
                                 for i in range(3) for j in range(3)]
            assert self._clusterizer.fit_predict(points) == expected_clusters

    def test_extra_points_are_not_clusterized(self, mock_carparks_clusterizer):
        points = [snapping.SnappedPoint(i, j) for i in range(3) for j in range(3)]

        points_to_keep = [0, 2, 3, 5, 6, 7, 8]

        db = DBSCAN(points=[points[i] for i in points_to_keep], return_value=[0] * 7)
        mst = MSTClusterizer(points=[points[i] for i in points_to_keep],
                             return_value=[0] * 7, base_area=None)

        with self.mock_clusterizers(db, mst, points_to_keep):
            self._clusterizer.MAX_POST_SNAP_POINTS = 7
            expected_clusters = [0 if i in points_to_keep else -3
                                 for i in range(9)]
            assert self._clusterizer.fit_predict(points) == expected_clusters

    def test_outlier_clusterizer_uses_original_point(self, mock_carparks_clusterizer):
        points = [snapping.SnappedPoint(i, j, original_x=i-1) for i in range(3) for j in range(3)]

        db = DBSCAN(points=points, return_value=[0] * 9)
        mst = MSTClusterizer(points=points, return_value=[0] * 9, base_area=None)

        with self.mock_clusterizers(db, mst):
            assert self._clusterizer.fit_predict(points) == [0] * 9

    def test_clusterization_with_outliers(self, mock_carparks_clusterizer):
        points = [snapping.SnappedPoint(i, j) for i in range(3) for j in range(3)]

        db = DBSCAN(points=points, return_value=[-1 if x % 3 == 0 else 0 for x in range(9)])
        mst = MSTClusterizer(points=ANY, return_value=[0] * 6, base_area=None)

        with self.mock_clusterizers(db, mst):
            expected_clusters = [-1 if x % 3 == 0 else 0 for x in range(9)]
            assert self._clusterizer.fit_predict(points) == expected_clusters

    def test_outliers_clusters_are_merged(self, mock_carparks_clusterizer):
        points = [snapping.SnappedPoint(i, j) for i in range(3) for j in range(3)]

        db = DBSCAN(points=points, return_value=[x % 2 for x in range(9)])
        mst = MSTClusterizer(points=points, return_value=[0] * 9, base_area=None)

        with self.mock_clusterizers(db, mst):
            assert self._clusterizer.fit_predict(points) == [0] * 9

    def test_final_clusters_are_accounted_for(self, mock_carparks_clusterizer):
        points = [snapping.SnappedPoint(i, j) for i in range(3) for j in range(3)]

        db = DBSCAN(points=points, return_value=[0] * 9)
        mst = MSTClusterizer(points=points, return_value=[x % 3 - 1 for x in range(9)],
                             base_area=None)

        with self.mock_clusterizers(db, mst):
            assert self._clusterizer.fit_predict(points) == [x % 3 - 1 for x in range(9)]

    def test_base_area(self, mock_carparks_clusterizer):
        points = [snapping.SnappedPoint(i, j) for i in range(3) for j in range(3)]

        db = DBSCAN(points=points, return_value=[0] * 9)
        mst = MSTClusterizer(points=points, return_value=[x % 3 - 1 for x in range(9)],
                             base_area=137)

        with self.mock_clusterizers(db, mst):
            self._clusterizer._calculate_base_area = lambda x, y: 137
            assert self._clusterizer.fit_predict(points) == [x % 3 - 1 for x in range(9)]

    def test_too_few_points(self, mock_carparks_clusterizer):
        points = [snapping.SnappedPoint(i, 1) for i in range(2)]

        assert self._clusterizer.fit_predict(points) == [0] * 2

    def test_too_few_points_after_outliers(self, mock_carparks_clusterizer):
        points = [snapping.SnappedPoint(i, 1) for i in range(10)]

        db = DBSCAN(points=points, return_value=[-1] * 8 + [0] * 2)

        with self.mock_clusterizers(db):
            assert self._clusterizer.fit_predict(points) == [-1] * 8 + [0] * 2
