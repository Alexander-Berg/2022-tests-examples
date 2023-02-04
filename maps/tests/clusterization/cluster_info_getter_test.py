from mock import patch, Mock
import pytest

import json
from collections import namedtuple

import maps.libs.snap as snap

from maps.carparks.libs.common.py import CarparkType
from maps.carparks.libs.geometry.py import GeometryType

from maps.carparks.tools.carparks_miner.lib.clusterization import \
    cluster_info_getter as tested_module
from maps.carparks.tools.carparks_miner.lib.clusterization import snapping


class TestGetMajority():
    def test_one_object(self):
        objects = [1]
        func = lambda x: x

        assert tested_module._get_majority(objects, func) == 1

    def test_two_same_object(self):
        objects = [1, 1]
        func = lambda x: x

        assert tested_module._get_majority(objects, func) == 1

    def test_two_different_object(self):
        objects = [1, 2]
        func = lambda x: x

        assert tested_module._get_majority(objects, func) is None

    def test_has_majority(self):
        objects = [1] * 10 + [2]
        func = lambda x: x

        assert tested_module._get_majority(objects, func) == 1

    def test_no_majority(self):
        objects = [1] * 10 + [2] * 10 + [3] * 10
        func = lambda x: x

        assert tested_module._get_majority(objects, func) is None

    def test_func(self):
        objects = [1] * 10 + [2] * 10 + [3] * 10
        func = lambda x: 'a'

        assert tested_module._get_majority(objects, func) == 'a'


CarparkInfo = namedtuple('CarparkInfo', ['id', 'type', 'isocode', 'org_id', 'price'])
EdgeId = namedtuple('EdgeId', ['edge_id'])
NearestEdge = namedtuple('NearestEdge', ['points', 'edges', 'return_value'])
ObjectId = namedtuple('ObjectId', ['roads'])
ObjectTitle = namedtuple('ObjectTitle', ['toponym'])
Toponym = namedtuple('Toponym', ['default_translation'])
DefaultTranslation = namedtuple('DefaultTranslation', ['text'])


