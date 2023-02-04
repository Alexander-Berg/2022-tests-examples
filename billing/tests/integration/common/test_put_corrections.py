
from unittest import mock

import pytest
from hamcrest import assert_that, contains_inanyorder

from yt.wrapper import ypath_join, ypath_dirname
from nirvana.job_context import DataItem

from billing.log_tariffication.py.jobs.common import put_corrections
from billing.library.python.logmeta_utils.meta import (
    set_log_tariff_meta,
    get_log_tariff_meta,
)

from billing.log_tariffication.py.lib.constants import (
    CORRECTIONS_DATA_KEY,
    ORIGINAL_PATH_KEY,
)
from billing.library.python.yt_utils.test_utils.utils import (
    create_subdirectory,
)
from billing.log_tariffication.py.tests.utils import (
    check_node_is_locked,
    patch_generate_run_id
)
from billing.log_tariffication.py.tests.constants import (
    PREV_RUN_ID,
    CURR_RUN_ID,
    NEXT_RUN_ID,
    PREV_LOG_TARIFF_META,
    CURR_LOG_TARIFF_META,
)

CORRECTION_DATA = [
    {
        'ticket': 'TEST-666',
        'run_id': '2020-01-01T00:00:00'
    }
]

META_W_CORRECTIONS = {
    **CURR_LOG_TARIFF_META,
    CORRECTIONS_DATA_KEY: CORRECTION_DATA
}


@pytest.fixture(name='src_dirs')
def src_dirs_fixture(yt_client, yt_root):
    return [
        create_subdirectory(yt_client, yt_root, 'src'),
        create_subdirectory(yt_client, yt_root, 'src2'),
    ]


@pytest.fixture(name='dst_dirs')
def dst_dirs_fixture(yt_client, yt_root):
    return [
        create_subdirectory(yt_client, yt_root, 'dst'),
        create_subdirectory(yt_client, yt_root, 'dst2'),
    ]


@pytest.fixture(name='dst_dir')
def dst_dir_fixture(dst_dirs):
    return dst_dirs[0]


@pytest.fixture(name='backup_dirs')
def backup_dirs_fixture(yt_client, yt_root):
    return [
        create_subdirectory(yt_client, yt_root, 'backup'),
        create_subdirectory(yt_client, yt_root, 'backup2'),
    ]


def create_artifact_meta_w_correction(yt_root, tbl_name, corrections_meta):
    meta = {
        **CURR_LOG_TARIFF_META,
        CORRECTIONS_DATA_KEY: {ypath_join(yt_root, tbl_name): {CORRECTIONS_DATA_KEY: corrections_meta}}
    }
    return meta


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
            'dst_dir': '//first_out_dir',
            'backup_dir': '//first_backup_dir'
        },
        'second': {
            'dst_dir': '//second_out_dir',
            'backup_dir': '//second_backup_dir'
        },
        'third': {
            'dst_dir': '//third_out_dir',
            'backup_dir': '//third_backup_dir'
        },
        'fourth': {
            'dst_dir': '//fourth_out_dir',
            'backup_dir': '//fourth_backup_dir'
        },
    }

    expected_tasks = [
        put_corrections.Task(
            src_path='//first_src_dir/first_table_name',
            dst_dir='//first_out_dir',
            backup_dir='//first_backup_dir',
        ),
        put_corrections.Task(
            src_path='//second_src_dir/second_table_name',
            dst_dir='//second_out_dir',
            backup_dir='//second_backup_dir',
        ),
        put_corrections.Task(
            src_path='//third_src_dir/third_table_name',
            dst_dir='//third_out_dir',
            backup_dir='//third_backup_dir',
        ),
    ]
    content_path_to_src_table_path = {
        'first': '//first_src_dir/first_table_name',
        'second': '//second_src_dir/second_table_name',
        'third': '//third_src_dir/third_table_name',
    }
    with mock.patch('billing.log_tariffication.py.lib.utils.job.read_file',
                    mock.Mock(side_effect=content_path_to_src_table_path.get)):
        assert_that(put_corrections.prepare_tasks(data_items, link_infos),
                    contains_inanyorder(*expected_tasks))


def test_bad_last_table_interval(yt_client, dst_dir):
    dst_table_path = ypath_join(dst_dir, 'table')
    yt_client.create('table', dst_table_path)
    set_log_tariff_meta(yt_client, dst_table_path, PREV_LOG_TARIFF_META)

    checker = put_corrections.LastTableChecker(yt_client, CURR_LOG_TARIFF_META)
    with pytest.raises(AssertionError, match='Correction table misaligned'):
        checker.check_last_table(dst_dir)


@pytest.mark.parametrize(
    'table_meta, artifact_meta, error',
    [
        pytest.param(
            ('dst_table1', META_W_CORRECTIONS),
            None,
            'Correction data not present in artifact',
            id='no corr meta in artifact'
        ),
        pytest.param(
            ('dst_table2', None),
            ('dst_table2', CORRECTION_DATA),
            'Correction data not present in table',
            id='no corr meta in table'
        ),
        pytest.param(
            ('dst_table3', META_W_CORRECTIONS),
            ('other_dst_table', CORRECTION_DATA),
            r'No correction data in artifact for table *',
            id='no corr meta for table'
        ),
        pytest.param(
            ('dst_table4', META_W_CORRECTIONS),
            ('dst_table4', [{'ticket': 'wrong', 'run_id': 'meta'}]),
            r'Correction data mismatch for table *',
            id='not matching corr metas for table'
        ),
    ]
)
def test_bad_last_table_meta(yt_client, yt_root, table_meta, artifact_meta, error):
    dst_path = ypath_join(yt_root, table_meta[0])
    yt_client.create('table', dst_path)
    table_meta = table_meta[1] or CURR_LOG_TARIFF_META
    artifact_meta = artifact_meta and create_artifact_meta_w_correction(yt_root, *artifact_meta)
    artifact_meta = artifact_meta or CURR_LOG_TARIFF_META

    set_log_tariff_meta(yt_client, dst_path, table_meta)

    checker = put_corrections.LastTableChecker(yt_client, artifact_meta)
    with pytest.raises(AssertionError, match=error):
        checker.check_last_table(ypath_dirname(dst_path))


