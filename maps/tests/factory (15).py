from decimal import Decimal

import pytest

from maps_adv.common.helpers import dt

from . import make_event, make_event_v2, make_raw_event

events = [
    (dt("2019-01-01 10:00:00"), 10, "di0", "pin.show", Decimal("0.1")),
    (dt("2019-01-01 10:05:00"), 10, "di0", "pin.show", Decimal("0.1")),
    (dt("2019-01-01 10:10:00"), 10, "di0", "pin.tap"),
    (dt("2019-01-01 10:12:00"), 10, "di0", "action.call"),
    #
    (dt("2019-01-01 14:00:00"), 10, "di1", "pin.show", Decimal("0.1")),
    (dt("2019-01-01 14:10:00"), 10, "di1", "pin.show", Decimal("0.1")),
    (dt("2019-01-01 14:11:00"), 10, "di1", "pin.tap"),
    (dt("2019-01-01 14:15:00"), 10, "di1", "action.search"),
    (dt("2019-01-01 14:16:00"), 10, "di1", "action.makeRoute"),
    #
    (dt("2019-01-01 19:00:00"), 10, "di2", "pin.show", Decimal("0.1")),
    (dt("2019-01-01 19:03:00"), 10, "di2", "pin.show", Decimal("0.1")),
    (dt("2019-01-01 19:05:00"), 10, "di2", "pin.show", Decimal("0.1")),
    (dt("2019-01-01 19:10:00"), 10, "di2", "pin.tap"),
    (dt("2019-01-01 19:11:00"), 10, "di2", "pin.tap"),
    (dt("2019-01-01 19:15:00"), 10, "di2", "action.search"),
    #
    (dt("2019-01-01 17:00:00"), 20, "di0", "pin.show", Decimal("1")),
    (dt("2019-01-01 17:02:00"), 20, "di0", "pin.tap"),
    (dt("2019-01-01 17:05:00"), 20, "di0", "action.openSite"),
    #
    (dt("2019-01-01 23:30:00"), 20, "di1", "pin.show", Decimal("1")),
    (dt("2019-01-01 23:35:00"), 20, "di1", "pin.tap"),
    (dt("2019-01-01 23:36:00"), 20, "di1", "action.openSite"),
    #
    (dt("2019-01-01 23:50:00"), 20, "di5", "pin.show", Decimal("1")),
    (dt("2019-01-01 23:58:00"), 20, "di5", "pin.tap"),
    (dt("2019-01-01 23:59:00"), 20, "di5", "action.saveOffer"),
    #
    (dt("2019-01-01 01:00:00"), 20, "di6", "pin.show", Decimal("1")),
    #
    (dt("2019-01-01 00:10:00"), 20, "di7", "pin.show", Decimal("1")),
    #
    (dt("2019-01-02 10:00:00"), 10, "di0", "pin.show", Decimal("0.1")),
    (dt("2019-01-02 10:05:00"), 10, "di0", "pin.tap"),
    (dt("2019-01-02 10:06:00"), 10, "di0", "action.call"),
    #
    (dt("2019-01-02 23:00:00"), 10, "di8", "pin.show", Decimal("0.1")),
    (dt("2019-01-02 23:00:00"), 10, "di8", "pin.tap"),
    (dt("2019-01-02 23:00:00"), 10, "di8", "action.search"),
    #
    (dt("2019-01-02 15:00:00"), 10, "di1", "pin.show", Decimal("0.1")),
    #
    (dt("2019-01-02 19:00:00"), 10, "di1", "pin.show", Decimal("0.1")),
    #
    (dt("2019-01-01 12:00:00"), 30, "di10", "pin.show", Decimal("0.1234")),
    (dt("2019-01-01 12:10:00"), 30, "di10", "pin.show", Decimal("0.1234")),
    (dt("2019-01-01 12:20:00"), 30, "di10", "pin.show", Decimal("0.1234")),
    #
    (dt("2019-01-04 12:00:00"), 40, "di20", "pin.show", Decimal("0.4444")),
    (dt("2019-01-04 12:10:00"), 40, "di20", "pin.show", Decimal("0.1111")),
]


