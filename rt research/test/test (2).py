import time

import bannerland.backup_yt_tables.lib as backup_yt_tables
import irt.logging

logger = irt.logging.getLogger(irt.logging.BANNERLAND_PROJECT, __name__)

TABLE_NAME = '//home/some_table_to_backup'


def get_data(number):
    return [{"i": x, "n": number} for x in range(3)]


def make_backup_config(
        proxy,
        period=0.1,
        max_tables=None,
        regex=None,
        source_path=TABLE_NAME,
        backup_subdir='test'):
    return [{
        'source_path': source_path,
        'backup_subdir': backup_subdir,
        'src_cluster': proxy,
        'dst_cluster': proxy,
        'backup_period': f'PT{period}S',
        'backup_latest_regex': regex,
        'max_tables': max_tables,
    }]


def clear_before_test(local_yt_client):
    if local_yt_client.exists(TABLE_NAME):
        local_yt_client.remove(TABLE_NAME, recursive=True, force=True)
    if local_yt_client.exists('//home/bannerland'):
        local_yt_client.remove('//home/bannerland', recursive=True, force=True)


def get_result_dir(subdir='test'):
    return f'//home/bannerland/testing/backups/{subdir}'


def test_backup_when_there_is_none(local_yt_client):
    clear_before_test(local_yt_client)
    proxy = local_yt_client.config['proxy']['url']

    backup_config = make_backup_config(proxy)

    local_yt_client.write_table(TABLE_NAME, get_data(1), format="json")
    backup_yt_tables.backup(backup_config, 'token', 'testing')

    assert local_yt_client.exists(get_result_dir())
    backups = local_yt_client.list(get_result_dir(), absolute=True, sort=True)
    assert len(backups) == 1
    assert get_data(1) == list(local_yt_client.read_table(backups[-1]))
    assert local_yt_client.get_attribute(backups[-1], 'backup_source_object') == TABLE_NAME
    assert local_yt_client.get_attribute(backups[-1], 'backup_source_path') == TABLE_NAME


def test_backup_when_time_is_not_come(local_yt_client):
    clear_before_test(local_yt_client)
    proxy = local_yt_client.config['proxy']['url']

    backup_config = make_backup_config(proxy, period=10000)

    local_yt_client.write_table(TABLE_NAME, get_data(1), format="json")
    backup_yt_tables.backup(backup_config, 'token', 'testing')
    prev_attr = local_yt_client.get_attribute(get_result_dir(), 'last_backup_dt')
    local_yt_client.write_table(TABLE_NAME, get_data(2), format="json")
    backup_yt_tables.backup(backup_config, 'token', 'testing')
    current_attr = local_yt_client.get_attribute(get_result_dir(), 'last_backup_dt')

    assert len(local_yt_client.list(get_result_dir())) == 1
    assert prev_attr == current_attr


def test_backup_after_period(local_yt_client):
    clear_before_test(local_yt_client)
    proxy = local_yt_client.config['proxy']['url']

    backup_config = make_backup_config(proxy, period=1)

    local_yt_client.write_table(TABLE_NAME, get_data(1), format="json")
    backup_yt_tables.backup(backup_config, 'token', 'testing')
    prev_attr = local_yt_client.get_attribute(get_result_dir(), 'last_backup_dt')
    local_yt_client.write_table(TABLE_NAME, get_data(2), format="json")
    time.sleep(1.05)
    backup_yt_tables.backup(backup_config, 'token', 'testing')
    current_attr = local_yt_client.get_attribute(get_result_dir(), 'last_backup_dt')

    backups = local_yt_client.list(get_result_dir(), absolute=True, sort=True)
    assert len(backups) == 2
    assert get_data(2) == list(local_yt_client.read_table(backups[-1]))
    assert prev_attr != current_attr


