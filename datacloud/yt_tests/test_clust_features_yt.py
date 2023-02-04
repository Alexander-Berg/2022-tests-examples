import numpy as np
from yt.wrapper import ypath_join, TablePath
from datacloud.features.cluster import clust_features
from datacloud.features.cluster.path_config import PathConfig
from datacloud.dev_utils.yt import yt_utils


def is_equal_records(actual, expected):
    actual = set(tuple(sorted(it.items())) for it in actual)
    expected = set(tuple(sorted(it.items())) for it in expected)
    return actual == expected


def test_remove_old_tables(yt_stuff):
    yt_client = yt_stuff.get_yt_client()
    folder = '//test/cluster/test-remove-old-tables'
    yt_utils.create_folders([folder], yt_client)
    tables_to_create = [
        folder + '/2010-01-01',
        folder + '/2010-01-02',
        folder + '/2010-01-03',
        folder + '/2010-01-04',
    ]
    for table in tables_to_create:
        yt_client.create(type='table', path=table)
    clust_features.remove_old_tables(yt_client, folder, tables_to_keep=2)
    result_tables = [table for table in yt_client.list(folder)]
    assert result_tables == ['2010-01-03', '2010-01-04']


def test_append_crypta_vector_reducer(yt_stuff):
    yt_client = yt_stuff.get_yt_client()
    folder = '//test/cluster/appenc-crypta-verctor-reducer'
    yt_utils.create_folders([folder], yt_client)
    input_vector_table = TablePath(ypath_join(folder, 'vector_table'),
                                   sorted_by=['key'])
    input_key_table = TablePath(ypath_join(folder, 'key_table'), sorted_by=['key'])
    output_table = ypath_join(folder, 'output_table')
    yt_client.write_table(
        input_vector_table,
        [{'key': 'key1', 'vector_b': 'b1b1b1', 'vector_m': 'm1m1m1'}],
    )
    yt_client.write_table(
        input_key_table,
        [{'key': 'key1', 'data': 'sample_data'}],
    )
    yt_client.run_reduce(
        clust_features.AppendCriptaVectorReducer(),
        [input_vector_table, input_key_table],
        output_table,
        reduce_by='key',
    )
    actual = list(yt_client.read_table(output_table))
    expected = [{'key': 'key1', 'vector_b': 'b1b1b1', 'vector_m': 'm1m1m1',
                 'data': 'sample_data'}]
    assert is_equal_records(actual, expected)


def test_cluster_center_reducer_integer(yt_stuff):
    yt_client = yt_stuff.get_yt_client()
    folder = '//test/cluster/cluster-center-reducer-integer'
    yt_utils.create_folders([folder], yt_client)
    input_table = TablePath(ypath_join(folder, 'input_data'), sorted_by=['cat'])
    output_table = ypath_join(folder, 'output')
    yt_client.write_table(
        input_table,
        [
            {
                'cat': 'cat1',
                'cat_name': 'cat_name1',
                'vector_b': '\x00\x00\x80?\x00\x00\x00@\x00\x00@@',  # [1, 2, 3]
                'vector_m': '\x00\x00\x80@\x00\x00\xa0@\x00\x00\xc0@',  # [4 5 6]
            },
            {
                'cat': 'cat1',
                'cat_name': 'cat_name1',
                'vector_b': '\x00\x00\x80?\x00\x00\x80?\x00\x00\x80?',  # [1, 1, 1]
                'vector_m': '\x00\x00\x00@\x00\x00\x00@\x00\x00\x00@',  # [2, 2, 2]
            },
        ]
    )

    yt_client.run_reduce(
        clust_features.cluster_center_reduce, input_table, output_table,
        reduce_by='cat',
    )
    actual = list(yt_client.read_table(output_table))
    expected = [
        {
            'cat': 'cat1',
            'cat_name': 'cat_name1',
            'vector': '\x00\x00\x00@\x00\x00@@\x00\x00\x80@\x00\x00\xc0@\x00\x00\xe0@\x00\x00\x00A',  # noqa [2, 3, 4, 6, 7, 8]
        },
    ]
    assert is_equal_records(actual, expected)


def test_cluster_center_reducer_float(yt_stuff):
    yt_client = yt_stuff.get_yt_client()
    folder = '//test/cluster/cluster-center-reducer-float'
    yt_utils.create_folders([folder], yt_client)
    input_table = TablePath(ypath_join(folder, 'input_data'), sorted_by=['cat'])
    output_table = ypath_join(folder, 'output')
    yt_client.write_table(
        input_table,
        [
            {
                'cat': 12,
                'cat_name': 'Непознанное',
                'vector_b': '\x00\x00\xc0?\x9a\x99\x19@33S@',  # [1.5, 2.4, 3.3]
                'vector_m': '33\x83@ff\xa6@\x9a\x99\xc9@',  # [4.1 5.2 6.3]
            },
            {
                'cat': 12,
                'cat_name': 'Непознанное',
                'vector_b': '\xcd\xcc\xcc=\xcd\xccL>\x9a\x99\x99>',  # [0.1, 0.2, 0.3]
                'vector_m': '\xcd\xcc\xcc>\x00\x00\x00?\x9a\x99\x19?',  # [0.4 0.5 0.6]
            },
        ]
    )

    yt_client.run_reduce(
        clust_features.cluster_center_reduce, input_table, output_table,
        reduce_by='cat',
    )
    actual = list(yt_client.read_table(output_table))
    expected = [
        {
            'cat': 12,
            'cat_name': 'Непознанное',
            'vector': '\xcd\xcc\xcc?gf&@fff@\x00\x00\x90@ff\xb6@\xcd\xcc\xdc@'  # noqa [1.6 2.6. 3.6 4.5 5.7 6.9]
        },
    ]
    assert is_equal_records(actual, expected)


