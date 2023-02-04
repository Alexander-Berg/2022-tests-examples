import logging

from maps.infra.sedem.cli.tests.release.fixtures.conftest import sedem_fixture, rendering_fixture  # noqa
from maps.infra.sedem.machine.tests.integration_tests.fixtures.conftest import sedem_machine_fixture  # noqa
from maps.pylibs.fixtures.sandbox.tasks import (  # noqa
    BuildDockerAndReleaseDefinition, GardenUploadModuleBinaryDefinition
)

logger = logging.getLogger(__name__)
logger.setLevel(logging.DEBUG)
