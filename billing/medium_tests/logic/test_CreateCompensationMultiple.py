# -*- coding: utf-8 -*-

from __future__ import with_statement

import datetime
import xmlrpclib
from functools import partial

import mock
import pytest
import hamcrest

from balance.actions.nirvana.task import process_nirvana_task_item
from balance.queue_processor import process_object
from balance.constants import (
    DIRECT_PRODUCT_RUB_ID,
    DIRECT_SERVICE_ID
)
from balance import exc, mapper
from tests import (
    object_builder as ob,
    tutils as tut
)


@pytest.fixture(autouse=True)
def mock_batch_processor(session):
    patch_path = 'balance.util.ParallelBatchProcessor.process_batches'
    session_maker_path = 'butils.dbhelper.helper.DbHelper.create_session'
    calls = []

    def _process_batches(_s, func, batches, **kw):
        calls.append(batches)
        return map(partial(func, **kw), batches)


    with mock.patch(patch_path, _process_batches), mock.patch(session_maker_path, return_value=session):
        yield calls


def test_no_config(xmlrpcserver, session):
    comp_list = [
        {
            'ServiceOrderID': 666,
            'Sum': 666
        }
    ]

    with pytest.raises(xmlrpclib.Fault) as exc_info:
        xmlrpcserver.CreateCompensationMultiple(
            session.oper_id,
            DIRECT_SERVICE_ID,
            comp_list
        )

    hamcrest.assert_that(
        tut.get_exception_code(exc_info.value, 'msg'),
        hamcrest.equal_to(
            'There are no available services/agencies'
        )
    )

    hamcrest.assert_that(
        tut.get_exception_code(exc_info.value, 'code'),
        hamcrest.equal_to(
            'NO_AVAILABLE_SERVICES'
        )
    )


def test_not_allowed_service(xmlrpcserver, session):
    comp_list = []
    agency = ob.ClientBuilder.construct(session, is_agency=1)
    client = ob.ClientBuilder.construct(session, agency=agency)
    session.config.__dict__['CREATE_COMPENSATION_MULTIPLE'] = {
        '666': [client.id]
    }
    for sum_ in range(1, 3):
        o_ = ob.OrderBuilder.construct(
            session,
            service_id=DIRECT_SERVICE_ID,
            client=client,
            agency=agency,
            product_id=DIRECT_PRODUCT_RUB_ID
        )
        comp_list.append(
            {
                'ServiceOrderID': o_.service_order_id,
                'Sum': sum_
            }
        )

    with pytest.raises(xmlrpclib.Fault) as exc_info:
        xmlrpcserver.CreateCompensationMultiple(
            session.oper_id,
            DIRECT_SERVICE_ID,
            comp_list
        )

    hamcrest.assert_that(
        tut.get_exception_code(exc_info.value, 'msg'),
        hamcrest.equal_to(
            'Compensation is not allowed for service_id {}'.format(
                DIRECT_SERVICE_ID
            )
        )
    )

    hamcrest.assert_that(
        tut.get_exception_code(exc_info.value, 'code'),
        hamcrest.equal_to(
            'NOT_ALLOWED_SERVICE'
        )
    )


def test_not_allowed_agency(xmlrpcserver, session):
    comp_list = []
    agency = ob.ClientBuilder.construct(session, is_agency=1)
    client = ob.ClientBuilder.construct(session, agency=agency)
    session.config.__dict__['CREATE_COMPENSATION_MULTIPLE'] = {
        str(DIRECT_SERVICE_ID): [client.id]
    }
    for sum_ in range(1, 3):
        o_ = ob.OrderBuilder.construct(
            session,
            service_id=DIRECT_SERVICE_ID,
            client=client,
            agency=agency,
            product_id=DIRECT_PRODUCT_RUB_ID
        )
        comp_list.append(
            {
                'ServiceOrderID': o_.service_order_id,
                'Sum': sum_
            }
        )

    with pytest.raises(xmlrpclib.Fault) as exc_info:
        xmlrpcserver.CreateCompensationMultiple(
            session.oper_id,
            DIRECT_SERVICE_ID,
            comp_list
        )

    hamcrest.assert_that(
        sorted(tut.get_exception_code(exc_info.value, 'msg')),
        hamcrest.equal_to(
            sorted(('Orders has not allowed agency: %s' %
                    ', '.join(map(str, [i['ServiceOrderID'] for i in comp_list])))
                   )
        )
    )

    hamcrest.assert_that(
        tut.get_exception_code(exc_info.value, 'code'),
        hamcrest.equal_to(
            'NOT_ALLOWED_AGENCY'
        )
    )


def test_success(xmlrpcserver, session):
    comp_list = []
    orders = []
    agency = ob.ClientBuilder.construct(session, is_agency=1)
    client = ob.ClientBuilder.construct(session, agency=agency)
    session.config.__dict__['CREATE_COMPENSATION_MULTIPLE'] = {
        '7': [agency.id]
    }
    for sum_ in range(1, 3):
        o_ = ob.OrderBuilder.construct(
            session,
            service_id=DIRECT_SERVICE_ID,
            client=client,
            agency=agency,
            product_id=DIRECT_PRODUCT_RUB_ID
        )
        comp_list.append(
            {
                'ServiceOrderID': o_.service_order_id,
                'Sum': sum_
            }
        )
        orders.append(o_)

    res = xmlrpcserver.CreateCompensationMultiple(
        session.oper_id,
        DIRECT_SERVICE_ID,
        comp_list
    )

    task = session.query(mapper.NirvanaTask).filter(
        mapper.NirvanaTask.dt > datetime.datetime.now() - datetime.timedelta(0.001)
    ).one()

    hamcrest.assert_that(
        res,
        hamcrest.has_entry(
            'compensation_task_id',
            task.id
        )
    )

    export_objects = session.query(mapper.Export).filter(
        mapper.Export.classname == 'NirvanaTaskItem',
        mapper.Export.type == 'NIRVANA_TASK_ITEM',
        mapper.Export.object_id.in_([i.id for i in task.items])
    ).all()

    hamcrest.assert_that(
        export_objects,
        hamcrest.has_length(2)
    )

    hamcrest.assert_that(
        task.items,
        hamcrest.has_items(
            hamcrest.has_properties(
                output=None,
                processed=0
            )
        )
    )

    for item in export_objects:
        process_nirvana_task_item(item)

    hamcrest.assert_that(
        task.items,
        hamcrest.has_items(
            hamcrest.has_properties(
                output=hamcrest.is_not(None),
                processed=1
            )
        )
    )

    for o_ in orders:
        assert len(o_.consumes) == 1
        q = o_.consumes[0]
        assert q.current_sum.as_decimal() == filter(lambda i: i['ServiceOrderID'] == o_.service_order_id, comp_list)[0]['Sum']
        invoice = q.invoice
        assert invoice.paysys.cc == 'ce'
        assert not invoice.compensation_ticket_id
        assert not invoice.compensation_reason
