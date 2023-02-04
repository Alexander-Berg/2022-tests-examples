from mock import patch, Mock
import pytest

from collections import namedtuple

import maps.libs.snap as snap
import yandex.maps.road_graph.road_graph as rg

from maps.carparks.tools.carparks_miner.lib.clusterization import geotools
from maps.carparks.tools.carparks_miner.lib.clusterization import barrier_detector as tested_module


# Enables simple mocking of different rg calls
EdgeId = namedtuple('EdgeId', ['edge_id'])
Category = namedtuple('Category', ['category'])
Source = namedtuple('Source', ['source'])
Id = namedtuple('Id', ['id'])


class TestBarrierDetector():
    @pytest.fixture()
    def mock_detector(self):
        self._road_graph = Mock()
        self._rtree_fb = Mock()

        self._detector = tested_module.BarrierDetector(
            self._road_graph, self._rtree_fb)

    @staticmethod
    def mock_vertex_geometry(edge_id):
        return 'vertex{}geometry'.format(edge_id)

    def mock_road_graph_functions(self, base, reverse={}, edge_data={},
                                  edge={}, in_edges={}, access_pass={},
                                  forbidden_turn={}):
        self._road_graph.vertex_geometry = Mock(
            side_effect=self.mock_vertex_geometry)
        self._road_graph.base = Mock(side_effect=base.get)
        self._road_graph.reverse = Mock(side_effect=reverse.get)
        self._road_graph.edge_data = Mock(side_effect=edge_data.get)
        self._road_graph.edge = Mock(side_effect=edge.get)
        self._road_graph.in_edges = Mock(side_effect=in_edges.get)
        self._road_graph.is_access_pass_for = Mock(
            side_effect=lambda start, finish, id: access_pass[(start, finish, id)])
        self._road_graph.is_forbidden_turn = Mock(
            side_effect=lambda start, finish, id: forbidden_turn[(start, finish, id)])

    @patch.object(snap, 'snap_point', Mock(return_value=(None, EdgeId(5))))
    def test_snap_to_important_road(self, mock_detector):
        point = (10, 20)

        self.mock_road_graph_functions(
            base={5: 5},
            reverse={5: None},
            edge_data={5: Category(4)}
        )

        assert not self._detector.is_behind_barrier(point)

    @patch.object(snap, 'snap_point', Mock(return_value=(None, EdgeId(5))))
    @patch.object(geotools, 'fast_geodistance', Mock(side_effect=[10]))
    def test_way_to_important_road(self, mock_detector):
        point = (10, 20)

        self.mock_road_graph_functions(
            base={5: 5},
            reverse={5: None},
            edge_data={5: Category(8), 8: Category(4)},
            edge={5: Source(12)},
            in_edges={12: [Id(8)]},
            access_pass={(8, 5, rg.AccessId.Automobile): False},
            forbidden_turn={(8, 5, rg.AccessId.Automobile): False}
        )

        assert not self._detector.is_behind_barrier(point)

    @patch.object(snap, 'snap_point', Mock(return_value=(None, EdgeId(5))))
    @patch.object(geotools, 'fast_geodistance', Mock(side_effect=[10, 1000]))
    def test_way_to_far_away_point(self, mock_detector):
        point = (10, 20)

        self.mock_road_graph_functions(
            base={5: 5},
            reverse={5: None},
            edge_data={5: Category(8), 8: Category(4)},
            edge={5: Source(12), 8: Source(14)},
            in_edges={12: [Id(8)]},
            access_pass={(8, 5, rg.AccessId.Automobile): False},
            forbidden_turn={(8, 5, rg.AccessId.Automobile): False}
        )

        assert not self._detector.is_behind_barrier(point)

    @patch.object(snap, 'snap_point', Mock(return_value=(None, EdgeId(5))))
    @patch.object(geotools, 'fast_geodistance', Mock(side_effect=[10, 10]))
    def test_reverse_edge(self, mock_detector):
        point = (10, 20)

        self.mock_road_graph_functions(
            base={5: 5},
            reverse={5: 6},
            edge_data={5: Category(8), 6: Category(8), 8: Category(4)},
            edge={5: Source(12), 6: Source(13)},
            in_edges={12: [], 13: [Id(8)]},
            access_pass={(8, 6, rg.AccessId.Automobile): False},
            forbidden_turn={(8, 6, rg.AccessId.Automobile): False}
        )

        assert not self._detector.is_behind_barrier(point)

    @patch.object(snap, 'snap_point', Mock(return_value=(None, EdgeId(5))))
    @patch.object(geotools, 'fast_geodistance', Mock(side_effect=[10, 10]))
    def test_different_base(self, mock_detector):
        point = (10, 20)

        self.mock_road_graph_functions(
            base={5: 6},
            reverse={5: None},
            edge_data={5: Category(8), 6: Category(8), 8: Category(4)},
            edge={5: Source(12), 6: Source(13)},
            in_edges={12: [], 13: [Id(8)]},
            access_pass={(8, 6, rg.AccessId.Automobile): False},
            forbidden_turn={(8, 6, rg.AccessId.Automobile): False}
        )

        assert not self._detector.is_behind_barrier(point)

    @patch.object(snap, 'snap_point', Mock(return_value=(None, EdgeId(5))))
    def test_no_base(self, mock_detector):
        point = (10, 20)

        self.mock_road_graph_functions(
            base={5: None},
            edge_data={5: Category(4)}
        )

        assert not self._detector.is_behind_barrier(point)

    @patch.object(snap, 'snap_point', Mock(return_value=(None, EdgeId(5))))
    @patch.object(geotools, 'fast_geodistance', Mock(side_effect=[10]))
    def test_access_pass_blocks_path(self, mock_detector):
        point = (10, 20)

        self.mock_road_graph_functions(
            base={5: 5},
            reverse={5: None},
            edge_data={5: Category(8)},
            edge={5: Source(12)},
            in_edges={12: [Id(8)]},
            access_pass={(8, 5, rg.AccessId.Automobile): True}
        )

        assert self._detector.is_behind_barrier(point)

    @patch.object(snap, 'snap_point', Mock(return_value=(None, EdgeId(5))))
    @patch.object(geotools, 'fast_geodistance', Mock(side_effect=[10]))
    def test_forbidden_turn_blocks_road(self, mock_detector):
        point = (10, 20)

        self.mock_road_graph_functions(
            base={5: 5},
            reverse={5: None},
            edge_data={5: Category(8)},
            edge={5: Source(12)},
            in_edges={12: [Id(8)]},
            access_pass={(8, 5, rg.AccessId.Automobile): False},
            forbidden_turn={(8, 5, rg.AccessId.Automobile): True}
        )

        assert self._detector.is_behind_barrier(point)

    @patch.object(snap, 'snap_point', Mock(return_value=(None, EdgeId(5))))
    @patch.object(geotools, 'fast_geodistance', Mock(side_effect=[10, 10]))
    def test_loop(self, mock_detector):
        point = (10, 20)

        self.mock_road_graph_functions(
            base={5: 5},
            reverse={5: None},
            edge_data={5: Category(8), 8: Category(8)},
            edge={5: Source(12), 8: Source(14)},
            in_edges={12: [Id(8)], 14: [Id(5)]},
            access_pass={
                (8, 5, rg.AccessId.Automobile): False,
                (5, 8, rg.AccessId.Automobile): False
            },
            forbidden_turn={
                (8, 5, rg.AccessId.Automobile): False,
                (5, 8, rg.AccessId.Automobile): False
            }
        )

        assert self._detector.is_behind_barrier(point)

    @patch.object(snap, 'snap_point', Mock(return_value=(None, EdgeId(5))))
    @patch.object(geotools, 'fast_geodistance', Mock(side_effect=[10, 10]))
    def test_branching(self, mock_detector):
        point = (10, 20)

        self.mock_road_graph_functions(
            base={5: 5},
            reverse={5: None},
            edge_data={5: Category(8), 8: Category(8), 9: Category(2)},
            edge={5: Source(12), 8: Source(14)},
            in_edges={12: [Id(8), Id(9)], 14: []},
            access_pass={
                (8, 5, rg.AccessId.Automobile): False,
                (9, 5, rg.AccessId.Automobile): False
            },
            forbidden_turn={
                (8, 5, rg.AccessId.Automobile): False,
                (9, 5, rg.AccessId.Automobile): False
            }
        )

        assert not self._detector.is_behind_barrier(point)

    @patch.object(snap, 'snap_point', Mock(return_value=None))
    def test_cant_snap(self, mock_detector):
        point = (10, 20)

        assert not self._detector.is_behind_barrier(point)
