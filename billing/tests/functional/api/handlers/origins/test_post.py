import pytest

from hamcrest import assert_that, equal_to, has_entries, has_properties, match_equality

pytestmark = pytest.mark.usefixtures(
    'mock_app_authentication',
    'setup_interactions_tvm',
    'mock_pay_backend_put_merchant_both_environments',
    'mock_pay_plus_backend_put_merchant_both_environments',
)


@pytest.mark.asyncio
async def test_returned(app, partner, role, merchant, rands, disable_tvm_checking):
    partner_id = partner.partner_id
    merchant_id = merchant.merchant_id
    origin = rands()

    r = await app.post(f'/api/web/v1/partners/{partner_id}/merchants/{merchant_id}/origins', json={"origin": origin})

    assert_that(r.status, equal_to(200))
    assert_that(
        await r.json(),
        equal_to(
            {
                'status': 'success',
                'code': 200,
                'data': match_equality(
                    has_entries(
                        merchant_id=str(merchant_id),
                        moderation=None,
                        layouts=[],
                        origin=origin,
                    )
                ),
            }
        ),
    )


@pytest.mark.asyncio
async def test_creates_origin(app, partner, role, merchant, rands, storage, disable_tvm_checking):
    partner_id = partner.partner_id
    merchant_id = merchant.merchant_id
    origin = rands()

    r = await app.post(f'/api/web/v1/partners/{partner_id}/merchants/{merchant_id}/origins', json={"origin": origin})
    assert r.status < 300

    origin_id = (await r.json())['data']['origin_id']
    assert_that(
        await storage.origin.get(origin_id),
        has_properties(
            merchant_id=merchant_id,
            origin=origin,
        ),
    )


@pytest.mark.asyncio
async def test_when_origin_already_exists__raises_409(app, partner, role, merchant, rands, disable_tvm_checking):
    partner_id = partner.partner_id
    merchant_id = merchant.merchant_id
    origin = rands()
    r = await app.post(f'/api/web/v1/partners/{partner_id}/merchants/{merchant_id}/origins', json={"origin": origin})
    assert r.status < 300

    r = await app.post(f'/api/web/v1/partners/{partner_id}/merchants/{merchant_id}/origins', json={"origin": origin})

    assert_that(r.status, equal_to(409))
    assert_that(
        await r.json(),
        equal_to(
            {
                'status': 'fail',
                'code': 409,
                'data': {'message': 'ORIGIN_ALREADY_EXISTS'},
            }
        ),
    )
