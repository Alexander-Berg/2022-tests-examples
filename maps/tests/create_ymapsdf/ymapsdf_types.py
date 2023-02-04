import enum
from dataclasses import dataclass, field
from typing import Optional, List

import shapely.geometry
import shapely.wkb

from maps.libs.ymapsdf.py import rd
from maps.libs.ymapsdf.py.ft_type import FtType
from yandex.maps import geolib3


def _common_junction(first_road_element, second_road_element):
    first_junctions = [first_road_element.f_jc, first_road_element.t_jc]
    second_junctions = [second_road_element.f_jc, second_road_element.t_jc]
    common_junctions = [jc for jc in first_junctions if jc in second_junctions]
    assert len(common_junctions) == 1
    return common_junctions[0]


AccessIdMask = enum.Flag("AccessIdMask", [(x.name, x.value) for x in rd.AccessId])


class Geometry:
    def __init__(self, shape):
        self._shape = shape

    @property
    def shape(self):
        return self._shape

    @property
    def x(self):
        assert isinstance(self.shape, geolib3.Point2)
        return self.shape.x

    @property
    def y(self):
        assert isinstance(self.shape, geolib3.Point2)
        return self.shape.y

    @property
    def shape_hex(self):
        wkb = self.shape.to_EWKB(geolib3.SpatialReference.Epsg4326)
        return wkb.hex().upper().encode("ascii")

    @property
    def xmin(self):
        return self.shape.bounding_box().min_x

    @property
    def ymin(self):
        return self.shape.bounding_box().min_y

    @property
    def xmax(self):
        return self.shape.bounding_box().max_x

    @property
    def ymax(self):
        return self.shape.bounding_box().max_y


# https://doc.yandex-team.ru/ymaps/ymapsdf/ymapsdf-ref/concepts/road_lane.html#cond_ann
class AnnotationId(enum.Enum):
    UTurnLeft = 1
    SharpLeftTurn = 2
    LeftTurn = 3
    GradualLeftTurn = 4
    KeepLeft = 5
    Straight = 6
    KeepRight = 7
    GradualRightTurn = 8
    RightTurn = 9
    SharpRightTurn = 10
    UTurnRight = 11


# https://doc.yandex-team.ru/ymaps/ymapsdf/ymapsdf-ref/concepts/annotation.html#cond_annotation_phrase_element
class Proximity(enum.Enum):
    Immediate = 1
    Close = 2
    Faraway = 3


# https://doc.yandex-team.ru/ymaps/ymapsdf/ymapsdf-ref/concepts/road_el.html?lang=en#road_el__oneway
class Oneway(enum.Enum):
    Forward = "F"
    Backward = "T"
    Both = "B"


# https://doc.yandex-team.ru/ymaps/ymapsdf/ymapsdf-ref/concepts/road_cond.html
class RoadConditionType(enum.Enum):
    Forbidden = 1
    Prescribed = 2
    Allowed = 3
    Undesirable = 4
    AccessPass = 5
    Priority = 6
    TrafficLight = 7
    RailroadCrossing = 8
    Bifurcation = 9
    Warnings = 10
    BorderControl = 11
    TollwayEntrance = 12
    TollwayExit = 13
    TollBooth = 14
    SpeedMonitoring = 15
    BusLaneMonitoring = 16
    PatrolCheckpoint = 17
    LaneAnnotation = 18
    ManoeuvreAnnotation = 19
    DirectionSignAnnotation = 20
    RoadMarkingMonitoring = 21
    IntersectionPassingControl = 22
    StopControl = 23
    PreferableManoeuvre = 24
    FreeCountryBorder = 25
    SpeedBump = 26


# https://doc.yandex-team.ru/ymaps/ymapsdf/ymapsdf-ref/concepts/road.html#road__road1
class RoadType(enum.Enum):
    RoadWithinLocality = 1
    NumberedRoute = 2
    RoadStructure = 3
    UrbanThroughfare = 4
    ThruRoad = 5
    KilometerRoad = 6
    NamedExit = 7


# https://doc.yandex-team.ru/ymaps/ymapsdf/ymapsdf-ref/concepts/road_el.html#road_el__sidewalks
class Sidewalk(enum.Enum):
    No = "N"
    BothSides = "B"
    LeftSide = "L"
    RightSide = "R"


class LaneKind(enum.Enum):
    NonRoutedTraffic = 0
    NonRailTransit = 1
    Tramway = 2
    Bicycle = 3


