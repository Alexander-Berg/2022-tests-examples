import http.client
import logging

from maps.pylibs.utils.lib.common import flatten

logger = logging.getLogger("garden.server")


def _traverse(garden_client, url, visited_urls):
    if url in visited_urls:
        return
    if not url.startswith("http://localhost"):
        logger.warning("Skip url {}".format(url))
        return
    visited_urls.add(url)

    response = garden_client.get(url)
    assert response.status_code == http.client.OK

    # try traverse only json results
    if response.headers["content-type"] != "application/json":
        return

    for doc in flatten(response.get_json()):
        for key, item in doc.items():
            if key.endswith("_url"):
                _traverse(garden_client, item, visited_urls)
            else:
                for it in flatten(item):
                    if isinstance(it, str):
                        _traverse(garden_client, it, visited_urls)


def test_traverse(garden_client):
    _traverse(garden_client, "http://localhost/", set())


def test_redirections(garden_client):
    URLS_TO_TEST = [
        "/modules/denormalization",
        "/storage",
        "/modules/geocoder_indexer/builds",
        "/builds"
    ]
    for url in URLS_TO_TEST:
        response = garden_client.get(url)
        assert response.status_code == http.client.PERMANENT_REDIRECT
