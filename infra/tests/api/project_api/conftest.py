import pytest

from infra.walle.server.tests.lib.dns import dns_box, dns_box_fixture  # noqa NOTE(rocco66): fixture
from infra.walle.server.tests.lib.util import monkeypatch_config


@pytest.fixture
def mock_certificator_allowed_domain_list(monkeypatch):
    monkeypatch_config(
        monkeypatch, "certificator.allowed_dns_domains_re", [r"^search\.yandex\.net$", r"\.yt\.yandex\.net"]
    )


@pytest.fixture
def enable_idm_push(mp):
    monkeypatch_config(mp, "idm.push_api_enabled", True)
