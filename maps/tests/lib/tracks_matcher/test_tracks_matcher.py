from datetime import datetime, timedelta
import pytest

from maps.analyzer.libs.data.lib.time_data import Time
import maps.analyzer.toolkit.lib.schema as schema
import maps.analyzer.toolkit.lib.tracks_matcher as tm

from maps.analyzer.pylibs.test_tools.compare_tables import assert_equal_tables
from maps.pylibs.yt.lib.unwrap_yt_error import unwrap_yt_error


TRACKS_PATH = "//home/maps/jams/production/data/assessors/"
EVENTS_PATH = "//home/maps/jams/production/navi-metrics-daily/events/"
DATE_FORMAT = "%Y-%m-%d"
DATETIME_FORMAT = "%Y%m%dT%H%M%S"

ROUTE_ID_1 = "00000012-50d2-4bf0-8f76-bb99968fc9e0"
ROUTE_ID_2 = "00000053-0188-4a26-82fb-145a75e4d678"
ROUTE_TAG_1 = "UserRoute"
ROUTE_TAG_2 = "Alternative"

CLID_1 = "CLID_1"
CLID_2 = "CLID_2"

UUID_0 = "UUID_1"
UUID_1 = "UUID_1"
UUID_2 = "UUID_2"

DATE_FROM = "2018-03-01"
DAY_COUNT = 2


def get_dates_for_period(date_from, day_count):
    date_from = datetime.strptime(date_from, DATE_FORMAT)
    for d in range(0, day_count):
        yield (date_from + timedelta(days=d))


def getTrackRerord(clid, uuid, track_start_time, topology):
    for i, edgeId in enumerate(topology):
        yield {
            "clid": clid,
            "uuid": uuid,
            "track_start_time": track_start_time.strftime(DATETIME_FORMAT),
            "enter_time": (track_start_time + timedelta(seconds=i)).strftime(DATETIME_FORMAT),
            "leave_time": (track_start_time + timedelta(seconds=i+1)).strftime(DATETIME_FORMAT),
            "match_time": track_start_time.strftime(DATETIME_FORMAT),
            "travel_time": 1.,
            "persistent_id": edgeId,
            "segment_index": 0,
        }


@pytest.fixture(scope='session')
def tracks_tables(ytc):
    tracks_tables = []
    for day in get_dates_for_period(DATE_FROM, DAY_COUNT):
        tracks_table = TRACKS_PATH + datetime.strftime(day, DATE_FORMAT)
        ytc.create(
            "table",
            tracks_table,
            recursive=True,
            ignore_existing=True,
            attributes={"schema": schema.TRAVEL_TIMES_BASE_TABLE.schema}
        )
        start_1 = day
        start_2 = day + timedelta(hours=2)
        tracks = (
            [] +
            list(getTrackRerord(CLID_1, UUID_1, start_1, [2, 119130, 119131])) +
            list(getTrackRerord(CLID_1, UUID_2, start_1, [2, 104302, 119131])) +
            list(getTrackRerord(CLID_1, UUID_2, start_2, [1, 2, 104302, 119131, 4])) +
            list(getTrackRerord(CLID_2, UUID_2, start_2, [2, 3, 119131])))
        ytc.write_table(tracks_table, tracks)
        tracks_tables.append(tracks_table)
    return tracks_tables


def get_track_events(uuid, start):
    startTimeStamp = Time.from_datetime(start).to_timestamp()
    drivingSessionID = '{0}_{1}'.format(uuid, startTimeStamp)
    yield {
        "UUID": uuid,
        "EventTimestamp": int(startTimeStamp),
        "DrivingSessionID": drivingSessionID
    }
    yield {
        "UUID": uuid,
        "EventTimestamp": int(Time.from_datetime(
            start + timedelta(minutes=1)).to_timestamp()),
        "DrivingSessionID": drivingSessionID
    }
    yield {
        "UUID": uuid,
        "EventTimestamp": int(Time.from_datetime(
            start + timedelta(hours=1)).to_timestamp()),
        "DrivingSessionID": drivingSessionID
    }


