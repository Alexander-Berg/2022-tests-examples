import json
from datetime import datetime, timezone
from decimal import Decimal
from operator import itemgetter
from typing import Iterable

import pytest


class Factory:
    def __init__(self, ch_client):
        self._ch_client = ch_client

    def insert_source_datatube(self, overrides: Iterable[dict]):
        defaults = {
            "_timestamp": int(
                datetime(2020, 10, 1, 12, 0, 0, tzinfo=timezone.utc).timestamp()
            ),
            "CampaignID": 1000,
            "EventGroupId": "someeventgroupid",
            "APIKey": 4,
            "DeviceID": "hashash",
            "AppPlatform": "iOS",
            "AppVersionName": "1.2.3",
            "AppBuildNumber": 101,
            "Latitude": 55.718732876522175,
            "Longitude": 37.40151579701865,
            "EventName": "geoadv.bb.pin.show",
        }

        rows = []
        for overs in overrides:
            row = defaults.copy()
            row.update(overs)
            rows.append(row)

        self._ch_client.execute(
            """
            INSERT INTO maps_adv_statistics_raw_metrika_log_distributed (
                _timestamp,
                CampaignID,
                EventGroupId,
                APIKey,
                DeviceID,
                AppPlatform,
                AppVersionName,
                AppBuildNumber,
                Latitude,
                Longitude,
                EventName
            )
            VALUES
            """,
            rows,
        )

    def insert_source_mapkit(self, overrides: Iterable[dict]):
        defaults = {
            "receive_time": int(
                datetime(2020, 10, 1, 12, 0, 0, tzinfo=timezone.utc).timestamp()
            ),
            "event": "billboard.click",
            "reqid": "1582203548595863-15182776-man1-4265-man-addrs-advert-32299",
            "log_time": int(
                datetime(2020, 10, 1, 11, 0, 0, tzinfo=timezone.utc).timestamp()
            ),
            "req_time": int(
                datetime(2020, 10, 1, 10, 0, 0, tzinfo=timezone.utc).timestamp()
            ),
            "log_id": '{"advertiserId":"None","campaignId":"1000","product":"pin_on_route_v2","eventGroupId":"anothereventgroupid"}',  # noqa: E501
            "user_lat": 55.718732876522175,
            "user_lon": 37.40151579701865,
            "place_id": "bb:123",
            "device_id": "hashash",
            "user_agent": "{app_package}/{app_version}.{app_build} mapkit/209.46.3 runtime/202.5.5 {app_platform}/12.4.5 (Apple; iPhone7,1; ru_RU)",  # noqa: E501
            "event_group_id": "anothereventgroupid",
        }

        rows = []
        for overs in overrides:
            row = defaults.copy()
            row.update(overs)
            rows.append(row)

            row["receive_time"] = row.pop("receive_timestamp", row["receive_time"])
            row["event"] = row.pop("event_name", row["event"])
            row["user_agent"] = row["user_agent"].format(
                app_package=row.pop("app_package", "ru.yandex.yandexmaps"),
                app_version=row.pop("app_version", "432"),
                app_build=row.pop("app_build", 201),
                app_platform=row.pop("app_platform", "iphoneos"),
            )

            if "campaign_id" in row or "product" in row:
                row["log_id"] = dict(
                    advertiserId="None",
                    campaignId=str(overs.pop("campaign_id", 1000)),
                    product=row.pop("product", "pin_on_route_v2"),
                    eventGroupId=row.get("event_group_id", "anothereventgroupid"),
                )

            if isinstance(row["log_id"], (dict, list)):
                row["log_id"] = json.dumps(row["log_id"], sort_keys=True)

        self._ch_client.execute(
            """
            INSERT INTO mapkit_events_distributed (
                receive_time,
                event,
                reqid,
                log_time,
                req_time,
                log_id,
                place_id,
                user_lat,
                user_lon,
                device_id,
                user_agent,
                event_group_id
            )
            VALUES
            """,
            rows,
        )
        pass

    def insert_into_normalized(self, overrides: Iterable[dict]):
        defaults = {
            "receive_timestamp": int(
                datetime(2020, 10, 1, 12, 0, 0, tzinfo=timezone.utc).timestamp()
            ),
            "event_name": "BILLBOARD_SHOW",
            "campaign_id": 1000,
            "event_group_id": "someeventgroupidinnormalized",
            "application": "NAVIGATOR",
            "device_id": "deviceidasuid",
            "app_platform": "ANDROID",
            "app_version_name": "1.2.3",
            "app_build_number": 456,
            "user_latitude": Decimal("55.718732876"),
            "user_longitude": Decimal("37.401515797"),
            "place_id": "altay:123",
            "_normalization_metadata": '{"json": "data"}',
        }

        rows = []
        for overs in overrides:
            row = defaults.copy()
            row.update(overs)
            rows.append(row)

        self._ch_client.execute(
            """
            INSERT INTO normalized_events_distributed (
                receive_timestamp,
                event_name,
                campaign_id,
                event_group_id,
                application,
                device_id,
                app_platform,
                app_version_name,
                app_build_number,
                user_latitude,
                user_longitude,
                place_id,
                _normalization_metadata
            )
            VALUES
            """,
            rows,
        )

    def insert_into_processed(self, overrides: Iterable[dict]):
        defaults = {
            "receive_timestamp": int(
                datetime(2020, 10, 1, 12, 0, 0, tzinfo=timezone.utc).timestamp()
            ),
            "event_name": "BILLBOARD_SHOW",
            "campaign_id": 1000,
            "event_group_id": "someeventgroupidinnormalized",
            "application": "NAVIGATOR",
            "device_id": "deviceidasuid",
            "app_platform": "ANDROID",
            "app_version_name": "1.2.3",
            "app_build_number": 456,
            "user_latitude": Decimal("55.718732876"),
            "user_longitude": Decimal("37.401515797"),
            "place_id": "altay:123",
            "cost": Decimal("0"),
            "timezone": "UTC",
            "_normalization_metadata": '{"json": "data"}',
            "_processing_metadata": '{"json": "data"}',
        }

        rows = []
        for overs in overrides:
            row = defaults.copy()
            row.update(overs)
            rows.append(row)

        self._ch_client.execute(
            """
            INSERT INTO processed_events_distributed (
                receive_timestamp,
                event_name,
                campaign_id,
                event_group_id,
                application,
                device_id,
                app_platform,
                app_version_name,
                app_build_number,
                user_latitude,
                user_longitude,
                place_id, cost,
                _normalization_metadata,
                _processing_metadata
            )
            VALUES
            """,
            rows,
        )

    def get_all_normalized(self, *field_nums):
        result = self._ch_client.execute(
            """
                SELECT *
                FROM normalized_events_distributed
                ORDER BY receive_timestamp
            """
        )

        if field_nums:
            return list(map(itemgetter(*field_nums), result))
        else:
            return result

    def get_all_processed(self, *field_nums):
        result = self._ch_client.execute(
            """
                SELECT *
                FROM processed_events_distributed
                ORDER BY receive_timestamp, event_name
            """
        )

        if field_nums:
            return list(map(itemgetter(*field_nums), result))
        else:
            return result


@pytest.fixture
def factory(ch_client):
    return Factory(ch_client)
