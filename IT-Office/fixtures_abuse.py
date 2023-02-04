import pytest
import paramiko
import source.abuse
import io
from .data_abuse import test_ticket_description, test_log
from source.config import st_client


class FakeIssue():
    def __init__(self, key, descr):
        self.k = key
        self.descr = descr

    @property
    def key(self):
        return self.k

    @property
    def description(self):
        return self.descr

    @property
    def update(self):
        return None


class FakeSHH():
    def set_missing_host_key_policy(self, arg):
        return None

    def connect(self, *args, **kwargs):
        return None

    def exec_command(self, *args):
        return ('',
                io.StringIO(test_log),
                '')


@pytest.fixture
def check_abuse(monkeypatch):
    def fakeSSH():
        return FakeSHH()

    def fakeFind(arg):
        arg = FakeIssue('HDRFS-000111', test_ticket_description)
        return [arg, arg]

    def fake_pkey(*args, **kwargs):
        return True

    def fake_st(*args, **kwargs):
        return None

    monkeypatch.setattr(paramiko, 'SSHClient', fakeSSH)
    monkeypatch.setattr(st_client.issues, 'find', fakeFind)
    monkeypatch.setattr(abuse, 'st_update', fake_st)
    monkeypatch.setattr(abuse, 'st_comment', fake_st)
    monkeypatch.setattr(paramiko.RSAKey, 'from_private_key_file', fake_pkey)
