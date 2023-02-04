import os

from ads_pytorch.hash_embedding.optim import create_optimizer
from ads_pytorch.hash_embedding.hash_embedding import (
    HashEmbedding,
    create_hash_table,
    create_item,
)

os.environ["OMP_NUM_THREADS"] = "1"
import torch
import torch.nn
import numpy as np
import pytest
import math
import libcpp_lib


def EmbeddingFTRLProximalLinearOptimizer(params, **kwargs):
    return create_optimizer(params, "ftrllinear", **kwargs)


def EmbeddingFTRLL1GroupedOptimizer(params, **kwargs):
    return create_optimizer(params, "ftrll1grouped", **kwargs)


def cmp2float(x: float, y: float, eps: float = 2e-3):
    return abs(x - y) <= eps


@pytest.mark.parametrize(
    "hash_type",
    [
        "ftrll1grouped",
    ],
)
def test_massive_expiration(hash_type):
    hash_table = HashEmbedding(create_hash_table(hash_type, 1))
    optim = EmbeddingFTRLL1GroupedOptimizer(
        hash_table.parameters(), lr=1, eps=1e-8, ttl=10
    )
    for u in optim.updaters:
        u.update_counter(1000)
        assert u.ttl_counter == 1000

    for i in range(100):
        item = create_item(hash_type, 1)
        item.ttl_counter = 0
        hash_table.insert_item(i, item)

    for i in range(100):
        item = create_item(hash_type, 1)
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
    "hash_type",
    [
        "ftrll1grouped",
    ],
)
def test_state_dict(hash_type):
    ttl = 2
    hash_tables = [
        HashEmbedding(create_hash_table(hash_type, i + 1)) for i in range(10)
    ]
    optim = EmbeddingFTRLL1GroupedOptimizer(
        [
            {"params": hash_table.parameters(), "lr": 1.0 + i * 10}
            for i, hash_table in enumerate(hash_tables)
        ],
        lr=1,
        eps=1e-8,
        ttl=ttl,
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
    optim2 = EmbeddingFTRLL1GroupedOptimizer(
        [
            {"params": hash_table.parameters(), "lr": 5}
            for i, hash_table in enumerate(hash_tables)
        ],
        lr=5,
        eps=1.0,
        ttl=100500,
    )
    for u in optim2.updaters:
        assert u.ttl_counter == 0
    optim2.load_state_dict(state)
    for i, group in enumerate(optim2.param_groups):
        assert group["lr"] == 1.0 + i * 10
        assert group["eps"] == 1e-8
        assert group["ttl"] == 2
        assert group["ttl_counter"] == 1

    for u in optim2.updaters:
        assert u.ttl_counter == 1


def update_equations_impl(hash_type, num_threads, max_size_per_update_job, dim):
    items_count = 1000
    epoch_count = 15

    mul_tensor = torch.rand(1000, dim)
    mul_tensor.requires_grad = False

    hash_table = HashEmbedding(create_hash_table(hash_type, dim))

    lr = 0.001
    eps = 1e-8

    optim = EmbeddingFTRLL1GroupedOptimizer(
        hash_table.parameters(), lr=lr, eps=eps, ttl=100
    )
    optim.num_threads = num_threads

    for i in range(items_count):
        item = hash_table.lookup_item(i)
        assert torch.allclose(item.w, torch.zeros(dim))

    targets = torch.nn.init.normal_(torch.zeros(items_count))
    loss = torch.nn.MSELoss(reduction="mean")

    # Usual pytorch code
    usual_tensor = torch.zeros(items_count, dim)
    with torch.no_grad():
        for i in range(items_count):
            usual_tensor[i, :] = hash_table.lookup_item(i).w
    usual_tensor.requires_grad = True
    param = torch.nn.Parameter(usual_tensor)
    optimizer = torch.optim.Adagrad([param], lr=lr, weight_decay=0.0)

    usual_optimize_path = []
    for _ in range(epoch_count):
        param.grad = None
        out = (param * mul_tensor).sum(dim=1)
        loss_val = loss(out, targets)
        usual_optimize_path.append(float(loss_val))
        loss_val.backward()
        optimizer.step()

    # Hash embedding code
    hash_optimizer = EmbeddingFTRLL1GroupedOptimizer(
        hash_table.parameters(), lr=lr, eps=eps, ttl=100
    )
    hash_optimizer.num_threads = num_threads
    data = torch.LongTensor(list(range(items_count)))
    data_len = torch.IntTensor([1] * items_count)

    # print("HashTable optimization")
    hash_optimize_path = []
    means = []
    for _ in range(epoch_count):
        # hash embedding step
        tensor = hash_table(data, data_len)
        means.append(tensor.mean())
        out = (tensor * mul_tensor).sum(dim=1)
        loss_val = loss(out, targets)
        hash_optimize_path.append(float(loss_val))
        loss_val.backward()
        hash_optimizer.step(max_size_per_update_job=max_size_per_update_job)

    for x, y in zip(usual_optimize_path, hash_optimize_path):
        assert cmp2float(x, y)


# need comprehensive test across all dims because of manual vectorization
@pytest.mark.parametrize("hash_type", ["ftrll1grouped"])
@pytest.mark.parametrize("dim", [1])
def test_different_dimensions(hash_type, dim):
    update_equations_impl(
        hash_type=hash_type, num_threads=3, max_size_per_update_job=200000, dim=dim
    )


def update_path_group_non_group(
    hash_type_group, hash_type_nongroup, num_threads, max_size_per_update_job, dim
):
    items_count = 1000
    epoch_count = 15

    mul_tensor = torch.rand(1000, dim)
    mul_tensor.requires_grad = False

    hash_table_group = HashEmbedding(create_hash_table(hash_type_group, dim))
    hash_table_non_group = HashEmbedding(create_hash_table(hash_type_nongroup, dim))

    lr = 0.001
    eps = 1e-8

    for i in range(items_count):
        item = hash_table_group.lookup_item(i)
        assert torch.allclose(item.w, torch.zeros(dim))

    for i in range(items_count):
        item = hash_table_non_group.lookup_item(i)
        assert torch.allclose(item.w, torch.zeros(dim))

    targets = torch.nn.init.normal_(torch.zeros(items_count))
    loss = torch.nn.MSELoss(reduction="mean")

    # Hash embedding code
    hash_optimizer_group = EmbeddingFTRLL1GroupedOptimizer(
        hash_table_group.parameters(), lr=lr, eps=eps, ttl=100
    )
    hash_optimizer_group.num_threads = num_threads

    hash_optimizer_non_group = EmbeddingFTRLProximalLinearOptimizer(
        hash_table_non_group.parameters(),
        lr=lr,
        L1Global=0.0,
        L1LinearIncremental=0.0,
        L1SqrtIncremental=0.0,
        L2Global=0.0,
        L2LinearIncremental=0.0,
        L2SqrtIncremental=0.0,
        eps=eps,
        ttl=100,
    )
    hash_optimizer_non_group.num_threads = num_threads

    data = torch.LongTensor(list(range(items_count)))
    data_len = torch.IntTensor([1] * items_count)

    hash_optimize_group_path = []
    means = []
    for _ in range(epoch_count):
        tensor = hash_table_group(data, data_len)
        means.append(tensor.mean())
        out = (tensor * mul_tensor).sum(dim=1)
        loss_val = loss(out, targets)
        hash_optimize_group_path.append(float(loss_val))
        loss_val.backward()
        hash_optimizer_group.step(max_size_per_update_job=max_size_per_update_job)

    hash_optimize_non_group_path = []
    means = []
    for _ in range(epoch_count):
        tensor = hash_table_non_group(data, data_len)
        means.append(tensor.mean())
        out = (tensor * mul_tensor).sum(dim=1)
        loss_val = loss(out, targets)
        hash_optimize_non_group_path.append(float(loss_val))
        loss_val.backward()
        hash_optimizer_non_group.step(max_size_per_update_job=max_size_per_update_job)

    for x, y in zip(hash_optimize_group_path, hash_optimize_non_group_path):
        assert cmp2float(x, y)


# need comprehensive test across all dims because of manual vectorization
@pytest.mark.parametrize("hash_type_group", ["ftrll1grouped"])
@pytest.mark.parametrize("hash_type_non_group", ["ftrllinear"])
@pytest.mark.parametrize("dim", [1])
def test_different_dimensions_group_non(hash_type_group, hash_type_non_group, dim):
    update_path_group_non_group(
        hash_type_group=hash_type_group,
        hash_type_nongroup=hash_type_non_group,
        num_threads=3,
        max_size_per_update_job=200000,
        dim=dim,
    )


def sparse_test(
    hash_type, num_threads, max_size_per_update_job, dim
):
    items_count = 2
    epoch_count = 15

    mul_tensor = torch.rand(items_count, dim)
    mul_tensor.requires_grad = False

    hash_table = HashEmbedding(create_hash_table(hash_type, dim))

    lr = 0.001
    eps = 1e-8

    for i in range(items_count):
        item = libcpp_lib.FTRLL1GroupedItem(3)
        item.w = torch.FloatTensor([i] * dim)
        item = hash_table.insert_item(i, item)

    targets = torch.FloatTensor([0, 2]) # input = target gives us zero grad
    loss = torch.nn.MSELoss(reduction="mean")

# Hash embedding code
    hash_optimizer = EmbeddingFTRLL1GroupedOptimizer(
        hash_table.parameters(), lr=lr, eps=eps, ttl=100
    )
    hash_optimizer.num_threads = num_threads

    data = torch.LongTensor(list(range(items_count)))
    data_len = torch.IntTensor([1] * items_count)

    tensor = hash_table(data, data_len)

    out = (tensor * mul_tensor).sum(dim=1)
    loss_val = loss(out, targets)
    loss_val.backward()

    hash_optimizer.step(max_size_per_update_job=max_size_per_update_job)
    assert hash_table.lookup_item(0).empty()
    assert not hash_table.lookup_item(1).empty()


@pytest.mark.parametrize("hash_type", ["ftrll1grouped"])
@pytest.mark.parametrize("dim", [100])
def test_sparsity(hash_type, dim):
    sparse_test(
        hash_type=hash_type,
        num_threads=3,
        max_size_per_update_job=200000,
        dim=dim,
    )
