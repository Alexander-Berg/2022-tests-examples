import pytest
from aiohttp import web

from sendr_aiohttp import Url

from hamcrest import assert_that, equal_to, has_entries

from billing.yandex_pay_admin.yandex_pay_admin.api.app import YandexPayAdminApplication
from billing.yandex_pay_admin.yandex_pay_admin.api.handlers.base import BaseHandler
from billing.yandex_pay_admin.yandex_pay_admin.core.actions.base import BaseAction
from billing.yandex_pay_admin.yandex_pay_admin.core.exceptions import (
    CoreAlreadyExistsError,
    CoreDataError,
    CoreNotFoundError,
    CoreSecurityError,
    DocumentNotFoundError,
    InvalidIntegrationCredentialValueError,
    InvalidIntegrationStatusError,
    LayoutNotFoundError,
    OriginAlreadyExistsError,
    OriginHasApprovedModerationError,
    OriginModerationNotFoundError,
    OriginNotFoundError,
    OwnerUIDChangedError,
    PartnerNotFoundError,
    PartnerTypeChangedError,
    PartnerUpdateError,
    PSPExternalIDChangedError,
    PSPExternalIDEmptyError,
    UnsupportedPSPForIntegrationError,
)


class ExampleAction(BaseAction):
    pass


class ExampleHandler(BaseHandler):
    async def get(self):
        await self.run_action(ExampleAction)
        return web.Response()


@pytest.fixture
async def app(aiohttp_client, mocker, db_engine, yandex_pay_admin_settings):
    mocker.patch.object(
        YandexPayAdminApplication,
        '_urls',
        YandexPayAdminApplication._urls + ([Url('/path', ExampleHandler, 'v_test')],),
    )
    app = YandexPayAdminApplication(db_engine=db_engine)
    app.file_storage = mocker.Mock()
    app.pushers = mocker.Mock()
    return await aiohttp_client(app)


@pytest.mark.parametrize(
    'exc, expected_code, expected_exc_params',
    (
        pytest.param(CoreNotFoundError, 404, None, id='CoreNotFoundError'),
        pytest.param(CoreSecurityError, 403, None, id='CoreSecurityError'),
        pytest.param(CoreDataError, 400, None, id='CoreDataError'),
        pytest.param(CoreAlreadyExistsError, 409, None, id='CoreAlreadyExistsError'),
        pytest.param(PartnerUpdateError, 400, None, id='PartnerUpdateError'),
        pytest.param(PartnerNotFoundError, 404, None, id='PartnerNotFoundError'),
        pytest.param(LayoutNotFoundError, 404, None, id='LayoutNotFoundError'),
        pytest.param(OriginNotFoundError, 404, None, id='OriginNotFoundError'),
        pytest.param(OriginHasApprovedModerationError, 400, None, id='OriginHasApprovedModerationError'),
        pytest.param(OriginAlreadyExistsError, 409, None, id='OriginAlreadyExistsError'),
        pytest.param(DocumentNotFoundError, 404, None, id='DocumentNotFoundError'),
        pytest.param(OriginModerationNotFoundError, 404, None, id='OriginModerationNotFoundError'),
        pytest.param(OwnerUIDChangedError, 400, None, id='OwnerUIDChangedError'),
        pytest.param(PartnerTypeChangedError, 400, None, id='PartnerTypeChangedError'),
        pytest.param(PSPExternalIDChangedError, 400, None, id='PSPExternalIDChangedError'),
        pytest.param(PSPExternalIDEmptyError, 400, None, id='PSPExternalIDEmptyError'),
        pytest.param(UnsupportedPSPForIntegrationError, 400, None, id='UnsupportedPSPForIntegrationError'),
        pytest.param(InvalidIntegrationStatusError, 400, None, id='InvalidIntegrationStatusError'),
        pytest.param(
            InvalidIntegrationCredentialValueError(
                params={'validation_errors': {'password': ['Missing data for required field.']}}
            ),
            400,
            {'validation_errors': {'password': ['Missing data for required field.']}},
            id='InvalidIntegrationCredentialValueError',
        ),
    ),
)
@pytest.mark.asyncio
async def test_action_raised_exception(app, mock_action, exc, expected_code, expected_exc_params, disable_tvm_checking):
    mock_action(ExampleAction, side_effect=exc)
    params = {'params': expected_exc_params} if expected_exc_params is not None else {}

    response = await app.get('/path')

    assert_that(response.status, equal_to(expected_code))
    data = await response.json()
    expected_response = {
        'code': expected_code,
        'status': 'fail',
        'data': {
            'message': exc.message,
            **params,
        },
    }
    assert_that(data, has_entries(expected_response))
