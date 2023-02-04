from uuid import uuid4

import pytest

from sendr_utils import alist

from hamcrest import assert_that, equal_to, match_equality, matches_regexp

from billing.yandex_pay_admin.yandex_pay_admin.core.actions.document.content import GetDocumentContentAction
from billing.yandex_pay_admin.yandex_pay_admin.core.actions.document.get import GetDocumentAction
from billing.yandex_pay_admin.yandex_pay_admin.file_storage.documents import YandexPayAdminDocsFileStorage
from billing.yandex_pay_admin.yandex_pay_admin.storage.entities.document import Document

PATH_PREFIX = '/prefix'
PATH_MATCHER = match_equality(matches_regexp(r'/prefix/[0-9a-f\-]+'))


@pytest.mark.asyncio
async def test_returned(partner, file_content_iter, document):
    returned = await GetDocumentContentAction(
        partner_id=partner.partner_id,
        document_id=document.document_id,
    ).run()

    content = returned['content']
    del returned['content']
    assert_that(
        returned,
        equal_to(
            {
                'filename': 'file.png',
                'content_type': 'application/octet-stream',
            }
        ),
    )
    assert_that(b''.join(await alist(content)), equal_to(b'chunk1chunk2'))


@pytest.fixture
async def document(storage, partner):
    return await storage.document.create(
        Document(
            document_id=uuid4(),
            partner_id=partner.partner_id,
            path='/file-s3-path',
            digest='',
            name='file.png',
        )
    )


@pytest.fixture(autouse=True)
async def mock_get_document(mock_action, document):
    return mock_action(GetDocumentAction, document)


@pytest.fixture
def file_content_iter():
    async def _iter():
        yield b'chunk1'
        yield b'chunk2'

    return _iter


@pytest.fixture(autouse=True)
def mock_storage(mocker, actx_mock, file_content_iter):
    return mocker.patch.object(
        YandexPayAdminDocsFileStorage,
        'acquire',
        actx_mock(
            return_value=mocker.Mock(download=mocker.AsyncMock(return_value=mocker.Mock(content=file_content_iter())))
        ),
    )
