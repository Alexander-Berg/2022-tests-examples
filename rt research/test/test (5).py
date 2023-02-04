# -*- coding: utf-8 -*-

import irt.monitoring.juggler.configs

# Поля, необходимые к присутствию во всех агрегатах
REQUIRED_FOR_ALL = ["host", "service", "description", "refresh_time", "ttl", "notifications"]

# Поля, необходимые к присутствию в каждом из разновидностей агрегатов
REQUIRED_FOR_ACTIVE = ["active", "active_kwargs"]
REQUIRED_FOR_PASSIVE = ["aggregator", "children"]


def test_aggregates_config():
    for module_config in irt.monitoring.juggler.configs.get_all_configs():
        for check in module_config.get_checks():
            assert all(key in check for key in REQUIRED_FOR_ALL)
            assert all(key in check for key in REQUIRED_FOR_ACTIVE) or all(key in check for key in REQUIRED_FOR_PASSIVE)
