import pytest

from infra.walle.server.tests.lib.util import TestCase


@pytest.fixture
def test(request, monkeypatch_timestamp, monkeypatch_audit_log):
    return TestCase.create(request)
