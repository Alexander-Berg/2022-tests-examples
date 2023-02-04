from typing import Any
import asyncio
import datetime
import dataclasses
from ads_pytorch.online_learning.production.callbacks.lag_callback import (
    create_lag_callbacks,
    AbstractMetricSenderByTimestamp,
    AbstractMetricSenderByLogDate
)
from ads_pytorch.online_learning.production.uri import ProdURI
from ads_pytorch.online_learning.production.dataset.datetime_dataset import DatetimeURI
import pytest


class MyLagSenderByLogDate(AbstractMetricSenderByLogDate):
    def __init__(self):
        self.proc_time = None
        self.date = None

    async def send(self, processing_time: float, log_date: datetime.datetime):
        self.proc_time = processing_time
        self.date = log_date


class MyLagSenderByTimestamp(AbstractMetricSenderByTimestamp):
    def __init__(self):
        self.lag = []
        self.processing_time = []

    async def send(self, lag: float, processing_time: float):
        self.lag.append(lag)
        self.processing_time.append(processing_time)


@pytest.mark.asyncio
async def test_properties():
    sender_by_log_date = MyLagSenderByLogDate()
    sender_by_timestamp = MyLagSenderByTimestamp()
    lag_baseline = datetime.datetime.now()

    start, finish = create_lag_callbacks(
        sender_by_timestamp=sender_by_timestamp,
        sender_by_log_date=sender_by_log_date,
        lag_baseline=lag_baseline
    )
    assert start.call_before_train
    assert not start.call_after_train

    assert not finish.call_before_train
    assert finish.call_after_train


@pytest.mark.asyncio
async def test_send_value():
    sender_by_log_date = MyLagSenderByLogDate()
    sender_by_timestamp = MyLagSenderByTimestamp()
    lag_baseline = datetime.datetime.now()

    start, finish = create_lag_callbacks(
        sender_by_timestamp=sender_by_timestamp,
        sender_by_log_date=sender_by_log_date,
        lag_baseline=lag_baseline,
        frequency=0.1
    )

    uri_date = datetime.datetime.now() - datetime.timedelta(hours=1)
    uri = DatetimeURI(uri="1234", date=uri_date)
    await start(None, None, None, ProdURI(uri=uri))
    await asyncio.sleep(1)
    await finish(None, None, None, ProdURI(uri=uri))

    assert sender_by_log_date.date == uri_date
    assert round(sender_by_log_date.proc_time) == 1

    assert len(sender_by_timestamp.processing_time) > 9
    assert sender_by_timestamp.lag[-1] - sender_by_timestamp.lag[0] < 1


class ToCatch(Exception):
    pass


@dataclasses.dataclass
class ExceptionSenderByLogDate(AbstractMetricSenderByLogDate):
    called: bool = False

    async def send(self, processing_time: float, log_date: datetime.datetime):
        self.called = True
        raise ToCatch


@dataclasses.dataclass
class ExceptionSenderByTimestamp(AbstractMetricSenderByTimestamp):
    called: bool = False

    async def send(self, lag: float, processing_time: float):
        self.called = True
        raise ToCatch


@pytest.mark.asyncio
async def test_by_log_date_exception():
    sender_by_log_date = ExceptionSenderByLogDate()
    sender_by_timestamp = MyLagSenderByTimestamp()
    lag_baseline = datetime.datetime.now()

    start, finish = create_lag_callbacks(
        sender_by_timestamp=sender_by_timestamp,
        sender_by_log_date=sender_by_log_date,
        lag_baseline=lag_baseline,
        frequency=0.1
    )

    uri_date = datetime.datetime.now() - datetime.timedelta(hours=1)
    uri = DatetimeURI(uri="1234", date=uri_date)
    await start(None, None, None, ProdURI(uri=uri))
    await asyncio.sleep(0.3)
    with pytest.raises(ToCatch):
        await finish(None, None, None, ProdURI(uri=uri))

    assert sender_by_log_date.called


@pytest.mark.asyncio
async def test_by_timestamp_exception():
    sender_by_log_date = MyLagSenderByLogDate()
    sender_by_timestamp = ExceptionSenderByTimestamp()
    lag_baseline = datetime.datetime.now()

    start, finish = create_lag_callbacks(
        sender_by_timestamp=sender_by_timestamp,
        sender_by_log_date=sender_by_log_date,
        lag_baseline=lag_baseline,
        frequency=0.1
    )

    uri_date = datetime.datetime.now() - datetime.timedelta(hours=1)
    uri = DatetimeURI(uri="1234", date=uri_date)
    await start(None, None, None, ProdURI(uri=uri))
    await asyncio.sleep(0.3)
    with pytest.raises(ToCatch):
        await finish(None, None, None, ProdURI(uri=uri))

    assert sender_by_timestamp.called
