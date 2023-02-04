import aiohttp.test_utils
import pytest

import hamcrest


@pytest.mark.asyncio
async def test_ping(app_client: aiohttp.test_utils.TestClient):
    resp = await app_client.get(path='ping')
    assert resp.status == 200
    assert await resp.text() == 'pong'


@pytest.mark.asyncio
async def test_blackbox(app_client: aiohttp.test_utils.TestClient):
    resp = await app_client.get(path='/blackbox')
    assert resp.status == 200
    data = await resp.json()
    hamcrest.assert_that(
        data,
        hamcrest.has_entries({
            'uid': hamcrest.has_entries({
                'value': hamcrest.instance_of(int),
            }),
            'status': hamcrest.has_entries({'value': 'VALID'})
        })
    )


@pytest.mark.asyncio
async def test_blackbox_with_default_uid(app_client: aiohttp.test_utils.TestClient):
    resp = await app_client.get(path='/blackbox', params={'default_uid': 777})
    assert resp.status == 200
    data = await resp.json()
    hamcrest.assert_that(
        data,
        hamcrest.has_entries({
            'uid': hamcrest.has_entries({
                'value': 777,
            }),
            'status': hamcrest.has_entries({'value': 'VALID'})
        })
    )


@pytest.mark.asyncio
async def test_trust_gateway(app_client: aiohttp.test_utils.TestClient):
    uid = 252627

    resp = await app_client.get(
        path='/trust-gateway/legacy/payment-methods',
        headers={'X-Uid': str(uid)},
    )

    assert resp.status == 200

    data = await resp.json()
    hamcrest.assert_that(
        data,
        hamcrest.has_entries({
            "status": "success",
            "bound_payment_methods": hamcrest.has_items(
                hamcrest.has_entries({
                    "region_id": 225,
                    "payment_method": "card",
                    "system": "MasterCard",
                    "expiration_month": "01",
                    "card_country": "RUS",
                    "binding_ts": "1586458392.247",
                    "ebin_tags_version": 0,
                    "card_level": "STANDARD",
                    "holder": "Card Holder",
                    "id": "card-a1a1234567a12abcd12345a1a",
                    "payment_system": "MasterCard",
                    "last_paid_ts": "1586458392.247",
                    "account": "123456****7890",
                    "ebin_tags": [],
                    "expiration_year": "2030",
                    "aliases": [
                        "card-a1a1234567a12abcd12345a1a"
                    ],
                    "expired": False,
                    "card_bank": "SBERBANK OF RUSSIA",
                    "card_id": "card-a1a1234567a12abcd12345a1a",
                    "recommended_verification_type": "standard2",
                    "orig_uid": str(uid),
                    "binding_systems": [
                        "trust"
                    ]
                })
            ),
        })
    )
