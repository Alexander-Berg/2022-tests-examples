import unittest

import base64
from contextlib import closing
import itertools
import os
import os.path
import socket
import subprocess
import sys
import time
import urllib

import yatest.common

import requests

from yandex.maps.geolib3.proto import decode_proto_polyline
from yandex.maps.proto.driving.alternatives_request_pb2 import AlternativesRequest
from yandex.maps.proto.common2.response_pb2 import Response
from yandex.maps.proto.driving.reroute_request_pb2 import RerouteRequest
from yandex.maps.proto.driving.route_pb2 import ROUTE_METADATA
from yandex.maps.geolib3 import Point2, bearing, radians_to_degrees


def select_random_free_port():
    with closing(socket.socket(socket.AF_INET, socket.SOCK_STREAM)) as sock:
        sock.bind(("", 0))
        return sock.getsockname()[1]


def parse_route_polyline(route):
    result_polyline = []
    for section in route.geo_object:
        polyline = decode_proto_polyline(section.geometry[0].polyline)
        for point in polyline:
            result_polyline.append((point.x, point.y))
    return result_polyline


def to_pctx(*daps):
    # Don't encode point context in end user applications. Its format is
    # a subject to change. Here we do encoding to test routes with
    # driving arrival points
    pctx_list = [base64.urlsafe_b64encode("v1||" + d).replace("=", ",")
                 if d else ""
                 for d in daps]
    return "~".join(pctx_list)


def get_dir(polyline, index):
    assert index + 1 < len(polyline)
    return radians_to_degrees(bearing(
        Point2(*polyline[index]), Point2(*polyline[index + 1])))


class DapllTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls._port = select_random_free_port()

        env = os.environ.copy()
        env["CONFIG"] = yatest.common.output_path("router.json")
        env["YCR_MODE"] = "http:{}".format(cls._port)

        binary_path = os.path.join(
            yatest.common.binary_path("maps/routing/router/yacare/bin"),
            "router")

        cls._router_process = subprocess.Popen(
            [binary_path],
            stdout=subprocess.PIPE,
            stdin=subprocess.PIPE,
            env=env)

        while cls._router_process.poll() is None:
            try:
                requests.request(
                    "GET",
                    "http://localhost:{}/graph_version".format(cls._port),
                    headers={"Host": "router.maps.yandex.ru"}
                ).raise_for_status()

                break

            except requests.RequestException:
                time.sleep(1)

        if cls._router_process.poll() is not None:
            raise RuntimeError("Router process exited")

    @classmethod
    def tearDownClass(cls):
        cls._router_process.terminate()

    def send_request(
            self,
            method,
            handle,
            dir=0,
            results=5,
            timeleft=7000,  # dummy value
            via=None,
            rll=None,
            dapll=None,
            pctx=None,
            traits=None,
            enable_carparks_alternatives_experiment=True,
            body_pb=None):
        arguments = {
            "dir": dir,
            "results": results,
            "timeleft": timeleft,
            "mode": "best"
        }

        if via:
            arguments["via"] = via
        if rll:
            arguments["rll"] = rll
        if dapll:
            arguments["dapll"] = dapll
        if pctx:
            arguments["pctx"] = pctx
        if traits:
            arguments["traits"] = base64.urlsafe_b64encode(traits)
        if enable_carparks_alternatives_experiment:
            arguments["experimental_carparks_alternatives_enabled"] = 1

        http_response = requests.request(
            method,
            "http://localhost:{}{}?{}".format(
                self._port, handle, urllib.urlencode(arguments)),
            headers={
                "Accept": "application/x-protobuf",
                "Host": "router.maps.yandex.ru"
            },
            data=(body_pb.SerializeToString() if body_pb else None))
        http_response.raise_for_status()

        response = Response()
        response.ParseFromString(http_response.content)
        return response

    def parse_check_response(self,
                             response,
                             expected_carpark_daplls,
                             expected_nearby_dapll=None,
                             route_points_count=2,
                             dapll_to_return=None):
        result_route = None
        result_route_traits = None
        result_route_descriptor = None

        sys.stderr.write("Found {} alternatives\n"
                         .format(len(response.reply.geo_object)))
        found_dapll = []
        for object in response.reply.geo_object:
            metadata = object.metadata[0].Extensions[ROUTE_METADATA]
            self.assertEqual(len(metadata.route_point), route_points_count)

            route = parse_route_polyline(object)
            id = metadata.route_point[1].driving_arrival_point_id
            sys.stderr.write("Found route to id {} {}\n".format(
                id, route[-1]))
            if id == expected_nearby_dapll:
                # we check only carpark alternatives, not nearby
                sys.stderr.write("This is a nearby alternative, not checking it\n")
            else:
                found_dapll.append((id, route[-1]))

            if id == dapll_to_return and result_route is None:
                result_route = route
                result_route_traits = metadata.route_traits
                result_route_descriptor = metadata.route_descriptor

        self.assertEqual(len(found_dapll), len(expected_carpark_daplls))

        for found, expected in itertools.izip_longest(
                found_dapll, expected_carpark_daplls):
            self.assertEqual(found[0], expected[0])
            self.assertAlmostEqual(found[1][0], expected[1][0], places=2)
            self.assertAlmostEqual(found[1][1], expected[1][1], places=2)

        return result_route, result_route_descriptor, result_route_traits

    def test_all(self):
        dapll = "~37.6,55.8,1;37.7,55.9,2;37.8,55.8,3;37.5,55.8,4"
        rll = "{},{}~37.7,55.8"

        # make first request for route
        response = self.send_request(
            "GET",
            "/v2/route",
            rll=rll.format(37.5, 55.7),
            dapll=dapll)

        expected_carpark_daplls = []
        main_route, main_route_descriptor, main_route_traits = \
            self.parse_check_response(
                response,
                expected_carpark_daplls,
                expected_nearby_dapll="",
                dapll_to_return="")

        # move to a point near destination
        index = len(main_route) * 4 / 5

        # try reroute from that point, should reroute to original destination
        response = self.send_request(
            "POST",
            "/v2/reroute",
            results=5,
            rll=rll.format(*main_route[index]),
            dir=get_dir(main_route, index),
            dapll=dapll,
            traits=main_route_traits,
            body_pb=RerouteRequest())

        expected_carpark_daplls = []
        self.parse_check_response(
            response,
            expected_carpark_daplls,
            expected_nearby_dapll="")

        # request alternatives, should return carpark alternatives
        alternatives_request = AlternativesRequest()
        alternatives_request.route_representation.route_descriptor \
            = main_route_descriptor
        response = self.send_request(
            "POST",
            "/v2/nearby_alternatives",
            results=5,
            rll=rll.format(*main_route[index]),
            dir=get_dir(main_route, index),
            dapll=dapll,
            body_pb=alternatives_request)

        expected_carpark_daplls = [
            ("1", (37.6, 55.8)),
            ("3", (37.8, 55.8)),
            ("2", (37.7, 55.9))]

        (alternative_route,
         alternative_route_descriptor,
         alternative_route_traits) = \
            self.parse_check_response(
                response,
                expected_carpark_daplls,
                expected_nearby_dapll="",
                dapll_to_return="3")

        # now reroute from alternative route, should reroute to dapll
        response = self.send_request(
            "POST",
            "/v2/reroute",
            results=5,
            rll=rll.format(*main_route[index]),
            dir=get_dir(main_route, index),
            dapll=dapll,
            traits=alternative_route_traits,
            body_pb=RerouteRequest())

        expected_carpark_daplls = []
        self.parse_check_response(
            response,
            expected_carpark_daplls,
            expected_nearby_dapll="3")

        # request alternatives from alternative road
        alternatives_request = AlternativesRequest()
        alternatives_request.route_representation.route_descriptor \
            = alternative_route_descriptor
        response = self.send_request(
            "POST",
            "/v2/nearby_alternatives",
            results=5,
            rll=rll.format(*alternative_route[10]),
            dir=get_dir(alternative_route, 10),
            dapll=dapll,
            traits=alternative_route_traits,
            body_pb=alternatives_request)

        expected_carpark_daplls = [
            ("", (37.7, 55.8)),
            ("1", (37.6, 55.8))]

        self.parse_check_response(
            response,
            expected_carpark_daplls,
            expected_nearby_dapll="3")

    def test_pctx_for_via(self):
        pctx = to_pctx("", "37.7,55.8,1;37.7,55.9,2;37.8,55.8,3;37.5,55.8,4", "")

        rll = "{},{}~37.7,55.8~37.5,55.7"
        via = "1"
        pctx_after_passed = to_pctx("")

        rll_after_passed = "{},{}~37.5,55.7"
        via_after_passed = ""

        # make first request for route
        response = self.send_request(
            "GET",
            "/v2/route",
            rll=rll.format(37.5, 55.7),
            pctx=pctx,
            via=via)

        expected_carpark_daplls = []
        main_route, main_route_descriptor, main_route_traits = \
            self.parse_check_response(
                response,
                expected_carpark_daplls,
                expected_nearby_dapll="",
                route_points_count=3,
                dapll_to_return="")

        index = len(main_route) * 2 / 5

        # try reroute from that point, should reroute to original destination
        response = self.send_request(
            "POST",
            "/v2/reroute",
            results=5,
            rll=rll.format(*main_route[index]),
            dir=get_dir(main_route, index),
            pctx=pctx,
            via=via,
            traits=main_route_traits,
            body_pb=RerouteRequest())

        expected_carpark_daplls = []
        self.parse_check_response(
            response,
            expected_carpark_daplls,
            expected_nearby_dapll="",
            route_points_count=3)

        # request alternatives, should return carpark alternatives
        alternatives_request = AlternativesRequest()
        alternatives_request.route_representation.route_descriptor \
            = main_route_descriptor
        response = self.send_request(
            "POST",
            "/v2/nearby_alternatives",
            results=5,
            rll=rll.format(*main_route[index]),
            dir=get_dir(main_route, index),
            pctx=pctx,
            via=via,
            body_pb=alternatives_request)

        # all alternatives finally end at last destination point
        expected_carpark_daplls = [("4", (37.5, 55.7)),
                                   ("3", (37.5, 55.7)),
                                   ("2", (37.5, 55.7))]
        (alternative_route,
         alternative_route_descriptor,
         alternative_route_traits) = self.parse_check_response(
            response,
            expected_carpark_daplls,
            expected_nearby_dapll="",
            route_points_count=3,
            dapll_to_return="3")

        # now reroute from alternative route, should reroute to dapll
        response = self.send_request(
            "POST",
            "/v2/reroute",
            results=5,
            rll=rll.format(*main_route[index]),
            dir=get_dir(main_route, index),
            pctx=pctx,
            via=via,
            traits=alternative_route_traits,
            body_pb=RerouteRequest())

        expected_carpark_daplls = []
        self.parse_check_response(
            response,
            expected_carpark_daplls,
            expected_nearby_dapll="3",
            route_points_count=3)

        # request alternatives from alternative road
        alternatives_request = AlternativesRequest()
        alternatives_request.route_representation.route_descriptor \
            = alternative_route_descriptor
        response = self.send_request(
            "POST",
            "/v2/nearby_alternatives",
            results=5,
            rll=rll.format(*alternative_route[10]),
            dir=get_dir(alternative_route, 10),
            pctx=pctx,
            via=via,
            traits=alternative_route_traits,
            body_pb=alternatives_request)

        expected_carpark_daplls = [("", (37.5, 55.7)),
                                   ("4", (37.5, 55.7))]
        self.parse_check_response(
            response,
            expected_carpark_daplls,
            expected_nearby_dapll="3",
            route_points_count=3)

        # pass via point
        # now reroute from alternative route, should reroute to dapll
        response = self.send_request(
            "POST",
            "/v2/reroute",
            results=5,
            rll=rll_after_passed.format(*main_route[index]),
            dir=get_dir(main_route, index),
            pctx=pctx_after_passed,
            via=via_after_passed,
            traits=alternative_route_traits,
            body_pb=RerouteRequest())

        expected_carpark_daplls = []
        self.parse_check_response(
            response,
            expected_carpark_daplls,
            expected_nearby_dapll="",
            route_points_count=2)

        # request alternatives from alternative road
        alternatives_request = AlternativesRequest()
        alternatives_request.route_representation.route_descriptor \
            = alternative_route_descriptor
        response = self.send_request(
            "POST",
            "/v2/nearby_alternatives",
            results=5,
            rll=rll_after_passed.format(*alternative_route[10]),
            dir=get_dir(alternative_route, 10),
            pctx=pctx_after_passed,
            via=via_after_passed,
            traits=alternative_route_traits,
            body_pb=alternatives_request)

        expected_carpark_daplls = []
        self.parse_check_response(
            response,
            expected_carpark_daplls,
            expected_nearby_dapll="",
            route_points_count=2)

    def test_destination_is_replaced_with_parking_dap_if_there_is_no_dropoff_dap(self):
        pctx = to_pctx("", "37.7,55.8,1_;37.7,55.9,2;37.8,55.8,3;37.5,55.8,4")
        rll = "{},{}~37.6,55.8"

        # make first request for route
        response = self.send_request(
            "GET",
            "/v2/route",
            rll=rll.format(37.5, 55.7),
            pctx=pctx)

        # all routes go to a popular dap instead of the original destination
        expected_carpark_daplls = []
        main_route, main_route_descriptor, main_route_traits = \
            self.parse_check_response(
                response,
                expected_carpark_daplls,
                expected_nearby_dapll="1_",
                dapll_to_return="1_")

        # move to a point near destination
        index = len(main_route) * 4 / 5

        # try reroute from that point, should reroute to original destination
        response = self.send_request(
            "POST",
            "/v2/reroute",
            results=5,
            rll=rll.format(*main_route[index]),
            dir=get_dir(main_route, index),
            pctx=pctx,
            traits=main_route_traits,
            body_pb=RerouteRequest())

        expected_carpark_daplls = []
        self.parse_check_response(
            response,
            expected_carpark_daplls,
            expected_nearby_dapll="1_")

        # request alternatives, should return carpark alternatives
        alternatives_request = AlternativesRequest()
        alternatives_request.route_representation.route_descriptor \
            = main_route_descriptor
        response = self.send_request(
            "POST",
            "/v2/nearby_alternatives",
            results=5,
            rll=rll.format(*main_route[index]),
            dir=get_dir(main_route, index),
            pctx=pctx,
            body_pb=alternatives_request)

        expected_carpark_daplls = [
            ("", (37.6, 55.8)),
            ("3", (37.8, 55.8)),
            ("2", (37.7, 55.9))]

        (alternative_route,
         alternative_route_descriptor,
         alternative_route_traits) = \
            self.parse_check_response(
                response,
                expected_carpark_daplls,
                expected_nearby_dapll="1_",
                dapll_to_return="3")

        # now reroute from alternative route, should reroute to dapll
        response = self.send_request(
            "POST",
            "/v2/reroute",
            results=5,
            rll=rll.format(*main_route[index]),
            dir=get_dir(main_route, index),
            pctx=pctx,
            traits=alternative_route_traits,
            body_pb=RerouteRequest())

        expected_carpark_daplls = []
        self.parse_check_response(
            response,
            expected_carpark_daplls,
            expected_nearby_dapll="3")

        # request alternatives from alternative road
        alternatives_request = AlternativesRequest()
        alternatives_request.route_representation.route_descriptor \
            = alternative_route_descriptor
        response = self.send_request(
            "POST",
            "/v2/nearby_alternatives",
            results=5,
            rll=rll.format(*alternative_route[10]),
            dir=get_dir(alternative_route, 10),
            pctx=pctx,
            traits=alternative_route_traits,
            body_pb=alternatives_request)

        expected_carpark_daplls = [
            ("1_", (37.7, 55.8)),
            ("", (37.6, 55.8))]

        self.parse_check_response(
            response,
            expected_carpark_daplls,
            expected_nearby_dapll="3")

    def test_destination_is_replaced_with_dropoff_dap(self):
        pctx = to_pctx("", "37.7,55.8,1d;37.7,55.9,2;37.8,55.8,3;37.5,55.8,4")
        rll = "{},{}~37.6,55.8"

        # make first request for route
        response = self.send_request(
            "GET",
            "/v2/route",
            rll=rll.format(37.5, 55.7),
            pctx=pctx)

        # all routes go to a popular dap instead of the original destination
        expected_carpark_daplls = []
        main_route, main_route_descriptor, main_route_traits = \
            self.parse_check_response(
                response,
                expected_carpark_daplls,
                expected_nearby_dapll="1d",
                dapll_to_return="1d")

        # move to a point near destination
        index = len(main_route) * 4 / 5

        # try reroute from that point, should reroute to original destination
        response = self.send_request(
            "POST",
            "/v2/reroute",
            results=5,
            rll=rll.format(*main_route[index]),
            dir=get_dir(main_route, index),
            pctx=pctx,
            traits=main_route_traits,
            body_pb=RerouteRequest())

        expected_carpark_daplls = []
        self.parse_check_response(
            response,
            expected_carpark_daplls,
            expected_nearby_dapll="1d")

        # request alternatives, should return carpark alternatives
        alternatives_request = AlternativesRequest()
        alternatives_request.route_representation.route_descriptor \
            = main_route_descriptor
        response = self.send_request(
            "POST",
            "/v2/nearby_alternatives",
            results=5,
            rll=rll.format(*main_route[index]),
            dir=get_dir(main_route, index),
            pctx=pctx,
            body_pb=alternatives_request)

        expected_carpark_daplls = [
            ("", (37.6, 55.8)),
            ("3", (37.8, 55.8)),
            ("2", (37.7, 55.9))]

        (alternative_route,
         alternative_route_descriptor,
         alternative_route_traits) = \
            self.parse_check_response(
                response,
                expected_carpark_daplls,
                expected_nearby_dapll="1d",
                dapll_to_return="3")

        # now reroute from alternative route, should reroute to dapll
        response = self.send_request(
            "POST",
            "/v2/reroute",
            results=5,
            rll=rll.format(*main_route[index]),
            dir=get_dir(main_route, index),
            pctx=pctx,
            traits=alternative_route_traits,
            body_pb=RerouteRequest())

        expected_carpark_daplls = []
        self.parse_check_response(
            response,
            expected_carpark_daplls,
            expected_nearby_dapll="3")

        # request alternatives from alternative road
        alternatives_request = AlternativesRequest()
        alternatives_request.route_representation.route_descriptor \
            = alternative_route_descriptor
        response = self.send_request(
            "POST",
            "/v2/nearby_alternatives",
            results=5,
            rll=rll.format(*alternative_route[10]),
            dir=get_dir(alternative_route, 10),
            pctx=pctx,
            traits=alternative_route_traits,
            body_pb=alternatives_request)

        expected_carpark_daplls = [
            ("1d", (37.7, 55.8)),
            ("", (37.6, 55.8))]

        self.parse_check_response(
            response,
            expected_carpark_daplls,
            expected_nearby_dapll="3")

    def test_parking_dap_is_replaced_with_dropoff_dap(self):
        pctx = to_pctx("", "37.7,55.8,1d;37.7,55.9,2_;37.8,55.8,3;37.5,55.8,4")
        rll = "{},{}~37.6,55.8"

        # make first request for route
        response = self.send_request(
            "GET",
            "/v2/route",
            rll=rll.format(37.5, 55.7),
            pctx=pctx)

        # all routes go to a popular dap instead of the original destination
        expected_carpark_daplls = []
        main_route, main_route_descriptor, main_route_traits = \
            self.parse_check_response(
                response,
                expected_carpark_daplls,
                expected_nearby_dapll="1d",
                dapll_to_return="1d")

        # move to a point near destination
        index = len(main_route) * 4 / 5

        # try reroute from that point, should reroute to original destination
        response = self.send_request(
            "POST",
            "/v2/reroute",
            results=5,
            rll=rll.format(*main_route[index]),
            dir=get_dir(main_route, index),
            pctx=pctx,
            traits=main_route_traits,
            body_pb=RerouteRequest())

        expected_carpark_daplls = []
        self.parse_check_response(
            response,
            expected_carpark_daplls,
            expected_nearby_dapll="1d")

        # request alternatives, should return carpark alternatives
        alternatives_request = AlternativesRequest()
        alternatives_request.route_representation.route_descriptor \
            = main_route_descriptor
        response = self.send_request(
            "POST",
            "/v2/nearby_alternatives",
            results=5,
            rll=rll.format(*main_route[index]),
            dir=get_dir(main_route, index),
            pctx=pctx,
            body_pb=alternatives_request)

        expected_carpark_daplls = [
            ("", (37.6, 55.8)),
            ("3", (37.8, 55.8)),
            ("2_", (37.7, 55.9))]

        (alternative_route,
         alternative_route_descriptor,
         alternative_route_traits) = \
            self.parse_check_response(
                response,
                expected_carpark_daplls,
                expected_nearby_dapll="1d",
                dapll_to_return="3")

        # now reroute from alternative route, should reroute to dapll
        response = self.send_request(
            "POST",
            "/v2/reroute",
            results=5,
            rll=rll.format(*main_route[index]),
            dir=get_dir(main_route, index),
            pctx=pctx,
            traits=alternative_route_traits,
            body_pb=RerouteRequest())

        expected_carpark_daplls = []
        self.parse_check_response(
            response,
            expected_carpark_daplls,
            expected_nearby_dapll="3")

        # request alternatives from alternative road
        alternatives_request = AlternativesRequest()
        alternatives_request.route_representation.route_descriptor \
            = alternative_route_descriptor
        response = self.send_request(
            "POST",
            "/v2/nearby_alternatives",
            results=5,
            rll=rll.format(*alternative_route[10]),
            dir=get_dir(alternative_route, 10),
            pctx=pctx,
            traits=alternative_route_traits,
            body_pb=alternatives_request)

        expected_carpark_daplls = [
            ("1d", (37.7, 55.8)),
            ("", (37.6, 55.8))]

        self.parse_check_response(
            response,
            expected_carpark_daplls,
            expected_nearby_dapll="3")

    def test_pctx_for_second_point_are_not_considered(self):
        pctx = to_pctx("", "", "37.6,55.8,1;37.7,55.9,2;37.8,55.8,3;37.5,55.8,4")
        rll = "{},{}~37.500340,55.748914~37.7,55.8"

        # make first request for route
        response = self.send_request(
            "GET",
            "/v2/route",
            rll=rll.format(37.5, 55.7),
            pctx=pctx)

        expected_carpark_daplls = []
        main_route, main_route_descriptor, main_route_traits = \
            self.parse_check_response(
                response,
                expected_carpark_daplls,
                expected_nearby_dapll="",
                route_points_count=3,
                dapll_to_return="")

        # simulate small move along the route
        # so that we can request nearby alternatives
        index = len(main_route) * 1 / 30

        # request alternatives, should *not* return carpark alternatives
        alternatives_request = AlternativesRequest()
        alternatives_request.route_representation.route_descriptor \
            = main_route_descriptor
        response = self.send_request(
            "POST",
            "/v2/nearby_alternatives",
            results=5,
            rll=rll.format(*main_route[index]),
            dir=get_dir(main_route, index),
            pctx=pctx,
            body_pb=alternatives_request)

        expected_carpark_daplls = []
        self.parse_check_response(
            response,
            expected_carpark_daplls,
            expected_nearby_dapll="",
            route_points_count=3)

    def test_pctx_for_second_point_are_not_considered_even_with_via(self):
        pctx = to_pctx("", "", "37.6,55.8,1;37.7,55.9,2;37.8,55.8,3;37.5,55.8,4")
        rll = "{},{}~37.500340,55.748914~37.7,55.8"
        via = "1"

        # make first request for route
        response = self.send_request(
            "GET",
            "/v2/route",
            rll=rll.format(37.5, 55.7),
            pctx=pctx,
            via=via)

        expected_carpark_daplls = []
        main_route, main_route_descriptor, main_route_traits = \
            self.parse_check_response(
                response,
                expected_carpark_daplls,
                expected_nearby_dapll="",
                route_points_count=3,
                dapll_to_return="")

        # simulate small move along the route
        # so that we can request nearby alternatives
        index = len(main_route) * 1 / 30

        # request alternatives, should *not* return carpark alternatives
        alternatives_request = AlternativesRequest()
        alternatives_request.route_representation.route_descriptor \
            = main_route_descriptor
        response = self.send_request(
            "POST",
            "/v2/nearby_alternatives",
            results=5,
            rll=rll.format(*main_route[index]),
            dir=get_dir(main_route, index),
            pctx=pctx,
            via=via,
            body_pb=alternatives_request)

        expected_carpark_daplls = []
        self.parse_check_response(
            response,
            expected_carpark_daplls,
            expected_nearby_dapll="",
            route_points_count=3)

    def test_fork_at_end(self):
        pctx = to_pctx("", "37.765244,55.776705,1")
        rll = "{},{}~37.765102,55.777369"

        # make first request for route
        response = self.send_request(
            "GET",
            "/v2/route",
            rll=rll.format(37.762061, 55.787936),
            pctx=pctx)

        expected_carpark_daplls = []
        main_route, main_route_descriptor, main_route_traits = \
            self.parse_check_response(
                response,
                expected_carpark_daplls,
                expected_nearby_dapll="",
                dapll_to_return="")

        # simulate small move along the route
        # so that we can request nearby alternatives
        index = 1

        # request alternatives, should return carpark alternatives
        alternatives_request = AlternativesRequest()
        alternatives_request.route_representation.route_descriptor \
            = main_route_descriptor
        response = self.send_request(
            "POST",
            "/v2/nearby_alternatives",
            results=5,
            rll=rll.format(*main_route[index]),
            dir=get_dir(main_route, index),
            pctx=pctx,
            body_pb=alternatives_request)

        expected_carpark_daplls = [("1", (37.765244, 55.776705))]
        self.parse_check_response(
            response,
            expected_carpark_daplls,
            expected_nearby_dapll="")

    def test_no_alternatives_without_experiment(self):
        pctx = to_pctx(
            "", "37.6,55.8,1,0.25;37.7,55.9,2,0.25;37.8,55.8,3,0.25;37.5,55.8,4,0.25")
        rll = "{},{}~37.7,55.8"

        # make first request for route
        response = self.send_request(
            "GET",
            "/v2/route",
            rll=rll.format(37.5, 55.7),
            pctx=pctx)

        expected_carpark_daplls = []
        main_route, main_route_descriptor, main_route_traits = \
            self.parse_check_response(
                response,
                expected_carpark_daplls,
                expected_nearby_dapll="",
                dapll_to_return="")

        # move to a point near destination
        index = len(main_route) * 4 / 5

        # request alternatives, should *not* return carpark alternatives
        alternatives_request = AlternativesRequest()
        alternatives_request.route_representation.route_descriptor \
            = main_route_descriptor
        response = self.send_request(
            "POST",
            "/v2/nearby_alternatives",
            results=5,
            rll=rll.format(*main_route[index]),
            dir=get_dir(main_route, index),
            pctx=pctx,
            enable_carparks_alternatives_experiment=False,
            body_pb=alternatives_request)

        expected_carpark_daplls = []
        self.parse_check_response(
            response,
            expected_carpark_daplls,
            expected_nearby_dapll="")

    def test_with_context(self):
        pctx = to_pctx(
            "", "37.6,55.8,1,0.25;37.7,55.9,2,0.25;37.8,55.8,3,0.25;37.5,55.8,4,0.25")
        rll = "{},{}~37.7,55.8"

        # make first request for route
        response = self.send_request(
            "GET",
            "/v2/route",
            rll=rll.format(37.5, 55.7),
            pctx=pctx)

        expected_carpark_daplls = []
        main_route, main_route_descriptor, main_route_traits = \
            self.parse_check_response(
                response,
                expected_carpark_daplls,
                expected_nearby_dapll="",
                dapll_to_return="")

        # move to a point near destination
        index = len(main_route) * 4 / 5

        # request alternatives, should return carpark alternatives
        alternatives_request = AlternativesRequest()
        alternatives_request.route_representation.route_descriptor \
            = main_route_descriptor
        response = self.send_request(
            "POST",
            "/v2/nearby_alternatives",
            results=5,
            rll=rll.format(*main_route[index]),
            dir=get_dir(main_route, index),
            pctx=pctx,
            body_pb=alternatives_request)

        expected_carpark_daplls = [
            ("1", (37.6, 55.8)),
            ("3", (37.8, 55.8)),
            ("2", (37.7, 55.9))]

        self.parse_check_response(
            response,
            expected_carpark_daplls,
            expected_nearby_dapll="")

    def test_nonempty_selected_dap_in_traits_and_empty_pctx(self):
        pctx = to_pctx(
            "", "37.6,55.8,1_,0.25;37.7,55.9,2,0.25;37.8,55.8,3,0.25;37.5,55.8,4,0.25")
        rll = "{},{}~37.7,55.8"

        # make first request for route
        response = self.send_request(
            "GET",
            "/v2/route",
            rll=rll.format(37.5, 55.7),
            pctx=pctx,
            results=1)

        expected_carpark_daplls = [("1_", (37.6, 55.8))]

        main_route, main_route_descriptor, main_route_traits = \
            self.parse_check_response(
                response,
                expected_carpark_daplls,
                expected_nearby_dapll="",
                dapll_to_return="1_")

        # move to a point near destination
        index = len(main_route) * 4 / 5

        # request alternatives, should return carpark alternatives
        alternatives_request = AlternativesRequest()
        alternatives_request.route_representation.route_descriptor \
            = main_route_descriptor
        response = self.send_request(
            "POST",
            "/v2/nearby_alternatives",
            results=5,
            rll=rll.format(*main_route[index]),
            dir=get_dir(main_route, index),
            pctx="",
            traits=main_route_traits,
            body_pb=alternatives_request)

        expected_carpark_daplls = []
        self.parse_check_response(
            response,
            expected_carpark_daplls,
            expected_nearby_dapll="")
