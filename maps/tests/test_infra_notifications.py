import datetime
import re

import pytest
import pytz
import requests

from maps.garden.libs_server.application import state
from maps.garden.libs_server.build.build_defs import Build
from maps.garden.libs_server.build.build_defs import BuildStatus
from maps.garden.libs_server.build.build_defs import BuildStatusString

from maps.garden.scheduler.lib.notification.infra import InfraNotifier
from maps.garden.scheduler.lib.notification.utils import EventType


TEST_CONFIG = {"token": "secret", "namespace_id": 133}
CONTOUR_NAME = "development"


class _BuildsMock:
    def update(self, build, fields):
        assert fields == ["infra_event_id"]


@pytest.fixture
def mock_predefined(monkeypatch, requests_mock, request):
    env_name, build_name = getattr(request, "param", (CONTOUR_NAME, "test"))

    monkeypatch.setattr(state, "build_manager", _BuildsMock)

    api_url = InfraNotifier.INFRA_API_URL
    namespace_id = TEST_CONFIG["namespace_id"]
    requests_mock.get(
        f"{api_url}/namespace/{namespace_id}/services",
        json=[{"name": build_name, "id": 1}],
    )
    requests_mock.get(
        f"{api_url}/environments?namespaceId={namespace_id}",
        json=[{"service_id": 1, "id": 1, "name": env_name}],
    )
    return requests_mock


@pytest.mark.parametrize(
    "config",
    [{}, {"token": "secret"}, {"namespace_id": 133}],
)
def test_invalid_config(config):
    with pytest.raises(KeyError):
        InfraNotifier(config)


def test_invalid_namespace_id(requests_mock):
    requests_mock.get(
        "{}/namespace/{}/services".format(
            InfraNotifier.INFRA_API_URL,
            TEST_CONFIG["namespace_id"],
        ),
        status_code=404,
    )
    with pytest.raises(requests.exceptions.HTTPError):
        InfraNotifier(TEST_CONFIG)


@pytest.mark.parametrize(
    ("mock_predefined"),
    [
        ("invalid", "valid"),
        (CONTOUR_NAME, "invalid"),
    ],
    indirect=["mock_predefined"],
)
def test_post_non_existing(mock_predefined):
    build = Build(
        id=1,
        name="valid",
        contour_name=CONTOUR_NAME,
        created_at=datetime.datetime(2020, 5, 18, 12, 0, 0),
        status=BuildStatus(
            string=BuildStatusString.IN_PROGRESS,
            finish_time=datetime.datetime(2020, 5, 18, 12, 30, 0),
        ),
    )
    post = mock_predefined.post(f"{InfraNotifier.INFRA_API_URL}/events")
    any_event_url = re.compile(f"{InfraNotifier.INFRA_API_URL}/events/.*")
    put = mock_predefined.put(any_event_url)

    notifier = InfraNotifier(TEST_CONFIG)
    notifier.notify(build, EventType.STARTED)
    build.status.string = BuildStatusString.COMPLETED
    notifier.notify(build, EventType.COMPLETED)
    assert not post.called
    assert not put.called


@pytest.mark.parametrize(
    ("start_status", "finish_status", "finish_event_type", "extras"),
    [
        (
            BuildStatusString.WAITING,
            BuildStatusString.FAILED,
            EventType.FAILED,
            {},
        ),
        (
            BuildStatusString.SCHEDULING,
            BuildStatusString.COMPLETED,
            EventType.COMPLETED,
            {
                "release_name": "21.21.21 21",
            },
        ),
        (
            BuildStatusString.IN_PROGRESS,
            BuildStatusString.IN_PROGRESS,
            EventType.FAILED,
            {
                "release_name": "21.21.21 21",
                "deploy_step": "prestable",
            },
        ),
    ],
)
def test_call_ok(mock_predefined, start_status, finish_status, finish_event_type, extras):
    notifier = InfraNotifier(TEST_CONFIG)

    creation_time = datetime.datetime(2020, 5, 18, 12, 0, 0, tzinfo=pytz.UTC)
    real_finish_time = datetime.datetime(2020, 5, 18, 12, 30, 0, tzinfo=pytz.UTC)

    build = Build(
        id=1,
        name="test",
        contour_name=CONTOUR_NAME,
        created_at=creation_time,
        status=BuildStatus(string=start_status),
        extras=extras,
    )
    event_id = 13232

    req = mock_predefined.post(
        f"{InfraNotifier.INFRA_API_URL}/events",
        json={"id": event_id},
    )
    notifier.notify(build, EventType.STARTED)
    assert req.called_once
    result = {"start": req.last_request.json()}

    build.status.finish_time = real_finish_time
    build.status.string = finish_status

    req = mock_predefined.put(
        f"{InfraNotifier.INFRA_API_URL}/events/{event_id}",
    )
    notifier.notify(build, finish_event_type)
    assert req.called_once
    result["finish"] = req.last_request.json()
    return result
