import pytest
import torch
import torch.nn.functional
import asyncio
import dataclasses
import random
from typing import List
from ads_pytorch.core.psmodel import BaseParameterServerModule
from ads_pytorch.model_calcer.concat_wrapper import (
    _set_mark,
    mark_parameter,
    mark_module,
    get_mark,
    validate_optimizers,
    wrap_model_with_concat_wrapper,
    is_wrapped_model,
    DEFAULT_MARK,
    finalize_gradient_computation,
    _CONCAT_WRAPPER_PARAMETERS_ORDER_KEY,
    mark_unconcatable,
    is_unconcatable
)
from ads_pytorch.model_calcer.factory import MinibatchPoolFactory
from ads_pytorch.model_calcer.minibatch_record import MinibatchRecord
from ads_pytorch.model_calcer.minibatch_worker import CalcerResults

from ads_pytorch.nn.module.universal_normalizer.universal_normalizer import (
    QuantizedNormalizerParameter,
    is_universal_normalizer_parameter,
    QuantizedNormalizerParameterServerOptimizer,
    _QuantizedNormalizer
)
from ads_pytorch.nn.module.universal_normalizer.layer import QuantizedEmbeddingInputsNormalizer


def test_get_mark():
    p = torch.nn.Parameter(torch.zeros(1))
    assert get_mark(param=p) == frozenset({DEFAULT_MARK})


def test_mark():
    p = torch.nn.Parameter(torch.zeros(1))
    mark_parameter(param=p, mark="ahaha")
    assert get_mark(param=p) == frozenset({"ahaha"})


def test_mark_unconcatable_universal_normalizer_parameter_true():
    p = QuantizedNormalizerParameter(torch.zeros(1))
    assert is_unconcatable(p)


def test_mark_unconcatable_parameter_universal_normalizer_parameter_false():
    p = torch.nn.Parameter(torch.zeros(1))
    assert not is_unconcatable(p)


def test_mark_module():
    linear = torch.nn.Linear(10, 10)
    mark_module(module=linear, mark="ahaha")
    for p in linear.parameters():
        assert get_mark(param=p) == frozenset({"ahaha"})


def test_double_mark():
    p = torch.nn.Parameter(torch.zeros(1))
    mark_parameter(param=p, mark="ahaha")
    mark_parameter(param=p, mark="ohohoh")
    assert get_mark(param=p) == frozenset({"ahaha", "ohohoh"})


def test_set_mark():
    p = torch.nn.Parameter(torch.zeros(1))
    mark_parameter(param=p, mark="ahaha")
    _set_mark(param=p, mark=frozenset({"ohohoh"}))
    assert get_mark(param=p) == frozenset({"ohohoh"})


##############################################################
#                        MODEL WRAPPING                      #
##############################################################


# use fixture everywhere to test backward compatibility with old dumps
# which are equal to False
@pytest.fixture(params=[True, False], ids=["FixedOrder", "LegacyOrder"])
def fixed_order(request):
    return request.param


def test_is_wrapped_model(fixed_order):
    linear = torch.nn.Linear(10, 10)
    assert not is_wrapped_model(linear)
    linear = wrap_model_with_concat_wrapper(linear, fixed_sorted_parameters_order=fixed_order)
    assert is_wrapped_model(linear)


def test_wrap_model_single_buffer(fixed_order):
    model = torch.nn.Linear(10, 10)
    model = wrap_model_with_concat_wrapper(model=model, fixed_sorted_parameters_order=fixed_order)

    assert len(list(model.buffer_parameters())) == 1


def test_wrap_model_unconcatable_several_buffers_fixed_True():
    model = torch.nn.Sequential(torch.nn.Linear(10, 10),
                                QuantizedEmbeddingInputsNormalizer(shape=[10], dims_to_reduce=[0]),
                                QuantizedEmbeddingInputsNormalizer(shape=[10], dims_to_reduce=[0]))
    model = wrap_model_with_concat_wrapper(model=model, fixed_sorted_parameters_order=True)

    assert len(list(model.buffer_parameters())) == 3


def test_wrap_model_unconcatable_several_buffers_fixed_False():
    model = torch.nn.Sequential(torch.nn.Linear(10, 10),
                                QuantizedEmbeddingInputsNormalizer(shape=[10], dims_to_reduce=[0]),
                                QuantizedEmbeddingInputsNormalizer(shape=[10], dims_to_reduce=[0]))
    with pytest.raises(Exception):
        wrap_model_with_concat_wrapper(model=model, fixed_sorted_parameters_order=False)


