import pytest
import torch
import random
import dataclasses
import itertools
import copy

from ads_pytorch.model_calcer.parameter_server_module.calcer import ParameterServerModuleCalcer
from ads_pytorch.model_calcer.parameter_server_module.factory import ParameterServerCalcerFactory
from ads_pytorch.model_calcer.calcer import AbstractCalcer, CalcerResults
from ads_pytorch.core import BaseParameterServerModule
from ads_pytorch.deploy.deployable_model import IDeployableModel
from ads_pytorch.nn.module.base_embedding_output import BaseEmbeddingModelOutput
from ads_pytorch.nn.module.base_embedding_model import BaseEmbeddingModel, EmbeddingDescriptor, EmbeddingComputeDescriptor

from ads_pytorch.hash_embedding.hash_embedding import (
    HashEmbedding,
    create_hash_table,
    create_item
)
from ads_pytorch.hash_embedding.optim import EmbeddingAdamOptimizer
from ads_pytorch.cpp_lib import libcpp_lib
from ads_pytorch.tools.nested_structure import apply

from typing import Dict, List, Any
from ads_pytorch.tools.async_worker_pool import BaseAsyncWorker, BaseAsyncWorkerPool

DIM = 5


@pytest.fixture(params=[True, False], ids=["C++", "Python"])
def cpp_lazy_context(request):
    return request.param


@dataclasses.dataclass
class SynchronousDeepCalcer(AbstractCalcer):
    model: BaseParameterServerModule
    loss: torch.nn.Module

    def __post_init__(self):
        self.model = copy.copy(self.model)
        setattr(self.model, 'forward', self.model.sync_forward)

    async def __call__(
            self,
            inputs,
            targets,
            data_identity: int = 0
    ) -> CalcerResults:
        predictions = self.model(inputs)
        output = self.loss(predictions, targets)
        output.backward()

        input_gradients = apply(inputs, fn=lambda x: x.grad if x is not None else None)

        return CalcerResults(
            metrics={"some_value": 123},
            input_gradients=input_gradients,
            predictions=predictions,
            losses=output
        )

    def get_device(self) -> torch.device:
        return list(self.model.parameters())[0].device


class MyParameterServerModule(IDeployableModel):
    def __init__(self, dim=DIM):
        super(MyParameterServerModule, self).__init__(embedding_model=BaseEmbeddingModel(
            embeddings=[
                EmbeddingDescriptor(
                    name="hash1",
                    features=[EmbeddingComputeDescriptor(feature="1", compute_mode="sum")],
                    dim=dim
                ),
                EmbeddingDescriptor(
                    name="hash2",
                    features=[EmbeddingComputeDescriptor(feature="2", compute_mode="sum")],
                    dim=dim
                ),
            ],
            external_factors=[]
        ))

    def deployable_model_forward(self, embedded_inputs: List[torch.Tensor]):
        return sum([torch.sum(x, dim=1) for x in embedded_inputs])


def ps_calcer(
        model: torch.nn.Module,
        loss: torch.nn.Module,
        optimizers,
        cpp_lazy_context: bool,
        num_threads=3,
        train_mode: bool = True,
        cls=ParameterServerModuleCalcer,
):
    return cls(
        model=model,
        deep_calcer=SynchronousDeepCalcer(model=model, loss=loss),
        cpp_thread_pool=libcpp_lib.ThreadPoolHandle(num_threads),
        updated_parameters=dict(model.named_hash_embedding_parameters()),
        optimizers=optimizers,
        train_mode=train_mode,
        cpp_lazy_context=cpp_lazy_context
    )


@pytest.fixture
def data():
    inputs = {
        "1": [torch.LongTensor([1, 2, 3]), torch.IntTensor([1, 2])],
        "2": [torch.LongTensor([10, 20, 30]), torch.IntTensor([2, 1])]
    }
    targets = torch.FloatTensor([1, 2])
    return inputs, targets


@pytest.mark.parametrize('train_mode', [True, False], ids=['Train', 'Evaluate'])
@pytest.mark.asyncio
async def test_parameter_server_mode(train_mode, cpp_lazy_context):
    model = MyParameterServerModule()
    loss = torch.nn.MSELoss()

    with pytest.raises(ValueError):
        ps_calcer(model=model, loss=loss, num_threads=3, optimizers=[], train_mode=train_mode, cpp_lazy_context=cpp_lazy_context)


