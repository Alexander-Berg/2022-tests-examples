import pytest

from maps_adv.export.lib.core.utils import split_to_chunks


@pytest.mark.parametrize(
    "collection, expected_chunks",
    (
        # equal chunks
        [[1, 2, 3, 4], ((1, 2), (3, 4))],
        [{1: "a", 2: "b", 3: "c", 4: "d"}, ((1, 2), (3, 4))],
        # not equal chunks
        [[1, 2, 3, 4, 5], ((1, 2), (3, 4), (5,))],
        [{1: "a", 2: "b", 3: "c", 4: "d", 5: "e"}, ((1, 2), (3, 4), (5,))],
        # empty collection
        [list(), tuple()],
        [dict(), tuple()],
        # chunk size > collection size
        [[1], ((1,),)],
        [{1: "a"}, ((1,),)],
    ),
)
def test_splits_to_chunks(collection, expected_chunks):
    chunks = split_to_chunks(collection, 2)

    chunks = tuple(chunks)
    assert chunks == expected_chunks


def test_raises_for_not_iterable():
    chunks = split_to_chunks(3, 2)
    with pytest.raises(TypeError):
        tuple(chunks)
