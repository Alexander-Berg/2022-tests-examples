import json
import sys
import geopy.distance
import tabulate
import datetime
from argparse import ArgumentParser
from copy import deepcopy
import logging
import dateutil.parser
import dateutil.tz
import collections
import copy

from multiprocessing import Pool

import yatest
import maps.b2bgeo.mvrp_solver.annealing_mvrp.tests_lib.tools as tools
from maps.b2bgeo.libs.py_geo.util import COORDINATES_THRESHOLD, coordinates_equal


MINUTE_S = 60.0
HOUR_S = 3600.0
DAY_S = 86400.0

KM = 1000
TONNE = 1000

DEFAULT_VEHICLE_CAPACITY_KG = 1e9
DEFAULT_VEHICLE_CAPACITY_UNITS = 1e9

DEFAULT_LATE_PER_MINUTE_COST = 17.
DEFAULT_LATE_FIXED_COST = 1000.
DEFAULT_DROP_COST = 1e6

DEFAULT_VEHICLE_PER_HOUR_COST = 100.
DEFAULT_VEHICLE_FIXED_COST = 3000.
DEFAULT_VEHICLE_RUN_COST = 0.
DEFAULT_VEHICLE_PER_KM_COST = 8.
DEFAULT_VEHICLE_PER_TONNE_KM_COST = 0.
DEFAULT_VEHICLE_PER_LOC_COST = 0.
DEFAULT_SHIFT_DURATION_S = 2 * DAY_S

DEFAULT_THROUGHPUT_PENALTY_FIXED = 1000
DEFAULT_THROUGHPUT_PENALTY_PER_KG = 50
DEFAULT_THROUGHPUT_PENALTY_PER_UNIT = 100
DEFAULT_THROUGHPUT_PENALTY_PER_VEHICLE = 100000

BALANCED_RUNS_PENALTY_PER_HOUR = 200
BALANCED_RUNS_PENALTY_PER_STOP = 100

DEFAULT_MILEAGE_PENALTY_FIXED = 1000
DEFAULT_MILEAGE_PENALTY_KM = 100

DEFAULT_MIN_WEIGHT_PENALTY_FIXED = 1000
DEFAULT_MIN_WEIGHT_PENALTY_PER_KG = 50

DEFAULT_MAX_DROP_PERCENTAGE_PENALTY_FIXED = 1000
DEFAULT_MAX_DROP_PERCENTAGE_PENALTY_PER_PERCENT = 50

DEPOT_THROUGHPUT_INTERVAL_THRESHOLD_S = MINUTE_S
DEPOT_PENALTY_ACCURACY_REL = 0.02
DEPOT_PENALTY_ACCURACY_REL_FIXED = 2
TIME_ACCURACY_S = MINUTE_S
TIME_THRESHOLD_S = 0.01
DISTANCE_THRESHOLD_M = 1e-2

METRICS_GENERAL_THRESHOLD = 1e-6

DIMENSIONS = ['width_m', 'height_m', 'depth_m']
UNLIMITED_VEHICLE_VOLUME = dict((dim, 1e3) for dim in DIMENSIONS)
ZERO_VOLUME = dict((dim, 0.) for dim in DIMENSIONS)

WEIGHT_THRESHOLD_KG = 1e-2
UNITS_THRESHOLD = 1e-3
VOLUME_THRESHOLD_CBM = 1e-6
CUSTOM_THRESHOLD = 1e-9

# temporary solution for backward compatibility
PROXIMITY_FACTOR_BOOST = 1.5

AVG_SPEED_M_S = 20e3 / HOUR_S  # 20 KM/H

OrderTypes = ["pickup", "delivery"]
OrderOrDropoffTypes = ["pickup", "delivery", "drop_off"]


def parse_args():
    p = ArgumentParser(description="Check MVRP routing problem")

    p.add_argument('-s', '--source', required=True, help="Source input data [required].")
    p.add_argument('-d', '--distances', help="Distances data.")
    p.add_argument('-v', '--verbose', action='store_true', help="more info messages")
    p.add_argument(
        '--duration', choices=DURATION_CALLBACKS.keys(), default="solver",
        help="Transit duration calculator to use")
    p.add_argument(
        '--time-accuracy', type=int, default=TIME_ACCURACY_S,
        help="Times with differense less than accuracy considered to be equal in checks")

    return p.parse_args()


class VehicleStartInfo:
    def __init__(self, location, time, hard_window, visited):
        self.location = location
        self.time = time
        assert time >= 0, "route started before date from `options`"
        self.hard_window = hard_window
        self.visited = visited


def check(cond, msg, *args, **kwargs):
    if not cond:
        raise Exception(msg.format(*args, **kwargs))


def percent(a, b):
    return 0 if b <= 0 else 100.0 * a / b


def seconds_to_time(sec):
    sec = int(sec)
    return datetime.timedelta(
        days=sec // DAY_S,
        hours=sec % DAY_S // HOUR_S,
        minutes=sec % HOUR_S // MINUTE_S,
        seconds=sec % MINUTE_S)


def point_coords(point):
    return point["node"]["value"]["point"]


def nodes_arrival_time(nodes):
    return nodes[0]['arrival_time_s']


def route_arrival_time(route):
    return nodes_arrival_time(route['route'])


def start_service_time(node):
    return node["arrival_time_s"] + node['waiting_duration_s']


def solver_duration(point_from, point_to, start_time_s, distances):
    return point_to["transit_duration_s"], point_to["transit_distance_m"]


def points_equal(point_from, point_to):
    p = point_coords(point_from)
    q = point_coords(point_to)
    return coordinates_equal((p["lat"], p["lon"]), (q["lat"], q["lon"]), COORDINATES_THRESHOLD)


def haversine_distance_m(point_from, point_to):
    p = point_coords(point_from)
    q = point_coords(point_to)
    return geopy.distance.great_circle((p["lat"], p["lon"]), (q["lat"], q["lon"])).m


def haversine_20_duration(point_from, point_to, start_time_s, distances):
    dist_m = haversine_distance_m(point_from, point_to)
    return dist_m / AVG_SPEED_M_S, dist_m


def find_slice(matrix, timestamp):
    for interval in matrix["driving"]:
        if interval["begin"] <= timestamp and timestamp <= interval["end"]:
            return interval
    return matrix["driving"][0 if timestamp <= matrix["driving"][0]["begin"] else -1]


def traffic_duration(point_from, point_to, start_time_s, distances):
    p1 = point_from["node"]["value"]["id"]
    p2 = point_to["node"]["value"]["id"]
    interval = find_slice(distances, start_time_s)
    return interval["durations"][p1][p2], interval["distances"][p1][p2]


DURATION_CALLBACKS = {
    "solver": solver_duration,
    "haversine_20": haversine_20_duration,
    "traffic": traffic_duration
}


def parse_time_relative(s):
    """
    Parses time in "15", "15:40", "15:40:01", "2.15:40:01" formats.
    Returns timedelta object
    """
    days = '0'
    if '.' in s:
        days, s = s.split('.', 1)
    sep_count = s.count(':')
    if sep_count == 2:
        t = datetime.datetime.strptime(s, '%H:%M:%S')
    elif sep_count == 1:
        t = datetime.datetime.strptime(s, '%H:%M')
    else:
        t = datetime.datetime.strptime(s, '%H')
    return datetime.timedelta(days=int(days), hours=t.hour, minutes=t.minute, seconds=t.second)


def parse_time_absolute(timestamp_str, options):
    timestamp = dateutil.parser.parse(timestamp_str)
    timestamp = timestamp.astimezone(dateutil.tz.tzutc())

    date = dateutil.parser.parse(options["date"])
    date = date.replace(
        tzinfo=dateutil.tz.tzoffset(
            None,
            datetime.timedelta(hours=options["time_zone"]).total_seconds()
        )
    )
    date = date.astimezone(dateutil.tz.tzutc())

    return timestamp - date


def parse_time(s, options):
    if "T" in s:
        return parse_time_absolute(s, options)
    return parse_time_relative(s)


def parse_interval_sec_absolute(time_window, options):
    """
    https://en.wikipedia.org/wiki/ISO_8601#Time_intervals
    In format <start>/<end>. Examples:
    2007-03-01T13:00:00Z/2008-05-11T15:30:00Z
    2007-03-01T13:00:00+03/2008-05-11T15:30:00+03
    """
    t = time_window.split('/')
    if len(t) != 2:
        raise ValueError('Invalid time interval ISO 8601 format "{0}"'.format(time_window))
    return [parse_time_absolute(s.strip(), options).total_seconds() for s in t]


def parse_interval_sec_relative(time_window):
    """
    Parses time interval string separated by minus: "15:00 - 15:30".
    Time format can be any supported by parse_time.
    Returns [timedelta1, timedelta2]
    """
    return [parse_time_relative(s.strip()).total_seconds() for s in time_window.split('-')]


def check_time_range(begin, end, interval_str):
    if begin > end:
        raise ValueError(
            f'Invalid time interval "{interval_str}", interval ' +
            'beginning is greater than interval end')


def parse_interval_sec(time_window, options):
    if "/" in time_window:
        begin, end = parse_interval_sec_absolute(time_window, options)
    else:
        begin, end = parse_interval_sec_relative(time_window)
    check_time_range(begin, end, time_window)
    return begin, end


def work_break_time_window(work_break, work_start_time, route_start_time, last_stop_time):
    if 'work_time_range_till_rest' in work_break:
        time_window = work_break['work_time_range_till_rest']
        begin, end = parse_interval_sec_relative(time_window)
        check_time_range(begin, end, time_window)
        return work_start_time + begin, work_start_time + end
    elif 'work_time_range_from_start' in work_break:
        time_window = work_break['work_time_range_from_start']
        begin, end = parse_interval_sec_relative(time_window)
        check_time_range(begin, end, time_window)
        return route_start_time + begin, route_start_time + end
    elif 'continuous_travel_time_range' in work_break:
        time_window = work_break['continuous_travel_time_range']
        begin, end = parse_interval_sec_relative(time_window)
        check_time_range(begin, end, time_window)
        return last_stop_time + begin, last_stop_time + end
    else:
        return float('-inf'), float('inf')


class BalancedRun:
    def __init__(self, duration_s=0.0, stop_count=0):
        self.duration_s = duration_s
        self.stop_count = stop_count

    def __iadd__(self, other):
        self.duration_s += other.duration_s
        self.stop_count += other.stop_count
        return self

    def __sub__(self, other):
        return BalancedRun(self.duration_s - other.duration_s, self.stop_count - other.stop_count)

    def __itruediv__(self, k):
        self.duration_s *= 1.0 / k
        self.stop_count *= 1.0 / k
        return self


class BalancedGroups:
    def __init__(self):
        self.groups = collections.defaultdict(dict)

    def init_run(self, group_id, vehicle_id, shift_id):
        self.groups[group_id][(vehicle_id, shift_id)] = BalancedRun()

    def add_run(self, group_id, vehicle_id, shift_id, balanced_run):
        stats = self.groups[group_id].setdefault((vehicle_id, shift_id), BalancedRun())
        stats += balanced_run

    def __add__(self, other):
        result = copy.deepcopy(self)
        for group_id, runs in other.groups.items():
            for (vehicle_id, shift_id), stats in runs.items():
                result.add_run(group_id, vehicle_id, shift_id, stats)
        return result

    def penalty(self, penalties):
        penalty = 0.0
        for group_id, runs in self.groups.items():
            if not runs:
                continue
            average = BalancedRun()
            for stats in runs.values():
                average += stats

            average /= len(runs)

            squared_hours = 0
            squared_stops = 0
            for stats in runs.values():
                diff = average - stats
                squared_hours += (diff.duration_s/HOUR_S)**2
                squared_stops += diff.stop_count**2

            p = penalties[group_id]['penalty']
            penalty += p['hour'] * (squared_hours**0.5) + p['stop'] * (squared_stops**0.5)
        return penalty


class ShiftStats:
    def __init__(self):
        self.stops = 0
        self.duration_s = 0.0
        self.distance_m = 0.0

    def add(self, stops, duration_s, distance_m):
        self.stops += stops
        self.duration_s += duration_s
        self.distance_m += distance_m

    def __add__(self, other):
        result = ShiftStats()
        result += self
        result += other
        return result

    def __iadd__(self, other):
        self.add(other.stops, other.duration_s, other.distance_m)
        return self


class VehicleShifts:
    def __init__(self):
        self.stats = collections.defaultdict(ShiftStats)

    def add_stats(self, vehicle_id, shift_id, stops, duration_s, distance_m):
        self.stats[vehicle_id, shift_id].add(stops, duration_s, distance_m)

    def get_stats(self, vehicle_id, shift_id):
        return self.stats[vehicle_id, shift_id]

    def __add__(self, other):
        result = copy.deepcopy(self)
        for key, stats in other.stats.items():
            result.stats[key] += stats
        return result


class Metrics(object):
    ATTRIBUTES = [
        "duration_s",
        "service_duration_s",
        "rest_duration_s",
        "intershift_duration_s",
        "failed_time_window_duration_s",
        "failed_time_window_penalty",
        "failed_delivery_deadline_duration_s",
        "failed_delivery_deadline_penalty",
        "transit_distance_m",
        "transit_duration_s",
        "trailer_transit_distance_m",
        "trailer_transit_duration_s",
        "walking_transit_distance_m",
        "walking_transit_duration_s",
        "waiting_duration_s",
        "vehicle_transit_duration_s",
        "vehicle_transit_distance_m",
        "time_s",
        "work_start_time",
        "fixed_cost",
        "runs_cost",
        "locations_cost",
        "distance_cost",
        "duration_cost",
        "transport_work_cost",
        "custom_cost",
        "drop_penalty",
        "drop_penalty_percentage",
        "max_drop_percentage_penalty",
        "unfeasibility_penalty",
        "depot_penalty",
        "multiorders_penalty",
        "stop_count_penalty",
        "mileage_penalty",
        "min_stop_weight_penalty",
        "proximity_penalty",
        "global_proximity_penalty",
        "optional_tags_cost",
        "balanced_group_penalty",
        "orders_count",
        "walking_stops",
        "vehicle_stops",
        "stops",
        "unique_stops",
        "overtime_shifts_count",
        "overtime_duration_s",
        "overtime_penalty",
        "use_trailer",
        "utilization_weight_kg",
        "utilization_units",
        "utilization_volume_m3",
        "utilization_weight_perc",
        "utilization_units_perc",
        "utilization_volume_perc",
        "total_weight_kg",
        "total_units",
        "total_volume_m3",
        "total_fails_penalty",
        "arrival_after_start_penalty",
        "transit_time_penalty",
        "total_custom_value"
    ]

    def __init__(self):
        for attr in self.ATTRIBUTES:
            setattr(self, attr, 0)
        self.balanced_groups = BalancedGroups()
        self.vehicle_shifts = VehicleShifts()
        self.location_groups = set()

    def __add__(self, other):
        result = Metrics()
        for attr in self.ATTRIBUTES + ['balanced_groups', 'vehicle_shifts']:
            setattr(result, attr, getattr(self, attr) + getattr(other, attr))
        result.location_groups = self.location_groups | other.location_groups
        return result

    def total_cost(self):
        return self.fixed_cost \
            + self.runs_cost \
            + self.locations_cost \
            + self.distance_cost \
            + self.duration_cost \
            + self.transport_work_cost \
            + self.custom_cost

    def guaranteed_penalty(self):
        return self.failed_time_window_penalty \
            + self.failed_delivery_deadline_penalty \
            + self.stop_count_penalty \
            + self.mileage_penalty \
            + self.overtime_penalty \
            + self.proximity_penalty \
            + self.global_proximity_penalty \
            + self.optional_tags_cost \
            + self.balanced_group_penalty \
            + self.drop_penalty \
            + self.max_drop_percentage_penalty \
            + self.unfeasibility_penalty \
            + self.depot_penalty \
            + self.multiorders_penalty \
            + self.min_stop_weight_penalty \
            + self.arrival_after_start_penalty \
            + self.transit_time_penalty


