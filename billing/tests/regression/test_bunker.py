import random as rnd

from dwh.grocery.tools.bunker import (
    get_from_cache,
    put_into_cache,
    get_from_bunker,
)

test_node_1 = "cus/cus"
test_node_2 = "cus/pus"
version = "stable"


class TestBunker:

    def test_get_none(self):
        assert get_from_cache("123/123", version) is None
        i = rnd.randint(1, 100)
        put_into_cache(i, "123/123", version)

    def test_set_get(self):
        i = rnd.randint(1, 100)
        put_into_cache(i, test_node_1, version)
        assert get_from_cache(test_node_1, version) == i

    def test_cross_set_get(self):
        i = rnd.randint(1, 100)
        j = rnd.randint(1, 100)
        put_into_cache(i, test_node_1, version)
        put_into_cache(j, test_node_2, version)

        assert get_from_cache(test_node_1, version) == i
        assert get_from_cache(test_node_2, version) == j

    def test_multi_set_get(self):
        i = rnd.randint(1, 100)
        j = rnd.randint(1, 100)
        put_into_cache(i, test_node_1, version)
        assert get_from_cache(test_node_1, version) == i
        put_into_cache(j, test_node_1, version)
        assert get_from_cache(test_node_1, version) == j

    def test_get_from_bunker(self):
        c = get_from_bunker("/dwh/test/export", version)
        cc = tuple(get_from_cache("/dwh/test/export", version))
        ccc = get_from_bunker("/dwh/test/export", version)

        assert c == cc
        assert cc == ccc
