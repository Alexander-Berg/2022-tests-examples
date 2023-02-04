import pytest
import typing

from dataclasses import dataclass

from infi.clickhouse_orm.database import Database

from infra.rtc_sla_tentacles.backend.lib.config.interface import ConfigInterface
from infra.rtc_sla_tentacles.backend.lib.reroll_history.history import ReallocationHistory, RedeploymentHistory


@dataclass
class ReallocationRow:
    ts: int
    reallocation_state_status: str
    reallocation_taskgroup_id: str = "reallocation-taskgroup-id-foo"
    latest_snapshot_id: str = "latest-snapshot-id-foo"


@dataclass
class RedeploymentRow:
    ts: int
    current_state: str
    latest_snapshot_taskgroup_id: str = "latest-snapshot-taskgroup-id-foo"
    latest_snapshot_id: str = "latest-snapshot-id-foo"


def check_session(session, assertions):
    if assertions is None:
        assert session is None
        return
    assert session.in_progress_duration == assertions["in_progress_duration"]
    assert session.cooldown_duration == assertions["cooldown_duration"]


dataset = {

    "reallocation": {

        "rtc_sla_tentacles_testing_yp_lite": {

            "finished": [
                ReallocationRow(ts, reallocation_state_status)
                for ts, reallocation_state_status in zip(
                    range(0, 660, 60),
                    [
                        "IDLE",
                        "IN_PROGRESS",
                        "IN_PROGRESS",
                        "IDLE",
                        "IDLE",
                        "IDLE",
                        "IDLE",
                        "IDLE",
                        "IDLE",
                        "IDLE",
                        "IDLE",
                    ]
                )
            ],

            "in_progress": [
                ReallocationRow(ts, reallocation_state_status)
                for ts, reallocation_state_status in zip(
                    range(0, 660, 60),
                    [
                        "IDLE",
                        "IDLE",
                        "IDLE",
                        "IDLE",
                        "IDLE",
                        "IDLE",
                        "IDLE",
                        "IDLE",
                        "IN_PROGRESS",
                        "IN_PROGRESS",
                        "IN_PROGRESS"
                    ]
                )
            ],

            "partial": [
                ReallocationRow(120, "IN_PROGRESS"),
                ReallocationRow(360, "IDLE"),
                ReallocationRow(420, "IDLE"),
                ReallocationRow(480, "IDLE"),
                ReallocationRow(540, "IN_PROGRESS"),
            ]
        }
    },

    "redeployment": {

        "rtc_sla_tentacles_testing_gencfg": {

            "finished": [
                RedeploymentRow(ts, current_state)
                for ts, current_state in zip(
                    range(0, 660, 60),
                    [
                        "ONLINE",
                        "ONLINE",
                        "ONLINE",
                        "ONLINE",
                        "ONLINE",
                        "ONLINE",
                        "PREPARING",
                        "UPDATING",
                        "UPDATING",
                        "ONLINE",
                        "ONLINE"
                    ]
                )
            ],

            "in_progress": [
                RedeploymentRow(ts, current_state)
                for ts, current_state in zip(
                    range(0, 660, 60),
                    [
                        "ONLINE",
                        "ONLINE",
                        "ONLINE",
                        "ONLINE",
                        "PREPARING",
                        "PREPARING",
                        "UPDATING",
                        "UPDATING",
                        "UPDATING",
                        "UPDATING",
                        "UPDATING"
                    ]
                )
            ]
        },

        "rtc_sla_tentacles_testing_yp_lite": {

            "finished": [
                RedeploymentRow(ts, current_state)
                for ts, current_state in zip(
                    range(0, 660, 60),
                    [
                        "ONLINE",
                        "ONLINE",
                        "ONLINE",
                        "ONLINE",
                        "ONLINE",
                        "ONLINE",
                        "PREPARING",
                        "UPDATING",
                        "UPDATING",
                        "ONLINE",
                        "ONLINE"
                    ]
                )
            ],

            "in_progress": [
                RedeploymentRow(ts, current_state)
                for ts, current_state in zip(
                    range(0, 660, 60),
                    [
                        "ONLINE",
                        "ONLINE",
                        "ONLINE",
                        "ONLINE",
                        "PREPARING",
                        "PREPARING",
                        "UPDATING",
                        "UPDATING",
                        "UPDATING",
                        "UPDATING",
                        "UPDATING"
                    ]
                )
            ],

            "in_cooldown": [
                RedeploymentRow(ts, current_state)
                for ts, current_state in zip(
                    range(0, 660, 60),
                    [
                        "ONLINE",
                        "ONLINE",
                        "ONLINE",
                        "ONLINE",
                        "PREPARING",
                        "PREPARING",
                        "UPDATING",
                        "UPDATING",
                        "UPDATING",
                        "UPDATING",
                        "ONLINE"
                    ]
                )
            ],

            "several_sessions": [
                RedeploymentRow(ts, current_state)
                for ts, current_state in zip(
                    range(0, 1560, 60),
                    [
                        "PREPARING",
                        "UPDATING",
                        "UPDATING",
                        "ONLINE",
                        "ONLINE",
                        "PREPARING",
                        "UPDATING",
                        "ONLINE",
                        "ONLINE",
                        "ONLINE",
                        "PREPARING",
                        "UPDATING",
                        "UPDATING",
                        "ONLINE",
                        "ONLINE",
                        "PREPARING",
                        "UPDATING",
                        "UPDATING",
                        "ONLINE",
                        "ONLINE",
                        "PREPARING",
                        "UPDATING",
                        "UPDATING",
                        "ONLINE",
                        "ONLINE",
                        "PREPARING",
                    ]
                )
            ],
        }
    }
}


