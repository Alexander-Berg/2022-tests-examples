from checks import walle_meta, common

import pytest
from juggler.bundles import Status, CheckResult
from .tools import time, mocked_timestamp

from utils import make_canonization

WALLE_META = "walle_meta"
MOCK_BUNDLE_VERSION = 3919006


def expected_result(expected_status, expected_description):
    return CheckResult([walle_meta.make_event(expected_status, expected_description)]).to_dict(service=WALLE_META)


@pytest.fixture(autouse=True)
def mock_timestamp(monkeypatch):
    monkeypatch.setattr(walle_meta, "timestamp", mocked_timestamp)


@pytest.fixture()
def mock_bundle_version(monkeypatch):
    monkeypatch.setattr(walle_meta, "get_bundle_version", lambda: MOCK_BUNDLE_VERSION)


@pytest.fixture()
def mock_hw_watcher_version(monkeypatch):
    monkeypatch.setattr(walle_meta, "path_exists", lambda p: True)
    monkeypatch.setattr(walle_meta, "get_command_output", lambda a: "hw_watcher 0.6.0.1")


@pytest.fixture()
def mock_missing_hw_watcher(monkeypatch):
    monkeypatch.setattr(walle_meta, "path_exists", lambda p: False)


@pytest.fixture()
def mock_broken_hw_watcher(monkeypatch):
    monkeypatch.setattr(walle_meta, "path_exists", lambda p: True)

    def raiser(*args, **kwargs):
        raise OSError("Much mysterious exception")
    monkeypatch.setattr(walle_meta, "get_command_output", raiser)


@pytest.mark.usefixtures("mock_missing_hw_watcher", "mock_bundle_version")
def test_detects_missing_hw_watcher(manifest):
    check_result = manifest.execute(WALLE_META)

    expected_description = {
        "hw_watcher": "File \"/usr/sbin/hw_watcher\" does not exist",
        "timestamp": mocked_timestamp(),
        "bundle": MOCK_BUNDLE_VERSION
    }

    return make_canonization(check_result, expected_result(Status.WARN, expected_description))


@pytest.mark.usefixtures("mock_hw_watcher_version", "mock_bundle_version")
def test_parses_hw_watcher_version(manifest):
    check_result = manifest.execute(WALLE_META)

    expected_description = {"hw_watcher": "0.6.0.1", "timestamp": mocked_timestamp(), "bundle": MOCK_BUNDLE_VERSION}

    return make_canonization(check_result, expected_result(Status.OK, expected_description))


@pytest.mark.usefixtures("mock_broken_hw_watcher", "mock_bundle_version")
def test_detects_broken_hw_watcher(manifest):
    check_result = manifest.execute(WALLE_META)

    expected_description = {
        "hw_watcher": "Much mysterious exception",
        "timestamp": mocked_timestamp(),
        "bundle": MOCK_BUNDLE_VERSION
    }

    return make_canonization(check_result, expected_result(Status.CRIT, expected_description))
