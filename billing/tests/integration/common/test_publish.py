
from unittest import mock

import pytest
from hamcrest import assert_that, contains_inanyorder

from yt.wrapper import ypath_join
from nirvana.job_context import DataItem

from billing.log_tariffication.py.jobs.common import publish
from billing.library.python.logmeta_utils.meta import (
    set_log_tariff_meta,
)
from billing.library.python.yt_utils.yt import (
    set_node_ttl
)
from billing.library.python.yt_utils.test_utils.utils import (
    create_subdirectory,
)
from billing.log_tariffication.py.tests.utils import (
    check_node_is_locked
)
from billing.log_tariffication.py.tests.constants import (
    PREV_RUN_ID,
    CURR_RUN_ID,
    NEXT_RUN_ID,
    PREV_LOG_TARIFF_META,
    CURR_LOG_TARIFF_META,
)


@pytest.fixture(name='src_dirs')
def src_dirs_fixture(yt_client, yt_root):
    return [
        create_subdirectory(yt_client, yt_root, 'src'),
        create_subdirectory(yt_client, yt_root, 'src2'),
    ]


@pytest.fixture(name='src_dir')
def src_dir_fixture(src_dirs):
    return src_dirs[0]


@pytest.fixture(name='dst_dirs')
def dst_dirs_fixture(yt_client, yt_root):
    return [
        create_subdirectory(yt_client, yt_root, 'dst'),
        create_subdirectory(yt_client, yt_root, 'dst2'),
    ]


@pytest.fixture(name='dst_dir')
def dst_dir_fixture(dst_dirs):
    return dst_dirs[0]


def create_data_item(link_name: str) -> DataItem:
    item = dict(
        dataType='dataType',
        wasUnpacked=True,
        unpackedDir='unpackedDir',
        unpackedFile='unpackedFile',
        downloadURL='downloadURL',
        linkName=link_name
    )
    return DataItem(item=item, path=link_name)


def test_prepare_tasks():
    data_items = [
        create_data_item('first'),
        create_data_item('second'),
        create_data_item('third'),
    ]
    link_infos = {
        'first': {
            'dst_dir_path': '//first_out_dir',
        },
        'second': {
            'dst_dir_path': '//second_out_dir',
            'preserve_expiration_time': True
        },
        'third': {
            'dst_dir_path': '//third_out_dir',
            'preserve_expiration_time': False
        },
        'fourth': {
            'dst_dir_path': '//fourth_out_dir',
        },
    }

    expected_tasks = [
        publish.Task(
            src_table_path='//first_src_dir/first_table_name',
            dst_dir_path='//first_out_dir',
            preserve_expiration_time=True
        ),
        publish.Task(
            src_table_path='//second_src_dir/second_table_name',
            dst_dir_path='//second_out_dir',
            preserve_expiration_time=True
        ),
        publish.Task(
            src_table_path='//third_src_dir/third_table_name',
            dst_dir_path='//third_out_dir',
            preserve_expiration_time=False
        ),
    ]
    content_path_to_src_table_path = {
        'first': '//first_src_dir/first_table_name',
        'second': '//second_src_dir/second_table_name',
        'third': '//third_src_dir/third_table_name',
    }
    with mock.patch('billing.log_tariffication.py.lib.utils.job.read_file',
                    mock.Mock(side_effect=content_path_to_src_table_path.get)):
        assert_that(publish.prepare_tasks(data_items, link_infos),
                    contains_inanyorder(*expected_tasks))


def test_bad_meta_in_src_table(yt_client, src_dir, dst_dir):
    src_table_path = ypath_join(src_dir, 'table')
    yt_client.create('table', src_table_path)
    set_log_tariff_meta(yt_client, src_table_path, PREV_LOG_TARIFF_META)
    with pytest.raises(AssertionError, match='has bad meta'):
        publish.do_checks(yt_client, publish.Task(src_table_path, dst_dir),
                          CURR_LOG_TARIFF_META,
                          interested_in_curr_table=True)


