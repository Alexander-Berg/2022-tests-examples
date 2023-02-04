from unittest import mock
import pytest
from io import StringIO
from maps.pylibs.utils.lib import process

from maps.garden.sdk.core import Version
from maps.garden.sdk.resources import FileResource

from maps.garden.sdk.extensions import exec_embedded_tool_task as exec_task

TEST_BIN_RESOURCE_PATH = "/garden/test_bin"
WRONG_BIN_RESOURCE_PATH = "/garden/wrong_bin"


def test_unexistent_resource_name():
    task = exec_task.DeprecatedExecEmbeddedToolTask(
        resource_key="unexistent_name",
        arg_templates=[])

    with pytest.raises(TypeError):
        task()


def test_wrong_binary():
    task = exec_task.DeprecatedExecEmbeddedToolTask(
        resource_key=WRONG_BIN_RESOURCE_PATH,
        arg_templates=[])

    with pytest.raises(RuntimeError) as e:
        task()

    assert "[Errno 8] Exec format error" in str(e.value)


@mock.patch('sys.stderr', new_callable=StringIO)
@mock.patch('sys.stdout', new_callable=StringIO)
def test_success(mock_stdout, mock_stderr):
    task = exec_task.DeprecatedExecEmbeddedToolTask(
        resource_key=TEST_BIN_RESOURCE_PATH,
        arg_templates=[])
    task()
    assert mock_stdout.getvalue() == 'Hello, world!'
    assert mock_stderr.getvalue() == 'Hello, errors!'


@mock.patch('sys.stdout', new_callable=StringIO)
def test_good_argument(mock_stdout):
    task = exec_task.DeprecatedExecEmbeddedToolTask(
        resource_key=TEST_BIN_RESOURCE_PATH,
        arg_templates=["check argument"])
    task()
    assert mock_stdout.getvalue() == 'good argument'


def test_bad_argument():
    task = exec_task.DeprecatedExecEmbeddedToolTask(
        resource_key=TEST_BIN_RESOURCE_PATH,
        arg_templates=["bad argument"])

    with pytest.raises(process.ExecutionFailed) as e:
        task()

    assert e.value.err == "bad argument `bad argument`"


@mock.patch('sys.stdout', new_callable=StringIO)
def test_format_env(mock_stdout):
    task = exec_task.DeprecatedExecEmbeddedToolTask(
        resource_key=TEST_BIN_RESOURCE_PATH,
        arg_templates=["check environment"],
        env={"var_name": "{param}"})
    task(extra_strings={"param": "good value"})
    assert mock_stdout.getvalue() == 'good environment'


def _construct_resource(environment_settings):
    resource = FileResource("name", "name")
    resource.version = Version()
    resource.load_environment_settings(environment_settings)
    resource.exists = False
    return resource


@mock.patch('sys.stdout', new_callable=StringIO)
def test_input_stream(mock_stdout, environment_settings):
    resource = _construct_resource(environment_settings)

    with resource.open("w") as f:
        f.write("good data")

    task = exec_task.DeprecatedExecEmbeddedToolTask(
        resource_key=TEST_BIN_RESOURCE_PATH,
        arg_templates=["check stdin"])
    task(stdin=resource)
    assert mock_stdout.getvalue() == 'good data'


def test_output_streams(environment_settings):
    resource = _construct_resource(environment_settings)

    task = exec_task.DeprecatedExecEmbeddedToolTask(
        resource_key=TEST_BIN_RESOURCE_PATH,
        arg_templates=["check argument"])
    task(stdout=resource)

    with resource.open("r") as f:
        out_data = f.readline()
        assert out_data == "good argument"