def test_wrap_model_several_buffers(fixed_order):
    model = torch.nn.Linear(10, 10)
    mark_parameter(model.bias, "x1")
    model = wrap_model_with_concat_wrapper(model=model, fixed_sorted_parameters_order=fixed_order)

    assert len(list(model.buffer_parameters())) == 2


def test_train_mode(fixed_order):
    model = torch.nn.Linear(10, 10)
    model = wrap_model_with_concat_wrapper(model, fixed_sorted_parameters_order=fixed_order)
    assert model.training
    model.eval()
    assert not model.training
    model.train()
    assert model.training
    model.train(False)
    assert not model.training


# We have to test our wrapper in three different settings:
# 1. Ordinary torch optimization
# 2. CPU worker
# 3. GPU worker


@dataclasses.dataclass
class ModelResults:
    predictions: List[torch.Tensor] = dataclasses.field(default_factory=list)
    losses: List[float] = dataclasses.field(default_factory=list)


async def run_model(
    model: torch.nn.Module,
    loss: torch.nn.Module,
    optimizers: List[torch.optim.Optimizer],
    data_iterator,
    mode: str = "cpu"
):
    assert mode in {"usual", "cpu", "cuda"}
    if mode == "usual":
        res = ModelResults()
        for inputs, targets in data_iterator:
            model.zero_grad()
            prediction = model(inputs)
            out = loss(prediction, targets)
            out.backward()
            res.predictions.append(prediction.data)
            res.losses.append(float(out.data))
            finalize_gradient_computation(model=model)
            for opt in optimizers:
                opt.step()
        return res

    devices = {torch.device("cpu")} if mode == "cpu" else {torch.device("cuda", 0)}
    futures = []
    async with MinibatchPoolFactory(
        allow_async_gpu_update=False,
        model=model,
        loss=loss,
        deep_optimizers=optimizers,
        hash_embedding_optimizers=[],
        calcer_results_handlers=[],
        train_mode=True,
        devices=frozenset(devices),
        num_workers=1,
        get_predictions=True,
    )() as worker_pool:
        for data, target in data_iterator:
            futures.append(await worker_pool.assign_job(MinibatchRecord(inputs=data, targets=target)))

    results: List[CalcerResults] = await asyncio.gather(*futures)
    user_res = ModelResults()
    user_res.predictions = [x.predictions for x in results]
    user_res.losses = [x.losses["Loss"] for x in results]
    return user_res


MODES = [
    "usual",
    "cpu",
    pytest.param("cuda", marks=pytest.mark.requires_cuda)
]


@pytest.mark.parametrize("mode", MODES)
@pytest.mark.asyncio
async def test_wrapped_model_preserve_initialization(mode, fixed_order):
    torch.manual_seed(7287428)
    model = torch.nn.Linear(10, 10)
    torch.nn.init.normal_(model.bias, mean=0., std=2)
    torch.nn.init.orthogonal_(model.weight)

    data = torch.rand(10, 10)
    targets = torch.rand(10)

    reference = model(data)

    model = wrap_model_with_concat_wrapper(model, fixed_sorted_parameters_order=fixed_order)
    results = await run_model(
        model=model,
        loss=torch.nn.MSELoss(),
        optimizers=[],
        data_iterator=[(data, targets)],
        mode=mode
    )

    assert torch.allclose(reference, results.predictions[0], atol=1e-5)


