from mapreduce.yt.python.yt_stuff import YtStuff
import sys
import time

from os import environ


environ["YT_STUFF_MAX_START_RETRIES"] = "2"

# import yt.wrapper as yt_wrapper


def test_start_stop():
    yt_stuff = YtStuff()
    yt_stuff.start_local_yt()
    yt_stuff.stop_local_yt()


def test_yt_client(yt_stuff):
    yt_client = yt_stuff.get_yt_client()
    assert not yt_client.exists("//hello/world/path")


def test_create_folder(yt_stuff):
    print('here!!!')
    yt_client = yt_stuff.get_yt_client()
    path = '//home/x-products/production'
    yt_client.create('map_node', path, recursive=True)
    assert yt_client.exists(path)


def test_create_dyn_table(yt_stuff):
    yt_client = yt_stuff.get_yt_client()
    table = '//testing/dyn-table'
    yt_client.create('map_node', '//testing')

    schema = [
        {'name': 'x', 'type': 'string', 'sort_order': 'ascending'},
        {'name': 'y', 'type': 'string'}
    ]

    yt_client.create('table', table, attributes={'schema': schema, 'dynamic': True})
    yt_client.mount_table(table)

    while yt_client.get('{0}/@tablets/0/state'.format(table)) != 'mounted':
        time.sleep(0.1)

    yt_client.insert_rows(table, [{'x': 'hello', 'y': 'world'}])
    print >>sys.stderr, list(yt_client.select_rows('* from [*]'.format(table)))
