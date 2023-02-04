import re
import json
import pytest

from collections import namedtuple
from datetime import timedelta
from functools import partial
from itertools import product

from httpretty import httprettified, HTTPretty
from httpretty.core import HTTPrettyRequest

from maps.geoq.hypotheses.lib.nmaps import (
    already_uploaded,
    check_coordinates,
    upload_hypothesis,
    upload_poi_hypothesis,
    SourceContextTypes,
    SuggestedActions,
)
from maps.geoq.hypotheses.lib.tvm import Destination


Point = namedtuple("Point", ["lon", "lat"])


INVALID_LON = [-180 - 1e-6, 180 + 1e-6]
INVALID_LAT = [-90 - 1e-6, 90 + 1e-6]

DEFAULT_TEST_POINT = Point(37.622582, 55.753497)

TEST_HOST = "http://test/feedback/tasks"
TEST_DESTINATION = Destination(TEST_HOST, "", 0)
TEST_SOURCE = "geoq_test"

TEST_DESCRIPTION = "[Island in the Sun](https://www.youtube.com/watch?v=erG5rgNYSdk)"
TEST_SOURCE_TYPE = "other"

TEST_POI_HOST = TEST_HOST + "/poi"
TEST_FT_ID = 123456
TEST_PERMALINK = 111111
TEST_CREATED_AT = "2020-02-02T12:34:56.123456000Z"

TEST_SOURCE_CONTEXT_DATA = {
    "permalink": TEST_PERMALINK,
    "hyp_lon": DEFAULT_TEST_POINT.lon,
    "hyp_lat": DEFAULT_TEST_POINT.lat,
    "pano_timestamp": 1600000000,
    "bearing": 30,
    "mds_url": "http://test/mds/url",
    "map_url": "http://test/map/url"
}

TEST_IMAGE_FEATURES = [
    {
        "id": "1110301804",
        "geometry": {
            "coordinates": [TEST_SOURCE_CONTEXT_DATA["hyp_lon"], TEST_SOURCE_CONTEXT_DATA["hyp_lat"]],
            "type": "Point"
        },
        "timestamp": "2020-09-13T12:26:40Z",
        "heading": TEST_SOURCE_CONTEXT_DATA["bearing"],
        "imageFull": {
            "width": 1000,
            "height": 1000,
            "url": TEST_SOURCE_CONTEXT_DATA["mds_url"]
        },
        "imagePreview": {
            "width": 150,
            "height": 150,
            "url": TEST_SOURCE_CONTEXT_DATA["mds_url"]
        }
    },
    {
        "id": "1045990020",
        "geometry": {
            "coordinates": [TEST_SOURCE_CONTEXT_DATA["hyp_lon"], TEST_SOURCE_CONTEXT_DATA["hyp_lat"]],
            "type": "Point"
        },
        "timestamp": "2020-09-13T12:26:40Z",
        "heading": TEST_SOURCE_CONTEXT_DATA["bearing"],
        "imageFull": {
            "width": 600,
            "height": 450,
            "url": TEST_SOURCE_CONTEXT_DATA["map_url"]
        },
        "imagePreview": {
            "width": 200,
            "height": 150,
            "url": TEST_SOURCE_CONTEXT_DATA["map_url"]
        }
    }
]


def test_check_coordinates():
    for lon, lat in zip(INVALID_LON, INVALID_LAT):
        with pytest.raises(ValueError):
            check_coordinates(lon, 0.0)
        with pytest.raises(ValueError):
            check_coordinates(0.0, lat)
        with pytest.raises(ValueError):
            check_coordinates(lon, lat)

    # no exception
    check_coordinates(0.0, 0.0)


def check_bbox(bbox_description: str, test_point: Point):
    bbox = bbox_description.split(",")
    assert len(bbox) == 4

    a_lon, a_lat, b_lon, b_lat = map(float, bbox)
    check_coordinates(a_lon, a_lat)
    check_coordinates(b_lon, b_lat)

    assert a_lon < b_lon
    assert a_lat < b_lat

    assert a_lon <= test_point.lon <= b_lon
    assert a_lat <= test_point.lat <= b_lat


def feedback_get_handler(
    request: HTTPrettyRequest,
    url: str,
    headers: dict,
    test_point: Point,
    has_tasks: bool,
    ft_id: int
):
    query = request.querystring

    check_bbox(query["bbox-geo"][0], test_point)

    source = query["source"][0]
    assert source == TEST_SOURCE

    response_tasks = []
    if has_tasks:
        response_tasks.append({"id": "1", "source": source, "createdAt": TEST_CREATED_AT})

    if ft_id:
        response_tasks.append({"id": "2", "source": source, "createdAt": TEST_CREATED_AT})
        if has_tasks:
            response_tasks.append({
                "id": "3",
                "source": source,
                "createdAt": TEST_CREATED_AT,
                "objectId": str(ft_id)
            })

    return (
        200,
        headers,
        json.dumps({
            "totalCount": len(response_tasks),
            "tasks": response_tasks,
            "hasMore": False
        }))


