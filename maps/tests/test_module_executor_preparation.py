import datetime as dt
from unittest import mock

import pytest

import yt.wrapper as yt

from maps.garden.libs_server.yt_task_handler.pymod import utils


@pytest.fixture
def yt_client_mock():
    yt_client_mock = mock.Mock()
    with mock.patch("yt.wrapper.YtClient", return_value=yt_client_mock):
        transaction = mock.Mock()
        transaction.__enter__ = mock.Mock(return_value=None)
        transaction.__exit__ = mock.Mock(return_value=None)
        yt_client_mock.Transaction.return_value = transaction
        yield yt_client_mock


@pytest.fixture
def module_executor_path():
    module_executor_path = "module_executor"
    with open(module_executor_path, "wb") as f:
        f.write(b"some data")
    return module_executor_path


def test_module_executor_upload(yt_stuff, mocker, module_executor_path):
    yt_client = yt_stuff.get_yt_client()
    mocker.patch.object(utils, "MODULE_EXECUTOR_LOCAL_PATH", module_executor_path)
    yt_bin_dir = "//home/garden/test_module_executor_upload"
    yt_file = f"{yt_bin_dir}/tool"
    utils.upload_module_executor(yt_client, yt_file)
    assert yt_client.list(yt_bin_dir) == ["tool"]
    assert yt_client.read_file(yt_file).read() == b"some data"


def _make_yson(filename, modification_time):
    result = yt.yson.YsonUnicode(filename)
    result.attributes = {"modification_time": modification_time}
    return result


def test_old_binary_cleanup(freezer, mocker, yt_client_mock, module_executor_path):
    mocker.patch.object(utils, "MODULE_EXECUTOR_LOCAL_PATH", module_executor_path)
    mocker.patch.object(utils, "MAX_REMOTE_MODULE_EXECUTOR_FILES", 3)
    mocker.patch.object(utils, "MIN_MODULE_EXECUTOR_REMOTE_EXISTENCE_PERIOD", dt.timedelta(days=7))

    start_time = dt.datetime(2021, 1, 10, 11, 0, 0)
    yt_client_mock.exists.return_value = False
    old_files = [
        _make_yson(str(attempt), yt.common.datetime_to_string(start_time + dt.timedelta(days=attempt)))
        for attempt in range(0, 5)
    ]

    freezer.move_to(start_time + dt.timedelta(days=9))

    yt_client_mock.search.return_value = (
        old_files + [
            _make_yson("new", yt.common.datetime_to_string(start_time + dt.timedelta(days=9)))
        ]
    )

    utils.upload_module_executor(yt_client_mock, "//tmp/garden/new")

    assert yt_client_mock.remove.mock_calls == [
        mock.call("//tmp/garden/1", force=True),
        mock.call("//tmp/garden/0", force=True)
    ]
