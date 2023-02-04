import os

from ads_pytorch.hash_embedding.hash_embedding import (
    HashEmbedding,
    create_hash_table, create_item,
    HashEmbeddingGradientWrapper,
)

os.environ["OMP_NUM_THREADS"] = "1"
import torch
import numpy as np
import pytest
from copy import deepcopy


def _make_prediction_hash_table(dim):
    return create_hash_table("prediction", dim)


def _make_prediction_item(dim):
    return create_item("prediction", dim)


# Test only on prediction table - forward/backward pass does not depend
# on hash table type
def _parametrize_hash_table_cls(fn):
    return pytest.mark.parametrize(
        "hash_table_cls",
        [
            _make_prediction_hash_table,
        ],
        ids=[
            "Prediction",
        ]
    )(fn)


def _parametrize_hash_table_classes(fn):
    return pytest.mark.parametrize(
        'hash_table_classes',
        [
            (_make_prediction_hash_table, _make_prediction_item),
        ],
        ids=[
            "Prediction",
        ]
    )(fn)


_DIMS_TO_TEST = list(range(1, 203, 13))


############################################################
#                       FORWARD PASS                       #
############################################################


@_parametrize_hash_table_classes
@pytest.mark.parametrize('dim', _DIMS_TO_TEST)
@pytest.mark.parametrize('dtype', [torch.float32, torch.float16], ids=["Float", "Half"])
def test_forward_pass(dim, hash_table_classes, dtype):
    hash_table_cls, item_cls = hash_table_classes
    hash_table = HashEmbedding(hash_table_cls(dim))
    item = item_cls(dim)
    item.w = torch.FloatTensor([1] * dim)
    hash_table.insert_item(1, item)

    item = item_cls(dim)
    item.w = torch.FloatTensor([2] * dim)
    hash_table.insert_item(2, item)

    data = torch.LongTensor([1, 2, 3, 4, 1, 5, 2])
    data_len = torch.IntTensor([2, 3, 2])
    res = hash_table.forward(data, data_len, result_dtype=dtype)
    reference = torch.FloatTensor([[3] * dim, [1] * dim, [2] * dim])

    assert torch.allclose(res.float(), reference)


@_parametrize_hash_table_classes
@pytest.mark.parametrize('dim', _DIMS_TO_TEST)
@pytest.mark.parametrize('dtype', [torch.float32, torch.float16], ids=["Float", "Half"])
def test_forward_pass_sequence_mode(dim, hash_table_classes, dtype):
    hash_table_cls, item_cls = hash_table_classes
    hash_table = HashEmbedding(hash_table_cls(dim))
    for i in range(6):
        item = item_cls(dim)
        item.w = torch.FloatTensor([i] * dim)
        hash_table.insert_item(i, item)

    data = torch.LongTensor([1, 2, 1, 3, 4, 3, 4, 1, 5, 2])
    data_len = torch.IntTensor([[1, 1, 3], [1, 1, 1], [1, 1, 0]])
    res = hash_table.forward(data, data_len, compute_mode="sum", result_dtype=dtype)

    reference = torch.FloatTensor([
        [
            [1] * dim,
            [2] * dim,
            [8] * dim
        ],
        [
            [3] * dim,
            [4] * dim,
            [1] * dim
        ],
        [
            [5] * dim,
            [2] * dim,
            [0] * dim
        ]
    ])

    assert torch.allclose(res.float(), reference)


@_parametrize_hash_table_classes
@pytest.mark.parametrize('dim', _DIMS_TO_TEST)
@pytest.mark.parametrize('dtype', [torch.float32, torch.float16], ids=["Float", "Half"])
def test_forward_pass_mean_mode(dim, hash_table_classes, dtype):
    hash_table_cls, item_cls = hash_table_classes
    hash_table = HashEmbedding(hash_table_cls(dim))
    item = item_cls(dim)
    item.w = torch.FloatTensor([1] * dim)
    hash_table.insert_item(1, item)

    item = item_cls(dim)
    item.w = torch.FloatTensor([2] * dim)
    hash_table.insert_item(2, item)

    data = torch.LongTensor([1, 2, 3, 4, 1, 5, 2])
    data_len = torch.IntTensor([2, 3, 2])
    res = hash_table.forward(data, data_len, compute_mode="mean", result_dtype=dtype)
    reference = torch.FloatTensor([[3 / 2] * dim, [1 / 3] * dim, [2 / 2] * dim])

    atol = 1e-4 if dtype == torch.float16 else 1e-7
    assert torch.allclose(res.float(), reference, atol=atol)


