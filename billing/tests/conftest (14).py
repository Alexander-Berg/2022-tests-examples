import os
import shutil
import pathlib

import pytest
from invoke.context import Context
from invoke.executor import Executor

context_commands = []


class CommandResult:

    def __init__(self, command):
        self.command = command

    stdout = 'tstout'
    ok = True


class MockContext(Context):

    def _run(self, runner, command, **kwargs):
        context_commands.append(command)
        return CommandResult(command)

    def _sudo(self, runner, command, **kwargs):
        context_commands.append(command)
        return CommandResult(command)

    def cd(self, path):
        context_commands.append('cd %s' % path)
        return super().cd(path)

    def put(self, what, where):
        context_commands.append('put %s %s' % (what, where))


class MockExecutor(Executor):

    def expand_calls(self, calls):
        for call in calls:
            call.make_context = lambda config: MockContext(config)
        return super().expand_calls(calls)


class RunResultMock:

    def __init__(self,  out='', err=''):
        self.stderr = err
        self.stdout = out


@pytest.fixture
def run_command_mock(monkeypatch, arcadia_prefix: str, demo_path: str):

    context = {
        'run_result': RunResultMock(),
    }

    def run_mock(cmd, **kwargs):
        context_commands.append(cmd)
        return context['run_result']

    monkeypatch.chdir(demo_path)
    monkeypatch.setattr(f'{arcadia_prefix}baf.overrides.BafExecutor', MockExecutor)
    monkeypatch.setattr(f'{arcadia_prefix}baf.utils.get_token', lambda path, hint: 'testtoken')
    monkeypatch.setattr(f'{arcadia_prefix}baf.commands.utils.Utils.run', run_mock)
    monkeypatch.setattr(f'{arcadia_prefix}baf.commands.utils.Utils.show_message', lambda msg: context_commands.append(msg))

    try:
        from billing.library.tools.baf.baf.cli import program
    except ImportError:
        from baf.cli import program

    def run_command_mock_(command, *args, run_result: dict = None):
        cfg_path = f'{demo_path}/baf.yml'

        with open(cfg_path, 'rb') as f:
            cfg = f.read()

        context['run_result'] = RunResultMock(**run_result or {})
        try:
            program.run(['baf', command] + list(args), exit=False)
            commands = list(context_commands)
            return commands
        finally:
            context_commands.clear()
            # восстанавливаем конфиг (он мог быть дополнен выполнением команды)
            with open(cfg_path, 'wb') as f:
                f.write(cfg)

    return run_command_mock_


@pytest.fixture(scope='session')
def is_ya_make() -> bool:
    """
    Проверяем, работаем ли через `ya make -t`.
    """
    return os.getenv('YA_TEST_RUNNER', False)


@pytest.fixture
def arcadia_prefix(is_ya_make: bool) -> str:
    """
    Если работаем через `ya make -t`, то в используем полный путь до модулей в моках.
    """
    return is_ya_make and 'billing.library.tools.baf.' or ''


@pytest.fixture(scope='session')
def working_path(is_ya_make: bool) -> pathlib.Path:
    """
    Если работаем через `ya make -t` копируем используемые директории в рабочую директорию тестов.

    Для некоторых тестов нам требуется редактировать файлы из `demo`.
    В автосборке это не работает, т.к. `DATA` файлы монтируются как read-only.
    Поэтому используем рабочую директорию теста.
    """
    if not is_ya_make:
        return pathlib.Path.cwd()

    import yatest.common

    source_path = pathlib.Path(
        yatest.common.source_path('billing/library/tools/baf')
    ).resolve()
    working_path = pathlib.Path(
        yatest.common.work_path('baf')
    ).resolve()

    shutil.copytree(str(source_path), str(working_path))
    for path in working_path.rglob('*'):
        path.chmod(0o755 if path.is_dir() else 0o644)

    return working_path


@pytest.fixture(scope='session', autouse=True)
def chdir_tests(working_path: pathlib.Path):
    """
    Рабочая директория должна быть`tests`.
    """
    os.chdir(str(working_path))

    if working_path.name != 'tests':
        # To run both from package root and tests/ subdir.
        os.chdir(str(working_path / 'tests'))


@pytest.fixture
def demo_path(working_path: pathlib.Path) -> str:
    """
    Путь до директории `demo`, обычно это `../demo`.
    Но если работаем через `ya make -t`, то используем специально подготовленную директорию.
    """
    if working_path.name == 'tests':
        working_path = working_path.parent
    return str(working_path / 'demo')
