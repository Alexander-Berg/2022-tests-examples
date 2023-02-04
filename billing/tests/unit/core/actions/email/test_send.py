from uuid import UUID

import pytest

from sendr_interactions.clients.blackbox.exceptions import UserNotFoundBlackboxError
from sendr_utils import alist

from hamcrest import anything, assert_that, equal_to, has_entries, instance_of, match_equality

import billing.yandex_pay_plus.yandex_pay_plus.core.actions.email.send
from billing.yandex_pay_plus.yandex_pay_plus.core.actions.email.send import SendTransactionalEmailAction
from billing.yandex_pay_plus.yandex_pay_plus.core.actions.email.sender.send import SendTransactionalEmailToSenderAction
from billing.yandex_pay_plus.yandex_pay_plus.core.exceptions import EmailNotFoundError
from billing.yandex_pay_plus.yandex_pay_plus.interactions.blackbox import BlackBoxClient
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.enums import TaskState, TaskType
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.transactional_email import TransactionalEmail


@pytest.mark.asyncio
async def test_creates_transactional_email(storage, params):
    await SendTransactionalEmailAction(**params).run()

    created = await storage.transactional_email.get_by_idempotency_key(params['idempotency_key'])
    assert_that(
        created,
        equal_to(
            TransactionalEmail(
                idempotency_key=params['idempotency_key'],
                email='email@test',
                render_context={'text': 'variable', 'bool': True},
                sender_campaign_slug='sender_slug',
                reply_email='no-reply@test',
                has_user_generated_content=True,
                transactional_email_id=match_equality(instance_of(UUID)),
                sender_message_id=None,
                created=match_equality(anything()),
                updated=match_equality(anything()),
            )
        )
    )


@pytest.mark.asyncio
async def test_returns_transactional_email(storage, params):
    returned = await SendTransactionalEmailAction(**params).run()

    created = await storage.transactional_email.get_by_idempotency_key(params['idempotency_key'])
    assert_that(created, equal_to(returned))


@pytest.mark.asyncio
async def test_creates_send_transactional_email_task(storage, params):
    returned = await SendTransactionalEmailAction(**params).run()

    [task] = await alist(
        storage.task.find(
            filters=dict(
                task_type=TaskType.RUN_ACTION,
                state=TaskState.PENDING,
                action_name=SendTransactionalEmailToSenderAction.action_name,
            )
        )
    )
    assert_that(
        task.params,
        has_entries({
            'action_kwargs': {
                'transactional_email_id': str(returned.transactional_email_id),
            }
        })
    )


@pytest.mark.asyncio
async def test_when_transactional_email_already_exists__returns_transactional_email(storage, params):
    transactional_email = await storage.transactional_email.create(
        TransactionalEmail(
            idempotency_key=params['idempotency_key'],
            email='email@test',
            render_context={'text': 'variable', 'bool': True},
            sender_campaign_slug='sender_slug',
            reply_email='no-reply@test',
            has_user_generated_content=True,
        )
    )

    returned = await SendTransactionalEmailAction(**params).run()

    assert_that(
        returned,
        equal_to(transactional_email)
    )


@pytest.mark.asyncio
async def test_when_transactional_email_already_exists__doesnt_call_send(storage, params, mock_send_to_sender_action):
    await storage.transactional_email.create(
        TransactionalEmail(
            idempotency_key=params['idempotency_key'],
            email='email@test',
            render_context={'text': 'variable', 'bool': True},
            sender_campaign_slug='sender_slug',
            reply_email='no-reply@test',
            has_user_generated_content=True,
        )
    )

    await SendTransactionalEmailAction(**params).run()

    mock_send_to_sender_action.assert_not_called()


