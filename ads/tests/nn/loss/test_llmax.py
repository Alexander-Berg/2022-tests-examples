from functools import wraps
import torch
from typing import Optional
import pytest
from ads_pytorch.nn.loss.llmax import llmax, query_cross_entropy, LLmaxComputeResult


device_params = ["CPU"]
device_params.append(pytest.param("CUDA", marks=pytest.mark.requires_cuda))


@pytest.fixture(params=device_params)
def device(request):
    if request.param == "CPU":
        return torch.device("cpu")
    elif request.param == "CUDA":
        return torch.device("cuda", 0)
    else:
        raise ValueError("Unknown device type :(")


@pytest.fixture(params=[
    lambda _: None,
    lambda x: torch.ones_like(x, requires_grad=False),
    lambda x: torch.rand_like(x, requires_grad=False)
])
def weights_func(request):
    return request.param


@pytest.fixture(scope="function", autouse=True)
def manual_seed():
    torch.manual_seed(12345)


def llmax_with_check(
        predictions: torch.Tensor,
        targets: torch.Tensor,
        shards: torch.Tensor,
        reduction: str = "mean",
        weights: Optional[torch.Tensor] = None,
):
    res = llmax(predictions, targets, shards, reduction, weights)
    if weights is None:
        assert torch.allclose(
            llmax(predictions, targets, shards, reduction, weights=torch.ones_like(predictions)).loss,
            llmax(predictions, targets, shards, reduction).loss
        )
    return res


def test_all_zeros(device, weights_func):
    predictions = torch.rand(3).to(device)
    targets = torch.FloatTensor([0, 0, 0]).to(device)
    shards = torch.LongTensor([1, 1, 1]).to(device)
    weights = weights_func(targets)
    llmax_res = llmax_with_check(predictions=predictions, targets=targets, shards=shards, weights=weights)
    assert round(float(llmax_res.loss), 6) == 0
    assert torch.all(~llmax_res.select_mask)


def test_all_ones(device, weights_func):
    predictions = torch.rand(3).to(device)
    targets = torch.FloatTensor([1, 1, 1]).to(device)
    shards = torch.LongTensor([1, 1, 1]).to(device)
    weights = weights_func(targets)
    llmax_res = llmax_with_check(predictions=predictions, targets=targets, shards=shards, weights=weights)
    assert round(float(llmax_res.loss), 6) == 0
    assert torch.all(~llmax_res.select_mask)


# manually train optimal bias
class BiasModel(torch.nn.Module):
    def __init__(self):
        super(BiasModel, self).__init__()
        self.bias = torch.nn.Parameter(torch.zeros(1))

    def forward(self, tensor):
        return tensor + self.bias


def compute_llmax_manual(predictions: torch.Tensor, targets: torch.Tensor, weights: Optional[torch.Tensor] = None, reduction: str = 'mean'):
    device = predictions.device
    model = BiasModel().to(device)
    optim = torch.optim.LBFGS(model.parameters(), lr=1)
    if weights is None:
        weights = torch.ones_like(targets, requires_grad=False)

    def closure():
        model.zero_grad()
        loss = torch.nn.functional.binary_cross_entropy_with_logits(
            input=model(predictions),
            target=targets,
            reduction=reduction,
            weight=weights
        )
        loss.backward()
        return loss

    losses = []
    for _ in range(50):
        loss = optim.step(closure)
        losses.append(float(loss))

    return torch.nn.functional.binary_cross_entropy_with_logits(
        input=model(predictions),
        target=targets,
        reduction=reduction,
        weight=weights
    )


