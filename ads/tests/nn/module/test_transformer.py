from ads_pytorch.nn.module.transformer import (
    Attention,
    LinearAttention,
    SoftmaxAttention,
    ReZeroTransformerEncoderLayer,
    ReZeroTransformerEncoder,
    DenseTransformerEncoderLayer,
    DenseTransformerEncoder,
    SymmetricLinearSelfAttention,
    SymmetricSelfAttention
)
from ads_pytorch.deploy.eigen_yaml import dump_model_to_eigen_format
import itertools
import torch
import pytest


_DEVICE_PARAMS = ["cpu"]
_DEVICE_PARAMS.append(pytest.param("cuda", marks=pytest.mark.requires_cuda))


@pytest.fixture(params=_DEVICE_PARAMS)
def device(request):
    return torch.device(request.param, 0)


###########################################################
#                     ReZero Attention                    #
###########################################################


def test_rezero_flag_attention(device):
    rezero = Attention(
        attention=LinearAttention(),
        d_model=10,
        n_heads=1,
        use_rezero=True
    ).to(device)
    usual = Attention(
        attention=LinearAttention(),
        d_model=10,
        n_heads=1,
        use_rezero=False
    ).to(device)
    for p in itertools.chain(rezero.parameters(), usual.parameters()):
        torch.nn.init.constant_(p, 0.1)

    x = torch.rand(3, 4, 10).to(device)
    rezero_res = rezero(x, x, x)
    usual_res = usual(x, x, x) * 0.1

    assert torch.allclose(rezero_res, usual_res)


@pytest.mark.parametrize("n_heads", [1, 3])
def test_num_heads(n_heads, device):
    model = Attention(
        attention=LinearAttention(),
        d_model=10,
        n_heads=n_heads
    ).to(device)

    x = torch.rand(3, 4, 10).to(device)
    model(x, x, x).sum().backward()


###########################################################
#                    Transformer Encoder                  #
###########################################################

# tests all attentions have fine interface


@pytest.mark.parametrize(
    "attention_impl",
    [
        SoftmaxAttention(),
        LinearAttention()
    ],
    ids=["Softmax", "Linear"]
)
def test_transformer_encoder(attention_impl):
    d_model = 10
    attention = Attention(attention=attention_impl, d_model=d_model, n_heads=1)
    encoder_layer = ReZeroTransformerEncoderLayer(
        attention=attention,
        d_model=d_model,
        d_feedforward=d_model,
        activation=torch.nn.ReLU()
    )
    encoder = ReZeroTransformerEncoder(encoder_layer=encoder_layer, num_layers=3)
    x = torch.rand(3, 4, 10)
    encoder(x).sum().backward()


###########################################################
#                     Masked Attention                    #
###########################################################


@pytest.mark.parametrize(
    "attention_impl",
    [
        SoftmaxAttention(),
        LinearAttention()
    ],
    ids=["Softmax", "Linear"]
)
@pytest.mark.parametrize("n_heads", [1, 3], ids=["SingleHead", "MultiHead"])
def test_src_key_mask(attention_impl, n_heads, device):
    torch.manual_seed(3287826387)
    src_key_mask = torch.BoolTensor([
        [1, 0, 0, 1],
        [0, 0, 1, 1],
        [1, 1, 1, 1]
    ]).to(device)

    d_model = 9
    x = torch.rand(3, 4, d_model).to(device)
    model = Attention(attention=attention_impl, d_model=d_model, n_heads=n_heads).to(device)

    # masked
    result = model(x, x, x, key_padding_mask=src_key_mask)

    # unmasked
    unmasked_inputs = [
        torch.cat([x[0, 0, :][None, None, :], x[0, 3, :][None, None, :]], dim=1),
        torch.cat([x[1, 2, :][None, None, :], x[1, 3, :][None, None, :]], dim=1),
        x[2, :, :].unsqueeze(0)
    ]
    unmasked_results = [model(cur_x, cur_x, cur_x) for cur_x in unmasked_inputs]

    reference0 = torch.cat([result[0, 0, :][None, None, :], result[0, 3, :][None, None, :]], dim=1)
    reference1 = torch.cat([result[1, 2, :][None, None, :], result[1, 3, :][None, None, :]], dim=1)
    reference2 = result[2, :, :].unsqueeze(0)

    res0, res1, res2 = unmasked_results
    assert torch.allclose(res0, reference0)
    assert torch.allclose(res1, reference1)
    assert torch.allclose(res2, reference2)


# Important contract for all models: if we have masked out everything
# we obtain zeros on output
@pytest.mark.parametrize(
    "attention_impl",
    [
        SoftmaxAttention(),
        LinearAttention()
    ],
    ids=["Softmax", "Linear"]
)
@pytest.mark.parametrize("n_heads", [1, 3], ids=["SingleHead", "MultiHead"])
def test_src_key_mask_empty(attention_impl, n_heads, device):
    torch.manual_seed(3287826387)
    src_key_mask = torch.zeros(3, 4, dtype=torch.bool).to(device)

    d_model = 9
    x = torch.rand(3, 4, d_model).to(device)
    model = Attention(attention=attention_impl, d_model=d_model, n_heads=n_heads).to(device)
    # Remove biases to remove randomness in computations
    for m in model.modules():
        if hasattr(m, "bias"):
            torch.nn.init.zeros_(m.bias)

    result = model(x, x, x, key_padding_mask=src_key_mask)
    assert torch.allclose(result, torch.zeros_like(x))


def test_dump_to_eigen(device):
    model = ReZeroTransformerEncoder(
        encoder_layer=ReZeroTransformerEncoderLayer(
            attention=Attention(
                attention=LinearAttention(),
                d_model=4,
                n_heads=1
            ),
            d_model=4,
            d_feedforward=4,
            activation=torch.nn.ReLU()
        ),
        num_layers=3
    ).to(device)

    dump_model_to_eigen_format(model)


####################################################################
#                           Dense Transformer                      #
####################################################################


@pytest.mark.parametrize("fast", [True, False], ids=["FastTrue", "FastFalse"])
def test_dense_transformer_forward_backward(fast, device):
    d_model = 16
    dense_encoder = DenseTransformerEncoder(
        attention=SymmetricSelfAttention(
            inner_attention=SymmetricLinearSelfAttention(),
            d_model=d_model,
            n_heads=1
        ),
        in_features=32,
        out_features=32,
        num_layers=7,
        d_model=d_model,
        fast=fast
    ).to(device)

    tensor = torch.rand(10, 50, 32).to(device)
    dense_encoder(tensor).sum().backward()
