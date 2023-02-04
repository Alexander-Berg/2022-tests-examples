from decimal import Decimal
from uuid import uuid4

import psycopg2.errors
import pytest

from hamcrest import assert_that, contains_inanyorder, equal_to

from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.customer import Customer
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.enums import NFCRewardTransactionState
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.nfc_install_reward import NFCInstallReward


@pytest.fixture
async def customer(storage):
    return await storage.customer.create(Customer(uid=1400))


@pytest.fixture
def make_reward(storage, customer, rands):
    def _inner(**kwargs):
        kwargs = dict(
            uid=customer.uid,
            device_id=rands(),
            amount=Decimal('200'),
        ) | kwargs
        return NFCInstallReward(**kwargs)

    return _inner


@pytest.mark.asyncio
async def test_create(storage, customer, make_reward):
    reward = make_reward()

    created = await storage.nfc_install_reward.create(reward)

    reward.reward_id = created.reward_id
    reward.created = created.created
    reward.updated = created.updated
    assert_that(
        created,
        equal_to(reward),
    )


@pytest.mark.asyncio
async def test_duplicate_uid_not_allowed(make_reward, storage):
    await storage.nfc_install_reward.create(make_reward())

    pattern = 'uniq_nfc_install_rewards_uid'
    with pytest.raises(psycopg2.errors.UniqueViolation, match=pattern):
        await storage.nfc_install_reward.create(make_reward())


@pytest.mark.asyncio
async def test_duplicate_device_id_not_allowed(make_reward, storage):
    reward1 = make_reward(device_id='device_id')
    await storage.nfc_install_reward.create(reward1)

    new_customer = await storage.customer.create(Customer(uid=reward1.uid + 1))
    reward2 = make_reward(device_id='device_id', uid=new_customer.uid)
    pattern = 'uniq_nfc_install_rewards_device_id'
    with pytest.raises(psycopg2.errors.UniqueViolation, match=pattern):
        await storage.nfc_install_reward.create(reward2)


@pytest.mark.asyncio
async def test_get(storage, customer, make_reward):
    reward = make_reward()

    created = await storage.nfc_install_reward.create(reward)
    got = await storage.nfc_install_reward.get(created.reward_id)

    assert_that(
        got,
        equal_to(created),
    )


@pytest.mark.asyncio
async def test_get_not_found(storage):
    with pytest.raises(NFCInstallReward.DoesNotExist):
        await storage.nfc_install_reward.get(uuid4())


@pytest.mark.asyncio
async def test_save(storage, make_reward):
    reward = make_reward()
    created = await storage.nfc_install_reward.create(reward)
    created.amount += 1
    created.trust_purchase_token = 'some_token'
    created.transaction_state = NFCRewardTransactionState.CREATED

    saved = await storage.nfc_install_reward.save(created)

    created.updated = saved.updated
    assert_that(
        saved,
        equal_to(created),
    )


@pytest.mark.asyncio
async def test_get_by_uid_or_device_id(storage, make_reward, randn, rands):
    reward1 = make_reward()
    reward1 = await storage.nfc_install_reward.create(reward1)

    new_customer = await storage.customer.create(Customer(uid=reward1.uid + 1))
    reward2 = make_reward(uid=new_customer.uid)
    reward2 = await storage.nfc_install_reward.create(reward2)

    loaded_all = await storage.nfc_install_reward.get_rewards_for_uid_or_device_id(
        uid=reward1.uid,
        device_id=reward2.device_id,
    )
    assert_that(loaded_all, contains_inanyorder(reward1, reward2))

    loaded_first = await storage.nfc_install_reward.get_rewards_for_uid_or_device_id(
        uid=reward1.uid,
        device_id=reward1.device_id,
    )
    assert_that(loaded_first, equal_to([reward1]))

    loaded_second = await storage.nfc_install_reward.get_rewards_for_uid_or_device_id(
        uid=reward2.uid,
        device_id=reward2.device_id,
    )
    assert_that(loaded_second, equal_to([reward2]))

    loaded_none = await storage.nfc_install_reward.get_rewards_for_uid_or_device_id(
        uid=randn(),
        device_id=rands(),
    )
    assert_that(loaded_none, equal_to([]))
