import pytest

from smarttv.droideka.utils import strings

MINIFIED_JSON_TEST_DATA = [
    ({'a': 'b', 'c': 123}, '{"a":"b","c":123}'),
    ({'a': 'b', 'c': 123, 'd': {'e': 4}}, '{"a":"b","c":123,"d":{"e":4}}'),
    ([{'a': 'b', 'c': 123}, {'a': 'b', 'c': 123, 'd': {'e': 4}}], '[{"a":"b","c":123},{"a":"b","c":123,"d":{"e":4}}]'),
    ({}, '{}'),
    ([], '[]'),
    (None, None)
]


@pytest.mark.parametrize('input, output', MINIFIED_JSON_TEST_DATA)
def test_to_minified_json(input, output):
    assert strings.to_minified_json(input) == output
