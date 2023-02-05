from maps.infra.ecstatic.tool.ecstatic_api.coordinator import Coordinator


def test_datatesting():
    coordinator = Coordinator(environment_name='datatesting')
    assert coordinator._coordinator_host == 'core-ecstatic-coordinator.datatesting.maps.yandex.net'


def test_custom_environment():
    coordinator = Coordinator(environment_name='testing')
    assert coordinator._coordinator_host == 'core-ecstatic-coordinator.testing.maps.yandex.net'


def test_default_environment():
    coordinator = Coordinator()
    assert coordinator._coordinator_host == 'localhost'