@_parametrize_hash_table_classes
def test_hash_embedding_empty_forward(hash_table_classes):
    hash_cls, _ = hash_table_classes
    hash_table = HashEmbedding(hash_cls(1))
    res = hash_table(torch.from_numpy(np.arange(100, dtype=np.int64)), torch.from_numpy(np.ones(100, dtype=np.int32)))
    assert np.allclose(res.detach().numpy(), np.zeros(100))


@_parametrize_hash_table_classes
@pytest.mark.parametrize(
    ["data", "data_lens"],
    [
        (
            torch.randint(1000, size=(65, ), dtype=torch.int64),
            torch.ones(100, dtype=torch.int32),
        ),
        (
            torch.randint(1000, size=(103, ), dtype=torch.int64),
            torch.ones(100, dtype=torch.int32),
        ),
        (
            torch.randint(1000, size=(103, ), dtype=torch.int64),
            torch.ones(10, 10, dtype=torch.int32),
        ),
        (
            torch.randint(1000, size=(45, ), dtype=torch.int64),
            torch.ones(10, 10, dtype=torch.int32),
        )
    ],
    ids=["Lesser", "Greater", "MultidimLesser", "MultidimGreater"]
)
def test_malformed_inputs(hash_table_classes, data, data_lens):
    hash_cls, _ = hash_table_classes
    hash_table = HashEmbedding(hash_cls(1))

    with pytest.raises(RuntimeError):
        hash_table(data, data_lens)


############################################################
#                       FORWARD PASS WITH WEIGHTS          #
############################################################


@_parametrize_hash_table_classes
@pytest.mark.parametrize('dim', _DIMS_TO_TEST)
def test_weighted_forward_pass(dim, hash_table_classes):
    hash_table_cls, item_cls = hash_table_classes
    hash_table = HashEmbedding(hash_table_cls(dim))
    item = item_cls(dim)
    item.w = torch.FloatTensor([1] * dim)
    hash_table.insert_item(1, item)

    item = item_cls(dim)
    item.w = torch.FloatTensor([2] * dim)
    hash_table.insert_item(2, item)

    data = torch.LongTensor([1, 2, 3, 4, 1, 5, 2])
    data_len = torch.IntTensor([2, 3, 2])
    data_weight = torch.FloatTensor([0.1, 1, 10, 0.1, 1, 10, 100])
    res = hash_table.forward(data, data_len, data_weight)
    reference = torch.FloatTensor([[2.1] * dim, [1] * dim, [200] * dim])

    assert torch.allclose(res, reference, atol=1e-7)


@_parametrize_hash_table_classes
@pytest.mark.parametrize('dim', _DIMS_TO_TEST)
def test_weighted_forward_pass_sequence_mode(dim, hash_table_classes):
    hash_table_cls, item_cls = hash_table_classes
    hash_table = HashEmbedding(hash_table_cls(dim))
    for i in range(6):
        item = item_cls(dim)
        item.w = torch.FloatTensor([i] * dim)
        hash_table.insert_item(i, item)

    data = torch.LongTensor([1, 2, 1, 3, 4, 3, 4, 1, 5, 2])
    data_len = torch.IntTensor([[1, 1, 3], [1, 1, 1], [1, 1, 0]])
    data_weight = torch.FloatTensor([0.1, 1, 10, 100, 0.1, 1, 10, 100, 0.1, 1])
    res = hash_table.forward(data, data_len, data_weight, compute_mode="sum")

    reference = torch.FloatTensor([
        [
            [0.1] * dim,
            [2] * dim,
            [310.4] * dim
        ],
        [
            [3] * dim,
            [40] * dim,
            [100] * dim
        ],
        [
            [0.5] * dim,
            [2] * dim,
            [0] * dim
        ]
    ])

    assert torch.allclose(res, reference, atol=1e-7)


