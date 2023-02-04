import torch
import pytest
import itertools
from typing import List
from pytorch_embedding_model import (
    MinibatchRecord,
    EmbeddingModelInput,
    EmbeddingModelOutput,
    EmbeddingModel,
    EmbeddingDescriptor,
    create_optimizer,
    MinibatchPoolFactory,
    deepspeed_singlehost_world_descriptor,
    train_on_data_iterator,
    CommonMinibatchPoolOptions
)
from ._test_lib import (
    allclose_with_pretty_print,
    emulate_allreduce_training_in_single_loop,
    unique_ipv6_port
)


class CommonDeepPart(torch.nn.Module):
    def __init__(self):
        super(CommonDeepPart, self).__init__()
        torch.manual_seed(12345)
        self.input_bias = torch.nn.Parameter(torch.rand(96))
        self.deep = torch.nn.Sequential(
            torch.nn.Linear(96, 32),
            torch.nn.ReLU(),
            torch.nn.Linear(32, 16),
            torch.nn.ReLU(),
            torch.nn.Linear(16, 8),
            torch.nn.ReLU(),
            torch.nn.Linear(8, 1)
        )
        for module in self.deep.modules():
            if isinstance(module, torch.nn.Linear):
                torch.nn.init.orthogonal_(module.weight)
                torch.nn.init.normal_(module.bias, std=2e-4)

    def forward(self, concatted_embeds):
        return self.deep(concatted_embeds + self.input_bias)


class ModelForTorchEmbeds(torch.nn.Module):
    def __init__(self):
        super(ModelForTorchEmbeds, self).__init__()
        self.embed1 = torch.nn.EmbeddingBag(
            num_embeddings=100,
            embedding_dim=64,
            sparse=True  # !! only sparse=True will be equivalent for EmbeddingModel
        )

        self.embed2 = torch.nn.EmbeddingBag(
            num_embeddings=10,
            embedding_dim=32,
            sparse=True  # !! only sparse=True will be equivalent for EmbeddingModel
        )

        torch.nn.init.zeros_(self.embed1.weight)
        torch.nn.init.zeros_(self.embed2.weight)

        self.deep = CommonDeepPart()

    def forward(self, input: EmbeddingModelOutput):
        e1_out = self.embed1(input.external["embed1_data"], input.external["embed1_offset"])
        e2_out = self.embed2(input.external["embed2_data"], input.external["embed2_offset"])

        return self.deep(torch.cat([e1_out, e2_out], dim=-1))


class ModelForOurEmbeds(torch.nn.Module):
    def __init__(self):
        super(ModelForOurEmbeds, self).__init__()
        self.deep = CommonDeepPart()

    def forward(self, input: EmbeddingModelOutput):
        catted = torch.cat([input.embeddings["embed1"], input.embeddings["embed2"]], dim=-1)
        return self.deep(catted)


def _build_torch_embed_optim(params, algo_type, algo_kwargs) -> torch.optim.Optimizer:
    if algo_type == "rmsprop":
        return torch.optim.RMSprop(params, **algo_kwargs)
    if algo_type == "adam":
        return torch.optim.SparseAdam(params, **algo_kwargs)
    raise ValueError(f"Unknown torch optimizer {algo_type}")


@pytest.fixture(scope="module")
def common_data():
    batch_size = 64
    # batch_size = 2
    record_count = 1
    epoch_count = 100
    one_epoch = [
        {
            "embed1": (
                torch.randint(0, 100, size=(batch_size,), dtype=torch.int64),
                torch.ones(batch_size, dtype=torch.int32)
            ),
            "embed2": (
                torch.randint(0, 10, size=(batch_size,), dtype=torch.int64),
                torch.ones(batch_size, dtype=torch.int32)
            ),
            "target": torch.randint(0, 2, size=(batch_size, 1)).float()
        }
        for _ in range(record_count)
    ]
    return one_epoch * epoch_count


