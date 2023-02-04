import collections
import mock

from infra.rtc.nodeinfo.lib import statefile


def test_touch_ok():
    m = mock.Mock()
    o = mock.mock_open()
    rv = statefile.touch("mock-file", m, o)
    assert rv is None
    o.assert_called_once_with("mock-file", 'w')
    m.assert_called_once_with("mock-file", None)


def test_touch_error():
    # utime error
    m = mock.Mock(side_effect=OSError('permission denied'))
    o = mock.mock_open()
    rv = statefile.touch("mock-file", m, o)
    assert rv == 'failed to touch mock-file: permission denied'
    o.assert_called_once_with("mock-file", 'w')
    m.assert_called_once_with("mock-file", None)
    # open error
    m = mock.Mock()
    o = mock.Mock(side_effect=OSError('open failed'))
    rv = statefile.touch("mock-file", m, o)
    assert rv == 'failed to touch mock-file: open failed'
    o.assert_called_once_with("mock-file", 'w')
    m.assert_not_called()


def test_state_error_run_ok():
    assert statefile.state_error(None) is None


def test_state_ok_run_fail():
    StatResult = collections.namedtuple("StatResult", "st_mtime")
    # file updated recently
    stat = mock.Mock(return_value=StatResult(1000))
    now = mock.Mock(return_value=1100)
    assert statefile.state_error('mock-run-error', "mock-file", stat, now) is None
    stat.assert_called_once_with("mock-file")
    now.assert_called()
    # file not updated too long
    stat.reset_mock()
    now = mock.Mock(return_value=4000)
    assert statefile.state_error('mock-run-error', "mock-file", stat, now) == 'failed to run nodeinfo: mock-run-error and state file mock-file was updated 3000 seconds ago'
    stat.assert_called_once_with("mock-file")
    now.assert_called()
    # stat exception
    stat = mock.Mock(side_effect=OSError('permission denied'))
    assert statefile.state_error('mock-run-error', "mock-file", stat, now) == 'failed to run nodeinfo: mock-run-error and failed to check state file mock-file mtime: permission denied'
    stat.assert_called_once_with("mock-file")


def test_retcode_from_run_error():
    # run without error
    m = mock.Mock(return_value=None)
    t = mock.Mock(return_value=None)
    assert statefile.retcode_from_run_error(None, m, t) == 0
    m.assert_called_once_with(None)
    t.assert_called_once()
    # run error, state updated recently
    m.reset_mock()
    t.reset_mock()
    assert statefile.retcode_from_run_error('mock-run-err', m, t) == 0
    m.assert_called_once_with('mock-run-err')
    t.assert_not_called()
    # run error, state not updated too long
    m.reset_mock(return_value='mock-state-err')
    assert statefile.retcode_from_run_error('mock-run-err', m, t) == 1
    m.assert_called_once_with('mock-run-err')
    t.assert_not_called()
    # run without error, failed to touch state
    m = mock.Mock(return_value=None)
    t.reset_mock(return_value='mock-touch-err')
    assert statefile.retcode_from_run_error(None, m, t) == 1
    m.assert_called_once_with(None)
    t.assert_called_once()
