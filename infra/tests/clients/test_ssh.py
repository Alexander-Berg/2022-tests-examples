import os
from unittest.mock import patch, Mock, MagicMock

import pytest

from infra.walle.server.tests.lib.util import TestCase
from walle.clients import ssh
from walle.credentials import Credentials, _SSH_CREDENTIALS_ID
from walle.util import cache

TEST_SSH_USER = "wall-e-autotest"
TEST_SSH_HOST = "sas1-2864.search.yandex.net"
PRESUMABLY_UNREACHABLE_HOST = "example.com"  # suppose we can't really connect there via ssh.
PRESUMABLY_INVALID_HOST = "please.tell.me.that.host.does.not.exist.yandex.net"


@pytest.fixture
def test(request):
    return TestCase.create(request)


@pytest.mark.ssh_tests
@pytest.mark.slow
@patch.object(ssh, "SSH_CONNECTION_TIMEOUT", new=1)
@patch.object(ssh, "SSH_COMMAND_TIMEOUT", new=0.5)
class TestSshClient:
    @pytest.fixture
    def create_credentials(self, test):
        with open(os.path.expanduser("~/.ssh/wall-e-autotest.rsa")) as pkey_file:
            pkey = pkey_file.read()

        # global credentials without auth keys.
        # credentials for the project should inherit username from global credentials.
        Credentials(crd_id=_SSH_CREDENTIALS_ID, public=TEST_SSH_USER, private=pkey).save()
        map(lambda c: c._clear(), cache._CACHE)

    def test_ssh_no_credentials(self, test):
        with pytest.raises(ssh.SshAuthenticationError) as exc:
            with ssh.SshClient(TEST_SSH_HOST):
                pass

        assert str(exc).endswith("Can't find credentials for ssh connection.")

    def test_ssh_project_credentials(self, create_credentials):
        with ssh.SshClient(TEST_SSH_HOST) as client:
            out = client.execute("whoami")

        # `out` is a list of output lines, with their linebreaks carefully preserved for our much annoyance.
        assert out == [TEST_SSH_USER + "\n"]

    def test_ssh_sudo(self, create_credentials):
        with ssh.SshClient(TEST_SSH_HOST) as client:
            out = client.execute("sudo whoami")

        # `out` is a list of output lines, with their linebreaks carefully preserved for our much annoyance.
        assert out == ["root\n"]

    def test_ssh_uptime(self, create_credentials):
        with ssh.SshClient(TEST_SSH_HOST) as client:
            boot_id = client.get_boot_id()

        # check that uptime is a non-empty string
        assert isinstance(boot_id, str)
        assert len(boot_id)

    def test_ssh_connection_timeout(self, create_credentials):
        with pytest.raises(ssh.SshConnectionFailedError) as exc:
            with ssh.SshClient(PRESUMABLY_UNREACHABLE_HOST):
                pass

        assert str(exc).endswith("timed out.")

    def test_ssh_connection_fail(self, create_credentials):
        with pytest.raises(ssh.SshConnectionFailedError):
            with ssh.SshClient(PRESUMABLY_INVALID_HOST):
                pass
        # actual error message is platform-dependent, no real need in checking it.

    def test_ssh_command_start_timeout(self, create_credentials):
        with ssh.SshClient(TEST_SSH_HOST) as client:
            with pytest.raises(ssh.SshCommandTimeoutError) as exc:
                client.execute("sleep 5s && whoami")

        assert str(exc).endswith("command execution timeout.")

    def test_ssh_command_run_timeout(self, create_credentials):
        with ssh.SshClient(TEST_SSH_HOST) as client:
            with pytest.raises(ssh.SshCommandTimeoutError) as exc:
                client.execute(
                    " && ".join(
                        # fixed delay   # stdout  # stderr
                        ("sleep 0.02s", "whoami", "whoami >&2")
                        * 100
                    )
                )

        assert str(exc).endswith("command execution timeout.")

    def test_ssh_command_connection_lost(self, create_credentials):
        with ssh.SshClient(TEST_SSH_HOST) as client:
            with self.mock_exec_command(client) as mock_exec_command:
                # emulate raising an exception while reading stdout.
                mock_exec_command.return_value[1].__iter__.side_effect = Exception("mocked exception occurred")
                with pytest.raises(ssh.SshConnectionFailedError) as exc:
                    client.execute("no matter what")

        assert str(exc).endswith("mocked exception occurred.")

    def test_ssh_command_exec_failed(self, create_credentials):
        with ssh.SshClient(TEST_SSH_HOST) as client:
            with self.mock_exec_command(client) as mock_exec_command:
                # emulate raising an exception while creating channel (IRL it sends stuff over network)
                mock_exec_command.side_effect = Exception("mocked exception occurred")

                with pytest.raises(ssh.SshConnectionFailedError) as exc:
                    client.execute("no matter what")

        assert str(exc).endswith("mocked exception occurred.")

    @staticmethod
    def mock_exec_command(client):
        mock_exec_command = Mock(spec_set=client._client.exec_command)
        mock_stdin = MagicMock()
        mock_stdout = MagicMock()
        mock_stderr = MagicMock()

        mock_stdin.channel = mock_stdout.channel = mock_stderr.channel = MagicMock()
        mock_stdout.__iter__.return_value = iter([])
        mock_stderr.__iter__.return_value = iter([])

        mock_exec_command.return_value = mock_stdin, mock_stdout, mock_stderr
        return patch.object(client._client, "exec_command", mock_exec_command)
