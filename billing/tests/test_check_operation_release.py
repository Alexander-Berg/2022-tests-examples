from unittest.mock import MagicMock
from pathlib import Path
import asyncio
from unittest.mock import AsyncMock

from arc.api.public.repo_pb2 import ChangelistResponse
import pytest
from aiohttp.web import HTTPServerError

from billing.tasklets.nirvana.packing.impl import (
    get_ya_make_artifact_path,
    parse_operations_from_arc,
    NvOperation,
    arc_revision,
    make_import_parameters,
    iter_proper_changes,
    wait_for_import,
)
from billing.tasklets.nirvana.packing.impl.nirvana_client import retry, ExceptionToFail
from conftest import MockResponse


@pytest.mark.parametrize(
    'folder,artifact_path,ya_make_path',
    [
        (Path('x'), Path('x/folder/folder'), 'ya_make1.txt'),
        (Path('y'), Path('y/folder/ololo'), 'ya_make2.txt'),
        (Path('z'), Path('z/folder/ololo'), 'ya_make3.txt'),
    ],
)
def test_get_ya_artifact_path(folder, artifact_path, ya_make_path, mock_arc_client):
    actual_path = get_ya_make_artifact_path(
        mock_arc_client.expect_read(f"{folder}/folder/ya.make", arc_revision(42), ya_make_path), 42, folder / "folder"
    )

    assert artifact_path == actual_path


def test_parse_operations(mock_arc_client):
    expected = [
        NvOperation(
            operation_code="dj_operation",
            operation_version="0.1.22",
            declaring_file_path=Path("path/path/script.py"),
            revision=43,
            ya_make_folder_path=Path("dj/nirvana/operations/dj/dj_operation"),
            ya_make_build_artifact_path=Path("dj/nirvana/operations/dj/dj_operation/dj_operation"),
            script_method="dj/dj_operation",
            nirvana_quota="ml-marines",
            vcs_token_nirvana_secret_name="solozobov_vcs_token",
            operation_representing_entity_name='my_operation',
            sandbox_build_params={'sandboxGroup': 'my_group'},
        ),
        NvOperation(
            operation_code="dj_operation2",
            operation_version="4.1.22",
            declaring_file_path=Path("path/path/script.py"),
            revision=43,
            ya_make_build_artifact_path=Path("path/path/path"),
            script_method="dj/dj_operation",
            operation_representing_entity_name='my_operation2',
        ),
        NvOperation(
            operation_code="dj_operation2_5",
            operation_version="4.1.22",
            declaring_file_path=Path("path/path/script.py"),
            revision=43,
            ya_make_build_artifact_path=Path("path/path/path"),
            script_method="dj/dj_operation",
            operation_representing_entity_name='my_operation2_5',
        ),
        NvOperation(
            operation_code="dj_operation3",
            operation_version="4.1.22",
            declaring_file_path=Path("path/path/script.py"),
            revision=43,
            script_method="dj/dj_operation",
            operation_representing_entity_name='my_operation3',
        ),
    ]
    actual = list(
        parse_operations_from_arc(
            mock_arc_client.expect_read("path/path/script.py", arc_revision(43), "operations_with_autopack.py"),
            Path("path/path/script.py"),
            43,
        )
    )

    assert expected == actual
    assert actual[1].ya_make_folder_path == actual[2].ya_make_folder_path == Path('path/path')


def test_default_scrip_method(mock_arc_client):
    expected = [
        NvOperation(
            operation_code="my_operation",
            operation_version="0.1.22",
            declaring_file_path=Path("path/path/script.py"),
            revision=43,
            script_method="path.path.script.my_operation",
            operation_representing_entity_name='my_operation',
        ),
        NvOperation(
            operation_code="nope_operation",
            operation_version="0.1.22",
            declaring_file_path=Path("path/path/script.py"),
            revision=43,
            script_method="path.path.script.NopeOperation",
            operation_representing_entity_name='NopeOperation',
        ),
    ]

    actual = list(
        parse_operations_from_arc(
            mock_arc_client.expect_read("path/path/script.py", arc_revision(43), "empty_script_method.py"),
            Path("path/path/script.py"),
            43,
        )
    )

    assert expected == actual


# TODO: change the way to remove different revision operation and fix the test.
# def test_fetch_correct_files_from_different_revisions(mock_arc_client):
#     files_to_test = [
#         ChangelistResponse.Change(Path='file_to_modify', Change=ChangelistResponse.ChangeType.Modify),
#         ChangelistResponse.Change(Path='new_file', Change=ChangelistResponse.ChangeType.Add),
#     ]
#     mock_arc_client.expect_read('file_to_modify', arc_revision(123), 'different_revisions/v2/file_to_modify.py')
#     mock_arc_client.expect_read('new_file', arc_revision(123), 'different_revisions/v2/new_file.py')
#     # mock_arc_client.expect_read('file_to_modify', arc_revision(122), 'different_revisions/v1/file_to_modify.py')
#
#     with patch('billing.tasklets.nirvana.packing.impl.iter_proper_changes', return_value=files_to_test):
#         operations = fetch_operations_from_commit(mock_arc_client, 123, {'different_revisions/**/*.py'})
#
#     for op in ('my_modified_operation', '0.0.2'), ('my_new_operation', '0.0.1'), ('my_other_new_operation', '0.0.1'):
#         assert op in operations


