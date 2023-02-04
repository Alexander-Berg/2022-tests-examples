# -*- coding: utf-8 -*-

from datetime import datetime
import pytest
import hamcrest as hm
import logging

from marshmallow.exceptions import MarshmallowError
from yt.wrapper import ypath_join
import yt.wrapper as yt

from billing.library.python.logfeller_utils.log_interval import (
    LogInterval,
    Subinterval,
)
from billing.library.python import yt_utils
from billing.log_tariffication.py.lib.archive import (
    ArchiveTreeProcessor,
    DirectoryConfiguration,
    MergeMode,
    DeletionMode,
)
from billing.log_tariffication.py.lib.constants import (
    LOG_TARIFF_ARCHIVE_ATTR,
    LOG_TARIFF_META_ATTR,
)


TEST_SCHEMA = [
    {'name': 'a', 'type': 'int64'},
    {'name': 'b', 'type': 'uint64'},
    {'name': 'c', 'type': 'string'},
]


@pytest.fixture(name='task_dir')
def create_task_dir(yt_root, yt_client):
    path = ypath_join(yt_root, 'task_dir')
    yt_client.create(
        'map_node',
        path,
    )
    return path


@pytest.fixture(name='archive_tree_processor')
def create_archive_tree_processor(yt_client, task_dir):
    return ArchiveTreeProcessor(task_dir, yt_client)


@pytest.mark.parametrize('node_type', ['map_node', 'table'])
def test_get_type(yt_client, task_dir, node_type):
    test = ypath_join(task_dir, 'test')
    yt_client.create(node_type, test)
    hm.assert_that(yt_utils.yt.get_node_type(yt_client, test), node_type)


@pytest.mark.parametrize('merge_mode', [mode for mode in MergeMode] + [None])
@pytest.mark.parametrize('merge_period', [10, None])
@pytest.mark.parametrize(
    ['deletion_mode', 'deletion_params'],
    [
        pytest.param(DeletionMode.AFTER_N_FILES, {'last_files': 10}),
        pytest.param(DeletionMode.AFTER_N_DAYS, {'last_days': 10}),
        pytest.param(None, None),
    ],
)
@pytest.mark.parametrize('recursive', [True, False, None])
def test_load_config(yt_client, task_dir, archive_tree_processor, merge_period,
                     merge_mode, deletion_mode, deletion_params, recursive):
    attr_dict = {}
    if merge_mode is not None:
        attr_dict['merge_mode'] = merge_mode.value
    if merge_period is not None:
        attr_dict['merge_period'] = merge_period
    if deletion_mode is not None:
        attr_dict['deletion_mode'] = deletion_mode.value
        attr_dict['deletion_params'] = deletion_params
    if recursive is not None:
        attr_dict['recursive'] = recursive
    yt_client.set_attribute(
        task_dir,
        LOG_TARIFF_ARCHIVE_ATTR,
        attr_dict,
    )

    config = archive_tree_processor.load_config(task_dir)
    hm.assert_that(
        config,
        hm.has_properties(
            merge_mode=merge_mode,
            merge_period=merge_period,
            deletion_mode=deletion_mode,
            deletion_params=hm.equal_to(deletion_params) if deletion_params else {},
            recursive=bool(recursive),
        ),
    )


def test_load_broken_config(yt_client, task_dir, archive_tree_processor):
    yt_client.set_attribute(
        task_dir,
        LOG_TARIFF_ARCHIVE_ATTR,
        {'merge_mode': 'something wrong'},
    )

    with pytest.raises(MarshmallowError):
        archive_tree_processor.load_config(task_dir)


@pytest.mark.parametrize('merge_mode', [mode for mode in MergeMode] + [None])
@pytest.mark.parametrize('merge_period', [10, None])
@pytest.mark.parametrize(
    ['deletion_mode', 'deletion_params'],
    [
        pytest.param(DeletionMode.AFTER_N_FILES, {'last_files': 10}),
        pytest.param(DeletionMode.AFTER_N_DAYS, {'last_days': 10}),
        pytest.param(None, None),
    ],
)
@pytest.mark.parametrize('recursive', [True, False, None])
def test_save_config(yt_client, task_dir, archive_tree_processor, merge_period,
                     merge_mode, deletion_mode, deletion_params, recursive):
    config = DirectoryConfiguration(
        merge_mode,
        merge_period,
        recursive,
        deletion_mode,
        deletion_params,
    )
    archive_tree_processor.save_config(task_dir, config)

    attribute = yt_client.get_attribute(
        task_dir,
        LOG_TARIFF_ARCHIVE_ATTR,
    )

    hm.assert_that(
        attribute,
        hm.has_entries(
            merge_mode=merge_mode.value if merge_mode else None,
            merge_period=merge_period,
            deletion_mode=deletion_mode.value if deletion_mode else None,
            deletion_params=deletion_params if deletion_params else {},
            recursive=recursive,
        ),
    )


def test_recursively_update_config(yt_client, task_dir, archive_tree_processor):
    rel_path = 'a/b/c/d'
    path = ypath_join(task_dir, rel_path)
    yt_client.create('map_node', path, recursive=True)
    config = DirectoryConfiguration(merge_mode=MergeMode.UNORDERED)
    archive_tree_processor.recursively_update_config(rel_path, config)

    for p in ['', 'a', 'a/b', 'a/b/c']:
        hm.assert_that(
            yt_client.get_attribute(
                ypath_join(task_dir, p) if p else task_dir,
                LOG_TARIFF_ARCHIVE_ATTR,
            ),
            hm.has_entries(
                recursive=True,
            ),
        )
    hm.assert_that(
        yt_client.get_attribute(path, LOG_TARIFF_ARCHIVE_ATTR),
        hm.has_entries(
            merge_mode=MergeMode.UNORDERED.value,
        ),
    )


def test_clean(yt_client, task_dir, archive_tree_processor):
    yt_client.set_attribute(
        task_dir,
        LOG_TARIFF_ARCHIVE_ATTR,
        {
            'merge_mode': MergeMode.UNORDERED.value,
            'deletion_mode': DeletionMode.AFTER_N_FILES.value,
            'deletion_params': {'last_files': 10},
            'recursive': True,
        },
    )
    path = ypath_join(task_dir, 'clean_me')
    yt_client.create('map_node', path)
    yt_client.set_attribute(
        path,
        LOG_TARIFF_ARCHIVE_ATTR,
        {
            'merge_mode': MergeMode.UNORDERED.value,
            'deletion_mode': DeletionMode.AFTER_N_FILES.value,
            'deletion_params': {'last_files': 10},
            'recursive': False,
        },
    )
    archive_tree_processor.clean(task_dir)
    hm.assert_that(not yt.exists('{0}/@{1}'.format(task_dir, LOG_TARIFF_ARCHIVE_ATTR)))
    hm.assert_that(yt.exists('{0}/@{1}'.format(path, LOG_TARIFF_ARCHIVE_ATTR)))


def test_clean_subtree(yt_client, task_dir, archive_tree_processor):
    yt_client.set_attribute(
        task_dir,
        LOG_TARIFF_ARCHIVE_ATTR,
        {
            'merge_mode': MergeMode.UNORDERED.value,
            'deletion_mode': DeletionMode.AFTER_N_FILES.value,
            'deletion_params': {'last_files': 10},
            'recursive': True,
        },
    )
    path = ypath_join(task_dir, 'clean_me')
    yt_client.create('map_node', path)
    yt_client.set_attribute(
        path,
        LOG_TARIFF_ARCHIVE_ATTR,
        {
            'merge_mode': MergeMode.UNORDERED.value,
            'deletion_mode': DeletionMode.AFTER_N_FILES.value,
            'deletion_params': {'last_files': 10},
            'recursive': False,
        },
    )
    archive_tree_processor.clean_subtree(task_dir)
    hm.assert_that(not yt.exists('{0}/@{1}'.format(task_dir, LOG_TARIFF_ARCHIVE_ATTR)))
    hm.assert_that(not yt.exists('{0}/@{1}'.format(path, LOG_TARIFF_ARCHIVE_ATTR)))


def test_read_subtree_configurations(yt_client, task_dir, archive_tree_processor):
    yt_client.set_attribute(
        task_dir,
        LOG_TARIFF_ARCHIVE_ATTR,
        {
            'merge_mode': MergeMode.UNORDERED.value,
            'deletion_mode': DeletionMode.AFTER_N_FILES.value,
            'deletion_params': {'last_files': 10},
            'recursive': True,
        },
    )
    dir1 = ypath_join(task_dir, 'dir1')
    yt_client.create('map_node', dir1)
    yt_client.set_attribute(
        dir1,
        LOG_TARIFF_ARCHIVE_ATTR,
        {
            'deletion_mode': DeletionMode.AFTER_N_DAYS.value,
            'deletion_params': {'last_days': 10},
            'recursive': False,
        },
    )
    dir2 = ypath_join(task_dir, 'dir2')
    yt_client.create('map_node', dir2)
    yt_client.set_attribute(
        dir2,
        LOG_TARIFF_ARCHIVE_ATTR,
        {
            'merge_mode': MergeMode.ORDERED.value,
            'recursive': False,
        },
    )

    root_conf, tree = archive_tree_processor.read_subtree_configurations(task_dir)
    hm.assert_that(
        root_conf,
        hm.has_properties(
            merge_mode=MergeMode.UNORDERED,
            deletion_mode=DeletionMode.AFTER_N_FILES,
            deletion_params={'last_files': 10},
            recursive=True,
        ),
    )
    hm.assert_that(
        tree[dir1][0],
        hm.has_properties(
            merge_mode=None,
            deletion_mode=DeletionMode.AFTER_N_DAYS,
            deletion_params={'last_days': 10},
            recursive=False,
        ),
    )
    hm.assert_that(
        tree[dir2][0],
        hm.has_properties(
            merge_mode=MergeMode.ORDERED,
            deletion_mode=None,
            deletion_params={},
            recursive=False,
        ),
    )
    hm.assert_that(tree.keys(), hm.contains_inanyorder(dir1, dir2))
    hm.assert_that(tree[dir1][1], hm.equal_to({}))
    hm.assert_that(tree[dir2][1], hm.equal_to({}))