class ConditionLaneDirection(enum.Flag):
    Left180 = 1
    Left135 = 2
    Left90 = 4
    Left45 = 8
    StraightAhead = 16
    Right45 = 32
    Right90 = 64
    Right135 = 128
    Right180 = 256
    LeftFromRight = 512
    RightFromLeft = 1024
    LeftShift = 2048
    RightShift = 4096


class LaneDirection(enum.Enum):
    Forward = "T"
    Backward = "F"


class Junction(Geometry):
    def __init__(self, jc_id, x, y, is_border=False):
        super().__init__(geolib3.Point2(x, y))
        self.jc_id = jc_id
        self.is_border = is_border


@dataclass
class RoadElementLane:
    lane_kind: LaneKind = LaneKind.NonRoutedTraffic
    direction: LaneDirection = LaneDirection.Forward
    lane_direction_id: ConditionLaneDirection = ConditionLaneDirection.StraightAhead


@dataclass
class RoadElement(Geometry):
    rd_el_id: int
    f_jc: Junction
    t_jc: Junction
    polyline: Optional[geolib3.Polyline2] = None
    fc: rd.FunctionalClass = rd.FunctionalClass.MAJOR_LOCAL_ROAD
    fow: rd.FormOfWay = rd.FormOfWay.TWO_WAY_ROAD
    speed_cat: int = 6
    speed_limit: Optional[int] = None
    f_zlev: int = 0
    t_zlev: int = 0
    oneway: Oneway = Oneway.Forward
    access_id: AccessIdMask = AccessIdMask.CAR
    back_taxi: int = 0
    forward_taxi: int = 0
    residential: int = 0
    restricted_for_trucks: int = 0
    paved: int = 1
    poor_condition: int = 0
    struct_type: rd.StructType = rd.StructType.GROUND
    ferry: int = 0
    toll: int = 0
    srv_ra: int = 0
    stairs: int = 0
    isocode: str = "RU"
    subcode: Optional[str] = None
    sidewalk: Sidewalk = Sidewalk.No
    srv_uc: int = 0
    dr: int = 0
    lanes: List[RoadElementLane] = field(default_factory=list)
    back_bus: int = 0
    forward_bus: int = 0
    speed_limit_f: Optional[int] = None
    speed_limit_t: Optional[int] = None
    speed_limit_truck_f: Optional[int] = None
    speed_limit_truck_t: Optional[int] = None
    back_bicycle: int = 0

    def __post_init__(self):
        if self.polyline is None:
            self.polyline = geolib3.Polyline2()
            self.polyline.add(self.f_jc.shape)
            self.polyline.add(self.t_jc.shape)
        super().__init__(self.polyline)


class Road:
    def __init__(
            self,
            rd_id,
            road_elements,
            rd_type=RoadType.RoadWithinLocality,
            search_class=1,
            isocode="RU",
            subcode=None,
            names=list()):
        self.rd_id = rd_id
        self.rd_type = rd_type
        self.search_class = search_class
        self.isocode = isocode
        self.subcode = subcode
        self.road_elements = road_elements
        self.names = names


class RoadCondition:
    def __init__(
            self,
            cond_id,
            condition_type,
            road_elements,
            access_id=AccessIdMask.CAR,
            first_junction=None,
            annotation_id=None,
            annotation_phrase_id=None,
            schedules=[],
            lanes=list()):
        assert (
            first_junction is not None or
            len(road_elements) >= 2 and road_elements[0] != road_elements[1]
        )

        if first_junction is None:
            first_junction = _common_junction(road_elements[0], road_elements[1])

        self.cond_id = cond_id
        self.condition_type = condition_type
        self.road_elements = road_elements
        self.access_id = access_id
        self.first_junction = first_junction
        self.annotation_id = annotation_id
        self.annotation_phrase_id = annotation_phrase_id
        self.schedules = schedules
        self.lanes = lanes


class Schedule:
    def __init__(
            self,
            start_month=None,
            start_day=None,
            end_month=None,
            end_day=None,
            start_hour=None,
            start_minute=0,
            end_hour=None,
            end_minute=0,
            day=127):
        self.start_month = start_month
        self.start_day = start_day
        self.end_month = end_month
        self.end_day = end_day
        self.start_hour = start_hour
        self.start_minute = start_minute
        self.end_hour = end_hour
        self.end_minute = end_minute
        self.day = day

    @property
    def date_start(self):
        if self.start_month is None or self.start_day is None:
            return ""
        else:
            return "{:02d}{:02d}".format(self.start_month, self.start_day)

    @property
    def date_end(self):
        if self.end_month is None or self.end_day is None:
            return ""
        else:
            return "{:02d}{:02d}".format(self.end_month, self.end_day)

    @property
    def time_start(self):
        if self.start_hour is None or self.start_minute is None:
            return ""
        else:
            return "{:02d}{:02d}".format(self.start_hour, self.start_minute)

    @property
    def time_end(self):
        if self.end_hour is None or self.end_minute is None:
            return ""
        else:
            return "{:02d}{:02d}".format(self.end_hour, self.end_minute)


