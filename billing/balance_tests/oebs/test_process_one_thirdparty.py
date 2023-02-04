# -*- coding: utf-8 -*-
import cx_Oracle
import datetime
import mock
import pytest
import hamcrest
import json
from balance import muzzle_util as ut
from balance.queue_processor import QueueProcessor
from tests import object_builder as ob
from tests.balance_tests.oebs.common import check_export_obj


@pytest.mark.parametrize('code', [12541, 12514, 666])
def test_defer_when_db_error(session, code):
    contract = ob.ContractBuilder.construct(session)
    transaction = ob.ThirdPartyTransactionBuilder.construct(session, contract=contract)
    db_exc = cx_Oracle.DatabaseError()
    db_exc.args = (mock.MagicMock(code=code, message='OEBS не доступен'),)
    mf = mock.MagicMock()
    mf.return_value.dbhelper.create_raw_connection = mock.MagicMock(side_effect=db_exc)
    patch_g_ap = mock.patch('balance.processors.oebs.utils.getApplication', mf)
    with patch_g_ap:
        QueueProcessor('OEBS').process_one(transaction.exports['OEBS'])
    session.flush()
    delay = (session.now() + datetime.timedelta(minutes=1)).replace(microsecond=0)
    if code in (12541, 12514):
        check_export_obj(transaction.exports['OEBS'],
                         state=0,
                         output=u'OEBS DatabaseError \'OEBS не доступен\' was thrown, defer export',
                         error='Retrying OEBS_API processing',
                         input=None,
                         rate=0,
                         next_export=hamcrest.greater_than_or_equal_to(delay))
    else:
        check_export_obj(transaction.exports['OEBS'],
                         state=0,
                         output=None,
                         error=ut.uni(db_exc),
                         input=None,
                         rate=1,
                         next_export=hamcrest.greater_than_or_equal_to(delay))


@mock.patch('balance.processors.oebs.dao.oebs.OebsDao')
@pytest.mark.parametrize('oebs_msg, is_deferred',
                         [(u'11 Ошибка - невозможно найти договор в OeBS, billing_contract_id: 11', True),
                          (u'11Ошибка - невозможно найти договор в OeBS, billing_contract_id: 11', False)])
def test_defer_when_oebs_error(mock_dao, session, oebs_msg, is_deferred):
    session.config.__dict__['THIRDPARTY_OEBS_EXPORT_DELAYABLE_ERRORS'] = [
        [u"\d+ Ошибка - невозможно найти договор в OeBS, billing_contract_id: \d+", 60],
        [u"В схеме платежей CPA в OeBS идут профилактические работы. Система будет доступна через некоторое время.",
         60]]
    contract = ob.ContractBuilder.construct(session)
    transaction = ob.ThirdPartyTransactionBuilder.construct(session, contract=contract)
    patch_get_firm_cursor = mock.patch('balance.processors.oebs.get_firm_cursor')
    with patch_get_firm_cursor as patch_cursor:
        out_cursor_mock = mock.MagicMock()
        code = mock.MagicMock()
        code.getvalue.return_value = 12
        msg = mock.MagicMock()
        msg.getvalue.return_value = oebs_msg
        out_cursor_mock.var = mock.MagicMock(side_effect=[code, msg])
        patch_cursor.return_value.__enter__.return_value = [out_cursor_mock, 2]
        QueueProcessor('OEBS').process_one(transaction.exports['OEBS'])
        session.flush()
        if is_deferred:
            check_export_obj(transaction.exports['OEBS'],
                             state=0,
                             output=None,
                             error=u'Export has been deferred: {}'.format(oebs_msg),
                             input=None,
                             rate=0,
                             next_export=hamcrest.greater_than(session.now() + datetime.timedelta(minutes=49)))
        else:
            check_export_obj(transaction.exports['OEBS'],
                             state=0,
                             output=None,
                             error=u'Code: 12, Message: 11Ошибка - невозможно найти договор в OeBS, billing_contract_id: 11',
                             input=None,
                             rate=1,
                             next_export=hamcrest.less_than(session.now() + datetime.timedelta(minutes=8)))
