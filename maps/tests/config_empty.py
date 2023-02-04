from maps.infra.ecstatic.sandbox.reconfigurer.lib.reconfigure import ReconfigureError
import pytest


def test_config_empty(mongo):
    # empty config should not be accepted
    with pytest.raises(ReconfigureError):
        mongo.reconfigure(config='data/empty.conf')