def test_no_merge_no_delete(yt_client, task_dir, archive_tree_processor):
    table1 = ypath_join(task_dir, '2020-01-01T12:23:21')
    table2 = ypath_join(task_dir, '2020-01-01T17:43:21')
    table3 = ypath_join(task_dir, '2020-01-02T23:43:21')

    yt_client.create('table', table1, attributes={
        LOG_TARIFF_META_ATTR: {
            'log_interval': LogInterval([
                Subinterval('c1', 't1', 0, 20, 30),
                Subinterval('c1', 't1', 1, 5, 10),
                Subinterval('c1', 't1', 4, 20, 20),
            ]).to_meta(),
        },
        'schema': TEST_SCHEMA,
    })
    yt_client.write_table(
        table1,
        [
            {'a': -241, 'b': 12536, 'c': 'wow'},
            {'a': 0, 'b': 123, 'c': 'wow'},
        ],
    )

    yt_client.create('table', table2, attributes={
        LOG_TARIFF_META_ATTR: {
            'log_interval': LogInterval([
                Subinterval('c1', 't1', 0, 30, 50),
                Subinterval('c1', 't1', 1, 10, 22),
                Subinterval('c1', 't1', 4, 20, 50),
            ]).to_meta(),
        },
        'schema': TEST_SCHEMA,
    })
    yt_client.write_table(
        table2,
        [
            {'a': -1, 'b': 3, 'c': 'abc'},
            {'a': -2, 'b': 0, 'c': 'wow'},
        ],
    )

    yt_client.create('table', table3, attributes={
        LOG_TARIFF_META_ATTR: {
            'log_interval': LogInterval([
                Subinterval('c1', 't1', 0, 50, 50),
                Subinterval('c1', 't1', 1, 22, 50),
                Subinterval('c1', 't1', 4, 50, 50),
                Subinterval('c1', 't2', 0, 0, 50),
            ]).to_meta(),
        },
        'schema': TEST_SCHEMA,
    })
    yt_client.write_table(
        table3,
        [
            {'a': -666, 'b': 0, 'c': ''},
            {'a': 666, 'b': 0, 'c': '666'},
        ],
    )
    archive_tree_processor.process_root(now=datetime(year=2020, month=1, day=3, hour=12))

    hm.assert_that(yt_client.list(task_dir, absolute=True), hm.contains_inanyorder(table1, table2, table3))
    logging.info('%s', yt_client.list(task_dir, absolute=True))
    hm.assert_that(
        yt.read_table(table1),
        hm.contains_inanyorder(
            hm.has_entries({'a': -241, 'b': 12536, 'c': 'wow'}),
            hm.has_entries({'a': 0, 'b': 123, 'c': 'wow'}),
        ),
    )
    hm.assert_that(
        yt.get_attribute(table1, 'schema'),
        hm.contains(*(hm.has_entries(s) for s in TEST_SCHEMA)),
    )
    hm.assert_that(
        yt.get_attribute(table1, LOG_TARIFF_META_ATTR)['log_interval'],
        hm.equal_to(
            LogInterval([
                Subinterval('c1', 't1', 0, 20, 30),
                Subinterval('c1', 't1', 1, 5, 10),
                Subinterval('c1', 't1', 4, 20, 20),
            ]).to_meta()
        ),
    )
    hm.assert_that(
        yt.get_attribute(table1, LOG_TARIFF_ARCHIVE_ATTR, None),
        hm.equal_to(None),
    )

    hm.assert_that(
        yt.read_table(table2),
        hm.contains_inanyorder(
            hm.has_entries({'a': -1, 'b': 3, 'c': 'abc'}),
            hm.has_entries({'a': -2, 'b': 0, 'c': 'wow'}),
        ),
    )
    hm.assert_that(
        yt.get_attribute(table2, 'schema'),
        hm.contains(*(hm.has_entries(s) for s in TEST_SCHEMA)),
    )
    hm.assert_that(
        yt.get_attribute(table2, LOG_TARIFF_META_ATTR)['log_interval'],
        hm.equal_to(
            LogInterval([
                Subinterval('c1', 't1', 0, 30, 50),
                Subinterval('c1', 't1', 1, 10, 22),
                Subinterval('c1', 't1', 4, 20, 50),
            ]).to_meta()
        ),
    )
    hm.assert_that(
        yt.get_attribute(table2, LOG_TARIFF_ARCHIVE_ATTR, None),
        hm.equal_to(None),
    )

    hm.assert_that(
        yt.read_table(table3),
        hm.contains_inanyorder(
            hm.has_entries({'a': -666, 'b': 0, 'c': ''}),
            hm.has_entries({'a': 666, 'b': 0, 'c': '666'}),
        ),
    )
    hm.assert_that(
        yt.get_attribute(table3, 'schema'),
        hm.contains(*(hm.has_entries(s) for s in TEST_SCHEMA)),
    )
    hm.assert_that(
        yt.get_attribute(table3, LOG_TARIFF_META_ATTR)['log_interval'],
        hm.equal_to(
            LogInterval([
                Subinterval('c1', 't1', 0, 50, 50),
                Subinterval('c1', 't1', 1, 22, 50),
                Subinterval('c1', 't1', 4, 50, 50),
                Subinterval('c1', 't2', 0, 0, 50),
            ]).to_meta()
        ),
    )
    hm.assert_that(
        yt.get_attribute(table3, LOG_TARIFF_ARCHIVE_ATTR, None),
        hm.equal_to(None),
    )


def test_simple_unordered_merging(yt_client, task_dir, archive_tree_processor):
    yt_client.set_attribute(
        task_dir,
        LOG_TARIFF_ARCHIVE_ATTR,
        {
            'merge_mode': MergeMode.UNORDERED.value,
            'merge_period': None,  # Merge everything
            'recursive': False,
        },
    )
    table1 = ypath_join(task_dir, '2020-01-01T12:23:21')
    table2 = ypath_join(task_dir, '2020-01-01T17:43:21')
    table3 = ypath_join(task_dir, '2020-01-01T23:43:21')

    yt_client.create('table', table1, attributes={
        LOG_TARIFF_META_ATTR: {
            'log_interval': LogInterval([
                Subinterval('c1', 't1', 0, 20, 30),
                Subinterval('c1', 't1', 1, 5, 10),
                Subinterval('c1', 't1', 4, 20, 20),
            ]).to_meta(),
        },
        'schema': TEST_SCHEMA,
    })
    yt_client.write_table(
        table1,
        [
            {'a': -241, 'b': 12536, 'c': 'wow'},
            {'a': 0, 'b': 123, 'c': 'wow'},
        ],
    )

    yt_client.create('table', table2, attributes={
        LOG_TARIFF_META_ATTR: {
            'log_interval': LogInterval([
                Subinterval('c1', 't1', 0, 30, 50),
                Subinterval('c1', 't1', 1, 10, 22),
                Subinterval('c1', 't1', 4, 20, 50),
            ]).to_meta(),
        },
        'schema': TEST_SCHEMA,
    })
    yt_client.write_table(
        table2,
        [
            {'a': -1, 'b': 3, 'c': 'abc'},
            {'a': -2, 'b': 0, 'c': 'wow'},
        ],
    )

    yt_client.create('table', table3, attributes={
        LOG_TARIFF_META_ATTR: {
            'log_interval': LogInterval([
                Subinterval('c1', 't1', 0, 50, 50),
                Subinterval('c1', 't1', 1, 22, 50),
                Subinterval('c1', 't1', 4, 50, 50),
                Subinterval('c1', 't2', 0, 0, 50),
            ]).to_meta(),
        },
        'schema': TEST_SCHEMA,
    })
    yt_client.write_table(
        table3,
        [
            {'a': -666, 'b': 0, 'c': ''},
            {'a': 666, 'b': 0, 'c': '666'},
        ],
    )
    archive_tree_processor.process_root()

    hm.assert_that(yt_client.list(task_dir, absolute=True), hm.contains(table3))
    logging.info('%s', yt_client.list(task_dir, absolute=True))
    hm.assert_that(
        yt.read_table(table3),
        hm.contains_inanyorder(
            hm.has_entries({'a': -241, 'b': 12536, 'c': 'wow'}),
            hm.has_entries({'a': 0, 'b': 123, 'c': 'wow'}),
            hm.has_entries({'a': -1, 'b': 3, 'c': 'abc'}),
            hm.has_entries({'a': -2, 'b': 0, 'c': 'wow'}),
            hm.has_entries({'a': -666, 'b': 0, 'c': ''}),
            hm.has_entries({'a': 666, 'b': 0, 'c': '666'}),
        ),
    )
    hm.assert_that(
        yt.get_attribute(table3, 'schema'),
        hm.contains(*(hm.has_entries(s) for s in TEST_SCHEMA)),
    )
    hm.assert_that(
        yt.get_attribute(table3, LOG_TARIFF_META_ATTR)['log_interval'],
        hm.equal_to(
            LogInterval([
                Subinterval('c1', 't1', 0, 20, 50),
                Subinterval('c1', 't1', 1, 5, 50),
                Subinterval('c1', 't1', 4, 20, 50),
                Subinterval('c1', 't2', 0, 0, 50),
            ]).to_meta()
        ),
    )
    logging.info('%s', yt.get_attribute(table3, LOG_TARIFF_ARCHIVE_ATTR))
    hm.assert_that(
        yt.get_attribute(table3, LOG_TARIFF_ARCHIVE_ATTR),
        hm.has_entries({
            'merged': True,
            'merged_tables': {
                '2020-01-01T12:23:21': {
                    'log_interval': LogInterval(
                        [Subinterval('c1', 't1', 0, 20, 30),
                         Subinterval('c1', 't1', 1, 5, 10),
                         Subinterval('c1', 't1', 4, 20, 20)]
                    ).to_meta()
                },
                '2020-01-01T17:43:21': {
                    'log_interval': LogInterval(
                        [Subinterval('c1', 't1', 0, 30, 50),
                         Subinterval('c1', 't1', 1, 10, 22),
                         Subinterval('c1', 't1', 4, 20, 50)]
                    ).to_meta()
                },
                '2020-01-01T23:43:21': {
                    'log_interval': LogInterval(
                        [Subinterval('c1', 't1', 0, 50, 50),
                         Subinterval('c1', 't1', 1, 22, 50),
                         Subinterval('c1', 't1', 4, 50, 50),
                         Subinterval('c1', 't2', 0, 0, 50)]
                    ).to_meta()
                }
            }
        }),
    )


