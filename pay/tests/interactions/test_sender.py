import random
import re
import string

import pytest
from aiohttp import BasicAuth, TCPConnector

from hamcrest import all_of, assert_that, has_entries, has_properties, instance_of, none

from pay.lib.interactions.sender.client import AbstractSenderClient
from pay.lib.interactions.sender.enums import SenderResultStatus
from pay.lib.interactions.sender.exceptions import SenderMessageNotFoundError, SenderResponseError

SENDER_URL = 'https://sender.yandex-team.ru/api/0'
SENDER_CLIENT_ACCOUNT_SLUG = 'efg'
SENDER_AUTH_LOGIN = 'abc'


class SenderClient(AbstractSenderClient):
    BASE_URL = SENDER_URL
    DEBUG = False
    REQUEST_RETRY_TIMEOUTS = ()
    REQUEST_TIMEOUT = 5.0
    CONNECTOR = TCPConnector()

    basic_auth_login = SENDER_AUTH_LOGIN
    account_slug = SENDER_CLIENT_ACCOUNT_SLUG


@pytest.fixture
async def sender_client(create_interaction_client) -> AbstractSenderClient:
    client = create_interaction_client(SenderClient)
    yield client
    await client.close()


@pytest.fixture
def rands_url_safe():
    def _inner(k: int = 16) -> str:
        return ''.join(random.choices(string.ascii_letters + string.digits, k=k))

    return _inner


@pytest.fixture
def sender_campaign_slug(rands_url_safe):
    return rands_url_safe()


@pytest.fixture
def maillist_slug(rands_url_safe):
    return rands_url_safe()


@pytest.fixture
def unsubscribe_list_slug(rands_url_safe):
    return rands_url_safe()


@pytest.fixture
def message_id(rands):
    return rands()


@pytest.fixture
def task_id(rands):
    return rands()


@pytest.fixture
def email():
    return 'test@example.test'


@pytest.fixture
def fake_login(rands_url_safe):
    return rands_url_safe()


class TestSendTransactionalEmail:
    @pytest.fixture
    def endpoint_url(self, sender_campaign_slug):
        base_url = SENDER_URL
        account_slug = SENDER_CLIENT_ACCOUNT_SLUG
        return f'{base_url}/{account_slug}/transactional/{sender_campaign_slug}/send'

    @pytest.mark.asyncio
    async def test_send_to_email(
        self,
        sender_client: AbstractSenderClient,
        sender_campaign_slug,
        endpoint_url,
        aioresponses_mocker,
        email,
        message_id,
        task_id,
    ):
        payload = {
            'params': {'control': {'async': True}},
            'result': {'status': 'ok', 'message_id': message_id, 'task_id': task_id},
        }
        mock_ = aioresponses_mocker.post(endpoint_url, payload=payload)
        render_context = {'key1': 'value1', 'key2': 42}

        response = await sender_client.send_transactional_email(
            sender_campaign_slug, to_email=email, render_context=render_context
        )

        assert_that(
            response,
            has_properties(
                status=SenderResultStatus.OK,
                message_id=message_id,
                task_id=task_id,
            ),
        )

        mock_.assert_called_once()
        assert_that(
            mock_.call_args.kwargs,
            has_entries(
                json={
                    'to_email': email,
                    'async': True,
                    'args': render_context,
                    'has_ugc': False,
                    'for_testing': False,
                },
            ),
        )

    @pytest.mark.asyncio
    async def test_send_to_uid(
        self,
        sender_client: AbstractSenderClient,
        sender_campaign_slug,
        endpoint_url,
        aioresponses_mocker,
        randn,
        message_id,
        task_id,
    ):
        uid = randn()
        payload = {
            'params': {'control': {'async': True}},
            'result': {'status': 'ok', 'message_id': message_id, 'task_id': task_id},
        }
        mock_ = aioresponses_mocker.post(endpoint_url, payload=payload)

        response = await sender_client.send_transactional_email(sender_campaign_slug, to_uid=uid)

        assert_that(
            response,
            has_properties(
                status=SenderResultStatus.OK,
                message_id=message_id,
                task_id=task_id,
            ),
        )

        mock_.assert_called_once()
        assert_that(
            mock_.call_args.kwargs,
            has_entries(
                json={
                    'to_yandex_puid': uid,
                    'async': True,
                    'args': {},
                    'has_ugc': False,
                    'for_testing': False,
                },
            ),
        )

    @pytest.mark.asyncio
    async def test_either_email_or_uid_must_be_provided(
        self, sender_client: AbstractSenderClient, sender_campaign_slug
    ):
        pattern = 'Either to_email or to_uid must be provided'
        with pytest.raises(ValueError, match=pattern):
            await sender_client.send_transactional_email(sender_campaign_slug)

    @pytest.mark.asyncio
    async def test_generic_json_exception(
        self,
        sender_client: AbstractSenderClient,
        sender_campaign_slug,
        email,
        endpoint_url,
        aioresponses_mocker,
    ):
        payload = {'code': 599, 'msg': 'Server Error'}
        mock_ = aioresponses_mocker.post(endpoint_url, payload=payload, status=500)

        with pytest.raises(SenderResponseError) as exc_info:
            await sender_client.send_transactional_email(sender_campaign_slug, to_email=email)

        mock_.assert_called_once()
        assert_that(
            exc_info.value,
            has_properties(
                status_code=500,
                method='post',
                service=sender_client.SERVICE,
                params=payload,
            ),
        )

    @pytest.mark.asyncio
    async def test_generic_text_exception(
        self,
        sender_client: AbstractSenderClient,
        sender_campaign_slug,
        email,
        endpoint_url,
        aioresponses_mocker,
    ):
        body = 'Fake error body'
        mock_ = aioresponses_mocker.post(endpoint_url, body=body, status=500, content_type='text/plain')

        with pytest.raises(SenderResponseError) as exc_info:
            await sender_client.send_transactional_email(sender_campaign_slug, to_email=email)

        mock_.assert_called_once()
        assert_that(
            exc_info.value,
            has_properties(
                status_code=500,
                method='post',
                service=sender_client.SERVICE,
                params={'response_text': body},
            ),
        )


