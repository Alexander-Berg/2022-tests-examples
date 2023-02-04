# coding=utf-8

import http.client
import requests

from tasklet.api.v2 import tasklet_service_pb2

from tasklet.experimental.tests.common import server_mock
from tasklet.experimental.tests.common import utils as test_utils


def test_namespaces(tasklet_server: server_mock.TaskletServer):
    invalid_get = requests.get(f"{tasklet_server.api}/namespaces/Foo")
    test_utils.ensure_response_status(invalid_get, http.client.NOT_FOUND)

    rv = requests.post(
        f"{tasklet_server.api}/namespaces",
        json={
            "name": "Foo",
            "account_id": "abc:123",
        },
    )

    def check_create_foo(rv_: requests.Response):
        test_utils.ensure_response_status(rv_, http.client.OK)
        post_result = rv_.json()
        assert post_result["namespace"]["meta"]["name"] == "Foo"
        assert post_result["namespace"]["meta"]["account_id"] == "abc:123"

    test_utils.ensure_response(rv, check_create_foo)

    rv2 = requests.get(f"{tasklet_server.api}/namespaces:getByName", params={"namespace": "Foo"})

    def check_get(x):
        assert rv.json() == x.json()

    test_utils.ensure_response(rv2, check_get)

    # Post for existing namespace
    rv = requests.post(
        f"{tasklet_server.api}/namespaces",
        json={
            "name": "Foo",
            "account_id": "abc:123",
        },
    )

    def check_duplicate_post(x):
        assert x.status_code == http.client.CONFLICT
        assert x.json() == {
            'code': 6,
            'error': 'Namespace exists. Namespace: "Foo"',
            'message': 'Namespace exists. Namespace: "Foo"',
        }

    test_utils.ensure_response(rv, check_duplicate_post)


def test_list_namespaces(tasklet_server: server_mock.TaskletServer):
    namespaces = {}
    for name, account in [("foo", "abc:123"), ("bar", "abc:123"), ("baz", "abc:1234")]:
        rv = requests.post(
            f"{tasklet_server.api}/namespaces",
            json={
                "name": name,
                "account_id": account,
            },
        )
        test_utils.ensure_response_status(rv, http.client.OK)
        namespaces[name] = rv.json()["namespace"]

    # NB: get all
    rv = requests.get(f"{tasklet_server.api}/namespaces:list")

    def check_get_all_namespaces(xx: requests.Response):
        test_utils.ensure_response_status(xx, http.client.OK)
        all_ns = xx.json()["namespaces"]

        assert len(all_ns) == 3
        assert sorted([x["meta"]["name"] for x in all_ns]) == sorted(namespaces.keys())
        for x in all_ns:
            assert x == namespaces[x["meta"]["name"]]

    test_utils.ensure_response(rv, check_get_all_namespaces)

    # NB: get by owner
    rv = requests.get(f"{tasklet_server.api}/namespaces:list", params={"owner": "abc:123"})

    def check_get_by_owner(xx: requests.Response):
        test_utils.ensure_response_status(xx, http.client.OK)
        abc_123 = xx.json()["namespaces"]

        assert len(abc_123) == 2
        assert sorted([x["meta"]["name"] for x in abc_123]) == sorted(["foo", "bar"])
        for i in abc_123:
            assert i == namespaces[i["meta"]["name"]]

    test_utils.ensure_response(rv, check_get_by_owner)


def test__grpc__namespaces(tasklet_server: server_mock.TaskletServer):
    api = tasklet_server.grpc_stub

    request = tasklet_service_pb2.CreateNamespaceRequest(
        name="namespace-test",
        account_id="abc:469",
    )

    resp = api.CreateNamespace(request)
    ns_id = resp.namespace.meta.id
    assert ns_id

    resp = api.GetNamespace(tasklet_service_pb2.GetNamespaceRequest(namespace=request.name))
    assert ns_id == resp.namespace.meta.id
    assert request.name == resp.namespace.meta.name
    assert request.account_id == resp.namespace.meta.account_id
