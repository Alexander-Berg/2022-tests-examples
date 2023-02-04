import pytest
import torch
try:
    import deepspeed
    from ads_pytorch.nn.optim.fused_adam import FusedAdam
    from deepspeed.ops.adam.fused_adam import FusedAdam as OriginalFusedAdam
    from ads_pytorch.tools.multiprocessing import get_multiprocessing_context
except (ImportError, ModuleNotFoundError):
    # will soon fix containers for CI tests and remove this if
    pytestmark = pytest.mark.skip("Deepspeed required to test fused adam")


def test_update():
    torch.manual_seed(12345)
    device = torch.device("cuda", 0)
    p = torch.nn.Parameter(torch.rand(10, 10, device=device))
    p.grad = torch.rand_like(p)

    with torch.no_grad():
        p2 = torch.nn.Parameter(p.clone())
        p2.grad = p.grad.clone()

    opt1 = FusedAdam([p])
    opt2 = OriginalFusedAdam([p2])

    opt1.step()
    opt2.step()

    assert torch.allclose(p, p2)


@pytest.mark.parametrize("adam_w", [True, False], ids=["AdamWOn", "AdamWOff"])
@pytest.mark.parametrize("set_grad_none", [True, False], ids=["GradNoneOn", "GradNoneOff"])
def test_state_dict_preserve_hardcoded_init_args(adam_w, set_grad_none):
    device = torch.device("cuda", 0)
    p = torch.nn.Parameter(torch.rand(10, 10, device=device))
    p.grad = torch.rand_like(p)

    opt = FusedAdam([p])
    opt.step()

    opt2 = FusedAdam([p])
    opt2.load_state_dict(opt.state_dict())

    assert opt2.adam_w_mode == opt.adam_w_mode
    assert opt2.set_grad_none == opt.set_grad_none


@pytest.mark.parametrize("zero_steps", [0, 2, 10])
def test_zero_lr_steps(zero_steps):
    device = torch.device("cuda", 0)
    p = torch.nn.Parameter(torch.rand(10, 10, device=device))
    p.grad = torch.rand_like(p)
    with torch.no_grad():
        old_value = p.clone()

    opt = FusedAdam([p], zero_lr_steps=zero_steps)

    for _ in range(zero_steps):
        opt.step()
        assert torch.allclose(old_value, p)

    opt.step()
    assert not torch.allclose(old_value, p)


def _mp_foo(p: torch.nn.Parameter, optim: torch.optim.Optimizer, res_queue):
    p.grad = torch.rand_like(p)
    with torch.no_grad():
        reference_p = torch.nn.Parameter(p.clone())
        reference_p.grad = p.grad.clone()

    optim_reference = OriginalFusedAdam([reference_p], lr=10.)

    optim.step()
    optim_reference.step()

    res_queue.put(torch.allclose(p, reference_p))


def test_send_optimizer_to_subprocess():
    torch.manual_seed(98663)
    p = torch.nn.Parameter(torch.rand(10, 10, device=torch.device("cuda", 0)))
    optim = FusedAdam([p], lr=10.)
    ctx = get_multiprocessing_context()
    res_queue = ctx.SimpleQueue()
    proc = ctx.Process(target=_mp_foo, kwargs=dict(p=p, optim=optim, res_queue=res_queue))
    proc.start()

    result = res_queue.get()
    assert result
