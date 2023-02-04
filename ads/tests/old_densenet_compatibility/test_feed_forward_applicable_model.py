import torch


from densenet_tsar_query_attention_v2.old_densenet_compatibility.feed_forward_applicable_model import (
    convert_generic_feed_forward_applicable_model_to_ideployable_model,
)
from densenet_tsar_query_attention.feed_forward_applicable_model import (
    GenericFeedForwardApplicableModel
)
from densenet_tsar_query_attention.base_embedding_model import (
    EmbeddingDescriptor as LegacyEmbeddingDescriptor
)
from ads_pytorch.nn.module.densenet import DenseNetEmbeddingNetwork
from ads_pytorch import create_hash_table_item


class AddTwo(torch.nn.Module):
    def forward(self, tensor):
        return tensor + 2


def test_convert_model():
    torch.manual_seed(1298582)
    out_features = 64

    def _make_deep_net() -> DenseNetEmbeddingNetwork:
        return DenseNetEmbeddingNetwork(in_features=16 * 3 + 5 + 7 + 9, out_features=out_features)

    legacy_model = GenericFeedForwardApplicableModel(
        embeddings=[
            LegacyEmbeddingDescriptor(
                name="BannerID",
                features=["BannerID"],
                dim=16,
                algo_type="adam"
            ),
            LegacyEmbeddingDescriptor(
                name="Texts",
                features=["BannerText", "QueryText"],
                dim=16,
                algo_type="adam"
            )
        ],
        normalizers={
            "Rv1": torch.nn.LayerNorm(5),
            "Rv3": AddTwo()
        },
        external_factors={
            "Rv1": 5,
            "Rv2": 7,
            "Rv3": 9
        },
        deep_network=_make_deep_net(),
        out_features=out_features
    )

    # fill legacy model with some values
    for p in legacy_model.deep_parameters():
        torch.nn.init.normal_(p)

    for embed in legacy_model.hash_embedding_parameters():
        for i in range(100):
            item = create_hash_table_item("adam", 16)
            item.w = torch.rand(16)
            embed.hash_table.insert_item(i, item)

    new_model = convert_generic_feed_forward_applicable_model_to_ideployable_model(
        model=legacy_model,
        serializable_model_name="banner"
    )

    # Validate that outputs are same
    inputs = {
        "BannerID": [
            torch.LongTensor([4, 19, 76]),
            torch.IntTensor([1, 1, 1])
        ],
        "BannerText": [
            torch.LongTensor([2, 3, 4, 5, 87, 45, 18]),
            torch.IntTensor([4, 2, 1])
        ],
        "QueryText": [
            torch.LongTensor([34, 56, 62]),
            torch.IntTensor([1, 1, 1])
        ],
        "Rv1": torch.rand(3, 5),
        "Rv2": torch.rand(3, 7),
        "Rv3": torch.rand(3, 9)
    }

    legacy_output = legacy_model(inputs)
    new_output = new_model(inputs)

    assert torch.allclose(legacy_output, new_output)
