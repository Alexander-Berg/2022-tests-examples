import pytest

from maps_adv.adv_store.v2.tests import dt

pytestmark = [pytest.mark.asyncio, pytest.mark.freeze_time(dt("2020-02-28 07:59:30"))]


async def test_receives_used_audiences(campaigns_dm, factory):
    await factory.create_campaign(
        end_datetime=dt("2020-04-02 12:00:00"),
        targeting={"tag": "audience", "attributes": {"id": "1111"}},
    )
    await factory.create_campaign(
        end_datetime=dt("2020-04-02 12:00:00"),
        targeting={"tag": "not_audience", "attributes": {"id": "3333"}},
    )

    got = await campaigns_dm.retrieve_targetings()

    assert sorted(got, key=lambda x: x["tag"]) == sorted(
        [
            {"tag": "audience", "attributes": {"id": "1111"}},
            {"tag": "not_audience", "attributes": {"id": "3333"}},
        ],
        key=lambda x: x["tag"],
    )


async def test_receives_used_audiences_for_future_campaigns(campaigns_dm, factory):
    await factory.create_campaign(
        start_datetime=dt("2020-04-02 12:00:00"),
        end_datetime=dt("2021-04-02 12:00:00"),
        targeting={"tag": "audience", "attributes": {"id": "1111"}},
    )

    got = await campaigns_dm.retrieve_targetings()

    assert got == [{"tag": "audience", "attributes": {"id": "1111"}}]


async def test_ignores_ended_campaigns(campaigns_dm, factory):
    # campaigns ended
    await factory.create_campaign(
        end_datetime=dt("2020-02-02 12:00:00"),
        targeting={"tag": "audience", "attributes": {"id": "1111"}},
    )
    await factory.create_campaign(
        end_datetime=dt("2019-04-02 12:00:00"),
        targeting={"tag": "audience", "attributes": {"id": "3333"}},
    )
    # ongoing campaign
    await factory.create_campaign(
        end_datetime=dt("2020-02-28 07:59:30"),
        targeting={"tag": "audience", "attributes": {"id": "2222"}},
    )

    got = await campaigns_dm.retrieve_targetings()

    assert got == [{"tag": "audience", "attributes": {"id": "2222"}}]


async def test_returns_empty_list_if_nothing_found(campaigns_dm, factory):
    # campaign in the past
    await factory.create_campaign(
        end_datetime=dt("2019-04-02 12:00:00"),
        targeting={"tag": "audience", "attributes": {"id": "1111"}},
    )
    # campaign without targeting
    await factory.create_campaign(
        end_datetime=dt("2020-03-02 12:00:00"), targeting=None
    )

    got = await campaigns_dm.retrieve_targetings()

    assert got == []