def check_limits(dct, minimum, error_msg):
    for k, v in dct.items():
        if isinstance(v, int) or isinstance(v, float):
            check(v >= minimum, "{} {}: {}", error_msg, k, v)
        elif isinstance(v, dict):
            check_limits(v, minimum, error_msg)


def get_volume_cbm(vol, default=0):
    return vol["width_m"]*vol["height_m"]*vol["depth_m"] if vol else default


def fits_with_rotation(loc_dims, vehicle_dims):
    # rewrite of Volume::dominatesWithAlignedHeight
    if vehicle_dims[1] < loc_dims[1]:
        return False
    w, h = (fun(vehicle_dims[0], vehicle_dims[2]) for fun in (max, min))
    a, b = (fun(loc_dims[0], loc_dims[2]) for fun in (max, min))
    if h < b:
        return False
    if a <= w:
        return True
    return 2 * a * b * w + (a ** 2 - b ** 2) * (a ** 2 + b ** 2 - w ** 2) ** 0.5 <= h * (a ** 2 + b ** 2)


def fits_by_volume(vehicle, loc):
    if loc['type'] == 'depot':
        return True
    vehicle_cap = vehicle['capacity']
    k = vehicle_cap.setdefault('limits', {}).setdefault('volume_perc', 100) * 0.01
    vehicle_dims = [vehicle_cap['volume'][dim] * k ** (1./3) for dim in DIMENSIONS]
    loc_volume = loc['shipment_size']['volume']
    loc_dims = [loc_volume[dim] for dim in DIMENSIONS]
    lvtype = loc_volume['type']
    lvalign = loc_volume['align']
    assert lvalign in ['height', 'all_axes'], \
        "Location id {} has unknown volume alignment {}".format(loc["id"], lvalign)
    if lvtype == 'bulk':
        return True
    if lvalign == 'all_axes':
        if lvtype == 'rigid':
            return all(ld <= vd for ld, vd in zip(sorted(loc_dims), sorted(vehicle_dims)))
        if lvtype == 'fixed_bottom':
            return loc_dims[1] <= vehicle_dims[1] and all(
                fun(loc_dims[0], loc_dims[2]) <= fun(vehicle_dims[0], vehicle_dims[2])
                for fun in (min, max))
    if lvalign == 'height':
        if lvtype == 'fixed_bottom':
            return fits_with_rotation(loc_dims, vehicle_dims)
        if lvtype == 'rigid':
            def rotate(lst, n):
                return lst[-n:] + lst[:-n]
            return any(fits_with_rotation(rotate(loc_dims, i), vehicle_dims) for i in range(3))
    raise Exception("Location id {} has unknown volume type {}".format(loc["id"], lvtype))


def init_oot_penalty(item):
    penalty = item.setdefault('penalty', {})
    out_of_time = penalty.setdefault("out_of_time", {})
    out_of_time.setdefault("fixed", DEFAULT_LATE_FIXED_COST)
    out_of_time.setdefault("minute", DEFAULT_LATE_PER_MINUTE_COST)
    return penalty


def init_location_penalty(loc):
    return init_oot_penalty(loc)


def init_shift_penalty(shift):
    penalty = init_oot_penalty(shift)

    for name in ["stop_lack", "stop_excess"]:
        stop_penalty = penalty.setdefault(name, {})
        stop_penalty.setdefault("fixed", 0)
        stop_penalty.setdefault("per_stop", 0)

    mileage_penalty = penalty.setdefault("max_mileage", {})
    mileage_penalty.setdefault("fixed", DEFAULT_MILEAGE_PENALTY_FIXED)
    mileage_penalty.setdefault("km", DEFAULT_MILEAGE_PENALTY_KM)


def init_shift(shift):
    init_shift_penalty(shift)
    shift['max_duration_s'] = max(0, shift.get('max_duration_s', DEFAULT_SHIFT_DURATION_S))
    shift['minimal_stops'] = max(0, shift.get('minimal_stops', 0))
    if 'maximal_stops' in shift:
        shift['maximal_stops'] = max(0, shift['maximal_stops'])


def stop_lack_penalty(shift, stops):
    missing_stop_count = shift['minimal_stops'] - stops
    if missing_stop_count <= 0:
        return 0
    penalty = shift['penalty']['stop_lack']
    return penalty['fixed'] + missing_stop_count * penalty['per_stop']


def stop_excess_penalty(shift, stops):
    maximal_stops = shift.get('maximal_stops')
    if maximal_stops is None:
        return 0
    extra_stop_count = stops - maximal_stops
    if extra_stop_count <= 0:
        return 0
    penalty = shift['penalty']['stop_excess']
    return penalty['fixed'] + extra_stop_count * penalty['per_stop']


def mileage_penalty(mileage_m, shift):
    max_mileage_km = shift.get('max_mileage_km')
    if max_mileage_km is None:
        return 0
    assert max_mileage_km >= 0
    excess_km = mileage_m / 1000.0 - max_mileage_km
    if excess_km <= 0:
        return 0
    penalty = shift['penalty']['max_mileage']
    return penalty['fixed'] + excess_km * penalty['km']


def failed_time_penalty(duration_s, penalty, kind):
    p = penalty.get(kind, {})
    penalty_fixed = p.get('fixed', penalty['out_of_time']['fixed'])
    penalty_min = p.get('minute', penalty['out_of_time']['minute'])
    return penalty_fixed + penalty_min * duration_s / MINUTE_S


def _generate_missing_ids(ids):
    existing_ids = [x for x in ids if x is not None]
    existing_ids_set = set(existing_ids)
    assert len(existing_ids_set) == len(existing_ids)
    id = 0
    for i in range(len(ids)):
        if ids[i] is None:
            while id in existing_ids_set:
                id += 1
            ids[i] = id
            id += 1


def _get_time_interval_from_time_window(time_window, options):
    start, end = parse_interval_sec(time_window, options)
    return {
        "start": start,
        "end": end
    }


class Locations(dict):
    """ Dictionary representing locations from the request. """

    def __init__(self, source):
        dict.__init__(self)

        self.depot_ids = []
        depots = [source['depot']] if 'depot' in source else source['depots']

        ids = [x.get('id') for x in depots] + [x.get('id') for x in source['locations']]
        _generate_missing_ids(ids)

        id_index = 0

        for depot_loc in depots:
            depot = self._add_location(depot_loc, ids[id_index], source['options'], is_depot=True)
            id_index += 1
            self[depot['id']] = depot
            self.depot_ids.append(depot["id"])

        for sloc in source["locations"]:
            loc = self._add_location(sloc, ids[id_index], source['options'], is_depot=False)
            id_index += 1
            lid = loc['id']
            check(lid not in self.depot_ids, "Depot id listed in locations: {}", lid)
            check(lid not in self, "Duplicate location id: {}", lid)
            self[lid] = loc

        self._check_pickup_and_delivery()

    def _check_pickup_and_delivery(self):
        for loc in self.values():
            loc['pud'] = False
            loc['delivery_cnt'] = 0
        for loc in self.values():
            lid = loc.get('delivery_to')
            if lid is not None:
                assert loc['type'] == 'pickup', 'Only locations of type `pickup` can have `delivery_to` field defined.'
                assert lid in self, "Location with id {} not found".format(lid)
                loc2 = self[lid]
                assert loc2['type'] == 'delivery', 'Location which is referenced by `delivery_to` must be of type `delivery`.'
                loc['pud'] = True
                loc2['pud'] = True
                if loc2.get('pickup_from_any', False):
                    loc2['delivery_cnt'] = 1
                else:
                    loc2['delivery_cnt'] += 1
            assert not (loc.get('delivery_to_any') and loc['type'] != 'pickup'), \
                "delivery_to_any field can be specified in pickup locations only"

    def _add_location(self, loc, id, options, is_depot):
        loc = deepcopy(loc)
        loc["id"] = id
        loc["node_type"] = "depot" if is_depot else "location"

        check("point" in loc, f'{loc["node_type"]} missing required key `point`: id = {loc["id"]}')
        check(("time_window" in loc) is not ("time_windows" in loc),
                f'both `time_window` and `time_windows` are present or missed in location: id = {loc["id"]}')

        loc.setdefault('hard_window', False)
        loc.setdefault("service_duration_s", 0)
        loc.setdefault("preliminary_service_duration_s", 0)
        loc.setdefault("shared_service_duration_s", 0)
        if "time_window" in loc:
            loc["time_windows"] = [{"time_window": loc["time_window"]}]
        loc["time_intervals"] = [
            _get_time_interval_from_time_window(item["time_window"], options)
            for item in loc["time_windows"]
        ]
        if is_depot:
            for key in ["time_windows_loading", "time_windows_unloading", "time_windows_refilling"]:
                if key in loc:
                    if "time_window" in loc[key]:
                        loc["time_windows"].append({"time_window": loc[key]["time_window"]})
                    if "time_windows" in loc[key]:
                        loc["time_windows"].extend(loc[key]["time_windows"])

            # This times used to depot load timerange calculation
            loc["time_start"] = 1e9
            loc["time_end"] = -1e9
            loc["hard_window"] = loc["hard_window"] or "hard_time_window" in loc["time_windows"][0]
            for item in loc["time_windows"]:
                if "hard_time_window" in item:
                    time_start, time_end = parse_interval_sec(item["hard_time_window"], options)
                else:
                    time_start, time_end = parse_interval_sec(item["time_window"], options)
                loc["time_start"] = min(time_start, loc["time_start"])
                loc["time_end"] = max(time_end, loc["time_end"])
            assert loc["time_start"] <= loc["time_end"]

        penalty = init_location_penalty(loc)
        if not is_depot:
            loc.setdefault('type', 'delivery')
            loc.setdefault("depot_duration_s", 0)
            penalty.setdefault('drop', DEFAULT_DROP_COST)
            self.init_shipment_size(loc)
        else:
            loc['type'] = 'depot'
            loc.setdefault("finish_service_duration_s", 0)
            throughput_penalty = penalty.setdefault("throughput", {})
            throughput_penalty.setdefault("fixed", DEFAULT_THROUGHPUT_PENALTY_FIXED)
            throughput_penalty.setdefault("kg", DEFAULT_THROUGHPUT_PENALTY_PER_KG)
            throughput_penalty.setdefault("unit", DEFAULT_THROUGHPUT_PENALTY_PER_UNIT)
            throughput_penalty.setdefault("vehicle", DEFAULT_THROUGHPUT_PENALTY_PER_VEHICLE)

            for attr in ["shipment_size"]:
                check(attr not in loc, "Depot cannot have key: {}", attr)

        check_limits(penalty, 0, "Negative penalty detected")
        return loc

    def init_shipment_size(self, loc):
        sz = loc.setdefault('shipment_size', {})
        sz.setdefault('units', 0.)
        sz.setdefault('weight_kg', 0.)
        sz.setdefault('volume', ZERO_VOLUME)
        if 'volume_cbm' not in sz:
            sz['volume_cbm'] = get_volume_cbm(sz['volume'])
        sz['volume'].setdefault('type', 'bulk')
        sz['volume'].setdefault('align', 'all_axes')

    def first_depot(self):
        return self[self.depot_ids[0]]

    def iter_depots(self):
        for depot_id in self.depot_ids:
            yield self[depot_id]

    def some_depots_have_flexible_start_time_and_soft_window(self):
        return any(depot.get("flexible_start_time") and not depot.get('hard_window')
                   for depot in self.iter_depots())

    def some_depots_have_flexible_start_time(self):
        return any(depot.get("flexible_start_time") for depot in self.iter_depots())

    def all_depots_have_hard_window(self):
        return all(depot.get("hard_window") for depot in self.iter_depots())

    def order_location_ids(self):
        return [loc_id for loc_id, loc in self.items() if loc['type'] in ("pickup", "delivery")]

    def initial_drop_penalty(self):
        return sum([loc["penalty"].get("drop", 0) for loc in self.values() if
                    loc['type'] in ("pickup", "delivery") and "penalty" in loc])

    def vehicle_planned_locations(self, vehicle, shift_id=None):
        planned_locations = vehicle.get('planned_route', {}).get('locations', [])

        ret = []
        for ploc in planned_locations:
            check(ploc["id"] in self, "Unknown planned location id: {}", ploc["id"])
            if shift_id is None or ploc.get('shift_id') == shift_id:
                ret.append(self[ploc["id"]])

        return ret

    def vehicle_visited_locations(self, vehicle, shift_id=None):
        visited_locations = vehicle.get('visited_locations', [])

        ret = []
        cur_shift_id = vehicle['default_shift_id']
        for vloc in visited_locations:
            check(vloc["id"] in self, "Unknown visited location id: {}", vloc["id"])
            if 'shift_id' in vloc:
                cur_shift_id = vloc['shift_id']
            if shift_id is None or cur_shift_id == shift_id:
                ret.append(vloc)

        return ret

    def first_vehicle_depot(self, vehicle):
        depot_id = vehicle.get("first_depot_id")
        return None if depot_id is None else self[depot_id]

    def last_vehicle_depot(self, vehicle):
        depot_id = vehicle.get("last_depot_id")
        return None if depot_id is None else self[depot_id]

    def fisrt_vehicle_location(selt, vehicle):
        return None

    def vehicle_visited_shift_start(self, vehicle, shift_id):
        visited_locations = vehicle.get('visited_locations', [])
        if not visited_locations:
            return None
        start = visited_locations[0]
        if any(['time' in loc for loc in visited_locations]) and shift_id == start.get('shift_id', vehicle['default_shift_id']):
            return start
        return None

    def vehicle_start(self, vehicle, first_location_id, shift_id, first_used_shift, options):
        start = self.vehicle_visited_shift_start(vehicle, shift_id)
        if start:
            if 'time' in start:
                time_start = parse_time(start['time'], options).total_seconds()
                loc = self[start['id']]
                time_start -= loc['service_duration_s'] * vehicle.get('service_duration_multiplier', 1)
                time_start -= loc['preliminary_service_duration_s'] * vehicle.get('service_duration_multiplier', 1)
                time_start -= loc['shared_service_duration_s'] * vehicle.get('shared_service_duration_multiplier', 1)
            else:
                if shift_id is not None:
                    shift = vehicle['shifts'][shift_id]
                    time_start = parse_interval_sec(shift['time_window'], options)[0]
                else:
                    time_start = self.first_vehicle_depot(vehicle)['time_start']
            return VehicleStartInfo(self[start["id"]], time_start, True, True)

        if vehicle.get('start_at') is not None and first_used_shift:
            start_loc = self[vehicle['start_at']]
        elif vehicle.get('visit_depot_at_start', True):
            start_loc = self.first_vehicle_depot(vehicle)
        else:
            start_loc = self[first_location_id]

        if shift_id is not None:
            shift = vehicle['shifts'][shift_id]
            time_start = parse_interval_sec(shift['time_window'], options)[0]
            hard_window = shift.get('hard_window', False)
        else:
            if start_loc['type'] != 'depot':
                time_start = parse_interval_sec(start_loc['time_window'], options)[0]
            else:
                time_start = start_loc['time_start']
            hard_window = start_loc.get('hard_window', False)
        return VehicleStartInfo(start_loc, time_start, hard_window, False)


