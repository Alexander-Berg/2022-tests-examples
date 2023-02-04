# -*- coding: utf-8 -*-

import contextlib
import mock
import pytest

import butils.exc as exc
from balance import mapper
from balance.constants import ExportState
from balance.queue_processor import QueueProcessor
from balance.queue_processor_deco import processors

from tests.object_builder import ClientBuilder


def _success_export(obj):
    return


def _fail_export(obj):
    raise Exception('Айяйяй')


def _deferred_export(obj):
    raise exc.DEFERRED_ERROR()


@contextlib.contextmanager
def patch_export_type_params(session, queue_name, params):
    et = session.query(mapper.ExportType).getone(queue_name)
    old_params = et.params
    et.params = params
    yield
    et.params = old_params


QUEUE_NAME = "SMS_NOTIFY"


def _build_export(session):
    client = ClientBuilder().build(session).obj
    client.enqueue(QUEUE_NAME)

    export_obj = client.exports[QUEUE_NAME]

    assert export_obj.state == ExportState.enqueued
    return export_obj


def test_successful_processing(session):
    export_obj = _build_export(session)
    qp = QueueProcessor(QUEUE_NAME)
    with mock.patch.dict(processors[QUEUE_NAME], {'Client': _success_export}):
        qp.process_one(export_obj)
    assert export_obj.state == ExportState.exported


def test_failure(session):
    export_obj = _build_export(session)
    qp = QueueProcessor(QUEUE_NAME, max_rate=1)
    with mock.patch.dict(processors[QUEUE_NAME], {'Client': _fail_export}):
        qp.process_one(export_obj)

    assert export_obj.state == ExportState.failed


def test_max_deferment_time_default(session):
    export_obj = _build_export(session)
    qp = QueueProcessor(QUEUE_NAME)
    with mock.patch.dict(processors[QUEUE_NAME], {'Client': _deferred_export}):
        qp.process_one(export_obj)

    assert export_obj.state == ExportState.enqueued


def test_max_deferment_time_from_export_type(session):
    params = {"MaxDefermentTime": 0}
    with patch_export_type_params(session, QUEUE_NAME, params):
        export_obj = _build_export(session)
        export_obj.enqueue_dt = None
        qp = QueueProcessor(QUEUE_NAME)
        with mock.patch.dict(processors[QUEUE_NAME], {'Client': _deferred_export}):
            qp.process_one(export_obj)
            session.flush()
            # нет enqueue_dt  - defer проставляет
            assert export_obj.enqueue_dt is not None
            assert export_obj.state == ExportState.enqueued
            # есть enqueue_dt - полноценная логика
            qp.process_one(export_obj)
            assert export_obj.state == ExportState.failed
    session.flush()
