import subprocess

import mock
import pytest

from balance import exc
from balance.actions.nirvana.operations import ybar_run_full_calc

from tests import object_builder as ob


@pytest.fixture
def popen_mock():
    with mock.patch('subprocess.Popen') as m:
        m.return_value.communicate.return_value = ('', '')
        m.return_value.returncode = 0
        yield m


@pytest.fixture
def nirvana_block(session):
    return ob.NirvanaBlockBuilder(). \
        add_output('stdout', data_type='text'). \
        add_output('stderr', data_type='text'). \
        build(session).obj


@pytest.fixture
def nirvana_block_upload_mock():
    with mock.patch('balance.mapper.nirvana_processor.NirvanaBlock.upload') as m:
        yield m


def test_success_run(nirvana_block, nirvana_block_upload_mock, popen_mock):
    """
     Checks that the command runs without params
    """
    popen_mock.return_value.communicate.return_value = ('some output', 'some errors')
    ybar_run_full_calc.process(nirvana_block)
    expected_command = ['/usr/bin/yb-ar-calculate']
    popen_mock.assert_called_once_with(expected_command, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
    nirvana_block_upload_mock.assert_any_call('stdout', 'some output')
    nirvana_block_upload_mock.assert_any_call('stderr', 'some errors')


def test_success_run_with_params(nirvana_block, nirvana_block_upload_mock, popen_mock):
    """
        Checks that the command runs with params
    """
    popen_mock.return_value.communicate.return_value = ('some output', 'some errors')
    nirvana_block.options['params'] = '--prod-testing --no-mnclose --platform-run-dt "2021.11.17 16:35:10"'
    ybar_run_full_calc.process(nirvana_block)
    expected_command = ['/usr/bin/yb-ar-calculate', '--prod-testing', '--no-mnclose', '--platform-run-dt',
                        '2021.11.17 16:35:10']
    popen_mock.assert_called_once_with(expected_command, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
    nirvana_block_upload_mock.assert_any_call('stdout', 'some output')
    nirvana_block_upload_mock.assert_any_call('stderr', 'some errors')


def test_exc(nirvana_block, nirvana_block_upload_mock, popen_mock):
    """
        Checks that an error is returned when the command fails
    """
    popen_mock.return_value.communicate.return_value = ('some output', 'some errors')
    popen_mock.return_value.returncode = 1
    with pytest.raises(exc.CRITICAL_ERROR):
        ybar_run_full_calc.process(nirvana_block)
    nirvana_block_upload_mock.assert_any_call('stdout', 'some output')
    nirvana_block_upload_mock.assert_any_call('stderr', 'some errors')
