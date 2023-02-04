import torch
import pytest
from typing import Dict

from ads_pytorch.hash_embedding.base_embedding_model import (
    EmbeddingDescriptor,
    BaseEmbeddingModel,
    EmbeddingComputeDescriptor,
    RealvalueDescriptor,
    crop_embedding_model
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
                    EmbeddingComputeDescriptor(feature="ahaha_2", compute_mode="mean", add_prob=0.5)
                ],
                dim=3
            )
        ],
        external_factors=["Realvalue1", "Realvalue2"]
    )

    assert set(model.embeddings.keys()) == {"Text", "Categorical", "ahaha"}


def test_outputs():
    torch.manual_seed(783748)
    model = BaseEmbeddingModel(
        embeddings=[
            EmbeddingDescriptor(name="Text", features=["Text1", "Text2"], dim=3),
            EmbeddingDescriptor(name="Categorical", features=["Cat1", "Cat2"], dim=3),
            EmbeddingDescriptor(
                name="DifferentMode",
                features=[
                    EmbeddingComputeDescriptor(feature="DiffSum", compute_mode="sum"),
                    EmbeddingComputeDescriptor(feature="DiffMean", compute_mode="mean")
                ],
                dim=3
            ),
            EmbeddingDescriptor(
                name="DifferentDtype",
                features=[
                    EmbeddingComputeDescriptor(feature="F32_F32", compute_mode="sum", embedding_dtype=torch.float32,
                                               deep_dtype=torch.float32),
                    EmbeddingComputeDescriptor(feature="F32_F16", compute_mode="sum", embedding_dtype=torch.float32,
                                               deep_dtype=torch.float16),
                    EmbeddingComputeDescriptor(feature="F16_F32", compute_mode="sum", embedding_dtype=torch.float16,
                                               deep_dtype=torch.float32),
                    EmbeddingComputeDescriptor(feature="F16_F16", compute_mode="sum", embedding_dtype=torch.float16,
                                               deep_dtype=torch.float16),
                ],
                dim=3
            )
        ],
        external_factors=["Realvalue1", "Realvalue2"]
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

    embed = model.embeddings["DifferentDtype"]
    for i in [1000, 2000, 3000, 4000]:
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
        "Realvalue2": torch.rand(3, 3, 3, 3),
        "F32_F32": [torch.LongTensor([1000]), torch.IntTensor([1])],
        "F32_F16": [torch.LongTensor([2000]), torch.IntTensor([1])],
        "F16_F32": [torch.LongTensor([3000]), torch.IntTensor([1])],
        "F16_F16": [torch.LongTensor([4000]), torch.IntTensor([1])],
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
        "F32_F32": torch.FloatTensor([[1000] * 3]),
        "F32_F16": torch.FloatTensor([[2000] * 3]).half(),
        "F16_F32": torch.HalfTensor([[3000] * 3]).float(),
        "F16_F16": torch.HalfTensor([[4000] * 3]),
    }

    res = model(inputs)
    assert isinstance(res, list)
    holder = model.get_feature_order_holder()
    for i, feature in enumerate(holder.get_order()):
        assert torch.allclose(res[i], baselines[feature])


@pytest.fixture()
def model_and_dims():
    model = BaseEmbeddingModel(
        embeddings=[
            EmbeddingDescriptor(name="Text", features=["Text1", "Text2"], dim=3),
            EmbeddingDescriptor(name="Categorical", features=["Cat1", "Cat2"], dim=4),
            EmbeddingDescriptor(
                name="DifferentMode",
                features=[
                    EmbeddingComputeDescriptor(feature="DiffSum", compute_mode="sum"),
                    EmbeddingComputeDescriptor(feature="DiffMean", compute_mode="mean"),
                ],
                dim=5
            ),
            EmbeddingDescriptor(
                name="DifferentProb",
                features=[
                    EmbeddingComputeDescriptor(feature="FullProb", compute_mode="sum", add_prob=1.0),
                    EmbeddingComputeDescriptor(feature="HalfProb", compute_mode="sum", add_prob=0.5),
                ],
                dim=6
            )
        ],
        external_factors=["Realvalue1", "Realvalue2"]
    )
    all_features = {
        "Text1": 3, "Text2": 3, "Cat1": 4, "Cat2": 4,
        "DiffSum": 5, "DiffMean": 5, "FullProb": 6, "HalfProb": 6,
        "Realvalue1": 2, "Realvalue2": 5
    }

    return model, all_features