def test_simple_merging_table_name_formats(yt_client, task_dir, archive_tree_processor):
    yt_client.set_attribute(
        task_dir,
        LOG_TARIFF_ARCHIVE_ATTR,
        {
            'merge_mode': MergeMode.UNORDERED.value,
            'merge_period': 2,
            'single_table_merge_mode': True,
            'single_table_merge_period': 1,
            'recursive': False,
        },
    )
    table1 = ypath_join(task_dir, '2020-01-01T12:23:21')
    table2 = ypath_join(task_dir, '2020-01-01T17:43:21')
    table3 = ypath_join(task_dir, '2020-01-01')
    table4 = ypath_join(task_dir, '2020-01-01-unsupported-format')

    yt_client.create('table', table1, attributes={
        LOG_TARIFF_META_ATTR: {
            'log_interval': LogInterval([
                Subinterval('c1', 't1', 0, 20, 30),
                Subinterval('c1', 't1', 1, 5, 10),
                Subinterval('c1', 't1', 4, 20, 20),
            ]).to_meta(),
        },
        'schema': TEST_SCHEMA,
    })
    yt_client.write_table(
        table1,
        [
            {'a': -241, 'b': 12536, 'c': 'wow'},
            {'a': 0, 'b': 123, 'c': 'wow'},
        ],
    )

    yt_client.create('table', table2, attributes={
        LOG_TARIFF_META_ATTR: {
            'log_interval': LogInterval([
                Subinterval('c1', 't1', 0, 30, 50),
                Subinterval('c1', 't1', 1, 10, 22),
                Subinterval('c1', 't1', 4, 20, 50),
            ]).to_meta(),
        },
        'schema': TEST_SCHEMA,
    })
    yt_client.write_table(
        table2,
        [
            {'a': -1, 'b': 3, 'c': 'abc'},
            {'a': -2, 'b': 0, 'c': 'wow'},
        ],
    )

    yt_client.create('table', table3, attributes={
        LOG_TARIFF_META_ATTR: {
            'log_interval': LogInterval([
                Subinterval('c1', 't1', 0, 50, 50),
                Subinterval('c1', 't1', 1, 22, 50),
                Subinterval('c1', 't1', 4, 50, 50),
                Subinterval('c1', 't2', 0, 0, 50),
            ]).to_meta(),
        },
        'schema': TEST_SCHEMA,
    })
    yt_client.write_table(
        table3,
        [
            {'a': -666, 'b': 0, 'c': ''},
            {'a': 666, 'b': 0, 'c': '666'},
        ],
    )
    yt_client.create('table', table4, attributes={
        LOG_TARIFF_META_ATTR: {
            'log_interval': LogInterval([
                Subinterval('c1', 't1', 0, 50, 55),
                Subinterval('c1', 't1', 1, 50, 55),
                Subinterval('c1', 't1', 4, 50, 55),
                Subinterval('c1', 't2', 0, 50, 55),
            ]).to_meta(),
        },
        'schema': TEST_SCHEMA,
    })
    yt_client.write_table(
        table4,
        [
            {'a': -333, 'b': 0, 'c': ''},
            {'a': 333, 'b': 0, 'c': '666'},
        ],
    )
    archive_tree_processor.process_root(now=datetime(year=2020, month=1, day=3, hour=12))

    hm.assert_that(yt_client.list(task_dir, absolute=True), hm.contains_inanyorder(table2, table3, table4))
    logging.info('%s', yt_client.list(task_dir, absolute=True))
    hm.assert_that(
        yt.read_table(table2),
        hm.contains_inanyorder(
            hm.has_entries({'a': -241, 'b': 12536, 'c': 'wow'}),
            hm.has_entries({'a': 0, 'b': 123, 'c': 'wow'}),
            hm.has_entries({'a': -1, 'b': 3, 'c': 'abc'}),
            hm.has_entries({'a': -2, 'b': 0, 'c': 'wow'}),
        ),
    )
    hm.assert_that(
        yt.get_attribute(table2, 'schema'),
        hm.contains(*(hm.has_entries(s) for s in TEST_SCHEMA)),
    )
    hm.assert_that(
        yt.get_attribute(table2, LOG_TARIFF_META_ATTR)['log_interval'],
        hm.equal_to(
            LogInterval([
                Subinterval('c1', 't1', 0, 20, 50),
                Subinterval('c1', 't1', 1, 5, 22),
                Subinterval('c1', 't1', 4, 20, 50),
            ]).to_meta()
        ),
    )
    logging.info('%s', yt.get_attribute(table2, LOG_TARIFF_ARCHIVE_ATTR))
    hm.assert_that(
        yt.get_attribute(table2, LOG_TARIFF_ARCHIVE_ATTR),
        hm.has_entries({
            'merged': True,
            'merged_tables': {
                '2020-01-01T12:23:21': {
                    'log_interval': LogInterval(
                        [Subinterval('c1', 't1', 0, 20, 30),
                         Subinterval('c1', 't1', 1, 5, 10),
                         Subinterval('c1', 't1', 4, 20, 20)]
                    ).to_meta()
                },
                '2020-01-01T17:43:21': {
                    'log_interval': LogInterval(
                        [Subinterval('c1', 't1', 0, 30, 50),
                         Subinterval('c1', 't1', 1, 10, 22),
                         Subinterval('c1', 't1', 4, 20, 50)]
                    ).to_meta()
                }
            }
        }),
    )

    hm.assert_that(
        yt.read_table(table3),
        hm.contains_inanyorder(
            hm.has_entries({'a': -666, 'b': 0, 'c': ''}),
            hm.has_entries({'a': 666, 'b': 0, 'c': '666'}),
        ),
    )
    hm.assert_that(
        yt.get_attribute(table3, 'schema'),
        hm.contains(*(hm.has_entries(s) for s in TEST_SCHEMA)),
    )
    hm.assert_that(
        yt.get_attribute(table3, LOG_TARIFF_META_ATTR)['log_interval'],
        hm.equal_to(
            LogInterval([
                Subinterval('c1', 't1', 0, 50, 50),
                Subinterval('c1', 't1', 1, 22, 50),
                Subinterval('c1', 't1', 4, 50, 50),
                Subinterval('c1', 't2', 0, 0, 50),
            ]).to_meta()
        ),
    )
    logging.info('%s', yt.get_attribute(table3, LOG_TARIFF_ARCHIVE_ATTR))
    hm.assert_that(
        yt.get_attribute(table3, LOG_TARIFF_ARCHIVE_ATTR),
        hm.has_entries({
            'merged': True,
            'merged_tables': {
                '2020-01-01': {
                    'log_interval': LogInterval([
                Subinterval('c1', 't1', 0, 50, 50),
                Subinterval('c1', 't1', 1, 22, 50),
                Subinterval('c1', 't1', 4, 50, 50),
                Subinterval('c1', 't2', 0, 0, 50),
            ]).to_meta()
                }
            }
        }),
    )

    hm.assert_that(
        yt.read_table(table4),
        hm.contains_inanyorder(
            hm.has_entries({'a': -333, 'b': 0, 'c': ''}),
            hm.has_entries({'a': 333, 'b': 0, 'c': '666'}),
        ),
    )
    hm.assert_that(
        yt.get_attribute(table4, 'schema'),
        hm.contains(*(hm.has_entries(s) for s in TEST_SCHEMA)),
    )
    hm.assert_that(
        yt.get_attribute(table4, LOG_TARIFF_META_ATTR)['log_interval'],
        hm.equal_to(
            LogInterval([
                Subinterval('c1', 't1', 0, 50, 55),
                Subinterval('c1', 't1', 1, 50, 55),
                Subinterval('c1', 't1', 4, 50, 55),
                Subinterval('c1', 't2', 0, 50, 55),
            ]).to_meta()
        ),
    )
    logging.info('%s', yt.get_attribute(table4, LOG_TARIFF_ARCHIVE_ATTR, default='not_merged'))
    hm.assert_that(
        yt.get_attribute(table4, LOG_TARIFF_ARCHIVE_ATTR, default='not_merged'),
        hm.equal_to('not_merged'),
    )


def test_sorted_merging(yt_client, task_dir, archive_tree_processor):
    yt_client.set_attribute(
        task_dir,
        LOG_TARIFF_ARCHIVE_ATTR,
        {
            'merge_mode': MergeMode.UNORDERED.value,
            'merge_period': None,  # Merge everything
            'recursive': False,
        },
    )
    table1 = ypath_join(task_dir, '2020-01-01T12:23:21')
    table2 = ypath_join(task_dir, '2020-01-01T17:43:21')
    table3 = ypath_join(task_dir, '2020-01-01T23:43:21')

    yt_client.create('table', table1, attributes={
        LOG_TARIFF_META_ATTR: {
            'log_interval': LogInterval([
                Subinterval('c1', 't1', 0, 20, 30),
                Subinterval('c1', 't1', 1, 5, 10),
                Subinterval('c1', 't1', 4, 20, 20),
            ]).to_meta(),
        },
        'schema': TEST_SCHEMA,
    })
    yt_client.write_table(
        table1,
        [
            {'a': 0, 'b': 123, 'c': 'wow'},
            {'a': 241, 'b': 12536, 'c': 'wow'},
        ],
    )

    yt_client.create('table', table2, attributes={
        LOG_TARIFF_META_ATTR: {
            'log_interval': LogInterval([
                Subinterval('c1', 't1', 0, 30, 50),
                Subinterval('c1', 't1', 1, 10, 22),
                Subinterval('c1', 't1', 4, 20, 50),
            ]).to_meta(),
        },
        'schema': TEST_SCHEMA,
    })
    yt_client.write_table(
        table2,
        [
            {'a': 1, 'b': 0, 'c': 'wow'},
            {'a': 2, 'b': 3, 'c': 'abc'},
        ],
    )

    yt_client.create('table', table3, attributes={
        LOG_TARIFF_META_ATTR: {
            'log_interval': LogInterval([
                Subinterval('c1', 't1', 0, 50, 50),
                Subinterval('c1', 't1', 1, 22, 50),
                Subinterval('c1', 't1', 4, 50, 50),
                Subinterval('c1', 't2', 0, 0, 50),
            ]).to_meta(),
        },
        'schema': TEST_SCHEMA,
    })
    yt_client.write_table(
        table3,
        [
            {'a': 333, 'b': 0, 'c': ''},
            {'a': 666, 'b': 0, 'c': '666'},
        ],
    )

    yt_client.run_sort(table1, sort_by=['a', ])
    yt_client.run_sort(table2, sort_by=['a', ])
    yt_client.run_sort(table3, sort_by=['a', ])

    archive_tree_processor.process_root()

    hm.assert_that(yt_client.list(task_dir, absolute=True), hm.contains(table3))
    logging.info('%s', yt_client.list(task_dir, absolute=True))
    hm.assert_that(
        yt.read_table(table3),
        hm.contains_inanyorder(
            hm.has_entries({'a': 0, 'b': 123, 'c': 'wow'}),
            hm.has_entries({'a': 1, 'b': 0, 'c': 'wow'}),
            hm.has_entries({'a': 2, 'b': 3, 'c': 'abc'}),
            hm.has_entries({'a': 241, 'b': 12536, 'c': 'wow'}),
            hm.has_entries({'a': 333, 'b': 0, 'c': ''}),
            hm.has_entries({'a': 666, 'b': 0, 'c': '666'}),
        ),
    )
    hm.assert_that(
        yt.get_attribute(table3, 'schema'),
        hm.contains(
            hm.has_entries({'name': 'a', 'type': 'int64', 'sort_order': 'ascending'}),
            hm.has_entries({'name': 'b', 'type': 'uint64'}),
            hm.has_entries({'name': 'c', 'type': 'string'}),
        ),
    )
    hm.assert_that(
        yt.get_attribute(table3, 'sorted_by'),
        hm.equal_to(['a', ]),
    )
    hm.assert_that(
        yt.get_attribute(table3, LOG_TARIFF_META_ATTR)['log_interval'],
        hm.equal_to(
            LogInterval([
                Subinterval('c1', 't1', 0, 20, 50),
                Subinterval('c1', 't1', 1, 5, 50),
                Subinterval('c1', 't1', 4, 20, 50),
                Subinterval('c1', 't2', 0, 0, 50),
            ]).to_meta()
        ),
    )
    logging.info('%s', yt.get_attribute(table3, LOG_TARIFF_ARCHIVE_ATTR))
    hm.assert_that(
        yt.get_attribute(table3, LOG_TARIFF_ARCHIVE_ATTR),
        hm.has_entries({
            'merged': True,
            'merged_tables': {
                '2020-01-01T12:23:21': {
                    'log_interval': LogInterval(
                        [Subinterval('c1', 't1', 0, 20, 30),
                         Subinterval('c1', 't1', 1, 5, 10),
                         Subinterval('c1', 't1', 4, 20, 20)]
                    ).to_meta()
                },
                '2020-01-01T17:43:21': {
                    'log_interval': LogInterval(
                        [Subinterval('c1', 't1', 0, 30, 50),
                         Subinterval('c1', 't1', 1, 10, 22),
                         Subinterval('c1', 't1', 4, 20, 50)]
                    ).to_meta()
                },
                '2020-01-01T23:43:21': {
                    'log_interval': LogInterval(
                        [Subinterval('c1', 't1', 0, 50, 50),
                         Subinterval('c1', 't1', 1, 22, 50),
                         Subinterval('c1', 't1', 4, 50, 50),
                         Subinterval('c1', 't2', 0, 0, 50)]
                    ).to_meta()
                }
            }
        }),
    )


