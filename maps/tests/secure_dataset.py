import pytest

from maps.infra.ecstatic.coordinator.bin.tests.fixtures.status_exceptions import ForbiddenError


@pytest.mark.parametrize('tvm_id', [1, 2020905])
def test_secure_dataset(tvm_id, coordinator):
    coordinator.upload('pkg-secure', '1', 'gen', tvm_id=tvm_id)
    with pytest.raises(ForbiddenError):
        coordinator.download('pkg-secure', '1')


def test_backupper_download(coordinator):
    coordinator.upload('pkg-secure', '1', 'gen', tvm_id=1)
    coordinator.download('pkg-secure', '1', tvm_id=2020905)


def test_not_secure_download(coordinator):
    coordinator.upload('pkg-a', '1', 'gen', tvm_id=1)
    coordinator.download('pkg-a', '1')
