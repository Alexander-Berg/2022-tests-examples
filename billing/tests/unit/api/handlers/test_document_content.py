from uuid import uuid4

import pytest

from hamcrest import assert_that, equal_to, has_entries

from billing.yandex_pay_admin.yandex_pay_admin.core.actions.document.content import GetDocumentContentAction

PARTNER_ID = uuid4()
DOCUMENT_ID = uuid4()
FILE_CONTENT = b'1' * 1000


@pytest.mark.asyncio
async def test_success(
    app,
    disable_tvm_checking,
):
    response = await app.get(f'/api/web/v1/partners/{PARTNER_ID}/documents/{DOCUMENT_ID}/content')
    content = await response.read()

    assert_that(response.status, equal_to(200))
    assert_that(
        response.headers,
        has_entries(
            {
                'content-type': 'application/octet-stream',
                'content-disposition': 'attachment; filename="file.png"',
            }
        ),
    )
    assert_that(content, equal_to(FILE_CONTENT))


@pytest.mark.asyncio
async def test_calls_get_document_content(
    app,
    disable_tvm_checking,
    mock_get_document_content,
):
    await app.get(f'/api/web/v1/partners/{PARTNER_ID}/documents/{DOCUMENT_ID}/content')

    mock_get_document_content.assert_run_once_with(
        partner_id=PARTNER_ID,
        document_id=DOCUMENT_ID,
    )


@pytest.fixture(autouse=True)
def mock_get_document_content(mock_action):
    async def content():
        yield FILE_CONTENT

    return mock_action(
        GetDocumentContentAction,
        {
            'filename': 'file.png',
            'content_type': 'application/octet-stream',
            'content': content(),
        },
    )
