import pytest
from pay.integration.steps.checkout.order import OrderStepper
from pay.lib.entities.operation import OperationStatus
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.enums import TransactionStatus


@pytest.mark.asyncio
async def test_capture(clients, catalog, logger):
    # curl -i https://pci-tf.fin.yandex.net/api/bind_card --data \
    #   '{"params": {"cvn": "123", "token":"", "expiration_year": "2032", \
    #   "expiration_month": "12", "card_number": "4111111111100031", "service_token": ""}}'
    # TODO: upsert psp
    # TODO: upsert integration
    stepper = OrderStepper(
        user=catalog.users.default, merchant=catalog.merchants.payture_non3ds, logger=logger, clients=clients
    )

    await stepper.create_order()
    await stepper.create_transaction()
    await stepper.wait_transaction_status(TransactionStatus.AUTHORIZED)
    operation = await stepper.capture()
    await stepper.wait_operation_status(operation.external_operation_id, OperationStatus.SUCCESS)


@pytest.mark.asyncio
async def test_cancel(clients, catalog, logger):
    stepper = OrderStepper(
        user=catalog.users.default,
        merchant=catalog.merchants.payture_non3ds,
        logger=logger,
        clients=clients,
    )

    await stepper.create_order()
    await stepper.create_transaction()
    await stepper.wait_transaction_status(TransactionStatus.AUTHORIZED)
    operation = await stepper.cancel()
    await stepper.wait_operation_status(operation.external_operation_id, OperationStatus.SUCCESS)


@pytest.mark.asyncio
async def test_refund(clients, catalog, logger):
    stepper = OrderStepper(
        user=catalog.users.default,
        merchant=catalog.merchants.payture_non3ds,
        logger=logger,
        clients=clients,
    )

    await stepper.create_order()
    await stepper.create_transaction()
    await stepper.wait_transaction_status(TransactionStatus.AUTHORIZED)
    operation = await stepper.capture()
    await stepper.wait_operation_status(operation.external_operation_id, OperationStatus.SUCCESS)
    operation = await stepper.refund()
    await stepper.wait_operation_status(operation.external_operation_id, OperationStatus.SUCCESS)