def test_single_run(yt_client, src_dirs, dst_dirs, backup_dirs):
    ticket = 'TEST-666'
    src_table_path = ypath_join(src_dirs[0], CURR_RUN_ID)
    yt_client.write_table(src_table_path, [{'src': 1}])

    corrections_data = [
        {
            'ticket': ticket,
            'run_id': CURR_RUN_ID
        }
    ]

    task = put_corrections.Task(
        src_path=src_table_path,
        dst_dir=dst_dirs[0],
        backup_dir=backup_dirs[0]
    )

    dst_path = ypath_join(task.dst_dir, PREV_RUN_ID)
    yt_client.create('table', dst_path)
    set_log_tariff_meta(yt_client, dst_path, PREV_LOG_TARIFF_META)

    with yt_client.Transaction(ping=False), patch_generate_run_id(return_value=CURR_RUN_ID):
        new_meta = put_corrections.run_job(
            yt_client,
            [task],
            PREV_LOG_TARIFF_META,
            ticket
        )
        check_node_is_locked(task.dst_dir)

    dst_rows = yt_client.read_table(dst_path)
    dst_meta = get_log_tariff_meta(yt_client, dst_path)
    assert_that(list(dst_rows), contains_inanyorder({'src': 1}))
    assert dst_meta[CORRECTIONS_DATA_KEY] == corrections_data

    backup_path = ypath_join(task.backup_dir, f'{CURR_RUN_ID}_{ticket}')
    backup_rows = yt_client.read_table(backup_path)
    backup_meta = get_log_tariff_meta(yt_client, backup_path)
    assert len(list(backup_rows)) == 0
    assert backup_meta[ORIGINAL_PATH_KEY] == dst_path

    assert new_meta[CORRECTIONS_DATA_KEY] == {dst_path: corrections_data}


def test_multiple_runs(yt_client, src_dirs, dst_dirs, backup_dirs):
    ticket1, ticket2 = 'TEST-666', 'TEST-667'
    src_table_path1 = ypath_join(src_dirs[1], CURR_RUN_ID)
    src_table_path2 = ypath_join(src_dirs[1], NEXT_RUN_ID)
    yt_client.write_table(src_table_path1, [{'src': 1}])
    yt_client.write_table(src_table_path2, [{'src': 2}])

    corrections_data = [
        {
            'ticket': ticket1,
            'run_id': CURR_RUN_ID
        },
        {
            'ticket': ticket2,
            'run_id': NEXT_RUN_ID
        },
    ]

    task1 = put_corrections.Task(
        src_path=src_table_path1,
        dst_dir=dst_dirs[1],
        backup_dir=backup_dirs[1]
    )
    task2 = put_corrections.Task(
        src_path=src_table_path2,
        dst_dir=dst_dirs[1],
        backup_dir=backup_dirs[1]
    )

    dst_path = ypath_join(task1.dst_dir, PREV_RUN_ID)
    yt_client.create('table', dst_path)
    set_log_tariff_meta(yt_client, dst_path, PREV_LOG_TARIFF_META)

    with yt_client.Transaction(ping=False), patch_generate_run_id(return_value=CURR_RUN_ID):
        new_meta1 = put_corrections.run_job(
            yt_client,
            [task1],
            PREV_LOG_TARIFF_META,
            ticket1
        )
        check_node_is_locked(task1.dst_dir)

    with yt_client.Transaction(ping=False), patch_generate_run_id(return_value=NEXT_RUN_ID):
        new_meta2 = put_corrections.run_job(
            yt_client,
            [task2],
            new_meta1,
            ticket2
        )
        check_node_is_locked(task2.dst_dir)

    dst_rows = yt_client.read_table(dst_path)
    dst_meta = get_log_tariff_meta(yt_client, dst_path)
    assert_that(list(dst_rows), contains_inanyorder({'src': 1}, {'src': 2}))
    assert dst_meta[CORRECTIONS_DATA_KEY] == corrections_data

    backup_path1 = ypath_join(task1.backup_dir, f'{CURR_RUN_ID}_{ticket1}')
    backup_rows1 = yt_client.read_table(backup_path1)
    backup_meta1 = get_log_tariff_meta(yt_client, backup_path1)
    assert len(list(backup_rows1)) == 0
    assert backup_meta1[ORIGINAL_PATH_KEY] == dst_path

    backup_path2 = ypath_join(task2.backup_dir, f'{NEXT_RUN_ID}_{ticket2}')
    backup_rows2 = yt_client.read_table(backup_path2)
    backup_meta2 = get_log_tariff_meta(yt_client, backup_path2)
    assert_that(list(backup_rows2), contains_inanyorder({'src': 1}))
    assert backup_meta2[ORIGINAL_PATH_KEY] == dst_path
    assert backup_meta2[CORRECTIONS_DATA_KEY] == [corrections_data[0]]

    assert new_meta2[CORRECTIONS_DATA_KEY] == {dst_path: corrections_data}
