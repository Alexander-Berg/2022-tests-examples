from typing import Any, Optional, AsyncContextManager

import torch
import pytest
import tempfile
import contextlib
import datetime

from densenet_tsar_query_attention.user_network import (
    UserNetwork,
    UserNetworkForTzarConvolution,
    EmbeddingDescriptor
)
from deploy.production_model import DeployableModel
from ads_pytorch.deploy import register_meta_serializer, MODULE_NAME_SERIALIZE_KEY, VERSION_SERIALIZE_KEY

from ads_pytorch.yt.file_system_adapter_mock import CypressAdapterMock
from ads_pytorch.core.disk_adapter import DiskSavePool
from deploy.callbacks.cpp_apply_processor import CppApplyProcessorCallbackTorchYAML
from ads_pytorch.online_learning.production.uri import ProdURI, DatetimeURI
from ads_pytorch.online_learning.production.artifact.storage import AbstractArtifactStorage, NewValueCtx
from ads_pytorch import create_hash_table_item, HashEmbedding


class FakeStorage(AbstractArtifactStorage):

    @contextlib.asynccontextmanager
    async def new_artifact_path(self, artifact_name: str, uri: Any, parent_tx: Optional[Any] = None) -> \
    AsyncContextManager[NewValueCtx]:
        with tempfile.TemporaryDirectory() as tmp_dir:
            yield NewValueCtx(path=tmp_dir, tx=parent_tx)


class AnyInputs(torch.nn.Module):
    def forward(self, inputs):
        return inputs


@register_meta_serializer(AnyInputs)
def _save(module):
    return {
        MODULE_NAME_SERIALIZE_KEY: "identity",
        VERSION_SERIALIZE_KEY: 1
    }


@pytest.fixture
def model():
    return UserNetwork(
        embeddings=[
            EmbeddingDescriptor(name="UniqID", features=["UniqID"], dim=10),
            EmbeddingDescriptor(name="Gender", features=["Gender"], dim=10),
            EmbeddingDescriptor(name="Texts", features=["Text1", "Text2"], dim=10),
            EmbeddingDescriptor(name="SelectTypes", features=["St1", "St2"], dim=10),
        ],
        external_factors={"QueryFactors": 100},
        normalizers={},
        top_net=AnyInputs(),
        transformer_net=torch.nn.TransformerEncoder(
            encoder_layer=torch.nn.TransformerEncoderLayer(
                d_model=15,
                nhead=1,
                dim_feedforward=10,
                dropout=0
            ),
            num_layers=3
        ),
        attention_features={
            "Texts": ["Text1", "Text2"],
            "SelectTypes": ["St1", "St2"]
        },
        top_net_features=["UniqID", "Gender"],
        query_factors_count=40,
        out_features=10
    )


def test_user_network_outputs(model):
    query_factors_count = 40

    def _randcat():
        return [
            torch.randint(100, size=(3, ), dtype=torch.int64),
            torch.IntTensor([3])
        ]

    inputs = {
        name: _randcat()
        for name in ["Text1", "Text2", "St1", "St2", "UniqID", "Gender"]
    }
    inputs["QueryFactors"] = torch.rand(1, query_factors_count, 5)

    # todo: check outputs
    model(inputs)


class SimpleDeployableModel(DeployableModel):
    def __init__(self, net):
        super(SimpleDeployableModel, self).__init__()
        self.net = net

    def get_applicable_models(self):
        return {"model": self.net}


@pytest.mark.asyncio
async def test_deploy(model):
    with tempfile.TemporaryDirectory() as model_dir:
        cb = CppApplyProcessorCallbackTorchYAML(
            file_system_adapter=CypressAdapterMock(),
            upload_pool=DiskSavePool(10, ),
            artifact_storage=FakeStorage(),
            artifact_name="tsar_processed_model",
            min_frequency=0,
        )

        await cb(
            model=SimpleDeployableModel(model),
            optimizer=None,
            loss=None,
            uri=ProdURI(
                uri=DatetimeURI("//home/f1", date=datetime.datetime.now()),
                force_skip=False
            )
        )

        # fixme will we check files here?..


def test_remove_external_factor():
    # model1 has ShowTime, model2 does not
    model1 = UserNetwork(
        embeddings=[
            EmbeddingDescriptor(name="UniqID", features=["UniqID"], dim=10),
            EmbeddingDescriptor(name="Gender", features=["Gender"], dim=10),
            EmbeddingDescriptor(name="Texts", features=["Text1", "Text2"], dim=10),
            EmbeddingDescriptor(name="SelectTypes", features=["St1", "St2"], dim=10),
        ],
        external_factors={"QueryFactors": 100, "ShowTime": 2},
        normalizers={},
        top_net=AnyInputs(),
        transformer_net=torch.nn.TransformerEncoder(
            encoder_layer=torch.nn.TransformerEncoderLayer(
                d_model=15,
                nhead=1,
                dim_feedforward=10,
                dropout=0
            ),
            num_layers=3
        ),
        attention_features={
            "Texts": ["Text1", "Text2"],
            "SelectTypes": ["St1", "St2"]
        },
        top_net_features=["UniqID", "Gender"],
        query_factors_count=40,
        out_features=10
    )

    model2 = UserNetwork(
        embeddings=[
            EmbeddingDescriptor(name="UniqID", features=["UniqID"], dim=10),
            EmbeddingDescriptor(name="Gender", features=["Gender"], dim=10),
            EmbeddingDescriptor(name="Texts", features=["Text1", "Text2"], dim=10),
            EmbeddingDescriptor(name="SelectTypes", features=["St1", "St2"], dim=10),
        ],
        external_factors={"QueryFactors": 100},
        normalizers={},
        top_net=AnyInputs(),
        transformer_net=torch.nn.TransformerEncoder(
            encoder_layer=torch.nn.TransformerEncoderLayer(
                d_model=15,
                nhead=1,
                dim_feedforward=10,
                dropout=0
            ),
            num_layers=3
        ),
        attention_features={
            "Texts": ["Text1", "Text2"],
            "SelectTypes": ["St1", "St2"]
        },
        top_net_features=["UniqID", "Gender"],
        query_factors_count=40,
        out_features=10
    )

    model2.load_state_dict(model1.state_dict())

    model1_params = dict(model1.named_hash_embedding_parameters())
    model2_params = dict(model2.named_hash_embedding_parameters())
    for key in model1_params.keys():
        p1 = model1_params[key]
        p2 = model2_params[key]

        for i in range(10):
            dim = p1.hash_table.dim()
            weight = torch.rand(dim)

            item = create_hash_table_item("adam", dim)
            item.w = weight
            p1.hash_table.insert_item(i, item)

            item = create_hash_table_item("adam", dim)
            item.w = weight
            p2.hash_table.insert_item(i, item)

    query_factors_count = 40

    def _randcat():
        return [
            torch.randint(10, size=(3,), dtype=torch.int64),
            torch.IntTensor([3])
        ]

    inputs = {
        name: _randcat()
        for name in ["Text1", "Text2", "St1", "St2", "UniqID", "Gender", "ShowTime"]
    }
    inputs["QueryFactors"] = torch.rand(1, query_factors_count, 5)

    assert torch.allclose(model1(inputs), model2(inputs))
