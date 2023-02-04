import pytest

from hamcrest import assert_that, equal_to

from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.customer import Customer


@pytest.mark.asyncio
async def test_create(storage):
    customer = Customer(uid=1300)

    created = await storage.customer.create(customer)

    customer.created = created.created
    customer.updated = created.updated
    assert_that(
        created,
        equal_to(customer),
    )


@pytest.mark.asyncio
async def test_get(storage):
    customer = Customer(
        uid=1300,
    )
    created = await storage.customer.create(customer)

    got = await storage.customer.get(1300)

    assert_that(
        got,
        equal_to(created),
    )


@pytest.mark.asyncio
async def test_get_not_found(storage):
    with pytest.raises(Customer.DoesNotExist):
        await storage.customer.get(1301)


@pytest.mark.asyncio
async def test_save(storage):
    customer = Customer(
        uid=1300,
    )
    created = await storage.customer.create(customer)

    saved = await storage.customer.save(customer)

    assert_that(
        saved,
        equal_to(created),
    )
