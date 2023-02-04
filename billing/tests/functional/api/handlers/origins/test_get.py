import re

import pytest
from aiohttp import MultipartWriter

from hamcrest import assert_that, equal_to, has_entries, match_equality

from billing.yandex_pay_admin.yandex_pay_admin.file_storage.documents import YandexPayAdminDocsFileStorage

pytestmark = pytest.mark.usefixtures(
    'mock_app_authentication',
    'setup_interactions_tvm',
    'mock_pay_backend_put_merchant_both_environments',
    'mock_pay_plus_backend_put_merchant_both_environments',
)


@pytest.mark.asyncio
async def test_get_origins(app, partner, merchant, origin, layout, moderation, disable_tvm_checking):
    partner_id = partner.partner_id
    merchant_id = merchant.merchant_id
    origin_id = origin['origin_id']

    r = await app.get(f'/api/web/v1/partners/{partner_id}/merchants/{merchant_id}/origins')

    assert_that(r.status, equal_to(200))
    assert_that(
        await r.json(),
        equal_to(
            {
                'status': 'success',
                'code': 200,
                'data': {
                    'origins': [
                        match_equality(
                            has_entries(
                                merchant_id=str(merchant_id),
                                origin_id=origin_id,
                                moderation=has_entries(origin_moderation_id=moderation['origin_moderation_id']),
                                layouts=[
                                    match_equality(
                                        has_entries(
                                            layout_id=layout['layout_id'],
                                            document=has_entries(document_id=layout['document']['document_id']),
                                        )
                                    )
                                ],
                            )
                        ),
                    ],
                },
            }
        ),
    )


@pytest.fixture
async def origin(app, rands, partner, role, merchant, disable_tvm_checking):
    partner_id = partner.partner_id
    merchant_id = merchant.merchant_id
    r = await app.post(f'/api/web/v1/partners/{partner_id}/merchants/{merchant_id}/origins', json={"origin": rands()})
    assert r.status < 300
    return (await r.json())['data']


@pytest.fixture
async def layout(app, partner, origin, disable_tvm_checking):
    partner_id = partner.partner_id
    origin_id = origin['origin_id']
    with MultipartWriter("form-data") as mpwriter:
        file_part = mpwriter.append('filecontent', {'content-type': 'image/png'})
        file_part.set_content_disposition('form-data', filename='file.png', name='file')

        type_part = mpwriter.append('checkout')
        type_part.set_content_disposition('form-data', name='type')
        r = await app.post(f'/api/web/v1/partners/{partner_id}/origins/{origin_id}/layouts', data=mpwriter)
        assert r.status < 300
        return (await r.json())['data']


@pytest.fixture
async def moderation(app, partner, origin, layout, disable_tvm_checking):
    partner_id = partner.partner_id
    origin_id = origin['origin_id']
    r = await app.post(f'/api/web/v1/partners/{partner_id}/origins/{origin_id}/moderations')
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
                drop_file=mocker.AsyncMock(),
            )
        ),
    )


@pytest.fixture(autouse=True)
def mock_startrek_create(aioresponses_mocker):
    payload = {
        'id': 'xxxyyy',
        'key': 'TICKET-1',
        'status': {
            'id': '1',
            'key': 'open',
        },
    }
    return aioresponses_mocker.post(re.compile('.*/v2/issues'), payload=payload)