@_parametrize_hash_table_classes
@pytest.mark.parametrize('dim', _DIMS_TO_TEST)
def test_weighted_forward_pass_mean_mode(dim, hash_table_classes):
    hash_table_cls, item_cls = hash_table_classes
    hash_table = HashEmbedding(hash_table_cls(dim))
    item = item_cls(dim)
    item.w = torch.FloatTensor([1] * dim)
    hash_table.insert_item(1, item)

    item = item_cls(dim)
    item.w = torch.FloatTensor([2] * dim)
    hash_table.insert_item(2, item)

    data = torch.LongTensor([1, 2, 3, 4, 1, 5, 2])
    data_len = torch.IntTensor([2, 3, 2])
    data_weight = torch.FloatTensor([0.1, 1, 10, 0.1, 1, 10, 100])
    res = hash_table.forward(data, data_len, data_weight, compute_mode="mean")
    reference = torch.FloatTensor([[2.1 / 2] * dim, [1 / 3] * dim, [200 / 2] * dim])

    assert torch.allclose(res, reference, atol=1e-7)


@_parametrize_hash_table_classes
def test_weighted_hash_embedding_empty_forward(hash_table_classes):
    hash_cls, _ = hash_table_classes
    hash_table = HashEmbedding(hash_cls(1))
    res = hash_table(torch.from_numpy(np.arange(100, dtype=np.int64)), torch.from_numpy(np.ones(100, dtype=np.int32)),
                     torch.from_numpy(np.ones(100, dtype=np.float32)))
    assert np.allclose(res.detach().numpy(), np.zeros(100))


############################################################
#                      BACKWARD PASS                       #
############################################################


@_parametrize_hash_table_cls
@pytest.mark.parametrize('dim', [1, 3, 100])
def test_gradient_wrapper(hash_table_cls, dim):
    hash_table = HashEmbedding(hash_table_cls(dim))
    data = torch.LongTensor([1, 2, 3, 4])
    data_len = torch.IntTensor([2, 2])
    res = hash_table(data, data_len)
    res.sum().backward()

    wrap = HashEmbeddingGradientWrapper(dim, hash_table.parameter_with_hash_table.grad)
    for i in range(4):
        assert torch.all(torch.eq(wrap[data[i]], torch.FloatTensor([1] * dim)))

    idx, grad = wrap.to_tensors()
    assert torch.all(torch.eq(idx, data))
    assert torch.all(torch.eq(grad, torch.FloatTensor([[1] * dim] * 4)))

    assert str(wrap) == repr(wrap)


@_parametrize_hash_table_classes
@pytest.mark.parametrize('dim', _DIMS_TO_TEST)
@pytest.mark.parametrize("compute_mode", ["sum", "mean"])
def test_backward_pass(hash_table_classes, dim, compute_mode):
    torch.manual_seed(23899872)
    hash_table_cls, item_cls = hash_table_classes

    embedding_count = 5
    hash_table = HashEmbedding(hash_table_cls(dim))
    embedding = torch.nn.EmbeddingBag(
        num_embeddings=embedding_count,
        embedding_dim=dim,
        mode=compute_mode,
        sparse=False,
        scale_grad_by_freq=False
    )
    embed_table = torch.zeros(embedding_count, dim)
    for i in range(embedding_count):
        embed_table[i, :] = i + 1
        item = item_cls(dim)
        item.w = torch.full((dim, ), fill_value=float(i + 1))
        hash_table.insert_item(i, item)

    with torch.no_grad():
        embedding.weight.copy_(embed_table)

    data = torch.LongTensor([0, 2, 3, 4, 1, 2, 4, 3])
    data_len = torch.IntTensor([3, 3, 2])
    targets = torch.rand(3)

    # hash table forward
    hash_tensor = hash_table(data, data_len, compute_mode=compute_mode)
    torch.nn.functional.mse_loss(hash_tensor.sum(dim=1), targets).backward()
    wrap = HashEmbeddingGradientWrapper(dim, hash_table.parameter_with_hash_table.grad)
    idx, hash_grad = wrap.to_tensors()
    # reorder features in hash_grad to match sorted order of embedding.grad
    hash_grad_2 = torch.zeros_like(hash_grad)
    for i, index in enumerate(idx):
        hash_grad_2[index] = hash_grad[i]

    hash_grad = hash_grad_2

    # embedding forward
    tensor = embedding(data, torch.LongTensor([0, 3, 6]))
    torch.nn.functional.mse_loss(tensor.sum(dim=1), targets).backward()
    embed_grad = embedding.weight.grad

    assert torch.allclose(tensor, hash_tensor)
    assert torch.allclose(embed_grad, hash_grad)


