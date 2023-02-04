import torch
import pytest
from ads_pytorch.nn.module.attention_mask import build_attention_mask


DEVICES = [torch.device("cpu")]
DEVICES.append(pytest.param(torch.device("cuda", 0), marks=pytest.mark.requires_cuda))


@pytest.fixture(params=DEVICES)
def device(request):
    return request.param


########################################################
#                  Zero-sized mask tensor              #
########################################################


def test_zero_sized(device):
    src = torch.tensor(1).to(device) * 5
    assert src.dim() == 0
    mask = build_attention_mask(tensor=src, seq_len=10)
    assert mask.device == device
    assert torch.all(~torch.logical_xor(mask, ~torch.BoolTensor([[0, 0, 0, 0, 0, 1, 1, 1, 1, 1]]).to(device)))


def test_zero_sizes_zero_seq_len(device):
    src = torch.tensor(1).to(device) * 0
    mask = build_attention_mask(tensor=src, seq_len=10)
    assert mask.device == device
    # Zero seq len hack test
    assert torch.all(~torch.logical_xor(mask, torch.zeros(1, 10, dtype=torch.bool, device=device)))


def test_zero_sizes_greater_seq_len(device):
    src = torch.tensor(1).to(device) * 50
    mask = build_attention_mask(tensor=src, seq_len=10)
    assert mask.device == device
    assert torch.all(~torch.logical_xor(mask, torch.ones(1, 10, dtype=torch.bool, device=device)))


def test_zero_sizes_exact_seq_len(device):
    src = torch.tensor(1).to(device) * 10
    mask = build_attention_mask(tensor=src, seq_len=10)
    assert mask.device == device
    assert torch.all(~torch.logical_xor(mask, torch.ones(1, 10, dtype=torch.bool, device=device)))


########################################################
#               One-dimensional mask tensor            #
########################################################


def test_one_dimensional(device):
    src = torch.IntTensor([1, 3]).to(device)
    mask = build_attention_mask(tensor=src, seq_len=4)
    assert mask.device == device
    reference = torch.BoolTensor([
        [1, 0, 0, 0],
        [1, 1, 1, 0]
    ]).to(device)
    assert torch.all(~torch.logical_xor(mask, reference))


def test_one_dimensional_zero_seqlen(device):
    src = torch.IntTensor([1, 3, 0]).to(device)
    mask = build_attention_mask(tensor=src, seq_len=4)
    assert mask.device == device
    reference = torch.BoolTensor([
        [1, 0, 0, 0],
        [1, 1, 1, 0],
        [0, 0, 0, 0],
    ]).to(device)
    assert torch.all(~torch.logical_xor(mask, reference))


def test_one_dimensional_exact_seq_size(device):
    src = torch.IntTensor([1, 3, 4]).to(device)
    mask = build_attention_mask(tensor=src, seq_len=4)
    assert mask.device == device
    reference = torch.BoolTensor([
        [1, 0, 0, 0],
        [1, 1, 1, 0],
        [1, 1, 1, 1],
    ]).to(device)
    assert torch.all(~torch.logical_xor(mask, reference))


def test_one_dimensional_greater_seq_size(device):
    src = torch.IntTensor([1, 105, 4]).to(device)
    mask = build_attention_mask(tensor=src, seq_len=4)
    reference = torch.BoolTensor([
        [1, 0, 0, 0],
        [1, 1, 1, 1],
        [1, 1, 1, 1],
    ]).to(device)
    assert torch.all(~torch.logical_xor(mask, reference))


########################################################
#               Two-dimensional mask tensor            #
########################################################

# For two-dimensional, we will just check zero seqlen hack and sizes check


@pytest.mark.parametrize("dtype", [torch.int8, torch.uint8, torch.int32, torch.int64, torch.float])
def test_two_dimensional(dtype, device):
    mask_data = [
        [0, 1, 1, 0],
        [0, 1, 1, 1],
        [1, 0, 0, 0],
        [1, 1, 1, 1]
    ]
    src = torch.tensor(mask_data, dtype=dtype).to(device)

    mask = build_attention_mask(tensor=src, seq_len=4)
    assert mask.device == device

    reference = torch.BoolTensor(mask_data).to(device)

    assert torch.all(~torch.logical_xor(mask, reference))


########################################################
#               Two-dimensional SeqLens tensor         #
########################################################


# See comment in code - we sometimes have ambiguity in key_masks sizes

@pytest.mark.parametrize("dtype", [torch.int8, torch.uint8, torch.int32, torch.int64, torch.float])
def test_two_dimensional_column_vector_like_bool(dtype, device):
    mask_data = [
        [0],
        [0],
        [1],
        [1]
    ]
    src = torch.tensor(mask_data, dtype=dtype).to(device)

    mask = build_attention_mask(tensor=src, seq_len=1)
    assert mask.device == device

    reference = torch.BoolTensor(mask_data).to(device)

    assert torch.all(~torch.logical_xor(mask, reference))


def test_two_dimensional_column_vector_seqlens(device):
    mask_data = [
        [5],
        [3],
        [0],
        [2]
    ]
    src = torch.tensor(mask_data, dtype=torch.int).to(device)

    mask = build_attention_mask(tensor=src, seq_len=5)
    assert mask.device == device

    reference = torch.BoolTensor([
        [1, 1, 1, 1, 1],
        [1, 1, 1, 0, 0],
        [0, 0, 0, 0, 0],
        [1, 1, 0, 0, 0]
    ]).to(device)

    assert torch.all(~torch.logical_xor(mask, reference))
