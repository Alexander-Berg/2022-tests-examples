# -*- coding: utf-8 -*-
import base64
import time

from google.protobuf import descriptor_pb2
from google.protobuf import struct_pb2

from tasklet.api.v2 import schema_registry_service_pb2
from tasklet.api.v2 import schema_registry_service_pb2_grpc

from tasklet.experimental.tests.common import server_mock

# noinspection SpellCheckingInspection
serialized_fds = """
Cp8CChZzdHJpbmdfY29udGFpbmVyLnByb3RvEjd0YXNrbGV0LmV4cGVyaW1lbnRhbC5yZWdpc3Ry
eV90ZXN0LnByaW1pdGl2ZV9jb250YWluZXJzIjQKD1N0cmluZ0NvbnRhaW5lchIhCgxzdHJpbmdf
dmFsdWUYASABKAlSC3N0cmluZ1ZhbHVlQo0BCkFydS55YW5kZXgudGFza2xldC5leHBlcmltZW50
YWwucmVnaXN0cnlfdGVzdC5wcmltaXRpdmVfY29udGFpbmVyc1pIYS55YW5kZXgtdGVhbS5ydS90
YXNrbGV0L2V4cGVyaW1lbnRhbC9yZWdpc3RyeV90ZXN0L3ByaW1pdGl2ZV9jb250YWluZXJzYgZw
cm90bzM=
"""

# /registry_test/containers_with_options$ protoc -o /dev/stdout container_with_options.proto  | base64
# noinspection SpellCheckingInspection
serialized_fds_with_options = """
CpkEChxjb250YWluZXJfd2l0aF9vcHRpb25zLnByb3RvEjp0YXNrbGV0LmV4cGVyaW1lbnRhbC5y
ZWdpc3RyeV90ZXN0LmNvbnRhaW5lcnNfd2l0aF9vcHRpb25zGiBnb29nbGUvcHJvdG9idWYvZGVz
Y3JpcHRvci5wcm90byJrChRDb250YWluZXJXaXRoT3B0aW9ucxIoCglpbnRfdmFsdWUYASABKAVC
C4i1GAGStRgDYWJjUghpbnRWYWx1ZRIpChBvcHRpb25sZXNzX3ZhbHVlGAIgASgJUg9vcHRpb25s
ZXNzVmFsdWU6RwoPY3VzdG9tX29wdGlvbl8xEh0uZ29vZ2xlLnByb3RvYnVmLkZpZWxkT3B0aW9u
cxjRhgMgASgFUg1jdXN0b21PcHRpb24xOkcKD2N1c3RvbV9vcHRpb25fMhIdLmdvb2dsZS5wcm90
b2J1Zi5GaWVsZE9wdGlvbnMY0oYDIAEoCVINY3VzdG9tT3B0aW9uMkKTAQpEcnUueWFuZGV4LnRh
c2tsZXQuZXhwZXJpbWVudGFsLnJlZ2lzdHJ5X3Rlc3QuY29udGFpbmVyc193aXRoX29wdGlvbnNa
S2EueWFuZGV4LXRlYW0ucnUvdGFza2xldC9leHBlcmltZW50YWwvcmVnaXN0cnlfdGVzdC9jb250
YWluZXJzX3dpdGhfb3B0aW9uc2IGcHJvdG8z
"""


def get_fds(mutation: int) -> descriptor_pb2.FileDescriptorSet:
    b = base64.standard_b64decode(serialized_fds)
    fds = descriptor_pb2.FileDescriptorSet()
    fds.MergeFromString(b)

    # NB: simple alter to shift md5
    fds.file[0].package += f".v{mutation}"
    return fds


def get_set_simple(api: schema_registry_service_pb2_grpc.SchemaRegistryServiceStub,
                   fds: descriptor_pb2.FileDescriptorSet):
    req = schema_registry_service_pb2.CreateSchemaRequest(
        schema=fds,
        annotations=None,
    )

    resp = api.CreateSchema(
        request=req,
        metadata=[("x-test-user", "boomer")],
    )  # type: schema_registry_service_pb2.CreateSchemaResponse

    assert resp.meta.user == "boomer"
    assert abs((resp.meta.timestamp - time.time_ns())) // 10 ** 9 < 60 * 60
    assert len(resp.annotations) == 0

    # get back
    get_req = schema_registry_service_pb2.GetSchemaRequest(
        hash=resp.hash,
    )
    get_resp = api.GetSchema(get_req)
    assert get_resp.schema == req.schema
    assert get_resp.meta.user == "boomer"


def double_create(api: schema_registry_service_pb2_grpc.SchemaRegistryServiceStub,
                  fds: descriptor_pb2.FileDescriptorSet):
    annotations = {
        "foo": ["bar", "baz"],
        "lol": "kek",
    }
    add_req_1 = schema_registry_service_pb2.CreateSchemaRequest(
        schema=fds,
        annotations=struct_pb2.Struct(),
    )
    add_req_1.annotations.update(annotations)

    add_resp_1 = api.CreateSchema(
        request=add_req_1,
        metadata=[("x-test-user", "boomer")],
    )  # type: schema_registry_service_pb2.CreateSchemaResponse
    assert add_resp_1.annotations == add_req_1.annotations

    # 2
    add_req_2 = schema_registry_service_pb2.CreateSchemaRequest(
        schema=fds,
        annotations=struct_pb2.Struct(),
    )
    add_req_2.annotations.update(annotations)
    add_req_2.annotations.update({"boo": "kaboom"})
    del add_req_2.annotations["foo"]

    time.sleep(2)
    add_resp_2 = api.CreateSchema(
        request=add_req_2,
        metadata=[("x-test-user", "the_korum")],
    )  # type: schema_registry_service_pb2.CreateSchemaResponse
    assert add_resp_1.hash == add_resp_2.hash
    assert add_resp_1.annotations == add_resp_2.annotations
    assert add_resp_1.meta == add_resp_2.meta

    # final get
    get_req = schema_registry_service_pb2.GetSchemaRequest(
        hash=add_resp_1.hash,
    )
    get_resp = api.GetSchema(get_req)  # type: schema_registry_service_pb2.GetSchemaResponse
    assert get_resp.schema == add_req_1.schema
    assert get_resp.meta == add_resp_1.meta
    assert get_resp.annotations == add_resp_1.annotations


def check_unknown_extensions(api: schema_registry_service_pb2_grpc.SchemaRegistryServiceStub):
    fds = descriptor_pb2.FileDescriptorSet()
    fds.MergeFromString(base64.standard_b64decode(serialized_fds_with_options))

    req = schema_registry_service_pb2.CreateSchemaRequest(
        schema=fds,
        annotations=None,
    )
    assert fds.SerializeToString() == base64.standard_b64decode(serialized_fds_with_options)
    resp = api.CreateSchema(
        request=req,
        metadata=[("x-test-user", "zooom")],
    )  # type: schema_registry_service_pb2.CreateSchemaResponse

    # get back
    get_req = schema_registry_service_pb2.GetSchemaRequest(
        hash=resp.hash,
    )
    get_resp = api.GetSchema(get_req)  # type: schema_registry_service_pb2.GetSchemaResponse
    got_schema = get_resp.schema.SerializeToString()
    original_schema = base64.standard_b64decode(serialized_fds_with_options)
    assert got_schema == original_schema


def test__grpc__schema_registry(tasklet_server: server_mock.TaskletServer):
    api = tasklet_server.grpc_schema_registry_stub
    for idx, checker in enumerate([get_set_simple, double_create]):
        checker(api, get_fds(idx + 1))
    check_unknown_extensions(api)
