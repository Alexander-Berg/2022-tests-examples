"""
FIXME
This file exists because authors of library are VERY lazy to maintain two testing contours
outside of ya.make builds

It's much simpler to make only python tests through bindings - less pain in integrating
out tests with arcadia's
"""

import pytest
import torch
import math
from ads_pytorch.cpp_lib import libcpp_lib


@pytest.mark.parametrize("dim", list(range(200)))
def test_l2_norm(dim):
    precision = 3

    torch.manual_seed(71)
    tensor = torch.rand(dim)
    reference = (tensor * tensor).sum().item()
    value = libcpp_lib.simd_vector_norm(tensor)
    assert round(reference, precision) == round(value, precision)


@pytest.mark.parametrize("dim", list(range(200)))
@pytest.mark.parametrize("res_dtype", [torch.float32, torch.float16], ids=["ResultFloat", "ResultHalf"])
@pytest.mark.parametrize("v1_dtype", [torch.float32, torch.float16], ids=["V1Float", "V1Half"])
@pytest.mark.parametrize("v2_dtype", [torch.float32, torch.float16], ids=["V2Float", "V2Half"])
def test_vector_add(dim, res_dtype, v1_dtype, v2_dtype):
    torch.manual_seed(71)
    result = torch.empty(dim, dtype=res_dtype)
    v1 = torch.rand(dim, dtype=v1_dtype)
    v2 = torch.rand(dim, dtype=v2_dtype)

    libcpp_lib.simd_add(result, v1, v2)

    assert torch.allclose(result, (v1.float() + v2.float()).to(res_dtype))
