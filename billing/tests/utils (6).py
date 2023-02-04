# -*- coding: utf-8 -*-

from typing import (
    Any,
)
import math
import itertools
from unittest import mock
from contextlib import contextmanager

import pytest
import hamcrest

from yt.wrapper import (
    YtError,
)

from billing.library.python.logfeller_utils.log_interval import (
    LB_META_ATTR,
)
from billing.library.python.yt_utils.test_utils.utils import (
    create_yt_client,
    create_dyntable,
)
from billing.log_tariffication.py.lib.constants import (
    LOG_TARIFF_META_ATTR,
    LOG_INTERVAL_KEY,
    DYN_TABLE_IS_UPDATING_KEY,
)

from billing.log_tariffication.py.lib.schema import (
    HISTORICAL_AGGREGATES_TABLE_SCHEMA,
)

from billing.log_tariffication.py.tests.constants import (
    BILLABLE_LOG_TABLE_SCHEMA,
)

FLOAT_PRECISION = 0.00000001
SENTINEL = object()


def dict2matcher(d, float_epsilon=0.1):
    """
    Creates hamcrest matcher from a dict.
    Additionally, replaces floats with close_to matchers.
    """
    for k, v in list(d.items()):
        if isinstance(v, float):
            d[k] = hamcrest.close_to(v, float_epsilon)

    return hamcrest.all_of(
        hamcrest.has_length(len(d)),
        hamcrest.has_entries(d),
    )


@contextmanager
def patch_generate_run_id(return_value=None):
    patch_path = 'billing.log_tariffication.py.jobs.common.generate_new_log_interval.utils.meta.generate_run_id'
    with mock.patch(patch_path) as generate_run_id_mock:
        if return_value:
            generate_run_id_mock.return_value = return_value
        yield generate_run_id_mock


def check_node_is_locked(node_path: Any) -> None:
    other_yt_client: Any = create_yt_client()
    exc_match = (
        r'Cannot take "exclusive" lock for node .+ since "exclusive" lock '
        r'is taken by concurrent transaction'
    )
    with pytest.raises(YtError, match=exc_match):
        with other_yt_client.Transaction():
            other_yt_client.lock(node_path)


