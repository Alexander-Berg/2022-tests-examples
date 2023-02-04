import torch
import pytest
from ads_pytorch.deploy import dump_model_to_eigen_format
from ads_pytorch.nn.module.clamp_layer import ClampLayer


@pytest.fixture
def to_clamp():
    return torch.FloatTensor([
        [-100, 3, 10.1],
        [10, -10, 123],
        [0.4, -.7, 9.999]
    ])


def test_default_clamp(to_clamp):
    clamper = ClampLayer()
    res = clamper(to_clamp)
    assert torch.allclose(res, to_clamp)


def test_max_clamp(to_clamp):
    clamper = ClampLayer(max_value=10)
    res = clamper(to_clamp)
    assert not torch.allclose(res, to_clamp)
    assert torch.allclose(res, torch.clamp(to_clamp, max=10))


def test_min_clamp(to_clamp):
    clamper = ClampLayer(min_value=-10)
    res = clamper(to_clamp)
    assert not torch.allclose(res, to_clamp)
    assert torch.allclose(res, torch.clamp(to_clamp, min=-10))


def test_max_min_clamp(to_clamp):
    clamper = ClampLayer(min_value=-10, max_value=10)
    res = clamper(to_clamp)
    assert not torch.allclose(res, to_clamp)
    assert torch.allclose(res, torch.clamp(to_clamp, max=10, min=-10))


@pytest.mark.parametrize(
    'kwargs',
    [
        dict(),
        dict(max_value=10),
        dict(min_value=10),
        dict(max_value=10, min_value=10)
    ],
    ids=[
        'Empty',
        'Max',
        'Min',
        'MaxMin'
    ]
)
def test_serialization(kwargs):
    clamper = ClampLayer(**kwargs)
    dump_model_to_eigen_format(clamper)
