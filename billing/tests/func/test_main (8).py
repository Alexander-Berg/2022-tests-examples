from contextlib import contextmanager
import json
import jsonschema.exceptions
from unittest import mock

import pytest

from billing.monthclosing.operations.wait_json_artifact.lib.main import main, r_objs


@contextmanager
def does_not_raise():
    yield


def patched_main(
    environment, token_file, artifact_id, schema, delay, allowed_statuses, output,
):
    args = [
        __name__,
        '-e',
        environment,
        '-t',
        str(token_file),
        '-a',
        str(artifact_id),
        '-s',
        schema,
        '-d',
        str(delay),
        '-as',
        *allowed_statuses,
        '-o',
        str(output),
    ]

    with mock.patch('sys.argv', args):
        main()

    return


@pytest.mark.parametrize(
    'allowed_statuses, schema, last_instance, storage_metadata, storage_data, expectation',
    [
        (
            [r_objs.ArtifactInstanceStatus.ACTIVE],
            {'type': 'integer32'},
            mock.Mock(metadata=mock.Mock(dict_obj={'value': '0'})),
            {'dataType': 'json', 'storageUrl': '0'},
            {'id': 'test'},
            pytest.raises(jsonschema.exceptions.SchemaError),
        ),
        (
            [r_objs.ArtifactInstanceStatus.ACTIVE],
            {'type': 'object'},
            mock.Mock(
                metadata=mock.Mock(dict_obj={'value': '0'}),
                status=r_objs.ArtifactInstanceStatus.ACTIVE,
            ),
            {'dataType': 'csv', 'storageUrl': '0'},
            {'id': 'test'},
            pytest.raises(Exception, match='Incorrect data type'),
        ),
        (
            [
                r_objs.ArtifactInstanceStatus.ACTIVE,
                r_objs.ArtifactInstanceStatus.CREATED,
            ],
            {'type': 'object'},
            mock.Mock(
                metadata=mock.Mock(dict_obj={'value': '0'}),
                status=r_objs.ArtifactInstanceStatus.ACTIVE,
            ),
            {'dataType': 'csv', 'storageUrl': '0'},
            {'id': 'test'},
            pytest.raises(Exception, match='Incorrect data type'),
        ),
        (
            [r_objs.ArtifactInstanceStatus.ACTIVE],
            {'type': 'object', 'properties': {'id': {'type': 'string'}}},
            mock.Mock(
                metadata=mock.Mock(dict_obj={'value': '0'}),
                status=r_objs.ArtifactInstanceStatus.ACTIVE,
            ),
            {'dataType': 'json', 'storageUrl': '0', '404': True},
            {'id': 'test'},
            pytest.raises(Exception, match='we got 404'),
        ),
        (
            [
                r_objs.ArtifactInstanceStatus.ACTIVE,
                r_objs.ArtifactInstanceStatus.DEPRECATED,
            ],
            {'type': 'object'},
            mock.Mock(
                metadata=mock.Mock(dict_obj={'value': '0'}),
                status=r_objs.ArtifactInstanceStatus.DEPRECATED,
            ),
            {'dataType': 'json', 'storageUrl': '0'},
            {'id': 'test'},
            does_not_raise(),
        ),
        (
            [r_objs.ArtifactInstanceStatus.ACTIVE],
            {'type': 'object', 'properties': {'id': {'type': 'string'}}},
            mock.Mock(
                metadata=mock.Mock(dict_obj={'value': '0'}),
                status=r_objs.ArtifactInstanceStatus.ACTIVE,
            ),
            {'dataType': 'json', 'storageUrl': '0'},
            {'id': 'test'},
            does_not_raise(),
        ),
    ],
)
def test_main(
    tmp_path,
    allowed_statuses,
    schema,
    last_instance,
    storage_metadata,
    storage_data,
    expectation,
):
    environment = 'testing'
    token_file = tmp_path / 'token_file'
    with open(token_file, 'w') as f:
        pass
    artifact_id = 0
    delay = 1
    output_file = tmp_path / 'output_file'
    with open(output_file, 'w') as f:
        json.dump(storage_data, f)

    with expectation, mock.patch(
        'reactor_client.reactor_api.ArtifactInstanceEndpoint'
    ) as aie_mock, mock.patch(
        'billing.monthclosing.operations.wait_json_artifact.lib.main.NirvanaApi'
    ) as nirvana_api_mock:
        artifact_instance_endpoint = aie_mock.return_value
        artifact_instance_endpoint.last = lambda artifact_identifier: last_instance
        nirvana_api = nirvana_api_mock.return_value

        def get_data(data_id):
            if storage_metadata.get('404'):
                if storage_metadata['404'] == 'Exc':
                    storage_metadata['storageUrl'] = 'Exc'
                else:
                    storage_metadata['404'] = 'Exc'
            return storage_metadata

        nirvana_api.get_data = get_data

        data = {}
        if storage_metadata:
            if storage_metadata.get('404'):
                data[storage_metadata['storageUrl']] = mock.Mock(status_code=404)
            else:
                data[storage_metadata['storageUrl']] = mock.Mock(json=lambda: storage_data)

        def get_from_storage(storage_url):
            if storage_url == 'Exc':
                raise Exception('we got 404')
            return data[storage_url]

        nirvana_api.get_from_storage = get_from_storage

        patched_main(
            environment,
            token_file,
            artifact_id,
            json.dumps(schema),
            delay,
            map(lambda e: e.name, allowed_statuses),
            output_file,
        )

        with open(output_file, 'r') as f:
            output = json.load(f)
        assert output == storage_data
