import http.client
import requests

from maps.garden.sandbox.osm_downloader.task import _extract_shipping_date_from_url


def test_official(requests_mock):
    requests_mock.head(
        "https://planet.openstreetmap.org/pbf/planet-latest.osm.pbf",
        headers={
            "location": "https://planet.openstreetmap.org/pbf/planet-211115.osm.pbf",
        },
        status_code=http.client.FOUND)

    requests_mock.head(
        "https://planet.openstreetmap.org/pbf/planet-211115.osm.pbf",
        headers={
            "location": "https://ftp5.gwdg.de/pub/misc/openstreetmap/planet.openstreetmap.org/pbf/planet-211115.osm.pbf",
        },
        status_code=http.client.FOUND)

    requests_mock.head(
        "https://ftp5.gwdg.de/pub/misc/openstreetmap/planet.openstreetmap.org/pbf/planet-211115.osm.pbf",
        headers={
            "Last-Modified": "Fri, 19 Nov 2021 21:03:41 GMT",
        })

    shipping_date = _extract_shipping_date_from_url("https://planet.openstreetmap.org/pbf/planet-latest.osm.pbf", requests.Session())
    assert shipping_date == "20211119"


def test_bbbike(requests_mock):
    requests_mock.head(
        "https://download.bbbike.org/osm/planet/planet-latest.osm.pbf",
        headers={
            "Last-Modified": "Sat, 20 Nov 2021 03:59:54 GMT",
        })

    shipping_date = _extract_shipping_date_from_url("https://download.bbbike.org/osm/planet/planet-latest.osm.pbf", requests.Session())
    assert shipping_date == "20211120"
