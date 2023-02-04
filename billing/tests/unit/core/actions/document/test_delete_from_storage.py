import pytest

from sendr_filestore.s3.exceptions import FileNotFound

from billing.yandex_pay_admin.yandex_pay_admin.core.actions.document.delete_from_storage import (
    DeleteDocumentFromFileStorageAction,
)
from billing.yandex_pay_admin.yandex_pay_admin.file_storage.documents import YandexPayAdminDocsFileStorage


@pytest.mark.asyncio
async def test_calls_file_storage(mocker, actx_mock):
    mock_storage = mocker.patch.object(
        YandexPayAdminDocsFileStorage, 'acquire', actx_mock(return_value=mocker.Mock(drop_file=mocker.AsyncMock()))
    )

    await DeleteDocumentFromFileStorageAction(path='pa/th').run()

    mock_storage.assert_exited_once()
    mock_storage.ctx_result.drop_file.assert_awaited_once_with('pa/th')


@pytest.mark.asyncio
async def test_not_raises_when_file_already_deleted(mocker, actx_mock):
    mock_storage = mocker.patch.object(
        YandexPayAdminDocsFileStorage,
        'acquire',
        actx_mock(return_value=mocker.Mock(drop_file=mocker.AsyncMock(side_effect=FileNotFound))),
    )

    await DeleteDocumentFromFileStorageAction(path='pa/th').run()

    mock_storage.assert_exited_once()
    mock_storage.ctx_result.drop_file.assert_awaited_once_with('pa/th')
