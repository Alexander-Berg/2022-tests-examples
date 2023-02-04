import pytest

from hamcrest import assert_that, equal_to


@pytest.mark.asyncio
async def test_response(app, yandex_pay_settings):
    r = await app.get('api/jwks')
    json_body = await r.json()

    assert_that(r.status, equal_to(200))
    assert_that(
        json_body,
        equal_to({
            'keys': [
                {
                    'alg': 'ES256',
                    'kty': 'EC',
                    'crv': 'P-256',
                    'x': 'LEBfQpwTDXJtLFiPcnYvGv-WaFXZGBnFP_yGhLL9MGc',
                    'y': 'a1Or3ovkpH12b0o3ruZUtm_z8bg3xQtHXi-uPC7UJT0',
                    'kid': 'test-key',
                },
            ],
        })
    )
