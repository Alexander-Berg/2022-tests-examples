import torch
import copy
import itertools
import os
import numpy as np
from typing import Generator, List, Iterable
from pytorch_embedding_model import (
    MinibatchRecord,
    MinibatchPoolFactory,
    EmbeddingModel,
    create_optimizer,
    EmbeddingModelOutput,
    train_on_data_iterator,
    CommonMinibatchPoolOptions,
    deepspeed_singlehost_world_descriptor
)


def unique_ipv6_port():
    # 1024 to 49151 are "user" ports in linux
    # FIXME: generally unreliable scheme, but when pytest-xdist launches multiple processes
    # for tests, it will launch them simultaneously and they will get almost sequential
    # process ID's (resulting in different remainders). It's almos impossible
    # to launch 49151 additional processes between xdist launches to get same port
    return os.getpid() % (49151 - 1024) + 1024


class ITestModelFactory:
    def create_model(self) -> torch.nn.Module:
        raise NotImplementedError

    def create_optimizers(self, model: torch.nn.Module) -> List[torch.optim.Optimizer]:
        raise NotImplementedError

    def create_loss(self) -> torch.nn.Module:
        raise NotImplementedError


def emulate_allreduce_training_in_single_loop(
    model: torch.nn.Module,
    optimizers: List[torch.optim.Optimizer],
    loss: torch.nn.Module,
    worker_count: int,
    data_iterator
) -> List[float]:
    device = torch.device("cuda", 0)
    for n, p in model.named_parameters():
        if p.device != device:
            raise ValueError(f"Expected all parameters to be on 0 GPU, got {n} with {p.device}")
    losses = []

    def _compute(records: List[MinibatchRecord]):
        model.zero_grad(set_to_none=True)
        for batch in records:
            prediction = model(EmbeddingModelOutput(
                embeddings={},
                external={name: value.to(device) for name, value in batch.inputs.external.items()}
            ))
            value = loss(prediction, batch.targets.to(device))
            losses.append(float(value))
            value.backward()
        # average all gradients - allreduce's property
        for p in model.parameters():
            assert p.grad is not None
            p.grad /= worker_count
        for opt in optimizers:
            opt.step()

    current_pack = []
    for record in data_iterator:
        current_pack.append(record)
        if len(current_pack) == worker_count:
            _compute(current_pack)
            current_pack.clear()
    # tail
    if len(current_pack):
        cycle_iterator = itertools.cycle(current_pack[:])
        while len(current_pack) < worker_count:
            current_pack.append(next(cycle_iterator))
        _compute(current_pack)
        current_pack.clear()

    return losses


def allclose_with_pretty_print(list1: List[float], list2: List[float], atol: float = 1e-5, rtol: float = 1e-4):
    if not np.allclose(list1, list2, atol=atol, rtol=rtol):
        # pretty-print error
        losses_rows = []
        losses_rows.append(f"Comparing two arrays with atol = {atol} and rtol = {rtol}")
        for loss1, loss2 in zip(list1, list2):
            if not np.allclose([loss1], [loss2], atol=atol, rtol=rtol):
                losses_rows.append(f"{loss1}, {loss2}   <------ DIFF")
            else:
                losses_rows.append(f"{loss1}, {loss2}")
        raise ValueError("\n".join(losses_rows))


def train_pure_deep_model(
    model_factory: ITestModelFactory,
    data_iterator: Iterable[MinibatchRecord],
    device_count: int,
    atol: float = 1e-5,
    rtol: float = 1e-4
):
    torch.manual_seed(187124)

    deep_model = model_factory.create_model()
    deep_optimizers = model_factory.create_optimizers(deep_model)

    reference_model = copy.deepcopy(deep_model).cuda()
    reference_optimizers = model_factory.create_optimizers(reference_model)

    # "fake"
    embedding_model = EmbeddingModel(embeddings={})
    embedding_optimizer = create_optimizer(embedding_model=embedding_model)

    minibatch_pool = MinibatchPoolFactory(
        embedding_model=embedding_model,
        embedding_optimizer=embedding_optimizer,
        deep_model=deep_model,
        deep_optimizers=deep_optimizers,
        loss=model_factory.create_loss(),
        # When benchmarking against usual non-distributed training, we must use single device
        # only in this case deepspeed is equivalent to usual mode
        world_descriptor=deepspeed_singlehost_world_descriptor(
            master_port=unique_ipv6_port(),
            devices={torch.device("cuda", i) for i in range(device_count)}
        ),
        common_options=CommonMinibatchPoolOptions(gpu_concurrency=1)
    )()

    # to ensure exactly same order of records
    records = list(data_iterator)
    def _repeat_iterator():
        yield from records

    with train_on_data_iterator(data_iterator=_repeat_iterator(), minibatch_pool=minibatch_pool) as out_records:
        losses = [res.compute_result.losses["Loss"] for res in out_records]

    minibatch_pool.shutdown()

    # reference
    reference_losses = emulate_allreduce_training_in_single_loop(
        model=reference_model,
        optimizers=reference_optimizers,
        loss=model_factory.create_loss(),
        worker_count=device_count,
        data_iterator=_repeat_iterator()
    )

    allclose_with_pretty_print(losses, reference_losses, atol=atol, rtol=rtol)
