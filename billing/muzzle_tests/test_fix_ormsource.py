# -*- coding: utf-8 -*-

"""
This test case shows that certain vulnerability is fixed (BALANCE-31313)

The vulnerability was a code injection introduced by passing fragments of web request's
query string into Python's eval()

    # This worked before the fix
    CODE="id==None if open('/tmp/1', 'w').write('1') else obj.id==None"
    URLENCODED=$( python -c "import urllib; print urllib.quote('''$CODE''')" )
    QUERY="method=GetSource&name=contract_list&filter=$URLENCODED"
    curl \
        --cookie "$MY_BROWSER_COOKIE" \
        -H "X-Requested-With: XMLHttpRequest" \
        "$MUZZLE_URL/ajax-helper.xml?$QUERY"

"""

import sys

import mock
import pytest

from balance import mapper
from balance.exc import EXCEPTION
from balance.webcontrols.sources import ORMSource, BaseSource
from muzzle.ajax.contract import GetSource


class TestFixORMSourceCodeInjection(object):

    @pytest.mark.parametrize(
        'code',
        [
            "__class__.__new__(a=1 if setattr(__import__('sys'), 'foo', 43) else 2)",
            "item=='A' if setattr(__import__('sys'), 'foo', 43) else obj.item=='A'",
            "__class__.__new__(a=1 if setattr(__import__('sys'), 'foo', 43) else 2)=='A' if setattr(__import__('sys'), 'foo', 43) else obj.item=='A'",
        ],
    )
    def test_bad_orm_criteria(self, session, code):
        sys.foo = 42
        ORMSource(
            "my_orm_source",
            ormobject=mapper.Config,
            attributes={
                "item": "item",
            },
        )
        request_obj_mock = mock.Mock()
        request_obj_mock.getHeaderIn.return_value = "XMLHttpRequest"
        state_obj_mock = mock.Mock()
        state_obj_mock.getParam.return_value = "GET"
        with pytest.raises(EXCEPTION, match=r"Malformed ORM criterion"):
            GetSource(
                session,
                "my_orm_source",
                filter=code,
                state_obj=state_obj_mock,
                request_obj=request_obj_mock,
            )
        assert sys.foo == 42

    def test_good_orm_criteria_comparison(self, session):
        code = "item=='A'"
        ORMSource(
            "my_orm_source",
            ormobject=mapper.Config,
            attributes={
                "item": "item",
            },
        )
        request_obj_mock = mock.Mock()
        request_obj_mock.getHeaderIn.return_value = "XMLHttpRequest"
        state_obj_mock = mock.Mock()
        state_obj_mock.getParam.return_value = "GET"
        try:
            GetSource(
                session,
                "my_orm_source",
                filter=code,
                state_obj=state_obj_mock,
                request_obj=request_obj_mock,
            )
        except Exception as exc:
            pytest.fail("Unexpected error [%s]" % exc)

    def test_good_orm_criteria_call(self, session):
        code = "item.like('B')"
        ORMSource(
            "my_orm_source",
            ormobject=mapper.Config,
            attributes={
                "item": "item",
            },
        )
        request_obj_mock = mock.Mock()
        request_obj_mock.getHeaderIn.return_value = "XMLHttpRequest"
        state_obj_mock = mock.Mock()
        state_obj_mock.getParam.return_value = "GET"
        try:
            GetSource(
                session,
                "my_orm_source",
                filter=code,
                state_obj=state_obj_mock,
                request_obj=request_obj_mock,
            )
        except Exception as exc:
            pytest.fail("Unexpected error [%s]" % exc)

    def test_good_orm_criteria_multiple(self, session):
        code = "item=='A'&item.like('B')"
        ORMSource(
            "my_orm_source",
            ormobject=mapper.Config,
            attributes={
                "item": "item",
            },
        )
        request_obj_mock = mock.Mock()
        request_obj_mock.getHeaderIn.return_value = "XMLHttpRequest"
        state_obj_mock = mock.Mock()
        state_obj_mock.getParam.return_value = "GET"
        try:
            GetSource(
                session,
                "my_orm_source",
                filter=code,
                state_obj=state_obj_mock,
                request_obj=request_obj_mock,
            )
        except Exception as exc:
            pytest.fail("Unexpected error [%s]" % exc)

    @pytest.mark.parametrize(
        'code',
        [
            "name=(setattr(__import__('sys'), 'foo', 43) or 'A')",
            "name=__class__.__new__(a=1 if setattr(__import__('sys'), 'foo', 43) else 2)",
        ],
    )
    def test_bad_orm_kwarg(self, session, code):

        # markg@: I didn't manage to exploit `useparams` from web, but it still smelled

        sys.foo = 42
        ORMSource(
            "my_orm_source",
            ormobject=mapper.ActivityType,
            attributes={
                "name": "name",
            },
            useparams=1,  # use .filter_by() instead of .filter()
        )
        with pytest.raises(EXCEPTION, match=r"Malformed ORM kwarg"):
            base_source = BaseSource.get(
                "my_orm_source",
                sourceparams="params=%s" % code,
            )
            # base_source.source(session)
        assert sys.foo == 42

    def test_good_orm_kwarg(self, session):
        code = "name='A'"
        ORMSource(
            "my_orm_source",
            ormobject=mapper.ActivityType,
            attributes={
                "name": "name",
            },
            useparams=1,
        )
        try:
            base_source = BaseSource.get(
                "my_orm_source",
                sourceparams="params=%s" % code,
            )
            base_source.source(session)
        except Exception as exc:
            pytest.fail("Unexpected error [%s]" % exc)

    def test_good_orm_kwarg_multiple(self, session):
        code = "name='A'&hidden=666"
        ORMSource(
            "my_orm_source",
            ormobject=mapper.ActivityType,
            attributes={
                "name": "name",
            },
            useparams=1,
        )
        try:
            base_source = BaseSource.get(
                "my_orm_source",
                sourceparams="params=%s" % code,
            )
            base_source.source(session)
        except Exception as exc:
            pytest.fail("Unexpected error [%s]" % exc)


# vim:ts=4:sts=4:sw=4:tw=88:et:
