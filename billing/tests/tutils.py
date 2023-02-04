# coding: utf-8
import contextlib
import mock
import datetime
import hamcrest as hm
from dateutil.relativedelta import relativedelta
import xml.etree.ElementTree as ET
import xmlrpclib

from butils.dbhelper.session import SessionBase


def shift_date(dt=None, years=0, months=0, days=0, hours=0, minutes=0, seconds=0):
    dt = dt or datetime.datetime.now()
    return dt + relativedelta(years=years, months=months, days=days, hours=hours, minutes=minutes, seconds=seconds)


def get_exception_code(exc, tag_name='code'):
    if isinstance(exc, xmlrpclib.Fault):
        try:
            exc_text = ET.fromstring(exc.faultString.encode('utf-8'))
            exc_code = exc_text.find(tag_name).text
            return exc_code
        except ET.ParseError:
            return exc
    else:
        try:
            exc = ET.fromstring(exc)
            exc_code = exc.find(tag_name).text
            return exc_code
        except Exception as e:
            return exc


@contextlib.contextmanager
def mock_transactions():
    base_begin = SessionBase.begin

    def patch_begin(self, *args, **kwargs):
        kwargs['nested'] = True
        return base_begin(self, *args, **kwargs)

    with mock.patch('butils.dbhelper.session.SessionBase.begin', patch_begin):
        yield


def has_exact_entries(data_dict=None, **kwargs):
    data_dict = data_dict or {}
    data_dict.update(kwargs)
    return hm.all_of(
        hm.has_length(len(data_dict)),
        hm.has_entries(data_dict),
    )


class TestsError(Exception):
    pass
