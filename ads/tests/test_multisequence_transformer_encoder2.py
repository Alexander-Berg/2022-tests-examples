import torch
import copy
import pytest
from typing import Dict, List, Optional
from densenet_tsar_query_attention_v2.multisequence_transformer_encoder2 import (
    MultiSequenceTransformerEncoderSubnetwork,
    InputSequenceBuilder,
    SumEmbeddingProjector,
    FeatureOrderHolder,
    masked_mean,
    IMultisequenceTransformerEncoderImpl,
    InputSequenceBuilderImplMixin,
    ITransformerSubnetwork,
    TorchTransformerEncoderLayerImpl,
    ConcatEmbeddingProjector
)
from ads_pytorch.model_calcer.concat_wrapper import is_wrapped_model
from ads_pytorch.model_calcer.concat_wrapper import (
    wrap_model_with_concat_wrapper,
    finalize_gradient_computation
)
from ads_pytorch.deploy.eigen_yaml import dump_model_to_eigen_format


_DEVICES = [torch.device("cpu")]
_DEVICE_IDS = ["CPU"]
if torch.cuda.is_available():
    _DEVICES.append(torch.device("cuda", 0))
    _DEVICE_IDS.append("CUDA")


@pytest.fixture(params=_DEVICES, ids=_DEVICE_IDS)
def device(request):
    return request.param


# Masked mean utility test


def test_masked_mean_all_zeros(device):
    torch.manual_seed(13257)
    tensor = torch.rand(3, 20, 5).to(device)
    mask = torch.zeros(3, 20, dtype=torch.bool).to(device)
    assert torch.allclose(masked_mean(tensor=tensor, mask=mask), torch.zeros(3, 5).to(device))


def test_masked_mean_all_ones(device):
    torch.manual_seed(13257)
    tensor = torch.rand(3, 20, 5).to(device)
    mask = torch.ones(3, 20, dtype=torch.bool).to(device)
    assert torch.allclose(masked_mean(tensor=tensor, mask=mask), tensor.mean(dim=1).to(device))


def test_masked_mean_nontrivial_mask(device):
    torch.manual_seed(13257)
    tensor = torch.rand(4, 4, 1).to(device)
    mask = torch.BoolTensor([
        [0, 1, 1, 0],
        [1, 1, 0, 0],
        [1, 1, 1, 1],
        [0, 0, 1, 0],
    ]).to(device)
    reference = torch.FloatTensor([
        (tensor[0][1] + tensor[0][2]) / 2,
        (tensor[1][0] + tensor[1][1]) / 2,
        (tensor[2][0] + tensor[2][1] + tensor[2][2] + tensor[2][3]) / 4,
        tensor[3][2]
    ]).to(device)
    assert torch.allclose(masked_mean(tensor=tensor, mask=mask).squeeze(), reference)


@pytest.fixture
def feature_holder() -> FeatureOrderHolder:
    feature_to_id = {
        **{f"e_{i}": i for i in range(10)},  # embeddings
        **{f"rv_{i}": i + 10 for i in range(10)},  # realvalue
        **{f"m_{i}": i + 20 for i in range(10)}  # masks
    }
    feature_order = list(feature_to_id.keys())
    return FeatureOrderHolder(feature_to_id=feature_to_id)


DIM = 10


def build_inputs(inputs_dict: Dict[str, torch.Tensor], holder: FeatureOrderHolder) -> List[torch.Tensor]:
    return [inputs_dict.get(f, torch.empty(1)) for f in holder.get_order()]


def manual_stacked_features(
    inputs_dct: Dict[str, torch.Tensor],
    embeddings: List[str],
    realvalue: Dict[str, int],
    normalizers: Dict[str, torch.nn.Module]
) -> torch.Tensor:
    embed_lst = [] if not len(embeddings) else [sum([inputs_dct[key] for key in embeddings])]
    rv_lst = [] if not len(realvalue) else [inputs_dct[key] for key in realvalue.keys()]
    rv_lst = [
        normalizers.get(key, torch.nn.Identity())(x)
        for key, x in zip(realvalue.keys(), rv_lst)
    ]
    to_cat = [x for x in embed_lst + rv_lst]
    return torch.cat(to_cat, dim=2)


