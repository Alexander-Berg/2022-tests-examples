import torch
import torch.nn.intrinsic as nni
from densenet_tsar_query_attention.generic_feed_forward_with_normalizers import (
    GenericFeedForwardWithPerFeatureNormalizers,
)
from ads_pytorch.deploy import dump_model_to_eigen_format
from ads_pytorch.nn.module.float_normalizer import FloatInputsNormalizer


def test_deployable():
    model = GenericFeedForwardWithPerFeatureNormalizers(
        deep=torch.nn.Sequential(
            nni.LinearReLU(torch.nn.Linear(10, 10), torch.nn.ReLU()),
            nni.LinearReLU(torch.nn.Linear(10, 10), torch.nn.ReLU()),
            torch.nn.Linear(10, 5)
        ),
        feature_to_id={f"factor{i}": i for i in range(4)},
        normalizers={
            "factor0": torch.nn.Identity(),
            "factor2": FloatInputsNormalizer(3)
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


def test_output():
    torch.manual_seed(12345)
    model = GenericFeedForwardWithPerFeatureNormalizers(
        deep=AnyInputs(),
        deep_features=["factor0", "factor5", "factor7", "factor9"],
        feature_to_id={f"factor{i}": i for i in range(10)},
        normalizers={
            "factor0": MyNormalizer(10),
            "factor7": MyNormalizer(100)
        }
    )

    inputs = [torch.rand(5, 1) for _ in range(10)]

    res = model(inputs)
    references = [
        inputs[0] + 10,
        inputs[5],
        inputs[7] + 100,
        inputs[9],
    ]
    for i, ref in enumerate(references):
        assert torch.allclose(res[:, i].squeeze(), ref.squeeze())