@_parametrize_hash_table_classes
@pytest.mark.parametrize('dim', _DIMS_TO_TEST)
def test_backward_pass_sequence(hash_table_classes, dim):
    torch.manual_seed(23899872)
    hash_table_cls, item_cls = hash_table_classes

    embedding_count = 5
    hash_table = HashEmbedding(hash_table_cls(dim))
    embedding = torch.nn.Embedding(
        num_embeddings=embedding_count,
        embedding_dim=dim,
        sparse=False,
        scale_grad_by_freq=False
    )
    embed_table = torch.zeros(embedding_count, dim)
    for i in range(embedding_count):
        embed_table[i, :] = i + 1
        item = item_cls(dim)
        item.w = torch.full((dim, ), fill_value=float(i + 1))
        hash_table.insert_item(i, item)

    with torch.no_grad():
        embedding.weight.copy_(embed_table)

    data = torch.LongTensor([0, 2, 3, 4, 1, 2, 4, 3, 0])
    data_len = torch.IntTensor([
        [1, 1, 1],
        [1, 1, 1],
        [1, 1, 1]
    ])
    targets = torch.rand(3)

    # hash table forward
    hash_tensor = hash_table(data, data_len, compute_mode="sum")
    torch.nn.functional.mse_loss(hash_tensor.sum(dim=0).sum(dim=1), targets).backward()
    wrap = HashEmbeddingGradientWrapper(dim, hash_table.parameter_with_hash_table.grad)
    idx, hash_grad = wrap.to_tensors()
    # reorder features in hash_grad to match sorted order of embedding.grad
    hash_grad_2 = torch.zeros_like(hash_grad)
    for i, index in enumerate(idx):
        hash_grad_2[index] = hash_grad[i]

    hash_grad = hash_grad_2

    # embedding forward
    data = torch.LongTensor([[0, 2, 3], [4, 1, 2], [4, 3, 0]])
    tensor = embedding(data)
    torch.nn.functional.mse_loss(tensor.sum(dim=0).sum(dim=1), targets).backward()
    embed_grad = embedding.weight.grad

    assert torch.allclose(tensor, hash_tensor)
    assert torch.allclose(embed_grad, hash_grad)


@_parametrize_hash_table_cls
def test_default_parameter_server_mode(hash_table_cls):
    hash_table = HashEmbedding(hash_table_cls(1))
    assert not hash_table.parameter_server_mode


@_parametrize_hash_table_cls
@pytest.mark.parametrize('dim', [1, 3])
def test_train_mode(dim, hash_table_cls):
    hash_table = HashEmbedding(hash_table_cls(dim))
    assert not hash_table.parameter_server_mode  # just sanity check
    data = torch.LongTensor([1, 2, 3, 4])
    data_len = torch.IntTensor([2, 2])
    some_lin = torch.nn.Linear(dim, 1)
    loss = torch.nn.MSELoss()
    targets = torch.FloatTensor([1, 1])
    targets.requires_grad = False

    res = hash_table(data, data_len)
    pred = some_lin(res)

    assert len(res._backward_hooks)

    loss(pred, targets).backward()

    assert hash_table.parameter_with_hash_table.grad is not None  # we must call the hook

    # Then, let's check that after backward execution our gradient-calculating hook destroyed itself
    assert not len(res._backward_hooks)