@pytest.mark.parametrize('train_mode', [True, False], ids=['Train', 'Evaluate'])
@pytest.mark.asyncio
async def test_forward_without_optimizars(data, train_mode, cpp_lazy_context):
    model = MyParameterServerModule()
    model.parameter_server_mode = True

    loss = torch.nn.MSELoss()

    calcer = ps_calcer(model=model, loss=loss, optimizers=[], num_threads=3, train_mode=train_mode, cpp_lazy_context=cpp_lazy_context)

    for i in range(100):
        item = create_item("adam", DIM)
        item.w = torch.ones(DIM) * i * 0.0705
        model.embedding_model.embeddings["hash1"].insert_item(i, item)

        item = create_item("adam", DIM)
        item.w = torch.ones(DIM) * (i * 0.0134)
        model.embedding_model.embeddings["hash2"].insert_item(i, item)

    assert model.embedding_model.embeddings["hash1"].size() == 100
    assert model.embedding_model.embeddings["hash2"].size() == 100

    inputs, targets = data

    res = await calcer(inputs=inputs, targets=targets)

    value1 = (0.0705 * 1 + 30 * 0.0134) * DIM
    value2 = (0.0705 * 5 + 30 * 0.0134) * DIM

    assert torch.allclose(res.predictions, torch.FloatTensor([value1, value2]))
    assert torch.allclose(loss(torch.FloatTensor([value1, value2]), targets), res.losses)


@pytest.mark.asyncio
async def test_forward_with_optimizer_converge(data, cpp_lazy_context):
    torch.manual_seed(12345)
    model = MyParameterServerModule()
    model.parameter_server_mode = True

    optimizers = [EmbeddingAdamOptimizer(model.hash_embedding_parameters(), lr=0.001)]

    loss = torch.nn.MSELoss()

    calcer = ps_calcer(model=model, loss=loss, optimizers=optimizers, num_threads=3, train_mode=True, cpp_lazy_context=cpp_lazy_context)

    inputs, targets = data

    for _ in range(99):
        await calcer(inputs=inputs, targets=targets)
    res = await calcer(inputs=inputs, targets=targets)

    assert float(res.losses) < 0.33


class CounterParameterServerModuleCalcer(ParameterServerModuleCalcer):
    def __init__(self, *args, **kwargs):
        super(CounterParameterServerModuleCalcer, self).__init__(*args, **kwargs)

        self.forward_counter_call = 0
        self.update_counter_call = 0
        self.backward_counter_call = 0

    async def _calc_forward(self, inputs):
        self.forward_counter_call += 1
        return await super(CounterParameterServerModuleCalcer, self)._calc_forward(inputs=inputs)

    async def _update(self):
        self.update_counter_call += 1
        return await super(CounterParameterServerModuleCalcer, self)._update()

    async def _backward(
            self,
            async_tensors,
            calc_res: CalcerResults,
    ):
        self.backward_counter_call += 1
        return await super(CounterParameterServerModuleCalcer, self)._backward(async_tensors=async_tensors, calc_res=calc_res)


@pytest.mark.parametrize('train_mode', [True, False], ids=['Train', 'Evaluate'])
@pytest.mark.asyncio
async def test_backward_and_update_called_with_optimizer(data, train_mode, cpp_lazy_context):
    torch.manual_seed(12345)
    model = MyParameterServerModule()
    model.parameter_server_mode = True

    optimizers = [EmbeddingAdamOptimizer(model.hash_embedding_parameters(), lr=0.001)]

    loss = torch.nn.MSELoss()

    calcer = ps_calcer(
        model=model,
        loss=loss,
        optimizers=optimizers,
        num_threads=3,
        train_mode=train_mode,
        cls=CounterParameterServerModuleCalcer,
        cpp_lazy_context=cpp_lazy_context
    )

    inputs, targets = data

    for _ in range(3):
        await calcer(inputs=inputs, targets=targets)

    assert calcer.forward_counter_call == 3
    if train_mode:
        assert calcer.backward_counter_call == 3
        assert calcer.update_counter_call == 3
    else:
        assert calcer.backward_counter_call == 0
        assert calcer.update_counter_call == 0


@pytest.mark.parametrize('train_mode', [True, False], ids=['Train', 'Evaluate'])
@pytest.mark.asyncio
async def test_backward_and_update_not_called_without_optimizer(data, train_mode, cpp_lazy_context):
    torch.manual_seed(12345)
    model = MyParameterServerModule()
    model.parameter_server_mode = True

    optimizers = []

    loss = torch.nn.MSELoss()

    calcer = ps_calcer(
        model=model,
        loss=loss,
        optimizers=optimizers,
        num_threads=3,
        train_mode=train_mode,
        cls=CounterParameterServerModuleCalcer,
        cpp_lazy_context=cpp_lazy_context
    )

    inputs, targets = data

    for _ in range(3):
        await calcer(inputs=inputs, targets=targets)

    assert calcer.forward_counter_call == 3
    assert calcer.backward_counter_call == 0
    assert calcer.update_counter_call == 0


#############################################################
#                    PARALLEL COMPUTATION                   #
#############################################################


