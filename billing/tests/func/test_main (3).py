from contextlib import contextmanager
from unittest import mock

import pytest

from billing.monthclosing.operations.log_tariff_gap_yt_wait.lib.main import (
    main,
    r_objs,
)


@contextmanager
def does_not_raise():
    yield


def patched_main(
    environment,
    token_file,
    artifact_path_interim,
    artifact_path_processed,
    reaction_path,
    delay,
    allowed_statuses,
):
    args = [
        __name__,
        '-e',
        environment,
        '-t',
        str(token_file),
        '-ai',
        artifact_path_interim,
        '-ap',
        artifact_path_processed,
        '-r',
        reaction_path,
        '-d',
        str(delay),
        '-as',
        *allowed_statuses,
    ]

    with mock.patch('sys.argv', args):
        main()

    return


@pytest.mark.parametrize(
    'allowed_statuses, last_instances, storage_metadata, storage_data, reaction_instance_views, expectation, error',
    [
        (
            [r_objs.ArtifactInstanceStatus.ACTIVE],
            [
                mock.Mock(
                    metadata=mock.Mock(dict_obj={'value': '0'}),
                    status=r_objs.ArtifactInstanceStatus.ACTIVE,
                )
            ],
            [{'dataType': 'csv', 'storageUrl': '0'}],
            [{}],
            [[mock.Mock(status=r_objs.ReactionInstanceStatus.COMPLETED)]],
            pytest.raises(Exception),
            'Incorrect data type',
        ),
        (
            [
                r_objs.ArtifactInstanceStatus.ACTIVE,
                r_objs.ArtifactInstanceStatus.CREATED,
            ],
            [
                mock.Mock(
                    metadata=mock.Mock(dict_obj={'value': '0'}),
                    status=r_objs.ArtifactInstanceStatus.ACTIVE,
                )
            ],
            [{'dataType': 'csv', 'storageUrl': '0'}],
            [{}],
            [[mock.Mock(status=r_objs.ReactionInstanceStatus.COMPLETED)]],
            pytest.raises(Exception),
            'Incorrect data type',
        ),
        (
            [
                r_objs.ArtifactInstanceStatus.ACTIVE,
                r_objs.ArtifactInstanceStatus.DEPRECATED,
            ],
            [
                mock.Mock(
                    metadata=mock.Mock(dict_obj={'value': '0'}),
                    status=r_objs.ArtifactInstanceStatus.DEPRECATED,
                ),
                mock.Mock(
                    metadata=mock.Mock(dict_obj={'value': '1'}),
                    status=r_objs.ArtifactInstanceStatus.ACTIVE,
                ),
            ],
            [
                {'dataType': 'json', 'storageUrl': '0'},
                {'dataType': 'json', 'storageUrl': '1'},
            ],
            [
                {
                    'log_interval': {
                        'topics': [
                            {
                                'topic': 'yabs',
                                'cluster': 'hahn',
                                'partitions': [
                                    {
                                        'partition': 0,
                                        'first_offset': 2,
                                        'next_offset': 10,
                                    }
                                ],
                            }
                        ]
                    }
                },
                {
                    'log_interval': {
                        'topics': [
                            {
                                'topic': 'yabs',
                                'cluster': 'hahn',
                                'partitions': [
                                    {
                                        'partition': 0,
                                        'first_offset': 0,
                                        'next_offset': 10,
                                    }
                                ],
                            }
                        ]
                    }
                },
            ],
            [[mock.Mock(status=r_objs.ReactionInstanceStatus.COMPLETED)]],
            does_not_raise(),
            None,
        ),
        (
            [r_objs.ArtifactInstanceStatus.ACTIVE],
            [
                mock.Mock(
                    metadata=mock.Mock(dict_obj={'value': '0'}),
                    status=r_objs.ArtifactInstanceStatus.ACTIVE,
                ),
                mock.Mock(
                    metadata=mock.Mock(dict_obj={'value': '1'}),
                    status=r_objs.ArtifactInstanceStatus.ACTIVE,
                ),
            ]
            * 2,
            [
                {'dataType': 'json', 'storageUrl': '0'},
                {'dataType': 'json', 'storageUrl': '1'},
            ]
            * 2,
            [
                {
                    'log_interval': {
                        'topics': [
                            {
                                'topic': 'yabs',
                                'cluster': 'hahn',
                                'partitions': [
                                    {
                                        'partition': 0,
                                        'first_offset': 1,
                                        'next_offset': 100,
                                    }
                                ],
                            },
                            {
                                'topic': 'oneshot',
                                'cluster': 'arnold',
                                'partitions': [
                                    {
                                        'partition': 1,
                                        'first_offset': 1,
                                        'next_offset': 100,
                                    }
                                ],
                            },
                        ]
                    }
                },
                {
                    'log_interval': {
                        'topics': [
                            {
                                'topic': 'yabs',
                                'cluster': 'hahn',
                                'partitions': [
                                    {
                                        'partition': 0,
                                        'first_offset': 10,
                                        'next_offset': 100,
                                    }
                                ],
                            },
                            {
                                'topic': 'oneshot',
                                'cluster': 'arnold',
                                'partitions': [
                                    {
                                        'partition': 1,
                                        'first_offset': 10,
                                        'next_offset': 100,
                                    }
                                ],
                            },
                        ]
                    }
                },
            ]
            * 2,
            [
                [
                    mock.Mock(status=r_objs.ReactionInstanceStatus.TIMEOUT),
                    mock.Mock(status=r_objs.ReactionInstanceStatus.COMPLETED),
                ],
                [
                    mock.Mock(status=r_objs.ReactionInstanceStatus.COMPLETED),
                    mock.Mock(status=r_objs.ReactionInstanceStatus.TIMEOUT),
                    mock.Mock(status=r_objs.ReactionInstanceStatus.COMPLETED),
                ],
            ],
            does_not_raise(),
            None,
        ),
        (
            [r_objs.ArtifactInstanceStatus.ACTIVE],
            [
                mock.Mock(
                    metadata=mock.Mock(dict_obj={'value': '0'}),
                    status=r_objs.ArtifactInstanceStatus.ACTIVE,
                ),
                mock.Mock(
                    metadata=mock.Mock(dict_obj={'value': '1'}),
                    status=r_objs.ArtifactInstanceStatus.ACTIVE,
                ),
            ]
            * 2,
            [
                {'dataType': 'json', 'storageUrl': '0'},
                {'dataType': 'json', 'storageUrl': '1'},
            ]
            * 2,
            [
                {
                    'log_interval': {
                        'topics': [
                            {
                                'topic': 'yabs',
                                'cluster': 'hahn',
                                'partitions': [
                                    {
                                        'partition': 0,
                                        'first_offset': 1,
                                        'next_offset': 100,
                                    }
                                ],
                            },
                            {
                                'topic': 'oneshot',
                                'cluster': 'arnold',
                                'partitions': [
                                    {
                                        'partition': 1,
                                        'first_offset': 1,
                                        'next_offset': 90,
                                    }
                                ],
                            },
                        ]
                    }
                },
                {
                    'log_interval': {
                        'topics': [
                            {
                                'topic': 'yabs',
                                'cluster': 'hahn',
                                'partitions': [
                                    {
                                        'partition': 0,
                                        'first_offset': 10,
                                        'next_offset': 80,
                                    }
                                ],
                            },
                            {
                                'topic': 'oneshot',
                                'cluster': 'arnold',
                                'partitions': [
                                    {
                                        'partition': 1,
                                        'first_offset': 10,
                                        'next_offset': 70,
                                    }
                                ],
                            },
                        ]
                    }
                },
                {
                    'log_interval': {
                        'topics': [
                            {
                                'topic': 'yabs',
                                'cluster': 'hahn',
                                'partitions': [
                                    {
                                        'partition': 0,
                                        'first_offset': 1,
                                        'next_offset': 100,
                                    }
                                ],
                            },
                            {
                                'topic': 'oneshot',
                                'cluster': 'arnold',
                                'partitions': [
                                    {
                                        'partition': 1,
                                        'first_offset': 1,
                                        'next_offset': 90,
                                    }
                                ],
                            },
                        ]
                    }
                },
                {
                    'log_interval': {
                        'topics': [
                            {
                                'topic': 'yabs',
                                'cluster': 'hahn',
                                'partitions': [
                                    {
                                        'partition': 0,
                                        'first_offset': 10,
                                        'next_offset': 100,
                                    }
                                ],
                            },
                            {
                                'topic': 'oneshot',
                                'cluster': 'arnold',
                                'partitions': [
                                    {
                                        'partition': 1,
                                        'first_offset': 10,
                                        'next_offset': 90,
                                    }
                                ],
                            },
                        ]
                    }
                },
            ],
            [
                [mock.Mock(status=r_objs.ReactionInstanceStatus.COMPLETED)],
                [mock.Mock(status=r_objs.ReactionInstanceStatus.COMPLETED)],
            ],
            does_not_raise(),
            None,
        ),
    ],
)
def test_main(
    tmp_path,
    allowed_statuses,
    last_instances,
    storage_metadata,
    storage_data,
    reaction_instance_views,
    expectation,
    error,
):
    environment = 'testing'
    token_file = tmp_path / 'token_file'
    with open(token_file, 'w'):
        pass
    artifact_path_interim = '0'
    artifact_path_processed = '0'
    reaction_path = '0'
    delay = 1

    with expectation as exc, mock.patch(
        'reactor_client.reactor_api.ArtifactInstanceEndpoint'
    ) as aie_mock, mock.patch(
        'reactor_client.reactor_api.ReactionInstanceEndpoint'
    ) as rie_mock, mock.patch(
        'billing.monthclosing.operations.wait_json_artifact.lib.main.NirvanaApi'
    ) as nirvana_api_mock:
        artifact_instance_endpoint = aie_mock.return_value
        artifact_instance_endpoint.last.side_effect = last_instances
        reaction_instance_endpoint = rie_mock.return_value
        reaction_instance_endpoint.list_statuses.side_effect = reaction_instance_views
        nirvana_api = nirvana_api_mock.return_value
        nirvana_api.get_data.side_effect = storage_metadata

        storage = {
            metadata['storageUrl']: mock.Mock(json=(lambda data=data: (lambda: data))())
            for metadata, data in zip(storage_metadata, storage_data)
        }

        def get_from_storage(storage_url):
            return storage[storage_url]

        nirvana_api.get_from_storage = get_from_storage

        patched_main(
            environment,
            token_file,
            artifact_path_interim,
            artifact_path_processed,
            reaction_path,
            delay,
            map(lambda e: e.name, allowed_statuses),
        )

    if error:
        assert error in str(exc)
