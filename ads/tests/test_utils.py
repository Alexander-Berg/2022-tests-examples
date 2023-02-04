from ads.bigkv.tensor_transport.lib.utils import (
    tensor_table_to_dict,
    dict_to_tensor_table,
    merge_tensor_tables
)
from ads.bigkv.tensor_transport.lib.tensor_utils import flatten_tensor
import numpy as np

from .data import get_table_data


def test_tensor_table_to_dict(local_yt, tensor_table):
    expected_table = {
        row['Version']: row['Vector']
        for row in get_table_data()['data']
    }
    real_table = tensor_table_to_dict(local_yt.get_client(), tensor_table)

    assert expected_table == real_table


def test_dict_to_tensor_table(local_yt):
    yt_client = local_yt.get_client()
    expected_table = {1: 'asd', 2: 'qwe', '123': 'jlndfgjl'}

    dict_to_tensor_table(expected_table, yt_client, '//home/bs/test_dict_to_tensor_table')
    real_table = tensor_table_to_dict(yt_client, '//home/bs/test_dict_to_tensor_table')

    assert expected_table == real_table


def test_merge_tensor_tables():
    main_table = {1: 'asd', 2: 'qwe', 3: 'zxc'}
    patch_table = {1: 'asd2', 2: 'qwe', 4: 'klnfg', 5: 'ret'}
    expected_table = {1: 'asd2', 2: 'qwe', 3: 'zxc', 4: 'klnfg', 5: 'ret'}
    has_update, real_table = merge_tensor_tables(main_table, patch_table)

    assert has_update == True
    assert expected_table == real_table


def test_merge_tensor_tables_no_update():
    main_table = {1: 'asd', 2: 'qwe', 3: 'zxc'}
    patch_table = {1: 'asd', 3: 'zxc'}
    has_update, real_table = merge_tensor_tables(main_table, patch_table)

    assert has_update == False
    assert main_table == real_table


def test_flatten_vector():
    # https://st.yandex-team.ru/BSSERVER-2189 <- тут написан обман, на самом деле
    # Vector - TsarVector одномерный вектор, где T[i][j][k] = V[i * d2 * d3 + j * d3 + k]
    # dim(banner) = d1, dim(user) = d2, dim(page) = d3
    # Пруфы: https://a.yandex-team.ru/arc/trunk/arcadia/yabs/server/libs/server/tsar_computations.cpp?blame=true&rev=7161390#L187
    T = np.arange(3 * 4 * 5, dtype=np.float32).reshape(3, 4, 5)
    V = flatten_tensor(T)
    banne_dim, user_dim, page_dim = T.shape

    for i in range(banne_dim):
        for j in range(user_dim):
            for k in range(page_dim):
                assert T[i, j, k] == V[i * user_dim * page_dim + j * page_dim + k]
