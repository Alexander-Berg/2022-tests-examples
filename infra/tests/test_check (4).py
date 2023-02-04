import time
import unittest.mock as mock


MAILS_FILE = '/var/log/fs_mail/fs_mail.dump'
IGNORE_OLDER_THAN_SEC = 60 * 60 * 24 * 4  # ignore old messages


def test_file_absent(manifest):
    with mock.patch('os.stat', side_effect=FileNotFoundError):
        data = manifest.execute('cron_errs')

    expected = {'events': [{'description': 'Ok', 'service': 'cron_errs', 'status': 'OK'}]}
    assert expected == data


def test_file_other_exceptions(manifest):
    with mock.patch('os.stat', side_effect=IOError('shit happens')):
        data = manifest.execute('cron_errs')

    expected = {'events': [{'description': 'shit happens', 'service': 'cron_errs', 'status': 'CRIT'}]}
    assert expected == data


def test_file_empty(manifest, os_stat_proxy):
    stat_proxy = os_stat_proxy()
    stat_proxy.set(MAILS_FILE, size=0)

    with mock.patch('os.stat', side_effect=stat_proxy.proxy):
        data = manifest.execute('cron_errs')

    expected = {'events': [{'description': 'Ok', 'service': 'cron_errs', 'status': 'OK'}]}
    assert expected == data


def test_errors_fresh(manifest, os_stat_proxy):
    stat_proxy = os_stat_proxy()
    stat_proxy.set(MAILS_FILE, size=1, mtime=time.time() - (IGNORE_OLDER_THAN_SEC - 1))

    with mock.patch('os.stat', side_effect=stat_proxy.proxy):
        data = manifest.execute('cron_errs')

    expected = {'events': [{'description': 'Details in /var/log/fs_mail/fs_mail.dump',
                            'service': 'cron_errs',
                            'status': 'CRIT'}]}
    assert expected == data


def test_errors_stale(manifest, os_stat_proxy):
    stat_proxy = os_stat_proxy()
    stat_proxy.set(MAILS_FILE, size=1, mtime=time.time() - (IGNORE_OLDER_THAN_SEC + 1))

    with mock.patch('os.stat', side_effect=stat_proxy.proxy):
        data = manifest.execute('cron_errs')

    expected = {'events': [{'description': 'Ok', 'service': 'cron_errs', 'status': 'OK'}]}
    assert expected == data