@pytest.mark.parametrize("same_buffer", [True, False], ids=['OneBuffer', 'MultipleBuffer'])
@pytest.mark.parametrize("mode", MODES)
@pytest.mark.asyncio
async def test_wrapped_model_same_optimization_path(mode, same_buffer, fixed_order):
    torch.manual_seed(7287428)

    data = torch.rand(10, 10)
    targets = torch.rand(10)
    loss = torch.nn.MSELoss()

    usual_model = torch.nn.Linear(10, 1)
    torch.nn.init.normal_(usual_model.bias, mean=0., std=2)
    torch.nn.init.orthogonal_(usual_model.weight)
    usual_optimizer = torch.optim.SGD(usual_model.parameters(), lr=0.001)

    model = torch.nn.Linear(10, 1)
    model.load_state_dict(usual_model.state_dict())
    if not same_buffer:
        mark_parameter(model.bias, "bias")
        mark_parameter(model.weight, "weight")

    for _ in range(10):
        usual_model.zero_grad()
        loss(usual_model(data), targets).backward()
        usual_optimizer.step()

    model = wrap_model_with_concat_wrapper(model, fixed_sorted_parameters_order=fixed_order)
    if same_buffer:
        assert len(list(model.buffer_parameters())) == 1
    else:
        assert len(list(model.buffer_parameters())) == 2
    await run_model(
        model=model,
        loss=loss,
        optimizers=[
            torch.optim.SGD(model.buffer_parameters(), lr=0.001)
        ],
        data_iterator=[(data, targets)] * 10,
        mode=mode
    )
    model.cpu()

    assert torch.allclose(model(data), usual_model(data))


@pytest.mark.parametrize("same_buffer", [True, False], ids=['OneBuffer', 'MultipleBuffer'])
@pytest.mark.parametrize("mode", MODES)
@pytest.mark.asyncio
async def test_wrapped_model_same_optimization_path_universal_normalizer(mode, same_buffer):
    torch.manual_seed(7287428)

    data = torch.rand(10, 10)
    targets = torch.rand(10)
    loss = torch.nn.MSELoss()

    usual_model = torch.nn.Sequential(
        QuantizedEmbeddingInputsNormalizer(
            shape=[10],
            dims_to_reduce=[0],
            bins_number=128,
            compression=1000.0,
            interpolate=False,
            use_cdf=False,
            use_double_binarization=False
        ),
        torch.nn.Linear(10, 1)
    )
    for p in usual_model[0].embedding_layer.parameters():
        torch.nn.init.normal_(p)

    torch.nn.init.normal_(usual_model[1].bias, mean=0., std=2)
    torch.nn.init.orthogonal_(usual_model[1].weight)
    usual_optimizer = torch.optim.SGD([p for p in list(usual_model.parameters()) if not is_universal_normalizer_parameter(p)], lr=0.001)
    usual_universal_optimizer = QuantizedNormalizerParameterServerOptimizer([p for p in list(usual_model.parameters()) if is_universal_normalizer_parameter(p)])

    model = torch.nn.Sequential(
        QuantizedEmbeddingInputsNormalizer(
            shape=[10],
            dims_to_reduce=[0],
            bins_number=128,
            compression=1000.0,
            interpolate=False,
            use_cdf=False,
            use_double_binarization=False,
        ),
        torch.nn.Linear(10, 1)
    )

    for p in model[0].embedding_layer.parameters():
        torch.nn.init.normal_(p)

    model.load_state_dict(usual_model.state_dict())

    if not same_buffer:
        mark_parameter(model[1].bias, "bias")
        mark_parameter(model[1].weight, "weight")
        mark_parameter(model[0].embedding_layer[0].weight, "embed_weight")

    for _ in range(10):
        usual_model.zero_grad()
        loss(usual_model(data), targets).backward()
        for opt in [usual_optimizer, usual_universal_optimizer]:
            opt.step()

    model = wrap_model_with_concat_wrapper(model, fixed_sorted_parameters_order=True)

    if same_buffer:
        assert len(list(model.buffer_parameters())) == 2 # (linear layer + embeddings) + TDigest tensor parameters
    else:
        assert len(list(model.buffer_parameters())) == 4 # (embeddings) + (bias) + (weight) + TDigest tensor parameters

    await run_model(
        model=model,
        loss=loss,
        optimizers=[
            torch.optim.SGD([p for p in list(model.buffer_parameters()) if not is_universal_normalizer_parameter(p)], lr=0.001),
            QuantizedNormalizerParameterServerOptimizer([p for p in list(model.buffer_parameters()) if is_universal_normalizer_parameter(p)])
        ],
        data_iterator=[(data, targets)] * 10,
        mode=mode
    )
    model.cpu()
    a = model(data)
    b = usual_model(data)

    assert torch.allclose(a, b)

class UnusedParameterModel(torch.nn.Module):
    def __init__(self):
        super(UnusedParameterModel, self).__init__()
        self.net = torch.nn.Linear(10, 1)
        self.useless = torch.nn.Parameter(torch.zeros(10))

    def forward(self, tensor):
        return self.net(tensor)


