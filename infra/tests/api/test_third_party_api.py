"""Test API to third-party data."""

import http.client
import json

import pytest

from infra.walle.server.tests.lib.util import (
    TestCase,
    monkeypatch_request,
    mock_response,
    monkeypatch_config,
    load_mock_data,
)
from walle.clients import bot


@pytest.fixture
def test(request, disable_caches):
    return TestCase.create(request)


_RACKTABLES_VM_PROJECTS = """AUGURNETS	591
TINYURLNETS	592
SEARCHPRODNETS	595
SEARCHPRODNETS	604"""

_WALLE_VM_PROJECTS = [
    {"id": "591", "name": "AUGURNETS"},
    {"id": "592", "name": "TINYURLNETS"},
    {"id": "595", "name": "SEARCHPRODNETS"},
    {"id": "604", "name": "SEARCHPRODNETS"},
]


@pytest.fixture
def mock_vm_projects(mp):
    resp = mock_response(_RACKTABLES_VM_PROJECTS, as_json=False)
    monkeypatch_request(mp, resp)


@pytest.fixture
def bot_projects_raw_data():
    # Return stored mock data. Actual data can be found at
    # https://bot.yandex-team.ru/api/view.php?name=view_oebs_services&format=json
    return load_mock_data("mocks/bot-oebs-projects.json")


_BOT_PROJECTS_TREE = [
    {
        "project_id": "-1",
        "ru_description": "Nenaznachen",
        "en_description": "Unassigned",
        "parent_project_id": None,
        "planner_id": "0",
        "subprojects": [],
    },
    {
        "project_id": "100000022",
        "ru_description": "Brauzer",
        "en_description": "Browser",
        "parent_project_id": None,
        "planner_id": "981",
        "subprojects": [],
    },
    {
        "project_id": "100000040",
        "ru_description": "Monetizatsia",
        "en_description": "Monetization",
        "parent_project_id": None,
        "planner_id": "867",
        "subprojects": [],
    },
    {
        "project_id": "100000551",
        "ru_description": "Poisk",
        "en_description": "Search",
        "parent_project_id": None,
        "planner_id": "851",
        "subprojects": [
            {
                "project_id": "100000041",
                "ru_description": "Poisk web",
                "en_description": "Search",
                "parent_project_id": "100000551",
                "planner_id": "852",
                "subprojects": [
                    {
                        "project_id": "100000042",
                        "ru_description": "OLD_Infrastrukturnye servisy",
                        "en_description": "OLD_Infrastructure services",
                        "parent_project_id": "100000041",
                        "planner_id": "0",
                        "subprojects": [],
                    }
                ],
            }
        ],
    },
]


def test_get_bot_projects(mp, bot_projects_raw_data, test):
    mp.function(bot.json_request, return_value=json.loads(bot_projects_raw_data))
    response = test.api_client.get("/v1/bot-projects")

    assert response.status_code == http.client.OK
    assert response.json["result"] == _BOT_PROJECTS_TREE


def test_get_hbf_projects(test, mp, mock_vm_projects):
    monkeypatch_config(mp, "racktables.access_token", "pass")
    response = test.api_client.get("/v1/hbf-projects")

    assert sorted(response.json["result"], key=lambda x: x["id"]) == _WALLE_VM_PROJECTS
