import pytest
import requests_mock
from urllib.parse import urljoin
from infra.rtc_sla_tentacles.backend.lib.harvesters.nanny_state_dumper import NannyStateDumper
from infra.rtc_sla_tentacles.backend.lib.tentacle_agent import const as agent_const


@pytest.fixture
def nanny_state_dumper(config_interface, harvesters_snapshot_manager):
    type_config = config_interface.get_harvester_config("nanny_state_dumper")
    params = type_config["common_parameters"]
    nanny_service_name = "rtc_sla_tentacles_testing_sas"

    state_response = {
        "content": {
            "active_snapshots": [
                {
                    "state": "ACTIVE",
                    "conf_id": "rtc_sla_tentacles_testing_sas-1566460064577",
                    "snapshot_id": "032b8ff1d993f517e048c71b3f6bfc7bfb6a2b1e",
                    "taskgroup_id": "search-42",
                    "entered": 1566460217575,
                },
                {
                    "state": "PREPARED",
                    "conf_id": "rtc_sla_tentacles_testing_sas-1566458198683",
                    "snapshot_id": "98e1241ac090fb28964a99befb05c21dbe2cc877",
                    "taskgroup_id": "search-0077352720",
                    "entered": 1566460233715,
                }
            ],
            "summary": {
                "entered": 1566460245861,
                "value": "ONLINE"
            }
        },
        "reallocation": {
            "taskgroup_id": "search-123",
            "state": {
                "status": "In progress",
                "reason": "",
                "entered": 1566460245871,
                "message": ""
            },
            "id": "id123"
        }
    }

    revision_id_response = {"active_revision_id": "rtc_sla_tentacles_testing_sas-1566460064577"}

    snapshot_resources_response = {
        "content": {
            "resources": {
                "url_files": [
                    {
                        "is_dynamic": "false",
                        "url": "rbtorrent:9e3d347c013a3a8b71b132526786c9a41558c582",
                        "local_path": agent_const.AGENT_RESOURCE_NAME,
                        "extract_path": ""
                    }
                ]
            }
        }
    }

    with requests_mock.Mocker() as api_calls:

        api_calls.get(urljoin(params["nanny_api"],
                              params["nanny_current_state_url_template"].format(nanny_service_name)),
                      json=state_response)
        api_calls.get(urljoin(params["nanny_api"],
                              params["nanny_active_revision_id_url_template"].format(nanny_service_name)),
                      json=revision_id_response)
        api_calls.get(urljoin(params["nanny_api"],
                              params["nanny_snapshot_resources_url_template"].format(nanny_service_name)),
                      json=snapshot_resources_response)

        harvester = next(NannyStateDumper.build_instances(config_interface, harvesters_snapshot_manager, type_config))
        yield harvester


def test_nanny_extract(nanny_state_dumper):
    expected_mongodb_result = {
        "current_active": {
            "snapshot_id": "032b8ff1d993f517e048c71b3f6bfc7bfb6a2b1e",
            "timestamp_rbtorrent_id": "rbtorrent:9e3d347c013a3a8b71b132526786c9a41558c582",
        }
    }
    expected_clickhouse_result = {
        "ts": 42,
        "nanny_service_name": "rtc_sla_tentacles_testing_sas",
        "deploy_gencfg": 1,
        "deploy_yp_lite": 0,
        "current_state": "ONLINE",
        "reallocation_id": "id123",
        "reallocation_state_status": "IN_PROGRESS",
        "reallocation_taskgroup_id": "search-123",
        "latest_snapshot_id": "032b8ff1d993f517e048c71b3f6bfc7bfb6a2b1e",
        "latest_snapshot_taskgroup_id": "search-42",
    }
    expected_data = {
        "mongodb_result": expected_mongodb_result,
        "clickhouse_result": expected_clickhouse_result
    }
    assert expected_data == nanny_state_dumper.extract(42)
