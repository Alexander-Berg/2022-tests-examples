import base64

import pytest

from billing.yandex_pay.yandex_pay.core.actions.visa_verify_request import VisaVerifyRequestAction
from billing.yandex_pay.yandex_pay.core.exceptions import CoreSecurityError, CoreVisaBadSignatureError
from billing.yandex_pay.yandex_pay.interactions import DuckGoClient
from billing.yandex_pay.yandex_pay.interactions.duckgo import DuckGoInteractionError


@pytest.fixture
def mock_visa_token_status_update_action(mocker):
    mock_run = mocker.AsyncMock()
    mock_action_cls = mocker.patch(
        'billing.yandex_pay.yandex_pay.api.handlers.events.'
        'visa.VisaUpdateTokenStatusAction'
    )
    mock_action_cls.return_value.run_async = mock_run
    return mock_action_cls


@pytest.fixture
def mock_duckgo_visa_verify_request(mocker):
    return mocker.patch.object(
        DuckGoClient,
        'visa_verify_request',
        return_value=None,
    )


@pytest.fixture
def mock_duckgo_visa_verify_request_error(mocker):
    return mocker.patch.object(
        DuckGoClient,
        'visa_verify_request',
        side_effect=DuckGoInteractionError(
            status_code=403,
            method='POST',
            service='DUCKGO',
        )
    )


@pytest.fixture
def call_args():
    return {
        'url': 'http://ya.ru?a=1',
        'body': b'hello world',
        'signature': 'this-is-signature'
    }


@pytest.mark.asyncio
async def test_should_call_duckgo_api(
    mock_duckgo_visa_verify_request,
    call_args,
):
    await VisaVerifyRequestAction(**call_args).run()

    mock_duckgo_visa_verify_request.assert_called_once_with(
        signature=call_args['signature'],
        url=call_args['url'],
        body_b64=base64.b64encode(call_args['body']).decode("utf-8")
    )


@pytest.mark.asyncio
async def test_should_raise_on_bad_duckgo_response(
    mock_duckgo_visa_verify_request_error,
    call_args
):
    with pytest.raises(CoreVisaBadSignatureError):
        await VisaVerifyRequestAction(**call_args).run()


def test_bad_signature_error_has_correct_parent():
    assert isinstance(
        CoreVisaBadSignatureError(description=''),
        CoreSecurityError
    )
