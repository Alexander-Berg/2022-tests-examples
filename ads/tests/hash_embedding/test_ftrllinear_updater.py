import os

from ads_pytorch.hash_embedding.optim import create_optimizer
from ads_pytorch.hash_embedding.hash_embedding import (
    HashEmbedding,
    create_hash_table,
    create_item
)

os.environ["OMP_NUM_THREADS"] = "1"
import torch
import torch.nn
import numpy as np
import pytest
import math


def EmbeddingFTRLProximalLinearOptimizer(params, **kwargs):
    return create_optimizer(params, "ftrllinear", **kwargs)


def cmp2float(x: float, y: float, eps: float = 2e-3):
    return abs(x - y) <= eps


@pytest.mark.parametrize(
    "hash_type",
    [
        "ftrllinear"
    ]
)
def test_massive_expiration(hash_type):
    hash_table = HashEmbedding(create_hash_table(hash_type, 1))
    optim = EmbeddingFTRLProximalLinearOptimizer(hash_table.parameters(), lr=1, L1Global=0.0, L1LinearIncremental=1.0, L1SqrtIncremental=1.0, 
                L2Global=0.0, L2LinearIncremental=1.0, L2SqrtIncremental=1.0, eps=1e-8, ttl=10)
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


########################################################
#                  EQUATIONS TESTS                     #
########################################################

# Here we test two use-cases:
# 1. split jobs between threads
# 2. testing proper vectorization code


def update_equations_impl(hash_type, num_threads, max_size_per_update_job, dim, weight_decay):
    items_count = 1000
    epoch_count = 15

    mul_tensor = torch.rand(1000, dim)
    mul_tensor.requires_grad = False

    hash_table = HashEmbedding(create_hash_table(hash_type, dim))

    lr = 0.001
    eps = 1e-8

    optim = EmbeddingFTRLProximalLinearOptimizer(hash_table.parameters(), lr=lr, L1Global=0.0, L1LinearIncremental=0.0, L1SqrtIncremental=0.0, 
                L2Global=weight_decay, L2LinearIncremental=0.0, L2SqrtIncremental=0.0, eps=eps, ttl=100)
    optim.num_threads = num_threads

    for i in range(items_count):
        # This will generate some items
        item = hash_table.lookup_item(i)
        # check that ftrllinear items have trivial initialization
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
    optimizer = torch.optim.Adagrad(
        [param],
        lr=lr,
        weight_decay=weight_decay
    )

    usual_optimize_path = []
    for _ in range(epoch_count):
        param.grad = None
        out = (param * mul_tensor).sum(dim=1)
        loss_val = loss(out, targets)
        usual_optimize_path.append(float(loss_val))
        loss_val.backward()
        optimizer.step()

    # Hash embedding code
    hash_optimizer = EmbeddingFTRLProximalLinearOptimizer(hash_table.parameters(), lr=lr, L1Global=0.0, L1LinearIncremental=0.0, L1SqrtIncremental=0.0, 
                L2Global=weight_decay, L2LinearIncremental=0.0, L2SqrtIncremental=0.0, eps=eps, ttl=100)
    hash_optimizer.num_threads = num_threads
    data = torch.LongTensor(list(range(items_count)))
    data_len = torch.IntTensor([1] * items_count)

    # print("HashTable optimization")
    hash_optimize_path = []
    for _ in range(epoch_count):
        # hash embedding step
        tensor = hash_table(data, data_len)
        out = (tensor * mul_tensor).sum(dim=1)
        loss_val = loss(out, targets)
        hash_optimize_path.append(float(loss_val))
        loss_val.backward()
        hash_optimizer.step(max_size_per_update_job=max_size_per_update_job)

    for x, y in zip(usual_optimize_path, hash_optimize_path):
        assert cmp2float(x, y)


# need comprehensive test across all dims because of manual vectorization
@pytest.mark.parametrize(
    "hash_type",
    [
        "ftrllinear"
    ]
)
@pytest.mark.parametrize("dim", [1])
@pytest.mark.parametrize("l2", [0, 0.001])
def test_different_dimensions(hash_type, dim, l2):
    update_equations_impl(
        hash_type=hash_type,
        num_threads=3,
        max_size_per_update_job=200000,
        dim=dim,
        weight_decay=l2
    )

def FTRLProximalLinearEmbeddingHashTable(dim):
    return create_hash_table("ftrllinear", dim)


