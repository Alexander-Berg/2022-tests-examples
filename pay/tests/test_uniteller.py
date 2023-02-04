import pytest
from pay.integration.steps.checkout.order import OrderStepper
from pay.lib.entities.operation import OperationStatus
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.enums import TransactionStatus


@pytest.fixture
def stepper(clients, catalog, logger):
    return OrderStepper(
        user=catalog.users.default, merchant=catalog.merchants.uniteller, logger=logger, clients=clients
    )


@pytest.mark.skip
@pytest.mark.asyncio
async def test_capture(stepper):
    # curl -i https://pci-tf.fin.yandex.net/api/bind_card --data \
    #   '{"params": {"cvn": "123", "token":"", "expiration_year": "2032", \
    #   "expiration_month": "12", "card_number": "4111111111100031", "service_token": ""}}'
    # TODO: upsert psp
    # TODO: upsert integration
    await stepper.create_order(currency_code='RUB')
    await stepper.create_transaction()
    operation = await stepper.capture()
    await stepper.wait_operation_status(operation.external_operation_id, OperationStatus.SUCCESS)


@pytest.mark.skip
@pytest.mark.asyncio
async def test_cancel(stepper):
    await stepper.create_order(currency_code='RUB')
    await stepper.create_transaction()
    await stepper.wait_transaction_status(TransactionStatus.AUTHORIZED)
    operation = await stepper.cancel()
    await stepper.wait_operation_status(operation.external_operation_id, OperationStatus.SUCCESS)


@pytest.mark.skip
@pytest.mark.asyncio
async def test_refund(stepper):
    await stepper.create_order(currency_code='RUB')
    await stepper.create_transaction()
    await stepper.wait_transaction_status(TransactionStatus.AUTHORIZED)
    operation = await stepper.capture()
    await stepper.wait_operation_status(operation.external_operation_id, OperationStatus.SUCCESS)
    operation = await stepper.refund()
    await stepper.wait_operation_status(operation.external_operation_id, OperationStatus.SUCCESS)
