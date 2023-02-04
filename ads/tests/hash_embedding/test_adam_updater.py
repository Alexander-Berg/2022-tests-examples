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


def EmbeddingAdamOptimizer(params, **kwargs):
    return create_optimizer(params, "adam", **kwargs)


def cmp2float(x: float, y: float, eps: float = 2e-3):
    return abs(x - y) <= eps


@pytest.mark.parametrize(
    "hash_type",
    [
        "adam",
        "adam_half",
    ]
)
def test_expiration(hash_type):
    ttl = 2
    hash_table = HashEmbedding(create_hash_table(hash_type, 1))
    optim = EmbeddingAdamOptimizer(hash_table.parameters(), lr=1, beta1=0.9, beta2=0.999, eps=1e-8, ttl=ttl)
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
    "hash_type",
    [
        "adam",
        "adam_half",
    ]
)
def test_massive_expiration(hash_type):
    hash_table = HashEmbedding(create_hash_table(hash_type, 1))
    optim = EmbeddingAdamOptimizer(hash_table.parameters(), lr=1, beta1=0.9, beta2=0.999, eps=1e-8, ttl=10)
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
        "adam",
        "adam_half",
    ]
)
@pytest.mark.parametrize('size', [1, 4, 1000])
@pytest.mark.parametrize('num_threads', [1, 3, 5, 7])
def test_parallel_update(hash_type, num_threads, size):
    is_half: bool = hash_type == "adam_half"
    atol = 1e-8 if not is_half else 1e-4
    beta1 = 0.9
    beta2 = 0.999

    hash_table = HashEmbedding(create_hash_table(hash_type, 1))
    optim = EmbeddingAdamOptimizer(hash_table.parameters(), lr=1, beta1=beta1, beta2=beta2, eps=1e-8, ttl=100)
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
        assert torch.allclose(elem.first_moment * (1 - beta1), torch.FloatTensor([0.1]), atol=atol)
        assert torch.allclose(elem.second_moment * (1 - beta2), torch.FloatTensor([0.001]), atol=atol)

    res = hash_table(data, data_len)
    res.sum().backward()
    optim.step()

    assert hash_table.size() == size

    for i in range(size):
        elem = hash_table.lookup_item(i)
        assert math.fabs(elem.w + 2) <= 0.02
        # !!! We have slightly optimized equations for adam
        assert torch.allclose(elem.first_moment * (1 - beta1), torch.FloatTensor([0.19]), atol=atol)
        assert torch.allclose(elem.second_moment * (1 - beta2), torch.FloatTensor([0.001999]), atol=atol)


@pytest.mark.parametrize(
    "hash_type",
    [
        "adam",
        "adam_half",
    ]
)
def test_state_dict(hash_type):
    ttl = 2
    hash_tables = [HashEmbedding(create_hash_table(hash_type, i + 1)) for i in range(10)]
    optim = EmbeddingAdamOptimizer(
        [
            {"params": hash_table.parameters(), "lr": 1.0 + i * 10, "beta1": 0.4 + i * 0.05}
            for i, hash_table in enumerate(hash_tables)
        ],
        lr=1, beta1=0.9, beta2=0.999, eps=1e-8, ttl=ttl
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
    optim2 = EmbeddingAdamOptimizer(
        [
            {"params": hash_table.parameters(), "lr": 5, "beta1": 0.3}
            for i, hash_table in enumerate(hash_tables)
        ],
        lr=5, beta1=0.2, beta2=0.8, eps=1.0, ttl=100500
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


def update_equations_impl(hash_type, num_threads, max_size_per_update_job, dim):
    items_count = 1000
    epoch_count = 15

    mul_tensor = torch.rand(1000, dim)
    mul_tensor.requires_grad = False

    hash_table = HashEmbedding(create_hash_table(hash_type, dim))
    is_half = hash_table.parameter_with_hash_table.hash_table.is_half()

    lr = 0.001
    if is_half:
        eps = 0
        beta1 = 0.9
        beta2 = 0.999
    else:
        eps = 0
        beta1 = 0.9
        beta2 = 0.999

    optim = EmbeddingAdamOptimizer(hash_table.parameters(), lr=lr, beta1=beta1, beta2=beta2, eps=eps, ttl=100)
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
    optimizer = torch.optim.Adam(
        [param],
        eps=eps,
        betas=(beta1, beta2),
        lr=lr,
        weight_decay=.0,
        amsgrad=False
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

    hash_optimizer = EmbeddingAdamOptimizer(hash_table.parameters(), lr=lr, beta1=beta1, beta2=beta2, eps=eps, ttl=100)
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
    "hash_type",
    [
        "adam", "adam_half"
    ]
)
@pytest.mark.parametrize("dim", list(range(1, 34)) + [100])
def test_different_dimensions(hash_type, dim):
    update_equations_impl(
        hash_type=hash_type,
        num_threads=3,
        max_size_per_update_job=200000,
        dim=dim
    )


@pytest.mark.parametrize('max_size_per_update_job', [-1, 200, 1000000000])
@pytest.mark.parametrize('num_threads', [1, 3, 5, 7])
def test_update_equations_with_different_item_occurencies(num_threads, max_size_per_update_job):
    torch.manual_seed(12345)
    dim = 10
    items_count = 1000
    first_part_epoch_count = 10
    second_part_epoch_count = 15
    hash_table = HashEmbedding(create_hash_table("adam", dim))
    optim = EmbeddingAdamOptimizer(hash_table.parameters(), lr=1, beta1=0.9, beta2=0.999, eps=1e-8, ttl=100)
    optim.num_threads = num_threads

    for i in range(items_count):
        # This will generate some items
        item = hash_table.lookup_item(i)
        # sanity check that adam items have non-trivial initialization (rest of test code relies on it)
        assert not torch.allclose(item.w, torch.zeros(dim))

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
        optimizer = torch.optim.Adam([param])

        for epoch in range(epoch_count):
            param.grad = None
            loss(param.sum(dim=1), targets).backward()
            optimizer.step()

        usual_params.append((param, optimizer))

    usual_tensor = torch.cat([x[0].clone() for x in usual_params], dim=0)

    # Hash embedding code

    hash_optimizer = EmbeddingAdamOptimizer([hash_table.parameter_with_hash_table])

    data = torch.LongTensor(list(range(items_count // 2)))
    data_len = torch.IntTensor([1] * (items_count // 2))

    for epoch in range(first_part_epoch_count):
        # hash embedding step
        tensor = hash_table(data, data_len)
        loss(tensor.sum(dim=1), targets).backward()
        hash_optimizer.step()

    data = torch.LongTensor(list(range(items_count // 2, items_count)))
    data_len = torch.IntTensor([1] * (items_count // 2))

    for epoch in range(second_part_epoch_count):
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

    for epoch in range(epoch_count):
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
    ["adam"]
)
@pytest.mark.parametrize("l2", [0.1, 10, 100000])
def test_l2_regularization(hash_type, l2):
    torch.manual_seed(12345)
    dim = 10
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

    hash_optimizer = EmbeddingAdamOptimizer([hash_table.parameter_with_hash_table], l2=l2)
    hash_optimizer_noreg = EmbeddingAdamOptimizer([hash_table_noreg.parameter_with_hash_table])
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
