import torch
import pytest
import tempfile
import contextlib
import datetime

from ads_pytorch import HashEmbedding, create_hash_table_item
from densenet_tsar_query_attention.feed_forward_applicable_model import (
    GenericFeedForwardApplicableModel,
    GenericFeedForwardApplicableModelForTzarConvolution,
    EmbeddingDescriptor,
)
from densenet_tsar_query_attention.base_embedding_model import EmbeddingComputeDescriptor
from deploy.production_model import DeployableModel
from ads_pytorch.deploy import register_meta_serializer, MODULE_NAME_SERIALIZE_KEY, VERSION_SERIALIZE_KEY

from ads_pytorch.yt.file_system_adapter_mock import CypressAdapterMock
from ads_pytorch.core.disk_adapter import DiskSavePool
from deploy.callbacks.cpp_apply_processor import CppApplyProcessorCallbackTorchYAML
from ads_pytorch.online_learning.production.uri import ProdURI
from ads_pytorch.online_learning.production.dataset import DatetimeURI
from ads_pytorch.online_learning.production.artifact.storage import AbstractArtifactStorage, NewValueCtx


class FakeStorage(AbstractArtifactStorage):
    @contextlib.asynccontextmanager
    async def new_artifact_path(self, artifact_name: str, uri, parent_tx=None):
        with tempfile.TemporaryDirectory() as tmp_dir:
            yield NewValueCtx(path=tmp_dir, tx=parent_tx)


class MyNormalizer(torch.nn.Module):
    def __init__(self, value: int):
        super(MyNormalizer, self).__init__()
        self.value = value

    def forward(self, tensor):
        return tensor + self.value


class AnyInputs(torch.nn.Module):
    def forward(self, inputs):
        return inputs


def _save_identity(model):
    return {
        MODULE_NAME_SERIALIZE_KEY: "identity",
        VERSION_SERIALIZE_KEY: 1
    }


register_meta_serializer(module_type=AnyInputs)(_save_identity)
register_meta_serializer(module_type=MyNormalizer)(_save_identity)


@pytest.fixture(params=[True, False], ids=["TzarConvolution", "Simple"])
def model_cls(request):
    if request.param:
        return GenericFeedForwardApplicableModelForTzarConvolution
    else:
        return GenericFeedForwardApplicableModel


@pytest.fixture
def model(model_cls):
    return model_cls(
        embeddings=[
            EmbeddingDescriptor(name="Text", features=["Text"], dim=30)
        ],
        normalizers={
            "Realvalue1": MyNormalizer(5),
            "Realvalue2": MyNormalizer(50)
        },
        deep_network=torch.nn.Linear(85, 13),
        out_features=13,
        external_factors={
            "Realvalue1": 5,
            "Realvalue2": 50
        }
    )


class SumModel(torch.nn.Module):
    def forward(self, tensor: torch.Tensor):
        return tensor.sum(dim=-1)


# fixme: i'm too lazy to split this megatest with megamodel to smaller ones
def test_training_forward_pass(model_cls):
    text_dim = 30
    cat_dim = 15
    model = model_cls(
        embeddings=[
            EmbeddingDescriptor(name="Text", features=["Text", "Text2"], dim=text_dim),
            EmbeddingDescriptor(
                name="Cat",
                features=[
                    EmbeddingComputeDescriptor(feature="Cat1", compute_mode="sum"),
                    EmbeddingComputeDescriptor(feature="Cat2", compute_mode="mean"),
                    "Cat3",
                    "Cat4"
                ],
                dim=cat_dim
            )
        ],
        normalizers={
            "Cat4": MyNormalizer(3),
            "rv2": MyNormalizer(101)
        },
        deep_network=SumModel(),
        out_features=1,
        external_factors={
            "rv1": 14,
            "rv2": 15
        }
    )

    text_embeds = model.embedding_model.embeddings["Text"]
    for i in range(1, 30):
        item = create_hash_table_item("adam", text_dim)
        item.w = torch.full((text_dim,), fill_value=float(i))
        text_embeds.insert_item(i, item)

    text_embeds = model.embedding_model.embeddings["Cat"]
    for i in range(1, 30):
        item = create_hash_table_item("adam", cat_dim)
        item.w = torch.full((cat_dim,), fill_value=float(i * 10))
        text_embeds.insert_item(i, item)

    assert len(model.embedding_model.embeddings) == 2

    res = model({
        "Text": [torch.LongTensor([1, 2, 3]), torch.IntTensor([2, 1])],
        "Text2": [torch.LongTensor([4, 5, 6]), torch.IntTensor([1, 2])],
        "Cat1": [torch.LongTensor([7, 8, 9]), torch.IntTensor([0, 3])],
        "Cat2": [torch.LongTensor([10, 11, 12]), torch.IntTensor([3, 0])],
        "Cat3": [torch.LongTensor([13, 14, 15]), torch.IntTensor([1, 2])],
        "Cat4": [torch.LongTensor([16, 17, 18]), torch.IntTensor([2, 1])],
        "rv1": torch.full((2, 14), fill_value=304.),
        "rv2": torch.full((2, 15), fill_value=403.)
    })

    reference = torch.zeros_like(res)
    # Text
    reference[0] = (1 + 2) / 2 * text_dim
    reference[1] = 3 * text_dim

    # Text2
    reference[0] += 4 * text_dim
    reference[1] += (5 + 6) / 2 * text_dim

    # Cat1
    reference[0] += 0 * cat_dim
    reference[1] += (70 + 80 + 90) * cat_dim

    # Cat2
    reference[0] += (100 + 110 + 120) / 3 * cat_dim
    reference[1] += 0 * cat_dim

    # Cat3
    reference[0] += 130 * cat_dim
    reference[1] += (140 + 150) / 2 * cat_dim

    # Cat4 (with normalizer + 3)
    reference[0] += ((160 + 170) / 2 + 3) * cat_dim
    reference[1] += (180 + 3) * cat_dim

    # rv1
    reference += 304 * 14

    # rv2
    reference += (403 + 101) * 15

    assert torch.allclose(res, reference)


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


def test_deployed_tzar_convolution_forward():
    torch.manual_seed(1234567)
    deep_net = torch.nn.Sequential(
        torch.nn.Linear(10, 10),
        torch.nn.ReLU(),
        torch.nn.Linear(10, 10)
    )

    model = GenericFeedForwardApplicableModelForTzarConvolution(
        embeddings=[],
        normalizers={},
        deep_network=deep_net,
        out_features=10,
        external_factors={
            "rv1": 10
        }
    )

    tensor = torch.rand(10, 10)

    to_deploy = model.get_deep_part()
    res = to_deploy([tensor])
    # Should be equal
    reference = model({"rv1": tensor})

    assert torch.allclose(res[:, 1:], reference)
    first_row = res[:, 0]
    assert torch.allclose(first_row, torch.ones_like(first_row))


def test_deployed_generic_applicable_forward():
    torch.manual_seed(12345)
    deep_net = torch.nn.Sequential(
        torch.nn.Linear(10, 10),
        torch.nn.ReLU(),
        torch.nn.Linear(10, 10)
    )

    model = GenericFeedForwardApplicableModel(
        embeddings=[],
        normalizers={},
        deep_network=deep_net,
        out_features=10,
        external_factors={
            "rv1": 10
        }
    )

    tensor = torch.rand(10, 10)

    to_deploy = model.get_deep_part()
    res = to_deploy([tensor])
    # Should be equal
    reference = model({"rv1": tensor})

    assert torch.allclose(res, reference)
