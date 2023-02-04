import base64
import copy
import http.client
import json
import typing

import requests
import uuid
from google.protobuf import json_format
from google.protobuf import message as proto_message

from tasklet.experimental.examples.proto import fake_ya_make_pb2

from tasklet.experimental.tests.common import models as test_models
from tasklet.experimental.tests.common import server_mock
from tasklet.experimental.tests.common import utils as test_utils

from tasklet.api.v2 import well_known_structures_pb2
from tasklet.api.v2 import tasklet_service_pb2
from tasklet.api.v2 import data_model_pb2


def _format_execution_json(data: dict) -> dict:
    data2 = copy.deepcopy(data)
    data2.setdefault("status", {}).setdefault("result", {})
    buf = data_model_pb2.Execution()
    json_format.ParseDict(data2, buf)
    return json_format.MessageToDict(buf, including_default_value_fields=True)


def _execution_to_json(msg: data_model_pb2.Execution) -> dict:
    return _format_execution_json(json_format.MessageToDict(msg))


def _run_execution(
    tasklet_server: server_mock.TaskletServer,
    tasklet: test_models.Tasklet,
    build: test_models.Build,
    label: test_models.Label,
    message_obj: proto_message.Message,
) -> dict:
    rv = requests.post(
        f"{tasklet_server.api}/executions",
        json={
            "namespace": tasklet.NS,
            "tasklet": tasklet.Name,
            "label": label.Name,
            "input": {
                "serialized_data": base64.b64encode(message_obj.SerializeToString()).decode()
            }
        }
    )

    test_utils.ensure_response_status(rv)
    execution_json = rv.json()["execution"]
    assert execution_json["meta"]["build_id"] == build.ID
    assert execution_json["meta"]["tasklet_id"] == tasklet.ID
    assert execution_json["status"]["status"] == "E_EXECUTION_STATUS_EXECUTING"
    return execution_json


def test_executions(tasklet_server: server_mock.TaskletServer, registered_schema: str):
    namespace = tasklet_server.create_ns("default")
    tasklet = tasklet_server.create_tasklet(namespace, "TLFoo", {"catalog": "/home/alximik"})
    build = tasklet_server.create_build(tasklet, registered_schema, None)
    label = tasklet_server.create_label(tasklet, "latest", build, {})

    sample_input = well_known_structures_pb2.GenericBinary(
        payload=b"foo-bar-baz",
    )
    execution_json = _run_execution(tasklet_server, tasklet, build, label, sample_input)

    execution2_rv = requests.post(
        f"{tasklet_server.api}/executions",
        data=json.dumps({
            "namespace": namespace.name,
            "tasklet": tasklet.Name,
            "label": label.Name,
            "input": {
                "serialized_data": base64.b64encode(sample_input.SerializeToString()).decode(),
            }
        }),
    )
    test_utils.ensure_response_status(execution2_rv)

    execution2_json = execution2_rv.json()["execution"]
    assert execution2_json["meta"]["build_id"] == build.ID
    assert execution2_json["meta"]["tasklet_id"] == tasklet.ID
    assert execution2_json["status"]["status"] == "E_EXECUTION_STATUS_EXECUTING"

    # GET execution
    rv = requests.get(
        f"{tasklet_server.api}/executions:getById", params={"id": execution_json['meta']['id']},
    )
    test_utils.ensure_response_status(rv)
    assert _format_execution_json(rv.json()["execution"]) == _format_execution_json(execution_json)

    for url, searchQuery in (
        (f"{tasklet_server.api}/executions:listByTasklet", {"namespace": namespace.name, "tasklet": tasklet.Name}),
        (f"{tasklet_server.api}/executions:listByBuild", {"build_id": build.ID}),
    ):
        rv = requests.get(url, params=searchQuery)
        test_utils.ensure_response_status(rv)

        execution_ids = set(x["meta"]["id"] for x in rv.json()["executions"])
        assert execution_ids == {execution_json["meta"]["id"], execution2_json["meta"]["id"]}
        assert all(x["meta"]["tasklet_id"] == tasklet.ID for x in rv.json()["executions"])
        assert all(x["meta"]["build_id"] == build.ID for x in rv.json()["executions"])

        next_page_query = copy.deepcopy(searchQuery)
        next_page_query.update({"token": rv.json()["token"]})
        rv = requests.get(url, params=next_page_query)
        test_utils.ensure_response_status(rv)
        assert len(rv.json().get("executions", [])) == 0


