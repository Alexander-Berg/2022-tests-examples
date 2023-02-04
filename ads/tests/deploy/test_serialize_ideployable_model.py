from typing import Any, List, Dict, Union, Optional

import torch
import tempfile
import pytest
import json
import os
import copy

from ads_pytorch.nn.module.single_matrix_output_network import SingleMatrixOutputMixin
from ads_pytorch.deploy.deployable_model import (
    IDeployableModel,
    ParameterServerModelDeployDescriptor,
    TorchModuleDeployDescriptor,
    DEFAULT_SERIALIZE_MODE,
    is_ideployable_model,
    CropNetworkDescriptor
)
from ads_pytorch.deploy.eigen_yaml import (
    register_meta_serializer,
    VERSION_SERIALIZE_KEY,
    MODULE_NAME_SERIALIZE_KEY
)

from ads_pytorch.deploy.deployable_model_serialization import (
    save_ideployable_model,
    register_deep_serializer,
    unregister_deep_serializer,
    IValidationDataSerializer
)
from ads_pytorch.nn.module.base_embedding_model import (
    BaseEmbeddingModel,
    EmbeddingDescriptor,
    EmbeddingComputeDescriptor,
    FeatureOrderHolder
)
from ads_pytorch.tools.progress import ProgressLogger, TimeTracker
from ads_pytorch.core.model_serializer import StdoutModelSaverProgressLogger
from ads_pytorch.core.disk_adapter import DiskFileSystemAdapter, DiskSavePool
from ads_pytorch.model_calcer.concat_wrapper import wrap_model_with_concat_wrapper
from ads_pytorch.nn.module.named_matrix_output_mixin import NamedMatrixListOutputMixin


class MyModule(torch.nn.Module):
    pass


class TorchTensorDumper(IValidationDataSerializer):
    def serialize(self, *args, **kwargs) -> bytes:
        return b'12345'

    def get_meta(self) -> Dict[str, Any]:
        return {
            "type": "ahaha"
        }


@pytest.fixture
def mymodule_serializer():
    @register_deep_serializer("__pytest_model__")
    async def serialize_mymodule(model, fs_adapter, save_pool, path, tx):
        with open(os.path.join(path, "ahaha"), "wt") as f:
            f.write("123")

    yield

    unregister_deep_serializer("__pytest_model__")


class MyModuleDeployable(IDeployableModel):
    def get_serializable_models(self):
        return {
            "model123": TorchModuleDeployDescriptor(
                model=MyModule(),
                serialize_mode="3ejckerfiuherskmfwakjenc"
            )
        }


@pytest.mark.asyncio
async def test_unknown_serializer():
    model = MyModuleDeployable(embedding_model=BaseEmbeddingModel(embeddings=[], external_factors=[]))
    progress_logger = ProgressLogger([StdoutModelSaverProgressLogger()], frequency=30)
    time_tracker = TimeTracker()
    progress_logger.register(time_tracker)
    with tempfile.TemporaryDirectory() as tmp:
        async with DiskSavePool() as save_pool:
            with pytest.raises(ValueError):
                await save_ideployable_model(
                    model=model,
                    path=tmp,
                    fs_adapter=DiskFileSystemAdapter(),
                    save_pool=save_pool,
                    progress_logger=progress_logger,
                    validation_inputs={"x1": torch.rand(10)},
                    validation_tensors_dumper=TorchTensorDumper()
                )


@pytest.mark.asyncio
async def test_custom_serializer(mymodule_serializer):
    class MyModuleDeployable(IDeployableModel):
        def get_serializable_models(self):
            return {
                "model123": TorchModuleDeployDescriptor(
                    model=MyModule(),
                    serialize_mode="__pytest_model__"
                )
            }

    model = MyModuleDeployable(embedding_model=BaseEmbeddingModel(embeddings=[], external_factors=[]))
    progress_logger = ProgressLogger([StdoutModelSaverProgressLogger()], frequency=30)
    time_tracker = TimeTracker()
    progress_logger.register(time_tracker)
    with tempfile.TemporaryDirectory() as tmp:
        async with DiskSavePool() as save_pool:
            await save_ideployable_model(
                model=model,
                path=tmp,
                fs_adapter=DiskFileSystemAdapter(),
                save_pool=save_pool,
                progress_logger=progress_logger,
                validation_inputs={"x1": torch.rand(10)},
                validation_tensors_dumper=TorchTensorDumper()
            )

            with open(os.path.join(tmp, "model123", "ahaha"), "rt") as f:
                res = f.read()
            assert res == "123"

            with open(os.path.join(tmp, "model123", "serialize_mode.json"), "rt") as f:
                res = json.load(f)
            assert res == {"mode": "__pytest_model__"}


