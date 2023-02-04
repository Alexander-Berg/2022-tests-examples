from ads_pytorch import (
    HashEmbedding,
    create_hash_table_item,
    create_hash_table
)
import torch
from ads_pytorch.cpp_lib import libcpp_lib
from ads_pytorch.yt.data_loader import realvalue_feature_name, cat_feature_data_name


def test_add_functor():
    torch.manual_seed(8458275)
    dim = 3
    embedding = HashEmbedding(create_hash_table("prediction", dim))
    embed_tensor = torch.rand(3, dim)
    r = libcpp_lib.DictionaryLikeRecord({
        cat_feature_data_name("Keys"): torch.LongTensor([4, 6, 785]),
        realvalue_feature_name("Embeddings"): embed_tensor
    })
    functor = libcpp_lib.TCacheEmbeddingAddFunctor(embedding.parameter_with_hash_table.hash_table, r)
    functor()

    res = embedding(torch.LongTensor([4, 6, 787, 785]), torch.IntTensor([1, 1, 1, 1]))
    reference = torch.zeros(4, 3)
    reference[:2, :].copy_(embed_tensor[:2, :])
    reference[-1, :].copy_(embed_tensor[-1, :])
    assert torch.allclose(reference, res)
    assert functor.GetAddCount() == 3


def test_remove_functor():
    torch.manual_seed(8458275)
    dim = 3
    features = [4, 6, 785]
    embedding = HashEmbedding(create_hash_table("prediction", dim))
    for i in features:
        item = create_hash_table_item("prediction", dim)
        item.w = torch.ones(dim) * 3
        embedding.insert_item(i, item)

    res = embedding(torch.LongTensor([4, 6, 787, 785]), torch.IntTensor([1, 1, 1, 1]))
    assert not torch.allclose(res, torch.zeros_like(res))

    r = libcpp_lib.DictionaryLikeRecord({
        cat_feature_data_name("Keys"): torch.LongTensor(features)
    })
    functor = libcpp_lib.TCacheEmbeddingRemoveFunctor(embedding.parameter_with_hash_table.hash_table, r)
    functor()

    res = embedding(torch.LongTensor([4, 6, 787, 785]), torch.IntTensor([1, 1, 1, 1]))
    assert torch.allclose(res, torch.zeros_like(res))
    assert functor.GetRemoveCount() == 3
