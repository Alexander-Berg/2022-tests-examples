import pytest

from hamcrest import assert_that, equal_to

from billing.yandex_pay_plus.yandex_pay_plus.core.actions.email.sender.send import SendTransactionalEmailToSenderAction
from billing.yandex_pay_plus.yandex_pay_plus.interactions.sender import SenderClient
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.transactional_email import TransactionalEmail


@pytest.fixture
async def transactional_email(storage, rands):
    return await storage.transactional_email.create(
        TransactionalEmail(
            idempotency_key=rands(),
            email='email@test',
            render_context={'con': 'text', 'variable': True},
            sender_campaign_slug='campaign_slug',
            has_user_generated_content=True,
            sender_message_id=None,
        )
    )


@pytest.fixture(autouse=True)
def mock_sender_client(mocker):
    return mocker.patch.object(
        SenderClient, 'send_transactional_email', mocker.AsyncMock(return_value=mocker.Mock(message_id='sndr_msg_id'))
    )


@pytest.mark.asyncio
@pytest.mark.parametrize('reply_email', ['no-reply@test', None])
async def test_calls_sender_client(transactional_email, mock_sender_client, reply_email, storage):
    transactional_email.reply_email = reply_email
    await storage.transactional_email.save(transactional_email)

    await SendTransactionalEmailToSenderAction(transactional_email.transactional_email_id).run()

    mock_sender_client.assert_called_once_with(
        campaign_slug='campaign_slug',
        to_email='email@test',
        render_context={'con': 'text', 'variable': True},
        reply_email=reply_email,
        has_user_generated_content=True,
    )


@pytest.mark.asyncio
async def test_saves_message_id(storage, transactional_email):
    await SendTransactionalEmailToSenderAction(transactional_email.transactional_email_id).run()

    updated_transactional_email = await storage.transactional_email.get(transactional_email.transactional_email_id)
    assert_that(updated_transactional_email.sender_message_id, equal_to('sndr_msg_id'))


@pytest.mark.asyncio
async def test_returns_transactional_email(storage, transactional_email):
    returned_transactional_email = await SendTransactionalEmailToSenderAction(
        transactional_email.transactional_email_id
    ).run()

    updated_transactional_email = await storage.transactional_email.get(transactional_email.transactional_email_id)
    assert_that(returned_transactional_email, equal_to(updated_transactional_email))


@pytest.mark.asyncio
async def test_accepts_stringified_uuid(storage, transactional_email):
    returned_transactional_email = await SendTransactionalEmailToSenderAction(
        str(transactional_email.transactional_email_id)
    ).run()

    updated_transactional_email = await storage.transactional_email.get(transactional_email.transactional_email_id)
    assert_that(returned_transactional_email, equal_to(updated_transactional_email))
