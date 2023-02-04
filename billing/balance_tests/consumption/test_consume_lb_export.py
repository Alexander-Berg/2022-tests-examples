# -*- coding: utf-8 -*-

import pytest
import hamcrest

import sqlalchemy as sa

from balance import mapper
from balance import scheme
from balance.actions import consumption
from balance.constants import (
    ExportState,
)


@pytest.fixture(params=[False, True], ids=['exporting', 'not_exporting'])
def is_exporting(request, session):
    session.config.__dict__['EXPORT_CONSUMES_LOGBROKER'] = request.param
    return request.param


def set_export_state(consume, state):
    upd = (
        scheme.export_ng.update()
        .where(sa.and_(scheme.export_ng.c.object_id == consume.id,
                       scheme.export_ng.c.type == 'LOGBROKER-CONSUME'))
        .values(state=state)
    )
    consume.session.execute(upd)


def assert_export(consume, state):
    export = (
        consume.session.query(scheme.export_ng)
        .filter_by(object_id=consume.id,
                   type='LOGBROKER-CONSUME')
        .one_or_none()
    )
    if state is None:
        assert export is None
    else:
        hamcrest.assert_that(
            export,
            hamcrest.has_properties(
                state=state
            )
        )


def cr_consume(invoice, order, qty):
    return consumption.consume_order(
        invoice,
        order,
        qty,
        mapper.PriceObject(1, 1),
        mapper.DiscountObj(),
        qty,
    ).consume


def test_new_consume(session, invoice, order, is_exporting):
    consume = cr_consume(invoice, order, 666)
    session.expire_all()

    hamcrest.assert_that(
        consume,
        hamcrest.has_properties(
            version_id=0,
            current_qty=666,
        )
    )
    assert_export(consume, ExportState.enqueued if is_exporting else None)


def test_reverse(session, invoice, order, is_exporting):
    consume = cr_consume(invoice, order, 666)
    set_export_state(consume, ExportState.exported)
    consumption.reverse_consume(consume, None, 333)
    session.expire_all()

    hamcrest.assert_that(
        consume,
        hamcrest.has_properties(
            version_id=0,
            current_qty=333,
        )
    )
    assert_export(consume, ExportState.exported if is_exporting else None)


@pytest.mark.parametrize(
    'values',
    [
        pytest.param({'current_qty': 111}, id='current_qty'),
        pytest.param({'current_sum': 111}, id='current_sum'),
        pytest.param({'completion_qty': 666}, id='completion_qty'),
        pytest.param({'completion_sum': 666}, id='completion_sum'),
        pytest.param({'act_qty': 666}, id='act_qty'),
        pytest.param({'act_sum': 666}, id='act_sum'),
    ]
)
def test_ignored_fields(session, invoice, order, is_exporting, values):
    consume = cr_consume(invoice, order, 666)
    set_export_state(consume, ExportState.exported)

    for k, v in values.items():
        setattr(consume, k, v)
    session.flush()
    session.expire_all()

    hamcrest.assert_that(
        consume,
        hamcrest.has_properties(
            version_id=0,
            **values
        )
    )
    assert_export(consume, ExportState.exported if is_exporting else None)


@pytest.mark.parametrize(
    'values',
    [
        pytest.param({'consume_qty': 111}, id='consume_qty'),
        pytest.param({'consume_sum': 111}, id='consume_sum'),
        pytest.param({'price': 666}, id='price'),
        pytest.param({'discount_obj': mapper.DiscountObj(1)}, id='discount_obj'),
    ]
)
def test_exported_fields(session, invoice, order, is_exporting, values):
    consume = cr_consume(invoice, order, 666)
    set_export_state(consume, ExportState.exported)

    for k, v in values.items():
        setattr(consume, k, v)
    session.flush()
    session.expire_all()

    hamcrest.assert_that(
        consume,
        hamcrest.has_properties(
            version_id=1,
            **values
        )
    )
    assert_export(consume, ExportState.enqueued if is_exporting else None)