@pytest.mark.parametrize("mode", MODES)
@pytest.mark.asyncio
async def test_parameter_not_involved_in_computation(mode, fixed_order):
    torch.manual_seed(7287428)

    data = torch.rand(10, 10)
    targets = torch.rand(10)
    loss = torch.nn.MSELoss()

    model = UnusedParameterModel()
    model = wrap_model_with_concat_wrapper(model, fixed_sorted_parameters_order=fixed_order)

    with pytest.raises(Exception):
        await run_model(
            model=model,
            loss=loss,
            optimizers=[torch.optim.SGD(model.buffer_parameters(), lr=0.001)],
            data_iterator=[(data, targets)],
            mode=mode
        )


@pytest.mark.asyncio
async def test_invalid_optimizers(fixed_order):
    torch.manual_seed(7287428)
    model = torch.nn.Linear(10, 1)
    model = wrap_model_with_concat_wrapper(model, fixed_sorted_parameters_order=fixed_order)
    with pytest.raises(ValueError):
        validate_optimizers(
            model=model,
            optimizers=[
                torch.optim.SGD(model.buffer_parameters(), lr=0.001),
                torch.optim.Adam([model.net.bias])
            ]
        )


_invalid_modes = MODES[:]
_invalid_modes.remove("usual")


@pytest.mark.parametrize("mode", _invalid_modes)
@pytest.mark.asyncio
async def test_invalid_optimizers_check_works_inside_create_pool(mode, fixed_order):
    torch.manual_seed(7287428)

    data = torch.rand(10, 10)
    targets = torch.rand(10)
    loss = torch.nn.MSELoss()

    model = torch.nn.Linear(10, 1)
    model = wrap_model_with_concat_wrapper(model, fixed_sorted_parameters_order=fixed_order)
    with pytest.raises(ValueError):
        await run_model(
            model=model,
            loss=loss,
            optimizers=[
                torch.optim.SGD(model.buffer_parameters(), lr=0.001),
                torch.optim.Adam([model.net.bias])
            ],
            data_iterator=[(data, targets)],
            mode=mode
        )


class DifferentDtypeModel(torch.nn.Module):
    def __init__(self):
        super(DifferentDtypeModel, self).__init__()
        self.p1 = torch.nn.Parameter(torch.zeros(1, dtype=torch.float32))
        self.p2 = torch.nn.Parameter(torch.zeros(1, dtype=torch.float64))

    def forward(self, x):
        x = x.to(torch.float32) + self.p1
        x = x.to(torch.float64) + self.p2
        return x.to(torch.float32).sum(dim=1)


@pytest.mark.parametrize("mode", MODES)
@pytest.mark.asyncio
async def test_different_dtypes(mode, fixed_order):
    torch.manual_seed(7287428)

    data = torch.rand(10, 10)
    targets = torch.rand(10)
    loss = torch.nn.MSELoss()

    model = DifferentDtypeModel()
    model = wrap_model_with_concat_wrapper(model, fixed_sorted_parameters_order=fixed_order)
    buffers = list(model.buffer_parameters())
    assert len(buffers) == 2
    assert all(x.numel() == 1 for x in buffers)
    assert {x.dtype for x in buffers} == {torch.float64, torch.float32}

    await run_model(
        model=model,
        loss=loss,
        optimizers=[
            torch.optim.SGD(model.buffer_parameters(), lr=0.001),
        ],
        data_iterator=[(data, targets)] * 3,
        mode=mode
    )
    model.cpu()
    model(data)
    for p in model.net.parameters():
        assert not torch.allclose(p, torch.zeros_like(p))


_MINIBATCH_MODES = MODES[:]
_MINIBATCH_MODES.remove("usual")


