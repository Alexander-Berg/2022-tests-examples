import pytest
import maps.garden.server.lib.utils.prefix_tree as prefix_tree


NEED_OCCURRENCE_PERCENTAGE = 60


def test_prefix_tree():
    tree = prefix_tree.PrefixTree()
    tree.add_word("a")
    assert "a" in tree._root.children
    assert tree._root.occurrence == 1
    assert tree._root.children["a"].occurrence == 1


@pytest.mark.parametrize(
    ("input", "output"),
    [
        ([""], [""]),
        (["a", "b"], ["a", "b"]),
        (["a_a", "a_b"], ["a", "b"]),
        (["a_a", "a_b", "b_b", "b_a"], ["a_a", "a_b", "b_b", "b_a"]),
        ([" _aa", "a_a", "a_b"], [" _aa", "a", "b"]),
        (["ymapsdf_osm_ad", "ymapsdf_osm_ft", "osm_to_yt_input"], ["ad", "ft", "osm_to_yt_input"]),
    ],
)
def test_cut_most_common_prefix(input, output):
    assert output == prefix_tree.cut_most_common_prefix(input, NEED_OCCURRENCE_PERCENTAGE)


@pytest.mark.parametrize(
    ("input", "output"),
    [
        ([""], [""]),
        (["a", "b"], ["a", "b"]),
        (["a_a", "a_b"], ["a_a", "a_b"]),
        (["a_a", "b_a"], ["a", "b"]),
        (["a_a", "a_b", "b_b", "b_a"], ["a_a", "a_b", "b_b", "b_a"]),
        ([" _aa", "_aa", "_ab"], [" ", "", "_ab"]),
        (["addr_cis1_osm", "ad_cis1_osm", "ft_cis1_osm", "input_cis1_osm"], ["addr", "ad", "ft", "input"]),
    ],
)
def test_cut_most_common_suffix(input, output):
    assert output == prefix_tree.cut_most_common_suffix(input, NEED_OCCURRENCE_PERCENTAGE)


@pytest.mark.parametrize(
    ("input", "output"),
    [
        ([""], [""]),
        (["a", "b"], ["a", "b"]),
        (["aaa bcd", "aaa efd"], ["aaa bcd", "aaa efd"]),
        (["a_a", "a_b"], ["a", "b"]),
        (["a_a", "b_a"], ["a", "b"]),
        (["a_a", "a_b", "b_b", "b_a"], ["a_a", "a_b", "b_b", "b_a"]),
        (
            ["ymapsdf_osm_addr_cis1_osm", "ymapsdf_osm_ad_cis1_osm", "ymapsdf_osm_ft_cis1_osm", "osm_to_yt_input_cis1_osm"],
            ["addr", "ad", "ft", "osm_to_yt_input"]
        ),
    ],
)
def test_cut_most_common_prefix_and_suffix(input, output):
    assert output == prefix_tree.cut_most_common_prefix_and_suffix(input, NEED_OCCURRENCE_PERCENTAGE)
