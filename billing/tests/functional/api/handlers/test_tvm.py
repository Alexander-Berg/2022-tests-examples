import re

import pytest
from sendr_auth import skip_authentication

from hamcrest import assert_that, equal_to

from billing.yandex_pay_admin.yandex_pay_admin.api.handlers.internal.base import BaseInternalHandler


@skip_authentication
class EnabledTvmHandler(BaseInternalHandler):
    async def get(self):
        return self.make_response({})

    async def options(self):
        return self.make_response({})


@skip_authentication
class DisabledTvmHandler(BaseInternalHandler):
    CHECK_TVM = False

    async def get(self):
        return self.make_response({})


@pytest.fixture
async def patched_app(aiohttp_client, application):
    application.router.add_view('/enabled_tvm', EnabledTvmHandler)
    application.router.add_view('/disabled_tvm', DisabledTvmHandler)
    return await aiohttp_client(application)


@pytest.fixture
def expected_no_ticket_json() -> dict:
    return {'code': 403, 'status': 'fail', 'data': {'message': 'SERVICE_TICKET_NOT_PASSED'}}


@pytest.fixture
def expected_not_allowed_src_json() -> dict:
    return {'code': 403, 'status': 'fail', 'data': {'message': 'SERVICE_NOT_ALLOWED'}}


@pytest.fixture
def allowed_src() -> int:
    return 111


@pytest.fixture(autouse=True)
def overwrite_settings(yandex_pay_admin_settings, allowed_src):
    yandex_pay_admin_settings.TVM_ALLOWED_SRC = {allowed_src}


@pytest.mark.asyncio
async def test_should_respond_forbidden_if_header_not_passed(patched_app, expected_no_ticket_json):
    r = await patched_app.get('/enabled_tvm')
    json = await r.json()

    assert_that(r.status, equal_to(403))
    assert_that(json, equal_to(expected_no_ticket_json))


@pytest.mark.asyncio
async def test_tvm_checking_should_be_disabled_for_options_method(patched_app):
    r = await patched_app.options('/enabled_tvm')

    assert_that(r.status, equal_to(200))


@pytest.mark.asyncio
async def test_should_respond_forbidden_if_ticket_belongs_to_not_allowed_src(
    patched_app,
    expected_not_allowed_src_json,
    aioresponses_mocker,
    yandex_pay_admin_settings,
):
    aioresponses_mocker.get(
        re.compile(f'^{yandex_pay_admin_settings.TVM_URL}/tvm/checksrv'), status=200, payload={'src': 5345245}
    )
    r = await patched_app.get('/enabled_tvm', headers={'x-Ya-service-Ticket': 'ticket'})
    json = await r.json()

    assert_that(r.status, equal_to(403))
    assert_that(json, equal_to(expected_not_allowed_src_json))


@pytest.mark.asyncio
async def test_should_pass_if_ticket_belongs_to_allowed_src(
    patched_app,
    aioresponses_mocker,
    yandex_pay_admin_settings,
    allowed_src,
):
    aioresponses_mocker.get(
        re.compile(f'^{yandex_pay_admin_settings.TVM_URL}/tvm/checksrv'), status=200, payload={'src': allowed_src}
    )
    r = await patched_app.get('/enabled_tvm', headers={'x-Ya-service-Ticket': 'ticket'})

    assert_that(r.status, equal_to(200))


@pytest.mark.asyncio
async def test_can_disable_tvm_checking(
    patched_app,
):
    r = await patched_app.get('/disabled_tvm')

    assert_that(r.status, equal_to(200))


@pytest.mark.asyncio
async def test_check_request_to_tvm_is_correct(
    patched_app,
    aioresponses_mocker,
    yandex_pay_admin_settings,
    allowed_src,
):
    ticket = 'some_ticket'

    def assert_tvm_request(url, **kwargs):
        assert_that(
            kwargs,
            equal_to(
                {
                    'allow_redirects': True,
                    'params': {'dst': yandex_pay_admin_settings.TVM_ID},
                    'headers': {'Authorization': yandex_pay_admin_settings.TVM_TOKEN, 'X-Ya-Service-Ticket': ticket},
                }
            ),
        )

    aioresponses_mocker.get(
        re.compile(f'^{yandex_pay_admin_settings.TVM_URL}/tvm/checksrv'),
        status=200,
        payload={'src': allowed_src},
        callback=assert_tvm_request,
    )
    await patched_app.get('/enabled_tvm', headers={'x-Ya-service-Ticket': ticket})
