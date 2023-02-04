import random
import socket
import tempfile
from contextlib import contextmanager
from pathlib import Path
from typing import ContextManager
from unittest import mock

import porto.api
import pytest

from idm.core.workflow import exceptions
from idm.core.constants.workflow import WORKFLOW_CONTAINER_START_STATUS
from idm.core.workflow.sandbox import connection
from idm.core.workflow.sandbox.manager import executors
from idm.tests.utils import create_system, random_slug

EXECUTOR_MODULE = 'idm.core.workflow.sandbox.manager.executors'


@contextmanager
def temporary_directory(*args, **kwargs) -> ContextManager[Path]:
    with tempfile.TemporaryDirectory(*args, **kwargs) as root_directory:
        yield Path(root_directory)


@contextmanager
def temporary_file(*args, **kwargs) -> ContextManager[Path]:
    with tempfile.NamedTemporaryFile(*args, **kwargs) as _file:
        yield Path(_file.name)


@pytest.fixture
def porto_connection() -> mock.MagicMock:
    with mock.patch(
            f'{EXECUTOR_MODULE}.get_porto_connection',
            return_value=mock.MagicMock(spec=porto.Connection),
    ) as _conn_mock:
        yield _conn_mock.return_value


@pytest.fixture
def executor():
    return executors.SandboxedWorkflowExecutor(code='', system=create_system())


@pytest.fixture
def container_name() -> str:
    with mock.patch(
            f'{EXECUTOR_MODULE}.SandboxedWorkflowExecutor._get_unique_identifier',
            return_value=random_slug(),
    ) as _mock:
        yield _mock.return_value


@pytest.fixture
def container_user():
    return random.randint(0, 1000)


@pytest.fixture
def python2_layer(container_user) -> Path:
    with temporary_directory() as _path, \
            mock.patch(
                f'{EXECUTOR_MODULE}.PYTHON_PORTO_LAYER_PATH',
                new=_path
            ):
        (_path / 'etc').mkdir()
        with (_path / 'etc' / 'passwd').open('w+') as f:
            print(executors.CONTAINER_USER, '', container_user, container_user, '', sep=':', file=f)
        yield _path


def test_sandbox_executor_get_uid_and_gid(executor, container_user, python2_layer):
    assert executor._get_user_and_group_by_name(executors.CONTAINER_USER) == (str(container_user), str(container_user))


def test_sandbox_executor_get_uid_and_gid__file_not_found(executor, container_user, python2_layer):
    (python2_layer / 'etc' / 'passwd').unlink()

    with pytest.raises(exceptions.WorkflowContainerInitError):
        assert executor._get_user_and_group_by_name(executors.CONTAINER_USER)


def test_sandbox_executor_get_uid_and_gid__user_not_found(executor, python2_layer):
    with (python2_layer / 'etc' / 'passwd').open('w'):
        pass

    with pytest.raises(exceptions.WorkflowContainerInitError):
        assert executor._get_user_and_group_by_name(executors.CONTAINER_USER)


def test_sandbox_executor_init_container(porto_connection, executor, container_name, python2_layer):
    container = porto.api.Container(porto_connection, container_name)
    porto_connection.Create.return_value = container
    with temporary_directory() as root_directory:
        result = executor._init_container(container_name, root_directory)

        porto_connection.Create.assert_called_once_with(f'self/{container_name}')

        assert root_directory.exists()
        for descriptor in ('stdout', 'stderr'):
            (root_directory / descriptor).exists()
            (root_directory / descriptor).is_fifo()

        assert (root_directory / executors.CONTAINER_CODE_DIR).exists()
        assert (root_directory / executors.CONTAINER_CODE_DIR).exists()

        for _, module in executors.MODULES_TO_COPY:
            assert (root_directory / executors.CONTAINER_CODE_DIR / module).exists()
        entrypoint = root_directory / executors.CONTAINER_CODE_DIR / executors.ENTRYPOINT_NAME
        assert entrypoint.exists() and entrypoint.is_file()

    assert result is container


def test_sandbox_executor_init_container__creation_failed(porto_connection, executor, container_name):
    porto_connection.Create.side_effect = porto.exceptions.UnknownError
    with temporary_directory() as root_directory, pytest.raises(exceptions.WorkflowContainerInitError):
        executor._init_container(container_name, root_directory)

        porto_connection.Create.assert_called_once_with(container_name)

        assert not root_directory.exists()
        assert list(root_directory.iterdir()) == []


