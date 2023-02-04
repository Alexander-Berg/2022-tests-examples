# -*- coding: utf-8 -*-
import os
from collections import defaultdict
from csv import DictReader
from itertools import izip
import numpy as np

from yt.wrapper import ypath_join
from datacloud.dev_utils.yt.yt_utils import DynTable
from datacloud.dev_utils.data.data_utils import array_fromstring, array_tostring


def glue_external_id(row):
    row['external_id'] = row['external_id'] + '_' + row['retro_date']
    return row


def empty_str_to_none(row):
    for key in row:
        if row[key] == '':
            row[key] = None
    return row


def assert_raw_upload(yt_client, input_file, norm_file, constants, **kwargs):
    assert yt_client.exists(constants.RAW_DATA_DIR), 'Raw data dir does not exist!'
    assert yt_client.exists(constants.RAW_TABLE), 'Raw table does not exist!'
    assert yt_client.exists(ypath_join(
        constants.RAW_DATA_DIR,
        os.path.split(input_file)[1]
    )), 'Input file does not exist!'
    assert yt_client.exists(constants.GLUED_TABLE), 'Glued table does not exist!'
    assert yt_client.get_attribute(constants.GLUED_TABLE, 'sorted'), 'Glued table is not sorted!'
    assert yt_client.get_attribute(constants.GLUED_TABLE, 'sorted_by') == ['external_id'], \
        'Glued table is not sorted by ["external_id"]!'

    yt_rows = list(yt_client.read_table(constants.GLUED_TABLE))
    with open(norm_file) as nf:
        local_rows = list(DictReader(nf, delimiter=constants.NORM_DELIMITER))
    local_rows = map(empty_str_to_none, local_rows)
    local_rows = map(glue_external_id, local_rows)
    for yt_row, local_row in izip(yt_rows, local_rows):
        assert local_row == yt_row, '{} != {}'.format(local_row, yt_row)


def assert_append_meta_table(yt_client, input_pipeline, input_file, constants, **kwargs):
    meta_rows = DynTable.get_rows_from_table(
        input_pipeline.settings.METADATA_TABLE,
        {
            'partner_id': constants.PARTNER_ID,
            'ticket': constants.TICKET_NAME,
            'file': os.path.split(input_file)[-1]
        },
        yt_client
    )
    row = list(meta_rows)[0]
    row.pop('upload_time')

    reference = {
        'partner_id': constants.PARTNER_ID,
        'ticket': constants.TICKET_NAME,
        'file': os.path.split(input_file)[-1],
        'meta_data': {
            'hits': {
                'input_file': {'abs': 5L, 'rel': 1L},
                'normalized_file': {'abs': 5L, 'rel': 1.},
                'all_yuid': {'abs': 2L, 'rel': 2. / 5}
            },
            'min_retro_date': '2016-08-19',
            'max_retro_date': '2016-08-22',
            'targets_count': {'target': {
                '-1': {'abs': 2L, 'rel': 2. / 5},
                '0': {'abs': 2L, 'rel': 2. / 5},
                '1': {'abs': 1L, 'rel': 1. / 5}
            }},
            'ids_count': {
                'phone': {'abs': 5L, 'rel': 1.},
                'email': {'abs': 2L, 'rel': 2. / 5}
            },
            'months_dict': {'2016-08': 5L},
        }
    }

    assert row == reference


def assert_merge_audience(yt_client, constants, **kwargs):
    audience_table = ypath_join(constants.AUDIENCE_DIR, constants.PARTNER_ID)
    assert yt_client.row_count(audience_table) == 6, 'Bad rows number in audience table!'

    phones_num = {'87129671': 2, '87132795': 1, '87151998': 1,
                  '87160997': 1, '87161045': 1, '0000496547': 1}
    emails_num = {'87129671': 1, '87132795': 0, '87151998': 3,
                  '87160997': 1, '87161045': 0, '0000496547': 1}
    for row in yt_client.read_table(audience_table):
        external_id = row['external_id']
        assert external_id in phones_num, '{} not found in phones_num'.format(external_id)
        assert external_id in emails_num, '{} not found in emails_num'.format(external_id)

        assert phones_num[external_id] == len(row['phones']), \
               'Bad number of phones for {}'.format(external_id)
        assert emails_num[external_id] == len(row['emails']), \
               'Bad number of emails for {}'.format(external_id)


def assert_make_input(yt_client, constants, **kwargs):
    assert yt_client.exists(constants.INPUT_TABLE), 'Input table does not exist!'
    schema = yt_client.get_attribute(constants.INPUT_TABLE, 'schema')
    col_names = set(col['name'] for col in schema)
    assert col_names == set(('id_type', 'id_value', 'external_id',
                             'timestamp', 'target')), 'Bad columns schema!'

    eid2id_values = defaultdict(set)
    for row in yt_client.read_table(constants.INPUT_TABLE):
        eid2id_values[row['external_id']].add(row['id_value'])

    eid2id_values_num = {'87129671_2016-08-19': 1, '87132795_2016-08-20': 1,
                         '87151998_2016-08-21': 3, '87160997_2016-08-22': 2,
                         '87161045_2016-08-21': 1}
    assert set(eid2id_values.keys()) == set(eid2id_values_num.keys())

    for eid, num in eid2id_values_num.iteritems():
        assert num == len(eid2id_values[eid]), \
            'Unexpected id values number at {}!'.format(eid)


