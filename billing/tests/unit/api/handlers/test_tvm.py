import pytest

from hamcrest import assert_that, equal_to

from billing.yandex_pay_plus.yandex_pay_plus.api.handlers.internal.base import BaseInternalHandler


class ExampleTvmHandler(BaseInternalHandler):
    async def get(self):
        return self.make_response({'uid': self.get_valid_uid(), 'src': self.get_tvm_src()})


@pytest.mark.asyncio
async def test_success(app, tvm_service_id, tvm_user_id):
    r = await app.get('/tvm')

    assert_that(
        await r.json(),
        equal_to({
            'uid': tvm_user_id,
            'src': tvm_service_id,
        })
    )


class TestInvalidServiceTicket:
    @pytest.mark.asyncio
    async def test_unknown_service_ticket(self, app):
        r = await app.get('/tvm')

        assert_that(
            await r.json(),
            equal_to({
                'code': 403,
                'status': 'fail',
                'data': {
                    'message': 'SERVICE_NOT_AUTHORIZED',
                }
            })
        )

    @pytest.fixture
    def tvm_service_id(self):
        return None


class TestInvalidUserTicket:
    @pytest.mark.asyncio
    async def test_invalid_user_ticket(self, app):
        r = await app.get('/tvm')

        assert_that(
            await r.json(),
            equal_to({
                'code': 403,
                'status': 'fail',
                'data': {
                    'message': 'USER_NOT_AUTHORIZED',
                }
            })
        )

    @pytest.fixture
    def tvm_user_id(self):
        return None


@pytest.fixture
async def app(aiohttp_client, application):
    application.router.add_view('/tvm', ExampleTvmHandler)
    return await aiohttp_client(application)