def test_get_cluster_centers_and_norms(yt_stuff):
    yt_client = yt_stuff.get_yt_client()
    folder = '//test/cluster/get-cluster-centers-and-norms'
    yt_utils.create_folders([folder], yt_client)
    centers_table = ypath_join(folder, 'centers-table')
    yt_client.write_table(
        centers_table,
        [
            {'cat': 5, 'cat_name': 'Интернет', 'vector': '0.72 -1.13 3.8 5.0'},
            {'cat': 12, 'cat_name': 'Непознанное', 'vector': '3.0 4.1 5.3 6.7'},
            {'cat': 15, 'cat_name': 'Музеи', 'vector': '0.22 -4.1 -3.2 -0.24'},
        ]
    )
    path_config = PathConfig()
    path_config.cluster_centers_table = centers_table
    path_config.cat2index = {5: 2, 12: 0, 15: 1}
    centers, norms = clust_features.get_cluster_centers_and_norms(
        path_config, yt_client)
    expected_centers = np.array([
        [3., 0.22, 0.72],
        [4.1, -4.1, -1.13],
        [5.3, -3.2, 3.8],
        [6.7, -0.24, 5.]])
    expected_norms = np.array([[9.93931587, 5.21114191, 6.42147179]])
    assert np.allclose(centers, expected_centers)
    assert np.allclose(norms, expected_norms)


# TODO: Fix
# def test_append_cluster_centers_mapper(yt_stuff):
#     yt_client = yt_stuff.get_yt_client()
#     folder = '//test/cluster/append-cluster-centers-mapper'
#     yt_utils.create_folders([folder], yt_client)
#     input_table = ypath_join(folder, 'input_table')
#     output_table = ypath_join(folder, 'output_table')
#     yt_client.write_table(
#         input_table,
#         [{'key': 'key1', 'features': '\xcd\xcc\x8c?\xcd\xcc\x0c@33S@\xcd\xcc\x8c@'}],
#     )
#     centers = np.array([
#         [3., 0.22, 0.72],
#         [4.1, -4.1, -1.13],
#         [5.3, -3.2, 3.8],
#         [6.7, -0.24, 5.]])
#     norms = np.array([[9.93931587, 5.21114191, 6.42147179]])
#     yt_client.run_map(
#         clust_features.AppendClusterCentersMapper(centers, norms),
#         input_table, output_table)
#     actual = list(yt_client.read_table(output_table))
#     expected = []
#     assert actual == expected


def test_hostname_bow_reducer(yt_stuff):
    yt_client = yt_stuff.get_yt_client()
    folder = '//test/cluster/hostname-bow-reducer'
    yt_utils.create_folders([folder], yt_client)
    input_table = TablePath(ypath_join(folder, 'input_table'), sorted_by=['key'])
    output_table = ypath_join(folder, 'output_table')
    yt_client.write_table(
        input_table,
        [
            {'key': 'key1', 'host': 'host1', 'counter': 1},
            {'key': 'key1', 'host': 'host2', 'counter': 3},
            {'key': 'key1', 'host': 'host1', 'counter': 8},
            {'key': 'key1', 'host': 'host2', 'counter': 1},
        ]
    )
    yt_client.run_reduce(
        clust_features.HostnameBowReducer('key'),
        input_table, output_table, reduce_by='key')
    expected = [
        {'key': 'key1', 'host': 'host1', 'counter': 9},
        {'key': 'key1', 'host': 'host2', 'counter': 4},
    ]
    actual = list(yt_client.read_table(output_table))
    assert is_equal_records(expected, actual)


def test_norm_s2v_mapper(yt_stuff):
    yt_client = yt_stuff.get_yt_client()
    folder = '//test/cluster/norms2vmapper'
    yt_utils.create_folders([folder], yt_client)
    input_table = ypath_join(folder, 'input_table')
    output_table = ypath_join(folder, 'output_table')
    yt_client.write_table(
        input_table,
        [{'some_key': 'key1', 'features': '\xcd\xcc\x8c?\xcd\xcc\x0c@33S@'}],
    )
    yt_client.run_map(
        clust_features.NormS2vMapper('some_key', 'new_key', 'features'),
        input_table,
        output_table,
    )
    expected = [{'new_key': 'key1', 'features': 'x\xd6\x88>x\xd6\x08?\xb4AM?'}]
    actual = list(yt_client.read_table(output_table))
    assert is_equal_records(expected, actual)
