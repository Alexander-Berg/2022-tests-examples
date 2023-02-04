from typing import List, Dict, Union
import torch
import pytest
from densenet_tsar_query_attention_v2.multihead_network.named_split import (
    NamedSplit,
    FeatureDescriptor,
    FeatureOrderHolder,
    build_named_split_with_main_head
)
from ads_pytorch.nn.module.base_embedding_model import EmbeddingDescriptor
from ads_pytorch.nn.module.single_matrix_output_network import SingleMatrixOutputMixin
from ads_pytorch.nn.module.named_matrix_output_mixin import NamedMatrixListOutputMixin


class _HelperNet(torch.nn.Module, NamedMatrixListOutputMixin):
    def __init__(self, networks: Dict[str, Union[SingleMatrixOutputMixin, torch.nn.Module]]):
        super(_HelperNet, self).__init__()
        self.networks = torch.nn.ModuleDict(networks)
        self.dims: Dict[str, int] = {key: net.get_out_features() for key, net in networks.items()}
        self.names = list(self.dims.keys())

    def forward(self, tensor):
        return [self.networks[k](tensor) for k in self.names]

    def get_names_order(self) -> List[str]:
        return self.names[:]

    def get_output_dims(self) -> Dict[str, int]:
        return self.dims.copy()


def _holder_from_features(features: List[FeatureDescriptor]) -> FeatureOrderHolder:
    return FeatureOrderHolder({d.feature: i for i, d in enumerate(features)})


@pytest.mark.parametrize(
    ["features", "dim_dict", "reference"],
    [
        (
            [
                FeatureDescriptor(feature="a", split_dims={"h1": 1})
            ],
            {"h1": 1},
            {
                "h1": torch.tensor([1.])
            }
        ),
        (
            [
                FeatureDescriptor(feature="a", split_dims={"h1": 1, "h2": 2, "h3": 3})
            ],
            {"h1": 1, "h2": 2, "h3": 3},
            {
                "h1": torch.tensor([1.]),
                "h2": torch.tensor([1., 1.]),
                "h3": torch.tensor([1., 1., 1.])
            }
        ),
        (
            [
                FeatureDescriptor(feature="a", split_dims={"h1": 2}),
                FeatureDescriptor(feature="b", split_dims={"h2": 3}),
                FeatureDescriptor(feature="c", split_dims={"h3": 1})
            ],
            {"h1": 2, "h2": 3, "h3": 1},
            {
                "h1": torch.tensor([1., 1.]),
                "h2": torch.tensor([2., 2., 2.]),
                "h3": torch.tensor([3.])
            }
        ),
        (
            [
                FeatureDescriptor(feature="a", split_dims={"h1": 2, "h2": 5}),
                FeatureDescriptor(feature="b", split_dims={"h2": 3, "h3": 1}),
                FeatureDescriptor(feature="c", split_dims={"h1": 2, "h3": 2})
            ],
            {"h1": 4, "h2": 8, "h3": 3},
            {
                "h1": torch.tensor([1., 1., 3., 3.]),
                "h2": torch.tensor([1., 1., 1., 1., 1., 2., 2., 2.]),
                "h3": torch.tensor([2., 3., 3.])
            }
        ),
        (
            [
                FeatureDescriptor(feature="a", split_dims={"h1": 2, "h2": 5}),
                FeatureDescriptor(feature="b", split_dims={"h2": 3, "h3": 1}),
                FeatureDescriptor(feature="c", split_dims={"h1": 2, "h3": 2, "h4": 8})
            ],
            {"h1": 4, "h2": 8, "h3": 3, "h4": 8},
            {
                "h1": torch.tensor([1., 1., 3., 3.]),
                "h2": torch.tensor([1., 1., 1., 1., 1., 2., 2., 2.]),
                "h3": torch.tensor([2., 3., 3.]),
                "h4": torch.tensor([3.])
            }
        ),
    ]
)
def test_feature_split_dimensions(features, dim_dict, reference):
    holder = _holder_from_features(features=features)

    net = NamedSplit(
        features=features,
        feature_order_holder=holder,
        sub_networks={},
        normalizers={}
    )

    inputs = [
        torch.full((1, sum(d.split_dims.values())), fill_value=float(i + 1))
        for i, d in enumerate(features)
    ]
    output = net(inputs)
    assert len(output) == len(net.get_names_order())
    assert net.get_output_dims() == dim_dict

    for head_name, tensor in zip(net.get_names_order(), output):
        assert tensor.size()[-1] == dim_dict[head_name]
        assert torch.allclose(tensor, reference[head_name])