class TestEmailResolution:
    @pytest.mark.asyncio
    async def test_when_uid_is_given__gets_default_email_from_blackbox(self, params):
        params['uid'] = 12345
        params['email'] = None
        returned = await SendTransactionalEmailAction(**params).run()

        assert_that(
            returned.email, equal_to('default@test'),
        )

    @pytest.mark.asyncio
    async def test_when_uid_default_email_doesnt_exist__raises_error(self, params, blackbox_user_info):
        params['uid'] = 12345
        params['email'] = None
        blackbox_user_info.emails.default = None

        with pytest.raises(EmailNotFoundError):
            await SendTransactionalEmailAction(**params).run()

    @pytest.mark.asyncio
    async def test_when_uid_doesnt_exist__raises_error(self, mocker, params):
        params['uid'] = 12345
        params['email'] = None
        mocker.patch.object(
            BlackBoxClient,
            'get_user_info',
            mocker.AsyncMock(side_effect=UserNotFoundBlackboxError(status_code=200, method='GET')),
        )

        with pytest.raises(EmailNotFoundError):
            await SendTransactionalEmailAction(**params).run()

    @pytest.mark.asyncio
    async def test_when_neither_uid_nor_email_are_given(self, mocker, params):
        params['uid'] = None
        params['email'] = None

        with pytest.raises(EmailNotFoundError):
            await SendTransactionalEmailAction(**params).run()


class TestCanSend:
    @pytest.mark.asyncio
    async def test_when_whitelist_is_set_and_check_passed__calls_send(
        self, yandex_pay_plus_settings, params, mock_send_to_sender_action,
    ):
        yandex_pay_plus_settings.SENDER_EMAIL_WHITELIST = ('email@test',)
        params['email'] = 'email@test'

        await SendTransactionalEmailAction(**params).run()

        mock_send_to_sender_action.assert_called_once()

    @pytest.mark.asyncio
    async def test_when_whitelist_is_set_and_check_failed__doesnt_call_send(
        self, yandex_pay_plus_settings, params, mock_send_to_sender_action,
    ):
        yandex_pay_plus_settings.SENDER_EMAIL_WHITELIST = ('good-email@test',)
        params['email'] = 'bad-email@test'

        await SendTransactionalEmailAction(**params).run()

        mock_send_to_sender_action.assert_not_called()

    @pytest.mark.asyncio
    async def test_when_share_set_and_random_is_low__calls_send(
        self, yandex_pay_plus_settings, mocker, params, mock_send_to_sender_action,
    ):
        yandex_pay_plus_settings.SENDER_SENDING_SHARE = 50
        mocker.patch.object(
            billing.yandex_pay_plus.yandex_pay_plus.core.actions.email.send.random,
            'randint',
            mocker.Mock(return_value=49),
        )

        await SendTransactionalEmailAction(**params).run()

        mock_send_to_sender_action.assert_called_once()

    @pytest.mark.asyncio
    async def test_when_share_set_and_random_is_high__doesnt_call_send(
        self, yandex_pay_plus_settings, mocker, params, mock_send_to_sender_action,
    ):
        yandex_pay_plus_settings.SENDER_SENDING_SHARE = 50
        mocker.patch.object(
            billing.yandex_pay_plus.yandex_pay_plus.core.actions.email.send.random,
            'randint',
            mocker.Mock(return_value=50),
        )

        await SendTransactionalEmailAction(**params).run()

        mock_send_to_sender_action.assert_not_called()


@pytest.fixture
def params(rands):
    return dict(
        sender_campaign_slug='sender_slug',
        idempotency_key=rands(),
        render_context={'text': 'variable', 'bool': True},
        email='email@test',
        uid=None,
        reply_email='no-reply@test',
        has_user_generated_content=True,
    )


@pytest.fixture(autouse=True)
def set_settings_to_be_very_permissive_for_sending(yandex_pay_plus_settings):
    yandex_pay_plus_settings.SENDER_EMAIL_WHITELIST = ()
    yandex_pay_plus_settings.SENDER_SENDING_SHARE = 100


@pytest.fixture
def blackbox_user_info(mocker):
    return mocker.Mock(emails=mocker.Mock(default=mocker.Mock(address='default@test')))


@pytest.fixture(autouse=True)
def mock_blackbox(mocker, blackbox_user_info):
    return mocker.patch.object(BlackBoxClient, 'get_user_info', mocker.AsyncMock(return_value=blackbox_user_info))


@pytest.fixture
def mock_send_to_sender_action(mock_action):
    return mock_action(SendTransactionalEmailToSenderAction)