def test_simple_single_table_merging(yt_client, task_dir, archive_tree_processor):
    yt_client.set_attribute(
        task_dir,
        LOG_TARIFF_ARCHIVE_ATTR,
        {
            'merge_mode': MergeMode.UNORDERED.value,
            'merge_period': 1000,
            'single_table_merge_mode': True,
            'single_table_merge_period': None,   # Merge everything
            'recursive': False,
        },
    )
    table1 = ypath_join(task_dir, '2020-01-01T12:23:21')
    table2 = ypath_join(task_dir, '2020-01-01T17:43:21')

    yt_client.create('table', table1, attributes={
        LOG_TARIFF_META_ATTR: {
            'log_interval': LogInterval([
                Subinterval('c1', 't1', 0, 20, 30),
                Subinterval('c1', 't1', 1, 5, 10),
                Subinterval('c1', 't1', 4, 20, 20),
            ]).to_meta(),
        },
        'schema': TEST_SCHEMA,
    })
    yt_client.write_table(
        table1,
        [
            {'a': -241, 'b': 12536, 'c': 'wow'},
            {'a': 0, 'b': 123, 'c': 'wow'},
        ],
    )

    yt_client.create('table', table2, attributes={
        LOG_TARIFF_META_ATTR: {
            'log_interval': LogInterval([
                Subinterval('c1', 't1', 0, 30, 50),
                Subinterval('c1', 't1', 1, 10, 22),
                Subinterval('c1', 't1', 4, 20, 50),
            ]).to_meta(),
        },
        'schema': TEST_SCHEMA,
    })
    yt_client.write_table(
        table2,
        [
            {'a': -1, 'b': 3, 'c': 'abc'},
            {'a': -2, 'b': 0, 'c': 'wow'},
        ],
    )

    archive_tree_processor.process_root()

    hm.assert_that(yt_client.list(task_dir, absolute=True), hm.contains_inanyorder(table1, table2))
    logging.info('%s', yt_client.list(task_dir, absolute=True))
    hm.assert_that(
        yt.read_table(table1),
        hm.contains_inanyorder(
            hm.has_entries({'a': -241, 'b': 12536, 'c': 'wow'}),
            hm.has_entries({'a': 0, 'b': 123, 'c': 'wow'}),
        ),
    )
    hm.assert_that(
        yt.get_attribute(table1, 'schema'),
        hm.contains(*(hm.has_entries(s) for s in TEST_SCHEMA)),
    )
    hm.assert_that(
        yt.get_attribute(table1, LOG_TARIFF_ARCHIVE_ATTR),
        hm.has_entries(
            {
                'merged': True,
                'merged_tables': {
                    '2020-01-01T12:23:21': {'log_interval': LogInterval([
                Subinterval('c1', 't1', 0, 20, 30),
                Subinterval('c1', 't1', 1, 5, 10),
                Subinterval('c1', 't1', 4, 20, 20),
            ]).to_meta()},
                }
            }
        ),
    )
    hm.assert_that(
        yt.get_attribute(table1, LOG_TARIFF_META_ATTR)['log_interval'],
        hm.equal_to(
            LogInterval([
                Subinterval('c1', 't1', 0, 20, 30),
                Subinterval('c1', 't1', 1, 5, 10),
                Subinterval('c1', 't1', 4, 20, 20),
            ]).to_meta()
        ),
    )

    hm.assert_that(
        yt.read_table(table2),
        hm.contains_inanyorder(
            hm.has_entries({'a': -1, 'b': 3, 'c': 'abc'}),
            hm.has_entries({'a': -2, 'b': 0, 'c': 'wow'}),
        ),
    )
    hm.assert_that(
        yt.get_attribute(table2, 'schema'),
        hm.contains(*(hm.has_entries(s) for s in TEST_SCHEMA)),
    )
    hm.assert_that(
        yt.get_attribute(table2, LOG_TARIFF_ARCHIVE_ATTR),
        hm.has_entries(
            {
                'merged': True,
                'merged_tables': {
                    '2020-01-01T17:43:21': {'log_interval': LogInterval([
                Subinterval('c1', 't1', 0, 30, 50),
                Subinterval('c1', 't1', 1, 10, 22),
                Subinterval('c1', 't1', 4, 20, 50),
            ]).to_meta()},
                }
            }
        ),
    )
    hm.assert_that(
        yt.get_attribute(table2, LOG_TARIFF_META_ATTR)['log_interval'],
        hm.equal_to(
            LogInterval([
                Subinterval('c1', 't1', 0, 30, 50),
                Subinterval('c1', 't1', 1, 10, 22),
                Subinterval('c1', 't1', 4, 20, 50),
            ]).to_meta()
        ),
    )


def test_simple_merging_and_single_table_merging(yt_client, task_dir, archive_tree_processor):
    yt_client.set_attribute(
        task_dir,
        LOG_TARIFF_ARCHIVE_ATTR,
        {
            'merge_mode': MergeMode.UNORDERED.value,
            'merge_period': 2,
            'single_table_merge_mode': True,
            'single_table_merge_period': 1,
            'recursive': False,
        },
    )
    table1 = ypath_join(task_dir, '2020-01-01T12:23:21')
    table2 = ypath_join(task_dir, '2020-01-01T17:43:21')
    table3 = ypath_join(task_dir, '2020-01-02T23:43:21')

    yt_client.create('table', table1, attributes={
        LOG_TARIFF_META_ATTR: {
            'log_interval': LogInterval([
                Subinterval('c1', 't1', 0, 20, 30),
                Subinterval('c1', 't1', 1, 5, 10),
                Subinterval('c1', 't1', 4, 20, 20),
            ]).to_meta(),
        },
        'schema': TEST_SCHEMA,
    })
    yt_client.write_table(
        table1,
        [
            {'a': -241, 'b': 12536, 'c': 'wow'},
            {'a': 0, 'b': 123, 'c': 'wow'},
        ],
    )

    yt_client.create('table', table2, attributes={
        LOG_TARIFF_META_ATTR: {
            'log_interval': LogInterval([
                Subinterval('c1', 't1', 0, 30, 50),
                Subinterval('c1', 't1', 1, 10, 22),
                Subinterval('c1', 't1', 4, 20, 50),
            ]).to_meta(),
        },
        'schema': TEST_SCHEMA,
    })
    yt_client.write_table(
        table2,
        [
            {'a': -1, 'b': 3, 'c': 'abc'},
            {'a': -2, 'b': 0, 'c': 'wow'},
        ],
    )

    yt_client.create('table', table3, attributes={
        LOG_TARIFF_META_ATTR: {
            'log_interval': LogInterval([
                Subinterval('c1', 't1', 0, 50, 50),
                Subinterval('c1', 't1', 1, 22, 50),
                Subinterval('c1', 't1', 4, 50, 50),
                Subinterval('c1', 't2', 0, 0, 50),
            ]).to_meta(),
        },
        'schema': TEST_SCHEMA,
    })
    yt_client.write_table(
        table3,
        [
            {'a': -666, 'b': 0, 'c': ''},
            {'a': 666, 'b': 0, 'c': '666'},
        ],
    )
    archive_tree_processor.process_root(now=datetime(year=2020, month=1, day=3, hour=12))

    hm.assert_that(yt_client.list(task_dir, absolute=True), hm.contains_inanyorder(table2, table3))
    logging.info('%s', yt_client.list(task_dir, absolute=True))
    hm.assert_that(
        yt.read_table(table2),
        hm.contains_inanyorder(
            hm.has_entries({'a': -241, 'b': 12536, 'c': 'wow'}),
            hm.has_entries({'a': 0, 'b': 123, 'c': 'wow'}),
            hm.has_entries({'a': -1, 'b': 3, 'c': 'abc'}),
            hm.has_entries({'a': -2, 'b': 0, 'c': 'wow'}),
        ),
    )
    hm.assert_that(
        yt.get_attribute(table2, 'schema'),
        hm.contains(*(hm.has_entries(s) for s in TEST_SCHEMA)),
    )
    hm.assert_that(
        yt.get_attribute(table2, LOG_TARIFF_META_ATTR)['log_interval'],
        hm.equal_to(
            LogInterval([
                Subinterval('c1', 't1', 0, 20, 50),
                Subinterval('c1', 't1', 1, 5, 22),
                Subinterval('c1', 't1', 4, 20, 50),
            ]).to_meta()
        ),
    )
    logging.info('%s', yt.get_attribute(table2, LOG_TARIFF_ARCHIVE_ATTR))
    hm.assert_that(
        yt.get_attribute(table2, LOG_TARIFF_ARCHIVE_ATTR),
        hm.has_entries({
            'merged': True,
            'merged_tables': {
                '2020-01-01T12:23:21': {
                    'log_interval': LogInterval(
                        [Subinterval('c1', 't1', 0, 20, 30),
                         Subinterval('c1', 't1', 1, 5, 10),
                         Subinterval('c1', 't1', 4, 20, 20)]
                    ).to_meta()
                },
                '2020-01-01T17:43:21': {
                    'log_interval': LogInterval(
                        [Subinterval('c1', 't1', 0, 30, 50),
                         Subinterval('c1', 't1', 1, 10, 22),
                         Subinterval('c1', 't1', 4, 20, 50)]
                    ).to_meta()
                }
            }
        }),
    )

    hm.assert_that(
        yt.read_table(table3),
        hm.contains_inanyorder(
            hm.has_entries({'a': -666, 'b': 0, 'c': ''}),
            hm.has_entries({'a': 666, 'b': 0, 'c': '666'}),
        ),
    )
    hm.assert_that(
        yt.get_attribute(table3, 'schema'),
        hm.contains(*(hm.has_entries(s) for s in TEST_SCHEMA)),
    )
    hm.assert_that(
        yt.get_attribute(table3, LOG_TARIFF_META_ATTR)['log_interval'],
        hm.equal_to(
            LogInterval([
                Subinterval('c1', 't1', 0, 50, 50),
                Subinterval('c1', 't1', 1, 22, 50),
                Subinterval('c1', 't1', 4, 50, 50),
                Subinterval('c1', 't2', 0, 0, 50),
            ]).to_meta()
        ),
    )
    logging.info('%s', yt.get_attribute(table3, LOG_TARIFF_ARCHIVE_ATTR))
    hm.assert_that(
        yt.get_attribute(table3, LOG_TARIFF_ARCHIVE_ATTR),
        hm.has_entries({
            'merged': True,
            'merged_tables': {
                '2020-01-02T23:43:21': {
                    'log_interval': LogInterval([
                Subinterval('c1', 't1', 0, 50, 50),
                Subinterval('c1', 't1', 1, 22, 50),
                Subinterval('c1', 't1', 4, 50, 50),
                Subinterval('c1', 't2', 0, 0, 50),
            ]).to_meta()
                }
            }
        }),
    )


