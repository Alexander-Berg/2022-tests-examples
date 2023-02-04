import uuid

import pytest

from hamcrest import assert_that, equal_to

from billing.yandex_pay_admin.yandex_pay_admin.core.actions.partner_moderation import UpdatePartnerModerationAction
from billing.yandex_pay_admin.yandex_pay_admin.core.exceptions import PartnerNotFoundError
from billing.yandex_pay_admin.yandex_pay_admin.storage.entities.enums import ContractDocumentType

pytest.skip('Partner moderation is under reconstruction', allow_module_level=True)


@pytest.fixture
def partner_id():
    return uuid.uuid4()


@pytest.fixture
def partner_moderation_json() -> dict:
    return {
        'verified': True,
        'documents': [
            {'path': 'patz', 'name': 'teh-name', 'type': ContractDocumentType.PCI_DSS_CERT.value},
            {'path': 'patz2', 'type': ContractDocumentType.PCI_DSS_CERT.value},
        ],
    }


@pytest.fixture
def expected_bad_json_response():
    return {'code': 404, 'status': 'fail', 'data': {'message': 'PARTNER_NOT_FOUND'}}


@pytest.mark.asyncio
async def test_should_call_action_with_passed_body(
    app,
    partner_id,
    mocker,
    mock_action,
    partner_moderation_json,
    disable_tvm_checking,
):
    mock = mock_action(UpdatePartnerModerationAction)

    await app.post(f'api/v1/partner/{partner_id}/moderation', json=partner_moderation_json)

    mock.assert_run_once_with(
        partner_id=partner_id,
        verified=partner_moderation_json['verified'],
        documents=[
            {'path': 'patz', 'name': 'teh-name', 'type': ContractDocumentType.PCI_DSS_CERT},
            {'path': 'patz2', 'name': None, 'type': ContractDocumentType.PCI_DSS_CERT},
        ],
    )


@pytest.mark.asyncio
async def test_should_respond_bad_request_code_on_invalid_update(
    app,
    mock_action,
    partner_id,
    partner_moderation_json,
    expected_bad_json_response,
    disable_tvm_checking,
):
    mock_action(UpdatePartnerModerationAction, side_effect=PartnerNotFoundError)

    r = await app.post(f'api/v1/partner/{partner_id}/moderation', json=partner_moderation_json)
    json_response = await r.json()

    assert_that(r.status, equal_to(404))
    assert_that(json_response, equal_to(expected_bad_json_response))


@pytest.mark.asyncio
async def test_no_verified(
    app,
    partner_id,
    partner_moderation_json,
    disable_tvm_checking,
):
    partner_moderation_json['verified'] = None
    r = await app.post(f'api/v1/partner/{partner_id}/moderation', json=partner_moderation_json)
    json_response = await r.json()

    assert_that(r.status, equal_to(400))
    assert_that(
        json_response,
        equal_to(
            {
                'code': 400,
                'status': 'fail',
                'data': {
                    'message': 'SCHEMA_VALIDATION_ERROR',
                    'params': {'verified': ['Field may not be null.']},
                },
            }
        ),
    )


@pytest.mark.asyncio
async def test_bad_document(
    app,
    partner_id,
    partner_moderation_json,
    disable_tvm_checking,
):
    partner_moderation_json['documents'] = [{'name': '123'}]
    r = await app.post(f'api/v1/partner/{partner_id}/moderation', json=partner_moderation_json)
    json_response = await r.json()

    assert_that(r.status, equal_to(400))
    assert_that(
        json_response,
        equal_to(
            {
                'code': 400,
                'status': 'fail',
                'data': {
                    'message': 'SCHEMA_VALIDATION_ERROR',
                    'params': {
                        'documents': {
                            '0': {
                                'type': ['Missing data for required field.'],
                                'path': ['Missing data for required field.'],
                            }
                        }
                    },
                },
            }
        ),
    )
