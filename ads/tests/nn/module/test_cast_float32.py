import torch
import pytest
from ads_pytorch.nn.module.cast_float32 import CastToFloat32


@pytest.mark.parametrize(
    "tensor",
    [
        torch.HalfTensor([0.345, 0.789]),
        torch.FloatTensor([0.345, 0.789]),
        torch.DoubleTensor([0.345, 0.789]),
    ],
    ids=[
        "Half",
        "Float",
        "Double"
    ]
)
def test_module(tensor):
    reference = tensor.clone().float()
    net = CastToFloat32()
    result: torch.Tensor = net(tensor)
    assert result.dtype == torch.float32
    assert torch.allclose(result, reference)
