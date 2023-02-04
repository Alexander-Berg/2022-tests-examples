from os import path
import time

from ads.bigkv.tensor_transport.lib.sb_getter import ConstantTensorGetter, get_new_tensors
from ads.bigkv.tensor_transport.lib.add_new_tensors import add_new_tensors
from ads.bigkv.tensor_transport.lib.utils import tensor_table_to_dict


def get_result_and_new_dump(yt_client, table_path, dumps_path):
    new_dump_path = max(path.join(dumps_path, table) for table in yt_client.list(dumps_path))

    return (
        tensor_table_to_dict(yt_client, table_path),
        tensor_table_to_dict(yt_client, new_dump_path),
        new_dump_path,
    )


def test_tensor_table_to_dict(local_yt, tensor_table):
    dumps_path = '//home/bs/test_tensor_table_to_dict_dumps'
    yt_client = local_yt.get_client()

    tmp_table = '//home/bs/test_tensor_table_to_dict_tensor_table'
    yt_client.copy(tensor_table, tmp_table)
    tensor_table = tmp_table

    add_new_tensors_wrapper = lambda new_tensors: add_new_tensors(
        client=yt_client,
        table_path=tensor_table,
        dump_path=dumps_path,
        new_tensors=new_tensors
    )
    result_0 = tensor_table_to_dict(yt_client, tensor_table)

    # modify 1 tensor, add 2 tensor
    result_path, dump_path = add_new_tensors_wrapper(
        get_new_tensors([
            ConstantTensorGetter(1, 'First tensor 2', with_checks=False),
            ConstantTensorGetter(2, 'Second tensor 1', with_checks=False),
        ])
    )
    result_1, dump_1, dump_path_1 = get_result_and_new_dump(yt_client, tensor_table, dumps_path)
    assert result_1 == tensor_table_to_dict(yt_client, result_path)
    assert dump_path == dump_path_1
    assert dump_1 == result_0, 'Current dump should be equal previous result'
    assert result_1 == {
        1: 'First tensor 2',
        2: 'Second tensor 1',
        3: 'Third tensor 1',
    }

    # modify 1 and 2 tensor, add 10 tensor
    time.sleep(1) # dumps index by seconds, sleep 1 second to avoid overriding
    result_path, dump_path = add_new_tensors_wrapper(
        get_new_tensors([
            ConstantTensorGetter(1, 'First tensor 3', with_checks=False),
            ConstantTensorGetter(2, 'Second tensor 1', with_checks=False),
            ConstantTensorGetter(10, 'Tenth tensor 1', with_checks=False),
        ])
    )
    result_2, dump_2, dump_path_2 = get_result_and_new_dump(yt_client, tensor_table, dumps_path)
    assert result_2 == tensor_table_to_dict(yt_client, result_path)
    assert dump_path == dump_path_2
    assert dump_2 == result_1, 'Current dump should be equal previous result'
    assert result_2 == {
        1: 'First tensor 3',
        2: 'Second tensor 1',
        3: 'Third tensor 1',
        10: 'Tenth tensor 1',
    }
    assert dump_path_2 > dump_path_1, 'Dumps should be time ordered'

    # no tensors
    time.sleep(1) # dumps index by seconds, sleep 1 second to avoid overriding
    result_path, dump_path = add_new_tensors_wrapper(
        get_new_tensors([])
    )
    result_3, dump_3, dump_path_3 = get_result_and_new_dump(yt_client, tensor_table, dumps_path)
    assert result_3 == tensor_table_to_dict(yt_client, result_path)
    assert dump_path == None
    assert dump_3 == dump_2, 'Here should not be any new dumps due to no changes in table'
    assert dump_path_3 == dump_path_2, 'Here should not be any new dumps due to no changes in table'
    assert result_3 == result_2

    # no differences
    time.sleep(1) # dumps index by seconds, sleep 1 second to avoid overriding
    result_path, dump_path = add_new_tensors_wrapper(
        get_new_tensors([
            ConstantTensorGetter(1, 'First tensor 3', with_checks=False),
            ConstantTensorGetter(2, 'Second tensor 1', with_checks=False),
        ])
    )
    result_4, dump_4, dump_path_4 = get_result_and_new_dump(yt_client, tensor_table, dumps_path)
    assert result_4 == tensor_table_to_dict(yt_client, result_path)
    assert dump_path == None
    assert dump_4 == dump_3, 'Here should not be any new dumps due to no changes in table'
    assert dump_path_4 == dump_path_3, 'Here should not be any new dumps due to no changes in table'
    assert result_4 == result_3
