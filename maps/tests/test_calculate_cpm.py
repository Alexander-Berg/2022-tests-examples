import itertools
import pytest

from aiohttp.web import Response
from datetime import timezone
from decimal import Decimal
from google.protobuf.timestamp_pb2 import Timestamp

from maps_adv.billing_proxy.client import (
    Client,
)
from maps_adv.billing_proxy.client.lib.enums import (
    AdvRubric,
    CreativeType,
    OrderSize,
)

from maps_adv.billing_proxy.proto.common_pb2 import MoneyQuantity
from maps_adv.billing_proxy.proto.products_pb2 import (
    CpmCalculationInput,
    CpmCalculationResult,
)

from maps_adv.common.helpers import dt

pytestmark = [pytest.mark.asyncio]

BILLING_API_URL = "http://billing_proxy.server"

example_proto_result = CpmCalculationResult(
    OBSOLETE__cpm=MoneyQuantity(value=100), cpm="100.0"
)


@pytest.mark.parametrize(
    "rubric, order_size, datetime, expected_rubric, expected_order_size",
    itertools.zip_longest(
        AdvRubric,
        OrderSize,
        [dt("2020-01-01 00:00:00")],
        [
            CpmCalculationInput.AdvRubric.COMMON,
            CpmCalculationInput.AdvRubric.AUTO,
            CpmCalculationInput.AdvRubric.REALTY,
        ],
        [
            CpmCalculationInput.OrderSize.SMALL,
            CpmCalculationInput.OrderSize.BIG,
            CpmCalculationInput.OrderSize.VERY_BIG,
        ],
    ),
)
async def test_requests_data_correctly(
    mock_calculate_cpm,
    rubric,
    order_size,
    datetime,
    expected_rubric,
    expected_order_size,
):
    req_details = {}

    async def _handler(request):
        req_details.update(path=request.path, body=await request.read())
        return Response(status=200, body=example_proto_result.SerializeToString())

    mock_calculate_cpm(_handler)

    async with Client(BILLING_API_URL) as client:
        result = await client.calculate_product_cpm(
            1,
            rubric=rubric,
            targeting_query={},
            dt=datetime,
            order_size=order_size,
            creative_types=list(CreativeType),
        )

    assert req_details["path"] == "/products/1/cpm/"
    proto_body = CpmCalculationInput.FromString(req_details["body"])

    assert proto_body == CpmCalculationInput(
        rubric=expected_rubric,
        targeting_query="{}",
        dt=Timestamp(seconds=int(datetime.replace(tzinfo=timezone.utc).timestamp()))
        if datetime is not None
        else None,
        order_size=expected_order_size,
        creative_types=[
            CpmCalculationInput.CreativeType.BILLBOARD,
            CpmCalculationInput.CreativeType.PIN,
            CpmCalculationInput.CreativeType.BANNER,
            CpmCalculationInput.CreativeType.TEXT,
            CpmCalculationInput.CreativeType.ICON,
            CpmCalculationInput.CreativeType.PIN_SEARCH,
            CpmCalculationInput.CreativeType.LOGO_AND_TEXT,
            CpmCalculationInput.CreativeType.VIA_POINT,
            CpmCalculationInput.CreativeType.AUDIO_BANNER,
        ],
    )
    assert result == Decimal(100)