def get_route_shift(vehicle, route):
    shift_id = route.get('shift', {}).get('id')
    return vehicle['shifts'][shift_id] if shift_id is not None else None


def get_route_nodes(route):
    nodes = route['route']
    shift = route.get('shift')
    if shift:
        if 'start' in shift:
            nodes = [deepcopy(shift['start'])] + nodes
            nodes[0]['shift_location'] = True
        if 'end' in shift:
            nodes = nodes + [deepcopy(shift['end'])]
            nodes[-1]['shift_location'] = True
    return nodes


class Vehicles(dict):
    """ Dictionary representing vehicles from the request """

    def _get_vehicles(self, source):
        vehicles = deepcopy(source.get("vehicles"))
        vehicle = deepcopy(source.get("vehicle"))

        if vehicles and vehicle:
            raise Exception("Both 'vehicles' and 'vehicle' tags are present")

        if not vehicles and not vehicle:
            raise Exception("No vehicles found in request")

        if vehicle:
            vehicle.setdefault('id', 0)
            vehicles = [vehicle]
        elif isinstance(vehicles, dict):
            vehicles_array = []
            for idx in range(vehicles['number_of_vehicles']):
                vehicle = deepcopy(vehicles)
                del vehicle["number_of_vehicles"]
                vehicle["id"] = idx
                vehicles_array.append(vehicle)
            vehicles = vehicles_array
        return vehicles

    def __init__(self, source, locations):
        dict.__init__(self)

        self.options = source['options']
        self.balanced_groups = BalancedGroups()

        for vehicle in self._get_vehicles(source):
            capacity = vehicle.setdefault('capacity', {})
            capacity.setdefault('units', DEFAULT_VEHICLE_CAPACITY_UNITS)
            capacity.setdefault('weight_kg', DEFAULT_VEHICLE_CAPACITY_KG)
            capacity.setdefault('volume', UNLIMITED_VEHICLE_VOLUME)
            capacity['volume_cbm'] = get_volume_cbm(capacity['volume'])

            vehicle.setdefault('cost', {})
            if (isinstance(vehicle['cost'], dict)):
                vehicle['cost'].setdefault('fixed', DEFAULT_VEHICLE_FIXED_COST)
                vehicle['cost'].setdefault('run', DEFAULT_VEHICLE_RUN_COST)
                vehicle['cost'].setdefault('hour', DEFAULT_VEHICLE_PER_HOUR_COST)
                vehicle['cost'].setdefault('km', DEFAULT_VEHICLE_PER_KM_COST)
                vehicle['cost'].setdefault('location', DEFAULT_VEHICLE_PER_LOC_COST)
                vehicle['cost'].setdefault('tonne_km', DEFAULT_VEHICLE_PER_TONNE_KM_COST)

            vehicle["shifts"] = dict((shift["id"], shift) for shift in vehicle.get('shifts', []))
            first_shift = self.first_shift(vehicle)
            vehicle["default_shift_id"] = first_shift["id"] if first_shift else None
            for shift in vehicle["shifts"].values():
                init_shift(shift)
                bgroup_id = shift.get('balanced_group_id')
                if bgroup_id:
                    self.balanced_groups.init_run(bgroup_id, vehicle['id'], shift['id'])

            vehicle.setdefault('', True)
            vehicle.setdefault('return_to_depot', True)

            depot_ids = vehicle.get('depot_id')
            if depot_ids is not None:
                if not isinstance(depot_ids, list):
                    depot_ids = [depot_ids]
                for depot_id in depot_ids:
                    depot = locations[depot_id]
                    check(depot['node_type'] == 'depot',
                          'A non-depot location is referenced by a vehicle as a depot.')

            if 'incompatible_load_types' in vehicle:
                vehicle['incompatible_types'] = IncompatiblityHandler(vehicle, 'incompatible_load_types')

            self[vehicle['id']] = vehicle

    def first_shift(self, vehicle):
        if not vehicle['shifts']:
            return None
        return min(vehicle["shifts"].values(), key=lambda shift: parse_interval_sec(shift["time_window"], self.options))

    def shifts_total(self):
        return sum(max(1, len(vehicle.get('shifts', {}))) for vehicle in self.values())


# skips consequent item repeats, the same items can still occur in the result
# when they are separated by the other items
def iter_uniq(values):
    prev = []
    for v in values:
        if not prev or prev[0] != v:
            yield v
            prev = [v]


# the same as iter_uniq but returns list
def uniq_list(values):
    return list(iter_uniq(values))


def route_title(route):
    title = 'Vehicle ' + str(route['vehicle_id'])
    shift_id = route.get('shift', {}).get('id')
    if shift_id is not None:
        title += f', shift {shift_id}'
    return title


# filter input tags: only tags which match at least one regexp survive
def match_tags(tags, regexs):
    import _solver as solver
    return list(filter(
        lambda tag: (
            tag in regexs or
            any(solver.posix_eregex_match(tag, regex) for regex in regexs)
        ),
        tags))


class IncompatiblityHandler:
    def __init__(self, source, name):
        self.incompatible = collections.defaultdict(set)
        for types in source.get(name, []):
            for type1 in types:
                for type2 in types:
                    if type1 != type2:
                        self.incompatible[type1].add(type2)

    def size(self):
        return len(self.incompatible)

    def find_incompatible(self, types):
        result = set()
        for type1 in types:
            incompatible = self.incompatible.get(type1)
            for type2 in incompatible & types:
                if type1 != type2:
                    result.add(tuple(sorted([type1, type2])))
        return result


# Computes depot load by time intervals with fixed duration as it is done
# in solver for multi-threading purposes.
class DiscreteDepotLoad:
    def __init__(self, start_time_s, step_s=10 * MINUTE_S):
        self.start_time_s = start_time_s
        self.step_s = step_s
        self.load = collections.defaultdict(float)

    def addLoad(self, start_s, end_s, load):
        self.loadPerStep(start_s, end_s, load * HOUR_S / (end_s - start_s))

    def addVehicle(self, start_s, end_s):
        self.loadPerStep(start_s, end_s, 1)

    def loadPerStep(self, start_s, end_s, load_per_step):
        n1 = 1.0 * (start_s - self.start_time_s) / self.step_s
        index1 = int(n1)
        n2 = 1.0 * (end_s - self.start_time_s) / self.step_s
        index2 = int(n2)

        if index1 == index2:
            self.load[index1] += (end_s - start_s) / self.step_s * load_per_step
            return
        first_interval_coeff = 1.0 - (n1 - index1)
        last_interval_coeff = n2 - index2
        self.load[index1] += first_interval_coeff * load_per_step
        self.load[index2] += last_interval_coeff * load_per_step
        for index in range(index1 + 1, index2):
            self.load[index] += load_per_step

    def __iter__(self):
        for index, load in sorted(self.load.items()):
            yield load, self.start_time_s + index * self.step_s


class DepotLoadEvent:
    def __init__(self, time_s, diff_per_h):
        self.time_s = time_s
        self.diff_per_h = diff_per_h


# Computes exact depot load
class DepotLoad:
    def __init__(self):
        self.events = []

    def add(self, start_s, end_s, load_per_h):
        self.events.append(DepotLoadEvent(start_s, load_per_h))
        self.events.append(DepotLoadEvent(end_s, -load_per_h))

    def __iter__(self):
        self.events.sort(key=lambda x: (x.time_s, -x.diff_per_h))

        load_per_h = 0

        for event1, event2 in zip(self.events, self.events[1:]):
            load_per_h += event1.diff_per_h
            assert load_per_h >= -1e-6

            yield load_per_h, float(event2.time_s - event1.time_s)

        load_per_h += self.events[-1].diff_per_h
        assert abs(load_per_h) <= 1e-6