class TestClusterInfoGetter():
    @pytest.fixture()
    def mock_getter(self):
        self._road_graph = Mock()
        self._rtree_fb = Mock()

        self._getter = tested_module.ClusterInfoGetter(
            self._road_graph, self._rtree_fb)

    def mock_graph_functions(self, road={}, edge_data={}):
        self._road_graph.road = Mock(side_effect=road.get)
        self._road_graph.edge_data = Mock(side_effect=edge_data.get)

    def _convert_to_str(self, data):
        # json.loads returns strings as unicodes,
        # but for us it is easier to work with str
        result = {}
        for key in data:
            value = data[key]
            if isinstance(key, unicode):
                key = str(key)
            if isinstance(value, unicode):
                value = str(value)
            result[key] = value
        return result

    def _check_result(self, generated, expected):
        generated['carpark_info'] = self._convert_to_str(json.loads(str(generated['carpark_info'])))
        assert generated == expected

    @patch.object(snap, 'snap_point_to_all', Mock(return_value=[]))
    def test_area_no_street(self, mock_getter):
        # type 2 = polygon
        carpark_info = CarparkInfo(12, CarparkType.Free, 'ru', 12345, 100)
        points = [snapping.SnappedPoint(
            10, 20,
            carpark_id=carpark_info.id, carpark_type=carpark_info.type,
            carpark_geometry_type=GeometryType.Polygon, carpark_info=carpark_info
        )]

        self._check_result(
            self._getter.get_cluster_props(points),
            {"type": "area",
             "street_name": None,
             "carpark_info": {"id": 12, "type": CarparkType.Free, "isocode": "ru", "org_id": 12345, "price": 100}})

    @patch.object(snap, 'snap_point_to_all', Mock(return_value=[(EdgeId(15),)]))
    @patch.object(snapping, 'choose_nearest_edge', Mock(return_value=(15, 123)))
    def test_area_with_street(self, mock_getter):
        carpark_info = CarparkInfo(12, CarparkType.Free, 'ru', 12345, 100)
        points = [snapping.SnappedPoint(
            10, 20,
            carpark_id=carpark_info.id, carpark_type=carpark_info.type,
            carpark_geometry_type=GeometryType.Polygon, carpark_info=carpark_info,
            edge_id=90, road_id=99, edge_category=8
        )]

        text = DefaultTranslation('road name')
        toponym = Toponym(text)
        object_title = ObjectTitle(toponym)

        self.mock_graph_functions(
            road={99: object_title},
            edge_data={15: ObjectId([123])}
        )

        self._check_result(
            self._getter.get_cluster_props(points),
            {"type": "area",
             "street_name": "road name",
             "carpark_info": {"id": 12, "type": CarparkType.Free, "isocode": "ru", "org_id": 12345, "price": 100}})

    @patch.object(snap, 'snap_point_to_all', Mock(return_value=[(EdgeId(15),)]))
    @patch.object(snapping, 'choose_nearest_edge', Mock(return_value=(15, 123)))
    def test_free_bld_no_road(self, mock_getter):
        carpark_info = CarparkInfo(12, CarparkType.FreeBld, 'ru', 12345, 100)
        points = [snapping.SnappedPoint(
            10, 20,
            carpark_id=carpark_info.id, carpark_type=carpark_info.type,
            carpark_geometry_type=GeometryType.Polyline, carpark_info=carpark_info,
            edge_id=90, edge_category=8
        )]

        self.mock_graph_functions(
            edge_data={15: ObjectId([123])}
        )

        self._check_result(
            self._getter.get_cluster_props(points),
            {"type": "bld",
             "street_name": None,
             "carpark_info": {"id": 12, "type": CarparkType.FreeBld, "isocode": "ru", "org_id": 12345, "price": 100}})

    @patch.object(snap, 'snap_point_to_all', Mock(return_value=[(EdgeId(15),)]))
    @patch.object(snapping, 'choose_nearest_edge', Mock(return_value=(15, 123)))
    def test_restricted_bld_no_road(self, mock_getter):
        carpark_info = CarparkInfo(12, CarparkType.RestrictedBld, 'ru', 12345, 100)
        points = [snapping.SnappedPoint(
            10, 20,
            carpark_id=carpark_info.id, carpark_type=carpark_info.type,
            carpark_geometry_type=GeometryType.Polyline, carpark_info=carpark_info,
            edge_id=90, edge_category=8
        )]

        self.mock_graph_functions(
            edge_data={15: ObjectId([123])}
        )

        self._check_result(
            self._getter.get_cluster_props(points),
            {"type": "bld",
             "street_name": None,
             "carpark_info": {"id": 12, "type": CarparkType.RestrictedBld, "isocode": "ru", "org_id": 12345, "price": 100}})

    @patch.object(snap, 'snap_point_to_all', Mock(return_value=[(EdgeId(15),)]))
    @patch.object(snapping, 'choose_nearest_edge', Mock(return_value=(15, 123)))
    def test_toll_bld_with_road(self, mock_getter):
        carpark_info = CarparkInfo(12, CarparkType.TollBld, 'ru', 12345, 100)
        points = [snapping.SnappedPoint(
            10, 20,
            carpark_id=carpark_info.id, carpark_type=carpark_info.type,
            carpark_geometry_type=GeometryType.Polyline, carpark_info=carpark_info,
            edge_id=90, road_id=99, edge_category=8
        )]

        text = DefaultTranslation('road name')
        toponym = Toponym(text)
        object_title = ObjectTitle(toponym)

        self.mock_graph_functions(
            road={99: object_title},
            edge_data={15: ObjectId([123])}
        )

        self._check_result(
            self._getter.get_cluster_props(points),
            {"type": "bld",
             "street_name": "road name",
             "carpark_info": {"id": 12, "type": CarparkType.TollBld, "isocode": "ru", "org_id": 12345, "price": 100}})

    @patch.object(snap, 'snap_point_to_all', Mock(return_value=[]))
    def test_has_road(self, mock_getter):
        points = [snapping.SnappedPoint(
            10, 20,
            edge_id=90, road_id=99, edge_category=8
        )]

        text = DefaultTranslation('road name')
        toponym = Toponym(text)
        object_title = ObjectTitle(toponym)

        self.mock_graph_functions(
            road={99: object_title}
        )

        self._check_result(
            self._getter.get_cluster_props(points),
            {"type": "road",
             "street_name": "road name",
             "carpark_info": {"id": None, "type": None, "isocode": None, "org_id": None, "price": None}})

    @patch.object(snap, 'snap_point_to_all', Mock(return_value=[]))
    def test_importand_unnamed_road(self, mock_getter):
        points = [snapping.SnappedPoint(
            10, 20,
            edge_id=90, edge_category=5
        )]

        self._check_result(
            self._getter.get_cluster_props(points),
            {"type": "road",
             "street_name": None,
             "carpark_info": {"id": None, "type": None, "isocode": None, "org_id": None, "price": None}})

    @patch.object(snap, 'snap_point_to_all', Mock(return_value=[(EdgeId(15),)]))
    @patch.object(snapping, 'choose_nearest_edge', Mock(return_value=(15, 123)))
    def test_frontage_with_road_nearby(self, mock_getter):
        points = [snapping.SnappedPoint(10, 20, edge_id=90, edge_category=8)]

        text = DefaultTranslation('road name')
        toponym = Toponym(text)
        object_title = ObjectTitle(toponym)

        self.mock_graph_functions(
            road={123: object_title},
            edge_data={15: ObjectId([123])}
        )

        self._check_result(
            self._getter.get_cluster_props(points),
            {"type": "frontage",
             "street_name": "road name",
             "carpark_info": {"id": None, "type": None, "isocode": None, "org_id": None, "price": None}})

    @patch.object(snap, 'snap_point_to_all', Mock(return_value=[]))
    def test_frontage_no_road_nearby(self, mock_getter):
        points = [snapping.SnappedPoint(
            10, 20,
            edge_id=90, edge_category=7
        )]

        self._check_result(
            self._getter.get_cluster_props(points),
            {"type": "frontage",
             "street_name": None,
             "carpark_info": {"id": None, "type": None, "isocode": None, "org_id": None, "price": None}})

    @patch.object(snap, 'snap_point_to_all', Mock(return_value=[(EdgeId(15),)]))
    @patch.object(snapping, 'choose_nearest_edge', Mock(return_value=None))
    def test_frontage_with_road_nearby_too_far(self, mock_getter):
        points = [snapping.SnappedPoint(
            10, 20,
            edge_id=90, edge_category=7
        )]

        self.mock_graph_functions(
            edge_data={15: ObjectId([123])}
        )

        # this happens if the edge is too far from points
        self._check_result(
            self._getter.get_cluster_props(points),
            {"type": "frontage",
             "street_name": None,
             "carpark_info": {"id": None, "type": None, "isocode": None, "org_id": None, "price": None}})

    @patch.object(snap, 'snap_point_to_all', Mock(return_value=[(EdgeId(15),)]))
    def test_frontage_unnamed_road_nearby(self, mock_getter):
        points = [snapping.SnappedPoint(
            10, 20,
            edge_id=90, edge_category=7
        )]

        self.mock_graph_functions(
            edge_data={15: ObjectId(None)}
        )

        self._check_result(
            self._getter.get_cluster_props(points),
            {"type": "frontage",
             "street_name": None,
             "carpark_info": {"id": None, "type": None, "isocode": None, "org_id": None, "price": None}})

    @patch.object(snap, 'snap_point_to_all', Mock(return_value=[(EdgeId(15),)]))
    def test_yard(self, mock_getter):
        points = [snapping.SnappedPoint(
            10, 20,
            edge_id=90, edge_category=8
        )]

        self.mock_graph_functions(
            edge_data={15: ObjectId(None)}
        )

        self._check_result(
            self._getter.get_cluster_props(points),
            {"type": "yard",
             "street_name": None,
             "carpark_info": {"id": None, "type": None, "isocode": None, "org_id": None, "price": None}})

    @patch.object(snap, 'snap_point_to_all', Mock(return_value=[(EdgeId(15),)]))
    def test_mixed(self, mock_getter):
        carpark_info1 = CarparkInfo(12, CarparkType.TollBld, 'ru', 12345, 100)
        carpark_info2 = CarparkInfo(13, CarparkType.FreeBld, 'ru', 12345, 100)
        carpark_info3 = CarparkInfo(14, CarparkType.Free, 'ru', 12345, 100)
        carpark_info4 = CarparkInfo(14, CarparkType.Toll, 'ru', 12345, 100)
        points = [
            snapping.SnappedPoint(
                10, 20,
                edge_id=90, edge_category=8),
            snapping.SnappedPoint(
                10, 20,
                edge_id=90, edge_category=7),
            snapping.SnappedPoint(
                10, 20,
                edge_id=90, edge_category=5),
            snapping.SnappedPoint(
                10, 20,
                edge_id=90, road_id=99, edge_category=8),
            snapping.SnappedPoint(
                10, 20,
                carpark_id=carpark_info1.id, carpark_type=carpark_info1.type,
                carpark_geometry_type=GeometryType.Polyline, carpark_info=carpark_info1,
                edge_id=90, road_id=98, edge_category=8),
            snapping.SnappedPoint(
                10, 20,
                carpark_id=carpark_info2.id, carpark_type=carpark_info2.type,
                carpark_geometry_type=GeometryType.Polyline, carpark_info=carpark_info2,
                edge_id=90, edge_category=8),
            snapping.SnappedPoint(
                10, 20,
                carpark_id=carpark_info3.id, carpark_type=carpark_info3.type,
                carpark_geometry_type=GeometryType.Polygon, carpark_info=carpark_info3,
                edge_id=90, road_id=97, edge_category=4),
            snapping.SnappedPoint(
                10, 20,
                carpark_id=carpark_info4.id, carpark_type=carpark_info4.type,
                carpark_geometry_type=GeometryType.Polygon, carpark_info=carpark_info4)
        ]

        self.mock_graph_functions(
            edge_data={15: ObjectId(None)}
        )

        self._check_result(
            self._getter.get_cluster_props(points),
            {'type': 'mixed',
                'street_name': None,
                'carpark_info': {'id': None, 'type': None, 'isocode': None, 'org_id': None, 'price': None}})

    @patch.object(snap, 'snap_point_to_all', Mock(side_effect=[[(EdgeId(15),)], [(EdgeId(16),), (EdgeId(17),)]]))
    @patch.object(snapping, 'choose_nearest_edge', Mock(return_value=(16, 124)))
    def test_several_road_nearby(self, mock_getter):
        points = [
            snapping.SnappedPoint(
                10, 20,
                edge_id=90, edge_category=8),
            snapping.SnappedPoint(
                11, 22,
                edge_id=90, edge_category=8)
        ]

        text = DefaultTranslation('road name')
        toponym = Toponym(text)
        object_title = ObjectTitle(toponym)

        self.mock_graph_functions(
            road={124: object_title},
            edge_data={15: ObjectId([123]), 16: ObjectId([124]), 17: ObjectId([125])}
        )

        self._check_result(
            self._getter.get_cluster_props(points),
            {"type": "frontage",
             "street_name": "road name",
             "carpark_info": {"id": None, "type": None, "isocode": None, "org_id": None, "price": None}})
