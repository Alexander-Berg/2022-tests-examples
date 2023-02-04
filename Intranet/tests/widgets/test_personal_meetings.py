# coding: utf-8

from __future__ import unicode_literals

from cab.widgets import personal_meetings
from cab import utils

from tests.data import wiki_data


SUPERIOR_LOGIN = 'business_cat'
EMPLOYEE_LOGIN = 'janet'
SUPERIOR_AUTH = utils.auth.Auth(login=SUPERIOR_LOGIN)


def test_get_get_first_header_data_from_one_header_page():
    header_data = personal_meetings.get_header_data(
        page_structure=wiki_data.ONE_HEADER_BEMJSON)

    assert isinstance(header_data, dict)
    assert header_data.get('text') == '2016-08-02'
    assert header_data.get('anchor') == '2016-08-02'

    start = header_data.get('text_pos_start')
    end = header_data.get('text_pos_end')

    assert wiki_data.ONE_HEADER_MARKUP[start:end] == wiki_data.FIRST_NOTE


def test_get_get_first_header_data_from_two_header_page():
    header_data = personal_meetings.get_header_data(
        page_structure=wiki_data.TWO_HEADER_BEMJSON)

    assert isinstance(header_data, dict)
    assert header_data.get('text') == '2016-09-02'
    assert header_data.get('anchor') == '2016-09-02'

    start = header_data.get('text_pos_start')
    end = header_data.get('text_pos_end')

    assert wiki_data.TWO_HEADER_MARKUP[start:end] == wiki_data.SECOND_NOTE


def test_get_anchored_header_from_two_header_page():
    header_data = personal_meetings.get_header_data(
        page_structure=wiki_data.TWO_HEADER_BEMJSON,
        anchor='2016-08-02',
    )

    assert isinstance(header_data, dict)
    assert header_data.get('text') == '2016-08-02'
    assert header_data.get('anchor') == '2016-08-02'
    assert header_data.get('text_pos_start') == 50
    assert header_data.get('text_pos_end') == 113