def assert_make_all_yuid(yt_client, constants, **kwargs):
    assert yt_client.exists(constants.ALL_CID_TABLE)
    assert yt_client.exists(constants.ALL_YUID_TABLE)
    assert yt_client.get_attribute(constants.ALL_CID_TABLE, 'sorted_by') == ['cid']
    assert yt_client.get_attribute(constants.ALL_YUID_TABLE, 'sorted_by') == ['external_id']

    eid2cids_expected = {
        '87151998_2016-08-21': set(['0']),
        '87161045_2016-08-21': set(['1'])
    }
    eid2cids = defaultdict(set)
    for row in yt_client.read_table(constants.ALL_CID_TABLE):
        eid2cids[row['external_id']].add(row['cid'])
    assert eid2cids_expected == eid2cids

    eid2yuids_expected = {
        '87151998_2016-08-21': set(['0', '1', '2']),
        '87161045_2016-08-21': set(['3', '4'])
    }
    eid2yuids = defaultdict(set)
    for row in yt_client.read_table(constants.ALL_YUID_TABLE):
        eid2yuids[row['external_id']].add(row['yuid'])
    assert eid2yuids == eid2yuids_expected

    assert yt_client.exists(constants.INPUT_YUID_TABLE)
    assert yt_client.get_attribute(constants.INPUT_YUID_TABLE, 'sorted_by') == [
        'external_id',
        'yuid'
    ]


def assert_grep(yt_client, constants, **kwargs):
    expected_logs = {
        'spy_log': {
            '87151998_2016-08-21': 8 + 5 + 6,
            '87161045_2016-08-21': 5 + 8
        },
        'watch_log_tskv': {
            '87151998_2016-08-21': 5 + 9 + 6,
            '87161045_2016-08-21': 6 + 6
        }
    }
    for log_type, count_expected in expected_logs.iteritems():
        count_actual = defaultdict(int)
        for table in yt_client.list(ypath_join(constants.GREP_TICKET_DIR, log_type),
                                    absolute=True):
            for row in yt_client.read_table(table):
                count_actual[row['external_id']] += 1
        assert count_actual == count_expected, log_type


def normalize_features(features):
    n_features = np.array(features) / np.linalg.norm(features)
    return list(array_fromstring(array_tostring(n_features)))


def assert_cluster(yt_client, constants):
    user2clust = ypath_join(constants.AGG_TICKET_DIR, 'cluster', 'user2clust', 'learn')
    eid2features = dict()
    for row in yt_client.read_table(user2clust):
        eid2features[row['external_id']] = list(array_fromstring(row['features']))[:2]

    eid2features_expected = {
        '87151998_2016-08-21': normalize_features([1., 1.]),
        '87161045_2016-08-21': normalize_features([1., 0.]),
    }
    assert eid2features == eid2features_expected


def assert_normed_s2v(yt_client, constants):
    user2normed_s2v = ypath_join(constants.AGG_TICKET_DIR, 'normed_s2v', 'weekly', 'learn')
    eid2features = dict()
    for row in yt_client.read_table(user2normed_s2v):
        eid2features[row['external_id']] = list(array_fromstring(row['features']))[:2]

    eid2features_expected = {
        '87151998_2016-08-21': normalize_features([1., 1.]),
        '87161045_2016-08-21': normalize_features([1., 0.]),
    }
    assert eid2features == eid2features_expected


def assert_calc_cluster_features(yt_client, constants, **kwargs):
    assert_cluster(yt_client=yt_client, constants=constants)
    assert_normed_s2v(yt_client=yt_client, constants=constants)


def assert_calc_dssm_features(yt_client, constants, input_pipeline, **kwargs):
    dssm_features = ypath_join(constants.AGG_TICKET_DIR, 'dssm', 'weekly', 'retro')
    assert yt_client.exists(dssm_features)


def assert_combine_features(yt_client, constants, **kwargs):
    assert yt_client.exists(ypath_join(constants.TICKET_DIR, 'features_dssm_normed_s2v'))


def assert_apply_model(yt_client, **kwargs):
    pass


def test_steps(yt_client, constants, input_pipeline, input_file, norm_file):
    os.environ['YT_TOKEN'] = 'sample-yt-token'
    steps_and_checks = [
        # ('run_raw_upload', assert_raw_upload),
        # ('run_append_meta_table', None),
        # ('run_merge_audience', assert_merge_audience),
        # ('run_make_input', assert_make_input),
        # ('run_make_all_yuid', assert_make_all_yuid),
        # ('run_metadata_all_yuid', assert_append_meta_table),
        # ('run_grep', assert_grep),
        # ('run_calc_cluster_features', assert_calc_cluster_features),
        # ('run_calc_dssm_features', assert_calc_dssm_features),
        # ('run_combine_features', assert_combine_features),
        # ('run_apply_model', assert_apply_model)
    ]

    for step, check in steps_and_checks:
        method_to_call = getattr(input_pipeline, step)
        method_to_call()

        if callable(check):
            check(
                yt_client=yt_client, constants=constants, input_pipeline=input_pipeline,
                input_file=input_file, norm_file=norm_file
            )
