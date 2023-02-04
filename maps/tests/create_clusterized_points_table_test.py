from mock import patch, Mock
import copy
import pytest

from contextlib import contextmanager

from maps.carparks.tools.carparks_miner.lib.clusterization import (
    carparks_clusterizer, snapping, barrier_detector, cluster_info_getter)

import maps.carparks.tools.carparks_miner.lib.create_clusterized_points_table \
    as tested_module


class TestClusterizedPointsReducer():
    def assert_results_equal(self, results, expected):
        assert len(results) == len(expected)
        for i in range(len(results)):
            result_keys = sorted(results[i].keys())
            expected_keys = sorted(expected[i].keys())
            assert result_keys == expected_keys
            for key in result_keys:
                result_value = results[i][key]
                expected_value = expected[i][key]
                message = 'Result {}.{} value differs: result {}, expected {}' \
                    .format(i, key, result_value, expected_value)
                message += '\nFull: results {}, expected {}' \
                    .format(results, expected)
                if isinstance(result_value, int) \
                        or isinstance(result_value, float):
                    assert result_value == pytest.approx(expected_value, 1e-7), message
                else:
                    assert result_value == expected_value, message

    @contextmanager
    def mock_global_modules(self, snapper, clusterizer, barrier=None, info_getter=None):
        patch_snapper = patch.object(
            snapping.PointSnapper,
            'snap_points',
            Mock(side_effect=lambda data: snapper.get(tuple(data)))
        )

        patch_clusterizer = patch.object(
            carparks_clusterizer.CarparksClusterizer,
            'fit_predict',
            Mock(side_effect=lambda data: clusterizer.get(tuple(data)))
        )

        if barrier:
            patch_barrier = patch.object(
                barrier_detector.BarrierDetector,
                'is_behind_barrier',
                Mock(side_effect=barrier.get)
            )

            patch_barrier.start()

        if info_getter:
            patch_info_getter = patch.object(
                cluster_info_getter.ClusterInfoGetter,
                'get_cluster_props',
                Mock(side_effect=lambda data: info_getter.get(tuple(data)))
            )

            patch_info_getter.start()

        patch_snapper.start()
        patch_clusterizer.start()

        yield

        patch.stopall()

    def _setup_reducer(self):
        reducer = tested_module.ClusterizeReducer(
            'graph_version', 'carparks_dataset')
        reducer._road_graph = '_road_graph'
        reducer._rtree_fb = '_rtree_fb'
        reducer._carpark_index = 'carpark_index'
        reducer._carpark_info_map = 'carpark_info_map'
        return reducer

    @staticmethod
    def _make_expected_row(key, cluster=1, cluster_props='', size=1,
                           lonlat=(40, 50), target=(40, 50),
                           behind_barrier=False, wait_fraction=0,
                           address=None, kind=None, geometry=None):
        '''
        All coordinates in format (lon, lat)
        '''

        result_dict = {
            'is_organization': key['is_organization'],
            'id': key['id'],
            'address': address,
            'kind': kind,
            'cluster': cluster,
            'size': size,
            'cluster_props': cluster_props,
            'lon': lonlat[0],
            'lat': lonlat[1],
            'target_lon': target[0],
            'target_lat': target[1],
            'behind_barrier': behind_barrier,
            'wait_fraction': wait_fraction
        }

        if geometry is not None:
            result_dict.update({'geometry': geometry})

        return result_dict

    @staticmethod
    def _make_endpoint_row(lonlat=(40, 50), target=(40, 50), end_type='end-track',
                           address=None, kind=None):
        '''
        end_type is in ['end-track', 'wait']
        '''
        return {
            'address': address,
            'kind': kind,
            'lon': lonlat[0],
            'lat': lonlat[1],
            'target_lon': target[0],
            'target_lat': target[1],
            'type': end_type
        }

    def test_one_point(self):
        rows = [self._make_endpoint_row(end_type='wait')]

        key = {'is_organization': False, 'id': 1301}
        snapped_points = [snapping.SnappedPoint(40, 50)]

        mock_snapper = {tuple([(40, 50)]): snapped_points}
        mock_clusterizer = {tuple(snapped_points): [0]}
        mock_barrier = {(40.0, 50.0): False}
        mock_info_getter = {tuple(snapped_points): {'cluster_props': ''}}

        with self.mock_global_modules(
                snapper=mock_snapper, clusterizer=mock_clusterizer,
                barrier=mock_barrier, info_getter=mock_info_getter):
            reducer = self._setup_reducer()
            results = list(reducer(key, rows))

            expected = copy.deepcopy(rows)
            expected[0]['cluster'] = 1
            expected.append(self._make_expected_row(key, wait_fraction=1))
            self.assert_results_equal(results, expected)

    def test_no_clusters_point(self):
        rows = [self._make_endpoint_row(end_type='wait')]
        key = {'is_organization': False, 'id': 1301}
        snapped_points = [snapping.SnappedPoint(40, 50)]

        mock_snapper = {tuple([(40, 50)]): snapped_points}
        mock_clusterizer = {tuple(snapped_points): [-1]}

        with self.mock_global_modules(snapper=mock_snapper, clusterizer=mock_clusterizer):
            reducer = self._setup_reducer()

            results = list(reducer(key, rows))

            expected = copy.deepcopy(rows)
            expected[0]['cluster'] = 0
            self.assert_results_equal(results, expected)

    def test_several_clusters(self):
        rows = [self._make_endpoint_row(lonlat=(40 + 1e-3 * (i // 3), 50 + 1e-3 * (i % 3)),
                                        target=(40, 50)) for i in range(8)]
        key = {'is_organization': False, 'id': 1301}
        snapped_points = [
            snapping.SnappedPoint(40 + 1e-3 * (i // 3), 50 + 1e-3 * (i % 3))
            for i in range(8)]

        mock_snapper = {
            tuple([(40 + 1e-3 * (i // 3), 50 + 1e-3 * (i % 3))
                   for i in range(8)]): snapped_points}
        mock_clusterizer = {tuple(snapped_points): [0] * 4 + [1] * 4}
        mock_barrier = {(40.0, 50.0005): False, tuple((40.0015, 50.001)): False}
        mock_info_getter = {tuple(snapped_points[:4]): {'cluster_props': '1'},
                            tuple(snapped_points[4:]): {'cluster_props': '2'}}

        with self.mock_global_modules(
                snapper=mock_snapper, clusterizer=mock_clusterizer,
                barrier=mock_barrier, info_getter=mock_info_getter):
            reducer = self._setup_reducer()
            reducer.BUILD_CLUSTER_HULL_BUFFER = False

            results = list(reducer(key, rows))

            expected = copy.deepcopy(rows)
            for i in range(4):
                expected[i]['cluster'] = 1
            for i in range(4, 8):
                expected[i]['cluster'] = 2

            polygon_1 = 'POLYGON ((40.0000000000000000 50.0000000000052296,' + \
                        ' 40.0000000000000000 50.0020000000052391,' + \
                        ' 40.0009999999999906 50.0000000000052296,' + \
                        ' 40.0000000000000000 50.0000000000052296))'

            cluster_1 = self._make_expected_row(
                key, cluster=1, size=4, lonlat=(40, 50.0005),
                cluster_props='1', geometry=polygon_1)
            expected.append(cluster_1)

            polygon_2 = 'POLYGON ((40.0020000000000024 50.0000000000052296,' + \
                        ' 40.0009999999999906 50.0010000000052202,' + \
                        ' 40.0009999999999906 50.0020000000052391,' + \
                        ' 40.0020000000000024 50.0010000000052202,' + \
                        ' 40.0020000000000024 50.0000000000052296))'

            cluster_2 = self._make_expected_row(
                key, cluster=2, size=4, lonlat=(40.0015, 50.001),
                cluster_props='2', geometry=polygon_2)
            expected.append(cluster_2)

            self.assert_results_equal(results, expected)

    def test_clusters_ordered_by_size(self):
        rows = [self._make_endpoint_row()] * 3
        key = {'is_organization': False, 'id': 1301}
        snapped_points = [snapping.SnappedPoint(40, 50)] * 3

        mock_snapper = {tuple([(40, 50)] * 3): snapped_points}
        mock_clusterizer = {tuple(snapped_points): [1, 0, 1]}
        mock_barrier = {(40.0, 50.0): False, tuple((40.0, 50.0)): False}
        mock_info_getter = {tuple([snapped_points[0], snapped_points[2]]): {'cluster_props': '1'},
                            tuple([snapped_points[1]]): {'cluster_props': '2'}}

        with self.mock_global_modules(
                snapper=mock_snapper, clusterizer=mock_clusterizer,
                barrier=mock_barrier, info_getter=mock_info_getter):
            reducer = self._setup_reducer()

            results = list(reducer(key, rows))

            expected = copy.deepcopy(rows)
            expected[0]['cluster'] = 1
            expected[1]['cluster'] = 2
            expected[2]['cluster'] = 1
            expected.append(self._make_expected_row(key, cluster=1, size=2, cluster_props='1'))
            expected.append(self._make_expected_row(key, cluster=2, size=1, cluster_props='2'))
            self.assert_results_equal(results, expected)

    def test_snapper_can_change_coordinates(self):
        rows = [self._make_endpoint_row(target=(40.002, 50.002), lonlat=(40.001, 50.001))]
        key = {'is_organization': False, 'id': 1301}
        snapped_points = [snapping.SnappedPoint(40, 50)]

        mock_snapper = {tuple([(40.001, 50.001)]): snapped_points}
        mock_clusterizer = {tuple(snapped_points): [0]}
        mock_barrier = {(40.0, 50.0): False}
        mock_info_getter = {tuple(snapped_points): {'cluster_props': ''}}

        with self.mock_global_modules(
                snapper=mock_snapper, clusterizer=mock_clusterizer,
                barrier=mock_barrier, info_getter=mock_info_getter):
            reducer = self._setup_reducer()

            results = list(reducer(key, rows))

            expected = copy.deepcopy(rows)
            expected[0]['cluster'] = 1
            expected.append(self._make_expected_row(key, target=(40.002, 50.002), lonlat=(40, 50)))
            self.assert_results_equal(results, expected)

    def test_small_clusters_are_not_included(self):
        rows = [self._make_endpoint_row()] * 3
        key = {'is_organization': False, 'id': 1301}
        snapped_points = [snapping.SnappedPoint(40, 50)] * 3

        mock_snapper = {tuple([(40, 50)] * 3): snapped_points}
        mock_clusterizer = {tuple(snapped_points): [1, 0, 1]}
        mock_barrier = {(40.0, 50.0): False}
        mock_info_getter = {tuple([snapped_points[0], snapped_points[2]]): {'cluster_props': ''}}

        with self.mock_global_modules(
                snapper=mock_snapper, clusterizer=mock_clusterizer,
                barrier=mock_barrier, info_getter=mock_info_getter):
            reducer = self._setup_reducer()
            reducer.MIN_CLUSTER_SIZE_FACTOR = 2.0 / 3

            results = list(reducer(key, rows))

            expected = copy.deepcopy(rows)
            expected[0]['cluster'] = 1
            expected[1]['cluster'] = 2
            expected[2]['cluster'] = 1
            expected.append(self._make_expected_row(key, cluster=1, size=2))
            self.assert_results_equal(results, expected)

    def test_too_much_clusters_are_not_included(self):
        rows = [self._make_endpoint_row()] * 3
        key = {'is_organization': False, 'id': 1301}
        snapped_points = [snapping.SnappedPoint(40, 50)] * 3

        mock_snapper = {tuple([(40, 50)] * 3): snapped_points}
        mock_clusterizer = {tuple(snapped_points): [1, 0, 1]}
        mock_barrier = {(40.0, 50.0): False}
        mock_info_getter = {tuple([snapped_points[0], snapped_points[2]]): {'cluster_props': ''}}

        with self.mock_global_modules(
                snapper=mock_snapper, clusterizer=mock_clusterizer,
                barrier=mock_barrier, info_getter=mock_info_getter):
            reducer = self._setup_reducer()
            reducer.MAX_CLUSTERS = 1

            results = list(reducer(key, rows))

            expected = copy.deepcopy(rows)
            expected[0]['cluster'] = 1
            expected[1]['cluster'] = 2
            expected[2]['cluster'] = 1
            expected.append(self._make_expected_row(key, cluster=1, size=2))
            self.assert_results_equal(results, expected)

    def test_wait_fraction(self):
        rows = [self._make_endpoint_row(end_type='wait' if i % 2 else 'end-track')
                for i in range(3)]
        key = {'is_organization': False, 'id': 1301}
        snapped_points = [snapping.SnappedPoint(40, 50)] * 3

        mock_snapper = {tuple([(40, 50)] * 3): snapped_points}
        mock_clusterizer = {tuple(snapped_points): [0] * 3}
        mock_barrier = {(40.0, 50.0): False}
        mock_info_getter = {tuple(snapped_points): {'cluster_props': ''}}

        with self.mock_global_modules(
                snapper=mock_snapper, clusterizer=mock_clusterizer,
                barrier=mock_barrier, info_getter=mock_info_getter):
            reducer = self._setup_reducer()

            results = list(reducer(key, rows))

            expected = copy.deepcopy(rows)
            expected[0]['cluster'] = 1
            expected[1]['cluster'] = 1
            expected[2]['cluster'] = 1
            expected.append(self._make_expected_row(key, size=3, wait_fraction=1.0/3))
            self.assert_results_equal(results, expected)

    @patch.object(barrier_detector.BarrierDetector, 'is_behind_barrier', Mock(side_effect=[False, True]))
    def test_behind_barrier(self):
        rows = [self._make_endpoint_row()] * 3
        key = {'is_organization': False, 'id': 1301}
        snapped_points = [snapping.SnappedPoint(40, 50)] * 3

        mock_snapper = {tuple([(40, 50)] * 3): snapped_points}
        mock_clusterizer = {tuple(snapped_points): [1, 0, 1]}
        mock_info_getter = {tuple([snapped_points[0], snapped_points[2]]): {'cluster_props': ''},
                            tuple([snapped_points[1]]): {'cluster_props': ''}}

        with self.mock_global_modules(
                snapper=mock_snapper, clusterizer=mock_clusterizer, info_getter=mock_info_getter):
            reducer = self._setup_reducer()

            results = list(reducer(key, rows))

            expected = copy.deepcopy(rows)
            expected[0]['cluster'] = 1
            expected[1]['cluster'] = 2
            expected[2]['cluster'] = 1
            expected.append(self._make_expected_row(key, cluster=1, size=2, behind_barrier=False))
            expected.append(self._make_expected_row(key, cluster=2, size=1, behind_barrier=True))
            self.assert_results_equal(results, expected)
