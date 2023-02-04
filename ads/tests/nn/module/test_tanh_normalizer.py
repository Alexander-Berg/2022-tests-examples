import torch
from ads_pytorch.nn.module.tanh_normalizer import TanhNormalizer


def test_forward():
    torch.manual_seed(232873487)
    tensor = torch.rand(100, 20)
    model = TanhNormalizer()
    assert torch.allclose(model(tensor), torch.tanh(tensor))