@_parametrize_hash_table_cls
@pytest.mark.parametrize('dim', [1, 3])
def test_no_grad_mode(dim, hash_table_cls):
    hash_table = HashEmbedding(hash_table_cls(dim))
    assert not hash_table.parameter_server_mode  # just sanity check
    data = torch.LongTensor([1, 2, 3, 4])
    data_len = torch.IntTensor([2, 2])
    some_lin = torch.nn.Linear(dim, 1)
    loss = torch.nn.MSELoss()
    targets = torch.FloatTensor([1, 1])
    targets.requires_grad = False

    with torch.no_grad():
        res = hash_table(data, data_len)

    assert res._backward_hooks is None

    pred = some_lin(res)
    loss(pred, targets).backward()

    assert res._backward_hooks is None

    assert hash_table.parameter_with_hash_table.grad is None


############################################################
#                      BACKWARD PASS WITH WEIGHTS          #
############################################################


@_parametrize_hash_table_cls
@pytest.mark.parametrize('dim', [1, 3, 100])
def test_weighted_gradient_wrapper(hash_table_cls, dim):
    hash_table = HashEmbedding(hash_table_cls(dim))
    data = torch.LongTensor([1, 2, 3, 4])
    data_len = torch.IntTensor([2, 2])
    data_weight = torch.FloatTensor([0.1, 1, 10, 100])
    res = hash_table(data, data_len, data_weight)
    res.sum().backward()

    wrap = HashEmbeddingGradientWrapper(dim, hash_table.parameter_with_hash_table.grad)
    for i in range(4):
        assert torch.allclose(wrap[data[i]], torch.ones(dim, dtype=torch.float32) * data_weight[i])

    idx, grad = wrap.to_tensors()
    assert torch.all(torch.eq(idx, data))
    assert torch.allclose(grad, torch.ones(4, dim, dtype=torch.float32) * data_weight.unsqueeze(1))

    assert str(wrap) == repr(wrap)


@_parametrize_hash_table_classes
@pytest.mark.parametrize('dim', _DIMS_TO_TEST)
@pytest.mark.parametrize("compute_mode", ["sum", "mean"])
def test_weighted_backward_pass(hash_table_classes, dim, compute_mode):
    torch.manual_seed(23899872)
    hash_table_cls, item_cls = hash_table_classes

    embedding_count = 5
    hash_table = HashEmbedding(hash_table_cls(dim))
    embedding = torch.nn.EmbeddingBag(
        num_embeddings=embedding_count,
        embedding_dim=dim,
        mode="sum",
        sparse=False,
        scale_grad_by_freq=False
    )
    embed_table = torch.zeros(embedding_count, dim)
    for i in range(embedding_count):
        embed_table[i, :] = i + 1
        item = item_cls(dim)
        item.w = torch.full((dim, ), fill_value=float(i + 1))
        hash_table.insert_item(i, item)

    with torch.no_grad():
        embedding.weight.copy_(embed_table)

    data = torch.LongTensor([0, 2, 3, 4, 1, 2, 4, 3])
    data_len = torch.IntTensor([3, 3, 2])
    data_weight = torch.FloatTensor([0.1, 1, 10, 0.1, 1, 10, 0.1, 1])
    data_weight_mean = torch.FloatTensor([0.1 / 3, 1 / 3, 10 / 3, 0.1 / 3, 1 / 3, 10 / 3, 0.1 / 2, 1 / 2])
    targets = torch.rand(3)

    # hash table forward
    hash_tensor = hash_table(data, data_len, data_weight, compute_mode=compute_mode)
    torch.nn.functional.mse_loss(hash_tensor.sum(dim=1), targets).backward()
    wrap = HashEmbeddingGradientWrapper(dim, hash_table.parameter_with_hash_table.grad)
    idx, hash_grad = wrap.to_tensors()
    # reorder features in hash_grad to match sorted order of embedding.grad
    hash_grad_2 = torch.zeros_like(hash_grad)
    for i, index in enumerate(idx):
        hash_grad_2[index] = hash_grad[i]

    hash_grad = hash_grad_2

    # embedding forward
    tensor = embedding(data, torch.LongTensor([0, 3, 6]), per_sample_weights=(data_weight_mean if compute_mode == "mean" else data_weight))
    torch.nn.functional.mse_loss(tensor.sum(dim=1), targets).backward()
    embed_grad = embedding.weight.grad

    assert torch.allclose(tensor, hash_tensor)
    assert torch.allclose(embed_grad, hash_grad)


