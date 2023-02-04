import os

from ads_pytorch.hash_embedding.optim import create_optimizer
from ads_pytorch.hash_embedding.hash_embedding import (
    HashEmbedding,
    create_hash_table, create_item
)

os.environ["OMP_NUM_THREADS"] = "1"
import torch
import torch.nn
import numpy as np
import pytest
import math


def cmp2float(x: float, y: float, eps: float = 2e-3):
    return abs(x - y) <= eps


def EmbeddingRMSPropOptimizer(params, **kwargs):
    return create_optimizer(params, "rmsprop", **kwargs)


def RMSPropEmbeddingHashTable(dim):
    return create_hash_table("rmsprop", dim)


def RMSPropItem(dim):
    return create_item("rmsprop", dim)


def RMSPropHalfEmbeddingHashTable(dim):
    return create_hash_table("rmsprop_half", dim)


def RMSPropHalfItem(dim):
    return create_item("rmsprop_half", dim)


@pytest.mark.parametrize(
    "hash_cls",
    [
        RMSPropEmbeddingHashTable,
        RMSPropHalfEmbeddingHashTable,
    ],
    ids=["RMSProp", "RMSPropHalf"]
)
def test_expiration(hash_cls):
    ttl = 2
    hash_table = HashEmbedding(hash_cls(1))
    optim = EmbeddingRMSPropOptimizer(hash_table.parameters(), lr=1, beta2=0.999, eps=1e-8, ttl=ttl)
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


@pytest.mark.parametrize(
    ["hash_cls", "item_cls"],
    [
        (RMSPropEmbeddingHashTable, RMSPropItem),
        (RMSPropHalfEmbeddingHashTable, RMSPropHalfItem),
    ],
    ids=["RMSProp", "RMSPropHalf"]
)
def test_massive_expiration(hash_cls, item_cls):
    hash_table = HashEmbedding(hash_cls(1))
    optim = EmbeddingRMSPropOptimizer(hash_table.parameters(), lr=1, beta2=0.999, eps=1e-8, ttl=10)
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


@pytest.mark.parametrize(
    ["hash_cls", "item_cls"],
    [
        (RMSPropEmbeddingHashTable, RMSPropItem),
        (RMSPropHalfEmbeddingHashTable, RMSPropHalfItem),
    ],
    ids=["RMSProp", "RMSPropHalf"]
)
@pytest.mark.parametrize('size', [1, 4, 1000])
@pytest.mark.parametrize('num_threads', [1, 3, 5, 7])
def test_parallel_update(hash_cls, item_cls, num_threads, size):
    is_half: bool = item_cls == RMSPropHalfItem
    atol = 1e-8 if not is_half else 1e-4
    beta1 = 0.9
    beta2 = 0.999

    hash_table = HashEmbedding(hash_cls(1))
    optim = EmbeddingRMSPropOptimizer(hash_table.parameters(), lr=1, beta2=beta2, eps=1e-8, ttl=100)
    optim.num_threads = num_threads

    data = torch.from_numpy(np.arange(size, dtype=np.int64))
    data_len = torch.from_numpy(np.ones(size, dtype=np.int32))

    res = hash_table(data, data_len)
    res.sum().backward()
    optim.step()

    assert hash_table.size() == size

    for i in range(size):
        elem = hash_table.lookup_item(i)
        assert math.fabs(elem.w + 1) <= 0.02
        # !!! We have slightly optimized equations for adam
        assert torch.allclose(elem.second_moment * (1 - beta2), torch.FloatTensor([0.001]), atol=atol)

    res = hash_table(data, data_len)
    res.sum().backward()
    optim.step()

    assert hash_table.size() == size

    for i in range(size):
        elem = hash_table.lookup_item(i)
        assert math.fabs(elem.w + 2) <= 0.02
        # !!! We have slightly optimized equations for adam
        assert torch.allclose(elem.second_moment * (1 - beta2), torch.FloatTensor([0.001999]), atol=atol)