@pytest.mark.parametrize("mode", _MINIBATCH_MODES)
@pytest.mark.asyncio
async def test_switch_train_modes(mode, fixed_order):
    torch.manual_seed(7287428)

    data = torch.rand(10, 10)
    targets = torch.rand(10)
    loss = torch.nn.MSELoss()

    usual_model = torch.nn.Linear(10, 1)
    torch.nn.init.normal_(usual_model.bias, mean=0., std=2)
    torch.nn.init.orthogonal_(usual_model.weight)
    usual_optimizer = torch.optim.SGD(usual_model.parameters(), lr=0.001)

    model = torch.nn.Linear(10, 1)
    model.load_state_dict(usual_model.state_dict())

    for _ in range(10):
        usual_model.zero_grad()
        loss(usual_model(data), targets).backward()
        usual_optimizer.step()

    model = wrap_model_with_concat_wrapper(model, fixed_sorted_parameters_order=fixed_order)

    assert len(list(model.buffer_parameters())) == 1

    devices = {torch.device("cpu")} if mode == "cpu" else {torch.device("cuda", 0)}
    futures = []
    optimizers = [torch.optim.SGD(model.buffer_parameters(), lr=0.001)]
    data_iterator = [(data, targets)] * 1
    worker_pool = MinibatchPoolFactory(
        model=model,
        loss=loss,
        deep_optimizers=optimizers,
        hash_embedding_optimizers=[],
        calcer_results_handlers=[],
        train_mode=True,
        devices=frozenset(devices),
        num_workers=1,
        get_predictions=True,
        allow_async_gpu_update=False,
    )()
    async with worker_pool:
        for data, target in data_iterator:
            futures.append(await worker_pool.assign_job(MinibatchRecord(inputs=data, targets=target)))

    results: List[CalcerResults] = await asyncio.gather(*futures)
    user_res = ModelResults()
    user_res.predictions = [x.predictions for x in results]
    user_res.losses = [x.losses["Loss"] for x in results]

    await worker_pool.set_train_mode(False)
    async with worker_pool:
        for data, target in data_iterator:
            futures.append(await worker_pool.assign_job(MinibatchRecord(inputs=data, targets=target)))

    await worker_pool.set_train_mode(True)
    async with worker_pool:
        for data, target in data_iterator:
            futures.append(await worker_pool.assign_job(MinibatchRecord(inputs=data, targets=target)))


class MultiModel(torch.nn.Module):
    def __init__(self):
        super(MultiModel, self).__init__()
        self.part1 = torch.nn.Sequential(torch.nn.Linear(10, 10), torch.nn.Linear(10, 10))
        self.part2 = torch.nn.Sequential(torch.nn.Linear(10, 20), torch.nn.Linear(20, 10))

    def forward(self, inputs):
        return torch.sum(self.part1(inputs) + self.part2(inputs) * 2, dim=1)


def test_mark_module_save_load(fixed_order):
    inputs = torch.rand(20, 10)
    targets = torch.rand(20)
    model = MultiModel()
    mark_module(model.part1, "part1_mark")
    mark_module(model.part2, "part2_mark")
    wraped = wrap_model_with_concat_wrapper(model, fixed_sorted_parameters_order=fixed_order)
    assert len(wraped._concat_buffers) == 2
    opt = torch.optim.Adam(wraped.buffer_parameters())
    loss = torch.nn.MSELoss()
    for _ in range(10):
        wraped.zero_grad()
        loss_val = loss(wraped(inputs), targets)
        loss_val.backward()
        opt.step()
        finalize_gradient_computation(model=wraped)
    old_out = wraped(inputs)
    state_dict = wraped.state_dict()

    new_model = MultiModel()
    mark_module(new_model.part1, "part1_mark")
    mark_module(new_model.part2, "part2_mark")
    new_wraped = wrap_model_with_concat_wrapper(new_model, fixed_sorted_parameters_order=fixed_order)
    new_wraped.load_state_dict(state_dict)

    assert torch.allclose(old_out, new_wraped(inputs))


##################################################################
#                      Parameter order testing                   #
##################################################################
# https://st.yandex-team.ru/BSDEV-83520


def _make_random_init_params_class(base_cls, sizes, marks, names):
    class MyRandomInitParamsModule(base_cls):
        def __init__(self, shuffled_names):
            super(MyRandomInitParamsModule, self).__init__()
            # check different sizes, it's important
            for name in shuffled_names:
                self.register_parameter(name, torch.nn.Parameter(torch.rand(sizes[name])))

            # This will test different buffers. Old models have kept their
            # buffers in lists
            for name in shuffled_names:
                mark_parameter(getattr(self, name), marks[name])

        def _forward_impl(self, tensor: torch.Tensor) -> torch.Tensor:
            # fixed order for concat
            weights = torch.cat([getattr(self, name) for name in names])
            return tensor @ weights.unsqueeze(1)

        def forward(self, tensor: torch.Tensor) -> torch.Tensor:
            if issubclass(base_cls, BaseParameterServerModule):
                return super(MyRandomInitParamsModule, self).forward(tensor)
            return self._forward_impl(tensor)

        def async_forward(self, tensor):
            return tensor

        def sync_forward(self, tensor):
            return self._forward_impl(tensor)

    return MyRandomInitParamsModule


