# coding=utf-8
import copy
import http.client
import random
import requests

from typing import Dict, Set, List, Optional

from tasklet.experimental.tests.common import models as test_models
from tasklet.experimental.tests.common import server_mock
from tasklet.experimental.tests.common import utils as test_utils


def test_builds(tasklet_server: server_mock.TaskletServer, registered_schema: str):
    tl_to_builds: Dict[test_models.Tasklet, Set[test_models.Build]] = {}
    for ns in ["NSFoo", "NSBar"]:
        n = tasklet_server.create_ns(ns)
        for tl_name in ["TLBaz", "TLBunny"]:
            tl = tasklet_server.create_tasklet(n, tl_name, None)
            assert tl not in tl_to_builds
            tl_to_builds[tl] = set()

    build_ids = set()
    for n, tl in enumerate(sorted(tl_to_builds.keys())):
        for rev in range(1, n + 1):
            spec = {
                "description": f"Build for {tl.NS}/{tl.Name} rev {rev}",
            }
            build = tasklet_server.create_build(tl, registered_schema, spec)
            assert build.NS == tl.NS
            assert build.Tasklet == tl.Name
            assert build.TaskletID == tl.ID
            assert build.Revision == rev

            assert build.ID not in build_ids
            build_ids.add(build.ID)
            tl_to_builds[tl].add(build)

    # Get by Id

    for tl, builds in tl_to_builds.items():
        for build in builds:
            rv = requests.get(
                f"{tasklet_server.api}/builds:getById",
                params={
                    "build_id": build.ID,
                }
            )
            test_utils.ensure_response_status(rv)

            b = rv.json()["build"]
            assert b["meta"]["id"] == build.ID
            assert int(b["meta"].get("revision", 0)) == build.Revision
            assert b["meta"]["tasklet"] == tl.Name
            assert b["meta"]["tasklet_id"] == tl.ID
            assert b["meta"]["namespace"] == tl.NS
            assert b["spec"]["description"] == f"Build for {tl.NS}/{tl.Name} rev {build.Revision}"

    # List
    for tl, builds in tl_to_builds.items():
        rv = requests.get(
            f"{tasklet_server.api}/builds:listByTaskletName",
            params={
                "namespace": tl.NS,
                "tasklet": tl.Name,
                "limit": 100,
            },
        )
        test_utils.ensure_response_status(rv)
        restored_builds = set()
        for build_json in rv.json().get("builds", []):
            restored_builds.add((build_json["meta"]["id"], int(build_json["meta"].get("revision", "0"))))
        expected_builds = set()
        for b in builds:
            expected_builds.add((b.ID, b.Revision))
        assert restored_builds == expected_builds


def test_build_schemas(tasklet_server: server_mock.TaskletServer, registered_schema: str):
    ns = tasklet_server.create_ns("schema_test")
    tl = tasklet_server.create_tasklet(ns, "TLtarget", None)

    _ = tasklet_server.create_build(tl, registered_schema, None)

    build_spec = {
        "description": f"Build for {tl.NS}/{tl.Name}",
        "compute_resources": {
            "vcpu_limit": 1543,
            "memory_limit": 200 * 2 ** 20,
        },
        "payload": {
            "sandbox_resource_id": "1543",
        },
        "launch_spec": {
            "type": "binary",
        },
        "workspace": {
            "storage_class": "E_STORAGE_CLASS_SSD",
            "storage_size": "1000000000",
        },
        "schema": {
            "simple_proto": {
                "schema_hash": registered_schema,
                "input_message": "tasklet.api.v2.GenericBinary",
                "output_message": "tasklet.api.v2.GenericBinary",
            },
        },
    }

    bad_build_request = copy.deepcopy(build_spec)
    bad_build_request["schema"]["simple_proto"]["schema_hash"] = "not_found_hash_error"
    bad_build_request.update(
        {
            "namespace": tl.NS,
            "tasklet": tl.Name,
        }
    )
    rv = requests.post(
        f"{tasklet_server.api}/builds",
        json=bad_build_request,
    )

    def bad_hash_checker(resp: requests.Response):
        assert resp.status_code == http.client.BAD_REQUEST
        assert "IO schema not registered" in resp.text

    test_utils.ensure_response(rv, bad_hash_checker)


def test_builds_pagination(tasklet_server: server_mock.TaskletServer, registered_schema: str):
    ns = tasklet_server.create_ns("NSFoo")
    tl = tasklet_server.create_tasklet(ns, "TLBar", None)

    test_builds_count = 100
    rev_to_build_id: Dict[int, str] = {}
    for r in range(test_builds_count):
        build = tasklet_server.create_build(tl, registered_schema, None)
        assert build.Revision not in rev_to_build_id
        assert build.Revision == r + 1
        rev_to_build_id[build.Revision] = build.ID

    def get_builds(limit_: int, token_: Optional[int]) -> (int, List[test_models.Build]):
        params = {
            "namespace": tl.NS,
            "tasklet": tl.Name,
            "limit": limit_,
            "token": token_,
        }
        rv = requests.get(
            f"{tasklet_server.api}/builds:listByTaskletName",
            params=params,
        )
        test_utils.ensure_response_status(rv)
        next_token = int(rv.json()["token"])
        if token_ == 1:
            assert rv.json().get("builds", []) == []
            assert next_token == 1
            return next_token, []

        builds_: List[test_models.Build] = []

        for b_ in rv.json()["builds"]:
            builds_.append(test_models.Build.from_meta_obj(b_["meta"]))
        min_rev = min(x.Revision for x in builds_)
        assert min_rev == next_token
        assert limit_ >= len(builds_)
        if next_token > 1:
            assert limit_ == len(builds_)
        return next_token, builds_

    page_token, initial_builds = get_builds(13, None)
    seen_builds: Set[str] = set()
    for b in initial_builds:
        assert rev_to_build_id[b.Revision] == b.ID
        assert b.ID not in seen_builds
        seen_builds.add(b.ID)

    rng = random.Random()
    rng.seed(1235364)
    items_left = test_builds_count - 13

    while True:
        limit = rng.randint(1, 15)
        new_page_token, builds = get_builds(limit, page_token)
        if not builds:
            assert new_page_token == 1
            assert page_token == 1
            break

        assert new_page_token < page_token
        page_token = new_page_token
        items_left = items_left - len(builds)
        assert items_left >= 0
        for b in builds:
            assert rev_to_build_id[b.Revision] == b.ID
            assert b.ID not in seen_builds
            seen_builds.add(b.ID)

    assert seen_builds == set(rev_to_build_id.values())
