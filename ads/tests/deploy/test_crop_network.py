from typing import Any, List, Dict, Union

import torch
import tempfile
import pytest
import json
import os

from ads_pytorch.nn.module.single_matrix_output_network import SingleMatrixOutputMixin
from ads_pytorch.nn.module.named_matrix_output_mixin import NamedMatrixListOutputMixin
from ads_pytorch.deploy.deployable_model import (
    IDeployableModel,
    ParameterServerModelDeployDescriptor,
    TorchModuleDeployDescriptor,
    DEFAULT_SERIALIZE_MODE,
    is_ideployable_model,
    CropNetworkDescriptor
)
from ads_pytorch.nn.module.base_embedding_model import (
    BaseEmbeddingModel,
    EmbeddingDescriptor,
    EmbeddingComputeDescriptor,
    FeatureOrderHolder
)
from ads_pytorch.deploy.crop_ideployable_model import (
    crop_subnetworks,
    crop_embedding_model,
    crop_ideployable_model,
    CropSingleMatrix,
    CropNamedOutput
)


class FailOnForward(Exception):
    pass


class Linear(torch.nn.Module, SingleMatrixOutputMixin):
    def __init__(self, linear: torch.nn.Linear, feature_holder: FeatureOrderHolder, features: List[str]):
        super(Linear, self).__init__()
        self.linear = linear
        self.feature_holder = feature_holder
        self.features = features[:]
        self.fail_on_forward = False

    def forward(self, inputs: List[torch.Tensor]):
        if self.fail_on_forward:
            raise FailOnForward("This means you have not really cropped module, it's still computed")
        tensor = torch.cat([inputs[i] for i in self.feature_holder.get_ids(self.features)], dim=1)
        return self.linear(tensor)

    def get_out_features(self) -> int:
        return self.linear.out_features


class MultiheadLinear(torch.nn.Module, NamedMatrixListOutputMixin):
    def __init__(
        self,
        linears: Dict[str, torch.nn.Linear],
        feature_holder: FeatureOrderHolder,
        features: List[str]
    ):
        super(MultiheadLinear, self).__init__()
        self.linears = torch.nn.ModuleDict(linears)
        self.feature_holder = feature_holder
        self.features = features[:]
        self.key_order = sorted(list(self.linears.keys()))
        self.fail_on_forward = False

    def get_names_order(self) -> List[str]:
        return self.key_order[:]

    def get_output_dims(self) -> Dict[str, int]:
        return {key: net.out_features for key, net in self.linears.items()}

    def forward(self, inputs: List[torch.Tensor]):
        if self.fail_on_forward:
            raise FailOnForward("This means you have not really cropped module, it's still computed")
        tensor = torch.cat([inputs[i] for i in self.feature_holder.get_ids(self.features)], dim=1)
        results = {name: net(tensor) for name, net in self.linears.items()}
        return [results[name] for name in self.key_order]


class TopLevelNetwork(torch.nn.Module):
    def __init__(
        self,
        top_level: torch.nn.Module,
        linear: Linear,
        multihead_linear: MultiheadLinear
    ):
        super(TopLevelNetwork, self).__init__()
        self.linear = linear
        self.multihead_linear = multihead_linear
        self.top_level = top_level

    def forward(self, inputs):
        tensor = torch.cat([self.linear(inputs)] + self.multihead_linear(inputs), dim=-1)
        return self.top_level(tensor)


@pytest.fixture
def build_models():
    torch.manual_seed(672687234)

    def _make_descr(name: str, dim: int):
        return EmbeddingDescriptor(name=name, features=[name], dim=dim)

    embedding_model = BaseEmbeddingModel(
        embeddings=[
            _make_descr("x1", 5),
            _make_descr("x2", 6),
            _make_descr("x3", 7)
        ],
        external_factors=[
            "rv1",
            "rv2"
        ]
    )

    linear_model = Linear(
        linear=torch.nn.Linear(5 + 8, 2),
        feature_holder=embedding_model.get_feature_order_holder(),
        features=["x1", "rv1"]
    )

    multihead_linear = MultiheadLinear(
        linears={
            "l1": torch.nn.Linear(9 + 6, 3),
            "l2": torch.nn.Linear(9 + 6, 4),
        },
        feature_holder=embedding_model.get_feature_order_holder(),
        features=["x2", "rv2"]
    )

    model = TopLevelNetwork(
        top_level=torch.nn.Linear(3 + 4 + 2, 5),
        linear=linear_model,
        multihead_linear=multihead_linear
    )

    return model, embedding_model


def test_crop_single_matrix_output(build_models):
    model, embedding_model = build_models
    model: TopLevelNetwork
    embedding_model: BaseEmbeddingModel

    inputs = [
        torch.rand(2, i)
        for i in range(5, 10)
    ]

    reference = model(inputs)
    cropped_reference = model.linear(inputs)
    model.linear.fail_on_forward = True
    with pytest.raises(FailOnForward):  # sanity check
        model(inputs)

    # one is SingleMatrix, other is NamedMatrixList, test different crop code
    cropped_model, new_feature_holder = crop_subnetworks(
        model=model,
        descriptors=[
            CropNetworkDescriptor(
                subnetwork_name="linear",
                replace_names={
                    "linear_prediction": "linear_prediction"
                },
                defaults={}
            )
        ],
        original_feature_order_holder=embedding_model.get_feature_order_holder()
    )
    cropped_model: TopLevelNetwork
    assert isinstance(cropped_model.linear, CropSingleMatrix)
    enhanced_inputs = inputs + [cropped_reference]
    cropped_result = cropped_model(enhanced_inputs)
    assert torch.allclose(cropped_result, reference)


def test_crop_named_matrix_list(build_models):
    model, embedding_model = build_models
    model: TopLevelNetwork
    embedding_model: BaseEmbeddingModel

    inputs = [
        torch.rand(2, i)
        for i in range(5, 10)
    ]

    reference = model(inputs)
    cropped_reference = model.multihead_linear(inputs)
    model.multihead_linear.fail_on_forward = True
    with pytest.raises(FailOnForward):  # sanity check
        model(inputs)

    # one is SingleMatrix, other is NamedMatrixList, test different crop code
    cropped_model, new_feature_holder = crop_subnetworks(
        model=model,
        descriptors=[
            CropNetworkDescriptor(
                subnetwork_name="multihead_linear",
                replace_names={
                    f"replaced_{name}": name
                    for name in model.multihead_linear.get_names_order()
                },
                defaults={}
            )
        ],
        original_feature_order_holder=embedding_model.get_feature_order_holder()
    )
    cropped_model: TopLevelNetwork
    assert isinstance(cropped_model.multihead_linear, CropNamedOutput)
    enhanced_inputs = inputs + cropped_reference
    cropped_result = cropped_model(enhanced_inputs)
    assert torch.allclose(cropped_result, reference)