def test_executions_idempotent_create(tasklet_server: server_mock.TaskletServer, registered_schema: str):
    namespace = tasklet_server.create_ns("default")
    tasklet = tasklet_server.create_tasklet(namespace, "TLFoo", {"catalog": "/home/alximik"})
    build = tasklet_server.create_build(tasklet, registered_schema, None)
    label = tasklet_server.create_label(tasklet, "latest", build, {})

    sample_input = well_known_structures_pb2.GenericBinary(
        payload=b"foo-bar-baz",
    )
    reqId = str(uuid.uuid4())
    rv = requests.post(
        f"{tasklet_server.api}/executions",
        headers={
            "X-Request-ID": reqId,
        },
        json={
            "namespace": tasklet.NS,
            "tasklet": tasklet.Name,
            "label": label.Name,
            "input": {
                "serialized_data": base64.b64encode(sample_input.SerializeToString()).decode()
            }
        }
    )
    test_utils.ensure_response_status(rv)
    execution_one = rv.json()["execution"]
    rv = requests.post(
        f"{tasklet_server.api}/executions",
        headers={
            "X-Request-ID": reqId,
        },
        json={
            "namespace": tasklet.NS,
            "tasklet": tasklet.Name,
            "label": label.Name,
            "input": {
                "serialized_data": base64.b64encode(sample_input.SerializeToString()).decode()
            }
        }
    )
    test_utils.ensure_response_status(rv)
    execution_two = rv.json()["execution"]
    assert execution_one["meta"]["id"] == execution_two["meta"]["id"]
    assert execution_one == execution_two


def test_executions_abort(tasklet_server: server_mock.TaskletServer, registered_schema: str):
    namespace = tasklet_server.create_ns("default")
    tasklet = tasklet_server.create_tasklet(namespace, "TLFoo", {"catalog": "/home/alximik"})
    build = tasklet_server.create_build(tasklet, registered_schema, None)
    label = tasklet_server.create_label(tasklet, "latest", build, {})

    sample_input = well_known_structures_pb2.GenericBinary(
        payload=b"foo-bar-baz",
    )
    execution_id = tasklet_server.create_execution(tasklet, label, sample_input)

    rv = requests.post(
        f"{tasklet_server.api}/executions:abort",
        json={
            "id": execution_id,
            "reason": "ya peredumal!!!!",
        }
    )
    test_utils.ensure_response_status(rv)

    rv = requests.post(
        f"{tasklet_server.api}/executions:abort",
        json={
            "id": execution_id,
            "reason": "ostanovites!1111!!",
        }
    )
    test_utils.ensure_response_status(rv, http.client.BAD_REQUEST)
    assert "already aborted" in rv.text

    rv = requests.get(
        f"{tasklet_server.api}/executions:getById", params={"id": execution_id},
    )
    test_utils.ensure_response_status(rv)
    assert rv.json()["execution"]["status"]["abort_request"]["author"] == "fake_default_user"
    assert rv.json()["execution"]["status"]["abort_request"]["reason"] == "ya peredumal!!!!"


def test_executions_abort_acl(tasklet_server: server_mock.TaskletServer, registered_schema: str):
    namespace = tasklet_server.create_ns("default")
    tasklet = tasklet_server.create_tasklet(namespace, "TLFoo", {"catalog": "/home/alximik"})
    build = tasklet_server.create_build(tasklet, registered_schema, None)
    label = tasklet_server.create_label(tasklet, "latest", build, {})

    sample_input = well_known_structures_pb2.GenericBinary(
        payload=b"foo-bar-baz",
    )
    execution_id = tasklet_server.create_execution(tasklet, label, sample_input)
    rv = requests.post(
        f"{tasklet_server.api}/executions:abort",
        headers={"Grpc-Metadata-X-Test-User": "user_unknown"},
        json={
            "id": execution_id,
            "reason": "ya peredumal!!!!",
        }
    )
    test_utils.ensure_response_status(rv, http.client.FORBIDDEN)

    rv = requests.get(
        f"{tasklet_server.api}/executions:getById", params={"id": execution_id},
    )
    test_utils.ensure_response_status(rv)
    assert rv.json()["execution"]["status"].get("abort_request", None) is None


def _gen_test_data_bundle(
    tasklet_server: server_mock.TaskletServer,
    registered_schema: str,
) -> (
    data_model_pb2.NamespaceMeta,
    test_models.Tasklet,
    test_models.Build,
    test_models.Label,
    fake_ya_make_pb2.FakeYaMakeInput,
):
    namespace = tasklet_server.create_ns("default")
    tasklet = tasklet_server.create_tasklet(namespace, "TLFoo", {"catalog": "/home/alximik"})
    build = tasklet_server.create_build(tasklet, None, {
        "schema": {
            "simple_proto": {
                "schema_hash": registered_schema,
                "input_message": "tasklet_examples.FakeYaMakeInput",
                "output_message": "tasklet_examples.FakeYaMakeOutput",
            },
        },
    })
    label = tasklet_server.create_label(tasklet, "latest", build, {})

    typed_input = fake_ya_make_pb2.FakeYaMakeInput(
        branch="junk-branch",
        revision="junk-revision",
        arc_token=well_known_structures_pb2.SecretRef(
            id="sec1-XXXX",
            version="ver1-YYY",
            key="my_arc_token"
        )
    )
    return namespace, tasklet, build, label, typed_input