class TestCheckTransactionalEmailStatus:
    @pytest.fixture
    def endpoint_url(self, sender_campaign_slug):
        base_url = SENDER_URL
        account_slug = SENDER_CLIENT_ACCOUNT_SLUG
        return re.compile(f'^{base_url}/{account_slug}/transactional/{sender_campaign_slug}/status.*$')

    @pytest.mark.asyncio
    async def test_check_succeeds(
        self,
        sender_client: AbstractSenderClient,
        sender_campaign_slug,
        message_id,
        endpoint_url,
        aioresponses_mocker,
        task_id,
    ):
        payload = {'code': 200, 'message_id': message_id, 'msg': 'test', 'retry': False}
        mock_ = aioresponses_mocker.get(endpoint_url, payload=payload)

        response = await sender_client.check_transactional_email_status(sender_campaign_slug, message_id)

        assert_that(
            response,
            has_properties(
                code=200,
                message='test',
                retry=False,
            ),
        )
        mock_.assert_called_once()
        assert_that(
            mock_.call_args.kwargs,
            has_entries(params={'message_id': message_id}),
        )

    @pytest.mark.asyncio
    async def test_message_not_found(
        self,
        sender_client: AbstractSenderClient,
        sender_campaign_slug,
        message_id,
        endpoint_url,
        aioresponses_mocker,
    ):
        payload = {'code': 404, 'message_id': message_id, 'msg': 'Not Found', 'retry': False}
        mock_ = aioresponses_mocker.get(endpoint_url, payload=payload)

        with pytest.raises(SenderMessageNotFoundError):
            await sender_client.check_transactional_email_status(sender_campaign_slug, message_id)

        mock_.assert_called_once()

    @pytest.mark.asyncio
    async def test_sender_returns_500(
        self,
        sender_client: AbstractSenderClient,
        sender_campaign_slug,
        message_id,
        endpoint_url,
        aioresponses_mocker,
    ):
        payload = {'code': 500}
        mock_ = aioresponses_mocker.get(endpoint_url, payload=payload)

        with pytest.raises(SenderResponseError):
            await sender_client.check_transactional_email_status(sender_campaign_slug, message_id)

        mock_.assert_called_once()


