import errno
import mock
import pytest

from sepelib.subprocess import util


def test_subprocess_terminate():
    p = mock.Mock()
    term_mock = mock.Mock(return_value=None)
    p.terminate = term_mock
    util.terminate(p)
    def raise_esrch():
        raise EnvironmentError(errno.ESRCH, 'test')
    p.terminate = mock.Mock(side_effect=raise_esrch)


def test_subprocess_terminate_with_raise():
    def raise_no_code():
        raise EnvironmentError()
    p = mock.Mock()
    p.terminate = mock.Mock(side_effect=raise_no_code)
    with pytest.raises(EnvironmentError):
        util.terminate(p)