@pytest.mark.parametrize(
    ['deletion_mode', 'deletion_params'],
    [
        pytest.param(DeletionMode.AFTER_N_FILES.value, {'last_files': 2}),
        pytest.param(DeletionMode.AFTER_N_DAYS.value, {'last_days': 2}),
        pytest.param(None, None),
    ],
)
def test_simple_deletion(yt_client, task_dir, archive_tree_processor, deletion_mode, deletion_params):
    attrs = {
        'deletion_mode': deletion_mode,
        'recursive': False,
    }
    if deletion_params is not None:
        attrs['deletion_params'] = deletion_params
    yt_client.set_attribute(
        task_dir,
        LOG_TARIFF_ARCHIVE_ATTR,
        attrs,
    )
    table1 = ypath_join(task_dir, '2020-01-06T12:23:21')
    table2 = ypath_join(task_dir, '2020-01-05')
    table3 = ypath_join(task_dir, '2020-01-04T23:43:21')
    table4 = ypath_join(task_dir, '2020-01-03T23:43:21')
    table5 = ypath_join(task_dir, '2020-01-02T23:43:21-unsupported-format')

    for table_path in [table1, table2, table3, table4, table5]:
        yt_client.create('table', table_path, attributes={
            LOG_TARIFF_META_ATTR: {
                'log_interval': LogInterval([
                    Subinterval('c1', 't1', 0, 20, 30),
                    Subinterval('c1', 't1', 1, 5, 10),
                    Subinterval('c1', 't1', 4, 20, 20),
                ]).to_meta(),
            },
            'schema': TEST_SCHEMA,
        })
    archive_tree_processor.process_root(now=datetime(year=2020, month=1, day=6, hour=12))
    if deletion_mode == DeletionMode.AFTER_N_FILES.value:
        hm.assert_that(yt_client.list(task_dir, absolute=True), hm.contains_inanyorder(table1, table2))
    elif deletion_mode == DeletionMode.AFTER_N_DAYS.value:
        hm.assert_that(yt_client.list(task_dir, absolute=True), hm.contains_inanyorder(table1, table2, table5))
    else:
        hm.assert_that(
            yt_client.list(task_dir, absolute=True),
            hm.contains_inanyorder(table1, table2, table3, table4, table5)
        )


@pytest.mark.parametrize(
    ['deletion_mode', 'deletion_params'],
    [
        pytest.param(DeletionMode.AFTER_N_FILES.value, {'last_files': 2}),
        pytest.param(DeletionMode.AFTER_N_DAYS.value, {'last_days': 2}),
    ],
)
def test_simple_deletion_and_single_table_merging(yt_client, task_dir, archive_tree_processor, deletion_mode,
                                                  deletion_params):
    attrs = {
        'deletion_mode': deletion_mode,
        'recursive': False,
        'single_table_merge_mode': True,
    }
    if deletion_params is not None:
        attrs['deletion_params'] = deletion_params
    yt_client.set_attribute(
        task_dir,
        LOG_TARIFF_ARCHIVE_ATTR,
        attrs,
    )

    table1 = ypath_join(task_dir, '2020-01-06T12:23:21')
    table2 = ypath_join(task_dir, '2020-01-05T17:43:21')
    table3 = ypath_join(task_dir, '2020-01-04T23:43:21')
    table4 = ypath_join(task_dir, '2020-01-03T23:43:21')

    for table_path in [table1, table2, table3, table4]:
        yt_client.create('table', table_path, attributes={
            LOG_TARIFF_META_ATTR: {
                'log_interval': LogInterval([
                    Subinterval('c1', 't1', 0, 20, 30),
                    Subinterval('c1', 't1', 1, 5, 10),
                    Subinterval('c1', 't1', 4, 20, 20),
                ]).to_meta(),
            },
            'schema': TEST_SCHEMA,
        })
    archive_tree_processor.process_root(now=datetime(year=2020, month=1, day=6, hour=12))

    hm.assert_that(yt_client.list(task_dir, absolute=True), hm.contains_inanyorder(table1, table2))

    hm.assert_that(
        yt.get_attribute(table1, LOG_TARIFF_ARCHIVE_ATTR),
        hm.has_entries({
            'merged': True,
            'merged_tables': {
                '2020-01-06T12:23:21': {
                    'log_interval': LogInterval([
                        Subinterval('c1', 't1', 0, 20, 30),
                        Subinterval('c1', 't1', 1, 5, 10),
                        Subinterval('c1', 't1', 4, 20, 20),
                    ]).to_meta()
                }
            }
        }),
    )

    hm.assert_that(
        yt.get_attribute(table2, LOG_TARIFF_ARCHIVE_ATTR),
        hm.has_entries({
            'merged': True,
            'merged_tables': {
                '2020-01-05T17:43:21': {
                    'log_interval': LogInterval([
                        Subinterval('c1', 't1', 0, 20, 30),
                        Subinterval('c1', 't1', 1, 5, 10),
                        Subinterval('c1', 't1', 4, 20, 20),
                    ]).to_meta()
                }
            }
        }),
    )


def test_recursive_merging(yt_client, task_dir, archive_tree_processor):
    yt_client.set_attribute(
        task_dir,
        LOG_TARIFF_ARCHIVE_ATTR,
        {
            'recursive': True,
        },
    )
    dir1 = ypath_join(task_dir, 'dir1')
    yt_client.create(
        'map_node',
        dir1,
        attributes={
            LOG_TARIFF_ARCHIVE_ATTR: {
                'merge_mode': MergeMode.UNORDERED.value,
                'merge_period': 1,
            },
        },
    )
    table1 = ypath_join(dir1, '2020-01-04T12:23:21')
    table2 = ypath_join(dir1, '2020-01-04T17:43:21')
    table3 = ypath_join(dir1, '2020-01-04T23:43:21')
    table4 = ypath_join(dir1, '2020-01-05T23:43:21')

    for i, table_path in enumerate([table4, table1, table2, table3]):
        yt_client.create(
            'table',
            table_path,
            attributes={
                LOG_TARIFF_META_ATTR: {
                    'log_interval': LogInterval([
                        Subinterval('c1', 't1', 0, i * 10, i * 10 + 10),
                    ]).to_meta(),
                },
                'schema': TEST_SCHEMA,
            },
        )
        yt_client.write_table(
            table_path,
            [
                {'a': 0, 'b': 2, 'c': 'a'},
                {'a': 1, 'b': 3, 'c': 'b'},
            ],
        )

    archive_tree_processor.process_root(now=datetime(year=2020, month=1, day=5, hour=12))

    hm.assert_that(yt_client.list(dir1, absolute=True), hm.contains_inanyorder(table3, table4))
    hm.assert_that(
        yt.read_table(table3),
        hm.contains_inanyorder(
            hm.has_entries({'a': 0, 'b': 2, 'c': 'a'}),
            hm.has_entries({'a': 1, 'b': 3, 'c': 'b'}),
            hm.has_entries({'a': 0, 'b': 2, 'c': 'a'}),
            hm.has_entries({'a': 1, 'b': 3, 'c': 'b'}),
            hm.has_entries({'a': 0, 'b': 2, 'c': 'a'}),
            hm.has_entries({'a': 1, 'b': 3, 'c': 'b'}),
        ),
    )
    hm.assert_that(
        yt.get_attribute(table3, 'schema'),
        hm.contains(*(hm.has_entries(s) for s in TEST_SCHEMA)),
    )
    hm.assert_that(
        yt.get_attribute(table3, LOG_TARIFF_META_ATTR)['log_interval'],
        hm.equal_to(
            LogInterval([
                Subinterval('c1', 't1', 0, 10, 40),
            ]).to_meta()
        ),
    )

    hm.assert_that(
        yt.read_table(table4),
        hm.contains_inanyorder(
            hm.has_entries({'a': 0, 'b': 2, 'c': 'a'}),
            hm.has_entries({'a': 1, 'b': 3, 'c': 'b'}),
        ),
    )
    hm.assert_that(
        yt.get_attribute(table4, 'schema'),
        hm.contains(*(hm.has_entries(s) for s in TEST_SCHEMA)),
    )
    hm.assert_that(
        yt.get_attribute(table4, LOG_TARIFF_META_ATTR)['log_interval'],
        hm.equal_to(
            LogInterval([
                Subinterval('c1', 't1', 0, 0, 10),
            ]).to_meta()
        ),
    )


