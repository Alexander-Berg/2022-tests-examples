import pytest
import torch
import torch.nn.intrinsic as nni
from densenet_tsar_query_attention_v2.generic_feed_forward_with_normalizers import (
    GenericFeedForwardWithPerFeatureNormalizers,
    GenericFeedForwardWithHeads,
    HeadDescriptor
)
from densenet_tsar_query_attention_v2.identity_network import IdentityNetwork
from ads_pytorch.nn.module.in_feature_projection import InFeatureProjection
from ads_pytorch.deploy import dump_model_to_eigen_format
from ads_pytorch.nn.module.float_normalizer import FloatInputsNormalizer
from ads_pytorch.nn.module.base_embedding_model import FeatureOrderHolder


@pytest.fixture()
def holder():
    feature_to_id = {f"factor{i}": i for i in range(10)}
    return FeatureOrderHolder(feature_to_id=feature_to_id)


def test_deployable(holder):
    model = GenericFeedForwardWithPerFeatureNormalizers(
        deep=torch.nn.Sequential(
            nni.LinearReLU(torch.nn.Linear(10, 10), torch.nn.ReLU()),
            nni.LinearReLU(torch.nn.Linear(10, 10), torch.nn.ReLU()),
            torch.nn.Linear(10, 5)
        ),
        feature_order_holder=holder,
        normalizers={
            "factor0": torch.nn.Identity(),
            "factor2": FloatInputsNormalizer(3)
        },
        sub_networks={
            "some_subnet": torch.nn.Sequential(
                nni.LinearReLU(torch.nn.Linear(10, 10), torch.nn.ReLU()),
                nni.LinearReLU(torch.nn.Linear(10, 10), torch.nn.ReLU()),
                torch.nn.Linear(10, 5)
            )
        },
        deep_features=[f"factor{i}" for i in range(4)],

    )
    dump_model_to_eigen_format(model=model)


def test_deployable_with_heads(holder):
    head_descriptors = {
        "IsClick": HeadDescriptor(
            deep_part=torch.nn.Linear(5, 5),
            input_compression_module=IdentityNetwork(dim=5),
            uniq_embeddings_dim={
                f"factor{i}": 1 for i in range(4)
            }
        ),
        "GG": HeadDescriptor(
            deep_part=torch.nn.Linear(5, 5),
            input_compression_module=IdentityNetwork(dim=5),
            uniq_embeddings_dim={
                f"factor{i}": 1 for i in range(4)
            }
        )
    }
    model = GenericFeedForwardWithHeads(
        shared_deep=torch.nn.Sequential(
            nni.LinearReLU(torch.nn.Linear(10, 10), torch.nn.ReLU()),
            nni.LinearReLU(torch.nn.Linear(10, 10), torch.nn.ReLU()),
            torch.nn.Linear(10, 5)
        ),
        heads=head_descriptors,
        split_embeds=True,
        external_features=["factor2"],
        feature_order_holder=holder,
        normalizers={
            "factor0": torch.nn.Identity(),
            "factor2": FloatInputsNormalizer(3)
        },
        sub_networks={
            "some_subnet": torch.nn.Sequential(
                nni.LinearReLU(torch.nn.Linear(10, 10), torch.nn.ReLU()),
                nni.LinearReLU(torch.nn.Linear(10, 10), torch.nn.ReLU()),
                torch.nn.Linear(10, 5)
            )
        },
        deep_features=[f"factor{i}" for i in range(4)],
    )
    dump_model_to_eigen_format(model=model)


class MyNormalizer(torch.nn.Module):
    def __init__(self, value: int):
        super(MyNormalizer, self).__init__()
        self.value = value

    def forward(self, tensor):
        return tensor + self.value


class AnyInputs(torch.nn.Module):
    def forward(self, inputs):
        return inputs


class AnyInputsConcat(torch.nn.Module):
    def forward(self, inputs):
        return torch.cat(inputs, dim=1)


