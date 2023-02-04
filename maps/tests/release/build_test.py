import logging
import typing as tp
from contextlib import nullcontext
from dataclasses import dataclass
from unittest.mock import Mock

import pytest

from maps.infra.sedem.cli.commands.release import release_build
from maps.infra.sedem.client.machine_api import MachineApi, MachineApiError, sedem_pb2
from maps.pylibs.fixtures.arcadia.fixture import ArcadiaFixture
from maps.infra.sedem.cli.tests.release.utils.test_data import (
    assert_click_result
)

logger = logging.getLogger(__name__)
logger.setLevel(logging.DEBUG)


@dataclass
class BuildWithCITestCase:
    service_path: str
    mock_response: Mock
    expected_error: tp.Optional[str]


@pytest.mark.parametrize('case', [
    BuildWithCITestCase(
        service_path='maps/garden/modules/backa_export',
        mock_response=Mock(
            return_value=sedem_pb2.ReleaseCandidateBuildResponse(launch_url='https://ci.url'),
        ),
        expected_error=None,
    ),
    BuildWithCITestCase(
        service_path='maps/infra/ecstatic/sandbox',
        mock_response=Mock(
            return_value=sedem_pb2.ReleaseCandidateBuildResponse(launch_url='https://ci.url'),
        ),
        expected_error=None,
    ),
    BuildWithCITestCase(
        service_path='maps/infra/ecstatic/sandbox',
        mock_response=Mock(
            side_effect=MachineApiError('User john-doe is not in abc service maps-core-sedem-machine'),
        ),
        expected_error='User .+ is not in abc service .+',
    ),
])
def test_build_with_ci(fixture_factory, monkeypatch, case):
    arcadia_fixture: ArcadiaFixture = fixture_factory(ArcadiaFixture)

    revision_to_build = 100
    arcadia_fixture.set_head_revision(revision_to_build)

    monkeypatch.setattr(
        MachineApi,
        'candidate_build',
        case.mock_response,
    )

    if case.expected_error is not None:
        handle_error = pytest.raises(Exception, match=case.expected_error)
    else:
        handle_error = nullcontext()

    with handle_error:
        assert_click_result(release_build, [case.service_path, f'r{revision_to_build}'])