def test_merging_with_deletion(yt_client, task_dir, archive_tree_processor):
    yt_client.set_attribute(
        task_dir,
        LOG_TARIFF_ARCHIVE_ATTR,
        {
            'merge_mode': MergeMode.UNORDERED.value,
            'merge_period': 1,
            'deletion_mode': DeletionMode.AFTER_N_DAYS.value,
            'deletion_params': {'last_days': 2},
            'recursive': False,
        },
    )
    table1 = ypath_join(task_dir, '2020-01-05T12:23:21')
    table2 = ypath_join(task_dir, '2020-01-05T17:43:21')
    table3 = ypath_join(task_dir, '2020-01-04T10:43:21')
    table4 = ypath_join(task_dir, '2020-01-04T23:43:21')
    table5 = ypath_join(task_dir, '2020-01-03T23:43:21')

    for i, table_path in enumerate([table5, table3, table4, table1, table2]):
        yt_client.create(
            'table',
            table_path,
            attributes={
                LOG_TARIFF_META_ATTR: {
                    'log_interval': LogInterval([
                        Subinterval('c1', 't1', 0, i * 10, i * 10 + 10),
                    ]).to_meta(),
                },
                'schema': TEST_SCHEMA,
            },
        )
        yt_client.write_table(
            table_path,
            [
                {'a': 0, 'b': 2, 'c': table_path},
                {'a': 1, 'b': 3, 'c': table_path},
            ],
        )

    archive_tree_processor.process_root(now=datetime(year=2020, month=1, day=5, hour=12))

    hm.assert_that(yt_client.list(task_dir, absolute=True), hm.contains_inanyorder(table1, table2, table4))
    hm.assert_that(
        yt.read_table(table4),
        hm.contains_inanyorder(
            hm.has_entries({'a': 0, 'b': 2, 'c': table3}),
            hm.has_entries({'a': 1, 'b': 3, 'c': table3}),
            hm.has_entries({'a': 0, 'b': 2, 'c': table4}),
            hm.has_entries({'a': 1, 'b': 3, 'c': table4}),
        ),
    )
    hm.assert_that(
        yt.get_attribute(table4, 'schema'),
        hm.contains(*(hm.has_entries(s) for s in TEST_SCHEMA)),
    )
    hm.assert_that(
        yt.get_attribute(table4, LOG_TARIFF_META_ATTR)['log_interval'],
        hm.equal_to(
            LogInterval([
                Subinterval('c1', 't1', 0, 10, 30)
            ]).to_meta()
        ),
    )

    for table, num in [(table1, 3), (table2, 4)]:
        hm.assert_that(
            yt.read_table(table),
            hm.contains_inanyorder(
                hm.has_entries({'a': 0, 'b': 2, 'c': table}),
                hm.has_entries({'a': 1, 'b': 3, 'c': table}),
            ),
        )
        hm.assert_that(
            yt.get_attribute(table, 'schema'),
            hm.contains(*(hm.has_entries(s) for s in TEST_SCHEMA)),
        )
        hm.assert_that(
            yt.get_attribute(table, LOG_TARIFF_META_ATTR)['log_interval'],
            hm.equal_to(
                LogInterval([
                    Subinterval('c1', 't1', 0, num * 10, num * 10 + 10),
                ]).to_meta()
            ),
        )


def test_merging_merged_tables(yt_client, task_dir, archive_tree_processor):
    yt_client.set_attribute(
        task_dir,
        LOG_TARIFF_ARCHIVE_ATTR,
        {
            'merge_mode': MergeMode.UNORDERED.value,
            'merge_period': None,
            'recursive': False,
        },
    )
    table1 = ypath_join(task_dir, '2020-01-05T12:23:21')
    table2 = ypath_join(task_dir, '2020-01-05T17:43:21')

    for i, table_path in enumerate([table1, table2]):
        yt_client.create(
            'table',
            table_path,
            attributes={
                LOG_TARIFF_META_ATTR: {
                    'log_interval': LogInterval([
                        Subinterval('c1', 't1', 0, i * 10, i * 10 + 10),
                    ]).to_meta(),
                },
                'schema': TEST_SCHEMA,
            },
        )
        yt_client.write_table(
            table_path,
            [
                {'a': 0, 'b': 2, 'c': table_path},
                {'a': 1, 'b': 3, 'c': table_path},
            ],
        )
    yt_client.set_attribute(
        table1,
        LOG_TARIFF_ARCHIVE_ATTR,
        {
            'merged': True,
            'merged_tables': {
                '2020-01-05T01:23:21': {'log_interval': LogInterval([Subinterval('c1', 't1', 0, 0, 5)]).to_meta()},
                '2020-01-05T12:23:21': {'log_interval': LogInterval([Subinterval('c1', 't1', 0, 5, 10)]).to_meta()},
            }
        },
    )

    archive_tree_processor.process_root()

    hm.assert_that(yt_client.list(task_dir, absolute=True), hm.contains_inanyorder(table2))
    hm.assert_that(
        yt.read_table(table2),
        hm.contains_inanyorder(
            hm.has_entries({'a': 0, 'b': 2, 'c': table1}),
            hm.has_entries({'a': 1, 'b': 3, 'c': table1}),
            hm.has_entries({'a': 0, 'b': 2, 'c': table2}),
            hm.has_entries({'a': 1, 'b': 3, 'c': table2}),
        ),
    )
    hm.assert_that(
        yt.get_attribute(table2, 'schema'),
        hm.contains(*(hm.has_entries(s) for s in TEST_SCHEMA)),
    )
    hm.assert_that(
        yt.get_attribute(table2, LOG_TARIFF_ARCHIVE_ATTR),
        hm.has_entries(
            {
                'merged': True,
                'merged_tables': {
                    '2020-01-05T01:23:21': {'log_interval': LogInterval([Subinterval('c1', 't1', 0, 0, 5)]).to_meta()},
                    '2020-01-05T12:23:21': {'log_interval': LogInterval([Subinterval('c1', 't1', 0, 5, 10)]).to_meta()},
                    '2020-01-05T17:43:21': {'log_interval': LogInterval([Subinterval('c1', 't1', 0, 10, 20)]).to_meta()}
                }
            }
        ),
    )
    hm.assert_that(
        yt.get_attribute(table2, LOG_TARIFF_META_ATTR)['log_interval'],
        hm.equal_to(
            LogInterval([
                Subinterval('c1', 't1', 0, 0, 20),
            ]).to_meta()
        ),
    )


def test_saving_log_tariff_meta(yt_client, task_dir, archive_tree_processor):
    yt_client.set_attribute(
        task_dir,
        LOG_TARIFF_ARCHIVE_ATTR,
        {
            'merge_mode': MergeMode.UNORDERED.value,
            'merge_period': None,
        },
    )
    table1 = ypath_join(task_dir, '2020-01-05T12:23:21')
    table2 = ypath_join(task_dir, '2020-01-05T17:43:21')

    yt_client.create(
        'table',
        table1,
        attributes={
            LOG_TARIFF_META_ATTR: {
                'log_interval': LogInterval([
                    Subinterval('c1', 't1', 0, 0, 10),
                ]).to_meta(),
                'first_thing': 1,
            },
            'schema': TEST_SCHEMA,
        },
    )
    yt_client.create(
        'table',
        table2,
        attributes={
            LOG_TARIFF_META_ATTR: {
                'log_interval': LogInterval([
                    Subinterval('c1', 't1', 0, 10, 20),
                ]).to_meta(),
                'second_thing': 2,
            },
            'schema': TEST_SCHEMA,
        },
    )
    for table_path in [table1, table2]:
        yt_client.write_table(
            table_path,
            [
                {'a': 0, 'b': 2, 'c': table_path},
                {'a': 1, 'b': 3, 'c': table_path},
            ],
        )

    archive_tree_processor.process_root()

    hm.assert_that(yt_client.list(task_dir, absolute=True), hm.contains_inanyorder(table2))
    hm.assert_that(
        yt.read_table(table2),
        hm.contains_inanyorder(
            hm.has_entries({'a': 0, 'b': 2, 'c': table1}),
            hm.has_entries({'a': 1, 'b': 3, 'c': table1}),
            hm.has_entries({'a': 0, 'b': 2, 'c': table2}),
            hm.has_entries({'a': 1, 'b': 3, 'c': table2}),
        ),
    )
    hm.assert_that(
        yt.get_attribute(table2, 'schema'),
        hm.contains(*(hm.has_entries(s) for s in TEST_SCHEMA)),
    )
    hm.assert_that(
        yt.get_attribute(table2, LOG_TARIFF_META_ATTR),
        hm.has_entries(
            log_interval=hm.equal_to(
                LogInterval([
                    Subinterval('c1', 't1', 0, 0, 20),
                ]).to_meta()
            ),
            second_thing=2,
        ),
    )
    hm.assert_that(
        yt.get_attribute(table2, LOG_TARIFF_ARCHIVE_ATTR),
        hm.has_entries(
            {
                'merged': True,
                'merged_tables': {
                    '2020-01-05T12:23:21': {
                        'log_interval': LogInterval([Subinterval('c1', 't1', 0, 0, 10)]).to_meta(),
                        'first_thing': 1,
                    },
                    '2020-01-05T17:43:21': {
                        'log_interval': LogInterval([Subinterval('c1', 't1', 0, 10, 20)]).to_meta(),
                        'second_thing': 2,
                    }
                }
            }
        ),
    )


def test_check_intervals_alignment_fail(yt_client, task_dir, archive_tree_processor):
    yt_client.set_attribute(
        task_dir,
        LOG_TARIFF_ARCHIVE_ATTR,
        {
            'merge_mode': MergeMode.UNORDERED.value,
            'merge_period': None,
        },
    )
    table1 = ypath_join(task_dir, '2020-01-05T12:23:21')
    table2 = ypath_join(task_dir, '2020-01-05T17:43:21')

    yt_client.create(
        'table',
        table1,
        attributes={
            LOG_TARIFF_META_ATTR: {
                'log_interval': LogInterval([
                    Subinterval('c1', 't1', 0, 0, 10),
                ]).to_meta(),
            },
            'schema': TEST_SCHEMA,
        },
    )
    yt_client.create(
        'table',
        table2,
        attributes={
            LOG_TARIFF_META_ATTR: {
                'log_interval': LogInterval([
                    Subinterval('c1', 't1', 0, 20, 30),
                ]).to_meta(),
            },
            'schema': TEST_SCHEMA,
        },
    )
    for table_path in [table1, table2]:
        yt_client.write_table(
            table_path,
            [
                {'a': 0, 'b': 2, 'c': table_path},
                {'a': 1, 'b': 3, 'c': table_path},
            ],
        )

    with pytest.raises(AssertionError) as excinfo:
        archive_tree_processor.process_root()
    assert excinfo.value.args[0] == "Intervals misaligned: [(('c1', 't1', 0), 10)] != [(('c1', 't1', 0), 20)]"

    hm.assert_that(yt_client.list(task_dir, absolute=True), hm.contains_inanyorder(table1, table2))
    for table_path in [table1, table2]:
        hm.assert_that(
            yt.read_table(table_path),
            hm.contains_inanyorder(
                hm.has_entries({'a': 0, 'b': 2, 'c': table_path}),
                hm.has_entries({'a': 1, 'b': 3, 'c': table_path}),
            ),
        )
        hm.assert_that(
            yt.get_attribute(table_path, 'schema'),
            hm.contains(*(hm.has_entries(s) for s in TEST_SCHEMA)),
        )
        hm.assert_that(
            yt_client.get(table_path, attributes=[LOG_TARIFF_ARCHIVE_ATTR]).has_attributes(),
            hm.is_(False),
        )
        hm.assert_that(
            yt.get_attribute(table1, LOG_TARIFF_META_ATTR),
            hm.has_entries(
                log_interval=hm.equal_to(
                    LogInterval([
                        Subinterval('c1', 't1', 0, 0, 10),
                    ]).to_meta()
                ),
            ),
        )
        hm.assert_that(
            yt.get_attribute(table2, LOG_TARIFF_META_ATTR),
            hm.has_entries(
                log_interval=hm.equal_to(
                    LogInterval([
                        Subinterval('c1', 't1', 0, 20, 30),
                    ]).to_meta()
                ),
            ),
        )


