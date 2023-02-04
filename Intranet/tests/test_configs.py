from staff.stats.libraries import reports_library


def test_configs():
    for report_manager in reports_library:
        assert report_manager().is_config_valid()