"""
As always - just brute-force every possible combination of parameters
Also, this test uses the parameter server calcer factory because
this calcer is not intended for standalone use without it.

These are mostly API tests
"""


class CPUWorker(BaseAsyncWorker):
    def __init__(self, calcer: AbstractCalcer, rank: int):
        super(CPUWorker, self).__init__(rank)
        self._calcer = calcer

    async def _do_job(self, inputs, targets):
        return await self._calcer(inputs=inputs, targets=targets, data_identity=self.rank)


class CPUWorkerPool(BaseAsyncWorkerPool):
    def __init__(self, calcers: Dict[int, AbstractCalcer], num_workers: int):
        super(CPUWorkerPool, self).__init__(workers_count=num_workers)
        self._calcers = calcers

    def create_new_worker(self, new_rank: int):
        return CPUWorker(
            calcer=self._calcers[new_rank],
            rank=new_rank
        )


class SingleEmbeddingModel(IDeployableModel):
    def __init__(self, num_embeds: int = 10, dim: int = 100):
        super(SingleEmbeddingModel, self).__init__(embedding_model=BaseEmbeddingModel(
            embeddings=[
                EmbeddingDescriptor(name=str(i), features=[str(i)], dim=dim)
                for i in range(num_embeds)
            ],
            external_factors=[]
        ))
        self.net = torch.nn.Linear(dim, 1)
        torch.nn.init.orthogonal_(self.net.weight)

    @property
    def embeddings(self):
        return self.embedding_model.embeddings

    def deployable_model_forward(self, embedded_inputs: List[torch.Tensor]) -> Any:
        return self.net(sum(embedded_inputs)).squeeze()


# Test on every possible configuration of the trainer


@pytest.mark.parametrize('do_update', [True, False], ids=['Update', 'Forward'])
@pytest.mark.parametrize('num_threads', [1, 6], ids=['1Thread', '3Thread'])
@pytest.mark.parametrize('max_size_per_update_job', [-1, 40, 350000])
@pytest.mark.asyncio
async def test_parallel_computation(
        num_threads,
        max_size_per_update_job,
        do_update,
        cpp_lazy_context
):
    torch.manual_seed(12345)

    num_workers = 5
    num_embeddings = 10
    dim = 5

    loss = torch.nn.MSELoss()
    model = SingleEmbeddingModel(
        dim=dim,
        num_embeds=num_embeddings,
    )
    model.parameter_server_mode = True

    if do_update:
        optimizers = [EmbeddingAdamOptimizer(model.hash_embedding_parameters())]
    else:
        optimizers = []

    factory = ParameterServerCalcerFactory(
        deep_calcer_factory=lambda x: SynchronousDeepCalcer(model=model, loss=loss),
        model=model,
        num_threads=num_threads,
        hash_embedding_optimizers=optimizers,
        max_size_per_update_job=max_size_per_update_job,
        train_mode=True,
        cpp_lazy_context=cpp_lazy_context
    )

    calcers = {i: factory(i) for i in range(num_workers)}

    pool = CPUWorkerPool(calcers=calcers, num_workers=num_workers)

    inputs_history = []
    async with pool:
        for _ in range(30):
            # Variable-sized batch with variable-sized inputs
            batch_size = random.randint(2, 100)
            lens = torch.randint(3, [batch_size], dtype=torch.int)
            inputs = {
                str(i): [
                    torch.randint(1 << 20, [sum(lens)], dtype=torch.long),
                    lens
                ]
                for i in range(num_embeddings)
            }
            targets = torch.randn(batch_size)
            inputs_history.append(inputs)
            calcer_task = await pool.assign_job(inputs=inputs, targets=targets)

    calcer_res = await calcer_task

    if do_update:
        # with update enabled, we must have inserted all features into hash tables
        for i, embed in model.embeddings.items():
            tensors = [x[i][0] for x in inputs_history]
            new_features = set(torch.cat(tensors, 0).tolist())
            assert embed.size() == len(new_features)
    else:
        # no new features
        for embed in model.embeddings.values():
            assert embed.size() == 0

    # sanity check on calcer return value, again, in any configuration
    calcer_metric_names = set(calcer_res.metrics.keys())
    assert calcer_res.input_gradients is None
    # Check that predictions and losses are forwarded from deep calcer
    assert isinstance(calcer_res.predictions, torch.Tensor)
    assert isinstance(calcer_res.losses, torch.Tensor)


###################################################################
#                           CORNER CASES                          #
###################################################################

"""
1. Hash table used more than once
2. Hash table that is not updated
3. Hash table which gradient flaps, i.e. there is some randomness in the model
"""


