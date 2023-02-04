# encoding: utf-8

import pytest

from cppzoom import (
    ZRe2Validator
)


@pytest.mark.parametrize("pattern,is_valid,error", [
    ("123", True, ""),
    ("[A-Z]+", True, ""),
    ("(", False, "missing ): ("),
    ("[A-Z+", False, "missing ]: [A-Z+"),
    ("(?!test)", False, "invalid perl operator: (?!"),
])
def test_re2_validator(pattern, is_valid, error):
    validator = ZRe2Validator(pattern)
    assert is_valid == validator.is_ok()
    assert error == validator.get_error()
