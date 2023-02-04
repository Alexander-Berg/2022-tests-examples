from maps.infra.ecstatic.sandbox.reconfigurer.lib.reconfigure import ReconfigureError
import pytest


def test_expiration(mongo, coordinator):
    with pytest.raises(ReconfigureError):
        mongo.reconfigure(config='data/missing-expire.conf')

    # Check dataset expiration after specific duration
    coordinator.upload('pkg-a', '1.0', 'gen-ab1', now=0, tvm_id=1)
    coordinator.check_exists('pkg-a', '1.0')

    coordinator.http_post('/debug/PurgeExpiredVersions', now=10)
    coordinator.check_exists('pkg-a', '1.0')

    coordinator.upload('pkg-a', '2.0', 'gen-ab1', now=60, tvm_id=1)
    coordinator.check_exists('pkg-a', '1.0')
    coordinator.check_exists('pkg-a', '2.0')

    coordinator.http_post('/debug/PurgeExpiredVersions', now=130)
    coordinator.check_exists('pkg-a', '1.0')
    coordinator.check_exists('pkg-a', '2.0')

    coordinator.http_post('/debug/PurgeExpiredVersions', now=310)
    coordinator.check_exists('pkg-a', '1.0', existency=False)
    coordinator.check_exists('pkg-a', '2.0')

    coordinator.http_post('/debug/PurgeExpiredVersions', now=370)
    coordinator.check_exists('pkg-a', '1.0', existency=False)
    coordinator.check_exists('pkg-a', '2.0', existency=False)

    # Check dataset expiration after specific version count
    coordinator.upload('pkg-b', '1.0', 'gen-ab1', now=0, tvm_id=1)
    coordinator.http_post('/debug/PurgeExpiredVersions', now=1)
    coordinator.check_exists('pkg-b', '1.0')

    coordinator.upload('pkg-b:X', '2.0', 'gen-ab1', now=2, tvm_id=1)
    coordinator.http_post('/debug/PurgeExpiredVersions', now=3)
    coordinator.check_exists('pkg-b', '1.0')
    coordinator.check_exists('pkg-b:X', '2.0')

    coordinator.upload('pkg-b:Y', '3.0', 'gen-ab1', now=4, tvm_id=1)
    coordinator.http_post('/debug/PurgeExpiredVersions', now=5)
    coordinator.check_exists('pkg-b', '1.0')
    coordinator.check_exists('pkg-b:X', '2.0')
    coordinator.check_exists('pkg-b:Y', '3.0')

    coordinator.upload('pkg-b:X', '4.0', 'gen-ab1', now=6, tvm_id=1)
    coordinator.http_post('/debug/PurgeExpiredVersions', now=7)
    coordinator.check_exists('pkg-b', '1.0')
    coordinator.check_exists('pkg-b:X', '2.0')
    coordinator.check_exists('pkg-b:X', '4.0')
    coordinator.check_exists('pkg-b:Y', '3.0')

    coordinator.upload('pkg-b:Y', '5.0', 'gen-ab1', now=8, tvm_id=1)
    coordinator.http_post('/debug/PurgeExpiredVersions', now=9)
    coordinator.check_exists('pkg-b', '1.0')
    coordinator.check_exists('pkg-b:X', '2.0')
    coordinator.check_exists('pkg-b:X', '4.0')
    coordinator.check_exists('pkg-b:Y', '3.0')
    coordinator.check_exists('pkg-b:Y', '5.0')

    coordinator.upload('pkg-b:X', '6.0', 'gen-ab1', now=10, tvm_id=1)
    coordinator.http_post('/debug/PurgeExpiredVersions', now=11)
    coordinator.check_exists('pkg-b', '1.0')
    coordinator.check_exists('pkg-b:X', '2.0', existency=False)
    coordinator.check_exists('pkg-b:X', '4.0')
    coordinator.check_exists('pkg-b:X', '6.0')
    coordinator.check_exists('pkg-b:Y', '3.0')
    coordinator.check_exists('pkg-b:Y', '5.0')

    coordinator.upload('pkg-b:Y', '7.0', 'gen-ab1', now=12, tvm_id=1)
    coordinator.http_post('/debug/PurgeExpiredVersions', now=13)
    coordinator.check_exists('pkg-b', '1.0')
    coordinator.check_exists('pkg-b:X', '2.0', existency=False)
    coordinator.check_exists('pkg-b:X', '4.0')
    coordinator.check_exists('pkg-b:X', '6.0')
    coordinator.check_exists('pkg-b:Y', '3.0', existency=False)
    coordinator.check_exists('pkg-b:Y', '5.0')
    coordinator.check_exists('pkg-b:Y', '7.0')