@pytest.mark.parametrize(
    ['dst_table_name', 'dst_table_meta'],
    [
        pytest.param(
            CURR_RUN_ID, PREV_LOG_TARIFF_META,
            id='bad meta in curr dst table'
        ),
        pytest.param(
            NEXT_RUN_ID, CURR_LOG_TARIFF_META,
            id='next dst table exists'
        ),
        pytest.param(
            PREV_RUN_ID, CURR_LOG_TARIFF_META,
            id='bad meta in prev dst table'
        )
    ]
)
def test_bad_dst_tables(yt_client, src_dir, dst_dir, dst_table_name,
                        dst_table_meta):
    src_table_path = ypath_join(src_dir, CURR_RUN_ID)
    yt_client.create('table', src_table_path)
    set_log_tariff_meta(yt_client, src_table_path, CURR_LOG_TARIFF_META)

    dst_table_path = ypath_join(dst_dir, dst_table_name)
    yt_client.create('table', dst_table_path)
    set_log_tariff_meta(yt_client, dst_table_path, dst_table_meta)

    with pytest.raises(AssertionError):
        publish.do_checks(yt_client, publish.Task(src_table_path, dst_dir),
                          CURR_LOG_TARIFF_META,
                          interested_in_curr_table=True)


@pytest.mark.parametrize(
    ['overwrite', 'preserve_expiration_time'],
    [
        pytest.param(True, True,
                     id='overwrite=True, preserve_expiration_time=True'),
        pytest.param(False, False,
                     id='overwrite=False, preserve_expiration_time=False'),
    ]
)
@pytest.mark.parametrize(
    ['with_checks'],
    [
        pytest.param(True, id='with_checks=True'),
        pytest.param(False, id='with_checks=False'),
    ]
)
def test_copy(yt_client, src_dirs, dst_dirs,
              overwrite, with_checks, preserve_expiration_time):
    src_table_path = ypath_join(src_dirs[0], CURR_RUN_ID)
    yt_client.create('table', src_table_path,
                     # to check that table has been copied
                     attributes={'src-attr': True})
    set_log_tariff_meta(yt_client, src_table_path, CURR_LOG_TARIFF_META)
    set_node_ttl(yt_client, src_table_path, hours=1)

    task = publish.Task(src_table_path, dst_dirs[0],
                        preserve_expiration_time=preserve_expiration_time)

    if overwrite or not with_checks:
        # Must be ignored
        yt_client.create('table', task.dst_table_path)
        set_log_tariff_meta(yt_client, task.dst_table_path, PREV_LOG_TARIFF_META)

    if not with_checks:
        # Must not check its existence
        yt_client.create('table', ypath_join(src_dirs[0], NEXT_RUN_ID))

    task2 = publish.Task(ypath_join(src_dirs[1], CURR_RUN_ID), dst_dirs[1])
    yt_client.create('table', task2.src_table_path)
    set_log_tariff_meta(yt_client, task2.src_table_path, CURR_LOG_TARIFF_META)

    with yt_client.Transaction(ping=False):
        publish.run_job(
            yt_client, [task, task2],
            overwrite,
            with_checks,
        )
        check_node_is_locked(task.dst_dir_path)

    dst_table = yt_client.get(task.dst_table_path,
                              attributes=['src-attr', 'expiration_time'])
    assert dst_table.attributes['src-attr']
    expiration_time = dst_table.attributes.get('expiration_time')
    assert expiration_time is None or preserve_expiration_time

    assert yt_client.exists(task2.dst_table_path)


@pytest.mark.usefixtures('yt_transaction')
def test_skipped(yt_client, src_dirs, dst_dirs):
    tasks = []
    for src_dir, dst_dir in zip(src_dirs, dst_dirs):
        src_table_path = ypath_join(src_dir, CURR_RUN_ID)
        task = publish.Task(src_table_path, dst_dir)
        yt_client.create('table', src_table_path)
        set_log_tariff_meta(yt_client, src_table_path, CURR_LOG_TARIFF_META)
        yt_client.create('table', task.dst_table_path,
                         # to check that table has not been overwritten
                         attributes={'dst-attr': True})
        set_log_tariff_meta(yt_client, task.dst_table_path, CURR_LOG_TARIFF_META)
        tasks.append(task)

    publish.run_job(yt_client, tasks)
    for task in tasks:
        assert yt_client.get(ypath_join(task.dst_table_path, '@dst-attr'))


@pytest.mark.usefixtures('yt_transaction')
def test_dst_tables_partially_exist(yt_client, src_dirs, dst_dirs):
    tasks = []
    for src_dir, dst_dir in zip(src_dirs, dst_dirs):
        src_table_path = ypath_join(src_dir, CURR_RUN_ID)
        task = publish.Task(src_table_path, dst_dir)
        yt_client.create('table', src_table_path)
        set_log_tariff_meta(yt_client, src_table_path, CURR_LOG_TARIFF_META)
        tasks.append(task)

    yt_client.create('table', tasks[0].dst_table_path)
    set_log_tariff_meta(yt_client, tasks[0].dst_table_path, CURR_LOG_TARIFF_META)

    with pytest.raises(AssertionError, match='Destination tables partially exist'):
        publish.run_job(yt_client, tasks)
