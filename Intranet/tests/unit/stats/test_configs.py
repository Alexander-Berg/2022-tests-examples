from intranet.femida.src.stats.registry import registry


def test_configs():
    for report_manager in registry.reports.values():
        assert report_manager().is_config_valid()