class MockTransformerWrapper(IMultisequenceTransformerEncoderImpl):
    def __init__(self, net):
        super(MockTransformerWrapper, self).__init__()
        self.net = net

    def _forward_impl(self, src: torch.Tensor, mask: Optional[torch.Tensor] = None,
                      src_key_padding_mask: Optional[torch.Tensor] = None) -> torch.Tensor:
        return self.net(src, src_key_padding_mask)


class TransformerMock(torch.nn.Module):
    def forward(self, tensor, src_key_padding_mask):
        return tensor.clone()


class TransformerMockWithKeyMask(torch.nn.Module):
    def __init__(self, key_reference):
        super(TransformerMockWithKeyMask, self).__init__()
        self.key_reference = key_reference.clone()

    def forward(self, tensor, src_key_padding_mask):
        assert src_key_padding_mask.dtype == torch.bool
        assert torch.all(~torch.logical_xor(self.key_reference, src_key_padding_mask))
        return tensor.clone()


# Separate from code to make tests pure
class UnitTestSumEmbeddingProjector(torch.nn.Module, InputSequenceBuilderImplMixin):
    def __init__(self, embed_count: int, output_dim: int):
        super(UnitTestSumEmbeddingProjector, self).__init__()
        self.embed_count = embed_count
        self._output_dim = output_dim

    def forward(self, inputs: List[torch.Tensor]) -> torch.Tensor:
        embeddings = inputs[:self.embed_count]
        realvalue = inputs[self.embed_count:]
        return torch.cat([sum(embeddings)] + realvalue, dim=2)

    def get_embedding_dim(self) -> int:
        return self._output_dim


@pytest.fixture
def encoder_impl(device):
    # For testing purposes, we use mock which just pass inputs and averages them
    return MockTransformerWrapper(TransformerMock()).to(device)


# The first try test - just check single sequence works OK


class MulTwo(torch.nn.Module):
    def forward(self, tensor):
        return tensor * 2


def test_concat_projector(device):
    inputs = [
        torch.tensor([
            [
                [1, 2],
                [3, 4]
            ],
            [
                [5, 6],
                [7, 8],
            ]
        ], device=device),
        torch.tensor([
            [
                [101, 102, 103],
                [104, 105, 106]
            ],
            [
                [107, 108, 109],
                [110, 111, 112],
            ]
        ], device=device)
    ]
    expected = torch.tensor([
        [
            [1, 2, 101, 102, 103],
            [3, 4, 104, 105, 106]
        ],
        [
            [5, 6, 107, 108, 109],
            [7, 8, 110, 111, 112]
        ]
    ], device=device)
    projector = ConcatEmbeddingProjector(output_dim=5)
    outputs = projector(inputs)

    assert torch.allclose(outputs, expected)


@pytest.mark.parametrize("mask_name", [None, "m_0"], ids=["NoMask", "UseMask"])
@pytest.mark.parametrize("embeddings", [["e_0"], ["e_3", "e_7"]], ids=["SingleEmbed", "SeveralEmbed"])
@pytest.mark.parametrize("realvalue", [{}, {"rv_0": 2}], ids=["NoRV", "HasRV"])
@pytest.mark.parametrize("normalizers", [{}, {"rv_0": MulTwo()}], ids=["NoNorm", "RvNorm"])
def test_input_sequence_builder(device, feature_holder, embeddings, realvalue, normalizers, mask_name):
    embed_dim = 10
    seq_len = 30

    if mask_name is None:
        mask_tensor = None
        mask_reference = torch.ones(10, seq_len, dtype=torch.bool).to(device)
    else:
        mask_tensor = torch.tensor([2, 3, 4, 1, 3, 5, 7, 8, 9, 2], dtype=torch.int32).to(device)
        mask_reference = torch.BoolTensor([
            [1] * int(i) + [0] * (seq_len - int(i))
            for i in list(mask_tensor)
        ]).to(device)

    model = InputSequenceBuilder(
        embeddings=embeddings,
        realvalue=list(realvalue.keys()),
        feature_holder=feature_holder,
        normalizers=normalizers,
        projector=UnitTestSumEmbeddingProjector(
            embed_count=len(embeddings),
            output_dim=embed_dim + sum(realvalue.values())
        ),
        key_mask=mask_name,
        max_seq_len=seq_len
    ).to(device)

    inputs_dct = {
        **{key: torch.rand(10, seq_len, embed_dim) for key in embeddings},
        **{key: torch.rand(10, seq_len, value) for key, value in realvalue.items()}
    }
    if mask_name is not None:
        inputs_dct[mask_name] = mask_tensor
    inputs_dct = {k: v.to(device) for k, v in inputs_dct.items()}

    outputs, out_mask = model(build_inputs(inputs_dct, feature_holder))
    torch.allclose(mask_reference.int(), out_mask.int())

    manual_outputs = manual_stacked_features(
        inputs_dct=inputs_dct,
        embeddings=embeddings,
        realvalue=realvalue,
        normalizers=normalizers
    )

    assert torch.allclose(outputs, manual_outputs)


