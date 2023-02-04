# -*- coding: utf-8 -*-
from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

from future import standard_library

standard_library.install_aliases()

import pytest
import mock
import hamcrest as hm
import http.client as http
from lxml import etree

from yb_snout_api.tests_unit.base import TestCaseApiAppBase


TMP = """<?xml version="1.0" encoding="utf-8"?>
<cat lang="%(lang)s">
  <msg id="3">
    <value>%(text)s</value>
  </msg>
</cat>
"""
RU_TEXT = 'Очень длинный текст оферты'
EN_TEXT = 'Offer very long text'


def parse_file(self, lang):
    if lang.lower() == 'en':
        text = EN_TEXT
    elif lang.lower() == 'ru':
        text = RU_TEXT
    else:
        raise ValueError('Unexpected lang')

    offer_file_body = TMP % {'lang': lang, 'text': text}
    tree = etree.fromstring(offer_file_body.encode('utf-8'))
    self._values_dict = {}
    self._iterate_to_dict(tree)


@mock.patch('balance.multilang_support._CommonManager._parse', parse_file)
@pytest.mark.smoke
class TestOfferText(TestCaseApiAppBase):
    BASE_API = '/v1/paystep/offer'

    @pytest.mark.parametrize('is_admin', [True, False])
    @pytest.mark.parametrize(
        'lang, text',
        [
            pytest.param('ru', RU_TEXT, id='ru'),
            pytest.param('en', EN_TEXT, id='en'),
        ],
    )
    def test_get_offer_text(self, is_admin, lang, text):
        res = self.test_client.get(
            self.BASE_API,
            {'offer_id': 3, 'lang': lang},
            is_admin=is_admin,
        )
        hm.assert_that(res.status_code, hm.equal_to(http.OK))

        hm.assert_that(
            res.get_json()['data'],
            hm.has_entries({
                'offer_id': 3,
                'offer_text': text,
            }),
        )

    def test_offer_not_found(self):
        res = self.test_client.get(
            self.BASE_API,
            {'offer_id': 10, 'lang': 'ru'},
        )
        hm.assert_that(res.status_code, hm.equal_to(http.NOT_FOUND))

        hm.assert_that(
            res.get_json(),
            hm.has_entries({
                'error': 'OFFER_NOT_FOUND',
                'description': 'Offer 10 is not found',
            }),
        )
