from checks.number_ranges import RangeList, CpuNumbersRange
import pytest


class TestNumbersRange(object):
    @pytest.mark.parametrize(["range_str", "range_borders"], [
        ("0-1", [0, 1]),
        ("0-2", [0, 2]),
        ("1-2", [1, 2]),
        ("10-20", [10, 20]),
    ])
    def test_parses_single_range(self, range_str, range_borders):
        assert CpuNumbersRange.parse(range_str) == CpuNumbersRange(*range_borders)

    @pytest.mark.parametrize("range_str", ["0", "1", "12",])
    def test_parses_single_digit_range(self, range_str):
        assert CpuNumbersRange.parse(range_str) == CpuNumbersRange(int(range_str))

    @pytest.mark.parametrize(["range_str", "range_borders"], [
        ("0", [0]),
        ("1", [1]),
        ("0-1", [0, 1]),
        ("0-2", [0, 2]),
        ("1-2", [1, 2]),
        ("10-20", [10, 20]),
    ])
    def test_serializes_range_into_string(self, range_str, range_borders):
        assert CpuNumbersRange(*range_borders).as_string() == range_str

    @pytest.mark.parametrize("range_str", ["0", "1", "12", ])
    def test_serializes_single_digit_range(self, range_str):
        assert CpuNumbersRange(int(range_str)).as_string() == range_str

    @pytest.mark.parametrize(["range_borders", "expected_numbers"], [
        ([0], [0]),
        ([0, 1], [0, 1]),
        ([0, 2], [0, 1, 2]),
        ([1, 2], [1, 2]),
        ([10, 20], [10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20]),
    ])
    def test_iterates_over_numbers(self, range_borders, expected_numbers):
        assert list(CpuNumbersRange(*range_borders)) == expected_numbers

    @pytest.mark.parametrize(["smaller_range_borders", "bigger_range_borders"], [
        ([0], [1]),
        ([0], [0, 1]),
        ([1, 1], [1, 2]),
        ([1, 2], [1, 3]),
    ])
    def test_compares_borders(self, smaller_range_borders, bigger_range_borders):
        smaller_range = CpuNumbersRange(*smaller_range_borders)
        bigger_range = CpuNumbersRange(*bigger_range_borders)

        assert bigger_range > smaller_range
        assert smaller_range < bigger_range

    @pytest.mark.parametrize(["first", "second", "common"], [
        ("0", "0", "0"),
        ("0", "0-1", "0"),
        ("1", "0-1", "1"),
        ("0-1", "0-1", "0-1"),
        ("0-1", "1", "1"),
        ("0-1", "1-2", "1"),
        ("0", "1-2", None),
        ("3", "1-2", None),
        ("0-5", "6-9", None),
        ("0-6", "6-9", "6"),
        ("0-9", "6-9", "6-9"),
    ])
    def test_intersection(self, first, second, common):
        first = CpuNumbersRange.parse(first)
        second = CpuNumbersRange.parse(second)
        common = CpuNumbersRange.parse(common) if common is not None else None

        assert first.intersection(second) == common
        assert second.intersection(first) == common


class TestRangeList(object):
    def test_parses_single_range(self):
        assert RangeList.parse("0-1") == RangeList([CpuNumbersRange(0, 1)])

    def test_parses_multiple_ranges(self):
        assert RangeList.parse("0-1,10,20-32") == RangeList([
            CpuNumbersRange(0, 1), CpuNumbersRange(10), CpuNumbersRange(20, 32)
        ])

    def test_serializes_into_string(self):
        assert "0-1,10,20-32" == RangeList([
            CpuNumbersRange(0, 1), CpuNumbersRange(10), CpuNumbersRange(20, 32)
        ]).as_string()

    def test_compresses_ranges_before_serializing(self):
        assert "0-10,20-30" == RangeList([
            CpuNumbersRange(0, 1), CpuNumbersRange(1), CpuNumbersRange(1, 2), CpuNumbersRange(0, 2),
            CpuNumbersRange(0, 10), CpuNumbersRange(1, 9), CpuNumbersRange(20, 25), CpuNumbersRange(26, 30),
        ]).as_string()

    @pytest.mark.parametrize(["ranges_str", "expected_numbers"], [
        ("0", [0]),
        ("0,1", [0, 1]),
        ("0-1", [0, 1]),
        ("0-2,4-7", [0, 1, 2, 4, 5, 6, 7]),
    ])
    def test_iterates_over_ranges(self, ranges_str, expected_numbers):
        assert list(RangeList.parse(ranges_str)) == expected_numbers

    @pytest.mark.parametrize(["first", "second", "common"], [
        ("0", "0", "0"),
        ("0", "1", None),
        ("0", "0-1", "0"),
        ("1", "0-1", "1"),
        ("0-1", "0-1", "0-1"),
        ("0-6", "7-11", None),
        ("0-6", "6", "6"),
        ("0-6", "6-11", "6"),
        ("0-6", "3-11", "3-6"),
        ("0-1,3-5", "0-1", "0-1"),
        ("0-1,3-5", "3-5", "3-5"),
        ("0-1,3-5", "0-5", "0-1,3-5"),
        ("0-1,3-5", "0-4", "0-1,3-4"),
        ("0-1,3-5", "1-4", "1,3-4"),
        ("0-2,4-6", "1-5", "1-2,4-5"),
        ("0-2,4-6,8-20", "1-10", "1-2,4-6,8-10"),
        ("0-2,8-20", "1-10", "1-2,8-10"),
        ("0-2,4-6,8-20", "3", None),
        ("0-2,4-6,8-20", "7", None),
        ("0-2,4-6,8-20", "3,7", None),
        ("4-6,8-20", "0", None),
        ("4-6,8-20", "3", None),
        ("4-6,8-20", "7", None),
        ("4-6,8-20", "21", None),
        ("4-6,8-20", "22-40", None),
        ("2,4-6,8-20", "0-40", "2,4-6,8-20"),
        ("0-99,100-200,300-400,500-900", "0-99,100-200,300-400,500-900", "0-200,300-400,500-900"),
    ])
    def test_intersection(self, first, second, common):
        first = RangeList.parse(first)
        second = RangeList.parse(second)
        common = RangeList.parse(common) if common is not None else None

        assert first.intersection(second) == common
        assert second.intersection(first) == common
