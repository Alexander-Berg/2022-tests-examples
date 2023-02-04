# coding=utf-8
import http.client
import typing

import requests

from tasklet.experimental.tests.common import models as test_models
from tasklet.experimental.tests.common import server_mock
from tasklet.experimental.tests.common import utils as test_utils


def test_labels(tasklet_server: server_mock.TaskletServer, registered_schema: str):
    tl_to_builds: typing.Dict[test_models.Tasklet, typing.Set[test_models.Build]] = {}

    for ns_name in ["NS_Foo", "NS_Bar"]:
        ns = tasklet_server.create_ns(ns_name, "abc:123")
        for tl_name in ["TL_Baz", "TL_Bunny"]:
            tl = tasklet_server.create_tasklet(
                ns=ns,
                name=tl_name,
                spec={"catalog": "/home/alximik"},
                owner="abc:123",
            )
            assert tl not in tl_to_builds
            tl_to_builds[tl] = set()

    # NB: at least 2 builds for each tasklet
    for n, tl in enumerate(sorted(tl_to_builds.keys())):
        for rev in range(1, n + 3):
            build = tasklet_server.create_build(
                tl,
                registered_schema,
                {"description": f"Build for {tl.NS}/{tl.Name} rev {rev}"},
            )
            tl_to_builds[tl].add(build)

    tl_default_labels = {}
    for tl in tl_to_builds.keys():
        target_build = sorted(tl_to_builds[tl])[0].ID
        rv = requests.post(
            f"{tasklet_server.api}/labels",
            json={
                "name": "default",
                "namespace": tl.NS,
                "tasklet": tl.Name,
                "build_id": target_build,
            },
        )
        test_utils.ensure_response_status(rv)
        label = rv.json()["label"]
        assert label["meta"]["name"] == "default"
        assert label["meta"]["tasklet_id"] == tl.ID
        assert label["spec"]["build_id"] == target_build
        assert label["spec"].get("revision", 0) == 0
        tl_default_labels[tl] = test_models.Label(ID=label["meta"]["id"], Name="default", Target=target_build)

    # Bad get
    tl_first = next(iter(tl_to_builds.keys()))
    rv = requests.get(
        f"{tasklet_server.api}/labels:getByName",
        params={
            "label": "invalid",
            "tasklet": tl_first.Name,
            "namespace": tl_first.NS,
        },
    )
    test_utils.ensure_response_status(rv, http.client.NOT_FOUND)

    # add extra label
    target_build = sorted(tl_to_builds[tl_first])[1].ID
    rv = requests.post(
        f"{tasklet_server.api}/labels",
        json={
            "name": "extra",
            "namespace": tl_first.NS,
            "tasklet": tl_first.Name,
            "build_id": target_build,
        },
    )
    test_utils.ensure_response_status(rv)
    label = rv.json()["label"]
    assert label["meta"]["name"] == "extra"
    assert label["meta"]["tasklet"] == tl_first.Name
    assert label["meta"]["tasklet_id"] == tl_first.ID
    assert label["spec"]["build_id"] == target_build
    assert label["spec"].get("revision", 0) == 0
    extra_label = test_models.Label(ID=label["meta"]["id"], Name="extra", Target=target_build)

    all_labels = requests.get(
        f"{tasklet_server.api}/labels:listByTaskletName",
        params={
            "namespace": tl_first.NS,
            "tasklet": tl_first.Name,
        },
    )
    test_utils.ensure_response_status(all_labels)
    all_labels = all_labels.json()["labels"]

    assert {extra_label, tl_default_labels[tl_first]} == set([
        test_models.Label(ID=x["meta"]["id"], Name=x["meta"]["name"], Target=x["spec"]["build_id"])
        for x in all_labels
    ]
    )
    assert all(x["meta"]["tasklet"] == tl_first.Name for x in all_labels)
    assert all(x["meta"]["tasklet_id"] == tl_first.ID for x in all_labels)

    # Labels update & move

    for label in [extra_label, tl_default_labels[tl_first]]:
        # Update label
        rv = requests.put(
            f"{tasklet_server.api}/labels:updateByName",
            json={
                "label": label.Name,
                "tasklet": tl_first.Name,
                "namespace": tl_first.NS,
                "expected_revision": 0,
                "build_id": sorted(tl_to_builds[tl_first])[0].ID,
            },
        )
        test_utils.ensure_response_status(rv)
        updated_label = rv.json()["label"]
        assert updated_label["meta"]["id"] == label.ID
        assert updated_label["meta"]["name"] == label.Name
        assert updated_label["meta"]["namespace"] == tl_first.NS
        assert updated_label["meta"]["tasklet"] == tl_first.Name
        assert updated_label["meta"]["tasklet_id"] == tl_first.ID
        assert updated_label["spec"]["build_id"] == sorted(tl_to_builds[tl_first])[0].ID
        assert updated_label["spec"]["revision"] == 1

        # Update label (bad revision)
        rv = requests.put(
            f"{tasklet_server.api}/labels:updateByName",
            json={
                "namespace": tl_first.NS,
                "tasklet": tl_first.Name,
                "label": label.Name,
                "expected_revision": 99,
                "build_id": sorted(tl_to_builds[tl_first])[0].ID,
            },
        )
        assert rv.status_code == http.client.CONFLICT
        assert "Bad label revision" in rv.json().get("message", "")

        # Update label #2
        rv = requests.put(
            f"{tasklet_server.api}/labels:updateByName",
            json={
                "namespace": tl_first.NS,
                "tasklet": tl_first.Name,
                "label": label.Name,
                "expected_revision": 1,
                "build_id": sorted(tl_to_builds[tl_first])[0].ID,
            },
        )
        test_utils.ensure_response_status(rv)
        assert rv.json()["label"]["spec"]["revision"] == 2
        assert rv.json()["label"]["spec"]["build_id"] == sorted(tl_to_builds[tl_first])[0].ID

        # Move label

        rv = requests.post(
            f"{tasklet_server.api}/labels:moveByName",
            json={
                "namespace": tl_first.NS,
                "tasklet": tl_first.Name,
                "label": label.Name,
                "old_build_id": sorted(tl_to_builds[tl_first])[0].ID,
                "new_build_id": sorted(tl_to_builds[tl_first])[1].ID,
            },
        )
        test_utils.ensure_response_status(rv)
        assert rv.json()["label"]["spec"]["revision"] == 3
        assert rv.json()["label"]["spec"]["build_id"] == sorted(tl_to_builds[tl_first])[1].ID

        rv = requests.get(
            f"{tasklet_server.api}/labels:getByName",
            params={
                "label": label.Name,
                "tasklet": tl_first.Name,
                "namespace": tl_first.NS,
            },
        )
        test_utils.ensure_response_status(rv)

        assert rv.json()["label"]["meta"]["id"] == label.ID
        assert rv.json()["label"]["meta"]["name"] == label.Name
        assert rv.json()["label"]["meta"]["tasklet_id"] == tl_first.ID
        assert rv.json()["label"]["meta"]["tasklet"] == tl_first.Name
        assert rv.json()["label"]["meta"]["namespace"] == tl_first.NS

        assert rv.json()["label"]["spec"]["revision"] == 3
        assert rv.json()["label"]["spec"]["build_id"] == sorted(tl_to_builds[tl_first])[1].ID
