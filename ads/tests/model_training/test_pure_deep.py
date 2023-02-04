import torch
import pytest
from typing import Generator, List
from pytorch_embedding_model import (
    MinibatchRecord,
    EmbeddingModelInput,
    EmbeddingModelOutput,
)
from ._test_lib import ITestModelFactory, train_pure_deep_model


RECORDS_COUNT = 1000
BATCH_SIZE = 128
SINGLE_FEATURE_SIZE = 256
RV1_SIZE = 13
RV2_SIZE = 26
RV3_SIZE = 39


def _same_batch_data_iterator() -> Generator[MinibatchRecord, None, None]:
    input_example = torch.rand(BATCH_SIZE, SINGLE_FEATURE_SIZE)
    targets_sample = torch.randint(0, 2, (BATCH_SIZE, 1)).float()
    for _ in range(RECORDS_COUNT):
        yield MinibatchRecord(
            inputs=EmbeddingModelInput(embeddings={}, external={"feature": input_example}),
            targets=targets_sample,
            # just for pretty-printing of training speed
            # default logger may try to automatically infer object count by first dim
            # but we like better
            object_count=torch.tensor(BATCH_SIZE)
        )


def _different_batch_data_iterator() -> Generator[MinibatchRecord, None, None]:
    for _ in range(RECORDS_COUNT):
        yield MinibatchRecord(
            inputs=EmbeddingModelInput(
                embeddings={},
                external={"feature": torch.rand(BATCH_SIZE, SINGLE_FEATURE_SIZE)}
            ),
            targets=torch.randint(0, 2, (BATCH_SIZE, 1)).float(),
            # just for pretty-printing of training speed
            # default logger may try to automatically infer object count by first dim
            # but we like better
            object_count=torch.tensor(BATCH_SIZE)
        )


def _multiple_batch_multiple_feature_iterator() -> Generator[MinibatchRecord, None, None]:
    for _ in range(RECORDS_COUNT):
        rv1 = torch.empty(BATCH_SIZE, RV1_SIZE)
        torch.nn.init.normal_(rv1)

        rv2 = torch.empty(BATCH_SIZE, RV2_SIZE)
        torch.nn.init.normal_(rv2, mean=-1, std=5)

        rv3 = torch.empty(BATCH_SIZE, RV3_SIZE)
        torch.nn.init.normal_(rv3, mean=1, std=2)

        yield MinibatchRecord(
            inputs=EmbeddingModelInput(
                embeddings={},
                external={
                    "rv1": rv1,
                    "rv2": rv2,
                    "rv3": rv3
                }
            ),
            targets=torch.randint(0, 2, (BATCH_SIZE, 1)).float(),
            # just for pretty-printing of training speed
            # default logger may try to automatically infer object count by first dim
            # but we like better
            object_count=torch.tensor(BATCH_SIZE)
        )


@pytest.fixture(
    params=[
        "SingleBatchSingleFeature",
        "MultipleBatchSingleFeature",
        "MultipleBatchMultipleFeature"
    ]
)
def data_iterator_and_size(request):
    if request.param == "SingleBatchSingleFeature":
        return _same_batch_data_iterator(), SINGLE_FEATURE_SIZE
    if request.param == "MultipleBatchSingleFeature":
        return _different_batch_data_iterator(), SINGLE_FEATURE_SIZE
    if request.param == "MultipleBatchMultipleFeature":
        return _multiple_batch_multiple_feature_iterator(), RV1_SIZE + RV2_SIZE + RV3_SIZE
    raise ValueError(f"Unknown param {request.param}")


class FeedforwardModelWrapper(torch.nn.Module):
    def __init__(self, deep: torch.nn.Module):
        super(FeedforwardModelWrapper, self).__init__()
        self.deep = deep

    def forward(self, inputs: EmbeddingModelOutput):
        tensors = list(inputs.embeddings.values()) + list(inputs.external.values())
        catted = torch.cat(tensors, dim=-1)
        return self.deep(catted)


@pytest.mark.parametrize("single_optimizer", [True, False], ids=["SingleOptim", "SeveralOptim"])
def test_train_model(
    device_count,
    data_iterator_and_size,
    single_optimizer
):
    data_iterator, input_size = data_iterator_and_size

    class Factory(ITestModelFactory):
        def create_model(self) -> torch.nn.Module:
            return FeedforwardModelWrapper(torch.nn.Sequential(
                torch.nn.Linear(input_size, 128),
                torch.nn.ReLU(),
                torch.nn.Linear(128, 64),
                torch.nn.ReLU(),
                torch.nn.Linear(64, 1)
            ))

        def create_optimizers(self, model: FeedforwardModelWrapper) -> List[torch.optim.Optimizer]:
            if single_optimizer:
                return [torch.optim.Adam(model.parameters())]
            else:
                # not different param groups, but different optimizers, it's important
                layer0_params = model.deep[0].parameters()
                layer1_params = model.deep[2].parameters()
                layer2_params = model.deep[4].parameters()
                return [
                    torch.optim.SGD(layer0_params, lr=0.0001),
                    torch.optim.Adam(layer1_params, lr=0.01),
                    torch.optim.RMSprop(layer2_params, lr=0.03)
                ]

        def create_loss(self) -> torch.nn.Module:
            return torch.nn.BCEWithLogitsLoss()

    train_pure_deep_model(
        model_factory=Factory(),
        data_iterator=data_iterator,
        device_count=device_count
    )
