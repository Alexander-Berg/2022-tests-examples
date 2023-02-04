import torch
import pytest
import os
import yaml
from typing import List
from ads_pytorch_unitednn.split_inputs import SplitConcatFunction
from unitednn.config import ModelConfig
from unitednn.modules.common import DEFAULT_PYTORCH_MODULES_REGISTRY
from unitednn.utils import DenseTensor


CUR_MODULE_REGISTRY = DEFAULT_PYTORCH_MODULES_REGISTRY.copy()


#############################################################################
#                              Implementation test                          #
#############################################################################


def total_sizes_from_slices(slices: List[List[int]]) -> List[int]:
    result = [0] * len(slices[0])
    for slice in slices:
        for i, x in enumerate(slice):
            result[i] += x
    return result


@pytest.fixture(params=["cpu", "cuda"])
def device(request):
    device_type = request.param
    if device_type == "cpu":
        return torch.device("cpu")
    else:
        return torch.device(device_type, 0)


@pytest.mark.parametrize(
    "tensors",
    [
        [torch.rand(10, 10)],
        [
            torch.rand(10, 10),
            torch.rand(10, 5),
            torch.rand(10, 16)
        ]
    ],
    ids=[
        "SingleTensor",
        "MultipleTensor"
    ]
)
def test_single_split(tensors, device):
    # clone to avoid side effects from test to tet
    tensors = [x.clone().to(device).requires_grad_(True) for x in tensors]
    slices = [[x.size()[-1]] for x in tensors]

    # forward
    result = SplitConcatFunction.apply(slices, *tensors)
    assert len(result) == 1
    result = result[0]
    assert result.requires_grad
    assert torch.allclose(result, torch.cat(tensors, dim=-1))

    # backward
    gradient = torch.rand_like(result)
    torch.autograd.backward(tensors=[result], grad_tensors=[gradient])

    for tensor in tensors:
        assert tensor.grad is not None

    for grad, tensor in zip(gradient.split_with_sizes([x[0] for x in slices], dim=-1), tensors):
        assert torch.allclose(tensor.grad, grad)


def test_multiple_split(device):
    tensors = [
        torch.rand(10, 9, device=device, requires_grad=True),
        torch.rand(10, 6, device=device, requires_grad=True),
        torch.rand(10, 12, device=device, requires_grad=True)
    ]

    slices = [
        [3, 2, 4],
        [1, 2, 3],
        [6, 1, 5]
    ]

    # forward
    result = SplitConcatFunction.apply(slices, *tensors)

    assert len(result) == 3
    assert all(x.requires_grad for x in result)
    to_cat = [[], [], []]
    for tensor, slice in zip(tensors, slices):
        for lst, split in zip(to_cat, tensor.split_with_sizes(slice, dim=-1)):
            lst.append(split)

    concatted_references = [torch.cat(x, dim=-1) for x in to_cat]
    for reference, res in zip(concatted_references, result):
        assert torch.allclose(reference, res)

    # backward
    gradients = [torch.rand_like(x) for x in result]
    torch.autograd.backward(tensors=result, grad_tensors=gradients)

    to_cat = [[], [], []]
    gradient_slices = torch.tensor(slices).t().tolist()
    for grad, slice in zip(gradients, gradient_slices):
        for lst, split in zip(to_cat, grad.split_with_sizes(slice, dim=-1)):
            lst.append(split)

    res_gradients = [torch.cat(x, dim=-1) for x in to_cat]
    for tensor, grad in zip(tensors, res_gradients):
        assert torch.allclose(tensor.grad, grad)


#############################################################################
#                                Layer test                                 #
#############################################################################


@pytest.fixture
def config():
    path = os.path.join(os.path.dirname(__file__), "fixtures", "split_concat.yaml")
    with open(path, "rt") as f:
        config_dct = yaml.safe_load(f)
    return ModelConfig(config_dct, debug=True, registry=CUR_MODULE_REGISTRY)


@pytest.fixture
def input_dict(device):
    batch_size = 10
    return {
        "x1": DenseTensor(torch.ones(batch_size, 8, device=device)),
        "x2": DenseTensor(torch.ones(batch_size, 4, device=device) * 2),
        "x3": DenseTensor(torch.ones(batch_size, 12, device=device) * 3)
    }


@pytest.mark.parametrize("name", ["concat_float", "concat_int"])
def test_concat_config(config, input_dict, name):
    model = config.create(name=name)
    result = model(input_dict)
    assert len(result) == 1
    assert torch.allclose(result[0], torch.cat([input_dict[k] for k in ["x1", "x2", "x3"]], dim=-1))


@pytest.mark.parametrize(
    ["name", "reference"],
    [
        # 0.7-0.3 is 6-2
        ("split_float", [
            torch.cat([torch.full((10, 6), 1.), torch.full((10, 3), 2.), torch.full((10, 9), 3.)], dim=-1),
            torch.cat([torch.full((10, 2), 1.), torch.full((10, 1), 2.), torch.full((10, 3), 3.)], dim=-1)
        ]),
        ("split_float_four", [
            torch.cat([torch.full((10, 5), 1.), torch.full((10, 1), 2.), torch.full((10, 9), 3.)], dim=-1),
            torch.cat([torch.full((10, 1), 1.), torch.full((10, 1), 2.), torch.full((10, 1), 3.)], dim=-1),
            torch.cat([torch.full((10, 1), 1.), torch.full((10, 1), 2.), torch.full((10, 1), 3.)], dim=-1),
            torch.cat([torch.full((10, 1), 1.), torch.full((10, 1), 2.), torch.full((10, 1), 3.)], dim=-1)
        ]),
        ("split_int", [
            torch.cat([torch.full((10, 6), 1.), torch.full((10, 3), 2.), torch.full((10, 9), 3.)], dim=-1),
            torch.cat([torch.full((10, 2), 1.), torch.full((10, 1), 2.), torch.full((10, 3), 3.)], dim=-1)
        ])
    ]
)
def test_nontrivial_split_config(config, input_dict, name, reference):
    # avoid side effects between parametrized tests
    with torch.no_grad():
        input_dict = {key: value.clone().requires_grad_(True) for key, value in input_dict.items()}
        reference = [x.clone() for x in reference]

    model = config.create(name=name)
    result = model(input_dict)
    assert len(result) == len(reference)
    for res, ref in zip(result, reference):
        assert torch.allclose(res.cpu(), ref.cpu())

    with torch.no_grad():
        gradients = [x.clone() for x in result]
    torch.autograd.backward(tensors=result, grad_tensors=gradients)
    for value in input_dict.values():
        assert torch.allclose(value, value.grad)


@pytest.mark.parametrize(
    "name",
    [
        "split_int_not_divisible",
        "split_float_proportion_sum",
        "split_type_mixture",
        "split_negative_int",
        "split_zero_int",
        "split_negative_float",
        "split_zero_float",
        "split_string_in_argument_list",
        "split_string_argument",
        "split_unconcatable"
    ]
)
def test_error_config(config, input_dict, name):
    with pytest.raises((ValueError, AssertionError)):
        model = config.create(name=name)
        model(input_dict)
