import pytest
from datetime import datetime, timedelta, timezone

from maps_adv.adv_store.api.proto.campaign_pb2 import CampaignPaidTillChangeInput
from maps_adv.adv_store.api.proto.error_pb2 import Error
from google.protobuf import timestamp_pb2

pytestmark = [pytest.mark.asyncio]

URL = "/campaigns/{}/paid-till/"


async def test_sets_campaign_paid_till(api, factory, con):
    campaign_id = (await factory.create_campaign())["id"]
    paid_till_ts = int((datetime.now(timezone.utc) + timedelta(days=1)).timestamp())
    paid_till_pb = timestamp_pb2.Timestamp(seconds=paid_till_ts)

    input_pb = CampaignPaidTillChangeInput(paid_till=paid_till_pb)
    await api.put(URL.format(campaign_id), proto=input_pb, expected_status=200)

    sql = """
        SELECT paid_till
        FROM campaign
        WHERE id = $1
    """
    assert int((await con.fetchval(sql, campaign_id)).timestamp()) == paid_till_ts


async def test_updates_paid_till(api, factory, con):
    campaign_id = (await factory.create_campaign())["id"]
    paid_till_ts = int((datetime.now(timezone.utc) + timedelta(days=1)).timestamp())
    paid_till_pb = timestamp_pb2.Timestamp(seconds=paid_till_ts)
    input_pb = CampaignPaidTillChangeInput(paid_till=paid_till_pb)

    await api.put(URL.format(campaign_id), proto=input_pb, expected_status=200)

    sql = """
        SELECT paid_till
        FROM campaign
        WHERE id = $1
    """
    assert int((await con.fetchval(sql, campaign_id)).timestamp()) == paid_till_ts

    paid_till_ts = int((datetime.now(timezone.utc) + timedelta(weeks=1)).timestamp())
    paid_till_pb = timestamp_pb2.Timestamp(seconds=paid_till_ts)
    input_pb = CampaignPaidTillChangeInput(paid_till=paid_till_pb)

    await api.put(URL.format(campaign_id), proto=input_pb, expected_status=200)

    sql = """
        SELECT paid_till
        FROM campaign
        WHERE id = $1
    """
    assert int((await con.fetchval(sql, campaign_id)).timestamp()) == paid_till_ts

    input_pb = CampaignPaidTillChangeInput(paid_till=None)
    await api.put(URL.format(campaign_id), proto=input_pb, expected_status=200)

    sql = """
        SELECT paid_till
        FROM campaign
        WHERE id = $1
    """
    assert await con.fetchval(sql, campaign_id) is None


async def test_fails_on_no_campaign(api):
    input_pb = CampaignPaidTillChangeInput(paid_till=None)
    got = await api.put(
        URL.format(100), proto=input_pb, decode_as=Error, expected_status=404
    )
    assert got == Error(code=Error.CAMPAIGN_NOT_FOUND)


async def test_raises_on_before_now(api, factory):
    campaign_id = (await factory.create_campaign())["id"]
    paid_till_ts = int((datetime.now(timezone.utc) - timedelta(days=1)).timestamp())
    paid_till_pb = timestamp_pb2.Timestamp(seconds=paid_till_ts)
    input_pb = CampaignPaidTillChangeInput(paid_till=paid_till_pb)

    got = await api.put(
        URL.format(campaign_id), proto=input_pb, decode_as=Error, expected_status=400
    )
    assert got == Error(code=Error.PAID_TILL_IS_TOO_SMALL)


async def test_raises_on_before_current(api, factory):
    campaign_id = (await factory.create_campaign())["id"]

    paid_till_ts = int((datetime.now(timezone.utc) + timedelta(days=5)).timestamp())
    paid_till_pb = timestamp_pb2.Timestamp(seconds=paid_till_ts)
    input_pb = CampaignPaidTillChangeInput(paid_till=paid_till_pb)

    await api.put(URL.format(campaign_id), proto=input_pb, expected_status=200)

    paid_till_ts = int((datetime.now(timezone.utc) + timedelta(days=4)).timestamp())
    paid_till_pb = timestamp_pb2.Timestamp(seconds=paid_till_ts)
    input_pb = CampaignPaidTillChangeInput(paid_till=paid_till_pb)

    got = await api.put(
        URL.format(campaign_id), proto=input_pb, decode_as=Error, expected_status=400
    )
    assert got == Error(code=Error.PAID_TILL_IS_TOO_SMALL)


# async def test_raises_on_before_current(campaigns_domain, campaigns_dm):