class ConstantAddNet(torch.nn.Module, SingleMatrixOutputMixin):
    def __init__(self, value: float, name: str, holder: FeatureOrderHolder, dim: int):
        super(ConstantAddNet, self).__init__()
        self.value = value
        self.name = name
        self.holder = holder
        self.dim = dim

    def forward(self, tensors):
        return tensors[self.holder.get_ids([self.name])[0]] + self.value

    def get_out_features(self) -> int:
        return self.dim


def test_subnetworks():
    holder = FeatureOrderHolder({n: i for i, n in enumerate(["a", "b", "c"])})
    net = NamedSplit(
        features=[],
        feature_order_holder=holder,
        sub_networks={
            "net1": _HelperNet({
                "h1": ConstantAddNet(value=1., name="a", holder=holder, dim=1),
                "h2": ConstantAddNet(value=2., name="b", holder=holder, dim=2),
            }),
            "net2": _HelperNet({
                "h2": ConstantAddNet(value=3., name="c", holder=holder, dim=3),
                "h3": ConstantAddNet(value=4., name="b", holder=holder, dim=2),
            }),
            "net3": _HelperNet({
                "h1": ConstantAddNet(value=5., name="c", holder=holder, dim=3),
                "h3": ConstantAddNet(value=6., name="a", holder=holder, dim=1),
            }),
        },
        normalizers={}
    )

    reference = {
        "h1": torch.tensor([11., 1005., 1005., 1005.]),
        "h2": torch.tensor([102., 102., 1003., 1003., 1003.]),
        "h3": torch.tensor([104., 104., 16.])
    }

    inputs = [
        torch.tensor([10.]),
        torch.tensor([100., 100.]),
        torch.tensor([1000., 1000., 1000.]),
    ]

    outputs = net(inputs)
    for name, tensor in zip(net.get_names_order(), outputs):
        assert net.get_output_dims()[name] == tensor.size()[-1]
        assert torch.allclose(tensor, reference[name])


# Many network configurations are machine-generated
# This can cause troubles in complex cases when some features/subnetworks are reordered
# NamedSplit concatenates many features and subnets into single tensor, and
# for same *set* of feature descriptors and same *set* of subnetworks it must have
# persistent result
# FIXME implement
@pytest.mark.skip
def test_shuffle_invariance():
    pass


def test_named_split_with_main_head_factory():
    cfg = {
        "main_head": "shared",
        "feature_heads_ratio": {
            "shared": 0.6,
            "is_click": 0.2,
            "gg": 0.2
        },
        "features": [
            "PageID",
            "BannerID",
            "UniqID"
        ]
    }

    embeddings = [
        EmbeddingDescriptor(name="PageID", features=["PageID"], dim=10),
        EmbeddingDescriptor(name="BannerID", features=["BannerID"], dim=11),
        EmbeddingDescriptor(name="UniqID", features=["UniqID"], dim=12),
    ]

    holder = FeatureOrderHolder({"PageID": 0, "BannerID": 1, "UniqID": 2})

    sub_networks = {
        "some_net_in_shared_head": ConstantAddNet(value=1., name="PageID", holder=holder, dim=10),  # this goes to shared head
        "some_multihead_net": _HelperNet({
            "extra_head": ConstantAddNet(value=1., name="BannerID", holder=holder, dim=11),
            "is_click": ConstantAddNet(value=5., name="PageID", holder=holder, dim=10),
            "gg": ConstantAddNet(value=17., name="UniqID", holder=holder, dim=12),
        })
    }

    build_res = build_named_split_with_main_head(
        cfg=cfg,
        sub_networks=sub_networks,
        normalizers={},
        embedding_descriptors=embeddings,
        feature_order_holder=holder,
        unique_prefix="",
        common_embedding_normalizer_config=None
    )
    assert build_res.features == {"PageID", "BannerID", "UniqID"}
    network: NamedSplit = build_res.net

    assert set(network.get_names_order()) == {"shared", "is_click", "gg", "extra_head"}
    assert network.get_output_dims() == {
        "shared": 21 + 10,
        "is_click": 6 + 10,
        "gg": 6 + 12,
        "extra_head": 11
    }

    inputs = [
        torch.rand(10, 10),
        torch.rand(10, 11),
        torch.rand(10, 12),
    ]

    results = network(inputs)
    for name, res in zip(network.get_names_order(), results):
        assert res.size()[-1] == network.get_output_dims()[name]
