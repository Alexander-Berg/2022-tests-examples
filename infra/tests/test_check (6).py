from unittest.mock import patch, MagicMock


FAKE_NOW = 1024


class MockRun():
    def __init__(self, returncode, out, err):
        self.returncode = returncode
        self.stdout = out
        self.stderr = err


@patch("time.time", MagicMock(return_value=FAKE_NOW))
@patch("infra.rtc.juggler.bundle.checks.ll_duplicate_addr.interfaces", MagicMock(return_value=[]))
def test_empty_ifaces_on_host(manifest):
    data = manifest.execute('ll_duplicate_addr')
    expected = {
        "events": [
            {
                "description": '{"status": "OK", "timestamp": 1024, "reason": "Ok"}',
                "service": "ll_duplicate_addr",
                "status": "OK",
            }
        ]
    }
    assert expected == data


@patch("time.time", MagicMock(return_value=FAKE_NOW))
@patch("infra.rtc.juggler.bundle.checks.ll_duplicate_addr.run", MagicMock(return_value=MockRun(1, b"", b"/usr/bin/ndisc6 not found")))
@patch("infra.rtc.juggler.bundle.checks.ll_duplicate_addr.ifaddresses", MagicMock(return_value={10: [{'addr': 'fe80::a:1'}]}))
@patch("infra.rtc.juggler.bundle.checks.ll_duplicate_addr.interfaces", MagicMock(return_value=['vlan688', 'vlan788']))
def test_inavalid_command(manifest):
    data = manifest.execute('ll_duplicate_addr')
    expected = {
        "events": [
            {
                "description": '{"status": "WARN", "timestamp": 1024, "reason": "/usr/bin/ndisc6 not found"}',
                "service": "ll_duplicate_addr",
                "status": "WARN",
            }
        ]
    }
    assert expected == data


@patch("time.time", MagicMock(return_value=FAKE_NOW))
@patch("infra.rtc.juggler.bundle.checks.ll_duplicate_addr.run", MagicMock(return_value=MockRun(0, b"address:52:54:00:12:34:56", b"")))
@patch("infra.rtc.juggler.bundle.checks.ll_duplicate_addr.ifaddresses", MagicMock(return_value={10: [{'addr': 'fe80::a:1'}]}))
@patch("infra.rtc.juggler.bundle.checks.ll_duplicate_addr.interfaces", MagicMock(return_value=['vlan', 'vlan688']))
def test_duplicate_found_vlan688(manifest):
    data = manifest.execute('ll_duplicate_addr')
    expected = {
        "events": [
            {
                "description": '{"status": "CRIT", "timestamp": 1024, "reason": "Found duplicate address fe80::a:1 on vlan688"}',
                "service": "ll_duplicate_addr",
                "status": "CRIT",
            }
        ]
    }
    assert expected == data


@patch("time.time", MagicMock(return_value=FAKE_NOW))
@patch("infra.rtc.juggler.bundle.checks.ll_duplicate_addr.run", MagicMock(return_value=MockRun(0, b"address:52:54:00:12:34:56", b"")))
@patch("infra.rtc.juggler.bundle.checks.ll_duplicate_addr.ifaddresses", MagicMock(return_value={10: [{'addr': 'fe80::a:1'}]}))
@patch("infra.rtc.juggler.bundle.checks.ll_duplicate_addr.interfaces", MagicMock(return_value=[' ', 'vlan788']))
def test_duplicate_found_vlan788(manifest):
    data = manifest.execute('ll_duplicate_addr')
    expected = {
        "events": [
            {
                "description": '{"status": "CRIT", "timestamp": 1024, "reason": "Found duplicate address fe80::a:1 on vlan788"}',
                "service": "ll_duplicate_addr",
                "status": "CRIT",
            }
        ]
    }
    assert expected == data