def test_sandbox_executor_start_container(porto_connection, executor, container_name, python2_layer):
    container = porto.api.Container(porto_connection, container_name)
    porto_connection.Start.return_value = container

    with temporary_file() as socket_file, \
            mock.patch(
                f'{EXECUTOR_MODULE}.SandboxedWorkflowExecutor._create_socket',
                return_value=mock.MagicMock(spec=socket.socket)) as create_socket_mock, \
            mock.patch(
                f'{EXECUTOR_MODULE}.SandboxedWorkflowExecutor._create_connection',
                return_value=mock.MagicMock(spec=connection.Connection)) as create_connection_mock:
        create_connection_mock.return_value.get_data.return_value = \
            (0, {'status': WORKFLOW_CONTAINER_START_STATUS.CONTAINER_START_SUCCESS})

        result = executor._start_container(container, socket_file)

        create_socket_mock.assert_called_once_with(str(socket_file))
        porto_connection.Start.assert_called_once()
        create_connection_mock.assert_called_once_with(create_socket_mock.return_value)

    assert result is create_connection_mock.return_value


def test_sandbox_executor_start_container__start_error(porto_connection, executor, container_name):
    container = porto.api.Container(porto_connection, container_name)
    porto_connection.Start.side_effect = porto.exceptions.UnknownError

    with temporary_file() as socket_file, \
            mock.patch(
                f'{EXECUTOR_MODULE}.SandboxedWorkflowExecutor._create_socket',
                return_value=mock.MagicMock(spec=socket.socket)) as create_socket_mock, \
            mock.patch(
                f'{EXECUTOR_MODULE}.SandboxedWorkflowExecutor._create_connection',
                return_value=mock.MagicMock(spec=connection.Connection)) as create_connection_mock, \
            pytest.raises(exceptions.WorkflowContainerInitError):
        executor._start_container(container, socket_file)

        create_socket_mock.assert_called_once_with(socket_file.name)
        porto_connection.Start.assert_called_once()
        create_connection_mock.assert_not_called()


def test_sandbox_executor_start_container__connection_timeout(porto_connection, executor, container_name):
    container = porto.api.Container(porto_connection, container_name)
    porto_connection.Start.return_value = container

    with temporary_file() as socket_file, \
            mock.patch(
                f'{EXECUTOR_MODULE}.SandboxedWorkflowExecutor._create_socket',
                return_value=mock.MagicMock(spec=socket.socket)) as create_socket_mock, \
            mock.patch(
                f'{EXECUTOR_MODULE}.SandboxedWorkflowExecutor._create_connection',
                return_value=mock.MagicMock(spec=connection.Connection)) as create_connection_mock, \
            pytest.raises(exceptions.WorkflowContainerInitError):
        create_connection_mock.side_effect = socket.timeout

        executor._start_container(container, socket_file)

        create_socket_mock.assert_called_once_with(socket_file.name)
        porto_connection.Start.assert_called_once()
        create_connection_mock.assert_called_once_with(create_socket_mock.return_value)


def test_sandbox_executor_start_container__invalid_response_status(porto_connection, executor, container_name):
    container = porto.api.Container(porto_connection, container_name)
    porto_connection.Start.return_value = container

    with temporary_file() as socket_file, \
            mock.patch(
                f'{EXECUTOR_MODULE}.SandboxedWorkflowExecutor._create_socket',
                return_value=mock.MagicMock(spec=socket.socket)) as create_socket_mock, \
            mock.patch(
                f'{EXECUTOR_MODULE}.SandboxedWorkflowExecutor._create_connection',
                return_value=mock.MagicMock(spec=connection.Connection)) as create_connection_mock, \
            pytest.raises(exceptions.WorkflowContainerInitError):
        create_connection_mock.return_value.get_data.return_value = \
            (0, {'status': random_slug()})

        executor._start_container(container, socket_file)

        create_socket_mock.assert_called_once_with(socket_file.name)
        porto_connection.Start.assert_called_once()
        create_connection_mock.assert_called_once_with(create_socket_mock.return_value)


