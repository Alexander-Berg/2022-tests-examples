# coding: utf-8

import json
import logging
import os.path
import subprocess

from yt.wrapper import ypath_join
import yt.yson
from yt.wrapper.errors import YtOperationFailedError
import yatest.common

from irt.bannerland.options import get_option as get_bl_opt

from bannerland.make_banners import PerfMaker, DynMaker


# в SB-ресурсе могут быть таблицы в старом формате
def fix_tasks_and_offers(row):
    if 'task_id' not in row:
        row['task_id'] = row.pop('task_parent_id')
    if 'export_offers_info' not in row:
        row['export_offers_info'] = row.pop('info_export_offers')

    # в старых данных нет этих айдишников, ставим фейковые
    task = json.loads(row['task_inf'])
    task.setdefault('ClientID', '12345')
    task.setdefault('CounterID', '67890')
    row['task_inf'] = json.dumps(task, ensure_ascii=False)

    yield row


# table_config is a dict {'name': {'path': yt_path, 'file': file_path, **other}}
# if 'empty' is set, just create empty table, do not upload
def upload_tables(yt_client, table_config):
    logging.warning('upload config: %s', table_config)
    for name, info in table_config.items():
        dst_path = info['path']
        logging.warning('upload %s: file %s => table %s', name, info['file'], dst_path)
        yt_client.create('map_node', os.path.dirname(dst_path), recursive=True, ignore_existing=True)
        attrs = {}
        if 'schema' in info:
            attrs['schema'] = yt.yson.YsonList(info['schema'])
            attrs['schema'].attributes['strict'] = False
        yt_client.create('table', dst_path, attributes=attrs)
        if info.get('empty'):
            continue
        with open(info['file']) as fh:
            yt_client.write_table(dst_path, fh, format='json', raw=True)
        if 'post_mapper' in info:
            yt_client.run_map(info['post_mapper'], dst_path, dst_path)

    logging.warning('upload done!')


def run_make_banners(task_type, yt_client, yt_root, input_tao_table):
    build_dir = yatest.common.build_path()
    bl_gendicts = get_bl_opt('bmyt_gendicts')
    bl_gendicts += ['dyn_stat']  # for dyn
    catalogia_spec = {
        'lib': {'type': 'local', 'build_dir': build_dir},
        'dicts': {'type': 'local', 'build_dir': build_dir},
        'perllibs': {'type': 'local', 'dir': '.'},
        'gendicts': [{'name': d, 'type': 'local', 'dir': '.'} for d in bl_gendicts],
        'fake_archives': True,
    }
    yt_meta_spec = {
        'max_cpu': 4,
        'max_mem': 16 * 2**30,
        'bm_layers': 1,
    }

    run_dir = ypath_join(yt_root, 'run')
    maker_class = PerfMaker if task_type == 'perf' else DynMaker
    maker = maker_class(
        yt_dir=run_dir,
        input_tao_table=input_tao_table,
        process_count=1,
        yt_meta_spec=yt_meta_spec, yt_client=yt_client,
        catalogia_spec=catalogia_spec,
        use_yql=False,
    )
    try:
        maker.run()
    except YtOperationFailedError as err:
        logging.error('BMYT operation failed!')
        logging.error(err.attributes)
        raise err

    logging.warning('generated bannerphrases: %d', yt_client.row_count(ypath_join(run_dir, 'generated_banners.final')))


def test_perf_make_banners(yt_stuff):
    yt_client = yt_stuff.get_yt_client()

    yt_root = '//home'
    yt_client.create('map_node', yt_root, ignore_existing=True)
    subprocess.check_call(['tar', 'zxf', 'yt_tables.tar.gz'])

    input_tao_table = ypath_join(yt_root, 'tasks_and_offers')
    table_config = {
        'tasks_and_offers': {
            'path': input_tao_table,
            # указываем только обязательные для генерации поля
            'schema': [
                {'name': 'task_id',         'type': 'string'},
                {'name': 'ppar',            'type': 'string'},
                {'name': 'task_inf',        'type': 'string'},
                {'name': 'product_inf',     'type': 'string'},
                {'name': 'product_class',   'type': 'string'},
                {'name': 'product_md5',     'type': 'string'},
            ],
            'post_mapper': fix_tasks_and_offers,
        },
        'cdict_datoteka': {
            'path': '//home/broadmatching/work/cdict/cdict_chronicle',
        },
        'cdict_chronicle': {
            'path': '//home/broadmatching/work/cdict/cdict_datoteka',
        },
        'dse': {
            'path': "//home/bannerland/data/dse/result/dse_banners",
        },
    }
    for name, info in table_config.items():
        info['file'] = 'yt_tables/{}.json'.format(name)

    upload_tables(yt_client, table_config)

    run_make_banners('perf', yt_client, yt_root, input_tao_table=input_tao_table)


def test_dyn_make_banners(yt_stuff):
    yt_client = yt_stuff.get_yt_client()

    yt_root = '//home'
    yt_client.create('map_node', yt_root, ignore_existing=True)
    subprocess.check_call(['tar', 'zxf', 'dyn_yt_tables.tar.gz'])

    input_tao_table = ypath_join(yt_root, 'tasks_and_offers')
    table_config = {
        'tasks_and_offers': {
            'path': input_tao_table,
            # указываем только обязательные для генерации поля
            'schema': [
                {'name': 'task_id',         'type': 'string'},
                {'name': 'ppar',            'type': 'string'},
                {'name': 'task_inf',        'type': 'string'},
                {'name': 'product_inf',     'type': 'string'},
                {'name': 'product_class',   'type': 'string'},
                {'name': 'product_md5',     'type': 'string'},
            ],
        },
        'cdict_datoteka': {
            'path': '//home/broadmatching/work/cdict/cdict_chronicle',
        },
        'cdict_chronicle': {
            'path': '//home/broadmatching/work/cdict/cdict_datoteka',
        },
        'dse': {
            'path': "//home/bannerland/data/dse/result/dse_banners",
        },
    }

    for name, info in table_config.items():
        info['file'] = 'dyn_yt_tables/{}.json'.format(name)

    upload_tables(yt_client, table_config)
    run_make_banners('dyn', yt_client, yt_root, input_tao_table=input_tao_table)
