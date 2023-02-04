import torch
import pytest
from pytorch_embedding_model import (
    MinibatchRecord,
    MinibatchPoolFactory,
    EmbeddingModel,
    create_optimizer,
    EmbeddingDescriptor,
    EmbeddingModelInput,
    CommonMinibatchPoolOptions
)
from pytorch_embedding_model.deepspeed.world_constructor import singlehost_world_descriptor
from pytorch_embedding_model.profile_transport import ProfileTransportCalcerBuilder


"""
These modes are necessary only for profiling code speed by developers
In these tests, we don't make any performance tests because it's hard to maintain
Just check that these modes launch and *somehow* work
"""


class DieModel(torch.nn.Module):
    def forward(self, *args, **kwargs):
        raise RuntimeError("This should never be called in profile embedding mode")


@pytest.mark.asyncio
async def test_profile_transport():
    embedding_model = EmbeddingModel(embeddings={
        "x": EmbeddingDescriptor(
            features=["x1", "x2", "x3"],
            dim=64,
            algo_type="rmsprop",
            algo_kwargs=dict(lr=0.01)
        )
    })
    embedding_optimizer = create_optimizer(embedding_model=embedding_model)
    minibatch_pool = MinibatchPoolFactory(
        embedding_model=embedding_model,
        embedding_optimizer=embedding_optimizer,
        deep_model=DieModel(),
        deep_optimizers=[],
        loss=DieModel(),
        deep_calcers_builder=ProfileTransportCalcerBuilder(),
        # may not care about master port - deepspeed won't be launched
        world_descriptor=singlehost_world_descriptor(
            devices={torch.device("cuda", i) for i in range(2)}
        )
    )()

    # No result checking here - just keep it launching and working
    # Profile transport mode is needed only for developers
    async with minibatch_pool:
        for _ in range(20):
            await minibatch_pool.assign_job(MinibatchRecord(
                inputs=EmbeddingModelInput(
                    embeddings={
                        "x1": (torch.LongTensor(list(range(3))), torch.IntTensor([3] * 1)),
                        "x2": (torch.LongTensor(list(range(6))), torch.IntTensor([3] * 2)),
                        "x3": (torch.LongTensor(list(range(9))), torch.IntTensor([3] * 3))
                    },
                    external={
                        "lolrv": torch.rand(10, 10)
                    }
                ),
                targets=torch.rand(1)
            ))

    minibatch_pool.shutdown()


@pytest.mark.asyncio
async def test_profile_embedding():
    embedding_model = EmbeddingModel(embeddings={
        "x": EmbeddingDescriptor(
            features=["x1", "x2", "x3"],
            dim=64,
            algo_type="rmsprop",
            algo_kwargs=dict(lr=0.01)
        )
    })
    embedding_optimizer = create_optimizer(embedding_model=embedding_model)
    minibatch_pool = MinibatchPoolFactory(
        embedding_model=embedding_model,
        embedding_optimizer=embedding_optimizer,
        deep_model=DieModel(),
        deep_optimizers=[],
        loss=DieModel(),
        # may not care about master port - deepspeed won't be launched
        world_descriptor=singlehost_world_descriptor(
            devices={torch.device("cuda", i) for i in range(2)}
        ),
        common_options=CommonMinibatchPoolOptions(profile_embedding_mode=True)
    )()

    # No result checking here - just keep it launching and working
    # Profile transport mode is needed only for developers
    async with minibatch_pool:
        for _ in range(20):
            await minibatch_pool.assign_job(MinibatchRecord(
                inputs=EmbeddingModelInput(
                    embeddings={
                        "x1": (torch.LongTensor(list(range(3))), torch.IntTensor([3] * 1)),
                        "x2": (torch.LongTensor(list(range(6))), torch.IntTensor([3] * 2)),
                        "x3": (torch.LongTensor(list(range(9))), torch.IntTensor([3] * 3))
                    },
                    external={
                        "lolrv": torch.rand(10, 10)
                    }
                ),
                targets=torch.rand(1)
            ))

    minibatch_pool.shutdown()
