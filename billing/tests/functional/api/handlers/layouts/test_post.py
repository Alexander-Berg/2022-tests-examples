from uuid import UUID

import pytest
from aiohttp import MultipartWriter

from hamcrest import assert_that, equal_to, has_entries, has_properties

from billing.yandex_pay_admin.yandex_pay_admin.core.entities.enums import PayBackendType
from billing.yandex_pay_admin.yandex_pay_admin.file_storage.documents import YandexPayAdminDocsFileStorage

pytestmark = pytest.mark.usefixtures('mock_app_authentication', 'setup_interactions_tvm')


@pytest.mark.asyncio
async def test_returned(app, partner, origin, make_request):
    origin_id = origin['origin_id']

    r = await make_request()

    assert_that(r.status, equal_to(200))
    assert_that(
        await r.json(),
        has_entries(
            data=has_entries(
                origin_id=origin_id,
                document=has_entries(
                    name='file.png',
                ),
            ),
        ),
    )


@pytest.mark.asyncio
async def test_creates_layout(app, origin, make_request, storage):
    origin_id = origin['origin_id']

    r = await make_request()
    assert r.status < 300

    layout_data = (await r.json())['data']
    layout = await storage.layout.get(layout_data['layout_id'])
    assert_that(
        layout,
        has_properties(
            origin_id=UUID(origin_id),
        ),
    )


@pytest.mark.asyncio
async def test_creates_document(app, partner, make_request, storage):
    r = await make_request()
    assert r.status < 300

    layout = (await r.json())['data']
    layout_document_id = layout['document']['document_id']
    document = await storage.document.get(layout_document_id)
    assert_that(
        document,
        has_properties(
            partner_id=partner.partner_id,
            path=f'/origin-layouts/{layout_document_id}',
        ),
    )


@pytest.mark.asyncio
async def test_uploads_file(app, make_request, mock_file_storage):
    r = await make_request()
    assert r.status < 300

    layout = (await r.json())['data']
    layout_document_id = layout['document']['document_id']
    mock_file_storage.ctx_result.upload_stream.assert_called_once_with(f'/origin-layouts/{layout_document_id}')
    mock_file_storage.ctx_result.upload_stream.ctx_result.write.assert_awaited_once_with(b'filecontent')


@pytest.fixture
async def make_request(app, partner, origin, disable_tvm_checking):
    async def _make_request():
        partner_id = partner.partner_id
        origin_id = origin['origin_id']
        with MultipartWriter("form-data") as mpwriter:
            file_part = mpwriter.append('filecontent', {'content-type': 'image/png'})
            file_part.set_content_disposition('form-data', filename='file.png', name='file')

            type_part = mpwriter.append('checkout')
            type_part.set_content_disposition('form-data', name='type')
            return await app.post(f'/api/web/v1/partners/{partner_id}/origins/{origin_id}/layouts', data=mpwriter)

    return _make_request


@pytest.fixture
async def origin(app, rands, partner, role, merchant, disable_tvm_checking):
    r = await app.post(
        f'/api/web/v1/partners/{merchant.partner_id}/merchants/{merchant.merchant_id}/origins',
        json={"origin": rands()},
    )
    assert r.status < 300
    return (await r.json())['data']


@pytest.fixture(autouse=True)
def mock_file_storage(mocker, actx_mock):
    return mocker.patch.object(
        YandexPayAdminDocsFileStorage,
        'acquire',
        actx_mock(
            return_value=mocker.Mock(
                upload_stream=actx_mock(return_value=mocker.Mock(write=mocker.AsyncMock())),
            )
        ),
    )


@pytest.fixture(autouse=True)
def mock_pay_backends(mock_pay_backend_put_merchant, mock_pay_plus_backend_put_merchant):
    mock_pay_backend_put_merchant(PayBackendType.PRODUCTION)
    mock_pay_backend_put_merchant(PayBackendType.SANDBOX)
    mock_pay_plus_backend_put_merchant(PayBackendType.PRODUCTION)
    mock_pay_plus_backend_put_merchant(PayBackendType.SANDBOX)