# *functional* 2e2 test of the model
@pytest.mark.parametrize("mask_name", [None, "m_0"], ids=["NoMask", "UseMask"])
@pytest.mark.parametrize("embed_dim", [DIM, DIM * 2])
@pytest.mark.parametrize("use_cls_token", [True, False], ids=["UseClsToken", "NoClsToken"])
def test_single_sequence_encoder(device, feature_holder, embed_dim, mask_name, use_cls_token):
    embeddings = ["e_3", "e_7"]
    realvalue = {"rv_0": 2}
    normalizers = {"rv_0": MulTwo()}

    seq_len = 30
    if mask_name is None:
        mask_tensor = None
        mask_reference = torch.ones(10, seq_len, dtype=torch.bool)
    else:
        mask_tensor = torch.tensor([2, 3, 4, 1, 3, 5, 7, 8, 9, 2], dtype=torch.int32)
        mask_reference = torch.BoolTensor([
            [1] * int(i) + [0] * (seq_len - int(i))
            for i in list(mask_tensor)
        ])
    if use_cls_token:
        mask_reference = torch.cat([
            torch.ones(10, 1, dtype=torch.bool),
            mask_reference,
        ], dim=-1)

    encoder_impl = MockTransformerWrapper(TransformerMockWithKeyMask(key_reference=mask_reference))
    sequences = [
        InputSequenceBuilder(
            embeddings=embeddings,
            realvalue=list(realvalue.keys()),
            feature_holder=feature_holder,
            normalizers=normalizers,
            projector=UnitTestSumEmbeddingProjector(
                embed_count=len(embeddings),
                output_dim=embed_dim + sum(realvalue.values())
            ),
            key_mask=mask_name,
            max_seq_len=seq_len
        )
    ]
    model = MultiSequenceTransformerEncoderSubnetwork(
        transformer_encoder_impl=encoder_impl,
        sequence_builders=sequences,
        feature_holder=feature_holder,
        transformer_embedding_dim=DIM,
        input_projections=[torch.nn.Linear(seq.get_embedding_dim(), DIM) for seq in sequences],
        use_cls_token=use_cls_token
    )

    inputs_dct = {
        **{key: torch.rand(10, seq_len, embed_dim) for key in embeddings},
        **{key: torch.rand(10, seq_len, value) for key, value in realvalue.items()}
    }
    if mask_name is not None:
        inputs_dct[mask_name] = mask_tensor

    outputs = model(build_inputs(inputs_dct, feature_holder))
    assert outputs.size() == (10, DIM)

    manual_outputs = manual_stacked_features(
        inputs_dct=inputs_dct,
        embeddings=embeddings,
        realvalue=realvalue,
        normalizers=normalizers
    )
    manual_outputs = model.input_projections[0](manual_outputs)
    if use_cls_token:
        manual_outputs = torch.cat([
            torch.ones(manual_outputs.size()[0], 1, DIM) * model.cls_token,
            manual_outputs
        ], dim=1)
    manual_outputs = masked_mean(tensor=manual_outputs, mask=mask_reference)

    assert torch.allclose(outputs, manual_outputs)


#############################################################
#                  Input Sequence Builder Tests             #
#############################################################

# The most common seq builder!
# TODO:
# 1. Unit-test C++ implementation
# 2. Test hypothesis about combining realtime profile and action history


