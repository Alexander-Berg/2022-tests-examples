import os

from ads_pytorch.hash_embedding.optim import create_optimizer
from ads_pytorch.hash_embedding.hash_embedding import (
    HashEmbedding,
    create_item,
    create_hash_table
)

os.environ["OMP_NUM_THREADS"] = "1"
import torch
import torch.nn
import numpy as np
import pytest
import math


def EmbeddingRMSPropNormOptimizer(params, **kwargs):
    return create_optimizer(params, "rmsprop_norm", **kwargs)


def cmp2float(x: float, y: float, eps: float = 2e-3):
    return abs(x - y) <= eps


def RMSPropNormEmbeddingHashTable(dim):
    return create_hash_table("rmsprop_norm", dim)


def RMSPropNormItem(dim):
    return create_item("rmsprop_norm", dim)


def RMSPropNormHalfEmbeddingHashTable(dim):
    return create_hash_table("rmsprop_norm_half", dim)


def RMSPropNormHalfItem(dim):
    return create_item("rmsprop_norm_half", dim)



# Simple reference

class TorchedRMSPropNormOptimizer(torch.optim.Optimizer):
    def __init__(self, params, lr=1e-3, beta=0.999, eps=1e-8, old_style_update=True):
        defaults = dict(lr=lr, beta=beta, eps=eps, old_style_update=old_style_update)
        super(TorchedRMSPropNormOptimizer, self).__init__(params, defaults)

    def step(self, closure=None):
        loss = None
        if closure is not None:
            loss = closure()

        for group in self.param_groups:
            for p in group['params']:
                if p.grad is None:
                    continue
                grad = p.grad.data
                if grad.is_sparse:
                    raise RuntimeError('Adam does not support sparse gradients, please consider SparseAdam instead')

                state = self.state[p]

                # State initialization
                if len(state) == 0:
                    state['step'] = torch.tensor(0, dtype=torch.int64)
                    state['exp_avg_sq'] = torch.zeros(p.size()[0], 1)

                beta = group['beta']
                state['step'] += 1
                bias_correction = 1 - beta ** int(state['step'])

                norm_square = (grad * grad).sum(dim=1).view_as(state['exp_avg_sq'])
                state['exp_avg_sq'] = beta * state['exp_avg_sq'] + (1 - beta) * norm_square
                dim = p.data.size()[-1]
                lr = - group['lr'] * math.sqrt(bias_correction) / (state['exp_avg_sq'] + group['eps']).sqrt()
                if group["old_style_update"]:
                    lr *= dim
                else:
                    lr *= math.sqrt(dim)
                p.data += lr * grad

        return loss


@pytest.fixture(
    params=[
        (RMSPropNormEmbeddingHashTable, RMSPropNormItem),
        (RMSPropNormHalfEmbeddingHashTable, RMSPropNormHalfItem),
    ],
    ids=["RMSPropNorm", "RMSPropNormHalf"]
)
def hash_cls_and_item(request):
    return request.param


@pytest.fixture(params=[True, False], ids=["OldStyle", "NewStyle"])
def old_style_update(request):
    return request.param


def test_expiration(hash_cls_and_item, old_style_update):
    hash_cls, _ = hash_cls_and_item
    ttl = 2
    hash_table = HashEmbedding(hash_cls(1))
    optim = EmbeddingRMSPropNormOptimizer(hash_table.parameters(), lr=1, beta2=0.999, eps=1e-8, ttl=ttl, old_style_update=old_style_update)
    for u in optim.updaters:
        assert u.ttl_counter == 0

    data = torch.LongTensor([1])
    data_len = torch.IntTensor([1])

    res = hash_table(data, data_len)
    assert float(res.data) == 0
    res.sum().backward()
    optim.step()
    optim.update_counter(1)
    for u in optim.updaters:
        assert u.ttl_counter == 1

    res = hash_table(data, data_len)
    assert float(res.data) != 0
    assert math.fabs(float(res.data) + 1) < 0.02
    assert math.fabs(float(hash_table.lookup_item(1).w)) - 1 < 0.02
    res.sum().backward()
    optim.step()

    optim.update_counter(100)
    res = hash_table(data, data_len)
    assert float(res.data) != 0
    assert math.fabs(float(res.data) + 2) < 0.02
    assert math.fabs(float(hash_table.lookup_item(1).w)) - 2 < 0.02
    res.sum().backward()
    optim.step()

    # Our item has been erased and then earned a gradient from previously initialized value
    # we can consider it as a biased init
    # Gradient for sum is equal to 1, we incorporate it, hence our item must be closed to 1
    # if it has been erased
    assert math.fabs(float(hash_table.lookup_item(1).w)) - 1 < 0.02


def test_massive_expiration(hash_cls_and_item, old_style_update):
    hash_cls, item_cls = hash_cls_and_item
    hash_table = HashEmbedding(hash_cls(1))
    optim = EmbeddingRMSPropNormOptimizer(hash_table.parameters(), lr=1, beta2=0.999, eps=1e-8, ttl=10, old_style_update=old_style_update)
    for u in optim.updaters:
        u.update_counter(1000)
        assert u.ttl_counter == 1000

    for i in range(100):
        item = item_cls(1)
        item.ttl_counter = 0
        hash_table.insert_item(i, item)

    for i in range(100):
        item = item_cls(1)
        item.ttl_counter = 999
        hash_table.insert_item(i + 100, item)

    data = torch.LongTensor(list(range(200)))
    data_len = torch.IntTensor([1] * 200)

    assert hash_table.size() == 200

    res = hash_table(data, data_len)
    res.sum().backward()
    optim.step()

    assert hash_table.size() == 200


