import unittest
from mock import patch
from maps.infra.baseimage.juggler_check_wrapper.lib import juggler_check_wrapper as jcw


class Tests(unittest.TestCase):
    def test_parse_dict(self):
        assert jcw.parse_dict(['foo=bar']) == {'foo': 'bar'}

    def test_unparsed(self):
        for prefix in ['', 'OK ;', 'EMERG;', 'CRIT:']:
            assert jcw.wrap_check(['sh', '-c', f'echo -n "{prefix}echoed $FOO"'], ['FOO=BAR']) == \
                   jcw.Check(jcw.Status.WARN, f'Cannot parse wrapped check message: {prefix}echoed BAR')

    def test_wrap(self):
        for check_status in jcw.Status:
            assert jcw.wrap_check(['sh', '-c', f'echo -n "{check_status.name};echoed $FOO"'], ['FOO=BAR']) == \
                   jcw.Check(check_status, 'echoed BAR')

    @patch('syslog.syslog')
    def test_stderr(self, syslog_mock):
        assert jcw.wrap_check(['sh', '-c', 'echo some_stderr >&2; echo -n "OK;echoed $FOO"'], ['FOO=BAR']) == \
               jcw.Check(jcw.Status.OK, 'echoed BAR')
        syslog_mock.assert_called_once_with(
            6,
            "Juggler check (sh -c echo some_stderr >&2; echo -n \"OK;echoed $FOO\") STDERR: some_stderr")

    @patch("maps.infra.baseimage.juggler_check_wrapper.lib.juggler_check_wrapper.is_host_detached")
    def test_detached(self, mock_container_detached):
        mock_container_detached.side_effect = lambda: True

        assert jcw.wrap_check(['sh', '-c', 'echo -n "CRIT;echoed $FOO"'], ['FOO=BAR']) == \
               jcw.Check(jcw.Status.WARN, 'Pod detached by ITS, suppressing (CRIT echoed BAR)')

    @patch("maps.infra.baseimage.juggler_check_wrapper.lib.juggler_check_wrapper.container_age_seconds")
    def test_starting(self, mock_container_age_seconds):
        mock_container_age_seconds.side_effect = lambda: 170

        assert jcw.wrap_check(['sh', '-c', 'echo -n "CRIT;echoed $FOO"'], ['FOO=BAR']) == \
               jcw.Check(jcw.Status.WARN, 'Pod starting, suppressing (CRIT echoed BAR)')
