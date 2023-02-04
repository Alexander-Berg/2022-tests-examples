# -*- coding: utf-8 -*-

import mock
import cx_Oracle
import sqlalchemy as sa
import pytest
from balance import mapper, exc
from balance.processors.oebs.utils import get_firm_cursor


@pytest.mark.parametrize('code', [12541, 12514, 666])
def test_handle_oebs_db_error(session, code):
    firm = session.query(mapper.Firm).getone(id=1)
    db_exc = cx_Oracle.DatabaseError()
    db_exc.args = (mock.MagicMock(code=code, message='OEBS не доступен'),)
    mf = mock.MagicMock()
    mf.return_value.dbhelper.create_raw_connection = mock.MagicMock(side_effect=db_exc)
    patch_g_ap = mock.patch('balance.processors.oebs.utils.getApplication', mf)
    if code in [12541, 12514]:
        with patch_g_ap, pytest.raises(exc.DEFERRED_ERROR) as exc_info:
            get_firm_cursor(firm).__enter__()
        assert exc_info.value.output == u'OEBS DatabaseError \'OEBS не доступен\' was thrown, defer export'
    else:
        with patch_g_ap, pytest.raises(cx_Oracle.DatabaseError):
            get_firm_cursor(firm).__enter__()


@pytest.mark.parametrize('code', [12541, 12514, 666])
def test_handle_oebs_db_error_sa(session, code):
    firm = session.query(mapper.Firm).getone(id=1)
    db_exc = cx_Oracle.DatabaseError()
    db_exc.args = (mock.MagicMock(code=code, message='OEBS не доступен'),)
    sa_exc = sa.exc.DatabaseError(None, None, db_exc)
    mf = mock.MagicMock()
    mf.return_value.dbhelper.create_raw_connection = mock.MagicMock(side_effect=sa_exc)
    patch_g_ap = mock.patch('balance.processors.oebs.utils.getApplication', mf)
    if code in [12541, 12514]:
        with patch_g_ap, pytest.raises(exc.DEFERRED_ERROR) as exc_info:
            get_firm_cursor(firm).__enter__()
        assert exc_info.value.output == u'OEBS DatabaseError \'OEBS не доступен\' was thrown, defer export'
    else:
        with patch_g_ap, pytest.raises(cx_Oracle.DatabaseError):
            get_firm_cursor(firm).__enter__()

def test_handle_another_error(session):
    firm = session.query(mapper.Firm).getone(id=1)
    mf = mock.MagicMock()
    mf.return_value.dbhelper.create_raw_connection = mock.MagicMock(side_effect=KeyError())
    patch_g_ap = mock.patch('balance.processors.oebs.utils.getApplication', mf)
    with patch_g_ap, pytest.raises(KeyError):
        get_firm_cursor(firm).__enter__()
