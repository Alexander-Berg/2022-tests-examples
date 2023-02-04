import torch
import pytest
from typing import Dict, Union, List

from ads_pytorch.model_calcer.factory import MinibatchPoolFactory

from ads_pytorch.model_calcer.minibatch_record import MinibatchRecord
from ads_pytorch.core.psmodel import BaseParameterServerModule
from ads_pytorch.hash_embedding.hash_embedding import HashEmbedding, create_hash_table
from ads_pytorch.hash_embedding.optim import create_optimizer


SOME_FIELD = 'SomeField'

class DeepModel(torch.nn.Module):
    def __init__(self, in_dim: int = 10, out_dim: int = 10):
        super(DeepModel, self).__init__()
        self.l1 = torch.nn.Linear(in_dim, out_dim, bias=False)
        self.const_bias = torch.nn.Parameter(torch.ones(out_dim), requires_grad=False)

    def forward(self, inputs: Dict[str, torch.Tensor]):
        return self.l1(inputs[SOME_FIELD]) + self.const_bias


@pytest.mark.asyncio
async def test_updated_parameters_cpu_deep_model():
    # 1. Model definition
    model = DeepModel()
    loss = torch.nn.MSELoss()
    optimizer = torch.optim.Adam(model.parameters())

    worker_pool = MinibatchPoolFactory(
        model=model,
        loss=loss,
        deep_optimizers=[optimizer],
        hash_embedding_optimizers=[],
        calcer_results_handlers=[],
        allow_async_gpu_update=False
    )()

    old_params = {name: param.clone() for name, param in model.named_parameters()}

    updated_params = {'l1.weight'}

    # 3. Train model on 1 MinibatchRecord and check parameters
    async with worker_pool:
        future = await worker_pool.assign_job(MinibatchRecord(inputs={SOME_FIELD: torch.randn(10)}, targets=torch.randn(10)))

    for name, parameter in model.named_parameters():
        if name in updated_params:
            assert not torch.allclose(parameter, old_params[name])
        else:
            assert torch.allclose(parameter, old_params[name])


class HashEmbeddingModel(BaseParameterServerModule):
    def __init__(self, in_dim: int = 10, out_dim: int = 10):
        super(HashEmbeddingModel, self).__init__()

        self.embedder = HashEmbedding(create_hash_table(table_type='adam', dim=in_dim))
        self.deep_model = DeepModel(in_dim, out_dim)

    def async_forward(self, inputs: Dict[str, torch.Tensor]) -> Dict[str, torch.Tensor]:
        return {SOME_FIELD: self.embedder(*inputs[SOME_FIELD])}

    def sync_forward(self, inputs: Dict[str, torch.Tensor]):
        return self.deep_model(inputs)


@pytest.mark.asyncio
async def test_updated_parameters_cpu_hash_embedding_model():
    # 1. Model definition
    model = HashEmbeddingModel()
    model.parameter_server_mode = True

    loss = torch.nn.MSELoss()
    deep_optimizer = torch.optim.Adam(model.deep_parameters())
    emb_optimizer = create_optimizer(
        params=model.embedder.parameters(),
        type='adam'
    )

    worker_pool = MinibatchPoolFactory(
        model=model,
        loss=loss,
        deep_optimizers=[deep_optimizer],
        hash_embedding_optimizers=[emb_optimizer],
        calcer_results_handlers=[],
        allow_async_gpu_update=False,
    )()

    old_params = {name: param.clone() for name, param in model.named_deep_parameters()}

    updated_params = {'deep_model.l1.weight'}

    # 3. Train model on 1 MinibatchRecord and check parameters
    for _ in range(10): # 0 gradient on the 1st step
        async with worker_pool:
            future = await worker_pool.assign_job(
                MinibatchRecord(
                    inputs={SOME_FIELD: (torch.randint(10, (5, )), torch.IntTensor([3, 2]), )},
                    targets=torch.randn(10)
                )
            )

    for name, parameter in model.named_deep_parameters():
        if name in updated_params:
            print(name)
            assert not torch.allclose(parameter, old_params[name])
        else:
            assert torch.allclose(parameter, old_params[name])
