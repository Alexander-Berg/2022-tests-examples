import unittest
import time
import os
import re
import urllib.parse
from request_client import RequestClient, DEFAULT_APIKEY


ROUTER_API_URL = os.environ.get(
    "ROUTER_API_URL",
    "https://prestable.api.routing.n.yandex-team.ru/v1/route")

HEADERS = {"Host": "api.routing.yandex.net"}

SIMPLE_ONE_SEGMENT_QUERY = "?waypoints=55.734494627139355,37.68191922355621|55.733441295701056,37.59027350593535"

MAX_RPS = 10

ROUTING_MODES = ['driving', 'transit', 'walking']

_request_client = RequestClient(MAX_RPS)


def request_without_apikey(query_url):
    return _request_client.request(ROUTER_API_URL + query_url, headers=HEADERS)


def request(query_url, apikey=DEFAULT_APIKEY):
    return _request_client.request(f"{ROUTER_API_URL}{query_url}&apikey={apikey}", headers=HEADERS)


def request_absolute_path(path):
    url = urllib.parse.urlparse(ROUTER_API_URL)
    url = url._replace(path=path, query='').geturl()
    return _request_client.request(url, headers=HEADERS)


def is_v2_api():
    return '/v2' in ROUTER_API_URL


class TestRouterApi(unittest.TestCase):
    def assert_code(self, resp, expected_code):
        self.assertEqual(
            resp.status_code, expected_code,
            "Query to geosaas finished with code {} but expected {}: {}".format(
                resp.status_code, expected_code, resp.text.encode("utf-8")))

    def assert_paths(self, resp, path_count):
        self.assert_code(resp, 200)
        resp = resp.json()
        self.assertEqual(len(resp), 2)
        self.assertTrue(resp["traffic_type"] in ["realtime", "forecast"])

        route = resp["route"]
        self.assertTrue(isinstance(route, dict))
        self.assertEqual(len(route), 1)

        legs = route["legs"]
        self.assertTrue(isinstance(legs, list))
        self.assertEqual(len(legs), path_count)

        for leg in legs:
            if leg["status"] != "OK":
                continue
            self._assert_steps(leg["steps"])

    def _assert_steps(self, steps):
        self.assertTrue(isinstance(steps, list))
        last_mode = None
        last_feature_class = None
        for step in steps:
            self.assertTrue(isinstance(step["length"], (float, int)))
            self.assertTrue(isinstance(step["duration"], (float, int)))
            self.assertTrue(isinstance(
                step["waiting_duration"], (float, int)))
            self.assertTrue(step["mode"] in ["driving", "walking", "transit"])

            if "feature_class" in step:
                self.assertTrue(isinstance(step["feature_class"], str))
                self._assert_feature_class(step["feature_class"])

            self.assertTrue(isinstance(step["polyline"], dict))
            self.assertEqual(len(step["polyline"]), 1)
            self._assert_points(step["polyline"]["points"])

            if last_mode is not None and last_feature_class is not None:
                self.assertTrue(
                    last_mode != step["mode"] or
                    last_feature_class != step.get("feature_class"))
            last_mode = step["mode"]
            last_feature_class = step.get("feature_class")

    def _assert_feature_class(self, feature_class):
        road_pattern = re.compile(r"road\.[0-9]+")
        transit_pattern = re.compile(r"transit\.bus\.[0-9]+")
        transit_v2_pattern = re.compile(r"transit\.\w+")
        self.assertTrue(road_pattern.match(feature_class)
                        or transit_pattern.match(feature_class)
                        or transit_v2_pattern.match(feature_class)
                        or "transit.subway" == feature_class)

    def _assert_points(self, points):
        self.assertTrue(isinstance(points, list))
        for point in points:
            self.assertTrue(isinstance(point, list))
            self.assertEqual(len(point), 2)
            lat = point[0]
            lon = point[1]
            self.assertTrue(isinstance(lat, (float, int)))
            self.assertTrue(isinstance(lon, (float, int)))
            self.assertTrue(lat >= -90)
            self.assertTrue(lat <= 90)
            self.assertTrue(lon >= -180)
            self.assertTrue(lon <= 180)


class TestRouterApiGeneric(TestRouterApi):
    def test_generic(self):
        resp = request(f"{SIMPLE_ONE_SEGMENT_QUERY}&mode=driving&avoid_tolls=true&departure_time={int(time.time())}")

        self.assert_paths(resp, 1)

    @unittest.skip("skipping, becuase /unistat is no longer present")
    def test_unistat(self):
        resp = request_absolute_path("/unistat")
        self.assert_code(resp, 200)

        resp = resp.json()
        signals = {x[0]: x[1] for x in resp}

        handle = urllib.parse.urlparse(ROUTER_API_URL).path
        self.assertIn(f"handle_{handle}_summ", signals)
