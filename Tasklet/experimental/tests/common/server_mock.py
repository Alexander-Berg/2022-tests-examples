import copy
import typing
import dataclasses
import base64

import grpc
import requests
from google.protobuf import json_format
from google.protobuf import message as proto_message

from tasklet.api.v2 import data_model_pb2
from tasklet.api.v2 import schema_registry_service_pb2_grpc
from tasklet.api.v2 import tasklet_service_pb2_grpc

from . import models as test_models
from . import utils as test_utils


@dataclasses.dataclass(frozen=True)
class TaskletServer:
    http_port: int
    grpc_port: int

    @property
    def root(self):
        return f"http://localhost:{self.http_port}"

    @property
    def api(self):
        return f"http://localhost:{self.http_port}/v1"

    @property
    def grpc_address(self):
        return f"localhost:{self.grpc_port}"

    @property
    def grpc_stub(self) -> tasklet_service_pb2_grpc.TaskletServiceStub:
        channel = grpc.insecure_channel(self.grpc_address)
        return tasklet_service_pb2_grpc.TaskletServiceStub(channel)

    @property
    def grpc_schema_registry_stub(self) -> schema_registry_service_pb2_grpc.SchemaRegistryServiceStub:
        channel = grpc.insecure_channel(self.grpc_address)
        return schema_registry_service_pb2_grpc.SchemaRegistryServiceStub(channel)

    def create_ns(self, name: str, owner: str = "abc:123") -> data_model_pb2.NamespaceMeta:
        rv = requests.post(
            f"{self.api}/namespaces",
            json={
                "name": name,
                "account_id": owner,
            },
        )
        test_utils.ensure_response_status(rv)
        meta = rv.json()["namespace"]["meta"]

        namespace = data_model_pb2.NamespaceMeta()
        json_format.ParseDict(meta, namespace)
        return namespace

    def create_tasklet(
        self,
        ns: data_model_pb2.NamespaceMeta,
        name: str,
        spec: typing.Optional[typing.Dict],
        owner: str = "abc:123"
    ) -> test_models.Tasklet:

        new_spec = {
            "catalog": "/inferred/catalog"
        }
        if spec is not None:
            new_spec.update(spec)
        request = copy.deepcopy(new_spec)
        request["name"] = name
        request["namespace"] = ns.name
        request["account_id"] = owner
        rv = requests.post(
            f"{self.api}/tasklets",
            json=request,
        )

        test_utils.ensure_response_status(rv)
        meta = rv.json()["tasklet"]["meta"]
        return test_models.Tasklet(meta["id"], meta["name"], meta["namespace"])

    def create_build(
        self,
        tl: test_models.Tasklet,
        registered_schema: typing.Optional[str],
        spec: typing.Optional[typing.Dict],
    ) -> test_models.Build:

        new_spec = {
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
        if spec is not None:
            new_spec.update(spec)

        request = copy.deepcopy(new_spec)
        request["tasklet"] = tl.Name
        request["namespace"] = tl.NS

        rv = requests.post(
            f"{self.api}/builds",
            json=request,
        )
        test_utils.ensure_response_status(rv)
        meta = rv.json()["build"]["meta"]
        return test_models.Build.from_meta_obj(meta)

    def create_label(
        self, tl: test_models.Tasklet,
        name: str,
        build: typing.Optional[test_models.Build],
        spec: typing.Optional[typing.Dict],
    ) -> test_models.Label:

        request = copy.deepcopy(spec or {})
        request.update({
            "name": name,
            "namespace": tl.NS,
            "tasklet": tl.Name,
        }
        )

        if build is not None:
            request.update({"build_id": build.ID})

        rv = requests.post(
            f"{self.api}/labels",
            json=request,
        )
        test_utils.ensure_response_status(rv)
        label = rv.json()["label"]
        assert label["meta"]["name"] == name
        assert label["meta"]["tasklet_id"] == tl.ID
        if build is not None:
            assert label["spec"]["build_id"] == build.ID
        assert label["spec"].get("revision", 0) == 0
        return test_models.Label(
            ID=label["meta"]["id"], Name=label["meta"]["name"], Target=build.ID if build is not None else None
        )

    def create_execution(
        self,
        tasklet: test_models.Tasklet,
        label: typing.Optional[test_models.Label],
        message_obj: proto_message.Message,
    ) -> str:
        rv = requests.post(
            f"{self.api}/executions",
            json={
                "namespace": tasklet.NS,
                "tasklet": tasklet.Name,
                "label": label.Name if label is not None else "",
                "input": {
                    "serialized_data": base64.b64encode(message_obj.SerializeToString()).decode()
                }
            }
        )
        test_utils.ensure_response_status(rv)
        assert rv.json()["execution"]["spec"]["input"]["serialized_data"] == base64.b64encode(
            message_obj.SerializeToString()
        ).decode()
        return rv.json()["execution"]["meta"]["id"]