@pytest.mark.parametrize("base_cls", [torch.nn.Module, BaseParameterServerModule])
@pytest.mark.parametrize("max_marks", [1, 3, 100500], ids=["Mark1", "Mark3", "AllMark"])
def test_deep_parameters_order_from_state_dict(base_cls, max_marks):
    seed = 43878748378
    torch.manual_seed(seed)
    names = [f"x{i}" for i in range(20)]
    sizes = {name: i + 5 for i, name in enumerate(names)}
    marks = {name: f"_deep_param_mark_{i % max_marks}" for i, name in enumerate(names)}

    # This will ensure different buffers order, different parameters order in state dict
    # and different order of parameters inside buffers
    first_order = ['x17', 'x5', 'x6', 'x18', 'x8', 'x0', 'x19', 'x1', 'x14', 'x3', 'x12', 'x2', 'x4', 'x11', 'x16', 'x13', 'x15', 'x7', 'x10', 'x9']
    second_order = ['x18', 'x10', 'x0', 'x19', 'x11', 'x9', 'x7', 'x16', 'x13', 'x14', 'x3', 'x1', 'x8', 'x6', 'x4', 'x2', 'x15', 'x5', 'x17', 'x12']

    inputs = torch.rand(10, sum(sizes.values()))
    targets = torch.rand(10, 1)

    def _compare_params():
        for name in names:
            p1 = getattr(loaded_model.net, name)
            p2 = getattr(model.net, name)
            assert torch.allclose(p1, p2)

    def _step(cur_model: torch.nn.Module, cur_optim):
        cur_model.zero_grad(set_to_none=True)
        torch.nn.functional.mse_loss(cur_model(inputs), targets).backward()
        finalize_gradient_computation(model=cur_model)
        cur_optim.step()

    MyRandomInitParamsModule = _make_random_init_params_class(base_cls=base_cls, sizes=sizes, marks=marks, names=names)

    model = MyRandomInitParamsModule(first_order)
    model = wrap_model_with_concat_wrapper(model, fixed_sorted_parameters_order=True)
    optim = torch.optim.Adam(model.buffer_parameters())

    # Train model for a while to warmup optimizer
    for _ in range(2):
        _step(cur_model=model, cur_optim=optim)

    state_dict = model.state_dict()
    optim_state_dict = optim.state_dict()

    loaded_model = MyRandomInitParamsModule(second_order)
    loaded_model = wrap_model_with_concat_wrapper(loaded_model, fixed_sorted_parameters_order=True)
    # make before!!!
    loaded_optim = torch.optim.Adam(loaded_model.buffer_parameters())
    loaded_model.load_state_dict(state_dict)
    loaded_optim.load_state_dict(optim_state_dict)

    _compare_params()

    for _ in range(10):
        _step(cur_model=model, cur_optim=optim)
        _step(cur_model=loaded_model, cur_optim=loaded_optim)

    with torch.no_grad():
        assert torch.allclose(loaded_model(inputs), model(inputs))
    _compare_params()


@dataclasses.dataclass
class _LoadWrappedDumpConfig:
    first_order: bool
    second_order: bool
    is_legacy_dump: bool


@pytest.mark.parametrize("base_cls", [torch.nn.Module, BaseParameterServerModule])
@pytest.mark.parametrize("max_marks", [1, 3, 100500], ids=["Mark1", "Mark3", "AllMark"])
@pytest.mark.parametrize(
    "flags",
    [
        _LoadWrappedDumpConfig(first_order=True, second_order=False, is_legacy_dump=False),
        _LoadWrappedDumpConfig(first_order=False, second_order=True, is_legacy_dump=False),
        _LoadWrappedDumpConfig(first_order=False, second_order=True, is_legacy_dump=True)
    ])