def get_reroll_clickhouse_client(monkeypatch: typing.Any, settings: dict) -> Database:
    def _mocked_clickhouse_client_init(_db_name):
        return None

    monkeypatch.setattr(Database, "__init__", _mocked_clickhouse_client_init)

    clickhouse_client = Database()

    def _mocked_clickhouse_select(_query):
        query_type = settings["query_type"]
        nanny_service_name = settings["nanny_service_name"]
        dataset_name = settings["dataset_name"]
        for row in dataset[query_type][nanny_service_name][dataset_name]:
            yield row

    monkeypatch.setattr(clickhouse_client, "select", _mocked_clickhouse_select)
    return clickhouse_client


@pytest.mark.parametrize(
    "settings,assertions",
    [
        (
            {
                "query_type": "reallocation",
                "nanny_service_name": "rtc_sla_tentacles_testing_yp_lite",
                "dataset_name": "finished"
            },
            {
                "if_session_in_progress": False,
                "current_period_duration": 600-360,
                "last_complete_session_start_ts": 60,
                "last_complete_session_end_ts": 360,
                "if_cooldown_in_progress": False,
                "current_session": None,
                "last_complete_session": {
                    "in_progress_duration": 120,
                    "cooldown_duration": 180,
                },
            }
        ),

        (
            {
                "query_type": "reallocation",
                "nanny_service_name": "rtc_sla_tentacles_testing_yp_lite",
                "dataset_name": "in_progress"
            },
            {
                "if_session_in_progress": True,
                "current_period_duration": 600-480,
                "last_complete_session_start_ts": None,
                "last_complete_session_end_ts": None,
                "if_cooldown_in_progress": False,
                "current_session": {
                    "in_progress_duration": 180,
                    "cooldown_duration": 0,
                },
                "last_complete_session": None,
            }
        ),

        (
            {
                "query_type": "reallocation",
                "nanny_service_name": "rtc_sla_tentacles_testing_yp_lite",
                "dataset_name": "partial"
            },
            {
                "if_session_in_progress": True,
                "current_period_duration": 600-540,
                "last_complete_session_start_ts": 0,
                "last_complete_session_end_ts": 540,
                "if_cooldown_in_progress": False,
                "current_session": {
                    "in_progress_duration": 60,
                    "cooldown_duration": 0,
                },
                "last_complete_session": {
                    "in_progress_duration": 120,
                    "cooldown_duration": 180,
                },
            }
        ),
    ]
)
def test_reallocation_history(monkeypatch: typing.Any, config_interface: ConfigInterface, settings: dict,
                              assertions: dict):
    clickhouse_client = get_reroll_clickhouse_client(monkeypatch, settings)
    reallocation_history = ReallocationHistory(nanny_service_name=settings["nanny_service_name"],
                                               config_interface=config_interface,
                                               clickhouse_client=clickhouse_client,
                                               start_ts=0,
                                               end_ts=settings.get("history_end_ts", 600))
    assert reallocation_history.if_session_in_progress() == assertions["if_session_in_progress"]
    assert reallocation_history.get_current_period_duration() == assertions["current_period_duration"]
    start_ts, end_ts = reallocation_history.get_last_complete_session_borders()
    assert start_ts == assertions["last_complete_session_start_ts"]
    assert end_ts == assertions["last_complete_session_end_ts"]
    assert reallocation_history.if_cooldown_in_progress() == assertions["if_cooldown_in_progress"]
    check_session(reallocation_history.get_current_session(), assertions["current_session"])
    check_session(reallocation_history.get_last_complete_session(), assertions["last_complete_session"])


