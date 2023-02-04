import pytest

from hamcrest import assert_that, equal_to, has_entries, has_properties

from billing.yandex_pay_plus.yandex_pay_plus.core.actions.email.add_to_maillist import AddToMaillistAction
from billing.yandex_pay_plus.yandex_pay_plus.core.exceptions import InvalidSubscriptionTokenError


class TestSubscribeToMaillist:
    URL = '/api/public/v1/maillist/subscription'

    @pytest.fixture
    def token(self, rands):
        return rands()

    @pytest.fixture
    def app(self, public_app):
        return public_app

    @pytest.mark.asyncio
    async def test_redirect_on_success(self, mock_action, app, token, yandex_pay_plus_settings):
        action = mock_action(AddToMaillistAction, action_result=True)

        r = await app.get(self.URL, params={'token': token}, allow_redirects=False)

        assert_that(
            r,
            has_properties(
                status=302,
                headers=has_entries(
                    {'Location': yandex_pay_plus_settings.API_MAILLIST_SUBSCRIBE_REDIRECTION_URL}
                )
            )
        )
        action.assert_called_once_with(token=token)

    @pytest.mark.asyncio
    async def test_return_500_if_subscription_fails(self, mock_action, app, token):
        action = mock_action(AddToMaillistAction, action_result=False)

        r = await app.get(self.URL, params={'token': token}, allow_redirects=False)

        assert_that(r.status, equal_to(500))
        assert_that(
            await r.json(),
            equal_to(
                {
                    'status': 'fail',
                    'code': 500,
                    'data': {'message': 'INTERNAL_SERVER_ERROR'},
                }
            )
        )
        action.assert_called_once_with(token=token)

    @pytest.mark.asyncio
    async def test_return_400_if_token_is_empty(self, app):
        r = await app.get(self.URL, params={'token': ''}, allow_redirects=False)

        assert_that(r.status, equal_to(400))
        assert_that(
            await r.json(),
            equal_to(
                {
                    'status': 'fail',
                    'code': 400,
                    'data': {'message': 'INVALID_SUBSCRIPTION_TOKEN'},
                }
            )
        )

    @pytest.mark.asyncio
    async def test_return_400_if_token_invalid(self, mock_action, app, token):
        action = mock_action(AddToMaillistAction, action_result=InvalidSubscriptionTokenError)

        r = await app.get(self.URL, params={'token': token}, allow_redirects=False)

        assert_that(r.status, equal_to(400))
        assert_that(
            await r.json(),
            equal_to(
                {'status': 'fail', 'code': 400, 'data': {'message': 'INVALID_SUBSCRIPTION_TOKEN'}}
            )
        )
        action.assert_called_once_with(token=token)

    @pytest.mark.asyncio
    async def test_validation_error__token_not_provided(self, mock_action, app):
        action = mock_action(AddToMaillistAction)

        r = await app.get(self.URL, allow_redirects=False)

        assert_that(r.status, equal_to(400))
        assert_that(
            await r.json(),
            equal_to(
                {
                    'status': 'fail',
                    'code': 400,
                    'data': {
                        'params': {'token': ['Missing data for required field.']},
                        'message': 'BAD_FORMAT'
                    },
                }
            )
        )
        action.assert_not_called()

    @pytest.mark.asyncio
    async def test_maillist_endpoint_unavailable_in_internal_app(
        self, db_engine, internal_app, token, mock_action
    ):
        action = mock_action(AddToMaillistAction)

        r = await internal_app.get(self.URL, params={'token': token}, allow_redirects=False)

        assert_that(r.status, equal_to(404))
        action.assert_not_called()