events_v2 = [
    (dt("2019-01-01 10:00:00"), 10, "di0", "BILLBOARD_SHOW", Decimal("0.1")),
    (dt("2019-01-01 10:05:00"), 10, "di0", "BILLBOARD_SHOW", Decimal("0.1")),
    (dt("2019-01-01 10:10:00"), 10, "di0", "BILLBOARD_TAP"),
    (dt("2019-01-01 10:12:00"), 10, "di0", "ACTION_CALL"),
    #
    (dt("2019-01-01 14:00:00"), 10, "di1", "BILLBOARD_SHOW", Decimal("0.1")),
    (dt("2019-01-01 14:10:00"), 10, "di1", "BILLBOARD_SHOW", Decimal("0.1")),
    (dt("2019-01-01 14:11:00"), 10, "di1", "BILLBOARD_TAP"),
    (dt("2019-01-01 14:15:00"), 10, "di1", "ACTION_SEARCH"),
    (dt("2019-01-01 14:16:00"), 10, "di1", "ACTION_MAKE_ROUTE"),
    #
    (dt("2019-01-01 19:00:00"), 10, "di2", "BILLBOARD_SHOW", Decimal("0.1")),
    (dt("2019-01-01 19:03:00"), 10, "di2", "BILLBOARD_SHOW", Decimal("0.1")),
    (dt("2019-01-01 19:05:00"), 10, "di2", "BILLBOARD_SHOW", Decimal("0.1")),
    (dt("2019-01-01 19:10:00"), 10, "di2", "BILLBOARD_TAP"),
    (dt("2019-01-01 19:11:00"), 10, "di2", "BILLBOARD_TAP"),
    (dt("2019-01-01 19:15:00"), 10, "di2", "ACTION_SEARCH"),
    #
    (dt("2019-01-01 17:00:00"), 20, "di0", "BILLBOARD_SHOW", Decimal("1")),
    (dt("2019-01-01 17:02:00"), 20, "di0", "BILLBOARD_TAP"),
    (dt("2019-01-01 17:05:00"), 20, "di0", "ACTION_OPEN_SITE"),
    #
    (dt("2019-01-01 23:30:00"), 20, "di1", "BILLBOARD_SHOW", Decimal("1")),
    (dt("2019-01-01 23:35:00"), 20, "di1", "BILLBOARD_TAP"),
    (dt("2019-01-01 23:36:00"), 20, "di1", "ACTION_OPEN_SITE"),
    #
    (dt("2019-01-01 23:50:00"), 20, "di5", "BILLBOARD_SHOW", Decimal("1")),
    (dt("2019-01-01 23:58:00"), 20, "di5", "BILLBOARD_TAP"),
    (dt("2019-01-01 23:59:00"), 20, "di5", "ACTION_SAVE_OFFER"),
    #
    (dt("2019-01-01 01:00:00"), 20, "di6", "BILLBOARD_SHOW", Decimal("1")),
    #
    (dt("2019-01-01 00:10:00"), 20, "di7", "BILLBOARD_SHOW", Decimal("1")),
    #
    (dt("2019-01-02 10:00:00"), 10, "di0", "BILLBOARD_SHOW", Decimal("0.1")),
    (dt("2019-01-02 10:05:00"), 10, "di0", "BILLBOARD_TAP"),
    (dt("2019-01-02 10:06:00"), 10, "di0", "ACTION_CALL"),
    #
    (dt("2019-01-02 23:00:00"), 10, "di8", "BILLBOARD_SHOW", Decimal("0.1")),
    (dt("2019-01-02 23:00:00"), 10, "di8", "BILLBOARD_TAP"),
    (dt("2019-01-02 23:00:00"), 10, "di8", "ACTION_SEARCH"),
    #
    (dt("2019-01-02 15:00:00"), 10, "di1", "BILLBOARD_SHOW", Decimal("0.1")),
    #
    (dt("2019-01-02 19:00:00"), 10, "di1", "BILLBOARD_SHOW", Decimal("0.1")),
    #
    (dt("2019-01-01 12:00:00"), 30, "di10", "BILLBOARD_SHOW", Decimal("0.1234")),
    (dt("2019-01-01 12:10:00"), 30, "di10", "BILLBOARD_SHOW", Decimal("0.1234")),
    (dt("2019-01-01 12:20:00"), 30, "di10", "BILLBOARD_SHOW", Decimal("0.1234")),
    #
    (dt("2019-01-04 12:00:00"), 40, "di20", "BILLBOARD_SHOW", Decimal("0.4444")),
    (dt("2019-01-04 12:10:00"), 40, "di20", "BILLBOARD_SHOW", Decimal("0.1111")),
]


@pytest.fixture
def fill_ch(ch):
    ch.execute(
        "INSERT INTO stat.accepted_sample VALUES", [make_event(*e) for e in events]
    )
    ch.execute(
        "INSERT INTO stat.processed_events_distributed VALUES",
        [make_event_v2(*e) for e in events_v2],
    )
    ch.execute(
        "INSERT INTO stat.normalized_events_distributed VALUES",
        [make_event_v2(*e)[:4] for e in events_v2],
    )
    ch.execute(
        "INSERT INTO stat.mapkit_events_distributed VALUES",
        [
            (
                e[0],
                "billboard.show"
                if e[3] == "BILLBOARD_SHOW"
                else "billboard.navigation.via",
                f"""{{"campaignId": "{e[1]}"}}""",
            )
            for e in events_v2
            if e[3] in ["BILLBOARD_SHOW", "ACTION_MAKE_ROUTE"]
        ],
    )
    yield
    ch.execute("TRUNCATE TABLE stat.accepted_sample")
    ch.execute("TRUNCATE TABLE stat.mapkit_events_distributed")
    ch.execute("TRUNCATE TABLE stat.normalized_events_distributed")
    ch.execute("TRUNCATE TABLE stat.processed_events_distributed")
    ch.execute("TRUNCATE TABLE stat.aggregated_sample")
    ch.execute(
        "TRUNCATE TABLE stat.aggregated_processed_events_by_campaigns_and_days_distributed"
    )


