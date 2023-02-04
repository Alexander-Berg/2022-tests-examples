#!/usr/bin/env python

from maps.pylibs.coverage5 import Coverage
from cStringIO import StringIO
from yandex.maps.proto.common2.response_pb2 import Response
from yandex.maps.proto.driving.route_pb2 import ROUTE_METADATA

import argparse
import contextlib
import itertools
import numpy
import requests
import shapely.wkt
import shapely.geometry
import subprocess
import time


def random_coords_pairs(min_x, min_y, max_x, max_y):
    def normal_range(min, max):
        a = (min + max) / 2.0
        sigma = (max - min) / 8.0
        while True:
            x = numpy.random.standard_normal() * sigma + a
            if min <= x <= max:
                return x

    while True:
        yield normal_range(min_x, max_x), normal_range(min_y, max_y)


def parse_polyline(polyline_pb):
    def parse_seq(seq):
        return numpy.cumsum([seq.first] + list(seq.deltas)) * 1e-6

    return shapely.geometry.LineString(
        zip(parse_seq(polyline_pb.lons), parse_seq(polyline_pb.lats)))


@contextlib.contextmanager
def run_router(binary_path, port):
    process = subprocess.Popen(
        binary_path, env={"YCR_MODE": "http:{}".format(port)})

    def call(**params):
        while True:
            try:
                r = requests.get(
                    "http://[::1]:{}/route/".format(port),
                    params=params,
                    headers={"Accept": "application/x-protobuf"},
                    stream=True)
                break
            except requests.exceptions.ConnectionError:
                time.sleep(1)
                pass
        r.raise_for_status()

        response = Response()
        response.ParseFromString(r.raw.data)
        return response.reply

    try:
        yield call
    finally:
        process.kill()


def random_routes_endpoints(router, geometry, points_generator):
    def router_request(source, target):
        routes = router(
            rll="{0[0]:.10f},{0[1]:.10f}~{1[0]:.10f},{1[1]:.10f}".format(source, target))

        if len(routes.geo_object) == 0:
            return None
        route = routes.geo_object[0]
        return route

    def route_is_in_region(route):
        polylines = [ parse_polyline(section.geometry[0].polyline)
                for section in route.geo_object ]
        return all(polyline.within(geometry) for polyline in polylines)

    while True:
        source = next(points_generator)
        target = next(points_generator)

        route = router_request(source, target)
        if route is None or not route_is_in_region(route):
            continue

        weight = route.metadata[0].Extensions[ROUTE_METADATA].weight

        METERS_IN_ONE_DEGREE = 90000
        OFFSET_IN_DEGREES = 1.0 / METERS_IN_ONE_DEGREE
        ALLOWED_DISTANCE_DIFFERENCE = 5.0 # meters
        OFFSETS = (
            (0,  OFFSET_IN_DEGREES),
            (0, -OFFSET_IN_DEGREES),
            ( OFFSET_IN_DEGREES, 0),
            (-OFFSET_IN_DEGREES, 0),
        )

        route_is_stable = True

        for offset in OFFSETS:
            distorted_source = (source[0] + offset[0], source[1] + offset[1])
            distorted_target = (target[0] + offset[0], target[1] + offset[1])

            distorted_route = router_request(distorted_source, distorted_target)
            if distorted_route is None:
                route_is_stable = False
                continue
            distorted_weight = distorted_route.metadata[0].Extensions[ROUTE_METADATA].weight

            if (abs(distorted_weight.distance.value - weight.distance.value)
                    > ALLOWED_DISTANCE_DIFFERENCE):
                route_is_stable = False
                continue

        if route_is_stable:
            yield (source, target, (weight.distance.value, weight.time.value))


def main():
    parser = argparse.ArgumentParser(
        description="Make offline driving test data")
    parser.add_argument(
        "-r", "--region",
        dest="region_id",
        type=int,
        required=True,
        help="region id")
    parser.add_argument(
        "-b", "--binary",
        dest="binary",
        type=str,
        required=True,
        help="Path to router binary")
    parser.add_argument(
        "-c", "--count",
        dest="count",
        type=int,
        default=1000,
        help="number of test cases")

    args = parser.parse_args()

    geoid = Coverage("/usr/share/yandex/maps/coverage5/")["geoid"]
    region = next(r for r in geoid.regions(None) if r.id == args.region_id)
    geometry = shapely.wkt.loads(region.wkt_geoms())

    def random_pairs_in_region():
        numpy.random.seed(0)
        for x, y in random_coords_pairs(*geometry.bounds):
            if any(r.id == args.region_id for r in geoid.regions(x, y, None)):
                yield x, y


    print "\t".join([
        "source_lon",
        "source_lat",
        "target_lon",
        "target_lat",
        "distance",
        "duration"])

    with run_router(args.binary, 4283) as router:
        for (source, target, stats) in itertools.islice(
                random_routes_endpoints(router, geometry, random_pairs_in_region()),
                args.count):
            print "\t".join(str(x) for x in source + target + stats)


if __name__ == "__main__":
    main()
