# -*- coding: utf-8 -*-

import pytest
import yatest

import irt.monitoring.atoms.config.funcs
import irt.monitoring.solomon.alerts.configs


@pytest.fixture
def mock_perl_common_options(monkeypatch):
    monkeypatch.setattr(irt.monitoring.atoms.config.funcs, "get_arcadia_root", yatest.common.build_path)


@pytest.mark.usefixtures("mock_perl_common_options")
def test_alerts_config():
    alerts_ids = set()
    for config_module in irt.monitoring.solomon.alerts.configs.get_all_configs():
        for alert in config_module.get_alerts():
            for neccessary_field in ["id", "name", "notificationChannels", "type", "annotations"]:
                assert neccessary_field in alert

            assert alert["id"] not in alerts_ids
            alerts_ids.add(alert["id"])