# нужно ли делать такой тест, если размерность только 1 может быть?
@pytest.mark.parametrize(
    "hash_cls",
    [
        FTRLProximalLinearEmbeddingHashTable
    ],
    ids=["RMSProp"]
)
def test_state_dict(hash_cls):
    ttl = 2
    hash_tables = [HashEmbedding(hash_cls(1))]
    optim = EmbeddingFTRLProximalLinearOptimizer(
        [
            {"params": hash_table.parameters(), "lr": 1.0 + i * 10, "L2Global": 0.4 + i * 0.05}
            for i, hash_table in enumerate(hash_tables)
        ],
        lr=1, eps=1e-8, ttl=ttl
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
    optim2 = EmbeddingFTRLProximalLinearOptimizer(
        [
            {"params": hash_table.parameters(), "lr": 5, "L2Global": 0.3}
            for i, hash_table in enumerate(hash_tables)
        ],
        lr=5, eps=1.0, ttl=100500
    )
    for u in optim2.updaters:
        assert u.ttl_counter == 0
    optim2.load_state_dict(state)
    for i, group in enumerate(optim2.param_groups):
        assert group["lr"] == 1.0 + i * 10
        assert group["L2Global"] == 0.4 + i * 0.05
        assert group["eps"] == 1e-8
        assert group["ttl"] == 2
        assert group["ttl_counter"] == 1

    for u in optim2.updaters:
        assert u.ttl_counter == 1

@pytest.mark.parametrize('max_size_per_update_job', [-1, 200, 1000000000])
@pytest.mark.parametrize('num_threads', [1, 3, 5, 7])
def test_update_equations_with_different_item_occurencies(num_threads, max_size_per_update_job):
    torch.manual_seed(12345)
    dim = 1
    items_count = 1000
    first_part_epoch_count = 10
    second_part_epoch_count = 15
    hash_table = HashEmbedding(create_hash_table("ftrllinear", dim))
    optim = EmbeddingFTRLProximalLinearOptimizer(hash_table.parameters(), lr=1, L1Global=0.0, L1LinearIncremental=0.0, L1SqrtIncremental=0.0, 
                L2Global=0.0, L2LinearIncremental=0.0, L2SqrtIncremental=0.0, eps=1e-8, ttl=100)
    optim.num_threads = num_threads

    for i in range(items_count):
        # This will generate some items
        item = hash_table.lookup_item(i)
        # check that ftrllinear items have trivial initialization
        assert torch.allclose(item.w, torch.zeros(dim))

    targets = torch.nn.init.normal_(torch.zeros(items_count // 2))
    loss = torch.nn.MSELoss()

    # Usual pytorch code
    usual_params = []
    for chunk_id, epoch_count in enumerate([first_part_epoch_count, second_part_epoch_count]):
        usual_tensor = torch.zeros(items_count // 2, dim)
        with torch.no_grad():
            for i in range(items_count // 2):
                usual_tensor[i, :] = hash_table.lookup_item(i + chunk_id * (items_count // 2)).w
        usual_tensor.requires_grad = True
        param = torch.nn.Parameter(usual_tensor)
        optimizer = optimizer = torch.optim.Adagrad(
            [param],
            lr=1,
            weight_decay=0.0
        )

        for _ in range(epoch_count):
            param.grad = None
            loss(param.sum(dim=1), targets).backward()
            optimizer.step()

        usual_params.append((param, optimizer))

    usual_tensor = torch.cat([x[0].clone() for x in usual_params], dim=0)

    # Hash embedding code

    hash_optimizer = EmbeddingFTRLProximalLinearOptimizer([hash_table.parameter_with_hash_table], lr=1, L1Global=0.0, L1LinearIncremental=0.0, L1SqrtIncremental=0.0, 
                L2Global=0.0, L2LinearIncremental=0.0, L2SqrtIncremental=0.0, eps=1e-8, ttl=100)

    data = torch.LongTensor(list(range(items_count // 2)))
    data_len = torch.IntTensor([1] * (items_count // 2))

    for _ in range(first_part_epoch_count):
        # hash embedding step
        tensor = hash_table(data, data_len)
        loss(tensor.sum(dim=1), targets).backward()
        hash_optimizer.step()

    data = torch.LongTensor(list(range(items_count // 2, items_count)))
    data_len = torch.IntTensor([1] * (items_count // 2))

    for _ in range(second_part_epoch_count):
        # hash embedding step
        tensor = hash_table(data, data_len)
        loss(tensor.sum(dim=1), targets).backward()
        hash_optimizer.step(max_size_per_update_job=max_size_per_update_job)

    # Check that items are equal
    data = torch.LongTensor(list(range(items_count)))
    data_len = torch.IntTensor([1] * (items_count))
    hash_table_tensor = hash_table(data, data_len)

    assert torch.allclose(hash_table_tensor, usual_tensor, rtol=1e-3, atol=1e-3)

    # NOW, when we have optimized several parameters, we start to optimize them jointly and
    # see whether pytorch correctly incorporate different step size counts for parameters

    epoch_count = 5

    data = torch.LongTensor(sum([[i, i + 500] for i in range(items_count // 2)], []))
    data_len = torch.IntTensor([2] * (items_count // 2))

    for _ in range(epoch_count):
        opt1, opt2 = [x[1] for x in usual_params]
        param1, param2 = [x[0] for x in usual_params]
        param1.grad = None
        param2.grad = None

        loss(param1.sum(dim=1) + param2.sum(dim=1), targets).backward()
        opt1.step()
        opt2.step()

        tensor = hash_table(data, data_len)
        loss(tensor.sum(dim=1), targets).backward()
        hash_optimizer.step()

    usual_tensor = torch.cat([x[0].clone() for x in usual_params], dim=0)
    hash_table_tensor = hash_table(
        torch.LongTensor(list(range(items_count))),
        torch.IntTensor([1] * (items_count))
    )

    assert torch.allclose(hash_table_tensor, usual_tensor, rtol=1e-3, atol=1e-3)


@pytest.mark.parametrize(
    "hash_type",
    ["ftrllinear"]
)
@pytest.mark.parametrize("l2", [0.1, 10, 100000])
def test_l2_regularization(hash_type, l2):
    torch.manual_seed(12345)
    dim = 1
    items_count = 1000
    hash_table_noreg = HashEmbedding(create_hash_table(hash_type, dim))
    hash_table = HashEmbedding(create_hash_table(hash_type, dim))

    for i in range(items_count):
        # ensure same initialization
        item = hash_table.lookup_item(i)
        hash_table_noreg.insert_item(i, item)

    targets = torch.nn.init.normal_(torch.zeros(items_count))
    loss = torch.nn.MSELoss()
    # Hash embedding code

    hash_optimizer = EmbeddingFTRLProximalLinearOptimizer([hash_table.parameter_with_hash_table], L2Global=l2)
    hash_optimizer_noreg = EmbeddingFTRLProximalLinearOptimizer([hash_table_noreg.parameter_with_hash_table])
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