import os

from ads_pytorch import (
    HashEmbedding,
    create_hash_table,
    create_hash_table_item
)

os.environ["OMP_NUM_THREADS"] = "1"
import torch
import asyncio
import pytest

HASH_TYPE = "adam"
HASH_HALF_TYPE = "adam_half"


@pytest.mark.parametrize("hash_type", [HASH_TYPE, HASH_HALF_TYPE])
@pytest.mark.parametrize('dim', [1, 3, 100])
def test_empty_hash_lookup_adam(dim, hash_type):
    hash_table = HashEmbedding(create_hash_table(hash_type, dim))
    item = hash_table.lookup_item(0)
    assert not item.empty()
    assert not torch.all(torch.eq(item.w, torch.FloatTensor([0] * dim)))
    assert torch.all(torch.eq(item.first_moment, torch.FloatTensor([0] * dim)))
    assert torch.all(torch.eq(item.second_moment, torch.FloatTensor([0] * dim)))
    assert item.ttl_counter == 0


@pytest.mark.parametrize("hash_type", [HASH_TYPE, HASH_HALF_TYPE])
@pytest.mark.parametrize('dim', [1, 3, 100])
def test_hash_insert_lookup_adam(dim, hash_type):
    hash_table = HashEmbedding(create_hash_table(hash_type, dim))
    item = hash_table.lookup_item(0)
    assert not item.empty()
    item.w = torch.FloatTensor([4] * dim)
    item.first_moment = torch.FloatTensor([3] * dim)

    # Check for no side-effects
    item2 = hash_table.lookup_item(0)
    assert not torch.all(torch.eq(item2.w, torch.FloatTensor([4] * dim)))
    assert not torch.all(torch.eq(item2.first_moment, torch.FloatTensor([3] * dim)))

    hash_table.insert_item(0, item)
    item2 = hash_table.lookup_item(0)
    assert not item2.empty()
    assert torch.all(torch.eq(item2.w, torch.FloatTensor([4] * dim)))
    assert torch.all(torch.eq(item2.first_moment, torch.FloatTensor([3] * dim)))

    hash_table.insert_item(1, item)
    item2 = hash_table.lookup_item(1)
    assert not item2.empty()
    assert torch.all(torch.eq(item2.w, torch.FloatTensor([4] * dim)))
    assert torch.all(torch.eq(item2.first_moment, torch.FloatTensor([3] * dim)))


##################################################################
#               TEST HASH EMBEDDING SERIALIZATION                #
##################################################################


@pytest.mark.parametrize("hash_type", [HASH_TYPE, HASH_HALF_TYPE])
@pytest.mark.parametrize('dim', [1, 3, 100])
@pytest.mark.parametrize('buffer_size', [1, 1000000])
def test_serialize_model(dim, buffer_size, hash_type):
    hash_table = HashEmbedding(create_hash_table(hash_type, dim))
    item = create_hash_table_item(hash_type, dim)
    item.first_moment = torch.FloatTensor([1.0] * dim)
    item.second_moment = torch.FloatTensor([2.0] * dim)
    hash_table.insert_item(123, item)
    item = create_hash_table_item(hash_type, dim)
    item.first_moment = torch.FloatTensor([3.0] * dim)
    item.w = torch.FloatTensor([4.0] * dim)
    hash_table.insert_item(321, item)

    assert hash_table.size() == 2
    save_stream = hash_table.get_serialization_stream(buffer_size=buffer_size)
    string = b''.join(list(save_stream))
    assert string

    save_stream = hash_table.get_serialization_stream(buffer_size=buffer_size)

    hash_table2 = HashEmbedding(create_hash_table(hash_type, dim))
    load_stream = hash_table2.get_deserialization_stream()
    for s in save_stream:
        load_stream.feed_data(s)
    assert hash_table2.size() == 2
    item = hash_table2.lookup_item(123)
    assert torch.all(torch.eq(item.first_moment, torch.FloatTensor([1.0] * dim)))
    assert torch.all(torch.eq(item.second_moment, torch.FloatTensor([2.0] * dim)))

    item = hash_table2.lookup_item(321)
    assert torch.all(torch.eq(item.first_moment, torch.FloatTensor([3.0] * dim)))
    assert torch.all(torch.eq(item.w, torch.FloatTensor([4.0] * dim)))


@pytest.mark.parametrize("hash_type", [HASH_TYPE, HASH_HALF_TYPE])
@pytest.mark.parametrize('dim', [1, 3, 100])
def test_partially_interrupted_deserialize_stream(dim, hash_type):
    hash_table = HashEmbedding(create_hash_table(hash_type, dim))
    item = create_hash_table_item(hash_type, dim)
    item.first_moment = torch.FloatTensor([1.0] * dim)
    item.w = torch.FloatTensor([2.0] * dim)
    hash_table.insert_item(123, item)
    item = create_hash_table_item(hash_type, dim)
    item.first_moment = torch.FloatTensor([3.0] * dim)
    item.w = torch.FloatTensor([4.0] * dim)
    hash_table.insert_item(321, item)
    assert hash_table.size() == 2
    save_stream = hash_table.get_serialization_stream(buffer_size=1000000)
    string = b''.join(list(save_stream))
    assert string

    hash_table = HashEmbedding(create_hash_table(hash_type, dim))
    load_stream = hash_table.get_deserialization_stream()
    # here, we put a char-by-char to the deserialization stream. This will cover all use-cases
    for s in string:
        # iterate over bytes gives int (char number)
        b = bytes([s])
        load_stream.feed_data(b)

    assert hash_table.size() == 2
    item = hash_table.lookup_item(123)
    assert torch.all(torch.eq(item.first_moment, torch.FloatTensor([1.0] * dim)))
    assert torch.all(torch.eq(item.w, torch.FloatTensor([2.0] * dim)))

    item = hash_table.lookup_item(321)
    assert torch.all(torch.eq(item.first_moment, torch.FloatTensor([3.0] * dim)))
    assert torch.all(torch.eq(item.w, torch.FloatTensor([4.0] * dim)))