# #####################################################################
# #                      MULTIPLE SEQUENCES TEST                      #
# #####################################################################


def test_multiple_sequences(device, encoder_impl, feature_holder):
    embed_dim = 30
    realvalue_dims = {
        "rv_0": 5,
        "rv_4": 4,
        "rv_8": 10
    }
    normalizers = {
        "rv_0": MulTwo(),
        "rv_8": MulTwo(),
        "rv_2": MulTwo()  # not used, check it's not in model
    }
    two_sequences = [
        InputSequenceBuilder(
            embeddings=["e_3", "e_7"],
            realvalue=["rv_0", "rv_4"],
            feature_holder=feature_holder,
            normalizers=normalizers,
            projector=UnitTestSumEmbeddingProjector(
                embed_count=2,
                output_dim=39
            )
        ),
        InputSequenceBuilder(
            embeddings=["e_3", "e_9"],
            realvalue=["rv_0", "rv_8"],
            feature_holder=feature_holder,
            normalizers=normalizers,
            projector=UnitTestSumEmbeddingProjector(
                embed_count=2,
                output_dim=45
            )
        )
    ]
    model = MultiSequenceTransformerEncoderSubnetwork(
        transformer_encoder_impl=encoder_impl,
        sequence_builders=two_sequences,
        feature_holder=feature_holder,
        transformer_embedding_dim=DIM,
        input_projections=[torch.nn.Linear(seq.get_embedding_dim(), DIM) for seq in two_sequences]
    ).to(device)

    inputs_dct = {
        **{key: torch.rand(10, 30, embed_dim) for key in ["e_3", "e_7", "e_9"]},
        **{key: torch.rand(10, 30, value) for key, value in realvalue_dims.items()}
    }
    inputs_dct = {k: v.to(device) for k, v in inputs_dct.items()}

    outputs = model(build_inputs(inputs_dct, feature_holder))
    manual_outputs = [
        manual_stacked_features(
            inputs_dct=inputs_dct,
            embeddings=seq._embeddings,
            realvalue={k: realvalue_dims[k] for k in seq._realvalue},
            normalizers=normalizers
        )
        for seq in two_sequences
    ]
    manual_outputs = [model.input_projections[i](x) for i, x in enumerate(manual_outputs)]
    manual_result = torch.cat(manual_outputs, dim=1).mean(dim=1)
    assert torch.allclose(outputs, manual_result)
    assert outputs.size() == (10, DIM)


###############################################################
#                        EMPTY TEST                           #
###############################################################


class MulParam(torch.nn.Module):
    def __init__(self):
        super(MulParam, self).__init__()
        self.p = torch.nn.Parameter(torch.ones(1) * 2)

    def forward(self, tensor):
        return self.p * tensor


class MulParamWrapper(IMultisequenceTransformerEncoderImpl):
    def __init__(self, net):
        super(MulParamWrapper, self).__init__()
        self.net = net

    def _forward_impl(self, src: torch.Tensor, mask: Optional[torch.Tensor] = None,
                      src_key_padding_mask: Optional[torch.Tensor] = None) -> torch.Tensor:
        return self.net(src)


def _make_empty_seq_reference(encoder_impl, cls_token):
    device = next(encoder_impl.parameters()).device
    with torch.no_grad():
        if cls_token is not None:
            # In case of cls token, we must use it even on empty sequences
            return encoder_impl(torch.ones(10, 1, DIM, device=device) * cls_token.detach())
        else:
            return torch.zeros(10, 1, DIM, device=device)


def _check_gradients_empty_seq(use_cls_token, model, sequences):
    if is_wrapped_model(model):
        model = model.net
    if not use_cls_token:
        for p in model.parameters():
            assert p.grad is None
    else:
        assert model.cls_token.grad is not None
        for p in model.transformer_encoder_impl.parameters():
            assert p.grad is not None
        for seq in sequences:
            for p in seq.parameters():
                assert p.grad is None


