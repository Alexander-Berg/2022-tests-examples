from uuid import uuid4

import pytest

from sendr_utils import utcnow

from hamcrest import assert_that, equal_to, has_entries

from billing.yandex_pay.yandex_pay.core.actions.merchant.create_or_update import CreateOrUpdateMerchantAction
from billing.yandex_pay.yandex_pay.core.actions.merchant.get import GetMerchantAction
from billing.yandex_pay.yandex_pay.core.entities.merchant import MerchantWithRelated
from billing.yandex_pay.yandex_pay.core.entities.merchant_origin import MerchantOrigin


@pytest.mark.asyncio
async def test_get(
    internal_app,
    action_result,
    mock_action,
    mock_internal_tvm,
    expected_handler_result,
):
    mock_action(GetMerchantAction, action_result)

    r = await internal_app.get(f'api/internal/v1/merchants/{uuid4()}')
    json_body = await r.json()

    assert_that(r.status, equal_to(200))
    assert_that(json_body, equal_to(expected_handler_result))


@pytest.mark.asyncio
async def test_put(
    internal_app,
    mock_action,
    mock_internal_tvm,
    merchant_put_data,
    action_result,
    expected_handler_result,
):
    mock_action(CreateOrUpdateMerchantAction, action_result)

    r = await internal_app.put(
        f'api/internal/v1/merchants/{uuid4()}',
        json=merchant_put_data,
    )
    json_body = await r.json()

    assert_that(r.status, equal_to(200))
    assert_that(json_body, equal_to(expected_handler_result))


@pytest.mark.asyncio
async def test_put_no_name(internal_app, mock_action, mock_internal_tvm, merchant_put_data):
    mock_action(CreateOrUpdateMerchantAction, action_result)
    del merchant_put_data['name']

    r = await internal_app.put(
        f'api/internal/v1/merchants/{uuid4()}',
        json=merchant_put_data,
    )
    json = await r.json()

    assert_that(r.status, equal_to(400))
    assert_that(
        json['data']['params'],
        has_entries({
            'name': ['Missing data for required field.'],
        })
    )


@pytest.mark.asyncio
async def test_put_no_origins(internal_app, mock_action, mock_internal_tvm, merchant_put_data):
    mock_action(CreateOrUpdateMerchantAction, action_result)
    del merchant_put_data['origins']

    r = await internal_app.put(
        f'api/internal/v1/merchants/{uuid4()}',
        json=merchant_put_data,
    )
    json = await r.json()

    assert_that(r.status, equal_to(400))
    assert_that(
        json['data']['params'],
        has_entries({
            'origins': has_entries({
                '0': has_entries({
                    'origin': ['Missing data for required field.']
                })
            })
        })
    )


@pytest.mark.asyncio
async def test_put_empty_origin(internal_app, mock_action, mock_internal_tvm, merchant_put_data):
    mock_action(CreateOrUpdateMerchantAction, action_result)
    merchant_put_data['origins'] = [{}]

    r = await internal_app.put(
        f'api/internal/v1/merchants/{uuid4()}',
        json=merchant_put_data,
    )
    json = await r.json()

    assert_that(r.status, equal_to(400))
    assert_that(
        json['data']['params'],
        has_entries({
            'origins': has_entries({
                '0': has_entries({
                    'origin': ['Missing data for required field.']
                })
            })
        })
    )


@pytest.mark.asyncio
async def test_put_no_callback_url(internal_app, mock_action, mock_internal_tvm, merchant_put_data):
    mock_action(CreateOrUpdateMerchantAction, action_result)
    del merchant_put_data['callback_url']

    r = await internal_app.put(
        f'api/internal/v1/merchants/{uuid4()}',
        json=merchant_put_data,
    )

    assert_that(r.status, equal_to(200))


@pytest.mark.asyncio
async def test_should_call_action_with_expected_args(internal_app, mock_action, mock_internal_tvm, merchant_put_data):
    mocked_action = mock_action(CreateOrUpdateMerchantAction, action_result)
    merchant_id = uuid4()

    await internal_app.put(
        f'api/internal/v1/merchants/{merchant_id}',
        json=merchant_put_data,
    )

    mocked_action.assert_called_once_with(
        origins=[{'origin': 'https://a.test'}],
        name='name',
        callback_url='https://test.back',
        merchant_id=merchant_id,
    )


@pytest.fixture
def action_result(rands):
    return MerchantWithRelated(
        merchant_id=uuid4(),
        name=rands(),
        created=utcnow(),
        updated=utcnow(),
        origins=[
            MerchantOrigin(
                merchant_id=uuid4(),
                origin='https://origin.test:443',
                created=utcnow(),
            )
        ],
        callback_url='https://callack.test',
    )


@pytest.fixture
def expected_handler_result(action_result):
    return {
        'status': 'success',
        'code': 200,
        'data': {
            'merchant': {
                'merchant_id': str(action_result.merchant_id),
                'name': action_result.name,
                'created': action_result.created.isoformat(),
                'origins': [
                    {'origin': origin.origin, 'created': origin.created.isoformat(), 'is_blocked': False}
                    for origin in action_result.origins
                ],
                'is_blocked': False,
                'callback_url': action_result.callback_url,
            }
        }
    }


@pytest.fixture
def merchant_put_data():
    return {
        'name': 'name',
        'origins': [{'origin': 'https://a.test'}],
        'callback_url': 'https://test.back',
    }
