from maps.infra.ecstatic.coordinator.bin.tests.fixtures.coordinator import Coordinator


def test_list_datasets(coordinator: Coordinator):
    coordinator.upload('pkg-a', '1.0', 'gen-ab1', tvm_id=1)
    coordinator.upload('pkg-b', '1.0', 'gen-ab1', tvm_id=1)

    assert set(coordinator.list_datasets().datasets) == {'pkg-a', 'pkg-b'}
