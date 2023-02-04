import pytest

from hamcrest import assert_that, equal_to

from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.customer import Customer
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.customer_serial import CustomerSerial


@pytest.fixture
async def customer(storage):
    return await storage.customer.create(Customer(uid=1400))


@pytest.mark.asyncio
async def test_create(storage, customer):
    customer_serial = CustomerSerial(uid=customer.uid)

    created = await storage.customer_serial.create(customer_serial)

    customer_serial.created = created.created
    customer_serial.updated = created.updated
    assert_that(
        created,
        equal_to(customer_serial),
    )


@pytest.mark.asyncio
async def test_get(storage, customer):
    customer_serial = CustomerSerial(uid=customer.uid)
    created = await storage.customer_serial.create(customer_serial)

    got = await storage.customer_serial.get(customer_serial.uid)

    assert_that(
        got,
        equal_to(created),
    )


@pytest.mark.asyncio
async def test_get_not_found(storage, customer):
    with pytest.raises(CustomerSerial.DoesNotExist):
        await storage.customer_serial.get(customer.uid + 1)


@pytest.mark.asyncio
async def test_save(storage, customer):
    customer_serial = CustomerSerial(
        uid=customer.uid,
    )
    created = await storage.customer_serial.create(customer_serial)
    created.sheet_id += 1

    saved = await storage.customer_serial.save(created)

    assert_that(
        saved.sheet_id,
        equal_to(customer_serial.sheet_id + 1),
    )
