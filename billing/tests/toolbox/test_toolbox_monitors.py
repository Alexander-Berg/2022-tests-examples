from bcl.toolbox.notifiers import NotifierBase
from bcl.toolbox.monitors import MonitorBase


class DummyNotifier(NotifierBase):

    def send(self, data=None):
        return data


class DummyMonitor(MonitorBase):

    notifier_cls = DummyNotifier

    def check(self):
        return 'checked'

    def compose(self, check_result):
        return 'composed'


def test_monitor(mocker, monkeypatch):

    monitor = DummyMonitor({})

    assert monitor.run()[0] == 'composed'

    mock_check = mocker.MagicMock()
    mock_check.return_value = None

    mock_compose = mocker.MagicMock()
    mock_compose.return_value = None

    monkeypatch.setattr(monitor, 'check', mock_check)
    monkeypatch.setattr(monitor, 'compose', mock_compose)

    assert monitor.run() is None
    assert mock_check.call_count == 1
    assert mock_compose.call_count == 0

    mock_check.return_value = True

    assert monitor.run() is None
    assert mock_check.call_count == 2
    assert mock_compose.call_count == 1