class NormalizerAsSubnetwork(torch.nn.Module):
    def __init__(self, feature_name: str, feature_holder: FeatureOrderHolder, normalizer: torch.nn.Module):
        super(NormalizerAsSubnetwork, self).__init__()
        self._holder = feature_holder
        self._feature_name = feature_name
        self._normalizer = normalizer

    def forward(self, inputs):
        feature = inputs[self._holder.get_ids([self._feature_name])[0]]
        return self._normalizer(feature)


def test_output(holder):
    torch.manual_seed(12345)
    model = GenericFeedForwardWithPerFeatureNormalizers(
        deep=AnyInputs(),
        deep_features=["factor0", "factor5", "factor7", "factor9"],
        feature_order_holder=holder,
        normalizers={
            "factor0": MyNormalizer(10),
            "factor7": MyNormalizer(100)
        },
        sub_networks={
            "some_subnet": AnyInputsConcat(),
            "factor5_subnet": NormalizerAsSubnetwork(
                feature_name="factor5",
                feature_holder=holder,
                normalizer=MyNormalizer(2)
            )
        }
    )

    inputs = [torch.rand(5, 1) for _ in range(10)]

    res = model(inputs)
    references = [
        inputs[0] + 10,
        inputs[5],
        inputs[7] + 100,
        inputs[9],
        # must be in order of sub_networks dict keys
        *inputs,
        inputs[5] + 2,
    ]
    assert torch.allclose(res, torch.cat(references, dim=1))

    state_dict = model.state_dict()
    _new_subnet_order = ["factor5_subnet", "some_subnet"]
    assert state_dict["sub_networks_order"] != _new_subnet_order
    state_dict["sub_networks_order"] = _new_subnet_order[:]
    model.load_state_dict(state_dict)
    assert model.sub_networks_order == _new_subnet_order

    res = model(inputs)
    references = [
        inputs[0] + 10,
        inputs[5],
        inputs[7] + 100,
        inputs[9],
        # shuffled inputs[5] and *inputs
        inputs[5] + 2,
        *inputs,
    ]
    assert torch.allclose(res, torch.cat(references, dim=1))

    # Check that deep_features_order must always be the same
    state_dict = model.state_dict()
    _new_feature_order = ["factor5", "factor0", "factor9", "factor7"]
    assert state_dict["deep_features"] != _new_feature_order
    state_dict["deep_features"] = _new_feature_order[:]
    with pytest.raises(RuntimeError):
        model.load_state_dict(state_dict)


def test_output_fixed_order(holder):
    torch.manual_seed(12345)
    model = GenericFeedForwardWithPerFeatureNormalizers(
        deep=AnyInputs(),
        deep_features=["factor0", "factor5", "factor7", "factor9"],
        feature_order_holder=holder,
        normalizers={
            "factor0": MyNormalizer(10),
            "factor7": MyNormalizer(100)
        },
        sub_networks={
            "some_subnet": AnyInputsConcat(),
            "factor5_subnet": NormalizerAsSubnetwork(
                feature_name="factor5",
                feature_holder=holder,
                normalizer=MyNormalizer(2)
            ),
            "factor1_subnet": NormalizerAsSubnetwork(
                feature_name="factor1",
                feature_holder=holder,
                normalizer=MyNormalizer(3)
            )
        },
        sub_networks_order=["factor5_subnet", "factor1_subnet", "some_subnet"]
    )

    inputs = [torch.rand(5, 1) for _ in range(10)]

    res = model(inputs)
    references = [
        inputs[0] + 10,
        inputs[5],
        inputs[7] + 100,
        inputs[9],
        inputs[5] + 2,
        inputs[1] + 3,
        *inputs,
    ]
    assert torch.allclose(res, torch.cat(references, dim=1))

    # state dict testing
    state_dict = model.state_dict()
    state_dict["sub_networks_order"] = ["some_subnet", "factor5_subnet", "factor1_subnet"]
    with pytest.raises(RuntimeError):
        model.load_state_dict(state_dict)


class MultiplierLayer(torch.nn.Module):
    def __init__(self, coef: float):
        super(MultiplierLayer, self).__init__()
        self._coef = coef

    def forward(self, tensor):
        return tensor * self._coef


