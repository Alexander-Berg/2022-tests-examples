import pytest

import maps.analyzer.pylibs.cachely as cachely

from .calls import CALLS, register_call


CACHE_ENABLED = True
SIMULATE_FAIL = False


# Tests


def test_cache(ytc, clear_cache):
    CALLS.clear()
    enable_cache(True)
    simulate_fail(False)

    run1 = run(ytc)
    run2 = run(ytc)

    assert run1 == run2
    assert CALLS[generate_data] == 2
    assert CALLS[sum_values] == 3
    assert CALLS[merge_tables] == 1 * 2  # not cached, called both times


def test_cache_on_table_path(ytc, clear_cache):
    CALLS.clear()
    enable_cache(True)
    simulate_fail(False)

    run1 = run_on_table_path(ytc)
    run2 = run_on_table_path(ytc)

    assert run1 == run2
    assert CALLS[generate_data] == 1
    assert CALLS[sum_values] == 2


def test_no_cache(ytc, clear_cache):
    CALLS.clear()
    enable_cache(False)
    simulate_fail(False)

    run1 = run(ytc)
    run2 = run(ytc)

    assert run1 == run2
    assert CALLS[generate_data] == 2 * 2
    assert CALLS[sum_values] == 3 * 2
    assert CALLS[merge_tables] == 1 * 2


def test_local_cache(ytc, clear_cache):
    _test_local_impl(ytc, run_local, sum_values)


def test_local_cache_wrapped(ytc, clear_cache):
    _test_local_impl(ytc, run_local_wrapped, sum_values_local)


def _test_local_impl(ytc, run_fn, sum_fn):
    CALLS.clear()
    enable_cache(True)

    simulate_fail(True)
    with pytest.raises(RuntimeError):
        run_fn(ytc)

    simulate_fail(False)
    res1 = run_fn(ytc)

    assert CALLS[generate_data] == 1
    assert CALLS[sum_fn] == 1

    res2 = run_fn(ytc)

    assert res1 == res2
    assert CALLS[generate_data] == 1
    assert CALLS[sum_fn] == 2, "should be cached only in case of failure"


def test_check_enabled_arg():
    with pytest.raises(ValueError):
        @cachely.caches(enabled=CACHE_ENABLED)
        def incorrect_decorator(ytc):
            return ytc.create_temp_table()


def test_check_already_caches():
    with pytest.raises(ValueError):
        @cachely.caches()
        @cachely.caches()
        def cached_function(ytc):
            return ytc.create_temp_table()

    @cachely.caches()
    def cached_function_2(ytc):
        return ytc.create_temp_table()

    with pytest.raises(ValueError):
        cachely.cached(cached_function_2, None)


def test_fn_returns_list(ytc):
    @cachely.caches()
    def list_fn(ytc):
        return ytc.create_temp_tables(count=2)

    with pytest.raises(BaseException):
        list_fn(ytc)


def test_fn_returns_none(ytc):
    @cachely.caches()
    @register_call
    def none_fn(ytc):
        return None

    CALLS.clear()

    run1 = none_fn(ytc)
    run2 = none_fn(ytc)

    assert run1 is None and run2 is None
    assert CALLS[none_fn] == 1


# Tested functions


@cachely.hashes()
def get_range(ytc, t, start, end):
    res = ytc.create_temp_table()
    ytc.copy(t, res, force=True)
    return ytc.TablePath(res, start_index=start, end_index=end)


def run_on_table_path(ytc):
    n = 100
    gen = generate_data(ytc, 0, n)
    gen1 = get_range(ytc, gen, start=0, end=n // 2)
    gen2 = get_range(ytc, gen, start=n // 2, end=n)

    sum1 = cachely.cached(sum_values, ytc, gen1)
    sum2 = cachely.cached(sum_values, ytc, gen2)

    val1 = get_result(ytc, sum1)
    val2 = get_result(ytc, sum2)

    assert val1 == (n // 2 - 1) * n // 2 // 2
    assert val2 == (n - 1) * n // 2 - val1

    return val1, val2


def run(ytc):
    gen1 = generate_data(ytc, 0, 100)
    gen2 = generate_data(ytc, 200, 400)
    merged = cachely.hashed(merge_tables, ytc, [gen1, gen2])

    sum1 = cachely.cached_if(CACHE_ENABLED, sum_values, ytc, gen1)
    sum2 = cachely.cached_if(CACHE_ENABLED, sum_values, ytc, gen2)
    merged_sum = cachely.cached_if(CACHE_ENABLED, sum_values, ytc, merged)

    val1 = get_result(ytc, sum1)
    val2 = get_result(ytc, sum2)
    merged_val = get_result(ytc, merged_sum)

    assert val1 + val2 == merged_val
    return val1, val2


def run_local(ytc):
    with cachely.Local() as l:
        gen1 = generate_data(ytc, 0, 100)
        sum1 = l.cached(sum_values, ytc, gen1)
        return get_result(ytc, sum1)


@cachely.scopes()
def run_local_wrapped(ytc):
    gen1 = generate_data(ytc, 0, 100)
    sum1 = sum_values_local(ytc, gen1)
    return get_result(ytc, sum1)


@cachely.caches(enabled=lambda: CACHE_ENABLED)
@register_call
def generate_data(ytc, start, end):
    tmp = ytc.create_temp_table()
    ytc.write_table(tmp, [
        {'value': i}
        for i in range(start, end)
    ])
    return tmp


@register_call
def merge_tables(ytc, tbls):
    tmp = ytc.create_temp_table()
    ytc.run_merge(tbls, tmp)
    return tmp


@register_call
def sum_values(ytc, tbl):
    def sum_reducer(key, rows):
        yield {'result': sum(r['value'] for r in rows)}

    tbl_sorted = ytc.create_temp_table()
    ytc.run_sort(tbl, tbl_sorted, sort_by=['value'])

    tmp = ytc.create_temp_table()
    ytc.run_reduce(sum_reducer, tbl_sorted, tmp, reduce_by=[])
    ytc.remove(tbl_sorted)
    return tmp


@cachely.caches(local=True)
@register_call
def sum_values_local(ytc, tbl):
    return sum_values(ytc, tbl)


@register_call
def get_result(ytc, tbl):
    if SIMULATE_FAIL:
        raise RuntimeError('failure')
    return list(ytc.read_table(tbl))[0]['result']


# Test settings


def enable_cache(enable):
    global CACHE_ENABLED
    CACHE_ENABLED = enable


def simulate_fail(enable):
    global SIMULATE_FAIL
    SIMULATE_FAIL = enable
