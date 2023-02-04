import pytest

from hamcrest import assert_that, equal_to, match_equality, not_none

from billing.yandex_pay.yandex_pay.core.actions.user import GetUserSettingsAction, UpsertUserSettingsAction
from billing.yandex_pay.yandex_pay.core.entities.user import User, UserSettings
from billing.yandex_pay.yandex_pay.tests.entities import APIKind


@pytest.fixture(params=(APIKind.WEB, APIKind.MOBILE))
def api_kind(request):
    return request.param


@pytest.fixture
def api_url(api_kind):
    return {
        APIKind.WEB: '/api/v1/user_settings',
        APIKind.MOBILE: '/api/mobile/v1/user_settings',
    }[api_kind]


@pytest.fixture(autouse=True)
def mock_authentication(mocker, uid):
    return mocker.patch('sendr_auth.BlackboxAuthenticator.get_user', mocker.AsyncMock(return_value=User(uid)))


@pytest.fixture
def uid(randn):
    return randn()


@pytest.fixture
async def user_settings(uid, storage) -> UserSettings:
    return await storage.user_settings.create(
        UserSettings(
            uid=uid,
            address_id='addr',
            contact_id='cont',
            card_id='card',
        )
    )


def get_expected_json_body(user_settings: UserSettings):
    return {
        'code': 200,
        'status': 'success',
        'data': {
            'user_settings': {
                'address_id': user_settings.address_id,
                'contact_id': user_settings.contact_id,
                'card_id': user_settings.card_id,
                'is_checkout_onboarded': user_settings.is_checkout_onboarded,
            }
        }
    }


@pytest.mark.asyncio
async def test_should_respond_user_settings(
    app,
    api_url,
    user_settings: UserSettings,
    mocker,
    uid,
):
    action_ctor_spy = mocker.spy(GetUserSettingsAction, '__init__')

    r = await app.get(api_url)
    json_body = await r.json()

    assert_that(r.status, equal_to(200))
    assert_that(json_body, equal_to(get_expected_json_body(user_settings)))
    action_ctor_spy.assert_called_once_with(match_equality(not_none()), uid)


@pytest.mark.asyncio
async def test_should_update_user_settings(
    app,
    api_url,
    user_settings: UserSettings,
    storage,
    uid,
    mocker,
):
    action_ctor_spy = mocker.spy(UpsertUserSettingsAction, '__init__')

    r = await app.put(
        api_url,
        json={
            'contact_id': '',
            'card_id': None,
            'is_checkout_onboarded': False,
        },
    )
    json_body = await r.json()

    from_base = await storage.user_settings.get(uid)
    assert from_base.address_id == user_settings.address_id
    assert from_base.contact_id is None
    assert from_base.card_id == user_settings.card_id
    assert from_base.is_checkout_onboarded is False
    assert_that(r.status, equal_to(200))
    assert_that(json_body, equal_to(get_expected_json_body(from_base)))
    action_ctor_spy.assert_called_once_with(
        match_equality(not_none()),
        uid=uid,
        contact_id='',
        card_id=None,
        is_checkout_onboarded=False,
    )