@pytest.fixture(scope='session')
def event_tables(ytc):
    event_tables = []
    for day in get_dates_for_period(DATE_FROM, DAY_COUNT):
        event_table = EVENTS_PATH + datetime.strftime(day, DATE_FORMAT)
        ytc.create(
            "table",
            event_table,
            recursive=True,
            ignore_existing=True
        )
        start_1 = day
        events = (
            [] +
            list(get_track_events(UUID_1, start_1)) +
            list(get_track_events(UUID_2, start_1)))
        ytc.write_table(event_table, events)
        event_tables.append(event_table)
    return event_tables


@pytest.fixture(scope='session')
def routes_table(ytc):
    routes_table = ytc.create_temp_table()
    ytc.write_table(
        routes_table,
        [
            {
                "route_id": ROUTE_ID_1,
                "route_tag": ROUTE_TAG_1,
                "topology": tm.array_to_topology([2, 119130, 119131]),
                "uuid": UUID_0
            }, {
                "route_id": ROUTE_ID_1,
                "route_tag": ROUTE_TAG_2,
                "topology": tm.array_to_topology([2, 104302, 119131]),
                "uuid": UUID_0
            }, {
                "route_id": ROUTE_ID_2,
                "route_tag": ROUTE_TAG_1,
                "topology": tm.array_to_topology([4, 105066, 105067]),
                "uuid": UUID_0
            }, {
                "route_id": ROUTE_ID_2,
                "route_tag": ROUTE_TAG_2,
                "topology": tm.array_to_topology([4, 5, 105067]),
                "uuid": UUID_0
            }
        ]
    )
    return routes_table


def test_count_traks_for_routes(ytc, routes_table, tracks_tables):
    with unwrap_yt_error():
        result = tm.count_traks_for_routes(
            ytc,
            tracks_tables,
            routes_table)

        expected = ytc.create_temp_table()
        ytc.write_table(
            expected,
            [
                {
                    "route_id": ROUTE_ID_1,
                    "route_tag": ROUTE_TAG_1,
                    "uuid": UUID_0,
                    "track_count": 1 * len(tracks_tables)
                }, {
                    "route_id": ROUTE_ID_1,
                    "route_tag": ROUTE_TAG_2,
                    "uuid": UUID_0,
                    "track_count": 2 * len(tracks_tables)
                }, {
                    "route_id": ROUTE_ID_2,
                    "route_tag": ROUTE_TAG_1,
                    "uuid": UUID_0,
                    "track_count": 0
                }, {
                    "route_id": ROUTE_ID_2,
                    "route_tag": ROUTE_TAG_2,
                    "uuid": UUID_0,
                    "track_count": 0
                }
            ]
        )

        assert_equal_tables(ytc, expected, result, unordered=True)


def test_find_tracks_for_routes(ytc, routes_table, tracks_tables):
    with unwrap_yt_error():
        result = tm.find_tracks_for_routes(
            ytc,
            tracks_tables,
            routes_table)

        expected = ytc.create_temp_table()
        pairs = []
        for day in get_dates_for_period(DATE_FROM, DAY_COUNT):
            start_1 = day
            start_2 = day + timedelta(hours=2)
            pairs += [
                {
                    'clid': CLID_1,
                    'uuid': UUID_1,
                    'track_start_time': start_1.strftime(DATETIME_FORMAT),
                    'enter_time': start_1.strftime(DATETIME_FORMAT),
                    'persistent_id': 2,
                    'route_id': ROUTE_ID_1,
                    'route_tag': ROUTE_TAG_1,
                    'topology': tm.array_to_topology([2, 119130, 119131])
                }, {
                    'clid': CLID_1,
                    'uuid': UUID_2,
                    'track_start_time': start_1.strftime(DATETIME_FORMAT),
                    'enter_time': start_1.strftime(DATETIME_FORMAT),
                    'persistent_id': 2,
                    'route_id': ROUTE_ID_1,
                    'route_tag': ROUTE_TAG_2,
                    'topology': tm.array_to_topology([2, 104302, 119131])
                }, {
                    'clid': CLID_1,
                    'uuid': UUID_2,
                    'track_start_time': start_2.strftime(DATETIME_FORMAT),
                    'enter_time': (start_2 + timedelta(seconds=1)).strftime(DATETIME_FORMAT),
                    'persistent_id': 2,
                    'route_id': ROUTE_ID_1,
                    'route_tag': ROUTE_TAG_2,
                    'topology': tm.array_to_topology([2, 104302, 119131])
                }
            ]

        ytc.write_table(expected, pairs)
        assert_equal_tables(ytc, expected, result, unordered=True, bytestrings=True)


