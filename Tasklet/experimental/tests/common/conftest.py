import logging
import os
import socket
import time
import yaml

import jinja2
import pytest
from google.protobuf import descriptor_pb2
from google.protobuf import struct_pb2

from yatest import common as yatest
from yatest.common import network as yatest_network
import ydb

from tasklet.api.v2 import data_model_pb2
from tasklet.api.v2 import schema_registry_service_pb2

from . import models
from . import server_mock

log = logging.getLogger(__name__)


@pytest.fixture(scope="session")
def port_manager():
    pm = yatest_network.PortManager()
    try:
        yield pm
    finally:
        pm.release()


CONFIG_TEMPLATE = """
proxy:
  port: {{http_port}}
apiserver:
  port: {{grpc_port}}
  expose_error: true
  middleware:
    auth: false
    log_requests: true
    log_requests_fancy: true
handler: {}
staff:
  enabled: false
db:
  backend: ydb
  ydb:
      endpoint: "{{ydb_endpoint}}"
      database: "{{ydb_database}}"
      folder: "test"
      token_path: "{{ydb_token_path}}"
sandbox:
  host: "localhost:9999"
loggers:
  log_root: "{{logs_root}}"
  loggers:
    core:
      level: debug
      format: console
      paths:
        - "core.log"
    proxy:
      level: debug
      format: console
      paths:
        - "proxy.log"
    grpc:
      level: debug
      format: console
      paths:
        - "grpc.log"
processor:
  enabled: false
yt_config:
  cluster: "localhost"
  token_path: "~/.yt/token"
  resource_cache: "//tmp/sbx_dev"
"""


def _make_server_config(http_port: int, grpc_port: int) -> str:
    ydb_token_path = yatest.output_path("ydb-oauth-token")
    with open(ydb_token_path, "w") as token_file:
        token_file.write("test_secret_token")

    t = jinja2.Template(CONFIG_TEMPLATE)
    config_body = t.render(
        http_port=http_port,
        grpc_port=grpc_port,
        logs_root=yatest.test_output_path(),
        ydb_endpoint=os.getenv("YDB_ENDPOINT"),
        ydb_database="/" + os.getenv("YDB_DATABASE"),
        ydb_token_path=ydb_token_path,
    )

    conf = yaml.safe_load(config_body)
    assert conf["db"]["backend"] == "ydb"
    ydb_config = conf["db"]["ydb"]

    driver = ydb.Driver(ydb.DriverConfig(ydb_config["endpoint"], ydb_config["database"]))
    driver.wait()

    root_folder = os.path.join(ydb_config["database"], ydb_config["folder"])
    try:
        ydb_scheme = driver.scheme_client.describe_path(root_folder)
        assert ydb_scheme.is_directory()
    except ydb.SchemeError:
        driver.scheme_client.make_directory(root_folder)

    config_path = yatest.output_path("config.yaml")
    with open(config_path, "w") as config_file:
        config_file.write(yaml.safe_dump(conf))

    return config_path


@pytest.fixture(scope="function")
def tasklet_server(port_manager) -> server_mock.TaskletServer:
    server_process = None
    try:
        http_port = port_manager.get_port()
        grpc_port = port_manager.get_port()

        config_path = _make_server_config(http_port, grpc_port)

        server_path = yatest.binary_path("tasklet/experimental/cmd/server/tasklet-server")
        init_process = yatest.execute(
            [server_path, "--config-path", config_path, "storage", "init", "--purge", "--force-yes"])
        assert init_process.returncode == 0

        server_process = yatest.execute([server_path, "--config-path", config_path, "run"], wait=False)

        for name, port in {"http": http_port, "grpc": grpc_port}.items():
            start_time = time.perf_counter()
            while True:
                try:
                    log.debug("Probing server port. Protocol: \"%s\", Address: \"%s\"", name, ("localhost", port))
                    with socket.create_connection(("localhost", port), timeout=1):
                        break
                except OSError:
                    time.sleep(0.01)
                    if time.perf_counter() - start_time >= 10:
                        raise TimeoutError(f"Server failed to start at {name} port")
                    if not server_process.running:
                        raise EnvironmentError(f"Server crashed with code {server_process.returncode}")
        assert server_process.running

        yield server_mock.TaskletServer(http_port, grpc_port)
    finally:
        if server_process is not None:
            server_process.terminate()