def test_backup_not_changed_table(local_yt_client):
    clear_before_test(local_yt_client)
    proxy = local_yt_client.config['proxy']['url']

    backup_config = make_backup_config(proxy, period=1)

    local_yt_client.write_table(TABLE_NAME, get_data(1), format="json")
    backup_yt_tables.backup(backup_config, 'token', 'testing')
    prev_attr = local_yt_client.get_attribute(get_result_dir(), 'last_backup_dt')
    time.sleep(1.05)
    backup_yt_tables.backup(backup_config, 'token', 'testing')
    current_attr = local_yt_client.get_attribute(get_result_dir(), 'last_backup_dt')

    assert len(local_yt_client.list(get_result_dir())) == 1
    assert prev_attr != current_attr


def test_backup_max_tables(local_yt_client):
    clear_before_test(local_yt_client)
    proxy = local_yt_client.config['proxy']['url']

    backup_config = make_backup_config(proxy, max_tables=2, period=1)

    local_yt_client.write_table(TABLE_NAME, get_data(1), format="json")
    backup_yt_tables.backup(backup_config, 'token', 'testing')
    local_yt_client.write_table(TABLE_NAME, get_data(2), format="json")
    time.sleep(1.05)
    backup_yt_tables.backup(backup_config, 'token', 'testing')
    local_yt_client.write_table(TABLE_NAME, get_data(1), format="json")
    time.sleep(1.05)
    backup_yt_tables.backup(backup_config, 'token', 'testing')

    assert len(local_yt_client.list(get_result_dir())) == 2


def test_backup_regex(local_yt_client):
    clear_before_test(local_yt_client)
    proxy = local_yt_client.config['proxy']['url']

    backup_config = make_backup_config(proxy, period=1, source_path='//home', regex=r'^.+\.diff$', backup_subdir='diff')
    backup_config += make_backup_config(proxy, period=1, source_path='//home', regex=r'^.+\.del$', backup_subdir='del')

    local_yt_client.write_table('//home/a.diff', get_data(1), format="json")
    local_yt_client.write_table('//home/a.del', get_data(2), format="json")
    local_yt_client.write_table('//home/b.diff', get_data(3), format="json")
    local_yt_client.write_table('//home/b.del', get_data(4), format="json")
    backup_yt_tables.backup(backup_config, 'token', 'testing')

    backups = local_yt_client.list(get_result_dir('diff'), absolute=True, sort=True)
    assert len(backups) == 1
    assert get_data(3) == list(local_yt_client.read_table(backups[-1]))
    assert local_yt_client.get_attribute(backups[-1], 'backup_source_object') == '//home/b.diff'
    assert local_yt_client.get_attribute(backups[-1], 'backup_source_path') == '//home'
    backups = local_yt_client.list(get_result_dir('del'), absolute=True, sort=True)
    assert len(backups) == 1
    assert get_data(4) == list(local_yt_client.read_table(backups[-1]))
    assert local_yt_client.get_attribute(backups[-1], 'backup_source_object') == '//home/b.del'
    assert local_yt_client.get_attribute(backups[-1], 'backup_source_path') == '//home'

    local_yt_client.write_table('//home/c.diff', get_data(5), format="json")
    local_yt_client.write_table('//home/c.del', get_data(6), format="json")
    local_yt_client.write_table('//home/.diff', get_data(7), format="json")
    local_yt_client.write_table('//home/olololo_del', get_data(8), format="json")
    time.sleep(1.05)
    backup_yt_tables.backup(backup_config, 'token', 'testing')

    backups = local_yt_client.list(get_result_dir('diff'), absolute=True, sort=True)
    assert len(backups) == 2
    assert get_data(5) == list(local_yt_client.read_table(backups[-1]))
    assert local_yt_client.get_attribute(backups[-1], 'backup_source_object') == '//home/c.diff'
    assert local_yt_client.get_attribute(backups[-1], 'backup_source_path') == '//home'
    backups = local_yt_client.list(get_result_dir('del'), absolute=True, sort=True)
    assert len(backups) == 2
    assert get_data(6) == list(local_yt_client.read_table(backups[-1]))
    assert local_yt_client.get_attribute(backups[-1], 'backup_source_object') == '//home/c.del'
    assert local_yt_client.get_attribute(backups[-1], 'backup_source_path') == '//home'