def test_third_party_input_in_execution_grpc(tasklet_server: server_mock.TaskletServer, registered_schema: str):
    namespace, tasklet, build, label, typed_input = _gen_test_data_bundle(tasklet_server, registered_schema)

    grpc_stub = tasklet_server.grpc_stub
    req = tasklet_service_pb2.ExecuteRequest(
        namespace=namespace.name,
        tasklet=tasklet.Name,
        label=label.Name,
        input=data_model_pb2.ExecutionInput(
            serialized_data=typed_input.SerializeToString(),
        )
    )

    resp: tasklet_service_pb2.ExecuteResponse = grpc_stub.Execute(req)
    execution_id = resp.execution.meta.id

    get_req = tasklet_service_pb2.GetExecutionRequest(id=execution_id)
    get_resp: tasklet_service_pb2.GetExecutionResponse = grpc_stub.GetExecution(get_req)

    assert _execution_to_json(resp.execution) == _execution_to_json(get_resp.execution)


def test_third_party_input_in_execution_json(tasklet_server: server_mock.TaskletServer, registered_schema: str):
    namespace, tasklet, build, label, typed_input = _gen_test_data_bundle(tasklet_server, registered_schema)
    execution_json = _run_execution(tasklet_server, tasklet, build, label, typed_input)

    # GET execution
    rv = requests.get(
        f"{tasklet_server.api}/executions:getById", params={"id": execution_json['meta']['id']},
    )
    test_utils.ensure_response_status(rv)
    execution_json["status"].setdefault("result", {})
    assert _format_execution_json(rv.json()["execution"]) == _format_execution_json(execution_json)


def test_list_executions_pagination(tasklet_server: server_mock.TaskletServer, registered_schema: str):
    namespaces = [tasklet_server.create_ns(f"ns{x}") for x in range(2)]
    tasklet_to_builds: dict[test_models.Tasklet, typing.List[test_models.Build]] = {}
    build_to_labels: dict[test_models.Build, test_models.Label] = {}

    for ns in namespaces:
        ns_tasklets = [tasklet_server.create_tasklet(ns, f"TL_t{x}", {}) for x in range(2)]
        for tl in ns_tasklets:
            builds = [tasklet_server.create_build(tl, registered_schema, None) for _ in range(2)]
            for build in builds:
                assert build not in build_to_labels
                build_to_labels[build] = tasklet_server.create_label(tl, f"build-{build.ID[-5:]}", build, {})
            assert tl not in tasklet_to_builds
            tasklet_to_builds[tl] = builds

    execution_to_parents: typing.Dict[str, typing.Tuple[test_models.Tasklet, test_models.Build]] = {}
    build_to_executions: typing.Dict[test_models.Build, typing.Set[str]] = {}
    step = 0
    total_execution_count = 0

    input_message = well_known_structures_pb2.GenericBinary(payload=b"foo-bar-baz")

    def spawn_executions(count, tl_: test_models.Tasklet, build_: test_models.Build):
        for _ in range(count):
            execution_id = tasklet_server.create_execution(
                tl_,
                build_to_labels[build_],
                input_message,
            )
            assert execution_id not in execution_to_parents
            execution_to_parents[execution_id] = (tl_, build_)
            assert execution_id not in build_to_executions[build_]
            build_to_executions[build_].add(execution_id)

    for tl, builds in tasklet_to_builds.items():
        for build in builds:
            assert build not in build_to_executions
            build_to_executions[build] = set()
            total_execution_count += 10 + step
            assert total_execution_count < 1000
            spawn_executions(10 + step, tl, build)

    # List executions by build
    for tl, builds in tasklet_to_builds.items():
        for build in builds:
            this_build_executions = set()
            token = None
            while True:
                rv = requests.get(
                    f"{tasklet_server.api}/executions:listByBuild",
                    params={"token": token, "build_id": build.ID}
                )
                test_utils.ensure_response_status(rv)
                token = rv.json()["token"]
                if not rv.json().get("executions", []):
                    break
                for execution in rv.json()["executions"]:
                    assert execution["meta"]["tasklet_id"] == build.TaskletID
                    assert execution["meta"]["build_id"] == build.ID
                    assert execution["meta"]["id"] not in this_build_executions
                    this_build_executions.add(execution["meta"]["id"])

            assert this_build_executions == build_to_executions[build]

    # List executions by build
    for tl, builds in tasklet_to_builds.items():
        this_tasklet_executions = set()
        token = None
        while True:
            rv = requests.get(
                f"{tasklet_server.api}/executions:listByTasklet",
                params={
                    "namespace": tl.NS,
                    "tasklet": tl.Name,
                    "token": token,
                },
            )
            test_utils.ensure_response_status(rv)
            token = rv.json()["token"]
            if not rv.json().get("executions", []):
                break
            for execution in rv.json()["executions"]:
                assert execution["meta"]["tasklet_id"] == tl.ID
                assert execution["meta"]["tasklet_id"] == execution_to_parents[execution["meta"]["id"]][0].ID
                assert execution["meta"]["build_id"] == execution_to_parents[execution["meta"]["id"]][1].ID
                assert execution["meta"]["id"] not in this_tasklet_executions
                this_tasklet_executions.add(execution["meta"]["id"])

        assert this_tasklet_executions == \
               set(execution_id for execution_id, parents in execution_to_parents.items() if tl == parents[0])