class SolutionChecker:
    def __init__(self, solution, source_data, distances_data, duration_callback,
                 time_accuracy_s, soft_windows, expected_status):
        self.solution = solution
        self.routes = solution['routes']
        self.source = json.loads(source_data)
        self.options = self.source['options']
        self.distances = json.loads(distances_data).get("matrices") if distances_data else None
        self.locations = Locations(self.source)
        self.vehicles = Vehicles(self.source, self.locations)
        self.duration_callback = duration_callback
        self.time_accuracy_s = time_accuracy_s
        self.soft_windows = soft_windows
        self.expected_status = expected_status
        self.incompatible_types = IncompatiblityHandler(self.source.get('options', {}), 'incompatible_load_types')
        self.incompatible_zones = IncompatiblityHandler(self.source.get('options', {}), 'incompatible_zones')

        self.balanced_groups = {}
        for bgroup in self.options.get('balanced_groups', []):
            penalty = bgroup.setdefault('penalty', {})
            penalty.setdefault('hour', BALANCED_RUNS_PENALTY_PER_HOUR)
            penalty.setdefault('stop', BALANCED_RUNS_PENALTY_PER_STOP)
            self.balanced_groups[bgroup['id']] = bgroup

        self.location2group = {}
        self.solid = []
        self.dependent = []
        self.location_group_size = []
        for group_id, location_group in enumerate(self.options.get('location_groups', [])):
            self.solid.append(location_group.get('solid', False))
            self.dependent.append(location_group.get('dependent', False))
            self.location_group_size.append(len(location_group['location_ids']))
            for loc_id in location_group['location_ids']:
                assert loc_id not in self.location2group, "Location was specified in more than one location group. "
                self.location2group[loc_id] = group_id
        if self.location2group:
            logging.info(
                "{} location groups have been loaded for {} locations".format(
                    len(set(self.location2group.values())),
                    len(self.location2group)
                )
            )

        for route in self.routes:
            vehicle = self.vehicles[route['vehicle_id']]
            for elem in route['route']:
                node = elem['node']
                if node['type'] == 'depot':
                    depot_id = node['value']['id']
                    if 'first_depot_id' not in vehicle:
                        vehicle['first_depot_id'] = depot_id
                    vehicle['last_depot_id'] = depot_id
                else:
                    loc = node['value']
                    self.locations.init_shipment_size(loc)

    def check_status(self):
        """
        Check for correct status of the solution
        """
        status = self.solution["solver_status"]
        check(status == self.expected_status,
              "Solution status is '%s' and expected status is '%s'" %
              (status, self.expected_status))

    def check_locations_are_assigned(self):
        """
        Check that all locations are visited and visited only once
        """
        visited = set()
        alternate_pickups_for_visited_delivery = set()
        delivered_pickups = set()
        multiple_visits = set()

        for route in self.solution["routes"]:
            vehicle = self.vehicles[route['vehicle_id']]
            nodes = get_route_nodes(route)

            for elem in nodes:
                node = elem["node"]
                if node['type'] == 'break':
                    continue

                if node["value"].get("type", "NO_NODE_TYPE") == "parking" and node["value"].get("parking_type") == "vehicle":
                    continue

                node_id = node["value"]["id"]

                check(node_id in self.locations, "Unknown location id in route: {}", node_id)

                loc = self.locations[node_id]

                check(
                    (loc["node_type"] == node["type"])
                    or (loc["node_type"] == 'depot' and node["type"] == 'location' and node['value'].get('type', 'NO_NODE_TYPE') == 'crossdock'),
                    "Invalid route node type for location id {}: '{}'", node_id, node["type"]
                )

                is_shift_node = elem.get('shift_location')
                if node["type"] != 'depot' and not is_shift_node:
                    check(
                        loc.get("type", 'NO_LOCATION_TYPE') == node['value'].get('type', 'NO_NODE_TYPE')
                        or (loc.get("type", 'NO_LOCATION_TYPE') == 'depot' and node['value'].get('type', 'NO_NODE_TYPE') == 'crossdock'),
                        "Invalid type for route location id {}: '{}'", node_id, loc["type"]
                    )

                if "drop_reason" not in node["value"] and not is_shift_node:
                    check(
                        any(
                            parse_interval_sec(item["time_window"], self.options) == parse_interval_sec(node["used_time_window"], self.options)
                            for item in loc["time_windows"]
                        ),
                        "Invalid time window for route location {}: expected: {}; got from solver: {}",
                        node_id,
                        loc["time_windows"],
                        node["used_time_window"]
                    )

                if loc["type"] in OrderTypes and not is_shift_node:
                    if node_id in visited:
                        if loc['type'] == 'delivery' and 'pickup_id' in node['value'] and node['value']['pickup_id'] not in delivered_pickups:
                            delivered_pickups.add(node['value']['pickup_id'])
                        else:
                            multiple_visits.add(node_id)
                    visited.add(node_id)

        check(
            not multiple_visits,
            "List of locations visited multiple times: {}", list(multiple_visits)
        )

        # Check not visited

        for vehicle in self.vehicles.values():
            visited.update(loc["id"] for loc in self.locations.vehicle_visited_locations(vehicle))

        not_visited = set(self.locations.order_location_ids()) - visited

        for loc_id in not_visited:
            loc = self.locations[loc_id]
            if 'delivery_to' in loc and loc['delivery_to'] in visited:
                alternate_pickups_for_visited_delivery.add(loc_id)

        not_visited -= alternate_pickups_for_visited_delivery

        drop_penalty = 0
        for loc_id in not_visited:
            drop_penalty += self.locations[loc_id]["penalty"]["drop"]

        status = self.solution["solver_status"]
        if status == 'SOLVED':
            check(not not_visited, "Not visited locations: {}", not_visited)
        elif status == 'PARTIAL_SOLVED':
            drop_ids = set(loc['id'] for loc in self.solution["dropped_locations"])
            check(
                drop_ids == not_visited,
                "Some of dropped locations are not found in results.\n" +
                "Dropped location ids: " + ', '.join(map(str, drop_ids)) + "\n" +
                "Not visited location ids: " + ', '.join(map(str, not_visited))
            )

        return drop_penalty

    def iter_route_locations(self, route, node_filter=None, type_filter=None, nodes=False):
        def make_iterable(value):
            if value is not None and \
               not isinstance(value, list) and \
               not isinstance(value, set):
                return [value]
            return value

        node_filter = make_iterable(node_filter)
        type_filter = make_iterable(type_filter)

        for elem in route:
            node = elem['node']
            if node_filter is None or node["type"] in node_filter:
                if type_filter is not None:
                    if node['type'] != 'location':
                        continue
                    loc = self.locations[node["value"]["id"]]
                    if loc["type"] not in type_filter:
                        continue
                if nodes:
                    yield node["value"]
                else:
                    yield self.locations[node["value"]["id"]]

    def iter_orders(self, route):
        for loc in self.iter_route_locations(route, type_filter=OrderTypes):
            yield loc

    def iter_orders_and_dropoffs(self, route):
        for loc in self.iter_route_locations(route, type_filter=OrderOrDropoffTypes):
            yield loc

    def iter_crossdocks(self, route):
        for loc in self.iter_route_locations(route, node_filter=['location'], nodes=True):
            if loc.get('type', 'depot') == 'crossdock':
                print(loc)
                yield loc

    def check_pickup_and_delivery(self):
        for route in self.routes:
            pickedUpLocations = set()
            delivery_cnts = collections.defaultdict(int)
            lifo = []
            for loc in self.iter_route_locations(route['route'], type_filter=OrderTypes, nodes=True):
                if loc.get('in_lifo_order', False):
                    if loc['type'] == 'pickup':
                        lifo.append(loc.get('delivery_to'))
                    else:
                        check(loc['type'] == 'delivery', 'unsupported location type')
                        if lifo:
                            check(lifo[-1] == loc['id'], 'wrong LIFO order')
                            lifo.pop()
                        else:
                            check('pickup_id' not in loc and 'pickup_ids' not in loc, 'wrong LIFO order')
                if 'delivery_to' in loc:
                    check(loc['type'] == 'pickup', 'unsupported location type')
                    pickedUpLocations.add(loc['id'])
                elif 'pickup_id' in loc or 'pickup_ids' in loc:
                    check(loc['type'] == 'delivery', 'unsupported location type')
                    pickup_ids = loc['pickup_ids'] if 'pickup_ids' in loc else [loc['pickup_id']]
                    for lid in pickup_ids:
                        check(lid in pickedUpLocations, "Delivery without pickup detected.")
                        pickedUpLocations.remove(lid)
                        delivery_cnts[loc['id']] += 1
            check((delivery is None for delivery in lifo), 'wrong LIFO order')
            check(
                pickedUpLocations == set(),
                'Not all picked up location were delivered: ' + ', '.join(map(str, sorted(pickedUpLocations)))
            )
            for loc_id, cnt in delivery_cnts.items():
                check(cnt == self.locations[loc_id]['delivery_cnt'], '{} pickups are not delivered to location {}',
                      self.locations[loc_id]['delivery_cnt'] - cnt, loc_id)

    def check_locations_availability(self):
        """
        Check that arrival time is in available time window and
        there is enough time for service.
        """
        if self.soft_windows:
            return

        for route in self.solution["routes"]:
            for item in route["route"]:
                if item["node"]["type"] != "location":
                    continue
                node = item["node"]["value"]
                location = self.locations[node["id"]]
                time_windows = location["time_windows"]
                arrival = int(item["arrival_time_s"])
                waiting = int(item["waiting_duration_s"])
                time_s = arrival + waiting

                soft_window_allowed = not location['hard_window']
                if soft_window_allowed:
                    continue

                time_window_ok = any(
                    (
                        time_s + self.time_accuracy_s >= interval["start"] and
                        time_s - self.time_accuracy_s <= interval["end"]
                    ) for interval in location["time_intervals"]
                )

                check(
                    time_window_ok,
                    "Out of time windows: location id {}, time: {}, time windows: {}, arrival: {}, waiting: {}",
                    location['id'], seconds_to_time(time_s), time_windows,
                    seconds_to_time(arrival), seconds_to_time(waiting)
                )

    def get_planned_routes(self):
        for vehicle_id, routes in self.get_vehicle_routes().items():
            vehicle = self.vehicles[vehicle_id]
            first_used_shift_id = routes[0].get('shift', {}).get('id')
            last_used_shift_id = routes[-1].get('shift', {}).get('id')

            is_planned = True
            checked_shifts = set()
            expected_loc_ids = []
            first_location_id = routes[0]["route"][0]['node']['value']['id']
            for route in routes:
                shift_id = route.get('shift', {}).get('id')

                if shift_id not in checked_shifts:
                    if expected_loc_ids:
                        is_planned = False
                        break
                    checked_shifts.add(shift_id)
                    planned = self.locations.vehicle_planned_locations(vehicle, shift_id)
                    expected_loc_ids = [loc["id"] for loc in planned]
                    if not expected_loc_ids:
                        is_planned = False
                        break
                    start_info = self.locations.vehicle_start(vehicle, first_location_id, shift_id, shift_id == first_used_shift_id, self.options)
                    if not start_info.visited:
                        expected_loc_ids[0:0] = [start_info.location['id']]  # garage or depot
                        if vehicle.get('start_at') is not None and vehicle.get('visit_depot_at_start', True) and shift_id == first_used_shift_id:
                            # started from garage but still need to go to the depot before orders
                            expected_loc_ids[1:1] = [self.locations.first_vehicle_depot(vehicle)['id']]
                    last_loc = self.locations.last_vehicle_depot(vehicle)['id']
                    return_to_depot = vehicle['return_to_depot']
                    finish_at = vehicle.get('finish_at')
                    if shift_id == last_used_shift_id and finish_at is not None:
                        if return_to_depot:
                            expected_loc_ids.append(last_loc)
                        last_loc = finish_at
                    expected_loc_ids.append(last_loc)

                if not expected_loc_ids:
                    is_planned = False
                    break

                solver_loc_ids = []
                for r in route['route']:
                    if 'id' in r['node']['value']:
                        solver_loc_ids.append(r['node']['value']['id'])

                if len(expected_loc_ids) < len(solver_loc_ids):
                    is_planned = False
                    break

                numLocs = len(solver_loc_ids)
                if solver_loc_ids != expected_loc_ids[:numLocs]:
                    is_planned = False
                    break

                if numLocs < len(expected_loc_ids):
                    del expected_loc_ids[:numLocs - 1]
                else:
                    expected_loc_ids = []
            if expected_loc_ids:
                is_planned = False
            vehicle['is_planned'] = is_planned

    def check_vehicle_visited_locations(self):
        for vehicle_id, routes in self.get_vehicle_routes().items():
            vehicle = self.vehicles[vehicle_id]
            first_used_shift_id = routes[0].get('shift', {}).get('id')

            checked_shifts = set()
            expected_loc_ids = []
            expected_times = []
            prev_route = routes[0]
            first_location_id = routes[0]["route"][0]['node']['value']['id']
            for route in routes:
                shift_id = route.get('shift', {}).get('id')

                if shift_id not in checked_shifts:
                    check(
                        not expected_loc_ids,
                        "Mismatch between expected visited locations and route for {}:\n"
                        "expected location ids: {}\ngot from solver: {}",
                        route_title(prev_route), expected_loc_ids, []
                    )
                    checked_shifts.add(shift_id)
                    visited = self.locations.vehicle_visited_locations(vehicle, shift_id)
                    expected_loc_ids = [loc["id"] for loc in visited]
                    expected_times = [loc.get('time') for loc in visited]
                    if not expected_loc_ids:
                        continue
                    start_info = self.locations.vehicle_start(vehicle, first_location_id, shift_id, shift_id == first_used_shift_id, self.options)
                    if all([time is None for time in expected_times]) or shift_id != first_used_shift_id:
                        first_loc = self.locations[expected_loc_ids[0]]
                        if not first_loc['type'] in ['depot', 'garage']:
                            expected_loc_ids[0:0] = [start_info.location['id']]  # garage or depot
                            expected_times[0:0] = [None]
                        if vehicle.get('start_at') is not None and vehicle.get('visit_depot_at_start', True) and shift_id == first_used_shift_id:
                            # started from garage but still need to go to the depot before orders
                            if len(expected_loc_ids) >= 2:
                                second_loc = self.locations[expected_loc_ids[0]]
                                if second_loc['id'] == self.locations.first_vehicle_depot(vehicle)['id']:
                                    continue
                            expected_loc_ids[1:1] = [self.locations.first_vehicle_depot(vehicle)['id']]
                            expected_times[1:1] = [None]

                if not expected_loc_ids:
                    continue

                for node in prev_route['route']:
                    node['node']['is_visited'] = True

                solver_loc_ids = []
                for r in route['route']:
                    if 'id' in r['node']['value']:
                        solver_loc_ids.append(r['node']['value']['id'])

                numLocs = min(len(expected_loc_ids), len(solver_loc_ids))
                check(
                    solver_loc_ids[:numLocs] == expected_loc_ids[:numLocs],
                    "Mismatch between expected visited locations and route for {}:\n"
                    "expected location ids: {}\ngot from solver: {}",
                    route_title(route), expected_loc_ids, solver_loc_ids
                )

                min_time = float('-inf')
                if shift_id is not None:
                    shift = vehicle['shifts'][shift_id]
                    if shift.get('hard_window', False):
                        min_time, _ = parse_interval_sec(shift['time_window'], self.options)

                for i in range(numLocs):
                    route['route'][i]['node']['is_visited'] = True
                    if expected_times[i] is not None:
                        point = route['route'][i]
                        node = point['node']
                        loc = self.locations[node['value']['id']]
                        visit_time = parse_time(expected_times[i], self.options).total_seconds() \
                            - node['value'].get('service_duration_s', 0) * vehicle.get('service_duration_multiplier', 1) \
                            - node['value'].get('shared_service_duration_s', 0) * vehicle.get('shared_service_duration_multiplier', 1)
                        visit_time = max(visit_time, min_time)
                        assert abs(visit_time - point['arrival_time_s']) < TIME_THRESHOLD_S, \
                            'Visited location {}, arrival time is {}, but visit time is {}' .format(
                                loc['id'], point['arrival_time_s'], visit_time)
                if numLocs < len(expected_loc_ids):
                    del expected_loc_ids[:numLocs - 1]
                else:
                    expected_loc_ids = []
                prev_route = route
            check(
                not expected_loc_ids,
                "Mismatch between expected visited locations and route for {}:\n"
                "expected location ids: {}\ngot from solver: {}",
                route_title(prev_route), expected_loc_ids, []
            )

    def get_vehicle_routes(self):
        vehicle_runs = {}

        for route in self.routes:
            vehicle_runs.setdefault(route['vehicle_id'], []).append(route)

        # sort routes of the same vehicle by run index
        return dict((vid, list(sorted(runs, key=(lambda run: run['run_number'])))) for vid, runs in vehicle_runs.items())

    def check_start_end_locations(self):
        """
        Check that all routes start and end at an expected location.
        It can be depot, garage or even an order (via visited_locations).
        """

        for vehicle_id, routes in self.get_vehicle_routes().items():
            vehicle = self.vehicles[vehicle_id]
            first_depot = self.locations.first_vehicle_depot(vehicle)
            last_depot = self.locations.last_vehicle_depot(vehicle)

            first_route = routes[0]
            shift_id = first_route.get('shift', {}).get('id')
            first_location_id = first_route["route"][0]['node']['value']["id"]
            start = self.locations.vehicle_start(vehicle, first_location_id, shift_id, True, self.options)
            route_start = first_route["route"][0]
            first_loc_id = route_start["node"]["value"]["id"]

            check(
                first_loc_id == start.location["id"],
                "Invalid route start for vehicle id: {}; expected loc id: {}; solver loc id: {}",
                vehicle_id, start.location["id"], first_loc_id
            )

            arrival_time = float(start_service_time(route_start))

            if (first_depot is not None and not first_depot.get("flexible_start_time")) or start.hard_window:
                check(
                    arrival_time >= start.time - self.time_accuracy_s,
                    "Departure from the starting location is less than expected: {}\nexpected: {}, departure: {}",
                    route_title(first_route),
                    seconds_to_time(start.time),
                    seconds_to_time(arrival_time)
                )

            last_route = routes[-1]
            return_to_depot = vehicle['return_to_depot']
            finish_at = vehicle.get('finish_at')
            min_nodes_count = 1 + (1 if return_to_depot else 0) + (1 if finish_at is not None else 0)

            check(len(last_route) >= min_nodes_count,
                  f"Route with return_to_depot={return_to_depot} and finish_at={finish_at} has less than {min_nodes_count} nodes")

            if finish_at is not None:
                last_node = last_route["route"][-1]
                last_loc = last_node["node"]["value"]
                check(
                    last_loc["id"] == finish_at,
                    f"Route doesn't end in the location specified in `finish_at` field of the vehicle {vehicle_id}"
                )
                check(
                    last_loc["type"] == "garage",
                    f"Route doesn't end in a location of type 'garage': vehicle id {vehicle_id}."
                )

            if return_to_depot:
                node = last_route["route"][-1 if finish_at is None else -2]

                check(
                    node["node"]["value"]["id"] == last_depot["id"],
                    f"Route doesn't end in the depot: {vehicle_id}"
                )

                if last_depot['hard_window']:
                    depot_arrival = int(node["arrival_time_s"])
                    check(
                        depot_arrival <= last_depot["time_end"] + self.time_accuracy_s,
                        f"Arrival to the depot is out of depot's time window: {vehicle_id}"
                    )

    def check_parkings(self):
        for route in self.routes:
            vehicle_id = route['vehicle_id']

            is_walking = False
            parked_at = None
            has_parking_source = False
            for loc in route['route']:
                if loc['node']['type'] == 'depot':
                    assert not is_walking, f'Vehicle {vehicle_id} ends a run in walking mode'
                elif loc['node']['type'] == 'location':
                    id = loc['node']['value']['id']
                    subtype = loc['node']['value']['type']
                    if subtype == 'parking':
                        if loc['node']['value']['parking_type'] == 'vehicle':
                            parking_type = loc['node']['value']['parking_mode']
                            if parking_type == 'ParkingBegin':
                                assert not is_walking, f'Vehicle {vehicle_id} has a start parking inside a walking part'
                                is_walking = True
                                parked_at = id
                                has_parking_source = False
                            elif parking_type == 'ParkingRefill':
                                assert is_walking, f'Vehicle {vehicle_id} has a middle parking outside a walking part'
                                assert parked_at == id, f'Vehicle {vehicle_id} has inconsistent parking ids'
                            elif parking_type == 'ParkingEnd':
                                assert is_walking, f'Vehicle {vehicle_id} has a middle parking outside a walking part'
                                assert parked_at == id, f'Vehicle {vehicle_id} has inconsistent parking ids'
                                assert has_parking_source, f'Vehicle {vehicle_id} has a walking part without parking source'
                                is_walking = False
                            else:
                                raise Exception(f'Vehicle {vehicle_id} has invalid parking type {parking_type}')
                    else:
                        if is_walking and id == parked_at:
                            has_parking_source = True

    def check_depot_location(self, route_node, vehicle, depot, position):
        check(
            route_node.get("is_visited", False) or vehicle["is_planned"] or
            route_node["value"]["id"] == depot["id"],
            f"Vehicle id {vehicle['id']}: route doesn't {position} in the depot id {depot['id']}."
        )

    def check_compatibility(self):
        for route in self.routes:
            vehicle_id = route['vehicle_id']
            vehicle = self.vehicles[vehicle_id]
            vehicle_tags = set(vehicle.get("tags", []))
            vehicle_excluded_tags = set(vehicle.get("excluded_tags", []))
            for loc in self.iter_route_locations(route['route'], 'location'):
                required_tags = set(loc.get("required_tags", []))
                missed_tags = list(required_tags - set(match_tags(required_tags, vehicle_tags)))
                if missed_tags:
                    raise Exception("Location id {} requires tags: {}, but vehicle id {} doesn't have them".format(
                        loc["id"], str(missed_tags), vehicle_id))
                forbidden_tags = match_tags(required_tags, vehicle_excluded_tags)
                if forbidden_tags:
                    raise Exception("Location id {} requires tags: {}, but vehicle id {} excludes them".format(
                        loc["id"], str(forbidden_tags), vehicle_id))
                if not fits_by_volume(vehicle, loc):
                    raise Exception("Location id {} has inappropriate container for vehicle id {}".format(
                        loc["id"], vehicle_id))

    def find_incompatibilities(self, locations, name, incompatibility_handler):
        data = set()
        for loc in locations:
            data |= set(loc.get(name + "s", []))
        return incompatibility_handler.find_incompatible(data)

    def check_incompatibility(self, name, incompatibility_handler, vehicles_incompatibility_handlers={}):
        if incompatibility_handler.size() == 0 and not vehicles_incompatibility_handlers:
            return

        given_incompatibilities = {}
        for vid, vehicle in self.vehicles.items():
            loc_ids = [
                node['id'] for node in
                vehicle.get('planned_route', {}).get('locations', []) +
                vehicle.get('visited_locations', [])
            ]
            given_incompatibilities[vid] = self.find_incompatibilities(
                [self.locations[loc_id] for loc_id in loc_ids], name,
                vehicles_incompatibility_handlers.get(vid, incompatibility_handler))

        for route in self.routes:
            type_pairs = self.find_incompatibilities(
                self.iter_route_locations(route['route'], 'location'), name,
                vehicles_incompatibility_handlers.get(route['vehicle_id'], incompatibility_handler))
            type_pairs -= given_incompatibilities[route['vehicle_id']]
            if type_pairs:
                raise Exception("Vehicle id {} includes incompatible {}: {}".format(
                    route['vehicle_id'], name, str(sorted(type_pairs))))

    def route_shipment_size(self, route, capacity_key, type_filter):
        total = 0.
        for elem in route['route']:
            node = elem['node']
            if node['type'] == 'location':
                loc = self.locations[node["value"]["id"]]
                # TODO: кажется тут должна быть более сложная логика и мы не учитываем все возможные варианты локаций сейчас
                if loc['type'] == type_filter and not loc['pud'] and 'delivery_to_any' not in loc:
                    total += loc['shipment_size'][capacity_key]
        return total

    def depot_service_duration(self, route, start):
        vehicle = self.vehicles[route["vehicle_id"]]
        if start:
            depot = self.locations.first_vehicle_depot(vehicle)
            total = depot['service_duration_s'] + depot['preliminary_service_duration_s'] + vehicle.get('depot_extra_service_duration_s', 0)
        else:
            depot = self.locations.last_vehicle_depot(vehicle)
            total = depot['finish_service_duration_s']
        total *= vehicle.get('service_duration_multiplier', 1.0)
        type_filter = 'delivery' if start else 'pickup'
        pickups = []
        for loc in self.iter_orders_and_dropoffs(route['route']):
            if type_filter == 'pickup':
                if loc['type'] == 'pickup' and 'delivery_to_any' in loc:
                    pickups.append(loc)
                if loc['type'] == 'drop_off':
                    pickups = [pickup for pickup in pickups if loc['id'] not in pickup['delivery_to_any']]
            if loc['type'] == type_filter and not loc['pud'] and 'delivery_to_any' not in loc:
                total += loc['depot_duration_s']
        for pickup in pickups:
            if depot['id'] in pickup['delivery_to_any']:
                total += pickup['depot_duration_s']
        return total

    def get_transport_work_cost(self, location, cur_distance, pickup_distance):
        return (cur_distance - pickup_distance) * float(location["shipment_size"]["weight_kg"])

    def drop_off_unload_duration(self, drop_off_id, undelivered_pickups, cur_distance, pickup_distances):
        total = 0
        transport_work_cost = 0
        filtered_pickups = []
        for loc in undelivered_pickups:
            if 'delivery_to_any' in loc and drop_off_id in loc['delivery_to_any']:
                total += loc['depot_duration_s']
                transport_work_cost += self.get_transport_work_cost(loc, cur_distance, pickup_distances[loc["id"]])
            else:
                filtered_pickups.append(loc)
        undelivered_pickups[:] = filtered_pickups
        return total, transport_work_cost

    def compute_depot_load_start_time(self):
        start = min(depot["time_start"] for depot in self.locations.iter_depots())
        if not self.locations.all_depots_have_hard_window() and \
           self.locations.some_depots_have_flexible_start_time():
            for loc in self.locations.values():
                if loc["type"] != "depot" or not loc.get("hard_window") and loc.get('flexible_start_time'):
                    for interval in loc["time_intervals"]:
                        start = min(start, interval["start"] - 2 * HOUR_S)
        if self.locations.some_depots_have_flexible_start_time_and_soft_window():
            start -= DAY_S
        return start

    def check_depot_service_duration_and_throughput(self):
        start_time = self.compute_depot_load_start_time()
        depot_loads = dict(
            (depot_id, DiscreteDepotLoad(start_time))
            for depot_id in self.locations.depot_ids
        )

        for route_idx, route in enumerate(self.routes, 1):
            for start in [True, False]:
                depot_node = route["route"][0 if start else -1]
                if depot_node["node"]["type"] != "depot":
                    continue

                service_time_s = self.depot_service_duration(route, start)
                depot_loc = depot_node["node"]["value"]
                solver_service_time_s = depot_loc["service_duration_s"]
                if depot_node['node'].get('is_visited', False):
                    service_time_s = solver_service_time_s
                assert abs(service_time_s - solver_service_time_s) < TIME_THRESHOLD_S, \
                    "Route #%d, wrong %s depot service duration: %.1f, while expected: %.1f" % \
                    (route_idx, 'starting' if start else 'ending', solver_service_time_s, service_time_s)

                depot = self.locations[depot_loc["id"]]
                throughput = depot.get("throughput", {})
                possible_limit_types = {
                    "kg_per_hour": "weight_kg",
                    "units_per_hour": "units",
                    "vehicle_count": "vehicles"
                }
                found_limit_types = {}
                for type_name, capacity_key in possible_limit_types.items():
                    if type_name in throughput:
                        found_limit_types[type_name] = capacity_key
                assert len(found_limit_types) <= 1, \
                    "throughput limit is exclusive, either kg, units or vehicles should be used"

                if not found_limit_types:
                    continue
                type_name, capacity_key = found_limit_types.popitem()

                if capacity_key != "vehicles":
                    load_size = self.route_shipment_size(route, capacity_key, 'delivery' if start else 'pickup')
                    if service_time_s != 0 and load_size != 0:
                        loading_start = start_service_time(depot_node)
                        loading_end = loading_start + service_time_s
                        depot_loads[depot_loc['id']].addLoad(loading_start, loading_end, load_size)
                elif service_time_s != 0:
                    loading_start = start_service_time(depot_node)
                    loading_end = loading_start + service_time_s
                    depot_loads[depot_loc['id']].addVehicle(loading_start, loading_end)

        depot_penalty = sum(
            self.depot_throughput_penalty(depot, depot_loads[depot["id"]])
            for depot in self.locations.iter_depots()
        )

        solver_depot_penalty = self.solution["metrics"]["total_depot_penalty"] if "total_depot_penalty" in self.solution["metrics"] else 0.

        diff = abs(depot_penalty - solver_depot_penalty)
        if diff > 0:
            rel_diff = diff / max(abs(depot_penalty), abs(solver_depot_penalty))
            assert rel_diff <= DEPOT_PENALTY_ACCURACY_REL, \
                "Wrong depot penalty! Expected: {}; solver: {}; diff: {} {}%.".format(
                    depot_penalty, solver_depot_penalty, diff, '%.2f' % (100.0 * rel_diff))

        return solver_depot_penalty

    def depot_throughput_penalty(self, depot, depot_load):
        throughput = depot.get("throughput", {})
        possible_limit_types = {
            "kg_per_hour" : "kg",
            "units_per_hour": "unit",
            "vehicle_count": "vehicle"
        }
        found_limit_types = {}
        for type_name, penalty_name in possible_limit_types.items():
            if type_name in throughput:
                found_limit_types[type_name] = penalty_name
        assert len(found_limit_types) <= 1, \
            "throughput limit is exclusive, either kg, units or vehicles should be used"
        if not found_limit_types:
            return 0

        type_name, penalty_name = found_limit_types.popitem()
        if type(throughput[type_name]) is list:
            extra_load = self.depot_throughput_overload_timed(depot, depot_load, throughput[type_name])
        else:
            limit_per_h = throughput[type_name]
            assert limit_per_h >= 0
            extra_load = self.depot_throughput_overload_fixed(depot, depot_load, limit_per_h)

        penalty = depot['penalty']['throughput']
        return penalty['fixed'] + extra_load * penalty[penalty_name] if extra_load > 0 else 0

    def depot_throughput_overload_fixed(self, depot, depot_load, limit_per_h):
        extra_load = 0

        for load_per_h, _ in depot_load:
            if load_per_h > limit_per_h + 1e-6:
                p = (load_per_h - limit_per_h)
                extra_load += p

        extra_load *= depot_load.step_s / HOUR_S
        return extra_load

    def depot_throughput_overload_timed(self, depot, depot_load, timed_limits_json):
        extra_load = 0

        timed_limits = [(item["value"], _get_time_interval_from_time_window(item["time_window"], self.options))
                        for item in timed_limits_json]
        timed_limits.sort(key=lambda x : x[1]["end"])
        limit_index = 0
        for load_per_h, interval_load_start in depot_load:
            if interval_load_start >= timed_limits[limit_index][1]["end"] and limit_index + 1 < len(timed_limits):
                limit_index += 1
            limit_per_h = timed_limits[limit_index][0]
            if load_per_h > limit_per_h + 1e-6:
                p = (load_per_h - limit_per_h)
                extra_load += p
        extra_load *= depot_load.step_s / HOUR_S
        return extra_load

    def get_size(self, value, is_custom):
        return value.get('custom', {}) if is_custom else value

    def overload_reported(self, node_loc):
        unfeasible_reasons = node_loc.get('unfeasible_reasons', [])
        return any((reason["type"].startswith("OVERLOAD_") or
                    (reason["type"] == "OTHER" and
                     reason["text"] == "linked pickup is overloaded"))
                   for reason in unfeasible_reasons)

    def compute_initial_depot_loads(self, locations, is_custom, capacity_key, overload_allowed):
        initial_loads = collections.defaultdict(float)
        deliveries = []
        for pos, node_loc in reversed(list(enumerate(locations))):
            loc = self.locations[node_loc["id"]]
            if (loc["type"] == "delivery" and not loc["pud"] and
                    (node_loc["type"] != 'parking'or node_loc["parking_type"] != "vehicle")):
                deliveries.append(node_loc)

            if loc["type"] == "depot" or pos == 0:
                rest_deliveries = []
                for delivery in deliveries:
                    if pos > 0 and "depot_id" in delivery and loc["id"] != delivery["depot_id"]:
                        rest_deliveries.append(delivery)
                        continue

                    shipment_size = self.get_size(delivery['shipment_size'], is_custom)
                    size = shipment_size.get(capacity_key, 0)
                    if overload_allowed or not self.overload_reported(delivery):
                        initial_loads[pos] += size
                deliveries = rest_deliveries
        return initial_loads

    def check_vehicle_capacity(self, route, vehicle, is_custom, capacity_key, limit_key,
                               threshold, overload_allowed, dropped_orders_load=0):
        capacities = self.get_size(vehicle['capacity'], is_custom)
        limit_perc = 100.0 if is_custom else capacities.get('limits', {}).get(limit_key, 100.0)
        capacity_coeff = limit_perc / 100.0
        capacity = capacities[capacity_key]
        if 'trailer' in vehicle:
            capacity += self.get_size(vehicle['trailer'].get('capacity', {}), is_custom).get(capacity_key, 0)
        real_capacity = capacity * capacity_coeff
        locations = list(self.iter_route_locations(route['route'], ['location', 'depot'], nodes=True))
        crossdocks = list(self.iter_crossdocks(route['route']))
        location_ids = '[' + ', '.join([str(loc['id']) for loc in (locations + crossdocks)]) + ']'

        def check_shipment_size(size, loc):
            check(size <= real_capacity + threshold,
                  "Vehicle id #{} cannot load loc id {}, total shipment size: {}, max {}: {}{}; route: {}",
                  vehicle['id'], loc['id'], size, capacity_key, capacity,
                  '' if capacity_coeff == 1.0 else ' * %g = %g' % (capacity_coeff, real_capacity),
                  location_ids)

        initial_load = dropped_orders_load
        initial_depot_loads = self.compute_initial_depot_loads(
            locations, is_custom, capacity_key, overload_allowed)

        for xdock_loc in crossdocks:
            for loc_id in xdock_loc['delivered_orders']:
                loc = self.locations[loc_id]
                if loc['type'] == 'delivery' and not loc['pud']:
                    shipment_size = self.get_size(loc['shipment_size'], is_custom)
                    size = shipment_size.get(capacity_key, 0)
                    if overload_allowed:
                        initial_load += size
                    else:
                        if not self.overload_reported(loc):
                            initial_load += size
                            check_shipment_size(initial_load, loc)

        cur_load = initial_load
        max_load = initial_load
        total_load = initial_load
        saved_max_load = initial_load

        undelivered_pickups = []
        banned_deliveries = {}
        for pos, loc in enumerate(locations):
            if pos in initial_depot_loads:
                initial_load += initial_depot_loads[pos]
                total_load += initial_depot_loads[pos]
                cur_load += initial_depot_loads[pos]
                saved_max_load = max(saved_max_load, max_load)
                max_load = cur_load

            if not overload_allowed and self.overload_reported(loc):
                if loc.get('delivery_to'):
                    banned_deliveries[loc['delivery_to']] = loc['id']
                continue

            loc_type = self.locations[loc["id"]]["type"]
            if loc_type in ["delivery", "pickup"]:
                shipment_size = self.get_size(loc['shipment_size'], is_custom)
                size = shipment_size.get(capacity_key, 0)
            if loc_type == 'delivery':
                assert loc['id'] not in banned_deliveries, \
                    "Pickup with loc id {} is marked as unfeasible due to overload, " \
                    "but its coupled delivery (id: {}) is not".format(banned_deliveries[loc['id']], loc['id'])
                cur_load -= size
            elif loc_type == 'pickup':
                cur_load += size
                total_load += size
                max_load = max(max_load, cur_load)
                if not overload_allowed:
                    check_shipment_size(cur_load, loc)
                if loc.get('delivery_to_any'):
                    undelivered_pickups.append(loc)
                elif not loc.get('delivery_to'):
                    undelivered_pickups.append(loc)
            elif loc_type == 'drop_off':
                rest_undelivered_pickups = []
                for pickup in undelivered_pickups:
                    if 'delivery_to_any' in pickup and loc['id'] in pickup['delivery_to_any']:
                        cur_load -= self.get_size(pickup['shipment_size'], is_custom).get(capacity_key, 0)
                    else:
                        rest_undelivered_pickups.append(pickup)
                undelivered_pickups = rest_undelivered_pickups
            elif loc_type == 'depot':
                rest_undelivered_pickups = []
                for pickup in undelivered_pickups:
                    if 'delivery_to_any' not in pickup and ('depot_id' not in pickup or loc['id'] == pickup['depot_id']):
                        cur_load -= self.get_size(pickup['shipment_size'], is_custom).get(capacity_key, 0)
                    elif 'delivery_to_any' in pickup and (loc['id'] in pickup['delivery_to_any']):
                        cur_load -= self.get_size(pickup['shipment_size'], is_custom).get(capacity_key, 0)
                    else:
                        rest_undelivered_pickups.append(pickup)
                undelivered_pickups = rest_undelivered_pickups
            elif loc_type == 'garage':
                pass
            elif loc_type == 'anchor':
                pass
            elif loc_type == 'parking':
                pass
            elif loc_type == 'crossdock':
                pass
            else:
                raise Exception("Unsupported location type: {}".format(loc_type))

        logging.debug("Checking %s load: locations=%d, initial=%g, max=%g" % (capacity_key, len(locations), initial_load, max_load))

        return max(saved_max_load, max_load), total_load

    def check_vehicle_utilization(self, route, vehicle, overload_allowed):
        for custom_type in vehicle['capacity'].get('custom', {}):
            self.check_vehicle_capacity(route, vehicle, True, custom_type, None, CUSTOM_THRESHOLD, overload_allowed)
        return [
            self.check_vehicle_capacity(
                route, vehicle, False, 'weight_kg', 'weight_perc',
                WEIGHT_THRESHOLD_KG, overload_allowed, route["metrics"]["dropped_orders_weight_kg"]),
            self.check_vehicle_capacity(
                route, vehicle, False, 'units', 'units_perc',
                UNITS_THRESHOLD, overload_allowed, route["metrics"]["dropped_orders_volume_m3"]),
            self.check_vehicle_capacity(
                route, vehicle, False, 'volume_cbm', 'volume_perc',
                VOLUME_THRESHOLD_CBM, overload_allowed, route["metrics"]["dropped_orders_units"])
        ]

    def check_vehicle_load(self, route, vehicle, metrics):
        # first check that the vehicle is not overloaded or solver reported an unfeasible reason of type overload
        self.check_vehicle_utilization(route, vehicle, False)

        # utilization should be computed taking into account all the orders even if they overload the vehicle
        (
            (metrics.utilization_weight_kg, metrics.total_weight_kg),
            (metrics.utilization_units, metrics.total_units),
            (metrics.utilization_volume_m3, metrics.total_volume_m3)
        ) = self.check_vehicle_utilization(route, vehicle, True)

        metrics.utilization_weight_perc = percent(metrics.utilization_weight_kg, vehicle['capacity']['weight_kg'])
        metrics.utilization_units_perc = percent(metrics.utilization_units, vehicle['capacity']['units'])
        metrics.utilization_volume_perc = percent(metrics.utilization_volume_m3, vehicle['capacity']['volume_cbm'])

    def get_custom_cost(self, cost_expr, metrics, runs):
        import _solver as solver
        return solver.compute_custom_cost(
            cost_expr,
            runs=runs,
            locations=metrics.orders_count,
            duration_h=metrics.duration_s / HOUR_S,
            distance_km=metrics.transit_distance_m / KM,
            walking_transit_duration_h=metrics.walking_transit_duration_s / HOUR_S,
            driving_transit_duration_h=metrics.vehicle_transit_duration_s / HOUR_S,
            walking_distance_km=metrics.walking_transit_distance_m / KM,
            driving_distance_km=metrics.vehicle_transit_distance_m / KM,
            walking_stops=metrics.walking_stops,
            driving_stops=metrics.vehicle_stops,
            transport_work_tonne_km=metrics.transport_work_cost / (KM * TONNE),
            unique_stops=metrics.unique_stops,
            trailer_used=1.0 if metrics.use_trailer else 0.0,
            trailer_duration_h=metrics.trailer_transit_duration_s / HOUR_S,
            trailer_distance_km=metrics.trailer_transit_distance_m / KM,
            total_custom_value=metrics.total_custom_value
        )

    def check_route(self, route, work_start_time, last_vehicle_run):
        vehicle = self.vehicles[route['vehicle_id']]
        shift = get_route_shift(vehicle, route)
        vehicle_orders_total = self.count_vehicle_orders(vehicle)
        nodes = get_route_nodes(route)
        metrics = Metrics()
        metrics.orders_count = 0
        title = route_title(route)
        vehicle_tags = set(vehicle.get("tags", []))
        vehicle_excluded_tags = set(vehicle.get("excluded_tags", []))

        self.check_vehicle_load(route, vehicle, metrics)

        def is_zero_service_duration(point, is_starting):
            return point.get("shift_location") if is_starting \
                else (last_vehicle_run and point.get("shift_location"))

        metrics.time_s = nodes_arrival_time(nodes)
        metrics.work_start_time = work_start_time
        last_stop_time = work_start_time
        prev_arrival_time = None
        prev_work_break = False
        prev_shared_service_duration = 0
        prev_service_duration = 0
        prev_clients_service_duration = 0
        cur_client_service_duration = 0
        prev_client_id = None
        prev_loc_point = None
        prev_stop_point = None
        undelivered_pickups = []  # non-dropoffed (yet) delivery_to_any and pickups to depot
        pickup_distances = {}
        coupled_pickups = {}
        last_depot_distance = 0
        is_start_location = True
        deadline_locs = []
        last_group_id = None
        dependent_group_sizes = collections.defaultdict(int)
        unique_stops = set()
        total_pickup_weight = None
        total_delivery_weight = None
        metrics.total_custom_value = 0

        trailer_used = False
        trailer_decoupled = False
        metrics.use_trailer = False
        walking_active = False

        def failed_time_window(kind, node_idx, duration_s, penalties):
            penalty = failed_time_penalty(duration_s, penalties, kind)
            metrics.failed_time_window_duration_s += duration_s
            metrics.failed_time_window_penalty += penalty
            logging.info(
                '{}, node #{}: {} {}; total_seconds: {}; penalty: +{}, total_penalty: {}'.format(
                    title, node_idx, seconds_to_time(duration_s), kind,
                    metrics.failed_time_window_duration_s,
                    penalty, metrics.failed_time_window_penalty
                )
            )

        for node_idx, point in enumerate(nodes, 1):
            node = point['node']
            route_element = node["value"]
            work_break = node['value'] if node['type'] == 'break' else None
            is_loc = node['type'] in ['location', 'depot']
            loc_id = route_element["id"] if is_loc else None
            location = self.locations[loc_id] if is_loc else None
            is_shift_node = point.get('shift_location')

            should_add_trailer_transit = trailer_used and not trailer_decoupled

            if is_loc:
                if node['type'] == 'depot':
                    check(not trailer_decoupled, "Return to the depot without the trailer.")
                    trailer_used = route_element.get('trailer_used', False)
                    metrics.use_trailer = metrics.use_trailer or trailer_used
                else:
                    metrics.total_custom_value += route_element.get("custom_value", 0.)
                    check(
                        trailer_used == route_element.get('trailer_used', False),
                        "Wrong value of 'trailer_used' field at node {}: {}, but {} expected.".format(
                            loc_id,
                            route_element.get('trailer_used', False),
                            trailer_used
                        )
                    )
                    if location["type"] == 'parking' or (location["type"] == 'anchor' and route_element['anchor_mode'] != 'Rolling'):
                        trailer_decoupled = route_element['trailer_decoupled']
                    else:
                        check(
                            trailer_decoupled == route_element.get('trailer_decoupled', False),
                            "Wrong value of 'trailer_decoupled' field at node {}: {}, but {} expected.".format(
                                loc_id,
                                route_element.get('trailer_decoupled', False),
                                trailer_decoupled
                            )
                        )

            if point.get("multi_order") and prev_arrival_time is not None:
                metrics.time_s = point["arrival_time_s"]

            if work_break:
                if prev_arrival_time is not None and prev_arrival_time == point['arrival_time_s']:
                    metrics.time_s = prev_arrival_time
                    if not prev_work_break:
                        metrics.rest_duration_s += work_break["rest_duration_s"]
                else:
                    metrics.rest_duration_s += point["departure_time_s"] - \
                        point["arrival_time_s"] - point["waiting_duration_s"]

            if is_start_location and is_shift_node:
                metrics.work_start_time = start_service_time(point)
                last_stop_time = metrics.work_start_time

            if prev_loc_point:
                if is_start_location and not prev_loc_point.get("shift_location"):
                    is_start_location = False

                duration_checker = solver_duration if work_break else self.duration_callback
                checker_transit_duration, checker_transit_distance = duration_checker(
                    prev_loc_point, point, metrics.time_s, self.distances)

                if duration_checker is not solver_duration:
                    checker_transit_duration *= vehicle.get('travel_time_multiplier', 1.0)

                    check(abs(checker_transit_duration - point["transit_duration_s"]) <= self.time_accuracy_s,
                          "Wrong transit duration is used by solver: {}, however expected {}",
                          point["transit_duration_s"], checker_transit_duration)

                if is_loc and location["type"] in ["pickup", "delivery", "drop_off"] and \
                   (route_element.get('type') != 'parking' or route_element.get('parking_type') != 'vehicle') and \
                   (prev_stop_point is None or not points_equal(point, prev_stop_point)):
                    metrics.stops += 1
                    if walking_active:
                        metrics.walking_stops += 1
                    else:
                        metrics.vehicle_stops += 1
                    pos = point["node"]["value"]["point"]
                    unique_stops.add((int(pos["lat"] / COORDINATES_THRESHOLD), int(pos["lon"] / COORDINATES_THRESHOLD)))

                metrics.time_s += checker_transit_duration
                metrics.transit_duration_s += checker_transit_duration
                metrics.transit_distance_m += checker_transit_distance

                if should_add_trailer_transit:
                    metrics.trailer_transit_duration_s += checker_transit_duration
                    metrics.trailer_transit_distance_m += checker_transit_distance if not walking_active else 0.
                if walking_active:
                    metrics.walking_transit_duration_s += checker_transit_duration
                    metrics.walking_transit_distance_m += checker_transit_distance
                else:
                    metrics.vehicle_transit_duration_s += checker_transit_duration
                    metrics.vehicle_transit_distance_m += checker_transit_distance

            if node.get('is_visited'):
                metrics.time_s = point['arrival_time_s']

            metrics.time_s += point["waiting_duration_s"]
            metrics.waiting_duration_s += point["waiting_duration_s"]

            if not point.get("multi_order"):
                prev_arrival_time = metrics.time_s
                metrics.service_duration_s += prev_service_duration + prev_shared_service_duration + \
                    prev_clients_service_duration + cur_client_service_duration

                if total_pickup_weight is not None:
                    total_weight = max(total_pickup_weight, total_delivery_weight)
                    if total_weight < vehicle.get("min_stop_weight", 0):
                        vehicle_penalty = vehicle.get("penalty", {})
                        if "min_stop_weight" in vehicle_penalty:
                            min_stop_weight_fixed = vehicle_penalty["min_stop_weight"]["fixed"]
                            min_stop_weight_per_kg = vehicle_penalty["min_stop_weight"]["kg"]
                        else:
                            min_stop_weight_fixed = DEFAULT_MIN_WEIGHT_PENALTY_FIXED
                            min_stop_weight_per_kg = DEFAULT_MIN_WEIGHT_PENALTY_PER_KG

                        metrics.min_stop_weight_penalty += \
                            min_stop_weight_fixed + \
                            min_stop_weight_per_kg * (vehicle.get("min_stop_weight", 0) - total_weight)

                if location and location["type"] in ["pickup", "delivery"]:
                    total_pickup_weight = 0
                    total_delivery_weight = 0

            if work_break:
                begin, end = work_break_time_window(work_break, metrics.work_start_time, work_start_time, last_stop_time)
                init_oot_penalty(work_break)
            elif route_element.get('type') == 'parking' and route_element.get('parking_type') == 'vehicle':
                begin = 0
                end = 1e400
            else:
                time_window = shift['time_window'] if is_shift_node else node["used_time_window"]
                begin, end = parse_interval_sec(time_window, self.options)

            penalty = work_break['penalty'] if work_break else \
                shift['penalty'] if is_shift_node else location["penalty"]

            fail_s = begin - metrics.time_s
            if fail_s > TIME_THRESHOLD_S:
                failed_time_window("early", node_idx, fail_s, penalty)

            fail_s = metrics.time_s - end
            if fail_s > TIME_THRESHOLD_S:
                failed_time_window("late", node_idx, fail_s, penalty)

            if is_loc and 'optional_tags' in location:
                for optional_tag in location['optional_tags']:
                    tag = optional_tag['tag']
                    value = optional_tag['value']
                    if match_tags({tag}, vehicle_tags):
                        metrics.optional_tags_cost -= value
                    if match_tags({tag}, vehicle_excluded_tags):
                        metrics.optional_tags_cost += value

            if 'delivery_deadline' in route_element:
                deadline_locs.append(route_element)

            if node['type'] == 'depot':
                for pickup in deadline_locs:
                    fail_s = metrics.time_s - parse_time(pickup['delivery_deadline'], self.options).total_seconds()
                    if fail_s > TIME_THRESHOLD_S:
                        penalty = failed_time_penalty(fail_s, pickup['penalty'], 'delivery_deadline')
                        metrics.failed_delivery_deadline_duration_s += fail_s
                        metrics.failed_delivery_deadline_penalty += penalty
                deadline_locs = []

                last_depot_distance = metrics.transit_distance_m
                for pickup in undelivered_pickups:
                    metrics.transport_work_cost += self.get_transport_work_cost(
                        pickup, last_depot_distance, pickup_distances[pickup["id"]])

            if is_loc and not is_shift_node:
                loc_weight = route_element.get("shipment_size", {}).get("weight_kg", 0)

                if location["type"] == "pickup":
                    total_pickup_weight += loc_weight
                    pickup_distances[loc_id] = metrics.transit_distance_m
                    if "delivery_to_any" in location:
                        undelivered_pickups.append(location)
                    elif "delivery_to" in location:
                        coupled_pickups[location["delivery_to"]] = loc_id
                    else:
                        undelivered_pickups.append(location)
                elif location["type"] == "delivery":
                    total_delivery_weight += loc_weight
                    pickup_distance = last_depot_distance
                    if loc_id in coupled_pickups:
                        pickup_distance = pickup_distances[coupled_pickups[loc_id]]
                    metrics.transport_work_cost += self.get_transport_work_cost(
                        location, metrics.transit_distance_m, pickup_distance)

            if work_break:
                # TODO check rest_duration_s
                metrics.time_s = point['departure_time_s']
                metrics.work_start_time = metrics.time_s
            else:
                if node["type"] != "depot" and not node.get('is_visited', False):
                    # depot service duration is checked in `check_depot_service_duration_and_throughput()`
                    if is_zero_service_duration(point, is_start_location):
                        loc_service_duration = loc_shared_service_duration = total_service_duration = loc_parking_service_duration = 0
                    else:
                        loc_service_duration = int(location.get("service_duration_s", 0)) * \
                            vehicle.get("service_duration_multiplier", 1)

                        if node["value"]["type"] == "drop_off":
                            unload_duration, transport_work_cost = self.drop_off_unload_duration(
                                loc_id, undelivered_pickups, metrics.transit_distance_m, pickup_distances)
                            loc_service_duration += unload_duration
                            metrics.transport_work_cost += transport_work_cost

                        if node["value"]["type"] == "anchor":
                            if node["value"]["anchor_mode"] == "Decoupling":
                                loc_service_duration += vehicle["trailer"].get("decoupling_time_s", 0)
                            elif node["value"]["anchor_mode"] == "Coupling":
                                loc_service_duration += vehicle["trailer"].get("coupling_time_s", 0)
                            if node["value"]["anchor_mode"] == "Rolling":
                                loc_service_duration += vehicle["trailer"].get("rolling_time", {}).get("fixed_time_s", 0)

                        loc_parking_service_duration = 0
                        if node["value"]["routing_mode"] == "driving" or node["value"]["routing_mode"] == "truck":
                            loc_parking_service_duration += route_element["parking_service_duration_s"] * \
                                vehicle.get("service_duration_multiplier", 1)

                        if node["value"]["type"] == "crossdock" and "delivered_orders" in node["value"]:
                            for loc_id in node["value"]["delivered_orders"]:
                                loc_service_duration += self.locations[loc_id].get("crossdock_service_duration_s", 0)

                        loc_shared_service_duration = int(location.get("shared_service_duration_s", 0)) * \
                            vehicle.get("shared_service_duration_multiplier", 1)

                        total_service_duration = loc_service_duration + loc_parking_service_duration + \
                            route_element.get('added_shared_service_duration_s', 0) + route_element.get('service_waiting_duration_s', 0)

                    if route_element['type'] != 'parking':
                        check(abs(route_element["service_duration_s"] - loc_service_duration) < 1e-6,
                                "location id {}, wrong service_duration_s: {}, expected: {}",
                                loc_id, route_element["service_duration_s"], loc_service_duration)

                        check(abs(route_element.get("shared_service_duration_s", 0) - loc_shared_service_duration) < 1e-6,
                                "location id {}, wrong shared_service_duration_s: {}, expected: {}",
                                loc_id, route_element.get("shared_service_duration_s", 0), loc_shared_service_duration)

                        check(abs(total_service_duration - route_element['total_service_duration_s']) < 1e-6,
                                "location id {}, wrong total_service_duration_s: {}, expected: {}",
                                loc_id, route_element['total_service_duration_s'], total_service_duration)
                if point.get("multi_order"):
                    metrics.time_s = point['departure_time_s']
                else:
                    metrics.time_s += route_element.get('actual_total_service_duration_s',
                                                        route_element['total_service_duration_s'])

            last_stop_time = metrics.time_s

            location_group_id = self.location2group.get(loc_id)
            if is_loc:
                if location["type"] in OrderTypes and not is_shift_node:
                    metrics.orders_count += 1

                    if location_group_id is not None:
                        if location_group_id in metrics.location_groups:
                            if self.solid[location_group_id]:
                                check(location_group_id == last_group_id, "locations in solid location group {} are not consecutive", location_group_id)
                        else:
                            metrics.location_groups.add(location_group_id)
                        if self.dependent[location_group_id]:
                            dependent_group_sizes[location_group_id] += 1
                prev_loc_point = point
                if location["type"] != 'parking' and (route_element.get("type") != 'parking' or route_element.get("parking_type") != 'vehicle'):
                    prev_stop_point = point
                else:
                    prev_stop_point = None

            last_group_id = location_group_id
            loc_client_service_duration = node["value"].get("client_service_duration_s", 0)
            client_id = node["value"].get("client_id", "")
            if point.get("multi_order"):
                prev_service_duration += node["value"].get("service_duration_s", 0)
                prev_shared_service_duration = max(prev_shared_service_duration, node["value"].get("shared_service_duration_s", 0))
                if prev_client_id is not None and prev_client_id != client_id:
                    prev_clients_service_duration += cur_client_service_duration
                    cur_client_service_duration = loc_client_service_duration
                else:
                    cur_client_service_duration = max(cur_client_service_duration,
                                                      loc_client_service_duration)
            elif node["value"].get("type", "") != "parking":
                prev_service_duration = node["value"].get("service_duration_s", 0)
                prev_shared_service_duration = node["value"].get("shared_service_duration_s", 0)
                cur_client_service_duration = loc_client_service_duration
                prev_clients_service_duration = 0
            else:
                prev_service_duration = 0
                prev_shared_service_duration = 0
                cur_client_service_duration = 0
                prev_clients_service_duration = 0
            prev_work_break = work_break is not None
            prev_client_id = client_id

            if route_element.get("type") == 'parking' and route_element.get("parking_type") == 'vehicle':
                walking_active = not walking_active

        assert not deadline_locs, '{}, locations with delivery_deadline parameter are not delivered to the depot'.format(title)

        for location_group_id, size in dependent_group_sizes.items():
            check(size == self.location_group_size[location_group_id],
                  'some locations in dependent location group {} are on different routes or dropped', location_group_id)

        metrics.duration_s = metrics.time_s - nodes_arrival_time(nodes)
        metrics.unique_stops = len(unique_stops)
        metrics.service_duration_s += prev_service_duration + prev_shared_service_duration + \
            prev_clients_service_duration + cur_client_service_duration

        if shift is not None:
            if nodes[0].get('shift_location'):
                metrics.intershift_duration_s = nodes[0]["waiting_duration_s"]
                metrics.duration_s -= metrics.intershift_duration_s

            if 'balanced_group_id' in shift:
                metrics.balanced_groups.add_run(
                    shift['balanced_group_id'],
                    vehicle['id'],
                    shift['id'],
                    BalancedRun(metrics.duration_s, metrics.stops)
                )

            metrics.vehicle_shifts.add_stats(
                vehicle['id'], shift['id'], metrics.stops, metrics.duration_s, metrics.transit_distance_m)

        cost = vehicle['cost']
        fixed_cost_k = 1.0 * self.count_route_orders(route) / max(1, vehicle_orders_total)

        custom_cost = isinstance(cost, str)
        if custom_cost:
            metrics.transport_work_cost = 0
            metrics.custom_cost = self.get_custom_cost(cost, metrics, 1)
        else:
            metrics.fixed_cost = cost['fixed'] * fixed_cost_k
            metrics.runs_cost = cost['run']
            metrics.distance_cost = metrics.vehicle_transit_distance_m * cost['km'] / KM
            metrics.duration_cost = metrics.duration_s * cost['hour'] / HOUR_S
            metrics.locations_cost = metrics.orders_count * cost['location']
            metrics.transport_work_cost = metrics.transport_work_cost / (KM * TONNE) * cost['tonne_km']

            if metrics.use_trailer and ('cost' in vehicle['trailer']):
                trailer_cost = vehicle['trailer']['cost']
                metrics.fixed_cost += trailer_cost.get('fixed', 0) * fixed_cost_k
                metrics.distance_cost += metrics.trailer_transit_distance_m * trailer_cost.get('km', 0) / KM
                metrics.duration_cost += metrics.trailer_transit_duration_s * trailer_cost.get('hour', 0) / HOUR_S
            if 'walking_courier' in vehicle:
                walking_courier = vehicle['walking_courier']
                walking_cost = {}
                if 'cost' in walking_courier:
                    walking_cost = walking_courier['cost']
                metrics.distance_cost += metrics.walking_transit_distance_m * walking_cost.get('km', 0) / KM
                metrics.duration_cost += metrics.walking_transit_duration_s * walking_cost.get('hour', 0) / HOUR_S

        cost_km = DEFAULT_VEHICLE_PER_KM_COST if custom_cost else cost['km']
        cost_hour = DEFAULT_VEHICLE_PER_HOUR_COST if custom_cost else cost['hour']
        metrics.proximity_penalty = PROXIMITY_FACTOR_BOOST * self.options.get('proximity_factor', 0) * (
            route['metrics'].get('total_proximity_distance_m', 0) * cost_km / KM +
            route['metrics'].get('total_proximity_duration_s', 0) * cost_hour / HOUR_S
        )

        prefix = 'new_' if ('new_total_global_proximity_distance_m' in route['metrics']) else ''

        metrics.global_proximity_penalty = PROXIMITY_FACTOR_BOOST * self.options.get('global_proximity_factor', 0) * (
            route['metrics'].get(prefix + 'total_global_proximity_distance_m', 0) * cost_km / KM +
            route['metrics'].get(prefix + 'total_global_proximity_duration_s', 0) * cost_hour / HOUR_S
        )

        metrics.unfeasibility_penalty = route['metrics'].get('total_unfeasibility_penalty', 0)

        metrics.failed_time_window_duration_s += route['metrics'].get('failed_dropped_breaks_duration_s', 0)
        metrics.failed_time_window_penalty += route['metrics'].get('failed_dropped_breaks_penalty', 0)

        penalty = metrics.guaranteed_penalty()
        if penalty > 0:
            logging.info(
                'vehicle ' + str(vehicle['id']) + ', starting at ' +
                str(seconds_to_time(nodes_arrival_time(nodes))) +
                ": penalty=" + str(penalty))

        return metrics

    def compare_metrics_rel(self, metrics, title, values):
        rows = []

        for name, value in values:
            if ("new_" + name) in metrics:
                solver_value = metrics["new_" + name]
            else:
                solver_value = metrics.get(name)
            if solver_value is not None:
                if not tools.is_rel_close(value, solver_value, tools.ACCURACY_RELATIVE):
                    rows.append([name, value, solver_value, solver_value - value])
        if rows:
            header = ["Metric", "Checker", "Solver", "Diff"]
            raise Exception("\n" + title + ':\n' + tabulate.tabulate(rows, headers=header))

    def compare_metrics_abs_rel(self, solver_metrics, metrics, check_attrs, abs_accuracy, title):
        rows = []
        for attr, solver_attr in check_attrs:
            checked = getattr(metrics, attr)
            if ("new_" + solver_attr) in solver_metrics:
                solved = solver_metrics["new_" + solver_attr]
            else:
                solved = solver_metrics[solver_attr]
            if not tools.is_close(solved, checked, abs_accuracy, tools.ACCURACY_RELATIVE):
                rows.append([attr, checked, solved, solved - checked])
        if rows:
            header = ["Attr", "Checker", "Solver", "Diff"]
            raise Exception("\n" + title + ':\n' + tabulate.tabulate(rows, headers=header))

    def compare_general_metrics(self, solver_metrics, metrics, title):
        check_attrs = [
            ("stops", "total_stops"),
            ("unique_stops", "total_unique_stops"),
            ("duration_s", "total_duration_s"),
            ("service_duration_s", "total_service_duration_s"),
            ("rest_duration_s", "total_rest_duration_s"),
            ("transit_duration_s", "total_transit_duration_s"),
            ("transit_distance_m", "total_transit_distance_m"),
            ("failed_time_window_duration_s", "total_failed_time_window_duration_s"),
            ("failed_time_window_penalty", "total_failed_time_window_penalty"),
            ("failed_delivery_deadline_duration_s", "total_failed_delivery_deadline_duration_s"),
            ("failed_delivery_deadline_penalty", "total_failed_delivery_deadline_penalty"),
            ("proximity_penalty", "total_proximity_penalty"),
            ("global_proximity_penalty", "total_global_proximity_penalty"),
            ("optional_tags_cost", "total_optional_tags_cost"),
            ("fixed_cost", "total_fixed_cost"),
            ("runs_cost", "total_runs_cost"),
            ("transport_work_cost", "total_transport_work_cost"),
            ("locations_cost", "total_locations_cost"),
            ("distance_cost", "total_transit_distance_cost"),
            ("duration_cost", "total_duration_cost"),
            ("trailer_transit_duration_s", "total_trailer_transit_duration_s"),
            ("trailer_transit_distance_m", "total_trailer_transit_distance_m"),
        ]
        self.compare_metrics_abs_rel(
            solver_metrics, metrics, check_attrs, METRICS_GENERAL_THRESHOLD, title)
        self.compare_metrics_rel(
            solver_metrics, title, [('total_cost', metrics.total_cost())])

    def compare_single_run_metrics(self, route, solver_metrics, metrics):
        title = 'Vehicle id {}, run #{}'.format(route['vehicle_id'], route['run_number'])
        self.compare_general_metrics(solver_metrics, metrics, title)
        attrs = [
            'utilization_weight_kg',
            'utilization_units',
            'utilization_volume_m3',
            'utilization_weight_perc',
            'utilization_units_perc',
            'utilization_volume_perc',
            'total_weight_kg',
            'total_units',
            'total_volume_m3',
        ]
        check_attrs = [(attr, attr) for attr in attrs]
        self.compare_metrics_abs_rel(solver_metrics, metrics, check_attrs, METRICS_GENERAL_THRESHOLD, title)

    def compare_global_metrics(self, metrics):
        solver_metrics = self.solution["metrics"]

        self.compare_general_metrics(solver_metrics, metrics, "Global metrics")

        if 'new_total_penalty' in solver_metrics:
            expected_penalty = solver_metrics['new_total_penalty']
        else:
            expected_penalty = solver_metrics.get('total_penalty', 0)

        self.compare_metrics_rel(
            solver_metrics,
            "Global metrics",
            [
                ('overtime_duration_s', metrics.overtime_duration_s),
                ('overtime_shifts_count', metrics.overtime_shifts_count),
                ('overtime_penalty', metrics.overtime_penalty),
                ('total_stop_count_penalty', metrics.stop_count_penalty),
                ('total_mileage_penalty', metrics.mileage_penalty),
                ('total_min_stop_weight_penalty', metrics.min_stop_weight_penalty),
                ('balanced_group_penalty', metrics.balanced_group_penalty),
                ('total_depot_penalty', metrics.depot_penalty),
                ('total_drop_penalty', metrics.drop_penalty),
                ('drop_penalty_percentage', metrics.drop_penalty_percentage),
                ('max_drop_percentage_penalty', metrics.max_drop_percentage_penalty),
                ('total_unfeasibility_penalty', metrics.unfeasibility_penalty),
                ('total_guaranteed_penalty', metrics.guaranteed_penalty()),
                ('total_cost_with_penalty', metrics.total_cost() + expected_penalty),
                ('transit_time_penalty', metrics.transit_time_penalty)
            ]
        )

    def check_location_groups(self, route_metrics, total, title):
        def groups2str(groups):
            return ', '.join('#%d' % gid for gid in groups)

        if route_metrics.location_groups:
            logging.info(
                f'{title} location groups: ' +
                groups2str(route_metrics.location_groups) + '.'
            )
        extra_location_groups = total.location_groups & route_metrics.location_groups
        assert len(extra_location_groups) == 0, (
            "Location group(s) is(are) assigned to different vehicle runs: " +
            groups2str(extra_location_groups) + '.')

    def iter_vehicle_routes(self):
        routes = []
        vehicle_id = None
        processed_vehicle_ids = set()

        for route in self.routes:
            if route['vehicle_id'] != vehicle_id:
                if routes:
                    yield vehicle_id, routes
                routes = []
                vehicle_id = route['vehicle_id']
                assert vehicle_id not in processed_vehicle_ids, "Routes are not sorted by vehicles!"
                processed_vehicle_ids.add(vehicle_id)
            if routes:
                assert route_arrival_time(routes[-1]) <= route_arrival_time(route), \
                    "Vehicle runs are not sorted by start time!"
            routes.append(route)
        if routes:
            yield vehicle_id, routes

    def check_metrics(self, total):
        custom_cost = 0
        total_fixed_cost = 0
        for vehicle_id, routes in self.iter_vehicle_routes():
            vehicle_metrics = None
            use_trailer = False
            work_start_time = route_arrival_time(routes[0])
            for run_idx, route in enumerate(routes, 1):
                title = f'vehicle id {vehicle_id}, run #{run_idx}'
                route_metrics = self.check_route(route, work_start_time, run_idx == len(routes))
                if len(route["route"]) > 2:
                    self.compare_single_run_metrics(route, route['metrics'], route_metrics)
                self.check_location_groups(route_metrics, total, title)
                total += route_metrics
                if vehicle_metrics is None:
                    vehicle_metrics = route_metrics
                else:
                    vehicle_metrics += route_metrics
                work_start_time = route_metrics.work_start_time
                use_trailer |= route_metrics.use_trailer

            vehicle = self.vehicles[route['vehicle_id']]
            if isinstance(vehicle.get("cost", None), str):
                custom_cost += self.get_custom_cost(vehicle["cost"], vehicle_metrics, len(routes))

            if isinstance(vehicle["cost"], dict):
                total_fixed_cost += vehicle["cost"]["fixed"]
                if use_trailer and ("cost" in vehicle["trailer"]):
                    total_fixed_cost += vehicle["trailer"]["cost"].get('fixed', 0)

        total.custom_cost = custom_cost
        total.arrival_after_start_penalty = self.solution["metrics"]["arrival_after_start_penalty"]
        total.transit_time_penalty = self.solution["metrics"]["transit_time_penalty"]
        total.fixed_cost = total_fixed_cost

        for vehicle in self.vehicles.values():
            for shift in vehicle["shifts"].values():
                stats = total.vehicle_shifts.get_stats(vehicle['id'], shift['id'])

                lack_penalty = 0 \
                    if self.options.get("ignore_min_stops_for_unused", False) and stats.stops == 0 \
                    else stop_lack_penalty(shift, stats.stops)
                total.stop_count_penalty += lack_penalty + stop_excess_penalty(shift, stats.stops)
                total.mileage_penalty += mileage_penalty(stats.distance_m, shift)

                overtime = stats.duration_s - shift['max_duration_s']
                if overtime > 0:
                    total.overtime_duration_s += overtime
                    total.overtime_shifts_count += 1
                    total.overtime_penalty += failed_time_penalty(overtime, shift["penalty"], 'late')

        balanced_groups = self.vehicles.balanced_groups + total.balanced_groups
        total.balanced_group_penalty = balanced_groups.penalty(self.balanced_groups)

        self.compare_global_metrics(total)

    def count_route_orders(self, route):
        return (
            sum(1 for node in self.iter_orders(route['route'])) +
            sum(len(node['delivered_orders']) for node in self.iter_crossdocks(route['route']))
        )

    def count_vehicle_orders(self, vehicle):
        return sum(self.count_route_orders(route) for route in self.routes if route['vehicle_id'] == vehicle['id'])

    def check_depot_ready_time(self, route, route_idx, start_time, load_when_ready, waiting_duration_exp):
        orders = self.iter_orders(route['route'])
        cur_time = start_time
        if load_when_ready:
            orders = sorted(orders, key=lambda order: order.get('depot_ready_time', 0))
            waiting_duration = 0
        for loc in orders:
            if 'depot_ready_time' in loc:
                ready_time = parse_time(loc['depot_ready_time'], self.options).total_seconds()
                if load_when_ready:
                    if ready_time > cur_time:
                        waiting_duration += ready_time - cur_time
                        cur_time = ready_time
                    cur_time += loc.get('depot_duration_s', 0)
                else:
                    check(cur_time >= ready_time - TIME_THRESHOLD_S,
                          'Route #{}, service starts at {}, but location {} has ready_time at {}',
                          route_idx, seconds_to_time(start_time), loc['id'], seconds_to_time(ready_time))

        if load_when_ready and waiting_duration_exp:
            # In this case ready_time check is uninformative, so checking waiting duration instead
            check(waiting_duration == waiting_duration_exp,
                  'Route #{}, depot waiting duration is {}, but expected waiting duration is {}',
                  route_idx, waiting_duration, waiting_duration_exp)

    def check_depot_ready_times(self):
        load_when_ready = self.options.get('load_when_ready', False)
        for route_idx, route in enumerate(self.routes, 1):
            depot_node = route['route'][0]
            vehicle = self.vehicles[route['vehicle_id']]
            if depot_node['node']['type'] != 'depot':
                shift_id = route.get('shift', {}).get('id')
                if self.locations.vehicle_visited_shift_start(vehicle, shift_id) or not vehicle.get('visit_depot_at_start', True):
                    depot_node = None
                else:
                    depot_node = route['route'][1]

            waiting_duration = None
            if depot_node:
                depot = self.locations[depot_node['node']['value']['id']]
                preliminary_service_duration = depot['preliminary_service_duration_s'] * vehicle.get('service_duration_multiplier', 1.0)
                start_time = depot_node['arrival_time_s'] if load_when_ready else start_service_time(depot_node) \
                    + preliminary_service_duration
                waiting_duration = depot_node['waiting_duration_s']
            else:
                start_time = float('-inf')
            self.check_depot_ready_time(route, route_idx, start_time, load_when_ready, waiting_duration)

    def get_max_drop_percentage_penalty(self, drop_penalty_percentage):
        violation = drop_penalty_percentage - self.options.get("max_drop_penalty_percentage", 100.0)
        penalty = self.options.get("penalty", {})
        drop_percentage_penalty = penalty.get("drop_penalty_percentage", {
            "fixed": DEFAULT_MAX_DROP_PERCENTAGE_PENALTY_FIXED,
            "per_percent": DEFAULT_MAX_DROP_PERCENTAGE_PENALTY_PER_PERCENT
        })
        return 0 if violation <= 0 else \
            drop_percentage_penalty["fixed"] + violation + drop_percentage_penalty["per_percent"]

    def check(self):
        self.check_status()
        total = Metrics()
        total.drop_penalty = self.check_locations_are_assigned()
        total.drop_penalty_percentage = total.drop_penalty / self.locations.initial_drop_penalty() * 100.0
        total.max_drop_percentage_penalty = self.get_max_drop_percentage_penalty(total.drop_penalty_percentage)
        self.check_locations_availability()
        self.get_planned_routes()
        self.check_parkings()
        self.check_vehicle_visited_locations()
        self.check_start_end_locations()
        self.check_compatibility()
        self.check_incompatibility("load_types", self.incompatible_types, {
            vid: vehicle["incompatible_types"]
            for vid, vehicle in self.vehicles.items()
            if "incompatible_types" in vehicle
        })
        self.check_incompatibility("zones", self.incompatible_zones)
        self.check_pickup_and_delivery()
        total.depot_penalty = self.check_depot_service_duration_and_throughput()
        total.multiorders_penalty = self.solution["metrics"].get("total_multiorders_penalty", 0.)
        self.check_depot_ready_times()
        self.check_metrics(total)


