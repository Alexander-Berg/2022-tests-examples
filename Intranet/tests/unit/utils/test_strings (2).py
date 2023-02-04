import pytest

from plan.common.utils.strings import fetch_lines

strings = [
    """FIRST_STRING
    SECOND_STRING""",
    """
FIRST_STRING
SECOND_STRING

Function bla-bla
""",
    """
FIRST_STRING
SECOND_STRING
Function bla-bla
""",

]


@pytest.mark.parametrize('s', strings)
def test_splitter(s):
    assert fetch_lines(s, 2) == ['FIRST_STRING', 'SECOND_STRING']