@pytest.fixture(scope="function")
def test_namespace(tasklet_server: server_mock.TaskletServer) -> data_model_pb2.NamespaceMeta:
    return tasklet_server.create_ns("default-test-ns")


@pytest.fixture(scope="function")
def dummy_tasklet_model(
    tasklet_server: server_mock.TaskletServer,
    test_namespace: data_model_pb2.NamespaceMeta,
    registered_schema: str,
) -> models.ConfiguredTasklet:
    tasklet: models.Tasklet = tasklet_server.create_tasklet(
        test_namespace,
        "DummyTasklet",
        {"catalog": "/home/test"},
    )
    build: models.Build = tasklet_server.create_build(tasklet, registered_schema, None)
    label_latest: models.Label = tasklet_server.create_label(tasklet, "latest", build, {})

    return models.ConfiguredTasklet(
        namespace=test_namespace,
        tasklet=tasklet,
        build=build,
        label=label_latest,
    )


@pytest.fixture(scope="function")
def dummy_go_tasklet_model(
    tasklet_server: server_mock.TaskletServer,
    test_namespace: data_model_pb2.NamespaceMeta,
    registered_schema: str
) -> models.ConfiguredTasklet:
    tasklet: models.Tasklet = tasklet_server.create_tasklet(
        test_namespace,
        "DummyGoTasklet",
        {"catalog": "/home/test"},
    )
    build: models.Build = tasklet_server.create_build(tasklet, registered_schema, None)
    label_latest: models.Label = tasklet_server.create_label(tasklet, "latest", build, {})

    return models.ConfiguredTasklet(
        namespace=test_namespace,
        tasklet=tasklet,
        build=build,
        label=label_latest,
    )


@pytest.fixture(scope="function")
def dummy_java_tasklet_model(
    tasklet_server: server_mock.TaskletServer,
    test_namespace: data_model_pb2.NamespaceMeta,
    registered_schema: str
) -> models.ConfiguredTasklet:
    tasklet: models.Tasklet = tasklet_server.create_tasklet(
        test_namespace,
        "DummyJavaTasklet",
        {"catalog": "/home/test"},
    )
    build: models.Build = tasklet_server.create_build(
        tasklet,
        registered_schema,
        {
            "launch_spec": {"type": "jdk17", "jdk": {"main_class": "ru.yandex.example.DummyJavaTasklet"}},
        },
    )
    label_latest: models.Label = tasklet_server.create_label(tasklet, "latest", build, {})

    return models.ConfiguredTasklet(
        namespace=test_namespace,
        tasklet=tasklet,
        build=build,
        label=label_latest,
    )


@pytest.fixture(scope="function")
def registered_schema(tasklet_server: server_mock.TaskletServer) -> str:
    schemas = yatest.binary_path("tasklet/registry/common/tasklet-registry-common.protodesc")
    fds = descriptor_pb2.FileDescriptorSet()
    with open(schemas, "rb") as f:
        fds.ParseFromString(f.read())

    annotations = struct_pb2.Struct()
    annotations.update({"key": "value", "foo": [1, 2, 3]})
    schema_registry_client = tasklet_server.grpc_schema_registry_stub
    req = schema_registry_service_pb2.CreateSchemaRequest(
        schema=fds,
        annotations=annotations,
    )
    resp = schema_registry_client.CreateSchema(req)  # type: schema_registry_service_pb2.CreateSchemaResponse

    return resp.hash
