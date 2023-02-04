import requests
import httpretty
import pytest
import time

from urllib.parse import urljoin

from library.python import resource

from maps.geoq.hypotheses.lib.static_api import (
    HOST, API_VERSION
)

from maps.geoq.hypotheses.manoeuvres.prepare_dataset_2.lib import (
    maps_fetcher
)


TEST_RPS = 100

# https://youtu.be/KWtwIf-TSlo
TILE_DATA = resource.find('sus')

TILE_SIZE = (224, 224)
TILE_TYPE = "map"
TILE_CONTENT_TYPE = "image/png"
TILE_ZOOM = 18
TILE_COORDS = (0.0, 0.0)

TILE_URL = urljoin(HOST, API_VERSION) + (
    f'?l=map'
    f'&ll={TILE_COORDS[0]},{TILE_COORDS[1]}'
    '&z={zoom}'
    f'size={TILE_SIZE[0]},{TILE_SIZE[1]}'
)


class MockFetcher:
    def __init__(self):
        self.requests_count = 0
        self.first_request_time = None

    def __call__(self, point):
        if not self.first_request_time:
            self.first_request_time = time.time()

        self.requests_count += 1

        ellapsed_time = time.time() - self.first_request_time
        if ellapsed_time <= 1.0:
            assert self.requests_count <= TEST_RPS, \
                f"Too many requests. Limit reached in {ellapsed_time} second"
        else:
            self.first_request_time = time.time()
            self.requests_count = 1

        return TILE_DATA


def test_fetch_tiles():
    fetcher = MockFetcher()

    test_set = [(0.0, 0.0)] * (TEST_RPS * 2)

    fetch_iterator = maps_fetcher.fetch_tiles(fetcher, test_set, 0, TEST_RPS)
    for target_index, (recieved_index, tile) in enumerate(fetch_iterator):
        assert target_index == recieved_index
        assert tile == TILE_DATA


def test_fetch_tiles_bad_rps():
    fetcher = MockFetcher()

    test_set = [(0.0, 0.0)] * (TEST_RPS * 2)

    fetch_iterator = maps_fetcher.fetch_tiles(fetcher, test_set, 0, 0.0)
    with pytest.raises(AssertionError):
        list(fetch_iterator)


@httpretty.activate
def run_requests_test(responses):
    httpretty.register_uri(
        httpretty.GET, TILE_URL.format(zoom=TILE_ZOOM), responses=responses)

    fetcher = maps_fetcher.MapsFetcher(TILE_TYPE, TILE_ZOOM, TILE_SIZE)
    output_tile = fetcher(TILE_COORDS)

    assert output_tile == TILE_DATA


def test_ok_maps_fetcher():
    run_requests_test([
        httpretty.Response(
            body=TILE_DATA,
            status=requests.codes["ok"],
            content_type=TILE_CONTENT_TYPE)
    ])


@httpretty.activate
def test_fallback_zooms():
    for zoom in range(maps_fetcher.DEFAULT_ZOOM, maps_fetcher.MIN_FALLBACK_ZOOM, -1):
        httpretty.register_uri(
            httpretty.GET,
            TILE_URL.format(zoom=zoom),
            body=b"Not found!", status=requests.codes["not_found"])
    httpretty.register_uri(
        httpretty.GET,
        TILE_URL.format(zoom=maps_fetcher.MIN_FALLBACK_ZOOM),
        body=TILE_DATA, status=requests.codes["ok"], content_type=TILE_CONTENT_TYPE)

    fetcher = maps_fetcher.MapsFetcher(TILE_TYPE, TILE_ZOOM, TILE_SIZE)
    output_tile = fetcher(TILE_COORDS)

    assert output_tile == TILE_DATA


def run_http_errors_test(error_body, error_status_code):
    run_requests_test([
        httpretty.Response(
            body=error_body,
            status=error_status_code),
        httpretty.Response(
            body=TILE_DATA,
            status=requests.codes["ok"],
            content_type=TILE_CONTENT_TYPE)
    ])


def test_internal_error_maps_fetcher():
    run_http_errors_test(b"Internal error!", requests.codes["internal_server_error"])


def test_rate_limiter_maps_fetcher():
    run_http_errors_test(b"Too many requests!", requests.codes["too_many_requests"])


def test_not_found_maps_fetcher():
    run_http_errors_test(b"Not found!", requests.codes["not_found"])