@pytest.mark.parametrize(
    "hash_cls",
    [
        RMSPropEmbeddingHashTable,
        RMSPropHalfEmbeddingHashTable,
    ],
    ids=["RMSProp", "RMSPropHalf"]
)
def test_state_dict(hash_cls):
    ttl = 2
    hash_tables = [HashEmbedding(hash_cls(i + 1)) for i in range(10)]
    optim = EmbeddingRMSPropOptimizer(
        [
            {"params": hash_table.parameters(), "lr": 1.0 + i * 10, "beta1": 0.4 + i * 0.05}
            for i, hash_table in enumerate(hash_tables)
        ],
        lr=1, beta2=0.999, eps=1e-8, ttl=ttl
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
    optim2 = EmbeddingRMSPropOptimizer(
        [
            {"params": hash_table.parameters(), "lr": 5, "beta1": 0.3}
            for i, hash_table in enumerate(hash_tables)
        ],
        lr=5, beta2=0.8, eps=1.0, ttl=100500
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


def update_equations_impl(hash_cls, num_threads, max_size_per_update_job, dim):
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

    optim = EmbeddingRMSPropOptimizer(hash_table.parameters(), lr=lr, beta2=beta2, eps=eps, ttl=100)
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
    optimizer = torch.optim.Adam(
        [param],
        eps=eps,
        betas=(0, beta2),
        lr=lr,
        weight_decay=.0,
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

    hash_optimizer = EmbeddingRMSPropOptimizer(hash_table.parameters(), lr=lr, beta2=beta2, eps=eps, ttl=100)
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
@pytest.mark.parametrize(
    "hash_cls",
    [
        RMSPropEmbeddingHashTable,
        RMSPropHalfEmbeddingHashTable,
    ],
    ids=["RMSProp", "RMSPropHalf"]
)
@pytest.mark.parametrize("dim", list(range(1, 34)) + [100])
def test_different_dimensions(hash_cls, dim):
    update_equations_impl(
        hash_cls=hash_cls,
        num_threads=3,
        max_size_per_update_job=200000,
        dim=dim
    )


@pytest.mark.parametrize(
    "hash_cls",
    [
        RMSPropEmbeddingHashTable,
    ],
    ids=["RMSProp"]
)
@pytest.mark.parametrize("l2", [0.1, 10, 100000])
def test_l2_regularization(hash_cls, l2):
    torch.manual_seed(12345)
    dim = 10
    items_count = 1000
    hash_table_noreg = HashEmbedding(hash_cls(dim))
    hash_table = HashEmbedding(hash_cls(dim))

    for i in range(items_count):
        # ensure same initialization
        item = hash_table.lookup_item(i)
        hash_table_noreg.insert_item(i, item)

    targets = torch.nn.init.normal_(torch.zeros(items_count))
    loss = torch.nn.MSELoss()
    # Hash embedding code

    hash_optimizer = EmbeddingRMSPropOptimizer([hash_table.parameter_with_hash_table], l2=l2)
    hash_optimizer_noreg = EmbeddingRMSPropOptimizer([hash_table_noreg.parameter_with_hash_table])
    data = torch.LongTensor(list(range(items_count)))
    data_len = torch.IntTensor([1] * items_count)

    # hash embedding step
    tensor = hash_table(data, data_len)
    loss(tensor.sum(dim=1), targets).backward()
    hash_optimizer.step()

    tensor = hash_table_noreg(data, data_len)
    loss(tensor.sum(dim=1), targets).backward()
    hash_optimizer_noreg.step()

    # Check that items are equal
    hash_table_tensor = hash_table(data, data_len).abs()
    hash_table_noreg_tensor = hash_table_noreg(data, data_len).abs()
    assert torch.all(torch.abs(hash_table_noreg_tensor - hash_table_tensor) > 0)