@_parametrize_hash_table_classes
@pytest.mark.parametrize('dim', _DIMS_TO_TEST)
def test_weighted_backward_pass_sequence(hash_table_classes, dim):
    torch.manual_seed(23899872)
    hash_table_cls, item_cls = hash_table_classes

    embedding_count = 5
    hash_table = HashEmbedding(hash_table_cls(dim))
    embedding = torch.nn.Embedding(
        num_embeddings=embedding_count,
        embedding_dim=dim,
        sparse=False,
        scale_grad_by_freq=False
    )
    embed_table = torch.zeros(embedding_count, dim)
    for i in range(embedding_count):
        embed_table[i, :] = i + 1
        item = item_cls(dim)
        item.w = torch.full((dim, ), fill_value=float(i + 1))
        hash_table.insert_item(i, item)

    with torch.no_grad():
        embedding.weight.copy_(embed_table)

    data = torch.LongTensor([0, 2, 3, 4, 1, 2, 4, 3, 0])
    data_len = torch.IntTensor([
        [1, 1, 1],
        [1, 1, 1],
        [1, 1, 1]
    ])
    data_weight = torch.FloatTensor([0.1, 1, 10, 100, 0.1, 1, 10, 100, 0.1])
    targets = torch.rand(3)

    # hash table forward
    hash_tensor = hash_table(data, data_len, data_weight, compute_mode="sum")
    torch.nn.functional.mse_loss(hash_tensor.sum(dim=0).sum(dim=1), targets).backward()
    wrap = HashEmbeddingGradientWrapper(dim, hash_table.parameter_with_hash_table.grad)
    idx, hash_grad = wrap.to_tensors()
    # reorder features in hash_grad to match sorted order of embedding.grad
    hash_grad_2 = torch.zeros_like(hash_grad)
    for i, index in enumerate(idx):
        hash_grad_2[index] = hash_grad[i]

    hash_grad = hash_grad_2

    # embedding forward
    data = torch.LongTensor([[0, 2, 3], [4, 1, 2], [4, 3, 0]])
    data_weight = torch.FloatTensor([[0.1, 1, 10], [100, 0.1, 1], [10, 100, 0.1]])
    tensor = embedding(data) * data_weight.unsqueeze(2)
    torch.nn.functional.mse_loss(tensor.sum(dim=0).sum(dim=1), targets).backward()
    embed_grad = embedding.weight.grad

    assert torch.allclose(tensor, hash_tensor)
    assert torch.allclose(embed_grad, hash_grad)


@_parametrize_hash_table_cls
@pytest.mark.parametrize('dim', [1, 3])
def test_weighted_train_mode(dim, hash_table_cls):
    hash_table = HashEmbedding(hash_table_cls(dim))
    assert not hash_table.parameter_server_mode  # just sanity check
    data = torch.LongTensor([1, 2, 3, 4])
    data_len = torch.IntTensor([2, 2])
    data_weight = torch.FloatTensor([0.1, 1, 10, 100])
    some_lin = torch.nn.Linear(dim, 1)
    loss = torch.nn.MSELoss()
    targets = torch.FloatTensor([1, 1])
    targets.requires_grad = False

    res = hash_table(data, data_len, data_weight)
    pred = some_lin(res)

    assert len(res._backward_hooks)

    loss(pred, targets).backward()

    assert hash_table.parameter_with_hash_table.grad is not None  # we must call the hook

    # Then, let's check that after backward execution our gradient-calculating hook destroyed itself
    assert not len(res._backward_hooks)


