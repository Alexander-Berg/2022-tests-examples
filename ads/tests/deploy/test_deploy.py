import pytest
import torch.nn.intrinsic
from ads_pytorch.deploy import (
    register_meta_serializer,
    register_serializer,
    dump_model_to_eigen_format,
    MODULE_NAME_SERIALIZE_KEY,
    VERSION_SERIALIZE_KEY
)
from ads_pytorch.nn.module.orthogonal_linear import OrthogonalLinearReLU, OrthogonalLinear
from ads_pytorch.nn.module.densenet import (
    DenseNet,
    WeightNormalizedEmbeddingNetwork,
    DenseNetEmbeddingNetwork
)
from ads_pytorch.nn.module.in_feature_projection import InFeatureProjection
from ads_pytorch.nn.module.float_normalizer import FloatInputsNormalizer, LogScaleNormalizer
from ads_pytorch.nn.module.clamp_layer import ClampLayer
from ads_pytorch.nn.module.cast_float32 import CastToFloat32
from ads_pytorch.nn.module.tanh_normalizer import TanhNormalizer
from ads_pytorch.deploy.crop_ideployable_model import CropNamedOutput, CropSingleMatrix, FeatureOrderHolder


class SimpleModel(torch.nn.Module):
    def __init__(self):
        super(SimpleModel, self).__init__()

        self.p1 = torch.nn.Parameter(torch.ones(10))
        self.p2 = torch.nn.Parameter(torch.full((3, 3), -1.))


class NoSerializerModule(SimpleModel):
    pass


class BadSerializedModule(SimpleModel):
    pass


def test_no_serializer():
    model = NoSerializerModule()
    with pytest.raises(ValueError):
        dump_model_to_eigen_format(model)


def test_register_serializer():
    def serialize_fn(module):
        return {
            MODULE_NAME_SERIALIZE_KEY: "__SIMPLE_TEST_MODEL__",
            VERSION_SERIALIZE_KEY: 1
        }
    register_meta_serializer(SimpleModel)(serialize_fn)
    dump_model_to_eigen_format(SimpleModel())


def test_bad_serializer():
    def serialize_fn(module):
        return {
            VERSION_SERIALIZE_KEY: 1
        }
    register_meta_serializer(BadSerializedModule)(serialize_fn)
    with pytest.raises(ValueError):
        dump_model_to_eigen_format(BadSerializedModule())


def test_duplicate_serializer():
    with pytest.raises(ValueError):
        register_serializer(torch.nn.Linear)(lambda x: {})


SIMPLE_DUMP_TEST_MODULES = [
    torch.nn.ReLU(),
    torch.nn.Linear(10, 10),
    torch.nn.LayerNorm(10),
    torch.nn.Sequential(
        torch.nn.Linear(10, 10),
        torch.nn.ReLU(),
        torch.nn.LayerNorm(10)
    ),
    DenseNet(69, 10, 5),
    WeightNormalizedEmbeddingNetwork(in_features=69, depth=10, width=10, split_depth=3),
    WeightNormalizedEmbeddingNetwork(in_features=69, depth=10, width=10, split_depth=10),
    DenseNetEmbeddingNetwork(in_features=54, depth=10, width=16, out_features=10),
    InFeatureProjection(in_feature=10, projector=torch.nn.Linear(10, 5)),
    torch.nn.intrinsic.LinearReLU(torch.nn.Linear(10, 10), torch.nn.ReLU()),
    OrthogonalLinear(30, 30),
    OrthogonalLinearReLU(30, 30),
    torch.nn.MultiheadAttention(69, 3, 0),
    torch.nn.TransformerEncoderLayer(69, 3, 69, 0, "relu"),
    torch.nn.TransformerEncoder(torch.nn.TransformerEncoderLayer(69, 3, 69, 0, "relu"), num_layers=3),
    torch.nn.ModuleList([torch.nn.Linear(10, 10), torch.nn.TransformerEncoderLayer(69, 3, 69, 0)]),
    torch.nn.ModuleDict({
        "linear": torch.nn.Linear(10, 10),
        "transformer": torch.nn.TransformerEncoderLayer(69, 3, 69, 0)
    }),
    FloatInputsNormalizer(5),
    LogScaleNormalizer(),
    ClampLayer(),
    CastToFloat32(),
    TanhNormalizer(),
    CropNamedOutput(
        name_dims={"head1": 1, "head2": 2},
        feature_order_holder=FeatureOrderHolder({}),
        replace_names={"fea1": "head1", "fea2": "head2"},
        names_order=["head1", "head2"],
        defaults={}
    ),
    CropSingleMatrix(
        out_features=10,
        feature_order_holder=FeatureOrderHolder({}),
        replace_names={"fea1": "head1", "fea2": "head2"},
        names_order=["head1", "head2"],
        defaults={}
    )
]


@pytest.mark.parametrize(
    "module",
    SIMPLE_DUMP_TEST_MODULES,
    ids=[type(x).__name__ for x in SIMPLE_DUMP_TEST_MODULES]
)
def test_usual_modules_deploy(module):
    # just check that is does not fail. The real work test occurs in large test in c++ deploy code
    # when we first call python code in torch container and then call C++ binary
    # to load model and calc some data
    dump_model_to_eigen_format(model=module)


def test_float_normalizer_deploy():
    module = FloatInputsNormalizer(5)
    torch.nn.init.normal_(module.realvalue_normalizers)
    for _ in range(3):
        module(torch.rand(100, 5))
    dump_model_to_eigen_format(model=module)


# recurrent deploy test - again, don't check values, they will be checked in large test


class Submodel(torch.nn.Module):
    def __init__(self):
        super(Submodel, self).__init__()
        self.ln1 = torch.nn.Linear(10, 10)
        self.m2 = torch.nn.TransformerEncoderLayer(69, 1, 69, dropout=0)


class Model(torch.nn.Module):
    def __init__(self):
        super(Model, self).__init__()
        self.m1 = Submodel()
        self.m2 = Submodel()
        self.nn1 = torch.nn.Sequential(
            torch.nn.Linear(10, 10),
            torch.nn.LayerNorm(10, 10),
            torch.nn.ReLU()
        )


@register_meta_serializer(Submodel)
def serialize_submodel(module):
    return {
        MODULE_NAME_SERIALIZE_KEY: "ahaha",
        VERSION_SERIALIZE_KEY: 1
    }


@register_meta_serializer(Model)
def serialize_model(module):
    return {
        MODULE_NAME_SERIALIZE_KEY: "ahaha",
        VERSION_SERIALIZE_KEY: 1
    }


def test_recurrent_deploy():
    dump_model_to_eigen_format(Model())
