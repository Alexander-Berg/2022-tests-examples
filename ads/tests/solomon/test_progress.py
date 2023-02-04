import pytest
import asyncio
import dataclasses
from typing import List

from ads_pytorch.tools.progress import ProgressCallable, ProgressEntity, ProgressLogger
from ads_pytorch.solomon.progress import SolomonLogger, SolomonMetric, SolomonShard, AbstractSolomonMetricSender


class FakeSolomonSender(AbstractSolomonMetricSender):
    def __init__(self, delay: float = 0):
        self._calls = []
        self._delay = delay

    async def send(self, metrics: List[SolomonMetric], shard: SolomonShard):
        await asyncio.sleep(self._delay)
        self._calls.append((metrics, shard))


@dataclasses.dataclass
class _SomeValue:
    x: int = 1
    y: float = 2.5


@pytest.fixture
def sender_and_logger():
    sender = FakeSolomonSender()
    logger_impl = SolomonLogger(
        sender=sender,
        shard=SolomonShard(project="x", cluster="y", service="z"),
        model_yt_dir="//tmp/ahaha/model1"
    )
    logger = ProgressLogger([logger_impl], frequency=0.1)
    return sender, logger


@pytest.mark.asyncio
async def test_logger_logs():
    sender = FakeSolomonSender()
    shard = SolomonShard(project="x", cluster="y", service="z")
    logger_impl = SolomonLogger(
        sender=sender,
        shard=shard,
        model_yt_dir="//tmp/ahaha/model1"
    )
    await logger_impl.log_progress([
        ProgressEntity(name="float", value=0.1),
        ProgressEntity(name="str", value="ahaha"),
        ProgressEntity(name="int", value=25),
        ProgressEntity(name="object", value=_SomeValue(x=5, y=-0.9))
    ])

    await asyncio.sleep(0.1)
    assert len(sender._calls) == 1
    call = sender._calls[0]
    assert call[1] == shard
    assert len(call[0]) == 2
    for metric in call[0]:
        metric: SolomonMetric
        assert metric.labels["model_yt_dir"] == "//tmp/ahaha/model1"
        assert metric.labels["type"] == "progress"
        assert metric.labels["sensor"] in {"float", "int"}
        if metric.labels["sensor"] == "float":
            assert metric.value == 0.1
        elif metric.labels["sensor"] == "int":
            assert metric.value == 25
        else:
            raise ValueError("Unknown sensor :(")


@pytest.mark.asyncio
async def test_logger_aggregate_messages():
    sender = FakeSolomonSender(delay=0.23)
    shard = SolomonShard(project="x", cluster="y", service="z")
    logger_impl = SolomonLogger(
        sender=sender,
        shard=shard,
        model_yt_dir="//tmp/ahaha/model1"
    )
    for i in range(10):
        await logger_impl.log_progress([
            ProgressEntity(name="float", value=round(0.1 * i, 1)),
            ProgressEntity(name="int", value=25 * i)
        ])
        await asyncio.sleep(0.05)

    await asyncio.sleep(1)

    assert all(x[1] == shard for x in sender._calls)
    all_metrics: List[SolomonMetric] = sum([x[0] for x in sender._calls], [])
    assert len(all_metrics) == 20
    floats = [x for x in all_metrics if x.labels["sensor"] == "float"]
    ints = [x for x in all_metrics if x.labels["sensor"] == "int"]
    assert {x.value for x in floats} == {round(0.1 * i, 1) for i in range(10)}
    assert {x.value for x in ints} == {25 * i for i in range(10)}
