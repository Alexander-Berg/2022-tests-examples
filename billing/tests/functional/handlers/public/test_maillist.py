import pytest
from yarl import URL

from hamcrest import assert_that, has_entries, has_properties

from billing.yandex_pay_plus.yandex_pay_plus.api.public_app import YandexPayPlusPublicApplication
from billing.yandex_pay_plus.yandex_pay_plus.core.actions.email.add_to_maillist import AddToMaillistAction


@pytest.fixture
def application(db_engine) -> YandexPayPlusPublicApplication:
    # overrides the application fixture from common_conftest.py
    return YandexPayPlusPublicApplication(db_engine=db_engine)


@pytest.fixture
def uid(randn):
    return randn()


@pytest.fixture
def email():
    return 'test@example.tld'


@pytest.fixture
def sender_endpoint_url(yandex_pay_plus_settings) -> URL:
    base_url = URL(yandex_pay_plus_settings.SENDER_CLIENT_API_URL)
    account_slug = yandex_pay_plus_settings.SENDER_CLIENT_ACCOUNT_SLUG
    maillist_slug = yandex_pay_plus_settings.SENDER_MAILLIST_SLUG
    return base_url / account_slug / 'maillist' / maillist_slug / 'subscription'


@pytest.fixture(autouse=True)
def mock_sender_client(email, sender_endpoint_url, aioresponses_mocker):
    payload = {'params': {'email': email}, 'result': {'status': 'OK'}}
    return aioresponses_mocker.put(sender_endpoint_url, payload=payload)


@pytest.mark.asyncio
async def test_subscribe_to_maillist(
    uid, email, app, mock_sender_client, yandex_pay_plus_settings
):
    token = AddToMaillistAction.generate_token(uid, email)

    r = await app.get(
        '/api/public/v1/maillist/subscription',
        params={'token': token},
        allow_redirects=False,
    )

    assert_that(
        r,
        has_properties(
            status=302,
            headers=has_entries(
                {'Location': yandex_pay_plus_settings.API_MAILLIST_SUBSCRIBE_REDIRECTION_URL}
            )
        )
    )

    mock_sender_client.assert_called_once()
    _, call_kwargs = mock_sender_client.call_args
    assert_that(call_kwargs, has_entries(json={'email': email}))