@pytest.mark.parametrize(
    ['predictions', 'targets'],
    [
        (
            torch.FloatTensor([-0.2, -0.1, 0, 0.1]),
            torch.FloatTensor([0, 0, 1, 0]),
        ),
        # these are "hard" examples where naive newton like in most llmax implementations
        # fails and we need backtracking
        (
            torch.FloatTensor([1.6924, 1.8731, 2.0396, 1.9805, 1.8536, 1.5711]),
            torch.FloatTensor([0, 0, 0, 1, 0, 0]),
        )
    ],
    ids=['Simple', 'Hard']
)
def test_nontrivial_llmax_single_shard(device, predictions, targets, weights_func):
    predictions = predictions.to(device)
    targets = targets.to(device)
    shards = torch.ones_like(predictions, dtype=torch.int64)
    weights = weights_func(targets)
    output = llmax_with_check(predictions=predictions, targets=targets, shards=shards, weights=weights).loss
    output_bce = torch.nn.functional.binary_cross_entropy_with_logits(input=predictions, target=targets, weight=weights)
    assert float(output_bce) > float(output)

    output_manual = compute_llmax_manual(
        predictions=predictions,
        targets=targets,
        weights=weights
    )

    assert torch.allclose(output, output_manual)


def test_llmax_many_shards(device, weights_func):
    predictions = torch.FloatTensor([-0.2, -0.1, 0, 0.1, -3, 4, -5, 6, 0.7, -0.1, 0.1]).to(device)
    predictions.requires_grad = False
    targets = torch.FloatTensor([0, 0, 1, 0, 1, 0, 0, 0, 1, 0, 1]).to(device)
    targets.requires_grad = False
    shards = torch.LongTensor([1, 1, 1, 1, -2, 3, 3, 45, 500, 500, 500]).to(device)
    shards.requires_grad = False
    weights = weights_func(targets)

    # Okay, what we expect:
    # 1-shard and 500-shards are non-trivial shards with non-zero losses
    # all others have all equal labels (including 1-length shards) and should have zero losses

    # use sum reduction for simplicity
    llmax_res = llmax_with_check(predictions=predictions, targets=targets, shards=shards, weights=weights, reduction="sum")
    assert torch.all(~(llmax_res.select_mask ^ torch.BoolTensor([1, 1, 1, 1, 0, 0, 0, 0, 1, 1, 1]).to(device)))

    first_manual = compute_llmax_manual(
        predictions=predictions.narrow(0, 0, 4),
        targets=targets.narrow(0, 0, 4),
        weights=weights.narrow(0, 0, 4) if weights is not None else None,
        reduction="sum"
    )

    last_manual = compute_llmax_manual(
        predictions=predictions.narrow(0, 8, 3),
        targets=targets.narrow(0, 8, 3),
        weights=weights.narrow(0, 8, 3) if weights is not None else None,
        reduction="sum"
    )

    assert round(float(llmax_res.loss), 5) == round(float(first_manual) + float(last_manual), 5)


#################################################
#               CORNER CASES TESTS              #
#################################################


@pytest.mark.parametrize(
    'float_dtype',
    [
        pytest.param(torch.float16, marks=pytest.mark.requires_cuda),
        torch.float32,
        torch.float64
    ],
    ids=['Float16', 'Float32', 'Float64']
)
@pytest.mark.parametrize(
    'int_dtype',
    [torch.int8, torch.uint8, torch.int16, torch.int32, torch.int64],
    ids=['Int8', 'UInt8', 'Int16', 'Int32', 'Int64']
)
def test_different_dtypes(device, float_dtype, int_dtype, weights_func):
    predictions = torch.FloatTensor([-0.2, -0.1, 0, 0.1]).to(device).to(float_dtype)
    targets = torch.FloatTensor([0, 0, 1, 0]).to(device).to(float_dtype)
    shards = torch.LongTensor([1, 1, 1, 1]).to(device).to(int_dtype)
    weights = weights_func(targets)
    output = llmax_with_check(predictions=predictions, targets=targets, shards=shards, weights=weights).loss
    output_manual = compute_llmax_manual(
        predictions=predictions.to(torch.float32),
        targets=targets.to(torch.float32),
        weights=weights.to(torch.float32) if weights is not None else None
    )
    # nearly infinite
    round_count = 15
    if float_dtype == torch.float16:
        # precision losses are significant :(
        round_count = 3
    assert round(float(output.cpu().float())) == round(float(output_manual.cpu().float()))


