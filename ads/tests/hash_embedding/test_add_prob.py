import os

from ads_pytorch.hash_embedding.optim import create_optimizer
from ads_pytorch.hash_embedding.hash_embedding import (
    HashEmbedding,
    create_hash_table
)

os.environ["OMP_NUM_THREADS"] = "1"
import torch
import torch.nn


def EmbeddingAdamOptimizer(params, **kwargs):
    return create_optimizer(params, "adam", **kwargs)


def test_add_prob():
    torch.manual_seed(12345)
    dim = 10
    items_count = 1000
    add_prob = 0.5
    hash_table = HashEmbedding(create_hash_table("adam", dim))

    targets = torch.nn.init.normal_(torch.zeros(items_count))
    loss = torch.nn.MSELoss()

    hash_optimizer = EmbeddingAdamOptimizer([hash_table.parameter_with_hash_table], lr=100)

    data = torch.LongTensor(list(range(items_count)))
    data_len = torch.IntTensor([1] * items_count)

    tensor = hash_table(data, data_len, add_prob=add_prob)
    loss(tensor.sum(dim=1), targets).backward()
    hash_optimizer.step()

    tensor = hash_table(data, data_len, add_prob=add_prob)

    count_add = (tensor.abs().sum(dim=1) > 0.1).sum().item()

    assert add_prob - 0.1 < count_add / tensor.shape[0] < add_prob + 0.1
