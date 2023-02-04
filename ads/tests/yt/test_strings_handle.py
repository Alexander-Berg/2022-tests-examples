import torch
from ads_pytorch.cpp_lib import libcpp_lib
import pytest


def StringHandle(value) -> libcpp_lib.StringHandle:
    return libcpp_lib.StringHandle(value)


@pytest.mark.parametrize(
    "src",
    [
        b"",
        b"123",
        torch.zeros(0, dtype=torch.int8),
        torch.zeros(0, dtype=torch.uint8),
        torch.zeros(5, dtype=torch.int8),
        torch.zeros(5, dtype=torch.uint8)
    ],
    ids=[
        "EmptyBytes", "Bytes",
        "EmptyInt8Tensor", "Int8Tensor",
        "EmptyUInt8Tensor", "UInt8Tensor"
    ]
)
def test_constructor(src):
    handle = StringHandle(src)
    if isinstance(src, bytes):
        assert bytes(handle) == src
    elif isinstance(src, torch.Tensor):
        assert bytes(handle) == b"\x00" * src.numel()
    else:
        raise TypeError("Unknown type")


@pytest.mark.parametrize("src", [b"12345", torch.ones(100, dtype=torch.int8)], ids=["Bytes", "Tensor"])
def test_copy_constructible(src):
    other = bytes(StringHandle(StringHandle(src)))
    if isinstance(src, bytes):
        assert other == src
    elif isinstance(src, torch.Tensor):
        assert other == bytes(StringHandle(src))
    else:
        raise TypeError("Unknown srcType")


@pytest.mark.parametrize(
    "to_join",
    [
        b"",
        b"1",
        b"12345",
        b"7" * 1000
    ],
    ids=["Empty", "One", "Five", "ExactSize"]
)
def test_copy_to_tensor_one(to_join):
    result = torch.empty(1000, dtype=torch.int8)
    copier = libcpp_lib.StringsToTensorCopier(result)
    copier.push_string(StringHandle(to_join))
    copier()
    assert bytes(StringHandle(result.narrow(0, 0, len(to_join)))) == to_join


def test_copy_to_tensor_one_cannot_fit():
    result = torch.empty(10, dtype=torch.int8)
    copier = libcpp_lib.StringsToTensorCopier(result)
    copier.push_string(StringHandle(b"1" * 11))
    with pytest.raises(RuntimeError):
        copier()


def test_copy_to_tensor_multiple():
    result = torch.empty(15, dtype=torch.int8)
    data = [b"", b"1", b"2345", b"", b"6789abc", b""]
    reference = b"".join(data)
    copier = libcpp_lib.StringsToTensorCopier(result)
    for x in data:
        copier.push_string(StringHandle(x))
    copier()
    assert bytes(StringHandle(result.narrow(0, 0, len(reference)))) == reference


def test_copy_empty():
    result = torch.empty(15, dtype=torch.int8)
    data = [b""] * 100
    reference = b"".join(data)
    copier = libcpp_lib.StringsToTensorCopier(result)
    for x in data:
        copier.push_string(StringHandle(x))
    copier()
    assert bytes(StringHandle(result.narrow(0, 0, len(reference)))) == reference


def test_copy_to_tensor_exceed_size():
    result = torch.empty(15, dtype=torch.int8)
    data = [b"", b"1", b"2345", b"", b"6789abc", b"", b"1" * 10000]
    copier = libcpp_lib.StringsToTensorCopier(result)
    for x in data:
        copier.push_string(StringHandle(x))
    with pytest.raises(RuntimeError):
        copier()