class RoadConditionLane:
    def __init__(self, lane_min_num, lane_max_num, lane_direction_id):
        self.lane_min_num = lane_min_num
        self.lane_max_num = lane_max_num
        self.lane_direction_id = lane_direction_id


class MapFeature:
    def __init__(
            self,
            ft_id,
            geometry,
            parent_id=None,
            type_id=FtType.URBAN_ROADNET_CLOSURE,
            rubric_id=None,
            icon_class=None,
            disp_class=1,
            disp_class_tweak=0.0,
            disp_class_navi=1,
            disp_class_tweak_navi=0.0,
            search_class=None,
            isocode="RU",
            subcode=None,
            names=list()):
        self.ft_id = ft_id
        self.geometry = geometry
        self.parent_id = parent_id
        self.type_id = type_id
        self.rubric_id = rubric_id
        self.icon_class = icon_class
        self.disp_class = disp_class
        self.disp_class_tweak = disp_class_tweak
        self.disp_class_navi = disp_class_navi
        self.disp_class_tweak_navi = disp_class_tweak_navi
        self.search_class = search_class
        self.isocode = isocode
        self.subcode = subcode
        self.names = names


# Used for both road names (rd_nm table) and map feature names (ft_nm table).
class Name:
    def __init__(
            self,
            name,
            lang="en",
            extlang=None,
            script=None,
            region=None,
            variant=None,
            is_local=True,
            is_auto=False,
            name_type=0):
        self.name = name
        self.lang = lang
        self.extlang = extlang
        self.script = script
        self.region = region
        self.variant = variant
        self.is_local = is_local
        self.is_auto = is_auto
        self.name_type = name_type


class Node(Geometry):
    def __init__(self, node_id, x, y):
        super().__init__(geolib3.Point2(x, y))
        self.node_id = node_id


class Edge(Geometry):
    def __init__(self, edge_id, nodes, f_zlev=0, t_zlev=0):
        polyline = geolib3.Polyline2()
        for node in nodes:
            polyline.add(geolib3.Point2(node.x, node.y))
        super().__init__(polyline)

        self.edge_id = edge_id
        self.f_node_id = nodes[0].node_id
        self.t_node_id = nodes[-1].node_id
        self.f_zlev = f_zlev
        self.t_zlev = t_zlev


class Face(Geometry):
    def __init__(self, face_id, edges):
        polyline = geolib3.Polyline2()
        for edge in edges:
            polyline.extend(
                edge.shape, geolib3.EndPointMergePolicy.MergeEqualPoints)

        points = []
        for point in polyline:
            points.append((point.x, point.y))

        wkb = shapely.geometry.Polygon(points).wkb
        polygon = geolib3.Polygon2.from_WKB(wkb)

        super().__init__(polygon)
        self.face_id = face_id
        self.edges = edges


class VehicleRestriction:
    def __init__(
            self,
            vehicle_restriction_id,
            access_id=None,
            universal_id=None,
            pass_id=None,
            weight_limit=None,
            axle_weight_limit=None,
            max_weight_limit=None,
            height_limit=None,
            width_limit=None,
            length_limit=None,
            payload_limit=None,
            min_eco_class=None,
            trailer_not_allowed=False,
            schedules=list(),
            road_elements=list(),
            conditions=list()):
        self.vehicle_restriction_id = vehicle_restriction_id
        self.access_id = access_id
        self.universal_id = universal_id
        self.pass_id = pass_id
        self.weight_limit = weight_limit
        self.axle_weight_limit = axle_weight_limit
        self.max_weight_limit = max_weight_limit
        self.height_limit = height_limit
        self.width_limit = width_limit
        self.length_limit = length_limit
        self.payload_limit = payload_limit
        self.min_eco_class = min_eco_class
        self.trailer_not_allowed = trailer_not_allowed
        self.schedules = schedules
        self.road_elements = road_elements
        self.conditions = conditions


@dataclass
class AnnotationPhraseElement:
    proximity: Proximity
    translations: List[Name]


@dataclass
class AnnotationPhrase:
    annotation_phrase_id: Optional[int]
    elements: List[AnnotationPhraseElement]
