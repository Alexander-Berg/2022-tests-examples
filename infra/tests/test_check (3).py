import unittest.mock as mock


def test_pidfile_read_error(manifest):
    with mock.patch("builtins.open", mock.mock_open(), create=True) as m:
        m.side_effect = IOError('IO err happened')
        data = manifest.execute('cron')

    expected = \
        {'events': [{'description': 'Failed to read pid file: IO err happened',
                     'service': 'cron',
                     'status': 'CRIT',
                     'tags': ['HOSTMAN-516']}]}
    assert expected == data


def test_pidfile_contain_garbage(manifest):
    with mock.patch('infra.rtc.juggler.bundle.checks.cron.get_file_content', return_value='garbage'):
        data = manifest.execute('cron')

    expected = \
        {'events': [{'description': "Failed to read pid file: invalid literal for int() with base 10: 'garbage'",
                     'service': 'cron',
                     'status': 'CRIT',
                     'tags': ['HOSTMAN-516']}]}
    assert expected == data


def test_process_absent(manifest):
    with mock.patch('psutil.Process.as_dict', return_value={'status': '?'}):
        data = manifest.execute('cron')

    expected = \
        {'events': [{'description': 'Process is absent',
                     'service': 'cron',
                     'status': 'CRIT',
                     'tags': ['HOSTMAN-516']}]}
    assert expected == data


def test_process_running(manifest):
    with mock.patch('psutil.Process.as_dict', return_value={'name': 'cron', 'status': 'running'}):
        data = manifest.execute('cron')

    expected = {'events': [{'description': 'Ok', 'service': 'cron', 'status': 'OK', 'tags': ['HOSTMAN-516']}]}
    assert expected == data


def test_process_sleeping(manifest):
    with mock.patch('psutil.Process.as_dict', return_value={'name': 'cron', 'status': 'sleeping'}):
        data = manifest.execute('cron')

    expected = {'events': [{'description': 'Ok', 'service': 'cron', 'status': 'OK', 'tags': ['HOSTMAN-516']}]}
    assert expected == data


def test_incorrect_status(manifest):
    with mock.patch('psutil.Process.as_dict', return_value={'name': 'cron', 'status': 'stopped'}):
        data = manifest.execute('cron')

    expected = \
        {'events': [{'description': 'Bad process status: stopped',
                     'service': 'cron',
                     'status': 'CRIT',
                     'tags': ['HOSTMAN-516']}]}
    assert expected == data


def test_process_mismatch(manifest):
    with mock.patch('psutil.Process.as_dict', return_value={'name': 'grep', 'status': 'sleeping'}):
        data = manifest.execute('cron')

    expected = \
        {'events': [{'description': 'Pidfile contain pid for another process',
                     'service': 'cron',
                     'status': 'CRIT',
                     'tags': ['HOSTMAN-516']}]}
    assert expected == data
