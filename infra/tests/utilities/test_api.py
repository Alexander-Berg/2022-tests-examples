"""Test api utilities."""
from walle.util import patterns


def test_pattern_compiler():
    # plain substring
    assert patterns.parse_pattern("substring").pattern == "substring"
    # all regex special symbols are escaped
    assert patterns.parse_pattern("(").pattern == "\\("

    # guards borders
    assert patterns.parse_pattern("*substring").pattern == "^.*substring$"
    assert patterns.parse_pattern("substring*").pattern == "^substring.*$"
    assert patterns.parse_pattern("sub*string").pattern == "^sub.*string$"

    # multiple inner wildcards
    assert patterns.parse_pattern("*sub*string").pattern == "^.*sub.*string$"
    assert patterns.parse_pattern("*sub*string*").pattern == "^.*sub.*string.*$"
    assert patterns.parse_pattern("*sub*str*ing*").pattern == "^.*sub.*str.*ing.*$"

    # escapes pattern
    assert patterns.parse_pattern(".*sub.string").pattern == r"^\..*sub\.string$"