def test_inputs_info(model_and_dims):
    model, all_features = model_and_dims
    holder = model.get_feature_order_holder()
    for f in all_features.keys():
        assert f in holder
    assert len(all_features) == len(holder.get_order())
    assert all([f in holder.get_order() for f in all_features.keys()])
    assert sorted(holder.get_ids(all_features.keys())) == list(range(len(all_features)))


def test_add_realvalue_feature(model_and_dims):
    model, all_features = model_and_dims
    model: BaseEmbeddingModel
    holder = model.get_feature_order_holder()
    old_ids = holder.get_ids(all_features.keys())
    old_order = holder.get_order()
    new_feature = "Realvalue3"
    model.add_real_value_feature(RealvalueDescriptor(feature=new_feature))
    assert model._external_factors[-1] == new_feature
    assert new_feature in holder
    assert new_feature == holder.get_order()[-1]
    assert holder.get_ids([new_feature]) == [len(all_features)]
    assert old_ids == holder.get_ids(all_features.keys())
    assert old_order == holder.get_order()[:-1]
    inputs = _make_inputs(model=model, dims_dict={**all_features, "Realvalue3": 40})
    outputs = model.async_forward(inputs)
    assert outputs.external["Realvalue3"].size() == (5, 40)


def test_add_categorical_feature(model_and_dims):
    model, all_features = model_and_dims
    model: BaseEmbeddingModel
    holder = model.get_feature_order_holder()
    old_ids = holder.get_ids(all_features.keys())
    old_order = holder.get_order()
    new_feature = "Cat3"
    model.add_cat_feature_to_existing_embed("Categorical", EmbeddingComputeDescriptor(new_feature, "mean", 0.5))
    assert new_feature in holder
    assert new_feature == holder.get_order()[-1]
    assert holder.get_ids([new_feature]) == [len(all_features)]
    assert old_ids == holder.get_ids(all_features.keys())
    assert old_order == holder.get_order()[:-1]
    inputs = _make_inputs(model=model, dims_dict={**all_features, "Cat3": 4})
    outputs = model.async_forward(inputs)
    assert outputs.embeddings["Cat3"].size() == (5, 4)


def test_add_new_categorical_feature(model_and_dims):
    model, all_features = model_and_dims
    model: BaseEmbeddingModel
    holder = model.get_feature_order_holder()
    old_ids = holder.get_ids(all_features.keys())
    old_order = holder.get_order()
    new_features = ["NewCateg1", "NewCateg2"]
    new_descriptor = EmbeddingDescriptor(
        name="NewCateg",
        features=[
            EmbeddingComputeDescriptor(feature=f, compute_mode="sum") for f in new_features
        ],
        dim=7
    )

    new_hash_embedding = model.add_new_cat_feature(new_descriptor)
    for f in new_features:
        assert f in holder
    assert new_features == holder.get_order()[-2:]
    assert holder.get_ids(new_features) == [len(all_features), len(all_features) + 1]
    assert old_ids == holder.get_ids(all_features.keys())
    assert old_order == holder.get_order()[:-2]
    inputs = _make_inputs(model=model, dims_dict={**all_features, **{f: 7 for f in new_features}})
    outputs = model.async_forward(inputs)
    for f in new_features:
        assert outputs.embeddings[f].size() == (5, 7)


