# coding: utf-8
import pytest
import yatest

xfail = pytest.mark.xfail(yatest.common.get_param("xfail_should_fail") != "yes", reason="Expected fail")
