from datetime import datetime
from decimal import Decimal

import pytest
from pay.lib.entities.shipping import DeliveryStatus
from pay.lib.interactions.yandex_delivery.entities import Claim, ClaimStatus

from sendr_pytest.matchers import equal_to
from sendr_pytest.mocks import spy_action  # noqa
from sendr_taskqueue.worker.storage.db.entities import TaskState  # noqa
from sendr_utils import alist

from hamcrest import assert_that

from billing.yandex_pay_plus.yandex_pay_plus.core.actions.delivery.accept_claim import AcceptDeliveryClaimAction
from billing.yandex_pay_plus.yandex_pay_plus.core.actions.delivery.check_state import CheckClaimStateAction
from billing.yandex_pay_plus.yandex_pay_plus.core.actions.delivery.update import UpdateDeliveryAction
from billing.yandex_pay_plus.yandex_pay_plus.interactions import YandexDeliveryClient
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.delivery import Delivery, StorageWarehouse
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.merchant import (
    DeliveryIntegrationParams,
    YandexDeliveryParams,
)


@pytest.mark.asyncio
async def test_final_status(mocker, delivery, storage, update_spy, claim):
    claim.status = ClaimStatus.DELIVERED
    ydelivery = mocker.patch.object(YandexDeliveryClient, 'get_claim', mocker.AsyncMock(return_value=claim))

    await CheckClaimStateAction(
        merchant_id=delivery.merchant_id,
        checkout_order_id=delivery.checkout_order_id,
        delivery_version=delivery.version,
    ).run()

    ydelivery.assert_called_once_with(claim_id='claim-id', auth_token='OaUtHtOkEn')
    update_spy.assert_run_once()

    tasks = await alist(storage.task.find(filters={'state': TaskState.PENDING}))

    assert len(tasks) == 1
    assert tasks[0].action_name == 'notify_merchant_delivery'


@pytest.mark.asyncio
async def test_obsolete_version(mocker, delivery, storage, update_spy, claim):
    mocker.patch.object(YandexDeliveryClient, 'get_claim', mocker.AsyncMock(return_value=claim))
    task_count = await storage.task.get_size()

    await CheckClaimStateAction(
        merchant_id=delivery.merchant_id, checkout_order_id=delivery.checkout_order_id, delivery_version=0
    ).run()

    update_spy.assert_not_run()
    assert_that(await storage.task.get_size(), equal_to(task_count))


@pytest.mark.asyncio
async def test_claim_not_updated(mocker, delivery, storage, update_spy, claim, find_delivery_tasks):
    claim.version = delivery.version
    mocker.patch.object(YandexDeliveryClient, 'get_claim', mocker.AsyncMock(return_value=claim))

    await CheckClaimStateAction(
        merchant_id=delivery.merchant_id,
        checkout_order_id=delivery.checkout_order_id,
        delivery_version=delivery.version,
    ).run()

    update_spy.assert_not_run()

    tasks = await find_delivery_tasks(action=CheckClaimStateAction, delivery_version=1)
    assert_that(len(tasks), equal_to(1))


@pytest.mark.asyncio
async def test_non_final_status(mocker, delivery, storage, update_spy, claim, find_delivery_tasks):
    claim.status = ClaimStatus.PERFORMER_LOOKUP
    mocker.patch.object(YandexDeliveryClient, 'get_claim', mocker.AsyncMock(return_value=claim))

    await CheckClaimStateAction(
        merchant_id=delivery.merchant_id,
        checkout_order_id=delivery.checkout_order_id,
        delivery_version=delivery.version,
    ).run()

    update_spy.assert_run_once()

    tasks = await find_delivery_tasks(action=CheckClaimStateAction, delivery_version=2)
    assert_that(len(tasks), equal_to(1))


@pytest.mark.parametrize('autoaccept', (True, False))
@pytest.mark.asyncio
async def test_calls_accept(mocker, delivery, storage, autoaccept, claim, find_delivery_tasks):
    claim.status = ClaimStatus.READY_FOR_APPROVAL
    mocker.patch.object(YandexDeliveryClient, 'get_claim', mocker.AsyncMock(return_value=claim))

    await CheckClaimStateAction(
        merchant_id=delivery.merchant_id,
        checkout_order_id=delivery.checkout_order_id,
        delivery_version=delivery.version,
    ).run()

    tasks = await find_delivery_tasks(action=AcceptDeliveryClaimAction)
    assert_that(len(tasks), equal_to(autoaccept))


@pytest.fixture
def find_delivery_tasks(storage, delivery):
    async def _find_tasks(action, **params):
        params['checkout_order_id'] = str(delivery.checkout_order_id)
        params['merchant_id'] = str(delivery.merchant_id)
        filters = {
            'action_name': action.action_name,
            'params': lambda x: x['action_kwargs'] == params,
        }
        return await alist(storage.task.find(filters=filters))

    return _find_tasks


@pytest.fixture
def update_spy(spy_action):  # noqa
    return spy_action(UpdateDeliveryAction)


@pytest.fixture
def claim():
    return Claim(id='claim-id', version=2, revision=1, updated_ts=datetime(2022, 2, 22), status=ClaimStatus.ESTIMATING)


@pytest.fixture
def autoaccept(request):
    return getattr(request, 'param', False)


@pytest.fixture(autouse=True)
async def merchant(storage, stored_merchant, autoaccept):
    stored_merchant.delivery_integration_params = DeliveryIntegrationParams(
        yandex_delivery=YandexDeliveryParams(
            oauth_token=YandexDeliveryParams.encrypt_oauth_token('OaUtHtOkEn'),
            autoaccept=autoaccept,
        ),
    )
    return await storage.merchant.save(stored_merchant)


@pytest.fixture
async def delivery(storage, stored_checkout_order, entity_warehouse):
    return await storage.delivery.create(
        Delivery(
            checkout_order_id=stored_checkout_order.checkout_order_id,
            merchant_id=stored_checkout_order.merchant_id,
            price=Decimal('11'),
            warehouse=StorageWarehouse.from_warehouse(entity_warehouse),
            external_id='claim-id',
            status=DeliveryStatus.ESTIMATING,
            raw_status='estimating',
        )
    )
