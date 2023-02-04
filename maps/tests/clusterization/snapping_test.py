from mock import patch, Mock
import pytest

from collections import namedtuple

import yandex.maps.geolib3 as geolib3
import maps.libs.snap as snap

from maps.carparks.libs.common.py import CarparkType
from maps.carparks.libs.geometry.py import GeometryType

from maps.carparks.tools.carparks_miner.lib.clusterization import snapping as tested_module


SAMPLE_POLYLINE = geolib3.Polyline2()
SAMPLE_POLYLINE.add(geolib3.Point2(15, 25))


class TestChooseNearestEdge():
    @pytest.fixture()
    def mock_graph(self):
        self._road_graph = Mock()

    def mock_polyline(self, points):
        polyline = geolib3.Polyline2()
        for point in points:
            polyline.add(geolib3.Point2(*point))

        data = Mock()
        data.polyline = Mock(return_value=polyline)

        return data

    def test_simple(self, mock_graph):
        point = tested_module.SnappedPoint(10, 20)
        edge_tuples = [(5, None)]
        edge_data = {5 : self.mock_polyline([(11, 21)])}

        self._road_graph.edge_data = Mock(side_effect=edge_data.get)

        nearest_edge = tested_module.choose_nearest_edge(self._road_graph, [point], edge_tuples)

        assert nearest_edge == (5, None)

    def test_nearest_edge_is_chosen(self, mock_graph):
        point = tested_module.SnappedPoint(10, 20)
        edge_tuples = [(5, None), (7, None)]
        edge_data = {5: self.mock_polyline([(12, 22)]),
                     7: self.mock_polyline([(11, 21)])}

        self._road_graph.edge_data = Mock(side_effect=edge_data.get)

        nearest_edge = tested_module.choose_nearest_edge(self._road_graph, [point], edge_tuples)

        assert nearest_edge == (7, None)

    def test_minmax_is_chosen(self, mock_graph):
        points = [tested_module.SnappedPoint(10, 20),
                  tested_module.SnappedPoint(30, 40)]
        edge_tuples = [(5, None), (7, None), (9, None)]
        edge_data = {5: self.mock_polyline([(11, 21)]),
                     7: self.mock_polyline([(12, 22), (32, 42)]),
                     9: self.mock_polyline([(31, 41)])}

        self._road_graph.edge_data = Mock(side_effect=edge_data.get)

        nearest_edge = tested_module.choose_nearest_edge(self._road_graph, points, edge_tuples)

        assert nearest_edge == (7, None)

    def test_road_segments_are_merged(self, mock_graph):
        points = [tested_module.SnappedPoint(10, 20),
                  tested_module.SnappedPoint(30, 40)]
        edge_tuples = [(5, None), (7, None), (9, 1), (11, None), (13, 1)]
        edge_data = {5: self.mock_polyline([(11, 21)]),
                     7: self.mock_polyline([(14, 24), (34, 44)]),
                     9: self.mock_polyline([(12, 22)]),
                     11: self.mock_polyline([(31, 41)]),
                     13: self.mock_polyline([(32, 42)])}

        self._road_graph.edge_data = Mock(side_effect=edge_data.get)

        nearest_edge = tested_module.choose_nearest_edge(self._road_graph, points, edge_tuples)

        assert nearest_edge == (9, 1)

    def test_max_distance(self, mock_graph):
        point = tested_module.SnappedPoint(10, 20)
        edge_tuples = [(5, 0)]
        edge_data = {5: self.mock_polyline([(11, 21)])}

        self._road_graph.edge_data = Mock(side_effect=edge_data.get)

        nearest_edge = tested_module.choose_nearest_edge(self._road_graph, [point], edge_tuples, 153000)

        assert nearest_edge == (5, 0)

        nearest_edge = tested_module.choose_nearest_edge(self._road_graph, [point], edge_tuples, 152000)

        assert nearest_edge is None


Point = namedtuple('Point', ['x', 'y'])
EdgeId = namedtuple('EdgeId', ['edge_id'])
ClosestPoint = namedtuple('ClosestPoint', ['point', 'distance'])
CarparkCommonInfo = namedtuple('CarparkCommonInfo', ['type'])


# we need polyline() to be method, not field
class EdgeData(object):
    def __init__(self, roads, category, polyline=SAMPLE_POLYLINE):
        self.roads = roads
        self.category = category
        self._polyline = polyline

    def polyline(self):
        return self._polyline


class CarparkSnapResult(object):
    def __init__(self, id, type, point, distance):
        self.id = id
        self.type = type
        self.closest_point = ClosestPoint(point, distance)


