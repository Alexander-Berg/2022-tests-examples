# -*- coding: utf-8 -*-

import datetime

from balance.queue_processor import QueueProcessor
from tests.balance_tests.oebs_api.conftest import (mock_post, check_export_obj)

on_dt = datetime.datetime.now().replace(microsecond=0)


def test_wo_person_firms(use_oebs_api, person, firm, service_ticket_mock):
    """
    Плательщик без закешированных фирм переходит в статус 1, без обработки плательщика и
    возвращает соот-ее сообщение.
    """
    export_obj = person.exports['OEBS_API']
    assert person.exports['OEBS_API'].state == 0
    with mock_post(answer={}) as do_call:
        QueueProcessor('OEBS_API').process_one(export_obj)
    assert do_call.call_count == 0
    check_export_obj(export_obj,
                     state=1,
                     output=u'No firms',
                     error=None,
                     input=None,
                     next_export=None)