@_parametrize_hash_table_cls
@pytest.mark.parametrize('dim', [1, 3])
def test_weighted_no_grad_mode(dim, hash_table_cls):
    hash_table = HashEmbedding(hash_table_cls(dim))
    assert not hash_table.parameter_server_mode  # just sanity check
    data = torch.LongTensor([1, 2, 3, 4])
    data_len = torch.IntTensor([2, 2])
    data_weight = torch.FloatTensor([0.1, 1, 10, 100])
    some_lin = torch.nn.Linear(dim, 1)
    loss = torch.nn.MSELoss()
    targets = torch.FloatTensor([1, 1])
    targets.requires_grad = False

    with torch.no_grad():
        res = hash_table(data, data_len, data_weight)

    assert res._backward_hooks is None

    pred = some_lin(res)
    loss(pred, targets).backward()

    assert res._backward_hooks is None

    assert hash_table.parameter_with_hash_table.grad is None


##################################################################
#               TEST HASH EMBEDDING SERIALIZATION                #
##################################################################


@_parametrize_hash_table_cls
@pytest.mark.parametrize('dim', [1, 3, 100])
def test_save_empty_model(dim, hash_table_cls):
    hash_table = HashEmbedding(hash_table_cls(dim))
    stream = hash_table.get_serialization_stream()
    string = b''.join(list(stream))
    assert string == b''


@_parametrize_hash_table_cls
@pytest.mark.parametrize('dim', [1, 3, 100])
def test_load_empty_model(dim, hash_table_cls):
    hash_table = HashEmbedding(hash_table_cls(dim))
    stream = hash_table.get_deserialization_stream()
    stream.feed_data(b'')
    assert hash_table.size() == 0


@_parametrize_hash_table_classes
@pytest.mark.parametrize('dim', [1, 100])
def test_partial_serialization(hash_table_classes, dim):
    buffer_size = 1 << 10
    hash_table_cls, item_cls = hash_table_classes
    hash_table = HashEmbedding(hash_table_cls(dim))

    for i in range(1000):
        item = item_cls(dim)
        item.w = torch.from_numpy(np.random.rand(dim).astype(np.float32))
        hash_table.insert_item(i, item)

    # Get the real chunk count
    stream = hash_table.get_serialization_stream(buffer_size=buffer_size)
    chunk_count = len(list(stream))
    assert chunk_count > 10, "Sanity check for test validity"
    middle = int(chunk_count / 2)

    # Start serialization. Stop in the middle, deepcopy streams, and yield from both. Then check that
    # resulting serialized strings are equal
    stream = hash_table.get_serialization_stream(buffer_size=buffer_size)
    # Here we test that iter returns same object. model_serializer rely on this behavior
    assert stream is iter(stream)
    deepcopy_stream1 = deepcopy(stream)

    first_part = []
    for i in range(middle):
        first_part.append(next(stream))

    deepcopy_stream2 = deepcopy(stream)
    second_part1 = list(stream)
    second_part2 = list(deepcopy_stream2)

    full_from_first_deepcopy = list(deepcopy_stream1)

    full1 = b''.join(first_part + second_part1)
    full2 = b''.join(first_part + second_part2)
    full3 = b''.join(full_from_first_deepcopy)

    assert full1 == full2 == full3

    # tests on get_saved_count method
    assert stream.get_saved_count() == deepcopy_stream1.get_saved_count() == deepcopy_stream2.get_saved_count() == hash_table.size()


@_parametrize_hash_table_classes
@pytest.mark.parametrize('dim', [1, 100])
def test_partial_deserialization(hash_table_classes, dim):
    # Here, we test that we can re-create deserializer at any point if it's empty (i.e. no partially-fed data)
    # parameter_server.model_serializer.py expects this behavior
    hash_table_cls, item_cls = hash_table_classes
    hash_table = HashEmbedding(hash_table_cls(dim))

    for i in range(1000):
        item = item_cls(dim)
        item.w = torch.from_numpy(np.random.rand(dim).astype(np.float32))
        hash_table.insert_item(i, item)

    saved_chunks = list(hash_table.get_serialization_stream())
    hash_table2 = HashEmbedding(hash_table_cls(dim))
    for chunk in saved_chunks:
        stream = hash_table2.get_deserialization_stream()
        for symbol in chunk:
            stream.feed_data(bytes(bytearray([symbol])))

    # As always, we feed symbols one-by-one
    assert hash_table2.size() == hash_table.size()
    for i in range(1000):
        hash_table2.lookup_item(i)
