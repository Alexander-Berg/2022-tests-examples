import base64
import json

import brotli
import numpy as np
import pytest
import torch
import math

from ads_pytorch.tools.iterator_tools import async_batch_iterator
from ads_pytorch.cpp_lib import libcpp_lib


def serialize_array(arr: np.ndarray) -> str:
    return json.dumps({
        "dtype": str(arr.dtype),
        "size": list(arr.shape),
        "data": base64.b64encode(brotli.compress(arr.tostring())).decode('utf-8')
    })


@pytest.fixture
def array():
    arr = np.zeros(100, np.int32)
    return arr, serialize_array(arr)


@pytest.fixture
def dict_rec():
    return libcpp_lib.DictionaryLikeRecord({"some_tensor": torch.LongTensor([1])})


def test_dictionary_like_record_add(dict_rec):
    dict_rec["some_tensor2"] = torch.FloatTensor([1.5])
    assert dict_rec["some_tensor2"] == 1.5


def test_dictionary_like_record(dict_rec):
    assert dict_rec["some_tensor"] == 1
    with pytest.raises(Exception):
        dict_rec["some_tensor2"]


###########################################################
#                   TEST MERGING RECORDS                  #
###########################################################


def test_merge_records():
    rec1 = libcpp_lib.DictionaryLikeRecord({
        "tensor1": torch.LongTensor([1, 2, 3]),
        "tensor2": torch.FloatTensor([[1, 2, 3], [4, 5, 6]])
    })
    rec2 = libcpp_lib.DictionaryLikeRecord({
        "tensor1": torch.LongTensor([-1, -2, -3]),
        "tensor2": torch.FloatTensor([[-1, -2, -3], [-4, -5, -6]])
    })
    merged_record = libcpp_lib.merge_records([rec1, rec2])
    float_reference = torch.FloatTensor([
        [1, 2, 3],
        [4, 5, 6],
        [-1, -2, -3],
        [-4, -5, -6]
    ])
    assert torch.allclose(merged_record["tensor1"], torch.LongTensor([1, 2, 3, -1, -2, -3]))
    assert torch.allclose(merged_record["tensor2"], float_reference)
    assert len(merged_record) == 2


def test_merge_one_record():
    rec1 = libcpp_lib.DictionaryLikeRecord({
        "tensor1": torch.LongTensor([1, 2, 3]),
        "tensor2": torch.FloatTensor([[1, 2, 3], [4, 5, 6]])
    })
    merged_record = libcpp_lib.merge_records([rec1])
    float_reference = torch.FloatTensor([
        [1, 2, 3],
        [4, 5, 6]
    ])
    assert torch.allclose(merged_record["tensor1"], torch.LongTensor([1, 2, 3]))
    assert torch.allclose(merged_record["tensor2"], float_reference)
    assert len(merged_record) == 2


def test_merge_no_records():
    merged_record = libcpp_lib.merge_records([])
    assert len(merged_record) == 0
