import torch
from ads_pytorch.model_calcer.utils.create_optimizer_cache import preallocate_optimizer_buffers


def test_buffers_allocated():
    model = torch.nn.Linear(10, 10)
    optim = torch.optim.Adam(model.parameters())
    preallocate_optimizer_buffers(list(model.parameters()), [optim])

    assert model.weight in optim.state
    assert model.bias in optim.state


def test_buffers_shared():
    model = torch.nn.Linear(10, 10)
    optim = torch.optim.Adam(model.parameters())
    preallocate_optimizer_buffers(list(model.parameters()), [optim])

    for p in model.parameters():
        for v in optim.state[p].values():
            if isinstance(v, torch.Tensor):
                assert v.is_shared()
