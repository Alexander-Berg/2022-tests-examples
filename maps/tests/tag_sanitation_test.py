import pytest
from hypothesis import given, strategies as st

from maps.infra.sandbox.tag_sanitation import sanitize_tag
from sandbox.common.types.task import TaskTag


@pytest.mark.parametrize(
    'raw,expected',
    [
        ('', 'empty'),
        ('Normal boring tag', 'Normal_boring_tag'),
        ('-starts-with-non-alphanumeric-', '_starts-with-non-alphanumeric-'),
    ],
)
def test_baseline(raw: str, expected: str) -> None:
    actual = sanitize_tag(raw)
    assert TaskTag.test(actual)
    assert expected == actual


@given(st.text(max_size=1_000))
def test_fuzzy(raw: str) -> None:
    assert TaskTag.test(sanitize_tag(raw))