class CarparkGeometryInfo(object):
    def __init__(self, geometry_type, common_info):
        self.geometry_type = geometry_type
        self.info = common_info


class CarparkInfoType(object):
    FreePolyline = CarparkGeometryInfo(GeometryType.Polyline,
                                       CarparkCommonInfo(CarparkType.Free))
    ProhibitedPolyline = CarparkGeometryInfo(GeometryType.Polyline,
                                             CarparkCommonInfo(CarparkType.Prohibited))
    RestrictedPolyline = CarparkGeometryInfo(GeometryType.Polyline,
                                             CarparkCommonInfo(CarparkType.Restricted))
    RestrictedPolygone = CarparkGeometryInfo(GeometryType.Polygon,
                                             CarparkCommonInfo(CarparkType.Restricted))


class TestPointSnapper():
    @pytest.fixture()
    def mock_snapper(self):
        self._road_graph = Mock()
        self._rtree_fb = Mock()
        self._carpark_index = Mock()
        self._carpark_info_map = dict()

        self._snapper = tested_module.PointSnapper(
            self._road_graph, self._rtree_fb, self._carpark_index, self._carpark_info_map)

    def assert_snapped_points_equal(self, result, expected, index):
        """
        :param snapping.SnappedPoint result:
        :param snapping.SnappedPoint expected:
        """
        for field in result.__dict__:
            result_value = result.__getattribute__(field)
            expected_value = expected.__getattribute__(field)
            message = ('result[{}].{} differs:\n'
                       + 'result={}, expected={}\n'
                       + '(full: result={}, expected={})') \
                .format(index, field, result_value, expected_value, result, expected)
            if isinstance(result_value, float) and isinstance(expected_value, float):
                assert result_value == pytest.approx(expected_value, 1e-7), message
            else:
                assert result_value == expected_value, message

    def _snap_and_check(self, points, expected):
        results = self._snapper.snap_points(points)
        assert len(results) == len(expected)
        for i in range(len(results)):
            self.assert_snapped_points_equal(results[i], expected[i], i)

    def _make_expected(self, snapped_point, original_point,
                       carpark_id=None, carpark_info=None,
                       edge_id=None, edge_info=EdgeData(roads=None, category=None)):
        return tested_module.SnappedPoint(
            x=snapped_point.x, y=snapped_point.y,
            original_x=original_point.x, original_y=original_point.y,
            carpark_id=carpark_id, carpark_type=carpark_info.info.type if carpark_info else None,
            carpark_info=carpark_info.info if carpark_info else None,
            carpark_geometry_type=carpark_info.geometry_type if carpark_info else None,
            edge_id=edge_id, road_id=edge_info.roads[0] if edge_info.roads else None, edge_category=edge_info.category)

    @staticmethod
    def geo_to_point(geopoint):
        return Point(geopoint.x, geopoint.y)

    def mock_find_nearest(self, nearest_index={}):
        self._carpark_index.find_nearest = Mock(
            side_effect=lambda point, value, distance: nearest_index[self.geo_to_point(point)])

    @patch.object(snap, 'snap_point', Mock(return_value=None))
    def test_no_snap(self, mock_snapper):
        point = Point(10, 20)

        self.mock_find_nearest(nearest_index={point: []})

        expected = self._make_expected(snapped_point=point, original_point=point)
        self._snap_and_check(points=[point], expected=[expected])

    @patch.object(snap, 'snap_point', Mock(return_value=(Point(11, 21), EdgeId(5))))
    def test_snap_only_to_edge(self, mock_snapper):
        point, snapped = Point(10, 20), Point(11, 21)
        edge_id, edge = 5, EdgeData(roads=[15], category=8, polyline=None)

        self.mock_find_nearest(nearest_index={point: [], snapped: []})

        edge_id_to_edge = {edge_id: edge}
        self._road_graph.edge_data = Mock(side_effect=edge_id_to_edge.get)

        expected = self._make_expected(
            snapped_point=snapped, original_point=point,
            edge_id=edge_id, edge_info=edge)
        self._snap_and_check(points=[point], expected=[expected])

    @patch.object(snap, 'snap_point', Mock(return_value=(Point(11, 21), EdgeId(5))))
    def test_snap_only_to_edge_without_road(self, mock_snapper):
        point, snapped = Point(10, 20), Point(11, 21)
        edge_id, edge = 5, EdgeData(roads=None, category=8, polyline=None)

        self.mock_find_nearest(nearest_index={point: [], snapped: []})

        edge_id_to_edge = {edge_id: edge}
        self._road_graph.edge_data = Mock(side_effect=edge_id_to_edge.get)

        expected = self._make_expected(
            snapped_point=snapped, original_point=point,
            edge_id=edge_id, edge_info=edge)
        self._snap_and_check(points=[point], expected=[expected])

    @patch.object(snap, 'snap_point', Mock(return_value=None))
    def test_snap_to_prohibited_carpark_without_snapping_to_road(self, mock_snapper):
        point, snapped = Point(10, 20), Point(12, 22)
        carpark_id, carpark_info = 123, CarparkInfoType.ProhibitedPolyline

        self.mock_find_nearest(nearest_index={
            point: [CarparkSnapResult(id=carpark_id, type=carpark_info.info.type,
                                      point=snapped, distance=1)]
        })

        self._carpark_info_map.update({carpark_id: carpark_info})

        expected = self._make_expected(
            snapped_point=snapped, original_point=point,
            carpark_id=carpark_id, carpark_info=carpark_info)
        self._snap_and_check(points=[point], expected=[expected])

    @patch.object(snap, 'snap_point', Mock(return_value=None))
    def test_snap_to_nearest_allowed_carpark(self, mock_snapper):
        point, snapped_1, snapped_2 = Point(10, 20), Point(11, 21), Point(12, 22)
        carpark_id, carpark_info = 123, CarparkInfoType.FreePolyline

        self.mock_find_nearest(nearest_index={
            point: [CarparkSnapResult(id=carpark_id, type=carpark_info.info.type,
                                      point=snapped_1, distance=1),
                    CarparkSnapResult(id=carpark_id, type=carpark_info.info.type,
                                      point=snapped_2, distance=2)]
        })

        self._carpark_info_map.update({carpark_id: carpark_info})

        expected = self._make_expected(
            snapped_point=snapped_1, original_point=point,
            carpark_id=carpark_id, carpark_info=carpark_info)
        self._snap_and_check(points=[point], expected=[expected])

    @patch.object(snap, 'snap_point', Mock(return_value=None))
    def test_restricted_area_is_considered_allowed(self, mock_snapper):
        point, snapped = Point(10, 20), Point(12, 22)
        carpark_id, carpark_info = 123, CarparkInfoType.RestrictedPolygone

        self.mock_find_nearest(nearest_index={
            point: [CarparkSnapResult(id=carpark_id, type=carpark_info.info.type,
                                      point=snapped, distance=1)]
        })

        self._carpark_info_map.update({carpark_id: carpark_info})

        expected = self._make_expected(
            snapped_point=snapped, original_point=point,
            carpark_id=carpark_id, carpark_info=carpark_info)
        self._snap_and_check(points=[point], expected=[expected])

    @patch.object(snap, 'snap_point', Mock(return_value=None))
    def test_restricted_line_is_considered_allowed(self, mock_snapper):
        point, snapped = Point(10, 20), Point(12, 22)
        carpark_id, carpark_info = 123, CarparkInfoType.RestrictedPolyline

        self.mock_find_nearest(nearest_index={
            point: [CarparkSnapResult(id=carpark_id, type=carpark_info.info.type,
                                      point=snapped, distance=1)]
        })

        self._carpark_info_map.update({carpark_id: carpark_info})

        expected = self._make_expected(
            snapped_point=snapped, original_point=point,
            carpark_id=carpark_id, carpark_info=carpark_info)
        self._snap_and_check(points=[point], expected=[expected])

    @patch.object(snap, 'snap_point', Mock(return_value=None))
    def test_snap_with_ignored_carparks(self, mock_snapper):
        point, snapped_1, snapped_2, snapped_3 = \
            Point(10, 20), Point(11, 21), Point(12, 22), Point(13, 23)
        carpark_id_1, carpark_info_1 = 123, CarparkGeometryInfo(
            GeometryType.Point, CarparkCommonInfo(type=CarparkType.Meter))
        carpark_id_2, carpark_info_2 = 124, CarparkGeometryInfo(
            GeometryType.Point, CarparkCommonInfo(type=CarparkType.ControlledZone))
        carpark_id_3, carpark_info_3 = 125, CarparkInfoType.FreePolyline

        self.mock_find_nearest(nearest_index={
            point: [CarparkSnapResult(id=carpark_id_1, type=carpark_info_1.info.type,
                                      point=snapped_1, distance=1),
                    CarparkSnapResult(id=carpark_id_2, type=carpark_info_2.info.type,
                                      point=snapped_2, distance=1),
                    CarparkSnapResult(id=carpark_id_3, type=carpark_info_3.info.type,
                                      point=snapped_3, distance=50)]
        })

        self._carpark_info_map.update({carpark_id_1: carpark_info_1, carpark_id_2: carpark_info_2})

        expected = self._make_expected(snapped_point=point, original_point=point)
        self._snap_and_check(points=[point], expected=[expected])

    @patch.object(snap, 'snap_point', Mock(return_value=None))
    def test_allowed_carpark_overrides_forbidden_if_distance_less_then_one_and_half_times_bigger(self, mock_snapper):
        point, snapped_1, snapped_2 = Point(10, 20), Point(11, 21), Point(12, 22)

        carpark_id_1, carpark_info_1 = 123, CarparkInfoType.ProhibitedPolyline
        carpark_id_2, carpark_info_2 = 124, CarparkInfoType.FreePolyline

        self.mock_find_nearest(nearest_index={
            point: [CarparkSnapResult(id=carpark_id_1, type=carpark_info_1.info.type,
                                      point=snapped_1, distance=3),
                    CarparkSnapResult(id=carpark_id_2, type=carpark_info_2.info.type,
                                      point=snapped_2, distance=4)]
        })

        self._carpark_info_map.update({carpark_id_1: carpark_info_1, carpark_id_2: carpark_info_2})

        expected = self._make_expected(
            snapped_point=snapped_2, original_point=point,
            carpark_id=carpark_id_2, carpark_info=carpark_info_2)
        self._snap_and_check(points=[point], expected=[expected])

    @patch.object(snap, 'snap_point', Mock(return_value=None))
    def test_nearby_forbidden_carpark_overrides_far_away_allowed(self, mock_snapper):
        point, snapped_1, snapped_2 = Point(10, 20), Point(11, 21), Point(12, 22)
        carpark_id_1, carpark_info_1 = 123, CarparkInfoType.ProhibitedPolyline
        carpark_id_2, carpark_info_2 = 124, CarparkInfoType.FreePolyline

        self.mock_find_nearest(nearest_index={
            point: [CarparkSnapResult(id=carpark_id_1, type=carpark_info_1.info.type,
                                      point=snapped_1, distance=1),
                    CarparkSnapResult(id=carpark_id_2, type=carpark_info_2.info.type,
                                      point=snapped_2, distance=10)]
        })

        self._carpark_info_map.update({carpark_id_1: carpark_info_1, carpark_id_2: carpark_info_2})

        expected = self._make_expected(
            snapped_point=snapped_1, original_point=point,
            carpark_id=carpark_id_1, carpark_info=carpark_info_1)
        self._snap_and_check(points=[point], expected=[expected])

    @patch.object(snap, 'snap_point', Mock(return_value=(Point(11, 21), EdgeId(5))))
    def test_if_first_snap_to_carpark_was_unsuccessfull_resnap_from_road(self, mock_snapper):
        point, road, snapped = Point(10, 20), Point(11, 21), Point(12, 22)
        carpark_id, carpark_info = 123, CarparkInfoType.FreePolyline
        edge_id, edge = 5, EdgeData(roads=[15], category=8)

        self.mock_find_nearest(nearest_index={
            point: [],
            road: [CarparkSnapResult(id=carpark_id, type=carpark_info.info.type,
                                     point=snapped, distance=1)]
        })

        edge_id_to_edge = {edge_id: edge}
        self._road_graph.edge_data = Mock(side_effect=edge_id_to_edge.get)

        self._carpark_info_map.update({carpark_id: carpark_info})

        expected = self._make_expected(
            snapped_point=snapped, original_point=point,
            carpark_id=carpark_id, carpark_info=carpark_info,
            edge_id=edge_id, edge_info=edge)
        self._snap_and_check(points=[point], expected=[expected])

    @patch.object(snap, 'snap_point', Mock(side_effect=[
        (Point(10, 20), EdgeId(5)), (Point(30, 40), EdgeId(6)),
        (Point(50, 60), EdgeId(7)), (Point(72, 82), EdgeId(8))]))
    def test_same_carpark_gets_same_edge(self, mock_snapper):
        carpark_id_1, carpark_id_2, carpark_info = 123, 124, CarparkInfoType.FreePolyline

        # first point is snapped to first carpark
        point_1, snapped_1 = Point(10, 20), Point(12, 22)
        edge_id_1, edge_1 = 5, EdgeData(roads=[15], category=8)

        # second point is snapped to second carpark
        point_2, snapped_2 = Point(30, 40), Point(32, 42)
        edge_id_2, edge_2 = 6, EdgeData(roads=[16], category=8)

        # third point is snapped to first carpark, but different edge
        point_3, snapped_3 = Point(50, 60), Point(52, 62)
        edge_id_3, edge_3 = 7, EdgeData(roads=[17], category=4)

        # fourth point is not snapped to carpark
        point_4, snapped_4 = Point(70, 80), Point(72, 82)
        edge_id_4, edge_4 = 8, EdgeData(roads=[18], category=4)

        self.mock_find_nearest(nearest_index={
            point_1: [CarparkSnapResult(id=carpark_id_1, type=carpark_info.info.type,
                                        point=snapped_1, distance=1)],
            point_2: [CarparkSnapResult(id=carpark_id_2, type=carpark_info.info.type,
                                        point=snapped_2, distance=1)],
            point_3: [CarparkSnapResult(id=carpark_id_1, type=carpark_info.info.type,
                                        point=snapped_3, distance=1)],
            point_4: [],
            snapped_4: []
        })

        self._carpark_info_map.update({carpark_id_1: carpark_info, carpark_id_2: carpark_info})

        edge_id_to_edge = {
            edge_id_1: edge_1,
            edge_id_2: edge_2,
            edge_id_3: edge_3,
            edge_id_4: edge_4
        }
        self._road_graph.edge_data = Mock(side_effect=edge_id_to_edge.get)

        expected = [
            self._make_expected(
                snapped_point=snapped_1, original_point=point_1,
                carpark_id=carpark_id_1, carpark_info=carpark_info,
                edge_id=edge_id_1, edge_info=edge_1),
            self._make_expected(
                snapped_point=snapped_2, original_point=point_2,
                carpark_id=carpark_id_2, carpark_info=carpark_info,
                edge_id=edge_id_2, edge_info=edge_2),
            self._make_expected(
                snapped_point=snapped_3, original_point=point_3,
                carpark_id=carpark_id_1, carpark_info=carpark_info,
                # this line below is the most important in this test: here goes edge 5, not edge 7
                edge_id=edge_id_1, edge_info=edge_1),
            self._make_expected(
                snapped_point=snapped_4, original_point=point_4,
                edge_id=edge_id_4, edge_info=edge_4),
        ]
        self._snap_and_check(points=[point_1, point_2, point_3, point_4], expected=expected)

    @patch.object(snap, 'snap_point', Mock(return_value=(Point(10, 20), EdgeId(5))))
    def test_snap_point_from_4_category_road_to_carpark_with_distance_14(self, mock_snapper):
        point, snapped = Point(10, 20), Point(12, 22)

        carpark_id, carpark_info = 123, CarparkInfoType.FreePolyline
        edge_id, edge = 5, EdgeData(roads=[15], category=4)

        self.mock_find_nearest(nearest_index={
            point: [CarparkSnapResult(id=carpark_id, type=carpark_info.info.type,
                                      point=snapped, distance=14)]
        })

        self._carpark_info_map.update({carpark_id: carpark_info})

        edge_id_to_edge = {edge_id: edge}
        self._road_graph.edge_data = Mock(side_effect=edge_id_to_edge.get)

        expected = self._make_expected(
            snapped_point=snapped, original_point=point,
            carpark_id=carpark_id, carpark_info=carpark_info,
            edge_id=edge_id, edge_info=edge)
        self._snap_and_check(points=[point], expected=[expected])

    @patch.object(snap, 'snap_point', Mock(return_value=(Point(10, 20), EdgeId(5))))
    def test_do_not_snap_point_from_8_category_road_to_carpark_with_distance_6(self, mock_snapper):
        point, snapped = Point(10, 20), Point(12, 22)
        carpark_id, carpark_info = 123, CarparkInfoType.FreePolyline
        edge_id, edge = 5, EdgeData(roads=[15], category=8)

        self.mock_find_nearest(nearest_index={
            point: [CarparkSnapResult(id=carpark_id, type=carpark_info.info.type,
                                      point=snapped, distance=14)],
            snapped: [CarparkSnapResult(id=carpark_id, type=carpark_info.info.type,
                                        point=snapped, distance=14)],
        })

        edge_id_to_edge = {edge_id: edge}
        self._road_graph.edge_data = Mock(side_effect=edge_id_to_edge.get)

        expected = self._make_expected(
            snapped_point=point, original_point=point,
            edge_id=edge_id, edge_info=edge)
        self._snap_and_check(points=[point], expected=[expected])
