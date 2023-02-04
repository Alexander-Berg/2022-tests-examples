# -*- coding: utf-8 -*-
import datetime
import json

import hamcrest
import httpretty
import mock
import pytest

from balance.application import getApplication
from balance.queue_processor import QueueProcessor
from butils import decimal_unit
from tests import object_builder as ob
from tests.balance_tests.oebs.common import check_export_obj

DU = decimal_unit.DecimalUnit

MONTH_BEFORE = datetime.datetime.now() - datetime.timedelta(days=30)
TWO_MONTH_BEFORE = datetime.datetime.now() - datetime.timedelta(days=60)
MONTH_AFTER = datetime.datetime.now() + datetime.timedelta(days=30)


@pytest.mark.usefixtures('httpretty_enabled_fixture')
@pytest.mark.parametrize('error_text, is_delayed', [(u'Договор с указанным GUID \("\d+"\) не найден.', True),
                                               (u'Дог2овор с указанным GUID \("\d+"\) не найден.', False),
                                        ])
def test_defer_regexp(session, error_text, is_delayed, service_ticket_mock):
    session.config.__dict__['CLASSNAMES_EXPORTED_WITH_OEBS_API'] = {'Invoice': 1}
    session.config.__dict__['OEBS_API_EXPORT_DELAYABLE_ERRORS_BY_CLASSNAME'] = {
        'Invoice': [(error_text, 60)]}
    invoice = ob.InvoiceBuilder.construct(session)
    export_obj = invoice.exports['OEBS_API']
    request_id = ob.get_big_number()
    input = {'request_id': request_id}
    export_obj.input = input
    export_obj.rate = 9
    answer = {"status": "ERROR",
              "errors": ["Договор с указанным GUID (\"123345\") не найден."]}
    qp = QueueProcessor('OEBS_API')
    for key in ('Url', 'CloudUrl'):
        httpretty.register_uri(
            httpretty.POST,
            getApplication().get_component_cfg('oebs_api')[key] + 'getStatusBilling',
            json.dumps([answer]))
    qp.process_one(export_obj)
    session.flush()
    if is_delayed:
        check_export_obj(invoice.exports['OEBS_API'],
                         state=0,
                         output=None,
                         error=u'Retrying OEBS_API processing: Договор с указанным GUID ("123345") не найден.',
                         input=None,
                         rate=9,
                         next_export=hamcrest.greater_than(session.now() + datetime.timedelta(minutes=49)))
    else:
        check_export_obj(invoice.exports['OEBS_API'],
                         state=2,
                         output=None,
                         error=u'Error for export check: Договор с указанным GUID ("123345") не найден.',
                         input=None,
                         rate=10,
                         next_export=None)
