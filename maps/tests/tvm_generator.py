import pytest
from maps.infra.ecstatic.coordinator.bin.tests.fixtures.status_exceptions import ForbiddenError


def test_tvm_upload(coordinator):
    coordinator.upload('pkg-tvm', '1.0', 'gen-q1', branch='testing', tvm_id=42)

    with pytest.raises(ForbiddenError):
        coordinator.upload('pkg-tvm', '2.0', 'gen-q1', branch='testing', tvm_id=1234)

    with pytest.raises(ForbiddenError):
        coordinator.step_in('pkg-tvm', '1.0', 'gen-q1', branch='stable', tvm_id=1234)

    coordinator.step_in('pkg-tvm', '1.0', 'gen-q1', branch='stable', tvm_id=42)


def test_wildcard_tvm_upload(coordinator, mongo):
    mongo.reconfigure(config='data/ecstatic-tvm.conf')
    coordinator.upload('pkg-tvm', '1.0', 'gen-q1', branch='testing', tvm_id=2020905)
