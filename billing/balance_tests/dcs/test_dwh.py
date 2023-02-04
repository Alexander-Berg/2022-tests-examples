import subprocess

import mock
import pytest

from balance import exc
from balance.actions.dcs import dwh

from tests import object_builder as ob

DEFAULT_MODULE = 'grocery.dcs.export.aob'
DEFAULT_TASK = 'PrepareCheckData'
DEFAULT_ARGS = ('--local-scheduler', '--workers', '2')


@pytest.fixture
def popen_mock():
    with mock.patch('subprocess.Popen') as m:
        m.return_value.stdout.readline.return_value = b''
        m.return_value.returncode = 0
        yield m


@pytest.fixture
def nirvana_block(session):
    return ob.NirvanaBlockBuilder(). \
        build(session).obj


@pytest.fixture
def yt_logger_mock():
    with mock.patch('balance.actions.dcs.dwh.YtLogger') as m:
        yt_logger_mock = m.return_value
        yield yt_logger_mock


@pytest.mark.usefixtures("yt_logger_mock")
def test_invalid_module_name(nirvana_block, popen_mock):
    with pytest.raises(exc.INVALID_PARAM):
        dwh.run_data_export(
            nirvana_block,
            'grocery.dwh-200',
            'Task'
        )

    popen_mock.assert_not_called()


def test_success_run(nirvana_block, popen_mock, yt_logger_mock):
    output = [b'some output', b'some errors', b'']
    popen_mock.return_value.stdout.readline.side_effect = output
    dwh.run_data_export(nirvana_block, DEFAULT_MODULE, DEFAULT_TASK, DEFAULT_ARGS)

    expected_command = [
        '/usr/bin/dwh/run_with_env.sh',
        '-m', 'luigi',
        '--module', DEFAULT_MODULE,
        DEFAULT_TASK
    ] + list(DEFAULT_ARGS)

    popen_mock.assert_called_once_with(expected_command,
                                       stdout=subprocess.PIPE, stderr=subprocess.STDOUT)

    assert yt_logger_mock.log.call_args_list == map(mock.call, output[:-1])


@pytest.mark.usefixtures("yt_logger_mock")
def test_retry(nirvana_block, popen_mock):
    popen_mock.return_value.returncode = 1
    with pytest.raises(exc.DEFERRED_ERROR) as e:
        dwh.run_data_export(nirvana_block, DEFAULT_MODULE, DEFAULT_TASK, DEFAULT_ARGS)
    assert e.value.delay == 15
