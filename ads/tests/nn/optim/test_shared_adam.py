import torch
import pytest
import multiprocessing
from ads_pytorch.model_calcer.utils import preallocate_optimizer_buffers

from ads_pytorch.nn.optim.shared_adam import SharedAdam


def test_update_equations():
    torch.manual_seed(12345)
    weight = torch.randn(10, 10)
    gradient = torch.randn(10, 10)
    p1 = torch.nn.Parameter(weight.clone())
    p2 = torch.nn.Parameter(weight.clone())

    assert torch.allclose(p1.data, p2.data)

    optim = torch.optim.Adam([p1])
    shared_optim = SharedAdam([p2])

    p1.grad = gradient.clone()
    p2.grad = gradient.clone()

    optim.step()
    shared_optim.step()

    assert torch.allclose(p1.data, p2.data)


def test_proximal_regularization():
    torch.manual_seed(12345)
    p1 = torch.nn.Parameter(torch.randn(10, 10))

    shared_optim = SharedAdam([p1], weight_decay=1000000)  # this will blow if linearized
    p1.grad = torch.randn(10, 10)

    shared_optim.step()

    assert torch.all(p1.data < 1)


def _foo(param, grad, optim):
    param.grad = grad
    for _ in range(3):
        optim.step()


def test_shared():
    torch.manual_seed(12345)
    p1 = torch.nn.Parameter(torch.randn(10, 10))
    shared_optim = SharedAdam([p1], weight_decay=1000000)
    preallocate_optimizer_buffers([p1], [shared_optim])

    assert shared_optim.state[p1]['step'].is_shared()

    proc = multiprocessing.Process(
        target=_foo,
        args=(p1, torch.randn(10, 10), shared_optim),
        daemon=True
    )
    proc.start()
    proc.join()

    assert int(shared_optim.state[p1]['step']) == 4
