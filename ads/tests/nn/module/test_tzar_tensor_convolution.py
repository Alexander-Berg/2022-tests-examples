import torch
import torch.nn
import torch.nn.intrinsic as nni
import pytest
from ads_pytorch.nn.module.tzar_tensor_convolution import (
    TzarTopLevelModel,
    make_embedding_converter_for_tzar_tensor,
    add_embedding_converter_to_model
)
from ads_pytorch.deploy import dump_model_to_eigen_format


@pytest.mark.parametrize(
    'kwargs',
    [
        dict(),
        dict(alpha=.1),
        dict(bias=3.),
        dict(alpha=.1, bias=3.)
    ],
    ids=[
        "Default",
        "Alpha",
        "Bias",
        "AlphaAndBias"
    ]
)
def test_embedding_converter(kwargs):
    obj_count = 10
    dim = 3
    alpha = kwargs.get("alpha", 1.)
    bias = kwargs.get("bias", 1.)

    tensor = torch.rand(obj_count, dim)
    reference = torch.cat([torch.full((obj_count, 1), fill_value=bias), tensor * alpha], dim=1)

    one_convert_model = make_embedding_converter_for_tzar_tensor(dim=dim, **kwargs)
    res = one_convert_model(tensor)
    assert torch.allclose(res, reference)


@pytest.mark.parametrize("use_previous_model", [True, False])
def test_tzar_top_level_model(use_previous_model):
    # end-to-end test with convertation to tensor and make_embedding_converter
    torch.manual_seed(13452)
    reference_model = TzarTopLevelModel()
    for p in reference_model.parameters():
        torch.nn.init.normal_(p)

    obj_count = 10
    dim = 3

    banner = torch.rand(obj_count, dim)
    page = torch.rand(obj_count, dim)
    user = torch.rand(obj_count, dim)

    reference = reference_model(banner, user, page)
    tensor = reference_model.to_tzar_tensor(dim=dim)

    if use_previous_model:
        one_convert_model = add_embedding_converter_to_model(
            model=torch.nn.Identity(),
            dim=dim
        )
    else:
        one_convert_model = make_embedding_converter_for_tzar_tensor(dim=dim)

    banner = one_convert_model(banner)
    page = one_convert_model(page)
    user = one_convert_model(user)

    # consistent with https://a.yandex-team.ru/arc/trunk/arcadia/yabs/server/test/ft/BSSERVER-2189/test_tsar_result.py?rev=6497672#L356
    res = torch.einsum("ai,aj,ak,ijk->a", banner, user, page, tensor)

    assert torch.allclose(res, reference)


@pytest.mark.parametrize(
    'kwargs',
    [
        dict(),
        dict(alpha=.1),
        dict(bias=3.),
        dict(alpha=.1, bias=3.)
    ],
    ids=[
        "Default",
        "Alpha",
        "Bias",
        "AlphaAndBias"
    ]
)
def test_deployability_of_converted_model(kwargs):
    dim = 10
    model = nni.LinearReLU(
        torch.nn.Linear(100, dim),
        torch.nn.ReLU()
    )
    model = add_embedding_converter_to_model(model=model, dim=dim, **kwargs)
    dump_model_to_eigen_format(model=model)