def test_output_with_heads(holder):
    torch.manual_seed(12345)
    head_descriptors = {
        "IsClick": HeadDescriptor(
            deep_part=AnyInputs(),
            input_compression_module=InFeatureProjection(in_feature=34, projector=MultiplierLayer(coef=-1.0)),
            uniq_embeddings_dim={
                f"factor{i}": 2 for i in [0, 5, 7, 9]
            }
        ),
        "GG": HeadDescriptor(
            deep_part=AnyInputs(),
            input_compression_module=InFeatureProjection(in_feature=34, projector=MultiplierLayer(coef=2.0)),
            uniq_embeddings_dim={
                f"factor{i}": 2 for i in [0, 5, 7, 9]
            }
        )
    }
    model = GenericFeedForwardWithHeads(
        shared_deep=AnyInputs(),
        heads=head_descriptors,
        split_embeds=True,
        external_features=["factor2"],
        feature_order_holder=holder,
        normalizers={
            "factor0": MyNormalizer(10),
            "factor7": MyNormalizer(100)
        },
        sub_networks={
            "some_subnet": AnyInputsConcat()
        },
        deep_features=["factor0", "factor2", "factor5", "factor7", "factor9"]
    )
    inputs = [torch.rand(5, 10) for _ in range(10)]
    res = model(inputs)
    ref_click = [
        -(inputs[0][:, :6] + 10),
        -(inputs[2]),
        -(inputs[5][:, :6]),
        -(inputs[7][:, :6] + 100),
        -(inputs[9][:, :6]),
        *inputs,
        inputs[0][:, 6:8] + 10,
        inputs[5][:, 6:8],
        inputs[7][:, 6:8] + 100,
        inputs[9][:, 6:8]
    ]
    ref_gg = [
        2 * (inputs[0][:, :6] + 10),
        2 * (inputs[2]),
        2 * (inputs[5][:, :6]),
        2 * (inputs[7][:, :6] + 100),
        2 * (inputs[9][:, :6]),
        *inputs,
        inputs[0][:, 8:] + 10,
        inputs[5][:, 8:],
        inputs[7][:, 8:] + 100,
        inputs[9][:, 8:]
    ]
    assert torch.allclose(res["IsClick"], torch.cat(ref_click, dim=1))
    assert torch.allclose(res["GG"], torch.cat(ref_gg, dim=1))


def test_state_dict_save_load(holder):
    head_descriptors = {
        "IsClick": HeadDescriptor(
            deep_part=torch.nn.Linear(5, 5),
            input_compression_module=IdentityNetwork(dim=5),
            uniq_embeddings_dim={
                f"factor{i}": 1 for i in range(4)
            }
        ),
        "GG": HeadDescriptor(
            deep_part=torch.nn.Linear(5, 5),
            input_compression_module=IdentityNetwork(dim=5),
            uniq_embeddings_dim={
                f"factor{i}": 1 for i in range(4)
            }
        )
    }
    model = GenericFeedForwardWithHeads(
        shared_deep=torch.nn.Sequential(
            nni.LinearReLU(torch.nn.Linear(10, 10), torch.nn.ReLU()),
            nni.LinearReLU(torch.nn.Linear(10, 10), torch.nn.ReLU()),
            torch.nn.Linear(10, 5)
        ),
        heads=head_descriptors,
        split_embeds=True,
        external_features=["factor2"],
        feature_order_holder=holder,
        normalizers={
            "factor0": torch.nn.Identity(),
            "factor2": FloatInputsNormalizer(3)
        },
        sub_networks={
            "some_subnet": torch.nn.Sequential(
                nni.LinearReLU(torch.nn.Linear(10, 10), torch.nn.ReLU()),
                nni.LinearReLU(torch.nn.Linear(10, 10), torch.nn.ReLU()),
                torch.nn.Linear(10, 5)
            )
        },
        deep_features=[f"factor{i}" for i in range(4)],
    )
    state_dict = model.state_dict()
    assert "heads_order" in state_dict
    assert "heads_embeddings_dim" in state_dict
    model.load_state_dict(state_dict)
