# -*- coding: utf-8 -*-

import datetime
import contextlib
import json

import pytest
import httpretty
import mock
import hamcrest

from balance import mapper
from balance import scheme
from cluster_tools import fps_banks_sync

from tests import object_builder as ob

URL = 'https://fake_balalayka_url.ru/api/proxy/getfpsbanks/'


@pytest.fixture(autouse=True)
def prepare_cfg(session):
    session.execute("""
        MERGE INTO bo.t_config c
        USING (
            SELECT
            'FPS_BANKS_SYNC_CONFIG' item,
               Q'{{
                   "url": "%s",
                   "src_tvm_alias": "yb-medium",
                   "dst_tvm_alias": "balalayka",
                   "banks": {
                       "raiffeisen": {
                           "front_id": 10,
                           "bik": "0066"
                       }
                   }
               }}' value
            FROM dual
        ) d
        ON (d.item = c.item)
        WHEN MATCHED THEN UPDATE SET c.VALUE_JSON = d.value
        WHEN NOT MATCHED THEN INSERT (item, VALUE_JSON) VALUES (d.item, d.value)
    """ % URL)

@httpretty.activate
@mock.patch('cluster_tools.fps_banks_sync.get_service_ticket', return_value='ayayayayayyaya')
def test_fps_bank_sync(_mock_tvm, session):
    session.execute(scheme.fps_bank.delete())
    balalayka_resp = {
        'data': [
            {u'name': u'Покемонячий банк', u'alias': u'pokemon'},
            {u'name': u'Трехлитровый банк', u'alias': u'3L'},
            {u'name': u'Банк под отзыв лицензии', u'alias': u'bb_bank'}
        ],
        'errors': None,
    }

    httpretty.register_uri(
        httpretty.POST,
        URL,
        json.dumps(balalayka_resp)
    )

    fps_banks_sync.do_sync(session)
    banks = session.query(mapper.FPSBank).all()

    hamcrest.assert_that(
        banks,
        hamcrest.contains_inanyorder(*[
            hamcrest.has_properties(
                {
                    u'front_id': 10,
                    u'processing_bank': u'raiffeisen',
                    u'cc': dct[u'alias'],
                    u'name': dct[u'name'],
                    u'hidden': 0,
                }
            )
            for dct in balalayka_resp['data']]
        )
    )

    deleted_bank = balalayka_resp['data'].pop()
    httpretty.register_uri(
        httpretty.POST,
        URL,
        json.dumps(balalayka_resp)
    )
    fps_banks_sync.do_sync(session)
    banks = session.query(mapper.FPSBank).all()

    hamcrest.assert_that(
        banks,
        hamcrest.contains_inanyorder(*[
            hamcrest.has_properties(
                {
                    u'front_id': 10,
                    u'processing_bank': u'raiffeisen',
                    u'cc': dct[u'alias'],
                    u'name': dct[u'name'],
                    u'hidden': int(dct[u'alias'] == deleted_bank[u'alias']),
                }
            )
            for dct in balalayka_resp['data'] + [deleted_bank]]
        )
    )
