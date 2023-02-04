from contextlib import contextmanager
import json
from unittest import mock

import pytest

from billing.monthclosing.operations.log_tariff_interim_yt_wait.lib.main import (
    main,
    r_objs,
)


@contextmanager
def does_not_raise():
    yield


def patched_main(
    environment,
    token_file,
    artifact_id,
    delay,
    allowed_statuses,
    input,
    output,
):
    args = [
        __name__,
        '-e',
        environment,
        '-t',
        str(token_file),
        '-a',
        str(artifact_id),
        '-d',
        str(delay),
        '-as',
        *allowed_statuses,
        '-i',
        str(input),
        '-o',
        str(output),
    ]

    with mock.patch('sys.argv', args):
        main()

    return


@pytest.mark.parametrize(
    'log_tariff_meta, allowed_statuses, last_instance, storage_metadata, storage_data, expectation, error',
    [
        (
            {},
            [r_objs.ArtifactInstanceStatus.ACTIVE],
            mock.Mock(
                metadata=mock.Mock(dict_obj={'value': '0'}),
                status=r_objs.ArtifactInstanceStatus.ACTIVE,
            ),
            {'dataType': 'csv', 'storageUrl': '0'},
            {},
            pytest.raises(Exception),
            'Incorrect data type',
        ),
        (
            {},
            [
                r_objs.ArtifactInstanceStatus.ACTIVE,
                r_objs.ArtifactInstanceStatus.CREATED,
            ],
            mock.Mock(
                metadata=mock.Mock(dict_obj={'value': '0'}),
                status=r_objs.ArtifactInstanceStatus.ACTIVE,
            ),
            {'dataType': 'csv', 'storageUrl': '0'},
            {},
            pytest.raises(Exception),
            'Incorrect data type',
        ),
        (
            {
                'log_interval': {
                    'topics': [
                        {
                            'topic': 'yabs',
                            'cluster': 'hahn',
                            'partitions': [
                                {'partition': 0, 'first_offset': 0, 'next_offset': 1}
                            ]
                        }
                    ]
                }
            },
            [
                r_objs.ArtifactInstanceStatus.ACTIVE,
                r_objs.ArtifactInstanceStatus.DEPRECATED,
            ],
            mock.Mock(
                metadata=mock.Mock(dict_obj={'value': '0'}),
                status=r_objs.ArtifactInstanceStatus.DEPRECATED,
            ),
            {'dataType': 'json', 'storageUrl': '0'},
            {
                'log_interval': {
                    'topics': [
                        {
                            'topic': 'yabs',
                            'cluster': 'hahn',
                            'partitions': [
                                {'partition': 0, 'first_offset': 0, 'next_offset': 1}
                            ]
                        }
                    ]
                }
            },
            does_not_raise(),
            None,
        ),
        (
            {
                'log_interval': {
                    'topics': [
                        {
                            'topic': 'yabs',
                            'cluster': 'hahn',
                            'partitions': [
                                {'partition': 0, 'first_offset': 1, 'next_offset': 2}
                            ]
                        },
                        {
                            'topic': 'oneshot',
                            'cluster': 'arnold',
                            'partitions': [
                                {'partition': 1, 'first_offset': 1, 'next_offset': 100}
                            ]
                        }
                    ]
                }
            },
            [r_objs.ArtifactInstanceStatus.ACTIVE],
            mock.Mock(
                metadata=mock.Mock(dict_obj={'value': '0'}),
                status=r_objs.ArtifactInstanceStatus.ACTIVE,
            ),
            {'dataType': 'json', 'storageUrl': '0'},
            {
                'log_interval': {
                    'topics': [
                        {
                            'topic': 'yabs',
                            'cluster': 'hahn',
                            'partitions': [
                                {'partition': 0, 'first_offset': 0, 'next_offset': 100}
                            ]
                        },
                        {
                            'topic': 'oneshot',
                            'cluster': 'arnold',
                            'partitions': [
                                {'partition': 1, 'first_offset': 10, 'next_offset': 100}
                            ]
                        }
                    ]
                }
            },
            does_not_raise(),
            None,
        ),
        (
            {
                'log_interval': {
                    'topics': [
                        {
                            'topic': 'yabs',
                            'cluster': 'hahn',
                            'partitions': [
                                {'partition': 0, 'first_offset': 1, 'next_offset': 2}
                            ]
                        }
                    ]
                }
            },
            [r_objs.ArtifactInstanceStatus.ACTIVE],
            mock.Mock(
                metadata=mock.Mock(dict_obj={'value': '0'}),
                status=r_objs.ArtifactInstanceStatus.ACTIVE,
            ),
            {'dataType': 'json', 'storageUrl': '0'},
            {
                'log_interval': {
                    'topics': [
                        {
                            'topic': 'yabs',
                            'cluster': 'hahn',
                            'partitions': [
                                {'partition': 0, 'first_offset': 0, 'next_offset': 100},
                                {
                                    'partition': 1,
                                    'first_offset': 200,
                                    'next_offset': 300,
                                },
                            ]
                        }
                    ]
                }
            },
            does_not_raise(),
            None,
        ),
    ],
)
def test_main(
    tmp_path,
    log_tariff_meta,
    allowed_statuses,
    last_instance,
    storage_metadata,
    storage_data,
    expectation,
    error,
):
    environment = 'testing'
    token_file = tmp_path / 'token_file'
    with open(token_file, 'w') as f:
        pass
    artifact_id = 0
    delay = 1
    input_file = tmp_path / 'input_file'
    with open(input_file, 'w') as f:
        json.dump(log_tariff_meta, f)
    output_file = tmp_path / 'output_file'
    with open(output_file, 'w') as f:
        json.dump(storage_data, f)

    with expectation as exc, mock.patch(
        'reactor_client.reactor_api.ArtifactInstanceEndpoint'
    ) as aie_mock, mock.patch(
        'billing.monthclosing.operations.wait_json_artifact.lib.main.NirvanaApi'
    ) as nirvana_api_mock:
        artifact_instance_endpoint = aie_mock.return_value
        artifact_instance_endpoint.last = lambda artifact_identifier: last_instance
        nirvana_api = nirvana_api_mock.return_value
        nirvana_api.get_data = lambda data_id: storage_metadata

        data = {}
        if storage_metadata:
            data[storage_metadata['storageUrl']] = mock.Mock(json=lambda: storage_data)

        def get_from_storage(storage_url):
            return data[storage_url]

        nirvana_api.get_from_storage = get_from_storage

        patched_main(
            environment,
            token_file,
            artifact_id,
            delay,
            map(lambda e: e.name, allowed_statuses),
            input_file,
            output_file,
        )

    if error:
        assert error in str(exc)
    else:
        with open(output_file, 'r') as f:
            output = json.load(f)
        assert output == storage_data