def test_save_load(model_and_dims):
    model, all_features = model_and_dims
    holder = model.get_feature_order_holder()
    old_ids = holder.get_ids(all_features.keys())
    old_order = holder.get_order()
    state_dict = model.state_dict(prefix='some_prefix')
    assert state_dict['some_prefix_feature_order'] == old_order
    for f, pos in zip(all_features.keys(), old_ids):
        assert state_dict['some_prefix_feature_to_id'][f] == pos
    model = BaseEmbeddingModel(
        embeddings=[
            EmbeddingDescriptor(
                name="DifferentProb",
                features=[
                    EmbeddingComputeDescriptor(feature="FullProb", compute_mode="sum", add_prob=1.0),
                    EmbeddingComputeDescriptor(feature="HalfProb", compute_mode="sum", add_prob=0.5),
                ],
                dim=6
            ),
            EmbeddingDescriptor(
                name="DifferentMode",
                features=[
                    EmbeddingComputeDescriptor(feature="DiffSum", compute_mode="sum"),
                    EmbeddingComputeDescriptor(feature="DiffMean", compute_mode="mean"),
                ],
                dim=5
            ),
            EmbeddingDescriptor(name="Text", features=["Text1", "Text2"], dim=3),
            EmbeddingDescriptor(name="Categorical", features=["Cat1", "Cat2"], dim=4),

        ],
        external_factors=["Realvalue1", "Realvalue2"]
    )
    holder = model.get_feature_order_holder()
    assert holder.get_order() != old_order
    assert holder.get_ids(all_features.keys()) != old_ids
    missing = []
    unexpected = []
    errors = []
    model._load_from_state_dict(state_dict, "some_prefix", {}, True, missing, unexpected, errors)
    assert holder.get_order() == old_order
    assert holder.get_ids(all_features.keys()) == old_ids
    assert len(missing) == len(unexpected) == len(errors) == 0


###########################################################
#                        Crop model                       #
###########################################################


def _make_inputs(model: BaseEmbeddingModel, dims_dict: Dict[str, int]):
    batch_size = 5
    externals = {
        key: torch.rand(batch_size, value)
        for key, value in dims_dict.items() if key in model.external_factors
    }
    embeddings = {
        key: [torch.LongTensor([1, 2, 3, 4, 5]), torch.ones(batch_size, dtype=torch.int32)]
        for key, value in dims_dict.items() if key not in model.external_factors
    }
    return {**externals, **embeddings}


@pytest.mark.parametrize(
    ["crop_categorical", "crop_realvalue", "hash_table_names"],
    [
        (
            {"Text1", "Text2", "Cat1", "Cat2"},
            set(),
            ["Text", "Categorical"]
        ),
        (
            {"FullProb", "HalfProb", "DiffSum", "DiffMean", "Text1", "Text2", "Cat1", "Cat2"},
            {"Realvalue1"},
            ["Text", "Categorical", "DifferentProb", "DifferentMode"]
        ),
        (
            {"Realvalue1"},
            {"Realvalue1"},
            []
        ),
        (
            {"Realvalue1", "Realvalue2"},
            {"Realvalue1", "Realvalue2"},
            []
        ),
        (
            {"FullProb", "DiffSum", "Text1", "Cat1"},
            set(),
            ["Text", "Categorical", "DifferentProb", "DifferentMode"]
        ),
    ]
)
def test_crop_model(model_and_dims, crop_categorical, crop_realvalue, hash_table_names):
    model, dims_dict = model_and_dims
    inputs = _make_inputs(model=model, dims_dict=dims_dict)
    crop_features = crop_categorical | crop_realvalue

    inputs = {k: v for k, v in inputs.items() if k in crop_features}
    cropped_model = crop_embedding_model(model=model, features=crop_features)

    assert set(cropped_model.embeddings.keys()) == set(hash_table_names)

    holder = cropped_model.get_feature_order_holder()
    assert set(holder.get_order()) == crop_features

    outputs = cropped_model(inputs)
    assert len(outputs) == len(crop_features)

    assert set(cropped_model.external_factors) == crop_realvalue


def test_empty_embedding():
    model = BaseEmbeddingModel(
        embeddings=[EmbeddingDescriptor(name="x", features=["x"], dim=10)],
        external_factors=[]
    )

    inputs = [
        torch.LongTensor([]),
        torch.IntTensor([[], [], []])
    ]

    res = model({"x": inputs})[0]
    assert res.size() == (3, 0, 10)


def test_empty_realvalue():
    model = BaseEmbeddingModel(
        embeddings=[],
        external_factors=["r"]
    )

    res = model({"r": torch.rand(3, 0, 10)})[0]
    assert res.size() == (3, 0, 10)