def test_merging_different_schemas(yt_client, task_dir, archive_tree_processor):
    yt_client.set_attribute(
        task_dir,
        LOG_TARIFF_ARCHIVE_ATTR,
        {
            'merge_mode': MergeMode.UNORDERED.value,
            'merge_period': None,
            'recursive': False,
        },
    )
    table1 = ypath_join(task_dir, '2020-01-05T12:23:21')
    table2 = ypath_join(task_dir, '2020-01-05T17:43:21')
    table3 = ypath_join(task_dir, '2020-01-05T20:23:21')
    table4 = ypath_join(task_dir, '2020-01-05T21:43:21')

    schema1 = [
        {'name': 'a', 'type': 'int64'},
    ]
    schema2 = [
        {'name': 'a', 'type': 'uint64'},
        {'name': 'b', 'type': 'string'},
    ]
    for i, table_path in enumerate([table1, table2]):
        yt_client.create(
            'table',
            table_path,
            attributes={
                LOG_TARIFF_META_ATTR: {
                    'log_interval': LogInterval([
                        Subinterval('c1', 't1', 0, i * 10, i * 10 + 10),
                    ]).to_meta(),
                },
                'schema': schema1,
            },
        )
        yt_client.write_table(
            table_path,
            [
                {'a': i},
            ],
        )
    for i, table_path in enumerate([table3, table4]):
        yt_client.create(
            'table',
            table_path,
            attributes={
                LOG_TARIFF_META_ATTR: {
                    'log_interval': LogInterval([
                        Subinterval('c1', 't1', 0, (i + 2) * 10, (i + 2) * 10 + 10),
                    ]).to_meta(),
                },
                'schema': schema2,
            },
        )
        yt_client.write_table(
            table_path,
            [
                {'a': i + 2, 'b': table_path},
            ],
        )

    archive_tree_processor.process_root()

    hm.assert_that(yt_client.list(task_dir, absolute=True), hm.contains_inanyorder(table2, table4))
    hm.assert_that(
        yt.read_table(table2),
        hm.contains_inanyorder(
            hm.has_entries({'a': 0}),
            hm.has_entries({'a': 1}),
        ),
    )
    hm.assert_that(
        yt.get_attribute(table2, 'schema'),
        hm.contains(*(hm.has_entries(s) for s in schema1)),
    )
    hm.assert_that(
        yt.get_attribute(table2, LOG_TARIFF_ARCHIVE_ATTR),
        hm.has_entries(
            {
                'merged': True,
                'merged_tables': {
                    '2020-01-05T12:23:21': {'log_interval': LogInterval([Subinterval('c1', 't1', 0, 0, 10)]).to_meta()},
                    '2020-01-05T17:43:21': {'log_interval': LogInterval([Subinterval('c1', 't1', 0, 10, 20)]).to_meta()},
                }
            }
        ),
    )
    hm.assert_that(
        yt.get_attribute(table2, LOG_TARIFF_META_ATTR)['log_interval'],
        hm.equal_to(
            LogInterval([
                Subinterval('c1', 't1', 0, 0, 20),
            ]).to_meta()
        ),
    )

    hm.assert_that(
        yt.read_table(table4),
        hm.contains_inanyorder(
            hm.has_entries({'a': 2, 'b': table3}),
            hm.has_entries({'a': 3, 'b': table4}),
        ),
    )
    hm.assert_that(
        yt.get_attribute(table4, 'schema'),
        hm.contains(*(hm.has_entries(s) for s in schema2)),
    )
    hm.assert_that(
        yt.get_attribute(table4, LOG_TARIFF_ARCHIVE_ATTR),
        hm.has_entries(
            {
                'merged': True,
                'merged_tables': {
                    '2020-01-05T20:23:21': {'log_interval': LogInterval([Subinterval('c1', 't1', 0, 20, 30)]).to_meta()},
                    '2020-01-05T21:43:21': {'log_interval': LogInterval([Subinterval('c1', 't1', 0, 30, 40)]).to_meta()},
                }
            }
        ),
    )
    hm.assert_that(
        yt.get_attribute(table4, LOG_TARIFF_META_ATTR)['log_interval'],
        hm.equal_to(
            LogInterval([
                Subinterval('c1', 't1', 0, 20, 40),
            ]).to_meta()
        ),
    )


def test_merging_with_interval_limit(yt_client, task_dir, archive_tree_processor, yt_root):
    interval_limits_dir_path = ypath_join(yt_root, 'intervals_dir')
    yt_client.create(
        'map_node',
        interval_limits_dir_path,
    )

    table_ = ypath_join(interval_limits_dir_path, '2020-01-05T16:23:21')
    yt_client.create(
        'table',
        table_,
        attributes={
            LOG_TARIFF_META_ATTR: {
                'log_interval': LogInterval([
                    Subinterval('c1', 't1', 0, 0, 30),
                    Subinterval('c1', 't1', 1, 0, 15),
                    Subinterval('c1', 't1', 4, 0, 20),
                ]).to_meta(),
            },
            'schema': TEST_SCHEMA,
        },
    )
    table_ = ypath_join(interval_limits_dir_path, '2020-01-05T18:23:21')
    yt_client.create(
        'table',
        table_,
        attributes={
            LOG_TARIFF_META_ATTR: {
                'log_interval': LogInterval([
                    Subinterval('c1', 't1', 0, 30, 55),
                    Subinterval('c1', 't1', 1, 15, 50),
                    Subinterval('c1', 't1', 4, 20, 50),
                ]).to_meta(),
            },
            'schema': TEST_SCHEMA,
        },
    )

    yt_client.set_attribute(
        task_dir,
        LOG_TARIFF_ARCHIVE_ATTR,
        {
            'merge_mode': MergeMode.UNORDERED.value,
            'merge_period': None,  # Merge everything
            'recursive': False,
            'interval_limit_folder': interval_limits_dir_path,
        },
    )
    table1 = ypath_join(task_dir, '2020-01-01T12:23:21')
    table2 = ypath_join(task_dir, '2020-01-01T17:43:21')
    table3 = ypath_join(task_dir, '2020-01-01T23:43:21')

    yt_client.create('table', table1, attributes={
        LOG_TARIFF_META_ATTR: {
            'log_interval': LogInterval([
                Subinterval('c1', 't1', 0, 20, 30),
                Subinterval('c1', 't1', 1, 5, 10),
                Subinterval('c1', 't1', 4, 20, 20),
            ]).to_meta(),
        },
        'schema': TEST_SCHEMA,
    })
    yt_client.write_table(
        table1,
        [
            {'a': -241, 'b': 12536, 'c': 'wow'},
            {'a': 0, 'b': 123, 'c': 'wow'},
        ],
    )

    yt_client.create('table', table2, attributes={
        LOG_TARIFF_META_ATTR: {
            'log_interval': LogInterval([
                Subinterval('c1', 't1', 0, 30, 50),
                Subinterval('c1', 't1', 1, 10, 22),
                Subinterval('c1', 't1', 4, 20, 50),
            ]).to_meta(),
        },
        'schema': TEST_SCHEMA,
    })
    yt_client.write_table(
        table2,
        [
            {'a': -1, 'b': 3, 'c': 'abc'},
            {'a': -2, 'b': 0, 'c': 'wow'},
        ],
    )

    yt_client.create('table', table3, attributes={
        LOG_TARIFF_META_ATTR: {
            'log_interval': LogInterval([
                Subinterval('c1', 't1', 0, 50, 60),
                Subinterval('c1', 't1', 1, 22, 50),
                Subinterval('c1', 't1', 4, 50, 50),
                Subinterval('c1', 't2', 0, 0, 50),
            ]).to_meta(),
        },
        'schema': TEST_SCHEMA,
    })
    yt_client.write_table(
        table3,
        [
            {'a': -666, 'b': 0, 'c': ''},
            {'a': 666, 'b': 0, 'c': '666'},
        ],
    )
    archive_tree_processor.process_root()

    hm.assert_that(yt_client.list(task_dir, absolute=True), hm.contains_inanyorder(table2, table3))
    logging.info('%s', yt_client.list(task_dir, absolute=True))
    hm.assert_that(
        yt.read_table(table2),
        hm.contains_inanyorder(
            hm.has_entries({'a': -241, 'b': 12536, 'c': 'wow'}),
            hm.has_entries({'a': 0, 'b': 123, 'c': 'wow'}),
            hm.has_entries({'a': -1, 'b': 3, 'c': 'abc'}),
            hm.has_entries({'a': -2, 'b': 0, 'c': 'wow'}),
        ),
    )
    hm.assert_that(
        yt.read_table(table3),
        hm.contains_inanyorder(
            hm.has_entries({'a': -666, 'b': 0, 'c': ''}),
            hm.has_entries({'a': 666, 'b': 0, 'c': '666'}),
        ),
    )
    hm.assert_that(
        yt.get_attribute(table2, 'schema'),
        hm.contains(*(hm.has_entries(s) for s in TEST_SCHEMA)),
    )
    hm.assert_that(
        yt.get_attribute(table3, 'schema'),
        hm.contains(*(hm.has_entries(s) for s in TEST_SCHEMA)),
    )
    hm.assert_that(
        yt.get_attribute(table2, LOG_TARIFF_META_ATTR)['log_interval'],
        hm.equal_to(
            LogInterval([
                Subinterval('c1', 't1', 0, 20, 50),
                Subinterval('c1', 't1', 1, 5, 22),
                Subinterval('c1', 't1', 4, 20, 50),
            ]).to_meta()
        ),
    )
    hm.assert_that(
        yt.get_attribute(table3, LOG_TARIFF_META_ATTR)['log_interval'],
        hm.equal_to(
            LogInterval([
                Subinterval('c1', 't1', 0, 50, 60),
                Subinterval('c1', 't1', 1, 22, 50),
                Subinterval('c1', 't1', 4, 50, 50),
                Subinterval('c1', 't2', 0, 0, 50),
            ]).to_meta()
        ),
    )
    logging.info('%s', yt.get_attribute(table2, LOG_TARIFF_ARCHIVE_ATTR))
    hm.assert_that(
        yt.get_attribute(table2, LOG_TARIFF_ARCHIVE_ATTR),
        hm.has_entries({
            'merged': True,
            'merged_tables': {
                '2020-01-01T12:23:21': {
                    'log_interval': LogInterval(
                        [Subinterval('c1', 't1', 0, 20, 30),
                         Subinterval('c1', 't1', 1, 5, 10),
                         Subinterval('c1', 't1', 4, 20, 20)]
                    ).to_meta()
                },
                '2020-01-01T17:43:21': {
                    'log_interval': LogInterval(
                        [Subinterval('c1', 't1', 0, 30, 50),
                         Subinterval('c1', 't1', 1, 10, 22),
                         Subinterval('c1', 't1', 4, 20, 50)]
                    ).to_meta()
                }
            }
        }),
    )