def check_mvrp_solution(solution, source_data, distances_data, duration_callback,
                        time_accuracy_s=TIME_ACCURACY_S,
                        soft_windows=False,
                        expected_status="SOLVED"):
    try:
        SolutionChecker(solution, source_data, distances_data,
                        duration_callback, time_accuracy_s,
                        soft_windows, expected_status).check()
    except Exception as ex:
        import traceback
        return False, str(ex) + "\n" + traceback.format_exc()
    return True, "Solution is correct."


def remove_internal_params(route, params):
    result = deepcopy(route)
    for param in params:
        result['options'].pop(param, None)
    return result


def run_solver(params):
    # This dependency should not be at the module lever, otherwise it is impossible to use checker separately
    import _solver as solver

    problem, distances, kind, rand_seed, solver_arguments,\
        duration_callback, soft_windows, expected_status = params
    task = solver.SolverTask(vrp_json=problem, sa_rand_seed=rand_seed, **solver_arguments)
    task.distanceMatrixFromJson(distances)
    result = task.solve()

    assert isinstance(result.solution, str)

    response = json.loads(result.solution)
    request = json.loads(problem)

    response_internal_params = [k.split('sa_', 1)[1] for k in solver_arguments.keys() if k.startswith('sa_')]
    route_valid = remove_internal_params(response, response_internal_params)

    tools.validate_format(request, route_valid, kind=kind)

    is_ok, report = check_mvrp_solution(
        response, problem, distances, duration_callback,
        soft_windows=soft_windows, expected_status=expected_status)

    assert is_ok, report

    return response


