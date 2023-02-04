# coding: utf-8
import datetime
import json
import pytest

import yt.wrapper as yt_wrapper
import yql.api.v1.client as yql
import library.python.resource as resource
import transfer_manager.python.recipe.interface as tm_recipe

import bannerland.archive_workers.export_deltas as export_deltas
import bannerland.archive_workers.common as common
import irt.bannerland.options
import irt.bannerland.options.cypress

TASK_TYPES = ['dyn', 'perf']


@pytest.fixture()
def yql_client(yql_api):
    return yql.YqlClient(
        server='localhost',
        port=yql_api.port,
        db='plato'
    )


@pytest.fixture()
def yt_client(yt):
    return yt.yt_wrapper


@pytest.fixture(params=TASK_TYPES)
def task_type(request):
    return request.param


@pytest.fixture()
def banners_schema(task_type):
    return get_fs_schema(task_type)


@pytest.fixture()
def archive_dirs(yt_client, task_type):
    fs_dir = create_fs_dir(yt_client, task_type)
    archive_dirs = create_archive_dirs(yt_client, fs_dir, 2)
    yt_client.link(archive_dirs[-1], yt_wrapper.ypath_join(archive_dirs[0], 'prev_fs_dir'))
    yield archive_dirs
    yt_client.remove(fs_dir, force=True, recursive=True)


@pytest.fixture()
def banner(task_type):
    return json.loads(resource.find('{}_banner.json'.format(task_type)))


def create_fs_dir(yt_client, task_type):
    fs_path_name = 'full_state_archive'
    deltas_export_path_name = 'deltas_export'
    if task_type == 'perf':
        fs_archive = irt.bannerland.options.cypress.PerfYTConfig().get_path(fs_path_name)
        deltas_export = irt.bannerland.options.cypress.PerfYTConfig().get_path(deltas_export_path_name)
    elif task_type == 'dyn':
        fs_archive = irt.bannerland.options.cypress.DynYTConfig().get_path(fs_path_name)
        deltas_export = irt.bannerland.options.cypress.DynYTConfig().get_path(deltas_export_path_name)
    else:
        raise ValueError('task_type not found')
    yt_client.create('map_node', fs_archive, recursive=True, ignore_existing=True)
    yt_client.create('map_node', yt_wrapper.ypath_join(deltas_export, 'v1', 'banners'), recursive=True, ignore_existing=True)
    yt_client.create('map_node', yt_wrapper.ypath_join(deltas_export, 'v1', 'phrases'), recursive=True, ignore_existing=True)
    return fs_archive


def create_archive_dirs(yt_client, fs_archive_dir, count):
    current_time = datetime.datetime.now()
    archive_tables = []
    for i in range(count):
        current_time -= datetime.timedelta(hours=i)
        archive_node_name = current_time.strftime(common.FS_NAME_DATE_FORMAT)
        path = yt_wrapper.ypath_join(fs_archive_dir, archive_node_name)
        yt_client.create('map_node', yt_wrapper.ypath_join(path, 'final'), recursive=True, ignore_existing=True)
        archive_tables.append(path)
    return archive_tables


def create_banners_table(yt_client, fs_archive_path, data, schema):
    table = yt_wrapper.ypath_join(fs_archive_path, 'final', 'banners')
    yt_client.create('table', table, attributes={'schema': schema})
    yt_client.write_table(table, data)
    yt_client.run_sort(table, sort_by=["BannerID"])


def get_fs_schema(task_type):
    if task_type == 'dyn':
        new_avatars_name = 'Images'
        all_columns = irt.bannerland.options.get_option('dyn_result_columns')
    else:
        new_avatars_name = 'Avatars'
        all_columns = irt.bannerland.options.get_option('perf_result_columns')

    banners_schema = []
    res_fields = [col for col in all_columns if col.get('banner')]
    for col_info in res_fields:
        field_name = col_info['name']
        if field_name == 'avatars':
            field_name = new_avatars_name
        banners_schema.append({'name': field_name, 'type': col_info['type']})
    banners_schema.append({'name': 'BroadPhrases', 'type': 'any'})

    return banners_schema


