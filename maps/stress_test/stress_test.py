#!/usr/bin/env python

from argparse import ArgumentParser
from contextlib import contextmanager
from itertools import product
from os import fdopen
from sys import stderr
import json
import math
import random
import subprocess
import sys
import time

from pexpect import spawn
from yandex.maps.geolib3 import (
    BoundingBox, degrees_to_radians, geodistance, Point2)
from yandex.maps.jams.graph4 import Graph, EdgesRTree


DISTANCE_FROM_GRAPH_VERTEX = 1000
EARTH_RADIUS = 6378137.0
METERS_IN_ONE_KM = 1000
ROUTER_PROMPT = "\r\n> "
WAIT_FOR_ONE_QUERY = 5
MAX_ROUTER_START_TIME = 900  # 15 minutes


@contextmanager
def output_duration(description):
    start_time = time.time()
    print >>stderr, description,
    yield
    print >>stderr, "done in {0:.2} s.".format(time.time() - start_time)


@contextmanager
def start_router(path_to_router_executable):
    with output_duration("Starting router..."):
        router = spawn(path_to_router_executable)
        router.delaybeforesend = 0
        router.expect(ROUTER_PROMPT, timeout=MAX_ROUTER_START_TIME)

    try:
        yield router
    finally:
        if router.isalive():
            with output_duration("Stopping router..."):
                router.terminate()
                router.wait()


def uniform_choose_in_bbox(bbox):
    return Point2(
        random.uniform(bbox.min_x, bbox.max_x),
        random.uniform(bbox.min_y, bbox.max_y))


def construct_queries(point_from, point_to):
    rll = "/?rll={0}%2C{1}~{2}%2C{3}".format(
        point_from.lon, point_from.lat, point_to.lon, point_to.lat)
    return ["".join(c) for c in product(
        ("GET /route",), ("", "_jams"), (rll,), ("", "&results=3"))]


def convert_distance_to_degrees(lat, distance):
    lon_distance = 360 * distance / (2 * math.pi * EARTH_RADIUS)
    lat_distance = lon_distance / math.cos(degrees_to_radians(lat))
    return lon_distance, lat_distance


def correct_coords(point):
    return Point2(
        (point.lon + 180.) % 360. - 180., (point.lat + 90.) % 180. - 90.)


def get_random_point_in_cicle(center, radius):
    lon_delta, lat_delta = convert_distance_to_degrees(center.lat, radius)
    while True:
        bbox = BoundingBox(center, lon_delta, lat_delta)
        point = uniform_choose_in_bbox(bbox)
        point = correct_coords(point)
        if geodistance(center, point) < radius:
            return point


