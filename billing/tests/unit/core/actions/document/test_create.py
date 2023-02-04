import pytest

from hamcrest import assert_that, equal_to, match_equality, matches_regexp

from billing.yandex_pay_admin.yandex_pay_admin.core.actions.document.create import CreateDocumentAction
from billing.yandex_pay_admin.yandex_pay_admin.file_storage.documents import YandexPayAdminDocsFileStorage
from billing.yandex_pay_admin.yandex_pay_admin.storage.entities.document import Document

PATH_PREFIX = '/prefix'
PATH_MATCHER = match_equality(matches_regexp(r'/prefix/[0-9a-f\-]+'))


@pytest.mark.asyncio
async def test_returned(partner, file_content_iter):
    document = await CreateDocumentAction(
        partner_id=partner.partner_id,
        path_prefix=PATH_PREFIX,
        original_name='file.png',
        content=file_content_iter(),
    ).run()

    assert_that(
        document,
        equal_to(
            Document(
                document_id=document.document_id,
                partner_id=partner.partner_id,
                path=PATH_MATCHER,
                digest='',
                name='file.png',
                created=document.created,
                updated=document.updated,
            )
        ),
    )


@pytest.mark.asyncio
async def test_calls_file_storage(mocker, partner, file_content_iter, mock_storage):
    await CreateDocumentAction(
        partner_id=partner.partner_id,
        path_prefix=PATH_PREFIX,
        original_name='file.png',
        content=file_content_iter(),
    ).run()

    mock_storage.assert_exited_once()
    mock_storage.ctx_result.upload_stream.assert_called_once_with(PATH_MATCHER)
    mock_storage.ctx_result.upload_stream.assert_exited_once()
    mock_storage.ctx_result.upload_stream.ctx_result.write.assert_has_awaits(
        [
            mocker.call(b'chunk1'),
            mocker.call(b'chunk2'),
        ]
    )


@pytest.fixture
def file_content_iter():
    async def _iter():
        yield b'chunk1'
        yield b'chunk2'

    return _iter


@pytest.fixture(autouse=True)
def mock_storage(mocker, actx_mock):
    return mocker.patch.object(
        YandexPayAdminDocsFileStorage,
        'acquire',
        actx_mock(
            return_value=mocker.Mock(upload_stream=actx_mock(return_value=mocker.Mock(write=mocker.AsyncMock())))
        ),
    )
