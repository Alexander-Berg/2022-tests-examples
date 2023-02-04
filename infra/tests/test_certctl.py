import json
import os
import time

from infra.rtc.certman import certctl, certificate


def test_command_status_absent(caplog):
    exit_code = certctl.run(args=['status', '--state-file', '/not/exist'])

    assert 'Failed to load state' in caplog.records[-1].getMessage()
    assert exit_code == 2


def test_command_status_ok(capsys, tmp_path):
    state_file = os.path.join(tmp_path, 'state')
    certificate.Status(days_left=365, error_msg='').dump_to_file(state_file)
    exit_code = certctl.run(args=['status', '--state-file', state_file])
    stdio = capsys.readouterr()

    assert 'Valid: True\nUpdate required: False\nDays left: 365\n' == stdio.out
    assert exit_code == 0


def test_command_status_err(capsys, tmp_path):
    state_file = os.path.join(tmp_path, 'state')
    certificate.Status(days_left=10, error_msg='Err msg').dump_to_file(state_file)
    exit_code = certctl.run(args=['status', '--state-file', state_file])
    stdio = capsys.readouterr()

    assert 'Valid: False\nUpdate required: False\nError: Err msg\n' == stdio.out
    assert exit_code == 4


def test_command_status_outdated(capsys, tmp_path):
    state_file = os.path.join(tmp_path, 'state')
    certificate.Status(days_left=10, error_msg='Err msg').dump_to_file(state_file)
    state_mtime = time.time() - (86400 + 1)
    os.utime(state_file, times=(state_mtime, state_mtime))
    exit_code = certctl.run(args=['status', '--state-file', state_file])
    stdio = capsys.readouterr()

    assert 'State outdated: True\n' == stdio.out
    assert exit_code == 8


def test_command_status_absent_juggler(capsys):
    exit_code = certctl.run(args=['status', '--state-file', '/not/exist', '--juggler'])
    check = json.loads(capsys.readouterr().out)['events'][0]

    assert check['status'] == 'WARN'
    assert 'No such file' in check['description']

    # cert status encoded in check body, exit code is check execution status
    assert exit_code == 0


def test_command_status_ok_juggler(capsys, tmp_path):
    state_file = os.path.join(tmp_path, 'state')
    certificate.Status(days_left=365, error_msg='').dump_to_file(state_file)
    exit_code = certctl.run(args=['status', '--juggler', '--state-file', state_file])
    check = json.loads(capsys.readouterr().out)['events'][0]

    assert check['status'] == 'OK'
    assert '"reason": "Ok"' in check['description']
    assert exit_code == 0


def test_command_status_warn_juggler__update_required(capsys, tmp_path):
    state_file = os.path.join(tmp_path, 'state')
    certificate.Status(days_left=10, error_msg='').dump_to_file(state_file)
    exit_code = certctl.run(args=['status', '--juggler', '--cert-min-days', '14', '--state-file', state_file])
    check = json.loads(capsys.readouterr().out)['events'][0]

    assert check['status'] == 'WARN'
    assert '"reason": "Update required"' in check['description']
    assert exit_code == 0


def test_command_status_warn_juggler__outdated_state(capsys, tmp_path):
    state_file = os.path.join(tmp_path, 'state')
    certificate.Status(days_left=10, error_msg='').dump_to_file(state_file)
    state_mtime = time.time() - (86400 + 1)
    os.utime(state_file, times=(state_mtime, state_mtime))
    exit_code = certctl.run(args=['status', '--juggler', '--cert-min-days', '14', '--state-file', state_file])
    check = json.loads(capsys.readouterr().out)['events'][0]

    assert check['status'] == 'WARN'
    assert '"reason": "State outdated"' in check['description']
    assert exit_code == 0
