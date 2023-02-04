# coding=utf-8
import pytest

from collections import namedtuple

import irt.multik.common as common

Banner = namedtuple('Banner', ['title', 'body', 'href', 'bid', 'pid', 'cid'])


def test_force_bytes():
    assert common.force_bytes('123') == b'123'
    assert common.force_bytes('test') == b'test'
    assert common.force_bytes('тест') == b'\xd1\x82\xd0\xb5\xd1\x81\xd1\x82'
    assert common.force_bytes(u'тест') == b'\xd1\x82\xd0\xb5\xd1\x81\xd1\x82'
    assert common.force_bytes(b'123') == b'123'
    assert common.force_bytes(None) == b'None'
    assert common.force_bytes({'a': True}) == b"{'a': True}"


def test_force_text():
    assert common.force_text(b'123') == '123'
    assert common.force_text(b'test') == 'test'
    assert common.force_text(b'\xd1\x82\xd0\xb5\xd1\x81\xd1\x82') == u'тест'
    assert common.force_text('123') == '123'
    assert common.force_text(None) == 'None'
    assert common.force_text({'a': True}) == "{'a': True}"


banner_data = {'title': 'title_value', 'body': 'body_value', 'href': 'href_value', 'bid': 123, 'pid': 456, 'cid': 789}


def test_bannerhash():
    answer = 'dbfdb7237319c27261dee61b145e82c4'
    assert common.bannerhash(banner_data) == answer
    assert common.bannerhash(Banner(**banner_data)) == answer
    assert common.bannerhash(**banner_data) == answer
    assert common.bannerhash(title=banner_data['title'], body=banner_data['body'], href=banner_data['href']) == answer
    assert common.bannerhash(body=banner_data['body'], href=banner_data['href'], title=banner_data['title']) == answer
    assert common.bannerhash(body=banner_data['body'], href=banner_data['href'], title=banner_data['title'], extra='value') == answer

    with pytest.raises(ValueError):
        common.bannerhash('123')
    with pytest.raises(ValueError):
        common.bannerhash('123', '123')
    with pytest.raises(ValueError):
        common.bannerhash('123', title='123')
    with pytest.raises(ValueError):
        common.bannerhash(body=banner_data['body'], href=banner_data['href'], extra='value')
    with pytest.raises(ValueError):
        common.bannerhash(body=banner_data['body'], extra='value')


def test_banner_uid():
    answer = '303afa47adc42a3aa87fcfedbbdf2652'
    assert common.banner_uid(banner_data) == answer
    assert common.banner_uid(Banner(**banner_data)) == answer
    assert common.banner_uid(**banner_data) == answer
    assert common.banner_uid(bid=banner_data['bid'], cid=banner_data['cid'], pid=banner_data['pid']) == answer
    assert common.banner_uid(cid=banner_data['cid'], bid=banner_data['bid'], pid=banner_data['pid']) == answer
    assert common.banner_uid(cid=banner_data['cid'], bid=banner_data['bid'], pid=banner_data['pid'], extra='value') == answer

    with pytest.raises(ValueError):
        common.banner_uid('123')
    with pytest.raises(ValueError):
        common.banner_uid('123', '123')
    with pytest.raises(ValueError):
        common.banner_uid('123', bid='123')
    with pytest.raises(ValueError):
        common.banner_uid(bid=banner_data['bid'], cid=banner_data['cid'], extra='value')
    with pytest.raises(ValueError):
        common.banner_uid(cid=banner_data['cid'], extra='value')