def test_state_dict(hash_cls_and_item, old_style_update):
    hash_cls, _ = hash_cls_and_item
    ttl = 2
    hash_tables = [HashEmbedding(hash_cls(i + 1)) for i in range(10)]
    optim = EmbeddingRMSPropNormOptimizer(
        [
            {"params": hash_table.parameters(), "lr": 1.0 + i * 10, "beta1": 0.4 + i * 0.05}
            for i, hash_table in enumerate(hash_tables)
        ],
        lr=1, beta2=0.999, eps=1e-8, ttl=ttl,
        old_style_update=old_style_update
    )
    for u in optim.updaters:
        assert u.ttl_counter == 0

    data = torch.LongTensor([1])
    data_len = torch.IntTensor([1])

    for i, hash_table in enumerate(hash_tables):
        res = hash_table(data, data_len)
        res.sum().backward()
        optim.step()
    optim.update_counter(1)
    for u in optim.updaters:
        assert u.ttl_counter == 1

    state = optim.state_dict()
    optim2 = EmbeddingRMSPropNormOptimizer(
        [
            {"params": hash_table.parameters(), "lr": 5, "beta1": 0.3}
            for i, hash_table in enumerate(hash_tables)
        ],
        lr=5, beta2=0.8, eps=1.0, ttl=100500, old_style_update=old_style_update
    )
    for u in optim2.updaters:
        assert u.ttl_counter == 0
    optim2.load_state_dict(state)
    for i, group in enumerate(optim2.param_groups):
        assert group["lr"] == 1.0 + i * 10
        assert group["beta1"] == 0.4 + i * 0.05
        assert group["beta2"] == 0.999
        assert group["eps"] == 1e-8
        assert group["ttl"] == 2
        assert group["ttl_counter"] == 1

    for u in optim2.updaters:
        assert u.ttl_counter == 1


########################################################
#                  EQUATIONS TESTS                     #
########################################################

# Here we test two use-cases:
# 1. split jobs between threads
# 2. testing proper vectorization code


def update_equations_impl(hash_cls, num_threads, max_size_per_update_job, dim, old_style_update):
    items_count = 1000
    epoch_count = 15

    mul_tensor = torch.rand(1000, dim)
    mul_tensor.requires_grad = False

    hash_table = HashEmbedding(hash_cls(dim))
    is_half = hash_table.parameter_with_hash_table.hash_table.is_half()

    lr = 0.001
    if is_half:
        eps = 0
        beta2 = 0.999
    else:
        eps = 0
        beta2 = 0.999

    optim = EmbeddingRMSPropNormOptimizer(hash_table.parameters(), lr=lr, beta2=beta2, eps=eps, ttl=100, old_style_update=old_style_update)
    optim.num_threads = num_threads

    for i in range(items_count):
        # This will generate some items
        item = hash_table.lookup_item(i)
        # sanity check that adam items have non-trivial initialization (rest of test code relies on it)
        assert not torch.allclose(item.w, torch.zeros(dim))

    targets = torch.nn.init.normal_(torch.zeros(items_count))
    loss = torch.nn.MSELoss(reduction="mean")

    # Usual pytorch code
    usual_tensor = torch.zeros(items_count, dim)
    with torch.no_grad():
        for i in range(items_count):
            usual_tensor[i, :] = hash_table.lookup_item(i).w
    usual_tensor.requires_grad = True
    param = torch.nn.Parameter(usual_tensor)
    # PyTorch's rmsprop does not use bias correction, so we just disable momentum in adam
    optimizer = TorchedRMSPropNormOptimizer(
        [param],
        eps=eps,
        beta=beta2,
        lr=lr,
        old_style_update=old_style_update
    )

    # print("Usual optimization")
    usual_optimize_path = []
    for epoch in range(epoch_count):
        param.grad = None
        out = (param * mul_tensor).sum(dim=1)
        loss_val = loss(out, targets)
        usual_optimize_path.append(float(loss_val))
        loss_val.backward()
        optimizer.step()

    # Hash embedding code

    hash_optimizer = EmbeddingRMSPropNormOptimizer(hash_table.parameters(), lr=lr, beta2=beta2, eps=eps, ttl=100, old_style_update=old_style_update)
    hash_optimizer.num_threads = num_threads
    data = torch.LongTensor(list(range(items_count)))
    data_len = torch.IntTensor([1] * items_count)

    # print("HashTable optimization")
    hash_optimize_path = []
    for epoch in range(epoch_count):
        # hash embedding step
        tensor = hash_table(data, data_len)
        out = (tensor * mul_tensor).sum(dim=1)
        loss_val = loss(out, targets)
        hash_optimize_path.append(float(loss_val))
        loss_val.backward()
        hash_optimizer.step(max_size_per_update_job=max_size_per_update_job)

    if not is_half:
        for x, y in zip(usual_optimize_path, hash_optimize_path):
            assert cmp2float(x, y)
    else:
        # FIXME half tests will be soon
        pass


# need comprehensive test across all dims because of manual vectorization
@pytest.mark.parametrize("dim", list(range(1, 34)) + [100])
def test_different_dimensions(hash_cls_and_item, dim, old_style_update):
    update_equations_impl(
        hash_cls=hash_cls_and_item[0],
        num_threads=3,
        max_size_per_update_job=200000,
        dim=dim,
        old_style_update=old_style_update
    )
