import os

from ads_pytorch.hash_embedding.hash_embedding import (
    HashEmbedding,
    create_item,
    create_hash_table
)

os.environ["OMP_NUM_THREADS"] = "1"
import torch
import asyncio
import pytest


def PredictionEmbeddingHashTable(dim):
    return create_hash_table("prediction", dim)


def PredictionItem(dim):
    return create_item("prediction", dim)


def PredictionHalfEmbeddingHashTable(dim):
    return create_hash_table("prediction_half", dim)


def PredictionHalfItem(dim):
    return create_item("prediction_half", dim)



@pytest.fixture(
    params=[
        (PredictionEmbeddingHashTable, PredictionItem),
        (PredictionHalfEmbeddingHashTable, PredictionHalfItem),
    ],
    ids=["Prediction", "PredictionHalf"]
)
def hash_cls_and_item(request):
    return request.param


@pytest.mark.parametrize('dim', [1, 3, 100])
def test_empty_hash_lookup_prediction(dim, hash_cls_and_item):
    hash_cls, _ = hash_cls_and_item
    hash_table = HashEmbedding(hash_cls(dim))
    item = hash_table.lookup_item(0)
    assert torch.all(torch.eq(item.w, torch.FloatTensor([0] * dim)))


@pytest.mark.parametrize('dim', [1, 3, 100])
def test_hash_insert_lookup_prediction(dim, hash_cls_and_item):
    hash_cls, hash_item_cls = hash_cls_and_item
    hash_table = HashEmbedding(hash_cls(dim))
    item = hash_table.lookup_item(0)
    item.w = torch.FloatTensor([1] * dim)

    # Check for no side-effects
    item2 = hash_table.lookup_item(0)
    assert torch.all(torch.eq(item2.w, torch.FloatTensor([0] * dim)))

    hash_table.insert_item(0, item)
    item2 = hash_table.lookup_item(0)
    assert torch.all(torch.eq(item2.w, torch.FloatTensor([1] * dim)))

    hash_table.insert_item(1, item)
    item2 = hash_table.lookup_item(1)
    assert torch.all(torch.eq(item2.w, torch.FloatTensor([1] * dim)))


##################################################################
#               TEST HASH EMBEDDING SERIALIZATION                #
##################################################################


@pytest.mark.parametrize('dim', [1, 3, 100])
@pytest.mark.parametrize('buffer_size', [1, 1000000])
def test_serialize_model(dim, buffer_size, hash_cls_and_item):
    hash_cls, hash_item_cls = hash_cls_and_item
    hash_table = HashEmbedding(hash_cls(dim))
    item = hash_item_cls(dim)
    item.w = torch.FloatTensor([1.0] * dim)
    hash_table.insert_item(123, item)
    item = hash_item_cls(dim)
    item.w = torch.FloatTensor([4.0] * dim)
    hash_table.insert_item(321, item)

    assert hash_table.size() == 2
    save_stream = hash_table.get_serialization_stream(buffer_size=buffer_size)
    string = b''.join(list(save_stream))
    assert string

    save_stream = hash_table.get_serialization_stream(buffer_size=buffer_size)

    hash_table2 = HashEmbedding(hash_cls(dim))
    load_stream = hash_table2.get_deserialization_stream()
    for s in save_stream:
        load_stream.feed_data(s)
    assert hash_table2.size() == 2
    item = hash_table2.lookup_item(123)
    assert torch.all(torch.eq(item.w, torch.FloatTensor([1.0] * dim)))

    item = hash_table2.lookup_item(321)
    assert torch.all(torch.eq(item.w, torch.FloatTensor([4.0] * dim)))


@pytest.mark.parametrize('dim', [1, 3, 100])
def test_partially_interrupted_deserialize_stream(dim, hash_cls_and_item):
    hash_cls, hash_item_cls = hash_cls_and_item
    hash_table = HashEmbedding(hash_cls(dim))
    item = hash_item_cls(dim)
    item.w = torch.FloatTensor([2.0] * dim)
    hash_table.insert_item(123, item)
    item = hash_item_cls(dim)
    item.w = torch.FloatTensor([4.0] * dim)
    hash_table.insert_item(321, item)
    assert hash_table.size() == 2
    save_stream = hash_table.get_serialization_stream(buffer_size=1000000)
    string = b''.join(list(save_stream))
    assert string

    hash_table = HashEmbedding(hash_cls(dim))
    load_stream = hash_table.get_deserialization_stream()
    # here, we put a char-by-char to the deserialization stream. This will cover all use-cases
    for s in string:
        # iterate over bytes gives int (char number)
        b = bytes([s])
        load_stream.feed_data(b)

    assert hash_table.size() == 2
    item = hash_table.lookup_item(123)
    assert torch.all(torch.eq(item.w, torch.FloatTensor([2.0] * dim)))

    item = hash_table.lookup_item(321)
    assert torch.all(torch.eq(item.w, torch.FloatTensor([4.0] * dim)))