def feedback_post_handler(request: HTTPrettyRequest, url: str, headers: dict):
    payload = request.parse_request_body(request.body)

    assert payload["source"] == TEST_SOURCE
    assert payload["type"] == TEST_SOURCE_TYPE
    assert payload["description"] == TEST_DESCRIPTION
    assert payload["workflow"] == "task"
    assert payload["hidden"]

    point = payload["position"]["coordinates"]
    assert point[0] == DEFAULT_TEST_POINT.lon and point[1] == DEFAULT_TEST_POINT.lat

    return (
        200,
        headers,
        json.dumps({}))


def poi_feedback_post_handler(request: HTTPrettyRequest, url: str, headers: dict, source_context_type: str):
    payload = request.parse_request_body(request.body)

    assert payload["source"] == TEST_SOURCE
    assert payload["workflow"] == "task"
    assert payload["hidden"]

    point = payload["position"]["coordinates"]
    assert point[0] == DEFAULT_TEST_POINT.lon and point[1] == DEFAULT_TEST_POINT.lat

    assert payload["objectId"] == str(TEST_FT_ID)
    assert payload["suggestedAction"] == SuggestedActions.VerifyPosition

    if source_context_type == SourceContextTypes.Permalink:
        assert payload["sourceContext"] == {
            "type": "sprav-feed",
            "content": {
                "permalink": TEST_PERMALINK
            }
        }
    elif source_context_type == SourceContextTypes.Images:
        assert payload["sourceContext"] == {
            "type": "images",
            "content": {
                "imageFeatures": TEST_IMAGE_FEATURES
            }
        }
    else:
        assert False

    return (
        200,
        headers,
        json.dumps({}))


@httprettified()
def run_already_uploaded_test(test_point: Point, has_tasks: bool = True, ft_id: int = None, cooldown: timedelta = None):
    HTTPretty.register_uri(
        HTTPretty.GET,
        re.compile(f"{TEST_HOST}.*"),
        body=partial(feedback_get_handler, test_point=test_point, has_tasks=has_tasks, ft_id=ft_id))
    if cooldown and cooldown < timedelta(days=365):
        has_tasks = False
    assert has_tasks == already_uploaded(
        lon=test_point.lon, lat=test_point.lat,
        source=TEST_SOURCE,
        host=TEST_DESTINATION,
        eps=1e-2,
        ft_id=ft_id,
        cooldown=cooldown)


@httprettified()
def test_already_uploaded():
    for has_tasks in (True, False):
        run_already_uploaded_test(DEFAULT_TEST_POINT, has_tasks)


@httprettified()
def test_already_uploaded_by_ft_id():
    for has_tasks in (True, False):
        run_already_uploaded_test(DEFAULT_TEST_POINT, has_tasks, TEST_FT_ID)


@httprettified()
def test_already_uploaded_cooldown():
    for cooldown in (30, 30000):
        run_already_uploaded_test(DEFAULT_TEST_POINT, has_tasks=True, cooldown=timedelta(days=cooldown))


@httprettified()
def test_already_uploaded_corner_cases():
    lon_coordinates = [-179.999999, 179.999999]
    lat_coordinates = [-89.999999, 89.999999]
    for lon, lat in product(lon_coordinates, lat_coordinates):
        run_already_uploaded_test(Point(lon, lat))


@httprettified()
def test_upload_hypothesis():
    HTTPretty.register_uri(HTTPretty.POST, re.compile(f"{TEST_HOST}.*"), body=feedback_post_handler)

    upload_hypothesis(
        lon=DEFAULT_TEST_POINT.lon,
        lat=DEFAULT_TEST_POINT.lat,
        description=TEST_DESCRIPTION,
        host=TEST_DESTINATION,
        source=TEST_SOURCE,
        source_type=TEST_SOURCE_TYPE)


@httprettified()
def test_upload_poi_hypothesis():
    for source_context_type in (SourceContextTypes.Permalink, SourceContextTypes.Images):
        HTTPretty.register_uri(
            HTTPretty.POST,
            re.compile(f"{TEST_POI_HOST}.*"),
            body=partial(poi_feedback_post_handler, source_context_type=source_context_type))

        upload_poi_hypothesis(
            lon=DEFAULT_TEST_POINT.lon,
            lat=DEFAULT_TEST_POINT.lat,
            source=TEST_SOURCE,
            ft_id=TEST_FT_ID,
            suggested_action=SuggestedActions.VerifyPosition,
            source_context_type=source_context_type,
            source_context_data=TEST_SOURCE_CONTEXT_DATA,
            host=TEST_DESTINATION)

        HTTPretty.reset()
