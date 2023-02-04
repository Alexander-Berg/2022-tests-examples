import dataclasses
import pytest
import os

import yatest.common as ya_test

pytest_plugins = (
    "tasklet.experimental.tests.common",
)


@dataclasses.dataclass
class TaskletEnv:
    executor_binary_path: str
    executable_path: str
    java_binary_path: str
    root_path: str

    # NB: subpaths from environment.go
    # taskletSandboxPath    = "sandbox"
    # resourcesDataPath     = "resources"
    # logsPath              = "tasklet_logs"

    def logs_dir(self):
        return os.path.join(self.root_path, "tasklet_logs")

    def sandbox_dir(self):
        return os.path.join(self.root_path, "sandbox")

    def saved_context_path(self):
        return os.path.join(self.sandbox_dir(), "tasklet_context.message")


@pytest.fixture(scope="session")
def java_binary_path():
    return ya_test.java_bin()


@pytest.fixture(scope="session")
def executor_binary_path() -> str:
    return ya_test.binary_path("tasklet/experimental/cmd/executor/tasklet-executor")


@pytest.fixture(scope="session")
def dummy_tasklet_binary_path() -> str:
    return ya_test.binary_path("tasklet/experimental/tests/tasklets/dummy_tasklet/dummy-tasklet")


@pytest.fixture(scope="session")
def dummy_go_tasklet_binary_path() -> str:
    return ya_test.binary_path("tasklet/experimental/tests/tasklets/dummy_go_tasklet/dummy-go-tasklet")


@pytest.fixture(scope="session")
def dummy_java_tasklet_jar_path() -> str:
    return ya_test.binary_path("tasklet/experimental/tests/tasklets/dummy_java_tasklet/dummy-java-tasklet.jar")


@pytest.fixture(scope="function")
def default_env(executor_binary_path: str) -> TaskletEnv:
    return TaskletEnv(
        executor_binary_path=executor_binary_path,
        executable_path="",
        java_binary_path="",
        root_path=ya_test.test_output_path("workdir"),
    )


@pytest.fixture(scope="function")
def dummy_tasklet_env(default_env: TaskletEnv, dummy_tasklet_binary_path: str) -> TaskletEnv:
    return dataclasses.replace(
        default_env,
        executable_path=dummy_tasklet_binary_path,
    )


@pytest.fixture(scope="function")
def dummy_go_tasklet_env(default_env: TaskletEnv, dummy_go_tasklet_binary_path: str) -> TaskletEnv:
    return dataclasses.replace(
        default_env,
        executable_path=dummy_go_tasklet_binary_path,
    )


@pytest.fixture(scope="function")
def dummy_java_tasklet_env(
    default_env: TaskletEnv,
    dummy_java_tasklet_jar_path: str,
    java_binary_path: str,
) -> TaskletEnv:

    return dataclasses.replace(
        default_env,
        java_binary_path=java_binary_path,  # Java of default Arcadia version
        executable_path=dummy_java_tasklet_jar_path,
    )
