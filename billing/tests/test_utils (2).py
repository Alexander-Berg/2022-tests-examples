from typing import Iterator

from dwh.grocery.dcs.utils.common import split_by_chunks


def test_split_by_chunks() -> None:
    def to_list(iter_of_iter: Iterator[Iterator]):
        return [list(it) for it in iter_of_iter]

    assert to_list(split_by_chunks([1, 2, 3, 4], 2)) == [[1, 2], [3, 4]]
    assert to_list(split_by_chunks([1, 2, 3], 2)) == [[1, 2], [3]]
    assert to_list(split_by_chunks([1], 2)) == [[1]]
    assert to_list(split_by_chunks([], 2)) == []
    assert to_list(split_by_chunks([1, 2, 3], 1)) == [[1], [2], [3]]
