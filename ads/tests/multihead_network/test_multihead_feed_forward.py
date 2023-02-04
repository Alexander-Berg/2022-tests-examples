from typing import List
import torch
import pytest
from densenet_tsar_query_attention_v2.multihead_network.multihead_feed_forward import (
    MultiheadFeedForward,
    NamedSplit,
    make_feature_holder_with_additional_features
)
from densenet_tsar_query_attention_v2.multihead_network.named_split import (
    NamedSplit,
    NamedMatrixListOutputMixin,
    FeatureDescriptor
)
from ads_pytorch.nn.module.single_matrix_output_network import SingleMatrixOutputMixin
from ads_pytorch.nn.module.base_embedding_model import (
    BaseEmbeddingModel,
    EmbeddingDescriptor,
    FeatureOrderHolder
)
from densenet_tsar_query_attention_v2.network_factory import build_network


class ConstantAddNet(torch.nn.Module, SingleMatrixOutputMixin):
    def __init__(
        self,
        value: float,
        holder: FeatureOrderHolder,
        feature_names: List[str],
        out: int
    ):
        super(ConstantAddNet, self).__init__()
        self.value = value
        self.holder = holder
        self.feature_names = feature_names
        self.out = out

    def forward(self, inputs):
        out = torch.cat([inputs[i] for i in self.holder.get_ids(self.feature_names)], dim=-1)
        return out + self.value

    def get_out_features(self) -> int:
        return self.out


def _holder_from_features(features: List[FeatureDescriptor]) -> FeatureOrderHolder:
    return FeatureOrderHolder({d.feature: i for i, d in enumerate(features)})


def test_multihead_feed_forward():
    features = [
        FeatureDescriptor(feature="a", split_dims={"h1": 2, "h2": 2, "main": 10}),
        FeatureDescriptor(feature="b", split_dims={"h2": 3, "h3": 4, "main": 12})
    ]

    holder = _holder_from_features(features=features)

    input_split = NamedSplit(
        features=features,
        feature_order_holder=holder,
        sub_networks={},
        normalizers={}
    )

    def _make_holder(features):
        return make_feature_holder_with_additional_features(holder=holder, features=features)

    shared_head = ConstantAddNet(
        value=10000.,
        holder=_make_holder(["shared"]),
        feature_names=["shared"],
        out=22
    )

    heads = {
        "h1": ConstantAddNet(out=22 + 2, value=10., holder=_make_holder(["shared", "head"]), feature_names=["shared", "head"]),
        "h2": ConstantAddNet(out=22 + 5, value=100., holder=_make_holder(["shared", "head"]), feature_names=["shared", "head"]),
        "h3": ConstantAddNet(out=22 + 4, value=1000., holder=_make_holder(["shared", "head"]), feature_names=["shared", "head"]),
    }

    inputs = [
        torch.rand(1, 14),  # "a"
        torch.rand(1, 19)   # "b"
    ]

    multihead = MultiheadFeedForward(
        shared_head=shared_head,
        input_split=input_split,
        shared_head_name="main",
        heads=heads,
        source_feature_count=len(inputs)
    )

    sizes = {
        key: value.get_out_features()
        for key, value in heads.items()
    }

    result = multihead(inputs)
    for head_name, tensor in zip(multihead.get_names_order(), result):
        assert tensor.size()[-1] == sizes[head_name]


def test_build_multihead_network():
    config = {
        "network_type": "multihead_feed_forward_from_named_split",
        "heads": {
            "is_click": {
                "network_type": "generic_feed_forward",
                "network_impl": {
                    "network_type": "densenet_embedding",
                    "width": 16,
                    "depth": 3,
                    "out_features": 10
                },
                "features": [
                    "is_click_custom_feature"
                ]
            },
            "gg": {
                "network_type": "generic_feed_forward",
                "network_impl": {
                    "network_type": "densenet_embedding",
                    "width": 17,
                    "depth": 4,
                    "out_features": 12
                },
                "features": [
                    "gg_custom_feature"
                ]
            }
        },
        "shared_head_name": "shared",
        "shared_head": {
            "network_type": "generic_feed_forward",
            "network_impl": {
                "network_type": "densenet_embedding",
                "width": 24,
                "depth": 6,
                "out_features": 50,
                "normalize": False
            },
            "features": [
                "shared_head_custom_feature"
            ]
        },
        "sub_networks": {
            "split_features": {
                "network_type": "named_split_with_main_head",
                "main_head": "shared",
                "features": [
                    "PageID",
                    "ImpID",
                    "BannerID"
                ],
                "feature_heads_ratio": {
                    "is_click": 0.2,
                    "gg": 0.2,
                    "shared": 0.6
                }
            }
        }
    }

    def _descr(name, dim):
        return EmbeddingDescriptor(name=name, features=[name], dim=dim)

    embeddings = [
        _descr(name="PageID", dim=20),
        _descr(name="ImpID", dim=21),
        _descr(name="BannerID", dim=22),
        _descr(name="gg_custom_feature", dim=23),
        _descr(name="is_click_custom_feature", dim=24),
        _descr(name="shared_head_custom_feature", dim=25)
    ]

    embedding_model = BaseEmbeddingModel(
        embeddings=embeddings,
        external_factors=[]
    )

    build_result = build_network(
        cfg=config,
        embedding_descriptors=embeddings,
        feature_order_holder=embedding_model.get_feature_order_holder()
    )
    assert build_result.features == {
        "PageID",
        "ImpID",
        "BannerID",
        "gg_custom_feature",
        "is_click_custom_feature",
        "shared_head_custom_feature",
    }

    network = build_result.net

    # FIXME check result :(
    network([
        torch.rand(10, 20),
        torch.rand(10, 21),
        torch.rand(10, 22),
        torch.rand(10, 23),
        torch.rand(10, 24),
        torch.rand(10, 25)
    ])
