from ads_pytorch.yt.table_path import TablePath
import yt.wrapper as yt
import pytest


def test_table_path_from_str():
    _s = "//home/ahaha"
    path = TablePath(_s)
    assert path.to_yson_string() == _s
    assert str(path) == _s


def test_table_path_from_table_path():
    p1 = TablePath("//home/ahaha", exact_key="123")
    path = TablePath(p1)
    assert path.to_yson_string() == p1.to_yson_string()
    assert str(path) == str(p1)
    assert path.ranges == p1.ranges == yt.TablePath("//home/ahaha", exact_key="123").ranges


@pytest.mark.parametrize("cls", [TablePath, yt.TablePath], ids=[f"{TablePath.__name__}", "yt.TablePath"])
def test_table_path_from_table_path_ranged(cls):
    p1 = cls("//home/ahaha", exact_key="123")
    path = TablePath(p1)
    assert path.to_yson_string() == p1.to_yson_string()
    assert str(path) == str(p1)
    assert path.ranges == p1.ranges == yt.TablePath("//home/ahaha", exact_key="123").ranges


@pytest.mark.parametrize("cls", [TablePath, yt.TablePath], ids=[f"{TablePath.__name__}", "yt.TablePath"])
def test_table_path_from_table_path_norange(cls):
    p1 = cls("//home/ahaha")
    path = TablePath(p1)
    assert path.to_yson_string() == p1.to_yson_string()
    assert str(path) == str(p1)
    assert path.ranges == p1.ranges == yt.TablePath("//home/ahaha").ranges


def test_extra_attributes():
    p1 = yt.TablePath("//home/ahaha", exact_key="123", columns=["1"])
    with pytest.raises(ValueError):
        TablePath(p1, exact_key="456")


# __eq__ test


@pytest.mark.parametrize(
    "range_kwargs",
    [
        {"exact_key": 1},
        {"lower_key": "2"},
        {"upper_key": "3"},
        {"exact_index": 4},
        {"start_index": 5},
        {"end_index": 6},
        {"lower_key": "4", "upper_key": 1006},
        {"start_index": 90, "end_index": 95}
    ]
)
def test_eq_hash_operator(range_kwargs):
    _raw_path = "//home/ahaha"
    p1 = TablePath(_raw_path, **range_kwargs)
    p2 = TablePath(_raw_path, **range_kwargs)
    assert p1 == p2
    assert hash(p1) == hash(p2)
