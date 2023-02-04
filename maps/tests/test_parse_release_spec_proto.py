import typing as tp
from dataclasses import dataclass

import pytest
from google.protobuf import json_format

from maps.infra.sedem.common.release.garden.release_spec import (
    GardenModule,
    GardenReleaseSpec,
)
from maps.infra.sedem.common.release.nanny.release_spec import (
    DockerImage,
    NannyEnvironmentSpec,
    NannyReleaseSpec,
    YavSecretSpec,
)
from maps.infra.sedem.common.release.sandbox.release_spec import (
    SandboxDeployUnitSpec,
    SandboxReleaseSpec,
    SandboxTaskParameter,
    YavSecretParameter,
)
from maps.infra.sedem.common.release.release_spec import (
    ReleaseSpec,
    YavSecret,
)
from maps.infra.sedem.proto import sedem_pb2


@dataclass
class ParseProtoTestCase:
    name: str
    json_proto: dict[str, tp.Any]
    expected_spec: ReleaseSpec

    def __str__(self) -> str:
        return self.name


PARSING_TESTS = [
    ParseProtoTestCase(
        name='nanny_spec',
        json_proto={
            'nanny': {
                'docker': {
                    'name': 'ubuntu',
                    'tag': 'latest',
                },
                'environments': [{
                    'name': 'stable',
                    'secrets': [{
                        'secret': {
                            'secret_id': 'sec-XXX',
                            'version_id': 'ver-YYY',
                            'key': 'oauth.token',
                        },
                        'env_var': 'OAUTH_TOKEN',
                    }, {
                        'secret': {
                            'secret_id': 'sec-AAA',
                            'version_id': 'ver-BBB',
                        },
                        'dir_name': 'secrets',
                    }],
                }],
            },
        },
        expected_spec=NannyReleaseSpec(
            docker=DockerImage(
                name='ubuntu',
                tag='latest',
            ),
            environments=[NannyEnvironmentSpec(
                name='stable',
                secrets=[
                    YavSecretSpec(
                        secret=YavSecret(
                            secret_id='sec-XXX',
                            version_id='ver-YYY',
                            key='oauth.token',
                        ),
                        env_var='OAUTH_TOKEN',
                    ),
                    YavSecretSpec(
                        secret=YavSecret(
                            secret_id='sec-AAA',
                            version_id='ver-BBB',
                        ),
                        dir_name='secrets',
                    )
                ],
            )],
        ),
    ),
    ParseProtoTestCase(
        name='garden_spec',
        json_proto={
            'garden': {
                'module': {
                    'name': 'ymapsdf',
                    'version': 'latest',
                },
            },
        },
        expected_spec=GardenReleaseSpec(
            module=GardenModule(
                name='ymapsdf',
                version='latest',
            ),
        ),
    ),
    ParseProtoTestCase(
        name='sandbox_spec',
        json_proto={
            'sandbox': {
                'task_type': 'EXAMPLE_TASK',
                'resource_id': '1234567',
                'deploy_units': [{
                    'name': 'stable_scheduler',
                    'secrets': [{
                        'name': 'secret',
                        'secret': {
                            'secret_id': 'sec-XXX',
                        },
                    }, {
                        'name': 'token',
                        'secret': {
                            'secret_id': 'sec-AAA',
                            'version_id': 'ver-BBB',
                            'key': 'oauth.token',
                        },
                    }, {
                        'name': 'unassigned',
                    }],
                    'parameters': [{
                        'name': 'string',
                        'jsonValue': '"some string"',
                    }, {
                        'name': 'unassigned',
                        'jsonValue': 'null',
                    }],
                }],
            },
        },
        expected_spec=SandboxReleaseSpec(
            task_type='EXAMPLE_TASK',
            resource_id='1234567',
            deploy_units=[SandboxDeployUnitSpec(
                name='stable_scheduler',
                secrets=[
                    YavSecretParameter(
                        name='secret',
                        secret=YavSecret(
                            secret_id='sec-XXX',
                        )
                    ),
                    YavSecretParameter(
                        name='token',
                        secret=YavSecret(
                            secret_id='sec-AAA',
                            version_id='ver-BBB',
                            key='oauth.token',
                        ),
                    ),
                    YavSecretParameter(
                        name='unassigned',
                    ),
                ],
                parameters=[
                    SandboxTaskParameter(
                        name='string',
                        jsonValue='"some string"',
                    ),
                    SandboxTaskParameter(
                        name='unassigned',
                        jsonValue='null',
                    ),
                ],
            )],
        ),
    ),
]


@pytest.mark.parametrize('case', PARSING_TESTS, ids=str)
def test_parse_proto_spec(case: ParseProtoTestCase) -> None:
    proto = sedem_pb2.ReleaseSpec()
    json_format.ParseDict(case.json_proto, proto)
    actual_spec = ReleaseSpec.parse_proto(proto)
    assert actual_spec == case.expected_spec
    actual_proto = case.expected_spec.to_proto()
    actual_json_proto = json_format.MessageToDict(
        actual_proto,
        preserving_proto_field_name=True,
    )
    assert actual_json_proto == case.json_proto
