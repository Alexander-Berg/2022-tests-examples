from copy import deepcopy
from collections import deque
from datacloud.features.contact_actions.contact_actions_grep import (
    parse_contact_actions,
    match_id_value,
    add_prevs_and_next
)


def test_match_id_value():
    assert (b'79002003040', b'phone') == match_id_value(b'tel:79002003040')
    assert (b'79002003040', b'phone') == match_id_value(b'tel:7 (900) 200-30-40')
    assert (b'79002003040', b'phone') == match_id_value(b'tel:%2B79002003040')
    assert (b'79002003040', b'phone') == match_id_value(b'tel:7-900-200-3040')
    assert (b'79002003040', b'phone') == match_id_value(b'tel:9002003040')
    assert (None, None) == match_id_value(b'tel:7900200304')
    assert (None, None) == match_id_value(b'tel:790020030400')
    assert (b'my@mail.ru', b'email') == match_id_value(b'mailto:my@mail.ru')
    assert (b'my@mail.ru', b'email') == match_id_value(b'my@mail.ru')
    assert (b'foo.bar@mail.ru', b'email') == match_id_value(b'foo.bar@mail.ru')
    assert (b'foo.bar@mail.ru', b'email') == match_id_value(b'email=foo.bar@mail.ru')
    assert (b'79002003040@mail.ru', b'email') == match_id_value(b'mailto:79002003040@mail.ru')


def test_add_prev_and_next():
    assert list(map(deepcopy, add_prevs_and_next([1, 2, 3]))) == [
        (1, deque([]), 2),
        (2, deque([1]), 3),
        (3, deque([2, 1]), None),
    ]
    assert list(map(deepcopy, add_prevs_and_next('abcdef', max_previous_size=3))) == [
        ('a', deque([]), 'b'),
        ('b', deque(['a']), 'c'),
        ('c', deque(['b', 'a']), 'd'),
        ('d', deque(['c', 'b', 'a']), 'e'),
        ('e', deque(['d', 'c', 'b']), 'f'),
        ('f', deque(['e', 'd', 'c']), None),
    ]
    assert list(add_prevs_and_next([])) == []


def test_parse_contact_actions():
    key = b'y123'
    input_recs = [
        {'key': key, 'subkey': b'100', 'value': '\turl=http://example.com\ttitle=Example\treferer='},
        {'key': key, 'subkey': b'200', 'value': '\turl=http://other.com\ttitle=Other\treferer='},
        {'key': key, 'subkey': b'300', 'value': '\turl=tel:79002003040\ttitle=Call me\treferer=example.com'},
        {'key': key, 'subkey': b'400', 'value': '\turl=mailto:79002003040@mail.ru\ttitle=Call me too\treferer=other.com'},
    ]
    expected_result = [
        {
            'yuid': b'123',
            'ts': 300,
            'id_value': b'79002003040',
            'id_type': b'phone',
            'id_value_md5': b'6d4f46fd21522773e71e9eb80b286076',
            'dwelltime': 100,
            'url': b'tel:79002003040',
            'title': b'Call me',
            'referer': b'example.com',
            'parent_url': b'http://example.com',
            'parent_title': b'Example'
        },
        {
            'yuid': b'123',
            'ts': 400,
            'id_value': b'79002003040@mail.ru',
            'id_type': b'email',
            'id_value_md5': b'65583fc3f2bd641cefbca2b7bc7130b1',
            'dwelltime': None,
            'url': b'mailto:79002003040@mail.ru',
            'title': b'Call me too',
            'referer': b'other.com',
            'parent_url': b'http://other.com',
            'parent_title': b'Other'
        }
    ]

    assert list(parse_contact_actions({'key': key}, input_recs)) == expected_result
