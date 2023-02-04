from billing.apikeys.apikeys.cacher import LFUCache


def test_lfu_cache_resize():
    cache = LFUCache(3, freq_list_size=4)

    cache.put(1, 6661, 40)
    cache.put(2, 6662)
    cache.put(3, 6663)

    # add cache hit
    cache.find(1)

    cache.resize(4)
    cache.put(7, 6667, 40)
    cache.put(8, 6668)

    assert cache.find(7)
    assert cache.find(8)

    cache.resize(3)

    assert cache.find(7)
    assert cache.find(8)

    cache.resize(2)

    # frequently used
    assert cache.find(7)
    assert cache.find(8)


def test_lfu_cache_expiring_earlier_evict_first():
    cache = LFUCache(3, freq_list_size=4)

    cache.put(1, 6661, 40)
    cache.put(2, 6662)
    cache.put(3, 6663)

    cache.put(4, 6664)

    assert not cache.find(2)  # evict

    assert cache.find(1)
    assert cache.find(3)
    assert cache.find(4)

    cache.put(5, 6665)

    assert not cache.find(3)

    cache.put(6, 6666)

    assert not cache.find(5)


def test_lfu_cache_min_frequency():
    cache = LFUCache(3, freq_list_size=4)

    cache.put(1, 6661, 40)
    cache.put(2, 6662)
    cache.put(3, 6663)
    cache.put(4, 6664)

    assert cache.find(1)
    assert cache._min_freq == 0

    assert cache.find(3)
    assert cache._min_freq == 0

    assert cache.find(4)
    assert cache._min_freq == 1

    cache.find(1)
    cache.find(3)
    cache.find(4)

    assert cache._min_freq == 2

    cache.find(1)
    cache.find(3)
    cache.find(4)

    assert cache._min_freq == 3

    cache.find(1)
    cache.find(3)
    cache.find(4)

    assert cache._min_freq == cache._max_freq