class StressTest(object):
    def __init__(
            self,
            router,
            path_to_router_executable,
            num_tests,
            region_center,
            region_radius,
            max_distance_between_points,
            timeout,
            die_immediately):
        self._path_to_router_executable = path_to_router_executable
        self._num_tests = num_tests
        self._done_tests = 0
        self._region_center = region_center
        self._region_radius = region_radius
        self._max_distance_between_points = max_distance_between_points
        self._router = router
        self._timeout = timeout
        self._die_immediately = die_immediately

    def _check_query(self, query):
        if not self._router.isalive():
            print >>stderr, "router process isn't alive",
            return False
        try:
            self._router.sendline(query)
            self._router.expect(ROUTER_PROMPT, timeout=self._timeout)
        except:
            print >>stderr, "hasn't ended in {0} s.".format(self._timeout),
            return False
        return self._router.before.find("HTTP 200") >= 0

    def _create_uniform_selector(self):
        def select_endpoint():
            point_from = get_random_point_in_cicle(
                self._region_center, self._region_radius)
            point_to = get_random_point_in_cicle(
                point_from, self._max_distance_between_points)
            return point_from, point_to

        return select_endpoint

    def _create_near_vertices_selector(self):
        config_string, _ = subprocess.Popen(
            [self._path_to_router_executable, "--conf"],
            stdout=subprocess.PIPE).communicate()

        config = json.loads(config_string)

        with output_duration("mmap graph..."):
            graph = Graph(
                str(config["graph"]), str(config["data"]))

        with output_duration("mmap rtree..."):
            rtree = EdgesRTree(str(config["edges_rtree"]))

        def get_almost_nearest_vertex(point):
            # '1' is the amount of nearest edges to search.
            # There may be more than one nearest edges. Choose first of them.
            nearest_edge = rtree.nearest_edges(graph, point, 1)[0]
            almost_nearest_vertex = graph.edge(nearest_edge).source
            return graph.vertex_geometry(almost_nearest_vertex)

        def select_endpoint():
            point_from, point_to = self._create_uniform_selector()()
            return (
                get_almost_nearest_vertex(point_from),
                get_almost_nearest_vertex(point_to))

        return select_endpoint

    def _run_test(self, id, select_endpoint):
        errors = 0
        point_from, point_to = select_endpoint()
        for sub_id, query in enumerate(
                construct_queries(point_from, point_to)):
            print >>stderr, "{0}.{1}: {2}".format(id, sub_id, query),
            self._done_tests += 1
            start_time = time.time()
            if not self._check_query(query):
                print >>stderr, "FAIL"
                errors += 1
            else:
                print >>stderr, "OK in {0:.2} ms".format(
                    (time.time() - start_time) * 1000)
        return errors

    def _run_tests(self, select_endpoint):
        errors = 0
        tests_start_time = time.time()
        for test in xrange(self._num_tests):
            errors += self._run_test(test, select_endpoint)
            if errors and self._die_immediately:
                break

        return errors, time.time() - tests_start_time

    def run(self):
        p2p_errors, p2p_running_time = self._run_tests(
            self._create_uniform_selector())
        if p2p_errors and self._die_immediately:
            return True
        v2v_errors, v2v_running_time = self._run_tests(
            self._create_near_vertices_selector())

        print >>stderr, "Run {0} tests in {1} s. ({2} + {3})".format(
            self._done_tests, p2p_running_time + v2v_running_time,
            p2p_running_time, v2v_running_time)

        errors = p2p_errors + v2v_errors
        if errors:
            print >>stderr, "Errors: {0}({1} + {2})".format(
                errors, p2p_errors, v2v_errors)
        return True if errors else False


def main():
    parser = ArgumentParser(description="Run stress test for router")
    parser.add_argument(
        "--router", dest="path_to_router_executable", default="./router",
        help="path for router executable")
    parser.add_argument(
        "-n", "--num-tests", dest="tests", type=int, default=100,
        help="number of tests in each query construct strategy")
    parser.add_argument(
        "-b", "--base", default="37.617778,55.751667",
        help="lon,lat of the test region center (default is Moscow Kremlin)")
    parser.add_argument(
        "-r", "--radius", type=float, default=20038,
        help="radius of the test region in km (default is half of equator")
    parser.add_argument(
        "-t", "--timeout", type=float, default=WAIT_FOR_ONE_QUERY,
        help="wait for response of one query")
    parser.add_argument(
        "-d", "--distance", type=float, default=200,
        help="max distance between checking points in km")
    parser.add_argument(
        "--die-immediately", dest="die", action="store_true",
        help="stop after first failed test")
    args = parser.parse_args()

    # autoflush of stdout
    sys.stdout = fdopen(sys.stdout.fileno(), 'w', 0)

    region_radius = args.radius * METERS_IN_ONE_KM
    max_distance_between_points = args.distance * METERS_IN_ONE_KM
    region_center = Point2(*map(float, args.base.split(',')))

    with start_router(args.path_to_router_executable) as router:
        tester = StressTest(
            router,
            args.path_to_router_executable,
            args.tests,
            region_center,
            region_radius,
            max_distance_between_points,
            args.timeout,
            args.die)

        fail = tester.run()
        if fail:
            exit(1)

if __name__ == '__main__':
    main()
