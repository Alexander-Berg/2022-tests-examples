import unittest
import time
import os
import jsonschema
from request_client import RequestClient, DEFAULT_APIKEY
from schema import SCHEMA


DISTANCE_MATRIX_API_URL = os.environ.get(
    "DISTANCE_MATRIX_API_URL",
    "https://prestable.api.routing.n.yandex-team.ru/v1.0.0/distancematrix")

HEADERS = {"Host": "api.routing.yandex.net"}

SIMPLE_ONE_TO_ONE_QUERY = "?origins=55.594416,37.606422&destinations=55.624017,37.603081"
MAX_RPS = 10


_request_client = RequestClient(MAX_RPS)


def request_without_apikey(query_url):
    return _request_client.request(DISTANCE_MATRIX_API_URL + query_url, headers=HEADERS)


def request(query_url, apikey=DEFAULT_APIKEY):
    return _request_client.request(DISTANCE_MATRIX_API_URL + query_url + "&apikey={}".format(apikey), headers=HEADERS)


def request_without_rps_control(query_url, apikey=DEFAULT_APIKEY):
    return _request_client._request(DISTANCE_MATRIX_API_URL + query_url + "&apikey={}".format(apikey), headers=HEADERS)


class TestDistanceMatrixApi(unittest.TestCase):
    def assert_code(self, resp, expected_code):
        self.assertEqual(
            resp.status_code, expected_code,
            "Query {} to geosaas finished with code {} but expected {}: {}".format(
                resp.url, resp.status_code, expected_code, resp.text.encode("utf-8")))
        self.assert_schema(resp)

    def assert_paths(self, resp, path_count):
        self.assert_code(resp, 200)
        data = resp.json()
        for row in data["rows"]:
            for element in row["elements"]:
                self.assertEqual(element["status"], "OK")
        self.assertEqual(sum(len(r["elements"]) for r in data["rows"]), path_count)

    def assert_schema(self, resp):
        jsonschema.validate(
            resp.json(),
            SCHEMA)


class TestDistanceMatrixApiGeneric(TestDistanceMatrixApi):
    def test_generic(self):
        resp = request("{}&mode=driving&avoid_tolls=true&departure_time={}".format(SIMPLE_ONE_TO_ONE_QUERY, int(time.time())))

        self.assertEqual(
            resp.status_code, 200,
            "Query {} to geosaas failed with code {}: {}".format(
                resp.url, resp.status_code, resp.text.encode("utf-8")))

        resp = resp.json()
        rows = resp["rows"]

        self.assertEqual(len(rows), 1)
        row = rows[0]
        elements = row["elements"]
        self.assertEqual(len(elements), 1)
        element = elements[0]

        self.assertEqual(element["status"], "OK")

        duration = element["duration"]
        duration_value = duration["value"]
        self.assertTrue(isinstance(duration_value, int))
        self.assertTrue(duration_value > 0)

        distance = element["distance"]
        distance_value = distance["value"]
        self.assertTrue(isinstance(distance_value, int))
        self.assertTrue(distance_value > 0)
