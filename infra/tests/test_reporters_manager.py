import pytest
from infra.deploy_export_stats.src.main import ReporterManager


def test_init():
    reporters = []

    config = {
        'reporter_idle_seconds': 300,
        'current_host': 'test1',
        'all_hosts': ['test1', 'test2', 'test3']
    }

    manager = ReporterManager(reporters, config)
    assert manager.first_run_idle_seconds == 100

    config['current_host'] = 'test2'
    manager = ReporterManager(reporters, config)
    assert manager.first_run_idle_seconds == 200

    config['current_host'] = 'test3'
    manager = ReporterManager(reporters, config)
    assert manager.first_run_idle_seconds == 300

    config['current_host'] = 'test4'
    with pytest.raises(ValueError):
        ReporterManager(reporters, config)


