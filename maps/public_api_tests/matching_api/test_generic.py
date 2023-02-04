import unittest
import time
import os
import jsonschema

from request_client import RequestClient, DEFAULT_APIKEY


MATCHING_API_URL = os.environ.get(
    "MATCHING_API_URL",
    "https://prestable.api.routing.n.yandex-team.ru/v1/match")

SIMPLE_QUERY = "?points=55.7538127,37.5755189|55.7548127,37.5765189"
FOUR_POINTS_QUERY = "?points=55.7538127,37.5755189|55.7548127,37.5765189|55.756434,37.577977|55.758152,37.578663"

MAX_RPS = 10

_request_client = RequestClient(MAX_RPS)


SCHEMA = {
    "$schema": "http://json-schema.org/draft-04/schema#",
    "definitions": {
        "position": {
            "type": "array",
            "items": [
                {
                    "type": "number",
                    "description": "Latitude"
                },
                {
                    "type": "number",
                    "description": "Longitude"
                },
                {
                    "type": ["null", "integer"],
                    "description": "Index of request a point, if point is a projection"
                },
                {
                    "type": "number",
                    "description": "Approximated timestamp"
                }
            ],
            "additionalItems": False,
        },
        "position_with_time": {
            "allOf": [
                {"$ref": "#/definitions/position"},
                {
                    "type": "array",
                    "minItems": 4,
                    "maxItems": 4
                }
            ]
        },
        "position_wo_time": {
            "allOf": [
                {"$ref": "#/definitions/position"},
                {
                    "type": "array",
                    "minItems": 3,
                    "maxItems": 3
                }
            ]
        },
        "points_with_time": {
            "type": "array",
            "items": {"$ref": "#/definitions/position_with_time"}
        },
        "points_wo_time": {
            "type": "array",
            "items": {"$ref": "#/definitions/position_wo_time"}
        },
        "match_response": {
            "type": "object",
            "description": "Matched track response",
            "properties": {
                "points": {
                    "description": "Track snapped to the road graph",
                    "oneOf": [
                        {"$ref": "#/definitions/points_with_time"},
                        {"$ref": "#/definitions/points_wo_time"}
                    ]
                }
            },
            "required": ["points"],
            "additionalProperties": False
        },
        "error_response": {
            "type": "object",
            "description": "Error response",
            "properties": {
                "errors": {
                    "type": "array",
                    "items": {"type": "string"},
                    "minItems": 1,
                    "additionalItems": False
                }
            },
            "required": ["errors"],
            "additionalProperties": False
        }
    },
    "type": "object",
    "oneOf": [
        {"$ref": "#/definitions/match_response"},
        {"$ref": "#/definitions/error_response"}
    ]

}


def request_without_apikey(query_url):
    return _request_client.request(MATCHING_API_URL + query_url)


def request(query_url, apikey=DEFAULT_APIKEY):
    return _request_client.request(MATCHING_API_URL + query_url + "&apikey={}".format(apikey))


class TestMatchApi(unittest.TestCase):
    def assert_code(self, resp, expected_code):
        self.assertEqual(
            resp.status_code, expected_code,
            "Query {url} finished with code {code} but expected {expected_code}: {msg}".format(
                url=resp.url,
                code=resp.status_code,
                expected_code=expected_code,
                msg=resp.text.encode("utf-8")))

    def assert_schema(self, resp):
        jsonschema.validate(
            resp.json(),
            SCHEMA)

    def check_response(self, resp, expected_code):
        self.assert_code(resp, expected_code)
        self.assert_schema(resp)


class TestMatchApiGeneric(TestMatchApi):
    def test_generic(self):
        now = int(time.time())
        resp = request(
            "{}&accuracies=100|500&timestamps={}|{}".format(
                SIMPLE_QUERY,
                now - 10,
                now)
            )

        self.check_response(resp, 200)

        resp = resp.json()
        self.assertIsInstance(resp, dict)
        self.assertIn("points", resp)
        self.assertEqual(len(resp.keys()), 1)
        self.assertIsInstance(resp["points"], list)

    def test_accuracies_length(self):
        resp = request(SIMPLE_QUERY + "&accuracies=100")
        self.check_response(resp, 200)

        resp = request(SIMPLE_QUERY + "&accuracies=100|200")
        self.check_response(resp, 200)

        resp = request(SIMPLE_QUERY + "&accuracies=100|200|200")
        self.check_response(resp, 400)

        resp = request(FOUR_POINTS_QUERY + "&accuracies=100|200|200")
        self.check_response(resp, 400)

        resp = request(FOUR_POINTS_QUERY + "&accuracies=100|200|200|100")
        self.check_response(resp, 200)

    def test_accuracies_type(self):
        resp = request(SIMPLE_QUERY + "&accuracies=100.01")
        self.check_response(resp, 200)

        resp = request(SIMPLE_QUERY + "&accuracies=100|200.5")
        self.check_response(resp, 200)

        resp = request(SIMPLE_QUERY + "&accuracies=xxx")
        self.check_response(resp, 400)

        resp = request(SIMPLE_QUERY + "&accuracies=xxx|yyy")
        self.check_response(resp, 400)

    def test_timestamps_length(self):
        resp = request(SIMPLE_QUERY + "&timestamps=1519390187")
        self.check_response(resp, 400)

        resp = request(SIMPLE_QUERY + "&timestamps=1519390187|1519390197")
        self.check_response(resp, 200)

        resp = request(SIMPLE_QUERY + "&timestamps=1519390177|1519390187|1519390197")
        self.check_response(resp, 400)

        resp = request(FOUR_POINTS_QUERY + "&timestamps=1519390177|1519390187|1519390197")
        self.check_response(resp, 400)

        resp = request(FOUR_POINTS_QUERY + "&timestamps=1519390167|1519390177|1519390187|1519390197""")
        self.check_response(resp, 200)

    def test_timestamp_type(self):
        resp = request(SIMPLE_QUERY + "&timestamps=1519390187|1519390197")
        self.check_response(resp, 200)

        resp = request(SIMPLE_QUERY + "&timestamps=1519390187.5|1519390197")
        self.check_response(resp, 400)

        resp = request(SIMPLE_QUERY + "&timestamps=yyy|xxx")
        self.check_response(resp, 400)

    def test_timestamp_onoff(self):
        resp = request(SIMPLE_QUERY + "&timestamps=1519390187|1519390197")
        self.check_response(resp, 200)
        points = resp.json()["points"]
        for point in points:
            self.assertEqual(len(point), 4)
            # Sanity check for timestamps
            self.assertGreater(point[3], 1500000000)

        resp = request(SIMPLE_QUERY)
        self.check_response(resp, 200)
        points = resp.json()["points"]
        for point in points:
            self.assertEqual(len(point), 3)

    def test_original_index(self):
        # Both points provided are close to the road, so we expect projection
        # of both in the response
        resp = request(SIMPLE_QUERY)
        self.assert_code(resp, 200)
        points = resp.json()["points"]
        indexes = []
        for point in points:
            if point[2] is not None:
                indexes.append(point[2])
        self.assertListEqual(indexes, [0, 1])

    def test_mode_values(self):
        resp = request(SIMPLE_QUERY + "&mode=driving")
        self.check_response(resp, 200)

        resp = request(SIMPLE_QUERY + "&mode=walking")
        self.check_response(resp, 200)

        resp = request(SIMPLE_QUERY + "&mode=pedestrian")
        self.check_response(resp, 400)

    @unittest.expectedFailure  # EXTDATA-1604
    def test_mode_values_transit(self):
        resp = request(SIMPLE_QUERY + "&mode=transit")
        self.check_response(resp, 200)