class MultiOutputNetwork(torch.nn.Module, NamedMatrixListOutputMixin):
    def __init__(self, outputs: Dict[str, int]):
        super(MultiOutputNetwork, self).__init__()
        self.output_dims = outputs
        self.output_order = sorted(list(self.output_dims.keys()))

    def forward(self, inputs: List[torch.Tensor]):
        batch_size = inputs[0].size()[0]
        outputs = {
            name: torch.full((batch_size, dim), fill_value=dim, dtype=torch.float)
            for name, dim in self.output_dims.items()
        }
        return [outputs[name] for name in self.output_order]

    def get_names_order(self) -> List[str]:
        return self.output_order[:]

    def get_output_dims(self) -> Dict[str, int]:
        return self.output_dims.copy()


@register_meta_serializer(MultiOutputNetwork)
def _serialize_embedinputs_linear(model: MultiOutputNetwork):
    return {
        VERSION_SERIALIZE_KEY: 1,
        MODULE_NAME_SERIALIZE_KEY: "LOL!"
    }


class LinearWithEmbeddingModelInputs(torch.nn.Module, SingleMatrixOutputMixin):
    def __init__(
        self,
        linear: torch.nn.Linear,
        feature_holder: FeatureOrderHolder,
        features: List[str],
        multi_out_subnetwork: Optional[MultiOutputNetwork] = None
    ):
        super(LinearWithEmbeddingModelInputs, self).__init__()
        self.linear = linear
        self.feature_holder = feature_holder
        self.features = features[:]
        self.multi_out_subnetwork = multi_out_subnetwork

    def forward(self, inputs: List[torch.Tensor]):
        if self.multi_out_subnetwork is not None:
            multiout_additional = self.multi_out_subnetwork(inputs)
        else:
            multiout_additional = []
        tensor = torch.cat([inputs[i] for i in self.feature_holder.get_ids(self.features)] + multiout_additional, dim=1)
        return self.linear(tensor)

    def get_out_features(self) -> int:
        return self.linear.out_features


@register_meta_serializer(LinearWithEmbeddingModelInputs)
def _serialize_embedinputs_linear(model: LinearWithEmbeddingModelInputs):
    return {
        VERSION_SERIALIZE_KEY: 1,
        MODULE_NAME_SERIALIZE_KEY: "ahaha"
    }


class _TopLevelModel(torch.nn.Module):
    def __init__(self):
        super(_TopLevelModel, self).__init__()

        self.alpha = torch.nn.Parameter(torch.ones(1))
        self.bias = torch.nn.Parameter(torch.zeros(1))

    def forward(self, vec1: torch.Tensor, vec2: torch.Tensor) -> torch.Tensor:
        return self.alpha * (vec1 * vec2).sum(dim=1) + self.bias


class _DssmImpl(torch.nn.Module):
    def __init__(
        self,
        banner: LinearWithEmbeddingModelInputs,
        user: LinearWithEmbeddingModelInputs
    ):
        super(_DssmImpl, self).__init__()
        self.banner = banner
        self.user = user
        self.top_level = _TopLevelModel()

    def forward(self, embedded_inputs):
        banner = self.banner(embedded_inputs)
        user = self.user(embedded_inputs)
        return self.top_level(banner, user)


@register_meta_serializer(_DssmImpl)
def _serialize_dssm(model: _DssmImpl):
    return {
        VERSION_SERIALIZE_KEY: 1,
        MODULE_NAME_SERIALIZE_KEY: "ahahadssm"
    }