def test_sandbox_executor_create_container(porto_connection, executor, container_name):
    container = porto.api.Container(porto_connection, container_name)

    def fake_init_container(_, _root: Path):
        _root.mkdir()
        return container

    ctx_manager = executor.create_container()
    with temporary_directory() as fs_root, \
            mock.patch(f'{EXECUTOR_MODULE}.LOCAL_ROOT', new=fs_root), \
            mock.patch(
                f'{EXECUTOR_MODULE}.SandboxedWorkflowExecutor._init_container',
                side_effect=fake_init_container,
            ) as init_container_mock, \
            mock.patch(f'{EXECUTOR_MODULE}.SandboxedWorkflowExecutor._start_container') as start_container_mock:
        containers_path = (fs_root / executors.CONTAINERS_DIRECTORY)
        containers_path.mkdir()
        container_root = containers_path / container_name

        assert ctx_manager.__enter__() is start_container_mock.return_value
        assert containers_path.exists()
        assert container_root.exists()

        ctx_manager.__exit__(None, None, None)
        assert containers_path.exists()
        assert not container_root.exists()

    porto_connection.Destroy.assert_called_once_with(container_name)
    init_container_mock.assert_called_once_with(container_name, containers_path / container_name)
    start_container_mock.assert_called_once_with(container, containers_path / container_name / 'socket.sock')


def test_sandbox_executor_create_container__containers_root_not_exists(porto_connection, executor):

    ctx_manager = executor.create_container()
    with temporary_directory() as fs_root, \
            mock.patch(f'{EXECUTOR_MODULE}.LOCAL_ROOT', new=fs_root), \
            mock.patch(f'{EXECUTOR_MODULE}.SandboxedWorkflowExecutor._init_container') as init_container_mock, \
            mock.patch(f'{EXECUTOR_MODULE}.SandboxedWorkflowExecutor._start_container') as start_container_mock, \
            pytest.raises(exceptions.WorkflowContainerInitError):
        ctx_manager.__enter__()
    init_container_mock.assert_not_called()
    start_container_mock.assert_not_called()


def test_sandbox_executor_create_container__init_error(porto_connection, executor, container_name):

    ctx_manager = executor.create_container()
    with temporary_directory() as fs_root, \
            mock.patch(f'{EXECUTOR_MODULE}.LOCAL_ROOT', new=fs_root), \
            mock.patch(
                f'{EXECUTOR_MODULE}.SandboxedWorkflowExecutor._init_container',
                side_effect=exceptions.WorkflowContainerInitError,
            ) as init_container_mock, \
            mock.patch(f'{EXECUTOR_MODULE}.SandboxedWorkflowExecutor._start_container') as start_container_mock:
        containers_path = (fs_root / executors.CONTAINERS_DIRECTORY)
        containers_path.mkdir()

        with pytest.raises(exceptions.WorkflowContainerInitError):
            ctx_manager.__enter__()

        assert containers_path.exists()
        assert not (containers_path / container_name).exists()

    init_container_mock.assert_called_once_with(container_name, containers_path / container_name)
    start_container_mock.assert_not_called()


def test_sandbox_executor_create_container__start_error(porto_connection, executor, container_name):
    container = porto.api.Container(porto_connection, container_name)

    def fake_init_container(_, _root: Path):
        _root.mkdir()
        return container

    ctx_manager = executor.create_container()
    with temporary_directory() as fs_root, \
            mock.patch(f'{EXECUTOR_MODULE}.LOCAL_ROOT', new=fs_root), \
            mock.patch(
                f'{EXECUTOR_MODULE}.SandboxedWorkflowExecutor._init_container',
                side_effect=fake_init_container,
            ) as init_container_mock, \
            mock.patch(
                f'{EXECUTOR_MODULE}.SandboxedWorkflowExecutor._start_container',
                side_effect=exceptions.WorkflowContainerInitError,
            ) as start_container_mock:
        containers_path = (fs_root / executors.CONTAINERS_DIRECTORY)
        containers_path.mkdir()
        container_root = containers_path / container_name

        with pytest.raises(exceptions.WorkflowContainerInitError):
            ctx_manager.__enter__()

        assert containers_path.exists()
        assert not container_root.exists()

    porto_connection.Destroy.assert_called_once_with(container_name)
    init_container_mock.assert_called_once_with(container_name, containers_path / container_name)
    start_container_mock.assert_called_once_with(container, containers_path / container_name / 'socket.sock')

# TODO покрыть тестами все executor-ы
