from maps.infra.ecstatic.sandbox.reconfigurer.lib.reconfigure import ReconfigureError
import pytest


def test_tolerance_bounded(mongo):
    mongo.reconfigure(config='data/neg-tolerance.conf')
    with pytest.raises(ReconfigureError):
        mongo.reconfigure(config='data/neg-quorum.conf')
