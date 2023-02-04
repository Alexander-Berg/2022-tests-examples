import torch

from densenet_tsar_query_attention.base_embedding_model import (
    EmbeddingDescriptor,
    BaseEmbeddingModel,
    EmbeddingComputeDescriptor
)
from ads_pytorch.hash_embedding.hash_embedding import create_item


def test_embeddings():
    model = BaseEmbeddingModel(
        embeddings=[
            EmbeddingDescriptor(name="Text", features=["Text1", "Text2"], dim=3),
            EmbeddingDescriptor(name="Categorical", features=["Cat1", "Cat2"], dim=3),
            EmbeddingDescriptor(
                name="ahaha",
                features=[
                    EmbeddingComputeDescriptor(feature="ahaha", compute_mode="mean"),
                    EmbeddingComputeDescriptor(feature="ahaha", compute_mode="mean", add_prob=0.5)
                ],
                dim=3
            )
        ],
        external_factors={"Realvalue1": 2, "Realvalue2": 5}
    )

    assert set(model.embeddings.keys()) == {"Text", "Categorical", "ahaha"}


def test_outputs():
    model = BaseEmbeddingModel(
        embeddings=[
            EmbeddingDescriptor(name="Text", features=["Text1", "Text2"], dim=3),
            EmbeddingDescriptor(name="Categorical", features=["Cat1", "Cat2"], dim=3),
            EmbeddingDescriptor(
                name="DifferentMode",
                features=[
                    EmbeddingComputeDescriptor(feature="DiffSum", compute_mode="sum"),
                    EmbeddingComputeDescriptor(feature="DiffMean", compute_mode="mean"),
                ],
                dim=3
            )
        ],
        external_factors={"Realvalue1": 2, "Realvalue2": 5}
    )

    embed = model.embeddings["Text"]
    for i in [1, 2, 3, 4]:
        item = create_item("adam", 3)
        item.w = torch.FloatTensor([i] * 3)
        embed.insert_item(i, item)

    embed = model.embeddings["Categorical"]
    for i in [10, 20, 30, 40]:
        item = create_item("adam", 3)
        item.w = torch.FloatTensor([i] * 3)
        embed.insert_item(i, item)

    embed = model.embeddings["DifferentMode"]
    for i in [100, 200, 300, 400]:
        item = create_item("adam", 3)
        item.w = torch.FloatTensor([i] * 3)
        embed.insert_item(i, item)

    inputs = {
        "Text1": [torch.LongTensor([1, 2, 3]), torch.IntTensor([3])],
        "Text2": [torch.LongTensor([2, 3, 4]), torch.IntTensor([3])],
        "Cat1": [torch.LongTensor([10, 20, 30]), torch.IntTensor([3])],
        "Cat2": [torch.LongTensor([20, 30, 40]), torch.IntTensor([3])],
        "DiffSum": [torch.LongTensor([100, 200, 300]), torch.IntTensor([3])],
        "DiffMean": [torch.LongTensor([200, 300, 400]), torch.IntTensor([3])],
        "Realvalue1": torch.rand(10, 10, 10),
        "Realvalue2": torch.rand(3, 3, 3, 3)
    }

    baselines = {
        "Text1": torch.FloatTensor([[2] * 3]),
        "Text2": torch.FloatTensor([[3] * 3]),
        "Cat1": torch.FloatTensor([[20] * 3]),
        "Cat2": torch.FloatTensor([[30] * 3]),
        "Realvalue1": inputs["Realvalue1"],
        "Realvalue2": inputs["Realvalue2"],
        "DiffSum": torch.FloatTensor([[600] * 3]),
        "DiffMean": torch.FloatTensor([[300] * 3]),
    }

    res = model(inputs)
    assert isinstance(res, list)
    for i, feature in enumerate(model.feature_order):
        assert torch.allclose(res[i], baselines[feature])
