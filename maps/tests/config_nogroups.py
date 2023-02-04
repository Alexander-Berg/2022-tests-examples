import pytest
from maps.infra.ecstatic.coordinator.bin.tests.fixtures.status_exceptions import ForbiddenError


def test_config_nogroups(mongo, coordinator):
    # use config with hosts only
    mongo.reconfigure(config='data/ecstatic-nogroups.conf')

    with pytest.raises(ForbiddenError):
        coordinator.upload('pkg-c', '1.0', 'gen-cd2', tvm_id=13)

    pkg_a_hash = coordinator.upload('pkg-a', '1.0', 'gen-ab1', branch='stable', tvm_id=1)
    coordinator.announce(pkg_a_hash, 'gen-ab1')

    coordinator.announce(pkg_a_hash, ['a1', 'a2', 'a3'])
    expected = 'pkg-a\t1.0\n'
    assert coordinator.get_postdl('a1').text == expected
    assert coordinator.get_postdl('a2').text == expected
    assert coordinator.get_postdl('a3').text == expected
    coordinator.postdl('pkg-a', '1.0', ['a1', 'a2', 'a3'])
    coordinator.require_version('pkg-a', '1.0', ['a1', 'a2', 'a3'])