category_search_report_data = [
    dict(
        campaign_id=1,
        date=dt("2019-12-01"),
        routes=100,
        devices=1000,
        pin_clicks=100,
        pin_shows=10,
        icon_clicks=1,
        icon_shows=10,
        created_at=61000,
    ),
    dict(
        campaign_id=2,
        date=dt("2019-12-01"),
        routes=200,
        devices=2000,
        pin_clicks=200,
        pin_shows=20,
        icon_clicks=2,
        icon_shows=20,
        created_at=62000,
    ),
    dict(
        campaign_id=3,
        date=dt("2019-12-01"),
        routes=300,
        devices=3000,
        pin_clicks=300,
        pin_shows=30,
        icon_clicks=3,
        icon_shows=30,
        created_at=63000,
    ),
    dict(
        campaign_id=1,
        date=dt("2019-12-02"),
        routes=1000,
        devices=100,
        pin_clicks=10,
        pin_shows=1,
        icon_clicks=10,
        icon_shows=100,
        created_at=61000,
    ),
    dict(
        campaign_id=2,
        date=dt("2019-12-02"),
        routes=2000,
        devices=200,
        pin_clicks=20,
        pin_shows=2,
        icon_clicks=20,
        icon_shows=200,
        created_at=62000,
    ),
    dict(
        campaign_id=3,
        date=dt("2019-12-02"),
        routes=3000,
        devices=300,
        pin_clicks=30,
        pin_shows=3,
        icon_clicks=30,
        icon_shows=300,
        created_at=63000,
    ),
    dict(
        campaign_id=2,
        date=dt("2019-12-03"),
        routes=2,
        devices=20,
        pin_clicks=2000,
        pin_shows=2,
        icon_clicks=200,
        icon_shows=2000,
        created_at=62000,
    ),
    dict(
        campaign_id=1,
        date=dt("2019-12-10"),
        routes=10,
        devices=1,
        pin_clicks=1,
        pin_shows=1000,
        icon_clicks=1000,
        icon_shows=1,
        created_at=61000,
    ),
    dict(
        campaign_id=1,
        date=dt("2019-12-31"),
        routes=30,
        devices=3,
        pin_clicks=3000,
        pin_shows=300,
        icon_clicks=3000,
        icon_shows=3,
        created_at=63000,
    ),
]


@pytest.fixture
async def fill_category_search_report_table(con):
    sql = """
        INSERT INTO category_search_report
            (campaign_id, date, routes, devices,
            pin_clicks, pin_shows, icon_clicks, icon_shows, created_at)
        VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9)
    """

    await con.executemany(
        sql, [[*record.values()] for record in category_search_report_data]
    )


raw_events = [
    (dt("2020-07-27 00:01:00"), 111, "di10", "geoadv.bb.pin.show"),
    (dt("2020-07-27 00:02:00"), 111, "di11", "geoadv.bb.pin.show"),
    (dt("2020-07-27 00:02:30"), 222, "di20", "geoadv.bb.pin.show"),
    (dt("2020-07-27 00:02:40"), 222, "di20", "geoadv.bb.pin.tap"),
    (dt("2020-07-27 00:03:00"), 333, "di30", "geoadv.bb.pin.show"),
    (dt("2020-07-27 00:03:05"), 333, "di30", "geoadv.bb.pin.show"),
    (dt("2020-07-27 00:03:06"), 333, "di31", "geoadv.bb.pin.show"),
    (dt("2020-07-27 00:03:10"), 333, "di31", "geoadv.bb.pin.tap"),
    (dt("2020-07-27 00:04:00"), 444, "di40", "geoadv.bb.pin.show"),
    (dt("2020-07-27 00:04:01"), 444, "di40", "geoadv.bb.pin.tap"),
    (dt("2020-07-27 00:04:02"), 444, "di41", "geoadv.bb.pin.show"),
    (dt("2020-07-27 00:04:03"), 444, "di42", "geoadv.bb.pin.show"),
    (dt("2020-07-27 00:05:00"), 555, "di50", "geoadv.bb.pin.show"),
    (dt("2020-07-27 00:05:01"), 555, "di50", "geoadv.bb.pin.show"),
]


@pytest.fixture
def fill_raw_table(ch, setup_ch_for_monitorings):
    ch.execute(
        "INSERT INTO stat.maps_adv_statistics_raw_metrika_log_distributed VALUES",
        [make_raw_event(*e) for e in raw_events],
    )
    yield
    ch.execute("TRUNCATE TABLE stat.maps_adv_statistics_raw_metrika_log_distributed")