def train_reference_model(common_data, algo_type, algo_kwargs, device_count) -> List[float]:
    def _sizes_to_cumsum(tensor):
        tensor = tensor.to(torch.int64)
        return torch.cumsum(tensor, dim=-1) - tensor[0]

    def _reference_iterator():
        for r in common_data:
            yield MinibatchRecord(
                inputs=EmbeddingModelInput(
                    embeddings={},
                    external={
                        "embed1_data": r["embed1"][0].to(torch.int64),
                        "embed1_offset": _sizes_to_cumsum(r["embed1"][1]),
                        "embed2_data": r["embed2"][0].to(torch.int64),
                        "embed2_offset": _sizes_to_cumsum(r["embed2"][1]),
                    }
                ),
                targets=r["target"]
            )

    reference_deep_model = ModelForTorchEmbeds().cuda()
    reference_deep_optimizers = [
        torch.optim.Adam(reference_deep_model.deep.parameters()),
        _build_torch_embed_optim(
            itertools.chain(reference_deep_model.embed1.parameters(), reference_deep_model.embed2.parameters()),
            algo_type=algo_type,
            algo_kwargs=algo_kwargs
        )
    ]
    return emulate_allreduce_training_in_single_loop(
        model=reference_deep_model,
        optimizers=reference_deep_optimizers,
        loss=torch.nn.BCEWithLogitsLoss(),
        worker_count=device_count,
        data_iterator=_reference_iterator()
    )


def train_our_model(algo_type, algo_kwargs, device_count, common_data):
    algo_kwargs = {**algo_kwargs, "init_type": "zeros"}
    embedding_model = EmbeddingModel(
        embeddings={
            "embed1": EmbeddingDescriptor(
                features=["embed1"],
                dim=64,
                algo_type=algo_type,
                algo_kwargs=algo_kwargs
            ),
            "embed2": EmbeddingDescriptor(
                features=["embed2"],
                dim=32,
                algo_type=algo_type,
                algo_kwargs=algo_kwargs
            )
        }
    )
    embedding_optimizer = create_optimizer(embedding_model=embedding_model)
    our_deep_model = ModelForOurEmbeds()
    our_deep_optimizer = torch.optim.Adam(our_deep_model.parameters())

    minibatch_pool = MinibatchPoolFactory(
        embedding_model=embedding_model,
        embedding_optimizer=embedding_optimizer,
        deep_model=our_deep_model,
        deep_optimizers=[our_deep_optimizer],
        loss=torch.nn.BCEWithLogitsLoss(),
        world_descriptor=deepspeed_singlehost_world_descriptor(
            master_port=unique_ipv6_port(),
            devices={torch.device("cuda", i) for i in range(device_count)}
        ),
        common_options=CommonMinibatchPoolOptions(gpu_concurrency=1)
    )()

    def _iterator():
        for data in common_data:
            yield MinibatchRecord(
                inputs=EmbeddingModelInput(
                    embeddings={
                        "embed1": data["embed1"],
                        "embed2": data["embed2"]
                    },
                    external={}
                ),
                targets=data["target"]
            )

    with train_on_data_iterator(_iterator(), minibatch_pool) as records:
        result = [r.compute_result.losses["Loss"] for r in records]

    minibatch_pool.shutdown()

    return result


@pytest.mark.parametrize(
    ["algo_type", "algo_kwargs"],
    [
        ("adam", dict(lr=0.01, eps=1e-8))
    ],
    ids=["adam"]
)
def test_embedding_training(algo_type, algo_kwargs, common_data):
    device_count = 1  # different averaging schemas in deepspeed and our embedding model
    torch.manual_seed(1987481)
    our_losses = train_our_model(algo_type, algo_kwargs, device_count, common_data)

    reference_losses = train_reference_model(
        common_data=common_data,
        algo_type=algo_type,
        algo_kwargs=algo_kwargs,
        device_count=device_count
    )

    # light atol because of differences between cuda and cpu evaluation code.
    # Relative remains strong
    allclose_with_pretty_print(our_losses, reference_losses, atol=1e-1, rtol=1e-4)