@pytest.mark.parametrize("use_cls_token", [True, False], ids=["UseClsToken", "NoClsToken"])
def test_single_empty_sequence(device, feature_holder, use_cls_token):
    encoder_impl = MulParamWrapper(MulParam())
    sequences = [
        InputSequenceBuilder(
            embeddings=["e_0"],
            realvalue=["rv_0"],
            feature_holder=feature_holder,
            normalizers={"rv_0": MulParam()},
            projector=UnitTestSumEmbeddingProjector(
                embed_count=1,
                output_dim=12
            )
        )
    ]

    model = MultiSequenceTransformerEncoderSubnetwork(
        transformer_encoder_impl=encoder_impl,
        feature_holder=feature_holder,
        sequence_builders=sequences,
        transformer_embedding_dim=DIM,
        input_projections=[torch.nn.Identity() for _ in range(len(sequences))],
        use_cls_token=use_cls_token
    ).to(device)
    model = wrap_model_with_concat_wrapper(model)
    model_state_dict = model.state_dict()
    optimizer = torch.optim.Adam(model.buffer_parameters())

    inputs_dct = {
        "e_0": torch.rand(10, 0, 10).to(device),
        "rv_0": torch.rand(10, 0, 2).to(device)
    }

    reference = _make_empty_seq_reference(encoder_impl=encoder_impl, cls_token=model.net.cls_token)

    # in case of empty sequence, we throw unrequired grad tensor
    p = torch.nn.Parameter(torch.ones(1)).to(device)
    outputs = model(build_inputs(inputs_dct, feature_holder))
    assert torch.allclose(outputs, reference)
    (outputs * p).sum().backward()
    finalize_gradient_computation(model)
    optimizer.step()

    new_state_dict = model.state_dict()
    for key in new_state_dict.keys():
        if isinstance(new_state_dict[key], torch.Tensor):
            assert torch.allclose(new_state_dict[key], model_state_dict[key])
        else:
            assert new_state_dict[key] == model_state_dict[key]

    _check_gradients_empty_seq(use_cls_token=use_cls_token, model=model, sequences=sequences)


@pytest.mark.parametrize("use_cls_token", [True, False], ids=["UseClsToken", "NoClsToken"])
def test_two_empty_sequences(device, feature_holder, use_cls_token):
    encoder_impl = MulParamWrapper(MulParam())
    two_sequences = [
        InputSequenceBuilder(
            embeddings=["e_0"],
            realvalue=["rv_0"],
            feature_holder=feature_holder,
            normalizers={"rv_0": MulParam()},
            projector=UnitTestSumEmbeddingProjector(
                embed_count=1,
                output_dim=12
            )
        ),
        InputSequenceBuilder(
            embeddings=["e_1"],
            realvalue=["rv_1"],
            feature_holder=feature_holder,
            normalizers={"rv_1": MulParam()},
            projector=UnitTestSumEmbeddingProjector(
                embed_count=1,
                output_dim=14
            )
        )
    ]

    model = MultiSequenceTransformerEncoderSubnetwork(
        transformer_encoder_impl=encoder_impl,
        feature_holder=feature_holder,
        sequence_builders=two_sequences,
        transformer_embedding_dim=DIM,
        input_projections=[torch.nn.Identity() for _ in range(len(two_sequences))],
        use_cls_token=use_cls_token
    ).to(device)
    model = wrap_model_with_concat_wrapper(model)
    model_state_dict = model.state_dict()
    optimizer = torch.optim.Adam(model.buffer_parameters())

    inputs_dct = {
        "e_0": torch.rand(10, 0, 10).to(device),
        "rv_0": torch.rand(10, 0, 2).to(device),
        "e_1": torch.rand(10, 0, 10).to(device),
        "rv_1": torch.rand(10, 0, 4).to(device),
    }

    reference = _make_empty_seq_reference(encoder_impl=encoder_impl, cls_token=model.net.cls_token)

    # in case of empty sequence, we throw unrequired grad tensor
    p = torch.nn.Parameter(torch.ones(1)).to(device)
    outputs = model(build_inputs(inputs_dct, feature_holder))
    assert torch.allclose(outputs, reference)
    (outputs * p).sum().backward()
    finalize_gradient_computation(model)
    optimizer.step()

    new_state_dict = model.state_dict()
    for key in new_state_dict.keys():
        if isinstance(new_state_dict[key], torch.Tensor):
            assert torch.allclose(new_state_dict[key], model_state_dict[key])
        else:
            assert new_state_dict[key] == model_state_dict[key]

    _check_gradients_empty_seq(use_cls_token=use_cls_token, model=model, sequences=two_sequences)


