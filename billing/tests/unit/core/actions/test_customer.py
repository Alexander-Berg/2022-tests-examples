import pytest

from hamcrest import assert_that, equal_to, has_property

from billing.yandex_pay_plus.yandex_pay_plus.core.actions.customer import CreateCustomerAction, EnsureCustomerAction
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.customer import Customer


class TestCreateCustomerAction:
    @pytest.mark.asyncio
    async def test_create_customer_returns_customer(self, storage):
        assert_that(
            await CreateCustomerAction(uid=100500).run(),
            has_property('uid', 100500),
        )

    @pytest.mark.asyncio
    async def test_create_customer_creates_serial(self, storage):
        await CreateCustomerAction(uid=100500).run(),

        assert_that(
            await storage.customer_serial.get(100500),
            has_property('uid', 100500),
        )


class TestEnsureCustomerAction:
    @pytest.fixture
    def created_customer(self, mocker):
        return mocker.Mock()

    @pytest.fixture(autouse=True)
    def mock_create_customer(self, mock_action, created_customer):
        return mock_action(CreateCustomerAction, created_customer)

    @pytest.mark.asyncio
    async def test_when_customer_exists__returns_customer(self, storage):
        await storage.customer.create(Customer(uid=100500))

        customer = await EnsureCustomerAction(uid=100500).run()

        assert_that(
            customer,
            has_property('uid', 100500),
        )

    @pytest.mark.asyncio
    async def test_when_customer_exists__does_not_call_create(self, storage, mock_create_customer):
        await storage.customer.create(Customer(uid=100500))

        await EnsureCustomerAction(uid=100500).run()

        mock_create_customer.assert_not_called()

    @pytest.mark.asyncio
    async def test_when_customer_is_new__returns_new_customer(self, storage, created_customer):
        customer = await EnsureCustomerAction(uid=100500).run()

        assert_that(
            customer,
            equal_to(created_customer),
        )

    @pytest.mark.asyncio
    async def test_when_customer_exists__calls_create(self, storage, mock_create_customer):
        await EnsureCustomerAction(uid=100500).run()

        mock_create_customer.assert_called_once_with(uid=100500)
