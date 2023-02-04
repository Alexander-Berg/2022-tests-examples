import pytest

from billing.yandex_pay.yandex_pay.core.entities.user import User


@pytest.fixture(autouse=True)
def configure_origins(yandex_pay_settings, allowed_origin):
    # NOTE: работает только потому, что app инициализируется позже этой фикстуры
    yandex_pay_settings.API_ALLOWED_ORIGINS = {allowed_origin}


@pytest.fixture
def no_cors_route() -> str:
    return '/api/v1/keys/keys.json'


@pytest.fixture
def allowed_origin(yandex_pay_settings) -> str:
    return 'test'


def assert_response_has_expected_access_origin_header_value(response, expected_value):
    assert 'Access-Control-Allow-Origin' in response.headers and \
           response.headers['Access-Control-Allow-Origin'] == expected_value and \
           'Access-Control-Allow-Credentials' in response.headers and \
           response.headers['Access-Control-Allow-Credentials'] == 'true'


@pytest.mark.asyncio
async def test_should_set_allow_access_origin_header(
    app,
    allowed_origin,
    mocker,
):
    mocker.patch('sendr_auth.BlackboxAuthenticator.get_user', mocker.AsyncMock(return_value=User(42)))
    r = await app.get(
        '/api/v1/tokenization/acceptance',
        headers={'Origin': allowed_origin},
    )

    assert_response_has_expected_access_origin_header_value(r, allowed_origin)


@pytest.mark.asyncio
async def test_should_support_preflight_options_method_and_set_allow_access_origin_header(
    app,
    allowed_origin,
):
    r = await app.options(
        '/api/v1/tokenization/acceptance',
        headers={
            'Origin': allowed_origin,
            'Access-Control-Request-Method': 'GET',
        },
    )

    assert_response_has_expected_access_origin_header_value(r, allowed_origin)


@pytest.mark.asyncio
async def test_authentication_is_not_needed_for_preflight_requests(
    app,
    allowed_origin,
):
    r = await app.options(
        'api/v1/tokenization/acceptance',
        headers={
            'Origin': allowed_origin,
            'Access-Control-Request-Method': 'POST',
        },
    )

    assert_response_has_expected_access_origin_header_value(r, allowed_origin)


@pytest.mark.asyncio
async def test_should_not_set_allow_access_origin_header_if_origin_is_not_expected(
    app,
    mocker,
):
    mocker.patch('sendr_auth.BlackboxAuthenticator.get_user', mocker.AsyncMock(return_value=User(42)))
    r = await app.get(
        '/api/v1/tokenization/acceptance',
        headers={'Origin': 'some unsupported origin'},
    )

    assert 'Access-Control-Allow-Origin' not in r.headers


@pytest.mark.asyncio
async def test_should_not_set_allow_access_origin_header_if_route_is_internal(
    app,
    no_cors_route,
    allowed_origin,
):
    r = await app.get(
        no_cors_route,
        headers={'Origin': allowed_origin},
    )

    assert r.status == 200 and 'Access-Control-Allow-Origin' not in r.headers
