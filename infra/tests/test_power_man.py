import mock

from infra.ya_salt.lib import subprocutil
from infra.ya_salt.lib import power_man


def test_reboot():
    m = mock.Mock()
    m.return_value = ('', '', subprocutil.Status(ok=False, message='Failed'))
    p = power_man.PowerManager(check_output=m)
    err = p.reboot()
    assert err is not None
    assert m.call_count == 1
    # Check timed out
    m.reset_mock()
    m.return_value = ('', '', subprocutil.Status(ok=True, message=''))
    p = power_man.PowerManager(check_output=m)
    err = p.reboot(timeout=0.01)
    assert err.startswith('failed to reboot in')
    assert m.call_count == 1
