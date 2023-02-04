# -*- coding: utf-8 -*-
import http.client

import requests

from tasklet.experimental.tests.common import utils as test_utils


def test_tasklets(tasklet_server):
    rv = requests.post(
        f"{tasklet_server.api}/namespaces",
        json={
            "name": "Foo",
            "account_id": "abc:123",
        },
    )

    test_utils.ensure_response_status(rv, http.client.OK)

    create_response = requests.post(
        f"{tasklet_server.api}/tasklets",
        json={
            "name": "TlFoo",
            "namespace": "Foo",
            "account_id": "abc:123",

            "catalog": "/home/alximik",
            "source_info": {"spec_path": "/path/to/t.yaml", "repository_url": "a.yandex-team.ru/arcadia"},
        },
    )
    test_utils.ensure_response_status(create_response, http.client.OK)

    # recreate tasklet
    rv = requests.post(
        f"{tasklet_server.api}/tasklets",
        json={
            "name": "TlFoo",
            "namespace": "Foo",
            "account_id": "abc:123",

            "catalog": "/home/alximik",
        },
    )
    test_utils.ensure_response_status(rv, http.client.CONFLICT)

    # invalid namespace
    rv = requests.post(
        f"{tasklet_server.api}/tasklets",
        json={
            "name": "Tl_Bar",
            "namespace": "Foo_INVALID",
            "account_id": "abc:123",
            "catalog": "/home/alximik"
        },
    )

    def check_duplicate_create(xx: requests.Response):
        assert xx.status_code == http.client.BAD_REQUEST
        assert xx.json()["code"] == 9
        assert "not found" in xx.json()["message"]

    test_utils.ensure_response(rv, check_duplicate_create)

    rv = requests.get(
        f"{tasklet_server.api}/tasklets:getByName",
        params={
            "namespace": "Foo",
            "tasklet": "TlFoo",
        },
    )

    def check_create_tasklet(xx: requests.Response):
        test_utils.ensure_response_status(xx, http.client.OK)
        assert xx.json()["tasklet"]["meta"] == create_response.json()["tasklet"]["meta"]
        assert xx.json()["tasklet"]["spec"]["source_info"] == create_response.json()["tasklet"]["spec"]["source_info"]

    test_utils.ensure_response(rv, check_create_tasklet)
    tasklet = rv.json()["tasklet"]

    # Update tasklet

    rv = requests.put(
        f"{tasklet_server.api}/tasklets:updateByName",
        json={
            "tasklet": "TlFoo",
            "namespace": "Foo",
            "expected_revision": tasklet.get("spec", {}).get("revision", 0),
            "catalog": "/junk/alximik",
        },
    )

    def check_update_tasklet(xx: requests.Response):
        test_utils.ensure_response_status(xx, http.client.OK)
        tasklet_ = xx.json()["tasklet"]
        assert tasklet_["spec"]["revision"] == 1
        assert tasklet_["spec"]["catalog"] == "/junk/alximik"
        assert tasklet_["meta"] == tasklet_["meta"]

    test_utils.ensure_response(rv, check_update_tasklet)

    rv1 = requests.get(
        f"{tasklet_server.api}/tasklets:getByName",
        params={
            "namespace": "Foo",
            "tasklet": "TlFoo",
        }
    )

    test_utils.ensure_response_status(rv1, http.client.OK)
    assert rv.json()["tasklet"] == rv1.json()["tasklet"]

    # update 2
    rv = requests.put(
        f"{tasklet_server.api}/tasklets:updateByName",
        json={
            "tasklet": "TlFoo",
            "namespace": "Foo",
            "expected_revision": 99,
            "catalog": "/junk/alonger",
        },
    )

    test_utils.ensure_response_status(rv, http.client.CONFLICT)

    # List
    rv = requests.post(
        f"{tasklet_server.api}/tasklets",
        json={
            "name": "TlBar",
            "namespace": "Foo",
            "account_id": "abc:124",

            "catalog": "/prod/taxi",
        },
    )

    test_utils.ensure_response_status(rv, http.client.OK)

    tasklets = [
        requests.get(
            f"{tasklet_server.api}/tasklets:getByName",
            params={
                "namespace": "Foo",
                "tasklet": "TlFoo",
            }
        ).json()["tasklet"],
        requests.get(
            f"{tasklet_server.api}/tasklets:getByName",
            params={
                "namespace": "Foo",
                "tasklet": "TlBar",
            }
        ).json()["tasklet"],
    ]

    list_rv = requests.get(f"{tasklet_server.api}/tasklets:list", params={"namespace": "Foo"})
    test_utils.ensure_response_status(list_rv, http.client.OK)

    assert sorted(list_rv.json()["tasklets"], key=lambda x: x["meta"]["id"]) == \
           sorted(tasklets, key=lambda x: x["meta"]["id"])

    rv_invalid = requests.get(
        f"{tasklet_server.api}/tasklets:getByName",
        params={
            "namespace": "Foo_INVALID",
            "tasklet": "TlFoo",
        }
    )
    assert rv_invalid.status_code == http.client.NOT_FOUND


def _user_subject(username: str) -> dict:
    return {
        "name": username,
        "source": "E_SOURCE_USER",
    }
