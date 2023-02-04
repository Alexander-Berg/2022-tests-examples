# -*- coding: utf-8 -*-

import xmlrpclib
import pytest


def test_ok(session, medium_xmlrpc):
    medium_xmlrpc.QueryCatalog(['t_distribution_products'])


@pytest.mark.parametrize(
    'flag, condition',
    [
        (False, "t_scale_points.scale_code='addappter_common_scale'"),
        (True, '1 = 0'),
    ]
)
def test_ok_condition(session, medium_xmlrpc, flag, condition):
    session.config.__dict__['MEDIUM_QUERY_CATALOG_SKIP_CHECK'] = flag
    medium_xmlrpc.QueryCatalog(['t_scale_points'], condition)


def test_fail_condition(session, medium_xmlrpc):
    session.config.__dict__['MEDIUM_QUERY_CATALOG_SKIP_CHECK'] = False

    with pytest.raises(xmlrpclib.Fault) as exc_info:
        medium_xmlrpc.QueryCatalog(['t_scale_points'], '1=0')

    assert 'unknown sql clause' in exc_info.value.faultString
