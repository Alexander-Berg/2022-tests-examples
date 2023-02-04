import re
import uuid

import pytest
import yarl

from hamcrest import assert_that, contains_string, equal_to, has_entry, has_property

from billing.yandex_pay_admin.yandex_pay_admin.file_storage.documents import YandexPayAdminDocsFileStorage
from billing.yandex_pay_admin.yandex_pay_admin.file_storage.payments_documents import PaymentsDocsFileStorage
from billing.yandex_pay_admin.yandex_pay_admin.storage.entities.enums import (
    ContractDocumentType,
    PartnerType,
    PaymentGatewayType,
)

pytest.skip(allow_module_level=True)  # TODO: remove when refactoring is complete


@pytest.fixture(autouse=True)
def mock_yandex_pay_sandbox_response(aioresponses_mocker, yandex_pay_admin_settings, setup_interactions_tvm):
    return aioresponses_mocker.put(
        re.compile(f'^{yandex_pay_admin_settings.YANDEX_PAY_BACKEND_SANDBOX_URL}.*'),
        status=200,
        payload={'status': 'success'},
    )


@pytest.mark.asyncio
async def test_post(
    app,
    partner_id,
    partner_moderation_json,
    expected_good_json_response,
    disable_tvm_checking,
):
    r = await app.post(f'api/v1/partner/{partner_id}/moderation', json=partner_moderation_json)
    json_response = await r.json()

    assert_that(r.status, equal_to(200))
    assert_that(json_response, equal_to(expected_good_json_response))


@pytest.mark.asyncio
async def test_post_calls_startrek(
    app,
    partner_id,
    partner_moderation_json,
    aioresponses_mocker,
    mock_startrek_update,
    disable_tvm_checking,
):
    r = await app.post(f'api/v1/partner/{partner_id}/moderation', json=partner_moderation_json)
    await r.json()

    assert_that(
        aioresponses_mocker.requests[mock_startrek_update][0],
        has_property('kwargs', has_entry('json', has_entry('description', contains_string('договор')))),
    )


@pytest.fixture(autouse=True)
def documents_storage_mock(mocker, actx_mock):
    return mocker.patch.object(
        YandexPayAdminDocsFileStorage,
        'acquire',
        actx_mock(
            return_value=mocker.Mock(
                upload_stream=actx_mock(
                    return_value=mocker.Mock(
                        write=mocker.AsyncMock(),
                    )
                ),
                drop_file=mocker.AsyncMock(),
            )
        ),
    )


@pytest.fixture(autouse=True)
def payments_documents_storage_mock(mocker, actx_mock):
    async def file_content_iter():
        yield 'content!'

    return mocker.patch.object(
        PaymentsDocsFileStorage,
        'acquire',
        actx_mock(
            return_value=mocker.Mock(
                download=mocker.AsyncMock(
                    return_value=mocker.Mock(
                        content=file_content_iter(),
                    )
                )
            )
        ),
    )


@pytest.fixture(autouse=True)
async def partner_id(app, disable_tvm_checking, mock_startrek_create):
    partner_id = uuid.uuid4()
    r = await app.put(
        '/api/v1/partner',
        json={
            'partner_id': str(partner_id),
            'name': 'partner_name',
            'psp_external_id': 'external_str_id',
            'type': PartnerType.PAYMENT_GATEWAY.value,
            'payment_gateway_type': PaymentGatewayType.DIRECT_MERCHANT.value,
            'uid': 228,
            'contact': {
                'email': 'email@test',
                'phone': '+1(000)555-0100',
                'name': 'John',
                'surname': 'Doe',
                'patronymic': 'Татьянович',
            },
        },
    )
    assert 200 <= r.status < 300
    return partner_id


@pytest.fixture(autouse=True)
async def mock_startrek_create(aioresponses_mocker, yandex_pay_admin_settings, storage):
    create_url = yarl.URL(yandex_pay_admin_settings.STARTREK_API_URL) / 'v2/issues'
    payload = {
        'id': '60d501d28c3625165e149687',
        'key': 'YANDEXPAYTEST-777',
        'status': {
            'id': '1',
            'key': 'open',
        },
    }
    aioresponses_mocker.post(create_url, payload=payload)
    return ('POST', create_url)


@pytest.fixture(autouse=True)
async def mock_startrek_update(aioresponses_mocker, yandex_pay_admin_settings, storage):
    update_url = yarl.URL(yandex_pay_admin_settings.STARTREK_API_URL) / 'v2/issues/YANDEXPAYTEST-777'
    payload = {
        'id': '60d501d28c3625165e149687',
        'key': 'YANDEXPAYTEST-777',
        'status': {
            'id': '1',
            'key': 'open',
        },
    }
    aioresponses_mocker.patch(update_url, payload=payload)
    return ('PATCH', update_url)


@pytest.fixture
def partner_moderation_json() -> dict:
    return {'verified': True, 'documents': [{'path': 'foobath', 'type': ContractDocumentType.CONTRACT.value}]}


@pytest.fixture
def expected_good_json_response():
    return {
        'code': 200,
        'status': 'success',
        'data': None,
    }
