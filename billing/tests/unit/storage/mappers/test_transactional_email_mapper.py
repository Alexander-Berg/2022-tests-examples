from uuid import uuid4

import pytest

from hamcrest import anything, assert_that, equal_to, match_equality

from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.transactional_email import TransactionalEmail


@pytest.mark.asyncio
async def test_create(storage, transactional_email):
    created = await storage.transactional_email.create(transactional_email)

    transactional_email.created = match_equality(anything())
    transactional_email.updated = match_equality(anything())
    assert_that(
        created,
        equal_to(transactional_email),
    )


@pytest.mark.asyncio
async def test_get(storage, transactional_email):
    created = await storage.transactional_email.create(transactional_email)

    got = await storage.transactional_email.get(transactional_email.transactional_email_id)

    assert_that(
        got,
        equal_to(created),
    )


@pytest.mark.asyncio
async def test_get_not_found(storage):
    with pytest.raises(TransactionalEmail.DoesNotExist):
        await storage.transactional_email.get(uuid4())


@pytest.mark.asyncio
async def test_save(storage):
    transactional_email = TransactionalEmail(
        idempotency_key='idkey',
        email='email@test',
        render_context={'var': 'iable', 'bool': True},
        sender_campaign_slug='sender_slug',
        reply_email='no-reply@test',
        has_user_generated_content=True
    )
    created = await storage.transactional_email.create(transactional_email)
    created.idempotency_key = 'idkey2'
    created.email = 'pochta@test'
    created.render_context['some'] = 'thing'
    created.sender_campaign_slug = 'other_slug'
    created.reply_email = 'reply@test'
    created.has_user_generated_content = False
    created.sender_message_id = 'sndrmsgid'

    saved = await storage.transactional_email.save(created)

    created.updated = saved.updated
    assert_that(
        saved,
        equal_to(created),
    )


@pytest.mark.asyncio
async def test_order_idempotency_key_is_unique(storage, transactional_email):
    await storage.transactional_email.create(transactional_email)

    with pytest.raises(TransactionalEmail.IdempotencyKeyAlreadyExists):
        transactional_email.transactional_email_id = uuid4()
        await storage.transactional_email.create(transactional_email)


@pytest.mark.asyncio
async def test_get_by_idempotency_key(storage, transactional_email):
    transactional_email = await storage.transactional_email.create(transactional_email)

    found = await storage.transactional_email.get_by_idempotency_key(transactional_email.idempotency_key)

    assert_that(found, equal_to(transactional_email))


@pytest.fixture
def transactional_email():
    return TransactionalEmail(
        idempotency_key='idkey',
        email='email@test',
        render_context={'var': 'iable', 'bool': True},
        sender_campaign_slug='sender_slug',
        reply_email='no-reply@test',
        has_user_generated_content=True
    )
