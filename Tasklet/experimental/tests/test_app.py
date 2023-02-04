# -*- coding: utf-8 -*-
import http.client
import requests

from tasklet.experimental.tests.common import utils as test_utils


def test_root_handle(tasklet_server):
    rv = requests.get(tasklet_server.root)
    assert rv.status_code == http.client.NOT_FOUND


def test_swagger_handle(tasklet_server):
    rv = requests.get(f"{tasklet_server.root}/_swagger")
    test_utils.ensure_response_status(rv, http.client.OK)

    rv = requests.get(f"{tasklet_server.root}/_swagger/scheme.json")
    test_utils.ensure_response_status(rv, http.client.OK)
    assert "post" in rv.json()["paths"]["/v1/namespaces"]


def test_solomon_handle(tasklet_server):
    rv = requests.get(f"{tasklet_server.root}/_solomon")
    test_utils.ensure_response_status(rv, http.client.OK)
    assert "metrics" in rv.json()
