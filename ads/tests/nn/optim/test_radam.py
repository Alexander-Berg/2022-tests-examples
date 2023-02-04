import torch
import pytest
import multiprocessing
from ads_pytorch.model_calcer.utils import preallocate_optimizer_buffers

from ads_pytorch.nn.optim.radam import RAdam


def test_zero_lr_steps():
    torch.manual_seed(12345)
    weight = torch.randn(10, 10)
    gradient = torch.randn(10, 10)
    p1 = torch.nn.Parameter(weight.clone())
    p2 = torch.nn.Parameter(weight.clone())

    assert torch.allclose(p1.data, p2.data)

    zero_step_optim = RAdam([p2], zero_lr_steps=2)

    def _step():
        zero_step_optim.zero_grad()
        p2.grad = gradient.clone()
        zero_step_optim.step()

    for _ in range(2):
        _step()
        assert torch.allclose(p1.data, p2.data)

    _step()
    assert not torch.allclose(p1.data, p2.data)


def _foo(param, grad, optim):
    param.grad = grad
    for _ in range(3):
        optim.step()


def test_shared():
    torch.manual_seed(12345)
    p1 = torch.nn.Parameter(torch.randn(10, 10))
    shared_optim = RAdam([p1], zero_lr_steps=1)
    preallocate_optimizer_buffers([p1], [shared_optim])

    assert shared_optim.state[p1]['step'].is_shared()

    proc = multiprocessing.Process(
        target=_foo,
        args=(p1, torch.randn(10, 10), shared_optim),
        daemon=True
    )
    proc.start()
    proc.join()

    assert int(shared_optim.state[p1]['step']) == 3
    assert int(shared_optim.state[p1]['global_step']) == 4
