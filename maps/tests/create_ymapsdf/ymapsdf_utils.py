import json
import logging
from typing import List

from library.python import resource
import yt.wrapper as yt

from maps.garden.libs.road_graph_builder import common
from yandex.maps import geolib3

from . import ymapsdf_types as ym

logger = logging.getLogger("ymapsdf_utils")


class Graph:
    def __init__(self):
        self._junctions: List[ym.Junction] = []
        self._road_elements: List[ym.RoadElement] = []
        self._roads: List[ym.Road] = []
        self._annotation_phrases: List[ym.AnnotationPhrase] = []
        self._conditions: List[ym.RoadCondition] = []
        self._map_features: List[ym.MapFeature] = []
        self._vehicle_restrictions: List[ym.VehicleRestriction] = []

        self._name_id = 1
        self._nodes = []
        self._edges = []
        self._faces = []

        self._table_data = {}

    def _create(self, table):
        assert table not in self._table_data, \
            "table is already created: {}".format(table)
        self._table_data[table] = []

    def _write(self, table, *args):
        assert table in self._table_data, \
            "table is not yet created: {}".format(table)
        for row in args:
            self._table_data[table].append(row)

    def _write_annotation_phrases(self):
        self._create(common.YmapsdfTables.COND_ANNOTATION_PHRASE_ELEMENT)
        self._create(common.YmapsdfTables.COND_ANNOTATION_PHRASE_ELEMENT_NM)

        annotation_phrase_element_id = 1
        for annotation_phrase in self._annotation_phrases:
            for annotation_phrase_element in annotation_phrase.elements:
                self._write(
                    common.YmapsdfTables.COND_ANNOTATION_PHRASE_ELEMENT,
                    {
                        "annotation_phrase_element_id": annotation_phrase_element_id,
                        "annotation_phrase_id": annotation_phrase.annotation_phrase_id,
                        "proximity": annotation_phrase_element.proximity.value,
                    })
                for translation in annotation_phrase_element.translations:
                    self._write(
                        common.YmapsdfTables.COND_ANNOTATION_PHRASE_ELEMENT_NM,
                        {
                            "nm_id": self._name_id,
                            "annotation_phrase_element_id": annotation_phrase_element_id,
                            "lang": translation.lang,
                            "extlang": translation.extlang,
                            "script": translation.script,
                            "region": translation.region,
                            "variant": translation.variant,
                            "is_local": translation.is_local,
                            "is_auto": translation.is_auto,
                            "name": translation.name,
                            "name_type": translation.name_type,
                        })
                    self._name_id += 1
                annotation_phrase_element_id += 1

    def _write_conditions(self):
        # Information related to direction signs is not filled, 'cond_ds',
        # 'cond_ds_nm' tables are created empty.
        self._create(common.YmapsdfTables.COND_DS_NM)
        self._create(common.YmapsdfTables.COND_DS)

        self._create(common.YmapsdfTables.COND)
        self._create(common.YmapsdfTables.COND_ANNOTATION)
        self._create(common.YmapsdfTables.COND_DT)
        self._create(common.YmapsdfTables.COND_RD_SEQ)
        self._create(common.YmapsdfTables.COND_LANE)

        cond_dt_id = 1
        for condition in self._conditions:
            self._write(
                common.YmapsdfTables.COND,
                {
                    "cond_id": condition.cond_id,
                    "cond_type": condition.condition_type.value,
                    "cond_seq_id": condition.cond_id,
                    "access_id": condition.access_id.value,
                })

            if condition.annotation_id is not None:
                self._write(
                    common.YmapsdfTables.COND_ANNOTATION,
                    {
                        "cond_id": condition.cond_id,
                        "annotation_id": condition.annotation_id.value,
                        "annotation_phrase_id": condition.annotation_phrase_id,
                    })

            for schedule in condition.schedules:
                self._write(
                    common.YmapsdfTables.COND_DT,
                    {
                        "cond_dt_id": cond_dt_id,
                        "cond_id": condition.cond_id,
                        "date_start": schedule.date_start,
                        "date_end": schedule.date_end,
                        "time_start": schedule.time_start,
                        "time_end": schedule.time_end,
                        "day": schedule.day,
                    })
                cond_dt_id += 1

            for seq_num, road_element in enumerate(condition.road_elements):
                self._write(
                    common.YmapsdfTables.COND_RD_SEQ,
                    {
                        "cond_seq_id": condition.cond_id,
                        "seq_num": seq_num,
                        "rd_el_id": road_element.rd_el_id,
                        "rd_jc_id": (
                            condition.first_junction.jc_id
                            if seq_num == 0 else None),
                    })

            for lane in condition.lanes:
                self._write(
                    common.YmapsdfTables.COND_LANE,
                    {
                        "cond_id": condition.cond_id,
                        "lane_min_num": lane.lane_min_num,
                        "lane_max_num": lane.lane_max_num,
                        "lane_direction_id": lane.lane_direction_id.value,
                    })

    def _write_map_features(self):
        self._create(common.YmapsdfTables.FT_TYPE)
        self._create(common.YmapsdfTables.FT)
        self._create(common.YmapsdfTables.FT_GEOM)
        self._create(common.YmapsdfTables.FT_CENTER)
        self._create(common.YmapsdfTables.FT_EDGE)
        self._create(common.YmapsdfTables.FT_NM)
        self._create(common.YmapsdfTables.FT_FACE)

        for ft_type_line in resource.find("/ft_type").splitlines():
            self._write(common.YmapsdfTables.FT_TYPE, json.loads(ft_type_line))

        for map_feature in self._map_features:
            self._write(
                common.YmapsdfTables.FT,
                {
                    "ft_id": map_feature.ft_id,
                    "p_ft_id": map_feature.parent_id,
                    "ft_type_id": map_feature.type_id.value,
                    "rubric_id": map_feature.rubric_id,
                    "icon_class": map_feature.icon_class,
                    "disp_class": map_feature.disp_class,
                    "disp_class_tweak": map_feature.disp_class_tweak,
                    "disp_class_navi": map_feature.disp_class_navi,
                    "disp_class_tweak_navi": map_feature.disp_class_tweak_navi,
                    "search_class": map_feature.search_class,
                    "isocode": map_feature.isocode,
                    "subcode": map_feature.subcode,
                })

            self._write(
                common.YmapsdfTables.FT_GEOM,
                {
                    "ft_id": map_feature.ft_id,
                    "shape": map_feature.geometry.shape_hex,
                    "xmin": map_feature.geometry.xmin,
                    "xmax": map_feature.geometry.xmax,
                    "ymin": map_feature.geometry.ymin,
                    "ymax": map_feature.geometry.ymax,
                })

            if isinstance(map_feature.geometry, ym.Node):
                self._write(
                    common.YmapsdfTables.FT_CENTER,
                    {
                        "ft_id": map_feature.ft_id,
                        "node_id": map_feature.geometry.node_id,
                    })
            elif isinstance(map_feature.geometry, ym.Edge):
                self._write(
                    common.YmapsdfTables.FT_EDGE,
                    {
                        "ft_id": map_feature.ft_id,
                        "edge_id": map_feature.geometry.edge_id,
                    })
            elif isinstance(map_feature.geometry, ym.Face):
                self._write(
                    common.YmapsdfTables.FT_FACE,
                    {
                        "ft_id": map_feature.ft_id,
                        "face_id": map_feature.geometry.face_id,
                        "is_interior": False,
                    })

            for name in map_feature.names:
                self._write(
                    common.YmapsdfTables.FT_NM,
                    {
                        "nm_id": self._name_id,
                        "ft_id": map_feature.ft_id,
                        "lang": name.lang,
                        "extlang": name.extlang,
                        "script": name.script,
                        "region": name.region,
                        "variant": name.variant,
                        "is_local": name.is_local,
                        "is_auto": name.is_auto,
                        "name": name.name,
                        "name_type": name.name_type,
                    })
                self._name_id += 1

    def _write_road_topology(self):
        self._create(common.YmapsdfTables.RD_JC)
        self._create(common.YmapsdfTables.RD_EL)
        self._create(common.YmapsdfTables.RD_EL_LANE)

        for junction in self._junctions:
            self._write(
                common.YmapsdfTables.RD_JC,
                {
                    "rd_jc_id": junction.jc_id,
                    "shape": junction.shape_hex,
                    "x": junction.x,
                    "y": junction.y,
                })

        for road_element in self._road_elements:
            self._write(
                common.YmapsdfTables.RD_EL,
                {
                    "rd_el_id": road_element.rd_el_id,
                    "f_rd_jc_id": road_element.f_jc.jc_id,
                    "t_rd_jc_id": road_element.t_jc.jc_id,
                    "fc": road_element.fc.value,
                    "fow": road_element.fow.value,
                    "speed_cat": road_element.speed_cat,
                    "speed_limit": road_element.speed_limit,
                    "f_zlev": road_element.f_zlev,
                    "t_zlev": road_element.t_zlev,
                    "oneway": road_element.oneway.value,
                    "access_id": road_element.access_id.value,
                    "back_bus": road_element.back_bus,
                    "forward_bus": road_element.forward_bus,
                    "back_taxi": road_element.back_taxi,
                    "forward_taxi": road_element.forward_taxi,
                    "residential": road_element.residential,
                    "restricted_for_trucks": road_element.restricted_for_trucks,
                    "paved": road_element.paved,
                    "poor_condition": road_element.poor_condition,
                    "stairs": road_element.stairs,
                    "sidewalk": road_element.sidewalk.value,
                    "struct_type": road_element.struct_type.value,
                    "ferry": road_element.ferry,
                    "dr": road_element.dr,
                    "toll": road_element.toll,
                    "srv_ra": road_element.srv_ra,
                    "srv_uc": road_element.srv_uc,
                    "isocode": road_element.isocode,
                    "subcode": road_element.subcode,
                    "shape": road_element.shape_hex,
                    "xmin": road_element.xmin,
                    "xmax": road_element.xmax,
                    "ymin": road_element.ymin,
                    "ymax": road_element.ymax,
                    "speed_limit_f": road_element.speed_limit_f,
                    "speed_limit_t": road_element.speed_limit_t,
                    "speed_limit_truck_f": road_element.speed_limit_truck_f,
                    "speed_limit_truck_t": road_element.speed_limit_truck_t,
                    "back_bicycle": road_element.back_bicycle,
                })

            for lane_num, lane in enumerate(road_element.lanes):
                self._write(
                    common.YmapsdfTables.RD_EL_LANE,
                    {
                        "rd_el_id": road_element.rd_el_id,
                        "rd_el_direction": lane.direction.value,
                        "lane_num": lane_num,
                        "lane_kind": lane.lane_kind.value,
                        "lane_direction_id": lane.lane_direction_id.value,
                    })

    def _write_vehicle_restrictions(self):
        self._create(common.YmapsdfTables.COND_VEHICLE_RESTRICTION)
        self._create(common.YmapsdfTables.RD_EL_VEHICLE_RESTRICTION)
        self._create(common.YmapsdfTables.VEHICLE_RESTRICTION)
        self._create(common.YmapsdfTables.VEHICLE_RESTRICTION_DT)

        vehicle_restriction_dt_id = 1
        for vehicle_restriction in self._vehicle_restrictions:
            self._write(
                common.YmapsdfTables.VEHICLE_RESTRICTION,
                {
                    "vehicle_restriction_id":
                        vehicle_restriction.vehicle_restriction_id,
                    "access_id": vehicle_restriction.access_id.value,
                    "universal_id": vehicle_restriction.universal_id,
                    "pass_id": vehicle_restriction.pass_id,
                    "weight_limit": vehicle_restriction.weight_limit,
                    "axle_weight_limit": vehicle_restriction.axle_weight_limit,
                    "max_weight_limit": vehicle_restriction.max_weight_limit,
                    "height_limit": vehicle_restriction.height_limit,
                    "width_limit": vehicle_restriction.width_limit,
                    "length_limit": vehicle_restriction.length_limit,
                    "payload_limit": vehicle_restriction.payload_limit,
                    "min_eco_class": vehicle_restriction.min_eco_class,
                    "trailer_not_allowed": vehicle_restriction.trailer_not_allowed,
                })

            for schedule in vehicle_restriction.schedules:
                self._write(
                    common.YmapsdfTables.VEHICLE_RESTRICTION_DT,
                    {
                        "vehicle_restriction_dt_id": vehicle_restriction_dt_id,
                        "vehicle_restriction_id":
                            vehicle_restriction.vehicle_restriction_id,
                        "date_start": schedule.date_start,
                        "date_end": schedule.date_end,
                        "time_start": schedule.time_start,
                        "time_end": schedule.time_end,
                        "day": schedule.day,
                    })
                vehicle_restriction_dt_id += 1

            for road_element in vehicle_restriction.road_elements:
                self._write(
                    common.YmapsdfTables.RD_EL_VEHICLE_RESTRICTION,
                    {
                        "rd_el_id": road_element.rd_el_id,
                        "vehicle_restriction_id":
                            vehicle_restriction.vehicle_restriction_id,
                    })

            for condition in vehicle_restriction.conditions:
                self._write(
                    common.YmapsdfTables.COND_VEHICLE_RESTRICTION,
                    {
                        "cond_id": condition.cond_id,
                        "vehicle_restriction_id":
                            vehicle_restriction.vehicle_restriction_id,
                    })

    def _write_roads(self):
        self._create(common.YmapsdfTables.RD)
        self._create(common.YmapsdfTables.RD_RD_EL)
        self._create(common.YmapsdfTables.RD_NM)
        self._create(common.YmapsdfTables.RD_GEOM)

        for road in self._roads:
            self._write(
                common.YmapsdfTables.RD,
                {
                    "rd_id": road.rd_id,
                    "rd_type": road.rd_type.value,
                    "search_class": road.search_class,
                    "isocode": road.isocode,
                    "subcode": road.subcode,
                })

            for road_element in road.road_elements:
                self._write(
                    common.YmapsdfTables.RD_RD_EL,
                    {
                        "rd_el_id": road_element.rd_el_id,
                        "rd_id": road.rd_id,
                    })

            for road_name in road.names:
                self._write(
                    common.YmapsdfTables.RD_NM,
                    {
                        "nm_id": self._name_id,
                        "rd_id": road.rd_id,
                        "lang": road_name.lang,
                        "extlang": road_name.extlang,
                        "script": road_name.script,
                        "region": road_name.region,
                        "variant": road_name.variant,
                        "is_local": road_name.is_local,
                        "is_auto": road_name.is_auto,
                        "name": road_name.name,
                        "name_type": road_name.name_type,
                    })
                self._name_id += 1

            road_polyline = geolib3.Polyline2()
            for road_element in road.road_elements:
                road_polyline.extend(
                    road_element.shape,
                    geolib3.EndPointMergePolicy.MergeEqualPoints)
            road_shape = ym.Geometry(road_polyline)
            self._write(
                common.YmapsdfTables.RD_GEOM,
                {
                    "rd_id": road.rd_id,
                    "shape": road_shape.shape_hex,
                    "xmin": road_shape.xmin,
                    "xmax": road_shape.xmax,
                    "ymin": road_shape.ymin,
                    "ymax": road_shape.ymax,
                })

    def _write_primitives(self):
        self._create(common.YmapsdfTables.NODE)
        self._create(common.YmapsdfTables.EDGE)
        self._create(common.YmapsdfTables.FACE_EDGE)

        for node in self._nodes:
            self._write(
                common.YmapsdfTables.NODE,
                {
                    "node_id": node.node_id,
                    "shape": node.shape_hex,
                    "x": node.x,
                    "y": node.y,
                })

        for edge in self._edges:
            self._write(
                common.YmapsdfTables.EDGE,
                {
                    "edge_id": edge.edge_id,
                    "f_node_id": edge.f_node_id,
                    "t_node_id": edge.t_node_id,
                    "f_zlev": edge.f_zlev,
                    "t_zlev": edge.t_zlev,
                    "shape": edge.shape_hex,
                    "xmin": edge.xmin,
                    "xmax": edge.xmax,
                    "ymin": edge.ymin,
                    "ymax": edge.ymax,
                })

        for face in self._faces:
            for edge in face.edges:
                self._write(
                    common.YmapsdfTables.FACE_EDGE,
                    {
                        "face_id": face.face_id,
                        "edge_id": edge.edge_id,
                    })

    def write(self, yt_client, output_yt_directory):
        self._write_road_topology()
        self._write_vehicle_restrictions()
        self._write_roads()
        self._write_annotation_phrases()
        self._write_conditions()
        self._write_map_features()
        self._write_primitives()

        logger.info("writing YMapsDF tables to: {}".format(output_yt_directory))
        if not yt_client.exists(output_yt_directory):
            yt_client.create("map_node", output_yt_directory, recursive=True)

        for table_id in common.YmapsdfTables:
            table_rows = self._table_data[table_id]
            logger.info("{}".format(table_id.value))
            for row in table_rows:
                logger.info("    {}".format(row))

            table_path = yt.ypath.ypath_join(output_yt_directory, table_id.value)
            sorted_by = yt_client.get_attribute(table_path, "sorted_by", [])
            sort_key = lambda row: [row[col] for col in sorted_by]
            yt_client.write_table(
                table_path,
                sorted(table_rows, key=sort_key))

    def add_junction(self, *args, **kwargs):
        self._junctions.append(
            ym.Junction(len(self._junctions) + 1, *args, **kwargs))
        return self._junctions[-1]

    def add_road_element(self, *args, **kwargs):
        self._road_elements.append(
            ym.RoadElement(len(self._road_elements) + 1, *args, **kwargs))
        return self._road_elements[-1]

    def add_road(self, *args, **kwargs):
        self._roads.append(ym.Road(len(self._roads) + 1, *args, **kwargs))
        return self._roads[-1]

    def add_annotation_phrase(self, *args, **kwargs):
        self._annotation_phrases.append(
            ym.AnnotationPhrase(len(self._annotation_phrases) + 1, *args, **kwargs))
        return self._annotation_phrases[-1]

    def add_road_condition(self, *args, **kwargs):
        self._conditions.append(
            ym.RoadCondition(len(self._conditions) + 1, *args, **kwargs))
        return self._conditions[-1]

    def add_map_feature(self, *args, **kwargs):
        self._map_features.append(
            ym.MapFeature(len(self._map_features) + 1, *args, **kwargs))
        return self._map_features[-1]

    def add_vehicle_restriction(self, *args, **kwargs):
        self._vehicle_restrictions.append(
            ym.VehicleRestriction(
                len(self._vehicle_restrictions) + 1, *args, **kwargs))
        return self._vehicle_restrictions[-1]

    def add_node(self, x, y):
        node = ym.Node(node_id=len(self._nodes) + 1, x=x, y=y)
        self._nodes.append(node)
        return node

    def add_edge(self, points, f_zlev=0, t_zlev=0):
        assert len(points) > 1

        nodes = [self.add_node(p[0], p[1]) for p in points]
        edge = ym.Edge(
            edge_id=len(self._edges) + 1,
            nodes=nodes,
            f_zlev=f_zlev,
            t_zlev=t_zlev)
        self._edges.append(edge)
        return edge

    def add_face(self, points):
        assert len(points) > 2 and points[0] == points[-1]

        nodes = [self.add_node(p[0], p[1]) for p in points[:-1]]
        edges = []
        for i, cur_node in enumerate(nodes):
            next_node = nodes[(i + 1) % len(nodes)]
            edge = ym.Edge(
                edge_id=len(self._edges) + 1,
                nodes=[cur_node, next_node])
            edges.append(edge)
            self._edges.append(edge)
        face = ym.Face(len(self._faces) + 1, edges)
        self._faces.append(face)
        return face
