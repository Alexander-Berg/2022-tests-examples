import json

import pytest

from yaphone.localization import try_to_fix_json

CORRUPT_COMPLEX_ONE_ELEMENT = """[{
    "key": "the value",
},]"""
CORRUPT_COMPLEX_MANY_KEYS = """[
  {
    'first_key': "the roof" ,
    "second_key": "the roof",
  },
]"""
CORRUPT_COMPLEX_MANY_ELEMENTS = """[
  { "third": "the roof", },
  { "fourth': "is on fire" } ,
]"""
CORRUPT_DATA = [
    "'My string'",
    '[1.3, 4, 5, ]',
    "{'answer': 42}",
    CORRUPT_COMPLEX_ONE_ELEMENT,
    CORRUPT_COMPLEX_MANY_KEYS,
    CORRUPT_COMPLEX_MANY_ELEMENTS,
]
UNRECOVERABLE_SIMPLE = '{"answer": 42}, {"question": 34}'
UNRECOVERABLE_COMPLEX = """[{
    key: "the value",
},]"""
UNRECOVERABLE_DATA = [
    "We don't need no water",
    UNRECOVERABLE_SIMPLE,
    UNRECOVERABLE_COMPLEX
]


@pytest.mark.parametrize('corrupt', CORRUPT_DATA)
def test_try_to_fix_json_should_succeed(corrupt):
    result = try_to_fix_json(corrupt)
    assert json.loads(result)


@pytest.mark.parametrize('unrecoverable', UNRECOVERABLE_DATA)
def test_try_to_fix_json_should_fail(unrecoverable):
    result = try_to_fix_json(unrecoverable)
    with pytest.raises(ValueError):
        assert json.loads(result)
