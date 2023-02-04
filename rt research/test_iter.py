import pytest

from irt.utils import chunks, list_like, ensure_list


def test_list_like():
    assert list_like(("f", "f"))
    assert list_like(["f", "f"])
    assert list_like(iter("ff"))
    assert list_like(range(44))

    assert not list_like("ff")
    assert not list_like(b"ff")
    assert not list_like(u"ff")
    assert not list_like(44)
    assert not list_like(list_like)


def test_chunks():
    assert list(list(chunk) for chunk in chunks([1, 2, 3, 4, 5, 6], 1)) == [[1], [2], [3], [4], [5], [6]]
    assert list(list(chunk) for chunk in chunks([1, 2, 3, 4, 5, 6], 2)) == [[1, 2], [3, 4], [5, 6]]
    assert list(list(chunk) for chunk in chunks([1, 2, 3, 4, 5, 6], 3)) == [[1, 2, 3], [4, 5, 6]]
    assert list(list(chunk) for chunk in chunks([1, 2, 3, 4, 5, 6], 4)) == [[1, 2, 3, 4], [5, 6]]
    assert list(list(chunk) for chunk in chunks([1, 2, 3, 4, 5, 6], 5)) == [[1, 2, 3, 4, 5], [6]]
    assert list(list(chunk) for chunk in chunks([1, 2, 3, 4, 5, 6], 6)) == [[1, 2, 3, 4, 5, 6]]
    assert list(list(chunk) for chunk in chunks([1, 2, 3, 4, 5, 6], 7)) == [[1, 2, 3, 4, 5, 6]]
    assert list(list(chunk) for chunk in chunks([1, 2, 3, 4, 5, 6])) == [[1, 2, 3, 4, 5, 6]]


def test_read_to_end():
    assert list(list(chunk) for chunk in list(chunks([1, 2, 3, 4, 5, 6], 6))) == [[1], [2], [3], [4], [5], [6]]


def test_read_twice():
    assert list((list(chunk), list(chunk)) for chunk in chunks([1, 2, 3, 4, 5, 6], 2)) == [([1, 2], []), ([3, 4], []), ([5, 6], [])]


def test_bad_size():
    list(chunks([], 0))
    list(chunks([], -1))
    with pytest.raises(ValueError):
        list(chunks([1], 0))
    with pytest.raises(ValueError):
        list(chunks([1], -1))


def test_ensure():
    assert ensure_list([]) == []
    assert ensure_list(tuple()) == []
    assert ensure_list(('', )) == ['']
    assert ensure_list('') == ['']
    assert ensure_list(None) == [None]
    assert ensure_list([1, 2, 3]) == [1, 2, 3]
    assert ensure_list(range(1, 3)) == [1, 2]
