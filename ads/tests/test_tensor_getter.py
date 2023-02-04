import pytest

from google.protobuf.message import DecodeError

from yabs.proto.tsar_pb2 import TTsarVector

from ads.bigkv.tensor_transport.lib.sb_getter import ConstantTensorGetter, BannerUserIdentityTensorGetter
from ads.bigkv.tensor_transport.lib.tensor_utils import flatten_tensor


def test_version_checker_no_checks():
    tensor_getter_without_checks = ConstantTensorGetter('asd', 'qwe', with_checks=False)
    tensor_getter_without_checks.get_version()

    tensor_getter_without_checks = ConstantTensorGetter(1, 'qwe', with_checks=False)
    tensor_getter_without_checks.get_version()


def test_version_checker_exception():
    tensor_getter = ConstantTensorGetter('asd', 'qwe')
    with pytest.raises(TypeError):
        tensor_getter.get_version()

    tensor_getter = ConstantTensorGetter(1, 'qwe')
    tensor_getter.get_version()


def test_tensor_checker_no_checks():
    tensor_getter_without_checks = ConstantTensorGetter(1, 'qwe', with_checks=False)
    tensor_getter_without_checks.get_tensor()

    tensor = TTsarVector()
    tensor_getter_without_checks = ConstantTensorGetter(1, tensor.SerializeToString(), with_checks=False)
    tensor_getter_without_checks.get_tensor()

    tensor.Factors.extend([123])
    tensor_getter_without_checks = ConstantTensorGetter(1, tensor.SerializeToString(), with_checks=False)
    tensor_getter_without_checks.get_tensor()


def test_tensor_checker():
    tensor_getter = ConstantTensorGetter(1, 'qwe')
    with pytest.raises(DecodeError):
        tensor_getter.get_tensor()

    tensor = TTsarVector()
    tensor_getter = ConstantTensorGetter(1, tensor.SerializeToString())
    with pytest.raises(AssertionError):
        tensor_getter.get_tensor()

    tensor.Factors.extend([123])
    tensor_getter = ConstantTensorGetter(1, tensor.SerializeToString())
    tensor_getter.get_tensor()


def test_banner_user_identity_tensor_getter():
    tensor_getter = BannerUserIdentityTensorGetter(dim=5, version=123)
    assert tensor_getter.get_version() == 123
    tensor = TTsarVector()
    tensor.ParseFromString(tensor_getter.get_tensor())
    expected_tensor = [0.0] * 25
    for i in range(5):
        expected_tensor[5 * i + i] = 1.0
    assert tensor.Factors == expected_tensor