@pytest.mark.parametrize(
    "settings,assertions",
    [
        (
            {
                "query_type": "redeployment",
                "nanny_service_name": "rtc_sla_tentacles_testing_gencfg",
                "dataset_name": "finished"
            },
            {
                "if_session_in_progress": False,
                "current_period_duration": 0,
                "last_complete_session_start_ts": 360,
                "last_complete_session_end_ts": 600,
                "if_cooldown_in_progress": False,
                "current_session": None,
                "last_complete_session": {
                    "in_progress_duration": 180,
                    "cooldown_duration": 60,
                },
            }
        ),

        (
            {
                "query_type": "redeployment",
                "nanny_service_name": "rtc_sla_tentacles_testing_gencfg",
                "dataset_name": "in_progress"
            },
            {
                "if_session_in_progress": True,
                "current_period_duration": 600-240,
                "last_complete_session_start_ts": None,
                "last_complete_session_end_ts": None,
                "if_cooldown_in_progress": False,
                "current_session": {
                    "in_progress_duration": 420,
                    "cooldown_duration": 0,
                },
                "last_complete_session": None,
            }
        ),

        (
            {
                "query_type": "redeployment",
                "nanny_service_name": "rtc_sla_tentacles_testing_yp_lite",
                "dataset_name": "finished"
            },
            {
                "if_session_in_progress": False,
                "current_period_duration": 0,
                "last_complete_session_start_ts": 360,
                "last_complete_session_end_ts": 600,
                "if_cooldown_in_progress": False,
                "current_session": None,
                "last_complete_session": {
                    "in_progress_duration": 180,
                    "cooldown_duration": 60,
                },
            }
        ),

        (
            {
                "query_type": "redeployment",
                "nanny_service_name": "rtc_sla_tentacles_testing_yp_lite",
                "dataset_name": "in_progress"
            },
            {
                "if_session_in_progress": True,
                "current_period_duration": 600-240,
                "last_complete_session_start_ts": None,
                "last_complete_session_end_ts": None,
                "if_cooldown_in_progress": False,
                "current_session": {
                    "in_progress_duration": 420,
                    "cooldown_duration": 0,
                },
                "last_complete_session": None,
            }
        ),

        (
            {
                "query_type": "redeployment",
                "nanny_service_name": "rtc_sla_tentacles_testing_yp_lite",
                "dataset_name": "in_cooldown"
            },
            {
                "if_session_in_progress": True,
                "current_period_duration": 600-240,
                "last_complete_session_start_ts": None,
                "last_complete_session_end_ts": None,
                "if_cooldown_in_progress": True,
                "current_session": {
                    "in_progress_duration": 360,
                    "cooldown_duration": 60,
                },
                "last_complete_session": None,
            }
        ),

        (
            {
                "query_type": "redeployment",
                "nanny_service_name": "rtc_sla_tentacles_testing_yp_lite",
                "dataset_name": "several_sessions",
                "history_end_ts": 1500,
            },
            {
                "if_session_in_progress": True,
                "current_period_duration": 0,
                "last_complete_session_start_ts": 1500-60*5,
                "last_complete_session_end_ts": 1500-60,
                "if_cooldown_in_progress": False,
                "current_session": {
                    "in_progress_duration": 60,
                    "cooldown_duration": 0,
                },
                "last_complete_session": {
                    "in_progress_duration": 180,
                    "cooldown_duration": 60,
                },
                "num_complete_sessions": 5,
            },
        ),
    ]
)
def test_redeployment_history(monkeypatch: typing.Any, config_interface: ConfigInterface, settings: dict,
                              assertions: dict):
    clickhouse_client = get_reroll_clickhouse_client(monkeypatch, settings)
    redeployment_history = RedeploymentHistory(nanny_service_name=settings["nanny_service_name"],
                                               config_interface=config_interface,
                                               clickhouse_client=clickhouse_client,
                                               start_ts=0,
                                               end_ts=settings.get("history_end_ts", 600))
    assert redeployment_history.if_session_in_progress() == assertions["if_session_in_progress"]
    assert redeployment_history.get_current_period_duration() == assertions["current_period_duration"]
    start_ts, end_ts = redeployment_history.get_last_complete_session_borders()
    assert start_ts == assertions["last_complete_session_start_ts"]
    assert end_ts == assertions["last_complete_session_end_ts"]
    assert redeployment_history.if_cooldown_in_progress() == assertions["if_cooldown_in_progress"]
    check_session(redeployment_history.get_current_session(), assertions["current_session"])
    check_session(redeployment_history.get_last_complete_session(), assertions["last_complete_session"])
    if num_complete_sessions := assertions.get("num_complete_sessions"):
        assert len(redeployment_history.get_complete_sessions(limit=999)) == num_complete_sessions