@pytest.mark.parametrize('target_value', [0, 1])
def test_zero_size_tensor(target_value, device, weights_func):
    output = llmax_with_check(
        predictions=torch.tensor(-3.1, dtype=torch.float32).to(device),
        targets=torch.tensor(target_value, dtype=torch.float32).to(device),
        shards=torch.tensor(0, dtype=torch.int64).to(device),
        weights=weights_func(torch.tensor(0, dtype=torch.float32).to(device))
    ).loss
    assert float(output) == 0


def test_non_contiguous_group(weights_func):
    with pytest.raises(RuntimeError):
        llmax_with_check(
            predictions=torch.rand(5),
            targets=torch.FloatTensor([0, 1, 0, 0, 0]),
            shards=torch.LongTensor([1, 2, 1, 3, 3]),
            weights=weights_func(torch.empty(5))
        )


#################################################
#               QUERY CROSS ENTROPY             #
#################################################


def test_query_cross_entropy_all_zeros(device, weights_func):
    predictions = torch.rand(3).to(device)
    targets = torch.FloatTensor([0, 0, 0]).to(device)
    shards = torch.LongTensor([1, 1, 1]).to(device)
    weights = weights_func(targets)
    output = query_cross_entropy(predictions=predictions, targets=targets, shards=shards, coef=0.5, weights=weights)
    assert torch.allclose(output, .5 * torch.nn.functional.binary_cross_entropy_with_logits(predictions, targets, weight=weights))


def test_query_cross_entropy_all_ones(device, weights_func):
    predictions = torch.rand(3).to(device)
    targets = torch.FloatTensor([1, 1, 1]).to(device)
    shards = torch.LongTensor([1, 1, 1]).to(device)
    weights = weights_func(targets)
    output = query_cross_entropy(predictions=predictions, targets=targets, shards=shards, coef=0.5, weights=weights)
    assert torch.allclose(output, .5 * torch.nn.functional.binary_cross_entropy_with_logits(predictions, targets, weight=weights))


def test_query_cross_entropy_single_shard(device, weights_func):
    predictions = torch.FloatTensor([1.6924, 1.8731, 2.0396, 1.9805, 1.8536, 1.5711]).to(device)
    targets = torch.FloatTensor([0, 0, 0, 1, 0, 0]).to(device)
    shards = torch.LongTensor([1, 1, 1, 1, 1, 1]).to(device)
    weights = weights_func(targets)
    output = query_cross_entropy(predictions=predictions, targets=targets, shards=shards, coef=0.5, weights=weights)
    llmax_res = llmax_with_check(predictions=predictions, targets=targets, shards=shards, weights=weights)
    logloss_out = torch.nn.functional.binary_cross_entropy_with_logits(predictions, targets, weight=weights)
    assert torch.allclose(output, (llmax_res.loss + logloss_out) / 2)


def test_query_cross_entropy_many_shard(device, weights_func):
    predictions = torch.FloatTensor([1.6924, 1.8731, 2.0396, 1.9805, 1.8536, 1.5711]).to(device)
    targets = torch.FloatTensor([0, 1, 0, 1, 0, 1]).to(device)
    shards = torch.LongTensor([1, 1, 2, 3, 4, 4]).to(device)
    weights = weights_func(targets)

    output = query_cross_entropy(predictions=predictions, targets=targets, shards=shards, coef=0.5, weights=weights)
    llmax_res = llmax_with_check(predictions=predictions, targets=targets, shards=shards, weights=weights)
    logloss = torch.nn.functional.binary_cross_entropy_with_logits(predictions, targets, reduction="mean", weight=weights)
    assert torch.allclose(output, logloss * .5 + llmax_res.loss * .5)
