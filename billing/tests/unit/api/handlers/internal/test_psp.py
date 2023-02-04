from uuid import uuid4

import pytest

from sendr_utils import utcnow

from hamcrest import assert_that, equal_to, has_entries

from billing.yandex_pay.yandex_pay.core.actions.psp.create_or_update import CreateOrUpdatePSPAction
from billing.yandex_pay.yandex_pay.core.actions.psp.get import GetPSPAction
from billing.yandex_pay.yandex_pay.core.entities.psp import PSPWithRelated
from billing.yandex_pay.yandex_pay.core.entities.psp_key import PSPKeyWithKid


@pytest.mark.asyncio
async def test_get(
    internal_app,
    action_result,
    mock_action,
    mock_internal_tvm,
    expected_handler_result,
):
    mock_action(GetPSPAction, action_result)

    r = await internal_app.get(f'api/internal/v1/psp/{uuid4()}')
    json_body = await r.json()

    assert_that(r.status, equal_to(200))
    assert_that(json_body, equal_to(expected_handler_result))


@pytest.mark.asyncio
async def test_put(
    internal_app,
    mock_action,
    mock_internal_tvm,
    psp_put_data,
    action_result,
    expected_handler_result,
):
    mock_action(CreateOrUpdatePSPAction, action_result)

    r = await internal_app.put(
        f'api/internal/v1/psp/{uuid4()}',
        json=psp_put_data,
    )
    json_body = await r.json()

    assert_that(r.status, equal_to(200))
    assert_that(json_body, equal_to(expected_handler_result))


@pytest.mark.asyncio
async def test_put_calls_action(
    internal_app,
    mock_action,
    mock_internal_tvm,
    psp_put_data,
    action_result,
    expected_handler_result,
):
    mock = mock_action(CreateOrUpdatePSPAction, action_result)
    psp_id = uuid4()

    await internal_app.put(
        f'api/internal/v1/psp/{psp_id}',
        json=psp_put_data,
    )

    mock.assert_called_once_with(
        psp_id=psp_id,
        psp_external_id=psp_put_data['psp_external_id'],
        public_key=psp_put_data['public_key'],
        public_key_signature=psp_put_data['public_key_signature'],
        psp_auth_keys=[{'key': auth_key['key'], 'alg': 'ES256'} for auth_key in psp_put_data['psp_auth_keys']],
    )


@pytest.mark.parametrize('field', (
    'psp_external_id', 'public_key_signature', 'public_key',
))
@pytest.mark.asyncio
async def test_put_omitted_required_field(internal_app, mock_action, mock_internal_tvm, psp_put_data, field):
    mock_action(CreateOrUpdatePSPAction, action_result)
    del psp_put_data[field]

    r = await internal_app.put(
        f'api/internal/v1/psp/{uuid4()}',
        json=psp_put_data,
    )
    json = await r.json()

    assert_that(r.status, equal_to(400))
    assert_that(
        json['data']['params'],
        has_entries({
            field: ['Missing data for required field.'],
        })
    )


@pytest.mark.asyncio
async def test_put_omitted_auth_keys(internal_app, mock_action, mock_internal_tvm, psp_put_data):
    mock_action(CreateOrUpdatePSPAction, action_result)
    del psp_put_data['psp_auth_keys']

    r = await internal_app.put(
        f'api/internal/v1/psp/{uuid4()}',
        json=psp_put_data,
    )
    json = await r.json()

    assert_that(r.status, equal_to(400))
    assert_that(
        json['data']['params'],
        has_entries({
            'psp_auth_keys': {
                '0': {
                    'key': ['Missing data for required field.'],
                    'alg': ['Missing data for required field.'],
                }
            }
        })
    )


@pytest.mark.parametrize('field', (
    'key', 'alg',
))
@pytest.mark.asyncio
async def test_put_unknown_auth_key_field_omited(internal_app, mock_action, mock_internal_tvm, psp_put_data, field):
    mock_action(CreateOrUpdatePSPAction, action_result)
    del psp_put_data['psp_auth_keys'][0][field]

    r = await internal_app.put(
        f'api/internal/v1/psp/{uuid4()}',
        json=psp_put_data,
    )
    json = await r.json()

    assert_that(r.status, equal_to(400))
    assert_that(
        json['data']['params'],
        has_entries({
            'psp_auth_keys': {
                '0': {
                    field: ['Missing data for required field.'],
                }
            }
        })
    )


@pytest.fixture
def action_result(rands):
    return PSPWithRelated(
        psp_id=uuid4(),
        psp_external_id='external-id',
        public_key='public-key',
        public_key_signature='public-key-signature',
        public_key_updated=utcnow(),
        created=utcnow(),
        psp_auth_keys=[
            PSPKeyWithKid(
                psp_id=uuid4(),
                psp_key_id=1,
                key='key',
                alg='ES256',
                psp_external_id='gwid',
                created=utcnow(),
            )
        ],
    )


@pytest.fixture
def expected_handler_result(action_result):
    return {
        'status': 'success',
        'code': 200,
        'data': {
            'psp': {
                'psp_id': str(action_result.psp_id),
                'psp_external_id': action_result.psp_external_id,
                'public_key': action_result.public_key,
                'public_key_signature': action_result.public_key_signature,
                'created': action_result.created.isoformat(),
                'public_key_updated': action_result.public_key_updated.isoformat(),
                'psp_auth_keys': [
                    {
                        'key': auth_key.key,
                        'alg': auth_key.alg,
                        'created': auth_key.created.isoformat(),
                        'jws_kid': auth_key.jws_kid,
                    }
                    for auth_key in action_result.psp_auth_keys
                ],
                'is_blocked': False,
            }
        }
    }


@pytest.fixture
def psp_put_data():
    return {
        'psp_external_id': 'external-id',
        'public_key': 'public-key',
        'public_key_signature': 'public-key-signature',
        'public_key_updated': utcnow().isoformat(),
        'created': utcnow().isoformat(),
        'psp_auth_keys': [
            {
                'key': 'key',
                'alg': 'ES256',
            }
        ],
    }
