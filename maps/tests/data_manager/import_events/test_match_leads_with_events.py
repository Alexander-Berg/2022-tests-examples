from operator import itemgetter

import pytest

from maps_adv.geosmb.promoter.server.tests import make_single_import_event_generator

pytestmark = [pytest.mark.asyncio]

get_lead_ids = itemgetter("passport_uid", "device_id", "yandex_uid")


@pytest.mark.parametrize(
    ("lead_params", "event_params"),
    [
        ({"passport_uid": "11"}, {"passport_uid": "22"}),
        ({"device_id": "111"}, {"device_id": "222"}),
        ({"yandex_uid": "1111"}, {"yandex_uid": "2222"}),
        (
            {"passport_uid": "11", "device_id": "111"},
            {"passport_uid": "22", "device_id": "222"},
        ),
        (
            {"passport_uid": "11", "yandex_uid": "1111"},
            {"passport_uid": "22", "yandex_uid": "2222"},
        ),
    ],
)
async def test_does_not_match_with_different_params(
    factory, dm, lead_params, event_params
):
    await factory.create_lead(**lead_params)

    await dm.import_events_from_generator(
        make_single_import_event_generator(**event_params)
    )

    leads = await factory.list_leads()
    assert len(leads) == 2
    existing_lead, created_lead = leads
    assert len(await factory.list_lead_events(existing_lead["id"])) == 0
    assert len(await factory.list_lead_events(created_lead["id"])) == 1


@pytest.mark.parametrize(
    ("event_device_id", "event_yandex_uid"),
    [(None, None), (None, "1111"), ("111", None), ("111", "1111")],
)
async def test_matches_by_passport_uid(factory, dm, event_device_id, event_yandex_uid):
    await factory.create_lead(passport_uid="11", device_id=None, yandex_uid=None)

    await dm.import_events_from_generator(
        make_single_import_event_generator(
            passport_uid="11", device_id=event_device_id, yandex_uid=event_yandex_uid
        )
    )

    leads = await factory.list_leads()
    assert len(leads) == 1
    lead = leads[0]
    assert get_lead_ids(lead) == ("11", event_device_id, event_yandex_uid)
    assert len(await factory.list_lead_events(lead["id"])) == 1


@pytest.mark.parametrize(("device_id", "yandex_uid"), [("111", None), (None, "1111")])
async def test_matches_by_secondary_param_if_both_passport_uids_are_none(
    factory, dm, device_id, yandex_uid
):
    await factory.create_lead(
        passport_uid=None, device_id=device_id, yandex_uid=yandex_uid
    )

    await dm.import_events_from_generator(
        make_single_import_event_generator(
            passport_uid=None, device_id=device_id, yandex_uid=yandex_uid
        )
    )

    leads = await factory.list_leads()
    assert len(leads) == 1
    lead = leads[0]
    assert get_lead_ids(lead) == (None, device_id, yandex_uid)
    assert len(await factory.list_lead_events(lead["id"])) == 1


@pytest.mark.parametrize(("device_id", "yandex_uid"), [("111", None), (None, "1111")])
async def test_matches_by_secondary_param_if_lead_passport_uid_is_none(
    factory, dm, device_id, yandex_uid
):
    await factory.create_lead(
        passport_uid=None, device_id=device_id, yandex_uid=yandex_uid
    )

    await dm.import_events_from_generator(
        make_single_import_event_generator(
            passport_uid="11", device_id=device_id, yandex_uid=yandex_uid
        )
    )

    leads = await factory.list_leads()
    assert len(leads) == 1
    lead = leads[0]
    assert get_lead_ids(lead) == ("11", device_id, yandex_uid)
    assert len(await factory.list_lead_events(lead["id"])) == 1


@pytest.mark.parametrize(("device_id", "yandex_uid"), [("111", None), (None, "1111")])
async def test_does_not_match_by_secondary_param_if_lead_passport_uid_is_not_none(
    factory, dm, device_id, yandex_uid
):
    await factory.create_lead(
        passport_uid="11", device_id=device_id, yandex_uid=yandex_uid
    )

    await dm.import_events_from_generator(
        make_single_import_event_generator(
            passport_uid=None, device_id=device_id, yandex_uid=yandex_uid
        )
    )

    leads = sorted(await factory.list_leads(), key=itemgetter("id"))
    assert len(leads) == 2
    existing_lead, created_lead = leads
    assert get_lead_ids(existing_lead) == ("11", device_id, yandex_uid)
    assert get_lead_ids(created_lead) == (None, device_id, yandex_uid)
    assert len(await factory.list_lead_events(existing_lead["id"])) == 0
    assert len(await factory.list_lead_events(created_lead["id"])) == 1


@pytest.mark.parametrize(("device_id", "yandex_uid"), [("111", None), (None, "1111")])
async def test_does_not_match_by_secondary_param_if_passport_uids_differ(
    factory, dm, device_id, yandex_uid
):
    await factory.create_lead(
        passport_uid="11", device_id=device_id, yandex_uid=yandex_uid
    )

    await dm.import_events_from_generator(
        make_single_import_event_generator(
            passport_uid="22", device_id=device_id, yandex_uid=yandex_uid
        )
    )

    leads = await factory.list_leads()
    assert len(leads) == 2
    existing_lead, created_lead = leads
    assert get_lead_ids(existing_lead) == ("11", device_id, yandex_uid)
    assert get_lead_ids(created_lead) == ("22", device_id, yandex_uid)
    assert len(await factory.list_lead_events(existing_lead["id"])) == 0
    assert len(await factory.list_lead_events(created_lead["id"])) == 1


@pytest.mark.parametrize(
    "params",
    [
        {"passport_uid": "11"},
        {"device_id": "111"},
        {"yandex_uid": "1111"},
        {"passport_uid": "11", "device_id": "111"},
    ],
)
async def test_does_not_match_different_biz_ids(factory, dm, params):
    await factory.create_lead(biz_id=123, **params)

    await dm.import_events_from_generator(
        make_single_import_event_generator(biz_id=456, **params)
    )

    assert len(await factory.list_leads()) == 2