class HardEmbeddingModel(BaseParameterServerModule):
    def __init__(self, num_embeds: int = 10, dim: int = 100):
        super(HardEmbeddingModel, self).__init__()
        # Ordinary and most-common usage: singly used embedding with gradient
        self.embeddings = torch.nn.ModuleDict({
            str(i): HashEmbedding(
                create_hash_table("adam", dim),
            )
            for i in range(num_embeds)
        })

        # used in optimizer, but gradients are Nones
        self.no_grad_embeddings = torch.nn.ModuleDict({
            str(i): HashEmbedding(create_hash_table("adam", dim))
            for i in range(num_embeds)
        })

        self.not_updated_embeddings = torch.nn.ModuleDict({
            str(i): HashEmbedding(create_hash_table("adam", dim))
            for i in range(num_embeds)
        })

        self.multiply_used_embedding = HashEmbedding(create_hash_table("adam", dim))

        self.net = torch.nn.Linear(dim, 1)
        torch.nn.init.orthogonal_(self.net.weight)

    def async_forward(self, inputs):
        return BaseEmbeddingModelOutput(
            embeddings={
                **{f"used{i}": self.embeddings[i](*value) for i, value in inputs.items()},
                **{f"not_updated{i}": self.not_updated_embeddings[i](*value) for i, value in inputs.items()},
                **{f"no_grad{i}": self.no_grad_embeddings[i](*value) for i, value in inputs.items()},
                **{f"multiple{i}": self.multiply_used_embedding(*value) for i, value in inputs.items()}
            },
            external={}
        )

    def sync_forward(self, async_outputs: BaseEmbeddingModelOutput):
        needed = sum(list(value for key, value in async_outputs.embeddings.items() if "no_grad" not in key))
        return self.net(needed).squeeze()


# Heavy parallel update on complicated model


@pytest.mark.parametrize('combine_optimizers', [True, False], ids=['OptimCombine', 'OptimSplit'])
@pytest.mark.asyncio
async def test_complicated_model(combine_optimizers, cpp_lazy_context):
    torch.manual_seed(12345)

    num_workers = 3
    num_threads = 10
    max_size_per_update_job = 100
    num_embeddings = 3
    dim = 5

    loss = torch.nn.MSELoss()
    model = HardEmbeddingModel(
        dim=dim,
        num_embeds=num_embeddings,
    )
    model.parameter_server_mode = True

    if combine_optimizers:
        optimizers = [
            EmbeddingAdamOptimizer(
                itertools.chain(
                    [x.parameter_with_hash_table for x in model.embeddings.values()],
                    [x.parameter_with_hash_table for x in model.no_grad_embeddings.values()],
                    [model.multiply_used_embedding.parameter_with_hash_table]
                ),
            )
        ]
    else:
        optimizers = [
            EmbeddingAdamOptimizer([x.parameter_with_hash_table for x in model.embeddings.values()]),
            EmbeddingAdamOptimizer([x.parameter_with_hash_table for x in model.no_grad_embeddings.values()]),
            EmbeddingAdamOptimizer([model.multiply_used_embedding.parameter_with_hash_table])
        ]

    factory = ParameterServerCalcerFactory(
        deep_calcer_factory=lambda x: SynchronousDeepCalcer(model=model, loss=loss),
        model=model,
        num_threads=num_threads,
        hash_embedding_optimizers=optimizers,
        max_size_per_update_job=max_size_per_update_job,
        train_mode=True,
        cpp_lazy_context=cpp_lazy_context
    )

    calcers = {i: factory(i) for i in range(num_workers)}

    pool = CPUWorkerPool(calcers=calcers, num_workers=num_workers)

    inputs_history = []
    async with pool:
        for _ in range(10):
            # Variable-sized batch with variable-sized inputs
            batch_size = random.randint(2, 100)
            lens = torch.randint(3, [batch_size], dtype=torch.int)
            inputs = {
                str(i): [
                    torch.randint(1 << 20, [sum(lens)], dtype=torch.long),
                    lens
                ]
                for i in range(num_embeddings)
            }
            targets = torch.randn(batch_size)
            inputs_history.append(inputs)
            await pool.assign_job(inputs=inputs, targets=targets)

    for i, embed in model.embeddings.items():
        tensors = [x[i][0] for x in inputs_history]
        new_features = set(torch.cat(tensors, 0).tolist())
        assert embed.size() == len(new_features)

    # no new features
    for embed in itertools.chain(model.no_grad_embeddings.values(), model.not_updated_embeddings.values()):
        assert embed.size() == 0

    # multiple embeeding has ALL features
    tensors = sum([[x[str(i)][0] for x in inputs_history] for i in range(num_embeddings)], [])
    new_features = set(torch.cat(tensors, 0).tolist())
    assert model.multiply_used_embedding.size() == len(new_features)