class TestDeltas(tm_recipe.TransferManagerTest):
    def test_no_diff(self, yql_client, yt_client, task_type, banners_schema, archive_dirs, banner):
        create_banners_table(yt_client, archive_dirs[-1], [banner], banners_schema)
        create_banners_table(yt_client, archive_dirs[0], [banner], banners_schema)

        worker = export_deltas.DeltasFSWorker(
            task_type=task_type, attr_name=common.get_attr_name('export_deltas'), yt_client=yt_client, yql_client=yql_client,
            ignore_depends=True, use_monitorings=False, transfer_manager_url="{}/api/v1".format(self.tm_proxy), transfer_manager_token=self.tm_token)
        worker.do_work(archive_dirs[0])

        assert yt_client.row_count(yt_wrapper.ypath_join(archive_dirs[0], 'delta_banners_diff')) == 0
        assert yt_client.row_count(yt_wrapper.ypath_join(archive_dirs[0], 'delta_banners_del')) == 0

    def test_has_del_and_came_diff(self, yql_client, yt_client, task_type, banners_schema, archive_dirs, banner):
        banner_old = banner.copy()
        banner_old['BannerID'] += 1

        create_banners_table(yt_client, archive_dirs[-1], [banner_old], banners_schema)
        create_banners_table(yt_client, archive_dirs[0], [banner], banners_schema)

        worker = export_deltas.DeltasFSWorker(
            task_type=task_type, attr_name=common.get_attr_name('export_deltas'), yt_client=yt_client, yql_client=yql_client,
            ignore_depends=True, use_monitorings=False, transfer_manager_url="{}/api/v1".format(self.tm_proxy), transfer_manager_token=self.tm_token)
        worker.do_work(archive_dirs[0])

        assert yt_client.row_count(yt_wrapper.ypath_join(archive_dirs[0], 'delta_banners_diff')) == 1
        for row in yt_client.read_table(yt_wrapper.ypath_join(archive_dirs[0], 'delta_banners_diff')):
            assert row['DeltaSource'] == {'type': 'new'}
            assert int(row['BannerID']) == int(banner['BannerID'])
        assert yt_client.row_count(yt_wrapper.ypath_join(archive_dirs[0], 'delta_banners_del')) == 1

    def test_has_del_diff(self, yql_client, yt_client, task_type, banners_schema, archive_dirs, banner):
        banner_old = banner.copy()
        banner_old['BannerID'] += 1

        create_banners_table(yt_client, archive_dirs[-1], [banner_old], banners_schema)
        create_banners_table(yt_client, archive_dirs[0], [banner, banner_old], banners_schema)

        worker = export_deltas.DeltasFSWorker(
            task_type=task_type, attr_name=common.get_attr_name('export_deltas'), yt_client=yt_client, yql_client=yql_client,
            ignore_depends=True, use_monitorings=False, transfer_manager_url="{}/api/v1".format(self.tm_proxy), transfer_manager_token=self.tm_token)
        worker.do_work(archive_dirs[0])

        assert yt_client.row_count(yt_wrapper.ypath_join(archive_dirs[0], 'delta_banners_diff')) == 1
        for row in yt_client.read_table(yt_wrapper.ypath_join(archive_dirs[0], 'delta_banners_diff')):
            assert row['DeltaSource'] == {'type': 'new'}
            assert int(row['BannerID']) == int(banner['BannerID'])
        assert yt_client.row_count(yt_wrapper.ypath_join(archive_dirs[0], 'delta_banners_del')) == 0

    def test_begin_time_diff(self, yql_client, yt_client, task_type, banners_schema, archive_dirs, banner):
        banner_1 = banner
        banner_1_new = banner.copy()
        banner_2 = banner.copy()
        banner_2['BannerID'] += 1
        banner_2_new = banner_2.copy()

        banner_1_new['BannerlandBeginTime'] += 24 * 3600
        banner_2_new['BannerlandBeginTime'] += 60 * 3600

        create_banners_table(yt_client, archive_dirs[-1], [banner_1, banner_2], banners_schema)
        create_banners_table(yt_client, archive_dirs[0], [banner_1_new, banner_2_new], banners_schema)

        yt_root = irt.bannerland.options.get_cypress_config(task_type).root
        yt_client.set(yt_wrapper.ypath_join(yt_root, '@banners__resend_by_begin_time_ratio'), 1.0)

        worker = export_deltas.DeltasFSWorker(
            task_type=task_type, attr_name=common.get_attr_name('export_deltas'), yt_client=yt_client, yql_client=yql_client,
            ignore_depends=True, use_monitorings=False, transfer_manager_url="{}/api/v1".format(self.tm_proxy), transfer_manager_token=self.tm_token)
        worker.do_work(archive_dirs[0])

        assert yt_client.row_count(yt_wrapper.ypath_join(archive_dirs[0], 'delta_banners_diff')) == 1
        for row in yt_client.read_table(yt_wrapper.ypath_join(archive_dirs[0], 'delta_banners_diff')):
            assert row['DeltaSource']['type'] == 'diff'
            assert row['DeltaSource']['fields'] == ['BannerlandBeginTime']
            assert int(row['BannerID']) == int(banner_2['BannerID'])
        assert yt_client.row_count(yt_wrapper.ypath_join(archive_dirs[0], 'delta_banners_del')) == 0

    def test_remove_column_diff(self, yql_client, yt_client, task_type, banners_schema, archive_dirs, banner):
        banner_old = banner.copy()
        banner_old.pop('BannerPrice')

        create_banners_table(yt_client, archive_dirs[-1], [banner_old], banners_schema)
        create_banners_table(yt_client, archive_dirs[0], [banner, banner_old], banners_schema)

        worker = export_deltas.DeltasFSWorker(
            task_type=task_type, attr_name=common.get_attr_name('export_deltas'), yt_client=yt_client, yql_client=yql_client,
            ignore_depends=True, use_monitorings=False, transfer_manager_url="{}/api/v1".format(self.tm_proxy), transfer_manager_token=self.tm_token)
        worker.do_work(archive_dirs[0])

        assert yt_client.row_count(yt_wrapper.ypath_join(archive_dirs[0], 'delta_banners_diff')) == 0
        assert yt_client.row_count(yt_wrapper.ypath_join(archive_dirs[0], 'delta_banners_del')) == 0

    def test_add_new_column_diff(self, yql_client, yt_client, task_type, banners_schema, archive_dirs, banner):
        banner_old = banner.copy()
        banner['BannerPriceNew'] = 1

        create_banners_table(yt_client, archive_dirs[-1], [banner_old], banners_schema)
        banners_schema.append({'name': 'BannerPriceNew', 'type': 'int64'})
        create_banners_table(yt_client, archive_dirs[0], [banner], banners_schema)

        worker = export_deltas.DeltasFSWorker(
            task_type=task_type, attr_name=common.get_attr_name('export_deltas'), yt_client=yt_client, yql_client=yql_client,
            ignore_depends=True, use_monitorings=False, transfer_manager_url="{}/api/v1".format(self.tm_proxy), transfer_manager_token=self.tm_token)
        worker.do_work(archive_dirs[0])

        assert yt_client.row_count(yt_wrapper.ypath_join(archive_dirs[0], 'delta_banners_diff')) == 1
        for row in yt_client.read_table(yt_wrapper.ypath_join(archive_dirs[0], 'delta_banners_diff')):
            assert row['DeltaSource']['type'] == 'diff'
            assert row['DeltaSource']['fields'] == ['BannerPriceNew']
            assert int(row['BannerID']) == int(banner['BannerID'])
        assert yt_client.row_count(yt_wrapper.ypath_join(archive_dirs[0], 'delta_banners_del')) == 0