class TestMaillist:
    @pytest.fixture
    def endpoint_url(self, maillist_slug):
        base_url = SENDER_URL
        account_slug = SENDER_CLIENT_ACCOUNT_SLUG
        return f'{base_url}/{account_slug}/maillist/{maillist_slug}/subscription'

    @pytest.mark.asyncio
    async def test_add_to_maillist(
        self,
        sender_client: AbstractSenderClient,
        maillist_slug,
        email,
        endpoint_url,
        aioresponses_mocker,
    ):
        subscriber_params = {'var1': 'value1'}
        payload = {
            'params': {'email': email, 'params': subscriber_params},
            'result': {'status': 'OK'},
        }
        mock_ = aioresponses_mocker.put(endpoint_url, payload=payload)

        response = await sender_client.add_to_maillist(maillist_slug, email, subscriber_params=subscriber_params)

        assert_that(response, has_properties(status=SenderResultStatus.OK))

        mock_.assert_called_once()
        assert_that(
            mock_.call_args.kwargs,
            has_entries(
                json={
                    'email': email,
                    'params': subscriber_params,
                }
            ),
        )

    @pytest.mark.asyncio
    async def test_remove_from_maillist(
        self,
        sender_client: AbstractSenderClient,
        maillist_slug,
        email,
        endpoint_url,
        aioresponses_mocker,
    ):
        payload = {
            'params': email,
            'result': {'status': 'OK', 'disabled': 1, 'already_disabled': 0},
        }
        mock_ = aioresponses_mocker.delete(endpoint_url, payload=payload)

        response = await sender_client.remove_from_maillist(maillist_slug, email)

        assert_that(
            response,
            has_properties(
                status=SenderResultStatus.OK,
                disabled=True,
                already_disabled=False,
            ),
        )

        mock_.assert_called_once()
        assert_that(
            mock_.call_args.kwargs,
            has_entries(json={'email': email}),
        )


class TestUnsubscribeList:
    @pytest.fixture
    def endpoint_url(self, unsubscribe_list_slug):
        base_url = SENDER_URL
        account_slug = SENDER_CLIENT_ACCOUNT_SLUG
        return re.compile(f'^{base_url}/{account_slug}/unsubscribe/list/{unsubscribe_list_slug}.*$')

    @pytest.mark.asyncio
    async def test_add_to_unsubscribe_list(
        self,
        sender_client: AbstractSenderClient,
        unsubscribe_list_slug,
        email,
        endpoint_url,
        aioresponses_mocker,
    ):
        payload = {
            'params': {'email': email},
            'result': {'status': 'ok'},
        }
        mock_ = aioresponses_mocker.put(endpoint_url, payload=payload)

        response = await sender_client.add_to_unsubscribe_list(unsubscribe_list_slug, email)

        assert_that(response, has_properties(status=SenderResultStatus.OK))

        mock_.assert_called_once()
        assert_that(
            mock_.call_args.kwargs,
            has_entries(params={'email': email}),
        )

    @pytest.mark.asyncio
    async def test_remove_from_unsubscribe_list(
        self,
        sender_client: AbstractSenderClient,
        unsubscribe_list_slug,
        email,
        endpoint_url,
        aioresponses_mocker,
    ):
        payload = {
            'params': {'email': email},
            'result': {'status': 'ok'},
        }
        mock_ = aioresponses_mocker.delete(endpoint_url, payload=payload)

        response = await sender_client.remove_from_unsubscribe_list(unsubscribe_list_slug, email)

        assert_that(response, has_properties(status=SenderResultStatus.OK))

        mock_.assert_called_once()
        assert_that(
            mock_.call_args.kwargs,
            has_entries(params={'email': email}),
        )


class TestAuth:
    def test_auth_with_login(self, sender_client):
        sender_client.TVM_ID = None

        session = sender_client.session

        assert_that(
            session,
            has_properties(
                auth=all_of(
                    instance_of(BasicAuth),
                    has_properties(login=SENDER_AUTH_LOGIN, password=''),
                )
            ),
        )

    def test_auth_with_tvm(self, sender_client, randn):
        sender_client.TVM_ID = randn()

        session = sender_client.session

        assert_that(session, has_properties(auth=none()))
