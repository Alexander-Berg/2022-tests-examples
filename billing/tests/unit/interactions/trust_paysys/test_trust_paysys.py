import dataclasses

import pytest

from hamcrest import assert_that, has_properties

from billing.yandex_pay.yandex_pay.interactions.trust_paysys.exceptions import (
    BadRequestTrustPaysysError, CardNotFoundTrustPaysysError, InteractionResponseError,
    InvalidCardTokenTrustPaysysError, InvalidExpirationDateTrustPaysysError, NotFoundTrustPaysysError,
    UnknownTrustPaysysError
)


@pytest.fixture
async def trust_paysys_client(create_client):
    from billing.yandex_pay.yandex_pay.interactions import TrustPaysysClient
    client = create_client(TrustPaysysClient)
    yield client
    await client.close()


@pytest.fixture
def trust_card_id():
    return 'card-x112233'


@pytest.mark.asyncio
async def test_get_card_ok(aioresponses_mocker, yandex_pay_settings, trust_paysys_client, trust_card_id):
    aioresponses_mocker.get(
        f'{yandex_pay_settings.TRUST_PAYSYS_API_URL}/yapay/v1/cards/{trust_card_id[len("card-x"):]}',
        payload={
            'card_id': trust_card_id,
            'card_token': 'the-token',
            'holder': 'the-holder',
            'expiration_year': 2099,
            'expiration_month': 4,
        }
    )

    trust_card = await trust_paysys_client.get_card(trust_card_id)

    assert_that(
        trust_card,
        has_properties({
            'card_id': trust_card_id,
            'card_token': 'the-token',
            'holder': 'the-holder',
            'expiration_year': 2099,
            'expiration_month': 4,
        })
    )


@pytest.mark.asyncio
@pytest.mark.parametrize('http_code, error_code, expected_exc_cls', (
    (404, 'card_not_found', CardNotFoundTrustPaysysError),
    (400, 'invalid_card_token', InvalidCardTokenTrustPaysysError),
    (400, 'expiration_date_normalization_error', InvalidExpirationDateTrustPaysysError),
    (400, 'the-unknown-error', UnknownTrustPaysysError),
))
async def test_get_card_error(
    aioresponses_mocker,
    yandex_pay_settings,
    trust_paysys_client,
    trust_card_id,
    http_code,
    error_code,
    expected_exc_cls,
):
    aioresponses_mocker.get(
        f'{yandex_pay_settings.TRUST_PAYSYS_API_URL}/yapay/v1/cards/{trust_card_id[len("card-x"):]}',
        status=http_code,
        payload={
            'status': error_code,
            'status_desc': 'the-desc',
        }
    )

    with pytest.raises(expected_exc_cls) as exc_info:
        await trust_paysys_client.get_card(trust_card_id)
    assert_that(
        exc_info.value,
        has_properties({
            'status_code': http_code,
            'response_status': error_code,
            'params': {
                'desc': 'the-desc',
                'request_id': 'unittest-request-id',
                'headers': {},
            },
            'service': 'trust-paysys',
            'method': 'get',
        }),
    )


@pytest.mark.asyncio
async def test_get_card_malformed_response(aioresponses_mocker,
                                           yandex_pay_settings,
                                           trust_paysys_client,
                                           trust_card_id):
    aioresponses_mocker.get(
        f'{yandex_pay_settings.TRUST_PAYSYS_API_URL}/yapay/v1/cards/{trust_card_id[len("card-x"):]}',
        status=400,
        body='Backend unexpected error'
    )

    with pytest.raises(InteractionResponseError):
        await trust_paysys_client.get_card(trust_card_id)


@pytest.mark.asyncio
async def test_get_card_bad_card_id(aioresponses_mocker, yandex_pay_settings, trust_paysys_client):
    with pytest.raises(AssertionError):
        await trust_paysys_client.get_card('foo/bar')


@pytest.mark.asyncio
@pytest.mark.parametrize('query, show_hidden', (
    ('show_hidden=true', True),
    ('show_hidden=false', False),
))
async def test_get_user_cards_success(
    aioresponses_mocker,
    yandex_pay_settings,
    trust_paysys_client,
    query,
    show_hidden,
):
    uid = 1234567
    raw_binding = {
        "expiration_month": "01",
        "binding_ts": 1622544806.585433,
        "holder": "CARD HOLDER",
        "id": "card-x66b88c90fc8d571e6d5e9715",
        "bank": "",
        "expiration_year": "2050",
        "masked_number": "000000****0000",
        "is_verified": True,
        "system": "unknown",
        "is_removed": False,
        "remove_ts": 0,
        "is_expired": False,
    }
    aioresponses_mocker.get(
        f'{yandex_pay_settings.TRUST_PAYSYS_API_URL}/api/v1/user/cards?uid={uid}&{query}',
        status=200,
        payload={
            "id": "yyy",
            "result": [
                raw_binding,
            ]
        }
    )
    bindings = await trust_paysys_client.get_user_cards(uid, show_hidden=show_hidden)

    assert len(bindings) == 1
    assert dataclasses.asdict(bindings[0]) == raw_binding


@pytest.mark.asyncio
@pytest.mark.parametrize('http_code, error_message, expected_exc_cls', (
    (400, 'X-Request-Id header is not provided', BadRequestTrustPaysysError),
    (404, 'User bindings are not found', NotFoundTrustPaysysError),
))
async def test_get_user_cards_error(
    aioresponses_mocker,
    yandex_pay_settings,
    trust_paysys_client,
    http_code,
    error_message,
    expected_exc_cls,
):
    uid = 1234567
    aioresponses_mocker.get(
        f'{yandex_pay_settings.TRUST_PAYSYS_API_URL}/api/v1/user/cards?uid={uid}&show_hidden=true',
        status=http_code,
        payload={
            "id": "",
            "error": {
                "code": http_code,
                "message": error_message
            }
        }
    )

    with pytest.raises(expected_exc_cls) as e:
        await trust_paysys_client.get_user_cards(uid)

    exc_info = e.value

    assert exc_info.method == 'get'
    assert exc_info.status_code == http_code
    assert exc_info.response_status == 'error'
    assert exc_info.params['desc'] == error_message
