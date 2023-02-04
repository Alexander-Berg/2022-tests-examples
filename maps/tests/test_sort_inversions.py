from maps.geoq.experiments.lib.sort_inversions import count_sort_inversions


def test_count_sort_inversions_empty():
    assert count_sort_inversions([]) == 0


def test_count_sort_inversions_single():
    assert count_sort_inversions([1]) == 0


def test_count_sort_inversions_pair():
    assert count_sort_inversions([2, 1]) == 1


def test_count_sort_inversions_sorted():
    test_array = [1, 3, 4, 5, 8, 9, 10]
    assert count_sort_inversions(test_array) == 0


def test_count_sort_inversions_sorted_reversed():
    test_array = [10, 6, 4, 3]
    assert count_sort_inversions(test_array, reverse=True) == 0


def test_count_sort_inversions():
    test_array = [6, 8, 1, 2, 3, 5, 6]
    # 6 - 1, 2, 3, 5
    # 8 - 1, 2, 3, 5, 6, 6
    assert count_sort_inversions(test_array) == 9


def test_count_sort_inversions_reverse():
    test_array = [5, 9, 3, 1, 2, 6]
    # 5 - 6, 9
    # 3 - 6
    # 1 - 2, 6
    # 2 - 6
    assert count_sort_inversions(test_array, reverse=True) == 6