class OrderDataBuilder(object):
    def __init__(self, effective_order_id=666, consume_counter=None, event_counter=None,
                 state=None, tariff_dt=None, service_id=7, currency_id=666, is_skipped=False,
                 order_id=333, corrections_skipped=False, group_dt=86400,
                 batch=0, batch_cost=0):
        self.id = effective_order_id
        self.effective_order_id = effective_order_id
        self.service_id = service_id
        self.order_id = order_id
        self._consume_counter = itertools.count(1) if consume_counter is None else consume_counter
        self._event_counter = itertools.count(1) if event_counter is None else event_counter
        self.currency_id = currency_id
        self.is_skipped = is_skipped
        self.batch = batch
        self.batch_cost = batch_cost

        self.consumes = []
        self.new_events = []
        self.new_batched_events = []
        self.untariffed_events = []

        self.corrections_untariffed = []
        self.corrections_events = []
        self.corrections_skipped = corrections_skipped

        self.state = state
        self.tariff_dt = tariff_dt
        self.group_dt = group_dt

    def add_consume(self, qty, sum_=SENTINEL, consume_qty=1, consume_sum=1, id_=SENTINEL):
        self.consumes.append(
            {
                'id': next(self._consume_counter) if id_ is SENTINEL else id_,
                'qty': float(qty),
                'sum': float(sum_ if sum_ is not SENTINEL else qty) if sum_ is not None else None,
                'k_qty': float(consume_qty) if consume_qty is not None else None,
                'k_sum': float(consume_sum) if consume_sum is not None else None,
            }
        )
        return self

    def _format_event(self, cost, time_=None):
        event_id = next(self._event_counter)
        return {
            'ServiceID': self.service_id,
            'EffectiveServiceOrderID': self.effective_order_id,
            'LBMessageUID': str(event_id),
            'EventTime': time_ if time_ is not None else event_id,
            'BillableEventCostCur': float(cost),
            'CurrencyID': self.currency_id
        }

    def add_event(self, cost, time_=None):
        self.new_events.append(self._format_event(cost, time_))
        return self

    def add_untariffed_event(self, cost, time_=None):
        self.untariffed_events.append(self._format_event(cost, time_))
        return self

    def add_corrections_untariffed(self, cost, time_=None):
        event = self._format_event(cost, time_)
        event['ServiceOrderID'] = self.order_id
        self.corrections_untariffed.append(event)
        return self

    def _format_batched_event(self, cost, time_=None, batch=0):
        event_id = next(self._event_counter)
        return {
            'ServiceID': self.service_id,
            'EffectiveServiceOrderID': self.effective_order_id,
            'LBMessageUID': str(event_id),
            'EventTime': time_ if time_ is not None else event_id,
            'BillableEventCostCur': float(cost),
            'CurrencyID': self.currency_id,
            'GroupDT': time_ if time_ is not None else event_id,
            'BatchID': batch
        }

    def add_batched_event(self, cost, time_=None, batch=0):
        self.new_batched_events.append(self._format_batched_event(cost, time_, batch))
        return self

    def add_batched_untariffed_event(self, cost, time_=None, batch=0):
        self.untariffed_events.append(self._format_batched_event(cost, time_, batch))
        return self

    def skip(self):
        self.is_skipped = True
        return self

    @property
    def events(self):
        if self.new_batched_events:
            return self.new_batched_events

        data = self.new_events + self.untariffed_events
        if not self.corrections_skipped:
            data += self.corrections_untariffed
        return data

    @property
    def sync_info(self):
        last_event_id = int(self.events[-1]['LBMessageUID']) if self.events else 0
        return {
            'ServiceID': self.service_id,
            'EffectiveServiceOrderID': self.effective_order_id,
            'CurrencyID': self.currency_id,
            'completion_qty_delta': sum(e['BillableEventCostCur'] for e in self.events),
            'state': self.state,
            'tariff_dt': self.tariff_dt if self.tariff_dt is not None else last_event_id + 1,
            'consumes': self.consumes,
            'group_dt': self.group_dt
        }

    @property
    def sync_batched_info(self):
        last_event_id = int(self.events[-1]['LBMessageUID']) if self.events else 0
        return {
            'ServiceID': self.service_id,
            'EffectiveServiceOrderID': self.effective_order_id,
            'GroupDT': self.group_dt,
            'CurrencyID': self.currency_id,
            'completion_qty_delta': sum(e['BillableEventCostCur'] for e in self.events),
            'state': self.state,
            'tariff_dt': self.tariff_dt if self.tariff_dt is not None else last_event_id + 1,
            'consumes': self.consumes,
            'PrefixBatchCost': self.batch_cost
        }


def create_historical_aggregates_dyntable(yt_client, path, log_interval, data, is_updating=False):
    create_dyntable(
        yt_client,
        path,
        HISTORICAL_AGGREGATES_TABLE_SCHEMA,
        [
            {
                'ServiceID': sid,
                'ServiceOrderID': soid,
                'CurrencyID': cid,
                'BillableEventCostCur': float(bc),
                'EventTime': et,
            }
            for sid, soid, cid, bc, et in data
        ],
        {
            LOG_TARIFF_META_ATTR: {
                LOG_INTERVAL_KEY: log_interval.to_meta(),
                DYN_TABLE_IS_UPDATING_KEY: is_updating,
            },
        }
    )


def create_billable_log_table(yt_client, path, log_interval, data):
    yt_client.create(
        'table',
        path,
        attributes={
            LB_META_ATTR: log_interval.to_meta(),
            'schema': BILLABLE_LOG_TABLE_SCHEMA,
        }
    )

    subinterval = next(iter(log_interval.subintervals.values()))
    num_offsets = subinterval.next_offset - subinterval.first_offset
    offset_size = int(math.ceil(float(len(data)) / num_offsets))

    yt_client.write_table(
        path,
        [
            {
                'ServiceID': sid,
                'ServiceOrderID': soid,
                'CurrencyID': cid,
                'BillableEventCostCur': float(bc),
                'EventCost': int(ec),
                'EventTime': tm,
                '_topic_cluster': subinterval.cluster,
                '_topic': subinterval.topic,
                '_partition': subinterval.partition,
                '_offset': subinterval.first_offset + idx // offset_size,
                '_chunk_record_index': 1 + idx % offset_size,
            }
            for idx, (sid, soid, cid, bc, ec, tm) in enumerate(data)
        ]
    )