@register_meta_serializer(_TopLevelModel)
def _serialize_top(model: _TopLevelModel):
    return {
        VERSION_SERIALIZE_KEY: 1,
        MODULE_NAME_SERIALIZE_KEY: "ahahadssm"
    }


class DSSM(IDeployableModel):
    def __init__(
        self,
        embedding_model: BaseEmbeddingModel,
        banner: LinearWithEmbeddingModelInputs,
        user: LinearWithEmbeddingModelInputs
    ):
        super(DSSM, self).__init__(embedding_model=embedding_model)
        self.net = _DssmImpl(banner=banner, user=user)

    def deployable_model_forward(self, embedded_inputs):
        return self.net(embedded_inputs)

    def get_serializable_models(self):
        return {
            "whole_network": ParameterServerModelDeployDescriptor(
                features_order=self.net.user.features,
                model=self.net,
                crop_networks=[
                    CropNetworkDescriptor(
                        subnetwork_name="banner",
                        replace_names={
                            "banner_prediction": "banner_prediction"
                        },
                        defaults={}
                    )
                ]
            ),
            "banner": ParameterServerModelDeployDescriptor(
                features_order=self.net.banner.features,
                model=self.net.banner
            ),
            "user": ParameterServerModelDeployDescriptor(
                features_order=self.net.user.features,
                model=self.net.user,
                crop_networks=[
                    CropNetworkDescriptor(
                        subnetwork_name="multi_out_subnetwork",
                        # it's very important to test different replace names
                        # because in production we frequently have several subnetworks
                        # with same head outputs which must be cropped properly with different feature names
                        replace_names={
                            "REPLACE1": "out1",
                            "REPLACE2": "out2"
                        },
                        defaults={}
                    )
                ]
            ),
            "top_level": TorchModuleDeployDescriptor(
                model=self.net.top_level,
                # in production, we will use sth like "ads_tsar_top_level_tensor"
                serialize_mode="__pytest_model__"
            )
        }


