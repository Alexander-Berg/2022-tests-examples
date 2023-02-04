from contextlib import contextmanager
import json
from unittest import mock
import uuid

import pytest

from billing.monthclosing.operations.instantiate_json_artifact.lib.main import (
    main,
    r_objs,
)


@contextmanager
def does_not_raise():
    yield


def patched_main(
    environment,
    token_file,
    artifact_path,
    filename,
    quota_project,
    time_to_live,
    pre_check_allowed_statuses,
    file_input,
    raw_input_,
):
    args = [
        __name__,
        '-e',
        environment,
        '-t',
        str(token_file),
        '-a',
        artifact_path,
        '-f',
        filename,
        '-q',
        quota_project,
        '-ttl',
        str(time_to_live),
    ]
    if pre_check_allowed_statuses:
        args.extend(['-pcas', *pre_check_allowed_statuses])
    if file_input:
        args.extend(['-fi', str(file_input)])
    if raw_input_:
        args.extend(['-ri', raw_input_])

    with mock.patch('sys.argv', args):
        main()

    return


@pytest.mark.parametrize(
    'pre_check_allowed_statuses, file_input, raw_input_, last_instance, storage_metadata, storage_data, expectation, error',
    [
        ([], None, None, None, None, None, pytest.raises(Exception), 'No data source',),
        ([], {'id': 'test'}, None, None, None, None, does_not_raise(), None),
        ([], None, {'id': 'test'}, None, None, None, does_not_raise(), None),
        (
            [],
            {'id': 'file_test'},
            {'id': 'raw_test'},
            None,
            None,
            None,
            does_not_raise(),
            None,
        ),
        (
            [r_objs.ArtifactInstanceStatus.ACTIVE],
            {'id': 'test'},
            None,
            mock.Mock(
                metadata=mock.Mock(dict_obj={'value': '0'}),
                status=r_objs.ArtifactInstanceStatus.ACTIVE,
            ),
            {'dataType': 'text', 'storageUrl': '0'},
            {'id': 'test'},
            pytest.raises(Exception),
            'Incorrect data type',
        ),
        (
            [],
            {'id': 'test'},
            None,
            mock.Mock(
                metadata=mock.Mock(dict_obj={'value': '0'}),
                status=r_objs.ArtifactInstanceStatus.ACTIVE,
            ),
            {'dataType': 'text', 'storageUrl': '0'},
            {'id': 'test'},
            does_not_raise(),
            None,
        ),
        (
            [r_objs.ArtifactInstanceStatus.ACTIVE],
            {'id': 'test'},
            None,
            mock.Mock(
                metadata=mock.Mock(dict_obj={'value': '0'}),
                status=r_objs.ArtifactInstanceStatus.ACTIVE,
            ),
            {'dataType': 'json', 'storageUrl': '0'},
            {'id': 'test'},
            does_not_raise(),
            None,
        ),
        (
            [
                r_objs.ArtifactInstanceStatus.ACTIVE,
                r_objs.ArtifactInstanceStatus.DEPRECATED,
            ],
            None,
            {'id': 'test'},
            mock.Mock(
                metadata=mock.Mock(dict_obj={'value': '0'}),
                status=r_objs.ArtifactInstanceStatus.ACTIVE,
            ),
            {'dataType': 'json', 'storageUrl': '0'},
            {'id': 'test'},
            does_not_raise(),
            None,
        ),
        (
            [r_objs.ArtifactInstanceStatus.DEPRECATED],
            {'id': 'file_test'},
            {'id': 'raw_test'},
            mock.Mock(
                metadata=mock.Mock(dict_obj={'value': '0'}),
                status=r_objs.ArtifactInstanceStatus.ACTIVE,
            ),
            {'dataType': 'json', 'storageUrl': '0'},
            {'id': 'file_test'},
            does_not_raise(),
            None,
        ),
        (
            [r_objs.ArtifactInstanceStatus.ACTIVE],
            {'id': 'test'},
            None,
            mock.Mock(
                metadata=mock.Mock(dict_obj={'value': '0'}),
                status=r_objs.ArtifactInstanceStatus.DEPRECATED,
            ),
            {'dataType': 'json', 'storageUrl': '0'},
            {'id': 'test_prev'},
            does_not_raise(),
            None,
        ),
        (
            [r_objs.ArtifactInstanceStatus.ACTIVE],
            None,
            {'id': 'test'},
            mock.Mock(
                metadata=mock.Mock(dict_obj={'value': '0'}),
                status=r_objs.ArtifactInstanceStatus.ACTIVE,
            ),
            {'dataType': 'json', 'storageUrl': '0'},
            {'id': 'test_prev'},
            does_not_raise(),
            None,
        ),
        (
            [r_objs.ArtifactInstanceStatus.ACTIVE],
            {'id': 'file_test'},
            {'id': 'raw_test'},
            mock.Mock(
                metadata=mock.Mock(dict_obj={'value': '0'}),
                status=r_objs.ArtifactInstanceStatus.ACTIVE,
            ),
            {'dataType': 'json', 'storageUrl': '0'},
            {'id': 'file_test_prev'},
            does_not_raise(),
            None,
        ),
        (
            [r_objs.ArtifactInstanceStatus.ACTIVE],
            {'id': 'file_test'},
            {'id': 'raw_test'},
            mock.Mock(
                metadata=mock.Mock(dict_obj={'value': '0'}),
                status=r_objs.ArtifactInstanceStatus.ACTIVE,
            ),
            {'dataType': 'json'},
            {'id': 'file_test'},
            does_not_raise(),
            None,
        ),
    ],
)
def test_main(
    tmp_path,
    pre_check_allowed_statuses,
    file_input,
    raw_input_,
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
    artifact_path = '/test'
    filename = 'data'
    quota_project = 'my little quota'
    time_to_live = 1

    file_input_ = None
    if file_input:
        file_input_ = tmp_path / 'file_input'
        with open(file_input_, 'w') as f:
            json.dump(file_input, f)

    raw_input__ = None
    if raw_input_:
        raw_input__ = json.dumps(raw_input_)

    with expectation as exc, mock.patch(
        'reactor_client.reactor_api.ArtifactInstanceEndpoint'
    ) as aie_mock, mock.patch(
        'billing.monthclosing.operations.instantiate_json_artifact.lib.main.NirvanaApi'
    ) as nirvana_api_mock:
        artifact_instance_endpoint = aie_mock.return_value
        artifact_instance_endpoint.last = lambda artifact_identifier: last_instance
        nirvana_api = nirvana_api_mock.return_value
        nirvana_api.get_data = lambda data_id: storage_metadata

        data = {}
        if storage_metadata and 'storageUrl' in storage_metadata:
            data[storage_metadata['storageUrl']] = mock.Mock(json=lambda: storage_data)
        uuids = []

        def get_from_storage(storage_url):
            return data[storage_url]

        def create_data(filename, data_type, quota_project, ttl_days):
            uuid_ = str(uuid.uuid4())
            data[uuid_] = None
            uuids.append(uuid_)
            return uuid_

        def upload_data_multipart(data_id, upload_parameters, stream_obj, filename):
            assert not data[data_id]
            data[data_id] = mock.Mock(json=lambda: json.load(stream_obj))

        nirvana_api.get_from_storage = get_from_storage
        nirvana_api.create_data = create_data
        nirvana_api.upload_data_multipart = upload_data_multipart

        patched_main(
            environment,
            token_file,
            artifact_path,
            filename,
            quota_project,
            time_to_live,
            list(map(lambda e: e.name, pre_check_allowed_statuses)),
            file_input_,
            raw_input__,
        )

    if error:
        assert error in str(exc)
    else:
        content = file_input or raw_input_
        if content:
            if (
                storage_data
                and content == storage_data
                and last_instance.status in pre_check_allowed_statuses
                and 'storageUrl' in storage_metadata
            ):
                assert not uuids
            else:
                assert len(uuids) == 1
                (created_uuid,) = uuids
                if file_input:
                    assert file_input == data[created_uuid].json()
                else:
                    assert raw_input_ == data[created_uuid].json()
