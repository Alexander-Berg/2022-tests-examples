import os
import pytest

from maps.infra.ecstatic.sandbox.reconfigurer.lib.reconfigure import run_reconfiguration, ReconfigurationStage


@pytest.fixture(scope='function')
def validate(tmpdir):
    def reconfigurer(config):
        config_file = os.path.join(tmpdir, 'ecstatic.conf')
        with open(config_file, 'w') as file:
            file.write(config)
        run_reconfiguration(
            mongo_uri='',
            stage=ReconfigurationStage.VALIDATE_SYNTAX,
            files=[config_file],
            installation='unstable'
        )
    return reconfigurer
