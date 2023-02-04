from ads_pytorch.nn.module.rezero import (
    calc_merged_linear_rezero
)
import torch
import pytest
from ads_pytorch.nn.module.orthogonal_linear import OrthogonalLinear


###################################################################
#                  TEST MERGED LINEAR LAYER OPTIMIZATION          #
###################################################################


@pytest.mark.parametrize(
    "linear_cls",
    [
        torch.nn.Linear,
        OrthogonalLinear
    ],
    ids=["Linear", "OrthogonalLinear"]
)
@pytest.mark.parametrize("bias", [True, False], ids=["Bias", "NoBias"])
def test_merged_linear_rezero(linear_cls, bias):
    torch.manual_seed(27358265)
    # Just check that all gradients are same
    inputs = torch.rand(1000, 100)
    targets = torch.rand(1000, 1)

    model = linear_cls(100, 1, bias=bias)
    model_copy = linear_cls(100, 1, bias=bias)
    model_copy.load_state_dict(model.state_dict())

    # Don't make it zero to have non-zero grads in other params
    rezero1 = torch.nn.Parameter(torch.ones(1) * 1e-1)
    rezero2 = torch.nn.Parameter(torch.ones(1) * 1e-1)

    pred1 = rezero1 * model(inputs)
    pred2 = calc_merged_linear_rezero(tensor=inputs, module=model_copy, rezero=rezero2)

    torch.nn.functional.mse_loss(pred1, targets, reduction="mean").backward()
    torch.nn.functional.mse_loss(pred2, targets, reduction="mean").backward()

    for p1, p2 in zip(model.parameters(), model_copy.parameters()):
        assert torch.allclose(p1.grad, p2.grad)
        assert not torch.allclose(p1.grad, torch.zeros_like(p1.grad))

    assert torch.allclose(rezero1.grad, rezero2.grad)
    assert not torch.allclose(rezero1.grad, torch.zeros_like(rezero1.grad))