def solve_and_check(problem,
                    distances=None,
                    solver_arguments={},
                    duration_callback=solver_duration,
                    kind='mvrp',
                    soft_windows=False,
                    expected_status="SOLVED",
                    expected_metrics=None,
                    rel_accuracies=None,
                    runs_count=1,
                    fixed_rand_seed=False):
    assert isinstance(problem, str)

    if distances is None:
        distances = ''

    if fixed_rand_seed or yatest.common.context.sanitize:
        runs_count = 1

    tasks = [(problem, distances, kind, rand_seed, solver_arguments,
              duration_callback, soft_windows, expected_status) for rand_seed in range(runs_count)]
    if runs_count > 1:
        pool = Pool(runs_count)
        responses = pool.map(run_solver, tasks)
    else:
        responses = [run_solver(tasks[0])]

    avg_response = responses[0]
    avg_response["metrics"] = {key : sum(response["metrics"][key] for response in responses) / len(responses)
                               for key in avg_response["metrics"]}

    if expected_metrics:
        tools.check_metrics_are_close(avg_response["metrics"], expected_metrics, rel_accuracies)

    return avg_response


def main():
    args = parse_args()

    logging.basicConfig(format='[%(levelname)s] %(message)s', level=logging.INFO if args.verbose else logging.WARNING)

    solution = json.load(sys.stdin)
    solution = solution.get('result', solution)

    source_data = None
    with open(args.source, "r") as f:
        source_data = f.read()

    distances = None
    if args.distances:
        with open(args.distances, "r") as f:
            distances = f.read()

    is_ok, report = check_mvrp_solution(
        solution,
        source_data,
        distances,
        DURATION_CALLBACKS[args.duration],
        args.time_accuracy,
        expected_status=solution['solver_status']
    )

    print(report)
    if not is_ok:
        sys.exit(-1)


if __name__ == "__main__":
    main()
