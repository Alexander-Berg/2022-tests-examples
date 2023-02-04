# *-* encoding: utf-8 *-*

import pytest

from at.common import Inflector

# TODO: fix tests
# def test_inflector_basic():
#     """Should call _do_inflector_request and remap result"""
#     res = Inflector.inflect('Олесь Писаренко', 'man')
#     need_res = {
#             'im': 'Олесь Писаренко',
#             'rod': 'Олеся Писаренко',
#             'dat': 'Олесю Писаренко',
#             'vin': 'Олеся Писаренко',
#             'tvor': 'Олесем Писаренко',
#             'pred': 'Олесе Писаренко'
#             }
#     assert res == need_res
#
#
# def test_inflector_composite():
#     """Correct results with composite second name"""
#     res = Inflector.inflect('Ирина Ажимова (Крупнова)', 'woman')
#     need_res = {
#             'im': 'Ирина Ажимова (Крупнова)',
#             'rod': 'Ирины Ажимовой (Крупновой)',
#             'dat': 'Ирине Ажимовой (Крупновой)',
#             'vin': 'Ирину Ажимову (Крупнову)',
#             'tvor': 'Ириной Ажимовой (Крупновой)',
#             'pred': 'Ирине Ажимовой (Крупновой)'
#             }
#     assert res == need_res
#
#
# def test_inflector_broken():
#     """Return empty dict for invalid title"""
#     assert Inflector.inflect('   broken', 'woman') == {}


