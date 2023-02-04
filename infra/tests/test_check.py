from unittest.mock import patch, MagicMock


FAKE_NOW = 0


class MockRun():
    def __init__(self, returncode, stdout):
        self.returncode = returncode
        self.stdout = stdout


@patch('time.time', MagicMock(return_value=FAKE_NOW))
@patch("infra.rtc.juggler.bundle.checks.bind.run", MagicMock(return_value=MockRun(127, b"test")))
def test_cmd_error(manifest):
    data = manifest.execute('bind')
    expected = {
        'events': [
            {
                'description': '{"status": "CRIT", "timestamp": 0, "reason": "test"}',
                'service': 'bind',
                'status': 'CRIT',
            }
        ]
    }
    assert expected == data


@patch('time.time', MagicMock(return_value=FAKE_NOW))
@patch("infra.rtc.juggler.bundle.checks.bind.run", MagicMock(return_value=MockRun(0, b"test")))
def test_cmd_ok(manifest):
    data = manifest.execute('bind')
    expected = {
        'events': [
            {
                'description': '{"status": "OK", "timestamp": 0, "reason": ""}',
                'service': 'bind',
                'status': 'OK',
            }
        ]
    }
    assert expected == data