def test_throwing_error_on_trying_to_load_inconsistent_dump(base_cls, max_marks, flags):
    seed = 43878748378
    torch.manual_seed(seed)
    names = [f"x{i}" for i in range(20)]
    sizes = {name: i + 5 for i, name in enumerate(names)}
    marks = {name: f"_deep_param_mark_{i % max_marks}" for i, name in enumerate(names)}

    # This will ensure different buffers order, different parameters order in state dict
    # and different order of parameters inside buffers
    first_order = ['x17', 'x5', 'x6', 'x18', 'x8', 'x0', 'x19', 'x1', 'x14', 'x3', 'x12', 'x2', 'x4', 'x11', 'x16', 'x13', 'x15', 'x7', 'x10', 'x9']
    second_order = ['x18', 'x10', 'x0', 'x19', 'x11', 'x9', 'x7', 'x16', 'x13', 'x14', 'x3', 'x1', 'x8', 'x6', 'x4', 'x2', 'x15', 'x5', 'x17', 'x12']

    MyRandomInitParamsModule = _make_random_init_params_class(base_cls=base_cls, sizes=sizes, marks=marks, names=names)

    model = MyRandomInitParamsModule(shuffled_names=first_order)
    model = wrap_model_with_concat_wrapper(model, fixed_sorted_parameters_order=flags.first_order)
    state_dict = model.state_dict()
    if flags.is_legacy_dump:
        state_dict.pop(_CONCAT_WRAPPER_PARAMETERS_ORDER_KEY)

    loaded_model = MyRandomInitParamsModule(shuffled_names=second_order)
    loaded_model = wrap_model_with_concat_wrapper(loaded_model, fixed_sorted_parameters_order=flags.second_order)

    with pytest.raises(RuntimeError):
        loaded_model.load_state_dict(state_dict)


@pytest.mark.parametrize("base_cls", [torch.nn.Module, BaseParameterServerModule])
@pytest.mark.parametrize("max_marks", [1, 3, 100500], ids=["Mark1", "Mark3", "AllMark"])
@pytest.mark.parametrize(
    "flags",
    [
        _LoadWrappedDumpConfig(first_order=False, second_order=False, is_legacy_dump=False),
        _LoadWrappedDumpConfig(first_order=False, second_order=False, is_legacy_dump=True)
    ])
def test_can_read_legacy_dump(base_cls, max_marks, flags):
    seed = 43878748378
    torch.manual_seed(seed)
    names = [f"x{i}" for i in range(20)]
    sizes = {name: i + 5 for i, name in enumerate(names)}
    marks = {name: f"_deep_param_mark_{i % max_marks}" for i, name in enumerate(names)}

    inputs = torch.rand(10, sum(sizes.values()))
    targets = torch.rand(10, 1)

    def _compare_params():
        for name in names:
            p1 = getattr(loaded_model.net, name)
            p2 = getattr(model.net, name)
            assert torch.allclose(p1, p2)

    def _step(cur_model: torch.nn.Module, cur_optim):
        cur_model.zero_grad(set_to_none=True)
        torch.nn.functional.mse_loss(cur_model(inputs), targets).backward()
        finalize_gradient_computation(model=cur_model)
        cur_optim.step()

    MyRandomInitParamsModule = _make_random_init_params_class(base_cls=base_cls, sizes=sizes, marks=marks, names=names)

    model = MyRandomInitParamsModule(names)
    model = wrap_model_with_concat_wrapper(model, fixed_sorted_parameters_order=flags.first_order)
    optim = torch.optim.Adam(model.buffer_parameters())

    # Train model for a while to warmup optimizer
    for _ in range(2):
        _step(cur_model=model, cur_optim=optim)

    state_dict = model.state_dict()
    if flags.is_legacy_dump:
        state_dict.pop(_CONCAT_WRAPPER_PARAMETERS_ORDER_KEY)
    optim_state_dict = optim.state_dict()

    loaded_model = MyRandomInitParamsModule(names)
    loaded_model = wrap_model_with_concat_wrapper(loaded_model, fixed_sorted_parameters_order=flags.second_order)
    # make before!!!
    loaded_optim = torch.optim.Adam(loaded_model.buffer_parameters())
    loaded_model.load_state_dict(state_dict)
    loaded_optim.load_state_dict(optim_state_dict)

    _compare_params()

    for _ in range(10):
        _step(cur_model=model, cur_optim=optim)
        _step(cur_model=loaded_model, cur_optim=loaded_optim)

    with torch.no_grad():
        assert torch.allclose(loaded_model(inputs), model(inputs))
    _compare_params()

