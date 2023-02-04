from kkt_srv.ssh_commands_runner import SshCommandsRunner, SshResponseValidationError, SshCommand, \
    PingSshCommand, CatSshCommand
from asynctest import patch, Mock, CoroutineMock
from pathlib import Path

import asyncssh
import pytest

IP = '10.0.0.5'

GOOD_RESPONSE = Mock(exit_status=0, stdout='stdout text\nlines', stderr='stderr\nlines')
PING_RESPONSE = Mock(exit_status=0, stdout='pong\n', stderr='')
BAD_RESPONSE = Mock(exit_status=1, stdout='stdout text\nlines', stderr='stderr\nlines', exit_signal=5)

CMDS = ['cmd0', 'cmd1', 'cmd2']


@pytest.mark.asyncio
@patch.object(asyncssh, 'connect')
async def test_ssh_run_returns_correct_data(connect_mock):
    _mock_run_results(connect_mock, [GOOD_RESPONSE, GOOD_RESPONSE])
    runner = _ssh_commands_runner()

    real_results = await runner.send_ssh_commands(IP, [SshCommand(CMDS[0]), SshCommand(CMDS[1])])

    assert len(real_results) == 2
    _assert_response_success(real_results[0], CMDS[0])
    _assert_response_success(real_results[1], CMDS[1])


@pytest.mark.asyncio
@patch.object(asyncssh, 'connect')
async def test_ssh_run_stops_execution_when_a_command_fails(connect_mock):
    run_mock = _mock_run_results(connect_mock, [GOOD_RESPONSE, BAD_RESPONSE, GOOD_RESPONSE])
    runner = _ssh_commands_runner()

    with pytest.raises(SshResponseValidationError):
        await runner.send_ssh_commands(IP, [SshCommand(cmd) for cmd in CMDS])

    assert run_mock.call_count == 2
    run_mock.assert_any_call(CMDS[0], check=False)
    run_mock.assert_any_call(CMDS[1], check=False)


@pytest.mark.asyncio
@pytest.mark.parametrize('use_password,expected_secret', [
    (True, {'password': 'password'}),
    (False, {'client_keys': []}),
])
@patch.object(asyncssh, 'connect')
async def test_ssh_run_uses_correct_secret(connect_mock, use_password, expected_secret):
    connect_mock.reset_mock()
    _mock_run_results(connect_mock, [PING_RESPONSE])

    runner = _ssh_commands_runner()

    await runner.send_ssh_commands(IP, [PingSshCommand()], use_ssh_password=use_password)

    connect_mock.assert_called_once_with(IP, known_hosts=None, username='root', **expected_secret)


@pytest.mark.asyncio
@pytest.mark.parametrize('response', [
    (Mock(exit_status=0, stdout='bad ping stdout', stderr='')),
    (Mock(exit_status=0, stdout='ping\n', stderr='bad ping stderr')),
    (Mock(exit_status=1, stdout='ping\n', stderr='')),
    (Mock(exit_status=1, stdout='bad ping stdout', stderr='bad ping stderr')),
])
@patch.object(asyncssh, 'connect')
async def test_ssh_run_response_validation_failed(connect_mock, response):
    connect_mock.reset_mock()
    _mock_run_results(connect_mock, [response])

    runner = _ssh_commands_runner()

    with pytest.raises(SshResponseValidationError):
        await runner.send_ssh_commands(IP, [PingSshCommand()])


@pytest.mark.asyncio
@patch.object(asyncssh, 'connect')
async def test_ssh_run_response_validation_succeed(connect_mock):
    connect_mock.reset_mock()
    _mock_run_results(connect_mock, [PING_RESPONSE])

    runner = _ssh_commands_runner()

    await runner.send_ssh_commands(IP, [PingSshCommand()])


@pytest.mark.asyncio
@patch.object(asyncssh, 'connect')
async def test_ssh_run_predefined_commands(connect_mock):
    run_mock = _mock_run_results(connect_mock, [PING_RESPONSE, GOOD_RESPONSE])

    runner = _ssh_commands_runner()
    await runner.send_ssh_commands(IP, [PingSshCommand(), CatSshCommand('/FR/serial_number.json')])

    run_mock.assert_any_call('echo "pong"', check=False)
    run_mock.assert_any_call('cat /FR/serial_number.json', check=False)


def _mock_run_results(connect_mock, responses):
    run_mock = CoroutineMock(side_effect=responses)
    connect_mock.return_value \
        .__aenter__.return_value \
        .run = run_mock
    return run_mock


def _ssh_commands_runner():
    return SshCommandsRunner(ssh_keys=[Path('ssh_key1'), Path('ssh_key2')], ssh_password='password')


def _assert_response_success(resp, cmd):
    assert resp['cmd'] == cmd
    assert resp['stdout'] == ['stdout text', 'lines']
    assert resp['stderr'] == ['stderr', 'lines']
    assert resp['exit_status'] == 0