def test_two_sequences_one_empty(device, feature_holder):
    encoder_impl = MulParamWrapper(MulParam())
    two_sequences = [
        InputSequenceBuilder(
            embeddings=["e_0"],
            realvalue=["rv_0"],
            feature_holder=feature_holder,
            normalizers={"rv_0": MulParam()},
            projector=UnitTestSumEmbeddingProjector(
                embed_count=1,
                output_dim=12
            )
        ),
        InputSequenceBuilder(
            embeddings=["e_1"],
            realvalue=["rv_1"],
            feature_holder=feature_holder,
            normalizers={"rv_1": MulParam()},
            projector=UnitTestSumEmbeddingProjector(
                embed_count=1,
                output_dim=14
            )
        )
    ]

    model = MultiSequenceTransformerEncoderSubnetwork(
        transformer_encoder_impl=encoder_impl,
        feature_holder=feature_holder,
        sequence_builders=two_sequences,
        transformer_embedding_dim=DIM,
        input_projections=[torch.nn.Identity() for _ in range(len(two_sequences))]
    ).to(device)
    model2: MultiSequenceTransformerEncoderSubnetwork = copy.deepcopy(model)
    model = wrap_model_with_concat_wrapper(model)
    optimizer = torch.optim.Adam(model.buffer_parameters())

    inputs_dct = {
        "e_0": torch.rand(10, 0, 10).to(device),
        "rv_0": torch.rand(10, 0, 2).to(device),
        "e_1": torch.rand(10, 5, 10).to(device),
        "rv_1": torch.rand(10, 5, 4).to(device),
    }

    # in case of empty sequence, we throw unrequired grad tensor
    p = torch.nn.Parameter(torch.ones(1)).to(device)
    outputs = model(build_inputs(inputs_dct, feature_holder))
    (outputs * p).sum().backward()
    finalize_gradient_computation(model)
    optimizer.step()

    model: MultiSequenceTransformerEncoderSubnetwork = model.net
    for p1, p2 in zip(
        model.sequence_builders[0].parameters(),
        model2.sequence_builders[0].parameters()
    ):
        assert torch.allclose(p1, p2)

    for p1, p2 in zip(
        model.sequence_builders[1].parameters(),
        model2.sequence_builders[1].parameters()
    ):
        assert not torch.allclose(p1, p2)

    for p1, p2 in zip(model.transformer_encoder_impl.parameters(), model2.transformer_encoder_impl.parameters()):
        assert not torch.allclose(p1, p2)


@pytest.mark.parametrize("use_cls_token", [True, False], ids=["UseClsToken", "NoClsToken"])
def test_serializability(use_cls_token):
    encoder_impl = TorchTransformerEncoderLayerImpl(
        encoder_impl=torch.nn.TransformerEncoder(
            encoder_layer=torch.nn.TransformerEncoderLayer(
                d_model=10,
                nhead=1,
                dim_feedforward=10,
                dropout=0
            ),
            num_layers=2
        )
    )

    two_sequences = [
        InputSequenceBuilder(
            embeddings=["e_0"],
            realvalue=["rv_0"],
            feature_holder=feature_holder,
            normalizers={},
            projector=SumEmbeddingProjector(
                embed_count=1,
                output_dim=12
            )
        ),
        InputSequenceBuilder(
            embeddings=["e_1"],
            realvalue=["rv_1"],
            feature_holder=feature_holder,
            normalizers={},
            projector=SumEmbeddingProjector(
                embed_count=1,
                output_dim=14
            )
        )
    ]

    model = MultiSequenceTransformerEncoderSubnetwork(
        transformer_encoder_impl=encoder_impl,
        feature_holder=feature_holder,
        sequence_builders=two_sequences,
        transformer_embedding_dim=DIM,
        input_projections=[torch.nn.Identity() for _ in range(len(two_sequences))],
        use_cls_token=use_cls_token
    )

    dump_model_to_eigen_format(model)