@pytest.mark.parametrize(
    ['deletion_mode', 'deletion_params'],
    [
        pytest.param(DeletionMode.AFTER_N_FILES.value, {'last_files': 2}),
        pytest.param(DeletionMode.AFTER_N_DAYS.value, {'last_days': 2}),
        pytest.param(None, None),
    ],
)
def test_deletion_with_interval_limit(yt_client, task_dir, archive_tree_processor, deletion_mode, deletion_params, yt_root):
    interval_limits_dir_path = ypath_join(yt_root, 'intervals_dir')
    yt_client.create(
        'map_node',
        interval_limits_dir_path,
    )

    table_ = ypath_join(interval_limits_dir_path, '2020-01-05T16:23:21')
    yt_client.create(
        'table',
        table_,
        attributes={
            LOG_TARIFF_META_ATTR: {
                'log_interval': LogInterval([
                    Subinterval('c1', 't1', 0, 0, 10),
                    Subinterval('c1', 't1', 1, 0, 5),
                    Subinterval('c1', 't1', 4, 0, 10),
                ]).to_meta(),
            },
            'schema': TEST_SCHEMA,
        },
    )
    table_ = ypath_join(interval_limits_dir_path, '2020-01-05T18:23:21')
    yt_client.create(
        'table',
        table_,
        attributes={
            LOG_TARIFF_META_ATTR: {
                'log_interval': LogInterval([
                    Subinterval('c1', 't1', 0, 10, 30),
                    Subinterval('c1', 't1', 1, 5, 29),
                    Subinterval('c1', 't1', 4, 10, 30),
                ]).to_meta(),
            },
            'schema': TEST_SCHEMA,
        },
    )

    attrs = {
        'deletion_mode': deletion_mode,
        'recursive': False,
        'interval_limit_folder': interval_limits_dir_path,
    }
    if deletion_params is not None:
        attrs['deletion_params'] = deletion_params
    yt_client.set_attribute(
        task_dir,
        LOG_TARIFF_ARCHIVE_ATTR,
        attrs,
    )
    table1 = ypath_join(task_dir, '2020-01-06T12:23:21')
    table2 = ypath_join(task_dir, '2020-01-05T17:43:21')
    table3 = ypath_join(task_dir, '2020-01-04T23:43:21')
    table4 = ypath_join(task_dir, '2020-01-03T23:43:21')

    for table_path in [table1, table2, table3]:
        yt_client.create('table', table_path, attributes={
            LOG_TARIFF_META_ATTR: {
                'log_interval': LogInterval([
                    Subinterval('c1', 't1', 0, 20, 30),
                    Subinterval('c1', 't1', 1, 5, 10),
                    Subinterval('c1', 't1', 4, 20, 20),
                ]).to_meta(),
            },
            'schema': TEST_SCHEMA,
        })
    yt_client.create('table', table4, attributes={
        LOG_TARIFF_META_ATTR: {
            'log_interval': LogInterval([
                Subinterval('c1', 't1', 0, 30, 30),
                Subinterval('c1', 't1', 1, 10, 30),
                Subinterval('c1', 't1', 4, 20, 30),
            ]).to_meta(),
        },
        'schema': TEST_SCHEMA,
    })
    archive_tree_processor.process_root(now=datetime(year=2020, month=1, day=6, hour=12))
    if deletion_mode:
        hm.assert_that(yt_client.list(task_dir, absolute=True), hm.contains_inanyorder(table1, table2, table4))
    else:
        hm.assert_that(yt_client.list(task_dir, absolute=True), hm.contains_inanyorder(table1, table2, table3, table4))


def test_transaction(yt_client, task_dir, archive_tree_processor):
    yt_client.set_attribute(
        task_dir,
        LOG_TARIFF_ARCHIVE_ATTR,
        {
            'merge_mode': MergeMode.UNORDERED.value,
            'merge_period': None,
            'recursive': False,
        },
    )

    table1 = ypath_join(task_dir, '2020-01-04T12:23:21')
    table2 = ypath_join(task_dir, '2020-01-04T17:43:21')
    table3 = ypath_join(task_dir, '2020-01-05T12:23:21')
    table4 = ypath_join(task_dir, '2020-01-05T17:43:21')

    for i, table_path in enumerate([table1, table2, table3, table4]):
        yt_client.create(
            'table',
            table_path,
            attributes={
                LOG_TARIFF_META_ATTR: {
                    'log_interval': LogInterval([
                        Subinterval('c1', 't1', 0, i * 10, i * 10 + 10),
                    ]).to_meta(),
                },
                'schema': TEST_SCHEMA,
            },
        )
        yt_client.write_table(
            table_path,
            [
                {'a': 0, 'b': 2, 'c': table_path},
                {'a': 1, 'b': 3, 'c': table_path},
            ],
        )

    class ExampleException(Exception):
        pass

    with pytest.raises(ExampleException):
        with yt_client.Transaction() as t:
            archive_tree_processor._merge_dir_tables(
                task_dir,
                archive_tree_processor.load_config(task_dir),
                merge_pool=4,
                transaction_id=t.transaction_id,
                now=datetime(year=2020, month=1, day=5, hour=12),
                interval_limit=None,
            )
            raise ExampleException()

    hm.assert_that(yt_client.list(task_dir, absolute=True), hm.contains_inanyorder(table1, table2, table3, table4))
    for i, table_path in enumerate([table1, table2, table3, table4]):
        logging.info('Table data: %s', list(yt.read_table(table_path)))
        hm.assert_that(
            yt.read_table(table_path),
            hm.contains_inanyorder(
                hm.has_entries({'a': 0, 'b': 2, 'c': table_path}),
                hm.has_entries({'a': 1, 'b': 3, 'c': table_path}),
            ),
        )
        hm.assert_that(
            yt.get_attribute(table_path, 'schema'),
            hm.contains(*(hm.has_entries(s) for s in TEST_SCHEMA)),
        )
        hm.assert_that(
            yt.get_attribute(table_path, LOG_TARIFF_META_ATTR)['log_interval'],
            hm.equal_to(
                LogInterval([
                    Subinterval('c1', 't1', 0, i * 10, i * 10 + 10),
                ]).to_meta()
            ),
        )
        hm.assert_that(
            yt_client.get(table_path, attributes=[LOG_TARIFF_ARCHIVE_ATTR]).has_attributes(),
            hm.is_(False),
        )


def test_thread_transaction(yt_client, task_dir, archive_tree_processor):
    yt_client.set_attribute(
        task_dir,
        LOG_TARIFF_ARCHIVE_ATTR,
        {
            'recursive': True,
        },
    )
    dir1 = ypath_join(task_dir, 'dir1')
    dir2 = ypath_join(task_dir, 'dir2')

    for dir_path in [dir1, dir2]:
        yt_client.create(
            'map_node',
            dir_path,
            attributes={
                LOG_TARIFF_ARCHIVE_ATTR: {
                    'merge_mode': MergeMode.UNORDERED.value,
                    'merge_period': None,
                },
            },
        )

    # Right tables
    table1 = ypath_join(dir1, '2020-01-04T12:23:21')
    table2 = ypath_join(dir1, '2020-01-04T17:43:21')

    # Wrong attributes
    table3 = ypath_join(dir2, '2020-01-04T18:43:21')
    table4 = ypath_join(dir2, '2020-01-04T23:43:21')

    for i, table_path in enumerate([table1, table2]):
        yt_client.create(
            'table',
            table_path,
            attributes={
                LOG_TARIFF_META_ATTR: {
                    'log_interval': LogInterval([
                        Subinterval('c1', 't1', 0, i * 10, i * 10 + 10),
                    ]).to_meta(),
                },
                'schema': TEST_SCHEMA,
            },
        )
        yt_client.write_table(
            table_path,
            [
                {'a': 0, 'b': 2, 'c': table_path},
                {'a': 1, 'b': 3, 'c': table_path},
            ],
        )
    for table_path in [table3, table4]:
        yt_client.create(
            'table',
            table_path,
            attributes={
                LOG_TARIFF_META_ATTR: {
                    'log_interval': 'nothing',
                },
                'schema': TEST_SCHEMA,
            },
        )
        yt_client.write_table(
            table_path,
            [
                {'a': 0, 'b': 2, 'c': table_path},
                {'a': 1, 'b': 3, 'c': table_path},
            ],
        )

    with pytest.raises(TypeError):
        archive_tree_processor.process_root(now=datetime(year=2020, month=1, day=5, hour=12))

    hm.assert_that(yt_client.list(dir2, absolute=True), hm.contains_inanyorder(table3, table4))
    for table_path in [table3, table4]:
        hm.assert_that(
            yt.read_table(table_path),
            hm.contains_inanyorder(
                hm.has_entries({'a': 0, 'b': 2, 'c': table_path}),
                hm.has_entries({'a': 1, 'b': 3, 'c': table_path}),
            ),
        )
        hm.assert_that(
            yt.get_attribute(table_path, 'schema'),
            hm.contains(*(hm.has_entries(s) for s in TEST_SCHEMA)),
        )
        hm.assert_that(
            yt_client.get(table_path, attributes=[LOG_TARIFF_ARCHIVE_ATTR]).has_attributes(),
            hm.is_(False),
        )

    hm.assert_that(yt_client.list(dir1, absolute=True), hm.contains_inanyorder(table2))
    hm.assert_that(
        yt.read_table(table2),
        hm.contains_inanyorder(
            hm.has_entries({'a': 0, 'b': 2, 'c': table1}),
            hm.has_entries({'a': 1, 'b': 3, 'c': table1}),
            hm.has_entries({'a': 0, 'b': 2, 'c': table2}),
            hm.has_entries({'a': 1, 'b': 3, 'c': table2}),
        ),
    )
    hm.assert_that(
        yt.get_attribute(table2, 'schema'),
        hm.contains(*(hm.has_entries(s) for s in TEST_SCHEMA)),
    )
    hm.assert_that(
        yt.get_attribute(table2, LOG_TARIFF_META_ATTR)['log_interval'],
        hm.equal_to(
            LogInterval([
                Subinterval('c1', 't1', 0, 0, 20),
            ]).to_meta()
        ),
    )
    hm.assert_that(
        yt.get_attribute(table2, LOG_TARIFF_ARCHIVE_ATTR),
        hm.has_entries(
            {
                'merged': True,
                'merged_tables': {
                    '2020-01-04T12:23:21': {'log_interval': LogInterval([Subinterval('c1', 't1', 0, 0, 10)]).to_meta()},
                    '2020-01-04T17:43:21': {'log_interval': LogInterval([Subinterval('c1', 't1', 0, 10, 20)]).to_meta()},
                }
            }
        ),
    )
