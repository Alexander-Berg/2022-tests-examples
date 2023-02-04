import pytest

import walle.expert.rules.utils as utils
from infra.walle.server.tests.lib.util import monkeypatch_config
from walle.expert.types import CheckType

TEST_CHECKS = ["check_mock1", "check_mock2"]


@pytest.fixture
def check_overrides(mp):
    mp.function(utils._get_checks_percentage_overrides, return_value={"check_mock1": 50, "check_mock2": 30})


@pytest.fixture
def config_check_overrides(monkeypatch):
    monkeypatch_config(monkeypatch, "automation.checks_percentage.check_mock1", 50)
    monkeypatch_config(monkeypatch, "automation.checks_percentage.check_mock2", 30)


@pytest.fixture
def host_hash_20(mp):
    mp.function(utils._get_host_hash, return_value=20)


@pytest.fixture
def host_hash_99(mp):
    mp.function(utils._get_host_hash, return_value=99)


class TestChecksDisabled:
    @pytest.mark.parametrize("check", TEST_CHECKS)
    def test_db_override(self, walle_test, check_overrides, check, host_hash_99):
        host = walle_test.mock_host({"name": "mock-host"})
        result = utils._should_be_disabled_check(host, check)
        assert result is True

    @pytest.mark.parametrize("check", TEST_CHECKS)
    def test_config_override(self, walle_test, config_check_overrides, check, host_hash_99):
        host = walle_test.mock_host({"name": "mock-host"})
        assert utils._should_be_disabled_check(host, check) is True

    def test_host_specific_checks_disabled_in_config(self, mp, walle_test):
        host_inv = 111111
        mp.config("automation.hosts_with_disabled_checks", {host_inv: [CheckType.MEMORY, CheckType.CPU_CAPPING]})
        host = walle_test.mock_host({"inv": host_inv})
        assert utils._should_be_disabled_check(host, CheckType.MEMORY)
        assert utils._should_be_disabled_check(host, CheckType.CPU_CAPPING)
        assert not utils._should_be_disabled_check(host, CheckType.LINK)


class TestChecksEnabled:
    @pytest.mark.parametrize("check", TEST_CHECKS)
    def test_existing_db_overrides(self, walle_test, check_overrides, check, host_hash_20):
        host = walle_test.mock_host({"name": "mock-host"})
        assert utils._should_be_disabled_check(host, check) is False

    @pytest.mark.parametrize("check", TEST_CHECKS)
    def test_existing_config_overrides(self, walle_test, config_check_overrides, check, host_hash_20):
        host = walle_test.mock_host({"name": "mock-host"})
        assert utils._should_be_disabled_check(host, check) is False

    def test_inexisting_override_default_enabled(self, walle_test, check_overrides, host_hash_20):
        host = walle_test.mock_host({"name": "mock-host"})
        assert utils._should_be_disabled_check(host, 'non_existing_check_mock') is False