def test_find_alternatives_in_tracks(ytc, routes_table, tracks_tables):
    with unwrap_yt_error():
        result = tm.find_alternatives_in_tracks(
            ytc,
            tracks_tables,
            routes_table)

        expected = ytc.create_temp_table()
        ytc.write_table(
            expected,
            [
                {
                    'route_id': ROUTE_ID_1,
                    'route_tag': "T",
                    'uuid': UUID_0,
                    'topology': tm.array_to_topology([2, 104302, 119131]),
                    'track_count': 2 * len(tracks_tables),
                    'on_route_track_count': len(tracks_tables)
                }, {
                    'route_id': ROUTE_ID_1,
                    'route_tag': "T",
                    "uuid": UUID_0,
                    'topology': tm.array_to_topology([2, 119130, 119131]),
                    'track_count': len(tracks_tables),
                    'on_route_track_count': 2 * len(tracks_tables)
                }, {
                    'route_id': ROUTE_ID_1,
                    'route_tag': "T",
                    'uuid': UUID_0,
                    'topology': tm.array_to_topology([2, 3, 119131]),
                    'track_count': len(tracks_tables),
                    'on_route_track_count': 2 * len(tracks_tables)
                }, {
                    'route_id': ROUTE_ID_1,
                    'route_tag': "T",
                    'uuid': UUID_0,
                    'topology': tm.array_to_topology([2, 3, 119131]),
                    'track_count': len(tracks_tables),
                    'on_route_track_count': len(tracks_tables)
                }
            ]
        )
        assert_equal_tables(ytc, expected, result, unordered=True, bytestrings=True)


def test_filter_travel_times_with_event_log(ytc, tracks_tables, event_tables):
    with unwrap_yt_error():
        result = tm.filter_travel_times_with_event_log(
            ytc,
            tracks_tables,
            event_tables,
            60)

        expected = ytc.create_temp_table()
        tracks = []
        for day in get_dates_for_period(DATE_FROM, DAY_COUNT):
            for i, edgeId in enumerate([2, 119130, 119131]):
                tracks.append({
                    "clid": CLID_1,
                    "uuid": UUID_1,
                    "track_start_time": day.strftime(DATETIME_FORMAT),
                    "enter_time": (day + timedelta(seconds=i)).strftime(DATETIME_FORMAT),
                    "leave_time": (day + timedelta(seconds=i+1)).strftime(DATETIME_FORMAT),
                    "persistent_id": edgeId
                })
            for i, edgeId in enumerate([2, 104302, 119131]):
                tracks.append({
                    "clid": CLID_1,
                    "uuid": UUID_2,
                    "track_start_time": day.strftime(DATETIME_FORMAT),
                    "enter_time": (day + timedelta(seconds=i)).strftime(DATETIME_FORMAT),
                    "leave_time": (day + timedelta(seconds=i+1)).strftime(DATETIME_FORMAT),
                    "persistent_id": edgeId
                })

        ytc.write_table(
            expected,
            tracks
        )
        assert_equal_tables(ytc, expected, result, unordered=True)
