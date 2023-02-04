import pytest
from aiohttp.web import Response
from datetime import datetime, timedelta, timezone

from maps_adv.billing_proxy.proto.orders_pb2 import OrderIds
from maps_adv.export.lib.pipeline.exceptions import StepException
from maps_adv.export.lib.pipeline.steps import BillingStep

pytestmark = [pytest.mark.asyncio]

PAID_TILL_PAST = datetime.now(timezone.utc) - timedelta(days=7)
PAID_TILL_FUTURE = datetime.now(timezone.utc) + timedelta(days=7)


async def test_returns_order_ids(config, mock_billing):
    campaigns = [
        {"id": 1212, "order_id": 1345},
        {"id": 8764, "order_id": 3465},
        {"id": 4326, "order_id": 3465},
        {"id": 6284, "order_id": 3465},
        # order_id will be not in list
        {"id": 9876, "order_id": 7654},
        {"id": 7632, "order_id": 7654},
        {"id": 4567, "order_id": 8764},
        # campaign with None order_id
        {"id": 3456, "order_id": None},
        # campaign without order_id
        {"id": 3786},
        # will be on the list because of paid_till
        {"id": 10001, "order_id": 1345, "paid_till": PAID_TILL_FUTURE},
        {"id": 10002, "order_id": 7653, "paid_till": PAID_TILL_FUTURE},
        {"id": 10003, "paid_till": PAID_TILL_FUTURE},
        # will be on the list because of order, despite paid_till
        {"id": 10004, "order_id": 1345, "paid_till": PAID_TILL_PAST},
        # wont be on the list because of paid_till
        {"id": 10005, "order_id": 7653, "paid_till": PAID_TILL_PAST},
        # will be on the list casue of no order_id
        {"id": 10006, "order_id": None, "paid_till": PAID_TILL_PAST},
        {"id": 10007, "paid_till": PAID_TILL_PAST},
    ]

    message = OrderIds(order_ids=[1345, 3465]).SerializeToString()
    mock_billing(Response(body=message, status=200))

    campaigns = await BillingStep(config)(campaigns)

    assert sorted(campaigns, key=lambda x: x["id"]) == sorted(
        [
            {"id": 1212, "order_id": 1345},
            {"id": 8764, "order_id": 3465},
            {"id": 4326, "order_id": 3465},
            {"id": 6284, "order_id": 3465},
            # campaign with None order_id
            {"id": 3456, "order_id": None},
            # campaign without order_id
            {"id": 3786},
            {"id": 10001, "order_id": 1345, "paid_till": PAID_TILL_FUTURE},
            {"id": 10002, "order_id": 7653, "paid_till": PAID_TILL_FUTURE},
            {"id": 10003, "paid_till": PAID_TILL_FUTURE},
            {"id": 10004, "order_id": 1345, "paid_till": PAID_TILL_PAST},
            {"id": 10006, "order_id": None, "paid_till": PAID_TILL_PAST},
            {"id": 10007, "paid_till": PAID_TILL_PAST},
        ],
        key=lambda x: x["id"],
    )


async def test_returns_nothing_if_empty_order_list(config, mock_billing):
    campaigns = []

    message = OrderIds(order_ids=[]).SerializeToString()
    mock_billing(Response(body=message, status=200))

    campaigns = await BillingStep(config)(campaigns)

    assert campaigns == []


async def test_raises_for_unknown_error(config, mock_billing):
    mock_billing(Response(body=b"", status=404))

    with pytest.raises(StepException):
        await BillingStep(config)([{"id": 1212, "order_id": 1345}])