@pytest.mark.asyncio
async def test_parameter_server_serialization(mymodule_serializer):
    embedding_model = BaseEmbeddingModel(
        embeddings=[
            EmbeddingDescriptor(
                name="Text1",
                features=[
                    "text1",
                    EmbeddingComputeDescriptor(feature="text2", compute_mode="sum")
                ],
                dim=2
            ),
            EmbeddingDescriptor(name="Text2", features=["text3"], dim=3),
        ],
        external_factors=["rv1", "rv2"]
    )

    banner = LinearWithEmbeddingModelInputs(
        linear=torch.nn.Linear(in_features=2 + 3 + 4, out_features=2),
        feature_holder=embedding_model.get_feature_order_holder(),
        features=["text1", "text3", "rv1"]
    )

    multi_out_dict = {
        "out1": 3,
        "out2": 4
    }

    user = LinearWithEmbeddingModelInputs(
        linear=torch.nn.Linear(in_features=2 + 5 + sum(multi_out_dict.values()), out_features=2),
        feature_holder=embedding_model.get_feature_order_holder(),
        features=["text2", "rv2"],
        multi_out_subnetwork=MultiOutputNetwork(multi_out_dict)
    )

    dssm = DSSM(embedding_model=embedding_model, banner=banner, user=user)

    # sanity check on model correctness
    inputs = {
        "text1": [torch.LongTensor([1, 2]), torch.IntTensor([2])],
        "text2": [torch.LongTensor([3, 4, 5]), torch.IntTensor([3])],
        "text3": [torch.LongTensor([6]), torch.IntTensor([1])],
        "rv1": torch.rand(1, 4),
        "rv2": torch.rand(1, 5)
    }

    output = dssm(inputs)
    output.sum().backward()

    progress_logger = ProgressLogger([StdoutModelSaverProgressLogger()], frequency=30)
    time_tracker = TimeTracker()
    progress_logger.register(time_tracker)
    # serialize it
    with tempfile.TemporaryDirectory() as tmp:
        async with DiskSavePool() as save_pool:
            await save_ideployable_model(
                model=dssm,
                path=tmp,
                fs_adapter=DiskFileSystemAdapter(),
                save_pool=save_pool,
                progress_logger=progress_logger,
                validation_inputs=inputs,
                validation_tensors_dumper=TorchTensorDumper()
            )

            banner_compute_mode = {
                "embeddings": {
                    "Text1": {
                        "features": {
                            "text1": {
                                "compute_mode": "mean",
                                "pad_empty_tensor": True
                            }
                        },
                        "dim": 2
                    },
                    "Text2": {
                        "features": {
                            "text3": {
                                "compute_mode": "mean",
                                "pad_empty_tensor": True
                            }
                        },
                        "dim": 3
                    }
                },
                "external_features": {
                    "rv1": {
                        "pad_empty_tensor": True
                    }
                }
            }

            user_compute_mode = {
                "embeddings": {
                    "Text1": {
                        "features": {
                            "text2": {
                                "compute_mode": "sum",
                                "pad_empty_tensor": True
                            }
                        },
                        "dim": 2
                    }
                },
                "external_features": {
                    "rv2": {
                        "pad_empty_tensor": True
                    },
                    # crop named split network stuff
                    "REPLACE1": {
                        "pad_empty_tensor": True
                    },
                    "REPLACE2": {
                        "pad_empty_tensor": True
                    }
                }
            }

            whole_compute_mode = {
                "embeddings": {
                    "Text1": {
                        "features": {
                            "text2": {
                                "compute_mode": "sum",
                                "pad_empty_tensor": True
                            }
                        },
                        "dim": 2
                    }
                },
                "external_features": {
                    "rv2": {
                        "pad_empty_tensor": True
                    },
                    "banner_prediction": {
                        "pad_empty_tensor": True
                    }
                }
            }

            # Validate embeddings
            for model_name, hash_table_names, feature_order, compute_mode in zip(
                ["banner", "user", "whole_network"],
                [["Text1", "Text2"], ["Text1"], ["Text1"]],
                [["text1", "text3", "rv1"], ["text2", "rv2"], ["text2", "rv2", "banner_prediction"]],
                [banner_compute_mode, user_compute_mode, whole_compute_mode]
            ):
                embedding_path = os.path.join(tmp, model_name, "embedding_model")
                assert os.path.exists(embedding_path)
                assert set(os.listdir(embedding_path)) == {"compute_info.json"} | set(hash_table_names)
                with open(os.path.join(embedding_path, "compute_info.json"), "rt") as f:
                    print(f"Testing {model_name}")
                    loaded = json.load(f)
                    print(loaded)
                    assert loaded == compute_mode

                deep_path = os.path.join(tmp, model_name, "deep_model")
                assert os.path.exists(deep_path)

                assert set(os.listdir(deep_path)) == {
                    "serialize_mode.json",
                    "model_dict.yaml"  # DEFAULT_SERIALIZE_MODE output
                }

                with open(os.path.join(deep_path, "serialize_mode.json"), "rt") as f:
                    assert json.load(f) == {"mode": DEFAULT_SERIALIZE_MODE}

                validation_data_path = os.path.join(tmp, model_name, "validation_data.json")
                assert os.path.exists(validation_data_path)
                with open(validation_data_path, "rb") as f:
                    data = f.read()
                assert data == b'12345'

                meta_path = os.path.join(tmp, model_name, "meta.json")
                with open(meta_path, "rt") as f:
                    assert json.load(f) == {"validation_meta": {"type": "ahaha"}}


@pytest.mark.parametrize('wrap', [True, False], ids=['ConcatWrapped', 'Usual'])
def test_is_ideployable_model(wrap):
    model = MyModuleDeployable(embedding_model=BaseEmbeddingModel(embeddings=[], external_factors=[]))
    if wrap:
        model = wrap_model_with_concat_wrapper(model)
    assert is_ideployable_model(model)

    undeployable_model = torch.nn.Linear(10, 10)
    if wrap:
        undeployable_model = wrap_model_with_concat_wrapper(undeployable_model)
    assert not is_ideployable_model(undeployable_model)
