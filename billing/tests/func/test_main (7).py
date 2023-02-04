from contextlib import contextmanager
import json
from unittest import mock
from datetime import datetime as dt

import pytest

from billing.monthclosing.operations.wait_event_artifact.lib.main import main, r_objs


@contextmanager
def does_not_raise():
    yield


class MockParams:
    def __init__(self, **params):
        self.params = params

    def get_parameters(self):
        return self.params


def patched_main(**params):
    with mock.patch('nirvana.job_context.context') as nv:
        nv.return_value = MockParams(**params)
        main()

    return


@pytest.mark.parametrize(
    'user_time, allowed_statuses, schema, last_instance, expectation, error',
    [
        (
            '2021-11-09 10:00:00',
            ['ACTIVE'],
            {'type': 'object', 'properties': {'id': {'type': 'string'}}},
            mock.Mock(
                metadata=mock.Mock(
                    type_='/yandex.reactor.artifact.IntArtifactValueProto',
                    dict_obj={'value': '1'},
                ),
                status=r_objs.ArtifactInstanceStatus.ACTIVE,
                user_time=dt(2021, 11, 9, 10),
            ),
            pytest.raises(RuntimeError),
            'Incorrect artifact type, expected: EventArtifactValueProto, got: IntArtifactValueProto',
        ),
        (
            '2021-11-09 10:00:00',
            ['ACTIVE'],
            {'type': 'object', 'properties': {'id': {'type': 'string'}}},
            mock.Mock(
                metadata=mock.Mock(
                    type_='/yandex.reactor.artifact.EventArtifactValueProto',
                    dict_obj={},
                ),
                status=r_objs.ArtifactInstanceStatus.ACTIVE,
                attributes=mock.Mock(key_value={'id': 'test'}),
                user_time=dt(2021, 11, 9, 10),
            ),
            does_not_raise(),
            None,
        ),
    ],
)
def test_main(user_time, allowed_statuses, schema, last_instance, expectation, error):
    params = {
        'user_time': user_time,
        'artifact_path': 0,
        'oauth_token': 'test',
        'delay': 1,
        'allowed_statuses': allowed_statuses,
        'environment': 'testing',
        'schema': json.dumps(schema),
    }

    with expectation as exc, mock.patch(
        'reactor_client.reactor_api.ArtifactInstanceEndpoint'
    ) as aie_mock:
        artifact_instance_endpoint = aie_mock.return_value
        artifact_instance_endpoint.last = lambda artifact_identifier: last_instance

        patched_main(**params)

    if error:
        assert error in str(exc)
