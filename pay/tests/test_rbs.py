import pytest

from pay.integration.steps.checkout.order import OrderStepper
from pay.integration.steps.threeds.rbs import RBSThreeDSStepper
from pay.integration.tests.marks import mark_selenium
from pay.lib.entities.operation import OperationStatus
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.enums import TransactionStatus


class BaseRBSTests:
    @pytest.mark.asyncio
    async def test_capture(self, stepper):
        # curl -i https://pci-tf.fin.yandex.net/api/bind_card --data \
        #   '{"params": {"cvn": "123", "token":"", "expiration_year": "2032", \
        #   "expiration_month": "12", "card_number": "4111111111100031", "service_token": ""}}'
        # TODO: upsert psp
        # TODO: upsert integration
        await stepper.create_order(currency_code='RUB')
        await stepper.create_transaction()
        await stepper.wait_transaction_status(TransactionStatus.AUTHORIZED)
        operation = await stepper.capture()
        await stepper.wait_operation_status(operation.external_operation_id, OperationStatus.SUCCESS)

    @pytest.mark.asyncio
    async def test_cancel(self, stepper):
        await stepper.create_order(currency_code='RUB')
        await stepper.create_transaction()
        await stepper.wait_transaction_status(TransactionStatus.AUTHORIZED)
        operation = await stepper.cancel()
        await stepper.wait_operation_status(operation.external_operation_id, OperationStatus.SUCCESS)

    @pytest.mark.asyncio
    async def test_refund(self, stepper):
        await stepper.create_order(currency_code='RUB')
        await stepper.create_transaction()
        await stepper.wait_transaction_status(TransactionStatus.AUTHORIZED)
        operation = await stepper.capture()
        await stepper.wait_operation_status(operation.external_operation_id, OperationStatus.SUCCESS)
        operation = await stepper.refund()
        await stepper.wait_operation_status(operation.external_operation_id, OperationStatus.SUCCESS)

    @mark_selenium
    @pytest.mark.asyncio
    async def test_3dsv1(self, catalog, logger, clients):
        stepper = OrderStepper(
            user=catalog.users.rbs_3dsv1, merchant=catalog.merchants.alfabank, logger=logger, clients=clients
        )
        tds_stepper = RBSThreeDSStepper(user=stepper.user, logger=logger)

        await stepper.create_order(currency_code='RUB')
        await stepper.create_transaction()
        transaction_view = await stepper.wait_transaction_status(TransactionStatus.THREEDS_CHALLENGE)

        action_url = transaction_view["action_url"]
        tds_stepper.challenge(action_url)
        await stepper.wait_transaction_status(TransactionStatus.AUTHORIZED, timeout=10)

    @mark_selenium
    @pytest.mark.asyncio
    async def test_3dsv2_frictionless(self, catalog, logger, clients):
        stepper = OrderStepper(
            user=catalog.users.rbs_3dsv2_frictionless,
            merchant=catalog.merchants.alfabank,
            logger=logger,
            clients=clients,
        )
        tds_stepper = RBSThreeDSStepper(user=stepper.user, logger=logger)

        await stepper.create_order(currency_code='RUB')
        await stepper.create_transaction()
        transaction_view = await stepper.wait_transaction_status(TransactionStatus.FINGERPRINTING, timeout=10)

        action_url = transaction_view["action_url"]
        tds_stepper.v2_method(action_url)

        await stepper.wait_transaction_status(TransactionStatus.AUTHORIZED, timeout=10)
        operation = await stepper.capture()
        await stepper.wait_operation_status(operation.external_operation_id, OperationStatus.SUCCESS)

    @mark_selenium
    @pytest.mark.skip('Всё ещё падает почему-то. Надо разбираться')
    @pytest.mark.asyncio
    async def test_3dsv2_challenge(self, catalog, logger, clients):
        stepper = OrderStepper(
            user=catalog.users.rbs_3dsv2_challenge, merchant=catalog.merchants.alfabank, logger=logger, clients=clients
        )
        tds_stepper = RBSThreeDSStepper(user=stepper.user, logger=logger)
        await stepper.create_order(currency_code='RUB')
        await stepper.create_transaction()
        transaction_view = await stepper.wait_transaction_status(TransactionStatus.FINGERPRINTING)

        action_url = transaction_view["action_url"]
        tds_stepper.v2_method(action_url)
        transaction_view = await stepper.wait_transaction_status(TransactionStatus.THREEDS_CHALLENGE, timeout=10)

        action_url = transaction_view["action_url"]
        tds_stepper.rbs_v2_challenge(action_url)
        await stepper.wait_transaction_status(TransactionStatus.AUTHORIZED, timeout=10)
        operation = await stepper.capture()
        await stepper.wait_operation_status(operation.external_operation_id, OperationStatus.SUCCESS)


class TestAlfaBank(BaseRBSTests):
    @pytest.fixture
    def stepper(self, clients, catalog, logger):
        return OrderStepper(
            user=catalog.users.rbs_non3ds, merchant=catalog.merchants.alfabank, logger=logger, clients=clients
        )


class TestMTS(BaseRBSTests):
    @pytest.fixture
    def stepper(self, clients, catalog, logger):
        return OrderStepper(
            user=catalog.users.mts_non3ds, merchant=catalog.merchants.rbs_mts, logger=logger, clients=clients
        )