def test_proper_quota_in_request(mock_arc_client):
    op_with_quota, op_without_quota, *_ = parse_operations_from_arc(
        mock_arc_client.expect_read("path/path/script.py", arc_revision(43), "operations_with_autopack.py"),
        Path("path/path/script.py"),
        43,
    )

    assert make_import_parameters(op_with_quota, 'default')['metaParams']['quotaProjectId'] == 'ml-marines'
    assert make_import_parameters(op_without_quota, 'default')['metaParams']['quotaProjectId'] == 'default'


def test_artifact_in_request():
    op = NvOperation(
        operation_code="dj_operation3",
        operation_version="4.1.22",
        declaring_file_path=Path("dj/folder/nested/dj_operation"),
        revision=43,
        script_method="method",
        ya_make_build_artifact_path=Path("dj/dj_operation/art"),
    )

    assert make_import_parameters(op, 'quota')['vcsReference']["path"] == "arcadia/dj/dj_operation"


def test_parse_operation_with_prefix(mock_arc_client):
    expected = [
        NvOperation(
            operation_code="dj_operation",
            operation_version="0.1.22",
            script_method="dj/my_operation",
            declaring_file_path=Path("path/path/script.py"),
            ya_make_build_artifact_path=None,
            revision=43,
            operation_representing_entity_name='my_operation',
        ),
        NvOperation(
            operation_code="dj_operation2",
            script_method="dj/my_operation2",
            operation_version="4.1.22",
            declaring_file_path=Path("path/path/script.py"),
            ya_make_build_artifact_path=None,
            revision=43,
            operation_representing_entity_name='my_operation2',
        ),
    ]
    actual = list(
        parse_operations_from_arc(
            mock_arc_client.expect_read("path/path/script.py", arc_revision(43), "operations_with_prefix.py"),
            Path("path/path/script.py"),
            43,
        )
    )

    assert expected == list(actual)
    assert expected[0].ya_make_folder_path == expected[1].ya_make_folder_path == Path("path/path")


def test_pack_only_in_sub_dirs_of_config(mock_arc_client):
    changes = MagicMock()
    changes.Changes = [
        ChangelistResponse.Change(Path='my/some/path/file1.py', Change=ChangelistResponse.ChangeType.Modify),
        ChangelistResponse.Change(Path='my/some/path/file2.py', Change=ChangelistResponse.ChangeType.Modify),
        ChangelistResponse.Change(Path='my/other/path/file1.py', Change=ChangelistResponse.ChangeType.Modify),
    ]
    mock_arc_client.get_changelist = MagicMock(return_value=[changes])

    paths = list(p.Path for p in iter_proper_changes(mock_arc_client, 42, {'my/some/**/*.py'}))

    assert paths == ['my/some/path/file1.py', 'my/some/path/file2.py']


def test_wait_import(mock_nv_client):
    mock_nv_client._request_with_retry = AsyncMock(
        side_effect=[
            {'result': {'state': {'status': 'waiting'}}},
            {'result': {'state': {'status': 'completed', 'result': 'success'}, 'operationId': 'fake-id'}},
        ]
    )

    asyncio.run(wait_for_import(mock_nv_client, 'fake-id'))

    assert mock_nv_client._request_with_retry.await_count == 2


def test_raise_on_import_error(mock_nv_client):
    mock_nv_client._request_with_retry = AsyncMock(
        side_effect=[{'result': {'state': {'status': 'completed', 'result': 'failure'}}}]
    )

    message = r'Import failed. See more https://nirvana.yandex-team.ru/operations/imports\?page=1&selectedId=fake-id'
    with pytest.raises(RuntimeError, match=message):
        asyncio.run(wait_for_import(mock_nv_client, 'fake-id'))


def test_retry_unknown_errors():
    cnt = 0

    @retry
    async def func():
        nonlocal cnt
        if cnt == 0:
            Exception('error')
        cnt += 1
        return {'good': 'response'}

    res = asyncio.run(func())

    assert res == {'good': 'response'}


def test_retry_fails_on_bad_response():
    @retry
    async def func():
        raise ExceptionToFail

    with pytest.raises(RuntimeError, match='Request failed!'):
        asyncio.run(func())


def test_retry_all_500_errors(mock_nv_client):
    mock_nv_client.session.post.side_effect = [
        MockResponse('bad response', 500, HTTPServerError),
        MockResponse('bad response', 500, HTTPServerError),
        MockResponse('bad response', 500, HTTPServerError),
        MockResponse('bad response', 500, HTTPServerError),
        MockResponse('{"result": "good"}', 200),
    ]

    asyncio.run(mock_nv_client.request('method', {}))

    assert mock_nv_client.session.post.call_count == 5
